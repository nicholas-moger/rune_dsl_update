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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR #195 (facet {@code fpmlInputFqn} — a function INPUT parameter whose Java simple type name
 * collides with the function's OUTPUT type but a DIFFERENT fully-qualified name is rendered
 * FULLY-QUALIFIED inline in the signatures, with NO import).
 *
 * <p>The law (upstream {@code ImportingStringConcatenation.internalDoImportIfPossible} +
 * {@code JavaFileScope}): when a generated file references two distinct types sharing a simple
 * name, the FIRST type to claim the simple name is imported and rendered simple; a later collider
 * with a different canonical name is rendered by its dotted FQN inline and NOT imported
 * (first-claim-wins). For the cdm6 ingest-fpml {@code Map*} functions the CDM OUTPUT type (e.g.
 * {@code cdm.observable.asset.Money}) claims the bare {@code Money} first; the same-simple-named
 * fpml INPUT param ({@code fpml.consolidated.shared.Money}) collides and is emitted as
 * {@code fpml.consolidated.shared.Money fpmlMoney} in {@code evaluate}/{@code doEvaluate}/
 * {@code assignOutput}, with NO {@code import fpml.consolidated.shared.Money;}.
 *
 * <p>The fork had NO input-param-vs-output collision handling: {@code FunctionGenerator} always
 * rendered the input by its simple name AND imported it, so BOTH {@code import cdm...Money;} and
 * {@code import fpml.consolidated.shared.Money;} were emitted — a DUPLICATE simple-name import =
 * Java compile error. So every carrier was already a waivered mismatch; no green file can carry the
 * duplicate-import form (green-safe by construction). The fix reuses the EXISTING dependency/host
 * collision convention ({@code ParamModel} with {@code typeName=FQN, typeFqn=null} → FQN-inline +
 * import suppressed; engine PR #6 / PR #194 {@code usRename}) applied to the sibling INPUT loop,
 * gated by the MANDATORY FQN-DIFFERENCE check (same-simple-name + DIFFERENT fqn) so a same-type
 * self-mapping input ({@code SetCashCurrency(Cash cash) → Cash}, same fqn) stays bare with one import.
 *
 * <p>This is the revert-verified lock: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader, which loads the
 * transitive rune-fpml dependency per PR #184 so the fpml input types resolve) and byte-compares
 * WHOLE FILES against the frozen goldens (newline-normalized — fragment assertions are insufficient
 * for the flip anchors per the PR #153 lesson). All carriers are cdm/6.20.6 ingest-fpml.
 *
 * <p>Anchored:
 * <ul>
 *   <li>{@code MapMoney} — output {@code cdm.observable.asset.Money}, input
 *       {@code fpml.consolidated.shared.Money}; the canonical single-input collision.</li>
 *   <li>{@code MapAddress} — party namespace; output {@code cdm...Address}, input
 *       {@code fpml.consolidated.shared.Address}.</li>
 *   <li>{@code MapAdjustableDates} — datetime namespace; output {@code cdm...AdjustableDates},
 *       input {@code fpml.consolidated.shared.AdjustableDates}.</li>
 *   <li>{@code SetCashCurrency} — the REGRESSION-LANDMINE GUARD: a SAME-FQN self-mapping input
 *       ({@code Cash cash} → {@code Cash}, both {@code cdm.base.staticdata.asset.common.Cash}) must
 *       STAY BARE with one import — the FQN-difference gate must NOT fire. (SetCashCurrency is
 *       waivered for the unrelated setterCoercion mechanism, so this is a fragment-level guard on
 *       the bare signature, not a whole-file match.)</li>
 * </ul>
 */
class FunctionFpmlInputFqnTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    // ---- input-param-vs-output simple-name collision → FQN-inline + suppressed import ----

    /** The canonical single-input collision (output cdm Money, input fpml Money). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapMoney_cdm6_inputCollidesWithOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/common/functions/MapMoney.java");
    }

    /** Party namespace collision (output cdm Address, input fpml Address). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapAddress_cdm6_inputCollidesWithOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapAddress.java");
    }

    /** Datetime namespace collision (output cdm AdjustableDates, input fpml AdjustableDates). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapAdjustableDates_cdm6_inputCollidesWithOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapAdjustableDates.java");
    }

    /**
     * REGRESSION-LANDMINE GUARD: a SAME-FQN self-mapping input must STAY BARE — the FQN-difference
     * gate must not fire when input fqn == output fqn. SetCashCurrency takes {@code Cash cash} and
     * returns {@code Cash} (both {@code cdm.base.staticdata.asset.common.Cash}); the param must
     * render bare {@code Cash cash}, never the FQN form, with a single Cash import. (Fragment guard:
     * SetCashCurrency is waivered for the unrelated setterCoercion mechanism, so a whole-file match
     * would not hold — this asserts only that the fpmlInputFqn gate leaves the signature bare.)
     */
    @Test
    @EnabledIf("cellsAvailable")
    void setCashCurrency_cdm6_sameFqnSelfMapStaysBare_noSpuriousFqn() {
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get(
                "cdm/base/staticdata/asset/common/functions/SetCashCurrency.java");
        assertNotNull(generated, "Function not generated: SetCashCurrency");
        assertTrue(generated.contains("public Cash evaluate(Cash cash,"),
                "SetCashCurrency's same-FQN self-mapping input must stay BARE (Cash cash); the "
                + "fpmlInputFqn FQN-difference gate must NOT fire when input fqn == output fqn.");
        assertFalse(generated.contains("cdm.base.staticdata.asset.common.Cash cash"),
                "SetCashCurrency's input must NOT be FQN-qualified — a same-FQN self-map is not a "
                + "collision; the gate over-fired (it ignored the FQN-difference check).");
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
                + " — the duplicate same-simple-name import (output cdm type + fpml input type) "
                + "reappears if the fpmlInputFqn collision fix (FunctionGenerator input ParamModel "
                + "FQN-inline + suppressed import) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
