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
 * SEAT 32, law B.1 -- facet {@code iteChainCtorArmMapperWrap}: <b>a CONSTRUCTOR arm assigned
 * into the ite-chain hoist's Mapper-typed local wraps at the assignment</b>. Golden drr
 * 7.0-7.3 {@code FXLeg1}/{@code FXLeg2}:
 * {@code ifThenElseResult = MapperS.of(Cashflow.builder()...build());} against the fork's bare
 * {@code ifThenElseResult = Cashflow.builder()...build();} -- <b>2 arms x 8 files</b>, 2 diff
 * lines per arm, everything between byte-identical (golden drr 7.0.0 {@code FXLeg1.java}:97 and
 * :109, closing at :107 and :122).
 *
 * <p><b>The defect.</b> {@code FunctionExpressionRenderer.appendIteHoistChainCore} mints its
 * local Mapper-typed UNCONDITIONALLY ({@code String resultType = (multi ? "MapperC<" :
 * "MapperS<") + iteDeclElement + ">";}), and its arm pipelines had SEVEN rungs -- resolved enum
 * constant, bare invocation, BigInteger literal, distinct-collapse, scalar-literal-to-singleton,
 * ComparisonResult-to-{@code asMapper}, explicit-empty-to-{@code ofNull} -- and NO constructor
 * rung at all. A constructor compiles to the RAW builder chain (never a Mapper), so it fell
 * through to a bare builder assigned to a {@code MapperS<X>} local.
 *
 * <p><b>TWO PIPELINES, ONE SHARED WALK -- the re-siting this suite was rewritten for.</b> That
 * method renders its arms through TWO parallel chains: the per-rung one (FER:9430 -> 9451) and
 * the TERMINAL ELSE (FER:9692 -> 9701), each with its own {@code if (mapperFormArms) ... else if
 * (valueMetaWrapTo ...) ... else { ... }} ladder. The LAW-75 probe prints ONE ctor arm per file
 * per cell at the rung seat, but {@code [P32-B1D]} shows
 * {@code thenKind=RConstructorExpr elseKind=RConstructorExpr}: <b>both</b> carrier arms are
 * constructors, one at each pipeline. A rung-only fix heals 4 of the 8 diff lines per file and
 * moves NO whole file -- the LAW-80 trap a count digest cannot see. The law therefore lands the
 * SAME shared call at BOTH seats, and {@code m-lawB1-elseonly} / {@code m-lawB1-thenonly} are
 * the lanes that hold it there.
 *
 * <p><b>The fix is an EXTRACTION, not a new predicate (LAW 69).</b> The sibling seat one level
 * down, {@code ControlFlowHandler.wrapDeepThenIteArm}, has carried exactly this rung since
 * PR #398 (facet {@code fnNotionalTogetherRestructure}). That rung moved VERBATIM into
 * {@code HandlerHelper.wrapCtorIteArm} -- same predicate, same order, same emitted strings, same
 * {@code refs} additions -- and all THREE call sites now consult it: the deep-then ladder
 * byte-frozen (locked by {@code corpus_c3} + {@code e4}), and both ite-chain pipelines gaining
 * the rung FIRST in their {@code else} chain -- a constructor arm reaches none of the other six:
 * five key on a different arm-node kind, and the sixth (the BigInteger-literal coercion) is
 * gated on a rule-scoped BigDecimal element AND on the arm rendering
 * {@code MapperS.of(new BigInteger("}, which a wrapped constructor is not. This is the shape
 * {@code HandlerHelper.comparisonResultIteArmNeedsAsMapper} (seat 23, law F21) already
 * established at these exact seats.
 *
 * <p><b>Green-safety -- the DISCRIMINATOR is the seat's declaration, and it was MEASURED over
 * the FULL population</b> (all 174,141 goldens, 25 cells, an {@code os.walk} that resolves each
 * assigned local back to its own declaration in the same file): the fork's pre-fix form
 * ({@code <local> = <Ctor>.builder()}) occurs <b>4,876</b> times over <b>48</b> basenames and
 * the declaration is Mapper-typed in <b>ZERO</b> of them -- every one is an ITEM-typed local at
 * a THIRD seat ({@code ControlFlowHandler.hoistAsItemLocalOrNull}, {@code mapperFormSlot ==
 * false}) that this law never reaches. Golden's post-fix form occurs <b>97</b> times over seven
 * basenames: the 16 band {@code FXLeg1}/{@code FXLeg2} rows, and green
 * {@code Notional} 20, {@code NotionalLeg} 20, {@code Create_CounterpartySpecificData} 18,
 * {@code _2} 18, {@code TechnicalRecordId} 5. Both sides of that discriminator are witnessed in
 * ONE green file by {@code e5} ({@code final MapperS<...> ifThenElseResult2;} wrapped, and
 * {@code LegalPersonIdentification1__1 ifThenElseResult0 = null;} bare, in the same method).
 *
 * <p><b>The else-side blast radius, MEASURED and ZERO.</b>
 * {@code [P32-B1D] elseKind=RConstructorExpr} is 13 rows corpus-wide over three names:
 * {@code fn:FXLeg1} 4, {@code fn:FXLeg2} 4 and the GREEN {@code fn:TechnicalRecordId} 5. The
 * green one is {@code mapperFormArms=true multi=true}, so its else arm takes the
 * {@code wrapMapperFormIteArm} branch and never reaches the chain this law extends; its golden
 * already reads {@code ifThenElseResult = MapperC.of(Collections.singletonList(
 * drr.regulation.common.TechnicalRecordId.builder()...build()));}, and the shared walk's
 * {@code !startsWith("MapperC.of(")} conjunct would decline it a second time. Filtered to
 * {@code elseKind=RConstructorExpr mapperFormArms=false} the population is exactly the 8 carrier
 * renders, {@code multi=false}, route-identical. That near-miss is double-locked here: as a
 * reduced fixture ({@code e1}) and as its real corpus file, whole ({@code e2}, drr 6.34.1 --
 * the cell where its ladder actually lives; drr 7.x {@code TechnicalRecordId} carries no
 * {@code ifThenElseResult} at all), and it is pinned INSIDE {@code corpus_control3}'s domain.
 *
 * <p><b>Charter:</b> {@code FXLeg1} + {@code FXLeg2} x drr 7.0.0/7.1.0/7.2.0/7.3.0 FUNCTION = 8
 * WHOLE files (class C001; B002 and B003 are the only signatures those files carry, and the F9
 * wildcard half already landed at seat 28 -- fork and golden both read
 * {@code final MapperS<? extends PayoutBase> thenArg;}).
 *
 * <p><b>LAW 77.</b> {@code IRFunctionExpressionRenderer extends FunctionExpressionRenderer} and
 * overrides ONLY {@code ifThenElseResultBaseName} (:111), {@code thenArgBaseName} (:158),
 * {@code booleanHoistBaseName} (:201) and {@code renderSetAssignmentStatement} (:248);
 * {@code appendIteHoistChainCore} and {@code appendThenConditionalBlock} INHERIT, and
 * {@code IRControlFlowHandler} overrides only {@code ifThenElseResultBaseName} (:78), so
 * {@code wrapDeepThenIteArm} inherits too. {@code rune-ir-java} contains no
 * {@code appendIteHoistChainCore} / {@code wrapEnumRungInMapperSOf} /
 * {@code wrapMapperFormIteArm} re-implementation, and neither {@code ConditionalRenderer} nor
 * {@code ConstructRenderer} touches the ite-arm assignment. Route-safe BY INHERITANCE -- no IR
 * twin. {@code corpus_control2} measures the IR route against GOLDEN under {@code -Pir-on}.
 *
 * <p><b>LAW 74 -- MEASURED, not analytic.</b> {@code javac32/javac32-report.md} row C2: the
 * fork's current text does NOT compile at these 8 files -- {@code Seat32Pre.java:464} and
 * {@code :479}, both {@code error: incompatible types: Cashflow cannot be converted to
 * MapperS<Cashflow>}, each pointing at an arm's {@code .build();}. ONE PER ARM, and the POST
 * block is EXIT=0. A LAW-74 repair -- but only with BOTH seats patched: a rung-only fix clears
 * :464 and leaves :479.
 *
 * <p><b>CLAIMED RED at the seat's base</b> (both routes; the chain measures it):
 * {@code a1}, {@code a2}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+
 * {@code corpus_control2} on {@code -Pir-on}). {@code e1}..{@code e5}, {@code corpus_c3} and
 * {@code corpus_control3} are GREEN in BOTH states -- they are the decline / preservation /
 * idempotence locks. CLAIMED GREEN at the head: 13/0F/1skip default, 13/0F/0skip
 * {@code -Pir-on}.
 *
 * <p><b>MUTATION LANES (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-lawB1-*.log}; six lanes, the drafter's optional sixth
 * included):</b>
 * <ul>
 *   <li><b>m-lawB1-revert</b> (BOTH {@code HandlerHelper.wrapCtorIteArm} calls deleted at the
 *       ite-chain seat, the extraction kept): MEASURED <b>13/5F/1S</b> = {@code a1},
 *       {@code a2}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} -- the claim
 *       exactly ({@code corpus_control2} is the {@code -Pir-on} member); {@code e1}..{@code e5},
 *       {@code corpus_c3}, {@code corpus_control3} GREEN.</li>
 *   <li><b>m-lawB1-elseonly</b> (the TERMINAL-ELSE call alone deleted): MEASURED
 *       <b>13/5F/1S</b>, the same five, with the per-seat counters printing exactly as designed
 *       -- {@code a1} {@code expected: <2> but was: <1>}, {@code a2} {@code expected: <3> but
 *       was: <2>}. The lane that would have caught the v1 draft, MEASURED catching it.</li>
 *   <li><b>m-lawB1-thenonly</b> (the PER-RUNG call alone deleted): MEASURED <b>13/5F/1S</b>,
 *       the same five, {@code a2} {@code expected: <3> but was: <1>} ({@code a1}
 *       {@code <2>/<1>}) -- the mirror witness distinguishes the two half-lanes numerically,
 *       as claimed.</li>
 *   <li><b>m-lawB1-kind</b> (the {@code armNode instanceof RConstructorExpr} test severed in
 *       the SHARED helper): MEASURED <b>31/5F/1S</b> (this suite's 13 + the 18 of the collateral
 *       {@code BlockLambdaSingleArmListLiftSeatTest}) = {@code e3}, {@code corpus_c1},
 *       {@code corpus_c2}, {@code corpus_control1}, {@code corpus_control3} -- FIVE of the eight
 *       claimed. <b>Re-scored:</b> {@code e1}/{@code e2} did NOT move, for the reason {@code e1}'s
 *       own javadoc gives -- the mapper-form ladder's else arm takes {@code wrapMapperFormIteArm}
 *       and never reaches the shared walk whose kind test the lane severs (the claim over-reached
 *       its own fixture); {@code corpus_c3} did NOT move either -- NOTIONAL's bare default arm is
 *       held by the walk's other conjuncts (the Mapper-typed-decl gate and the {@code startsWith}
 *       guards), which of the two is UNMEASURED by this lane; and the collateral pin
 *       {@code BlockLambdaSingleArmListLiftSeatTest.corpus_control3} was SILENT (18/0F) -- the
 *       "expected to fire" was a reading, not a measurement. The discriminator IS load-bearing
 *       (five members move, two of them whole-cell controls); it is load-bearing on a narrower
 *       population than the claim named.</li>
 *   <li><b>m-lawB1-startswith</b> (the {@code !ctorRendered.startsWith("MapperS.of(")} /
 *       {@code "MapperC.of("} conjunct severed): MEASURED <b>13/0F/1S -- EMPTY</b>, the outcome
 *       this javadoc named as possible. So it says what it said it would: {@code e5}'s
 *       pre-wrapped arm opens on the {@code ImportCollisionResolver.typeRef} private-use
 *       sentinel, not on {@code MapperS.of(}, so the conjunct never sees the wrapped form at
 *       this corpus; it is idempotence insurance inherited VERBATIM from the #398 sibling,
 *       unwitnessed here, kept because severing it in the shared walk would change the #398
 *       seat's contract. Not a measured guard; not a conjunct dropped.</li>
 *   <li><b>m-lawB1-cfh</b> (the drafter's optional sixth lane: the CFH seat's
 *       {@code if (!ctorWrapped.equals(ctorRendered))} guard made unconditional -- the LAW-69
 *       delegation half measured directly): MEASURED <b>13/0F/1S -- EMPTY</b>, against a claim of
 *       {@code corpus_c3}. EMPTY BY CONSTRUCTION, on reading the helper: when
 *       {@code wrapCtorIteArm} declines it returns its input text, so the unconditional
 *       assignment assigns an equal string -- the guard is an optimisation, not a discriminator,
 *       and the delegation is byte-neutral at {@code corpus_c3} either way.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code a1}, {@code a2}, {@code corpus_c1},
 * {@code corpus_c2}, {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on});
 * GREEN at the head 13/0F/1skip default, 13/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW 81 -- cross-suite tripwires: SCANNED at the drafting head, and the scan came back
 * clean.</b> {@code src/test} carries five suites naming {@code FXLeg1}/{@code FXLeg2} and none
 * of them moves: {@code FunctionMaxMinBodyCoercionTest} byte-locks both files but in <b>drr
 * 6.34.1</b>, whose {@code FXLeg1}/{@code FXLeg2} carry NO {@code ifThenElseResult} at all;
 * {@code MetaFaceShortFormSeatTest} holds them as drr 7.0.0 PARTIAL carriers on the
 * {@code -> location} meta tokens; {@code ThenSeatMultiDefaultTernarySeatTest} pins two
 * {@code MapperC.<UnitType>of(fxOptionPayout} alias-ternary counts in drr 7.0.0;
 * {@code WildcardLocalDeclSeatTest} and {@code FunctionThenItemTypingTest} mention them in prose
 * only. Of the suites that COUNT a {@code MapperS.of(} population,
 * {@code IntLiteralNumberSeatTest} counts only the {@code BigDecimal.valueOf} / bare-int forms,
 * {@code ThenChainFnValueWrapOnceSeatTest} only the exact
 * {@code MapperS.of(MapperS.of(<ident>.evaluate(...)))} double wrap, and
 * {@code ThenWrappedDefaultSeatTest}'s count is on its own fixture. Two STANDING locks are
 * FRIENDS of this law and the first thing an over-fire would break:
 * {@code VoidCtorHoistFqnComposeTest.drrFunctions_byteMatchGolden} byte-locks the drr 6.34.1
 * {@code TechnicalRecordId} (the else-seat near-miss) whole, and
 * {@code InputFormThenHoistComposeTest} byte-locks the fca
 * {@code Create_CounterpartySpecificData} pair in drr 6.34.1 (the already-wrapped population).
 * Nothing is edited in any of them here; the list is repeated in NOTES.md.
 */
class IteChainCtorArmMapperWrapSeatTest {

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

    // The two carriers, and the three GREEN locks that keep the shared walk honest.
    private static final String FXLEG1 = "drr/regulation/common/functions/FXLeg1.java";
    private static final String FXLEG2 = "drr/regulation/common/functions/FXLeg2.java";
    /**
     * corpus_c3 -- the LAW-69 preservation lock. This GREEN function carries FOUR
     * {@code thenArg0 = MapperC.of(Collections.singletonList(Measure.builder()...build()));}
     * sites (the extracted rung's MULTI branch, the only branch no fixture in this suite
     * witnesses) AND, in the same ladder, the seat's BARE default arm and its typed
     * {@code MapperC.<MeasureBase>ofNull()} terminal -- so a mis-extraction breaks it and so does
     * an over-firing kind test.
     */
    private static final String NOTIONAL =
            "drr/standards/iosco/cde/version1/quantity/functions/Notional.java";
    /**
     * e4 -- the ITEM-typed seat's GREEN representative:
     * {@code final OptionStrike ifThenElseResult;} with {@code ifThenElseResult =
     * OptionStrike.builder()} assigned bare. One of the 4,876-row family this law must not
     * touch, byte-locked whole.
     */
    private static final String ITEM_SEAT_GREEN =
            "cdm/ingest/fpml/confirmation/product/commodityoption/functions/"
            + "MapCommodityOptionPayout.java";
    /**
     * e5 -- BOTH sides of the discriminator in ONE green file. Its {@code assignOutput} declares
     * {@code final MapperS<OrganisationIdentification15Choice__1> ifThenElseResult2;} and assigns
     * it {@code MapperS.of(OrganisationIdentification15Choice__1.builder()...)} (golden's
     * post-fix form -- the idempotence witness), and in the same method declares
     * {@code LegalPersonIdentification1__1 ifThenElseResult0 = null;} and assigns it a BARE
     * {@code LegalPersonIdentification1__1.builder()} (the 4,876-row item-typed family). Whichever
     * seat mints the wrapped pair, the {@code !startsWith("MapperS.of(")} conjunct is what stops a
     * second wrap -- so this lock is the corpus witness for the {@code m-lawB1-startswith} lane.
     */
    private static final String WRAPPED_CTOR_GREEN =
            "drr/projection/iso20022/fca/ukemir/refit/margin/functions/"
            + "Create_CounterpartySpecificData.java";
    /**
     * e2 + corpus_control3 -- the ELSE-SEAT NEAR-MISS, the only GREEN
     * {@code elseKind=RConstructorExpr} row the LAW-75 probe found (5 rows, drr 6.34.1 ..
     * 6.38.0). Its ladder is {@code mapperFormArms=true multi=true}, so the else arm takes
     * {@code wrapMapperFormIteArm} and never reaches the chain this law extends. NOTE THE CELL:
     * drr 7.0.0-7.3.0 generate this function with no {@code ifThenElseResult} at all (measured
     * per cell), so the lock has to live in a 6.x cell -- drr 6.34.1, the same cell
     * {@code VoidCtorHoistFqnComposeTest} already byte-locks it in.
     */
    private static final String TECHNICAL_RECORD_ID =
            "drr/regulation/common/trade/link/functions/TechnicalRecordId.java";

    /** Cell A = drr 7.0.0 -- both carriers, three green locks, and the first whole-cell control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.3.0 -- the second cell (the law must hold across the 7.x family). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell C = drr 6.34.1 -- the else-seat near-miss and the second whole-cell control. */
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
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

    // =========================================================================
    // Fixtures.
    //
    // A1/A2 are reduced from test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
    // regulation-common-func.rosetta:483 `func FXLeg1` (its `set fxLeg1:` is a then-chain whose
    // LAST then-body is `if <cond> then <Ctor>{...} else <Ctor>{...}` over a model output).
    // The reduction keeps: the whole-output SET, the then-chain, the last-body conditional with
    // constructor arms of the OUTPUT type, the same-named attribute assignment
    // (`payerReceiver: payerReceiver` in the carrier, `ccy: ccy` here) and the model-typed
    // output that forces the `toBuilder(ifThenElseResult.get())` tail. It drops: the base
    // conditional, the aliases, the nested constructor and the meta wrappers -- none of which
    // the arm pipelines read. A1 has ONE rung arm + ONE terminal else (the carrier's own shape);
    // A2 adds an `else if` rung so the ladder is TWO rung arms + ONE terminal else, which is
    // what lets the two half-lanes be told apart by a count.
    //
    // E1 is reduced from test-corpus/drr/drr-6.34.1/rosetta-source/src/main/rosetta/
    // regulation-common-trade-link-func.rosetta:145 `func TechnicalRecordId` -- the MULTI
    // mapper-form ladder whose TERMINAL ELSE is a constructor. It keeps: the multi pipe, the
    // `then filter`, the parenthesised `then (if <nav> exists then <nav> else <Ctor>{...})`, the
    // `<attr> then distinct only-element` constructor argument, the `empty` argument and the
    // `then distinct only-element` collapse into a (1..1) output.
    //
    // E3 is A1 with NAVIGATION arms at both seats instead of constructors.
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat32b1
            version "1.0.0"

            type Src:
                ccy string (0..1)
                other string (0..1)
                amount number (0..1)
                out Out (0..1)

            type Holder:
                src Src (0..1)
                srcs Src (0..*)

            type Out:
                ccy string (0..1)
                amount number (0..1)

            func A1CtorIteArm: <"a1 - THE FXLeg1 SHAPE: a whole-output `set` whose then-chain's last body is a conditional with a CONSTRUCTOR in the rung arm AND in the terminal else, assigned into the seat's Mapper-typed local">
                inputs:
                    holder Holder (1..1)
                output:
                    result Out (0..1)
                set result:
                    holder -> src
                        then if ccy = "USD"
                            then Out {
                                ccy: ccy,
                                amount: amount
                            }
                            else Out {
                                ccy: other,
                                amount: amount
                            }

            func A2CtorIteLadder: <"a2 - THE SAME SHAPE with an `else if` rung: TWO rung arms + ONE terminal else, so m-lawB1-thenonly and m-lawB1-elseonly are told apart by a count">
                inputs:
                    holder Holder (1..1)
                output:
                    result Out (0..1)
                set result:
                    holder -> src
                        then if ccy = "USD"
                            then Out {
                                ccy: ccy,
                                amount: amount
                            }
                            else if ccy = "EUR"
                            then Out {
                                ccy: other,
                                amount: amount
                            }
                            else Out {
                                ccy: other,
                                amount: empty
                            }

            func E1MapperFormCtorElseArm: <"e1 - THE TechnicalRecordId SHAPE: a MULTI mapper-form ladder whose TERMINAL ELSE is a constructor - already wrapped by the mapper-form arm, so the new rung must never see it">
                inputs:
                    holder Holder (1..1)
                output:
                    result Out (1..1)
                set result:
                    holder -> srcs
                        then filter ccy = "USD"
                        then (if item -> out exists
                            then item -> out
                            else Out {
                                ccy: ccy then distinct only-element,
                                amount: empty
                            })
                        then distinct only-element

            func E3NonCtorIteArm: <"e3 - DECLINE: the SAME two seats with plain navigation arms keep the BARE Mapper form">
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..1)
                set result:
                    holder -> src
                        then if ccy = "USD"
                            then ccy
                            else other
            """;

    // =========================================================================
    // Part A -- the seats (CLAIMED RED at the base)
    // =========================================================================

    /**
     * a1 -- the heal at BOTH seats: the ladder's rung arm and its terminal else are both
     * constructors, and BOTH wrap {@code MapperS.of(...)} against the seat's Mapper-typed local.
     *
     * <p>The count is the point. {@code assertEquals(2, ...)} is what a rung-only or an
     * else-only fix fails on ({@code m-lawB1-thenonly} / {@code m-lawB1-elseonly}); a plain
     * {@code contains} would pass on half a heal, which is exactly how the v1 draft of this law
     * survived its own review until the LAW-75 probe measured
     * {@code thenKind=RConstructorExpr elseKind=RConstructorExpr}.
     *
     * <p>PIN AT RED: the first two asserts are FIXTURE-REACH pins that are GREEN in both states
     * -- {@code final MapperS<Out> ifThenElseResult;} proves the local is the Mapper-typed
     * ite-chain one, and {@code result = toBuilder(ifThenElseResult.get());} proves the Shape-A
     * block was emitted rather than the ternary fallback (golden {@code FXLeg1} closes its own
     * block with {@code fxLeg1 = toBuilder(ifThenElseResult.get());}). If either fails, the
     * fixture is not reaching {@code appendIteHoistChainCore} -- reshape the fixture, never
     * weaken the assert.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_ctorArmsWrapAtBothIteSeats() throws IOException {
        String out = fixtureFunction("A1CtorIteArm");
        assertContains(out, "final MapperS<Out> ifThenElseResult;");
        assertContains(out, "result = toBuilder(ifThenElseResult.get());");
        assertEquals(2, count(out, "ifThenElseResult = MapperS.of(Out.builder()"),
                "BOTH constructor arms must wrap - the rung arm at the per-rung pipeline and the"
                + " terminal else at its own. One occurrence means one seat was patched and the"
                + " law heals half of each carrier file:\n" + out);
        assertEquals(2, count(out, ".build());"),
                "each wrapped arm closes its builder chain inside the wrap:\n" + out);
        assertEquals(0, count(out, "ifThenElseResult = Out.builder()"),
                "no bare builder assignment against the Mapper-typed local may survive:\n" + out);
    }

    /**
     * a2 -- the same shape with an {@code else if} rung, so the ladder is TWO rung arms and ONE
     * terminal else. This is what tells the two half-lanes apart by a count:
     * {@code m-lawB1-elseonly} leaves 2 wrapped, {@code m-lawB1-thenonly} leaves 1.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_threeArmLadderWrapsBothRungsAndTheTerminalElse() throws IOException {
        String out = fixtureFunction("A2CtorIteLadder");
        assertContains(out, "final MapperS<Out> ifThenElseResult;");
        assertEquals(3, count(out, "ifThenElseResult = MapperS.of(Out.builder()"),
                "two rung arms + one terminal else = three wrapped constructor arms; 2 means the"
                + " terminal-else seat is unpatched, 1 means the per-rung seat is:\n" + out);
        assertEquals(0, count(out, "ifThenElseResult = Out.builder()"),
                "no bare builder assignment against the Mapper-typed local may survive:\n" + out);
    }

    // =========================================================================
    // Part E -- the decline locks (GREEN in BOTH states)
    // =========================================================================

    /**
     * e1 -- the ELSE-SEAT near-miss as a fixture: a MULTI mapper-form ladder whose TERMINAL ELSE
     * is a constructor. {@code mapperFormArms} is true, so the else arm takes
     * {@code wrapMapperFormIteArm} and never reaches the chain this law extends -- and even if
     * it did, the shared walk's {@code !startsWith("MapperC.of(")} conjunct declines an
     * already-wrapped arm. Bytes unchanged in both states.
     *
     * <p>PIN AT RED: {@code final MapperC<Out> ifThenElseResult;} is a FIXTURE-REACH pin. If it
     * fails, this fixture is not landing on the mapper-form MULTI ladder at all -- reshape it
     * against {@code func TechnicalRecordId} (the multi pipe, the {@code then filter}, the
     * parenthesised {@code then (if ... else <Ctor>{...})}, the {@code then distinct
     * only-element} collapse) rather than weakening the assert. {@code e2} carries the same lock
     * on the REAL corpus file and is reach-proof.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_mapperFormMultiLadderCtorElseArmIsNotTouched() throws IOException {
        String out = fixtureFunction("E1MapperFormCtorElseArm");
        assertContains(out, "final MapperC<Out> ifThenElseResult;");
        assertEquals(1, count(out, "ifThenElseResult = MapperC.of(Collections.singletonList(Out.builder()"),
                "the mapper-form arm already emits the wrap - exactly once, unchanged:\n" + out);
        assertEquals(0, count(out, "MapperC.of(Collections.singletonList(MapperC.of("),
                "the mapper-form arm must not be wrapped a second time:\n" + out);
        assertEquals(0, count(out, "ifThenElseResult = Out.builder()"),
                "the mapper-form ladder never emitted a bare builder and must not start:\n" + out);
    }

    /**
     * e2 -- the same near-miss on the REAL corpus file, whole. See {@link #TECHNICAL_RECORD_ID}:
     * the ONLY green {@code elseKind=RConstructorExpr} the LAW-75 probe found corpus-wide, and
     * the file {@code VoidCtorHoistFqnComposeTest} already byte-locks in this cell -- a
     * deliberate second lock at the seat that changes.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void e2_drr6341TechnicalRecordIdMapperFormElseArmByteFrozen() throws IOException {
        assertNotNull(drr634Output, "drr 6.34.1 generation did not run");
        String gen = drr634Output.get(TECHNICAL_RECORD_ID);
        assertNotNull(gen, "not generated: " + TECHNICAL_RECORD_ID);
        String golden = Files.readString(GOLDEN_C.resolve(TECHNICAL_RECORD_ID));
        assertEquals(1, count(golden,
                        "final MapperC<drr.regulation.common.TechnicalRecordId> ifThenElseResult;"),
                "the e2 lock must actually witness the Mapper-typed MULTI ite local; if golden has"
                + " changed shape, re-site the lock rather than dropping it");
        assertEquals(1, count(golden, "ifThenElseResult = MapperC.of(Collections.singletonList("
                        + "drr.regulation.common.TechnicalRecordId.builder()"),
                "the e2 lock must actually witness the ALREADY-WRAPPED constructor else arm; if"
                + " golden has changed shape, re-site the lock rather than dropping it");
        assertEquals(0, count(gen, "MapperC.of(Collections.singletonList(MapperC.of("),
                "the mapper-form else arm must not be wrapped a second time:\n" + gen);
        assertEquals(normalize(golden), normalize(gen),
                "the mapper-form MULTI ladder with a CONSTRUCTOR terminal else must be"
                + " byte-unchanged - it is the only green row the else-side rung could reach");
    }

    /**
     * e3 -- the kind discriminator: NON-constructor arms at BOTH seats keep the bare Mapper
     * form. This is the fixture witness for the {@code m-lawB1-kind} lane.
     *
     * <p>PIN AT RED: {@code ifThenElseResult = thenArg} is a fixture-reach pin on the chain's
     * base local name. If the base local is not named {@code thenArg...}, fix the assert to the
     * measured name rather than dropping it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e3_navigationArmsAtBothSeatsKeepTheBareMapperForm() throws IOException {
        String out = fixtureFunction("E3NonCtorIteArm");
        assertContains(out, "final MapperS<String> ifThenElseResult;");
        assertContains(out, "result = ifThenElseResult.get();");
        assertContains(out, "ifThenElseResult = thenArg");
        assertEquals(0, count(out, "ifThenElseResult = MapperS.of("),
                "a navigation arm is ALREADY a Mapper - neither seat may re-wrap it:\n" + out);
    }

    /**
     * e4 -- the ITEM-typed seat is untouched: a GREEN corpus carrier whose constructor arm is
     * assigned BARE to {@code final OptionStrike ifThenElseResult;} stays byte-identical. This
     * is the 4,876-row green family's representative -- the population the whole green-safety
     * argument rests on.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void e4_drr700ItemTypedSeatKeepsTheBareBuilderAssignment() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        String gen = drr7Output.get(ITEM_SEAT_GREEN);
        assertNotNull(gen, "not generated: " + ITEM_SEAT_GREEN);
        String golden = Files.readString(GOLDEN_A.resolve(ITEM_SEAT_GREEN));
        assertTrue(golden.contains("final OptionStrike ifThenElseResult;")
                        && golden.contains("ifThenElseResult = OptionStrike.builder()"),
                "the e4 lock must actually witness the bare item-typed constructor arm under an"
                + " ITEM-typed decl; if golden has changed shape, re-site the lock rather than"
                + " dropping it");
        assertEquals(normalize(golden), normalize(gen),
                "the ITEM-typed hoist seat must be byte-unchanged - seat 32 law B.1 touches only"
                + " the Mapper-typed seats");
    }

    /**
     * e5 -- idempotence, and BOTH sides of the discriminator in one green file. See
     * {@link #WRAPPED_CTOR_GREEN}. A double wrap would show here first.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void e5_drr700AlreadyWrappedCtorArmIsNotWrappedTwice() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        String gen = drr7Output.get(WRAPPED_CTOR_GREEN);
        assertNotNull(gen, "not generated: " + WRAPPED_CTOR_GREEN);
        String golden = Files.readString(GOLDEN_A.resolve(WRAPPED_CTOR_GREEN));
        assertEquals(2, count(golden,
                        "= MapperS.of(OrganisationIdentification15Choice__1.builder()"),
                "the e5 lock must actually witness the already-wrapped form (two sites); if golden"
                + " has changed shape, re-site the lock rather than dropping it");
        assertEquals(1, count(golden, "LegalPersonIdentification1__1 ifThenElseResult0 = null;"),
                "the e5 lock must also witness the ITEM-typed decl in the SAME file - the two"
                + " sides of the discriminator; if golden has changed shape, re-site the lock");
        assertEquals(0, count(gen, "MapperS.of(MapperS.of("),
                "the already-wrapped constructor arm must not be wrapped a second time:\n" + gen);
        assertEquals(normalize(golden), normalize(gen),
                "the already-wrapped green carrier must be byte-unchanged - the"
                + " !startsWith(\"MapperS.of(\") conjunct is what keeps the walk idempotent");
    }

    // =========================================================================
    // Part C -- the whole-file heals and the LAW-69 preservation lock
    // =========================================================================

    /** corpus_c1 -- the FXLeg1 whole-file heal, drr 7.0.0. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700FXLeg1MatchesGolden() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        List<String> own = drr7GenErrors.stream().filter(e -> e.contains(FXLEG1)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + FXLEG1 + ": " + own);
        String gen = drr7Output.get(FXLEG1);
        assertNotNull(gen, "not generated: " + FXLEG1);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(FXLEG1))), normalize(gen),
                "FXLeg1 must byte-match golden - seat 32 law B.1: the constructor arm wraps"
                + " MapperS.of(...) against the ite-chain seat's Mapper-typed local, at the"
                + " per-rung pipeline AND at the terminal else (4 hunks, 8 diff lines)");
    }

    /** corpus_c2 -- the FXLeg2 whole-file heal in the SECOND cell, drr 7.3.0. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730FXLeg2MatchesGolden() throws IOException {
        assertNotNull(drr73Output, "drr 7.3.0 generation did not run");
        String gen = drr73Output.get(FXLEG2);
        assertNotNull(gen, "not generated: " + FXLEG2);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(FXLEG2))), normalize(gen),
                "FXLeg2 must byte-match golden in drr 7.3.0 - seat 32 law B.1 holds across the"
                + " whole 7.x family, not one cell");
    }

    /**
     * corpus_c3 -- LAW 69: the seat the rung was EXTRACTED from is byte-frozen. See
     * {@link #NOTIONAL}: four MULTI-branch sites plus the bare default arm and the typed
     * {@code ofNull()} terminal, all in one green file.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c3_drr700NotionalDeepThenLadderIsByteFrozen() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        String gen = drr7Output.get(NOTIONAL);
        assertNotNull(gen, "not generated: " + NOTIONAL);
        String golden = Files.readString(GOLDEN_A.resolve(NOTIONAL));
        assertEquals(4, count(golden, "MapperC.of(Collections.singletonList(Measure.builder()"),
                "the c3 lock must actually witness the extracted rung's MULTI branch (four"
                + " sites); if golden has changed shape, re-site the lock rather than dropping it");
        assertEquals(normalize(golden), normalize(gen),
                "the deep-then ladder seat must be byte-unchanged by the extraction - seat 32"
                + " law B.1 shares the #398 rung, it does not alter it");
    }

    // =========================================================================
    // corpus_control1 / corpus_control3 -- LAW 79, the UNION whole-cell controls.
    //
    // Three tokens per file, scanned over CODE only (comments stripped, string literals kept):
    //   T1  `<local> = Mapper[SC].of(<Upper>.builder()`  -- the POST-fix assignment
    //   T2  `<local> = <Upper>.builder()` at a statement start -- the PRE-fix assignment AND the
    //       whole 4,876-row ITEM-typed green family (a declaration `final X y = Z.builder()` is
    //       excluded: the character before the LHS identifier must start the statement)
    //   T3  `<ifThenElseResult|thenArg|_thenArg>* = Mapper[SC].of(` -- EVERY Mapper-wrap
    //       assignment at the hoist seats. T3 is what makes the controls fail on an OVER-fire:
    //       admitting non-constructor arms lifts it across the cell.
    //
    // The golden-domain pins below were DERIVED by a read-only os.walk over each cell's
    // rosetta-source/src/generated/java at head ddcdd151b, with a python transcription of the
    // three scanners below (same masking, same char walk, same evaluation order):
    //
    //   drr 7.0.0  287 files carrying a non-zero triple (208 under /functions/, 79 under
    //              /reports/, ZERO elsewhere - every one inside this harness's emission domain),
    //              T1 = 8 sites / 4 files (FXLeg1, FXLeg2, Create_CounterpartySpecificData,
    //              Create_CounterpartySpecificData_2), T2 = 525 sites / 119 files,
    //              T3 = 408 sites / 199 files.
    //   drr 6.34.1 296 files (172 functions + 124 reports, ZERO elsewhere), T1 = 4, T2 = 481,
    //              T3 = 478. TechnicalRecordId sits INSIDE this domain at [0, 0, 1] - which is
    //              why the else-seat near-miss is pinned in a control at all: drr 7.x generates
    //              that function with no ifThenElseResult and it is invisible to control1.
    //
    // control1's residue was derived the same way from the seat-31 final OFF dump: over the 13
    // band files of drr 7.0.0, exactly FIVE differ on the triple, and after this law exactly
    // THREE do - Price, QuantityUnitOfMeasure and TotalNotionalQuantity, none of which this law
    // touches (all three carry T1 = T2 = 0 on BOTH sides, so no constructor arm reaches any seat
    // in them). FXLeg1 and FXLeg2 read fork=[0, 2, 0] golden=[2, 0, 2] at the base and leave the
    // list when the law lands: that is control1's RED. NOTE the half-heal signature - a rung-only
    // or else-only fix lands them on [1, 1, 1], still not golden, so control1 fails under
    // m-lawB1-thenonly and m-lawB1-elseonly too.
    //
    // control3's residue is EMPTY on both sides of the law: no drr 6.x file is in the band at
    // this head (the 88 band files are drr 5.61.0 + drr 7.0/7.1/7.2/7.3 only), so drr 6.34.1 is
    // byte-identical throughout and every triple already agrees. Its job is the OVER-fire: the
    // m-lawB1-kind severance lifts T3 across 478 golden sites.
    //
    // BOTH residue lists are PRINT-FIRST pins. control1's was measured at the seat-31 head; if
    // another seat-32 law lands first and heals one of the disclosed three, re-pin from the
    // assert's own print (and say so - never delete a row to make the assert pass).
    // =========================================================================

    private static final int GOLDEN_DOMAIN_FILES = 287;
    private static final int GOLDEN_T1_SITES = 8;
    private static final int GOLDEN_T2_SITES = 525;
    private static final int GOLDEN_T3_SITES = 408;

    private static final int GOLDEN_C_DOMAIN_FILES = 296;
    private static final int GOLDEN_C_T1_SITES = 4;
    private static final int GOLDEN_C_T2_SITES = 481;
    private static final int GOLDEN_C_T3_SITES = 478;

    /** The drr 7.0.0 files whose triple differs from golden AFTER this law -- none of them ours. */
    private static final List<String> KNOWN_DRR7_RESIDUE = List.of(
            // the Price row (fork=[0, 0, 4] golden=[0, 0, 6]) LEFT this list: law C.1 (iteChainNestedThenLadderAdmit + R2a/R2b/R4)
            // rendered the seven-rung ladder as statements and took this scan's token set to golden's in
            // all four drr 7.x cells - the file stays BANDED on C.2's default join; transcribed from this
            // control's own print (C1-trip1.log), a pure row removal (was == expected minus it).
            // the QuantityUnitOfMeasure row (fork=[0, 0, 2] golden=[0, 0, 0]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            // the TotalNotionalQuantity row (fork=[0, 0, 0] golden=[0, 0, 1]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /** drr 6.34.1 carries no band file at this head -- the cell agrees with golden throughout. */
    private static final List<String> KNOWN_DRR634_RESIDUE = List.of();

    /** Golden carriers in the domain each cell does not emit (the cell's refusals). */
    private static final List<String> NOT_EMITTED_DRR7 = List.of();
    private static final List<String> NOT_EMITTED_DRR634 = List.of();

    private static final int[] ZERO_TRIPLE = { 0, 0, 0 };

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_drr700WholeCellCtorArmSitesEqualGoldenExceptTheDisclosedThree()
            throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        Map<String, int[]> f = scanTriples(drr7Output);
        Map<String, int[]> g = scanTriples(readGoldenTree(GOLDEN_A));
        assertEquals(GOLDEN_DOMAIN_FILES, g.size(),
                "golden drr 7.0.0 files carrying a non-zero (T1, T2, T3) triple");
        assertEquals(GOLDEN_T1_SITES, sum(g, 0), "golden drr 7.0.0 T1 sites");
        assertEquals(GOLDEN_T2_SITES, sum(g, 1), "golden drr 7.0.0 T2 sites");
        assertEquals(GOLDEN_T3_SITES, sum(g, 2), "golden drr 7.0.0 T3 sites");
        assertTrue(g.containsKey(FXLEG1) && g.containsKey(FXLEG2),
                "the carriers must be INSIDE this scan's domain, else control1 proves nothing");
        assertTrue(g.containsKey(ITEM_SEAT_GREEN),
                "the ITEM-typed green family must be INSIDE this scan's domain, else control1"
                + " cannot see an over-fire");
        assertEquals(KNOWN_DRR7_RESIDUE, mismatches(f, g, drr7Output, NOT_EMITTED_DRR7),
                "(T1, T2, T3) differ from golden beyond the disclosed three");
    }

    /**
     * corpus_control3 -- the SECOND whole-cell UNION control, over the cell where the else-seat
     * near-miss actually lives. The domain is pinned, {@link #TECHNICAL_RECORD_ID} is pinned
     * INSIDE it at its exact golden triple, and the residue is empty on both sides of the law.
     * Its job is the over-fire: {@code m-lawB1-kind} lifts T3 across 478 golden sites here.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_control3_drr6341WholeCellCtorArmSitesEqualGolden() throws IOException {
        assertNotNull(drr634Output, "drr 6.34.1 generation did not run");
        Map<String, int[]> f = scanTriples(drr634Output);
        Map<String, int[]> g = scanTriples(readGoldenTree(GOLDEN_C));
        assertEquals(GOLDEN_C_DOMAIN_FILES, g.size(),
                "golden drr 6.34.1 files carrying a non-zero (T1, T2, T3) triple");
        assertEquals(GOLDEN_C_T1_SITES, sum(g, 0), "golden drr 6.34.1 T1 sites");
        assertEquals(GOLDEN_C_T2_SITES, sum(g, 1), "golden drr 6.34.1 T2 sites");
        assertEquals(GOLDEN_C_T3_SITES, sum(g, 2), "golden drr 6.34.1 T3 sites");
        assertEquals("[0, 0, 1]", java.util.Arrays.toString(g.get(TECHNICAL_RECORD_ID)),
                "the else-seat near-miss must be INSIDE this scan's domain at its measured"
                + " golden triple, else control3 proves nothing about the rung this law adds");
        assertEquals(KNOWN_DRR634_RESIDUE, mismatches(f, g, drr634Output, NOT_EMITTED_DRR634),
                "drr 6.34.1 carries no band file - every (T1, T2, T3) must agree with golden");
    }

    /**
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN: the IR route inherits all three call
     * sites ({@code IRFunctionExpressionRenderer} overrides only the four naming methods,
     * {@code IRControlFlowHandler} only one), so it must render the healed carriers
     * golden-identical too. Skips unless the IR provider is on the classpath ({@code -Pir-on}).
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForFXLeg1And2() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(FXLEG1))),
                normalize(irOut.get(FXLEG1)), "IR route vs GOLDEN: " + FXLEG1);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(FXLEG2))),
                normalize(irOut.get(FXLEG2)), "IR route vs GOLDEN: " + FXLEG2);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(NOTIONAL))),
                normalize(irOut.get(NOTIONAL)), "IR route vs GOLDEN (the c3 lock): " + NOTIONAL);
    }

    // =========================================================================
    // The scan -- (T1, T2, T3) per file, over CODE only. Character walks, no regex
    // (the project's no-regex-on-structured-content rule); the codeOnly / count /
    // readGoldenTree helpers are the IntLiteralNumberSeatTest ones, reused verbatim.
    // =========================================================================

    private static final String[] HOIST_PREFIXES = { "ifThenElseResult", "thenArg", "_thenArg" };
    private static final String[] MAPPER_OF_ASSIGN = { " = MapperS.of(", " = MapperC.of(" };
    private static final String BUILDER = ".builder()";

    /**
     * The UNION diff of fork-vs-golden triples over BOTH key sets, with the cell's refusals
     * named rather than silently skipped (a golden carrier the cell does not emit is an
     * assertion of its own, not a hole in the control).
     */
    private static List<String> mismatches(Map<String, int[]> f, Map<String, int[]> g,
            Map<String, String> emitted, List<String> expectedNotEmitted) {
        List<String> notEmitted = new ArrayList<>();
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(f.keySet());
        universe.addAll(g.keySet());
        for (String key : universe) {
            if (!emitted.containsKey(key)) {
                notEmitted.add(key);          // a refusal in this cell - named, never silent
                continue;
            }
            int[] fc = f.getOrDefault(key, ZERO_TRIPLE);
            int[] gc = g.getOrDefault(key, ZERO_TRIPLE);
            if (!java.util.Arrays.equals(fc, gc)) {
                mismatched.add(key + " fork=" + java.util.Arrays.toString(fc)
                        + " golden=" + java.util.Arrays.toString(gc));
            }
        }
        assertEquals(expectedNotEmitted, notEmitted,
                "golden carriers this cell does not emit must be exactly the named set");
        return mismatched;
    }

    private static Map<String, int[]> scanTriples(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int[] t = { ctorWrapAssignSites(code), bareBuilderAssignSites(code),
                    hoistMapperAssignSites(code) };
            if (t[0] > 0 || t[1] > 0 || t[2] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    private static int sum(Map<String, int[]> scan, int idx) {
        int n = 0;
        for (int[] c : scan.values()) {
            n += c[idx];
        }
        return n;
    }

    /** T1 -- {@code <local> = Mapper[SC].of(<Upper>.builder()}: golden's post-fix assignment. */
    private static int ctorWrapAssignSites(String code) {
        int sites = 0;
        for (String head : MAPPER_OF_ASSIGN) {
            int i = code.indexOf(head);
            while (i >= 0) {
                int k = i + head.length();
                if (k < code.length() && Character.isUpperCase(code.charAt(k))
                        && code.startsWith(BUILDER, identEnd(code, k))) {
                    sites++;
                }
                i = code.indexOf(head, i + head.length());
            }
        }
        return sites;
    }

    /** T2 -- {@code <local> = <Upper>.builder()} at a statement start: the pre-fix assignment. */
    private static int bareBuilderAssignSites(String code) {
        int sites = 0;
        final String head = " = ";
        int i = code.indexOf(head);
        while (i >= 0) {
            int k = i + head.length();
            if (k < code.length() && Character.isUpperCase(code.charAt(k))
                    && code.startsWith(BUILDER, identEnd(code, k))
                    && isBareAssignTo(code, i, null)) {
                sites++;
            }
            i = code.indexOf(head, i + head.length());
        }
        return sites;
    }

    /** T3 -- {@code <hoistLocal> = Mapper[SC].of(}: every Mapper wrap at the hoist seats. */
    private static int hoistMapperAssignSites(String code) {
        int sites = 0;
        for (String head : MAPPER_OF_ASSIGN) {
            int i = code.indexOf(head);
            while (i >= 0) {
                if (isBareAssignTo(code, i, HOIST_PREFIXES)) {
                    sites++;
                }
                i = code.indexOf(head, i + head.length());
            }
        }
        return sites;
    }

    /**
     * True when the {@code " = "} at {@code eq} is a BARE assignment (not a declaration): an
     * identifier LHS whose first character starts the statement. When {@code prefixes} is
     * non-null the LHS must additionally begin with one of them; when it is null the LHS must
     * merely look like a local (lower-case or {@code _} initial), which is what excludes
     * {@code final Foo bar = Baz.builder()} -- there the character before the LHS identifier is
     * the type name, not a statement boundary.
     */
    private static boolean isBareAssignTo(String code, int eq, String[] prefixes) {
        int q = eq;
        while (q > 0 && isIdentChar(code.charAt(q - 1))) {
            q--;
        }
        if (q == eq) {
            return false;
        }
        int r = q;
        while (r > 0 && (code.charAt(r - 1) == ' ' || code.charAt(r - 1) == '\t')) {
            r--;
        }
        char prev = r > 0 ? code.charAt(r - 1) : '\n';
        if (prev != '\n' && prev != '{' && prev != '}' && prev != ';') {
            return false;
        }
        String lhs = code.substring(q, eq);
        if (prefixes == null) {
            char first = lhs.charAt(0);
            return Character.isLowerCase(first) || first == '_';
        }
        for (String p : prefixes) {
            if (lhs.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    private static int identEnd(String code, int k) {
        int j = k;
        while (j < code.length() && isIdentChar(code.charAt(j))) {
            j++;
        }
        return j;
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** OCCURRENCE count (the witness-uniqueness law -- never line counts). */
    private static int count(String text, String token) {
        int n = 0;
        int i = text.indexOf(token);
        while (i >= 0) {
            n++;
            i = text.indexOf(token, i + token.length());
        }
        return n;
    }

    private static Map<String, String> readGoldenTree(Path dir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) stream
                    .filter(q -> q.toString().endsWith(".java"))::iterator) {
                out.put(dir.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    private static String codeOnly(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("/*", i)) {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else if (s.startsWith("//", i)) {
                int end = s.indexOf('\n', i);
                i = end < 0 ? s.length() : end;
            } else if (s.charAt(i) == '"') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != '"') {
                    if (s.charAt(j) == '\\') {
                        j++;
                    }
                    j++;
                }
                sb.append(s, i, Math.min(j + 1, s.length()));
                i = j + 1;
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // Fixture harness (the ChoiceOptionLadderDeepHopSeatTest function renderer)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat32b1.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat32b1".equals(m.namespace()));
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
            throw new AssertionError("[IteChainCtorArmMapperWrapSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors = new ArrayList<>();
    private static Map<String, String> drr73Output;
    private static Map<String, String> drr634Output;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drr7GenErrors = errs;
        }
        if (cellBAvailable()) {
            drr73Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT),
                    new ArrayList<>());
        }
        if (cellCAvailable()) {
            drr634Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_C_ROOT),
                    new ArrayList<>());
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
