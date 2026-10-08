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
 * Facet {@code metaLocationConstruction} (PR #214, cluster 1) — a
 * {@code with-meta { key: <id> }} operation on a constructor whose value is a plain
 * Rosetta POJO populates the POJO's OWN {@code MetaFields} through upstream's
 * {@code getOrCreateMeta()} statement form (FunctionGenerator.xtend metaClass path +
 * PojoPropertyUtil {@code key}/{@code id} → {@code externalKey}, {@code scheme} →
 * {@code scheme}):
 *
 * <pre>
 * final &lt;Type&gt;.&lt;Type&gt;Builder withMetaArgument = &lt;ctor&gt; == null ? null : &lt;ctor&gt;.toBuilder();
 * withMetaArgument.getOrCreateMeta().setExternalKey(&lt;id&gt;.get());
 * &lt;out&gt; = toBuilder(withMetaArgument);
 * </pre>
 *
 * <p>The fork's M7b-1 legacy stub emitted
 * {@code <ctor>.toBuilder().setMeta(MetaFields.builder().setKey(<id>)…)} — there is no
 * such external-key setter on {@code MetaFields}, so every such with-meta carrier was
 * already a waivered mismatch; GREEN-SAFE by construction (zero goldens carry the stub
 * form; {@link com.regnosys.rosetta.generator.java.expression.handlers.ConstructionHandler#tryPojoMetaWithMeta}
 * fires ONLY on a {@code with-meta} whose argument is a model constructor, all of whose
 * entries map to a POJO meta setter, with a statement-hoist sink, and whose function
 * output carries no meta wrapper).
 *
 * <p>The value type is derived from the with-meta ARGUMENT's constructor type (NOT the
 * function output), so a NESTED with-meta (a constructor member, e.g.
 * {@code Payout{commodityPayout: CommodityPayout{…} with-meta{key:…}}}) hoists the
 * correct {@code final CommodityPayout.CommodityPayoutBuilder withMetaArgument} and is
 * referenced bare in the enclosing constructor —
 * {@code MapFloatingLegToCommodityPayout} anchors that path.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen cdm/6.20.6 goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen from the cluster's 20
 * waivered-flip files (byte-oracle over f-probe-214). REVERT-VERIFIED RED: reverting
 * the {@code tryPojoMetaWithMeta} arm reverts these anchors to the stub shape.
 */
class FunctionMetaLocationConstructionTest {

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

    /** The canonical whole-output SET: {@code BusinessCenters{…} with-meta{key: …->id}}. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapBusinessCenters_externalKeyStatementForm_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapBusinessCenters.java");
    }

    /** A TransferState payment carrier in the same external-key family. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapPaymentToTransferState_externalKeyStatementForm_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/payment/functions/MapPaymentToTransferState.java");
    }

    /** A single-attribute POJO output (BusinessUnit). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapBusinessUnit_externalKeyStatementForm_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapBusinessUnit.java");
    }

    /** Another distinct POJO output (NaturalPerson). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapNaturalPerson_externalKeyStatementForm_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapNaturalPerson.java");
    }

    /** The NESTED with-meta: the value type comes from the ctor (CommodityPayout), not the output (Payout). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapFloatingLegToCommodityPayout_nestedWithMeta_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/commodityswap/functions/MapFloatingLegToCommodityPayout.java");
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
                + path + " — a with-meta{key/id/scheme: …} on a model constructor hoists "
                + "`final <Type>Builder withMetaArgument = <ctor> == null ? null : "
                + "<ctor>.toBuilder();` then `withMetaArgument.getOrCreateMeta()"
                + ".setExternalKey(<id>.get());` and references the bare local; reverting the "
                + "metaLocationConstruction facet emits the non-compiling "
                + "setMeta(MetaFields.builder().setKey(…)) stub and reverts this anchor to RED.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
