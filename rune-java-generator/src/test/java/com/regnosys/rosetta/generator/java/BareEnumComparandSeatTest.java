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
 * SEAT 28, law 10 — facet {@code bareEnumAliasShadowComparand} (chartered as
 * {@code bareEnumComparandNestedArm}, a MISNOMER the design corrects): <b>a bare enum comparand
 * whose simple name is shadowed by a {@code typeAlias} declaration in scope still qualifies to
 * {@code <Enum>.<CONSTANT>}</b> — the FOURTH member of the #239/#299 shadow family
 * ({@link com.regnosys.rosetta.ast.types.RDataType} → {@code RChoice} → {@code RBody} →
 * {@code RTypeAlias}).
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0
 * {@code drr/regulation/hkma/rewrite/trade/reports/ReferenceEntityFormatRule.java}, ONE hunk):
 * <pre>
 *   if (areEqual(thenArg, MapperS.of(EntityIdentifierTypeEnum.LEI), CardinalityOperator.All)…) {   // BOTH agree
 * golden: } else if (areEqual(thenArg, MapperS.of(EntityIdentifierTypeEnum.COUNTRY_CODE), …)) {
 * fork  : } else if (areEqual(thenArg, MapperS.of(CountryCode),                          …)) {
 * </pre>
 * {@code MapperS.of(CountryCode)} is an undefined Java symbol — <b>LAW 74</b>: the PRE javac probe
 * must show it and the POST must exit 0. <b>The sibling arm two lines above is byte-identical in
 * golden and fork</b> — {@code LEI} qualifies, {@code CountryCode} does not — and that asymmetry
 * inside one {@code areEqual} ladder over one sibling is the whole diagnostic.
 *
 * <p><b>TWO prior sitings are REFUTED and this suite pins the correction.</b>
 * <ul>
 *   <li>The census ({@code v31-close-census.md} F15(c)) blamed
 *       {@code NavigationHandler.bareItemPipeSiblingEnumeration}'s {@code ownerNode instanceof
 *       RThenExpr} gate. {@code PROBE28-F15c1} measures that rung resolving an enumeration
 *       <b>ZERO times corpus-wide</b> ({@code pipeRung=(null)} in 17,539 of 17,539 lines) and
 *       produces ZERO lines on this carrier — it is never consulted.</li>
 *   <li>The F14/F15 lens inferred a Category-16 BIND stamp and proposed
 *       {@code HandlerHelper.boundBareEnumValue} as a third {@code valueName} rung. That predicts
 *       the SAME decline for {@code LEI}, which renders correctly — so it cannot be the cause.</li>
 * </ul>
 * <b>The measured cause</b>: {@code typeAlias CountryCode: string(pattern: "[A-Z]{2,2}")}
 * (drr {@code standards-iso-type.rosetta:45}) is pulled into the hkma rule namespace by
 * {@code import drr.standards.iso.*}, so the lexical pass binds the bare name to that
 * {@code RTypeAlias}; the parser's Category-14/16 enum bind declines to override ANY present
 * symbol; and {@code HandlerHelper.typeShadowEnumValueName} filters only
 * {@code RDataType | RChoice | RBody}, so {@code tryBareEnumComparand} exits at its
 * {@code valueName == null} guard before rung 1. {@code LEI} has NO declaration anywhere in the
 * cell, stays symbol-EMPTY, is bind-stamped, and renders through
 * {@code ReferenceHandler.handle(RSymbolReference)}'s bound bare-enum arm — the in-file positive
 * control.
 *
 * <p><b>Blast radius (measured).</b> 173 distinct {@code typeAlias} names corpus-wide; exactly
 * FOUR also name an enum value ({@code CountryCode}, {@code Initial}, {@code Percentage},
 * {@code Scheme}); the last three are declared ONLY in rune-fpml's
 * {@code consolidated-shared-type.rosetta}, a namespace the enum-value use sites do not import, so
 * their bare uses stay symbol-EMPTY and the arm is inert for them. <b>0 of 179,209 goldens carry
 * {@code MapperS.of(<Capitalized bare>)}</b> (positive control: the qualified form returns 18
 * files).
 *
 * <p><b>RED at the pre-law head</b>: a1, corpus_c1, corpus_control1.
 * a2 (the unshadowed sibling) and b1 (the decline pin) are GREEN in BOTH states.
 *
 * <p><b>LAW 66/76 mutation sets — CLAIMS until the seat's chain measures them (LAW 82).</b>
 * See {@code law9-13-notes.md}; rewrite this paragraph FROM the chain's logs before merge.
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>HALF A reverted (law10-apply --revert: the RTypeAlias shadow kind removed) -> a1, corpus_c1, corpus_control1 (3F)</li>
 *   <li>HALF B reverted (law10b-apply --revert: the extract pipe-base unwrap removed) -> a1, corpus_c1, corpus_control1 (3F - the halves are conjunctively necessary)</li>
 * </ul>
 */
class BareEnumComparandSeatTest {

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

    /** The carrier cell. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** The reach cell — the #447 RESOLVED-arm population and the EffectiveDateRule token twin. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
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
     * A1 = the {@code ReferenceEntityFormatRule} shape verbatim: a func-headed producing extract,
     * a then-step ladder, an elided-left first arm with an UNSHADOWED bare value, and an explicit
     * {@code item} second arm with a value shadowed by a {@code typeAlias} in the SAME file.
     * B1 = the decline pin: the SAME shadowed name compared against a sibling whose enum does NOT
     * declare it — the value-match gate declines and the bare name survives.
     *
     * <p>{@code note} is used instead of {@code tag} — {@code tag} is a lexer keyword.
     */
    private static final String MODEL = """
            namespace census.seat28j
            version "1.0.0"

            typeAlias CountryCode: string(pattern: "[A-Z]{2,2}")

            enum IdTypeEnum:
                LEI
                CountryCode

            enum FormatEnum:
                Lei
                Country
                Other

            enum ShapeEnum:
                Round
                Square

            type Ent:
                idType IdTypeEnum (0..1)
                shape ShapeEnum (0..1)
                note string (0..1)

            type Holder:
                ent Ent (0..1)

            func ExtractEnt: <"the func-headed producer - mirrors ExtractReferenceEntity">
                inputs:
                    h Holder (1..1)
                output:
                    result Ent (0..1)
                set result:
                    h -> ent

            reporting rule A1AliasShadowComparand from Holder: <"a1 - a bare enum value shadowed by a typeAlias">
                extract ExtractEnt -> idType
                then if = LEI
                    then FormatEnum -> Lei
                    else if item = CountryCode
                    then FormatEnum -> Country

            reporting rule B1AliasShadowNoValueMatch from Holder: <"b1 - the shadow whose target enum has no such value">
                extract ExtractEnt -> shape
                then if item = CountryCode
                    then FormatEnum -> Other
            """;

    // =========================================================================
    // Part A — the fixtures
    // =========================================================================

    /**
     * a1 — BOTH arms qualify. The first (elided-left, unshadowed {@code LEI}) is the in-fixture
     * positive control that must be green in BOTH states; the second (explicit {@code item},
     * alias-shadowed {@code CountryCode}) is the law.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_aliasShadowedBareEnumComparandQualifies() throws IOException {
        String out = rule("A1AliasShadowComparandRule.java");
        assertContains(out, "MapperS.of(IdTypeEnum.LEI)");
        assertContains(out, "MapperS.of(IdTypeEnum.COUNTRY_CODE)");
        assertTrue(!codeOnly(out).contains("MapperS.of(CountryCode)"),
                "the alias-shadowed bare name must not survive as a Java identifier:\n" + out);
    }

    /**
     * a2 — the sibling asymmetry is the diagnostic: the UNSHADOWED value was always correct, so
     * this assert must hold at the PRE-law head too. It is the instrument's own positive control
     * (LAW: prove the instrument can fail — if a1 goes green by breaking LEI, a2 catches it).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_unshadowedSiblingWasAlwaysQualified() throws IOException {
        String out = rule("A1AliasShadowComparandRule.java");
        assertContains(out, "MapperS.of(IdTypeEnum.LEI)");
        assertTrue(!codeOnly(out).contains("MapperS.of(LEI)"),
                "the unshadowed sibling must stay qualified:\n" + out);
    }

    /**
     * b1 — the decline pin (LAW 76 witness-uniqueness). The SAME alias-shadowed name against a
     * sibling typed {@code ShapeEnum}, which declares no {@code CountryCode} value: the ladder's
     * value-match gate declines and the file keeps TODAY's bytes. The witness is the token the
     * flip REMOVES — the bare name itself — asserted PRESENT on {@code codeOnly}; asserting only
     * "no qualified form" would also pass if the file failed to generate.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_shadowedNameWithNoValueMatchKeepsTodaysBytes() throws IOException {
        String out = rule("B1AliasShadowNoValueMatchRule.java");
        String code = codeOnly(out);
        assertTrue(code.contains("MapperS.of(CountryCode)"),
                "a shadow whose target enum has no such value must keep the bare name:\n" + out);
        assertTrue(!code.contains("ShapeEnum.COUNTRY_CODE"),
                "law 10 must not invent a constant on an unrelated enum:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carriers (4 whole-file rows, one per drr 7.x cell)
    // =========================================================================

    private static final String CARRIER_A =
            "drr/regulation/hkma/rewrite/trade/reports/ReferenceEntityFormatRule.java";

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_referenceEntityFormatRuleByteIdentical() throws IOException {
        lockA(CARRIER_A);
    }

    /**
     * control0 — golden is the oracle: BOTH arms of the ladder carry a QUALIFIED constant, and
     * golden carries no bare-name operand at all.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenQualifiesBothArms() throws IOException {
        String golden = Files.readString(GOLDEN_A.resolve(CARRIER_A));
        assertTrue(golden.contains("MapperS.of(EntityIdentifierTypeEnum.LEI)"),
                "golden must qualify the unshadowed sibling");
        assertTrue(golden.contains("MapperS.of(EntityIdentifierTypeEnum.COUNTRY_CODE)"),
                "golden must qualify the alias-shadowed operand");
        assertTrue(!golden.contains("MapperS.of(CountryCode)"),
                "golden must NOT carry the bare shadowed name");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the
     * (bare-Capitalized {@code MapperS.of} operands, qualified enum-constant operands,
     * {@code areEqual(} sites) triple must equal golden's, file for file over the UNION, beyond
     * the NAMED residue. T3 is present so a rung that changes the ladder's SHAPE — not just its
     * operand — is caught.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellEnumOperandsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /** control2 — LAW 77 route parity for the drr 7.0.0 carrier (the probe is 4=4 both routes). */
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
     * control3 — LAW 79: the same scan on drr 5.61.0, which carries BOTH the #447 RESOLVED-arm
     * population (the second consumer of {@code bareItemPipeSiblingEnumeration}, exposed by the
     * design's half B) and the ONE other band file whose T1 token is non-zero.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellEnumOperandsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /** MEASURED EMPTY at the chain head c6871ed5 (LAW 73: pin the SET, not the count). */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    /**
     * MEASURED at the chain head c6871ed5: EXACTLY ONE entry, with its measured triple —
     * {@code drr/regulation/asic/rewrite/trade/reports/EffectiveDateRule.java} — a 2-hunk band
     * file whose T1 token is {@code MapperS.of(TradeForEvent)}, a bare FUNCTION nav head (the
     * F13/F14 family, NOT this law). Pin the SET with its fork/golden triple and REMOVE the entry
     * when a later seat heals that file.
     */
    ///PIN: the ONE entry (the asic EffectiveDateRule row `fork=[1, 0, 0] golden=[0, 0, 0]`) LEFT this list
    ///PIN: at seat 32: law F.1 (ruleArmBareOutputEvidence - the effective-else block seat's baresym META
    ///PIN: then arm un-declined and deref'd beside a bare-output RULE else arm) healed the file WHOLE in
    ///PIN: drr 5.61.0; transcribed from this control's own print (F1-trip1.log): the residue is EMPTY.
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /** MEASURED at the chain head c6871ed5: the drr 7.0.0 union domain (golden ∪ fork token-bearing files). */
    private static final int DOMAIN_DRR7 = 1051;

    /** MEASURED at the chain head c6871ed5: the drr 5.61.0 union domain. */
    ///PIN: 821 -> 820 at seat 32 (law F.1): the healed asic file's GOLDEN triple is all-zero and its fork
    ///PIN: junk token is gone, so the file leaves the token-bearing union (this javadoc's own REMOVE rule).
    ///PIN: DERIVED, not transcribed - the residue assert fires before the domain assert, so F1-trip1.log
    ///PIN: carries no domain print; CONFIRMED by F1-trip2.log's green at 820 (8/0F), an equality assert
    ///PIN: that fails at every other value.
    private static final int DOMAIN_DRR561 = 820;

    /**
     * (T1, T2, T3) = bare-Capitalized {@code MapperS.of} operands, qualified
     * {@code MapperS.of(<Type>.<CONSTANT>)} operands, and {@code areEqual(} sites.
     */
    private static final Pattern BARE_OPERAND =
            Pattern.compile("MapperS\\.of\\([A-Z][A-Za-z0-9]*\\)");

    private static final Pattern QUALIFIED_OPERAND =
            Pattern.compile("MapperS\\.of\\([A-Za-z0-9_]+\\.[A-Z][A-Z0-9_]*\\)");

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            Matcher m1 = BARE_OPERAND.matcher(code);
            while (m1.find()) {
                t1++;
            }
            Matcher m2 = QUALIFIED_OPERAND.matcher(code);
            while (m2.find()) {
                t2++;
            }
            int i = code.indexOf("areEqual(");
            while (i >= 0) {
                t3++;
                i = code.indexOf("areEqual(", i + 1);
            }
            if (t1 + t2 + t3 > 0) {
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
        // Scoped to the files this harness emits (the seat-28 correction class).
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[3];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;

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
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
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
                + path + " — seat 28 law 10: a typeAlias-shadowed bare enum comparand qualifies.");
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat28j.rosetta");
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
            fixtureOut = render(m -> "census.seat28j".equals(m.namespace()));
        }
        return fixtureOut;
    }

    /** Reporting rules land under {@code .../reports/}. */
    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
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
            throw new AssertionError("[BareEnumComparandSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
