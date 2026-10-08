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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Facet {@code dispatchWrapperRendering} (PR #229) — the dispatch wrapper file
 * (the routing class emitted by {@code FunctionGenerator.generateDispatchFunction}
 * via {@code templates/java-function-dispatch.stg}) carried three rendering bugs
 * relative to the golden, all confined to the dispatch path (no standard function
 * uses this template, and no dispatch function is green, so the fixes carry zero
 * regression risk):
 * <ul>
 *   <li><b>javadoc leading-space</b> — the class-level descriptive javadoc
 *       continuation lines were written with a literal leading space ({@code  * def}),
 *       which {@link org.stringtemplate.v4.NoIndentWriter} strips (it treats the
 *       leading whitespace as an INDENT instruction), emitting {@code * def} instead
 *       of golden's {@code  * def}. The leading space is now emitted via a
 *       {@code <" ">} string-literal expression that survives NoIndentWriter (the
 *       same reason the standard template's {@code <\t>} tab survives).</li>
 *   <li><b>trailing blank lines</b> — the inline {@code <m.dispatchVariants:&#123;v | …\n&#125;>}
 *       iterations embedded a trailing newline in the body AND were followed by a
 *       newline-terminated {@code &#125;>} line, emitting an extra empty line after the
 *       last {@code @Inject} field and after the last dispatch {@code case}. The
 *       iterations now use {@code separator="\n"} so no trailing newline is added.</li>
 *   <li><b>enum-constant casing</b> — a dispatch variant class name + the routing
 *       {@code case} label used the raw Rosetta enum value name ({@code Screen}) rather
 *       than the Java enum CONSTANT name ({@code SCREEN}). The variant class name and
 *       case label now use {@code EnumHelper.formatEnumName(stripEscape(valueName))};
 *       the {@code @Inject} field name keeps the raw value name (golden
 *       {@code processFloatingRateResetScreen}). For an already-constant value
 *       ({@code ACT_360}) {@code formatEnumName} is a no-op, so DayCountBasis is
 *       unaffected by the casing change.</li>
 * </ul>
 *
 * <p>DayCountBasis is sole on the two FORMATTING bugs (its enum values are already
 * constant-shaped), so this facet flips it byte-exact. The other three dispatch
 * functions (ComputeCalculationPeriod / ProcessFloatingRateReset / YearFraction)
 * FLIPPED at PR #369 (facet dispatchVariantParamResolution): the
 * {@code __synthesized_input__} enclosing-resolution gap this javadoc had deferred
 * is closed — variant-body params resolve against the dispatch BASE
 * ({@code HandlerHelper.dispatchBaseOf}), the library functions render the runtime
 * {@code new Min()/Max()/IsLeapYear().execute(…)} forms, and the remaining template
 * geometry (the anonymous-subtemplate {@code \{} escapes + the trailing-newline
 * blanks) matches golden. All four dispatch functions are now byte-identical in
 * both cdm cells (the #369 byte-locks live in DispatchVariantResolutionComposeTest).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} — reverting the dispatch
 * template / generator changes restores the {@code * def} javadoc + extra blank lines
 * and turns these RED.
 */
class FunctionDispatchWrapperRenderingTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(),
                cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    /**
     * DayCountBasis (cdm5) — a dispatch function sole on the two formatting bugs
     * (javadoc leading-space + trailing blank lines); its enum values are already
     * constant-shaped so the casing change is a no-op.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void dayCountBasisCdm5_dispatchWrapper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/datetime/daycount/functions/DayCountBasis.java");
    }

    /** DayCountBasis (cdm6) — the cdm6 twin. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void dayCountBasisCdm6_dispatchWrapper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/datetime/daycount/functions/DayCountBasis.java");
    }

    /**
     * ProcessFloatingRateReset (cdm5) — a NON-flipping lock on the enum-constant
     * casing fix (#3), which is a byte-no-op for DayCountBasis (its enum values
     * are already constant — {@code ACT_360} etc.). ProcessFloatingRateReset's
     * dispatch enum values are PascalCase ({@code Screen} / {@code Modular} /
     * {@code OvernightAvg} / {@code CompoundIndex}), so the casing fix is
     * observable here: the dispatch variant CLASS name + {@code @ImplementedBy} +
     * routing {@code case} label use the Java enum CONSTANT
     * ({@code …SCREEN} / {@code case SCREEN:} / {@code case OVERNIGHT_AVG:}), while
     * the {@code @Inject} FIELD name keeps the raw (PascalCase-derived) value name
     * ({@code processFloatingRateResetScreen}). The file FLIPPED byte-identical at
     * PR #369 (the dispatchVariantParamResolution facet closed the
     * {@code __synthesized_input__} gap this lock's javadoc had deferred); the
     * SUBSTRING lock stays as the focused casing witness — reverting the
     * {@code FunctionGenerator} {@code EnumHelper.formatEnumName} casing change
     * still turns it RED independently of the #369 whole-file byte anchors.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void processFloatingRateReset_enumConstantCasing_locksCasingFix() {
        assertNotNull(cdm5FunctionOutput, "Function generation did not run — corpus unavailable?");
        String gen = cdm5FunctionOutput.get(
                "cdm/product/asset/floatingrate/functions/ProcessFloatingRateReset.java");
        assertNotNull(gen, "ProcessFloatingRateReset.java not generated");
        // CLASS name + routing case label use the Java enum CONSTANT name.
        assertTrue(gen.contains("public static abstract class ProcessFloatingRateResetSCREEN "),
                "dispatch variant class must use the enum CONSTANT name (SCREEN), not the raw value (Screen)");
        assertTrue(gen.contains("case SCREEN:"),
                "routing case label must use the enum CONSTANT name (SCREEN)");
        assertTrue(gen.contains("case OVERNIGHT_AVG:"),
                "a multi-word enum value must format to SCREAMING_SNAKE (OvernightAvg -> OVERNIGHT_AVG)");
        // @Inject FIELD name keeps the raw (PascalCase-derived) value name.
        assertTrue(gen.contains("processFloatingRateResetScreen;"),
                "@Inject field name must keep the raw value name (lowerCamel of the PascalCase value)");
        // Negative: the raw-value variant class name must NOT be emitted.
        assertFalse(gen.contains("public static abstract class ProcessFloatingRateResetScreen "),
                "the raw-value variant class name (Screen) must not be emitted post-fix");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated dispatch wrapper must byte-match the golden (newline-normalized) for "
                + path + " — reverting the java-function-dispatch.stg javadoc/separator changes "
                + "restores the NoIndentWriter-stripped '* def' javadoc and the extra blank lines.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
