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
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 32, law A1 -- facet {@code choiceOptionProjectionTypeId}: a BARE capitalised
 * choice-OPTION name read over a THEN-piped item whose INFERRED type is the choice resolves
 * its attribute off {@code RChoiceTypeRef.asRDataType()}'s bridge, whose option typeCalls are
 * id-less deep copies ({@code RTypeCall.deepCopy} explicitly drops {@code referencedTypeId}).
 * The law routes THAT ONE consumer -- {@code ReferenceHandler.synthesizeImplicitItemBareNav}
 * -- through the seat-13 id+attach bridge {@code NavigationHandler.idCarryingChoiceBridge}
 * (LAW 69: {@code projectedOptionAttribute}, the same projection seats 1/13/29 already
 * share), so the option's own linker id resolves its wrapper and the NEXT hop derefs it.
 *
 * <p><b>GOLDEN left arrow FORK</b> (drr 7.0-7.3 POJO,
 * {@code UnderlyingAssetTradingPlatformIdentifierLeg1Rule} + {@code Leg2Rule}, class C002,
 * sigs B004 B005 B006; 12 lines / 3 hunks each):
 * <pre>
 * GOLDEN  item.&lt;ReferenceWithMetaObservable&gt;map("getObservable", underlier -&gt; underlier.getObservable())
 *             .&lt;Observable&gt;map("Type coercion", referenceWithMetaObservable0 -&gt; referenceWithMetaObservable0 == null ? null : referenceWithMetaObservable0.getValue())
 *             .&lt;Asset&gt;map("getAsset", observable -&gt; observable.getAsset()) ...
 * FORK    item.&lt;ReferenceWithMetaObservable&gt;map("getObservable", underlier -&gt; underlier.getObservable())
 *             .&lt;Asset&gt;map("getAsset", observable -&gt; observable.getAsset()) ...
 * </pre>
 * Six hops per file (4x {@code feature=Asset} + 2x {@code feature=Basket}), 48 over the eight
 * files. The source is {@code standards-iosco-cde-version3-underlier-rule.rosetta:113-146}
 * ({@code then extract (if Observable -> Asset -> Commodity -> exchange exists then ...)})
 * over {@code choice Underlier: Observable [metadata address "pointsTo"=PriceQuantity->observable] | Product}.
 *
 * <p><b>THE MEASURED DISCRIMINATOR</b> ({@code [P32-NAVTYPE]} at {@code NavigationHandler:653}
 * and {@code [P32-SYNTHATTR]} at {@code ReferenceHandler:5655}, LAW-75 probe round of
 * 2026-08-27, whole corpus, BOTH routes): the resolved attribute's
 * {@code typeCall().referencedTypeId()} is EMPTY -- 48 NAVTYPE rows of 444,042 and 96
 * SYNTHATTR rows of 3,318, ALL in the eight carriers, ZERO green, route-identical. The
 * minimal pair sits INSIDE ONE FILE: {@code feature=Observable recvKind=RImplicitVariable
 * attr=Observable metaKind=REFERENCE_WITH_META hasId=false rt=RMissingType wrapper=null
 * result=-} (24 per Leg) against {@code recvKind=RSymbolReference ... hasId=true
 * rt=RChoiceTypeRef wrapper=ReferenceWithMetaObservable
 * result=MapperS&lt;...ReferenceWithMetaObservable&gt;} (4 per Leg). {@code hasId} is the ONLY
 * differing input. The probe also RE-SITED the producing rung: {@code authoritySlot=false
 * byName=true} -- the attribute comes from {@code HandlerHelper.findAttributeOnDataType} on
 * the bridge, not from {@code findChoiceSuperOption}; the DEFECT site
 * ({@code RChoiceTypeRef.asRDataType()}'s id-less copy) is unchanged.
 *
 * <p><b>LAW 74 -- this is a COMPILE REPAIR, not a byte-only heal.</b>
 * {@code target/seat32-instruments/javac32/} (row C13) measured the fork's UATPI text at
 * <b>6 errors per file</b>: {@code cannot find symbol: method getAsset()} x4 and
 * {@code getBasket()} x2, every one {@code location: variable observable of type
 * ReferenceWithMetaObservable} -- one javac error per missing deref. PRE 6 -&gt; POST 0
 * (x8 files = 48 reported errors in the generated tree). The seat-A dossier's
 * "the fork's current UATPI text compiles" note was REFUTED by that run and must not be
 * carried forward.
 *
 * <p><b>Charter / whole ceiling: 8</b> -- Leg1 + Leg2 x drr 7.0.0/7.1.0/7.2.0/7.3.0 POJO.
 * Every non-import sig of those files is this one mechanism and all 48 hops are accounted for.
 *
 * <p><b>LAW 77.</b> The 48 bare {@code feature=Observable} navs and all 48 downstream
 * {@code Asset}/{@code Basket} navs print with IDENTICAL counts and identical fields in the
 * {@code -Pir-on} log, so the IR route delegates these seats to the legacy nav today and
 * inherits the heal. The residual the probe CANNOT see: a newly-typed receiver could flip
 * {@code IRExpressionCompiler.isGuardedItemNav} / {@code argItemTypeIsMeta} into a native
 * render (and {@code IRJavaLeafEmitter} never emits {@code Type coercion}). {@code corpus_control2}
 * is therefore NON-optional and the mid-seat BOTH-ROUTES checkpoint stays MANDATORY.
 *
 * <p><b>CLAIMED RED at the seat's base head</b> (both routes): {@code a1}, {@code corpus_c1},
 * {@code corpus_c2}, {@code corpus_control1} (+ {@code corpus_control2} under {@code -Pir-on});
 * {@code e1} GREEN at RED -- it is the decline lock and must never invert.
 * <b>CLAIMED GREEN at the law head:</b> 6/0F/1skip default, 6/0F/0skip under {@code -Pir-on}.
 * (CLAIMED -- measured by the chain.)
 *
 * <p><b>MUTATION LANES (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-lawA1-*.log}):</b>
 * <ul>
 *   <li><b>m-lawA1-idcarry</b> (the id carry severed -- the arm forced to
 *       {@code return ctr.asRDataType();}): MEASURED <b>6/4F/1S</b> = {@code a1},
 *       {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} -- the claim exactly on
 *       the default route ({@code corpus_control2} is the {@code -Pir-on} member); {@code e1}
 *       GREEN; the whole-cell control MOVES (LAW 76).</li>
 *   <li><b>m-lawA1-scope</b> (the CONSUMER SCOPING severed -- all eight consumers take the
 *       id-carrying bridge): MEASURED <b>6/0F/1S -- EMPTY</b> at this suite's grain, the
 *       designed-empty adjudication. Its real instrument -- the ring + matrix under the lane --
 *       was NOT run: the lane executes this suite alone, and no ring was measured with the
 *       scoping severed. The verdict is therefore recorded as <b>UNMEASURED at ring grain</b>,
 *       not as a pass and not as "defence-in-depth"; the scoping stays as a blast-radius hold
 *       on the seven unmeasured consumers, exactly the reason shape (b) was rejected.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on}); GREEN at the head
 * 6/0F/1skip default, 6/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW-81 TRIPWIRES this law FIRES in OTHER suites</b> (do not edit them from here --
 * the lead re-pins each from its own failing print, in this law's commit):
 * {@code FilterPredicateMetaDerefSeatTest.KNOWN_RESIDUE_7} (both UATPI rows
 * {@code fork=[0, 1, 1] golden=[0, 1, 7]} HEAL and leave the list),
 * {@code CondArmMultiMetaElementDerefSeatTest} ({@code fork=[2, 0, 3] golden=[2, 0, 9]}),
 * {@code ChainMapperCRootRungsSeatTest} ({@code fork=[1, 2, 0, 0] golden=[7, 2, 0, 6]}),
 * {@code BlockLambdaSingleItemChainStampSeatTest} ({@code fork=[3, 3, 3] golden=[9, 3, 3]}).
 *
 * <p><b>This suite's OWN standing tripwire.</b> {@code corpus_control1}'s residue names
 * {@code UnderlierProductIdentifier.java fork=[10, 12, 6] golden=[20, 12, 16]} -- the seat-A
 * law A.2 ({@code wrapperItemReceiverBind}) carrier. If A.2 lands in this seat that row HEALS
 * and this list must be re-pinned in A.2's commit, from this control's own print.
 */
class ChoiceOptionProjectionTypeIdSeatTest {

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

    private static final String LEG1 = "drr/standards/iosco/cde/version3/underlier/reports/"
            + "UnderlyingAssetTradingPlatformIdentifierLeg1Rule.java";
    private static final String LEG2 = "drr/standards/iosco/cde/version3/underlier/reports/"
            + "UnderlyingAssetTradingPlatformIdentifierLeg2Rule.java";

    /** Cell A = drr 7.0.0 -- both carriers + the whole-cell union control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.3.0 -- the CROSS-CELL control (7.1/7.2 carry the identical rows). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
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

    // =========================================================================
    // Fixtures -- reduced from the REAL carrier source
    // (test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
    //  standards-iosco-cde-version3-underlier-rule.rosetta:113-146, rule
    //  UnderlyingAssetTradingPlatformIdentifierLeg1)
    // =========================================================================

    /**
     * The DEPENDENCY namespace -- the corpus condition reproduced: these types are LOADED but
     * NOT GENERATED (the emission filter accepts {@code census.seat32a1} only), exactly as
     * every drr cell navigates vendored cdm types. That filter is the whole mechanism: an
     * id-less option typeCall resolves through {@code GeneratorModel.resolveFromWorkspace},
     * which filters by {@code shouldGenerate}, so a dependency-namespace option type never
     * resolves and {@code metaWrapperOf} reads back {@code RMissingType}.
     *
     * <p>{@code UnderlierC} mirrors cdm's {@code choice Underlier} whose {@code Observable}
     * option is meta-annotated. The corpus carrier uses
     * {@code [metadata address "pointsTo"=...]}; the fixture uses {@code [metadata reference]}
     * -- the SAME {@code REFERENCE_WITH_META} family and the same
     * {@code MetaFieldGenerator.detectMetaKind} answer (the precedent is
     * {@code DisguisedChainOptionHopSeatTest}'s {@code a4}, green today on the identical
     * reduction); the address form is locked by {@code corpus_c1}/{@code corpus_c2}.
     */
    private static final String MODEL_DEP = """
            namespace census.seat32a1.dep
            version "1.0.0"

            type BasketA:
                bname string (0..1)

            type ObservableM:
                basket BasketA (0..1)

            type ProductA:
                pname string (0..1)

            choice UnderlierC:
                ObservableM
                    [metadata reference]
                ProductA

            type PayoutA:
                underlierC UnderlierC (0..1)

            type RootA:
                payoutA PayoutA (0..1)
                payoutB PayoutA (0..1)
            """;

    /**
     * The GENERATED namespace.
     *
     * <p>{@code A1BareOption} is the carrier's shape, hop for hop: THREE extracts, each with a
     * conditional body, the option read BARE inside the third. The third {@code then extract}'s
     * argument is itself an {@code RThenExpr}, which is precisely the shape the structural
     * receiver walk declines (facet {@code rerootItemNav}, PR #282: "it returns null for an
     * RThenExpr argument, the dominant rule-body shape") -- so the item type comes from the
     * INFERRED route, {@code gm.workspace().getInferredType(arg)} yields the CHOICE, and the
     * bare option resolves off {@code asRDataType()}'s bridge. That is the seat.
     *
     * <p>{@code E1ExplicitOption} is the decline lock: the EXPLICIT-receiver twin of the very
     * same option hop (the carrier's own line 119,
     * {@code underlier.UnderlierForProduct(ProductForEvent) -> Observable -> Basket ...},
     * which the fork already renders byte-identically to golden). It reaches the option
     * through the receiver walk / authority path, never through this law's entry point, and
     * its bytes must not move.
     *
     * <p><b>PIN AT RED -- RESHAPED (A1-red1.log).</b> The drafter's three single-arm extracts
     * rendered GREEN at the base: a single-arm conditional's item resolves through the
     * structural walk. The carrier's second extract is a 4-arm {@code else if} LADDER
     * ({@code optionPayout -> underlier ... else if commodityPayout -> underlier}) whose join is
     * what pushes the piped item onto the INFERRED route; the reduction now mirrors it with a
     * two-arm ladder over {@code payoutA/payoutB -> underlierC}. The assertions are unchanged.
     */
    private static final String MODEL_MAIN = """
            namespace census.seat32a1
            version "1.0.0"

            import census.seat32a1.dep.*

            reporting rule A1BareOption from RootA: <"a1 - THE UATPI SHAPE: a BARE capitalised choice-OPTION name over a THEN-piped item whose INFERRED type IS the choice">
                extract
                    (if payoutA -> underlierC exists
                    then payoutA -> underlierC
                    else if payoutB -> underlierC exists
                    then payoutB -> underlierC)
                then extract
                    (if ObservableM -> basket -> bname exists
                    then ObservableM -> basket -> bname)

            reporting rule E1ExplicitOption from PayoutA: <"e1 - the DECLINE LOCK: the EXPLICIT-receiver twin of the same option hop, byte-frozen">
                extract underlierC -> ObservableM -> basket -> bname
            """;

    private static final String WITNESS_STEP =
            ".<ReferenceWithMetaObservableM>map(\"getObservableM\", underlierC -> underlierC.getObservableM())";
    private static final String DEREF_PREFIX =
            ".<ObservableM>map(\"Type coercion\", referenceWithMetaObservableM";
    private static final String NEXT_HOP =
            ".<BasketA>map(\"getBasket\", observableM -> observableM.getBasket())";
    /** The fork's shape: the witness step with NO deref between it and the next hop. */
    private static final String UNDEREFERENCED =
            "underlierC.getObservableM())" + NEXT_HOP;

    // =========================================================================
    // Part A -- the reduced fixture (unit grain)
    // =========================================================================

    /**
     * a1 -- the heal: the bare option gets its wrapper result type, so the NEXT hop derefs it.
     * TWO occurrences (the {@code exists} condition and the {@code then} arm), so the deref
     * lambda params are numbered by the per-method counter exactly as golden's
     * {@code referenceWithMetaObservable0..5} are -- the assertion keys on the prefix and pins
     * the COUNT, never on a suffix.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_bareChoiceOptionOverInferredChoiceItem_nextHopDerefs() throws IOException {
        String out = fixtureRule("A1BareOptionRule.java");
        assertContains(out, WITNESS_STEP);
        assertEquals(2, count(out, DEREF_PREFIX),
                "both bare-option hops must deref (golden's per-method numbered form):\n" + out);
        assertContains(out, "== null ? null : referenceWithMetaObservableM");
        assertContains(out, NEXT_HOP);
        assertContains(out, "import census.seat32a1.dep.ObservableM;");
        assertTrue(!out.contains(UNDEREFERENCED),
                "the fork's un-dereferenced hop must be gone:\n  " + UNDEREFERENCED + "\nin:\n" + out);
    }

    /**
     * e1 -- the decline lock. The EXPLICIT-receiver option hop already renders golden's deref
     * today (it resolves through the receiver/authority walk, whose projection has carried the
     * id since seat 1); the law must leave it byte-identical. ONE occurrence in the method, so
     * the deref param is unnumbered -- the exact string below is the pre-law render and is
     * asserted verbatim.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_explicitReceiverOptionHop_staysByteIdentical() throws IOException {
        String out = fixtureRule("E1ExplicitOptionRule.java");
        assertContains(out, WITNESS_STEP);
        assertContains(out, ".<ObservableM>map(\"Type coercion\", referenceWithMetaObservableM -> "
                + "referenceWithMetaObservableM == null ? null : referenceWithMetaObservableM.getValue())");
        assertContains(out, NEXT_HOP);
        assertEquals(1, count(out, DEREF_PREFIX),
                "the explicit twin carries exactly ONE deref - the law must not add one:\n" + out);
        assertTrue(!out.contains("ObservableM -> ObservableM.getBasket()"),
                "the option's TYPE name must never become a lambda var:\n" + out);
    }

    // =========================================================================
    // Part B -- the corpus (whole-file byte compares + the LAW-79 union control)
    // =========================================================================

    /** corpus_c1 -- the drr 7.0.0 whole-file heals: BOTH legs, byte-for-byte. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700UatpiLeg1AndLeg2MatchGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertCarrierMatchesGolden(drrAOutput, drrAGenErrors, GOLDEN_A, LEG1, "drr 7.0.0");
        assertCarrierMatchesGolden(drrAOutput, drrAGenErrors, GOLDEN_A, LEG2, "drr 7.0.0");
    }

    /** corpus_c2 -- the CROSS-CELL heals: the same two carriers in drr 7.3.0. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730UatpiLeg1AndLeg2MatchGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        assertCarrierMatchesGolden(drrBOutput, drrBGenErrors, GOLDEN_B, LEG1, "drr 7.3.0");
        assertCarrierMatchesGolden(drrBOutput, drrBGenErrors, GOLDEN_B, LEG2, "drr 7.3.0");
    }

    /**
     * corpus_control1 -- LAW 79, the UNION whole-cell control over drr 7.0.0.
     *
     * <p>The scan is the raw generated text (NOT comment/string-stripped: T1 keys on the
     * rendered {@code "Type coercion"} literal, which a stripping pass would erase):
     * <ul>
     *   <li><b>T1</b> -- the law's ADDED shape: {@code >map("Type coercion", referenceWithMeta}
     *       -- ANY {@code ReferenceWithMeta} deref step in the file, not just this carrier's,
     *       so a spurious deref ANYWHERE in the cell fails the control.</li>
     *   <li><b>T2</b> -- the shape that must NOT move: {@code .<ReferenceWithMeta} -- the
     *       wrapper WITNESS. The fork already emits all 7 per carrier; the law adds the deref,
     *       never a witness.</li>
     *   <li><b>T3</b> -- the OVER-FIRE NET: the file's TOTAL guarded-deref count
     *       ({@code == null ? null :}), deliberately global.</li>
     * </ul>
     *
     * <p><b>The domain</b> is the UNION of the golden-side and fork-side T1-bearing files,
     * intersected with what this harness emits -- so a green file that GAINS a deref enters
     * the domain and fails both the row set and the count, and a carrier that loses one fails
     * the rows. DERIVED by the read-only walk {@code target/seat32-instruments/drafts32/A1/domain-walk.py}
     * over {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java} plus the
     * seat-31 final OFF dump's band files: <b>216</b> files, every one of them a
     * {@code .../functions/} or {@code .../reports/} output (so the emitted intersection is a
     * no-op and the pin is exact). Re-pin from this control's OWN print if it moves
     * (print-first pin discipline).
     *
     * <p><b>The residue</b> below is what remains AFTER this law: SIX rows, each another
     * family's band delta, MEASURED at {@code ddcdd151b} from the seat-31 final OFF dump. At
     * the base head the list carries EIGHT -- the two UATPI rows
     * ({@code fork=[1, 7, 1] golden=[7, 7, 7]}) are this law's heal.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_drr700WholeCellOptionDerefsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_700, DOMAIN_DRR700);
    }

    /** MEASURED by {@code domain-walk.py} at {@code ddcdd151b} (see the control's javadoc). */
    private static final int DOMAIN_DRR700 = 216;

    /**
     * The NAMED residue of drr 7.0.0 after law A1 (LAW 73: pin the SET, not the count).
     * Every row is another family's band delta at this head; none is this law's.
     *
     * <p><b>The A.2 tripwire:</b> {@code UnderlierProductIdentifier.java} is the seat-A law A.2
     * ({@code wrapperItemReceiverBind}) carrier. If A.2 lands in this seat its row heals out of
     * this list and the list is re-pinned in A.2's commit, from this control's own print.
     */
    private static final List<String> KNOWN_RESIDUE_700 = List.of(
            // the GetBasketConstituents row (fork=[1, 1, 2] golden=[1, 1, 4]) LEFT this list: law A.1
            // (ctorSetterMetaDerefFunctionHost + the rung-3 ctorSetterHoistTextOrder numbering) took its tuples to
            // golden's in all four drr 7.x cells; the file stays BANDED on law A.2's lambda-name line, which this
            // tuple set cannot see; transcribed from this control's own print (A1-trip1.log).
            // the UnderlierProductIdentifier row (fork=[10, 12, 6] golden=[20, 12, 16]) LEFT this list at seat 32: law A.2 (wrapperItemReceiverBind)
            // healed it WHOLE in all four drr 7.x cells after this suite's pins were measured; transcribed from
            // the checkpoint-2 full-gensuite print (ckpt2-gensuite.log).
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[1, 1, 2] golden=[1, 1, 1]) LEFT this list: law D.2
            // healed it WHOLE two commits before this law landed (the draft derived its rows from the 88-band
            // dumps); transcribed from this control's own print (A1-green1.log).
            // the Price row (fork=[5, 7, 12] golden=[7, 8, 12]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // LAW 81 re-pin (seat 33, law C.1): fork=[1, 7, 10] -> fork=[5, 7, 12] - the statement ladder moved this scan's fork side toward golden; the file stays BANDED on C.2's default join; from C1-trip1.log.
            // the QuantityUnitOfMeasure row (fork=[1, 4, 5] golden=[1, 4, 7]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
               // TRANSCRIBED from the print: scan() keys the fork side on T1 > 0, so a T1-less fork file reads as absent = zeros (the draft counted raw text)
            // LAW 81 re-pin (seat 33, law C.2): fork=[0, 0, 0] -> fork=[1, 4, 5] - R3a's join deref moved this scan's fork side (LAW 80 IMPROVED-not-whole, planned; QUOM closes at B.24); from C2-trip1.log.
            // the TotalNotionalQuantity row (fork=[3, 12, 10] golden=[9, 12, 11]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /**
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN. The probe shows the IR route delegating
     * these navs to the legacy seat with identical counts, so it should inherit the heal; what
     * the probe CANNOT see is whether a newly-typed receiver flips
     * {@code IRExpressionCompiler.isGuardedItemNav} / {@code argItemTypeIsMeta} into a native
     * render (and {@code IRJavaLeafEmitter} never emits {@code Type coercion}). This test is
     * therefore the law's route gate, not a formality. Skips unless {@code -Pir-on}.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForUatpi() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(LEG1))),
                normalize(irOut.get(LEG1)), "IR route vs GOLDEN: " + LEG1);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(LEG2))),
                normalize(irOut.get(LEG2)), "IR route vs GOLDEN: " + LEG2);
    }

    private static void assertCarrierMatchesGolden(Map<String, String> output, List<String> errors,
            Path goldenRoot, String path, String cell) throws IOException {
        List<String> own = errors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + path + ": " + own);
        String gen = output.get(path);
        assertNotNull(gen, "not generated (" + cell + "): " + path);
        assertEquals(normalize(Files.readString(goldenRoot.resolve(path))), normalize(gen),
                cell + " " + path + " must byte-match golden - seat 32 law A1: the bare"
                + " choice-option projection carries the option's linker id, so the next hop derefs");
    }

    // =========================================================================
    // The scan + the union assert (the FilterPredicateMetaDerefSeatTest shape)
    // =========================================================================

    private static final String T1 = ">map(\"Type coercion\", referenceWithMeta";
    private static final String T2 = ".<ReferenceWithMeta";
    private static final String T3 = "== null ? null :";

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String s = e.getValue();
            int[] t = new int[] {count(s, T1), count(s, T2), count(s, T3)};
            if (t[0] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    /**
     * The scan universe is the T1-bearing UNION (golden side + fork side) intersected with the
     * files this harness actually emits (rule/report/function kinds). Golden's POJO/metafields
     * kinds legitimately carry T2/T3 and these generators never produce them; keying the
     * domain on T1 -- the token this law moves -- keeps the control honest AND exactly
     * pinnable, while the union keeps it blind in neither direction (a green file that gains a
     * deref ENTERS the domain).
     */
    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
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
                "the union domain must equal the emitted T1-bearing files (" + expectedDomain + ")");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    // =========================================================================
    // Fixture harness (the InLambdaBoolHoistShadowSeatTest helpers, with the
    // DisguisedChainOptionHopSeatTest two-model filtered render -- the dependency
    // namespace must be LOADED but NOT GENERATED for the mechanism to exist)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel dep = AstBuilder.buildFromString(MODEL_DEP, "seat32a1-dep.rosetta");
        RModel main = AstBuilder.buildFromString(MODEL_MAIN, "seat32a1.rosetta");
        dep.setVersion("0.0.0.test");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(dep);
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        Predicate<RModel> filter = m -> "census.seat32a1".equals(m.namespace());
        GeneratorModel gm = new GeneratorModel(workspace, filter);
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(main, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fixtureRule(String fileName) throws IOException {
        Render r = render();
        List<String> own = r.errors().stream().filter(e -> e.contains(fileName)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + fileName + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith(fileName))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + fileName + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
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
            throw new AssertionError("[ChoiceOptionProjectionTypeIdSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT), errs);
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
