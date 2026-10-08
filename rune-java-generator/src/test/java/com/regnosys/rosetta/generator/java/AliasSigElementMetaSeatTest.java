// SEAT 30 LAW 1 — facet aliasSigElementMetaKeep (apply scripts: drafts30/law1s-apply.py,
// law1s-apply2.py). SHIPPED.
// Every PIN marked  ///PIN:  carries its MEASURED value, transcribed from the chain's own
// print (LAW 73: pin the SET, not the count). The chain that measured them:
// chain-all30.ps1 @ e223ce19; logs f30-{red,green}-{default,on}.log and f30-mut-<lane>.log.
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
 * SEAT 30, law 1 — facet {@code aliasSigElementMetaKeep}: <b>a then-arg piped from a META
 * ALIAS declares the element the alias's own EMITTED SIGNATURE declares, not the one its
 * BODY walk re-derives.</b>
 *
 * <p><b>The two halves that disagreed (LAW 69).</b> The then-arg decl seat
 * ({@code FunctionExpressionRenderer.renderThenExtractSetImpl}) takes its element from
 * {@code NavigationHandler.aliasDerivedThenArgItemType}, whose own javadoc says it
 * <i>"[resolves] the alias's element type by walking its OWN expression body …
 * (tryAliasReceiverMapperType exposes the item type only for META aliases …)"</i> — a
 * meta-BLIND walk that names the META channel it declines to use. The alias METHOD rendered
 * into the same file takes its return type from that META channel. So the two halves emitted,
 * in one file:
 * <pre>
 * fork decl:   final MapperC&lt;? extends PriceSchedule&gt;              thenArg0 = priceSchedule(…);
 * fork method: protected MapperC&lt;? extends FieldWithMetaPriceSchedule&gt; priceSchedule(…)
 * </pre>
 * — a generics non-compile, and the bare binding also cost the body its
 * {@code final FieldWithMetaPriceSchedule w = item.get();} deref hoist. FER already consults
 * the signature channel 2,000 lines away at the WHOLE-OUTPUT seat (facet {@code metaSigKeep},
 * PR #382) and seat 28 law D ({@code aliasSigWildcardDecl}) settled the same law for the
 * WILDCARD. This is that law for the ELEMENT.
 *
 * <p><b>TWO RUNGS, CONJUNCTIVE — the "one read" claim was REFUTED BY MEASUREMENT (LAW 82).</b>
 * The draft claimed both hunks follow from the decl's element read. The measured run at the
 * rung-1 head says otherwise: the DECL hunk healed and {@code a2} FAILED, with
 * {@code corpus_c1} printing the exact residue — the fork still emits the INLINE lambda
 * {@code .mapItemToList(item -> MapperC.<PricePeriod>of(reportablePricePeriod.evaluate(
 * item.get(), …)))} where golden emits the BLOCK lambda with the wrapper hoist and the
 * null-guarded deref. The two hunks are <b>conjunctive halves</b> (the seat-29 law-9/9b
 * sense), not one read:
 * <ul>
 *   <li><b>rung 1</b> ({@code law1s-apply.py}, {@code FunctionExpressionRenderer:6736-6741})
 *       — the DECL's element reads the alias SIGNATURE channel;</li>
 *   <li><b>rung 2</b> ({@code law1s-apply2.py}, {@code ReferenceHandler:2477-2479}) — the
 *       evaluate-ARG's meta type reads the then-arg BINDING.</li>
 * </ul>
 *
 * <p><b>Why rung 2 is needed, and why rung 1 unlocks it.</b> The deref is produced by
 * {@code ReferenceHandler.tryMetaDerefArg}, which needs the arg's META type. For an explicit
 * {@code item} argument the compiled type is <b>null</b> — {@code handle(RImplicitVariable)}
 * returns a bare untyped identifier ({@code ReferenceHandler:3521-3526}) — the alias and
 * terminal-nav fallbacks decline (the arg is neither), the {@code #285} recovery is
 * rule-scoped and this is a FUNCTION, so the ONLY channel left is the {@code #346}
 * {@code implicitItemArgMeta} walk at {@code :2477-2479}. <b>That walk recovers the wrapper
 * from the owning chain's TERMINAL NAV via {@code recoverMetaFromExpr}, which has no
 * alias-SIGNATURE arm — the same meta-blindness rung 1 displaced at the decl seat, one seat
 * over.</b> Rung 2 consults {@code HandlerHelper.bareItemThenPipeMetaType} instead — the
 * {@code #362} render-truth channel that reads the scope's {@code thenArgRefFor} binding, and
 * that already has SIX consumers (ArithmeticHandler ×2, CollectionHandler, ComparisonHandler,
 * ConstructionHandler, NavigationHandler's {@code #375} A4-c arm). This seat was the one
 * consumer missing: LAW 69, one walk, now seven consumers. <b>Pre-rung-1 that binding's
 * element was the bare {@code PriceSchedule}, so rung 2 would have been byte-inert — rung 1
 * is what creates the intersection rung 2 fires in.</b>
 *
 * <p>{@code a2} is the pin for the second rung and MUST NOT be weakened: a {@code partial}
 * LAW-80 row — decl moved, body deref did not — is exactly the state the measurement found.
 *
 * <p><b>The probe verdict this law answers (LAW 75).</b> {@code [PROBE30-ALIASDECL]} at the
 * decl's element derivation, both routes, all 25 cells:
 * {@code tryAliasReceiverMapperType} resolves at this seat in <b>4 rows out of 22,794</b>
 * corpus-wide, <b>all four are the carrier</b>, all four {@code disagree=true}, and the row
 * text is byte-identical OFF and ON:
 * <pre>
 * [PROBE30-ALIASDECL] where=fn:ReportablePricePeriod k=0 argKind=RSymbolReference single=false
 *   bodyItem=PriceSchedule bodyItemWrapper=false sigItem=FieldWithMetaPriceSchedule sigItemWrapper=true
 *   seam=MapperC&lt;?_extends_FieldWithMetaPriceSchedule&gt; baseKind=RSymbolReference
 *   baseSigItem=cdm.observable.asset.metafields.FieldWithMetaPriceSchedule
 *   baseSeam=MapperC&lt;?_extends_FieldWithMetaPriceSchedule&gt; disagree=true ruleScoped=false
 * </pre>
 * <b>The measured over-fire domain is ZERO rows.</b> The dump settles the one question the
 * probe could not: the fork ALREADY emits the {@code ? extends}, so this is a pure ELEMENT
 * upgrade, one rung, not a two-part fix.
 *
 * <p><b>Carriers.</b> {@code drr/standards/iosco/cde/version1/price/functions/
 * ReportablePricePeriod.java}, drr 7.0.0 / 7.1.0 / 7.2.0 / 7.3.0, {@code lines=7 hunks=2},
 * <b>F13-only → 4 of 4 WHOLE</b>. This is the seat's only law whose {@code corpus_c1} is a
 * byte-WHOLE lock rather than a line lock.
 *
 * <p><b>LAW 80 expectation.</b> {@code 4 whole / 0 partial / 0 entered}, route-identical,
 * −4 mismatches on BOTH routes.
 *
 * <p><b>LAW 74 — RUNG 2 IS THE COMPILE REPAIR, NOT POLISH.</b> MEASURED by reading the
 * generated signatures: {@code drr/base/trade/price/functions/ReportablePricePeriod.java:46}
 * declares {@code evaluate(PriceSchedule, CalculationSchedule, DefaultingType)}, and
 * {@code cdm/observable/asset/metafields/FieldWithMetaPriceSchedule.java:28} declares
 * {@code FieldWithMetaPriceSchedule extends RosettaModelObject, FieldWithMeta<PriceSchedule>,
 * GlobalKey} — the wrapper is <b>not</b> a {@code PriceSchedule}, it WRAPS one. So the carrier
 * is non-compiling in BOTH states, at DIFFERENT sites:
 * <ul>
 *   <li><b>pre-rung-1</b> at the DECL — {@code MapperC<? extends PriceSchedule> thenArg0 =}
 *       a method returning {@code MapperC<? extends FieldWithMetaPriceSchedule>};</li>
 *   <li><b>post-rung-1, pre-rung-2</b> at the ARG — {@code evaluate(item.get(), …)} splices a
 *       {@code FieldWithMetaPriceSchedule} into a {@code PriceSchedule} parameter.</li>
 * </ul>
 * {@code javac30 PRE} must hold RPP ×4 in either state; {@code POST} is 0 only with BOTH
 * rungs. <b>Landing rung 1 alone would ship a moved-but-still-broken file.</b>
 *
 * <p><b>RED — MEASURED at the RED base {@code 6c8e1544}, BOTH routes</b>
 * ({@code f30-red-default.log}, {@code f30-red-on.log}):
 * <ul>
 *   <li>default profile <b>12 run / 6F / 0E / 1 skip</b> — <b>a1, a2, a3, b4, corpus_c1,
 *       corpus_control1</b>. {@code corpus_control1} prints
 *       {@code differ beyond the named residue in 9 file(s)} against its pinned 7.</li>
 *   <li>{@code -Pir-on} <b>12 run / 7F / 0E / 0 skip</b> — the SAME six plus
 *       <b>{@code corpus_control2_bothRoutesEqualGoldenForTheCarrier}</b>
 *       ({@code the LEGACY route differs from golden: …ReportablePricePeriod.java}). That one
 *       member is <b>ON-only by construction</b>: it is {@code @EnabledIf}-gated on the IR
 *       route and is the single skip the default profile reports. Over the 114 tests both
 *       profiles execute the two RED sets are IDENTICAL.</li>
 *   <li>GREEN at the seat head: <b>12/0F/1 skip</b> default, <b>12/0F/0 skip</b> ON. The whole
 *       suite moves RED → GREEN.</li>
 * </ul>
 * <b>The RED leg's measured construction caveat (recorded, not waived).</b> The chain's RED
 * checkout is {@code git checkout 6c8e1544 -- rune-java-generator/src/main} — it reverts the
 * GENERATOR only. {@code rune-ir-java/…/IRExpressionCompiler.java} (+101/−10, rung 2's IR
 * twin) stays at HEAD through the whole RED leg on both profiles, so <b>RED under-reports rung
 * 2 on the IR route</b>. What RED does prove for rung 2 is the legacy-route half, and
 * {@code m-law1-rung2} below is the lane that isolates it.
 *
 * <p><b>MUTATION LANES — MEASURED (LAW 82). Every set below is transcribed from its archived
 * log {@code f30-mut-&lt;lane&gt;.log}; where the measurement contradicted the drafted claim the
 * correction is NAMED in place.</b>
 * <b>⚠ THE LANE LOOP RUNS THE DEFAULT PROFILE ONLY.</b> {@code corpus_control2} is
 * {@code @EnabledIf}-gated on the IR route and is the ONE skip in every lane's
 * {@code …/1 skipped} run, so it CANNOT fail in a lane by construction. Each set below is the
 * DEFAULT-classpath measurement; {@code corpus_control2} is carried in a parenthetical as an
 * UNMEASURED claim — adjudicating it means running that lane under {@code -Pir-on} explicitly,
 * which the lane loop does not do.
 * <ul>
 *   <li><b>m-law1</b> ({@code f30-mut-m-law1.log}) = {@code law1s-apply.py --revert}.
 *       <b>MEASURED 12/6F/0E/1S: a1, a2, a3, b4, corpus_c1, corpus_control1</b>
 *       (+{@code corpus_control2} claimed on {@code -Pir-on}, unmeasured).
 *       <b>Drafted a1, a2, a3, corpus_c1; measured that set PLUS b4 and corpus_control1 — the
 *       difference explained:</b> (i) {@code b4}'s own assert is
 *       {@code "b4's decl must still take rung 1's wrapper, else the pin proves nothing"} — its
 *       PRECONDITION is rung 1's shape, so reverting rung 1 fires it. b4 is therefore a
 *       boundary pin that MOVES WITH the law, not a decline lock that survives it; the draft
 *       mis-scored it as the latter. (ii) {@code corpus_control1} was under-named: reverting
 *       rung 1 puts {@code ReportablePricePeriod.java} back into the differing set, so the
 *       control prints <b>8 file(s)</b> against its pinned 7. The lane's reach is the whole
 *       cell, not just the fixtures — exactly what a whole-cell control is for.</li>
 *   <li><b>m-law1-valuetype</b> ({@code f30-mut-m-law1-valuetype.log}) = {@code --mut-novaluetype}
 *       (the {@code getValueType().equals(itemType)} conjunct escaped).
 *       <b>MEASURED 12/0F/0E/1S — EMPTY.</b> <b>Drafted b2 + corpus_control1; measured EMPTY —
 *       the difference explained:</b> the draft's own text pre-authorised this reading (<i>"if
 *       control1 comes back GREEN the conjunct is not load-bearing at this corpus"</i>), and the
 *       measurement went further: b2 is green too. <b>EMPTY is the ADJUDICATED value here, per
 *       the seat-29 {@code m-law5ii} precedent</b> — a measured zero is a finding, not a missing
 *       measurement. What it means: the value-type conjunct has <b>ZERO carriers in this
 *       corpus and zero in the fixture set</b>. It ships as a strictly-NARROWING guard justified
 *       by the shape it expresses (never retype a bare element across value types), and it is
 *       recorded as byte-neutral at 25 cells — the honest score is "no corpus evidence either
 *       way", not "load-bearing".</li>
 *   <li><b>m-law1-narrow</b> ({@code f30-mut-m-law1-narrow.log}) = {@code --mut-narrow} (the
 *       rung moved INSIDE the {@code #238} wildcard {@code if}, so it fires only when the
 *       wildcard already fired). <b>MEASURED 12/0F/0E/1S — EMPTY, and EMPTY was the claim:
 *       MATCH.</b> The DESIGNED ZERO discharged. What the empty means: the wide and narrow
 *       forms emit IDENTICAL bytes over the whole corpus and the whole fixture set, i.e. every
 *       carrier already takes the wildcard. <b>The narrow form therefore has the strictly
 *       smaller blast radius for identical output</b>, and that trade — wider reach kept for
 *       the families the seat-30 F13 charter still has open, against the narrower form the
 *       measurement licenses — is the one live decision this lane hands forward.</li>
 *   <li><b>m-law1-rung2</b> ({@code f30-mut-m-law1-rung2.log}) = {@code law1s-apply2.py
 *       --mut-nofallback} (the binding read removed — i.e. rung 1 shipped alone).
 *       <b>MEASURED 12/3F/0E/1S: a2, corpus_c1, corpus_control1.</b>
 *       <b>Drafted a2 + corpus_c1 (recorded from the coordinator's earlier rung-1-head run);
 *       measured that set PLUS corpus_control1 — the difference explained:</b> the earlier
 *       record predates this class's whole-cell control, so it could not have named it. At the
 *       rung-1-only head the control prints <b>8 file(s)</b> against its pinned 7 — the same
 *       {@code ReportablePricePeriod} row m-law1 exposes, proving <b>rung 2 is corpus-visible on
 *       its own</b> and not merely a fixture repair. This is the lane that carries the
 *       conjunctive-halves claim: rung 1 alone leaves the file moved-but-still-broken.</li>
 *   <li><b>m-law1-rung2-bindingfirst</b> ({@code f30-mut-m-law1-rung2-bindingfirst.log}) =
 *       {@code law1s-apply2.py --mut-bindingfirst} (the binding read placed BEFORE the
 *       {@code #346} walk instead of behind it). <b>MEASURED 12/0F/0E/1S — EMPTY, and EMPTY was
 *       the claim: MATCH.</b> The ORDER adjudication is discharged in the direction the draft
 *       named as the good one: the two channels do NOT disagree anywhere in this corpus or
 *       fixture set, so the order is free and the fallback placement (which keeps every existing
 *       {@code #346} carrier's bytes by construction) is the right ship. Had this measured
 *       NON-EMPTY it would have been a LAW-69 finding to resolve; it did not.</li>
 * </ul>
 * <b>Lane tally for this suite: 3 MATCH (2 of them designed zeros, both ADJUDICATED) · 2
 * MISMATCH-corrected · 1 MEASURED-EMPTY-against-a-non-empty-claim.</b> Measured at
 * {@code e223ce19}; the pinned residue lists this class carries were NOT moved by the seat's
 * re-pin commit, so these sets stand at the current head.
 *
 * <p><b>⚠ THE NAMED ENTERING RISK OF RUNG 2.</b> Rung 1 created the wrapper-element
 * then-binding for the alias-rooted family, but a PRE-EXISTING population already has
 * wrapper-element bindings that rung 2 newly exposes to the evaluate-arg seat: the
 * {@code #362 deepThenLevelElementPreserve} family (drr
 * {@code DTCC_TradeParty1/2ReportingDestination}, whose {@code thenArg3} is
 * {@code MapperC<FieldWithMetaSupervisoryBodyEnum>}) and the {@code #375 iteChainArmThenHoist}
 * family ({@code Create_AnnaDsbUpiRequestUnderlyingForRate}). Their items are consumed by
 * comparison/nav consumers today rather than by an explicit-args evaluate at a bare param —
 * but that is a READING, not a measurement. {@code b4} pins the boundary at fixture grain and
 * {@code corpus_control1} / {@code corpus_control3} are the corpus measurement; <b>do not ship
 * rung 2 on the reading alone.</b>
 */
class AliasSigElementMetaSeatTest {

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
     * Cell B = cdm 6.20.6 — the EMPTY-DOMAIN guard, deliberately CROSS-CORPUS (not a second
     * drr cell): dense in aliases and in then-chains, ZERO {@code [PROBE30-ALIASDECL]} rows
     * that resolve a signature item. The rung must not reach a byte here.
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

    // =========================================================================
    // The fixture — the corpus shape minimised, plus its three decline twins
    // =========================================================================

    /**
     * The corpus shape ({@code ReportablePricePeriod}): an ALIAS whose body navigates to a
     * {@code [metadata scheme]} leaf — so its emitted signature is
     * {@code MapperC<? extends FieldWithMetaX>} — piped into a {@code then} whose body calls a
     * function with the element as its first argument.
     *
     * <p><b>The three decline twins are minimal pairs</b>, each isolating one conjunct:
     * <ul>
     *   <li>{@code NonMetaAliasThen} — the SAME shape over a meta-FREE alias leaf
     *       ({@code tryAliasReceiverMapperType} answers null: the #352 META-only boundary);</li>
     *   <li>{@code MismatchedWrapperAliasThen} — a META alias whose wrapper's VALUE type
     *       differs from the element the earlier rungs already computed (the value-type
     *       conjunct);</li>
     *   <li>{@code DirectNavThen} — the same pipe with a DIRECT navigation instead of an alias
     *       ({@code resolveAliasShortcut} returns null).</li>
     * </ul>
     */
    private static final String MODEL = """
            namespace census.seat30law1
            version "1.0.0"

            type Sched:
                tenor string (0..1)

            type OtherSched:
                marker string (0..1)

            type Period:
                tenor string (0..1)

            type Holder:
                sched Sched (0..*)
                    [metadata scheme]
                bare Sched (0..*)
                other OtherSched (0..*)
                    [metadata scheme]

            func MakePeriod: <"the then-body callee: its first arg is the piped element">
                inputs:
                    s Sched (1..1)
                output:
                    p Period (0..1)
                set p:
                    Period { tenor: s -> tenor }

            func MakePeriodFromOther:
                inputs:
                    o OtherSched (1..1)
                output:
                    p Period (0..1)
                set p:
                    Period { tenor: o -> marker }

            func A1MetaAliasThenArg: <"a1/a2/a3 - THE CARRIER shape: a META alias piped into a then">
                inputs:
                    h Holder (1..1)
                output:
                    out Period (0..*)
                alias metaSched: <"emitted signature is MapperC<? extends FieldWithMetaSched>">
                    h -> sched
                add out:
                    metaSched
                        then extract [ MakePeriod( item, h ) ]

            func B1NonMetaAliasThenArg: <"b1 - DECLINE: the alias leaf carries no metadata">
                inputs:
                    h Holder (1..1)
                output:
                    out Period (0..*)
                alias bareSched:
                    h -> bare
                add out:
                    bareSched
                        then extract [ MakePeriod( item, h ) ]

            func B2MismatchedWrapperAliasThenArg: <"b2 - DECLINE: the wrapper's VALUE type is not the computed element">
                inputs:
                    h Holder (1..1)
                output:
                    out Period (0..*)
                alias otherMeta:
                    h -> other
                add out:
                    otherMeta
                        then extract [ MakePeriodFromOther( item, h ) ]

            func B3DirectNavThenArg: <"b3 - DECLINE: a direct navigation, not an alias reference">
                inputs:
                    h Holder (1..1)
                output:
                    out Period (0..*)
                add out:
                    h -> bare
                        then extract [ MakePeriod( item, h ) ]

            func MakePeriodFromMetaParam: <"b4's callee - its param IS [metadata scheme]-annotated, so it EXPECTS the wrapper">
                inputs:
                    s Sched (1..1)
                        [metadata scheme]
                output:
                    p Period (0..1)
                set p:
                    Period { tenor: s -> tenor }

            func B4MetaParamCalleeKeepsBareItem: <"b4 - DECLINE: rung 2's boundary - a meta-annotated callee param takes the wrapper BARE">
                inputs:
                    h Holder (1..1)
                output:
                    out Period (0..*)
                alias metaSchedForMetaParam:
                    h -> sched
                add out:
                    metaSchedForMetaParam
                        then extract [ MakePeriodFromMetaParam( item, h ) ]
            """;

    ///PIN: MEASURED from the rendered fixture text at the law head — the wrapper simple names
    ///PIN: as the builtins actually render them. GREEN at 12/0F on both routes confirms both.
    private static final String WRAPPER = "FieldWithMetaSched";
    private static final String WRAPPER_OTHER = "FieldWithMetaOtherSched";

    // =========================================================================
    // Part A — the failing-first pins (RED pre-law)
    // =========================================================================

    /**
     * a1 — the DECL. The then-arg piped from a META alias must declare the alias's own
     * signature element. Fails pre-law: the decl carries the bare element.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_metaAliasThenArgDeclaresTheSignatureWrapper() throws IOException {
        String out = fn("A1MetaAliasThenArg.java");
        String code = normalize(out);
        assertTrue(code.contains("final MapperC<? extends " + WRAPPER + "> thenArg"),
                "the thenArg decl must carry the alias signature's WRAPPER element:\n" + out);
        assertTrue(!code.contains("final MapperC<? extends Sched> thenArg"),
                "the bare-element decl must be gone:\n" + out);
    }

    /**
     * a2 — the SECOND hunk, and the pin that proves the two hunks are one law: the lambda item
     * now binds to the wrapper, so the body hoists the deref local and passes the guarded
     * value. A LAW-80 {@code partial} row is exactly "a1 passed and a2 did not".
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theBoundItemThenDerefsInTheBody() throws IOException {
        String out = fn("A1MetaAliasThenArg.java");
        String code = normalize(out);
        assertTrue(code.contains("final " + WRAPPER + " "),
                "the body must hoist `final " + WRAPPER + " <name> = item.get();`:\n" + out);
        assertTrue(code.contains("== null ? null : ") && code.contains(".getValue()"),
                "the callee argument must be the null-guarded deref:\n" + out);
    }

    /**
     * a3 — the LAW-69 pin proper. Extract the alias METHOD's rendered return type and the
     * thenArg DECL from the SAME rendered unit and assert they agree textually. This is the
     * test that catches a future divergence in EITHER half — the whole reason the defect
     * existed is that nothing checked the two against each other.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_declAgreesWithTheAliasMethodReturnTypeInTheSameFile() throws IOException {
        String out = fn("A1MetaAliasThenArg.java");
        String sigElem = between(normalize(out), "protected MapperC<", "> metaSched(");
        String declElem = between(normalize(out), "final MapperC<", "> thenArg");
        assertNotNull(sigElem, "the alias method's rendered return type was not found:\n" + out);
        assertNotNull(declElem, "the thenArg decl was not found:\n" + out);
        assertEquals(sigElem, declElem,
                "LAW 69: the alias method's signature element and the thenArg decl's element"
                        + " must AGREE in the same file:\n" + out);
    }

    // =========================================================================
    // Part B — the decline pins (witness-unique on the token the flip ADDS)
    // =========================================================================

    /**
     * b1 — the #352 META-only boundary. {@code tryAliasReceiverMapperType} answers null for a
     * non-meta alias, so the decl keeps the bare element. WITNESS-UNIQUE on a token the flip
     * ADDS: no {@code FieldWithMeta}/{@code ReferenceWithMeta} may appear in the decl.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_nonMetaAliasThenArgIsByteUnchanged() throws IOException {
        String out = fn("B1NonMetaAliasThenArg.java");
        String decl = declLine(normalize(out));
        assertNotNull(decl, "the thenArg decl was not found:\n" + out);
        assertTrue(!decl.contains("FieldWithMeta") && !decl.contains("ReferenceWithMeta"),
                "a non-meta alias thenArg must keep its bare element: " + decl);
    }

    /**
     * b2 — the VALUE-TYPE conjunct. A META alias whose wrapper wraps a DIFFERENT value type
     * than the element already computed must not be taken: the rung may only ever upgrade a
     * bare element to ITS OWN wrapper, never retype across value types.
     *
     * <p><b>MEASURED CORRECTION (LAW 82).</b> The draft said this test "fires under
     * {@code m-law1-valuetype}". <b>It does not.</b> That lane measured 12/0F/0E/1S — EMPTY
     * ({@code f30-mut-m-law1-valuetype.log}), b2 included. Escaping the
     * {@code getValueType().equals(itemType)} conjunct moves NO byte: this fixture's earlier
     * rungs never compute an element the mismatched wrapper could be substituted for, so the
     * assert ("NOT the mismatched wrapper") holds in both states. b2 is therefore a SHAPE lock
     * with no measured carrier — it documents and freezes the intended boundary, and it is
     * honestly scored as un-witnessed rather than as a firing decline lock. The falsifier that
     * would give it a witness: a fixture whose alias wrapper wraps a value type the decl's
     * computed element ACTUALLY equals under the escaped conjunct.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_metaAliasWhoseWrapperValueTypeDiffersIsUnmoved() throws IOException {
        String out = fn("B2MismatchedWrapperAliasThenArg.java");
        String decl = declLine(normalize(out));
        assertNotNull(decl, "the thenArg decl was not found:\n" + out);
        ///PIN: MEASURED — the assert ("NOT the mismatched wrapper") holds in BOTH states;
        ///PIN: m-law1-valuetype measured EMPTY (f30-mut-m-law1-valuetype.log, 12/0F/1S).
        assertTrue(!decl.contains(WRAPPER + ">"),
                "a wrapper whose value type is not the computed element must not be taken: "
                        + decl);
    }

    /**
     * b3 — the alias-ONLY boundary. A DIRECT navigation thenArg reaches no alias shortcut, so
     * neither the {@code ? extends} nor a wrapper may appear.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_directNavThenArgIsUnmoved() throws IOException {
        String out = fn("B3DirectNavThenArg.java");
        String decl = declLine(normalize(out));
        assertNotNull(decl, "the thenArg decl was not found:\n" + out);
        assertTrue(!decl.contains("? extends") && !decl.contains("FieldWithMeta"),
                "a direct-nav thenArg carries neither the wildcard nor a wrapper: " + decl);
    }

    /**
     * b4 — <b>RUNG 2's BOUNDARY, and the fixture-grain pin on its NAMED entering risk.</b> The
     * decl still takes rung 1's wrapper (the alias is a META alias, identical to a1), so the
     * lambda item IS a wrapper and rung 2's binding read RESOLVES — but the callee parameter
     * is itself {@code [metadata scheme]}-annotated, so it EXPECTS the wrapper and upstream's
     * type-directed coercion is IDENTITY there. Golden passes {@code item.get()} BARE.
     *
     * <p>This is the one conjunct that separates "the item is a wrapper" from "the item must
     * be deref'd", and it is carried TWICE — by {@code fnItemLambdaArm}'s own
     * {@code detectMetaKind(calleeFn.inputs().get(argIndex)) == NONE} gate
     * ({@code ReferenceHandler:1296-1298}) and by the {@code #347 inverseN7CalleeParamMeta}
     * decline in the core arm ({@code :2586-2589}). {@code toJavaReferenceType} STRIPS meta
     * from the param, so the type-equality gate alone could NOT decline here — which is
     * exactly the {@code #346} cp1 catch. WITNESS-UNIQUE on the tokens rung 2 ADDS: no hoist
     * local and no null-guarded {@code .getValue()} may appear.
     *
     * <p><b>MEASURED (LAW 82).</b> GREEN in both RUNG-2 states — {@code m-law1-rung2} (rung 2
     * severed) measured 12/3F with b4 NOT among them, so the decline half is confirmed: it
     * fires if rung 2 is ever widened past the meta-param gate. But <b>b4 is NOT green in both
     * RUNG-1 states</b>: it FAILS at the RED base and under {@code m-law1}
     * ({@code f30-mut-m-law1.log}) on its FIRST assert — the precondition
     * {@code "b4's decl must still take rung 1's wrapper, else the pin proves nothing"}. That
     * is by design (a decline pin whose precondition is gone proves nothing) but it means b4
     * MOVES WITH rung 1 and must be read as a rung-1 member of the RED set, not as a pure
     * decline lock. The draft's "GREEN in both states" was written before that measurement and
     * is corrected here.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_metaAnnotatedCalleeParamTakesTheBareItem() throws IOException {
        String out = fn("B4MetaParamCalleeKeepsBareItem.java");
        String code = normalize(out);
        assertTrue(code.contains("final MapperC<? extends " + WRAPPER + "> thenArg"),
                "b4's decl must still take rung 1's wrapper, else the pin proves nothing:\n"
                        + out);
        assertTrue(!code.contains("final " + WRAPPER + " "),
                "a meta-annotated callee param must NOT get the rung-2 hoist local:\n" + out);
        assertTrue(!code.contains("== null ? null : "),
                "a meta-annotated callee param must NOT get the null-guarded deref:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus (LAW 79/80: a byte-WHOLE lock; this file is F13-only)
    // =========================================================================

    private static final String RPP =
            "drr/standards/iosco/cde/version1/price/functions/ReportablePricePeriod.java";

    /**
     * control0 — golden is the oracle (prove the instrument can fail). Read from GOLDEN bytes
     * only: golden's {@code thenArg0} element EQUALS golden's {@code priceSchedule} return
     * element in the same file, and golden's body carries the deref hoist. A broken scan
     * cannot pass this quietly.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenDeclAndAliasSignatureAgree() throws IOException {
        String g = normalize(Files.readString(GOLDEN_A.resolve(RPP)));
        String sigElem = between(g, "protected MapperC<", "> priceSchedule(");
        String declElem = between(g, "final MapperC<", "> thenArg0");
        assertNotNull(sigElem, "golden's alias method return type was not found");
        assertNotNull(declElem, "golden's thenArg0 decl was not found");
        assertEquals(sigElem, declElem, "golden's two halves must agree in " + RPP);
        assertTrue(sigElem.contains("FieldWithMetaPriceSchedule"),
                "golden's agreed element is the wrapper, not the bare value: " + sigElem);
        assertTrue(g.contains("final FieldWithMetaPriceSchedule fieldWithMetaPriceSchedule = item.get();"),
                "golden's body must hoist the deref local in " + RPP);
    }

    /**
     * c1 — the WHOLE-FILE lock. {@code ReportablePricePeriod} is F13-ONLY at 7.0.0–7.3.0
     * ({@code lines=7 hunks=2}, both hunks this law), so the fork's rendered file must equal
     * golden BYTE FOR BYTE. Do not weaken this to a line lock — the whole-file claim is the
     * law's entire LAW-80 value.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_reportablePricePeriodIsByteIdenticalToGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String gen = drrAOutput.get(RPP);
        assertNotNull(gen, "not generated: " + RPP);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(RPP))), normalize(gen),
                RPP + " must be byte-identical to golden (F13-only, both hunks this law)");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0. Domain = every file whose
     * GOLDEN <b>or</b> FORK text contains a {@code thenArg} declaration; per file the
     * (wildcard-wrapper decls, bare decls, item.get() deref hoists) triple must equal golden's
     * beyond the NAMED residue. Union, never intersection — an intersection scan is blind in
     * both directions.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellThenArgDeclsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /**
     * control2 — <b>LAW 77, per FILE, on BOTH routes.</b> This test caught a real divergence and its shape is
     * the lesson: route PARITY alone is not enough, because two routes can agree by both being wrong. The
     * mid-seat whole-matrix checkpoint measured rung 1 healing the DECL hunk on both routes while rung 2's
     * body deref fired on the LEGACY route only — {@code ReportablePricePeriod} ×4 left the OFF band and
     * stayed in the ON band, all four cells, 5 lines / 1 hunk. So this asserts all three legs:
     * <ol>
     *   <li>the LEGACY route equals GOLDEN byte-for-byte;</li>
     *   <li>the IR route equals GOLDEN byte-for-byte — the leg that was RED, and the reason the assert is
     *       written against golden rather than against the other route;</li>
     *   <li>the two routes agree (implied by 1 and 2, kept so a future failure names the axis).</li>
     * </ol>
     *
     * <p><b>The divergence point, for the record.</b>
     * {@code IRExpressionCompiler.argItemTypeIsMeta} is a scope-free MIRROR of legacy's typing cascade whose
     * verdict trips the post-pin guard so the claim is served by legacy's own render. Rung 2 added a FOURTH
     * channel to legacy's cascade (the then-pipe BINDING read); the mirror had only THREE, and all three
     * bottom out in {@code recoverMetaFromExpr}, which is alias-blind. So the mirror answered "not meta", the
     * guard did not trip, and the IR rendered the call itself without the hoist. The fix is the SAME predicate
     * at that seat, threaded with the scope it needs — not a route fork.
     *
     * <p>⚠ This test is {@code @EnabledIf}-skipped unless the IR provider is on the classpath, so it pins
     * nothing on a default suite run — <b>the {@code -Pir-on} leg of the chain is what makes it load-bearing.</b>
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_bothRoutesEqualGoldenForTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        String golden = normalize(Files.readString(GOLDEN_A.resolve(RPP)));
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        String legacyGen = drrAOutput.get(RPP);
        String irGen = irOut.get(RPP);
        assertNotNull(legacyGen, "the legacy route did not generate: " + RPP);
        assertNotNull(irGen, "the IR route did not generate: " + RPP);
        assertEquals(golden, normalize(legacyGen), "the LEGACY route differs from golden: " + RPP);
        assertEquals(golden, normalize(irGen),
                "the IR route differs from golden (the LAW-77 leg — rung 2's BINDING channel must live at the"
                        + " IR claim seat too, not only in ReferenceHandler): " + RPP);
        assertEquals(normalize(legacyGen), normalize(irGen), "route divergence: " + RPP);
    }

    /**
     * control3 — the EMPTY-DOMAIN guard in a NON-carrier CORPUS (cdm 6.20.6). Zero
     * signature-resolving rows were measured here, so the whole-cell triple must be flat
     * against golden beyond the named residue.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkCdm6206WholeCellThenArgDeclsEqualGoldenFileByFile() throws IOException {
        assertNotNull(cdmBOutput, "cdm 6.20.6 generation did not run");
        assertEquals(List.of(), cdmBGenErrors,
                "cdm 6.20.6 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(cdmBOutput), scan(readGoldenTree(GOLDEN_B)), cdmBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM6206, DOMAIN_CDM6206);
    }

    ///PIN: MEASURED at the seat head (7 rows) — the drr 7.0.0 files whose triple still differs;
    ///PIN: each is an OTHER-family band file that carries thenArg decls. GREEN 12/0F both
    ///PIN: routes says the list is exact. The lanes move it: m-law1 and m-law1-rung2 each
    ///PIN: print "8 file(s)" (ReportablePricePeriod re-enters) — see the class javadoc.
    ///PIN: RE-MEASURED at the seat-31 chain head f2a4d5c0 — 6 rows, transcribed VERBATIM from
    ///PIN: control1's own failing print (LAW 81): the UnderlierProductIdentifier row LEFT (below).
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // UnderlierProductIdentifier.java (was fork=[0, 1, 5, 0] golden=[0, 2, 5, 0]) left this list
            // at seat 31: law 2b (aliasSigWildcardDecl at the CFH deep-then seat - agreeing arms that are bare calls to a wildcard-signed
            // alias force the `? extends` DECL) - its cdsProductID
            // thenArg0 decl is now the wildcard form golden carries, so the file's then-arg decl tuple
            // equals golden's. The file itself stays BANDED (26 diff lines, the F1 residue), and golden's
            // tuple stays non-zero, so it remains inside the union domain and DOMAIN_DRR7 is UNMOVED at 1585.
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[0, 0, 2, 1] golden=[0, 0, 2, 0]) LEFT this list at seat 32: law D.1's left deref made this
            // scan's tuple equal golden's (the file stays BANDED on its D.2 sigs); transcribed from the print (D1-trip1.log).
            // the CQWF row left this list at the law-4 head (defaultRightNestedThenHoist
            // healed it) - the within-seat LAW-81 re-pin, transcribed from the measured
            // -Pir-on print at the law-8 + IR-twin head (7 rows, was 8).
            // the UnderlyingIndexIndicatorRule row (fork=[0, 0, 3, 1] golden=[0, 0, 3, 0]) LEFT this list at seat 32: law D.1 (defaultJoinDerefAtCollapsedLeft)
            // healed it WHOLE in all four drr 7.x cells; transcribed from this control's own print (D1-trip1.log).
            // the StrikePrice row (fork=[0, 0, 1, 0] golden=[0, 1, 1, 0]) LEFT this list at seat 33: law D.12
            // (iteDeclWildcardFromDisagreeingArms) declares golden's 'final MapperC<? extends PriceSchedule>
            // thenArg;' so T2 rises 0 -> 1 = golden's; the file is WHOLE in all four drr 7.x cells
            // (StrikePriceIteLadderWholeSeatTest corpus_c1/c2); transcribed from the control print (D12-trip1.log).
            // the QuantityUnitOfMeasure row (fork=[0, 0, 10, 3] golden=[0, 0, 10, 2]) LEFT this list: law C.2 rung R3a (the heteroMeta join deref)
            // took this scan's token set to golden's - the file stays BANDED (its close is B.24); transcribed
            // from this control's own print (C2-trip1.log), a pure row removal.
            // LAW 81 re-pin (seat 33, law B.3): fork=[0, 0, 10, 0] -> fork=[0, 0, 10, 3] - the bare-rule invocations + their guarded arg derefs moved this scan's fork side; the file stays BANDED (B.24); from B3-trip1.log.
            // LAW 81 re-pin (seat 33, law B.1): fork=[0, 0, 7, 0] -> fork=[0, 0, 10, 0] - the enabler's k>0 window moved this scan's fork side; the file stays BANDED (B.3 + B.24); from B1-trip1.log.
            // the TotalNotionalQuantity row (fork=[0, 0, 2, 0] golden=[0, 0, 3, 0]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    ///PIN: MEASURED EMPTY at the law's head — this corpus carries no law-1 row, which is the
    ///PIN: stronger claim and it held (GREEN 12/0F both routes).
    private static final List<String> KNOWN_RESIDUE_CDM6206 = List.of();

    ///PIN: the union domain size (LAW 73), MEASURED — the value was transcribed from the
    ///PIN: sentinel's own "MEASURED domain=" print, not derived by hand, and it is CONFIRMED at
    ///PIN: 71a92e826 (chain run 2): the suite is 12/0F on both routes, so the residue assert
    ///PIN: passed, execution reached assertEquals(expectedDomain, universe.size()), and that
    ///PIN: passed too.
    ///PIN: THE FALSIFIER, stated correctly: it is that domain assert itself failing on a later
    ///PIN: run. It is NOT "a GREEN run whose MEASURED domain= print disagrees" — that print only
    ///PIN: surfaces when expectedDomain < 0 (the sentinel), so a green run never emits it. An
    ///PIN: earlier note here also called a wrong value "masked"; that holds only on a run where
    ///PIN: the RESIDUE assert fails first, which neither measured head did.
    private static final int DOMAIN_DRR7 = 1585;

    ///PIN: same story as DOMAIN_DRR7 above — transcribed from the sentinel print, and CONFIRMED
    ///PIN: by the same 12/0F run 2 at 71a92e826 reaching and passing the domain assert.
    private static final int DOMAIN_CDM6206 = 99;

    /**
     * (T1, T2, T3) per file:
     * <ul>
     *   <li><b>T1</b> — the law's ADDED shape: {@code final Mapper*<? extends *WithMeta*>
     *       thenArg} decls (a wrapper-element then-arg declaration).</li>
     *   <li><b>T2</b> — the law's REMOVED shape: {@code final Mapper*<? extends } decls whose
     *       element is NOT a meta wrapper.</li>
     *   <li><b>T3</b> — the OVER-FIRE NET: the file's total {@code thenArg} declaration count.
     *       Deliberately global: the decl population itself must not move by one occurrence
     *       anywhere in the cell.</li>
     * </ul>
     * The a2 hunk rides T1 by construction (the hoist follows the decl), but it is ALSO
     * scanned as T4 so a decl-moved/body-unmoved partial cannot hide inside an equal triple.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = e.getValue();
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            int t4 = 0;
            for (String line : normalize(code).split("\n")) {
                String s = line.trim();
                if (s.startsWith("final Mapper") && s.contains(" thenArg")) {
                    t3++;
                    if (s.contains("? extends ")) {
                        if (s.contains("WithMeta")) {
                            t1++;
                        } else {
                            t2++;
                        }
                    }
                }
                if (s.startsWith("final ") && s.contains("WithMeta") && s.endsWith("= item.get();")) {
                    t4++;
                }
            }
            if (t1 + t2 + t3 + t4 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3, t4});
            }
        }
        return out;
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
        int[] zero = new int[4];
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
                "(T1, T2, T3, T4) differ beyond the named residue in " + mismatched.size()
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
    private static Map<String, String> cdmBOutput;
    private static List<String> cdmBGenErrors;

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
            cdmBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_B_ROOT), errs);
            cdmBGenErrors = errs;
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

    /** The text between the first {@code open} and the following {@code close}, or null. */
    private static String between(String s, String open, String close) {
        int i = s.indexOf(open);
        if (i < 0) {
            return null;
        }
        int j = s.indexOf(close, i + open.length());
        return j < 0 ? null : s.substring(i + open.length(), j);
    }

    /** The first {@code final Mapper…  thenArg…} declaration line, trimmed, or null. */
    private static String declLine(String s) {
        for (String line : s.split("\n")) {
            String t = line.trim();
            if (t.startsWith("final Mapper") && t.contains(" thenArg")) {
                return t;
            }
        }
        return null;
    }

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat30law1.rosetta");
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
            fixtureOut = render(m -> "census.seat30law1".equals(m.namespace()));
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
            throw new AssertionError("[AliasSigElementMetaSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
