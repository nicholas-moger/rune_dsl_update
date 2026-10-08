package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR-A Task 2 (C2.13) — four pinned byte-level tests that guard the
 * Java-generator import-collection behaviour for representative functions.
 *
 * <p>Each test covers one of the three spec-required mismatch categories
 * from <code>docs/superpowers/specs/2026-04-17-m7b-architecture-review.md</code>
 * § 9.1 line 523 (MISSING_IMPORTS, EXTRA_IMPORTS, WRONG_IMPORT_ORDER) plus
 * the spec-named scenario.
 *
 * <p>They pin the fork's current generator output so a later refactor
 * cannot silently regress import attribution — they exist to catch
 * import-section drift in the codegen path.
 *
 * <p>Determinism: the corpus is the version-frozen {@code cdm/6.20.6} cell
 * (see {@link #CDM_ROSETTA_DIR}), NOT the unpinned {@code ../common-domain-model}
 * master clone these tests originally tracked. A frozen cell makes the
 * generator output a pure function of the fork's code, so these tests flip
 * only when the generator changes — never when an external corpus clone
 * advances. (Re-pointed 2026-05-30; the master clone had drifted SSNC's
 * import set 12 → 22, turning the exact-list test red on a green tree.)
 *
 * <p>Harness note: follows {@link
 * com.regnosys.rosetta.generator.java.D11GoldenComparisonTest}'s
 * {@code @BeforeAll} full-corpus generation pattern. Cost is the one-time
 * corpus load; per-test assertions are fast string filters.
 */
class FunctionGeneratorImportCollectionTest {

    // Pinned to the version-frozen cdm/6.20.6 cell (the same pinned corpus
    // D11CorpusRegressionTest's ALL_CELLS uses), NOT the unpinned
    // ../common-domain-model master clone. The master clone drifts
    // independently of any code change (it tracked CDM 7.0.0-dev.9x and grew
    // SSNC's import set from 12 → 22), which made this test non-deterministic
    // and red on a green tree. A frozen cell makes the generator output a pure
    // function of the fork's code: this test now flips ONLY when the generator
    // changes (a true regression guard), not when an external clone advances.
    // cdm/6.20.6 carries both probe functions (SSNC source + golden, and Abs).
    private static final Path CDM_ROSETTA_DIR = Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR = Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/generated/java");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    private static Map<String, String> functionOutput;

    static boolean cdmAvailable() {
        // Copilot round-13 finding: isDirectory() alone passes even when the
        // directory is empty (e.g. checked-out-but-not-populated corpus). Walk
        // the tree for at least one .rosetta file so the test actually has
        // input before running.
        if (!Files.isDirectory(CDM_ROSETTA_DIR) || !Files.isDirectory(CDM_GOLDEN_DIR)) {
            return false;
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            return stream.anyMatch(p -> p.toString().endsWith(".rosetta"));
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeAll
    static void generateAllFunctions() throws IOException {
        if (!cdmAvailable()) return;
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        functionOutput = new LinkedHashMap<>();
        gen.generate(functionOutput);
    }

    /**
     * Pinned FQN list for
     * {@code cdm/margin/schedule/functions/StandardizedScheduleNotionalCurrency.java}
     * in emitted order, captured from the fork's generator output against the
     * version-frozen {@code cdm/6.20.6} cell (see {@link #CDM_ROSETTA_DIR}).
     * The generator emits exactly 2 blocks separated by one blank line:
     * (1) regular imports — all FQNs globally alphabetically sorted (cdm /
     * com / javax interleaved by lexicographic order, NOT grouped by prefix —
     * {@code ImportCollector} uses a single {@code TreeSet<String>});
     * (2) static imports — always last.
     *
     * <p>This is the fork's CURRENT output for the frozen cell, not the
     * upstream golden — it exists to catch import-attribution regressions, so
     * the value is whatever the fork deterministically emits today. Because
     * the cell is version-frozen, that value is a pure function of the fork's
     * code: this list changes ONLY when the generator changes, never when an
     * external corpus clone advances. (It previously tracked the unpinned
     * {@code ../common-domain-model} master clone and drifted 12 → 22 imports
     * with no code change, which made this test red on a green tree — the
     * reason for the re-point.)
     *
     * <p>The fork's SSNC body is still incomplete vs the 267-line golden. When
     * an engine-phase PR improves SSNC codegen the emitted import set will
     * change — update this list AS PART OF that PR's diff. That is a legitimate
     * generator-change update, categorically different from the external-clone
     * drift this re-point eliminated.
     *
     * <p>Engine PR #9 (PR-B, 2026-05-31) did exactly that: activating the
     * meta-coercion typed pipeline made the SSNC body emit the {@code .getValue()}
     * unwrap for its {@code [metadata reference]}/{@code [metadata scheme]}
     * navigations, adding the three meta imports {@code cdm.observable.asset.PriceSchedule},
     * {@code cdm.observable.asset.metafields.ReferenceWithMetaPriceSchedule} and
     * {@code com.rosetta.model.metafields.FieldWithMetaString} (all present in the
     * golden) — 18 → 21.
     */
    private static final List<String> EXPECTED_IMPORTS_SSNC = List.of(
        // Regular imports (globally alphabetical — cdm/com/javax interleaved by lex order)
        // PR #163 (alias_receiver_typing) — SSNC's body is built almost entirely on
        // qualify-style aliases; the alias-rooted navigation chains now recover their
        // <Type> witnesses, so the witness imports register (all ten present in the
        // golden import set). 24 → 34; the import set moves toward the golden (SSNC
        // stays waivered — its un-emitted thenArg navigations are a separate facet).
        "cdm.base.math.NonNegativeQuantitySchedule",
        "cdm.base.math.UnitType",
        "cdm.base.math.metafields.ReferenceWithMetaNonNegativeQuantitySchedule",
        // PR #159 (checkedmap_enum_source) — the ENUM arm of ConversionHandler now
        // registers the TARGET enum's import ref: SSNC's condition compiles a
        // `notionalCurrency to-enum ISOCurrencyCodeEnum` conversion whose emitted
        // ISOCurrencyCodeEnum::fromDisplayName references the enum's simple name
        // (golden import L6, call L66) — exactly the anticipated "when an
        // engine-phase PR improves SSNC codegen" update from the class javadoc.
        // 23 → 24; the import set moves toward the golden (SSNC stays waivered —
        // its un-emitted thenArg navigations are a separate facet).
        "cdm.base.staticdata.asset.common.ISOCurrencyCodeEnum",
        "cdm.event.common.Trade",
        "cdm.margin.schedule.StandardizedScheduleAssetClassEnum",
        "cdm.margin.schedule.StandardizedScheduleProductClassEnum",
        // Engine PR #9 (PR-B) — meta-coercion activation: the SSNC body navigates a
        // priceSchedule [metadata reference] (ReferenceWithMetaPriceSchedule), now
        // unwrapped via .<PriceSchedule>map("Type coercion", ... .getValue()). Both the
        // value type and its wrapper are in the golden import set (golden L11-L12).
        "cdm.observable.asset.Price",
        "cdm.observable.asset.PriceSchedule",
        "cdm.observable.asset.metafields.ReferenceWithMetaPriceSchedule",
        // PR #153 (choice_option_nav_witness) — the SSNC body navigates the `Payout`
        // CHOICE options (`payout -> CreditDefaultPayout`/`InterestRatePayout`/
        // `FixedPricePayout`); recovering the choice-option witness now emits their
        // imports (all three present in the 267-line golden, L13/L15/L21). SSNC stays
        // waivered (its body is still incomplete vs the golden — un-emitted thenArg
        // navigations, a separate facet), but the import set moves toward the golden.
        "cdm.product.asset.CreditDefaultPayout",
        // PR #168 (lambda_item_body_coercion, arm B2) — SSNC's in-lambda
        // disguised-chain navigations through `rateSpecification ->
        // FixedRateSpecification` now synthesize item-rooted feature calls,
        // registering the step witnesses' imports (both present in the golden
        // import set). SSNC stays waivered (remaining body debt is a separate
        // facet), but the import set moves toward the golden again.
        "cdm.product.asset.FixedRateSpecification",
        "cdm.product.asset.InterestRatePayout",
        "cdm.product.asset.RateSpecification",
        "cdm.product.asset.VarianceReturnTerms",
        "cdm.product.common.settlement.FixedPrice",
        "cdm.product.common.settlement.ResolvablePriceQuantity",
        "cdm.product.template.EconomicTerms",
        "cdm.product.template.FixedPricePayout",
        "cdm.product.template.NonTransferableProduct",
        "cdm.product.template.OptionPayout",
        "cdm.product.template.OptionStrike",
        "cdm.product.template.Payout",
        "cdm.product.template.PerformancePayout",
        "cdm.product.template.Product",
        "cdm.product.template.ReturnTerms",
        "cdm.product.template.SettlementPayout",
        "cdm.product.template.TradeLot",
        // PR #205 (deep_path_util_resolution) — SSNC's body navigates a `product ->>
        // economicTerms` deep-arrow call through the one-of/choice Product. The
        // FunctionDependencyCollector now resolves the receiver type gm-aware
        // (NavigationHandler.resolveDeepReceiverSymbolId) and injects the
        // `@Inject ProductDeepPathUtil` field (golden import set), where the dormant
        // M7b-4 stub emitted no DeepPathUtil dependency. SSNC stays waivered (remaining
        // body debt is a separate facet), but the import set moves toward the golden.
        "cdm.product.template.util.ProductDeepPathUtil",
        "com.google.inject.ImplementedBy",
        "com.rosetta.model.lib.expression.CardinalityOperator",
        "com.rosetta.model.lib.functions.ConditionValidator",
        "com.rosetta.model.lib.functions.RosettaFunction",
        // PR #121 (nested_then_conditional) — the nested-then if-block fix
        // removed the spurious `MapperC.of().get()` ternary else the fork used
        // to emit for SSNC's inner `if/then` (no else). PR #391
        // (armSeatChainedBareFnThen): the arm-seat then-chain decomposition now
        // RENDERS the golden `MapperC<InterestRatePayout> thenArg*` decls the
        // PR #121 note called un-emitted body debt — SSNC FLIPPED byte-identical
        // and the import collector re-imports MapperC exactly as golden does.
        "com.rosetta.model.lib.mapper.MapperC",
        "com.rosetta.model.lib.mapper.MapperS",
        // Engine PR #9 (PR-B) — meta-coercion activation: FieldWithMeta value type for
        // the currency [metadata scheme] navigation (golden import L38).
        "com.rosetta.model.metafields.FieldWithMetaString",
        "javax.inject.Inject",
        // Static imports (always last)
        "com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*"
    );

    private static final int EXPECTED_SSNC_COUNT = 38; // PR #391: +1 (com.rosetta.model.lib.mapper.MapperC — the SSNC flip)

    @Test
    @EnabledIf("cdmAvailable")
    void missing_imports_do_not_recur_for_qualify_aliases() {
        // Spec § 9.1 line 523 — pinned FQN set for
        // StandardizedScheduleNotionalCurrency (a function whose qualify-
        // style aliases surface types that the regex import scan misses).
        // Self-check against empty paste: if the pinned list is ever
        // cleared, fail early with a clear message.
        assertFalse(EXPECTED_IMPORTS_SSNC.isEmpty(),
            "EXPECTED_IMPORTS_SSNC empty — did you paste the diagnostic output in Step 2.1?");
        assertEquals(EXPECTED_SSNC_COUNT, EXPECTED_IMPORTS_SSNC.size(),
            "EXPECTED_IMPORTS_SSNC size drift — update EXPECTED_SSNC_COUNT or the list.");

        String generated = requireGenerated(
            "cdm/margin/schedule/functions/StandardizedScheduleNotionalCurrency.java");

        List<String> actual = extractImportLines(generated);
        assertEquals(EXPECTED_IMPORTS_SSNC, actual,
            "SSNC import set drifted — PR-A refactor may have changed import attribution");
    }

    @Test
    @EnabledIf("cdmAvailable")
    void no_extra_collections_import_when_unused() {
        // Spec § 9.1 line 523 — a function whose generated body does not
        // reference java.util.Collections must not acquire a spurious
        // Collections import.
        //
        // Source of java.util.Collections (the only emit gate): the
        // needsCollections heuristic in FunctionGenerator (L626-650)
        // emits {@code Collections.emptyList()} when an input is
        // multi-valued. Post-C3c.2, the legacy regex scan has been
        // deleted entirely, so needsCollections is the sole gate; no
        // handler declares Collections as a structured ref, and
        // HandlerHelper.COLLECTIONS is wired but not yet consumed by
        // any emission. This invariant guards against the
        // needsCollections heuristic firing on a function whose final
        // body omits {@code Collections.emptyList()} (e.g. because the
        // emit site was short-circuited elsewhere), leaving a dangling
        // import.
        //
        // Substitution note: spec names GenerateDateList, but no function in
        // the pinned cdm/6.20.6 cell acquires a spurious java.util.Collections
        // import (the needsCollections gate only fires when the body actually
        // emits Collections.emptyList()). The test therefore pins the
        // structural invariant — checked across ALL generated functions
        // below — rather than a specific
        // function: NONE of the currently-generated functions must
        // acquire java.util.Collections unless the function body actually
        // references it. The SSNC function is used as the concrete probe
        // because its generated body is fully known to the other tests
        // in this class and does not use Collections.
        String generated = requireGenerated(
            "cdm/margin/schedule/functions/StandardizedScheduleNotionalCurrency.java");

        List<String> actualImports = extractImportLines(generated);
        assertFalse(actualImports.contains("java.util.Collections"),
            "Spurious java.util.Collections import in SSNC — needsCollections "
                + "heuristic misfired (FunctionGenerator L626-650).");
        // Guard the invariant for all currently-generated functions too:
        // no function body whose output omits "Collections." may import it.
        // Collect all offenders then fail once with the full list (see
        // D11GoldenComparisonTest.pojo_all_base_math_types for the pattern)
        // so that if C3c.2 regresses N functions, we surface all N rather
        // than the first one only.
        //
        // Word-boundary probe: {@code \bCollections\b} avoids the naive
        // {@code contains("Collections")} false-positive where generated
        // bodies embed the word "Collections" inside a string literal,
        // javadoc, or comment but the class itself never references the
        // type. A true Collections type-reference always sits at a word
        // boundary (Collections.emptyList, Collections<..., (Collections),
        // etc.) so this tightening is sound for the current emitter.
        //
        // Critical: strip the import block BEFORE probing. The probe must
        // scan only the class body — otherwise the import declaration
        // {@code import java.util.Collections;} itself matches the word
        // boundary, the guard collapses to {@code true && !true = false},
        // and the loop silently never flags any offender. Caught by Copilot
        // review on commit db31616 / 99f1649 2026-04-20.
        //
        // Edge case to be aware of: a Rosetta identifier named exactly
        // "Collections" would also match, but the generator's attribute
        // lowercasing rules make a bare uppercase "Collections" identifier
        // unreachable in generated output.
        java.util.regex.Pattern collectionsWord =
            java.util.regex.Pattern.compile("\\bCollections\\b");
        List<String> offenders = new ArrayList<>();
        for (var entry : functionOutput.entrySet()) {
            String body = entry.getValue();
            List<String> imports = extractImportLines(body);
            boolean importsCollections = imports.contains("java.util.Collections");
            if (!importsCollections) continue;
            // Scan only the class body, not the imports — the import line
            // itself trivially matches \bCollections\b.
            String bodyWithoutImports = stripImportBlock(body);
            boolean referencesCollections =
                collectionsWord.matcher(bodyWithoutImports).find();
            if (!referencesCollections) {
                offenders.add(entry.getKey());
            }
        }
        if (!offenders.isEmpty()) {
            throw new AssertionError("Spurious Collections import in "
                + offenders.size() + " function(s) — needsCollections "
                + "heuristic misfired (FunctionGenerator L626-650): "
                + offenders);
        }
    }

    @Test
    @EnabledIf("cdmAvailable")
    void lowercase_builtin_types_in_metafield_wrappers() {
        // Spec § 9.1 line 523 — when an alias body references a lowercase
        // Rosetta builtin type (e.g. "number", "date", "time"), the
        // generator must resolve it to the corresponding Java type and
        // emit the import. The Rosetta lowercase builtin "number" maps
        // to java.math.BigDecimal, which is the most common "lowercase
        // builtin" case appearing in the CDM corpus.
        //
        // Substitution note: spec names LocalDate as the lowercase-builtin
        // example, but no function in the pinned cdm/6.20.6 cell imports
        // java.time.LocalDate (the DSL uses
        // com.rosetta.model.lib.records.Date for date values — a
        // design-time choice, not a generator gap). java.math.BigDecimal
        // is the closest semantic equivalent: the lowercase Rosetta
        // builtin "number" resolves to BigDecimal via the same lookup
        // path that "date" → LocalDate would take. Probe:
        // cdm/base/math/functions/Abs.java (byte-identical to its golden in
        // the pinned cell, so this test guards a known-good attribution).
        String generated = requireGenerated("cdm/base/math/functions/Abs.java");

        List<String> imports = extractImportLines(generated);
        assertTrue(imports.contains("java.math.BigDecimal"),
            "BigDecimal import missing from Abs — lowercase builtin 'number' "
                + "type resolution regressed. Actual imports: " + imports);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void imports_sorted_alphabetically_within_groups() {
        // Spec § 9.1 line 523 (WRONG_IMPORT_ORDER) — within each import
        // group imports must be emitted in alphabetical order. The
        // generator emits exactly 2 groups separated by one blank line:
        // (1) regular imports, globally alphabetically sorted (no
        // cdm/com/javax prefix grouping); (2) static imports, always
        // last regardless of FQN lexicographic position.
        //
        // Probe function: StandardizedScheduleNotionalCurrency (the
        // function with the largest import set in the baseline sample —
        // exercises the sort invariant across the 2 groups: regular
        // (globally sorted) and static (always last)).
        String generated = requireGenerated(
            "cdm/margin/schedule/functions/StandardizedScheduleNotionalCurrency.java");

        List<List<String>> groups = extractImportGroups(generated);
        assertFalse(groups.isEmpty(),
            "No import groups found in SSNC — extraction regression?");

        for (int i = 0; i < groups.size(); i++) {
            List<String> group = groups.get(i);
            List<String> sortedCopy = new ArrayList<>(group);
            Collections.sort(sortedCopy);
            assertEquals(sortedCopy, group,
                "Import FQNs in group " + i + " not alphabetically sorted — "
                    + "ImportCollector order invariant regressed. Group: " + group);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns the imports flattened to a {@code List<String>} in emitted
     * order — useful for set-equality comparisons and simple containment
     * checks that do not care about the blank-line-separated group
     * structure. Strips the leading {@code import } and optional
     * {@code static } keyword plus the trailing semicolon and whitespace.
     * Blank lines between groups are discarded; group separation is NOT
     * preserved. Test 4 uses {@link #extractImportGroups} instead, which
     * preserves the blank-line-separated 2-group structure required to
     * verify the static-imports-last invariant (per-group sort is strictly
     * WEAKER than global sort, not stricter — static imports routinely
     * break global order at the regular/static boundary).
     */
    private static List<String> extractImportLines(String source) {
        return source.lines()
            .filter(l -> l.startsWith("import "))
            .map(l -> l.replaceAll("^import\\s+(static\\s+)?", "")
                       .replaceAll(";\\s*$", ""))
            .toList();
    }

    /**
     * Return the generated source with the import block removed, so that
     * body-content probes (e.g. {@code \bCollections\b} word-boundary match)
     * don't spuriously hit the import declarations themselves. Drops every
     * line that starts with {@code import } and every blank line between or
     * after import lines, up to (and NOT including) the first non-blank,
     * non-import line. Handles multi-group import blocks correctly — e.g.
     * regular imports, blank, static imports, blank, class body — all
     * import lines and inter-group blanks are dropped. Safe on sources
     * without any import lines (returns the source unchanged).
     */
    private static String stripImportBlock(String source) {
        StringBuilder out = new StringBuilder();
        boolean inImportBlock = false;
        boolean pastImports = false;
        for (String line : source.lines().toList()) {
            if (!pastImports) {
                if (line.startsWith("import ")) {
                    inImportBlock = true;
                    continue;               // drop import line
                }
                if (inImportBlock && line.isBlank()) {
                    continue;               // drop blank between/after import groups
                }
                if (inImportBlock) {
                    // First non-import, non-blank line — imports are done.
                    pastImports = true;
                    // fall through to append this line
                }
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }

    /**
     * Split imports into blank-line-separated groups, preserving order.
     * The generator emits exactly 2 groups separated by one blank line:
     * (1) regular imports — all FQNs globally alphabetically sorted with
     * no cdm/com/javax prefix sub-grouping; (2) static imports — always
     * last. Returns FQNs only (leading {@code import } /
     * {@code import static } and trailing semicolon stripped).
     */
    private static List<List<String>> extractImportGroups(String source) {
        List<List<String>> groups = new ArrayList<>();
        List<String> current = new ArrayList<>();
        boolean inImportBlock = false;
        for (String line : source.lines().toList()) {
            if (line.startsWith("import ")) {
                inImportBlock = true;
                current.add(line.replaceAll("^import\\s+(static\\s+)?", "")
                                .replaceAll(";\\s*$", ""));
            } else if (inImportBlock && line.isBlank()) {
                if (!current.isEmpty()) {
                    groups.add(List.copyOf(current));
                    current.clear();
                }
            } else if (inImportBlock) {
                // End of import block (first non-import, non-blank line
                // after imports started).
                if (!current.isEmpty()) {
                    groups.add(List.copyOf(current));
                    current.clear();
                }
                break;
            }
        }
        if (!current.isEmpty()) {
            groups.add(List.copyOf(current));
        }
        return groups;
    }

    private static String requireGenerated(String path) {
        assertNotNull(functionOutput,
            "Function generation did not run — CDM corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        return generated.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static RLinkingResult loadFullCorpus() throws IOException {
        // Copilot round-13 findings: (a) Files.list() is non-recursive — CDM
        // rosetta files are nested under subdirectories; switched to
        // Files.walk(). (b) catch(Exception e) {/* skip */} silently discarded
        // parse failures — converted to collecting and fail-loud on any skip
        // so broken corpus files are surfaced instead of producing
        // non-deterministic partial-workspace runs.
        List<RModel> models = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.walk(BUILTINS_DIR)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) {
                              skipped.add(p + ": " + e.getMessage());
                          }
                      });
            }
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          RModel model = AstBuilder.buildFromFile(p);
                          model.setVersion("0.0.0.master-SNAPSHOT");
                          models.add(model);
                      } catch (Exception e) {
                          skipped.add(p + ": " + e.getMessage());
                      }
                  });
        }
        if (!skipped.isEmpty()) {
            throw new IOException(
                    "Corpus loader failed to parse " + skipped.size()
                            + " file(s); refusing to produce partial workspace: "
                            + String.join("; ", skipped));
        }
        return RWorkspace.build(models);
    }
}
