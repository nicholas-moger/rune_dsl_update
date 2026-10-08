package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../tests/RosettaExpressionsTest.xtend} —
 * 10 methods (9 active + the upstream-{@code @Disabled}
 * date-subtraction-compile carried). The absent-else AST method ports
 * faithfully: the fork's {@code DefaultElseRule} mirrors Xtext's
 * {@code RosettaDerivedStateComputer} (a synthetic empty {@code RListLiteral}
 * fills the absent else; {@code SourceRange.NONE} = upstream's
 * {@code generated} flag). FOUR methods are PINNED facet leads carrying
 * leg-C findings #3 (the date−date {@code MapperMaths} triple renders
 * non-compiling BigDecimal generics), #4 (input-nav inside binary operands
 * leaves un-retracted {@code ENUM_NOT_FOUND} linking diagnostics) and #5
 * (the arithmetic operand validator is numeric-only — false positives on
 * upstream's legal date/time algebra). Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamRosettaExpressionsPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    private static final String MODEL_FILE = "expressions-port-model.rosetta";

    private record ParsedModel(RWorkspace ws, RModel model,
            List<LinkingDiagnostic> linking, List<ValidationDiagnostic> validation) { }

    /** Header-prepended, builtins+companion-loaded parse with MODEL-scoped diagnostics. */
    private static ParsedModel parseModel(String snippet) {
        try {
            List<RModel> models = new ArrayList<>();
            for (Path p : HoldOutByteCompareTest.resolveBuiltinFiles()) {
                models.add(AstBuilder.buildFromFile(p));
            }
            models.add(AstBuilder.buildFromString(
                    UpstreamPortHarness.COMMON_TEST_TYPES, "common-test-types.rosetta"));
            RModel model = AstBuilder.buildFromString(
                    UpstreamPortHarness.TEST_NS_HEADER + "\n" + snippet, MODEL_FILE);
            models.add(model);
            RLinkingResult result = RWorkspace.build(models);
            List<LinkingDiagnostic> linking = result.linkingDiagnostics().stream()
                    .filter(d -> MODEL_FILE.equals(d.range().file()))
                    .toList();
            List<ValidationDiagnostic> validation = result.workspace().validationDiagnostics().stream()
                    .filter(d -> MODEL_FILE.equals(d.range().file()))
                    .toList();
            return new ParsedModel(result.workspace(), model, linking, validation);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Upstream {@code absentElseBranchShouldBeSyntacticSugarForEmptyListLiteral}. */
    @Test
    void absentElseBranchShouldBeSyntacticSugarForEmptyListLiteral() {
        ParsedModel p = parseModel("""
                func AbsentElseSyntacticSugar:
                	output: result int (0..1)
                	set result:
                		if True then 0
                """);
        assertTrue(p.linking().isEmpty(), "parse/link errors: " + p.linking());

        RFunction fn = (RFunction) p.model().rootElements().get(0);
        RConditionalExpr conditional = AstWalker
                .findAll(fn.operations().get(0).expression(), RConditionalExpr.class).get(0);
        // Upstream: isFull == false (no ELSE keyword in source) and elsethen == a
        // GENERATED empty ListLiteral. The fork mirrors the SAME derived-state law
        // (DefaultElseRule ≙ Xtext's RosettaDerivedStateComputer): no "else" token
        // range, and the else branch IS a synthetic empty list literal
        // (SourceRange.NONE = the fork's "generated" marker).
        assertFalse(conditional.tokenRanges().containsKey("else"),
                "the else keyword must not appear in source (upstream isFull == false)");
        RExpression els = conditional.elseBranch().orElseThrow(
                () -> new AssertionError("the desugared else branch must be present"));
        RListLiteral lit = assertInstanceOf(RListLiteral.class, els);
        assertTrue(lit.elements().isEmpty(), "the desugared else must be the EMPTY list literal");
        assertEquals(SourceRange.NONE, lit.sourceRange(),
                "the desugared else must be synthetic (upstream lit.generated == true)");
    }

    /**
     * Upstream {@code shouldParseQualifierWithAdditiveExpression} — upstream
     * parses this model with NO errors. The leg-C finding-#4 pin (PR #423)
     * that recorded one un-retracted input-nav {@code ENUM_NOT_FOUND} per
     * {@code test -> one}/{@code test -> two} nav HEALED at the PR #443
     * class-(a) wave (the #279 {@code resolvedInputFeature} arm — which
     * already typed these navs and generated correct Java — now clears the
     * stale linking diagnostic on bind); un-pinned to upstream's no-errors
     * form.
     */
    @Test
    void shouldParseQualifierWithAdditiveExpression() {
        ParsedModel p = parseModel("""
                type Test:
                	one number (1..1)
                	two number (1..1)

                func TestQualifier:
                	inputs: test Test (1..1)
                	output: result boolean (1..1)
                	set result:
                		test -> one + test -> two = 42
                """);
        assertEquals(List.of(), p.linking(),
                "upstream parses this model with NO errors (the finding-#4 stale "
                + "nav diagnostics healed at #443): " + p.linking());
        assertEquals(List.of(), p.validation(),
                "upstream's parseRosettaWithNoErrors covers the validation "
                + "channel too: " + p.validation());
    }

    /**
     * Upstream {@code shouldParseNoIssuesWhenDateSubtraction} — upstream:
     * date − date is LEGAL (types as the day-count int) and the model parses
     * AND validates clean. Both pinned halves have now healed: the
     * finding-#5 half at PR #441 (facet arithTypeAlgebra — upstream's
     * arithmetic algebra admits the date−date form) and the finding-#4
     * linking half at the PR #443 class-(a) wave (the input-feature channel
     * clears the stale nav diagnostics on bind) — the full upstream
     * no-issues form on both channels.
     */
    @Test
    void shouldParseNoIssuesWhenDateSubtraction() {
        ParsedModel p = parseModel("""
                type Test:
                	one date (1..1)
                	two date (1..1)

                func TestQualifier:
                	inputs: test Test (1..1)
                	output: result boolean (1..1)
                	set result:
                		test -> one - test -> two = 42
                """);
        assertEquals(List.of(), p.linking(),
                "upstream parses this model with NO errors (the finding-#4 stale "
                + "nav diagnostics healed at #443): " + p.linking());
        assertEquals(List.of(), p.validation(),
                "the LEGAL date subtraction must validate CLEAN (upstream's no-issues "
                + "form — the finding-#5 numeric-only false positives died at the "
                + "#441 arithmetic-algebra wave): " + p.validation());
    }

    /**
     * Upstream {@code shouldParseWithErrorWhenAddingDates} — un-pinned at
     * PR #441 (the finding-#5 wave, facet arithTypeAlgebra): the ported
     * arithmetic algebra rejects date + date on the RIGHT operand with
     * upstream's message byte-identical (date + TIME is the legal form —
     * the date/time algebra). Body = the upstream assertError message
     * verbatim (the finding-#4 linking pair that rode the same model healed
     * at #443 — the subtraction test above asserts the clean form).
     */
    @Test
    void shouldParseWithErrorWhenAddingDates() {
        ParsedModel p = parseModel("""
                type Test:
                	one date (1..1)
                	two date (1..1)


                func TestQualifier:
                	inputs: test Test (1..1)
                	output: result boolean (1..1)
                	set result:
                		test -> one + test -> two = 42
                """);
        assertEquals(1, p.validation().stream()
                .filter(d -> d.message().equals(
                        "Expected type `time`, but got `date` instead. Cannot add `date` to a `date`"))
                .count(),
                "expected exactly the upstream date+date error on the right operand: "
                + p.validation());
    }

    /**
     * Upstream {@code shouldCodeGenerateWithMoreGenericsInformation} — un-pinned
     * at PR #424 (leg-C finding #3 HEALED — facet dateArithTyping types
     * date − date as int [upstream caseSubtractOperation's UNCONSTRAINED_INT
     * day-count arm] + facet dateArithWitness renders upstream's non-numeric
     * branch {@code MapperMaths.<Integer, Date, Date>subtract(…)} over the
     * operands' OWN Date types; the {@code expr-date-subtract} oracle group is
     * the byte witness, 6/6 byte-identical). Body = the upstream containsString
     * assert verbatim.
     */
    @Test
    void shouldCodeGenerateWithMoreGenericsInformation() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Test:
                	one date (1..1)
                	two date (1..1)

                func TestQualifier:
                	inputs: test Test (1..1)
                	output: result boolean (1..1)
                	set result:
                		test -> one - test -> two = 42
                """);
        String qualifier = code.get("com.rosetta.test.model.functions.TestQualifier");
        assertTrue(qualifier.contains("MapperMaths.<Integer, Date, Date>subtract"),
                "the upstream day-count witness must render for date - date in:\n" + qualifier);
    }

    /** Upstream {@code shoudCodeGenerateAndCompileWhenSubtractingDates} — upstream-@Disabled, carried. */
    @Test
    @Disabled("upstream-@Disabled: date-subtraction compile (upstream never enabled it)")
    void shoudCodeGenerateAndCompileWhenSubtractingDates() {
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                type Test:
                	one date (1..1)
                	two date (1..1)

                func TestQualifier:
                	inputs: test Test (1..1)
                	output: result boolean (1..1)
                	set result:
                		test -> one - test -> two = 42
                """));
    }

    /** Upstream {@code shoudCodeGenerateAndCompileWhenAddingNumbers}. */
    @Test
    void shoudCodeGenerateAndCompileWhenAddingNumbers() {
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                type Test:
                	one number (1..1)
                	two int (1..1)


                func TestQualifier:
                	inputs: test Test (1..1)
                	output: result boolean (1..1)
                	set result:
                		test -> one + test -> two = 42
                """));
    }

    /** Upstream {@code shoudCodeGenerateAndCompileAccessingMetaSimple}. */
    @Test
    void shoudCodeGenerateAndCompileAccessingMetaSimple() {
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                type Test:
                	one string (1..1)
                		[metadata scheme]
                	two int (1..1)

                func TestQualifier:
                	inputs: test Test (1..1)
                	output: result boolean (1..1)
                	set result:
                		test -> one -> scheme = "scheme"
                """));
    }

    /** Upstream {@code shoudCodeGenerateAndCompileAccessingMeta}. */
    @Test
    void shoudCodeGenerateAndCompileAccessingMeta() {
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                type Test:
                	one Foo (1..1)
                		[metadata scheme]
                	two int (1..1)

                type Foo:
                	one string (1..1)
                		[metadata scheme]
                	two int (1..1)

                func TestQualifier:
                	inputs: test Test(1..1)
                	output: is_product boolean (1..1)
                	set is_product:
                		test -> one -> scheme = "scheme"
                """));
    }

    /** Upstream {@code shoudCodeGenerateAndCompileAccessPastMeta}. */
    @Test
    void shoudCodeGenerateAndCompileAccessPastMeta() {
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                type Test:
                	one Foo (1..1)
                		[metadata scheme]
                	two int (1..1)

                type Foo:
                	one string (1..1)
                		[metadata scheme]
                	two int (1..1)

                func TestQualifier:
                	inputs: test Test(1..1)
                	output: is_product boolean (1..1)
                	set is_product:
                		test -> one -> one = "scheme"
                """));
    }
}
