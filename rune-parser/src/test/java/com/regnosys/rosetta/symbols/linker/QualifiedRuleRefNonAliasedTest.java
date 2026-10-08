package com.regnosys.rosetta.symbols.linker;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.symbolid.SymbolIdTestFixtures;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RWorkspace;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase X1 PR #76 — verifies M3 resolves cross-namespace qualified rule
 * references through a NON-ALIASED wildcard import. Sister to
 * {@link QualifiedRuleRefAliasedTest} which covers the aliased shape.
 *
 * <p>Locks the {@code GlobalResolutionPass#resolveQualifiedOrLocal} non-aliased
 * branch — paste-quoted from the source:
 *
 * <pre>
 * // Non-aliased wildcard: prepend unconditionally. If the caller
 * // already passed a fully-qualified name that happens to start
 * // with the imported namespace, the doubled-prefix form will
 * // miss every namespace in the workspace — a harmless miss.
 * dealiased = imp.importedNamespace() + "." + name;
 * </pre>
 *
 * <p>Without this regression test, the non-aliased branch is exercised only
 * indirectly through corpus tests; a future refactor that drops the branch
 * (e.g. consolidating to aliased-only) would slip through unit tests and
 * surface only at the corpus regression gate, which is far slower to
 * diagnose. This test pins the contract directly (Copilot PR #76 R19 F2).
 *
 * <p>Synthetic minimal repro: fixture under
 * {@code rune-parser/src/test/resources/symbolid-fixtures/qualified-rule-ref-non-aliased/}
 * declares a producer rule {@code TradeId} in {@code com.test.outer.inner}
 * and a consumer rule {@code DelegatingRule} that references it via
 * {@code inner.TradeId} where the consumer file imports
 * {@code com.test.outer.*} (non-aliased wildcard).
 */
class QualifiedRuleRefNonAliasedTest {

    @Test
    void nonAliasedCrossNamespaceRuleRef_resolves() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("qualified-rule-ref-non-aliased/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        // Find the consumer's DelegatingRule.
        RRule delegatingRule = ws.namespace("com.test.consumer")
                .flatMap(n -> n.lookup("DelegatingRule"))
                .filter(RRule.class::isInstance)
                .map(RRule.class::cast)
                .orElseThrow(() -> new AssertionError(
                        "Consumer's DelegatingRule not found in workspace"));

        // The producer's TradeId rule.
        RRule producerTradeId = ws.namespace("com.test.outer.inner")
                .flatMap(n -> n.lookup("TradeId"))
                .filter(RRule.class::isInstance)
                .map(RRule.class::cast)
                .orElseThrow(() -> new AssertionError(
                        "Producer's TradeId not found in workspace"));

        // Find the RSymbolReference inside DelegatingRule's body — should be
        // the bare `inner.TradeId` reference (non-aliased relative suffix).
        RExpression body = delegatingRule.expression()
                .orElseThrow(() -> new AssertionError("DelegatingRule has no expression body"));
        List<RSymbolReference> refs = AstWalker.findAll(body, RSymbolReference.class);
        assertEquals(1, refs.size(), "Expected exactly one RSymbolReference in DelegatingRule body");

        RSymbolReference ref = refs.get(0);
        assertEquals("inner.TradeId", ref.name(),
                "RSymbolReference.name() should be the non-aliased relative qualified name");

        // ASSERTION: ref.symbol() resolves to the producer's TradeId RRule via
        // the non-aliased wildcard branch in GlobalResolutionPass#resolveQualifiedOrLocal:
        // it prepends the imported namespace (`com.test.outer`) to `inner.TradeId`
        // → `com.test.outer.inner.TradeId`, then progressively splits on dots
        // until a namespace match (`com.test.outer.inner`) is found and
        // `lookup("TradeId")` succeeds.
        Optional<? extends Object> symbolOpt = ref.symbol();
        assertTrue(symbolOpt.isPresent(),
                "ref.symbol() should resolve via M3 non-aliased wildcard + sub-namespace traversal");
        assertSame(producerTradeId, symbolOpt.get(),
                "ref.symbol() should be the producer's TradeId RRule instance");
    }
}
