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
 * Facet {@code member_name_disambiguation} — when an injected function DEPENDENCY
 * (a {@code @Inject protected X name;} field) and a SHORTCUT/alias (rendered as
 * {@code protected abstract MapperS<…> name(…)}) share a name, upstream rune-dsl's
 * {@code GeneratorScope.computeActualNames} groups every class-scope generated
 * identifier by desired name and numbers any group of size &gt; 1 in registration
 * order (dependencies first, shortcuts second). So a dependency {@code dayOfWeek}
 * and an alias {@code dayOfWeek} render as {@code dayOfWeek0} (the dependency) and
 * {@code dayOfWeek1} (the alias) consistently across the field decl, the abstract
 * method decl, the impl, and EVERY call site. The fork emitted the bare name for
 * both members (a field and a same-named method DO compile, but byte-mismatch the
 * numbered golden), so every collision file was already a waivered mismatch —
 * GREEN-SAFE by construction (the disambiguation is a strict no-op on every
 * collision-free function: {@code collidingDependencyAliasNames} returns an empty
 * set unless a name is simultaneously a dependency field name and a shortcut name).
 *
 * <p>The numbering is applied at six seats: the dependency field decl + the
 * {@code AliasModel} name in {@code FunctionGenerator.buildStandardModel} /
 * {@code compileAliases} (via
 * {@code FunctionDependencyCollector.collidingDependencyAliasNames}), and the four
 * call-site renderers in {@code ReferenceHandler} (the explicit-args dependency
 * receiver, the two implicit dependency receivers, and the alias invocation).
 *
 * <p>This test is REVERT-VERIFIED RED: reverting the disambiguation reverts these
 * anchors to the bare-name shape. Anchors are WHOLE-FILE byte comparisons against
 * the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen from the facet's 13
 * waivered-flip files (byte-oracle over f-probe-213):
 * <ul>
 *   <li>cdm/5.38.0 + cdm/6.20.6 {@code IsWeekend} — the minimal single collision: a
 *       dependency {@code DayOfWeek dayOfWeek} and an alias {@code dayOfWeek} →
 *       {@code dayOfWeek0} / {@code dayOfWeek1};</li>
 *   <li>cdm/6.20.6 {@code DeliveryAmount} — TWO independent collisions in one class
 *       ({@code creditSupportAmount0}/{@code 1} and
 *       {@code undisputedAdjustedPostedCreditSupportAmount0}/{@code 1}), with the
 *       non-colliding members ({@code max}, {@code roundToNearest},
 *       {@code deliveryAmount}, the validators) left bare;</li>
 *   <li>cdm/5.38.0 {@code FixedAmount} — the alias-references-alias chain: the
 *       {@code calcPeriodBase} body calls the numbered {@code calculationPeriod1};</li>
 *   <li>cdm/5.38.0 {@code ResolvePerformancePeriodStartPrice} — a cdm5-only carrier
 *       ({@code adjustedValuationDates0}/{@code 1}).</li>
 * </ul>
 */
class FunctionMemberNameDisambiguateTest {

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

    /** Minimal single collision: dependency dayOfWeek0 + alias dayOfWeek1 (cdm5). */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void isWeekend_cdm5_singleCollisionNumbered_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/datetime/functions/IsWeekend.java");
    }

    /** The same minimal single collision in cdm6. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void isWeekend_cdm6_singleCollisionNumbered_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/datetime/functions/IsWeekend.java");
    }

    /** Two independent collisions in one class; non-colliding members left bare. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void deliveryAmount_cdm6_twoCollisionsNumbered_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/legaldocumentation/csa/functions/DeliveryAmount.java");
    }

    /** Alias-references-alias chain: calcPeriodBase calls the numbered calculationPeriod1. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void fixedAmount_cdm5_aliasChainNumbered_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/asset/functions/FixedAmount.java");
    }

    /** cdm5-only carrier: adjustedValuationDates0 / adjustedValuationDates1. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void resolvePerformancePeriodStartPrice_cdm5_numbered_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/asset/functions/ResolvePerformancePeriodStartPrice.java");
    }

    private static void assertByteMatchesGolden(
            Map<String, String> functionOutput, Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — a dependency whose name collides with a shortcut/alias is "
                + "numbered name0 (the dependency) and name1 (the alias) across the field "
                + "decl, the abstract method, the impl and every call site (the dependency "
                + "receiver and the alias invocation); reverting the "
                + "member_name_disambiguation facet emits the bare name for both and reverts "
                + "this anchor to RED.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
