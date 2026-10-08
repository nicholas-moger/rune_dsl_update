package com.regnosys.rosetta.symbols.conformance;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.parser.ChaosParseExpectations;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * THE CHAOS L2 GATE (v3.2 PR-2, charter § 4/§ 6): the fork's name bindings against
 * upstream 9.83.0's over the whole chaos cell — the same differential as
 * {@link ResolutionConformanceTest}, pointed at the committed chaos oracle dump.
 *
 * <p><b>The upstream side</b> is {@code chaos-<version>-upstream-9.83.0.tsv}: the
 * released 9.83.0 oracle over the admitted sink — at chaos-1.0.0 {@code records=11725}
 * (regenerated 2026-09-02, determinism-checked ×2, the XREF section byte-identical to the
 * PR-1 admission run l1-run13); at chaos-1.1.0 (v3.2 seat 10, D49) {@code records=23875}, the
 * seat's own admission dump l1-s10-run7 whose run8 twin is byte-identical WHOLE (round 1, cq SF-2: the
 * first cut had named the 43-seed lab's run5 / 23902, superseded at the re-admission cycle). Regenerate with:
 * <pre>
 * python scripts/chaos-expander/expand.py
 * scripts/xtext-oracle/run-oracle.sh \
 *     --out rune-parser/src/test/resources/resolution-conformance/chaos-&lt;version&gt;-upstream-9.83.0.tsv \
 *     target/chaos-expander/work
 * </pre>
 *
 * <p><b>The fork side</b> is the SAME cell sources (the committed cell is the
 * verified copy of the sink) minus the 34 § 4b expected-refusal files (22 at chaos-1.0.0) — which carry
 * no XREF rows on the oracle side either (both pipelines refuse them; refusal
 * parity is {@code ChaosForkDiagnosticsGateTest}'s gate), so the exclusion is
 * population-consistent, not scope-narrowing.
 *
 * <p><b>Scope</b> is inherited, stated not implied (charter § 4b): only
 * {@link ForkResolutionDump#IN_SCOPE_REFS} — the three C1 reference kinds — on BOTH
 * sides identically. Widening is a chartered v3.2-or-later decision.
 *
 * <p><b>The declared-differences file</b> ({@code chaos-l2-expected-differences.txt})
 * plays the same role as the D11 chaos expected-divergence baseline: every row is a
 * MEASURED disagreement of the first differential run, classified in the census,
 * shrink-only thereafter, deleted at v3.2 close (close bar: differential = 0 at the
 * stated scope). An UNDECLARED difference fails; a declared difference that stops
 * differing fails as STALE (the freeze must never outlive its defect — the
 * {@link ResolutionConformanceTest} exception-staleness law, applied file-wide).
 *
 * <p><b>The positive control</b> (charter § 4, L2 row): a doctored dump row must be
 * reported as exactly itself — proven here by running the SAME comparison logic over
 * a copy of the upstream map with one row's binding rewritten, without touching the
 * committed dump.
 */
class ChaosResolutionConformanceTest {

    private static final Path SUITE = Paths.get("src/test/resources/resolution-conformance");
    // The cell (and so the dump's name, {@code chaos-<version>-upstream-9.83.0.tsv}) from the corpus
    // SOT through ONE read (v3.2 seat 10, D49 — the chaos-1.0.0 -> chaos-1.1.0 cut).
    private static final Path ORACLE_DUMP = SUITE.resolve(
            com.regnosys.rosetta.testutil.ChaosCatalogue.cell().dirName() + "-upstream-9.83.0.tsv");
    private static final Path EXPECTED_DIFFERENCES =
            SUITE.resolve("chaos-l2-expected-differences.txt");
    private static final Path CHAOS_SOURCES = com.regnosys.rosetta.testutil.ChaosCatalogue.sources();

    @Test
    void fork_resolves_every_chaos_name_where_upstream_does_beyond_the_declared_set()
            throws IOException {
        assumeTrue(Files.isDirectory(CHAOS_SOURCES),
                "chaos cell not present at " + CHAOS_SOURCES + " — checkout incomplete");

        Map<String, String> upstream = readOracleDump();
        buildCellOnce();
        Map<String, String> fork = index(ForkResolutionDump.dump(cellModels, SOURCE_BY_FILE_NAME));

        assertFalse(upstream.isEmpty(), "the committed chaos oracle dump has no in-scope records");
        assertFalse(fork.isEmpty(), "the fork produced no in-scope records over the chaos cell");

        Map<String, String> declared = readExpectedDifferences();
        Map<String, String> allDifferences = differences(upstream, fork);
        writeCensusIfAsked(allDifferences, upstream, fork, declared);
        List<String> undeclared = new ArrayList<>();
        Set<String> matchedDeclared = new LinkedHashSet<>();
        for (Map.Entry<String, String> diff : allDifferences.entrySet()) {
            if (declared.containsKey(diff.getKey())) {
                matchedDeclared.add(diff.getKey());
            } else {
                undeclared.add(diff.getValue());
            }
        }

        Set<String> comparedUnion = new TreeSet<>(upstream.keySet());
        comparedUnion.addAll(fork.keySet());
        int comparedKeys = comparedUnion.size();
        assertEquals(List.of(), undeclared,
                undeclared.size() + " of " + comparedKeys + " chaos references resolve differently"
                        + " from upstream 9.83.0 beyond the declared set (" + declared.size()
                        + " rows). Upstream wins every disagreement; a NEW difference is a"
                        + " resolution regression or an undiscovered family — census it, never"
                        + " append it silently.\n" + String.join("\n", undeclared));
        // Shrink-only, staleness-checked: a declared row that no longer differs means
        // the divergence healed — delete the row in the healing seat's own commit.
        assertEquals(new TreeSet<>(declared.keySet()), new TreeSet<>(matchedDeclared),
                "declared L2 difference row(s) no longer differ — STALE; delete them from "
                        + EXPECTED_DIFFERENCES + " in the seat that healed them");
    }

    /**
     * The doctored-row positive control (charter § 4): the differential logic must
     * report a corrupted binding as exactly itself. The corruption is IN-MEMORY on a
     * copy — the committed dump is never touched — and targets the first in-scope
     * key, so the control cannot silently no-op on an empty selection.
     */
    @Test
    void a_doctored_dump_row_is_reported_as_exactly_itself() throws IOException {
        assumeTrue(Files.isDirectory(CHAOS_SOURCES),
                "chaos cell not present at " + CHAOS_SOURCES + " — checkout incomplete");

        Map<String, String> upstream = readOracleDump();
        buildCellOnce();
        Map<String, String> fork = index(ForkResolutionDump.dump(cellModels, SOURCE_BY_FILE_NAME));
        Map<String, String> declared = readExpectedDifferences();

        // Pick a key both sides AGREE on and which is not already declared — the
        // doctoring must CREATE a difference, not shadow an existing one.
        String victim = null;
        for (Map.Entry<String, String> e : upstream.entrySet()) {
            if (!declared.containsKey(e.getKey()) && e.getValue().equals(fork.get(e.getKey()))) {
                victim = e.getKey();
                break;
            }
        }
        assertTrue(victim != null, "no agreeing undeclared key to doctor — the control needs one");

        Map<String, String> doctored = new LinkedHashMap<>(upstream);
        doctored.put(victim, "DoctoredEClass chaos.doctored.Binding");

        // THE SAME comparison the real gate runs (the reviews' shared catch: the
        // control previously re-implemented the diff inline, dropped both null legs,
        // and could pass while the production loop was broken — the exact failure
        // "prove the instrument can fail" exists to prevent).
        List<String> reported = new ArrayList<>();
        for (String key : differences(doctored, fork).keySet()) {
            if (!declared.containsKey(key)) {
                reported.add(key);
            }
        }
        assertEquals(List.of(victim), reported,
                "the doctored row must be reported as exactly itself — a differential that"
                        + " cannot see a planted wrong binding gates nothing (LAW 73's"
                        + " prove-the-instrument-can-fail)");
    }

    /**
     * THE ONE comparison both the gate and its positive control run (the reviews'
     * shared finding): the union of both key sets, null legs included — a key
     * missing on either side IS a difference (the F15 class). Returns
     * {@code key -> rendered difference}, insertion-ordered over the sorted union.
     */
    private static Map<String, String> differences(Map<String, String> upstream,
                                                   Map<String, String> fork) {
        Map<String, String> out = new LinkedHashMap<>();
        Set<String> keys = new TreeSet<>();
        keys.addAll(upstream.keySet());
        keys.addAll(fork.keySet());
        for (String key : keys) {
            String theirs = upstream.get(key);
            String ours = fork.get(key);
            if (theirs == null) {
                out.put(key, key + "\n    upstream  (no such reference)\n    fork      " + ours);
            } else if (ours == null) {
                out.put(key, key + "\n    upstream  " + theirs + "\n    fork      (no such reference)");
            } else if (!theirs.equals(ours)) {
                out.put(key, key + "\n    upstream  " + theirs + "\n    fork      " + ours);
            }
        }
        return out;
    }

    /**
     * THE CENSUS RECEIPT (v3.2 seat 8): with {@code -Dl2.census.dir=<dir>} the gate
     * writes every differing row — declared or not — with BOTH sides rendered
     * ({@code l2-differences.txt}) and a class summary by seed family × reference
     * kind × fork target kind × upstream target kind ({@code l2-census.txt}), so a
     * seat can measure the family it is about to heal at its starting head and
     * re-take the figure at the fix (the measure-first law: a census figure is a
     * hypothesis until the gate prints it). Absent the property nothing is written;
     * the assertions of the gate are untouched either way.
     */
    private static void writeCensusIfAsked(Map<String, String> allDifferences,
                                           Map<String, String> upstream, Map<String, String> fork,
                                           Map<String, String> declared) throws IOException {
        String dir = System.getProperty("l2.census.dir");
        if (dir == null || dir.isBlank()) {
            return;
        }
        Path out = Paths.get(dir);
        Files.createDirectories(out);
        List<String> rows = new ArrayList<>();
        Map<String, Integer> classes = new java.util.TreeMap<>();
        for (Map.Entry<String, String> diff : allDifferences.entrySet()) {
            String key = diff.getKey();
            rows.add((declared.containsKey(key) ? "DECLARED   " : "UNDECLARED ") + diff.getValue());
            // key form: <file>@<offset> <reference kind> '<text>'; the seed family is the
            // file name's second dash-separated token (chaos-s05-a1o1.rosetta -> s05)
            String file = key.substring(0, key.indexOf('@'));
            String[] fileParts = file.split("-");
            String seed = fileParts.length > 1 ? fileParts[1] : file;
            String refKind = key.substring(key.indexOf(' ') + 1, key.indexOf(" '"));
            String theirs = upstream.get(key);
            String ours = fork.get(key);
            String cls = seed + " | " + refKind
                    + " | fork=" + (ours == null ? "(no record)" : ours.substring(0, ours.indexOf(' ')))
                    + " | upstream=" + (theirs == null ? "(no record)" : theirs.substring(0, theirs.indexOf(' ')));
            classes.merge(cls, 1, Integer::sum);
        }
        Files.write(out.resolve("l2-differences.txt"), rows);
        Set<String> compared = new TreeSet<>(upstream.keySet());
        compared.addAll(fork.keySet());
        List<String> summary = new ArrayList<>();
        long declaredDiffering = allDifferences.keySet().stream().filter(declared::containsKey).count();
        // the two declared counts are distinct facts: the declared FILE's row count and the
        // number of differing rows that file covers (equal iff nothing is stale) — labelled apart
        // since the round-1 review (a `(declared N)` read as either)
        summary.add("# chaos L2 census: " + allDifferences.size() + " differing rows over "
                + compared.size() + " compared keys (declaredFileRows=" + declared.size()
                + " declaredDiffering=" + declaredDiffering
                + "); classes = seed | reference kind | fork target | upstream target");
        classes.forEach((cls, n) -> summary.add(String.format("%6d  %s", n, cls)));
        Files.write(out.resolve("l2-census.txt"), summary);
    }

    // ------------------------------------------------------------------ plumbing --

    private static List<RModel> cellModels;
    private static final Map<String, String> SOURCE_BY_FILE_NAME = new LinkedHashMap<>();

    private static synchronized void buildCellOnce() throws IOException {
        if (cellModels != null) {
            return;
        }
        List<RModel> subjects = new ArrayList<>();
        try (Stream<Path> files = Files.walk(CHAOS_SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList()) {
                if (ChaosParseExpectations.isExpectedRefusal(file)) {
                    continue; // § 4b: refused on BOTH sides; no XREF rows exist for these
                }
                String source = Files.readString(file);
                SOURCE_BY_FILE_NAME.put(file.getFileName().toString(), source);
                subjects.add(AstBuilder.buildFromString(source, file.toString()));
            }
        }
        assertFalse(subjects.isEmpty(), "no chaos sources found under " + CHAOS_SOURCES.toAbsolutePath());

        List<RModel> models = new ArrayList<>(builtinModels());
        models.addAll(subjects);
        RWorkspace.build(models);
        cellModels = subjects;
    }

    /** Same builtin copies the oracle driver defaults to — both sides see the same inputs. */
    private static List<RModel> builtinModels() throws IOException {
        Path builtins = Paths.get("../test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model");
        assertTrue(Files.isDirectory(builtins),
                "the builtin models must be present at " + builtins.toAbsolutePath());
        List<RModel> models = new ArrayList<>();
        try (Stream<Path> files = Files.list(builtins)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList()) {
                models.add(AstBuilder.buildFromString(Files.readString(file), file.toString()));
            }
        }
        return models;
    }

    /**
     * {@link ResolutionConformanceTest}'s dump reader, pointed at the chaos dump:
     * same columns, same in-scope filter, same records=N truncation guard.
     */
    private static Map<String, String> readOracleDump() throws IOException {
        Map<String, String> byKey = new LinkedHashMap<>();
        Set<String> inScope = new LinkedHashSet<>(ForkResolutionDump.IN_SCOPE_REFS);
        int declaredRecords = -1;
        int actualRecords = 0;
        for (String line : Files.readAllLines(ORACLE_DUMP)) {
            if (line.startsWith("#")) {
                java.util.regex.Matcher header =
                        java.util.regex.Pattern.compile("records=(\\d+)").matcher(line);
                if (header.find()) {
                    declaredRecords = Integer.parseInt(header.group(1));
                }
                continue;
            }
            if (!line.isBlank()) {
                actualRecords++;
            }
            if (!line.startsWith("XREF\t")) {
                continue;
            }
            String[] columns = line.split("\t", -1);
            if (columns.length != 9 || !inScope.contains(columns[5])) {
                continue;
            }
            // The § 4b expected-refusal files are excluded from BOTH sides. The oracle
            // REFUSES each a5bom file whole (LINKERR at 1:0) yet still records XREFs
            // from its error-recovered parse; the fork refuses with no recovery. Rows
            // from a file neither pipeline accepts are not comparable bindings — the
            // refusal itself is the contract, gated by ChaosForkDiagnosticsGateTest.
            if (ChaosParseExpectations.expectedRefusals().containsKey(columns[1])) {
                continue;
            }
            byKey.put(key(columns[1], columns[2], columns[5], columns[6]),
                    columns[7] + " " + columns[8]);
        }
        assertTrue(declaredRecords >= 0, "the chaos oracle dump has no records=N header");
        assertEquals(declaredRecords, actualRecords,
                "the chaos oracle dump declares records=" + declaredRecords + " but carries "
                        + actualRecords + " — truncated or drifted; regenerate it");
        return byKey;
    }

    /**
     * The declared L2 differences: {@code <key>} lines (the differential's own key
     * form), {@code #} comments and blanks ignored. Absent file = empty set (the
     * differential is then asserted CLEAN outright).
     */
    private static Map<String, String> readExpectedDifferences() throws IOException {
        Map<String, String> keys = new LinkedHashMap<>();
        if (!Files.isRegularFile(EXPECTED_DIFFERENCES)) {
            return keys;
        }
        int lineNo = 0;
        for (String raw : Files.readAllLines(EXPECTED_DIFFERENCES)) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (keys.put(line, "declared@" + lineNo) != null) {
                throw new AssertionError(EXPECTED_DIFFERENCES + ":" + lineNo
                        + " — duplicate declared difference: " + line);
            }
        }
        return keys;
    }

    private static Map<String, String> index(List<String> records) {
        Map<String, String> byKey = new LinkedHashMap<>();
        for (String record : records) {
            String[] columns = record.split("\t", -1);
            if (columns.length != 7) {
                continue;
            }
            byKey.put(key(columns[1], columns[2], columns[3], columns[4]),
                    columns[5] + " " + columns[6]);
        }
        return byKey;
    }

    private static String key(String file, String offset, String reference, String text) {
        return file + "@" + offset + " " + reference + " '" + text + "'";
    }
}
