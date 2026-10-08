package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Witness locks for the PR #458 issue-ANCHOR wave: the mojo-seam stream
 * measure (the fork plugin's live diagnostic stream diffed byte-for-byte
 * against the V0 oracle banks) proved every fork line file/line/message/
 * severity-identical to the released plugin's — and exposed 3,769 lines
 * whose COLUMN diverged: the fork anchored several warning families at the
 * node where upstream anchors a specific FEATURE token. The wave re-anchors
 * five seats:
 *
 * <ul>
 *   <li>{@code regulatoryReference} deprecation → the keyword token (one past
 *       the opening bracket; 3,749 drr bank lines);</li>
 *   <li>codeImplementation + dispatch missing-impl → the function NAME token
 *       (column 6 for a top-level {@code func }; 8+3 cdm + 3 drr lines);</li>
 *   <li>unused import → the imported-namespace token (column 8 after
 *       {@code import }; 3 drr lines);</li>
 *   <li>list-operation single-receiver → the OPERATOR keyword token (the drr
 *       bank's {@code last} pair);</li>
 *   <li>disjoint multi-cardinality → the LEFT operand for BOTH operand checks
 *       (upstream's left-feature anchoring quirk; 1 drr line).</li>
 * </ul>
 *
 * <p>Every lock here asserts the EXACT (line, column) of the anchored issue
 * and fails against the pre-#458 anchors (the node/operand columns differ),
 * satisfying the witness-uniqueness law. The corpus-wide proof is the mojo
 * re-run: both streams byte-identical to the banks INCLUDING columns.
 */
class IssueAnchorWaveTest {

    private static List<ValidationDiagnostic> validate(String... sources) {
        var models = Arrays.stream(sources)
                .map(s -> AstBuilder.buildFromString(s, "anchor-wave-test.rosetta"))
                .toList();
        return RWorkspace.build(models).workspace().validationDiagnostics();
    }

    private static ValidationDiagnostic sole(List<ValidationDiagnostic> diags,
            Severity severity, String message) {
        var matches = diags.stream()
                .filter(d -> d.severity() == severity && d.message().equals(message))
                .toList();
        assertEquals(1, matches.size(),
                "expected exactly one " + severity + " '" + message + "', got: " + diags);
        return matches.get(0);
    }

    @Test void regulatory_reference_anchors_at_the_keyword_token() {
        var diags = validate("""
                namespace test
                body Authority CFTC
                corpus Regulations "CFTC 17 CFR Parts 45" Rewrite
                type T:
                    a int (1..1)
                        [regulatoryReference CFTC Rewrite provision "x"]
                """);
        var d = sole(diags, Severity.WARNING,
                "Using `regulatoryReference` is deprecated. Use `docReference` instead");
        // Line 6: 8 spaces, `[` at column 9 — the keyword starts at column 10.
        assertEquals(6, d.range().startLine());
        assertEquals(10, d.range().startCol(),
                "the anchor is the keyword token, one past the opening bracket");
    }

    @Test void code_implementation_warning_anchors_at_the_function_name() {
        var diags = validate("""
                namespace test
                func NoImpl:
                    inputs:
                        x int (0..1)
                    output:
                        y int (0..1)
                """);
        var d = sole(diags, Severity.WARNING,
                "A function should specify an implementation, or they should be annotated with codeImplementation");
        // Line 2: `func NoImpl:` — the name token starts at column 6.
        assertEquals(2, d.range().startLine());
        assertEquals(6, d.range().startCol(), "the anchor is the function NAME token");
    }

    @Test void dispatch_missing_impl_anchors_at_the_base_function_name() {
        var diags = validate("""
                namespace test
                enum E:
                    A
                    B
                func DispF:
                    [codeImplementation]
                    inputs:
                        e E (1..1)
                    output:
                        out string (1..1)
                func DispF(e: E -> A):
                    set out: "a"
                """);
        var d = sole(diags, Severity.WARNING, "Missing implementation for E: B");
        // Line 5: `func DispF:` — the base function's name token at column 6.
        assertEquals(5, d.range().startLine());
        assertEquals(6, d.range().startCol(), "the anchor is the BASE function's NAME token");
    }

    @Test void unused_import_anchors_at_the_namespace_token() {
        var other = """
                namespace other.ns
                type Elsewhere:
                    a int (1..1)
                """;
        var importer = """
                namespace test
                import other.ns.*
                type Local:
                    a int (1..1)
                """;
        var d = sole(validate(other, importer), Severity.WARNING, "Unused import other.ns.*");
        // Line 2: `import other.ns.*` — the namespace token starts at column 8.
        assertEquals(2, d.range().startLine());
        assertEquals(8, d.range().startCol(), "the anchor is the imported-namespace token");
    }

    @Test void list_operation_single_receiver_anchors_at_the_operator_keyword() {
        var diags = validate("""
                namespace test
                type T:
                    one string (0..1)
                func F:
                    inputs:
                        t T (1..1)
                    output:
                        out string (0..1)
                    set out: t -> one last
                """);
        var d = sole(diags, Severity.WARNING,
                "List last operation cannot be used for single cardinality expressions.");
        // Line 9: `    set out: t -> one last` — the `last` keyword at column 23.
        assertEquals(9, d.range().startLine());
        assertEquals(23, d.range().startCol(), "the anchor is the OPERATOR keyword token");
    }

    @Test void disjoint_single_operand_anchors_at_the_left_operand() {
        var diags = validate("""
                namespace test
                enum E:
                    A
                    B
                type T:
                    x E (0..1)
                func F:
                    inputs:
                        t T (1..1)
                    output:
                        out boolean (0..1)
                    set out: [E -> A, E -> B] disjoint t -> x
                """);
        var d = sole(diags, Severity.WARNING,
                "Expecting multi cardinality. The `disjoint` operator requires a multi cardinality input");
        // Line 12: `    set out: [E -> A, E -> B] disjoint t -> x` — the RIGHT
        // operand is the single-cardinality violator, but upstream anchors the
        // check at the LEFT operand (the list literal's `[`, column 14).
        assertEquals(12, d.range().startLine());
        assertEquals(14, d.range().startCol(),
                "the anchor is the LEFT operand (upstream's left-feature quirk)");
    }
}
