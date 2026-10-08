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
 * v3.2 SEAT 12 (PR #633, D52) — THE SILENT-CLASS SEAT SUITE: the three register sites the seat adds to the LOUD line
 * BEFORE any heal (COUNTERS FIRST — the seat-3 precedent), each proven ABLE TO FIRE by a control whose shape is a chaos
 * row's in miniature, and each bounded by a negative control that the neighbouring blessed seat still renders:
 *
 * <ul>
 *   <li><b>c1 / n1 — {@code SWITCH_TERNARY_STUB}</b> ({@code ControlFlowHandler.handle(RSwitchExpr)}, the residual
 *       inline-ternary fall-through AFTER the two guarded sites): a switch at a {@code +} operand of a SET body (the chaos
 *       s31 {@code C31Which} shape) reaches the ternary and REFUSES; a switch AT the SET position (the assignment
 *       ladder, {@code FunctionExpressionRenderer.renderSwitchAssignment}) still renders, with no
 *       {@code Objects.equals(} in the file.</li>
 *   <li><b>c2 / n2 — {@code DEEP_PATH_UTIL_UNRESOLVED}</b> ({@code NavigationHandler.handle(RDeepFeatureCall)}, the
 *       {@code TODO(M7b-4)} stub fallback): a deep call whose receiver is ITSELF a deep call ({@code o ->> inners ->>
 *       deep}, the chaos s29 {@code C29LoL} shape — the resolver has no arm for a deep-call receiver) REFUSES; a deep
 *       call on the lambda item ({@code o ->> text}, the seat-6 heal) still renders WITH its {@code @Inject
 *       <Type>DeepPathUtil} field and no placeholder.</li>
 *   <li><b>c3 / n3 — {@code ALIAS_SIGNATURE_RAW_TYPE}</b> ({@code FunctionAliasHelper.computeReturnType}'s raw-name
 *       fallback, gated on a BUILTIN output type): an alias the signature walk declines in a function whose output is
 *       {@code number} (a reduce over counts, the chaos s25 {@code C25Shadow} shape — the pre-seat seam
 *       {@code MapperS<? extends number>}) REFUSES; the same declining shape in a function whose output is a MODEL type
 *       keeps the fallback (its raw name is the Java simple name) and renders.</li>
 *   <li><b>r1 — the LOUD line</b> carries the three names ({@code SilentDegradation.render()}), each at 0 after a
 *       reset.</li>
 *   <li><b>h1 / h1n — M12 the duplicate closure parameter, HEALED</b> (D52 H1, {@code HandlerHelper.reduceParamRenderNames}
 *       / {@code reduceParamReadName} - the ONE predicate both halves consult, LAW 69): {@code reduce a, a [ a + a ]}
 *       renders upstream's numbered pair {@code (a0, a1)} with every read bound to the FIRST; two distinct names stay
 *       raw. The hold-out group {@code closure-param-duplicate} pins the released plugin's bytes.</li>
 *   <li><b>h1v / h1vn — the duplicate pair NAVIGATED off</b> (round 1, cq MF-1): a closure-parameter RECEIVER of
 *       the shared name ({@code reduce a, a [ MergeV(a, a -> v) ]}) renders the first parameter's numbered name too
 *       ({@code a0.<BigDecimal>map(...)} - {@code NavigationHandler}'s receiver seat reads the same predicate); the
 *       distinct-name receivers stay raw. The hold-out group {@code closure-param-duplicate-reads} pins the
 *       released plugin's bytes (its call-read shape, {@code Merge(a, a)}, was identical BEFORE the round-1 line -
 *       the review's second seat refuted by measurement).</li>
 *   <li><b>h2 / h2m / h2n — M8a the meta wrapper into a rule's plain output, HEALED</b> (D52 H2): the meta-recovery
 *       walker's FUNCTION-callee case ({@code NavigationHandler.recoverMetaFromExpr}) recovers the callee's declared
 *       meta output, and the single-output SET seat's gate admits a direct call body - the wrapper is hoisted and
 *       unwrapped (single) or mapped by the "Type coercion" map (multi); a plain-output callee keeps the bare form.
 *       The hold-out group {@code rule-meta-output-unwrap} pins the released plugin's bytes.</li>
 *   <li><b>h3 / h3f / h3n — M6 the {@code item ->} only-exists root, HEALED</b> (D52 H3): the parser builds the
 *       item root as a real {@code RImplicitVariable} ({@code AstBuilder.buildOnlyExistsPath}) and the generator's
 *       existing gm-aware receiver walk types it from its context - {@code MapperS.of(<instance>)} with the
 *       declaring type's attributes in a condition, {@code item} inside a lambda, a hop over it as the navigation;
 *       the bare-symbol and explicit-parameter forms keep their bytes. The hold-out group
 *       {@code only-exists-item-root} pins the released plugin's bytes.</li>
 * </ul>
 *
 * <p>Both routes: the IR route serves a switch and a deep feature call through these same handlers as oracle roots
 * ({@code IRExpressionCompiler.visitSwitch} → {@code super}; the deep call at the #507 targeted set), and the alias
 * signature is rendered by the function generator the IR provider swaps for a subclass that keeps the helper — so the
 * three sites are consulted on {@code -Pir-on} by identity (LAW 77); the chaos D11 on both routes is the witness
 * ({@code target/v32-seat12-instruments/scratch/d11-*.status}, local). No test here gates on the IR provider, so the
 * ON-route seat step of the chain runs every test again with 0 skipped. The register is JVM-global and the module
 * reuses forks: every control resets it before and after (the seat-7 control5 law).
 */
class SilentClassSeatTest {

    // =========================================================================
    // Fixtures (the chaos rows' shapes in miniature)
    // =========================================================================

    private static final String SWITCH_OPERAND = """
            namespace census.seat12c1
            version "0.0.0"

            enum SideEnum: <"The E2 pair's first enum.">
                Long
                Short

            func Which: <"A switch at a + operand of a SET body - the chaos s31 C31Which shape: no ladder renderer at the operand seat.">
                inputs:
                    s SideEnum (1..1)
                output:
                    r string (1..1)
                set r: (s switch Long then "side-long", Short then "side-short") + "!"
            """;

    private static final String SWITCH_SET = """
            namespace census.seat12n1
            version "0.0.0"

            enum SideEnum: <"The E2 pair's first enum.">
                Long
                Short

            func Plain: <"A switch AT the SET position - the assignment ladder renders it (the blessed seat).">
                inputs:
                    s SideEnum (1..1)
                output:
                    r string (1..1)
                set r: s switch Long then "side-long", Short then "side-short", default "x"
            """;

    private static final String DEEP_CHAIN = """
            namespace census.seat12c2
            version "0.0.0"

            type In1: <"Inner option 1.">
                deep string (0..1)
            type In2: <"Inner option 2.">
                deep string (0..1)
            choice Inner: <"The inner choice.">
                In1
                In2
            type Note: <"Outer option 1.">
                text string (0..1)
                inners Inner (0..*)
            type Wrap: <"Outer option 2 - the same attributes.">
                text string (0..1)
                inners Inner (0..*)
            choice Outer: <"The outer choice.">
                Note
                Wrap

            func Chained: <"A deep call whose RECEIVER is itself a deep call - the chaos s29 C29LoL shape.">
                inputs:
                    outers Outer (0..*)
                output:
                    vs string (0..*)
                add vs: outers extract o [ o ->> inners ->> deep ]
            """;

    private static final String DEEP_ITEM = """
            namespace census.seat12n2
            version "0.0.0"

            type Note: <"Outer option 1.">
                text string (0..1)
            type Wrap: <"Outer option 2 - the same attribute.">
                text string (0..1)
            choice Outer: <"The outer choice.">
                Note
                Wrap

            func Direct: <"A deep call on the lambda item - the seat-6 heal: the util resolves and injects.">
                inputs:
                    outers Outer (0..*)
                output:
                    ts string (0..*)
                add ts: outers extract o [ o ->> text ]
            """;

    private static final String ALIAS_BUILTIN = """
            namespace census.seat12c3
            version "0.0.0"

            type Sub: <"Helper.">
                sname string (0..1)
                subs number (0..*)
            type Paths: <"The carrier - a closure parameter named like its attribute.">
                sub Sub (0..1)
                subs Sub (0..*)

            func Shadow: <"An alias the signature walk declines in a NUMBER-output function - the chaos s25 C25Shadow shape.">
                inputs:
                    ps Paths (0..*)
                output:
                    n number (1..1)
                alias total: ps extract sub [ sub -> subs count ] then reduce a, b [ a + b ]
                set n: total
            """;

    private static final String ALIAS_MODEL = """
            namespace census.seat12n3
            version "0.0.0"

            type Sub: <"Helper.">
                sname string (0..1)
                subs number (0..*)
            type Paths: <"The carrier.">
                sub Sub (0..1)
                subs Sub (0..*)
            type Tally: <"A model output type - its raw name IS the Java simple name.">
                n number (0..1)

            func ShadowModel: <"The same declining alias shape in a MODEL-output function - the fallback keeps its bytes.">
                inputs:
                    ps Paths (0..*)
                output:
                    t Tally (1..1)
                alias total: ps extract sub [ sub -> subs count ] then reduce a, b [ a + b ]
                set t -> n: total
            """;

    private static final String REDUCE_DUP = """
            namespace census.seat12h1
            version "0.0.0"

            type Item: <"The M12 carrier's element.">
                v number (1..1)

            func ReduceDup: <"Two closure parameters of ONE name - the chaos s95 C95Reduce shape (M12).">
                inputs:
                    items Item (0..*)
                output:
                    total number (1..1)
                set total: items extract i [ i -> v ] then reduce a, a [ a + a ]
            """;

    private static final String REDUCE_DUP_NAV = """
            namespace census.seat12h1v
            version "0.0.0"

            type Item: <"The M12 carrier's element.">
                v number (1..1)

            func MergeV: <"An item and a number in, the item out.">
                inputs:
                    x Item (1..1)
                    n number (1..1)
                output:
                    z Item (1..1)
                set z: x

            func ReduceDupNav: <"The shared-name pair NAVIGATED off (`a -> v`) - the closure-parameter receiver seat.">
                inputs:
                    items Item (0..*)
                output:
                    pick Item (1..1)
                set pick: items reduce a, a [ MergeV(a, a -> v) ]
            """;
    /**
     * h1x (round 2, cq SF-1): a NON-reduce owner declaring the shared name TWICE. The fork's front end admits N closure
     * parameters on every inline function (upstream refuses `extract a, a`); the predicate's own gate keeps every read raw
     * for such an owner - at commit 13 the receiver seat alone would have numbered it (`a0.` beside a lambda declaring `a`).
     */
    private static final String EXTRACT_DUP_NAV = """
            namespace census.seat12h1x
            version "0.0.0"

            type Item: <"The control's element.">
                v number (1..1)

            func MergeV: <"An item and a number in, the item out.">
                inputs:
                    x Item (1..1)
                    n number (1..1)
                output:
                    z Item (1..1)
                set z: x

            func ExtractDupNav: <"A NON-reduce owner declaring the shared name twice - the fork admits it; the predicate's gate keeps every read raw.">
                inputs:
                    items Item (0..*)
                output:
                    picks Item (0..*)
                add picks: items extract a, a [ MergeV(a, a -> v) ]
            """;
    private static final String REDUCE_DISTINCT_NAV = """
            namespace census.seat12h1vn
            version "0.0.0"

            type Item: <"The M12 carrier's element.">
                v number (1..1)

            func MergeV: <"An item and a number in, the item out.">
                inputs:
                    x Item (1..1)
                    n number (1..1)
                output:
                    z Item (1..1)
                set z: x

            func ReduceDistinctNav: <"Two DISTINCT parameters navigated off - the receivers stay raw.">
                inputs:
                    items Item (0..*)
                output:
                    pick Item (1..1)
                set pick: items reduce a, b [ MergeV(a, b -> v) ]
            """;
    private static final String REDUCE_DISTINCT = """
            namespace census.seat12h1n
            version "0.0.0"

            type Item: <"The control's element.">
                v number (1..1)

            func ReduceDistinct: <"Two DISTINCT closure parameters - the raw names, byte-frozen.">
                inputs:
                    items Item (0..*)
                output:
                    total number (1..1)
                set total: items extract i [ i -> v ] then reduce a, b [ a + b ]
            """;

    private static final String RULE_META_CALL = """
            namespace census.seat12h2
            version "0.0.0"

            type Trade: <"The M8a carrier's root.">
                utid string (1..1)
                venue string (0..1)
                    [metadata scheme]
                venues string (0..*)
                    [metadata scheme]

            func VenueOf: <"A callee returning a scheme-carrying SINGLE value.">
                inputs:
                    t Trade (1..1)
                output:
                    venue string (1..1)
                        [metadata scheme]
                set venue: t -> venue

            func VenuesOf: <"A callee returning a scheme-carrying MULTI value.">
                inputs:
                    t Trade (1..1)
                output:
                    venues string (0..*)
                        [metadata scheme]
                add venues: t -> venues

            func UtidOf: <"The control's callee - a PLAIN string output.">
                inputs:
                    t Trade (1..1)
                output:
                    utid string (1..1)
                set utid: t -> utid

            reporting rule Venue from Trade: <"THE M8a SHAPE - the chaos s28 C28VenueRule: a rule extracting a function call whose output is meta-annotated.">
                extract VenueOf(item)

            reporting rule Venues from Trade: <"The MULTI twin.">
                extract VenuesOf(item)

            reporting rule Utid from Trade: <"THE CONTROL - a plain-output callee keeps the bare form.">
                extract UtidOf(item)
            """;

    private static final String ONLY_EXISTS_ITEM_ROOT = """
            namespace census.seat12h3
            version "0.0.0"

            type Sub: <"The M6 carrier's nested type - every attribute optional.">
                sname string (0..1)
                subs string (0..*)

            type OptA: <"Choice option A.">
                av string (0..1)

            type OptB: <"Choice option B.">
                bv string (0..1)

            choice Pick: <"The choice whose option is an only-exists leaf.">
                OptA
                OptB

            type Paths: <"only-exists targets: a bare, a nested, a choice-typed and a multi attribute.">
                p string (0..1)
                q string (0..1)
                sub Sub (0..1)
                pick Pick (0..1)
                condition ItemRoot: <"THE M6 SHAPE in a data-type condition: an `item ->` root.">
                    item -> p only exists or (item -> p, item -> q) only exists
                condition ItemTwoHop: <"The item root under a two-hop element.">
                    item -> sub -> sname only exists or (item -> sub -> sname, item -> sub -> subs) only exists
                condition ItemOption: <"The item root to a choice-option leaf.">
                    if pick exists then item -> pick -> OptA only exists or item -> pick -> OptB only exists
                condition BareControl: <"THE CONTROL: the bare-symbol form - byte-exact before the seat.">
                    p only exists or (p, q) only exists

            func OnlyItem: <"THE M6 SHAPE at a function seat: the implicit item inside a then-extract lambda.">
                inputs:
                    ps Paths (0..*)
                    t Paths (1..1)
                output:
                    oks boolean (0..*)
                add oks: ps then extract (item -> p only exists)
                add oks: ps then extract (item -> pick -> OptA only exists)
                add oks: ps then extract ((item -> sub -> sname, item -> sub -> subs) only exists)

            func OnlyExplicit: <"THE CONTROL at the function seat: an EXPLICIT lambda parameter.">
                inputs:
                    ps Paths (0..*)
                output:
                    oks boolean (0..*)
                add oks: ps extract x [ x -> p only exists ]
                add oks: ps extract x [ x -> pick -> OptA only exists ]
                add oks: ps extract x [ (x -> sub -> sname, x -> sub -> subs) only exists ]
            """;

    // =========================================================================
    // c1 / n1 — the residual switch ternary
    // =========================================================================

    @Test
    void c1_switchAtOperandSeat_refusedAtSwitchTernaryStub() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(SWITCH_OPERAND, "c1");
            assertOneRefusal(run, SilentDegradation.Site.SWITCH_TERNARY_STUB,
                    "census/seat12c1/functions/Which.java", "switch at a seat with no ladder renderer");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void n1_switchAtSetPosition_stillRendersTheLadder() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(SWITCH_SET, "n1");
            assertTrue(run.errors().isEmpty(), "no refusal at the SET position: " + run.errorMessages());
            String f = file(run.output(), "census/seat12n1/functions/Plain.java");
            assertAbsent(f, "Objects.equals(", "the SET-position switch");
            assertContains(f, "SideEnum.LONG", "the SET-position switch");
            assertEquals(0, SilentDegradation.counts().get(SilentDegradation.Site.SWITCH_TERNARY_STUB));
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // c2 / n2 — the deep-path stub
    // =========================================================================

    @Test
    void c2_deepCallOnDeepReceiver_refusedAtDeepPathUtilUnresolved() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(DEEP_CHAIN, "c2");
            assertOneRefusal(run, SilentDegradation.Site.DEEP_PATH_UTIL_UNRESOLVED,
                    "census/seat12c2/functions/Chained.java", "deep path '->> deep' whose receiver type the resolver cannot name");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void n2_deepCallOnLambdaItem_stillResolvesAndInjects() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(DEEP_ITEM, "n2");
            assertTrue(run.errors().isEmpty(), "no refusal on the lambda-item receiver: " + run.errorMessages());
            String f = file(run.output(), "census/seat12n2/functions/Direct.java");
            assertContains(f, "@Inject protected OuterDeepPathUtil outerDeepPathUtil;", "the seat-6 heal");
            assertContains(f, "outerDeepPathUtil.chooseText(", "the seat-6 heal");
            assertAbsent(f, "TODO(M7b-4)", "the seat-6 heal");
            assertEquals(0, SilentDegradation.counts().get(SilentDegradation.Site.DEEP_PATH_UTIL_UNRESOLVED));
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // c3 / n3 — the alias signature's raw builtin name
    // =========================================================================

    @Test
    void c3_decliningAliasInNumberOutputFunction_refusedAtAliasSignatureRawType() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(ALIAS_BUILTIN, "c3");
            assertOneRefusal(run, SilentDegradation.Site.ALIAS_SIGNATURE_RAW_TYPE,
                    "census/seat12c3/functions/Shadow.java", "raw rune type name 'number'");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    void n3_decliningAliasInModelOutputFunction_keepsTheFallback() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(ALIAS_MODEL, "n3");
            assertTrue(run.errors().isEmpty(), "no refusal on a model-typed output: " + run.errorMessages());
            String f = file(run.output(), "census/seat12n3/functions/ShadowModel.java");
            // the fallback's bytes: the walk declines the reduce-over-counts alias and the signature falls to the
            // output's raw name, which for a model type is its Java simple name - the pre-seat render, unchanged
            assertContains(f, "MapperS<? extends Tally> total(", "the model-typed fallback");
            assertEquals(0, SilentDegradation.counts().get(SilentDegradation.Site.ALIAS_SIGNATURE_RAW_TYPE));
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // h1 / h1n — M12 the duplicate closure parameter, HEALED (D52 H1)
    // =========================================================================

    /**
     * h1: {@code reduce a, a [ a + a ]} renders upstream's numbered pair with every read bound to the FIRST -
     * {@code (a0, a1) -> MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(a0, a0)} (the chaos s95 golden's
     * lambda; the hold-out group {@code closure-param-duplicate} pins the whole file from the released plugin).
     * The pre-seat render was {@code (a, a) -> ...add(a, a)}: a duplicate lambda parameter, non-compiling.
     */
    @Test
    void h1_duplicateReduceParameters_numberedAndBoundToTheFirst() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(REDUCE_DUP, "h1");
            assertTrue(run.errors().isEmpty(), "no refusal on the duplicate pair: " + run.errorMessages());
            String f = file(run.output(), "census/seat12h1/functions/ReduceDup.java");
            assertContains(f, "reduce((a0, a1) -> MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(a0, a0))",
                    "the numbered pair, the body bound to the first");
            assertAbsent(f, "(a, a)", "the duplicate lambda parameter");
            assertAbsent(f, "add(a, a)", "the raw read");
        } finally {
            SilentDegradation.reset();
        }
    }

    /** h1n: two DISTINCT reduce parameters keep the raw names - the predicate numbers a SHARED name alone. */
    @Test
    void h1n_distinctReduceParameters_stayRaw() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(REDUCE_DISTINCT, "h1n");
            assertTrue(run.errors().isEmpty(), "no refusal on the distinct pair: " + run.errorMessages());
            String f = file(run.output(), "census/seat12h1n/functions/ReduceDistinct.java");
            assertContains(f, "reduce((a, b) -> MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(a, b))",
                    "the raw pair");
            assertAbsent(f, "a0", "no numbering of a distinct name");
        } finally {
            SilentDegradation.reset();
        }
    }

    /**
     * h1v (round 1, cq MF-1): a closure-parameter RECEIVER of the duplicated pair renders the FIRST parameter's
     * numbered name — {@code NavigationHandler}'s #342 receiver seat reads {@code reduceParamReadName} like the
     * bare-read arm (the hold-out group {@code closure-param-duplicate-reads}, ReduceDupNavRead: the fork wrote
     * {@code a.<BigDecimal>map(...)} beside the numbered pair before this line - {@code a} unbound, non-compiling).
     */
    @Test
    void h1v_duplicateReduceParameters_navigatedOff_receiverNumbered() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(REDUCE_DUP_NAV, "h1v");
            assertTrue(run.errors().isEmpty(), "no refusal on the navigated duplicate pair: " + run.errorMessages());
            String f = file(run.output(), "census/seat12h1v/functions/ReduceDupNav.java");
            assertContains(f, "reduce((a0, a1) -> MapperS.of(mergeV.evaluate(a0.get(), a0.<BigDecimal>map(\"getV\", item -> item.getV()).get())))",
                    "the numbered pair, the argument AND the receiver bound to the first");
            assertAbsent(f, "a.<BigDecimal>map", "the raw receiver");
            assertAbsent(f, "(a, a)", "the duplicate lambda parameter");
        } finally {
            SilentDegradation.reset();
        }
    }

    /**
     * h1x (round 2, cq SF-1): the predicate's gate is ITS OWN - a shared name is numbered only under a REDUCE owner. A
     * two-name EXTRACT owner (admitted by the fork's front end, refused upstream) keeps every read raw: no {@code a0} /
     * {@code a1} anywhere, the navigated receiver {@code a.}. Lane L12 (the gate removed from the predicate) turns this
     * control RED and nothing else in the suite.
     */
    @Test
    void h1x_nonReduceOwnerWithSharedName_everyReadStaysRaw() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(EXTRACT_DUP_NAV, "h1x");
            assertTrue(run.errors().isEmpty(), "no refusal on the two-name extract owner: " + run.errorMessages());
            String f = file(run.output(), "census/seat12h1x/functions/ExtractDupNav.java");
            assertContains(f, "a.<BigDecimal>map(\"getV\", item -> item.getV())", "the raw receiver under a non-reduce owner");
            assertAbsent(f, "a0", "no numbering under a non-reduce owner");
            assertAbsent(f, "a1", "no numbering under a non-reduce owner");
        } finally {
            SilentDegradation.reset();
        }
    }

    /** h1vn: two DISTINCT parameters navigated off keep the raw receivers - the predicate numbers a SHARED name alone. */
    @Test
    void h1vn_distinctReduceParameters_navigatedOff_receiversStayRaw() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(REDUCE_DISTINCT_NAV, "h1vn");
            assertTrue(run.errors().isEmpty(), "no refusal on the navigated distinct pair: " + run.errorMessages());
            String f = file(run.output(), "census/seat12h1vn/functions/ReduceDistinctNav.java");
            assertContains(f, "reduce((a, b) -> MapperS.of(mergeV.evaluate(a.get(), b.<BigDecimal>map(\"getV\", item -> item.getV()).get())))",
                    "the raw pair and the raw receiver");
            assertAbsent(f, "a0", "no numbering of a distinct name");
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // h2 / h2m / h2n — M8a the meta wrapper into a rule's plain output, HEALED (D52 H2)
    // =========================================================================

    /**
     * h2: a rule extracting a function call whose output is {@code string [metadata scheme]} hoists the
     * {@code FieldWithMetaString} to a type-named local and unwraps {@code .getValue()} under a null guard - the
     * released plugin's form (the hold-out group {@code rule-meta-output-unwrap}'s VenueRule pins the whole file).
     * The pre-seat render assigned the wrapper to the String output: non-compiling, the chaos s28 rows.
     */
    @Test
    void h2_ruleExtractingMetaOutputCall_hoistsAndUnwrapsTheWrapper() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(RULE_META_CALL, "h2");
            assertTrue(run.errors().isEmpty(), "no refusal on the meta-output call: " + run.errorMessages());
            String f = file(run.output(), "census/seat12h2/reports/VenueRule.java");
            assertContains(f, "final FieldWithMetaString fieldWithMetaString = MapperS.of(input)", "the hoisted wrapper");
            assertContains(f, ".mapSingleToItem(item -> MapperS.of(venueOf.evaluate(item.get()))).get();", "the hoisted call");
            assertContains(f, "output = fieldWithMetaString.getValue();", "the unwrap");
            assertContains(f, "import com.rosetta.model.metafields.FieldWithMetaString;", "the wrapper's import");
            assertAbsent(f, "output = MapperS.of(input)", "the pre-seat wrapper-into-String assignment");
        } finally {
            SilentDegradation.reset();
        }
    }

    /** h2m: the MULTI twin - the list of wrappers mapped to values by the "Type coercion" map. */
    @Test
    void h2m_ruleExtractingMetaMultiOutputCall_mapsTheWrappers() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(RULE_META_CALL, "h2m");
            assertTrue(run.errors().isEmpty(), "no refusal on the meta multi-output call: " + run.errorMessages());
            String f = file(run.output(), "census/seat12h2/reports/VenuesRule.java");
            assertContains(f, ".<String>map(\"Type coercion\", fieldWithMetaString -> fieldWithMetaString.getValue()).getMulti();",
                    "the coercion map over the wrappers");
            assertContains(f, "MapperC.<FieldWithMetaString>of(venuesOf.evaluate(item.get()))", "the wrapper-typed list");
        } finally {
            SilentDegradation.reset();
        }
    }

    /** h2n: THE CONTROL - a plain-output callee keeps the bare {@code MapperS.of(...).get()} form, no wrapper. */
    @Test
    void h2n_ruleExtractingPlainOutputCall_keepsTheBareForm() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(RULE_META_CALL, "h2n");
            assertTrue(run.errors().isEmpty(), "no refusal on the plain-output call: " + run.errorMessages());
            String f = file(run.output(), "census/seat12h2/reports/UtidRule.java");
            assertContains(f, "output = MapperS.of(input)", "the bare assignment");
            assertContains(f, ".mapSingleToItem(item -> MapperS.of(utidOf.evaluate(item.get()))).get();", "the bare call");
            assertAbsent(f, "FieldWithMeta", "no wrapper on a plain output");
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // h3 / h3f / h3n — M6 the `item ->` only-exists root, HEALED (D52 H3)
    // =========================================================================

    /**
     * h3: an {@code item ->} only-exists root in a DATA-TYPE condition renders the implicit variable as the shared
     * parent - {@code MapperS.of(paths)} with the declaring type's full attribute list, a hop over it as the
     * navigation ({@code .<Sub>map("getSub", _paths -> _paths.getSub())}), a choice-typed hop with the choice's
     * options as the attribute list - the released plugin's form (the hold-out group {@code only-exists-item-root}
     * pins the whole files). The pre-seat render was the legacy dotted placeholder {@code onlyExists(item,
     * Arrays.asList("item.p"), ...)}: no {@code item} variable in a data rule, non-compiling (the chaos s25 rows).
     */
    @Test
    void h3_itemRootOnlyExists_inACondition_rendersTheImplicitInstanceAsTheParent() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(ONLY_EXISTS_ITEM_ROOT, "h3");
            assertTrue(run.errors().isEmpty(), "no refusal on the item-rooted elements: " + run.errorMessages());
            String root = file(run.output(), "census/seat12h3/validation/datarule/PathsItemRoot.java");
            assertContains(root, "onlyExists(MapperS.of(paths), Arrays.asList(\"p\", \"q\", \"sub\", \"pick\"), Arrays.asList(\"p\"))",
                    "the implicit instance as the parent, the declaring type's attributes");
            assertContains(root, "Arrays.asList(\"p\", \"q\"))", "the list form's selected fields");
            assertAbsent(root, "item.p", "the legacy dotted placeholder");
            assertAbsent(root, "onlyExists(item,", "the undefined `item` variable");
            String twoHop = file(run.output(), "census/seat12h3/validation/datarule/PathsItemTwoHop.java");
            assertContains(twoHop, "onlyExists(MapperS.of(paths).<Sub>map(\"getSub\", _paths -> _paths.getSub()), Arrays.asList(\"sname\", \"subs\"), Arrays.asList(\"sname\"))",
                    "the hop over the implicit instance");
            String option = file(run.output(), "census/seat12h3/validation/datarule/PathsItemOption.java");
            assertContains(option, "onlyExists(MapperS.of(paths).<Pick>map(\"getPick\", _paths -> _paths.getPick()), Arrays.asList(\"OptA\", \"OptB\"), Arrays.asList(\"OptA\"))",
                    "the choice's options as the attribute list");
        } finally {
            SilentDegradation.reset();
        }
    }

    /**
     * h3f: the FUNCTION seat - the implicit item inside a then-extract lambda renders {@code item} as the parent
     * (the nine chaos {@code C25Only} rows: compiling on the default route before the seat and WRONG - the pair
     * gate's one-sided {@code NoSuchMethodException: ...getItem.pick.OptA()}).
     */
    @Test
    void h3f_itemRootOnlyExists_inAThenExtractLambda_rendersTheItemAsTheParent() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(ONLY_EXISTS_ITEM_ROOT, "h3f");
            assertTrue(run.errors().isEmpty(), "no refusal at the function seat: " + run.errorMessages());
            String f = file(run.output(), "census/seat12h3/functions/OnlyItem.java");
            assertContains(f, ".mapItem(item -> onlyExists(item, Arrays.asList(\"p\", \"q\", \"sub\", \"pick\"), Arrays.asList(\"p\")).asMapper()).getMulti());",
                    "the bare item as the parent");
            assertContains(f, ".mapItem(item -> onlyExists(item.<Pick>map(\"getPick\", paths -> paths.getPick()), Arrays.asList(\"OptA\", \"OptB\"), Arrays.asList(\"OptA\")).asMapper()).getMulti());",
                    "the choice hop over the item");
            assertContains(f, ".mapItem(item -> onlyExists(item.<Sub>map(\"getSub\", paths -> paths.getSub()), Arrays.asList(\"sname\", \"subs\"), Arrays.asList(\"sname\", \"subs\")).asMapper()).getMulti());",
                    "the two-hop list form over the item");
            assertAbsent(f, "item.pick", "the legacy dotted placeholder");
        } finally {
            SilentDegradation.reset();
        }
    }

    /** h3n: THE CONTROLS - the bare-symbol condition and the explicit-parameter lambda keep their pre-seat bytes. */
    @Test
    void h3n_bareSymbolAndExplicitParameterForms_keepTheirRender() throws IOException {
        SilentDegradation.reset();
        try {
            HoldOutByteCompareTest.GenerationRun run = render(ONLY_EXISTS_ITEM_ROOT, "h3n");
            assertTrue(run.errors().isEmpty(), "no refusal on the controls: " + run.errorMessages());
            String bare = file(run.output(), "census/seat12h3/validation/datarule/PathsBareControl.java");
            assertContains(bare, "onlyExists(MapperS.of(paths), Arrays.asList(\"p\", \"q\", \"sub\", \"pick\"), Arrays.asList(\"p\"))",
                    "the bare-symbol form (arm 4)");
            String explicit = file(run.output(), "census/seat12h3/functions/OnlyExplicit.java");
            assertContains(explicit, ".mapItem(x -> onlyExists(x, Arrays.asList(\"p\", \"q\", \"sub\", \"pick\"), Arrays.asList(\"p\")).asMapper()).getMulti());",
                    "the explicit parameter as the parent");
            assertContains(explicit, ".mapItem(x -> onlyExists(x.<Pick>map(\"getPick\", paths -> paths.getPick()), Arrays.asList(\"OptA\", \"OptB\"), Arrays.asList(\"OptA\")).asMapper()).getMulti());",
                    "the choice hop over the explicit parameter");
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // r1 — the LOUD line
    // =========================================================================

    @Test
    void r1_loudLineCarriesTheThreeSites_atZeroAfterReset() {
        SilentDegradation.reset();
        String line = SilentDegradation.render();
        for (String site : List.of("SWITCH_TERNARY_STUB", "DEEP_PATH_UTIL_UNRESOLVED", "ALIAS_SIGNATURE_RAW_TYPE")) {
            assertContains(line, " " + site + "=0", "the LOUD line");
        }
        // v3.2 seat 13 (D53): the closing seat adds its sites to the register - the count is pinned by the closing seat suite
        // (ClosingSeatTest.r1) from now on; this test keeps its three-name witness alone.
        assertTrue(SilentDegradation.Site.values().length >= 15, "at least the fifteen register sites of seat 12");
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
        RModel m = AstBuilder.buildFromString(source, "seat12-" + label + ".rosetta");
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
