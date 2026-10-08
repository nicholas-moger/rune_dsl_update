package com.regnosys.rosetta.types.inference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

/**
 * Anchor for the IR-Lab head&rarr;feature cascade fix (2026-06-27): type a
 * disguised {@code <input> -> <feature>} navigation at the top level of a
 * FUNCTION body. The grammar parses {@code before -> trade} as
 * {@link REnumValueRef}({@code enumName=before}, {@code valueName=trade}); when
 * {@code before} is the enclosing function's input (by param name OR the
 * implicit lowercase-input-type-name convention), the nav reads {@code trade} on
 * that input's type.
 *
 * <p><b>The gap.</b> {@code TypeInferenceEngine.getEnclosingItemType} /
 * {@code computeArgumentElementType} fall back to {@code RRule.fromType()} but
 * had no {@code RFunction}-input analogue, so the SAME nav resolves inside a RULE
 * body (the rule from-type implicit scope) but never inside a FUNCTION body — the
 * dominant {@code MISSING} cascade the IR-Lab census measured (~92% of ~6,600
 * MISSING bases bottom out here, ~77% with {@code head = the function input}).
 *
 * <p><b>Field separation (not byte-neutrality).</b> The resolved type is exposed via
 * a dedicated {@link REnumValueRef#resolvedInputFeature()} field that ONLY
 * {@code ExpressionTypeComputer.computeEnumValueRef} reads; the generator does not bind
 * it as a nav (it keeps its own gm-aware nav walk). This is a separation-of-concerns
 * boundary, NOT a byte-neutrality claim — the generator DOES consult
 * {@code getInferredType}, so the better type is byte-POSITIVE (PR #279: +6 FUNCTION
 * flips, 0 within-waiver regressions). Mirrors the {@link SortMaxMinItemTypingTest}
 * (PART A) field-separation pattern (PART A was genuinely byte-neutral because the
 * generator types those items at emit time; this fix is not).
 */
class FunctionInputNavTypingTest {

    private RWorkspace parseAndLink(String source) {
        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        RLinkingResult result = RWorkspace.build(List.of(model));
        return result.workspace();
    }

    /** The disguised {@code enumName -> valueName} nav matching the given names. */
    private REnumValueRef disguisedNav(RWorkspace ws, String head, String feature) {
        RModel model = ws.files().get(0);
        return AstWalker.findAll(model, REnumValueRef.class).stream()
                .filter(e -> head.equals(e.enumName()) && feature.equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "expected a disguised REnumValueRef " + head + " -> " + feature));
    }

    // === RED→GREEN flip locks =================================================

    /** head = explicit input PARAM name (`before -> trade`, before : TradeState). */
    @Test
    void inputParamNav_typesAsFeature() {
        String src = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "type TradeState:",
                "    trade Trade (1..1)",
                "func F:",
                "    inputs:",
                "        before TradeState (1..1)",
                "    output:",
                "        result Trade (1..1)",
                "    set result:",
                "        before -> trade");
        RWorkspace ws = parseAndLink(src);
        REnumValueRef nav = disguisedNav(ws, "before", "trade");
        RMetaAnnotatedType t = ws.getInferredType(nav);
        assertFalse(t.isMissing(),
                "before -> trade (before : TradeState) must type as Trade, not MISSING");
        assertEquals("Trade", t.type().name(), "feature type must be Trade");
    }

    /** head = the implicit lowercase-input-type reference, param named differently. */
    @Test
    void lowercaseInputNav_typesAsFeature() {
        String src = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "type TradeState:",
                "    trade Trade (1..1)",
                "func F:",
                "    inputs:",
                "        in0 TradeState (1..1)",
                "    output:",
                "        result Trade (1..1)",
                "    set result:",
                "        tradeState -> trade");
        RWorkspace ws = parseAndLink(src);
        REnumValueRef nav = disguisedNav(ws, "tradeState", "trade");
        RMetaAnnotatedType t = ws.getInferredType(nav);
        assertFalse(t.isMissing(),
                "tradeState -> trade (implicit lowercase reference to input : TradeState) must type");
        assertEquals("Trade", t.type().name(), "feature type must be Trade");
    }

    /** A longer chain rooted at the input base unblocks downstream feature calls. */
    @Test
    void inputNavChain_cascadeResolves() {
        String src = String.join("\n",
                "namespace test",
                "type Product:",
                "    id string (1..1)",
                "type Trade:",
                "    product Product (1..1)",
                "type TradeState:",
                "    trade Trade (1..1)",
                "func F:",
                "    inputs:",
                "        before TradeState (1..1)",
                "    output:",
                "        result Product (1..1)",
                "    set result:",
                "        before -> trade -> product");
        RWorkspace ws = parseAndLink(src);
        REnumValueRef base = disguisedNav(ws, "before", "trade");
        assertFalse(ws.getInferredType(base).isMissing(), "before -> trade base must type");
        // The whole chain `before -> trade -> product` (an RFeatureCall on the base)
        // must now also resolve to Product.
        var fc = AstWalker.findAll(ws.files().get(0),
                com.regnosys.rosetta.ast.expressions.references.RFeatureCall.class).stream()
                .filter(c -> "product".equals(c.featureName()))
                .findFirst().orElseThrow();
        RMetaAnnotatedType chainType = ws.getInferredType(fc);
        assertFalse(chainType.isMissing(), "the full input-nav chain must cascade-resolve");
        assertEquals("Product", chainType.type().name());
    }

    // === green-safety decline locks (soundness) ==============================

    /** A genuine enum value ref must STILL type as the enum (not mis-bound to an input). */
    @Test
    void genuineEnumValueRef_stillTypesAsEnum() {
        String src = String.join("\n",
                "namespace test",
                "enum Color: RED GREEN",
                "func F:",
                "    inputs:",
                "        before Color (1..1)",
                "    output:",
                "        result Color (1..1)",
                "    set result:",
                "        Color -> RED");
        RWorkspace ws = parseAndLink(src);
        REnumValueRef nav = disguisedNav(ws, "Color", "RED");
        RMetaAnnotatedType t = ws.getInferredType(nav);
        assertFalse(t.isMissing(), "Color -> RED must type as the enum");
        assertEquals("Color", t.type().name());
    }

    /** head that is NOT an enclosing input must NOT be falsely resolved. */
    @Test
    void nonInputHead_staysMissing() {
        String src = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "type TradeState:",
                "    trade Trade (1..1)",
                "func F:",
                "    inputs:",
                "        before TradeState (1..1)",
                "    output:",
                "        result Trade (1..1)",
                "    set result:",
                "        somethingElse -> trade");
        RWorkspace ws = parseAndLink(src);
        REnumValueRef nav = disguisedNav(ws, "somethingElse", "trade");
        assertTrue(ws.getInferredType(nav).isMissing(),
                "somethingElse -> trade (not an input) must stay MISSING — no false resolution");
    }

    /** head IS an input but valueName is NOT a feature on its type → stays MISSING. */
    @Test
    void inputHeadButUnknownFeature_staysMissing() {
        String src = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "type TradeState:",
                "    trade Trade (1..1)",
                "func F:",
                "    inputs:",
                "        before TradeState (1..1)",
                "    output:",
                "        result Trade (1..1)",
                "    set result:",
                "        before -> notAFeature");
        RWorkspace ws = parseAndLink(src);
        REnumValueRef nav = disguisedNav(ws, "before", "notAFeature");
        assertTrue(ws.getInferredType(nav).isMissing(),
                "before -> notAFeature (no such feature on TradeState) must stay MISSING");
    }
}
