package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR #211 facet enumQualifyInherited (2 cdm6 FUNCTION flips), whole-file byte anchors through the
 * REAL D11 loader. Each anchor reverts RED if the fix is removed; the law is green-safe by
 * construction (the pre-fix bare unresolved-enum splice never compiled, so every carrier was
 * already waivered; the stash-baseline confirmed 0 now-matching pristine and
 * {@code function_comparison} stays green).
 *
 * <p><b>The law.</b> A bare unresolved enum value used as a ctor-setter argument
 * ({@code identifierType: Other}) qualifies to {@code <Enum>.<CONSTANT>} — the PR #209
 * ctorSetterEnumQualify mechanism. PR #209 searched only the attribute enum's DIRECT values
 * ({@code en.values()}) and so missed a value declared on a SUPER-enum: {@code AssetIdTypeEnum
 * extends ProductIdTypeEnum}, where {@code Other} / {@code Name} are inherited. The generated Java
 * enum FLATTENS inherited values under the child name, so golden qualifies
 * {@code AssetIdTypeEnum.OTHER} (the attribute's enum, NOT the declaring parent
 * {@code ProductIdTypeEnum}). Fix: {@code ConstructionHandler.findEnumValueInHierarchy} walks the
 * {@code extends} chain via {@code REnumeration.superType()} while keeping {@code en.name()} as the
 * qualifier. Carriers: {@code MapFloatingRateIndex} ({@code Other}) and
 * {@code MapIndexNameToAssetIdentifier} ({@code Name}).
 */
class FunctionPr211EnumQualifyInheritedTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm6Available() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm6Available()) {
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(), cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    /** MapFloatingRateIndex: identifierType: Other → AssetIdTypeEnum.OTHER (inherited from ProductIdTypeEnum). */
    @Test
    @EnabledIf("cdm6Available")
    void mapFloatingRateIndex_inheritedOtherQualify_byteMatchesGolden() throws IOException {
        assertByteMatches("cdm/ingest/fpml/confirmation/pricequantity/functions/MapFloatingRateIndex.java",
                "the .setIdentifierType(AssetIdTypeEnum.OTHER) qualification + import revert to bare `Other` "
                + "— findEnumValueInHierarchy stopped walking the extends chain");
    }

    /** MapIndexNameToAssetIdentifier: identifierType: Name → AssetIdTypeEnum.NAME (also inherited). */
    @Test
    @EnabledIf("cdm6Available")
    void mapIndexNameToAssetIdentifier_inheritedNameQualify_byteMatchesGolden() throws IOException {
        assertByteMatches("cdm/ingest/fpml/confirmation/pricequantity/functions/MapIndexNameToAssetIdentifier.java",
                "the .setIdentifierType(AssetIdTypeEnum.NAME) qualification + import revert to bare `Name`");
    }

    private static void assertByteMatches(String path, String revertHint) throws IOException {
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — " + revertHint + ".");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
