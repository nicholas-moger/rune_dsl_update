package com.regnosys.rosetta.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.testutil.ChaosCatalogue;
import com.regnosys.rosetta.testutil.CorpusCatalogue;

/**
 * v3.2 SEAT 10 (PR #631, D49 — the generation-2 chaos cell) — THE SEAT SUITE, parser side: the
 * corpus-free witnesses of the harness laws the {@code chaos-1.0.0 → chaos-1.1.0} swap added.
 * Every law here is otherwise exercised only through a corpus gate that reads the LIVE cell
 * ({@code ChaosForkDiagnosticsGateTest}), whose verdict on a mutation is "the gate is red" — these
 * name the law that broke.
 *
 * <ul>
 *   <li><b>a1–a3 the validator-grain band contract</b> ({@link ChaosParseExpectations#bandRows()},
 *       {@link ChaosParseExpectations#isBandFile(String)}): the base-only band
 *       ({@code chaos-s9N-base.rosetta}) is the ONLY population the validator-grain verdicts may
 *       name; the committed tsv's ten band rows partition exactly as the gate's own {@code [BAND]}
 *       print did at the swap (fills-f5, local); and the parse grain and the band grain read ONE
 *       file through two DISJOINT filters — the parse grain is the a5bom set, the band grain the
 *       s9x set, the tsv's remaining verdicts belong to neither reader.</li>
 *   <li><b>a4–a7 the one-read helper</b> ({@link ChaosCatalogue}): the cell is DERIVED from the
 *       corpus SOT's one chaos row (the oracle dump's name and the sources root derive from it —
 *       the next version cut is a TSV edit), and the one-row law refuses zero or two chaos rows
 *       rather than picking one (a5–a7 over synthetic catalogues through {@code cellOf}).</li>
 * </ul>
 *
 * <p><b>a8–a10 (round 1, cq MF-1)</b>: the refusal leg's pure halves — a row declares EVERY error of its
 * file ({@code ||}-separated) and the live set must match it whole, each substring on a distinct message.
 *
 * <p>The lane record (target/v32-seat10-instruments/lanes-s10.py, local): the band verdict flipped
 * → a2 + the gate; {@code isBandFile} declining everything → a1, a2, a3 + the gate; the one-row law
 * short-circuited → a5, a6 alone; s93's second declared error dropped → the gate alone.
 */
class ChaosBandSeatTest {

    private static CorpusCatalogue.Cell cell(String corpus, String version) {
        return new CorpusCatalogue.Cell(corpus, version, true, true, "9.83.0", List.of(), List.of());
    }

    private static TreeSet<String> bandNames() {
        TreeSet<String> names = new TreeSet<>();
        for (int n = 0; n <= 9; n++) {
            names.add("chaos-s9" + n + "-base.rosetta");
        }
        return names;
    }

    @Test
    void a1_isBandFile_admitsTheBaseOnlyBandShapeAlone() {
        for (String band : bandNames()) {
            assertTrue(ChaosParseExpectations.isBandFile(band), band);
        }
        for (String notBand : List.of(
                "chaos-s09-base.rosetta",        // a gen-1 family's base (s09 is not s9N)
                "chaos-s90-a1o1.rosetta",        // a band family under an axis (axes.tsv expands none)
                "chaos-s9-base.rosetta",         // one digit short
                "chaos-s9x-base.rosetta",        // a non-digit where the family digit sits
                "chaos-s90-base.rosett",         // the wrong extension
                "aaa-chaos-s28-a7first.rosetta", // the A7 order-of-load file
                "zz-chaos-s28-a7last.rosetta",
                "chaos-s34-a5bom.rosetta")) {    // the last parse-grain refusal
            assertFalse(ChaosParseExpectations.isBandFile(notBand), notBand);
        }
    }

    @Test
    void a2_bandRows_areTheTenCommittedBandVerdicts_partitionedAsTheGatePrinted() {
        Map<String, String[]> rows = ChaosParseExpectations.bandRows();
        assertEquals(bandNames(), new TreeSet<>(rows.keySet()), "the ten band files, one row each");
        Map<String, TreeSet<String>> byVerdict = new TreeMap<>();
        rows.forEach((name, row) -> byVerdict.computeIfAbsent(row[0], k -> new TreeSet<>()).add(name));
        // The partition the gate PRINTED at the swap ([BAND] lines, fills-f5.status — local), never typed:
        // s93 / s97 refuse with the released validator's message (parity), s90 / s91 / s92 / s94 / s96 are
        // the fork's DECLARED acceptances of shapes the released plugin refuses (the banked parser /
        // resolver / validator seats), s95 / s98 / s99 both validators admit.
        Map<String, TreeSet<String>> expected = new TreeMap<>();
        expected.put(ChaosParseExpectations.FORK_ACCEPTS_DIVERGENT, new TreeSet<>(List.of(
                "chaos-s90-base.rosetta", "chaos-s91-base.rosetta", "chaos-s92-base.rosetta",
                "chaos-s94-base.rosetta", "chaos-s96-base.rosetta")));
        expected.put(ChaosParseExpectations.FORK_REFUSES_VALIDATION, new TreeSet<>(List.of(
                "chaos-s93-base.rosetta", "chaos-s97-base.rosetta")));
        expected.put(ChaosParseExpectations.FORK_ACCEPTS_PARITY, new TreeSet<>(List.of(
                "chaos-s95-base.rosetta", "chaos-s98-base.rosetta", "chaos-s99-base.rosetta")));
        assertEquals(expected, byVerdict,
                "the band verdicts must partition as the gate printed at the swap; a movement is a"
                        + " re-adjudicated row (LAW 81), never an edit here");
        for (Map.Entry<String, String[]> e : rows.entrySet()) {
            assertFalse(e.getValue()[1].isBlank(), e.getKey() + ": every band row names its shape and banking");
        }
    }

    @Test
    void a3_theParseGrainAndTheBandGrainReadOneFileThroughDisjointFilters() throws IOException {
        Map<String, String> parse = ChaosParseExpectations.expectedRefusals();
        Map<String, String[]> band = ChaosParseExpectations.bandRows();
        for (String name : parse.keySet()) {
            assertFalse(ChaosParseExpectations.isBandFile(name), "a parse-grain row on a band file: " + name);
        }
        for (String name : band.keySet()) {
            assertFalse(parse.containsKey(name), "a band file in the parse grain: " + name);
        }
        // The parse grain IS the a5bom set (LAW 73: the set, both directions).
        assertEquals(ChaosCoverageCensusTest.expectedBomSet(), new TreeSet<>(parse.keySet()));
        // ONE file, TWO filters: every tsv row whose verdict is a parse-grain or band-grain verdict is
        // read by exactly that reader; the remaining verdicts (an element refusal, an emits-as-upstream
        // row) are read by neither — counted here so a new verdict cannot silently join a reader.
        Map<String, Integer> verdictCensus = new TreeMap<>();
        for (String raw : Files.readAllLines(ChaosParseExpectations.FORK_DIAGNOSTICS_TSV, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            verdictCensus.merge(f[1], 1, Integer::sum);
        }
        assertEquals(parse.size(), verdictCensus.getOrDefault("FORK-REFUSES", 0), "the parse grain reads every FORK-REFUSES row");
        int bandVerdictRows = verdictCensus.getOrDefault(ChaosParseExpectations.FORK_ACCEPTS_DIVERGENT, 0)
                + verdictCensus.getOrDefault(ChaosParseExpectations.FORK_REFUSES_VALIDATION, 0)
                + verdictCensus.getOrDefault(ChaosParseExpectations.FORK_ACCEPTS_PARITY, 0);
        assertEquals(band.size(), bandVerdictRows, "the band grain reads every validator-grain row");
        TreeSet<String> neither = new TreeSet<>(verdictCensus.keySet());
        neither.removeAll(List.of("FORK-REFUSES", ChaosParseExpectations.FORK_ACCEPTS_DIVERGENT,
                ChaosParseExpectations.FORK_REFUSES_VALIDATION, ChaosParseExpectations.FORK_ACCEPTS_PARITY));
        assertEquals(new TreeSet<>(List.of("FORK-EMITS-AS-UPSTREAM", "FORK-REFUSES-ELEMENT")), neither,
                "the verdicts NEITHER reader consumes (a new one here is a reader question, not a silent skip)");
    }

    @Test
    void a4_chaosCatalogue_derivesTheCellFromTheOneRow_theOracleDumpAndTheSourcesRoot() throws IOException {
        CorpusCatalogue.Cell c = ChaosCatalogue.cell();
        assertEquals("chaos", c.corpus());
        assertTrue(c.active() && c.catalogue(), "the chaos row is an active catalogue cell");
        assertEquals("chaos-" + c.version(), c.dirName());
        assertEquals(CorpusCatalogue.cellDir(c), ChaosCatalogue.root(), "the root is the SOT's derived cell dir");
        assertEquals(ChaosCatalogue.root().resolve("rosetta-source").resolve("src").resolve("main").resolve("rosetta"),
                ChaosCatalogue.sources(), "the charter § 2.1 layout");
        // The oracle dump's name derives from the same row (ChaosResolutionConformanceTest's read):
        // chaos-<dirName>-upstream-9.83.0.tsv is committed beside the L2 set for the ACTIVE cell.
        Path dump = com.regnosys.rosetta.testutil.CorpusWalker.repoRoot()
                .resolve("rune-parser").resolve("src").resolve("test").resolve("resources")
                .resolve("resolution-conformance").resolve(c.dirName() + "-upstream-9.83.0.tsv");
        assertTrue(Files.isRegularFile(dump), "the active cell's committed oracle dump: " + dump);
        assumeTrue(Files.isDirectory(ChaosCatalogue.sources()), "chaos corpus cell absent");
        try (Stream<Path> s = Files.list(ChaosCatalogue.sources())) {
            assertTrue(s.anyMatch(p -> p.getFileName().toString().endsWith(".rosetta")),
                    "the derived sources root holds the cell's .rosetta files");
        }
    }

    @Test
    void a8_declaredErrorSubstrings_splitsOnTheDoubleBar_refusesBlank() {
        assertEquals(List.of("Enum 'eth' not found"), ChaosParseExpectations.declaredErrorSubstrings("Enum 'eth' not found"));
        assertEquals(List.of("a", "b", "c"), ChaosParseExpectations.declaredErrorSubstrings("a || b || c"));
        assertThrows(IllegalStateException.class, () -> ChaosParseExpectations.declaredErrorSubstrings("a || "));
    }

    @Test
    void a9_declaredErrorsMismatch_matchesTheWholeSet_eachSubstringOnADistinctMessage() {
        assertEquals(null, ChaosForkDiagnosticsGateTest.declaredErrorsMismatch(
                List.of("eth", "C93Either"), List.of("Type 'C93Either' not found", "Enum 'eth' not found")));
        // the count law: an UNDECLARED second error is a mismatch, never absorbed
        assertTrue(ChaosForkDiagnosticsGateTest.declaredErrorsMismatch(
                List.of("eth"), List.of("Enum 'eth' not found", "another")).contains("declares 1 error(s)"));
        // a declared error the fork no longer reports (a heal) is a mismatch too
        assertTrue(ChaosForkDiagnosticsGateTest.declaredErrorsMismatch(
                List.of("eth", "gone"), List.of("Enum 'eth' not found", "another")).contains("'gone'"));
    }

    @Test
    void a10_declaredErrorsMismatch_twoSubstringsCannotShareOneMessage() {
        assertTrue(ChaosForkDiagnosticsGateTest.declaredErrorsMismatch(
                List.of("eth", "eth"), List.of("Enum 'eth' not found", "another")).contains("'eth'"));
    }

    @Test
    void a5_cellOf_refusesZeroChaosRows() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ChaosCatalogue.cellOf(List.of(cell("cdm", "6.20.6"), cell("drr", "7.3.0"))));
        assertTrue(e.getMessage().contains("found 0"), e.getMessage());
    }

    @Test
    void a6_cellOf_refusesTwoChaosRows() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ChaosCatalogue.cellOf(List.of(cell("chaos", "1.0.0"), cell("cdm", "6.20.6"), cell("chaos", "1.1.0"))));
        assertTrue(e.getMessage().contains("found 2"), e.getMessage());
    }

    @Test
    void a7_cellOf_returnsTheOneChaosRow() {
        CorpusCatalogue.Cell c = ChaosCatalogue.cellOf(List.of(cell("cdm", "6.20.6"), cell("chaos", "1.1.0"), cell("drr", "7.3.0")));
        assertEquals("chaos-1.1.0", c.dirName());
        assertEquals("chaos/chaos-1.1.0", c.relPath());
    }
}
