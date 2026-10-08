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
 * SEAT 32, law E3 -- facet {@code explicitClosureParamNameBurn} (the seat-31 law 3 rung (a)
 * re-land, route 1 of the three the bank named): an EXPLICIT Rune closure param
 * ({@code then extract product [ ... ]}) is render TEXT only -- {@code CollectionHandler
 * .resolveParamName} computes a string and registers NOTHING -- so the DEFERRED resolution of
 * a nav hop ONE LAMBDA DEEPER cannot see the name it must escape and emits the shadowing bare
 * form. {@code compileLambda} now BURNS the param on the lambda BODY scope it has just minted
 * ({@code JavaStatementScope.registerNameBurn}, the #363 law: the escaped name lands in the
 * site's taken set and the sentinel renders {@code ""}), so every DESCENDANT deferred lambda
 * param escalates by construction.
 *
 * <p><b>GOLDEN &lt;- FORK</b> (drr 5.61.0 iosco cde {@code CDENotionalRule} /
 * {@code CDENotionalCurrencyRule}; 2 lines / 1 hunk each, the file's WHOLE diff):
 * <pre>
 * golden: .&lt;ContractualProduct&gt;map("getContractualProduct", _product -&gt; _product.getContractualProduct())
 * fork  : .&lt;ContractualProduct&gt;map("getContractualProduct",  product -&gt;  product.getContractualProduct())
 * </pre>
 * both hops sitting inside {@code .mapSingleToItem(product -> { ... return _thenArg
 * .mapSingleToItem(item -> { ... }); })}. The SHALLOW hop on line 71 -- inside {@code product ->}
 * but NOT inside {@code item ->} -- is already byte-correct: the #362
 * {@code deepThenLevelElementPreserve} arm pre-escapes it from the AST. The two hops this law
 * heals are one lambda deeper, on the gm-aware CHAINED arm
 * ({@code NavigationHandler.resolveLambdaVarName}, the {@code recv instanceof RFeatureCall
 * prevFc} branch), which has no AST pre-escape -- and cannot usefully get one, because the IR
 * route re-implements that naming seat with no AST origin pointer to walk (the seat-31 bank
 * {@code law3a-bank2.md}: the legacy-only arm fix measured 4 LEFT / 0 ENTERED on the OFF route
 * and did NOT heal the ON route). Burning at the MINT serves both routes from one seat.
 *
 * <p><b>WHY THIS IS ROUTE-SAFE (LAW 77), stated as CHAIN evidence, not hash equality.</b> The
 * LAW-75 probe's {@code lamScope=} field was dropped at apply time, so the mint's own lambda-body
 * scope identity was never printed. What WAS printed is the NESTED-MINT chain: on the ON route the
 * {@code CDENotional} render prints {@code CPMINT item scope=5fce03cb} -> {@code CPMINT product
 * scope=5fce03cb} -> {@code CPMINT item scope=42a3a47c} -> 24x {@code IRLAM ... scope=6f10e8b0},
 * including {@code [P32-IRLAM] desired=product feature=contractualProduct scope=6f10e8b0
 * taken=false} x2. The THIRD mint reports {@code 42a3a47c} as its PARENT and it is the
 * {@code item ->} lambda golden nests INSIDE {@code product ->}; therefore {@code 42a3a47c} IS the
 * product lambda's {@code bodyCtx.scope()}, and the IR emitter's {@code 6f10e8b0} is that scope's
 * own descendant. {@code CDENotionalCurrency} reproduces the pattern independently
 * ({@code 30711852 -> 2677e264 -> IRLAM 31815cbd}), and corpus-wide 5,077 scope identities are
 * SHARED between {@code CPMINT} parents and {@code IRLAM} scopes -- the emitter runs inside the
 * same scope tree, not a private one. {@code isNameTaken} and {@code computeActualNames} walk
 * ancestors, so an ancestor burn escalates the emitter's deferred param with ZERO new IR state.
 * The IR twin was checked and is NOT needed: {@code IRJavaLeafEmitter.emitFieldAccess} (the one
 * {@code " -> "} emission in {@code rune-ir-java}) names through
 * {@code ctx.scope().registerDeferredLambdaParam(desired)} -- the same scope object -- and
 * {@code IRExpressionCompiler} mints no lambda param of its own (its eleven {@code " -> "} hits
 * are all switch arrows). {@code corpus_control2} is the measurement.
 *
 * <p><b>DISCRIMINATOR.</b> {@code !func.isImplicit() && !func.paramNames().isEmpty()} -- exactly
 * {@code resolveParamName}'s own branch, inverted. MEASURED reach: 47,574 lambda mints at this
 * seat, of which 1,794 are explicit over 151 distinct enclosing fn/rule, 49 distinct rendered
 * names, and {@code takenInParent=false} in ALL 1,794 (route-identical). The implicit 45,780 stay
 * with {@code HandlerHelper.escapedImplicitItemName}'s per-depth {@code item}/{@code _item} escape
 * (#292, 44 measured drr-7.0.0 goldens) and are never double-driven. The universal
 * {@code takenInParent=false} is also what makes the escalation SINGLE-level: the burn's own actual
 * name is the param name itself, so a descendant escapes exactly once ({@code _product}, never
 * {@code __product}).
 *
 * <p><b>GREEN BLAST RADIUS -- MEASURED, and it is ONE named file.</b> Over the ON-route probe log's
 * 106,200 contiguous render blocks, a block is at risk iff an {@code explicit=true} mint's rendered
 * name is ALSO an {@code IRLAM desired=} name in the same block. Result: THREE blocks --
 * {@code rule:CDENotional} ({@code product}, 2 rows, CARRIER), {@code rule:CDENotionalCurrency}
 * ({@code product}, 2 rows, CARRIER) and {@code rule:NotionalQuantityLeg1} ({@code trade}, 30 rows,
 * <b>GREEN</b>). {@code NotionalQuantityLeg1Rule} (drr 5.61.0 asic AND cftc) golden already carries
 * {@code (trade -> } x1 and {@code _trade -> } x5 and is green today, so the burn MUST be
 * IDEMPOTENT there. It is, analytically: those five hops take the #358/#360 pre-escape arm and
 * register the desired name {@code _trade}, which the {@code trade} burn does not touch. {@code e1}
 * locks that shape on the fixture and {@code corpus_c3} pins both golden files byte-identical.
 * NOTE the honest limit: the 3-block figure is an ON-ROUTE measure -- {@code [P32-LAMNAME]} was
 * never applied, so the LEGACY nav-hop naming channel is UNINSTRUMENTED and the mid-seat
 * BOTH-ROUTES whole-matrix checkpoint is MANDATORY for this law.
 *
 * <p><b>COMPILE (LAW 74) -- the fork's current text does NOT compile.</b>
 * {@code target/seat32-instruments/javac32/javac32-report.md} row C21, measured PRE:
 * <pre>
 * Seat32Pre.java:1034: error: variable product is already defined in method pre_cdeNotionalRule(BigDecimal,MapperS&lt;Product&gt;)
 * Seat32Pre.java:1035: error: variable product is already defined in method pre_cdeNotionalRule(BigDecimal,MapperS&lt;Product&gt;)
 * </pre>
 * 2 errors per shape; POST (the golden form) compiles EXIT=0. This is also the green-safety
 * proof: the un-escaped form is not legal Java, so no green file can carry it.
 *
 * <p><b>Charter:</b> {@code CDENotionalRule} + {@code CDENotionalCurrencyRule}, drr 5.61.0, x1 cell
 * = 2 WHOLE files (classes C026 / C027, sigs B086 / B087). LAW 79/80: the whole-cell instrument is
 * {@code corpus_control1} plus the seat chain's matrix digest and per-file accounting.
 *
 * <p><b>CLAIMED RED at the pre-law head (BOTH routes) -- measured by the chain:</b> {@code a1},
 * {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+ {@code corpus_control2} under
 * {@code -Pir-on}). {@code a2}, {@code e1} and {@code corpus_c3} are GREEN at RED by design -- they
 * are the no-move locks, wired to the mutation lanes below, not law witnesses.
 * <b>CLAIMED GREEN at the law head:</b> 8/0F/1skip default, 8/0F/0skip {@code -Pir-on}.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-e3*.log}), with ONE fixture re-scored as a NON-WITNESS
 * and reshaped in the same commit (below):</b>
 * <ul>
 *   <li><b>m-e3</b> (the burn severed): MEASURED <b>8/3F/1S</b> = {@code corpus_c1},
 *       {@code corpus_c2}, {@code corpus_control1} ({@code corpus_control2} is the
 *       {@code -Pir-on} member); {@code a2}/{@code e1}/{@code corpus_c3} GREEN as claimed --
 *       and {@code a1} GREEN, against the claim. See the a1 paragraph.</li>
 *   <li><b>m-e3-explicit</b> (the EXPLICIT gate severed -- implicit lambdas burn too):
 *       MEASURED <b>8/0F/1S -- EMPTY</b>. Re-scored as this javadoc said it would be: at THIS
 *       corpus an implicit {@code item} lambda's burn escalates nothing (no descendant desires
 *       {@code item}; the #292 per-depth escape already owns that population), so the gate is
 *       defence-in-depth here; the #292 double-driving it prevents remains the reason it ships,
 *       un-witnessed.</li>
 *   <li><b>m-e3-sentinel</b> (the sentinel dropped from the emitted text): MEASURED
 *       <b>8/3F/1S</b> = {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} -- the
 *       survival filter discards the sentinel-less burn and the corpus heal vanishes, as claimed;
 *       {@code a1} GREEN, against the claim (the same non-witness).</li>
 *   <li><b>m-e3-scope</b> (the burn on the PARENT scope): MEASURED <b>8/0F/1S -- EMPTY</b>;
 *       {@code a2} and {@code corpus_control1} GREEN. <b>Re-scored:</b> a parent-scope burn
 *       renames only a sibling lambda that DESIRES the param's own name, and neither this
 *       fixture's siblings nor any rule in the drr 5.61.0 union does; the grouping hazard the
 *       {@code registerNameBurn} javadoc documents is real in its own terms and has no witness
 *       here. The body-scope choice ships as documented hazard-avoidance, un-witnessed; a witness
 *       needs a sibling lambda desiring the param's name -- BANKED.</li>
 * </ul>
 * <p><b>a1 -- RE-SCORED FROM THE LOGS (the PIN-AT-RED slip the chain caught).</b> {@code a1}
 * measured GREEN at the chain's base {@code ddcdd151b} (every seat-32 law absent), GREEN under
 * {@code m-e3} and GREEN under {@code m-e3-sentinel}: as committed it never witnessed the burn.
 * Its RED at {@code E3-red2.log} was the draft's {@code expected: <2> but was: <3>} against a
 * render that ALREADY carried three {@code _product32 -> _product32.} hops and zero bare
 * collisions (the #358/#360 AST pre-escape reaches every hop of that reduction); the count was
 * then transcribed from a GREEN print identical to the RED one. RESHAPED in this commit ({@code reshape-e3a1.py}): the fixture gains the carrier's MULTI step
 * ({@code Payout32.optionPayout Opt32 (0..*)}, {@code only-element} in the then arm) and MEASURED
 * standalone -- the src REVERTED: 8/4F with {@code a1} RED on its bare-collision assert (two bare
 * {@code product32 ->} hops in the print); the head: 8/0F/1skip, count 3 transcribed from the print;
 * {@code m-e3} and {@code m-e3-sentinel}: 8/4F each with {@code a1} among them ({@code e3w-*.log}).
 * Chain run 2 measures the reshaped suite in-chain; its RED and lane sets read 4F with {@code a1}.
 * <p>RED at the chain's base {@code ddcdd151b} (as committed at run 1): {@code corpus_c1},
 * {@code corpus_c2}, {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on});
 * GREEN at the head 8/0F/1skip default, 8/0F/0skip {@code -Pir-on}.
 *
 * <p><b>Fixture provenance.</b> {@code E3Nested} is reduced from
 * {@code test-corpus/drr/drr-5.61.0/rosetta-source/src/main/rosetta/standards-iosco-cde-rule.rosetta}
 * lines 1699-1720 ({@code reporting rule CDENotional from Trade}): {@code extract ProductForTrade}
 * / {@code then extract product [ contractualProduct -> economicTerms then extract ... ]}, with the
 * ladder arms reduced to the ONE nav chain that carries the collision. {@code E3Idem} is reduced
 * from {@code regulation-asic-rewrite-trade-rule.rosetta} line 544
 * ({@code reporting rule NotionalQuantityLeg1 from TransactionReportInstruction}:
 * {@code then extract trade [ trade -> tradableProduct -> ... ]}) -- the already-escaped near miss.
 */
class ExplicitClosureParamNameBurnSeatTest {

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

    private static final String CDEN =
            "drr/standards/iosco/cde/reports/CDENotionalRule.java";
    private static final String CDENC =
            "drr/standards/iosco/cde/reports/CDENotionalCurrencyRule.java";
    /** The MEASURED green control: the only other block corpus-wide with this collision. */
    private static final String NQL1_ASIC =
            "drr/regulation/asic/rewrite/trade/reports/NotionalQuantityLeg1Rule.java";
    private static final String NQL1_CFTC =
            "drr/regulation/cftc/rewrite/reports/NotionalQuantityLeg1Rule.java";

    /** Cell A = drr 5.61.0 -- both carriers and both green controls. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixtures
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat32e3
            version "1.0.0"

            type Econ32:
                payout Payout32 (0..1)

            type Contract32:
                economicTerms Econ32 (0..1)

            type Product32:
                contractualProduct Contract32 (0..1)

            type Payout32:
                optionPayout Opt32 (0..*)
                amount number (0..1)

            type Opt32:
                underlier Product32 (0..1)

            type Trade32:
                held Product32 (0..1)

            type Lot32:
                qty number (0..1)

            type TradableProd32:
                lot Lot32 (0..1)

            type Trade32b:
                tradableProduct TradableProd32 (0..1)

            type Holder32:
                booked Trade32b (0..1)

            func ProductFor32:
                inputs:
                    trade Trade32 (1..1)
                output:
                    result Product32 (0..1)
                set result:
                    trade -> held

            func TradeFor32:
                inputs:
                    holder Holder32 (1..1)
                output:
                    result Trade32b (0..1)
                set result:
                    holder -> booked

            reporting rule E3Nested from Trade32: <"a1/a2 - a nav hop ONE LAMBDA DEEPER than an explicit closure param, whose type-derived var wants the param's own name">
                extract ProductFor32
                then extract product32 [
                    contractualProduct -> economicTerms
                        then extract
                            if payout -> optionPayout -> underlier -> contractualProduct -> economicTerms -> payout -> amount exists
                            then payout -> optionPayout only-element -> underlier -> contractualProduct -> economicTerms -> payout -> amount
                            else if payout -> amount exists
                            then payout -> amount
                ]

            reporting rule E3Idem from Holder32: <"e1 - the SAME collision one level shallower, already escaped by the #358/#360 AST pre-escape: the burn must leave it alone">
                extract TradeFor32
                then extract trade32b [
                    trade32b -> tradableProduct -> lot -> qty
                ]
            """;

    /**
     * a1 -- THE LAW. The deep hop ({@code underlier -> contractualProduct}, a gm-chained
     * {@code RFeatureCall} receiver whose previous step types {@code Product32}) wants the lambda
     * var {@code product32}, which is the ENCLOSING extract's explicit closure param. Post-law it
     * escapes, so the file carries the escaped form TWICE: once at the shallow hop (already correct
     * pre-law, the #362 AST pre-escape) and once at the deep hop (the heal).
     *
     * <p><b>PIN AT RED -- RESHAPED TWICE.</b> (1) {@code E3-red1.log}: the drafter's plain inner
     * {@code then extract} rendered the escape at both hops already at the base (the #358/#360 AST
     * pre-escape reaches a nav hop that sits directly in the nested lambda body); the reduction
     * gained the carrier's two-arm ladder. (2) <b>The seat-32 chain's own base RED and mutation
     * lanes then showed the laddered reduction was STILL a non-witness</b>: a1 measured GREEN at
     * the base {@code ddcdd151b} (every seat-32 law absent), GREEN under {@code m-e3} (the burn
     * severed) and GREEN under {@code m-e3-sentinel}; {@code E3-red2.log}'s failure was the draft's
     * {@code expected: <2> but was: <3>} against a render that already carried three
     * {@code _product32 -> _product32.} hops and ZERO bare collisions, and the 3 was then
     * transcribed from a GREEN print identical to the RED one. The pre-escape reaches a laddered
     * SINGLE-cardinality chain too. What the carrier has and the reduction lacked is the MULTI step
     * ahead of the colliding hop -- {@code payout -> optionPayout (0..*) -> underlier ->
     * contractualProduct} (standards-iosco-cde-rule.rosetta:1709-1710), collapsed by
     * {@code only-element} in the then arm -- past which the hop is minted one lambda deeper than
     * the pre-escape can see. The reduction now carries {@code Payout32.optionPayout Opt32 (0..*)}
     * and mirrors the carrier's first arm. MEASURED after the reshape (commit 18, standalone, then
     * reproduced by chain run 2): the src REVERTED ({@code apply32.py E3 --revert}) -- 8/4F/1S = a1 (on the bare-collision assert, {@code , product32 -> product32.} present in the deep hop), corpus_c1, corpus_c2, corpus_control1 ({@code e3w-red.log}); at the
     * head -- 8/0F/1skip, the count 3 = the shallow pre-escaped hop + the deep hop in the {@code exists} condition + the deep hop in the {@code only-element} then arm, every one escaped ({@code e3w-green0.log}); under {@code m-e3} and {@code m-e3-sentinel} -- 8/4F/1S each with a1 among the four ({@code e3w-mut-m-e3.log}, {@code e3w-mut-m-e3-sentinel.log}); every src/main restore left {@code git diff} empty. The equality is an exact
     * count so a lost or doubled hop shows; the bare-collision assert is the witness.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_deepHopUnderExplicitClosureParamEscapes() throws IOException {
        String out = fixtureRule("E3NestedRule");
        assertTrue(!out.contains(", product32 -> product32."),
                "the shadowing bare closure-param name must be gone:\n" + out);
        // MEASURED AFTER THE RESHAPE (print-first, commit 18): 3 escaped hops at the head - the
        // shallow (pre-escaped) hop plus the deep hop past the MULTI step, every one escaped; with
        // the src reverted the deep hop renders the BARE `product32 ->` and the assert above fires
        // first. The earlier "1 at the base / 3 after" reading was a misread of E3-red2.log (see
        // the javadoc).
        assertEquals(3, count(out, "_product32 -> _product32."),
                "the shallow (pre-escaped) hop and the deep (burned) hop in both ladder positions must"
                + " carry the escape; pre-law this is 1:\n" + out);
    }

    /**
     * a2 -- the escalation is SINGLE-level and the burn is a GROUP OF ONE. No mint's param name is
     * taken in its parent chain ({@code takenInParent=false} in all 1,794 measured explicit mints),
     * so the burn's own actual name is the param name itself and a descendant escapes exactly once
     * -- never {@code __product32}. And because the burn registers on a FRESH {@code lambdaScope()}
     * child that nothing else in the corpus registers the same desired name into, it never numbers
     * ({@code product320} / {@code product321}, the {@code registerNameBurn} javadoc's documented
     * grouping hazard). GREEN at RED by design; it is the {@code m-e3-scope} lane's witness -- a
     * burn on the PARENT scope puts the burn in the statement body group, where both directions can
     * break.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theEscalationIsSingleLevelAndUnnumbered() throws IOException {
        String out = fixtureRule("E3NestedRule");
        assertTrue(!out.contains("__product32"),
                "the burn must escalate exactly ONE level:\n" + out);
        assertTrue(!out.contains("product320") && !out.contains("product321"),
                "the burn must not group-and-number with anything in its scope:\n" + out);
    }

    /**
     * e1 -- the DECLINE LOCK, reduced from {@code NotionalQuantityLeg1}'s real source: the first
     * hop off the explicit closure param is already escaped by the #358/#360 pre-escape, so it
     * registers the desired name {@code _trade32b} and the {@code trade32b} burn does not touch it.
     * Bytes unchanged pre-law and post-law. This is the fixture face of the ONE measured green
     * block in the whole corpus; {@code corpus_c3} is its corpus face.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_alreadyEscapedFirstHopIsIdempotent() throws IOException {
        String out = fixtureRule("E3IdemRule");
        assertContains(out, "_trade32b -> _trade32b.getTradableProduct()");
        assertTrue(!out.contains("__trade32b"),
                "an already-escaped hop must NOT escalate again under the burn:\n" + out);
        assertTrue(!out.contains(", trade32b -> trade32b."),
                "the pre-escape must still fire (the burn does not replace it):\n" + out);
    }

    /** corpus_c1 -- the CDENotionalRule whole-file heal (drr 5.61.0 iosco cde). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr5610CDENotionalRuleMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(CDEN)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CDEN + ": " + own);
        String gen = drrAOutput.get(CDEN);
        assertNotNull(gen, "not generated: " + CDEN);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CDEN))), normalize(gen),
                "CDENotionalRule must byte-match golden - seat 32 law E3: the explicit closure"
                + " param is burned on its own lambda body scope, so the two hops one lambda"
                + " deeper escape it");
    }

    /** corpus_c2 -- the CDENotionalCurrencyRule whole-file heal (drr 5.61.0 iosco cde). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_drr5610CDENotionalCurrencyRuleMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(CDENC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CDENC + ": " + own);
        String gen = drrAOutput.get(CDENC);
        assertNotNull(gen, "not generated: " + CDENC);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CDENC))), normalize(gen),
                "CDENotionalCurrencyRule must byte-match golden - seat 32 law E3");
    }

    /**
     * corpus_c3 -- THE MEASURED GREEN CONTROL. {@code NotionalQuantityLeg1Rule} is the only block
     * in the corpus, besides the two carriers, whose explicit closure param name collides with a
     * nav-hop's type-derived var ({@code trade}, 30 IRLAM rows). Its golden already carries
     * {@code (trade -> } x1 and {@code _trade -> } x5 in BOTH cells, and it is green today, so the
     * burn must leave it byte-identical. The two shape asserts are here so the file cannot pass by
     * being empty or by losing the collision.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c3_drr5610NotionalQuantityLeg1RulesStayGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        for (String path : List.of(NQL1_ASIC, NQL1_CFTC)) {
            String golden = Files.readString(GOLDEN_A.resolve(path));
            assertEquals(1, count(golden, "(trade -> "),
                    "the green control must still carry the bare explicit param: " + path);
            assertEquals(5, count(golden, "_trade -> "),
                    "the green control must still carry the pre-escaped hops: " + path);
            String gen = drrAOutput.get(path);
            assertNotNull(gen, "not generated: " + path);
            assertEquals(normalize(golden), normalize(gen),
                    "the burn must be IDEMPOTENT on the already-escaped collision: " + path);
        }
    }

    /**
     * corpus_control1 -- LAW 79, the UNION whole-cell control over drr 5.61.0. Every file the fork
     * emits for the cell is compared to golden on a (T1, T2, T3) triple, file by file, and the
     * union domain is pinned:
     * <ul>
     *   <li><b>T1</b> -- {@code " -> "} occurrences: the STRUCTURE net. This law renames lambda
     *       params and can never add or remove a lambda, so T1 must be invariant everywhere; a
     *       crash-stubbed body (the seat-30/31 closed-scope class, which replaces the body with a
     *       rendered TODO stub naming the expression-compilation error) moves it.</li>
     *   <li><b>T2</b> -- {@code '", _'} occurrences: ESCAPED nav-hop lambda params. This is exactly
     *       what the law adds -- the carriers move 1 -> 3 and 2 -> 4 -- and it is what an OVER-fire
     *       moves anywhere else in the cell.</li>
     *   <li><b>T3</b> -- {@code '", __'} occurrences: DOUBLE-escaped params, the over-escalation
     *       net. MEASURED 0 in every one of the 5,249 drr 5.61.0 goldens, so any appearance is a
     *       defect.</li>
     * </ul>
     * All three run on {@code collapse} ONLY, deliberately NOT on the sibling seats'
     * {@code codeOnly}: {@code codeOnly} applied AFTER {@code collapse} swallows the rest of the
     * file at the generated {@code // RosettaFunction dependencies} header (the newline it scans
     * for is gone), which zeroes T1 on every rule and function file.
     *
     * <p><b>{@code GOLDEN_DOMAIN} is DERIVED, not drafted</b> -- by the read-only walk
     * {@code target/seat32-instruments/drafts32/E3/golden-domain-walk.py}, which reproduces this
     * scan outside the JVM over {@code test-corpus/drr/drr-5.61.0/rosetta-source/src/generated/java}
     * (5,249 golden files, 4,119 token-bearing, 1,772 with T2 &gt; 0, 0 with T3 &gt; 0). Asserting it
     * here pins the GOLDEN side independently of what the fork emits.
     *
     * <p>{@code DOMAIN_DRR561} (the emitted-side union) ships as the print-first SENTINEL
     * {@code -1}: it FAILS on the first green run and PRINTS its measured value, which the lead
     * transcribes in the same commit (the seat-30 sentinel discipline -- never draft a domain pin).
     * The drafter's estimate for that print is ~1,904 (golden files that carry the generated
     * function/rule marker AND are token-bearing).
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr561WholeCellEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_A));
        assertEquals(GOLDEN_DOMAIN, golden.size(),
                "the GOLDEN token-bearing domain is pinned from the read-only walk"
                + " (target/seat32-instruments/drafts32/E3/golden-domain-walk.py, which counted"
                + " 4119 of 5249 files with os.walk). This side of the scan cannot move with the"
                + " law - it reads goldens only - so a difference here is a WALK-SEMANTICS"
                + " difference (Files.walk vs os.walk over the corpus tree), not a defect:"
                + " transcribe the measured value and say so in the commit");
        assertTrue(drrAOutput.containsKey(NQL1_ASIC) && drrAOutput.containsKey(NQL1_CFTC),
                "the MEASURED green collision block must be INSIDE this scan's domain, else"
                + " control1 proves nothing about idempotence");
        assertUnionEqual(scan(drrAOutput), golden, drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN. This is THE POINT of the whole re-land: the
     * seat-31 leg fixed the legacy arm only and the ON route did not heal. The burn is at the
     * shared MINT, so the IR route -- which names its nav hops through
     * {@code IRJavaLeafEmitter.emitFieldAccess}'s {@code ctx.scope()
     * .registerDeferredLambdaParam(desired)} on a DESCENDANT of the burned scope -- must render the
     * carriers golden-identical too, and the green control must stay byte-identical there as well.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForCde() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CDEN))),
                normalize(irOut.get(CDEN)), "IR route vs GOLDEN: " + CDEN);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CDENC))),
                normalize(irOut.get(CDENC)), "IR route vs GOLDEN: " + CDENC);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(NQL1_ASIC))),
                normalize(irOut.get(NQL1_ASIC)), "IR route vs GOLDEN (green control): " + NQL1_ASIC);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(NQL1_CFTC))),
                normalize(irOut.get(NQL1_CFTC)), "IR route vs GOLDEN (green control): " + NQL1_CFTC);
    }

    // =========================================================================
    // The measured residue + domains (LAW 73: pin the SET, not only the count)
    // =========================================================================

    /**
     * MEASURED, not drafted: computed by {@code golden-domain-walk.py} over the seat-31 FINAL OFF
     * dump ({@code d31final-off/drr_5.61.0_{POJO,FUNCTION}}), which is complete for this purpose --
     * every drr 5.61.0 file OUTSIDE the band is byte-identical fork-to-golden, so only band files
     * can carry a triple difference. At the seat-31 head the walk printed EIGHT rows; the two CDE
     * rows ({@code fork=[63, 1, 0] golden=[63, 3, 0]} and {@code fork=[63, 2, 0] golden=[63, 4, 0]})
     * LEAVE the list when this law lands, and these SIX remain.
     *
     * <p>Every row is a LAW-81 tripwire owned by ANOTHER seat-32 law -- {@code EffectiveDateRule} is
     * javac32 C19 (scout F, F.1); the four cftc {@code Notional*Leg1/2Rule} files and jfsa
     * {@code NotionalLeg2Rule} are javac32 C22 (laws C.3 / B.3 / G.4). When one of those lands it
     * fires THIS control, and the re-pin is transcribed from that run's own print, in that law's
     * commit. If a seat-32 law lands BEFORE this one, this list is already stale at the law head:
     * re-pin from the print, do not re-derive from the dump.
     */
    ///PIN: TRANSCRIBED from this control's own print at the law head (E3-green2.log): of the draft's six rows
    ///PIN: (derived from the 88-band dumps) five were healed before this law landed - EffectiveDateRule by
    ///PIN: F.1, NotionalAmountLeg1/2 + NotionalCurrencyLeg1/2 by C.3 (NotionalCurrencyLeg1 to a tuple this
    ///PIN: scan reads as golden's) - and the two CDE rows are this law's own heal. ONE row remains.
    // LAW 81 re-pin (v3.1 flip seat 33, law F.B navTailExtractLadderNestedTreeAdmit): the jfsa NotionalLeg2Rule
    // row LEFT this list - the file is WHOLE in drr 5.61.0 at the F.B head; transcribed from this control's own
    // failing print (FB-trip1.log: "differ beyond the named residue" expected [NotionalLeg2Rule row] but was []).
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /**
     * The GOLDEN-side domain, DERIVED by {@code golden-domain-walk.py} at HEAD {@code ddcdd151b}:
     * of 5,249 {@code .java} goldens under {@code test-corpus/drr/drr-5.61.0/rosetta-source/src/
     * generated/java}, 4,119 carry at least one of the three tokens.
     */
    private static final int GOLDEN_DOMAIN = 4119;

    /** The print-first domain-pin sentinel (LAW 73) -- see corpus_control1's javadoc. */
    private static final int DOMAIN_DRR561 = 2171;   // MEASURED at E3-green1.log ('MEASURED DOMAIN = 2171 token-bearing files'), transcribed

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String flat = collapse(e.getValue());
            int t1 = count(flat, " -> ");
            int t2 = count(flat, "\", _");
            int t3 = count(flat, "\", __");
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
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

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        // The print-first domain-pin sentinel (seat 30): a -1 pin FAILS here and PRINTS its
        // measured value in the assert's own message - transcribe the pin FROM this print
        // (sentinel -> measured -> pin), never draft it.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means an"
                + " unpinned call site; MEASURED DOMAIN = " + universe.size()
                + " token-bearing files");
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

    // =========================================================================
    // Fixture harness (the InLambdaBoolHoistShadowSeatTest rule renderer, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat32e3.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat32e3".equals(m.namespace()));
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

    private static String fixtureRule(String ruleName) throws IOException {
        Render r = render();
        String path = ruleName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith(path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
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
            throw new AssertionError("[ExplicitClosureParamNameBurnSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
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

}
