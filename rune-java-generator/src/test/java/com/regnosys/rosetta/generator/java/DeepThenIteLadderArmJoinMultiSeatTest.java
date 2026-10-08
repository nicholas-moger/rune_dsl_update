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
 * SEAT 33, law A.5 -- facet {@code deepThenIteLadderArmJoinMulti}: <b>the deep-then CONSUMER
 * handshake joins the ladder's ARM cardinalities</b>. {@code CollectionHandler.tryDeepThenHoist}
 * mints {@code JavaStatementScope.DeepThenIteHoist(consumerCond, consumerMulti)} at its consumer
 * seat (facet {@code deepThenCtlRestructure}, PR #351) from
 * {@code CARDINALITY.computeRuleBody(consumerCond)} alone. {@code computeRuleBody} DOES join a
 * conditional's arms ({@code compute(expr, null, true)}: {@code case RConditionalExpr c ->
 * thenAware ? conditionalCardinality(c, visited) : SINGLE}) and already reads MULTI on 22 of the
 * 220 {@code [P33-CONSUMERMULTI]} rows at this seat, both routes; what it cannot see is the
 * choice-option / piped-list MULTI at the carrier ({@code computed=SINGLE chainProves=true}, 4
 * rows / 1 {@code where=fn:GetBasketConstituents}) -- the review of this seat corrected the
 * draft's "SINGLE on both paths" reading. The law ORs in the OR-join that already exists and is
 * already consulted by the block-form sibling (the ladder-block converter's consult) --
 * {@code NavigationHandler.chainProvesMulti} ({@code NH:5133},
 * public) over {@code conditionalLadderProvesMulti} ({@code NH:5795}, facet
 * {@code blockLambdaCardinalityJoin}, PR #373: "upstream CardinalityProvider joins a
 * conditional's arm cardinalities with OR -- ANY multi arm makes the whole block MULTI").
 * LAW 69: two halves of one question now read one design.
 *
 * <p><b>ONE FLAG, TWO GOLDEN SIGNATURES - AND A SECOND RUNG FOR THE THIRD.</b> {@code multi} is read ONCE, at
 * {@code ControlFlowHandler:313-314} ({@code String mapperSimple = multi ? "MapperC" :
 * "MapperS"}), and from there the decl ({@code CFH:797-799}), the per-arm wrap
 * ({@code wrapDeepThenIteArm}, {@code CFH:1056-1061}, facet {@code inLambdaCondBaseArmChainDecomp}
 * PR #381) and the elseless terminal ({@code CFH:765-768}) follow - but the draft's claim that the ARMS
 * follow too was REFUTED by the first GREEN run (A5-green1.log: decl and terminal MapperC, both
 * only-element arms still bare). The [P33-A5ARM] runtime probe at that seat showed why: the
 * only-element re-wrap reaches {@code wrapDeepThenIteArm} NULL-typed with NO unwrap contract, so
 * the #364 and #381 lifts both decline. Rung 2 (same facet) lifts a null-typed, contract-less
 * {@code RFeatureCall} arm of non-MULTI cardinality rooted at {@code MapperS.of(} as
 * {@code MapperC.of(...)} - 6 of 238 probed arm rows over drr 7.0.0 + 7.3.0 + this fixture = the
 * carrier's four + this suite's two; the 8 RFilterExpr rows sharing the root are excluded by node kind.
 * drr 7.0.0 {@code GetBasketConstituents.java} {@code underliers()}, sigs B019 + B005 (x2 arms)
 * + B037:
 * <pre>
 * GOLDEN  final MapperC&lt;Underlier&gt; ifThenElseResult;
 *         ifThenElseResult = MapperC.of(MapperS.of(thenArg.&lt;OptionPayout&gt;map(…).get()).&lt;Underlier&gt;map("getUnderlier", …));
 *         ifThenElseResult = MapperC.of(MapperS.of(thenArg.&lt;SettlementPayout&gt;map(…).get()).&lt;Underlier&gt;map("getUnderlier", …));
 *         … (the PerformancePayout / CommodityPayout arms are already MapperC in both texts) …
 *         ifThenElseResult = MapperC.&lt;Underlier&gt;ofNull();
 * FORK    final MapperS&lt;Underlier&gt; ifThenElseResult;   /  un-lifted arms  /  MapperS.&lt;Underlier&gt;ofNull();
 * </pre>
 * The source is {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
 * base-trade-basket-func.rosetta:20-29}, {@code func GetBasketConstituents}'s
 * {@code alias underliers}: ONE {@code then} over a MULTI {@code -> payout} pipe whose body is a
 * four-rung elseless ladder -- two {@code only-element -> underlier} arms (genuinely SINGLE,
 * which golden LIFTS) and two bare choice-option arms ({@code PerformancePayout -> underlier},
 * {@code CommodityPayout -> underlier}) that ride the piped list. The alias signature
 * {@code MapperC<? extends Underlier> underliers(Trade)} is ALREADY correct in both texts -- the
 * decl was read from the ladder's own mis-read cardinality, never from the sink.
 *
 * <p><b>RE-SITED BY THE PROBE, and this matters.</b> The seat-A dossier put this law at the k&gt;0
 * LEVEL handshake ({@code CollectionHandler:2992-2993}). {@code [P33-LEVELMULTI]} (LAW-75 round 2
 * at {@code fa49da010}, whole matrix, both routes) carries <b>no carrier row at all</b> -- 40 rows
 * over four GREEN {@code where=} ({@code rule:UnderlyingAssetPriceSourceLeg1/2},
 * {@code fn:StandardizedScheduleVarianceSwapNotionalAmount}, {@code rule:DTCC_UnderlyingAssetReport})
 * -- and {@code wouldFlip=true} is <b>0</b> at the LEVEL mint AND at the BASE mint
 * ({@code [P33-BASEMULTI]}). The anchor is alive ({@code computed=MULTI} on 32 of its 40 rows), so
 * the zero is trusted. The carrier prints at the CONSUMER mint instead:
 * {@code [P33-CONSUMERMULTI] where=fn:GetBasketConstituents seat=consumer computed=SINGLE
 * final=false chainProves=true wouldFlip=true thenKind=RFeatureCall elseKind=RConditionalExpr},
 * x4 cells, and {@code wouldFlip=true} grouped by {@code where=} over its 220 rows is
 * <b>4 rows / 1 {@code where=} = the carrier, ZERO green, row-identical on {@code p33off} and
 * {@code p33on}</b>. That is this law's measured blast radius.
 *
 * <p><b>LAW 80 -- PARTIAL BY DESIGN at this law's head, WHOLE since A.2.</b>
 * {@code GetBasketConstituents} x drr 7.0-7.3 went WHOLE when ALL FIVE seat-33 GBC laws had landed
 * (A.4 {@code lolDefaultBodyMulti} -&gt; A.3 {@code ladderBlockCtorNestedExtract} -&gt; <b>A.5</b>
 * -&gt; A.1 {@code ctorSetterMetaDerefFunctionHost} -&gt; A.2 {@code aliasCondLadderChoiceJoin});
 * 13 sigs, no proper subset closes the file. {@code corpus_c1} pinned the EXACT residue at this
 * law's head and was re-pinned to the whole-file compare as each sibling landed (7 -&gt; 1 at A.1,
 * 1 -&gt; 0 at A.2 / CHECKPOINT 1, {@code ckpt1-gensuite.log}).
 *
 * <p><b>LAW 74 -- a COMPILE REPAIR.</b> {@code target/seat33-instruments/javac33/} row <b>C8a</b>,
 * STANDING <b>3</b> at the seat base: {@code Seat33Pre.java:515} and {@code :517}
 * {@code incompatible types: MapperC<Underlier> cannot be converted to MapperS<Underlier>} (the
 * two un-lifted arms) and {@code :521} {@code MapperS<Underlier> cannot be converted to
 * MapperC<? extends Underlier>} (the {@code return ifThenElseResult;} into the alias signature).
 * <b>PRE 3 -&gt; POST 0</b>, and C8a's whole standing set is this law's. (C8b/C8c -- the basket and
 * pool arms -- are A.4/A.3/A.1's and are NOT touched here.)
 *
 * <p><b>LAW 77 -- INHERITS.</b> {@code IRControlFlowHandler} overrides exactly one method,
 * {@code ifThenElseResultBaseName}; {@code rune-ir-java}'s {@code ConditionalRenderer} is the
 * literal legacy fallback ({@code super.visitConditional(site, ctx)}), and
 * {@code grep -rn 'DeepThenIteHoist' rune-ir-java/src/main/java} is EMPTY, as is
 * {@code grep -rn 'chainProvesMulti' rune-ir-java/src/main/java} -- there is no route twin to
 * keep in step (the seat-31 law-3a lesson). {@code [P33-CONSUMERMULTI]}'s carrier rows print
 * IDENTICALLY on the ON route. {@code corpus_control2} measures it rather than asserting it.
 *
 * <p><b>CLAIMED RED at this law's head-of-branch (both routes):</b> {@code a1},
 * {@code corpus_c1}, {@code corpus_control1} (+ {@code corpus_control2} under {@code -Pir-on}).
 * {@code e1} is GREEN at RED and must never invert -- it is the decline lock.
 * <b>CLAIMED GREEN after the law:</b> 6/0F/1skip default, 6/0F/0skip under {@code -Pir-on}.
 * (CLAIMED -- measured by the chain.)
 *
 * <p><b>MUTATION LANES (LAW 66/76) -- CLAIMED, adjudicated by the chain (LAW 82):</b>
 * <ul>
 *   <li><b>m-lawA5-armjoin</b> ({@code CH_PAIRS_MUT_ARMJOIN}: the {@code chainProvesMulti}
 *       disjunct severed): CLAIMED {@code a1}, {@code corpus_c1}, {@code corpus_control1} fail
 *       ({@code corpus_control2} is the {@code -Pir-on} member); {@code e1} GREEN. LAW 76: the
 *       whole-cell control MOVES -- {@code GetBasketConstituents.java} re-enters
 *       {@code KNOWN_RESIDUE_700} with {@code fork=[1, 0, 0, 3]}.</li>
 *   <li><b>m-lawA5-levelport</b> ({@code CH_PAIRS_MUT_LEVELPORT}: the SAME disjunct additionally
 *       ported to the k&gt;0 LEVEL mint -- the dossier's refuted siting, executed): DECLARED
 *       <b>EMPTY</b>. {@code [P33-LEVELMULTI]} measured {@code wouldFlip=true} = 0 rows on both
 *       routes, so the port must move ZERO bytes and every test here keeps its law-head verdict.
 *       The lane turns that probe zero into a BYTE measurement; it is declared empty, never
 *       claimed as a pass.</li>
 * </ul>
 *
 * <p><b>LAW-81 TRIPWIRES: NONE EXPECTED.</b> All fifteen suites that pin a
 * {@code GetBasketConstituents.java} residue row were re-scored against this law's exact token
 * deltas ({@code target/seat33-instruments/drafts33/A5/NOTES.md} names the walk): the law changes
 * only {@code MapperS} -&gt; {@code MapperC} on the {@code ifThenElseResult} decl/arms/terminal, and
 * every one of those tuples is invariant under it ({@code ofNull()},
 * {@code final Mapper*<…> thenArg}, {@code MapperC.of(<exact single MapperS.of wrap>)} -- the two
 * arms this law adds are CHAINED, not exact-single, so
 * {@code BlockLambdaSingleArmListLiftSeatTest.corpus_control1} does not see them either). If the
 * chain fires one anyway, it is re-pinned from ITS OWN print in this law's commit.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 6/0F/1skip default (f33-green-default.log) /
 * 6/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 4F = a1, corpus_c1, corpus_c2, corpus_control1; {@code -Pir-on} 5F = a1, corpus_c1, corpus_c2, corpus_control1, corpus_control2 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawA5-armjoin}</b> ({@code CH_PAIRS_MUT_ARMJOIN}): MEASURED 6/4F/1skip = a1, corpus_c1, corpus_c2, corpus_control1 - RE-SCORED (superset): c2 fell as well as the claimed a1, c1, control1 - the claim omitted the drr 7.3.0 twin of c1; e1 held.</li>
 *   <li><b>{@code m-lawA5-levelport}</b> ({@code CH_PAIRS_MUT_LEVELPORT}): MEASURED 6/0F/1skip = (none) - EMPTY-as-declared (the refuted k>0 LEVEL siting, applied on top of the law, moves nothing).</li>
 *   <li><b>{@code m-lawA5-armlift}</b> ({@code CFH_PAIRS_MUT_ARMLIFT}): MEASURED 6/4F/1skip = a1, corpus_c1, corpus_c2, corpus_control1 - MATCH (a1, c1, c2, control1).</li>
 * </ul>
 */
class DeepThenIteLadderArmJoinMultiSeatTest {

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

    /** The carrier, identical in drr 7.0.0 / 7.1.0 / 7.2.0 / 7.3.0. */
    private static final String GBC = "drr/base/trade/basket/functions/GetBasketConstituents.java";

    /** Cell A = drr 7.0.0 -- the carrier + the LAW-79 whole-cell union control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.3.0 -- the CROSS-CELL control (7.1/7.2 carry the identical text). */
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
    // The fixture -- reduced from the REAL carrier source
    // (test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
    //  base-trade-basket-func.rosetta:14-29, func GetBasketConstituents's
    //  `alias underliers`)
    // =========================================================================

    /**
     * A FAITHFUL reduction, construct for construct and cardinality for cardinality.
     *
     * <ul>
     *   <li>{@code PayoutA} is a <b>choice</b>, as {@code cdm.product.template.Payout} is in
     *       CDM 6.21.0 (the version drr 7.0.0 pins) -- the arms navigate BARE capitalised
     *       option names off the piped item, which is what makes them disguised heads;</li>
     *   <li>the alias head is a nested FUNCTION CALL over a {@code (0..*)} leaf
     *       ({@code EconomicTermsForProductA(ProductForTradeA(t)) -> payout}), so the single
     *       {@code then} hoists {@code final MapperC<PayoutA> thenArg = …;} exactly as golden
     *       hoists {@code final MapperC<Payout> thenArg = …;} -- the MULTI pipe is the whole
     *       point (the seat-32 E.3 lesson: a single-cardinality reduction of a
     *       multi-cardinality carrier is a NON-WITNESS);</li>
     *   <li>the ladder is ELSELESS and four rungs deep: two {@code only-element -> underlier}
     *       arms (SINGLE) and two bare option arms (which ride the piped list). The OR-join
     *       needs at least one of each to be a witness AND a control at once;</li>
     *   <li>the alias is CONSUMED ({@code add result: us}) so the alias method is emitted.</li>
     * </ul>
     *
     * <p>{@code E1AllSingleLadder} is the DECLINE LOCK: the identical shape with EVERY arm
     * {@code only-element}-collapsed, so no arm proves MULTI, {@code conditionalLadderProvesMulti}
     * answers false and the decl/arms/terminal must stay {@code MapperS} under BOTH states. It is
     * the minimal pair -- same head, same pipe, same rung count, only the arms' collapse differs.
     */
    private static final String MODEL = """
            namespace census.seat33a5
            version "1.0.0"

            type UnderlierA:
                uname string (0..1)

            type OptionPayoutA:
                underlier UnderlierA (0..1)

            type SettlementPayoutA:
                underlier UnderlierA (0..1)

            type PerformancePayoutA:
                underlier UnderlierA (0..1)

            type CommodityPayoutA:
                underlier UnderlierA (0..1)

            choice PayoutA:
                OptionPayoutA
                SettlementPayoutA
                PerformancePayoutA
                CommodityPayoutA

            type EconomicTermsA:
                payout PayoutA (0..*)

            type ProductA:
                eterms EconomicTermsA (1..1)

            type TradeA:
                product ProductA (1..1)

            func ProductForTradeA:
                inputs:
                    t TradeA (1..1)
                output:
                    p ProductA (1..1)
                set p:
                    t -> product

            func EconomicTermsForProductA:
                inputs:
                    p ProductA (1..1)
                output:
                    et EconomicTermsA (1..1)
                set et:
                    p -> eterms

            func A1DeepThenIteLadderArmJoin: <"a1 - THE GetBasketConstituents `alias underliers` SHAPE: ONE then over a MULTI pipe whose body is an elseless choice-option ladder with TWO only-element (SINGLE) arms and TWO bare (MULTI) arms">
                inputs:
                    t TradeA (1..1)
                output:
                    result UnderlierA (0..*)

                alias us:
                    EconomicTermsForProductA(ProductForTradeA(t)) -> payout
                        then if OptionPayoutA exists
                            then OptionPayoutA only-element -> underlier
                            else if SettlementPayoutA exists
                            then SettlementPayoutA only-element -> underlier
                            else if PerformancePayoutA exists
                            then PerformancePayoutA -> underlier
                            else if CommodityPayoutA exists
                            then CommodityPayoutA -> underlier

                add result:
                    us

            func E1AllSingleLadder: <"e1 - the DECLINE LOCK: the identical shape with EVERY arm only-element-collapsed, so NO arm proves MULTI and the MapperS decl/arms/terminal are byte-frozen">
                inputs:
                    t TradeA (1..1)
                output:
                    result UnderlierA (0..*)

                alias us:
                    EconomicTermsForProductA(ProductForTradeA(t)) -> payout
                        then if OptionPayoutA exists
                            then OptionPayoutA only-element -> underlier
                            else if SettlementPayoutA exists
                            then SettlementPayoutA only-element -> underlier
                            else if PerformancePayoutA exists
                            then PerformancePayoutA only-element -> underlier
                            else if CommodityPayoutA exists
                            then CommodityPayoutA only-element -> underlier

                add result:
                    us
            """;

    private static final String DECL_C = "final MapperC<UnderlierA> ifThenElseResult;";
    private static final String DECL_S = "final MapperS<UnderlierA> ifThenElseResult;";
    /** The LIFTED single arm -- golden's {@code MapperC.of(MapperS.of(thenArg…))}. */
    private static final String LIFTED_ARM = "ifThenElseResult = MapperC.of(MapperS.of(thenArg.";
    /** The fork's un-lifted single arm. */
    private static final String UNLIFTED_ARM = "ifThenElseResult = MapperS.of(thenArg.";
    /** The elementwise (already-MapperC) arms -- the law must NOT wrap these. */
    private static final String BARE_ARM = "ifThenElseResult = thenArg.";
    private static final String TERMINAL_C = "ifThenElseResult = MapperC.<UnderlierA>ofNull();";
    private static final String TERMINAL_S = "ifThenElseResult = MapperS.<UnderlierA>ofNull();";

    // =========================================================================
    // Part A -- the reduced fixture (unit grain)
    // =========================================================================

    /**
     * a1 -- the heal, all three signatures at once (rung 1 the decl + terminal, rung 2 the arms). Every assert fails PRE-law for THIS law's
     * reason: the handshake's {@code multi} flag is false, so the decl is {@code MapperS<…>}, the
     * two collapsed arms are assigned bare and the elseless terminal is
     * {@code MapperS.<UnderlierA>ofNull()}. The two elementwise arms are pinned as a COUNT so the
     * law cannot over-wrap them (they are already {@code MapperC} in both texts -- golden's
     * {@code PerformancePayout}/{@code CommodityPayout} arms stay bare).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_deepThenConsumerLadderWithAMultiArm_declaresAndLiftsMapperC() throws IOException {
        String out = fixtureFunction("A1DeepThenIteLadderArmJoin");
        assertContains(out, "final MapperC<PayoutA> thenArg = ");
        assertContains(out, DECL_C);
        assertEquals(2, count(out, LIFTED_ARM),
                "both only-element arms must be lifted MapperC.of(MapperS.of(…)) (golden's B005"
                        + " pair):\n" + out);
        assertContains(out, TERMINAL_C);
        assertEquals(2, count(out, BARE_ARM),
                "the two elementwise arms ride the MapperC pipe and must stay BARE - the law"
                        + " must not over-wrap them:\n" + out);
        assertTrue(!out.contains(DECL_S),
                "the MapperS ite decl must be gone (the fork's B019):\n" + out);
        assertTrue(!out.contains(UNLIFTED_ARM),
                "no un-lifted single arm may survive (the fork's B005):\n" + out);
        assertTrue(!out.contains(TERMINAL_S),
                "the MapperS typed-empty terminal must be gone (the fork's B037):\n" + out);
    }

    /**
     * e1 -- the decline lock. The SAME head, the SAME MULTI pipe, the SAME four rungs; only the
     * arms differ ({@code only-element} everywhere), so no arm proves MULTI,
     * {@code conditionalLadderProvesMulti} answers false and the render must be byte-identical
     * under both states. This is the test that fails if the disjunct is widened into "any
     * conditional over a multi pipe".
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_allSingleArmLadder_keepsTheMapperSDeclAndTerminal() throws IOException {
        String out = fixtureFunction("E1AllSingleLadder");
        assertContains(out, "final MapperC<PayoutA> thenArg = ");
        assertContains(out, DECL_S);
        assertContains(out, TERMINAL_S);
        assertEquals(4, count(out, UNLIFTED_ARM),
                "all four collapsed arms stay bare MapperS assignments:\n" + out);
        assertEquals(0, count(out, LIFTED_ARM),
                "an all-single ladder must gain NO MapperC.of lift:\n" + out);
        assertTrue(!out.contains(DECL_C),
                "an all-single ladder must keep its MapperS decl:\n" + out);
        assertTrue(!out.contains(TERMINAL_C),
                "an all-single ladder must keep its MapperS terminal:\n" + out);
    }

    // =========================================================================
    // Part B -- the corpus (the PARTIAL residue pin + the LAW-79 union control)
    // =========================================================================

    /**
     * corpus_c1 -- {@code GetBasketConstituents} x drr 7.0.0: <b>IMPROVED, NOT WHOLE at this
     * law's head</b> (LAW 80 -- disclosed, not re-scoped); this law owns 4 of the file's 15 hunks
     * (B019, B005 x2, B037). <b>WHOLE since A.2 / CHECKPOINT 1</b>: the residue asserts below now
     * read {@code assertEquals(0, ...)} on both sides -- re-pinned 7 -&gt; 1 at A.1 and 1 -&gt; 0 at
     * CHECKPOINT 1 ({@code 7e94f2d16}, {@code ckpt1-gensuite.log}); the paragraph that follows is
     * the pin's PROVENANCE at this law's head, kept as the record.
     *
     * <p>The pin WAS the EXACT line-set residue at THIS law's head, DERIVED from the seat-33
     * pre-law dumps ({@code p33-off/drr_7.0.0_FUNCTION/{gen,golden}}) by replaying the 15
     * difflib opcodes with A.4's, A.3's and A.5's taken from golden
     * ({@code target/seat33-instruments/drafts33/A5/NOTES.md} names the script):
     * <b>7 golden-only lines and 5 fork-only lines</b>. Six of the seven and four of the five
     * are law A.1's ctor-setter meta-deref family ({@code ctorSetterMetaDerefFunctionHost} --
     * the {@code fieldWithMetaString} hoists, the two guarded {@code .setIdentifier} derefs and
     * the two renumbered consumers); the remaining one on each side is law A.2's
     * ({@code aliasCondLadderChoiceJoin} -- the {@code _underliers} lambda name on the
     * {@code thenArg0} decl). NOTHING here is A.5's.
     *
     * <p><b>THIS TEST IS ORDER-COUPLED.</b> It assumes A.4 ({@code lolDefaultBodyMulti}) and A.3
     * ({@code ladderBlockCtorNestedExtract}) have already landed, per the charter's GBC order
     * A.4 -&gt; A.3 -&gt; A.5 -&gt; A.1 -&gt; A.2. If either lands differently the counts move and the
     * pin is re-taken FROM THIS TEST'S OWN PRINT in this law's commit (print-first pin
     * discipline) -- the law's own assertions above and below the residue block are the ones
     * that must not move.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700GetBasketConstituentsUnderliersIsMapperCTheRestDisclosed()
            throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(GBC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + GBC + ": " + own);
        String fork = normalize(drrAOutput.get(GBC));
        assertNotNull(fork, "not generated: " + GBC);
        String golden = normalize(Files.readString(GOLDEN_A.resolve(GBC)));

        // --- the law's OWN four hunks, against golden's own bytes -------------------------
        assertContains(fork, "final MapperC<Underlier> ifThenElseResult;");
        assertEquals(2, count(fork, "ifThenElseResult = MapperC.of(MapperS.of(thenArg."),
                "golden lifts BOTH only-element arms (B005 x2):\n" + fork);
        assertContains(fork, "ifThenElseResult = MapperC.<Underlier>ofNull();");
        assertTrue(!fork.contains("final MapperS<Underlier> ifThenElseResult;"),
                "the fork's B019 MapperS decl must be gone:\n" + fork);
        assertTrue(!fork.contains("ifThenElseResult = MapperS.of(thenArg."),
                "the fork's B005 un-lifted arms must be gone:\n" + fork);
        assertTrue(!fork.contains("MapperS.<Underlier>ofNull()"),
                "the fork's B037 MapperS terminal must be gone:\n" + fork);
        assertEquals(2, count(fork, "ifThenElseResult = thenArg."),
                "the two elementwise arms stay bare (golden's own form):\n" + fork);

        // --- the DISCLOSED residue: A.1's five sigs + A.2's one --------------------------
        List<String> goldenLines = List.of(golden.split("\n"));
        List<String> forkLines = List.of(fork.split("\n"));
        List<String> goldenOnly = new ArrayList<>(goldenLines);
        goldenOnly.removeAll(forkLines);
        List<String> forkOnly = new ArrayList<>(forkLines);
        forkOnly.removeAll(goldenLines);
        assertEquals(0, goldenOnly.size(),
                "GetBasketConstituents is WHOLE after A.2 (LAW 81 re-pin 1 -> 0 from the"
                        + " checkpoint's own gensuite print, ckpt1-gensuite.log): " + goldenOnly);
        assertEquals(0, forkOnly.size(),
                "GetBasketConstituents is WHOLE after A.2 (LAW 81 re-pin 1 -> 0,"
                        + " ckpt1-gensuite.log): " + forkOnly);
        assertEquals(0, goldenOnly.stream().filter(l -> l.contains("ieldWithMetaString")).count(),
                "law A.1's golden-only lines are GONE (flipped at A.1): "
                        + goldenOnly);
        assertEquals(0, forkOnly.stream().filter(l -> l.contains("ieldWithMetaString")).count(),
                "law A.1's fork-only lines are GONE (flipped at A.1): "
                        + forkOnly);
        assertEquals(0, forkOnly.stream().filter(l -> l.contains("_underliers")).count(),
                "law A.2's lambda-name line is GONE (flipped at A.2): " + forkOnly);
    }

    /**
     * corpus_c2 -- the CROSS-CELL half: drr 7.3.0 carries the identical carrier text (the four
     * cells' generated {@code GetBasketConstituents.java} are ONE byte string --
     * {@code md5 634e3137d981058e1c058d62c77eb48b} across 7.0/7.1/7.2/7.3 on BOTH routes,
     * verified from the seat-33 probe dumps), so one fix heals four cells. This test asserts the
     * law's four hunks in the SECOND cell so a cell-scoped over/under-fire cannot hide.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730GetBasketConstituentsUnderliersIsMapperC() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        List<String> own = drrBGenErrors.stream().filter(e -> e.contains(GBC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + GBC + " (drr 7.3.0): " + own);
        String fork = normalize(drrBOutput.get(GBC));
        assertNotNull(fork, "not generated (drr 7.3.0): " + GBC);
        assertContains(fork, "final MapperC<Underlier> ifThenElseResult;");
        assertEquals(2, count(fork, "ifThenElseResult = MapperC.of(MapperS.of(thenArg."), fork);
        assertContains(fork, "ifThenElseResult = MapperC.<Underlier>ofNull();");
        assertTrue(!fork.contains("MapperS.<Underlier>ofNull()"), fork);
        String golden = normalize(Files.readString(GOLDEN_B.resolve(GBC)));
        assertEquals(count(golden, "ifThenElseResult = "), count(fork, "ifThenElseResult = "),
                "the arm COUNT must equal golden's in the cross cell too");
    }

    /**
     * corpus_control1 -- LAW 79, the UNION whole-cell control over drr 7.0.0.
     *
     * <p>Per file, over a {@code normalize}d line walk, the tuple is
     * <b>(T1, T2, T3, T4)</b> keyed on the ONE local this law's flag names:
     * <ul>
     *   <li><b>T1</b> -- {@code final MapperS<…> ifThenElseResult…;} declarations (the shape the
     *       law REMOVES at the carrier);</li>
     *   <li><b>T2</b> -- {@code final MapperC<…> ifThenElseResult…;} declarations (the shape it
     *       ADDS);</li>
     *   <li><b>T3</b> -- assignments {@code ifThenElseResult… = MapperC.…} (the lifted arms and
     *       the MapperC terminal);</li>
     *   <li><b>T4</b> -- assignments {@code ifThenElseResult… = MapperS.…} (the un-lifted arms
     *       and the MapperS terminal).</li>
     * </ul>
     * The domain is the UNION of the golden-side and fork-side tuple-bearing files intersected
     * with what this harness emits, so a GREEN file that gains a MapperC decl ENTERS the domain
     * and fails both the row set and the count, and a carrier that keeps its MapperS decl fails
     * the rows. Over-fire and under-fire both land here.
     *
     * <p><b>The domain</b> was DERIVED by a read-only walk of
     * {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java}
     * ({@code target/seat33-instruments/drafts33/A5/NOTES.md} names it): <b>209</b> goldens carry
     * a non-zero tuple, of which <b>4</b> sit under {@code .../validation/datarule/} -- a kind
     * {@code DataRuleGenerator} emits and this harness does not run -- so they fall out of the
     * {@code retainAll(emitted)} intersection, leaving 205; the fork adds <b>2</b> band files
     * whose golden tuple is all-zero ({@code Price.java}, {@code QuantityUnitOfMeasure.java}),
     * for <b>207</b>. Re-pin from this control's OWN print if it moves.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_drr700WholeCellIteHoistKindEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_700, DOMAIN_DRR700);
    }

    /** MEASURED by the read-only golden walk at {@code fa49da010} (see the control's javadoc). */
    // LAW 81 re-pin (seat 33, law B.24's live-row census): 207 -> 205 - Price and QuantityUnitOfMeasure left the token-bearing
    // UNION domain (golden carries ZERO of these tokens in them and the fork now matches, so neither side
    // bears a token); the CtorSetterAttrType 444 -> 443 precedent (C.1); from this control's own print (B24-trip5.log).
    private static final int DOMAIN_DRR700 = 205;

    /**
     * The NAMED residue of drr 7.0.0 after law A.5 (LAW 73: pin the SET, not the count). Neither
     * row is this law's: both are files whose fork renders an ite ladder golden does not, and
     * both belong to the Price / QuantityUnitOfMeasure families chartered for laws C.1/C.2 and
     * B.1/B.2/B.3 LATER in this seat. When those land, these rows heal out of this list and the
     * list is re-pinned in THEIR commits, from this control's own print.
     *
     * <p>At the RED head the list carries a THIRD row --
     * {@code drr/base/trade/basket/functions/GetBasketConstituents.java fork=[1, 0, 0, 3]
     * golden=[0, 1, 3, 0]} -- and that row is exactly this law's heal.
     */
    private static final List<String> KNOWN_RESIDUE_700 = List.of(
            // the Price row (fork=[0, 1, 0, 4] golden=[0, 0, 0, 0]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary) took Price WHOLE in all four drr 7.x cells;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            // the QuantityUnitOfMeasure row (fork=[0, 0, 0, 2] golden=[0, 0, 0, 0]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth + iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) took QUOM WHOLE in all four drr 7.x cells - the band's last four files;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            );

    /**
     * corpus_control2 -- LAW 77, the ROUTE gate. {@code rune-ir-java} re-implements neither the
     * deep-then handshake nor {@code chainProvesMulti}, and {@code ConditionalRenderer} delegates
     * wholesale to the legacy {@code visitConditional}, so the IR route should inherit this heal
     * verbatim. Because the file is IMPROVED-not-whole under this law, the assertion is ROUTE
     * IDENTITY (the IR text must equal the legacy text byte for byte) PLUS the law's own four
     * hunks -- not a golden byte compare. Skips unless {@code -Pir-on}.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteRendersTheSameMapperCLadder() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        String ir = normalize(irOut.get(GBC));
        assertNotNull(ir, "IR route did not generate: " + GBC);
        assertContains(ir, "final MapperC<Underlier> ifThenElseResult;");
        assertEquals(2, count(ir, "ifThenElseResult = MapperC.of(MapperS.of(thenArg."), ir);
        assertContains(ir, "ifThenElseResult = MapperC.<Underlier>ofNull();");
        assertEquals(normalize(drrAOutput.get(GBC)), ir,
                "LAW 77: the IR route must render the SAME text as the legacy route for the"
                        + " IMPROVED-not-whole carrier " + GBC);
    }

    // =========================================================================
    // The scan + the union assert (the ChoiceOptionProjectionTypeIdSeatTest shape)
    // =========================================================================

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = new int[4];
            for (String line : normalize(e.getValue()).split("\n")) {
                String s = line.trim();
                if (s.startsWith("final MapperS<") && s.contains(" ifThenElseResult")) {
                    t[0]++;
                } else if (s.startsWith("final MapperC<") && s.contains(" ifThenElseResult")) {
                    t[1]++;
                }
                if (s.startsWith("ifThenElseResult")) {
                    if (s.contains("= MapperC.")) {
                        t[2]++;
                    } else if (s.contains("= MapperS.")) {
                        t[3]++;
                    }
                }
            }
            if (t[0] + t[1] + t[2] + t[3] > 0) {
                out.put(e.getKey(), t);
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
        int[] zero = new int[4];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3, T4) differ beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted tuple-bearing files (" + expectedDomain
                        + ")");
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
    // Fixture harness (the IteChainCtorArmMapperWrapSeatTest helpers, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat33a5.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33a5".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fixtureFunction(String fnName) throws IOException {
        Render r = render();
        String path = fnName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith(path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return normalize(out);
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
            throw new AssertionError("[DeepThenIteLadderArmJoinMultiSeatTest] builtins parse"
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
