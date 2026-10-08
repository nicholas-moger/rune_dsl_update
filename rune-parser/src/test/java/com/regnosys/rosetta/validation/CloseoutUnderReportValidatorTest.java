package com.regnosys.rosetta.validation;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.1 CLOSE-OUT — the parser's standing conformance red (the close census's list (a),
 * "the four error-count under-reports"): {@code ResolutionConformanceTest} found four
 * snippet files where upstream 9.83.0 reports an ERROR the fork did not. Each rule is
 * ported at its upstream seat and pinned here in BOTH directions — the rejected shape
 * fires at the line upstream's oracle names, and the accepted twin beside it stays
 * silent — so the port can neither under-fire nor over-fire without a red:
 * <ol>
 *   <li>{@code [metadata key]} on an attribute (a2:45) — {@code MetadataValidator};</li>
 *   <li>{@code flatten} over a list that is not a list of lists (b2:64) —
 *       {@code ExpressionValidator.checkFlattenArgument};</li>
 *   <li>a bare enumeration in value position (c1:53) and the output-assignment type
 *       check it then trips (c1:52, c1:78) — {@code ExpressionValidator};</li>
 *   <li>a feature off an enum-typed expression (p9:26) — the linking diagnostic
 *       {@code TypeInferenceEngine.reportUnresolvedFeaturesOnEnumReceivers}.</li>
 *   <li>v3.2 seat 5 (PR #626): {@code with-meta} on a multi-cardinality argument — the released
 *       plugin's ERROR ("Expecting single cardinality. The with-meta operator can only be used with
 *       single cardinality arguments") the fork did not carry until the seat's oracle run refused
 *       its fixture — {@code ExpressionValidator.checkWithMetaSingle}.</li>
 * </ol>
 * Built without the builtin models on purpose: none of the shapes needs them, and the
 * unresolved {@code metadata} annotation is a LINKING diagnostic this test never reads
 * for the validation cases (the validation half is the one under test; the two linking
 * cases filter on {@code FEATURE_NOT_FOUND} alone). The conformance suite is the
 * like-for-like oracle comparison; this is the per-rule lock. The two review-driven
 * cases at the end pin the #606 review's MF-1 (the enum-receiver diagnostic's decline
 * where the meta channel cannot look) and SF-1 (the condition and enum-value hosts).
 */
class CloseoutUnderReportValidatorTest {

    @Test
    void metadata_key_on_an_attribute_is_rejected_where_the_same_face_on_a_type_is_accepted() {
        var ws = build("""
                namespace test
                type Security:
                    identifier string (1..1)
                type Holder:
                    tagged Security (0..*)
                        [metadata key]
                type Keyed:
                    [metadata key]
                    id string (1..1)
                """);
        List<ValidationDiagnostic> hits = errorsMatching(ws,
                "[metadata key] annotation only allowed on a type.");
        assertEquals(List.of(6), lines(hits), "exactly the attribute-level face, at its own line: " + hits);
    }

    @Test
    void with_meta_on_a_multi_cardinality_argument_is_rejected_where_a_single_one_is_accepted() {
        // v3.2 seat 5 (PR #626): the released plugin REFUSED the seat's oracle fixture `h -> codes with-meta {…}`
        // (upstream ExpressionValidator.checkWithMetaOperation → isSingleCheckError) — an ERROR the fork did not carry.
        String src = """
                namespace test
                metaType scheme string
                type Holder:
                    codes string (0..*)
                        [metadata scheme]
                    coded string (0..1)
                        [metadata scheme]
                func Bad:
                    inputs:
                        h Holder (1..1)
                    output:
                        cs string (0..*)
                            [metadata scheme]
                    set cs: h -> codes with-meta { scheme: "x" }
                func Good:
                    inputs:
                        h Holder (1..1)
                    output:
                        c string (0..1)
                            [metadata scheme]
                    set c: h -> coded with-meta { scheme: "x" }
                """;
        var ws = build(src);
        List<ValidationDiagnostic> hits = errorsMatching(ws,
                "Expecting single cardinality. The with-meta operator can only be used with single cardinality arguments");
        assertEquals(List.of(14), lines(hits), "exactly the multi argument, at its own line: " + hits);
        // Anchored at the ARGUMENT, not at the whole `with-meta` expression (round 1, cq NIT-2 / spec N-4): both
        // share line 14, so the line alone cannot tell them apart — the diagnostic's columns, read back through
        // the fixture text, can. `h -> codes` is the argument; the expression runs on to the closing `}`.
        SourceRange at = hits.get(0).range();
        assertEquals(14, at.endLine(), "the argument sits on its own line: " + at);
        String line14 = src.lines().toList().get(at.startLine() - 1);
        assertEquals("h -> codes", line14.substring(at.startCol() - 1, at.endCol()),
                "the range covers the argument alone, not the with-meta expression it sits in: " + at);
    }

    @Test
    void flatten_over_a_plain_list_is_rejected_where_flatten_over_a_list_of_lists_is_accepted() {
        var ws = build("""
                namespace test
                type Party:
                    role string (0..1)
                    names string (0..*)
                func Bad:
                    inputs:
                        parties Party (0..*)
                    output:
                        result string (0..*)
                    set result:
                        parties extract role
                            then flatten
                func Good:
                    inputs:
                        parties Party (0..*)
                    output:
                        result string (0..*)
                    set result:
                        parties extract names
                            then flatten
                """);
        List<ValidationDiagnostic> hits = errorsMatching(ws, "List flatten only allowed for list of lists.");
        assertEquals(List.of(12), lines(hits),
                "the keyword line of the single-bodied extract's flatten, and nothing at Good's: " + hits);
    }

    @Test
    void flatten_over_a_pipe_with_a_disguised_choice_hop_declines_the_fork_cannot_read_that_cardinality() {
        // The three drr 7.x corpus carriers' shape: a choice-option hop (`items -> Basket`, parsed as
        // one REnumValueRef with a choice-option binding only) feeds the pipe; the fork reads the
        // hop's cardinality blind, upstream reads the MULTI head — the check must DECLINE, never fire.
        var ws = build("""
                namespace test
                type Basket:
                    constituents string (0..*)
                type Security:
                    identifier string (1..1)
                choice Observable:
                    Basket
                    Security
                type Trade:
                    underliers Observable (0..*)
                func GetConstituents:
                    inputs:
                        basket Basket (0..1)
                    output:
                        result string (0..*)
                    set result:
                        basket -> constituents
                func Carrier:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result string (0..*)
                    set result:
                        trade -> underliers -> Basket
                            then extract GetConstituents
                            then flatten
                """);
        assertEquals(List.of(), lines(errorsMatching(ws, "List flatten only allowed for list of lists.")),
                "a pipe whose head is a choice-option hop the fork cannot size DECLINES (the #454 law)");
        // An absence-only lock by construction: the test cannot tell "declined" from "read as a
        // genuine list of lists". The decline mechanism's positive evidence is the measurement that
        // introduced it — the twenty-cell gate read CARDINALITY_ERROR=3 on drr 7.0.0 for exactly this
        // shape before the decline landed (commit 6's record) and ZERO after.
    }

    @Test
    void a_bare_enumeration_in_value_position_is_rejected_and_its_type_fails_the_output_assignment() {
        var ws = build("""
                namespace test
                enum Colour:
                    Red
                    Green
                type Item:
                    Colour string (0..1)
                type Box:
                    items Item (0..*)
                func Bad:
                    inputs:
                        box Box (1..1)
                    output:
                        result string (0..*)
                    set result:
                        box -> items
                            extract Colour
                func Good:
                    inputs:
                        box Box (1..1)
                    output:
                        result Colour (0..*)
                    set result:
                        box -> items
                            extract Colour -> Red
                """);
        List<ValidationDiagnostic> face = errorsMatching(ws,
                "Enum type `Colour` must be followed by ` -> <enum value>`. Possible values are: Red, Green");
        assertEquals(List.of(16), lines(face), "the bare `extract Colour` binds the GLOBAL enum (the oracle's"
                + " precedence) and names a type where a value is expected: " + face);
        List<ValidationDiagnostic> assign = errorsMatching(ws,
                "Expected type `string`, but got `Colour` instead. Cannot assign `Colour` to output `result`");
        assertEquals(List.of(15), lines(assign),
                "the enum-typed value against the string output, anchored at the assigned expression: " + assign);
        assertEquals(List.of(), lines(errorsMatching(ws, "Expected type ").stream()
                .filter(d -> d.range().startLine() > 16).toList()),
                "Good's `Colour -> Red` types as the enum and assigns cleanly (every assignment error"
                        + " begins `Expected type `, so this prefix is the one that can match)");
    }

    @Test
    void the_output_assignment_check_reads_the_item_feature_when_the_bare_name_binds_it() {
        var ws = build("""
                namespace test
                enum Flavour:
                    Sweet
                    Salt
                type Item:
                    Salt string (0..1)
                type Box:
                    items Item (0..*)
                func Bad:
                    inputs:
                        box Box (1..1)
                    output:
                        result Flavour (0..*)
                    set result:
                        box -> items
                            extract Salt
                func Good:
                    inputs:
                        box Box (1..1)
                    output:
                        result string (0..*)
                    set result:
                        box -> items
                            extract Salt
                """);
        List<ValidationDiagnostic> assign = errorsMatching(ws,
                "Expected type `Flavour`, but got `string` instead. Cannot assign `string` to output `result`");
        assertEquals(List.of(15), lines(assign), "c1:78's shape — the implicit item's string feature outranks"
                + " the expected enum's value within the lowest bucket, so the string fails the enum output: " + assign);
        assertEquals(List.of(), lines(errorsMatching(ws, "Expected type ").stream()
                .filter(d -> d.range().startLine() > 16).toList()),
                "Good assigns the same string feature to a string output — silent");
    }

    @Test
    void metadata_key_on_a_condition_or_an_enum_value_is_rejected_like_any_non_type_host() {
        // The two upstream `Annotated` hosts the first port did not walk (the #606 review's SF-1).
        var ws = build("""
                namespace test
                type Holder:
                    id string (1..1)
                    condition Named:
                        [metadata key]
                        id exists
                enum Colour:
                    Red
                        [metadata key]
                    Green
                """);
        List<ValidationDiagnostic> hits = errorsMatching(ws,
                "[metadata key] annotation only allowed on a type.");
        assertEquals(List.of(5, 9), lines(hits), "the condition's and the enum value's faces, each at its own line: " + hits);
    }

    @Test
    void a_meta_feature_off_an_enum_typed_function_call_declines_the_meta_channel_cannot_look_there() {
        // The #606 review's MF-1: `[metadata scheme]` on an enum-typed attribute resolves `-> scheme`
        // upstream even through a function-call receiver; the fork's R10 meta arm cannot name that
        // receiver's attribute, so the enum-receiver diagnostic must DECLINE there, never fire.
        RLinkingResult result = link("""
                namespace test
                enum EntityTypeEnum:
                    Bank
                    Fund
                type Ref:
                    entityType EntityTypeEnum (0..1)
                        [metadata scheme]
                func GetEntityType:
                    inputs:
                        r Ref (1..1)
                    output:
                        out EntityTypeEnum (0..1)
                    set out:
                        r -> entityType
                func F:
                    inputs:
                        r Ref (1..1)
                    output:
                        s string (0..1)
                    set s:
                        GetEntityType(r) -> scheme
                """);
        assertEquals(List.of(), result.linkingDiagnostics().stream()
                .filter(d -> d.category() == DiagnosticCategory.FEATURE_NOT_FOUND).toList(),
                "a function-call receiver is outside the meta channel's reach — silence, not an error");
    }

    @Test
    void a_feature_off_an_enum_typed_expression_is_a_linking_error_where_the_literal_enumeration_resolves() {
        RLinkingResult result = link("""
                namespace test
                enum Beta9:
                    V1
                    V2
                type Holder9:
                    b Beta9 (1..1)
                func Bad:
                    inputs:
                        h Holder9 (1..1)
                    output:
                        result Beta9 (0..1)
                    set result:
                        h -> b -> V1
                func Good:
                    inputs:
                        h Holder9 (1..1)
                    output:
                        result Beta9 (0..1)
                    set result:
                        Beta9 -> V1
                """);
        List<LinkingDiagnostic> hits = result.linkingDiagnostics().stream()
                .filter(d -> d.category() == DiagnosticCategory.FEATURE_NOT_FOUND)
                .toList();
        assertEquals(1, hits.size(), "exactly the enum-typed receiver's feature: " + hits);
        assertEquals(13, hits.get(0).range().startLine(), hits.get(0).toString());
        assertEquals("V1", hits.get(0).unresolvedName());
        assertEquals(Severity.ERROR, hits.get(0).severity());
        assertTrue(hits.get(0).message().contains("Beta9 -> V1"), hits.get(0).message());
    }

    private static RWorkspace build(String source) {
        return link(source).workspace();
    }

    private static RLinkingResult link(String source) {
        RModel model = AstBuilder.buildFromString(source, "closeout-port.rosetta");
        return RWorkspace.build(List.of(model));
    }

    private static List<ValidationDiagnostic> errorsMatching(RWorkspace ws, String messagePrefix) {
        return ws.validationDiagnostics().stream()
                .filter(d -> d.severity() == Severity.ERROR)
                .filter(d -> d.message().startsWith(messagePrefix))
                .toList();
    }

    private static List<Integer> lines(List<ValidationDiagnostic> diags) {
        return diags.stream().map(d -> d.range().startLine()).sorted().toList();
    }
}
