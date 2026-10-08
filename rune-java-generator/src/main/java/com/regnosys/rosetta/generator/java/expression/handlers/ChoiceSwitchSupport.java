package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;
import com.rosetta.util.types.JavaClass;

/**
 * Shared CHOICE/TYPE-keyed {@code switch} resolution used by BOTH the function-body
 * seat (facet switchChoiceHoist, PR #221 — {@code FunctionExpressionRenderer}'s
 * {@code instanceof} if-else-if ASSIGNMENT block) and the in-lambda seat (facet
 * switchChoiceLambda, PR #226 — {@code CollectionHandler}'s {@code mapItem(item -> { … })}
 * block-lambda with {@code return} arms). Both seats render the SAME upstream
 * {@code instanceof} ladder, so the GATE ({@link #isChoiceTypeSwitch}) and the per-case
 * {@code NAME}-guard TYPE resolution ({@link #resolveCaseType}) are the single source of
 * truth — extracted here verbatim from PR #221's renderer so the two seats can never
 * disagree on which switches qualify or how a case type resolves.
 */
public final class ChoiceSwitchSupport {

    private ChoiceSwitchSupport() {
    }

    /**
     * True when {@code sw}'s argument is inferred as a DATA or CHOICE type (alias-stripped)
     * and every non-default case is a NAME (type) guard whose qualified name is present — the
     * upstream {@code switch} shape that renders the {@code instanceof} block (distinct from the
     * basic-type {@code areEqual(switchArgument, …)} form and the enum-argument {@code ==
     * EnumConst} ladder). Declines for a non-data/choice argument, any non-NAME guard, or no
     * non-default cases — so a switch that does not fully resolve never reaches the block
     * renderer (the caller then falls through to {@code ControlFlowHandler.handle(RSwitchExpr)}'s
     * residual seat's refusal, in render order: {@code LITERAL_SWITCH_TERNARY_STUB} for a literal guard over a
     * lambda-bound subject, {@code TYPE_SWITCH_TERNARY_STUB} per resolvable case type, else
     * {@code SWITCH_TERNARY_STUB} - R1; the LITERAL site since v3.2 seat 7, TYPE_ since v3.1 C0, R1 since
     * v3.2 seat 12 - before each, the waivered ternary).
     *
     * <p>The argument is read via {@link HandlerHelper#orSyntheticImplicit} so an in-lambda
     * {@code item switch …} whose explicit {@code item} argument the parser may elide still
     * types — a no-op when the argument is present (PR #221's function-body seat).
     */
    public static boolean isChoiceTypeSwitch(RSwitchExpr sw, ExpressionCompiler compiler) {
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null) {
            return false;
        }
        // PR #221's gate DELIBERATELY proceeds on an erased (missing/Object) argument type
        // (`fpmlTrade -> product` infers erased while the compile resolves Product) — the
        // discriminator is the NAME guards below, not the arg type — so guard ONLY a null
        // wrapper (never returned for #221's explicit arguments; the synthetic-implicit path
        // for an elided in-lambda `item switch` argument is the one that can be absent).
        RMetaAnnotatedType inferred =
                gm.workspace().getInferredType(HandlerHelper.orSyntheticImplicit(sw.argument(), sw));
        if (inferred == null) {
            return false;
        }
        RType argType = inferred.type();
        argType = HandlerHelper.stripAliases(argType);
        // EXCLUDE the basic-type (areEqual) and enum-argument (== EnumConst) switch shapes — their
        // own renderers own those forms. A data/choice/UNRESOLVED arg type proceeds to the guard
        // check: the discriminator is that every non-default case carries a NAME (type) guard.
        if (argType instanceof RStringType || argType instanceof RNumberType
                || argType == RBasicType.BOOLEAN || argType instanceof REnumTypeRef) {
            return false;
        }
        boolean anyCase = false;
        for (RSwitchCase sc : sw.cases()) {
            if (sc.isDefault()) {
                continue;
            }
            RSwitchCaseGuard guard = sc.guard().orElse(null);
            if (guard == null || guard.kind() != SwitchGuardKind.NAME
                    || guard.qualifiedName().isEmpty()) {
                return false;
            }
            anyCase = true;
        }
        return anyCase;
    }

    /**
     * facet aliasSwitchBareCaseNav (seat 23, law A1) — ONE shape predicate for the two seats that
     * splice a case-narrowed NAV arm of a choice-type switch as the raw Mapper chain (LAW 69 — the
     * #221 SET twin's {@code .get()} collapse in {@code FunctionExpressionRenderer}'s choice-switch
     * assignment and the alias RETURN ladder's {@code compileSwitchLadderValueOrNull}): a case result
     * that is a BARE 1-name symbol (no args, never a function — the #590
     * {@code caseNarrowedBareNavCardinality} class, which the linker binds to the narrowed type's own
     * {@code RAttribute}) or a 2-name DISGUISED chain (an enumeration-empty {@code REnumValueRef} — the
     * #368/#369 class). Both compile, under the #221 subject binding, to the full chain over the bound
     * cast local ({@code MapperS.of(floatingRateCalculation).<Schedule>map(…)}), which a
     * {@code MapperS.of} re-wrap would double-wrap.
     */
    public static boolean isCaseNarrowedNavShapeArm(
            com.regnosys.rosetta.ast.RExpression e) {
        return (e instanceof com.regnosys.rosetta.ast.expressions.references.RSymbolReference bareArm
                        && bareArm.args().isEmpty()
                        && bareArm.symbol().filter(
                                com.regnosys.rosetta.ast.functions.RFunction.class::isInstance).isEmpty())
                || (e instanceof com.regnosys.rosetta.ast.expressions.references.REnumValueRef navEvr
                        && navEvr.enumeration().isEmpty());
    }

    /**
     * facet guardNamespaceAwareResolve (PR #396, Copilot R1 — the Seat-1 OBS-2 seat):
     * resolve a NAME guard's qualified-name string to an {@link RType} with
     * namespace-consistency preference instead of the raw first-wins simple-name
     * lookup. Candidates matching the SIMPLE name are filtered by (a) the guard's
     * QUALIFIER segments when present ({@code fpml.ReturnLeg} → a candidate whose
     * namespace carries the {@code fpml} segment), then (b) the consulting SITE's own
     * model namespace; a still-ambiguous set falls back to the first candidate — the
     * pre-#396 behaviour, so every currently-resolving seat keeps its bytes (the
     * carriers are corpus-unique by simple name; this guard is for the FUTURE
     * collision the widened consumers could otherwise mis-fix).
     */
    public static RType resolveGuardRTypeNamespaceAware(String qualifiedName,
            com.regnosys.rosetta.ast.RNode site, GeneratorModel gm) {
        if (qualifiedName == null || gm == null || gm.workspace() == null) {
            return null;
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        String simple = qualifiedName.substring(lastDot + 1);
        String qualifier = lastDot < 0 ? "" : qualifiedName.substring(0, lastDot);
        java.util.List<com.regnosys.rosetta.ast.RNode> candidates = new java.util.ArrayList<>();
        java.util.List<String> namespaces = new java.util.ArrayList<>();
        for (com.regnosys.rosetta.ast.model.RModel m : gm.workspace().files()) {
            for (var e : m.rootElements()) {
                String eName =
                        e instanceof com.regnosys.rosetta.ast.types.RDataType dt ? dt.name()
                        : e instanceof com.regnosys.rosetta.ast.types.REnumeration en ? en.name()
                        : e instanceof com.regnosys.rosetta.ast.types.RChoice ch ? ch.name()
                        : null;
                if (simple.equals(eName)) {
                    candidates.add(e);
                    namespaces.add(m.namespace() == null ? "" : m.namespace());
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int pick = 0;
        if (candidates.size() > 1) {
            // (a) the guard's own qualifier segments.
            if (!qualifier.isEmpty()) {
                for (int i = 0; i < candidates.size(); i++) {
                    String ns = "." + namespaces.get(i) + ".";
                    if (ns.contains("." + qualifier + ".")) {
                        pick = i;
                        break;
                    }
                }
            } else {
                // (b) the consulting site's own model namespace.
                String siteNs = enclosingModelNamespace(site);
                if (siteNs != null) {
                    for (int i = 0; i < candidates.size(); i++) {
                        if (siteNs.equals(namespaces.get(i))) {
                            pick = i;
                            break;
                        }
                    }
                }
            }
        }
        var elt = candidates.get(pick);
        if (elt instanceof com.regnosys.rosetta.ast.types.RDataType dt) {
            return new com.regnosys.rosetta.types.RDataTypeRef(dt);
        }
        if (elt instanceof com.regnosys.rosetta.ast.types.REnumeration en) {
            return new com.regnosys.rosetta.types.REnumTypeRef(en);
        }
        if (elt instanceof com.regnosys.rosetta.ast.types.RChoice ch) {
            return new com.regnosys.rosetta.types.RChoiceTypeRef(ch.name(), java.util.List.of(), ch);
        }
        return null;
    }

    /** The enclosing {@link com.regnosys.rosetta.ast.model.RModel}'s namespace, or null. */
    private static String enclosingModelNamespace(com.regnosys.rosetta.ast.RNode site) {
        com.regnosys.rosetta.ast.RNode cur = site;
        int depth = 0;
        while (cur != null && depth++ < 512) {
            if (cur instanceof com.regnosys.rosetta.ast.model.RModel model) {
                return model.namespace();
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * Resolve a choice-type switch case's NAME (type) guard to its Java type: the linker's
     * {@code resolvedGuard} node when present (an {@code RDataType}/{@code RChoice} — the
     * import/alias-aware binding, PR #460), else the SIMPLE name against the loaded workspace
     * ({@link GeneratorModel#resolveTypeByName} — the pre-#460 path, kept for guards the linker
     * left unresolved), translated to its Java type. Returns {@code null} (caller declines the
     * whole switch) when the guard is not a NAME guard or its type does not resolve to a
     * {@link JavaClass}.
     */
    public static JavaClass<?> resolveCaseType(RSwitchCaseGuard guard, ExpressionCompiler compiler) {
        if (guard.kind() != SwitchGuardKind.NAME) {
            return null;
        }
        String qn = guard.qualifiedName().orElse(null);
        if (qn == null) {
            return null;
        }
        // Decline (rather than NPE) when the compiler carries no GeneratorModel / translator — the
        // stateless unit-compiler mode (`new ExpressionCompiler()`). Both real callers (the #221
        // function-body seat + the #226 in-lambda seat) gate on isChoiceTypeSwitch (which already
        // returns false for a null gm) BEFORE reaching here, but a public util must be defensively
        // consistent with the other handler helpers (Copilot R1).
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null || compiler.getTypeTranslator() == null) {
            return null;
        }
        RType caseType = resolvedGuardRType(guard);
        if (caseType == null) {
            String simple = qn.substring(qn.lastIndexOf('.') + 1);
            caseType = gm.resolveTypeByName(simple).orElse(null);
        }
        if (caseType == null) {
            return null;
        }
        var jt = compiler.getTypeTranslator().toJavaReferenceType(caseType);
        return jt instanceof JavaClass<?> jc ? jc : null;
    }

    /**
     * facet caseGuardResolvedBinding (PR #460 — the plugin-composition wave): the linker's
     * {@code resolvedGuard} as an {@link RType}, or {@code null} when the guard is absent,
     * unresolved, or resolved to a non-type node (an enum VALUE). The linker's store is the
     * AUTHORITATIVE binding for a NAME guard — {@code GlobalResolutionPass
     * .resolveSwitchGuardType} resolves the WRITTEN qualified name through the file's
     * import/alias-aware ladder (an alias-qualified guard like {@code fpml.Loan} binds through
     * {@code import fpml.consolidated.asset.* as fpml}), and the engine's switch-case item
     * narrowing consumes the SAME node. Every generator seat that re-derives a case type from
     * the guard's NAME consults this FIRST: the simple-name re-resolution those seats fall back
     * to ({@link GeneratorModel#resolveTypeByName} — first match in workspace FILE order) is
     * composition-order sensitive, and under the maven-plugin's single flat sorted sourceRoot
     * the cdm.base files register before the fpml payload, so the colliding simple names in
     * cdm6 MapAsset ({@code fpml.Loan}/{@code fpml.Commodity} vs
     * {@code cdm.base.staticdata.asset.common}) bound the WRONG namespace — the #459
     * compile-gate finding; the D11 loader's dependency-payload-first order had masked it.
     * The RChoice arm mirrors {@code GeneratorModel.resolveTypeByName}'s empty-options
     * {@code RChoiceTypeRef} shape, so downstream {@code asRDataType()} consumers see the
     * fallback's exact form. Guards the linker left unresolved keep each seat's pre-#460
     * fallback byte-for-byte.
     */
    public static RType resolvedGuardRType(RSwitchCaseGuard guard) {
        var resolved = guard == null ? null : guard.resolvedGuard().orElse(null);
        if (resolved instanceof com.regnosys.rosetta.ast.types.RDataType dt) {
            return new com.regnosys.rosetta.types.RDataTypeRef(dt);
        }
        if (resolved instanceof com.regnosys.rosetta.ast.types.RChoice ch) {
            return new com.regnosys.rosetta.types.RChoiceTypeRef(ch.name(), java.util.List.of(), ch);
        }
        return null;
    }

    /** One hop of a choice option path: the projected option attribute + its resolved types. */
    public record ChoiceOptionHop(com.regnosys.rosetta.ast.supporting.RAttribute attr,
            RType optType, JavaClass<?> optJC) {}

    /**
     * facet choiceDeepNavLadderArm (PR #396): upstream {@code findOptionPath} — DFS the
     * choice's projected options in DECLARATION order; the first option under which the
     * goal is reachable wins; descent recurses ONLY into nested-choice options (an exact
     * option NAME match terminates — the type-name law, namespace-correct because the
     * tree itself is). The name-keyed option lookup pairs the projection attribute with
     * its {@code options()} RType (the parser skips unresolved options, so positional
     * pairing could skew). False when the goal is unreachable (the caller declines the
     * whole ladder).
     *
     * <p>Promoted here (seat 31, facet choiceOptionNavLadderDeepHop) from
     * {@code ControlFlowHandler}'s private copy so the THREE ladder seats — the #396
     * conditional-arm form, the datarule RETURN-ladder form (both ControlFlowHandler),
     * and the #394 SET-seat option-nav ladder ({@code FunctionExpressionRenderer}) —
     * consult ONE walk (the two-halves-agree law): a path the arm seat can render is
     * exactly a path the SET seat can render, hop for hop.
     */
    public static boolean findChoiceOptionPath(com.regnosys.rosetta.types.RChoiceTypeRef choice,
            String goalSimple, ExpressionCompiler compiler, java.util.List<ChoiceOptionHop> out,
            java.util.Set<String> visited) {
        if (!visited.add(choice.name())) {
            return false;
        }
        // Derive option types from the choice's AST node — a typeCall-resolved choice
        // ref carries an EMPTY options() list (the T396A cp5 read: Underlier opts=0);
        // the asRDataType projection iterates astNode.options() 1:1, so positional
        // pairing with the projected attrs is exact.
        com.regnosys.rosetta.ast.types.RChoice ast = choice.astNode();
        com.regnosys.rosetta.ast.types.RDataType proj = choice.asRDataType();
        var tt = compiler.getTypeTranslator();
        GeneratorModel gm = compiler.getGeneratorModel();
        if (ast == null || proj == null || tt == null || gm == null
                || proj.attributes().size() != ast.options().size()) {
            return false;
        }
        for (int i = 0; i < ast.options().size(); i++) {
            var astOpt = ast.options().get(i);
            com.regnosys.rosetta.ast.supporting.RAttribute attr = proj.attributes().get(i);
            if (astOpt.typeCall() == null) {
                continue;
            }
            RType optT = HandlerHelper.stripAliases(gm.resolveTypeCall(astOpt.typeCall()));
            if (optT == null) {
                continue;
            }
            JavaClass<?> optJC = tt.toJavaReferenceType(optT) instanceof JavaClass<?> jc
                    ? jc : null;
            if (optJC == null || "Object".equals(optJC.getSimpleName())) {
                continue;
            }
            ChoiceOptionHop hop = new ChoiceOptionHop(attr, optT, optJC);
            if (goalSimple.equals(optT.name())) {
                out.add(hop);
                return true;
            }
            if (optT instanceof com.regnosys.rosetta.types.RChoiceTypeRef nested) {
                out.add(hop);
                if (findChoiceOptionPath(nested, goalSimple, compiler, out, visited)) {
                    return true;
                }
                out.remove(out.size() - 1);
            }
        }
        return false;
    }

    /**
     * v3.2 seat 7 (F11, facet literalSwitchLambdaBlock): render a switch-case guard as the {@code MapperS.of(<operand>)}
     * second argument of {@code areEqual(<subject>, …)} — extracted VERBATIM from
     * {@code FunctionExpressionRenderer.renderSwitchGuardMapper} (the SET seat's basic-type switch, which now delegates
     * here) so the in-lambda literal-guard block ({@code CollectionHandler.compileLiteralSwitchBlockLambda}) and the SET
     * seat cannot disagree on a guard's operand (LAW 69): the operand is quoted as a Java string literal iff the switch
     * argument's element type is a string (upstream coerces every guard literal to the switchArgument element type, so a
     * numeric-looking guard against a string switch renders {@code MapperS.of("1")}); a boolean switch renders the lowered
     * bare literal ({@code MapperS.of(true)} — the #365 F-B law); a non-int NUMBER switch coerces an int-shaped guard to
     * {@code MapperS.of(BigDecimal.valueOf(1))} (the #418 switchGuardNumberCoerce law); an int switch renders the bare
     * literal. Seeds MAPPER_S (and BIG_DECIMAL where used) into {@code refs}. Returns {@code null} — the caller declines the
     * whole switch render — when the guard payload is absent.
     */
    public static String renderGuardMapper(RSwitchCaseGuard guard, RType switchArgType,
            java.util.Set<JavaClass<?>> refs) {
        String literal = guard.kind() == SwitchGuardKind.LITERAL
                ? guard.literalValue().orElse(null)
                : guard.qualifiedName().orElse(null);
        if (literal == null) {
            return null;
        }
        refs.add(HandlerHelper.MAPPER_S);
        if (switchArgType instanceof RStringType) {
            String escaped = literal.replace("\\", "\\\\").replace("\"", "\\\"");
            return "MapperS.of(\"" + escaped + "\")";
        }
        // facet condArmBasicSwitch (PR #365, F-B): the rosetta boolean literals are the
        // CAPITALIZED keywords (`True then Correct`), so a Boolean-argument switch guard
        // carries the raw "True"/"False" literal text — lower it to the Java literal
        // (golden `MapperS.of(true)`; the raw splice `MapperS.of(True)` never compiled,
        // so no green file carries it). Exact-match only; every other literal is
        // spliced verbatim as before. (The render moved here VERBATIM at v3.2 seat 7; this
        // measured justification restored at round 1 — the code-quality review's SF-2.)
        if (switchArgType == RBasicType.BOOLEAN
                && ("True".equals(literal) || "False".equals(literal))) {
            return "MapperS.of(" + literal.toLowerCase(java.util.Locale.ROOT) + ")";
        }
        // facet switchGuardNumberCoerce (PR #418): the javadoc law above ("upstream
        // coerces every guard literal to the switchArgument element type") gets its
        // NUMBER half — under a number-typed (non-int) switch argument an int-shaped
        // guard renders the joined BigDecimal form: oracle golden expr-switch
        // `areEqual(switchArgument, MapperS.of(BigDecimal.valueOf(1)), …)`. The
        // fork's bare splice `MapperS.of(1)` passed MapperS<Integer> where the
        // subject is MapperS<BigDecimal> — never green, so only already-waivered
        // carriers move (divergent=1 → zero corpus carriers; D11 proves by
        // measurement). Int-SHAPED (all-digits, long-range — the emitted
        // BigDecimal.valueOf(long) overload) literals only; decimal/signed guards
        // keep the verbatim splice (oracle-unwitnessed). (Restored at round 1, as above.)
        if (switchArgType instanceof RNumberType numType && !numType.isInteger()
                && isLongRangeDigits(literal)) {
            refs.add(HandlerHelper.BIG_DECIMAL);
            return "MapperS.of(BigDecimal.valueOf(" + literal + "))";
        }
        return "MapperS.of(" + literal + ")";
    }

    /**
     * facet switchGuardNumberCoerce (PR #418; moved here at v3.2 seat 7 with {@link #renderGuardMapper}): true iff
     * {@code s} is a plain unsigned digit run that fits in a {@code long} — the only literal shape the guard coercion emits
     * through {@code BigDecimal.valueOf(long)}. Trivial literal-token classification (the engineering-standards exception),
     * not structural parsing.
     */
    public static boolean isLongRangeDigits(String s) {
        if (s == null || s.isEmpty() || s.length() > 19) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        try {
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * v3.2 seat 7 round 1 (the code-quality review's SF-6; LAW 69): the literal ARM / DEFAULT kinds a LITERAL-guarded
     * lambda-item switch admits — string and int — ONE declaration for both halves of the law: the literal-guard
     * block's admission ({@code CollectionHandler.compileLiteralSwitchBlockLambda}, its arms and its default) and the
     * alias signature walk's ({@code FunctionAliasHelper.switchValueCaseJoin}, the default and the arm gates under
     * {@code literalGuarded}). The witnesses (round 2, the spec review's NIT-1): STRING at both halves (golden
     * ControlLiteral, the chaos C18ToKind {@code switched} alias); INT at the SIGNATURE half only (the seat-2 int
     * ladder, a then-body switch) — the block half admits it as the pre-seat kind it already was, unwitnessed there.
     * A number or boolean literal under literal guards is UNWITNESSED (no oracle golden) and declines at BOTH halves:
     * the signature to {@code null}, the block to {@code ControlFlowHandler}'s residual path, which REFUSES it at
     * {@code SilentDegradation.Site#LITERAL_SWITCH_TERNARY_STUB} (round 2, the cq review's MF-1 — the pre-seat
     * fall-through was the SILENT always-false {@code Objects.equals} ternary). So no LITERAL-armed block can leave
     * its alias signature to the raw-name output-type fallback (the shape the review named:
     * {@code alias a: [raw] extract (item switch "r" then 1.5, default 2.5)}). The block's OTHER admitted arm kinds
     * under literal guards — the bare item, a function call, an item-rooted nav, a to-string over one — and a
     * closure-parameter SUBJECT are signed by the walk under NAME guards only, so an ALIAS carrying one of them under
     * literal guards still reaches the fallback (the cq review's round-2 SF-1): both shapes BANKED with the oracle
     * group {@code alias-literal-switch-number-boolean-arms}, which is chartered to pin them all FIRST. Under NAME
     * guards the four kinds of {@code admissibleBlockSwitchCaseBody} stand (NumberArms, the boolean defaults), signed
     * by the walk's {@code valueShaped} arm.
     */
    public static boolean isLiteralGuardedLambdaArmLiteral(com.regnosys.rosetta.ast.RExpression e) {
        return e instanceof com.regnosys.rosetta.ast.expressions.literals.RStringLiteral
                || e instanceof com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
    }

    /**
     * v3.2 seat 7 round 1: a literal of any of the four kinds the block seats admit (string / int / number / boolean).
     * Byte-identical to {@code ControlFlowHandler.isValueLiteral} (seat 1's conditional-initializer arm class) - a
     * DIFFERENT law over the same four kinds, declared as a twin rather than consulted (round 3, the code-quality
     * review's NIT-5).
     */
    public static boolean isBlockSwitchLiteral(com.regnosys.rosetta.ast.RExpression e) {
        return e instanceof com.regnosys.rosetta.ast.expressions.literals.RStringLiteral
                || e instanceof com.regnosys.rosetta.ast.expressions.literals.RIntLiteral
                || e instanceof com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral
                || e instanceof com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
    }
}
