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
 * PR #202 (facet {@code convertToMeta} — the {@code with-meta} value→meta-wrapper construction).
 * {@code ConstructionHandler.handle(RWithMetaExpr)} was an M7b-1 STUB emitting the non-compiling
 * {@code <arg>.toBuilder().setMeta(MetaFields.builder()...).build()} (and the syntactically-impossible
 * {@code null.toBuilder()...} when the value is null). PR #202 builds the concrete meta wrapper,
 * mirroring upstream {@code ExpressionGenerator.caseWithMetaOperation} (in-tree 9.83.0). The expected
 * wrapper type comes from the enclosing function output's {@code [metadata …]} annotation
 * ({@code detectMetaKind} + {@code RJavaWithMetaValue.create} over {@code gm.getType(output)}); the
 * value compiles + item-coerces, and a MODEL value takes the null-guarded
 * {@code v == null ? null : v.toBuilder()} form.
 *
 * <p>Two wrapper arms (the kind = the output's meta annotation; the meta-name → setter via
 * {@code toPojoPropertyName}: scheme→Scheme, location→ScopedKey, id→ExternalKey, reference→ExternalReference,
 * address→Reference):
 * <ul>
 *   <li><b>FieldWithMeta</b> (scheme/location/id): hoist
 *       {@code final <ValueType|Builder> withMetaArgument = <argValue>;} on the statement-hoist sink,
 *       then {@code <Wrapper>.builder().setValue(withMetaArgument).setMeta(MetaFields.builder().set<X>(<val>))}
 *       (NO trailing {@code .build()} — the consumer wraps in {@code toBuilder(…)}).</li>
 *   <li><b>ReferenceWithMeta</b> (reference/address):
 *       {@code <Wrapper>.builder().setValue(<argValue>)[.setExternalReference(<href>)][.setReference(Reference.builder().setReference(<addr>))].build()}.</li>
 * </ul>
 *
 * <p>Green-safe by construction: the stub compiled to nothing valid (90 cdm6 carriers carry the broken
 * form, 20 the impossible {@code null.toBuilder(}; ZERO goldens carry it), so every with-meta carrier
 * was already a waivered mismatch — fixing the one render point cannot regress a green file.
 *
 * <p>Whole-file byte anchors through the REAL D11 loader (transitive rune-fpml dep per PR #184),
 * one per arm/sub-shape:
 * <ul>
 *   <li>{@code MapStringWithScheme} — FieldWithMeta, BASIC value ({@code final String withMetaArgument = value;}), setScheme.</li>
 *   <li>{@code CreateObservableWithLocation} — FieldWithMeta, MODEL value ({@code final Observable.ObservableBuilder withMetaArgument = observable == null ? null : observable.toBuilder();}), setScopedKey.</li>
 *   <li>{@code MapDateReference} — ReferenceWithMeta, href ({@code setValue(null).setExternalReference(<href>.get()).build()}).</li>
 *   <li>{@code CreatePriceWithAddress} — ReferenceWithMeta, address ({@code .setReference(Reference.builder().setReference(keyValue)).build()}).</li>
 *   <li>{@code MapPartyReference} — ReferenceWithMeta href, a second sub-shape.</li>
 * </ul>
 */
class FunctionConvertToMetaSetTest {

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

    /** FieldWithMeta, BASIC value: {@code final String withMetaArgument = value;} + setScheme. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapStringWithScheme_cdm6_fieldBasic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/common/functions/MapStringWithScheme.java");
    }

    /** FieldWithMeta, MODEL value: {@code Observable.ObservableBuilder ... == null ? null : ....toBuilder()} + setScopedKey. */
    @Test
    @EnabledIf("cellsAvailable")
    void createObservableWithLocation_cdm6_fieldModel_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/CreateObservableWithLocation.java");
    }

    /** ReferenceWithMeta, href: {@code setValue(null).setExternalReference(<href>.get()).build()}. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapDateReference_cdm6_referenceHref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapDateReference.java");
    }

    /** ReferenceWithMeta, address: {@code .setReference(Reference.builder().setReference(keyValue)).build()}. */
    @Test
    @EnabledIf("cellsAvailable")
    void createPriceWithAddress_cdm6_referenceAddress_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/CreatePriceWithAddress.java");
    }

    /** ReferenceWithMeta href, a second sub-shape. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapPartyReference_cdm6_reference_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapPartyReference.java");
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
                + " — the non-compiling stub `<arg>.toBuilder().setMeta(MetaFields.builder()...).build()` "
                + "reappears if the convertToMeta fix (ConstructionHandler.tryTypedWithMeta — the "
                + "FieldWithMeta / ReferenceWithMeta wrapper construction) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
