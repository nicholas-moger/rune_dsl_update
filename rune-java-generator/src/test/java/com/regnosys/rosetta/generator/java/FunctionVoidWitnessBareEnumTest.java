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
 * Facet {@code void_witness_bare_enum} — upstream types a list literal from its
 * OWN inferred element join and binds bare element names against the
 * EXPECTED enum type, so no golden ever carries a {@code MapperC.<Void>of}
 * witness or an unqualified/unwrapped enum constant (wf169 mechanism (d)).
 *
 * <p>One upstream law, four fork arms:
 *
 * <ol>
 *   <li><b>D1 — list-literal witness fallback ({@code LiteralHandler.listItemJavaType}).</b>
 *       The inference-side element join swallows MISSING elements and lands on
 *       NOTHING ({@code ExpressionTypeComputer.computeListLiteral} +
 *       {@code TypeJoin}'s bottom treatment), which the translator maps to
 *       {@code Void} — a witness no golden carries
 *       ({@code MapperC.of(MapperBuilder&lt;? extends T&gt;...)} cannot accept a
 *       non-Void element, so the pre-fix render is NON-COMPILING). Gated
 *       EXACTLY on that shape (non-empty elements, inferred type NOTHING), the
 *       arm re-derives the witness by the SAME gm-aware per-element walk the
 *       navigation renderer types chains with
 *       ({@code NavigationHandler.resolveReceiverDataType} /
 *       {@code implicitItemDataType}); all elements must agree on ONE type and
 *       meta leafs decline (no concrete-meta witness machinery here — those
 *       carriers are multi-mechanism REACH).</li>
 *   <li><b>D2 — Cat 13c: bare-enum binding for list-literal elements
 *       (parser-side {@code TypeInferenceEngine}).</b> Upstream's expected-type
 *       scoping flows the operation target's enum type THROUGH a list literal
 *       to its elements ({@code ExpectedTypeProvider.caseListLiteral} +
 *       {@code RosettaScopeProvider}'s expected-enum implicit features), so a
 *       bare {@code [CA_AB_ASC, ...]} binds each name to the enum value. The
 *       fork's Cat 13/13b/14 covered conditional branches and comparison
 *       operands only; 13c adds list-literal elements under the SAME hard gate
 *       as 13b (the literal is the DIRECT body of an operation targeting the
 *       function output, which types as an enum) via the SAME idempotent
 *       {@code bindBareEnumValue} worker. The witness then follows from the
 *       EXISTING inference join, and {@code ReferenceHandler}'s Cat-13 render
 *       emits the qualified constant.</li>
 *   <li><b>D3 — element wrap for a RESOLVED bare enum value
 *       ({@code LiteralHandler}).</b> The Cat-13 render is UNWRAPPED by design
 *       (its other consumers self-wrap), so a list element that D2 just bound
 *       still needs the literal's own {@code MapperS.of(...)} element wrap —
 *       the same wrap the qualified {@code REnumValueRef} element form already
 *       gets.</li>
 *   <li><b>D4 — contains/disjoint enum-operand wrap
 *       ({@code SetOperationHandler}).</b> Upstream compiles BOTH set-operation
 *       operands against {@code MAPPER.wrapExtends(joined)}
 *       ({@code ExpressionGenerator.binaryExpr} contains/disjoint case), so an
 *       enum-constant operand coerces item→{@code MapperS.of(E.V)}. The fork's
 *       equality/comparison handlers already apply exactly this wrap
 *       ({@code ComparisonHandler.wrapEnumOperand}); the contains/disjoint
 *       handler did not — {@code contains(chain, PartyRoleEnum.X)} passes a
 *       bare constant where a {@code Mapper} is expected (NON-COMPILING).</li>
 * </ol>
 *
 * <p>Green-safety: every firing shape renders non-compiling Java pre-fix — a
 * {@code Void} witness over non-Void elements, an unresolved bare constant
 * name, a bare enum constant in a {@code Mapper}-typed operand slot — and the
 * corpus goldens carry ZERO {@code MapperC.<Void>of} occurrences, so no
 * byte-matching file can carry any of them. D2 binds only symbol-EMPTY
 * references under the direct-output hard gate (the Cat 13/13b/14 idempotence
 * contract); D1 declines unless every element walks to one identical
 * non-meta type.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}:
 * <ul>
 *   <li>drr/6.34.1 {@code SupervisoryBodyForCSA} — D2+D3 (13 bare enum
 *       elements bind, render qualified, wrap, and the witness joins to
 *       {@code &lt;SupervisoryBodyEnum&gt;} through the existing inference
 *       path);</li>
 *   <li>cdm/5.38.0 + cdm/6.20.6 {@code Create_ContractFormation} — D1 (two
 *       nav-chain elements walk to {@code LegalAgreement}) + the multi
 *       SET-leaf list-literal value extracting at LIST arity
 *       ({@code .getMulti()} via {@code extractValueProvesMulti}'s
 *       list-literal arm);</li>
 *   <li>cdm/5.38.0 {@code Qualify_ClearedTrade} + cdm/6.20.6
 *       {@code Qualify_OpenOfferClearedTrade} — D4 ({@code contains} right
 *       operand {@code MapperS.of(PartyRoleEnum.CLEARING_ORGANIZATION)}).</li>
 * </ul>
 */
class FunctionVoidWitnessBareEnumTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
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
        if (drrCellAvailable()) {
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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

    /** D2+D3: 13 bare enum elements bind, qualify, wrap; witness joins to the enum (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void supervisoryBodyForCSADrr_bareEnumListLiteral_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/csa/rewrite/trade/functions/SupervisoryBodyForCSA.java");
    }

    /** D1 + list-literal SET-leaf LIST arity: {@code <LegalAgreement>} witness + {@code .getMulti()} (cdm5). */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void createContractFormationCdm5_listLiteralWitnessAndMultiLeaf_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Create_ContractFormation.java");
    }

    /** D1 + list-literal SET-leaf LIST arity: the identical shape in the cdm6 cell. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void createContractFormationCdm6_listLiteralWitnessAndMultiLeaf_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Create_ContractFormation.java");
    }

    /** D4: contains right operand wraps {@code MapperS.of(PartyRoleEnum.CLEARING_ORGANIZATION)} (cdm5). */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyClearedTradeCdm5_containsEnumOperandWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_ClearedTrade.java");
    }

    /** D4: the second carrier shape, pinned in the cdm6 cell. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyOpenOfferClearedTradeCdm6_containsEnumOperandWrap_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_OpenOfferClearedTrade.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> functionOutput,
            Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — the list-literal witness fallback (D1), the Cat-13c "
                + "bare-enum element binding (D2), the resolved-element MapperS.of wrap "
                + "(D3), the contains/disjoint enum-operand wrap (D4), or the "
                + "list-literal multi SET-leaf arity is missing or regressed, if the "
                + "void_witness_bare_enum recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
