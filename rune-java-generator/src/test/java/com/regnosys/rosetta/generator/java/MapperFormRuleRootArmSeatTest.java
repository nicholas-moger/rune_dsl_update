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
 * SEAT 31, law 1b -- facet {@code mapperFormRuleRootArmKeep}: <b>a RULE then-body-ROOT
 * mapper-form slot does not re-present a Mapper-chain arm, and its type-less META arm derefs
 * in-chain.</b> The mas {@code PlatformIdentifierRule} twin the seat-30 law-9 bank carried as
 * "the re-wrap half, producer not identified" -- the producer IS identified now, by measurement.
 *
 * <p><b>The seat, measured.</b> mas {@code PlatformIdentifier} never reaches
 * {@code FunctionExpressionRenderer.appendIteHoistChainCore} (its first rung is a nested-THEN
 * whose inner ladder has a conditional else, which FER declines); it falls through to
 * {@code ControlFlowHandler.hoistAsItemLocalOrNull}, whose {@code mapperFormSlot} disjunct
 * {@code ruleThenBodyRootSlot} fired on exactly SIX rules corpus-wide
 * ({@code [P31-MASFORM]}, 21 rows, ROUTE-IDENTICAL): mas {@code PlatformIdentifier} (String,
 * 1 row), {@code CryptoAssetUnderlyingIndicatorLeg1/2} (Boolean, 4 rows each),
 * {@code UnderlyingAssetPriceSourceLeg1/2} (String, 4 rows each) and
 * {@code UnderlyingIdentificationType} ({@code UnderlyingIdentificationTypeEnum}, 4 rows) --
 * the draft said FIVE, which the 21-row total never footed (5 x 4 + 1 = 21 needs the sixth;
 * corrected at the review of #603, MF-2). Of the SIX, ONLY mas reaches the arm
 * renderer's TERMINAL ({@code [P31-MASWRAP]} fired twice, both on mas: an {@code RExtractExpr}
 * arm and an {@code RFeatureCall} arm) -- the other five exit through the earlier
 * bool-hoist/enum/bare-fn/unwrap arms. The terminal's
 * {@code mapperFormSlot ? "MapperS.of(" + src + ".get())"} re-present is golden-WRONG at the
 * rule root: golden assigns the Mapper chain DIRECT (B238/B239) and derefs the construct-block
 * arm in-chain with the trailing {@code .<String>map("Type coercion", ...getValue())} (B245).
 *
 * <p><b>The law, two rungs, both gated {@code mapperFormSlot && ruleThenBodyRootSlot}:</b>
 * rung i keeps a Mapper-chain arm AS the Mapper it already is (direct assign); rung ii derefs a
 * TYPE-LESS arm whose META wrapper is recoverable from the arm NODE with
 * {@code valueType == declClazz} -- the SAME unrendered-arm coerce channel law 1's rung B uses
 * at the FER seat, handed an OPEN CHILD scope (the seat-31 closed-scope law generalizes to any
 * statement-seat coercion). The {@code fn:Create_*} projection greens ride the OTHER
 * {@code mapperFormSlot} disjuncts and keep the wrap byte-identically.
 *
 * <p><b>RED at the pre-law head</b>: corpus_c1 (the mas whole-file lock). The fixture a1
 * renders the mas SHAPE (the nested-THEN inner-else that FER declines) and pins the post-law
 * arm form. corpus_control1 (the six-path {@code RULE_ROOT_GREENS} lock, drr 7.0.0 -- four
 * distinct rules over two namespaces) is GREEN in BOTH states. {@code UnderlyingIdentificationType}
 * fires the gate too ({@code [P31-MASFORM]} x4) but has ZERO {@code [P31-MASWRAP]} terminal rows,
 * so it is deliberately NOT in the lock: the law cannot reach it.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82) at the seat-31 chain head {@code f2a4d5c0}:</b>
 * <ul>
 *   <li><b>m-law1b</b> ({@code law1bs-apply.py --revert}): MEASURED 4/2F/1S = a1, corpus_c1 --
 *       exactly the claim (MATCH). The chain's own copy of this lane was TRUNCATED by an external
 *       {@code java.exe} kill (log stops at the surefire banner, no {@code Tests run:} line -- not a
 *       measurement); the set above is the standalone re-measurement at the same head
 *       ({@code f31-mut-m-law1b-rerun.log}).</li>
 *   <li><b>m-law1b-rung2</b> ({@code --mut-rung2}, the deref channel severed): MEASURED 4/2F/1S =
 *       a1, corpus_c1 -- exactly the claim (MATCH; the deref line vanishes, rung i alone cannot
 *       match golden). corpus_control1 GREEN under both lanes, as claimed.</li>
 * </ul>
 * RED at the seat's base {@code 0c8fc7dc4} (both routes): a1, corpus_c1 (+ corpus_control2 on
 * {@code -Pir-on}); GREEN at the head 4/0F/1skip default, 4/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW 81 -- the residue rows this law MOVES</b> (re-pinned in this commit from their own
 * prints): the mas rows in {@code IteElseArmRecoveredMetaDerefSeatTest} (corpus_c3 -- the
 * law-1-leaves-mas-untouched pin flips to a positive whole-golden lock -- and control1's
 * residue), {@code DispatchBaseInputDeclineSeatTest}, {@code ChainMapperCRootRungsSeatTest},
 * {@code FilterPredicateMetaDerefSeatTest}, {@code ThenWrappedDefaultSeatTest},
 * {@code BlockLambdaSingleItemChainStampSeatTest}, {@code SetTerminalRuleMultiSeatTest}.
 */
class MapperFormRuleRootArmSeatTest {

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

    private static final String PI_MAS =
            "drr/regulation/mas/rewrite/trade/reports/PlatformIdentifierRule.java";

    /** Cell A = drr 5.61.0 -- the carrier cell. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.0.0 -- the six-path ruleThenBodyRootSlot GREENS (four distinct rules over
     *  two namespaces) live here. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    private static final List<String> RULE_ROOT_GREENS = List.of(
            "drr/regulation/csa/rewrite/trade/reports/CryptoAssetUnderlyingIndicatorLeg1Rule.java",
            "drr/regulation/csa/rewrite/trade/reports/CryptoAssetUnderlyingIndicatorLeg2Rule.java",
            "drr/regulation/csa/rewrite/trade/reports/UnderlyingAssetPriceSourceLeg1Rule.java",
            "drr/regulation/csa/rewrite/trade/reports/UnderlyingAssetPriceSourceLeg2Rule.java",
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetPriceSourceLeg1Rule.java",
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetPriceSourceLeg2Rule.java");

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
     * The fixture: the mas SHAPE reduced from the real source -- the leading filter pipe, a
     * nested-THEN then arm whose inner ladder carries a conditional else (exactly what makes
     * FER decline the ladder and fall through to CFH), and an {@code extract <meta-output
     * rule>} else arm (the #265 construct block, type-less, wrapper recoverable from the
     * node). Lexer-safe identifiers.
     */
    private static final String MODEL = """
            namespace census.seat31mf
            version "1.0.0"

            type Seat31Ident:
                ident string (0..1)
                    [metadata scheme]

            type Seat31Holder:
                primaryIdents Seat31Ident (0..*)
                otherIdents Seat31Ident (0..*)

            type Seat31Venue:
                code string (0..1)
                venueCode string (0..1)
                holder Seat31Holder (0..1)
                mark string (0..1)

            func Seat31Allow: <"the IsAllowableActionForMAS twin">
                inputs:
                    venue Seat31Venue (1..1)
                output:
                    ok boolean (1..1)
                set ok:
                    venue -> mark exists

            reporting rule Seat31InnerIdent from Seat31Venue: <"the META-OUTPUT inner rule">
                extract holder -> primaryIdents -> ident first
                as "inner"

            reporting rule A1MasShapedMapperFormArm from Seat31Venue: <"a1 - the mas shape REDUCED FROM the real source: a nested literal ladder then-arm (whose inner else-if makes FER decline), a bare-nav else-if arm, and a bare `extract <meta-output rule>` terminal else; CFH renders the mapper-form slot - post-law the nav arm assigns DIRECT and the construct arm derefs in-chain">
                filter Seat31Allow
                then if holder -> primaryIdents is absent
                        and venueCode exists
                    then (if venueCode = "A"
                        then "XOFF"
                        else if venueCode = "B"
                        then "XXXX")
                    else if code exists
                    then code
                    else extract Seat31InnerIdent
                as "a1"
            """;

    private static final String DEREF =
            ">map(\"Type coercion\", fieldWithMetaString -> fieldWithMetaString == null"
            + " ? null : fieldWithMetaString.getValue())";

    /**
     * a1 -- the fixture heals: the construct-block else arm derefs in-chain (rung ii) and no
     * arm is re-presented through {@code MapperS.of(<arm>.get())} (rung i).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_masShapedRuleRootArmsAssignDirectAndDerefInChain() throws IOException {
        String raw = rule("A1MasShapedMapperFormArmRule.java");
        String flat = collapse(raw);
        assertTrue(flat.contains(DEREF),
                "the construct-block arm must deref in-chain at the rule-root mapper-form"
                + " slot (rung ii):\n" + raw);
        // Rung i's fixture witness is the corpus lock's byte-identity: a token-level negative
        // here ("no MapperS.of(<arm>.get()) wrap") is not writable without a fragile match -
        // the construct block's own `evaluate(item.get());` carries the same tail - so rung i
        // rests on corpus_c1 (whole-file) + m-law1b (the sever lane flips BOTH rungs).
    }

    /** corpus_c1 -- the mas whole-file heal. RED pre-law, GREEN after. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr561MasPlatformIdentifierRuleMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(PI_MAS)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + PI_MAS + ": " + own);
        String gen = drrAOutput.get(PI_MAS);
        assertNotNull(gen, "not generated: " + PI_MAS);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(PI_MAS))), normalize(gen),
                "mas PlatformIdentifierRule must byte-match golden - seat 31 law 1b: the"
                + " rule-root mapper-form arms assign direct and the construct arm derefs"
                + " in-chain");
    }

    /**
     * corpus_control1 -- the {@code ruleThenBodyRootSlot} GREENS (drr 7.0.0): six paths, four
     * distinct rules over two namespaces, byte-locked in BOTH states: they exit the arm renderer
     * through earlier arms, so this law must not move them ({@code [P31-MASWRAP]} measured ZERO
     * terminal rows on them). {@code UnderlyingIdentificationType} (the sixth gate-firing rule)
     * likewise has ZERO terminal rows and is deliberately unlocked -- the probe shows one
     * terminal carrier (mas) and LAW 80 covers the cell.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control1_ruleRootGreensStayByteIdentical() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run");
        for (String path : RULE_ROOT_GREENS) {
            String gen = drrBOutput.get(path);
            assertNotNull(gen, "not generated in drr 7.0.0: " + path);
            assertEquals(normalize(Files.readString(GOLDEN_B.resolve(path))), normalize(gen),
                    "the ruleThenBodyRootSlot GREEN must stay byte-identical: " + path);
        }
    }

    /**
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN (the seat-30 standing lesson): the IR
     * route substitutes FER but INHERITS ControlFlowHandler, so the heal must arrive on the ON
     * route too, and against GOLDEN rather than mere parity.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForMas() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(PI_MAS))),
                normalize(irOut.get(PI_MAS)), "IR route vs GOLDEN: " + PI_MAS);
    }

    // =========================================================================
    // Harness (the IteElseArmRecoveredMetaDerefSeatTest shape, trimmed)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_B_ROOT),
                    new ArrayList<>());
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
                    "the IR provider must be resolvable under -Pir-on");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator");
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean inWs = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                inWs = true;
                continue;
            }
            if (inWs && sb.length() > 0) {
                sb.append(' ');
            }
            inWs = false;
            sb.append(ch);
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat31mf.rosetta");
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
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat31mf".equals(m.namespace()));
        }
        return fixtureOut;
    }

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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[MapperFormRuleRootArmSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
