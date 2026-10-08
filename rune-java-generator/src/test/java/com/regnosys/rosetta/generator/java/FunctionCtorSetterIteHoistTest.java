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
 * Facet {@code ctor_setter_ite_hoist} — a NO-ELSE single-cardinality Rosetta
 * conditional consumed at a single-expression seat (constructor pair value,
 * evaluate arg) inside a statement-hoist sink compiles as upstream's
 * ITEM-typed initializer-form local:
 *
 * <pre>
 * &lt;ItemType&gt; ifThenElseResultN = null;
 * if (&lt;cond&gt;.getOrDefault(false)) {
 *     ifThenElseResultN = &lt;ITEM-form value&gt;;
 * }
 * …
 *     .setX(ifThenElseResultN)
 * </pre>
 *
 * replacing the fork's inline {@code <cond>.getOrDefault(false) ? <MAPPER-form>
 * : MapperC.of().get()} ternary (plus the gen-only {@code MapperC} import that
 * existed only for the synthesized empty-else render). Upstream law: EVERY
 * mid-expression consumption of a compiled conditional collapses via
 * {@code JavaIfThenElseBuilder.collapseToSingleExpression} →
 * {@code declareAsVariable(true, "ifThenElseResult")} (in-tree 9.83.0
 * JavaIfThenElseBuilder.java:131-134; ctor entry
 * ExpressionGenerator.xtend:1205-1207; method args
 * JavaStatementBuilder.invokeMethod:92-112). The local types at the join of
 * the seat-coerced branch types — the absent else compiles to
 * {@code JavaLiteral.NULL} whose NULL_TYPE joins away, so the THEN branch's
 * ITEM type declares the local, and the JavaLiteral else selects the
 * initializer form ({@code declareAsVariable}'s literal-else rule). Assignment
 * values are the ITEM-form (fn calls and ctor blocks bare via the structural
 * strip, Mapper chains {@code .get()}, dotted enum constants bare); numbering
 * rides the method-spanning {@code StatementHoistSession} ifThenElseResult
 * group (singleton bare, 0..n-1 in consumption source order).
 *
 * <p>Declines (the inline ternary stays, file stays waivered): EFFECTIVE-else
 * conditionals (upstream's {@code final <T> ifThenElseResultN;} if/else-ladder
 * form — a separate facet family), MULTI-cardinality seats (the list-seat
 * golden form uses {@code Collections.<T>emptyList()}), sink-less paths
 * (rule/alias/POJO-condition emission — {@code hoistSessionEligible} freezing;
 * LAMBDA interiors — the sink walk stops at the boundary; upstream's
 * block-body lambda lift is the deferred in-lambda variant, 6 known carriers),
 * and then-branches outside the typing ladder.
 *
 * <p>Green-safe by construction: ZERO of the 34,686 frozen goldens carry the
 * {@code getOrDefault(false) ? } ternary in ANY seat (corpus-wide plain-grep,
 * wf181 green-safety seat), so a green file's gen bytes — equal to its golden
 * — contain no convertible seat; every file this facet touches is a waivered
 * FUNCTION mismatch. The full 5-cell D11 matrix (20/20) is the empirical
 * arbiter; the facet pays 93 FUNCTION flips (all drr 6.34.1 — the law-reversal
 * prediction listed 99, the 6-file remainder being the in-lambda variant).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionCtorSetterIteHoistTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrFunctionOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
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

    /**
     * Explicit-args FN-CALL then-branch ({@code subString.evaluate(…, 1, 3)}):
     * the structural strip renders the call BARE as the assignment value (the
     * gen ternary wrapped it {@code MapperS.of(…)}); two hoists number
     * {@code ifThenElseResult0/1} in consumption source order, and the
     * gen-only {@code MapperC} import (the synthesized empty-else render's
     * only use) drops with the ternary.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getCcyEsmaDrr_fnCallThenNumberingImportDrop_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/esma/emir/refit/trade/functions/GetCcy.java");
    }

    /**
     * DISGUISED 2-name chain then-branch ({@code drrReport ->
     * uniqueTransactionIdentifierProprietary} parses as {@code REnumValueRef}
     * with no resolved enumeration): the local types from the
     * enclosing-function head attribute's feature (String), the chain value
     * takes the {@code .get()} item form, and the SINGLETON group keeps the
     * bare {@code ifThenElseResult} name — at a seat three constructor levels
     * deep (the sink threads through nested ctor pair compiles).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createTradeTransaction50_1AsicDrr_disguisedChainSingleton_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/trade/functions/Create_TradeTransaction50__1.java");
    }

    /**
     * Multi-line CONSTRUCTOR then-branch: the ctor block splices bare as the
     * assignment value with its relative continuation lines re-anchored at the
     * hoist's arm depth (decl line + 1), the locals typed at the ctor's pojo
     * class ({@code LegalPersonIdentification1__1} /
     * {@code NaturalPersonIdentification3__1}), numbering 0/1.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createCounterpartySpecificDataEsmaDrr_ctorThenReanchor_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/esma/emir/refit/margin/functions/Create_CounterpartySpecificData.java");
    }

    /**
     * ENUM-CONSTANT then-branches ({@code NotApplicable1Code.NOAP}, typed from
     * the resolved enumeration, dotted constant bare) INTERLEAVED with a
     * ctor-then hoist in ONE method-spanning numbering group — partial-arm
     * firing would renumber the group (the pre-fix probe emitted
     * {@code ifThenElseResult1} where golden carries {@code 2}), so this
     * anchor pins the all-arms-fire numbering.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getCollPrtflCdAsicDrr_enumThenInterleavedNumbering_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/trade/functions/GetCollPrtflCd.java");
    }

    /**
     * TWO pojo-ctor hoists with NESTED builders carrying evaluate-arg values
     * ({@code create_OrganisationIdentification15Choice__1.evaluate(…)}) and
     * chain {@code .get()} leaves inside the re-anchored ctor blocks —
     * numbering 0/1, the deepest re-anchor shape among the anchors.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getTradClrAsicDrr_nestedCtorEvaluateArgValues_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/trade/functions/GetTradClr.java");
    }

    /**
     * SINGLETON bare-name carrier where the consuming ctor ALSO carries
     * non-conditional sibling pairs (to-string chains, evaluate args): only
     * the conditional pair converts; every sibling pair's bytes — and the
     * surrounding {@code toBuilder(…)} assignment — stay byte-identical.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createPartyIdentification248Choice2AsicDrr_singletonSiblingsUntouched_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/trade/functions/Create_PartyIdentification248Choice__2.java");
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
                + path + " — the no-else conditional's ITEM-typed initializer-form "
                + "statement hoist (its typing ladder, ITEM-form value, "
                + "ifThenElseResult numbering, or MapperC import drop) is missing or "
                + "regressed, if the ctor_setter_ite_hoist recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
