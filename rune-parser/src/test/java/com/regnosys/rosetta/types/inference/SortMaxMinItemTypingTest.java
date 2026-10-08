package com.regnosys.rosetta.types.inference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * Anchor for the IR-Lab PART A request (2026-06-25): extend
 * {@code TypeInferenceEngine.computeArgumentElementType} so the lambda-item
 * element type resolves for {@code sort} / {@code max} / {@code min} bodies
 * instead of dropping to {@link com.regnosys.rosetta.types.RMetaAnnotatedType#MISSING}.
 *
 * <p>{@code RSortExpr} / {@code RMaxExpr} / {@code RMinExpr} each carry an
 * {@code argument()} (the source list) exactly like {@code RExtractExpr} /
 * {@code RFilterExpr}, so the item's element type is the argument's element
 * type. Before the fix the op-switch matched only extract/filter/then/reduce,
 * so the implicit {@code item} inside a sort/max/min body typed MISSING and a
 * bare attribute reference there failed to resolve element-wise.
 *
 * <p>Scope boundary (the Lab's honest caveat): PART A types the DIRECT
 * {@code <typed-list> sort/max/min [...]} case. The {@code xs then sort [...]}
 * form, where the sort's {@code argument()} is the elided then-pipe whose own
 * element type is still MISSING, is the PART B then-pipe gap and is NOT covered
 * here.
 *
 * <p>Byte-neutrality: this only exposes a resolved type to {@code getInferredType}
 * (the path the IR adapter calls); Path-1 emission resolves these types at emit
 * time via the gm-aware navigation walk, so the full gensuite + D11 prove ZERO
 * golden drift. Mirrors the {@code TypeDirectedResolverImplicitItemTest} pattern.
 */
class SortMaxMinItemTypingTest {

    /** Parse + link a source string; returns the built workspace. */
    private RWorkspace parseAndLink(String source) {
        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        RLinkingResult result = RWorkspace.build(List.of(model));
        return result.workspace();
    }

    /** Source with a Trade type carrying a string + a numeric attribute and a
     *  collection-op over a List&lt;Trade&gt; using the literal {@code item}
     *  keyword in the key body. {@code op} is "sort" / "max" / "min";
     *  {@code key} is the bare attribute navigated from {@code item}. */
    private String collectionOpSource(String op, String key) {
        return String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "    amount int (1..1)",
                "func F:",
                "    inputs:",
                "        trades Trade (0..*)",
                "    output:",
                "        result Trade (0..*)",
                "    set result:",
                "        trades " + op + " [ item -> " + key + " ]");
    }

    /** The single literal (non-synthetic) {@code item} implicit variable. */
    private RImplicitVariable literalItem(RWorkspace ws) {
        RModel model = ws.files().get(0);
        return AstWalker.findAll(model, RImplicitVariable.class).stream()
                .filter(iv -> !iv.isSynthetic())
                .findFirst().orElseThrow(() -> new AssertionError(
                        "expected a literal 'item' RImplicitVariable in the key body"));
    }

    // === sort — implicit item types as the list element ======================

    @Test
    void sortBody_implicitItem_typesAsElement() {
        RWorkspace ws = parseAndLink(collectionOpSource("sort", "id"));
        RImplicitVariable item = literalItem(ws);
        assertFalse(ws.getInferredType(item).isMissing(),
                "sort body implicit 'item' must NOT type MISSING — the sort's "
                + "argument (List<Trade>) element type is Trade. "
                + "Diagnostics: " + ws.diagnostics());
        assertEquals("Trade", ws.getInferredType(item).type().name(),
                "sort body implicit 'item' must type as the element type Trade");
    }

    // === max — implicit item types as the list element =======================

    @Test
    void maxBody_implicitItem_typesAsElement() {
        RWorkspace ws = parseAndLink(collectionOpSource("max", "amount"));
        RImplicitVariable item = literalItem(ws);
        assertEquals("Trade", ws.getInferredType(item).type().name(),
                "max body implicit 'item' must type as the element type Trade. "
                + "Diagnostics: " + ws.diagnostics());
    }

    // === min — implicit item types as the list element =======================

    @Test
    void minBody_implicitItem_typesAsElement() {
        RWorkspace ws = parseAndLink(collectionOpSource("min", "amount"));
        RImplicitVariable item = literalItem(ws);
        assertEquals("Trade", ws.getInferredType(item).type().name(),
                "min body implicit 'item' must type as the element type Trade. "
                + "Diagnostics: " + ws.diagnostics());
    }

    // === sort — bare attribute resolves element-wise (Category 9 downstream) ==

    @Test
    void sortBody_bareAttr_resolvesElementWise() {
        // No literal `item`: `sort [ id ]` is the implicit-item bare-attribute
        // form. `id` resolves only when the item type (Trade) is known — which
        // requires PART A. Mirrors TypeDirectedResolverImplicitItemTest #2 for
        // extract.
        String source = String.join("\n",
                "namespace test",
                "type Trade:",
                "    id string (1..1)",
                "func F:",
                "    inputs:",
                "        trades Trade (0..*)",
                "    output:",
                "        result Trade (0..*)",
                "    set result:",
                "        trades sort [ id ]");
        RWorkspace ws = parseAndLink(source);
        RModel model = ws.files().get(0);
        RSymbolReference idRef = AstWalker.findAll(model, RSymbolReference.class).stream()
                .filter(r -> "id".equals(r.name()))
                .findFirst().orElseThrow();
        assertTrue(idRef.symbol().isPresent(),
                "sort [ id ]: bare 'id' must resolve element-wise to Trade.id "
                + "via Category 9 once the sort item type is known. "
                + "Diagnostics: " + ws.diagnostics());
        assertTrue(idRef.symbol().get() instanceof RAttribute,
                "resolved symbol must be an RAttribute (Trade.id); got "
                + idRef.symbol().get().getClass().getName());
        assertEquals("id", ((RAttribute) idRef.symbol().get()).name(),
                "resolved attribute must be the 'id' attribute on Trade");
    }

    // === the `item -> key` feature resolves (Category 1, item-type-gated) =====

    @Test
    void sortBody_itemFeatureCall_resolvesFeature() {
        RWorkspace ws = parseAndLink(collectionOpSource("sort", "id"));
        RModel model = ws.files().get(0);
        RFeatureCall idCall = AstWalker.findAll(model, RFeatureCall.class).stream()
                .filter(fc -> "id".equals(fc.featureName()))
                .findFirst().orElseThrow();
        assertTrue(idCall.receiver() instanceof RImplicitVariable,
                "the `item -> id` receiver must be the implicit 'item'");
        // The feature `id` resolves against item's type (Trade); this is only
        // possible once the item types non-MISSING (PART A). Asserting the
        // resolved feature (not the call's result type) keeps the test robust
        // against the incidental builtin-type TYPE_NOT_FOUND noise in a
        // single-file minimal source — the 'id' attribute exists on Trade
        // regardless of whether its own declared type ('string') resolves.
        assertTrue(idCall.resolvedFeature().isPresent(),
                "the `item -> id` feature 'id' must resolve to Trade.id once "
                + "the sort item types as Trade. Diagnostics: " + ws.diagnostics());
        assertEquals("id", idCall.resolvedFeature().get().name(),
                "the resolved feature must be Trade.id");
    }
}
