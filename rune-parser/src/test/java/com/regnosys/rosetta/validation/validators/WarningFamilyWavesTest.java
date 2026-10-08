package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Witness locks for the PR #455 warning-family waves (facet
 * warningFamilyWaves): the remaining upstream warning classes land at the
 * RELEASED-9.83.0 severities and message bytes (the #454 severity oracle —
 * the released jar's bytecode and constant pool, not the vendored tree).
 * Every fixture reproduces a V0-bank witness topology; every assert pins the
 * released bytes exactly. The corpus-wide proof is the census's per-site
 * (file, message) diff against both V0 banks — oracle-clean at the #455
 * close (cdm 801/801 + drr 3,783/3,786, the 3 then-recorded iosco
 * under-fires aside; the tests/ fixture's codeImplementation line is the
 * #454 population-artifact class). The #457 LoL item wave
 * ({@code LoLItemCardinalityWaveTest}) healed those 3 under-fires: both
 * streams now match completely — drr 3,786/3,786.
 */
class WarningFamilyWavesTest {

    private static List<ValidationDiagnostic> validate(String source) {
        var model = AstBuilder.buildFromString(source, "warning-waves-test.rosetta");
        return RWorkspace.build(List.of(model)).workspace().validationDiagnostics();
    }

    private static long count(List<ValidationDiagnostic> diags, Severity sev, String message) {
        return diags.stream()
                .filter(d -> d.severity() == sev && d.message().equals(message))
                .count();
    }

    // === path-operator-on-choice deprecation (784 cdm bank lines) ===========

    @Test void path_operator_on_choice_warns_per_step() {
        // The Asset->Instrument->Security chain shape (cdm event-common-func
        // :115): the EVR-disguised inner step (asset -> Instrument, a
        // choice-typed head) AND the outer feature call (receiver typed as
        // the Instrument choice) each fire once — upstream anchors both at
        // the receiver, the bank's duplicate-position rows.
        var diags = validate("""
                namespace test
                choice Asset:
                    Instrument
                    Cash
                choice Instrument:
                    Security
                    Loan
                type Security:
                    h int (1..1)
                type Loan:
                    l int (1..1)
                type Cash:
                    c int (1..1)
                type Transfer:
                    asset Asset (0..1)
                func G:
                    inputs:
                        transfers Transfer (0..*)
                    output:
                        out Transfer (0..*)
                    add out:
                        transfers filter asset -> Instrument -> Security exists
                """);
        assertEquals(2, count(diags, Severity.WARNING,
                "Using the path operator on a choice type is deprecated. Use the switch operator instead"));
    }

    @Test void path_operator_in_data_condition_warns() {
        // The product-template condition shape (the cdm bank's dominant
        // type-file block): a condition's disguised nav over a choice-typed
        // attribute of the enclosing type.
        var diags = validate("""
                namespace test
                choice Payout:
                    InterestRatePayout
                    AssetPayout
                type InterestRatePayout:
                    x int (1..1)
                type AssetPayout:
                    y int (1..1)
                type Product:
                    payout Payout (0..*)
                    terminationDate int (0..1)
                    condition FpML_cd:
                        if payout -> InterestRatePayout exists
                        then terminationDate exists
                """);
        assertEquals(1, count(diags, Severity.WARNING,
                "Using the path operator on a choice type is deprecated. Use the switch operator instead"));
    }

    @Test void extends_choice_view_head_stays_silent_on_path_operator() {
        // A `type X extends <choice>` head is a DATA type upstream — its
        // option nav is NOT a choice-typed receiver, so the deprecation
        // stays silent (the drr BasketConstituent class; the drr bank
        // carries ZERO path-operator lines). The extends itself carries the
        // extend-choice deprecation instead.
        var diags = validate("""
                namespace test
                choice Observable:
                    Index
                    Security
                type Index:
                    i int (1..1)
                type Security:
                    s int (1..1)
                type BasketConstituent extends Observable:
                type Wrapper:
                    bc BasketConstituent (0..1)
                    condition C:
                        if bc -> Index exists
                        then bc exists
                """);
        assertEquals(0, count(diags, Severity.WARNING,
                "Using the path operator on a choice type is deprecated. Use the switch operator instead"));
    }

    // === extending-a-choice deprecation (3 cdm bank lines) ==================

    @Test void extending_a_choice_warns() {
        var diags = validate("""
                namespace test
                choice Obs:
                    A
                    B
                type A:
                    a int (1..1)
                type B:
                    b int (1..1)
                type FromChoice extends Obs:
                """);
        assertEquals(1, count(diags, Severity.WARNING,
                "Extending a choice type is deprecated"));
    }

    // === regulatoryReference deprecation (3,749 drr bank lines) =============

    @Test void regulatory_reference_keyword_warns_docReference_silent() {
        var diags = validate("""
                namespace test
                body Authority CFTC
                corpus Regulations "CFTC 17 CFR Parts 45" Rewrite
                type T:
                    a int (1..1)
                        [regulatoryReference CFTC Rewrite provision "x"]
                    b int (1..1)
                        [docReference CFTC Rewrite provision "y"]
                """);
        assertEquals(1, count(diags, Severity.WARNING,
                "Using `regulatoryReference` is deprecated. Use `docReference` instead"));
    }

    // === codeImplementation (8 cdm bank lines) ==============================

    @Test void bodyless_function_without_codeImplementation_warns() {
        var diags = validate("""
                namespace test
                func MapShell:
                    inputs:
                        x int (0..1)
                    output:
                        y int (0..1)
                """);
        assertEquals(1, count(diags, Severity.WARNING,
                "A function should specify an implementation, or they should be annotated with codeImplementation"));
    }

    @Test void codeImplementation_annotated_shell_is_silent_and_inverse_warns() {
        var diags = validate("""
                namespace test
                annotation codeImplementation: <"impl provided externally">
                func Shell:
                    [codeImplementation]
                    inputs:
                        x int (0..1)
                    output:
                        y int (0..1)
                func WithBody:
                    [codeImplementation]
                    inputs:
                        x int (0..1)
                    output:
                        y int (0..1)
                    set y: x
                """);
        assertEquals(0, count(diags, Severity.WARNING,
                "A function should specify an implementation, or they should be annotated with codeImplementation"));
        assertEquals(1, count(diags, Severity.WARNING,
                "Functions annotated with codeImplementation should not have any setter operations as they will be overriden"));
    }

    // === missing dispatch implementation (3+3 bank lines) ===================

    @Test void dispatch_missing_enum_values_warn_sorted() {
        var diags = validate("""
                namespace test
                annotation codeImplementation: <"impl provided externally">
                enum DCF:
                    ACT_360
                    ACT_365
                    BUS_252
                func Basis:
                    [codeImplementation]
                    inputs:
                        dcf DCF (1..1)
                    output:
                        basis int (1..1)
                func Basis(dcf: DCF -> ACT_360):
                    set basis: 360
                """);
        assertEquals(1, count(diags, Severity.WARNING,
                "Missing implementation for DCF: ACT_365, BUS_252"));
    }

    @Test void complete_dispatch_family_is_silent() {
        var diags = validate("""
                namespace test
                annotation codeImplementation: <"impl provided externally">
                enum DCF:
                    ACT_360
                func Basis:
                    [codeImplementation]
                    inputs:
                        dcf DCF (1..1)
                    output:
                        basis int (1..1)
                func Basis(dcf: DCF -> ACT_360):
                    set basis: 360
                """);
        assertEquals(0, diags.stream()
                .filter(d -> d.message().startsWith("Missing implementation for")).count());
    }

    // === rule-reference cardinality (14 drr bank lines) =====================

    @Test void single_attribute_with_multi_rule_warns_released_severity() {
        var diags = validate("""
                namespace test
                type Trade:
                    ids string (0..*)
                type Report:
                    id string (0..1)
                        [ruleReference IdRule]
                reporting rule IdRule from Trade:
                    extract ids
                """);
        assertEquals(1, count(diags, Severity.WARNING,
                "Expected single cardinality, but rule has multi cardinality"));
    }

    @Test void single_rule_on_single_attribute_is_silent() {
        var diags = validate("""
                namespace test
                type Trade:
                    ids string (0..1)
                type Report:
                    id string (0..1)
                        [ruleReference IdRule]
                reporting rule IdRule from Trade:
                    extract ids
                """);
        assertEquals(0, count(diags, Severity.WARNING,
                "Expected single cardinality, but rule has multi cardinality"));
    }

    // === the widened ListOperation single-receiver warning (2 drr lines) ====

    @Test void last_on_single_receiver_warns_released_bytes() {
        var diags = validate("""
                namespace test
                func F:
                    inputs:
                        x int (0..1)
                    output:
                        y int (0..1)
                    set y: x last
                """);
        assertEquals(1, count(diags, Severity.WARNING,
                "List last operation cannot be used for single cardinality expressions."));
    }

    @Test void last_on_multi_receiver_is_silent() {
        var diags = validate("""
                namespace test
                func F:
                    inputs:
                        x int (0..*)
                    output:
                        y int (0..1)
                    set y: x last
                """);
        assertEquals(0, diags.stream()
                .filter(d -> d.message().startsWith("List last operation")).count());
    }

    // === constructor-pair cardinality (1 cdm bank line) =====================

    @Test void ctor_pair_list_to_single_warns_released_bytes() {
        var diags = validate("""
                namespace test
                type Inner:
                    v int (0..1)
                func F:
                    inputs:
                        xs int (0..*)
                    output:
                        out Inner (0..1)
                    set out:
                        Inner {
                            v: xs
                        }
                """);
        assertEquals(1, count(diags, Severity.WARNING,
                "Expecting single cardinality. Cannot assign a list to a single value"));
    }

    // === the operation-assignment sibling (corpus-silent seat) ==============

    @Test void set_list_to_single_output_warns_released_bytes() {
        var diags = validate("""
                namespace test
                func F:
                    inputs:
                        xs int (0..*)
                    output:
                        y int (0..1)
                    set y: xs
                """);
        assertEquals(1, count(diags, Severity.WARNING,
                "Expecting single cardinality. Cannot assign a list to a single value"));
    }
}
