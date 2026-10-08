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
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import com.regnosys.rosetta.validation.ValidationIssueCode;

/**
 * v3.2 seat 5 (PR #626): the seat suite of the F6 + F3 pair — the meta-scheme shapes ({@code C9Stamp},
 * {@code C9Sieve}) and the beyond-long literal's conversion render form ({@code C4Gate}), 37 declared
 * D11 rows healed on BOTH routes under fourteen oracle groups pinned from the released 9.83.0 plugin
 * BEFORE the code (the census {@code target/v32-seat5-instruments/f6-f3-census.md}, local).
 *
 * <ul>
 *   <li><b>F6-Stamp (arm a)</b> — {@code with-meta} on an ALREADY-wrapped argument: the fork hoisted the
 *       VALUE type and rebuilt a fresh wrapper where upstream hoists the wrapper's BUILDER null-guarded
 *       and stamps the entries through {@code getOrCreateMeta()} ({@code ExpressionGenerator.xtend:1443-1463}).
 *       {@code ConstructionHandler.buildTypedWithMeta}'s wrapped-argument arm reads the argument's
 *       wrapper from the compiled Mapper's ITEM type, else recovers it from the AST
 *       ({@code NavigationHandler.recoverExprMetaWrapper} — an ALIAS body included); a model-typed
 *       VARIABLE argument takes the POJO-meta form through its own unwrap channel; the SET seat's
 *       unwrap arm ({@code FunctionExpressionRenderer.renderSetWithMetaValueUnwrapOrNull}) reads a bare
 *       variable in place. The released plugin REFUSES {@code with-meta} on a MULTI argument — ported as
 *       {@code ExpressionValidator.checkWithMetaSingle} (v1).</li>
 *   <li><b>F6-Sieve (arm c)</b> — the meta ladder's ALIAS rung: a first / last collapse over an alias
 *       call compiled null-typed (the reference route's bare Mapper-returning call), so the SET seat's
 *       hoist + null-guarded deref never fired. {@code CollectionHandler}'s collapse stamp reads the alias
 *       SIGNATURE walk ({@code NavigationHandler.tryAliasReceiverMapperType} — the same walk the alias
 *       method's declaration renders from); the deref seat's alias-signature rung reads THROUGH an
 *       only-element collapse (null-typed by the #614 bijection).</li>
 *   <li><b>F3 (arm d)</b> — the beyond-long literal's VALUE consumption was the lambda channel's Mapper
 *       ternary at the STATEMENT seats too ({@code r = bigInteger == null ? ... : ....get();}, the
 *       {@code .get()} binding to the else operand). {@code JavaExpression.BigIntegerLiteralValue} is the
 *       literal seat's producer-stamped witness carrying the hoisted sentinel; ONE renderer
 *       ({@code FunctionExpressionRenderer.renderNumericConvertNullSafeOrNull} — upstream's
 *       {@code convertNullSafe} completed as an if/else statement, the conversion from the coercion
 *       service) is read at the conditional-SET arms (the else arm flattening to {@code } else if (}),
 *       the whole-output SET and the whole-output ADD; the alias signature walk types an int literal by
 *       the translator's digit law ({@code MapperS<BigInteger>}).</li>
 * </ul>
 *
 * <p>The fixtures below mirror the seat's ORACLE groups ({@code holdout/withmeta-wrapped-argument}, {@code -edge},
 * {@code meta-ladder-alias-rung}, {@code -edge}, {@code conv-bigint-statement}, {@code -edge} — byte-locked whole
 * by {@code HoldOutByteCompareTest}) under {@code census.seat5*} namespaces; each expected string is the golden's
 * own line (the namespace never appears in a function body), so every assertion is a mutation witness, not a
 * guess. The controls: a5 / c7 / c8 / d10 are the shapes the fix must NOT move (byte-identical before and after);
 * v1 is the ported validator's refusal witness; control2a pins the default route's populations; control2b is
 * LAW 77 on the IR route (runs under {@code -Pir-on} — the chain's ON-route seat step); the chaos carriers are
 * byte-locked whole over every placement variant ({@code C9Stamp} x12, {@code C9Sieve} x12, {@code C4Gate} x13);
 * control3 / control4 prove the locks able to fail. InLambda ({@code meta-ladder-alias-rung-edge}) is NOT
 * mirrored here: it stays PINNED in the bars, its two lambda-channel typing gaps of the F4 family banked.
 *
 * <p><b>THE LANE SET — every witness above proven able to fail.</b> Twenty-four mutations of the SHIPPED code, one
 * per fix arm and per refusal seat (seventeen at the fix head {@code 9713c12d1}, four more at the first review round
 * and three at the second — one per new witness), each run against this whole suite under {@code -Pir-on} (P1
 * against the parser lock in its own module) and each restored from git afterwards
 * ({@code target/v32-seat5-instruments/lanes-s5.py}, local; the receipt of record {@code scratch/lanes-s5.status}
 * at the round-2 code head {@code 79521b28b}, 2026-09-07 02:27-02:35 local, ONE scheduled invocation of all 24; the
 * fix-head run preserved as {@code lanes-s5-r0-9713c12d1.status} and the round-1 run — TWO foreground invocations
 * at {@code 31b82da24}, nine lanes then twelve, the file carrying both (the spec review's N-3) — as
 * {@code lanes-s5-r1-31b82da24.status}). Every lane went RED on EXACTLY the set named here and GREEN on the other
 * tests — the red set is asserted as a SET, not a count:
 * <ul>
 *   <li>arm (a): <b>A1</b> the wrapped-argument arm gated off &rarr; a1 a2 a3 a4 a6 a8 corpus_c1 (7);
 *       <b>A2</b> the alias body's wrapper recovery dropped &rarr; a8; <b>A3</b> the variable-argument admission
 *       dropped &rarr; a7 a9 a10; <b>A4</b> the in-place unwrap of a bare variable off &rarr; a4; <b>A5</b> the
 *       variable-argument item ladder reverted to the raw render &rarr; a7 a9 a10 (the input {@code k} renders
 *       {@code MapperS.of(k)}, the alias {@code src(kh)}, the call {@code keyOnKeyed.evaluate(k)} — none takes
 *       {@code .toBuilder()}); <b>A7</b> the POJO-meta arm's gate NARROWED to bare symbols (the round-2 review's
 *       proposal, refuted by the oracle) &rarr; a10 ALONE; <b>A6</b> the
 *       hoisted-variable stamp dropped ({@code selfUnwrapping} again) &rarr; a4 (WrappedToPlainOutput re-hoists
 *       the variable); <b>V1</b> the ported {@code checkWithMetaSingle} deleted (the PARSER module, re-installed
 *       after the restore) &rarr; v1.</li>
 *   <li>arm (c): <b>C1</b> the collapse stamp's alias-signature rung dropped &rarr; c1 c2 c3 c4 c5 c6 corpus_c2 (7);
 *       <b>C2</b> the deref seat's only-element subject reduced to the bare reference &rarr; c6.</li>
 *   <li>arm (d): <b>D1</b> the literal witness dropped &rarr; d1 d2 d3 d5 d6 d7 d8 d9 d12 d13 corpus_c3 <i>and
 *       control3</i> (12); <b>D2</b> the statement-seat hoist gate closed &rarr; d3 d5 d8 d13; <b>D3</b> the else-arm
 *       flatten dropped &rarr; d6; <b>D4</b> the alias-call conversion arm dropped &rarr; d4; <b>D5</b> the
 *       bare-variable item-to-list arm dropped &rarr; d6; <b>D6</b> the whole-SET literal arm dropped &rarr; d3 d13;
 *       <b>D7</b> the ADD-seat literal arm dropped &rarr; d5; <b>D8</b> the alias literal typing reverted to
 *       {@code "Integer"} &rarr; d4; <b>D9</b> the then-arm conversion read gated off &rarr; d1 d4 d6 d7 d8 d9 d12
 *       corpus_c3 <i>and control3</i> (9); <b>D10</b> the shared predicate's {@code BigInteger} identity gate
 *       dropped &rarr; d11 ALONE (the hoist fires on the identity output, the conversion seat declines it, the
 *       generic arm appends {@code .get()} to the ternary — the round-1 finding, reproduced by its lane); <b>D11</b>
 *       the escaped-name compare reverted to the Java name &rarr; d12 ALONE; <b>D12</b> the whole-SET MULTI literal
 *       arm gated off (the pre-round-2 decline restored) &rarr; d13 ALONE (MultiWhole: the hoist fires, the seat
 *       declines, the generic arm's {@code .get()} — the round-2 finding, reproduced by its lane).</li>
 *   <li>the ported rule: <b>P1</b> its error re-anchored at the WHOLE {@code with-meta} expression (the PARSER
 *       module; its own lock {@code CloseoutUnderReportValidatorTest} run there, 9/1F) &rarr; the with-meta case
 *       ALONE — the column witness, which reads the diagnostic's columns back through the fixture text.</li>
 *   <li>LAW 77: <b>Z</b> the IR leaf emitter's {@code GT} rendered as {@code lessThan} (the {@code rune-ir-java}
 *       module) &rarr; control2b ALONE, the default route unmoved — the IR-only lane.</li>
 * </ul>
 * <p>Measured against the pre-run predictions and recorded rather than re-predicted (a prediction is a claim until
 * the run measures it): control3's carrier IS one of the thirteen healed rows
 * ({@code chaos/s04/a1o1/functions/C4Gate.java}) and its FIRST assertion is that the undoctored carrier locks
 * green, so a mutation that breaks that render fails control3 at its PRECONDITION, before its own claim is reached
 * — D1 and D9 do so, no other lane does; at round 1 the new then-arm witness d12 joined those two sets (it shares
 * their mechanism) and a9 joined A3's (the alias reaches the variable-argument admission); at round 2 a10 joined
 * A3's and A5's (the call reaches the same admission and its item ladder) and d13 joined D1's, D2's and D6's (the
 * multi whole-SET literal is hoisted by the gate D2 closes, carried by the witness D1 drops and served by the arm D6
 * drops) — every other set identical to the round-1 run. That D2 / D6 / D7 leave
 * both control3 and corpus_c3 GREEN is itself a measurement: the chaos {@code C4Gate} rows carry the
 * conditional-ARM shapes, not the whole-SET, ADD or int-output shapes — those three sub-shapes are witnessed by the
 * oracle-group fixtures (d3 / d5 / d8) alone.
 */
class MetaSchemeConversionSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    private static final Path CHAOS_ROOT = com.regnosys.rosetta.testutil.ChaosCell.root();
    private static final Path CHAOS_GOLDEN = CHAOS_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean chaosAvailable() {
        return Files.isDirectory(CHAOS_GOLDEN)
                && Files.isDirectory(CHAOS_ROOT.resolve("rosetta-source/src/main/rosetta"));
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

    /** The twelve placement families every chaos seat is expanded over (the a1/a2/a3 axes + base). */
    private static final List<String> FAMILIES_12 = List.of(
            "a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half", "a3hub", "a3third", "base");
    /**
     * chaos-1.1.0 (v3.2 seat 10, D49): s04 carries a FOURTEENTH — the A8 output-package axis's {@code a8pkg}
     * variant (a rival TYPE in {@code <ns>.functions} beside the seed's same-named function; the seed's own
     * functions emit beside it byte-identical, the rival's clobbered goldens are the upstream register's). Round 1:
     * the c6 verify gate's catch — the lock had counted thirteen (the whole-suite law). The thirteen-family list that
     * preceded this one (the twelve + the half-split {@code x13half/p1}, the #624 C4Speed family's shape) is retired:
     * a stale family count beside the live one is the drift the c6 RED was made of (round 2, cq SF-2).
     */
    private static final List<String> FAMILIES_14 = List.of(
            "a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half", "a3hub", "a3third", "a8pkg", "base", "x13half");

    // =========================================================================
    // The fixtures — the oracle groups under census.seat5* namespaces
    // =========================================================================

    private static final String META_PRELUDE = """
            metaType scheme string
            metaType id string
            metaType key string
            metaType reference string

            type Keyed: <"A keyed root (the chaos C9Keyed).">
                [metadata key]
                kid string (1..1)

            type Holder: <"Attribute-level meta shapes (the chaos C9Holder).">
                coded string (0..1)
                    [metadata scheme]
                codes string (0..*)
                    [metadata scheme]
                marked string (0..1)
                    [metadata id]
                plain string (0..1)
                byRefs Keyed (0..*)
                    [metadata reference]
            """;

    /** withmeta-wrapped-argument: the C9Stamp shape and its meta-kind siblings. */
    private static final String WM = "namespace census.seat5wm\n\n" + META_PRELUDE + """

            func Wrapped: <"The chaos C9Stamp shape: scheme on an already-scheme'd attribute.">
                inputs:
                    h Holder (1..1)
                output:
                    c string (0..1)
                        [metadata scheme]
                set c: h -> coded with-meta { scheme: "oracle-scheme" }

            func Plain: <"Scheme on a plain attribute (the fork's pre-seat arm, byte-identical before and after).">
                inputs:
                    h Holder (1..1)
                output:
                    c string (0..1)
                        [metadata scheme]
                set c: h -> plain with-meta { scheme: "oracle-scheme" }

            func IdOnId: <"id on an already-id attribute.">
                inputs:
                    h Holder (1..1)
                output:
                    c string (0..1)
                        [metadata id]
                set c: h -> marked with-meta { id: "the-id" }

            func SchemeOnId: <"scheme on an id-wrapped attribute.">
                inputs:
                    h Holder (1..1)
                output:
                    c string (0..1)
                        [metadata scheme]
                set c: h -> marked with-meta { scheme: "oracle-scheme" }

            func WrappedToPlainOutput: <"The wrapped argument stamped, assigned to a PLAIN output.">
                inputs:
                    h Holder (1..1)
                output:
                    c string (0..1)
                set c: h -> coded with-meta { scheme: "oracle-scheme" }
            """;

    /** withmeta-wrapped-argument-edge: the call, the keyed variable, the alias. */
    private static final String WME = "namespace census.seat5wme\n\n" + META_PRELUDE + """

            func Wrapped: <"The callee of FromCall.">
                inputs:
                    h Holder (1..1)
                output:
                    c string (0..1)
                        [metadata scheme]
                set c: h -> coded with-meta { scheme: "oracle-scheme" }

            func FromCall: <"Scheme on a function call returning a wrapped value.">
                inputs:
                    h Holder (1..1)
                output:
                    c string (0..1)
                        [metadata scheme]
                set c: Wrapped(h) with-meta { scheme: "other-scheme" }

            func KeyOnKeyed: <"key (type meta) on a keyed argument.">
                inputs:
                    k Keyed (1..1)
                output:
                    r Keyed (1..1)
                set r: k with-meta { key: "the-key" }

            func ViaAlias: <"The wrapped argument through an alias.">
                inputs:
                    h Holder (1..1)
                output:
                    c string (0..1)
                        [metadata scheme]
                alias src: h -> coded
                set c: src with-meta { scheme: "oracle-scheme" }

            type KeyedHolder: <"A holder of a keyed root (round 1: the alias into the POJO-meta arm).">
                keyed Keyed (0..1)

            func AliasKeyed: <"key (type meta) on a keyed value reached through an ALIAS - the POJO-meta arm's alias case.">
                inputs:
                    kh KeyedHolder (1..1)
                output:
                    r Keyed (1..1)
                alias src: kh -> keyed
                set r: src with-meta { key: "the-key" }

            func CallKeyed: <"key (type meta) on a function CALL returning a keyed value (round 2).">
                inputs:
                    k Keyed (1..1)
                output:
                    r Keyed (1..1)
                set r: KeyOnKeyed(k) with-meta { key: "call-key" }
            """;

    /** The validator's refusal witness: with-meta on a MULTI argument (the released plugin's ERROR). */
    private static final String WMV = "namespace census.seat5wmv\n\n" + META_PRELUDE + """

            func MultiArg: <"with-meta on a multi-cardinality argument - REFUSED by the released plugin.">
                inputs:
                    h Holder (1..1)
                output:
                    cs string (0..*)
                        [metadata scheme]
                set cs: h -> codes with-meta { scheme: "x" }
            """;

    /** meta-ladder-alias-rung: the C9Sieve shape and its rung siblings. */
    private static final String ML = "namespace census.seat5ml\n\n" + META_PRELUDE + """

            func Sieve: <"The chaos C9Sieve shape: a filtered alias at the second rung.">
                inputs:
                    h Holder (1..1)
                output:
                    pick string (0..1)
                alias firstRef: h -> byRefs then filter item -> kid exists then first
                alias goodCodes: h -> codes then filter item <> "void"
                set pick:
                    if h -> coded exists
                    then h -> coded
                    else if goodCodes exists
                    then goodCodes first
                    else firstRef -> kid

            func NoFilter: <"An alias without a filter at the rung.">
                inputs:
                    h Holder (1..1)
                output:
                    pick string (0..1)
                alias allCodes: h -> codes
                set pick:
                    if h -> coded exists
                    then h -> coded
                    else allCodes first

            func Direct: <"A direct then-first chain at the rung, no alias (byte-identical before and after).">
                inputs:
                    h Holder (1..1)
                output:
                    pick string (0..1)
                set pick:
                    if h -> coded exists
                    then h -> coded
                    else h -> codes then filter item <> "void" then first

            func ThreeRungs: <"Three wrapped rungs - the locals number 0 1 2.">
                inputs:
                    h Holder (1..1)
                    g Holder (1..1)
                output:
                    pick string (0..1)
                alias goodCodes: h -> codes then filter item <> "void"
                set pick:
                    if h -> coded exists
                    then h -> coded
                    else if goodCodes exists
                    then goodCodes first
                    else g -> coded

            func TopLevel: <"The alias rung at the top level, no ladder.">
                inputs:
                    h Holder (1..1)
                output:
                    pick string (0..1)
                alias goodCodes: h -> codes then filter item <> "void"
                set pick: goodCodes first
            """;

    /** meta-ladder-alias-rung-edge (InLambda excluded — pinned in the bars, banked). */
    private static final String MLE = "namespace census.seat5mle\n\n" + META_PRELUDE + """

            func LastAndOnly: <"last and only-element in place of first.">
                inputs:
                    h Holder (1..1)
                output:
                    pick string (0..1)
                alias goodCodes: h -> codes then filter item <> "void"
                set pick:
                    if h -> coded exists
                    then goodCodes last
                    else goodCodes only-element

            func ElseArmOnly: <"The wrapped rung at the else-arm alone, the then-arm plain.">
                inputs:
                    h Holder (1..1)
                    p string (0..1)
                output:
                    pick string (0..1)
                alias goodCodes: h -> codes then filter item <> "void"
                set pick:
                    if p exists
                    then p
                    else goodCodes first

            func WrappedOutput: <"The ladder into a WRAPPED output (no unwrap wanted; byte-identical before and after).">
                inputs:
                    h Holder (1..1)
                output:
                    pick string (0..1)
                        [metadata scheme]
                alias goodCodes: h -> codes then filter item <> "void"
                set pick:
                    if h -> coded exists
                    then h -> coded
                    else goodCodes first
            """;

    /** conv-bigint-statement: the C4Gate shape and the F3 family's seats. */
    private static final String CB = """
            namespace census.seat5cb

            func IsBig: <"Boolean helper for the bare-call condition (the chaos C4IsBig).">
                inputs:
                    v number (1..1)
                output:
                    big boolean (1..1)
                set big: v > 1000000

            func ThenArm: <"The chaos C4Gate shape: the beyond-long literal at a set's then-arm.">
                inputs:
                    v number (1..1)
                    w number (0..1)
                output:
                    r number (1..1)
                set r:
                    if IsBig(v)
                    then 9999999999999999999999999
                    else v + (w default 0)

            func ElseArm: <"The literal at the else-arm.">
                inputs:
                    v number (1..1)
                output:
                    r number (1..1)
                set r:
                    if IsBig(v)
                    then v
                    else 9999999999999999999999999

            func Whole: <"A whole set, no conditional.">
                inputs:
                    v number (1..1)
                output:
                    r number (1..1)
                set r: 9999999999999999999999999

            func ViaAlias: <"Through an alias.">
                inputs:
                    v number (1..1)
                output:
                    r number (1..1)
                alias big: 9999999999999999999999999
                set r:
                    if IsBig(v)
                    then big
                    else v

            func AddSeat: <"An add on a multi output.">
                inputs:
                    v number (1..1)
                output:
                    rs number (0..*)
                add rs: v
                add rs: 9999999999999999999999999

            func MultiThen: <"The then-arm on a multi output.">
                inputs:
                    v number (1..1)
                output:
                    rs number (0..*)
                set rs:
                    if IsBig(v)
                    then 9999999999999999999999999
                    else v
            """;

    /** conv-bigint-statement-edge: nested, the argument (a control), the int output, both arms. */
    private static final String CBE = """
            namespace census.seat5cbe

            func IsBig: <"Boolean helper.">
                inputs:
                    v number (1..1)
                output:
                    big boolean (1..1)
                set big: v > 1000000

            func Nested: <"Two conditionals deep.">
                inputs:
                    v number (1..1)
                    f boolean (1..1)
                output:
                    r number (1..1)
                set r:
                    if f
                    then if IsBig(v) then 9999999999999999999999999 else v
                    else 0

            func AsArg: <"As a function-call argument (the arg route's own form; byte-identical before and after).">
                inputs:
                    v number (1..1)
                output:
                    r boolean (1..1)
                set r: IsBig(9999999999999999999999999)

            func IntOut: <"An int output takes the exact conversion.">
                inputs:
                    v number (1..1)
                output:
                    r int (1..1)
                set r:
                    if IsBig(v)
                    then 9999999999999999999999999
                    else 1

            func BothArms: <"Both arms beyond long.">
                inputs:
                    v number (1..1)
                output:
                    r number (1..1)
                set r:
                    if IsBig(v)
                    then 9999999999999999999999999
                    else 8888888888888888888888888

            typeAlias BigInt: <"A 25-digit integer - the BigInteger-typed output (round 1).">
                int(digits: 25)

            func IdentityOut: <"A beyond-long literal into a BigInteger-typed output: the conversion is the identity.">
                inputs:
                    v number (1..1)
                output:
                    r BigInt (0..1)
                set r: 9999999999999999999999999

            func EscapedName: <"The then-arm literal into an output whose Java name needs the keyword escape.">
                inputs:
                    v number (1..1)
                output:
                    transient number (1..1)
                set transient:
                    if IsBig(v)
                    then 9999999999999999999999999
                    else v

            func MultiWhole: <"A beyond-long literal as the WHOLE value of a MULTI output (round 2).">
                inputs:
                    v number (1..1)
                output:
                    rs number (0..*)
                set rs: 9999999999999999999999999
            """;

    // =========================================================================
    // F6-Stamp (arm a) — the wrapped argument's builder hoist + getOrCreateMeta stamp
    // =========================================================================

    private static final String WM_CODED_GET =
            "MapperS.of(h).<FieldWithMetaString>map(\"getCoded\", holder -> holder.getCoded()).get()";
    private static final String WM_MARKED_GET =
            "MapperS.of(h).<FieldWithMetaString>map(\"getMarked\", holder -> holder.getMarked()).get()";

    /** a1: the C9Stamp shape — the wrapper's BUILDER hoisted null-guarded, the scheme stamped, no MetaFields rebuild. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_schemeOnAnAlreadySchemedAttribute_hoistsTheBuilderAndStampsThroughGetOrCreateMeta() throws IOException {
        String code = fn("wm", "Wrapped.java");
        assertContains(code, "final FieldWithMetaString.FieldWithMetaStringBuilder withMetaArgument = "
                + WM_CODED_GET + " == null ? null : " + WM_CODED_GET + ".toBuilder();");
        assertContains(code, "withMetaArgument.getOrCreateMeta().setScheme(\"oracle-scheme\");");
        assertContains(code, "c = toBuilder(withMetaArgument);");
        assertAbsent(code, "MetaFields");
        assertAbsent(code, "FieldWithMetaString.builder().setValue(");
    }

    /** a2: id on an id-wrapped attribute — the same arm, the entry's setter is setExternalKey. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_idOnAnIdAttribute_stampsSetExternalKey() throws IOException {
        String code = fn("wm", "IdOnId.java");
        assertContains(code, "final FieldWithMetaString.FieldWithMetaStringBuilder withMetaArgument = "
                + WM_MARKED_GET + " == null ? null : " + WM_MARKED_GET + ".toBuilder();");
        assertContains(code, "withMetaArgument.getOrCreateMeta().setExternalKey(\"the-id\");");
    }

    /** a3: scheme on an id-wrapped attribute — another meta kind on the SAME wrapper class takes the arm. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_schemeOnAnIdAttribute_theSameWrapperClassTakesTheArm() throws IOException {
        String code = fn("wm", "SchemeOnId.java");
        assertContains(code, "withMetaArgument.getOrCreateMeta().setScheme(\"oracle-scheme\");");
        assertContains(code, WM_MARKED_GET + ".toBuilder();");
    }

    /** a4: the stamped wrapper into a PLAIN output — the unwrap arm reads the hoisted variable in place. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_wrappedArgumentIntoAPlainOutput_unwrapsTheVariableInPlace() throws IOException {
        String code = fn("wm", "WrappedToPlainOutput.java");
        assertContains(code, "withMetaArgument.getOrCreateMeta().setScheme(\"oracle-scheme\");");
        assertContains(code, "if (withMetaArgument == null) {\n\t\t\t\tc = null;\n\t\t\t} else {\n\t\t\t\tc = withMetaArgument.getValue();\n\t\t\t}");
        assertAbsent(code, "final FieldWithMetaString fieldWithMetaString");
    }

    /** a5 (control): scheme on a PLAIN attribute keeps the pre-seat value hoist + MetaFields build. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_control_schemeOnAPlainAttribute_keepsTheValueHoistAndTheMetaFieldsBuild() throws IOException {
        String code = fn("wm", "Plain.java");
        assertContains(code, "final String withMetaArgument = MapperS.of(h).<String>map(\"getPlain\", holder -> holder.getPlain()).get();");
        assertContains(code, "c = toBuilder(FieldWithMetaString.builder().setValue(withMetaArgument).setMeta(MetaFields.builder().setScheme(\"oracle-scheme\")));");
        assertAbsent(code, "getOrCreateMeta");
    }

    /** a6: a function CALL returning the wrapper — the bare evaluate is the item, hoisted the same way. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_schemeOnAWrappedFunctionCall_hoistsTheCallsBuilder() throws IOException {
        String code = fn("wme", "FromCall.java");
        assertContains(code, "final FieldWithMetaString.FieldWithMetaStringBuilder withMetaArgument = wrapped.evaluate(h) == null ? null : wrapped.evaluate(h).toBuilder();");
        assertContains(code, "withMetaArgument.getOrCreateMeta().setScheme(\"other-scheme\");");
    }

    /** a7: a TYPE-meta entry on a keyed model VARIABLE — the POJO-meta form over the bare variable (never the stub). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a7_keyOnAKeyedVariable_takesThePojoMetaFormOverTheBareVariable() throws IOException {
        String code = fn("wme", "KeyOnKeyed.java");
        assertContains(code, "final Keyed.KeyedBuilder withMetaArgument = k == null ? null : k.toBuilder();");
        assertContains(code, "withMetaArgument.getOrCreateMeta().setExternalKey(\"the-key\");");
        assertContains(code, "r = toBuilder(withMetaArgument);");
        assertAbsent(code, "MapperS.of(k)");
    }

    /** a8: the wrapped argument THROUGH AN ALIAS — the wrapper recovered from the alias body. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a8_wrappedArgumentThroughAnAlias_recoversTheWrapperFromTheAliasBody() throws IOException {
        String code = fn("wme", "ViaAlias.java");
        assertContains(code, "final FieldWithMetaString.FieldWithMetaStringBuilder withMetaArgument = src(h).get() == null ? null : src(h).get().toBuilder();");
        assertContains(code, "withMetaArgument.getOrCreateMeta().setScheme(\"oracle-scheme\");");
    }

    /**
     * a9 (round 1, the code-quality review's SF-1): key on a keyed value reached through an ALIAS — the POJO-meta
     * (variable-argument) arm's alias case takes the item ladder's Mapper-chain form, {@code src(kh).get()} twice,
     * where the first cut had rendered {@code src(kh).toBuilder()} on a MapperS. Golden AliasKeyed (pinned from the
     * released plugin at round 1, {@code scratch/oracle-s5b.status}).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a9_keyOnAKeyedValueThroughAnAlias_takesTheItemLadder() throws IOException {
        String code = fn("wme", "AliasKeyed.java");
        assertContains(code, "final Keyed.KeyedBuilder withMetaArgument = src(kh).get() == null ? null : src(kh).get().toBuilder();");
        assertContains(code, "withMetaArgument.getOrCreateMeta().setExternalKey(\"the-key\");");
        assertContains(code, "r = toBuilder(withMetaArgument);");
        assertAbsent(code, "src(kh).toBuilder()");
    }

    /** v1: with-meta on a MULTI argument is the released plugin's ERROR — ported, its text verbatim (CARDINALITY_ERROR). */
    @Test
    @EnabledIf("builtinsAvailable")
    void v1_withMetaOnAMultiArgument_isTheReleasedPluginsCardinalityError() throws IOException {
        Render r = render("wmv");
        List<ValidationDiagnostic> errors = r.validation(ValidationIssueCode.CARDINALITY_ERROR).stream()
                .filter(d -> d.severity() == Severity.ERROR)
                .toList();
        assertEquals(1, errors.size(), "exactly one cardinality error for the multi with-meta argument: " + r.diagnostics());
        assertEquals("Expecting single cardinality. The with-meta operator can only be used with single cardinality arguments",
                errors.get(0).message());
        Render ok = render("wm");
        assertTrue(ok.validation(ValidationIssueCode.CARDINALITY_ERROR).isEmpty(),
                "the single-argument fixtures carry no cardinality error: " + ok.diagnostics());
    }

    // =========================================================================
    // F6-Sieve (arm c) — the alias rung typed from the alias signature walk
    // =========================================================================

    private static final String UNWRAP_PICK_0 =
            "if (fieldWithMetaString0 == null) {\n\t\t\t\t\tpick = null;\n\t\t\t\t} else {\n\t\t\t\t\tpick = fieldWithMetaString0.getValue();\n\t\t\t\t}";
    private static final String UNWRAP_PICK_1 =
            "if (fieldWithMetaString1 == null) {\n\t\t\t\t\tpick = null;\n\t\t\t\t} else {\n\t\t\t\t\tpick = fieldWithMetaString1.getValue();\n\t\t\t\t}";

    /** c1: the C9Sieve shape — the alias `first` rung hoists the wrapper and derefs, numbered after the direct rung. */
    @Test
    @EnabledIf("builtinsAvailable")
    void c1_filteredAliasAtTheSecondRung_hoistsAndDerefsLikeTheDirectRung() throws IOException {
        String code = fn("ml", "Sieve.java");
        assertContains(code, "final FieldWithMetaString fieldWithMetaString0 = MapperS.of(h).<FieldWithMetaString>map(\"getCoded\", holder -> holder.getCoded()).get();");
        assertContains(code, UNWRAP_PICK_0);
        assertContains(code, "final FieldWithMetaString fieldWithMetaString1 = goodCodes(h)\n\t\t\t\t\t.first().get();");
        assertContains(code, UNWRAP_PICK_1);
        assertContains(code, "pick = firstRef(h).<Keyed>map(\"Type coercion\", referenceWithMetaKeyed -> referenceWithMetaKeyed == null ? null : referenceWithMetaKeyed.getValue()).<String>map(\"getKid\", keyed -> keyed.getKid()).get();");
        assertAbsent(code, "pick = goodCodes(h)");
    }

    /** c2: an unfiltered alias at the rung — the same stamp (the signature walk answers for every alias body). */
    @Test
    @EnabledIf("builtinsAvailable")
    void c2_unfilteredAliasAtTheRung_derefsThroughTheSameStamp() throws IOException {
        String code = fn("ml", "NoFilter.java");
        assertContains(code, "final FieldWithMetaString fieldWithMetaString1 = allCodes(h)\n\t\t\t\t\t.first().get();");
        assertContains(code, UNWRAP_PICK_1);
    }

    /** c3: three wrapped rungs — the session group numbers the locals 0 / 1 / 2 in registration order. */
    @Test
    @EnabledIf("builtinsAvailable")
    void c3_threeWrappedRungs_numberTheLocalsZeroOneTwo() throws IOException {
        String code = fn("ml", "ThreeRungs.java");
        assertContains(code, "final FieldWithMetaString fieldWithMetaString0 = MapperS.of(h)");
        assertContains(code, "final FieldWithMetaString fieldWithMetaString1 = goodCodes(h, g)\n\t\t\t\t\t.first().get();");
        assertContains(code, "final FieldWithMetaString fieldWithMetaString2 = MapperS.of(g).<FieldWithMetaString>map(\"getCoded\", holder -> holder.getCoded()).get();");
        assertContains(code, "pick = fieldWithMetaString2.getValue();");
    }

    /** c4: the alias rung as the WHOLE set — a singleton keeps the bare name. */
    @Test
    @EnabledIf("builtinsAvailable")
    void c4_aliasRungAtTheTopLevel_keepsTheBareLocalName() throws IOException {
        String code = fn("ml", "TopLevel.java");
        assertContains(code, "final FieldWithMetaString fieldWithMetaString = goodCodes(h)\n\t\t\t\t.first().get();");
        assertContains(code, "if (fieldWithMetaString == null) {\n\t\t\t\tpick = null;\n\t\t\t} else {\n\t\t\t\tpick = fieldWithMetaString.getValue();\n\t\t\t}");
        assertAbsent(code, "fieldWithMetaString0");
    }

    /** c5: the wrapped rung at the ELSE arm alone — the then arm's bare param stays bare. */
    @Test
    @EnabledIf("builtinsAvailable")
    void c5_wrappedRungAtTheElseArmAlone_theThenArmStaysBare() throws IOException {
        String code = fn("mle", "ElseArmOnly.java");
        assertContains(code, "pick = p;");
        assertContains(code, "final FieldWithMetaString fieldWithMetaString = goodCodes(h, p)\n\t\t\t\t\t.first().get();");
        assertContains(code, "pick = fieldWithMetaString.getValue();");
    }

    /** c6: `last` takes the collapse stamp; `only-element` (null-typed by the #614 bijection) is recovered at the deref seat. */
    @Test
    @EnabledIf("builtinsAvailable")
    void c6_lastAndOnlyElementOverTheAlias_derefBothRungs() throws IOException {
        String code = fn("mle", "LastAndOnly.java");
        assertContains(code, "final FieldWithMetaString fieldWithMetaString0 = goodCodes(h)\n\t\t\t\t\t.last().get();");
        assertContains(code, UNWRAP_PICK_0);
        assertContains(code, "final FieldWithMetaString fieldWithMetaString1 = goodCodes(h).get();");
        assertContains(code, UNWRAP_PICK_1);
    }

    /** c7 (control): the DIRECT then-chain rung derefs through the pre-seat channel — byte-identical before and after. */
    @Test
    @EnabledIf("builtinsAvailable")
    void c7_control_directChainRung_keepsThePreSeatDeref() throws IOException {
        String code = fn("ml", "Direct.java");
        assertContains(code, "final FieldWithMetaString fieldWithMetaString = thenArg1\n\t\t\t\t.first().get();");
        assertContains(code, "pick = fieldWithMetaString.getValue();");
    }

    /** c8 (control): the ladder into a WRAPPED output — no unwrap wanted, the alias rung assigned as the wrapper. */
    @Test
    @EnabledIf("builtinsAvailable")
    void c8_control_wrappedOutput_assignsTheAliasRungAsTheWrapper() throws IOException {
        String code = fn("mle", "WrappedOutput.java");
        assertContains(code, "pick = toBuilder(goodCodes(h)\n\t\t\t\t\t.first().get());");
        // the alias body's coercion lambda names its parameter fieldWithMetaString - the absence claim is the HOIST
        assertAbsent(code, "final FieldWithMetaString fieldWithMetaString");
    }

    // =========================================================================
    // F3 (arm d) — the null-safe conversion statement at the statement seats
    // =========================================================================

    private static final String BI_DECL = "final BigInteger bigInteger = new BigInteger(\"9999999999999999999999999\");";
    private static final String MAPPER_S_IMPORT = "import com.rosetta.model.lib.mapper.MapperS;";

    /** d1: the C4Gate shape — the then-arm literal's if/else conversion, the hoist inside the branch, no Mapper ternary. */
    @Test
    @EnabledIf("builtinsAvailable")
    void d1_beyondLongLiteralAtTheThenArm_rendersTheIfElseConversion() throws IOException {
        String code = fn("cb", "ThenArm.java");
        assertContains(code, BI_DECL);
        assertContains(code, "if (bigInteger == null) {\n\t\t\t\t\tr = null;\n\t\t\t\t} else {\n\t\t\t\t\tr = new BigDecimal(bigInteger);\n\t\t\t\t}");
        assertAbsent(code, "MapperS.<BigDecimal>ofNull()");
        // (ThenArm's golden DOES import MapperS - its else arm `v + (w default 0)` wraps; the import claim belongs to
        // the fixtures whose ONLY MapperS use was the ternary: Whole, BothArms - the seat suite's run-1 catch)
    }

    /** d2: the literal at the ELSE arm — the hoist inside the else block keeps the nested if/else (no flatten). */
    @Test
    @EnabledIf("builtinsAvailable")
    void d2_beyondLongLiteralAtTheElseArm_keepsTheNestedBlockBehindItsHoist() throws IOException {
        String code = fn("cb", "ElseArm.java");
        assertContains(code, "} else {\n\t\t\t\t" + BI_DECL + "\n\t\t\t\tif (bigInteger == null) {\n\t\t\t\t\tr = null;\n\t\t\t\t} else {\n\t\t\t\t\tr = new BigDecimal(bigInteger);\n\t\t\t\t}\n\t\t\t}");
        assertAbsent(code, "} else if (bigInteger == null)");
    }

    /** d3: the WHOLE set — the sink's declaration, then the conversion statement; no Mapper import. */
    @Test
    @EnabledIf("builtinsAvailable")
    void d3_beyondLongLiteralAsTheWholeSet_hoistsAndConverts() throws IOException {
        String code = fn("cb", "Whole.java");
        assertContains(code, BI_DECL + "\n\t\t\tif (bigInteger == null) {\n\t\t\t\tr = null;\n\t\t\t} else {\n\t\t\t\tr = new BigDecimal(bigInteger);\n\t\t\t}");
        // `r = new BigInteger(` is a SUBSTRING of the declaration `bigInteger = new BigInteger(`: the claim anchors on the
        // statement's own indentation (the seat suite's run-1 catch)
        assertAbsent(code, "\tr = new BigInteger(");
        assertAbsent(code, MAPPER_S_IMPORT);
    }

    /** d4: the alias seat — the alias declares MapperS<BigInteger>, its call is hoisted and converted at the arm. */
    @Test
    @EnabledIf("builtinsAvailable")
    void d4_beyondLongLiteralThroughAnAlias_typesTheAliasBigIntegerAndConvertsTheCall() throws IOException {
        String code = fn("cb", "ViaAlias.java");
        assertContains(code, "protected abstract MapperS<BigInteger> big(BigDecimal v);");
        assertContains(code, "final BigInteger bigInteger = big(v).get();\n\t\t\t\tif (bigInteger == null) {\n\t\t\t\t\tr = null;\n\t\t\t\t} else {\n\t\t\t\t\tr = new BigDecimal(bigInteger);\n\t\t\t\t}");
        assertContains(code, "return MapperS.of(new BigInteger(\"9999999999999999999999999\"));");
        assertAbsent(code, "MapperS<Integer>");
    }

    /** d5: the ADD seat — the declaration, then the conversion mapped over addAll (empty / singleton). */
    @Test
    @EnabledIf("builtinsAvailable")
    void d5_beyondLongLiteralAdded_mapsTheConversionOverAddAll() throws IOException {
        String code = fn("cb", "AddSeat.java");
        assertContains(code, "if (v == null) {\n\t\t\t\trs.addAll(Collections.<BigDecimal>emptyList());\n\t\t\t} else {\n\t\t\t\trs.addAll(Collections.singletonList(v));\n\t\t\t}");
        assertContains(code, BI_DECL + "\n\t\t\tif (bigInteger == null) {\n\t\t\t\trs.addAll(Collections.<BigDecimal>emptyList());\n\t\t\t} else {\n\t\t\t\trs.addAll(Collections.singletonList(new BigDecimal(bigInteger)));\n\t\t\t}");
        assertAbsent(code, "rs.addAll(new BigInteger(");
    }

    /** d6: the MULTI output — the singleton / empty forms at the then arm, the bare variable's guard FLATTENED to else-if. */
    @Test
    @EnabledIf("builtinsAvailable")
    void d6_beyondLongLiteralAtAMultiOutput_singletonForms_andTheBareVariableFlattensToElseIf() throws IOException {
        String code = fn("cb", "MultiThen.java");
        assertContains(code, "if (bigInteger == null) {\n\t\t\t\t\trs = Collections.<BigDecimal>emptyList();\n\t\t\t\t} else {\n\t\t\t\t\trs = Collections.singletonList(new BigDecimal(bigInteger));\n\t\t\t\t}");
        assertContains(code, "} else if (v == null) {\n\t\t\t\trs = Collections.<BigDecimal>emptyList();\n\t\t\t} else {\n\t\t\t\trs = Collections.singletonList(v);\n\t\t\t}");
        assertAbsent(code, "rs = v;");
        assertAbsent(code, ".getMulti()");
    }

    /** d7: two conditionals deep — the inner arm takes the same statement at its own depth. */
    @Test
    @EnabledIf("builtinsAvailable")
    void d7_nestedConditional_theInnerArmConvertsAtItsOwnDepth() throws IOException {
        String code = fn("cbe", "Nested.java");
        assertContains(code, "\t\t\t\t\t" + BI_DECL + "\n\t\t\t\t\tif (bigInteger == null) {\n\t\t\t\t\t\tr = null;\n\t\t\t\t\t} else {\n\t\t\t\t\t\tr = new BigDecimal(bigInteger);\n\t\t\t\t\t}");
        assertAbsent(code, "MapperS.<BigDecimal>ofNull()");
    }

    /** d8: an INT output — the exact conversion (intValueExact) from the coercion service, the same statement. */
    @Test
    @EnabledIf("builtinsAvailable")
    void d8_beyondLongLiteralIntoAnIntOutput_takesTheExactConversion() throws IOException {
        String code = fn("cbe", "IntOut.java");
        assertContains(code, BI_DECL);
        assertContains(code, "if (bigInteger == null) {\n\t\t\t\t\tr = null;\n\t\t\t\t} else {\n\t\t\t\t\tr = bigInteger.intValueExact();\n\t\t\t\t}");
        assertContains(code, "r = 1;");
        assertAbsent(code, "\tr = new BigInteger(");
    }

    /** d9: both arms beyond long — the sentinels number 0 / 1, each arm its own statement. */
    @Test
    @EnabledIf("builtinsAvailable")
    void d9_bothArmsBeyondLong_numberTheSentinelsAndConvertEach() throws IOException {
        String code = fn("cbe", "BothArms.java");
        assertContains(code, "final BigInteger bigInteger0 = new BigInteger(\"9999999999999999999999999\");");
        assertContains(code, "r = new BigDecimal(bigInteger0);");
        assertContains(code, "final BigInteger bigInteger1 = new BigInteger(\"8888888888888888888888888\");");
        assertContains(code, "r = new BigDecimal(bigInteger1);");
        assertAbsent(code, MAPPER_S_IMPORT);
    }

    /** d10 (control): the function-call ARGUMENT keeps the arg route's single hoist + item ternary (the first-cut catch). */
    @Test
    @EnabledIf("builtinsAvailable")
    void d10_control_beyondLongLiteralAsAnArgument_keepsTheArgRoutesForm() throws IOException {
        String code = fn("cbe", "AsArg.java");
        assertContains(code, BI_DECL);
        assertContains(code, "r = isBig.evaluate((bigInteger == null ? null : new BigDecimal(bigInteger)));");
        assertAbsent(code, "_bigInteger");
        assertAbsent(code, "MapperS.<BigDecimal>ofNull()");
    }

    // =========================================================================
    // Controls — the populations, LAW 77, the corpus locks and their ability to fail
    // =========================================================================

    /**
     * The fixture sets' emitted-file counts (functions only) — control2a's population pins, MEASURED: the values are
     * the sizes {@link #fixtureFunctionKeys} printed at the seat's pre-commit drivers (round 1: wme 5 → 7 was first
     * typed as the oracle group's golden count and corrected to the rendered 5 — the fixture set mirrors the group's
     * FUNCTIONS, not its POJOs; round 2: cbe 7 → 8 with MultiWhole, wme 5 → 6 with CallKeyed) and ASSERTED by this
     * control at every chain's ON-route seat step since (the seat log prints the suite's count - 40/0F/0sk, the step
     * 136 in {@code scratch/s5e.status}, the first chain at the round-2 pins - and control2a asserts the sizes);
     * control2c proves the pin able to fail (round 2, the spec review's SF-5).
     */
    private static final Map<String, Integer> POPULATION = Map.of(
            "wm", 5, "wme", 6, "wmv", 1, "ml", 5, "mle", 3, "cb", 7, "cbe", 8);
    /** Every fixture set this class renders — the ONE list {@link #source} dispatches over (spec SF-5). */
    private static final List<String> FIXTURE_SETS = List.of("wm", "wme", "wmv", "ml", "mle", "cb", "cbe");

    /**
     * d11 (round 1, the code-quality review's MF-1): a beyond-long literal into a {@code BigInteger}-typed output
     * ({@code typeAlias BigInt: int(digits: 25)}) — the conversion is the IDENTITY, so upstream declares no
     * variable and assigns the literal inline; the first cut's context-free hoist fired here and the conversion
     * seat declined it, leaving the non-compiling {@code <ternary>.get()} on a shape that compiled before the seat.
     * The hoist gate and the conversion seat now consult ONE predicate
     * ({@code HandlerHelper.statementSeatConversionItemOrNull}). Golden IdentityOut (the released plugin, round 1).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void d11_beyondLongLiteralIntoABigIntegerOutput_assignsInlineWithNoHoist() throws IOException {
        String code = fn("cbe", "IdentityOut.java");
        assertContains(code, "r = new BigInteger(\"9999999999999999999999999\");");
        assertAbsent(code, "final BigInteger bigInteger");
        assertAbsent(code, "MapperS");
    }

    /**
     * d12 (round 1, the code-quality review's SF-4): the then-arm literal into an output whose Java name is the
     * keyword-escaped {@code _transient} — the conversion seat compares the output's AST name against the
     * operation's own target name, never against the escaped Java name (which made it decline silently for every
     * escaped output). Golden EscapedName (the released plugin, round 1).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void d12_thenArmLiteralIntoAnEscapedOutputName_rendersTheConversion() throws IOException {
        String code = fn("cbe", "EscapedName.java");
        assertContains(code, "if (bigInteger == null) {\n\t\t\t\t\t_transient = null;\n\t\t\t\t} else {\n\t\t\t\t\t_transient = new BigDecimal(bigInteger);\n\t\t\t\t}");
        assertAbsent(code, "MapperS.<BigDecimal>ofNull()");
    }

    /**
     * d13 (round 2, the code-quality review's MF-1): a beyond-long literal as the WHOLE value of a MULTI output — the
     * shared predicate admits the hoist and the whole-SET seat used to decline every multi output, so the seat's own
     * hoist fell to the generic arm and its {@code .get()} (the fork's pre-fix render MISMATCHED the golden and did not
     * compile: {@code BigDecimal cannot be converted to List<BigDecimal>}). Golden MultiWhole (the released plugin,
     * round 2, {@code scratch/oracle-s5c.status}): the list form MultiThen takes at the arm, at the whole-SET seat.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void d13_beyondLongLiteralAsTheWholeValueOfAMultiOutput_rendersTheListForm() throws IOException {
        String code = fn("cbe", "MultiWhole.java");
        assertContains(code, "final BigInteger bigInteger = new BigInteger(\"9999999999999999999999999\");");
        assertContains(code, "if (bigInteger == null) {\n\t\t\t\trs = Collections.<BigDecimal>emptyList();\n\t\t\t} else {\n\t\t\t\trs = Collections.singletonList(new BigDecimal(bigInteger));\n\t\t\t}");
        assertAbsent(code, ".get();");
        assertAbsent(code, "MapperS.<BigDecimal>ofNull()");
    }

    /**
     * a10 (round 2, the code-quality review's SF-2 — REFUTED by the oracle): {@code key} on a function CALL returning
     * a keyed value takes the POJO-meta arm exactly as a bare input does; the review proposed narrowing the gate to
     * bare symbols, the released plugin serves the call. Golden CallKeyed (round 2, {@code scratch/oracle-s5c.status}),
     * byte-identical on the fork's first run — the witness that the gate's width is upstream's.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a10_keyOnAFunctionCallReturningAKeyedValue_takesThePojoMetaArm() throws IOException {
        String code = fn("wme", "CallKeyed.java");
        assertContains(code, "final Keyed.KeyedBuilder withMetaArgument = keyOnKeyed.evaluate(k) == null ? null : keyOnKeyed.evaluate(k).toBuilder();");
        assertContains(code, "withMetaArgument.getOrCreateMeta().setExternalKey(\"call-key\");");
        assertContains(code, "r = toBuilder(withMetaArgument);");
    }

    /** control2a: the default route renders every fixture set at its declared population, without a generation error. */
    @Test
    @EnabledIf("builtinsAvailable")
    void control2a_defaultRoute_populationPins() throws IOException {
        // completeness (round 1, spec SF-5): a fixture set added to the dispatch without a pin fails HERE, not silently
        assertEquals(new java.util.TreeSet<>(FIXTURE_SETS), new java.util.TreeSet<>(POPULATION.keySet()),
                "every fixture set the class dispatches over must carry a population pin");
        for (Map.Entry<String, Integer> pin : POPULATION.entrySet()) {
            Render r = render(pin.getKey());
            List<String> keys = fixtureFunctionKeys(r);
            assertEquals(pin.getValue().intValue(), keys.size(),
                    "fixture set " + pin.getKey() + " emits " + pin.getValue() + " functions on the default route; rendered "
                    + keys);
            // the SET, not the count: every `func` the fixture declares is emitted, and nothing else is - the declared
            // names read off the PARSED model (round 2, the spec review's SF-2 / the code-quality review's NIT-1: the
            // first cut ran a regex over `.rosetta` source, which the engineering standard forbids; the parser is the
            // answer, and it is the same builder renderModel feeds)
            java.util.Set<String> declared = declaredFunctionNames(source(pin.getKey()), pin.getKey());
            java.util.Set<String> emitted = new java.util.TreeSet<>();
            for (String k : keys) {
                String simple = k.substring(k.lastIndexOf('/') + 1);
                emitted.add(simple.endsWith(".java") ? simple.substring(0, simple.length() - 5) : simple);
            }
            assertEquals(declared, emitted, "fixture set " + pin.getKey() + ": the emitted function SET must be the declared one");
            assertTrue(r.errors().isEmpty(), "fixture set " + pin.getKey() + " generation errors: " + r.errors());
        }
    }

    /**
     * control2c (round 2, the spec review's SF-5) — control2a's pins proven able to FAIL: the {@code cbe} fixture with
     * its last function deleted renders one function fewer than its pin and its declared set shrinks with it, so a
     * pin that could not move would fail here first. A doctored INPUT, never the shipped code (the seat-3 law for
     * population controls; control3 / control4 do the same for the corpus locks).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void control2c_populationPin_failsOnADoctoredFixture() throws IOException {
        String full = source("cbe");
        int cut = full.lastIndexOf("func MultiWhole:");
        assertTrue(cut > 0, "the cbe fixture ends with MultiWhole");
        String doctored = full.substring(0, cut);
        java.util.Set<String> declared = declaredFunctionNames(doctored, "cbe-doctored");
        assertEquals(POPULATION.get("cbe") - 1, declared.size(), "the doctored fixture declares one function fewer");
        assertTrue(!declared.contains("MultiWhole"), "MultiWhole is the deleted one");
        Render r = renderSource(doctored, "cbe", "cbe-doctored", false);
        assertEquals(POPULATION.get("cbe") - 1, fixtureFunctionKeys(r).size(),
                "the rendered population moves with the input - the pin can fail: " + fixtureFunctionKeys(r));
    }

    /** The functions a fixture DECLARES, read off the parsed model (round 2: no regex over `.rosetta` source). */
    private static java.util.Set<String> declaredFunctionNames(String source, String label) {
        RModel model = AstBuilder.buildFromString(source, "seat5" + label + ".rosetta");
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (RFunction f : AstWalker.findAll(model, RFunction.class)) {
            names.add(f.name());
        }
        return names;
    }

    /**
     * control2b — LAW 77: the IR route renders every fixture set byte-identically to the default route
     * (the same file set, the same bytes per file). Runs only with the IR provider on the classpath
     * ({@code -Pir-on} — the chain's ON-route seat step); the OFF gensuite reports it skipped.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void control2b_irRoute_rendersEveryFixtureIdenticallyToTheDefaultRoute() throws IOException {
        for (String set : POPULATION.keySet()) {
            Render def = render(set);
            Render ir = renderIr(set);
            assertTrue(ir.errors().isEmpty(), set + ": IR-route generation errors: " + ir.errors());
            List<String> defFns = fixtureFunctionKeys(def);
            assertEquals(defFns, fixtureFunctionKeys(ir), set + ": the two routes must emit the SAME function set");
            for (String key : defFns) {
                assertEquals(normalize(def.output().get(key)), normalize(ir.output().get(key)),
                        set + ": the IR route must agree with the default route for " + key);
            }
        }
    }

    /** corpus_c1: the twelve s09 C9Stamp variants byte-identical to the chaos goldens (arm a). */
    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c1_C9Stamp_allTwelveVariants() throws IOException {
        lockChaos("s09", "C9Stamp", FAMILIES_12);
    }

    /** corpus_c2: the twelve s09 C9Sieve variants (arm c). */
    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c2_C9Sieve_allTwelveVariants() throws IOException {
        lockChaos("s09", "C9Sieve", FAMILIES_12);
    }

    /** corpus_c3: the fourteen s04 C4Gate variants, x13half/p1 and a8pkg included (arm d). */
    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c3_C4Gate_allFourteenVariants() throws IOException {
        lockChaos("s04", "C4Gate", FAMILIES_14);
    }

    /** control3: the whole-file lock is proven able to fail — a doctored golden is reported by name. */
    @Test
    @EnabledIf("chaosAvailable")
    void control3_doctoredGolden_isReportedByTheLock() throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        String carrier = "chaos/s04/a1o1/functions/C4Gate.java";
        String generated = chaosOutput.get(carrier);
        assertNotNull(generated, "not generated: " + carrier);
        String golden = normalize(Files.readString(CHAOS_GOLDEN.resolve(carrier)));
        assertTrue(compareCarrier(carrier, golden, generated) == null,
                "the undoctored carrier must lock green before the doctor is applied");
        String doctored = golden.replace("new BigDecimal(bigInteger)", "new BigDecimal(bigInteger).negate()");
        assertTrue(!doctored.equals(golden), "the doctor must change a byte");
        String verdict = compareCarrier(carrier, doctored, generated);
        assertNotNull(verdict, "a doctored golden MUST be reported by the lock");
        assertContains(verdict, "negate()");
    }

    /** control4: the placement-family identity pin proven able to fail (over the fourteen-family s04 set). */
    @Test
    void control4_placementFamilyPin_canFail() {
        assertEquals("a3half", placementFamily("a3half/p2"));
        assertEquals("x13half", placementFamily("x13half/p1"));
        assertEquals("a8pkg", placementFamily("a8pkg"));
        assertEquals("base", placementFamily("base"));
        List<String> good = List.of("a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
                "a3half/p2", "a3hub/p2", "a3third/p3", "a8pkg", "base", "x13half/p1").stream()
                .map(v -> "chaos/s99/" + v + "/functions/X.java").toList();
        assertTrue(placementFamilyVerdict("s99", good, FAMILIES_14) == null, "the enumerated fourteen must pass");
        List<String> doctored = new ArrayList<>(good);
        doctored.set(doctored.indexOf("chaos/s99/x13half/p1/functions/X.java"), "chaos/s99/a3half/p1/functions/X.java");
        String verdict = placementFamilyVerdict("s99", doctored, FAMILIES_14);
        assertNotNull(verdict, "a doctored population MUST be reported");
        String found = verdict.substring(verdict.indexOf("found "));
        assertTrue(!found.contains("x13half"), "the missing family must be absent from the found list: " + found);
        assertTrue(found.indexOf("a3half") != found.lastIndexOf("a3half"), "the doubled family must appear twice: " + found);
    }

    // =========================================================================
    // Fixture harness — functions only, both routes, the workspace's diagnostics kept per render
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors, List<RDiagnostic> diagnostics) {
        List<ValidationDiagnostic> validation(ValidationIssueCode code) {
            return diagnostics.stream()
                    .filter(d -> d instanceof ValidationDiagnostic v && v.issueCode() == code)
                    .map(d -> (ValidationDiagnostic) d)
                    .toList();
        }
    }

    private static final Map<String, Render> RENDERED = new LinkedHashMap<>();
    private static final Map<String, Render> RENDERED_IR = new LinkedHashMap<>();

    private static String source(String set) {
        assertTrue(FIXTURE_SETS.contains(set), "an undeclared fixture set: " + set + " (add it to FIXTURE_SETS and POPULATION)");
        return switch (set) {
            case "wm" -> WM;
            case "wme" -> WME;
            case "wmv" -> WMV;
            case "ml" -> ML;
            case "mle" -> MLE;
            case "cb" -> CB;
            case "cbe" -> CBE;
            default -> throw new IllegalArgumentException(set);
        };
    }

    private static Render render(String set) throws IOException {
        Render r = RENDERED.get(set);
        if (r == null) {
            r = renderModel(set, false);
            RENDERED.put(set, r);
        }
        return r;
    }

    private static Render renderIr(String set) throws IOException {
        Render r = RENDERED_IR.get(set);
        if (r != null) {
            return r;
        }
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            r = renderModel(set, true);
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
        RENDERED_IR.put(set, r);
        return r;
    }

    private static Render renderModel(String set, boolean irRoute) throws IOException {
        return renderSource(source(set), set, set, irRoute);
    }

    /** Renders {@code source} under set {@code set}'s namespace filter; {@code label} names the model file (control2c). */
    private static Render renderSource(String source, String set, String label, boolean irRoute) throws IOException {
        RModel main = AstBuilder.buildFromString(source, "seat5" + label + ".rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linked = RWorkspace.build(models);
        RWorkspace workspace = linked.workspace();
        String namespace = "census.seat5" + set;
        GeneratorModel gm = new GeneratorModel(workspace, m -> namespace.equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = irRoute
                ? IRGeneration.functionGenerator(gm, tt, typeUtil)
                : new FunctionGenerator(gm, tt, typeUtil);
        if (irRoute) {
            assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
        }
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        fg.generateWithErrors(out).forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        return new Render(out, errors, linked.diagnostics());
    }

    private static List<String> fixtureFunctionKeys(Render r) {
        return r.output().keySet().stream()
                .filter(k -> k.contains("/functions/"))
                .sorted()
                .toList();
    }

    /** Functions land under {@code .../functions/}. */
    private static String fn(String set, String fileName) throws IOException {
        return pick(render(set), "functions/" + fileName, fileName);
    }

    private static String pick(Render r, String suffix, String fileName) {
        List<String> own = r.errors().stream().filter(e -> e.contains(fileName)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for " + fileName + " (a broken fixture must fail"
                + " loudly, not skip): " + own);
        List<String> matches = r.output().keySet().stream()
                .filter(k -> k.endsWith("/" + suffix))
                .sorted()
                .toList();
        assertTrue(!matches.isEmpty(), "not generated: " + fileName + " (have: " + r.output().keySet() + ")");
        assertEquals(1, matches.size(), "exactly ONE emitted file may match " + suffix + ": " + matches);
        return normalize(r.output().get(matches.get(0)));
    }

    private static void assertContains(String code, String needle) {
        assertTrue(code.contains(needle), "expected <" + needle + "> in:\n" + code);
    }

    private static void assertAbsent(String code, String needle) {
        assertTrue(!code.contains(needle), "did NOT expect <" + needle + "> in:\n" + code);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    /**
     * {@code null} when the generated text byte-matches the golden (newline-normalised); else the
     * named difference — the ONE comparison every whole-class lock and control3 go through.
     */
    private static String compareCarrier(String carrier, String golden, String generated) {
        String gen = normalize(generated);
        return golden.equals(gen) ? null
                : carrier + ": differs from its golden — " + firstDifference(golden, gen);
    }

    private static String firstDifference(String golden, String generated) {
        String[] g = golden.split("\n");
        String[] r = generated.split("\n");
        int n = Math.min(g.length, r.length);
        for (int i = 0; i < n; i++) {
            if (!g[i].equals(r[i])) {
                return "line " + (i + 1) + " golden <" + g[i].strip() + "> vs generated <" + r[i].strip() + ">";
            }
        }
        return "lengths differ: golden " + g.length + " lines vs generated " + r.length + " lines";
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
            throw new AssertionError("[MetaSchemeConversionSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // The chaos cell — generated once (functions, rules, reports), every carrier locked whole
    // =========================================================================

    private static Map<String, String> chaosOutput;
    private static List<String> chaosGenErrors;

    @BeforeAll
    static void generateChaosCell() throws IOException {
        if (chaosAvailable()) {
            List<String> errs = new ArrayList<>();
            chaosOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("chaos", "1.0.0", CHAOS_ROOT), errs);
            chaosGenErrors = errs;
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

    private static void collect(List<String> errors, List<GenerationException> found) {
        for (GenerationException e : found) {
            errors.add(e.getTargetPath() + " - " + e);
        }
    }

    /** {@code a3half/p2} -> {@code a3half}; {@code x13half/p1} -> {@code x13half}; {@code base} -> {@code base}. */
    private static String placementFamily(String variant) {
        int slash = variant.indexOf('/');
        return slash < 0 ? variant : variant.substring(0, slash);
    }

    /**
     * {@code null} when {@code carriers} (golden-relative paths under {@code chaos/<seat>/}) hold every
     * placement FAMILY of {@code expectedFamilies} exactly once; else the named difference.
     */
    private static String placementFamilyVerdict(String seat, List<String> carriers, List<String> expectedFamilies) {
        String prefix = "chaos/" + seat + "/";
        List<String> families = carriers.stream()
                .map(c -> c.substring(prefix.length(), c.indexOf("/functions/")))
                .map(MetaSchemeConversionSeatTest::placementFamily)
                .sorted()
                .toList();
        List<String> expected = expectedFamilies.stream().sorted().toList();
        return expected.equals(families) ? null
                : "expected one carrier per placement family " + expected + ", found " + families;
    }

    /**
     * Byte-lock every placement variant of one declared function class against the chaos goldens. A
     * carrier the generator refused, did not emit, or emitted differently fails by NAME.
     */
    private static void lockChaos(String seat, String simpleName, List<String> families) throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        List<String> carriers;
        Path seatRoot = CHAOS_GOLDEN.resolve("chaos").resolve(seat);
        try (var stream = Files.walk(seatRoot)) {
            carriers = stream
                    .filter(p -> p.getFileName().toString().equals(simpleName + ".java"))
                    .filter(p -> p.getParent().getFileName().toString().equals("functions"))
                    .map(p -> CHAOS_GOLDEN.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
        assertEquals(families.size(), carriers.size(),
                "expected the declared " + families.size() + " placement variants of " + simpleName + " under " + seatRoot
                + ", found " + carriers);
        String familyVerdict = placementFamilyVerdict(seat, carriers, families);
        assertTrue(familyVerdict == null,
                "the carriers of " + simpleName + " must be one per placement family: " + familyVerdict);
        List<String> failures = new ArrayList<>();
        for (String carrier : carriers) {
            List<String> own = chaosGenErrors.stream().filter(e -> e.contains(carrier)).toList();
            if (!own.isEmpty()) {
                failures.add(carrier + ": generator errors " + own);
                continue;
            }
            String generated = chaosOutput.get(carrier);
            if (generated == null) {
                failures.add(carrier + ": not generated (have "
                        + chaosOutput.keySet().stream().filter(k -> k.endsWith(simpleName + ".java")).toList() + ")");
                continue;
            }
            Path goldenPath = CHAOS_GOLDEN.resolve(carrier);
            if (!Files.isRegularFile(goldenPath)) {
                failures.add(carrier + ": golden missing at " + goldenPath);
                continue;
            }
            String verdict = compareCarrier(carrier, normalize(Files.readString(goldenPath)), generated);
            if (verdict != null) {
                failures.add(verdict);
            }
        }
        assertTrue(failures.isEmpty(), "carriers of " + simpleName + " not byte-identical to the chaos"
                + " goldens (" + failures.size() + "/" + families.size() + "):\n  "
                + String.join("\n  ", failures));
    }
}
