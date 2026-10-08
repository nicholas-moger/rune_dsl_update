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
 * SEAT 28, law D — facet {@code aliasSigWildcardDecl}: <b>a hoisted Mapper local whose initialiser
 * IS an alias call takes the {@code ? extends} that alias's own emitted signature carries</b> — the
 * same verdict {@code FunctionAliasHelper.buildMapperReturnType} computes for the signature the
 * generator emits a few lines above (LAW 69: a call site sharing a law with a declaration CONSULTS
 * it, it does not re-derive it).
 *
 * <p><b>The golden-vs-fork shape — this is a COMPILE bug, not cosmetics.</b> The fork emits the
 * alias signature CORRECTLY and then declares the local without the wildcard, so the assignment is
 * an {@code incompatible types}:
 * <pre>
 * // drr 7.0-7.3 MapLastAvailableSpotPriceToPriceSchedule (thenArg0/1/2)   -- SEAT A
 * protected abstract MapperS&lt;? extends BasicQuotation&gt; lastAvailableSpotPrice(List&lt;? extends BasicQuotation&gt; fpmlQuote);
 * golden:  final MapperS&lt;? extends BasicQuotation&gt; thenArg0 = lastAvailableSpotPrice(fpmlQuote);
 * fork:    final MapperS&lt;BasicQuotation&gt; thenArg0 = lastAvailableSpotPrice(fpmlQuote);
 *
 * // drr 7.0-7.3 ExtractReferenceEntity (line 75, a BLANK FINAL)           -- SEAT E4
 * protected abstract MapperC&lt;? extends EntityIdentifier&gt; referenceEntityByType(ReportableEventBase e);
 * golden:  final MapperC&lt;? extends EntityIdentifier&gt; thenArg;
 *              thenArg = referenceEntityByType(reportableEvent);        // the wildcard-typed arm
 * fork:    final MapperC&lt;EntityIdentifier&gt; thenArg;
 * </pre>
 * <b>LAW 74</b>: both are {@code incompatible types} today — the PRE javac probe must show them and
 * the POST must exit 0. VERIFY-AT-RED: paste the PRE error lines here.
 *
 * <p><b>TWO seats, not one — the seat-27 attribution corrected, and MEASURED.</b>
 * {@code probe27-verdicts.md} pairs {@code ExtractReferenceEntity} with <i>seat A</i> ×14. That
 * pairing is the {@code (cell, fn, elementType)} join the same document flags as
 * over-approximating: the file's seat-A rows are its OTHER decl (golden line 109,
 * {@code final MapperC<EntityIdentifier> thenArg = referenceEntityProduct(...).<EntityIdentifier>mapC(...)}),
 * which is BYTE-GREEN today and must not move. The banded hunk is a BLANK FINAL, which seat A cannot
 * emit at all — it emits {@code final <T> <name> = <value>;}. The seat-28 mega-probe names the real
 * emitter and eliminates every other candidate for that file (identical OFF and ON):
 * {@code PROBE28-E4} 20 rows, {@code PROBE28-C}/{@code E1}/{@code E2}/{@code E5a}/{@code E5b}/{@code E5c}
 * ZERO rows each —
 * <pre>
 * [PROBE28-E4] where=fn:ExtractReferenceEntity site=FER-iteChainCore forced=thenArg name=thenArg
 *              multi=true omitDecl=false declText=MapperC&lt;?…EntityIdentifier?&gt;
 * </pre>
 * So:
 * <ul>
 *   <li><b>seat A</b> {@code CollectionHandler.tryDeepThenHoist} — a level whose VALUE is the bare
 *       alias reference. Carrier: {@code MapLastAvailableSpotPriceToPriceSchedule} ×4 cells.</li>
 *   <li><b>seat E4</b> {@code FunctionExpressionRenderer.appendIteHoistChainCore} — a ladder ARM
 *       that is the bare alias reference. That method has NO wildcard path at all. Carrier:
 *       {@code ExtractReferenceEntity} ×4 cells.</li>
 * </ul>
 * {@code ControlFlowHandler.hoistAsDeepThenMapperLocalOrNull} (the P27D "seat D") has the SAME blind
 * spot — its {@code lubJoin} needs {@code anyDisagree} and so cannot see an AGREEING arm whose own
 * declared type is upper-bounded — but it has ZERO measured carriers for this law and is NOT edited.
 *
 * <p><b>Why the fork disagrees with itself.</b> Seat A's only wildcard arm (#241/L-032) types an
 * alias-leaf level by walking the alias's OWN BODY
 * ({@code NavigationHandler.aliasDerivedThenArgItemType} → {@code recoverThenArgItemRType}), and its
 * javadoc already concedes that walk and the signature walk are DISTINCT. For
 * {@code lastAvailableSpotPrice} (body {@code <input> filter … then only-element}) the body walk
 * dies at the implicit item, so {@code wildcard} stays false while the element still arrives
 * correctly from the #358 receiver walk — exactly the measured row
 * {@code [PROBE27D-seatA] … item=BasicQuotation wildcard=FALSE}, reproduced at
 * {@code [PROBE28-D] … item=BasicQuotation wildcard=false ctw=null cti=null}. Seat E4 types its
 * local from the conditional's own inferred item type only and has no channel through which an arm's
 * DECLARED type could reach the decl.
 *
 * <p><b>The compiled-initialiser channel is EMPTY — do not reach for it (MEASURED).</b>
 * {@code ReferenceHandler}'s alias invocation returns
 * {@code JavaExpression.from(<text>, null, Set.of())}: the expression type of an alias call is
 * literally {@code null}, and seat A compiles its value with a {@code null} expected type so no
 * coercion re-stamps it. {@code [PROBE28-D]} reads {@code ctw=null cti=null} at every carrier site
 * on both routes. {@code FunctionAliasHelper.inferShortcutMapperJavaType} deliberately builds
 * the INVARIANT wrapper ({@code typeUtil.wrap}, "a ternary would capture a wildcard declaration and
 * defeat wrap's inference"). The type layer CAN carry a wildcard
 * ({@code JavaTypeUtil.wrapExtends} / {@code hasWildcardArgument}); nothing in the alias path ever
 * puts one there. The law therefore consults the SIGNATURE VERDICT
 * ({@code FunctionAliasHelper.aliasReturnIsWildcarded}, the very predicate
 * {@code buildMapperReturnType} branches on) through
 * {@code NavigationHandler.aliasSignatureWildcardItemType}, resolved per-site from THIS decl's own
 * initialiser leaf — never a producer/element-name join, and never a read of the rendered signature
 * TEXT.
 *
 * <p><b>THE BLAST RADIUS — read before writing a line of code.</b> 260 drr 7.0.0 golden files /
 * 1,122 sites already carry {@code Mapper[SC]<? extends} and are byte-identical TODAY (110 files in
 * drr 5.61.0). A wildcard emitted where golden has none reddens a green file. The whole-cell UNION
 * controls below are the instrument, and they must be GREEN on BOTH routes and BOTH cells before
 * this law is claimed. Three gates keep the new fire narrow: the byte-locked #241 arm keeps priority
 * ({@code !wildcard}); the level's value must BE the bare alias reference
 * ({@code thenArgElementLeafIsWholeValue} — a nav ABOVE an alias call is excluded, which is
 * precisely what keeps golden's ExtractReferenceEntity line 109 bare, test a3); and the element must
 * agree under the same {@code JavaClass} canonical identity the #241 arm uses.
 *
 * <p><b>MANDATORY same-commit companion.</b> {@code CollectionHandler}'s lambda-channel
 * {@code if (wildcard) { return null; }} declines NOTHING today — 5,284
 * {@code [PROBE27D-seatAdecline]} rows, {@code wildcard=false} and {@code declines=false} at every
 * one. It is dead BECAUSE seat A never computes {@code wildcard=true} on that channel, and it goes
 * LIVE the moment this law lands: a live decline there drops the in-lambda hoist and the whole
 * chain. It is RETIRED in this law's commit and locked by <b>d1</b> below.
 *
 * <p><b>FXLeg — the seat-27 "fifth emitter" is FOUND, and it is seat E4.</b>
 * {@code probe27-verdicts.md} records that {@code PayoutBase} appears in ZERO seat-27 probe lines at
 * ANY tag and that a fifth, unnamed decl emitter owns {@code FXLeg1}/{@code FXLeg2}. PROBE28 names
 * it — both of FXLeg1's hunks come out of {@code appendIteHoistChainCore}:
 * <pre>
 * [PROBE28-E4] fn:FXLeg1 forced=__STMT_HOIST_26665__ multi=false declText=MapperS&lt;?…PayoutBase?&gt;   (law D)
 * [PROBE28-E4] fn:FXLeg1 forced=-                    multi=false declText=MapperS&lt;?…Cashflow?&gt;     (law E)
 * </pre>
 * So the {@code ? extends PayoutBase} hunk is now inside this law's reach — MEASURE whether its
 * wildcard-carrying arm is a bare alias call before claiming it. Either way {@code FXLeg1}/
 * {@code FXLeg2} stay PARTIAL (improved) rows, because their second hunk is law E's. This law claims
 * <b>8</b> whole rows (two basenames × four drr 7.x cells), not 12.
 *
 * <p><b>RED at the pre-seat blob — MEASURED at the chain's RED leg</b> (receipt
 * {@code target/seat28-instruments/run3-c6871ed5/f28-red-default.log}; <b>d1</b> at line 3406):
 * 7F = a1, a2, b2, d1, corpus_c1, corpus_c2, corpus_control1. a3, b1, control0 GREEN in both
 * states. b2 was RED there too — the pre-existing #241 over-fire its own javadoc predicted, which
 * this law's third half repairs; its pre-seat colour is a MEASUREMENT, not an assumption.
 *
 * <p><b>LAW 81</b>: {@code DeclaredThenArgTypeSeatTest} (seat 14 — "the then-level declaration must
 * agree with the type its own value actually has") is this law's direct ancestor; its
 * {@code CONTROL1_PRE_EXISTING_MISMATCHES} javadoc asks for a re-pin when an entry heals — audit it
 * in THIS commit, together with any {@code KNOWN_RESIDUE_*} naming {@code ExtractReferenceEntity},
 * {@code MapLastAvailableSpotPriceToPriceSchedule}, {@code UnderlierProductIdentifier} or
 * {@code StrikePrice}. Content locks to audit: {@code MidChainCondLambdaSextetComposeTest}
 * (ExtractReferenceEntity); the {@code FunctionMaxMinBodyCoercionTest} /
 * {@code MetaFaceShortFormSeatTest} / {@code ThenSeatMultiDefaultTernarySeatTest} trio (FXLeg —
 * expected UNMOVED, which is itself a claim to check).
 *
 * <p><b>LAW 66/76 mutations — THE FOUR PLANNED SEVERANCES, NONE RUN.</b> The seat's chain ran TWO
 * mutations on this suite (lawD2 and lawD3 — see MEASURED MUTATIONS at the foot of this javadoc);
 * neither is one of the four below. Each severance below is <b>NOT RUN — banked to S29</b>, and its
 * failing set would be a claim, not a receipt (LAW 82).
 * <ol>
 *   <li>the seat-A consult block deleted — NOT RUN, banked to S29;</li>
 *   <li>seat E4's {@code declResultType} reverted to {@code resultType} at the decl emit — NOT RUN,
 *       banked to S29;</li>
 *   <li>{@code if (wildcard) { return null; }} restored on the lambda channel — NOT RUN, banked to
 *       S29;</li>
 *   <li>the LAW-69 severance proper — {@code aliasReturnIsWildcarded} hard-coded {@code false} while
 *       {@code buildMapperReturnType} keeps the inline field test — NOT RUN, banked to S29.</li>
 * </ol>
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>the THIRD HALF alone reverted (lawD2-apply --revert: the #241 arm's signature consult) -> b2 (1F - the reverse-defect witness)</li>
 *   <li>the E4 AMENDMENT alone reverted (lawD3-apply --revert: the bare-model scope removed) -> corpus_control3 (1F - the no-carrier-cell control, the amendment's own catch)</li>
 * </ul>
 */
class WildcardLocalDeclSeatTest {

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

    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
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

    /**
     * The fixture mirrors the two carriers shape-for-shape and adds the three negatives.
     *
     * <ul>
     *   <li><b>A1</b> = the {@code MapLastAvailableSpotPriceToPriceSchedule} shape (SEAT A): an alias
     *       whose body is {@code <multi> filter <pred> then only-element} — the shape whose BODY walk
     *       provably declines — consumed as the BARE base of a then-chain in a constructor argument,
     *       twice, exactly as the carrier's {@code thenArg0}/{@code thenArg1}.</li>
     *   <li><b>A2</b> = the {@code ExtractReferenceEntity} shape (SEAT E4): a conditional BASE whose
     *       then-arm is a bare wildcard-signed alias call and whose else-arm is a plain nav of the
     *       SAME element, so no element join is involved and nothing in the ladder's own type
     *       recovery can see the arm's declared bound. Written UNPARENTHESISED exactly as the
     *       carrier writes it, because that is the form measured to bind {@code then last} to the
     *       whole conditional. VERIFY-AT-RED: confirm from the pre-seat render that this fixture
     *       routes to {@code appendIteHoistChainCore} (a blank-final decl) and not to
     *       {@code CollectionHandler}'s conditional-base branch — if it renders an INITIALISED decl
     *       instead, the fixture has drifted from the carrier and must be re-shaped, never the
     *       assert relaxed.</li>
     *   <li><b>A3</b> = the gate-(b) lock and golden's own counter-example: an alias whose body
     *       NAVIGATES through another alias ({@code legsAlias -> marks then filter …}). Its own
     *       signature is wildcarded, but the {@code thenArg} inside its body is INVARIANT — exactly
     *       golden ExtractReferenceEntity's line 65 vs line 109 contrast.</li>
     *   <li><b>B1</b> = the producer-side negative: a BASIC-typed alias element is not a Rosetta
     *       model type, so its signature renders INVARIANT and must never take {@code ? extends}.</li>
     *   <li><b>B2</b> = the decl-side sibling of B1 (the adjacent-defect probe — see its javadoc).</li>
     *   <li><b>D1</b> = the decline-lock witness: the same A1 chain INSIDE an {@code extract}
     *       lambda, so seat A runs on the LAMBDA channel where the retired decline sat.</li>
     * </ul>
     * ({@code tag} is a rune lexer keyword — the basic attributes are {@code note}/{@code notes}.)
     */
    private static final String MODEL = """
            namespace census.seat28d
            version "1.0.0"

            type Mark:
                kind string (0..1)

            type Leg:
                code string (0..1)
                marks Mark (0..*)

            type Holder:
                legs Leg (0..*)
                notes string (0..*)
                note string (0..1)

            type Out:
                code string (0..1)
                other string (0..1)

            func A1CtorArgAliasThenArgWildcard: <"a1 - seat A: a bare wildcard-signed alias at a then-chain base">
                inputs:
                    h Holder (1..1)
                output:
                    out Out (0..1)
                alias picked:
                    h -> legs
                        filter code = "X"
                        then only-element
                set out:
                    Out {
                        code: picked then item -> code,
                        other: picked then item -> code
                    }

            func A2CondBaseAliasArmWildcard: <"a2 - seat E4: a conditional BASE whose then-arm is a bare wildcard-signed alias call">
                inputs:
                    h Holder (1..1)
                output:
                    result Leg (0..1)
                alias byCode:
                    h -> legs
                        filter code = "X"
                set result:
                    if byCode exists
                    then byCode
                    else h -> legs
                        then last

            func A3NavAboveAliasStaysBare: <"a3 - the gate-(b) lock: a NAV above an alias call keeps the invariant decl">
                inputs:
                    h Holder (1..1)
                output:
                    result Mark (0..*)
                alias legsAlias:
                    h -> legs
                alias marksOfLegs:
                    legsAlias -> marks
                        then filter kind = "K"
                add result:
                    marksOfLegs

            func B1BasicAliasSignatureInvariant: <"b1 - a BASIC-typed alias signature is INVARIANT">
                inputs:
                    h Holder (1..1)
                output:
                    result string (0..1)
                alias noteAlias:
                    h -> note
                set result:
                    noteAlias

            func B2BasicAliasThenArgInvariant: <"b2 - the DECL half of b1: a basic-element alias's then-arg stays invariant">
                inputs:
                    h Holder (1..1)
                output:
                    out Out (0..1)
                alias someNotes:
                    h -> notes
                set out:
                    Out {
                        code: someNotes then first
                    }

            func D1InLambdaAliasThenArgWildcard: <"d1 - the decline-lock witness for the retired lambda-channel wildcard decline">
                inputs:
                    h Holder (1..1)
                output:
                    out string (0..*)
                alias picked:
                    h -> legs
                        filter code = "X"
                        then only-element
                add out:
                    h -> legs
                        extract [ picked then item -> code ]
            """;

    // =========================================================================
    // Part A — the flip's own carriers, in fixture form
    // =========================================================================

    /**
     * a1 (SEAT A) — the two halves must be textually identical modulo the wrapper: whatever the
     * alias SIGNATURE declares, the {@code thenArg} initialised from a call to it declares. The
     * negative token is the one the flip REMOVES ({@code final MapperS<Leg> thenArg…}), asserted on
     * {@code codeOnly} so a javadoc can never satisfy it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_bareAliasThenArgDeclKeepsSignatureWildcard() throws IOException {
        String out = fn("A1CtorArgAliasThenArgWildcard.java");
        assertContains(out, "MapperS<? extends Leg> picked(");
        assertTrue(declLines(out, "MapperS<? extends Leg>").size() >= 2,
                "both ctor-arg then-args must declare the alias signature's wildcard:\n" + out);
        assertEquals(List.of(), declLines(codeOnly(out), "MapperS<Leg>"),
                "no then-arg may erase the alias signature's wildcard:\n" + out);
    }

    /**
     * a2 (SEAT E4) — the blank-final Mapper decl over arms that AGREE on the element. No element
     * join is in play, so the wildcard can only come from the arm's own alias signature. The decl is
     * a BLANK FINAL: it ends with {@code ;} and carries no {@code =} — which is itself the seat
     * discriminator, since the seat-A emitter can only produce an initialised decl.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_condArmAliasBlankFinalKeepsWildcardWithoutLubDisagreement() throws IOException {
        String out = fn("A2CondBaseAliasArmWildcard.java");
        assertContains(out, "MapperC<? extends Leg> byCode(");
        List<String> wild = declLines(out, "MapperC<? extends Leg>");
        assertTrue(wild.stream().anyMatch(s -> s.endsWith(";") && !s.contains("=")),
                "the conditional-base hoist must be a wildcard-typed BLANK FINAL:\n" + out);
        assertEquals(List.of(), declLines(codeOnly(out), "MapperC<Leg>"),
                "arms that AGREE on a wildcard-signed alias still owe the wildcard:\n" + out);
    }

    /**
     * a3 — the gate-(b) lock, and the reason the 260 already-green wildcard files stay green: a
     * level whose value NAVIGATES through an alias produces the navigated attribute's OWN invariant
     * element. golden ExtractReferenceEntity states both halves in one file — the signature at line
     * 65 is {@code MapperC<? extends EntityIdentifier>}, the decl at line 109 is the bare
     * {@code MapperC<EntityIdentifier>}. GREEN before and after (measured at both heads); mutation
     * (i) would prove it, and is NOT RUN — banked to S29.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_navigationAboveAnAliasCallKeepsTheInvariantDecl() throws IOException {
        String out = fn("A3NavAboveAliasStaysBare.java");
        assertContains(out, "MapperC<? extends Mark> marksOfLegs(");
        assertTrue(!declLines(out, "MapperC<Mark>").isEmpty(),
                "the alias body's own then-arg is the NAVIGATED element and stays invariant:\n" + out);
        assertEquals(List.of(), declLines(codeOnly(out), "MapperC<? extends Mark>"),
                "a nav above an alias call must never take the alias's wildcard onto a local:\n" + out);
    }

    /**
     * b1 — the producer-side negative. {@code buildMapperReturnType} emits {@code ? extends} for a
     * Rosetta MODEL element and the INVARIANT form for everything else; a {@code string} alias is
     * everything else. Witness-unique: {@code MapperS<? extends String>} is a token the flip must
     * never add anywhere; mutation (iv) — {@code aliasReturnIsWildcarded} forced true — would add
     * exactly it, and is NOT RUN — banked to S29.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_basicTypedAliasSignatureStaysInvariant() throws IOException {
        String out = fn("B1BasicAliasSignatureInvariant.java");
        assertContains(out, "MapperS<String> noteAlias(");
        assertTrue(!codeOnly(out).contains("? extends String"),
                "a basic-typed alias must NEVER take the wildcard:\n" + out);
    }

    /**
     * b2 — the DECL half of b1, and this seat's ADJACENT-DEFECT PROBE. The #241 arm sets
     * {@code wildcard = true} for ANY alias element its body walk resolves, including a BASIC one,
     * while the producer emits the wildcard only for {@code isRosettaModelType}. The same law is
     * already written down inside {@code NavigationHandler.condLadderFirstModelArmItem} — "the
     * signature emits the wildcard ONLY for isRosettaModelType elements (a basic-element alias
     * renders INVARIANT), so a basic-typed arm must not admit" — but it is applied ONLY on the
     * conditional-body arm, not on the main body walk.
     *
     * <p>VERIFY-AT-RED, and do NOT weaken this test to make it pass: run it at the PRE-SEAT head
     * first. If it is already RED there, that is a PRE-EXISTING #241 over-fire in the opposite
     * direction, and the seat's LAW-69 completion is to gate the #241 arm's {@code wildcard = true}
     * on the SAME {@code aliasReturnIsWildcarded} predicate — a third half of the same law, measured
     * by the rings like any other.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_basicTypedAliasThenArgDeclStaysInvariant() throws IOException {
        String out = fn("B2BasicAliasThenArgInvariant.java");
        assertContains(out, "MapperC<String> someNotes(");
        assertTrue(!codeOnly(out).contains("? extends String"),
                "a basic-element alias owes NO wildcard to its signature or to any local:\n" + out);
    }

    /**
     * d1 — THE DECLINE-LOCK WITNESS for the retired
     * {@code CollectionHandler} lambda-channel {@code if (wildcard) { return null; }} (LAW 76
     * witness-uniqueness). Three states, three different outputs:
     * <ul>
     *   <li>pre-seat: the level hoists with an INVARIANT in-lambda decl (wildcard is false there, at
     *       all 5,284 measured sites);</li>
     *   <li>law D landed WITHOUT the retirement: the decline fires and the level hoists NOTHING —
     *       the in-lambda {@code thenArg} decl disappears entirely. This state was never produced:
     *       it is severance 3, NOT RUN — banked to S29;</li>
     *   <li>law D landed WITH the retirement: the same decl, now wildcard-typed.</li>
     * </ul>
     * So the asserted token is one the flip ADDS ({@code MapperS<? extends Leg>} on a {@code final}
     * line naming a then-arg) plus one it REMOVES ({@code MapperS<Leg>}). <b>MEASURED non-vacuity</b>:
     * d1 FAILED at the chain's RED leg — receipt
     * {@code target/seat28-instruments/run3-c6871ed5/f28-red-default.log} line 3406 — so the
     * pre-seat state really does fail it. Mutation (iii), which would remove the decl outright, is
     * NOT RUN — banked to S29.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void d1_inLambdaAliasThenArgHoistsWithWildcardAfterTheDeclineRetires() throws IOException {
        String out = fn("D1InLambdaAliasThenArgWildcard.java");
        assertContains(out, "MapperS<? extends Leg> picked(");
        List<String> wild = declLines(out, "MapperS<? extends Leg>");
        assertTrue(wild.stream().anyMatch(s -> s.contains("thenArg")),
                "the in-lambda level must STILL hoist its then-arg decl (the retired decline would"
                        + " drop it) and must carry the alias signature's wildcard:\n" + out);
        assertEquals(List.of(), declLines(codeOnly(out), "MapperS<Leg>"),
                "the in-lambda decl must not keep the invariant pre-seat form:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carriers (8 whole-file rows: 2 basenames x drr 7.0-7.3)
    // =========================================================================

    private static final String EXTRACT_REFERENCE_ENTITY =
            "drr/regulation/common/functions/ExtractReferenceEntity.java";
    private static final String MAP_LAST_SPOT =
            "drr/ingest/fpml/recordkeeping/reportableinfo/functions/"
            + "MapLastAvailableSpotPriceToPriceSchedule.java";

    /** c1 — the SEAT D carrier. Its ONLY diff today is line 75, the blank final. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_extractReferenceEntityByteIdentical() throws IOException {
        lockA(EXTRACT_REFERENCE_ENTITY);
    }

    /** c2 — the SEAT A carrier. Its ONLY diffs today are the three thenArg0/1/2 decls. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_mapLastAvailableSpotPriceByteIdentical() throws IOException {
        lockA(MAP_LAST_SPOT);
    }

    /**
     * control0 — golden is the oracle, and it states BOTH halves of the law in the same two files:
     * the signature and the local agree character for character where the local IS initialised from
     * the call, and DISAGREE (invariant local) where the local is initialised from a navigation
     * THROUGH the call. Both readings must hold or the law as written is wrong.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenSignatureAndDeclAgreeExactlyWhereTheValueIsTheCall()
            throws IOException {
        String ere = Files.readString(GOLDEN_A.resolve(EXTRACT_REFERENCE_ENTITY));
        assertTrue(ere.contains("MapperC<? extends EntityIdentifier> referenceEntityByType("),
                "golden's alias signature is wildcard-typed");
        assertTrue(ere.contains("final MapperC<? extends EntityIdentifier> thenArg;"),
                "golden's blank-final DECL carries the same wildcard — the two halves of one law");
        assertTrue(ere.contains("final MapperC<EntityIdentifier> thenArg = "
                        + "referenceEntityProduct(reportableEvent)"),
                "golden's OTHER decl — a navigation THROUGH the alias — is INVARIANT; this is the"
                        + " gate-(b) boundary and it must not move");
        String mls = Files.readString(GOLDEN_A.resolve(MAP_LAST_SPOT));
        assertTrue(mls.contains("MapperS<? extends BasicQuotation> lastAvailableSpotPrice("),
                "golden's alias signature is wildcard-typed");
        assertTrue(mls.contains("final MapperS<? extends BasicQuotation> thenArg0 = "
                        + "lastAvailableSpotPrice(fpmlQuote);"),
                "golden's DECL carries the same wildcard");
        assertTrue(mls.contains("final MapperC<BasicQuotation> thenArg = MapperC.<BasicQuotation>of("),
                "golden's in-alias-body decl, whose value is NOT an alias call, is INVARIANT");
    }

    /**
     * control1 — LAW 79, THE INSTRUMENT FOR THIS LAW'S BLAST RADIUS. Over the UNION of golden and
     * fork token-bearing files in drr 7.0.0, the (wildcard local, invariant Mapper local, wildcard
     * signature) triple must equal golden's FILE FOR FILE beyond the NAMED residue. T1 and T2 are
     * complements, so an over-fire cannot hide behind a coincidental total; T3 is the PRODUCER half
     * and must not move at all.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellWildcardSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, DOMAIN_DRR7);
    }

    /**
     * The NAMED residue of drr 7.0.0 (LAW 73: pin the SET, not the count) — files whose triple still
     * differs from golden's AFTER law D. VERIFY-AT-RED: fill from the first GREEN run.
     * {@code UnderlierProductIdentifier}, {@code StrikePrice} and {@code QuantityUnitOfMeasure} are
     * expected here as partial rows that only IMPROVE; {@code FXLeg1}/{@code FXLeg2} are expected
     * here UNMOVED (their emitter is out of this law's reach — see the class javadoc) and leave only
     * when the fifth emitter is found.
     *
     * <p><b>RE-MEASURED at the seat-30 chain head {@code e223ce19}</b> — 9 entries, transcribed
     * VERBATIM from this control's own failing print (LAW 81), down from 11. Both departures keep a
     * non-zero GOLDEN triple, so both stay inside the union domain and {@code DOMAIN_DRR7} is
     * UNMOVED at 1862.
     *
     * <p><b>RE-MEASURED at the seat-31 chain head {@code f2a4d5c0}</b> — 6 entries, transcribed
     * VERBATIM from this control's own failing print (LAW 81), down from 9: three rows LEFT (each
     * noted inline). All three departures keep a non-zero GOLDEN triple, so all three stay inside
     * the union domain and {@code DOMAIN_DRR7} is UNMOVED at 1862.
     */
    private static final List<String> KNOWN_RESIDUE_7 = List.of(
            // LAW 81 re-pin (v3.1 flip seat 33, law A.4): the GetBasketConstituents row (fork=[1, 7, 6] golden=[1, 6, 6]) LEFT this list -
            // facet lolDefaultBodyMulti moved this scan's tuple to golden's (the file stays BANDED on its A.3/A.5/A.1/A.2
            // residue; LolDefaultBodyMultiSeatTest pins that residue by name); transcribed from the control print (A4-trip1.log).
            // UnderlierBasketIdentifier.java (was fork=[0, 0, 0] golden=[0, 3, 0]) left this list at
            // seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so golden's three hoisted locals (T2) now render.
            // UnderlierProductIdentifier.java (was fork=[1, 3, 36] golden=[2, 2, 36]) left this list at
            // seat 31: law 2b (aliasSigWildcardDecl at the CFH deep-then seat - agreeing arms that are bare calls to a wildcard-signed
            // alias force the `? extends` DECL) - the cdsProductID thenArg0 decl is now the `? extends` form, so the
            // wildcard-local count (T1) rises 1 -> 2 and the bare-local count (T2) falls 3 -> 2, both
            // equal to golden's. The file itself stays BANDED (26 diff lines, the F1 residue).
            // CountryOfCounterparty2Rule and SmallScaleBuySideEntityIndicatorRule left this list at
            // seat 29 (law 3, rulePathFilterLolBinding) — healed WHOLE, measured in the seat's chain.
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[0, 2, 0] golden=[0, 3, 0]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary) healed it WHOLE - a cross-law tripwire D.2's list missed, caught by law C.3's
            // run (C3-trip1a.log) and transcribed from this control's own print.
            // GetUnderlierProductIdentifierLeg1.java (was fork=[1, 3, 2] golden=[1, 2, 2]) left this
            // list at seat 30: law 5 (thenArgDeclKindFromCompiled — the then-arg DECL reads the
            // compiled stamp instead of re-reading S) healed it WHOLE in all four drr 7.x cells, so
            // the surplus wildcard local (T2) is gone.
            // CommodityQuantityWithFrequency.java (was fork=[0, 6, 0] golden=[0, 7, 0]) left this
            // list at seat 30: law 4 (the d=4 function-level all-or-nothing decline at the RThenExpr
            // inline fallback, CollectionHandler.tryDeepThenHoist) healed it WHOLE in all four
            // drr 7.x cells, so its seventh wildcard local now renders.
            // CustomBasketCodeRule.java (was fork=[0, 3, 0] golden=[0, 7, 0]) left this list at seat 31:
            // law 4b (choiceSwitchLambdaOptionGetter - the in-lambda CHOICE switch lowers its case guards to option-getter
            // null-tests with MAPPER-typed case locals, the live-bound naming rung hoisted) healed it WHOLE in drr 7.0.0
            // ONLY (7.1-7.3 IMPROVED 50 -> 49 lines and stay banded on a DIFFERENT mechanism - the in-lambda nested
            // then-chain hoist, S32's), so golden's four MapperS-typed case locals (T2) now render alongside the three
            // the fork already had.
            // the StrikePrice row (fork=[0, 1, 4] golden=[1, 0, 4]) LEFT this list at seat 33: law D.12
            // (iteDeclWildcardFromDisagreeingArms) - the wildcard-local count (T1) rises 0 -> 1 and the
            // invariant-local count (T2) falls 1 -> 0, both equal to golden's; the file is WHOLE in all four
            // drr 7.x cells (StrikePriceIteLadderWholeSeatTest); transcribed from the control print (D12-trip1.log).
            // the QuantityUnitOfMeasure row (fork=[0, 7, 0] golden=[0, 10, 0]) LEFT this list: law B.1 (setSeatNestedValueThenTogetherHoist,
            // the k>0 restructure window) healed this scan's token set to golden's in all four drr 7.x cells -
            // the file itself stays BANDED (its close is B.3 + B.24); transcribed from this control's own print
            // (B1-trip1.log), a pure row removal (was == expected minus it).
            // the TotalNotionalQuantity row (fork=[0, 2, 0] golden=[0, 5, 0]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /** control2 — LAW 77 route parity for both carriers, on the REAL IR seams. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(EXTRACT_REFERENCE_ENTITY), irOut.get(EXTRACT_REFERENCE_ENTITY),
                "route divergence: " + EXTRACT_REFERENCE_ENTITY);
        assertEquals(drrAOutput.get(MAP_LAST_SPOT), irOut.get(MAP_LAST_SPOT),
                "route divergence: " + MAP_LAST_SPOT);
    }

    /**
     * control3 — LAW 79 on the SECOND cell (drr 5.61.0, ~110 golden wildcard files). This cell has
     * ZERO banded rows for the law, so its whole job is to prove the fire does not spread: its
     * residue must stay EMPTY.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellWildcardSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_561, DOMAIN_DRR561);
    }

    /** The NAMED residue of drr 5.61.0 — see {@link #KNOWN_RESIDUE_7}. VERIFY-AT-RED. */
    private static final List<String> KNOWN_RESIDUE_561 = List.of(
            // the four cftc Notional* rows LEFT this list at seat 32: law C.3 (ruleThenArmLadderNestedTreeAdmit) healed
            // NotionalAmountLeg1/2 + NotionalCurrencyLeg2 WHOLE in drr 5.61.0 and IMPROVED NotionalCurrencyLeg1 to a tuple this scan reads as golden's; transcribed from this control's own print (C3-trip1*.log).
            );  // LAW 81 (seat 33, law F.B): the NotionalLeg2Rule row LEFT - the file is WHOLE (FB-trip1.log print)

    /** VERIFY-AT-RED: the drr 7.0.0 union domain — golden union fork token-bearing files. */
    private static final int DOMAIN_DRR7 = 1862;

    /** VERIFY-AT-RED: the drr 5.61.0 union domain. */
    private static final int DOMAIN_DRR561 = 1219;

    /**
     * (T1, T2, T3) per file:
     * <ul>
     *   <li>T1 — LOCAL declarations carrying {@code Mapper[SC]<? extends } (the token this law adds);</li>
     *   <li>T2 — LOCAL declarations carrying an INVARIANT {@code Mapper[SC]<} (T1's complement, so a
     *       decl that flips from invariant to wildcard moves BOTH counters and cannot cancel out);</li>
     *   <li>T3 — every OTHER line carrying {@code Mapper[SC]<? extends } — the alias/abstract-method
     *       SIGNATURES, i.e. the PRODUCER half, which this law must leave untouched.</li>
     * </ul>
     * Counted per LINE (a signature carrying two {@code ? extends} counts once) so the measure is
     * stable under formatting. {@code codeOnly} strips comments and string literals first, so a
     * javadoc or a label can never contribute.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            for (String line : code.split("\n")) {
                String s = line.trim();
                boolean wild = s.contains("MapperS<? extends ") || s.contains("MapperC<? extends ");
                boolean invariant = !wild
                        && (s.contains("MapperS<") || s.contains("MapperC<"));
                boolean decl = s.startsWith("final ");
                if (decl && wild) {
                    t1++;
                } else if (decl && invariant) {
                    t2++;
                } else if (wild) {
                    t3++;
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

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        // Scoped to the files this harness emits (the seat-28 correction class).
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
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    /**
     * Every trimmed {@code final …} declaration line in {@code java} whose type text contains
     * {@code typeText}. Used instead of a bare {@code contains} so the asserts survive the
     * {@code thenArg} / {@code thenArg0} / {@code _thenArg1} sentinel numbering without ever
     * matching a comment (call it on {@code codeOnly(...)} for the NEGATIVE side).
     */
    private static List<String> declLines(String java, String typeText) {
        List<String> hits = new ArrayList<>();
        for (String line : java.split("\n")) {
            String s = line.trim();
            if (s.startsWith("final ") && s.contains(typeText + " ")) {
                hits.add(s);
            }
        }
        return hits;
    }

    // =========================================================================
    // Harness (the seat-27 suite shape verbatim)
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 28 law D: a hoisted Mapper local whose initialiser IS an alias"
                + " call declares that alias signature's own `? extends`.");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    /** Strip line and block comments plus string literals so a javadoc or label never counts as code. */
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat28d.rosetta");
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
            fixtureOut = render(m -> "census.seat28d".equals(m.namespace()));
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
            throw new AssertionError("[WildcardLocalDeclSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
