package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR #460 — facet caseGuardResolvedBinding (the plugin-composition wave; the #459
 * compile-gate finding). A type-switch NAME guard's Java type must come from the
 * linker's {@code resolvedGuard} (the import/alias-aware binding — the node the
 * engine's switch-case narrowing already consumes), NOT from a bare simple-name
 * re-resolution against workspace file order.
 *
 * <p><b>The carrier:</b> cdm6 {@code MapAsset} switches over {@code fpml.Asset} with
 * alias-qualified guards ({@code fpml.Loan} / {@code fpml.Commodity} through
 * {@code import fpml.consolidated.asset.* as fpml}) whose SIMPLE names collide with
 * the un-aliased earlier import {@code cdm.base.staticdata.asset.common.*}. Under the
 * maven-plugin's single flat sorted sourceRoot (upstream's own layout) the cdm.base
 * files register BEFORE the fpml payload, so the pre-#460
 * {@code ChoiceSwitchSupport.resolveCaseType} simple-name lottery
 * ({@code GeneratorModel.resolveTypeByName} — first match in file order) bound the
 * guards to {@code cdm.base…} — wrong imports, and the narrowed item's
 * {@code exchangeId} feature nav degraded to a bare unresolvable name (the CDM
 * consumer javac failure, {@code target-459-cdm-r2-compile1.log}). The D11 loader's
 * dependency-payload-FIRST order had masked the defect (fpml registered first there,
 * so the lottery happened to agree with the linker).
 *
 * <p>The two collision-order tests FAIL against the pre-#460 resolveCaseType (the
 * witness-uniqueness law — proven via the uncommitted-stash round); the fpml-only
 * control and the unresolved-guard fallback are stays-guards (the latter LABELED a
 * CONTRACT PIN: the linker resolves nothing for it pre- and post-fix, so both sides
 * take the simple-name path — it pins that the fallback SURVIVES the fix, not a
 * behavior flip).
 */
class CaseGuardResolvedBindingWaveTest {

    private static final Path BUILTINS_DIR =
            Path.of("../test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    static boolean builtinsAvailable() {
        return Files.isDirectory(BUILTINS_DIR);
    }

    /** The colliding cdm-side namespace — declares the SAME simple names as the fpml payload. */
    private static final String CDM_BASE = """
            namespace cdm.base.staticdata.asset.common
            type Asset:
                name string (0..1)
            type Loan:
                id string (0..1)
            type Commodity:
                id string (0..1)
            """;

    /** The fpml payload — the aliased-import target (subtypes of the switch subject). */
    private static final String FPML = """
            namespace fpml.consolidated.asset
            type Asset:
                desc string (0..1)
            type Loan extends Asset:
                exchangeId string (0..1)
            type Commodity extends Asset:
                productModel string (0..1)
            type Fund extends Asset:
                other string (0..1)
            """;

    /** The MapAsset carrier shape: alias-qualified guards + implicit-item feature args. */
    private static final String INGEST = """
            namespace cdm.ingest.test
            import cdm.base.staticdata.asset.common.*
            import fpml.consolidated.asset.* as fpml
            func ConvertLoan:
                inputs:
                    inLoan fpml.Loan (0..1)
                    exch string (0..1)
                output:
                    res string (0..1)
                set res: exch
            func ConvertCommodity:
                inputs:
                    inCommodity fpml.Commodity (0..1)
                output:
                    res string (0..1)
                set res: inCommodity -> productModel
            func ConvertFund:
                inputs:
                    inFund fpml.Fund (0..1)
                output:
                    res string (0..1)
                set res: inFund -> other
            func MapIt:
                inputs:
                    fpmlAsset fpml.Asset (0..1)
                output:
                    res string (0..1)
                set res:
                    fpmlAsset switch
                        fpml.Loan then ConvertLoan(item, exchangeId),
                        fpml.Commodity then ConvertCommodity(item),
                        fpml.Fund then ConvertFund(item),
                        default empty
            """;

    private static String generateMapIt(String... sourcesInRegistrationOrder) {
        List<RModel> models = new ArrayList<>();
        try (var stream = Files.walk(BUILTINS_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta")).sorted()
                    .forEach(p -> models.add(AstBuilder.buildFromFile(p)));
        } catch (IOException e) {
            throw new AssertionError("builtins walk failed: " + e.getMessage(), e);
        }
        int i = 0;
        for (String source : sourcesInRegistrationOrder) {
            models.add(AstBuilder.buildFromString(source, "case-guard-wave-" + (i++) + ".rosetta"));
        }
        var linkingResult = RWorkspace.build(models);
        var gm = new GeneratorModel(linkingResult.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = gen.generateWithErrors(output);
        assertTrue(errors.isEmpty(), "generation errors: " + errors);
        String mapIt = output.get("cdm/ingest/test/functions/MapIt.java");
        assertNotNull(mapIt, "MapIt.java not generated; keys: " + output.keySet());
        return mapIt;
    }

    // === The differential witnesses (FAIL pre-#460) ============================

    /**
     * The plugin composition order — the colliding cdm.base file registers FIRST
     * (the flat sorted sourceRoot: base-* &lt; consolidated-*). The alias-qualified
     * guards must still bind the IMPORTED fpml namespace, and the narrowed item's
     * {@code exchangeId} arg must render as the feature nav, not a bare name.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void pluginOrder_aliasQualifiedGuards_bindTheImportedNamespace() {
        String mapIt = generateMapIt(CDM_BASE, FPML, INGEST);
        assertTrue(mapIt.contains("import fpml.consolidated.asset.Loan;"),
                "the fpml.Loan guard must import the aliased-import target:\n" + mapIt);
        assertTrue(mapIt.contains("import fpml.consolidated.asset.Commodity;"),
                "the fpml.Commodity guard must import the aliased-import target:\n" + mapIt);
        assertFalse(mapIt.contains("cdm.base.staticdata.asset.common.Loan"),
                "the colliding first-registered cdm.base Loan must NOT bind:\n" + mapIt);
        assertFalse(mapIt.contains("cdm.base.staticdata.asset.common.Commodity"),
                "the colliding first-registered cdm.base Commodity must NOT bind:\n" + mapIt);
        assertTrue(mapIt.contains("getExchangeId"),
                "the narrowed item's exchangeId arg must render as the feature nav "
                        + "(pre-#460 it degraded to a bare unresolvable name):\n" + mapIt);
    }

    /**
     * Registration-order invariance: the D11 dependency-first order (fpml before the
     * colliding cdm.base file) and the plugin order must generate BYTE-IDENTICAL
     * output — pre-#460 the two orders disagreed on the guard binding.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void dependencyFirstOrder_generatesIdenticalBytes() {
        String pluginOrder = generateMapIt(CDM_BASE, FPML, INGEST);
        String dependencyFirst = generateMapIt(FPML, CDM_BASE, INGEST);
        assertEquals(dependencyFirst, pluginOrder,
                "the guard binding must not depend on workspace registration order");
    }

    // === The stays-guards ======================================================

    /** The fpml-only guard name (no collision) — correct under BOTH resolutions. */
    @Test
    @EnabledIf("builtinsAvailable")
    void fpmlOnlyGuard_staysBoundToTheImportedNamespace() {
        String mapIt = generateMapIt(CDM_BASE, FPML, INGEST);
        assertTrue(mapIt.contains("import fpml.consolidated.asset.Fund;"),
                "the collision-free fpml.Fund guard binds fpml under both resolutions:\n" + mapIt);
    }

    /**
     * CONTRACT PIN (not a differential witness): a guard the linker leaves UNRESOLVED
     * — written bare with no import able to reach either declaration — keeps the
     * pre-#460 simple-name fallback (first-registered wins). Pre- and post-fix both
     * take the fallback path here; the pin defends the fallback's survival.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void unresolvedGuard_keepsTheSimpleNameFallback() {
        String colliderA = """
                namespace colliders.a
                type Sub:
                    fieldA string (0..1)
                """;
        String colliderB = """
                namespace colliders.b
                type Sub:
                    fieldB string (0..1)
                """;
        String switcher = """
                namespace switcher.test
                type Base:
                    marker string (0..1)
                func MapBare:
                    inputs:
                        subject Base (0..1)
                    output:
                        res string (0..1)
                    set res:
                        subject switch
                            Sub then "hit",
                            default empty
                """;
        List<RModel> models = new ArrayList<>();
        try (var stream = Files.walk(BUILTINS_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta")).sorted()
                    .forEach(p -> models.add(AstBuilder.buildFromFile(p)));
        } catch (IOException e) {
            throw new AssertionError("builtins walk failed: " + e.getMessage(), e);
        }
        models.add(AstBuilder.buildFromString(colliderA, "colliders-a.rosetta"));
        models.add(AstBuilder.buildFromString(colliderB, "colliders-b.rosetta"));
        models.add(AstBuilder.buildFromString(switcher, "switcher.rosetta"));
        var linkingResult = RWorkspace.build(models);
        var gm = new GeneratorModel(linkingResult.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = gen.generateWithErrors(output);
        assertTrue(errors.isEmpty(), "generation errors: " + errors);
        String mapBare = output.get("switcher/test/functions/MapBare.java");
        assertNotNull(mapBare, "MapBare.java not generated; keys: " + output.keySet());
        assertTrue(mapBare.contains("import colliders.a.Sub;"),
                "an unresolved guard falls back to the first-registered simple-name match:\n"
                        + mapBare);
    }
}
