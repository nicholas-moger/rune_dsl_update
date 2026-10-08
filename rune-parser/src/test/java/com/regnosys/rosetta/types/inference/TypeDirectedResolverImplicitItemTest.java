package com.regnosys.rosetta.types.inference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;

/**
 * Phase X1 T2.0 — unit tests for the M3 implicit-input feature-call resolution
 * mechanism (Categories 8+9+10 in TypeInferenceEngine + resolveImplicitItemFeatureCall
 * on TypeDirectedResolver). Category 10 (REnumValueRef → AttributeChain fallback
 * for the grammar's {@code IDENT -> IDENT} disambiguation) is also covered here —
 * see tests #3, #14, and #15 (cross-namespace multi-level extends).
 *
 * <p>Spec: docs/superpowers/specs/2026-05-21-phase-x1-m3-implicit-input-extension.md (local).
 * <p>Architecture: Approach B per spec § 3 — extend the existing fixed-point +
 * type-directed pattern that already handles RFeatureCall (Category 1) +
 * RDeepFeatureCall (Category 2) + switch case guards (Category 4) inside
 * {@code TypeInferenceEngine.runTypeDirectedResolution}. (Line-number citations
 * intentionally omitted — they drift as the method grows; refer to the method
 * name + category numbers instead.)
 *
 * <p>Plan: the development plan "2026-05-21-phase-x1-T2.0-m3-amendment" committed at b5a402d.
 */
class TypeDirectedResolverImplicitItemTest {

    /** Parse + link a source string; returns the built workspace. */
    private RWorkspace parseAndLink(String source) {
        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        RLinkingResult result = RWorkspace.build(List.of(model));
        return result.workspace();
    }

    /**
     * Returns true when {@code d} is a {@link LinkingDiagnostic} carrying a
     * {@link DiagnosticCategory#SYMBOL_NOT_FOUND} category with
     * {@code unresolvedName == expectedName}. Replaces brittle
     * {@code d.toString().contains("Symbol 'X' not found")} string matching
     * per Copilot PR #76 R10 F3 — diagnostic rendering is not a stable API
     * and can change without breaking the diagnostic's structural contract.
     * Mirrors the structured-field assertion pattern already in
     * {@code LexicalResolutionTest.unresolved_symbol_emits_diagnostic}.
     */
    private static boolean hasSymbolNotFound(
            com.regnosys.rosetta.symbols.diagnostics.RDiagnostic d,
            String expectedName) {
        return d instanceof LinkingDiagnostic ld
                && ld.category() == DiagnosticCategory.SYMBOL_NOT_FOUND
                && expectedName.equals(ld.unresolvedName());
    }

    // === Test #1 — rule body `extract id` resolves to Trade.id ================

    @Test
    void extractId_overRuleFromTrade_resolvesIdToTradeIdAttribute() {
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "reporting rule R from Trade:",
                "    extract id");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        RExtractExpr extractExpr = (RExtractExpr) rule.expression().orElseThrow();
        RSymbolReference idRef = (RSymbolReference) extractExpr.body().body();

        assertTrue(idRef.symbol().isPresent(),
                "extract id: bare 'id' must resolve via Category 9 against "
                + "implicit item type Trade — got unresolved");
        assertTrue(idRef.symbol().get() instanceof RAttribute,
                "resolved symbol must be an RAttribute (Trade.id); got "
                + idRef.symbol().get().getClass().getName());
        RAttribute resolvedAttr = (RAttribute) idRef.symbol().get();
        assertEquals("id", resolvedAttr.name(),
                "resolved attribute must be the 'id' attribute on Trade");
    }

    // === Test #2 — extract id over List<Trade> resolves element type ==========

    @Test
    void extractId_overListOfTrade_resolvesIdAsElementAttribute() {
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "func F:",
                "    inputs: trades Trade (0..*)",
                "    output: result string (0..*)",
                "    set result:",
                "        trades extract id");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference idRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "id".equals(r.name()))
                .findFirst().orElseThrow();

        assertTrue(idRef.symbol().isPresent(),
                "extract id over List<Trade>: id must resolve element-wise to Trade.id");
    }

    // === Test #3 — extract deep path `extract a -> b` =========================

    @Test
    void extractDeepPath_aArrowB_resolvesChain() {
        String source = String.join("\n",
                "namespace test",
                "type Inner:",
                "    b string (1..1)",
                "type Outer:",
                "    a Inner (1..1)",
                "reporting rule R from Outer:",
                "    extract a -> b");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        // Grammar disambiguation parses `a -> b` as REnumValueRef (the
        // EnumName -> ValueName syntax takes precedence at parse time).
        // When the enum-resolution fails AND the node is inside an implicit
        // inline body, Category 10 must rewrite it as an attribute-feature
        // chain (a -> b becomes attribute `a` on the implicit-item type, then
        // feature `b` on `a`'s type).
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "extract a -> b: grammar produces an REnumValueRef for the "
                + "`a -> b` shape; AST must contain one");
        var enumRef = enumRefOpt.get();
        assertTrue(enumRef.resolvedAttributeChain().isPresent(),
                "extract a -> b: Category 10 must rewrite the unresolved "
                + "REnumValueRef('a','b') into an attribute-feature chain "
                + "when enum resolution fails inside an implicit inline body "
                + "and the implicit-item type has an attribute named 'a'");
        var chain = enumRef.resolvedAttributeChain().get();
        assertEquals("a", chain.attribute().name(),
                "Category 10: first segment resolves to the attribute on "
                + "the implicit-item type (Outer.a)");
        assertEquals("b", chain.feature().name(),
                "Category 10: second segment resolves to the feature on the "
                + "first segment's element type (Inner.b)");
    }

    // === Test #4 — filter with bare attribute reference =======================

    @Test
    void filterPrice_overListOfItem_resolvesPriceAttribute() {
        String source = String.join("\n",
                "namespace test",
                "type Item:",
                "    price int (1..1)",
                "func F:",
                "    inputs: items Item (0..*)",
                "    output: result Item (0..*)",
                "    set result:",
                "        items filter price > 100");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference priceRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "price".equals(r.name()))
                .findFirst().orElseThrow();

        assertTrue(priceRef.symbol().isPresent(),
                "filter price>100: price must resolve to Item.price via Category 9");
    }

    // === Test #5 — chained `trades then extract id` ===========================

    @Test
    void thenExtractChained_innermostItemIsOuterElementType() {
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "func F:",
                "    inputs: trades Trade (0..*)",
                "    output: result string (0..*)",
                "    set result:",
                "        trades then extract id");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference idRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "id".equals(r.name())).findFirst().orElseThrow();
        assertTrue(idRef.symbol().isPresent(),
                "trades then extract id: second op's implicit-item must be Trade "
                + "(first op's element type); id must resolve to Trade.id");
    }

    // === Test #6 — literal `item` keyword + feature-call chain (Cat 8 → Cat 1) =
    // The `item` token in `extract item -> id` is the grammar's ITEM keyword,
    // which `AstBuilder.visitImplicitVarExpr` (line 5369) maps to an
    // `RImplicitVariable` AST node — NOT an explicit closure parameter. The
    // resolution path is: Cat 8 sets RImplicitVariable's type to the implicit
    // item type (Trade); then Cat 1 (RFeatureCall) resolves `id` as a feature
    // on the now-typed receiver. The two passes must converge via the
    // fixed-point loop. Test originally framed as "explicit closure form via
    // RInlineFunctionScope" which was semantically wrong — corrected at
    // PR #76 R2 F11.
    // =========================================================================

    @Test
    void literalItemKeyword_arrowFeatureCall_resolvesViaCat8ThenCat1() {
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "reporting rule R from Trade:",
                "    extract item -> id");
        RWorkspace ws = parseAndLink(source);

        assertFalse(ws.diagnostics().stream()
                .anyMatch(d -> hasSymbolNotFound(d, "id")),
                "literal `item` + feature `id`: Cat 8 (implicit-var receiver) "
                + "must establish Trade as receiver type, then Cat 1 resolves "
                + "`id` as a feature on Trade — fixed-point must converge");
    }

    // === Test #7 — `reduce` with explicit closure params ======================

    @Test
    void reduceWithExplicitParams_implicitItemDoesNotFire() {
        String source = String.join("\n",
                "namespace test",
                "func Sum:",
                "    inputs: values int (0..*)",
                "    output: result int (1..1)",
                "    set result:",
                "        values reduce a, b [a + b]");
        RWorkspace ws = parseAndLink(source);

        assertFalse(ws.diagnostics().stream()
                .anyMatch(d -> hasSymbolNotFound(d, "a")),
                "reduce with explicit a,b: 'a' must resolve via "
                + "RInlineFunctionScope; Category 9 must not interfere");
        assertFalse(ws.diagnostics().stream()
                .anyMatch(d -> hasSymbolNotFound(d, "b")),
                "reduce with explicit a,b: 'b' must resolve via "
                + "RInlineFunctionScope; Category 9 must not interfere");
    }

    // === Test #8 — unknown attribute stays unresolved =========================

    @Test
    void extractUnknownAttribute_returnsUnresolved() {
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "reporting rule R from Trade:",
                "    extract notARealAttribute");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference badRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "notARealAttribute".equals(r.name()))
                .findFirst().orElseThrow();

        assertFalse(badRef.symbol().isPresent(),
                "unknown attribute name on implicit item must stay unresolved");
    }

    // === Test #9 — implicit-item is the resolution fallback when M3 doesn't
    //               bind the bare ref in any outer scope. Cat 9 is gated on
    //               `symbol().isEmpty()` (set by M3 lexical scope), so it
    //               fires ONLY when M3 found no outer binding. If an outer
    //               scope (e.g. a function named `id`) DID bind the ref,
    //               that binding stands and Cat 9 does not fire.
    //               Test originally claimed "shadows outer scope" which was
    //               wrong per the Cat 9 guard — corrected at PR #76 R2 F5.
    // =========================================================================

    @Test
    void implicitItem_resolvesBareId_whenNoOuterScopeBinding() {
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "reporting rule R from Trade:",
                "    extract id");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference idRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "id".equals(r.name())).findFirst().orElseThrow();

        assertTrue(idRef.symbol().isPresent(),
                "id must resolve to Trade.id via Category 9 fallback "
                + "(no outer-scope `id` binding exists at namespace level)");
        RAttribute resolvedAttr = (RAttribute) idRef.symbol().get();
        assertEquals("id", resolvedAttr.name(),
                "resolved attribute must be the implicit-item's 'id' — "
                + "Cat 9 fires because M3's lexical scope chain (synthetic "
                + "'input' only inside rule scope, no other 'id' in outer "
                + "namespace) leaves ref.symbol() empty, so the fallback "
                + "engages and binds to Trade.id");
    }

    // === Test #10 — nested `extract` inside `extract` =========================

    @Test
    void nestedExtract_innerItemIsOuterElement() {
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "func F:",
                "    inputs: trades Trade (0..*)",
                "    output: result string (0..*)",
                "    set result:",
                "        trades extract [ extract id ]");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference idRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "id".equals(r.name())).findFirst().orElseThrow();
        assertTrue(idRef.symbol().isPresent(),
                "nested extract id: inner implicit-item must be Trade "
                + "(element type of outer extract's argument)");
    }

    // === Test #11 — literal `item` keyword uses Category 8 ====================

    @Test
    void literalItemKeyword_resolvesViaCategory8() {
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "reporting rule R from Trade:",
                "    extract item -> id");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var ivOpt = AstWalker.findAll(model, RImplicitVariable.class)
                .stream().findFirst();
        assertTrue(ivOpt.isPresent(),
                "literal 'item' keyword source must produce an RImplicitVariable "
                + "AST node via AstBuilder.visitImplicitVarExpr");
        var iv = ivOpt.get();
        assertFalse(ws.getInferredType(iv).isMissing(),
                "Category 8 must populate the implicit-item type for "
                + "RImplicitVariable; got MISSING — Category 8 didn't fire");
    }

    // === Test #12 — fixed-point convergence stays bounded =====================

    @Test
    void fixedPointIteration_convergesBounded() {
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "reporting rule R from Trade:",
                "    extract id");
        RWorkspace ws = parseAndLink(source);

        int iterations = ws.typeInferenceIterations();
        assertTrue(iterations >= 1 && iterations <= 5,
                "fixed-point must converge within 5 iterations for a trivial "
                + "rule; got " + iterations);
    }

    // === Test #13 — Cat 9 args-guard: function-call shape NOT bound ===========
    // Regression test for Phase X1 R1 F1 (PR #76 Copilot review). The grammar
    // visitFunctionCallExpr (AstBuilder line 5319) emits an RSymbolReference
    // WITH populated args() for the `foo(x)` shape. Cat 9's implicit-item
    // attribute lookup must NOT fire on this shape — binding a function-call
    // to an implicit-item attribute named `foo` and clearing its
    // SYMBOL_NOT_FOUND would mis-classify a legitimately-unresolved call.
    // Guard: `ref.args().isEmpty()` at TypeInferenceEngine line 153.

    @Test
    void functionCallShape_insideImplicitBody_notBoundByCat9() {
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "reporting rule R from Trade:",
                "    extract id(id)");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        RExtractExpr extractExpr = (RExtractExpr) rule.expression().orElseThrow();
        RSymbolReference callRef = (RSymbolReference) extractExpr.body().body();

        assertFalse(callRef.args().isEmpty(),
                "test precondition: function-call shape must have populated args() "
                + "from visitFunctionCallExpr — sanity check on grammar");
        assertTrue(callRef.symbol().isEmpty(),
                "Cat 9 must NOT resolve a function-call-shape RSymbolReference "
                + "(args populated) against the implicit-item attribute table; "
                + "got resolved-symbol on a call shape, indicating a mis-bind");
    }

    // === Test #14 — multi-level extends + extract a -> b ======================
    // Phase X1 shape coverage (2026-05-22). Reproduces the dominant remaining
    // RExtractExpr failure shape from the drr/7.0.0-dev.113 focused D11 run:
    // rule `EnrichmentData from TransactionReportInstruction: extract reportableInformation -> enrichment`
    // where `reportableInformation` is inherited via 2 `extends` hops.
    // Cat 10's findAttributeOnDataType must follow the extends chain.

    @Test
    void extractDeepPath_multiLevelExtends_resolvesChain() {
        String source = String.join("\n",
                "namespace test",
                "type Inner:",
                "    leaf string (1..1)",
                "type GrandBase:",
                "    deep Inner (1..1)",
                "type Middle extends GrandBase:",
                "    middleField string (1..1)",
                "type Top extends Middle:",
                "    topField string (1..1)",
                "reporting rule R from Top:",
                "    extract deep -> leaf");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "multi-extends + extract a -> b: AST must contain an REnumValueRef");
        var enumRef = enumRefOpt.get();
        assertTrue(enumRef.resolvedAttributeChain().isPresent(),
                "Cat 10 must follow multi-level extends to find 'deep' on Top — "
                + "Top extends Middle extends GrandBase; 'deep' is on GrandBase");
        var chain = enumRef.resolvedAttributeChain().get();
        assertEquals("deep", chain.attribute().name());
        assertEquals("leaf", chain.feature().name());
    }

    // === Test #15 — cross-namespace multi-extends via fixtures ================
    // Same shape as Test #14 but spread across 4 namespaces, matching the
    // corpus failure pattern. Uses SymbolIdTestFixtures.parse for fixture-
    // based loading (buildFromString called per file).

    @Test
    void extractDeepPath_crossNamespaceMultiExtends_resolvesChain() throws Exception {
        List<RModel> files = com.regnosys.rosetta.ast.symbolid.SymbolIdTestFixtures.parse(
                "cat10-cross-namespace/");
        com.regnosys.rosetta.symbols.RLinkingResult result = RWorkspace.build(files);
        RWorkspace ws = result.workspace();

        // Find any REnumValueRef across all files; there should be one in rule.rosetta.
        var enumRefOpt = files.stream()
                .flatMap(m -> AstWalker.findAll(m,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class).stream())
                .findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "cross-namespace multi-extends: AST must contain REnumValueRef for `deep -> leaf`");
        var enumRef = enumRefOpt.get();
        assertTrue(enumRef.resolvedAttributeChain().isPresent(),
                "Cat 10 must follow cross-namespace + multi-level extends chain "
                + "to resolve `deep -> leaf` on Top (Top extends Middle extends "
                + "GrandBase; `deep` defined on GrandBase in namespace b.base; "
                + "rule in namespace d.consumer imports c.top.*). "
                + "Diagnostics: " + ws.diagnostics());
    }

    // === Test #16 — explicit closure param as leading REnumValueRef name ======
    // Phase X1 closure (D42 LOCK 2026-05-23). Reproduces the DRR corpus pattern
    // `extract X [X -> field ...]` where X is the EXPLICIT closure parameter
    // (a local variable), not an attribute on the implicit-item type. The
    // standard 2-step Cat 10 lookup tries to find X as a field on itemType,
    // which fails because X is the param NAME. Cat 10's closure-param branch
    // detects the match + binds a one-segment AttributeChain.

    // === Test #17 — func-call → feature M3 extension (Gap A) ==================
    // Phase X1 closure T0h (2026-05-23). The DRR corpus pattern
    // `EconomicTermsForProduct -> payout` parses as REnumValueRef
    // (enumName=EconomicTermsForProduct, valueName=payout) per the
    // {EnumName -> ValueName} grammar production, but EconomicTermsForProduct
    // is a workspace `func`, not an enum. GlobalResolutionPass now binds
    // resolvedSymbol when the LHS resolves to RFunction; TypeInferenceEngine's
    // Cat 10 extension then looks up valueName as a feature on the func's
    // output type.

    @Test
    void enumValueRef_lhsIsFuncWithFeatureOnOutput_resolvesViaFuncChain() {
        // Fixture: a func F returning an Inner with field `payload`, and a rule
        // body `F -> payload` that should type-resolve to the payload's type.
        String source = String.join("\n",
                "namespace test",
                "type Inner:",
                "    payload string (1..1)",
                "type Top:",
                "    leaf string (1..1)",
                "func F:",
                "    output: out Inner (1..1)",
                "    set out -> payload: \"x\"",
                "reporting rule R from Top:",
                "    F -> payload");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream()
                // Skip the operation-path enum-value-ref inside `set out -> payload`
                // (that one resolves via the standard operation path; we want the
                // one inside the rule body that targets the func F).
                .filter(e -> "F".equals(e.enumName()) && "payload".equals(e.valueName()))
                .findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "Gap A test: AST must contain an REnumValueRef for "
                + "`F -> payload` in the rule body");
        var enumRef = enumRefOpt.get();
        assertTrue(enumRef.enumeration().isEmpty(),
                "LHS `F` must NOT resolve as an enum (F is a func)");
        assertTrue(enumRef.resolvedSymbol().isPresent(),
                "GlobalResolutionPass must bind resolvedSymbol to the func F "
                + "when LHS resolves to a non-enum callable. Diagnostics: "
                + ws.diagnostics());
        assertTrue(
                enumRef.resolvedSymbol().get() instanceof com.regnosys.rosetta.ast.functions.RFunction,
                "resolvedSymbol must be an RFunction");
        assertTrue(enumRef.resolvedAttributeChain().isPresent(),
                "Cat 10 Gap A branch must complete the chain by looking up "
                + "`payload` as a feature on F's output type. Diagnostics: "
                + ws.diagnostics());
        var chain = enumRef.resolvedAttributeChain().get();
        assertTrue(chain.attributeOpt().isEmpty(),
                "Gap A shape uses the one-segment AttributeChain ctor "
                + "(no first-segment attribute; resolvedSymbol holds the LHS)");
        assertEquals("payload", chain.feature().name(),
                "Gap A: feature should be `payload` resolved on F's output type Inner");
    }

    // === Test #18 — T0i Gap B: attribute → type-restriction (downcast) =========
    // The grammar parses `<attribute> -> <Subtype>` as REnumValueRef
    // (enumName=<attribute>, valueName=<Subtype>) per the
    // {EnumName -> ValueName} production precedence. In Rosetta semantics this
    // is the type-restriction (downcast) operator — narrow the polymorphic
    // attribute to its declared subtype. Cat 12 (T0i Gap B) resolves it when:
    //   (a) LHS resolves to an attribute on item-type,
    //   (b) RHS resolves to a workspace RDataType (bound at Phase A as
    //       resolvedRestrictionType speculation), AND
    //   (c) RHS is a subtype of LHS's element type (SubtypeRelation check at
    //       Phase B).
    // On verification success, binds a full TypeRestriction(lhsAttribute,
    // restrictionType) record + clears ENUM_NOT_FOUND/ENUM_VALUE_NOT_FOUND.
    //
    // Empirical corpus shape per T0g § 3: payout/OptionPayout × 18 +
    // payout/CommodityPayout × 5 + payout/SettlementPayout × 5 +
    // payout/InterestRatePayout × 2 + payout/PerformancePayout × 2 = ~33 direct
    // entries on focused D11 drr/7.0.0-dev.113 POJO.

    @Test
    void enumValueRef_lhsIsAttributeRhsIsSubtype_resolvesAsTypeRestriction() {
        // Fixture: Holder.payload : Base (1..1); Sub extends Base.
        // Rule body `payload -> Sub` should resolve as a type-restriction.
        String source = String.join("\n",
                "namespace test",
                "type Base:",
                "    common string (1..1)",
                "type Sub extends Base:",
                "    extra string (1..1)",
                "type Holder:",
                "    payload Base (1..1)",
                "reporting rule R from Holder:",
                "    payload -> Sub");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream()
                .filter(e -> "payload".equals(e.enumName()) && "Sub".equals(e.valueName()))
                .findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "Gap B test: AST must contain REnumValueRef for `payload -> Sub`");
        var enumRef = enumRefOpt.get();
        assertTrue(enumRef.enumeration().isEmpty(),
                "LHS `payload` must NOT resolve as an enum (it's an attribute name)");
        assertTrue(enumRef.resolvedAttributeChain().isEmpty(),
                "Cat 10's standard 2-step lookup must NOT bind for type-restriction shape: "
                + "step 2 finds `Sub` not an attribute on Base, so Cat 10 bails and Cat 12 fires. "
                + "This negative assertion locks Cat 10 ↔ Cat 12 mutual exclusion.");
        assertTrue(enumRef.resolvedTypeRestriction().isPresent(),
                "Cat 12 (Gap B) must bind a verified TypeRestriction when:\n"
                + "  (a) LHS resolves to an attribute on item-type,\n"
                + "  (b) RHS resolves to a workspace data type, AND\n"
                + "  (c) RHS is a subtype of LHS's element type.\n"
                + "Diagnostics: " + ws.diagnostics());
        var restriction = enumRef.resolvedTypeRestriction().get();
        assertEquals("payload", restriction.lhsAttribute().name(),
                "Gap B: lhsAttribute should be `payload` resolved on Holder");
        assertEquals("Sub", restriction.restrictionType().name(),
                "Gap B: restrictionType should be `Sub`");
    }

    @Test
    void enumValueRef_typeRestrictionInsideThenExtractBody_resolvesViaCat12() {
        // Closer-to-corpus fixture: type-restriction inside a `then extract <body>`
        // chain-op body, where implicit-item is the upstream chain's element type.
        // Mirrors the dominant DRR shape e.g. regulation-common-dtcc-trade-rule.rosetta:
        //   extract economicTerms then extract payout -> OptionPayout
        String source = String.join("\n",
                "namespace test",
                "type Payout:",
                "type OptionPayout extends Payout:",
                "type EconomicTerms:",
                "    payout Payout (1..1)",
                "type Holder:",
                "    economicTerms EconomicTerms (1..1)",
                "reporting rule R from Holder:",
                "    extract economicTerms then extract payout -> OptionPayout");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream()
                .filter(e -> "payout".equals(e.enumName()) && "OptionPayout".equals(e.valueName()))
                .findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "Then-extract Gap B test: AST must contain REnumValueRef for `payout -> OptionPayout`");
        var enumRef = enumRefOpt.get();
        // Diagnostic: split Phase A speculation from Phase B verification.
        assertTrue(enumRef.resolvedRestrictionType().isPresent(),
                "Phase A speculation must bind resolvedRestrictionType when "
                + "valueName resolves to workspace RDataType. "
                + "Diagnostics: " + ws.diagnostics());
        assertTrue(enumRef.resolvedTypeRestriction().isPresent(),
                "Cat 12 must fire inside `then extract <body>` chain-op body. "
                + "Diagnostics: " + ws.diagnostics());
        var restriction = enumRef.resolvedTypeRestriction().get();
        assertEquals("payout", restriction.lhsAttribute().name());
        assertEquals("OptionPayout", restriction.restrictionType().name());
    }

    // === T0p Gap I: choice-typed attribute modelled as populated RChoiceTypeRef =
    // The dominant DRR rule-gating shape `payout -> OptionPayout` (T4 rule-scoped
    // deepest-MISSING, ~16 occurrences) is blocked when `payout` is typed as a
    // `choice` (corpus: `choice Payout: SettlementPayout / OptionPayout / ...`).
    // The inference path previously returned MISSING for choices, so Gap-B Cat 12
    // bailed at the `inferAttributeRefType(payout)` step before the subtype check.
    // Gap I models a choice as a FULLY-POPULATED RChoiceTypeRef (options resolved)
    // so (a) the attribute infers non-MISSING and (b) SubtypeRelation Rule 8
    // (`S <= choice{A,B}`, which iterates options()) can match the restriction.

    @Test
    void choiceTypedAttribute_infersPopulatedChoiceType() {
        // A choice-typed attribute must infer a populated RChoiceTypeRef, not
        // MISSING. (Empty options would defeat SubtypeRelation Rule 8.)
        String source = String.join("\n",
                "namespace test",
                "type A:",
                "    x int (1..1)",
                "type B:",
                "    y int (1..1)",
                "choice C:",
                "    A",
                "    B",
                "type Holder:",
                "    c C (1..1)",
                "reporting rule R from Holder:",
                "    extract c");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference cRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "c".equals(r.name())).findFirst().orElseThrow();
        var t = ws.getInferredType(cRef);
        assertFalse(t.isMissing(),
                "Gap I: choice-typed attribute `c : C` must NOT infer MISSING. "
                + "Diagnostics: " + ws.diagnostics());
        assertTrue(t.type() instanceof com.regnosys.rosetta.types.RChoiceTypeRef,
                "Gap I: choice attribute must infer an RChoiceTypeRef; got "
                + t.type().getClass().getName());
        var choiceRef = (com.regnosys.rosetta.types.RChoiceTypeRef) t.type();
        assertEquals(2, choiceRef.options().size(),
                "Gap I: RChoiceTypeRef options MUST be populated (required by "
                + "SubtypeRelation Rule 8); got " + choiceRef.options().size());
    }

    @Test
    void enumValueRef_lhsIsChoiceTypedAttribute_optionName_resolvesViaChoiceOptionNav() {
        // The rule-gating shape: `payout : Payout` where Payout is a CHOICE and
        // OptionPayout is one of its options. CLOSURE T10's choice-option
        // navigation handles this: OptionPayout matches an option NAME on the
        // (Gap-I populated) Payout RChoiceTypeRef, so the narrowing resolves
        // from the choice's own option list — membership IS the downcast's
        // validity. (This supersedes the earlier Gap-B / Cat-12 path for
        // choice-typed LHS, which required GlobalResolutionPass to also resolve
        // OptionPayout as a workspace global symbol; Gap B still serves
        // non-choice-LHS subtype downcasts via its SubtypeRelation check.)
        String source = String.join("\n",
                "namespace test",
                "type OptionPayout:",
                "    strike int (1..1)",
                "type SettlementPayout:",
                "    amount int (1..1)",
                "choice Payout:",
                "    OptionPayout",
                "    SettlementPayout",
                "type Holder:",
                "    payout Payout (1..1)",
                "reporting rule R from Holder:",
                "    payout -> OptionPayout");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream()
                .filter(e -> "payout".equals(e.enumName()) && "OptionPayout".equals(e.valueName()))
                .findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "AST must contain REnumValueRef for `payout -> OptionPayout`");
        var enumRef = enumRefOpt.get();
        assertTrue(enumRef.resolvedChoiceOption().isPresent(),
                "CLOSURE T10: a choice-typed attribute `payout : Payout` must "
                + "resolve `-> OptionPayout` via choice-option navigation (OptionPayout "
                + "is one of the Payout choice's options). Diagnostics: " + ws.diagnostics());
        assertEquals("OptionPayout",
                ws.getInferredType(enumRef).type().name(),
                "payout -> OptionPayout narrows to the OptionPayout data-type option");
    }

    // === Gap J: choice-option navigation via RFeatureCall =====================
    // When a feature chain yields a (Gap-I populated) choice-typed value and the
    // trailing segment names one of the choice's option types, the grammar emits
    // an RFeatureCall whose featureName is the option type-name. The expression
    // narrows to that option's type. This is the dominant rule-gating shape that
    // surfaced AFTER Gap I resolved the choice receiver (focused D11 fail-fast
    // 59 → 54): EffectiveDate `... payout -> InterestRatePayout`, Series/Version
    // `... payout -> CreditDefaultPayout`, IndexFactor `... underlier -> Product`.
    // (The simple-LHS `attr -> Option` form is an REnumValueRef handled by
    // enumValueRef_lhsIsChoiceTypedAttribute_restrictionToOption_resolvesViaCat12.)

    @Test
    void choiceOptionNavigation_featureCallToOption_narrowsToOptionType() {
        String source = String.join("\n",
                "namespace test",
                "type OptionPayout:",
                "    strike int (1..1)",
                "type SettlementPayout:",
                "    amount int (1..1)",
                "choice Payout:",
                "    OptionPayout",
                "    SettlementPayout",
                "type EconomicTerms:",
                "    payout Payout (1..1)",
                "type Holder:",
                "    economicTerms EconomicTerms (1..1)",
                "reporting rule R from Holder:",
                "    extract economicTerms -> payout -> OptionPayout");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var fcOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.RFeatureCall.class)
                .stream()
                .filter(fc -> "OptionPayout".equals(fc.featureName()))
                .findFirst();
        assertTrue(fcOpt.isPresent(),
                "Gap J test: AST must contain an RFeatureCall for the option-nav "
                + "`payout -> OptionPayout` (deep-chain LHS produces a feature call, "
                + "NOT an REnumValueRef). If this fails, the fixture's parse shape "
                + "changed. Diagnostics: " + ws.diagnostics());
        var fc = fcOpt.get();
        var t = ws.getInferredType(fc);
        assertFalse(t.isMissing(),
                "Gap J: choice-option navigation `payout -> OptionPayout` must NOT "
                + "infer MISSING once the receiver is a populated choice (Gap I). "
                + "Diagnostics: " + ws.diagnostics());
        assertEquals("OptionPayout", t.type().name(),
                "Gap J: option-nav must narrow to the named option's type");
    }

    // === CLOSURE T10 lever 10: option-of-option choice navigation =============
    // The DRR cdeV3 underlier rules navigate `Observable -> Asset` where the
    // implicit item is the `choice Underlier: Observable | Product` (Observable
    // is one of Underlier's options AND is itself `choice Observable: Asset |
    // Basket | Index`), so the single REnumValueRef must narrow TWICE:
    // Underlier -> Observable (enumName, an option that is itself a choice) ->
    // Asset (valueName, an option of the inner choice). The L8 choice-TYPE-NAME
    // block bails (item choice name is "Underlier", not "Observable"); the
    // attribute-LHS choice-option block bails (Observable is an option type, not
    // an attribute on Underlier). The resolved type is the inner option (Asset).
    @Test
    void optionOfOptionNav_itemChoiceOptionIsItselfChoice_narrowsToInnerOption() {
        String source = String.join("\n",
                "namespace test",
                "type Asset:",
                "    a int (1..1)",
                "type Basket:",
                "    b int (1..1)",
                "choice Observable:",
                "    Asset",
                "    Basket",
                "type Product:",
                "    p int (1..1)",
                "choice Underlier:",
                "    Observable",
                "    Product",
                "type Holder:",
                "    u Underlier (1..1)",
                "reporting rule R from Holder:",
                "    extract u then extract Observable -> Asset");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream()
                .filter(e -> "Observable".equals(e.enumName()) && "Asset".equals(e.valueName()))
                .findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "AST must contain REnumValueRef for `Observable -> Asset`");
        var enumRef = enumRefOpt.get();
        assertTrue(enumRef.resolvedChoiceOption().isPresent(),
                "lever 10: option-of-option nav must resolve `Observable -> Asset` "
                + "where item is `choice Underlier` (Observable is an option of "
                + "Underlier and is itself a choice; Asset is an option of "
                + "Observable). Diagnostics: " + ws.diagnostics());
        assertEquals("Asset",
                ws.getInferredType(enumRef).type().name(),
                "option-of-option nav narrows to the inner option (Asset)");
    }

    // === CLOSURE T10 lever 11: closure param referenced inside a NESTED closure =
    // The DRR cdeV3 underlier rules use `extract X [ ... (extract X -> field) ]`
    // where X is the OUTER explicit closure parameter but is referenced inside a
    // NESTED inner closure. D42's nearest-`findAncestor` returns the inner closure
    // (whose paramNames lack X) and `getEnclosingItemType` returns the inner item
    // type (often MISSING), so the leading-name fallback never fires. Lever 11
    // walks the full ancestor chain for the DECLARING closure and resolves the
    // trailing feature on THAT closure's item element type, even when the nearest
    // item type is MISSING.
    @Test
    void closureParamReferencedInNestedClosure_resolvesViaDeclaringClosureItemType() {
        String source = String.join("\n",
                "namespace test",
                "type Leaf:",
                "    v string (1..1)",
                "type Top:",
                "    payload Leaf (1..1)",
                "    other Leaf (1..1)",
                "reporting rule R from Top:",
                "    extract outer [ other then extract (outer -> payload) ]");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream()
                .filter(e -> "outer".equals(e.enumName()) && "payload".equals(e.valueName()))
                .findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "AST must contain REnumValueRef for `outer -> payload`");
        var enumRef = enumRefOpt.get();
        assertTrue(enumRef.resolvedAttributeChain().isPresent(),
                "lever 11: `outer -> payload` referenced inside a nested inner "
                + "closure must resolve via the OUTER declaring closure's item type "
                + "(Top), even though the nearest closure's item type differs. "
                + "Diagnostics: " + ws.diagnostics());
        var chain = enumRef.resolvedAttributeChain().get();
        assertTrue(chain.attributeOpt().isEmpty(),
                "closure-param shape must have null leading attribute (the leading "
                + "name is a local var, not an attribute on the item)");
        assertEquals("payload", chain.feature().name(),
                "trailing feature resolves on the declaring closure's item type (Top)");
    }

    @Test
    void extractWithExplicitParam_leadingNameMatchesParam_resolvesViaClosureBranch() {
        // Fixture pattern: `extract X [X -> field]` where:
        //   - X is the explicit closure param name
        //   - X is NOT an attribute on itemType (rule fromType)
        //   - field IS an attribute on itemType
        // Standard 2-step Cat 10 path fails on step 1 (no `X` attr on itemType);
        // closure-param branch fires and resolves trailing name as feature on
        // itemType (= closure param's type for the no-receiver case).
        String source = String.join("\n",
                "namespace test",
                "type Inner:",
                "    leaf string (1..1)",
                "type Top:",
                "    payload Inner (1..1)",
                "reporting rule R from Top:",
                "    extract param [ param -> payload ]");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "explicit-closure-param test: AST must contain an REnumValueRef "
                + "for the body `param -> payload`");
        var enumRef = enumRefOpt.get();
        assertTrue(enumRef.resolvedAttributeChain().isPresent(),
                "Cat 10 closure-param branch must resolve `param -> payload` "
                + "where leading `param` matches the explicit closure parameter "
                + "(a local var, not an attribute on the chain op's argument "
                + "element type). Diagnostics: " + ws.diagnostics());
        var chain = enumRef.resolvedAttributeChain().get();
        assertTrue(chain.attributeOpt().isEmpty(),
                "closure-param shape must have null attribute (no real RAttribute "
                + "backs the leading source name; it's a local var). Actual attribute: "
                + (chain.attributeOpt().map(a -> a.name()).orElse("EMPTY")));
        assertEquals("payload", chain.feature().name(),
                "closure-param shape: feature should be the trailing name resolved "
                + "on itemType (Top here, since the closure has no receiver and "
                + "itemType falls back to the rule's fromType)");
    }

    // === T0k Gap F: type-alias-typed attribute/output → inference =============
    // The deepest-MISSING walker (T0j) found 26 fail-fast on drr/7.0.0-dev.113
    // bottom out at RSymbolReference→RFunction whose output type is a typeAlias
    // (e.g. NotationStringFromEnum → NumericChar1to4 = string(...)). The type
    // inference path (ExpressionTypeComputer#inferTypeOfAttribute) previously
    // returned MISSING for alias-typed attributes — its javadoc explicitly
    // punted RTypeAlias to a builtin fallback that misses (the alias name is
    // not a builtin key). D35's TypeAliasSolver.evaluateAliasBody was wired on
    // the *generator* path (GeneratorModel.resolveTypeCall) but not on the
    // *inference* path. These two tests lock the inference-side alias resolution.

    @Test
    void inferType_aliasTypedAttribute_resolvesToUnderlyingBuiltin() {
        // `extract field` where field : a string-alias. The body's inferred
        // type must be the alias underlying (string), not MISSING.
        String source = String.join("\n",
                "namespace test",
                "typeAlias AliasStr: <\"1-char string\">",
                "    string(minLength: 1, maxLength: 1)",
                "type Top:",
                "    field AliasStr (1..1)",
                "reporting rule R from Top:",
                "    extract field");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = AstWalker.findAll(model, RRule.class).get(0);
        RExpression body = rule.expression().orElseThrow();
        var inferred = ws.getInferredType(body);
        assertFalse(inferred.isMissing(),
                "Gap F: alias-typed attribute `field : AliasStr` (string alias) "
                + "must infer to its underlying builtin, not MISSING. "
                + "Diagnostics: " + ws.diagnostics());
        assertTrue(inferred.type() instanceof com.regnosys.rosetta.types.RStringType,
                "Gap F: AliasStr underlying is `string`, so inferred type should "
                + "be an RStringType. Actual: " + inferred.type().getClass().getSimpleName());
    }

    @Test
    void inferType_aliasOutputFuncReference_resolvesToUnderlyingBuiltin() {
        // Bare reference to a func whose output is a string-alias. The reference's
        // inferred type = func output type = alias underlying (string). Mirrors
        // the dominant DRR corpus shape (NotationStringFromEnum × 9).
        String source = String.join("\n",
                "namespace test",
                "typeAlias AliasStr: <\"1-char string\">",
                "    string(minLength: 1, maxLength: 1)",
                "type Top:",
                "    raw string (1..1)",
                "func F:",
                "    inputs: arg string (1..1)",
                "    output: out AliasStr (1..1)",
                "    set out: \"x\"",
                "reporting rule R from Top:",
                "    extract F(raw)");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var refOpt = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> r.symbol()
                        .map(s -> s instanceof com.regnosys.rosetta.ast.functions.RFunction)
                        .orElse(false))
                .findFirst();
        assertTrue(refOpt.isPresent(),
                "Gap F func test: AST must contain an RSymbolReference resolving "
                + "to the func F. Diagnostics: " + ws.diagnostics());
        var inferred = ws.getInferredType(refOpt.get());
        assertFalse(inferred.isMissing(),
                "Gap F: reference to a func with alias-typed output must infer to "
                + "the alias underlying, not MISSING. Diagnostics: " + ws.diagnostics());
        assertTrue(inferred.type() instanceof com.regnosys.rosetta.types.RStringType,
                "Gap F: F's output AliasStr underlying is `string`. Actual: "
                + inferred.type().getClass().getSimpleName());
    }

    // Note: the evaluateAliasBody SUCCESS branch (numeric/constraint-preserving
    // aliases reconstructing to a precise RNumberType, e.g. the int↔BigDecimal
    // distinction) is exercised on the generator path by D35's GeneratorModelTest
    // + TypeAliasSolverTest. The two inference-path tests above cover the
    // aliasBaseBuiltin fallback (constrained-string aliases) which is the
    // genuinely new T0k code path; both branches funnel through the same
    // inferTypeOfAttribute RTypeAlias dispatch.

    // === Test — Gap E (T0n): bare enum value in a conditional branch ==========
    // resolves via the expected enum type propagated from a sibling branch.

    @Test
    void conditionalBranch_bareEnumValue_resolvesViaSiblingExpectedType() {
        // One branch is a qualified enum value (`Color -> RED`, resolves to the
        // enum Color); the sibling branch is a BARE enum value (`GREEN`) that the
        // grammar parses as an unresolved RSymbolReference. M3 must resolve the
        // bare symbol as a value of the expected enum type carried by the sibling
        // branch, so the whole conditional types as the enum (unblocking
        // RuleGenerator's output-type back-fill). Mirrors the DRR shape
        // `if .. then DeliveryTypeEnum -> CASH else if .. then PHYS`.
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    flag boolean (1..1)",
                "enum Color:",
                "    RED",
                "    GREEN",
                "reporting rule PickColor from Trade:",
                "    if flag then Color -> RED else GREEN");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference greenRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "GREEN".equals(r.name()))
                .findFirst().orElseThrow();

        assertTrue(greenRef.symbol().isPresent(),
                "bare enum value GREEN must resolve via the conditional's expected "
                + "enum type (Color, from the sibling `Color -> RED` branch)");
        assertTrue(greenRef.symbol().get() instanceof com.regnosys.rosetta.ast.supporting.REnumValue,
                "resolved symbol must be the REnumValue Color.GREEN; got "
                + greenRef.symbol().get().getClass().getName());
        assertEquals("GREEN",
                ((com.regnosys.rosetta.ast.supporting.REnumValue) greenRef.symbol().get()).name(),
                "resolved enum value must be GREEN");

        var cond = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr.class)
                .stream().findFirst().orElseThrow();
        assertFalse(ws.getInferredType(cond).isMissing(),
                "conditional with a resolved enum branch + a bare-enum sibling must "
                + "not be MISSING once Gap E resolves the bare value");
        assertEquals("Color", ws.getInferredType(cond).type().name(),
                "conditional types as the expected enum Color");
    }

    // === Test — Gap E (T0n) comparison operand: bare enum value across `=` ====
    // resolves via the enum type carried by the sibling attribute operand.

    @Test
    void comparisonOperand_bareEnumValue_resolvesViaSiblingAttributeEnumType() {
        // `settlementType = Cash`: the LHS attribute types as the enum
        // SettlementTypeEnum (Cat 9 implicit-item resolution + Phase B typing);
        // the RHS `Cash` parses as a bare unresolved RSymbolReference. Category
        // 14 must bind it to the enum value SettlementTypeEnum.Cash via the
        // sibling operand's enum type. This does NOT change the comparison's
        // type (it stays BOOLEAN) — it is required for byte-identity so the
        // operand renders as `SettlementTypeEnum.CASH` rather than a broken
        // bare-variable form. Mirrors the DRR shape `settlementType = Cash`.
        String source = String.join("\n",
                "namespace test",
                "enum SettlementTypeEnum:",
                "    Cash",
                "    Physical",
                "type Trade:",
                "    settlementType SettlementTypeEnum (1..1)",
                "reporting rule IsCash from Trade:",
                "    settlementType = Cash");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference cashRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "Cash".equals(r.name()))
                .findFirst().orElseThrow();

        assertTrue(cashRef.symbol().isPresent(),
                "bare enum value Cash must resolve via the comparison's sibling "
                + "attribute operand type (SettlementTypeEnum). Diagnostics: "
                + ws.diagnostics());
        assertTrue(cashRef.symbol().get() instanceof com.regnosys.rosetta.ast.supporting.REnumValue,
                "resolved symbol must be the REnumValue SettlementTypeEnum.Cash; got "
                + cashRef.symbol().get().getClass().getName());
        assertEquals("Cash",
                ((com.regnosys.rosetta.ast.supporting.REnumValue) cashRef.symbol().get()).name(),
                "resolved enum value must be Cash");
    }

    // === Grammar precedence (Phase X1 closure) — conditional-first extract =====
    // The dominant DRR rule shape `extract (if COND then X) then extract (... bare
    // implicit-attr ...)` used to mis-parse: the chain `then` was swallowed INTO
    // the conditional (condition = `COND then X`, then-branch = the second
    // extract, else = synthesised empty list), so the second extract read a
    // boolean item type and its bare implicit-attrs (`intent`) went unresolved.
    // The grammar split (then-chain wrapper over a then-free expression rule;
    // conditional operands are now then-free, matching upstream's OrOperation)
    // makes this parse correctly: `(extract (if COND then X)) then extract (...)`.
    // This locks the regression — `intent` must resolve as a BusinessEvent
    // attribute against the first extract's element type.

    @Test
    void thenChainAfterConditionalFirstExtract_resolvesImplicitAttrInSecondExtract() {
        String source = String.join("\n",
                "namespace test",
                "enum IntentEnum:",
                "    Novation",
                "    Decrease",
                "type BusinessEvent:",
                "    intent IntentEnum (0..1)",
                "type WorkflowStep:",
                "    businessEvent BusinessEvent (0..1)",
                "type Instruction:",
                "    flag boolean (0..1)",
                "    originatingWorkflowStep WorkflowStep (0..1)",
                "reporting rule R from Instruction:",
                "    extract",
                "        if flag = True",
                "        then originatingWorkflowStep -> businessEvent",
                "    then extract",
                "        if intent = Novation",
                "        then True",
                "        else False");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        // Top-level expression must be the then-chain (RThenExpr), NOT a single
        // RConditionalExpr that swallowed the chain `then`.
        assertTrue(rule.expression().orElseThrow()
                        instanceof com.regnosys.rosetta.ast.expressions.binary.RThenExpr,
                "Rule body must parse as a `then` chain (the first extract piped "
                + "into the second), not a conditional that swallowed the chain "
                + "`then`. Got: " + rule.expression().orElseThrow().getClass().getSimpleName());

        RSymbolReference intentRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "intent".equals(r.name())).findFirst().orElseThrow();
        assertTrue(intentRef.symbol().isPresent(),
                "`intent` (bare implicit-attr in the second extract) must resolve "
                + "against the first extract's element type (BusinessEvent) via "
                + "Category 9. Diagnostics: " + ws.diagnostics());
        assertTrue(intentRef.symbol().get() instanceof RAttribute,
                "resolved `intent` symbol must be an RAttribute; got "
                + intentRef.symbol().get().getClass().getName());
        assertFalse(ws.getInferredType(intentRef).isMissing(),
                "`intent` must infer a non-MISSING type (IntentEnum).");
        assertEquals("IntentEnum", ws.getInferredType(intentRef).type().name(),
                "`intent` must infer as IntentEnum (BusinessEvent.intent's type)");
    }

    // === CLOSURE T9 — deep dependency chain converges via post-order Phase B ===

    @Test
    void deeplyNestedElseIfChain_convergesWithinIterationBudget() {
        // The fixed point re-computes Phase B in post-order (children before
        // parents), so a deep `if / else if` ladder converges bottom-up in a
        // single pass rather than one nesting-level per iteration. This mirrors
        // the ~15-deep DRR DayCountConvention enum-mapping rule that exceeded
        // the MAX_ITERATIONS budget under the old pre-order pass — the outer
        // conditional was computed against still-MISSING inner branches and
        // never recomputed, so its type stayed MISSING. With `depth` chosen
        // safely greater than MAX_ITERATIONS, a pre-order pass cannot converge
        // the ladder within the budget; post-order does. Every branch yields
        // the same enum, so the whole ladder types as that enum.
        int depth = TypeInferenceEngine.MAX_ITERATIONS + 4; // deeper than the budget
        StringBuilder body = new StringBuilder();
        for (int i = 1; i <= depth; i++) {
            body.append(i == 1 ? "    if " : "    else if ")
                .append("x = ").append(i).append("\n")
                .append("    then Color -> A\n");
        }
        body.append("    else Color -> B");
        String source = String.join("\n",
                "namespace test",
                "enum Color:",
                "    A",
                "    B",
                "type Holder:",
                "    x int (1..1)",
                "reporting rule DeepChain from Holder:",
                body.toString());
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        RExpression chain = rule.expression().orElseThrow();

        assertFalse(ws.getInferredType(chain).isMissing(),
                "A " + depth + "-deep if/else-if ladder (deeper than MAX_ITERATIONS="
                + TypeInferenceEngine.MAX_ITERATIONS + ") must converge to a "
                + "non-MISSING type — post-order Phase B re-computation propagates "
                + "the inner branch types up to the outer conditional within the "
                + "iteration budget. Diagnostics: " + ws.diagnostics());
        assertEquals("Color", ws.getInferredType(chain).type().name(),
                "Every branch yields a Color enum value, so the ladder types as Color");
    }

    // === CLOSURE T10 — `then only-element then extract <field>` ===============
    // Reproduces the DRR UniqueProductIdentifier tail:
    //   extract productId then filter source = UPI then only-element then extract identifier
    // The final `extract identifier` must resolve `identifier` as an attribute
    // on ProductIdentifier (the only-element's output type).
    @Test
    void thenOnlyElementThenExtract_resolvesFieldOnElementType() {
        String source = String.join("\n",
                "namespace test",
                "enum SourceEnum:",
                "    UPI",
                "    Other",
                "type ProductIdentifier:",
                "    identifier string (1..1)",
                "    source SourceEnum (1..1)",
                "type Product:",
                "    productId ProductIdentifier (0..*)",
                "reporting rule R from Product:",
                "    extract productId",
                "    then filter source = SourceEnum -> UPI",
                "    then only-element",
                "    then extract identifier");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        RExpression chain = rule.expression().orElseThrow();

        assertFalse(ws.getInferredType(chain).isMissing(),
                "extract productId then filter .. then only-element then extract "
                + "identifier: the final `extract identifier` must resolve on "
                + "ProductIdentifier (only-element's output type). Diagnostics: "
                + ws.diagnostics());
        assertEquals("string", ws.getInferredType(chain).type().name(),
                "ProductIdentifier.identifier is a string");
    }

    // === CLOSURE T10 — attribute shadows builtin record-type name ============
    // Reproduces the DRR ClearingTimestamp tail:
    //   extract originatingWorkflowStep -> timestamp then filter .. then
    //   only-element then extract dateTime
    // `dateTime` is intended as the EventTimestamp.dateTime ATTRIBUTE, but it
    // also names the builtin `dateTime` record type. The global symbol
    // resolver binds the bare reference to the builtin record type, so Cat 9
    // (which only fires on symbol().isEmpty()) never re-binds it as the
    // implicit-item attribute. In an extract body a bare name is always a
    // feature navigation, so the attribute must shadow the builtin type.
    @Test
    void thenExtract_attributeNameMatchesBuiltinRecordType_resolvesAttribute() {
        String source = String.join("\n",
                "namespace test",
                "type EventTimestamp:",
                "    dateTime zonedDateTime (1..1)",
                "    qualification string (1..1)",
                "type WorkflowStep:",
                "    timestamp EventTimestamp (0..*)",
                "reporting rule R from WorkflowStep:",
                "    extract timestamp",
                "    then only-element",
                "    then extract dateTime");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference dtRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "dateTime".equals(r.name())).findFirst().orElseThrow();
        assertTrue(dtRef.symbol().isPresent()
                        && dtRef.symbol().get() instanceof RAttribute,
                "extract dateTime: bare `dateTime` must re-bind to the "
                + "EventTimestamp.dateTime attribute (an attribute shadows the "
                + "builtin `dateTime` record-type name in an implicit-item body); "
                + "got " + dtRef.symbol().map(s -> s.getClass().getSimpleName()).orElse("EMPTY"));
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        assertFalse(ws.getInferredType(rule.expression().orElseThrow()).isMissing(),
                "the chain output type must resolve (EventTimestamp.dateTime "
                + "= zonedDateTime). Diagnostics: " + ws.diagnostics());
    }

    // === CLOSURE T10 — choice-option navigation ==============================
    // `<choiceAttr> -> <OptionName>` narrows a choice-typed attribute to one of
    // the choice's options. Mirrors the dominant DRR residual shape
    // `payout -> CommodityPayout` (Payout is a choice; CommodityPayout is one
    // of its options). The grammar parses `a -> b` as REnumValueRef. The Gap B
    // (type-restriction) path only fires when GlobalResolutionPass resolved
    // <OptionName> as a workspace GLOBAL symbol in the rule file's scope, which
    // fails when the option type is not imported there. The choice's own option
    // list is always in scope (it IS part of the choice type), so the narrowing
    // resolves locally without a global-symbol bind.

    // The narrowed option is itself a CHOICE → Gap B cannot fire (it requires
    // an RDataType global bind), so this is a genuine RED reproduction of the
    // new path. Mirrors the corpus `underlier -> Product` (Underlier choice,
    // Product option is itself a choice).
    @Test
    void choiceOptionNav_choiceTypedOption_narrowsToOptionChoice() {
        String source = String.join("\n",
                "namespace test",
                "type Asset:",
                "    name string (1..1)",
                "type TransferableProduct:",
                "    id string (1..1)",
                "choice Product:",
                "    TransferableProduct",
                "    Asset",
                "choice Underlier:",
                "    Product",
                "    Asset",
                "type Holder:",
                "    underlier Underlier (0..1)",
                "reporting rule NarrowToProduct from Holder:",
                "    extract underlier -> Product");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        REnumValueRef enr = AstWalker.findAll(model, REnumValueRef.class).stream()
                .filter(r -> "Product".equals(r.valueName())).findFirst().orElseThrow();
        assertTrue(enr.resolvedChoiceOption().isPresent(),
                "underlier -> Product: `Product` must resolve as a choice-option "
                + "navigation on the Underlier choice (Product is one of its "
                + "options); got resolvedChoiceOption EMPTY. Diagnostics: "
                + ws.diagnostics());
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        RExpression chain = rule.expression().orElseThrow();
        assertFalse(ws.getInferredType(chain).isMissing(),
                "the chain output type must resolve to the Product choice. "
                + "Diagnostics: " + ws.diagnostics());
        assertEquals("Product", ws.getInferredType(chain).type().name(),
                "underlier -> Product narrows to the Product choice option");
    }

    // The narrowed option is a DATA TYPE. Asserts the choice-option path binds
    // (resolvedChoiceOption) rather than relying on Gap B, and that the
    // inferred type is the data-type option. Mirrors `payout -> CommodityPayout`.
    @Test
    void choiceOptionNav_dataTypeOption_narrowsToOptionDataType() {
        String source = String.join("\n",
                "namespace test",
                "type CommodityLeg:",
                "    notional number (1..1)",
                "type InterestLeg:",
                "    rate number (1..1)",
                "choice Leg:",
                "    CommodityLeg",
                "    InterestLeg",
                "type Trade:",
                "    leg Leg (0..1)",
                "reporting rule NarrowToCommodity from Trade:",
                "    extract leg -> CommodityLeg");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        REnumValueRef enr = AstWalker.findAll(model, REnumValueRef.class).stream()
                .filter(r -> "CommodityLeg".equals(r.valueName())).findFirst().orElseThrow();
        assertTrue(enr.resolvedChoiceOption().isPresent(),
                "leg -> CommodityLeg: `CommodityLeg` must resolve via the "
                + "choice-option navigation path (Leg choice option), not only "
                + "via Gap B global resolution. Diagnostics: " + ws.diagnostics());
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        assertEquals("CommodityLeg", ws.getInferredType(rule.expression().orElseThrow()).type().name(),
                "leg -> CommodityLeg narrows to the CommodityLeg data-type option");
    }

    // Gap A variant: the LHS resolves to a func/rule whose OUTPUT is a choice,
    // and the trailing name is one of that choice's options. Mirrors the corpus
    // `underlier.UnderlierForProduct(..) -> Product` shape (UnderlierForProduct
    // returns the Underlier choice; Product is an option). The Gap A feature
    // lookup bails (Product is a type, not an attribute on the choice), so the
    // Gap A choice-option fallthrough must narrow to the option.
    @Test
    void choiceOptionNav_funcOutputIsChoice_narrowsToOption() {
        String source = String.join("\n",
                "namespace test",
                "type AssetLeaf:",
                "    name string (1..1)",
                "type CashLeaf:",
                "    code string (1..1)",
                "choice Underlier2:",
                "    AssetLeaf",
                "    CashLeaf",
                "type Inp:",
                "    raw string (1..1)",
                "func GetUnderlier:",
                "    inputs: arg Inp (1..1)",
                "    output: result Underlier2 (1..1)",
                "reporting rule R from Inp:",
                "    GetUnderlier -> AssetLeaf");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        REnumValueRef enr = AstWalker.findAll(model, REnumValueRef.class).stream()
                .filter(r -> "AssetLeaf".equals(r.valueName())).findFirst().orElseThrow();
        assertTrue(enr.resolvedChoiceOption().isPresent(),
                "GetUnderlier -> AssetLeaf: the func output is the Underlier2 choice "
                + "and AssetLeaf is one of its options — must resolve via the Gap A "
                + "choice-option fallthrough. Diagnostics: " + ws.diagnostics());
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        assertEquals("AssetLeaf", ws.getInferredType(rule.expression().orElseThrow()).type().name(),
                "GetUnderlier -> AssetLeaf narrows to the AssetLeaf data-type option");
    }

    // ===== Phase X1 closure T10 — builtin record-field navigation ===========
    // `<receiver typed as a builtin record> -> <field>` where the field is one
    // of the record's declared fields (date/dateTime/zonedDateTime per
    // basictypes.rosetta). The TRAILING `-> field` is an RFeatureCall whose
    // feature is NOT an RAttribute; its receiver is a prior `attr -> attr`
    // navigation (a REnumValueRef typed via the attribute chain). Two `->`
    // levels are required: a single `attr -> field` parses as one REnumValueRef
    // (the FIRST arrow), so the outer record-field nav only appears as an
    // RFeatureCall on a multi-segment receiver. Mirrors the dominant corpus
    // shape EffectiveDate's `PositionForEvent -> openDateTime -> date`.

    @Test
    void recordFieldNav_zonedDateTimeToDate_narrowsToDateRecord() {
        String source = String.join("\n",
                "namespace test",
                "type Inner:",
                "    openDateTime zonedDateTime (1..1)",
                "type Position:",
                "    inner Inner (1..1)",
                "reporting rule OpenDate from Position:",
                "    extract inner -> openDateTime -> date");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        RExpression chain = rule.expression().orElseThrow();
        assertFalse(ws.getInferredType(chain).isMissing(),
                "inner -> openDateTime -> date: trailing `-> date` must resolve as a "
                + "builtin record-field navigation on the zonedDateTime record. "
                + "Diagnostics: " + ws.diagnostics());
        assertEquals("date", ws.getInferredType(chain).type().name(),
                "zonedDateTime -> date narrows to the builtin date record");
    }

    // ===== Phase X1 closure T10 — data-type-extends-choice option navigation ==
    // `<receiver typed as a data type whose super-type chain reaches a choice>
    // -> <OptionName>` where OptionName is one of that choice's options. The
    // TRAILING `-> OptionName` is an RFeatureCall (its feature is NOT an
    // RAttribute); its receiver is a prior multi-segment navigation typed as the
    // data type. Two `->` levels are required (a single `attr -> X` parses as one
    // REnumValueRef). Mirrors the dominant corpus shape
    // `Observable -> Basket -> basketConstituent -> Asset -> ...` where
    // `type BasketConstituent extends Observable` (Observable is a choice; Asset
    // is one of its options). The existing Gap J branch narrows only when the
    // receiver is itself an RChoiceTypeRef; here the receiver is an RDataTypeRef
    // whose choice super-type carries the option.
    @Test
    void dataTypeExtendsChoiceNav_dataTypeReceiver_narrowsToChoiceOption() {
        String source = String.join("\n",
                "namespace test",
                "type Circle:",
                "    radius int (1..1)",
                "type Square:",
                "    side int (1..1)",
                "choice Shape:",
                "    Circle",
                "    Square",
                "type SpecialShape extends Shape:",
                "    note string (0..1)",
                "type Inner:",
                "    special SpecialShape (1..1)",
                "type Holder:",
                "    inner Inner (1..1)",
                "reporting rule R from Holder:",
                "    extract inner -> special -> Circle");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        RExpression chain = rule.expression().orElseThrow();
        assertFalse(ws.getInferredType(chain).isMissing(),
                "inner -> special -> Circle: trailing `-> Circle` must resolve as a "
                + "choice-option navigation on the receiver SpecialShape (a data type "
                + "whose super-type is the Shape choice; Circle is a Shape option). "
                + "Diagnostics: " + ws.diagnostics());
        assertEquals("Circle", ws.getInferredType(chain).type().name(),
                "SpecialShape -> Circle narrows to the Circle data-type option of the "
                + "Shape choice that SpecialShape extends");
    }

    // CLOSURE T10 — rule-output record-field navigation. `<RuleRef> -> <field>`
    // where the LHS resolves to a RULE whose output is a builtin record and
    // the trailing name is a record field. Mirrors DRR EventDate
    // `extract ValuationTimestamp -> date` (ValuationTimestamp rule outputs a
    // zonedDateTime; `date` is a record field). The REnumValueRef binds
    // resolvedSymbol=RRule (GlobalResolutionPass) but no AttributeChain (record
    // fields are not RAttributes, and symbolOutputAttribute is RFunction-only);
    // computeEnumValueRef computes the type from the rule's output record.
    @Test
    void ruleOutputRecordFieldNav_ruleRefToDate_narrowsToDateRecord() {
        String source = String.join("\n",
                "namespace test",
                "type In:",
                "    ts zonedDateTime (1..1)",
                "reporting rule TS from In:",
                "    extract ts",
                "reporting rule R from In:",
                "    extract TS -> date");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream()
                .filter(e -> "TS".equals(e.enumName()) && "date".equals(e.valueName()))
                .findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "AST must contain REnumValueRef for `TS -> date`");
        var t = ws.getInferredType(enumRefOpt.get());
        assertFalse(t.isMissing(),
                "rule-output record-field nav: `TS -> date` where the TS rule outputs "
                + "a zonedDateTime and `date` is a record field must resolve via the "
                + "resolved rule symbol's output record. Diagnostics: " + ws.diagnostics());
        assertEquals("date", t.type().name(),
                "TS -> date narrows to the builtin date record");
    }

    // CLOSURE T10 — rule-output FEATURE navigation. `<RuleRef> -> <feature>`
    // where the LHS resolves to a RULE whose output is a DATA type and the
    // trailing name is a real RAttribute feature on it. Mirrors DRR
    // `UnderlierProductIdentifierOther -> identifier`. The Gap A branch in
    // TypeInferenceEngine now derives the rule's output type from its expression
    // (symbolOutputAttribute is RFunction-only) and binds the feature chain.
    @Test
    void ruleOutputFeatureNav_ruleRefToFeature_bindsChain() {
        String source = String.join("\n",
                "namespace test",
                "type Ident:",
                "    identifier string (1..1)",
                "    identifierType string (1..1)",
                "type In:",
                "    id Ident (1..1)",
                "reporting rule MakeId from In:",
                "    extract id",
                "reporting rule R from In:",
                "    extract MakeId -> identifier");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enumRefOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream()
                .filter(e -> "MakeId".equals(e.enumName()) && "identifier".equals(e.valueName()))
                .findFirst();
        assertTrue(enumRefOpt.isPresent(),
                "AST must contain REnumValueRef for `MakeId -> identifier`");
        var enr = enumRefOpt.get();
        assertTrue(enr.resolvedAttributeChain().isPresent(),
                "rule-output feature nav: `MakeId -> identifier` (MakeId rule outputs "
                + "the Ident data type; identifier is an RAttribute feature) must bind "
                + "an AttributeChain via the Gap A rule-output path. Diagnostics: "
                + ws.diagnostics());
        assertEquals("identifier", enr.resolvedAttributeChain().get().feature().name(),
                "feature resolves to identifier on the rule's output type");
    }

    @Test
    void recordFieldNav_dateTimeToTime_narrowsToTime() {
        String source = String.join("\n",
                "namespace test",
                "type Inner:",
                "    stamp dateTime (1..1)",
                "type Event:",
                "    inner Inner (1..1)",
                "reporting rule StampTime from Event:",
                "    extract inner -> stamp -> time");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        assertEquals("time", ws.getInferredType(rule.expression().orElseThrow()).type().name(),
                "dateTime -> time narrows to the builtin time type. Diagnostics: " + ws.diagnostics());
    }

    @Test
    void recordFieldNav_dateToYear_narrowsToInt() {
        String source = String.join("\n",
                "namespace test",
                "type Inner:",
                "    born date (1..1)",
                "type Birth:",
                "    inner Inner (1..1)",
                "reporting rule BornYear from Birth:",
                "    extract inner -> born -> year");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        assertEquals("int", ws.getInferredType(rule.expression().orElseThrow()).type().name(),
                "date -> year narrows to int. Diagnostics: " + ws.diagnostics());
    }

    @Test
    void recordFieldNav_zonedDateTimeToTimezone_narrowsToString() {
        String source = String.join("\n",
                "namespace test",
                "type Inner:",
                "    openDateTime zonedDateTime (1..1)",
                "type Position:",
                "    inner Inner (1..1)",
                "reporting rule OpenZone from Position:",
                "    extract inner -> openDateTime -> timezone");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        assertEquals("string", ws.getInferredType(rule.expression().orElseThrow()).type().name(),
                "zonedDateTime -> timezone narrows to string. Diagnostics: " + ws.diagnostics());
    }

    // Negative guard: `day` is a field of the `date` record but NOT of
    // `zonedDateTime`. Navigating a non-field name on a record must stay
    // MISSING (the receiver's own feature list gates the lookup) so we never
    // fabricate a type for a navigation the receiver does not support.
    @Test
    void recordFieldNav_nonFieldOnRecord_staysMissing() {
        String source = String.join("\n",
                "namespace test",
                "type Inner:",
                "    openDateTime zonedDateTime (1..1)",
                "type Position:",
                "    inner Inner (1..1)",
                "reporting rule OpenDay from Position:",
                "    extract inner -> openDateTime -> day");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        assertTrue(ws.getInferredType(rule.expression().orElseThrow()).isMissing(),
                "zonedDateTime -> day must stay MISSING — `day` is a field of the "
                + "`date` record, not of `zonedDateTime`. Diagnostics: " + ws.diagnostics());
    }

    // === Test — CLOSURE T10 lever 7 (bare-enum wrapper-barrier + inheritance) ==
    // The expected enum type must propagate through an `extract` / `then` map
    // wrapper into a nested conditional whose branches are all bare, and the
    // bare-value match must consider inherited values of an `extends` super-enum.

    @Test
    void bareEnum_inExtractWrappedNestedConditional_resolvesViaOuterSiblingEnum() {
        // Mirrors the DRR `Confirmed` rule shape:
        //   if .. then ConfirmationEnum -> NCNF
        //   else extract (if .. then ECNF else if .. then YCNF)
        // The outer then-branch (`Color -> RED`) establishes the enum Color; the
        // bare GREEN/BLUE live inside an `extract`-wrapped nested conditional whose
        // own branches are all bare (so the inner conditional cannot self-seed the
        // expected enum). The expected type must flow from the OUTER conditional
        // THROUGH the extract wrapper into the inner conditional's bare branches.
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    flag boolean (1..1)",
                "    flag2 boolean (1..1)",
                "enum Color:",
                "    RED",
                "    GREEN",
                "    BLUE",
                "reporting rule PickColor from Trade:",
                "    if flag then Color -> RED",
                "    else extract",
                "        if flag2 then GREEN",
                "        else BLUE");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        for (String name : new String[]{"GREEN", "BLUE"}) {
            RSymbolReference ref = AstWalker.findAll(model, RSymbolReference.class).stream()
                    .filter(r -> name.equals(r.name()))
                    .findFirst().orElseThrow();
            assertTrue(ref.symbol().isPresent(),
                    "bare enum value " + name + " inside an extract-wrapped nested "
                    + "conditional must resolve via the outer conditional's expected "
                    + "enum Color. Diagnostics: " + ws.diagnostics());
            assertTrue(ref.symbol().get() instanceof com.regnosys.rosetta.ast.supporting.REnumValue,
                    name + " must bind to an REnumValue; got "
                    + ref.symbol().get().getClass().getName());
        }
    }

    @Test
    void bareEnum_inheritedValueFromSuperEnum_resolvesViaSiblingExpectedType() {
        // Mirrors the DRR `PartyIdentifierFormat2Enum extends LeiIdentifierFormatEnum`
        // shape: the conditional's qualified then-branch establishes the CHILD enum,
        // but the bare sibling value (`Lei`) is declared on the PARENT enum. The
        // bare-value match must walk the `extends` super-enum chain.
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    flag boolean (1..1)",
                "enum ParentColor:",
                "    Lei",
                "    LeiAndPerson",
                "enum ChildColor extends ParentColor:",
                "    NaturalPerson",
                "    SwiftBic",
                "reporting rule PickColor from Trade:",
                "    if flag then ChildColor -> NaturalPerson else Lei");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        RSymbolReference leiRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "Lei".equals(r.name()))
                .findFirst().orElseThrow();
        assertTrue(leiRef.symbol().isPresent(),
                "bare enum value Lei (declared on the super-enum ParentColor) must "
                + "resolve via the child enum ChildColor's inheritance chain. "
                + "Diagnostics: " + ws.diagnostics());
        assertTrue(leiRef.symbol().get() instanceof com.regnosys.rosetta.ast.supporting.REnumValue,
                "Lei must bind to an REnumValue; got "
                + leiRef.symbol().get().getClass().getName());
        assertEquals("Lei",
                ((com.regnosys.rosetta.ast.supporting.REnumValue) leiRef.symbol().get()).name(),
                "resolved inherited enum value must be Lei");
    }

    // === Probe — CLOSURE T10 lever 8 (choice-TYPE-NAME-LHS option nav) =========
    // The DRR cdeV3 underlier rules navigate `Observable -> Asset` where the
    // implicit item IS the choice `Observable` (choice Observable: Asset|Basket
    // |Index) and the LHS is the choice TYPE NAME (capitalised), not a lowercase
    // attribute (so lever-4's simple-attr Cat-12 path does not match). This probe
    // isolates the DIRECT nav: a rule whose input type is a choice, narrowing to
    // an option via the choice type name. If RED, the direct nav needs a fix; if
    // GREEN, the corpus `Observable -> Asset` failure is purely an upstream
    // cascade (the receiver's item type is MISSING) and the fix is elsewhere.
    @Test
    void choiceTypeNameLhs_itemIsChoice_narrowsToOption() {
        String source = String.join("\n",
                "namespace test",
                "type Asset:",
                "    id int (1..1)",
                "type Reference:",
                "    ref int (1..1)",
                "choice Observable:",
                "    Asset",
                "    Reference",
                "reporting rule NarrowToAsset from Observable:",
                "    Observable -> Asset");
        RWorkspace ws = parseAndLink(source);

        RModel model = ws.files().get(0);
        var enrOpt = AstWalker.findAll(model,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream()
                .filter(e -> "Observable".equals(e.enumName()) && "Asset".equals(e.valueName()))
                .findFirst();
        assertTrue(enrOpt.isPresent(),
                "AST must contain REnumValueRef for `Observable -> Asset`");
        var enr = enrOpt.get();
        assertTrue(enr.resolvedChoiceOption().isPresent(),
                "choice-type-name LHS `Observable -> Asset` on a choice-typed item "
                + "must resolve via choice-option navigation. Diagnostics: " + ws.diagnostics());
        assertEquals("Asset", ws.getInferredType(enr).type().name(),
                "Observable -> Asset narrows to the Asset option type");
    }

    // === Probe — CLOSURE T10 lever 9 (func-call output-type inference) =========
    // A bare RFunction reference in a rule body must type as the func's OUTPUT
    // type. The DRR corpus shows `GetUnderlyingIdentificationType` (output:
    // result trade.underlier.UnderlyingIdentificationTypeEnum) resolving to its
    // RFunction but typing MISSING. These two probes isolate whether the gap is
    // the QUALIFIED (`namespace.Type`) output-type form or the bare-func
    // output-typing path itself: A uses an unqualified enum output, B the
    // qualified self-reference `test.Color`.

    @Test
    void funcRef_unqualifiedEnumOutput_typesAsEnum() {
        String source = String.join("\n",
                "namespace test",
                "enum Color:",
                "    RED",
                "    GREEN",
                "type In:",
                "    x int (1..1)",
                "func F:",
                "    inputs:",
                "        i In (1..1)",
                "    output:",
                "        result Color (0..1)",
                "    set result:",
                "        Color -> RED",
                "reporting rule R from In:",
                "    F");
        RWorkspace ws = parseAndLink(source);
        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        assertEquals("Color", ws.getInferredType(rule.expression().orElseThrow()).type().name(),
                "bare func ref F (output Color) must type as Color. Diagnostics: "
                + ws.diagnostics());
    }

    @Test
    void funcRef_qualifiedEnumOutput_typesAsEnum() {
        String source = String.join("\n",
                "namespace test",
                "enum Color:",
                "    RED",
                "    GREEN",
                "type In:",
                "    x int (1..1)",
                "func F:",
                "    inputs:",
                "        i In (1..1)",
                "    output:",
                "        result test.Color (0..1)",
                "    set result:",
                "        Color -> RED",
                "reporting rule R from In:",
                "    F");
        RWorkspace ws = parseAndLink(source);
        RModel model = ws.files().get(0);
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule).findFirst().orElseThrow();
        assertEquals("Color", ws.getInferredType(rule.expression().orElseThrow()).type().name(),
                "bare func ref F (output test.Color, QUALIFIED) must type as Color. "
                + "Diagnostics: " + ws.diagnostics());
    }

    @Test
    void funcRef_crossFileQualifiedEnumOutput_typesAsEnum() {
        // Faithful corpus repro: the enum lives in a DIFFERENT file/namespace
        // (trade.underlier) than the func (test, importing it), and the func's
        // output type is the cross-namespace qualified name. Mirrors DRR
        // `GetUnderlyingIdentificationType output: result
        // trade.underlier.UnderlyingIdentificationTypeEnum`.
        String enumSrc = String.join("\n",
                "namespace trade.underlier",
                "enum Color:",
                "    RED",
                "    GREEN");
        String funcSrc = String.join("\n",
                "namespace test",
                "import trade.underlier.*",
                "type In:",
                "    x int (1..1)",
                "func F:",
                "    inputs:",
                "        i In (1..1)",
                "    output:",
                "        result trade.underlier.Color (0..1)",
                "    set result:",
                "        empty",
                "reporting rule R from In:",
                "    F");
        RModel enumModel = AstBuilder.buildFromString(enumSrc, "enums.rosetta");
        RModel funcModel = AstBuilder.buildFromString(funcSrc, "funcs.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(enumModel, funcModel)).workspace();
        RRule rule = ws.files().stream()
                .flatMap(m -> m.rootElements().stream())
                .filter(e -> e instanceof RRule)
                .map(e -> (RRule) e)
                .findFirst().orElseThrow();
        assertEquals("Color", ws.getInferredType(rule.expression().orElseThrow()).type().name(),
                "bare func ref F (output trade.underlier.Color, CROSS-FILE QUALIFIED) must "
                + "type as Color. Diagnostics: " + ws.diagnostics());
    }

    @Test
    void funcRef_currentNamespaceRelativeEnumOutput_typesAsEnum() {
        // The actual corpus shape: the func's output type is written RELATIVE to
        // the func file's own namespace. Enum in `a.b.c`, func in namespace `a`,
        // output `b.c.Color` (= currentNs `a` + relative `b.c.Color`). Mirrors DRR
        // `GetUnderlyingIdentificationType` (namespace drr.regulation.common,
        // output trade.underlier.UnderlyingIdentificationTypeEnum where the enum is
        // in drr.regulation.common.trade.underlier). resolveQualifiedOrLocal must
        // try the current file namespace as an implicit prefix for a partial
        // qualified type-call.
        String enumSrc = String.join("\n",
                "namespace a.b.c",
                "enum Color:",
                "    RED",
                "    GREEN");
        String funcSrc = String.join("\n",
                "namespace a",
                "import a.b.c.*",
                "type In:",
                "    x int (1..1)",
                "func F:",
                "    inputs:",
                "        i In (1..1)",
                "    output:",
                "        result b.c.Color (0..1)",
                "    set result:",
                "        empty",
                "reporting rule R from In:",
                "    F");
        RModel enumModel = AstBuilder.buildFromString(enumSrc, "enums.rosetta");
        RModel funcModel = AstBuilder.buildFromString(funcSrc, "funcs.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(enumModel, funcModel)).workspace();
        RRule rule = ws.files().stream()
                .flatMap(m -> m.rootElements().stream())
                .filter(e -> e instanceof RRule)
                .map(e -> (RRule) e)
                .findFirst().orElseThrow();
        assertEquals("Color", ws.getInferredType(rule.expression().orElseThrow()).type().name(),
                "bare func ref F (output b.c.Color, CURRENT-NAMESPACE-RELATIVE from namespace a) "
                + "must type as Color. Diagnostics: " + ws.diagnostics());
    }
}
