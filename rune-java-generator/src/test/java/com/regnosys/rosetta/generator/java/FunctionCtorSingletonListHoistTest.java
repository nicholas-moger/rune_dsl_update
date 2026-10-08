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
 * PR #198 (facet {@code ctorSingletonListHoist} — a SINGLE-cardinality function-call value SET on a
 * MULTI (List) constructor-builder attribute hoists {@code final <Item> x = <call>;} BEFORE the
 * assignment and consumes the null-guarded {@code (x == null ? Collections.<Item>emptyList() :
 * Collections.singletonList(x))} coercion, dropping the fork's non-compiling
 * {@code MapperS.of(<call>)} wrap + {@code .build().get()} tail).
 *
 * <p>The non-meta, item-into-list sibling of PR #190 (the meta multi-leaf {@code singletonList} wrap)
 * and PR #192 ({@code emptyMultiArg}'s {@code Collections.<T>emptyList()}). Upstream's
 * {@code TypeCoercionService.convertNullSafe} ALWAYS hoists a non-trivial value through
 * {@code declareAsVariable} when the value is consumed twice (the null-check + the singletonList), so
 * the inline double-evaluation form appears in ZERO goldens. The fork's #157/#158 typed builder block
 * DECLINED this shape — {@code ConstructionHandler.coerceCtorArg} returned {@code null} for a single
 * value into a MULTI attribute (the list-to-item collapse it did not reproduce) — so the whole
 * constructor fell back to the legacy one-line placeholder
 * ({@code EconomicTerms.builder().setPayout(MapperS.of(<call>)).build().get()}, a {@code MapperS} into
 * a {@code List<Payout>} setter = a Java compile error, hence every carrier was already a waivered,
 * non-compiling mismatch — green-safe by construction).
 *
 * <p>The fix adds the MULTI-attribute / single-value arm to {@code coerceCtorArg}: it registers the
 * value as a hoisted {@code final <ItemType> <name> = <call>;} local at the nearest statement-hoist
 * sink (the {@link com.regnosys.rosetta.generator.java.function.StatementHoistSession}, name group =
 * the lowercased item-type simple name — bare for a singleton, {@code base0..n-1} otherwise), then
 * returns the {@code Collections.<Item>emptyList()} / {@code singletonList(name)} ternary referencing
 * that local. Activating the arm makes the whole typed block render (its single-attribute setters
 * already drop the {@code MapperS.of} wrap correctly), the {@code selfUnwrapping} block drops the
 * {@code .build().get()} tail, and {@code prependStatementHoists} lifts the {@code final} decl before
 * the assignment. The item type is meta-wrapped via {@code RJavaWithMetaValue.create} when the
 * attribute carries a {@code [metadata ...]} annotation (PR #186/#193 machinery), so a meta attribute
 * declares the {@code FieldWithMetaX}/{@code ReferenceWithMetaX} wrapper local.
 *
 * <p>Whole-file byte anchors through the REAL D11 loader (which loads the transitive rune-fpml dep per
 * PR #184). All carriers are cdm6 ingest-fpml {@code Map*EconomicTerms} / ctor-builder mappers. NO
 * green FUNCTION carrier exists (the pre-fix {@code MapperS.of(value)} into a {@code List} setter never
 * compiled), so the over-fire guard is the full all-kinds D11 20/20 (zero collateral) + the byte-oracle
 * exact count, not a separate anchor.
 *
 * <p>Anchored:
 * <ul>
 *   <li>{@code MapBondOptionEconomicTerms} (cdm6 bondoption) — the PURE single {@code setPayout(...)}
 *       on an {@code EconomicTerms} ctor; non-meta item {@code Payout}; {@code Collections} already
 *       imported (proves the bare singleton-group name and the {@code -MapperS} import drop).</li>
 *   <li>{@code MapFxOptionEconomicTerms} (cdm6 fxoption) — MIXED: a single-attr {@code setEffectiveDate}
 *       (whose {@code MapperS.of} wrap drops via the existing path) ALONGSIDE the multi {@code setPayout}
 *       hoist; {@code Collections} import added, {@code MapperS} retained (the chain still uses it).</li>
 *   <li>{@code MapVolatilityLegToPriceQuantity} (cdm6 volatilityswap) — a META item type
 *       ({@code FieldWithMetaNonNegativeQuantitySchedule}) on a {@code PriceQuantity} ctor; proves the
 *       {@code RJavaWithMetaValue.create} wrapper local + its metafields import.</li>
 *   <li>{@code MapIssuerTradeIdModelToIdentifier} (cdm6 header) — a {@code TradeIdentifier} ctor (a
 *       DIFFERENT output type) with a multi {@code setAssignedIdentifier} hoist alongside two
 *       single-attr setters; confirms generality beyond the {@code EconomicTerms} family.</li>
 * </ul>
 */
class FunctionCtorSingletonListHoistTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
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

    // ---- single value into a MULTI ctor setter → hoist + singletonList null-guard ----

    /** PURE single {@code setPayout} on EconomicTerms; non-meta {@code Payout}; Collections pre-imported. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapBondOptionEconomicTerms_cdm6_singletonHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/bondoption/functions/MapBondOptionEconomicTerms.java");
    }

    /** MIXED: a single-attr setter (MapperS.of drop) alongside the multi {@code setPayout} hoist. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapFxOptionEconomicTerms_cdm6_mixedSetters_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/fxoption/functions/MapFxOptionEconomicTerms.java");
    }

    /** META item type — the {@code FieldWithMetaNonNegativeQuantitySchedule} wrapper local. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapVolatilityLegToPriceQuantity_cdm6_metaItemHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/volatilityswap/functions/MapVolatilityLegToPriceQuantity.java");
    }

    /** A {@code TradeIdentifier} ctor (different output type) — generality beyond EconomicTerms. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapIssuerTradeIdModelToIdentifier_cdm6_tradeIdentifierCtor_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/header/functions/MapIssuerTradeIdModelToIdentifier.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the non-compiling MapperS.of(<call>) into a List setter reappears if the "
                + "ctorSingletonListHoist fix (ConstructionHandler.coerceCtorArg — hoist a single value "
                + "into a MULTI ctor attribute to a final local + Collections.<Item>emptyList()/"
                + "singletonList null-guard ternary) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
