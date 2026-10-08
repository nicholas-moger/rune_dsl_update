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
 * PR #203 — FOUR disjoint FUNCTION-codegen mechanisms (25 flips), whole-file byte anchors through the
 * REAL D11 loader (transitive rune-fpml dep per PR #184). Each anchor reverts RED if its mechanism is
 * removed; all four are green-safe by construction (each replaces a non-compiling, already-waivered
 * form — stash-baseline confirmed 0 now-matching pristine).
 *
 * <ul>
 *   <li><b>typedAddSegment</b> ({@code FunctionExpressionRenderer.renderAddSegmentChainOrNull}) — an
 *       ADD to a SEGMENT-path MULTI attribute renders the typed builder add-method chain
 *       {@code <root>\n\t.add<Leaf>(<value>.getMulti())}; the fork ignored the segment and emitted the
 *       non-compiling generic {@code <root>.addAll(toBuilder(<value>.getMulti()))} (addAll on the
 *       output ROOT, not the segment's list). Anchors: {@code Create_Observation} (single value,
 *       {@code .addObservationHistory}), {@code Create_Transfer} ({@code mapC} value,
 *       {@code .addTransferHistory}).</li>
 *   <li><b>dateRecordNavResolution</b> ({@code NavigationHandler.resolveReceiverRType} gm-aware
 *       overload + an {@code REnumValueRef} disguised {@code head->feature} chain branch) — a
 *       {@code -> date} nav whose receiver navigates to a transitive-fpml {@code zonedDateTime} now
 *       fires the record form {@code .<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))} (was the
 *       non-compiling {@code .map("getDate", zonedDateTime -> zonedDateTime.getDate())} —
 *       {@code ZonedDateTime} has no {@code getDate()}). Anchors: {@code MapFraCalculationPeriodDates}
 *       (plain ctor-pair), {@code MapDateWithId} (the {@code withMetaArgument} sub-compile — proves the
 *       gm threads through {@code ConstructionHandler.tryTypedWithMeta}).</li>
 *   <li><b>enumArgSwitch</b> ({@code FunctionExpressionRenderer.renderEnumSwitchAssignment}) — a switch
 *       over an ENUM argument renders the upstream {@code <arg> == <EnumType>.<CONST>} if-else-if
 *       assignment ladder; the dormant M7b-1 {@code ControlFlowHandler.handle(RSwitchExpr)} stub
 *       emitted the broken {@code Objects.equals(<bareGuard>, MapperS.of(<arg>)) ? ... : null.get()}
 *       ternary. Anchor: {@code UpdateAmount} ({@code direction == QuantityChangeDirectionEnum.INCREASE}).</li>
 *   <li><b>ctorIteEffElse</b> ({@code ControlFlowHandler.hoistAsItemLocalOrNull} drops the
 *       {@code hasEffectiveElse} decline) — a SINGLE-cardinality effective-else conditional hoists the
 *       {@code final <T> ifThenElseResult; if(..){=A;} else if(..){=B;} else {=null;}} ladder (#181 did
 *       only the no-else initializer form; the MULTI list-seat {@code Collections.emptyList()} form
 *       stays declined). Anchor: {@code MapNextEvent}.</li>
 * </ul>
 */
class FunctionPr203BundleTest {

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

    /** typedAddSegment — single value into a MULTI segment: {@code after.addObservationHistory(....getMulti())}. */
    @Test
    @EnabledIf("cellsAvailable")
    void createObservation_cdm6_typedAddSegment_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/event/common/functions/Create_Observation.java",
                "the typed `.add<Leaf>(value.getMulti())` segment-ADD chain "
                + "(renderAddSegmentChainOrNull) reverts to the non-compiling `addAll(toBuilder(..))`");
    }

    /** typedAddSegment — a mapC value: {@code transfer.addTransferHistory(....getMulti())}. */
    @Test
    @EnabledIf("cellsAvailable")
    void createTransfer_cdm6_typedAddSegment_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/event/common/functions/Create_Transfer.java",
                "the typed segment-ADD chain reverts to the non-compiling `addAll(toBuilder(..))`");
    }

    /** dateRecordNavResolution — a disguised-chain receiver to a transitive-fpml zonedDateTime. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapFraCalculationPeriodDates_cdm6_dateRecordNav_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/ingest/fpml/confirmation/product/fra/functions/MapFraCalculationPeriodDates.java",
                "the `.<Date>map(\"Date\", zdt -> Date.of(zdt.toLocalDate()))` record form reverts to the "
                + "non-compiling `.map(\"getDate\", zonedDateTime -> zonedDateTime.getDate())` "
                + "(resolveReceiverRType gm-aware / REnumValueRef branch removed)");
    }

    /** dateRecordNavResolution — the date nav compiled inside the convertToMeta withMetaArgument sub-compile. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapDateWithId_cdm6_dateRecordNavInWithMeta_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/ingest/fpml/confirmation/datetime/functions/MapDateWithId.java",
                "the date record nav inside the withMetaArgument sub-compile reverts to the getter form");
    }

    /** enumArgSwitch — `direction == QuantityChangeDirectionEnum.INCREASE` if-else-if ladder. */
    @Test
    @EnabledIf("cellsAvailable")
    void updateAmount_cdm6_enumArgSwitch_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/base/math/functions/UpdateAmount.java",
                "the `<arg> == <EnumType>.<CONST>` switch ladder (renderEnumSwitchAssignment) reverts to "
                + "the dormant M7b-1 `Objects.equals(<bareGuard>, MapperS.of(<arg>)) ? ... : null.get()` stub");
    }

    /** ctorIteEffElse — a single-card effective-else conditional hoists the `final <T> …; if/else-if/else` ladder. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapNextEvent_cdm6_ctorIteEffElse_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/ingest/fpml/confirmation/workflowstep/functions/MapNextEvent.java",
                "the effective-else hoist ladder reverts to the inline `getOrDefault(false) ? ... : ...get()` "
                + "ternary (hasEffectiveElse decline restored in hoistAsItemLocalOrNull)");
    }

    private static void assertByteMatchesGolden(String path, String revertHint) throws IOException {
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — " + revertHint + ".");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
