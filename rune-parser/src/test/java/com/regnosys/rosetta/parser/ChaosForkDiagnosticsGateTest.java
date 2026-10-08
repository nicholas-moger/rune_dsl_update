package com.regnosys.rosetta.parser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * THE L4 STANDING FORK-DIAGNOSTICS GATE (v3.2 PR-2, charter § 4b): the chaos cell's
 * parse-level contract, re-taken LIVE at every suite run.
 *
 * <p>PR-1 measured BOM refusal parity (the fork parser refuses the SAME 22 a5bom
 * files upstream refuses) and asserted it as a set inside the coverage census.
 * This test makes the committed per-file data —
 * {@code scripts/chaos-expander/expectations/fork-diagnostics.tsv} — a SUITE GATE
 * over the committed CELL ({@code test-corpus/chaos/chaos-<version>} — the catalogue's one chaos row,
 * {@code chaos-1.1.0} since v3.2 seat 10), in both
 * directions (LAW 73 — the SET, not the count):
 * <ul>
 *   <li><b>No healed refusal</b>: every expected-refusal file still refuses, with
 *       the pinned diagnostic substring (LAW 81 — a control pinned to a refusal
 *       set fires when the refusals heal; a heal is a verdict change to
 *       re-adjudicate, never silently absorb).</li>
 *   <li><b>No silent degradation</b>: every OTHER chaos file parses CLEAN — a new
 *       refusal is a fork parser regression against admitted-valid input, reported
 *       by name.</li>
 * </ul>
 *
 * <p>The three committed statements of the refusal set — the tsv rows, the census
 * pin ({@code ChaosCoverageCensusTest.expectedBomSet()}), and the live verdicts —
 * are asserted to be ONE set, so none can drift from the others (Rule 3 by
 * mechanical reconciliation rather than by deduplication: the tsv carries the
 * per-file diagnostic DATA, the census pin carries the shape, and each polices
 * the other).
 *
 * <p>The instrument can fail (the positive-control law): set equality is
 * two-sided, so deleting a BOM file, healing one, or a new refusal each flips a
 * distinct assert — no mutation lane needed beyond the set laws themselves.
 *
 * <p>Corpus-gated: the chaos cell is COMMITTED, but the corpus tree as a whole is
 * local-only — a fresh clone HAS the cell, so this gate only skips when the whole
 * {@code test-corpus/} is absent (it is committed content; treat absence like any
 * other missing checkout piece).
 */
class ChaosForkDiagnosticsGateTest {

    /** The committed chaos cell's source root (charter § 2.1 layout), from the corpus SOT through
     *  ONE read (v3.2 seat 10, D49 — the chaos-1.0.0 -> chaos-1.1.0 cut). */
    private static final Path CHAOS_SOURCES = com.regnosys.rosetta.testutil.ChaosCatalogue.sources();

    /** The cell's pinned source population (the expander cap receipt; census-pinned too):
     *  606 at chaos-1.0.0, 988 at chaos-1.1.0 (44 seeds, D49). */
    private static final int CHAOS_CELL_FILES = 988;

    /**
     * The COMMITTED CELL's byte gate (the spec review's MF-2): the charter § 2.1/§ 3
     * dropped a chaos manifest because "the expander-determinism diff is the drift
     * gate" — but the coverage census digests the gitignored SINK, so until this pin
     * a hand-edited committed source passed every gate (and a fresh clone, where the
     * census assumption-skips, checked nothing). This is the SAME digest algorithm
     * over the SAME population ({@link ChaosCoverageCensusTest#sinkDigest} — sorted
     * name\0bytes\0 pairs, first 16 hex), taken over the CELL instead: cell == sink
     * == the expander's output, or this gate fails on whichever copy drifted.
     */
    private static final String CHAOS_CELL_DIGEST = "313bfac85dbf843e";

    @Test
    void committedRefusalDataAndCensusPinAgree() {
        // The tsv's FORK-REFUSES filenames and the census's programmatic pin must be
        // the same set — the reconciliation that lets both exist without drifting.
        assertEquals(ChaosCoverageCensusTest.expectedBomSet(),
                new TreeSet<>(ChaosParseExpectations.expectedRefusals().keySet()),
                "fork-diagnostics.tsv FORK-REFUSES rows and ChaosCoverageCensusTest.expectedBomSet()"
                        + " disagree — the committed § 4b data has drifted from the census pin");
    }

    /**
     * The § 4b class-file reconciliation (the spec review's SF-7): the golden-free
     * register ({@code chaos-golden-free.txt} — the noGolden CLASS the baseline's
     * format cannot carry per row), read cross-module by path (rune-java-generator
     * publishes no test-jar).
     *
     * <p>RE-CUT at v3.2 seat 9 (PR #630, F13 / D48). Until the adjudication the register
     * had to be a SUBSET of the expected-divergence baseline (the baseline's noGolden rows
     * were what D11 tolerated; the register only named the class). D48 makes the register
     * the ASSERTED expected-noGolden set itself: {@code D11CorpusRegressionTest} asserts,
     * per chaos kind, that the fork's noGolden files EQUAL the register's rows of that kind
     * (both directions — a row the fork stops emitting or that gains a golden is a stale
     * row), so the baseline no longer carries noGolden rows at all and the two sets are
     * DISJOINT: the baseline holds the two element REFUSALS (p2's {@code C17Sift} and
     * {@code C17BooksC17Agree}, the E2 wrong-enum echo the fork now refuses at
     * {@code ENUM_VALUE_NAME_ECHO} where the released plugin refuses p2 whole) and the
     * register p2's seven still-golden-free files; p1's eight left the register when the
     * chaos pin script stopped excluding split PARTNERS (the released plugin defines p1's
     * goldens when it judges p1 alone; the fork is byte-identical on all eight). A row in
     * BOTH files would be tolerated twice under two different laws.
     */
    @Test
    void goldenFreeRowsAreDisjointFromTheDeclaredDivergenceRows() throws IOException {
        Path base = com.regnosys.rosetta.testutil.CorpusWalker.repoRoot()
                .resolve("rune-java-generator").resolve("src").resolve("test")
                .resolve("resources");
        java.util.Set<String> baselinePaths = new TreeSet<>();
        for (String raw : Files.readAllLines(base.resolve("chaos-expected-divergence.txt"))) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int colon = line.indexOf(':');
            assertTrue(colon > 0 && colon < line.length() - 1,
                    "malformed baseline row: " + line);
            baselinePaths.add(line.substring(colon + 1));
        }
        java.util.List<String> goldenFree = new ArrayList<>();
        for (String raw : Files.readAllLines(base.resolve("chaos-golden-free.txt"))) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            assertTrue(goldenFree.isEmpty() || !goldenFree.contains(line),
                    "duplicate golden-free row: " + line);
            goldenFree.add(line);
        }
        // chaos-1.1.0 (v3.2 seat 10, D49): the register is the D48 union over the GEN-2 cell — the
        // four rival-enum split p2 halves (s17 / s31, x36 and their A1xA6 x16 twins: the released
        // plugin refuses p2 WHOLE, the fork's per-element licence emits the clean elements) and the
        // base-only band's fork-emitted files (s90–s97 the released plugin REFUSES and the fork
        // accepts — DECLARED FORK-ACCEPTS-DIVERGENT rows; s98 the generator CRASH the fork emits
        // through — the § 4b two-oracles class). 111 rows at the first gen-2 census (d11-m1, both
        // routes, 2026-09-10); a movement is a measurement, never an edit.
        // v3.2 seat 12 (PR #633, D52 - COUNTERS FIRST): 111 -> 107. The four s31 split-p2 function files
        // (C31Which / C31Keys at x16enum / x36enum p2) had been emitted SILENTLY with the switch ternary the
        // gen-2 census never counted (it counted byte rows; these had no golden); the SWITCH_TERNARY_STUB
        // counter REFUSES them now, so they left this register for the baseline as refusal rows - the union
        // of the two files UNMOVED (the D48 move of #630 in the other direction is the precedent). Read from
        // the D11's gen-error dump at the counter tree (recut-declared-s12.py, both routes asserted equal).
        assertEquals(107, goldenFree.size(),
                "the golden-free register's population is pinned (the first gen-2 census, D49:"
                        + " 30 split-p2 rows + 69 band rows + 12 s98 rows = 111; 107 since v3.2 seat 12's"
                        + " counters re-classified four s31 p2 rows as refusals); a movement is an"
                        + " adjudication or a re-measured census, never an edit");
        for (String row : goldenFree) {
            assertTrue(!baselinePaths.contains(row),
                    "golden-free row is ALSO a declared divergence row — it would be tolerated"
                            + " twice, by the register's noGolden law AND the baseline's: " + row);
            assertTrue(row.startsWith("chaos/s17/x36enum/p2/") || row.startsWith("chaos/s17/x16enum/p2/")
                            || row.startsWith("chaos/s31/x36enum/p2/") || row.startsWith("chaos/s31/x16enum/p2/")
                            || row.matches("chaos/s9[0-8]/base/.*"),
                    "golden-free row outside the adjudicated populations (the four split p2 halves"
                            + " and the s9x band): " + row);
        }
        // The baseline: the first gen-2 census's 611 rows = 524 byte mismatches + 86 refusal rows
        // + the one silent METAFIELD missing output (the global ReferenceWithMetaVoid), every row
        // under its mechanism-family comment; SHRINK-ONLY from here (the second round's meter).
        // v3.2 seat 11 (PR #632, M1 / D50): 611 -> 506 — the M1 block's 105 rows (chaos/s32's List-named
        // type: five kinds × 21 placement variants) healed on BOTH routes by the D11's now-matching print
        // at abee82e37 and DELETED at commit 4 (419 byte mismatches + 86 refusal rows + 1). This pin is
        // the parser's copy of the meter, moved WITH the heal — the seat's c8 verify gate caught it RED
        // before any chain JVM, the heal recorded exactly as the message prescribes.
        // v3.2 seat 12 (PR #633, D52): 506 -> 510 - NOT a regression and NOT a heal: the +4 are the golden-free
        // register's -4 above (four silent s31 p2 emissions made LOUD refusal rows), the byte / refusal split
        // inside the baseline 419 / 86 -> 294 / 215 as the three register sites landed before any fix. A
        // larger count is a regression ONLY when the two files' UNION grows; the union is 617 = 617 here.
        // v3.2 seat 12, H1 (M12 the duplicate closure parameter, healed): 510 -> 509 - the one M12 row (C95Reduce)
        // now-matching on BOTH routes at the H1 head (recut-healed-s12.py from the D11's FLIP-OUT print).
        // v3.2 seat 12, H2 (M8a the meta wrapper into a rule's plain output, healed): 509 -> 497 - the twelve M8a rows
        // (C28VenueRule x12) now-matching on BOTH routes at the H2 head (recut-healed-s12.py from the D11's FLIP-OUT print).
        // v3.2 seat 12, H3 (M6 the `item ->` only-exists root, healed): 497 -> 472 - the twenty-five M6 rows
        // (C25PathsC25Item x16 + C25Only x9) now-matching on BOTH routes at the H3 head (recut-healed-s12.py from the
        // D11's FLIP-OUT print).
        // v3.2 seat 13, commit 4 (D53 - the closing seat's heals): 472 -> 370 - the 102 rows healed on BOTH routes at
        // the commit-4 tree (C27NatCalled x18, C23CmpArm x13, C23LongAlias x13, C23LongOut x13, C23WholeSet x20,
        // C29InLambda x12, C31Defaults x13 - recut-healed-s13.py from the D11's FLIP-OUT print, the offload run
        // of 2026-09-14 copied to target/v32-seat13-instruments/scratch/ds13c4g-off.log / -on.log, local); the four
        // register sites of the same commit moved 76 rows from byte to refusal WITHIN the count (370 = 53 + 316 + 1).
        assertEquals(370, baselinePaths.size(),
                "the expected-divergence baseline holds the second round's declared rows (611 at the"
                        + " gen-2 swap, D49; 506 since v3.2 seat 11's M1 heal; 510 since v3.2 seat 12's"
                        + " counters took four rows from the golden-free register; 509 since its H1 heal;"
                        + " 497 since its H2 heal; 472 since its H3 heal; 370 since v3.2 seat 13's commit-4"
                        + " heals took 102 rows); a smaller count is a"
                        + " heal to record, a larger one a regression unless the golden-free register"
                        + " shrank by the same rows");
        assertTrue(baselinePaths.contains("chaos/s17/x36enum/p2/functions/C17Sift.java")
                        && baselinePaths.contains("chaos/s17/x36enum/p2/validation/datarule/C17BooksC17Agree.java")
                        && baselinePaths.contains("chaos/s17/x16enum/p2/functions/C17Sift.java")
                        && baselinePaths.contains("chaos/s17/x16enum/p2/validation/datarule/C17BooksC17Agree.java"),
                "the D48 refusal rows (x36enum p2 and its x16enum twin): " + baselinePaths.size());
    }

    @Test
    void liveParseVerdictsMatchTheCommittedContract() throws IOException {
        assumeTrue(Files.isDirectory(CHAOS_SOURCES),
                "chaos cell not present at " + CHAOS_SOURCES + " — checkout incomplete");

        List<Path> files = new ArrayList<>();
        try (var walk = Files.walk(CHAOS_SOURCES)) {
            walk.filter(p -> p.toString().endsWith(".rosetta")).forEach(files::add);
        }
        files.sort(Path::compareTo);
        assertEquals(CHAOS_CELL_FILES, files.size(),
                "chaos cell population moved — the committed cell must carry the expander's"
                        + " admitted file set exactly");
        assertEquals(CHAOS_CELL_DIGEST, ChaosCoverageCensusTest.sinkDigest(files),
                "the COMMITTED cell's bytes have drifted from the pinned expander digest —"
                        + " the verified-copy law is broken: re-run"
                        + " python scripts/chaos-expander/expand.py, re-verify, and only"
                        + " re-ratify the pin for a deliberate, reviewed seed change");

        Map<String, String> expected = ChaosParseExpectations.expectedRefusals();
        TreeSet<String> liveRefusals = new TreeSet<>();
        List<String> unexpectedRefusalDetails = new ArrayList<>();
        List<String> wrongDiagnostic = new ArrayList<>();
        for (Path file : files) {
            RosettaParseResult result = RosettaParserFacade.parseFile(file);
            String name = file.getFileName().toString();
            if (result.errors().isEmpty()) {
                continue;
            }
            liveRefusals.add(name);
            String required = expected.get(name);
            if (required == null) {
                unexpectedRefusalDetails.add(name + ": " + result.errors().get(0));
            } else if (!result.errors().get(0).contains(required)) {
                wrongDiagnostic.add(name + ": wanted a first error containing '" + required
                        + "', got: " + result.errors().get(0));
            }
        }

        // Both directions of the set law, reported by name (the loud-findings order:
        // a NEW refusal — a fork regression against admitted-valid input — first).
        assertTrue(unexpectedRefusalDetails.isEmpty(),
                "FORK PARSER REGRESSION: admitted-valid chaos files now REFUSED ("
                        + unexpectedRefusalDetails.size() + "):\n  "
                        + String.join("\n  ", unexpectedRefusalDetails));
        assertBandContractHolds(files);
        assertEquals(new TreeSet<>(expected.keySet()), liveRefusals,
                "the live refusal set is not the committed § 4b set — a missing member is a"
                        + " HEALED refusal (re-adjudicate the tsv row; LAW 81), an extra one a"
                        + " new refusal");
        assertTrue(wrongDiagnostic.isEmpty(),
                "expected-refusal files refused with a DIFFERENT diagnostic ("
                        + wrongDiagnostic.size() + "):\n  " + String.join("\n  ", wrongDiagnostic));
    }

    /**
     * THE VALIDATOR-GRAIN BAND CONTRACT (chaos-1.1.0, v3.2 seat 10 — D49): the base-only band
     * ({@code chaos-s9N-base.rosetta}, one shape per file — the shapes the released 9.83.0 plugin
     * REFUSES, or admits and crashes on) is judged by the fork's own front end exactly as the
     * released plugin judged it: ALONE — the builtins plus that one file (the pin script's
     * {@code --judge-alone} staging, {@code judged-alone.tsv}) — and the verdict (parse, link and
     * validation diagnostics at ERROR severity, the builtins' own never counted) must match the
     * file's committed row of {@code fork-diagnostics.tsv} in BOTH directions:
     * <ul>
     *   <li>{@code FORK-REFUSES-VALIDATION} — refusal parity: the row declares EVERY error message of
     *       the file, {@code ||}-separated (one substring per ERROR, in any order), and the live
     *       message set must match it whole — the same count, each substring on a distinct message
     *       (round 1, cq MF-1: the first cut asked for ONE substring among the errors and printed the
     *       first message alone, so s93's second error was absorbed and expressible in no receipt);</li>
     *   <li>{@code FORK-ACCEPTS-DIVERGENT} — a DECLARED divergence (the fork accepts a shape the
     *       released plugin refuses; each row a priced parser / resolver / validator seat, LAW 81's
     *       shrink-only class): ZERO errors, the row's detail naming the shape and its banking;</li>
     *   <li>{@code FORK-ACCEPTS-PARITY} — both validators admit the file (an ORACLE-CLEAN or MOJO-CRASH
     *       row: s95 the duplicate closure parameter, s98 the generator crash, s99 the java.lang shadow):
     *       ZERO errors.</li>
     * </ul>
     * A band file with no row, a row with no file, a declared refusal that heals, an undeclared error
     * beside a declared one, or a declared acceptance that starts refusing all fail loud, with the LIVE
     * verdict of every band file printed ({@code [BAND] file verdict errors=N messages=[...]}, every
     * message) — a row is written from that print, never typed.
     * {@link ChaosParseExpectations#expectedRefusals()} skips these rows by verdict, so the a5bom parse
     * contract above is untouched (one file, two filters).
     */
    private static void assertBandContractHolds(List<Path> cellFiles) throws IOException {
        Map<String, String[]> rows = ChaosParseExpectations.bandRows();
        List<Path> band = new ArrayList<>();
        for (Path p : cellFiles) {
            if (ChaosParseExpectations.isBandFile(p.getFileName().toString())) {
                band.add(p);
            }
        }
        assertTrue(!band.isEmpty(), "the base-only band is absent from the cell — the s9x seeds are gone");
        Path builtinsDir = com.regnosys.rosetta.testutil.CorpusWalker.BUILTINS_DIR;
        List<String> failures = new ArrayList<>();
        TreeSet<String> seen = new TreeSet<>();
        for (Path file : band) {
            String name = file.getFileName().toString();
            seen.add(name);
            List<String> errors = new ArrayList<>();
            RosettaParseResult parsed = RosettaParserFacade.parseFile(file);
            errors.addAll(parsed.errors());
            if (parsed.errors().isEmpty()) {
                // A linked model is FROZEN (RNode.checkMutable), so every judged-alone workspace
                // gets its own fresh builtin models — one build per band file, as the pin script
                // stages one scaffold per file.
                List<com.regnosys.rosetta.ast.model.RModel> models = new ArrayList<>();
                try (var files = Files.list(builtinsDir)) {
                    for (Path f : files.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList()) {
                        models.add(com.regnosys.rosetta.ast.builder.AstBuilder.buildFromString(
                                Files.readString(f), f.toString()));
                    }
                }
                models.add(com.regnosys.rosetta.ast.builder.AstBuilder.buildFromString(
                        Files.readString(file), file.toString()));
                com.regnosys.rosetta.symbols.RLinkingResult linked =
                        com.regnosys.rosetta.symbols.RWorkspace.build(models);
                for (com.regnosys.rosetta.symbols.diagnostics.RDiagnostic d : linked.diagnostics()) {
                    if (d.severity() != com.regnosys.rosetta.symbols.diagnostics.Severity.ERROR) {
                        continue;
                    }
                    String at = d.range() == null || d.range().file() == null ? "" : d.range().file();
                    if (!at.isEmpty() && !at.replace('\\', '/').endsWith("/" + name)) {
                        continue; // a builtin's diagnostic (none expected) is not the band file's
                    }
                    errors.add(d.message());
                }
            }
            String live = errors.isEmpty() ? "ACCEPTS" : "REFUSES";
            System.out.println("[BAND] " + name + " " + live + " errors=" + errors.size()
                    + (errors.isEmpty() ? "" : " messages=" + errors));
            String[] row = rows.get(name);
            if (row == null) {
                failures.add(name + ": NO ROW in fork-diagnostics.tsv (live " + live + ", "
                        + errors.size() + " error(s)" + (errors.isEmpty() ? "" : ": " + errors.get(0)) + ")");
            } else if (ChaosParseExpectations.FORK_REFUSES_VALIDATION.equals(row[0])) {
                List<String> declaredErrors = ChaosParseExpectations.declaredErrorSubstrings(row[1]);
                String mismatch = declaredErrorsMismatch(declaredErrors, errors);
                if (mismatch != null) {
                    failures.add(name + ": declared FORK-REFUSES-VALIDATION " + declaredErrors + " — "
                            + (errors.isEmpty()
                                    ? "the fork ACCEPTS (a healed refusal is a verdict change — LAW 81)"
                                    : mismatch + " — live: " + errors));
                }
            } else if (ChaosParseExpectations.FORK_ACCEPTS_DIVERGENT.equals(row[0])) {
                if (!errors.isEmpty()) {
                    failures.add(name + ": declared FORK-ACCEPTS-DIVERGENT but the fork REFUSES: " + errors
                            + " (parity reached — re-adjudicate the row to FORK-REFUSES-VALIDATION)");
                }
            } else if (ChaosParseExpectations.FORK_ACCEPTS_PARITY.equals(row[0])) {
                if (!errors.isEmpty()) {
                    failures.add(name + ": declared FORK-ACCEPTS-PARITY (the released validator admits it too)"
                            + " but the fork REFUSES: " + errors + " (a NEW fork refusal of an upstream-clean file)");
                }
            } else {
                failures.add(name + ": unknown band verdict " + row[0]);
            }
        }
        for (String declared : rows.keySet()) {
            if (!seen.contains(declared)) {
                failures.add(declared + ": a band row with NO file in the cell (a retired seed leaves its row stale)");
            }
        }
        assertTrue(failures.isEmpty(), "THE BAND CONTRACT (validator grain) is not the committed one:\n  "
                + String.join("\n  ", failures));
    }

    /**
     * {@code null} when the live ERROR messages match the declared substrings WHOLE — the same count, and each
     * declared substring found on a distinct live message (greedy, in declaration order); else the named mismatch.
     * The pure half of the refusal leg (round 1, cq MF-1); {@code ChaosBandSeatTest} a8–a10 witness it corpus-free.
     */
    static String declaredErrorsMismatch(List<String> declaredSubstrings, List<String> liveErrors) {
        if (declaredSubstrings.size() != liveErrors.size()) {
            return "the row declares " + declaredSubstrings.size() + " error(s) where the fork reports "
                    + liveErrors.size() + " (an undeclared error is a finding to declare, a missing one a heal to record)";
        }
        List<String> unmatched = new ArrayList<>(liveErrors);
        for (String sub : declaredSubstrings) {
            int at = -1;
            for (int i = 0; i < unmatched.size(); i++) {
                if (unmatched.get(i).contains(sub)) {
                    at = i;
                    break;
                }
            }
            if (at < 0) {
                return "no live message matches the declared substring '" + sub + "'";
            }
            unmatched.remove(at);
        }
        return null;
    }
}
