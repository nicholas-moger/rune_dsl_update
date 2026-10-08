package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * v3.2 seat 8, rounds 1 to 3 — the only-exists diagnostic ANCHORS, pinned to the RELEASED
 * 9.83.0 validator's own columns. The MODEL below is byte-for-byte the fixture the upstream
 * resolution oracle validated with its {@code --issues} dump (v3.2 seat 8 rounds 2 and 3:
 * {@code scripts/xtext-oracle/run-oracle.sh --issues}, every issue of every severity with its
 * line / column / offset / length — {@code target/v32-seat8-instruments/scratch/oracle-anchors/
 * anchor-fixture3.rosetta} and {@code issues3.tsv}, local; the round-2 fixture and dump,
 * {@code anchor-fixture2.rosetta} / {@code issues2.tsv}, are the same text without line 36), so
 * every column asserted here is a PRINT of the released plugin, never a reading of the fork's
 * own code.
 *
 * <p>THE MEASURED LAW — two anchor families: {@code Duplicate attribute}, the unsupported-type
 * error and {@code Object must have a parent object} anchor on the WHOLE element ({@code t ->
 * a -> b}, {@code t -> p}, {@code t}); the single-cardinality WARNING and {@code All parent
 * paths must be equal} anchor on the element's PARENT expression ({@code ts -> a}, {@code ts},
 * {@code t -> r}, {@code t}) — and, measured at round 3, a CHAINLESS offender in a mixed list
 * ({@code (t -> p, t) only exists}) anchors {@code All parent paths must be equal} on itself,
 * exactly where the fork's parent read falls back to the element. One-hop and two-hop elements
 * alike.
 *
 * <p>THE HISTORY this pin closes: the pre-seat fork read the synthesized receiver's range for
 * every one of the five (the whole element on a two-or-more-feature path — the receiver hop was
 * ranged over the element — and the bare root on a one-feature path), so it matched the parent
 * family on one-hop elements and the element family on multi-hop ones, never both; the seat's
 * per-hop ranging (commit 4) moved the receiver to the parent path, which matched the PARENT
 * family everywhere and left the element family one feature short on multi-hop elements (round
 * 1's catch); round 1 read the whole element for all five, which matched the ELEMENT family
 * everywhere and broke the parent family (round 2's catch — the same class, inverted, with a
 * "same bytes" claim that was false on one-hop elements). Round 2 measured instead of arguing:
 * {@code ExpressionValidator.rangeOf} (the element) and {@code parentRangeOf} (the parent) carry
 * the two families, and since round 3 every parent-family site CONSULTS {@code parentRangeOf}
 * (LAW 69); these ten pins are upstream's columns.
 *
 * <p>THE LANES (lanes K1 and K2 of {@code target/v32-seat8-instruments/lanes-s8.py}, LAW 82 —
 * the measured sets are the pinned per-run status files', never the rolling one): K1,
 * {@code rangeOf} reverted to the receiver's range → EXACTLY the four ELEMENT-family pins on
 * the pathed elements ({@link #duplicateAttribute_twoHop}, {@link #unsupportedType_twoHop},
 * {@link #duplicateAttribute_oneHop}, {@link #unsupportedType_oneHop}) red, measured at the
 * round-2 code head {@code 22f71e1c8} ({@code scratch/lanes-s8-c12.status}; 37 run / 4 F); K2,
 * {@code parentRangeOf} reduced to the element read (round 1's behaviour) → the four
 * PARENT-family pins ({@link #singleCardinalityWarning_twoHop_anchorsOnTheParent},
 * {@link #singleCardinalityWarning_oneHop_anchorsOnTheParent},
 * {@link #parentPathsMustBeEqual_oneHopOffender_anchorsOnItsParent},
 * {@link #parentPathsMustBeEqual_twoHopOffender_anchorsOnItsParent}) — measured at the round-3
 * code head {@code a3f392517} ({@code scratch/lanes-s8-c14.status}; 38 run / 4 F): EXACTLY the
 * prediction, K1's four UNMOVED, every other lane's set UNMOVED, I1 green and withdrawn; the
 * chainless pins stay green under both (their anchor IS the element under either read).
 *
 * <p>Out of this pin's reach, banked: on {@code t -> m -> s -> scheme only exists} the released
 * plugin reports {@code Couldn't resolve reference to RosettaFeature 'scheme'} at the feature
 * token and the unsupported-type error on the whole element; the fork's meta-feature error
 * ({@code Invalid use of `only exists` on meta feature}) is a port of a message this fixture
 * does not reach upstream, its anchor stays the element, UNMEASURED; and the implicit-context
 * cardinality ERROR ({@code Expecting single cardinality input}) has no fixture case.
 */
class OnlyExistsDiagnosticAnchorTest {

    /** Byte-identical to {@code scratch/oracle-anchors/anchor-fixture3.rosetta} (the oracle's input). */
    private static final String MODEL = """
            namespace test
            type A:
                b string (0..1)
                c string (0..1)
            type R:
                d string (1..1)
            type U:
                g string (1..1)
                f string (0..1)
            type M:
                s string (0..1)
                    [metadata scheme]
            type T:
                a A (0..1)
                r R (0..1)
                p string (0..1)
                q string (0..1)
                m M (0..1)
            func F:
                inputs:
                    t T (1..1)
                    ts T (0..*)
                    u U (1..1)
                output:
                    ok boolean (0..*)
                add ok: (t -> a -> b, t -> a -> b) only exists
                add ok: ts -> a -> b only exists
                add ok: t -> r -> d only exists
                add ok: (t -> p, t -> p) only exists
                add ok: ts -> p only exists
                add ok: u -> f only exists
                add ok: (t -> a -> b, t -> q) only exists
                add ok: (t -> a -> b, t -> r -> d) only exists
                add ok: t -> m -> s -> scheme only exists
                add ok: t -> m -> scheme only exists
                add ok: (t -> p, t) only exists
            func G:
                inputs:
                    t T (1..1)
                output:
                    ok boolean (0..1)
                set ok: t only exists
            """;

    private static final String CARD = "Expecting single cardinality. The `only exists` operator requires a single cardinality input";
    private static final String DUP = "Duplicate attribute";
    private static final String PARENTS = "All parent paths must be equal";
    private static final String PARENT_OBJECT = "Object must have a parent object";

    private static List<ValidationDiagnostic> validate() {
        var model = AstBuilder.buildFromString(MODEL, "anchor-fixture3.rosetta");
        return RWorkspace.build(List.of(model)).workspace().validationDiagnostics();
    }

    /** The one diagnostic of that severity and message STARTING on {@code line} (a message can recur on other lines). */
    private static ValidationDiagnostic soleAt(int line, Severity severity, String message) {
        var matches = validate().stream()
                .filter(d -> d.severity() == severity && d.message().equals(message) && d.range().startLine() == line)
                .toList();
        assertEquals(1, matches.size(),
                "expected exactly one " + severity + " '" + message + "' on line " + line + ", got: " + matches);
        return matches.get(0);
    }

    /** Upstream's anchor as the oracle prints it: 1-based column and a length, on one line. */
    private static void assertUpstreamAnchor(ValidationDiagnostic d, int line, int column, int length, String why) {
        assertEquals(line, d.range().startLine(), why + " — start line");
        assertEquals(column, d.range().startCol(), why + " — start column (upstream's column)");
        assertEquals(line, d.range().endLine(), why + " — end line");
        assertEquals(column + length - 1, d.range().endCol(), why + " — END column (upstream's column + length - 1)");
    }

    // ---- the ELEMENT family: the whole element ---------------------------------------------------

    @Test
    void duplicateAttribute_twoHop() {
        // issues3.tsv: 26 27 428 11 ERROR Duplicate attribute — the whole second element `t -> a -> b`
        assertUpstreamAnchor(soleAt(26, Severity.ERROR, DUP), 26, 27, 11, "Duplicate attribute, two-hop");
    }

    @Test
    void duplicateAttribute_oneHop() {
        // issues3.tsv: 29 22 547 6 — the whole second element `t -> p` (the pre-seat fork anchored the root `t`)
        assertUpstreamAnchor(soleAt(29, Severity.ERROR, DUP), 29, 22, 6, "Duplicate attribute, one-hop");
    }

    @Test
    void unsupportedType_twoHop() {
        // issues3.tsv: 28 13 502 11 — the whole element `t -> r -> d`
        assertUpstreamAnchor(soleAt(28, Severity.ERROR,
                "Operator `only exists` is not supported for type `R`. All attributes of input type should be optional"),
                28, 13, 11, "the unsupported-type error, two-hop");
    }

    @Test
    void unsupportedType_oneHop() {
        // issues3.tsv: 31 13 611 6 — the whole element `u -> f` (the pre-seat fork anchored the root `u`)
        assertUpstreamAnchor(soleAt(31, Severity.ERROR,
                "Operator `only exists` is not supported for type `U`. All attributes of input type should be optional"),
                31, 13, 6, "the unsupported-type error, one-hop");
    }

    @Test
    void parentObject_chainless() {
        // issues3.tsv: 42 13 939 1 — the chainless element `t` itself
        assertUpstreamAnchor(soleAt(42, Severity.ERROR, PARENT_OBJECT), 42, 13, 1, "Object must have a parent object");
    }

    // ---- the PARENT family: the element's parent expression ------------------------------------

    @Test
    void singleCardinalityWarning_twoHop_anchorsOnTheParent() {
        // issues3.tsv: 27 13 465 7 WARNING — the PARENT `ts -> a`, not the element `ts -> a -> b` (round 1 had widened it)
        assertUpstreamAnchor(soleAt(27, Severity.WARNING, CARD), 27, 13, 7, "the single-cardinality warning, two-hop");
    }

    @Test
    void singleCardinalityWarning_oneHop_anchorsOnTheParent() {
        // issues3.tsv: 30 13 579 2 WARNING — the PARENT `ts` (the root token)
        assertUpstreamAnchor(soleAt(30, Severity.WARNING, CARD), 30, 13, 2, "the single-cardinality warning, one-hop");
    }

    @Test
    void parentPathsMustBeEqual_oneHopOffender_anchorsOnItsParent() {
        // issues3.tsv: 32 27 656 1 — the offending element's PARENT `t` (of `t -> q`)
        assertUpstreamAnchor(soleAt(32, Severity.ERROR, PARENTS), 32, 27, 1, "All parent paths must be equal, one-hop offender");
    }

    @Test
    void parentPathsMustBeEqual_twoHopOffender_anchorsOnItsParent() {
        // issues3.tsv: 33 27 702 6 — the offending element's PARENT `t -> r` (of `t -> r -> d`)
        assertUpstreamAnchor(soleAt(33, Severity.ERROR, PARENTS), 33, 27, 6, "All parent paths must be equal, two-hop offender");
    }

    // ---- the fallback leg, measured at round 3: a chainless offender has no parent path ----------

    @Test
    void parentPathsMustBeEqual_chainlessOffender_anchorsOnItself() {
        // issues3.tsv: 36 22 835 1 — the chainless offender `t` of `(t -> p, t)`: no parent expression, so upstream
        // anchors the element itself — exactly where parentRangeOf falls back to rangeOf (the round-3 cq NIT-1 leg)
        assertUpstreamAnchor(soleAt(36, Severity.ERROR, PARENTS), 36, 22, 1, "All parent paths must be equal, chainless offender");
    }
}
