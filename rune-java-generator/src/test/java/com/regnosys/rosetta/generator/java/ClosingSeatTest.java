package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;

/**
 * v3.2 SEAT 13 (PR #634, D53) — THE CLOSING SEAT SUITE: v3.2 closes when NOTHING SILENT remains on the chaos cell, so
 * every NON-COMPILING silent class of the seat-13 javac census owes a register site on the LOUD line — each proven ABLE
 * TO FIRE by a control whose shape is the chaos row's in miniature, each bounded by a negative control that the
 * neighbouring blessed shape still renders. The heals are v3.3's (D53 decision 2): every site here is a v3.3 heal's
 * own witness, re-cut to the healed render when the heal lands (LAW 73).
 *
 * <ul>
 *   <li><b>c4 / n4 — {@code ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE}</b> (site R4, {@code FunctionAliasHelper
 *       .inferEnumValueRefType}'s 2-name-root fallback): an alias whose then-piped extract binds a lambda parameter and
 *       reads a feature off it ({@code … then extract l2 [ l2 -> v ] …}, the chaos s29 {@code C29Piped} shape — the
 *       parser carries {@code l2 -> v} as a 2-name ref, the extract has no bare-symbol left, the pre-seat seam was
 *       {@code MapperC<l2>}) REFUSES; the same 2-name root under an extract WITH a bare-symbol left
 *       ({@code leaves extract l2 [ l2 -> v ]}) still types through the extract-item arm and renders
 *       {@code MapperC<BigDecimal>}.</li>
 *   <li><b>c5 / n5 — {@code ALIAS_BODY_ITEM_UNWRAPPED}</b> (site R6, {@code FunctionExpressionRenderer
 *       .renderAliasThenHoistOrNull}'s return): a then-chain alias whose consumer is a bare {@code count}
 *       ({@code bs extract b [ b -> lid ] then count}, the chaos s23 {@code C23Scale} shape — the count render's
 *       primitive {@code int} returned against a {@code MapperS<Integer>} seam) REFUSES; a then-chain alias whose
 *       consumer is a Mapper ({@code … then extract lid}, the implicit item) still renders its {@code return thenArg…;}.
 *       (A named-parameter then-extract reading a feature off its own parameter is R4's shape - it refuses there first.)</li>
 *   <li><b>c6 / n6 — {@code WITH_META_UNTYPED_STUB}</b> (site R5, {@code ConstructionHandler.handle(RWithMetaExpr)}'s
 *       legacy M7b-1 stub): a with-meta construction over {@code empty} ({@code empty with-meta { reference: href }},
 *       the chaos s24 {@code C24WithMetaPlain} shape — the pre-seat render {@code null.toBuilder().setMeta(…)})
 *       REFUSES; a with-meta over a typed value into a scheme-annotated output still renders the typed wrapper
 *       ({@code FieldWithMetaString.builder().setValue(…)}).</li>
 *   <li><b>c7 / n7 — {@code VOID_INTO_META_OUTPUT}</b> (site R13, the single-output SET fallback): a Void-typed value
 *       ({@code c -> tok}, a model-declared {@code basicType}) SET into a scheme-annotated output (the chaos s24
 *       {@code C24IntoMeta} shape — the pre-seat render {@code toBuilder(<chain>.<Void>map(…).get())}) REFUSES; a
 *       plain string into a scheme-annotated string output keeps its {@code toBuilder(…)} wrap.</li>
 *   <li><b>c8 / n8 — {@code INLINE_CONDITIONAL_TERNARY}</b> (site R14, {@code ControlFlowHandler.handle(RConditionalExpr)}'s
 *       last-resort ternary): a conditional at a pathed ADD with list arms of different element types
 *       ({@code add w -> scores: if n > 0 then [n, n + 1] else [0]}, the chaos s30 {@code C30MultiThenAdd} shape)
 *       REFUSES; a conditional at a whole-output SET still renders its hoisted {@code ifThenElseResult} block.</li>
 *   <li><b>c9 / n9 — {@code CALL_ARGUMENT_COERCION_DROPPED}</b> (site R7, the explicit-args evaluate loop's positions
 *       with no hoist consumer): a call nested in a REDUCE lambda whose argument is an Integer navigation into a
 *       {@code number} parameter (route NONE — neither an extract/map lambda, a then body nor a filter predicate)
 *       REFUSES; the same coercion at a function's whole-output SET seat (the BLOCK route) still renders
 *       ({@code BigDecimal.valueOf(}). The chaos s27 {@code C27NatCalled} shape itself (a typeAlias condition
 *       calling a function with its int-grained {@code item}) is HEALED by this seat, not refused — the oracle group
 *       {@code arg-coercion-bare-local} pins the released plugin's inline bare-local coercion
 *       ({@code check.evaluate((nat == null ? null : BigDecimal.valueOf(nat)))}) and the fork renders it byte-exact;
 *       the first c9 (that shape) reached no refusal once the alias-condition item was typed and was WITHDRAWN for
 *       this NONE-position witness.</li>
 *   <li><b>c10 / n10 — {@code HOIST_ARM_COERCION_DROPPED}</b> (site R10, {@code HandlerHelper
 *       .refuseIfBareValueIntoMetaLocal} at {@code ControlFlowHandler.hoistAsItemLocalOrNull}'s arm renderer and the
 *       FER pathed-conditional arms - ONE predicate): a pathed conditional SET at a META leaf after a whole-output ctor
 *       SET ({@code set w -> code: if a exists then a else "z"} under {@code code string [metadata scheme]}, the chaos
 *       s30 {@code C30TwoOp} shape - the bare String arm assigned into a {@code FieldWithMetaString} local) REFUSES;
 *       the same conditional at a PLAIN leaf keeps its item local ({@code .setCode(ifThenElseResult…)}).</li>
 *   <li><b>r1 — the LOUD line</b> carries every seat-13 site name ({@code SilentDegradation.render()}), each at 0
 *       after a reset, and the register's size is pinned EXACTLY (the seat-12 suite keeps a floor).</li>
 * </ul>
 *
 * <p>Both routes: the alias signature and the alias body are rendered by the function generator the IR provider swaps
 * for a subclass that keeps the helper and the renderer, so both sites are consulted on {@code -Pir-on} by identity
 * (LAW 77); the chaos D11 on both routes is the witness ({@code target/v32-seat13-instruments/scratch/d11-*.status},
 * local). No test here gates on the IR provider, so the ON-route seat step of the chain runs every test again with 0
 * skipped. The register is JVM-global and the module reuses forks: every control resets it before and after (the
 * seat-7 control5 law).
 */
class ClosingSeatTest {

    // =========================================================================
    // Fixtures (the chaos rows' shapes in miniature)
    // =========================================================================

    private static final String ALIAS_THEN_PIPED_EXTRACT = """
            namespace census.seat13c4
            version "0.0.0"

            type Leaf: <"The leaf.">
                v number (1..1)
            type Outer: <"The outer with leaves.">
                leaves Leaf (0..*)

            func Piped: <"A then-piped extract binding a lambda parameter and reading a feature off it - the chaos s29 C29Piped shape.">
                inputs:
                    outers Outer (0..*)
                output:
                    vs number (0..*)
                alias piped: outers extract o [ o -> leaves then filter l [ l -> v > 0 ] then extract l2 [ l2 -> v ] ] then flatten
                add vs: piped
            """;

    private static final String ALIAS_EXTRACT_WITH_LEFT = """
            namespace census.seat13n4
            version "0.0.0"

            type Leaf: <"The leaf.">
                v number (1..1)

            func Plain: <"The same 2-name root under an extract WITH a bare-symbol left - the extract-item arm types it (the blessed shape).">
                inputs:
                    leaves Leaf (0..*)
                output:
                    vs number (0..*)
                alias picked: leaves extract l2 [ l2 -> v ]
                add vs: picked
            """;

    private static final String ALIAS_THEN_COUNT = """
            namespace census.seat13c5
            version "0.0.0"

            type Box: <"The box.">
                lid string (0..1)

            func Counted: <"A then-chain alias whose consumer is a bare count - the chaos s23 C23Scale `counted` shape.">
                inputs:
                    bs Box (0..*)
                output:
                    r number (1..1)
                alias counted: bs extract b [ b -> lid ] then count
                set r: counted
            """;

    private static final String ALIAS_THEN_MAPPER = """
            namespace census.seat13n5
            version "0.0.0"

            type Box: <"The box.">
                lid string (0..1)
                inner Box (0..1)

            func Piped: <"A then-chain alias whose consumer is a Mapper - the blessed then-hoist shape.">
                inputs:
                    bs Box (0..*)
                output:
                    lids string (0..*)
                alias lids2: bs extract b [ b -> inner ] then extract lid
                add lids: lids2
            """;

    private static final String WITH_META_OVER_EMPTY = """
            namespace census.seat13c6
            version "0.0.0"

            metaType reference string
            type Ref: <"The referenced type.">
                mark string (1..1)

            func Plain: <"A with-meta over `nothing` into a plain output - the chaos s24 C24WithMetaPlain shape.">
                inputs:
                    href string (0..1)
                output:
                    r Ref (0..1)
                set r: empty with-meta { reference: href }
            """;

    private static final String WITH_META_TYPED = """
            namespace census.seat13n6
            version "0.0.0"

            metaType scheme string

            func Typed: <"A with-meta over a typed value into a scheme-annotated output - the typed arm's shape (the blessed seat).">
                inputs:
                    s string (1..1)
                output:
                    r string (1..1)
                        [metadata scheme]
                set r: s with-meta { scheme: "census" }
            """;

    private static final String VOID_INTO_META = """
            namespace census.seat13c7
            version "0.0.0"

            metaType scheme string
            basicType tok <"A model-declared basic type - Void in the generated Java (D47).">
            type Carrier: <"A Void-typed attribute.">
                tok tok (0..1)

            func IntoMeta: <"A Void value SET into a scheme-annotated Void output - the chaos s24 C24IntoMeta shape.">
                inputs:
                    c Carrier (0..1)
                output:
                    t tok (0..1)
                        [metadata scheme]
                set t: c -> tok
            """;

    private static final String STRING_INTO_META = """
            namespace census.seat13n7
            version "0.0.0"

            metaType scheme string
            type Carrier: <"A string attribute.">
                name string (0..1)

            func IntoMeta: <"A plain string SET into a scheme-annotated string output - the blessed toBuilder seat.">
                inputs:
                    c Carrier (0..1)
                output:
                    t string (0..1)
                        [metadata scheme]
                set t: c -> name
            """;

    private static final String TERNARY_AT_ADD = """
            namespace census.seat13c8
            version "0.0.0"

            type Whole: <"The ADD target.">
                scores number (0..*)

            func MultiThenAdd: <"A conditional at a pathed ADD with list arms of different element types - the chaos s30 C30MultiThenAdd shape.">
                inputs:
                    n number (1..1)
                output:
                    w Whole (1..1)
                set w -> scores: [n]
                add w -> scores: if n > 0 then [n, n + 1] else [0]
            """;

    private static final String CONDITIONAL_AT_SET = """
            namespace census.seat13n8
            version "0.0.0"

            func Pick: <"A conditional at a whole-output SET - the hoisted ifThenElseResult block (the blessed seat).">
                inputs:
                    flag boolean (1..1)
                    a string (1..1)
                output:
                    r string (1..1)
                set r: if flag then a else "z"
            """;

    private static final String REDUCE_NESTED_CALL = """
            namespace census.seat13c9
            version "0.0.0"

            type Holder: <"The reduced item.">
                tally int (0..1)

            func Check: <"The callee whose parameter needs the coercion.">
                inputs:
                    v number (1..1)
                output:
                    ok boolean (1..1)
                set ok: v >= 0

            func Pick: <"The outer call the coerced call is nested in.">
                inputs:
                    x Holder (1..1)
                    y Holder (1..1)
                    flag boolean (1..1)
                output:
                    h Holder (1..1)
                set h: if flag then x else y

            func ReduceCall: <"A call nested in a REDUCE lambda whose argument (an Integer navigation) needs the number coercion - route NONE, the position with no hoist consumer.">
                inputs:
                    hs Holder (0..*)
                output:
                    h Holder (0..1)
                set h: hs reduce a, b [ Pick(a, b, Check(a -> tally)) ]
            """;

    private static final String FUNCTION_SET_CALL = """
            namespace census.seat13n9
            version "0.0.0"

            func Check: <"The callee.">
                inputs:
                    v number (1..1)
                output:
                    ok boolean (1..1)
                set ok: v >= 0
            func Call: <"The same call at a function's whole-output SET seat - the BLOCK route hoists and coerces (the blessed seat).">
                inputs:
                    n int (1..1)
                output:
                    ok boolean (1..1)
                set ok: Check(n)
            """;

    private static final String META_LEAF_BARE_ARM = """
            namespace census.seat13c10
            version "0.0.0"

            metaType scheme string

            type Whole: <"The holder with a scheme-annotated leaf.">
                name string (0..1)
                code string (0..1)
                    [metadata scheme]

            func TwoOp: <"A pathed conditional SET at a META leaf after a whole-output ctor SET - the chaos s30 C30TwoOp shape: a bare string arm into a FieldWithMetaString hoist local.">
                inputs:
                    a string (0..1)
                output:
                    w Whole (1..1)
                set w: Whole { name: "root", ... }
                set w -> code: if a exists then a else "z"
            """;

    private static final String PLAIN_LEAF_BARE_ARM = """
            namespace census.seat13n10
            version "0.0.0"

            type Whole: <"The holder with a plain leaf.">
                name string (0..1)
                code string (0..1)

            func TwoOp: <"The same conditional at a PLAIN leaf - the item local is the golden form (the blessed shape).">
                inputs:
                    a string (0..1)
                output:
                    w Whole (1..1)
                set w: Whole { name: "root", ... }
                set w -> code: if a exists then a else "z"
            """;

    // =========================================================================
    // c4 / n4 — ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE
    // =========================================================================

    @Test
    void c4_thenPipedExtractLambdaParameterRoot_refusedAtAliasSignatureClosureParamType() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(ALIAS_THEN_PIPED_EXTRACT, "c4");
            assertOneRefusal(run, SilentDegradation.Site.ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE,
                    "census/seat13c4/functions/Piped.java", "whose root is the closure parameter 'l2'");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void n4_extractWithBareSymbolLeft_stillTypesThroughTheExtractItemArm() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(ALIAS_EXTRACT_WITH_LEFT, "n4");
            assertEquals(List.of(), run.errorMessages(), "no refusal on the blessed shape");
            String src = file(run.output(), "census/seat13n4/functions/Plain.java");
            assertContains(src, "MapperC<BigDecimal> picked(", "the extract-item arm types the seam");
            assertAbsent(src, "MapperC<l2>", "no lambda parameter as a type");
            assertEquals(0, SilentDegradation.counts().get(SilentDegradation.Site.ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE));
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // c5 / n5 — ALIAS_BODY_ITEM_UNWRAPPED
    // =========================================================================

    @Test
    void c5_thenCountAliasBody_refusedAtAliasBodyItemUnwrapped() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(ALIAS_THEN_COUNT, "c5");
            assertOneRefusal(run, SilentDegradation.Site.ALIAS_BODY_ITEM_UNWRAPPED,
                    "census/seat13c5/functions/Counted.java", "the consumer's compiled type is the item int");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void n5_thenMapperAliasBody_stillRendersItsReturn() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(ALIAS_THEN_MAPPER, "n5");
            assertEquals(List.of(), run.errorMessages(), "no refusal on the blessed shape");
            String src = file(run.output(), "census/seat13n5/functions/Piped.java");
            assertContains(src, "protected MapperC<String> lids2(", "the Mapper seam");
            assertContains(src, "return thenArg", "the then-hoist's return of its Mapper consumer");
            assertEquals(0, SilentDegradation.counts().get(SilentDegradation.Site.ALIAS_BODY_ITEM_UNWRAPPED));
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // c6 / n6 — WITH_META_UNTYPED_STUB
    // =========================================================================

    @Test
    void c6_withMetaOverEmpty_refusedAtWithMetaUntypedStub() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(WITH_META_OVER_EMPTY, "c6");
            assertOneRefusal(run, SilentDegradation.Site.WITH_META_UNTYPED_STUB,
                    "census/seat13c6/functions/Plain.java", "whose meta wrapper no typed arm could derive");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void n6_withMetaOverTypedValue_stillRendersTheTypedWrapper() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(WITH_META_TYPED, "n6");
            assertEquals(List.of(), run.errorMessages(), "no refusal on the blessed shape");
            String src = file(run.output(), "census/seat13n6/functions/Typed.java");
            assertContains(src, "FieldWithMetaString.builder().setValue(", "the typed wrapper arm");
            assertAbsent(src, ".toBuilder().setMeta(MetaFields.builder()", "no M7b-1 stub");
            assertEquals(0, SilentDegradation.counts().get(SilentDegradation.Site.WITH_META_UNTYPED_STUB));
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // c7 / n7 — VOID_INTO_META_OUTPUT
    // =========================================================================

    @Test
    void c7_voidValueIntoMetaOutput_refusedAtVoidIntoMetaOutput() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(VOID_INTO_META, "c7");
            assertOneRefusal(run, SilentDegradation.Site.VOID_INTO_META_OUTPUT,
                    "census/seat13c7/functions/IntoMeta.java", "a Void-typed value assigned to the meta-annotated output 't'");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void n7_stringIntoMetaOutput_keepsTheToBuilderWrap() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(STRING_INTO_META, "n7");
            assertEquals(List.of(), run.errorMessages(), "no refusal on the blessed shape");
            String src = file(run.output(), "census/seat13n7/functions/IntoMeta.java");
            assertContains(src, "toBuilder(", "the meta output's toBuilder wrap");
            assertEquals(0, SilentDegradation.counts().get(SilentDegradation.Site.VOID_INTO_META_OUTPUT));
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // c8 / n8 — INLINE_CONDITIONAL_TERNARY
    // =========================================================================

    @Test
    void c8_conditionalAtPathedAddWithListArms_refusedAtInlineConditionalTernary() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(TERNARY_AT_ADD, "c8");
            assertOneRefusal(run, SilentDegradation.Site.INLINE_CONDITIONAL_TERNARY,
                    "census/seat13c8/functions/MultiThenAdd.java", "conditional at a seat with no hoist renderer");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void n8_conditionalAtWholeOutputSet_stillRendersTheHoistBlock() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(CONDITIONAL_AT_SET, "n8");
            assertEquals(List.of(), run.errorMessages(), "no refusal on the blessed shape");
            String src = file(run.output(), "census/seat13n8/functions/Pick.java");
            assertAbsent(src, ".getOrDefault(false) ? ", "no inline ternary");
            assertContains(src, "if (", "the hoisted conditional block");
            assertEquals(0, SilentDegradation.counts().get(SilentDegradation.Site.INLINE_CONDITIONAL_TERNARY));
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // c9 / n9 — CALL_ARGUMENT_COERCION_DROPPED
    // =========================================================================

    @Test
    void c9_callNestedInReduceLambdaWithIntNavIntoNumberParam_refusedAtCallArgumentCoercionDropped() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(REDUCE_NESTED_CALL, "c9");
            assertOneRefusal(run, SilentDegradation.Site.CALL_ARGUMENT_COERCION_DROPPED,
                    "census/seat13c9/functions/ReduceCall.java",
                    "argument 0 of `Check(...)` needs the Integer -> BigDecimal coercion its parameter 'v' declares, and the call sits in a position with no hoist consumer (route NONE)");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void n9_functionSetSeatCall_keepsItsHoistedCoercion() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(FUNCTION_SET_CALL, "n9");
            assertEquals(List.of(), run.errorMessages(), "no refusal on the blessed shape");
            String src = file(run.output(), "census/seat13n9/functions/Call.java");
            assertContains(src, "BigDecimal.valueOf(", "the BLOCK route's coercion");
            assertEquals(0, SilentDegradation.counts().get(SilentDegradation.Site.CALL_ARGUMENT_COERCION_DROPPED));
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // r1 — the LOUD line
    // =========================================================================

    // =========================================================================
    // c10 / n10 — HOIST_ARM_COERCION_DROPPED
    // =========================================================================

    @Test
    void c10_bareStringArmIntoMetaLeafHoist_refusedAtHoistArmCoercionDropped() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(META_LEAF_BARE_ARM, "c10");
            assertOneRefusal(run, SilentDegradation.Site.HOIST_ARM_COERCION_DROPPED,
                    "census/seat13c10/functions/TwoOp.java",
                    "a bare String conditional arm at a META leaf of 'w' whose hoisted local is FieldWithMetaString");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void n10_bareStringArmIntoPlainLeafHoist_keepsItsItemLocal() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(PLAIN_LEAF_BARE_ARM, "n10");
            assertEquals(List.of(), run.errorMessages(), "no refusal on the blessed shape");
            String src = file(run.output(), "census/seat13n10/functions/TwoOp.java");
            assertContains(src, "ifThenElseResult", "the hoisted conditional local");
            assertContains(src, ".setCode(ifThenElseResult", "the item local consumed by the setter");
            assertEquals(0, SilentDegradation.counts().get(SilentDegradation.Site.HOIST_ARM_COERCION_DROPPED));
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void r1_loudLineCarriesTheSeat13Sites_atZeroAfterReset() {
        SilentDegradation.reset();
        String line = SilentDegradation.render();
        for (String site : List.of("ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE", "ALIAS_BODY_ITEM_UNWRAPPED",
                "WITH_META_UNTYPED_STUB", "VOID_INTO_META_OUTPUT", "INLINE_CONDITIONAL_TERNARY",
                "CALL_ARGUMENT_COERCION_DROPPED", "HOIST_ARM_COERCION_DROPPED")) {
            assertContains(line, " " + site + "=0", "the LOUD line");
        }
        assertEquals(22, SilentDegradation.Site.values().length, "twenty-two register sites at seat 13 commit 4");
    }

    // =========================================================================
    // Harness — the hold-out bars' own all-kinds pipeline over an in-memory model (the seat-11 shape)
    // =========================================================================

    private static HoldOutByteCompareTest.GenerationRun render(String source, String label) throws IOException {
        List<Path> builtinFiles = HoldOutByteCompareTest.resolveBuiltinFiles();
        Assumptions.assumeTrue(!builtinFiles.isEmpty(), "rune-dsl builtins absent - the seat suite needs them");
        List<RModel> models = new ArrayList<>();
        for (Path p : builtinFiles) {
            models.add(AstBuilder.buildFromFile(p));
        }
        RModel m = AstBuilder.buildFromString(source, "seat13-" + label + ".rosetta");
        Set<RModel> group = Collections.newSetFromMap(new IdentityHashMap<>());
        group.add(m);
        models.add(m);
        return HoldOutByteCompareTest.generateAllKindsFromModels(models, group);
    }

    private static void assertOneRefusal(HoldOutByteCompareTest.GenerationRun run, SilentDegradation.Site site,
            String targetPath, String needle) {
        List<GenerationException> errors = run.errors();
        assertEquals(1, errors.size(), "exactly one refusal: " + run.errorMessages());
        GenerationException e = errors.get(0);
        assertTrue(e instanceof SilentDegradation.Refusal r && r.site() == site, "the refusal's site: " + e);
        assertEquals(targetPath, e.getTargetPath());
        assertFalse(run.output().containsKey(targetPath), "the refused element does not emit");
        assertEquals(1, SilentDegradation.counts().get(site), "the site counted once");
        assertContains(SilentDegradation.witnesses(site).get(0), needle, "the witness");
    }

    private static String file(Map<String, String> out, String path) {
        String s = out.get(path);
        assertTrue(s != null, "not generated: " + path + " (have: " + out.keySet() + ")");
        return s.replace("\r\n", "\n");
    }

    private static void assertContains(String text, String needle, String where) {
        assertTrue(text.contains(needle), where + ": expected <" + needle + "> in:\n" + text);
    }

    private static void assertAbsent(String text, String needle, String where) {
        assertFalse(text.contains(needle), where + ": did not expect <" + needle + "> in:\n" + text);
    }
}
