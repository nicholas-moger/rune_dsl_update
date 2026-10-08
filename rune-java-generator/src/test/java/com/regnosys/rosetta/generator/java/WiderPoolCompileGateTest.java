package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * W42 sweep leg B — the WIDER fixture pool (PR #412; banked at slice 1 with the
 * CompileGate bar): the fork's 64 parser-only snippet fixtures
 * ({@code rune-parser/src/test/resources/snippets/**}) run through the FULL
 * generation battery as single-file hold-out models, with golden-free bars.
 * (The slice-1 plan's "~109 parser-only fixtures" counted the whole parser
 * resource tree; the 45 non-snippet fixtures — {@code symbolid-fixtures/} +
 * {@code symbols/} — are symbol-table/SymbolId test inputs, not generator-input
 * models, and are deliberately excluded — Seat-1 #412 MF-1.)
 *
 * <p>These snippets were authored for PARSE coverage — they exercise grammar corners
 * the engine-gating corpus does not — so no oracle golden exists for them (pinning
 * one is {@code scripts/holdout-oracle} work, deliberate and per-group). The bars
 * here are the slice-1 kind plus the PR #412 compile gate:
 * <ul>
 *   <li><b>Crash-freedom</b> — the pipeline must never throw (hard bar, no pins);</li>
 *   <li><b>Generation-error census</b> — a snippet producing generation errors must
 *       be listed in {@link #EXPECTED_GENERATION_ERRORS} (Freezing pattern: the set
 *       can only shrink; a new erroring snippet fails loudly);</li>
 *   <li><b>CompileGate</b> — a cleanly-generated snippet's output must compile
 *       against the released 9.83.0 runtime surface unless listed in
 *       {@link #EXPECTED_NON_COMPILING} (snippets with deliberately dangling
 *       cross-namespace references generate best-effort output that cannot compile —
 *       the documented mojo-validation design difference; the pin locks the set).</li>
 * </ul>
 *
 * <p>Skips when the builtins / released runtime jar are absent (CI / fresh clone),
 * the hold-out convention. FINOS sample models beyond the vendored hero-model (already
 * a hold-out group) are not vendored locally; widening to them stays with sweep leg E
 * (the differential-fuzz pool loader is the same plumbing).
 */
class WiderPoolCompileGateTest {

    private static final Path SNIPPETS_ROOT =
            Path.of("../rune-parser/src/test/resources/snippets");

    /**
     * Snippets whose generation reports errors (Freezing pattern — shrink-only; a
     * heal fails loudly until un-pinned). Population pinned at PR #412 from the first
     * classification run. BOTH original entries VALIDITY-TRIAGED at PR #422 (leg-E burn 8):
     * staged byte-verbatim through the released-9.83.0 oracle and REFUSED (severe
     * validation errors — the #416 invalid-fixture class: the fixture is the bug,
     * the pins stay; the fork's library pipeline best-effort-generates and reports
     * errors where upstream's mojo validation gate refuses outright):
     * <ul>
     *   <li>{@code function-with-extends} — oracle-REFUSED ×4: "You can only extend
     *       a function in a file with a scope"; "Function ChildFunc does not define
     *       all inputs of the original function BaseFunc"; the un-re-declared
     *       {@code result}/{@code x} unresolvable (upstream scoping does NOT inherit
     *       the super-func's inputs/output — FunctionExtensionValidator requires the
     *       child to re-declare them EcoreUtil2-equal). The fork's error on this
     *       invalid shape (the ST4 function template nulls on the missing own-output)
     *       is the invalid-fixture artifact, NOT a feature gap: the VALID form —
     *       {@code scope} + verbatim re-declared inputs/output + the
     *       experimental-scopes {@code rosetta-config.yml} — is the
     *       {@code func-extends-valid} oracle group, BYTE-IDENTICAL on the fork
     *       first-run (2/2). Generation-wise {@code extends} is validation-only at
     *       9.83.0: zero upstream generator consumers of
     *       {@code getSuperFunction()}/{@code getScope()}; the child renders as a
     *       standalone func.</li>
     *   <li>{@code simple-rule} — oracle-REFUSED ×4: dangling {@code Trade} ×2 +
     *       {@code tradeId} + {@code isActive} ("Couldn't resolve reference to
     *       RosettaType 'Trade'"). The fork's RMissingType error (the M3 Categories
     *       8/9/10 seat) is likewise the dangling-reference artifact: the VALID form
     *       — {@code Trade} declared locally, both rules verbatim — is the
     *       {@code report-simple-rule-valid} oracle group, BYTE-IDENTICAL on the
     *       fork first-run (7/7).</li>
     *   <li>{@code function-dispatch} — oracle-REFUSED: "Couldn't resolve reference to
     *       Attribute 'action'" (a dispatch VARIANT with no base declaration at all;
     *       the #421 triage). MOVED here at PR #624 (v3.2 seat 3, the round-1 code
     *       head's chain) from {@link #EXPECTED_NON_COMPILING}: the seat's register
     *       belt {@code SilentDegradation.Site#DISPATCH_BASE_MISSING} refuses a
     *       baseless group at generation with its reason, where the fork used to
     *       best-effort-emit a dispatch class from the variant that did not compile —
     *       the fork's verdict is now the released plugin's. The VALID form (the base
     *       declared in the same file as its variants) is the
     *       {@code func-dispatch-namespaces} oracle group (7/7 byte-identical) and the
     *       seat fixture's b1–b3; the cross-file split is the recorded upstream refusal
     *       ({@code func-dispatch-crossfile}, inadmissible).</li>
     *   <li>{@code simple-report} — oracle-REFUSED: every reference dangling ({@code from Trade},
     *       {@code when IsReportable}, {@code with type TradeReport} — none declared; the released
     *       plugin's verdict recorded at {@code target/v32-seat4-instruments/scratch/oracle-s4c.status},
     *       local: "Couldn't resolve reference to Data 'TradeReport'" and its siblings). MOVED here at
     *       PR #625 (v3.2 seat 4, commit 4: the commit-3 chain {@code s4a} @ {@code 83e32d490} — the
     *       chain's own catch, applied at {@code 4e6863310}): the seat's
     *       register site {@code SilentDegradation.Site#REPORT_REFERENCE_UNRESOLVED} refuses a report
     *       whose {@code with type} the linker could not resolve, at BOTH report seats, where the fork
     *       used to best-effort-emit a report function typed by a workspace-wide name search that found
     *       nothing — an emission that COMPILED by accident and sat in the pool's "clean" count. The
     *       VALID forms are the seat's four report oracle groups ({@code report-withtype-shadow} and
     *       siblings, byte-identical) and the seat fixture's a1–a4; the unresolvable reference is the
     *       fixture's a5 / a6 (the refusal by type and site at both seats).</li>
     * </ul>
     */
    private static final Set<String> EXPECTED_GENERATION_ERRORS = Set.of(
            "expressions/list-operations.rosetta",
            "functions/function-dispatch.rosetta",
            "functions/function-with-extends.rosetta",
            "reporting/simple-report.rosetta",
            "reporting/simple-rule.rosetta");

    /**
     * Snippets whose generated output does not compile against the released runtime
     * (Freezing pattern — shrink-only; a heal fails loudly until un-pinned).
     * Population pinned at PR #412 from the first classification run: 15 fixtures,
     * ALL expression-compiler facet leads on parse-coverage shapes the engine-gating
     * corpus never witnesses (e.g. the {@code to-string}-family conversions rendering
     * the rune type name {@code string}; sort/min-max, switch, with-meta and
     * deep-feature shapes outside the corpus-witnessed forms). The differential-fuzz
     * sweep (leg E) consumes this inventory; each heal un-pins its fixture in the same
     * change. SHRUNK at PR #417: {@code expressions/default-op.rosetta} retired — the
     * default-op facet healed oracle-byte-exact (the joined-literal coercion + the
     * MapperS ref-survival fix; see {@code HoldOutByteCompareTest.PINNED_FACET_LEADS}).
     * SHRUNK at PR #418: {@code expressions/switch.rosetta} retired — the
     * switch-literal facet healed oracle-byte-exact (the guard + arm joined-literal
     * coercions). SHRUNK at PR #419: {@code expressions/sort-min-max.rosetta}
     * retired — the explicit-param comparator facet healed oracle-byte-exact
     * (facet sortMinMaxClosureParam; see the byte bar's {@code PINNED_FACET_LEADS}
     * javadoc). SHRUNK at PR #420 (leg E burn 6 — FIVE pins retired together, each
     * healed oracle-byte-exact against its own new hold-out group): {@code
     * expressions/list-literal.rosetta} (facet listLiteralItemCoerce — the
     * output-item "Type coercion" map at the whole-output multi SET),
     * {@code expressions/logical.rosetta} (facet logicalParamCompResultLift —
     * bare boolean input-param operands of and/or lift into
     * {@code ComparisonResult.ofNullSafe}), {@code
     * expressions/functional-operations.rosetta} (facet reduceClosureParam —
     * reduce params render bare + the {@code <T>reduce} witness),
     * {@code expressions/misc-primaries.rosetta} (facet aliasStaticImportEscape —
     * an alias colliding with a referenced static operator member escapes
     * {@code _<name>}), {@code expressions/nested-complex.rosetta} (facet
     * aliasFilterCardinality — a filter receiver preserves its argument's
     * cardinality in the alias-signature walk).
     * {@code expressions/conversion.rosetta} STAYS the #416
     * missing-validation-gate class (the snippet itself is semantically invalid;
     * the VALID conversions oracle lives in the {@code expr-conversions-valid}
     * hold-out group — healed at #419). SHRUNK at PR #421 (leg E burn 7):
     * {@code expressions/with-meta.rosetta} retired — the ONE valid fixture of
     * the six-fixture oracle triage, healed oracle-byte-exact as the
     * {@code expr-with-meta} hold-out group (facets withMetaOwnTypeDerive +
     * setWithMetaValueUnwrap + withMetaExprWrapperCollect). The OTHER FIVE were
     * oracle-REFUSED at the same triage (each a severe validation error from the
     * released 9.83.0 plugin — the #416 invalid-fixture class; the refusal
     * verdicts, {@code target-421-oracle1/2.log}):
     * <ul>
     *   <li>{@code deep-feature-call} — "Couldn't resolve reference to Attribute
     *       'price'": {@code ->>} on a PLAIN attribute is not a deep-path
     *       receiver;</li>
     *   <li>{@code list-operations} — "List flatten only allowed for list of
     *       lists" ({@code items flatten} on a flat {@code number (0..*)}) — MOVED to
     *       {@link #EXPECTED_GENERATION_ERRORS} at v3.2 seat 12 (PR #633, D52 R3): the
     *       {@code ALIAS_SIGNATURE_RAW_TYPE} register site refuses an alias whose signature walk
     *       declines in a function with a BUILTIN ({@code number}) output at generation, where the fork
     *       used to emit {@code MapperS<? extends number>} alias signatures - non-compiling by construction
     *       (the #624 precedent: no non-compiling class is emitted any more; the three others of the five —
     *       {@code deep-feature-call} above, {@code without-left} and {@code label-annotation} below — stay
     *       non-compiling);</li>
     *   <li>{@code without-left} — "There is no implicit variable in this
     *       context" ×4 (every implicit-left {@code or/and/exists} form in the
     *       FUNC condition/SET seats);</li>
     *   <li>{@code function-dispatch} — "Couldn't resolve reference to Attribute
     *       'action'" (a dispatch func with NO base declaration) — MOVED to
     *       {@link #EXPECTED_GENERATION_ERRORS} at PR #624: the {@code DISPATCH_BASE_MISSING}
     *       belt refuses the baseless group at generation, so no non-compiling class is
     *       emitted any more (the other four of the five stayed non-compiling at #624; three since
     *       v3.2 seat 12 moved {@code list-operations});</li>
     *   <li>{@code label-annotation} — "Couldn't resolve reference to RosettaType
     *       'Party'" + Attribute 'name' (undeclared referent).</li>
     * </ul>
     * The three of those five that still generate (v3.2 seat 12 moved {@code list-operations} to the
     * generation-error set too) — plus {@code conversion}, the #416 class above —
     * stay pinned as parse-coverage inputs the fork's library pipeline
     * best-effort-generates without upstream's mojo validation gate (the
     * recorded expression-type-validation robustness item).
     */
    private static final Set<String> EXPECTED_NON_COMPILING = Set.of(
            "expressions/conversion.rosetta",
            "expressions/deep-feature-call.rosetta",
            "expressions/without-left.rosetta",
            "reporting/label-annotation.rosetta");

    static Stream<Path> snippetFixtures() throws IOException {
        if (!Files.isDirectory(SNIPPETS_ROOT)) {
            return Stream.empty();
        }
        try (var stream = Files.walk(SNIPPETS_ROOT)) {
            return stream.filter(p -> p.toString().endsWith(".rosetta"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("snippetFixtures")
    void snippet_generates_and_compiles(Path snippet) throws IOException {
        List<Path> builtinFiles = HoldOutByteCompareTest.resolveBuiltinFiles();
        Assumptions.assumeTrue(!builtinFiles.isEmpty(),
                "rune-dsl builtins absent — wider pool skipped");
        Assumptions.assumeTrue(ToolProvider.getSystemJavaCompiler() != null,
                "system java compiler unavailable — wider pool skipped");

        String key = SNIPPETS_ROOT.relativize(snippet).toString().replace('\\', '/');
        // Crash-freedom is the hard bar: any throw below fails the test raw.
        HoldOutByteCompareTest.GenerationRun run =
                HoldOutByteCompareTest.generateAllKindsFromFiles(
                        List.of(snippet), builtinFiles);

        boolean expectedErrors = EXPECTED_GENERATION_ERRORS.contains(key);
        if (!run.errorMessages().isEmpty()) {
            if (!expectedErrors) {
                Assertions.fail("WIDERPOOL " + key + ": UNEXPECTED generation error(s) — pin"
                        + " deliberately in EXPECTED_GENERATION_ERRORS or fix:\n  - "
                        + String.join("\n  - ", run.errorMessages()));
            }
            System.out.println("WIDERPOOL " + key + ": generation-errors (pinned) — "
                    + run.errorMessages().size());
            return;
        }
        if (expectedErrors) {
            Assertions.fail("WIDERPOOL " + key + ": pinned as generation-erroring but now"
                    + " generates CLEAN — un-pin deliberately (remove from"
                    + " EXPECTED_GENERATION_ERRORS in the same change)");
        }

        if (run.output().isEmpty()) {
            System.out.println("WIDERPOOL " + key + ": no output (alias/synonym-only)");
            return;
        }

        // The compile gate needs the released runtime surface.
        Assumptions.assumeTrue(hasReleasedRuntime(),
                "released rune-runtime:9.83.0 jar absent — wider-pool compile bar skipped");
        String label = "widerpool/" + key.replace('/', '~').replace(".rosetta", "");
        List<String> errors = HoldOutCompileGateTest.compileInMemory(label, run.output());
        boolean expectedNonCompiling = EXPECTED_NON_COMPILING.contains(key);
        if (!errors.isEmpty()) {
            if (!expectedNonCompiling) {
                StringBuilder sb = new StringBuilder("WIDERPOOL " + key + ": UNEXPECTED"
                        + " compile failure (" + errors.size() + " error(s)) — pin"
                        + " deliberately in EXPECTED_NON_COMPILING or fix:\n");
                errors.stream().limit(10).forEach(e -> sb.append("  ").append(e).append('\n'));
                Assertions.fail(sb.toString());
            }
            System.out.println("WIDERPOOL " + key + ": non-compiling (pinned) — "
                    + errors.size() + " error(s)");
            return;
        }
        if (expectedNonCompiling) {
            Assertions.fail("WIDERPOOL " + key + ": pinned as non-compiling but now compiles"
                    + " CLEAN — un-pin deliberately (remove from EXPECTED_NON_COMPILING in"
                    + " the same change)");
        }
        System.out.println("WIDERPOOL " + key + ": " + run.output().size()
                + " files compiled clean");
    }

    /** Delegates to the gate's single jar-path SOT (Copilot #412 R1 — no duplicate path logic). */
    private static boolean hasReleasedRuntime() {
        return HoldOutCompileGateTest.released983RuntimeJar() != null;
    }
}
