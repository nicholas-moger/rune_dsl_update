package com.regnosys.rosetta.symbols.conformance;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
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

/**
 * THE RESOLUTION DIFFERENTIAL — the fork's name bindings against upstream
 * 9.83.0's, name by name, on a corpus of self-contained conformance snippets.
 *
 * <p>Upstream's answers are not recomputed here. They are read from
 * {@code upstream-9.83.0.tsv}, produced by {@code scripts/xtext-oracle} running
 * the RELEASED 9.83.0 jars in their own JVM — which is the only way the two
 * pipelines can both run, since they share the package
 * {@code com.regnosys.rosetta.*} with incompatible shapes and cannot sit on one
 * classpath. Regenerate with:
 *
 * <pre>
 * scripts/xtext-oracle/run-oracle.sh \
 *     --out rune-parser/src/test/resources/resolution-conformance/upstream-9.83.0.tsv \
 *     rune-parser/src/test/resources/resolution-conformance
 * </pre>
 *
 * <p><b>This test is EXPECTED RED until C1's Layer-1 rebuild lands.</b> It is
 * the failing test that the fix is written against, and its diff is the
 * work list. A green run means the fork resolves every conformance name exactly
 * where upstream does.
 *
 * <p>Scope is declared, not implied: only the three reference kinds C1 owns are
 * compared (see {@link ForkResolutionDump#IN_SCOPE_REFS}). Everything else the
 * oracle dumps — type calls, annotation refs, operation paths — belongs to
 * other layers and is filtered from BOTH sides identically, so nothing is
 * silently absent.
 */
class ResolutionConformanceTest {

    private static final Path SUITE =
            Paths.get("src/test/resources/resolution-conformance");
    private static final Path ORACLE_DUMP = SUITE.resolve("upstream-9.83.0.tsv");

    /**
     * THE FROZEN STRUCTURAL EXCEPTIONS — EMPTY since v3.2 seat 8. The set held ONE
     * row from the C1 close (MF-5) to seat 8:
     * {@code c1-precedence.rosetta@00003083 RosettaSymbolReference.symbol 'other'} —
     * upstream binds a lambda parameter to a {@code ClosureParameter} node while the
     * fork kept a lambda's parameters as a {@code List<String>} on
     * {@link com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction} and
     * could only record the DECLARING lambda (spec § 7's modelling gap). Seat 8
     * gave every declared parameter its own node
     * ({@link com.regnosys.rosetta.ast.expressions.supporting.RClosureParameter}),
     * the row agreed, the staleness assert below fired as designed, and the entry
     * left in the same change that closed the gap.
     *
     * <p>The machinery stays: an UNEXPECTED difference fails, and an exception that
     * STOPS differing also fails (staleness — a freeze must never outlive its
     * defect), per the ArchUnit Freezing pattern the SDLC rules already use. A
     * future entry uses the differential's own key form; if the snippet is edited
     * above the seat the offset shifts, the entry goes stale and this test forces
     * the re-pin rather than silently widening.
     */
    private static final Set<String> KNOWN_STRUCTURAL_EXCEPTIONS = Set.of();

    @Test
    void fork_resolves_every_conformance_name_where_upstream_does() throws IOException {
        Map<String, String> upstream = readOracleDump();
        buildSuiteOnce();
        Map<String, String> fork = index(ForkResolutionDump.dump(suiteModels, SOURCE_BY_FILE_NAME));

        // A dump that lost its records must never read as agreement — the
        // predecessor harness passed green precisely by comparing nothing.
        assertFalse(upstream.isEmpty(),
                "the committed oracle dump has no in-scope records; regenerate it "
                        + "with scripts/xtext-oracle/run-oracle.sh before trusting this test");
        assertFalse(fork.isEmpty(), "the fork produced no in-scope records");

        List<String> differences = new ArrayList<>();
        Set<String> matchedExceptions = new LinkedHashSet<>();
        Set<String> keys = new TreeSet<>();
        keys.addAll(upstream.keySet());
        keys.addAll(fork.keySet());
        for (String key : keys) {
            String theirs = upstream.get(key);
            String ours = fork.get(key);
            String difference = null;
            if (theirs == null) {
                difference = key + "\n    upstream  (no such reference)\n    fork      " + ours;
            } else if (ours == null) {
                difference = key + "\n    upstream  " + theirs + "\n    fork      (no such reference)";
            } else if (!theirs.equals(ours)) {
                difference = key + "\n    upstream  " + theirs + "\n    fork      " + ours;
            }
            if (difference != null) {
                // The freeze admits ONLY the present-on-both-sides-but-differing
                // shape — the structural mismatch it exists for. A row DROPPED
                // from either dump (theirs/ours null) is a different defect
                // (a lost record) and reports normally even on a frozen key
                // (Copilot R1, PR #568).
                if (theirs != null && ours != null && KNOWN_STRUCTURAL_EXCEPTIONS.contains(key)) {
                    matchedExceptions.add(key);
                } else {
                    differences.add(difference);
                }
            }
        }

        assertEquals(List.of(), differences,
                differences.size() + " of " + keys.size() + " conformance references resolve "
                        + "differently from upstream 9.83.0"
                        + (KNOWN_STRUCTURAL_EXCEPTIONS.isEmpty()
                                ? " (the frozen-exception set is EMPTY since v3.2 seat 8 — every"
                                        + " disagreement fails)"
                                : " beyond the " + KNOWN_STRUCTURAL_EXCEPTIONS.size()
                                        + " frozen structural exception(s)")
                        + ". Upstream wins every disagreement.\n"
                        + String.join("\n", differences));
        // The freeze must never outlive its defect: an exception that no longer
        // differs means the structural gap closed — remove the entry in the
        // same change that closed it.
        assertEquals(KNOWN_STRUCTURAL_EXCEPTIONS, matchedExceptions,
                "frozen structural exception(s) no longer differ from upstream — the gap "
                        + "closed; remove the stale entry from KNOWN_STRUCTURAL_EXCEPTIONS");
    }

    /**
     * The suite is only evidence if every snippet is a model upstream itself
     * accepts. Five expected refusals are pinned BY NAME, each the rule its
     * case exists to pin — the refusal IS the pin:
     * <ul>
     *   <li>{@code c1} {@code 'result'} — the function output named from a
     *       PRE-condition, which upstream filters out of scope on purpose;</li>
     *   <li>{@code p1} {@code 'shared'} — a common attribute reached through a
     *       plain {@code ->} on a CHOICE receiver: upstream's feature scope
     *       there is the options and nothing else (the reach-through is the
     *       deep path's semantics, R11);</li>
     *   <li>{@code p4} {@code 'Other'} — a bare enum value in a THEN arm with
     *       no container expectation: the ELSETHEN fallback is DIRECTIONAL and
     *       the then-arm gets the container only;</li>
     *   <li>{@code p5} {@code 'Other5'} — a bare enum value on the LEFT of
     *       {@code =}: upstream flows the expected type into the RIGHT operand
     *       only;</li>
     *   <li>{@code p9} {@code 'V1'} — an enum VALUE looked up on an expression
     *       that merely TYPES as the enum: the enum-values feature scope
     *       appears only on a literal enumeration reference (R12), so an
     *       enum-typed receiver's ordinary feature scope is empty.</li>
     * </ul>
     */
    @Test
    void oracle_dump_records_exactly_the_expected_upstream_refusals() throws IOException {
        List<String> refusals = Files.readAllLines(ORACLE_DUMP).stream()
                .filter(line -> line.startsWith("LINKERR\t"))
                .toList();
        assertEquals(5, refusals.size(),
                "the conformance snippets must be valid 9.83.0 models apart from the five "
                        + "deliberate refusals; upstream reported:\n  " + String.join("\n  ", refusals));
        assertTrue(refusals.stream().anyMatch(r -> r.contains("c1-precedence") && r.contains("'result'")),
                "the c1 refusal is the pre-condition output reference, but upstream reported:\n  "
                        + String.join("\n  ", refusals));
        assertTrue(refusals.stream().anyMatch(r -> r.contains("p1-choice-common-attr") && r.contains("'shared'")),
                "the p1 refusal is the plain-arrow common attribute on a choice, but upstream reported:\n  "
                        + String.join("\n  ", refusals));
        assertTrue(refusals.stream().anyMatch(r -> r.contains("p4-bare-then-arm") && r.contains("'Other'")),
                "the p4 refusal is the container-less bare THEN arm, but upstream reported:\n  "
                        + String.join("\n  ", refusals));
        assertTrue(refusals.stream().anyMatch(r -> r.contains("p5-bare-lhs-compare") && r.contains("'Other5'")),
                "the p5 refusal is the bare LEFT equality operand, but upstream reported:\n  "
                        + String.join("\n  ", refusals));
        assertTrue(refusals.stream().anyMatch(r -> r.contains("p9-enum-typed-expr") && r.contains("'V1'")),
                "the p9 refusal is the enum value on a merely-enum-typed receiver, but upstream reported:\n  "
                        + String.join("\n  ", refusals));
    }

    /**
     * THE DIAGNOSTIC SIDE OF THE SAME DIFFERENTIAL. Bindings are only half the
     * question: a resolver can bind every name where upstream does and still
     * report errors upstream never reports — which is the shape of the 32
     * TYPE_ERRORs per drr 7.x cell.
     *
     * <p>Upstream's verdict comes from the oracle dump's LINKERR and VALIDERR
     * records. It is a COMPLETE verdict only because the oracle runs upstream's
     * VALIDATOR explicitly: {@code XtextResource.getErrors()} carries syntax and
     * linking problems alone, and every type check upstream performs lives in
     * the validator. The oracle carries a positive control for exactly this —
     * see its README — because a validator that returns nothing is
     * indistinguishable from a language with no complaints.
     *
     * <p>The comparison is per-file sorted LINE MULTISETS, not messages and not
     * bare counts (SF-4): the two pipelines word their diagnostics differently,
     * so holding them to identical prose would be a gate about wording — but
     * bare counts TIE when the fork misses one error and invents another, and
     * that arithmetic coincidence happened live (commit 16/n resolved a2's
     * last unresolved name and a2 IMMEDIATELY appeared in the report, because
     * upstream's one a2 error was an unrelated validator rule the two sides
     * had been cancelling 1-1). Same-line coincidences remain theoretically
     * possible and accepted; both full lists print on failure either way.
     *
     * <p>Note what this does NOT assume: that the snippets are error-free.
     * Several are not — upstream rejects `extract Colour` once it binds to an
     * enum, and rejects a flatten over a non-list. A snippet can pin a binding
     * and still fail validation; the two questions are separate, and this gate
     * asks only that the fork agrees with upstream on the second.
     */
    @Test
    void fork_and_upstream_agree_on_which_files_have_errors() throws IOException {
        buildSuiteOnce();

        Map<String, List<Integer>> upstreamLines = new java.util.TreeMap<>();
        Map<String, List<String>> upstreamDetail = new java.util.TreeMap<>();
        for (String line : Files.readAllLines(ORACLE_DUMP)) {
            if (line.startsWith("VALIDERR	") || line.startsWith("LINKERR	")) {
                String[] columns = line.split("	", -1);
                // A single unresolved reference surfaces through BOTH channels
                // upstream; count it once.
                if (line.startsWith("LINKERR	")) {
                    continue;
                }
                upstreamLines.computeIfAbsent(columns[1], k -> new ArrayList<>())
                        .add(Integer.parseInt(columns[2]));
                upstreamDetail.computeIfAbsent(columns[1], k -> new ArrayList<>())
                        .add("line " + columns[2] + " :: " + columns[3]);
            }
        }

        Map<String, List<Integer>> forkLines = new java.util.TreeMap<>();
        Map<String, List<String>> forkDetail = new java.util.TreeMap<>();
        for (LinkingDiagnostic diagnostic : suiteResult.linkingDiagnostics()) {
            if (diagnostic.severity() == Severity.ERROR) {
                forkLines.computeIfAbsent(fileOf(diagnostic.range()), k -> new ArrayList<>())
                        .add(diagnostic.range().startLine());
                forkDetail.computeIfAbsent(fileOf(diagnostic.range()), k -> new ArrayList<>())
                        .add("line " + diagnostic.range().startLine() + " :: " + diagnostic.category()
                                + " '" + diagnostic.unresolvedName() + "'");
            }
        }
        for (ValidationDiagnostic diagnostic : suiteResult.workspace().validationDiagnostics()) {
            if (diagnostic.severity() == Severity.ERROR) {
                forkLines.computeIfAbsent(fileOf(diagnostic.range()), k -> new ArrayList<>())
                        .add(diagnostic.range().startLine());
                forkDetail.computeIfAbsent(fileOf(diagnostic.range()), k -> new ArrayList<>())
                        .add("line " + diagnostic.range().startLine() + " :: "
                                + diagnostic.issueCode() + " " + diagnostic.message());
            }
        }

        List<String> report = new ArrayList<>();
        Set<String> files = new TreeSet<>();
        files.addAll(upstreamLines.keySet());
        files.addAll(forkLines.keySet());
        for (String file : files) {
            List<Integer> theirs = new ArrayList<>(upstreamLines.getOrDefault(file, List.of()));
            List<Integer> ours = new ArrayList<>(forkLines.getOrDefault(file, List.of()));
            theirs.sort(Integer::compareTo);
            ours.sort(Integer::compareTo);
            if (!theirs.equals(ours)) {
                report.add(file + " - upstream lines " + theirs + ", fork lines " + ours
                        + "\n      upstream: " + String.join("\n                ",
                                upstreamDetail.getOrDefault(file, List.of()))
                        + "\n      fork:     " + String.join("\n                ",
                                forkDetail.getOrDefault(file, List.of())));
            }
        }

        assertEquals(List.of(), report,
                report.size() + " file(s) where the fork and upstream 9.83.0 disagree about which "
                        + "lines have errors.\n  " + String.join("\n  ", report));
    }

    private static String fileOf(com.regnosys.rosetta.ast.SourceRange range) {
        return range.file() == null ? "?" : Paths.get(range.file()).getFileName().toString();
    }


    /**
     * The suite is parsed and linked ONCE for the whole class. RWorkspace.build
     * freezes the AST it links, so building a second time in the same JVM trips
     * the freeze guard — and both tests need the same linked graph anyway.
     */
    private static List<RModel> suiteModels;
    private static RLinkingResult suiteResult;
    private static final Map<String, String> SOURCE_BY_FILE_NAME = new LinkedHashMap<>();

    private static synchronized void buildSuiteOnce() throws IOException {
        if (suiteModels != null) {
            return;
        }
        List<RModel> subjects = new ArrayList<>();
        try (Stream<Path> files = Files.list(SUITE)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList()) {
                String source = Files.readString(file);
                SOURCE_BY_FILE_NAME.put(file.getFileName().toString(), source);
                subjects.add(AstBuilder.buildFromString(source, file.toString()));
            }
        }
        assertFalse(subjects.isEmpty(), "no conformance snippets found under " + SUITE.toAbsolutePath());

        // BOTH SIDES MUST SEE THE SAME INPUTS. The oracle driver loads the
        // builtin models (annotations.rosetta, basictypes.rosetta) into its
        // resource set, so `string`, `boolean` and `[metadata ...]` resolve
        // there. Linking the suite without them here would manufacture dozens
        // of fork-only errors that say nothing about the engine — an
        // apples-to-oranges comparison dressed up as a finding.
        List<RModel> models = new ArrayList<>(builtinModels());
        models.addAll(subjects);

        suiteResult = RWorkspace.build(models);
        suiteModels = subjects;
    }

    /**
     * The builtin models, from the same in-repo copies the oracle driver
     * defaults to. Loaded for RESOLUTION only — they are not part of the
     * subject set and never appear in either dump.
     */
    private static List<RModel> builtinModels() throws IOException {
        Path builtins = Paths.get("../test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model");
        assertTrue(Files.isDirectory(builtins),
                "the builtin models must be present at " + builtins.toAbsolutePath()
                        + " — without them this comparison is not like-for-like");
        List<RModel> models = new ArrayList<>();
        try (Stream<Path> files = Files.list(builtins)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList()) {
                models.add(AstBuilder.buildFromString(Files.readString(file), file.toString()));
            }
        }
        return models;
    }

    /**
     * Reads the oracle dump into {@code key -> binding}, applying the same
     * in-scope filter the fork side applies.
     *
     * <p>Oracle columns: {@code XREF file offset length srcEClass ref text
     * targetEClass targetFqn}; the fork emits the same record without the
     * {@code length} and {@code srcEClass} columns, which describe upstream's
     * node model rather than its resolution.
     */
    private static Map<String, String> readOracleDump() throws IOException {
        Map<String, String> byKey = new LinkedHashMap<>();
        Set<String> inScope = new LinkedHashSet<>(ForkResolutionDump.IN_SCOPE_REFS);
        // The records=N header is WRITTEN by the oracle but was never read back
        // (SF-4): a dump truncated mid-write, or one whose format drifted so
        // that rows stopped parsing, read exactly like a smaller suite. Parse
        // the header and hold the file to it.
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
            byKey.put(key(columns[1], columns[2], columns[5], columns[6]),
                    columns[7] + " " + columns[8]);
        }
        assertTrue(declaredRecords >= 0,
                "the oracle dump has no records=N header — it was not produced by the "
                        + "oracle driver; regenerate it with scripts/xtext-oracle/run-oracle.sh");
        assertEquals(declaredRecords, actualRecords,
                "the oracle dump declares records=" + declaredRecords + " but carries "
                        + actualRecords + " — the file is truncated or drifted; regenerate it");
        return byKey;
    }

    /** Indexes the fork's records, which already carry only in-scope kinds. */
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
