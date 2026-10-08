package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.expressions.binary.*;
import com.regnosys.rosetta.ast.expressions.constructors.*;
import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.ast.expressions.references.*;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.unary.*;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Computes the inferred type of a single expression. Called by
 * {@link TypeInferenceEngine} during each fixed-point iteration.
 *
 * <p>Tasks 8-12 add expression-specific cases via instanceof pattern
 * matching. This scaffold returns MISSING for all expressions.
 *
 * <p>Spec: section 3.4 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public final class ExpressionTypeComputer {

    private final BuiltinTypeRegistry builtins;
    private final SubtypeRelation subtypeRelation;
    private final TypeJoin typeJoin;
    private final TypeAliasSolver aliasSolver;

    public ExpressionTypeComputer(
            BuiltinTypeRegistry builtins,
            SubtypeRelation subtypeRelation,
            TypeJoin typeJoin,
            TypeAliasSolver aliasSolver) {
        this.builtins = builtins;
        this.subtypeRelation = subtypeRelation;
        this.typeJoin = typeJoin;
        this.aliasSolver = aliasSolver;
    }

    /**
     * Computes the type of the given expression. Uses the engine to look
     * up already-inferred types of sub-expressions (fixed-point reads).
     *
     * @param expr the expression to type
     * @param engine the inference engine (for sub-expression type lookups)
     * @return the inferred type, or MISSING if not yet determinable
     */
    public RMetaAnnotatedType compute(RExpression expr, TypeInferenceEngine engine) {
        return switch (expr) {
            // === T8: Literals ================================================
            case RIntLiteral lit -> computeIntLiteral(lit);
            case RNumberLiteral lit -> computeNumberLiteral(lit);
            case RStringLiteral lit -> computeStringLiteral(lit);
            case RBooleanLiteral lit -> RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
            case REmptyLiteral lit -> RMetaAnnotatedType.withNoMeta(RBasicType.NOTHING);
            case RListLiteral lit -> computeListLiteral(lit, engine);

            // === T8: Binary operators ========================================
            case RArithmeticExpr arith -> computeArithmetic(arith, engine);
            case RComparisonExpr comp -> RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
            case REqualityExpr eq -> RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
            case RLogicalExpr log -> RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);

            // === T9: References + function calls =================================
            case RSymbolReference ref -> computeSymbolRef(ref, engine);
            case RFeatureCall fc -> computeFeatureCall(fc, engine);
            case RDeepFeatureCall dfc -> computeDeepFeatureCall(dfc);
            case REnumValueRef enr -> computeEnumValueRef(enr, engine);
            case RImplicitVariable iv -> {
                // Category 8 in TypeInferenceEngine populates implicit-item
                // type during type-directed resolution. Read it here (MISSING
                // if Category 8 hasn't run yet; fixed-point will converge on a
                // later iteration).
                yield engine.getInferredType(iv);
            }

            // === T10: Conditionals + switch + constructors =======================
            case RConditionalExpr cond -> computeConditional(cond, engine);
            case RSwitchExpr sw -> computeSwitch(sw, engine);
            case RConstructorExpr ctor -> computeConstructor(ctor);

            // === T11: List operations + unary ====================================
            case RExtractExpr ext -> computeBodyType(ext.body(), engine);
            case RFilterExpr flt -> computeArgumentType(flt, engine);
            case RReduceExpr red -> computeBodyType(red.body(), engine);
            case RSortExpr srt -> computeArgumentType(srt, engine);
            case RListOpExpr lop -> computeListOp(lop, engine);
            case RCountExpr cnt -> RMetaAnnotatedType.withNoMeta(RNumberType.intType());
            case RExistenceExpr ex -> RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
            case RCardinalityCheckExpr cc -> RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
            case RMaxExpr mx -> computeArgumentType(mx, engine);
            case RMinExpr mn -> computeArgumentType(mn, engine);
            case RToStringExpr ts -> RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
            case ROnlyExistsExpr oe -> RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
            case RWithMetaExpr wm -> computeArgumentType(wm, engine);

            // === T12: Remaining expressions ==================================
            case RConversionExpr conv -> computeConversion(conv);
            case RThenExpr then -> computeThenExpr(then, engine);
            case RJoinExpr join -> RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
            case RDefaultExpr def -> computeArgumentType(def, engine);
            case RContainsExpr cont -> RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
            case RDisjointExpr dis -> RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);

            default -> RMetaAnnotatedType.MISSING;
        };
    }

    // === Literal type computation ============================================

    private RMetaAnnotatedType computeIntLiteral(RIntLiteral lit) {
        if (lit.value() == null) return RMetaAnnotatedType.MISSING;
        int digits = lit.value().toString().length();
        if (lit.value().signum() < 0) digits--; // don't count minus sign
        var type = new RNumberType(
            OptionalInt.of(digits), OptionalInt.of(0),
            Optional.empty(), Optional.empty());
        return RMetaAnnotatedType.withNoMeta(type);
    }

    private RMetaAnnotatedType computeNumberLiteral(RNumberLiteral lit) {
        if (lit.value() == null) return RMetaAnnotatedType.MISSING;
        BigDecimal val = lit.value();
        int totalDigits = val.precision();
        int fracDigits = Math.max(0, val.scale());
        var type = new RNumberType(
            OptionalInt.of(totalDigits), OptionalInt.of(fracDigits),
            Optional.empty(), Optional.empty());
        return RMetaAnnotatedType.withNoMeta(type);
    }

    private RMetaAnnotatedType computeStringLiteral(RStringLiteral lit) {
        if (lit.value() == null) return RMetaAnnotatedType.MISSING;
        int len = lit.value().length();
        var type = new RStringType(
            OptionalInt.of(len), OptionalInt.of(len), Optional.empty());
        return RMetaAnnotatedType.withNoMeta(type);
    }

    private RMetaAnnotatedType computeListLiteral(RListLiteral lit, TypeInferenceEngine engine) {
        if (lit.elements() == null || lit.elements().isEmpty()) {
            return RMetaAnnotatedType.withNoMeta(RBasicType.NOTHING);
        }
        // Join all element types
        RType result = RBasicType.NOTHING;
        for (RExpression elem : lit.elements()) {
            RMetaAnnotatedType elemType = engine.getInferredType(elem);
            result = typeJoin.join(result, elemType.type());
        }
        return RMetaAnnotatedType.withNoMeta(result);
    }

    // === Operator type computation ===========================================

    private RMetaAnnotatedType computeArithmetic(RArithmeticExpr arith, TypeInferenceEngine engine) {
        if (arith.left().isEmpty() || arith.right().isEmpty()) return RMetaAnnotatedType.MISSING;
        RMetaAnnotatedType lhs = engine.getInferredType(arith.left().get());
        RMetaAnnotatedType rhs = engine.getInferredType(arith.right().get());
        if (lhs.isMissing() || rhs.isMissing()) return RMetaAnnotatedType.MISSING;
        // facet dateArithTyping (W42 finding #3, PR #424): upstream's LEFT-keyed date
        // arms (RosettaTypeProvider.caseSubtractOperation: `left <: date` →
        // UNCONSTRAINED_INT — the day-count algebra; caseAddOperation: `left <: date`
        // → DATE_TIME). The alias-stripped view stands in for upstream's
        // isSubtypeOf(left, DATE) — date has no other subtype. dateTime/
        // zonedDateTime operands, string/number arms and everything else keep the
        // fork-native constraint JOIN below (the leg-C finding-#6 record).
        RType lhsBase = aliasSolver.evaluateForward(lhs.type());
        if (lhsBase instanceof RRecordType rec && rec.kind() == RecordKind.DATE) {
            if (arith.op() == ArithOp.MINUS) {
                return RMetaAnnotatedType.withNoMeta(RNumberType.intType());
            }
            if (arith.op() == ArithOp.PLUS) {
                return RMetaAnnotatedType.withNoMeta(RRecordType.DATE_TIME);
            }
        }
        RType joined = typeJoin.join(lhs.type(), rhs.type());
        return RMetaAnnotatedType.withNoMeta(joined);
    }

    // === T11/T12: List operation + unary type computation ===================

    /**
     * Returns the type of the argument (lhs) expression. When the argument
     * is absent (chain ops directly in a rule body — e.g.
     * {@code reporting rule R from Trade: filter active}), falls back to
     * the enclosing {@link com.regnosys.rosetta.ast.functions.RRule}'s
     * {@code fromType} via {@link #inferRuleFromType}.
     */
    private RMetaAnnotatedType computeArgumentType(RExpression expr, TypeInferenceEngine engine) {
        var left = expr.left();
        if (left.isPresent()) {
            return engine.getInferredType(left.get());
        }
        // arg=null. Two cases — ORDER MATTERS, mirroring
        // TypeInferenceEngine.computeArgumentElementType (Phase X1 closure T0i).
        //
        // (a) NESTED chain — this op is piped as another chain op's
        //     inline-function body (e.g. the `filter`/`only-element`/`extract`
        //     in `X then filter .. then only-element then extract f`). Its
        //     input type is the OUTER chain op's argument output, NOT the
        //     rule's fromType. Detect by finding the enclosing
        //     RInlineFunction that wraps THIS op, and recurse on its parent
        //     op. Previously this branch was missing here (it existed only in
        //     the engine's computeArgumentElementType), so a piped filter /
        //     only-element / then computed its output type as the rule's
        //     fromType instead of the upstream chain element type — collapsing
        //     `extract productId then filter .. then only-element then extract
        //     identifier` to the rule's fromType and breaking the downstream
        //     implicit-item resolution (CLOSURE T10).
        // (b) OUTERMOST chain — the op sits directly at the top of a rule
        //     body (no wrapping inline function). Fall back to the rule's
        //     fromType via the shared helper.
        com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction wrappingInline =
                com.regnosys.rosetta.ast.util.AstWalker
                        .findAncestor(expr, com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction.class)
                        .orElse(null);
        if (wrappingInline != null
                && wrappingInline.parent() instanceof RExpression outerOp) {
            return computeArgumentType(outerOp, engine);
        }
        com.regnosys.rosetta.ast.functions.RRule enclosingRule =
                com.regnosys.rosetta.ast.util.AstWalker
                        .findAncestor(expr, com.regnosys.rosetta.ast.functions.RRule.class)
                        .orElse(null);
        return inferRuleFromType(enclosingRule);
    }

    /**
     * Returns the inferred type of a rule's {@code from} typeCall — used by
     * both {@link #computeArgumentType} (rule-body chain op with null
     * argument) and {@link TypeInferenceEngine#computeArgumentElementType}
     * (chain-element type inference) to keep the two call sites in lockstep.
     *
     * <p>Resolution sequence mirrors {@link #inferTypeOfAttribute}:
     * <ol>
     *   <li>If M3 resolved {@code tc.referencedType()} to an AST node that
     *       {@link #astNodeToRType} can translate (currently {@link RDataType}
     *       or {@link REnumeration}), return that {@link RType} wrapped in
     *       {@link RMetaAnnotatedType#withNoMeta}.</li>
     *   <li>Otherwise (M3-unresolved, or resolved to a builtin AST node such
     *       as those carrying {@code string} / {@code int} / {@code boolean}),
     *       fall through to {@link BuiltinTypeRegistry#lookup} keyed by
     *       {@code tc.typeName()}.</li>
     *   <li>If both fail, return {@link RMetaAnnotatedType#MISSING}.</li>
     * </ol>
     *
     * <p>Fix for Copilot PR #76 R10 F1+F2 — the previous inline implementations
     * filtered with {@code instanceof RDataType} only, which silently produced
     * MISSING when the rule's {@code from} type was an enum, choice, basic
     * type, record, or alias. The grammar permits any type-like declaration
     * in the {@code from} position; the typeName-builtin fallback below now
     * covers builtin types (which {@link #astNodeToRType} otherwise can't
     * translate). <b>Choice</b> {@code from}-types now resolve via
     * {@link #astNodeToRType}'s {@code RChoice} branch (added later as
     * Gap&nbsp;I). <b>Alias</b> ({@code RTypeAlias}) {@code from}-types resolve
     * via the {@code aliasSolver} branch in the body below: their resolution
     * lives outside the builtin registry, and {@link #astNodeToRType} lacks the
     * {@code RTypeCall} that {@code evaluateAliasBody} needs for parametric
     * reconstruction, so the alias case is handled here (scoped to the rule-from
     * path) rather than widening {@link #astNodeToRType} and its many callers.
     *
     * @return MISSING when {@code rule == null} or the rule has no fromType.
     */
    public RMetaAnnotatedType inferRuleFromType(com.regnosys.rosetta.ast.functions.RRule rule) {
        if (rule == null) return RMetaAnnotatedType.MISSING;
        var tc = rule.fromType().orElse(null);
        if (tc == null) return RMetaAnnotatedType.MISSING;
        var resolved = tc.referencedType();
        if (resolved.isPresent()) {
            RType rt = astNodeToRType(resolved.get());
            if (rt != RMissingType.INSTANCE) {
                return RMetaAnnotatedType.withNoMeta(rt);
            }
            // Alias (RTypeAlias) from-type: astNodeToRType returns
            // MISSING for an alias (its resolution lives outside the builtin
            // registry). Resolve it exactly as inferTypeOfAttribute does — the
            // alias body via TypeAliasSolver (D35), with the alias's terminal
            // base builtin as a type-identity fallback for the constrained
            // cases evaluateAliasBody cannot parametrically reconstruct.
            // (Choice from-types already resolve above via astNodeToRType.)
            if (resolved.get() instanceof RTypeAlias alias) {
                RType aliasType = aliasSolver.evaluateAliasBody(alias, tc);
                if (aliasType != RMissingType.INSTANCE) {
                    return RMetaAnnotatedType.withNoMeta(aliasType);
                }
                RType base = aliasBaseBuiltin(alias);
                if (base != RMissingType.INSTANCE) {
                    return RMetaAnnotatedType.withNoMeta(base);
                }
            }
        }
        String typeName = tc.typeName();
        if (typeName != null) {
            var builtin = builtins.lookup(typeName);
            if (builtin.isPresent()) {
                return RMetaAnnotatedType.withNoMeta(builtin.get());
            }
        }
        return RMetaAnnotatedType.MISSING;
    }

    /** Returns the type of the inline function body. */
    private RMetaAnnotatedType computeBodyType(
            com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction body,
            TypeInferenceEngine engine) {
        if (body == null || body.body() == null) return RMetaAnnotatedType.MISSING;
        return engine.getInferredType(body.body());
    }

    private RMetaAnnotatedType computeListOp(RListOpExpr lop, TypeInferenceEngine engine) {
        return switch (lop.op()) {
            case ONLY_ELEMENT, FLATTEN, DISTINCT, REVERSE, LAST ->
                computeArgumentType(lop, engine);
            case FIRST -> computeArgumentType(lop, engine);
            // facet sumArgItemTyping (W42 finding #19, PR #432): upstream types a
            // sum as its ARGUMENT's item type (RosettaTypeProvider.caseSumOperation
            // = safeRType(expr.getArgument())) — an int-list sum is int, so a list
            // literal like `[item count, item sum]` joins <Integer>, not the
            // unconstrained-number BigDecimal the old fallback forced (which also
            // dragged a spurious `.map("Type coercion", …intValueExact())` tail
            // onto int-typed outputs). Number-item sums still read number.
            case SUM -> computeArgumentType(lop, engine);
        };
    }

    private RMetaAnnotatedType computeConversion(RConversionExpr conv) {
        return switch (conv.kind()) {
            case ENUM -> {
                // to-enum: target type is the resolved enum
                if (conv.targetEnum().isPresent()) {
                    yield RMetaAnnotatedType.withNoMeta(new REnumTypeRef(conv.targetEnum().get()));
                }
                yield RMetaAnnotatedType.MISSING;
            }
            case NUMBER -> RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
            case INT -> RMetaAnnotatedType.withNoMeta(RNumberType.intType());
            case TIME -> RMetaAnnotatedType.withNoMeta(RBasicType.TIME);
            case DATE -> RMetaAnnotatedType.withNoMeta(RRecordType.DATE);
            case DATE_TIME -> RMetaAnnotatedType.withNoMeta(RRecordType.DATE_TIME);
            case ZONED_DATE_TIME -> RMetaAnnotatedType.withNoMeta(RRecordType.ZONED_DATE_TIME);
        };
    }

    private RMetaAnnotatedType computeThenExpr(RThenExpr then, TypeInferenceEngine engine) {
        // then expr: argument then body — type is body's type
        if (then.body().isPresent() && then.body().get().body() != null) {
            return engine.getInferredType(then.body().get().body());
        }
        return computeArgumentType(then, engine);
    }

    // === T10: Conditional + switch + constructor type computation ===========

    private RMetaAnnotatedType computeConditional(RConditionalExpr cond, TypeInferenceEngine engine) {
        RMetaAnnotatedType thenType = engine.getInferredType(cond.thenBranch());
        if (thenType.isMissing()) return RMetaAnnotatedType.MISSING;

        RType result = thenType.type();
        if (cond.elseBranch().isPresent()) {
            RMetaAnnotatedType elseType = engine.getInferredType(cond.elseBranch().get());
            if (elseType.isMissing()) return RMetaAnnotatedType.MISSING;
            result = typeJoin.join(result, elseType.type());
        }
        return RMetaAnnotatedType.withNoMeta(result);
    }

    private RMetaAnnotatedType computeSwitch(RSwitchExpr sw, TypeInferenceEngine engine) {
        RType result = RBasicType.NOTHING;
        for (RSwitchCase sc : sw.cases()) {
            if (sc.expression() != null) {
                RMetaAnnotatedType caseType = engine.getInferredType(sc.expression());
                if (!caseType.isMissing()) {
                    result = typeJoin.join(result, caseType.type());
                }
            }
        }
        return RMetaAnnotatedType.withNoMeta(result);
    }

    private RMetaAnnotatedType computeConstructor(RConstructorExpr ctor) {
        RTypeCall tc = ctor.typeCall();
        if (tc == null) return RMetaAnnotatedType.MISSING;

        // M3 resolves the typeCall; read the resolved type
        var resolved = tc.referencedType();
        if (resolved.isPresent()) {
            return RMetaAnnotatedType.withNoMeta(astNodeToRType(resolved.get()));
        }
        // Fallback to builtin registry
        if (tc.typeName() != null) {
            var builtin = builtins.lookup(tc.typeName());
            if (builtin.isPresent()) return RMetaAnnotatedType.withNoMeta(builtin.get());
        }
        return RMetaAnnotatedType.MISSING;
    }

    // === T9: Reference + function call type computation ====================

    private RMetaAnnotatedType computeSymbolRef(RSymbolReference ref, TypeInferenceEngine engine) {
        var resolved = ref.symbol();
        // v3.1 C1 (spec R6) — A TYPE IS NOT A SYMBOL, SO IT DOES NOT GET TO
        // DECIDE THE TYPE OF A BARE NAME.
        //
        // Upstream filters the symbol scope to RosettaSymbol implementors; a
        // Data is not one and (since Choice extends Data) neither is a choice,
        // so a top-level type NEVER competes for a bare name. The fork's global
        // pass carries no such filter, so `Index` inside an extract over an
        // Underlier binds the GLOBAL `choice Index` into the legacy symbol slot
        // — while the C1 authority slot correctly holds the item's ChoiceOption.
        // Typing then asked the legacy slot, got a type-name-as-expression, and
        // returned MISSING; the deep call hanging off it had no receiver type
        // and could not resolve. Measured on the a2 conformance snippet:
        //   SYM 'Index'    symbol=RChoice   featNode=RChoiceOption  type=MISSING
        //   SYM 'Security' symbol=RDataType featNode=RChoiceOption  type=MISSING
        //
        // Where the legacy binding is NOT something upstream's symbol scope
        // could hold, the authority slot answers instead. Narrow by
        // construction: a legacy binding that IS an upstream symbol (an input,
        // an alias, an enumeration) still wins, so every currently-green typing
        // decision is untouched. Un-binding the legacy slot would be the fuller
        // fix and is the ladder-retirement follow-on; it flips render arms keyed
        // on that bind state, which is why C1 does not do it here.
        if (resolved.isPresent()
                && !com.regnosys.rosetta.symbols.UpstreamSymbolKinds.isUpstreamSymbol(resolved.get())
                && ref.resolvedFeatureNode().isPresent()) {
            com.regnosys.rosetta.ast.RNode authority = ref.resolvedFeatureNode().get();
            if (authority instanceof RChoiceOption option) {
                return typeOfChoiceOption(option);
            }
            return inferTypeOfNode(authority, engine, ref);
        }
        if (resolved.isEmpty()) {
            // v3.1 C1 — a bare name that names a FEATURE of the implicit item
            // (spec R7.1) binds into resolvedFeatureNode, because the legacy
            // symbol slot carries a narrower contract. Before this, such a name
            // typed MISSING even though it was fully resolved — which is what
            // left `... then <Option> then extract <feature>` with no item type
            // to resolve the next hop against, the dominant drr 7.x shape.
            // Additive: fires only where symbol() is empty, so no existing
            // binding's type changes.
            var feature = ref.resolvedFeatureNode();
            if (feature.isPresent()) {
                if (feature.get() instanceof RChoiceOption option) {
                    return typeOfChoiceOption(option);
                }
                return inferTypeOfNode(feature.get(), engine, ref);
            }
            return RMetaAnnotatedType.MISSING;
        }
        return inferTypeOfNode(resolved.get(), engine, ref);
    }

    /**
     * The type a choice OPTION names. Upstream never needs this: an option there
     * IS an attribute, so its type is its type call like any other. The fork
     * models options separately, so the step is explicit.
     *
     * <p>Data and choice targets keep their populated refs (the choice view is
     * this class's own walk); EVERYTHING ELSE delegates to
     * {@link #inferTypeOfTypeCall} — the same channel an attribute's typeCall
     * resolves through — because the grammar makes {@code choiceOption :
     * typeCall}, the same production {@code attribute} uses, so anything an
     * attribute can be typed as is admissible in an option slot. Upstream
     * ACCEPTS a type alias there (probe P10, adopted: zero errors, the option
     * binds {@code RosettaTypeAlias}); mapping only data/choice returned
     * MISSING for it — a silent type loss no conformance gate could see, pinned
     * by {@code ChoiceOptionAliasTypingTest} (v3.1 C1 part 2, SF-8). Zero
     * corpus carriers (80 choices, 173 aliases, no overlap) — latent, and the
     * seed/ring were re-measured anyway when this landed.
     */
    private RMetaAnnotatedType typeOfChoiceOption(RChoiceOption option) {
        var typeCall = option.typeCall();
        if (typeCall == null) {
            return RMetaAnnotatedType.MISSING;
        }
        var referenced = typeCall.referencedType();
        if (referenced.isPresent()) {
            if (referenced.get() instanceof RDataType dt) {
                return RMetaAnnotatedType.withNoMeta(new RDataTypeRef(dt));
            }
            if (referenced.get() instanceof com.regnosys.rosetta.ast.types.RChoice ch) {
                return RMetaAnnotatedType.withNoMeta(
                        choiceToRType(ch, new java.util.HashSet<>()));
            }
        }
        return inferTypeOfTypeCall(typeCall);
    }

    private RMetaAnnotatedType computeFeatureCall(RFeatureCall fc, TypeInferenceEngine engine) {
        var resolved = fc.resolvedFeature();
        if (resolved.isPresent()) {
            return inferTypeOfAttribute(resolved.get());
        }
        // Phase X1 closure (Gap J) — choice-option navigation. The feature is
        // NOT an attribute: when the receiver is a (Gap-I populated) choice
        // whose option type-name equals the feature name, the expression
        // narrows to that option's type (Rune `<choiceExpr> -> OptionType`
        // selection). The receiver type comes from the fixed-point engine, so
        // it is non-MISSING once the receiver chain has converged.
        var receiver = fc.left();
        if (receiver.isPresent()) {
            RMetaAnnotatedType recvType = engine.getInferredType(receiver.get());
            if (!recvType.isMissing()) {
                if (recvType.type() instanceof RChoiceTypeRef choice) {
                    for (RType option : choice.options()) {
                        if (fc.featureName().equals(option.name())) {
                            return RMetaAnnotatedType.withNoMeta(option);
                        }
                    }
                }
                // Phase X1 closure T10 — data-type-extends-choice option
                // navigation. When the receiver is a DATA TYPE whose super-type
                // chain reaches a choice (Rune `type Foo extends ChoiceBar`), an
                // option-name feature selects that option. Mirrors the corpus
                // `Observable -> Basket -> basketConstituent -> Asset` where
                // `type BasketConstituent extends Observable` (Observable is a
                // choice; Asset is one of its options). The RChoiceTypeRef branch
                // above only fires when the receiver is the choice itself; here
                // the choice is reached via the data type's super-type.
                if (recvType.type() instanceof RDataTypeRef dtr) {
                    var choiceSuper = choiceSuperTypeInChain(dtr.astNode());
                    if (choiceSuper.isPresent()
                            && choiceToRType(choiceSuper.get(), new java.util.HashSet<>())
                                    instanceof RChoiceTypeRef choice) {
                        for (RType option : choice.options()) {
                            if (fc.featureName().equals(option.name())) {
                                return RMetaAnnotatedType.withNoMeta(option);
                            }
                        }
                    }
                }
                // Phase X1 closure T10 — builtin record-field navigation. The
                // feature is NOT an RAttribute: when the receiver is a builtin
                // record type (date / dateTime / zonedDateTime per
                // basictypes.rosetta) and the feature names one of its fields,
                // the expression's type is that field's builtin type (e.g.
                // zonedDateTime -> date narrows to the date record). The
                // receiver type comes from the fixed-point engine, so it is
                // non-MISSING once the receiver chain has converged. Mirrors the
                // dominant corpus shape EffectiveDate's
                // `PositionForEvent -> openDateTime -> date`.
                if (recvType.type() instanceof RRecordType record) {
                    var fieldType = recordFieldType(record, fc.featureName());
                    if (fieldType.isPresent()) {
                        return RMetaAnnotatedType.withNoMeta(fieldType.get());
                    }
                }
            }
            // Post-parity finding #21 (PR #433) — unresolved META-FEATURE reads.
            // The fork has no RosettaMetaType node, so `-> scheme` / `-> reference`
            // never links (the render side's metaPathShortForm law, PR #348);
            // upstream types both meta features as string
            // (RosettaTypeProvider — metaType scheme/reference are string-typed).
            // Gated on the receiver's TERMINAL symbol carrying a [metadata …]
            // annotation on its own AST (a declared attribute or a called
            // function's output), so an unresolved feature merely NAMED
            // scheme/reference on a meta-free receiver stays MISSING.
            String metaFeatureName = fc.featureName();
            if (("scheme".equals(metaFeatureName) || "reference".equals(metaFeatureName))
                    && receiverCarriesMetadataAnnotation(receiver.get())) {
                return RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
            }
        }
        return RMetaAnnotatedType.MISSING;
    }

    /**
     * Whether the receiver's terminal symbol declares a {@code [metadata …]}
     * annotation on its own AST: a symbol reference to a meta-annotated
     * attribute ({@code myInput -> scheme}) or a call to a function whose
     * output attribute is meta-annotated ({@code A(myInput) -> scheme}). The
     * gate for the finding-#21 meta-feature typing arm — reads the same
     * annotation surface the generator's {@code metaWrapperOf} resolution
     * keys on, so the typing and render halves admit the same shapes.
     */
    private static boolean receiverCarriesMetadataAnnotation(RExpression receiver) {
        RAttribute terminal = null;
        if (receiver instanceof RSymbolReference sr) {
            var sym = sr.symbol().orElse(null);
            if (sym instanceof RAttribute a) {
                terminal = a;
            } else if (sym instanceof RFunction fn) {
                terminal = fn.output().orElse(null);
            }
        }
        if (terminal == null) {
            return false;
        }
        return hasMetadataAnnotation(terminal);
    }

    /**
     * Maps a builtin record field to its type, gated by the receiver's own
     * field list so a non-field name (e.g. {@code zonedDateTime -> day}) stays
     * MISSING rather than fabricating a type. Field types per
     * {@code basictypes.rosetta}: day/month/year → int; date → the date record;
     * time → time; timezone → string.
     */
    private static Optional<RType> recordFieldType(RRecordType record, String featureName) {
        boolean isField = record.features().stream()
                .anyMatch(f -> f.name().equals(featureName));
        if (!isField) {
            return Optional.empty();
        }
        return switch (featureName) {
            case "day", "month", "year" -> Optional.of(RNumberType.intType());
            case "date" -> Optional.of(RRecordType.DATE);
            case "time" -> Optional.of(RBasicType.TIME);
            case "timezone" -> Optional.of(RStringType.unconstrained());
            default -> Optional.empty();
        };
    }

    private RMetaAnnotatedType computeDeepFeatureCall(RDeepFeatureCall dfc) {
        var resolved = dfc.resolvedFeature();
        if (resolved.isEmpty()) return RMetaAnnotatedType.MISSING;
        return inferTypeOfAttribute(resolved.get());
    }

    /**
     * Computes the type of an REnumValueRef. The grammar emits this node
     * for the {@code EnumName -> ValueName} syntax. Four resolution paths:
     * <ol>
     * <li>Standard enum-value resolution → type is the enumeration.</li>
     * <li>Phase X1 Category 10 attribute-feature chain fallback → type is
     *     the feature attribute's type.</li>
     * <li>Phase X1 closure T0i (Gap B) verified type-restriction (downcast)
     *     → type is the narrowed restriction RDataType. Reads ONLY the
     *     verified {@link REnumValueRef#resolvedTypeRestriction()} record
     *     (the Phase-A speculative {@code resolvedRestrictionType} is the
     *     Phase-A→Phase-B handoff; if Phase B's subtype check failed, that
     *     speculative field is still set but the verified record is empty
     *     and this branch does not fire — MISSING falls through correctly).
     * </li>
     * <li>None of the above → MISSING.</li>
     * </ol>
     */
    private RMetaAnnotatedType computeEnumValueRef(REnumValueRef enr, TypeInferenceEngine engine) {
        var chain = enr.resolvedAttributeChain();
        if (chain.isPresent()) {
            return inferTypeOfAttribute(chain.get().feature());
        }
        // IR-Lab cascade fix (2026-06-27) — lexical-head-nav feature binding. When a
        // disguised `<head> -> <feature>` head resolves in the LEXICAL context —
        // the enclosing FUNCTION's input (2026-06-27), the data type declaring the
        // enclosing condition (#443), the enclosing function's ALIAS (#447 —
        // the bind that makes chained aliases type through the fixed point), or
        // the enclosing function's OUTPUT (#449 — the clearing-to-BIND wave) —
        // TypeInferenceEngine binds <feature> (looked up on the head's type) into a
        // TYPING-ONLY field; the nav types as that feature's type. The field is
        // deliberately separate from resolvedAttributeChain (which the Path-1
        // generator reads to RENDER the nav) — a separation-of-concerns boundary, not a
        // byte-neutrality claim: the generator DOES consult getInferredType, so exposing
        // this type is byte-POSITIVE (the PR #279 +6 FUNCTION flips; the #447
        // alias exposure landed with the generator's meta-coercion seat aligned).
        var inputFeature = enr.resolvedInputFeature();
        if (inputFeature.isPresent()) {
            return inferTypeOfAttribute(inputFeature.get());
        }
        // Phase X1 closure T10 — choice-option navigation (`<choiceAttr> ->
        // <OptionName>`). TypeInferenceEngine bound the matched option's AST
        // node (RDataType | RChoice) when the LHS attribute is choice-typed and
        // <OptionName> names one of its options. astNodeToRType handles both
        // (RDataType → RDataTypeRef; RChoice → fully-populated RChoiceTypeRef).
        var choiceOption = enr.resolvedChoiceOption();
        if (choiceOption.isPresent()) {
            RType ot = astNodeToRType(choiceOption.get());
            if (ot != RMissingType.INSTANCE) {
                return RMetaAnnotatedType.withNoMeta(ot);
            }
        }
        var restriction = enr.resolvedTypeRestriction();
        if (restriction.isPresent()) {
            return RMetaAnnotatedType.withNoMeta(
                new RDataTypeRef(restriction.get().restrictionType()));
        }
        var en = enr.enumeration();
        if (en.isPresent()) {
            return RMetaAnnotatedType.withNoMeta(new REnumTypeRef(en.get()));
        }
        // Phase X1 closure T10 — rule/func-output record-field navigation.
        // `<RuleRef> -> <recordField>` where the LHS resolved (via
        // GlobalResolutionPass.resolveEnumValueRef) to a rule/func whose OUTPUT
        // type is a builtin record (date / dateTime / zonedDateTime) and
        // valueName is one of its fields. e.g. EventDate's
        // `extract ValuationTimestamp -> date`: the ValuationTimestamp rule
        // outputs a zonedDateTime; `date` is a record field. The Gap A branch in
        // TypeInferenceEngine binds an AttributeChain only for func outputs whose
        // feature is a real RAttribute (symbolOutputAttribute is RFunction-only,
        // and record fields are not RAttributes), so this REnumValueRef stays
        // unbound; compute its type here from the resolved symbol's output.
        var sym = enr.resolvedSymbol();
        if (sym.isPresent()) {
            RMetaAnnotatedType outType = inferTypeOfNode(sym.get(), engine, enr);
            if (!outType.isMissing() && outType.type() instanceof RRecordType record) {
                var fieldType = recordFieldType(record, enr.valueName());
                if (fieldType.isPresent()) {
                    return RMetaAnnotatedType.withNoMeta(fieldType.get());
                }
            }
        }
        // Post-parity finding #21 (PR #433) — the DISGUISED meta-feature read.
        // `myInput -> scheme` parses as the disguised 2-name REnumValueRef
        // (head = the function input, value = the meta feature); the fork has no
        // RosettaMetaType node, so no resolution channel ever binds `scheme` /
        // `reference`. Upstream types both meta features as string
        // (RosettaTypeProvider). Same gate as the RFeatureCall arm: the head must
        // name a [metadata …]-annotated input of the ENCLOSING function (or a
        // called function's meta-annotated output via resolvedSymbol above),
        // so an unresolved value merely named scheme/reference on a meta-free
        // head stays MISSING.
        String disguisedValue = enr.valueName();
        if ("scheme".equals(disguisedValue) || "reference".equals(disguisedValue)) {
            RAttribute headInput = disguisedHeadMetaAnnotatedInput(enr);
            if (headInput != null) {
                return RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
            }
            if (sym.isPresent()
                    && sym.get() instanceof RFunction headFn
                    && headFn.output()
                            .filter(ExpressionTypeComputer::hasMetadataAnnotation)
                            .isPresent()) {
                return RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
            }
        }
        // v3.1 C1 part 2 (SF-8, then the drr residue) — the AUTHORITY slot,
        // read ADDITIVELY at the ladder's very end. The R9 arm stamps what
        // lookupFeature found into resolvedFeatureNode; the legacy rungs above
        // carry narrower channels (the T10 rung holds only the option's TARGET
        // TYPE node, which astNodeToRType cannot map for an alias; no legacy
        // rung carries an R9-bound ATTRIBUTE at all) — so a correct binding
        // fell through every rung to MISSING. Two legs:
        // - ChoiceOption -> typeOfChoiceOption (probe P10 / the alias option,
        //   pinned by ChoiceOptionAliasTypingTest);
        // - RAttribute -> the attribute channel (the drr residue's `Security
        //   -> instrumentType = Debt`: the option-headed LHS bound its
        //   attribute in the authority slot, typed MISSING, and the bare RHS
        //   could not seed — pinned by DrrResidueResolutionTest).
        // Placed after every legacy rung, so it can only turn a MISSING into a
        // type, never change an existing one. The metaType kind migrates here
        // at the ladder retirement.
        var authority = enr.resolvedFeatureNode();
        if (authority.isPresent()) {
            if (authority.get() instanceof RChoiceOption option) {
                return typeOfChoiceOption(option);
            }
            if (authority.get() instanceof RAttribute authorityAttribute) {
                return inferTypeOfAttribute(authorityAttribute);
            }
        }
        return RMetaAnnotatedType.MISSING;
    }

    /**
     * The enclosing function's [metadata …]-annotated input the disguised
     * 2-name head names, or {@code null}. The finding-#21 disguised-arm gate:
     * walks the parent chain to the declaring {@link RFunction} and matches
     * {@link REnumValueRef#enumName()} against its declared inputs.
     */
    private static RAttribute disguisedHeadMetaAnnotatedInput(REnumValueRef enr) {
        String head = enr.enumName();
        if (head == null) {
            return null;
        }
        com.regnosys.rosetta.ast.RNode node = enr.parent();
        while (node != null && !(node instanceof RFunction)) {
            node = node.parent();
        }
        if (!(node instanceof RFunction fn)) {
            return null;
        }
        for (RAttribute input : fn.inputs()) {
            if (head.equals(input.name()) && hasMetadataAnnotation(input)) {
                return input;
            }
        }
        return null;
    }

    /** Whether the attribute declares a {@code [metadata …]} annotation. */
    private static boolean hasMetadataAnnotation(RAttribute attr) {
        for (RAnnotationRef ref : attr.annotationRefs()) {
            if ("metadata".equals(ref.annotationName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * THE ONE derivation of a closure parameter's type (v3.2 seat 8 round 1, LAW 69):
     * the declaring lambda's argument ELEMENT type — consulted by the
     * {@code RClosureParameter} arm (a declared parameter's node) and the
     * {@code RInlineFunction} arm (a name-only parameter's lambda) alike; MISSING when
     * the parameter has no declaring lambda.
     */
    private static RMetaAnnotatedType closureParameterType(TypeInferenceEngine engine,
            com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction declaring) {
        return declaring == null
                ? RMetaAnnotatedType.MISSING
                : engine.computeArgumentElementType(declaring.parent());
    }

    /**
     * Infers the type of a resolved node (M3 resolution target).
     * Handles: RAttribute, RFunction, RShortcut, RRule, and other node types.
     *
     * <p>Phase X1 PR #76: added RRule branch. Cross-namespace qualified rule
     * references (e.g. {@code cdeV3.collateral.X}) resolve via the new
     * GlobalResolutionPass walk to an {@link com.regnosys.rosetta.ast.functions.RRule}
     * node; the rule's expression body's inferred type IS the call's type.
     * Uses the engine's fixed-point convergence so chained rule-to-rule
     * dispatch stabilizes across iterations.
     *
     * <p>PR #330 ({@code referringExpr}): a DIRECT self-reference — the referenced
     * rule's whole body IS the referring expression (drr iosco cdeV2
     * {@code ReportingTimestamp}, whose body is {@code cdeV2.datetime.ReportingTimestamp})
     * — reads its own memo entry, so the equation {@code T(body) = T(body)} stays
     * MISSING from the engine's MISSING seed forever. Upstream's least fixed point
     * for the self-cycle is NOTHING (the 9.83.0 golden emits
     * {@code ReportFunction<I, Void>} + {@code output = null} + a self-{@code @Inject});
     * return NOTHING on the identity match. Monotone: only a previously-forever-MISSING
     * equation gains a type — every non-cyclic reference reads the memo exactly as
     * before.
     */
    private RMetaAnnotatedType inferTypeOfNode(com.regnosys.rosetta.ast.RNode node,
            TypeInferenceEngine engine, RExpression referringExpr) {
        if (node instanceof RAttribute attr) {
            return inferTypeOfAttribute(attr);
        }
        if (node instanceof RFunction fn) {
            return fn.output().map(this::inferTypeOfAttribute).orElse(RMetaAnnotatedType.MISSING);
        }
        if (node instanceof com.regnosys.rosetta.ast.functions.RRule rule) {
            if (rule.expression().filter(e -> e == referringExpr).isPresent()) {
                return RMetaAnnotatedType.withNoMeta(RBasicType.NOTHING);
            }
            RMetaAnnotatedType ruleBodyType = rule.expression()
                    .map(engine::getInferredType)
                    .orElse(RMetaAnnotatedType.MISSING);
            // facet ruleRecursionTyping (PR #437, finding #32): a rule call NESTED
            // inside the rule's OWN body (`Fac(item - 1)` in `reporting rule Fac from
            // int: if item = 1 then 1 else item * Fac(item - 1)`) — the #330 identity
            // law widened to the DESCENDANT cycle. Upstream RosettaTypeProvider's
            // cycleTracker seeds every in-computation symbol with NOTHING
            // (safeRType(symbol): cycleTracker.put(symbol, NOTHING_WITH_ANY_META)
            // before computing), so the in-cycle call types BOTTOM and the
            // conditional JOIN collapses it — the rule infers its non-recursive
            // arms' type (int → ReportFunction<Integer, Integer>, the
            // report-rule-recursion oracle golden). Memo-aware: the NOTHING seed
            // fires only while the body's memoized type is STILL MISSING — once the
            // fixed point converges the call reads the settled type (upstream's
            // final tracker state), so the generator's operand walk sees int, not
            // bottom. Monotone: a previously-forever-MISSING equation gains a type;
            // every non-cyclic reference reads the memo exactly as before.
            if (ruleBodyType.isMissing() && referringExpr != null
                    && rule.expression().isPresent()) {
                com.regnosys.rosetta.ast.RExpression body = rule.expression().get();
                for (com.regnosys.rosetta.ast.RNode p = referringExpr; p != null; p = p.parent()) {
                    if (p == body) {
                        return RMetaAnnotatedType.withNoMeta(RBasicType.NOTHING);
                    }
                }
            }
            return ruleBodyType;
        }
        if (node instanceof com.regnosys.rosetta.ast.supporting.REnumValue ev) {
            // v3.1 C1 — UPSTREAM TYPES AN ENUM VALUE BY WHERE IT IS USED, NOT BY
            // WHERE IT IS DECLARED. RosettaTypeProvider, on a reference to a
            // RosettaEnumValue: a FEATURE CALL types as the RECEIVER's type, a
            // BARE reference types as the EXPECTED TYPE at the seat. So `Lei`
            // written bare where PartyIdentifierFormat2Enum is expected types as
            // PartyIdentifierFormat2Enum, even though the value is declared two
            // `extends` levels up in LeiIdentifierFormatEnum.
            //
            // Typing it by its declaring enum instead is what produces BOTH
            // TYPE_ERROR families on the band: the arms of one if/else chain
            // typing as two different enums with no common supertype (upstream's
            // join has no enum arm either, so it would also give `any` — the
            // difference is that upstream never gets there), and the
            // `Expected X but got Y` family where a value found by name search
            // is then typed by whichever enum the search landed in.
            //
            // CONSERVATIVE where upstream is not: upstream returns NOTHING when
            // no expected type is available, which would be a behaviour change on
            // cells that are byte-EXACT today. Here the declaring enum stays as
            // the fallback, so this can only move a seat that HAS an expected
            // type — strictly closer to upstream, never further.
            if (referringExpr != null && engine != null) {
                com.regnosys.rosetta.ast.types.REnumeration expected =
                    engine.expectedEnumAt(referringExpr);
                if (expected != null) {
                    return RMetaAnnotatedType.withNoMeta(
                        new com.regnosys.rosetta.types.REnumTypeRef(expected));
                }
            }
            // Phase X1 T0n (Gap E): a bare RSymbolReference resolved to an enum
            // value (TypeInferenceEngine Category 13) types as its parent
            // enumeration. The REnumValue's parent is the REnumeration it belongs
            // to (set during AST tree wiring; the value comes from
            // REnumeration.values()).
            if (ev.parent() instanceof com.regnosys.rosetta.ast.types.REnumeration en) {
                return RMetaAnnotatedType.withNoMeta(
                    new com.regnosys.rosetta.types.REnumTypeRef(en));
            }
            return RMetaAnnotatedType.MISSING;
        }
        if (node instanceof com.regnosys.rosetta.ast.expressions.supporting.RClosureParameter parameter) {
            // v3.2 seat 8 (F14): a DECLARED closure parameter binds to its own node
            // now (upstream's ClosureParameter); its type is its declaring lambda's —
            // the ONE derivation both binding shapes consult (closureParameterType),
            // which is why lane A1 could not tell the shapes apart by type.
            return closureParameterType(engine, parameter.declaringFunction());
        }
        if (node instanceof com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction inline) {
            // Leg-C findings #11/#16 typing-channel heal (2026-07-18) — a bare
            // closure-parameter reference. LexicalResolutionPass registers every
            // NAME-ONLY closure param (the synthetic `item`; a hand-built lambda)
            // against its DECLARING RInlineFunction node, so a
            // bare `a` inside `intList reduce a, b [ a + b ]` resolves HERE.
            // Before this branch the node fell through to MISSING, which is the
            // channel behind all four #16 faces: arithmetic over closure params
            // fell back to the BigDecimal triple, reduce lost its element-type
            // witness (MapperS<Object> operands), closure-param navs rendered the
            // raw un-witnessed .map(...), and the untyped render path re-used the
            // outer lambda name (the freshname SHADOW).
            //
            // The parameter's ELEMENT type is the declaring closure's argument
            // element type: extract/filter/map/sort/max/min bind their param
            // per-item, reduce binds (T, T) -> T so both params share it, and a
            // then-param's list-ness is CardinalityComputer's dimension
            // (safeIsClosureParameterMulti) — the inference channel tracks
            // element types only, so computeArgumentElementType is the exact
            // derivation for every closure form. The elided then-pipe argument
            // stays MISSING there (the documented separate gap) — this branch
            // makes no new claim for it.
            return closureParameterType(engine, inline);
        }
        if (node instanceof RShortcut shortcut) {
            // A shortcut (alias) types as its body expression — the exact
            // analogue of the RRule branch above. We read the body's CURRENT
            // inferred type from the engine's memoized map; the engine's
            // fixed-point loop re-runs until that value stabilizes (the
            // "fixed-point iteration" the previous deferral comment named).
            //
            // No explicit cycle guard is needed here — unlike
            // CardinalityComputer.compute, which recurses into
            // shortcut.expression() DIRECTLY and therefore carries an
            // IdentityHashMap visited-set to avoid a stack overflow on a
            // self-/mutually-referential alias. This path instead goes through
            // the memoized getInferredType lookup (a map read, not a recursive
            // call), so a cyclic alias simply stays MISSING until convergence
            // rather than overflowing the stack.
            return shortcut.expression() != null
                    ? engine.getInferredType(shortcut.expression())
                    : RMetaAnnotatedType.MISSING;
        }
        if (node instanceof REnumeration en) {
            // v3.1 CLOSE-OUT (the parser's standing conformance red, list (a)):
            // upstream's RosettaTypeProvider.safeRType on a RosettaEnumeration
            // symbol (vendored :173-175) types a bare reference to an ENUMERATION
            // as the enum type itself — the value seat is then caught by the
            // enum-face check ("must be followed by ` -> <enum value>`") AND its
            // enum type fails the output assignment (c1-precedence:53 `extract
            // Colour`, the global enum outranking the item feature; the
            // assignment error anchors at :52). The fork returned MISSING here,
            // which silenced the assignment half. The fork parses `E -> V` as
            // one REnumValueRef, so a symbol reference resolved to an
            // enumeration is exactly that invalid bare form (or a feature call's
            // receiver, which types the same way upstream); no valid corpus
            // model writes one — the twenty-cell gate measures it. Upstream
            // attaches the symbol's meta attributes to the type; deliberately
            // NOT carried here (no meta ever rides a bare enumeration name).
            return RMetaAnnotatedType.withNoMeta(new com.regnosys.rosetta.types.REnumTypeRef(en));
        }
        return RMetaAnnotatedType.MISSING;
    }

    /**
     * Infers the type of an attribute by reading its typeCall's resolved type.
     * Falls back to BuiltinTypeRegistry for built-in type names.
     *
     * <p>Phase X1 fix: when M3 resolves the {@code typeCall.referencedType}
     * to a builtin-typed AST node (e.g. one of the {@code BuiltinTypeRegistry}
     * type-defining nodes for {@code string} / {@code int} / {@code boolean}
     * / etc.), {@link #astNodeToRType} returns {@link RMissingType#INSTANCE}
     * because it only directly translates {@code RDataType} +
     * {@code REnumeration}. In that case we MUST fall through to the
     * {@link BuiltinTypeRegistry} lookup keyed by {@code typeName} —
     * otherwise attributes typed as {@code string} / {@code int} / etc.
     * appear as MISSING in the inferred-type map, which breaks the
     * Phase X1 RuleGenerator back-fill (and any other consumer that reads
     * inferred types of bare-attribute references resolved by Category 9).
     *
     * <p>Note: this fallback does NOT cover {@code RTypeAlias} — the alias
     * name is not a builtin key, so {@code BuiltinTypeRegistry.lookup}
     * returns empty and the attribute still infers as MISSING. Aliases must
     * resolve via {@code TypeAliasSolver.evaluateAliasBody} (D35) before
     * reaching this point. If you see an RTypeAlias-typed attribute hitting
     * this code path with MISSING, the bug is upstream of here in the
     * alias-resolution stage.
     */
    RMetaAnnotatedType inferTypeOfAttribute(RAttribute attr) {
        return inferTypeOfTypeCall(attr.typeCall());
    }

    /**
     * The {@link RTypeCall} → {@link RMetaAnnotatedType} core the attribute
     * channel has always used, extracted at PR #454 so the library-function
     * PARAMETER channel ({@code TypeInferenceEngine.getInferredParameterType}
     * — the call-site validator seat) resolves declared types identically.
     */
    RMetaAnnotatedType inferTypeOfTypeCall(RTypeCall tc) {
        if (tc == null) return RMetaAnnotatedType.MISSING;

        // First: check M3's resolved type (for user-defined types)
        var resolved = tc.referencedType();
        if (resolved.isPresent()) {
            RType rt = astNodeToRType(resolved.get());
            if (rt != RMissingType.INSTANCE) {
                return RMetaAnnotatedType.withNoMeta(rt);
            }
            // Phase X1 closure T0k (Gap F): alias-typed attribute / func output.
            // {@link #astNodeToRType} only translates RDataType + REnumeration,
            // so an RTypeAlias falls through to MISSING here — which is why a
            // reference to a func with alias-typed output (e.g.
            // {@code NotationStringFromEnum -> NumericChar1to4 = string(...)})
            // previously inferred MISSING. Resolve the alias body to its
            // underlying parametric RType via TypeAliasSolver (D35), mirroring
            // the generator-side wiring in GeneratorModel.resolveTypeCall.
            if (resolved.get() instanceof RTypeAlias alias) {
                RType aliasType = aliasSolver.evaluateAliasBody(alias, tc);
                if (aliasType != RMissingType.INSTANCE) {
                    return RMetaAnnotatedType.withNoMeta(aliasType);
                }
                // evaluateAliasBody returns MISSING for the cases it cannot
                // parametrically reconstruct — chiefly constrained string
                // aliases (e.g. {@code string(minLength,maxLength,pattern)};
                // RStringType reconstruction was deferred at P2.1.3b). For type
                // IDENTITY — all the rule-output back-fill + downstream codegen
                // need — the alias is transparent: its Java type is its terminal
                // base builtin regardless of the constraint args. Fall back to
                // that unconstrained base so an alias-typed reference no longer
                // infers MISSING. (Constraint-sensitive int↔BigDecimal cases are
                // already resolved non-MISSING above and never reach here.)
                RType base = aliasBaseBuiltin(alias);
                if (base != RMissingType.INSTANCE) {
                    return RMetaAnnotatedType.withNoMeta(base);
                }
            }
            // Fall through to builtin lookup (resolved node is a builtin
            // type AST node that astNodeToRType doesn't directly translate).
        }

        // Fallback: built-in type by name (covers both unresolved and
        // M3-resolved-to-builtin cases).
        String typeName = tc.typeName();
        if (typeName != null) {
            var builtin = builtins.lookup(typeName);
            if (builtin.isPresent()) {
                return RMetaAnnotatedType.withNoMeta(builtin.get());
            }
        }
        return RMetaAnnotatedType.MISSING;
    }

    /**
     * Converts an M2 AST node (resolved by M3) to an M4 RType.
     */
    private RType astNodeToRType(com.regnosys.rosetta.ast.RNode node) {
        if (node instanceof RDataType dt) return new RDataTypeRef(dt);
        if (node instanceof REnumeration en) return new REnumTypeRef(en);
        // Phase X1 closure T0p (Gap I): a `choice` type. The inference path
        // previously returned MISSING for choices, which starved both the
        // Gap-B type-restriction (`payout -> OptionPayout` where `payout :
        // Payout` and `Payout` is a choice — the rule-gating #1 leaf) and any
        // feature chain whose intermediate attribute is choice-typed. Build a
        // FULLY-POPULATED RChoiceTypeRef so SubtypeRelation Rule 8
        // (`S <= choice{A,B}`) — which iterates options() — can match.
        if (node instanceof com.regnosys.rosetta.ast.types.RChoice ch) {
            return choiceToRType(ch, new java.util.HashSet<>());
        }
        // v3.2 seat 9 (D47, F8): a resolved basic / record type DECLARATION node — ONE law with the
        // generator twin GeneratorModel.astNodeToRType (BuiltinTypeRegistry.basicOrRecordNodeType,
        // LAW 69). The builtins model's declaration answers its registered twin — the SAME type the
        // callers' by-name fallback derived before the seat (inferTypeOfTypeCall / inferRuleFromType
        // fell through MISSING to builtins.lookup(typeName)), so those sites are unmoved; a
        // MODEL-declared basicType / recordType answers `nothing` (Void at the Java seat, the released
        // plugin's mapping) where the seat found MISSING → Object (the chaos s15 family). One arm
        // moves with the twin verdict: choiceToRType's option loop admits a RESOLVED builtin option it
        // used to drop (it kept only the unresolved-by-name case) — no vendored or chaos carrier. The D47
        // reach census (round 3, cq MF-2; scratch/d47-census-r1.txt, LOCAL) counts 4,743 sources
        // (4,137 vendored + 606 chaos) and 56 basicType / recordType declarations over 20 files - 48 over
        // the 19 chaos-s15 files and 8 in the builtins model, the population the collision law of D47's
        // round-1 addendum (d) covers (round 4, cq SF-3); whether a choice option is typed by a builtin is
        // a PARSE-tree question the text census does not answer (its own note) — the seat suite and the
        // 26-cell ring witness it, both
        // byte-inert on this arm.
        var declared = builtins.basicOrRecordNodeType(node);
        if (declared.isPresent()) return declared.get();
        // Other resolved nodes are handled by their own arms (the alias solver, the choice walk).
        return RMissingType.INSTANCE;
    }

    /**
     * Builds a fully-populated {@link RChoiceTypeRef} from an {@link
     * com.regnosys.rosetta.ast.types.RChoice}: each option's resolved type
     * becomes an element of {@code options()} (required by SubtypeRelation
     * Rule 8). Recurses for nested choices; {@code visited} guards against
     * cyclic choice declarations (a self/mutually-referential choice yields an
     * empty-options ref on the re-entry, terminating the recursion).
     */
    private RType choiceToRType(com.regnosys.rosetta.ast.types.RChoice ch,
                                java.util.Set<com.regnosys.rosetta.ast.types.RChoice> visited) {
        if (!visited.add(ch)) {
            return new RChoiceTypeRef(ch.name(), java.util.List.of(), ch);
        }
        java.util.List<RType> options = new java.util.ArrayList<>();
        for (var option : ch.options()) {
            RTypeCall tc = option.typeCall();
            if (tc == null) continue;
            var resolved = tc.referencedType();
            if (resolved.isPresent()) {
                RType ot = resolved.get() instanceof com.regnosys.rosetta.ast.types.RChoice inner
                    ? choiceToRType(inner, visited)
                    : astNodeToRType(resolved.get());
                if (ot != RMissingType.INSTANCE) options.add(ot);
            } else if (tc.typeName() != null) {
                builtins.lookup(tc.typeName()).ifPresent(options::add);
            }
        }
        return new RChoiceTypeRef(ch.name(), options, ch);
    }

    /**
     * Walks an {@link RDataType}'s super-type chain to the first choice
     * super-type, if any. Rune allows {@code type Foo extends ChoiceBar} (a data
     * type extending a choice — {@link RDataType#choiceSuperType()}) as well as
     * the common {@code type Foo extends DataBar} chain
     * ({@link RDataType#superType()}). A data type has at most one super-type, so
     * this follows the data-type links upward until it reaches a choice
     * super-type or the chain ends. Cycle-guarded against malformed models.
     */
    private static Optional<com.regnosys.rosetta.ast.types.RChoice> choiceSuperTypeInChain(RDataType dt) {
        java.util.Set<RDataType> visited = new java.util.HashSet<>();
        RDataType cur = dt;
        while (cur != null && visited.add(cur)) {
            var choiceSuper = cur.choiceSuperType();
            if (choiceSuper.isPresent()) {
                return choiceSuper;
            }
            cur = cur.superType().orElse(null);
        }
        return Optional.empty();
    }

    /**
     * Phase X1 closure T0k (Gap F) helper — walks an alias chain to its
     * terminal base and returns the UNCONSTRAINED base builtin. Used only as a
     * type-identity fallback when {@link TypeAliasSolver#evaluateAliasBody}
     * cannot parametrically reconstruct the alias (constrained string aliases).
     * Depth-bounded to guard against cyclic alias declarations.
     */
    private RType aliasBaseBuiltin(RTypeAlias alias) {
        RTypeCall body = alias.typeCall();
        // Depth bound intentionally agrees with TypeAliasSolver.MAX_DEPTH (100)
        // so this fallback walks no shallower than the primary alias solver.
        for (int depth = 0; body != null && depth < 100; depth++) {
            var ref = body.referencedType();
            if (ref.isPresent() && ref.get() instanceof RTypeAlias inner) {
                body = inner.typeCall();
                continue;
            }
            String baseName = body.typeName();
            return baseName == null
                    ? RMissingType.INSTANCE
                    : builtins.lookup(baseName).orElse(RMissingType.INSTANCE);
        }
        return RMissingType.INSTANCE;
    }

    // === Accessors for sub-computers ========================================

    public BuiltinTypeRegistry builtins() { return builtins; }
    public SubtypeRelation subtypeRelation() { return subtypeRelation; }
    public TypeJoin typeJoin() { return typeJoin; }
    public TypeAliasSolver aliasSolver() { return aliasSolver; }
}
