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
 * PR #252 — aliasThenSignatureTyping (the alias-then signature/return-type slice of the
 * inline-then hoist family). A function ALIAS (shortcut) whose body is a then-chain ending
 * in a bare-item CARDINALITY list-op — {@code then [ item first / last / only-element /
 * flatten ]} — was mis-typed at BOTH ends:
 *
 * <ul>
 *   <li>the alias method SIGNATURE leaked the function OUTPUT type
 *       ({@code FunctionAliasHelper.inferExpressionType}'s RThenExpr arm walked the then-body
 *       in isolation, where the implicit {@code item} is untyped &rarr; null &rarr;
 *       {@code computeReturnType} fell back to the output type), emitting e.g.
 *       {@code MapperC<? extends Counterparty>} where golden's {@code .first()} over a
 *       {@code MapperC<PayerReceiverModel>} receiver yields {@code MapperS<? extends
 *       PayerReceiverModel>}; AND</li>
 *   <li>the BODY rendered the broken inline {@code <receiver>.then(item -> item.first())}
 *       ({@code renderAliasThenHoistOrNull} was scoped to ComparisonResult-output then-chains,
 *       declining VALUE; PR #251's M2 confirmed widening the body-hoist ALONE churns without
 *       flipping — the signature fix is load-bearing).</li>
 * </ul>
 *
 * <p>The fix pairs the two: the RThenExpr arm types a bare-item cardinality then-body by
 * applying the op to the then-RECEIVER's type (first/last/only-element &rarr; single;
 * flatten &rarr; multi), and {@code renderAliasThenHoistOrNull}'s VALUE gate is widened to
 * exactly that shape ({@code isBareItemCardinalityThenBody}). Green-safe by construction: an
 * alias whose expression is an RThenExpr renders the broken runtime {@code .then(item -> …)}
 * form (no such method — ZERO of the 34,686 corpus-9.83.0 goldens carry runtime {@code .then(}
 * anywhere), so on clean main every such alias file is already a waivered, non-compiling mismatch
 * — the fix can only flip a waivered file toward golden (e.g. this carrier), never turn a
 * pre-existing green file red.
 *
 * <p>Whole-file byte anchor through the REAL D11 loader over the sole clean carrier (cdm6
 * {@code MapCommoditySwapCounterpartyList} — {@code … mapItem then [item first]}) + the
 * MapBuyerSeller lock, ORIGINALLY the #252 scope-DECLINE lock and CONVERTED at PR #350 (F4
 * aliasGeneralValueThenHoist) to the general-value HOIST lock — the #349-F4 signature
 * machinery retired the #251-M2 only-churns prediction. REVERT-VERIFIED RED.
 */
class FunctionAliasThenSignatureTypingTest {

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

    // ---- flip carrier: alias-then signature typing makes the file byte-match golden ----

    /**
     * cdm6 {@code MapCommoditySwapCounterpartyList}: the alias {@code payerReceiver} body is
     * {@code … .mapItem(item -> MapperS.of(getFpmlPayerReceiver.evaluate(item.get()))) then [item first]}.
     * The fork leaked {@code MapperC<? extends Counterparty>} (output type) on the abstract +
     * impl signatures and rendered the inline {@code .then(item -> item.first())}; golden hoists
     * {@code final MapperC<PayerReceiverModel> thenArg = …; return thenArg.first();} with the
     * signature {@code MapperS<? extends PayerReceiverModel>}. Reverting either the RThenExpr
     * signature arm OR the VALUE gate widening reverts this anchor (RED).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapCommoditySwapCounterpartyList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/commodityswap/functions/MapCommoditySwapCounterpartyList.java");
    }

    // ---- the #252 scope-DECLINE lock CONVERTED (PR #350, F4 aliasGeneralValueThenHoist) ----

    /**
     * cdm6 {@code MapBuyerSellerToAccountPartyReference}'s alias bodies are VALUE then-chains
     * whose LAST body is {@code distinct(item).get()}. The #252 pin locked the DECLINE
     * (predicting the #251-M2 widening "would hoist it and only churn"); the #350-F4 general
     * VALUE admission retired that prediction — the widened seat hoists {@code thenArg0/thenArg1}
     * and consumes {@code return distinct(thenArg1).get();} — the {@code distinct(thenArg1)}
     * fragment is golden-verbatim at both alias bodies (golden's full return additionally
     * wraps it {@code MapperS.of(...)}). The file moved 0.8471 → 0.9195 TOWARD at #350,
     * the #351 walk retyped the signature (the MapperS.of return-wrap fired), and the
     * PR #352 F-e2 defaultAliasArgDeref reduced the last residual (the getOrDefault
     * Mapper arg) — the file is now BYTE-GOLDEN, whole-file-locked by
     * MicroResidualNestedThenComposeTest; these fragment pins stay as the mechanism
     * witnesses.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapBuyerSellerToAccountPartyReference_generalValueThenHoists() {
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get(
                "cdm/ingest/fpml/confirmation/party/functions/MapBuyerSellerToAccountPartyReference.java");
        assertNotNull(generated, "Function not generated: MapBuyerSellerToAccountPartyReference.java");
        assertFalse(generated.contains(".then(item -> distinct(item).get())"),
                "The #350-F4 general VALUE admission hoists the alias then-chains — the inline "
                + "runtime `.then(` form (count 0 in every golden) must be gone.");
        // facet aliasThenSigRenderTruth (PR #351): the signature retyped (the item-bound
        // walk), so the F4 signature-keyed collapse now fires — the return carries
        // golden's FULL MapperS.of wrap (the #350 javadoc predicted exactly this
        // residual conversion).
        assertTrue(generated.contains("return MapperS.of(distinct(thenArg1).get());"),
                "The hoisted consumer collapses under the retyped signature — golden's "
                + "MapperS.of-wrapped return, verbatim at both alias bodies "
                + "(the #350-F4 witness, converted by the #351 walk).");
    }

    private static void assertByteMatchesGolden(Map<String, String> cellOutput, Path goldenDir, String path)
            throws IOException {
        assertNotNull(cellOutput, "Function generation did not run — corpus unavailable?");
        String generated = cellOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — an alias whose then-chain ends in a bare-item cardinality op must type its SIGNATURE from "
                + "the then-receiver (RThenExpr arm in FunctionAliasHelper) AND hoist its body "
                + "(renderAliasThenHoistOrNull VALUE gate). Reverting either reverts this anchor (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
