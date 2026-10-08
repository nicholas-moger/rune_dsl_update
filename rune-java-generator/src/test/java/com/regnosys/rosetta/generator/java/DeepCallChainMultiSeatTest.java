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
 * SEAT 29, law 5 — facet {@code deepCallChainProvesMulti}: <b>a DEEP-arrow ({@code ->>}) chain
 * whose hop renders {@code mapC} proves MULTI, so the MapperC ladder rung stops wrapping it
 * {@code MapperC.of(…)}</b>. {@code NavigationHandler.chainProvesMulti} — the gm-aware MONOTONE
 * multi-proof overlay — carries arms for {@code RFeatureCall}, the disguised
 * {@code REnumValueRef}, {@code RImplicitVariable}, filter/extract/then, conditionals,
 * {@code default}, list literals, list ops and {@code RSymbolReference}, and had <b>no
 * {@code RDeepFeatureCall} arm at all</b> ({@code RDeepFeatureCall extends RExpression}, not
 * {@code RFeatureCall}). A deep-headed arm therefore fell to the tail's conservative
 * {@code false} = SINGLE.
 *
 * <p><b>The golden vs fork shape</b> — drr 7.0.0 {@code GetBasketConstituents} (one hunk of six):
 * <pre>
 * fork:    return MapperC.of(item.&lt;Index&gt;map("getIndex", …)
 *              .&lt;AssetIdentifier&gt;mapC("chooseIdentifier", index -&gt; indexDeepPathUtil.chooseIdentifier(index)));
 * golden:  return item.&lt;Index&gt;map("getIndex", …)
 *              .&lt;AssetIdentifier&gt;mapC("chooseIdentifier", index -&gt; indexDeepPathUtil.chooseIdentifier(index));
 * </pre>
 *
 * <p><b>Why the AST side and not the type side.</b> The wrap seat
 * ({@code CollectionHandler.wrapSingleArmMapperCOf}, reached from {@code renderLadderLevel}'s
 * mapperC rung) reads the compiled type FIRST and the AST verdict only as the fallback — but a
 * deep call's stamped type is the hop's ELEMENT type ({@code metaNavResultType}), never a Mapper,
 * so the AST verdict decides EVERY deep arm at that seat: {@code decidedBy=astChainProvesMulti}
 * on 16 of 16 measured rows. Making the deep chain stamp a {@code MapperC} instead would flip
 * {@code decidedBy} for every arm in every ladder — a far larger blast radius.
 *
 * <p><b>LAW 69</b>: the arm reads the hop's cardinality from the SAME two calls
 * {@code handle(RDeepFeatureCall)} makes to RENDER it — {@code resolveDeepFeature} +
 * {@code resolveDeepMapMethod}, the #348 lockstep law — so the arity read and the emitted
 * {@code .mapC(} cannot disagree. {@code resolveDeepMapMethod} goes STATIC in the same commit
 * (modifier-only, stateless; the PR #344 {@code resolveDeepFeature} precedent verbatim). The
 * shape is the seat-28 {@code chainMapperCRootRungs} rung R1 one walk over.
 *
 * <p><b>The probe verdict this law answers (LAW 75).</b> {@code PROBE29-F16w} at the wrap seat,
 * both routes, 25 cells, <b>route-IDENTICAL row for row</b>:
 * {@code armKind=RDeepFeatureCall} is <b>16 rows / 16 files, ALL BAND, ZERO green</b> —
 * every one of them {@code compiled=null|x decidedBy=astChainProvesMulti chainProvesMulti=false
 * wrapped=true wrapKind=mapperCOf}. Twelve are the CARRIERS (drr 7.0.0/7.1.0/7.2.0/7.3.0 ×
 * {@code GetBasketConstituents}, {@code GetUnderlierProductIdentifierLeg1},
 * {@code UnderlierProductIdentifier}; deep hop {@code chooseIdentifier} → {@code mapC}). Four are
 * the in-band NEGATIVE CONTROL (the same four cells' {@code NameOfTheUnderlyingIndexRule}; deep
 * hop {@code chooseName} → {@code map}), whose wrap is <b>byte-CORRECT in golden</b>
 * ({@code drr/regulation/common/trade/underlier/reports/NameOfTheUnderlyingIndexRule.java:113}).
 * <b>The {@code mapC} refinement is therefore LOAD-BEARING</b>: an {@code armKind}-only law would
 * unwrap those four — a 4-cell regression. (Their file is in the band for a DIFFERENT arm, an
 * {@code RToStringExpr} whose missing wrap reads {@code decidedBy=mapperCPrefix}.)
 *
 * <p><b>LAW 80 — what this law does NOT do.</b> It heals <b>ZERO whole files on its own</b>
 * (−12 diff lines, one {@code return} per carrier file); the band stays 152. Its whole-file value
 * is JOINT: it closes {@code GetUnderlierProductIdentifierLeg1}'s wrap hunk (the file's other hunk
 * is an F11 receiver-cardinality family), {@code GetBasketConstituents}' wrap hunk (that file
 * stays 6-family) and {@code UnderlierProductIdentifier}'s wrap hunk (that file stays 4-family).
 * {@code corpus_c1} is therefore a LINE-level lock, not a byte-whole lock.
 *
 * <p><b>The half that was measured away.</b> Seat 29 shipped this arm with a SECOND term — the
 * receiver recursion {@code chainProvesMulti(dfc.receiver(), compiler)}, the {@code RFeatureCall}
 * arm's own upstream-propagation law carried to the deep node (receiver multi ⇒ chain multi) —
 * and built mutation {@code m-law5ii} to sever it so the chain could ADJUDICATE it rather than
 * assume it. The chain MEASURED IT EMPTY, twice (below), and <b>seat 30 removed it</b>: the arm
 * is now the single expression {@code return "mapC".equals(resolveDeepMapMethod(
 * resolveDeepFeature(dfc, compiler)))}. All twelve carriers flip on the {@code mapC} term alone;
 * a2/b2 remain its minimal pair and b2 remains the DECLINE lock, now satisfied by construction.
 *
 * <p><b>RED at the pre-law head — MEASURED at the seat-29 chain's RED leg ({@code fa277393},
 * identical on BOTH routes)</b>: a1, a2, corpus_c1, corpus_control1.
 * b1, b2, b3, corpus_c1b and corpus_control3 are GREEN in BOTH states — b1/b2 the witness-unique
 * decline locks (they key on {@code MapperC.of(} being PRESENT, the token the flip REMOVES),
 * corpus_c1b the in-band negative control's line lock.
 *
 * <p><b>MEASURED MUTATIONS (LAW 82 — the seat-29 chain's suite-lane loop at {@code 687c8feb};
 * each lane = the named apply-script variant applied, the suite run, restored; every set below is
 * the RECORDED failing set from the f29-mut logs):</b>
 * <ul>
 *   <li><b>m-law5</b> the whole law reverted ({@code law5-apply29.py --revert}) → MEASURED a1, a2,
 *       corpus_c1, corpus_control1 (4F — exactly the drafted claim); b1/b2/b3/corpus_c1b unmoved
 *       (they are the wrap-PRESENT pins)</li>
 *   <li><b>m-law5i</b> (the {@code f29-mut-m-law5i} log) the {@code mapC} refinement severed — the arm becomes {@code armKind}-only
 *       ({@code law5-apply29.py --mut-armonly}) → MEASURED b1, b2, corpus_c1b, corpus_control1
 *       (4F); a1/a2/corpus_c1 unmoved (already unwrapped). The drafted claim ALSO named
 *       corpus_control3 — MEASURED GREEN: the mutant's over-unwrap reach is drr 7.x-only at this
 *       corpus (the drr 5.61.0 whole-cell scan holds), consistent with this class javadoc's own
 *       green-in-both-states line for control3; the drafted entry was the error.</li>
 *   <li><b>m-law5ii</b> (the {@code f29-mut-m-law5ii} log) the receiver recursion severed ({@code law5-apply29.py --mut-norecurse})
 *       → MEASURED EMPTY (11 run / 0F — the claimed zero, ADJUDICATED), and reproduced at the
 *       chain's second head {@code 219fc263}. <b>SPENT — the lane is RETIRED at seat 30</b>, which
 *       acted on the adjudication and deleted the term; its anchor no longer exists, and m-law5 /
 *       m-law5i must be re-sited to the collapsed arm before they can run again. What the lane
 *       bought: the recursion carried nothing in this corpus, so the arm is one term, not two.
 *       (The 1 skip in every lane's {@code 11 run / 1 skipped} is
 *       {@code corpus_control2} — {@code @EnabledIf("cellAAndIrProviderAvailable")}; the IR-route
 *       control is exercised by the {@code -Pir-on} leg and by the both-route matrix digest, not
 *       by the default-profile mutation loop.)</li>
 * </ul>
 *
 * <p><b>SEAT 30 — MEASURED, and what the measurement says about this class
 * ({@code chain-all30.ps1} @ {@code e223ce19}; {@code f30-red-default.log},
 * {@code f30-red-on.log}, {@code f30-green-default.log}, {@code f30-green-on.log}).</b>
 * <ul>
 *   <li><b>RED at {@code 6c8e1544}: 11 run / 0F / 0E / 1 skip (default) and 11/0/0/0
 *       ({@code -Pir-on}) — ZERO failures, both routes.</b> This is the ONLY one of seat 30's
 *       ten suites with an EMPTY RED set. GREEN is the same 11/0F. <b>That is not a
 *       failing-first gap in law 5</b> — law 5's failing-first evidence is the seat-29 RED leg
 *       at {@code fa277393} recorded above (a1, a2, corpus_c1, corpus_control1, both routes).
 *       It is the seat-30 change this class documents having <b>no test-visible surface</b>:
 *       seat 30's edit here is the DELETION of the receiver-recursion term, and seat 29's
 *       {@code m-law5ii} already measured that term EMPTY (11 run / 0F, twice). A byte-neutral
 *       deletion moves no test, so restoring it at RED moves no test either. The 0F is the
 *       byte-neutrality RE-CONFIRMED at a third head, and it is exactly what the adjudication
 *       predicted.</li>
 *   <li><b>NO seat-30 mutation lane exists for this suite.</b> {@code m-law5},
 *       {@code m-law5i} and {@code m-law5ii} are the SEAT-29 lanes recorded above; all three
 *       are DROPPED at seat 30 ("not driveable with the shipped flags") because the anchor
 *       they key on was deleted. This class therefore contributes <b>zero measured lanes to
 *       seat 30's lane tally</b>, and says so rather than restating seat 29's sets as if the
 *       seat-30 chain had produced them. Restoring lane coverage means re-siting
 *       {@code m-law5} / {@code m-law5i} to the collapsed single-term arm.</li>
 *   <li><b>The seat-30 diff to this file is COMMENT-ONLY</b> — 27 added / 17 removed lines,
 *       every one of them inside a javadoc or a {@code //} comment ({@code git diff -U0
 *       6c8e1544 HEAD} over this path returns no non-comment line). No assertion moved.</li>
 * </ul>
 *
 * <p><b>⚠ THE b2 CLAIM, CORRECTED (LAW 82).</b> The seat-30 draft of this javadoc said b2 "is
 * now the guard against the term being re-introduced without a carrier". <b>No measurement
 * supports that, and one refutes it:</b> {@code m-law5ii} severed the receiver recursion and
 * measured EMPTY — b2 among the greens. A term whose presence or absence moves no byte cannot
 * be guarded by a byte assert, so re-introducing it would leave b2 green exactly as severing it
 * did. What b2 IS, measured: the <b>{@code mapC}-refinement decline lock</b> — it fired under
 * {@code m-law5i} (the {@code armKind}-only mutant, 4F: b1, b2, corpus_c1b, corpus_control1),
 * which is the mutation that would actually unwrap its shape. The receiver-recursion term has
 * NO lock in this suite and cannot have one at this corpus; that is the content of
 * {@code m-law5ii}'s adjudicated zero, and the honest score is "un-witnessed", not "guarded".
 */
class DeepCallChainMultiSeatTest {

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

    /** Cell A = drr 7.0.0 — the carrier cell (7.1/7.2/7.3 carry the identical four rows). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = drr 5.61.0 — a NON-carrier cell (zero {@code armKind=RDeepFeatureCall} rows at the
     * wrap seat). Its whole-cell scan is the over-fire control: the arm must not move a byte
     * outside its measured population.
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
    // The fixture — the minimal pair twice over
    // =========================================================================

    /**
     * The shape under test is the corpus's, minimised: a conditional LADDER inside an
     * {@code extract} over a MULTI receiver, so the lambda is {@code mapItemToList} and the ladder
     * renders at the MapperC seat, with one arm a bare deep call.
     * ({@code UnderlierProductIdentifier}'s source: {@code … then Observable extract (if Asset ->
     * Instrument -> Security exists then … else if Index exists then Index ->> identifier)}.)
     *
     * <p><b>The two minimal pairs.</b> a1/b1 share the bare-name deep receiver ({@code idx});
     * a2/b2 share the point-free SINGLE-output FUNCTION receiver ({@code IdxForHolder}) — exactly
     * the negative control's own receiver shape,
     * {@code MapperS.of(underlierForProduct.evaluate(item.get()))}. Within each pair the ONLY
     * difference is the deep hop's cardinality ({@code legs} 0..* ⇒ {@code mapC};
     * {@code moniker} 0..1 ⇒ {@code map}), so the pairs prove the discriminator is the HOP and
     * never the receiver — which is what locked the arm's second term long enough for the chain
     * to measure it away (see the class javadoc: seat 30 collapsed the arm to one term).
     *
     * <p>{@code tag} / {@code label} read naturally here but are rune lexer keywords; the
     * single-valued deep leaf is {@code moniker}.
     */
    private static final String MODEL = """
            namespace census.seat29law5
            version "1.0.0"

            type Leg:
                code string (0..1)
                amt number (0..1)

            type Sub:
                legs Leg (0..*)
                moniker string (0..1)

            type Idx:
                subA Sub (0..1)

            type Carrier:
                legs Leg (0..*)
                one Leg (0..1)

            type Holder:
                idx Idx (0..1)
                direct Carrier (0..1)

            func IdxForHolder: <"a2/b2's deep receiver - a point-free SINGLE-output function call, the NameOfTheUnderlyingIndexRule receiver shape">
                inputs:
                    h Holder (1..1)
                output:
                    i Idx (0..1)
                set i:
                    h -> idx

            func A1DeepArmMapCUnwrapped: <"a1 - THE CARRIER: a bare deep arm whose hop is MULTI (mapC) at the MapperC ladder seat">
                inputs:
                    hs Holder (0..*)
                output:
                    out Leg (0..*)
                add out:
                    hs
                        extract
                            if direct exists
                            then direct -> legs
                            else if idx exists
                            then idx ->> legs

            func A2DeepArmFnRootedMapCUnwrapped: <"a2 - the SAME carrier over the negative control's own receiver: a point-free function call">
                inputs:
                    hs Holder (0..*)
                output:
                    out Leg (0..*)
                add out:
                    hs
                        extract
                            if direct exists
                            then direct -> legs
                            else if IdxForHolder exists
                            then IdxForHolder ->> legs

            func B1DeepArmMapKeepsWrap: <"b1 - the DECLINE pin: the same bare deep arm with a SINGLE hop (map) keeps its MapperC.of wrap">
                inputs:
                    hs Holder (0..*)
                output:
                    out string (0..*)
                add out:
                    hs
                        extract
                            if direct exists
                            then direct -> legs -> code
                            else if idx exists
                            then idx ->> moniker

            func B2DeepArmFnRootedMapKeepsWrap: <"b2 - THE in-fixture NameOfTheUnderlyingIndexRule twin: a function-rooted SINGLE hop keeps its wrap">
                inputs:
                    hs Holder (0..*)
                output:
                    out string (0..*)
                add out:
                    hs
                        extract
                            if direct exists
                            then direct -> legs -> code
                            else if IdxForHolder exists
                            then IdxForHolder ->> moniker

            func B3SingleChainArmKeepsWrap: <"b3 - the seat's OTHER arms must not move: a plain SINGLE chain arm keeps its wrap">
                inputs:
                    hs Holder (0..*)
                output:
                    out Leg (0..*)
                add out:
                    hs
                        extract
                            if direct -> legs exists
                            then direct -> legs
                            else if direct -> one exists
                            then direct -> one
            """;

    private static final String MAPC_HOP =
            "mapC(\"chooseLegs\", idx -> idxDeepPathUtil.chooseLegs(idx))";
    private static final String MAP_HOP =
            "map(\"chooseMoniker\", idx -> idxDeepPathUtil.chooseMoniker(idx))";
    private static final String FN_RECEIVER = "MapperS.of(idxForHolder.evaluate(";

    // =========================================================================
    // Part A — the carriers (RED before the flip)
    // =========================================================================

    /**
     * a1 — the carrier. The deep hop renders {@code mapC} (the leaf {@code legs} is 0..*), so the
     * chain IS multi and the ladder's MapperC rung must return it BARE. Witness-unique on the
     * token the flip REMOVES: {@code MapperC.of(item.} — the rendered prefix CONFIRMED at the
     * seat-29 chain's RED leg (a1 in the measured RED set, both routes).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_deepArmWithMapCHopReturnsBare() throws IOException {
        String out = fn("A1DeepArmMapCUnwrapped.java");
        assertContains(out, MAPC_HOP);
        assertTrue(!codeOnly(out).contains("MapperC.of(item."),
                "a deep arm whose hop is mapC must return the BARE chain:\n" + out);
    }

    /**
     * a2 — the same carrier over the NEGATIVE CONTROL's own receiver (a point-free SINGLE-output
     * function call). Pairs with b2: same receiver, opposite hop cardinality.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_fnRootedDeepArmWithMapCHopReturnsBare() throws IOException {
        String out = fn("A2DeepArmFnRootedMapCUnwrapped.java");
        assertContains(out, MAPC_HOP);
        assertContains(out, FN_RECEIVER);
        assertTrue(!codeOnly(out).contains("MapperC.of(" + FN_RECEIVER),
                "a function-rooted deep arm whose hop is mapC must return the BARE chain:\n" + out);
    }

    // =========================================================================
    // Part B — the decline pins (witness-unique on the token the flip REMOVES)
    // =========================================================================

    /**
     * b1 — the DECLINE pin. The identical arm shape with a SINGLE deep hop ({@code moniker} is
     * 0..1 ⇒ {@code map}) is genuinely single, so upstream's item→list coercion is CORRECT and
     * the wrap must SURVIVE. This is the in-fixture {@code NameOfTheUnderlyingIndexRule} law: the
     * corpus's own golden carries the wrap at that shape.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_deepArmWithMapHopKeepsItsWrap() throws IOException {
        String out = fn("B1DeepArmMapKeepsWrap.java");
        assertContains(out, MAP_HOP);
        assertContains(out, "MapperC.of(item.");
    }

    /**
     * b2 — the RECEIVER DECLINE lock, and the closest in-fixture twin of the corpus negative
     * control. Its receiver is byte-identical to a2's; only the hop's cardinality differs, so if
     * ANY term of the arm ever proved MULTI off that receiver, this wrap would vanish and the four
     * corpus rows with it. Seat 29 shipped a second term (the receiver recursion) and this test
     * was its lock; mutation {@code m-law5ii} measured that term EMPTY and seat 30 removed it, so
     * the arm now declines off this receiver <b>by construction</b> rather than by walking it.
     * The test is unchanged and its verdict is unchanged.
     *
     * <p><b>MEASURED role (LAW 82) — do not overstate it.</b> b2 did NOT move under
     * {@code m-law5ii} (that lane measured EMPTY, b2 green), so b2 never locked the receiver
     * recursion and is <b>not</b> a guard against its re-introduction: a byte-neutral term
     * cannot be caught by a byte assert in either direction. b2's measured lock is the
     * {@code mapC} REFINEMENT — it fired under {@code m-law5i} (the {@code armKind}-only
     * mutant: b1, b2, corpus_c1b, corpus_control1, 4F). It is also green at seat 30's RED base
     * ({@code 11/0F}, both routes), consistent with it being wrap-PRESENT witness-unique.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_fnRootedDeepArmWithMapHopKeepsItsWrap() throws IOException {
        String out = fn("B2DeepArmFnRootedMapKeepsWrap.java");
        assertContains(out, MAP_HOP);
        assertContains(out, "MapperC.of(" + FN_RECEIVER);
    }

    /**
     * b3 — the seat's OTHER arms. A plain SINGLE navigation arm at the same MapperC rung keeps
     * its {@code MapperC.of(…)} coercion: law 5 adds ONE arm to the cardinality overlay, it does
     * not touch the wrap seat's own decision procedure.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_plainSingleArmKeepsItsWrap() throws IOException {
        String out = fn("B3SingleChainArmKeepsWrap.java");
        assertContains(out, "MapperC.of(item.");
    }

    // =========================================================================
    // Part C — the corpus (LAW 80: line-level locks, NOT byte-whole)
    // =========================================================================

    /** The three carrier files of drr 7.0.0 (each also present in 7.1.0/7.2.0/7.3.0). */
    private static final String LEG1 =
            "drr/regulation/common/functions/GetUnderlierProductIdentifierLeg1.java";
    private static final String UPI =
            "drr/base/trade/underlier/functions/UnderlierProductIdentifier.java";
    private static final String GBC =
            "drr/base/trade/basket/functions/GetBasketConstituents.java";
    /** The in-band NEGATIVE control — the rule is declared once and the other three files delegate. */
    private static final String NOTUI =
            "drr/regulation/common/trade/underlier/reports/NameOfTheUnderlyingIndexRule.java";

    private static final String CHOOSE_ID =
            ".<AssetIdentifier>mapC(\"chooseIdentifier\", index -> indexDeepPathUtil.chooseIdentifier(index));";
    private static final String CHOOSE_NAME =
            ".<FieldWithMetaString>map(\"chooseName\", index -> indexDeepPathUtil.chooseName(index))";

    /**
     * control0 — golden is the oracle (prove the instrument can fail). Golden returns the deep
     * {@code chooseIdentifier} chain BARE at all three carriers AND keeps the
     * {@code MapperC.of(…)} wrap on the {@code chooseName} arm of the negative control. Both
     * halves are read from golden bytes, so a broken scan cannot pass this test quietly.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenIsBareAtCarriersAndWrappedAtTheControl() throws IOException {
        for (String p : List.of(LEG1, UPI, GBC)) {
            String g = normalize(Files.readString(GOLDEN_A.resolve(p)));
            assertTrue(g.contains("return item.") || g.contains("return MapperS.of("),
                    "golden must carry a deep-arm return in " + p);
            assertTrue(g.contains(CHOOSE_ID),
                    "golden must end the deep arm with the bare chooseIdentifier hop in " + p);
            assertTrue(!g.contains("MapperC.of(item.<Index>map(\"getIndex\""),
                    "golden must NOT wrap the mapC deep arm in " + p);
        }
        String c = normalize(Files.readString(GOLDEN_A.resolve(NOTUI)));
        assertTrue(c.contains("return MapperC.of("),
                "golden must KEEP the wrap on the chooseName arm of " + NOTUI);
        assertTrue(c.contains(CHOOSE_NAME),
                "golden's wrapped arm must be the chooseName (map) hop in " + NOTUI);
    }

    /**
     * c1 — the carriers' LINE-level lock. LAW 80: none of these three files goes byte-whole under
     * law 5 alone ({@code GetUnderlierProductIdentifierLeg1} needs one F11 receiver-cardinality
     * law, {@code UnderlierProductIdentifier} needs four families, {@code GetBasketConstituents}
     * six), so this pins the ONE line the law owns: the deep arm's {@code return} must be bare and
     * the wrapped form must be gone.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr7CarriersReturnTheDeepArmBare() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        for (String p : List.of(LEG1, UPI, GBC)) {
            String gen = drrAOutput.get(p);
            assertNotNull(gen, "not generated: " + p);
            // normalize WITHOUT codeOnly: these literals embed string quotes, which
            // codeOnly strips - matching against codeOnly text is vacuous (the RED-head
            // measurement caught this bug class pre-law, on c1b's chooseName assert).
            String code = normalize(gen);
            assertTrue(code.contains(CHOOSE_ID),
                    "the deep chooseIdentifier hop must still render in " + p);
            // ONLY the deep-arm shape is asserted absent: golden GetBasketConstituents
            // itself carries `MapperC.of(MapperS.of(` twice on OTHER (correct) shapes, so
            // the broad term would fail against golden's own text (measured at the law
            // head: golden 2, fork 2, byte-equal on those sites).
            assertTrue(!code.contains("MapperC.of(item.<Index>map(\"getIndex\""),
                    "the mapC deep arm must not be MapperC.of-wrapped in " + p + ":\n" + gen);
        }
    }

    /**
     * c1b — the in-band NEGATIVE control at corpus grain (LAW 76): the {@code chooseName} arm's
     * wrap is byte-CORRECT and must SURVIVE. Green in both states; it fires under
     * {@code m-law5i}.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1b_nameOfTheUnderlyingIndexKeepsItsCorrectWrap() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String gen = drrAOutput.get(NOTUI);
        assertNotNull(gen, "not generated: " + NOTUI);
        // normalize WITHOUT codeOnly - CHOOSE_NAME embeds string quotes (see c1's note).
        String code = normalize(gen);
        assertTrue(code.contains(CHOOSE_NAME),
                "the chooseName deep hop must still render in " + NOTUI);
        assertTrue(code.contains("return MapperC.of("),
                "the byte-CORRECT wrap on the chooseName arm must survive in " + NOTUI + ":\n" + gen);
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0 (the carrier cell): per file the
     * (wrapped deep return, bare deep return, deep-hop population) triple must equal golden's,
     * file for file over the UNION, beyond the NAMED residue.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellDeepArmReturnsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /** control2 — LAW 77 route parity for the three carriers and the negative control. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        for (String p : List.of(LEG1, UPI, GBC, NOTUI)) {
            assertEquals(drrAOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /**
     * control3 — LAW 79 in a NON-carrier cell. drr 5.61.0 carries ZERO
     * {@code armKind=RDeepFeatureCall} rows at the wrap seat, so its whole-cell triple must be
     * byte-flat against golden beyond the named residue: the arm must not reach a deep chain
     * outside its measured population.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellDeepArmReturnsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * MEASURED EMPTY at the law's head, and re-confirmed EMPTY by seat 30's chain (this suite
     * runs 11/0F on BOTH routes at {@code e223ce19}). The SET is pinned, not the count (LAW 73).
     * {@code UnderlierProductIdentifier} and {@code GetBasketConstituents} keep other-family
     * deltas but their triples came back EQUAL, so they drop out — the empty list is the
     * stronger claim and it is the measured one.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    /** MEASURED EMPTY at the law's head — this cell carries no law-5 row at all. */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /**
     * MEASURED at the first corpus run — the drr 7.0.0 union domain (golden ∪ fork
     * token-bearing files, intersected with what this harness emits). Note the residue assert
     * runs BEFORE the domain assert, so a stale value here is masked by a green residue;
     * falsifier = the harness's own "MEASURED domain=" print at a failing run.
     */
    private static final int DOMAIN_DRR7 = 19;

    /** MEASURED at the first corpus run — the drr 5.61.0 union domain (zero rows: see above). */
    private static final int DOMAIN_DRR561 = 0;

    /**
     * (T1, T2, T3) per file:
     * <ul>
     *   <li><b>T1</b> — the law's REMOVED shape: a {@code return MapperC.of(} whose line also
     *       carries a {@code DeepPathUtil.choose} hop (a WRAPPED deep-call return).</li>
     *   <li><b>T2</b> — the law's ADDED shape: a {@code return } that carries a
     *       {@code DeepPathUtil.choose} hop and is NOT {@code MapperC.of}-wrapped (a BARE
     *       deep-call return).</li>
     *   <li><b>T3</b> — the OVER-FIRE NET: the file's TOTAL {@code DeepPathUtil.choose} count.
     *       Deliberately global (not return-scoped): the deep-hop population itself must not move
     *       by a single occurrence anywhere in the cell.</li>
     * </ul>
     * Each carrier moves T1 −1, T2 +1, T3 unchanged; the negative control and every other file
     * must match golden exactly. The generated returns are single-line by construction (the
     * renderer emits one {@code return <chain>;} per arm), which is what makes the line-grain
     * state machine sound — re-verify with a corpus grep before pinning the domain.
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
                boolean deep = s.contains("DeepPathUtil.choose");
                if (deep && s.startsWith("return MapperC.of(")) {
                    t1++;
                } else if (deep && s.startsWith("return ")) {
                    t2++;
                }
                int from = 0;
                while ((from = s.indexOf("DeepPathUtil.choose", from)) >= 0) {
                    t3++;
                    from += "DeepPathUtil.choose".length();
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

    /**
     * The scan universe is the token-bearing union INTERSECTED with the files this harness emits
     * (rule/report/function kinds): golden's POJO and metafield kinds can bear tokens these
     * generators never produce, and whether the fork emits every expected FILE is the D11 ring's
     * question (missingOutput), not this suite's.
     */
    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means an"
                + " unpinned call site");
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
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-26/27/28 suite shape verbatim)
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat29law5.rosetta");
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
            fixtureOut = render(m -> "census.seat29law5".equals(m.namespace()));
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
            throw new AssertionError("[DeepCallChainMultiSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
