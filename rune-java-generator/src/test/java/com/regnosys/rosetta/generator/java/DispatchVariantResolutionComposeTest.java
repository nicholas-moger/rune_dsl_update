package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * PR #369 — the dispatch-variant param resolution + the alias switch nav-ladder +
 * the inner-ctor round-trip: 9 byte flips (3 cdm5 FUNCTION + 6 cdm6 FUNCTION)
 * + 0 new / 0 away.
 *
 * <p><b>F-C dispatchVariantParamResolution</b> (HandlerHelper + ReferenceHandler +
 * NavigationHandler + FunctionAliasHelper + FunctionGenerator +
 * FunctionExpressionRenderer + the dispatch template): a dispatch VARIANT's
 * RFunction carries {@code inputs=[__synthesized_input__]}, so every
 * expression-side consumer that scanned the enclosing function's declared inputs
 * failed inside variant bodies. {@code HandlerHelper.dispatchBaseOf} locates the
 * BASE (same-RModel same-named dispatch-free declaration — the signatureSource
 * law's expression-side mirror) and feeds five consumer seats in lockstep:
 * alias-call arg forwarding ({@code endDate(calculationPeriod, …)} not
 * {@code endDate(__synthesized_input__)}), disguised-nav heads, the
 * receiver-element walk (witness/arity/naming cascade), the alias-signature walk
 * ({@code MapperS<ResetRelativeToEnum>}, the conditional join
 * {@code MapperS<? extends CalculationPeriodBase>}), and the naming seeds (the
 * self-shadow {@code _resetDates} escape). The template's anonymous-subtemplate
 * {@code \{} escapes leaked literally (STv4 unescapes only {@code \}}) and the
 * deps/alias iterations carried trailing-newline blanks — both fixed. The
 * RLibraryFunction calls render the runtime forms ({@code new Min()/Max()
 * .execute(…)}; IsLeapYear's guarded {@code integer == null ?
 * MapperS.<Boolean>ofNull() : MapperS.of(new IsLeapYear().execute(
 * BigDecimal.valueOf(integer)))} two-statement lifted return), the DATE
 * record-accessors type Integer at the walk + operand seats
 * ({@code .<Integer>map("Year", Date::getYear)}), and numericOperandKind gains
 * the nested-DIVIDE-before-engine + dispatch-param + conditional-join +
 * Min/Max-arg-join arms (the MapperMaths type args): YearFraction +
 * ComputeCalculationPeriod + ProcessFloatingRateReset flip cdm5 + cdm6.
 *
 * <p><b>F-A aliasSwitchNavLadder</b> (FunctionAliasHelper + FunctionGenerator +
 * FunctionExpressionRenderer): the #365 alias switch RETURN ladder admits
 * case-narrowed DISGUISED-nav results — the walk types the leaf through the case
 * guard's type CARRYING cardinality (single heads only), the ladder threads the
 * FULL join (MapperS/MapperC ofNull terminals) and compiles each case value under
 * the #221 bindSwitchSubject re-root so the #368 synthesis renders the narrowed
 * chain, splicing the nav result RAW:
 * MapFloatingRateMultiplerScheduleToPriceWithLocation +
 * MapSpreadScheduleToPriceWithLocation cdm6.
 *
 * <p><b>F-E innerCtorMapperSRoundTrip</b> (ConstructionHandler): a SINGLE
 * ctor-setter value terminating in a BARE nested ctor (bare, or a then-chain whose
 * FINAL body root is one) wraps {@code MapperS.of(<built>).get()} — the #218 law
 * at the nested-ctor setter-arg seat; the {@code then extract <ctor>} wrap
 * deliberately declines (the in-chain lambda form is the GREEN
 * MapQuantityMultiplier class — the cp9 over-fire catch):
 * MapAdjustableOrRelativeDates cdm6.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and
 * revert RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below
 * was occurrence-counted in its PRE gen (f-probe-368post — counts stated per
 * witness) and 0 in its golden — the flips REMOVE them.
 */
class DispatchVariantResolutionComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 3 cdm5 FUNCTION flip carriers (F-C — the dispatch trio). */
    private static final String[] CDM5_FUNCTIONS = {
            "cdm/base/datetime/daycount/functions/YearFraction.java",
            "cdm/observable/asset/calculatedrate/functions/ComputeCalculationPeriod.java",
            "cdm/product/asset/floatingrate/functions/ProcessFloatingRateReset.java",
    };

    /** The 6 cdm6 FUNCTION flip carriers (F-C trio + F-A pair + F-E). */
    private static final String[] CDM6_FUNCTIONS = {
            "cdm/base/datetime/daycount/functions/YearFraction.java",
            "cdm/observable/asset/calculatedrate/functions/ComputeCalculationPeriod.java",
            "cdm/product/asset/floatingrate/functions/ProcessFloatingRateReset.java",
            "cdm/ingest/fpml/confirmation/datetime/functions/MapAdjustableOrRelativeDates.java",
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapFloatingRateMultiplerScheduleToPriceWithLocation.java",
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapSpreadScheduleToPriceWithLocation.java",
    };

    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    /** The REAL D11 FUNCTION-kind generation path (function_comparison). */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    // -------------------------------------------- cdm5 FUNCTION byte locks (3)

    @Test
    @EnabledIf("cdm5CellAvailable")
    void cdm5Functions_byteMatchGolden() throws IOException {
        for (String path : CDM5_FUNCTIONS) {
            assertBytes(path, fn(cdm5FnOutput, path), CDM5_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- cdm6 FUNCTION byte locks (6)

    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6Functions_byteMatchGolden() throws IOException {
        for (String path : CDM6_FUNCTIONS) {
            assertBytes(path, fn(cdm6FnOutput, path), CDM6_GOLDEN_DIR);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The F-C param-forwarding witness (ComputeCalculationPeriod cdm6): the
     * placeholder-forwarded alias call {@code endDate(__synthesized_input__)}
     * counted EXACTLY 1 occurrence in the PRE gen (f-probe-368post) and 0 in the
     * golden — the dispatch-BASE forwarding replaces it with the full param list.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void computeCalculationPeriod_synthesizedInputForwardingGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[1]),
                        "endDate(__synthesized_input__)"),
                "The __synthesized_input__ alias-call forwarding must be gone (PRE count 1)");
    }

    /**
     * The F-C template-escape witness (ProcessFloatingRateReset cdm6): the literal
     * {@code ) \{} brace-escape leak counted EXACTLY 18 occurrences in the PRE gen
     * (f-probe-368post; ComputeCalculationPeriod carried 7) and 0 in the golden —
     * STv4 unescapes only {@code \}}, so the open braces are now plain.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void processFloatingRateReset_braceEscapeLeakGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[2]), ") \\{"),
                "The literal \\{ template-escape leak must be gone (PRE count 18)");
    }

    /**
     * The F-C library-render witnesses (YearFraction cdm6): the non-compiling
     * symbol-echo static calls {@code Min.evaluate(} / {@code IsLeapYear.evaluate(}
     * counted EXACTLY 4 and 3 occurrences in the PRE gen (f-probe-368post) and 0 in
     * the golden — the runtime {@code new Min().execute(…)} form and the guarded
     * IsLeapYear two-statement lifted return replace them.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void yearFraction_libraryEchoCallsGone() {
        String gen = fn(cdm6FnOutput, CDM6_FUNCTIONS[0]);
        assertEquals(0, count(gen, "Min.evaluate("),
                "The Min symbol-echo static call must be gone (PRE count 4)");
        assertEquals(0, count(gen, "IsLeapYear.evaluate("),
                "The IsLeapYear symbol-echo static call must be gone (PRE count 3)");
    }

    /**
     * The F-C record-accessor witness (YearFraction cdm6): the getter-lambda form
     * {@code .map("getYear", } counted EXACTLY 9 occurrences in the PRE gen
     * (f-probe-368post) and 0 in the golden — the DATE record method-ref form
     * {@code .<Integer>map("Year", Date::getYear)} replaces it.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void yearFraction_dateGetterLambdaGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[0]), ".map(\"getYear\", "),
                "The Date getter-lambda nav must be gone (PRE count 9)");
    }

    /**
     * The F-C signature-echo witness (YearFraction cdm5 — the cdm5-cell twin): the
     * raw-symbol signature echo {@code MapperS<startDate>} counted EXACTLY 12
     * occurrences in the PRE gen (f-probe-368post) and 0 in the golden — the DATE
     * record-accessor typing resolves the alias signatures to
     * {@code MapperS<Integer>}.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void yearFractionCdm5_signatureEchoGone() {
        assertEquals(0, count(fn(cdm5FnOutput, CDM5_FUNCTIONS[0]), "MapperS<startDate>"),
                "The raw-symbol alias-signature echo must be gone (PRE count 12)");
    }

    /**
     * The F-C numeric-join witness (YearFraction cdm6): the engine's int/int-join
     * mis-typed add-over-divide {@code MapperMaths.<Integer, Integer, Integer>add(
     * MapperMaths.<BigDecimal} counted EXACTLY 1 occurrence in the PRE gen
     * (f-probe-368post) and 0 in the golden — the nested-DIVIDE-before-engine arm
     * joins it {@code <BigDecimal, BigDecimal, BigDecimal>}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void yearFraction_divideJoinMistypeGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[0]),
                        "MapperMaths.<Integer, Integer, Integer>add(MapperMaths.<BigDecimal"),
                "The int/int-joined add-over-divide must be gone (PRE count 1)");
    }

    /**
     * The F-A ladder witness (MapFloatingRateMultiplerSchedule cdm6): the legacy
     * inline type-equality ternary head {@code Objects.equals(fpml.
     * FloatingRateCalculation, MapperS.of(fpmlRate)) ?} counted EXACTLY 1 occurrence
     * in the PRE gen (f-probe-368post) and 0 in the golden — the instanceof RETURN
     * ladder with the case-narrowed chain replaces it.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapFloatingRateMultipler_inlineTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[4]),
                        "Objects.equals(fpml.FloatingRateCalculation, MapperS.of(fpmlRate)) ?"),
                "The legacy type-equality ternary must be gone (PRE count 1)");
    }

    /**
     * The F-A signature witness (MapSpreadSchedule cdm6): the mis-typed legacy
     * signature {@code MapperC<? extends PriceSchedule> spreadSchedule} counted
     * EXACTLY 2 occurrences in the PRE gen (f-probe-368post — abstract + override)
     * and 0 in the golden — the case-narrowed leaf join types it
     * {@code MapperC<? extends SpreadSchedule>}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapSpreadSchedule_legacySignatureGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[5]),
                        "MapperC<? extends PriceSchedule> spreadSchedule"),
                "The mis-typed legacy alias signature must be gone (PRE count 2)");
    }

    /**
     * The F-E round-trip witness (MapAdjustableOrRelativeDates cdm6): the bare
     * built-ctor deref {@code .build().get())} counted EXACTLY 1 occurrence in the
     * PRE gen (f-probe-368post) and 0 in the golden — the identity round-trip
     * {@code MapperS.of(….build()).get()} replaces it.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAdjustableOrRelativeDates_bareBuiltDerefGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[3]), ".build().get())"),
                "The bare built-ctor .get() deref must be gone (PRE count 1)");
    }

    // ------------------------------------------------------------------ helpers

    private static String fn(Map<String, String> output, String path) {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String gen = output.get(path);
        assertNotNull(gen, () -> "Function output missing for " + path);
        return gen;
    }

    private static void assertBytes(String path, String gen, Path goldenDir) throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), () -> "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r\n", "\n");
        assertEquals(golden, gen.replace("\r\n", "\n"),
                () -> "Generated bytes must match golden for " + path);
    }

    /** OCCURRENCE count (the witness-uniqueness law — never line counts). */
    private static int count(String text, String token) {
        int n = 0;
        int i = text.indexOf(token);
        while (i >= 0) {
            n++;
            i = text.indexOf(token, i + 1);
        }
        return n;
    }
}
