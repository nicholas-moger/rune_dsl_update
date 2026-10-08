package com.regnosys.rosetta.ast.builder;

import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * PR #337 anchor — facet {@code extractDefaultAssociativity}
 * ({@link AstBuilder#reassociateExtractTrailingDefaults}).
 *
 * <p>Upstream's stratified grammar ({@code ImplicitInlineFunction: body=OrOperation} →
 * {@code BinaryOperation}'s single optional binary rung) admits AT MOST ONE {@code default}
 * inside an implicit inline-function body, so in {@code extract A default B default C} the
 * second {@code default} climbs OUT of the extract:
 * {@code Default( Extract(body=Default(A,B)), C )} — C's implicit input binds the OUTER scope
 * (the rule input), not the extract item. The fork's left-recursive grammar nests the whole
 * chain greedily; the AstBuilder re-associates post-parse. Carrier: drr
 * {@code OptionPremiumCurrency} (the sole chained-default expression in the frozen 9.83.0
 * corpus).
 */
class AstBuilderExtractDefaultAssociativityTest {

    private static RRule parseRule(String body) {
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    %s
                """.formatted(body);
        RModel model = AstBuilder.buildFromString(
                source, "AstBuilderExtractDefaultAssociativityTest.rosetta");
        return (RRule) model.rootElements().get(0);
    }

    /**
     * The carrier shape: {@code extract A default B default C} re-associates to
     * {@code Default( Extract(body=Default(A,B)), C )} — the outer default's LEFT is the
     * extract, its RIGHT is C, and the extract's implicit body keeps EXACTLY the inner rung.
     */
    @Test
    void chainedDefaults_secondDefaultClimbsOutOfExtract() {
        RRule rule = parseRule("extract a default b default c");
        RDefaultExpr outer = assertInstanceOf(RDefaultExpr.class, rule.expression().orElseThrow(),
                "the rule body must be the re-associated OUTER default");
        RExtractExpr extract = assertInstanceOf(RExtractExpr.class, outer.rawLeft(),
                "the outer default's LEFT must be the extract");
        assertNotNull(outer.rawRight(), "the outer default's RIGHT must be C");
        RDefaultExpr inner = assertInstanceOf(RDefaultExpr.class,
                extract.body().body(),
                "the extract's implicit body must keep exactly the INNER default rung");
        assertFalse(inner.rawLeft() instanceof RDefaultExpr,
                "the inner rung must be the innermost (A default B) — no residual chain");
        // Parent links (stamped post-build) must be consistent with the re-parented shape.
        assertSame(outer, extract.parent(),
                "the extract must re-parent under the outer default");
    }

    /**
     * Decline — a SINGLE default stays INSIDE the extract body (upstream's one admitted rung):
     * {@code extract A default B} keeps {@code Extract(body=Default(A,B))}.
     */
    @Test
    void singleDefault_staysInsideExtractBody() {
        RRule rule = parseRule("extract a default b");
        RExtractExpr extract = assertInstanceOf(RExtractExpr.class,
                rule.expression().orElseThrow(),
                "a single-default extract must stay the rule body's top node");
        assertInstanceOf(RDefaultExpr.class, extract.body().body(),
                "the single default must stay inside the implicit body");
    }

    /**
     * Decline — a PARENTHESIZED inner chain stays fully inside: upstream parses
     * {@code extract (A default B) default C} with the paren as a primary, so the body's one
     * admitted rung is {@code <paren> default C} — nothing climbs out. The parse-tree gate
     * (the {@code ParenExprContext} stops the left-spine walk; parens are AST-transparent
     * per D9, so an AST-level walk could not see the boundary) keeps the whole chain inside.
     */
    @Test
    void parenthesizedInnerChain_staysInsideExtractBody() {
        RRule rule = parseRule("extract (a default b) default c");
        RExtractExpr extract = assertInstanceOf(RExtractExpr.class,
                rule.expression().orElseThrow(),
                "a parenthesized inner chain must keep the extract as the top node");
        RDefaultExpr outer = assertInstanceOf(RDefaultExpr.class, extract.body().body(),
                "the body keeps the full (paren-bounded) chain");
        assertInstanceOf(RDefaultExpr.class, outer.rawLeft(),
                "the paren-transparent inner default stays nested inside the body");
    }
}
