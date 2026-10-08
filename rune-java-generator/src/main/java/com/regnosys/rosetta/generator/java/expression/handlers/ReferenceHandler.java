package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.enums.OperationOp;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSuperCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.regulatory.RBody;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.function.FunctionDependencyCollector;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaLocalVariableDeclarationStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatementList;
import com.regnosys.rosetta.generator.java.statement.builder.JavaBlockBuilder;
import com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RRecordType;
import com.regnosys.rosetta.types.RType;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;

import javax.lang.model.SourceVersion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Logger;

/**
 * Handles code generation for reference expression types:
 * {@link RSymbolReference}, {@link REnumValueRef}, {@link RImplicitVariable},
 * and {@link RSuperCall}.
 *
 * <p>Golden output patterns (verified against CDM golden files):
 * <pre>
 *   variable          →  MapperS.of(variableName)
 *   function(a, b)    →  MapperS.of(functionName.evaluate(unwrappedArg1, unwrappedArg2))
 *   EnumType.VALUE    →  EnumTypeName.valueName
 *   item              →  item
 *   super             →  super.doEvaluate()   (placeholder — no CDM golden usage found)
 * </pre>
 *
 * <p><b>Function call argument unwrapping:</b> The {@code evaluate()} method
 * on generated function classes takes raw Java types (e.g. {@code List<?>},
 * {@code BigDecimal}, {@code int}), NOT Mapper-wrapped values. See
 * {@link #unwrapForEvaluateArg(JavaStatementBuilder)} for the full dispatch
 * ladder (v6.2 C3a.2 — six branches: null builder → structural
 * {@code unwrapToBuilder} → {@code "null"} source → the enum-constant witness →
 * legacy {@code MapperS.of} string-scan → fall-through {@code .get()}).
 */
public class ReferenceHandler {

    private static final Logger LOG = Logger.getLogger(ReferenceHandler.class.getName());

    // =========================================================================
    // Symbol references — variables and function calls
    // =========================================================================

    /**
     * Compiles a symbol reference.
     *
     * <p>Dispatch paths (checked in order):
     * <ol>
     *   <li><b>Alias (RShortcut) reference</b> — when the resolved symbol is an
     *       {@link RShortcut}, emit {@code aliasName(input1, input2, ...)} using
     *       the enclosing function's input names in declaration order. Alias
     *       methods are declared as
     *       {@code protected abstract MapperS<? extends T> aliasName(InputType input)}
     *       on the generated function class, so the invocation already returns a
     *       Mapper — no {@code MapperS.of(...)} wrapping. Degrades gracefully to
     *       an empty argument list when the enclosing function cannot be
     *       resolved.</li>
     *   <li><b>Variable reference</b> (no args, no RShortcut symbol) — emits
     *       {@code MapperS.of(name)}.</li>
     *   <li><b>Function call</b> (args present) — emits
     *       {@code MapperS.of(receiver.evaluate(unwrappedArg1, unwrappedArg2))},
     *       where {@code receiver} is the lowerCamel instance name of the
     *       resolved {@link RFunction} (derived via
     *       {@link FunctionDependencyCollector#lowerCamelCase} from
     *       {@code callee.name()} — matching the injected {@code @Inject} field,
     *       not the UpperCamel type name; facet F3). Function call arguments are
     *       unwrapped because {@code evaluate()} methods on generated function
     *       classes take raw Java types, not Mapper-wrapped values.</li>
     * </ol>
     *
     * @param expr     the symbol reference node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of arguments
     * @return a {@link JavaExpression} rendering the reference
     */
    public JavaStatementBuilder handle(RSymbolReference expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        String name = expr.name();

        // v3.1 flip seat 21 — facet metaFaceShortForm: a BARE metadata FACE name over the implicit
        // item (`commodityPayouts min [ key ]` — drr CommodityCommodityLeg1/2) arrives with the
        // SYMBOL bound to the exported `metaType key` root element (the linker's global pass) —
        // upstream's `RosettaSymbolReference → RosettaMetaType` arm takes the same metaCall as an
        // explicit `item -> key` (ExpressionGenerator.xtend:1113-1116). Synthesize that explicit
        // shape (receiver = the synthetic implicit item, resolvedFeature EMPTY, the metaType carried)
        // and let the ONE short-form seat render it; the pre-seat fall-through was the variable echo
        // `MapperS.of(key)` — non-compiling, no golden carries it. Only where an implicit item
        // exists (a binding lambda or a rule body); a real attribute/alias/param named like a face
        // is never bound to the metaType and takes its usual branch below.
        com.regnosys.rosetta.ast.types.RMetaType bareFace = HandlerHelper.boundMetaType(expr);
        if (expr.args().isEmpty()
                && bareFace != null
                && expr.symbol().filter(RAttribute.class::isInstance).isEmpty()
                && (nearestEnclosingInlineFunction(expr) != null
                        || HandlerHelper.findEnclosingRule(expr) != null)) {
            RImplicitVariable metaItem = new RImplicitVariable();
            metaItem.setSynthetic(true);
            metaItem.setParent(expr.parent());
            RFeatureCall metaFc = new RFeatureCall();
            metaFc.setReceiver(metaItem);
            metaFc.setFeatureName(name);
            metaFc.setParent(expr.parent());
            metaFc.setResolvedFeatureNode(bareFace);
            return compiler.visitFeatureCall(metaFc, ctx);
        }

        // Alias (RShortcut) reference → method invocation with the enclosing
        // function's inputs. The alias method signature is
        // `MapperS<? extends T> aliasName(Input1Type input1, ...)`, so it already
        // returns a Mapper — no MapperS.of(...) wrapping.
        //
        // Aliases are unary-named references that never carry args in the
        // Rune grammar. A non-empty args list alongside RShortcut resolution
        // indicates a malformed AST — fail loud so the bug surfaces at
        // codegen time, not downstream when invalid Java fails to compile.
        if (isAliasReference(expr)) {
            if (!expr.args().isEmpty()) {
                throw new IllegalStateException(
                        "Alias reference '" + name + "' cannot carry arguments; "
                        + "aliases are name-only references to RShortcut. "
                        + "Got " + expr.args().size() + " argument(s).");
            }
            // facet aliasSelfShadowItemFeature (PR #372, F-delta-5): a bare name inside
            // an alias's OWN lambda that the linker bound to the alias ITSELF is the
            // upstream features-first mis-bind (`alias intentToAllocate:
            // partyTradeInformation extract intentToAllocate = True` — an alias cannot
            // recurse in rune, so a self-reference is ALWAYS the shadowed ITEM feature;
            // upstream resolves implicit-item features FIRST, ExpressionGenerator
            // xtend:1078-1090). When the implicit item's type carries the same-named
            // attribute, synthesize the item nav (golden cdm6 MapIntent
            // `item.<Boolean>map("getIntentToAllocate", _partyTradeInformation ->
            // _partyTradeInformation.getIntentToAllocate())`); the mis-bound self-call
            // rendered infinite recursion — non-compiling, zero green carriers.
            // Since PR #453 the linker filters the alias's own name from its body's
            // scope (the upstream :401-402 mirror), so the self-BIND state this arm
            // admits is linker-unwritable: the corpus carriers now arrive
            // attribute-bound and route through the guarded isAliasReference
            // fallback instead. The arm stays as the belt-and-braces render for a
            // stale self-bound AST (the fallback's documented partial-resolution
            // scenario) — it and the fallback guard encode the SAME law: a
            // self-name is the shadowed item feature, never a self-call.
            RShortcut selfAlias = expr.symbol().filter(RShortcut.class::isInstance)
                    .map(RShortcut.class::cast).orElse(null);
            if (selfAlias != null
                    && HandlerHelper.findEnclosingShortcut(expr) == selfAlias) {
                RInlineFunction nearestSelf = nearestEnclosingInlineFunction(expr);
                if (nearestSelf != null && bindsImplicitItem(nearestSelf)) {
                    RDataType selfItemType =
                            NavigationHandler.implicitItemDataTypeOrInferred(expr, compiler);
                    RAttribute selfAttr = selfItemType == null ? null
                            : HandlerHelper.findAttributeOnDataType(selfItemType, name);
                    if (selfAttr != null) {
                        RImplicitVariable selfItem = new RImplicitVariable();
                        selfItem.setSynthetic(true);
                        selfItem.setParent(expr.parent());
                        RFeatureCall selfFc = new RFeatureCall();
                        selfFc.setReceiver(selfItem);
                        selfFc.setFeatureName(selfAttr.name());
                        selfFc.setResolvedFeature(selfAttr);
                        selfFc.setParent(expr.parent());
                        return compiler.compile(selfFc, ctx.expectedType(), ctx.scope());
                    }
                }
            }
            // Alias invocation renders as a plain helper-method call on the
            // generated function class: {@code aliasName(inputs)}. The
            // alias helper's declared return type is determined by alias
            // analysis and may be MapperS, MapperC, or a non-mapper type
            // (e.g. List / builder / Object for usesOutput aliases).
            // This handler must NOT assume a specific mapper import here —
            // imports implied by the alias method declaration are
            // contributed where that declaration is emitted (via
            // AliasModel.inferredRefs and FunctionAliasHelper's wrapper
            // class seed on AliasModel.refs). Only refs required by the
            // rendered expression itself are attached at this point.
            // (Copilot round 18 finding — removed the MAPPER_S
            // over-approximation that was incorrect for MapperC / non-mapper
            // aliases.)
            // facet aliasOutputBuilderNav (PR #381, W): a call to an OUTPUT-ROOTED
            // disguised-chain alias threads the output builder as the FIRST arg and
            // wraps the Builder-returning call back into the Mapper world —
            // `MapperS.of(payout(product.toBuilder(), security, masterConfirmation)
            // .build())` (golden cdm5 NewEquitySwapProduct ×6 call sites). Same-walk
            // gated (FunctionAliasHelper.isOutputBuilderNavAlias) so the call, the
            // signature and the body wrap cannot disagree; every other alias call
            // keeps the bare form below.
            RShortcut wAliasTarget = expr.symbol().filter(RShortcut.class::isInstance)
                    .map(RShortcut.class::cast).orElse(null);
            RFunction wEnclosingFunc = HandlerHelper.findEnclosingFunction(expr);
            if (wAliasTarget == null && wEnclosingFunc != null && name != null) {
                // The same name-based resolution isAliasReference used to admit this
                // call (the resolver leaves alias references symbol-empty on this
                // path), so the discriminator reads the SAME shortcut body.
                for (RShortcut sc : wEnclosingFunc.shortcuts()) {
                    if (name.equals(sc.name())) {
                        wAliasTarget = sc;
                        break;
                    }
                }
            }
            String wOutputName = wEnclosingFunc == null ? null
                    : wEnclosingFunc.output().map(RAttribute::name).orElse(null);
            if (wAliasTarget != null && wOutputName != null
                    && com.regnosys.rosetta.generator.java.function.FunctionAliasHelper
                            .isOutputBuilderNavAlias(wAliasTarget.expression(), wOutputName)) {
                String wInputs = renderEnclosingInputs(expr, name);
                return JavaExpression.from(
                        "MapperS.of(" + disambiguateAliasInvocation(expr, name)
                                + "(" + wOutputName + ".toBuilder()"
                                + (wInputs.isEmpty() ? "" : ", " + wInputs)
                                + ").build())",
                        null,
                        Set.of(HandlerHelper.MAPPER_S));
            }
            // THE § 6.3 VALUE-TYPED SEAM BRIDGE (T1): when the compiler's seam
            // policy says this alias is emitted VALUE-typed in the class being
            // rendered (reference route: always null — the byte-inert seam), the
            // invocation wraps in the STRUCTURAL MapperS.of / MapperC.<X>of
            // bridge instead of the bare Mapper-returning call. The structural
            // wrap's unwrapToBuilder channel restores the bare value at the
            // evaluate-arg / assignment / addAll strip seats (the § 5 CALL_ARG /
            // BODY_ROOT value forms, round-trip deleted); every other consumer
            // composes Mapper machinery over the wrap text exactly as it
            // composed over the Mapper-returning call (the disclosed bridge
            // form; runtime operand class MapperS/MapperC preserved per the
            // wrap-class clause). The multi witness rides the seam policy (the
            // chain's element class — its import is already file-present via
            // the seam declaration's own element reference).
            if (wAliasTarget != null) {
                ExpressionCompiler.AliasValueSeam valueSeam =
                        compiler.aliasValueSeamOrNull(wEnclosingFunc, wAliasTarget);
                if (valueSeam != null) {
                    JavaExpression valueCall = JavaExpression.from(
                            disambiguateAliasInvocation(expr, name)
                                    + "(" + renderEnclosingInputs(expr, name) + ")",
                            null,
                            Set.of());
                    // A sentinel-spelt element (the seam policy's witnessRender, PR #607)
                    // takes the FQN-witness channel so the bridge and the seam
                    // declaration resolve the colliding simple name together.
                    return valueSeam.isMulti()
                            ? JavaExpression.wrappedInMapperCOfSingle(valueCall, null,
                                    valueSeam.multiWitness(), valueSeam.witnessRender())
                            : JavaExpression.wrappedInMapperSOf(valueCall);
                }
            }
            // facet member_name_disambiguation: a shortcut/alias colliding with a
            // function dependency is numbered name1 at its invocation (decl numbered
            // in compileAliases); renderEnclosingInputs keeps the bare name (log only).
            return JavaExpression.from(
                    disambiguateAliasInvocation(expr, name)
                            + "(" + renderEnclosingInputs(expr, name) + ")",
                    null,
                    Set.of());
        }

        // Bare enum value (Phase X1 T0n / Gap E): TypeInferenceEngine Category 13
        // resolves a bare symbol to an REnumValue when an expected enum type makes
        // it unambiguous (e.g. `then PHYS` in a conditional whose sibling branch is
        // `DeliveryTypeEnum -> CASH`, or `settlementType = Cash`). Emit the enum
        // constant `Enum.VALUE` — mirrors the qualified REnumValueRef path
        // (handle(REnumValueRef) enumeration-present branch): UNWRAPPED (no
        // MapperS.of), relying on the ambient import contributed by the output /
        // attribute type, with the enclosing context (comparison operand / branch
        // assignment) applying any MapperS.of wrap exactly as it does for the
        // qualified form. The value's Java constant comes from
        // EnumHelper.convertValue (source `Cash` -> Java `CASH`); the enum's simple
        // name from the qualifying REnumeration — see the owner rule below.
        //
        // The enum import is collected on the refs channel (the SAME enumImportRefs
        // the qualified handle(REnumValueRef) path uses, PR #130). The original
        // PR #130-era assumption — that this Category 13/14 resolution fires ONLY in
        // comparison-operand / conditional-branch contexts where a sibling enum-typed
        // attribute / output ALWAYS contributes the ambient import, so collecting refs
        // here would be unreachable — no longer holds: PR #202 (facet ctorSetterEnum,
        // Cat 13d) reaches this branch at a CONSTRUCTOR-SETTER conditional-then seat
        // with NO such ambient import, so the bare `Enum.VALUE` constant would not
        // compile without it. refs is a Set, so the prior comparison/branch carriers
        // (whose import is already ambient) stay byte-unchanged.
        //
        // facet boundEnumInferredOwner (v3.1 flip seat 12 — THE ROOT FIX of the enum
        // expected-owner family, PR #582): the QUALIFYING enum is the node's
        // INFERRED type — the EXPECTED enum at the seat — not the value's DECLARING
        // enum. Upstream types a bare enum value by where it is USED
        // (ExpressionGenerator.xtend:340-343 `enumCall(feature,
        // expectedType.getItemValueType)`; the generated Java flattens inherited
        // values under the child's name — the #211/#358 flatten law), and the fork's
        // parser already types the node that way (ExpressionTypeComputer: a bare
        // REnumValue-symbol reference types as engine.expectedEnumAt(ref); the
        // declaring enum is only its fallback). Rendering `ev.parent()` here emitted
        // `MapperS.of(PutCallEnum.PUT)` against an OptionTypeEnum operand and
        // `ProductIdTypeEnum.ISIN` against an AssetIdTypeEnum navigation — NON-COMPILING
        // (drr 7.x DTCC_OptionTypeRule / UnderlierIDOtherSourceLeg1Rule / the
        // MAS_BR_0052 data rule), while eight downstream qualifier producers each
        // rebuilt the expected enum by hand for the shapes they knew. The IR route's
        // IRJavaLeafEmitter.emitEnumValue already qualifies by the node's type. The
        // read is the SetOperationHandler `default`-RHS precedent (gm.workspace()
        // .getInferredType) — HandlerHelper.boundEnumInferredOwner, SHARED with the
        // ITE hoist local's declared type (ControlFlowHandler.thenItemJavaClass) so
        // the two halves cannot disagree (LAW 69) — under the #215 SAME-INSTANCE
        // descend-only gate: the
        // expected enum's hierarchy must flatten the EXACT bound value (an unrelated
        // same-name enum is a different instance; a WIDENING seat — the value declared
        // BELOW the expected enum — is never found, so the declaring qualifier stays;
        // by the bind law such a value never binds at all). MISSING / non-enum
        // inference and expected == declaring keep today's bytes. This arm owns a
        // FRESH single-element ref set, so the child import simply REPLACES the
        // parent's — no strip on any wider set (LAW 70). An EXPLICIT `Enum -> Value`
        // (handle(REnumValueRef)) keeps the author's enum — the golden two-sided
        // witness UnderlierIDOtherSourceLeg1Rule.java:69 (bare → child) vs :74
        // (explicit → parent).
        if (expr.symbol().isPresent()
                && expr.symbol().get() instanceof com.regnosys.rosetta.ast.supporting.REnumValue ev
                && ev.parent() instanceof com.regnosys.rosetta.ast.types.REnumeration en) {
            com.regnosys.rosetta.ast.types.REnumeration owner =
                    HandlerHelper.boundEnumInferredOwner(expr, ev, en, compiler.getGeneratorModel());
            String enumConstant = owner.name() + "."
                + com.regnosys.rosetta.generator.java.enums.EnumHelper.convertValue(ev);
            return JavaExpression.enumConstant(enumConstant, null, enumImportRefs(owner, compiler),
                    Set.of());
        }

        // Rule-body implicit-input feature (Phase X1 Gap #2): a bare attribute
        // reference that is a feature of the enclosing RRule's from-type renders
        // as an input navigation, not a bare variable. D39 resolves the *type* of
        // such a reference (TypeInferenceEngine Cat 9); here we resolve its
        // *rendering*: MapperS.of(input).<T>map("getX", x -> x.getX()) by
        // synthesizing an RFeatureCall and delegating to the NavigationHandler
        // (mirroring the disguised-REnumValueRef feature-call path below).
        //
        // Gated to rule bodies via findEnclosingRule: a function body has no
        // enclosing RRule (its parent chain reaches the RFunction), so this
        // branch never fires for genuine function params/locals — protecting
        // existing function/POJO byte output (BC-PRIMARY, D31).
        if (expr.args().isEmpty()
                && expr.symbol().isPresent()
                && expr.symbol().get() instanceof RAttribute attr) {
            RFeatureCall implicitInputNav = synthesizeImplicitInputNavigation(expr, attr);
            if (implicitInputNav != null) {
                return compiler.visitFeatureCall(implicitInputNav, ctx);
            }
            // facet filter_predicate_item_typing (mechanism 2): the inline-lambda
            // implicit-ITEM analogue of the rule-side implicit-input synthesis
            // above. A bare attribute reference inside a filter/extract lambda
            // (`partyRoles filter role = partyRoleEnum`) resolves to the lambda
            // ITEM type's attribute, which the variable path below would render as
            // the non-compiling bare name (`MapperS.of(role)` — `role` is not a
            // Java local); golden navigates the implicit item
            // (`item.<PartyRoleEnum>map("getRole", partyRole -> partyRole.getRole())`).
            RFeatureCall implicitItemNav = synthesizeImplicitItemNavigation(expr, attr, compiler);
            if (implicitItemNav != null) {
                return compiler.visitFeatureCall(implicitItemNav, ctx);
            }
            // Coverage wave D (datarule): the TYPE-condition analogue of the two
            // branches above. A bare attribute reference inside a data-type
            // condition — bound by the linker's type-condition scope (wave D) to
            // the declaring type's attribute — navigates the condition's implicit
            // instance, the executeDataRule parameter (`MapperS.of(cash)
            // .<AssetIdentifier>mapC("getIdentifier", _cash ->
            // _cash.getIdentifier())`, golden CashCurrencyExists). The identity
            // guard (findAttributeOnDataType == the bound attr) is ALSO the
            // recursion guard: this branch's own synthetic instance receiver
            // carries a synthetic RAttribute that can never be the type's found
            // attribute, so its compile re-entry declines to the variable path
            // (`MapperS.of(<instance>)`) even on a type whose attribute is
            // self-named. Context-gated by findEnclosingTypeCondition — null for
            // every function/rule-path expression by construction.
            RFeatureCall conditionInstanceNav = synthesizeConditionInstanceNavigation(expr, attr, compiler);
            if (conditionInstanceNav != null) {
                return compiler.visitFeatureCall(conditionInstanceNav, ctx);
            }
        }

        // Function invocation in elided-argument (predicate) position
        // (engine PR #6): a bare reference to a `func` used as a list-op
        // predicate — `filter Foo`, a `then Foo` body — resolves to an
        // RFunction with EMPTY args(). Upstream injects the enclosing implicit
        // variable as the call argument (ExpressionGenerator.xtend
        // caseSymbolReference → callableWithArgsCall → evaluateCall); the fork's
        // AstBuilder builds a bare RSymbolReference with no args
        // (buildImplicitInlineFunction), so the implicit variable is synthesised
        // here. Without this branch the reference falls through to the variable
        // path below and renders the function *type name* as a literal
        // (MapperS.of(Foo)) — non-compiling.
        //
        // Emits `<camelName>.evaluate(<binding>.get())` UNWRAPPED: the enclosing
        // operation (e.g. filterSingleNullSafe's lambda) supplies the boolean
        // context, and per upstream evaluateCall the invocation itself carries
        // no MapperS.of wrap. The function dependency is already collected and
        // injected by FunctionDependencyCollector#walkExpression for any
        // RFunction symbol reference, so no generator-side change is needed for
        // functions. Bare `then <rule>` in a RULE body (engine PR #6 facets b+c:
        // FunctionDependencyCollector injects the `<Name>Rule` field) AND bare
        // `then <function>` in rule AND function bodies (engine PR #13 facet F6;
        // widened by facet then_statement_hoisting arm S1) are both
        // intercepted upstream by FunctionExpressionRenderer.renderBareInvokableThenSet,
        // which emits the 2-statement thenArg form; this branch covers
        // function-reference invocation in OTHER positions (filter/list-op
        // predicates, and any generic then-lambda fallthrough). Other
        // rule-reference shapes beyond bare then-rule remain deferred.
        // Explicit-argument calls (`Foo(a, b)`) keep the existing branch below
        // — this is gated on args().isEmpty().
        //
        // SCOPE NOTE (engine PR #6 codex_review P1): this branch fires for ANY
        // empty-args RFunction reference, not only elided-predicate position.
        // It is byte-identity-safe by construction: pre-PR-6 an empty-args
        // RFunction reference fell through to the variable path below and
        // rendered the non-compiling `MapperS.of(<Name>)` literal, so NO
        // currently-byte-matching (green) file contains this shape — only
        // already-waivered files are affected. A stricter context/arity guard
        // (e.g. require an enclosing implicit RInlineFunction, or callee arity
        // == 1) is a tracked robustness follow-up: it depends on the resolved
        // RFunction's inputs() being populated in expression position, which is
        // not yet confirmed, so it is deferred rather than risk the verified
        // 113-file flip. See codegen-completeness-audit Tier 3.
        if (expr.args().isEmpty()
                && expr.symbol().isPresent()
                && expr.symbol().get() instanceof RFunction callee) {
            return renderImplicitFunctionInvocation(expr, callee, ctx, compiler);
        }

        // facet pointFreeLibraryFnRef (W42 finding #26, PR #436): a ZERO-ARG reference
        // to the builtin IsLeapYear (an RLibraryFunction — the rune-dsl basicfunctions,
        // which the bare-RFunction branch above cannot admit) in operand position is
        // upstream's POINT-FREE form (`extract IsLeapYear`): the named function applies
        // to the implicit item, so it lowers to the #369 guarded-coercion arm with the
        // argument text = the unwrapped implicit item — golden func-named-func-ref F2:
        // `item -> { final Integer integer = item.get(); return integer == null ?
        // MapperS.<Boolean>ofNull() : MapperS.of(new IsLeapYear().execute(
        // BigDecimal.valueOf(integer))); }`. Min/Max decline (two-arg builtins have no
        // unary point-free form — the variable path keeps today's bytes). Green-safe by
        // construction: the pre-fix render was the non-compiling bare name echo
        // `MapperS.of(IsLeapYear)` (no such variable), so no green file carries it.
        if (expr.args().isEmpty()
                && expr.symbol().isPresent()
                && expr.symbol().get()
                        instanceof com.regnosys.rosetta.ast.types.RLibraryFunction
                && "IsLeapYear".equals(name)) {
            RImplicitVariable libItem = new RImplicitVariable();
            libItem.setSynthetic(true);
            libItem.setParent(expr.parent());
            JavaStatementBuilder libArg = unwrapForEvaluateArg(handle(libItem, ctx, compiler));
            if (libArg instanceof JavaExpression libArgExpr) {
                return isLeapYearGuardedBlock(libArgExpr.renderToString(),
                        libArgExpr.getRefs(), libArgExpr.getStaticWildcardImports(),
                        ctx, compiler);
            }
        }

        // Bare RULE reference in operand position (facet F6 residual) — symmetric
        // to the bare-FUNCTION branch above. A no-arg RSymbolReference resolving to
        // an RRule, used OUTSIDE a then-body (function argument, exists/comparison
        // operand, conditional branch), renders as the injected rule instance
        // invoked on the implicit input. Bare then-rule BODIES are intercepted
        // earlier by FunctionExpressionRenderer.renderBareInvokableThenSet, so they
        // never reach here; this covers every OTHER position, which previously fell
        // through to the variable path below and rendered the non-compiling
        // MapperS.of(<qualifiedName>) literal (e.g. cde.price.StrikePriceNoFormat).
        //
        // GATED on TWO conditions:
        //  (1) a HOST that can supply the receiver argument. At rule-body top level
        //      that is findEnclosingRule != null, mirroring
        //      FunctionDependencyCollector's bare-RRule injection gate
        //      (hostSimpleName != null). IN A LAMBDA the receiver is the lambda
        //      binding itself, so the host may equally be a FUNCTION — v3.1 flip
        //      seat 33 law B.3 (facet bareRuleRefFunctionHost) widened this half
        //      once the collector was measured to inject the @Inject field on BOTH
        //      paths (the fork's own QuantityUnitOfMeasure output carries it); the
        //      pre-B.3 sentence, "this rendering must not fire for a rule reference
        //      inside a plain function body where no such field is declared", was
        //      the PR #332 half that never landed, not a live invariant.
        //  (2) the implicit-input argument must resolve to a REAL binding. Inside an
        //      extract/filter/then lambda that binding is the lambda `item` /
        //      `thenArg` (nearestEnclosingInlineFunction != null). At rule-body TOP
        //      LEVEL there is no lambda, so the argument is the rule's `input`
        //      parameter — available only when the enclosing rule declares a
        //      from-type (fromType().isPresent()), which types the synthetic `input`
        //      reference. A from-type-less rule at top level still DECLINES (falls
        //      through to the variable path below — the pre-fix, still-waivered form)
        //      rather than emit an untyped `input`. renderImplicitRuleInvocation
        //      picks `item.get()` vs `input` from the same in-lambda predicate.
        // Byte-identity-safe by construction (same argument as the bare-FUNCTION
        // branch): pre-fix BOTH shapes produced non-compiling Java (a bare qualified
        // reference name, e.g. cde.price.StrikePriceNoFormat), so no currently-green
        // file contains either — only already-waivered files are affected. The
        // top-level `input` form is the deferred sub-facet PR #100 left pending a
        // byte-oracle; the DRR corpus residual supplied a uniform one — the
        // dump-now-matching oracle flips 240 waivered reports/*Rule.java to
        // `output = RULE.evaluate(input);` (a 2-line mechanical classifier
        // undercounts to 202; the byte-oracle is authoritative).
        if (expr.args().isEmpty()
                && expr.symbol().isPresent()
                && expr.symbol().get() instanceof RRule rule) {
            // Resolve the enclosing rule ONCE (a bounded parent walk) — reused for
            // the null check, the top-level fromType gate, and the receiver-argument
            // build in renderImplicitRuleInvocation.
            RRule enclosingRule = HandlerHelper.findEnclosingRule(expr);
            // facet bareRuleRefFunctionHost (v3.1 flip seat 33, law B.3): a no-arg bare RRule
            // reference sitting INSIDE an extract/filter/then lambda renders the injected rule
            // invocation in a FUNCTION host too — golden drr 7.x QuantityUnitOfMeasure
            // `exists(MapperS.of(unitOfMeasureFromQuantityRule.evaluate((fieldWithMetaNonNegative
            // QuantitySchedule == null ? null : fieldWithMetaNonNegativeQuantitySchedule
            // .getValue()))))` against the fork's `exists(MapperS.of(quantity.
            // UnitOfMeasureFromQuantity))`, a namespace-qualified RUNE name spliced as a Java
            // identifier at three seats (the filter predicate, the extract body, the terminal
            // mapSingleToItem). Gate (1) above mirrored a FunctionDependencyCollector rule-host
            // gate that PR #332 DROPPED: the collector injects the field on BOTH paths and the
            // fork's own output already carries `@Inject protected UnitOfMeasureFromQuantityRule
            // unitOfMeasureFromQuantityRule;` at this FUNCTION host (fork:34, byte-identical to
            // golden:34) — #332 landed two of its three halves; this is the third.
            //
            // Only the IN-LAMBDA arm widens. In a lambda the receiver argument IS the lambda
            // binding (`item.get()` / `thenArg.get()`), which exists whatever the host kind, so
            // the host test becomes "a rule OR a function encloses this" — the two are
            // mutually exclusive on one node (HandlerHelper:811-822), and
            // renderImplicitRuleInvocation dereferences {@code enclosingRule} only on its
            // non-lambda branch, so the null rule is safe on the path this arm reaches. The
            // TOP-LEVEL arm is UNCHANGED, both halves: a from-type-less rule at rule-body top
            // level still declines rather than emit an untyped `input`, and a bare reference at
            // FUNCTION-body top level (no lambda binding, no `input` parameter) declines exactly
            // as before.
            //
            // MEASURED radius ZERO on BOTH routes ([P33-RULEARG], round 2 at this head): of
            // 14,335 admitting rows the declining `encRule=- inLambda=true` set is 24 rows and
            // ONE `where=` (fn:QuantityUnitOfMeasure), identical row for row on the IR route.
            // Green-safe by construction as well as by measurement: the pre-fix render is a
            // dotted rune name, not a Java identifier, so no compiling file can carry it (LAW 74
            // — javac33 C7 lines 472/474/487, `cannot find symbol: variable quantity`, plus
            // 487's `Object cannot be converted to String` cascade at the String output sink).
            // The IR route INHERITS: IRExpressionCompiler.ruleDelegationDeclines returns true for
            // a function host and falls through to this seat (that decline is DO-NOT-TOUCH —
            // it protects the #274 PriorUniqueTransactionIdentifierRule MapperC band regression).
            boolean bareRuleInLambda = nearestEnclosingInlineFunction(expr) != null;
            if (bareRuleInLambda
                    ? (enclosingRule != null
                        || HandlerHelper.findEnclosingFunction(expr) != null)
                    : (enclosingRule != null && enclosingRule.fromType().isPresent())) {
                return renderImplicitRuleInvocation(expr, rule, enclosingRule, ctx, compiler);
            }
        }

        // facet lambda_item_body_coercion (arm B2, bare name): a bare name inside
        // an IMPLICIT inline filter/extract lambda that names an attribute of the
        // lambda's ITEM type, but which the linker either left symbol-EMPTY or
        // bound to a TYPE declaration, synthesizes the item navigation. The
        // type-bound shape is a CHOICE OPTION referenced by its type name
        // (`payouts extract [ PerformancePayout exists ]` on cdm6 choice Payout):
        // the file-scope chain binds the imported RDataType/RChoice node, the
        // RAttribute-gated PR #161 arm above rejects it, and the variable path
        // below rendered the raw type name (`MapperS.of(PerformancePayout)` — not
        // a Java local, non-compiling; same for the symbol-EMPTY bare-attr form).
        // Upstream caseSymbolReference resolves implicit-item features FIRST when
        // an implicit variable is present (ExpressionGenerator.xtend:1078-1090),
        // and choice options ARE features of the choice's asRDataType projection.
        // Declines (legacy bytes) for closure params, function-scope names, and
        // names the item type's projection does not carry. Regression-safe by
        // construction: a firing case previously rendered a non-compiling bare
        // name, which no green file can carry.
        //
        // facet thenArgTypeFromCompiled (PR #204): the linker can MIS-BIND a bare
        // implicit-item feature inside a then-chain filter/extract lambda to a
        // same-named GLOBAL root element instead of the item's attribute, when the
        // chain's item type was erased to Object by the parser snapshot (the same
        // root cause as the thenArg-declaration Object erasure). The OBSERVED
        // mis-binding kinds (exercised by the 8 flips) are an {@code RAnnotation}
        // (e.g. `filter qualification = …` bound the `qualification` annotation) and an
        // {@code RRecordType} (e.g. `extract dateTime` bound the `dateTime` record
        // type); {@code REnumeration} is admitted for COMPLETENESS (the same
        // type-vs-attribute name-collision class, no current carrier). These join the
        // symbol-EMPTY / {@code RDataType}
        // (choice-option type name) / {@code RChoice} cases the arm already handled
        // — all are NON-value bindings the variable path below would render as a
        // non-compiling bare name. synthesizeImplicitItemBareNav's decline ladder
        // (closure param / function-scope name / non-implicit lambda / item type
        // unresolved / no exact-name attribute) gates each; regression-safe by
        // construction (a firing case previously rendered a non-compiling bare name,
        // which no green file can carry).
        // facet rerootItemNav (PR #282): a bare implicit-item feature in a RULE-body map-lambda may
        // be linker-bound to an {@link RAttribute} — EITHER the item's own feature (e.g. a multi
        // `cashSettlementTerms`) OR, in the then-piped (item-type-erased) shape this facet targets,
        // a same-named GLOBAL element the linker MIS-BOUND (the #204 mis-binding) — NOT one of the
        // non-value mis-bindings the existing #204 arm admits, so it fell to the variable path below
        // as the bogus, non-compiling bare `MapperC.<Item>of(<name>)` / `MapperS.of(<name>)` (no
        // local of that name exists). Admit an RAttribute symbol so
        // synthesizeImplicitItemBareNav RE-RESOLVES the feature BY NAME on the (inferred) item type —
        // deliberately IGNORING the possibly-mis-bound `expr.symbol()` (the #204 recovery), so NO
        // `expr.symbol()` identity guard is added (unlike the Cat-9 `synthesizeImplicitItemNavigation`,
        // whose guard compares to a KNOWN-item-feature Cat-9 binding, not the linker symbol — adding
        // such a guard here would decline exactly the mis-bound recovery cases this facet flips).
        // Its decline ladder (closure param / function-scope name / non-implicit lambda / item type
        // unresolved / no exact-name item attribute) gates non-feature names back to the variable
        // path; the green-safety discriminator is the `RAttribute`-only filter — a rule shortcut /
        // alias is an `RShortcut` (excluded), so a green local is never admitted. Green-safe by
        // construction: the bare-name render never compiled, so no green file carries it
        // (empirically: regscan 0 within-waiver regressions).
        // facet filterPredicateOperandNavReRoot (PR #341): the #282 RAttribute admission was
        // RULE-SCOPED at introduction (the #232 shared-seat caution); now un-rule-scoped — a
        // FUNCTION-path filter/extract-lambda operand whose bare name the linker bound to an
        // RAttribute re-resolves BY NAME on the item type exactly like the rule path (drr
        // Extract_UTIPropietary's `identifierType` filter operand). The green-safety
        // discriminator was ALWAYS synthesizeImplicitItemBareNav's decline ladder, which the
        // rule path relied on too: a closure param or a FUNCTION-SCOPE name (input/output/
        // alias — the #232 concern that motivated the original rule gate) declines BEFORE the
        // item-type resolution, so every green function-local reference keeps the variable
        // path byte-identical (cp4 scan: 0 new divergents, 0 green movement, all 3 cells).
        // facet builtinTypeBoundItemNav (PR #346): the RSegmentDef binding joins the #204
        // NON-value mis-binding class — a bare implicit-item feature whose name collides
        // with a same-named ROOT element binds THAT, not the attribute (drr GetOthrPmt
        // esma: OtherPayment declares `date date (0..1)`, and the P346-F5 probe showed the
        // bare `date` linker-bound to the drr `segment date` RSegmentDef → the variable
        // path rendered the non-compiling bare `.setPmtDt(date)` where golden re-roots
        // `item.<Date>map("getDate", …).get()`). RBasicType is admitted for COMPLETENESS
        // (the same builtin-type-name collision class, no current carrier — the #204
        // REnumeration precedent). The decline ladder is the green-safety discriminator as
        // ever: a function-scope `date` (input/output/alias — CompareDateTo's own `date`
        // INPUT declines at gate 2, probe-verified) or a closure param declines BEFORE the
        // item-type resolution, and an item type without the attribute falls back to the
        // variable path byte-identically (the 6 green bare-`date` cdm goldens: 4
        // input-param, 2 explicit-nav — all gate-saved).
        boolean attrBoundSymbol = expr.symbol().filter(RAttribute.class::isInstance).isPresent();
        if (expr.args().isEmpty()
                && (expr.symbol().isEmpty()
                        || expr.symbol().get() instanceof RDataType
                        || expr.symbol().get() instanceof RChoice
                        || expr.symbol().get() instanceof com.regnosys.rosetta.ast.types.RRecordType
                        || expr.symbol().get() instanceof com.regnosys.rosetta.ast.types.REnumeration
                        || expr.symbol().get() instanceof com.regnosys.rosetta.ast.types.RBasicType
                        || expr.symbol().get() instanceof com.regnosys.rosetta.ast.regulatory.RSegmentDef
                        || expr.symbol().get() instanceof com.regnosys.rosetta.ast.annotations.RAnnotation
                        || attrBoundSymbol)) {
            // Coverage wave D (datarule): at type-condition TOP LEVEL (no
            // enclosing lambda) the declaring type's attribute wins over the
            // implicit-item recovery below — there is no item outside a lambda,
            // and a pass-4 GLOBAL binding shadowing a same-named attribute
            // (e.g. the `location` metadata ANNOTATION vs LegacyValuationTime's
            // `location` attribute, golden LegacyValuationTimeDayAndTime) must
            // not divert the instance navigation into the item form.
            if (nearestEnclosingInlineFunction(expr) == null) {
                RFeatureCall conditionTopNav = synthesizeConditionInstanceBareNav(expr, compiler);
                if (conditionTopNav != null) {
                    return compiler.visitFeatureCall(conditionTopNav, ctx);
                }
            }
            RFeatureCall bareItemNav = synthesizeImplicitItemBareNav(expr, compiler);
            if (bareItemNav != null) {
                return compiler.visitFeatureCall(bareItemNav, ctx);
            }
            // facet caseNarrowedDisguisedNav (PR #368, F-B): the BARE 1-name sibling —
            // a bare symbol inside a TYPE-guard switch CASE that is an attribute of
            // the NARROWED case type re-roots off the #221-bound cast var (golden
            // GetFpmlPayerReceiver `MapperS.of(floatingLeg).<PayerReceiverModel>map(
            // "getPayerReceiverModel", …)`; MapAsset's per-case `exchangeId`
            // evaluate-args). Same ACTIVE-binding gate + decline ladder as the
            // 2-name chain arm; green-safe by the same bare-undefined-name argument.
            if (ctx.scope() != null) {
                RFeatureCall caseBareNav = synthesizeCaseNarrowedBareNav(expr, ctx, compiler);
                if (caseBareNav != null) {
                    return compiler.visitFeatureCall(caseBareNav, ctx);
                }
            }
            // Coverage wave D (datarule): a bare name inside a DATA-TYPE condition
            // that is an attribute of the condition's declaring type (supertype
            // chain included) navigates the condition's implicit instance — the
            // executeDataRule parameter (`MapperS.of(cash).<AssetIdentifier>mapC(
            // "getIdentifier", _cash -> _cash.getIdentifier())`, golden
            // CashCurrencyExists). The wave-D linker walk
            // (LexicalResolutionPass.buildTypeConditionScope) binds MOST such refs
            // to the declaring type's attribute (those route through the
            // RAttribute-BOUND identity arm above); this BY-NAME arm covers the
            // refs the BEST-EFFORT walk left symbol-EMPTY (the walk is silent on
            // anything it cannot resolve — choice-super options, shadowed names),
            // resolving by name on the declaring type exactly like the item arm
            // above; the item arm keeps priority inside implicit lambdas. Decline
            // ladder: a closure-param name declines first (a lambda param read
            // stays a bare Java variable); a non-feature name falls to the
            // variable path. Context-gated by
            // HandlerHelper.findEnclosingTypeCondition — null for every
            // function/rule-path expression by construction (a func condition's
            // RCondition parents at the RFunction), so it cannot fire for any
            // byte-proven kind's compile.
            RFeatureCall conditionInstanceNav = synthesizeConditionInstanceBareNav(expr, compiler);
            if (conditionInstanceNav != null) {
                return compiler.visitFeatureCall(conditionInstanceNav, ctx);
            }
        }

        if (expr.args().isEmpty()) {
            // Variable reference → MapperS.of(name). Use the wrap factory so
            // MAPPER_S enters refs atomically with the wrap (and drops
            // atomically if the builder is later unwrapped for evaluate-arg
            // use). PR-A v6.2 C3a.2.
            //
            // facet multi_cardinality_value_wrap: a MULTI-cardinality ((0..*)/(1..*))
            // symbol's Java value is a List<? extends Item>, which upstream 9.83.0
            // coerces to the witnessed MapperC.<Item>of(name) (item-to-MapperC
            // cardinality coercion), not the scalar MapperS.of(name). Regression-safe
            // by construction: a green file never carries a SURVIVING
            // MapperS.of(<multi var>) render (upstream would have emitted MapperC
            // there), and transient wraps that downstream sites strip structurally
            // strip the MapperC wrap identically (wrappedInMapperCOfSingle mirrors
            // the unwrapToBuilder contract).
            // facet thenParamCollisionEscape (PR #375, B1b): a bare read of an
            // EXPLICIT then-step extract param renders the ESCAPED `_<name>` when
            // the param name collides with the piped element's own type simple
            // name (the enclosing restructured chain's hoisted decl
            // `final MapperS<Product> thenArg = …` references the class, so
            // upstream's identifier-vs-class collision escape renames the param —
            // golden DTCC_OptionTypeRule `_Product.get()` evaluate args). The
            // consult is render-truth-gated on the wrapper fn's LIVE #350 binding
            // (see thenStepParamRenderName); every unbound / non-colliding /
            // implicit shape keeps the raw name byte-frozen.
            // ONE owner walk feeds both the render-name consult and the reduce
            // elision below (Seat-1 #420 OBS-6 — no second walk).
            RInlineFunction bareCpOwner = enclosingClosureParamOwner(expr, name);
            String cpRenderName = thenStepParamRenderName(
                    bareCpOwner, name, ctx.scope(),
                    compiler.getTypeUtil());
            // facet reduceClosureParam (PR #420): a REDUCE-owned closure param IS the
            // MapperS the runtime binds (`MapperC.reduce((a, b) -> …)` — both params
            // are MapperS<T>-typed, the mechanism-4 law's own signature census), so a
            // bare read renders the param name RAW: golden expr-functional-ops
            // `MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(a, b)` where the
            // wrap path re-wrapped `MapperS.of(a)` — a MapperS<MapperS<T>> that
            // compiles in NO consuming position (green-safe by construction), and
            // upstream's inlineFunction binds reduce params as Mapper items referenced
            // raw. The MAPPER_S ref never enters, so the stale import drops with the
            // wrap (golden carries no MapperS import). Corpus-neutral by ABSENCE: the
            // `reduce` operator occurs in ZERO corpus .rosetta sources (33 grep lines
            // across 26 files, ALL prose — Seat-1-enumerated) and `.reduce(` in ZERO
            // of the 34,686 goldens. A name that shadows a real attribute keeps the
            // same law — the generated Java identifier resolves to the lambda param
            // (Java shadowing = upstream's inner binding). Non-reduce owners keep the
            // wrap path byte-verbatim.
            if (bareCpOwner != null
                    && bareCpOwner.parent() instanceof RReduceExpr) {
                // v3.2 seat 12 (D52, H1 - M12): a read of a DUPLICATED reduce parameter binds to the FIRST and
                // renders its numbered name (`a0`), the half of HandlerHelper.reduceParamRenderNames that
                // compileReduceLambda wrote the pair by; a distinct name renders raw as before. The predicate
                // compares the RAW declared names, so it reads the raw `name` (round 1, cq NIT-1: the post-escape
                // cpRenderName is the same string for a reduce owner - the escape keys on a named extract parent -
                // but the two halves of the law must read ONE channel). The RReduceExpr test above selects this ARM;
                // the predicate carries the same gate itself since round 2 (cq SF-1), so its third consumer - the
                // receiver seat - meets it too.
                return JavaExpression.from(
                        HandlerHelper.reduceParamReadName(bareCpOwner, name), null, Set.of());
            }
            // facet listOfListLowering (PR #430): a closure param of a LIST-consuming
            // lambda over a MapperListOfLists IS the inner MapperC the runtime binds
            // (mapListToItem/mapListToList take Function<MapperC<T>, …>;
            // filterListNullSafe takes Function<MapperC<T>, Boolean>), so a bare read
            // renders the param name RAW — the #420 reduce-param law at the
            // list-of-lists seats: golden `.mapListToItem(fooListItem ->
            // MapperS.of(fooListItem.resultCount()))` (upstream ListOperationTest
            // ExtractListOfListThenExtractToListOfCounts' own expected text) where the
            // wrap path re-wrapped `MapperS.of(fooListItem)` — a MapperS<MapperC<T>>
            // that compiles in NO consuming position (green-safe by construction).
            // The binding-kind read consults the SAME method-selection SOTs the render
            // halves use (CollectionHandler.lambdaParamBindsListItem — the #274
            // two-halves-agree pattern).
            if (bareCpOwner != null
                    && CollectionHandler.lambdaParamBindsListItem(bareCpOwner, compiler,
                            ctx.scope())) {
                // facet lolLambdaParamTypeStamp (W42 finding #19, PR #432): stamp the
                // raw read with the MapperC<E> the runtime binds (the SAME binding
                // walk the admission consulted) — the untyped #430 form left the
                // in-lambda typed-sum recovery blind to the param's element type
                // (`l sum` fell to the non-existent generic `.sum()`). A null stamp
                // (no provable binding) keeps the untyped read byte-verbatim.
                return JavaExpression.from(cpRenderName,
                        CollectionHandler.lambdaParamBoundListType(bareCpOwner, compiler,
                                ctx.scope()),
                        Set.of());
            }
            // facet singleLambdaParamRawRead (PR #431): a closure param of a
            // SINGLE-consuming extract lambda IS the inner MapperS the runtime binds
            // (mapSingleToItem/mapSingleToList take Function<MapperS<T>, …>), so a
            // bare read renders the param name RAW — the #420/#430 law at the SINGLE
            // seats (upstream FunctionGeneratorTest nestedInlineFunctionsTest's
            // cross-lambda `item + param1 + param2` operands, where the wrap path's
            // MapperS.of(param1) is a MapperS<MapperS<Integer>> that compiles in no
            // consuming position — green-safe by construction). The binding-kind read
            // consults the SAME method-selection SOT the render half uses
            // (CollectionHandler.lambdaParamBindsSingleItem — the #274 pattern).
            if (bareCpOwner != null
                    && CollectionHandler.lambdaParamBindsSingleItem(bareCpOwner, compiler,
                            ctx.scope())) {
                return JavaExpression.from(cpRenderName, null, Set.of());
            }
            RAttribute varAttr = expr.symbol()
                    .filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast)
                    .orElse(null);
            // facet fnInputDepCollisionEscape (W42 finding #27, PR #436): a bare read
            // of a function INPUT renders the SAME "_"-escaped name the signature
            // carries when the raw name collides with a dependency field / the class
            // name / a Java keyword — FunctionGenerator.escapedFunctionInputName is
            // the single source of truth for both halves (golden
            // func-call-single-to-list C: `Collections.singletonList(_a)` against
            // `@Inject protected A a`). Gated to a NON-closure-param read whose
            // RAttribute symbol IS an input of the enclosing function (identity —
            // a dispatch-variant body binding the base's attrs declines to the raw
            // name, today's bytes). The class-name arm passes the function's own
            // name (== the emitted simple name on the standard path; rule hosts
            // never carry real inputs).
            if (bareCpOwner == null && varAttr != null) {
                RFunction inputOwner = HandlerHelper.findEnclosingFunction(expr);
                if (inputOwner != null) {
                    for (RAttribute ownerInput : inputOwner.inputs()) {
                        if (ownerInput == varAttr) {
                            cpRenderName = FunctionGenerator.escapedFunctionInputName(
                                    inputOwner, name, inputOwner.name());
                            break;
                        }
                    }
                }
            }
            // v3.1 C0 item 1 — THE LOUD REGISTER. A reference the resolver never bound,
            // and which is not a closure parameter (those bind at render time, by name),
            // is about to be echoed into Java as a bare identifier: the name the author
            // wrote, unqualified and unbound. That is the E1/E2 surface — golden
            // `MapperS.of(PartyRoleEnum.MARGIN_AFFILIATE)` against our
            // `MapperS.of(MarginAffiliate)` — and it does not compile. Counting is
            // deliberately ALL the C0 stage does here: the matrix decides whether any
            // green cell depends on the echo before it becomes a refusal (§ 4's
            // count-then-refuse ordering).
            //
            // The test is the LINKER'S OWN VERDICT, not "the node carries no symbol". Those
            // are different populations, and the difference was measured: the loose test
            // fired 117 times on cells that are byte-perfect (cdm 5.38.0's `startDate`,
            // `resetDates`, drr 6.34.1's `confirmationDateTime` — ordinary names that render
            // correctly), which would have made this counter useless as a refusal gate.
            // isReportedUnresolved keys on the LinkingDiagnostic the resolver already
            // emitted for the exact source range, so it fires only where resolution FAILED.
            //
            // C0 CONVERTED THIS TO A REFUSAL. The 25x2 matrix measured the site at 88
            // hits across the band and ZERO on every cell the fork considers clean, so no
            // green output depends on the echo: emitting it can only produce a file that
            // does not compile, in place of a file we could have refused by name. The
            // element now refuses (JavaClassGenerator catches per element, attaches the
            // target path) and D11's existing generation-error gate reports it. C1 retires
            // the site by binding the name.
            if (bareCpOwner == null && expr.symbol().isEmpty()
                    && compiler.getGeneratorModel() != null
                    && compiler.getGeneratorModel().isReportedUnresolved(expr, name)) {
                throw SilentDegradation.refuse(SilentDegradation.Site.UNRESOLVED_SYMBOL_ECHO,
                        "unresolved reference '" + cpRenderName + "'", expr);
            }
            JavaExpression innerName = JavaExpression.from(cpRenderName, null, Set.of());
            JavaExpression multiWrap = tryMultiValueWrap(varAttr, innerName, compiler, expr);
            if (multiWrap != null) {
                return multiWrap;
            }
            // facet metaInputParam (PR #342): a [metadata …]-annotated single-card
            // FUNCTION-INPUT param is signature-typed the concrete meta wrapper
            // (FunctionGenerator.resolveParam — the #186 output law extended to inputs),
            // so its bare reference keeps the TEXT-identical MapperS.of(name) wrap but
            // reports the WRAPPER item type (the #176 two-arg typed-wrap overload; the
            // structural unwrap contract is unchanged — a ctor value strips to the bare
            // wrapper name and the #327-B2 render-truth proof selects the PLAIN setter,
            // golden `.setCurrency(currency)`). The nav-receiver and evaluate-arg seats
            // apply upstream's meta-param deref forms via their own #342 arms; every
            // other consumer of a bare meta-input ref sits inside the 5 corpus-closed
            // carriers (no green file has a meta-annotated input).
            RJavaWithMetaValue metaParamWrapper =
                    NavigationHandler.metaInputParamWrapper(expr, compiler);
            if (metaParamWrapper != null) {
                JavaTypeUtil tu = compiler.getTypeUtil();
                return JavaExpression.wrappedInMapperSOf(innerName,
                        tu.wrap(tu.MAPPER_S, metaParamWrapper));
            }
            return JavaExpression.wrappedInMapperSOf(innerName);
        }

        // Function call — compile each argument, unwrap via the builder-level
        // helper, and emit name.evaluate(...). Refs AND staticWildcardImports
        // from each unwrapped arg accumulate into the inner expression so they
        // survive the outer MapperS.of wrap. Dropping staticWildcardImports
        // here (Copilot round 7 finding) silently loses imports like
        // EXPRESSION_OPERATORS_NULL_SAFE on any argument that emits a
        // comparison/existence call. PR-A v6.2 C3a.2.
        // ref_coercion facet (PR ref-coercion arg-deref): when an argument
        // resolves to a {@code ReferenceWithMeta<T>} wrapper but the called
        // function's parameter expects the plain value type {@code T}, the
        // upstream golden HOISTS the unwrapped reference into a
        // {@code final <MetaType> <var> = <nav>.get();} local and passes a
        // null-guarded deref {@code (<var> == null ? null : <var>.getValue())},
        // lifting the enclosing extract lambda to BLOCK form. The fork's coercion
        // service lacks upstream's {@code convertNullSafe} (declareAsVariable +
        // null-guard), so the local + guard are reproduced here — mirroring the
        // PR #14 coercion variant in
        // {@link com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer}
        // — while the deref conversion itself stays service-sourced. ROUTED by
        // {@link #metaDerefHoistRoute}: BLOCK when the call IS an {@code extract}/{@code map}
        // lambda body (the JavaBlockBuilder reaches {@code CollectionHandler.compileLambda})
        // or a DIRECT then-body/rule-body call in a reporting RULE (PR #129/#143 statement
        // lifts); LAMBDA_CHANNEL when the call is a NESTED operand inside an extract/map
        // lambda (facet evaluate_arg_consumption — decls register on the lambda-body scope
        // and drain into the block body); every other position keeps the flat
        // (still-waivered) form — zero regression.
        MetaDerefHoistRoute hoistRoute = metaDerefHoistRoute(expr);
        RFunction calleeFn = expr.symbol()
                .filter(RFunction.class::isInstance).map(RFunction.class::cast).orElse(null);
        // facet blockArmWrapperHopDeref (v3.1 flip seat 33, law F.A rung (b)): an
        // EXPLICIT-ARGS reporting-RULE invocation has an RRule symbol, so calleeFn is null
        // and every tryMetaDerefArg arm below is gated shut - golden derefs such an argument
        // (drr 5.61.0 cftc NotionalCurrencyLeg1Rule / jfsa NotionalCurrencyOfLeg1Rule:
        // `cDECommodityNotionalCurrencyRule.evaluate((referenceWithMetaPriceSchedule1 == null
        // ? null : referenceWithMetaPriceSchedule1.getValue()))` <- the fork's
        // `cDECommodityNotionalCurrencyRule.evaluate(<chain>.<ReferenceWithMetaPriceSchedule>
        // map("getPrice", ...).get())`, a wrapper handed to a PriceSchedule-typed evaluate()
        // that does not compile - javac32 C20 lines 712/714).
        // LAW 69: the IMPLICIT no-args sibling renderImplicitRuleInvocation already builds
        // this exact callee (`RFunction.fromRule(rule)` + the setTypeCall restoration,
        // :3896-3897) and feeds it to tryMetaDerefArg; the restoration is required because
        // RTypeCall.deepCopy DELIBERATELY drops the referencedTypeId resolution state, so
        // getType() on the ad-hoc synthetic input would return MISSING and tryMetaDerefArg's
        // param-type gate would decline. It is the SAME single `from`-typed input the
        // RuleGenerator emits the rule's evaluate(...) signature from.
        // A SEPARATE local, never an assignment to calleeFn: tryBareEnumArg (:1153),
        // evaluateParamIsMulti (:1178) and the call-output attribute reads (:1641, :1688)
        // must keep seeing null across all 21,772 RRule call sites - byte-inert by
        // construction.
        // THE MEASURED NARROWING (both routes, at this head): symKind=RRule = 21,772;
        // route=LAMBDA_CHANNEL = 401 (args=0 occurs 0 times corpus-wide, so the args gate is
        // ADJUDICATED-EMPTY structural defence); isWrapper=true = 8, and those 8 ARE the two
        // carriers' four calls each, ZERO green. The LAMBDA_CHANNEL belt is EMPTY at that
        // measured grain but is NOT inert: severing it exposes 21,371 further rows
        // (STATEMENT_SINK 20,899 + BLOCK 472) to tryMetaDerefArg's five null-compiled-type
        // RECOVERY channels (:2438, :2452, :2467, :2477, :2512), which the probe's naive
        // compiled-type read does not reproduce - 393 of the 401 LAMBDA_CHANNEL rows read
        // compiledItem=-. Ship the belt; declare the lane empty at the isWrapper grain.
        RRule metaDerefRule = expr.symbol()
                .filter(RRule.class::isInstance).map(RRule.class::cast).orElse(null);
        RFunction metaDerefCallee = calleeFn;
        if (calleeFn == null && metaDerefRule != null
                && hoistRoute == MetaDerefHoistRoute.LAMBDA_CHANNEL
                && !expr.args().isEmpty()
                && metaDerefRule.fromType().isPresent()) {
            RFunction ruleCallee = RFunction.fromRule(metaDerefRule);
            ruleCallee.inputs().get(0).setTypeCall(metaDerefRule.fromType().orElseThrow());
            metaDerefCallee = ruleCallee;
        }
        StringJoiner argsJoiner = new StringJoiner(", ");
        Set<JavaClass<?>> combinedRefs = new HashSet<>();
        Set<JavaClass<?>> combinedStaticWildcards = new HashSet<>();
        List<JavaStatement> hoists = new ArrayList<>();
        // facet itemGetMetaDerefBlock (PR #346): true once the FUNCTION-path implicit-item
        // recovery below produced a hoist — the consumption site then registers the decl(s)
        // on the LAMBDA channel (compileLambda drains, per-pass self-contained under the
        // #257 double-render) instead of forming the JavaBlockBuilder whose second-pass
        // wrapper-local landed in a finalized scope at the operand seats (the #340 cp4
        // degradation signature).
        boolean lambdaChannelHoists = false;
        int argIndex = 0;
        for (RExpression arg : expr.args()) {
            // facet evaluateArgExpectedTypeReset (PR #360): a call boundary RESETS the
            // argument expectation — upstream compiles each argument against the CALLEE
            // parameter's type, never the CALLER's context expectation. Threading
            // ctx.expectedType() through here let the entry-point coercion service deref
            // a meta-wrapper argument INLINE (`.get().getValue()` + the evaluate-arg
            // unwrap's chained `.get()` append — non-compiling) BEFORE tryMetaDerefArg
            // ever saw the wrapper (its type-equality probe then found the types already
            // equal — the P359D exit F). The sibling CONDITION seat of the same file,
            // whose context expectation is null, always hoisted (drr iosco
            // ExchangeRateBasisRule fieldWithMetaString0/1 vs the inlined arm seat).
            // Args compile NEUTRAL; the callee-param-driven channels below (argAsMulti /
            // the meta-free arg coercion / tryMetaDerefArg) are the ONE arg-expectation
            // law — the caller's own coercion still applies to the CALL's result at the
            // outer compile entry, unchanged.
            JavaStatementBuilder compiled = compiler.compile(arg, null, ctx.scope());
            // bare_fn_invocation_arg (PR #132): an argument that is itself a bare
            // FUNCTION reference invoked implicitly (a no-explicit-arg
            // RSymbolReference resolving to an RFunction — the bare-function
            // dispatch branch of handle(RSymbolReference) that routes to
            // renderImplicitFunctionInvocation) compiles to a
            // BARE scalar `innerFn.evaluate(item.get())` with NO MapperS.of wrap. It
            // is ALREADY the callee's scalar output, so it must be passed RAW;
            // unwrapForEvaluateArg's fall-through would append a non-compiling
            // `.get()` (a bare scalar value has no `.get()`). The bare-RULE sibling
            // never hits this — renderImplicitRuleInvocation wraps the invocation in
            // MapperS.of, which unwrapForEvaluateArg strips STRUCTURALLY (no spurious
            // `.get()`) — which is why a rule-arg sibling on the same line stays
            // clean while the function-arg over-unwraps. Gated EXACTLY to that
            // emission shape (args().isEmpty() + RFunction symbol), so it only ever
            // rewrites the still-waivered non-compiling form; a green file never
            // carries `evaluate(...).get()` on a scalar, so this is regression-safe by
            // construction.
            boolean argIsBareFnInvocation = arg instanceof RSymbolReference fnRef
                    && fnRef.args().isEmpty()
                    && fnRef.symbol().filter(RFunction.class::isInstance).isPresent();
            // getmulti_multi_arg (PR #131): a multi-cardinality argument (a navigation
            // ending in a multi-valued feature, e.g. a `(0..*)` mapC tail, compiled to
            // a MapperC) passed into a multi-cardinality parameter must unwrap via
            // `.getMulti()` (→ List<T>), not the scalar `.get()` (→ single T) — the
            // latter is a non-compiling single-where-List mismatch. The gate is exact
            // (callee param multi AND the argument expression is multi-cardinality), so
            // it only ever rewrites the still-waivered non-compiling form; a green file
            // never carries it.
            boolean argAsMulti = evaluateArgIsMulti(calleeFn, argIndex, arg, compiler);
            // facet tailMulti (PR #191): the evaluate-arg unwrap ACCESSOR follows the
            // CALLEE PARAMETER's cardinality, not the argument's. argAsMulti (above)
            // stays as the tighter param-multi-AND-arg-multi probe that gates the
            // meta-free arg coercion (below) and the closure-param `.get()` append
            // (PR #171/#180) — those keep their exact behaviour. The accessor,
            // however, widens to `.getMulti()` for ANY Mapper-chain argument into a
            // multi (`List`) parameter, so a SINGLE navigation into a `0..*` param
            // coerces single->List the way upstream's ExpressionGenerator does
            // (MapperS.getMulti() yields a 0-or-1 element list) — the deferred
            // cardinality dimension of the PR #171 evaluate_arg_consumption law.
            // Because argAsMulti implies evaluateParamIsMulti, this is a pure
            // relaxation: the only NEW positions are single Mapper chains into multi
            // params, which the fork rendered as the non-compiling scalar `.get()`
            // (a single `T` where `List<T>` is expected — waivered, never green) and
            // which only ever reach unwrapForEvaluateArg's chained-Mapper fall-through
            // (where `.getMulti()` is always valid). Green-safe by construction.
            boolean paramAcceptsMulti = evaluateParamIsMulti(calleeFn, argIndex, compiler);
            // facet evaluate_arg_consumption (arm B2): a MULTI arg whose compiled
            // MapperC chain ends META-item-typed, passed to a parameter whose
            // declared attribute is meta-FREE, coerces meta-free at the arg site
            // BEFORE the .getMulti() unwrap — upstream compiles the arg against
            // the parameter's expected type, and the wrapper-kind law renders the
            // MapperC conversion as the BARE Type-coercion map (facet
            // meta_coercion_numbering). The lever is the EXISTING
            // coerceNavigationReceiver gate — a strict no-op for null-typed and
            // non-meta chains — and the meta-FREE param guard keeps a
            // meta-declared parameter receiving its wrapper unchanged. Pre-fix
            // the firing shape passed List<ReferenceWithMetaX> where
            // List<? extends X> is expected (non-compiling), so no green file
            // carries it.
            if (argAsMulti && MetaFieldGenerator.detectMetaKind(
                    calleeFn.inputs().get(argIndex)) == MetaFieldGenerator.MetaKind.NONE) {
                // facet coercionWitnessFollowsCalleeParam (PR #342): the coercion witness
                // follows the CALLEE parameter's element type when it resolves — upstream
                // compiles the arg against the parameter's expected item type, so a
                // wrapper whose value is a model SUBTYPE of the param element witnesses
                // the param element (golden Create_StockSplit `<QuantitySchedule>` over a
                // FieldWithMetaNonNegativeQuantitySchedule chain, both cdm cells). An
                // unresolvable param element (or one equal to the wrapper value) keeps
                // the receiver-strip target — the pre-facet bytes.
                JavaClass<?> calleeElem = calleeParamElementClass(calleeFn, argIndex, compiler);
                compiled = compiler.coerceNavigationReceiver(compiled, ctx.scope(), calleeElem);
            }
            // facet onlyElementMapperSRoundTrip (PR #345, W1): an arg that is a
            // THEN-CHAIN whose tail is an only-element collapse over a PROVEN-MapperC
            // pipe keeps golden's identity round-trip `MapperS.of(<mc>.get()).get()`.
            // The compiled value arrives PRE-WRAPPED `MapperS.of(<…>.get())` (the
            // then-machinery's single-value wrap — which unwrapForEvaluateArg's legacy
            // unwrapMapperSOf string-scan would STRIP back to the bare collapse, the
            // GetEventDate shape); re-present it as a real MapperS and take the
            // param-side deref. The THEN-TAIL gate is load-bearing: a DIRECT nav-chain
            // collapse arg stays BARE in golden (`evaluate(item.<Trade>map(…)
            // .<TradeLot>mapC(…).get(), …)` — QuantityIncreased and 9 green siblings,
            // the #345 cp1 over-fire catch), and the 859 green MapperS-pipe
            // `evaluate(thenArg.get())` forms decline via the monotone multi-proof.
            boolean argCollapsedMulti = !argIsBareFnInvocation
                    && HandlerHelper.collapsedMultiOnlyElementThenTail(arg, compiler);
            JavaStatementBuilder unwrapped;
            if (argIsBareFnInvocation) {
                unwrapped = compiled;
            } else if (argCollapsedMulti && compiled instanceof JavaExpression collapsedExpr) {
                String collapsedTxt = HandlerHelper.render(collapsedExpr);
                // unwrapMapperSOf is the WHOLE-wrap test (paren-balanced) — a chain-ROOT
                // `MapperS.of(x).mapC(…).get()` also startsWith "MapperS.of(" but is NOT
                // pre-wrapped (the #345 cp1 `.get().get()` catch).
                String wrappedTxt = HandlerHelper.unwrapMapperSOf(collapsedTxt) != null
                        ? collapsedTxt
                        : "MapperS.of(" + collapsedTxt + ")";
                Set<JavaClass<?>> collapsedRefs = new HashSet<>(collapsedExpr.getRefs());
                collapsedRefs.add(HandlerHelper.MAPPER_S);
                // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): a SIBLING arm of
                // the evaluate-arg unwrap that appends the `.getMulti()` terminal itself, so it
                // reports the same fact (see JavaExpression.evaluateArgMultiExtracted).
                String collapsedOut =
                        wrappedTxt + (paramAcceptsMulti ? ".getMulti()" : ".get()");
                unwrapped = paramAcceptsMulti
                        ? JavaExpression.evaluateArgMultiExtracted(collapsedOut,
                                collapsedExpr.getExpressionType(), collapsedRefs,
                                collapsedExpr.getStaticWildcardImports())
                        : JavaExpression.from(collapsedOut, collapsedExpr.getExpressionType(),
                                collapsedRefs, collapsedExpr.getStaticWildcardImports());
            } else if (paramAcceptsMulti && compiled instanceof JavaExpression bridgedExpr
                    && bridgedExpr.unwrapToBuilder().isPresent()
                    && multiWrapperArgCoercionApplies(arg, calleeFn, argIndex, compiler)) {
                // T2 (PR-23): a structurally-wrapped VALUE-seam bridge whose
                // Mapper ELEMENT is a meta WRAPPER while the callee's meta-free
                // multi param expects the VALUE element must NOT strip — the
                // #347-F5 "Type coercion" arm below rewrites the Mapper form
                // (`MapperC.<Wrapper>of(alias(…)).<Value>map("Type coercion",
                // …).getMulti()`), whereas the strip would pass List<Wrapper>
                // where List<? extends Value> is expected (non-compiling — the
                // T2 multi META-topped member class: FxMarkToMarket.quantities /
                // Qualify_StockSplit.beforeQuantities, both cdm cells). The
                // element-AGREEING bridge class keeps the T1-landed strip.
                Set<JavaClass<?>> keptRefs = new HashSet<>(bridgedExpr.getRefs());
                // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the T2 lift is
                // the second-most-frequent producer of the suffix this seat's gate reads — the c7b
                // census logged it at 80 optimised-walk fires — so it reports the same arm fact.
                unwrapped = JavaExpression.evaluateArgMultiExtracted(
                        HandlerHelper.render(bridgedExpr) + ".getMulti()",
                        bridgedExpr.getExpressionType(), keptRefs,
                        bridgedExpr.getStaticWildcardImports());
            } else if (paramAcceptsMulti && compiled instanceof JavaExpression singleBridge
                    && singleBridge.unwrapToBuilder().isPresent()
                    && isSingleSeamBridgeArg(arg, compiler)) {
                // T3 (PR-24): a SINGLE-seamed VALUE bridge at a MULTI param keeps
                // the bridge + the `.getMulti()` single→list lift — the reference
                // road's item→list coercion at this seat (`MapperS.of(v).getMulti()`
                // = [v], null → the EMPTY list, never a singleton-of-null); the
                // strip would hand the BARE single value where List<? extends T>
                // is expected (non-compiling — the drr IsActionTypeMODI
                // afterTradeStateOpen differential-gate catch). The ADD-seat
                // single-lift guard's exact class at the evaluate-arg seat, on the
                // SAME alias-seam channel (the § 3 S/C authority; wrap-present ∧
                // walk-resolves is the optimised bridge exclusively — reference
                // bytes inert BY CODE). A MULTI-seamed bridge keeps the strip:
                // its bare value IS the List this param wants.
                Set<JavaClass<?>> liftRefs = new HashSet<>(singleBridge.getRefs());
                // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the T3 lift
                // appends the same terminal, so it reports the same arm fact. The c7b census
                // recorded ZERO fires from this producer at the seat below on any of the three
                // walks, so stamping it is byte-inert HERE and keeps the witness faithful to the
                // text off-corpus (a producer that appends the suffix must report it, or the seat
                // would silently under-fire where the retired text read fired).
                unwrapped = JavaExpression.evaluateArgMultiExtracted(
                        HandlerHelper.render(singleBridge) + ".getMulti()",
                        singleBridge.getExpressionType(), liftRefs,
                        singleBridge.getStaticWildcardImports());
            } else {
                unwrapped = unwrapForEvaluateArg(compiled, paramAcceptsMulti);
            }
            // facet mapitem_ctor_wrap (mechanism 4): a DECLARED closure param
            // (`extract unitOfAmount [ … ]`) is a Mapper-typed Java variable
            // at runtime — the map*/filter*/sort/reduce/max/min lambda
            // signatures all bind MapperS<T>-typed params — so upstream's
            // evaluate-arg coercion (every arg compiles against the callee
            // input's ITEM-level expected type) collapses it wrapperToItem
            // with `.get()` (golden `compareQuantityByUnitOfAmount.evaluate(…,
            // unitOfAmount.get())`, CompareTradeLot). The fork's variable path
            // renders every bare name wrappedInMapperSOf, and the structural
            // unwrap above strips that wrap back to the bare name — correct
            // for raw function inputs (plain Java values), wrong for a
            // Mapper-typed closure param. Append the terminal accessor exactly
            // when the unwrap returned the BARE param name (the
            // render-equals-name guard keeps every chained/converted arg
            // untouched), gated by the EXISTING closure-param walk (the #168
            // arm-B2 lever, owner variant). THEN-declared params DECLINE: an
            // explicit then-lambda param binds the WHOLE piped list — the one
            // closure-param shape whose runtime value can be a MapperC, where
            // upstream's collapse is `.getMulti()`, not `.get()` (the
            // argAsMulti exclusion cannot catch it: evaluateArgIsMulti's three
            // probes all need nav/resolvable shapes a bare closure-param
            // symbol never matches). The implicit `item` param already
            // collapses through the existing channels. Green-safe: a raw input
            // name satisfies the walk only when an enclosing lambda declares
            // the same name — in which case the generated Java identifier
            // resolves to the Mapper-typed lambda param (Java shadowing =
            // upstream's inner binding) and the `.get()` is still the correct
            // collapse — and the bare Mapper-typed param as an item-level arg
            // was non-compiling pre-fix, so no green file carries it.
            RInlineFunction cpOwner = !argIsBareFnInvocation && !argAsMulti
                    && arg instanceof RSymbolReference cpRefProbe
                    && cpRefProbe.args().isEmpty()
                    ? enclosingClosureParamOwner(cpRefProbe, cpRefProbe.name())
                    : null;
            // facet thenParamCollisionEscape (PR #375, B1b): the render-equals-name
            // guard compares against the ESCAPE-AWARE rendered name (a #375-renamed
            // then-step param arrives `_Product`, not `Product` — the raw-name compare
            // silently dropped the `.get()` collapse at exactly the escaped seats).
            // Every non-escaped param renders raw, so the compare is unchanged there.
            // v3.2 seat 12 round 2 (cq NIT-4 - the mechanism behind round 1's refuted point 2): a REDUCE
            // parameter's read never reaches this collapse at all - the reduce arm's read carries no unwrap
            // marker, so unwrapForEvaluateArg's fall-through appends the `.get()` before this compare runs,
            // and the compare is false for every reduce read (numbered `a0` != `a`; distinct `a.get()` != `a`):
            // `merge.evaluate(a0.get(), a0.get())` renders byte-identical to the plugin with no arm of this
            // guard - the #191 arm is dead for reduce owners (measured: closure-param-duplicate-reads).
            if (cpOwner != null && !(cpOwner.parent() instanceof RThenExpr)
                    && arg instanceof RSymbolReference cpRef
                    && unwrapped instanceof JavaExpression cpExpr
                    && cpExpr.renderToString().equals(thenStepParamRenderName(
                            cpOwner, cpRef.name(), ctx.scope(), compiler.getTypeUtil()))) {
                // Coverage wave D (datarule): the collapse accessor follows the CALLEE
                // parameter exactly like the #191 tailMulti law — a Mapper-typed
                // closure param into a MULTI (`List`) parameter collapses
                // `.getMulti()` (golden drr ESMAEMIRTransactionReportEMIR_VR_2121_01
                // `isAcceptedEicCode.evaluate(dp.getMulti())` — the input is
                // `eicCode string (0..*)`); a single param keeps `.get()`
                // byte-identically. A scalar `.get()` into a List param never
                // compiled — green-safe by construction.
                // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the #191
                // closure-param collapse — the producer the triage row NAMED — appends the terminal
                // itself, so it reports the same arm fact. (The census found its products never
                // reaching the seat below with the suffix present, so this too is byte-inert at
                // this corpus and faithful off it.)
                String cpOut = cpExpr.renderToString()
                        + (paramAcceptsMulti ? ".getMulti()" : ".get()");
                unwrapped = paramAcceptsMulti
                        ? JavaExpression.evaluateArgMultiExtracted(cpOut,
                                cpExpr.getExpressionType(), cpExpr.getRefs(),
                                cpExpr.getStaticWildcardImports())
                        : JavaExpression.from(cpOut, cpExpr.getExpressionType(), cpExpr.getRefs(),
                                cpExpr.getStaticWildcardImports());
            }
            // facet metaInputParam (PR #342): a BARE [metadata …]-annotated single-card
            // FUNCTION-INPUT param passed to a VALUE-typed callee parameter derefs inline
            // with upstream's null-guarded meta-param value form — golden
            // `priceUnitEquals.evaluate(item.get(), (price == null ? null : price.getValue()))`.
            // The structural unwrap above stripped the #342 typed MapperS.of wrap back to
            // the bare wrapper name (the pre-facet NON_COMPILING form: a wrapper passed
            // where the value type is expected). Render-equals-name-guarded exactly like
            // the closure-param arm above (a chained/converted/`.get()`-appended arg never
            // matches); declines for a meta-annotated callee parameter (a meta-expecting
            // param takes the wrapper RAW — no corpus carrier today) and a multi callee
            // parameter (unproven). Only the 5 corpus-closed meta-input carriers reach it.
            if (arg instanceof RSymbolReference metaArgRef
                    && unwrapped instanceof JavaExpression metaArgExpr
                    && metaArgExpr.renderToString().equals(metaArgRef.name())
                    && !paramAcceptsMulti
                    && calleeFn != null
                    && argIndex < calleeFn.inputs().size()
                    && MetaFieldGenerator.detectMetaKind(calleeFn.inputs().get(argIndex))
                            == MetaFieldGenerator.MetaKind.NONE
                    && NavigationHandler.metaInputParamWrapper(metaArgRef, compiler) != null) {
                unwrapped = JavaExpression.from(
                        "(" + metaArgRef.name() + " == null ? null : "
                                + metaArgRef.name() + ".getValue())",
                        null, metaArgExpr.getRefs(),
                        metaArgExpr.getStaticWildcardImports());
            }
            // facet multiArgElementwiseWrapperDeref (PR #349, S2): a MULTI callee param
            // whose element is the VALUE type of the arg's item WRAPPER derefs
            // ELEMENTWISE inside the Mapper chain — upstream's MapperC item conversion
            // (TypeCoercionService getMapperCItemConversionExpression), whose lambda body
            // is BARE (no null guard — the MapperC law; contrast the null-guarded MapperS
            // form): golden FxMarkToMarket `quantities(trade).<QuantitySchedule>map("Type
            // coercion", fieldWithMetaNonNegativeQuantitySchedule ->
            // fieldWithMetaNonNegativeQuantitySchedule.getValue()).getMulti()`. The item
            // type recovers from the compiled type, else the alias walk (the #347-F5
            // render-truth lockstep channel). Declines: a meta-annotated param (expects
            // the wrappers — the #347 inverse law), a non-wrapper item, an element-type
            // mismatch, or a non-`.getMulti()` render. Green-safe by construction: the
            // pre-fix form passes List<Wrapper> where the callee expects List<Value> —
            // non-compiling, so no green file carries it.
            // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): "the arg took the
            // multi collapse" is the UNWRAP PIPELINE'S OWN ARM REPORT
            // (JavaExpression.EvaluateArgMultiExtracted), not a suffix scan. The row named ONE
            // producer; the c7b census registered all SIX arms feeding `unwrapped` by identity over
            // 177,550 (default) / 99,419 (IR) / 144,175 (optimised) arrivals per walk and found
            // ZERO `.getMulti()`-tailed arrival with an unknown producer, and — decisively — that
            // the row's named `paramAcceptsMulti` INTENT is not the fact: within
            // `evalArgUnwrap AND multi=true` — 16,275 / 11,228 / 10,694 arrivals (default-route,
            // IR-route, optimised) — the suffix is present at only 10,127 / 8,712 / 5,533 of them
            // and ABSENT at 6,148 / 2,516 / 5,161, because unwrapForEvaluateArg appends it only on
            // its terminal-accessor arms.
            // Every arm that appends the suffix now stamps the witness; the SPLICE below stays text
            // surgery (only the GATE retires — the row's own wording).
            if (paramAcceptsMulti && calleeFn != null && argIndex < calleeFn.inputs().size()
                    && unwrapped instanceof JavaExpression.EvaluateArgMultiExtracted multiArgExpr
                    && MetaFieldGenerator.detectMetaKind(calleeFn.inputs().get(argIndex))
                            == MetaFieldGenerator.MetaKind.NONE
                    && compiler.getGeneratorModel() != null
                    && compiler.getTypeTranslator() != null
                    && compiler.getTypeUtil() != null) {
                JavaType multiArgType = compiled.getExpressionType();
                if (multiArgType == null) {
                    multiArgType = NavigationHandler.tryAliasReceiverMapperType(arg, compiler);
                }
                JavaType multiArgItem = multiArgType == null
                        ? null
                        : compiler.getTypeUtil().getItemType(multiArgType);
                if (multiArgItem instanceof RJavaWithMetaValue elemWrapper
                        && elemWrapper.getValueType() instanceof JavaClass<?> elemValueClass) {
                    RType multiParamRType = compiler.getGeneratorModel()
                            .getType(calleeFn.inputs().get(argIndex));
                    JavaClass<?> paramElem = multiParamRType == null
                            ? null
                            : compiler.getTypeTranslator().toJavaReferenceType(multiParamRType);
                    // The wrapper VALUE must be assignable to the param ELEMENT (equal or
                    // a MODEL subtype — FilterQuantityByCurrency takes QuantitySchedule,
                    // the wrapper value is NonNegativeQuantitySchedule). Generated model
                    // classes are FLAT (the #323 law: runtime isSubtypeOf cannot see
                    // model-level ancestry), so the check walks the Rosetta supertype
                    // chain by name. The witness and its import are the PARAM element,
                    // golden's `<QuantitySchedule>map`.
                    if (paramElem != null
                            && valueModelExtendsParamElem(elemValueClass, paramElem, compiler)) {
                        String multiRender = multiArgExpr.renderToString();
                        String multiHead = multiRender.substring(0,
                                multiRender.length() - ".getMulti()".length());
                        String elemLam = JavaNamingUtil.toFirstLower(elemWrapper.getSimpleName());
                        Set<JavaClass<?>> elemRefs = new HashSet<>(multiArgExpr.getRefs());
                        elemRefs.add(paramElem);
                        unwrapped = JavaExpression.from(
                                multiHead + ".<" + paramElem.getSimpleName()
                                        + ">map(\"Type coercion\", " + elemLam + " -> " + elemLam
                                        + ".getValue()).getMulti()",
                                multiArgExpr.getExpressionType(), elemRefs,
                                multiArgExpr.getStaticWildcardImports());
                    }
                }
            }
            if (!(unwrapped instanceof JavaExpression unwrappedExpr)) {
                throw new IllegalStateException(
                        "Function argument unwrapped to non-expression type: "
                        + unwrapped.getClass().getSimpleName());
            }
            JavaExpression argExpr = unwrappedExpr;
            // enum_const_arg (PR #137): a BARE enum value passed as a function-call
            // argument (e.g. RoundToPrecision(item, 5, Nearest)) parses as an
            // UNRESOLVED bare RSymbolReference — the TypeInferenceEngine binds a bare
            // enum value only in comparison-operand / conditional-branch context, not
            // in argument position — so the variable path renders the bare Rune value
            // name `Nearest`, a non-compiling undefined symbol. The QUALIFIED form
            // (RoundingDirectionEnum -> Nearest, an REnumValueRef) already renders
            // RoundingDirectionEnum.NEAREST via handle(REnumValueRef). Resolve the bare
            // value against the callee's DECLARED enum parameter type and emit the same
            // qualified constant + import. Takes precedence over the meta-deref hoist
            // (mutually exclusive: an enum value is never a meta wrapper).
            JavaExpression bareEnumArg = tryBareEnumArg(arg, calleeFn, argIndex, compiler);
            if (bareEnumArg != null) {
                argExpr = bareEnumArg;
            } else if (hoistRoute == MetaDerefHoistRoute.STATEMENT_SINK
                    && metaDerefCallee != null) {
                // facet evalArgMetaDerefHoist (PR #237): a meta-deref arg of a function
                // call NESTED in a statement-level expression hoists its `final <Wrapper>
                // <name> = <chain>;` decl onto the nearest statement-hoist sink (the #236
                // ctorSetterMetaDerefHoist analogue at the evaluate-arg seat),
                // lifted ahead of the statement by prependStatementHoists. Decline (keep
                // the flat, still-waivered form) when no sink is reachable — a rule/report
                // path opens no session; a lambda interior stops the walk. Verifying the
                // sink BEFORE tryMetaDerefArg keeps the arg deref + its decl atomic (the
                // decl registers only when its sentinel will be lifted, never orphaned).
                JavaStatementScope argSink = ctx.scope().findStatementHoistSink();
                // facet guardedDerefHoist (PR #364): the RESTRICTED arm-deref channel —
                // the pathed-conditional ARM scope carries no full sink (deliberately),
                // so the #237 route reaches it through the fallback; the registered decl
                // relocates INTO the owning branch (or before a nested `if (`) via
                // ControlFlowHandler's windows (golden AnnaDsbUpiRequestUnderlyingForCredit
                // `translateIndexNameToId.evaluate((fieldWithMetaString0 == null ? null :
                // fieldWithMetaString0.getValue()))` ×3, numbered across the arms).
                if (argSink == null) {
                    argSink = ctx.scope().findArmDerefHoistSink();
                }
                if (argSink != null) {
                    JavaExpression hoisted = tryMetaDerefArg(
                            arg, compiled, unwrappedExpr, metaDerefCallee, argIndex, ctx,
                            compiler, hoists, argSink);
                    if (hoisted != null) {
                        argExpr = hoisted;
                    }
                } else {
                    // v3.2 seat 13 (D53, THE CLOSING SEAT - the LOUD register's evaluate-arg site R7, the chaos
                    // M5f rows C27NatCalled at DATA_RULE): no sink is reachable from this seat (the data-rule /
                    // type-condition path opens no session), so the pre-seat arm DECLINED and the flat form went
                    // out - `c27Check.evaluate(c27Nat)`, an Integer into a BigDecimal parameter, 18 of 18
                    // non-compiling by the seat-13 census, SILENT. The SAME plan tryMetaDerefArg commits on is
                    // consulted here (LAW 69); a planned coercion with no consumer is refused, never spliced flat.
                    refuseIfArgCoercionDropped(arg, compiled, unwrappedExpr, metaDerefCallee, argIndex, ctx,
                            compiler, expr, "no statement-hoist sink is reachable from this seat");
                }
            } else if (hoistRoute == MetaDerefHoistRoute.NONE && metaDerefCallee != null) {
                // v3.2 seat 13 (D53, site R7 - the second position with no hoist consumer): route NONE is a call
                // inside an inline function that is neither an extract/map lambda, a then body nor a filter
                // predicate, where the pre-seat code "kept the flat, still-waivered rendering". The same plan, the
                // same refusal; no chaos carrier sits here (every M5f row is the sink-less STATEMENT_SINK seat
                // above), so the position is covered by the predicate's identity, not by a measured row.
                refuseIfArgCoercionDropped(arg, compiled, unwrappedExpr, metaDerefCallee, argIndex, ctx,
                        compiler, expr, "the call sits in a position with no hoist consumer (route NONE)");
            } else if (hoistRoute != MetaDerefHoistRoute.NONE && metaDerefCallee != null) {
                // facet itemGetMetaDerefBlock (PR #346): the EXPLICIT-args sibling of the
                // #340 bare-invocation arm — an explicit `item` argument at any index of a
                // function call that IS a drainable map/extract lambda body (route BLOCK),
                // on the FUNCTION path (`ObservableQualification(item, securityType,
                // assetClass)` — cdm6 ObservableQualification / Qualify_InterestRate_
                // Option_DebtOption). The #285 recovery walk (implicitItemArgMeta) types
                // the item from the owning chain's terminal nav; tryMetaDerefArg's
                // param-mismatch + real-coercion gates then hoist `final <Wrapper> <id> =
                // item.get();` + deref. The RULE-path arm above (findEnclosingRule) is
                // untouched; green-safe by the same argument (a green `evaluate(item.get()
                // …)` compiles ⟹ item non-meta [walk null] OR the param takes the wrapper
                // [type-equality decline]). The OPEN-scope belt is the #340 cp4 catch.
                // The callee param must be meta-FREE: a [metadata]-annotated param EXPECTS
                // the wrapper — golden passes `item.get()` BARE there (the cp1
                // UpdateAmountForEachMatchingQuantity catch: its callees' price/quantity
                // params carry meta, and toJavaReferenceType strips meta so the
                // tryMetaDerefArg type-equality gate alone cannot decline).
                boolean fnItemLambdaArm = hoistRoute == MetaDerefHoistRoute.BLOCK
                        && arg instanceof RImplicitVariable
                        && !ctx.scope().isClosed()
                        && HandlerHelper.findEnclosingRule(arg) == null
                        && HandlerHelper.isInsideDrainableMapLambda(arg)
                        && argIndex < metaDerefCallee.inputs().size()
                        && MetaFieldGenerator.detectMetaKind(
                                metaDerefCallee.inputs().get(argIndex))
                                == MetaFieldGenerator.MetaKind.NONE;
                // facet filterPredicateMetaDeref (seat 28, law 6 AMENDMENT - measured by
                // the mid-seat whole-matrix D11, the control the cell-scoped suite scans
                // cannot be): on the LAMBDA_CHANNEL route an arg ROOTED ON A CALLER INPUT
                // declines - golden derefs such an arg with the input's own inline
                // ternary form and hoists NOTHING (cdm
                // UpdatePriceAmountForEachMatchingQuantity x10 cells ENTERED under the
                // unguarded widening: `final FieldWithMetaPriceSchedule ... =
                // (price == null ? null : price.getValue());` + a SECOND deref). Two
                // wrong cuts are on the record (LAW 82 honesty): the callee-param
                // meta-kind guard (the #346 comment's cp1 sentence) was REFUTED by the
                // targeted cdm re-run (priceUnitEquals' param is plain), and a
                // root-must-be-RImplicitVariable guard over-declined the healed carriers
                // (an elided implicit receiver walks to null, and the drr 5.61.0 bare-fn
                // form's operand is not an explicit item node). The landed predicate is
                // the MEASURED class itself: the chain root is a symbol bound to an
                // ATTRIBUTE that is an INPUT of the enclosing function. Scoped to
                // LAMBDA_CHANNEL: the BLOCK route's populations pre-date law 6 and keep
                // their bytes verbatim.
                //
                // facet filterPredicateMetaDeref (seat 29, law 6b - the #600 review bank,
                // a ZERO-CARRIER correctness fix: no corpus row moves, MEASURED at the
                // seat-29 chain, and the witness is the fixture): the caller-input set is
                // read THROUGH the dispatch base. A DISPATCH VARIANT (`func Foo(param:
                // Enum -> VALUE):`) declares no `inputs:` section of its own - AstBuilder
                // .visitFunction fills RFunction.inputs() only from the header's
                // `attribute+`, which a variant does not carry - so `argOwner.inputs()` is
                // EMPTY inside a variant while its body resolves every input NAME against
                // the BASE: LexicalResolutionPass.buildFunctionScope registers
                // `fn.dispatchBase().get().inputs()` for a variant and `fn.inputs()`
                // otherwise, and resolveSymbolsInScope binds THAT instance into
                // RSymbolReference.symbol(). Un-amended, the decline could never engage in
                // a dispatch impl and the hoist fired on an arg golden derefs inline - the
                // exact class the seat-28 mid-seat whole-matrix D11 caught in cdm.
                //
                // RFunction.dispatchBase() is consulted, NOT the sibling
                // HandlerHelper.dispatchBaseOf this file uses at the alias-signature seat
                // (LAW 69, precisely): the two select the base by DIFFERENT predicates
                // (dispatchBase = the first same-file sibling with an EMPTY operations
                // list, upstream's RosettaFunctionExtensions.getMainFunction; dispatchBaseOf
                // = the first same-model root element with an EMPTY dispatch), and only the
                // FORMER is the accessor buildFunctionScope used to produce the binding this
                // predicate tests for identity. Consulting the producer's own accessor is
                // what makes the identity test total. (They agree on all four corpus dispatch
                // groups - YearFraction, DayCountBasis, ComputeCalculationPeriod,
                // ProcessFloatingRateReset: every base is signature-only, so both terms hold.)
                //
                // THE NAME-EQUALITY DISJUNCT IS RETIRED (it was `in == argRootAttr ||
                // in.name().equals(argRootAttr.name())`). It is DEAD by the identity proof:
                // buildFunctionScope registers the very RAttribute INSTANCES it later hands
                // to setResolvedSymbol, so a reference bound to a caller input IS that
                // instance - and the only other RAttribute bindings a body reference can
                // carry are the OUTPUT (registered from the same source) and, on the
                // type-condition walk, a data-type attribute (no enclosing RFunction there).
                // A file-scope fallback cannot supply one (RFileScope.lookup returns
                // RRootElement) and a lambda param binds an RInlineFunction, not an
                // RAttribute, so the instanceof gate above already excludes shadowing
                // closure params. The disjunct's only two REACHABLE fires are FALSE ADMITS -
                // an output sharing an input's name (the output is registered LAST, so it
                // wins the scope and the name test would mis-report it as a caller input),
                // and a dispatch variant declaring its OWN same-named inputs (grammar-legal,
                // ignored by the scope's replace semantics). Both are corpus-absent; the
                // second is BANKED as the one shape that would witness the retirement.
                boolean lambdaChannelCallerInputArg = false;
                if (hoistRoute == MetaDerefHoistRoute.LAMBDA_CHANNEL) {
                    RExpression argRoot = arg;
                    int argRootDepth = 0;
                    while (argRootDepth++ < 32) {
                        if (argRoot instanceof RFeatureCall argRootFc
                                && argRootFc.receiver() != null) {
                            argRoot = argRootFc.receiver();
                        } else if (argRoot instanceof com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall argRootDfc
                                && argRootDfc.receiver() != null) {
                            argRoot = argRootDfc.receiver();
                        } else {
                            break;
                        }
                    }
                    if (argRoot instanceof RSymbolReference argRootSr
                            && argRootSr.symbol().orElse(null) instanceof RAttribute argRootAttr) {
                        RFunction argOwner = HandlerHelper.findEnclosingFunction(arg);
                        List<RAttribute> callerInputs = argOwner == null
                                ? List.of()
                                : argOwner.dispatchBase().map(RFunction::inputs)
                                        .orElseGet(argOwner::inputs);
                        lambdaChannelCallerInputArg =
                                callerInputs.stream().anyMatch(in -> in == argRootAttr);
                    }
                }
                int hoistMark = hoists.size();
                JavaExpression hoisted = lambdaChannelCallerInputArg ? null : tryMetaDerefArg(
                        arg, compiled, unwrappedExpr, metaDerefCallee, argIndex, ctx, compiler,
                        hoists, null, fnItemLambdaArm);
                if (hoisted != null) {
                    argExpr = hoisted;
                    if (fnItemLambdaArm && hoists.size() > hoistMark) {
                        lambdaChannelHoists = true;
                    }
                }
            }
            // facet enumArgCollections (PR #269): a PRESENT enum constant (the bareEnumArg-resolved
            // or qualified REnumValueRef form now in argExpr) passed to a MULTI callee parameter
            // hoists the constant + coerces it to the singletonList item→list form. Runs after the
            // bare-enum/meta-deref chain (argExpr is finalized) + before emptyMultiArg (mutually
            // exclusive — an enum constant is never the bare `null` emptyMultiArg keys on). RULE-
            // scoped (findEnclosingRule != null): the M7b-3 rule-body then-arg seat; a FUNCTION-body
            // enum-into-multi arg keeps its existing rendering — FUNCTION-byte-neutral by construction.
            JavaExpression enumMultiArg = HandlerHelper.findEnclosingRule(expr) != null
                    ? tryEnumSingletonListArg(arg, argExpr, bareEnumArg != null,
                            paramAcceptsMulti, calleeFn, argIndex, ctx, compiler,
                            ctx.scope().findStatementHoistSink())
                    : null;
            if (enumMultiArg != null) {
                argExpr = enumMultiArg;
            }
            // facet singleFnCallArgIntoMulti (PR #368): a DIRECT single-output fn-call
            // arg (`FilterOpenTradeStates(TradeStateForEvent(reportableEvent))`) into a
            // MULTI callee parameter hoists the inner call result + null-guards the
            // upstream convertNullSafe singletonList coercion — the #212
            // hoistSingleValueIntoMultiLeafOrNull law at the evaluate-arg seat (golden
            // IsActionTypeMODI `final TradeState tradeState =
            // tradeStateForEvent.evaluate(reportableEvent);` +
            // `filterOpenTradeStates.evaluate((tradeState == null ?
            // Collections.<TradeState>emptyList() :
            // Collections.singletonList(tradeState)))`). Disjoint from the enum arm
            // above (an explicit-args fn call is never an enum constant) and the
            // emptyMultiArg below (never the bare `null`). Green-safe by construction:
            // the pre-fix form passes the scalar call result where List is expected —
            // non-compiling, so no green file carries it.
            JavaExpression singleFnMultiArg = trySingleFnCallArgIntoMulti(
                    arg, argExpr, paramAcceptsMulti, argAsMulti, calleeFn, argIndex,
                    ctx, compiler);
            if (singleFnMultiArg != null) {
                argExpr = singleFnMultiArg;
            }
            // facet singleVarArgIntoMulti (W42 finding #27, PR #436): a bare
            // SINGLE-cardinality function-INPUT arg into a MULTI callee parameter takes
            // upstream's convertNullSafe item→list coercion DIRECTLY on the (escaped)
            // param name — no hoist, the param IS the local (golden
            // func-call-single-to-list C: `a.evaluate((_a == null ?
            // Collections.<Integer>emptyList() : Collections.singletonList(_a)))`).
            // The VARIABLE sibling of the #368 fn-call arm above (which hoists the call
            // result first) and the #269 enum-constant arm; disjoint from the #192
            // emptyMultiArg below (a bare `null` is no symbol ref). Green-safe by
            // construction: the pre-fix form passed the scalar where List is expected —
            // non-compiling, so no green file carries it.
            JavaExpression singleVarMultiArg = trySingleVarArgIntoMulti(
                    arg, argExpr, paramAcceptsMulti, argAsMulti, calleeFn, argIndex,
                    compiler);
            if (singleVarMultiArg != null) {
                argExpr = singleVarMultiArg;
            }
            // facet emptyMultiArg (PR #192): an ABSENT/empty evaluate argument (an
            // `empty` Rune literal, which compiles to the bare literal `null`) into a
            // MULTI (`0..*`, `List`-typed) callee parameter coerces to the typed empty
            // list `Collections.<ElementType>emptyList()`, not `null` — upstream's
            // item-to-list "empty" conversion (TypeCoercionService.xtend L377-380, the
            // same coercion FunctionExpressionRenderer's arm C6 renders for a MULTI
            // conditional-output implicit else). This is the third cardinality dimension
            // of the PR #171 evaluate_arg_consumption law: PR #131/#171 widened a MULTI
            // ARGUMENT into a multi parameter; PR #191 (tailMulti) widened a SINGLE
            // Mapper chain into a multi parameter via `.getMulti()`; this widens an
            // ABSENT argument into a multi parameter to the typed empty list. The
            // SINGLE-parameter absent arg stays `null` (the gate keys on the callee
            // PARAMETER's cardinality via the PR #191 paramAcceptsMulti probe), so the
            // sibling scalar `null` args on the same call are untouched. Green-safe by
            // construction: the fork rendered `null` for EVERY absent multi argument
            // while the golden always renders `emptyList()`, so every such file was
            // already a mismatch (waivered) — no green file carries the pre-fix form.
            JavaExpression emptyMultiArg = tryEmptyMultiArg(
                    argExpr, paramAcceptsMulti, calleeFn, argIndex, compiler);
            if (emptyMultiArg != null) {
                argExpr = emptyMultiArg;
            }
            argsJoiner.add(argExpr.renderToString());
            combinedRefs.addAll(argExpr.getRefs());
            combinedStaticWildcards.addAll(argExpr.getStaticWildcardImports());
            argIndex++;
        }
        // Function-call receiver (engine PR #11 / facet F3, completeness-ledger
        // Tier-3 #20): derive from the RESOLVED RFunction's simple name via the
        // dependency collector's single-source-of-truth lowerCamelCase — so the
        // receiver matches the {@code @Inject} field FunctionDependencyCollector
        // registers for the same function. The golden uses the injected
        // lowerCamel instance (e.g. {@code extractPartyResponsibleForReportingIdentifier}),
        // NEVER the UpperCamel type name. Deriving from {@code callee.name()}
        // (the SIMPLE name) — NOT {@code lowerCamelCase(expr.name())}: a
        // cross-namespace reference name is namespace-qualified
        // (e.g. {@code cde.collateral.Foo}), which lowerCamelCase would leave
        // mangled into a reference to an undeclared identifier (its first char is
        // already lowercase). Falls back to the raw reference name when the symbol
        // is unresolved or is not an RFunction (partial-resolution safety — the
        // pre-fix behaviour, exercised by the lowercase-name with-args unit tests).
        // facet injectedRuleRef (PR #263): an EXPLICIT-args reference to a RULE
        // (`cde.valuation.ValuationMethod(GetValuation)` — an RRule symbol WITH args,
        // not the no-arg bare-rule branch above) inside a reporting-rule body invokes
        // the injected `<Name>Rule` field, exactly like the no-arg `then <rule>` form.
        // Derive the receiver from the rule's GENERATED class via the SAME
        // RFunction.fromRule → toFunctionJavaClass → lowerCamelCase path
        // FunctionDependencyCollector.injectRuleDependency uses (single source of
        // truth — the receiver matches the @Inject field name it registers, now that
        // the collector's RRule branch admits explicit-args refs too). Pre-fix the
        // RRule fell through to `.orElse(name)` = the raw cross-namespace qualified
        // reference name (`cde.valuation.ValuationMethod`), a non-compiling undefined
        // identifier — so every carrier is already a waivered mismatch (green-safe by
        // construction; no green file carries the mangled form).
        // facet injectRuleDepInFunctions (PR #332): the rule-family seat gate
        // (findEnclosingRule != null || isReportOriginSeat — the #263 gate widened at
        // #322 for report-origin synthetics) was DROPPED, together with the collector's
        // mirror ruleGateSimpleName gate: a plain FUNCTION body's explicit-args rule
        // reference now renders the injected `<name>Rule` field receiver too, matching
        // upstream ExpressionGenerator.callableWithArgsCall (the RosettaRule case is
        // rendered identically to the Function case — the injected dependency-instance
        // receiver — with no host-kind gate; upstream JavaDependencyProvider injects the
        // field unconditionally on both paths). Field and receiver stay a consistent
        // single-source-of-truth pair (ruleDependencyFieldName both sides). Green-safe
        // by construction: the pre-fix FUNCTION-path receiver was the raw rosetta
        // reference name (dotted cross-namespace `cde.datetime.EffectiveDate` or bare
        // same-namespace UpperCamel `EventType`) — non-compiling, so no green file
        // carries it; the defensive degrade-to-skip below keeps a resolution failure at
        // the pre-fix bytes, exactly matching the collector's skip.
        String receiver = null;
        RRule explicitArgsRule = expr.symbol()
                .filter(RRule.class::isInstance).map(RRule.class::cast).orElse(null);
        if (explicitArgsRule != null
                && compiler.getTypeTranslator() != null
                && compiler.getGeneratorModel() != null) {
            try {
                RFunction ruleSynthetic = RFunction.fromRule(explicitArgsRule);
                JavaClass<?> ruleClass = compiler.getTypeTranslator().toFunctionJavaClass(
                        ruleSynthetic, compiler.getGeneratorModel().symbolId(ruleSynthetic));
                // PR #322: route through ruleDependencyFieldName so a lowercase-named
                // rule class's `_`-escaped field (barrierRule -> _barrierRule) matches
                // the @Inject field the collector registers.
                receiver = FunctionDependencyCollector.ruleDependencyFieldName(ruleClass.getSimpleName());
            } catch (RuntimeException ex) {
                // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
                // boundary (JavaClassGenerator), which attaches the target path and reports
                // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
                if (ex instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                // Mirror FunctionDependencyCollector.injectRuleDependency's defensive
                // degrade-to-skip: a resolution failure leaves receiver null and falls
                // through to the prior (still-waivered) variable-path form rather than
                // propagating an opaque generation crash. The collector's RRule branch
                // would likewise skip the @Inject field, so the two stay consistent.
                receiver = null;
            }
        }
        if (receiver == null) {
            receiver = expr.symbol()
                    .filter(RFunction.class::isInstance)
                    .map(RFunction.class::cast)
                    // facet member_name_disambiguation: number the dependency receiver
                    // name0 when it collides with a same-named shortcut/alias.
                    // facet injectDepCollision (PR #364): the callee-aware overload
                    // numbers a same-simple-name dep-vs-DEP collision group member too.
                    .map(callee -> disambiguateDependencyReceiver(expr, callee,
                            FunctionDependencyCollector.lowerCamelCase(callee.name()),
                            compiler))
                    .orElse(name);
        }
        // facet dispatchVariantParamResolution (PR #369): a call bound to an
        // {@link com.regnosys.rosetta.ast.types.RLibraryFunction} (the rune-dsl BUILTIN
        // basicfunctions — `Min`/`Max`/`IsLeapYear` resolve there when no model import
        // is in scope) renders the RUNTIME form, not the generated-function
        // `<recv>.evaluate(…)`:
        //   - Min/Max (generic T→T): `new <Name>().execute(<args>)` with the args RAW
        //     (golden `MapperS.of(new Min().execute(MapperS.of(endDate).<Integer>map(
        //     "Day", Date::getDay).get(), 30))`, YearFraction _30_360) — flows into the
        //     standard MapperS.of wrap below;
        //   - IsLeapYear (declared param `number`, Integer arg): the arg hoists
        //     `final Integer integer = <chain>.get();` (deferred-token — the #277
        //     numCoerceArgHoist naming law) and the WHOLE wrap is the null-guarded
        //     `integer == null ? MapperS.<Boolean>ofNull() : MapperS.of(new IsLeapYear()
        //     .execute(BigDecimal.valueOf(integer)))` — the guard lifts AROUND the
        //     MapperS.of (the runtime execute NPEs on null, unlike a generated
        //     evaluate), so this arm returns the two-statement block directly.
        // The pre-fix render was the non-compiling symbol-echo `Min.evaluate(…)` (no
        // such static method), so every carrier is an already-waivered mismatch —
        // green-safe by construction.
        String callText = receiver + ".evaluate(" + argsJoiner + ")";
        if (expr.symbol().orElse(null)
                instanceof com.regnosys.rosetta.ast.types.RLibraryFunction) {
            if ("Min".equals(name) || "Max".equals(name)) {
                callText = "new " + name + "().execute(" + argsJoiner + ")";
                combinedRefs.add("Min".equals(name)
                        ? JavaClass.from(com.rosetta.model.lib.functions.Min.class)
                        : JavaClass.from(com.rosetta.model.lib.functions.Max.class));
            } else if ("IsLeapYear".equals(name) && expr.args().size() == 1) {
                return isLeapYearGuardedBlock(argsJoiner.toString(), combinedRefs,
                        combinedStaticWildcards, ctx, compiler);
            }
        }
        JavaExpression innerCall = JavaExpression.from(
                callText, null,
                combinedRefs, combinedStaticWildcards);
        // facet ruleMetaLiftResidueSeats (seat 6, the with-args exists rung): the
        // #315/#331 exists-operand meta-wrap now reaches the WITH-ARGS rule-call seat
        // (`cde.price.Spread(common.PayoutLeg2) exists` — the SpreadOfLeg carriers;
        // the arms previously lived only in the no-args renderImplicitRuleInvocation,
        // so a with-args meta-output rule call under exists kept the bare
        // `exists(MapperS.of(<rule>.evaluate(<argRule>.evaluate(item.get()))))`).
        // IDENTICAL gates + recovery + conditional build via the shared helper —
        // hoist-free calls only (a hoisting arg keeps the existing block route below,
        // today's bytes). Green-safe per the #315 argument verbatim: the bare form
        // compiles + behaves identically, so every carrier is a COMPILES_DIVERGENT
        // byte mismatch and no green file carries the distributed wrap.
        if (explicitArgsRule != null && hoists.isEmpty()) {
            JavaStatementBuilder eowArgsWrap = tryExistsOperandMetaWrap(
                    expr, explicitArgsRule, innerCall, ctx, compiler);
            if (eowArgsWrap != null) {
                return eowArgsWrap;
            }
        }
        // facet ruleCallArmMetaWrap (seat 25, law B): the #265/#331/#372 single meta-wrap
        // now ALSO reaches the WITH-ARGS rule-call seat — the NotionalCurrencyOfLeg2 arm
        // calls (`if … then CDEInterestRateNotionalCurrency(<argChain>) …` inside the
        // extract lambda) never reach the no-args renderImplicitRuleInvocation (the
        // seat-25 LAW-75 probe measured ZERO P25A lines for them; they compile here),
        // so golden's in-branch value hoist + null-safe builder wrap
        // (`final String string0 = <rule>.evaluate(<argChain>); return string0 == null
        // ? MapperS.<FieldWithMetaString>ofNull() : MapperS.of(FieldWithMetaString
        // .builder().setValue(string0).build());`) never fired. IDENTICAL gates +
        // recovery + wrap via the shared helper (LAW 69 — the seat-6 with-args exists
        // rung precedent, one block up). Hoist-free calls only (a hoisting arg keeps
        // the existing block route, today's bytes). Green-safe per the #265 argument:
        // the bare `MapperS.of(<call>)` compiles + behaves identically, so every
        // carrier is a COMPILES_DIVERGENT byte mismatch — a green file cannot carry
        // the golden wrap form.
        if (explicitArgsRule != null && hoists.isEmpty()) {
            JavaStatementBuilder armsMetaWrap = trySingleRuleMetaWrap(
                    expr, explicitArgsRule, innerCall, ctx, compiler, true);
            if (armsMetaWrap != null) {
                return armsMetaWrap;
            }
        }
        // facet multi_cardinality_value_wrap: a call to a function whose OUTPUT is
        // multi returns a List<? extends Item> from evaluate(...), which upstream
        // 9.83.0 coerces to the witnessed MapperC.<Item>of(callee.evaluate(...)) —
        // not the scalar MapperS.of(...). Same regression-safety argument as the
        // variable path above (no green file carries a surviving
        // MapperS.of(<multi call>) render; transient wraps strip identically).
        JavaExpression multiCallWrap = tryMultiValueWrap(
                calleeFn == null ? null : calleeFn.output().orElse(null),
                innerCall, compiler, expr);
        // facet fnIoMetaCallResultType (PR #433, finding #21): a call to a function
        // whose SINGLE output is [metadata …]-annotated returns the concrete wrapper
        // from evaluate(...) (the #186/#342 signature law), so the MapperS.of wrap
        // REPORTS the wrapper item type — the TEXT is byte-unchanged (the #176
        // two-arg typed-wrap overload, the metaInputParam pattern at the CALL seat).
        // The typed read lets metaFeatureShortFormOrNull fire on a call-receiver
        // `-> scheme` nav (upstream `MapperS.of(a.evaluate(myInput)).map("getMeta",
        // _a->_a.getMeta()).map("getScheme", _a->_a.getScheme())` — the
        // func-meta-call-scheme oracle golden).
        JavaStatementBuilder wrapped;
        if (multiCallWrap != null) {
            wrapped = multiCallWrap;
        } else if (explicitArgsRule != null
                && HandlerHelper.findEnclosingRule(expr) != null
                && compiler.getGeneratorModel() != null
                && compiler.getTypeTranslator() != null
                && compiler.getTypeUtil() != null
                && NavigationHandler.recoverInnerRuleMetaWrapper(explicitArgsRule, compiler) == null
                && NavigationHandler.ruleOutputProvesMulti(expr, compiler)) {
            // facet ruleOutputCardFormSeats (seat 8, Rung A): a WITH-ARGS call to a
            // MULTI-bodied rule takes the witnessed MapperC wrap — the verbatim
            // no-args sibling arm (#273/#274, the renderImplicitRuleInvocation
            // MapperC arm below) at the seat upstream never split from it
            // (ExpressionGenerator.xtend's callableWithArgsCall renders Function
            // and RosettaRule through ONE case; the List-typed evaluate coerces
            // MapperC.<X>of at every Mapper seat — TypeCoercionService). The
            // tryMultiValueWrap consult above is RFunction-scoped (calleeFn is
            // null for a rule), so pre-seat every carrier fell to the scalar
            // MapperS.of — non-compiling (MapperS.of(<List>) infers
            // MapperS<List<X>>; .getMulti() yields List<List<X>>, rejected at
            // every toBuilder/decl consumer), banded by construction. Gates:
            // findEnclosingRule mirrors the no-args entry gate (the FUNCTION-body
            // with-args rule call keeps today's raw stripped form —
            // FUNCTION-byte-neutral by construction); a META-output callee
            // DECLINES to today's bytes (zero corpus carriers; the faithful
            // consumer form would be the upstream itemToWrapper element re-lift,
            // unknowable from goldens — the seat-5 decline law); the multi proof
            // is the shared ruleOutputProvesMulti predicate (the #367 law — the
            // wrap and the callee's own List signature cannot disagree).
            JavaClass<?> outElem = compiler.getTypeTranslator().toJavaReferenceType(
                    compiler.getGeneratorModel().workspace().getInferredType(expr).type());
            wrapped = JavaExpression.wrappedInMapperCOfSingle(innerCall,
                    compiler.getTypeUtil().wrap(compiler.getTypeUtil().MAPPER_C, outElem),
                    outElem);
        } else {
            RAttribute callOutputAttr = calleeFn == null ? null
                    : calleeFn.output().orElse(null);
            RJavaWithMetaValue callResultWrapper = callOutputAttr == null ? null
                    : NavigationHandler.metaWrapperOf(callOutputAttr, compiler);
            JavaTypeUtil tuCall = compiler.getTypeUtil();
            wrapped = callResultWrapper != null && tuCall != null
                    ? JavaExpression.wrappedInMapperSOf(innerCall,
                            tuCall.wrap(tuCall.MAPPER_S, callResultWrapper))
                    : JavaExpression.wrappedInMapperSOf(innerCall);
        }
        // ref_coercion: a hoisted meta-deref (or bigint) arg makes the whole call a BLOCK
        // (leading `final … = …;` decl(s) + the MapperS.of(…) trailing expression).
        // Reachable under the BLOCK route from EITHER an extract/map lambda whose body
        // IS this call (the block flows to CollectionHandler.compileLambda's
        // block-lambda renderer) OR a direct then-body call in a rule (PR #129 —
        // FunctionExpressionRenderer.renderThenExtractSet lifts the leading decl(s)
        // to statement level before the `output =` line).
        if (!hoists.isEmpty() && wrapped instanceof JavaExpression wrappedExpr) {
            // facet itemGetMetaDerefBlock (PR #346): the FUNCTION-path implicit-item hoist
            // (lambdaChannelHoists) ALSO takes the lambda channel even though its route is
            // BLOCK — the decl registers on the per-pass lambda-body scope and compileLambda
            // block-converts + drains, so the #257 double-render's discarded pass stays
            // self-contained (the #340 JavaBlockBuilder route degraded at the operand seats:
            // the second pass's wrapper-local landed in a finalized scope).
            if (hoistRoute == MetaDerefHoistRoute.LAMBDA_CHANNEL || lambdaChannelHoists) {
                // facet evaluate_arg_consumption (arm B3): the call is a NESTED
                // operand inside an extract/map lambda — the intermediate operand
                // consumers between here and the lambda body render eagerly and
                // accept only JavaExpression, so a block return would degrade the
                // whole statement. Register the decl(s) on the lambda-body scope;
                // CollectionHandler.compileLambda drains them into the
                // brace-delimited block body (golden's hoist-at-lambda-top shape),
                // and the flat call expression stays consumable in place.
                for (JavaStatement hoist : hoists) {
                    ctx.scope().registerPendingLambdaHoist(hoist);
                }
                return wrapped;
            }
            return new JavaBlockBuilder(JavaStatementList.of(hoists), wrappedExpr);
        }
        return wrapped;
    }

    /**
     * facet multi_cardinality_value_wrap — wrap a MULTI-cardinality value as the
     * witnessed {@code MapperC.<Item>of(...)} the upstream 9.83.0 goldens carry,
     * instead of the cardinality-blind scalar {@code MapperS.of(...)}.
     *
     * <p>Serves both emission shapes of {@code handle(RSymbolReference)}:
     * a bare-variable reference whose resolved symbol is a multi {@link RAttribute}
     * (function input/output, Java value {@code List<? extends Item>}), and an
     * explicit-args function call whose callee's OUTPUT attribute is multi
     * ({@code evaluate(...)} returns {@code List<? extends Item>}).
     *
     * <p>Declines (returns {@code null} → caller keeps the MapperS wrap) when:
     * <ul>
     *   <li>{@code attr} is null (unresolved symbol / non-attribute symbol /
     *       output-less callee) — partial-resolution safety;</li>
     *   <li>the attribute's declared cardinality is not multi
     *       ({@link GeneratorModel#isMulti});</li>
     *   <li>the attribute carries a {@code [metadata …]} annotation — the witness
     *       for a meta-wrapped item is the concrete {@code FieldWithMetaX} class,
     *       which this path cannot yet name (the fork's
     *       {@code JavaTypeTranslator.toMetaJavaType} returns the GENERIC
     *       {@code FieldWithMeta<T>} form); conservative decline keeps the
     *       (still-waivered) MapperS form rather than emit a wrong witness;</li>
     *   <li>the generator model / type translator is unavailable (no-arg compiler
     *       construction in unit tests), or the item type does not resolve
     *       ({@link RMissingType} → an {@code Object} witness would be wrong).</li>
     * </ul>
     *
     * <p>The wrap factory ({@link JavaExpression#wrappedInMapperCOfSingle}) mirrors the
     * MapperS wrap's structural-unwrap contract, so transient wraps that downstream
     * strip sites remove (evaluate-args, assignments) strip identically — a multi param
     * passed into another function's {@code evaluate(...)} still renders raw.
     *
     * @param attr the resolved multi-candidate attribute (variable symbol or callee
     *        output), or {@code null}
     * @param inner the unwrapped inner expression (bare name or {@code …evaluate(…)})
     * @param compiler the expression compiler (generator model + type translator source)
     * @return the witnessed MapperC wrap, or {@code null} to keep the MapperS wrap
     */
    private JavaExpression tryMultiValueWrap(RAttribute attr, JavaExpression inner,
            ExpressionCompiler compiler, RExpression refExpr) {
        JavaClass<?> witness = mapperCOfWrapWitness(attr, compiler);
        if (witness == null) {
            return null;
        }
        // facet fqnWitnessMapperC: when the MapperC.<Item>of witness simple name collides
        // with the enclosing function OUTPUT type (different FQN), render the witness
        // FQN-inline and suppress its (duplicate, non-compiling) import — the MapperC.of
        // render position of the #194–#197 first-claim-wins import-collision law. Null (no
        // collision) keeps the bare-simple-name witness byte-unchanged.
        String fqnWitness = mapperCWitnessOutputCollisionFqn(witness, refExpr, compiler);
        // facet fnIoMetaListWrap (PR #433): a META wrapper witness stamps the wrap's
        // MapperC<Wrapper> type (the #430 lolLambdaParamTypeStamp law at the
        // multi-input seat) so the wrapper-item consumers — the sort key-extractor,
        // the aggregate stream fusion, the whole-output "Type coercion" terminal —
        // can read it; plain witnesses keep the untyped wrap byte-verbatim.
        JavaType metaWrapType = null;
        if (witness instanceof RJavaWithMetaValue metaWitness
                && compiler.getTypeUtil() != null) {
            metaWrapType = compiler.getTypeUtil()
                    .wrap(compiler.getTypeUtil().MAPPER_C, metaWitness);
        }
        return JavaExpression.wrappedInMapperCOfSingle(inner, metaWrapType, witness, fqnWitness);
    }

    /**
     * facet fqnWitnessMapperC — the FQN-inline render string for a
     * {@code MapperC.<Item>of(...)} witness whose Java simple name collides with the
     * enclosing function OUTPUT type (same simple name, DIFFERENT fully-qualified name),
     * or {@code null} when there is no collision (the bare-simple-name witness renders
     * unchanged). On collision the fork's bare witness import duplicates the simple name
     * the output import already claims — a duplicate-simple-name import that does NOT
     * compile, so every collision carrier is already waivered (green-safe by construction;
     * exemplar {@code MapPartyList}: witness {@code fpml.consolidated.shared.Party} vs
     * output {@code cdm.base.staticdata.party.Party}). Mirrors
     * {@code NavigationHandler.witnessOutputCollisionFqn} at the MapperC.of locus — the
     * witness here is already resolved to its {@link JavaClass} by
     * {@link #mapperCOfWrapWitness}. Since PR #433 (facet fnIoMetaListWrap) that
     * witness MAY be a concrete meta wrapper ({@code FieldWithMetaString}) rather
     * than a plain model type; the collision math is unchanged — the comparison is
     * canonical-name-based and a wrapper simple name colliding with the output
     * type takes the same FQN-inline escape a plain witness would.
     */
    private String mapperCWitnessOutputCollisionFqn(JavaClass<?> witness, RExpression refExpr,
            ExpressionCompiler compiler) {
        if (witness == null || refExpr == null) {
            return null;
        }
        var tt = compiler.getTypeTranslator();
        GeneratorModel gm = compiler.getGeneratorModel();
        if (tt == null || gm == null) {
            return null;
        }
        RFunction fn = HandlerHelper.findEnclosingFunction(refExpr);
        if (fn == null) {
            return null;
        }
        RAttribute out = fn.output().orElse(null);
        if (out == null || out.typeCall() == null) {
            return null;
        }
        // A META-annotated output's EMITTED type is the concrete meta wrapper
        // (ReferenceWithMetaX / FieldWithMetaX, PR #186 metaWit), NOT the bare base X that
        // toJavaReferenceType resolves below. A PLAIN witness can never share that
        // wrapper simple name; since PR #433 the witness itself may be a meta wrapper
        // (facet fnIoMetaListWrap), and a wrapper-vs-wrapper simple-name collision
        // (same value type at input AND output) is same-CANONICAL by construction in
        // the single-metafields-package model — no FQN escape is ever due here.
        if (MetaFieldGenerator.detectMetaKind(out) != MetaFieldGenerator.MetaKind.NONE) {
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
     * The {@code MapperC.<Item>of(...)} wrap's witness class when {@code attr}
     * renders as a multi-value MapperC root, or {@code null} when the wrap
     * declines (the legacy {@code MapperS.of} render). EXACTLY
     * {@link #tryMultiValueWrap}'s decline ladder — gm-aware multi + meta-free
     * + resolvable item type + translatable witness — extracted so readers of
     * the chain's wrapper KIND (facet meta_coercion_numbering:
     * {@code NavigationHandler.chainRendersMapperC}'s disguised-navigation
     * root arm) consult the SAME predicate that renders the wrap; the reading
     * and the rendered chain cannot disagree. Package-private for its sibling
     * handlers: {@code NavigationHandler}'s wrapper-kind read and, since PR #610
     * (v3.1 C2d family 3), {@code CollectionHandler.caseBodyCompilesMulti} — the
     * switch-case MULTI read that replaced the {@code MapperC} prefix tests.
     */
    static JavaClass<?> mapperCOfWrapWitness(RAttribute attr, ExpressionCompiler compiler) {
        if (attr == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        if (gm == null || translator == null) {
            return null;
        }
        if (!gm.isMulti(attr)) {
            return null;
        }
        for (var annotationRef : attr.annotationRefs()) {
            if ("metadata".equals(annotationRef.annotationName())) {
                // facet fnIoMetaListWrap (PR #433, findings #21/#22): a MULTI
                // meta-annotated input wraps `MapperC.<ConcreteWrapper>of(name)` —
                // the func-meta-sort oracle golden (`MapperC.<FieldWithMetaString>
                // of(myInputs)`). The historical decline ("toMetaJavaType returns
                // the GENERIC FieldWithMeta<T> form") is solved by the #342
                // metaWrapperOf resolution, which names the CONCRETE wrapper; a
                // wrapper that still fails to resolve keeps the decline (the
                // pre-facet MapperS form).
                return NavigationHandler.metaWrapperOf(attr, compiler);
            }
        }
        RType itemType = gm.getType(attr);
        if (itemType == null || itemType instanceof RMissingType) {
            return null;
        }
        return translator.toJavaReferenceType(itemType);
    }

    /**
     * How a hoisted meta-deref/bigint arg declaration leaves the call site (facet
     * evaluate_arg_consumption arm B3 split of the former boolean block gate).
     */
    private enum MetaDerefHoistRoute {
        /** No hoist-consuming context — keep the flat (still-waivered) rendering. */
        NONE,
        /**
         * The call sits in a position whose renderer consumes a
         * {@link JavaBlockBuilder} directly (decl(s) + trailing expression).
         */
        BLOCK,
        /**
         * The call is a NESTED operand inside an extract/map lambda — decl(s)
         * register on the lambda-body scope
         * ({@code JavaStatementScope.registerPendingLambdaHoist}) and
         * {@code CollectionHandler.compileLambda} drains them into the block
         * lambda body; the call itself stays a flat expression.
         */
        LAMBDA_CHANNEL,
        /**
         * The call is a NESTED operand inside a statement-level expression — a
         * conditional-branch / whole-RHS SET value, a {@code return} value, or a
         * ctor/builder-setter value — with NO enclosing extract/map lambda. The
         * hoisted meta-deref decl(s) register as sentinel-bearing strings on the
         * nearest statement-hoist sink
         * ({@link JavaStatementScope#registerStatementHoist}) and
         * {@code FunctionExpressionRenderer.prependStatementHoists} lifts them ahead
         * of the enclosing statement (the #236 {@code ctorSetterMetaDerefHoist}
         * analogue at the evaluate-arg seat); the call itself stays a flat
         * expression (facet evalArgMetaDerefHoist, PR #237). The sink-reachability
         * check happens at the call site (a rule/report path opens no session; a
         * lambda interior stops the {@code findStatementHoistSink} walk → decline).
         */
        STATEMENT_SINK
    }

    /**
     * Route for the ref_coercion / bigint meta-deref-arg hoist. The symbol must be an
     * {@link RFunction} called in one of the hoist-consuming contexts:
     * <ol>
     *   <li>(a) inside an {@code extract}/{@code map} lambda
     *       (the {@link #nearestEnclosingInlineFunction} parent is an {@link RExtractExpr}) —
     *       when the call IS the lambda body, the {@link JavaBlockBuilder} it produces is
     *       consumed by {@code CollectionHandler.compileLambda} (the block-lambda renderer;
     *       PR #109 ref_coercion / PR #128 bigint) → {@link MetaDerefHoistRoute#BLOCK};
     *       when the call is NESTED deeper in the body's expression tree (an operand of a
     *       logical/comparison chain — the intermediate consumers render eagerly and accept
     *       only {@link JavaExpression}, so a block here would degrade the whole statement)
     *       → {@link MetaDerefHoistRoute#LAMBDA_CHANNEL} (facet evaluate_arg_consumption
     *       arm B3; the decls drain into the same block-lambda body).</li>
     *   <li>(b) a then-body function call in a reporting RULE (parent {@link RThenExpr} and
     *       {@link com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper#findEnclosingRule}
     *       non-null, the call being the then lambda's OWN body) — PR #129; the block is
     *       lifted to STATEMENT level by {@code FunctionExpressionRenderer.renderThenExtractSet}
     *       (the line before the {@code output =} assignment). Rule-body-only: a FUNCTION
     *       then-extract has no statement-level lift path. A NESTED call in a then body has
     *       no drain site (then bodies do not route through {@code compileLambda}) — NONE,
     *       the flat (still-waivered) form.</li>
     *   <li>(c) facet {@code filterPredicateMetaDeref} (seat 28, law 6): a FILTER
     *       predicate lambda, in EITHER predicate position -- the call NESTED in the
     *       predicate (the #363 {@code filter <Rule> = True} form) or the call that IS
     *       the predicate body (the bare {@code filter <Fn>} form and the
     *       explicit-args {@code filter <Fn>(<metaNav>, ...)} form) -- routes
     *       {@link MetaDerefHoistRoute#LAMBDA_CHANNEL}: golden hoists the wrapper at
     *       lambda top and passes the null-guarded deref, and
     *       {@code CollectionHandler.compileLambda} drains the decl into the block
     *       body. {@link MetaDerefHoistRoute#BLOCK} is provably WRONG for the direct
     *       body: {@code compileLambda}'s FILTER_PREDICATE {@code MapperS.of} strip
     *       runs only on a {@link JavaExpression} body, so a {@code JavaBlockBuilder}
     *       would carry the spurious wrap into its {@code return}.</li>
     * </ol>
     * A meta-deref/bigint arg in any OTHER position (a FUNCTION then-body operand with
     * no enclosing lambda) keeps the flat, still-waivered rendering
     * ({@link MetaDerefHoistRoute#NONE}).
     */
    private static MetaDerefHoistRoute metaDerefHoistRoute(RSymbolReference expr) {
        // facet ruleCalleeMetaDeref (PR #336): route reporting-RULE callees too (an RRule
        // `extract <Rule>`), not only RFunction — the caller resolves the rule's synthetic
        // RFunction.fromRule as the meta-deref callee. Position-based routing below is symbol-
        // kind-agnostic. Green-safe: every route USE downstream is guarded by a non-empty
        // {@code hoists}, which only tryMetaDerefArg populates (and only for a genuine
        // meta-wrapper→value arg), so a non-meta rule call keeps the flat form regardless.
        if (expr.symbol().filter(s -> s instanceof RFunction || s instanceof RRule).isEmpty()) {
            return MetaDerefHoistRoute.NONE;
        }
        RInlineFunction inline = nearestEnclosingInlineFunction(expr);
        if (inline == null) {
            // (c) PR #143 (meta_deref_input_hoist): a DIRECT top-level function call
            //     that IS a reporting RULE's body expression (no enclosing lambda, no
            //     `then`). The JavaBlockBuilder hoist is lifted ONLY by
            //     FunctionExpressionRenderer.renderOperation's generic SET path (the
            //     direct-SET analogue of the PR #129 then-extract lift): RFunction.fromRule
            //     wraps the RRule's body expression into a plain (non-segment, non-ADD) SET
            //     ROperation, whose else-branch performs the lift. Gate PRECISELY to that
            //     lifting position (Copilot PR #143 R1): expr must be the RRule's OWN body
            //     expression (expr.parent() instanceof RRule — RRule.expression is its only
            //     RExpression child). A call NESTED in a conditional branch / operand / outer
            //     call / alias keeps an RExpression (or RShortcut) parent and routes to
            //     renderConditionalAssignment / arg-rendering / renderAlias, which do NOT
            //     lift — there an unlifted multi-statement block would be inlined into an
            //     expression slot → invalid Java; declining keeps it on the flat
            //     (still-waivered) form (zero regression). The RFunction-symbol guard
            //     above still applies. The FUNCTION analogue — a top-level no-segment
            //     SET operation, whose renderOperationInner else-branch DOES lift — is
            //     the facet convertNullSafe arm added immediately below (PR #187).
            if (expr.parent() instanceof RRule) {
                return MetaDerefHoistRoute.BLOCK;
            }
            // facet convertNullSafe (B, argument position): a DIRECT top-level
            // FUNCTION-body call whose meta args hoist, in the LIFTABLE statement
            // position — a no-segment SET operation, whose
            // FunctionExpressionRenderer.renderOperationInner ELSE-branch lifts the
            // block decl(s) to statement level via liftThenBodyHoists (the function
            // analogue of the PR #143 / #129 rule-body lift). For a genuine function
            // operation, AstBuilderHelper parents the value expression to its
            // ROperation (RFunction.fromRule keeps the RRule parent instead — handled
            // above). A SEGMENT set (renderSetBuilderChain, no lift), an ADD
            // (addAll), or a conditional / return / alias-body position has NO
            // statement-level lift path → NONE: an unlifted multi-statement block
            // inlined into an expression slot is invalid Java, so declining keeps the
            // flat, still-waivered form (zero regression).
            if (expr.parent() instanceof ROperation op
                    && op.operator() == OperationOp.SET
                    && op.segment().isEmpty()) {
                return MetaDerefHoistRoute.BLOCK;
            }
            // facet convertNullSafe (slice 2, B at the alias-body / RETURN seat): a
            // DIRECT top-level FUNCTION call that IS an alias (shortcut) body. The
            // alias renderer FunctionExpressionRenderer.renderAliasLiftedReturnOrNull
            // (called from compileAliases beside the PR #183 returnIte ladder) LIFTS
            // the resulting JavaBlockBuilder (the meta-deref hoist decl(s) + the
            // trailing MapperS.of(callee.evaluate(…)) value) to the statement form
            // `final <Meta> name = …; return <trailing>;`. RShortcut.children() is its
            // single body expression, so expr.parent() instanceof RShortcut pins
            // EXACTLY the alias-body-top position; a call NESTED in the body (an
            // operand / branch / outer call) keeps an RExpression parent and routes to
            // a non-lifting renderer → NONE, the flat (still-waivered) form (zero
            // regression).
            if (expr.parent() instanceof RShortcut) {
                return MetaDerefHoistRoute.BLOCK;
            }
            // facet evalArgMetaDerefHoist (PR #237): every OTHER top-level (no enclosing
            // extract/map lambda) function-call position — a NESTED operand inside a
            // conditional-branch / whole-RHS SET value (Qualify_Increase /
            // Qualify_PartialTermination / UnitEquals), a `return` value
            // (EquityPerformance), a ctor/builder-setter value (GetNotionalAmount), or a
            // conditional ladder (Create_AnnaDsbUpiRequestUnderlyingForCredit) — routes to
            // STATEMENT_SINK. Unlike the BLOCK positions above (whose renderer consumes a
            // JavaBlockBuilder directly), the intermediate operand consumers here render
            // eagerly and accept only JavaExpression, so the hoist decl(s) lift to the
            // enclosing STATEMENT via the statement-hoist sink instead. The call site
            // verifies a sink is reachable (findStatementHoistSink != null) before
            // committing — a rule/report path opens no session and a lambda interior stops
            // the walk, so those keep the flat (still-waivered) form. tryMetaDerefArg's
            // own coercion-service probe + single-param + meta-wrapper-item-type gate keeps
            // the route a no-op for every non-meta-deref call; a bare meta wrapper passed
            // where the callee expects the bare item never compiled, so every carrier was
            // already waivered → green-safe by construction.
            return MetaDerefHoistRoute.STATEMENT_SINK;
        }
        // (a) extract/map lambda (PR #109 ref_coercion, PR #128 bigint + the facet
        //     evaluate_arg_consumption nested split — see the javadoc).
        if (inline.parent() instanceof RExtractExpr) {
            return inline.body() == expr
                    ? MetaDerefHoistRoute.BLOCK
                    : MetaDerefHoistRoute.LAMBDA_CHANNEL;
        }
        // (b) a then-body function call in a reporting RULE.
        //   - DIRECT (inline.body() == expr, PR #129 bigint top-level then-extract): the call is
        //     compiled at statement level by FunctionExpressionRenderer.renderThenExtractSet, which
        //     LIFTS the JavaBlockBuilder hoist to the line before the `output =` assignment → BLOCK.
        //   - NESTED inside the then-body output expression (inline.body() != expr, facet
        //     ruleMetaDerefStatementSink PR #267): a meta-deref-arg call NESTED in the output (e.g.
        //     NatureOfTheCounterparty's `extractRegimeInformation.evaluate(thenArg.get(),
        //     <metaChain>)` inside the outer `extractNatureOfCounterparty.evaluate(...)`) renders
        //     EAGERLY into the output expression (no enclosing lambda — nearestEnclosingInlineFunction
        //     resolves to the THEN's own body function, whose body is the OUTER call), so its
        //     `final <Wrapper> <name> = <chain>.get();` decl lifts to the STATEMENT sink that
        //     renderThenExtractSet's scope now marks (#267) → STATEMENT_SINK. Pre-#267 this was NONE
        //     (no drain site). Both rule-scoped via findEnclosingRule, so a FUNCTION then-body is
        //     unaffected (→ NONE below, byte-neutral); the STATEMENT_SINK call-site verifies a sink
        //     is reachable + tryMetaDerefArg's own meta/param-mismatch gate keeps non-meta nested
        //     calls a no-op. A call nested in a `.mapSingleToItem(item -> …)` lambda WITHIN the
        //     then-body resolves a CLOSER inline function (the RExtractExpr arm above), not this one.
        if (inline.parent() instanceof RThenExpr) {
            if (inline.body() == expr) {
                // DIRECT RULE keeps the #129 BLOCK lift seat.
                // facet fnBareInvokableArgMetaDeref (PR #399, R7): the FUNCTION direct
                // then-body call routes the LAMBDA channel instead of NONE — a
                // #339-relayed bare-invokable consumer's implicit META arg derefs
                // in-lambda (`final FieldWithMetaString _fieldWithMetaString1 =
                // thenArg.get();` + the guarded getValue — golden AnnaDsb-FRE's
                // DIGITAL settlementCurrency arm). The consumption-site seat booleans
                // (blockArmSeatConditional / filter-predicate / ite-handshake) +
                // tryMetaDerefArg's meta/param gates keep every other shape flat
                // byte-identically, and the bare wrapper into a bare-item callee
                // never compiled (green-safe by construction).
                return HandlerHelper.findEnclosingRule(expr) != null
                        ? MetaDerefHoistRoute.BLOCK
                        : MetaDerefHoistRoute.LAMBDA_CHANNEL;
            }
            // facet fnAnnaDsbTogetherRestructure (PR #399, R2/R6): the NESTED arm joins the
            // FUNCTION path — a meta-deref-arg call nested in a FUNCTION then-body output
            // (the AnnaDsb-FRE `ConvertCurrency(<meta chain>)` ITE-ladder arms) lifts its
            // `final FieldWithMetaString <name> = <chain>.get();` decl to the SAME
            // statement sink renderThenExtractSet's scope marks (#267) — the sink-reachable
            // call-site check + tryMetaDerefArg's meta/param gates keep every other shape a
            // no-op, and the bare wrapper into the bare-item callee never compiled
            // (green-safe by construction).
            return MetaDerefHoistRoute.STATEMENT_SINK;
        }
        // facet inLambdaFilterArgDeref (PR #363): a call NESTED inside a FILTER
        // predicate (`then filter <Rule> = True` — the rule ref is an operand of the
        // predicate comparison, inline.body() != expr) routes the meta-deref decl
        // through the LAMBDA channel: CollectionHandler.compileLambda drains it into
        // the block form (golden jfsa OriginalSwapUTI/OriginalSwapUTIProprietary:
        // `.filterItemNullSafe(item -> { final FieldWithMetaString
        // _fieldWithMetaString = item.get(); return areEqual(MapperS.of(
        // isMax32UpperCaseAlphanumericText.evaluate((_fieldWithMetaString == null ?
        // null : _fieldWithMetaString.getValue()))), …).get(); })` — the `_` escape
        // against the method-level #362 output-deref hoist falls out of the unified
        // naming replay). A DIRECT filter body (inline.body() == expr — the bare
        // `filter <Fn>` boolean form) takes the SAME channel since seat 28 (law 6,
        // facet filterPredicateMetaDeref). The pre-seat claim that "golden passes the
        // piped item raw there" is REFUTED for a META WRAPPER item: drr 5.61.0 asic
        // SpreadCurrencyLeg2Rule, esma/fca SpreadCurrencyOfLeg2Rule and mas
        // FixedFloatRateLeg1/2Rule + InterestRatePriceRule all render
        // `.filterSingleNullSafe(item -> { final ReferenceWithMetaPriceSchedule
        // referenceWithMetaPriceSchedule = item.get(); return
        // isPriceMonetary.evaluate((referenceWithMetaPriceSchedule == null ? null :
        // referenceWithMetaPriceSchedule.getValue())); })`, and the EXPLICIT-args
        // sibling (drr 7.x ExtractPartyByNameContains) hoists a NAV CHAIN
        // (`item.<FieldWithMetaString>map("getName", ...).get()`) the same way at
        // argument index 0 of a two-arg call. The claim IS still true for a non-meta
        // item, and stays true by machinery, not by this route: tryMetaDerefArg's
        // param-type-equality, meta-param (#347) and coercion-service probes each
        // decline, so every non-meta predicate call is byte-identical.
        if (inline.parent() instanceof com.regnosys.rosetta.ast.expressions.unary.RFilterExpr) {
            return MetaDerefHoistRoute.LAMBDA_CHANNEL;
        }
        return MetaDerefHoistRoute.NONE;
    }

    /**
     * Resolve a BARE enum value used as a function-call argument to its qualified
     * Java constant ({@code enum_const_arg}, PR #137).
     *
     * <p>A bare enum value ({@code Nearest}, NOT the qualified
     * {@code RoundingDirectionEnum -> Nearest}) in argument position parses as an
     * UNRESOLVED {@link RSymbolReference} — the {@code TypeInferenceEngine}
     * type-directs a bare enum value to its {@link
     * com.regnosys.rosetta.ast.supporting.REnumValue} only in comparison-operand /
     * conditional-branch context (where a sibling seeds the expected enum type),
     * NOT in a function-call argument — so the variable path renders the bare Rune
     * value name, a non-compiling undefined symbol. The callee's DECLARED
     * parameter type pins the enum: when the parameter at {@code argIndex} resolves
     * to an {@link com.regnosys.rosetta.ast.types.REnumeration} whose values
     * include the bare name, emit {@code EnumName.CONSTANT}
     * ({@link com.regnosys.rosetta.generator.java.enums.EnumHelper#convertValue}
     * maps source {@code Nearest} -> Java {@code NEAREST}) plus the enum import
     * (the same {@code enumImportRefs} the qualified {@code handle(REnumValueRef)}
     * path uses — the rule otherwise never names the enum type, so no ambient
     * import exists). The qualified sibling already renders correctly via
     * {@code handle(REnumValueRef)}.
     *
     * <p>PR #143 ({@code enum_arg_qualify}) extends the accepted shape to a bare ref
     * whose resolved symbol is an {@link com.regnosys.rosetta.ast.regulatory.RBody}:
     * a regime acronym ({@code ASIC}/{@code HKMA}/{@code JFSA}/{@code MAS}) NAME-COLLIDES
     * with a {@code body Authority <NAME>} regulatory declaration, so the linker
     * false-resolves the bare ref to that RBody rather than leaving it unresolved.
     * The RBody has no Java representation, so the variable path still emits the
     * non-compiling bare value name. Both shapes (unresolved + RBody-collision) are
     * pinned the same way against the callee's declared enum parameter.
     *
     * <p>v3.1 flip seat 3 (facet {@code cat16BindEnumSeats} rider — the fn-ARG
     * seat joins PR #452's three): the Cat-16 BIND-STAMPED shape (an
     * {@link com.regnosys.rosetta.ast.supporting.REnumValue} symbol — the
     * expected-type bind resolves a bare value through the param enum's
     * {@code extends} chain and stamps the instance declared possibly on a
     * SUPER-enum) is admitted through {@code HandlerHelper.boundBareEnumValue}
     * as the third rung and re-verified against the callee param's own
     * hierarchy under the #215 SAME-INSTANCE descend-only law
     * ({@code findEnumValueInHierarchy(en, name) == bound}), so the render
     * qualifies by the EXPECTED (child) enum — the #211/#358 flatten law.
     * Measured (the LAW-65 content dump, drr 7.0.0): {@code
     * FilterAssetIdentifier(item, ISIN)} rendered {@code ProductIdTypeEnum.ISIN}
     * (the stamp's declaring parent) where golden has
     * {@code AssetIdTypeEnum.ISIN}. Unlike the non-compiling shapes below, the
     * pre-seat rendering COMPILES (Java flattening declares the constant on
     * both) — the swap is byte-moving toward golden and ring-gated; an
     * unrelated same-name enum's stamp fails the same-instance check and keeps
     * the resolved rendering.
     *
     * <p>Returns {@code null} (leaving the caller's rendering intact) when the arg
     * is not a bare ref, has explicit args, resolves to any symbol OTHER than an
     * RBody, the parameter type is not a resolvable enum, or no value matches.
     * Regression-safe by construction: the pre-fix rendering is the bare value name,
     * which does not compile, so no currently-green file carries it — only
     * already-waivered files are affected.
     */
    private JavaExpression tryBareEnumArg(RExpression arg, RFunction calleeFn, int argIndex,
            ExpressionCompiler compiler) {
        if (calleeFn == null || argIndex >= calleeFn.inputs().size()) {
            return null;
        }
        // Accept a bare ref that is UNRESOLVED (PR #137 — type-directed resolution
        // does not fire for an enum value in argument position) OR one whose resolved
        // symbol is an RBody (PR #143 enum_arg_qualify — a regime acronym like `ASIC`
        // name-collides with a `body Authority ASIC` regulatory declaration, so the
        // linker false-resolves the bare ref to the RBody; the RBody has no Java
        // representation, so the variable path emits a non-compiling bare value name).
        // Any OTHER resolved symbol (an actual variable / feature / function) keeps the
        // existing decline — only the unresolved + RBody-collision shapes are rewritten,
        // and only when the value-name match below succeeds against the callee's
        // declared enum parameter (regression-safe: the pre-fix bare name does not
        // compile, so no green file carries it).
        // facet caseNarrowedDisguisedNav rider (PR #368): an RDataType-bound bare ref
        // joins the RBody collision class — a bare enum value whose name collides
        // with a MODEL TYPE (`ListedDerivative` — both an InstrumentTypeEnum value
        // and a cdm type) false-resolves to the type; the type name as a VALUE arg
        // is non-compiling (`MapperS.of(ListedDerivative)` — a class literal without
        // .class), so no green file carries it. The value-name match below against
        // the callee's declared enum param stays the real gate (golden MapAsset
        // `InstrumentTypeEnum.LISTED_DERIVATIVE`).
        // facet corpusCollisionEnumArg (PR #376, M4): an RCorpus-bound bare ref joins
        // the RBody/RDataType collision class — a bare enum value whose name collides
        // with a regulatory `corpus` declaration (`EMIR` — both a RegimeNameEnum value
        // and a corpus name) false-resolves to the corpus, which has no Java value
        // representation, so the variable path emits the non-compiling bare name
        // (golden EMIR_ISIN/UKEMIR_ISIN `RegimeNameEnum.EMIR`). The value-name match
        // below against the callee's declared enum param stays the real gate.
        if (!(arg instanceof RSymbolReference bareRef)
                || !bareRef.args().isEmpty()
                || (bareRef.symbol().isPresent()
                        && !(bareRef.symbol().get() instanceof RBody)
                        && !(bareRef.symbol().get() instanceof RDataType)
                        && !(bareRef.symbol().get()
                                instanceof com.regnosys.rosetta.ast.regulatory.RCorpus)
                        // v3.1 flip seat 3: the Cat-16 bind-stamped shape joins
                        // (the third rung below re-verifies same-instance).
                        && !(bareRef.symbol().get()
                                instanceof com.regnosys.rosetta.ast.supporting.REnumValue))) {
            return null;
        }
        String valueName = bareRef.name();
        if (valueName == null || valueName.isEmpty()) {
            return null;
        }
        RTypeCall paramTypeCall = calleeFn.inputs().get(argIndex).typeCall();
        if (paramTypeCall == null) {
            return null;
        }
        Optional<RNode> referenced = paramTypeCall.referencedType();
        if (referenced.isEmpty()
                || !(referenced.get() instanceof com.regnosys.rosetta.ast.types.REnumeration en)) {
            return null;
        }
        com.regnosys.rosetta.ast.supporting.REnumValue bound =
                HandlerHelper.boundBareEnumValue(arg);
        if (bound != null) {
            // v3.1 flip seat 3 — the #452 third rung at the fn-arg seat: the
            // Cat-16 stamp is re-verified against the callee param's OWN
            // hierarchy (the #215 SAME-INSTANCE descend-only law) and the
            // render qualifies by the EXPECTED child enum (the #211/#358
            // flatten law — the generated child enum flattens inherited
            // values, so the constant exists on the child).
            if (HandlerHelper.findEnumValueInHierarchy(en, valueName) == bound) {
                String constant = en.name() + "."
                        + com.regnosys.rosetta.generator.java.enums.EnumHelper.convertValue(bound);
                return JavaExpression.enumConstant(constant, null, enumImportRefs(en, compiler),
                        Set.of());
            }
            return null; // an unrelated same-name enum's stamp — keep the resolved rendering
        }
        for (com.regnosys.rosetta.ast.supporting.REnumValue val : en.values()) {
            if (valueName.equals(val.name())) {
                String constant = en.name() + "."
                        + com.regnosys.rosetta.generator.java.enums.EnumHelper.convertValue(val);
                return JavaExpression.enumConstant(constant, null, enumImportRefs(en, compiler),
                        Set.of());
            }
        }
        return null;
    }

    /**
     * If argument {@code argIndex}'s item type differs from the callee's (scalar)
     * parameter type in one of two corpus-exercised ways — (a) a
     * {@code ReferenceWithMeta<T>}/{@code FieldWithMeta<T>}
     * ({@link RJavaWithMetaValue}) arg where the parameter expects the plain value
     * type {@code T} (PR #109 ref_coercion), or (b) a {@code BigInteger} arg (a
     * {@code number} literal exceeding {@code long}) where the parameter expects
     * {@code BigDecimal} (PR #128 bigint_bigdecimal_coerce) — hoist the unwrapped
     * value into a {@code final <ItemType> <var> = <unwrapped>;} local (added to
     * {@code hoists}) and return the null-guarded conversion expression
     * {@code (<var> == null ? null : <conv>)}; the conversion {@code <conv>}
     * ({@code <var>.getValue()} for the meta case, {@code new BigDecimal(<var>)} for
     * the bigint case) is produced by the
     * {@link com.regnosys.rosetta.generator.java.expression.TypeCoercionService},
     * never hardcoded. Returns {@code null} (no hoist) when the wiring is absent,
     * the parameter is multi-cardinality, the arg item type is neither a meta
     * wrapper nor a BigInteger-into-BigDecimal mismatch, the parameter type cannot
     * be resolved, or the service yields no real conversion (identity) — leaving the
     * caller's flat arg rendering intact.
     */
    private JavaExpression tryMetaDerefArg(
            RExpression arg, JavaStatementBuilder compiled, JavaExpression unwrappedExpr,
            RFunction callee, int argIndex,
            ExpressionContext ctx, ExpressionCompiler compiler,
            List<JavaStatement> hoists, JavaStatementScope statementSink) {
        return tryMetaDerefArg(arg, compiled, unwrappedExpr, callee, argIndex, ctx, compiler,
                hoists, statementSink, false);
    }

    /**
     * T3 (PR-24): whether this evaluate-arg is a SINGLE-seamed flipped alias's
     * bridge — the single→list lift guard's read at a MULTI param (the ADD-seat
     * guard's exact class at the arg seat). THE ALIAS-SEAM CHANNEL IS THE ONLY
     * READ ({@link NavigationHandler#aliasSeamOrNull} — the § 3
     * S/C authority, which unlike the typed #347-F5 channel answers for
     * model-item aliases too, the #352 boundary): a reference-route alias
     * reference never wraps, so wrap-present ∧ walk-resolves is satisfiable by
     * the optimised bridge exclusively — reference bytes inert BY CODE.
     *
     * <p>facet aliasSeamSignature (PR #612): the read is the seam's own recorded FORM,
     * not the rendered seam string's prefix (LAW 69 — the producer's decision).
     */
    private static boolean isSingleSeamBridgeArg(RExpression arg, ExpressionCompiler compiler) {
        FunctionTemplateModel.AliasSeam seam = NavigationHandler.aliasSeamOrNull(arg, compiler);
        return seam != null && seam.isMapperSingle();
    }

    /**
     * T2 (PR-23): whether the #347-F5 wrapper→value {@code "Type coercion"}
     * arm WILL apply to this multi evaluate-arg — the strip guard's twin of
     * that arm's own gates: a meta-FREE callee param whose element resolves,
     * an ALIAS-reference arg whose Mapper ELEMENT (the #347-F5 alias-walk
     * channel — the semantic {@code inferShortcutMapperJavaType} walk,
     * unchanged by the value-seam flip) is a meta WRAPPER, and the wrapper's
     * VALUE model-extends the param element. A structurally-wrapped value-seam
     * bridge satisfying this keeps its Mapper form so the arm can rewrite it;
     * the strip would pass {@code List<Wrapper>} where
     * {@code List<? extends Value>} is expected (non-compiling).
     *
     * <p>THE ALIAS WALK IS THE ONLY ELEMENT READ (never the compiled type):
     * a reference-route alias reference compiles to the BARE Mapper-returning
     * call (never a structural wrap), so wrap-present ∧ alias-walk-resolves is
     * satisfiable ONLY by the optimised route's bridge — reference bytes are
     * inert BY CODE (the #433 fnIoMetaListWrap stamped wraps carry a compiled
     * TYPE but are never alias references; reading the type channel here
     * would open a reference-visible path this deliberately refuses).
     */
    private static boolean multiWrapperArgCoercionApplies(RExpression arg, RFunction calleeFn,
            int argIndex, ExpressionCompiler compiler) {
        if (calleeFn == null || argIndex >= calleeFn.inputs().size()
                || compiler.getGeneratorModel() == null
                || compiler.getTypeTranslator() == null
                || compiler.getTypeUtil() == null
                || MetaFieldGenerator.detectMetaKind(calleeFn.inputs().get(argIndex))
                        != MetaFieldGenerator.MetaKind.NONE) {
            return false;
        }
        JavaType argType = NavigationHandler.tryAliasReceiverMapperType(arg, compiler);
        JavaType argItem = argType == null ? null : compiler.getTypeUtil().getItemType(argType);
        if (!(argItem instanceof RJavaWithMetaValue elemWrapper)
                || !(elemWrapper.getValueType() instanceof JavaClass<?> elemValueClass)) {
            return false;
        }
        RType paramRType = compiler.getGeneratorModel().getType(calleeFn.inputs().get(argIndex));
        JavaClass<?> paramElem = paramRType == null ? null
                : compiler.getTypeTranslator().toJavaReferenceType(paramRType);
        return paramElem != null
                && valueModelExtendsParamElem(elemValueClass, paramElem, compiler);
    }

    /**
     * facet multiArgElementwiseWrapperDeref (PR #349, S2): is the wrapper VALUE class
     * assignable to the callee param's ELEMENT class at the MODEL level? Generated
     * model classes are FLAT ({@code RGeneratedJavaClass} extends only
     * {@code RosettaModelObject} — the #323 pojoOverrideNaming law), so
     * {@code JavaType.isSubtypeOf} cannot see model ancestry; this walks the Rosetta
     * {@code RDataType.superType()} chain instead, resolving the value's model type BY
     * NAME through a fresh detached {@link RTypeCall} (the #347-F8 deep-copy law: a
     * name-only typeCall resolves via the gm workspace fallback).
     */
    private static boolean valueModelExtendsParamElem(JavaClass<?> valueClass,
            JavaClass<?> paramElem, ExpressionCompiler compiler) {
        if (valueClass.getCanonicalName().withDots()
                .equals(paramElem.getCanonicalName().withDots())) {
            return true;
        }
        if (compiler.getGeneratorModel() == null) {
            return false;
        }
        RTypeCall byName = new RTypeCall();
        byName.setTypeName(valueClass.getSimpleName());
        RType resolved = compiler.getGeneratorModel().resolveTypeCall(byName);
        RDataType dt = resolved instanceof com.regnosys.rosetta.types.RDataTypeRef ref
                ? ref.astNode()
                : null;
        java.util.Set<RDataType> seen = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<>());
        while (dt != null && seen.add(dt)) {
            if (paramElem.getSimpleName().equals(dt.name())) {
                return true;
            }
            dt = dt.superType().orElse(null);
        }
        return false;
    }

    /**
     * v3.2 seat 13 (D53, site R7): the DECISION of {@link #tryMetaDerefArg} - the argument's ITEM type against the
     * callee parameter's Java type, through every recovery channel and the coercion service's probe - as a value,
     * so the commit half ({@code tryMetaDerefArg}) and the refusal at a seat with no hoist consumer
     * ({@link #refuseIfArgCoercionDropped}) read ONE predicate (LAW 69). {@code inlineLiteral} is the #300 M2 arm
     * (a within-long int literal into a BigDecimal parameter renders the inline {@code BigDecimal.valueOf(<lit>)},
     * no hoist); every other plan hoists the value and coerces the null-guarded local.
     */
    record ArgCoercionPlan(JavaType actualItemType, JavaClass<?> paramJavaType, boolean inlineLiteral) {}

    /**
     * v3.2 seat 13 (D53, site R7): at a seat with NO hoist consumer, a planned coercion is REFUSED
     * ({@link SilentDegradation.Site#CALL_ARGUMENT_COERCION_DROPPED}) instead of the flat splice the pre-seat code
     * emitted - the flat form is an Integer into a BigDecimal parameter or a wrapper into a bare-value parameter,
     * which javac refuses by construction. Consults {@link #planArgCoercion} - the same plan the commit reads.
     */
    private void refuseIfArgCoercionDropped(
            RExpression arg, JavaStatementBuilder compiled, JavaExpression unwrappedExpr,
            RFunction callee, int argIndex, ExpressionContext ctx, ExpressionCompiler compiler,
            RSymbolReference call, String position) {
        // functionItemLambdaRecovery is FALSE at both refusal seats by construction (#634 round 1, cq NIT-6): the
        // fn-item lambda arm needs hoistRoute == BLOCK, and the two positions that refuse are the sink-less
        // STATEMENT_SINK seat and route NONE; a third caller on a BLOCK-route position passes the arm's own answer.
        ArgCoercionPlan plan = planArgCoercion(arg, compiled, unwrappedExpr, callee, argIndex, ctx, compiler,
                /* functionItemLambdaRecovery */ false);
        if (plan == null) {
            return;
        }
        throw SilentDegradation.refuse(SilentDegradation.Site.CALL_ARGUMENT_COERCION_DROPPED,
                "argument " + argIndex + " of `" + callee.name() + "(...)` needs the " + plan.actualItemType()
                        .getSimpleName() + " -> " + plan.paramJavaType().getSimpleName()
                        + " coercion its parameter '" + callee.inputs().get(argIndex).name()
                        + "' declares, and " + position + " - the pre-seat render spliced the argument flat: `"
                        + unwrappedExpr.renderToString() + "`",
                call);
    }

    private JavaExpression tryMetaDerefArg(
            RExpression arg, JavaStatementBuilder compiled, JavaExpression unwrappedExpr,
            RFunction callee, int argIndex,
            ExpressionContext ctx, ExpressionCompiler compiler,
            List<JavaStatement> hoists, JavaStatementScope statementSink,
            boolean functionItemLambdaRecovery) {
        ArgCoercionPlan plan = planArgCoercion(arg, compiled, unwrappedExpr, callee, argIndex, ctx, compiler,
                functionItemLambdaRecovery);
        if (plan == null) {
            return null;
        }
        JavaType actualItemType = plan.actualItemType();
        JavaClass<?> paramJavaType = plan.paramJavaType();
        if (plan.inlineLiteral()) {
            Set<JavaClass<?>> litRefs = new HashSet<>(unwrappedExpr.getRefs());
            litRefs.add(compiler.getTypeUtil().BIG_DECIMAL);
            return JavaExpression.from(
                    "BigDecimal.valueOf(" + unwrappedExpr.renderToString() + ")",
                    paramJavaType, litRefs, unwrappedExpr.getStaticWildcardImports());
        }
        String baseVar = JavaNamingUtil.toFirstLower(actualItemType.getSimpleName());
        return commitArgCoercion(actualItemType, paramJavaType, baseVar, unwrappedExpr, ctx, compiler, hoists,
                statementSink, functionItemLambdaRecovery, isBareLocalArgument(arg));
    }

    /**
     * v3.2 seat 13 (D53, the R7 heal - the oracle group {@code arg-coercion-bare-local}, pinned from the released
     * plugin BEFORE this arm): true when the evaluate-argument IS a local of the generated method, read bare - a
     * symbol bound to an INPUT attribute of the enclosing function (its dispatch base's inputs for a variant: the
     * seat-28 identity test, {@code RFunction.dispatchBase()} the accessor the scope builder bound it through) or the
     * implicit {@code item} of a {@code typeAlias} condition (the datarule method's own parameter). Upstream's
     * {@code convertNullSafe} coerces such a value INLINE on its name and hoists nothing
     * ({@code check.evaluate((n == null ? null : BigDecimal.valueOf(n)))}); a navigated value hoists first (the #277
     * class, the group's {@code CallWithIntNav} control). A closure parameter is NOT admitted - no golden pins it
     * (banked).
     */
    private static boolean isBareLocalArgument(RExpression arg) {
        if (arg instanceof RImplicitVariable) {
            return HandlerHelper.aliasOwner(HandlerHelper.findEnclosingTypeCondition(arg)) != null;
        }
        if (arg instanceof RSymbolReference sr && sr.symbol().orElse(null) instanceof RAttribute attr) {
            RFunction owner = HandlerHelper.findEnclosingFunction(arg);
            List<RAttribute> inputs = owner == null
                    ? List.of()
                    : owner.dispatchBase().map(RFunction::inputs).orElseGet(owner::inputs);
            return inputs.stream().anyMatch(in -> in == attr);
        }
        return false;
    }

    /**
     * v3.2 seat 13 (D53, site R7): the DECISION half of the pre-seat {@code tryMetaDerefArg}, verbatim - every
     * decline a {@code null} plan; the #300 M2 literal arm an {@code inlineLiteral} plan; the coercion service's
     * probe the last gate. Pure refactor: the commit half reads the plan's two types exactly as its locals read them.
     */
    private ArgCoercionPlan planArgCoercion(
            RExpression arg, JavaStatementBuilder compiled, JavaExpression unwrappedExpr,
            RFunction callee, int argIndex,
            ExpressionContext ctx, ExpressionCompiler compiler,
            boolean functionItemLambdaRecovery) {
        if (compiler.getGeneratorModel() == null || compiler.getTypeTranslator() == null
                || compiler.getTypeUtil() == null || compiler.getCoercionService() == null) {
            return null;
        }
        if (argIndex >= callee.inputs().size()) {
            return null;
        }
        RAttribute param = callee.inputs().get(argIndex);
        if (compiler.getGeneratorModel().isMulti(param)) {
            return null;
        }
        // The argument's meta-ness comes from the COMPILED expression type — the
        // navigation renderer surfaces a concrete RJavaWithMetaValue
        // (MapperS/MapperC<ReferenceWithMetaX>) for a `[metadata reference|address]`
        // feature (NavigationHandler.metaNavResultType / PR-B). The AST inferred type
        // drops meta for the REnumValueRef `a -> b` nav shape, so the compiled type is
        // the reliable signal here.
        JavaType compiledType = compiled.getExpressionType();
        // facet evaluate_arg_consumption (arm B3, typing): an ALIAS-call arg
        // (`QuantityDecreased(beforeTradeState, ...)`) renders with a null
        // expression type, so the meta gate below declined and the raw wrapper
        // was passed where the callee expects the value type (non-compiling).
        // Recover the item type from the SAME FunctionAliasHelper walk that
        // renders the alias method signature — PR #170 arm A1's typing channel
        // (NavigationHandler.tryAliasReceiverMapperType), so the signature, the
        // receiver coercion, and this arg coercion cannot disagree.
        if (compiledType == null) {
            compiledType = NavigationHandler.tryAliasReceiverMapperType(arg, compiler);
        }
        // facet evalArgMetaDerefHoist (PR #237): a multi-intermediate nav arg
        // (`businessEvent -> instruction -> before`, the `before` reached through a
        // multi `instruction` mapC and collapsed by an ONLY_ELEMENT / extract-map list-op)
        // compiles with a NULL expression type — the list-op wrapper does not surface the
        // terminal feature's meta wrapper — so the meta gate below would decline and the
        // raw wrapper would be spliced bare where the callee expects the value type
        // (non-compiling). Recover the terminal feature's `MapperS<FieldWithMetaX>` type
        // from the AST (the same metaNavResultType the witness derives from), so the
        // hoist + deref fires for these list-op-wrapped meta args too
        // (Qualify_PartialTermination / EquityPerformance). Non-meta terminals resolve to
        // null and keep the flat form.
        if (compiledType == null) {
            compiledType = NavigationHandler.tryTerminalMetaMapperType(arg, compiler);
        }
        // facet metaCoercionRule LAMBDA-channel (PR #285): the arg is the IMPLICIT ITEM
        // (`item.get()`) of a rule-body `.mapSingleToItem(item -> MapperS.of(<fn>.evaluate(
        // item.get(), …)))` lambda — its compiled type erases to null and the alias/terminal
        // fallbacks above decline (it is neither an alias call nor a navigable terminal). Recover
        // the wrapper from the owning then-chain's terminal nav (the OtherPaymentPayer/Receiver
        // #267-deferred LAMBDA_CHANNEL family) so the meta-deref hoist fires + the lambda converts
        // to a block (registerPendingLambdaHoist → compileLambda). RULE-SCOPED AT THIS SEAT (the
        // #285 scope, kept verbatim at PR #340: un-gating it here re-rendered FUNCTION-path
        // explicit-arg carriers AWAY — Qualify_AssetClass_Equity dl 5 → 36, the cp4 over-fire
        // catch — while the recovery HELPER went path-agnostic for the #340
        // renderImplicitFunctionInvocation fallback, whose BLOCK-route arm consumes it safely);
        // the param-mismatch + coercion-service gates below keep it a no-op for a non-meta item.
        if (compiledType == null && HandlerHelper.findEnclosingRule(arg) != null) {
            compiledType = NavigationHandler.implicitItemArgMeta(arg, compiler);
        }
        // facet itemGetMetaDerefBlock (PR #346): the FUNCTION-path sibling of the #285
        // rule-scoped recovery above — the caller armed it ONLY for an explicit
        // RImplicitVariable argument of a call that IS a drainable map/extract lambda
        // body, with an OPEN scope, outside any rule (mutually exclusive with the arm
        // above). The consumption site routes the resulting hoist through the LAMBDA
        // channel (per-pass registration + compileLambda drain), NOT the #340-degrading
        // JavaBlockBuilder — see lambdaChannelHoists at the explicit-args loop.
        if (compiledType == null && functionItemLambdaRecovery) {
            compiledType = NavigationHandler.implicitItemArgMeta(arg, compiler);
            // v3.1 flip seat 30, law 1 RUNG 2 - facet aliasSigElementMetaKeep, the BODY
            // half. The #346 walk above recovers the item's wrapper from the owning
            // chain's TERMINAL NAV (recoverMetaFromExpr), which is meta-BLIND for an
            // ALIAS-rooted chain - the same blindness rung 1 displaced at the then-arg
            // DECL seat, one seat over. So a then-arg piped from a META alias declared the
            // wrapper (rung 1) while its lambda item still passed `item.get()` BARE into a
            // bare-value callee param: `evaluate(FieldWithMetaPriceSchedule, ...)` where
            // `evaluate(PriceSchedule, ...)` is declared - a javac error, not cosmetic
            // residue (golden drr ReportablePricePeriod hoists
            // `final FieldWithMetaPriceSchedule fieldWithMetaPriceSchedule = item.get();`
            // at lambda top and passes the null-guarded `.getValue()`).
            //
            // Read the BINDING instead: HandlerHelper.bareItemThenPipeMetaType is the #362
            // render-truth channel for precisely this question (the scope's thenArgRefFor
            // compiled type, never an AST walk - the #361 cp6 law), and it already has SIX
            // consumers (ArithmeticHandler x2, CollectionHandler, ComparisonHandler,
            // ConstructionHandler, NavigationHandler's #375 A4-c re-stamp). This seat was
            // the one consumer missing. LAW 69: one walk, now seven consumers.
            //
            // MapperS-wrapped to match the sibling it falls back from (implicitItemArgMeta
            // returns MapperS<wrapper>); only getItemType is read downstream, so the kind
            // is presentational here.
            //
            // Strictly a fallback behind three declines, and every consumption gate still
            // applies: functionItemLambdaRecovery is itself armed only for an explicit
            // RImplicitVariable arg of a BLOCK-route drainable-lambda body, outside any
            // rule, with an OPEN scope and a meta-FREE callee param; the helper declines
            // unless the enclosing extract/filter lambda pipes an implicit item off a
            // then-step whose binding element IS a wrapper; and the coercion-service probe
            // below must produce a REAL conversion. A bare-element binding recovers a
            // non-wrapper item and this whole arm is byte-inert - which is also why rung 2
            // is INERT WITHOUT RUNG 1 (pre-rung-1 the carrier's binding element was the
            // bare PriceSchedule). The two rungs are conjunctive halves, not one read.
            if (compiledType == null && compiler.getTypeUtil() != null) {
                RJavaWithMetaValue pipedItemMeta =
                        HandlerHelper.bareItemThenPipeMetaType(arg, ctx.scope(), compiler);
                if (pipedItemMeta != null) {
                    compiledType = compiler.getTypeUtil()
                            .wrap(compiler.getTypeUtil().MAPPER_S, pipedItemMeta);
                }
            }
        }
        JavaType actualItemType = compiledType == null
                ? null
                : compiler.getTypeUtil().getItemType(compiledType);
        // A `number` literal exceeding `long` (e.g. 9999999999999999999999999) compiles
        // to a `new BigInteger("…")` expression that carries NO Java type (LiteralHandler
        // leaves the inner expression type null), so the compiled type is absent. Recover
        // the BigInteger item type from the AST literal's magnitude (the same long-range
        // overflow threshold LiteralHandler.handle(RIntLiteral) applies) so the
        // BigInteger→BigDecimal coercion (b) below can fire. The meta case (a) always
        // carries a concrete compiled type.
        if (actualItemType == null && arg instanceof RIntLiteral intLit) {
            // A beyond-long `number` literal compiles to `new BigInteger("…")` (no Java type);
            // a within-long int literal (facet numericCoercionCompose M2, PR #300) carries
            // Integer — recover both so the BigInteger→BigDecimal (b) / literal Integer→BigDecimal
            // (M2) arms below can fire on a literal whose compiled type is absent.
            actualItemType = intLit.value().bitLength() > 63
                    ? compiler.getTypeUtil().BIG_INTEGER
                    : compiler.getTypeUtil().INTEGER;
        }
        // facet numericCoercionCompose (PR #300, M1): a NULLABLE Integer (or BigInteger) nav-chain
        // arg compiled INLINE (`…<Integer>map("getPeriodMultiplier", …).get()` — the esma
        // FloatingRateResetFrequency*Of* carriers navigate periodMultiplier inline as a 2nd
        // `adjustPeriodMultiplier.evaluate(…, <chain>)` arg, NOT via a declared `MapperS<Integer>
        // thenArg` local whose `.get()` surfaces the type) leaves a NULL expression type, so the
        // integerToBigDecimal (c) arm below could not fire — #277 only reached carriers whose Integer
        // came through a declared-local `.get()`. Recover the bare numeric item type from the AST
        // inferred type (the SAME type the chain's `<Integer>map` witness derives from). NUMERIC-only
        // (Integer / BigInteger): those are the sole arms that consume it, and a still-null
        // actualItemType after every meta fallback means a non-meta arg, so this never disturbs the
        // meta-deref path; the param-match + coercion-service gates below keep it a no-op when no real
        // conversion applies. RULE-scoping is NOT needed — the route gate (BLOCK only fires for a
        // genuine hoistable position) + the regscan confirm FUNCTION-byte-neutrality.
        if (actualItemType == null && !(arg instanceof RIntLiteral)
                && !numCoerceArgInConditionalLambdaBody(arg)) {
            var argInferred = compiler.getGeneratorModel().workspace().getInferredType(arg);
            if (argInferred != null && argInferred.type() != null) {
                JavaClass<?> candidate =
                        compiler.getTypeTranslator().toJavaReferenceType(argInferred.type());
                if (compiler.getTypeUtil().isInteger(candidate)
                        || compiler.getTypeUtil().isBigInteger(candidate)) {
                    actualItemType = candidate;
                }
            }
        }
        // v3.2 seat 13 (D53, site R7 - the c9 control's catch): a typeAlias CONDITION's implicit `item` (the chaos
        // M5f rows: `condition Called: C27Check(item)` over `number(fractionalDigits: 0)`) is left MISSING by the
        // engine, so every channel above declines and the plan was null - the SAME blindness that let the flat
        // `c27Check.evaluate(c27Nat)` out silently. The alias's resolved base type is the ONE read
        // (HandlerHelper.aliasConditionItemJavaType - numericOperandKind's literal-widening arm reads it too,
        // LAW 69). NUMERIC-only like the #300 M1 arm: Integer / BigInteger are the sole kinds the arms below
        // consume, and a Data owner's implicit resolves to no alias at all.
        if (actualItemType == null && arg instanceof RImplicitVariable) {
            JavaType aliasItem = HandlerHelper.aliasConditionItemJavaType(arg, compiler);
            if (aliasItem != null && (compiler.getTypeUtil().isInteger(aliasItem)
                    || compiler.getTypeUtil().isBigInteger(aliasItem))) {
                actualItemType = aliasItem;
            }
        }
        if (actualItemType == null) {
            return null;
        }
        RType paramRType = compiler.getGeneratorModel().getType(param);
        if (paramRType == null || paramRType instanceof RMissingType) {
            return null;
        }
        JavaClass<?> paramJavaType = compiler.getTypeTranslator().toJavaReferenceType(paramRType);
        // Parameter already expects the arg's own item type (or is unresolved) → no conversion.
        if (paramJavaType == null || paramJavaType.equals(actualItemType)) {
            return null;
        }
        // facet numericCoercionCompose (PR #300, M2): a LITERAL within-long Integer arg into a
        // BigDecimal param (IndexFactor's `formatToBaseOne18Rate.evaluate(1)`) — golden renders the
        // INLINE `BigDecimal.valueOf(<lit>)` (a literal is provably non-null, so NO hoist / NO
        // null-guard, unlike the nav-chain M1 / meta paths below; #277 deliberately EXCLUDED
        // RIntLiteral from integerToBigDecimal because the hoisted-and-guarded form would diverge
        // from this inline literal form). The fork passed the bare `1` (an int into a BigDecimal
        // param → non-compiling, already waivered), so green-safe by construction. The beyond-long
        // literal stays on the bigIntToBigDecimal (b) `new BigDecimal(new BigInteger("…"))` hoist
        // path (a literal exceeding long cannot be an inline `BigDecimal.valueOf` int arg).
        if (arg instanceof RIntLiteral
                && compiler.getTypeUtil().isInteger(actualItemType)
                && compiler.getTypeUtil().isBigDecimal(paramJavaType)) {
            // v3.2 seat 13 (site R7): the inline-literal arm is a PLAN; tryMetaDerefArg renders it.
            return new ArgCoercionPlan(actualItemType, paramJavaType, true);
        }
        // Gate: the hoist + null-guarded conversion fires for three arg/param type
        // mismatches the corpus exercises, ALL served by the same machinery —
        //   (a) meta-wrapper arg vs plain value param (PR #109 ref_coercion):
        //       FieldWithMetaX/ReferenceWithMetaX → X via .getValue();
        //   (b) BigInteger arg vs BigDecimal param (PR #128 bigint_bigdecimal_coerce):
        //       a `number` literal exceeding `long`
        //       (e.g. 9999999999999999999999999 → new BigInteger("…")) where the
        //       callee param expects BigDecimal → new BigDecimal(x);
        //   (c) Integer arg vs BigDecimal param (facet numCoerceArgHoist, PR #277):
        //       a NULLABLE Integer nav-chain value (`…<Integer>map("getPeriodMultiplier",
        //       …).get()`) passed to a BigDecimal-expecting param hoists
        //       `final Integer integer = <chain>.get();` + coerces
        //       `(integer == null ? null : BigDecimal.valueOf(integer))` (upstream
        //       convertNullSafe for a boxed-Integer narrow — BigDecimal.valueOf(int)
        //       auto-unboxes, so the value is null-guarded). LITERAL Integer args are
        //       EXCLUDED (an RIntLiteral is provably non-null — golden emits the bare
        //       `BigDecimal.valueOf(<lit>)` with no hoist/guard there, so guarding it
        //       would diverge); only nav-chain / Mapper.get() values (the actual
        //       carriers — FloatingRateResetFrequency*MultiplierOf*) reach this arm.
        // The coercion-service probe below confirms a REAL (non-identity) conversion
        // before committing, so the conversion string is never hardcoded here.
        boolean metaDeref = actualItemType instanceof RJavaWithMetaValue;
        // facet inverseN7CalleeParamMeta (PR #347): a META-annotated callee param EXPECTS
        // the wrapper — upstream's type-directed coercion is IDENTITY there, so golden
        // passes the whole nav chain's wrapper BARE (cdm5 Create_Cashflow's
        // `currency string [metadata scheme]` ← Create_OnDemandInterestPaymentPrimitive-
        // Instruction passes `…<FieldWithMetaString>map("getCurrency", …).get()` with no
        // hoist/deref). toJavaReferenceType STRIPS meta from the param, so the
        // type-equality gate above cannot decline (the #346 cp1 catch); the #346 W1/W2
        // call sites already carry this gate when ARMING functionItemLambdaRecovery —
        // this core decline extends it to every route (the #237 statement-sink path
        // reached this seat ungated).
        if (metaDeref && MetaFieldGenerator.detectMetaKind(param)
                != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        boolean bigIntToBigDecimal = compiler.getTypeUtil().isBigInteger(actualItemType)
                && compiler.getTypeUtil().isBigDecimal(paramJavaType);
        boolean integerToBigDecimal = !(arg instanceof RIntLiteral)
                && compiler.getTypeUtil().isInteger(actualItemType)
                && compiler.getTypeUtil().isBigDecimal(paramJavaType);
        if (!metaDeref && !bigIntToBigDecimal && !integerToBigDecimal) {
            return null;
        }
        // Probe whether the coercion service produces a REAL conversion (the
        // meta-unwrap or the BigInteger→BigDecimal construction). If it returns the
        // bare variable reference unchanged, there is no conversion to apply —
        // decline (keeps the flat form).
        String baseVar = JavaNamingUtil.toFirstLower(actualItemType.getSimpleName());
        JavaExpression probeRef = JavaExpression.from(baseVar, actualItemType);
        JavaExpression probeConv = compiler.getCoercionService()
                .coerceExpression(probeRef, actualItemType, paramJavaType, true, ctx.scope());
        if (probeConv.renderToString().equals(baseVar)) {
            return null;
        }
        return new ArgCoercionPlan(actualItemType, paramJavaType, false);
    }

    /**
     * v3.2 seat 13 (D53, site R7): the COMMIT half of the pre-seat {@code tryMetaDerefArg}, verbatim - the hoist of
     * the unwrapped reference into a deferred-named local on the route's consumer (the statement sink, the lambda
     * channel or the block's hoist list) and the null-guarded coercion of that local.
     */
    private JavaExpression commitArgCoercion(
            JavaType actualItemType, JavaClass<?> paramJavaType, String baseVar, JavaExpression unwrappedExpr,
            ExpressionContext ctx, ExpressionCompiler compiler,
            List<JavaStatement> hoists, JavaStatementScope statementSink,
            boolean functionItemLambdaRecovery, boolean bareLocal) {
        // v3.2 seat 13 (D53, the R7 heal - oracle group arg-coercion-bare-local, pinned FIRST): a BARE LOCAL is
        // coerced INLINE on its own name with no hoist - the plugin's convertNullSafe law for a plain variable
        // (`check.evaluate((nat == null ? null : BigDecimal.valueOf(nat)))` in a typeAlias condition's datarule,
        // `(n == null ? null : BigDecimal.valueOf(n))` for a function input). NUMERIC plans only: a meta wrapper read
        // bare from an input already derefs inline through the #342 metaInputParam arm one seat up, and the two arms
        // must not compose. The chaos M5f rows (C27NatCalled x18) are this class - a HEAL, not a refusal.
        if (bareLocal && !(actualItemType instanceof RJavaWithMetaValue)) {
            String local = unwrappedExpr.renderToString();
            JavaExpression localRef = JavaExpression.from(local, actualItemType);
            JavaExpression inlineConv = compiler.getCoercionService()
                    .coerceExpression(localRef, actualItemType, paramJavaType, true, ctx.scope());
            Set<JavaClass<?>> inlineRefs = new HashSet<>(unwrappedExpr.getRefs());
            inlineRefs.addAll(inlineConv.getRefs());
            Set<JavaClass<?>> inlineWildcards = new HashSet<>(unwrappedExpr.getStaticWildcardImports());
            inlineWildcards.addAll(inlineConv.getStaticWildcardImports());
            return JavaExpression.from(
                    "(" + local + " == null ? null : " + inlineConv.renderToString() + ")",
                    paramJavaType, inlineRefs, inlineWildcards);
        }
        // Commit: hoist the unwrapped reference into a local. The local registers
        // through the PR #170 deferred-naming machinery (facet
        // evaluate_arg_consumption arm B3, naming): one GeneratedIdentifier is
        // shared by the declaration statement (which resolves it at render time,
        // when the scope is legitimately closed — never via getActualName here,
        // which would CLOSE the scope mid-compilation and break later
        // registrations in the same expression) and by the sentinel-rendered
        // string references below (resolved at statement finalization via
        // resolveDeferredCoercionNames). computeActualNames then applies the
        // upstream group law: a hoist sharing its desired base with the
        // statement's guarded Type-coercion params NUMBERS with them
        // (`referenceWithMetaTradeState4` after the in-chain 0..3 — golden's
        // registration-order numbering); a single-member group resolves to the
        // IDENTICAL escaped-iff-taken name the pre-#170 disambiguate produced.
        GeneratedIdentifier id = ctx.scope().createUniqueIdentifier(baseVar);
        String nameToken = ctx.scope().registerDeferredCoercionName(id);
        if (statementSink != null) {
            // facet evalArgMetaDerefHoist (PR #237): the nested-operand statement-sink
            // route — register the hoist decl as a sentinel-bearing STRING on the
            // statement-hoist sink (mirroring #236 hoistMetaDerefCtorValueOrNull).
            // prependStatementHoists lifts it ahead of the statement; the sentinel
            // nameToken resolves at finalizeDeferredNames together with the arg deref
            // below, so the deferred-coercion numbering (golden's registration-order group
            // law — bare for a singleton, base0..n-1 / `_`-escape across a same-base group)
            // is identical to the BLOCK route's JavaLocalVariableDeclarationStatement. The
            // decl cannot be the statement OBJECT here: its render() calls
            // GeneratedIdentifier.getActualName(), which would close the scope
            // mid-compilation; the string carries the sentinel instead. The wrapper-type
            // import flows via the returned arg expr's refs (unwrappedExpr.getRefs() + the
            // explicit actualItemType add below), exactly as the BLOCK route surfaces it.
            statementSink.registerStatementHoist(
                    "final " + actualItemType.getSimpleName() + " " + nameToken + " = "
                            + unwrappedExpr.renderToString() + ";");
        } else if (functionItemLambdaRecovery) {
            // facet itemGetMetaDerefBlock (PR #346): the LAMBDA-channel decl renders the
            // deferred TOKEN (see ItemGetMetaDerefHoist) — an eagerly-rendered
            // JavaLocalVariableDeclarationStatement would getActualName()-close the whole
            // ancestor scope chain mid-statement and poison later same-statement hoists
            // (the cp1 Qualify_AssetClass_Equity degradation).
            Set<JavaClass<?>> declRefs = new HashSet<>(unwrappedExpr.getRefs());
            if (actualItemType instanceof JavaClass<?> lambdaDeclClass) {
                declRefs.add(lambdaDeclClass);
            }
            hoists.add(new ItemGetMetaDerefHoist(actualItemType.getSimpleName(), nameToken,
                    unwrappedExpr.renderToString(), declRefs,
                    unwrappedExpr.getStaticWildcardImports()));
        } else {
            // facet groupPCounterpartyBlockRelocate (PR #362): marker-classed (see
            // EvalArgMetaDerefHoist) — behaviour-identical everywhere; the effective-else
            // block's arm drains instanceof-admit it for the in-branch relocation.
            hoists.add(new EvalArgMetaDerefHoist(actualItemType, id, unwrappedExpr, nameToken));
        }
        JavaExpression varRef = JavaExpression.from(nameToken, actualItemType);
        JavaExpression conv = compiler.getCoercionService()
                .coerceExpression(varRef, actualItemType, paramJavaType, true, ctx.scope());
        Set<JavaClass<?>> refs = new HashSet<>(unwrappedExpr.getRefs());
        refs.addAll(conv.getRefs());
        // facet evalArgMetaDerefHoist (PR #237, Copilot R1): guarantee the import of the
        // DECLARED local type. The hoist decl renders `final <actualItemType> <name> = …;`
        // (the BLOCK route via the JavaLocalVariableDeclarationStatement type, the
        // STATEMENT_SINK route via the registered string above). The wrapper import
        // otherwise rides ONLY the chain's witness generic in unwrappedExpr.getRefs() —
        // absent when the meta value comes from a chain whose render carries no `<Wrapper>`
        // witness — which would drop the import and make the generated Java non-compiling.
        // Adding actualItemType is idempotent for the witness-bearing carriers (the import
        // is already collected, so the bytes are unchanged) and closes the latent gap.
        if (actualItemType instanceof JavaClass<?> declClass) {
            refs.add(declClass);
        }
        Set<JavaClass<?>> wildcards = new HashSet<>(unwrappedExpr.getStaticWildcardImports());
        wildcards.addAll(conv.getStaticWildcardImports());
        return JavaExpression.from(
                "(" + nameToken + " == null ? null : " + conv.renderToString() + ")",
                paramJavaType, refs, wildcards);
    }

    /**
     * getmulti_multi_arg gate (PR #131). True when argument {@code argIndex} of an
     * explicit-args function call must be unwrapped to a {@code List} via
     * {@code .getMulti()} rather than a scalar via {@code .get()}: the callee's
     * parameter is multi-cardinality ({@code (1..*)} / {@code (0..*)}) AND the
     * argument expression itself is multi-cardinality ({@code RWorkspace.getCardinality}
     * = {@link ExpressionCardinality#MULTI} — a navigation ending in a multi-valued
     * feature, compiled to a {@code MapperC} {@code mapC} tail). Upstream's
     * {@code ExpressionGenerator} coerces such a {@code MapperC} argument to a
     * {@code List}-typed parameter with {@code getMulti()}; the fork otherwise emits a
     * non-compiling scalar {@code .get()} (single {@code T} where {@code List<T>} is
     * expected).
     *
     * <p>The cardinality signal is read from the AST via the workspace's cardinality
     * computer — NOT from {@code compiled.getExpressionType()}, which a navigation
     * leaves {@code null} (the chained {@code MapperC} carries no surfaced expression
     * type, same as the PR #128 BigInteger-literal surprise). The workspace computer is,
     * however, RESOLUTION-BLIND: it returns {@code SINGLE} for a top-level function-body
     * {@link RFeatureCall} whose {@code resolvedFeature} was never set during
     * type-directed resolution, and for a disguised {@link REnumValueRef} navigation (it
     * has no {@code REnumValueRef} case). {@link NavigationHandler} still emits
     * {@code .mapC(...)} for both via its own receiver-chain fallback, so the compiled
     * argument IS a multi mapper despite the {@code SINGLE} reading; PR #146 adds a
     * second probe — {@link NavigationHandler#navLeafFeatureMulti} — that recovers the
     * leaf feature's cardinality the SAME way the navigation side does, so these
     * resolution-blind multi nav-args also unwrap with {@code .getMulti()}. Declines when
     * the callee is unresolved / not an {@link RFunction}, the index is out of range, the
     * generator model is absent (the no-arg {@code ExpressionCompiler()} unit-test harness
     * leaves {@code getGeneratorModel()} null — this is the live harness guard; the
     * {@code workspace() == null} arm is defensive only, since a real
     * {@code GeneratorModel} {@code requireNonNull}s its workspace), the parameter is
     * scalar, or neither the leaf NOR the receiver chain proves multi (facet
     * evaluate_arg_consumption arm B1 adds the {@code chainProvesMulti}
     * receiver-propagating probe for interior-mapC chains) — every such case
     * keeps the existing {@code .get()} unwrap, so the change is confined to the exact
     * (multi-arg into multi-param) non-compiling form. The callee-param multi guard runs
     * first, so the broadened arg probe never applies to a scalar parameter.
     */
    private static boolean evaluateArgIsMulti(RFunction calleeFn, int argIndex,
            RExpression arg, ExpressionCompiler compiler) {
        if (calleeFn == null || argIndex >= calleeFn.inputs().size()) {
            return false;
        }
        var generatorModel = compiler.getGeneratorModel();
        if (generatorModel == null || generatorModel.workspace() == null) {
            return false;
        }
        if (!generatorModel.isMulti(calleeFn.inputs().get(argIndex))) {
            return false;
        }
        if (generatorModel.workspace().getCardinality(arg) == ExpressionCardinality.MULTI) {
            return true;
        }
        // The AST-level CardinalityComputer is resolution-blind: it returns SINGLE for a
        // top-level function-body RFeatureCall whose resolvedFeature was never set, and for a
        // disguised REnumValueRef nav (it has no REnumValueRef case). NavigationHandler still
        // emits .mapC(...) for both via its receiver-chain fallback, so the compiled argument
        // IS a multi mapper. Recover the leaf feature's cardinality the SAME way so a multi
        // nav-arg into a multi parameter unwraps with the .getMulti() the golden uses, not the
        // non-compiling scalar .get(). The callee-param multi guard above keeps this confined.
        // (gm-aware since facet choice_nav_chain_typing, in lock-step with the navigation
        // side's resolveMapMethod — the leaf-multi ⟺ mapC invariant requires both probes to
        // resolve through the same choice-narrowing fallback.)
        if (NavigationHandler.navLeafFeatureMulti(arg, compiler)) {
            return true;
        }
        // facet evaluate_arg_consumption (arm B1): both probes above read the LEAF
        // only, so a chain that is multi via an INTERIOR mapC step with a single
        // leaf (`businessEvent -> after -> transferHistory -> transfer` — transfer
        // is (0..1)) still read SINGLE and unwrapped with the non-compiling scalar
        // .get(). Upstream propagates receiver-multi through single feature steps
        // (CardinalityProvider.caseFeatureCall); consult the SAME gm-aware
        // receiver-propagating walk the renderer's mapItem selection trusts, so
        // the reading and the rendered MapperC chain cannot disagree. MONOTONE:
        // the walk only ADDs multi, and a green multi-param call site never
        // carries a scalar .get() (it would not compile against the List param).
        return NavigationHandler.chainProvesMulti(arg, compiler);
    }

    /**
     * Whether the callee parameter at {@code argIndex} is MULTI-cardinality
     * ({@code 0..*}, {@code List}-typed) — the CALLEE-driven half of the evaluate-arg
     * unwrap decision (facet {@code tailMulti}, PR #191). Upstream's
     * {@code ExpressionGenerator} coerces ANY {@code Mapper} argument into a
     * {@code List} parameter with {@code getMulti()} regardless of the argument's own
     * cardinality ({@code MapperS.getMulti()} yields a 0-or-1 element list), so the
     * unwrap accessor follows the PARAMETER, not the argument.
     *
     * <p>This is the relaxation of {@link #evaluateArgIsMulti}, which additionally
     * required the ARGUMENT to be multi (PR #131/#146/#171 only widened a
     * {@code MapperC} chain into a multi parameter) and therefore left a SINGLE
     * {@code Mapper} chain into a multi parameter unwrapping with the non-compiling
     * scalar {@code .get()} ({@code MapBondOptionAccountPartyReference}'s
     * {@code getBuyerSellerModel} navigation into the {@code 0..*}
     * {@code fpmlBuyerSellerModelModelList} parameter is the exemplar). Since
     * {@code evaluateArgIsMulti} runs the same callee-param-multi check first, every
     * {@code argAsMulti} position is also a {@code paramAcceptsMulti} position — so
     * driving {@link #unwrapForEvaluateArg}'s accessor from this predicate is a pure
     * superset that keeps the existing {@code MapperC}-into-multi positions and adds
     * the single-{@code Mapper}-into-multi ones.
     *
     * <p>Safe to drive the accessor: {@link #unwrapForEvaluateArg} consults the flag
     * only at its fall-through, which fires solely for a chained {@code Mapper}
     * expression (every {@code Mapper} has {@code getMulti()}); the bare/structural/
     * enum-constant/{@code "null"} branches return first and ignore it. A scalar
     * {@code .get()} into a {@code List} parameter never compiled, so no green file
     * carries the pre-fix form. Declines on the same partial-resolution guards as
     * {@link #evaluateArgIsMulti} (callee unresolved / not an {@link RFunction}, index
     * out of range, generator model absent — the no-arg {@code ExpressionCompiler()}
     * unit harness leaves {@code getGeneratorModel()} null). No {@code workspace()}
     * guard is needed here: {@code isMulti(RAttribute)} reads the declared cardinality
     * and never touches the workspace (only the arg-cardinality probes do).
     */
    // public (was private) for the IR-routed compiler's verbatim oracle reuse — the D43 seam.
    public static boolean evaluateParamIsMulti(RFunction calleeFn, int argIndex,
            ExpressionCompiler compiler) {
        if (calleeFn == null || argIndex >= calleeFn.inputs().size()) {
            return false;
        }
        var generatorModel = compiler.getGeneratorModel();
        if (generatorModel == null) {
            return false;
        }
        return generatorModel.isMulti(calleeFn.inputs().get(argIndex));
    }

    /**
     * facet coercionWitnessFollowsCalleeParam (PR #342): the callee parameter's Java
     * reference element type, for the evaluate-arg meta-coercion witness (upstream
     * compiles every arg against the parameter's expected ITEM type). Null on any
     * partial resolution — the caller keeps the receiver-strip target (pre-facet
     * bytes).
     */
    private static JavaClass<?> calleeParamElementClass(RFunction calleeFn, int argIndex,
            ExpressionCompiler compiler) {
        if (calleeFn == null || argIndex >= calleeFn.inputs().size()) {
            return null;
        }
        var gm = compiler.getGeneratorModel();
        var tt = compiler.getTypeTranslator();
        RAttribute param = calleeFn.inputs().get(argIndex);
        if (gm == null || tt == null || param.typeCall() == null) {
            return null;
        }
        var rt = gm.resolveTypeCall(param.typeCall());
        if (rt == null || rt instanceof com.regnosys.rosetta.types.RMissingType) {
            return null;
        }
        var jc = tt.toJavaReferenceType(rt);
        return jc instanceof JavaClass<?> cls ? cls : null;
    }

    /**
     * Coerces an ABSENT/empty evaluate argument into a MULTI callee parameter to the
     * typed empty list {@code Collections.<ElementType>emptyList()} (facet
     * {@code emptyMultiArg}, PR #192). Returns {@code null} (no rewrite — today's bytes)
     * for every other shape.
     *
     * <p>An {@code empty} Rune literal in argument position compiles to the bare literal
     * {@code null} ({@link #unwrapForEvaluateArg}'s {@code "null"}-source passthrough
     * preserves it). Upstream coerces it to the parameter's expected type: a MULTI
     * ({@code 0..*}, {@code List}-typed) parameter takes the item-to-list "empty"
     * conversion {@code Collections.<ElementType>emptyList()}
     * ({@code TypeCoercionService.xtend} L377-380), the same form
     * {@code FunctionExpressionRenderer}'s arm C6 already renders for a MULTI
     * conditional-output implicit else; a SINGLE parameter keeps the bare {@code null}.
     * The element type is the callee parameter's item type, resolved exactly as arm C6
     * resolves the output item type ({@link GeneratorModel#getType(RAttribute)} then
     * {@code JavaTypeTranslator.toJavaReferenceType}); the {@code java.util.Collections}
     * import and the element-type import ride the refs channel.
     *
     * <p>Fires only when the callee parameter is MULTI (the PR #191
     * {@link #evaluateParamIsMulti} probe — a {@link GeneratorModel#isMulti(RAttribute)}
     * read of the declared cardinality) AND the finalized argument renders the bare
     * literal {@code "null"}. Declines a META-annotated parameter (its list element is
     * the wrapper {@code ReferenceWithMetaX}/{@code FieldWithMetaX}, not the plain item
     * {@code gm.getType} returns — mirrors arm C6's meta decline; no corpus carrier
     * observed, the rarer case stays waivered) and the partial-resolution guards
     * ({@code calleeFn} unresolved, index out of range, generator model / translator
     * absent — the no-arg {@code ExpressionCompiler()} unit harness).
     *
     * <p>Green-safe by construction: the fork rendered {@code null} for EVERY absent
     * multi argument while the golden always renders {@code emptyList()}, so any file
     * with such an argument was already a mismatch (waivered). No green file carries the
     * pre-fix {@code null} form, so the rewrite can only touch waivered files. The
     * {@code "null"}-render + callee-param-multi gate cannot match a SINGLE parameter's
     * absent argument (it correctly stays {@code null}).
     *
     * <p>Refs/type detail: the emitted expression carries the inbound {@code argExpr}'s
     * expression-type unchanged (the empty literal's {@code NULL} type — only the refs and
     * rendered text are consumed downstream at the call site's import collection) and a refs
     * set that MERGES the inbound refs (empty for the bare {@code "null"} source today) with
     * {@link HandlerHelper#COLLECTIONS} + the element {@link JavaClass} — preserving inbound
     * refs to stay consistent with {@link #unwrapForEvaluateArg}'s {@code "null"}-source
     * passthrough (which likewise preserves the caller's refs); the inbound static-wildcard
     * imports pass through.
     */
    private static JavaExpression tryEmptyMultiArg(JavaExpression argExpr,
            boolean paramAcceptsMulti, RFunction calleeFn, int argIndex,
            ExpressionCompiler compiler) {
        if (!paramAcceptsMulti || calleeFn == null
                || argIndex >= calleeFn.inputs().size()) {
            return null;
        }
        if (!"null".equals(argExpr.renderToString())) {
            return null;
        }
        GeneratorModel generatorModel = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        if (generatorModel == null || generatorModel.workspace() == null
                || translator == null) {
            return null;
        }
        RAttribute param = calleeFn.inputs().get(argIndex);
        if (MetaFieldGenerator.detectMetaKind(param) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RType itemType = generatorModel.getType(param);
        if (itemType == null || itemType instanceof RMissingType) {
            return null;
        }
        JavaClass<?> itemJavaClass = translator.toJavaReferenceType(itemType);
        // Preserve any inbound refs (empty for the bare "null" source today) + add the
        // Collections + element-type imports — mirrors unwrapForEvaluateArg's "null"-source
        // passthrough, which likewise preserves the inbound builder's refs.
        Set<JavaClass<?>> refs = new HashSet<>(argExpr.getRefs());
        refs.add(HandlerHelper.COLLECTIONS);
        refs.add(itemJavaClass);
        return JavaExpression.from(
                "Collections.<" + itemJavaClass.getSimpleName() + ">emptyList()",
                argExpr.getExpressionType(), refs, argExpr.getStaticWildcardImports());
    }

    /**
     * facet singleVarArgIntoMulti (W42 finding #27, PR #436): a bare single-cardinality
     * non-meta function-INPUT reference passed to a MULTI ({@code 0..*}/{@code 1..*})
     * non-meta callee parameter renders upstream's {@code convertNullSafe} item→list
     * coercion directly on the compiled (escape-carrying) name:
     *
     * <pre>
     * (&lt;name&gt; == null ? Collections.&lt;Item&gt;emptyList() : Collections.singletonList(&lt;name&gt;))
     * </pre>
     *
     * The VARIABLE sibling of {@link #trySingleFnCallArgIntoMulti} — no hoist (the
     * parameter is already a named local; upstream evaluates it twice in the guard
     * exactly as the golden shows). Declines (the arg stays bare — today's
     * still-waivered bytes) for: a single or meta callee param; a MULTI-proven arg;
     * a non-symbol / args-carrying / non-RAttribute arg; a symbol that is not an
     * INPUT of the enclosing function (identity — closure-param and mis-bound reads
     * fall through); a MULTI or meta input; an unresolved or mismatched item type;
     * or a compiled render that is not a bare identifier.
     */
    private JavaExpression trySingleVarArgIntoMulti(RExpression arg, JavaExpression argExpr,
            boolean paramAcceptsMulti, boolean argAsMulti, RFunction calleeFn, int argIndex,
            ExpressionCompiler compiler) {
        if (!paramAcceptsMulti || argAsMulti || calleeFn == null
                || argIndex >= calleeFn.inputs().size()) {
            return null;
        }
        if (!(arg instanceof RSymbolReference argRef) || !argRef.args().isEmpty()) {
            return null;
        }
        RAttribute argAttr = argRef.symbol()
                .filter(RAttribute.class::isInstance).map(RAttribute.class::cast).orElse(null);
        GeneratorModel gm = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        if (argAttr == null || gm == null || translator == null || gm.isMulti(argAttr)
                || MetaFieldGenerator.detectMetaKind(argAttr) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RFunction argOwner = HandlerHelper.findEnclosingFunction(argRef);
        boolean isOwnInput = false;
        if (argOwner != null) {
            for (RAttribute ownerInput : argOwner.inputs()) {
                if (ownerInput == argAttr) {
                    isOwnInput = true;
                    break;
                }
            }
        }
        if (!isOwnInput) {
            return null;
        }
        RAttribute param = calleeFn.inputs().get(argIndex);
        if (MetaFieldGenerator.detectMetaKind(param) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RType paramItemType = gm.getType(param);
        RType argType = gm.getType(argAttr);
        if (paramItemType == null || paramItemType instanceof RMissingType
                || argType == null || argType instanceof RMissingType) {
            return null;
        }
        JavaClass<?> itemClass = translator.toJavaReferenceType(paramItemType);
        JavaClass<?> argClass = translator.toJavaReferenceType(argType);
        if (itemClass == null || !itemClass.equals(argClass)) {
            return null;
        }
        String varName = argExpr.renderToString();
        if (!SourceVersion.isIdentifier(varName)) {
            return null;
        }
        Set<JavaClass<?>> refs = new HashSet<>(argExpr.getRefs());
        refs.add(itemClass);
        refs.add(HandlerHelper.COLLECTIONS);
        return JavaExpression.from(
                "(" + varName + " == null ? Collections.<" + itemClass.getSimpleName()
                        + ">emptyList() : Collections.singletonList(" + varName + "))",
                null, refs, argExpr.getStaticWildcardImports());
    }

    /**
     * facet singleFnCallArgIntoMulti (PR #368): a DIRECT explicit-args call to a
     * SINGLE-output function, passed as an argument to a MULTI ({@code 0..*}/
     * {@code 1..*}) callee parameter, hoists the call result to a named local and
     * null-guards the upstream {@code convertNullSafe} item→list coercion:
     *
     * <pre>
     * final &lt;Item&gt; &lt;name&gt; = &lt;inner&gt;.evaluate(…);
     * &lt;callee&gt;.evaluate((&lt;name&gt; == null ? Collections.&lt;Item&gt;emptyList()
     *                                     : Collections.singletonList(&lt;name&gt;)))
     * </pre>
     *
     * <p>The evaluate-arg seat sibling of the #212
     * {@code FunctionExpressionRenderer.hoistSingleValueIntoMultiLeafOrNull} SET/ADD
     * arm — same value class (a provably-single fn call, consumed twice by the
     * null-check + the {@code singletonList}), same session-registered hoist (the
     * #237 decl-as-string pattern; the name group is the lowercased item-type simple
     * name with the #361 {@code _}-prefix input-collision escape). Declines (the arg
     * stays bare — today's still-waivered bytes) for: a single-cardinality or
     * meta-annotated callee param; a MULTI-proven arg ({@code argAsMulti} — the
     * #131/#191 {@code .getMulti()} channel owns those); a non-call / bare / non-fn
     * arg; a MULTI or meta-annotated inner output; an unresolved or mismatched
     * item type (the param element must equal the inner output's translated class —
     * a subtype coercion has no golden carrier); a keyword-shaped base name; a
     * stacked {@code _}-escape collision; or no reachable statement-hoist sink (rule
     * paths open no session; lambda interiors stop the walk).
     */
    private JavaExpression trySingleFnCallArgIntoMulti(RExpression arg, JavaExpression argExpr,
            boolean paramAcceptsMulti, boolean argAsMulti, RFunction calleeFn, int argIndex,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        if (!paramAcceptsMulti || argAsMulti || calleeFn == null
                || argIndex >= calleeFn.inputs().size()) {
            return null;
        }
        if (!(arg instanceof RSymbolReference argRef) || argRef.args().isEmpty()) {
            return null;
        }
        RFunction argCallee = argRef.symbol()
                .filter(RFunction.class::isInstance).map(RFunction.class::cast).orElse(null);
        if (argCallee == null) {
            return null;
        }
        RAttribute argOut = argCallee.output().orElse(null);
        GeneratorModel gm = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        if (argOut == null || gm == null || translator == null || gm.isMulti(argOut)
                || MetaFieldGenerator.detectMetaKind(argOut) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RAttribute param = calleeFn.inputs().get(argIndex);
        if (MetaFieldGenerator.detectMetaKind(param) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RType paramItemType = gm.getType(param);
        RType argOutType = gm.getType(argOut);
        if (paramItemType == null || paramItemType instanceof RMissingType
                || argOutType == null || argOutType instanceof RMissingType) {
            return null;
        }
        JavaClass<?> itemClass = translator.toJavaReferenceType(paramItemType);
        JavaClass<?> argOutClass = translator.toJavaReferenceType(argOutType);
        if (itemClass == null || !itemClass.equals(argOutClass)) {
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        String itemSimple = itemClass.getSimpleName();
        String baseName = JavaNamingUtil.toFirstLower(itemSimple);
        if (!SourceVersion.isIdentifier(baseName) || SourceVersion.isKeyword(baseName)) {
            return null;
        }
        if (sink.isNameTaken(baseName)) {
            baseName = "_" + baseName;
            if (sink.isNameTaken(baseName)) {
                return null;
            }
        }
        String sentinel = sink.statementHoistSession().register(baseName);
        sink.registerStatementHoist(
                "final " + itemSimple + " " + sentinel + " = " + argExpr.renderToString() + ";");
        Set<JavaClass<?>> refs = new HashSet<>(argExpr.getRefs());
        refs.add(itemClass);
        refs.add(HandlerHelper.COLLECTIONS);
        return JavaExpression.from(
                "(" + sentinel + " == null ? Collections.<" + itemSimple
                        + ">emptyList() : Collections.singletonList(" + sentinel + "))",
                null, refs, argExpr.getStaticWildcardImports());
    }

    /**
     * facet enumArgCollections (PR #269): a PRESENT enum CONSTANT passed to a MULTI ({@code 1..*})
     * callee parameter — e.g. {@code getUniqueTransactionIdentifier.evaluate(input,
     * SupervisoryBodyEnum.ASIC)} where the callee declares {@code supervisoryBody
     * SupervisoryBodyEnum (1..*)} — is coerced item→list by golden: it HOISTS the resolved constant
     * to a named local {@code final SupervisoryBodyEnum supervisoryBodyEnum = SupervisoryBodyEnum.ASIC;}
     * and null-guards the arg into the upstream {@code convertNullSafe} singletonList coercion
     * {@code (supervisoryBodyEnum == null ? Collections.<SupervisoryBodyEnum>emptyList() :
     * Collections.singletonList(supervisoryBodyEnum))} + the {@code java.util.Collections} import.
     * The fork emitted the bare {@code SupervisoryBodyEnum.ASIC}. This is the singletonList sibling
     * of {@link #tryEmptyMultiArg} (the PR #192 emptyMultiArg, which coerces only the ABSENT/{@code
     * null} arg) WITH the named-local hoist, driven solely by callee-param cardinality
     * ({@code paramAcceptsMulti}).
     *
     * <p>The hoist rides the nearest statement-hoist sink (the renderThenExtractSet thenArg-decl
     * scope, marked by PR #267) via the #237 decl-as-STRING + deferred-coercion-name pattern (the
     * sentinel resolves at {@code finalizeDeferredNames} with the arg ref, so a singleton resolves
     * to the bare {@code lowerCamel} name); the renderThenExtractSet k==0 drain lifts the decl ahead
     * of the thenArg decl (golden line order). When NO statement-hoist sink is reachable but the arg
     * sits DIRECTLY in a drainable map/extract lambda body (PR #319), the decl instead registers on
     * the lambda-body scope ({@code registerPendingLambdaHoist}), which {@code
     * CollectionHandler.compileLambda} drains into the brace block — golden's mapSingleToItem
     * block-convert (the #301/#312 LAMBDA_CHANNEL pattern). When the arg sits in a conditional
     * that {@code compileEffectiveElseConditionalBlock} is compiling (PR #340: the identity-keyed
     * blessed-node handshake + the {@code isDirectlyInBlessedConditional} directness walk), the
     * decl registers as the NAMED {@link EnumConstArgHoist} instead, which the block's per-arm
     * drains place — cond-position before the {@code if (}, arm-position inside the owning
     * branch. Gated on a PRESENT enum constant (a qualified {@link REnumValueRef} OR a bare value
     * {@link #tryBareEnumArg} already resolved) + {@code paramAcceptsMulti} + (a reachable sink OR
     * a drainable map lambda OR the blessed conditional). Declines (the arg stays bare, still
     * waivered — never a regression) for a single-cardinality param (a green-safe enum stays bare
     * there), a path with no sink, no drainable map lambda and no blessed conditional (a filter
     * predicate / sort comparator / an un-blessed conditional arm — a ternary/ladder/elseless
     * path has no cond-hoist drain slot, so the pending would orphan, the #312 over-fire class),
     * or a non-enum/null/meta arg.
     * Green-safe by construction: 0 goldens carry the bare {@code evaluate(…, <Enum>.<CONST>)} form
     * into a multi param (golden always coerces), so the fork's bare form never byte-matched a green
     * file.
     */
    private JavaExpression tryEnumSingletonListArg(RExpression arg, JavaExpression argExpr,
            boolean bareEnumResolved, boolean paramAcceptsMulti, RFunction calleeFn, int argIndex,
            ExpressionContext ctx, ExpressionCompiler compiler, JavaStatementScope sink) {
        // facet enumSingletonListLambda (PR #319): route the enum decl through the
        // LAMBDA_CHANNEL when no statement-hoist sink is reachable but the arg sits
        // DIRECTLY in a drainable map/extract lambda body — golden block-converts the
        // mapSingleToItem lambda and hoists `final <Enum> <name> = <Enum>.<CONST>;` at
        // the lambda top (the #301 numericCoercion / #312 ctorSetterMetaDeref pattern at
        // the enum-into-multi arg seat).
        boolean lambdaChannel = sink == null && HandlerHelper.isInsideDrainableMapLambda(arg);
        // facet enumSingletonListCondArm (PR #340): the #312/#319 conditional-arm exclusion
        // CLOSED at the effective-else block seat. When compileEffectiveElseConditionalBlock
        // is compiling the conditional (the identity-keyed blessed-node handshake — the
        // #327/#339 single-slot pattern), an enum arg the conditional DIRECTLY owns (its
        // condition or an arm; a nested conditional/lambda is a different node and stays
        // declined, the #219/#250 cascade guard) registers a NAMED EnumConstArgHoist pending
        // that the block's per-arm drains place — cond-position before the `if (`,
        // arm-position inside the owning branch (golden's per-occurrence
        // `final SupervisoryBodyEnum supervisoryBodyEnumN = …;` placement, numbered by the
        // #170/#333 same-scope collision-group law). Carriers: the asic/cftc/hkma/mas
        // margin+valuation UniqueTransactionIdentifier family. Away from the handshake the
        // exclusion stands — a ternary/ladder/elseless path has no cond-hoist drain slot, so
        // the pending would orphan (the #312 over-fire class).
        RExpression blessedCond = null;
        if (sink == null && !lambdaChannel) {
            RExpression candidate = ctx.scope().findEnumConstArgDrainableCond();
            if (candidate != null
                    && HandlerHelper.isDirectlyInBlessedConditional(arg, candidate)) {
                blessedCond = candidate;
            }
        }
        boolean condArmChannel = blessedCond != null;
        if (!paramAcceptsMulti || (sink == null && !lambdaChannel && !condArmChannel)
                || calleeFn == null || argIndex >= calleeFn.inputs().size()) {
            return null;
        }
        // A GENUINE enum constant only: a bare value already resolved by tryBareEnumArg, OR a
        // qualified REnumValueRef whose enumeration() is PRESENT. A disguised 2-name nav chain
        // (`head -> feature`, e.g. DTCC's `tradeForEvent -> tradeIdentifier` into a MULTI
        // TradeIdentifier param) ALSO parses as an REnumValueRef but with an EMPTY enumeration()
        // (the #288/#291 lineage) — it must DECLINE (golden passes such a multi nav chain directly
        // via .getMulti(), never singletonList-coerced). The STATEMENT_SINK route masked this (its
        // disguised-chain carriers are inside lambdas, sink == null → declined pre-#319); the #319
        // LAMBDA_CHANNEL surfaced it, so the enumeration() check is load-bearing for both routes.
        if (!bareEnumResolved
                && !(arg instanceof REnumValueRef evr && evr.enumeration().isPresent())) {
            return null; // not a present enum constant
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        if (gm == null || gm.workspace() == null || translator == null) {
            return null;
        }
        RAttribute param = calleeFn.inputs().get(argIndex);
        if (MetaFieldGenerator.detectMetaKind(param) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RType itemType = gm.getType(param);
        if (itemType == null || itemType instanceof RMissingType) {
            return null;
        }
        JavaClass<?> enumJavaClass = translator.toJavaReferenceType(itemType);
        String enumSimple = enumJavaClass.getSimpleName();
        String constant = argExpr.renderToString();
        GeneratedIdentifier id = ctx.scope()
                .createUniqueIdentifier(JavaNamingUtil.toFirstLower(enumSimple));
        String nameToken = ctx.scope().registerDeferredCoercionName(id);
        if (sink != null) {
            sink.registerStatementHoist(
                    "final " + enumSimple + " " + nameToken + " = " + constant + ";");
        } else if (lambdaChannel) {
            // LAMBDA_CHANNEL: register the enum decl on the lambda-body scope;
            // CollectionHandler.compileLambda drains it into the brace block ahead of the
            // return. The initializer refs must carry the enum class —
            // JavaLocalVariableDeclarationStatement#getRefs ignores the declared type
            // (the #237/#314/#315 established import pattern). The decl uses `id` (rendered
            // via getActualName at finalization) while the coercion below uses `nameToken`
            // (the deferred sentinel); registerDeferredCoercionName links them to the SAME
            // actual name (the #312 precedent).
            Set<JavaClass<?>> declRefs = new HashSet<>(argExpr.getRefs());
            declRefs.add(enumJavaClass);
            JavaExpression declValue = JavaExpression.from(constant, argExpr.getExpressionType(),
                    declRefs, argExpr.getStaticWildcardImports());
            ctx.scope().registerPendingLambdaHoist(
                    new JavaLocalVariableDeclarationStatement(true, enumJavaClass, id, declValue));
        } else {
            // facet enumSingletonListCondArm (PR #340): the blessed-conditional channel
            // registers the NAMED EnumConstArgHoist so compileEffectiveElseConditionalBlock's
            // per-arm drains admit it by instanceof (any other path's kind-gate declines the
            // block, keeping the pre-#340 form). The decl renders the SAME deferred nameToken
            // the coercion below consumes (the #333 dtccDeclUseConsistency law); the id
            // created above lives in the block attempt's scope, so two occurrences (the
            // if-condition + the then-arm) number supervisoryBodyEnum0/1 per the #170/#333
            // collision-group law — golden's exact per-occurrence naming.
            Set<JavaClass<?>> declRefs = new HashSet<>(argExpr.getRefs());
            declRefs.add(enumJavaClass);
            ctx.scope().registerPendingLambdaHoist(new EnumConstArgHoist(
                    enumSimple, nameToken, constant, declRefs,
                    argExpr.getStaticWildcardImports()));
        }
        Set<JavaClass<?>> refs = new HashSet<>(argExpr.getRefs());
        refs.add(HandlerHelper.COLLECTIONS);
        refs.add(enumJavaClass);
        return JavaExpression.from(
                "(" + nameToken + " == null ? Collections.<" + enumSimple + ">emptyList() : "
                        + "Collections.singletonList(" + nameToken + "))",
                argExpr.getExpressionType(), refs, argExpr.getStaticWildcardImports());
    }

    /**
     * facet dispatchVariantParamResolution (PR #369) / pointFreeLibraryFnRef (PR #436):
     * the IsLeapYear guarded-coercion block — {@code final Integer <tok> = <argText>;}
     * (the #277 deferred-token hoist) + the null-guarded
     * {@code <tok> == null ? MapperS.<Boolean>ofNull() : MapperS.of(new IsLeapYear()
     * .execute(BigDecimal.valueOf(<tok>)))} return. The guard lifts AROUND the
     * MapperS.of because the runtime {@code execute} NPEs on null, unlike a generated
     * {@code evaluate}. ONE source of truth for both admission forms: the explicit
     * one-arg call ({@code IsLeapYear(x)} — argText = the unwrapped arg) and the
     * point-free operand reference ({@code extract IsLeapYear} — argText = the
     * unwrapped implicit item).
     */
    private JavaStatementBuilder isLeapYearGuardedBlock(String argText,
            Set<JavaClass<?>> baseRefs, Set<JavaClass<?>> baseWildcards,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        GeneratedIdentifier libId = ctx.scope().createUniqueIdentifier("integer");
        String libTok = ctx.scope().registerDeferredCoercionName(libId);
        Set<JavaClass<?>> libRefs = new HashSet<>(baseRefs);
        libRefs.add(JavaClass.from(com.rosetta.model.lib.functions.IsLeapYear.class));
        libRefs.add(compiler.getTypeUtil().BIG_DECIMAL);
        libRefs.add(HandlerHelper.MAPPER_S);
        JavaExpression guardedExpr = JavaExpression.from(
                libTok + " == null ? MapperS.<Boolean>ofNull() : MapperS.of(new IsLeapYear().execute(BigDecimal.valueOf("
                        + libTok + ")))",
                null, libRefs, baseWildcards);
        JavaStatement libDecl = new ItemGetMetaDerefHoist(
                "Integer", libTok, argText,
                new HashSet<>(baseRefs), baseWildcards);
        return new JavaBlockBuilder(
                JavaStatementList.of(List.of(libDecl)), guardedExpr);
    }

    /**
     * Renders a bare function reference used in elided-argument (predicate)
     * position as an invocation of the injected function instance:
     * {@code <camelName>.evaluate(<binding>.get())}.
     *
     * <p>The enclosing implicit variable is synthesised (mirroring
     * {@link #buildImplicitInputReceiver}) and rendered through
     * {@link #handle(RImplicitVariable, ExpressionContext, ExpressionCompiler)},
     * which yields the surrounding lambda's binding (e.g. {@code item}); it is
     * then unwrapped via {@link #unwrapForEvaluateArg} to the raw evaluate-arg
     * form ({@code item.get()}). The receiver is derived through
     * {@link FunctionDependencyCollector#lowerCamelCase} — the single source of
     * truth for the injected field name — so the receiver matches the
     * {@code @Inject} field the dependency collector registers for the same
     * function.
     *
     * <p>The result is UNWRAPPED (no {@code MapperS.of}); per upstream
     * {@code ExpressionGenerator#evaluateCall} the invocation carries the
     * function's output type and the enclosing operation applies any coercion.
     */
    // public (was private) so the IR-routed compiler reuses this exact oracle VERBATIM for the
    // point-free function-application delegation (the lab's L-109) — byte-identical to this
    // legacy fallback by construction, mirroring the renderImplicitRuleInvocation reuse widened
    // alongside it below. The D43 seam.
    public JavaStatementBuilder renderImplicitFunctionInvocation(
            RSymbolReference expr, RFunction callee,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        // facet tobuilder_output_assignment (mechanism 5): a callee with NO real
        // inputs (every input is the `__synthesized_input__` placeholder the
        // AstBuilder adds to an input-less function — excluded from the generated
        // `evaluate()` signature) takes the ARGLESS call. Upstream
        // ExpressionGenerator#evaluateCall builds the argument list only from the
        // EXPLICIT arguments, so no golden ever passes an argument to a
        // zero-input callee's `evaluate()` — hence no green file carries the
        // pre-fix `evaluate(item.get())` shape (at a function-body SET site
        // `item` is additionally a FREE identifier and the pre-fix render did
        // not even compile); regression-safe by construction. The result stays
        // UNWRAPPED per this method's contract — the enclosing operation applies
        // any coercion.
        // facet bareSymInvoke (PR #280): when this bare function invocation is a
        // NAVIGATION RECEIVER (a disguised `<Function> -> feature` head, synthesized by
        // synthesizeFunctionReceiverNavigation as the receiver of an RFeatureCall) the
        // value is navigated off, so it must be MapperS.of-wrapped — golden emits
        // `MapperS.of(<fn>.evaluate(...)).<T>map(...)` (the bare-RULE sibling
        // renderImplicitRuleInvocation wraps unconditionally; this method stays UNWRAPPED
        // at its other seats — mapSingleToList bodies, list-literal elements, filter
        // predicates — which are NOT feature-call receivers, so #278 is preserved).
        boolean navReceiver = expr.parent() instanceof RFeatureCall fcParent
                && fcParent.receiver() == expr;
        boolean hasRealInputs = callee.inputs().stream()
                .anyMatch(in -> !FunctionGenerator.isSynthesizedInput(in));
        if (!hasRealInputs) {
            JavaExpression arglessCall = JavaExpression.from(
                    disambiguateDependencyReceiver(expr, callee,
                            FunctionDependencyCollector.lowerCamelCase(callee.name()),
                            compiler)
                            + ".evaluate()",
                    null, Set.of());
            // The result stays UNWRAPPED per this method's contract — the corpus
            // consumer seats each apply their own coercion to the bare argless call
            // (the contains operand re-presents it MapperC.<Item>of(...), a builder
            // setter takes the raw List — golden Create_PartyChange /
            // IsAcceptedEicCode, the #436 cp1 double-wrap catch that reverted the
            // source-side wrap experiment). The TWO seats that mishandled the bare
            // form — the whole-output SET's spurious `.get()` and the alias return's
            // missing MapperS.of — carry their own #436 zeroArgCall arms instead
            // (both in FunctionExpressionRenderer: the widened bare-fn SET fast
            // path + the renderAlias wrap).
            return navReceiver ? JavaExpression.wrappedInMapperSOf(arglessCall) : arglessCall;
        }
        // facet topLevelBareFunctionInput (PR #261): the implicit-input argument
        // depends on position, mirroring the bare-RULE sibling
        // ({@link #renderImplicitRuleInvocation}). At rule-body TOP LEVEL (no
        // enclosing extract/filter/then lambda) `item` is undefined — the rule's
        // `input` parameter is the implicit value, so golden emits
        // `<fn>.evaluate(input)` while the fork emitted the non-compiling
        // `<fn>.evaluate(item.get())`. Inside a lambda the binding `item.get()` is
        // correct and UNCHANGED; a plain FUNCTION body (no enclosing rule, or a
        // from-type-less rule) keeps the synthetic `item` path so it is byte-neutral
        // for the FUNCTION tail. Byte-identity-safe by construction: `item` is a free
        // identifier at rule-body top level so the pre-fix `item.get()` never
        // compiled — no green file carries it.
        JavaStatementBuilder itemCompiled = null;
        JavaStatementBuilder argBuilder;
        RImplicitVariable item = null;
        RRule enclosingRule = HandlerHelper.findEnclosingRule(expr);
        RCondition enclosingCondition = HandlerHelper.findEnclosingTypeCondition(expr);
        if (enclosingRule != null
                && nearestEnclosingInlineFunction(expr) == null
                && enclosingRule.fromType().isPresent()) {
            RExpression inputRef =
                    buildImplicitInputReceiver(expr, enclosingRule.fromType().orElseThrow());
            argBuilder = unwrapForEvaluateArg(
                    compiler.compile(inputRef, ctx.expectedType(), ctx.scope()));
        } else if (enclosingCondition != null
                && nearestEnclosingInlineFunction(expr) == null
                && HandlerHelper.conditionOwnerName(enclosingCondition) != null) {
            // Coverage wave D (datarule): at type-condition TOP LEVEL the implicit
            // value is the condition instance — the executeDataRule parameter —
            // so a bare function invocation takes it as the argument (golden
            // `differentOrdinalsCondition.evaluate(taxonomy)`,
            // TaxonomyDifferentOrdinals), exactly mirroring the rule arm above.
            // v3.2 seat 3 (F9): the owner may be a typeAlias — oracle golden CheckedOk
            // `isOk.evaluate(checked)` (HandlerHelper.conditionOwnerName, LAW 69).
            RExpression instanceRef = syntheticConditionInstanceRef(
                    expr.parent(), HandlerHelper.conditionOwnerName(enclosingCondition));
            argBuilder = unwrapForEvaluateArg(
                    compiler.compile(instanceRef, ctx.expectedType(), ctx.scope()));
        } else {
            item = new RImplicitVariable();
            item.setSynthetic(true);
            item.setParent(expr.parent());
            itemCompiled = handle(item, ctx, compiler);
            // facet deepBareInvokableThenHoist (PR #339): the evaluate-arg accessor follows
            // the CALLEE PARAMETER's cardinality (the #191 tailMulti law at this IMPLICIT
            // seat) — a bound thenArg piped into a multi (`List`) input unwraps `.getMulti()`
            // (golden UnderlyingIdOtherSource's `extractProductIdentifierBySource.evaluate(
            // _thenArg0.getMulti())`), while a single param keeps `.get()` byte-identically.
            // Green-safe by construction: `.get()` (a single T) into a List<T> parameter
            // never compiled, so every rewritten position is an already-waivered mismatch.
            argBuilder = unwrapForEvaluateArg(itemCompiled,
                    evaluateParamIsMulti(callee, 0, compiler));
        }
        if (!(argBuilder instanceof JavaExpression argExpr)) {
            throw new IllegalStateException(
                    "Implicit function-call argument unwrapped to non-expression type: "
                    + argBuilder.getClass().getSimpleName());
        }
        // facet voidChainConstantFold (PR #366): an implicit item piped from a
        // Void-typed then-step coerces to the callee input's EMPTY representation —
        // the bare `null` literal at the unwrapped item seat (upstream
        // TypeCoercionService.empty for a non-wrapper/list/boolean expected type is
        // JavaLiteral.NULL). The lambda item itself compiles TYPE-less (P366A:
        // render=item type=null), so Void-ness reads from the item's SOURCE exactly
        // as upstream's coercion does: the implicit-receiver extract lambda hosting
        // this call is the body of a then-step whose ARGUMENT's inferred type is
        // NOTHING. Positive hop gates (ownerFn IS the extract's inline body, the
        // extract receiver is the implicit item — EMPTY or the parser-MATERIALIZED
        // RImplicitVariable, the P366B probe / the #358 materialization class — and
        // the outer inline fn IS the then's body) keep every explicit-receiver /
        // non-then seat on the existing render. Golden SettlementCurrency2Rule:
        // `convertNonISOToISOCurrency.evaluate(null)`.
        if (item != null && HandlerHelper.findEnclosingRule(expr) != null
                && compiler.getGeneratorModel() != null
                && compiler.getGeneratorModel().workspace() != null
                && compiler.getTypeTranslator() != null) {
            RInlineFunction voidOwnerFn = nearestEnclosingInlineFunction(expr);
            if (voidOwnerFn != null && voidOwnerFn.parent() instanceof RExtractExpr voidExt
                    && voidExt.body() == voidOwnerFn
                    && voidExt.left().map(RImplicitVariable.class::isInstance).orElse(true)
                    && voidExt.parent() instanceof RInlineFunction voidOuterFn
                    && voidOuterFn.parent() instanceof RThenExpr voidOwnerThen
                    && voidOwnerThen.body().orElse(null) == voidOuterFn) {
                RMetaAnnotatedType voidSrcInf = compiler.getGeneratorModel().workspace()
                        .getInferredType(voidOwnerThen.argument());
                if (voidSrcInf != null && compiler.getTypeUtil().isVoid(
                        compiler.getTypeTranslator().toJavaReferenceType(voidSrcInf.type()))) {
                    argExpr = JavaExpression.from("null", null, Set.of());
                }
            }
        }
        // facet multiCondBaseThenArg (PR #350, F2b): the elementwise wrapper deref at the
        // IMPLICIT evaluate-arg seat — inside a mapListToItem lambda the item is the
        // bound-LoL's inner MapperC<Wrapper>, and a MULTI callee param typed by the
        // wrapper's VALUE (equal or MODEL supertype — the #323 flat-class walk) derefs
        // elementwise before the `.getMulti()` (the #349-S2 law): golden GetUnderlier*'s
        // `getProductIdentifierFilteringISIN.evaluate(item.<ProductIdentifier>map("Type
        // coercion", referenceWithMetaProductIdentifier -> referenceWithMetaProductIdentifier
        // .getValue()).getMulti())`. Green-safe: List<Wrapper> into a List<Value> param
        // never compiled.
        // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the IMPLICIT twin of the
        // explicit-arg gate reads the same arm report. The c7b census: 32,954 / 36,302 / 32,326
        // arrivals (default-route, IR-route, optimised), text=true at 114, and `prod = implicitEvalArgUnwrap AND multi = true` selects exactly
        // those 114 — EXACT here. The #366 Void constant-fold's decline is preserved BY
        // CONSTRUCTION and no longer needs to be encoded in the suffix: when it fires it REPLACES
        // argExpr with a fresh `JavaExpression.from("null", …)`, which carries no arm report (the
        // census saw all 6 of its arrivals — 6 on each of the three walks — at text=false). The
        // two sibling unwrap arms (rule input / condition instance) never reach here at all: their
        // 748 / 854 / 728 arrivals (default-route, IR-route, optimised) all have `item == null`,
        // which this seat's own first conjunct already declines.
        if (item != null && argExpr instanceof JavaExpression.EvaluateArgMultiExtracted
                && compiler.getTypeUtil() != null && compiler.getGeneratorModel() != null
                && compiler.getTypeTranslator() != null && !callee.inputs().isEmpty()
                && MetaFieldGenerator.detectMetaKind(callee.inputs().get(0))
                        == MetaFieldGenerator.MetaKind.NONE) {
            JavaType lolItem = CollectionHandler.boundLoLLambdaItemMapperType(
                    item, ctx.scope(), compiler);
            JavaType lolElem = lolItem == null
                    ? null
                    : compiler.getTypeUtil().getItemType(lolItem);
            if (lolElem instanceof RJavaWithMetaValue lolWrapper
                    && lolWrapper.getValueType() instanceof JavaClass<?> lolValueClass) {
                RType lolParamRType = compiler.getGeneratorModel().getType(callee.inputs().get(0));
                JavaClass<?> lolParamElem = lolParamRType == null
                        ? null
                        : compiler.getTypeTranslator().toJavaReferenceType(lolParamRType);
                if (lolParamElem != null
                        && valueModelExtendsParamElem(lolValueClass, lolParamElem, compiler)) {
                    String lolRender = argExpr.renderToString();
                    String lolHead = lolRender.substring(0,
                            lolRender.length() - ".getMulti()".length());
                    String lolLam = JavaNamingUtil.toFirstLower(lolWrapper.getSimpleName());
                    Set<JavaClass<?>> lolRefs = new HashSet<>(argExpr.getRefs());
                    lolRefs.add(lolParamElem);
                    argExpr = JavaExpression.from(
                            lolHead + ".<" + lolParamElem.getSimpleName()
                                    + ">map(\"Type coercion\", " + lolLam + " -> " + lolLam
                                    + ".getValue()).getMulti()",
                            argExpr.getExpressionType(), lolRefs,
                            argExpr.getStaticWildcardImports());
                }
            } else if (lolElem == null) {
                // facet ruleMultiCondBaseThenArg (PR #365, F-D2b): the then-BOUND sibling of
                // the LoL arm above — the bound thenArg is a MapperC of a meta WRAPPER (the
                // F-D2 meta-recovered `final MapperC<ReferenceWithMetaProductIdentifier>
                // thenArg;` decl), and a MULTI callee param typed by the wrapper's VALUE
                // derefs elementwise before the `.getMulti()` (the #349-S2 unguarded law):
                // golden BasketConstituentIdentifier[Source]Rule's `evaluate(thenArg
                // .<ProductIdentifier>map("Type coercion", referenceWithMetaProductIdentifier
                // -> referenceWithMetaProductIdentifier.getValue()).getMulti())`. The two
                // arms are mutually exclusive by the binding kind (LoL-bound vs then-bound);
                // green-safe: List<Wrapper> into a List<Value> param never compiled.
                JavaType boundThenT = enclosingThenArgType(expr, ctx);
                JavaType boundElem = boundThenT == null
                        ? null
                        : compiler.getTypeUtil().getItemType(boundThenT);
                if (compiler.getTypeUtil().isMapperC(boundThenT)
                        && boundElem instanceof RJavaWithMetaValue boundWrapper
                        && boundWrapper.getValueType() instanceof JavaClass<?> boundValueClass) {
                    RType paramRType = compiler.getGeneratorModel().getType(callee.inputs().get(0));
                    JavaClass<?> paramElem = paramRType == null
                            ? null
                            : compiler.getTypeTranslator().toJavaReferenceType(paramRType);
                    if (paramElem != null
                            && valueModelExtendsParamElem(boundValueClass, paramElem, compiler)) {
                        String boundRender = argExpr.renderToString();
                        String boundHead = boundRender.substring(0,
                                boundRender.length() - ".getMulti()".length());
                        String boundLam = JavaNamingUtil.toFirstLower(boundWrapper.getSimpleName());
                        Set<JavaClass<?>> boundRefs = new HashSet<>(argExpr.getRefs());
                        boundRefs.add(paramElem);
                        argExpr = JavaExpression.from(
                                boundHead + ".<" + paramElem.getSimpleName()
                                        + ">map(\"Type coercion\", " + boundLam + " -> " + boundLam
                                        + ".getValue()).getMulti()",
                                argExpr.getExpressionType(), boundRefs,
                                argExpr.getStaticWildcardImports());
                    }
                }
            }
        }
        String receiver = disambiguateDependencyReceiver(expr, callee,
                FunctionDependencyCollector.lowerCamelCase(callee.name()), compiler);
        // valuation_currency_metabox_deref (PR #144): when the piped implicit value is a
        // meta wrapper (RJavaWithMetaValue) the callee receives as its plain value type
        // (e.g. ConvertNonISOToISOCurrency taking a String from a FieldWithMetaString
        // `currency` navigation), hoist the wrapper into a `final <MetaType> <var> =
        // item.get();` local and pass the null-guarded `.getValue()` deref — reusing the
        // explicit-args sibling's tryMetaDerefArg machinery (PR #109). The block is wrapped
        // in MapperS.of (mirroring handle(RSymbolReference)) and returned as a
        // JavaBlockBuilder the extract/map block-lambda renderer
        // (CollectionHandler.compileLambda) consumes. GATED through metaDerefHoistRoute's
        // extract/then context so the block is produced only where a renderer lifts it; in
        // any other position the flat (still-waivered) form is kept — zero regression, the
        // raw FieldWithMetaString-into-String arg never compiled.
        //
        // The synthesised `item` reference carries NO Java type (handle(RImplicitVariable)
        // returns a bare untyped identifier), so the piped element type is recovered from
        // the enclosing then's `thenArg` scope binding — typed MapperS<FieldWithMetaString>
        // only once renderThenExtractSet declares it with the meta wrapper (PR #144 edit 1),
        // so the two edits are co-dependent. A TYPED `item` expression is built from it so
        // tryMetaDerefArg's `compiled.getExpressionType()` probe surfaces the meta wrapper.
        JavaType pipedReceiverType = enclosingThenArgType(expr, ctx);
        // facet functionImplicitItemArgMeta (PR #340): the thenArg scope binding is the
        // THEN-chain recovery — an extract OUTSIDE a then-pipe (a ctor-field
        // `<nav> extract ConvertNonISOToISOCurrency`, BarrierFromTriggerEvent drr) has no
        // binding, so recover the piped element's wrapper from the owning extract's own
        // RECEIVER chain via the (now path-agnostic) #285 walk — implicitItemArgMeta over
        // the SAME synthetic item, whose owning-argument walk lands on the
        // `… -> currency` terminal ([metadata scheme] → MapperS<FieldWithMetaString>).
        // A non-meta receiver recovers null and the flat form stands. TWO belts (the cp4
        // over-fire catches): (1) DIRECTNESS (the #171 law) — the call must sit AS a
        // drainable map/extract lambda body (isInsideDrainableMapLambda), whose
        // CollectionHandler.compileLambda consumer lifts the #144 arm's JavaBlockBuilder
        // or drains the lambda-channel decl; (2) an OPEN scope. The former belt (3) —
        // the STATEMENT-DIRECT ancestor allow-list — became a ROUTE selector at PR #346:
        // a statement-direct seat keeps the #340 JavaBlockBuilder (BarrierFromTriggerEvent
        // byte-identical), while an OPERAND seat (a logical/comparison chain condition,
        // e.g. Qualify_AssetClass_Equity's areEqual(… .mapItem(item -> <bare fn>) …)) —
        // which re-renders under the #257 double-render machinery, where a second-pass
        // JavaBlockBuilder wrapper-local landed in a FINALIZED scope and degraded the
        // whole assignOutput to the TODO form (dl 5 → 36 AWAY, caught twice at the #340
        // cp4) — now registers the decl on the LAMBDA channel instead: the per-pass
        // lambda-body scope registration + compileLambda drain is self-contained under
        // the double-render (each pass opens its own bodyScope, the #170 isolation), so
        // the discarded pass's decl never leaks and the kept pass renders golden's
        // in-lambda hoist (facet itemGetMetaDerefBlock).
        if (pipedReceiverType == null && item != null
                && !ctx.scope().isClosed()
                && HandlerHelper.isInsideDrainableMapLambda(expr)) {
            pipedReceiverType = NavigationHandler.implicitItemArgMeta(item, compiler);
        }
        // facet ruleRefWrapThenArg (PR #360): the CONDITION-nested bare-fn call inside an
        // elseless-BLOCK conditional lambda body (`extract [if GetPriceNotation = … then …]`
        // — the iosco SpreadLeg family) admits through the #354 blockArmSeatConditionals
        // channel exactly like the ctor-nav twin: compileElselessConditionalBlock registers
        // the conditional around its CONDITION compile, drains the marker-classed decl and
        // renders it at BLOCK TOP (golden `final ReferenceWithMetaPriceSchedule
        // referenceWithMetaPriceSchedule0 = item.get();` before the if). The inline-ternary
        // fallback runs channel-empty and keeps the flat form.
        MetaDerefHoistRoute implRoute = metaDerefHoistRoute(expr);
        boolean blockArmCondSeat = false;
        // facet ruleCondBaseEvalArgDeref (PR #388): the DEEP-THEN ITE handshake seat —
        // the call sits in the CONDITION or an ARM of EXACTLY the conditional the #351
        // single-slot handshake is compiling (node identity, the #351 law), i.e. the
        // #374 cond-at-base `final MapperS<X> thenArg; if (…) {…}` block inside a
        // lambda. The (a)-arm LAMBDA_CHANNEL class kept its flat decline here until a
        // golden carrier admitted it (the #363 note) — csa CountryAndProvinceOrTerritory-
        // OfIndividualRule is that carrier (`final ReferenceWithMetaParty
        // referenceWithMetaParty0 = item.get();` at lambda top for the condition +
        // `referenceWithMetaParty1` INSIDE the then branch). A call under a NESTED
        // conditional inside an arm walks to the nested node ≠ the handshake's, so it
        // keeps the decline (no golden carrier).
        boolean deepThenIteCondSeat = false;
        if (implRoute == MetaDerefHoistRoute.LAMBDA_CHANNEL && ctx.scope() != null) {
            RNode condWalk = expr.parent();
            while (condWalk != null && !(condWalk instanceof RConditionalExpr)) {
                condWalk = condWalk.parent();
            }
            if (condWalk instanceof RConditionalExpr firstCond) {
                blockArmCondSeat = ctx.scope().isBlockArmSeatConditional(firstCond);
                var iteHandshake = ctx.scope().findDeepThenIteHoistCond();
                deepThenIteCondSeat = iteHandshake != null && iteHandshake.cond() == firstCond;
            }
        }
        // facet inLambdaFilterArgDeref (PR #363): the FILTER-predicate operand seat
        // admits through the LAMBDA channel — the metaDerefHoistRoute RFilterExpr arm
        // routes a call NESTED in a filter predicate (`then filter <Fn> = True`) here,
        // and the #144/#346 machinery derefs the piped META item at the evaluate-arg
        // (golden jfsa OriginalSwapUTI/OriginalSwapUTIProprietary:
        // `.filterItemNullSafe(item -> { final FieldWithMetaString
        // _fieldWithMetaString = item.get(); return areEqual(MapperS.of(
        // isMax32UpperCaseAlphanumericText.evaluate((_fieldWithMetaString == null ?
        // null : _fieldWithMetaString.getValue()))), …).get(); })`). The entrant is
        // operand-class BY CONSTRUCTION (sentinel decl + pending-lambda registration,
        // drained by compileLambda into the block form — never a JavaBlockBuilder,
        // the #340 operand-degradation law); a piped NON-wrapper item (the
        // MapperS<String> predicate siblings) type-equality-declines inside
        // tryMetaDerefArg, byte-neutral. Scoped to the FILTER seat — an extract-lambda
        // nested operand (the (a)-arm LAMBDA_CHANNEL class) keeps its pre-#363 flat
        // decline until a golden carrier admits it.
        boolean filterPredicateSeat = false;
        if (implRoute == MetaDerefHoistRoute.LAMBDA_CHANNEL) {
            RInlineFunction seatInline = nearestEnclosingInlineFunction(expr);
            filterPredicateSeat = seatInline != null
                    && seatInline.parent()
                            instanceof com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
        }
        if ((implRoute == MetaDerefHoistRoute.BLOCK || blockArmCondSeat || filterPredicateSeat
                        || deepThenIteCondSeat)
                && !callee.inputs().isEmpty()
                && pipedReceiverType != null
                && MetaFieldGenerator.detectMetaKind(callee.inputs().get(0))
                        == MetaFieldGenerator.MetaKind.NONE
                && itemCompiled instanceof JavaExpression itemExpr) {
            JavaExpression typedItem = JavaExpression.from(
                    itemExpr.renderToString(), pipedReceiverType,
                    itemExpr.getRefs(), itemExpr.getStaticWildcardImports());
            // A block-arm-conditional seat is ALWAYS the sentinel/lambda-channel class
            // (its decl drains via the elseless block's cond-hoist window, never a
            // JavaBlockBuilder — the operand-degradation law). The #363 filter-predicate
            // seat is operand-class too, and so is the #388 deep-then ite handshake seat
            // (its decls drain at lambda top / into the owning branch).
            boolean stmtDirect = implicitArgMetaSeatIsStatementDirect(expr)
                    && !blockArmCondSeat && !filterPredicateSeat && !deepThenIteCondSeat;
            List<JavaStatement> hoists = new ArrayList<>();
            // The operand class takes the sentinel-decl form (functionItemLambdaRecovery
            // = true): an eagerly-rendered decl object would getActualName()-close the
            // ancestor chain mid-statement (the cp1 degradation) — see
            // ItemGetMetaDerefHoist. The statement-direct class keeps the #340 decl
            // object + JavaBlockBuilder byte-for-byte (BarrierFromTriggerEvent).
            JavaExpression derefArg = tryMetaDerefArg(
                    item, typedItem, argExpr, callee, 0, ctx, compiler, hoists, null,
                    !stmtDirect);
            if (derefArg != null && !hoists.isEmpty()) {
                JavaExpression innerCall = JavaExpression.from(
                        receiver + ".evaluate(" + derefArg.renderToString() + ")",
                        null, derefArg.getRefs(), derefArg.getStaticWildcardImports());
                if (stmtDirect) {
                    // facet listOfListsCardinality (PR #370, F-D): the #298/#301 cardinality
                    // law at the meta-deref BLOCK seat — a MULTI-output callee's List
                    // re-presents as MapperC.<X>of(...) (golden mapItemToList lambda
                    // `return MapperC.<String>of(partyIdentifierType.evaluate(...))`,
                    // DTCC_OtherPaymentPayer/ReceiverIDTypeRule); a single-output callee
                    // keeps the MapperS.of block byte-identically, and a META-annotated
                    // output declines to MapperS.of (mirroring the #298 nav-receiver arm).
                    // Green-safe by construction: MapperS.of over a List value never
                    // compiled, so every rewritten carrier was already waivered.
                    JavaStatementBuilder wrapped = null;
                    GeneratorModel wgm = compiler.getGeneratorModel();
                    var wtt = compiler.getTypeTranslator();
                    RAttribute wOut = callee.output().orElse(null);
                    if (wgm != null && wtt != null && wOut != null && wOut.typeCall() != null
                            && wgm.isMulti(wOut)
                            && MetaFieldGenerator.detectMetaKind(wOut)
                                    == MetaFieldGenerator.MetaKind.NONE) {
                        RType wRt = wgm.resolveTypeCall(wOut.typeCall());
                        if (wRt != null && !(wRt instanceof RMissingType)) {
                            JavaClass<?> wElem = wtt.toJavaReferenceType(wRt);
                            var wtu = compiler.getTypeUtil();
                            wrapped = JavaExpression.wrappedInMapperCOfSingle(innerCall,
                                    wtu.wrap(wtu.MAPPER_C, wElem), wElem);
                        }
                    }
                    if (wrapped == null) {
                        wrapped = JavaExpression.wrappedInMapperSOf(innerCall);
                    }
                    if (wrapped instanceof JavaExpression wrappedExpr) {
                        return new JavaBlockBuilder(JavaStatementList.of(hoists), wrappedExpr);
                    }
                } else {
                    // facet itemGetMetaDerefBlock (PR #346): the operand class — LAMBDA
                    // channel (see the route-selector note above). Return the BARE
                    // invocation: the extract machinery wraps a bare-fn body in
                    // MapperS.of itself, so returning the pre-wrapped form here
                    // double-wrapped (the cp1 Qualify_UnderlierObservable_Equity
                    // `MapperS.of(MapperS.of(…))` catch).
                    for (JavaStatement hoist : hoists) {
                        ctx.scope().registerPendingLambdaHoist(hoist);
                    }
                    // facet ruleCondBaseEvalArgDeref (PR #388): at a NAV-RECEIVER
                    // position the flat route below wraps the invocation, so the
                    // deref'd call keeps the SAME wrap — the chained `.map(…)`
                    // compiles on the Mapper (the #374 cond-at-base arm chain,
                    // `MapperS.of(naturalPersonBuyerOrSeller.evaluate(…)).<Contact-
                    // Information>map(…)`). A lambda-BODY-ROOT / operand call stays
                    // BARE (the extract machinery / operand-wrap helpers wrap it —
                    // the #346 double-wrap catch).
                    if (navReceiver) {
                        JavaStatementBuilder recvWrapped =
                                wrapNavReceiverInvocation(innerCall, callee, compiler);
                        if (recvWrapped instanceof JavaExpression recvWrappedExpr) {
                            return recvWrappedExpr;
                        }
                    }
                    return innerCall;
                }
            }
        }
        JavaExpression invocation = JavaExpression.from(
                receiver + ".evaluate(" + argExpr.renderToString() + ")",
                null,
                argExpr.getRefs(),
                argExpr.getStaticWildcardImports());
        // facet bareInvokeMapperCWrap (PR #298): a bare FUNCTION invocation used as a NAVIGATION
        // RECEIVER whose callee OUTPUT is MULTI wraps `MapperC.<X>of(<fn>.evaluate(...))` not
        // `MapperS.of(...)` — the cardinality-aware extension of #280's bareSymInvoke nav-receiver
        // wrap. The fork wrapped MapperS.of unconditionally, so a multi-output function nav (e.g.
        // `contract_Price_Monetary` returning `PriceSchedule (0..*)`, navigated `.getPriceType()`)
        // mis-typed the receiver `MapperS<List<PriceSchedule>>`, and the lambda then navigates a
        // `List` (the navigation feature does not exist on `List`) — which never compiled (already
        // waivered). Golden keeps the list cardinality:
        // `MapperC.<PriceSchedule>of(contract_Price_Monetary.evaluate(item.get())).<PriceTypeEnum>map(…)`.
        // Cardinality-gated: a SINGLE-output function nav stays `MapperS.of` (the #280 form unchanged,
        // so every #280 carrier is byte-identical). Green-safe by construction: `MapperS.of` over a
        // multi list value is a non-compiling type mismatch, so every carrier this flips was already
        // a waivered mismatch. A META-annotated output declines (its emitted witness is the meta
        // wrapper, not the bare X — a separate, corpus-unverified shape kept on the old MapperS.of).
        if (navReceiver) {
            JavaStatementBuilder navWrapped = wrapNavReceiverInvocation(invocation, callee,
                    compiler);
            if (navWrapped instanceof JavaExpression navWrappedExpr) {
                return navWrappedExpr;
            }
        }
        return invocation;
    }

    /**
     * The #298 cardinality-aware nav-receiver wrap for a bare function invocation —
     * a MULTI meta-free callee output wraps {@code MapperC.<X>of(…)}, everything else
     * {@code MapperS.of(…)}. Extracted at PR #388 so the deref'd-arg route
     * (facet ruleCondBaseEvalArgDeref) and the flat route share ONE wrap law.
     */
    private static JavaStatementBuilder wrapNavReceiverInvocation(JavaExpression invocation,
            RFunction callee, ExpressionCompiler compiler) {
        GeneratorModel gm = compiler.getGeneratorModel();
        var tt = compiler.getTypeTranslator();
        RAttribute calleeOut = callee.output().orElse(null);
        if (gm != null && tt != null && calleeOut != null && calleeOut.typeCall() != null
                && gm.isMulti(calleeOut)
                && MetaFieldGenerator.detectMetaKind(calleeOut) == MetaFieldGenerator.MetaKind.NONE) {
            RType outRt = gm.resolveTypeCall(calleeOut.typeCall());
            if (outRt != null && !(outRt instanceof RMissingType)) {
                JavaClass<?> elem = tt.toJavaReferenceType(outRt);
                var typeUtil = compiler.getTypeUtil();
                return JavaExpression.wrappedInMapperCOfSingle(invocation,
                        typeUtil.wrap(typeUtil.MAPPER_C, elem), elem);
            }
        }
        return JavaExpression.wrappedInMapperSOf(invocation);
    }

    /**
     * Renders a bare RULE reference used in operand position (function argument,
     * exists/comparison operand, conditional branch — anywhere a bare no-arg
     * {@link RSymbolReference} resolving to an {@link RRule} is compiled OUTSIDE a
     * then-body) as an invocation of the injected rule instance, wrapped in
     * {@code MapperS.of}: {@code MapperS.of(<ruleField>.evaluate(<binding>.get()))}.
     *
     * <p>The {@code MapperS.of} wrap (symmetric to the variable path's wrap, NOT the
     * UNWRAPPED bare-FUNCTION form) lets the value flow correctly through BOTH
     * contexts the corpus exercises: as an {@code evaluate()} argument the wrap is
     * stripped structurally by {@link #unwrapForEvaluateArg} (golden
     * {@code priceFormatFromNotation.evaluate(strikePriceNoFormatRule.evaluate(item.get()), …)}),
     * and as a general operand the wrap is retained (golden
     * {@code exists(MapperS.of(originalSwapSDRIdentifierRule.evaluate(item.get())))}).
     * The bare-FUNCTION sibling ({@link #renderImplicitFunctionInvocation}) stays
     * UNWRAPPED because its sites are filter/predicate boolean contexts.
     *
     * <p>The implicit-input argument depends on position:
     * <ul>
     *   <li><b>Inside an extract/filter/then lambda</b> (nearestEnclosingInlineFunction
     *       != null) — the lambda binding, via a synthetic {@link RImplicitVariable}
     *       ({@code item.get()} / {@code thenArg.get()}). UNCHANGED from PR #100.</li>
     *   <li><b>At rule-body top level</b> — the rule's {@code input} parameter, via the
     *       synthetic {@code RSymbolReference("input")} typed as the enclosing rule's
     *       from-type that {@link #buildImplicitInputReceiver} also builds for
     *       implicit-input navigations. {@code item} would be undefined here. The
     *       caller's gate guarantees the from-type is present.</li>
     * </ul>
     * Both render through {@link #unwrapForEvaluateArg} so the {@code MapperS.of(...)}
     * the variable/item paths emit is stripped to the raw evaluate argument
     * ({@code input} / {@code item.get()}).
     *
     * @param enclosingRule the rule whose body contains {@code expr} (resolved once by
     *                      the caller's gate); its from-type types the top-level
     *                      synthetic {@code input} receiver.
     */
    // public (was private) for the IR route's in-lambda rule-delegation reuse (the lab's L-049):
    // the IR-side RuleDelegationRenderer reuses this oracle VERBATIM to render an in-lambda
    // bare-rule reference, so the polymorphic item/thenArg receiver binding is never re-derived
    // IR-side. Reached only with nearestEnclosingInlineFunction(expr) != null (the top-level
    // `input` form renders on the IR's own L-045 path). The D43 seam.
    public JavaStatementBuilder renderImplicitRuleInvocation(
            RSymbolReference expr, RRule rule, RRule enclosingRule,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        JavaStatementBuilder argBuilder;
        RImplicitVariable item = null;
        JavaStatementBuilder itemCompiled = null;
        if (nearestEnclosingInlineFunction(expr) != null) {
            // In an extract/filter/then lambda — pipe in the lambda binding.
            item = new RImplicitVariable();
            item.setSynthetic(true);
            item.setParent(expr.parent());
            itemCompiled = handle(item, ctx, compiler);
            argBuilder = unwrapForEvaluateArg(itemCompiled);
        } else {
            // Rule-body top level — invoke on the rule `input` parameter (NOT `item`,
            // which is undefined outside a lambda). buildImplicitInputReceiver returns
            // the synthetic RSymbolReference("input") typed as the from-type (its
            // hasEnclosingRuleLambda check is false here, so it takes the input
            // branch); compiling it yields MapperS.of(input), unwrapped to `input`.
            RExpression inputRef =
                    buildImplicitInputReceiver(expr, enclosingRule.fromType().orElseThrow());
            argBuilder = unwrapForEvaluateArg(
                    compiler.compile(inputRef, ctx.expectedType(), ctx.scope()));
        }
        if (!(argBuilder instanceof JavaExpression argExpr)) {
            throw new IllegalStateException(
                    "Implicit rule-call argument unwrapped to non-expression type: "
                    + argBuilder.getClass().getSimpleName());
        }
        // facet ruleCalleeMetaDeref (PR #336): the piped item is a META wrapper (`item.get()` =
        // ReferenceWithMetaNonNegativeQuantitySchedule) but the invoked reporting RULE's `from`
        // VALUE type is expected (`reporting rule QuantityUnitOfMeasure from
        // NonNegativeQuantitySchedule`), so the bare `item.get()` arg was non-compiling. Mirror the
        // FUNCTION sibling renderImplicitFunctionInvocation's PR #144 valuation_currency_metabox_deref:
        // hoist `final <Wrapper> <var> = item.get();` + pass the null-guarded `<var>.getValue()`
        // deref, reusing tryMetaDerefArg with the rule's synthetic RFunction (RFunction.fromRule —
        // the SAME single `from`-typed input the RuleGenerator emits the rule's evaluate(…) signature
        // from). The piped wrapper type comes from enclosingThenArgType (the declared thenArg element
        // type). BLOCK returns the JavaBlockBuilder (lambda-body-IS-the-call / then-body lift);
        // LAMBDA_CHANNEL registers the decl on the lambda scope so CollectionHandler.compileLambda
        // drains it into the block body (QuantityUnitOfMeasureLeg2 — the rule call is a NESTED operand
        // inside the `.mapSingleToItem(item -> …getOrDefault("OTHR"))` lambda). Green-safe by
        // construction: a bare wrapper into a value-typed rule input never compiled (no green carrier);
        // a rule whose `from` IS the wrapper type yields a coercion identity → null → no deref.
        MetaDerefHoistRoute ruleRoute = metaDerefHoistRoute(expr);
        JavaType pipedRuleType = enclosingThenArgType(expr, ctx);
        // facet bareRuleRefFunctionHost (v3.1 flip seat 33, law B.24 - the extract-base rung,
        // measured at QuantityUnitOfMeasure:78): when the callee IS the body root of an extract
        // lambda that carries no #350 binding of its own (a chain BASE `... -> quantity extract
        // <rule>`, not a `then extract` step), enclosingThenArgType walks PAST it to an OUTER
        // lambda's binding and reports that lambda's item (measured: MapperS<NonNegative-
        // QuantitySchedule>, the enclosing then-body's bare thenArg), so the callee saw a bare
        // item and kept the raw `item.get()` where golden hoists the extract's OWN wrapper item
        // and derefs. The piped type is dropped for that shape and tryMetaDerefArg's
        // function-item recovery (implicitItemArgMeta on the extract receiver's leaf) supplies
        // the wrapper - the same recovery the rule path already uses.
        RInlineFunction ruleInline = nearestEnclosingInlineFunction(expr);
        // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — VERDICT-MOVED
        // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar; S33): THE SEATMAP'S CHEAPEST
        // CANDIDATE IS THE CENSUS'S BIGGEST REFUTATION. c9 census, 40,126 arrivals (14,347 / 11,432
        // / 14,347) with parentIsExtract=true at 20,904 (the answerable population): the proposed
        // two-hop AST base/step test agrees at 9,426/20,904 (45.09%), T/T 1,905, T/F 10,101, F/T
        // 1,377. BOTH disagreement classes are large and fatal — the 10,101 are arrivals where this
        // seat drops the piped type while the AST test says "this IS a then step" and would keep
        // it, ten thousand byte-relevant decisions reversed; the 1,377 are where the AST test says
        // "base" and the seat declines because the body is not the callee. LAW 77: the -2,915 route
        // asymmetry is EXACTLY the unanswerable population and the ANSWERABLE 2x2 is IDENTICAL on
        // OFF, ON and OPT alike (635 / 3,367 / 459 / 2,507) — the cleanest route-identity proof in
        // the family. S33 and S35 are ONE LAW: this seat detects exactly the case where
        // enclosingThenArgType's walk-through gives the wrong answer and nulls the result, so with
        // the replacement refuted at 45.09% the PAIR cannot move.
        boolean extractBaseCallee = ruleInline != null
                && ruleInline.parent() instanceof RExtractExpr
                && ruleInline.body() == expr
                && ctx.scope() != null && ctx.scope().thenArgRefFor(ruleInline) == null;
        if (extractBaseCallee) {
            pipedRuleType = null;
        }
        if ((ruleRoute == MetaDerefHoistRoute.BLOCK || ruleRoute == MetaDerefHoistRoute.LAMBDA_CHANNEL)
                && item != null && (pipedRuleType != null || extractBaseCallee)
                && rule.fromType().isPresent()
                && itemCompiled instanceof JavaExpression ruleItemExpr) {
            // The rule's synthetic RFunction (RFunction.fromRule) deep-copies the `from` typeCall,
            // and RTypeCall.deepCopy DELIBERATELY drops the referencedTypeId resolution state
            // (back-filled only at codegen time by RuleGenerator), so getType() on the ad-hoc
            // synthetic input returns MISSING and tryMetaDerefArg's param-type gate would decline.
            // Restore the ORIGINAL rule.fromType() typeCall (which carries the resolvable
            // referencedTypeId) onto the throwaway callee's single input so the rule's `from` VALUE
            // type resolves — the coercion target for the meta-wrapper item.
            RFunction ruleCallee = RFunction.fromRule(rule);
            ruleCallee.inputs().get(0).setTypeCall(rule.fromType().orElseThrow());
            JavaExpression typedRuleItem = JavaExpression.from(
                    ruleItemExpr.renderToString(), pipedRuleType,
                    ruleItemExpr.getRefs(), ruleItemExpr.getStaticWildcardImports());
            List<JavaStatement> ruleHoists = new ArrayList<>();
            JavaExpression ruleDerefArg = tryMetaDerefArg(
                    item, typedRuleItem, argExpr, ruleCallee, 0, ctx, compiler,
                    ruleHoists, null, extractBaseCallee);
            if (ruleDerefArg != null && !ruleHoists.isEmpty()) {
                String ruleReceiver = ruleInvocationReceiver(rule, compiler);
                JavaExpression ruleInner = JavaExpression.from(
                        ruleReceiver + ".evaluate(" + ruleDerefArg.renderToString() + ")",
                        null, ruleDerefArg.getRefs(), ruleDerefArg.getStaticWildcardImports());
                JavaStatementBuilder ruleWrapped = JavaExpression.wrappedInMapperSOf(ruleInner);
                if (ruleWrapped instanceof JavaExpression ruleWrappedExpr) {
                    if (ruleRoute == MetaDerefHoistRoute.LAMBDA_CHANNEL) {
                        for (JavaStatement h : ruleHoists) {
                            ctx.scope().registerPendingLambdaHoist(h);
                        }
                        return ruleWrapped;
                    }
                    return new JavaBlockBuilder(JavaStatementList.of(ruleHoists), ruleWrappedExpr);
                }
            }
        }
        String receiver = ruleInvocationReceiver(rule, compiler);
        JavaExpression innerCall = JavaExpression.from(
                receiver + ".evaluate(" + argExpr.renderToString() + ")",
                null,
                argExpr.getRefs(),
                argExpr.getStaticWildcardImports());
        // facet existsOperandMetaWrap (PR #315): a bare inner-rule invocation used as the OPERAND
        // of an exists/notExists whose rosetta OUTPUT is META-typed ([metadata scheme/reference])
        // but whose Java evaluate() returns the BARE value. Golden coerces the value INTO the meta
        // wrapper via the upstream convertNullSafe null-safe ternary and DISTRIBUTES the existence
        // check (+ any downstream getOrDefault/asMapper) into both branches:
        //   final PriceSchedule priceSchedule = <rule>.evaluate(item.get());
        //   (priceSchedule == null ? exists(MapperS.<ReferenceWithMetaPriceSchedule>ofNull())
        //        : exists(MapperS.of(ReferenceWithMetaPriceSchedule.builder().setValue(priceSchedule).build())))
        // The fork emitted the bare `exists(MapperS.of(<rule>.evaluate(item.get())))` — a
        // MapperS<PriceSchedule>, no meta round-trip. Return the meta-wrap as a LIVE
        // JavaConditionalExpression (NOT the #265 flat string) so ExistenceHandler's mapExpression
        // distributes exists over both branches, and the downstream coercers (WrapperToItemCoercer
        // getOrDefault / WrapperToWrapperCoercer asMapper — which apply via mapExpression) distribute
        // too. Same meta-recovery + value-type gate + LAMBDA_CHANNEL value hoist as the #265
        // lambda-terminal arm; DISJOINT gate (exists-operand parent, not extract-lambda terminal).
        // Green-safe by construction: the bare exists(MapperS.of(<call>)) compiles + behaves
        // identically (a semantic round-trip), so every carrier is a COMPILES_DIVERGENT byte
        // mismatch — a green file cannot carry the distributed wrap form. Gated on the exists
        // sitting inside a map/extract lambda (eowInMapLambda — the walk allows an intervening
        // conditional, unlike isInsideDrainableMapLambda's #312 arm exclusion) so the value hoist
        // drains into the enclosing block form (compileLadderConditionalBlock / compileNested-
        // ConditionalBlock drain it at the block top). A co-occupied carrier whose block form
        // DECLINES suppresses the wrap via isExistsMetaWrapSuppressed (compileLambda's fall-through),
        // so the fork stays at its clean pre-#315 inline ternary. The STATEMENT_SINK exists-operand
        // shape (UPI Proprietary — a rule-body ite-hoist condition) is a follow-on.
        // Fire when the exists is a ladder/nested-conditional RUNG condition — an intervening
        // RConditionalExpr between the exists and the map/extract lambda. Those seats are the ones the
        // block renderers handle (compileLadderConditionalBlock distributes the getOrDefault + drains
        // the value hoist at the block top).
        // facet existsMetaSeats (PR #331) — the two #315-deferred seats now fire too:
        //  (a) a WHOLE-map-body exists (NO intervening conditional — the
        //      `item -> exists(…).asMapper()` CustomBasketIndicator shape): compileLambda now
        //      distributes the `.asMapper()` coercion into both branches via mapExpression and
        //      block-renders the drained value hoist + bare-ternary return (its new
        //      JavaConditionalExpression arms), so the two documented #315 orphan concerns are
        //      closed rather than escaped;
        //  (b) the STATEMENT seat (UPIProprietary hkma — a rule-body ite-hoist condition, no
        //      enclosing lambda): the value decl registers on the statement-hoist sink as a
        //      sentinel-bearing STRING (the #237 tryMetaDerefArg pattern — the decl OBJECT's
        //      render() would getActualName()-close the scope mid-compilation);
        //      FunctionExpressionRenderer.prependStatementHoists lifts it ahead of the ite
        //      statement (golden's `final String string = …;` before the ifThenElseResult decl);
        //      LogicalHandler collapses the distributed conditional to its parenthesized form
        //      when `.andNullSafe(…)` chains on it.
        // Both seats were deferred by DESIGN at #315, not by the incidental
        // recoverInnerRuleMetaWrapper-returns-null they escaped on then; the #330 CP5 recovery
        // extension (extract-descent + disguised-chain leaf) resolves their inner rules now.
        // facet ruleMetaLiftResidueSeats (seat 6): the #315/#331 block above is extracted
        // verbatim into tryExistsOperandMetaWrap so the WITH-ARGS rule-call seat (the
        // `Spread(PayoutLeg2) exists` SpreadOfLeg carriers — a seat the #315 arms never
        // reached) shares the SAME gates + recovery + conditional build. This no-args
        // seat's behavior is unchanged (pure extraction).
        JavaStatementBuilder eowWrap = tryExistsOperandMetaWrap(expr, rule, innerCall, ctx, compiler);
        if (eowWrap != null) {
            return eowWrap;
        }
        // facet ruleThenValueMetaWrap LAMBDA seat (PR #265): the inner-rule meta-wrap at the
        // `.mapSingleToItem(item -> <rule>)` extract-lambda seat (the deeper half of the #264
        // top-level family). When the invoked rule's rosetta OUTPUT is META-typed
        // ([metadata scheme] …) but its Java evaluate() returns the BARE value, golden wraps the
        // value INSIDE the lambda and dereferences at the output (the renderThenExtractSet deref):
        //   item -> { final String string = <rule>.evaluate(item.get());
        //             return string == null ? MapperS.<FieldWithMetaString>ofNull()
        //                  : MapperS.of(FieldWithMetaString.builder().setValue(string).build()); }
        // The fork emitted the bare `item -> MapperS.of(<rule>.evaluate(item.get()))` (a
        // MapperS<String>, which the output deref cannot fire on). Hoist the bare value into a
        // lambda-channel local (CollectionHandler.compileLambda drains it into the brace-block
        // body, the #171 B3 mechanism) and return the meta-wrap expression typed
        // MapperS<FieldWithMetaString>; FunctionExpressionRenderer.renderThenExtractSet then emits
        // the matching output deref (coordinated on the SAME recoverInnerRuleMetaWrapper + terminal
        // gate). Green-safe by construction: the bare MapperS.of(<call>) compiles + behaves
        // identically (a semantic round-trip), so every carrier is a byte-divergent
        // COMPILES_DIVERGENT mismatch — a green file cannot carry the golden wrap form.
        //
        // GATED tightly (the shared renderImplicitRuleInvocation seat serves functions too):
        //  (1) the rule reference is the TERMINAL body of an EXTRACT/MAP lambda — `extract` and `map`
        //      BOTH parse to a single RExtractExpr node (the IR unifies them), so the gate's
        //      `inlineFn.parent() instanceof RExtractExpr` covers both; expr.parent() is the inline
        //      function whose parent is that RExtractExpr (NOT navigated further onto an RFeatureCall
        //      receiver, and NOT a filter-predicate boolean lambda);
        //  (2) the inner rule's recovered meta wrapper's VALUE type equals the rule's bare Java
        //      output (the meta is a leaf [metadata scheme], not a wrapper navigated through).
        // NOTE — the LOAD-BEARING coordination with the output deref (the
        // renderThenExtractSet `!multiOutput`-gated lambdaSeatRuleMetaWrapper) is the meta-recovery
        // gate, NOT receiver cardinality: this seat has no multi-receiver guard, so it would wrap
        // INSIDE a multi `mapItem` lambda too — but the corpus has NO multi-receiver bare-meta-rule
        // extract whose inner rule recovers a nav-leaf meta wrapper (the multi cases —
        // QuantityUnitOfMeasureLeg1/2 — navigate to a CONDITIONAL terminal, so
        // recoverInnerRuleMetaWrapper returns null and the wrap declines). A hypothetical multi
        // carrier with a recoverable nav-leaf meta inner rule would wrap-without-deref (change 3
        // declines on multiOutput) → non-compiling, hence WAIVERED — not a green regression (a green
        // file cannot carry the wrap form); add a receiver-cardinality gate here if one ever appears.
        // facet existsMetaSeats (PR #331): the #265 wrap ALSO fires when the rule reference is the
        // THEN-arm of a conditional that is ITSELF the direct extract-lambda body
        // (`extract [if <cond> then <metaRule>]` — csa UnderlyingAssetTradingPlatformIdentifierLeg1/2):
        // golden hoists the value INSIDE the if-branch + returns the same null-safe wrap ternary,
        // and the elseless block's typed-empty re-types to the wrapper via the existing #144
        // arm-type preference. The hoist registers as the MetaWrapValueHoist marker so
        // CollectionHandler.compileElselessConditionalBlock's in-branch drain admits it (the #301
        // numeric-only filter otherwise DECLINES the block and the ternary would regress the
        // pre-#331 block form). ELSE-arm / deeper-nested positions stay declined (no carrier).
        boolean lambdaTerminalSeat = isLambdaTerminalSeat(expr);
        // facet ruleCallArmMetaWrap (seat 25, law B): the #265/#331/#372 SINGLE meta-wrap
        // block extracted VERBATIM to trySingleRuleMetaWrap (its javadoc carries the seat
        // history) so the WITH-ARGS rule-call seat shares it — this no-args consult carries
        // the extraction's THREE deltas (the review's B-3 count): the widened
        // conditional-arm walk (then- AND else-edges), the with-args share, and the
        // sentinel-rendering conditional-arm hoist.
        JavaStatementBuilder singleMetaWrap = trySingleRuleMetaWrap(expr, rule, innerCall, ctx, compiler, false);
        if (singleMetaWrap != null) {
            return singleMetaWrap;
        }
        // facet bodyMultiCardinality (PR #274): the extract-LAMBDA-seat completion of
        // PR #273's report-output cardinality — renderBareInvokableThenSet's ruleMultiThen
        // arm, one seat down. When the invoked sub-rule's OWN body is rule-aware MULTI
        // (its evaluate() returns a List), golden wraps the invocation
        // {@code MapperC.<Elem>of(…)} not {@code MapperS.of(…)} — the SINGLE wrap assigns a
        // List to a MapperS<Elem> (non-compiling). Coordinates with
        // CollectionHandler.mapMethod's rule-aware isBodyMulti (which already selects
        // {@code mapSingleToList} over {@code mapSingleToItem} on the SAME
        // getRuleBodyCardinality signal), so a multi {@code then extract <sub-rule>} body
        // renders golden's
        // {@code mapSingleToList(item -> MapperC.<Elem>of(<rule>.evaluate(item.get())))}
        // (OtherPayment, NotionalAmountScheduleLeg1/2, DTCC_OtherPayment*,
        // BasketConstituents). RULE-scoped (enclosingRule != null, guaranteed by the
        // caller's findEnclosingRule gate) and FUNCTION-byte-neutral by construction (a
        // reporting rule is only referenced from a rule/report by grammar, and
        // getRuleBodyCardinality's RRule recursion is thenAware/rule-output gated).
        // Green-safe: golden never emits {@code MapperS.of(List)} for a multi sub-rule (it
        // always MapperC-wraps), so every carrier was already a waivered non-compiling
        // mismatch; a SINGLE sub-rule keeps the {@code MapperS.of} wrap. The
        // wrappedInMapperCOfSingle factory mirrors the MapperS wrap's structural-unwrap
        // contract, so an evaluate-arg / assignment consumption strips it identically.
        // PRECEDENCE: this arm sits AFTER the #265 single-meta-wrap arm above (which
        // returns early for a recoverable nav-leaf meta inner rule) and BEFORE the final
        // {@link JavaExpression#wrappedInMapperSOf}. A hypothetical multi+meta sub-rule (a
        // recoverable nav-leaf meta AND a MULTI body) would take the #265 arm first and
        // emit a single {@code MapperS<FieldWithMeta…>} for a List — impossible in the
        // corpus today (recoverInnerRuleMetaWrapper returns null for the multi /
        // conditional-terminal cases), but if one ever appears the #265 arm should decline
        // on {@code getRuleBodyCardinality == MULTI} so it falls through to this MapperC wrap.
        if (compiler.getGeneratorModel() != null && compiler.getTypeTranslator() != null
                && compiler.getTypeUtil() != null
                // seat 8: re-pointed to the shared ruleOutputProvesMulti predicate
                // (engine-first — byte-neutral wherever the overlay is silent; the
                // csa/cftc DTCC_ProductGrade delegation cascades reach this arm with
                // an engine-SINGLE read the choice-option overlay proves MULTI).
                && NavigationHandler.ruleOutputProvesMulti(expr, compiler)) {
            // facet ruleCallMapperCWitness (seat 25, law C): the element witness reads the
            // CALLEE's inferred output when the reference node's own inference is MISSING —
            // the iosco UnderlierIDOther/Source carriers' `UnderlierProductIdentifierOther`
            // reference infers MISSING → Object → `MapperC.<Object>of(…)` with the
            // AssetIdentifier import dropped, a form golden never carries (the seat-25
            // golden census: `MapperC.<Object>of` / `MapperS.<Object>of` are ZERO over all
            // 25 cells). HandlerHelper.ruleInferredOutputRType is the ONE rule-output read
            // (#593); a null callee read (its body MISSING too) falls back to the node
            // read — today's bytes, the decline polarity.
            RMetaAnnotatedType exprInferred = compiler.getGeneratorModel().workspace()
                    .getInferredType(expr);
            RType exprElemType = exprInferred == null || exprInferred.isMissing()
                    ? HandlerHelper.ruleInferredOutputRType(rule, compiler.getGeneratorModel())
                    : exprInferred.type();
            JavaClass<?> outElem = compiler.getTypeTranslator().toJavaReferenceType(
                    exprElemType != null ? exprElemType
                            : compiler.getGeneratorModel().workspace().getInferredType(expr).type());
            // facet ruleMetaListWrap (PR #360): the multi+meta composition the #274 PRECEDENCE
            // note anticipated — a MULTI inner rule whose rosetta OUTPUT is META-typed (the
            // (a2)-recovered nav-leaf wrapper; its Java evaluate() returns the BARE List)
            // re-presents the MODEL type by wrapping EACH element into the meta builder
            // (upstream's list convertNullSafe — stream/map/collect), typed
            // MapperC<FieldWithMetaX> so the whole-output multi-element deref
            // (renderThenExtractSet's `.map("Type coercion", …).getMulti()` tail) fires on
            // the same recovered wrapper — the coordinated pair, exactly like #265's
            // single-seat wrap + output deref (drr cftc/csa dtcc DTCC_TradeParty1/2
            // ReportingDestinationRule). The continuations ride the relative CHAIN_LINK
            // `\n\t` convention (the closer at `\n` base); the consuming seat re-anchors.
            // A meta whose VALUE type differs from the rule's bare element declines (a
            // wrapper navigated through, not the leaf).
            // facet ruleCallMetaRewrap (seat 25, law A): the #360 wrap ALSO fires at a
            // NON-lambda seat when the invocation is a rule-body STAGE — a bare then-stage
            // (asic/mas `then CDECallCurrency`, cftc's chain-head receiver, cftc/jfsa
            // `then CDEPackageIdentifier`) or a conditional arm at the then-chain level
            // (jfsa `then if IsFXOption(…) then CDECallCurrency` — the ite-hoist arm, whose
            // ifThenElseResult decl + typed-empty tail re-type through the #330 arms-agree
            // join). The stage's consumers follow off the wrapper type by existing machinery:
            // the next extract stage's arg deref (tryMetaDerefArg arm (a)) and the
            // whole-output deref tail (the F1 recovery ladder). The ONLY multi+meta
            // population at this seat beyond these stages is the DIRECT whole-body delegation
            // (`output = <rule>.evaluate(input);` — expr IS the enclosing rule's body root:
            // the iosco basket v2→v1 alias chains), which stays on the strippable
            // wrappedInMapperCOfSingle path, which the whole-output SET strips back to the
            // bare green form; wrapping there regressed the v2/v1 green files (the cp2b D11
            // catch — 2 NEW mismatches, reverted same-checkpoint). Measured (LAW 75, the
            // seat-25 probe over all 275 matrix rows, BOTH routes): the multi+meta non-lambda
            // lines at this seat are EXACTLY the three carrier classes (CallCurrency/
            // PutCurrency ×5 each, PackageIdentifier ×2) and the two basket body-root
            // delegation classes (×5 each) — no other shape reaches here.
            RJavaWithMetaValue listMeta = lambdaTerminalSeat || ruleBodyNonRootSeat(expr)
                    ? NavigationHandler.recoverInnerRuleMetaWrapper(rule, compiler)
                    : null;
            if (listMeta != null && outElem != null
                    && outElem.equals(listMeta.getValueType())) {
                String wrapperSimple = listMeta.getSimpleName();
                String valueParam = JavaNamingUtil.toFirstLower(outElem.getSimpleName());
                Set<JavaClass<?>> wrapRefs = new HashSet<>(innerCall.getRefs());
                wrapRefs.add(HandlerHelper.MAPPER_C);
                wrapRefs.add(listMeta);
                wrapRefs.add(outElem);
                wrapRefs.add(HandlerHelper.COLLECTORS);
                String wrap = "MapperC.<" + wrapperSimple + ">of("
                        + innerCall.renderToString() + ".stream()"
                        + "\n\t.<" + wrapperSimple + ">map(" + valueParam + " -> "
                        + wrapperSimple + ".builder().setValue(" + valueParam + ").build())"
                        + "\n\t.collect(Collectors.toList())"
                        + "\n)";
                return JavaExpression.from(wrap,
                        compiler.getTypeUtil().wrap(compiler.getTypeUtil().MAPPER_C, listMeta),
                        wrapRefs, innerCall.getStaticWildcardImports());
            }
            JavaType wrapperType = compiler.getTypeUtil()
                    .wrap(compiler.getTypeUtil().MAPPER_C, outElem);
            return JavaExpression.wrappedInMapperCOfSingle(innerCall, wrapperType, outElem);
        }
        return JavaExpression.wrappedInMapperSOf(innerCall);
    }

    /**
     * facet ruleCallArmMetaWrap (seat 25, law B): the #265/#331/#372 SINGLE inner-rule
     * meta-wrap, extracted VERBATIM from {@link #renderImplicitRuleInvocation} so the
     * WITH-ARGS rule-call seat shares it (LAW 69 — seat 6's {@code tryExistsOperandMetaWrap}
     * precedent; the NotionalCurrencyOfLeg2 arm calls
     * {@code MapperS.of(cDEInterestRateNotionalCurrencyRule.evaluate(<argChain>))} never
     * reach the no-args seat — the seat-25 LAW-75 probe measured ZERO P25A lines for them,
     * they live at the {@code :1503} innerCall block). Two law-B widenings ride the
     * extraction:
     * <ul>
     *   <li><b>the conditional-arm WALK</b> follows BOTH then- and else-edges to the
     *       OUTERMOST conditional and admits the reference at EITHER branch of its
     *       immediate parent — the csa {@code UnderlyingAssetTradingPlatformIdentifier-
     *       Leg1/2} carriers sit at a then-edge-NESTED then ({@code if outer { if aligned
     *       then Leg1 …}}) and at the ELSE position ({@code … return Leg2}), both
     *       previously declined ({@code thenBranch() == expr} + else-edges-only —
     *       PR #372's walk). The #330 all-present-arms-agree JOIN on the outermost
     *       conditional still gates: a bare-joined ladder declines (hkma UATPI stays
     *       green), an elseless meta arm keeps its wrapper (recoverExprMetaWrapper's
     *       case (d));</li>
     *   <li><b>the with-args consult</b> — hoist-free calls only (a hoisting arg keeps
     *       the existing block route, today's bytes); the callee's meta is recovered from
     *       the SAME body walk (case (c) admits the with-args call — the args select the
     *       callee's INPUT, never its output meta).</li>
     * </ul>
     * The pre-extraction seat history, verbatim: facet condRungRuleValueMetaWrap (PR #372,
     * F-gamma-B) widened the #331 conditional-arm seat from the SINGLE outer conditional to
     * any RUNG of a nested else-if LADDER (golden asic CollateralPortfolioCodeVariationMargin:
     * rung 2's bare {@code cde.collateral.CollateralPortfolioCode} wraps in-branch while rung
     * 1 is the collapse-terminated nav pipe — the join agrees on FieldWithMetaString); a
     * mixed/bare-joined ladder still declines (null walk). facet ruleMetaListWrap (PR #360):
     * a MULTI sub-rule body DECLINES this single MapperS wrap and falls through to the #274
     * MapperC arm (seat 8: the shared {@code ruleOutputProvesMulti} predicate — this decline
     * and the MapperC arm read the SAME multi verdict the signature back-fill does).
     * Returns the wrap expression (typed {@code MapperS<FieldWithMetaX>}; the value hoist
     * registered on the lambda channel — the {@code MetaWrapValueHoist} in-branch drain for
     * a conditional arm) or {@code null} — the decline polarity.
     *
     * @param armSeatsOnly the WITH-ARGS caller passes {@code true}: its witnessed class is
     *        the CONDITIONAL-ARM seat only (the NotionalCurrencyOfLeg2 arms); a with-args
     *        single+meta call at the lambda-TERMINAL seat is corpus-unwitnessed in a rule
     *        context (the fn-context calls decline at the equality gate — their inferred
     *        type is MISSING) and stays on today's bare form — a stated decline, not a gate
     */
    private static JavaStatementBuilder trySingleRuleMetaWrap(RSymbolReference expr, RRule rule,
            JavaExpression innerCall, ExpressionContext ctx, ExpressionCompiler compiler,
            boolean armSeatsOnly) {
        boolean lambdaTerminalSeat = !armSeatsOnly && isLambdaTerminalSeat(expr);
        boolean conditionalArmSeat = false;
        if (expr.parent() instanceof RConditionalExpr armCond
                && (armCond.thenBranch() == expr
                        || armCond.elseBranch().filter(e -> e == expr).isPresent())) {
            RConditionalExpr outermost = armCond;
            while (true) {
                RConditionalExpr rung = outermost;
                if (outermost.parent() instanceof RConditionalExpr outerCond
                        && (outerCond.thenBranch() == rung
                                || outerCond.elseBranch().filter(e -> e == rung).isPresent())) {
                    outermost = outerCond;
                } else {
                    break;
                }
            }
            conditionalArmSeat = outermost.parent() instanceof RInlineFunction armInline
                    && armInline.parent() instanceof RExtractExpr
                    && NavigationHandler.recoverExprMetaWrapper(outermost, compiler) != null;
        }
        boolean multiRuleBody = compiler.getGeneratorModel() != null
                && NavigationHandler.ruleOutputProvesMulti(expr, compiler);
        if (compiler.getTypeUtil() != null && compiler.getTypeTranslator() != null
                && compiler.getGeneratorModel() != null && !multiRuleBody
                && (lambdaTerminalSeat || conditionalArmSeat)) {
            RJavaWithMetaValue meta = NavigationHandler.recoverInnerRuleMetaWrapper(rule, compiler);
            JavaType ruleBare = meta == null ? null : compiler.getTypeTranslator()
                    .toJavaReferenceType(compiler.getGeneratorModel().workspace()
                            .getInferredType(expr).type());
            if (meta != null && ruleBare != null && ruleBare.equals(meta.getValueType())) {
                String metaSimple = meta.getSimpleName();
                JavaType valueType = meta.getValueType();
                String valueSimple = valueType.getSimpleName();
                GeneratedIdentifier valueId = ctx.scope()
                        .createUniqueIdentifier(JavaNamingUtil.toFirstLower(valueSimple));
                String nameToken = ctx.scope().registerDeferredCoercionName(valueId);
                // law B: the conditional-arm hoist renders the deferred SENTINEL, never the
                // identifier — a two-arm ladder's first drain would otherwise close the
                // lambda scope before the second arm's createUniqueIdentifier (the #346
                // pattern; the MetaWrapValueHoist token constructor's javadoc).
                ctx.scope().registerPendingLambdaHoist(conditionalArmSeat
                        ? new MetaWrapValueHoist(valueType, valueId, innerCall, nameToken)
                        : new JavaLocalVariableDeclarationStatement(
                                true, valueType, valueId, innerCall));
                Set<JavaClass<?>> wrapRefs = new HashSet<>(innerCall.getRefs());
                wrapRefs.add(HandlerHelper.MAPPER_S);
                wrapRefs.add(meta);
                if (valueType instanceof JavaClass<?> valueClass) {
                    wrapRefs.add(valueClass);
                }
                String wrap = nameToken + " == null ? MapperS.<" + metaSimple + ">ofNull() : MapperS.of("
                        + metaSimple + ".builder().setValue(" + nameToken + ").build())";
                return JavaExpression.from(wrap,
                        compiler.getTypeUtil().wrap(compiler.getTypeUtil().MAPPER_S, meta),
                        wrapRefs, innerCall.getStaticWildcardImports());
            }
        }
        return null;
    }

    /**
     * The #265/#331 lambda-terminal gate — the rule reference is the TERMINAL body of an
     * EXTRACT/MAP lambda. Extracted (the review's B-9) so the #360 arm's seat test and
     * {@code trySingleRuleMetaWrap} consult ONE predicate instead of carrying two copies.
     */
    private static boolean isLambdaTerminalSeat(RSymbolReference expr) {
        return expr.parent() instanceof RInlineFunction inlineFn
                && inlineFn.parent() instanceof RExtractExpr;
    }

    /**
     * facet ruleCallMetaRewrap (seat 25, law A): the no-args rule invocation sits INSIDE
     * the enclosing REPORTING RULE's body but is NOT its body root (and not under a
     * constructor). The name states what the code TESTS (the review's B-8): there is no
     * positional 'then stage' check here — the narrowing to the measured carrier lines
     * comes from the CALL SITE's own gates (the #360 arm's multi+meta recovery + the
     * lambdaTerminal split). A non-root reference's result feeds a downstream consumer
     * (the next then stage, an ite-hoist arm join, or the whole-output deref), so golden
     * re-presents the MODEL type there; the body-root reference is the whole-body
     * delegation class (`output = <rule>.evaluate(input);`), which the whole-output SET
     * strips back to the bare green form — the iosco basket v2→v1 chains (see the
     * in-seat comment at the #360 arm for the measured population).
     */
    private static boolean ruleBodyNonRootSeat(RSymbolReference expr) {
        RRule enclosing = HandlerHelper.findEnclosingRule(expr);
        if (enclosing == null
                || enclosing.expression().filter(body -> body != expr).isEmpty()) {
            return false;
        }
        // law D coordination: a rule reference under ANY constructor ancestor (within the
        // parent-walk limit — broader than the direct ctor VALUE position, deliberately:
        // the decline direction can only under-fire) belongs to the ctor seat
        // (ConstructionHandler's ruleOutputProvesMulti cardinality authority + the
        // arm-C2r bare-splice/wrap classification) — a stream re-wrap spliced into a
        // setter is corpus-unwitnessed (the seat-25 probe's multi+meta population has
        // no ctor-value line), so the reference declines here and the ctor seat's own
        // laws decide (found by the law-D fixture's meta-rule ctor value; never
        // weakened — the decline is the fix).
        RNode c = expr.parent();
        int d = 0;
        while (c != null && d++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (c instanceof RConstructorExpr) {
                return false;
            }
            if (c instanceof RFunction || c instanceof RRule || c instanceof ROperation
                    || c instanceof RInlineFunction) {
                break;
            }
            c = c.parent();
        }
        return true;
    }

    /**
     * facet ruleMetaLiftResidueSeats (seat 6): the #315 {@code existsOperandMetaWrap} +
     * #331 {@code existsMetaSeats} exists-operand meta-wrap, extracted VERBATIM from
     * {@link #renderImplicitRuleInvocation} (whose in-place comment block documents the
     * mechanism, the seats and the green-safety) so the WITH-ARGS rule-call seat shares
     * it. Returns the live {@link JavaConditionalExpression} when the wrap fires, else
     * {@code null} (the caller keeps its existing render — the decline polarity).
     *
     * @param expr the rule-reference (no-args seat) or rule-call (with-args seat) node —
     *        the exists-operand parent walk + the inferred bare type read anchor on it
     * @param rule the invoked reporting rule (the recovery subject)
     * @param innerCall the rendered bare invocation ({@code <recv>.evaluate(…)})
     */
    private static JavaStatementBuilder tryExistsOperandMetaWrap(RSymbolReference expr, RRule rule,
            JavaExpression innerCall, ExpressionContext ctx, ExpressionCompiler compiler) {
        boolean eowInMapLambda = false;
        boolean eowStatementSeat = false;
        {
            RNode c = expr.parent();
            RNode existsNode = expr.parent();
            int d = 0;
            boolean sawConditional = false;
            while (c != null && d++ < HandlerHelper.PARENT_WALK_LIMIT) {
                if (c instanceof RConditionalExpr) {
                    sawConditional = true;
                }
                if (c instanceof RInlineFunction inlineC) {
                    if (inlineC.parent() instanceof RThenExpr) {
                        // A THEN-body lambda is a RENDER construct, not necessarily a
                        // runtime Java lambda — the rule then-chain renderers inline it
                        // at STATEMENT level (the ite-hoist seats). Whether this compile
                        // is truly inside a runtime lambda is the SCOPE's call: a
                        // compileLambda lambdaScope stops the findStatementHoistSink walk,
                        // so the statement-seat leg below self-gates. Keep walking.
                        c = c.parent();
                        continue;
                    }
                    eowInMapLambda = inlineC.parent() instanceof RExtractExpr
                            && (sawConditional || inlineC.body() == existsNode);
                    break;
                }
                if (c instanceof RFunction || c instanceof RRule) {
                    // No enclosing runtime lambda — a statement-position exists; fires
                    // only when a statement-hoist sink is reachable (rule/report path,
                    // the #262 session; a lambda-interior scope stops the walk → null)
                    // AND inside a RULE (the #232 shared-seat scoping — FUNCTION
                    // statement sinks exist too, but the ite condition-hoist channel
                    // drains rule-scoped only; the FUNCTION tail stays byte-frozen).
                    eowStatementSeat = c instanceof RRule
                            && ctx.scope().findStatementHoistSink() != null;
                    break;
                }
                c = c.parent();
            }
        }
        if (compiler.getTypeUtil() != null && compiler.getTypeTranslator() != null
                && compiler.getGeneratorModel() != null
                && expr.parent() instanceof RExistenceExpr
                && (eowInMapLambda || eowStatementSeat)
                && !ctx.scope().isExistsMetaWrapSuppressed()) {
            RJavaWithMetaValue meta = NavigationHandler.recoverInnerRuleMetaWrapper(rule, compiler);
            JavaType ruleBare = meta == null ? null : compiler.getTypeTranslator()
                    .toJavaReferenceType(compiler.getGeneratorModel().workspace()
                            .getInferredType(expr).type());
            if (meta != null && ruleBare != null && ruleBare.equals(meta.getValueType())) {
                String metaSimple = meta.getSimpleName();
                JavaType valueType = meta.getValueType();
                String valueSimple = valueType.getSimpleName();
                GeneratedIdentifier valueId = ctx.scope()
                        .createUniqueIdentifier(JavaNamingUtil.toFirstLower(valueSimple));
                String nameToken = ctx.scope().registerDeferredCoercionName(valueId);
                // The hoist decl's declared type import is contributed by its initializer refs
                // (JavaLocalVariableDeclarationStatement#getRefs ignores the declared type), so
                // enrich innerCall's refs with the value type (the #237/#314 established pattern).
                Set<JavaClass<?>> hoistRefs = new HashSet<>(innerCall.getRefs());
                if (valueType instanceof JavaClass<?> valueClass) {
                    hoistRefs.add(valueClass);
                }
                JavaExpression hoistValue = JavaExpression.from(innerCall.renderToString(),
                        valueType, hoistRefs, innerCall.getStaticWildcardImports());
                if (eowInMapLambda) {
                    ctx.scope().registerPendingLambdaHoist(new JavaLocalVariableDeclarationStatement(
                            true, valueType, valueId, hoistValue));
                } else {
                    // facet existsMetaSeats (PR #331), the STATEMENT seat: the decl registers
                    // as a sentinel-bearing STRING on the statement-hoist sink (the #237
                    // pattern); prependStatementHoists lifts it ahead of the enclosing ite
                    // statement. The value/wrapper imports flow via the returned conditional's
                    // branch refs (enriched below) — the string carries none.
                    ctx.scope().findStatementHoistSink().registerStatementHoist(
                            "final " + valueType.getSimpleName() + " " + nameToken + " = "
                                    + innerCall.renderToString() + ";");
                }
                JavaType mapperSMeta = compiler.getTypeUtil()
                        .wrap(compiler.getTypeUtil().MAPPER_S, meta);
                Set<JavaClass<?>> branchRefs = new HashSet<>();
                branchRefs.add(HandlerHelper.MAPPER_S);
                branchRefs.add(meta);
                if (valueType instanceof JavaClass<?> valueClass) {
                    branchRefs.add(valueClass);
                }
                // facet existsMetaSeats (PR #331): carry the inner call's refs on the
                // branches too — the STATEMENT seat's string-registered hoist drops the
                // initializer refs the LAMBDA route's decl statement carries (byte-neutral
                // for the lambda route: the same refs union at import collection).
                branchRefs.addAll(innerCall.getRefs());
                JavaExpression cond = JavaExpression.from(nameToken + " == null",
                        JavaPrimitiveType.BOOLEAN, Set.of(), Set.of());
                JavaExpression thenB = JavaExpression.from("MapperS.<" + metaSimple + ">ofNull()",
                        mapperSMeta, branchRefs, Set.of());
                JavaExpression elseB = JavaExpression.from("MapperS.of(" + metaSimple
                        + ".builder().setValue(" + nameToken + ").build())",
                        mapperSMeta, branchRefs, Set.of());
                return new JavaConditionalExpression(cond, thenB, elseB, compiler.getTypeUtil());
            }
        }
        return null;
    }

    /**
     * Injected-field receiver name for a bare {@link RRule} reference —
     * {@code lowerCamelCase(<generated rule class simple name>)}. This is the SAME
     * derivation used by {@link FunctionDependencyCollector}'s bare-RRule injection
     * branch (which registers the {@code @Inject} field) and by
     * {@code FunctionExpressionRenderer.renderBareInvokableThenSet} (the then-body
     * receiver) — the single source of truth, so the call receiver always matches the
     * declared field. The rule has no plain {@link RFunction}, so it is bridged via
     * {@link RFunction#fromRule(RRule)} and routed through the same
     * {@code toFunctionJavaClass} the {@code RuleGenerator} uses to name the class
     * ({@code <Name>Rule}). Requires a fully-wired compiler (generatorModel +
     * typeTranslator); the rule-body gate on the caller guarantees that — this branch
     * only fires during rule-family emission, where {@code FunctionGenerator} wires
     * both.
     */
    // public (was private) for the IR route's bare-rule-delegation reuse (the lab's L-045): the
    // IR-side RuleReceiverResolver reuses this byte-faithful <Name>Rule derivation rather than
    // re-deriving it. The D43 seam.
    public static String ruleInvocationReceiver(RRule rule, ExpressionCompiler compiler) {
        RFunction synthetic = RFunction.fromRule(rule);
        var symbolId = compiler.getGeneratorModel().symbolId(synthetic);
        JavaClass<?> javaClass = compiler.getTypeTranslator().toFunctionJavaClass(synthetic, symbolId);
        return FunctionDependencyCollector.ruleDependencyFieldName(javaClass.getSimpleName());
    }

    /**
     * True when the symbol reference is an alias invocation.
     *
     * <p>Primary path: {@link RSymbolReference#symbol()} resolves to an
     * {@link RShortcut} — set by the cross-reference resolver during AST
     * construction.
     *
     * <p>Belt-and-braces fallback: when {@code symbol()} is empty (rare — only
     * observed with partial or disabled resolution), walk the enclosing
     * function's shortcut list and match on name. This guards against
     * codegen silently falling back to the variable-reference path
     * (which emits broken {@code MapperS.of(aliasName)}) in partial-resolution
     * scenarios.
     *
     * <p>facet aliasSelfScopeFilter (PR #453): the fallback EXCLUDES the
     * shortcut the reference itself sits inside — upstream filters an alias's
     * OWN NAME from its body's symbol scope (vendored
     * RosettaScopeProvider:401-402), so a self-name is never an alias
     * invocation; it is the shadowed implicit-ITEM feature. The linker now
     * applies the same filter (LexicalResolutionPass), binding the 4 corpus
     * carriers to the item's attribute (Cat 9), and this admission must agree:
     * pre-guard, the name-based fallback re-admitted the self-name and
     * rendered a diverging self-call (`intentToAllocate(inputs)` where golden
     * maps the item feature — the probe's 4 FUNCTION mismatches). Excluded,
     * the self-name routes onto the exact path its already-clearing sibling
     * branch (`delegatedReportingSide`) takes. A self-name in a DIFFERENT
     * shortcut's body still admits (alias-to-alias calls), and a stale
     * symbol==self BIND (linker-unwritable since #453) keeps routing through
     * the primary path into the #372 aliasSelfShadowItemFeature synthesis.
     */
    // public (was private) so the IR-routed compiler reuses the EXACT shortcut-name-match
    // predicate as its point-free function-delegation guard (the lab's L-109): a bare function
    // reference colliding with an enclosing shortcut renders as an alias invocation, not a
    // function call, so the IR must NOT drive it via renderImplicitFunctionInvocation. The D43 seam.
    public static boolean isAliasReference(RSymbolReference expr) {
        if (expr.symbol().filter(RShortcut.class::isInstance).isPresent()) {
            return true;
        }
        // Fallback: match name against enclosing function's shortcuts
        RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
        if (enclosing == null || expr.name() == null) return false;
        RShortcut enclosingShortcut = HandlerHelper.findEnclosingShortcut(expr);
        for (RShortcut shortcut : enclosing.shortcuts()) {
            if (expr.name().equals(shortcut.name()) && shortcut != enclosingShortcut) {
                return true;
            }
        }
        return false;
    }

    /**
     * facet member_name_disambiguation — when an injected function dependency and a
     * shortcut/alias share a name, upstream
     * {@code GeneratorScope.computeActualNames} numbers the dependency {@code name0}
     * and the alias {@code name1} (see
     * {@link FunctionDependencyCollector#collidingDependencyAliasNames}). This applies
     * the numbering to a dependency RECEIVER ({@code name0.evaluate(…)}); a no-op
     * (returns the bare name) on every collision-free function, so green output is
     * untouched.
     */
    private static String disambiguateDependencyReceiver(RSymbolReference expr, String bareName) {
        // bareName is already a dependency field name (lowerCamelCase of the called
        // function), so the dependency-versus-alias collision reduces to a cheap
        // shortcut-name scan — no per-receiver full-AST walk (Copilot R1).
        RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
        if (enclosing != null
                && FunctionDependencyCollector.hasShortcutNamed(enclosing, bareName)) {
            return bareName + "0";
        }
        return bareName;
    }

    /**
     * Callee-aware overload (facet injectDepCollision, PR #364): tries the
     * dep-vs-DEP numbering FIRST — a callee in a same-simple-name FUNCTION-dependency
     * collision group renders the NUMBERED field the collector registered
     * ({@code commodityLeg11.evaluate(…)} for the csa {@code CommodityLeg1} while the
     * common one renders {@code commodityLeg10} — the single-source-of-truth
     * {@link FunctionDependencyCollector#collidingFunctionDependencyNumbering} map,
     * keyed by the callee's generated-class FQN) — then falls back to the
     * dep-vs-ALIAS arm. The numbering walk runs ONLY behind an identity-based
     * early-exit pre-check ({@code callsSecondDistinctFunctionSharing}), so the
     * universal no-collision population pays one allocation-free AST scan (the same
     * asymptotics as {@code hasFunctionDependencyNamed}, the accepted Copilot-R1
     * per-call-site pattern) and never resolves a type. Green-safe by construction:
     * pre-fix a collision pair rendered ONE bare field consumed by BOTH call sites
     * (semantically wrong Java), so no green file carries the shape.
     */
    private static String disambiguateDependencyReceiver(RSymbolReference expr,
            RFunction callee, String bareName, ExpressionCompiler compiler) {
        // Copilot #364 R1: the collision numbering is MEMOIZED per enclosing function
        // (FunctionDependencyCollector.COLLISION_NUMBERING_MEMO), so this per-receiver
        // consult is a map lookup after the function's first walk — the earlier
        // per-receiver early-exit AST pre-check (O(calls × AST)) was dropped with it.
        RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
        if (enclosing != null && callee != null && compiler != null
                && compiler.getTypeTranslator() != null
                && compiler.getGeneratorModel() != null) {
            var numbering = FunctionDependencyCollector.collidingFunctionDependencyNumbering(
                    enclosing, compiler.getGeneratorModel(), compiler.getTypeTranslator());
            if (!numbering.isEmpty()) {
                String fqn = compiler.getTypeTranslator()
                        .toFunctionJavaClass(compiler.getGeneratorModel().symbolId(callee))
                        .getCanonicalName().withDots();
                var numbered = numbering.get(fqn);
                if (numbered != null) {
                    return numbered.fieldName();
                }
            }
        }
        return disambiguateDependencyReceiver(expr, bareName);
    }

    /**
     * Alias-INVOCATION half of {@link #disambiguateDependencyReceiver}: numbers a
     * colliding alias call {@code name1(…)}.
     *
     * <p>facet aliasStaticImportEscape (PR #420): also applies the `_` escape when
     * the alias name collides with a static-imported operator member the enclosing
     * function references — the SAME memoized
     * {@link HandlerHelper#staticOperatorMembersUsed} census the DECLARATION seat
     * ({@code FunctionGenerator.compileAliases}) consults, so a called escaped
     * alias renders {@code _<name>(…)} in lockstep with its {@code _<name>} decl.
     * Applied after the dep numbering (a numbered {@code name1} never matches a
     * member simple name — the mechanisms never compose in practice).
     */
    private static String disambiguateAliasInvocation(RSymbolReference expr, String bareName) {
        // bareName is already a shortcut/alias name, so the collision reduces to an
        // early-exit check for a same-named function dependency (Copilot R1).
        RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
        String name = bareName;
        if (enclosing != null
                && FunctionDependencyCollector.hasFunctionDependencyNamed(enclosing, name)) {
            name = name + "1";
        }
        if (enclosing != null
                && HandlerHelper.staticOperatorMembersUsed(enclosing).contains(name)) {
            name = "_" + name;
        }
        return name;
    }

    /**
     * Render the enclosing function's input names as a comma-separated
     * argument list in declaration order.
     *
     * <p>When the enclosing function cannot be resolved, emits an empty
     * argument list and LOGs a warning. An empty arg list will typically
     * produce Java that fails to compile (alias methods on non-zero-input
     * functions require those inputs) — the warning surfaces the failure
     * at codegen time, rather than having the user hunt the empty-arg call
     * in a downstream compile error. The same defensive behaviour as
     * {@link NavigationHandler}'s deep-feature-call placeholder.
     *
     * <p>{@link RFunction#inputs()} returns an {@link java.util.ArrayList}
     * populated in source declaration order (see RFunction.java:46), so
     * iteration here preserves the alias method signature's parameter order.
     *
     * @param expr the alias symbol reference
     * @param aliasName the alias name (passed in for log context only)
     */
    private static String renderEnclosingInputs(RSymbolReference expr, String aliasName) {
        RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
        if (enclosing == null) {
            LOG.warning("Alias '" + aliasName + "': enclosing function not resolved "
                    + "— emitting empty arg list. Generated code will not compile "
                    + "unless the alias method is zero-arg. Wire receiver/parent "
                    + "resolution to fix.");
            return "";
        }
        // facet aliasCallInputEscape (v3.1 flip seat 32, law E.2): an alias CALL forwards the
        // enclosing function's inputs by their EMITTED render names, so an input whose raw name
        // is "_"-escaped at the declaration seat is forwarded escaped too — golden
        // `party1(counterparties, _partyLei)` against our `party1(counterparties, partyLei)`
        // (drr 7.0.0-7.3.0 CounterpartyRoleFromLEI, whose `@Inject protected PartyLei partyLei`
        // takes the raw name). FunctionGenerator.escapedFunctionInputName is the single source of
        // truth for every consumer of that name (facet fnInputDepCollisionEscape, PR #436): the
        // template ParamModel seat (FunctionGenerator:970), the bare-read seat above (:816) and
        // this CALL seat now read ONE escape table — one table, three consumers (LAW 69). The
        // owner is captured BEFORE the dispatch-base swap below: the arg NAMES come from the
        // BASE, while the escape is decided against the EMITTED function exactly as the
        // declaration seat decides it (FunctionGenerator:970 passes `func`, not the signature
        // source). Off-collision the helper returns the raw name unchanged, so every
        // non-escaping caller is byte-identical; and no green file can carry today's raw form
        // — passing the dependency FIELD where the parameter's own type is declared does not
        // compile (LAW 74). LAW 77: the IR route RE-IMPLEMENTS this seat at
        // IRExpressionCompiler.aliasCallInputNames, which reads the same table there.
        RFunction escapeOwner = enclosing;
        // facet dispatchVariantParamResolution (PR #369): inside a dispatch VARIANT the
        // enclosing function's own inputs are the single `__synthesized_input__`
        // placeholder — the alias METHOD signature takes the BASE's declared inputs
        // (FunctionGenerator threads the base as signatureSource), so the call must
        // forward those names (golden `endDate(calculationPeriod, priorCalculationPeriod,
        // calculateRelativeTo, resetDates)`). Non-variant callers keep the enclosing
        // function's own list byte-for-byte (dispatchBaseOf is null for them).
        RFunction dispatchBase = HandlerHelper.dispatchBaseOf(enclosing);
        if (dispatchBase != null) {
            enclosing = dispatchBase;
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (RAttribute input : enclosing.inputs()) {
            // facet zeroInputAliasCallArgs (W42 finding #14, PR #426): a ZERO-input
            // function's inputs() is the single `__synthesized_input__` placeholder the
            // AstBuilder adds — the alias METHOD declaration already filters it
            // (FunctionGenerator.isSynthesizedInput at the signature seats), so the CALL
            // must render the same empty list (golden `arg1()`, the func-zero-input-alias
            // oracle group; the leaked placeholder is no Java identifier in scope —
            // non-compiling, so no green file carries it).
            if (com.regnosys.rosetta.generator.java.function.FunctionGenerator
                    .isSynthesizedInput(input)) {
                continue;
            }
            // The EMITTED render name (the aliasCallInputEscape note above): the helper is the
            // identity off-collision, so this line is byte-neutral everywhere else.
            joiner.add(FunctionGenerator.escapedFunctionInputName(
                    escapeOwner, input.name(), escapeOwner.name()));
        }
        return joiner.toString();
    }

    // =========================================================================
    // Function call argument unwrapping
    // =========================================================================

    /**
     * Unwrap a compiled expression for use as a function {@code evaluate()} argument.
     *
     * <p>Generated function {@code evaluate()} methods accept raw Java types
     * (e.g. {@code List<?>}, {@code BigDecimal}, {@code int}), not Mapper-wrapped
     * values. This method strips the Mapper wrapper to produce bare values matching
     * golden output patterns.
     *
     * <p><b>Dispatch ladder (v6.2 C3a.2, re-numbered at PR #615 — evaluated in strict code
     * order).</b> The ladder was SEVEN branches until v3.1 C2d retirement family 8 deleted the
     * legacy {@code unwrapMapperSOf} string-scan strip that sat between the enum-constant branch
     * and the collapse re-wrap; it is now the SIX enumerated below. The deletion is byte-inert by
     * measurement (the whole-wrap parse returned null at all 320,024 census arrivals) — see facet
     * {@code mapperWrapPrefix} in the method body. (The counts here read SIX → FIVE until the
     * spec-compliance review: the pre-#615 {@code <ol>} never listed the #313 collapse re-wrap,
     * so its own item count had been one short of the ladder for as long as that branch existed.
     * The list below is now in bijection with the branches in the method body.)
     * <ol>
     *   <li><b>null builder</b> → returns a new synthetic
     *       {@code JavaExpression.from("null", null, Set.of())}. Distinct from
     *       the {@code "null"}-source case below: here there is no inbound
     *       builder at all, so a fresh expression must be synthesised.</li>
     *   <li><b>Structural unwrap</b> ({@code unwrapToBuilder().isPresent()} on
     *       a {@link JavaExpression}) — returns the inner builder directly.
     *       Source strip + {@code MAPPER_S} ref drop happen atomically via the
     *       {@link JavaExpression#wrappedInMapperSOf} factory contract. This
     *       is the preferred (no-string-scanning) path.</li>
     *   <li><b>{@code "null"}-source passthrough</b> ({@code "null".equals(source)})
     *       — returns the SAME inbound {@code expr} instance (pinned by
     *       {@code assertSame} in
     *       {@code ReferenceHandlerTest#unwrapForEvaluateArg_null_passes_through}).
     *       Distinct from branch #1 above: here the caller owns a real builder
     *       whose rendering happens to be {@code "null"}.</li>
     *   <li><b>Enum constant</b> (e.g. {@code FinancialUnitEnum.SHARE}, by the
     *       producer's witness {@link HandlerHelper#isBareEnumConstant} — PR #611
     *       retired the rendered-text shape test) — returns the SAME
     *       inbound {@code expr}. Enum constants have no {@code .get()} so the
     *       default fall-through branch would emit invalid Java.</li>
     *   <li><b>Collapsed bare-item re-wrap</b> (facet {@code consumerGetWrap}, PR #313) — a
     *       source already ending in a bare-item collapse {@code .get()} is re-wrapped
     *       {@code MapperS.of(...)} before the terminal accessor, so the fall-through cannot
     *       emit the non-compiling {@code <…>.get().get()}. Under {@code asMulti} the arm
     *       stamps {@link JavaExpression#evaluateArgMultiExtracted} (PR #614).</li>
     *   <li><b>Fall-through {@code .get()} suffix</b> — returns
     *       {@code expr.get()} with refs preserved VERBATIM (including
     *       {@code MAPPER_S}, because the resulting source still contains
     *       {@code MapperS.of(...)} via the chain). This is the chained-form
     *       branch ({@code MapperS.of(x).map(...)}); pinned by
     *       {@code JavaStatementBuilderRefsTest#refs_unwrap_falls_through_on_chained_form}.</li>
     * </ol>
     *
     * <p><b>Visibility:</b> {@code public static} is deliberate (promoted from
     * package-private in v6.2 C3a.2). The chained-form invariant test lives in
     * {@code ...statement.builder.JavaStatementBuilderRefsTest}, which is in a
     * different package. Public visibility is preferred over
     * {@code @VisibleForTesting} because the helper has a stable, documented
     * contract and cross-package handlers (future C3a.3/C3a.4 work) may call
     * it directly. Do NOT narrow this to package-private without relocating
     * the test.
     *
     * <p>Golden examples:
     * <pre>
     *   filterQuantityByCurrencyExists.evaluate(quantity)
     *   toTime.evaluate(0, 0, 0)
     *   qualify_BaseProduct_IRSwap.evaluate(economicTerms(trade).get())
     * </pre>
     *
     * @param builder the compiled argument expression builder, or {@code null}
     * @return an unwrapped {@link JavaStatementBuilder} whose rendered source is
     *     suitable for an {@code evaluate()} argument. For {@code null} input a
     *     synthetic new {@code JavaExpression} is returned; for the
     *     {@code "null"}-source and enum-constant branches the SAME inbound
     *     instance is returned (reference equality guaranteed); for all other
     *     branches a new expression is returned. Refs propagate from the
     *     inbound builder — MINUS {@code MAPPER_S} when a {@code MapperS.of(...)}
     *     wrap is stripped (branch #2, the structural unwrap's atomic drop — the
     *     conditional-drop string branch that was the other half of this sentence is gone at
     *     PR #615), PRESERVED VERBATIM otherwise.
     *     {@code staticWildcardImports} (e.g.
     *     {@code EXPRESSION_OPERATORS_NULL_SAFE} carried by
     *     ComparisonHandler/ExistenceHandler emissions in C3a.4.b/d) are
     *     ALWAYS preserved across every branch — dropping them would silently
     *     regress imports at C3b consumption time.
     * @throws IllegalStateException if {@code builder} is neither {@code null}
     *     nor a {@link JavaExpression}. Defensive guard — structurally
     *     unreachable from current call sites but surfaces future drift loudly.
     */
    public static JavaStatementBuilder unwrapForEvaluateArg(JavaStatementBuilder builder) {
        return unwrapForEvaluateArg(builder, false);
    }

    /**
     * Cardinality-aware variant of {@link #unwrapForEvaluateArg(JavaStatementBuilder)}.
     * When {@code asMulti}, the fall-through unwrap of a chained {@code Mapper}
     * argument uses {@code .getMulti()} (→ {@code List<T>}) instead of {@code .get()}
     * (→ scalar {@code T}). The caller passes {@link #evaluateParamIsMulti} (the callee
     * PARAMETER's cardinality, facet {@code tailMulti}, PR #191), a superset of the
     * original getmulti_multi_arg gate {@link #evaluateArgIsMulti} (PR #131, param-multi
     * AND arg-multi): both a {@code MapperC} chain AND a SINGLE {@code MapperS} chain
     * into a multi parameter now unwrap with {@code .getMulti()} (the latter is the
     * single→{@code List} coercion {@code MapperS.getMulti()} provides). The other
     * branches (null/{@code "null"}-source, the enum-constant witness, the structural
     * unwrap) are unaffected: a chained {@code Mapper} matches none of
     * them, so {@code asMulti} only ever changes the fall-through terminal accessor, and
     * every expression reaching the fall-through is a {@code Mapper} (which always has
     * {@code getMulti()}). {@code asMulti = false} reproduces the historical behaviour
     * exactly.
     */
    // public (was private) for the IR-routed compiler's verbatim oracle reuse — the D43 seam.
    public static JavaStatementBuilder unwrapForEvaluateArg(JavaStatementBuilder builder, boolean asMulti) {
        if (builder == null) {
            return JavaExpression.from("null", null, Set.of());
        }
        if (builder instanceof JavaExpression expr) {
            // v6.2 structured unwrap — preferred path. Carries refs minus
            // MAPPER_S through the wrap factory contract (see
            // JavaExpression.wrappedInMapperSOf javadoc).
            Optional<JavaStatementBuilder> innerBuilder = expr.unwrapToBuilder();
            if (innerBuilder.isPresent()) {
                return innerBuilder.get();
            }
            // BELOW THE STRUCTURAL UNWRAP: the four branches an expression with NO marker takes.
            // The preamble that stood here — "fall back to rendered-text inspection for legacy
            // emitters that haven't yet migrated to the wrap factory" — was written when the first
            // of them WAS a text strip, and it survived that branch's deletion; the code-quality
            // review's NIT-8 retires it. What actually follows, in order: the `"null"`-source
            // PASSTHROUGH, the #611 ENUM-CONSTANT WITNESS (a producer-stamped structural fact, not
            // a text read at all), the #313 collapse RE-WRAP, and the fall-through terminal
            // ACCESSOR. None of them mutates refs where it passes through — each preserves the
            // inbound builder's refs verbatim.
            //
            // facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615): the
            // TODO(C3a.4) that stood here — "delete the unwrapMapperSOf string-scan branch once
            // LiteralHandler + both coercers route through wrappedInMapperSOf" — is DISCHARGED AT
            // THIS SITE, and by MEASUREMENT rather than by the migration it named. The branch
            // (`String bare = HandlerHelper.unwrapMapperSOf(source)` and its PR #417 conditional
            // MAPPER_S drop, facet defaultOpMapperSRefSurvival) is DELETED as dead code: the c8
            // census logged 320,024 arrivals here (118,365 / 121,717 / 79,942 — default-route D11,
            // IR-route D11, optimised) and the whole-wrap parse returned NULL at every single one
            // — `wholeWrap=false` at 320,024/320,024, so the branch body NEVER executed and its
            // deletion moves no byte. The reason is structural: the branch is reached only when
            // the marker is EMPTY (the structural unwrap above returns first), and the
            // marker-less traffic arriving here is bare or chained, never a balanced whole wrap
            // (outer type: null at 288,543, MapperS at 30,129, Mapper at 1,062, MapperC at 290).
            //
            // The TODO's own premise was also measured FALSE, and is not restated elsewhere: two
            // of the three emitters it named (LiteralHandler, ItemToWrapperCoercer) are ALREADY
            // migrated, ~140 other raw `MapperS.of(` sites remain, and
            // `HandlerHelper.unwrapMapperSOf` itself survives on five non-strip idempotence
            // callers (1.25% of its arrivals — see its javadoc).
            //
            // CROSS-FAMILY GUARD, measured: the deleted branch RETURNED, upstream of the
            // `EvaluateArgMultiExtracted` stamp below (family 7's channel, PR #614). With ZERO
            // arrivals taking it, no arrival changes which stamp it reaches.
            String source = expr.renderToString();
            // "null"-source passthrough — returns the SAME inbound instance
            // (reference equality). Contrast with the null-BUILDER branch
            // above, which synthesises a fresh JavaExpression.from("null", ...)
            // because no inbound builder exists to return. Both branches
            // render as "null" but they are semantically distinct: passthrough
            // preserves the caller's refs (even if the caller somehow built a
            // "null"-rendering expression with non-empty refs), while the
            // synthetic path uses empty refs.
            if ("null".equals(source)) {
                return expr;
            }
            if (HandlerHelper.isBareEnumConstant(expr)) {
                return expr;
            }
            // facet consumerGetWrap (PR #313): a value whose rendered source ALREADY
            // ends in a bare-item collapse `.get()` (an `only-element`/`first`/`last`
            // terminal on a Mapper, e.g. GetInternalId's arg3
            // `… then cftcTransactionInformation -> internalTradeIdentifier only-element`)
            // is a BARE ITEM, not a Mapper — the fall-through's `.get()` append below
            // would emit the non-compiling `<…>.get().get()`. ZERO of the 34,686 goldens
            // carry `.get().get()`, so every arg reaching this branch with a
            // `.get()`-terminal source is an already-waivered non-compiling mismatch —
            // green-safe by construction (a green file cannot carry the firing shape).
            // Golden re-wraps the collapsed bare item in `MapperS.of(…)` so the
            // arg-collapse accessor is valid — a behaviour-neutral round-trip:
            // `MapperS.of(<…>.get()).get()`. Carriers: DTCC_TradeParty1TransactionIDRule
            // cftc/csa {dtcc,valuation}. Add MAPPER_S to refs (the wrap names it).
            // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614) — THIS ROW IS LEFT
            // WITH ITS TEXT READ, deliberately, and the reason is structural. The row's premise is
            // that "the collapse producer's marker on the incoming builder" is the channel; it is
            // not, and cannot be: the STRUCTURAL UNWRAP branch at the top of this method returns
            // `innerBuilder.get()` before control can reach here, so EVERY arrival at this branch
            // provably carries NO unwrap marker — and the family's producer kind
            // (JavaExpression.SelfUnwrappingBareCollapse) carries one by contract. The row's other
            // named channel, `isWrapper(getExpressionType())`, is REFUTED by the c7b census as
            // well: 118,365 / 121,717 / 79,942 arrivals (default-route, IR-route, optimised),
            // text=true at 32, and the compiled type is NULL at
            // ALL 32 (at every one of the 10,311 TYPED arrivals the render is not `.get()`-tailed),
            // so the wrapper test has no answer to give — the #610 pattern. There is no AST node
            // here either: unwrapForEvaluateArg receives a builder and a boolean, nothing else.
            // Whatever produced this `.get()` tail is a re-render through JavaExpression.from
            // somewhere upstream (which drops every marker by design), so stamping a producer would
            // not reach this seat. Adjudication is deferred rather than a speculative swap forced.
            if (source.endsWith(".get()")) {
                Set<JavaClass<?>> refsPlusMapperS = new HashSet<>(expr.getRefs());
                refsPlusMapperS.add(HandlerHelper.MAPPER_S);
                String reWrapped = "MapperS.of(" + source + ")"
                        + (asMulti ? ".getMulti()" : ".get()");
                // facet collapseGetSuffix (PR #614): under asMulti this arm APPENDS the
                // `.getMulti()` terminal, so it reports that arm exactly as the fall-through below
                // does — the two elementwise-deref seats read the witness, never the suffix.
                return asMulti
                        ? JavaExpression.evaluateArgMultiExtracted(reWrapped,
                                expr.getExpressionType(), refsPlusMapperS,
                                expr.getStaticWildcardImports())
                        : JavaExpression.from(reWrapped, expr.getExpressionType(), refsPlusMapperS,
                                expr.getStaticWildcardImports());
            }
            // Chained form or general mapper expression — append .get() and
            // preserve inbound refs + staticWildcardImports. The
            // refs-preservation is architecturally significant: chained
            // builders (e.g. MapperS.of(x).map(...)) do NOT set
            // unwrapToBuilder, so they hit this branch. MAPPER_S remains in
            // refs because the source still contains MapperS.of. Static
            // wildcards (e.g. EXPRESSION_OPERATORS_NULL_SAFE carried by
            // ComparisonHandler/ExistenceHandler emissions in C3a.4.b/d) must
            // also propagate through — dropping them here silently regresses
            // imports at C3b consumption time.
            // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): THE ARM REPORT. Under
            // asMulti this fall-through is the arm that appends the `.getMulti()` terminal, and the
            // two elementwise-deref seats that must know the arg took the multi collapse before
            // they splice that suffix away now read the witness class rather than the rendered
            // suffix (LAW 69 — the fact is this helper's own control flow, exactly as the row asks;
            // the same pattern as #613's unwrapForAddAssignment / MultiExtracted). The c7b census
            // measured why the report has to be the ARM and not the intent: at the explicit-arg
            // seat, the `evalArgUnwrap AND multi=true` class is 16,275 / 11,228 / 10,694 arrivals
            // (default-route, IR-route, optimised) but the suffix is present at only
            // 10,127 / 8,712 / 5,533 — the rest took one of the branches ABOVE, which append
            // nothing. Byte-identical to the `from` form it replaces.
            String unwrappedText = source + (asMulti ? ".getMulti()" : ".get()");
            return asMulti
                    ? JavaExpression.evaluateArgMultiExtracted(unwrappedText,
                            expr.getExpressionType(), expr.getRefs(),
                            expr.getStaticWildcardImports())
                    : JavaExpression.from(unwrappedText, expr.getExpressionType(),
                            expr.getRefs(), expr.getStaticWildcardImports());
        }
        throw new IllegalStateException(
                "Cannot unwrap non-JavaExpression builder for evaluate arg: "
                + builder.getClass().getSimpleName());
    }

    // =========================================================================
    // Enum value references
    // =========================================================================

    /**
     * The enum's {@link JavaClass} as a singleton refs set, for the import
     * channel of an emitted {@code EnumName.VALUE} constant. The constant uses
     * the enum's SIMPLE name, so the type must be imported; collecting it here
     * (rather than relying on an ambient output / attribute-type import) covers
     * the case where the enum is referenced ONLY via a function-call argument —
     * the rule otherwise never names the enum type, so no ambient import exists.
     * Refs are de-duplicated downstream, so adding it is a no-op when an ambient
     * import is already present (PR #130).
     */
    private Set<JavaClass<?>> enumImportRefs(
            com.regnosys.rosetta.ast.types.REnumeration enumeration,
            ExpressionCompiler compiler) {
        var translator = compiler.getTypeTranslator();
        if (translator == null) {
            // getTypeTranslator() is documented nullable — the no-arg
            // ExpressionCompiler() used by handler unit tests has no translator.
            // Production paths (RuleGenerator / FunctionGenerator) always inject
            // one, so the import is always collected there; skip when absent
            // rather than NPE (the rendering string is unaffected either way).
            return Set.of();
        }
        JavaClass<?> enumClass = translator
                .toJavaReferenceType(new com.regnosys.rosetta.types.REnumTypeRef(enumeration));
        return Set.of(enumClass);
    }

    /**
     * Compiles an enum value reference.
     *
     * <p>Due to grammar ambiguity, {@code paramName -> featureName} is parsed as
     * {@link REnumValueRef} with {@code enumName=paramName} and {@code valueName=featureName}
     * when it cannot be disambiguated at parse time. Resolution sets
     * {@code resolvedEnum} only when the name is a real enum.
     *
     * <p>Two cases:
     * <ul>
     *   <li><b>Real enum value reference</b> ({@code enumeration()} present):
     *       emits {@code EnumTypeName.valueName}. No wrapping in {@code MapperS.of(...)}
     *       — enum values used in comparisons are typically passed as direct enum
     *       constants rather than wrapped mappers.</li>
     *   <li><b>Disguised feature call</b> ({@code enumeration()} empty):
     *       synthesizes an {@link RFeatureCall} with receiver = {@link RSymbolReference}
     *       for {@code enumName} and feature = {@code valueName}, then delegates to the
     *       {@link NavigationHandler}. This produces the correct
     *       {@code MapperS.of(param).map("getFeature", _param -> _param.getFeature())}
     *       pattern.</li>
     * </ul>
     *
     * @param expr     the enum value reference node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler (used to dispatch the synthesized feature call)
     * @return a {@link JavaExpression} rendering either the enum constant or the mapper chain
     */
    public JavaStatementBuilder handle(REnumValueRef expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // Real enum value reference. The Java constant is the CONVERTED value name
        // (EnumHelper.convertValue: strip the `^` keyword-escape, camelCase →
        // UPPER_SNAKE), NOT the raw source value name — the same conversion the enum
        // DECLARATION emits (EnumGenerator), so a reference like `ReportableActionEnum
        // -> Correct` renders `ReportableActionEnum.CORRECT` (the actual Java constant)
        // rather than the non-compiling `ReportableActionEnum.Correct`. Mirrors the
        // bare-REnumValue branch above. Prefer the resolved REnumValue (exact name incl.
        // any escape); fall back to converting the value-name string when only the
        // enumeration (not the specific value) resolved.
        if (expr.enumeration().isPresent()) {
            // v3.2 seat 9 (PR #630, F13 / D48): the value-name fallback below renders a constant the
            // enum may not declare. When the LINKER reported the value unresolved (ENUM_VALUE_NOT_FOUND at
            // this reference's own range - the resolver's verdict, GeneratorModel.isReportedUnresolved),
            // that constant is the E2 wrong-enum echo (`C17SideEnum.SELL` against p2's rival enum, the
            // chaos s17 split) - REFUSE at the twelfth register site instead of emitting non-compiling Java
            // in silence. Unbound WITHOUT a diagnostic (an AST built outside the linker) keeps the fallback
            // and COUNTS at the same site (expected 0 on every cell) - and so does a compiler with NO
            // GeneratorModel wired (the stateless unit-compiler mode, `new ExpressionCompiler()`: no linker,
            // no verdict on record; the sibling UNRESOLVED_SYMBOL_ECHO gate above guards the same null -
            // eleven handler unit tests drive this arm without a model, and the chain's whole generator
            // suite caught the NPE at 44d323815). ONE site on both routes: the IR adapter declines this
            // shape (valueNameFallback) to this render.
            if (expr.enumValue().isEmpty()) {
                String witness = expr.enumeration().get().name() + " -> " + expr.valueName();
                // Round 1 (cq SF-9): the message names ENUM_VALUE_NOT_FOUND, so the predicate reads that category.
                if (compiler.getGeneratorModel() != null
                        && compiler.getGeneratorModel().isReportedUnresolved(expr, expr.valueName(),
                                com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.ENUM_VALUE_NOT_FOUND)) {
                    throw SilentDegradation.refuse(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO,
                            "enum value reference `" + witness + "` whose value the linker reported unresolved"
                                    + " (ENUM_VALUE_NOT_FOUND) - the raw name would render a constant the enum"
                                    + " does not declare",
                            expr);
                }
                SilentDegradation.record(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO, witness);
            }
            String valueConstant = expr.enumValue()
                    .map(com.regnosys.rosetta.generator.java.enums.EnumHelper::convertValue)
                    .orElseGet(() -> com.regnosys.rosetta.generator.java.enums.EnumHelper.formatEnumName(
                            com.regnosys.rosetta.generator.java.enums.EnumHelper.stripEscape(expr.valueName())));
            // Register the enum's JavaClass in the refs channel so its import is
            // emitted. The constant `EnumName.VALUE` uses the simple name, which
            // requires `import <pkg>.EnumName;`. Earlier this relied on an "ambient"
            // import contributed by the output / attribute type — which is absent
            // when the enum is a function-call ARGUMENT whose type the rule does not
            // otherwise reference (e.g. `RoundToPrecision(value, 5,
            // RoundingDirectionEnum -> NEAREST)`), dropping the import and producing
            // non-compiling Java. The reported type stays null (rendering / coercion
            // byte-unchanged); only the import is added. Refs are a Set, so a file
            // that already imports the enum ambiently is unaffected (PR #130).
            //
            // The simple name comes from the RESOLVED enumeration (REnumeration.name()),
            // NOT expr.enumName() (the raw source text, which may be namespace-qualified —
            // e.g. `staticdata.asset.common.TaxonomySourceEnum -> CFTC`). A multi-segment
            // prefix would emit a partial/invalid FQN `staticdata.asset.common.TaxonomySourceEnum.CFTC`
            // (missing the real `cdm.base.` root → non-compiling) AND, in an evaluate-arg slot,
            // failed the retired isDottedEnumConstant's single-dot probe so a spurious `.get()`
            // was appended (the enum_arg_fqn_get facet, PR #141; since PR #611 that slot reads the
            // producer's witness, not the spelling). For an already-simple source ref
            // enumeration().name() == enumName(), so this is a no-op (PR #130 byte-unchanged).
            return JavaExpression.enumConstant(expr.enumeration().get().name() + "." + valueConstant,
                    null, enumImportRefs(expr.enumeration().get(), compiler), Set.of());
        }

        // Bare RULE reference used as a navigation receiver (facet F6 residual):
        // `SomeRule -> feature` parses as an REnumValueRef (the grammar's
        // EnumName -> valueName shape) whose resolvedSymbol is an RRule. Synthesize an
        // RFeatureCall whose RECEIVER carries the RRule symbol, so the receiver compiles
        // through handle(RSymbolReference)'s bare-RRule branch
        // (MapperS.of(<ruleField>.evaluate(<arg>))) and THIS call navigates to `feature`:
        // MapperS.of(<ruleField>.evaluate(<arg>)).<T>map("getFeature", v -> v.getFeature()).
        // Without this, the reference falls through to synthesizeFeatureCall, whose
        // receiver symbol is only resolved for FUNCTION attributes — leaving the rule
        // symbol unset, so the bare-RRule branch can't fire and the bare (possibly
        // namespace-qualified) type name renders as a non-compiling literal (e.g.
        // MapperS.of(cde.payment.PeriodicPayment)). The bare-RRule branch's OWN gate
        // (enclosing rule + from-type / in-lambda binding) governs whether it fires —
        // declining to the pre-fix variable path otherwise — so this is
        // byte-identity-safe by the same argument as PR #100/#101.
        if (expr.resolvedSymbol().isPresent()
                && expr.resolvedSymbol().get() instanceof RRule rule
                && expr.enumName() != null && expr.valueName() != null) {
            RFeatureCall ruleNav = synthesizeRuleReceiverNavigation(expr, rule);
            return compiler.visitFeatureCall(ruleNav, ctx);
        }

        // Bare FUNCTION reference used as a navigation receiver (facet bareSymInvoke,
        // PR #280) — the bare-FUNCTION analogue of the bare-RULE arm above. A
        // disguised `SomeFunction -> feature` nav (e.g. `extract RateOption ->
        // indexTenor`) parses as an REnumValueRef whose resolvedSymbol is an RFunction
        // (the parser's GlobalResolutionPass binds the disguised head). Synthesize an
        // RFeatureCall whose RECEIVER carries the RFunction symbol so the receiver
        // compiles through handle(RSymbolReference)'s bare-FUNCTION branch
        // (renderImplicitFunctionInvocation -> `<fnField>.evaluate(item.get())`) and
        // THIS call navigates to `feature`. Without this, the reference falls through
        // to synthesizeFeatureCall (function-scope head resolution only) leaving the
        // function symbol unset, so the bare type name renders as a non-compiling
        // literal `MapperS.of(RateOption)`. Byte-identity-safe by construction (same
        // argument as the bare-RULE/bare-FUNCTION branches): the pre-fix bare type
        // literal never compiled, so no currently-green file carries it — only
        // already-waivered files flip.
        if (expr.resolvedSymbol().isPresent()
                && expr.resolvedSymbol().get() instanceof RFunction func
                && expr.enumName() != null && expr.valueName() != null) {
            // Decline when the navigated feature resolves to a META-annotated leaf
            // (FieldWithMetaX / ReferenceWithMetaX). Golden emits a `Type coercion`
            // `.getValue()` meta-deref at such a leaf (a SEPARATE co-occupied mechanism),
            // so resolving the receiver WITHOUT that deref surfaces the meta-wrapper element
            // type, which leaks onto an enclosing `thenArg` declaration's type witness — a
            // within-waiver regression (#280). EXCEPTION (facet conditionalMetaJoin, PR #295,
            // the #280 meta-leaf residual): when compiling the arms of a MIXED-baresym
            // conditional ladder — where CollectionHandler.compileLadderConditionalBlock has
            // proven a SIBLING baresym arm navigates the BARE (non-meta) leaf, so the
            // meta-aware join is the bare value and the wrapper arm WILL be deref'd back by
            // the caller — the nav fires (the scope flag is set) and renders the meta WRAPPER;
            // the caller's `.getValue()` deref then makes all arms agree on the bare value.
            // The same scope flag is also set by SetOperationHandler.handle(RDefaultExpr) for a
            // MIXED-baresym `default` (facet getOrDefaultMetaJoin, PR #296 — a meta-leaf LEFT
            // operand + a bare-leaf RIGHT operand). A HOMOGENEOUS-meta default / single-conditional
            // / non-baresym operand keeps the #280 decline (no leak). Green-safe by the #280
            // argument: the pre-fix bare type literal never compiled.
            RAttribute featureAttr = func.output()
                    .map(out -> resolveFeatureOnAttribute(out, expr.valueName()))
                    .orElse(null);
            boolean metaLeaf = featureAttr != null
                    && MetaFieldGenerator.detectMetaKind(featureAttr) != MetaFieldGenerator.MetaKind.NONE;
            // facet baresymConditionalMetaJoin (PR #333): the #295 mixed-ladder exception's
            // ALL-META sibling — when the OUTERMOST enclosing conditional (within this lambda,
            // the RInlineFunction boundary) JOINS to exactly this leaf's wrapper via the #331
            // all-present-arms-agree walker, the consumer seats coordinate on the WRAPPER
            // (the CP4/CP5 typedEmptyReturn + thenArg decl arms read the SAME walker), so the
            // #280 witness-leak cannot occur — golden keeps the wrapper at the nav
            // (`.<ReferenceWithMetaCommodity>map("getCommodity", …)`, DTCC_Leg1CommodityInstrumentID).
            // A mixed ladder joins bare (walker null) and a different-wrapper join fails the
            // equality — both keep the #280 decline. Green-safe by the #280 argument (the
            // declined bare type literal never compiled).
            boolean conditionalJoinAgrees = false;
            if (metaLeaf && ctx.scope() != null && !ctx.scope().isMetaLeafBaresymAllowed()) {
                RJavaWithMetaValue leafWrapper = NavigationHandler.metaWrapperOf(featureAttr, compiler);
                if (leafWrapper != null) {
                    RConditionalExpr outermostCond = null;
                    RNode p = expr.parent();
                    while (p != null && !(p instanceof RInlineFunction)
                            && !(p instanceof RFunction) && !(p instanceof RRule)) {
                        if (p instanceof RConditionalExpr rc) {
                            outermostCond = rc;
                        }
                        p = p.parent();
                    }
                    if (outermostCond != null) {
                        RJavaWithMetaValue joined =
                                NavigationHandler.recoverExprMetaWrapper(outermostCond, compiler);
                        conditionalJoinAgrees = joined != null && joined.equals(leafWrapper);
                    }
                }
            }
            // facet bareFnMetaLeafTerminal (PR #359, F-5): a meta leaf that TERMINATES
            // the disguised chain (the EVR is not the receiver of a FURTHER RFeatureCall
            // hop) fires the nav WITH the wrapper witness — the #280 leak concern is a
            // MID-CHAIN one: an interior meta hop needs its own mid-chain deref decision
            // (UnderlierForProduct -> commodity -> productIdentifier …, the deferred
            // UnderlyingIdentification family — still declined). At a TERMINAL leaf the
            // consumer seat reads the SAME compiled wrapper type golden coordinates on:
            // the mapSingleToList element + the pre-existing outer whole-output coercion
            // (IdentifierOfBasketConstituents iosco) and the thenArg decl's #204/#144
            // wrapper recovery (NotionalAmountScheduleLeg1/2 v1/v3 — golden's decls
            // CARRY the wrapper). A CONDITIONAL-ARM seat stays on the #280/#295/#333
            // laws (declined here): golden derefs a MIXED ladder's meta arm IN-ARM and
            // types the empty else to the bare join — coordination this terminal
            // widening does not provide (the EffectiveDateRule-common record-date
            // ladder — its elseless-block deref pass is a #360 facet: isProvablyBareArm
            // needs the record-date arm + the terminal-empty element must follow the
            // join). Green-safe by the #280 argument: the declined bare type literal
            // never compiled, so only waivered files change.
            boolean inConditionalArm = false;
            for (RNode p = expr.parent(); p != null
                    && !(p instanceof RInlineFunction) && !(p instanceof RFunction)
                    && !(p instanceof RRule); p = p.parent()) {
                if (p instanceof RConditionalExpr) {
                    inConditionalArm = true;
                    break;
                }
            }
            boolean terminalMetaLeaf = metaLeaf && !inConditionalArm
                    && !(expr.parent() instanceof RFeatureCall pfc && pfc.receiver() == expr)
                    && !(expr.parent() instanceof REnumValueRef);
            // facet midChainCallableMetaDeref (PR #371, F-D): a meta leaf MID-CHAIN — the
            // EVR is the RECEIVER of a further RFeatureCall hop — fires the nav WITH the
            // wrapper witness; the CONTINUATION hop's receiver coercion derefs the wrapper
            // inline (the standard WrappedItemCoercer null-guarded MapperS form: golden
            // CollateralPortfolioCodeRule `.<ReferenceWithMetaCollateral>map("getCollateral",
            // counterpartyPosition -> …).<Collateral>map("Type coercion",
            // referenceWithMetaCollateral0 -> referenceWithMetaCollateral0 == null ? null :
            // referenceWithMetaCollateral0.getValue()).<Identifier>mapC(…)` — the deferred
            // coercion params number 0..n-1 across the method exactly like golden). The
            // wrapper is CONSUMED by the next hop, so it cannot leak onto a thenArg decl —
            // the #280 leak and the #370-cp4 whole-output misread are both TERMINAL-leaf
            // concerns, and the terminal decline above stands.
            boolean midChainMetaLeaf = metaLeaf
                    && expr.parent() instanceof RFeatureCall pfc2 && pfc2.receiver() == expr;
            if (!metaLeaf || (ctx.scope() != null && ctx.scope().isMetaLeafBaresymAllowed())
                    || conditionalJoinAgrees || terminalMetaLeaf || midChainMetaLeaf) {
                RFeatureCall funcNav = synthesizeFunctionReceiverNavigation(expr, func);
                return compiler.visitFeatureCall(funcNav, ctx);
            }
        }

        // Rule-body implicit-input attribute-feature chain (D39 Category 10):
        // the grammar parses `a -> b` as REnumValueRef, and TypeInferenceEngine
        // binds resolvedAttributeChain when both names resolve as features rooted
        // at the rule's implicit input (e.g. `priceQuantity -> quantitySchedule`).
        // Codegen reuses that binding — rendering the input-navigation chain
        // MapperS.of(input).<A>map("getPriceQuantity", ...).<B>map("getQuantitySchedule", ...)
        // by synthesizing the equivalent RFeatureCall chain and delegating to the
        // NavigationHandler (type witnesses, map/mapC, lambda naming, meta-wrap all
        // derive from the resolved RAttributes). Without this, the fall-through
        // synthesizeFeatureCall path re-derives via the enclosing *function* (null
        // in a rule body), emitting a bare `MapperS.of(<name>)` with the leading
        // input getter dropped.
        if (expr.resolvedAttributeChain().isPresent()) {
            RFeatureCall chainCall = synthesizeImplicitInputChain(
                    expr, expr.resolvedAttributeChain().get());
            if (chainCall != null) {
                return compiler.visitFeatureCall(chainCall, ctx);
            }
        }

        // facet lambda_item_body_coercion (arm B2, disguised chain): an in-lambda
        // `head -> feature` chain whose HEAD is an attribute of the implicit ITEM
        // type synthesizes item-rooted navigation
        // (`item.<T>mapC("getClassification", taxonomyValue -> ...).<I>map(...)`)
        // — the chain disguise analogue of the PR #161 bare-name arm. Parse-time
        // Cat 9/10 cannot bind these (the owning list-op's ARGUMENT is itself a
        // disguised ref whose inferred type is MISSING), so the chain reaches
        // codegen unbound and previously fell to synthesizeFeatureCall, whose
        // function-scope-only head resolution left the receiver unresolved —
        // rendering the witness-less, non-compiling `MapperS.of(<head>).map(...)`.
        // Declines (legacy bytes) for closure params, function-scope heads, and
        // heads the gm-aware item type does not carry.
        if (expr.enumName() != null && expr.valueName() != null) {
            RFeatureCall itemChain = synthesizeImplicitItemChain(expr, compiler);
            if (itemChain != null) {
                return compiler.visitFeatureCall(itemChain, ctx);
            }
        }

        // facet caseNarrowedDisguisedNav (PR #368, F-B): a disguised `head -> feature`
        // chain INSIDE a TYPE-keyed switch CASE whose narrowed case type carries the
        // HEAD attribute synthesizes the implicit-item-rooted chain — the synthetic
        // item resolves to the #221-bound cast case var, so the render is
        // `MapperS.of(floatingRateCalculation).<FloatingRateModel>map(
        // "getFloatingRateModel", …).<StrikeSchedule>mapC("getCapRateSchedule", …)`
        // (golden MapCap/FloorRateScheduleToPriceWithLocation). Gated on the ACTIVE
        // subject binding (only a rendering instanceof ladder binds — every
        // un-laddered switch falls to the residual seat's refusal: TYPE_SWITCH_TERNARY_STUB per
        // resolvable case type first, since v3.1 C0, else SWITCH_TERNARY_STUB, R1, since v3.2 seat 12;
        // today's bytes before them) + the B2 arm's closure-param /
        // function-scope declines. Green-safe by construction: the pre-fix render is
        // the undefined bare `MapperS.of(<head>)` symbol — non-compiling.
        if (expr.enumName() != null && expr.valueName() != null && ctx.scope() != null) {
            RFeatureCall caseChain = synthesizeCaseNarrowedChain(expr, ctx, compiler);
            if (caseChain != null) {
                return compiler.visitFeatureCall(caseChain, ctx);
            }
        }

        // facet implicitInputAttrDisguiseHead (seat 28, law 11): the THIRD arm of the
        // disguised `head -> feature` chain - the rule-body IMPLICIT-INPUT rooting, the
        // by-NAME twin of the Cat-10 arm above (which needs a bound AttributeChain that a
        // CHOICE-OPTION leaf never produces - chainBound=false at every carrier,
        // PROBE28-F15d) and of the B2 item arm above (which needs an enclosing
        // item-binding lambda). Placed LAST, immediately before synthesizeFeatureCall, so
        // it fires only where the chain would otherwise fall to that function-scope-only
        // head resolution and render the non-compiling bare head name
        // `MapperS.of(rateSpecification)` (golden FixedRateRule:
        // `MapperS.of(input).<RateSpecification>map(...)...`). The green twin
        // PeriodicPaymentRule reports the IDENTICAL probe tuple and is separated by ARM
        // ORDER, not a predicate: its disguise sits inside a filter lambda, so the B2 arm
        // claims it and returns before this arm is reached; FixedRateRule's is at the
        // rule TOP level, which B2's bindsImplicitItem decline hands on. Green-safe by
        // the same #280/B2 argument: the pre-fix bare head literal never compiled
        // (LAW 74).
        if (expr.enumName() != null && expr.valueName() != null) {
            RFeatureCall inputChain = synthesizeImplicitInputChainByName(expr, compiler);
            if (inputChain != null) {
                return compiler.visitFeatureCall(inputChain, ctx);
            }
        }

        // Disguised feature call — synthesize RFeatureCall and dispatch via NavigationHandler
        if (expr.enumName() != null && expr.valueName() != null) {
            RFeatureCall synth = synthesizeFeatureCall(expr, compiler);
            return compiler.visitFeatureCall(synth, ctx);
        }

        // Fallback — preserve legacy behaviour
        return JavaExpression.from(expr.enumName() + "." + expr.valueName(), null);
    }

    /**
     * Synthesize an {@link RFeatureCall} from an unresolved {@link REnumValueRef}.
     *
     * <p>The enum name becomes a {@link RSymbolReference} receiver (with best-effort
     * symbol resolution via the enclosing RFunction), and the value name becomes the
     * feature name. If the enclosing function's inputs/output/shortcuts contain the
     * referenced name, the resolved attribute is wired into the synthesized symbol
     * reference so downstream handlers can resolve types for lambda naming.
     *
     * <p>Similarly, if the receiver type is known and has an attribute named
     * {@code valueName}, that attribute is wired as the feature call's resolvedFeature
     * so NavigationHandler can emit the generic type parameter and correct
     * {@code map}/{@code mapC} choice.
     */
    /**
     * Synthesize an input-navigation {@link RFeatureCall} for a bare attribute
     * reference inside a rule body whose attribute is a feature of the rule's
     * from-type. Returns {@code null} when the reference is not such a case (no
     * enclosing rule, no from-type, the from-type is not a data type, or the
     * attribute is not a feature of the from-type) so the caller falls through
     * to the variable path.
     *
     * <p>The synthesized receiver is {@code RSymbolReference("input")} carrying a
     * synthetic {@link RAttribute} typed as the rule's <em>from-type</em> — NOT
     * {@code attr.parent()}, which would be a supertype for inherited features.
     * {@link NavigationHandler} derives the lambda variable from this receiver
     * type, so the from-type is what yields the golden lambda name (e.g.
     * {@code settlementTerms} for from-type {@code SettlementTerms}). The
     * resolved feature on the synthesized call gives NavigationHandler the
     * generic type witness and the {@code map}/{@code mapC} choice.
     *
     * <p>The synthetic receiver itself re-enters {@code handle(RSymbolReference)}
     * when compiled, but its synthetic {@code "input"} attribute is not a feature
     * of the from-type, so this method returns {@code null} on that re-entry and
     * the receiver falls through to {@code MapperS.of(input)} as intended.
     */
    // public (was private) so the IR-routed compiler reuses this exact synthesizer for the
    // implicit-input navigation relabel (the lab's L-109e): a bare rule-from-type attribute
    // `attr` ⇒ `input -> attr`, routed through visitFeatureCall identically to this legacy
    // fallback; null off-case (the caller falls through to the item/variable path). The D43 seam.
    public RFeatureCall synthesizeImplicitInputNavigation(RSymbolReference expr, RAttribute attr) {
        RRule rule = HandlerHelper.findEnclosingRule(expr);
        if (rule == null || rule.fromType().isEmpty()) {
            return null;
        }
        RTypeCall fromTypeCall = rule.fromType().get();
        Optional<RNode> referenced = fromTypeCall.referencedType();
        if (referenced.isEmpty() || !(referenced.get() instanceof RDataType fromType)) {
            return null;
        }
        // Confirm attr really is the implicit-input feature the linker resolved
        // (identity match). Excludes bare refs that resolved to something other
        // than a feature of the from-type.
        if (HandlerHelper.findAttributeOnDataType(fromType, attr.name()) != attr) {
            return null;
        }

        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(buildImplicitInputReceiver(expr, fromTypeCall));
        fc.setFeatureName(attr.name());
        fc.setResolvedFeature(attr);
        fc.setParent(expr.parent());
        return fc;
    }

    /**
     * Coverage wave D (datarule): synthesize an instance-navigation
     * {@link RFeatureCall} for a linker-BOUND bare attribute reference inside a
     * DATA-TYPE condition (the wave-D type-condition scope binds them) whose
     * attribute is a feature of the condition's declaring type — the identity
     * guard ({@code findAttributeOnDataType == attr}) excludes shadowing
     * same-named symbols AND this branch's own synthetic receiver (whose
     * synthetic attribute is never the found one — the recursion guard).
     * Returns {@code null} so the caller falls through.
     */
    // public (was private) so the IR-routed compiler's #479 implicit-root shape witness re-synthesizes
    // the EXACT condition-instance equivalent this legacy arm renders (the L-109e reuse precedent).
    // Read-only: fresh nodes only (buildConditionInstanceReceiver resolves names, never compiles).
    // Null off-case unchanged (the caller falls through).
    public RFeatureCall synthesizeConditionInstanceNavigation(RSymbolReference expr, RAttribute attr,
            ExpressionCompiler compiler) {
        RCondition condition = HandlerHelper.findEnclosingTypeCondition(expr);
        if (condition == null || !(condition.parent() instanceof RDataType declaringType)) {
            return null;
        }
        if (HandlerHelper.findAttributeOnDataType(declaringType, attr.name()) != attr) {
            return null;
        }

        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(buildConditionInstanceReceiver(expr, declaringType, compiler));
        fc.setFeatureName(attr.name());
        fc.setResolvedFeature(attr);
        fc.setParent(expr.parent());
        return fc;
    }

    /**
     * Coverage wave D (datarule): the BY-NAME sibling of
     * {@link #synthesizeConditionInstanceNavigation} for a bare name the
     * type-condition scope left symbol-EMPTY (the scope is best-effort/silent —
     * a straggler miss recovers here instead of rendering the non-compiling
     * bare variable), the by-name analogue of
     * {@link #synthesizeImplicitItemBareNav}. Returns {@code null} when the
     * reference is not such a case (a closure-param name, an
     * {@link RAttribute}-BOUND symbol — the bound form routes through the
     * identity sibling, and this arm's own synthetic receiver must decline to
     * the variable path — no enclosing type-parented condition, or no
     * exact-name attribute on the declaring type).
     *
     * <p>The synthesized receiver mirrors {@link #buildImplicitInputReceiver}:
     * at condition-body top level, a synthetic {@code RSymbolReference} named
     * for the instance (lower-first type name, keyword-escaped — the
     * {@code executeDataRule} parameter) carrying a synthetic {@link RAttribute}
     * whose UNRESOLVED {@link RTypeCall} holds the declaring type's name text —
     * {@code NavigationHandler.resolveSymbolTypeName} reads the text only, so
     * the first-hop lambda variable derives from the declaring type
     * ({@code cash} → collision with the seeded parameter → {@code _cash});
     * {@code RTypeCall.referencedType()} on the unresolved synthetic returns
     * {@code Optional.empty()} (no workspace attach needed — every downstream
     * consumer null-declines). Inside an inline lambda, a synthetic
     * {@link RImplicitVariable} takes the lambda's own binding, exactly like the
     * rule-path receiver.
     */
    static RFeatureCall synthesizeConditionInstanceBareNav(RSymbolReference expr,
            ExpressionCompiler compiler) {
        String name = expr.name();
        if (name == null || isEnclosingClosureParam(expr, name)) {
            return null;
        }
        // An RAttribute-BOUND reference declines: the bound form routes through
        // the identity sibling (synthesizeConditionInstanceNavigation), and the
        // only OTHER attr-bound visitor here is this arm's OWN synthetic
        // instance receiver on its compile re-entry — which must fall to the
        // variable path (`MapperS.of(<instance>)`), and which would otherwise
        // RECURSE on any type carrying a self-named attribute (instance name ==
        // attribute name).
        if (expr.symbol().filter(RAttribute.class::isInstance).isPresent()) {
            return null;
        }
        RCondition condition = HandlerHelper.findEnclosingTypeCondition(expr);
        if (condition == null || !(condition.parent() instanceof RDataType declaringType)) {
            return null;
        }
        RAttribute attr = HandlerHelper.findAttributeOnDataType(declaringType, name);
        if (attr == null) {
            // A navigate-by-type choice-option name on the declaring type's
            // CHOICE supertype (`Basket is absent` on BasketConstituent — the
            // option projects as an attribute named by its type, rendered
            // through the lower-getter `.getBasket()`; golden
            // BasketConstituentBasketsOfBaskets). The same #207 recovery every
            // other navigation seat uses.
            attr = NavigationHandler.findChoiceSuperOption(declaringType, name, compiler);
        }
        if (attr == null) {
            return null;
        }

        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(buildConditionInstanceReceiver(expr, declaringType, compiler));
        fc.setFeatureName(attr.name());
        fc.setResolvedFeature(attr);
        fc.setParent(expr.parent());
        return fc;
    }

    /**
     * Build the head receiver for a synthesized condition-instance navigation
     * chain — the datarule analogue of {@link #buildImplicitInputReceiver}. At
     * condition-body top level the receiver is the instance parameter, rendered
     * {@code MapperS.of(<instance>)} via the variable path (the synthetic
     * attribute's type-call text feeds the lambda-variable naming).
     *
     * <p>Inside a lambda the receiver follows upstream's scope law: only an
     * IMPLICIT lambda ({@code extract (…)}) carries the item feature scope, and
     * the fork reached this arm with the DECLARING type's attribute (the linker's
     * condition scope — lambda items are linker-dark), so the item-rooted
     * receiver is byte-right only when the ITEM type declares the same name (the
     * shadowing overlap upstream resolves to the item's own feature). An
     * EXPLICIT-param lambda ({@code extract dp [ … ]}) has NO item feature scope
     * upstream — its bare names are the condition instance's features,
     * re-navigated from the {@code executeDataRule} parameter (golden drr
     * ESMAEMIRTransactionReportEMIR_VR_2121_01's
     * {@code MapperS.of(eSMAEMIRTransactionReport).<String>map(
     * "getInterconnectionPoint", …)} inside the {@code dp} lambda; VR_2077_05's
     * {@code centralCounterparty}). A NAMED then-step fn ({@code then extract
     * Name [ … ]}) keeps the item root like the implicit form — upstream's named
     * param does not rebind the implicit item (the #375 non-rebind law).
     */
    private static RExpression buildConditionInstanceReceiver(RSymbolReference expr,
            RDataType declaringType, ExpressionCompiler compiler) {
        RInlineFunction inline = enclosingConditionLambda(expr);
        if (inline != null) {
            boolean namedThenStepFn = !inline.isImplicit()
                    && !inline.paramNames().isEmpty()
                    && inline.parent() instanceof RExtractExpr namedStepX
                    && namedStepX.body() == inline
                    && nearestEnclosingInlineFunction(namedStepX) != null;
            boolean itemScope = inline.isImplicit() || namedThenStepFn;
            if (itemScope) {
                RDataType itemType = NavigationHandler.implicitItemDataType(expr, compiler);
                // An unresolvable item type keeps the pre-fix item root (no
                // negative evidence — the byte-frozen shadowing carriers).
                if (itemType == null
                        || HandlerHelper.findAttributeOnDataType(itemType, expr.name()) != null) {
                    RImplicitVariable item = new RImplicitVariable();
                    item.setSynthetic(true);
                    item.setParent(expr.parent());
                    return item;
                }
            }
        }
        return syntheticConditionInstanceRef(expr.parent(), declaringType.name());
    }

    /**
     * The condition-instance receiver core — a synthetic
     * {@link RSymbolReference} named for the instance parameter
     * ({@link HandlerHelper#conditionInstanceName}) carrying a synthetic
     * {@link RAttribute} whose UNRESOLVED {@link RTypeCall} holds the declaring
     * type's name text (lambda-variable naming reads the text; resolution
     * consumers see {@code Optional.empty()} and decline). Renders
     * {@code MapperS.of(<instance>)} through the variable path. PUBLIC for the
     * datarule generator's cardinality-twin synthesis (the one-of / choice
     * argument — {@code choice(MapperS.of(asset), …)}), which must produce the
     * IDENTICAL receiver form.
     *
     * @param parent   the node to parent the synthetic reference at (context
     *                 walks from inside the compile go through it)
     * @param typeName the declaring type's Rune name as written
     */
    public static RExpression syntheticConditionInstanceRef(RNode parent, String typeName) {
        RTypeCall typeCall = new RTypeCall();
        typeCall.setTypeName(typeName);
        RAttribute syntheticInstance = new RAttribute();
        syntheticInstance.setName(HandlerHelper.conditionInstanceName(typeName));
        syntheticInstance.setTypeCall(typeCall);
        RSymbolReference instanceRef = new RSymbolReference();
        instanceRef.setName(syntheticInstance.name());
        instanceRef.setParent(parent);
        instanceRef.setResolvedSymbol(syntheticInstance);
        return instanceRef;
    }

    /**
     * The nearest {@link RInlineFunction} between {@code start} and the
     * enclosing type condition, or {@code null} — the condition-body twin of
     * {@link #hasEnclosingRuleLambda} (stops at the {@link RCondition} instead
     * of the rule/function root). Bounded by
     * {@link HandlerHelper#PARENT_WALK_LIMIT}.
     */
    private static RInlineFunction enclosingConditionLambda(RNode start) {
        RNode cur = start.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) return inline;
            if (cur instanceof RCondition) return null;
            if (cur instanceof RRule || cur instanceof RFunction) return null;
            cur = cur.parent();
        }
        return null;
    }

    /**
     * Synthesize an implicit-ITEM navigation {@link RFeatureCall} for a bare
     * attribute reference inside an inline filter/extract/then/max/min lambda
     * (the shared walk's owners, widened at facet then_maxmin_item_typing —
     * that facet's then-body re-root rides exactly this route) whose attribute
     * is a feature of the lambda's ITEM type (facet filter_predicate_item_typing,
     * mechanism 2) — the inline-lambda analogue of
     * {@link #synthesizeImplicitInputNavigation}. Returns {@code null} when the
     * reference is not such a case (no enclosing inline function, the owning
     * list-op argument's item type does not resolve, or the linker-resolved
     * attribute is not the item type's feature) so the caller falls through to the
     * variable path.
     *
     * <p>The synthesized receiver is a synthetic {@link RImplicitVariable}, which
     * renders as the enclosing lambda's binding ({@code item}, or the explicit
     * parameter name) via {@link #handle(RImplicitVariable, ExpressionContext,
     * ExpressionCompiler)}; {@code NavigationHandler} derives the step's lambda
     * var from the SAME item-type resolution
     * ({@link NavigationHandler#implicitItemDataType}) this method gates on, and
     * the resolved feature gives it the generic witness + {@code map}/{@code mapC}
     * choice — so the guard and the rendering cannot disagree.
     *
     * <p>The identity guard ({@code findAttributeOnDataType(itemType, name) == attr})
     * mirrors {@link #synthesizeImplicitInputNavigation}: a bare reference the
     * linker resolved to anything OTHER than the item type's own feature — a
     * function input, output, alias or lambda-local, all distinct
     * {@link RAttribute} objects even when same-named (shadowing) — declines.
     * Regression-safe by construction: the pre-facet rendering of a firing case is
     * the bare non-compiling variable name, which no green file carries.
     */
    // public (was package-private) so the IR-routed compiler's #479 implicit-root shape witness
    // re-synthesizes the EXACT item-rooted equivalent this legacy arm renders (the L-109e
    // synthesizeImplicitInputNavigation precedent): the witness classifies each attrOutsideFunction
    // blocker by legacy's own synthesizer chain and adapts the equivalent to size the claimable
    // slice. Read-only: fresh nodes only, no compile calls. Null off-case unchanged.
    public static RFeatureCall synthesizeImplicitItemNavigation(RSymbolReference expr, RAttribute attr,
            ExpressionCompiler compiler) {
        RDataType itemType = NavigationHandler.implicitItemDataType(expr, compiler);
        if (itemType == null) {
            return null;
        }
        if (HandlerHelper.findAttributeOnDataType(itemType, attr.name()) != attr) {
            return null;
        }

        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(expr.parent());
        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(item);
        fc.setFeatureName(attr.name());
        fc.setResolvedFeature(attr);
        fc.setParent(expr.parent());
        return fc;
    }

    /**
     * facet lambda_item_body_coercion (arm B2, bare name): synthesize the
     * implicit-ITEM navigation for a bare {@link RSymbolReference} the linker
     * left symbol-EMPTY or bound to a TYPE node — the by-NAME analogue of
     * {@link #synthesizeImplicitItemNavigation} (which requires a Cat-9-bound
     * {@link RAttribute} symbol and keeps priority). Returns {@code null} —
     * the caller falls through to the variable path — when:
     * <ul>
     *   <li>the name is a closure param of any enclosing inline function (a
     *       legitimate Java lambda var — re-rooting to {@code item} would be
     *       wrong);</li>
     *   <li>the name resolves in function scope (input/output/alias — those
     *       heads correctly root at the variable/alias today, byte-frozen);</li>
     *   <li>the nearest enclosing inline function is not IMPLICIT (an
     *       explicit-param lambda binds its param, not {@code item} — upstream
     *       resolves implicit features only when an implicit variable is
     *       present);</li>
     *   <li>the gm-aware item type does not resolve, or carries no attribute
     *       with the exact name (choice projections are name-keyed by TYPE
     *       name, so {@code findAttributeOnDataType} is an exact-match gate).</li>
     * </ul>
     *
     * <p>Public since #524 for the IR blocker-probe census's BY-CALL read (the #523
     * {@code synthesizeImplicitItemChain} by-call precedent at the BARE-symbol seat):
     * the probe re-runs THIS ladder on the declined reference to name which legacy leg
     * renders it. Behaviour-inert — a visibility widening only.
     */
    public static RFeatureCall synthesizeImplicitItemBareNav(RSymbolReference expr,
            ExpressionCompiler compiler) {
        String name = expr.name();
        if (name == null || isEnclosingClosureParam(expr, name)) {
            return null;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
        if (enclosing != null && nameResolvesInFunctionScope(expr, enclosing, name)) {
            return null;
        }
        RInlineFunction nearest = nearestEnclosingInlineFunction(expr);
        // facet thenParamBareNavSynthesis (PR #375, B2): a NAMED then-step extract fn
        // joins the admit — upstream's named param does not rebind the implicit item
        // (the #364/#367 non-rebind law), so a symbol-EMPTY bare attribute inside
        // `X then extract Name [ … ]` still resolves against the PIPED item (golden
        // MessageID `thenArg.<WorkflowStep>map("getOriginatingWorkflowStep", …)` — the
        // linker leaves the `default`-arm read unbound and the pre-facet decline fell
        // to the non-compiling bare `MapperS.of(originatingWorkflowStep)` echo). The
        // WRAPPER-fn requirement keeps TOP-LEVEL named extracts declined (the 4 green
        // TechnicalRecordId rules root those reads at `input` via the Cat-9-bound
        // priority arm — they never reach this ladder); green-safe as ever: the bare
        // undefined-name echo never compiles, so no green file carries it.
        boolean namedThenStepFn = nearest != null
                && !nearest.isImplicit()
                && !nearest.paramNames().isEmpty()
                && nearest.parent() instanceof RExtractExpr namedStepX
                && namedStepX.body() == nearest
                && nearestEnclosingInlineFunction(namedStepX) != null;
        if (nearest == null || !(bindsImplicitItem(nearest) || namedThenStepFn)) {
            return null;
        }
        // facet rerootItemNav (PR #282): the INFERRED-type fallback (the 1-name bare-symbol sibling
        // of synthesizeImplicitItemChain's 2-name chain reroot) — a THEN-piped rule-body map-lambda
        // item type the structural walk leaves null is recovered from the lambda argument's inferred
        // element type, so a bare item-feature name (`cashSettlementTerms` / `partyId`) re-roots
        // `item.<T>map(C)("getX", …)` instead of the bogus bare `MapperS.of(<name>)` /
        // `MapperC.of(<name>)` (the variable-path fallthrough). Same green-safety argument: the bare
        // local never compiled, so no green file carries it. Monotone (structural walk wins first).
        // facet filterPredicateOperandNavReRoot (PR #341): the inferred fallback un-rule-scoped —
        // the #282 "function-body bare reroot" follow-on. A FUNCTION-path then-piped filter's
        // item type resolves through the same inferred walk (drr Extract_UTIPropietary's
        // TradeIdentifier), and the ladder's closure-param + function-scope declines ABOVE keep
        // every green function-local reference off this path (cp4 scan: 0 new divergents / 0
        // green movement, all 3 cells; 15 co-occupied waivered files move content-toward).
        // facet choiceOptionProjectionTypeId (v3.1 flip seat 32, law A1): a bare capitalised
        // CHOICE-OPTION name over a THEN-piped item whose inferred type IS the choice resolves
        // its attribute off the choice bridge, and asRDataType()'s copies carry no
        // referencedTypeId - so the option's wrapper never resolves and the NEXT hop loses
        // golden's `Type coercion` deref (drr 7.0-7.3 UATPI Leg1/Leg2, 48 hops). This seat -
        // and only this seat - takes the id-carrying bridge; the MEASURED discriminator is
        // `hasId=false` on the resolved attribute, 96 rows corpus-wide, all eight carriers,
        // ZERO green, both routes ([P32-SYNTHATTR]).
        RDataType itemType = NavigationHandler.implicitItemChoiceIdCarryingDataType(expr, compiler);
        // facet metaPathShortForm (PR #348) → seat 21 (facet metaFaceShortForm): a BARE
        // meta-FACE name over the implicit item (`filter scheme = …` — drr Extract_BondConnect
        // over a META-WRAPPER item; `min [ key ]` — drr CommodityCommodityLeg1/2 over a
        // type-level `[metadata key]` POJO item, CommodityPayout) synthesizes the SAME
        // feature-call shape the parser produces for an explicit `item -> <face>` read
        // (receiver = synthetic implicit item, resolvedFeature EMPTY, the metaType binding
        // carried) — the NavigationHandler metaFeatureShortFormOrNull arm renders upstream's
        // a->a short form from there, ONE render seat for both source shapes (upstream
        // ExpressionGenerator.xtend:1113-1116: a RosettaSymbolReference → RosettaMetaType takes
        // the same metaCall). Admission = the name is a face AND (the parser bound it to the
        // metaType, OR — scheme/reference ONLY, the pre-seat #285 population — the
        // implicitItemArgMeta walk PROVES a meta wrapper item; seat-21 review SF-4: the type-level-face
        // leg was redundant with the binding — a bare face over a `[metadata key]` item IS bound —
        // and an unbound face would reach the nav seat with no metaType to render). Hoisted out of the
        // `itemType == null` branch (the pre-seat arm ran only for a type-less wrapper item, so
        // a typed POJO item fell to the non-compiling bare `MapperS.of(key)` echo — no golden
        // carries it, green-safe). A real attribute of the item named like a face is
        // resolved by the walks below FIRST (the shadowing law) — this arm runs only when the
        // name is NOT an attribute of the item.
        if (HandlerHelper.isMetaFeatureName(name)
                && (itemType == null || HandlerHelper.findAttributeOnDataType(itemType, name) == null)) {
            RImplicitVariable metaItem = new RImplicitVariable();
            metaItem.setSynthetic(true);
            metaItem.setParent(expr.parent());
            com.regnosys.rosetta.ast.types.RMetaType boundFace = HandlerHelper.boundMetaType(expr);
            boolean admitted = boundFace != null
                    || (("scheme".equals(name) || "reference".equals(name))
                            && NavigationHandler.implicitItemArgMeta(metaItem, compiler) != null);
            if (admitted) {
                RFeatureCall metaFc = new RFeatureCall();
                metaFc.setReceiver(metaItem);
                metaFc.setFeatureName(name);
                metaFc.setParent(expr.parent());
                if (boundFace != null) {
                    metaFc.setResolvedFeatureNode(boundFace);
                }
                return metaFc;
            }
        }
        if (itemType == null) {
            return null;
        }
        // v3.1 flip seat 2 (LADDER RETIREMENT) — authority-first: when the C1
        // authority slot (resolvedFeatureNode, spec R7.1) bound this bare name to a
        // CHOICE OPTION of the implicit item, project THAT node (the seat-1
        // id+attach pair) instead of re-deriving structurally below. The structural
        // by-name walks find the same-NAMED projection but its typeCall is an
        // id-less deep copy, so in a cell whose option types live in a NON-generated
        // dependency namespace the witness import never registers (the E1
        // import-only class at the BARE seat — IsSingleCommodityPayoutProduct's
        // entire pre-seat diff). Same-workspace cells resolve to the SAME
        // declaration either way (the census's SAME_DENOTATION predicate), so green
        // renders cannot move. Empty/non-option authority → the walks below
        // unchanged: the flip's authority-first-with-legacy-fallback contract.
        // Theoretical new admission (indep-review audit, corpus-unrealized at the
        // seat-2 rings): an option-bound authority where BOTH structural walks
        // would decline makes this synth fire where it declined — green-safe by
        // the ladder's own corpus law (the pre-seat fallthrough is the
        // non-compiling bare-name echo, which no golden carries).
        RAttribute attr = NavigationHandler.authorityChoiceOptionAttr(
                expr.resolvedFeatureNode().orElse(null), compiler);
        if (attr == null) {
            attr = HandlerHelper.findAttributeOnDataType(itemType, name);
        }
        // facet itemNavChoiceSuperOption (PR #347): the item type EXTENDS a CHOICE and the
        // bare name is one of the choice's (type-named, capitalized) options — the #207
        // witnessDrop law at the bare-symbol re-root seat (the P347-R1 probe:
        // `itemType=BasketConstituent attr=false` for `Asset`/`Index`; `type
        // BasketConstituent extends Observable`, a choice, whose options live on
        // choiceSuperType() — invisible to the data-supertype walk). The same
        // findChoiceSuperOption recovery fallbackResolveFeature applies after its own
        // direct lookup. Green-safe by the ladder + the corpus law: zero goldens carry
        // `MapperS.of(<CapitalizedName>)` (the pre-facet render), so every firing carrier
        // is an already-waivered non-compiling mismatch (cdm6 ObservableIsCommodity /
        // Qualify_ForeignExchange_ParameterReturnCorrelation).
        if (attr == null) {
            attr = NavigationHandler.findChoiceSuperOption(itemType, name, compiler);
        }
        if (attr == null) {
            return null;
        }

        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(expr.parent());
        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(item);
        fc.setFeatureName(attr.name());
        fc.setResolvedFeature(attr);
        fc.setParent(expr.parent());
        return fc;
    }

    /**
     * facet onlyexists_implicit_item — the synthesized receiver for a
     * CHAIN-LESS bare onlyExists element: the bare synthetic implicit item
     * plus the gm-aware item type {@code ExistenceHandler} enumerates the
     * allFields list from.
     */
    record ImplicitItemOnlyExistsReceiver(RImplicitVariable receiver, RDataType itemType) {}

    /**
     * facet onlyexists_implicit_item: synthesize the bare IMPLICIT-ITEM
     * receiver for a CHAIN-LESS onlyExists element
     * ({@code foreignExchange only exists} inside an extract lambda) — the
     * receiver-less analogue of {@link #synthesizeImplicitItemBareNav} with
     * the SAME decline ladder, except the element's root names the SELECTED
     * attribute (not a navigation step), so the synthesis returns the bare
     * synthetic {@link RImplicitVariable} (upstream {@code caseOnlyExists}
     * renders the implicit variable as the shared parent —
     * ExpressionGenerator.xtend lines 1014-1021) plus the item type. The
     * synthetic item is parented INSIDE the lambda ({@code site.parent()}),
     * so {@code enclosingLambdaBinding} renders the lambda's own binding
     * ({@code item} for implicit forms). NOTE: in a then-BODY context the
     * synthetic item would render the {@code thenArg} local instead (the
     * {@code handle(RImplicitVariable)} scope {@code thenArgRefFor} check wins
     * by order) — plausibly upstream-consistent (the implicit variable IS the
     * thenArg synonym there) but carrier-unverified; both corpus carriers are
     * extract lambdas. Returns {@code null} — the caller
     * falls through to the legacy placeholder — on the
     * {@link #synthesizeImplicitItemBareNav} ladder: a closure-param or
     * function-scope name match, a non-implicit (or absent) enclosing inline
     * function, an unresolvable item type, or a root that is not an attribute
     * of the item type.
     */
    static ImplicitItemOnlyExistsReceiver synthesizeImplicitItemOnlyExistsReceiver(
            RExpression site, String rootName, ExpressionCompiler compiler) {
        if (rootName == null || isEnclosingClosureParam(site, rootName)) {
            return null;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(site);
        if (enclosing != null && nameResolvesInFunctionScope(site, enclosing, rootName)) {
            return null;
        }
        RInlineFunction nearest = nearestEnclosingInlineFunction(site);
        if (nearest == null || !bindsImplicitItem(nearest)) {
            return null;
        }
        RDataType itemType = NavigationHandler.implicitItemDataType(site, compiler);
        if (itemType == null) {
            return null;
        }
        if (HandlerHelper.findAttributeOnDataType(itemType, rootName) == null) {
            return null;
        }

        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(site.parent());
        return new ImplicitItemOnlyExistsReceiver(item, itemType);
    }

    /**
     * facet lambda_item_body_coercion (arm B2, disguised chain): synthesize the
     * implicit-ITEM navigation chain {@code item -> head -> leaf} for an
     * in-lambda disguised {@link REnumValueRef} ({@code head -> feature}) whose
     * HEAD is an attribute of the lambda's ITEM type — the chain analogue of
     * {@link #synthesizeImplicitItemBareNav} with the same decline ladder. When
     * the resolver DID bind an {@link REnumValueRef.AttributeChain} (the Cat-10
     * inline-body binding the {@link #synthesizeImplicitInputChain} identity
     * guard deliberately rejects), the bound head must be the SAME attribute the
     * item type resolves by name (identity guard against shadowing) and the
     * bound leaf is reused; otherwise both steps resolve by name through the
     * gm-aware walk ({@code attributeToDataType} narrows a choice-typed head to
     * its options projection).
     */
    // public (was private) so the IR-routed compiler's #479 implicit-root shape witness walks the
    // SAME fall-through chain legacy walks for an attributeChain blocker whose rule-input chain
    // synthesizer nulls (the L-109e reuse precedent): itemChain → caseNarrowed → generic. Read-only:
    // resolution walks + fresh nodes only, no compile calls. Null off-case unchanged.
    public RFeatureCall synthesizeImplicitItemChain(REnumValueRef evr,
            ExpressionCompiler compiler) {
        String headName = evr.enumName();
        String leafName = evr.valueName();
        if (headName == null || leafName == null || isEnclosingClosureParam(evr, headName)) {
            return null;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(evr);
        // facet aliasSelfShadowDisguisedChain (PR #389): the #372 F-delta-5
        // self-reference law at the DISGUISED-chain seat — a head name matching the
        // ENCLOSING shortcut ITSELF is always the shadowed ITEM feature (rune aliases
        // cannot recurse; upstream resolves implicit-item features FIRST), so the
        // function-scope decline must not count that match. Golden cdm6
        // MapPayerReceiverToAccountPartyReference: `payerPartyReference -> href`
        // inside the payerPartyReference alias renders item.<PartyReference>map(
        // "getPayerPartyReference", …).<String>map("getHref", …); the mis-bound
        // self-CALL rendered the infinitely-recursive `payerPartyReference(…)
        // .map("getHref", …)` — non-compiling, zero green carriers. Any OTHER-name
        // scope match keeps the byte-frozen decline.
        // facet aliasSwitchBareCaseNav (seat 23, law A1): the local self-shortcut exemption
        // retired to the ONE site-aware scope test (LAW 69 — the same law at every arm).
        if (enclosing != null && nameResolvesInFunctionScope(evr, enclosing, headName)) {
            return null;
        }
        RInlineFunction nearest = nearestEnclosingInlineFunction(evr);
        if (nearest == null || !bindsImplicitItem(nearest)) {
            return null;
        }
        // facet rerootItemNav (PR #282): the INFERRED-type fallback is RULE-SCOPED — only a
        // drr rule-body map-lambda then-piped item type is recovered from the inferred type. A
        // FUNCTION-body disguised nav keeps the pre-#282 structural-only resolution (declining to
        // the legacy bare form) so the FUNCTION tail stays byte-IDENTICAL (the #232 shared-seat
        // lesson; the function-body reroot is a documented follow-on). The lambda-var-naming half
        // (resolveLambdaVarName) is already from-typed-rule-scoped, so both seats coordinate.
        RDataType itemType = HandlerHelper.findEnclosingRule(evr) != null
                ? NavigationHandler.implicitItemDataTypeOrInferred(evr, compiler)
                : NavigationHandler.implicitItemDataType(evr, compiler);
        if (itemType == null) {
            return null;
        }
        RAttribute headAttr = HandlerHelper.findAttributeOnDataType(itemType, headName);
        if (headAttr == null) {
            return null;
        }
        RAttribute leafAttr;
        Optional<REnumValueRef.AttributeChain> bound = evr.resolvedAttributeChain();
        if (bound.isPresent() && bound.get().attribute() != null) {
            if (bound.get().attribute() != headAttr) {
                return null;
            }
            leafAttr = bound.get().feature();
        } else {
            RDataType headType = NavigationHandler.attributeToDataType(headAttr, compiler);
            leafAttr = headType == null ? null
                    : HandlerHelper.findAttributeOnDataType(headType, leafName);
        }
        if (leafAttr == null) {
            // facet maxmin_body_coercion (arm C): a disguised chain whose LEAF is
            // the `date` RECORD feature (`max [ timestamp -> date ]`) has no
            // RAttribute leaf to bind — the head's declared type is a built-in
            // record (zonedDateTime/dateTime), not a data type, so the by-name
            // leaf resolution above is structurally null. Synthesize the chain
            // with the RESOLVED head + the leaf's featureName UNSET-resolved: the
            // leaf re-enters NavigationHandler.handle(RFeatureCall), whose
            // tryRecordFeatureNav renders the upstream RecordJavaUtil form
            // (`.<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))`) off the
            // typed head. Admission reads the SAME resolveReceiverRType +
            // isDateRecordFeature gate the record nav itself reads — on the SAME
            // synthesized head node — so this arm fires exactly when the record
            // nav will (never the invented-getter path off a resolved head).
            // Green-safe: previously this shape DECLINED to synthesizeFeatureCall's
            // function-scope-only head resolution, rendering the non-compiling
            // `.map("getDate", … -> ….getDate())` getter (ZonedDateTime carries no
            // getDate()) — waivered space only (drr GetValuation).
            return synthesizeRecordLeafChain(evr, headAttr, leafName, compiler);
        }

        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(evr.parent());
        RFeatureCall headCall = new RFeatureCall();
        headCall.setReceiver(item);
        headCall.setFeatureName(headAttr.name());
        headCall.setResolvedFeature(headAttr);
        headCall.setParent(evr.parent());
        RFeatureCall leafCall = new RFeatureCall();
        leafCall.setReceiver(headCall);
        leafCall.setFeatureName(leafAttr.name());
        leafCall.setResolvedFeature(leafAttr);
        leafCall.setParent(evr.parent());
        return leafCall;
    }

    /**
     * facet maxmin_body_coercion (arm C): synthesize {@code item -> head -> leaf}
     * for a record-feature LEAF — the head {@link RFeatureCall} carries its
     * resolved {@link RAttribute}, the leaf carries only its feature name (a
     * record feature has no {@code RAttribute}; {@code tryRecordFeatureNav}
     * renders it from the head's resolved record type). Returns {@code null} —
     * the caller preserves the pre-facet decline — unless the head's type
     * resolves to a {@code dateTime}/{@code zonedDateTime} record AND the leaf
     * is the {@code date} feature (the shared
     * {@link NavigationHandler#isDateRecordFeature} gate, read through the SAME
     * {@link NavigationHandler#resolveReceiverRType} walk on the SAME synthesized
     * head node the record nav reads).
     */
    private static RFeatureCall synthesizeRecordLeafChain(REnumValueRef evr,
            RAttribute headAttr, String leafName, ExpressionCompiler compiler) {
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null) {
            return null;
        }
        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(evr.parent());
        RFeatureCall headCall = new RFeatureCall();
        headCall.setReceiver(item);
        headCall.setFeatureName(headAttr.name());
        headCall.setResolvedFeature(headAttr);
        headCall.setParent(evr.parent());
        if (!(NavigationHandler.resolveReceiverRType(headCall, gm) instanceof RRecordType rt)
                || !NavigationHandler.isDateRecordFeature(leafName, rt)) {
            return null;
        }
        RFeatureCall leafCall = new RFeatureCall();
        leafCall.setReceiver(headCall);
        leafCall.setFeatureName(leafName);
        leafCall.setParent(evr.parent());
        return leafCall;
    }

    /**
     * facet lambda_item_body_coercion (arm B2): true when an inline function
     * binds the IMPLICIT item — the elided no-bracket form
     * ({@code isImplicit()}) or a bracketed lambda with ZERO declared params
     * ({@code extract [ ... ]}, which still binds {@code item}; only an
     * explicit-param lambda binds its param instead).
     */
    private static boolean bindsImplicitItem(RInlineFunction inline) {
        return inline.isImplicit() || inline.paramNames().isEmpty();
    }

    /**
     * facet implicitInputAttrDisguiseHead (seat 28, law 11): synthesize the
     * {@code input -> head -> leaf} navigation for a rule-body disguised {@link REnumValueRef}
     * whose HEAD names an attribute of the enclosing rule's FROM-TYPE and whose LEAF is a
     * feature (attribute OR choice option - {@link NavigationHandler#attributeToDataType}
     * narrows a choice-typed head to its options projection) of that attribute's type. The
     * by-NAME twin of the Cat-10 bound-chain arm, which needs a parser-bound AttributeChain
     * that a CHOICE-OPTION leaf never produces ({@code chainBound=false} at every carrier -
     * PROBE28-F15d).
     *
     * <p>Declines (the caller's {@code synthesizeFeatureCall} fall-through, today's bytes)
     * for a closure-param head, a head that resolves in the enclosing FUNCTION scope, no
     * enclosing rule, a non-data-type from-type, a head that is not a from-type feature, and
     * an unresolvable leaf - the same decline ladder the sibling synthesis arms carry.
     */
    // v3.1 flip seat 30, law 3 (facet disguisedRenderChainCardinality): package-private
    // (was private) so the CARDINALITY half - NavigationHandler.chainProvesMulti's
    // REnumValueRef arm, rung A - consults the SAME synthesis this render arm performs
    // instead of re-deriving the rule-from-type head/leaf resolution (LAW 69, the
    // two-halves-agree law: the arity the walk reports and the `.mapC(` the render emits
    // are then one resolution). Read-only - resolution walks plus FRESH nodes only, no
    // compile calls, no scope mutation - so the cardinality consult cannot perturb the
    // render. The null off-case is unchanged and the ONE pre-existing caller
    // (handle(REnumValueRef)) is untouched.
    static RFeatureCall synthesizeImplicitInputChainByName(REnumValueRef evr,
            ExpressionCompiler compiler) {
        String headName = evr.enumName();
        String leafName = evr.valueName();
        if (headName == null || leafName == null || isEnclosingClosureParam(evr, headName)) {
            return null;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(evr);
        if (enclosing != null && nameResolvesInFunctionScope(evr, enclosing, headName)) {
            return null;
        }
        RRule rule = HandlerHelper.findEnclosingRule(evr);
        if (rule == null || rule.fromType().isEmpty()) {
            return null;
        }
        RTypeCall fromTypeCall = rule.fromType().get();
        Optional<RNode> referenced = fromTypeCall.referencedType();
        if (referenced.isEmpty() || !(referenced.get() instanceof RDataType fromType)) {
            return null;
        }
        RAttribute headAttr = HandlerHelper.findAttributeOnDataType(fromType, headName);
        if (headAttr == null) {
            return null;
        }
        RDataType headType = NavigationHandler.attributeToDataType(headAttr, compiler);
        RAttribute leafAttr = headType == null ? null
                : HandlerHelper.findAttributeOnDataType(headType, leafName);
        if (leafAttr == null) {
            return null;
        }

        // input -> head
        RFeatureCall headCall = new RFeatureCall();
        headCall.setReceiver(buildImplicitInputReceiver(evr, fromTypeCall));
        headCall.setFeatureName(headAttr.name());
        headCall.setResolvedFeature(headAttr);
        headCall.setParent(evr.parent());
        // (input -> head) -> leaf
        RFeatureCall leafCall = new RFeatureCall();
        leafCall.setReceiver(headCall);
        leafCall.setFeatureName(leafAttr.name());
        leafCall.setResolvedFeature(leafAttr);
        leafCall.setParent(evr.parent());
        return leafCall;
    }

    /**
     * facet lambda_item_body_coercion (arm B2): true when {@code name} is a
     * declared closure PARAM of any inline function enclosing {@code start} —
     * those are real Java lambda variables the item synthesis must not re-root
     * (the T10-lever-11 walk). Bounded like every other parent walk.
     */
    private static boolean isEnclosingClosureParam(RNode start, String name) {
        return enclosingClosureParamOwner(start, name) != null;
    }

    /**
     * The NEAREST enclosing inline function declaring {@code name} as a closure
     * param (the Java-shadowing owner of the rendered identifier), or
     * {@code null}. The owner variant of {@link #isEnclosingClosureParam} —
     * facet mapitem_ctor_wrap (mechanism 4) reads the owner's OPERATION to
     * decline then-declared params (the one closure-param shape whose runtime
     * value can be a {@code MapperC}). Package-visible since PR #326 (facet
     * containsOperandCardinalityWrap F1b): {@code SetOperationHandler}'s
     * contains-operand closure-param unwrap shares the same walk + then-decline.
     * Public since v3.2 seat 13 (#634 round 1, the code-quality seat's SF-1):
     * {@code FunctionAliasHelper.enclosingClosureParameterOwner} - site R4's witness -
     * reads the SAME walk from the function package instead of a second copy with a
     * literal bound.
     */
    public static RInlineFunction enclosingClosureParamOwner(RNode start, String name) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline && inline.paramNames().contains(name)) {
                return inline;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * facet thenParamCollisionEscape (PR #375, B1b): the rendered identifier for an
     * EXPLICIT then-step extract param — {@code _<name>} exactly when the raw param
     * name equals the piped element's own type SIMPLE NAME, else the raw name.
     *
     * <p>Upstream's identifier-vs-class collision escape: a restructured then step
     * hoists {@code final MapperS<Product> thenArg = …} BEFORE the step lambda, so a
     * param named {@code Product} collides with the referenced class and renders
     * {@code _Product} (golden DTCC_OptionTypeRule — the ONE escaped carrier in the
     * corpus-complete 7-file Capitalized-param population; MessageID's
     * {@code ReportableEvent} param over a {@code TransactionReportInstruction} pipe
     * and the 4 green top-level TechnicalRecordId extracts all keep the raw name).
     * Render-truth gate: the named-extract WRAPPER fn must carry a LIVE #350
     * {@code thenArgRefFor} binding with a compiled Mapper type — only then does the
     * colliding hoisted decl provably exist in the render. Unbound (runtime
     * {@code .then(}) shapes, implicit fns and non-first params keep the raw name.
     */
    static String thenStepParamRenderName(RInlineFunction owner, String rawName,
            JavaStatementScope scope, JavaTypeUtil tu) {
        if (owner == null || rawName == null || scope == null || tu == null
                || owner.isImplicit() || owner.paramNames().isEmpty()
                || !rawName.equals(owner.paramNames().get(0))) {
            return rawName;
        }
        if (!(owner.parent() instanceof RExtractExpr namedExtract)
                || namedExtract.body() != owner) {
            return rawName;
        }
        RInlineFunction wrapper = nearestEnclosingInlineFunction(namedExtract);
        // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — VERDICT-MOVED
        // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar; S34): the channel is STRUCTURALLY
        // UNREACHABLE and the EXISTENCE half has no channel at all. c9 census, 15,829 arrivals
        // (5,643 / 5,643 / 4,543), wrapperFound=true 6,741, bound=true 5,541, escaped=true at 105:
        // wsReachable=false at 15,829/15,829 and the inferred simple name unavailable at
        // 15,829/15,829 — this method is static, holds no ExpressionCompiler and no GeneratorModel,
        // so the proposed workspace read cannot be called without threading a compiler in from its
        // four caller frames. THE MEASURED UNREACHABILITY IS THE FINDING. The NAME half is
        // self-consistent (rawName == elemSimpleName at 105 of the 5,541 arrivals carrying both,
        // 1.89%, and escaped=true at exactly those 105) and the triage itself concedes the second
        // half. A wrong answer here moves an IDENTIFIER on both routes, so the bar is higher.
        JavaExpression piped = wrapper == null ? null : scope.thenArgRefFor(wrapper);
        if (piped == null || piped.getExpressionType() == null) {
            return rawName;
        }
        JavaType elem = tu.getItemType(piped.getExpressionType());
        if (elem instanceof JavaClass<?> elemClass
                && rawName.equals(elemClass.getSimpleName())) {
            return "_" + rawName;
        }
        return rawName;
    }

    /**
     * v3.1 flip seat 30, law 3 (facet disguisedRenderChainCardinality): the B2 arm's
     * CLOSURE-PARAM decline, exposed so the CARDINALITY half consults it BY CALL.
     * {@code NavigationHandler.chainProvesMulti}'s rung B may treat a disguised chain as
     * ITEM-rooted only where {@link #synthesizeImplicitItemChain} would actually root it
     * there; a closure-param head is a real Java lambda variable the synthesis refuses to
     * re-root, so the render falls through to {@link #synthesizeFeatureCall} and emits
     * {@code MapperS.of(<headName>)} — a SINGLE root that must never ride the then-pipe.
     */
    static boolean disguisedHeadIsClosureParam(REnumValueRef evr) {
        return evr != null && evr.enumName() != null
                && isEnclosingClosureParam(evr, evr.enumName());
    }

    /**
     * v3.1 flip seat 30, law 3 (facet disguisedRenderChainCardinality): the B2 arm's
     * FUNCTION-SCOPE decline, exposed so the CARDINALITY half consults it BY CALL — the
     * SAME site-aware read {@link #synthesizeImplicitItemChain} performs on the SAME node,
     * so the two halves cannot drift apart (LAW 69, the two-halves-agree law). It covers
     * inputs, the output AND shortcuts — a strict superset of
     * {@code NavigationHandler.resolveDisguisedRootAttribute}'s input/output/dispatch-base
     * match — and carries the #389 self-shadow exemption, so a head naming its OWN
     * enclosing alias still reads as the shadowed ITEM feature.
     *
     * <p><b>Why it is load-bearing.</b> When this answers true the render roots the chain
     * at {@code MapperS.of(<headName>)}, which is SINGLE whatever pipe encloses it, so a
     * cardinality rung that read the enclosing then-pipe there would claim MULTI for a
     * genuinely single chain — the seat-30 {@code b3} shape ({@code header -> msgIdent}
     * inside a then body over a MULTI pipe).
     */
    static boolean disguisedHeadResolvesInFunctionScope(REnumValueRef evr) {
        if (evr == null || evr.enumName() == null) {
            return false;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(evr);
        return enclosing != null
                && nameResolvesInFunctionScope(evr, enclosing, evr.enumName());
    }

    /**
     * facet aliasSwitchBareCaseNav (seat 23, law A1): the SITE-aware scope test — the parser's
     * alias self-scope filter ({@code LexicalResolutionPass.applyScopeFilters}, facet
     * {@code aliasSelfScopeFilter} #453, vendored {@code RosettaScopeProvider:401-406}): a
     * ShortcutDeclaration REMOVES ITS OWN NAME from its parent scope, so inside {@code alias x: … x
     * …} the bare {@code x} is never the alias — it falls through to the implicit ITEM's feature.
     * The generator's scope test consults the same law (LAW 69 — the render's decline mirrors the
     * linker's binding): the enclosing alias's own name is NOT a function-scope hit for a reference
     * inside that alias. ONE consult for every bare-name / chain-head synthesis arm (the five
     * callers; the 2-name item-chain arm had carried this exemption LOCALLY as
     * {@code headIsSelfShortcut} — retired to this one read). Lambda params sit below the alias
     * boundary upstream and stay the callers' separate closure-param decline. The seat-23 runtime
     * probe instrumented THREE of the five callers (the implicit-item bare nav, the case-narrowed
     * chain and the case-narrowed bare nav) and found the alias-own-name class at exactly the 14
     * carrier sites there — the cdm 6.21+ {@code MapFloatingRateMultiplerScheduleToPriceWithLocation}
     * / {@code MapSpreadScheduleToPriceWithLocation} aliases named AS the case attribute they read
     * (2 sites × 7 cells). The other two callers are covered without a probe line: the 2-name
     * item-chain arm BY CONSTRUCTION (its retired local {@code headIsSelfShortcut} computed this
     * exact predicate), the only-exists receiver arm BY MEASUREMENT (the full 275-row matrix on
     * both routes — ZERO files entered the band, both rings EXACT); mutation a1-ii anchors all
     * five at this one read.
     */
    private static boolean nameResolvesInFunctionScope(RNode site, RFunction func, String name) {
        RShortcut enclosingAlias = HandlerHelper.findEnclosingShortcut(site);
        if (enclosingAlias != null && name.equals(enclosingAlias.name())) {
            return false;
        }
        return nameResolvesInFunctionScope(func, name);
    }

    /**
     * facet lambda_item_body_coercion (arm B2): true when {@code name} matches a
     * function input, the output, or an alias — heads that correctly root at the
     * variable/alias rendering today (byte-frozen decline). Unlike
     * {@link #resolveNameInFunction}, a SHORTCUT match is distinguishable from a
     * no-match here (both return {@code null} there). Callers consult the SITE-aware
     * overload above (the alias self-scope law); this is its function-scope half.
     */
    private static boolean nameResolvesInFunctionScope(RFunction func, String name) {
        for (RAttribute input : func.inputs()) {
            if (name.equals(input.name())) {
                return true;
            }
        }
        if (func.output().isPresent() && name.equals(func.output().get().name())) {
            return true;
        }
        for (RShortcut shortcut : func.shortcuts()) {
            if (name.equals(shortcut.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Synthesize the {@code input -> attribute -> feature} navigation chain for a
     * rule-body {@link REnumValueRef} whose {@link REnumValueRef.AttributeChain}
     * the resolver (D39 Category 10) bound to features of the rule's implicit
     * input. Returns {@code null} when the chain is the one-segment closure-param
     * shape ({@code attribute() == null} — a lambda-local, not an input feature)
     * or when there is no enclosing rule with a data-type from-type, so the caller
     * falls through to {@link #synthesizeFeatureCall}.
     *
     * <p>Mirrors {@link #synthesizeImplicitInputNavigation}: the receiver is a
     * synthetic {@code RSymbolReference("input")} typed as the rule's from-type, so
     * {@link NavigationHandler} derives the golden lambda variable (from-type name)
     * for the first step and the resolved-feature type for subsequent steps. The
     * resolved {@link RAttribute}s on each synthesized feature call give
     * NavigationHandler the generic type witness, {@code map}/{@code mapC} choice,
     * and meta wrapper.
     */
    // public (was private) so the IR-routed compiler's #478 nav-gate shape witness re-synthesizes the
    // EXACT equivalent chain this legacy branch renders (the L-109e synthesizeImplicitInputNavigation
    // precedent): the witness adapts the equivalent to measure which attributeChain blockers the IR
    // route could claim byte-identically. Null off-case unchanged (the caller falls through).
    public RFeatureCall synthesizeImplicitInputChain(
            REnumValueRef expr, REnumValueRef.AttributeChain chain) {
        RAttribute attr = chain.attribute();
        if (attr == null) {
            // One-segment closure-param shape: the leading name is a lambda-local,
            // not a feature of the implicit input — not an input-navigation chain.
            return null;
        }
        RRule rule = HandlerHelper.findEnclosingRule(expr);
        if (rule == null || rule.fromType().isEmpty()) {
            return null;
        }
        RTypeCall fromTypeCall = rule.fromType().get();
        Optional<RNode> referenced = fromTypeCall.referencedType();
        if (referenced.isEmpty() || !(referenced.get() instanceof RDataType fromType)) {
            return null;
        }
        // The chain head must be a feature of the rule's from-type. Category 10
        // also binds AttributeChains inside chain/inline bodies whose leading
        // attribute belongs to the inline ITEM type, not the rule input; for
        // those, synthesizing `input -> attr -> feature` would be wrong navigation.
        // Mirror synthesizeImplicitInputNavigation's identity guard so only chains
        // genuinely rooted at the rule input render here; inline-body chains fall
        // through to synthesizeFeatureCall's feature-call rendering.
        if (HandlerHelper.findAttributeOnDataType(fromType, attr.name()) != attr) {
            return null;
        }

        // input -> attribute
        RFeatureCall attrCall = new RFeatureCall();
        attrCall.setReceiver(buildImplicitInputReceiver(expr, fromTypeCall));
        attrCall.setFeatureName(attr.name());
        attrCall.setResolvedFeature(attr);
        attrCall.setParent(expr.parent());

        // (input -> attribute) -> feature
        RFeatureCall featCall = new RFeatureCall();
        featCall.setReceiver(attrCall);
        featCall.setFeatureName(chain.feature().name());
        featCall.setResolvedFeature(chain.feature());
        featCall.setParent(expr.parent());
        return featCall;
    }

    /**
     * Build the head receiver for a synthesized implicit-input navigation chain.
     *
     * <p>At rule-body top level the receiver is the rule input, rendered
     * {@code MapperS.of(input)} — a synthetic {@code RSymbolReference("input")}
     * typed as the rule's from-type, so {@link NavigationHandler} derives the
     * golden lambda variable from that type. Inside an {@code extract}/{@code filter}
     * lambda over the rule input, however, the implicit receiver is the lambda's
     * {@code item}, NOT the rule input (engine PR #4 / facet C): golden renders the
     * inner chain head as the bare lambda binding (e.g.
     * {@code item.<T>map("getX", ...)}), not
     * {@code MapperS.of(input).<T>map("getX", ...)}. A synthetic
     * {@link RImplicitVariable} renders {@code item} via
     * {@link #handle(RImplicitVariable, ExpressionContext, ExpressionCompiler)}'s
     * in-lambda path.
     *
     * <p>Item-type equals from-type for the cases this fires on: the caller's
     * identity guard ({@code findAttributeOnDataType(fromType, name) == attr})
     * admits only chains whose head attribute is a feature of the rule's from-type,
     * so {@link NavigationHandler#resolveLambdaVarName} derives the first lambda
     * variable from the enclosing rule's from-type. General nested-lambda
     * item-typing (item-type != from-type) is out of scope (engine PR #4 spec).
     */
    private static RExpression buildImplicitInputReceiver(RExpression expr, RTypeCall fromTypeCall) {
        if (hasEnclosingRuleLambda(expr)) {
            RImplicitVariable item = new RImplicitVariable();
            item.setSynthetic(true);
            item.setParent(expr.parent());
            return item;
        }
        RAttribute syntheticInput = new RAttribute();
        syntheticInput.setName("input");
        syntheticInput.setTypeCall(fromTypeCall);
        RSymbolReference inputRef = new RSymbolReference();
        inputRef.setName("input");
        inputRef.setParent(expr.parent());
        inputRef.setResolvedSymbol(syntheticInput);
        return inputRef;
    }

    /**
     * True when an {@link RInlineFunction} (extract/filter/list-op lambda) lies
     * between {@code start} and the enclosing {@link RRule} — i.e. {@code start}
     * is inside a rule lambda body rather than at rule-body top level. Stops at
     * the first {@link RRule} or {@link RFunction} root (a function body never
     * roots the implicit input at {@code item}). Bounded by
     * {@link HandlerHelper#PARENT_WALK_LIMIT}.
     */
    private static boolean hasEnclosingRuleLambda(RNode start) {
        RNode cur = start.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) return true;
            if (cur instanceof RRule) return false;
            if (cur instanceof RFunction) return false;
            cur = cur.parent();
        }
        return false;
    }

    /**
     * Synthesize a navigation {@link RFeatureCall} for a bare {@link RRule} reference
     * used as a navigation receiver — {@code SomeRule -> feature}, parsed as an
     * {@link REnumValueRef} whose {@code resolvedSymbol} is an {@link RRule}.
     *
     * <p>The receiver is an {@link RSymbolReference} carrying the {@link RRule} symbol,
     * so {@link #handle(RSymbolReference, ExpressionContext, ExpressionCompiler)}'s
     * bare-RRule branch renders {@code MapperS.of(<ruleField>.evaluate(<arg>))}; the
     * navigated feature is {@code valueName}. The resolved feature (from the bound
     * {@link REnumValueRef.AttributeChain}) is wired as the call's resolvedFeature so
     * {@link NavigationHandler} emits the generic type witness, the {@code map}/{@code
     * mapC} choice, and (via the feature's owner type) the golden lambda variable —
     * the rule's OUTPUT type, NOT the rule name.
     */
    // public (was private) so the IR-routed compiler's #501 chain-drain census re-synthesizes the
    // EXACT equivalent feature-call this legacy branch renders (the L-109e / #478 synthesizeFeatureCall
    // precedent): the census adapts the equivalent to measure which attributeChain.headSymbolNav
    // blockers a deep teach could claim — adapter admissibility only, probe-only caller.
    public RFeatureCall synthesizeRuleReceiverNavigation(REnumValueRef evr, RRule rule) {
        RSymbolReference ruleRef = new RSymbolReference();
        ruleRef.setName(evr.enumName());
        ruleRef.setResolvedSymbol(rule);
        ruleRef.setParent(evr.parent());

        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(ruleRef);
        fc.setFeatureName(evr.valueName());
        fc.setParent(evr.parent());
        evr.resolvedAttributeChain()
           .map(REnumValueRef.AttributeChain::feature)
           .ifPresent(fc::setResolvedFeature);
        return fc;
    }

    /**
     * The bare-FUNCTION analogue of {@link #synthesizeRuleReceiverNavigation}
     * (facet bareSymInvoke, PR #280). Builds an {@link RFeatureCall} whose receiver
     * is an {@link RSymbolReference} carrying the {@link RFunction} symbol, so the
     * receiver compiles through {@code handle(RSymbolReference)}'s bare-FUNCTION
     * branch ({@link #renderImplicitFunctionInvocation}) and the call navigates to
     * {@code valueName}.
     */
    // public (was private) — the #501 chain-drain census's bare-FUNCTION leg (the same probe-only
    // precedent as synthesizeRuleReceiverNavigation above).
    public RFeatureCall synthesizeFunctionReceiverNavigation(REnumValueRef evr, RFunction func) {
        RSymbolReference funcRef = new RSymbolReference();
        funcRef.setName(evr.enumName());
        funcRef.setResolvedSymbol(func);

        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(funcRef);
        fc.setFeatureName(evr.valueName());
        fc.setParent(evr.parent());
        // Parent the receiver to the feature call (NOT evr.parent()) so
        // renderImplicitFunctionInvocation can detect it is a navigation receiver
        // (parent is an RFeatureCall whose receiver == this ref) and wrap the bare
        // invocation in MapperS.of — golden navigates off the WRAPPED value
        // (MapperS.of(<fn>.evaluate(...)).<T>map(...)). The enclosing-rule / inline-lambda
        // parent walk still reaches the rule via fc.parent() == evr.parent().
        funcRef.setParent(fc);
        evr.resolvedAttributeChain()
           .map(REnumValueRef.AttributeChain::feature)
           .ifPresent(fc::setResolvedFeature);
        return fc;
    }

    // public (was private) so the IR-routed compiler's #478 nav-gate shape witness re-synthesizes the
    // EXACT equivalent feature-call this legacy fallback renders (the L-109e precedent): the witness
    // adapts the equivalent to measure which inputFeatureNav blockers the IR route could claim —
    // adapter admissibility only (the live nested render routes THIS synthesized node back through
    // visitFeatureCall, where the adapter admits it but the emitter declines on the missing cached
    // type and the byte-proven legacy render serves it; the #478 arm pairs the same lowering with
    // the cache-boundary retype, proven at population by the conservation signature).
    public RFeatureCall synthesizeFeatureCall(REnumValueRef evr, ExpressionCompiler compiler) {
        RSymbolReference synthReceiver = new RSymbolReference();
        synthReceiver.setName(evr.enumName());
        synthReceiver.setParent(evr.parent());

        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(synthReceiver);
        fc.setFeatureName(evr.valueName());
        fc.setParent(evr.parent());

        // v3.1 flip seat 1 (LADDER RETIREMENT) — carry the C1 authority binding
        // onto the synthesized equivalent when the RIGHT name resolved as a CHOICE
        // OPTION: the synthesized node is fresh (no slots), so without the carry
        // NavigationHandler's authority-first read never sees the binding and the
        // hop stays on the structural re-derivation whose floating projection
        // cannot register the witness import (the E1 import-only class). Only the
        // option shape is carried — an RAttribute-valued authority agrees with the
        // resolution below, and every other shape keeps today's paths.
        // seat 21 (facet metaFaceShortForm): the parser's RMetaType binding rides the same
        // carry — `commodityPayouts -> key`, `quantity -> location` — so the nav seat's meta
        // admission reads the parser's verdict, not a re-derivation.
        evr.resolvedFeatureNode()
                .filter(n -> n instanceof com.regnosys.rosetta.ast.supporting.RChoiceOption
                        || n instanceof com.regnosys.rosetta.ast.types.RMetaType)
                .ifPresent(fc::setResolvedFeatureNode);

        // Best-effort type resolution — walk up to the enclosing RFunction.
        RFunction enclosing = HandlerHelper.findEnclosingFunction(evr);
        if (enclosing != null) {
            RAttribute receiverAttr = resolveNameInFunction(enclosing, evr.enumName());
            if (receiverAttr != null) {
                synthReceiver.setResolvedSymbol(receiverAttr);
                RAttribute featureAttr = resolveFeatureOnAttribute(receiverAttr, evr.valueName());
                if (featureAttr != null) {
                    fc.setResolvedFeature(featureAttr);
                }
            }
        } else {
            // Coverage wave D (datarule): the disguised chain inside a DATA-TYPE
            // condition (`taxonomyValue -> nonEnumeratedTaxonomyValue` on
            // CollateralTaxonomy) — the head resolves on the declaring type's
            // attribute chain, so the receiver compiles through the
            // condition-instance identity branch and the hop's <Type> witness +
            // type-derived lambda var (`collateralTaxonomyValue`, golden
            // CollateralTaxonomyTaxonomyValue) read the SAME resolution. Null
            // for every non-condition seat (pre-wave bytes).
            RCondition condition = HandlerHelper.findEnclosingTypeCondition(evr);
            if (condition != null && condition.parent() instanceof RDataType declaringType) {
                RAttribute receiverAttr =
                        HandlerHelper.findAttributeOnDataType(declaringType, evr.enumName());
                if (receiverAttr != null) {
                    synthReceiver.setResolvedSymbol(receiverAttr);
                    RAttribute featureAttr = resolveFeatureOnAttribute(receiverAttr, evr.valueName());
                    if (featureAttr != null) {
                        fc.setResolvedFeature(featureAttr);
                    }
                }
            }
        }

        return fc;
    }

    /**
     * Resolve a name to an attribute in the function's inputs, output, or shortcut list.
     *
     * <p>Shortcuts don't have a direct type, so shortcut references cannot be resolved
     * to a single attribute — returns {@code null} for shortcut matches. Callers can
     * treat a {@code null} return as "unresolvable" and fall back to symbol-name-based
     * lambda naming.
     */
    private RAttribute resolveNameInFunction(RFunction func, String name) {
        if (name == null) return null;
        for (RAttribute input : func.inputs()) {
            if (name.equals(input.name())) return input;
        }
        if (func.output().isPresent() && name.equals(func.output().get().name())) {
            return func.output().get();
        }
        // Shortcuts are not RAttributes — walk the shortcut expression to infer.
        // For now, return null; lambda naming will fall back to symbol name.
        for (RShortcut shortcut : func.shortcuts()) {
            if (name.equals(shortcut.name())) {
                return null; // unresolvable as an attribute
            }
        }
        return null;
    }

    /**
     * Resolve a feature name on an attribute's declared type.
     *
     * <p>Walks the attribute's type call to find the referenced data type, then
     * searches the data type's attributes (including supertype inheritance) for
     * a match on {@code featureName}.
     */
    private static RAttribute resolveFeatureOnAttribute(RAttribute receiverAttr, String featureName) {
        if (receiverAttr == null || featureName == null) return null;
        RTypeCall tc = receiverAttr.typeCall();
        if (tc == null) return null;
        Optional<RNode> referenced = tc.referencedType();
        if (referenced.isEmpty()) return null;
        if (referenced.get() instanceof RDataType dt) {
            return HandlerHelper.findAttributeOnDataType(dt, featureName);
        }
        return null;
    }

    /**
     * facet conditionalMetaJoin (PR #295, the #280 meta-leaf residual): the meta-kind of a
     * bare-FUNCTION navigation's leaf — a disguised 2-name {@code func -> feature}
     * ({@link REnumValueRef}) whose {@code resolvedSymbol} is an {@link RFunction}, navigating
     * {@code feature} on the function's OUTPUT type. Returns {@code null} when {@code e} is NOT
     * such a baresym nav (or the leaf is unresolvable); otherwise the leaf's
     * {@link MetaFieldGenerator.MetaKind} ({@code NONE} = bare leaf, {@code REFERENCE_WITH_META}
     * / {@code FIELD_WITH_META} = meta leaf). Used by
     * {@code CollectionHandler.compileLadderConditionalBlock} to detect a MIXED-baresym ladder
     * (some baresym arm bare-leaf, some meta-leaf) — the SAME leaf resolution as the
     * {@code handle(REnumValueRef)} meta-leaf gate, so the two cannot disagree.
     */
    public static MetaFieldGenerator.MetaKind baresymFunctionLeafMetaKind(RExpression e) {
        if (!(e instanceof REnumValueRef evr) || evr.enumName() == null || evr.valueName() == null
                || evr.resolvedSymbol().isEmpty()
                || !(evr.resolvedSymbol().get() instanceof RFunction func)) {
            return null;
        }
        RAttribute leaf = func.output()
                .map(out -> resolveFeatureOnAttribute(out, evr.valueName()))
                .orElse(null);
        return leaf == null ? null : MetaFieldGenerator.detectMetaKind(leaf);
    }

    /**
     * facet ladderBareCalleeMixedJoin (PR #338): the widened ladder-arm leaf meta-kind —
     * the {@link #baresymFunctionLeafMetaKind} SUPERSET consumed ONLY by
     * {@code CollectionHandler.compileLadderConditionalBlock}'s mixed-join pre-scan (the
     * #180 own-helper law: the shared baresym walker's other consumers stay byte-locked).
     * Two additional PROVABLE shapes, both resolved off a FUNCTION's declared output (the
     * same {@code resolveFeatureOnAttribute} + {@code detectMetaKind} leaf resolution as
     * the #280/#295 gate, so the probes cannot disagree with it):
     * <ul>
     *   <li>a BARE function-call arm ({@link RSymbolReference} whose symbol is an
     *       {@link RFunction}) — the arm IS the callee's output, so its declared output
     *       attribute's meta-kind is the arm's kind ({@code GetOtherUnderlierLeg1} →
     *       plain {@code string} → {@code NONE} = bare-join evidence; the #295 nav trap
     *       does not apply — a declared output is not an untyped nav);</li>
     *   <li>a single FEATURE nav over [element-preserving collapse]* over such a bare
     *       function call ({@code GetUnderlierProductIdentifierLeg1 first -> identifier}
     *       — {@link RFeatureCall} over {@link RListOpExpr} FIRST/ONLY_ELEMENT/LAST over
     *       the {@link RSymbolReference}): the collapse preserves the element, so the
     *       feature resolves on the callee's output attribute ({@code identifier} →
     *       {@code FieldWithMetaString} = meta evidence).</li>
     * </ul>
     * Anything deeper (multi-feature chains, non-function roots, other list-ops) returns
     * {@code null} — no evidence, conservative, byte-flat.
     */
    public static MetaFieldGenerator.MetaKind ladderArmLeafMetaKind(RExpression e) {
        MetaFieldGenerator.MetaKind baresym = baresymFunctionLeafMetaKind(e);
        if (baresym != null) {
            return baresym;
        }
        if (e instanceof RSymbolReference sr) {
            RFunction func = sr.symbol().filter(RFunction.class::isInstance)
                    .map(RFunction.class::cast).orElse(null);
            return func == null ? null
                    : func.output().map(MetaFieldGenerator::detectMetaKind).orElse(null);
        }
        if (e instanceof RFeatureCall fc) {
            RExpression root = fc.receiver();
            int depth = 0;
            while (root instanceof RListOpExpr op && depth++ < 8
                    && (op.op() == ListOp.FIRST || op.op() == ListOp.ONLY_ELEMENT
                        || op.op() == ListOp.LAST)) {
                root = op.argument();
            }
            if (root instanceof RSymbolReference sr2) {
                RFunction func = sr2.symbol().filter(RFunction.class::isInstance)
                        .map(RFunction.class::cast).orElse(null);
                if (func != null) {
                    RAttribute leaf = func.output()
                            .map(out -> resolveFeatureOnAttribute(out, fc.featureName()))
                            .orElse(null);
                    return leaf == null ? null : MetaFieldGenerator.detectMetaKind(leaf);
                }
            }
        }
        return null;
    }

    /**
     * facet nestedTreeMixedMetaJoin (PR #362): the nested-tree
     * ({@code CollectionHandler.compileNestedConditionalBlock}) pre-scan classifier —
     * {@link #ladderArmLeafMetaKind}'s evidence set PLUS the date-RECORD leaf arm: an
     * {@link RFeatureCall} whose {@code date} feature fires
     * {@code NavigationHandler.tryRecordFeatureNav}'s record form (the SAME
     * {@code resolveReceiverRType} + {@code isDateRecordFeature} admission, so the bare
     * verdict and the fired render cannot disagree — the record nav renders the bare
     * {@code .<Date>map("Date", dt -> Date.of(dt.toLocalDate()))}, never a meta wrapper).
     * Carrier: drr EffectiveDateRule-common's {@code positionForEvent -> openDateTime ->
     * date} arm, whose bare evidence lets the sibling {@code tradeForEvent -> tradeDate}
     * baresym META arm fire + deref in-arm (the #295/#334 mixed-join law at the
     * nested-tree seat). Own-helper per the #180 law:
     * {@code compileLadderConditionalBlock}'s pre-scan keeps {@link #ladderArmLeafMetaKind}
     * unchanged, so every ladder carrier stays byte-locked.
     */
    public static MetaFieldGenerator.MetaKind nestedTreeArmLeafMetaKind(RExpression e,
            ExpressionCompiler compiler) {
        MetaFieldGenerator.MetaKind base = ladderArmLeafMetaKind(e);
        if (base != null) {
            return base;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        if (e instanceof RFeatureCall fc && gm != null) {
            RType recvType = NavigationHandler.resolveReceiverRType(fc.receiver(), gm, compiler);
            if (recvType instanceof RRecordType rrt
                    && NavigationHandler.isDateRecordFeature(fc.featureName(), rrt)) {
                return MetaFieldGenerator.MetaKind.NONE;
            }
        }
        return null;
    }

    // =========================================================================
    // Implicit variable
    // =========================================================================

    /**
     * Compiles an {@link RImplicitVariable} reference.
     *
     * <p>Two shapes share this node type:
     * <ul>
     *   <li><b>Literal {@code item} keyword</b> — written by the user inside a
     *       lambda body (filter/extract). Renders as the bare identifier
     *       {@code item}; the surrounding lambda provides the binding.</li>
     *   <li><b>Elided-operand synthesis</b> — emitted by AstBuilder for the
     *       without-left list/extract/filter/conversion/toString forms
     *       (engine-phase PR #1). At rule-body top-level the synthetic operand IS the
     *       rule's implicit input, so it renders as {@code MapperS.of(input)}.
     *       Inside an enclosing lambda body the operand still piggy-backs on
     *       the surrounding {@code item} binding (the nested chain receives
     *       its receiver from the outer chain step), so we fall through to
     *       the bare {@code item} form. Mirrors upstream's
     *       {@code RosettaImplicitVariable} rendering split.</li>
     * </ul>
     */
    public JavaStatementBuilder handle(RImplicitVariable expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR #98 (facet F6 then-extract): inside a `then` body the implicit input
        // resolves to the hoisted `thenArg` local, NOT the rule input or a fresh `item`.
        // The binding lives on the scope (set by FunctionExpressionRenderer.renderThenExtractSet),
        // so it survives the 3-arg compile() sub-calls handlers use. It is keyed by the
        // then-function boundary, so a nested lambda inside the body — whose nearest enclosing
        // inline function differs — does not match and keeps its own `item`. Fork analogue of
        // upstream createKeySynonym(function.implicitVarInContext, thenArgCode).
        JavaStatementScope sc = ctx.scope();
        if (sc != null) {
            RInlineFunction thenBoundary = nearestEnclosingInlineFunction(expr);
            if (thenBoundary != null) {
                JavaExpression thenArgRef = sc.thenArgRefFor(thenBoundary);
                if (thenArgRef != null) {
                    return thenArgRef;
                }
                // facet namedExtractPipedRebind (PR #367): `then extract <name> [ … ]`
                // parses as an implicit WRAPPER fn holding the RExtractExpr whose own
                // body is the EXPLICIT-param fn — the wrapper carries the k-loop's
                // thenArg binding, the named fn does not. Upstream's named param does
                // not rebind the implicit item (the #364 non-rebind law), so INSIDE an
                // ACTIVE deep-then restructure window (the #356 chain-top flag,
                // ancestor-checked — the #357 arm's exact gate pattern) an implicit
                // whose nearest boundary is the UNBOUND named fn walks out ONE fn to
                // the wrapper's binding: golden asic CountryOfCounterparty2Rule
                // `thenArg0 = thenArg\n\t.mapSingleToList(…)` (the piped local, not
                // the param name). RULE-scoped; outside the window every named-extract
                // body implicit keeps the param-name/input renders byte-frozen (the
                // cp4/cp4b catches: a bind-time blanket regressed 35 green FUNCTION
                // files un-gated and 647 green rule POJOs rule-gated — the
                // discriminator is the CONSUMER's restructure window).
                if (!thenBoundary.isImplicit()
                        && !thenBoundary.paramNames().isEmpty()
                        && HandlerHelper.findEnclosingRule(expr) != null) {
                    // The isImplicit gate is load-bearing: the parser MATERIALIZES
                    // `item` into paramNames for implicit forms (P365A), so a
                    // paramNames-only test admits [item] fns — the cp4d catch:
                    // golden ExchangeRateRule esma/fca keeps `thenArg0 = item…` at
                    // its implicit-fn k0 seats (2 waivered movers went AWAY).
                    RThenExpr rebindChainTop = sc.findDeepThenRestructureChainTop();
                    if (rebindChainTop != null
                            && expr.parent() == chainBaseExtract(rebindChainTop)
                            && thenBoundary.parent() instanceof RExtractExpr namedExtract
                            && namedExtract.body() == thenBoundary) {
                        RInlineFunction outerWrapper =
                                nearestEnclosingInlineFunction(namedExtract);
                        JavaExpression pipedRef = outerWrapper == null ? null
                                : sc.thenArgRefFor(outerWrapper);
                        if (pipedRef != null) {
                            return pipedRef;
                        }
                    }
                    // facet namedExtractBaseCallRebind (PR #368, F-A1): the BASE-CALL
                    // sibling of the arm above — the flagged chain's level-0 step is a
                    // BARE fn invocation (`TradeStateForEvent then extract …`), whose
                    // SYNTHETIC implicit ARG (renderImplicitFunctionInvocation parents
                    // it at the innermost RThenExpr) is the same piped implicit: inside
                    // the ACTIVE window it walks out of the unbound named fn to the
                    // wrapper's binding — golden fca/esma/hkma OtherPaymentRule
                    // `thenArg0 = MapperS.of(tradeStateForEvent.evaluate(thenArg.get()))`
                    // (the piped local, not the param name). Node identity: the parent
                    // then's ARGUMENT must BE the window chain's base node (the cp4c
                    // law), and only the SYNTHETIC arg form is admitted (a literal
                    // `item` never parents at the then node).
                    if (rebindChainTop != null
                            && expr.isSynthetic()
                            && expr.parent() instanceof RThenExpr parentThen
                            && parentThen.argument() == chainBaseCall(rebindChainTop)
                            && thenBoundary.parent() instanceof RExtractExpr namedExtractB
                            && namedExtractB.body() == thenBoundary) {
                        RInlineFunction outerWrapperB =
                                nearestEnclosingInlineFunction(namedExtractB);
                        JavaExpression pipedRefB = outerWrapperB == null ? null
                                : sc.thenArgRefFor(outerWrapperB);
                        if (pipedRefB != null) {
                            return pipedRefB;
                        }
                    }
                }
                // facet thenParamImplicitReroot (PR #375, B1a): the #367/#368 walk-out
                // WITHOUT the restructure-window / rule gates — an implicit whose
                // nearest boundary is an UNBOUND named (explicit-param) extract fn
                // walks out ONE fn to the named-extract WRAPPER's #350 binding wherever
                // that binding is LIVE (bindThenArg wraps every restructured step-body
                // compile, so a non-null read proves the piped local exists in the
                // render — golden MessageID `thenArg.<ReportableInformation>map(…)` at
                // the block-conditional seats + DTCC_OptionTypeRule
                // `thenArg\n\t.filterSingleNullSafe(…)` at the in-lambda chain base;
                // upstream ImplicitVariableUtil: a named param does not rebind the
                // implicit item). Function + rule paths alike — the binding-liveness
                // gate is the discriminator the #367 cp4/cp4b bind-time blanket lacked
                // (an unrestructured runtime `.then(` body compiles with NO binding and
                // keeps the param-name/input renders byte-frozen).
                if (!thenBoundary.isImplicit()
                        && !thenBoundary.paramNames().isEmpty()
                        && thenBoundary.parent() instanceof RExtractExpr namedXWide
                        && namedXWide.body() == thenBoundary) {
                    RInlineFunction wrapperWide = nearestEnclosingInlineFunction(namedXWide);
                    JavaExpression pipedWide = wrapperWide == null ? null
                            : sc.thenArgRefFor(wrapperWide);
                    if (pipedWide != null) {
                        return pipedWide;
                    }
                }
            }
        }
        // facet switchChoiceHoist (PR #221): inside a CHOICE/TYPE-keyed switch CASE body
        // rendered as the `instanceof` block-hoist, the implicit subject `item` (narrowed to
        // the case type) resolves to the cast case var the renderer bound on the scope —
        // ONLY when the switch is the NEAREST binding boundary (a closer lambda owns its own
        // `item`, so a nested extract/map lambda inside a case body is unaffected). The bound
        // ref is a bare typed value (the cast var), so an evaluate-arg unwraps to the bare
        // name (golden `evaluate(bondOption, …)`) and a navigation receiver wraps it
        // (`MapperS.of(bondOption).map(…)`). The analogue of the thenArg re-root above.
        if (sc != null) {
            com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr switchBoundary =
                    nearestEnclosingSwitchSubject(expr);
            if (switchBoundary != null) {
                JavaExpression subjectRef = sc.switchSubjectRefFor(switchBoundary);
                if (subjectRef != null) {
                    return subjectRef;
                }
            }
        }
        // Coverage wave D (datarule): the implicit variable at type-condition
        // TOP LEVEL (no enclosing lambda — an in-lambda implicit keeps its own
        // binding via the arms above/below) is the condition instance — the
        // executeDataRule parameter. Renders `MapperS.of(<instance>)` (a bare
        // evaluate-arg seat unwraps it to the bare name — golden
        // `fpmlIrd8.evaluate(trade, …)`, TradeFpML_ird_8). Context-gated by
        // findEnclosingTypeCondition — null for every function/rule-path
        // expression by construction.
        {
            RCondition implicitCondition = HandlerHelper.findEnclosingTypeCondition(expr);
            // v3.2 seat 3 (F9): the owner may be a typeAlias — its condition's implicit item is
            // the alias-named instance parameter (oracle golden EvenNatNonNeg:
            // `MapperS.of(evenNat)`); HandlerHelper.conditionOwnerName is the ONE owner read.
            String implicitOwnerName = implicitCondition == null ? null
                    : HandlerHelper.conditionOwnerName(implicitCondition);
            if (implicitOwnerName != null && nearestEnclosingInlineFunction(expr) == null) {
                // The instance render is type-less for EVERY owner (the Data form, byte-frozen).
                // The literal-widening question at an alias owner's comparison (`item <= 100`
                // over a number alias → `BigDecimal.valueOf(100)`, oracle golden PctCapped) is
                // answered by HandlerHelper.numericOperandKind's alias-owner arm, which the
                // literal handler consults for its SIBLING's kind; a typed MapperS<T> wrapper
                // on this render was measured to carry no witness (v3.2 seat 3 lane H green)
                // and was withdrawn under the #614 law.
                JavaExpression inner = JavaExpression.from(
                        HandlerHelper.conditionInstanceName(implicitOwnerName),
                        null, Set.of());
                return JavaExpression.wrappedInMapperSOf(inner);
            }
        }
        if (isElidedOperandTopLevel(expr)) {
            // Rule-body top-level: render as MapperS.of(<rule-input-name>).
            // RFunction.fromRule synthesises the input attribute as literal
            // "input" (see RFunction.java:425), and the FunctionGenerator
            // surfaces it under that name in the generated evaluate signature.
            JavaExpression inner = JavaExpression.from("input", null, Set.of());
            return JavaExpression.wrappedInMapperSOf(inner);
        }
        // facet inputFormThenHoist (PR #357): the deep-then hoist's INPUT-form base — a
        // synthetic elided receiver compiling INSIDE the ACTIVE restructure window (the
        // #356 chain-top flag, pushed around the k==0 base compile; node identity: the
        // flagged chain must be this implicit's own ancestor) whose nearest enclosing
        // lambda is a NAMED top-level extract over the bare rule input renders
        // MapperS.of(input) — upstream's implicit context: a named extract does not
        // rebind the implicit item, so a name-unprefixed nav roots on the rule input
        // (the C2IF/Broker hoisted-decl bases). Outside the window every unconverted
        // runtime `.then(` chain keeps the outer-lambda-param re-root byte-frozen.
        if (expr.isSynthetic() && sc != null && HandlerHelper.findEnclosingRule(expr) != null) {
            RThenExpr chainTop = sc.findDeepThenRestructureChainTop();
            if (chainTop != null && hasAncestorNode(expr, chainTop)
                    && isInsideNamedTopLevelExtractOverBareInput(expr)) {
                JavaExpression inner = JavaExpression.from("input", null, Set.of());
                return JavaExpression.wrappedInMapperSOf(inner);
            }
        }
        // facet stringJoinTradeResidual (PR #364): the #357 arm's law WITHOUT the
        // restructure-window gate — a NAMED top-level extract over the bare rule input
        // does not rebind the implicit item (upstream ImplicitVariableUtil: explicit-param
        // lambdas define no `item`), so a name-unprefixed implicit receiver roots on the
        // rule input EVERYWHERE in the extract body, not only inside an active deep-then
        // restructure (golden asic/hkma/jfsa/mas trade TechnicalRecordId rules:
        // `technicalRecordId.evaluate(MapperS.of(input).<ReportableInformation>map(…)
        // .get(), …)` at the cond + if-arm seats, while the EXPLICIT param reference
        // `ReportingTimestamp(ReportableEvent)` keeps the param name — a different node
        // shape, RSymbolReference, untouched here). The #355 22/22 golden discriminator
        // (isInsideNamedTopLevelExtractOverBareInput) already pins the shape; implicits
        // whose NEAREST lambda is a nested implicit one keep their own `item` (the
        // helper's isImplicit decline).
        if (expr.isSynthetic() && HandlerHelper.findEnclosingRule(expr) != null
                && isInsideNamedTopLevelExtractOverBareInput(expr)) {
            JavaExpression inner = JavaExpression.from("input", null, Set.of());
            return JavaExpression.wrappedInMapperSOf(inner);
        }
        if (expr.isSynthetic()) {
            // Synthetic elided operand inside a lambda body — use the enclosing
            // inline function's binding (explicit parameter name when present,
            // else "item" for implicit forms). Mirrors
            // {@code CollectionHandler.resolveParamName} so the synthetic
            // operand references whatever binding the surrounding lambda
            // introduced (Copilot R2 F1 — `extract [t -> inner extract X]`
            // was emitting `item` for the synthetic argument of the inner
            // extract, undefined inside the `t`-named lambda body).
            // facet lambdaItemReceiverType (v3.1 flip seat 32, law A.2): the same
            // receiver-bound type the literal-item terminal below reads - the
            // synthetic and literal forms are one item.
            RInlineFunction synthBoundary = nearestEnclosingInlineFunction(expr);
            return JavaExpression.from(enclosingLambdaBinding(expr),
                    synthBoundary == null ? null
                            : boundLambdaItemMapperType(synthBoundary, ctx, compiler));
        }
        // PR #143 (meta_deref_input_hoist): a NON-synthetic literal `item` keyword
        // used as a FUNCTION-CALL ARGUMENT (parent is the RSymbolReference call) at
        // rule-body TOP LEVEL (no enclosing lambda) refers to the rule input. The
        // literal-keyword fall-through below renders bare `item`, which is undefined at
        // top level (no lambda binding) — golden emits the rule `input` parameter (e.g.
        // `Fn(item, …)` -> `fn.evaluate(input, …)`). Render `MapperS.of(input)`
        // (stripped to `input` by unwrapForEvaluateArg), mirroring isElidedOperandTopLevel
        // for the synthetic-elided-operand shape that gate does NOT cover (it requires
        // iv.isSynthetic() + a without-left-op parent). The `parent instanceof
        // RSymbolReference` guard SCOPES this to the call-argument slot, leaving a literal
        // `item` used as a without-left-op receiver (`item only-element`) on the bare-item
        // path (locked by ReferenceHandlerTest.literal_item_keyword_in_argument_slot_*).
        // Rule-body only — a function body's input is not named `input`, and an enclosing
        // lambda binds `item`. Regression-safe: bare top-level `item` arg does not
        // compile, so no green file carries it.
        // facet ruleRecursionTyping (PR #437, finding #32): the BINARY-OPERAND slots
        // join the #143 call-argument arm — a literal `item` operand of an
        // equality / arithmetic / comparison (any RBinaryExpression) at rule-body
        // TOP LEVEL is the rule input too (upstream's implicit variable IS the
        // input in EVERY slot; golden report-rule-recursion `areEqual(MapperS.of(
        // input), …)` + `multiply(MapperS.of(input), …)` for `if item = 1 … else
        // item * Fac(item - 1)`). The op-RECEIVER slot (`item only-element` —
        // parent RExtractExpr/RListOpExpr etc.) keeps the bare-item path and its
        // ReferenceHandlerTest lock — no witness moves it.
        // facet itemNavReceiverInputSlot (PR #579): the FEATURE-CALL RECEIVER slot
        // joins the #143 call-argument and #437 binary-operand arms — a literal
        // `item` ROOTING a navigation chain (`item -> attr`, parent RFeatureCall
        // with receiver identity) at rule-body TOP LEVEL is the rule input in that
        // slot too (the same upstream law: the implicit variable IS the input in
        // EVERY slot). The fall-through rendered bare `item`, UNDEFINED at
        // assignOutput root scope (the only `item` identifiers there are later
        // lambda params) — golden materializes the input root:
        // `getUniqueTransactionIdentifier.evaluate(MapperS.of(input)
        // .<ReportableInformation>map(…).get(), …)` (the drr 7.x
        // `Fn(item -> reportableInformation, …)` carriers — 31 whole-file per 7.x
        // cell, zero at 6.34.1). Two-forms-agree mirror: a BARE attribute at rule
        // root already renders on the same input root via
        // synthesizeImplicitInputNavigation. The op-RECEIVER slot
        // (`item only-element` — parent RExtractExpr/RListOpExpr) stays on the
        // bare-item path per the no-witness law above; in-lambda receivers keep
        // their binding via the nearestEnclosingInlineFunction gate; the then-body
        // shape never reaches here (the thenArg binding arms at the top of this
        // method). Regression-safe: goldens compile and a bare top-level `item`
        // cannot, so no golden carries the replaced form (the charter's
        // LAW-66-controlled scans: statement-root bare-item chains = 0 golden
        // hits, positive control 2/2; every golden `evaluate(item.<` hit is
        // lambda-bound — 373/373).
        if ((expr.parent() instanceof RSymbolReference
                        || expr.parent() instanceof com.regnosys.rosetta.ast.RBinaryExpression
                        || (expr.parent() instanceof RFeatureCall parentFc
                                && parentFc.receiver() == expr))
                && nearestEnclosingInlineFunction(expr) == null
                && HandlerHelper.findEnclosingRule(expr) != null) {
            JavaExpression inner = JavaExpression.from("input", null, Set.of());
            return JavaExpression.wrappedInMapperSOf(inner);
        }
        // Literal `item` keyword (non-synthetic) — renders as bare {@code item},
        // EXCEPT a nested implicit-item lambda escapes it to `_item` (…) to match
        // the param decl (facet lambdaParamItemEscape, PR #292; the body reference
        // must agree with CollectionHandler.resolveParamName's depth-escaped name).
        // The literal keyword only appears inside implicit-parameter lambdas (the
        // explicit-parameter form shadows it at the linker level), so the
        // surrounding lambda's binding is `item`/`_item`/… by construction; an
        // explicit-param enclosing lambda (or none) keeps the bare `item`,
        // preserving pre-PR behaviour (Copilot R5 F1 — the synthetic-fallback path
        // must NOT rewrite a literal `item` to an enclosing explicit-param name).
        RInlineFunction nearestImplicit = nearestEnclosingInlineFunction(expr);
        if (nearestImplicit != null
                && (nearestImplicit.isImplicit() || nearestImplicit.paramNames().isEmpty())) {
            // facet lambdaItemReceiverType (v3.1 flip seat 32, law A.2): the item keeps its
            // NAME and gains the TYPE the extract seat bound from its compiled RECEIVER -
            // MapperS<element>, exactly what the runtime hands a per-element lambda. A
            // boundary with no binding renders type-less as before; since the seat-32
            // narrowing the extract seat binds a META WRAPPER item ONLY, so this terminal can
            // only ever hand back a MapperS<FieldWithMetaX> / MapperS<ReferenceWithMetaX> and
            // never a bare element type (the mas FixedFloatRateLeg trio's seat).
            return JavaExpression.from(
                    HandlerHelper.escapedImplicitItemName(nearestImplicit),
                    boundLambdaItemMapperType(nearestImplicit, ctx, compiler));
        }
        return JavaExpression.from("item", null);
    }

    /**
     * facet lambdaItemReceiverType (v3.1 flip seat 32, law A.2): the {@code MapperS<element>}
     * type for an implicit lambda item whose boundary the extract seat bound
     * ({@code JavaStatementScope.bindLambdaItemType} - the receiver-render typing channel);
     * {@code null} when no binding or no type utility is available (the pre-law render).
     */
    private static com.rosetta.util.types.JavaType boundLambdaItemMapperType(
            RInlineFunction boundary, ExpressionContext ctx, ExpressionCompiler compiler) {
        if (ctx == null || ctx.scope() == null || compiler == null
                || compiler.getTypeUtil() == null) {
            return null;
        }
        com.rosetta.util.types.JavaType bound = ctx.scope().lambdaItemTypeFor(boundary);
        if (bound == null) {
            return null;
        }
        var tu = compiler.getTypeUtil();
        return tu.wrap(tu.MAPPER_S, bound);
    }

    /**
     * Walk the parent chain to the nearest enclosing {@link RInlineFunction}
     * and return its binding name — the first explicit parameter name when
     * present, else {@code "item"} for implicit forms or when no enclosing
     * inline function is found.
     */
    private static String enclosingLambdaBinding(RImplicitVariable iv) {
        RNode cur = iv.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                if (inline.isImplicit() || inline.paramNames().isEmpty()) {
                    // facet lambdaParamItemEscape (PR #292): a nested implicit-item
                    // lambda's body reference escapes to `_item` (…), matching the
                    // param decl (CollectionHandler.resolveParamName) — both derive
                    // the SAME depth-escaped name from the AST nesting.
                    return HandlerHelper.escapedImplicitItemName(inline);
                }
                return inline.paramNames().get(0);
            }
            cur = cur.parent();
        }
        return "item";
    }

    /**
     * facet switchChoiceHoist (PR #221): walk the parent chain to the nearest enclosing
     * {@link com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr} whose CASE body
     * contains {@code iv} — but return {@code null} if an {@link RInlineFunction} (lambda)
     * is encountered FIRST, because that lambda owns {@code item} and the switch subject
     * does not reach into it. Used to gate the switch-subject scope rebinding so a nested
     * {@code extract}/{@code map} lambda inside a case body keeps its own {@code item}.
     */
    /**
     * facet caseNarrowedDisguisedNav (PR #368, F-B): synthesize the implicit-item
     * navigation chain {@code item -> head -> leaf} for a disguised
     * {@link REnumValueRef} inside a TYPE-keyed switch CASE whose narrowed case
     * type carries the HEAD attribute — the case-scope sibling of
     * {@link #synthesizeImplicitItemChain} (same closure-param / function-scope
     * declines; the boundary is the nearest {@code RSwitchCase} with NO inline-fn
     * between). The synthetic item resolves through the #221 switch-subject
     * binding, so the arm REQUIRES the binding to be ACTIVE
     * ({@code switchSubjectRefFor} non-null — only a rendering instanceof ladder
     * binds); outside a ladder compile every un-laddered switch keeps the residual
     * seat's outcome (a refusal — {@code TYPE_SWITCH_TERNARY_STUB} per resolvable case type
     * first, since v3.1 C0, else {@code SWITCH_TERNARY_STUB}, R1, since v3.2 seat 12; today's
     * bytes before them). The narrowed type resolves via the guard through the SAME
     * resolution the ladder's cast used ({@code NavigationHandler
     * .caseNarrowedImplicitType}: the linker's {@code resolvedGuard} first —
     * PR #460 — then the {@code resolveTypeByName} fallback), so the nav and
     * the cast cannot disagree.
     */
    private RFeatureCall synthesizeCaseNarrowedChain(REnumValueRef evr,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        String headName = evr.enumName();
        String leafName = evr.valueName();
        if (headName == null || leafName == null || isEnclosingClosureParam(evr, headName)) {
            return null;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(evr);
        if (enclosing != null && nameResolvesInFunctionScope(evr, enclosing, headName)) {
            return null;
        }
        RDataType narrowedDt = activeCaseNarrowedType(evr, ctx, compiler);
        if (narrowedDt == null) {
            return null;
        }
        RAttribute headAttr = HandlerHelper.findAttributeOnDataType(narrowedDt, headName);
        if (headAttr == null) {
            return null;
        }
        RDataType headType = NavigationHandler.attributeToDataType(headAttr, compiler);
        RAttribute leafAttr = headType == null ? null
                : HandlerHelper.findAttributeOnDataType(headType, leafName);
        if (leafAttr == null) {
            return null;
        }
        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(evr.parent());
        RFeatureCall headCall = new RFeatureCall();
        headCall.setReceiver(item);
        headCall.setFeatureName(headAttr.name());
        headCall.setResolvedFeature(headAttr);
        headCall.setParent(evr.parent());
        RFeatureCall leafCall = new RFeatureCall();
        leafCall.setReceiver(headCall);
        leafCall.setFeatureName(leafAttr.name());
        leafCall.setResolvedFeature(leafAttr);
        leafCall.setParent(evr.parent());
        return leafCall;
    }

    /**
     * facet caseNarrowedDisguisedNav (PR #368, F-B): the BARE 1-name sibling of
     * {@link #synthesizeCaseNarrowedChain} — a bare symbol that is an attribute of
     * the hosting case's NARROWED type synthesizes {@code item -> attr} (the
     * synthetic item resolves to the #221-bound cast var). Same decline ladder.
     */
    private RFeatureCall synthesizeCaseNarrowedBareNav(RSymbolReference sym,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        String name = sym.name();
        if (name == null || isEnclosingClosureParam(sym, name)) {
            return null;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(sym);
        if (enclosing != null && nameResolvesInFunctionScope(sym, enclosing, name)) {
            return null;
        }
        RDataType narrowedDt = activeCaseNarrowedType(sym, ctx, compiler);
        if (narrowedDt == null) {
            return null;
        }
        RAttribute attr = HandlerHelper.findAttributeOnDataType(narrowedDt, name);
        if (attr == null) {
            return null;
        }
        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(sym.parent());
        RFeatureCall call = new RFeatureCall();
        call.setReceiver(item);
        call.setFeatureName(attr.name());
        call.setResolvedFeature(attr);
        call.setParent(sym.parent());
        return call;
    }

    /**
     * facet caseNarrowedDisguisedNav (PR #368, F-B): the ACTIVE-binding form of the
     * case-narrowed resolution — the narrowed type of the nearest enclosing switch
     * CASE (no inline-fn boundary between; {@code NavigationHandler
     * .caseNarrowedImplicitType} — the #178 same-walk law), admitted ONLY when the
     * hosting switch's #221 subject binding is live on the scope (a rendering
     * instanceof ladder). {@code null} keeps today's bytes everywhere else.
     */
    private static RDataType activeCaseNarrowedType(RExpression site,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase scase = null;
        RNode cur = site.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null; // a closer lambda owns `item` — the B2 arm's seat
            }
            if (cur instanceof com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase c) {
                scase = c;
                break;
            }
            cur = cur.parent();
        }
        if (scase == null) {
            return null;
        }
        com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr sw = null;
        RNode swCur = scase.parent();
        int swDepth = 0;
        while (swCur != null && swDepth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (swCur instanceof com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr s) {
                sw = s;
                break;
            }
            swCur = swCur.parent();
        }
        if (sw == null || ctx.scope().switchSubjectRefFor(sw) == null) {
            return null;
        }
        return NavigationHandler.caseNarrowedImplicitType(site, compiler);
    }

    private static com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr nearestEnclosingSwitchSubject(
            RImplicitVariable iv) {
        RNode cur = iv.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null; // a closer lambda owns `item`
            }
            if (cur instanceof com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr sw) {
                return sw;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * valuation_currency_metabox_deref (PR #144). Recover the piped element type for a
     * bare-function invocation inside a then-extract lambda: walk the parent chain to the
     * nearest enclosing {@link RInlineFunction} that carries a {@code thenArg} scope
     * binding (set by {@code FunctionExpressionRenderer.renderThenExtractSet}) and return
     * that binding's Java type (e.g. {@code MapperS<FieldWithMetaString>}). The
     * synthesised {@code item} reference is untyped (handle(RImplicitVariable) returns a
     * bare identifier), so this is the only place the meta wrapper the thenArg declaration
     * (PR #144 edit 1) surfaced is observable here. Returns {@code null} when no enclosing
     * then-extract binding exists (the call is not piped from a typed thenArg — e.g. a
     * filter predicate), leaving the flat (still-waivered) form. Companion to
     * {@link #metaDerefHoistRoute}: that gate's (a)/(b) extract/then context is exactly where
     * a {@code thenArg} binding is in scope, so the two agree by construction — the gate
     * authorises the block form, this helper supplies the meta type that fires it.
     */
    private static JavaType enclosingThenArgType(RNode expr, ExpressionContext ctx) {
        JavaStatementScope sc = ctx.scope();
        if (sc == null) {
            return null;
        }
        RNode cur = expr.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — VERDICT-MOVED
                // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar; S35), a LAW-77 seat: the
                // walk-THROUGH semantics are the NORM, not an edge case. c9 census, 283,896
                // arrivals (94,762 / 95,628 / 93,506) = 141,948 entry + 80,608 read + 61,340 none:
                // the walk passes THROUGH an unbound inline function at 111,246 of 141,948 entries
                // (78.37%), four arrivals in five. No equality is posed — the proposed workspace
                // channel needs a compiler threaded into a private static method, the same blocker
                // as thenStepParamRenderName's. LAW 77 — THE FAMILY'S ONE SEAT WHERE THE ASYMMETRY
                // REACHES THE ANSWER: entry OFF 47,381 vs ON 47,814 but read OFF 25,954 vs ON
                // 28,712 and none OFF 21,427 vs ON 19,102, so the IR route finds a binding at about
                // 2,758 arrivals where the default route finds none, and this method returns a type
                // on one route where it returns null on the other at ~5.8% of its entries (2,758 of
                // the 47,814 ON entries; ~2.9% if measured against arrivals). Both rings are EXACT
                // today so the difference is absorbed downstream, but it is a real per-route
                // difference in a seat's own OUTPUT: recorded, not acted on, and the first question
                // whoever next touches this method must ask. It does not block a KEEP; it WOULD
                // block a swap, which must prove byte-inertness on EACH ROUTE SEPARATELY. Producer
                // recovery 80,582/141,948 (56.77%) over SIX NAMED SITES plus 26 none and 61,340
                // nokey. Locator drift corrected: the triage's L7207 is live at 7321-7338.
                JavaExpression thenArgRef = sc.thenArgRefFor(inline);
                if (thenArgRef != null) {
                    return thenArgRef.getExpressionType();
                }
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * facet namedExtractPipedRebind (PR #367): the LEVEL-0 base extract of a
     * restructuring then-chain — descend the chain's {@code argument()} side
     * through the nested {@link RThenExpr}s to the leftmost node; the base is
     * that node when it is an {@link RExtractExpr} (the `extract A then …`
     * shape), else {@code null}. Identity-compared by the consumer arm so ONLY
     * the flagged chain's own level-0 receiver rebinds (the cp4c catch: a
     * window-wide gate moved 22 green rule POJOs whose deeper in-window
     * implicits keep golden's param-name renders).
     */
    private static RExtractExpr chainBaseExtract(RThenExpr chainTop) {
        RExpression cur = chainTop;
        while (cur instanceof RThenExpr thenNode) {
            cur = thenNode.argument();
        }
        return cur instanceof RExtractExpr baseExtract ? baseExtract : null;
    }

    /**
     * facet namedExtractBaseCallRebind (PR #368, F-A1): the LEVEL-0 base node of a
     * restructuring then-chain when it is a BARE fn invocation (an args-empty
     * {@link RSymbolReference} resolving to an {@link RFunction} — the
     * `TradeStateForEvent then …` shape), else {@code null}. The base-call sibling
     * of {@link #chainBaseExtract}, identity-compared by the consumer arm the same
     * way so ONLY the flagged chain's own base-call implicit arg rebinds.
     */
    private static RSymbolReference chainBaseCall(RThenExpr chainTop) {
        RExpression cur = chainTop;
        while (cur instanceof RThenExpr thenNode) {
            cur = thenNode.argument();
        }
        return cur instanceof RSymbolReference baseCall
                && baseCall.args().isEmpty()
                && baseCall.symbol().filter(RFunction.class::isInstance).isPresent()
                ? baseCall : null;
    }

    /**
     * Walk the parent chain to the nearest enclosing {@link RInlineFunction}, or
     * {@code null} if none is found within {@link HandlerHelper#PARENT_WALK_LIMIT}.
     * Companion to {@link #enclosingLambdaBinding}; used to scope the PR #98
     * then-arg implicit-input rebinding to its boundary function, so a nested lambda
     * inside the then-body (whose nearest inline function is the nested one, not the
     * then-function) keeps its own {@code item} binding.
     */
    // public (was private) for the IR route's rule-delegation gate (the lab's L-045): the IR's
    // decline predicate reuses this parent-walk so its claim cannot disagree with legacy's
    // renderImplicitRuleInvocation dispatch. The D43 seam.
    public static RInlineFunction nearestEnclosingInlineFunction(RNode start) {
        RNode cur = start.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                return inline;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * facet numericCoercionCompose (PR #300, M1) green-safety gate, refined by facet
     * numericCoercionIfBranchDrain (PR #301): true when the numeric-coercion arg sits inside a
     * {@code .mapSingleToItem(item -> if … then …)} lambda whose BODY is a conditional, EXCEPT the
     * ELSELESS single-conditional form (the {@code if (…) { return …; } return MapperS.<T>ofNull();}
     * block-lambda {@link CollectionHandler#compileElselessConditionalBlock} renders) which PR #301
     * now handles: that renderer DRAINS the pending lambda hoist INSIDE the if-branch (the #276
     * drain-inside-the-branch analogue), so the recovery is green there (the
     * FloatingRateResetFrequencyMultiplierOfLeg2 / PeriodOfLeg{1,2} esma carriers #300 deferred).
     *
     * <p>The OTHER conditional forms (effective-else / nested-then / else-if ladder) still DECLINE:
     * they have no if-branch drain, so recovering the Integer/BigInteger item type there would
     * register a pending lambda hoist (the {@link MetaDerefHoistRoute#LAMBDA_CHANNEL} route) at the
     * lambda TOP, which re-renders the inner conditional as the pre-#223 inline ternary
     * ({@code … ? … : MapperC.of()}) — moving the carrier AWAY from golden (the #300 regression the
     * regscan, not the byte-oracle, caught). The clean #300 BLOCK-route carriers
     * (FloatingRateResetFrequencyMultiplierOfLeg1 — a then-body thenArg-decl — and
     * PaymentFrequencyPeriodMultiplierAdjusted — an expr-lambda whose BODY IS the call) have a
     * NON-conditional body, so this gate returns false for them (recovery proceeds, unchanged).
     */
    private static boolean numCoerceArgInConditionalLambdaBody(RExpression arg) {
        RInlineFunction inline = nearestEnclosingInlineFunction(arg);
        if (inline == null || !(inline.body() instanceof RConditionalExpr cond)) {
            return false;
        }
        // The elseless single-conditional block-lambda (compileElselessConditionalBlock) drains the
        // pending lambda hoist INSIDE the if-branch (PR #301) → allow the recovery there; every other
        // conditional form keeps declining (no if-branch drain → the #300 lambda-top regression).
        // NOTE (latent, green-safe, regscan-0): the if-branch drain only runs in the MAPPER_EXPECTING
        // block path; a numeric coercion in an elseless conditional inside a NON-MAPPER_EXPECTING extract
        // lambda (mapSingleToList) would still drain at the lambda top. No such carrier exists today (the
        // integerToBigDecimal arm fires only on a bare Integer/BigInteger→BigDecimal-param shape that did
        // not compile, so no green file carries it; the regscan confirms 0 within-waiver regressions). A
        // future carrier would gate this on the LambdaBodyPosition (threaded through ExpressionContext).
        return !CollectionHandler.isElselessSingleConditional(cond);
    }

    /**
     * True when {@code iv} is the synthetic elided operand of a without-left
     * list/extract/filter/conversion/toString op AND that op sits directly at
     * the top of an {@link RRule} body (no wrapping lambda — nested chains keep
     * the {@code item} binding from the outer step).
     *
     * <p><b>Scoped to {@link RRule} roots only</b> (Copilot R1 F1 fix). Plain
     * {@link RFunction} bodies use the function's declared input parameters
     * (which are not necessarily named {@code "input"}) — emitting
     * {@code MapperS.of(input)} for an elided operand inside a function body
     * would reference an undefined variable. The rule/report path is the
     * only one where {@code RFunction.fromRule} / {@code fromReport} guarantee
     * the synthetic input attribute is literally named {@code "input"} (see
     * {@code RFunction.fromRule} at {@code RFunction.java:425}). If a future
     * engine PR extends this to function bodies, the parameter name must
     * flow through {@link ExpressionContext} / scope rather than being hard
     * coded here.
     */
    private static boolean isElidedOperandTopLevel(RImplicitVariable iv) {
        // Tighten the structural check by requiring iv.isSynthetic() (Copilot
        // R4) — a user-written literal `item` keyword used as an explicit
        // receiver also occupies its parent op's argument slot, but must keep
        // rendering as plain "item" via the bare-item fallback rather than as
        // MapperS.of(input). See RImplicitVariable.isSynthetic javadoc.
        if (!iv.isSynthetic()) {
            return false;
        }
        RNode parent = iv.parent();
        boolean isElidedOperand =
                (parent instanceof RListOpExpr listOp && listOp.argument() == iv)
             || (parent instanceof RConversionExpr conv && conv.argument() == iv)
             || (parent instanceof RToStringExpr ts && ts.argument() == iv)
             || (parent instanceof RExtractExpr ext && ext.argument() == iv)
             || (parent instanceof RFilterExpr filt && filt.argument() == iv)
             // Engine PR #2 Bucket A — residual 5 without-left visitors.
             || (parent instanceof RCountExpr count && count.argument() == iv)
             || (parent instanceof RSortExpr sort && sort.argument() == iv)
             || (parent instanceof RMinExpr min && min.argument() == iv)
             || (parent instanceof RMaxExpr max && max.argument() == iv)
             || (parent instanceof RReduceExpr reduce && reduce.argument() == iv);
        if (!isElidedOperand) {
            return false;
        }
        // Walk up from the parent op — if we find a wrapping RInlineFunction
        // before the RRule root, we're a nested chain receiver (outer lambda's
        // `item` is the binding). If we hit RFunction (function body, not a
        // rule), fall through to "item" — the rule-input naming guarantee
        // does not hold there. Otherwise (RRule root reached without an
        // intervening inline function) we're at rule-body top-level.
        RNode cur = parent.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) return false;
            if (cur instanceof RRule) return true;
            if (cur instanceof RFunction) return false;
            cur = cur.parent();
        }
        return false;
    }

    /** True iff {@code target} appears on {@code node}'s parent chain (node identity). */
    private static boolean hasAncestorNode(RNode node, RNode target) {
        RNode cur = node.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur == target) {
                return true;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * facet inputFormThenHoist (PR #357): true when {@code iv}'s nearest enclosing inline
     * function is a NAMED extract lambda over the BARE rule input at rule-body top level —
     * the one shape where the lambda item IS the rule input, so upstream renders a
     * name-unprefixed implicit receiver as {@code MapperS.of(input)} (the #355 22/22
     * golden discriminator). The extract must be the rule body's own map: its receiver is
     * the bare implicit input and its parent chain reaches the {@link RRule} without
     * crossing another inline function.
     */
    private static boolean isInsideNamedTopLevelExtractOverBareInput(RImplicitVariable iv) {
        RInlineFunction fn = nearestEnclosingInlineFunction(iv);
        if (fn == null || fn.isImplicit() || fn.paramNames().isEmpty()) {
            return false;
        }
        if (!(fn.parent() instanceof RExtractExpr ext)
                || !(ext.argument() instanceof RImplicitVariable)) {
            return false;
        }
        RNode cur = ext.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return false;
            }
            if (cur instanceof RRule) {
                return true;
            }
            if (cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    // =========================================================================
    // Super call
    // =========================================================================

    /**
     * Compiles a super call into {@code super.doEvaluate()} expression.
     *
     * <p>{@link RSuperCall} is a bare marker node with no fields (no method name,
     * no arguments) — only {@code accept()}. No CDM golden files contain
     * {@code super.doEvaluate()} in function bodies, so this does not block
     * D11 convergence. The rendering is kept for correctness in case dispatch
     * functions with super calls are encountered outside CDM.
     *
     * <p>Full argument threading (passing current function input params to super)
     * would require extending {@link ExpressionContext} with function input names,
     * which is deferred until needed.
     *
     * @param expr     the super call node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler (reserved for future use)
     * @return a {@link JavaExpression} rendering {@code super.doEvaluate()}
     */
    public JavaStatementBuilder handle(RSuperCall expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        return JavaExpression.from("super.doEvaluate()", null);
    }

    /**
     * facet functionImplicitItemArgMeta (PR #340): true iff the bare-invocation seat feeds a
     * STATEMENT/ctor value directly — the parent walk from the call to the enclosing
     * rule/function/operation crosses ONLY extract/lambda/then/ctor/list-literal nodes. Any
     * other ancestor (a logical/comparison/conditional/filter operand — the #257
     * double-render classes) is the OPERAND class. A ROUTE selector since PR #346 (facet
     * itemGetMetaDerefBlock): statement-direct keeps the #340 JavaBlockBuilder; the operand
     * class registers its hoist on the per-pass LAMBDA channel instead of the pre-#346
     * decline (the #340 degradation was the block route's second-pass wrapper-local in a
     * finalized scope — the cp4 Qualify_AssetClass_Equity catch).
     */
    private static boolean implicitArgMetaSeatIsStatementDirect(RSymbolReference expr) {
        RNode cur = expr.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RFunction || cur instanceof RRule || cur instanceof ROperation) {
                return true;
            }
            if (cur instanceof RInlineFunction || cur instanceof RExtractExpr
                    || cur instanceof RThenExpr || cur instanceof RConstructorExpr
                    || cur instanceof com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair
                    || cur instanceof RListLiteral) {
                cur = cur.parent();
                continue;
            }
            return false;
        }
        return false;
    }

    /**
     * facet itemGetMetaDerefBlock (PR #346): the sentinel-rendering wrapper-hoist decl for the
     * LAMBDA channel — {@code final <Wrapper> <token> = item.get();} where {@code <token>} is
     * the deferred-coercion sentinel shared with the arg deref (the #333 dtccDeclUseConsistency
     * law). The decl must NOT be a {@link JavaLocalVariableDeclarationStatement}: its render()
     * calls {@code GeneratedIdentifier.getActualName()}, which closes the identifier's scope AND
     * (via the parent recursion) EVERY ancestor scope — rendered mid-statement by
     * {@code CollectionHandler.compileLambda}'s drain, that poisoned every LATER
     * {@code createUniqueIdentifier} on the shared statement scope (the cp1
     * Qualify_AssetClass_Equity whole-body degradation: the second ladder-arm hoist threw on the
     * closed scope). The token resolves at {@code resolveDeferredCoercionNames} finalization,
     * after all creations — the {@code DeepThenArgHoist} / #237 sentinel-string pattern.
     */
    static final class ItemGetMetaDerefHoist
            extends com.regnosys.rosetta.generator.java.statement.JavaStatement {
        private final String declType;
        private final String token;
        private final String declValue;
        private final Set<JavaClass<?>> refs;
        private final Set<JavaClass<?>> wildcards;

        ItemGetMetaDerefHoist(String declType, String token, String declValue,
                Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
            this.declType = declType;
            this.token = token;
            this.declValue = declValue;
            this.refs = refs;
            this.wildcards = wildcards;
        }

        @Override
        public void render(StringBuilder sb) {
            sb.append("final ").append(declType).append(' ')
              .append(token).append(" = ")
              .append(declValue).append(";\n");
        }

        @Override
        public Set<JavaClass<?>> getRefs() {
            return refs;
        }

        @Override
        public Set<JavaClass<?>> getStaticWildcardImports() {
            return wildcards;
        }
    }

    /**
     * facet existsMetaSeats (PR #331): the marker for a #265 meta-wrap VALUE hoist registered at
     * the CONDITIONAL-ARM seat ({@code extract [if <cond> then <metaRule>]}) — a
     * {@code final <Bare> <name> = <rule>.evaluate(…);} declaration whose consuming ternary sits
     * in the same arm. {@code CollectionHandler.compileElselessConditionalBlock}'s in-branch
     * drain admits it by {@code instanceof} (the #301 numeric-only filter would otherwise
     * DECLINE the block form). Behaviour is identical to the plain declaration statement.
     */
    public static final class MetaWrapValueHoist extends JavaLocalVariableDeclarationStatement {
        private final String sentinelToken;
        private final JavaExpression sentinelInit;

        /**
         * facet ruleCallArmMetaWrap (seat 25, law B): the SENTINEL-rendering hoist — the
         * inherited render() calls {@code GeneratedIdentifier.getActualName()}, which closes
         * the identifier's scope and every ancestor scope (the #346
         * {@code ItemGetMetaDerefHoist} javadoc documents the cp1 catch), so draining the
         * FIRST arm's decl mid-ladder poisoned the SECOND arm's
         * {@code createUniqueIdentifier} (the csa UATPI Leg1/Leg2 two-arm carriers). The decl
         * renders the deferred-coercion sentinel and the name resolves at
         * {@code resolveDeferredCoercionNames} finalization, after all creations — the
         * #237/#346 sentinel-string pattern. Since seat 25 this is the SOLE conditional-arm
         * path (the one production construction passes the token whenever
         * {@code conditionalArmSeat} holds; the pre-seat token-less form has no caller and
         * was removed — the review's B-3): the #331/#372 single-arm carriers' byte-equivalence
         * under the sentinel render is MEASURED, not structural — the b-v mutation's 5F set
         * and the seat's zero-ENTERED matrix on both routes are the locks. Banked (B-14): the
         * generator now carries THREE deferred-token decl renders of this shape (this class,
         * {@code CollectionHandler.DeepThenArgHoist}, the #346 {@code ItemGetMetaDerefHoist})
         * — a LAW-69 consolidation candidate.
         */
        public MetaWrapValueHoist(JavaType type,
                GeneratedIdentifier id, JavaExpression initializer, String sentinelToken) {
            super(true, type, id, initializer);
            this.sentinelToken = sentinelToken;
            this.sentinelInit = initializer;
        }

        @Override
        public void render(StringBuilder sb) {
            if (sentinelToken == null) {
                super.render(sb);
                return;
            }
            sb.append("final ").append(getDeclaredType().getSimpleName()).append(' ')
              .append(sentinelToken).append(" = ");
            sentinelInit.render(sb);
            sb.append(";\n");
        }
    }

    /**
     * facet groupPCounterpartyBlockRelocate (PR #362): the {@link #tryMetaDerefArg} BLOCK-route
     * evaluate-arg wrapper decl ({@code final ReferenceWithMetaParty referenceWithMetaPartyN =
     * <chain>.get();}), MARKER-classed (the MetaWrapValueHoist pattern — behaviour-identical
     * subclass) so {@code CollectionHandler.compileEffectiveElseConditionalBlock}'s per-arm
     * drains can {@code instanceof}-admit it: golden RELOCATES an arm-position evaluate-arg
     * hoist INSIDE the owning branch, before its consuming return (asic/mas valuation
     * Counterparty2Rule: {@code if (…) { final ReferenceWithMetaParty referenceWithMetaParty3
     * = …; …4 = …; return MapperS.of(partyLeiAndPersonByRoles.evaluate(…)); }}). Every other
     * consumer sees the plain {@link JavaLocalVariableDeclarationStatement} contract unchanged
     * (compileLambda's top drain, the elseless block's decline set, the #257 cascade-fallback
     * carriers' inline-ternary form — a cond-position hoist of this class still declines).
     */
    public static final class EvalArgMetaDerefHoist extends JavaLocalVariableDeclarationStatement {
        private final String sentinelName;
        private final JavaType declType;
        private final JavaExpression init;

        EvalArgMetaDerefHoist(JavaType type,
                GeneratedIdentifier id, JavaExpression initializer) {
            this(type, id, initializer, null);
        }

        // facet fnNotionalTogetherRestructure (PR #398): with a sentinel, render emits
        // the deferred token in the decl position (resolved with the consuming guard's
        // token at finalization — one registration, one group entry, identical actuals)
        // instead of materialising the identifier — the plain super.render() calls
        // GeneratedIdentifier.getActualName(), which CLOSES the scope chain
        // mid-compile and poisons later same-function identifier creation (the
        // fixedPriceNotional closed-scope stub; the #346 ItemGetMetaDerefHoist and the
        // #398 facet-A CtorNavMetaDerefHoist precedents).
        EvalArgMetaDerefHoist(JavaType type,
                GeneratedIdentifier id, JavaExpression initializer, String sentinelName) {
            super(true, type, id, initializer);
            this.sentinelName = sentinelName;
            this.declType = type;
            this.init = initializer;
        }

        @Override
        public void render(StringBuilder sb) {
            if (sentinelName == null) {
                super.render(sb);
                return;
            }
            sb.append("final ").append(declType.getSimpleName()).append(' ')
                    .append(sentinelName).append(" = ");
            init.render(sb);
            sb.append(";\n");
        }
    }

    /**
     * facet enumSingletonListCondArm (PR #340): the {@link #tryEnumSingletonListArg} enum-decl
     * pending for the CONDITIONAL-ARM channel — named (the
     * {@code CollectionHandler.DeepThenArgHoist} pattern) so
     * {@code compileEffectiveElseConditionalBlock}'s per-arm drains can {@code instanceof}-gate
     * it: a cond-position hoist renders BEFORE the {@code if (}, an arm-position hoist inside
     * the owning branch (golden's per-occurrence {@code final SupervisoryBodyEnum
     * supervisoryBodyEnumN = …;} placement). The decl renders the deferred token shared with
     * the consumer's singletonList coercion (the #333 dtccDeclUseConsistency law).
     */
    public static final class EnumConstArgHoist
            extends com.regnosys.rosetta.generator.java.statement.JavaStatement {
        private final String declType;
        private final String token;
        private final String declValue;
        private final Set<JavaClass<?>> refs;
        private final Set<JavaClass<?>> wildcards;

        EnumConstArgHoist(String declType, String token, String declValue,
                Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
            this.declType = declType;
            this.token = token;
            this.declValue = declValue;
            this.refs = refs;
            this.wildcards = wildcards;
        }

        @Override
        public void render(StringBuilder sb) {
            sb.append("final ").append(declType).append(' ')
              .append(token).append(" = ")
              .append(declValue).append(";\n");
        }

        @Override
        public Set<JavaClass<?>> getRefs() {
            return refs;
        }

        @Override
        public Set<JavaClass<?>> getStaticWildcardImports() {
            return wildcards;
        }
    }
}
