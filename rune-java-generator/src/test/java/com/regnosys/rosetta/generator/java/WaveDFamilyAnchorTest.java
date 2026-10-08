package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #409 — coverage burn-down wave D anchors: the datarule family
 * ({@code <pkg>.validation.datarule.<Type><Condition>}) locked byte-identical
 * against the frozen 9.83.0 goldens, generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} route. Nine
 * whole-file locks with cell coverage 5/5 carrying the family's hardest
 * shapes, plus NEGATIVE WITNESSES — tokens with COUNT 0 IN THE GOLDEN that
 * exactly a broken law re-introduces.
 *
 * <p>The laws these anchors hold shut (the burn trail:
 * {@code target-409-audit1..22.log} — 3,853/3,853 at audit-22):
 * <ol>
 *   <li><b>the A2 logical-combine deferral</b> — upstream allocates
 *       {@code ifThenElseResult} names at COMPOSITION ({@code
 *       JavaIfThenElseBuilder.then} collapses the LEFT receiver at combine
 *       time, AFTER the right subtree composed), so {@code A and (B and C)}
 *       numbers B=0, C=1, A=2 while EMITTING in source order
 *       (CFTCPart43…FloatingRateResetFrequencyPeriodCond);</li>
 *   <li><b>the explicit-param scope law</b> — an explicit-param lambda
 *       ({@code extract dp [ … ]}) has NO item feature scope upstream: bare
 *       names re-navigate the condition instance from the
 *       {@code executeDataRule} parameter, and a Mapper-typed closure param
 *       into a MULTI ({@code 0..*}) callee input collapses {@code
 *       .getMulti()} (ESMAEMIRTransactionReportEMIR_VR_2121_01);</li>
 *   <li><b>the CR-arm asMapper law</b> — a ComparisonResult arm at a
 *       Mapper-returning block seat coerces {@code .asMapper()} (the #361
 *       ladder law extended to the effective-else / elseless / nested-tree /
 *       Mapper-slot-assign seats);</li>
 *   <li><b>the bare mapC-deref naming law</b> — a chain crossing an
 *       uncollapsed {@code mapC} hop derefs its meta chain-end with the BARE
 *       un-numbered form whose finalization escape yields
 *       {@code _fieldWithMetaDate} exactly when the guarded group's singleton
 *       claimed the plain name (EconomicTermsFpML_cd_26_28 — the cdm5 pin);</li>
 *   <li><b>the declared-input witness law</b> — a fn-call ARG nav's last hop
 *       witnesses the callee input's DECLARED type over the leaf OVERRIDE
 *       ({@code <PeriodicPayment>} where CommonLeg overrides
 *       {@code periodicPayment} as CommonPeriodicPayment);</li>
 *   <li><b>the raw-source DEFINITION law</b> — the DEFINITION string keeps the
 *       {@code ^type} escaped source form (the RosettaIdLexer de-escapes via
 *       setText; tokenText reads the raw span — CoalProductChoice);</li>
 *   <li><b>the choice one-of synthesis</b> — a {@code choice} type synthesizes
 *       the {@code <Name>Choice} one-of condition (AssetChoice); the iso cell
 *       stamps the literal {@code ${project.version}} javadoc
 *       (Cleared23Choice__1Choice);</li>
 *   <li><b>the switch-over-choice RETURN ladder + the boolHoist + the
 *       @Inject dep collection</b> — the step-1c/2 shapes
 *       (ExerciseInstructionIsOptionPayout / TradeSettlementPayout /
 *       CashCurrencyExists).</li>
 * </ol>
 *
 * <p><b>REVERT-RED (the wave-C law-reversion form).</b> The datarule family is
 * ENTIRELY NEW src/main (the ringfence D44 rule): the pre-wave tree has no
 * generator to check out, so the full-revert RED is an unconditional
 * anchor-compilation failure, and per-law discrimination is revert-verified by
 * LAW REVERSION instead: (a) the explicit-param gate reverted
 * ({@code buildConditionInstanceReceiver} → always the lambda item inside a
 * lambda) — the VR_2121 lock + the explicit-param witness go RED; (b) the A2
 * logical-combine deferral reverted (register-at-operand-compile) — the CFTC
 * lock + the numbering witness go RED ({@code target-409-anchor-red*.log}).
 */
class WaveDFamilyAnchorTest {

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    private static final Map<String, D11CorpusRegressionTest.CellSpec> CELLS = Map.of(
            "cdm5", new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0",
                    Path.of("../test-corpus/cdm/cdm-5.38.0")),
            "cdm6", new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6",
                    Path.of("../test-corpus/cdm/cdm-6.20.6")),
            "drr", new D11CorpusRegressionTest.CellSpec("drr", "6.34.1",
                    Path.of("../test-corpus/drr/drr-6.34.1")),
            "iso", new D11CorpusRegressionTest.CellSpec("iso20022", "1.38.0",
                    Path.of("../test-corpus/iso20022/iso20022-1.38.0")),
            "fpml", new D11CorpusRegressionTest.CellSpec("rune-fpml", "2.0.0",
                    Path.of("../test-corpus/rune-fpml/rune-fpml-2.0.0")));

    /** Per cell key: the datarule output map (path → source). */
    private static final Map<String, Map<String, String>> OUTPUTS = new LinkedHashMap<>();

    static boolean allCellsAvailable() {
        return CELLS.values().stream().allMatch(D11CorpusRegressionTest::cellGoldensExist);
    }

    @BeforeAll
    static void generateAll() throws IOException {
        if (!allCellsAvailable()) {
            return;
        }
        var d11 = new D11CorpusRegressionTest();
        for (var e : CELLS.entrySet()) {
            var cell = e.getValue();
            var corpus = d11.loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var gen = new DataRuleGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    assertNoGenerationErrors(gen.generateClasses(model, gm.version(model), output));
                }
            }
            OUTPUTS.put(e.getKey(), output);
        }
    }

    // ==== whole-file byte locks (cell coverage 5/5) ====

    /**
     * drr, THE numbering pin: {@code A and (B and C)} — three conditional
     * operands hoisted {@code final ComparisonResult ifThenElseResultN;} with
     * upstream's collapse-at-composition numbering (source-first = 2, the right
     * subtree 0/1) and the right-nested return
     * {@code ifThenElseResult2.andNullSafe(ifThenElseResult0.andNullSafe(ifThenElseResult1))}.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void datarule_drr_cftcFloatingRateReset_a2CombineNumbering_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr",
                "drr/regulation/cftc/rewrite/trade/validation/datarule/CFTCPart43TransactionReportFloatingRateResetFrequencyPeriodCond.java");
    }

    /**
     * drr, the explicit-param scope carrier: {@code extract dp [ … ]} — bare
     * {@code interconnectionPoint}/{@code loadType} re-navigate the condition
     * instance ({@code MapperS.of(eSMAEMIRTransactionReport)…}), the
     * {@code IsAcceptedEicCode(dp)} arg collapses {@code dp.getMulti()} (the
     * {@code eicCode string (0..*)} input), and the effective-else block arms
     * coerce {@code .asMapper()}.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void datarule_drr_eicCode_explicitParamScope_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr",
                "drr/regulation/esma/emir/refit/trade/validation/datarule/ESMAEMIRTransactionReportEMIR_VR_2121_01.java");
    }

    /**
     * cdm5, the deref-naming pin: the mapC-crossing chain's BARE un-numbered
     * meta deref ({@code _fieldWithMetaDate -> _fieldWithMetaDate.getValue()},
     * finalization-escaped against the guarded singleton's plain
     * {@code fieldWithMetaDate}) beside the guarded MapperS-chain form.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void datarule_cdm5_economicTermsFpml_mapCDerefNaming_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm5",
                "cdm/product/template/validation/datarule/EconomicTermsFpML_cd_26_28.java");
    }

    /**
     * cdm6, the choice one-of synthesis: the {@code choice Asset} type's
     * synthesized {@code AssetChoice} condition — the cardinality-twin
     * {@code choice(MapperS.of(asset), …)} over the option attributes by TYPE
     * NAME.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void datarule_cdm6_assetChoice_synthesizedOneOf_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6",
                "cdm/base/staticdata/asset/common/validation/datarule/AssetChoice.java");
    }

    /**
     * cdm6, the switch-over-choice RETURN ladder
     * ({@code hoistDataRuleSwitchReturnLadderOrNull}: subject decl + meta
     * Type-coercion deref + null-guard + per-case option-nav ifs with case
     * locals; the compiled default is the returned consumer).
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void datarule_cdm6_exerciseInstruction_switchReturnLadder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6",
                "cdm/event/common/validation/datarule/ExerciseInstructionIsOptionPayout.java");
    }

    /**
     * cdm6, the #217 boolHoist law at the datarule cond seat: {@code final
     * Boolean _boolean = fn.evaluate(…);} + the
     * {@code (_boolean == null ? false : _boolean)} guard.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void datarule_cdm6_tradeSettlementPayout_boolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6",
                "cdm/event/common/validation/datarule/TradeSettlementPayout.java");
    }

    /**
     * cdm6, the @Inject dep carrier: the FUNCTION collector over the synthetic
     * single-condition wrapper injects the callee field the condition body
     * evaluates.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void datarule_cdm6_cashCurrencyExists_injectDeps_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6",
                "cdm/base/staticdata/asset/common/validation/datarule/CashCurrencyExists.java");
    }

    /**
     * iso, the {@code ${project.version}} literal javadoc stamp + the
     * choice-type {@code <Name>Choice} synthesis in the iso golden tree
     * (the target/classes tree — the iso-tree grep law).
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void datarule_iso_cleared23Choice_versionStamp_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("iso",
                "iso20022/auth030/asic/validation/datarule/Cleared23Choice__1Choice.java");
    }

    /**
     * fpml, the raw-source DEFINITION carrier: {@code optional choice ^type,
     * coalProductSpecifications} — the caret-escaped attribute name survives
     * into DEFINITION (tokenText reads the raw span, not the lexer's
     * de-escaped setText).
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void datarule_fpml_coalProductChoice_caretDefinition_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml",
                "fpml/consolidated/com/validation/datarule/CoalProductChoice.java");
    }

    // ==== NEGATIVE WITNESSES (count 0 in golden; a broken law re-introduces each) ====

    /**
     * A2 combine-numbering witness: the register-at-operand-compile return
     * shape {@code ifThenElseResult0.andNullSafe(ifThenElseResult1…} has count
     * 0 in the golden — the combine deferral numbers the right subtree first
     * (B=0, C=1) and the source-first operand LAST (A=2).
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_a2CombineNumbering() {
        String gen = generated("drr",
                "drr/regulation/cftc/rewrite/trade/validation/datarule/CFTCPart43TransactionReportFloatingRateResetFrequencyPeriodCond.java");
        assertFalse(gen.contains("return ifThenElseResult0.andNullSafe(ifThenElseResult1.andNullSafe(ifThenElseResult2));"),
                "BROKEN LAW: register-at-operand-compile numbering (source order 0/1/2)");
        assertTrue(gen.contains("return ifThenElseResult2.andNullSafe(ifThenElseResult0.andNullSafe(ifThenElseResult1));"),
                "golden shape: collapse-at-composition numbering (right subtree first)");
    }

    /**
     * Explicit-param scope witness: the item-rooted synthesis
     * {@code dp.<String>map("getInterconnectionPoint"} and the scalar arg
     * collapse {@code evaluate(dp.get())} both have count 0 in the golden —
     * bare names under an explicit param re-navigate the condition instance,
     * and the MULTI callee input takes {@code dp.getMulti()}.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_explicitParamScope_argGetMulti() {
        String gen = generated("drr",
                "drr/regulation/esma/emir/refit/trade/validation/datarule/ESMAEMIRTransactionReportEMIR_VR_2121_01.java");
        assertFalse(gen.contains("dp.<String>map(\"getInterconnectionPoint\""),
                "BROKEN LAW: a bare name synthesized against the explicit-param lambda item");
        assertFalse(gen.contains("evaluate(dp.get())"),
                "BROKEN LAW: the scalar collapse into the MULTI eicCode (0..*) input");
        assertTrue(gen.contains("evaluate(dp.getMulti())"),
                "golden shape: the accessor follows the callee parameter (.getMulti())");
        assertTrue(gen.contains(
                "MapperS.of(eSMAEMIRTransactionReport).<String>map(\"getInterconnectionPoint\""),
                "golden shape: the condition-instance re-navigation inside the dp lambda");
    }

    /**
     * CR-arm asMapper witness: the BARE ComparisonResult return
     * {@code …getEffectiveDate()));} (no {@code .asMapper()}) has count 0 in
     * the golden — the Mapper-returning block arm coerces every
     * exists/notExists return.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_blockArmAsMapper() {
        String gen = generated("drr",
                "drr/regulation/esma/emir/refit/trade/validation/datarule/ESMAEMIRTransactionReportEMIR_VR_2057_01.java");
        assertFalse(gen.contains(
                "return exists(item.<Date>map(\"getEffectiveDate\", notionalPeriod -> notionalPeriod.getEffectiveDate()));"),
                "BROKEN LAW: a bare ComparisonResult return at the Mapper-returning block arm");
        assertTrue(gen.contains(
                "return exists(item.<Date>map(\"getEffectiveDate\", notionalPeriod -> notionalPeriod.getEffectiveDate())).asMapper();"),
                "golden shape: the .asMapper() coercion on the block-arm return");
    }

    /**
     * mapC-deref naming witness: the numbered {@code fieldWithMetaDate0} and
     * the GUARDED mapC form {@code _fieldWithMetaDate == null} both have count
     * 0 in the golden — the mapC-crossing deref is BARE and un-numbered, the
     * MapperS sibling guarded and plain.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_mapCDerefBareUnNumbered() {
        String gen = generated("cdm5",
                "cdm/product/template/validation/datarule/EconomicTermsFpML_cd_26_28.java");
        assertFalse(gen.contains("fieldWithMetaDate0"),
                "BROKEN LAW: the method-wide numbered group captured the mapC deref param");
        assertFalse(gen.contains("_fieldWithMetaDate == null"),
                "BROKEN LAW: the mapC (list) deref rendered the guarded MapperS form");
        assertTrue(gen.contains("_fieldWithMetaDate -> _fieldWithMetaDate.getValue()"),
                "golden shape: the BARE escaped mapC deref");
        assertTrue(gen.contains("fieldWithMetaDate -> fieldWithMetaDate == null ? null : fieldWithMetaDate.getValue()"),
                "golden shape: the guarded plain MapperS deref");
    }

    /**
     * Declared-input witness law: the OVERRIDE witness
     * {@code <CommonPeriodicPayment>map("getPeriodicPayment"} has count 0 in
     * the golden — the fn-arg last hop witnesses the callee input's declared
     * base type ({@code cde.payment.PeriodicPayment}).
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_argWitnessFollowsDeclaredInput() {
        String gen = generated("drr",
                "drr/regulation/asic/rewrite/trade/validation/datarule/ASICTransactionReportDTCC_ASIC_BR_1066_01.java");
        assertFalse(gen.contains("<CommonPeriodicPayment>map(\"getPeriodicPayment\""),
                "BROKEN LAW: the leaf-override witness at the fn-arg seat");
        assertTrue(gen.contains("<PeriodicPayment>map(\"getPeriodicPayment\""),
                "golden shape: the declared-input witness");
        assertTrue(gen.contains("import drr.standards.iosco.cde.base.payment.PeriodicPayment;"),
                "golden shape: the base type's import");
    }

    /**
     * Raw-source DEFINITION witness: the de-escaped
     * {@code optional choice type,} has count 0 in the golden — DEFINITION
     * keeps the caret-escaped {@code ^type} source form.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_caretDefinition() {
        String gen = generated("fpml",
                "fpml/consolidated/com/validation/datarule/CoalProductChoice.java");
        assertFalse(gen.contains("DEFINITION = \"optional choice type,"),
                "BROKEN LAW: the lexer's de-escaped attribute name leaked into DEFINITION");
        assertTrue(gen.contains("DEFINITION = \"optional choice ^type, coalProductSpecifications\""),
                "golden shape: the raw-source caret form");
    }

    // ==== plumbing ====

    private static String generated(String cellKey, String path) {
        Map<String, String> output = OUTPUTS.get(cellKey);
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String gen = output.get(path);
        assertNotNull(gen, "not generated: " + path + " (" + cellKey + ")");
        return gen;
    }

    private static void assertByteMatchesGolden(String cellKey, String path) throws IOException {
        String generated = generated(cellKey, path);
        Path goldenPath = D11CorpusRegressionTest.resolveGoldensDir(CELLS.get(cellKey)).resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #409 wave D).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
