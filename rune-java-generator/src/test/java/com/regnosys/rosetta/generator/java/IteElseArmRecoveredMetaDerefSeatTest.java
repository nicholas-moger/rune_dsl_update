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
 * SEAT 31, law 1 (the seat-30 law-9 bank, re-landed) -- facet {@code iteElseArmRecoveredMetaDeref}: <b>the STATEMENT-hoist ITE seat
 * gets the two else-arm meta-deref evidence channels its BLOCK-LAMBDA sibling has had since
 * PR #354 / PR #388.</b> LAW 69 -- the sibling walk already knows the law this one does not.
 *
 * <p><b>The asymmetry, stated exactly.</b>
 * {@code CollectionHandler.compileEffectiveElseConditionalBlock} derefs a meta else arm to the
 * bare join through THREE channels:
 * <pre>
 *   (1) :9377  #339 deepBareInvokableThenHoist   AST bare-sibling evidence + COMPILED wrapper else
 *   (2) :9402  #354 nullTypedElseMetaDeref       AST evidence + TYPE-LESS else, wrapper recovered
 *                                                from the arm NODE (recoverExprMetaWrapper)
 *   (3) :9445  #388 ruleCondBaseEvalArgDeref     COMPILED bare-sibling evidence + COMPILED wrapper
 *                                                else + joinItem.equals(wrapper.getValueType())
 * </pre>
 * {@code FunctionExpressionRenderer.appendIteHoistChainCore} has ONLY (1). Its
 * {@code anyBareThenArmEvidence} is {@code CollectionHandler.isProvablyBareJoinArm}, which is
 * AST-only -- "an arithmetic/to-string/literal/count shape, never a nav, symbol or empty" -- so a
 * ladder whose bare rung arm is a plain NAVIGATION carries no evidence at all and its else arm can
 * never deref, whatever its type says. This law adds (3) as <b>rung A</b> and (2) as <b>rung B</b>.
 *
 * <p><b>Carriers, and which rung each needs.</b>
 * <ul>
 *   <li><b>rung A</b> -- {@code drr 5.61.0 asic CustomBasketCodeIdentifierRule.java}.
 *       {@code final MapperS<String> ifThenElseResult;}; the rung arm {@code customBasketCode}
 *       compiles {@code MapperS<String>} (bare, == the join item); the else arm is a block-lambda
 *       ladder whose typed empty renders {@code MapperS.<FieldWithMetaString>ofNull()}. Its
 *       compiled type is {@code null} TODAY and becomes {@code MapperS<FieldWithMetaString>} under
 *       seat-30 <b>LAW 2</b> (facet {@code blockLambdaSingleItemChainStamp}).
 *       <b>CONJUNCTIVE: this file needs LAW 2 and rung A</b> (the seat-29 law-9/9b precedent).
 *       1 hunk / 2 lines -&gt; WHOLE-FILE HEAL, locked HERE because this is the later commit.</li>
 *   <li><b>rung B</b> -- {@code drr 5.61.0 asic PlatformIdentifierRule.java}. The rung arm
 *       {@code ... -> micData -> mic} compiles {@code MapperS<String>} (so rung A's evidence half
 *       is satisfied), but the else arm is {@code then extract <meta-output rule>}, which the #265
 *       {@code ruleThenValueMetaWrap} renders as a block that CONSTRUCTS its wrapper in a ternary:
 *       <pre>
 *   item -&gt; { final String string = cDEPlatformIdentifierRule.evaluate(item.get());
 *             return string == null ? MapperS.&lt;FieldWithMetaString&gt;ofNull()
 *                  : MapperS.of(FieldWithMetaString.builder().setValue(string).build()); }
 *       </pre>
 *       That block publishes no arm join, so {@code LambdaCompiled.chainItemType()} is
 *       structurally null and LAW 2 provably cannot reach it. <b>MEASURED</b> (LAW 75,
 *       {@code [PROBE30-MSTI]}, one instrumented D11 round, 25 cells, BOTH routes): all 25
 *       {@code mapM=mapSingleToItem} rows for {@code rule:PlatformIdentifier} read
 *       {@code chainItem=- chainWrapper=false missed=false}. The wrapper is reachable ONLY from
 *       the arm NODE. 1 hunk / 2 lines -&gt; WHOLE-FILE HEAL.</li>
 *   <li><b>rung B, mas -- NOT this law's.</b> {@code drr 5.61.0 mas PlatformIdentifierRule.java}
 *       was drafted here as a partial; the seat-31 LAW-75 round REFUTED the siting: mas never
 *       reaches FER's ladder at all ({@code [P31-MASFORM]}/{@code [P31-MASWRAP]}, ROUTE-IDENTICAL
 *       -- it falls through to {@code ControlFlowHandler.hoistAsItemLocalOrNull}) and was healed
 *       WHOLE later in this seat by law 1b ({@code mapperFormRuleRootArmKeep}; LAW 80 measured
 *       ZERO text movers). This law claims TWO whole-file heals -- its own carriers, asic PID +
 *       CBCId -- not three.</li>
 * </ul>
 *
 * <p><b>The charter's siting is REFUTED and this is the replacement.</b> The charter sited the mas
 * half at {@code ControlFlowHandler}:1153/:1186 ({@code derefDeepThenIteArmOrKeep}'s erased-type
 * gate). {@code [PROBE30-ITEARM]}, sited at that method's only call loop, measured <b>ZERO rows on
 * either PlatformIdentifierRule</b>, both routes -- the helper is never reached for these files --
 * and its only population is 5 rows on the GREEN {@code DTCC_UnderlyingAssetReportRule}. The seat
 * is FER, not CFH.
 *
 * <p><b>THE GREEN GATE IS THE VALUE-TYPE CONJUNCT, and it is measured off the goldens.</b> A
 * census over all 174,141 goldens found exactly <b>31</b> files carrying BOTH a #265
 * construct-block AND an {@code ifThenElseResult} ladder:
 * <pre>
 *    2  drr 5.61.0  PlatformIdentifierRule (asic + mas)      local = MapperS&lt;String&gt;             CARRIERS
 *   29  drr 5.61.0 / 6.34.1 / 6.35.0 / 6.36.0 / 6.37.0 / 6.38.0
 *       PriorUti / PriorUTI[Proprietary]Rule                 local = MapperS&lt;FieldWithMetaString&gt;  GREEN
 * </pre>
 * In the 29 green files the hoisted local's ITEM TYPE IS the wrapper -- golden keeps both arms
 * un-deref'd and the whole-output {@code fieldWithMetaString.getValue()} ladder does the
 * unwrapping -- so an ungated deref breaks every one of them.
 * {@code itemType.equals(<wrapper>.getValueType())} declines all 29 and admits both carriers. It
 * is the SAME equality the sibling seats already apply (#388's
 * {@code joinItem.equals(elseWrapJoin.getValueType())};
 * {@code ControlFlowHandler.derefDeepThenIteArmOrKeep}'s
 * {@code if (!itemType.equals(metaItem.getValueType())) return arm;}) and it is what
 * {@code recoverInnerRuleMetaWrapper}'s javadoc means by "the CALLER applies the
 * value-type-equality gate".
 *
 * <p><b>What this fixture set can and cannot prove (LAW: a control scans the domain it claims).</b>
 * The 29 green files decline on TWO independent conjuncts -- they carry no bare-arm evidence at
 * all (their then arm IS the meta construct block) AND their join item is the wrapper -- so a
 * fixture cannot separate the two: a rune ladder whose join is the wrapper cannot also carry a
 * bare-evidence arm, because rune would then join it bare. The shape therefore has NO fixture
 * pin (the b1 fixture was struck at the suite's creation -- the wire-or-delete note in Part B) and
 * the value-type conjunct's only claimed proof lived in
 * {@code corpus_control1} / {@code corpus_control3}, whose domains contain all 31 files by
 * construction. <b>{@code m-law1-valuetype} is expected to be caught by the CORPUS controls, not
 * by b1</b> -- stated here rather than discovered when the mutation measures a smaller set than
 * claimed. If the corpus controls ALSO come back green under that mutation, the conjunct is not
 * load-bearing at this corpus and the law must be re-scored before merge.
 *
 * <p><b>RED at the pre-law head -- MEASURED at the seat-31 chain's RED leg ({@code 0c8fc7dc4},
 * BOTH routes; {@code f31-red-default.log} / {@code f31-red-on.log}): a1, a2, corpus_c1,
 * corpus_c2, corpus_c3, corpus_control1</b> (+ corpus_control2 on {@code -Pir-on}, the IR-route
 * control the default profile skips); control0 and control3 GREEN in both states (b1/b2 do
 * not exist -- struck at creation, Part B). The
 * claim had said "a1 [only with LAW 2 applied]": a1 failed at RED WITHOUT any other law, so that
 * qualifier is withdrawn. c3 (the mas positive lock promoted at law 1b) is in the RED set because
 * law 1b lands AFTER this base -- the within-seat sequencing, not a defect. GREEN at the head:
 * 9/0F/1skip default, 9/0F/0skip {@code -Pir-on}.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82). Every set below is transcribed from the chain's
 * own {@code f31-mut-<lane>.log} at {@code f2a4d5c0}; the two lanes the external java.exe kill
 * truncated inside the chain ({@code m-law1-recover}, {@code m-law1-scope}) were re-measured
 * standalone at the same head ({@code f31-mut-<lane>-rerun.log}). Each correction the
 * measurement forced is named in place.</b>
 * <ul>
 *   <li><b>m-law1</b> ({@code law1s-apply.py --revert}): MEASURED 9/5F/1S = a1, a2, corpus_c1,
 *       corpus_c2, corpus_control1 -- exactly the claim (MATCH).</li>
 *   <li><b>m-law1-compiled</b> ({@code --mut-compiled}, rung A disabled): MEASURED 9/3F/1S = a1,
 *       corpus_c1, <b>corpus_control1</b>; a2 / corpus_c2 unmoved -- the two rungs ARE separable
 *       channels, as claimed. The claim under-named the set by one: control1 is the drr 5.61.0
 *       whole-cell lock and it sees the CustomBasketCodeIdentifier carrier regress
 *       ({@code fork=[3, 0, 0] golden=[4, 0, 0]} entering its residue print) whenever c1 does.</li>
 *   <li><b>m-law1-recover</b> ({@code --mut-recover}, rung B disabled): MEASURED 9/3F/1S = a2,
 *       corpus_c2, <b>corpus_control1</b>; a1 / corpus_c1 unmoved. The same under-naming: control1
 *       sees the asic PlatformIdentifier carrier regress ({@code fork=[0, 0, 0] golden=[1, 0, 0]}).</li>
 *   <li><b>m-law1-valuetype</b> ({@code --mut-valuetype}, both rungs' equality severed): <b>MEASURED
 *       EMPTY, 9/0F/1S</b> -- a full run, BUILD SUCCESS, every test green. The claim (control1 and
 *       control3 fail, naming the 29 PriorUti/PriorUTI files) is <b>REFUTED</b>, and the fixture-reach
 *       paragraph above had already written down what that outcome means: the 29 wrapper-join files
 *       decline on the bare-arm-evidence conjunct ALONE, so the value-type equality is NOT
 *       load-bearing at this corpus. <b>Re-scored:</b> the equality is DEFENCE-IN-DEPTH -- the same
 *       gate the #388 / #354 siblings apply, kept for the future corpus where a bare-evidence ladder
 *       joins the wrapper. <b>MEASURED: no fixture, no corpus control and no lane moves when the
 *       equality is severed (9/0F in BOTH chain runs)</b> -- the conjunct ships UNWITNESSED at
 *       this corpus, kept as defence-in-depth by parity with the #388/#354 siblings, and its
 *       witness is BANKED (the first corpus where a bare-evidence ladder joins the wrapper).
 *       Stated here so no reader takes the lane's zero for a measurement of nothing: it measured
 *       that a conjunct the draft called load-bearing is not, at this corpus.</li>
 *   <li><b>m-law1-scope</b> ({@code --mut-scope}, both rungs' coercion scope swapped from the
 *       open CHILD back to the statement scope): MEASURED 9/3F/1S = <b>a2</b>, corpus_c2,
 *       corpus_control1. TWO corrections. (1) <b>a2 FAILS</b> ("the fixture must reach the #265
 *       ternary construct block, else a2 proves nothing about rung B") -- the fixture's own
 *       statement scope IS closed at its else render, exactly like the carrier's, so the swap
 *       crash-stubs the fixture too; the seat-30 round-3 caution ("the fixture CANNOT witness the
 *       crash conjunct") was over-cautious at THIS fixture, and the lane now has a fixture witness
 *       as well as the corpus one. (2) control1 fails on its TRIPLE assert, not on {@code lockA}'s
 *       generation-error assert: the asic carrier enters the residue print as
 *       {@code fork=[0, 0, 0] golden=[1, 0, 0]} (the stubbed body carries no token) while
 *       {@code drr561GenErrors} stays EMPTY -- the closed-scope refusal is rendered as a stub, not
 *       reported through the generation-error channel, so the whole-cell triple is the witness
 *       that fires. The stub is in the lane's own print, twice, verbatim: {@code /* TODO: expression
 *       compilation error: Cannot create a new identifier in a closed scope. ({unique token for
 *       "fieldWithMetaString"} -> fieldWithMetaString)} -- the coercion's lambda param minted into
 *       the closed statement scope, exactly the seat-30 crash. The crash conjunct is load-bearing,
 *       as claimed; the channel it shows on was mis-named.</li>
 * </ul>
 *
 * <p><b>THE CLOSED-SCOPE CRASH, and the child-scope law (the seat-31 amendment; MEASURED by the
 * seat-31 LAW-75 round).</b> The seat-30 rework already handed the arm UNRENDERED to
 * {@code TypeCoercionService.coerce} and STILL crashed at the asic carrier
 * ("Cannot create a new identifier in a closed scope"). {@code [P31-ITE]} explains it: the asic
 * carrier's statement scope reads {@code closed=false} at preThenRender/postThenRender/
 * preElseCompile and {@code closed=true} at preElseRender -- the ONLY closed=true row in the
 * carrier set, ROUTE-IDENTICAL -- and the joined {@code [P31-CLOSE]} attributes the closure to
 * {@code getActualName}'s ancestor walk DURING THE ELSE COMPILE/DRAIN WINDOW (something inside
 * the else compile resolves an identifier, which closes the statement scope as an ancestor). So
 * ANY coercion that registers its lambda param into the statement scope after the else compile
 * can crash there. The fix is the one the surviving #354 sibling already embodies: register into
 * an OPEN CHILD ({@code scope.lambdaScope()}) -- {@code createIdentifier} checks only the OWN
 * scope's {@code isClosed}, ancestors are read-only in the resolve walk, and deferred coercion
 * entries live on the ROOT scope's registry regardless of the registering child
 * ({@code JavaStatementScope.resolveDeferredCoercionNames} reads {@code rootStatementScope()}),
 * so sentinel resolution rides the existing finalize machinery unchanged. The existing #339
 * block deliberately KEEPS the statement scope: its green carriers resolve their params there
 * today and moving them would change grouping/escape bytes.
 *
 * <p><b>LAW 81 -- the cross-suite residue rows this law MOVES</b> (each must be re-pinned in the
 * SAME commit, transcribed from its own measured print, never edited by arithmetic):
 * {@code ChainMapperCRootRungsSeatTest} (asic + mas rows), {@code DispatchBaseInputDeclineSeatTest}
 * (asic + mas), {@code FilterPredicateMetaDerefSeatTest} (asic + mas),
 * {@code ThenWrappedDefaultSeatTest} (asic + mas), {@code SetTerminalRuleMultiSeatTest} (mas) --
 * all in their drr 5.61.0 residue lists. {@code RuleMetaWrapLambdaTransitiveTest} byte-locks the
 * cftc and jfsa {@code PlatformIdentifierRule} in drr 6.34.1, which are OUTSIDE the measured
 * 31-file domain: those are TRIPWIRES that must stay GREEN, not rows to re-pin.
 */
class IteElseArmRecoveredMetaDerefSeatTest {

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

    private static final String CBCI =
            "drr/regulation/asic/rewrite/trade/reports/CustomBasketCodeIdentifierRule.java";
    private static final String PI_ASIC =
            "drr/regulation/asic/rewrite/trade/reports/PlatformIdentifierRule.java";
    private static final String PI_MAS =
            "drr/regulation/mas/rewrite/trade/reports/PlatformIdentifierRule.java";
    /** One of the 29 measured GREEN domain files, used as the discriminating oracle. */
    private static final String PRIOR_UTI_GREEN =
            "drr/regulation/esma/emir/refit/trade/reports/PriorUtiRule.java";

    /** Cell A = drr 5.61.0 -- both carriers plus 11 of the 31 measured domain files. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = drr 6.36.0 -- the EMPTY-DOMAIN guard, chosen because it contains FOUR of the 29
     * green {@code PriorUti*} domain files and ZERO carriers. A dropped value-type conjunct shows
     * up here as a whole-cell mismatch rather than as silence.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-6.36.0");
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
     * a1 = RUNG A (compiled evidence). A rule-body conditional -- so it hoists as the STATEMENT
     *      {@code final MapperS<String> ifThenElseResult;} form, NOT a block lambda -- whose rung
     *      arm is a plain NAV to a bare string (no AST bare evidence, compiled item {@code String})
     *      and whose else arm is a block-lambda ladder with a {@code FieldWithMetaString} typed
     *      empty. Requires LAW 2 to supply the else type; a1 is therefore RED at the law-2 head
     *      and GREEN at the law-9 head.
     * a2 = RUNG B (AST recovery). The same ladder, but the else arm is an extract over a
     *      META-OUTPUT inner RULE, which the #265 wrap renders as the ternary construct block --
     *      no arm join, no chain type, so only the arm-NODE walk can see the wrapper. Independent
     *      of LAW 2 by construction.
     * b1 = [STRUCK at creation -- Part B] THE 29-GREEN-FILE SHAPE ({@code PriorUti}): a ladder BOTH of whose arms are meta, so
     *      the join item IS the wrapper. No deref may appear and the wrapper-typed local must
     *      survive. (It declines on two independent conjuncts -- see the class javadoc.)
     * b2 = [STRUCK at creation -- Part B] the {@code ruleScopedIte} pin: the SAME shape inside a FUNCTION must not move.
     *
     * <p><b>PARSE-VERIFY BEFORE ASSERTING.</b> The two {@code a} fixtures must be confirmed to
     * render the carrier SHAPE (a1: {@code final MapperS<String> ifThenElseResult;} with a
     * {@code mapSingleToItem} block else arm ending in a typed wrapper empty; a2: the same with the
     * ternary construct block) at the pre-law head before their assertions are trusted. If the
     * bare rule-reference else arm in a2 does not reach the #265 lambda-terminal seat, use the
     * chain form {@code else marker then extract Seat31InnerIdent} instead -- that is the carrier's
     * own shape.
     *
     * <p>Lexer-safe identifiers: no {@code tag}, {@code single} or {@code label}.
     */
    private static final String MODEL = """
            namespace census.seat31ite
            version "1.0.0"

            type Seat31Ident:
                ident string (0..1)
                    [metadata scheme]

            type Seat31Holder:
                primaryIdents Seat31Ident (0..*)
                otherIdents Seat31Ident (0..*)

            type Seat31Venue:
                code string (0..1)
                venueCode string (0..1)
                holder Seat31Holder (0..1)

            func Seat31Allow: <"the IsAllowableActionForASIC twin (a FUNC, as in the real source) - the real carriers' leading filter pipe, which is what routes the conditional through the STATEMENT ite-hoist form">
                inputs:
                    venue Seat31Venue (1..1)
                output:
                    ok boolean (1..1)
                set ok:
                    venue -> mark exists

            reporting rule Seat31InnerIdent from Seat31Venue: <"the META-OUTPUT inner rule - its rosetta output is a [metadata scheme] leaf, so its Java evaluate() returns the BARE value and the #265 wrap rebuilds the wrapper inside the lambda">
                extract holder -> primaryIdents -> ident first
                as "inner"

            reporting rule A1IteCompiledEvidenceDerefsElseArm from Seat31Venue: <"a1 - RUNG A: a NAV rung arm (no AST bare evidence, compiled item String) joined with an else arm whose block-lambda ladder renders a FieldWithMetaString typed empty. Needs LAW 2 for the else type. REDUCED FROM the real asic CustomBasketCodeIdentifier source (the parenthesized extract-then-extract else chain - the first draft's unparenthesized form was an RMissingType PREMISE failure, the bank's a1/b1 lesson).">
                filter Seat31Allow
                then if venueCode exists
                    then venueCode
                    else (extract holder
                    then extract
                        (if primaryIdents exists
                        then primaryIdents -> ident first
                        else if otherIdents exists
                        then otherIdents -> ident first))
                as "a1"

            reporting rule A2IteRecoveredWrapperDerefsElseArm from Seat31Venue: <"a2 - RUNG B: the same NAV rung arm joined with an else arm that is an extract over a META-OUTPUT inner rule - the #265 ternary construct block, which publishes no arm join, so only the arm-NODE walk can see the wrapper">
                filter Seat31Allow
                then if venueCode exists
                    then venueCode
                    else (extract code
                    then extract Seat31InnerIdent)
                as "a2"
            """;

    /**
     * The deref both rungs emit -- the {@code WrappedItemCoercer} MapperS arm's fixed null-guarded
     * form, so the token is structural rather than a quoted accident.
     */
    private static final String DEREF =
            ">map(\"Type coercion\", fieldWithMetaString -> fieldWithMetaString == null"
            + " ? null : fieldWithMetaString.getValue())";

    // =========================================================================
    // Part A -- the positive fixtures
    // =========================================================================

    /**
     * a1 -- rung A. MEASURED at the seat-31 chain's RED leg ({@code f31-red-default.log}, the
     * fixture's own render quoted in the failure print): the pre-law else assignment ends
     * {@code return MapperS.<FieldWithMetaString>ofNull(); }); }} and is followed directly by
     * {@code output = ifThenElseResult.get();} -- no trailing {@code .<String>map("Type coercion",
     * …)} -- which is exactly the tail the negative assertion below names (after
     * {@code collapse}). The assertion is therefore a transcription of the measured pre-law text,
     * not a guess at it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_compiledBareRungEvidenceDerefsTheIteElseArm() throws IOException {
        String raw = rule("A1IteCompiledEvidenceDerefsElseArmRule.java");
        String code = collapse(codeOnly(raw));
        assertTrue(code.contains("final MapperS<String> ifThenElseResult"),
                "the fixture must reach the STATEMENT ite-hoist form, else a1 proves nothing about"
                + " appendIteHoistChainCore (LAW: a control scans the domain it claims):\n" + code);
        // DEREF carries the quoted "Type coercion" literal, which codeOnly() STRIPS - so it is
        // matched against the RAW render. The draft asserted it against codeOnly output, a
        // structurally impossible match; caught at the seat-31 RED leg and fixed here.
        assertTrue(collapse(raw).contains(DEREF),
                "the compiled-bare-rung ladder must deref its wrapper else arm:\n" + raw);
        assertTrue(!code.contains("MapperS.<FieldWithMetaString>ofNull(); }); }"),
                "the un-deref'd else assignment must be gone:\n" + code);
    }

    /**
     * a2 -- rung B. The else arm's wrapper exists only in the arm's AST; the compiled chain type
     * is structurally null, so this test fails for a reason a1 cannot reproduce.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_recoveredWrapperDerefsTheConstructedIteElseArm() throws IOException {
        String raw = rule("A2IteRecoveredWrapperDerefsElseArmRule.java");
        String code = collapse(codeOnly(raw));
        assertTrue(code.contains("FieldWithMetaString.builder().setValue("),
                "the fixture must reach the #265 ternary construct block, else a2 proves nothing"
                + " about rung B:\n" + code);
        // RAW match - see a1's codeOnly note.
        assertTrue(collapse(raw).contains(DEREF),
                "the constructed-wrapper else arm must deref through the arm-node recovery:\n" + raw);
    }

    // =========================================================================
    // Part B -- the decline pins
    // =========================================================================

    /*
     * b1 (the 29-green-file wrapper-join fixture) is DELETED per the same wire-or-delete demand
     * as b2, with the reason recorded: the synthetic model cannot reproduce the upstream
     * wrapper-local channel - its ladder local renders MapperS<String> (typed off the rule
     * output) where the real PriorUti green renders MapperS<FieldWithMetaString>, so the
     * fixture never reaches the shape it would pin and its pre-law render is itself ill-typed.
     * The wrapper-join decline witness is corpus_control0 (the REAL PriorUti oracle: wrapper
     * local asserted, deref absence asserted, fork byte-identity asserted) plus the 29-file
     * domains of corpus_control1/control3 - real bytes, strictly stronger than the fixture.
     */
    /*
     * b2 (the ruleScopedIte pin) is DELETED per the draft's own wire-or-delete demand, with the
     * reason recorded here: corpus_control1/control3 are whole-cell UNION scans over trees that
     * INCLUDE the function files (generateCell collects funcGen.generateWithErrors), so a rung
     * firing on the function path moves those scans; a fixture-level function twin would also be
     * ambiguous, because functions LEGITIMATELY deref meta at set seats and "no deref" is not a
     * valid function-path assertion. ruleScopedIte's pin is the corpus controls, BY DESIGN.
     */

    // =========================================================================
    // Part C -- the whole-file locks
    // =========================================================================

    /** corpus_c1 -- the rung-A carrier (CONJUNCTIVE with LAW 2). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr561CustomBasketCodeIdentifierRuleMatchesGolden() throws IOException {
        lockA(CBCI);
    }

    /** corpus_c2 -- the rung-B carrier. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_drr561AsicPlatformIdentifierRuleMatchesGolden() throws IOException {
        lockA(PI_ASIC);
    }

    /**
     * corpus_c3 -- the mas twin, PROMOTED to a whole-golden lock: byte-identical at this head,
     * healed WHOLE by law 1b ({@code 4e5f7e115}; primary lock {@code MapperFormRuleRootArmSeatTest}
     * corpus_c1, this the promoted cross-suite tripwire that fired exactly as its own message
     * prescribed). The draft had declared it a PARTIAL repair; the HISTORY comment inside keeps
     * the pin's meaning.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c3_drr561MasPlatformIdentifierRuleMatchesGolden()
            throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        String gen = drrAOutput.get(PI_MAS);
        assertNotNull(gen, "not generated: " + PI_MAS);
        // HISTORY, kept so the pin's meaning survives: the seat-30 draft CLAIMED the mas hunk
        // heals through law 1's rung B; the seat-31 LAW-75 round REFUTED the siting (mas never
        // reaches FER's ladder - it falls through to ControlFlowHandler.hoistAsItemLocalOrNull,
        // [P31-MASFORM]/[P31-MASWRAP], ROUTE-IDENTICAL), and this test then pinned "law 1
        // leaves mas untouched". LAW 1b landed at the CFH seat (facet
        // mapperFormRuleRootArmKeep) and healed the file WHOLE, firing this pin exactly as its
        // message prescribed - so it is PROMOTED to the positive whole-golden lock, the same
        // promotion the seat-29/30 heal-tripwires took.
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(PI_MAS))), normalize(gen),
                "mas PlatformIdentifierRule must byte-match golden (healed by seat-31 law 1b;"
                + " the primary lock is MapperFormRuleRootArmSeatTest corpus_c1 - this is the"
                + " promoted cross-suite tripwire)");
    }

    // =========================================================================
    // Part D -- the corpus controls (LAW 79 UNION scans)
    // =========================================================================

    /**
     * control0 -- golden is the oracle and it DISCRIMINATES: the asic carrier's golden carries the
     * deref, the green {@code PriorUtiRule} golden does not (its join item is the wrapper), and the
     * fork emits that green file byte-identically today.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenDerefsTheCarrierAndNotTheWrapperJoinSibling() throws IOException {
        String gCarrier = collapse(Files.readString(GOLDEN_A.resolve(PI_ASIC)));
        assertTrue(gCarrier.contains(DEREF),
                "golden must carry the deref at the asic carrier's else assignment");
        Path green = GOLDEN_A.resolve(PRIOR_UTI_GREEN);
        assertTrue(Files.isRegularFile(green), "the green-domain oracle must exist: " + green);
        String gGreen = collapse(Files.readString(green));
        assertTrue(gGreen.contains("final MapperS<FieldWithMetaString> ifThenElseResult"),
                "the green oracle's ite local must be WRAPPER-typed - the discriminator this law's"
                + " green argument rests on");
        assertTrue(!gGreen.contains(DEREF),
                "golden must NOT deref the wrapper-join ladder's arms");
        String forkGreen = drrAOutput == null ? null : drrAOutput.get(PRIOR_UTI_GREEN);
        assertNotNull(forkGreen, "the fork must emit the green domain file");
        assertEquals(normalize(Files.readString(green)), normalize(forkGreen),
                "the green domain file must stay byte-identical (the no-move witness)");
    }

    /** control1 -- LAW 79, the whole-cell UNION scan on drr 5.61.0 (both carriers + 11 domain files). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr561WholeCellEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        assertTrue(drrAOutput.containsKey(PRIOR_UTI_GREEN),
                "the green PriorUti family must be INSIDE this scan's domain, else control1 proves"
                + " nothing about the value-type conjunct");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * control2 -- LAW 77, written as BOTH-ROUTES-vs-GOLDEN per the seat-30 standing lesson: a
     * route-PARITY control is not enough, two routes can agree by both being wrong. This law
     * lives in {@code FunctionExpressionRenderer}, the class the IR route substitutes, so the
     * ON-route render is compared against GOLDEN for both whole carriers (the legacy route is
     * already golden-locked by corpus_c1/c2, so route agreement follows transitively); the mas
     * twin -- golden-identical since law 1b -- keeps the RAW-bytes parity form (its IR-vs-golden
     * identity follows transitively from corpus_c3, which runs in the same ON leg).
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForBothCarriers() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CBCI))),
                normalize(irOut.get(CBCI)), "IR route vs GOLDEN: " + CBCI);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(PI_ASIC))),
                normalize(irOut.get(PI_ASIC)), "IR route vs GOLDEN: " + PI_ASIC);
        assertEquals(drrAOutput.get(PI_MAS), irOut.get(PI_MAS),
                "route divergence on the mas twin (golden-identical since law 1b): " + PI_MAS);
    }

    /**
     * control3 -- LAW 79 on drr 6.36.0: the EMPTY-DOMAIN guard. Zero carriers, FOUR of the 29
     * green {@code PriorUti*} domain files, so the law must be byte-inert across the whole cell.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr636WholeCellEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 6.36.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 6.36.0 reported a generation error - the scan is incomplete");
        assertTrue(drrBOutput.keySet().stream()
                        .anyMatch(k -> k.endsWith("/PriorUtiRule.java")
                                || k.endsWith("/PriorUTIRule.java")),
                "the green PriorUti family must be INSIDE this scan's domain, else control3 proves"
                + " nothing (LAW: a control scans the domain it claims)");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR636, DOMAIN_DRR636);
    }

    // =========================================================================
    // The measured residue + domains (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * MEASURED and transcribed VERBATIM from the control's own assertEquals print (the band
     * lists are SUPERSETS and were never consulted). The two rows are PRE-EXISTING band files
     * this seat's laws do not own: asic {@code EffectiveDateRule} and cftc
     * {@code NotionalCurrencyLeg1Rule} (the F14-adjacent B087-kin carrier). Each residue row
     * is a LAW-81 tripwire: it fires when the owning law lands, and the re-pin is transcribed
     * from that run's print in that law's commit. The mas {@code PlatformIdentifierRule} row
     * (fork=[0, 0, 0] golden=[1, 0, 0] at the law-1 head) LEFT this list when seat-31 law 1b
     * healed the file whole - re-pinned from the fired control's print. drr 6.36.0 measured
     * residue-free.
     */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();
            // the NotionalCurrencyLeg1Rule row (fork=[11, 0, 0] golden=[21, 0, 0]) LEFT this list at seat 32: law C.3's
            // ladder block made this scan's tuple equal golden's (the file stays BANDED on its wrapper-hop residue);
            // transcribed from the print (C3-trip1b.log).
    private static final List<String> KNOWN_RESIDUE_DRR636 = List.of();

    /**
     * The print-first domain pins (LAW 73), transcribed from the sentinel's own failure print at
     * the law-1 head: {@code MEASURED DOMAIN = 458} (drr 5.61.0) / {@code 524} (drr 6.36.0)
     * token-bearing files.
     */
    private static final int DOMAIN_DRR561 = 458;
    private static final int DOMAIN_DRR636 = 524;

    /**
     * (T1, T2, T3) per file:
     * <ul>
     *   <li><b>T1</b> -- the law's ADDED shape: {@code >map("Type coercion",} hops.</li>
     *   <li><b>T2</b> -- the seat's own footprint: {@code ifThenElseResult} occurrences, so a
     *       change in the ite-hoist STRUCTURE (not just the arm text) is caught too.</li>
     *   <li><b>T3</b> -- the OVER-FIRE NET: total {@code .getValue())} occurrences. Deliberately
     *       global, because the green failure mode is a wrapper that gets unwrapped one level too
     *       early -- it shows up as a getValue count change anywhere in the cell, including at
     *       seats this law does not name.</li>
     * </ul>
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String flat = collapse(e.getValue());
            int t1 = count(flat, ">map(\"Type coercion\",");
            int t2 = count(codeOnly(flat), "ifThenElseResult");
            int t3 = count(codeOnly(flat), ".getValue())");
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
                + " token-bearing files: " + universe);
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
    // Harness (the seat-29 SetTerminalRuleMultiSeatTest shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.36.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
        }
    }

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run - corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 5.61.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for "
                + path + " - seat 31 law 1: the statement-hoist ITE seat derefs a meta else arm to"
                + " the bare join through the compiled-evidence and arm-node-recovery channels its"
                + " block-lambda sibling already has.");
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
                    "the IR provider must be resolvable under -Pir-on, else this is not an"
                    + " ON-route render");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got "
                    + funcGen.getClass());
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

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat31ite.rosetta");
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
            fixtureOut = render(m -> "census.seat31ite".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
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
            throw new AssertionError("[IteElseArmRecoveredMetaDerefSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
