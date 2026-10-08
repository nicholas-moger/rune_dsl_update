package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

/**
 * SEAT 31 law 2 (rung 2, SHIPPED) + <b>SEAT 32 law A.2 {@code wrapperItemReceiverBind}</b> --
 * facet {@code lambdaItemReceiverType}: <b>the RECEIVER-RENDER typing channel.</b>
 *
 * <p><b>The mechanism, from the S30 minimal pair + the seat-31 LAW-75 round.</b> GUPIL1 emits
 * its deref hops because its lambda receiver is a declared typed local (the {@code thenArgRef}
 * channel); UPI drops them because its receiver is an INLINE chain and the item render's
 * terminal is {@code JavaExpression.from(name, null)} -- type-less by construction. Both
 * AST-side recoveries measured ZERO corpus-wide (REFUTED); the render truth lives at
 * {@code CollectionHandler.handle(RExtractExpr)}, where the receiver compile is in scope.
 *
 * <p><b>Rung 2 (seat 31, SHIPPED and UNTOUCHED here):</b> the then-piped whole-output ADD
 * terminal derefs element-wise before {@code .getMulti()} when the terminal-meta walk and the
 * body's own inferred type disagree wrapper-vs-bare (the #337 two-halves-agree shape, sited
 * beside its {@code flattenList} precedent). Carrier {@code GetUnderlierLEIForCredit} x4,
 * locked by {@code corpus_c1}.
 *
 * <p><b>SEAT 32, LAW A.2 -- the RE-LANDING of rung 1, narrowed by ONE conjunct.</b> Rung 1 (the
 * item BINDER) was landed at seat 31, measured, and REVERTED at {@code 8ba04381a} as
 * REFUTED-AS-LANDED. As landed it bound the extract receiver's compiled ITEM type for the block
 * lambda's implicit item WHATEVER that type was. At the whole-matrix checkpoint it rendered
 * UnderlierProductIdentifier's ten missing hops (28 -> 2 diff lines) with zero basename-level
 * entries -- and the per-token suite scans then caught what the basename checkpoint cannot see:
 * the mas {@code FixedFloatRateLeg1Rule} / {@code FixedFloatRateLeg2Rule} /
 * {@code InterestRatePriceRule} trio lost golden's single {@code "Type coercion"} (fork 1 -> 0)
 * and its {@code thenArg8} decl flipped {@code FieldWithMetaPriceSchedule -> PriceSchedule}.
 *
 * <p><b>The seat-32 charter's premise -- a RESOLVER-PREFERENCE defect -- is REFUTED, and the
 * refutation is what makes the re-landing one conjunct.</b> The seat-31 probe log
 * ({@code [P31-RECV]}, OFF route) records the trio's extract seat as
 * {@code recvItem=cdm.observable.asset.PriceSchedule recvItemWrapper=false} -- the BARE value
 * type, never a wrapper. No feature was ever re-resolved onto a wrapper: feature ownership at
 * this seat is a PARSER fact ({@code NavigationHandler.navLeafAttrOrNull}, one ladder, no owner
 * election) and {@code ExpressionCompiler.coerceNavigationReceiver} reads ONLY the receiver's
 * compiled item type, never the feature. What rung 1 actually did at the trio was hand the item
 * a meta-BLIND type and thereby CLOSE the {@code getExpressionType() == null} recovery gate at
 * {@code NavigationHandler}:205-207, which is the sole producer of that coercion there.
 *
 * <p>So law A.2 binds ONLY when the compiled receiver item IS a meta wrapper
 * ({@code lawRecvItem instanceof RJavaWithMetaValue}). A bare item type carries nothing the
 * null-typed recovery cannot invent; a wrapper does. The trio's {@code recvItemWrapper=false}
 * seat therefore never binds and keeps today's bytes BY CONSTRUCTION -- and that construction is
 * not asserted, it is TESTED: {@code corpus_cGUARD} locks the trio at corpus grade and the
 * {@code m-lawA2-narrow} lane severs the conjunct alone to reproduce the seat-31 regression on
 * demand.
 *
 * <p><b>Green-safety, MEASURED over the frozen baseline (LAW 75, all 174,141 goldens across the
 * 25 cells -- the manifest's own {@code # generated-java: 174141}, drr 7.0-7.3 and drr 5.61.0
 * included).</b> The
 * newly-typed population is wrapper-typed receivers. Classifying the step that follows each of
 * the 24,004 wrapper {@code map}/{@code mapC} steps in the baseline: 12,081 are the
 * {@code map("Type coercion", ...)} deref; 128 are the wrapper's OWN metafield short forms
 * ({@code getReference}/{@code getMeta}), which {@code NavigationHandler.metaFeatureShortFormOrNull}
 * answers AHEAD of the coercion; the rest are terminals. <b>ZERO goldens navigate a witnessed
 * feature off an undereferenced wrapper</b> -- so the shapes this law moves are exactly the
 * shapes no green file carries.
 *
 * <p><b>CARRIERS (CLAIMED -- measured by the chain):</b> {@code UnderlierProductIdentifier.java}
 * x drr 7.0.0 / 7.1.0 / 7.2.0 / 7.3.0 FUNCTION, WHOLE (class C012, sigs B026 B054 B055; 26 diff
 * lines / 3 hunks each at the seat-32 base, all three sigs the same dropped
 * {@code .<Observable>map("Type coercion", ...)} hop). Ten hops per file: 8
 * {@code .<Observable>...getAsset} / {@code getIndex} hops in the {@code mapSingleToList} block
 * and 2 in the {@code mapSingleToItem} block. The seat-31 measurement of the UN-narrowed rung is
 * the direct evidence for the heal (28 -> 2 diff lines; the residual 2 were the F9 decl, since
 * healed by law 2b -- so 26 -> 0 is the claim here).
 *
 * <p><b>CLAIMED RED at the seat-32 base {@code ddcdd151b}</b> (both routes): {@code corpus_c2},
 * {@code corpus_c3}, {@code corpus_control1}, {@code corpus_control3} (+ {@code corpus_control2}
 * under {@code -Pir-on}). <b>CLAIMED GREEN at the law head:</b> the whole suite. {@code corpus_c1},
 * {@code corpus_cGUARD} and {@code corpus_control0} are GREEN at BOTH states by design --
 * {@code corpus_c1} is rung 2's standing lock, {@code control0} the golden-side oracle (prove the
 * instrument can fail), {@code cGUARD} the DECLINE lock A.2's narrowing conjunct is measured
 * against by {@code m-lawA2-narrow}. {@code corpus_control1}/{@code corpus_control3} are the
 * print-first whole-cell UNION controls (LAW 79) over drr 7.0.0 / drr 5.61.0; control3 is RED at
 * the base on its two rows (asic {@code EffectiveDateRule}, cftc {@code NotionalCurrencyLeg1Rule}),
 * which sibling laws F.1 ({@code ef5122b47}) and C.3 ({@code bf53146fb}) heal before A.2 lands,
 * and its {@code KNOWN_RESIDUE_DRR561} is pinned EMPTY at this head (the seat-32 review's
 * L1-02/L3-02 correction -- commit 18 rewrote the measured line and left this claimed one).
 *
 * <p><b>The 1,801-row {@code wouldBind=true} population, DISCLOSED</b> (the seat-32 review's
 * L5-06): 169 distinct {@code where=} values, 160 of them non-carrier (1,557 rows), split by the
 * probe as 59 no-recovery / 107 idempotent / 3 disjoint -- the three disjoint wheres by name:
 * {@code NotionalCurrencyLeg1Rule} and {@code NotionalCurrencyLeg2Rule} (band, in-charter) and
 * {@code QuantityUnitOfMeasureLeg1Rule} (GREEN, the adjudication target); plus UPI's third
 * would-bind seat ({@code mapItem}, {@code FieldWithMetaString}, x8) the dossier omitted.
 * {@code corpus_control3} (the drr 5.61.0 whole-cell union, {@code KNOWN_RESIDUE_DRR561} EMPTY,
 * with {@code QuantityUnitOfMeasureLeg1Rule.java} carrying 20 {@code map("} steps inside its
 * domain) is the IN-SUITE instrument adjudicating the green target; checkpoint 2's LAW-80
 * "0 ENTERED, both routes" is the corpus-wide backstop.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-lawA2-*.log}):</b>
 * <ul>
 *   <li><b>m-lawA2-binder</b> (the whole bind severed): MEASURED <b>8/3F/1S</b> =
 *       {@code corpus_c2}, {@code corpus_c3}, {@code corpus_control1} -- the claim exactly on
 *       the default route ({@code corpus_control2} is the {@code -Pir-on} member);
 *       {@code corpus_c1}, {@code corpus_cGUARD}, {@code corpus_control0},
 *       {@code corpus_control3} GREEN.</li>
 *   <li><b>m-lawA2-narrow</b> (ONLY the {@code instanceof RJavaWithMetaValue} conjunct severed
 *       -- {@code lawRecvItem != null} restored, the seat-31 shape): MEASURED <b>23/4F/2S</b>
 *       (this suite's 8 + the 15 of the collateral {@code FilterPredicateMetaDerefSeatTest}) =
 *       {@code corpus_cGUARD}, {@code corpus_control3}, and in the collateral suite
 *       {@code corpus_c1b} (the trio's one-line residue lock) AND {@code corpus_control3} (its
 *       drr 5.61.0 whole-cell union, which the trio's lost coercion moves as well);
 *       {@code corpus_c2}/{@code corpus_c3} GREEN. The claim plus one named collateral member:
 *       the reverted seat-31 regression REPRODUCED ON DEMAND -- the narrowing is the fix, not
 *       decoration, MEASURED.</li>
 *   <li><b>m-lawA2-scope</b> (the {@code !mapListToItem && !mapListToList} scoping severed):
 *       MEASURED <b>8/0F/1S -- EMPTY</b>. Re-scored as this javadoc said it would be: the
 *       scoping is defence-in-depth at this corpus -- no whole-list lambda in the drr 7.0.0 or
 *       drr 5.61.0 unions moved when the binder was allowed to reach the {@code mapListToItem}
 *       / {@code mapListToList} families. The category-error argument stands as the reason the
 *       scoping ships; it is un-witnessed here.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code corpus_c2}, {@code corpus_c3},
 * {@code corpus_control1}, {@code corpus_control3} (+ {@code corpus_control2} on
 * {@code -Pir-on}); GREEN at the head 8/0F/1skip default, 8/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW 77.</b> The three seats (CH bind / RH terminals / JSS channel) are all legacy
 * handlers the IR route delegates to, so the RENDER is inherited. The IR CLAIM-side mirrors --
 * {@code IRExpressionCompiler.argItemTypeIsMeta} (:12044) and {@code isGuardedItemNav} (:14186)
 * -- do NOT consult this scope channel. That is byte-safe in the DECLINE direction (a claim the
 * IR declines falls back to the legacy render), and for these carriers it is safe in the CLAIM
 * direction too, analytically: UPI's item is produced by
 * {@code thenArg.<ReferenceWithMetaObservable>map("getObservable", ...)}, whose owner-chain
 * terminal is the meta-annotated {@code observable} attribute, so
 * {@code itemOwnerChainTerminalIsMeta} makes {@code isGuardedItemNav} TRUE and the nav is served
 * by {@code super.visitFeatureCall} -- literally the legacy render ({@code ItemNavRenderer}'s
 * contract). <b>Analytic is not measured:</b> {@code corpus_control2} compares the IR route
 * against GOLDEN for both carriers, and the seat's mid-law checkpoint must dump BOTH routes and
 * show them identical file-for-file. If ON heals fewer hops than OFF, the twin is a FIFTH
 * channel in {@code argItemTypeIsMeta} plus a scope-taking {@code isGuardedItemNav} -- drafted
 * but deliberately NOT landed on a hypothesis.
 *
 * <p><b>LAW 74.</b> {@code UnderlierProductIdentifier} x drr 7.0-7.3 is NON-COMPILING at the
 * seat-32 base (the fork calls {@code observable.getAsset()} on a lambda parameter typed
 * {@code ReferenceWithMetaObservable}, which declares no such method --
 * {@code target/seat31-instruments/javac31/} carries it as a STANDING witness). This law is
 * therefore a LAW-74 REPAIR, MEASURED: {@code javac32-report.md} section 7.3 row C1 --
 * PRE 10 errors (the ten wrapper-item hops, moved INTO the seat's PRE from seat 31's STANDING
 * witness) -> POST 0, the fork's four files compiling at the head.
 *
 * <p><b>No reduced fixture, by the wire-or-delete discipline.</b> The seat-31 {@code a1} was
 * DELETED with its reason recorded: at the measured RED leg the reduced synthetic
 * {@code [metadata reference]} shape ALREADY deref'd through a pre-existing typed channel, so
 * its assert was vacuous as a law witness. Nothing about the narrowing changes that. The
 * witnesses here are corpus-grade and stronger: two whole-file byte compares in two cells, the
 * three-file DECLINE lock, two whole-cell UNION scans, and the two sever lanes.
 *
 * <p><b>LAW-81 tripwires this heal WILL fire in OTHER suites (not edited here; re-pinned from
 * their own prints):</b> {@code CtorSetterValueWrapperRecoverySeatTest.corpus_c1b}
 * ({@code F1_HOPS_AT_LAW8_HEAD} 8 -> 18 == golden's own count) and the UPI residue ROW leaving
 * the union lists of {@code BlockLambdaSingleItemChainStampSeatTest},
 * {@code ChainMapperCRootRungsSeatTest}, {@code CondArmMultiMetaElementDerefSeatTest},
 * {@code DefaultMetaJoinSeatTest}, {@code FilterPredicateMetaDerefSeatTest} and
 * {@code ThenWrappedDefaultSeatTest}.
 */
class ReceiverRenderTypingSeatTest {

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static final String GULEIFC =
            "drr/regulation/common/trade/underlier/functions/GetUnderlierLEIForCredit.java";
    private static final String UPI =
            "drr/base/trade/underlier/functions/UnderlierProductIdentifier.java";

    /** Cell A = drr 7.0.0 -- both carriers + the whole-cell control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    /** Cell B = drr 7.3.0 -- the SECOND cell of the UPI heal (a different transitive CDM). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    /**
     * Cell C = drr 5.61.0 -- the REGRESSION cell. This is the cell the reverted rung broke, and
     * the only reason it is generated here is to hold the narrowing honest.
     */
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_C = CELL_C_ROOT.resolve("rosetta-source/src/generated/java");

    /**
     * The mas trio -- the three drr 5.61.0 report classes whose single {@code "Type coercion"}
     * the un-narrowed rung deleted. NOT carriers: they are byte-equal to golden on this line at
     * both heads, and must stay that way.
     */
    private static final List<String> MAS_TRIO = List.of(
            "drr/regulation/techsprint/g20/mas/reports/FixedFloatRateLeg1Rule.java",
            "drr/regulation/techsprint/g20/mas/reports/FixedFloatRateLeg2Rule.java",
            "drr/regulation/techsprint/g20/mas/reports/InterestRatePriceRule.java");

    /**
     * The exact line the recovery at {@code NavigationHandler}:205-207 synthesizes for the trio,
     * transcribed from golden (line 75 of each of the three files; the fork's text is
     * byte-identical to it at the seat-32 base -- verified in the seat-31 OFF dump).
     */
    private static final String MAS_TRIO_COERCION_LINE =
            ".mapSingleToItem(item -> item.<PriceSchedule>map(\"Type coercion\","
            + " fieldWithMetaPriceSchedule -> fieldWithMetaPriceSchedule == null ? null :"
            + " fieldWithMetaPriceSchedule.getValue()).<BigDecimal>map(\"getValue\","
            + " priceSchedule -> priceSchedule.getValue())).get();";

    /** The golden hop this law restores -- the FIRST of UPI's ten, transcribed from golden. */
    private static final String UPI_GOLDEN_HOP =
            "item.<Observable>map(\"Type coercion\", referenceWithMetaObservable0 ->"
            + " referenceWithMetaObservable0 == null ? null :"
            + " referenceWithMetaObservable0.getValue()).<Asset>map(\"getAsset\","
            + " observable -> observable.getAsset())";

    /** The fork-only band text the law removes -- the undereferenced wrapper nav. */
    private static final String UPI_FORK_BAND_TEXT =
            "if (exists(item.<Asset>map(\"getAsset\", observable -> observable.getAsset())";

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellCAvailable() {
        return Files.isDirectory(GOLDEN_C);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // control0 -- golden is the oracle (prove the instrument can fail)
    // =========================================================================

    /**
     * control0 -- every token this suite asserts is read back off GOLDEN first, so a broken
     * scan cannot pass quietly. Golden UPI carries 20 {@code "Type coercion"} hops (18 of them
     * {@code .<Observable>}) and the restored hop verbatim, and carries the fork's band text
     * nowhere; each mas golden carries EXACTLY ONE coercion, and it is the trio line.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenIsTheOracle() throws IOException {
        String gUpi = normalize(Files.readString(GOLDEN_A.resolve(UPI)));
        assertEquals(20, count(gUpi, "map(\"Type coercion\""),
                "golden UnderlierProductIdentifier must carry 20 coercion hops");
        assertEquals(18, count(gUpi, ".<Observable>map(\"Type coercion\""),
                "18 of golden's hops are the Observable deref (the CtorSetter c1b pin's number)");
        assertTrue(gUpi.contains(UPI_GOLDEN_HOP),
                "golden must carry the restored hop verbatim");
        assertTrue(!gUpi.contains(UPI_FORK_BAND_TEXT),
                "golden must NOT carry the fork's undereferenced wrapper nav");
        if (cellCAvailable()) {
            for (String p : MAS_TRIO) {
                String g = normalize(Files.readString(GOLDEN_C.resolve(p)));
                assertEquals(1, count(g, "\"Type coercion\""),
                        "golden must carry EXACTLY ONE coercion in " + p);
                assertTrue(g.contains(MAS_TRIO_COERCION_LINE),
                        "golden's one coercion must be the trio line in " + p);
            }
        }
    }

    // =========================================================================
    // The carriers
    // =========================================================================

    /**
     * corpus_c1 -- the {@code GetUnderlierLEIForCredit} whole-file heal (seat 31, rung 2).
     * UNCHANGED by seat 32; kept because rung 2 still ships and this is its only byte lock.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700GetUnderlierLEIForCreditMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(GULEIFC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + GULEIFC + ": " + own);
        String gen = drrAOutput.get(GULEIFC);
        assertNotNull(gen, "not generated: " + GULEIFC);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(GULEIFC))), normalize(gen),
                "GetUnderlierLEIForCredit must byte-match golden - seat 31 law 2 rung 2: the"
                + " then-piped ADD terminal derefs element-wise before .getMulti()");
    }

    /**
     * corpus_c2 -- {@code UnderlierProductIdentifier} at drr 7.0.0, PROMOTED from the seat-31
     * F9-decl content pin to a WHOLE-FILE byte compare: law A.2 closes the file's last family.
     * The two F9 asserts are kept AHEAD of the byte compare so the {@code m-law2b} lane (law
     * 2b's deep-then decl-only wildcard gate) still names its own line, and so a decl
     * regression reports as a decl regression rather than as a 371-line diff.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_drr700UnderlierProductIdentifierMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(UPI)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + UPI + ": " + own);
        String gen = drrAOutput.get(UPI);
        assertNotNull(gen, "not generated: " + UPI);
        gen = normalize(gen);
        assertTrue(!gen.contains("final MapperC<ReferenceObligation> thenArg0;"),
                "the F9 bare decl must stay healed (seat-31 law 2b)");
        assertTrue(gen.contains("final MapperC<? extends ReferenceObligation> thenArg0;"),
                "the F9 wildcard decl must match golden (seat-31 law 2b)");
        assertTrue(gen.contains(UPI_GOLDEN_HOP),
                "law A.2: the wrapper item must deref before the feature hop");
        assertTrue(!gen.contains(UPI_FORK_BAND_TEXT),
                "law A.2: the undereferenced wrapper nav must be gone (it does not compile)");
        assertEquals(20, count(gen, "map(\"Type coercion\""),
                "law A.2 restores all ten dropped hops (10 -> 20 == golden)");
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(UPI))), gen,
                "UnderlierProductIdentifier must byte-match golden at drr 7.0.0 - seat 32 law"
                + " A.2 closes the file's last family (F1)");
    }

    /**
     * corpus_c3 -- the SECOND cell. drr 7.3.0 resolves against a different transitive CDM
     * (6.21.0) and rune-fpml (2.1.1) than 7.0.0, so this is a genuinely independent render even
     * though the two goldens happen to be byte-identical.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c3_drr730UnderlierProductIdentifierMatchesGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        List<String> own = drrBGenErrors.stream().filter(e -> e.contains(UPI)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + UPI + ": " + own);
        String gen = drrBOutput.get(UPI);
        assertNotNull(gen, "not generated: " + UPI);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(UPI))), normalize(gen),
                "UnderlierProductIdentifier must byte-match golden at drr 7.3.0 too - the law"
                + " is cell-independent (7.1.0 / 7.2.0 carry the same row)");
    }

    // =========================================================================
    // cGUARD -- THE REGRESSION LOCK the seat-31 revert was bought with
    // =========================================================================

    /**
     * corpus_cGUARD -- <b>the assertion the basename checkpoint structurally cannot make.</b>
     * The mas trio's receiver item is the BARE {@code PriceSchedule}
     * ({@code [P31-RECV] recvItemWrapper=false}), so the narrowed binder must never touch it and
     * the {@code getExpressionType() == null} recovery at {@code NavigationHandler}:205-207 must
     * keep synthesizing golden's single coercion. EXACTLY ONE occurrence per file, and it must
     * be the trio line verbatim -- a count-only assert would pass a swap.
     *
     * <p>This is law A.2's DECLINE lock (the e1 of this seat, at corpus grade rather than
     * fixture grade), and the {@code m-lawA2-narrow} lane's target.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_cGUARD_masTrioKeepsItsOneTypeCoercion() throws IOException {
        assertNotNull(drrCOutput, "drr 5.61.0 generation did not run");
        for (String p : MAS_TRIO) {
            String gen = drrCOutput.get(p);
            assertNotNull(gen, "not generated: " + p);
            gen = normalize(gen);
            String golden = normalize(Files.readString(GOLDEN_C.resolve(p)));
            assertEquals(1, count(gen, "\"Type coercion\""),
                    "the narrowing must leave the bare-item recovery alone: EXACTLY ONE"
                    + " coercion in " + p + " (the un-narrowed seat-31 rung made it 0)");
            assertEquals(count(golden, "\"Type coercion\""), count(gen, "\"Type coercion\""),
                    "fork and golden must agree on the coercion count in " + p);
            assertTrue(gen.contains(MAS_TRIO_COERCION_LINE),
                    "the one coercion must be the trio line verbatim in " + p + ":\n" + gen);
        }
        // NOT asserted here, deliberately: the trio's `thenArg8` decl element
        // (`MapperS<PriceSchedule>` where golden keeps `MapperS<FieldWithMetaPriceSchedule>`)
        // is the OPEN B074 residue, not this law's. Its lock already exists and is the
        // tightest one in the tree - FilterPredicateMetaDerefSeatTest.corpus_c1b pins the
        // trio at EXACTLY one fork-only and one golden-only line. That suite is NOT edited by
        // this seat; it must stay GREEN at the law head, and the m-lawA2-narrow lane is
        // expected to fire it (2 fork-only lines) alongside cGUARD.
    }

    // =========================================================================
    // The whole-cell UNION controls (LAW 79)
    // =========================================================================

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. Domain = every emitted file
     * whose GOLDEN <b>or</b> FORK text carries any {@code map("}/{@code mapC("} step; per file
     * the (coercion hops, total map/mapC steps) PAIR must equal golden's beyond the NAMED
     * residue. T1 is the law's own observable; T2 is the over-fire net -- deliberately global,
     * so a hop added in one place cannot cancel a hop lost in another.
     *
     * <p>This control fails when the law OVER-fires (a green file gains or loses a hop: it is a
     * UNION, so a file entering from either side is compared) and when it UNDER-fires (UPI's
     * row stays in the residue).
     *
     * <p>The GOLDEN-side domain is pinned exactly and is derivable without the generator: a
     * read-only walk of {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java}
     * (7,808 goldens) finds 4,223 carrying a {@code map("} or {@code mapC("} step
     * ({@code target/seat32-instruments/drafts32/A2/derive-domain.py}). The UNION domain also
     * depends on which of those the fork emits, so it carries the print-first SENTINEL until
     * the chain prints it.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellCoercionShapeEqualsGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_A));
        assertEquals(GOLDEN_DOMAIN_DRR700, golden.size(),
                "the GOLDEN tree this control scans must be the frozen one (read-only walk:"
                + " 4223 of 7808 goldens carry a map(/mapC( step)");
        assertUnionEqual(scan(drrAOutput), golden, drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, UNION_DOMAIN_DRR700);
    }

    /**
     * control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN. The CH/RH/JSS seats are legacy handlers the IR
     * route delegates to, and UPI's guarded item nav is served by {@code super.visitFeatureCall}
     * (the decline's own call), so the ON route must reach the SAME bytes. Both carriers are
     * compared against GOLDEN, and UPI additionally route-to-route so a shared-wrong render
     * still reports as a route fact.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForBothCarriers() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(GULEIFC))),
                normalize(irOut.get(GULEIFC)), "IR route vs GOLDEN: " + GULEIFC);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(UPI))),
                normalize(irOut.get(UPI)),
                "IR route vs GOLDEN: " + UPI + " - if this fails while corpus_c2 passes, the"
                + " IR CLAIM mirrors (argItemTypeIsMeta / isGuardedItemNav) need the FIFTH"
                + " channel twin (LAW 77)");
        assertEquals(drrAOutput == null ? null : normalize(drrAOutput.get(UPI)),
                normalize(irOut.get(UPI)), "route divergence on " + UPI);
    }

    /**
     * control3 -- LAW 79 in the REGRESSION cell (drr 5.61.0), the same PAIR. The trio's three
     * files are three rows of this scan; the other ~3,000 are the reason it exists -- the
     * un-narrowed rung's blast radius was never established beyond the three files the
     * per-token scans happened to look at, and this closes that gap.
     *
     * <p>GOLDEN-side domain from the same read-only walk: 3,001 of 5,249 goldens.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_control3_forkDrr561WholeCellCoercionShapeEqualsGolden() throws IOException {
        assertNotNull(drrCOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrCGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_C));
        assertEquals(GOLDEN_DOMAIN_DRR561, golden.size(),
                "the GOLDEN tree this control scans must be the frozen one (read-only walk:"
                + " 3001 of 5249 goldens carry a map(/mapC( step)");
        assertUnionEqual(scan(drrCOutput), golden, drrCOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, UNION_DOMAIN_DRR561);
    }

    // =========================================================================
    // Pins
    // =========================================================================

    /**
     * DERIVED by a read-only walk of the FROZEN golden tree at the seat-32 base -- the count of
     * drr 7.0.0 goldens carrying a {@code map("} or {@code mapC("} step (4,223 of 7,808). Not a
     * generator measurement: it locks the corpus side of the control, so a corpus drift or a
     * silently-empty golden read fails loudly instead of making the union vacuous.
     */
    private static final int GOLDEN_DOMAIN_DRR700 = 4223;

    /** DERIVED the same way for drr 5.61.0: 3,001 of 5,249. */
    private static final int GOLDEN_DOMAIN_DRR561 = 3001;

    ///PIN: SENTINEL (-1) -- the print-first domain-pin discipline. The UNION domain is
    ///PIN: (golden-scan UNION fork-scan) INTERSECT emitted, so it cannot be derived without
    ///PIN: running the generator. The sentinel assert FAILS and PRINTS "MEASURED domain=N
    ///PIN: residue=[...]"; the lead transcribes N here, in the same commit, from that print.
    private static final int UNION_DOMAIN_DRR700 = 2064;   // MEASURED at A2-red2.log ('MEASURED domain=2064'), transcribed

    ///PIN: SENTINEL (-1) -- as above, for drr 5.61.0.
    private static final int UNION_DOMAIN_DRR561 = 1476;   // MEASURED at A2-red2.log ('MEASURED domain=1476 residue=[]'), transcribed

    /**
     * DERIVED at the seat-32 base from the OFF-route band dump: every non-band file in the cell
     * is byte-identical to golden and therefore cannot contribute a row, so the residue is
     * exactly the band files whose PAIR differs. At the base that set is EIGHT; law A.2 removes
     * {@code UnderlierProductIdentifier.java fork=[10, 159] golden=[20, 169]} and leaves these
     * SEVEN, each an OTHER family's charter (F7/F9/F13/F22..F26, the CDE and UATPI faces).
     *
     * <p>Sorted as {@code assertUnionEqual} prints them (a {@code TreeSet} universe).
     * TRANSCRIBE from the control's own print at the first chain run if it disagrees -- a
     * disagreement is a MEASUREMENT, not a licence to weaken the list.
     */
    ///PIN: TRANSCRIBED from the control's own RED print at the A.1 head (A2-red2.log): the draft's
    ///PIN: seven rows were derived from the 88-band dumps; four of them (IndicatorOfTheUnderlyingIndex,
    ///PIN: UnderlyingIndexIndicator, UATPI Leg1/Leg2) were healed WHOLE by laws D.2 / D.1 / A.1 before
    ///PIN: this law landed. The RED print read FOUR rows (UnderlierProductIdentifier + these three);
    ///PIN: the GREEN print reads these THREE (UnderlierProductIdentifier LEFT - this law's heal).
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of(
            // the Price row (fork=[17, 83] golden=[20, 90]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // LAW 81 re-pin (seat 33, law C.1): fork=[10, 76] -> fork=[17, 83] - the statement ladder moved this scan's fork side toward golden; the file stays BANDED on C.2's default join; from C1-trip1.log.
            // the QuantityUnitOfMeasure row (fork=[3, 29] golden=[2, 28]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            // LAW 81 re-pin (seat 33, law C.2): fork=[1, 27] -> fork=[3, 29] - R3a's join deref moved this scan's fork side (LAW 80 IMPROVED-not-whole, planned; QUOM closes at B.24); from C2-trip1.log.
            // the TotalNotionalQuantity row (fork=[5, 65] golden=[12, 72]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /**
     * DERIVED the same way for drr 5.61.0: TWO of the cell's thirteen band files differ on the
     * PAIR, and law A.2 must move NEITHER. The mas trio is NOT in this list and must never
     * enter it -- that is the whole point of {@code corpus_control3}.
     */
    ///PIN: TRANSCRIBED from the control's own RED print (A2-red2.log: 'residue=[]'): the draft's two
    ///PIN: rows were healed before this law landed - EffectiveDateRule WHOLE by law F.1; NotionalCurrencyLeg1
    ///PIN: IMPROVED by law C.3 to a pair this scan reads as golden's. EMPTY, and law A.2 must keep it so.
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    // =========================================================================
    // The scan + the union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * (T1, T2) per file: the {@code map("Type coercion", ...)} deref hops this law adds, and the
     * file's TOTAL {@code map("}/{@code mapC("} step count (the over-fire net). Domain gate:
     * any map/mapC step at all, so a file that LOSES its only coercion stays in the universe
     * and reports rather than silently leaving the scan.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = normalize(e.getValue());
            int t1 = count(code, "map(\"Type coercion\"");
            int t2 = count(code, "map(\"") + count(code, "mapC(\"");
            if (t1 + t2 > 0) {
                out.put(e.getKey(), new int[] {t1, t2});
            }
        }
        return out;
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[2];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "the pair differs beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    // =========================================================================
    // Harness (the seat-26..31 suite shape verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> drrCOutput;
    private static List<String> drrCGenErrors;

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
                    new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
        }
        if (cellCAvailable()) {
            List<String> errs = new ArrayList<>();
            drrCOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_C_ROOT), errs);
            drrCGenErrors = errs;
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
