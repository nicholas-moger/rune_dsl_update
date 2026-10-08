package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

/**
 * SEAT 25, law B — facet {@code ruleCallArmMetaWrap}: <b>a SINGLE+meta rule-call ARM in an extract
 * lambda wraps null-safely in EVERY arm position</b> — golden drr 7.x csa
 * {@code UnderlyingAssetTradingPlatformIdentifierLeg1/2Rule} (then-edge-NESTED then + ELSE position)
 * and drr 5.61.0 jfsa {@code NotionalCurrencyOfLeg2Rule} (WITH-ARGS calls) hoist the bare value
 * in-branch and return the builder wrap ternary:
 * {@code final String string0 = <rule>.evaluate(…); return string0 == null ?
 * MapperS.<FieldWithMetaString>ofNull() : MapperS.of(FieldWithMetaString.builder()
 * .setValue(string0).build());} — with the ladder's decl and typed-empty tail at the wrapper.
 *
 * <p><b>The defect (three seats, measured by the seat-25 LAW-75 probes).</b> (1) the #331/#372
 * conditional-arm walk required {@code thenBranch() == expr} and climbed ELSE edges only, so the
 * then-edge-nested Leg1 and the else-position Leg2 both declined; (2) the NotionalCurrencyOfLeg2
 * arms are WITH-ARGS calls that never reach the no-args seat (probe 1 measured ZERO P25A lines for
 * them — they compile at the {@code :1503} innerCall block, which had no single meta-wrap consult);
 * (3) the #330 arms-agree JOIN read a with-args rule-call arm as BARE ({@code recoverMetaFromExpr}
 * case (c) required {@code args().isEmpty()}) and its walk-scoped {@code visited} set read the
 * SECOND sibling arm calling the SAME rule as null (NotionalCurrencyOfLeg2's arms 0 and 4 both call
 * {@code CDEInterestRateNotionalCurrency} — the P25F probe read the join collapsing there).
 *
 * <p><b>The law (ONE emitter, LAW 69).</b> {@code ReferenceHandler.trySingleRuleMetaWrap} — the
 * #265/#331/#372 wrap extracted verbatim — is consulted by the no-args seat AND the with-args seat
 * (arm positions only there; the lambda-terminal with-args form is corpus-unwitnessed — a stated
 * decline). The conditional-arm walk follows BOTH then- and else-edges to the outermost conditional
 * and admits the reference at EITHER branch of its immediate parent; the #330 join still gates (a
 * bare-joined ladder declines — the hkma UATPI green class). {@code recoverMetaFromExpr} case (c)
 * admits the with-args call (args select the callee's INPUT, never its output meta) and the
 * {@code visited} guard is STACK-scoped (a repeated sibling callee recovers; a true delegation cycle
 * still declines). A MULTI-ARM ladder's hoists render the deferred SENTINEL
 * ({@code MetaWrapValueHoist}'s token constructor) — the first arm's drain would otherwise close the
 * lambda scope before the second arm's {@code createUniqueIdentifier} (the #346 cp1 pattern; the
 * pre-fix run emitted a TODO-comment body for both UATPI files).
 *
 * <p><b>RED at the pre-seat blob — MEASURED</b> (the law commit's parent, this suite kept): a1, a2,
 * the three corpus locks c1–c3, corpus_control1 and corpus_control3 (the carriers' token counts);
 * b1, b2 and corpus_control0 GREEN in both states. Under {@code -Pir-on} also a3.
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by the
 * seat's receipts chain — LAW 82): (i) the walk reverted to {@code thenBranch() == expr} +
 * else-edges-only — 4F: a1, c1, c2, corpus_control3; (ii) the with-args consult deleted — 3F: a2,
 * c3, corpus_control1; (iii) case (c)'s with-args admission reverted ({@code args().isEmpty()}
 * restored) — 3F: the SAME set as (ii); (iv) the stack-scoped {@code visited} reverted to
 * walk-scoped — 3F: the SAME set as (ii). (ii)/(iii)/(iv) are three DISTINCT mechanism links on
 * the one with-args carrier chain (the consult, the join's admission, the join's sibling-repeat),
 * so their witness sets coincide — each link alone kills the wrap, stated rather than
 * over-claimed. (v) the sentinel token dropped (the hoist renders the identifier and the first
 * arm's drain closes the lambda scope) — 5F: a1, c1, c2, corpus_control3 + the cross-suite
 * {@code WholeOutputMetaDerefSeatTest.corpus_control1} (the scope error wipes the whole ladder
 * body, so the output deref census moves too).
 */
class RuleCallArmMetaWrapSeatTest {

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

    static boolean builtinsAndIrProviderAvailable() {
        return builtinsAvailable() && irProviderOnClasspath();
    }

    /**
     * A {@code [metadata scheme]} single leaf behind two single+meta inner rules, a Sub-typed
     * with-args callee, and one outer rule per carrier shape.
     */
    private static final String MODEL = """
            namespace census.seat25f6b
            version "1.0.0"

            type Ident:
                code string (0..1)
                    [metadata scheme]

            type Leg:
                ident Ident (0..1)
                name string (0..1)

            type Sub:
                leg Leg (0..1)

            type Instr:
                legA Leg (0..1)
                sub Sub (0..1)
                allowed boolean (0..1)
                isOpt boolean (0..1)
                isAligned boolean (0..1)

            func IsAllowable: <"IsAllowableActionForCSA twin - the function filter">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func IsOpt: <"the outer conditional guard">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isOpt = True

            func IsAligned: <"IsCSALeg1Aligned twin - the inner conditional guard">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isAligned = True

            func SubFor: <"ProductForTrade twin - the with-args call's argument chain">
                inputs:
                    i Instr (0..1)
                output:
                    s Sub (1..1)
                set s:
                    i -> sub

            reporting rule InnerA from Instr: <"UATPI Leg1 twin - a SINGLE rule whose rosetta output is meta-typed">
                extract legA -> ident -> code
                    as "innerA"

            reporting rule InnerB from Instr: <"UATPI Leg2 twin">
                extract legA -> ident -> code
                    as "innerB"

            reporting rule InnerSubCcy from Sub: <"CDEInterestRateNotionalCurrency twin - the with-args callee">
                extract leg -> ident -> code
                    as "innerSub"

            reporting rule A1NestedArms from Instr: <"a1 - THE csa UATPI SHAPE: a then-edge-NESTED then arm and an ELSE arm, both bare single+meta rule refs, elseless outer">
                filter IsAllowable
                then extract
                    if IsOpt(item)
                    then if IsAligned(item)
                        then InnerA
                        else InnerB
                    as "a1"

            reporting rule A2WithArgsArms from Instr: <"a2 - THE jfsa NotionalCurrencyOfLeg2 SHAPE: WITH-ARGS single+meta rule calls at then positions of an else-if ladder, both arms the SAME callee">
                filter IsAllowable
                then extract
                    if IsOpt(item)
                    then InnerSubCcy(SubFor(item))
                    else if IsAligned(item)
                    then InnerSubCcy(SubFor(item))
                    as "a2"

            reporting rule B1BareJoin from Instr: <"b1 - THE hkma UATPI GREEN: a ladder with a BARE (plain string) terminal arm joins bare - no wrap">
                filter IsAllowable
                then extract
                    if IsOpt(item)
                    then InnerA
                    else legA -> name
                    as "b1"

            reporting rule B2NonArmWithArgs from Instr: <"b2 - a with-args single+meta call at the lambda-TERMINAL seat (not an arm) stays bare - the stated decline">
                filter IsAllowable
                then extract InnerSubCcy(SubFor(item))
                    as "b2"
            """;

    private static final String WRAP_A = "== null ? MapperS.<FieldWithMetaString>ofNull() : MapperS.of(FieldWithMetaString.builder().setValue(";

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the nested-then arm AND the else arm both hoist in-branch and wrap; the tail + output deref follow. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_nestedThenAndElseArmsWrap() throws IOException {
        String out = rule("A1NestedArmsRule.java");
        assertContains(out, "final String string0 = innerARule.evaluate(item.get());");
        assertContains(out, "return string0 " + WRAP_A + "string0).build());");
        assertContains(out, "final String string1 = innerBRule.evaluate(item.get());");
        assertContains(out, "return string1 " + WRAP_A + "string1).build());");
        assertContains(out, "return MapperS.<FieldWithMetaString>ofNull();");
        assertContains(out, "output = fieldWithMetaString.getValue();");
        assertNotContains(out, "return MapperS.of(innerARule.evaluate(item.get()));");
        assertNotContains(out, "return MapperS.of(innerBRule.evaluate(item.get()));");
    }

    /** a2 — the WITH-ARGS arms wrap through the same emitter; the repeated callee joins (the stack-scoped visited). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_withArgsArmsWrap() throws IOException {
        String out = rule("A2WithArgsArmsRule.java");
        assertContains(out, "final String string0 = innerSubCcyRule.evaluate(subFor.evaluate(item.get()));");
        assertContains(out, "return string0 " + WRAP_A + "string0).build());");
        assertContains(out, "final String string1 = innerSubCcyRule.evaluate(subFor.evaluate(item.get()));");
        assertContains(out, "return string1 " + WRAP_A + "string1).build());");
        assertContains(out, "return MapperS.<FieldWithMetaString>ofNull();");
        assertNotContains(out, "return MapperS.of(innerSubCcyRule.evaluate(");
    }

    /** a3 — LAW 77: the a1 shape through the REAL {@code IRGeneration.functionGenerator} seam ({@code -Pir-on}). */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a3_nestedArmsWrapOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "reports/A1NestedArmsRule.java");
        assertContains(out, "final String string0 = innerARule.evaluate(item.get());");
        assertContains(out, "return string0 " + WRAP_A + "string0).build());");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — a bare-joined ladder declines: the #330 arms-agree join reads the plain-string arm and no wrap fires. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_bareJoinedLadderStaysBare() throws IOException {
        String out = rule("B1BareJoinRule.java");
        assertContains(out, "return MapperS.of(innerARule.evaluate(item.get()));");
        assertNotContains(out, "FieldWithMetaString.builder()");
    }

    /** b2 — a with-args single+meta call at the lambda-TERMINAL seat (not an arm) stays bare — the stated decline. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_nonArmWithArgsStaysBare() throws IOException {
        String out = rule("B2NonArmWithArgsRule.java");
        assertContains(out, "MapperS.of(innerSubCcyRule.evaluate(subFor.evaluate(item.get())))");
        assertNotContains(out, "FieldWithMetaString.builder()");
    }

    // =========================================================================
    // Part C — the corpus (drr 7.0.0: the csa UATPI carriers; drr 5.61.0: NotionalCurrencyOfLeg2; whole-cell controls)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), RuleCallArmMetaWrapSeatTest.class);
    }

    static boolean drr7AndIrProviderAvailable() {
        return drr7Available() && irProviderOnClasspath();
    }

    private static final String CSA_UATPI_LEG1 =
            "drr/regulation/csa/rewrite/trade/reports/UnderlyingAssetTradingPlatformIdentifierLeg1Rule.java";
    private static final String CSA_UATPI_LEG2 =
            "drr/regulation/csa/rewrite/trade/reports/UnderlyingAssetTradingPlatformIdentifierLeg2Rule.java";
    private static final String JFSA_NCOLEG2 =
            "drr/regulation/jfsa/rewrite/trade/reports/NotionalCurrencyOfLeg2Rule.java";

    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_csaUatpiLeg1ByteIdentical() throws IOException {
        lock7(CSA_UATPI_LEG1);
    }

    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_csaUatpiLeg2ByteIdentical() throws IOException {
        lock7(CSA_UATPI_LEG2);
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c3_jfsaNotionalCurrencyOfLeg2ByteIdentical() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr561GenErrors.stream().filter(e -> e.contains(JFSA_NCOLEG2)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + JFSA_NCOLEG2 + ": " + lockedErrors);
        String generated = drr561Output.get(JFSA_NCOLEG2);
        assertNotNull(generated, "not generated in drr 5.61.0: " + JFSA_NCOLEG2);
        assertEquals(normalize(Files.readString(DRR561_GOLDEN.resolve(JFSA_NCOLEG2))), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for " + JFSA_NCOLEG2
                + " — seat 25 law B: the with-args single+meta arm wraps.");
    }

    /**
     * control0 — golden is the oracle (the frozen trees, every kind): the two token populations at
     * their exact census counts — T1 the null-safe builder-wrap ternary line, T2 the hoisted
     * {@code final String stringN = <x>.evaluate(…)} decl — and each carrier's own counts.
     * (The counts are MEASURED against the frozen goldens — the golden-scan25 census read T1 73 /
     * T2 42 on drr 5.61.0 and T1 92 / T2 94 on drr 7.0.0 with sibling predicates.)
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenIsTheOracle() throws IOException {
        Map<String, int[]> g7 = scan(readGoldenTree(DRR7_GOLDEN));
        assertEquals(List.of(2, 2), counts(g7.get(CSA_UATPI_LEG1)), "golden csa UATPI Leg1 (T1, T2)");
        assertEquals(List.of(2, 2), counts(g7.get(CSA_UATPI_LEG2)), "golden csa UATPI Leg2 (T1, T2)");
        Map<String, int[]> g5 = scan(readGoldenTree(DRR561_GOLDEN));
        assertEquals(List.of(3, 3), counts(g5.get(JFSA_NCOLEG2)), "golden jfsa NCOLeg2 (T1, T2)");
    }

    /**
     * control1 — the FORK's WHOLE generated drr 5.61.0 cell (every kind, LAW 72): over the UNION of
     * the files either tree carries a token in (a missing side counts as all-zero, LAW 79), the
     * per-file (T1, T2) pairs agree FILE BY FILE except the NAMED residue (other families' band
     * rows — LAW 73: the set, not the count).
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control1_forkDrr561WholeCellArmWrapTokensEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drr561GenErrors, "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drr561Output), scan(readGoldenTree(DRR561_GOLDEN)), drr561Output.keySet(),
                "fork", "golden", KNOWN_RESIDUE_561, -1);
    }

    /**
     * The named residue in drr 5.61.0, MEASURED at this seat's head (LAW 73: the set, not the count):
     * cftc NotionalCurrencyLeg2Rule is ANOTHER family's band row (the rule-path ternary ladder) whose
     * arm-position rungs this law partially moves (2 of golden's 4 ternary LINES render — the fork's
     * flattened ternary chain carries all four wraps on fewer lines; the file stays in the band for
     * its ladder form). The two DeliveryRule entries this list pinned at law B's head (the fork's
     * Law-D {@code final String string = daysOfTheWeekRule.evaluate(item.get());}
     * singletonList-coercion hoist) HEALED at law D (facet ctorRuleValueCardinality — the
     * {@code CtorRuleValueCardinalitySeatTest} locks both files byte-identical) and left the list in
     * that law's commit — the within-seat LAW-81 hand-off, honored.
     */
    private static final List<String> KNOWN_RESIDUE_561 = List.of();
            // the NotionalCurrencyLeg2Rule row (fork=[2, 4] golden=[4, 4]) LEFT this list at seat 32: law C.3
            // (ruleThenArmLadderNestedTreeAdmit) healed it WHOLE; transcribed from the print (C3-trip1b.log).

    /**
     * control3 — the drr 7.0.0 cell: the UATPI carriers heal and every other token-bearing file
     * agrees file by file except the NAMED residue (other families' band rows).
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_forkDrr7WholeCellArmWrapTokensEqualGoldenFileByFile() throws IOException {
        List<String> errs = new ArrayList<>();
        Map<String, String> out = generateCell(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
        assertEquals(List.of(), errs, "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(out), scan(readGoldenTree(DRR7_GOLDEN)), out.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, -1);
    }

    /** The named pre-existing residue of other families in drr 7.0.0 (measured at this seat's head). */
    private static final List<String> KNOWN_RESIDUE_7 = List.of();

    /**
     * control2 — LAW 77 route parity: the whole drr 7.0.0 cell generated through the REAL
     * {@code IRGeneration} seams ({@code -Pir-on}) carries the SAME per-file pairs as the
     * legacy-route render, file by file over the UNION.
     */
    @Test
    @EnabledIf("drr7AndIrProviderAvailable")
    void corpus_control2_irRouteDrr7ArmWrapTokensEqualLegacyRouteFileByFile() throws IOException {
        List<String> errs = new ArrayList<>();
        Map<String, String> legacy = generateCell(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), new ArrayList<>());
        assertUnionEqual(scan(irOut), scan(legacy), irOut.keySet(), "ir", "legacy", List.of(),
                scan(legacy).size());
    }

    private static List<Integer> counts(int[] c) {
        assertNotNull(c, "the file carries no token");
        return List.of(c[0], c[1]);
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        int[] zero = new int[2];
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
                "(T1, T2) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "token-bearing files " + bName + " carries that " + aName + " does not emit at all");
        if (expectedDomain >= 0) {
            assertEquals(expectedDomain, universe.size(),
                    "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
        }
    }

    // =========================================================================
    // The scan — (T1, T2) per file over CODE only (comments stripped; a line walk)
    // =========================================================================

    static int[] countTokens(String java) {
        int[] c = new int[2];
        for (String raw : codeOnly(java).split("\n")) {
            String s = raw.strip();
            if (s.contains("== null ? MapperS.<") && s.contains(".builder().setValue(")) {
                c[0]++;
            }
            if (s.startsWith("final String string") && s.contains(" = ") && s.contains(".evaluate(")
                    && s.endsWith(";")) {
                c[1]++;
            }
        }
        return c;
    }

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains("== null ? MapperS.<") && !e.getValue().contains("final String string")) {
                continue;
            }
            int[] c = countTokens(e.getValue());
            if (c[0] + c[1] > 0) {
                out.put(e.getKey(), c);
            }
        }
        return out;
    }

    /** Strip line and block comments so a javadoc never counts as code. */
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
                sb.append(java, i, Math.min(j + 1, n));
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

    private static Map<String, String> readGoldenTree(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            for (Path p : stream.filter(q -> q.toString().endsWith(".java")).sorted().toList()) {
                out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    // =========================================================================
    // Cell generation (drr 5.61.0 / 7.0.0, the legacy route; the IR route for control2)
    // =========================================================================

    private static Map<String, String> drr561Output;
    private static List<String> drr561GenErrors;
    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr561Available()) {
            List<String> errs = new ArrayList<>();
            drr561Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), errs);
            drr561GenErrors = errs;
        }
        if (drr7Available()) {
            List<String> errs = new ArrayList<>();
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
            drr7GenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var enumGen = new EnumGenerator(gm);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, pojoGen.generateClasses(model, version, output));
                collect(errors, choiceGen.generateClasses(model, version, output));
                collect(errors, enumGen.generateClasses(model, version, output));
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    /**
     * The cell through the REAL {@code IRGeneration} seams — the D11 ON ring's wiring (the flag set
     * for the render and restored after; the provider asserted present; the function seam asserted
     * to hand back the IR-route generator, so this can never silently be an OFF-route render).
     */
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
            var pojoGen = IRGeneration.modelObjectGenerator(gm, typeTranslator, typeUtil);
            var choiceGen = IRGeneration.choiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
            var enumGen = IRGeneration.enumGenerator(gm);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
            var labelProviderGen = new LabelProviderGenerator(
                    gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                    new LabelProviderGeneratorUtil());
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, IRGeneration.generateClasses(pojoGen, model, version, output));
                    collect(errors, IRGeneration.generateClasses(choiceGen, model, version, output));
                    collect(errors, IRGeneration.generateClasses(enumGen, model, version, output));
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
                    collect(errors, dataRuleGen.generateClasses(model, version, output));
                    collect(errors, labelProviderGen.generateClasses(model, version, output));
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

    private static void lock7(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr7GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = DRR7_GOLDEN.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 25 law B: the single+meta arm wraps in every arm position.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat25f6b.rosetta");
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
            fixtureOut = render(m -> "census.seat25f6b".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static Map<String, String> fixtureOutIr;

    /** The fixture's rules through the REAL {@code IRGeneration.functionGenerator} seam (the ON route). */
    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureOutIr == null) {
            link();
            String previous = System.getProperty(IRGeneration.PROPERTY);
            System.setProperty(IRGeneration.PROPERTY, "true");
            try {
                assertNotNull(IRGeneration.providerOrNull(),
                        "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
                GeneratorModel gm = new GeneratorModel(linking.workspace(),
                        m -> "census.seat25f6b".equals(m.namespace()));
                JavaTypeUtil typeUtil = new JavaTypeUtil();
                JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
                FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
                assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                        "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
                RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
                Map<String, String> out = new LinkedHashMap<>();
                List<String> errors = new ArrayList<>();
                ruleGen.generateClasses(mainModel, "1.0", out)
                        .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
                fg.generateWithErrors(out)
                        .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
                if (!errors.isEmpty()) {
                    throw new AssertionError("fixture generation errors on the IR route: " + errors);
                }
                fixtureOutIr = out;
            } finally {
                if (previous == null) {
                    System.clearProperty(IRGeneration.PROPERTY);
                } else {
                    System.setProperty(IRGeneration.PROPERTY, previous);
                }
            }
        }
        return fixtureOutIr;
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
                        failures.add(p + " — " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[RuleCallArmMetaWrapSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle),
                "expected needle missing:\n" + needle + "\n--- in output:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertFalse(out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
