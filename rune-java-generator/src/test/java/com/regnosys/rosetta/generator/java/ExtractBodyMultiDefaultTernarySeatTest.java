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
 * SEAT 32, law D2 -- facet {@code extractBodyMultiDefaultTernary}: a {@code default} that is
 * the BODY of a {@code then extract} inline function, OUTSIDE any alias, whose LEFT operand
 * PROVES multi renders upstream's list-form ternary
 * {@code <L>.getMulti().isEmpty() ? MapperC.of(<R>) : <L>} stamped
 * {@code MapperC<joinedBareElement>} -- the THIRD seat of the
 * {@code thenSeatMultiDefaultTernary} family, after the #345 alias arm and the seat-5
 * then-BASE arm.
 *
 * <p><b>THREE RUNGS, ONE PREDICATE.</b> Upstream ({@code ExpressionGenerator.xtend}
 * L452-469) branches on {@code left.isMulti} ALONE and compiles the RIGHT at
 * {@code MAPPER.wrapExtends(joined)} in the multi branch, so the change is not only the
 * ternary text:
 * <ul>
 *   <li><b>rung 1</b> ({@code SetOperationHandler}, a new arm after the two existing multi
 *       arms have returned): the ternary itself, its right-arm {@code MapperS -> MapperC}
 *       lift taken from {@code WrapperToWrapperCoercer:107-114} (LAW 69, not hand-rolled),
 *       and the {@code MapperC<join>} stamp the thenArg decl seat reads.</li>
 *   <li><b>rung 2</b> ({@code ControlFlowHandler.hoistAsItemLocalOrNull}, the
 *       {@code mapperFormSlot} disjunction): the ternary's RIGHT operand joins the
 *       Mapper-form ite slot family. The #383 {@code RDefaultExpr} disjunct is LEFT-only
 *       because "the RIGHT operand is getOrDefault's ITEM argument, never a Mapper" -- true
 *       of the single render, FALSE of the ternary render. This yields golden's
 *       {@code final MapperS<String> ifThenElseResult;} and
 *       {@code ifThenElseResult = MapperS.<String>ofNull();}.</li>
 *   <li><b>rung 3</b> (the same method's arm renderer, :2668): at that slot a Mapper-chain
 *       arm is KEPT as the Mapper it already is -- the seat-31 law-1b {@code
 *       mapperFormRuleRootArmKeep} rung i, whose gate widens from the rule-root slot to the
 *       ternary-arm slot. Without it the :2703 terminal would re-present the chain as
 *       {@code MapperS.of(<chain>.get())}, which no golden carries here.</li>
 * </ul>
 * The seat is ONE shared predicate --
 * {@code NavigationHandler.extractBodyMultiDefaultTernary} -- read by rung 1 and by rungs
 * 2/3 as the NECESSARY condition both key on (the #367 single-predicate law); each half
 * carries its own further preconditions (the render arm a resolvable joined element from the
 * LEFT's compiled stamp plus its infra guards, the slot the hoist path being reached), and at
 * every measured carrier both fire (the 4 {@code [P32-DEF]} rows,
 * {@code leftType=MapperC<FieldWithMetaString>}).
 *
 * <p><b>THE DOSSIER'S STEP 2 WAS RE-SITED BY READING (say so plainly).</b> Scout D proposed
 * threading a {@code MAPPER.wrapExtends(joined)} expected type into the RIGHT compile.
 * {@code hoistAsItemLocalOrNull} NEVER reads {@code ctx.expectedType()} -- verified over its
 * whole body at head {@code ddcdd151b} -- so that thread would not have flipped the ite
 * form; it would only have added an entry coercion ({@code MapperS.of(ifThenElseResult)}) on
 * the sentinel, which is not golden. The measured probe field {@code expected=null} stands
 * exactly as measured (the RIGHT is not Mapper-consumed today); the plumbing that makes it
 * Mapper-consumed is the SLOT, not the expected type.
 *
 * <p><b>CARRIER:</b> drr 7.0/7.1/7.2/7.3 POJO {@code IndicatorOfTheUnderlyingIndexRule}
 * (sigs B058 root, B040 + B020 the ite form, B032 the thenArg decl, B021 the arg deref).
 * <b>4 whole files -- WITH LAW D1.</b> B058's hunk also carries the LEFT wrapper deref
 * ({@code .<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString
 * .getValue())}), which D1 ({@code defaultJoinDerefAtCollapsedLeft}) supplies. {@code
 * corpus_c1}/{@code corpus_c2} are therefore CLAIMED RED at the D1 head and CLAIMED GREEN at
 * the D2 head; D2 alone heals zero whole files.
 *
 * <p><b>LAW 74 -- MEASURED, not analytic.</b> {@code target/seat32-instruments/javac32/
 * pre-javac.txt}, {@code Seat32Pre.java:830} (section C14):
 * {@code error: incompatible types: String cannot be converted to FieldWithMetaString} on
 * {@code ...<FieldWithMetaString>map("getIdentifier", ...).getOrDefault(ifThenElseResult)}.
 * The javac32 report also CORRECTS the dossier: only that error is witnessed -- the claimed
 * second error (the {@code mapSingleToList} lambda returning a {@code MapperS}) is NOT
 * separately witnessed, because an erroneous lambda body stops javac checking the return.
 * The file is non-compiling either way, so every carrier is an already-waivered mismatch and
 * the law is green-safe by construction. POST is owed at the D2 head.
 *
 * <p><b>LAW 77 -- INHERITS at all three rungs, checked seat by seat.</b>
 * {@code IRExpressionCompiler.visitDefault} (:3440-3442) and its oracle leg (:13918-13921)
 * both call {@code super.visitDefault} -- the legacy {@code SetOperationHandler.handle}.
 * {@code IRControlFlowHandler extends ControlFlowHandler} and overrides ONLY
 * {@code ifThenElseResultBaseName} (the hoist NAME), never the hoist FORM;
 * {@code ConditionalRenderer} renders WHOLESALE through {@code super.visitConditional}.
 * {@code IRJavaLeafEmitter}'s {@code getMulti()} sites are the callee-param unwrap seat
 * (via {@code CallParamMultiResolver}), a different question -- no IR twin exists for the
 * default-ternary or the ite-slot decision. {@code corpus_control2} re-proves it per FILE.
 *
 * <p><b>CLAIMED RED (LAW 66) at the D1 head, both routes:</b> {@code a1}, {@code a2},
 * {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+ {@code corpus_control2}
 * under {@code -Pir-on}). <b>GREEN at RED:</b> {@code e1}, {@code e2}, {@code e3},
 * {@code corpus_control0}. <b>CLAIMED GREEN at the D2 head</b> -- with the single documented
 * exception of {@code corpus_control1}'s union-domain pin, which ships as the {@code -1}
 * print-first sentinel (seat 30's law) and must be transcribed from the assert's own print in
 * the SAME commit. Every other pin in this suite is derived, not drafted.
 *
 * <p><b>MUTATION LANES (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-d2*.log}):</b>
 * <ul>
 *   <li><b>m-d2</b> (all three rungs reverted): MEASURED <b>10/4F/1S</b> = {@code a1},
 *       {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1}; {@code a2} GREEN.
 *       <b>Re-scored:</b> the claim listed {@code a2}, but {@code a2} is the B032 then-arg DECL
 *       stamp lock, and that decl is bared by law D.1's left deref independently of this law --
 *       the commit note had already measured {@code a2} GREEN at the D.1 head; under the full
 *       revert of THIS law it stays GREEN for the same reason. The claim was stale, the lock is
 *       right where it is.</li>
 *   <li><b>m-d2-alias</b> (the {@code findEnclosingShortcut(expr) != null} decline dropped):
 *       MEASURED <b>10/1F/1S = {@code e1}</b> -- the claim; {@code corpus_control1} did NOT move
 *       (the UNKNOWN the claim declared, resolved: no movement inside this suite's cells; the
 *       cdm 6.20.x MapBasketReferenceInformation cells are the matrix digest's to see).</li>
 *   <li><b>m-d2-left</b> (the proof widened from {@code chainProvesMulti(rawLeft)} to
 *       {@code chainProvesMulti(expr)}): MEASURED <b>10/0F/1S -- EMPTY</b>. <b>Re-scored:</b>
 *       the claim "{@code e3} must FAIL" was wrong by construction -- {@code e3}'s right is
 *       SINGLE (it is the {@code first}-on-the-left twin), so no widening over the right can
 *       flip it; and {@code e2} (MapBreakdown, {@code multiLeft=false multiRight=true}) held
 *       because its right is exactly what {@code chainProvesMulti} under-approximates, on the
 *       whole expression as on {@code rawLeft}. The {@code rawLeft} scoping is therefore
 *       UNWITNESSED at this suite's grain: a witness needs a MULTI right that
 *       {@code chainProvesMulti} CAN prove over a SINGLE left -- BANKED, not re-scored as
 *       decorative and not widened.</li>
 *   <li><b>m-d2-slot</b> (rung 2 severed, rungs 1 and 3 kept): MEASURED <b>10/4F/1S</b> =
 *       {@code a1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} -- the claim
 *       exactly, with the ternary text still present: the two halves are separable and BOTH
 *       required, MEASURED.</li>
 *   <li><b>m-d2-armkeep</b> (rung 3 severed): MEASURED <b>10/2F/1S</b> = {@code corpus_c1},
 *       {@code corpus_c2}; {@code a1} GREEN and {@code corpus_control1} DESIGNED-EMPTY held.
 *       <b>Re-scored:</b> {@code a1}'s asserts pin the decl, the {@code ofNull()} init, the
 *       ternary and the slot tokens -- none of them is the arm's
 *       {@code MapperS.of(<chain>.get())} re-presentation the claim named, so the fixture cannot
 *       see this rung; the corpus carriers are the lane's witnesses, and they moved.</li>
 *   <li><b>m-d2-lift</b> (the {@code MapperS -> MapperC} coercion dropped): MEASURED
 *       <b>10/3F/1S</b> = {@code a1}, {@code corpus_c1}, {@code corpus_c2} -- the claim exactly,
 *       and {@code corpus_control1} control-NEUTRAL as claimed (T1's needle stops before the
 *       lift).</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on}); {@code a2} GREEN at
 * the base too (law D.1 absent there as well -- the decl's bare form is the B032 lock's own
 * text, present in both states). GREEN at the head 10/0F/1skip default, 10/0F/0skip
 * {@code -Pir-on}.
 *
 * <p><b>LAW-81 TRIPWIRES the lead must expect</b> (named in NOTES.md, not edited here):
 * {@code corpus_control1}'s {@code KNOWN_RESIDUE_DRR700} carries three rows this law does not
 * own -- {@code GetBasketConstituents}, {@code Price}, {@code TotalNotionalQuantity} -- each
 * of which LEAVES the list the moment its own law lands, firing this control. Re-pin from
 * that run's print, in that law's commit.
 */
class ExtractBodyMultiDefaultTernarySeatTest {

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

    /** The carrier -- 4 cells, sigs B058 B040 B020 B032 B021, no import sig. */
    private static final String CARRIER =
            "drr/regulation/common/emir/reports/IndicatorOfTheUnderlyingIndexRule.java";
    /** The family's own natural experiment: the SAME shape with {@code first} on the left. */
    private static final String SINGLE_LEFT_TWIN =
            "drr/regulation/jfsa/rewrite/trade/reports/UnderlyingIndexIndicatorRule.java";
    /** The conservative-proof witness -- 3 golden ternaries over lefts this proof calls single. */
    private static final String MAP_BREAKDOWN =
            "cdm/ingest/fpml/confirmation/workflowstep/functions/MapBreakdown.java";
    /** The #345 alias arm's green carrier family, in-domain here. */
    private static final String MAP_BASKET_REF =
            "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/"
                    + "MapBasketReferenceInformation.java";
    private static final String MAP_CREDIT_INDEX =
            "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapCreditIndex.java";

    private static final String TERNARY = ".getMulti().isEmpty() ? ";

    /** Cell A = drr 7.0.0 -- the carrier cell and the whole-cell control's cell. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.3.0 -- the second whole-file heal. */
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
    // Fixtures -- reduced from the REAL source
    // test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
    //   regulation-common-emir-rule.rosetta:869-879   (rule IndicatorOfTheUnderlyingIndex)
    //   regulation-jfsa-rewrite-trade-rule.rosetta:300-312 (rule UnderlyingIndexIndicator,
    //                                                       the `first`-on-the-left twin)
    // The multi-line `default if <cond> then <chain>` shape is copied from those sources
    // VERBATIM in structure (both parse in the corpus). If the reduced model fails to parse,
    // parenthesise the right operand -- `default (if IsFlag(item) then item -> code)` -- and
    // re-run; do NOT weaken an assert to make a fixture pass.
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat32d2
            version "1.0.0"

            type Box:
                identifier string (0..1)
                    [metadata scheme]

            type Prod:
                code string (0..1)
                boxes Box (0..*)

            type Root:
                prod Prod (0..1)

            func GetBoxes: <"the UnderlierProductIdentifier twin - a MULTI-output with-args call, the default LEFT root">
                inputs:
                    p Prod (0..1)
                output:
                    bs Box (0..*)
                add bs:
                    p -> boxes

            func IsFlag: <"the IsFRA twin - the conditional condition">
                inputs:
                    p Prod (0..1)
                output:
                    r boolean (1..1)
                set r:
                    p -> code exists

            func Consume: <"the GetIndexIndicatorFromFloatingRate twin - the stage whose ARG carries B021">
                inputs:
                    s string (0..1)
                output:
                    r string (0..1)
                set r:
                    s

            reporting rule D2Carrier from Root: <"a1/a2 - THE IndicatorOfTheUnderlyingIndex SHAPE: a MULTI-left default in a then-extract body, its RIGHT a no-else conditional">
                extract prod
                then extract
                    GetBoxes(item) -> identifier default if IsFlag(item)
                        then item -> code
                then extract Consume(item)

            reporting rule D2SingleLeft from Root: <"e3 - THE UnderlyingIndexIndicator SHAPE: the SAME rule with `first` on the left, so the LEFT-only proof declines and golden keeps getOrDefault">
                extract prod
                then extract
                    GetBoxes(item) -> identifier first
                        default if IsFlag(item)
                            then item -> code
                then extract Consume(item)

            func E1AliasScoped: <"e1 - the MapBasketReferenceInformation seat: the SAME default INSIDE an alias body, where the #345 arm owns the question">
                inputs:
                    p Prod (1..1)
                output:
                    out string (0..*)
                alias picked:
                    p
                        then extract
                            GetBoxes(item) -> identifier default if IsFlag(item)
                                then item -> code
                add out:
                    picked
            """;

    // =========================================================================
    // Part A -- the law, at fixture grain
    // =========================================================================

    /**
     * a1 -- the whole law at fixture grain: the ternary (rung 1) + its {@code MapperC.of}
     * arm lift + the Mapper-form ite slot (rung 2) + the kept Mapper-chain arm (rung 3), and
     * the two fork forms that must vanish (the #234 {@code getOrDefault} splice and the
     * downstream wrapper-item deref at the next stage's {@code mapItem} arg).
     *
     * <p>PIN AT RED: if the reduced rule does not hoist an {@code ifThenElseResult} at all
     * (the ite falling to the inline ternary), RESHAPE THE FIXTURE -- do not weaken the
     * assert. The carrier hoists on the lambda channel inside {@code mapSingleToList}, and
     * the reduction must reproduce that.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_extractBodyMultiDefaultRendersTheTernaryAndTheMapperFormSlot() throws IOException {
        String out = fixtureRule("D2CarrierRule");
        assertContains(out, "final MapperS<String> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = MapperS.<String>ofNull();");
        assertContains(out, TERNARY + "MapperC.of(ifThenElseResult) : ");
        assertTrue(!out.contains(".getOrDefault(ifThenElseResult"),
                "the #234 single-getOrDefault splice must be displaced by the ternary:\n" + out);
        assertTrue(!out.contains("String ifThenElseResult = null;"),
                "rung 2 must replace the item-form slot with the Mapper form:\n" + out);
        assertTrue(!out.contains("final FieldWithMetaString fieldWithMetaString = item.get();"),
                "with the ternary stamped MapperC<String> the next stage's arg deref must"
                + " decline (B021):\n" + out);
    }

    /**
     * a2 -- the stamp half: the ternary is stamped {@code MapperC<joinedBareElement>}, so the
     * {@code then}-arg decl seat's {@code compiledStampsBareElement} keeps the BARE element
     * (golden {@code final MapperC<String> thenArg1 = thenArg0}, against the fork's
     * {@code MapperC<FieldWithMetaString>} -- sig B032). PIN AT RED: if the reduction numbers
     * its thenArgs differently, correct the EXPECTED NAME, never the type.
     * MEASURED (D2-red1.log): GREEN already at the D.1 head -- law D.1's left deref bares this
     * fixture's then-arg decl before D.2 lands (the drafter claimed it RED against the pre-D.1
     * text); it stays as the B032 lock in both states.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theTernaryStampsTheBareJoinedElementAtTheThenArgDecl() throws IOException {
        String out = fixtureRule("D2CarrierRule");
        assertContains(out, "MapperC<String> thenArg1");
        assertTrue(!out.contains("MapperC<FieldWithMetaString> thenArg1"),
                "the wrapper element must not re-leak into the thenArg decl (B032):\n" + out);
    }

    // =========================================================================
    // Part B -- the decline locks
    // =========================================================================

    /**
     * e1 -- the ALIAS decline lock, the {@code fn:MapBasketReferenceInformation} seat
     * (cdm 6.20.2-6.20.6, GREEN, 5 of the 37 measured {@code [P32-DEFTERN]} rows). Measured
     * over 1,216 {@code [P32-DEF]} rows, that function is the ONLY non-carrier
     * {@code where=} value with {@code gateExtractBody=true AND multiLeft=true}, and it
     * already renders this ternary through the #345 alias arm. Both halves of this law must
     * therefore decline inside an alias body -- rung 1 by ordering AND by the predicate,
     * rungs 2/3 by the predicate alone (ordering cannot protect a decl seat).
     *
     * <p>The reduced shape is the alias twin of {@code a1}: its RIGHT is SINGLE, so the
     * #345 both-strict gate declines too and the whole default keeps the item form. The
     * asserts name only what the law would change, so they hold whether or not the ite
     * hoists on this path (an alias body is a sink-less compilation path).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_aliasScopedExtractBodyDefaultKeepsItsItemForm() throws IOException {
        String out = fixtureFunction("E1AliasScoped");
        assertContains(out, ".getOrDefault(");
        assertTrue(!out.contains(TERNARY),
                "an ALIAS-scoped extract-body default is the #345 arm's question - this law"
                + " must leave it alone:\n" + out);
        assertTrue(!out.contains(">ofNull()"),
                "rung 2 must not reach an alias-scoped default's ite slot:\n" + out);
    }

    /**
     * e2 -- the CONSERVATIVE-PROOF decline lock, in corpus bytes.
     * {@code NavigationHandler.chainProvesMulti} is an UNDER-approximation of upstream's
     * {@code left.isMulti}, not a mirror: measured at the green {@code fn:MapBreakdown},
     * 24 of its 36 default rows read {@code multiLeft=false multiRight=true} while each of
     * its goldens carries THREE {@code getMulti().isEmpty() ? } ternaries -- at least two
     * golden MULTI ternaries per file sit over a left this proof calls single. The fork
     * under-fires there today and MUST go on under-firing: widening the proof to close that
     * gap is a different law with a different blast radius. This is the byte lock on that.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void e2_drr700MapBreakdownStaysByteIdentical() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String gen = drrAOutput.get(MAP_BREAKDOWN);
        assertNotNull(gen, "the conservative-proof witness must be INSIDE this cell's emitted"
                + " domain, else this lock proves nothing: " + MAP_BREAKDOWN);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(MAP_BREAKDOWN))), normalize(gen),
                "MapBreakdown must stay byte-identical - the fork's LEFT proof under-fires on"
                + " its alias-call lefts by measurement, and this law must not change that");
    }

    /**
     * e3 -- the LEFT-ONLY discriminator, the family's own natural experiment reduced: the
     * SAME rule with {@code first} on the left. Golden drr 7.x
     * {@code UnderlyingIndexIndicatorRule} carries {@code final String ifThenElseResult;} +
     * {@code getOrDefault(ifThenElseResult)} and NO ternary, while the carrier -- one
     * {@code first} away -- carries {@code final MapperS<String> ifThenElseResult;} + the
     * ternary. That single source-level difference is the whole law's discriminator, and
     * {@code corpus_control0} asserts the same fact on the goldens themselves.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e3_singleLeftExtractBodyDefaultKeepsGetOrDefault() throws IOException {
        String out = fixtureRule("D2SingleLeftRule");
        assertContains(out, ".getOrDefault(");
        assertTrue(!out.contains(TERNARY),
                "a SINGLE left must keep the getOrDefault form - upstream branches on"
                + " left.isMulti alone:\n" + out);
        assertTrue(!out.contains(">ofNull()"),
                "rung 2 must not reach a single-left default's ite slot:\n" + out);
    }

    // =========================================================================
    // Part C -- the whole-file heals
    // =========================================================================

    /** corpus_c1 -- the drr 7.0.0 whole-file heal (RED at the D1 head, GREEN at the D2 head). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700IndicatorOfTheUnderlyingIndexRuleMatchesGolden() throws IOException {
        lockCell(drrAOutput, drrAGenErrors, GOLDEN_A, CARRIER, "drr 7.0.0");
    }

    /** corpus_c2 -- the drr 7.3.0 whole-file heal (the same five sigs, the same hunks). */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730IndicatorOfTheUnderlyingIndexRuleMatchesGolden() throws IOException {
        lockCell(drrBOutput, drrBGenErrors, GOLDEN_B, CARRIER, "drr 7.3.0");
    }

    // =========================================================================
    // Part D -- the controls
    // =========================================================================

    /**
     * control0 -- golden is the oracle and it DISCRIMINATES rather than merely agreeing: in
     * ONE cell, the multi-left rule's golden carries the ternary and the Mapper-form slot
     * while the {@code first}-on-the-left twin's golden carries neither, and the fork must
     * never render a ternary at the twin (the over-fire direction of the LEFT-only proof).
     * The twin's own whole-file identity belongs to law D1 and is deliberately NOT asserted
     * here, so this control's verdict does not depend on D1's state.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenTernaryAtTheMultiLeftAndGetOrDefaultAtTheSingleLeftTwin()
            throws IOException {
        String gCarrier = collapse(Files.readString(GOLDEN_A.resolve(CARRIER)));
        assertTrue(gCarrier.contains(TERNARY + "MapperC.of(ifThenElseResult) : "),
                "golden must carry the ternary at the MULTI-left carrier");
        assertTrue(gCarrier.contains("final MapperS<String> ifThenElseResult;"),
                "golden must carry the Mapper-form ite slot at the MULTI-left carrier");
        String gTwin = collapse(Files.readString(GOLDEN_A.resolve(SINGLE_LEFT_TWIN)));
        assertTrue(!gTwin.contains(TERNARY),
                "golden must NOT carry the ternary at the `first`-on-the-left twin - the"
                + " discriminator this law rests on");
        assertTrue(gTwin.contains(".getOrDefault(ifThenElseResult)"),
                "golden must keep getOrDefault at the `first`-on-the-left twin");
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String forkTwin = drrAOutput.get(SINGLE_LEFT_TWIN);
        assertNotNull(forkTwin, "the fork must emit the twin");
        assertTrue(!forkTwin.contains(TERNARY),
                "the fork must NOT render a ternary at the SINGLE-left twin (the over-fire"
                + " direction):\n" + forkTwin);
    }

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0: the carrier's cell, and the
     * cell that also holds the three green witnesses this law's blast radius was measured
     * against ({@code MapBreakdown}, {@code MapCreditIndex},
     * {@code MapBasketReferenceInformation}, all under the {@code cdm.ingest.*} namespace
     * this cell's {@code rosetta-config.yml} declares, hence all emitted here).
     *
     * <p>(T1, T2, T3) per file, chosen so that BOTH failure directions are visible:
     * <ul>
     *   <li><b>T1</b> {@code .getMulti().isEmpty() ? } -- the law's ADDED shape. Under-fire
     *       leaves the carrier at 0 against golden's 1; over-fire raises a green file.</li>
     *   <li><b>T2</b> {@code >ofNull()} -- rung 2's own signature (the Mapper-form empty-else
     *       terminal), so a slot that flips on a file this law does not name is caught.</li>
     *   <li><b>T3</b> {@code .getOrDefault(} -- the OVER-FIRE NET. Deliberately global: the
     *       green failure mode is a single default converted to a ternary somewhere this law
     *       does not name, and that shows up here as a getOrDefault count drop.</li>
     * </ul>
     * Counted on {@code collapse(codeOnly(text))} -- {@code codeOnly} FIRST, because its
     * {@code //} rule is newline-sensitive and every generated class carries
     * {@code // RosettaFunction dependencies} above its method bodies; the seat-31 helper's
     * collapse-then-codeOnly order truncates each file at that comment.
     *
     * <p>{@code GOLDEN_DOMAIN} is DERIVED, not drafted: {@code domain-walk.py} in the draft
     * directory reproduces this scan in Python over the whole golden tree.
     * {@code DOMAIN_DRR700} is the union domain (golden token-bearers plus any fork-only
     * one, intersected with the fork's emitted keys) and ships as the {@code -1} print-first
     * sentinel -- transcribe it from this assert's own print.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        for (String witness : List.of(MAP_BREAKDOWN, MAP_CREDIT_INDEX, MAP_BASKET_REF, CARRIER)) {
            assertTrue(drrAOutput.containsKey(witness),
                    "must be INSIDE this scan's domain, else control1 proves nothing about it"
                    + " (LAW: a control scans the domain it claims): " + witness);
        }
        Map<String, int[]> goldenScan = scan(readGoldenTree(GOLDEN_A));
        assertEquals(GOLDEN_DOMAIN, goldenScan.size(),
                "the GOLDEN-side token-bearing domain is derived by domain-walk.py and cannot"
                + " drift without the corpus drifting");
        assertUnionEqual(scan(drrAOutput), goldenScan, drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, DOMAIN_DRR700);
    }

    /**
     * control2 -- LAW 77, written as BOTH-ROUTES-vs-GOLDEN (the seat-30 standing lesson: two
     * routes can agree by both being wrong). All three rungs sit in classes the IR route
     * INHERITS rather than substitutes, but the carrier's render passes through
     * {@code IRExpressionCompiler}'s re-entrant interior visits and the ite hoist's IR-driven
     * NAME channel ({@code IRControlFlowHandler.ifThenElseResultBaseName}), so route parity
     * is load-bearing here rather than a formality.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CARRIER))),
                normalize(irOut.get(CARRIER)), "IR route vs GOLDEN: " + CARRIER);
        assertEquals(drrAOutput.get(MAP_BREAKDOWN), irOut.get(MAP_BREAKDOWN),
                "route divergence on the conservative-proof witness: " + MAP_BREAKDOWN);
    }

    // =========================================================================
    // The measured residue + domains (LAW 73: pin the SET, not just the count)
    // =========================================================================

    /**
     * MEASURED at head {@code ddcdd151b} by {@code domain-walk.py}, which scores the fork
     * side from the seat-31 final OFF dumps (the ONLY files whose fork text differs from
     * golden in this cell; every other file is byte-identical, so its counts agree by
     * identity). Sorted, because the control iterates a {@code TreeSet}.
     *
     * <p>The carrier's own row -- {@code IndicatorOfTheUnderlyingIndexRule fork=[0, 0, 2]
     * golden=[1, 1, 1]} -- is present at the D1 head and LEAVES at the D2 head; that
     * departure IS this control's failing-first evidence at corpus grain.
     *
     * <p>The three rows below are LAW-81 tripwires this law does not own: each leaves the
     * moment its own law lands. {@code GetBasketConstituents} (S32 charter, law 3 rung d),
     * {@code Price} (the shared {@code SetOperationHandler} seat -- B016/B029/B035/B062) and
     * {@code TotalNotionalQuantity} all sit in this cell. Re-pin from the fired control's
     * own print, in that law's commit.
     */
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of(
            // the GetBasketConstituents row (fork=[0, 4, 20] golden=[0, 6, 20]) LEFT this list at seat 33: law A.3
            // (ladderBlockCtorNestedExtract) block-converted both ctor-field ladders, so the two typed block
            // terminals now render (T2 = >ofNull() 4 -> 6 = golden; T3 = .getOrDefault( UNMOVED at 20); the file stays BANDED on its A.5/A.1/A.2 residue
            // (LadderBlockCtorNestedExtractSeatTest pins it by name); transcribed from the control print (A3-trip1.log).
            // the Price row (fork=[0, 3, 8] golden=[1, 3, 7]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // LAW 81 re-pin (seat 33, law C.1): fork=[0, 0, 8] -> fork=[0, 3, 8] - the statement ladder moved this scan's fork side toward golden; the file stays BANDED on C.2's default join; from C1-trip1.log.
            // the TotalNotionalQuantity row (fork=[0, 0, 10] golden=[0, 1, 10]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /**
     * The GOLDEN-side token-bearing count over the whole drr 7.0.0 golden tree (7,808
     * {@code .java} files, of which 3,579 carry at least one of T1/T2/T3; totals
     * T1/T2/T3 = 15/697/8,374). DERIVED by {@code domain-walk.py} -- a fork-independent,
     * corpus-only fact, correct at every head.
     */
    private static final int GOLDEN_DOMAIN = 3579;

    /**
     * The UNION domain pin (LAW 73) -- {@code -1} is the seat-30 PRINT-FIRST SENTINEL, not an
     * omission. It fails with the measured value in its own message; transcribe THAT number
     * here in the same commit (sentinel -> measured -> pin), never draft it. It cannot be
     * derived offline: the union is intersected with the fork's EMITTED key set, and this
     * suite's cell generator runs only the rule/report/function generators, so the emitted
     * set is a proper subset of the {@code GOLDEN_DOMAIN} above. Expect a value at or below
     * 3,579 (the golden walk measured ZERO fork-only token-bearing files).
     */
    private static final int DOMAIN_DRR700 = 1233;   // MEASURED at D2-green1.log: 'MEASURED DOMAIN = 1233 token-bearing files' (the print-first sentinel, transcribed)

    /** (T1, T2, T3) per file -- see {@code corpus_control1}'s javadoc. */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String flat = collapse(codeOnly(e.getValue()));
            int t1 = count(flat, TERNARY);
            int t2 = count(flat, ">ofNull()");
            int t3 = count(flat, ".getOrDefault(");
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

    /**
     * The residue assert runs BEFORE the domain sentinel deliberately: with the sentinel
     * ordered first (the seat-31 helper's order) a {@code -1} pin would hide the residue
     * verdict entirely, and the residue is the half this law's evidence lives in.
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
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means an"
                + " unpinned call site; MEASURED DOMAIN = " + universe.size()
                + " token-bearing files");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files ("
                + expectedDomain + ")");
    }

    // =========================================================================
    // Fixture harness (the InLambdaBoolHoistShadowSeatTest shape, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat32d2.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat32d2".equals(m.namespace()));
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
        return fixtureClass(ruleName);
    }

    private static String fixtureFunction(String funcName) throws IOException {
        return fixtureClass(funcName);
    }

    private static String fixtureClass(String simpleName) throws IOException {
        Render r = render();
        String path = simpleName + ".java";
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
            throw new AssertionError("[ExtractBodyMultiDefaultTernarySeatTest] builtins parse"
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

    private static void lockCell(Map<String, String> output, List<String> genErrors,
            Path goldenRoot, String path, String cell) throws IOException {
        assertNotNull(output, cell + " generation did not run - corpus unavailable?");
        List<String> own = genErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + path + ": " + own);
        String gen = output.get(path);
        assertNotNull(gen, "not generated: " + path);
        assertEquals(normalize(Files.readString(goldenRoot.resolve(path))), normalize(gen),
                path + " must byte-match golden in " + cell + " - seat 32 law D2"
                + " (extractBodyMultiDefaultTernary) ON TOP OF law D1 (the LEFT deref)");
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

}
