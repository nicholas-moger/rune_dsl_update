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
 * SEAT 29, law 6b — facet {@code filterPredicateMetaDeref}: <b>the seat-28 AMENDMENT's
 * caller-input decline reads the enclosing function's inputs THROUGH the dispatch base</b>.
 * A DISPATCH VARIANT ({@code func Foo(param: Enum -&gt; VALUE):}) declares no {@code inputs:}
 * section of its own — {@code AstBuilder.visitFunction} fills {@code RFunction.inputs()} only
 * from the header's {@code attribute+}, which a variant does not carry — so
 * {@code argOwner.inputs()} is EMPTY inside a variant while its body resolves every input NAME
 * against the BASE. The amendment's decline could therefore never engage inside a dispatch
 * impl, and the LAMBDA_CHANNEL hoist fired on an arg golden derefs inline.
 *
 * <p><b>THIS IS A ZERO-CARRIER CORRECTNESS FIX</b> (the #600 adversarial-review bank, finding
 * "law 6b dispatch-base-aware inputs"). It was proved by code-reading, not by a moved row: no
 * corpus dispatch variant reaches the LAMBDA_CHANNEL argument seat today. The expected corpus
 * measurement is BYTE-NEUTRALITY — the matrix digest UNMOVED, both rings EXACT — and the
 * witness is the fixture below (a1), which is RED at the pre-law head and GREEN after.
 *
 * <p><b>The wrong vs right shape</b> (fixture {@code A1DispatchCallerInputArgFilter}, the
 * dispatch twin of the seat-28 law-6 {@code b5} decline pin):
 * <pre>
 * wrong (today):  .filterItemNullSafe(item -&gt; {
 *                     final ReferenceWithMetaSched referenceWithMetaSched = ref.get();
 *                     return takesPlain.evaluate((referenceWithMetaSched == null ? null
 *                             : referenceWithMetaSched.getValue()));
 *                 })
 * right (after):  .filterItemNullSafe(item -&gt; takesPlain.evaluate(ref.get()))
 * </pre>
 * i.e. the same decline the NON-dispatch {@code b1} twin already gets, and the same one golden
 * gives cdm {@code UpdatePriceAmountForEachMatchingQuantity} (the class the seat-28 mid-seat
 * whole-matrix D11 caught ENTERING ten cells under the un-amended law).
 *
 * <p><b>The binding chain the law depends on</b> (the identity proof, cited):
 * {@code LexicalResolutionPass.buildFunctionScope} registers
 * {@code fn.dispatchBase().get().inputs()} for a variant and {@code fn.inputs()} otherwise —
 * the very {@code RAttribute} INSTANCES — and {@code resolveSymbolsInScope} binds THAT instance
 * into {@code RSymbolReference.symbol()}. Reading the caller inputs through
 * {@code RFunction.dispatchBase()} — the SAME accessor the binder used, LAW 69 — is what makes
 * the identity test total. The sibling {@code HandlerHelper.dispatchBaseOf} is deliberately NOT
 * used: it selects the base by a different predicate ({@code dispatch().isEmpty()} vs
 * {@code operations().isEmpty()}) and is the alias-signature seat's helper, not the binder's.
 * The two agree on all four corpus dispatch groups (YearFraction, DayCountBasis,
 * ComputeCalculationPeriod, ProcessFloatingRateReset — every base is signature-only).
 *
 * <p><b>The RETIRED name-equality disjunct.</b> The amendment tested
 * {@code in == argRootAttr || in.name().equals(argRootAttr.name())}. The second half is DEAD:
 * the scope registers the instances it later binds, so a reference bound to a caller input IS
 * that instance; the only other {@code RAttribute} bindings a body reference can carry are the
 * function OUTPUT (same source) and, on the type-condition walk, a data-type attribute (no
 * enclosing {@code RFunction} there); {@code RFileScope.lookup} returns {@code RRootElement},
 * so a file-scope fallback cannot supply one; and a lambda param binds an
 * {@code RInlineFunction}, which the {@code instanceof RAttribute} gate already excludes. Its
 * only two REACHABLE fires are FALSE ADMITS — an output sharing an input's name (registered
 * last, so it wins the scope), and a dispatch variant declaring its OWN same-named inputs
 * (grammar-legal, ignored by the scope's replace semantics). Both are corpus-absent; the second
 * is BANKED as {@code x1} below.
 *
 * <p><b>LAW 74</b>: the wrong form COMPILES (it is a legal null-guarded deref of a wrapper into
 * a bare-item parameter) — this law is a FIDELITY fix, not a compile fix, so the javac PRE/POST
 * probe is expected to read 0 → 0 and is NOT the oracle here. The byte oracle is golden.
 *
 * <p><b>RED at the pre-seat blob — MEASURED at the seat-29 chain's RED leg ({@code fa277393},
 * identical on BOTH routes)</b>: a1 AND a1n (the drafted claim named a1 only; a1n — the
 * no-wrapper-declaration companion assert — fires with it, the same defect seen from the decl
 * side). b1, b2, and every corpus control are GREEN in both states — the byte-neutrality claim,
 * MEASURED holding (the chain's matrix moved only by the seven laws' 24 heals; this law's lanes
 * moved no corpus bytes).
 *
 * <p><b>MEASURED MUTATIONS (LAW 82 — the seat-29 chain's suite-lane loop at {@code 687c8feb};
 * the set below is the RECORDED failing set from the f29-mut-m-law6b log):</b>
 * <ul>
 *   <li><b>m-law6b</b> the whole law reverted ({@code law6b-apply --revert} — the dispatch read
 *       AND the disjunct retirement together) → MEASURED a1, a1n (2F, fixture-only — the
 *       zero-carrier law's witnesses; the seat-27 c-ii precedent); every corpus control held,
 *       the byte-neutrality claim confirmed at mutation scale.</li>
 * </ul>
 * <p><b>Designed manual sever — stated, NOT run in the seat-29 chain</b> (no apply-script flag;
 * its set remains a claim): {@code m-law6b-half}, the disjunct retirement alone (hand-revert the
 * {@code anyMatch} lambda to the two-term form, keeping the dispatch read). Its failing set is
 * EXPECTED EMPTY — a dead-code retirement has no fixture witness by construction; were it run, a
 * measured zero would be ADJUDICATED by the identity proof above plus the two named false-admit
 * shapes, both corpus-absent.
 */
class DispatchBaseInputDeclineSeatTest {

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

    /** cell A = drr 5.61.0 — law 6's own healed population (the Spread trio + the mas trio). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * cell B = cdm 6.20.6 — the ENTERED-class tripwire. The seat-28 mid-seat whole-matrix D11
     * caught {@code UpdatePriceAmountForEachMatchingQuantity} entering ten cdm cells under the
     * un-amended law; this cell is where an over-fire of the WIDENED read would show first.
     */
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
     * a1 = the DISPATCH twin of the seat-28 law-6 {@code b5} pin: the predicate arg is the
     * CALLER'S {@code [metadata reference]} input, but the body sits in a dispatch VARIANT whose
     * own {@code inputs()} is empty. b1 = the same body on a PLAIN function — the decline
     * already engages there, bytes frozen. b2 = the OVER-DECLINE pin: a dispatch variant whose
     * predicate arg is the ITEM, which must STILL hoist.
     *
     * <p>{@code note} is used instead of {@code tag} — {@code tag} is a rune lexer keyword; no
     * {@code single} and no {@code label} appear either.
     */
    private static final String MODEL = """
            namespace census.seat29a
            version "1.0.0"

            enum ModeEnum:
                Alpha
                Beta

            type Sched:
                ccy string (0..1)
                amt number (0..1)
                note string (0..1)

            type PriceHolder:
                items Sched (0..*)
                mark number (0..1)

            type MetaHolder:
                prices Sched (0..*)
                    [metadata reference]
                mark number (0..1)

            func TakesPlain: <"the predicate callee takes the BARE value type (meta-free param)">
                inputs:
                    s Sched (0..1)
                output:
                    result boolean (1..1)
                set result:
                    s exists

            func IsMonetary: <"the bare-fn predicate callee - the law-6 a1 twin's callee">
                inputs:
                    p Sched (1..1)
                output:
                    result boolean (1..1)
                set result:
                    p -> ccy exists

            func A1DispatchCallerInputArgFilter: <"a1 BASE - the caller inputs live HERE; the variant body reads them by name">
                inputs:
                    ph PriceHolder (1..1)
                    ref Sched (0..1)
                        [metadata reference]
                    mode ModeEnum (1..1)
                output:
                    result Sched (0..*)

            func A1DispatchCallerInputArgFilter(mode: ModeEnum -> Alpha): <"a1 VARIANT - the b5 body verbatim; argOwner.inputs() is EMPTY here, so the decline missed">
                add result:
                    ph -> items
                        then filter TakesPlain(ref)

            func B1PlainCallerInputArgFilter: <"b1 - the seat-28 b5 shape on a NON-dispatch function: the decline already engages, bytes frozen">
                inputs:
                    ph PriceHolder (1..1)
                    ref Sched (0..1)
                        [metadata reference]
                output:
                    result Sched (0..*)
                add result:
                    ph -> items
                        then filter TakesPlain(ref)

            func B2DispatchItemRootedArgFilter: <"b2 BASE - the OVER-DECLINE pin's signature">
                inputs:
                    mh MetaHolder (1..1)
                    mode ModeEnum (1..1)
                output:
                    result Sched (0..*)

            func B2DispatchItemRootedArgFilter(mode: ModeEnum -> Beta): <"b2 VARIANT - an ITEM-rooted predicate arg inside a dispatch impl must STILL hoist">
                add result:
                    mh -> prices
                        then filter IsMonetary
            """;

    // =========================================================================
    // Part A — the positive fixture (the law's one shape)
    // =========================================================================

    /**
     * a1 — the dispatch variant's caller-input meta arg must NOT block-convert the predicate.
     * RED at the pre-law head: {@code argOwner.inputs()} is empty inside the variant, so
     * {@code lambdaChannelCallerInputArg} stays false and {@code tryMetaDerefArg} hoists
     * {@code final ReferenceWithMetaSched referenceWithMetaSched = ref.get();} into a converted
     * block, exactly the form the amendment removed on the plain twin.
     *
     * <p>The dispatch group emits ONE file named after the BASE, with each variant as a nested
     * {@code static abstract class} (the golden {@code YearFraction.java} shape), so the lookup
     * is the base's file name.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_dispatchVariantCallerInputMetaArgIsNotHoisted() throws IOException {
        String out = fn("A1DispatchCallerInputArgFilter.java");
        assertTrue(!out.contains("NullSafe(item -> {"),
                "a caller-input meta arg must not block-convert the predicate inside a dispatch"
                        + " variant:\n" + out);
        assertContains(out, "NullSafe(item -> ");
        // PIN AT RED: replace with the byte-exact flat predicate line, captured from the
        // pre-law run of the PLAIN twin (b1) whose form this must match exactly.
    }

    /**
     * a1n — the sibling reading of the same law: the variant body's arg must reach the callee
     * as the input's own value, with NO hoisted wrapper declaration anywhere in the file. Kept
     * separate from a1 so the RED run distinguishes "the block survived" from "a stray decl
     * survived" (the seat-28 a-run distinction).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1n_dispatchVariantHoistsNoWrapperDeclaration() throws IOException {
        String out = fn("A1DispatchCallerInputArgFilter.java");
        String code = codeOnly(out);
        for (String line : code.split("\n")) {
            String s = line.trim();
            assertTrue(!(s.startsWith("final ") && s.contains("WithMeta") && s.contains(" = ")),
                    "no meta-wrapper declaration may be hoisted for a caller-input arg, found <"
                            + s + "> in:\n" + out);
        }
    }

    // =========================================================================
    // Part B — the decline / over-decline pins
    // =========================================================================

    /**
     * b1 — the NON-dispatch twin. The seat-28 amendment already declines here; this law must
     * not change a byte of it (the read collapses to {@code argOwner.inputs()} when
     * {@code dispatchBase()} is empty, which is every plain function).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_plainCallerInputDeclineUnmoved() throws IOException {
        String out = fn("B1PlainCallerInputArgFilter.java");
        assertTrue(!out.contains("NullSafe(item -> {"),
                "the plain-function decline must be unmoved:\n" + out);
    }

    /**
     * b2 — the OVER-DECLINE pin. The widened read must not turn a dispatch variant into a
     * blanket decline: an ITEM-rooted predicate arg has no {@code RSymbolReference} root at all
     * (the walk stops at the implicit item), so the caller-input gate never engages and the
     * law-6 hoist must still fire. Deleting the {@code instanceof RAttribute} root gate — or
     * widening the caller-input set to "any input anywhere" — fails here.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_dispatchItemRootedArgStillHoists() throws IOException {
        String out = fn("B2DispatchItemRootedArgFilter.java");
        assertContains(out, "NullSafe(item -> {");
        assertTrue(codeOnly(out).contains("== null ? null :"),
                "the item-rooted meta arg must still take the guarded deref:\n" + out);
    }

    /**
     * x1 — BANKED GAP, not expressed in the fixture: a dispatch variant declaring its OWN
     * same-named inputs alongside a base. That is the one shape in which the RETIRED
     * name-equality disjunct was reachable (the scope's replace semantics bind the BASE's
     * instance, so identity fails while the names match), and it is the shape that would
     * witness the retirement as a behaviour change rather than a dead-code removal. It is NOT
     * fixtured because upstream ERRORS on a variant-own-input reference (the Seat-1 #444 MF-3
     * probe) and the corpus carries zero such declarations, so a fixture would be pinning a
     * shape the language rejects. Recorded so the retirement's measured-zero mutation lane has
     * a named adjudication rather than a silent one.
     */

    // =========================================================================
    // Part C — the corpus controls (BYTE-NEUTRAL: the residue must not move)
    // =========================================================================

    /** law 6's own healed carriers in cell A — these must stay exactly where seat 28 left them. */
    private static final List<String> CARRIERS_A = List.of(
            "drr/regulation/asic/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java",
            "drr/regulation/esma/emir/refit/trade/reports/SpreadCurrencyOfLeg2Rule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/SpreadCurrencyOfLeg2Rule.java");

    /**
     * control0 — golden is the oracle, so the instrument can fail. If golden's law-6 carriers
     * stop carrying the block form, this suite's scan has stopped measuring anything and every
     * other control is vacuously green.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesTheTargetShapes() throws IOException {
        for (String p : CARRIERS_A) {
            String golden = Files.readString(GOLDEN_A.resolve(p));
            assertTrue(golden.contains("NullSafe(item -> {"),
                    "golden must still carry the law-6 block form: " + p);
            assertTrue(golden.contains("== null ? null :"),
                    "golden must still carry the guarded deref: " + p);
        }
    }

    /**
     * c1 — the byte lock on law 6's healed carriers. This law changes no byte of them; the lock
     * is what turns "byte-neutral" from a claim into a measurement (LAW 80).
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_law6CarriersStayByteWhole() throws IOException {
        for (String p : CARRIERS_A) {
            lockA(p);
        }
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 5.61.0: per file the (block-with-meta-
     * decl, flat filter lambda, total guarded deref) triple must equal golden's, file for file
     * over the UNION, beyond the NAMED residue. For a BYTE-NEUTRAL law the residue is the
     * pin: it must be the SAME set the seat-28 head measured, so any movement — in either
     * direction — is a finding, not a heal.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr561WholeCellFilterSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_561, DOMAIN_DRR561);
    }

    /** control2 — LAW 77 route parity for law 6's carriers under the widened read. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), new ArrayList<>());
        for (String p : CARRIERS_A) {
            assertEquals(drrAOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /**
     * control3 — LAW 79 on cdm 6.20.6, the ENTERED-class tripwire. cdm carries the
     * {@code UpdatePriceAmountForEachMatchingQuantity} family the un-amended law entered and
     * the corpus's only dispatch groups (YearFraction, DayCountBasis,
     * ComputeCalculationPeriod, ProcessFloatingRateReset). Nothing may move here.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkCdm6WholeCellFilterSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "cdm 6.20.6 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "cdm 6.20.6 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM6, DOMAIN_CDM6);
    }

    /**
     * control4 — the DISPATCH-GROUP byte lock. The four corpus dispatch groups are the only
     * files whose {@code inputs()} read this law changes at all; locking them byte-for-byte
     * against golden is the most direct statement of the zero-carrier claim.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control4_dispatchGroupsStayByteWhole() throws IOException {
        assertTrue(!DISPATCH_GROUPS_CDM6.isEmpty(),
                "the dispatch-group set is MEASURED and pinned (Rule 4: per-member verification)"
                        + " - an empty list means an unpinned control, not a passing one");
        for (String p : DISPATCH_GROUPS_CDM6) {
            lockB(p);
        }
    }

    /**
     * The corpus's complete dispatch-group set in cdm 6.20.6 — MEASURED AT THE CHAIN (LAW 82):
     * re-derive with a {@code func <Name>(} scan over the cell's {@code .rosetta} sources before
     * pinning, and record the derivation in the seat notes (Rule 4: per-member verification,
     * never first-three-extrapolated). The four groups the {@code dispatchBaseOf} javadoc names
     * are the expected members.
     */
    private static final List<String> DISPATCH_GROUPS_CDM6 = List.of(
            // MEASURED: `grep -rhoE "^func [A-Za-z0-9_]+ *\(" <cdm-6.20.6>/*.rosetta | sort -u`
            // = EXACTLY these four bases (YearFraction 11 variants, DayCountBasis 9,
            // ProcessFloatingRateReset 5, ComputeCalculationPeriod 3) - the complete
            // per-member enumeration, Rule 4.
            "cdm/base/datetime/daycount/functions/DayCountBasis.java",
            "cdm/base/datetime/daycount/functions/YearFraction.java",
            "cdm/observable/asset/calculatedrate/functions/ComputeCalculationPeriod.java",
            "cdm/product/asset/floatingrate/functions/ProcessFloatingRateReset.java");

    /**
     * MEASURED at the law-6b head (LAW 73: the SET, not the count): the drr 5.61.0
     * residue = SEVEN pre-existing other-family band rows (the law is byte-neutral by
     * charter; the final chain's matrix-unmoved digest is the global proof none of these
     * moved). Either row healing OR a new row appearing fails this control (LAW 81).
     */
    // LAW 81 re-pin (v3.1 flip seat 31, law 1): the asic CustomBasketCodeIdentifier +
    // PlatformIdentifier rows LEFT this list - both files healed WHOLE (byte-identical
    // to golden, locked by IteElseArmRecoveredMetaDerefSeatTest corpus_c1/c2). The
    // heal-tripwire fired exactly as prescribed and the list is re-pinned from the
    // control's own measured print (delta = the two rows REMOVED, nothing else).
    // LAW 81 re-pin (v3.1 flip seat 33, law F.A): the cftc NotionalCurrencyLeg1Rule + jfsa
    // NotionalCurrencyOfLeg1Rule rows LEFT this list - both files healed WHOLE by facet
    // blockArmWrapperHopDeref (byte-identical to golden, locked by BlockArmWrapperHopDerefSeatTest
    // corpus_c1/c2); the list is EMPTY, transcribed from this control's own print (FA-trip1.log).
    private static final List<String> KNOWN_RESIDUE_561 = List.of();
    // LAW 81 re-pin (v3.1 flip seat 31, law 1b): the mas PlatformIdentifierRule row LEFT
    // this list - the file healed WHOLE (byte-identical to golden, locked by
    // MapperFormRuleRootArmSeatTest corpus_c1). Re-pinned from the control's own measured
    // print (delta = the one row REMOVED, nothing else).

    /** MEASURED AT THE CHAIN (LAW 82) — leave empty until the GREEN read fills it. */
    private static final List<String> KNOWN_RESIDUE_CDM6 = List.of();

    /** MEASURED AT THE CHAIN (LAW 73: pin the SET, and the domain with it). */
    private static final int DOMAIN_DRR561 = 1174;

    /** MEASURED AT THE CHAIN (LAW 73: pin the SET, and the domain with it). */
    private static final int DOMAIN_CDM6 = 213;

    /**
     * (T1, T2, T3) per file — the seat-28 law-6 scan verbatim, so the two suites' controls are
     * directly comparable:
     * <ul>
     *   <li><b>T1</b> — a filter-predicate BLOCK whose FIRST inner line declares a meta wrapper
     *       ({@code final &lt;X&gt;WithMeta&lt;Y&gt; &lt;name&gt; = ...;}). Two-line state
     *       machine over the code-only text.</li>
     *   <li><b>T2</b> — a filter-predicate lambda rendered FLAT.</li>
     *   <li><b>T3</b> — the OVER-FIRE NET: the file's TOTAL guarded-deref count
     *       ({@code == null ? null :}), deliberately global.</li>
     * </ul>
     * For THIS law every file must match golden exactly as it did at the seat-28 head — the
     * law moves no T1/T2/T3 anywhere.
     * <p>The three filter member names are the complete set {@code CollectionHandler.filterMethod}
     * can emit; re-verify with a corpus grep before pinning the domain (the LAW-79 domain law).
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            String[] lines = code.split("\n");
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            for (int i = 0; i < lines.length; i++) {
                String s = lines[i].trim();
                boolean isFilterLambda = s.contains("filterSingleNullSafe(")
                        || s.contains("filterItemNullSafe(")
                        || s.contains("filterListNullSafe(");
                if (isFilterLambda && s.endsWith("-> {")) {
                    String next = i + 1 < lines.length ? lines[i + 1].trim() : "";
                    if (next.startsWith("final ") && next.contains("WithMeta")
                            && next.contains(" = ")) {
                        t1++;
                    }
                } else if (isFilterLambda) {
                    t2++;
                }
                int from = 0;
                while ((from = s.indexOf("== null ? null :", from)) >= 0) {
                    t3++;
                    from += "== null ? null :".length();
                }
            }
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * The scan universe is the token-bearing union INTERSECTED with the files this harness
     * emits (rule/report/function kinds): golden's DATA-RULE files carry no filter tokens but
     * the intersection is the shipped correction and is kept verbatim.
     */
    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means "
                        + "an unpinned call site");
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
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 5.61.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 29 law 6b is BYTE-NEUTRAL: nothing here may move.");
    }

    private static void lockB(String path) throws IOException {
        assertNotNull(drrBOutput, "cdm 6.20.6 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drrBGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrBOutput.get(path);
        assertNotNull(generated, "not generated in cdm 6.20.6: " + path);
        Path goldenPath = GOLDEN_B.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated cdm 6.20.6 output must byte-match golden (newline-normalized) for "
                + path + " — seat 29 law 6b: the dispatch groups are the only files whose"
                + " inputs() read changes, and they must not move.");
    }

    // =========================================================================
    // Harness (the seat-26/27 suite shape verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_B_ROOT), errs);
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

    /** The cell through the REAL {@code IRGeneration} seams (the D11 ON ring's wiring). */
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

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat29a.rosetta");
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
            fixtureOut = render(m -> "census.seat29a".equals(m.namespace()));
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
            throw new AssertionError("[DispatchBaseInputDeclineSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

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
}
