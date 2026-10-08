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
 * references through an aliased wildcard import.
 *
 * <p>Real-world corpus pattern (paste-quoted from
 * {@code test-corpus/drr/drr-7.0.0-dev.113/.../regulation-common-margin-collateral-rule.rosetta}):
 *
 * <pre>
 * namespace drr.regulation.common.margin.collateral
 * import drr.standards.iosco.cde.version2.* as cdeV3
 * ...
 * reporting rule ExcessCollateralPostedByTheCounterparty1 from CollateralReportInstruction:
 *     cdeV3.collateral.ExcessCollateralPostedByTheReportingCounterparty
 * </pre>
 *
 * <p>{@code cdeV3.collateral.ExcessCollateral...} is an aliased prefix
 * ({@code cdeV3} → {@code drr.standards.iosco.cde.version2}) PLUS a
 * sub-namespace traversal ({@code .collateral}) PLUS a local rule name.
 *
 * <p>Empirical evidence: focused D11 on {@code drr/7.0.0-dev.113} found
 * 207 of 444 RuleGenerator fail-fast errors come from RSymbolReference
 * nodes with {@code symbol()} EMPTY — i.e. M3 isn't resolving these refs.
 * See the development audit "phase-x1-T2-mvp1-ast-bucket-evidence".
 *
 * <p>Synthetic minimal repro: fixture under
 * {@code rune-parser/src/test/resources/symbolid-fixtures/qualified-rule-ref-aliased/}
 * declares a producer rule {@code TradeId} in {@code com.test.outer.inner}
 * and a consumer rule {@code DelegatingRule} that references it via
 * {@code outerAlias.inner.TradeId} where {@code alias} is the import alias for
 * {@code com.test.outer.*}.
 */
class QualifiedRuleRefAliasedTest {

    @Test
    void aliasedCrossNamespaceRuleRef_resolves() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("qualified-rule-ref-aliased/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        // Find the consumer's DelegatingRule
        RRule delegatingRule = ws.namespace("com.test.consumer")
                .flatMap(n -> n.lookup("DelegatingRule"))
                .filter(RRule.class::isInstance)
                .map(RRule.class::cast)
                .orElseThrow(() -> new AssertionError(
                        "Consumer's DelegatingRule not found in workspace"));

        // The producer's TradeId rule
        RRule producerTradeId = ws.namespace("com.test.outer.inner")
                .flatMap(n -> n.lookup("TradeId"))
                .filter(RRule.class::isInstance)
                .map(RRule.class::cast)
                .orElseThrow(() -> new AssertionError(
                        "Producer's TradeId not found in workspace"));

        // Find the RSymbolReference inside DelegatingRule's body — should be
        // the bare `outerAlias.inner.TradeId` reference.
        RExpression body = delegatingRule.expression()
                .orElseThrow(() -> new AssertionError("DelegatingRule has no expression body"));
        List<RSymbolReference> refs = AstWalker.findAll(body, RSymbolReference.class);
        assertEquals(1, refs.size(), "Expected exactly one RSymbolReference in DelegatingRule body");

        RSymbolReference ref = refs.get(0);
        assertEquals("outerAlias.inner.TradeId", ref.name(),
                "RSymbolReference.name() should be the full aliased qualified name");

        // ASSERTION: ref.symbol() resolves to the producer's TradeId RRule.
        // PRE-FIX expectation: this assertion FAILS (symbol() is empty —
        // M3 doesn't resolve aliased cross-namespace qualified rule refs).
        // POST-FIX expectation: ref.symbol() is present and same instance.
        Optional<? extends Object> symbolOpt = ref.symbol();
        assertTrue(symbolOpt.isPresent(),
                "ref.symbol() should resolve via M3 alias + sub-namespace traversal");
        assertSame(producerTradeId, symbolOpt.get(),
                "ref.symbol() should be the producer's TradeId RRule instance");
    }
}
