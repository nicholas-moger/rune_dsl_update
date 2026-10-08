package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Witness locks for the PR #458 eq-row linking wave: the two upstream scope
 * channels the fork's lexical chain cannot see, measured LIVE at the mojo
 * seam as the drr corpus's ONLY two linking over-fires (upstream builds
 * clean; the fork's {@code failOnValidationError} default broke the literal
 * DRR pom swap on exactly these) — the #448-recorded "eq rows":
 *
 * <ul>
 *   <li><b>Arm A</b> — a bare name that is a META feature of the implicit
 *       item ({@code then filter scheme = "..."} over a
 *       {@code [metadata scheme]} stream — the hkma carrier; upstream's
 *       {@code findFeaturesOfImplicitVariable} includes the wrapper's meta
 *       fields);</li>
 *   <li><b>Arm B</b> — a bare name that is a VALUE of the enumeration its
 *       equality SIBLING types to ({@code then filter qualification =
 *       confirmationDateTime} — the datetime-rule carrier; upstream's
 *       expected-type channel adds the expected enum's values to the
 *       scope).</li>
 * </ul>
 *
 * <p>Both arms are CLEARING-ONLY (the #451 choice-option precedent): the
 * stale pass-5 SYMBOL_NOT_FOUND is removed at the pass-6 engine, nothing
 * binds, so the render's own recovery rungs (implicitItemArgMeta +
 * misBoundSiblingAttributeEnumeration) keep their decline-arm paths and the
 * byte surface is untouched by construction. The corpus-wide proof is the
 * V1 gate's drr linking table emptying (SYMBOL 2 → 0) + the mojo swap
 * re-proof building DRR green with the stream byte-identical to the V0 bank.
 *
 * <p>Every fire-negative (clears) here FAILS against the pre-#458 engine
 * (the error survived); the guard positives (stays) pin the decision table's
 * edges per the witness-uniqueness law.
 */
class EqRowLinkingWaveTest {

    private static List<LinkingDiagnostic> link(String... sources) {
        var models = Arrays.stream(sources)
                .map(s -> AstBuilder.buildFromString(s, "eq-row-wave-test.rosetta"))
                .toList();
        return RWorkspace.build(models).linkingDiagnostics();
    }

    private static long symbolNotFound(List<LinkingDiagnostic> diags, String name) {
        return diags.stream()
                .filter(d -> d.category() == DiagnosticCategory.SYMBOL_NOT_FOUND
                        && d.message().equals("Symbol '" + name + "' not found"))
                .count();
    }

    // === Arm A — implicit-item META feature ================================

    /** The hkma carrier shape: a bare `scheme` filter over a [metadata scheme] stream. */
    @Test void meta_feature_filter_head_clears() {
        var diags = link("""
                namespace test
                type Party:
                    identifier string (0..*)
                        [metadata scheme]
                type Holder:
                    party Party (1..1)
                func F:
                    inputs:
                        inp Holder (1..1)
                    output:
                        out string (0..1)
                    set out:
                        inp
                            then extract party -> identifier
                            then filter scheme = "https://x"
                            then first
                """);
        assertEquals(0, symbolNotFound(diags, "scheme"),
                "a bare meta-feature filter head resolves silently upstream — the arm clears it");
    }

    /** The guard: the SAME shape without the [metadata scheme] annotation keeps the error. */
    @Test void plain_attribute_without_metadata_keeps_the_error() {
        var diags = link("""
                namespace test
                type Party:
                    identifier string (0..*)
                type Holder:
                    party Party (1..1)
                func F:
                    inputs:
                        inp Holder (1..1)
                    output:
                        out string (0..1)
                    set out:
                        inp
                            then extract party -> identifier
                            then filter scheme = "https://x"
                            then first
                """);
        assertEquals(1, symbolNotFound(diags, "scheme"),
                "without [metadata scheme] the name matches nothing — the error must stay");
    }

    // === Arm B — equality-sibling expected enum value =======================

    /**
     * The datetime-rule carrier shape INCLUDING its load-bearing ingredient:
     * a top-level declaration named {@code qualification} (the corpus's
     * builtin {@code annotation qualification:}) MIS-BINDS the eq sibling at
     * pass 5 (the #204 root-element-collision class — the P354M2 probe's
     * {@code sibSym=RAnnotation}), so the pre-#458 engine's clean-bind
     * channel declines and the bare enum VALUE stays an error. Arm B's item
     * walk recovers the REAL attribute past the mis-bind.
     */
    @Test void eq_sibling_enum_value_clears_past_the_annotation_misbind() {
        var diags = link("""
                namespace test
                annotation qualification: <"collides with the attribute name">
                enum QualEnum:
                    confirmationDateTime
                    executionDateTime
                type Stamp:
                    qualification QualEnum (0..1)
                type Holder:
                    stamps Stamp (0..*)
                func F:
                    inputs:
                        inp Holder (1..1)
                    output:
                        out Stamp (0..1)
                    set out:
                        inp -> stamps
                            then filter qualification = confirmationDateTime
                            then first
                """);
        assertEquals(0, symbolNotFound(diags, "confirmationDateTime"),
                "the item walk recovers Stamp.qualification past the annotation mis-bind");
    }

    /** The super-chain: the value lives on the PARENT enum (upstream getAllEnumValues). */
    @Test void eq_sibling_enum_value_resolves_through_the_super_chain() {
        var diags = link("""
                namespace test
                annotation qualification: <"collides with the attribute name">
                enum BaseQual:
                    confirmationDateTime
                enum QualEnum extends BaseQual:
                    executionDateTime
                type Stamp:
                    qualification QualEnum (0..1)
                type Holder:
                    stamps Stamp (0..*)
                func F:
                    inputs:
                        inp Holder (1..1)
                    output:
                        out Stamp (0..1)
                    set out:
                        inp -> stamps
                            then filter qualification = confirmationDateTime
                            then first
                """);
        assertEquals(0, symbolNotFound(diags, "confirmationDateTime"),
                "upstream getAllEnumValues walks the super-enum chain — so does the arm");
    }

    /**
     * PIN (pre-dates #458): with NO name collision the sibling binds cleanly
     * at the linker and the bare enum value already cleared on the pre-#458
     * engine — this lock pins that channel against regression; it does not
     * witness the wave.
     */
    @Test void eq_sibling_cleanly_bound_already_clears() {
        var diags = link("""
                namespace test
                enum QualEnum:
                    confirmationDateTime
                    executionDateTime
                type Stamp:
                    qualification QualEnum (0..1)
                type Holder:
                    stamps Stamp (0..*)
                func F:
                    inputs:
                        inp Holder (1..1)
                    output:
                        out Stamp (0..1)
                    set out:
                        inp -> stamps
                            then filter qualification = confirmationDateTime
                            then first
                """);
        assertEquals(0, symbolNotFound(diags, "confirmationDateTime"),
                "the clean-bind shape cleared before #458 — pinned, not a wave witness");
    }

    /** The guard: a name that is NOT a value of the sibling's enum keeps the error. */
    @Test void eq_sibling_non_value_keeps_the_error() {
        var diags = link("""
                namespace test
                enum QualEnum:
                    executionDateTime
                type Stamp:
                    qualification QualEnum (0..1)
                type Holder:
                    stamps Stamp (0..*)
                func F:
                    inputs:
                        inp Holder (1..1)
                    output:
                        out Stamp (0..1)
                    set out:
                        inp -> stamps
                            then filter qualification = notAValue
                            then first
                """);
        assertEquals(1, symbolNotFound(diags, "notAValue"),
                "the name is not among the enum's values — the error must stay");
    }

    /** The guard: an eq sibling that is NOT enum-typed clears nothing. */
    @Test void eq_sibling_non_enum_keeps_the_error() {
        var diags = link("""
                namespace test
                type Stamp:
                    marker string (0..1)
                type Holder:
                    stamps Stamp (0..*)
                func F:
                    inputs:
                        inp Holder (1..1)
                    output:
                        out Stamp (0..1)
                    set out:
                        inp -> stamps
                            then filter marker = someBareName
                            then first
                """);
        assertEquals(1, symbolNotFound(diags, "someBareName"),
                "a string-typed sibling contributes no enum values — the error must stay");
    }
}
