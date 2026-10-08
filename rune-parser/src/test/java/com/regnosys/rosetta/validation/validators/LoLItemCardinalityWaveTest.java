package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Witness locks for the PR #457 list-of-lists item-cardinality wave (facet
 * ioscoLoLItemCardinality): upstream does not model the {@code then extract}
 * sugar as implicitly flattening — an {@code extract} whose per-element body
 * is itself a list, applied over a multi stream, produces a NESTED list, and
 * a downstream {@code extract}'s item over that input is a SUB-LIST (multi).
 * {@code CardinalityComputer.outputIsListOfLists} mirrors upstream
 * {@code CardinalityProvider.safeIsOutputListOfLists} arm for arm (vendored
 * :218-254), and {@code implicitItemCardinalityFrom}'s non-then arm now
 * consults it exactly as upstream's {@code safeIsClosureParameterMulti} does
 * (:194-203).
 *
 * <p>The corpus carrier: the iosco cde-v1 basket rules'
 * {@code then extract <fn>(<bare single attr>)} call arguments over the
 * {@code portfolioBasketConstituent -> quantity -> unit} pipe — the V0 drr
 * bank's three bare {@code Expecting single cardinality} lines
 * (standards-iosco-cde-version1-basket-rule :59/:61/:63), the LAST recorded
 * diagnostic divergence on valid models. The corpus-wide proof is the
 * census's per-site (file, message) diff against both V0 banks — with this
 * wave the streams match COMPLETELY: cdm 801/801 and drr 3,786/3,786, zero
 * divergence on valid models in either direction.
 *
 * <p>Every fire-positive lock here fails against the pre-#457 computer (the
 * blanket-SINGLE non-then arm read every call arg single — no warning); the
 * silent locks pair them per the decline-lock witness-uniqueness law.
 */
class LoLItemCardinalityWaveTest {

    private static List<ValidationDiagnostic> validate(String source) {
        var model = AstBuilder.buildFromString(source, "lol-item-wave-test.rosetta");
        return RWorkspace.build(List.of(model)).workspace().validationDiagnostics();
    }

    private static long bareSingleWarnings(List<ValidationDiagnostic> diags) {
        return diags.stream()
                .filter(d -> d.severity() == Severity.WARNING
                        && d.message().equals("Expecting single cardinality"))
                .count();
    }

    /** The shared fixture prefix: Root -> payouts (0..*) -> baskets (0..*) -> unit (1..1). */
    private static final String TYPES = """
            namespace test
            type Root:
                payouts Payout (0..*)
            type Payout:
                baskets Basket (0..*)
                note string (0..1)
            type Basket:
                unit Unit (1..1)
            type Unit:
                capacityUnit string (0..1)
                weatherUnit string (0..1)
                financialUnit string (0..1)
            func Conv:
                inputs:
                    u string (1..1)
                output:
                    out string (1..1)
                set out: u
            """;

    // === the fire arm: a LoL pipe makes the next extract's item a sub-list ==

    @Test void lol_pipe_call_arg_fires_bare_single_warning() {
        // `then extract baskets -> unit` = multi body over a multi stream — a
        // list of lists; the next extract's item is a SUB-LIST, so the bare
        // single-declared `capacityUnit` arg reads MULTI into Conv's (1..1)
        // parameter — the released artifact's bare warning form, exactly the
        // iosco basket-rule mechanism.
        var diags = validate(TYPES + """
                reporting rule W from Root:
                    extract payouts
                    then extract baskets -> unit
                    then extract Conv(capacityUnit)
                """);
        assertEquals(1, bareSingleWarnings(diags));
    }

    @Test void full_iosco_conditional_shape_fires_per_argument() {
        // The three-branch conditional shape of BasketConstituentUnitOfMeasure
        // (:57-63 — capacity/weather/financial, one fire per call argument;
        // the corpus carrier calls three distinct converter functions, this
        // fixture the one Conv — the argument seat is the mechanism).
        var diags = validate(TYPES + """
                reporting rule W from Root:
                    extract payouts
                    then extract baskets -> unit
                    then extract
                        if capacityUnit exists
                        then Conv(capacityUnit)
                        else if weatherUnit exists
                        then Conv(weatherUnit)
                        else if financialUnit exists
                        then Conv(financialUnit)
                """);
        assertEquals(3, bareSingleWarnings(diags));
    }

    // === the paired negative: a FLAT multi pipe stays single-item ===========

    @Test void flat_multi_pipe_stays_silent() {
        // `extract payouts` is a plain (flat) multi stream — the next
        // extract's item is ONE Payout, its single-declared feature is a
        // single call arg: no warning (the pre-#457 behavior, preserved).
        var diags = validate(TYPES + """
                reporting rule W from Root:
                    extract payouts
                    then extract Conv(note)
                """);
        assertEquals(0, bareSingleWarnings(diags));
    }

    // === the Flatten arm: un-nesting kills the fire =========================

    @Test void flatten_un_nests_and_silences() {
        // `then flatten` between the nesting step and the consumer restores a
        // flat list (upstream's FlattenOperation arm, vendored :219-220) —
        // the item is ONE Unit again. The corpus's own sibling
        // (BasketConstituentNumberOfUnits :80) ends `then flatten` for
        // exactly this reason and is bank-silent.
        var diags = validate(TYPES + """
                reporting rule W from Root:
                    extract payouts
                    then extract baskets -> unit
                    then flatten
                    then extract Conv(capacityUnit)
                """);
        assertEquals(0, bareSingleWarnings(diags));
    }

    // === the residual CanHandleListOfLists arm: filter preserves nesting ====

    @Test void filter_passes_nesting_through() {
        // FilterOperation is the ONLY CanHandleListOfLists implementor not
        // short-circuited by an earlier upstream arm (:249-252) — a filter
        // between the nesting step and the consumer narrows the OUTER list
        // and keeps the nesting, so the fire survives.
        var diags = validate(TYPES + """
                reporting rule W from Root:
                    extract payouts
                    then extract baskets -> unit
                    then filter item exists
                    then extract Conv(capacityUnit)
                """);
        assertEquals(1, bareSingleWarnings(diags));
    }

    // === the closure-param arm: a named then-param IS the piped value =======

    @Test void named_then_param_receiver_carries_nesting() {
        // `then p [ p extract ... ]` — the inner extract's receiver is a
        // symbol reference to the THEN's closure parameter (the fork binds it
        // to the declaring inline function), which upstream reads as the
        // whole piped value (vendored :236-245): nested input, nested item —
        // the fire survives the explicit-receiver spelling.
        var diags = validate(TYPES + """
                reporting rule W from Root:
                    extract payouts
                    then extract baskets -> unit
                    then p [ p extract Conv(capacityUnit) ]
                """);
        assertEquals(1, bareSingleWarnings(diags));
    }

    // === the single-body guard: a single body over a multi stream is flat ===

    @Test void single_body_extract_never_nests() {
        // A SINGLE per-element body (`note`) over the multi stream maps to a
        // flat list (upstream :228: body-multi is required for nesting) —
        // the next extract's item is one element and the bare-item arg is
        // silent.
        var diags = validate(TYPES + """
                reporting rule W from Root:
                    extract payouts
                    then extract note
                    then extract Conv(item)
                """);
        assertEquals(0, bareSingleWarnings(diags));
    }
}
