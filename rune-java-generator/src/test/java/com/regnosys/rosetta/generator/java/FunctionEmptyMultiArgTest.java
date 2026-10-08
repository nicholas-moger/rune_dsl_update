package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR #192 (facet {@code emptyMultiArg} — the absent/empty evaluate-arg into a MULTI
 * ({@code 0..*}) callee parameter, the THIRD and final cardinality dimension of PR #171's
 * {@code evaluate_arg_consumption} law after PR #191's {@code tailMulti}).
 *
 * <p>The law: an {@code evaluate()} argument that is ABSENT (an {@code empty} Rune literal —
 * the fork renders it as the bare literal {@code null}) and whose CALLEE PARAMETER is MULTI
 * ({@code 0..*}, {@code List}-typed) renders {@code Collections.<ElementType>emptyList()}, not
 * {@code null} — exactly upstream's item-to-list "empty" coercion
 * ({@code TypeCoercionService.xtend} L377-380; the same coercion PR #181's arm C6 already
 * renders for a MULTI conditional-output implicit else). The element type is the callee
 * parameter's item type; the golden adds the {@code java.util.Collections} import and the
 * element-type import. A SINGLE callee parameter's absent argument stays {@code null}
 * (untouched) — the sibling {@code null} arguments on the SAME call prove the per-parameter
 * precision.
 *
 * <p>Example ({@code MapConfirmationAgreedToTradeState}, trailing {@code partyTradeIdentifier
 * (0..*)} parameter of {@code mapTradeState}):
 * <pre>
 *   tradeState = toBuilder(mapTradeState.evaluate(fpmlTrade(fpmlConfirmationAgreed).get(),
 *       MapperS.of(fpmlConfirmationAgreed).&lt;PartiesAndAccountsModel&gt;map(...).get(),
 *       null,                                        // SINGLE absent arg — stays null
 *       Collections.&lt;PartyTradeIdentifier&gt;emptyList()));   // MULTI absent arg
 * </pre>
 * Pre-fix the fork emitted the trailing {@code null} for the MULTI parameter too. Because the
 * fork rendered {@code null} for EVERY absent MULTI argument while the golden always renders
 * {@code emptyList()}, every such file was already a mismatch (waivered); no green file can
 * carry the pre-fix form, so the rewrite touches only waivered files (green-safe by
 * construction).
 *
 * <p>Wiring (revert-locked by these whole-file anchors): the evaluate-arg loop in
 * {@link com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler} calls a new
 * {@code tryEmptyMultiArg} helper after the argument expression is finalized. It fires when the
 * callee parameter is MULTI (the PR #191 {@code evaluateParamIsMulti} probe) AND the compiled
 * argument renders the bare literal {@code "null"} AND the parameter is meta-free, resolving the
 * item type via {@code GeneratorModel.getType} + {@code JavaTypeTranslator.toJavaReferenceType}
 * and emitting {@code Collections.<Item>emptyList()} with the {@code Collections} + item-type
 * imports — mirroring {@code FunctionExpressionRenderer}'s arm-C6 setter form.
 *
 * <p>This is the revert-verified lock: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader) and byte-compares
 * WHOLE FILES against the frozen goldens (newline-normalized — fragment assertions are
 * insufficient per the PR #153 lesson).
 *
 * <p>Anchored:
 * <ul>
 *   <li>cdm6 {@code MapConfirmationAgreedToTradeState} — a TRAILING single empty-multi arg
 *       ({@code Collections.<PartyTradeIdentifier>emptyList()}).</li>
 *   <li>cdm6 {@code MapCalculationAgentIndependentToAncillaryParty} — an empty-multi arg
 *       COEXISTING with a PR #191 {@code .getMulti()} arg on the same call
 *       ({@code Collections.<Counterparty>emptyList()}).</li>
 *   <li>cdm6 {@code MapProtectionTermsToPriceQuantity} — a MIDDLE empty-multi arg (2nd of 4,
 *       not trailing — position-agnostic; {@code Collections.<Step>emptyList()}).</li>
 *   <li>drr {@code LeiRegistrationStatusIsValid} — proves the law is NOT cdm6-ingest-specific
 *       ({@code addBusinessDays.evaluate(eventDate, 1, Collections.<BusinessCenterEnum>emptyList())}).</li>
 * </ul>
 */
class FunctionEmptyMultiArgTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(),
                cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    // ---- absent/empty evaluate-arg into a MULTI param -> Collections.<T>emptyList() ----

    /** Trailing single empty-multi arg: {@code Collections.<PartyTradeIdentifier>emptyList()}. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapConfirmationAgreedToTradeState_cdm6_trailingEmptyMultiArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/message/functions/MapConfirmationAgreedToTradeState.java");
    }

    /** Empty-multi arg coexisting with a PR #191 {@code .getMulti()} arg on the same call. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapCalculationAgentIndependentToAncillaryParty_cdm6_emptyMultiWithGetMulti_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapCalculationAgentIndependentToAncillaryParty.java");
    }

    /** A MIDDLE empty-multi arg (2nd of 4, not trailing): {@code Collections.<Step>emptyList()}. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapProtectionTermsToPriceQuantity_cdm6_middleEmptyMultiArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapProtectionTermsToPriceQuantity.java");
    }

    /** drr carrier: proves the law is not cdm6-ingest-specific ({@code Collections.<BusinessCenterEnum>emptyList()}). */
    @Test
    @EnabledIf("cellsAvailable")
    void leiRegistrationStatusIsValid_drr_emptyMultiArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/enrichment/lei/functions/LeiRegistrationStatusIsValid.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the bare null at the absent MULTI evaluate-arg position reappears if the "
                + "tryEmptyMultiArg Collections.<T>emptyList() coercion is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
