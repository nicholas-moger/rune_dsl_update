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
 * PR #201 (facet {@code addAllToBuilderList} — the MODEL-TYPED-output extension of PR #199's
 * {@code addAllItemIntoList}). A whole-output ADD whose value is SINGLE-cardinality into a MULTI
 * (List) output whose element is a Rosetta MODEL object ({@code outputNeedsBuilder == true})
 * coerces the item into a list via the same null-guarded if/else BLOCK as #199, but each
 * {@code addAll(...)} arm wraps the coerced list in {@code toBuilder(...)} (the list element is the
 * builder type {@code <Item>.<Item>Builder}):
 *
 * <pre>
 * [final &lt;Item&gt; &lt;name&gt; = &lt;value&gt;;]                                  // hoisted unless &lt;value&gt; is a bare identifier
 * if (&lt;name&gt; == null) {
 *     &lt;list&gt;.addAll(toBuilder(Collections.&lt;Item&gt;emptyList()));
 * } else {
 *     &lt;list&gt;.addAll(toBuilder(Collections.singletonList(&lt;name&gt;)));
 * }
 * </pre>
 *
 * <p>PR #199 DELIBERATELY DECLINED the model-typed output (the {@code if (outputNeedsBuilder) return null;}
 * gate in {@code renderAddSingletonItemOrNull}) because the {@code toBuilder}-wrapped form was
 * corpus-UNVERIFIED — no model-typed carrier had flipped in its byte-oracle. The f-probe-201 census
 * surfaces 35 cdm6 ingest-fpml {@code Map*List} carriers verifying the exact form, so #201 removes the
 * gate and wraps the arms. TWO value-shape arms share the one upstream law
 * ({@code TypeCoercionService.convertNullSafe} item->list coercion at a MULTI ADD):
 * <ul>
 *   <li>ARM A — a FUNCTION-CALL value (an RSymbolReference resolving to an RFunction, e.g.
 *       {@code mapCounterparty.evaluate(...)}); already admitted by the #199 value-shape guard, so the
 *       single {@code outputNeedsBuilder} gate-removal flips it (20 of the 35 carriers — the 25 fn-call
 *       census carriers minus the 5 whose META-annotated output stays blocked by the separate
 *       {@code detectMetaKind(output)!=NONE} decline).</li>
 *   <li>ARM B — a CONSTRUCTOR value (an {@code RConstructorExpr}, e.g. {@code ProductTaxonomy.builder()
 *       ...build()}); always single-cardinality (builds exactly one object). The #199 guard required an
 *       RSymbolReference, so ARM B additionally admits a constructor value (10 of the 35 carriers).</li>
 * </ul>
 *
 * <p>Green-safe by construction: the fork's pre-fix form is {@code <list>.addAll(toBuilder(<single item>))}
 * — {@code toBuilder(<single model object>)} returns a single builder, and a single builder passed to
 * {@code List.addAll(Collection)} is a Java compile error, so every carrier was already a waivered,
 * non-compiling mismatch. The guard still EXCLUDES a genuinely MULTI value: {@code MapProductTaxonomyList}
 * carries BOTH a single-ctor ADD (flips, ARM B) AND a {@code MapperS.of(...).mapC(...).mapItem(...)}
 * MULTI nav-chain ADD that stays {@code addAll(toBuilder(<list>))} byte-identical (neither RSymbolReference
 * nor RConstructorExpr) — proving the coercion fires only on the provably-single value.
 *
 * <p>Whole-file byte anchors through the REAL D11 loader (transitive rune-fpml dep per PR #184):
 * <ul>
 *   <li>{@code MapBuyerSellerModelToCounterpartyList} (cdm6 party) — ARM A: two fn-call ADDs hoisting
 *       {@code final Counterparty counterparty0/1 = mapCounterparty.evaluate(...);} numbered through the
 *       StatementHoistSession, each with the {@code addAll(toBuilder(Collections...))} block + the
 *       {@code java.util.Collections} import.</li>
 *   <li>{@code MapFraPriceQuantityList} (cdm6 product/fra) — ARM A: two fn-call ADDs hoisting
 *       {@code final PriceQuantity priceQuantity0/1 = ...;}.</li>
 *   <li>{@code MapProductTaxonomyList} (cdm6 common) — ARM B: a single-ctor ADD hoisting an un-suffixed
 *       {@code final ProductTaxonomy productTaxonomy = ProductTaxonomy.builder()...build();} (multi-line
 *       ctor RHS) PLUS a MULTI nav-chain ADD that must stay byte-identical (the in-file green invariant).</li>
 * </ul>
 */
class FunctionAddAllToBuilderListTest {

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

    // ---- ARM A: function-call value into a MODEL-typed MULTI ADD → hoist + addAll(toBuilder(...)) ----

    /** ARM A: two numbered {@code counterparty0/1} fn-call hoists, the toBuilder-wrapped null-guard block. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapBuyerSellerModelToCounterpartyList_cdm6_armA_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapBuyerSellerModelToCounterpartyList.java");
    }

    /** ARM A: two numbered {@code priceQuantity0/1} fn-call hoists (a different Map*PriceQuantityList sub-shape). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapFraPriceQuantityList_cdm6_armA_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/fra/functions/MapFraPriceQuantityList.java");
    }

    // ---- ARM B: constructor value into a MODEL-typed MULTI ADD → un-suffixed hoist; the MULTI ADD stays ----

    /** ARM B: a single un-suffixed {@code productTaxonomy} ctor hoist; the sibling MULTI nav-chain ADD stays. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapProductTaxonomyList_cdm6_armB_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/common/functions/MapProductTaxonomyList.java");
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
                + " — the non-compiling <list>.addAll(toBuilder(<single item>)) reappears if the "
                + "addAllToBuilderList fix (FunctionExpressionRenderer.renderAddSingletonItemOrNull — drop the "
                + "outputNeedsBuilder decline, wrap the emptyList/singletonList arms in toBuilder(...), and "
                + "admit a constructor value) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
