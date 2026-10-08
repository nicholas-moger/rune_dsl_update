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
 * PR #236 — facet {@code ctorSetterMetaDerefHoist}: a complex ctor-setter value that
 * PROVABLY produces an attribute-meta wrapper ({@code FieldWithMeta*}/{@code ReferenceWithMeta*})
 * — a direct function call whose callee output carries a value-level {@code [metadata …]}
 * annotation — consumed by a NON-meta single setter (which takes the bare item) must hoist
 * the wrapper to a {@code final <Wrapper> <name> = <call>;} statement local (it is
 * referenced TWICE by the null guard) and unwrap it null-safely
 * {@code (<name> == null ? null : <name>.getValue())} — upstream's {@code convertNullSafe}
 * meta→item deref at the ctor-setter seat (the SETTER analogue of the PR #143/#170
 * evaluate-arg deref and the PR #208 numeric narrow, which DECLINED these complex values).
 * The fork spliced the wrapper BARE (a {@code FieldWithMetaX} where the bare item is
 * expected — non-compiling, so every carrier was already a waivered mismatch → green-safe
 * by construction). See {@link com.regnosys.rosetta.generator.java.expression.handlers.ConstructionHandler}
 * {@code hoistMetaDerefCtorValueOrNull}.
 *
 * <p>2 cdm6 FUNCTION flips, each locked here by a WHOLE-FILE byte anchor through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} + {@link FunctionGenerator}
 * (newline-normalized, revert-RED):
 * <ul>
 *   <li>{@code MapCashCollateralValuationMethod} — {@code mapCurrency.evaluate(…)} →
 *       {@code FieldWithMetaString} into {@code .setCashCollateralCurrency(String)};</li>
 *   <li>{@code MapCreditEventNotice} — {@code mapBusinessCenter.evaluate(…)} →
 *       {@code FieldWithMetaBusinessCenterEnum} into {@code .setBusinessCenter(BusinessCenterEnum)}.</li>
 * </ul>
 *
 * <p><b>Green-safety lock</b> — {@code SetCashCurrency} (cdm6): a green ctor-setter into a
 * META attribute (its field IS the {@code FieldWithMeta*} wrapper). The deref arm DECLINES
 * here — over-determined by BOTH gates: the attribute is meta-annotated
 * ({@code detectMetaKind(attr) != NONE}) AND its value is a Mapper chain, not a direct
 * meta-output function call ({@code valueProvenMetaKind == NONE}) — so the output stays
 * byte-identical (golden passes the value to the wrapper-typed setter unchanged). This is
 * the exact shape that the over-broad value→meta WRAP arm (prototyped then reverted)
 * regressed by over-firing on green meta-attr setters; this anchor is the byte-lock that the
 * deref arm must NOT touch such a file. (Because the decline is over-determined it does not
 * isolate the {@code detectMetaKind(attr)} gate specifically — that gate is exercised by the
 * 5-cell D11 byte-compare matrix's green meta-attr-from-meta-fn-call ctor-setters, which stay
 * green; the matrix 20/20 + the PRE/POST within-waiver regression scan (0 regressions)
 * establish green-safety in full, and these anchors lock the byte form.)
 */
class FunctionCtorSetterMetaDerefHoistTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
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
     * MapCashCollateralValuationMethod (cdm6) — golden hoists
     * {@code final FieldWithMetaString fieldWithMetaString = mapCurrency.evaluate(fpmlCashCollateralCurrency);}
     * then {@code .setCashCollateralCurrency((fieldWithMetaString == null ? null : fieldWithMetaString.getValue()))};
     * the fork spliced {@code mapCurrency.evaluate(...)} bare (a FieldWithMetaString into a String setter).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCashCollateralValuationMethod_metaDerefHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/settlement/functions/MapCashCollateralValuationMethod.java");
    }

    /**
     * MapCreditEventNotice (cdm6) — golden hoists
     * {@code final FieldWithMetaBusinessCenterEnum fieldWithMetaBusinessCenterEnum = mapBusinessCenter.evaluate(...);}
     * then {@code .setBusinessCenter((fieldWithMetaBusinessCenterEnum == null ? null : fieldWithMetaBusinessCenterEnum.getValue()))};
     * the fork spliced {@code mapBusinessCenter.evaluate(...)} bare.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCreditEventNotice_metaDerefHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapCreditEventNotice.java");
    }

    /**
     * SetCashCurrency (cdm6) — green-safety: a green ctor-setter into a META attribute. The
     * deref arm must DECLINE (the setter takes the wrapper, there is nothing to unwrap); the
     * output stays byte-identical to golden. A byte-lock that the deref arm leaves a green
     * meta-attr ctor-setter untouched (the regression class of the reverted value→meta WRAP
     * arm). Its decline is over-determined (meta attr AND Mapper-chain value), so it does not
     * isolate the {@code detectMetaKind(attr)} gate — see the class javadoc.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void setCashCurrency_metaAttrSetter_declines_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/staticdata/asset/common/functions/SetCashCurrency.java");
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
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — PR #236 ctorSetterMetaDerefHoist.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
