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

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 33, law A.2 -- facet {@code aliasCondLadderChoiceJoin} (sig B016): the ALIAS
 * then-cond ladder's lambda NAME.
 *
 * <p><b>The law.</b> Golden names the hop off an alias from the alias's own CHOICE value
 * type; the fork names it from the alias SYMBOL. drr 7.0-7.3
 * {@code GetBasketConstituents}: golden {@code underliers(trade).
 * <ReferenceWithMetaObservable>map("getObservable", underlier -> underlier.getObservable())},
 * fork {@code ... _underliers -> _underliers.getObservable()}. The {@code underliers} alias
 * is {@code <call> -> payout then if OptionPayout exists then ... only-element -> underlier
 * else if SettlementPayout ... else if PerformancePayout ... else if CommodityPayout ...} --
 * a FOUR-rung ladder whose every arm navigates an attribute typed by the CHOICE
 * {@code Underlier}. {@code NavigationHandler}'s conditional arm joins its branch types with
 * {@code ancestorJoin}, which compares DECLARATION IDENTITY, and every choice narrowing
 * mints a FRESH bridge ({@code idCarryingChoiceBridge} / {@code
 * RChoiceTypeRef.asRDataType}), so the deepest rung joins two fresh {@code Underlier}
 * objects to null and the outer rungs then join against that null. The walk returns null,
 * the naming arm falls back to {@code toLowerCamelCase(symRef.name())} and the scope escapes
 * it to {@code _underliers}.
 *
 * <p><b>MEASURED ({@code verdicts33-A.md} section LAW A.2, round-2 probe at
 * {@code fa49da010}, BOTH routes).</b> Mechanism CONFIRMED --
 * {@code [P33-CONDJOIN] identical=false sameName=true join=- fold=RChoiceTypeRef
 * foldName=Underlier} at the failing rungs, so the fold ANSWERS where the identity join
 * cannot. Siting REFUTED -- {@code arm=join join=- fold!=-} is 3,065 rows over 15
 * {@code where=} and FOURTEEN of them are byte-GREEN
 * ({@code UnderlyingAssetTradingPlatformIdentifierLeg1/2}, {@code FXLeg1/2},
 * {@code Notional}, {@code NotionalLeg}, {@code GetUnderlierProductIdentifierLeg1},
 * {@code SpreadCurrencyLeg2}, {@code SpreadCurrencyOfLeg2},
 * {@code BasketConstituentIdentifier}, {@code BasketConstituentIdentifierSource},
 * {@code MasterAgreementType}, {@code OtherMasterAgreementType},
 * {@code DTCC_Leg1CommodityInstrumentID}); narrowing to {@code sameName=true} still leaves
 * three, and NO printed field separates the carrier from
 * {@code UnderlyingAssetTradingPlatformIdentifierLeg1}. So the fix is NOT inside
 * {@code ancestorJoin} (its NULL contract -- an unrelated pair never becomes a join --
 * stands untouched) and NOT a fold at the shared conditional arm. It is an ALIAS-SCOPED
 * ENTRY POINT: a new private {@code aliasCondLadderChoiceJoin} consulted from ONE site, the
 * alias-naming arm of {@code resolveLambdaVarName}, and only where the walk itself already
 * declined. {@code [P32-ALIASWALK]} measured that decline at <b>4 rows / 1 {@code where=}</b>
 * (the carrier) over 11,957 alias consults OFF and over 2,023 ON -- the fourteen green
 * {@code where=} reach the join through the witness / arity / import consumers and the
 * other naming arms, never through this decline, so they are byte-frozen by construction.
 * <b>The whole-matrix checkpoint that follows this law immediately is the arbiter of the
 * fourteen</b>; {@code corpus_control1} below is this suite's own instrument for them.
 *
 * <p><b>Carriers / whole.</b> {@code GetBasketConstituents} x drr 7.0.0/7.1.0/7.2.0/7.3.0.
 * B016 is the LAST sig of the file, so this law TAKES THE FILE WHOLE -- but only WITH A.4,
 * A.3, A.5 and A.1 already landed (charter rows 7-10). Alone it heals 0 files.
 * <b>KNOWN_RESIDUE at the pre-A.2 head</b> (A.4/A.3/A.5/A.1 in, A.2 out): B016 ONLY -- one
 * line of {@code GetBasketConstituents.java}, the {@code _underliers} lambda. That is not a
 * drafting assumption: replaying the B016 hop backwards onto the golden text reproduces
 * {@code AliasCallInputEscapeSeatTest}'s pinned row for this file EXACTLY
 * ({@code fork=[2, 0, 4, 14] golden=[1, 0, 2, 14]}), so that row is B016's alone and this
 * law closes it.
 *
 * <p><b>CLAIMED sets (measured by the chain).</b> RED at the base and at the pre-A.2 head,
 * both routes: {@code a1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1}
 * (+ {@code corpus_control2} under {@code -Pir-on}). GREEN at RED: {@code e1} (as RESHAPED --
 * the compiled-chain naming lock; its drafted decline-direction premise was refuted at RED,
 * see its javadoc). GREEN at the head: 5/0F/1skip default, 6/0F/0skip {@code -Pir-on}.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- CLAIMED until the chain measures them (LAW 82).</b>
 * <ul>
 *   <li><b>m-a2-fold</b> ({@code src-pairs.py NH_PAIRS_MUT_A2_FOLD}: the ladder fold read
 *       severed -- the consult still runs and still reaches the ladder, but its value is
 *       always null): CLAIMED failing set {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 *       {@code corpus_control1} (the fork-side {@code _underliers ->} count returns to 1 and
 *       the {@code underlier -> underlier.getObservable()} total drops one below golden),
 *       {@code corpus_control2} under {@code -Pir-on}. {@code e1} stays GREEN -- it is a
 *       decline in both states.</li>
 *   <li><b>m-a2-scope</b> ({@code NH_PAIRS_MUT_A2_SCOPE}: the ALIAS scoping removed -- the
 *       identical fold consulted at the SHARED conditional arm, i.e. the siting the verdict
 *       REFUTED): DECLARED, adjudicated by the chain. The drafted measured half ({@code e1}
 *       flipping) was REFUTED at RED ({@code A2-red2.log}): the reduced no-alias ladder
 *       names its hop from the COMPILED CHAIN, so the shared arm's fold is invisible to it
 *       in both states. The lane's witnesses are {@code corpus_control1}'s 27 green-file
 *       locks (UATPI Leg1/Leg2 among them) IF the de-scoped fold moves bytes at the
 *       fourteen green {@code where=}; if the chain measures the lane EMPTY, that is a
 *       FINDING to record (the alias scoping is defence-in-depth at this corpus), not a
 *       lane to delete -- the vteq precedent, and the whole-matrix checkpoint carries the
 *       rest.</li>
 * </ul>
 *
 * <p><b>LAW 77.</b> The seat is {@code NavigationHandler.resolveLambdaVarName}, which the IR
 * route delegates to; the one twin risk -- {@code IRJavaLeafEmitter.emitFieldAccess}, which
 * re-derives a hop name from the IR typed layer -- is REFUTED BY MEASUREMENT, not by
 * reading: seat 32's {@code [P32-IRNAME]} scan of 193,096 ON-route rows found
 * {@code desired=underliers} in ZERO rows corpus-wide and {@code feature=observable} in 108
 * rows with not one {@code recvTypeName=Underlier}, and {@code [P32-ALIASWALK]} fired with
 * IDENTICAL fields on BOTH routes (4 rows / 1 {@code where=} either way) -- the carrier's
 * hop never reaches the IR emitter. The only {@code resolveReceiverDataType} mention in
 * {@code rune-ir-java} is a javadoc line. {@code corpus_control2} measures BOTH-ROUTES-vs-
 * GOLDEN anyway.
 *
 * <p><b>LAW 74.</b> A BYTE law -- {@code javac33} PRE <b>0</b> for B016. A lambda parameter
 * name is semantically inert and the fork's {@code _underliers -> _underliers.getObservable()}
 * compiles; the file's ten STANDING javac errors are A.5's C8a, A.1's C8b and C8c. <b>This
 * law's commit message may NOT say "cannot compile".</b>
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 6/0F/1skip default (f33-green-default.log) /
 * 6/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 4F = a1, corpus_c1, corpus_c2, corpus_control1; {@code -Pir-on} 5F = a1, corpus_c1, corpus_c2, corpus_control1, corpus_control2 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawA2-fold}</b> ({@code NH_PAIRS_MUT_A2_FOLD}): MEASURED 6/4F/1skip = a1, corpus_c1, corpus_c2, corpus_control1 - MATCH (a1, c1, c2, control1; e1 held).</li>
 *   <li><b>{@code m-lawA2-scope}</b> ({@code NH_PAIRS_MUT_A2_SCOPE}): MEASURED 6/0F/1skip = (none) - RE-SCORED: EMPTY - BOTH halves held: consulting the fold at the shared RConditionalExpr arm too changes neither e1's render (the claim's `underlier2` hop did not materialise - the shared arm's ancestorJoin already yields the same type for e1's ladder) nor any of the fourteen green where= rows; the alias scoping is defence-in-depth at this corpus and fixture.</li>
 * </ul>
 */
class AliasCondLadderChoiceJoinSeatTest {

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

    /** The carrier. */
    private static final String GBC =
            "drr/base/trade/basket/functions/GetBasketConstituents.java";

    /** Cell A = drr 7.0.0 (the carrier + the whole-cell control); cell B = drr 7.1.0. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.1.0");
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
    // Fixtures
    // =========================================================================

    /**
     * Reduced from the carrier's REAL source --
     * {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/base-trade-basket-func.rosetta}
     * lines 20-29 ({@code func GetBasketConstituents}, {@code alias underliers}) and its
     * first {@code add result:} hop ({@code underliers -> Observable -> Basket}). The
     * reduction keeps every property the mechanism needs:
     * <ul>
     *   <li>the alias body is {@code <MULTI nav> then <conditional>} -- an {@code RThenExpr}
     *       whose then-body is the ladder (seat-32 {@code [P32-ALIASWALK] bodyKind=RThenExpr
     *       thenBodyKind=RConditionalExpr});</li>
     *   <li>FOUR rungs, the last one ELSE-LESS, exactly as the carrier -- a one-rung
     *       conditional resolves through the arm's else-empty fast path and would be a
     *       NON-WITNESS (the seat-32 E.3 {@code a1} lesson: a reduction that renders green
     *       before the law proves nothing);</li>
     *   <li>the arms mix cardinalities the way the carrier's do -- two through
     *       {@code only-element} (SINGLE) and two elementwise over the MULTI item;</li>
     *   <li>every arm navigates an attribute typed by the SAME {@code choice}, so each arm
     *       mints its own fresh bridge and the identity join fails;</li>
     *   <li>the consuming hop is a CHOICE-OPTION projection off the alias, as in the
     *       carrier ({@code underliers -> Observable}).</li>
     * </ul>
     * Deliberate simplifications, none of them load-bearing for this law: the arms navigate
     * lower-case attribute names rather than the carrier's capitalised disguised form (the
     * law reads the arms' RTYPE, not the nav spelling), and the option carries no
     * {@code [metadata address]} (which only changes the {@code <Type>} witness spelling,
     * never the lambda name).
     */
    private static final String MODEL = """
            namespace census.seat33a2
            version "1.0.0"

            type Obs2:
                code string (0..1)

            type Ins2:
                isin string (0..1)

            choice Underlier2:
                Obs2
                Ins2

            type OptionPay2:
                underlier Underlier2 (0..1)

            type SettlementPay2:
                underlier Underlier2 (0..1)

            type PerformancePay2:
                underlier Underlier2 (0..1)

            type CommodityPay2:
                underlier Underlier2 (0..1)

            type Payout2:
                optionPayout OptionPay2 (0..1)
                settlementPayout SettlementPay2 (0..1)
                performancePayout PerformancePay2 (0..1)
                commodityPayout CommodityPay2 (0..1)

            type Terms2:
                payout Payout2 (0..*)

            func LadderAlias: <"a1 - the GetBasketConstituents alias shape: a then-piped FOUR-rung choice ladder behind an alias, navigated by option">
                inputs:
                    terms Terms2 (1..1)
                output:
                    result string (0..*)

                alias underliers:
                    terms -> payout
                        then if optionPayout exists
                            then optionPayout only-element -> underlier
                            else if settlementPayout exists
                            then settlementPayout only-element -> underlier
                            else if performancePayout exists
                            then performancePayout -> underlier
                            else if commodityPayout exists
                            then commodityPayout -> underlier

                add result:
                    underliers -> Obs2 -> code

            func LadderNoAlias: <"e1 - UnderlyingAssetTradingPlatformIdentifierLeg1's shape: the SAME ladder, NOT an alias body - the discriminator must leave it alone">
                inputs:
                    terms Terms2 (1..1)
                output:
                    result string (0..*)

                add result:
                    terms -> payout
                        then if optionPayout exists
                            then optionPayout only-element -> underlier
                            else if settlementPayout exists
                            then settlementPayout only-element -> underlier
                            else if performancePayout exists
                            then performancePayout -> underlier
                            else if commodityPayout exists
                            then commodityPayout -> underlier
                        then extract Obs2 -> code
            """;

    /**
     * a1 -- the carrier shape end to end. The hop off the alias must be named from the
     * ladder's FOLDED choice type ({@code Underlier2} -> {@code underlier2}), not from the
     * alias symbol. The PRE-law render names it from the symbol and escapes it
     * ({@code _underliers}), so this assert fails at RED for the LAW's own token -- the
     * lambda variable of the alias-rooted hop -- and for no other reason.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_aliasChoiceLadderHopIsNamedFromTheFoldedChoice() throws IOException {
        String out = fixtureFunction("LadderAlias");
        assertContains(out, ", underlier2 -> underlier2.");
        assertTrue(!out.contains("_underliers"),
                "the alias-symbol fallback name must be gone - golden names the hop from the"
                + " ladder's folded choice type:\n" + out);
    }

    /**
     * e1 -- the no-alias neighbour, RESHAPED AT RED (the drafted premise was REFUTED by the
     * measurement -- {@code A2-red2.log}). As drafted this test forbade
     * {@code underlier2 -> underlier2.} on the theory that a no-alias ladder's consuming hop
     * keeps a fallback name until {@code m-a2-scope} de-scopes the fold. MEASURED: the
     * pre-law render ALREADY names the hop {@code underlier2}, because the reduced ladder's
     * arms carry compiled {@code Underlier2} stamps and the {@code mapItem} item-naming
     * channel reads the COMPILED CHAIN -- {@code resolveReceiverDataType}'s conditional arm
     * (where the carrier's null join lives) is never consulted for this shape. The reduction
     * dropped the one feature of the real green neighbour
     * ({@code UnderlyingAssetTradingPlatformIdentifierLeg1}) that starves the compiled
     * channel -- its arms are reporting-rule refs -- so THIS fixture cannot discriminate the
     * alias scoping either way.
     *
     * <p>What it CAN hold, it holds: the compiled-chain naming channel itself. The law (an
     * alias-seat consult, entered only where the walk DECLINED) must leave this render
     * byte-stable -- the hop keeps its measured pre-law name and the ladder decl keeps its
     * stamped kind. The {@code m-a2-scope} discriminator lives in
     * {@code corpus_control1}'s 27 green-file locks (UATPI Leg1/Leg2 among them), and its
     * corpus half stays a CLAIM the chain adjudicates (LAW 82): a measured-EMPTY lane is a
     * FINDING (the alias scoping is defence-in-depth at this corpus), not a lane to delete.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_noAliasLadderKeepsItsCompiledChainNaming() throws IOException {
        String out = fixtureFunction("LadderNoAlias");
        assertContains(out, "underlier2 -> underlier2.");
        assertContains(out, "final MapperC<Underlier2> ifThenElseResult;");
    }

    /** corpus_c1 -- {@code GetBasketConstituents} whole, drr 7.0.0 FUNCTION. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700GetBasketConstituentsMatchesGolden() throws IOException {
        assertCarrierWhole(drrAOutput, drrAGenErrors, GOLDEN_A, "drr 7.0.0");
    }

    /** corpus_c2 -- {@code GetBasketConstituents} whole, drr 7.1.0 FUNCTION. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr710GetBasketConstituentsMatchesGolden() throws IOException {
        assertCarrierWhole(drrBOutput, drrBGenErrors, GOLDEN_B, "drr 7.1.0");
    }

    /**
     * corpus_control1 -- LAW 79, the UNION whole-cell control over drr 7.0.0, in two halves.
     *
     * <p><b>Half 1, the token nets over the WHOLE cell.</b> {@code G_TOKEN} is the golden
     * form of the hop this law renames; {@code F_TOKEN} is the fork form. Both are counted
     * over every golden in the cell AND over every file the fork emits for it -- an
     * UNDER-fire leaves {@code F_TOKEN} at 1 and {@code G_TOKEN} one short of golden; an
     * OVER-fire that touched any of the other 45 golden files carrying {@code G_TOKEN}
     * moves the total the other way. The golden side is pinned as a literal because it
     * cannot move with the law.
     *
     * <p><b>Half 2, the measured would-fire population.</b> The verdict bounds this law's
     * reach above by the fifteen {@code [P33-CONDJOIN] arm=join join=- fold!=-}
     * {@code where=} -- fourteen GREEN and the carrier. {@code CONDJOIN_GREEN} is those
     * fourteen resolved to the 27 files they name in this cell (a {@code where=} is a SIMPLE
     * name and several are declared in more than one namespace: {@code
     * UnderlyingAssetTradingPlatformIdentifierLeg1} alone is a csa file AND an iosco file --
     * the charter's own warning), each locked WHOLE against golden. Two of the fourteen
     * ({@code fn:NotionalLeg}, {@code rule:BasketConstituentIdentifier(Source)}) have no
     * file in drr 7.0.0 -- they are drr 6.x-only -- and are DISCLOSED as uncovered here; the
     * whole-matrix checkpoint that follows this law covers them.
     *
     * <p><b>{@code GOLDEN_DOMAIN}, {@code GOLDEN_G_TOKEN}, {@code GOLDEN_G_FILES} and
     * {@code GOLDEN_F_TOKEN} are DERIVED, not drafted</b> -- by the read-only walk
     * {@code target/seat33-instruments/drafts33/A2/golden-domain-walk.py}, which reproduces
     * this scan outside the JVM over
     * {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java}: 7,808 {@code
     * .java} goldens (35.5 MB), {@code G_TOKEN} 137 occurrences across 46 files,
     * {@code F_TOKEN} 0. A mismatch on the golden side is a WALK-SEMANTICS difference
     * ({@code Files.walk} vs {@code os.walk}), not a defect: transcribe the measured value
     * and say so in the commit.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_drr700WholeCellTokenNetsAndGreenLocks() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");

        List<Path> goldens;
        try (var stream = Files.walk(GOLDEN_A)) {
            goldens = stream.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        Map<String, Integer> goldenGByPath = new LinkedHashMap<>();
        int goldenFiles = 0;
        int goldenG = 0;
        int goldenGFiles = 0;
        int goldenF = 0;
        for (Path p : goldens) {
            goldenFiles++;
            String s = Files.readString(p);
            int g = count(s, G_TOKEN);
            if (g > 0) {
                goldenGFiles++;
                goldenG += g;
                goldenGByPath.put(
                        GOLDEN_A.relativize(p).toString().replace('\\', '/'), g);
            }
            goldenF += count(s, F_TOKEN);
        }
        assertEquals(GOLDEN_DOMAIN, goldenFiles,
                "the GOLDEN domain is pinned from the read-only walk (golden-domain-walk.py,"
                + " which counted " + GOLDEN_DOMAIN + " .java files with os.walk); this side"
                + " of the scan cannot move with the law");
        assertEquals(GOLDEN_G_TOKEN, goldenG, "golden occurrences of G_TOKEN");
        assertEquals(GOLDEN_G_FILES, goldenGFiles, "golden files carrying G_TOKEN");
        assertEquals(GOLDEN_F_TOKEN, goldenF,
                "the fork form must not exist in any golden - it is the defect's signature");

        int goldenGOverEmitted = 0;
        int forkG = 0;
        int forkF = 0;
        for (Map.Entry<String, String> e : drrAOutput.entrySet()) {
            forkG += count(e.getValue(), G_TOKEN);
            forkF += count(e.getValue(), F_TOKEN);
            goldenGOverEmitted += goldenGByPath.getOrDefault(e.getKey(), 0);
        }
        assertEquals(goldenGOverEmitted, forkG,
                "over the UNION of every file the fork emits for drr 7.0.0, the alias-hop"
                + " token must match golden exactly - one short is an UNDER-fire (the carrier"
                + " kept its symbol name), one over is an OVER-fire");
        assertEquals(0, forkF,
                "the fork form must be gone from the whole cell (PRE 1 - the carrier)");

        List<String> missing = CONDJOIN_GREEN.stream()
                .filter(p -> !drrAOutput.containsKey(p)).toList();
        assertEquals(List.of(), missing,
                "the measured would-fire GREEN population must be INSIDE this scan, else"
                + " control1 proves nothing about the fourteen");
        List<String> moved = new ArrayList<>();
        for (String path : CONDJOIN_GREEN) {
            if (!normalize(Files.readString(GOLDEN_A.resolve(path)))
                    .equals(normalize(drrAOutput.get(path)))) {
                moved.add(path);
            }
        }
        assertEquals(List.of(), moved,
                "every GREEN [P33-CONDJOIN] where= must stay byte-golden - these are the"
                + " fourteen the alias scoping exists to protect");
    }

    /**
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN. The seat is
     * {@code NavigationHandler.resolveLambdaVarName}, which the IR route delegates to, and
     * seat 32 measured the {@code IRJavaLeafEmitter.emitFieldAccess} twin risk away
     * (see the class javadoc). The carrier must render golden-identical on the IR route too,
     * and two of the fourteen green {@code where=} must stay byte-identical there as well.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForGetBasketConstituents() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(GBC))),
                normalize(irOut.get(GBC)), "IR route vs GOLDEN: " + GBC);
        for (String path : IR_GREEN_CONTROLS) {
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(path))),
                    normalize(irOut.get(path)), "IR route vs GOLDEN (green control): " + path);
        }
    }

    // =========================================================================
    // The measured pins (LAW 73: pin the SET, not only the count)
    // =========================================================================

    /** Golden's form of the hop this law renames (drr 7.0.0, 137 occurrences / 46 files). */
    private static final String G_TOKEN =
            "\"getObservable\", underlier -> underlier.getObservable()";

    /** The fork's form: the alias-symbol fallback, escaped by the scope. */
    private static final String F_TOKEN = "_underliers -> ";

    /** {@code .java} goldens under the drr 7.0.0 generated tree (golden-domain-walk.py). */
    private static final int GOLDEN_DOMAIN = 7808;

    /** Occurrences of {@code G_TOKEN} across those goldens (golden-domain-walk.py). */
    private static final int GOLDEN_G_TOKEN = 137;

    /** Goldens carrying {@code G_TOKEN} at least once (golden-domain-walk.py). */
    private static final int GOLDEN_G_FILES = 46;

    /** Occurrences of {@code F_TOKEN} across those goldens -- the defect has no golden. */
    private static final int GOLDEN_F_TOKEN = 0;

    /**
     * The fourteen GREEN {@code [P33-CONDJOIN] arm=join join=- fold!=-} {@code where=},
     * resolved to the 27 files they name in drr 7.0.0 by a read-only walk of the golden tree
     * ({@code golden-domain-walk.py --condjoin}). Every one of them is byte-green at this
     * seat's base -- none appears in the seat-32 final band
     * ({@code target/seat32-instruments/artefacts-final32/off-files.txt}, 30 files, of which
     * six are drr 7.0.0 and all six are {@code functions/} carriers), and drr 7.0.0's POJO
     * kind (which is where a {@code reports/*Rule.java} classifies -- {@code
     * D11CorpusRegressionTest.classify} rule 11) is an ACTIVE matrix param, so its silence
     * in the band list is a measurement, not an absence.
     */
    private static final List<String> CONDJOIN_GREEN = List.of(
            // rule:UnderlyingAssetTradingPlatformIdentifierLeg1 (888 rows) - TWO namespaces
            "drr/regulation/csa/rewrite/trade/reports/UnderlyingAssetTradingPlatformIdentifierLeg1Rule.java",
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetTradingPlatformIdentifierLeg1Rule.java",
            // rule:UnderlyingAssetTradingPlatformIdentifierLeg2 (888 rows) - TWO namespaces
            "drr/regulation/csa/rewrite/trade/reports/UnderlyingAssetTradingPlatformIdentifierLeg2Rule.java",
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetTradingPlatformIdentifierLeg2Rule.java",
            // rule:SpreadCurrencyOfLeg2 (266 rows)
            "drr/regulation/common/emir/reports/SpreadCurrencyOfLeg2Rule.java",
            // fn:FXLeg1 / fn:FXLeg2 (152 rows each)
            "drr/regulation/common/functions/FXLeg1.java",
            "drr/regulation/common/functions/FXLeg2.java",
            // fn:Notional (120 rows) - the three cde version functions
            "drr/standards/iosco/cde/version1/quantity/functions/Notional.java",
            "drr/standards/iosco/cde/version2/quantity/functions/Notional.java",
            "drr/standards/iosco/cde/version3/quantity/functions/Notional.java",
            // fn:GetUnderlierProductIdentifierLeg1 (120 rows) - 12 G_TOKEN hops of its own
            "drr/regulation/common/functions/GetUnderlierProductIdentifierLeg1.java",
            // rule:MasterAgreementType (33 rows) - FOUR namespaces
            "drr/regulation/common/emir/reports/MasterAgreementTypeRule.java",
            "drr/regulation/common/trade/contract/reports/MasterAgreementTypeRule.java",
            "drr/regulation/csa/rewrite/trade/reports/MasterAgreementTypeRule.java",
            "drr/regulation/esma/emir/article9/reports/MasterAgreementTypeRule.java",
            // rule:OtherMasterAgreementType (33 rows) - TWO namespaces
            "drr/regulation/common/emir/reports/OtherMasterAgreementTypeRule.java",
            "drr/regulation/common/trade/contract/reports/OtherMasterAgreementTypeRule.java",
            // rule:SpreadCurrencyLeg2 (26 rows) - SEVEN namespaces
            "drr/regulation/asic/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java",
            "drr/regulation/cftc/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java",
            "drr/regulation/csa/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java",
            "drr/regulation/hkma/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java",
            "drr/regulation/jfsa/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java",
            "drr/regulation/mas/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java",
            "drr/regulation/sec/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java",
            // rule:DTCC_Leg1CommodityInstrumentID (15 rows) - THREE namespaces
            "drr/regulation/cftc/rewrite/dtcc/trade/reports/DTCC_Leg1CommodityInstrumentIDRule.java",
            "drr/regulation/common/dtcc/trade/reports/DTCC_Leg1CommodityInstrumentIDRule.java",
            "drr/regulation/csa/rewrite/dtcc/trade/reports/DTCC_Leg1CommodityInstrumentIDRule.java");

    /** The two densest green {@code where=} files, re-checked on the IR route. */
    private static final List<String> IR_GREEN_CONTROLS = List.of(
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetTradingPlatformIdentifierLeg1Rule.java",
            "drr/regulation/common/functions/GetUnderlierProductIdentifierLeg1.java");

    // =========================================================================
    // Shared assertions
    // =========================================================================

    /**
     * The carrier's whole-file lock, with the LAW's own token asserted FIRST so a RED at the
     * pre-A.2 head names B016 rather than a byte diff of thirteen sigs.
     */
    private static void assertCarrierWhole(Map<String, String> output, List<String> genErrors,
            Path goldenRoot, String cellName) throws IOException {
        assertNotNull(output, cellName + " generation did not run");
        List<String> own = genErrors.stream().filter(e -> e.contains(GBC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + GBC + " (" + cellName + "): " + own);
        String gen = output.get(GBC);
        assertNotNull(gen, "not generated: " + GBC + " (" + cellName + ")");
        assertTrue(gen.contains(G_TOKEN),
                "the LAW's token (B016): the alias-rooted hop must be named from the ladder's"
                + " folded choice type in " + cellName + " - expected\n  " + G_TOKEN);
        assertTrue(!gen.contains(F_TOKEN),
                "the alias-symbol fallback must be gone in " + cellName);
        assertEquals(normalize(Files.readString(goldenRoot.resolve(GBC))), normalize(gen),
                "GetBasketConstituents must byte-match golden in " + cellName
                + " - seat 33 law A.2 takes the file WHOLE, with A.4/A.3/A.5/A.1 landed");
    }

    /** Literal, non-overlapping occurrence count (structured content - never a regex). */
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
    // Fixture harness (the ChoiceOptionLadderDeepHopSeatTest renderer, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat33a2.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33a2".equals(m.namespace()));
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
        String out = lookupOrNull(r.output(), path);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static String lookupOrNull(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith(suffix))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
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
            throw new AssertionError("[AliasCondLadderChoiceJoinSeatTest] builtins parse"
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
                    new D11CorpusRegressionTest.CellSpec("drr", "7.1.0", CELL_B_ROOT), errs);
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
