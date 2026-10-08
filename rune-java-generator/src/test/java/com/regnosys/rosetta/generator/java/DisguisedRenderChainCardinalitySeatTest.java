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
 * SEAT 30, law 3 -- facet {@code disguisedRenderChainCardinality}: <b>the cardinality half of a
 * disguised {@code a -> b} navigation must read the SAME resolution the RENDER half already used
 * to emit the chain</b> (LAW 69, one walk two halves).
 *
 * <p><b>The disagreement, measured.</b> The fork's grammar parses {@code a -> b} as an
 * {@link com.regnosys.rosetta.ast.expressions.references.REnumValueRef} ({@code REnumValueRef.java
 * :99-124}). {@code ReferenceHandler.handle(REnumValueRef)} renders one by SYNTHESIZING an
 * equivalent {@code RFeatureCall} chain through a four-arm ladder -- the Cat-10 bound input chain,
 * the B2 item chain, the case-narrowed chain, and (seat 28, law 11) the by-NAME rule-input chain.
 * {@code NavigationHandler.chainProvesMulti}'s {@code REnumValueRef} arm consults NONE of them; it
 * re-derives head and leaf through {@code resolveDisguisedFeature}, whose arms root {@code enumName}
 * against the enclosing FUNCTION's inputs/outputs/shortcuts, the enclosing lambda's item type, the
 * case-narrowed subject and a callable root -- but never against a RULE's from-type, and never
 * through the then-pipe binding. So the two halves read different nodes for the same hop.
 *
 * <p><b>PROBE30-F11r, both routes, row-for-row identical.</b> At the filter seat
 * {@code recvKind=REnumValueRef} is 440 rows: 428 already {@code chainProves=true} (all
 * {@code filterItemNullSafe}, already correct) and <b>12 with {@code chainProves=false} -- THREE
 * sites, corpus-wide</b>:
 * <pre>
 * x4 rule:ExecutionTimestamp          standards-iosco-cde-version1-datetime-rule.rosetta:118:11  BAND
 * x4 fn:GetRegimeSpecificIdentifiers  regulation-common-func.rosetta:2165:18                     BAND
 * x4 fn:MapTechnicalRecordId          ingest-fpml-recordkeeping-reportableinfo-func.rosetta:799:9 GREEN
 * </pre>
 * All three carry the IDENTICAL probe signature ({@code chosen=filterSingleNullSafe
 * recvKind=rootKind=REnumValueRef recvStamp=null rootWrap=- boundT=null astMulti=false
 * chainProves=false}), so the green one is a ready-made in-signature negative control and NO field
 * the probe measured separates it -- only the truth of the cardinality does. That is why both rungs
 * are cardinality-TRUE rungs and not signature rungs.
 *
 * <p><b>The two rungs, disjoint by carrier and by structure.</b>
 * <ul>
 *   <li><b>rung A</b> ({@code NavigationHandler} :5239, the arm's TAIL). ETR's head
 *       {@code originatingWorkflowStep} is an attribute of the RULE's from-type; a rule has no
 *       enclosing {@code RFunction}, so every existing rung declines and the arm answered
 *       {@code false}. The render did not: it synthesizes {@code input -> head -> leaf} and emits
 *       {@code .<EventTimestamp>mapC("getTimestamp", ...)} because {@code timestamp} is
 *       {@code 0..*} on {@code WorkflowStep}. Rung A calls the render's OWN last arm
 *       ({@code ReferenceHandler.synthesizeImplicitInputChainByName}, widened to package-private)
 *       and reads the resulting spine's own hops via {@code synthesizedSpineProvesMulti}. No new
 *       cardinality logic -- the hops carry the resolved {@code RAttribute}s the render typed the
 *       {@code .mapC(} from.
 *       <p><b>The read is spine-BOUNDED, and that is load-bearing.</b> The first cut handed the
 *       chain to {@code chainProvesMulti} wholesale (the {@code RSymbolReference} arm's idiom at
 *       :5438-5450). That is wrong HERE: {@code ReferenceHandler.buildImplicitInputReceiver} roots
 *       the synthesized chain at a fresh {@code RImplicitVariable} whose {@code parent} it sets to
 *       {@code evr.parent()} -- wired into the REAL tree -- whenever the disguise sits inside a
 *       rule LAMBDA. The generic walk then reaches the S2 then-item arm on that synthetic node,
 *       reads {@code thenOwnerArgument} off the real parent chain and returns the enclosing PIPE's
 *       cardinality, so EVERY rule-body input-rooted disguise inside a then lambda would read MULTI
 *       whatever its own hops say -- while the render emits {@code MapperS.of(input)}, a SINGLE
 *       root. Stopping at the first non-{@code RFeatureCall} receiver is what keeps the rung
 *       cardinality-true; {@code b2} is its fixture pin.</li>
 *   <li><b>rung B</b> ({@code NavigationHandler} :5174, inside {@code attr != null}). GRSI's head
 *       {@code transactionInformation} IS resolved -- by {@code resolveDisguisedFeature}'s ARM 2,
 *       the implicit-ITEM walk -- and its leaf {@code transactionIdentifier} is single, so the
 *       leaf rung answers false and the arm falls to the alias-root return. But the render roots
 *       the chain at the then-item, which the scope binds to {@code thenArg1}, a
 *       {@code MapperC<ReportableJurisdictionInformation>}. Rung B consults
 *       {@code thenOwnerArgument} + {@code argumentProvesMulti} -- the body of this method's OWN
 *       {@code RImplicitVariable} arm at :5249 -- gated on the head being ITEM-rooted.
 *       <p><b>Identity proof (no synthetic node is minted):</b> the render's B2 synthesis sets the
 *       synthetic root's parent to {@code evr.parent()}, and {@code thenOwnerArgument(start)} walks
 *       from {@code start.parent()}; therefore {@code thenOwnerArgument(evr)} and
 *       {@code thenOwnerArgument(syntheticRoot)} traverse the same nodes in the same order.</li>
 * </ul>
 *
 * <p><b>Green safety is LAW 74, and it is exact.</b> {@code filterSingleNullSafe} and
 * {@code mapSingleToItem} exist ONLY on {@code MapperS} ({@code MapperS.java:87} / {@code :182});
 * {@code filterItemNullSafe} and {@code mapItem} exist ONLY on {@code MapperC}
 * ({@code MapperC.java:125} / {@code :141}). Every site either rung flips renders a MapperC chain
 * (a {@code mapC} leaf hop, or a MapperC-bound {@code thenArg} root), so its pre-flip single-form
 * consumer never compiled -- BOTH carriers are non-compiling TODAY. A byte-correct golden therefore
 * cannot sit in the flip set <i>at the filter/extract selection seats</i>.
 *
 * <p><b>The residual exposure is NOT at those seats, and it is what the corpus controls are for.</b>
 * {@code chainProvesMulti} has ~40 consumers. The one that can move a green byte without any
 * compile signal is the rule-OUTPUT arity channel
 * ({@code ruleOutputProvesMulti -> defaultSpineProvesMulti -> chainProvesMulti(rawLeft)}), which
 * decides a rule's whole {@code ReportFunction} signature. PROBE30-F11r sized the mechanism's reach
 * at <b>332 rows / 88 distinct sites</b> ({@code REnumValueRef} with {@code chainProves=false} over
 * the filter AND extract seats), of which only the 2 carrier sites are band -- <b>86 green sites</b>
 * (85 extract + 1 filter). The largest green families named in the verdict --
 * {@code Create_OptionBarrierLevel1Choice__1}, {@code GetIntrstRate}, the
 * {@code ingest-fpml-confirmation-*} mappers, {@code rule:EventType} -- all live in the drr corpus,
 * which is why control1/control3/control4 are whole-CELL UNION scans (LAW 79) rather than
 * carrier-scoped locks, and why the seat takes the mid-seat whole-matrix checkpoint after this law.
 *
 * <p><b>RED at the pre-law head -- MEASURED at {@code 6c8e1544}, BOTH routes</b>
 * ({@code f30-red-default.log}, {@code f30-red-on.log}): <b>13 run / 5F / 0E / 1 skip</b>
 * (default) and <b>13 / 5F / 0E / 0 skip</b> ({@code -Pir-on}) -- the SAME five both routes,
 * <b>a1, a2, corpus_c1, corpus_c2, corpus_control1</b>, exactly the drafted claim, with
 * {@code corpus_control1} printing {@code in 3 file(s)} against its pinned 1. b1..b4 and
 * {@code corpus_control0} green at RED as claimed. GREEN at the seat head is <b>13/0F</b> on
 * both routes -- this suite carries NO standing failure, so its lane sets need no netting.
 *
 * <p><b>MUTATION LANES -- MEASURED (LAW 82). Both driveable lanes MATCHED their drafted sets
 * exactly; the sets below are transcribed from the archived logs at {@code e223ce19}.</b>
 * <b>⚠ the lane loop runs the DEFAULT profile only</b>, so {@code corpus_control2} is the one
 * skip in each {@code 13 run / 1 skipped} lane and cannot fail in a lane by construction.
 * <ul>
 *   <li><b>m-law3s</b> ({@code f30-mut-m-law3s.log}) = {@code law3s-apply.py --revert}.
 *       <b>MEASURED 13/5F/0E/1S: a1, a2, corpus_c1, corpus_c2, corpus_control1 -- MATCH, no
 *       correction needed.</b> The lane reproduces the RED set exactly, including
 *       {@code corpus_control1}'s residue count of {@code 3 file(s)}: law 3 alone accounts for
 *       the whole of this suite's RED movement.</li>
 *   <li><b>m-law3s-a</b> ({@code f30-mut-m-law3s-a.log}) = {@code law3s-apply.py --revert} then
 *       {@code --rung-a} (rung B severed; rung A + both {@code ReferenceHandler} edits standing).
 *       <b>MEASURED 13/3F/0E/1S: a2, corpus_c2, corpus_control1 -- MATCH, no correction
 *       needed.</b> {@code corpus_control1} drops to {@code 2 file(s)} (from the whole-law
 *       lane's 3), which is the severability claim measured rather than argued: rung A on its
 *       own heals {@code ExecutionTimestampRule} and leaves {@code GetRegimeSpecificIdentifiers}
 *       differing, so the two rungs are separable BY CARRIER exactly as designed. a1 and
 *       {@code corpus_c1} stay green, which is the other half of the same statement.</li>
 * </ul>
 *
 * <p><b>THE THREE LANES THAT WERE DRAFTED AND ARE NOT DRIVEABLE -- DROPPED, not pending.</b>
 * {@code m-law3s-b}, {@code m-law3s-itemroot} and {@code m-law3s-spine} are recorded DROPPED in
 * {@code mut30.py} ("drafted, not driveable with the shipped flags") and <b>no chain has
 * measured them</b>. Their claims are recorded here as UNMEASURED, with the mechanism each
 * needs, so nobody reads them as evidence:
 * <ul>
 *   <li><b>m-law3s-b</b> (rung A severed) -- CLAIMED a1, corpus_c1, corpus_control1 (ETR only).
 *       <b>Not driveable:</b> {@code --revert --rung-b} alone asserts {@code count=0} on
 *       {@code P_HELPER_B} -- helper A's landed text sits INSIDE the region helper B's
 *       replacement covers, so B's anchor only sites once A is removed. The two-step form
 *       {@code m-law3s-a} uses is the pattern that would make it run.</li>
 *   <li><b>m-law3s-itemroot</b> (the two {@code ReferenceHandler} render-gate calls dropped from
 *       {@code itemRootedDisguisedHead}) -- CLAIMED b3 and {@code corpus_control1}. The claim
 *       has a real basis and it is worth recording: <b>b3 was a MEASURED failure of the law's
 *       FIRST cut</b> (it emitted {@code filterItemNullSafe} on the input-rooted disguise at
 *       this seat's first suite run), which is what sent rung B to the render-by-call gate.
 *       That is history, not a lane measurement.</li>
 *   <li><b>m-law3s-spine</b> ({@code synthesizedSpineProvesMulti} replaced by
 *       {@code chainProvesMulti(renderChain, compiler)}) -- CLAIMED b2 at fixture scale plus
 *       {@code corpus_control1}/{@code control3}, and declared UP FRONT as possibly EMPTY on b2:
 *       b2's disguise sits at rule TOP level, where {@code buildImplicitInputReceiver} returns
 *       the {@code RSymbolReference("input")} branch and the S2 arm is never reached. If this
 *       lane is ever driven and b2 comes back green, the fixture set is missing an in-LAMBDA
 *       rule disguise and one must be ADDED rather than the lane written off.</li>
 * </ul>
 */
class DisguisedRenderChainCardinalitySeatTest {

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

    /** Cell A = drr 7.0.0 -- BOTH carriers plus the green in-signature control live here. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = drr 5.61.0 -- the second reach cell. Neither carrier is here (all 8 flip rows are
     * drr 7.x), so this is the EMPTY-DOMAIN guard, and it is chosen because it carries the ten
     * GREEN in-name {@code ExecutionTimestamp} twins ({@code regulation-*-rewrite-trade-rule
     * .rosetta}, {@code recvKind=RImplicitVariable}) and the {@code ingest-fpml-confirmation-*}
     * mapper family that holds a large share of the 86 green sites.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell C = cdm 6.20.6 -- the CROSS-CORPUS guard. The arm is corpus-blind: a disguised
     * {@code a -> b} nav in a cdm function reaches exactly the same rungs. The verdict names no cdm
     * family among the 86, so this cell's law contribution must be EMPTY, and a non-empty one is a
     * finding, not a residue to be pinned away.
     */
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path GOLDEN_C = CELL_C_ROOT.resolve("rosetta-source/src/generated/java");

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

    /**
     * The fixture set. Every rule/function below is a REDUCTION of a measured corpus site, and
     * each of the four decline pins is witness-unique on {@code filterSingleNullSafe} /
     * {@code mapSingleToItem} -- the exact tokens an over-widened flip REMOVES (the runtime carries
     * them only on {@code MapperS}; see {@code MapperS.java:87}/{@code :182}).
     *
     * <ul>
     *   <li><b>a1</b> = the {@code ExecutionTimestamp} shape: a RULE whose body filters a disguised
     *       {@code fromTypeAttr -> MULTI leaf}. Rung A must prove MULTI, so the filter reads
     *       {@code filterItemNullSafe}, the hoisted decl reads {@code MapperC<...>} and the
     *       consequent extract hop reads {@code mapItem} (ETR's second hunk comes free -- once
     *       {@code thenArg0} is a MapperC the extract's {@code rootWrap} is {@code C}).</li>
     *   <li><b>a2</b> = the {@code GetRegimeSpecificIdentifiers} shape: a FUNCTION alias piping a
     *       MULTI list through a {@code then}, whose next {@code then} body filters a disguised
     *       {@code itemFeature -> single leaf}. Rung B must prove MULTI from the PIPE.</li>
     *   <li><b>b1</b> = the {@code MapTechnicalRecordId} pin -- THE in-signature green control. A
     *       FUNCTION whose disguised head is an INPUT and whose leaf is {@code 0..1}: genuinely
     *       single, golden reads {@code filterSingleNullSafe}, and neither rung may fire (rung A
     *       needs an enclosing rule; rung B is declined by {@code resolveDisguisedRootAttribute}
     *       AND by having no enclosing then body -- two independent grounds).</li>
     *   <li><b>b2</b> = rung A's CARDINALITY-TRUTH pin: the a1 shape with a {@code 0..1} leaf. The
     *       rung resolves the same chain but the RFeatureCall arm reads the leaf's true
     *       cardinality, so the verdict must stay false. This is what makes rung A a
     *       cardinality rung rather than a shape rung.</li>
     *   <li><b>b3</b> = rung B's {@code itemRootedDisguisedHead} pin: the a2 shape with the
     *       disguised head being a function INPUT instead of an item feature. The render roots it
     *       at {@code MapperS.of(header)}, a genuinely SINGLE root, so a rung B that dropped the
     *       root discriminator would emit {@code filterItemNullSafe} over a MapperS receiver --
     *       which does not exist. This is the {@code m-law3s-itemroot} lane's witness.</li>
     *   <li><b>b4</b> = rung B's OWNER pin: the a2 shape with the disguised nav inside an
     *       {@code extract} lambda instead of a {@code then} body. An extract's item is
     *       per-element (SINGLE), {@code thenOwnerArgument} returns null for every non-then owner
     *       by construction, and the bytes must not move.</li>
     * </ul>
     *
     * <p>Lexer-safe identifiers: no {@code tag}, {@code single}, {@code label}, {@code body},
     * {@code value}, {@code key}, {@code item} as a declared name.
     */
    private static final String MODEL = """
            namespace census.seat30f11
            version "1.0.0"

            enum Seat30KindEnum:
                EXECUTED
                CONFIRMED

            type Seat30Stamp:
                kindOf Seat30KindEnum (0..1)
                momentAt zonedDateTime (0..1)

            type Seat30Step:
                stamps Seat30Stamp (0..*)
                solo Seat30Stamp (0..1)

            type Seat30Base:
                originStep Seat30Step (0..1)
                fallbackAt zonedDateTime (0..1)

            type Seat30Ident:
                idKind Seat30KindEnum (0..1)
                assigned string (0..1)

            type Seat30TxInfo:
                txIdent Seat30Ident (0..1)

            type Seat30Jur:
                regimeKind Seat30KindEnum (0..1)
                txInfo Seat30TxInfo (0..1)

            type Seat30Info:
                jurisdictions Seat30Jur (0..*)

            type Seat30Header:
                msgIdent Seat30Ident (0..1)

            reporting rule A1RuleInputMultiLeaf from Seat30Base: <"a1 - the ExecutionTimestamp shape: a RULE-from-type disguised head whose LEAF is 0..*. Only the render's by-name input synthesis can see it; the cardinality arm had no rung at all.">
                originStep -> stamps
                    filter kindOf = Seat30KindEnum -> EXECUTED
                    then extract momentAt
                    then only-element
                as "a1"

            reporting rule B2RuleInputSingleLeaf from Seat30Base: <"b2 - rung A's cardinality-truth pin: the SAME shape with a 0..1 leaf. The synthesis resolves, the leaf does not prove multi, the verdict stays false.">
                originStep -> solo
                    filter kindOf = Seat30KindEnum -> EXECUTED
                    then extract momentAt
                    then only-element
                as "b2"

            func Seat30RegimeIds: <"a2 - the GetRegimeSpecificIdentifiers shape">
                inputs:
                    info Seat30Info (1..1)
                    wanted Seat30KindEnum (1..1)
                    idType Seat30KindEnum (1..1)
                output:
                    picked string (0..*)

                alias regimeIdents:
                    info -> jurisdictions
                        then filter regimeKind = wanted
                        then txInfo -> txIdent
                            filter idKind = idType

                add picked:
                    regimeIdents -> assigned

            func Seat30TechnicalId: <"b1 - the MapTechnicalRecordId pin: an INPUT-rooted disguise with a 0..1 leaf, genuinely single">
                inputs:
                    header Seat30Header (0..1)
                    wanted Seat30KindEnum (0..1)
                output:
                    picked string (0..1)

                alias identValue:
                    header -> msgIdent
                        filter idKind = wanted
                        then extract assigned

                set picked:
                    identValue only-element

            func Seat30InputRootedInThen: <"b3 - rung B's itemRootedDisguisedHead pin: an INPUT-rooted disguise INSIDE a then body over a MULTI pipe. The render roots it at MapperS.of(header) - genuinely single.">
                inputs:
                    info Seat30Info (1..1)
                    header Seat30Header (0..1)
                    wanted Seat30KindEnum (1..1)
                    idType Seat30KindEnum (1..1)
                output:
                    picked string (0..*)

                alias headerIdents:
                    info -> jurisdictions
                        then filter regimeKind = wanted
                        then header -> msgIdent
                            filter idKind = idType

                add picked:
                    headerIdents -> assigned

            func Seat30ItemRootedUnderExtract: <"b4 - rung B's OWNER pin: the same item-rooted disguise inside an EXTRACT lambda. An extract's item is per-element, thenOwnerArgument declines every non-then owner.">
                inputs:
                    info Seat30Info (1..1)
                    idType Seat30KindEnum (1..1)
                output:
                    picked string (0..*)

                alias extractIdents:
                    info -> jurisdictions
                        extract txInfo -> txIdent
                            filter idKind = idType

                add picked:
                    extractIdents -> assigned
            """;

    // =========================================================================
    // Part A -- the positive fixtures
    // =========================================================================

    /**
     * a1 -- rung A. Three tokens move together and all three are golden's at the carrier:
     * the filter method, the hoisted decl's wrapper and the consequent extract hop.
     *
     * <p><b>MEASURED, not vacuous.</b> The three {@code contains} fragments were drafted from the
     * carrier's golden text ({@code hunks30/7.0.0_POJO~ExecutionTimestampRule.diff}); the chain's
     * RED leg settled that they discriminate on this fixture too -- <b>a1 FAILS at
     * {@code 6c8e1544} on both routes and under {@code m-law3s}, and passes at the seat head</b>
     * ({@code f30-red-default.log}: {@code "a MULTI disguised leaf must select the MapperC
     * filter"}). It is green under {@code m-law3s-a}, which is what makes it rung A's own pin
     * rather than the law's.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_ruleInputDisguisedMultiLeafProvesMulti() throws IOException {
        String code = collapse(codeOnly(rule("A1RuleInputMultiLeafRule.java")));
        assertTrue(code.contains(".filterItemNullSafe("),
                "a MULTI disguised leaf must select the MapperC filter:\n" + code);
        assertTrue(code.contains("MapperC<Seat30Stamp> thenArg"),
                "the hoisted then-arg must be declared MapperC:\n" + code);
        assertTrue(code.contains(".mapItem("),
                "the consequent extract hop must read mapItem (ETR's second hunk):\n" + code);
        // The REMOVED tokens -- both exist only on MapperS, so their survival is the defect.
        assertTrue(!code.contains(".filterSingleNullSafe("),
                "the MapperS-only filter must be gone:\n" + code);
        assertTrue(!code.contains(".mapSingleToItem("),
                "the MapperS-only extract hop must be gone:\n" + code);
    }

    /**
     * a2 -- rung B. The leaf is SINGLE and the head is SINGLE; the only thing that proves MULTI is
     * the enclosing then-pipe's argument, which is exactly what {@code thenOwnerArgument} +
     * {@code argumentProvesMulti} read. The alias method's own signature is the render-side
     * corroboration: it is {@code MapperC}-returning in both states, which is why the pre-law
     * {@code filterSingleNullSafe} over it never compiled.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_thenPipedItemRootedDisguiseProvesMulti() throws IOException {
        String code = collapse(codeOnly(func("Seat30RegimeIds.java")));
        assertTrue(code.contains(".filterItemNullSafe("),
                "a then-piped item-rooted disguise must select the MapperC filter:\n" + code);
        assertTrue(!code.contains(".filterSingleNullSafe("),
                "the MapperS-only filter must be gone:\n" + code);
    }

    // =========================================================================
    // Part B -- the decline pins (each witness-unique on a token the flip REMOVES)
    // =========================================================================

    /**
     * b1 -- THE in-signature green control, reduced from {@code MapTechnicalRecordId}. Its probe
     * row is character-for-character the carriers' row; only the cardinality differs. Both rungs
     * must decline, and the witness is {@code filterSingleNullSafe} -- the token an over-widened
     * flip removes and replaces with {@code filterItemNullSafe}, which is not a {@code MapperS}
     * method.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_inputRootedSingleLeafKeepsTheSingleFilter() throws IOException {
        String code = collapse(codeOnly(func("Seat30TechnicalId.java")));
        assertTrue(code.contains(".filterSingleNullSafe("),
                "the genuinely-single input-rooted disguise must keep the MapperS filter:\n" + code);
        assertTrue(!code.contains(".filterItemNullSafe("),
                "no MapperC filter may appear on a MapperS receiver:\n" + code);
    }

    /**
     * b2 -- rung A's cardinality-truth pin. {@code synthesizeImplicitInputChainByName} resolves
     * this chain exactly as it resolves a1's; the ONLY difference is that the leaf is {@code 0..1},
     * so the {@code RFeatureCall} arm the rung delegates to answers false. A rung that fired on the
     * SHAPE (a resolvable rule-input disguise) instead of on the CARDINALITY moves this file.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_ruleInputSingleLeafKeepsTheSingleFilter() throws IOException {
        String code = collapse(codeOnly(rule("B2RuleInputSingleLeafRule.java")));
        assertTrue(code.contains(".filterSingleNullSafe("),
                "a 0..1 disguised leaf must keep the MapperS filter:\n" + code);
        assertTrue(!code.contains(".filterItemNullSafe("),
                "rung A must fire on the leaf's cardinality, not on the shape:\n" + code);
    }

    /**
     * b3 -- rung B's render-gate pin, and the {@code m-law3s-itemroot} lane's witness. The
     * disguised head here is a function INPUT, so the render emits
     * {@code MapperS.of(header).<Seat30Ident>map("getMsgIdent", ...)} -- a genuinely SINGLE root,
     * regardless of the MULTI pipe it sits inside.
     *
     * <p><b>This test CAUGHT the first cut and is the reason rung B's gate changed</b> (LAW 82:
     * the mechanism sentence follows the measurement, not the draft). The first
     * {@code itemRootedDisguisedHead} restated the shared resolver's arm order locally -- an
     * {@code resolveDisguisedRootAttribute} test plus a hand-rolled shortcut loop -- and this
     * fixture rendered {@code filterItemNullSafe} anyway at the seat-30 chain's first suite run.
     * The gate now asks {@code ReferenceHandler} the two questions its B2 synthesis asks itself on
     * the same node ({@code disguisedHeadIsClosureParam} /
     * {@code disguisedHeadResolvesInFunctionScope}); the latter covers inputs, the output AND
     * shortcuts and is a strict SUPERSET of the first cut's test, so an input-rooted head can no
     * longer reach the then-pipe read by any path. A restatement can drift from the arm it mirrors;
     * a CALL cannot (LAW 69).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_inputRootedDisguiseInsideAThenBodyIsDeclined() throws IOException {
        String code = collapse(codeOnly(func("Seat30InputRootedInThen.java")));
        // MEASURED (the first cut's blanket negative was NOT witness-unique): the fixture
        // function legitimately renders TWO filters - the MULTI pipe's own
        // `thenArg0.filterItemNullSafe(` over MapperC<Seat30Jur>, and the disguise chain's
        // `MapperS.of(header).<Seat30Ident>map(...getMsgIdent()) .filterSingleNullSafe(`.
        // The pin is therefore scoped to the DISGUISE CHAIN: its getMsgIdent hop must be
        // followed by the single filter and never by the item filter. The blanket
        // no-filterItemNullSafe assert was red pre-law and post-law alike (verified by
        // running b3 at the reverted head) - law 3 is NOT a mover on this shape.
        assertTrue(code.contains("getMsgIdent()) .filterSingleNullSafe("),
                "an INPUT-rooted disguise inside a then body is still single-rooted:\n" + code);
        assertTrue(!code.contains("getMsgIdent()) .filterItemNullSafe("),
                "rung B must not claim a head that resolves in FUNCTION scope -- its render roots"
                + " at MapperS.of(<headName>), which is single whatever pipe encloses it:\n" + code);
        assertTrue(code.contains("MapperS.of(header)"),
                "the disguise root must stay the single input root:\n" + code);
    }

    /**
     * b4 -- rung B's OWNER pin. The same item-rooted disguise inside an {@code extract} lambda:
     * an extract binds its item PER ELEMENT, so the chain over it is single.
     * {@code thenOwnerArgument} returns null for every owner that is not a {@code then} body (see
     * its javadoc: filter/extract/max/min items are per-element), and the bytes must not move.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_itemRootedDisguiseUnderAnExtractOwnerIsDeclined() throws IOException {
        String code = collapse(codeOnly(func("Seat30ItemRootedUnderExtract.java")));
        assertTrue(code.contains(".filterSingleNullSafe("),
                "a per-element extract item must keep the MapperS filter:\n" + code);
        assertTrue(!code.contains(".filterItemNullSafe("),
                "rung B must fire only under a THEN owner:\n" + code);
    }

    // =========================================================================
    // Part C -- the corpus carriers (8 whole-file rows; drr 7.0.0 shown, 7.1/7.2/7.3 identical)
    // =========================================================================

    private static final String ETR =
            "drr/standards/iosco/cde/version1/datetime/reports/ExecutionTimestampRule.java";
    private static final String GRSI =
            "drr/regulation/common/functions/GetRegimeSpecificIdentifiers.java";
    /** VERIFIED against the drr 7.0.0 golden tree at the draft head (the path exists on disk). */
    private static final String MTRI =
            "drr/ingest/fpml/recordkeeping/reportableinfo/functions/MapTechnicalRecordId.java";

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_executionTimestampRuleByteIdentical() throws IOException {
        lockA(ETR);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_getRegimeSpecificIdentifiersByteIdentical() throws IOException {
        lockA(GRSI);
    }

    /**
     * control0 -- golden is the oracle, and it must DISCRIMINATE (prove the instrument can fail).
     * Golden's two carriers carry {@code filterItemNullSafe} and NOT {@code filterSingleNullSafe};
     * golden's in-signature green sibling {@code MapTechnicalRecordId} carries the opposite, and
     * the FORK must emit that sibling byte-identically -- the no-move witness for the whole
     * mechanism.
     *
     * <p>All three paths were verified to exist in the drr 7.0.0 golden tree when this suite was
     * drafted; the {@code Files.isRegularFile} assertion below keeps a later path drift from
     * making the control silently vacuous.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenDiscriminatesTheCarriersFromTheGreenTwin() throws IOException {
        String etr = collapse(Files.readString(GOLDEN_A.resolve(ETR)));
        assertTrue(etr.contains(".filterItemNullSafe("), "golden ETR must carry the MapperC filter");
        assertTrue(!etr.contains(".filterSingleNullSafe("),
                "golden ETR must NOT carry the MapperS filter");
        assertTrue(etr.contains(".mapItem("), "golden ETR must carry the MapperC extract hop");

        String grsi = collapse(Files.readString(GOLDEN_A.resolve(GRSI)));
        assertTrue(grsi.contains(".filterItemNullSafe("),
                "golden GRSI must carry the MapperC filter");

        Path twin = GOLDEN_A.resolve(MTRI);
        assertTrue(Files.isRegularFile(twin),
                "the in-signature green oracle must exist (else this control is vacuous): " + twin);
        String gt = collapse(Files.readString(twin));
        assertTrue(gt.contains(".filterSingleNullSafe("),
                "golden MapTechnicalRecordId must carry the MapperS filter -- it is genuinely single");
        String forkTwin = drrAOutput == null ? null : drrAOutput.get(MTRI);
        assertNotNull(forkTwin, "the fork must emit the in-signature green twin");
        assertEquals(gt, collapse(forkTwin),
                "the in-signature green twin must stay byte-identical (the no-move witness)");
    }

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. The 86 green sites in the
     * mechanism's reach are spread across the cell (85 extract + 1 filter) and no carrier-scoped
     * lock can see them; this can. The scan counts BOTH seats' method pairs, so an over-fire at
     * the extract seat fails here even though neither carrier's hunk is an extract-only move.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellSelectorsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertTrue(drrAOutput.keySet().stream().anyMatch(k -> k.contains("/GetIntrstRate")),
                "a named green family must be INSIDE this scan's domain, else control1 proves"
                + " nothing about it (LAW: a control scans the domain it claims)");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /**
     * control2 -- LAW 77 route parity, per FILE, on both carriers. {@code chainProvesMulti} is
     * consulted from both the legacy handlers and the IR-routed compiler, so route parity is
     * load-bearing here rather than a formality; PROBE30-F11r measured the two routes row-for-row
     * identical (46,878 rows / 14,814 distinct each, zero route-only rows) and this re-proves it on
     * the real IR seams.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForBothCarriers() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(ETR), irOut.get(ETR), "route divergence: " + ETR);
        assertEquals(drrAOutput.get(GRSI), irOut.get(GRSI), "route divergence: " + GRSI);
        assertEquals(drrAOutput.get(MTRI), irOut.get(MTRI), "route divergence: " + MTRI);
    }

    /**
     * control3 -- LAW 79 on drr 5.61.0: the EMPTY-DOMAIN guard. Neither carrier is in this cell, so
     * the law's contribution here must be nil, and the cell carries both the ten GREEN in-name
     * {@code ExecutionTimestamp} twins and the {@code ingest-fpml-confirmation-*} mapper family
     * that holds a large share of the 86 green sites.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellSelectorsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        assertTrue(drrBOutput.keySet().stream()
                        .anyMatch(k -> k.endsWith("/ExecutionTimestampRule.java")),
                "the green in-name twins must be INSIDE this scan's domain (LAW: a control scans"
                + " the domain it claims)");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * control4 -- LAW 79 CROSS-CORPUS, cdm 6.20.6. Both rungs are corpus-blind: rung A fires on any
     * rule from-type disguise with a multi leaf, rung B on any then-piped item-rooted disguise. The
     * verdict's 86-site list names no cdm family, so this cell's contribution must be EMPTY -- and
     * unlike control1/control3 its residue list is a claim to be FALSIFIED, not a set to pin away.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_control4_forkCdm6WholeCellSelectorsEqualGoldenFileByFile() throws IOException {
        assertNotNull(cdmCOutput, "cdm 6.20.6 generation did not run");
        assertEquals(List.of(), cdmCGenErrors,
                "cdm 6.20.6 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(cdmCOutput), scan(readGoldenTree(GOLDEN_C)), cdmCOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM6, DOMAIN_CDM6);
    }

    /**
     * MEASURED at the law's head and transcribed VERBATIM from control1's own printed mismatch
     * set (the domain-pin flow: sentinel -> measured -> pin; never hand-written). One entry, and
     * control1 is GREEN with it at the seat head on both routes.
     *
     * <p>The lanes move it, and that is how they are scored: RED and {@code m-law3s} each print
     * {@code 3 file(s)}, {@code m-law3s-a} prints {@code 2} -- the rung-severability measurement.
     */
    // LAW 81 re-pin (v3.1 flip seat 33, law A.4): the GetBasketConstituents row (fork=[0, 0, 1, 2] golden=[0, 0, 0, 2]) LEFT this list -
    // facet lolDefaultBodyMulti moved this scan's tuple to golden's (the file stays BANDED on its A.3/A.5/A.1/A.2
    // residue; LolDefaultBodyMultiSeatTest pins that residue by name); transcribed from the control print (A4-trip1.log).
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    /** MEASURED EMPTY at the law's head; control3 green with it. See {@link #KNOWN_RESIDUE_DRR7}. */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /**
     * MEASURED EMPTY at the law's head -- and this one is a claim FALSIFIED rather than a set
     * pinned away: the probe verdict named no cdm family, and the measurement agreed.
     */
    private static final List<String> KNOWN_RESIDUE_CDM6 = List.of();

    /**
     * The union domain, MEASURED and pinned (LAW 73) -- a negative value means an UNPINNED call
     * site and {@code assertUnionEqual} fails loudly on it rather than passing vacuously. All
     * three below are the chain's own control output at the law's head. Falsifier: the domain assert itself
     * ({@code assertEquals(expectedDomain, universe.size())}) failing on a later run. It is NOT
     * the harness's "MEASURED domain=" print, which only surfaces when the value is the -1
     * SENTINEL and is therefore never emitted once a real value is pinned. On a run where the
     * RESIDUE assert fails first the domain assert is simply unreachable, so re-derive the
     * domain from the control's own scan rather than waiting for a print.
     */
    private static final int DOMAIN_DRR7 = 1989;

    /** MEASURED at the law's head -- see {@link #DOMAIN_DRR7} for the falsifier. */
    private static final int DOMAIN_DRR561 = 1528;

    /** MEASURED at the law's head -- see {@link #DOMAIN_DRR7} for the falsifier. */
    private static final int DOMAIN_CDM6 = 222;

    /**
     * (T1, T2, T3, T4) per file -- the law's two seats, both directions:
     * <ul>
     *   <li><b>T1</b> {@code .filterItemNullSafe(} -- the FILTER seat's added form (MapperC only).
     *   <li><b>T2</b> {@code .filterSingleNullSafe(} -- the FILTER seat's removed form (MapperS
     *       only). Each carrier moves T1 +1 / T2 -1.
     *   <li><b>T3</b> {@code .mapItem(} -- the EXTRACT seat's added form. Deliberately included
     *       even though only ETR moves it: 85 of the 86 green sites in reach are EXTRACT sites, so
     *       a scan that watched only the filter seat would be blind to the dominant hazard.
     *   <li><b>T4</b> {@code .mapSingleToItem(} -- the EXTRACT seat's removed form.
     * </ul>
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String flat = collapse(codeOnly(e.getValue()));
            int t1 = count(flat, ".filterItemNullSafe(");
            int t2 = count(flat, ".filterSingleNullSafe(");
            int t3 = count(flat, ".mapItem(");
            int t4 = count(flat, ".mapSingleToItem(");
            if (t1 + t2 + t3 + t4 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3, t4});
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

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count) - the seat-29 shape verbatim
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
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-29 SetTerminalRuleMultiSeatTest shape verbatim, plus cell C)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> cdmCOutput;
    private static List<String> cdmCGenErrors;

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
        if (cellCAvailable()) {
            List<String> errs = new ArrayList<>();
            cdmCOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_C_ROOT), errs);
            cdmCGenErrors = errs;
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

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run - corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " - seat 30 law 3: the cardinality half of a disguised nav reads the same"
                + " resolution the render half used.");
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

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat30f11.rosetta");
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
            fixtureOut = render(m -> "census.seat30f11".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
    }

    private static String func(String fileName) throws IOException {
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
            throw new AssertionError("[DisguisedRenderChainCardinalitySeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
