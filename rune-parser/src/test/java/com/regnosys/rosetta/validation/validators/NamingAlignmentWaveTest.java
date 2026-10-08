package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Witness locks for the PR #456 naming-surface alignment: the fork's naming
 * checks re-scoped to the upstream RELEASED-9.83.0 populations and message
 * bytes (six arms across five upstream validators — TypeValidator /
 * EnumValidator / FunctionValidator / ConditionValidator /
 * AttributeValidator — every one a {@code warning} in the released bytecode,
 * disassembly-verified; on this family the vendored tree agrees with the
 * released artifact).
 *
 * <p>The alignment's corpus effect: the fork's former type-alias arm fired
 * 48 rows the V0 banks never carry (the builtins' {@code typeAlias
 * int/productType/eventType/calculation} = the cdm census's 4 naming rows,
 * plus iso20022's 40 lowercase dtcc-rds aliases in the drr closure) — all
 * dead here, closing the LAST non-matched warning class. The corpus-wide
 * proof is the census's per-site (file, message) diff against both V0 banks.
 */
class NamingAlignmentWaveTest {

    private static final String OLD_FORK_UPPER_FRAGMENT = "should start with an uppercase letter";
    private static final String TYPE_MSG = "Type name should start with a capital";
    private static final String ENUM_MSG = "Enumeration name should start with a capital";
    private static final String FUNC_MSG = "Function name should start with a capital";
    private static final String COND_UNNAMED_MSG = "Condition name should be specified";
    private static final String COND_CASE_MSG = "Condition name should start with a capital";
    private static final String ATTR_MSG = "Attribute name should start with a lower case";

    private static List<ValidationDiagnostic> validate(String source) {
        var model = AstBuilder.buildFromString(source, "naming-alignment-test.rosetta");
        return RWorkspace.build(List.of(model)).workspace().validationDiagnostics();
    }

    private static long count(List<ValidationDiagnostic> diags, Severity sev, String message) {
        return diags.stream()
                .filter(d -> d.severity() == sev && d.message().equals(message))
                .count();
    }

    // === The over-fire kill: type-alias names are NEVER checked ==============

    @Test void type_alias_lowercase_name_is_silent() {
        // Upstream RosettaTypeAlias is not a Data — TypeValidator's
        // population excludes it. The iso20022 dtcc-rds shape.
        var diags = validate("""
                namespace test
                typeAlias freeForm255:
                    string
                """);
        assertEquals(0, count(diags, Severity.WARNING, TYPE_MSG));
        assertEquals(0, diags.stream()
                .filter(d -> d.message().contains(OLD_FORK_UPPER_FRAGMENT))
                .count(),
                "the fork's pre-alignment type-alias arm must be retired");
    }

    @Test void data_type_lowercase_fires_the_released_type_bytes() {
        // The positive control proving the alias silence is the population
        // gate, not a dead check: the SAME lowercase name as a `type` fires.
        var diags = validate("""
                namespace test
                type freeForm255:
                    x int (1..1)
                """);
        assertEquals(1, count(diags, Severity.WARNING, TYPE_MSG));
        assertEquals(0, diags.stream()
                .filter(d -> d.message().contains(OLD_FORK_UPPER_FRAGMENT))
                .count());
    }

    @Test void builtin_alias_names_are_silent_in_workspace() {
        // The cdm census's exact 4: typeAlias int / productType / eventType /
        // calculation in the builtins resource — parsed as type aliases, no
        // naming row. Reproduced with the builtins' own shapes.
        var diags = validate("""
                namespace test
                typeAlias productType:
                    string
                typeAlias eventType:
                    string
                typeAlias calculation:
                    string
                """);
        assertEquals(0, count(diags, Severity.WARNING, TYPE_MSG));
        assertEquals(0, diags.stream()
                .filter(d -> d.message().contains(OLD_FORK_UPPER_FRAGMENT))
                .count());
    }

    // === The released bytes per family =======================================

    @Test void choice_lowercase_fires_the_type_bytes() {
        // Choice extends Data upstream (RosettaSimple.xcore:112) — the Data
        // arm's message, not a choice-specific one.
        var diags = validate("""
                namespace test
                type Security:
                    s int (1..1)
                type Cash:
                    c int (1..1)
                choice asset:
                    Security
                    Cash
                """);
        assertEquals(1, count(diags, Severity.WARNING, TYPE_MSG));
    }

    @Test void enum_lowercase_fires_the_released_enum_bytes() {
        var diags = validate("""
                namespace test
                enum color:
                    RED
                    GREEN
                """);
        assertEquals(1, count(diags, Severity.WARNING, ENUM_MSG));
    }

    @Test void function_lowercase_fires_the_released_function_bytes() {
        var diags = validate("""
                namespace test
                func doThing:
                    output:
                        result int (1..1)
                    set result: 1
                """);
        assertEquals(1, count(diags, Severity.WARNING, FUNC_MSG));
    }

    // === Condition names =====================================================

    @Test void unnamed_non_constraint_condition_warns_invalid_name() {
        var diags = validate("""
                namespace test
                type Foo:
                    a int (0..1)
                    condition:
                        a exists
                """);
        assertEquals(1, count(diags, Severity.WARNING, COND_UNNAMED_MSG));
        assertEquals(1, diags.stream()
                .filter(d -> d.message().equals(COND_UNNAMED_MSG)
                        && d.issueCode() == ValidationIssueCode.INVALID_NAME)
                .count());
    }

    @Test void unnamed_constraint_conditions_are_exempt() {
        // isConstraintCondition = one-of || choice — the corpus's 54 unnamed
        // conditions are ALL constraints (why the V0 banks carry zero
        // INVALID_NAME rows).
        var diags = validate("""
                namespace test
                type Foo:
                    a int (0..1)
                    b int (0..1)
                    condition:
                        one-of
                type Bar:
                    x int (0..1)
                    y int (0..1)
                    condition:
                        required choice x, y
                type Baz:
                    p int (0..1)
                    q int (0..1)
                    condition:
                        optional choice p, q
                """);
        assertEquals(0, count(diags, Severity.WARNING, COND_UNNAMED_MSG));
    }

    @Test void lowercase_condition_name_warns_invalid_case() {
        var diags = validate("""
                namespace test
                type Foo:
                    a int (0..1)
                    condition myCondition:
                        a exists
                """);
        assertEquals(1, count(diags, Severity.WARNING, COND_CASE_MSG));
    }

    @Test void function_and_post_condition_names_are_checked() {
        var diags = validate("""
                namespace test
                func Calc:
                    inputs:
                        a int (1..1)
                    output:
                        result int (1..1)
                    condition lowIn:
                        a exists
                    set result: a
                    post-condition lowOut:
                        result exists
                """);
        assertEquals(2, count(diags, Severity.WARNING, COND_CASE_MSG));
    }

    // === Attribute names =====================================================

    @Test void uppercase_data_attribute_fires_the_released_attribute_bytes() {
        var diags = validate("""
                namespace test
                type Foo:
                    Price int (1..1)
                """);
        assertEquals(1, count(diags, Severity.WARNING, ATTR_MSG));
    }

    @Test void function_inputs_and_output_are_attribute_checked() {
        // Function inputs/output are Attributes upstream
        // (RosettaSimple.xcore:162-163) — the widened fork population.
        var diags = validate("""
                namespace test
                func Calc:
                    inputs:
                        Input int (1..1)
                    output:
                        Result int (1..1)
                    set Result: Input
                """);
        assertEquals(2, count(diags, Severity.WARNING, ATTR_MSG));
    }

    @Test void choice_options_are_excluded_from_the_attribute_check() {
        // ChoiceOption extends Attribute upstream but is explicitly excluded
        // — options are type-named by design.
        var diags = validate("""
                namespace test
                type Security:
                    s int (1..1)
                type Cash:
                    c int (1..1)
                choice Asset:
                    Security
                    Cash
                """);
        assertEquals(0, count(diags, Severity.WARNING, ATTR_MSG));
    }

    @Test void annotation_declaration_attributes_are_excluded() {
        // The eContainer-instanceof-Annotation gate: the cdm `annotation
        // creation:` block's uppercase BusinessEvent/WorkflowStep rows are
        // silent upstream.
        var diags = validate("""
                namespace test
                type BusinessEvent:
                    b int (1..1)
                annotation creation: <"Marks a creation function.">
                    BusinessEvent BusinessEvent (0..1)
                """);
        assertEquals(0, count(diags, Severity.WARNING, ATTR_MSG));
    }

    @Test void suppress_warnings_capitalisation_silences_attribute_rows() {
        // The drr iosco AnnaDsb contract: [suppressWarnings capitalisation]
        // at type level over uppercase attributes — the container-suppression
        // read (upstream ORs eContainer()).
        var diags = validate("""
                namespace test
                annotation suppressWarnings:
                    capitalisation boolean (0..1)
                type AnnaDsbHeader:
                    [suppressWarnings capitalisation]
                    InstrumentType int (1..1)
                    UseCase int (1..1)
                """);
        assertEquals(0, count(diags, Severity.WARNING, ATTR_MSG));
    }

    @Test void suppress_warnings_capitalisation_silences_type_name_rows() {
        var diags = validate("""
                namespace test
                annotation suppressWarnings:
                    capitalisation boolean (0..1)
                type lowerType:
                    [suppressWarnings capitalisation]
                    x int (1..1)
                """);
        assertEquals(0, count(diags, Severity.WARNING, TYPE_MSG));
    }
}
