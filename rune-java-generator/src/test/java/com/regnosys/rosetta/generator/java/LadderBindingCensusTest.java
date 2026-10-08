package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.LadderBindingCensus.CensusResult;
import com.regnosys.rosetta.generator.java.LadderBindingCensus.Row;
import com.regnosys.rosetta.generator.java.LadderBindingCensus.RowClass;
import com.regnosys.rosetta.generator.java.LadderBindingCensus.Stats;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.1 LADDER RETIREMENT — the count-then-flip census (plan § 4).
 *
 * <p><b>Unit half (the LAW-60 positive controls):</b> the census must be PROVEN
 * able to detect every row class it reports, on live parses — a comparator that
 * silently reads empty on both sides is indistinguishable from agreement. Each
 * control is an inline model distilled from a committed conformance shape
 * (a2's bare option / E1's option-nav / T10's attr-option nav), and two silence
 * controls prove the deliberately-unrowed classes are SEEN and counted, not
 * missed: the fallback mass (authority slots are partial by design) and the
 * input-nav mass (head implied by an agreeing right side).
 *
 * <p><b>Corpus half (the census + the gate):</b> one parameterized run per
 * active cell — walk every EMITTING model, write the differing rows to
 * {@code target/ladder-census/<cell>.tsv}, print the summary line, and enforce
 * the flip precondition: every candidate file of every row must be a
 * currently-mismatching band member on BOTH byte routes (the flip spans the
 * Path-1 handlers and the IR adapter — the adapter reads the same legacy
 * rungs), or the (cell, file) pair must be triaged by name in the committed
 * triage file. The band lists, triage file and count pin live under
 * {@code src/test/resources/ladder-census/} and are COMMITTED — a missing
 * resource fails loudly (a gate that turns itself off reads exactly like a gate
 * that passed). {@link #gateResources_areInternallyValid()} additionally holds
 * every resource row to the active-cell universe, so a typo'd or retired cell
 * name cannot leave a permanently-dead row the per-cell staleness check never
 * sees.
 *
 * <p>The triage file is a BIDIRECTIONAL freeze (the ArchUnit Freezing pattern,
 * same as {@code KNOWN_STRUCTURAL_EXCEPTIONS} and the evidence ledger's join):
 * an untriaged out-of-band candidate fails, and a triage row no row uses ALSO
 * fails, so the freeze cannot outlive the gap it excuses.
 */
class LadderBindingCensusTest {

    private static final Path BUILTINS_DIR =
            Path.of("../test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    private static final Path CENSUS_OUT_DIR = Path.of("target", "ladder-census");
    private static final Path GATE_RESOURCE_DIR =
            Path.of("src", "test", "resources", "ladder-census");

    static boolean builtinsAvailable() {
        return Files.isDirectory(BUILTINS_DIR);
    }

    // =========================================================================
    // Unit half — the positive controls
    // =========================================================================

    /** The a2 star witness: a bare option name shadowed by a same-named GLOBAL choice. */
    private static final String REBIND_MODEL = """
            namespace census.rebind
            version "1.0.0"
            type CreditIndex:
                identifier string (1..1)
            type EquityIndex:
                identifier string (1..1)
            choice Index:
                CreditIndex
                EquityIndex
            type Basket:
                identifier string (1..1)
            choice Underlier:
                Basket
                Index
            type Holder:
                underlier Underlier (1..*)
            func BareOption:
                inputs:
                    holder Holder (1..1)
                output:
                    result boolean (0..*)
                set result:
                    holder -> underlier
                        extract
                            if Index exists
                            then True
                            else False
            """;

    /** E1's shape: option navigation where the LHS is the implicit item (no attribute rung fits). */
    private static final String AUTH_ONLY_MODEL = """
            namespace census.authonly
            version "1.0.0"
            type Basket:
                identifier string (1..1)
            type Security:
                identifier string (1..1)
            choice Underlier:
                Basket
                Security
            type Holder:
                underlier Underlier (1..*)
            func OptionNav:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..*)
                set result:
                    holder -> underlier
                        extract
                            Basket -> identifier
            """;

    /** T10's shape: {@code <choiceAttr> -> <OptionName>} — legacy binds the option's TYPE. */
    private static final String SAME_DENOTATION_MODEL = """
            namespace census.samedenot
            version "1.0.0"
            type Basket:
                identifier string (1..1)
            type Security:
                identifier string (1..1)
            choice Underlier:
                Basket
                Security
            type Holder:
                underlier Underlier (1..1)
                condition BasketOnly:
                    underlier -> Basket exists
            """;

    /** The input-nav mass: {@code <input> -> <feature>} — head implied, right side agrees. */
    private static final String INPUT_NAV_MODEL = """
            namespace census.inputnav
            version "1.0.0"
            type Payout:
                identifier string (1..1)
            type Trade:
                payout Payout (1..1)
            func InputNav:
                inputs:
                    trade Trade (1..1)
                output:
                    result string (0..1)
                set result:
                    trade -> payout -> identifier
            """;

    /** The agreeing/fallback bulk: a plain qualified enum value in a plain function. */
    private static final String AGREE_MODEL = """
            namespace census.agree
            version "1.0.0"
            enum SideEnum:
                Buy
                Sell
            func PickSide:
                inputs:
                    side SideEnum (1..1)
                output:
                    result boolean (1..1)
                set result:
                    side = SideEnum -> Buy
            """;

    private static CensusResult censusOf(String source) {
        List<RModel> models = new ArrayList<>();
        try (var stream = Files.walk(BUILTINS_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta")).sorted()
                    .forEach(p -> models.add(AstBuilder.buildFromFile(p)));
        } catch (IOException e) {
            throw new AssertionError("builtins walk failed: " + e.getMessage(), e);
        }
        RModel subject = AstBuilder.buildFromString(source, "census-control.rosetta");
        models.add(subject);
        var linkingResult = RWorkspace.build(models);
        var gm = new GeneratorModel(linkingResult.workspace());
        return LadderBindingCensus.censusOfModel(
                "control", subject, gm, TYPE_TRANSLATOR, TYPE_UTIL);
    }

    private static List<Row> rowsOf(List<Row> rows, RowClass rowClass) {
        return rows.stream().filter(r -> r.rowClass() == rowClass).toList();
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void positiveControl_rebind_bareOptionShadowedByGlobalChoice() {
        List<Row> rows = censusOf(REBIND_MODEL).rows();
        List<Row> rebinds = rowsOf(rows, RowClass.REBIND);
        assertTrue(rebinds.stream().anyMatch(r ->
                        "SYM".equals(r.side())
                        && "Index".equals(r.name())
                        && r.legacy().startsWith("SYMBOL=RChoice:")
                        && r.authority().startsWith("RChoiceOption:")),
                "expected the a2-shape REBIND row (bare Index: legacy=global choice through "
                        + "the SYMBOL slot, authority=ChoiceOption) — the rung condition must "
                        + "keep this OUT of SAME_DENOTATION; got rows:\n" + dump(rows));
        // The mapping control rides the same witness: a function-contained row maps
        // to exactly the function's generated file, by the generator's own naming.
        Row witness = rebinds.stream().filter(r -> "Index".equals(r.name())).findFirst().orElseThrow();
        assertEquals(List.of("census/rebind/functions/BareOption.java"), witness.candidateFiles(),
                "function-contained row must map to the function's own generated file");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void positiveControl_authOnly_optionNavOnImplicitItem() {
        List<Row> rows = censusOf(AUTH_ONLY_MODEL).rows();
        assertTrue(rowsOf(rows, RowClass.AUTH_ONLY).stream().anyMatch(r ->
                        r.side().startsWith("EVR.")
                        && "Basket".equals(r.name())
                        && "UNBOUND".equals(r.legacy())
                        && r.authority().startsWith("RChoiceOption:")),
                "expected the E1-shape AUTH_ONLY row (option nav on the implicit item: "
                        + "every legacy rung empty, authority=ChoiceOption); got rows:\n" + dump(rows));
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void positiveControl_sameDenotation_choiceAttrOptionNav_inCondition() {
        List<Row> rows = censusOf(SAME_DENOTATION_MODEL).rows();
        List<Row> sameDenotation = rowsOf(rows, RowClass.SAME_DENOTATION);
        assertTrue(sameDenotation.stream().anyMatch(r ->
                        "EVR.right".equals(r.side())
                        && "Basket".equals(r.name())
                        && r.legacy().startsWith("CHOICE_OPTION_TYPE=")
                        && r.authority().startsWith("RChoiceOption:")),
                "expected the T10-shape SAME_DENOTATION row (legacy=option's TYPE through "
                        + "the T10 rung, authority=the ChoiceOption); got rows:\n" + dump(rows));
        // Mapping control: a condition-contained row maps to the datarule class AND
        // the declaring type's POJO (the E1 import-only class reaches POJO imports).
        Row witness = sameDenotation.stream()
                .filter(r -> "Basket".equals(r.name())).findFirst().orElseThrow();
        assertEquals(
                List.of("census/samedenot/validation/datarule/HolderBasketOnly.java",
                        "census/samedenot/Holder.java"),
                witness.candidateFiles(),
                "condition-contained row must map to the datarule file plus the POJO");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void silenceControl_inputNav_headImpliedByAgreeingRight_isCountedNotRowed() {
        CensusResult result = censusOf(INPUT_NAV_MODEL);
        assertEquals(List.of(), result.rows(),
                "the input-nav mass must stay silent — the legacy engine resolved the nav "
                        + "through the right-side rung, the head is implied; got:\n"
                        + dump(result.rows()));
        assertTrue(result.stats().headOnlyAuthNodes() >= 1,
                "the silenced input-nav node must be COUNTED (headOnlyAuthNodes), proving the "
                        + "census saw it and chose silence deliberately; stats=" + result.stats());
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void silenceControl_fallbackMass_plainBindings_areCountedNotRowed() {
        CensusResult result = censusOf(AGREE_MODEL);
        assertEquals(List.of(), result.rows(),
                "the fallback mass must stay silent — the authority slots are partial by "
                        + "design and a bound legacy slot under an empty authority slot is "
                        + "byte-inert under the flip's fallback read; got:\n" + dump(result.rows()));
        assertTrue(result.stats().legacyOnlySides() >= 3,
                "the silenced fallback sides (the bound input read + the plain enum ref's two "
                        + "sides) must be COUNTED (legacyOnlySides); stats=" + result.stats());
    }

    private static String dump(List<Row> rows) {
        return rows.stream().map(Row::tsv).collect(Collectors.joining("\n"));
    }

    // =========================================================================
    // Corpus half — the census + the band gate
    // =========================================================================

    static Stream<D11CorpusRegressionTest.CellSpec> cells() {
        return D11CorpusRegressionTest.activeCells();
    }

    /**
     * The resource-universe check (the review's unknown-cell blind spot): the
     * per-cell staleness direction of the triage freeze only sees rows whose cell
     * column matches the running cell, so a row keyed to a typo'd or retired cell
     * would be permanently dead and never flagged. This test closes that hole
     * globally: every cell name in every gate resource must be an ACTIVE cell, the
     * count pin must have EXACTLY one row per active cell (no duplicates, none
     * missing), and triage rows must not duplicate a (cell, key) pair. Corpus-free
     * (reads the committed resources and the cell catalogue only), so it runs
     * everywhere the suite runs. Uses the unfiltered {@code ALL_CELLS} universe via
     * an unscoped {@code activeCells()} equivalent — the -Dd11.corpus/-Dd11.version
     * scoping flags must not shrink the universe this test validates against, or a
     * scoped run would false-fail rows for the cells scoped out.
     */
    @Test
    void gateResources_areInternallyValid() throws IOException {
        Set<String> active = D11CorpusRegressionTest.allCellNames();
        assertTrue(!active.isEmpty(), "the active-cell catalogue read empty");

        Map<String, Long> pinRowsPerCell = new HashMap<>();
        for (String line : Files.readAllLines(GATE_RESOURCE_DIR.resolve("expected-summary.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String cell = line.split("\t", -1)[0];
            assertTrue(active.contains(cell),
                    "expected-summary.tsv row for unknown cell '" + cell + "'");
            pinRowsPerCell.merge(cell, 1L, Long::sum);
        }
        assertEquals(active, pinRowsPerCell.keySet(),
                "expected-summary.tsv must pin EXACTLY the active cells");
        pinRowsPerCell.forEach((cell, n) -> assertEquals(1L, n,
                "expected-summary.tsv has " + n + " rows for " + cell
                        + " — duplicates shadow (first match wins in enforceCountPin)"));

        Set<String> triageKeys = new HashSet<>();
        for (String line : Files.readAllLines(GATE_RESOURCE_DIR.resolve("triage.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            assertTrue(active.contains(parts[0]),
                    "triage.tsv row for unknown cell '" + parts[0] + "' — a row the per-cell"
                            + " staleness check would never see");
            assertTrue(triageKeys.add(parts[0] + "\t" + parts[1]),
                    "triage.tsv duplicate row for " + parts[0] + " / " + parts[1]);
        }

        for (String bandFile : List.of("band-off.tsv", "band-on.tsv")) {
            for (String line : Files.readAllLines(GATE_RESOURCE_DIR.resolve(bandFile))) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String cell = line.split("\t", -1)[0];
                assertTrue(active.contains(cell),
                        bandFile + " row for unknown cell '" + cell + "'");
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cells")
    void ladder_census(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        Assumptions.assumeTrue(D11CorpusRegressionTest.cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));

        List<Row> rows = new ArrayList<>();
        Stats stats = Stats.ZERO;
        int emittingModels = 0;
        for (RModel model : gm.files()) {
            if (!gm.shouldGenerate(model)) {
                continue;
            }
            emittingModels++;
            CensusResult result = LadderBindingCensus.censusOfModel(
                    cell.toString(), model, gm, TYPE_TRANSLATOR, TYPE_UTIL);
            rows.addAll(result.rows());
            stats = stats.plus(result.stats());
        }
        rows.sort(Row.ORDER);
        assertTrue(emittingModels > 0,
                "census walked zero emitting models for " + cell + " — the corpus wiring is broken");
        // No nodesSeen floor: two cells measure a GENUINE zero (iso20022/1.38.0's only
        // conditions are `condition Choice:` one-ofs and rune-fpml/1.6.0 declares no
        // functions — verified against the sources at first run). The walker's
        // detectability is pinned by the unit-half positive controls; a zero here
        // prints loudly in the summary line rather than failing a truthful cell.

        Files.createDirectories(CENSUS_OUT_DIR);
        Path outFile = CENSUS_OUT_DIR.resolve(cell.toString().replace('/', '_') + ".tsv");
        Files.write(outFile, rows.stream().map(Row::tsv).toList());

        Map<RowClass, Long> byClass = rows.stream().collect(Collectors.groupingBy(
                Row::rowClass, () -> new EnumMap<>(RowClass.class), Collectors.counting()));
        Set<String> candidateFiles = rows.stream()
                .flatMap(r -> r.candidateFiles().stream())
                .collect(Collectors.toCollection(TreeSet::new));
        long unmapped = candidateFiles.stream().filter(f -> f.startsWith("UNMAPPED:")).count();
        System.out.println("LADDER-CENSUS " + cell
                + " rows=" + rows.size()
                + " rebind=" + byClass.getOrDefault(RowClass.REBIND, 0L)
                + " sameDenotation=" + byClass.getOrDefault(RowClass.SAME_DENOTATION, 0L)
                + " authOnly=" + byClass.getOrDefault(RowClass.AUTH_ONLY, 0L)
                + " candidateFiles=" + candidateFiles.size()
                + " unmapped=" + unmapped
                + " | nodes=" + stats.nodesSeen()
                + " agreeSides=" + stats.agreeSides()
                + " legacyOnlySides=" + stats.legacyOnlySides()
                + " headOnlyAuthNodes=" + stats.headOnlyAuthNodes());

        enforceCountPin(cell.toString(), rows.size(),
                byClass.getOrDefault(RowClass.REBIND, 0L),
                byClass.getOrDefault(RowClass.SAME_DENOTATION, 0L),
                byClass.getOrDefault(RowClass.AUTH_ONLY, 0L),
                candidateFiles);
        enforceBandGate(cell.toString(), rows);
    }

    /**
     * The per-cell census pin — EXACT both directions (the #569 ledger's pin
     * discipline), over SIX facts: the row count, the three class counts, and the
     * candidate-file REACH as both a count and a digest of the sorted distinct set.
     *
     * <p>This is what keeps the class-grain triage honest, and since the v3.1 stage
     * exit it is the ONLY thing that does. The flip healed enough band files that
     * every {@code (cell, RowClass)} group carrying rows now has a {@code CLASS:}
     * triage row, so {@link #enforceBandGate}'s violation direction is inert:
     * out-of-band candidates are class-exempt in all 25 cells. Its STALENESS
     * direction still bites (a class row that stops being used fails), but a NEW
     * differing binding, or an existing one re-pointing at a different file, is
     * caught HERE or not at all.
     *
     * <p>The digest is why the reach is pinned as a set and not only a count: a row
     * whose candidate moves from an in-band file to a byte-identical out-of-band one
     * leaves the count unchanged. Only the set digest sees that substitution — and
     * for the drr 7.x cells, which are NOT among the five ring cells, no byte ring
     * would see it either.
     *
     * <p>Re-pin deliberately, in the same commit as the flip step that moves it.
     * {@code scripts/ci/} is not involved: the pin is this file's committed
     * {@code expected-summary.tsv}, and the failure message prints the measured
     * replacement row ready to paste.
     */
    private void enforceCountPin(String cell, long rows, long rebind,
                                 long sameDenotation, long authOnly,
                                 Set<String> candidateFiles) throws IOException {
        Path pinPath = GATE_RESOURCE_DIR.resolve("expected-summary.tsv");
        // Committed resource — absence fails loudly, same rationale as the band gate.
        assertTrue(Files.isRegularFile(pinPath),
                "LADDER-CENSUS count pin missing (" + pinPath
                        + ") — the pin is committed; its absence fails loudly.");
        String reachDigest = digestOf(candidateFiles);
        for (String line : Files.readAllLines(pinPath)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length != 7) {
                throw new AssertionError(pinPath + " malformed pin line (expected 7 columns:"
                        + " cell, rows, rebind, sameDenotation, authOnly, candidateFiles,"
                        + " reachDigest): " + line);
            }
            if (parts[0].equals(cell)) {
                String expected = String.join("/", parts[1], parts[2], parts[3], parts[4],
                        parts[5], parts[6]);
                String actual = String.join("/", String.valueOf(rows), String.valueOf(rebind),
                        String.valueOf(sameDenotation), String.valueOf(authOnly),
                        String.valueOf(candidateFiles.size()), reachDigest);
                assertEquals(expected, actual,
                        "LADDER-CENSUS " + cell + " pin (rows/rebind/sameDenotation/authOnly/"
                                + "candidateFiles/reachDigest) moved — re-pin deliberately with"
                                + " the change that moved it. Measured row for " + pinPath + ":\n"
                                + cell + "\t" + rows + "\t" + rebind + "\t" + sameDenotation
                                + "\t" + authOnly + "\t" + candidateFiles.size() + "\t"
                                + reachDigest);
                return;
            }
        }
        throw new AssertionError("LADDER-CENSUS " + cell + " has no row in " + pinPath
                + " — every active cell must be pinned");
    }

    /**
     * The first 8 hex chars of the SHA-256 over the sorted candidate paths, one per
     * line, LF-terminated — the same shape as the D11 ring/matrix digests, so the
     * value can be reproduced from the census TSV with a shell one-liner:
     * {@code cut -f10 target/ladder-census/<cell>.tsv | tr ';' '\n' | sort -u
     * | sha256sum | cut -c1-8}.
     */
    private static String digestOf(Set<String> candidateFiles) {
        StringBuilder sb = new StringBuilder();
        for (String f : new TreeSet<>(candidateFiles)) {
            sb.append(f).append('\n');
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * The flip precondition (plan § 4): every candidate file a differing binding can
     * reach must be currently mismatching on BOTH byte routes, or be triaged. Triage
     * comes in two grains, both with a MANDATORY written reason:
     *
     * <ul>
     *   <li><b>File-grain</b> — {@code <cell>\t<file>\t<reason>}: this candidate file
     *       is allowed out-of-band wherever it appears. For the sharp, enumerable
     *       classes (REBIND rows on byte-identical ring cells).</li>
     *   <li><b>Class-grain</b> — {@code <cell>\tCLASS:<RowClass>\t<reason>}: every
     *       row of the class in that cell is dispositioned by ONE recorded argument
     *       (e.g. SAME_DENOTATION rows are identity-inert under the flip's
     *       option→type render contract — the instrument's own predicate proves the
     *       denotation). A class row exempts the whole row, all candidates. Class
     *       dispositions are the census twin of the ledger's KEPT-BY-CLASS:
     *       recorded and revocable, never hardcoded exemptions. Strictly per-cell —
     *       no band-wide wildcard (see {@link #readTriage}).</li>
     * </ul>
     *
     * Both grains are BIDIRECTIONAL: an unused triage row fails (the freeze cannot
     * outlive the gap it excuses).
     */
    private void enforceBandGate(String cell, List<Row> rows) throws IOException {
        Path bandOff = GATE_RESOURCE_DIR.resolve("band-off.tsv");
        Path bandOn = GATE_RESOURCE_DIR.resolve("band-on.tsv");
        Path triagePath = GATE_RESOURCE_DIR.resolve("triage.tsv");
        // The construction window is CLOSED: all three gate resources are committed,
        // so an absent one is a broken checkout or a silent gate-kill, never a state
        // to run green through (a gate that turns itself off reads exactly like a
        // gate that passed — LAW 60). triage.tsv included (Copilot #570 R1): without
        // it, a cell with no out-of-band candidates would pass while the freeze's
        // staleness direction died silently everywhere.
        assertTrue(Files.isRegularFile(bandOff) && Files.isRegularFile(bandOn)
                        && Files.isRegularFile(triagePath),
                "LADDER-CENSUS gate resources missing (" + bandOff + " / " + bandOn
                        + " / " + triagePath
                        + ") — the gate resources are committed; their absence fails loudly.");
        Set<String> off = readCellSet(bandOff, cell);
        Set<String> on = readCellSet(bandOn, cell);
        Triage triage = readTriage(triagePath, cell);

        Set<String> usedTriage = new HashSet<>();
        List<String> violations = new ArrayList<>();
        for (Row row : rows) {
            String classKey = "CLASS:" + row.rowClass();
            for (String candidate : row.candidateFiles()) {
                boolean inBothBands = off.contains(candidate) && on.contains(candidate);
                if (inBothBands) {
                    continue;
                }
                if (triage.classKeys().contains(classKey)) {
                    usedTriage.add(classKey);
                    continue;
                }
                if (triage.fileKeys().contains(candidate)) {
                    usedTriage.add(candidate);
                    continue;
                }
                violations.add(row.tsv() + "  [candidate " + candidate
                        + " off=" + off.contains(candidate) + " on=" + on.contains(candidate) + "]");
            }
        }
        Set<String> staleTriage = new TreeSet<>();
        staleTriage.addAll(triage.classKeys());
        staleTriage.addAll(triage.fileKeys());
        staleTriage.removeAll(usedTriage);

        assertTrue(violations.isEmpty() && staleTriage.isEmpty(),
                "LADDER-CENSUS " + cell + " gate: "
                        + violations.size() + " untriaged out-of-band candidate row(s) and "
                        + staleTriage.size() + " stale triage row(s).\n"
                        + "A differing binding may only reach currently-mismatching files "
                        + "(both routes) or be triaged in " + triagePath + ".\n"
                        + (violations.isEmpty() ? "" : "VIOLATIONS:\n  "
                                + String.join("\n  ", violations) + "\n")
                        + (staleTriage.isEmpty() ? "" : "STALE TRIAGE (freeze is bidirectional):\n  "
                                + String.join("\n  ", staleTriage)));
    }

    /** Reads {@code <cell>\t<file>} lines for one cell; {@code #} comments and blanks skipped. */
    private static Set<String> readCellSet(Path file, String cell) throws IOException {
        Set<String> result = new LinkedHashSet<>();
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length < 2) {
                throw new AssertionError(file + " malformed line: " + line);
            }
            if (parts[0].equals(cell)) {
                result.add(parts[1]);
            }
        }
        return result;
    }

    /** The triage rows applying to one cell, split by grain (see {@link #enforceBandGate}). */
    private record Triage(Set<String> classKeys, Set<String> fileKeys) {}

    /**
     * Reads {@code <cell>\t<CLASS:RowClass-or-file>\t<reason>} triage rows for one
     * cell; the reason column is MANDATORY. Rows are strictly per-cell — no
     * band-wide wildcard, because per-cell staleness detection (the freeze's
     * reverse direction) is exact only when every row belongs to the one cell
     * that must use it.
     */
    private static Triage readTriage(Path file, String cell) throws IOException {
        Set<String> classKeys = new HashSet<>();
        Set<String> fileKeys = new HashSet<>();
        // Existence is asserted by enforceBandGate before this is called; no lenient
        // missing-file branch (Copilot #570 R1 — a silently-empty triage disables the
        // freeze's staleness direction).
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length < 3 || parts[2].isBlank()) {
                throw new AssertionError(file
                        + " triage rows need <cell>\\t<key>\\t<reason>: " + line);
            }
            if (parts[1].startsWith("CLASS:")) {
                try {
                    RowClass.valueOf(parts[1].substring("CLASS:".length()));
                } catch (IllegalArgumentException e) {
                    throw new AssertionError(file + " unknown row class in triage: " + line);
                }
            }
            if (parts[0].equals(cell)) {
                (parts[1].startsWith("CLASS:") ? classKeys : fileKeys).add(parts[1]);
            }
        }
        return new Triage(classKeys, fileKeys);
    }
}
