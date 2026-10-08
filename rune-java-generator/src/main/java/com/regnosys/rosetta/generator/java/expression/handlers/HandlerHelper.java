package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.RContainsExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.regulatory.RBody;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.object.ConditionCases;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.enums.SwitchGuardLiteralKind;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Shared utility for expression handlers. */
public final class HandlerHelper {
    private HandlerHelper() {}

    /**
     * Maximum parent walks to guard against cyclic AST parent pointers — ONE limit every parent walk
     * consults instead of duplicating the literal (Copilot engine PR #1 R6 F2/F5, then package-visible
     * for the sibling handlers such as {@link ReferenceHandler}). PUBLIC since v3.2 seat 7 round 1:
     * {@code FunctionAliasHelper.caseNarrowedNavTypeOrNull} (the {@code function} package) reads it,
     * and since round 2 the IR module's {@code IRExpressionCompiler} consults it too (LAW 69).
     */
    public static final int PARENT_WALK_LIMIT = 64;

    /**
     * facet aliasOperandMetaCoerce (PR #326, F4a): the walk bound for the
     * CONTAINER-seeking walks ({@link #findEnclosingFunction} /
     * {@link #findEnclosingRule}). Those walks terminate at the AST root on every
     * well-formed tree — the bound only guards pathological parent cycles — but
     * {@value #PARENT_WALK_LIMIT} was too small for deep LEFT-RECURSIVE operator
     * chains: Qualify_Transaction_OIS's 86-comparison {@code or}-chain puts its
     * deepest operands ~90 parent steps from the {@link RFunction} root, so
     * {@code findEnclosingFunction} returned {@code null} for the deepest 26 and
     * {@code renderEnclosingInputs} emitted the arg-less (non-compiling)
     * {@code floatingRateIndex()} alias call. The LOCAL context walks (closure-param
     * owner, then-owner, …) keep {@link #PARENT_WALK_LIMIT} — their 64-step scope is
     * semantically part of the probe.
     */
    static final int CONTAINER_WALK_LIMIT = 4096;

    /**
     * facet implicit_operand_synthesis (PR #218 — the generator-crash lever):
     * when an operation's operand is {@code null} in the IR, substitute a
     * synthetic {@link RImplicitVariable} so the operand compiles to the bound
     * {@code thenArg} (in a {@code then}-body) or the enclosing lambda's
     * {@code item} via {@code ReferenceHandler.handle(RImplicitVariable, …)};
     * a non-{@code null} operand is returned unchanged.
     *
     * <p>A BARE implicit operand — the WHOLE piped value consumed directly with
     * no feature navigation, e.g. {@code … then all = True}, {@code … then exists},
     * {@code … then join ";"}, or a {@code switch} over the extract-lambda item —
     * parses with a {@code null} operand RExpression (the parser materialises no
     * node for the elided implicit). Upstream's ExpressionGenerator renders the
     * implicit variable for these elided operands; the fork's handlers compiled
     * the raw {@code null}, throwing an NPE at
     * {@link com.regnosys.rosetta.generator.java.expression.ExpressionCompiler#compile}
     * that FunctionGenerator caught and turned into a {@code TODO: expression
     * compilation error … "expr" is null} comment.
     *
     * <p>The synthetic variable is parented at the {@code operation} so
     * {@code ReferenceHandler.nearestEnclosingInlineFunction} resolves the
     * correct then-boundary / lambda binding — the same synthesis #177 uses for
     * a bare {@code onlyExists} element (synthesizeImplicitItemOnlyExistsReceiver).
     *
     * <p>GREEN-SAFE BY CONSTRUCTION: the {@code null}-operand branch is reached
     * ONLY on the pre-fix crash path (the generator emitted the TODO), so every
     * carrier is already waivered; a non-{@code null} operand is byte-unchanged.
     */
    public static RExpression orSyntheticImplicit(RExpression operand, RExpression operation) {
        if (operand != null) {
            return operand;
        }
        RImplicitVariable iv = new RImplicitVariable();
        iv.setSynthetic(true);
        iv.setParent(operation);
        return iv;
    }

    /**
     * facet implicit_operand_synthesis / asMapper (PR #218): true when {@code e}
     * compiles to a {@code ComparisonResult} — the boolean-operator family
     * (equality, ordered comparison, logical and/or, exists / is-absent,
     * only-exists, contains, disjoint). A {@code ComparisonResult} is a
     * {@code Mapper<Boolean>} but NOT a {@code MapperS}, so when one is consumed
     * in a {@code MapperS}-typed sink (a {@code then}-chain {@code thenArg}
     * declaration or {@code then}-chain output) upstream coerces it with
     * {@code .asMapper()} — the same node-type test {@code CollectionHandler}
     * uses for the map-body position. NOT the direct-output path: a top-level
     * {@code set out: a = b} renders the bare {@code areEqual(...).get()} form
     * (252 green carriers), so the caller MUST scope this to the then-chain
     * render points (renderThenExtractSet), never the generic output.
     */
    public static boolean isComparisonResultExpr(RExpression e) {
        return e instanceof REqualityExpr
                || e instanceof RComparisonExpr
                || e instanceof RLogicalExpr
                || e instanceof RExistenceExpr
                || e instanceof ROnlyExistsExpr
                || e instanceof RContainsExpr
                || e instanceof RDisjointExpr;
    }

    /**
     * facet switchConversionArmGet (seat 23, law F20): <b>an arm of a basic-type {@code switch} at
     * the SET seat is assigned as an ITEM</b> — upstream compiles every case result against the
     * output's item type ({@code FunctionGenerator.assign} → {@code TypeCoercionService}), so a
     * {@code Mapper}-valued arm takes {@code .get()} (golden drr 7.x {@code MapRegimeNameEnum}:
     * {@code regimeNameEnum = MapperS.of(reportingRegimeName).<String>map(…).checkedMap("to-enum",
     * RegimeNameEnum::fromDisplayName, IllegalArgumentException.class).get();}) and a wrapped item
     * is unwrapped ({@code MapperS.of(<fn>.evaluate(p))} → the bare invocation).
     *
     * <p>Render-truth arbiter (the #371 law), ONE predicate for the {@code default} arm AND every
     * case arm ({@code FunctionExpressionRenderer.renderSwitchAssignment} — LAW 69 within the seat):
     * <ul>
     *   <li>an arm whose RENDERED form is a Java NAME ({@code RegimeEnum.CSA} — the #149 enum-value
     *       family, 26,059 of the 27,380 arms the seat-23 probe counted corpus-wide; the #355/#358
     *       recovered constants render the same way) is already the item — verbatim;</li>
     *   <li>an {@code RIntLiteral} keeps mechanism 3's bare unwrap ({@code unwrapLiteralSwitchResult}
     *       — {@code result = 1;} on an Integer output, {@code BigDecimal.valueOf(N)} on a number one);
     *       an empty / empty-list default keeps {@code null};</li>
     *   <li>EVERY OTHER arm is Mapper-valued by construction — a conversion chain (the four corpus
     *       carriers), a wrap-factory invocation, a navigation, a wrapped literal — and takes the
     *       assignment unwrap ({@code unwrapForAssignment}: a whole {@code MapperS.of(…)} wrap strips
     *       to its item, a chain appends {@code .get()}).</li>
     * </ul>
     * Green-safe BY MEASUREMENT (LAW 75): the seat-23 runtime probe over all 275 matrix rows on both
     * routes found exactly four non-name, non-literal, non-empty arms at this seat — the carriers;
     * a name never changes, so no green arm can move. The NAME exclusion is a STATED decline, not a
     * load-bearing gate: {@code unwrapForAssignment}'s own Phase-X1 enum-constant guard (the producer's
     * witness, {@code isBareEnumConstant}, since PR #611)
     * already returns a bare enum constant unchanged, and every NAME-rendered arm the switch
     * assignment renders is one — the arms compile through {@code handle(REnumValueRef)}'s
     * enumeration-present branch, the bare-bound arm or {@code requalifyMisBoundEnumCase}, each of
     * which builds the witness (mutation f20-ii, the exclusion dropped, measured 0F over the whole
     * drr 7.0.0 cell when the guard still read the dotted spelling); it
     * stands for the corpus-unwitnessed UNDOTTED name, which that guard would not protect.
     *
     * <p>LAW 69 residue, DECLARED: the sibling enum-ARGUMENT SET seat
     * ({@code FunctionExpressionRenderer.renderEnumSwitchAssignment} — 14 switch RENDERS over two
     * declarations, {@code GetOrdinalForNonFinancialSectorEnum} × drr 7.0–7.3 and {@code UpdateAmount}
     * × 10 cells) carries the same "assigned as an ITEM" law as a pre-existing UNCONDITIONAL
     * {@code unwrapForAssignment} at its default and case arms and does not consult this predicate.
     * The two AGREE on a NAME arm (it reaches the {@code isBareEnumConstant} guard and returns unchanged) and
     * DIVERGE on a literal arm: this arbiter declines an {@code RIntLiteral} because F20's seat has
     * already unwrapped it (mechanism 3's {@code unwrapLiteralSwitchResult}), while the sibling has no
     * such step and relies on its unconditional unwrap to strip the wrap —
     * {@code GetOrdinalForNonFinancialSectorEnum}'s 20 int-literal case arms per drr 7.x cell depend on
     * it (golden {@code ordinal = 1;}). The BANKED consolidation onto this one read must therefore
     * carry a literal unwrap AHEAD of the read, not the read alone.
     */
    public static boolean switchArmTakesItemCoercion(RExpression arm, String rendered) {
        if (arm instanceof RIntLiteral
                || arm instanceof com.regnosys.rosetta.ast.expressions.references.REmptyLiteral
                || arm instanceof RListLiteral) {
            return false;
        }
        return !javax.lang.model.SourceVersion.isName(rendered);
    }

    /**
     * facet comparisonResultIteArmAsMapper (seat 23, law F21): <b>a ComparisonResult arm assigned
     * into a SINGLE {@code MapperS<Boolean>} if-then-else local coerces {@code .asMapper()} — in
     * EVERY context</b>. Upstream's {@code addCoercions} converts the arm to the local's type at the
     * assignment ({@code ComparisonResult} implements {@code Mapper<Boolean>}, never {@code MapperS},
     * so the bare arm never compiled). Golden drr 7.x emir {@code PTRRRule}: {@code ifThenElseResult =
     * ComparisonResult.ofNullSafe(MapperS.of(isCompressed.evaluate(…))).orNullSafe(…).asMapper();}
     * against {@code final MapperS<Boolean> ifThenElseResult;}.
     *
     * <p>ONE predicate for the two arm-wrap seats that declare that local (LAW 69 — the halves agree):
     * {@code FunctionExpressionRenderer.appendIteHoistChainCore} (the rule / function then-chain
     * {@code ifThenElseResult} hoist — the PTRRRule carrier's seat, its then AND else arm) and
     * {@code ControlFlowHandler.wrapDeepThenIteArm} (the deep-then ladder — the wave-D type-condition
     * carriers, whose TYPE-CONDITION gate this predicate retires: the seat-23 runtime probe over all
     * 275 rows on both routes found every one of its 31 ComparisonResult arms inside a type condition,
     * so the retirement moves nothing by measurement and the context never decided the law).
     * {@code WrapperToWrapperCoercer} carries the same conversion for typed expressions.
     *
     * <p>The arm's kind is the {@link #isComparisonResultExpr} family (equality, ordered comparison,
     * logical and/or, exists / is-absent, only-exists, contains, disjoint — the handlers that emit a
     * {@code ComparisonResult}); a MULTI seat ({@code MapperC} local) is excluded — a boolean
     * operator is single by typing, so no carrier exists there and the seat keeps its bytes. The
     * local IS {@code MapperS<Boolean>} by the ladder's inference whenever an arm is a boolean
     * operator. Green-safe by construction: a {@code ComparisonResult} assigned to a
     * {@code MapperS<Boolean>} local never compiled, so no byte-identical file carries the bare arm.
     */
    public static boolean comparisonResultIteArmNeedsAsMapper(RExpression armNode, boolean multi) {
        return !multi && isComparisonResultExpr(armNode);
    }

    /**
     * facet fnNotionalTogetherRestructure (PR #398) / facet iteChainCtorArmMapperWrap (v3.1
     * flip seat 32, law B.1): <b>a CONSTRUCTOR arm assigned into a Mapper-typed if-then-else
     * local wraps at the assignment</b> - {@code MapperS.of(<ctor>)} at a SINGLE seat,
     * {@code MapperC.of(Collections.singletonList(<ctor>))} at a MULTI one. A constructor
     * compiles to the RAW builder chain (never a Mapper), so the bare assignment is an
     * incompatible-types statement against the Mapper-typed local (LAW 74 - javac says
     * {@code incompatible types: Cashflow cannot be converted to MapperS<Cashflow>} at the
     * arm's {@code .build();}, once per arm) - golden drr {@code NotionalLeg}
     * {@code thenArg0 = MapperC.of(Collections.singletonList(Measure.builder()...build()));}
     * at the deep-then ladder seat, golden drr 7.0-7.3 {@code FXLeg1}/{@code FXLeg2}
     * {@code ifThenElseResult = MapperS.of(Cashflow.builder()...build());} at the ite-chain
     * seat.
     *
     * <p>ONE walk for the THREE arm-wrap call sites that assign into such a local (LAW 69 -
     * the halves agree; the same shape {@link #comparisonResultIteArmNeedsAsMapper} already
     * established at these seats): {@code ControlFlowHandler.wrapDeepThenIteArm} (the #398
     * rung, from which this body was EXTRACTED - same predicate, same order, same emitted
     * strings and the same {@code refs} additions, so that seat is byte-frozen) and BOTH arm
     * pipelines of {@code FunctionExpressionRenderer.appendIteHoistChainCore} - the per-rung
     * chain AND the terminal else - whose declaration is Mapper-typed UNCONDITIONALLY by
     * construction and which carried no constructor rung at all (the seat-32 defect). The
     * carriers put a constructor in BOTH arms, so a fix at one pipeline only heals half of
     * each file and moves no whole file (lanes {@code m-lawB1-thenonly} /
     * {@code m-lawB1-elseonly}).
     *
     * <p>The prefix test is the #218 render-truth arbiter, and it is DIRECTIONAL: a bare
     * constructor arm renders through {@code ImportCollisionResolver.typeRef}, so it opens on
     * the private-use sentinel and starts with neither wrap (the #588 review's correction to
     * an earlier claim that a prefix test would steal those arms); an ALREADY-wrapped arm
     * opens on the wrap literally. Declining on an already-wrapped render is what keeps the
     * walk idempotent across the three consumers.
     *
     * <p><b>Green-safe by construction, and MEASURED over the FULL population</b> (all
     * 174,141 goldens, 25 cells): NO golden anywhere assigns a bare {@code <Type>.builder()}
     * to a Mapper-typed local - 4,876 bare-builder assignment rows over 48 basenames, ZERO
     * of them under a Mapper-typed declaration. Every one sits under an ITEM-typed local at
     * a THIRD seat ({@code ControlFlowHandler.hoistAsItemLocalOrNull}, whose
     * {@code mapperFormSlot} is false) that this walk never reaches; the 97 wrapped rows are
     * the 16 band {@code FXLeg1}/{@code FXLeg2} ones plus green {@code Notional},
     * {@code NotionalLeg}, {@code Create_CounterpartySpecificData}(_2) and
     * {@code TechnicalRecordId} rows, which the #398 rung and the #357 Mapper-form slot arm
     * produce today. A raw builder against a Mapper local never compiled - the #339
     * green-safety argument.
     *
     * @param ctorRendered the arm as ALREADY rendered by the caller (the ite-chain seats have
     *     reindented their continuation lines; the wrap is a pure head/tail wrap that leaves
     *     them byte-identical)
     * @return the wrapped arm, or {@code ctorRendered} unchanged when the arm is not a
     *     constructor or already renders a {@code MapperS.of(} / {@code MapperC.of(} wrap
     */
    public static String wrapCtorIteArm(String ctorRendered, RExpression armNode,
            boolean multi, Set<JavaClass<?>> refs) {
        // facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615) — BOTH legs VERDICT-MOVED
        // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT, NO-STRUCTURAL-FACT, and no signature change
        // recommended. c8 census, 5,970 arrivals (1,990 / 1,990 / 1,990 — default-route D11,
        // IR-route D11, optimised) with `je=none` and `prod=none` at 5,970/5,970: there is no
        // expression in scope to ask, and the measured absence IS the result. The `MapperC.of(`
        // leg is additionally zero-fire (`text=false` at 5,970/5,970). Threading a builder is
        // available at only 240 of the 5,970 arrivals (4.0% — the one caller of four that provably
        // holds one), and it would answer a DIFFERENT question anyway, because the callers have
        // already reindented the continuation lines (the @param contract above), so
        // `render(unwrapToBuilder())` is not comparable to `ctorRendered`. The directional blind
        // spot the paragraph above discloses is now sized: 1,647 of 5,970 arrivals (27.6%) are
        // `__COERCION_PARAM_` placeholders, and 803 of them answer TRUE on the `MapperS.of(` leg —
        // 20.6% of that leg's 3,893 true answers are decided on sentinel text.
        if (armNode instanceof RConstructorExpr) {
            if (!ctorRendered.startsWith("MapperS.of(")
                    && !ctorRendered.startsWith("MapperC.of(")) {
                if (multi) {
                    refs.add(MAPPER_C);
                    refs.add(COLLECTIONS);
                    return "MapperC.of(Collections.singletonList(" + ctorRendered + "))";
                }
                refs.add(MAPPER_S);
                return "MapperS.of(" + ctorRendered + ")";
            }
        }
        return ctorRendered;
    }

    /**
     * facet defaultForm (PR #218): a SCALAR literal — the only default-value shape
     * golden renders as the bare plain-T {@code getOrDefault} argument. {@code RListLiteral}
     * is deliberately excluded (a list default is a {@code MapperC}, not a plain value).
     */
    public static boolean isScalarLiteral(RExpression e) {
        return e instanceof RBooleanLiteral || e instanceof RIntLiteral
                || e instanceof RNumberLiteral || e instanceof RStringLiteral;
    }

    /**
     * facet defaultForm (PR #218): true when {@code e} is a {@code default} operation
     * whose default value is a scalar literal — the {@code SetOperationHandler} renders it
     * {@code MapperS.of(<X>.getOrDefault(<bare-literal>))} (result-wrapped, bare arg). At a
     * {@code then}-chain OUTPUT seat the renderer must append {@code .get()} to that wrap
     * WITHOUT structurally stripping the {@code MapperS.of} (golden
     * {@code MapperS.of(<X>.getOrDefault(false)).get()}) — the generic
     * {@code unwrapForAssignment} would strip it back to the bare {@code getOrDefault}.
     */
    public static boolean isScalarLiteralDefault(RExpression e) {
        return e instanceof RDefaultExpr d && isScalarLiteral(d.rawRight());
    }

    // =========================================================================
    // PR-A §9.1 C3c.2: library-class references for emitted code.
    //
    // Handlers carry these on the builder refs (option F structured channel)
    // so the import collector receives the FQN without any regex or
    // substring scan of the emitted string. The 9 MapperS/C/… classes
    // below cover every regular library import that the deleted
    // {@code FunctionGenerator.collectExpressionImports} substring ladder
    // detected pre-C3c.2 (historical reference: L744-782 on main @ 8231bbe).
    //
    // Static-wildcard imports (ExpressionOperatorsNullSafe — covers
    // areEqual/notEqual/greaterThan/… 11 operator methods) are tracked via
    // EXPRESSION_OPERATORS_NULL_SAFE below, carried through the
    // JavaStatementBuilder.getStaticWildcardImports() parallel channel
    // added in C3a.1. ComparisonHandler + ExistenceHandler declare it at
    // their emission sites in C3a.4.b/d; the substring check that used to
    // emit this static import is deleted at C3c.2.
    // =========================================================================

    public static final JavaClass<?> MAPPER_S = JavaClass.from(
            com.rosetta.model.lib.mapper.MapperS.class);
    /**
     * v3.2 seat 2: the chained-ternary switch lowering's {@code Objects.equals(…)} ref — UNUSED since v3.2 seat 12
     * (D52 R1 retired the lowering; lane L1 of {@code lanes-s12.py} re-inserts it as its mutation), deleted with the
     * residual path's dead assembly (BANKED, see {@code ControlFlowHandler#renderGuard}).
     */
    public static final JavaClass<?> OBJECTS = JavaClass.from(java.util.Objects.class);
    public static final JavaClass<?> MAPPER_C = JavaClass.from(
            com.rosetta.model.lib.mapper.MapperC.class);

    /**
     * v3.2 seat 2 (Law 3): a STRING-literal switch-case guard as the {@code MapperS.of("…")}
     * comparand the deep-then ladder rungs compare the subject against — keyed on the front-end
     * literal KIND ({@code SwitchGuardLiteralKind}, the #609 channel), the string escaped. The ONE
     * class a corpus witness reaches at this seat (the seat-2 {@code graded} fixture and the
     * C5Forms locks); {@code null} for every other guard, so the ladder declines it. The boolean,
     * whole-number-at-BigDecimal and splice classes the SET-position ladder renders from the
     * subject's RType ({@code FunctionExpressionRenderer.renderSwitchGuardMapper}) stood here at
     * the fix commit and stayed green when narrowed away (the witness sweep's probes): banked
     * under the #614 law until a carrier appears, exactly like the predicate that admits this seat.
     */
    public static String literalGuardMapper(RSwitchCaseGuard guard, Set<JavaClass<?>> refs) {
        if (guard.kind() != SwitchGuardKind.LITERAL || guard.literalValue().isEmpty()
                || guard.literalKind().orElse(null) != SwitchGuardLiteralKind.STRING) {
            return null;
        }
        String literal = guard.literalValue().get();
        refs.add(MAPPER_S);
        String escaped = literal.replace("\\", "\\\\").replace("\"", "\\\"");
        return "MapperS.of(\"" + escaped + "\")";
    }

    /**
     * v3.2 seat 2 (the chaos C5Forms rows — the {@code fallback} default arm and the {@code asInt}
     * comparison operand): an INTEGER-item Mapper operand meeting a BIGDECIMAL-item operand widens to
     * the BigDecimal Mapper through the type-directed coercion service — upstream compiles both sides
     * against the JOINED type ({@code MAPPER.wrapExtends(left.join(right))}), so the Integer side takes
     * the wrapper→wrapper rung: {@code .<BigDecimal>map("Type coercion", integer -> integer == null ?
     * null : BigDecimal.valueOf(integer))} on a {@code MapperS} (guarded) and {@code integer ->
     * BigDecimal.valueOf(integer)} on a {@code MapperC} (its items are non-null) — the
     * {@code WrappedItemCoercer}'s own two forms. Typed channel: the operand's stamp (an alias call
     * re-typed from its signature walk, a hoisted {@code thenArg}, a list literal); a null-typed or
     * non-Integer operand returns unchanged, so the node-keyed {@code widenIntegerNavOperand} (a
     * navigation operand, re-stamped null after its own hop) can never be widened twice.
     */
    public static JavaStatementBuilder widenIntegerMapperToBigDecimal(JavaStatementBuilder operand,
            ExpressionCompiler compiler, JavaStatementScope scope) {
        JavaTypeUtil tu = compiler.getTypeUtil();
        TypeCoercionService svc = compiler.getCoercionService();
        JavaType actual = operand == null ? null : operand.getExpressionType();
        if (tu == null || svc == null || scope == null || actual == null
                || !(tu.isMapperS(actual) || tu.isMapperC(actual))
                || !tu.isInteger(tu.getItemType(actual))) {
            return operand;
        }
        return svc.coerce(operand, actual, tu.changeItemType(actual, tu.BIG_DECIMAL), scope);
    }

    /** True iff {@code b} is a Mapper-typed operand whose item is BigDecimal (the widening target). */
    public static boolean isBigDecimalMapper(JavaStatementBuilder b, JavaTypeUtil tu) {
        JavaType t = b == null ? null : b.getExpressionType();
        return tu != null && t != null && (tu.isMapperS(t) || tu.isMapperC(t))
                && tu.isBigDecimal(tu.getItemType(t));
    }

    /**
     * v3.2 seat 2 (the chaos C5Forms `fallback` rows): the Mapper type of a NON-EMPTY list literal
     * whose elements are all INT literals — {@code MapperC<Integer>}, the signature walk's literal
     * mapping at the list grain for the one witnessed class (the number / string / boolean lists
     * stayed green when narrowed away at PR #623's witness sweep and are banked under the #614
     * law). The literal handler renders {@code MapperC.<Integer>of(MapperS.of(0))} with NO stamp,
     * so a type-directed consumer (the default arm's numeric widening) could not see the element
     * it renders; this is the structural re-type the #326 alias re-type and the #362 bare-item
     * re-type perform for their null-typed operands. {@code null} for an empty, mixed or non-int
     * literal.
     */
    public static JavaType listLiteralMapperTypeOrNull(RExpression raw, JavaTypeUtil tu) {
        if (tu == null || !(raw instanceof RListLiteral ll) || ll.elements().isEmpty()) {
            return null;
        }
        JavaClass<?> elem = null;
        for (RExpression e : ll.elements()) {
            JavaClass<?> k = e instanceof RIntLiteral ? tu.INTEGER : null;
            if (k == null || (elem != null && !elem.equals(k))) {
                return null;
            }
            elem = k;
        }
        return tu.wrap(tu.MAPPER_C, elem);
    }

    /**
     * v3.2 seat 2 (Law 2, the chaos C5Deep + C5Preds rows): a PRIMITIVE-typed operand — the
     * count operator's {@code int} (the seat-1 producer stamp, {@code CollectionHandler.handle(
     * RCountExpr)}) — at a seat upstream compiles Mapper-expected ({@code caseToStringOperation}
     * and every comparison operand: {@code MAPPER.wrapExtends(item)}) is lifted by the
     * type-directed coercion service: the item→wrapper arm boxes and wraps, {@code MapperS.of(
     * <chain>.resultCount())}. This retires the comparison seat's node-keyed read (the RAW operand
     * being an {@code RCountExpr} — a {@code then count} chain arrived as an {@code RThenExpr} and
     * slipped through bare) and gives the to-string seat the arm it never had. Every non-primitive
     * operand (wrapper-typed, item-typed, null-typed) returns unchanged — a primitive is the ONE
     * stamp no Mapper local ever carries (the seat-1 gate law at ArithmeticHandler's operand arm),
     * so the lift can never double-wrap. Green-safe by the corpus law: 0 of the 181,200 vendored
     * goldens carry a bare {@code .resultCount().} deref (the seat-2 census § 5), and the
     * 1,947 that carry the wrapped form are the target this reproduces.
     */
    public static JavaStatementBuilder liftPrimitiveOperand(JavaStatementBuilder operand,
            ExpressionCompiler compiler, JavaStatementScope scope) {
        JavaTypeUtil tu = compiler.getTypeUtil();
        TypeCoercionService svc = compiler.getCoercionService();
        // PR #623 (round-1 cq review, N-1): the null-operand and VOID guards, the same set the
        // sibling widenIntegerMapperToBigDecimal carries (the service THROWS on a void source; no
        // producer stamps VOID today - the count's INT is the one primitive that reaches these seats).
        if (operand == null || tu == null || svc == null || scope == null
                || !(operand.getExpressionType() instanceof JavaPrimitiveType prim)
                || prim == JavaPrimitiveType.VOID) {
            return operand;
        }
        return svc.coerce(operand, prim, tu.wrapExtends(tu.MAPPER, prim.toReferenceType()), scope);
    }

    public static final JavaClass<?> MAPPER_MATHS = JavaClass.from(
            com.rosetta.model.lib.expression.MapperMaths.class);
    public static final JavaClass<?> MAPPER_LIST_OF_LISTS = JavaClass.from(
            com.rosetta.model.lib.mapper.MapperListOfLists.class);
    public static final JavaClass<?> CARDINALITY_OPERATOR = JavaClass.from(
            com.rosetta.model.lib.expression.CardinalityOperator.class);
    public static final JavaClass<?> COMPARISON_RESULT = JavaClass.from(
            com.rosetta.model.lib.expression.ComparisonResult.class);
    public static final JavaClass<?> ARRAYS = JavaClass.from(java.util.Arrays.class);
    public static final JavaClass<?> BIG_DECIMAL = JavaClass.from(java.math.BigDecimal.class);
    public static final JavaClass<?> BIG_INTEGER = JavaClass.from(java.math.BigInteger.class);

    /**
     * {@link java.util.Collections} — used by {@code unwrapForAddAssignment}
     * null-fallback (C3a.3). Not yet referenced by any handler; wired in v6.1
     * so the builder-level unwrap can emit {@code Collections.emptyList()}
     * without a string literal.
     */
    public static final JavaClass<?> COLLECTIONS = JavaClass.from(java.util.Collections.class);

    /**
     * {@code java.util.List} — import target for the facet condListCoerce
     * (PR #327) {@code final List<X> ifThenElseResultN;} list-typed conditional
     * hoist declaration (the per-branch single→list coercion of a conditional
     * value consumed at a MULTI seat).
     */
    public static final JavaClass<?> LIST = JavaClass.from(java.util.List.class);

    /**
     * {@code com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe} —
     * static-wildcard import target for the 11 operator methods (areEqual,
     * notEqual, greaterThan, lessThan, greaterThanEquals, lessThanEquals,
     * exists, notExists, onlyExists, singleExists, multipleExists). Carried
     * through the
     * {@link com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder#getStaticWildcardImports()}
     * channel; sole feeder post-C3c.2 (the substring trigger ladder at
     * the deleted {@code FunctionGenerator.collectExpressionImports} was
     * removed atomically).
     */
    public static final JavaClass<?> EXPRESSION_OPERATORS_NULL_SAFE = JavaClass.from(
            com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.class);

    // =========================================================================
    // PR-A §9.1 C3a.4.c (D2 α + D7 η) — ConversionHandler emitted types.
    //
    // Pre-C3c.2 these types were imported via the deleted BUILTIN_TYPE_FQN
    // regex path (4 entries) and — for DateTimeFormatter /
    // DateTimeParseException — were NOT imported at all (latent bug: they
    // fell through resolveWorkspaceTypeFqn and were silently dropped).
    // C3a.4.c wires all 6 to ConversionHandler emission sites as structured
    // refs; post-C3c.2 this is the sole feeder.
    // =========================================================================

    /** {@code java.time.LocalTime} — ConversionHandler TIME arm (D2 α). */
    public static final JavaClass<?> LOCAL_TIME = JavaClass.from(java.time.LocalTime.class);

    /** {@code java.time.LocalDateTime} — ConversionHandler DATE_TIME arm (D2 α). */
    public static final JavaClass<?> LOCAL_DATE_TIME = JavaClass.from(java.time.LocalDateTime.class);

    /** {@code java.time.ZonedDateTime} — ConversionHandler ZONED_DATE_TIME arm (D2 α). */
    public static final JavaClass<?> ZONED_DATE_TIME = JavaClass.from(java.time.ZonedDateTime.class);

    /** {@code com.rosetta.model.lib.records.Date} — ConversionHandler DATE arm (D2 α). */
    public static final JavaClass<?> ROSETTA_DATE = JavaClass.from(
            com.rosetta.model.lib.records.Date.class);

    /**
     * {@code java.time.format.DateTimeFormatter} — ConversionHandler TIME arm (D7 η).
     *
     * <p>Confirmed missing from the pre-C3c.2 regex/substring paths per
     * 2026-04-21 source verification. Without this structured ref, any
     * corpus function using {@code to time} conversion would fail to import
     * the class post-C3c.2. This ref is the sole feeder.
     */
    public static final JavaClass<?> DATE_TIME_FORMATTER = JavaClass.from(
            java.time.format.DateTimeFormatter.class);

    /**
     * {@code java.time.format.DateTimeParseException} — ConversionHandler
     * TIME / DATE / DATE_TIME / ZONED_DATE_TIME arms (D7 η).
     *
     * <p>Same confirmed-missing rationale as {@link #DATE_TIME_FORMATTER}.
     */
    public static final JavaClass<?> DATE_TIME_PARSE_EXCEPTION = JavaClass.from(
            java.time.format.DateTimeParseException.class);

    /**
     * {@code com.rosetta.model.lib.validation.ChoiceRuleValidationMethod} —
     * ExistenceHandler L175/L180/L181 emits
     * {@code ChoiceRuleValidationMethod.REQUIRED} or {@code .OPTIONAL}
     * inline as the third argument to the {@code choice(...)} static method.
     * Pre-C3c.2 this was resolved via the deleted DIRECT_TYPE_REF regex in
     * {@code resolveExpressionTypeImports}; C3a.4.d wired it structurally
     * so C3c.2's regex delete is regression-proof. (D6 ε.)
     */
    public static final JavaClass<?> CHOICE_RULE_VALIDATION_METHOD = JavaClass.from(
            com.rosetta.model.lib.validation.ChoiceRuleValidationMethod.class);

    /**
     * {@code java.util.stream.Collectors} — WrappedItemCoercer L69 emits
     * {@code .stream().map(...).collect(Collectors.toList())} for List-item
     * coercion (D4 γ). Current feeder is the `needsCollectors` template-flag
     * path at FunctionGenerator L628/L644/L652/L691/L704/L735 — that path
     * survives past C3c.2 because it's template-flag-driven not expression-
     * scanned. This ref is defensive for the structured channel.
     */
    public static final JavaClass<?> COLLECTORS = JavaClass.from(java.util.stream.Collectors.class);

    /**
     * {@code java.util.Optional} — facet asKeyReference (PR #328): the as-key
     * meta-key copy form reads the source's meta keys through
     * {@code Optional.ofNullable(<v>).map(r -> r.getMeta()).map(m -> m.get…Key())}
     * (upstream {@code FunctionGenerator.assignValue}'s single-cardinality
     * as-key arm).
     */
    public static final JavaClass<?> OPTIONAL = JavaClass.from(java.util.Optional.class);

    /**
     * {@code java.util.ArrayList} — WrapperToWrapperCoercer L52 + L163 emit
     * {@code new ArrayList<>(...)} when coercing {@code MapperS/MapperC →
     * List} or widening a wildcard-bounded {@code List<? extends T>} into a
     * mutable {@code List<T>}. Pre-C3c.2 relied on the `needsArrayList`
     * template-flag path (set only when {@code outputIsMulti}); after
     * C3c.2 a coercion emitted in an intermediate alias body of a
     * single-valued function would miss the import. Wiring this ref at
     * both emission sites makes the structured channel the sole and
     * complete feeder. (Copilot round 15 finding on WrapperToWrapperCoercer
     * L163 + bug-class sweep for L52.)
     */
    public static final JavaClass<?> ARRAY_LIST = JavaClass.from(java.util.ArrayList.class);

    /**
     * {@code java.util.function.Function} — WrapperToWrapperCoercer L57
     * emits {@code <expr>.map("Make mutable", Function.identity())} when
     * coercing {@code MapperS<? extends T>} to {@code MapperS<T>}. There
     * is no template-flag path for {@code java.util.function.Function},
     * so pre-C3c.2 relied on the deleted {@code DIRECT_TYPE_REF} regex
     * (which matched {@code Function.identity}) plus the
     * {@code resolveWorkspaceTypeFqn} lookup (which was silently failing
     * for {@code java.util.function.Function} — a latent bug). Wiring
     * this ref makes the structured channel the sole feeder.
     * (Bug-class sweep companion to {@link #ARRAY_LIST}.)
     */
    public static final JavaClass<?> FUNCTION = JavaClass.from(java.util.function.Function.class);

    /**
     * Walk the AST parent chain to find the enclosing {@link RFunction}.
     *
     * @param node starting node
     * @return enclosing function, or {@code null} if not found within {@value #CONTAINER_WALK_LIMIT} steps
     */
    public static RFunction findEnclosingFunction(RNode node) {
        RNode cur = node;
        int depth = 0;
        while (cur != null && depth++ < CONTAINER_WALK_LIMIT) {
            if (cur instanceof RFunction func) return func;
            cur = cur.parent();
        }
        return null;
    }

    /**
     * facet aliasStaticImportEscape (PR #420): the {@code ExpressionOperatorsNullSafe}
     * member simple names the function's rendered class will reference through the
     * static wildcard import — the fork analogue of upstream's file-scope taken-name
     * set ({@code GeneratorScope.computeActualNames} resolves every class-member
     * identifier against the file scope, where
     * {@code ImportingStringConcatenation.internalDoStaticImportIfPossible} registered
     * each referenced static member; a colliding alias method then escapes
     * {@code JavaClassScope.escapeName} = {@code "_" + name}). Consulted by BOTH the
     * alias DECLARATION seat ({@code FunctionGenerator.compileAliases}) and the alias
     * INVOCATION seat ({@code ReferenceHandler.disambiguateAliasInvocation}) — one
     * census, decl and call cannot disagree (the member_name_disambiguation lockstep
     * pattern).
     *
     * <p>The node→member mapping restates the render seats' own exhaustive selections
     * (REqualityExpr / RComparisonExpr: {@code ComparisonHandler}'s switches;
     * RExistenceExpr: {@link ExistenceHandler#existenceMethod} consulted DIRECTLY;
     * only-exists → {@code onlyExists}; cardinality-check → {@code choice};
     * contains / disjoint: {@code SetOperationHandler}). KNOWN under-counts (Seat-1
     * #420 OBS-2 — both corpus-absent): the {@code distinct} runtime method
     * (CollectionHandler's RListOpExpr render — no census arm) and the switch
     * literal ladder's {@code areEqual} (emitted with NO REqualityExpr in the
     * tree); any missed member reference under-counts — a missed escape renders
     * the colliding (non-compiling) form the compile gate catches loudly, never a
     * silent green flip. Corpus-neutral by ABSENCE: zero corpus aliases are named
     * as any operator member (grep census, PR #420), so every fire is on
     * non-corpus models.
     */
    public static java.util.Set<String> staticOperatorMembersUsed(RFunction func) {
        if (func == null) {
            return java.util.Set.of();
        }
        java.util.Set<String> memo = OPERATOR_MEMBERS_MEMO.get(func);
        if (memo != null) {
            return memo;
        }
        java.util.Set<String> computed = computeStaticOperatorMembersUsed(func);
        OPERATOR_MEMBERS_MEMO.put(func, computed);
        return computed;
    }

    /**
     * Per-function memo for {@link #staticOperatorMembersUsed} — the census is a pure
     * function of the {@link RFunction}'s expression tree and is consulted once per
     * alias DECLARATION and once per alias INVOCATION render; without the memo the
     * invocation path would re-walk the whole function AST per call site (the
     * O(calls × AST) class Copilot #364 R1 flagged — the
     * {@code COLLISION_NUMBERING_MEMO} pattern, weak keys + synchronized).
     */
    private static final java.util.Map<RFunction, java.util.Set<String>>
            OPERATOR_MEMBERS_MEMO =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * The uncached compute half of {@link #staticOperatorMembersUsed}. Returns an
     * IMMUTABLE set (Copilot #420 R1: the memoized value must not be mutable —
     * a caller mutation would corrupt every later consult). The node guard
     * fails LOUD on exhaustion (Copilot #420 R1: a silent partial census could
     * skip an escape and emit colliding Java with no failure signal — the A2
     * fail-loud law; 100k nodes is far beyond any real function's tree, so the
     * throw is a structural-anomaly trap, not a reachable path).
     */
    private static java.util.Set<String> computeStaticOperatorMembersUsed(RFunction func) {
        java.util.Set<String> used = new java.util.HashSet<>();
        java.util.Deque<RNode> stack = new java.util.ArrayDeque<>();
        stack.push(func);
        int guard = 0;
        while (!stack.isEmpty()) {
            if (guard++ >= 100_000) {
                throw new IllegalStateException(
                        "staticOperatorMembersUsed: function AST walk exceeded 100000 nodes"
                        + " (function '" + func.name() + "') — a partial census could emit"
                        + " a colliding (non-compiling) alias name silently; failing loud"
                        + " instead (Copilot #420 R1)");
            }
            RNode cur = stack.pop();
            if (cur instanceof REqualityExpr eq) {
                used.add(switch (eq.op()) {
                    case EQ  -> "areEqual";
                    case NEQ -> "notEqual";
                });
            } else if (cur instanceof RComparisonExpr cmp) {
                used.add(switch (cmp.op()) {
                    case LT  -> "lessThan";
                    case GT  -> "greaterThan";
                    case LTE -> "lessThanEquals";
                    case GTE -> "greaterThanEquals";
                });
            } else if (cur instanceof RExistenceExpr ex) {
                used.add(ExistenceHandler.existenceMethod(ex));
            } else if (cur instanceof ROnlyExistsExpr) {
                used.add("onlyExists");
            } else if (cur instanceof com.regnosys.rosetta.ast.expressions.unary
                    .RCardinalityCheckExpr) {
                used.add("choice");
            } else if (cur instanceof RContainsExpr) {
                used.add("contains");
            } else if (cur instanceof RDisjointExpr) {
                used.add("disjoint");
            }
            for (RNode child : cur.children()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
        return java.util.Set.copyOf(used);
    }

    /**
     * facet dispatchVariantParamResolution (PR #369): the dispatch BASE declaration of a
     * dispatch VARIANT function, or {@code null} when {@code fn} is not a variant or no
     * base is declared in the same model file.
     *
     * <p>A dispatch variant ({@code func Name(param: Enum -> Value):}) declares NO inputs
     * of its own — the AstBuilder gives it the single {@code __synthesized_input__}
     * placeholder — and its body references the BASE declaration's inputs by name
     * ({@code resetDates}, {@code periodsInYear}, …). Every expression-side consumer that
     * scans the enclosing function's declared inputs (alias-call argument forwarding,
     * disguised-navigation head resolution, the receiver-element walk, the alias-signature
     * type walk) must resolve those references against the BASE, mirroring
     * {@code FunctionGenerator}'s Phase-1 grouping ({@code dispatchBases}) that already
     * feeds the SIGNATURE side via {@code signatureSource}.
     *
     * <p>The base is located by scanning the variant's own {@link com.regnosys.rosetta.ast.model.RModel}'s
     * root elements for the same-named {@link RFunction} with no {@code dispatch()} —
     * dispatch groups are co-located in one model file (all four corpus groups:
     * YearFraction + DayCountBasis in base-datetime-daycount-func,
     * ComputeCalculationPeriod in observable-asset-calculatedrate-func,
     * ProcessFloatingRateReset in product-asset-floatingrate-func). A cross-file base —
     * none in the corpus — returns {@code null}, keeping the pre-facet bytes (decline,
     * stays waivered). Green-safe by construction: non-null ONLY for dispatch variants,
     * and the sole green dispatch function (DayCountBasis) has literal-only variant
     * bodies that exercise none of the consumer seats.
     */
    public static RFunction dispatchBaseOf(RFunction fn) {
        if (fn == null || fn.dispatch().isEmpty() || fn.name() == null) {
            return null;
        }
        RNode cur = fn.parent();
        int depth = 0;
        while (cur != null && depth++ < CONTAINER_WALK_LIMIT) {
            if (cur instanceof com.regnosys.rosetta.ast.model.RModel model) {
                for (var elt : model.rootElements()) {
                    if (elt instanceof RFunction cand && cand.dispatch().isEmpty()
                            && fn.name().equals(cand.name())) {
                        return cand;
                    }
                }
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * facet dispatchVariantParamResolution (PR #369): resolve {@code name} as a declared
     * INPUT of the dispatch BASE when {@code node} sits inside a dispatch VARIANT body.
     * {@code null} for every non-variant seat (the universal fast path), for an
     * unresolvable base, and for a name that is not a base input — consumers keep their
     * pre-facet declines.
     */
    public static RAttribute dispatchBaseInput(RNode node, String name) {
        if (name == null) {
            return null;
        }
        RFunction base = dispatchBaseOf(findEnclosingFunction(node));
        if (base == null) {
            return null;
        }
        for (RAttribute input : base.inputs()) {
            if (name.equals(input.name())) {
                return input;
            }
        }
        return null;
    }

    /**
     * facet onlyElementMapperSRoundTrip (PR #345): true when {@code e} is a THEN-CHAIN
     * whose LAST body is an {@code only-element} collapse over a PROVEN-MULTI operand
     * (a {@code MapperC} in render-truth) — the hoisted-thenArg TAIL render
     * ({@code … filter exists then only-element} / {@code … then distinct only-element}).
     * Golden re-presents THAT collapse as {@code MapperS.of(<mc>.get())} before the
     * value-consumption deref (the identity round-trip: {@code evaluate(MapperS.of(
     * thenArg.get()).get())} GetEventDate, {@code .setX(MapperS.of(distinct(thenArg1)
     * .get()).get())} MapGenericProductEconomicTerms). The THEN-TAIL shape is
     * load-bearing: a DIRECT nav-chain collapse in the same seats stays BARE in golden
     * ({@code evaluate(item.<Trade>map(…).<TradeLot>mapC(…).get(), …)} — the
     * QuantityIncreased green form, the #345 cp1 over-fire catch), so a bare
     * {@link RListOpExpr} never satisfies this predicate; the extract-RECEIVER seat,
     * where a direct collapse cannot compile unwrapped, carries its own direct-shape
     * gate at the call site. The monotone multi-proof
     * ({@link NavigationHandler#chainProvesMulti}) keeps the 859 green
     * MapperS-operand {@code evaluate(thenArg.get())} forms declined.
     */
    public static boolean collapsedMultiOnlyElementThenTail(RExpression e, ExpressionCompiler compiler) {
        if (!(e instanceof RThenExpr then)) {
            return false;
        }
        RInlineFunction body = then.body().orElse(null);
        return body != null
                && body.body() instanceof RListOpExpr op
                && op.op() == ListOp.ONLY_ELEMENT
                && NavigationHandler.chainProvesMulti(op.argument(), compiler);
    }

    /**
     * The {@link RListLiteral} that a SINGLE-collapsing list-op operates on
     * ({@code [<a>, <b>] only-element}), or {@code null} for every other shape.
     *
     * <p>ONE shape predicate, TWO consumers (LAW 69), each passing its OWN operator
     * set so that neither seat widens into the other:
     * <ul>
     *   <li>facet listLiteralNavMetaDeref (PR #337, part 2) - the value-returning ladder
     *       rung in {@code CollectionHandler}, which admits {@code ONLY_ELEMENT} alone and
     *       hands the literal to {@code LiteralHandler.listLiteralNavMetaWrapper};</li>
     *   <li>facet toStringListLiteralCollapseRewrap (v3.1 flip seat 32, law D.3) - the
     *       {@code to-string} re-wrap gate in {@code ConversionHandler}, which admits
     *       {@code ONLY_ELEMENT} / {@code FIRST} / {@code LAST}.</li>
     * </ul>
     *
     * <p>The EMPTY literal declines HERE rather than at each consumer, and hoisting that
     * decline is byte-neutral at the pre-existing seat by construction:
     * {@code listLiteralNavMetaWrapper} already returns {@code null} on
     * {@code elements().isEmpty()}, so a {@code [] only-element} rung reached the very
     * same {@code navMeta == null} arm before this predicate existed.
     *
     * @param e   the candidate expression - the COLLAPSE node, not the literal
     * @param ops the collapse operators the calling seat admits
     */
    public static RListLiteral listLiteralCollapseArgument(RExpression e, ListOp... ops) {
        if (!(e instanceof RListOpExpr collapse)
                || !(collapse.argument() instanceof RListLiteral literal)
                || literal.elements().isEmpty()) {
            return null;
        }
        for (ListOp admitted : ops) {
            if (collapse.op() == admitted) {
                return literal;
            }
        }
        return null;
    }

    /**
     * facet nullResultArmElision (PR #345): true when EVERY level of a nested-else
     * conditional-ladder TAIL has an {@code empty} then-arm and the tail terminates
     * in an absent / synthetic-empty-list / explicit-{@code empty} else — the whole
     * run produces exactly the {@code <target> = null} the final fall-through else
     * emits, so golden elides the arms ({@code if C then empty else empty ≡ empty};
     * FinancialUnitToISO20022UnitOfMeasure drr: the trailing ValuePerDay /
     * ValuePerPercent {@code then empty} cases fold into {@code } else { result =
     * null; }} while the NON-trailing first empty arm stays explicit). Any level
     * with a non-{@code empty} then or a real terminal else value declines (the
     * explicit chain stays — today's bytes). Consumed by BOTH ladder renderers
     * (ControlFlowHandler's hoist block + FunctionExpressionRenderer's
     * whole-output conditional SET).
     */
    public static boolean trailingRunElidesToNull(RConditionalExpr cond) {
        RConditionalExpr cur = cond;
        while (true) {
            if (!(cur.thenBranch() instanceof
                    com.regnosys.rosetta.ast.expressions.references.REmptyLiteral)) {
                return false;
            }
            java.util.Optional<RExpression> els = cur.elseBranch();
            if (els.isEmpty()
                    || (els.get() instanceof RListLiteral list && list.elements().isEmpty())
                    || els.get() instanceof
                            com.regnosys.rosetta.ast.expressions.references.REmptyLiteral) {
                return true;
            }
            if (els.get() instanceof RConditionalExpr next) {
                cur = next;
                continue;
            }
            return false;
        }
    }

    /**
     * Walk the AST parent chain to find the enclosing {@link RRule}.
     *
     * <p>Rule bodies are NOT reparented onto the synthetic {@link RFunction}
     * built by {@code RFunction.fromRule} — the {@code fromRule} operation only
     * holds a reference to the rule's expression, so a rule body expression's
     * parent chain reaches the {@link RRule}, never an {@link RFunction}
     * (confirmed against {@code TypeInferenceEngine.getEnclosingItemType}). This
     * is therefore the rule-body analogue of {@link #findEnclosingFunction}: the
     * two are mutually exclusive on any given expression — a function-body
     * expression yields a non-null {@code findEnclosingFunction} and a null
     * {@code findEnclosingRule}, and vice-versa for a rule body.
     *
     * @param node starting node
     * @return enclosing rule, or {@code null} if not found within {@value #CONTAINER_WALK_LIMIT} steps
     */
    public static RRule findEnclosingRule(RNode node) {
        RNode cur = node;
        int depth = 0;
        while (cur != null && depth++ < CONTAINER_WALK_LIMIT) {
            if (cur instanceof RRule rule) return rule;
            cur = cur.parent();
        }
        return null;
    }

    /**
     * v3.2 seat 12 (D52, H1 - M12 the duplicate closure parameter): the Java names of a REDUCE lambda's two
     * parameters. Upstream ({@code GeneratorScope.computeActualNames}) numbers EVERY holder of a shared desired
     * name in one scope and binds the body's reads to the FIRST - {@code reduce a, a [ a + a ]} renders
     * {@code (a0, a1) -> add(a0, a0)} (the chaos s95 golden; the hold-out group {@code closure-param-duplicate}
     * pinned from the released plugin before this code) - where the fork wrote the raw names {@code (a, a)}, a
     * duplicate lambda parameter that does not compile. ONE predicate for both halves (LAW 69):
     * {@code CollectionHandler.compileReduceLambda} writes the pair through this method, and every READ of the name
     * renders through {@link #reduceParamReadName} - {@code ReferenceHandler}'s reduce arm (a bare read) and, since
     * round 1, {@code NavigationHandler}'s closure-parameter RECEIVER seat (a navigation off the parameter): THREE
     * consumers, one channel. Two DISTINCT names stay raw (the control {@code reduce a, b}; every vendored reduce - the corpus carries none). The
     * numbering is per LAMBDA scope (the desired-name index restarts in each reduce), which is all the M12 row
     * and the oracle group reach; a reduce parameter that shares its name with a name of an ENCLOSING scope is
     * not numbered here (upstream would - no carrier prices it; banked).
     *
     * @param func the reduce's inline function (two declared parameters; a missing one falls to the legacy
     *             {@code a} / {@code b} defaults of the caller). REDUCE OWNERS ONLY, by its two callers -
     *             {@code CollectionHandler.compileReduceLambda}, reached from {@code handle(RReduceExpr)} with the
     *             reduce's own body, and {@link #reduceParamReadName} behind its own
     *             {@code func.parent() instanceof RReduceExpr} gate; any further caller must carry that gate,
     *             or the pair it writes is numbered where no read is (round 3, cq NIT-7; round 4, cq NIT-7)
     * @return {@code {first, second}} - the Java identifiers to write
     */
    public static String[] reduceParamRenderNames(RInlineFunction func) {
        List<String> params = func == null ? List.of() : func.paramNames();
        String p1 = params.size() > 0 ? params.get(0) : "a";
        String p2 = params.size() > 1 ? params.get(1) : "b";
        if (params.size() > 1 && p1.equals(p2)) {
            return new String[] {p1 + "0", p2 + "1"};
        }
        return new String[] {p1, p2};
    }

    /**
     * v3.2 seat 12 (D52, H1): the Java identifier a READ of a reduce parameter renders - the FIRST parameter's
     * numbered name when the two share the read name (upstream's binding law: every read of the shared name binds
     * to the first holder), the raw name otherwise. The other half of {@link #reduceParamRenderNames}; read by the
     * bare-read arm of {@code ReferenceHandler} and by the closure-parameter receiver seat of
     * {@code NavigationHandler} (round 1, cq MF-1). THE GATE IS THE PREDICATE'S OWN (round 2, cq SF-1): a name is
     * numbered only when its owner is a REDUCE's inline function. The fork's front end admits N closure parameters
     * on EVERY inline function ({@code extract a, a [ ... ]} - invalid upstream: the vendored 9.83.0 validator's
     * {@code RosettaSimpleValidator.checkOptionalNamedParameter} reports "Function must have 1 named parameter.";
     * no fork validator counts them), and
     * before this gate the receiver seat alone would have numbered such a non-reduce owner's receiver ({@code a0.})
     * while the lambda declared {@code a} - so every consumer meets ONE gate here, never one of its own (the
     * bare-read arm's outer {@code RReduceExpr} test selects the ARM, not the law); the seat suite's h1x control
     * pins the non-reduce owner raw at the receiver, lane L12 proves this gate able to fail.
     */
    public static String reduceParamReadName(RInlineFunction owner, String name) {
        if (owner == null || name == null || !(owner.parent() instanceof RReduceExpr)) {
            return name;
        }
        List<String> params = owner.paramNames();
        if (params.size() > 1 && params.get(0).equals(params.get(1)) && name.equals(params.get(0))) {
            return reduceParamRenderNames(owner)[0];
        }
        return name;
    }

    /**
     * v3.1 flip seat 21 (facet {@code ruleReceiverRecordFeature}, LAW 69): THE ONE source of a
     * reporting RULE's output type — the workspace type inference over the rule's body
     * expression. A rule's output is inference-derived, never declared on the node (the
     * synthetic {@code RFunction.fromRule} output attribute carries NO typeCall), and
     * {@code RuleGenerator} types the generated {@code <Name>Rule.evaluate()} from this same
     * read. Before this seat the read was written EIGHT times with eight local filters
     * ({@code NavigationHandler.resolveReceiverOutputType} → data type,
     * {@code ComparisonHandler.ruleOutputEnumeration} → enum, {@code ControlFlowHandler}'s
     * inner-rule ITE-hoist arm → Java ref, {@code FunctionAliasHelper}'s aliasRuleRefTyping
     * → alias signature, {@code CollectionHandler.enclosingOutputEnumeration} → enum (the
     * enclosing-rule twin of ComparisonHandler's), {@code NavigationHandler.callableChoiceOutputBridge}
     * → choice bridge, {@code FunctionExpressionRenderer.renderReportRuleSetOrNull} → the report
     * SET item type — all SEVEN re-pointed here, each keeping its own post-filter (the last three at
     * the seat-21 review, which found them un-re-pointed behind a "five times" count); plus
     * {@code RuleGenerator} → the authority, which THROWS on a missing inference and is
     * deliberately NOT re-pointed) and ZERO times at the
     * receiver-typing walk {@code NavigationHandler.resolveReceiverRType} — so a
     * {@code zonedDateTime}-returning rule used as a navigation RECEIVER
     * ({@code ExecutionTimestamp(transaction) -> date}, {@code extract ValuationTimestamp -> date})
     * was untyped there and the {@code date} record feature fell to the non-compiling getter
     * path. Every consumer now reads this helper and applies its OWN post-filter.
     *
     * @param rule the reporting rule (non-null)
     * @param gm   the generator model whose workspace holds the inference (a {@code null}
     *             model / workspace yields {@code null} — the stateless unit compiler)
     * @return the rule's inferred output {@link RType} (aliases NOT stripped — each consumer
     *         decides), or {@code null} when the rule has no body or the inference is absent or
     *         MISSING
     */
    public static RType ruleInferredOutputRType(RRule rule, GeneratorModel gm) {
        if (rule == null || gm == null || gm.workspace() == null) {
            return null;
        }
        RExpression body = rule.expression().orElse(null);
        if (body == null) {
            return null;
        }
        RMetaAnnotatedType inferred = gm.workspace().getInferredType(body);
        if (inferred == null || inferred.isMissing()) {
            return null;
        }
        return inferred.type();
    }

    /**
     * v3.1 flip seat 21 (facet {@code metaFaceShortForm}): the seven metadata FACE names a
     * receiver can expose as a feature ({@code annotations.rosetta}: {@code id}, {@code key},
     * {@code reference}, {@code scheme}, {@code template}, {@code location}, {@code address}).
     * A name test only — it GATES the bare-name synthesis in {@code ReferenceHandler}; the
     * ADMISSION of a face read is the parser's {@link #boundMetaType} binding (upstream links the
     * feature to a {@code RosettaMetaType}), or — for {@code scheme}/{@code reference} ONLY — the
     * #285 implicit-item wrapper proof; never this list on its own.
     */
    public static boolean isMetaFeatureName(String name) {
        if (name == null) {
            return false;
        }
        return switch (name) {
            case "id", "key", "reference", "scheme", "template", "location", "address" -> true;
            default -> false;
        };
    }

    /**
     * v3.1 flip seat 21: THE ONE meta-face → POJO property-name table — a port of upstream
     * {@code PojoPropertyUtil.toPojoPropertyName} ({@code reference} → {@code externalReference},
     * {@code id}/{@code key} → {@code externalKey}, {@code address} → {@code reference},
     * {@code location} → {@code scopedKey}, every other name itself). Before this seat the table
     * was written FOUR times — three incomplete (the nav short form's implicit two-case switch,
     * {@code ConstructionHandler.pojoMetaSetterProp} (key/id/scheme), {@code FunctionExpressionRenderer}'s
     * override-setter rename (key/id, reference special-cased for its setter SHAPE)) and ONE complete
     * exact mirror ({@code ConstructionHandler.toPojoPropertyName}, the with-meta constructor setter
     * seat — found by the seat-21 review behind a "three times" count); all four now read THIS table
     * (the mirror retired: LAW 69 — consult, do not mirror). A {@code null} name passes through
     * as {@code null}; a consumer that cannot name a setter from it must refuse loudly
     * ({@code ConstructionHandler.metaSetterName}).
     */
    public static String metaPojoPropertyName(String rosettaName) {
        if (rosettaName == null) {
            return null;
        }
        return switch (rosettaName) {
            case "reference" -> "externalReference";
            case "id", "key" -> "externalKey";
            case "address" -> "reference";
            case "location" -> "scopedKey";
            default -> rosettaName;
        };
    }

    /**
     * v3.1 flip seat 21: the parser's metadata-face binding on a feature node — the exported
     * {@code metaType <name>} root element the type-inference engine bound the read to
     * ({@code TypeInferenceEngine}: an {@code RFeatureCall} / two-hop {@code REnumValueRef} whose
     * leaf names a face the receiver attribute (or its declared type's type-level annotation)
     * carries; the {@code RSymbolReference} slot for a bare name) — or {@code null}. The
     * generator's one read of that channel (it had ZERO before this seat).
     */
    public static com.regnosys.rosetta.ast.types.RMetaType boundMetaType(RNode node) {
        java.util.Optional<? extends RNode> slot;
        if (node instanceof RFeatureCall fc) {
            slot = fc.resolvedFeatureNode();
        } else if (node instanceof REnumValueRef evr) {
            slot = evr.resolvedFeatureNode();
        } else if (node instanceof RSymbolReference sr) {
            // a BARE face name (`min [ key ]`): the linker's global pass binds the SYMBOL itself
            // to the exported metaType root element (the seat-21 a2 fixture: symbol=RMetaType,
            // resolvedFeatureNode empty) — upstream's RosettaSymbolReference → RosettaMetaType arm
            if (sr.symbol().orElse(null) instanceof com.regnosys.rosetta.ast.types.RMetaType bareMeta) {
                return bareMeta;
            }
            slot = sr.resolvedFeatureNode();
        } else {
            return null;
        }
        return slot.filter(com.regnosys.rosetta.ast.types.RMetaType.class::isInstance)
                .map(com.regnosys.rosetta.ast.types.RMetaType.class::cast)
                .orElse(null);
    }

    /**
     * Coverage wave D (datarule): the enclosing TYPE condition of {@code node} —
     * an {@link RCondition} whose parent is an {@link RDataType} or
     * {@link RChoice} — or {@code null} when the node does not sit inside one.
     *
     * <p>Discriminates the datarule compile context from every byte-proven
     * kind's expression context: a {@code func} pre/post condition's
     * {@link RCondition} parents at the {@link RFunction} (the walk aborts
     * there), and rule/report/function/alias bodies never sit under an
     * {@link RCondition} at all — so this probe returns {@code null} for every
     * expression the FUNCTION/RULE/POJO emission paths compile, by
     * construction. The datarule generator compiles condition expressions
     * whose parent chain reaches the declaring {@link RDataType}/{@link RChoice}
     * through the condition node; consumers use this to bind the implicit
     * instance (the {@code executeDataRule} parameter) exactly where upstream's
     * {@code ImplicitVariableUtil} binds the condition's implicit variable.
     *
     * @param node starting node
     * @return the enclosing type-parented condition, or {@code null}
     */
    public static RCondition findEnclosingTypeCondition(RNode node) {
        RNode cur = node;
        int depth = 0;
        while (cur != null && depth++ < CONTAINER_WALK_LIMIT) {
            if (cur instanceof RCondition condition) {
                // v3.2 seat 3 (F9): a typeAlias is a condition owner too — upstream's
                // ImplicitVariableUtil binds the implicit item for every
                // RosettaTypeWithConditions (Data, choice AND alias); the alias's condition body
                // is a data-rule body exactly like a Data type's. The owner kinds are read at ONE
                // place, conditionOwner (LAW 69; round-1 cq N-3). This predicate is consulted at
                // 23 call sites over nine handlers, and every one now admits an alias owner —
                // the carriers reach the two ReferenceHandler arms and the numeric-kind arm
                // below; the rest (list ops, conditionals, existence tests, set operations
                // inside an alias condition) have no corpus, chaos or fixture carrier and are
                // banked as gen-2 seed shapes (the seat plan's banked list).
                return conditionOwner(condition) != null ? condition : null;
            }
            if (cur instanceof RFunction || cur instanceof RRule) {
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * v3.2 seat 3 (F9), the ONE owner read (LAW 69; round-1 cq N-2 / N-3): the type that OWNS
     * {@code condition} — an {@link RDataType} or an {@link RTypeAlias} — else {@code null} (a
     * function pre/post condition, a rule, or no condition at all). Every consumer that asks
     * "is this a type condition?" or "is it alias-owned?" consults this, never its own
     * {@code instanceof} ladder: {@link #findEnclosingTypeCondition}, {@link #aliasOwner},
     * {@link #conditionOwnerName}, the numeric-kind arm, the data-rule generator's two seats.
     * A {@code choice} declares NO conditions in the fork's AST ({@code RChoice} carries options,
     * annotations and synonyms only; its one-of is SYNTHESISED by the data-rule generator with a
     * null condition), so a choice arm here would be unreachable and is not written.
     */
    public static RRootElement conditionOwner(RCondition condition) {
        RNode owner = condition == null ? null : condition.parent();
        return (owner instanceof RDataType || owner instanceof RTypeAlias) ? (RRootElement) owner : null;
    }

    /** The {@code typeAlias} that owns {@code condition}, else {@code null} — {@link #conditionOwner} narrowed. */
    public static RTypeAlias aliasOwner(RCondition condition) {
        return conditionOwner(condition) instanceof RTypeAlias alias ? alias : null;
    }

    /**
     * v3.2 seat 13 (D53, site R7): the resolved Java type of a {@code typeAlias} condition's implicit {@code item}
     * — the alias's own type call resolved transitively and translated (`number(fractionalDigits: 0)` is
     * {@code Integer}, `number(min: 0)` is {@code BigDecimal}; the same translator read the datarule class's subject
     * type takes). The engine leaves this implicit MISSING (Category 8 types the literal {@code item} only inside
     * lambdas and rule bodies), so every consumer that needs the item's kind reads it HERE: the literal-widening arm
     * of {@link #numericOperandKind} and the evaluate-arg plan ({@code ReferenceHandler.planArgCoercion}). {@code null}
     * unless {@code operand} is the implicit variable of a condition a {@code typeAlias} owns (a Data owner's
     * implicit is a model instance, not an alias value) and the generator model + translator are wired.
     */
    public static JavaType aliasConditionItemJavaType(RExpression operand, ExpressionCompiler compiler) {
        if (!(operand instanceof RImplicitVariable) || compiler == null || compiler.getGeneratorModel() == null
                || compiler.getTypeTranslator() == null) {
            return null;
        }
        RTypeAlias ownerAlias = aliasOwner(findEnclosingTypeCondition(operand));
        if (ownerAlias == null) {
            return null;
        }
        return compiler.getTypeTranslator().toJavaReferenceType(
                compiler.getGeneratorModel().resolveTypeCall(ownerAlias.typeCall()));
    }

    /**
     * v3.2 seat 3 (F9): the Rune name of a type condition's OWNER — upstream reads
     * {@code condition.getEnclosingType().getName()} whatever the owner; the instance name is
     * {@link #conditionInstanceName} of it. {@code null} when {@link #conditionOwner} is.
     */
    public static String conditionOwnerName(RCondition condition) {
        RRootElement owner = conditionOwner(condition);
        return owner == null ? null : ConditionCases.ownerName(owner);
    }

    /**
     * facet sumTypedFunctionPath (PR #328, F3): the enclosing ALIAS
     * ({@link RShortcut}) of {@code node}, or {@code null} when the node does
     * not sit inside an alias body. The container-walk twin of
     * {@link #findEnclosingRule} (same {@link #CONTAINER_WALK_LIMIT} bound —
     * the walk terminates at the AST root; the bound only guards cycles).
     */
    public static RShortcut findEnclosingShortcut(RNode node) {
        RNode cur = node;
        int depth = 0;
        while (cur != null && depth++ < CONTAINER_WALK_LIMIT) {
            if (cur instanceof RShortcut shortcut) return shortcut;
            cur = cur.parent();
        }
        return null;
    }

    /**
     * facet ctorSetterMetaDerefLambda (PR #312, promoted here at PR #314): true iff {@code node}
     * sits DIRECTLY in a DRAINABLE map/extract lambda body — the NEAREST enclosing
     * {@link RInlineFunction} is a map/extract lambda (its parent is an {@link RExtractExpr}, the
     * {@code mapItem}/{@code mapSingleToItem} seat that {@code CollectionHandler.compileLambda}
     * drains pending lambda hoists for) AND no {@link RConditionalExpr} sits between {@code node}
     * and that lambda. Walks UP the parent chain (the node's own argument lambdas are children,
     * never on this path), stopping at the enclosing rule/function root. A closer non-map inline
     * function (filter predicate / sort comparator / reduce) returns false so a
     * {@code registerPendingLambdaHoist} decl is never orphaned.
     *
     * <p>The conditional-arm exclusion is load-bearing (the #312 regscan caught the over-fire): a
     * pending lambda hoist inside a conditional arm makes {@code compileLambda}'s conditional-block
     * form DECLINE to the inline ternary (its {@code !hasPendingLambdaHoists()} gate), moving a
     * still-divergent co-occupied file AWAY from golden.
     */
    public static boolean isInsideDrainableMapLambda(RNode node) {
        RNode cur = node == null ? null : node.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RConditionalExpr) {
                return false;
            }
            if (cur instanceof RInlineFunction inline) {
                return inline.parent() instanceof RExtractExpr;
            }
            if (cur instanceof RFunction || cur instanceof RRule) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * facet deepThenSentinelOperandTyping (PR #389): the render-truth ITEM type of an
     * IMPLICIT-item operand whose OWN then-step binding carries a typed Mapper — the
     * binding IS what the implicit compiles to (ReferenceHandler's thenArgRefFor
     * re-root), so the selection and the render cannot disagree (the walk-render
     * alignment law; golden cdm6 StandardizedScheduleVarianceSwapNotionalAmount: the
     * #351 consumer re-roots {@code item} on the MapperS&lt;BigDecimal&gt;-typed
     * ifThenElseResult sentinel, and the arith/comparison consumers must see
     * BigDecimal to keep the sentinel BARE / wrap the int literal
     * {@code BigDecimal.valueOf(1)}). Consulted ONLY when the workspace snapshot is
     * MISSING — the engine's resolved verdicts (including the definitive non-numeric
     * decline) stay untouched. Scope-dependent BY DESIGN: deliberately not folded
     * into {@link #numericOperandKind}, whose scope-less mirror
     * ({@code ArithmeticHandler.binaryResultItemJavaClass}) must stay in lockstep.
     */
    public static JavaType implicitThenBindingItemType(RExpression operand,
            com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope,
            ExpressionCompiler compiler) {
        if (!(operand instanceof RImplicitVariable) || scope == null || compiler == null
                || compiler.getTypeUtil() == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm != null && gm.workspace() != null) {
            RMetaAnnotatedType inferred = gm.workspace().getInferredType(operand);
            if (inferred != null && !inferred.isMissing()) {
                return null;
            }
        }
        RInlineFunction boundary = null;
        RNode cur = operand.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                boundary = inline;
                break;
            }
            if (cur instanceof RFunction || cur instanceof RRule) {
                return null;
            }
            cur = cur.parent();
        }
        // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — VERDICT-MOVED
        // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar; S29): DORMANT PAST ITS GATE, and
        // the zero IS attributable. c9 census, 270 arrivals (90 / 90 / 90) = 135 entry + 135
        // workspace declines + ZERO reads — the gate above turns away 135/135 (100.00%), with the
        // workspace answering missing=false / hasMeta=false / RStringType at 270/270 over three
        // carriers, so this binding fallback is NEVER REACHED at this corpus. That is a
        // measurement, not a gap: the row's proposed channel is the one the javadoc says is
        // consulted FIRST, so retiring the row means making the FRONT END type the #351 sentinel —
        // a parser-side change, not a swap. DECLINE-ONLY, and the positive polarity is not drawable
        // at the corpus (recorded under LAW 72, with ThenBindingOverlaySeatTest.d1 as the decline
        // lock).
        JavaExpression bound = boundary == null ? null : scope.thenArgRefFor(boundary);
        JavaType boundType = bound == null ? null : bound.getExpressionType();
        return boundType == null ? null : compiler.getTypeUtil().getItemType(boundType);
    }

    /**
     * facet deepThenLevelElementPreserve (PR #362): the RENDER-TRUE meta element of a BARE
     * implicit-item reference inside a then-piped extract lambda — read through the scope's
     * thenArg BINDING (a compiled-type channel, never an AST-walk recovery: the #361 cp6
     * law). The item's nearest enclosing {@link RInlineFunction} must be an extract lambda
     * whose receiver is the piped implicit variable of a then-step body whose binding
     * ({@code JavaStatementScope.thenArgRefFor}) carries a Mapper whose ITEM is an
     * {@link com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue} — i.e. the
     * deep-then hoist compiled the level receiver and its decl element is the wrapper
     * (golden drr DTCC_TradeParty1/2ReportingDestination: {@code thenArg3} is
     * {@code MapperC<FieldWithMetaSupervisoryBodyEnum>}, so the {@code mapItem} lambda's
     * {@code item} IS the wrapper — its comparison consumers deref
     * {@code .<SupervisoryBodyEnum>map("Type coercion", …)} and the identity-arm block's
     * {@code ofNull} follows the wrapper). Returns {@code null} for every other shape —
     * a non-implicit operand, a non-extract lambda, an unbound then, or a bare-element
     * binding — declining to today's bytes.
     */
    public static com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue
            bareItemThenPipeMetaType(RExpression operand,
                    com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope,
                    ExpressionCompiler compiler) {
        // v3.2 seat 13 (D53, site R12 - the chaos C29InLambda rows, the seat-5 InLambda edge): the lambda's EXPLICIT
        // parameter names the same value as its implicit item (`then filter c [ c <> "void" ]` - `c` IS the piped
        // item), so a symbol reference bound to a closure parameter of the NEAREST enclosing inline function is
        // admitted beside the implicit item; the binding read below is unchanged (LAW 69 - one read, its admission
        // widened by the AST shape that names the same value). MEASURED before the cut: `c` compiles with a NULL
        // type and the front end types it `string` with hasMeta=false (probe-r12.log), so this binding is the ONE
        // channel that carries the deep path's wrapper to the comparison seat's meta-strip.
        RSymbolReference explicitParam = operand instanceof RSymbolReference sr
                && RInlineFunction.isClosureParameterBinding(sr.symbol().orElse(null)) ? sr : null;
        if ((!(operand instanceof RImplicitVariable) && explicitParam == null) || scope == null
                || compiler == null || compiler.getTypeUtil() == null) {
            return null;
        }
        RInlineFunction itemFn = null;
        RNode cur = operand.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                itemFn = inline;
                break;
            }
            if (cur instanceof RFunction || cur instanceof RRule) {
                return null;
            }
            cur = cur.parent();
        }
        if (explicitParam != null && (itemFn == null || RInlineFunction
                .declaringLambdaOf(explicitParam.symbol().orElse(null), explicitParam.name()).orElse(null) != itemFn)) {
            // an outer lambda's parameter read from an inner lambda is NOT this lambda's item - decline
            return null;
        }
        RExpression extArg;
        RNode pipeOwner;
        if (itemFn != null && itemFn.parent() instanceof RExtractExpr ext) {
            // facet iteChainArmThenHoist (PR #375, A4): an ELEMENT-PRESERVING collapse
            // (first/last/distinct) between the piped implicit and the extract keeps the
            // binding's element (MapperC<X>.first() yields the same X) — golden
            // Create_AnnaDsbUpiRequestUnderlyingForRate's consumer `thenArg.first()
            // .mapSingleToItem(item -> item.<ProductIdentifier>map("Type coercion", …)…)`
            // derefs the wrapper item exactly like the direct-pipe shape. Any other
            // receiver shape keeps the decline.
            extArg = ext.argument();
            if (extArg instanceof RListOpExpr extCollapse
                    && (extCollapse.op() == ListOp.FIRST || extCollapse.op() == ListOp.LAST
                            || extCollapse.op() == ListOp.DISTINCT)) {
                extArg = extCollapse.argument();
            }
            pipeOwner = ext;
        } else if (itemFn != null && itemFn.parent() instanceof RFilterExpr filt) {
            // facet seqThenChainDecomp (PR #380, D4): the FILTER-predicate sibling of the
            // extract arm — a filter is element-PRESERVING (the #329 M5 law), so the
            // predicate's item IS the binding's wrapper element (golden iosco cde v1
            // Counterparty2IdentifierTypeRule: `thenArg0.filterItemNullSafe(item ->
            // areEqual(item.<Party>map("Type coercion", …), …))` — both areEqual operands
            // deref). Same binding-channel read; a non-implicit filter receiver declines.
            extArg = filt.argument();
            pipeOwner = filt;
        } else {
            return null;
        }
        if (!(extArg instanceof RImplicitVariable)) {
            return null;
        }
        RInlineFunction thenFn = null;
        RNode up = pipeOwner.parent();
        int d2 = 0;
        while (up != null && d2++ < PARENT_WALK_LIMIT) {
            if (up instanceof RInlineFunction inline) {
                thenFn = inline;
                break;
            }
            if (up instanceof RFunction || up instanceof RRule) {
                return null;
            }
            up = up.parent();
        }
        if (thenFn == null || !(thenFn.parent() instanceof RThenExpr)) {
            return null;
        }
        // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — VERDICT-MOVED
        // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar; S30), a LAW-77 seat: BOTH
        // proposed channels fail on the same measurement on all three walks. c9 census, 133,847
        // arrivals (55,248 / 26,676 / 51,923) = 90,039 entry + 43,808 read. CHANNEL 1, the PR #613
        // family-6 NavigationHandler.recoverExprMetaWrapper: recover=null at 43,808/43,808 on EVERY
        // walk — it never answers. CHANNEL 2, the workspace: hasMeta=false at all eight distinct
        // signatures, 133,847/133,847, with missing=true at 73,198 besides. Producer recovery
        // 43,764/43,808 (99.90%) over SIX NAMED SITES plus 44 nokey — the widest producer spread in
        // the family, grounding the seatmap's unverified "seven consumers" as 23 distinct caller
        // frames. LAW 77: the route asymmetry is a CALLER-POPULATION one (the IR route reaches 706
        // distinct carriers where the default route reaches 1,086) and it is BENIGN because the
        // DECISIONS agree — recover=null at 43,808/43,808 on BOTH routes. OBLIGATION CARRIED
        // FORWARD: never quote this row's figure combined-only; any future swap here must be
        // measured per route. Locator drift recorded: the triage's L1077-1093 is live at 1094-1110.
        JavaExpression thenRef = scope.thenArgRefFor(thenFn);
        if (thenRef == null || thenRef.getExpressionType() == null) {
            return null;
        }
        JavaType item = compiler.getTypeUtil().getItemType(thenRef.getExpressionType());
        return item instanceof com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue meta
                ? meta : null;
    }

    /**
     * facet inLambdaArgSeatIteHoist (PR #355): {@link #isInsideDrainableMapLambda}
     * WITHOUT the #312 conditional-arm exclusion — the NEAREST enclosing
     * {@link RInlineFunction} must still be a drainable map/extract lambda, but
     * {@link RConditionalExpr} crossings are ALLOWED: an arg-seat conditional
     * nested in an OUTER conditional's arm value relocates into the owning branch
     * via the arm-interior window ({@code ControlFlowHandler.appendConditionalChain}'s
     * pending-lambda-hoist drain), the inner-before-outer numbering falling out of
     * registration order (golden GetOptn esma: {@code ifThenElseResult0/1} declared
     * inside the {@code ifThenElseResult2} branch). The render-truth guard against
     * an outer that stays an INLINE ternary is the producer's
     * {@code isThenValueHoistSuppressed()} gate (the ternary fall-through pushes it
     * around its arm compiles — the P352A suppress-class discriminator), not this
     * AST walk.
     */
    public static boolean isInsideDrainableMapLambdaAllowingConditionalArms(RNode node) {
        RNode cur = node == null ? null : node.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                // A lambda whose BODY ROOT is a conditional belongs WHOLE to the
                // block-lambda family (the elseless/effective-else/ladder/nested
                // if-return forms and the deep-then restructure) — an arg-seat
                // registration anywhere inside it lands on the block-form
                // ATTEMPT's scope and trips its !hasPendingLambdaHoists() gate,
                // regressing the already-landed block render to the inline
                // ternary (the cp2 esma/fca UnderlyingIdentificationRule FLAT
                // catch — the #312 mangling class, self-inflicted). Those
                // interiors unlock only when their outer converts with its own
                // drain (the P352A law).
                return inline.parent() instanceof RExtractExpr
                        && !(inline.body() instanceof RConditionalExpr);
            }
            if (cur instanceof RFunction || cur instanceof RRule) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * facet enumSingletonListCondArm (PR #340): true iff {@code node} is DIRECTLY owned by
     * the {@code blessed} conditional — the parent walk reaches {@code blessed} (its
     * condition or an arm) without crossing a DIFFERENT {@link RConditionalExpr} (a nested
     * conditional is a different drain owner — the #219/#250 cascade guard), an
     * {@link RInlineFunction} (a nested lambda drains through its own channel), or the
     * enclosing rule/function root. The complement of {@link #isInsideDrainableMapLambda}'s
     * #312 conditional-arm exclusion, admitted ONLY under the
     * {@code compileEffectiveElseConditionalBlock} handshake (the caller verifies the
     * blessed node came from {@code findEnumConstArgDrainableCond}), so the enum decl's
     * pending registration is never orphaned on a path without the per-arm drains.
     */
    public static boolean isDirectlyInBlessedConditional(RNode node, RExpression blessed) {
        if (blessed == null) {
            return false;
        }
        RNode cur = node == null ? null : node.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur == blessed) {
                return true;
            }
            if (cur instanceof RConditionalExpr || cur instanceof RInlineFunction
                    || cur instanceof RFunction || cur instanceof RRule) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * facet sumTypedMethod (PR #299): whether {@code node} sits inside an enclosing
     * lambda body ({@link RInlineFunction}) before reaching its enclosing rule/function
     * — i.e. it is NOT a direct rule-/function-body-level chain operation.
     *
     * <p>The typed-sum facet ({@code CollectionHandler.handle(RListOpExpr)} SUM arm)
     * fires only at the rule-body level: the clean drr POJO carriers render
     * {@code output = <chain>.sumBigDecimal()…} at the body terminal. A sum INSIDE a
     * {@code mapSingleToItem} / {@code extract} lambda body is co-occupied with the
     * block-lambda / then-hoist facet (golden converts the lambda and hoists the
     * {@code thenArg}), so the wrap-form line break the typed-sum introduces would move
     * the still-divergent file +1 line further from golden (a within-waiver regression).
     * Declining inside a lambda keeps the facet a clean rule-body terminal flip.
     *
     * <p>Walks UP the parent chain only (the sum's own argument lambdas are CHILDREN,
     * never on this path), stopping at the enclosing {@link RRule}/{@link RFunction}.
     *
     * @param node starting node
     * @return {@code true} if an {@link RInlineFunction} encloses {@code node} before its
     *         rule/function root; {@code false} otherwise (including not-found)
     */
    public static boolean isInsideEnclosingLambda(RNode node) {
        RNode cur = node == null ? null : node.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) return true;
            if (cur instanceof RRule || cur instanceof RFunction) return false;
            cur = cur.parent();
        }
        return false;
    }

    /**
     * facet lambdaParamItemEscape (PR #292): the depth-escaped name for an
     * implicit-item lambda's {@code item} parameter. A lambda genuinely nested
     * inside N enclosing implicit-{@code item}-binding lambdas escapes to
     * {@code _}×N + {@code item} ({@code item}, {@code _item}, {@code __item}, …),
     * mirroring upstream {@code JavaScope.createUniqueIdentifier}'s {@code _}-prefix
     * collision escape ({@code GeneratorScope.escapeName} returns {@code "_" + name}).
     *
     * <p>The depth counts only enclosing OPERATION lambdas (an {@code extract} /
     * {@code filter} / {@code map} / {@code max} / … body) whose {@code item} is
     * rendered INLINE in the same statement. It EXCLUDES:
     * <ul>
     *   <li>EXPLICIT-param enclosing lambdas (they bind their own name, not
     *       {@code item}, so are transparent to {@code item}'s visibility);</li>
     *   <li>{@code then}-chain step lambdas ({@link RInlineFunction} whose parent is
     *       an {@link RThenExpr}). A {@code then <op>} renders as
     *       {@code thenArgN.<method>(item -> <op-body>)} — the then-step lambda and
     *       its merged operation lambda (the {@code <op>}'s own
     *       {@code RExtractExpr}/{@code RFilterExpr}/… body, which IS counted) share
     *       ONE {@code item}, so counting the then-lambda too would double-count;
     *       and an EARLIER then-step is hoisted to a separate {@code thenArg}
     *       statement (its own scope), so its {@code item} never collides with a
     *       later step's. Excluding then-lambdas yields the correct depth for both
     *       the nested case (DTCC_ProductGrade's inner {@code extract to-string} →
     *       {@code _item}) and the chain case (a plain {@code then extract} →
     *       {@code item}).</li>
     * </ul>
     *
     * <p>The fork previously hardcoded {@code "item"} at every implicit-item seat
     * (the param decl {@link CollectionHandler#resolveParamName} and the body
     * reference {@link ReferenceHandler#handle(RImplicitVariable, ExpressionContext,
     * ExpressionCompiler)}), so a nested implicit lambda SHADOWED its enclosing
     * {@code item} — it compiles (legal Java shadowing) but is byte-divergent
     * because golden always escapes. Green-safe by construction: ZERO of the
     * 34,686 9.83.0 goldens carry a nested un-escaped {@code item -> … item ->}
     * shadow (289 carry the escaped {@code _item} form, none deeper than one
     * level), so escaping only turns an already-divergent file golden-ward, while
     * a non-nested implicit lambda (the overwhelming majority) keeps {@code item}
     * byte-for-byte. The seat is shared with the FUNCTION cells; the toward-golden
     * movement is verified zero-regression by the full all-kinds D11 + regscan.
     *
     * @param implicitLambda the implicit-item lambda whose param name is wanted
     *                       (may be {@code null} → depth 0)
     * @return the depth-escaped implicit-item name ({@code "item"} at depth 0)
     */
    public static String escapedImplicitItemName(RInlineFunction implicitLambda) {
        int depth = 0;
        RNode cur = implicitLambda == null ? null : implicitLambda.parent();
        int walk = 0;
        while (cur != null && walk++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline
                    && (inline.isImplicit() || inline.paramNames().isEmpty())
                    && !(inline.parent() instanceof RThenExpr)) {
                depth++;
            }
            cur = cur.parent();
        }
        return "_".repeat(depth) + "item";
    }

    /**
     * Coverage wave D (datarule): the condition-instance parameter name for a
     * declaring type — lower-first of the type's simple name, keyword-escaped
     * with the {@code "_"} prefix (upstream registers
     * {@code instanceType.name.toFirstLower} through {@code JavaScope}'s
     * identifier escaping; the fork's proven form is
     * {@code JavaNamingUtil.escapeJavaKeyword}). Single source of truth for the
     * {@code executeDataRule}/{@code getValidationResults} parameter name — the
     * datarule generator's parameter and
     * {@code ReferenceHandler.buildConditionInstanceReceiver}'s synthetic
     * receiver must agree byte-for-byte.
     */
    public static String conditionInstanceName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return typeName;
        }
        String lower = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
        return com.regnosys.rosetta.generator.java.JavaNamingUtil.escapeJavaKeyword(lower);
    }

    /**
     * Find an attribute by name on a data type, searching supertypes recursively.
     *
     * @param dt the data type to search
     * @param name the attribute name
     * @param visited tracks visited types to prevent infinite loops on cycles
     * @return the attribute if found, or {@code null}
     */
    public static RAttribute findAttributeOnDataType(RDataType dt, String name, Set<RDataType> visited) {
        if (dt == null || !visited.add(dt)) return null;
        for (RAttribute attr : dt.attributes()) {
            if (name.equals(attr.name())) return attr;
        }
        if (dt.superType().isPresent()) {
            return findAttributeOnDataType(dt.superType().get(), name, visited);
        }
        return null;
    }

    /**
     * Convenience overload for {@link #findAttributeOnDataType(RDataType, String, Set)} with a fresh visited set.
     */
    public static RAttribute findAttributeOnDataType(RDataType dt, String name) {
        return findAttributeOnDataType(dt, name, new HashSet<>());
    }

    /**
     * Resolve the {@link RDataType} that an expression's VALUE has, for the two
     * navigation-receiver shapes the only-exists renderer
     * ({@link ExistenceHandler#handle(com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr,
     * com.regnosys.rosetta.generator.java.expression.ExpressionContext,
     * com.regnosys.rosetta.generator.java.expression.ExpressionCompiler)}) synthesizes:
     * <ul>
     *   <li>{@link RSymbolReference} — the resolved symbol's declared data type (a function
     *       input / output / local; the simple {@code root -> leaf} parent);</li>
     *   <li>{@link RFeatureCall} — the resolved feature's declared data type (the
     *       {@code root -> ... -> chain[n-2]} parent navigation).</li>
     * </ul>
     *
     * <p>Returns {@code null} when the symbol/feature is unresolved, resolves to a
     * non-{@link RAttribute} symbol (e.g. an alias {@code RShortcut} whose output type is
     * inferred-only), or the attribute's type is not a data type (enum / basic / record /
     * choice). A choice-typed parent declines here rather than narrowing through the upstream
     * {@code caseOnlyExists} {@code asRDataType} path — a deferred missed flip, never a wrong
     * byte. The only-exists renderer treats {@code null} as "decline" → keep the legacy
     * placeholder, so a missing resolution can never produce a wrong byte (no regression).
     *
     * <p>Deliberately a FOCUSED variant of {@code NavigationHandler.resolveReceiverDataType}:
     * the only-exists receiver is always a synthesized {@link RSymbolReference} or
     * {@link RFeatureCall} chain whose features the type-directed resolver populates, so it
     * needs neither the disguised-{@code REnumValueRef} branch nor the receiver-chain
     * {@code fallbackResolveFeature} that path carries.
     */
    public static RDataType resolveValueDataType(RExpression expr) {
        if (expr instanceof RSymbolReference symRef) {
            return symRef.symbol()
                    .filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast)
                    .map(HandlerHelper::attributeDataType)
                    .orElse(null);
        }
        if (expr instanceof RFeatureCall fc) {
            return fc.resolvedFeature()
                    .map(HandlerHelper::attributeDataType)
                    .orElse(null);
        }
        return null;
    }

    /**
     * The {@link RDataType} an attribute's type call references, or {@code null}
     * for a non-data type. Public for the facet {@code ingest_setter_value_form}
     * arm-C4 leaf-receiver walk ({@code FunctionExpressionRenderer
     * .isSameTypeOverride}) alongside the in-package coercion consumers.
     */
    public static RDataType attributeDataType(RAttribute attr) {
        if (attr.typeCall() == null) return null;
        return attr.typeCall().referencedType()
                .filter(RDataType.class::isInstance)
                .map(RDataType.class::cast)
                .orElse(null);
    }

    /**
     * Enumerate a data type's complete attribute set — own plus inherited — in the EXACT
     * order upstream 9.83.0 uses for {@code only exists}'s {@code allFields} list.
     *
     * <p>Replicates {@code RDataType.buildAllAttributesMap} (vendored rune-lang): a
     * {@link LinkedHashMap} keyed by attribute NAME, populated SUPER-type-first then OWN,
     * so super attributes lead in their declaration order, own attributes follow, an
     * overriding own attribute keeps its super-type position, and duplicate names dedup. An
     * identity-set {@code visited} guard bounds a (malformed) cyclic hierarchy.
     */
    public static List<RAttribute> allAttributesInUpstreamOrder(RDataType dt) {
        LinkedHashMap<String, RAttribute> acc = new LinkedHashMap<>();
        collectAllAttributes(dt, acc, Collections.newSetFromMap(new IdentityHashMap<>()));
        return new ArrayList<>(acc.values());
    }

    private static void collectAllAttributes(
            RDataType dt, LinkedHashMap<String, RAttribute> acc, Set<RDataType> visited) {
        if (dt == null || !visited.add(dt)) return;
        dt.superType().ifPresent(st -> collectAllAttributes(st, acc, visited)); // super FIRST
        for (RAttribute attr : dt.attributes()) {
            acc.put(attr.name(), attr); // own AFTER — overrides retain super position
        }
    }

    /**
     * facet ctorChoiceSuperAttrs (PR #389): the choice-super-aware sibling of
     * {@link #allAttributesInUpstreamOrder} — a data type EXTENDING A CHOICE
     * ({@code type BasketConstituent extends Observable}) inherits the choice's
     * OPTIONS as attributes (upstream {@code buildRDataType} projects them; the
     * fork's bridge is {@code RChoiceTypeRef.asRDataType}, whose option typeCall
     * IS the attribute identity — capital type-name keys, the #386 upper-initial
     * law). Used ONLY at the ConstructionHandler ctor seats (tryTypedBuilderBlock
     * + the condListCoerce pre-resolution): the shared walk above stays
     * choice-blind because ExistenceHandler renders its result as onlyExists
     * allFields STRING LITERALS — widening it would change rendered bytes for a
     * green data-extends-choice onlyExists parent, a form no golden verifies.
     */
    public static List<RAttribute> allAttributesIncludingChoiceSuper(RDataType dt) {
        LinkedHashMap<String, RAttribute> acc = new LinkedHashMap<>();
        collectAllAttributesChoiceAware(dt, acc, Collections.newSetFromMap(new IdentityHashMap<>()));
        return new ArrayList<>(acc.values());
    }

    private static void collectAllAttributesChoiceAware(
            RDataType dt, LinkedHashMap<String, RAttribute> acc, Set<RDataType> visited) {
        if (dt == null || !visited.add(dt)) return;
        dt.superType().ifPresent(st -> collectAllAttributesChoiceAware(st, acc, visited));
        // A CHOICE super contributes its options like a data super contributes its
        // attributes (super FIRST). The projection is the SAME bridge
        // RChoiceTypeRef.asRDataType performs (no duplicated projection — the #178
        // same-walk law); the bridge RDataType is workspace-free with null super ids,
        // so recursing on it terminates at its own projected attributes.
        dt.choiceSuperType().ifPresent(cs -> collectAllAttributesChoiceAware(
                new com.regnosys.rosetta.types.RChoiceTypeRef(cs.name(), List.of(), cs)
                        .asRDataType(),
                acc, visited));
        for (RAttribute attr : dt.attributes()) {
            acc.put(attr.name(), attr); // own AFTER — overrides retain super position
        }
    }

    /**
     * Unwrap a bare {@code MapperS.of(X)} expression to its inner content {@code X}.
     *
     * <p>Returns {@code null} if the expression is not exactly {@code MapperS.of(...)}
     * (e.g., chained forms like {@code MapperS.of(x).map("getY", ...)} return null).
     * Uses a balanced-paren walk — greedy regex like {@code ^MapperS\.of\((.+)\)$}
     * would incorrectly match chained forms.
     *
     * <p><b>facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615) — this helper
     * SURVIVES its own strip sites, and the TODO(C3a.4) promise that it becomes dead code with
     * them is measured FALSE.</b> The c8 census counted 414,406 arrivals at the pre-#615 head
     * (150,586 / 153,963 / 109,857 — default-route D11, IR-route D11, optimised; the IR route's
     * surplus is {@code IRJavaLeafEmitter.emitApply}, a path the default route does not take) and
     * split them by caller: the THREE transitional string strips took 409,231 (98.75%) —
     * {@code ReferenceHandler.unwrapForEvaluateArg} 320,024, {@code
     * FunctionExpressionRenderer.unwrapForAssignment} 80,996, {@code
     * ...unwrapForAddAssignment} 8,211 — and the FIVE idempotence-test callers took the residue,
     * 5,175 (1.25%), of which {@code CollectionHandler.boolHoistValue} alone is <b>4,653</b>
     * (1,551 / 1,551 / 1,551 — route-identical) over FOUR caller chains: 3,780 through
     * {@code boolHoistValue:10060<renderLadderLevel:12973} and 873 through the other three
     * ({@code compileEffectiveElseConditionalBlock} 402, {@code appendNestedConditional} 306,
     * {@code compileElselessConditionalBlock} 165). The four OTHER residue callers total 522
     * ({@code ConstructionHandler}'s three ctor-setter seats 148, {@code
     * FunctionExpressionRenderer}'s relayed-render seat 216, {@code
     * CollectionHandler.handle(RExtractExpr)}'s #345 receiver gate 120 and {@code
     * ReferenceHandler}'s collapsed-nav seat 38), and 4,653 + 522 = 5,175 closes the residue.
     * (The spec-compliance review caught 3,780 standing here as the class figure: it is the
     * LARGEST CALLER CHAIN, one slice of four, and quoting it left 873 arrivals unaccounted —
     * the #614 "top-N slice quoted as a class" defect, repeated.) PR #615 DELETED two of
     * the three strips as dead branches (0 whole-wrap matches at 320,024 and at 8,211
     * respectively), so exactly ONE transitional strip caller remains —
     * {@code unwrapForAssignment}, kept because it has 12 LIVE string-branch arrivals. The five
     * idempotence callers have no channel of their own and are what makes this helper outlive the
     * transition. JUSTIFIED-KEPT with that measurement.
     *
     * @param expr the expression string (may be null)
     * @return the inner content, or null if not a bare MapperS.of wrapper
     */
    public static String unwrapMapperSOf(String expr) {
        if (expr == null) return null;
        final String PREFIX = "MapperS.of(";
        if (!expr.startsWith(PREFIX)) return null;
        int depth = 1;
        int start = PREFIX.length();
        int n = expr.length();
        for (int i = start; i < n; i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                if (--depth == 0) {
                    return i == n - 1 ? expr.substring(start, i) : null;
                }
            }
        }
        return null;
    }

    /**
     * facet enumConstantWitness (v3.1 C2d retirement family 4 {@code dotted-enum-constant}, PR #611):
     * is this compiled builder a BARE Java enum constant {@code EnumName.CONSTANT} — an item-typed
     * constant that has no {@code .get()} and must be {@code MapperS.of}-wrapped wherever a Mapper is
     * expected? The answer is the PRODUCER's own witness, {@link JavaExpression#enumConstant} (the
     * one factory every emitter of that form builds through), never the rendered text. The retired
     * {@code isDottedEnumConstant} recovered the same fact from the SPELLING — no parenthesis, no
     * space, exactly one dot, an uppercase type half and an uppercase (or, since PR #267 A3, a
     * digit-leading {@code _30_360}-escaped) value half — and {@code ControlFlowHandler}'s
     * Mapper-typed ite-arm seat kept an inline twin of it (a null-typed, unwrap-free, no-args
     * bare-symbol arm whose render was dotted, parenless and single-line).
     *
     * <p>The PR #611 C2c census read the text answer beside this witness at EVERY consumer arrival on
     * BOTH routes over every cell of the 25-cell matrix — 391,168 arrivals (default route, sixteen
     * seats) and 351,404 (IR route, seventeen seats: the IR-java operand wrap joins) — and found the
     * two identical at every one: every dotted-constant arrival was a producer's render (the resolved
     * {@code Enum -> Value} reference, the parser-bound bare value, the IR route's
     * {@code emitEnumValue}) or the comparison seat's own sibling-enum re-qualification, and no
     * non-enum {@code X.Y} spelling ever reached a seat. The inline twin fired on exactly nine
     * arrivals per route, all parser-BOUND bare values (the node's {@code symbol()} IS the
     * {@code REnumValue}), so the triage's premise that the recovery-qualified node "carries no
     * resolved enum value" no longer holds since the v3.1 C1 resolution rebuild — the witness at the
     * producer is the verdict, recorded where the render is made.
     *
     * <p>Shared by every consumer on both routes (LAW 77): {@code ReferenceHandler.unwrapForEvaluateArg},
     * {@code FunctionExpressionRenderer.unwrapForAssignment}, {@code ControlFlowHandler}'s ite-arm and
     * cond-list item ladders and its Mapper-typed ite-arm assign seat, {@code ConstructionHandler}'s
     * ctor-arg and with-meta item forms, {@link #wrapEnumOperand} (comparison / contains / disjoint /
     * to-string) and {@code IRJavaLeafEmitter.wrapEnumValueOperand} — and the two alias-retype hops
     * that pass a witness through by identity rather than re-creating it from its text
     * ({@code ComparisonHandler.retypeNullTypedAliasOperand} and {@code ConversionHandler}'s to-string
     * retype): eleven call sites in all.
     *
     * @param builder the compiled builder (may be null)
     * @return true iff the builder is an enum-constant render by its producer's witness
     */
    public static boolean isBareEnumConstant(JavaStatementBuilder builder) {
        return builder instanceof JavaExpression.EnumConstant;
    }

    /**
     * AST half of the {@code boolean_condition_hoist} gate (PR #179) — a bare
     * function-call condition (an {@link RSymbolReference} resolving to an
     * {@link RFunction}, with or without explicit arguments). Such a condition
     * compiles ITEM-typed Boolean (upstream {@code evaluateCall} types calls at
     * the output item type) and so coerces to a conditional's expected primitive
     * boolean via the {@code convertNullSafe} statement hoist
     * ({@code final Boolean <id> = fn.evaluate(args);} + the
     * {@code (<id> == null ? false : <id>)} guard). Every other condition shape
     * (ComparisonResult / {@code exists} / Mapper chains) compiles WRAPPER-typed
     * and keeps the inline {@code .getOrDefault(false)} (upstream
     * {@code wrapperToItem}).
     *
     * <p>The SINGLE source of truth for the boolean-hoist AST gate, shared by
     * {@code FunctionExpressionRenderer.renderConditionalAssignment} (the
     * top-level SET-conditional seat, PR #179) and
     * {@code ControlFlowHandler.appendConditionalChain} (the
     * {@code ifThenElseResult} hoist-block seat) so the two render paths agree
     * on which conditions hoist (the same-law invariant). The emission site
     * additionally pairs this with the structural {@code MapperS.of}
     * invocation-wrap witness ({@code unwrapToBuilder().isPresent()}).
     *
     * <p>facet ruleCondBareInvokableValueMetaWrap (PR #372, F-gamma-A): a bare
     * RULE-call condition ({@link com.regnosys.rosetta.ast.functions.RRule}
     * symbol — {@code then if csaTrade.IsCSAAligned then …}) hoists the SAME
     * Boolean local (golden csa DTCC_Leg1CommodityInstrumentIDRule
     * {@code final Boolean _boolean = isCSAAlignedRule.evaluate(thenArg.get());}
     * + the null-guarded {@code if}) — a rule invocation compiles ITEM-typed
     * Boolean exactly like a function's, so the upstream {@code convertNullSafe}
     * law is symbol-kind-blind. An {@code = True} comparison condition stays
     * inline (the Leg2 sibling — ComparisonResult-typed, not a bare call).
     */
    public static boolean isBareFunctionCallCondition(RExpression condition) {
        return condition instanceof RSymbolReference ref
                && ref.symbol().filter(s -> s instanceof RFunction
                        || s instanceof com.regnosys.rosetta.ast.functions.RRule).isPresent();
    }

    /**
     * facet nestedElseStatementSeatCapture (v3.1 flip seat 33, law C.1, rung R2b): does this
     * ite-ladder rung CONDITION need a STATEMENT seat - i.e. does compiling it emit a declaration
     * line that cannot live inside an {@code } else if (...)} header? TWO suppliers, and this is
     * the SINGLE source of truth for the ladder renderer's nested-else capture (the
     * {@link #isBareFunctionCallCondition} sibling above keeps its own five call sites, which
     * ask a narrower question and stay byte-frozen).
     *
     * <ul>
     *   <li>the #179/#399-R3 BARE-FN condition, which hoists {@code final Boolean <id> =
     *       fn.evaluate(...);} - {@link #isBareFunctionCallCondition};</li>
     *   <li>the #317 COLLAPSED-META nav-receiver hop, which hoists {@code final <Wrapper> <name>
     *       = <chain>.get();} - {@code NavigationHandler.conditionCarriesCollapsedMetaDerefHop},
     *       the same conjunct ladder the producer itself consults (LAW 69).</li>
     * </ul>
     *
     * @param ladderScoped {@code true} only under the ONE threaded seat of law C.1 (the k==0
     *     FUNCTION-path MULTI SET base arm). The second disjunct is an AST scan and can, by
     *     construction, see a hop the compile never entered, so it is belted to the seat whose
     *     population was measured; the first disjunct is unconditional and byte-frozen.
     */
    public static boolean conditionNeedsStatementSeat(RExpression condition,
            ExpressionCompiler compiler, boolean ladderScoped) {
        return isBareFunctionCallCondition(condition)
                || (ladderScoped
                        && NavigationHandler.conditionCarriesCollapsedMetaDerefHop(
                                condition, compiler));
    }

    /**
     * facet aliasElsefulCondStepAdmit (PR #377, L3): a nested-tree block condition that
     * is a BARE boolean FUNCTION-INPUT reference renders golden's raw null-safe guard
     * {@code (name == null ? false : name)} — the input is already a raw Java Boolean
     * local, so no hoist line precedes it (the {@link #isBareFunctionCallCondition}
     * boolHoist twin minus the hoist). Returns the guard string or {@code null}.
     * Discriminators: the symbol IS one of the enclosing function's inputs (node
     * identity — a same-named nav attribute resolves to the TYPE's attribute, never an
     * input), and the compiled render is exactly {@code MapperS.of(<name>)} (render
     * truth: a nav compiles a {@code .map(} chain, an alias a method call). Boolean-ness
     * is the parser's own condition-type validation. Green-safe by corpus law: ZERO of
     * the 34,686 goldens carry {@code if (MapperS.of(<x>).getOrDefault(false))} at any
     * seat, so every carrier of the replaced form is an already-waivered mismatch.
     */
    public static String bareBooleanInputGuardOrNull(RExpression condition, String condStr) {
        if (!(condition instanceof RSymbolReference ref)) {
            return null;
        }
        var sym = ref.symbol().orElse(null);
        if (!(sym instanceof RAttribute attr)) {
            return null;
        }
        RFunction fn = findEnclosingFunction(condition);
        if (fn == null || fn.inputs().stream().noneMatch(in -> in == attr)) {
            return null;
        }
        if (!condStr.equals("MapperS.of(" + attr.name() + ")")) {
            return null;
        }
        return "(" + attr.name() + " == null ? false : " + attr.name() + ")";
    }

    /**
     * Wrap a bare enum-constant operand (by the producer's witness) ({@code EnumType.VALUE}) in
     * {@code MapperS.of(...)} so a comparison / set-operation sees a Mapper
     * operand, registering the {@code MAPPER_S} ref. Upstream compiles every
     * such operand against {@code MAPPER.wrapExtends(joined)}
     * ({@code ExpressionGenerator.binaryExpr}), so the item-typed constant
     * always coerces to the {@code MapperS.of(E.V)} form; the fork's
     * {@code REnumValueRef}/resolved-bare-enum renders are UNWRAPPED by design
     * (their consumers self-wrap). Shared by {@code ComparisonHandler}
     * (equality + ordering comparisons), {@code SetOperationHandler}
     * (contains/disjoint — facet void_witness_bare_enum arm D4) and
     * {@code ConversionHandler.handle(RToStringExpr)} (a bare enum-constant
     * to-string SOURCE — facet enumConstantToStringWrap, PR #244 — which upstream
     * coerces to {@code MapperS.of(E.V)} so {@code .map("to-string", …)} applies).
     * Non-constant renders pass through unchanged. The wrap is keyed on the OPERAND's
     * producer witness ({@link #isBareEnumConstant} — PR #611) and renders the operand
     * itself, so the text and the witness can never come from two different builders (the
     * code review's SF-3): a caller passes the builder its operand text was rendered from
     * — or, at the comparison seat, the re-qualification arm's own witness — and takes the
     * returned text as the operand.
     */
    public static String wrapEnumOperand(JavaStatementBuilder operand, Set<JavaClass<?>> refs) {
        String rendered = render(operand);
        if (isBareEnumConstant(operand)) {
            refs.add(MAPPER_S);
            return "MapperS.of(" + rendered + ")";
        }
        return rendered;
    }

    /**
     * v3.2 seat 13 (D53, THE CLOSING SEAT - the LOUD register's site R10, the chaos s30 {@code C30TwoOp} and s24
     * {@code C24Segments} rows, 25 of 25 non-compiling by the seat-13 census): a conditional whose hoisted local is a
     * META WRAPPER ({@code final FieldWithMetaString ifThenElseResultN;} - the ctor-setter / pathed-leaf META seat)
     * reached an arm whose compiled ITEM type is a BARE value, which the item form would assign raw
     * ({@code ifThenElseResultN = a;}, {@code = <chain>.get();}) - a String or a Void into a FieldWithMeta local,
     * which javac refuses. The released plugin coerces the arm into the wrapper null-guarded
     * ({@code a == null ? FieldWithMetaString.builder().build() : FieldWithMetaString.builder().setValue(a).build()},
     * a literal hoisted to a type-named local first; a Void arm the EMPTY wrapper) - the v3.3 heal under D53
     * decision 2. Refused by TYPE, ONE predicate for {@code ControlFlowHandler.hoistAsItemLocalOrNull}'s arm renderer
     * (the seat the chaos carriers reach) and {@code FunctionExpressionRenderer.renderPathedConditionalSetOrNull}'s
     * two value arms (LAW 69). A bare-typed local, an untyped arm and an arm whose item IS the wrapper return
     * without refusing (their bytes are the golden's or undecidable). The pre-seat render is a {@link Supplier},
     * read only when the site refuses (#634 round 1, the code-quality seat's NIT-8: the item-local hoist seat had
     * rendered it eagerly for every arm).
     */
    public static void refuseIfBareValueIntoMetaLocal(RExpression arm, JavaStatementBuilder armCompiled,
            JavaClass<?> declType, ExpressionCompiler compiler, String seatLabel, Supplier<String> preSeatRender) {
        JavaTypeUtil typeUtil = compiler == null ? null : compiler.getTypeUtil();
        if (!(declType instanceof RJavaWithMetaValue) || armCompiled == null || typeUtil == null) {
            return;
        }
        JavaType armType = armCompiled.getExpressionType();
        JavaType item = armType == null ? null : typeUtil.getItemType(armType);
        if (item == null) {
            // The arm compiled UNTYPED (a bare input reference, a literal, a navigation without a stamp - the
            // chaos carriers' arms): the FRONT-END type decides through the SAME read the Void-into-meta site
            // uses (inferredJavaType, LAW 69 - its STRUCTURAL meta guard answers null for a [metadata …]-bound
            // attribute and a with-meta arm, whose Java item is the wrapper the golden assigns directly); a bare
            // front-end type is the refused shape; null is undecidable and keeps its bytes.
            // A bare FUNCTION-CALL arm is judged by its callee's OUTPUT attribute (the #377-U arm's own read,
            // NavigationHandler.metaWrapperOf: a `[metadata reference]` output IS the wrapper the golden assigns
            // directly - cdm 5.38.0's `payer(tradeState, …)` at a ReferenceWithMetaParty leaf, the optimised
            // suite's catch at the c4g overlay run: the front-end type of a call is the output's BARE type); an
            // ALIAS call by its signature walk's item (tryAliasReceiverMapperType).
            if (arm instanceof RSymbolReference callRef
                    && callRef.symbol().orElse(null) instanceof RFunction callee) {
                RAttribute calleeOut = callee.output().orElse(null);
                if (calleeOut == null || NavigationHandler.metaWrapperOf(calleeOut, compiler) != null) {
                    return;
                }
            }
            JavaType aliasSig = arm instanceof RSymbolReference
                    ? NavigationHandler.tryAliasReceiverMapperType(arm, compiler) : null;
            if (aliasSig != null) {
                JavaType aliasItem = typeUtil.getItemType(aliasSig);
                if (aliasItem == null || aliasItem instanceof RJavaWithMetaValue) {
                    return;
                }
                item = aliasItem;
            } else {
                item = inferredJavaType(arm, compiler);
            }
            if (item == null) {
                return;
            }
        }
        if (item instanceof RJavaWithMetaValue) {
            return;
        }
        String itemName = item instanceof JavaClass<?> itemClass ? itemClass.getSimpleName() : item.toString();
        throw SilentDegradation.refuse(SilentDegradation.Site.HOIST_ARM_COERCION_DROPPED,
                "a bare " + itemName + " conditional arm at " + seatLabel + " whose hoisted local is "
                        + declType.getSimpleName() + " - the pre-seat render was `ifThenElseResultN = " + preSeatRender.get()
                        + ";` (the released plugin coerces the arm into the wrapper null-guarded)",
                arm);
    }

    /**
     * facet metaValueDerefHoist (PR #361): the SHARED meta-collapse deref-hoist
     * emission — hoist a collapsed META-WRAPPER item ({@code <chain>.get()} whose
     * element is FieldWithMetaX / ReferenceWithMetaX) to a type-named local on the
     * nearest statement-hoist sink and return the guarded VALUE re-presentation
     * {@code (<name> == null ? MapperS.<Value>ofNull() : MapperS.of(<name>
     * .getValue()))}. The hoist name-group is the lowercased wrapper simple name
     * (the #237 fieldWithMetaString convention — method-spanning numbering, golden
     * drr SortIdentifiers {@code fieldWithMetaString0..4}). Consumed by the
     * to-string source seat ({@code ConversionHandler}) and the list-literal
     * element seat ({@code LiteralHandler}); at renderConditionalAssignment
     * condition seats the block relocates into the owning branch via the #173
     * caller-collected channel for free. Returns {@code null} — caller keeps
     * today's bytes — when no sink is reachable (rule/POJO emission paths).
     * Green-safe by construction at both consumer seats: a bare collapsed item
     * where a Mapper is required never compiles, so every converted seat lived in
     * a waivered file.
     */
    public static String metaCollapseDerefHoistOrNull(String collapsedText,
            com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue wrapper,
            com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope,
            Set<JavaClass<?>> refs) {
        if (scope == null || wrapper == null) {
            return null;
        }
        com.regnosys.rosetta.generator.java.scoping.JavaStatementScope sink =
                scope.findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        String wrapperSimple = wrapper.getSimpleName();
        String valueSimple = wrapper.getValueType().getSimpleName();
        String sentinel = sink.statementHoistSession().register(
                com.regnosys.rosetta.generator.java.JavaNamingUtil.toFirstLower(wrapperSimple));
        sink.registerStatementHoist(
                "final " + wrapperSimple + " " + sentinel + " = " + collapsedText + ";");
        refs.add(wrapper);
        if (wrapper.getValueType() instanceof JavaClass<?> valueClass) {
            refs.add(valueClass);
        }
        refs.add(MAPPER_S);
        return "(" + sentinel + " == null ? MapperS.<" + valueSimple
                + ">ofNull() : MapperS.of(" + sentinel + ".getValue()))";
    }

    /**
     * facet toStringCollapsedMetaRetype (PR #387): the LAMBDA-channel sibling of
     * {@link #metaCollapseDerefHoistOrNull} — the join-bare ladder REPLAY's to-string
     * collapse arm has NO reachable statement sink (rule-path in-lambda seat), but
     * golden hoists the collapsed wrapper IN-RUNG (`if (…) { final FieldWithMetaString
     * fieldWithMetaString3 = <collapse>; return MapperC.of((fieldWithMetaString3 ==
     * null ? …)…); }` — drr common NameOfTheUnderlyingIndexRule). The decl rides the
     * pending-lambda-hoist channel as a sentinel-token
     * {@link ReferenceHandler.ItemGetMetaDerefHoist} (the #346 eager-render-scope-close
     * law) and the token joins the deferred-coercion name group (the #333
     * dtccDeclUseConsistency law — golden numbers the hoist local WITH the guarded
     * coercion params, fieldWithMetaString0..3). The caller gates on the replay window;
     * {@code CollectionHandler.compileLadderArmWithDeepThenDrain}'s pull admits the
     * class under the same flag, so the decl lands inside the owning rung.
     */
    public static String metaCollapseDerefLambdaHoist(String collapsedText,
            com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue wrapper,
            com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope,
            Set<JavaClass<?>> refs) {
        String wrapperSimple = wrapper.getSimpleName();
        String valueSimple = wrapper.getValueType().getSimpleName();
        var id = scope.createUniqueIdentifier(
                com.regnosys.rosetta.generator.java.JavaNamingUtil.toFirstLower(wrapperSimple));
        String token = scope.registerDeferredCoercionName(id);
        Set<JavaClass<?>> declRefs = new HashSet<>(refs);
        refs.add(wrapper);
        declRefs.add(wrapper);
        if (wrapper.getValueType() instanceof JavaClass<?> valueClass) {
            refs.add(valueClass);
        }
        refs.add(MAPPER_S);
        scope.registerPendingLambdaHoist(new ReferenceHandler.ItemGetMetaDerefHoist(
                wrapperSimple, token, collapsedText, declRefs, new HashSet<>()));
        return "(" + token + " == null ? MapperS.<" + valueSimple
                + ">ofNull() : MapperS.of(" + token + ".getValue()))";
    }

    /**
     * facet navGetWrap (PR #243): re-wrap a Mapper-consuming operand that rendered as a
     * <em>transparent item-collapse</em> — a {@link JavaExpression#selfUnwrapping selfUnwrapping}
     * expression whose text ends in {@code .get()} (a {@code MapperC}/{@code Mapper} navigation
     * collapsed to its single item) — in {@code MapperS.of(...)} so the consuming seat
     * (areEqual/notEqual/lessThan/… comparison operand, exists/notExists/single/multipleExists
     * argument) sees a {@code Mapper}. These operands carry a {@code null} expression type, so the
     * type-driven {@code WrapperToWrapperCoercer} (which already emits {@code MapperS.of(<e>.get())}
     * for a typed {@code MapperC}/{@code Mapper}→{@code MapperS} coercion) never fires and the fork
     * passes the bare {@code .get()} value — which does not compile against the Mapper-operand
     * signature, so the carrier is already waivered. Upstream compiles every such operand against
     * {@code MAPPER.wrapExtendsWithoutMeta(joined)} and the coercion service re-wraps the collapsed
     * item, so golden always carries the wrap.
     *
     * <p><b>Regression-safe by construction</b> (corpus-verified, frozen 9.83.0 baseline, all 5
     * cells): ZERO golden leaves a {@code .get()}-collapsed operand bare at any exists/comparison
     * seat — every one is {@code MapperS.of(...)}-wrapped — so the rewrite only ever touches
     * currently-waivered output.
     *
     * <p>facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the discriminator is the
     * PRODUCER's KIND plus a transparency compare, not a text mechanism. {@link #isSelfUnwrappingGetOperand}
     * admits exactly a {@link JavaExpression.SelfUnwrappingBareCollapse} — the marker
     * {@code CollectionHandler}'s {@code ListOp.ONLY_ELEMENT} arm stamps, its one bare-collapse
     * producer — whose render is the caller's own string. Everything else declines BY CLASS: the
     * {@code MapperS.of(…)} / {@code MapperC.<T>of(…)} wraps and the ten identity
     * {@link JavaExpression#selfUnwrapping} call sites (ctor typed-builder blocks, hoist sentinels,
     * the {@code getOrDefault} joins) are simply not that class, so this never double-wraps and never
     * touches a green operand. The retired three-conjunct text form — an {@code .endsWith(".get()")}
     * plus the unpacking of the then-overloaded marker — is gone; see that method's javadoc for the
     * structural proof that the swap is an exact bijection.
     */
    public static String wrapSelfUnwrappingGetOperand(String rendered, JavaStatementBuilder operand,
            Set<JavaClass<?>> refs) {
        if (isSelfUnwrappingGetOperand(rendered, operand)) {
            refs.add(MAPPER_S);
            return "MapperS.of(" + rendered + ")";
        }
        return rendered;
    }

    /**
     * facet bareValueMapperSWrap (PR #256) — the single source of truth for the bare-no-args
     * FUNCTION-invocation wrap, used wherever a Mapper-expecting seat consumes a bare implicit
     * function invocation. {@link ReferenceHandler#renderImplicitFunctionInvocation} emits a
     * bare no-args {@link RFunction} reference UNWRAPPED ({@code <fn>.evaluate(<arg>)}) — correct
     * for the boolean filter-predicate position (which strips the wrap), but at a Mapper-expecting
     * seat (an {@code exists}/{@code notExists}/{@code singleExists} argument, an {@code areEqual}
     * /comparison operand, a {@code MapperS<…>}-typed {@code ifThenElseResult} arm, a
     * {@code MapperS}-returning lambda {@code return}) golden wraps it {@code MapperS.of(...)}
     * (upstream {@code TypeCoercionService} item&rarr;MAPPER_S coercion). The bare-RULE sibling
     * ({@link ReferenceHandler#renderImplicitRuleInvocation}) already returns {@code wrappedInMapperSOf},
     * so this is the bare-FUNCTION analogue.
     *
     * <p>The gate keys on the raw-operand AST node KIND (a no-args {@link RSymbolReference} whose
     * symbol is an {@link RFunction}), NOT a rendered-string scan, so it fires exactly once and never
     * over-wraps the legitimate nested {@code MapperS.of(MapperS.of(...))} / idempotent
     * {@code MapperS.of(x.get()).get()} goldens (the #251 double-wrap traps). It is deliberately
     * NOT broadened to {@link RRule} — a bare-RULE operand arrives ALREADY wrapped, so including it
     * would double-wrap. Mutually exclusive with {@link #wrapSelfUnwrappingGetOperand} (a bare
     * function reference renders ending in {@code ")"}, not {@code ".get()"}, and carries no
     * {@code unwrapToBuilder}).
     *
     * <p><b>Green-safe by construction</b> (corpus-verified, frozen 9.83.0 baseline, all 5 cells):
     * ZERO golden leaves a bare {@code <fn>.evaluate(...)} at a Mapper-expecting seat — every one is
     * {@code MapperS.of(...)}-wrapped (532 exists / 1,455 ofNullSafe wrapped, 0 bare) — so the rewrite
     * only ever touches currently-waivered (non-compiling) output.
     */
    public static String wrapBareInvocationOperand(String rendered, RExpression rawOperand,
            Set<JavaClass<?>> refs) {
        return wrapBareInvocationOperand(rendered, rawOperand, refs, null);
    }

    /**
     * facet existsOperandMapperCWrap (PR #301) — cardinality-aware variant of
     * {@link #wrapBareInvocationOperand(String, RExpression, Set)}: the #298 bareInvokeMapperCWrap
     * law at the bare-OPERAND seat (vs #298's navigation-RECEIVER seat). When the bare function's
     * callee OUTPUT is MULTI, golden wraps {@code MapperC.<X>of(<fn>.evaluate(...))} not
     * {@code MapperS.of(...)} — e.g. {@code exists(getUnderlierProductIdentifier)} over a
     * {@code ProductIdentifier (0..*)} function renders {@code exists(MapperC.<ProductIdentifier>of(...))}
     * (the UnderlyingIdentificationType asic carrier — its sibling {@code areEqual(MapperC.<…>of(…)…)}
     * nav-receiver operand ALREADY gets the #298 wrap, but the bare {@code exists} operand fell to the
     * #256 {@code MapperS.of}). A SINGLE-output function keeps {@code MapperS.of} (the #256 form
     * unchanged — every existing carrier byte-identical). A META-annotated output declines (its
     * emitted witness is the meta wrapper, not bare X — kept on {@code MapperS.of}, mirroring #298).
     * The compiler-less callers ({@code ComparisonHandler.wrapBareFunctionOperand}, the 3-arg overload)
     * keep the {@code MapperS.of} single-wrap — no observed multi-output bare comparison operand;
     * a future carrier there would pass the compiler.
     *
     * <p><b>Green-safe by construction:</b> {@code MapperS.of} over a multi-list value is a
     * non-compiling type mismatch (the lambda then navigates a {@code List}), so every carrier this
     * flips was already a waivered mismatch — no green file carries the pre-fix form.
     */
    public static String wrapBareInvocationOperand(String rendered, RExpression rawOperand,
            Set<JavaClass<?>> refs, ExpressionCompiler compiler) {
        if (!(rawOperand instanceof RSymbolReference ref
                && ref.args().isEmpty()
                && ref.symbol().filter(RFunction.class::isInstance).isPresent())) {
            return rendered;
        }
        JavaClass<?> elem = bareMultiOutputWitness(rawOperand, compiler);
        if (elem != null) {
            // facet fqnWitnessMapperC (PR #302, the #301 Copilot R1 follow-on): when the
            // MapperC.<X>of witness simple name collides with the enclosing function OUTPUT
            // (different FQN), render the witness FQN-inline and suppress its (duplicate,
            // non-compiling) import — the bare-operand locus of the #194–#197 first-claim-wins
            // import-collision law. Null (no collision) keeps the bare-simple-name witness.
            String fqnWitness = bareMultiOutputWitnessFqn(elem, rawOperand, compiler);
            refs.add(MAPPER_C);
            if (fqnWitness == null) {
                refs.add(elem);
                return "MapperC.<" + elem.getSimpleName() + ">of(" + rendered + ")";
            }
            return "MapperC.<" + fqnWitness + ">of(" + rendered + ")";
        }
        refs.add(MAPPER_S);
        return "MapperS.of(" + rendered + ")";
    }

    /**
     * facet existsOperandMapperCWrap / bareInvokeMapperCWrap (PR #298/#301/#302) — the witness
     * ELEMENT {@link JavaClass} of a bare no-args FUNCTION operand whose callee output is MULTI,
     * non-meta and resolvable, or {@code null} when the operand is not such a bare-multi-function
     * (so the caller keeps the {@code MapperS.of} single-wrap). The single source of truth for the
     * cardinality decision shared across every bare-operand seat — the exists-operand seat
     * ({@code ExistenceHandler}), the comparison-operand seat ({@code ComparisonHandler}), the
     * list-literal-element seat ({@code LiteralHandler}), and the inline 4-arg
     * {@link #wrapBareInvocationOperand} render — so the seats cannot drift on when a bare function
     * is multi (the #243 SOT-comment pattern). A SINGLE-output / META-annotated / unresolvable-type
     * function returns {@code null} (the {@code MapperS.of} form unchanged — every #256/#280 carrier
     * byte-identical). Returns {@code null} when {@code compiler} is null (the compiler-less callers
     * keep the single-wrap, no observed multi-output bare operand there).
     */
    public static JavaClass<?> bareMultiOutputWitness(RExpression rawOperand, ExpressionCompiler compiler) {
        if (compiler == null
                || !(rawOperand instanceof RSymbolReference ref
                    && ref.args().isEmpty()
                    && ref.symbol().filter(RFunction.class::isInstance).isPresent())) {
            return null;
        }
        RFunction callee = (RFunction) ref.symbol().orElseThrow();
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        RAttribute out = callee.output().orElse(null);
        if (gm == null || tt == null || out == null || out.typeCall() == null
                || !gm.isMulti(out)
                || MetaFieldGenerator.detectMetaKind(out) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RType outRt = gm.resolveTypeCall(out.typeCall());
        if (outRt == null || outRt instanceof RMissingType) {
            return null;
        }
        return tt.toJavaReferenceType(outRt);
    }

    /**
     * facet fqnWitnessMapperC (PR #302, the #301 Copilot R1 follow-on) — the FQN-inline render
     * string for a {@code MapperC.<X>of(...)} witness ({@code elem} from {@link #bareMultiOutputWitness})
     * whose Java simple name collides with the enclosing function OUTPUT type (same simple name,
     * DIFFERENT fully-qualified name), or {@code null} when there is no collision (the bare-simple-name
     * witness renders unchanged). Mirrors {@code ReferenceHandler.mapperCWitnessOutputCollisionFqn}
     * at the bare-OPERAND locus so the operand seats cannot reintroduce the duplicate-simple-name
     * import the #194–#197 first-claim-wins law solved at the nav-receiver locus. Green-safe by
     * construction: a duplicate-simple-name witness import does not compile, so every collision
     * carrier is already waivered. A meta-annotated enclosing output never collides (its EMITTED
     * type is the concrete meta wrapper, not the bare base — PR #186 metaWit).
     */
    public static String bareMultiOutputWitnessFqn(JavaClass<?> witness, RExpression rawOperand,
            ExpressionCompiler compiler) {
        if (witness == null || rawOperand == null || compiler == null) {
            return null;
        }
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        GeneratorModel gm = compiler.getGeneratorModel();
        if (tt == null || gm == null) {
            return null;
        }
        RFunction fn = findEnclosingFunction(rawOperand);
        if (fn == null) {
            return null;
        }
        RAttribute out = fn.output().orElse(null);
        if (out == null || out.typeCall() == null
                || MetaFieldGenerator.detectMetaKind(out) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RType outputRt = gm.resolveTypeCall(out.typeCall());
        if (outputRt == null || outputRt instanceof RMissingType) {
            return null;
        }
        JavaClass<?> output = tt.toJavaReferenceType(outputRt);
        String witnessFqn = witness.getCanonicalName().withDots();
        if (witness.getSimpleName().equals(output.getSimpleName())
                && !witnessFqn.equals(output.getCanonicalName().withDots())) {
            return witnessFqn;
        }
        return null;
    }

    /**
     * facet navGetWrap (PR #243) — the navGetWrap DISCRIMINATOR, the single source of truth for the
     * operand seats ({@link #wrapSelfUnwrappingGetOperand}, called from {@code ComparisonHandler},
     * {@code ExistenceHandler} and — the code-quality review's SF-2 — {@code SetOperationHandler}'s
     * two {@code default}-operand seats) and for the Mapper-typed alias-return-ladder rung seat
     * ({@code FunctionExpressionRenderer.wrapSelfUnwrappingGetRungInMapperSOf}). True when {@code rendered}
     * is the verbatim render of a
     * {@link JavaExpression#selfUnwrappingBareCollapse selfUnwrappingBareCollapse} expression (a
     * {@code MapperC}/{@code Mapper} navigation collapsed to its single item, rendering
     * {@code <recv>.get()}).
     *
     * <p>The <b>transparency</b> check ({@code rendered.equals(collapse.renderToString())}) is the
     * load-bearing HALF that stays: it asserts the caller's string IS this operand's own render, so a
     * seat whose earlier wrap already rewrote the text cannot re-wrap it.
     *
     * <p>facet collapseGetSuffix (v3.1 C2d retirement family 7 {@code collapse-get-suffix}, PR #614):
     * the OTHER half — {@code rendered.endsWith(".get()")} plus the three-conjunct unpacking of the
     * OVERLOADED {@code selfUnwrapping} marker — is now the PRODUCER's own kind
     * ({@link JavaExpression#selfUnwrappingBareCollapse}, stamped by {@code CollectionHandler}'s
     * {@code ListOp.ONLY_ELEMENT} arm — LAW 69). The swap is an EXACT bijection, structurally, not
     * merely at this corpus: {@code unwrapToBuilder} is set by exactly four factories
     * ({@code wrappedInMapperSOf}, the {@code MapperCOfSingleWrap} ctor, its {@code witnesslessForm}
     * and {@code selfUnwrapping}) and only the last renders TRANSPARENTLY, so a transparent
     * {@code .get()}-tailed arrival can only be a {@code selfUnwrapping} product; of its eleven call
     * sites only the {@code ONLY_ELEMENT} arm builds an inner whose text ends {@code .get()} (the
     * {@code listOpMethod}/{@code wrapsListOp} switch arms make it unconditional), and that arm now
     * stamps this kind. The PR #614 C2c census measured the same split empirically at this seat:
     * verdict TRUE at 377 arrivals (default route; 355 optimised) and SEVEN transparent arrivals that
     * do NOT end {@code .get()} — the identity ctor-block / hoist-sentinel / {@code getOrDefault}
     * classes, which take the plain {@code selfUnwrapping} factory and decline here as before.
     *
     * <p>Keeping the predicate here (rather than copied at each seat) guarantees the two seats cannot
     * drift on what "transparent" means and reintroduce a double-wrap hazard or under-fire one seat
     * (Copilot R1 #243).
     */
    public static boolean isSelfUnwrappingGetOperand(String rendered, JavaStatementBuilder operand) {
        return operand instanceof JavaExpression.SelfUnwrappingBareCollapse collapse
                && rendered.equals(collapse.renderToString());
    }

    /**
     * facet collapseGetSuffix (v3.1 C2d retirement family 7 {@code collapse-get-suffix}, PR #614):
     * THE SHARED ARBITER of the family's master bijection — the ONE point every consumer that used to
     * ask "does this render end {@code .get()}?" now reads.
     *
     * <p>The bijection, measured by the PR #614 C2c census over all three walks (the default-route
     * D11 275/0F, the IR-route D11 275/0F and the optimised suite 12/0F) with ZERO counterexamples at
     * every seat that has both the AST node and the compiled builder:
     *
     * <blockquote>the render ends {@code .get()} &hArr; the expression root is
     * {@code RListOpExpr(op == ONLY_ELEMENT)} AND {@code getExpressionType() == null}</blockquote>
     *
     * A null-typed only-element collapse renders {@code <recv>.get()} ({@code CollectionHandler}'s
     * {@code ListOp.ONLY_ELEMENT} arm: method {@code get}, chain link {@code "."} — an inline,
     * type-less bare item); {@code FIRST} renders the Mapper-valued {@code .first()} member call,
     * {@code LAST} is never {@code .get()}-tailed, and every TYPED collapse renders a Mapper form
     * (the compiled type is exactly what the re-rooting erases at the null-typed ones — #260).
     *
     * <p>Read from NINE call sites serving TEN seats, each keeping its OWN already-typed conjuncts:
     * the deep-then decl re-wrap (S01) and the shared {@code isBareGetCollapse} block/ladder helper
     * (S02 — one call site that four consumer seats reach) in {@code CollectionHandler}; the
     * filter-predicate-arm comparand hoist (S06, {@code ComparisonHandler}); the ITE-arm nav collapse
     * re-wrap (S07, {@code ControlFlowHandler}); in {@code ConversionHandler} the to-string retype
     * guard (S08), the C5 meta-deref hoist (S11) and ONE call site shared by the to-string deref
     * gate and the ctor-setter/list-literal wrap (S10 + S12); the list-literal element deref (S14,
     * {@code LiteralHandler}); and the alias singleton-list rung wrap (S21,
     * {@code FunctionExpressionRenderer}).
     *
     * <p>Two swapped seats of this family deliberately do NOT read it. The to-enum conversion re-wrap
     * (S09) consults its own sibling AST predicate {@code isBareCollapseConversionArg} instead — the
     * census proved the two EXACT both ways, and LAW 69 prefers the predicate already standing beside
     * the seat (mutation lane F is its read point, not lane A). The distinct-collapse ITE-arm wrap
     * (S19/S20) reads its arm builder's expression type DIRECTLY, because this helper's other leg is
     * already required by its own {@code isDistinctCollapseThenBody} gate and routing through here
     * would cross-pair the then-body node with the enclosing {@code RThenExpr}'s builder against the
     * contract below (the code-quality review, SF-3; mutation lane H is its read point).
     *
     * <p>Total by construction — no type NAME is read, so the {@code NULL_TYPE} singleton (whose
     * {@code getSimpleName()} throws) is safe here.
     *
     * @param root the AST expression the seat is deciding about (the collapse candidate)
     * @param compiled that expression's compiled builder; {@code null} declines
     */
    public static boolean bareOnlyElementCollapse(RExpression root, JavaStatementBuilder compiled) {
        return root instanceof RListOpExpr op
                && op.op() == ListOp.ONLY_ELEMENT
                && compiled != null
                && compiled.getExpressionType() == null;
    }

    /**
     * Find the {@link REnumValue} named {@code valueName} in the enum {@code en} or any of its
     * {@code extends} super-enums (rune enum inheritance), walking the chain via
     * {@link REnumeration#superType()}. The generated Java enum FLATTENS inherited values under the
     * CHILD name, so the caller always qualifies with the child enum {@code en.name()} even when the
     * value is declared on a super-enum (facet enumQualifyInherited, PR #211; extended to the
     * comparison-operand seat at PR #215). The IDENTITY-keyed {@code visited} guard keeps a malformed
     * cyclic {@code extends} from looping and — unlike a name-keyed guard — does not stop the walk
     * early on a same-simple-name super-enum across namespaces. Returns {@code null} when no value of
     * that name exists in the hierarchy (which also guards an inverse re-qualification: searching from
     * a super-enum for a value declared on a child below it finds nothing).
     */
    public static REnumValue findEnumValueInHierarchy(REnumeration en, String valueName) {
        Set<REnumeration> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        REnumeration cur = en;
        while (cur != null && visited.add(cur)) {
            for (REnumValue val : cur.values()) {
                if (valueName.equals(val.name())) {
                    return val;
                }
            }
            cur = cur.superType().orElse(null);
        }
        return null;
    }

    /**
     * facet boundEnumInferredOwner (v3.1 flip seat 12 — THE ROOT FIX of the enum expected-owner
     * family): the enum that QUALIFIES a BOUND bare enum value — the node's INFERRED type (the
     * EXPECTED enum at the seat, {@code TypeInferenceEngine.expectedEnumAt} through the workspace
     * inference; upstream {@code enumCall(feature, expectedType.getItemValueType)},
     * ExpressionGenerator.xtend:340-343 — the generated Java flattens inherited values under the
     * child's name, the #211/#358 flatten law) when it is an enumeration {@code expected != declaring}
     * whose hierarchy flattens the EXACT bound value ({@code findEnumValueInHierarchy(expected, name)
     * == ev} — the #215 SAME-INSTANCE descend-only law, the same predicate the seven sibling qualifier
     * sites re-verify); otherwise the value's DECLARING enum, i.e. the pre-seat-12 bytes. Declines to
     * the declaring enum without a generator model (the no-arg handler-test compiler), on MISSING /
     * non-enum inference, and on a WIDENING seat (the value declared BELOW the expected enum —
     * {@code findEnumValueInHierarchy} walks UP only, so upstream's non-compiling {@code Base.VALUE}
     * is never emitted; by the bind law — every bare-enum bind walks UP from the position enum — such
     * a value never binds in the first place). SHARED by the two halves that must agree (LAW 69):
     * {@code ReferenceHandler.handle(RSymbolReference)}'s bare-enum arm (the constant + its import)
     * and {@code ControlFlowHandler.thenItemJavaClass} (the ITE hoist local's declared type) — a
     * hoist local typed by the declaring enum beside a constant qualified by the expected one would
     * not compile.
     */
    public static REnumeration boundEnumInferredOwner(
            RSymbolReference expr, REnumValue ev, REnumeration declaring, GeneratorModel gm) {
        if (gm == null || gm.workspace() == null) {
            return declaring;
        }
        RMetaAnnotatedType inferred = gm.workspace().getInferredType(expr);
        if (inferred == null || inferred.isMissing()
                || !(inferred.type() instanceof com.regnosys.rosetta.types.REnumTypeRef enumRef)) {
            return declaring;
        }
        REnumeration expected = enumRef.astNode();
        if (expected == null || expected == declaring) {
            return declaring;
        }
        return findEnumValueInHierarchy(expected, ev.name()) == ev ? expected : declaring;
    }

    /**
     * The bare-enum-value name for an UNRESOLVED operand (a comparison operand, a ctor-setter value,
     * or a {@code default} right operand), or {@code null} when the expression is not a bare enum
     * reference. Single source of truth for the {@code bare_enum_comparison_operand} family
     * (facet PR #154/#206/#209/#239), shared by {@code ComparisonHandler}, {@code ConstructionHandler}
     * and {@code SetOperationHandler} so the three seats cannot drift apart. Two parse shapes carry a
     * bare enum value the consuming context must qualify:
     * <ul>
     *   <li>an unresolved no-arg {@link RSymbolReference} (PR #154 — e.g. {@code Clearing} /
     *       {@code Name}): a bare enum value never resolves at link time (enum values are not in
     *       function scope), so {@code symbol()} is EMPTY and it renders the non-compiling bare name;
     *       a parser-resolved sibling-seeded enum carries a {@code symbol()} and already renders
     *       correctly, so {@code symbol().isEmpty()} excludes it; and</li>
     *   <li>an {@link REnumValueRef} the parser type-directed to an enum value but left with an EMPTY
     *       {@code enumeration()} (PR #206 — e.g. {@code NEWT}), rendering the un-prefixed Java
     *       constant. A disguised feature-call / rule-nav ({@code resolvedSymbol}/
     *       {@code resolvedAttributeChain} present) is NOT a bare enum and is excluded so its own
     *       navigation rendering is preserved.</li>
     * </ul>
     * Both shapes render a non-compiling un-prefixed constant pre-fix, so every carrier is already a
     * waivered mismatch — the value-name match against the target enum (the caller's responsibility)
     * is the strong gate.
     */
    public static String bareEnumValueName(RExpression value) {
        if (value instanceof RSymbolReference ref
                && ref.args().isEmpty()
                && ref.symbol().isEmpty()) {
            String n = ref.name();
            return (n == null || n.isEmpty()) ? null : n;
        }
        if (value instanceof REnumValueRef evr
                && evr.enumeration().isEmpty()
                && evr.resolvedSymbol().isEmpty()
                && evr.resolvedAttributeChain().isEmpty()) {
            String n = evr.valueName();
            return (n == null || n.isEmpty()) ? null : n;
        }
        return null;
    }

    /**
     * The candidate enum-value name for a TYPE-SHADOWED bare enum value (facet PR #239): a bare enum
     * value whose simple name COLLIDES with a model type of the same name (e.g. {@code Commodity} is
     * both the {@code AssetClassEnum.Commodity} value AND the {@code Commodity} data type;
     * {@code CashPrice} is both {@code PriceTypeEnum.CashPrice} and the {@code CashPrice} type) resolves
     * its {@link RSymbolReference} {@code symbol()} to that TYPE declaration ({@link RDataType} /
     * {@link RChoice}) rather than staying empty, so {@link #bareEnumValueName} declines and the value
     * renders as the bare type simple name — a non-compiling reference (a type used as a value), so
     * every carrier is already a waivered mismatch.
     *
     * <p>Facet bareEnumQualifyRBodyShadow (PR #299) extends the shadow to a {@link RBody}: a bare
     * enum value whose simple name collides with a regulatory-body declaration
     * ({@code body Authority CFTC} / {@code ISDA}) resolves its symbol to that {@link RBody}, so the
     * value renders as the bare body name ({@code MapperS.of(CFTC)} — a non-compiling undefined
     * symbol). The sibling-enum value-match below is the load-bearing gate: the candidate qualifies
     * only when the target enum (e.g. {@code SupervisoryBodyEnum} / {@code TaxonomySourceEnum})
     * declares a value of that name, so a genuine regulatory-body reference recovers no enum and
     * declines.
     *
     * <p>Returns the bare name for the caller to match against the TARGET enum; the qualification fires
     * ONLY when the target enum (recovered from the sibling / attribute / inferred type) declares a
     * value of that name. That final match is the load-bearing gate that makes admitting a type symbol
     * safe — a genuine type reference compared / assigned against a non-enum context recovers no enum
     * and declines. Resolved enum values (symbol {@link REnumValue}), function inputs/outputs
     * ({@link RAttribute}), functions and aliases are NOT type declarations and are excluded, so a real
     * enum-typed variable reference is never rewritten.
     */
    public static String typeShadowEnumValueName(RExpression value) {
        if (value instanceof RSymbolReference ref
                && ref.args().isEmpty()
                && ref.symbol().filter(HandlerHelper::isEnumValueShadowDeclaration).isPresent()) {
            String n = ref.name();
            return (n == null || n.isEmpty()) ? null : n;
        }
        return null;
    }

    /**
     * facet bareEnumAliasShadowComparand (seat 28, law 10): the shadow-declaration kinds a
     * bare enum value's name can collide with. {@link RDataType} / {@link RChoice} are the
     * #239 type shadow, {@link RBody} the #299 regulatory-body shadow, and
     * {@code RTypeAlias} the FOURTH - a {@code typeAlias} declaration in scope (drr
     * {@code typeAlias CountryCode: string(pattern: "[A-Z]{2,2}")}, imported into the hkma
     * rule namespace by {@code import drr.standards.iso.*}). The parser's enum bind
     * declines to override ANY present symbol, so such a value keeps the declaration
     * binding and renders as the bare name - a non-compiling reference to a type used as a
     * value, which is why every carrier is already a waivered mismatch. The caller's
     * value-match against the TARGET enum stays the load-bearing gate: a genuine
     * alias-typed reference recovers no enum value of that name and declines.
     *
     * <p>MEASURED population of the alias arm: 173 distinct {@code typeAlias} names
     * corpus-wide, of which exactly FOUR also name an enum value - {@code CountryCode}
     * ({@code EntityIdentifierTypeEnum}), {@code Initial}, {@code Percentage},
     * {@code Scheme}. The last three are declared ONLY in rune-fpml's namespace, which the
     * enum-value use sites do not import, so their bare uses stay symbol-EMPTY and are
     * already handled by {@code bareEnumValueName} - this arm is inert for them.
     */
    private static boolean isEnumValueShadowDeclaration(RNode s) {
        return s instanceof RDataType
                || s instanceof RChoice
                || s instanceof RBody
                || s instanceof com.regnosys.rosetta.ast.types.RTypeAlias;
    }

    /**
     * facet cat16BindEnumSeats (PR #452): the BIND-STAMPED sibling of
     * {@link #bareEnumValueName} — the engine's Category-16 expected-type bind
     * resolves a bare enum-value name to the position enum's
     * {@link com.regnosys.rosetta.ast.supporting.REnumValue} (declared possibly on
     * an {@code extends} SUPER-enum: {@code AssetIdTypeEnum extends
     * ProductIdTypeEnum}, {@code Name}/{@code Other} parent-declared), so
     * {@code symbol()} is PRESENT and {@link #bareEnumValueName} correctly
     * declines. The consuming qualification seats (the ctor-setter value, the
     * {@code default} RIGHT operand, the getOrDefault-arg hoist gate) admit the
     * stamped value through THIS helper as their third rung and re-verify it
     * against their own target enum's hierarchy under the #215 SAME-INSTANCE
     * descend-only law ({@code findEnumValueInHierarchy(target, name) == bound}),
     * so the render still qualifies by the EXPECTED (child) enum — the #211/#358
     * flatten law — and a same-name value on an UNRELATED enum declines to the
     * resolved rendering. Returns {@code null} for every non-bind shape (absent
     * symbol, args, non-enum-value symbols), leaving the existing rungs'
     * admissions byte-untouched.
     */
    public static com.regnosys.rosetta.ast.supporting.REnumValue boundBareEnumValue(RExpression value) {
        if (value instanceof RSymbolReference ref
                && ref.args().isEmpty()
                && ref.symbol().isPresent()
                && ref.symbol().get() instanceof com.regnosys.rosetta.ast.supporting.REnumValue ev) {
            return ev;
        }
        return null;
    }

    /**
     * facet aliasStripBound (PR #418, Copilot R1 + Seat-1 OBS-2): strip
     * {@link RAliasType} chains to the base {@link RType}, BOUNDED exactly like
     * the type system's own walk ({@code TypeAliasSolver.MAX_DEPTH} = 100 — the
     * solver already guards cyclic aliases, but the generator's strip sites must
     * not hang if a cycle ever slips through its gate). Replaces the THIRTEEN
     * previously-unbounded alias-strip loops across the handlers + renderer (the
     * #417 census-driven class-sweep law — and its refinement: the simple-name
     * grep {@code 'while (.*instanceof RAliasType'} found twelve, the thirteenth
     * used the FQN form {@code instanceof com.regnosys.rosetta.types.RAliasType}
     * — a census must also cover qualified spellings; every body was the pure
     * {@code refersTo()} strip). On hitting the bound the last alias is
     * returned as-is — the caller's downstream {@code instanceof} gates then
     * decline exactly as they do for any other unexpected type.
     */
    public static RType stripAliases(RType t) {
        int depth = 0;
        // 100 mirrors TypeAliasSolver.MAX_DEPTH (private there; cited one-way —
        // this is the generator-side copy of the type system's own bound).
        while (t instanceof RAliasType alias && depth++ < 100) {
            t = alias.refersTo();
        }
        return t;
    }

    /** Render a compiled expression to its string form. */
    public static String render(JavaStatementBuilder compiled) {
        if (compiled instanceof JavaExpression javaExpr) {
            return javaExpr.renderToString();
        }
        throw new IllegalStateException("Expected JavaExpression but got " + compiled.getClass().getSimpleName());
    }

    /**
     * Infer the Rune DSL type name for an expression, if possible.
     *
     * <p>This is a best-effort heuristic used to propagate type context to
     * literal operands in comparisons and arithmetic. It handles the common cases:
     * <ul>
     *   <li>{@link RSymbolReference} → resolved symbol's type call name</li>
     *   <li>{@link RFeatureCall} → resolved feature's type call name</li>
     *   <li>{@link RIntLiteral} → {@code "int"}</li>
     *   <li>{@link RNumberLiteral} → {@code "number"}</li>
     * </ul>
     *
     * @param expr the expression to inspect
     * @return the Rune DSL type name, or {@code null} if not inferrable
     */
    public static String inferRuneTypeName(RExpression expr) {
        if (expr instanceof RSymbolReference symRef) {
            return symRef.symbol()
                    .filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast)
                    .map(RAttribute::typeCall)
                    .map(RTypeCall::typeName)
                    .orElse(null);
        }
        if (expr instanceof RFeatureCall fc) {
            return fc.resolvedFeature()
                    .map(RAttribute::typeCall)
                    .map(RTypeCall::typeName)
                    .orElse(null);
        }
        if (expr instanceof RIntLiteral) {
            return "int";
        }
        if (expr instanceof RNumberLiteral) {
            return "number";
        }
        return null;
    }

    /**
     * Check if the given Rune DSL type name is a number type (BigDecimal).
     *
     * @param runeTypeName the Rune DSL type name (may be null)
     * @return true if the type maps to BigDecimal in Java
     */
    public static boolean isNumberType(String runeTypeName) {
        return "number".equals(runeTypeName);
    }

    /**
     * Apply metafield wrapper type naming based on the attribute's metadata
     * annotations. Returns the concatenated wrapper name (e.g.,
     * {@code "FieldWithMetaDate"} — the full Java class name, NOT generic
     * syntax like {@code "FieldWithMeta<Date>"}). The caller is responsible
     * for wrapping the result in {@code <...>} when building the mapper-chain
     * generic parameter.
     *
     * <p>Returns the unmodified {@code typeName} when the attribute has no
     * metadata annotation, or only type-level ones like {@code [metadata key]}
     * or {@code [metadata template]} which do not influence getter return types.
     *
     * <p><b>Dual-annotation precedence:</b> when an attribute carries both
     * reference-family ({@code [metadata reference]} / {@code [metadata address]})
     * and field-family ({@code [metadata scheme]} / {@code [metadata id]} /
     * {@code [metadata location]}) annotations, the reference wrapper wins. This
     * matches the precedence enforced by
     * {@link com.regnosys.rosetta.generator.java.object.MetaFieldGenerator#detectMetaKind(RAttribute)}.
     *
     * <p>Delegates to {@code MetaFieldGenerator.detectMetaKind} for the detection
     * semantics so this method stays in lock-step with the metafield class
     * generator.
     *
     * <p>Null handling: either argument being {@code null} yields {@code typeName}
     * unchanged (including {@code null}) — this is a deliberate defensive no-op
     * so callers in partial-resolution paths don't need extra guards.
     *
     * @param typeName the bare Java type name (e.g., {@code "Date"}); may be null
     * @param attr the Rune attribute being navigated; may be null
     * @return the effective Java type name for the generic parameter
     */
    public static String applyMetaWrapper(String typeName, RAttribute attr) {
        if (typeName == null || attr == null) return typeName;
        MetaFieldGenerator.MetaKind kind = MetaFieldGenerator.detectMetaKind(attr);
        return switch (kind) {
            case FIELD_WITH_META -> "FieldWithMeta" + typeName;
            case REFERENCE_WITH_META -> "ReferenceWithMeta" + typeName;
            case NONE -> typeName;
        };
    }

    /**
     * Check if the inferred type is {@code int} from a <em>resolved</em> source
     * (symbol reference or feature call), NOT from a literal.
     *
     * <p>Integer literals can appear in both {@code int} and {@code number} contexts,
     * so they should not be used to determine the enclosing expression's type.
     * Only resolved attribute types (from symbol references and feature calls)
     * provide definitive type information.
     *
     * @param runeTypeName the inferred Rune DSL type name (may be null)
     * @param expr the expression the type was inferred from
     * @return true if the type is {@code int} from a resolved source
     */
    public static boolean isResolvedIntType(String runeTypeName, RExpression expr) {
        if (!"int".equals(runeTypeName)) return false;
        // Only trust resolved types from symbol references and feature calls
        return (expr instanceof RSymbolReference) || (expr instanceof RFeatureCall);
    }

    /**
     * Numeric kind of an arithmetic/comparison operand, as upstream's typed JOIN
     * sees it (facet numeric_literal_typing). {@link #UNKNOWN} means "decline" —
     * callers MUST fall back to their existing legacy emission byte-verbatim.
     */
    public enum NumericKind { INT, NUMBER, UNKNOWN }

    /**
     * Resolve an operand's {@link NumericKind} through the typed model, mirroring
     * the inputs upstream's {@code RosettaTypeProvider} feeds the operand-type
     * JOIN ({@code ExpressionGenerator.binaryExpr}): literals carry their own
     * kind (upstream joins an int literal as {@code int} — unlike the
     * comparison-type heuristics {@link #inferRuneTypeName}/
     * {@link #isResolvedIntType}, which deliberately exclude literals);
     * attributes / function-call outputs / alias bodies / navigation leaves
     * resolve to their declared builtin type; nested arithmetic recurses
     * (divide is always {@code number} — upstream
     * {@code caseDivideOperation = UNCONSTRAINED_NUMBER}, unconditionally).
     *
     * <p>Resolution order per operand: (1) literal kind; (2) the inference
     * engine + translator (resolves attributes, typed navigation chains and
     * number/int type-aliases transitively — e.g. {@code Max18D13Number} →
     * {@code BigDecimal}); (3) typed-model walks for the kinds the engine
     * leaves MISSING — an {@code RSymbolReference} to an {@link RAttribute}
     * (declared builtin name), to an {@link RFunction} (output attribute's
     * builtin name), or to an alias ({@code RShortcut}, dual-path via
     * {@link NavigationHandler#resolveAliasReceiver} so typing and the rendered
     * {@code aliasName(args)} call cannot disagree; recursion into the body
     * behind an identity-set cycle guard; a LITERAL alias body declines — a
     * literal must not type its consumption site through an alias either); an
     * {@code RFeatureCall} leaf via {@code resolvedFeature()} or the gm-aware
     * {@link NavigationHandler#fallbackResolveFeature} walk; a DISGUISED
     * navigation ({@code initialPrice -> value} parsing as
     * {@code REnumValueRef} with no resolved enumeration — the PR #154/#163
     * disguise) via {@link NavigationHandler#resolveDisguisedFeature}.
     *
     * <p>Kinds the ENGINE has already typed — {@code count} ({@code int}),
     * numeric conditionals, typed navigation chains — resolve through the
     * engine arm above; that is upstream-faithful (upstream's type provider
     * feeds the same kinds into the join — {@code count} IS int there too;
     * note the count-operand RENDERING wrap is a separate mechanism, so
     * count-bearing files stay waivered regardless of their witnesses).
     * Genuine declines (return {@link NumericKind#UNKNOWN}; W42 decline-gates):
     * string operands (the engine resolves them non-numeric — the golden
     * {@code <String, String, String>add} family is reach-only and stays on
     * the callers' legacy paths), unary-minus sub-expressions
     * ({@code rawLeft() == null} — their rendering arms hardcode BigDecimal),
     * literal-bodied aliases, engine-MISSING operands no typed-model walk arm
     * resolves, and non-builtin type names the engine cannot resolve.
     */
    public static NumericKind numericOperandKind(RExpression operand, ExpressionCompiler compiler) {
        return numericOperandKind(operand, compiler, null);
    }

    // {@code visited} is an identity set guarding numericOperandKind's OWN alias/arithmetic
    // recursion (ADD-ONLY — a pessimistic guard returning UNKNOWN on a re-visit, distinct from
    // the OPTIMISTIC remove-on-exit on-stack guard in NavigationHandler.resolveReceiverDataType).
    // Typed Set<RNode> (not Set<RShortcut>) only so it can be passed to the shared
    // NavigationHandler.resolveDisguisedFeature, whose param the PR #253 cycle-guard widened to
    // Set<RNode>; numericOperandKind itself only ever adds RShortcut nodes (the alias hop below).
    private static NumericKind numericOperandKind(RExpression operand, ExpressionCompiler compiler,
            Set<RNode> visited) {
        if (operand instanceof RIntLiteral) {
            return NumericKind.INT;
        }
        if (operand instanceof RNumberLiteral) {
            return NumericKind.NUMBER;
        }
        // facet dispatchVariantParamResolution (PR #369): a nested DIVIDE is
        // unconditionally {@code number} upstream ({@code caseDivideOperation =
        // UNCONSTRAINED_NUMBER}) — checked BEFORE the inference-engine arm, whose
        // int/int JOIN types a divide {@code int} and mis-joined the enclosing
        // operation (`<Integer, Integer, Integer>add(<BigDecimal, …>divide…` —
        // never a golden shape, so no green file carries the pre-fix join).
        if (operand instanceof RArithmeticExpr earlyDiv && earlyDiv.rawLeft() != null
                && earlyDiv.op() == ArithOp.DIVIDE) {
            return NumericKind.NUMBER;
        }
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (typeUtil == null) {
            return NumericKind.UNKNOWN;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        if (gm != null && tt != null) {
            RMetaAnnotatedType inferred = gm.workspace().getInferredType(operand);
            if (inferred != null && !inferred.isMissing()) {
                JavaType java = tt.toJavaReferenceType(inferred.type());
                if (java != null && typeUtil.isBigDecimal(java)) {
                    return NumericKind.NUMBER;
                }
                if (java != null && typeUtil.isInteger(java)) {
                    return NumericKind.INT;
                }
                // The engine resolved a NON-numeric type — definitive decline.
                return NumericKind.UNKNOWN;
            }
        }
        // v3.2 seat 3 (F9): the implicit item at a TYPE-ALIAS condition's top level is the
        // alias's base value — its kind is the alias's type call resolved transitively (the
        // same kindFromTypeCall the attribute arm below uses), so a literal compared against it
        // widens exactly as against a number attribute (oracle golden PctCapped:
        // `MapperS.of(BigDecimal.valueOf(100))`). The engine leaves this implicit MISSING
        // (Category 8 types the literal `item` only inside lambdas and rule bodies); a Data
        // owner's implicit is a model instance and stays on the declines below.
        if (operand instanceof RImplicitVariable && gm != null && tt != null) {
            RTypeAlias ownerAlias = aliasOwner(findEnclosingTypeCondition(operand));
            if (ownerAlias != null) {
                // The RESOLVED Java type, not the builtin name: `number(fractionalDigits: 0)` is
                // Integer (oracle golden EvenNatNonNeg keeps `MapperS.of(0)`), `number(min: 0)` is
                // BigDecimal — the same translator read the datarule class's subject type takes.
                // v3.2 seat 13 (site R7): the read lives in aliasConditionItemJavaType since the
                // evaluate-arg plan consults it too (LAW 69 - one read, two consumers).
                JavaType aliasJava = aliasConditionItemJavaType(operand, compiler);
                if (aliasJava != null && typeUtil.isBigDecimal(aliasJava)) {
                    return NumericKind.NUMBER;
                }
                if (aliasJava != null && typeUtil.isInteger(aliasJava)) {
                    return NumericKind.INT;
                }
                return NumericKind.UNKNOWN;
            }
        }
        if (operand instanceof RSymbolReference symRef) {
            RNode sym = symRef.symbol().orElse(null);
            if (sym instanceof RAttribute attr) {
                return kindFromTypeCall(attr.typeCall(), compiler);
            }
            if (sym instanceof RFunction fn) {
                return fn.output()
                        .map(out -> kindFromTypeCall(out.typeCall(), compiler))
                        .orElse(NumericKind.UNKNOWN);
            }
            RShortcut alias = NavigationHandler.resolveAliasReceiver(symRef, sym);
            if (alias != null) {
                Set<RNode> guard = visited != null
                        ? visited
                        : Collections.newSetFromMap(new IdentityHashMap<>());
                if (!guard.add(alias)) {
                    return NumericKind.UNKNOWN;
                }
                RExpression body = alias.expression();
                if (body instanceof RIntLiteral || body instanceof RNumberLiteral) {
                    return NumericKind.UNKNOWN;
                }
                return numericOperandKind(body, compiler, guard);
            }
            // facet dispatchVariantParamResolution (PR #369): a dispatch VARIANT body's
            // bare param operand resolves against the dispatch BASE's declared input
            // (`periodsInYear` → int — the variant's own inputs are the placeholder).
            RAttribute baseInput = dispatchBaseInput(symRef, symRef.name());
            if (baseInput != null) {
                return kindFromTypeCall(baseInput.typeCall(), compiler);
            }
            // facet dispatchVariantParamResolution (PR #369): an UNRESOLVED call to a
            // runtime LIBRARY function (`Min(a, b)` with no cdm import in scope — the
            // rune-dsl builtin, rendered `new Min().execute(…)`) types as the JOIN of
            // its argument kinds (the builtin Min/Max are generic T→T). Any UNKNOWN
            // argument declines the whole call.
            if (!symRef.args().isEmpty()
                    && ("Min".equals(symRef.name()) || "Max".equals(symRef.name()))) {
                NumericKind join = NumericKind.INT;
                for (RExpression arg : symRef.args()) {
                    NumericKind k = numericOperandKind(arg, compiler, visited);
                    if (k == NumericKind.UNKNOWN) {
                        return NumericKind.UNKNOWN;
                    }
                    if (k == NumericKind.NUMBER) {
                        join = NumericKind.NUMBER;
                    }
                }
                return join;
            }
            // Coverage wave D (datarule): a linker-dark bare name inside an IMPLICIT
            // extract lambda of a TYPE CONDITION resolves against the lambda ITEM's
            // data type — the SAME walk the render's disguised-feature synthesis uses
            // (NavigationHandler.implicitItemDataType + findAttributeOnDataType), so
            // classification and render stay in lockstep. Golden drr
            // ESMAEMIRTransactionReportEMIR_VR_2059_01: `value >= 0` inside
            // `notionalAmountSchedule extract (…)` — `value` is NotionalPeriod's
            // BigDecimal attribute (not a declaring-type attribute, so the linker's
            // condition scope leaves it EMPTY), and the int-literal comparand wraps
            // BigDecimal.valueOf(0) exactly like the resolved-sibling law. An
            // EXPLICIT-param lambda declines (upstream scopes bare names there to the
            // condition instance, which the linker already resolves — an EMPTY symbol
            // under an explicit param is genuinely unresolvable). Function/rule paths
            // are untouched (findEnclosingTypeCondition is null there by
            // construction).
            if (symRef.args().isEmpty() && findEnclosingTypeCondition(symRef) != null) {
                RInlineFunction inline = com.regnosys.rosetta.ast.util.AstWalker
                        .findAncestor(symRef, RInlineFunction.class).orElse(null);
                if (inline != null && inline.isImplicit()) {
                    com.regnosys.rosetta.ast.types.RDataType item =
                            NavigationHandler.implicitItemDataType(symRef, compiler);
                    RAttribute itemAttr = item == null ? null
                            : findAttributeOnDataType(item, symRef.name());
                    if (itemAttr != null) {
                        return kindFromTypeCall(itemAttr.typeCall(), compiler);
                    }
                }
            }
            return NumericKind.UNKNOWN;
        }
        if (operand instanceof RFeatureCall fc) {
            RAttribute leaf = fc.resolvedFeature().orElse(null);
            if (leaf == null) {
                leaf = NavigationHandler.fallbackResolveFeature(fc, compiler);
            }
            return leaf == null ? NumericKind.UNKNOWN : kindFromTypeCall(leaf.typeCall(), compiler);
        }
        if (operand instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            // Disguised navigation (`initialPrice -> value` parses as
            // REnumValueRef(enumName=receiver, valueName=feature) exactly like
            // the PR #154/#163 shapes) — resolve the leaf attribute through the
            // SAME dual input/output + alias disguise walk the navigation
            // renderer uses. A REAL enum value ref (enumeration present) is
            // non-numeric and falls through to UNKNOWN below.
            RAttribute leaf = NavigationHandler.resolveDisguisedFeature(evr, compiler, visited);
            // facet dispatchVariantParamResolution (PR #369): a disguised record
            // accessor over a DATE-typed dispatch-BASE param (`endDate -> day` in a
            // variant body) has no leaf RAttribute — year/month/day are Integer (the
            // #366 record method-ref law), so the operand kind is INT.
            if (leaf == null && evr.valueName() != null
                    && ("year".equals(evr.valueName()) || "month".equals(evr.valueName())
                            || "day".equals(evr.valueName()))) {
                RAttribute head = dispatchBaseInput(evr, evr.enumName());
                if (head != null && isDateRecordTypeCall(head.typeCall(), compiler)) {
                    return NumericKind.INT;
                }
            }
            return leaf == null ? NumericKind.UNKNOWN : kindFromTypeCall(leaf.typeCall(), compiler);
        }
        // facet dispatchVariantParamResolution (PR #369): a conditional operand joins
        // its branches exactly like upstream's type provider (both INT → INT, any
        // NUMBER → NUMBER, anything unresolvable declines) — the conditional-bodied
        // alias operand class (`endDay: if … then Min(…, 30) else endDate -> day`).
        if (operand instanceof RConditionalExpr cond) {
            RExpression thenB = cond.thenBranch();
            RExpression elseB = cond.elseBranch().orElse(null);
            if (thenB == null || elseB == null) {
                return NumericKind.UNKNOWN;
            }
            NumericKind thenK = numericOperandKind(thenB, compiler, visited);
            if (thenK == NumericKind.UNKNOWN) {
                return NumericKind.UNKNOWN;
            }
            NumericKind elseK = numericOperandKind(elseB, compiler, visited);
            if (elseK == NumericKind.UNKNOWN) {
                return NumericKind.UNKNOWN;
            }
            return (thenK == NumericKind.NUMBER || elseK == NumericKind.NUMBER)
                    ? NumericKind.NUMBER : NumericKind.INT;
        }
        if (operand instanceof RArithmeticExpr nested && nested.rawLeft() != null) {
            if (nested.op() == ArithOp.DIVIDE) {
                return NumericKind.NUMBER;
            }
            NumericKind left = numericOperandKind(nested.rawLeft(), compiler, visited);
            if (left == NumericKind.UNKNOWN) {
                return NumericKind.UNKNOWN;
            }
            NumericKind right = numericOperandKind(nested.rawRight(), compiler, visited);
            if (right == NumericKind.UNKNOWN) {
                return NumericKind.UNKNOWN;
            }
            return (left == NumericKind.NUMBER || right == NumericKind.NUMBER)
                    ? NumericKind.NUMBER : NumericKind.INT;
        }
        return NumericKind.UNKNOWN;
    }

    /**
     * facet dispatchVariantParamResolution (PR #369): true when {@code typeCall}
     * resolves (alias-unwrapped) to the DATE builtin record — the numeric-operand
     * mirror of the #366 record-accessor gate.
     */
    private static boolean isDateRecordTypeCall(RTypeCall typeCall, ExpressionCompiler compiler) {
        if (typeCall == null || compiler == null) {
            return false;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null) {
            return false;
        }
        com.regnosys.rosetta.types.RType rt = gm.resolveTypeCall(typeCall);
        while (rt instanceof com.regnosys.rosetta.types.RAliasType alias) {
            rt = alias.refersTo();
        }
        return rt instanceof com.regnosys.rosetta.types.RRecordType rec
                && rec.kind() == com.regnosys.rosetta.types.RecordKind.DATE;
    }

    /**
     * Map a declared builtin type name to its {@link NumericKind}. Non-builtin
     * names (type-aliases the engine did not already resolve, model types,
     * enums, strings) decline to {@link NumericKind#UNKNOWN}.
     */
    private static NumericKind kindFromTypeCall(RTypeCall typeCall, ExpressionCompiler compiler) {
        String name = typeCall == null ? null : typeCall.typeName();
        if ("number".equals(name)) {
            return NumericKind.NUMBER;
        }
        if ("int".equals(name)) {
            return NumericKind.INT;
        }
        // facet numeric_literal_typing (PR #207) — a number/int type-ALIAS sibling (e.g. drr's
        // `valuationAmount ShortFraction5DecimalNumber`, a `number` alias) names its type by the
        // ALIAS, not the bare builtin "number"/"int", so the raw-string match above misses it and
        // the int-literal comparand stays unwrapped (`MapperS.of(0)` not
        // `MapperS.of(BigDecimal.valueOf(0))`). Resolve the typeCall transitively through the engine
        // + translator (the SAME alias-resolution `NavigationHandler.resolveJavaSimpleName` and
        // `ComparisonHandler.resolveOperandJavaType` already use — Max3Number → Integer,
        // ShortFraction5DecimalNumber → BigDecimal) so an alias sibling drives the BigDecimal/Integer
        // comparison context. Reached ONLY on the resolution-blind-nav fallback path (a non-missing
        // inferred type resolves numerically at the inference-engine arm above, before kindFromTypeCall);
        // declines (UNKNOWN) for any non-numeric resolved type, so an int-typed sibling keeps INT and the
        // literal renders bare exactly as today (no byte delta).
        if (compiler != null && typeCall != null) {
            GeneratorModel gm = compiler.getGeneratorModel();
            JavaTypeTranslator tt = compiler.getTypeTranslator();
            JavaTypeUtil tu = compiler.getTypeUtil();
            if (gm != null && tt != null && tu != null) {
                RType rt = gm.resolveTypeCall(typeCall);
                if (rt != null && !(rt instanceof RMissingType)) {
                    JavaType java = tt.toJavaReferenceType(rt);
                    if (java != null && tu.isBigDecimal(java)) {
                        return NumericKind.NUMBER;
                    }
                    if (java != null && tu.isInteger(java)) {
                        return NumericKind.INT;
                    }
                }
            }
        }
        return NumericKind.UNKNOWN;
    }

    /**
     * Union two {@code Set<JavaClass<?>>} into a fresh mutable HashSet.
     * Shared helper for handlers and coercers that need to union operand
     * refs with per-emission refs without mutating either input.
     * (Copilot round 18 DRY consolidation — replaces 4 identical private
     * helpers that had accumulated across WrapperToWrapperCoercer,
     * WrapperToItemCoercer, WrappedItemCoercer, ItemToItemCoercer.)
     */
    public static Set<JavaClass<?>> union(Set<JavaClass<?>> a, Set<JavaClass<?>> b) {
        Set<JavaClass<?>> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    /**
     * facet asKeyReference — the shared as-key MULTI element-wise stream tail
     * (upstream {@code FunctionGenerator.xtend} assignAsKey's multi arm),
     * appended to a MapperC-typed receiver at BOTH the segment-ADD seat
     * ({@code FunctionExpressionRenderer.renderAddAsKeyMultiOrNull}, PR #426)
     * and the constructor value seat
     * ({@code ConstructionHandler.tryCtorAsKeyReference}, PR #437 finding #35):
     * {@code .getItems().map(item -> W.builder().setExternalReference(item
     * .getMappedObject().getMeta().getExternalKey()).setGlobalReference(…)
     * .build()).collect(Collectors.toList())} — external FIRST then global (the
     * OPPOSITE of the single form's global-first order, both upstream's own).
     * Relative {@code \n\t}/{@code \n\t\t} continuations — callers re-anchor.
     * The caller adds {@link #COLLECTORS} and the wrapper class to its refs.
     */
    public static String asKeyMultiStreamTail(String item, String wrapperSimpleName) {
        return "\n\t.getItems()"
                + "\n\t.map(" + item + " -> " + wrapperSimpleName + ".builder()"
                + "\n\t\t.setExternalReference(" + item + ".getMappedObject().getMeta().getExternalKey())"
                + "\n\t\t.setGlobalReference(" + item + ".getMappedObject().getMeta().getGlobalKey())"
                + "\n\t\t.build())"
                + "\n\t.collect(Collectors.toList())";
    }

    /**
     * v3.1 flip seat 24 (facet {@code bigIntegerLiteralRulePath}, law F5): the hoist declaration of a beyond-long
     * int literal — {@code final BigInteger <name> = new BigInteger("<digits>");} — THE ONE decl text every
     * producer writes (the method sink's registered string, the lambda channel's {@link BigIntegerLiteralHoist}).
     */
    public static String bigIntegerLiteralDecl(String name, String digits) {
        return "final BigInteger " + name + " = new BigInteger(\"" + digits + "\");";
    }

    /**
     * v3.1 flip seat 24 (law F5): the VALUE consumption of a hoisted BigInteger literal at a {@code number} seat —
     * upstream's {@code convertNullSafe} Mapper ternary, BARE. A whole conditional arm takes it as is (golden
     * {@code return bigInteger == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(new BigDecimal(bigInteger));},
     * {@code ifThenElseResult = bigInteger == null ? … ;}); an OPERAND seat parenthesises it (golden
     * {@code lessThanEquals(…, (bigInteger == null ? … ), …)} — the caller's choice, {@link #isConditionalArm}).
     */
    public static String bigIntegerValueTernary(String name) {
        return name + " == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(new BigDecimal(" + name + "))";
    }

    /**
     * v3.1 flip seat 24 (law F5): the ITEM consumption of a hoisted BigInteger literal — the parenthesised null-guarded
     * conversion an evaluate ARGUMENT or a {@code default} RIGHT takes (golden {@code extractCDECallAmount.evaluate(
     * input, (bigInteger == null ? null : new BigDecimal(bigInteger)))}, {@code .getOrDefault((bigInteger0 == null ?
     * null : new BigDecimal(bigInteger0)))}). The evaluate-arg seat's own service-sourced form is byte-identical.
     */
    public static String bigIntegerItemTernary(String name) {
        return "(" + name + " == null ? null : new BigDecimal(" + name + "))";
    }

    /**
     * v3.2 seat 5 (PR #626, F3) round 1, the code-quality review's MF-1 - THE ONE DECLARATION both halves of the
     * beyond-long literal's statement-seat law consult (LAW 69). Returns the ITEM type such a literal must be
     * CONVERTED to at this arm's statement seat, or {@code null} when the seat renders no conversion at all.
     *
     * <p>Why it must be one declaration: {@code LiteralHandler} decides whether to HOIST the literal and
     * {@code FunctionExpressionRenderer.renderNumericConvertNullSafeOrNull} decides whether to RENDER the
     * conversion. The review found the two halves disagreeing: the hoist fired at every statement-seat value while
     * the renderer declined whenever the conversion is the IDENTITY (a {@code BigInteger}-typed output, reachable
     * through {@code typeAlias Big: int(digits: 25)}), and a hoisted-but-unrendered literal falls to the generic
     * arm, which appends {@code .get()} to the value ternary - the very non-compiling form this seat exists to
     * remove, on a shape that COMPILED before the seat. Upstream declares a variable only INSIDE
     * {@code TypeCoercionService.convertNullSafe}: with no conversion to make there is no declaration either, and
     * the pre-seat inline form is upstream's own. So: no conversion, no hoist.
     *
     * <p>Admits what case (i) of the renderer serves: a whole-expression or conditional-arm value of an operation
     * with NO segment path (a segment targets an attribute of the output, whose type this does not read), whose
     * enclosing function declares an output with no meta, whose ITEM type is numeric and is NOT {@code BigInteger}
     * - a SINGLE or a MULTI output alike (round 2, the code-quality review's MF-1: the whole-SET seat used to
     * decline every multi output and the admitted hoist fell to the generic arm there; it serves the list form now,
     * golden MultiWhole). Returns that item type. The renderer reads the output's type again for the conversion
     * TEXT (the coercion service takes the RType) and keeps belts this predicate does not model. Two of them are
     * unreachable from an admitted hoist BY CONSTRUCTION (the re-verification's cq NIT-3): the rule path - this
     * predicate needs an enclosing function and returns null under a rule; a builder-backed target - a numeric,
     * meta-free output never needs a builder. Three are UNMEASURED (no corpus row, no oracle group): the
     * target-name compare, a multi/item disagreement, the ADD seat's item read - disclosed and banked, claimed
     * neither way.
     */
    public static JavaClass<?> statementSeatConversionItemOrNull(RExpression arm, ExpressionCompiler compiler) {
        if (arm == null || compiler == null) {
            return null;
        }
        ROperation owner = enclosingStatementSeatOperationOrNull(arm);
        if (owner == null || owner.segment().isPresent()) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        JavaTypeUtil tu = compiler.getTypeUtil();
        if (gm == null || tt == null || tu == null) {
            return null;
        }
        RFunction fn = findEnclosingFunction(arm);
        RAttribute out = fn == null ? null : fn.output().orElse(null);
        if (out == null || out.name() == null
                || MetaFieldGenerator.detectMetaKind(out) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RType outR = gm.getType(out);
        JavaType target = outR == null ? null : tt.toJavaReferenceType(outR);
        if (!(target instanceof JavaClass<?> item) || !tu.extendsNumber(item) || BIG_INTEGER.equals(item)) {
            return null;
        }
        return item;
    }

    /**
     * v3.2 seat 5 (PR #626) round 1, the code-quality review's SF-4: the AST target name of the operation whose
     * VALUE this expression is, or {@code null} when it is not a statement-seat value. Read beside
     * {@link #statementSeatConversionItemOrNull} so a seat comparing an output's name against a target name
     * compares against the name the AST carries, never against the ESCAPED Java name a caller may have
     * substituted (`class` vs `_class` - the escape made the conversion seat decline silently).
     */
    public static String findEnclosingOperationTargetNameOrNull(RExpression arm) {
        ROperation op = enclosingStatementSeatOperationOrNull(arm);
        return op == null ? null : op.targetName();
    }

    /**
     * The operation whose VALUE this expression is: itself when the expression is the operation's whole
     * expression, or the operation above a chain of conditional ARMS (a nested {@code if} arm is still the
     * operation's value). Any other parent - an operand, an argument, a list-op receiver, a lambda - declines,
     * which is what keeps the hoist off the evaluate-argument seat (the seat's own first-cut catch).
     */
    private static ROperation enclosingStatementSeatOperationOrNull(RExpression arm) {
        RExpression cur = arm;
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            RNode parent = cur.parent();
            if (parent instanceof ROperation op) {
                return op.expression() == cur ? op : null;
            }
            if (parent instanceof RConditionalExpr cond
                    && (cond.thenBranch() == cur || cond.elseBranch().orElse(null) == cur)) {
                cur = cond;
                continue;
            }
            return null;
        }
        return null;
    }

    /** v3.1 flip seat 24 (law F5): true when {@code e} is the then- or else-arm of a conditional (a WHOLE arm value). */
    public static boolean isConditionalArm(RExpression e) {
        return e != null && e.parent() instanceof RConditionalExpr cond
                && (cond.thenBranch() == e || cond.elseBranch().orElse(null) == e);
    }

    /**
     * v3.1 flip seat 24 (law F5): true when {@code node} sits inside an {@code extract}/{@code map} lambda (the nearest
     * enclosing inline function's parent is an {@link RExtractExpr}) — the lambda whose block forms drain the pending
     * lambda channel into the owning branch. Walks THROUGH conditionals, list-ops and {@code default}s (a literal arm
     * sits under them); a function or rule boundary declines. The #355 arg-seat predicates refuse a conditional
     * ancestor by design (a block splices no statement into a ternary arm); this literal seat is the arm itself.
     */
    public static boolean literalInsideExtractLambda(RNode node) {
        RNode cur = node == null ? null : node.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                return inline.parent() instanceof RExtractExpr;
            }
            if (cur instanceof RFunction || cur instanceof RRule) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * v3.1 flip seat 24 (law F5): the LAMBDA-channel hoist of a beyond-long literal — a marker-classed statement
     * (the {@code DeepThenArgHoist} / {@code ItemGetMetaDerefHoist} pattern: a sentinel-named decl rendered at drain
     * time, resolved by the method session's name pass) that the elseless, effective-else and ladder-arm drains admit
     * INTO the owning branch, immediately before the return that consumes it.
     */
    public static final class BigIntegerLiteralHoist
            extends com.regnosys.rosetta.generator.java.statement.JavaStatement {
        private final String token;
        private final String digits;

        public BigIntegerLiteralHoist(String token, String digits) {
            this.token = token;
            this.digits = digits;
        }

        @Override
        public void render(StringBuilder sb) {
            sb.append(bigIntegerLiteralDecl(token, digits)).append("\n");
        }

        @Override
        public Set<JavaClass<?>> getRefs() {
            return Set.of(BIG_INTEGER);
        }

        @Override
        public Set<JavaClass<?>> getStaticWildcardImports() {
            return Set.of();
        }
    }

    /**
     * v3.1 flip seat 22 (facet {@code intLiteralNumberSeat}, LAW 69 / LAW 77): the {@code BigDecimal}
     * expected type an int-literal conditional ARM would have received from upstream's context threading
     * when the fork compiled it with NONE. Upstream threads the expected type UNCHANGED through every
     * conditional arm ({@code ExpressionGenerator.caseConditionalExpression}: {@code
     * expr.ifthen.javaCode(context)} / {@code expr.elsethen.javaCode(context)}), so the literal's
     * effective consumer is the OUTERMOST conditional of the arm chain - the ladder root whose
     * workspace-inferred type is exactly what the fork's hoisted {@code ifThenElseResult} local and
     * typed-empty {@code Mapper*.<T>ofNull()} terminal are declared from (the decl the arm is assigned
     * into - the seat-14 law at the literal seat). Non-null exactly when: the literal IS a conditional
     * arm (then/else, possibly nested) of ANY magnitude (seat 24's law F5 lifted seat 22's within-long
     * exclusion: a beyond-long literal takes the same {@code BigDecimal} seat and then the BigInteger
     * hoist - {@link #bigIntegerLiteralDecl}), and the ladder root's
     * inferred type translates to {@code BigDecimal}. The negative domain (an {@code int}/MISSING
     * ladder) returns {@code null} and the literal stays bare - measured over all 275 matrix rows (the
     * seat-22 LAW-75 probe): the number-rooted within-long arm literals are exactly the 7 drr 5.61.0
     * carriers ({@code OptionPremiumAmountRule} asic/mas, {@code SpreadOfLeg1BasisRule} esma/fca,
     * {@code SpreadOfLeg2BasisRule} esma/fca, {@code SpreadOfLeg2PercentageRule} fca); every green
     * conditional-arm literal reads int/MISSING.
     *
     * <p>CONSULTED BY BOTH ROUTES (LAW 77): {@code LiteralHandler.isBigDecimalContext}'s null-expected
     * rung (the legacy literal seat) and {@code IRExpressionCompiler.visitIntLiteral} (the IR claim
     * root, which threads the returned type as the context's expected - the IR leaf
     * {@code IRJavaLeafEmitter.emitInt} honours an expected {@code BigDecimal} on its own).
     */
    public static JavaType intLiteralConditionalArmExpectedType(RIntLiteral literal,
            GeneratorModel gm, JavaTypeTranslator translator, JavaTypeUtil typeUtil) {
        if (literal == null || gm == null || gm.workspace() == null
                || translator == null || typeUtil == null) {
            return null;
        }
        RNode cur = literal;
        RNode parent = literal.parent();
        boolean sawArm = false;
        int depth = 0;
        while (parent instanceof RConditionalExpr cond && depth++ < PARENT_WALK_LIMIT
                && (cond.thenBranch() == cur || cond.elseBranch().orElse(null) == cur)) {
            sawArm = true;
            cur = parent;
            parent = parent.parent();
        }
        if (!sawArm) {
            return null;
        }
        RMetaAnnotatedType inferred = gm.workspace().getInferredType((RExpression) cur);
        if (inferred == null || inferred.isMissing()) {
            return null;
        }
        JavaType item = translator.toJavaReferenceType(inferred.type());
        if (item == null || !typeUtil.isBigDecimal(item)) {
            return null;
        }
        // facet ctorCondSingleCoerceLiteralArm (v3.1 flip seat 32, law B2): the CONSUMER beats
        // the ladder root's own inference. When the ladder ROOT this walk arrived at IS a
        // constructor key-value pair's value, the literal's effective consumer is that pair's
        // ATTRIBUTE - the very type ConstructionHandler.condSingleCoerceFor pushes as the
        // handshake's targetType, derived below by the IDENTICAL read (gm.getType(attr) ->
        // translator.toJavaReferenceType), so the two halves cannot disagree (LAW 69). At an
        // Integer/Long attribute the ladder re-declares its hoisted local at that type (the
        // ControlFlowHandler differs-gate, law B2 rung (a)) and golden assigns the literal BARE
        // there - golden drr 7.0-7.3 AdjustFrequencyPeriod `final Integer ifThenElseResult1;
        // ... ifThenElseResult1 = 1;` - so returning the root's BigDecimal would render
        // `BigDecimal.valueOf(1)` into an Integer slot.
        JavaClass<?> ctorTarget = ctorPairAttributeTypeOrNull(parent, cur, gm, translator);
        if (ctorTarget != null
                && (typeUtil.isInteger(ctorTarget) || typeUtil.isLong(ctorTarget))) {
            return null;
        }
        return item;
    }

    /**
     * facet ctorCondSingleCoerceLiteralArm (v3.1 flip seat 32, law B2): the CONSUMER's Java
     * type when {@code ladderRoot} IS a constructor key-value pair's VALUE, else {@code null}
     * (including when the pair is a seat the ctor handshake itself declines).
     *
     * <p>The guards are {@code ConstructionHandler.condSingleCoerceFor}'s, one for one, because
     * the two must key the same population (LAW 69): a MULTI attribute takes the LIST handshake
     * ({@code condListCoerceFor}), whose local is a {@code List} and never an item; a
     * META-annotated attribute is the seat-27 51-entered guard (its Java type is the WRAPPER
     * while the hoist's own walk keys the wrapper's VALUE type); an unresolved or missing item
     * type has no target at all. The attribute walk is {@code condListPreAttrs}'s
     * choice-super-aware one ({@link #allAttributesIncludingChoiceSuper}) so a
     * conditional-valued choice-option pair resolves identically on both sides.
     *
     * <p><b>MEASURED reach</b> (LAW-75 probe {@code [P32-B2T]}, whole matrix, both routes,
     * byte-identical): of the 422 conditional-arm int literals, 106 read
     * {@code verdict=BigDecimal}; the carrier's 4 rows are the ONLY ones whose ladder root's
     * parent is an {@code RKeyValuePair} - every other BigDecimal verdict roots under an
     * {@code RInlineFunction} (82 rows) or an {@code RShortcut} (20). Green blast radius:
     * <b>0 rows, 0 distinct green sites</b>.
     *
     * <p><b>Zero plumbing by design.</b> The handshake this mirrors IS live on the compile scope
     * at literal-compile time ({@code ConstructionHandler} pushes it around the value compile),
     * but reading it would need a scope parameter on
     * {@link #intLiteralConditionalArmExpectedType} and therefore an edit to all FOUR of its
     * readers ({@code LiteralHandler}, {@code IRExpressionCompiler}, {@code CollectionHandler},
     * {@code FunctionExpressionRenderer}). Reading the pair's own attribute instead keeps the
     * signature - so BOTH routes inherit this narrowing untouched, route-safe by SHARING and
     * not by an IR twin (LAW 77).
     */
    private static JavaClass<?> ctorPairAttributeTypeOrNull(RNode ladderParent, RNode ladderRoot,
            GeneratorModel gm, JavaTypeTranslator translator) {
        if (!(ladderParent instanceof com.regnosys.rosetta.ast.expressions.supporting
                        .RKeyValuePair pair)
                || pair.value() != ladderRoot
                || !(pair.parent() instanceof com.regnosys.rosetta.ast.expressions.constructors
                        .RConstructorExpr ctor)
                || ctor.typeCall() == null) {
            return null;
        }
        RType resolved = gm.resolveTypeCall(ctor.typeCall());
        RDataType dataType = null;
        if (resolved instanceof com.regnosys.rosetta.types.RDataTypeRef dtr) {
            dataType = dtr.astNode();
        } else if (resolved instanceof com.regnosys.rosetta.types.RChoiceTypeRef ctr) {
            dataType = ctr.asRDataType();
        }
        if (dataType == null) {
            return null;
        }
        for (RAttribute attr : allAttributesIncludingChoiceSuper(dataType)) {
            if (!attr.name().equals(pair.key())) {
                continue;
            }
            if (gm.isMulti(attr)
                    || MetaFieldGenerator.detectMetaKind(attr)
                            != MetaFieldGenerator.MetaKind.NONE) {
                return null;
            }
            RType itemType = gm.getType(attr);
            return itemType == null || itemType instanceof RMissingType
                    ? null
                    : translator.toJavaReferenceType(itemType);
        }
        return null;
    }

    /**
     * v3.2 seat 7 (F11, facet switchLambdaArmAdmit): the EXPECTED type threaded into a switch ARM compile — an int literal
     * under a {@code BigDecimal}-typed switch result compiles to {@code MapperS.of(BigDecimal.valueOf(N))} (golden
     * NumberArms; upstream's {@code TypeCoercionService} coerces every arm to the switch's join type). ONE declaration for
     * the SET seat's conditional / switch arm ({@code FunctionExpressionRenderer.conditionalArmExpectedType} consults it)
     * and the block-lambda switch seats ({@code CollectionHandler}'s instanceof, option-getter, enum and literal-guard
     * blocks and their default arms) — LAW 69. {@code null} for every other arm: no coercion threaded, the pre-seat compile.
     */
    public static JavaType intLiteralArmExpectedType(RExpression arm, String resultSimpleName,
            JavaTypeUtil typeUtil) {
        if (arm instanceof RIntLiteral && "BigDecimal".equals(resultSimpleName) && typeUtil != null) {
            return typeUtil.BIG_DECIMAL;
        }
        return null;
    }

    /**
     * v3.2 seat 9 (PR #630, F8 / D47 — the {@code nothing} render law): the Java ITEM type of an expression read
     * from the FRONT END — the workspace's inferred type through the SAME translator the D47 mapping feeds
     * ({@code JavaTypeTranslator.toJavaReferenceType}: {@code nothing} → {@code java.lang.Void}) — for the seats
     * whose compiled value carries no Java type (the operation seat's value and the existence operand: the #433
     * untyped class, measured at the seat: {@code getExpressionType() == null} at both). {@code null} when the
     * inference is absent or MISSING, when the type carries meta annotations (a {@code FieldWithMetaX} item is
     * NOT the Void item — upstream's coercion leaves it alone: WITNESSED at round 1 by the re-pinned edge group's
     * MetaCarrier.TokPresent, where the released plugin keeps the {@code FieldWithMetaVoid} getter under
     * {@code exists} — and that witness CAUGHT the seat's read: the front end's inferred type of an attribute
     * reference carries NO meta attributes ({@code TypeJoin} is the one producer of a meta-carrying
     * {@code RMetaAnnotatedType}), so the {@code hasMeta()} read never fired and the fork rendered the empty mapper
     * ({@code scratch/holdout-r1a-dump/}, local); the guard is STRUCTURAL since round 1 — the operand's RESOLVED
     * attribute's meta kind, {@code MetaFieldGenerator.detectMetaKind}, the generator's own read at its other meta
     * seats — with lane L12 dropping it; the {@code hasMeta()} read stays for the joined-type channel), when no
     * generator model / translator is wired (the
     * no-arg handler-test compiler), or for the SYNTHETIC implicit operand of a {@code then exists} / bare
     * {@code filter … exists} (a node the inference fixed point never saw — MISSING — so the Void law never reaches
     * that family on either route; round 1, cq SF-5, BANKED). The consumer applies
     * {@code TypeCoercionService.isNullOrVoidItem} to the result — the coercer's own early-exit predicate, ONE law
     * at every seat (LAW 69).
     *
     * <p>{@code null} too for a WITH-META construction ({@code empty with-meta { reference: href }}): the front end
     * types it by its ARGUMENT ({@code nothing}), but upstream mints its Java type from the CONTEXT — the meta
     * wrapper ({@code caseWithMetaOperation}: {@code ReferenceWithMetaX.builder().setValue(null)…build()}, the
     * argument alone coerced to {@code null} inside) — so the coercion at the operation seat never sees a Void item.
     * MEASURED, not argued: the first cut of the render law read the argument's type through this helper and moved
     * 14 cdm 6.20.2 / drr 7.x FUNCTION files on both routes ({@code target/v32-seat9-instruments/scratch/d11-f8r1.status},
     * every one an {@code empty with-meta} assignment — {@code quantityReference = null;} where the golden builds the
     * wrapper); the seat suite's control pins the shape. ({@code asKey} is a boolean flag on the operation and the
     * key-value pair, not a node kind — nothing else mints a type of its own there; the ring is the receipt.)
     *
     * <p>Round 2 (the code-quality review's SF-2) extended that with-meta decline to a construction nested as an ARM
     * of a JOIN CONTAINER — a conditional's two arms, a switch's case expressions, a {@code default}'s two operands,
     * a {@code then}'s body or a list literal's elements — walked recursively by {@code joinCarriesWithMetaArm} (five
     * container kinds; the recursion is depth-bounded — round 3, cq NIT-2). No fixture reaches the four non-conditional
     * containers with a with-meta arm (BANKED); the conditional spelling is the cdm ingest shape above.
     */
    public static JavaType inferredJavaType(RExpression expr, ExpressionCompiler compiler) {
        if (expr == null || compiler == null
                || expr instanceof com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr
                || joinCarriesWithMetaArm(expr)) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        if (gm == null || gm.workspace() == null || tt == null) {
            return null;
        }
        // round 1: the STRUCTURAL meta guard - a [metadata …] attribute's Java item is the wrapper, never Void
        RAttribute bound = boundAttributeOf(expr);
        if (bound != null && MetaFieldGenerator.detectMetaKind(bound) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RMetaAnnotatedType inferred = gm.workspace().getInferredType(expr);
        if (inferred == null || inferred.isMissing() || inferred.hasMeta()) {
            return null;
        }
        return tt.toJavaReferenceType(inferred.type());
    }

    /**
     * Round 1's whole-suite catch (the verify at the commit-10 head: cdm 6.2x / drr 7.x FUNCTION 4-5 mismatches per cell,
     * every one {@code set partyReference: if href exists then empty with-meta { reference: href }}): a CONDITIONAL whose
     * arm chain carries a with-meta construction is typed {@code nothing} by the front end (the join of an argument-typed
     * arm), but upstream mints the WRAPPER from the context arm by arm - the same exclusion as the plain with-meta.
     * Round 2 (the code-quality review's SF-2): the walk covers EVERY join container whose value can be a with-meta arm,
     * recursively - a conditional's two arms, a switch's case expressions, a {@code default}'s two operands, a
     * {@code then}'s body and a list literal's elements - not the conditional spelling alone (the one the vendored
     * corpus carried). No fixture reaches the four other containers with a with-meta arm (BANKED; the conditional
     * spelling's own witness is the cdm ingest unit test, shadowed alone by the META-OUTPUT decline at the render seat -
     * the c12 sweep's L18; the PAIR is lane L20's).
     */
    private static boolean joinCarriesWithMetaArm(RExpression expr) {
        return joinCarriesWithMetaArm(expr, 0);
    }

    // round 3 (the code-quality review's NIT-2): the recursion is bounded (LAW 69's #629 precedent -
    // ROnlyExistsElement.encloses at 64; the parser's own nesting limit is well under this), so a pathological
    // hand-crafted AST cannot StackOverflow this helper, now called for every existence operand and operation value.
    private static final int JOIN_WALK_MAX_DEPTH = 64;

    private static boolean joinCarriesWithMetaArm(RExpression expr, int depth) {
        if (depth > JOIN_WALK_MAX_DEPTH) {
            return false;
        }
        if (expr instanceof RConditionalExpr cond) {
            return armIsWithMeta(cond.thenBranch(), depth) || armIsWithMeta(cond.elseBranch().orElse(null), depth);
        }
        if (expr instanceof com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr sw) {
            for (com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase c : sw.cases()) {
                if (armIsWithMeta(c.expression(), depth)) {
                    return true;
                }
            }
            return false;
        }
        if (expr instanceof com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr d) {
            return armIsWithMeta(d.left().orElse(null), depth) || armIsWithMeta(d.right().orElse(null), depth);
        }
        if (expr instanceof com.regnosys.rosetta.ast.expressions.binary.RThenExpr t) {
            return t.body().isPresent() && armIsWithMeta(t.body().get().body(), depth);
        }
        if (expr instanceof com.regnosys.rosetta.ast.expressions.literals.RListLiteral l) {
            for (RExpression e : l.elements()) {
                if (armIsWithMeta(e, depth)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A join arm is a with-meta construction itself or a join container carrying one (recursive, depth-bounded). */
    private static boolean armIsWithMeta(RExpression arm, int depth) {
        return arm instanceof com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr
                || (arm != null && joinCarriesWithMetaArm(arm, depth + 1));
    }

    /**
     * The attribute an expression is BOUND to, if it is a plain attribute reference: a feature call's resolved
     * attribute, or a bare symbol reference the linker bound to an attribute (the implicit-receiver form inside a
     * condition); {@code null} for every other shape.
     */
    private static RAttribute boundAttributeOf(RExpression expr) {
        if (expr instanceof RFeatureCall fc) {
            return fc.resolvedFeature().orElse(null);
        }
        // round 2 (the code-quality review's SF-1): the DEEP-path form is its own node kind, not an RFeatureCall - the
        // same last-hop attribute (the oracle group void-mapping-deep-tok's DeepTok pins the render; round 3 cq SF-1 /
        // spec SF-4 corrected the group name - render-hoist-second's FIRST cut carried a DeepTok over a PLAIN receiver,
        // which the plugin REFUSED, so the shape moved to void-mapping-deep-tok over a choice)
        if (expr instanceof com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall dfc) {
            return dfc.resolvedFeature().orElse(null);
        }
        if (expr instanceof RSymbolReference sr && sr.symbol().orElse(null) instanceof RAttribute attr) {
            return attr;
        }
        return null;
    }
}
