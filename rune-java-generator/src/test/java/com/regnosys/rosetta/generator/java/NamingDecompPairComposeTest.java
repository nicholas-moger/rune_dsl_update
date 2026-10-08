package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ast.model.RModel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #380 — the naming/decomposition pair (2 byte flips, both drr POJO Rules:
 * EventTypeRule iosco cde v3 + Counterparty2IdentifierTypeRule iosco cde v1).
 *
 * <p><b>N disguisedChainEnclosingParamEscape</b> (NavigationHandler): a hop navigating
 * FROM a disguised chain ({@code REnumValueRef} with a parser-bound
 * {@code resolvedAttributeChain}) whose type-derived lambda var EQUALS an ENCLOSING
 * extract's EXPLICIT closure param shadows a real Java lambda variable — a compile
 * error, so no green file carries the unescaped form (the #360/#362 pre-escape law at
 * the disguised-chain-receiver naming arm; explicit closure params are render text,
 * never scope-registered — the #358 law, so the deferred resolution cannot see the
 * collision). Golden: {@code _workflowStep -> _workflowStep.getWorkflowState()} over
 * the {@code reportableEvent}-rooted chain inside {@code extract workflowStep [ … ]}.
 *
 * <p><b>D4a seqThenChainDecomp</b> (CollectionHandler + HandlerHelper): the RULE-path
 * sequential then-chain decomposition — three pieces. (1)
 * {@code thenChainHasUnhandledControlFlow} gains the RULE-path BASE arm: an
 * {@code extract [<ctl-free ELSELESS conditional ladder>]} base renders self-contained
 * (the #361 consumer arm's base sibling; {@code isElselessLadder} walks the else
 * spine), so the chain hoists like a ctl-free pipe — the F5 in-lambda channel hoists
 * {@code thenArg0..2} + the consumer re-roots. (2) {@code isCleanLadderContext} gains
 * the RULE-path ARGUMENT-side window read (the #356 nestedLadderRung pattern's rule
 * twin): the single-slot chain-top flag proves the hosting extract is a statement seat,
 * so the base ladder renders golden's mapSingleToList if/return block with the typed
 * {@code MapperC.<X>ofNull()} fall-through instead of the inline ternary. (3)
 * {@code bareItemThenPipeMetaType} gains the FILTER-predicate sibling arm (a filter is
 * element-preserving — the #329 M5 law), so the predicate's bare {@code item} carries
 * the binding's wrapper element and BOTH {@code areEqual} operands deref
 * ({@code referenceWithMetaParty0/1} — the per-lambda coercion group law). The two
 * #286-era converting locks graduated to byte-identity in the facet commit
 * (RuleLadderConditionalBlockTest + RuleDisguisedChainMetaRecoveryTest — the #329 law)
 * + the RuleRerootItemNavTest boundary lock at the close-out.
 *
 * <p>Whole-file byte locks run through the REAL D11 full-cell drr route and revert RED
 * without the facets; every witness token is occurrence-counted (python
 * {@code str.count} semantics — the #352 law) and PRE-counted against f-probe-379post
 * (each removal token PRE &ge; 1 / golden 0; each golden token PRE &lt; golden).
 */
class NamingDecompPairComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String EVENT_TYPE_RULE =
            "drr/standards/iosco/cde/version3/event/reports/EventTypeRule.java";
    private static final String COUNTERPARTY2_IDENTIFIER_TYPE_RULE =
            "drr/standards/iosco/cde/version1/party/reports/Counterparty2IdentifierTypeRule.java";

    private static Map<String, String> drrOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /** The REAL D11 full-cell generation path (POJO/choice/rule/report/label + functions). */
    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        funcGen.generate(output);
        return output;
    }

    // ==== byte locks (both flips through the REAL D11 full-cell drr route) ====

    /** N: EventTypeRule — the disguised-chain enclosing-param escape. */
    @Test
    @EnabledIf("drrCellAvailable")
    void eventTypeRule_byteMatchesGolden() throws IOException {
        assertDrrByteMatchesGolden(EVENT_TYPE_RULE);
    }

    /** D4a: Counterparty2IdentifierTypeRule — the sequential then-chain decomposition. */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2IdentifierType_byteMatchesGolden() throws IOException {
        assertDrrByteMatchesGolden(COUNTERPARTY2_IDENTIFIER_TYPE_RULE);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * N: the escaped {@code _workflowStep} nav param PRESENT exactly once (PRE 0 /
     * golden 1) — the outer-rooted {@code getWorkflowState} hop inside the enclosing
     * {@code extract workflowStep [ … ]} lambda.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void eventTypeRule_escapedParam_witness() {
        String gen = drrOutput.get(EVENT_TYPE_RULE);
        assertNotNull(gen, "EventTypeRule not generated");
        assertEquals(1, count(gen, "_workflowStep -> _workflowStep.getWorkflowState()"),
                "the enclosing-param escape must fire at the outer-rooted hop (PRE 0 / golden 1)");
    }

    /**
     * D4a: the base block ladder's typed empty fall-through PRESENT (PRE 0 / golden 1)
     * and the runtime {@code .then(item -> item.get())} step GONE (PRE 1 / golden 0 —
     * the negative witness the flip removes).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2_blockLadderAndRuntimeThenGone_witness() {
        String gen = drrOutput.get(COUNTERPARTY2_IDENTIFIER_TYPE_RULE);
        assertNotNull(gen, "Counterparty2IdentifierTypeRule not generated");
        assertEquals(1, count(gen, "return MapperC.<ReferenceWithMetaParty>ofNull();"),
                "the mapSingleToList block ladder's typed empty fall-through (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(item -> item.get())"),
                "the runtime only-element .then( step must be gone (PRE 1 / golden 0)");
    }

    /**
     * D4a: the filter-predicate LEFT-operand deref — the {@code areEqual(item.<Party>map(
     * "Type coercion", referenceWithMetaParty0} token counts 2 (PRE 1: the tail lambda's
     * first rung only; golden 2: the filter predicate gains its own numbered deref).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2_filterOperandDeref_witness() {
        String gen = drrOutput.get(COUNTERPARTY2_IDENTIFIER_TYPE_RULE);
        assertNotNull(gen, "Counterparty2IdentifierTypeRule not generated");
        assertEquals(2, count(gen,
                "areEqual(item.<Party>map(\"Type coercion\", referenceWithMetaParty0"),
                "both the filter predicate and the tail rung deref the item (PRE 1 / golden 2)");
    }

    /**
     * D4a: the sequential only-element re-wrap hoist PRESENT (PRE 0 / golden 1) — the
     * {@code thenArg2 = MapperS.of(thenArg1.get())} step of the decomposed chain.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2_seqHoistRewrap_witness() {
        String gen = drrOutput.get(COUNTERPARTY2_IDENTIFIER_TYPE_RULE);
        assertNotNull(gen, "Counterparty2IdentifierTypeRule not generated");
        assertEquals(1, count(gen,
                "final MapperS<ReferenceWithMetaParty> thenArg2 = MapperS.of(thenArg1.get());"),
                "the decomposed chain's only-element re-wrap hoist (PRE 0 / golden 1)");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertDrrByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for " + path);
    }
}
