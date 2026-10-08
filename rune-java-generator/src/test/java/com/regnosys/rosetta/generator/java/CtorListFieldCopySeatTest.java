package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 28, law 13 — facet {@code ctorListFieldNoCopy}: <b>the defensive
 * {@code new ArrayList<>(<call>.getMulti())} copy at a ctor list-field seat is emitted only when
 * the value's emitted Mapper signature carries a WILDCARD element</b> — i.e. only for a
 * generated-pojo (data / choice) element. An ENUM or builtin element splices BARE.
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0
 * {@code drr/ingest/fpml/recordkeeping/reportableinfo/functions/MapCorporateSector.java}, ONE
 * hunk — and note the CONTEXT line, which both sides agree on):
 * <pre>
 * golden: .setFinancialSector(financialSector(corporateSectorScheme, fpmlParty).getMulti())
 * fork  : .setFinancialSector(new ArrayList&lt;&gt;(financialSector(corporateSectorScheme, fpmlParty).getMulti()))
 * both  : .setNonFinancialSector(new ArrayList&lt;&gt;(nonFinancialSector(corporateSectorScheme, fpmlParty).getMulti()))
 * </pre>
 * <b>The positive and negative witnesses are two lines apart in ONE golden ctor</b>, and the
 * golden's own alias signatures say why:
 * <pre>
 * 53: protected abstract MapperC&lt;FinancialSectorEnum&gt; financialSector(String corporateSectorScheme, Party fpmlParty);
 * 55: protected abstract MapperC&lt;? extends NonFinancialSector&gt; nonFinancialSector(String corporateSectorScheme, Party fpmlParty);
 * </pre>
 * {@code List<FinancialSectorEnum>} assigns straight into the setter; {@code List<? extends
 * NonFinancialSector>} does not. Both forms COMPILE, so this law has <b>no LAW-74 PRE error of its
 * own</b> — it is a pure byte-fidelity law.
 *
 * <p><b>It is a LAW-69 two-halves-disagree, and both halves are in one file.</b>
 * {@code ConstructionHandler} arm C2r (the List-returning {@code evaluate(...)} branch,
 * pristine {@code :628}) consults {@code ctorValueItemIsBareSpliced} — whose javadoc quotes
 * {@code TypeCoercionService.xtend:342-344} and the wildcard law verbatim — while the #232
 * {@code ctorArgArrayListCopy} arm ({@code :759-764}) wraps unconditionally. A second gap sits
 * inside the shared predicate: it classifies {@code RAttribute} / {@code RFunction} / (seat 25)
 * {@code RRule} values and has <b>no {@code RShortcut} (alias) arm</b> — and both carriers are
 * aliases.
 *
 * <p><b>LAW 75 — the #232 javadoc's corpus census is REFUTED and re-derived at this head.</b> It
 * claims "every golden direct-call multi ctor-setter wraps (41 sites, 0 bare)". Measured over all
 * 179,209 corpus goldens: <b>233 wrapped occurrences (20 distinct setter/callee pairs, every one a
 * DATA-type element) and 26 bare (4 distinct)</b> — of which 3 are the PR #214 statement-seat
 * setters the javadoc already excluded ({@code setValuationHistory}, {@code setTradeLot},
 * {@code setAncillaryParty} at the {@code getOrCreate().set()} seat) and the fourth is this
 * carrier. <b>The element-kind discriminator separates wrapped from bare with ZERO exceptions.</b>
 * Per cell: cdm 6.20.6 20/3, cdm 6.23.0 21/3, drr 7.0.0 9/1, drr 5.61.0 · rune-fpml · iso20022 0/0.
 *
 * <p><b>RED at the pre-law head</b>: a1, a2, corpus_c1, corpus_control1.
 * a3, b1 and corpus_control0 are GREEN in BOTH states.
 *
 * <p><b>LAW 66/76 mutation sets — CLAIMS until the seat's chain measures them (LAW 82).</b>
 * See {@code law9-13-notes.md}; rewrite this paragraph FROM the chain's logs before merge.
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>the whole law reverted (law13-apply --revert) -> a1, corpus_c1, corpus_control1 (3F)</li>
 * </ul>
 */
class CtorListFieldCopySeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** The carrier cell: 9 wrapped ctor-arg sites that must not move + the 1 bare that must appear. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** The reach cell and the real over-fire risk: 20 wrapped (all data-element) + 3 statement-seat bare. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    /**
     * A1/A3 = the {@code MapCorporateSector} shape in ONE ctor: an ENUM-element ALIAS value (bare)
     * beside a DATA-element ALIAS value (wrapped). A2 = the ENUM-element direct FUNCTION call —
     * the arm {@code ctorValueItemIsBareSpliced} ALREADY classifies, so it isolates the LAW-69
     * consult (design edit 1) from the alias arm (edit 2). B1 = the decline pin: a DATA-element
     * direct FUNCTION call keeps its copy.
     *
     * <p>{@code note} is used instead of {@code tag} — {@code tag} is a lexer keyword.
     */
    private static final String MODEL = """
            namespace census.seat28l
            version "1.0.0"

            enum SectorEnum:
                Alpha
                Beta

            type Sub:
                code string (0..1)

            type Bag:
                sectors SectorEnum (0..*)
                subs Sub (0..*)

            type Src:
                sectors SectorEnum (0..*)
                subs Sub (0..*)
                note string (0..1)

            func SectorsOf: <"an ENUM-element direct FN call value">
                inputs:
                    src Src (1..1)
                output:
                    result SectorEnum (0..*)
                set result:
                    src -> sectors

            func SubsOf: <"a DATA-element direct FN call value">
                inputs:
                    src Src (1..1)
                output:
                    result Sub (0..*)
                set result:
                    src -> subs

            func A1CtorAliasElementKinds: <"a1 + a3 - the MapCorporateSector shape, both verdicts in ONE ctor">
                inputs:
                    src Src (1..1)
                output:
                    bag Bag (1..1)
                alias sectorList: src -> sectors
                alias subList: src -> subs
                set bag:
                    Bag {
                        sectors: sectorList,
                        subs: subList
                    }

            func A2CtorFnCallEnumElement: <"a2 - an ENUM-element direct FN call: bare">
                inputs:
                    src Src (1..1)
                output:
                    bag Bag (1..1)
                set bag:
                    Bag {
                        sectors: SectorsOf(src)
                    }

            func B1CtorFnCallDataElement: <"b1 - a DATA-element direct FN call KEEPS the copy">
                inputs:
                    src Src (1..1)
                output:
                    bag Bag (1..1)
                set bag:
                    Bag {
                        subs: SubsOf(src)
                    }
            """;

    // =========================================================================
    // Part A — the fixtures
    // =========================================================================

    /**
     * a1 — the ENUM-element ALIAS value splices BARE. This is the half that needs BOTH design
     * edits: the LAW-69 consult AND the new {@code RShortcut} arm in
     * {@code ctorValueItemIsBareSpliced}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_enumElementAliasCtorValueSplicesBare() throws IOException {
        String out = fn("A1CtorAliasElementKinds.java");
        assertContains(out, ".setSectors(sectorList(src).getMulti())");
        assertTrue(!codeOnly(out).contains("new ArrayList<>(sectorList(src).getMulti())"),
                "an ENUM-element list needs no defensive copy:\n" + out);
    }

    /**
     * a2 — the ENUM-element direct FUNCTION call splices BARE. {@code ctorValueItemIsBareSpliced}
     * already classifies an {@code RFunction} value, so this file moves on design edit 1 ALONE —
     * it is what measures the LAW-69 consult apart from the alias arm.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_enumElementFnCallCtorValueSplicesBare() throws IOException {
        // MEASURED at the RED head: a direct FN-CALL ctor value does NOT reach the #232
        // arm at all - it splices through the direct-evaluate channel
        // (`sectorsOf.evaluate(src)`, no Mapper chain, no .getMulti()). The draft's
        // `.getMulti()` expectation described the alias class, not this one. Pinned as a
        // DECLINE: the fn-call channel is not this law's seat and must not move.
        String out = fn("A2CtorFnCallEnumElement.java");
        assertContains(out, ".setSectors(sectorsOf.evaluate(src))");
    }

    /**
     * a3 — the in-file NEGATIVE control, the fixture twin of golden's own
     * {@code setNonFinancialSector} line: a DATA-element ALIAS value in the SAME ctor KEEPS the
     * copy. If the alias arm classified by "is an alias" instead of "what element does the alias
     * produce", this assert fails.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_dataElementAliasCtorValueKeepsTheCopy() throws IOException {
        String out = fn("A1CtorAliasElementKinds.java");
        assertContains(out, ".setSubs(new ArrayList<>(subList(src).getMulti()))");
        assertContains(out, "import java.util.ArrayList;");
    }

    /**
     * a4 — the WILDCARD oracle inside the fixture: the emitted alias signatures are the reason the
     * copy exists, so the suite asserts them directly rather than trusting the byte alone (the
     * seat-27 "prove the mechanism, not just the byte" discipline).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_aliasSignaturesCarryTheWildcardOnlyForThePojoElement() throws IOException {
        String out = fn("A1CtorAliasElementKinds.java");
        assertContains(out, "MapperC<SectorEnum> sectorList(");
        assertContains(out, "MapperC<? extends Sub> subList(");
    }

    /**
     * b1 — the decline pin (LAW 76 witness-uniqueness). A DATA-element direct FUNCTION call must
     * KEEP its copy, and the witness is a token this law REMOVES when it over-fires — the
     * {@code new ArrayList<>(} wrap itself — asserted PRESENT on {@code codeOnly}. Asserting only
     * "not bare" would also pass if the file failed to generate.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_dataElementFnCallKeepsTheCopy() throws IOException {
        // MEASURED at the RED head: the DATA-element direct FN call keeps its own
        // pre-existing copy form - the raw `new ArrayList(...)` of the direct-evaluate
        // channel (a different emitter from the #232 arm's `new ArrayList<>(...getMulti())`).
        // Pinned as-is: this law must not move the fn-call channel in either direction.
        String out = fn("B1CtorFnCallDataElement.java");
        assertContains(out, ".setSubs(new ArrayList(subsOf.evaluate(src)))");
    }

    // =========================================================================
    // Part C — the corpus carrier (4 whole-file rows, one per drr 7.x cell)
    // =========================================================================

    private static final String CARRIER_A =
            "drr/ingest/fpml/recordkeeping/reportableinfo/functions/MapCorporateSector.java";

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_mapCorporateSectorByteIdentical() throws IOException {
        lockA(CARRIER_A);
    }

    /**
     * control0 — the strongest oracle in the seat: BOTH verdicts AND their cause are in ONE golden
     * file. Assert the two setter forms and the two alias signatures, so the oracle proves the
     * WILDCARD law and not merely the byte.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesBothVerdictsAndTheirCause() throws IOException {
        String golden = Files.readString(GOLDEN_A.resolve(CARRIER_A));
        assertTrue(golden.contains(
                ".setFinancialSector(financialSector(corporateSectorScheme, fpmlParty).getMulti())"),
                "golden splices the ENUM-element list BARE");
        assertTrue(golden.contains(
                ".setNonFinancialSector(new ArrayList<>(nonFinancialSector(corporateSectorScheme,"
                + " fpmlParty).getMulti()))"),
                "golden copies the DATA-element list");
        assertTrue(golden.contains("MapperC<FinancialSectorEnum> financialSector("),
                "the ENUM-element alias signature carries NO wildcard — the cause of the bare splice");
        assertTrue(golden.contains("MapperC<? extends NonFinancialSector> nonFinancialSector("),
                "the DATA-element alias signature carries the wildcard — the cause of the copy");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the
     * (wrapped ctor-arg copies, bare direct-call list setters, {@code java.util.ArrayList} import)
     * triple must equal golden's, file for file over the UNION, beyond the NAMED residue. T3 is
     * the refs half of the edit — a healed site that is a file's ONLY copy site must also drop the
     * import.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellCtorCopiesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /** control2 — LAW 77 route parity for the drr 7.0.0 carrier. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(CARRIER_A), irOut.get(CARRIER_A),
                "route divergence: " + CARRIER_A);
    }

    /**
     * control3 — LAW 79 on cdm 6.20.6, which holds the law's whole over-fire population: 20 wrapped
     * ctor-arg copies (every one a DATA-type element, so every one must stay) plus the 3
     * statement-seat bare setters this law must not touch.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkCdm6WholeCellCtorCopiesEqualGoldenFileByFile() throws IOException {
        assertNotNull(cdmBOutput, "cdm 6.20.6 generation did not run");
        assertEquals(List.of(), cdmBGenErrors,
                "cdm 6.20.6 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(cdmBOutput), scan(readGoldenTree(GOLDEN_B)), cdmBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM6, DOMAIN_CDM6);
    }

    /** MEASURED EMPTY at the chain head c6871ed5 (LAW 73: pin the SET, not the count). */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    /** MEASURED EMPTY at the chain head c6871ed5. */
    private static final List<String> KNOWN_RESIDUE_CDM6 = List.of();

    /** MEASURED at the chain head c6871ed5: the drr 7.0.0 union domain (golden ∪ fork token-bearing files). */
    private static final int DOMAIN_DRR7 = 7;

    /** MEASURED at the chain head c6871ed5: the cdm 6.20.6 union domain. */
    private static final int DOMAIN_CDM6 = 14;

    /**
     * (T1, T2, T3) = wrapped ctor-arg copies, bare direct-call list setters, and the
     * {@code java.util.ArrayList} import (0 or 1).
     */
    private static final Pattern WRAPPED = Pattern.compile(
            "\\.set[A-Za-z0-9]+\\(new ArrayList<>\\([a-zA-Z0-9_.]+\\([^()]*\\)\\.getMulti\\(\\)\\)\\)");

    private static final Pattern BARE = Pattern.compile(
            "\\.set[A-Za-z0-9]+\\([a-zA-Z0-9_.]+\\([^()]*\\)\\.getMulti\\(\\)\\)");

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            int t2 = 0;
            Matcher m1 = WRAPPED.matcher(code);
            while (m1.find()) {
                t1++;
            }
            Matcher m2 = BARE.matcher(code);
            while (m2.find()) {
                t2++;
            }
            int t3 = e.getValue().contains("import java.util.ArrayList;") ? 1 : 0;
            if (t1 + t2 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73) + harness — the seat-27 shape verbatim
    // =========================================================================

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        int[] zero = new int[3];
        for (String key : universe) {
            if (!emittedA.contains(key)) {
                notEmitted.add(key);
                continue;
            }
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "token-bearing files " + bName + " carries that " + aName + " does not emit at all");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> cdmBOutput;
    private static List<String> cdmBGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            cdmBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_B_ROOT), errs);
            cdmBGenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    private static Map<String, String> generateCellOnIrRoute(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
                }
            }
            collect(errors, funcGen.generateWithErrors(output));
            return output;
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static Map<String, String> readGoldenTree(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> {
                try {
                    out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
                } catch (IOException e) {
                    throw new AssertionError("golden read failed: " + p, e);
                }
            });
        }
        return out;
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 28 law 13: the ctor list-field defensive copy is emitted only"
                + " for a WILDCARD (generated-pojo) element.");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    /** Strip line and block comments plus string literals so a javadoc or label never counts as code. */
    private static String codeOnly(String java) {
        StringBuilder sb = new StringBuilder(java.length());
        int i = 0;
        int n = java.length();
        while (i < n) {
            char ch = java.charAt(i);
            if (ch == '"') {
                int j = i + 1;
                while (j < n && java.charAt(j) != '"') {
                    if (java.charAt(j) == '\\') {
                        j++;
                    }
                    j++;
                }
                i = j + 1;
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '/') {
                while (i < n && java.charAt(i) != '\n') {
                    i++;
                }
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '*') {
                int e = java.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat28l.rosetta");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
            mainModel = main;
        }
    }

    private static Map<String, String> render(Predicate<RModel> filter) throws IOException {
        link();
        GeneratorModel gm = new GeneratorModel(linking.workspace(), filter);
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat28l".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String fn(String fileName) throws IOException {
        return lookup(fixture(), "functions/" + fileName);
    }

    private static String lookup(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "'" + suffix + "' was not generated; keys=" + output.keySet()));
    }

    private static List<RModel> loadBuiltinsOnly() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<String> failures = new ArrayList<>();
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try {
                        models.add(AstBuilder.buildFromFile(p));
                    } catch (Exception e) {
                        failures.add(p + " — " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[CtorListFieldCopySeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
