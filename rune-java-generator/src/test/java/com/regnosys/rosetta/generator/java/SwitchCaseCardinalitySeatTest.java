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
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * PR #610, C2d retirement family 3 {@code switch-case-cardinality} — the MULTI half of the
 * switch-block emitters' case-body cardinality decision moves off the RENDERED TEXT of the
 * compiled case and onto a typed channel.
 *
 * <p><b>The family, verbatim from the triage</b>
 * ({@code scripts/ci/evidence-api-triage.tsv}, family {@code switch-case-cardinality}):
 * FOUR ledger rows / FIVE live occurrences, all in
 * {@code rune-java-generator/.../expression/handlers/CollectionHandler.java}, all verdict
 * RETIRE-AFTER-CENSUS:
 * <ol>
 *   <li><b>row 1, occ 1</b> {@code boolean collapsingMulti = compiledCase.startsWith("MapperC")}
 *       — the seat-29 law-9 collapsing-MULTI admit in {@code compileEnumSwitchBlockLambda}. The
 *       AST kind ({@code RMinExpr}/{@code RMaxExpr}) already decides the COLLAPSE; only the
 *       MapperC cardinality half needs a typed answer. Witnessed by
 *       {@code EnumSwitchBlockWidenSeatTest}.</li>
 *   <li><b>row 2, occ 1</b> {@code if (!caseReturn.startsWith("MapperC.<")
 *       || caseReturn.contains("\n"))} — the MULTI model-choice list-block admit in
 *       {@code compileModelChoiceSwitchListBlockLambda} (PR #365). <b>THIS SUITE'S SEAT.</b> The
 *       fused newline leg is a LAYOUT test with no model counterpart, so only the cardinality
 *       half can ever move; the newline leg stays.</li>
 *   <li><b>row 3, occ 1</b> {@code if (compiledCase.startsWith("MapperC") && !collapsingMulti)}
 *       — the MapperC arm decline guarded by row 1's admit; retires under the same typed test.
 *       Witnessed by {@code EnumSwitchBlockWidenSeatTest}.</li>
 *   <li><b>row 4, occ 2</b> {@code if (compiledCase.startsWith("MapperC"))} at TWO seats — the
 *       instanceof-switch block ({@code compileChoiceSwitchBlockLambda}) and the SINGLE
 *       option-getter block ({@code compileChoiceOptionGetterBlockLambda}), both MULTI declines.
 *       Witnessed by {@code FunctionSwitchLambdaBlockTest} and
 *       {@code ChoiceSwitchLambdaOptionGetterSeatTest} respectively.</li>
 * </ol>
 * Three of the four switch-block emitters therefore already carried a unit witness. The FOURTH
 * — {@code compileModelChoiceSwitchListBlockLambda}, the MULTI model-choice LIST block — had
 * none: its only lock was the D11 whole-file ring. <b>This suite is that missing witness</b>, so
 * the swap at row 2 can be proven failing-first at the unit boundary and not only at the ring.
 *
 * <p><b>The retirement channel.</b> Every text read above answers ONE question — "is this case
 * body's compiled value MULTI?" — and retires onto
 * {@code CollectionHandler.caseBodyCompilesMulti(RExpression, ExpressionCompiler)}:
 * <ul>
 *   <li>a FUNCTION-CALL body (every corpus case body at these seats) consults
 *       {@code ReferenceHandler.mapperCOfWrapWitness} on the CALLEE'S OUTPUT attribute — the
 *       producer's OWN predicate, the very one {@code tryMultiValueWrap} renders the
 *       {@code MapperC.<X>of(…)} wrap from ({@code gm.isMulti(attr)} + the meta-wrapper arm), so
 *       the reading and the rendered chain cannot disagree. <b>LAW 69</b>: the call site sharing
 *       a law with its declaration CONSULTS it; a BARE implicit invocation (args empty) compiles
 *       unwrapped and answers {@code false};</li>
 *   <li>a {@code default} body renders {@code left.getOrDefault(right)} and is headed by its LEFT
 *       operand, so its MULTI half is the left operand's (the front-end types every
 *       {@code default} a conservative SINGLE — the seat mirrors the render head instead);</li>
 *   <li>every other body kind answers from the front-end {@code CardinalityComputer}
 *       ({@code CARDINALITY.compute(...) == ExpressionCardinality.MULTI}) — the typed answer the
 *       AST already carries; a min/max collapse arm passes its ARGUMENT.</li>
 * </ul>
 * The {@code contains("\n")} legs are NOT part of the swap (layout, no model counterpart) and
 * the {@code RMinExpr}/{@code RMaxExpr} AST kind test at row 1 is already typed and stays.
 *
 * <p><b>The corpus carrier this suite mirrors.</b> cdm 6.20.6
 * {@code cdm/product/template/functions/Create_CashflowFromPayout.java} — source
 * {@code product-template-func.rosetta}, {@code func Create_CashflowFromPayout}: a SINGLE
 * {@code Payout} (a model {@code choice}) input, {@code extract switch OptionPayout then …,
 * SettlementPayout then …, default empty} whose case bodies are EXPLICIT-ARGUMENT calls to
 * {@code Create_CashflowFromSettlementPayout} ({@code output: cashflows Cashflow (2..2)} — MULTI,
 * so each compiles {@code MapperC.<Cashflow>of(create_CashflowFromSettlementPayout.evaluate(…))}
 * and row 2's admit fires). Golden emits the option-PRESENCE ladder inside a
 * {@code mapSingleToList} block lambda with {@code MapperC.<Cashflow>ofNull()} terminals — a
 * model {@code choice} generates ONE class with one-of option attributes, so the guards lower to
 * option-nav null-tests, never the {@code instanceof} dispatch. A BARE {@code Fn} reference
 * compiles UNWRAPPED and is NOT this seat.
 *
 * <p><b>The dispatch order the fixtures exercise</b> ({@code CollectionHandler.compileLambda},
 * the {@code expr.body().body() instanceof RSwitchExpr} block): over a SINGLE receiver the MULTI
 * list block is attempted FIRST; on its decline the compile falls to
 * {@code compileChoiceSwitchBlockLambda}, which routes a model-choice subject to the SINGLE
 * option-getter form ({@code mapSingleToItem} + {@code MapperS.<X>ofNull()}). a1 and a2 are the
 * two sides of exactly that fork, and row 2's text read is the ONLY thing that separates them.
 *
 * <p><b>MUTATIONS (LAW 66/76) — MEASURED (LAW 82)</b> by {@code target/seat610-instruments/lanes-f3.py}
 * (local) against the four witness classes — this suite, {@code EnumSwitchBlockWidenSeatTest},
 * {@code ChoiceSwitchLambdaOptionGetterSeatTest}, {@code FunctionSwitchLambdaBlockTest}: 21 run / 0F /
 * 2 standing skips green at the swap head, 22 run / 0F / 3 skips at the reviews' fixes head (this
 * suite's {@code corpus_control2} skips without the IR provider, as its siblings' do). The swap is
 * an IDENTITY swap on the corpus, so it cannot fail first on its own; each lane makes the shared
 * helper LIE and the witnesses fail (measured at the swap head, replayed identically at the fixes
 * head — {@code lane-A-fixes.log} / {@code lane-B-fixes.log}):
 * <ul>
 *   <li><b>lane A</b> — {@code caseBodyCompilesMulti} answers {@code false} everywhere (no arm is
 *       ever MULTI): <b>3F</b> — this suite's {@code a1} (no list block: the render carries no
 *       {@code mapSingleToList} lambda) and {@code corpus_c1} (Create_CashflowFromPayout off
 *       golden), and {@code EnumSwitchBlockWidenSeatTest.corpus_c1} (the BERMUDA collapse admit
 *       never fires, the arm takes a {@code MapperS.of} wrap). The two ADMITTING reads (the list
 *       block, the collapse admit) show the lie; the two DECLINING reads (the instanceof and
 *       option-getter blocks) decline nothing on the corpus today either, so a false-everywhere
 *       lie is invisible there — disclosed; lane B covers them.</li>
 *   <li><b>lane B</b> — the helper answers {@code true} everywhere (every arm MULTI): <b>11F</b> —
 *       this suite's {@code a2} (SINGLE callees taken by the list block instead of the item block);
 *       {@code FunctionSwitchLambdaBlockTest} (the instanceof block declines every case and the
 *       cdm 6.20.6 generation falls to the {@code TYPE_SWITCH_TERNARY_STUB} refusal — 1F at
 *       {@code generate}); {@code ChoiceSwitchLambdaOptionGetterSeatTest} 3F ({@code a1},
 *       {@code e1}, {@code corpus_c1} — the option getter declines every case);
 *       {@code EnumSwitchBlockWidenSeatTest} 6F ({@code a1}, {@code a1b}, {@code a2}, {@code a3},
 *       {@code corpus_c1}, {@code corpus_control1} — the MULTI-arm decline fires on every arm).</li>
 * </ul>
 * Logs: {@code target/seat610-instruments/logs/lane-A.log} / {@code lane-B.log} (local); the lanes are
 * replayed at the seat's final head before the receipts chain (the CHANGELOG's Gates block names it).
 *
 * <p><b>Fixture naming.</b> {@code cash} / {@code cashes} / {@code bundle} avoid the lexer
 * keywords the sibling suites hit ({@code tag}, {@code single}, {@code label}); {@code OptA} /
 * {@code OptB} reuse {@code ChoiceSwitchLambdaOptionGetterSeatTest}'s proven option names so the
 * getter ({@code getOptA}) and the scope-allocated case local ({@code optA}) are pinned from a
 * measured render, not guessed.
 */
class SwitchCaseCardinalitySeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /** The carrier cell — cdm 6.20.6. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    /** The whole-file carrier: the ONLY unit-unwitnessed switch-block emitter's corpus row. */
    private static final String CARRIER_A =
            "cdm/product/template/functions/Create_CashflowFromPayout.java";

    // =========================================================================
    // Part A — the fixtures
    // =========================================================================

    /**
     * a1 = the {@code Create_CashflowFromPayout} shape reduced: a SINGLE model-{@code choice}
     * input, an {@code extract switch} whose non-default cases name the choice's OPTIONS, whose
     * bodies are EXPLICIT-ARGUMENT calls to MULTI-output functions (each compiling
     * {@code MapperC.<Cash>of(fnA.evaluate(…))}), and a {@code default empty}. a2 = the SAME
     * switch with SINGLE-output callees ({@code MapperS.of(gnA.evaluate(…))}) — row 2's admit
     * declines and the compile falls through to the SINGLE option-getter block. The two differ
     * ONLY in the callees' output cardinality, which is precisely the fact row 2 reads.
     */
    private static final String MODEL = """
            namespace census.seat610f3
            version "1.0.0"

            type Cash:
                amount string (0..1)

            type OptA:
                cash Cash (0..1)
                cashes Cash (0..*)

            type OptB:
                cash Cash (0..1)
                cashes Cash (0..*)

            choice Bundle:
                OptA
                OptB

            func FnA: <"a MULTI callee - its explicit-args call compiles MapperC-wrapped">
                inputs:
                    a OptA (0..1)
                output:
                    results Cash (0..*)
                add results:
                    a -> cashes

            func FnB: <"the OptB twin of FnA">
                inputs:
                    b OptB (0..1)
                output:
                    results Cash (0..*)
                add results:
                    b -> cashes

            func GnA: <"a SINGLE callee - the same call compiles MapperS-wrapped">
                inputs:
                    a OptA (0..1)
                output:
                    result Cash (0..1)
                set result:
                    a -> cash

            func GnB: <"the OptB twin of GnA">
                inputs:
                    b OptB (0..1)
                output:
                    result Cash (0..1)
                set result:
                    b -> cash

            func A1MultiChoiceSwitchListBlock: <"a1 - MULTI callees: the list block">
                inputs:
                    bundle Bundle (1..1)
                output:
                    results Cash (0..*)
                add results:
                    bundle
                        extract
                            switch
                                OptA then
                                    FnA(bundle -> OptA),
                                OptB then
                                    FnB(bundle -> OptB),
                                default empty

            func A2SingleChoiceSwitchItemBlock: <"a2 - SINGLE callees: the list block declines">
                inputs:
                    bundle Bundle (1..1)
                output:
                    result Cash (0..1)
                set result:
                    bundle
                        extract
                            switch
                                OptA then
                                    GnA(bundle -> OptA),
                                OptB then
                                    GnB(bundle -> OptB),
                                default empty
            """;

    /**
     * a1 — the MULTI model-choice switch renders the {@code mapSingleToList} LIST block: the
     * null-guard and both terminals are {@code MapperC.<Cash>ofNull()}, each arm probes its
     * option nav for presence, declares the {@code MapperS}-typed case local UNCONDITIONALLY
     * (upstream's per-case binding law — golden declares it even when the body never reads it)
     * and returns the {@code MapperC}-wrapped MULTI invocation. Nothing here may take the SINGLE
     * ({@code mapSingleToItem}) form and nothing may take the {@code instanceof} form.
     *
     * <p>The guard/decl asserts stop at the {@code map("getOptA",} prefix on purpose: the
     * generated nav-lambda parameter name is a scope-allocated fact, pinned from a green run's
     * log rather than guessed here. What separates this from a2 is the {@code MapperC} half, and
     * that is asserted directly, both positively and negatively.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_multiChoiceSwitchRendersTheListBlock() throws IOException {
        String out = fn("A1MultiChoiceSwitchListBlock.java");
        String code = codeOnly(out);
        assertContains(out, ".mapSingleToList(item -> {");
        assertContains(out, "if (item.get() == null) {");
        assertContains(out, "return MapperC.<Cash>ofNull();");
        assertContains(out, "if (item.<OptA>map(\"getOptA\", ");
        assertContains(out, "if (item.<OptB>map(\"getOptB\", ");
        assertContains(out, ".get() != null) {");
        assertContains(out, "final MapperS<OptA> optA = item.<OptA>map(");
        assertContains(out, "final MapperS<OptB> optB = item.<OptB>map(");
        assertContains(out, "return MapperC.<Cash>of(fnA.evaluate(");
        assertContains(out, "return MapperC.<Cash>of(fnB.evaluate(");
        assertTrue(!code.contains("mapSingleToItem("),
                "a MULTI case-body switch must take the LIST block, never the SINGLE one:\n" + out);
        assertTrue(!code.contains("instanceof"),
                "a model-choice subject lowers to option-nav presence tests, never instanceof:\n"
                + out);
    }

    /**
     * a2 — the decline pin (LAW 76 witness-uniqueness). SINGLE-output callees compile
     * {@code MapperS.of(…)}, row 2's admit declines, and the compile falls through to the SINGLE
     * option-getter block: {@code mapSingleToItem} with {@code MapperS.<Cash>ofNull()} terminals
     * (the form {@code ChoiceSwitchLambdaOptionGetterSeatTest} pins — a model-choice subject
     * takes the option-getter branch, NOT the {@code instanceof} ladder). The witness is a token
     * the seat's admit would ADD — {@code MapperC.<Cash>ofNull()} / {@code .mapSingleToList(} —
     * asserted ABSENT, alongside the positive token of the form that must stand, so a fixture
     * that failed to generate cannot pass this test.
     *
     * <p>The case-local NAMES are deliberately not asserted here: this fixture compiles the case
     * bodies TWICE (the declining list-block attempt, then the option-getter emission) and the
     * scope-allocated local name is the one fact that double compile could move. The cardinality
     * fork is what this test pins.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_singleOutputCalleesDoNotTakeTheListBlock() throws IOException {
        String out = fn("A2SingleChoiceSwitchItemBlock.java");
        String code = codeOnly(out);
        assertContains(out, ".mapSingleToItem(item -> {");
        assertContains(out, "if (item.get() == null) {");
        assertContains(out, "return MapperS.<Cash>ofNull();");
        assertContains(out, "if (item.<OptA>map(\"getOptA\", ");
        assertContains(out, ".get() != null) {");
        assertTrue(!code.contains("MapperC.<Cash>ofNull()"),
                "a SINGLE case-body switch must not reach the LIST block's terminal:\n" + out);
        assertTrue(!code.contains(".mapSingleToList("),
                "a SINGLE case-body switch must not reach the LIST block at all:\n" + out);
        assertTrue(!code.contains("instanceof"),
                "a model-choice subject takes the option-getter form, never instanceof:\n" + out);
    }

    // =========================================================================
    // Part B — the corpus carrier (cdm 6.20.6; the D11 ring locks the cell,
    // this suite locks the one whole file the seat can move)
    // =========================================================================

    /**
     * corpus_c1 — the whole-file byte lock on the emitter's ONLY unit-unwitnessed carrier. The
     * file is GREEN today (PR #365 healed it); the seat's swap must keep it byte-identical, and
     * either mutation lane must break it.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_cdm6206CreateCashflowFromPayoutByteIdentical() throws IOException {
        assertNotNull(cdmAOutput, "cdm 6.20.6 generation did not run — corpus unavailable?");
        List<String> own = cdmAGenErrors.stream().filter(e -> e.contains(CARRIER_A)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for the locked file " + CARRIER_A + ": " + own);
        String generated = cdmAOutput.get(CARRIER_A);
        assertNotNull(generated, "not generated in cdm 6.20.6: " + CARRIER_A);
        Path goldenPath = GOLDEN_A.resolve(CARRIER_A);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated cdm 6.20.6 output must byte-match golden (newline-normalized) for "
                + CARRIER_A + " — PR #610 family 3: the MULTI model-choice switch list block's"
                + " case-body cardinality read moves onto the typed channel with no byte change.");
    }

    /**
     * control0 — golden is the oracle: it carries the {@code mapSingleToList} block, the
     * {@code MapperC.<Cashflow>ofNull()} terminal and the {@code MapperC}-wrapped MULTI arm
     * invocation. If this ever stops holding, the carrier stopped being this family's carrier
     * and every claim above is restated, not patched.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesTheListBlock() throws IOException {
        String golden = Files.readString(GOLDEN_A.resolve(CARRIER_A));
        assertTrue(golden.contains(".mapSingleToList(item -> {"),
                "golden must carry the LIST block lambda");
        assertTrue(golden.contains("return MapperC.<Cashflow>ofNull();"),
                "golden's null-guard and fall-through terminals must be the MapperC ofNull");
        assertTrue(golden.contains(
                "return MapperC.<Cashflow>of(create_CashflowFromSettlementPayout.evaluate("),
                "golden's arms must return the MapperC-wrapped MULTI invocation — the shape the"
                + " seat's cardinality read admits");
        assertTrue(golden.contains(
                "if (item.<OptionPayout>map(\"getOptionPayout\", "),
                "golden's guards must be option-nav presence tests");
        assertTrue(!golden.contains("instanceof"),
                "golden must NOT carry the instanceof dispatch for a model-choice subject");
    }

    /**
     * control2 — LAW 77 route parity for the cdm 6.20.6 carrier: the seat is CollectionHandler-side and
     * the IR route inherits it (the census read an identical probe multiset on both routes), so the
     * carrier must render identically under {@code -Pir-on}. Skips (recorded) without the IR provider.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarrier() throws IOException {
        assertNotNull(cdmAOutput, "cdm 6.20.6 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_A_ROOT), new ArrayList<>());
        assertEquals(cdmAOutput.get(CARRIER_A), irOut.get(CARRIER_A),
                "route divergence: " + CARRIER_A);
    }

    // =========================================================================
    // Fixture harness (the ChoiceSwitchLambdaOptionGetterSeatTest renderer)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat610f3.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat610f3".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    /** Functions land under {@code .../functions/}. */
    private static String fn(String fileName) throws IOException {
        Render r = render();
        List<String> own = r.errors().stream().filter(e -> e.contains(fileName)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for " + fileName + " (a broken fixture must fail"
                + " loudly, not skip): " + own);
        String out = lookupOrNull(r.output(), "functions/" + fileName);
        assertNotNull(out, "not generated: " + fileName + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static String lookupOrNull(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    /**
     * Strip line and block comments plus string literals so a javadoc or a label never counts as
     * code. Every NEGATIVE assertion runs on this; the positive ones run on the raw render
     * (their needles carry the {@code "getOptA"} literals this strips).
     */
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
            throw new AssertionError("[SwitchCaseCardinalitySeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the sibling seat suites' cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> cdmAOutput;
    private static List<String> cdmAGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            cdmAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_A_ROOT), errs);
            cdmAGenErrors = errs;
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }
}
