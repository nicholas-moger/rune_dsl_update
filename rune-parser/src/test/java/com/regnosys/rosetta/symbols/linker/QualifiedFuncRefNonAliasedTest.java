package com.regnosys.rosetta.symbols.linker;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RFunction;
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
 * Phase X1 closure T0d — mirrors {@link QualifiedRuleRefNonAliasedTest} but the
 * cross-namespace target is a FUNCTION not a rule. Validates the DRR corpus
 * pattern {@code product.IsIRSwaption} (function target in sub-namespace) where
 * the consumer file imports the parent namespace via non-aliased wildcard.
 *
 * <p>Empirical match in drr/7.0.0-dev.113 corpus: {@code MaturityDateOfTheUnderlier}
 * rule body references {@code product.IsIRSwaption} where {@code IsIRSwaption} is
 * a {@code func} in {@code drr.base.qualification.product} and the consumer
 * file imports {@code drr.base.qualification.*}. Failing in PR #76 D11 dump as
 * {@code symRes=EMPTY}.
 */
class QualifiedFuncRefNonAliasedTest {

    @Test
    void nonAliasedCrossNamespaceFuncRef_resolves() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("qualified-func-ref-non-aliased/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RRule delegatingRule = ws.namespace("com.test.consumer")
                .flatMap(n -> n.lookup("DelegatesToFunc"))
                .filter(RRule.class::isInstance)
                .map(RRule.class::cast)
                .orElseThrow(() -> new AssertionError(
                        "Consumer's DelegatesToFunc not found"));

        RFunction producerFunc = ws.namespace("com.test.outer.inner")
                .flatMap(n -> n.lookup("IsSwap"))
                .filter(RFunction.class::isInstance)
                .map(RFunction.class::cast)
                .orElseThrow(() -> new AssertionError(
                        "Producer's IsSwap func not found"));

        RExpression body = delegatingRule.expression()
                .orElseThrow(() -> new AssertionError("DelegatesToFunc has no expression body"));
        List<RSymbolReference> refs = AstWalker.findAll(body, RSymbolReference.class);
        // The body is `extract inner.IsSwap`; the inner.IsSwap is the only
        // RSymbolReference in the body. Pin the exact-1 expectation here
        // (mirrors QualifiedRuleRefNonAliasedTest's strict count assertion)
        // so future fixture drift surfaces immediately.
        assertEquals(1, refs.size(),
                "Expected exactly one RSymbolReference in DelegatesToFunc body; found names: "
                + refs.stream().map(RSymbolReference::name).toList());
        RSymbolReference innerRef = refs.get(0);
        assertEquals("inner.IsSwap", innerRef.name(),
                "RSymbolReference.name() should be the non-aliased relative qualified name");

        Optional<? extends Object> symbolOpt = innerRef.symbol();
        assertTrue(symbolOpt.isPresent(),
                "ref.symbol() should resolve via M3 non-aliased wildcard + sub-namespace traversal "
                + "for a FUNCTION target");
        assertSame(producerFunc, symbolOpt.get(),
                "ref.symbol() should be the producer's IsSwap RFunction instance");
    }
}
