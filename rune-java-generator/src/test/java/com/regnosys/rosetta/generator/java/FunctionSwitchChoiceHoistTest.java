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
 * PR #221 — switchChoiceHoist. A CHOICE/TYPE-keyed {@code switch} (the argument is a
 * data/choice type, every case guard a NAME guard resolving to a TYPE) renders the
 * upstream {@code instanceof} if-else-if block-hoist instead of the dormant M7b-1
 * {@code ControlFlowHandler.handle(RSwitchExpr)} stub's broken
 * {@code Objects.equals(<type>, <mapper>) ? … : null.get()/null.getMulti()} chained
 * ternary. Emitted shape:
 * <pre>
 * final &lt;ArgItemType&gt; switchArgument = &lt;argExpr&gt;.get();
 * if (switchArgument == null) {
 *     &lt;target&gt; = null;                                  // SET
 * } else if (switchArgument instanceof BondOption) {
 *     final BondOption bondOption = (BondOption) switchArgument;
 *     &lt;target&gt; = toBuilder(mapBondOption…(bondOption, …), () -&gt; X.builder());
 * } … else {
 *     &lt;target&gt; = toBuilder(&lt;default&gt;, () -&gt; X.builder());
 * }
 * </pre>
 * The ADD seat assigns {@code <list>.addAll(toBuilder(<body>))} per arm with the null /
 * default arms emitting {@code <list>.addAll(toBuilder(Collections.<E>emptyList()))}. The
 * switch SUBJECT ({@code item}, narrowed to the case type) re-roots onto the cast case var
 * via the {@code JavaStatementScope} switch-subject binding read by
 * {@code ReferenceHandler.handle(RImplicitVariable)}.
 *
 * <p><b>Green-safe by construction:</b> the dormant stub's {@code Objects.equals(<Type>,
 * MapperS.of(<arg>)) ? … : null.get()} form is a COMPILE ERROR (a type literal compared to a
 * Mapper by {@code Objects.equals}; a trailing {@code null.get()}/{@code null.getMulti()}), so
 * every choice-type switch carrier was already a waivered mismatch — the block-hoist can only
 * turn a waivered red into golden-identical, never green&rarr;red.
 *
 * <p>Whole-file byte anchors through the REAL D11 loader over the 6 CLEAN carriers (all cdm6;
 * 2 SET-direct + 3 ADD-direct + 1 inside an {@code if exists then switch} conditional then-arm)
 * whose case bodies reference the subject ONLY as a direct evaluate-arg. The 4 co-occupied
 * carriers (MapAsset, GetFpmlPayerReceiver, MapCapRateScheduleToPriceWithLocation,
 * MapFloorRateScheduleToPriceWithLocation),
 * whose case bodies navigate implicit features off the narrowed subject (a separate
 * alias/feature re-inline mechanism), are deferred. REVERT-VERIFIED RED.
 */
class FunctionSwitchChoiceHoistTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(), cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    // ---- SET-direct seat (the switch is the whole SET body) ----

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_ingestFpmlConfirmationToTradeState_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/message/functions/Ingest_FpmlConfirmationToTradeState.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_ingestFpmlConfirmationToWorkflowStep_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/message/functions/Ingest_FpmlConfirmationToWorkflowStep.java");
    }

    // ---- conditional then-arm seat (`if <arg> exists then <switch> else <default>`) ----

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapNonTransferableProduct_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/tradestate/functions/MapNonTransferableProduct.java");
    }

    // ---- ADD-direct seat (the switch is the whole ADD body — list.addAll(toBuilder(...))) ----

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapAncillaryPartyList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapAncillaryPartyList.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapCounterpartyList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapCounterpartyList.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapPriceQuantityList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/tradestate/functions/MapPriceQuantityList.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> cellOutput, Path goldenDir, String path)
            throws IOException {
        assertNotNull(cellOutput, "Function generation did not run — corpus unavailable?");
        String generated = cellOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — a choice/type-keyed switch must render the upstream `final <Arg> switchArgument = "
                + "<arg>.get(); if (switchArgument == null) {…} else if (switchArgument instanceof X) "
                + "{ final X x = (X) switchArgument; … } … else {…}` block-hoist (SET assigns the arm, "
                + "ADD does list.addAll(toBuilder(<arm>))), with the subject re-rooted onto the cast "
                + "case var. Reverting the switchChoiceHoist arms reverts this anchor to the bare "
                + "Objects.equals(<type>, <mapper>) ? … : null.get() non-compiling ternary (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
