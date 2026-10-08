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
 * SEAT 32, law E.2 — facet {@code aliasCallInputEscape} (census family F18): <b>an alias CALL
 * forwards the enclosing function's inputs by their EMITTED render names, so an input whose raw
 * name is {@code "_"}-escaped at the declaration seat is forwarded escaped too</b> — and the IR
 * route's bare {@code PARAM} read renders the same escaped name.
 *
 * <pre>
 * golden:  ... party1(counterparties, _partyLei).&lt;ReferenceWithMetaParty&gt;map(...)
 * fork:    ... party1(counterparties,  partyLei).&lt;ReferenceWithMetaParty&gt;map(...)      (OFF and ON)
 * golden:  if (areEqual(MapperS.of(_partyLei), ...
 * fork:    if (areEqual(MapperS.of( partyLei), ...                                       (ON only)
 * </pre>
 *
 * <p><b>The mechanism.</b> {@code drr CounterpartyRoleFromLEI} declares an input named
 * {@code partyLei} and calls {@code func PartyLei}, so upstream registers
 * {@code @Inject protected PartyLei partyLei} on the CLASS scope and the input's method-scope
 * allocation escapes to {@code _partyLei}. The fork already renders that escape everywhere the
 * declaration seat owns ({@code @param}, {@code evaluate}, {@code doEvaluate}, {@code assignOutput}
 * and BOTH alias method signatures) and at legacy's bare-read seat — three seats read the RAW name:
 * <ol>
 *   <li>{@code ReferenceHandler.renderEnclosingInputs} — the legacy alias-call argument list;</li>
 *   <li>{@code IRExpressionCompiler.aliasCallInputNames} — the IR TWIN of (1);</li>
 *   <li>{@code IRJavaLeafEmitter.emitVariable}'s {@code PARAM} arm — the ON-only bare read, now
 *       served by the {@code inputEscape} component of the per-emission {@code Resolvers} frame
 *       the compiler installs through {@code swapResolvers} (built by
 *       {@code IRExpressionCompiler.buildInputEscape}; the {@code setInputEscape} setter is the
 *       record's own mirror, like its eleven siblings) -- the emitter holds no {@code RFunction},
 *       so it cannot compute the escape itself.</li>
 * </ol>
 * All three now read the SAME table, {@code FunctionGenerator.escapedFunctionInputName} (facet
 * {@code fnInputDepCollisionEscape}, PR #436) — one escape table, three consumers (LAW 69). The
 * helper IS the discriminator: off-collision it returns the raw name unchanged, so the reach is
 * exactly the escape population and every other caller is byte-identical.
 *
 * <p><b>Charter:</b> {@code CounterpartyRoleFromLEI.java} × drr 7.0.0 / 7.1.0 / 7.2.0 / 7.3.0
 * FUNCTION — census class C016, sigs B051 B072, 4 diff lines / 2 hunks each, <b>4 WHOLE files</b>.
 * The whole-file heal needs all three seats in ONE commit: a legacy-only fix leaves the ON route
 * carrying two further tokens per file.
 *
 * <p><b>LAW 77 — this law IS the F18 route-twin closure.</b> {@code CounterpartyRoleFromLEI} is the
 * standing OFF/ON content divergence disclosed at #602/#603 (the 176-file route content-hash
 * compare reads 4). Seats 2 and 3 live in {@code rune-ir-java}, which the legacy handler fix does
 * NOT reach by inheritance. The both-routes checkpoint must show that compare drop <b>4 → 0</b>.
 *
 * <p><b>LAW 74 (MEASURED: {@code javac32-report.md} section 7.3 row C5, PRE 2 errors -> POST
 * 0).</b> The fork's current text does NOT compile on EITHER
 * route: {@code party1(List<? extends Counterparty>, String)} is called with {@code partyLei},
 * which in {@code assignOutput}'s scope binds the {@code @Inject protected PartyLei partyLei}
 * FIELD (the parameter is {@code _partyLei}) — a {@code PartyLei} argument where {@code String} is
 * declared: 2 errors × 4 files. The ON route's EXTRA defect is the opposite kind — it COMPILES
 * ({@code areEqual} takes {@code Mapper<T>, Mapper<U>} and {@code MapperS.of(Object)} accepts the
 * field) but is semantically wrong: a silent defect only the byte gate catches, never javac.
 *
 * <p><b>The CLAIMED sets</b> (drafted with {@code DOMAIN_DRR7} as a {@code -1} print-first
 * sentinel on both legs) were superseded by the measured ones below at the law's own landing
 * ({@code 82c4cf3ff} transcribed {@code DOMAIN_DRR7 = 491} from {@code E2-red1.log}) and by the
 * chain; the default-route RED at the base is {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1} (control1 failing on a real four-row residue compare, not a sentinel
 * print), and {@code -Pir-on} adds {@code a2}, {@code corpus_control2}, {@code corpus_control3}.
 * {@code e1} is GREEN at RED and at GREEN -- the decline lock, not a RED leg.
 *
 * <p><b>MUTATION LANES (LAW 66/76) — MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-seat*.log}; the two IR lanes ran under {@code -Pir-on}
 * with an ir-java reinstall before and after — three DISTINCT failing sets, MEASURED):</b>
 * <ul>
 *   <li><b>m-seat1</b> (seat 1 reverted: {@code joiner.add(input.name())}): MEASURED
 *       <b>8/4F/3S</b> (default route) = {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 *       {@code corpus_control1} — the claim exactly; {@code e1} GREEN.</li>
 *   <li><b>m-seat2</b> (seat 2 reverted: {@code .map(RAttribute::name)} in
 *       {@code aliasCallInputNames}): MEASURED <b>8/3F/0S</b> ({@code -Pir-on}) =
 *       {@code corpus_control2}, {@code corpus_control3} — the claim — plus {@code a2}, the
 *       IR-route fixture, which reads the same IREC seat; the default-route legs GREEN.</li>
 *   <li><b>m-seat3</b> (seat 3 reverted: the {@code inputEscape} consult at the {@code PARAM}
 *       arm): MEASURED <b>8/3F/0S</b> ({@code -Pir-on}) = {@code a2}, {@code corpus_control2},
 *       {@code corpus_control3} — the ON-only third defect's own lane, non-empty as
 *       guaranteed; the default-route legs GREEN.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b} (BOTH modules' src/main checked out): default route
 * {@code a1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (8/4F/3skip);
 * {@code -Pir-on} those four plus {@code a2}, {@code corpus_control2}, {@code corpus_control3}
 * (8/7F/0skip). GREEN at the head 8/0F/3skip default, 8/0F/0skip {@code -Pir-on}.
 */
class AliasCallInputEscapeSeatTest {

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

    /** The carrier — identical in all four cells (4 diff lines / 2 hunks each). */
    private static final String CRL =
            "drr/base/util/party/functions/CounterpartyRoleFromLEI.java";

    /** Cell A = drr 7.0.0 (the union-control cell). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.3.0 (the far end of the carrier's cell range). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellsAndIrProviderAvailable() {
        return cellAAvailable() && cellBAvailable() && irProviderOnClasspath();
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixture — reduced from the carrier's REAL source
    // =========================================================================

    /**
     * Reduced from {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
     * base-util-party-func.rosetta} — {@code func CounterpartyRoleFromLEI} at lines 87-100 (its
     * {@code partyLei} input, its {@code party1} alias and the {@code partyLei = PartyLei(...)}
     * comparison) plus {@code func PartyLei} at lines 10-18 (the dependency whose class simple
     * name lower-camels onto the input name). The reduction keeps every construct the mechanism
     * needs and nothing else: an input whose raw name collides with an {@code @Inject} function
     * dependency, an alias referenced from the body (so the alias CALL renders the enclosing
     * inputs), and a bare read of that same input at a comparison operand (so the
     * {@code MapperS.of(...)} wrap seat renders it too).
     *
     * <p>{@code RoleFromLeiNoCollision} is the near-miss twin: the SAME dependency, the SAME alias
     * shape, an input named {@code lei} that collides with nothing — the decline lock.
     */
    private static final String MODEL = """
            namespace census.seat32e2
            version "1.0.0"

            type Cpty:
                role string (0..1)
                lei string (0..1)

            func PartyLei:
                inputs:
                    cp Cpty (0..1)
                output:
                    partyLei string (0..1)
                set partyLei:
                    cp -> lei

            func CounterpartyRoleFromLei:
                inputs:
                    counterparties Cpty (0..*)
                    partyLei string (0..1)
                output:
                    reportingParty string (0..1)
                alias party1:
                    counterparties only-element
                set reportingParty:
                    if partyLei = PartyLei(party1)
                    then "P1"

            func RoleFromLeiNoCollision:
                inputs:
                    counterparties Cpty (0..*)
                    lei string (0..1)
                output:
                    reportingParty string (0..1)
                alias first1:
                    counterparties only-element
                set reportingParty:
                    if lei = PartyLei(first1)
                    then "P1"
            """;

    // =========================================================================
    // Part A — the law
    // =========================================================================

    /**
     * a1 — the alias CALL forwards the ESCAPED input name (default route). The declaration-seat
     * assert is the fixture's own witness: if {@code String _partyLei} is absent the fixture is
     * not reproducing the collision at all.
     *
     * <p>PIN AT RED: if the fixture does not render the escaped signature, RESHAPE the fixture —
     * do not weaken the assert.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_aliasCallForwardsTheEscapedInputName() throws IOException {
        String out = fixtureFunction("CounterpartyRoleFromLei");
        assertContains(out, "String _partyLei");
        assertContains(out, "party1(counterparties, _partyLei)");
        assertNotContains(out, "party1(counterparties, partyLei)");
    }

    /**
     * a2 — LAW 77: the SAME fixture through the REAL {@code IRGeneration.functionGenerator} seam
     * ({@code -Pir-on}). Asserts BOTH IR seats: the alias-call argument list (seat 2, the twin of
     * seat 1) and the bare {@code PARAM} read's {@code MapperS.of} wrap (seat 3, the ON-only
     * defect).
     *
     * <p><b>Honesty note on the RED claim.</b> This leg is RED at the base head only if the IR
     * route CLAIMS the fixture's alias reference and its comparison operand; if it declines both
     * to legacy the fixture renders correctly at RED and a2 is vacuously green. The load-bearing
     * ON-route RED legs are {@code corpus_control2} / {@code corpus_control3}, which run the real
     * carriers (measured wrong on the ON route at the seat-31 final dump). a2 is kept because a
     * fixture-grain ON assert is the only cheap lane the mutation runs can move.
     *
     * <p>PIN AT RED: if the fixture does not produce a {@code MapperS.of(<input>)} bare-read wrap,
     * RESHAPE the fixture — do not weaken the assert.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a2_theIrRouteForwardsAndWrapsTheEscapedInputName() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "CounterpartyRoleFromLei.java");
        assertContains(out, "party1(counterparties, _partyLei)");
        assertNotContains(out, "party1(counterparties, partyLei)");
        assertContains(out, "MapperS.of(_partyLei)");
        assertNotContains(out, "MapperS.of(partyLei)");
    }

    /**
     * e1 — the decline lock. An input whose raw name collides with NOTHING (the near-miss
     * {@code lei} against the {@code partyLei} dependency field — the #194 near-miss law) keeps
     * the raw name in the alias call, on both the declaration and the call seat. This is the
     * direction that proves the escape helper, not a name rewrite, is the discriminator: bytes
     * unchanged for the whole non-escaping population.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_aNonCollidingInputKeepsTheRawNameInTheAliasCall() throws IOException {
        String out = fixtureFunction("RoleFromLeiNoCollision");
        assertContains(out, "String lei");
        assertContains(out, "first1(counterparties, lei)");
        assertNotContains(out, "first1(counterparties, _lei)");
        assertNotContains(out, "String _lei");
    }

    // =========================================================================
    // Corpus — the whole-file heals
    // =========================================================================

    /** corpus_c1 — the drr 7.0.0 whole-file heal (both hunks are this law; the file goes WHOLE). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700CounterpartyRoleFromLeiMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(CRL)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CRL + ": " + own);
        String gen = drrAOutput.get(CRL);
        assertNotNull(gen, "not generated: " + CRL);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CRL))), normalize(gen),
                CRL + " (drr 7.0.0) must be byte-identical to golden — seat 32 law E.2: the alias"
                        + " call forwards the escaped input name");
    }

    /** corpus_c2 — the drr 7.3.0 whole-file heal (the far end of the carrier's cell range). */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730CounterpartyRoleFromLeiMatchesGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        List<String> own = drrBGenErrors.stream().filter(e -> e.contains(CRL)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CRL + ": " + own);
        String gen = drrBOutput.get(CRL);
        assertNotNull(gen, "not generated: " + CRL);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(CRL))), normalize(gen),
                CRL + " (drr 7.3.0) must be byte-identical to golden — seat 32 law E.2");
    }

    /**
     * corpus_control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0 (default route). Domain =
     * every EMITTED file whose GOLDEN <b>or</b> FORK text carries a non-zero escape tuple; per
     * file the (T1, T2, T3, T4) tuple must equal golden's beyond the NAMED residue. Union, never
     * intersection — an intersection scan is blind in both directions.
     *
     * <p>The tuple is deliberately GLOBAL (see {@link #scan}): it moves if the escape over-fires
     * (a green file gains an escaped mention) AND if it under-fires (the carrier keeps its raw
     * name), anywhere in the cell.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellEscapeSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /**
     * corpus_control2 — <b>LAW 77, per FILE, on BOTH routes: the F18 closure.</b> This leg is the
     * whole point of the law, not a formality. {@code CounterpartyRoleFromLEI} is the standing
     * OFF/ON content divergence (#602 F18, the 176-file route content-hash compare = 4): the ON
     * route carries TWO FURTHER wrong tokens per file that no count digest can see. So all three
     * legs are asserted, in two cells:
     * <ol>
     *   <li>the LEGACY route equals GOLDEN byte-for-byte;</li>
     *   <li>the IR route equals GOLDEN byte-for-byte — the leg that needs seats 2 AND 3, and the
     *       reason this is written against golden rather than against the other route;</li>
     *   <li>the two routes agree (implied by 1 and 2, kept so a future failure names the axis).</li>
     * </ol>
     *
     * <p>⚠ {@code @EnabledIf}-skipped unless the IR provider is on the classpath, so it pins
     * nothing on a default suite run — <b>the {@code -Pir-on} leg of the chain is what makes it
     * load-bearing.</b>
     */
    @Test
    @EnabledIf("cellsAndIrProviderAvailable")
    void corpus_control2_bothRoutesEqualGoldenForTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        assertNotNull(drrBOutput, "drr 7.3.0 legacy-route generation did not run");
        String goldenA = normalize(Files.readString(GOLDEN_A.resolve(CRL)));
        String goldenB = normalize(Files.readString(GOLDEN_B.resolve(CRL)));
        String irA = irCellA().get(CRL);
        Map<String, String> irB = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT), new ArrayList<>());
        String irBGen = irB.get(CRL);
        assertNotNull(irA, "the IR route did not generate (drr 7.0.0): " + CRL);
        assertNotNull(irBGen, "the IR route did not generate (drr 7.3.0): " + CRL);
        assertEquals(goldenA, normalize(drrAOutput.get(CRL)),
                "the LEGACY route differs from golden (drr 7.0.0): " + CRL);
        assertEquals(goldenA, normalize(irA),
                "the IR route differs from golden (drr 7.0.0) — the LAW-77 leg: seats 2 and 3 live"
                        + " in rune-ir-java and are NOT reached by the handler fix: " + CRL);
        assertEquals(goldenB, normalize(drrBOutput.get(CRL)),
                "the LEGACY route differs from golden (drr 7.3.0): " + CRL);
        assertEquals(goldenB, normalize(irBGen),
                "the IR route differs from golden (drr 7.3.0) — the LAW-77 leg: " + CRL);
        assertEquals(normalize(drrAOutput.get(CRL)), normalize(irA),
                "route divergence (drr 7.0.0) — the standing F18 disclosure must close: " + CRL);
        assertEquals(normalize(drrBOutput.get(CRL)), normalize(irBGen),
                "route divergence (drr 7.3.0) — the standing F18 disclosure must close: " + CRL);
    }

    /**
     * corpus_control3 — the SAME whole-cell UNION scan, on the IR ROUTE (the whole-cell control
     * the IR-side mutation lanes need: a two-file byte compare is not a whole-cell instrument, and
     * seats 2 and 3 are IR-route changes). Same domain, same residue — the band lists are
     * file-for-file identical on both routes at this head, and outside the band the ON route
     * equals golden.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control3_irRouteDrr700WholeCellEscapeSitesEqualGoldenFileByFile() throws IOException {
        Map<String, String> irOut = irCellA();
        // the residue list is SHARED with corpus_control1 and its rows read "fork=..."; the IR
        // route must reproduce the SAME rows (E2-on1.log printed them identical under the label
        // "ir-fork="), so the label is the legacy one - route identity is the assertion.
        assertUnionEqual(scan(irOut), scan(readGoldenTree(GOLDEN_A)), irOut.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    // =========================================================================
    // The union instrument (LAW 73: pin the SET, not the count)
    // =========================================================================

    ///PIN: PRINT-FIRST SENTINEL (LAW 73). -1 makes assertUnionEqual FAIL and PRINT
    ///PIN:   "MEASURED domain=<n> residue=[...]"
    ///PIN: at the seat-32 chain head; transcribe BOTH from that print. It cannot be derived
    ///PIN: offline: the universe is the GOLDEN-or-FORK token-bearing files INTERSECTED with the
    ///PIN: cell's own emission key set, and the emission set is only known once the generator has
    ///PIN: run. Derived scale (read-only walk of test-corpus/drr/drr-7.0.0/rosetta-source/src/
    ///PIN: generated/java, 2026-08-27): 7,808 goldens, 2,873 carry a non-zero tuple, 491 of those
    ///PIN: under /functions/ or /reports/ — so expect a domain in the high hundreds.
    private static final int DOMAIN_DRR7 = 491;   // MEASURED at E2-red1.log (MEASURED domain=491), transcribed

    ///PIN: MEASURED at the SEAT-31 head (c7fbe358e) from the final OFF dump of the
    ///PIN: drr_7.0.0_FUNCTION cell: outside the 13-file band the fork is byte-identical to golden,
    ///PIN: so the residue is a strict subset of that band, computed file-for-file. FIVE band files
    ///PIN: differ on this tuple; CounterpartyRoleFromLEI (fork=[2, 2, 11, 2] golden=[4, 2, 13, 2])
    ///PIN: LEAVES the list when this law lands, and the four below are OTHER-family residue
    ///PIN: (thenArg/ifThenElseResult shapes, the ctor deref hoists, the CDE price family).
    ///PIN: VERIFIED at the seat-32 chain head (both runs, control1 GREEN): none of the four moved --
    ///PIN: GetBasketConstituents, Price, TotalNotionalQuantity and Enrich are S33's, not this seat's
    ///PIN: (charter32.md section BANKED); the in-seat LAW-81 watch this note carried is discharged.
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // the GetBasketConstituents row (fork=[2, 0, 4, 14] golden=[1, 0, 2, 14]) LEFT this list at seat 33:
            // law A.2 (aliasCondLadderChoiceJoin) renamed the alias-rooted hop's lambda from the escaped symbol
            // (`_underliers`) to the ladder's folded choice (`underlier`), healing the file WHOLE in all four
            // drr 7.x cells - the tuple was B016's alone (tokens ", _" / "MapperS.of(_" / " _" / "(_");
            // transcribed from this control's own print (A2-trip1.log), a pure row removal (was == expected minus it).
            // the Enrich_TransactionReportInstructionTestPackDefault row (fork=[6, 0, 12, 0] golden=[6, 0, 15, 6]) LEFT this list at seat 33:
            // law E.234 (rungs E.2 + E.3 + E.4) healed the file WHOLE in all four drr 7.x cells
            // (EnrichReportInstructionWholeSeatTest corpus_c1/c2); transcribed from the control print (E234-trip1.log).
            // the Price row (fork=[21, 0, 42, 0] golden=[22, 0, 44, 0]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // the TotalNotionalQuantity row (fork=[1, 0, 6, 9] golden=[3, 1, 11, 7]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /**
     * (T1, T2, T3, T4) per file — LITERAL occurrence counts (no regex on generated Java; the
     * house {@code contains}-style idiom):
     * <ul>
     *   <li><b>T1</b> {@code ", _"} — the law's ADDED shape: an escaped identifier in a non-first
     *       ARGUMENT position. A parameter DECLARATION never matches (a type sits between the
     *       comma and the name), so this counts argument lists, which is exactly what seats 1
     *       and 2 move: the carrier goes 2 → 4.</li>
     *   <li><b>T2</b> {@code "MapperS.of(_"} — seat 3's ADDED shape: the bare-read wrap of an
     *       escaped input. The carrier goes 0 → 2 on the ON route (already 2 on the legacy
     *       route, which serves this half at {@code ReferenceHandler}'s variable path).</li>
     *   <li><b>T3</b> {@code " _"} — the OVER-FIRE NET: every escaped mention in the file,
     *       javadoc and signatures included. Deliberately global: the escape population must not
     *       move by one occurrence anywhere in the cell.</li>
     *   <li><b>T4</b> {@code "(_"} — the second half of that net (a first-argument /
     *       parenthesised escaped read, which T3 cannot see: no preceding space).</li>
     * </ul>
     * A file with an all-zero tuple in BOTH trees stays out of the domain.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = normalize(e.getValue());
            int[] t = new int[] {
                    count(code, ", _"),
                    count(code, "MapperS.of(_"),
                    count(code, " _"),
                    count(code, "(_")};
            if (t[0] + t[1] + t[2] + t[3] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    /** Literal, non-overlapping occurrence count (structured content — never a regex). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
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
        int[] zero = new int[4];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values so the pin
        // is transcribed from this assert's own output, never invented.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3, T4) differ beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
    }

    // =========================================================================
    // Corpus harness (the seat-30/31 suite shape, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> irAOutput;

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

    /** The drr 7.0.0 cell on the IR route, generated once and shared by control2 + control3. */
    private static Map<String, String> irCellA() throws IOException {
        if (irAOutput == null) {
            irAOutput = generateCellOnIrRoute(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                    new ArrayList<>());
        }
        return irAOutput;
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

    // =========================================================================
    // Fixture harness (the CtorRuleValueCardinalitySeatTest link/render pattern)
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;
    private static Map<String, String> fixtureOutIr;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat32e2.rosetta");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
            mainModel = main;
        }
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            link();
            GeneratorModel gm = new GeneratorModel(linking.workspace(),
                    m -> "census.seat32e2".equals(m.namespace()));
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
            Map<String, String> out = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            fg.generateWithErrors(out)
                    .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
            if (!errors.isEmpty()) {
                throw new AssertionError("fixture generation errors (a broken fixture must fail"
                        + " loudly, not skip): " + errors);
            }
            fixtureOut = out;
        }
        return fixtureOut;
    }

    /** The fixture through the REAL {@code IRGeneration.functionGenerator} seam (the ON route). */
    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureOutIr == null) {
            link();
            String previous = System.getProperty(IRGeneration.PROPERTY);
            System.setProperty(IRGeneration.PROPERTY, "true");
            try {
                assertNotNull(IRGeneration.providerOrNull(),
                        "the IR provider must be resolvable under -Pir-on, else this is not an"
                                + " ON-route render");
                GeneratorModel gm = new GeneratorModel(linking.workspace(),
                        m -> "census.seat32e2".equals(m.namespace()));
                JavaTypeUtil typeUtil = new JavaTypeUtil();
                JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
                FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
                assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                        "the seam must hand back the IR-route FunctionGenerator, got "
                                + fg.getClass());
                Map<String, String> out = new LinkedHashMap<>();
                List<String> errors = new ArrayList<>();
                fg.generateWithErrors(out)
                        .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
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

    private static String fixtureFunction(String fnName) throws IOException {
        return lookup(fixture(), fnName + ".java");
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
            throw new AssertionError("[AliasCallInputEscapeSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertFalse(out.contains(token), "forbidden token present:\n  " + token + "\nin:\n" + out);
    }
}
