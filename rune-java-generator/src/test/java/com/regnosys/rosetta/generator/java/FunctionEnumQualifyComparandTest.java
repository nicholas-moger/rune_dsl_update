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
 * PR #215 facet {@code enumQualifyInheritedComparand} — a comparison operand that RESOLVED to an
 * enum value qualified by the DECLARING enum, where the SIBLING operand types as a CHILD enum that
 * INHERITS that value, re-qualifies to the sibling's child enum (the comparison type).
 *
 * <p>The carrier is {@code if idFormat = Lei …} where {@code idFormat} is a
 * {@code PartyIdentifierFormat2Enum} input and {@code PartyIdentifierFormat2Enum extends
 * LeiIdentifierFormatEnum}: the value {@code Lei} is declared on the parent, so the fork resolved it
 * to {@code LeiIdentifierFormatEnum.LEI}, while upstream qualifies by the sibling's child enum
 * {@code PartyIdentifierFormat2Enum.LEI} (the generated Java child enum flattens the inherited value
 * under its own name). The bare-operand arm ({@code ComparisonHandler.tryBareEnumComparand}, PR #154/
 * #206) only re-qualifies an UNRESOLVED operand; this RESOLVED sibling
 * ({@code tryInheritedEnumRequalify}) reuses {@code HandlerHelper.findEnumValueInHierarchy} (promoted
 * from {@code ConstructionHandler}'s PR #211 enumQualifyInherited) and resolves the sibling enum via
 * {@code leafEnumeration} / {@code siblingComparandEnumeration} / the new bare-input
 * {@code symbolReferenceEnumeration}. The hierarchy walk FROM the sibling UP its {@code extends} chain
 * finds the value only when the sibling is a CHILD (or the declaring enum itself), so the
 * re-qualification can only ever move DOWN to the comparison's child type — a sibling that is a SUPER
 * of the declaring enum declines.
 *
 * <p>Green-safe by construction: no byte-identical golden carries the parent-qualified comparison
 * form (the rewrite only touches currently-waivered output). Whole-file byte anchors through the REAL
 * D11 loader over the 4 drr hkma TRADE {@code Create_OrganisationIdentification15Choice} variants
 * (the valuation variants carry a SEPARATE {@code HKTRPartyScheme} FQN divergence — facet
 * {@code checkedMapTargetSimpleName}, {@link FunctionCheckedMapTargetSimpleNameTest}). REVERT-VERIFIED
 * RED at PR #215: reverting the {@code tryInheritedEnumRequalify} arm reverted these anchors to
 * {@code LeiIdentifierFormatEnum.LEI} + the spurious parent import. Since v3.1 flip seat 12 the
 * carriers' BARE {@code idFormat = Lei} operands are qualified by the ROOT arm
 * ({@code ReferenceHandler.handle(RSymbolReference)} — the node's INFERRED = expected enum,
 * {@code HandlerHelper.boundEnumInferredOwner}), so these anchors stay GREEN with the #215 arm
 * disabled (measured at seat 12: the arm early-returned, 4/4 green) — the revert-witness moved to the
 * root; the #215 arm remains load-bearing for EXPLICIT {@code Enum -> Value} operands only.
 */
class FunctionEnumQualifyComparandTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrFunctionOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrFunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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

    /** trade/dtcc __2 — the canonical {@code idFormat = Lei} carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void organisationIdentification15Choice_tradeDtcc2_childEnumRequalify_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/Create_OrganisationIdentification15Choice__2.java");
    }

    /** trade/dtcc __3 — a second variant in the same dtcc package. */
    @Test
    @EnabledIf("drrCellAvailable")
    void organisationIdentification15Choice_tradeDtcc3_childEnumRequalify_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/Create_OrganisationIdentification15Choice__3.java");
    }

    /** trade/tr __2 — a different projection target (tr) proving the law is package-independent. */
    @Test
    @EnabledIf("drrCellAvailable")
    void organisationIdentification15Choice_tradeTr2_childEnumRequalify_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/trade/tr/functions/Create_OrganisationIdentification15Choice__2.java");
    }

    /** trade/tr __3 — the fourth trade carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void organisationIdentification15Choice_tradeTr3_childEnumRequalify_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/trade/tr/functions/Create_OrganisationIdentification15Choice__3.java");
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
                + " — a comparison operand that resolves to an inherited enum value must re-qualify to "
                + "the sibling's CHILD enum (PartyIdentifierFormat2Enum.LEI, not LeiIdentifierFormatEnum.LEI); "
                + "since v3.1 flip seat 12 the ROOT arm (HandlerHelper.boundEnumInferredOwner) qualifies the "
                + "bare operand by the node's inferred = expected enum; the #215 ComparisonHandler arm "
                + "recomputes the same answer.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
