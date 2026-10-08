package com.regnosys.rosetta.generator.java.object;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * P2.1.3c β1 fix tests for {@link ChoiceObjectGenerator}.
 *
 * <p>(1) Byte-match check against the latest available cdm corpus's
 * {@code Asset.java} golden — the ground-truth integration target for the β1
 * fix. R4 F4-10: the cdm version is resolved dynamically from the newest
 * {@code cdm-*} child of {@code ../test-corpus/cdm/} rather than hardcoded,
 * so the byte-match tracks the active pin (currently cdm-6.18+). Skipped when
 * no cdm-* directory is present locally (mirrors {@code D11GoldenComparisonTest}).
 *
 * <p>(2) Shape-only check against a synthetic 2-option choice source — robust
 * to cdm-version bumps and runnable without the cdm corpus.
 */
class ChoiceObjectGeneratorTest {

    private static final Path TEST_CORPUS_ROOT = Path.of("../test-corpus");

    /**
     * R4 F4-10: dynamic resolution of the newest {@code cdm-*} directory under
     * {@code ../test-corpus/cdm/}. Hardcoding {@code cdm-6.18.0} silently lapsed
     * when the team bumped the cdm pin; this resolver finds the lexically-greatest
     * {@code cdm-*} child (which corresponds to the newest version for SemVer-pinned
     * directories) so the byte-match tests track the active pin. Returns
     * {@code null} when no {@code cdm-*} directory exists locally.
     */
    private static Path resolveLatestCdmDir() {
        Path cdmRoot = TEST_CORPUS_ROOT.resolve("cdm");
        if (!Files.isDirectory(cdmRoot)) return null;
        try (Stream<Path> stream = Files.list(cdmRoot)) {
            return stream.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("cdm-"))
                    .max(Comparator.comparing(p -> p.getFileName().toString()))
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static final Path CDM_BASE_DIR = resolveLatestCdmDir();
    private static final Path CDM_ROSETTA_DIR = CDM_BASE_DIR == null
            ? null
            : CDM_BASE_DIR.resolve("rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR = CDM_BASE_DIR == null
            ? null
            : CDM_BASE_DIR.resolve("rosetta-source/src/generated/java");

    /**
     * R4 F4-8: builtins search roots — mirrors {@code D11CorpusRegressionTest}'s
     * {@code BUILTINS_SEARCH_ROOTS} so the choice tests stay resilient to environments
     * that have the sibling {@code rune-dsl} clone but no full {@code test-corpus}
     * mirror. Without this fallback, {@code loadBuiltinsOnly()} would silently return
     * an empty list and the synthetic-source tests would fail to resolve {@code
     * string} / {@code int} primitive types.
     */
    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            TEST_CORPUS_ROOT.resolve("rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            Path.of("../rune-dsl/rune-runtime/src/main/resources/model")
    );

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);

    static boolean cdmAvailable() {
        return CDM_ROSETTA_DIR != null && CDM_GOLDEN_DIR != null
                && Files.isDirectory(CDM_ROSETTA_DIR) && Files.isDirectory(CDM_GOLDEN_DIR);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void choice_pojo_emits_bytematch_against_latest_cdm_Asset() throws IOException {
        // Load the latest cdm corpus (cdm/6.20.6 post-PR #85; resolved
        // dynamically by resolveLatestCdmDir()). The Asset choice references
        // Cash / Commodity / DigitalAsset / Instrument — all live in the same corpus.
        // Retain RLinkingResult to prevent GC of the WeakReference-held
        // workspace mid-test (mirrors GeneratorModelTest fence pattern).
        List<RModel> models = loadCdmCorpus();
        RLinkingResult linkingResult = RWorkspace.build(models);
        var workspace = linkingResult.workspace();
        var gm = new GeneratorModel(workspace);

        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);

        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : workspace.files()) {
            if (gm.shouldGenerate(model)) {
                assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output));
            }
        }

        String key = "cdm/base/staticdata/asset/common/Asset.java";
        String generated = output.get(key);
        assertNotNull(generated,
                "ChoiceObjectGenerator must emit " + key + "; outputs: " + output.keySet().size());

        Path goldenPath = CDM_GOLDEN_DIR.resolve(key);
        assertTrue(Files.exists(goldenPath), "Golden file must exist at " + goldenPath);
        String golden = normalize(Files.readString(goldenPath));
        String got = normalize(generated);

        if (!golden.equals(got)) {
            // Surface the first divergent line for fast triage on byte-mismatch.
            String[] gl = golden.split("\n", -1);
            String[] ge = got.split("\n", -1);
            int max = Math.max(gl.length, ge.length);
            StringBuilder diff = new StringBuilder();
            int shown = 0;
            for (int i = 0; i < max && shown < 5; i++) {
                String a = i < gl.length ? gl[i] : "<EOF>";
                String b = i < ge.length ? ge[i] : "<EOF>";
                if (!a.equals(b)) {
                    diff.append("L").append(i + 1).append(" GOLD: |").append(a).append("|\n");
                    diff.append("L").append(i + 1).append(" GEN : |").append(b).append("|\n");
                    shown++;
                }
            }
            assertEquals(golden, got, "Asset.java byte-mismatch; first diffs:\n" + diff);
        }

        Reference.reachabilityFence(linkingResult);
    }

    /**
     * P2.1.3c T2 (cont): byte-match the {@code cdm.product.template.Payout}
     * choice POJO golden — exercises the type-level {@code [metadata key]}
     * meta-annotation path on a top-level choice (synthetic {@code meta}
     * property + {@code GlobalKey} interface + {@code @RuneMetaType}). R4
     * F4-10: dynamically resolves the latest available cdm corpus.
     */
    @Test
    @EnabledIf("cdmAvailable")
    void choice_pojo_emits_bytematch_against_latest_cdm_Payout_metadata_key() throws IOException {
        List<RModel> models = loadCdmCorpus();
        RLinkingResult linkingResult = RWorkspace.build(models);
        var workspace = linkingResult.workspace();
        var gm = new GeneratorModel(workspace);

        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);

        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : workspace.files()) {
            if (gm.shouldGenerate(model)) {
                assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output));
            }
        }

        String key = "cdm/product/template/Payout.java";
        String generated = output.get(key);
        assertNotNull(generated,
                "ChoiceObjectGenerator must emit " + key + " (Payout has [metadata key])");

        Path goldenPath = CDM_GOLDEN_DIR.resolve(key);
        assertTrue(Files.exists(goldenPath), "Golden file must exist at " + goldenPath);
        String golden = normalize(Files.readString(goldenPath));
        String got = normalize(generated);
        assertEquals(golden, got, "Payout.java byte-mismatch (meta-key path)");

        Reference.reachabilityFence(linkingResult);
    }

    /**
     * P2.1.3c T2 (cont): shape-only check for option-level
     * {@code [metadata location]} / {@code [metadata address]} propagation
     * through {@link ChoiceObjectGenerator}. Robust to cdm-version bumps.
     */
    @Test
    void choice_pojo_propagates_option_level_meta_annotations() throws IOException {
        // Synthetic source: a choice whose options carry [metadata location] +
        // [metadata address] annotations. These should produce FieldWithMeta-wrapped
        // and ReferenceWithMeta-wrapped getter return types respectively, plus
        // @RuneScopedAttributeKey / @RuneScopedAttributeReference decorations.
        String source = String.join("\n",
                "namespace com.example.test : <\"Test namespace\">",
                "version \"0.0.0.test\"",
                "",
                "type LeafA: <\"Leaf A\">",
                "    a string (1..1)",
                "",
                "type LeafB: <\"Leaf B\">",
                "    b string (1..1)",
                "",
                "type LeafC: <\"Leaf C\">",
                "    c string (1..1)",
                "",
                "choice ChoiceWithMeta: <\"Choice with per-option meta annotations\">",
                "    LeafA <\"Option A with location\">",
                "        [metadata location]",
                "    LeafB <\"Option B with address\">",
                "        [metadata address]",
                "    LeafC <\"Option C plain\">",
                "");

        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        model.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        // R4 F4-11: workspace local omitted — synthetic-source tests use
        // gm.version(model) and pojoGen.generateClasses(model, ...) directly,
        // not workspace.files(). RLinkingResult retained for reachabilityFence.
        RLinkingResult linkingResult = RWorkspace.build(models);
        var gm = new GeneratorModel(linkingResult.workspace());

        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);

        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output));
        assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output));

        String generated = output.get("com/example/test/ChoiceWithMeta.java");
        assertNotNull(generated, "ChoiceWithMeta.java must be emitted; outputs: " + output.keySet());

        // [metadata location] → FieldWithMeta wrapping on the LeafA option getter.
        assertTrue(generated.contains("FieldWithMetaLeafA getLeafA();"),
                "LeafA option ([metadata location]) should be wrapped in FieldWithMetaLeafA");
        assertTrue(generated.contains("@RuneScopedAttributeKey"),
                "[metadata location] should emit @RuneScopedAttributeKey");
        // [metadata address] → ReferenceWithMeta wrapping on the LeafB option getter.
        assertTrue(generated.contains("ReferenceWithMetaLeafB getLeafB();"),
                "LeafB option ([metadata address]) should be wrapped in ReferenceWithMetaLeafB");
        assertTrue(generated.contains("@RuneScopedAttributeReference"),
                "[metadata address] should emit @RuneScopedAttributeReference");
        // Plain option (no meta) retains the raw type.
        assertTrue(generated.contains("LeafC getLeafC();"),
                "LeafC option (no meta) should not be wrapped");

        Reference.reachabilityFence(linkingResult);
    }

    /**
     * PR #85 rebaseline (9.83.0 alignment): choice-type POJOs must emit
     * {@code @RuneChoiceType}; data-type POJOs must NOT. Locks the {@code type == null}
     * discriminator in {@link ModelObjectGenerator#buildBody}. Golden-independent
     * (inline source) — runs regardless of which cells are locally present.
     */
    @Test
    void choice_pojo_emits_RuneChoiceType_and_data_type_does_not() throws IOException {
        String source = String.join("\n",
                "namespace com.example.test : <\"Test namespace\">",
                "version \"0.0.0.test\"",
                "",
                "type LeafA: <\"Leaf A\">",
                "    a string (1..1)",
                "",
                "type LeafB: <\"Leaf B\">",
                "    b string (1..1)",
                "",
                "choice ChoiceForRuneChoiceTypeTest: <\"Choice for @RuneChoiceType emit test\">",
                "    LeafA <\"Option A\">",
                "    LeafB <\"Option B\">",
                "");

        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        model.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linkingResult = RWorkspace.build(models);
        var gm = new GeneratorModel(linkingResult.workspace());

        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);

        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output));
        assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output));

        String choiceSrc = output.get("com/example/test/ChoiceForRuneChoiceTypeTest.java");
        String dataSrc = output.get("com/example/test/LeafA.java");
        assertNotNull(choiceSrc, "ChoiceForRuneChoiceTypeTest.java must be emitted; outputs: " + output.keySet());
        assertNotNull(dataSrc, "LeafA.java must be emitted; outputs: " + output.keySet());

        // Positive: choice POJO has @RuneChoiceType + import.
        assertTrue(choiceSrc.contains("@RuneChoiceType"),
                "choice POJO must carry @RuneChoiceType (9.83.0 alignment)");
        assertTrue(choiceSrc.contains("import com.rosetta.model.lib.annotations.RuneChoiceType;"),
                "choice POJO must import RuneChoiceType");

        // Negative: data-type POJO must NOT carry @RuneChoiceType (locks the type == null guard).
        assertFalse(dataSrc.contains("@RuneChoiceType"),
                "data-type POJO (RDataType path) must NOT carry @RuneChoiceType");
        assertFalse(dataSrc.contains("import com.rosetta.model.lib.annotations.RuneChoiceType;"),
                "data-type POJO must NOT import RuneChoiceType");

        Reference.reachabilityFence(linkingResult);
    }

    /**
     * P2.1.3c T2 (cont): shape-only check for type-level {@code [metadata key]}
     * on a choice (synthetic {@code meta} property + {@code GlobalKey}
     * interface inheritance). Robust to cdm-version bumps.
     */
    @Test
    void choice_pojo_propagates_type_level_metadata_key() throws IOException {
        // Synthetic source: a choice with type-level [metadata key] annotation.
        // Should produce: extends RosettaModelObject, GlobalKey + synthetic
        // `MetaFields getMeta();` accessor + @RuneMetaType decoration.
        String source = String.join("\n",
                "namespace com.example.test : <\"Test namespace\">",
                "version \"0.0.0.test\"",
                "",
                "type LeafX: <\"Leaf X\">",
                "    x string (1..1)",
                "",
                "type LeafY: <\"Leaf Y\">",
                "    y string (1..1)",
                "",
                "choice KeyedChoice: <\"Choice with type-level metadata key\">",
                "    [metadata key]",
                "    LeafX",
                "    LeafY",
                "");

        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        model.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        // R4 F4-11: workspace local omitted (see ChoiceWithMeta test for rationale).
        RLinkingResult linkingResult = RWorkspace.build(models);
        var gm = new GeneratorModel(linkingResult.workspace());

        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);

        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output));
        assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output));

        String generated = output.get("com/example/test/KeyedChoice.java");
        assertNotNull(generated, "KeyedChoice.java must be emitted; outputs: " + output.keySet());

        // Type-level [metadata key] → GlobalKey interface inheritance + meta property.
        assertTrue(generated.contains("extends RosettaModelObject, GlobalKey"),
                "Type-level [metadata key] should add GlobalKey to the extends clause");
        assertTrue(generated.contains("MetaFields getMeta();"),
                "Type-level [metadata key] should add the synthetic meta property");
        assertTrue(generated.contains("@RuneMetaType"),
                "Synthetic meta property should be decorated with @RuneMetaType");

        Reference.reachabilityFence(linkingResult);
    }

    @Test
    void choice_pojo_emits_correct_shape_robust_to_cdm_bump() throws IOException {
        // Synthetic source: a 2-option choice over two single-option choices. Provides
        // model-object option types (RDataType resolved within the same workspace).
        String source = String.join("\n",
                "namespace com.example.test : <\"Test namespace\">",
                "version \"0.0.0.test\"",
                "",
                "type Cash: <\"Cash leaf\">",
                "    amount string (1..1)",
                "",
                "type Commodity: <\"Commodity leaf\">",
                "    symbol string (1..1)",
                "",
                "choice Asset: <\"Test choice\">",
                "    Cash <\"Cash option\">",
                "    Commodity <\"Commodity option\">",
                "");

        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        model.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        // R4 F4-11: workspace local omitted (see ChoiceWithMeta test for rationale).
        RLinkingResult linkingResult = RWorkspace.build(models);
        var gm = new GeneratorModel(linkingResult.workspace());

        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);

        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output));
        assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output));

        String key = "com/example/test/Asset.java";
        String generated = output.get(key);
        assertNotNull(generated, "Choice POJO Asset.java must be emitted; outputs: " + output.keySet());

        // Shape-only assertions — these are invariant across cdm-version bumps:
        assertTrue(generated.contains("public interface Asset extends RosettaModelObject"),
                "Interface header should declare RosettaModelObject base");
        assertTrue(generated.contains("Cash getCash();"),
                "Variant getter for Cash option should be emitted");
        assertTrue(generated.contains("Commodity getCommodity();"),
                "Variant getter for Commodity option should be emitted");
        assertTrue(generated.contains("static Asset.AssetBuilder builder()"),
                "Static builder() hook should be emitted");
        assertTrue(generated.contains("class AssetBuilderImpl implements Asset.AssetBuilder"),
                "BuilderImpl class should be emitted");
        assertTrue(generated.contains("class AssetImpl implements Asset"),
                "Impl class should be emitted");

        Reference.reachabilityFence(linkingResult);
    }

    // ------------------------------------------------------------------------

    private List<RModel> loadCdmCorpus() throws IOException {
        List<RModel> models = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        models.addAll(loadBuiltinsOnly(failures));
        if (CDM_ROSETTA_DIR == null) {
            throw new AssertionError(
                    "loadCdmCorpus invoked but no cdm-* directory found under "
                    + TEST_CORPUS_ROOT.resolve("cdm") + " — cdmAvailable() should "
                    + "have gated this call.");
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          RModel m = AstBuilder.buildFromFile(p);
                          m.setVersion("0.0.0.master-SNAPSHOT");
                          models.add(m);
                      } catch (Exception e) {
                          // R4 F4-9: collect rather than swallow; aggregate-throw
                          // below so first-fail and total-fail surface together.
                          failures.add(p + " — " + e);
                      }
                  });
        }
        if (!failures.isEmpty()) {
            throw new AssertionError(
                    "[ChoiceObjectGeneratorTest] loadCdmCorpus: " + failures.size()
                    + " parse failure(s) — aborting cdm-byte-match suite. "
                    + "First failure: " + failures.get(0)
                    + (failures.size() > 1
                        ? " (and " + (failures.size() - 1) + " more — full list: "
                          + String.join("; ", failures.subList(1, failures.size())) + ")"
                        : ""));
        }
        return models;
    }

    private List<RModel> loadBuiltinsOnly() throws IOException {
        List<String> failures = new ArrayList<>();
        List<RModel> models = loadBuiltinsOnly(failures);
        if (!failures.isEmpty()) {
            throw new AssertionError(
                    "[ChoiceObjectGeneratorTest] loadBuiltinsOnly: " + failures.size()
                    + " parse failure(s) — first: " + failures.get(0));
        }
        return models;
    }

    /**
     * R4 F4-8: resolves builtin {@code .rosetta} files via union across
     * {@link #BUILTINS_SEARCH_ROOTS} — mirrors {@code D11CorpusRegressionTest#resolveBuiltinFiles}.
     * Dedup'd by filename in priority order (test-corpus root wins for filename
     * collisions; sibling rune-dsl root fills any gaps). Parse failures are
     * collected into the caller-provided {@code failures} list rather than swallowed,
     * so the caller can surface them via an aggregate AssertionError per R4 F4-9.
     */
    private List<RModel> loadBuiltinsOnly(List<String> failures) throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) {
                        failures.add(p + " — " + e);
                    }
                });
        return models;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
