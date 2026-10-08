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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engine PR (facet {@code enum_set_result_qualify}) — a PARSER-ONLY fix that resolves a bare
 * enum value written as a conditional RESULT of an enum-typed function-output assignment
 * ({@code set result: if .. then ENDU else if .. then AFFL ..}) to its qualified Java constant
 * ({@code ClearingExceptionsAndExemptionsEnum.ENDU}), matching upstream 9.83.0.
 *
 * <p>On current {@code main} the fork emits the BARE unresolved value name
 * ({@code result = ENDU;} / {@code notation = Basis;}), which does not compile — the
 * grammar parses each branch as a single-identifier {@link
 * com.regnosys.rosetta.ast.expressions.references.RSymbolReference} with no expected type, and
 * lexical/global resolution leaves it unbound (enum VALUES are not in function/file scope).
 *
 * <p>Root cause: {@code TypeInferenceEngine} Category 13 ({@code expectedConditionalEnum})
 * only seeds the expected enum from a SIBLING branch that already types as an enum. When ALL
 * branches are bare (this shape), no branch self-seeds, so Cat 13 never fires. The fix adds an
 * output-enum fallback ({@code enclosingFunctionOutputEnumForDirectConditional}) — a near-copy
 * of the merged Cat 15 / PR #149 {@code enclosingFunctionOutputEnumForDirectSwitch} — that
 * seeds the expected enum from the assignment-target OUTPUT type when the conditional is the
 * direct body of that output's {@code set}, then reuses the existing
 * {@code resolveBareEnumBranches}/{@code bindBareEnumValue} workers. The renderer is already
 * upstream-correct ({@code ReferenceHandler.handle(RSymbolReference)} emits {@code E.CONSTANT}
 * for a resolved {@code REnumValue}); ZERO renderer change.
 *
 * <p>This test is REVERT-VERIFIED RED against the PARSER half: revert the Cat-13b output-enum
 * fallback → the bare branches stay unresolved → {@code result = ENDU;} / {@code notation =
 * Basis;} reappear (the negative assertions fail) and the qualified-constant positive
 * assertions fail.
 *
 * <p>Determinism follows {@link FunctionMapEnumSwitchTest}: the corpus is the version-frozen
 * {@code drr/6.34.1} cell (+ its transitive CDM 5.38.0 resolution closure), so the generator
 * output is a pure function of the fork's code — this test flips only when the fork changes.
 *
 * <p>Anchors (exact golden strings, materialised from
 * {@code test-corpus/drr/drr-6.34.1/.../generated/java}):
 * <ul>
 *   <li>{@code ClearingExceptionsAndExemptions} — flat if/else-if chain, output {@code result
 *       ClearingExceptionsAndExemptionsEnum}; six bare results ENDU/AFFL/SMBK/COOP/NOAL/OTHR.</li>
 *   <li>{@code FloatingReferencePeriod} — flat if/else-if chain, output {@code result
 *       FrequencyPeriodEnum}; bare results DAIL/WEEK/MNTH/YEAR.</li>
 *   <li>{@code GetPriceNotation} — NESTED conditional (exercises {@code resolveEnumBranch}
 *       recursion), output {@code notation PriceNotationEnum}; bare results Basis/.../Monetary,
 *       where value-name {@code Basis} ≠ constant {@code BASIS} (locks the
 *       {@code EnumHelper.convertValue} SCREAMING_SNAKE derivation).</li>
 * </ul>
 */
class FunctionSetEnumConditionalTest {

    private static final Path DRR_ROSETTA_DIR =
            Path.of("../test-corpus/drr/drr-6.34.1/rosetta-source/src/main/rosetta");
    private static final Path DRR_GOLDEN_DIR =
            Path.of("../test-corpus/drr/drr-6.34.1/rosetta-source/src/generated/java");
    // DRR 6.34.1 bundles CDM 5.38.0 transitively (DRR_TO_CDM_VERSION in D11CorpusRegressionTest);
    // the CDM .rosetta closure is loaded resolution-only (excluded from emission by the filter).
    private static final Path CDM_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-5.38.0/rosetta-source/src/main/rosetta");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    private static Map<String, String> functionOutput;

    static boolean drrAvailable() {
        if (!Files.isDirectory(DRR_ROSETTA_DIR) || !Files.isDirectory(DRR_GOLDEN_DIR)
                || !Files.isDirectory(CDM_DEP_ROSETTA_DIR)) {
            return false;
        }
        try (var stream = Files.walk(DRR_ROSETTA_DIR)) {
            return stream.anyMatch(p -> p.toString().endsWith(".rosetta"));
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeAll
    static void generateDrrFunctions() throws IOException {
        if (!drrAvailable()) return;
        var corpus = loadDrrCorpus();
        // DRR emission filter — mirrors D11CorpusRegressionTest.emissionFilter("drr"): only
        // drr.* namespaces emit Java; the transitive-CDM closure stays resolution-only.
        var gm = new GeneratorModel(corpus.workspace(),
                model -> model.namespace() != null && model.namespace().startsWith("drr."));
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        functionOutput = new LinkedHashMap<>();
        gen.generate(functionOutput);
    }

    /**
     * Flat if/else-if chain, all-bare results, enum output. Each bare result resolves to the
     * qualified output-enum constant.
     */
    @Test
    @EnabledIf("drrAvailable")
    void clearingExceptions_allBareConditionalResults_resolveToOutputEnumConstant() {
        String body = requireGenerated(
                "drr/regulation/common/functions/ClearingExceptionsAndExemptions.java");

        assertTrue(body.contains("result = ClearingExceptionsAndExemptionsEnum.ENDU;"),
                "the bare conditional result `ENDU` must resolve to the output-enum constant "
                + "ClearingExceptionsAndExemptionsEnum.ENDU (parser Cat-13b output-enum seed). "
                + "Body:\n" + body);
        assertTrue(body.contains("result = ClearingExceptionsAndExemptionsEnum.AFFL;")
                        && body.contains("result = ClearingExceptionsAndExemptionsEnum.OTHR;"),
                "every bare result in the chain (AFFL..OTHR) must resolve to the output-enum "
                + "constant. Body:\n" + body);
        assertFalse(body.contains("result = ENDU;"),
                "the BARE unresolved result `result = ENDU;` must be gone — it reappears if the "
                + "Cat-13b output-enum fallback is reverted. Body:\n" + body);
    }

    /** Flat if/else-if chain, distinct enum (FrequencyPeriodEnum). */
    @Test
    @EnabledIf("drrAvailable")
    void floatingReferencePeriod_allBareConditionalResults_resolveToOutputEnumConstant() {
        String body = requireGenerated(
                "drr/regulation/common/functions/FloatingReferencePeriod.java");

        assertTrue(body.contains("result = FrequencyPeriodEnum.DAIL;")
                        && body.contains("result = FrequencyPeriodEnum.YEAR;"),
                "bare results DAIL..YEAR must resolve to FrequencyPeriodEnum constants. "
                + "Body:\n" + body);
        assertFalse(body.contains("result = DAIL;"),
                "the BARE unresolved result `result = DAIL;` must be gone (reverts with the "
                + "Cat-13b fallback). Body:\n" + body);
    }

    /**
     * NESTED conditional — exercises the {@code resolveEnumBranch} recursion through the inner
     * if/then/else whose own branches are all bare and cannot self-seed. Also locks the
     * value-name≠constant derivation ({@code Basis} → {@code BASIS}).
     */
    @Test
    @EnabledIf("drrAvailable")
    void getPriceNotation_nestedBareConditional_resolveToOutputEnumConstant() {
        String body = requireGenerated(
                "drr/regulation/common/functions/GetPriceNotation.java");

        assertTrue(body.contains("notation = PriceNotationEnum.BASIS;"),
                "the bare nested-conditional result `Basis` must resolve to "
                + "PriceNotationEnum.BASIS (value-name Basis → constant BASIS via "
                + "EnumHelper.convertValue). Body:\n" + body);
        assertTrue(body.contains("notation = PriceNotationEnum.MONETARY;")
                        && body.contains("notation = PriceNotationEnum.DECIMAL;"),
                "every bare nested result (MONETARY/DECIMAL/...) must resolve to the output-enum "
                + "constant. Body:\n" + body);
        assertFalse(body.contains("notation = Basis;"),
                "the BARE unresolved result `notation = Basis;` must be gone (reverts with the "
                + "Cat-13b fallback). Body:\n" + body);
    }

    // =========================================================================
    // Helpers (mirror FunctionMapEnumSwitchTest + D11CorpusRegressionTest DRR loading)
    // =========================================================================

    private static String requireGenerated(String path) {
        assertNotNull(functionOutput,
                "Function generation did not run — DRR corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        return generated.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static RLinkingResult loadDrrCorpus() throws IOException {
        List<RModel> models = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        // 1. builtins.
        addRosetta(BUILTINS_DIR, models, skipped, null);
        // 2. transitive-CDM 5.38.0 closure — resolution-only (NOT version-stamped; excluded
        //    from emission by the drr namespace filter). Mirrors D11 loadCellCorpus DRR branch.
        addRosetta(CDM_DEP_ROSETTA_DIR, models, skipped, null);
        // 3. DRR cell source — version-stamped like D11 (CellSpec.versionStamp default).
        addRosetta(DRR_ROSETTA_DIR, models, skipped, "0.0.0.master-SNAPSHOT");
        if (!skipped.isEmpty()) {
            throw new IOException(
                    "Corpus loader failed to parse " + skipped.size()
                            + " file(s); refusing to produce partial workspace: "
                            + String.join("; ", skipped));
        }
        return RWorkspace.build(models);
    }

    private static void addRosetta(Path dir, List<RModel> models, List<String> skipped,
            String versionStamp) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          RModel model = AstBuilder.buildFromFile(p);
                          if (versionStamp != null) {
                              model.setVersion(versionStamp);
                          }
                          models.add(model);
                      } catch (Exception e) {
                          skipped.add(p + ": " + e.getMessage());
                      }
                  });
        }
    }
}
