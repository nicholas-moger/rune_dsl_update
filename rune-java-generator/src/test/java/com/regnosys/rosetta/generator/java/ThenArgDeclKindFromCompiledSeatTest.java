// SEAT 30 LAW 5 — facet thenArgDeclKindFromCompiled (apply script: drafts30/law5s-apply.py).
// SHIPPED.
// Every PIN marked  ///PIN:  carries its MEASURED value, transcribed from the chain's own
// print. The chain that measured them: chain-all30.ps1 @ e223ce19; logs
// f30-{red,green}-{default,on}.log and f30-mut-m-law5s{,-nolol,-anykind}.log.
// READ THE CLASS JAVADOC'S "FAILING-FIRST GAP" PARAGRAPH BEFORE TRUSTING a1/a2/a3.
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
 * SEAT 30, law 5 — facet {@code thenArgDeclKindFromCompiled}: <b>the then-level declaration
 * must agree with its own value, on the CARDINALITY axis.</b>
 *
 * <p><b>The two halves that disagreed (LAW 69).</b> In
 * {@code FunctionExpressionRenderer.renderThenExtractSetImpl}, {@code single} is fixed at the
 * top of each level from the AST alone ({@code CardinalityComputer} +
 * {@code NavigationHandler.chainProvesMulti}) and is never reconciled with the value about to
 * be assigned to the local. The ELEMENT axis of the same declaration is reconciled with render
 * truth EIGHT rungs over (#204 / #391 / #238 / #144 / #338 / #297 / #392 + seat 14's
 * {@code thenExtractFnCalleeMetaElement}); the KIND axis was reconciled zero ways. This law is
 * seat 14's law — "the then-level decl must agree with its own value's type" — carried to the
 * axis it had not reached.
 *
 * <p><b>The probe verdict this law answers (LAW 75).</b> {@code verdicts30-recharter.md} §3
 * REFUTED the drafted {@code metaStamp} siting and NAMED this seat. Both rows ×4 cells,
 * present and identical on BOTH routes:
 * <pre>
 * x4 [PROBE30-F16w] seat=metaStamp where=fn:GetUnderlierProductIdentifierLeg1
 *    mapMethod=map attr=Observable metaKind=REFERENCE_WITH_META
 *    chainMapperC=true chainRendersMapperC=false boundPipeC=true resultWrap=C
 *    recvKind=RImplicitVariable rootKind=RImplicitVariable recvStamp=C rootWrap=C
 *
 * x4 site=extract where=fn:GetUnderlierProductIdentifierLeg1
 *    src=regulation-common-func.rosetta:2244:18
 *    chosen=mapSingleToList recvKind=RImplicitVariable rootKind=RImplicitVariable
 *    recvStamp=S rootWrap=S boundT=S bodyMulti=true astMulti=false chainProves=false
 * </pre>
 * {@code metaNavResultType} STAMPS the {@code then Observable} hop {@code resultWrap=C} — the
 * golden type; the row is unique in the corpus (of 1,092 {@code attr=Observable} metaStamp rows
 * it is the only one with {@code boundPipeC=true}). The DECL then re-derived SINGLE and emitted
 * {@code MapperS}; the extract one hop later read {@code boundT=S} off that decl and picked
 * {@code mapSingleToList}. <b>The stamp and the declaration contradict each other on the same
 * value.</b> The three healthy sibling extract rows in the same function
 * ({@code :4574:18 recvStamp=C chosen=mapItemToList}, {@code :4583:18 recvStamp=LoL
 * chosen=mapListToItem}, {@code :4609:18 recvStamp=C chosen=mapItem}) are the IN-FILE negative
 * controls: they already read the right binding, and they must not move.
 *
 * <p><b>THREE hunks, ONE read.</b> golden drr
 * {@code drr/regulation/common/functions/GetUnderlierProductIdentifierLeg1.java}:
 * <pre>
 * golden  import com.rosetta.model.lib.mapper.MapperListOfLists;            &lt;- hunk C
 *         final MapperC&lt;ReferenceWithMetaObservable&gt; thenArg1 = …;          &lt;- hunk A
 *         final MapperListOfLists&lt;AssetIdentifier&gt; thenArg2 = thenArg1
 *             .mapItemToList(item -&gt; {                                      &lt;- hunk B
 * fork    (no MapperListOfLists import)
 *         final MapperS&lt;ReferenceWithMetaObservable&gt; thenArg1 = …;
 *         final MapperC&lt;AssetIdentifier&gt; thenArg2 = thenArg1
 *             .mapSingleToList(item -&gt; {
 * </pre>
 * Hunk A is the rung itself. Hunks B and C follow through {@code prevRef}: the decl re-binds
 * {@code prevRef = JavaExpression.from(name, thenArgType)}, so the NEXT level's
 * {@code CollectionHandler.producesListOfLists} sees a MapperC receiver with a MULTI body,
 * {@code listOfLists} becomes true (→ hunk C's {@code refs.add(MAPPER_LIST_OF_LISTS)} and the
 * {@code MapperListOfLists} decl) and {@code CollectionHandler.mapMethod} renders
 * {@code .mapItemToList(} (hunk B). The two halves cannot disagree — {@code producesListOfLists}
 * IS {@code mapMethod}'s own SOT (the #274 two-halves law).
 *
 * <p><b>THE LAW-80 STOP, in the recharter verdict's own words:</b> <i>"If only the decl moves
 * and the extract does not flip, the file heals ZERO."</i> {@code a2} and {@code a3} are that
 * stop's pins, and {@code corpus_c1} is a byte-WHOLE lock — do not weaken it.
 *
 * <p><b>LAW 74.</b> The carrier does NOT compile at this head, by reading:
 * {@code MapperC.map(…)} returns {@code MapperC<R>} and the fork assigns it to a
 * {@code MapperS<ReferenceWithMetaObservable>} local — {@code incompatible types}. This is the
 * seat-29 {@code f16f29} T2 defect ("MapperS&lt;ReferenceWithMetaObservable&gt; thenArg1 off a
 * MapperC&lt;Underlier&gt; receiver CANNOT COMPILE"), and this law heals it. <b>The
 * {@code javac30} PRE census MUST hold {@code GetUnderlierProductIdentifierLeg1} ×4 (drr
 * 7.0/7.1/7.2/7.3); POST must be 0 for those four files.</b>
 *
 * <p><b>Carriers.</b> {@code GetUnderlierProductIdentifierLeg1.java}, drr 7.0.0–7.3.0,
 * band29 family <b>F11</b>, {@code lines=7 hunks=2} (+ the import). All three hunks are this
 * law ⇒ <b>4 of 4 WHOLE</b>.
 *
 * <p><b>Green safety.</b> A {@code final MapperS<X> t = <MapperC<X> expression>;} assignment
 * never compiled, so no green file can carry the pre-fix form. The rung is sited AFTER the two
 * other {@code single} consumers in the method (the #260 {@code MapperS.of} re-wrap and the
 * #360 {@code ruleRefWrapThenArg} arm), which therefore both read the ORIGINAL {@code single}
 * and are byte-neutral by construction — {@code b3} is that pin. It is also sited AFTER
 * {@code listOfLists}, so the third cardinality state (which owns its own wrapper) declines
 * rather than being overridden — {@code b2} is that pin.
 *
 * <p><b>RED at the pre-law head — MEASURED at {@code 6c8e1544}, BOTH routes</b>
 * ({@code f30-red-default.log}, {@code f30-red-on.log}): <b>11 run / 2F / 0E / 1 skip</b>
 * (default) and <b>11 / 2F / 0E / 0 skip</b> ({@code -Pir-on}) — the SAME two both routes:
 * <b>corpus_c1</b> (the byte-whole lock on {@code GetUnderlierProductIdentifierLeg1.java}) and
 * <b>corpus_control1</b> (printing {@code in 7 file(s)} against its pinned 4). <b>GREEN at the
 * CURRENT head is 11/0F on both routes</b> — measured by chain run 2 at {@code 71a92e826}. At
 * the run-1 head {@code e223ce19} it was 11/1F, the one failure being {@code corpus_control1} at
 * {@code 4 file(s)} — the LAW-81 tripwire the seat's own laws 3 and 4 fired, re-pinned below at
 * {@code 80bbe57b}, and that re-pin is what took the suite to 0F.
 *
 * <p><b>⚠ THE FAILING-FIRST GAP, MEASURED AND NOT PAPERED OVER (LAW 66).</b> The draft claimed
 * RED = {@code a1, a2, a3, corpus_c1, corpus_control2}. <b>Three of those five are wrong, and
 * one member it never named is right:</b>
 * <ul>
 *   <li><b>{@code a1}, {@code a2}, {@code a3} PASS at the RED base</b> — and they pass under
 *       {@code m-law5s}, the WHOLE-law revert, too. <b>The three unit fixtures do not lock this
 *       law.</b> Whatever their fixture renders, it renders the same way with the rung present
 *       and with the rung reverted, so their green is not evidence the law works; it is only
 *       evidence the fixture never reaches the rung. They are post-law mechanism pins — they
 *       document and freeze the intended three-hunk shape — and must be read as such until
 *       someone re-cuts them.</li>
 *   <li><b>{@code corpus_control2} does NOT fail at RED even on {@code -Pir-on}</b>, where it
 *       actually runs (RED-on is 2F, not 3F). Naming it in the claim was a guess.</li>
 *   <li><b>{@code corpus_control1} DOES fail at RED and was never claimed.</b></li>
 * </ul>
 * <b>So the law's real failing-first evidence is exactly two tests: {@code corpus_c1} and
 * {@code corpus_control1}</b> — a byte-whole carrier lock plus a whole-cell control. That is
 * genuine failing-first evidence at CORPUS grain, and it is what the LAW-74 javac census
 * corroborates ({@code GetUnderlierProductIdentifierLeg1} ×4 in PRE, 0 in POST). It is NOT
 * fixture-grain evidence. <b>The falsifier that would close the gap:</b> re-cut a1/a2/a3 (or add
 * a fourth fixture) whose PRE-law render actually carries the {@code MapperS} decl off a
 * {@code MapperC} receiver — the carrier's own shape — and confirm it RED at
 * {@code 6c8e1544} before trusting it. Do not weaken {@code corpus_c1} to compensate.
 *
 * <p><b>MUTATION LANES — MEASURED (LAW 82). Every set transcribed from its archived logs
 * {@code f30-mut-&lt;lane&gt;.log} of BOTH chain runs — run 1 at {@code e223ce19} and run 2 at the
 * re-pin head {@code 71a92e826}. The RAW counts quoted below are run 2's, with run 1's in
 * parentheses where they differ (by exactly the suite's then-standing failure, retired at
 * {@code 80bbe57b}); every NET set is identical across the two runs. Corrections NAMED in
 * place.</b>
 * <b>⚠ THE LANE LOOP RUNS THE DEFAULT PROFILE ONLY.</b> {@code corpus_control2} is
 * {@code @EnabledIf}-gated on the IR route and is the ONE skip in every lane's
 * {@code …/1 skipped} run, so it CANNOT fail in a lane by construction; it is carried below as
 * an UNMEASURED claim only. <b>Reading rule, HISTORICAL:</b> at the run-1 head these logs come
 * from, this suite CARRIED a standing GREEN failure — {@code corpus_control1} at
 * {@code 4 file(s)} — so a lane was scored by whether it MOVED that residue count, not merely by
 * whether the test appeared in the log. <b>The {@code 80bbe57b} re-pin retired that standing
 * failure</b>: at the run-2 head ({@code 71a92e826}) the suite is 11/0F and each lane's raw
 * measured set equals the net set stated below, with nothing to net against.
 * <ul>
 *   <li><b>m-law5s</b> ({@code f30-mut-m-law5s.log}) = {@code law5s-apply.py --revert}.
 *       <b>MEASURED 11/2F/0E/1S: corpus_c1 and corpus_control1 — and control1 MOVED, 4 → 5
 *       file(s).</b> <b>Drafted a1, a2, a3, corpus_c1 (+control2 on {@code -Pir-on}); measured
 *       corpus_c1 + corpus_control1 — the difference explained:</b> a1/a2/a3 are green under the
 *       whole-law revert (the failing-first gap above — this lane is where it was caught, not
 *       inferred), and {@code corpus_control1} was under-named: reverting the law puts
 *       {@code GetUnderlierProductIdentifierLeg1.java} back into the differing set, which is the
 *       fifth file. <b>The lane reproduces the RED set exactly</b> (corpus_c1 + control1), which
 *       is the strongest thing it can say: law 5 alone accounts for the whole of this suite's
 *       RED movement, no other seat-30 law contributes to it.</li>
 *   <li><b>m-law5s-nolol</b> ({@code f30-mut-m-law5s-nolol.log}) = {@code --mut-nolol} (the
 *       {@code !listOfLists} conjunct escaped). <b>MEASURED at the run-2 head {@code 71a92e826}:
 *       11/0F/0E/1S — an unambiguous, contamination-free EMPTY set.</b> (Run 1 at
 *       {@code e223ce19} measured 11/<b>1</b>F/0E/1S, the one member being the then-standing
 *       {@code corpus_control1} at {@code 4 file(s)}, IDENTICAL to GREEN → the same net EMPTY,
 *       inferred then and now measured directly.)
 *       <b>Drafted b2 + corpus_control1; measured EMPTY — the difference explained:</b> b2 is
 *       green with the conjunct escaped and the whole-cell control does not move by one file, so
 *       there is <b>no observable movement at all</b> at 25 cells or in the fixture set.
 *       <b>EMPTY is the ADJUDICATED value, per the seat-29 {@code m-law5ii} precedent.</b> What
 *       it means: the {@code !listOfLists} conjunct has ZERO carriers here — no
 *       MapperListOfLists level whose compiled value is a MapperC exists in this corpus. It ships
 *       as a strictly-narrowing guard on the shape (the third cardinality state owns its own
 *       wrapper and must decline), honestly scored as un-witnessed rather than load-bearing.</li>
 *   <li><b>m-law5s-anykind</b> ({@code f30-mut-m-law5s-anykind.log}) = {@code --mut-anykind}
 *       ({@code isMapperC} widened to "not MapperS"). <b>MEASURED at the run-2 head
 *       {@code 71a92e826}: 11/0F/0E/1S — an unambiguous, contamination-free EMPTY set.</b>
 *       (Run 1 measured 11/<b>1</b>F/0E/1S, the one member being the then-standing
 *       {@code corpus_control1} at {@code 4 file(s)}, IDENTICAL to GREEN → the same net EMPTY.)
 *       <b>Drafted b1, b2, corpus_control1; measured EMPTY — the difference explained:</b> same
 *       reading as {@code -nolol}. The lane was built to prove that the {@code MapperC}
 *       REFINEMENT — not merely "the compiled kind" — is what the law reads; <b>it did not prove
 *       that, and the honest statement is that no row in this corpus separates the two
 *       readings.</b> The refinement stands on the shape argument alone. <b>The falsifier:</b> a
 *       level whose compiled value is neither {@code MapperS} nor {@code MapperC} (a
 *       {@code MapperListOfLists} or a bare {@code Mapper}) reaching this decl seat — none
 *       exists at 25 cells, so it needs a FIXTURE, and building it is the way to give b1/b2 a
 *       measured witness.</li>
 * </ul>
 * <b>Lane tally for this suite: 0 MATCH · 1 MISMATCH-corrected · 2 MEASURED-EMPTY against
 * non-empty claims.</b> None of the three lanes moves a unit fixture; every scrap of measured
 * movement in this suite is at corpus grain.
 */
class ThenArgDeclKindFromCompiledSeatTest {

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

    /** Cell A = drr 7.0.0 — the carrier cell (7.1/7.2/7.3 carry the identical rows). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = drr 5.61.0 — a NON-carrier cell, dense in then-chains and thenArg decls. The
     * measured {@code boundPipeC=true} row set is drr 7.x-only, so this cell's whole-cell
     * decl-kind triple must be flat against golden.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
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
    // The fixture — the corpus chain minimised, plus the three decline twins
    // =========================================================================

    /**
     * The corpus shape ({@code regulation-common-func.rosetta:2242-2253}):
     * {@code … -> underlier then Observable then extract [ … ] then flatten}, where the base
     * resolves to a MULTI conditional (so {@code thenArg0} is a {@code MapperC}), the
     * {@code then <metaAttr>} hop is stamped {@code MapperC} by {@code metaNavResultType}
     * off the bound MapperC pipe, and the AST cardinality computer reads the hop SINGLE.
     *
     * <p><b>The decline twins.</b>
     * <ul>
     *   <li>{@code B1SingleValuedThen} — the same chain whose {@code thenArg1} value genuinely
     *       compiles {@code MapperS}: the decl must STAY {@code MapperS} and the next level
     *       must stay {@code mapSingleToList}. This is the pin that says the law reads the
     *       COMPILED kind and not "always MapperC".</li>
     *   <li>{@code B2ListOfListsThen} — a level whose {@code listOfLists} is true: the third
     *       cardinality state wins and the rung declines.</li>
     *   <li>{@code B3CollapsedGetThen} — the #260 {@code thenArgMapperSWrap} shape (a
     *       collapse step whose value ends {@code .get()}): the rung is sited AFTER that arm,
     *       so its {@code MapperS.of(…)} re-wrap and its {@code MapperS} decl must both
     *       survive byte-identically.</li>
     * </ul>
     */
    private static final String MODEL = """
            namespace census.seat30law5
            version "1.0.0"

            type Ident:
                code string (0..1)

            type Obs:
                idents Ident (0..*)
                name string (0..1)

            type Under:
                obs Obs (0..1)
                    [metadata reference]

            type Leg:
                unders Under (0..*)
                one Under (0..1)

            type Root:
                legs Leg (0..*)
                flag boolean (0..1)

            func A1ThenArgKindFromCompiled: <"a1/a2/a3 - THE CARRIER shape">
                inputs:
                    r Root (1..1)
                output:
                    out Ident (0..*)
                add out:
                    (if r -> flag then r -> legs -> unders else r -> legs -> unders)
                        then obs
                        then extract [ item -> idents ]
                        then flatten

            func B1SingleValuedThen: <"b1 - DECLINE: the value genuinely compiles MapperS">
                inputs:
                    r Root (1..1)
                output:
                    out Ident (0..*)
                add out:
                    r -> legs first -> one
                        then obs
                        then extract [ item -> idents ]
                        then flatten

            func B2ListOfListsThen: <"b2 - DECLINE: the level is MapperListOfLists">
                inputs:
                    r Root (1..1)
                output:
                    out Ident (0..*)
                add out:
                    r -> legs
                        then extract [ item -> unders ]
                        then extract [ item -> obs -> idents ]
                        then flatten

            func B3CollapsedGetThen: <"b3 - DECLINE: the #260 collapse re-wrap arm must survive">
                inputs:
                    r Root (1..1)
                output:
                    out Ident (0..*)
                add out:
                    r -> legs -> unders
                        then only-element
                        then extract [ item -> obs -> idents ]
            """;

    // =========================================================================
    // Part A — the three-hunk mechanism pins.
    //
    // ⚠ MEASURED (LAW 66/82): these were DRAFTED as failing-first pins and they are NOT.
    // a1, a2 and a3 all PASS at the RED base 6c8e1544 (f30-red-default.log / f30-red-on.log,
    // 11/2F both routes) and they also pass under m-law5s, the whole-law revert
    // (f30-mut-m-law5s.log, 11/2F). The fixture does not reach the rung, so their green says
    // nothing about the law. They are post-law MECHANISM pins: they document and freeze the
    // intended three-hunk shape, and they would catch a later regression that changes it.
    // The law's failing-first evidence is corpus_c1 + corpus_control1 (corpus grain) plus the
    // LAW-74 javac census. See the class javadoc's FAILING-FIRST GAP paragraph.
    // =========================================================================

    /**
     * a1 — hunk A. The level whose compiled value is a {@code MapperC} declares
     * {@code MapperC}.
     *
     * <p><b>MEASURED: green at RED and under {@code m-law5s}</b> — this fixture's render does
     * not carry the pre-law {@code MapperS} decl in either state, so a1 is a mechanism pin, not
     * a failing-first pin. The draft's "Fails pre-law: the AST said SINGLE and the decl said
     * {@code MapperS}" describes the CARRIER ({@code GetUnderlierProductIdentifierLeg1.java},
     * locked by {@code corpus_c1}), not this fixture.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_thenArgWhoseCompiledValueIsMapperCDeclaresMapperC() throws IOException {
        String out = fn("A1ThenArgKindFromCompiled.java");
        String code = normalize(out);
        ///PIN: MEASURED — the fixture's rendered wrapper simple name (ReferenceWithMetaObs) and
        ///PIN: the thenArg index, confirmed by a1 passing green at 11/0F in the GREEN leg.
        assertTrue(code.contains("final MapperC<ReferenceWithMetaObs> thenArg1"),
                "the meta-nav level must declare MapperC:\n" + out);
        assertTrue(!code.contains("final MapperS<ReferenceWithMetaObs> thenArg1"),
                "the MapperS decl must be gone:\n" + out);
    }

    /**
     * a2 — hunk B, the LAW-80 stop. The very next level's render must flip with the decl:
     * {@code mapSingleToList} → {@code mapItemToList}. If a1 passes and a2 does not, the file
     * heals ZERO and the seat has a PARTIAL row, not a whole one.
     *
     * <p><b>MEASURED: green at RED and under {@code m-law5s}</b> — same reading as a1. The
     * LAW-80 stop it expresses was actually enforced at CORPUS grain by {@code corpus_c1}, the
     * byte-whole lock, and by the seat's 26/0/4/0/102 LAW-80 tuple; a2 freezes the shape.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theNextLevelRendersMapItemToList() throws IOException {
        String out = fn("A1ThenArgKindFromCompiled.java");
        String code = normalize(out);
        assertTrue(code.contains(".mapItemToList("),
                "the level after a MapperC thenArg must render mapItemToList:\n" + out);
        assertTrue(!code.contains(".mapSingleToList("),
                "the mapSingleToList render must be gone:\n" + out);
    }

    /**
     * a3 — hunk C. The third-state decl AND its import ride the same read. Both are asserted
     * because they enter through two different channels ({@code wrapperSimple} and
     * {@code refs.add}) and a regression in either alone is a real failure mode.
     *
     * <p><b>MEASURED: green at RED and under {@code m-law5s}</b> — same reading as a1/a2. The
     * import channel IS separately worth freezing, but this test has no measured witness for
     * it at 25 cells or at the RED base.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_theListOfListsLevelDeclaresAndImportsMapperListOfLists() throws IOException {
        String out = fn("A1ThenArgKindFromCompiled.java");
        String code = normalize(out);
        assertTrue(code.contains("import com.rosetta.model.lib.mapper.MapperListOfLists;"),
                "the MapperListOfLists import must be emitted:\n" + out);
        assertTrue(code.contains("final MapperListOfLists<"),
                "the mapItemToList level must declare MapperListOfLists:\n" + out);
    }

    // =========================================================================
    // Part B — the decline pins (witness-unique on tokens the flip REMOVES)
    // =========================================================================

    /**
     * b1 — the law reads the COMPILED kind, not "always MapperC". WITNESS-UNIQUE on
     * {@code final MapperS<} + {@code .mapSingleToList(} — the exact pair the flip removes at
     * a1.
     *
     * <p><b>MEASURED CORRECTION (LAW 82).</b> The draft said this "fires under
     * {@code m-law5s-anykind}". <b>It does not</b>: that lane measured 11/1F — the standing
     * {@code corpus_control1} at its unchanged residue of 4 — i.e. net EMPTY, b1 green
     * ({@code f30-mut-m-law5s-anykind.log}). Widening {@code isMapperC} to "not MapperS" moves
     * no byte in this fixture and none at 25 cells. b1 is therefore an un-witnessed shape lock,
     * not a firing decline lock; the falsifier that would give it a witness is a level whose
     * compiled value is neither {@code MapperS} nor {@code MapperC} reaching this decl seat.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_genuinelySingleThenArgKeepsMapperS() throws IOException {
        String out = fn("B1SingleValuedThen.java");
        String code = normalize(out);
        assertTrue(code.contains("final MapperS<"),
                "a genuinely single level must keep its MapperS decl:\n" + out);
        assertTrue(code.contains(".mapSingleToList("),
                "and its mapSingleToList render:\n" + out);
    }

    /**
     * b2 — the {@code !listOfLists} conjunct. A MapperListOfLists level owns its own wrapper
     * and the rung must not override it. WITNESS-UNIQUE on {@code final MapperListOfLists<},
     * the token {@code m-law5s-nolol} was drafted to replace with {@code MapperC}.
     *
     * <p><b>MEASURED CORRECTION (LAW 82).</b> Both lanes that named b2 measured net EMPTY —
     * {@code m-law5s-nolol} and {@code m-law5s-anykind} each came back 11/1F, the standing
     * {@code corpus_control1} at its unchanged residue of 4, b2 green. Escaping the
     * {@code !listOfLists} conjunct does NOT rewrite this fixture's third-state wrapper, so the
     * conjunct has no measured carrier at 25 cells or here. b2 stands as a shape lock on the
     * third cardinality state, honestly scored as un-witnessed.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_listOfListsLevelKeepsItsThirdStateWrapper() throws IOException {
        String out = fn("B2ListOfListsThen.java");
        String code = normalize(out);
        assertTrue(code.contains("final MapperListOfLists<"),
                "a mapItemToList level must keep MapperListOfLists:\n" + out);
        assertTrue(code.contains(".mapItemToList("),
                "and its mapItemToList render:\n" + out);
    }

    /**
     * b3 — the SITING pin: the #260 {@code thenArgMapperSWrap} arm reads {@code single} BEFORE
     * this rung, so its {@code MapperS.of(<prev>.get())} re-wrap and its {@code MapperS} decl
     * must both survive byte-identically. This is the refactor-safety pin: it fails the moment
     * anyone moves the rung earlier in the method.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_theCollapseReWrapArmIsUnmoved() throws IOException {
        String out = fn("B3CollapsedGetThen.java");
        String code = normalize(out);
        assertTrue(code.contains("MapperS.of(") && code.contains(".get())"),
                "the #260 collapse re-wrap must survive:\n" + out);
        assertTrue(code.contains("final MapperS<"),
                "and the collapse level must keep its MapperS decl:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus (LAW 79/80: a byte-WHOLE lock)
    // =========================================================================

    private static final String LEG1 =
            "drr/regulation/common/functions/GetUnderlierProductIdentifierLeg1.java";

    /**
     * control0 — golden is the oracle (prove the instrument can fail). Read from GOLDEN bytes
     * only: golden declares the MapperC level, the MapperListOfLists level, the import, and
     * the {@code mapItemToList} render — and golden's THREE healthy sibling extracts in the
     * SAME file keep their own methods.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenDeclaresMapperCAndMapItemToList() throws IOException {
        String g = normalize(Files.readString(GOLDEN_A.resolve(LEG1)));
        assertTrue(g.contains("final MapperC<ReferenceWithMetaObservable> thenArg1"),
                "golden must declare the MapperC level in " + LEG1);
        assertTrue(g.contains("final MapperListOfLists<AssetIdentifier> thenArg2"),
                "golden must declare the MapperListOfLists level in " + LEG1);
        assertTrue(g.contains("import com.rosetta.model.lib.mapper.MapperListOfLists;"),
                "golden must import MapperListOfLists in " + LEG1);
        assertTrue(g.contains(".mapItemToList("),
                "golden must render mapItemToList in " + LEG1);
        assertTrue(!g.contains(".mapSingleToList("),
                "golden must NOT render mapSingleToList in " + LEG1);
        // MEASURED at the law head (the domain-pin flow): golden's rendered sibling set in
        // this file is `.mapItem(` x1 + `.mapItemToList(` x1 — the draft's unmeasured
        // `.mapListToItem(` claim did not survive the measurement and was corrected here.
        assertTrue(g.contains(".mapItem("),
                "golden's healthy sibling extract must be present in " + LEG1);
    }

    /**
     * c1 — the WHOLE-FILE lock. {@code GetUnderlierProductIdentifierLeg1} is F11-only at
     * 7.0.0–7.3.0 and all three hunks are this law, so the fork's rendered file must equal
     * golden BYTE FOR BYTE. Do not weaken to a line lock: the LAW-80 stop
     * ("decl moved, extract did not ⇒ the file heals ZERO") is only visible at whole grain.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_getUnderlierProductIdentifierLeg1IsByteIdenticalToGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String gen = drrAOutput.get(LEG1);
        assertNotNull(gen, "not generated: " + LEG1);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(LEG1))), normalize(gen),
                LEG1 + " must be byte-identical to golden (F11-only, all three hunks this law)");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0. The domain is every file
     * whose GOLDEN <b>or</b> FORK text carries a {@code thenArg} declaration; per file the
     * (MapperS decls, MapperC decls, MapperListOfLists decls, mapSingleToList renders,
     * mapItemToList renders) tuple must equal golden's beyond the NAMED residue. This is the
     * control that would catch the rung flipping a level anywhere else in the cell.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellThenArgKindsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /** control2 — LAW 77 route parity for the carrier. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(LEG1), irOut.get(LEG1), "route divergence: " + LEG1);
    }

    /** control3 — LAW 79 in a NON-carrier cell (drr 5.61.0), same UNION tuple. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellThenArgKindsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    ///PIN: MEASURED at the law's head — the drr 7.0.0 files whose tuple still differs (the
    ///PIN: other-family band files that carry thenArg decls, each with its measured tuple).
    ///PIN: The lanes MOVE this list and that is how they are scored: m-law5s prints 5 file(s)
    ///PIN: (GetUnderlierProductIdentifierLeg1 re-enters), m-law5s-nolol and m-law5s-anykind both
    ///PIN: print 4 — identical to GREEN, i.e. no movement at all. RED prints 7.
    ///PIN: RE-MEASURED at the seat-30 chain head e223ce19 — 4 entries, transcribed VERBATIM from
    ///PIN: this control's own failing print (LAW 81), down from 6. Both departures keep a non-zero
    ///PIN: GOLDEN tuple, so both stay in the union domain and DOMAIN_DRR7 is UNMOVED at 1592.
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // LAW 81 re-pin (v3.1 flip seat 33, law A.4): the GetBasketConstituents row (fork=[2, 5, 3, 0, 3] golden=[2, 4, 4, 0, 3]) LEFT this list -
            // facet lolDefaultBodyMulti moved this scan's tuple to golden's (the file stays BANDED on its A.3/A.5/A.1/A.2
            // residue; LolDefaultBodyMultiSeatTest pins that residue by name); transcribed from the control print (A4-trip1.log).
            // CommodityQuantityWithFrequency.java (was fork=[5, 1, 0, 0, 0] golden=[5, 2, 0, 0, 0])
            // left this list in a LATER commit of this same seat: law 4 (the d=4 function-level
            // all-or-nothing decline at the RThenExpr inline fallback, CollectionHandler
            // .tryDeepThenHoist) healed it WHOLE in all four drr 7.x cells, so its second MapperC
            // thenArg decl (T2) now renders.
            // ExecutionTimestampRule.java (was fork=[2, 0, 0, 0, 0] golden=[0, 2, 0, 0, 0]) left
            // this list at seat 30: law 3 (the disguised-nav REnumValueRef arm in chainProvesMulti,
            // the LEAF rung) healed it WHOLE in all four drr 7.x cells, so its two thenArg decls
            // are now MapperC (T2) where the fork had rendered MapperS (T1).
            // Both are the within-seat LAW-81 hand-off: this control went RED, as designed, when
            // the later law healed its pinned row.
            // the QuantityUnitOfMeasure row (fork=[6, 1, 0, 0, 0] golden=[6, 4, 0, 0, 0]) LEFT this list: law B.1 (setSeatNestedValueThenTogetherHoist,
            // the k>0 restructure window) healed this scan's token set to golden's in all four drr 7.x cells -
            // the file itself stays BANDED (its close is B.3 + B.24); transcribed from this control's own print
            // (B1-trip1.log), a pure row removal (was == expected minus it).
            // the TotalNotionalQuantity row (fork=[2, 0, 0, 0, 0] golden=[3, 0, 0, 0, 0]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    ///PIN: MEASURED at the law's head. The draft EXPECTED EMPTY ("no boundPipeC=true row in
    ///PIN: this cell") — the measurement said otherwise: four cftc rewrite rules carry a
    ///PIN: golden-only MapperC decl count. Transcribed VERBATIM from the control's own print;
    ///PIN: the expectation was corrected, the control was not weakened. control3 is green in
    ///PIN: every leg the seat ran with this list, so the law is zero-carrier in drr 5.61.0 as
    ///PIN: claimed - the four rows are a pre-existing OTHER-family delta, not law-5 movement.
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();
            // the four cftc Notional* rows LEFT this list at seat 32: law C.3 (ruleThenArmLadderNestedTreeAdmit) healed
            // NotionalAmountLeg1/2 + NotionalCurrencyLeg2 WHOLE in drr 5.61.0 and IMPROVED NotionalCurrencyLeg1 to a tuple this scan reads as golden's; transcribed from this control's own print (C3-trip1*.log).

    ///PIN: MEASURED at the first corpus run (LAW 73); UNMOVED at the seat-30 head — both
    ///PIN: departures from KNOWN_RESIDUE_DRR7 keep a non-zero GOLDEN tuple, so neither leaves
    ///PIN: the union.
    ///PIN: Falsifier: the domain assert itself
    ///PIN: (assertEquals(expectedDomain, universe.size())) failing on a later run. NOT the
    ///PIN: harness's "MEASURED domain=" print, which only surfaces at the -1 SENTINEL and is
    ///PIN: never emitted once a real value is pinned; on a run where the RESIDUE assert fails
    ///PIN: first the domain assert is simply unreachable.
    ///PIN: (An earlier note here said a wrong value is "masked by a green residue"; that is
    ///PIN: backwards - a GREEN residue is exactly the case where the domain assert DOES run.)
    private static final int DOMAIN_DRR7 = 1592;

    ///PIN: MEASURED at the first corpus run (LAW 73). Same falsifier as DOMAIN_DRR7.
    private static final int DOMAIN_DRR561 = 1163;

    /**
     * (T1..T5) per file: MapperS thenArg decls, MapperC thenArg decls, MapperListOfLists
     * thenArg decls, {@code .mapSingleToList(} renders, {@code .mapItemToList(} renders. The
     * decl counts and the render counts are scanned INDEPENDENTLY on purpose: the whole point
     * of the law is that those two halves had drifted apart, so a control that derived one
     * from the other would be blind to exactly the regression it exists to catch.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = new int[5];
            for (String line : normalize(e.getValue()).split("\n")) {
                String s = line.trim();
                if (s.startsWith("final Mapper") && s.contains(" thenArg")) {
                    if (s.startsWith("final MapperS<")) {
                        t[0]++;
                    } else if (s.startsWith("final MapperC<")) {
                        t[1]++;
                    } else if (s.startsWith("final MapperListOfLists<")) {
                        t[2]++;
                    }
                }
                t[3] += count(s, ".mapSingleToList(");
                t[4] += count(s, ".mapItemToList(");
            }
            if (t[0] + t[1] + t[2] + t[3] + t[4] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    private static int count(String s, String needle) {
        int n = 0;
        int from = 0;
        while ((from = s.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[5];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values so
        // the pin is transcribed from this assert's own output, never invented.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "the decl/render tuple differs beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
    }

    // =========================================================================
    // Harness (the seat-26/27/28/29 suite shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_B_ROOT), errs);
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

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat30law5.rosetta");
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
            fixtureOut = render(m -> "census.seat30law5".equals(m.namespace()));
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[ThenArgDeclKindFromCompiledSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
