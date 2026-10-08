package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
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
 * Anchor for PR #320's TWO disjoint green-safe GENERATOR facets (parser UNTOUCHED).
 *
 * <p><b>Facet A — corpusJoin</b> ({@code ModelGeneratorUtil.appendDocReference}): ALL corpus refs of
 * ONE {@code [regulatoryReference <body> <corpus1> <corpus2> …]} doc reference render on a SINGLE
 * javadoc line ({@code * Corpus <c1> … * Corpus <c2> …}, like the segment refs), matching upstream —
 * the per-corpus newline moves out of the loop. The fork emitted one {@code * Corpus} line per
 * corpus, which never byte-matched: 0 of the 34,686 goldens carry consecutive {@code * Corpus}
 * lines (plain-grep over corpus-baseline-9.83), and the 6 joined-form golden carriers are all
 * already-waivered drr POJO files. A 0-/1-corpus reference renders byte-identically (only the
 * newline placement moves, never the content) — green-safe by construction. Flip carrier:
 * {@code PartiesToTheDerivative} (fca ukemir refit margin, two 2-corpus references, SOLE); the 5
 * co-occupied Report classes move toward golden.
 *
 * <p><b>Facet B — distinctMetaThenArg</b> ({@code FunctionExpressionRenderer}): a
 * {@code then extract assignedIdentifier -> identifier} producer (a DISGUISED 2-name chain whose
 * {@code identifier} leaf is {@code [metadata scheme]}) piped through STANDALONE element-preserving
 * reshapes ({@code then flatten} / {@code then distinct}) into a {@code then only-element} collapse.
 * The fork (1) declared the reshape thenArgs with the BARE element
 * ({@code MapperC<String> thenArgN = distinct(<MapperC<FieldWithMetaString>>)} — a generics
 * mismatch, NON_COMPILING) and (2) assigned the collapsed wrapper bare to the {@code String}
 * output. Golden keeps the wrapper element through the reshapes and hoists
 * {@code final FieldWithMetaString fieldWithMetaString = MapperS.of(thenArgN.get()).get();} +
 * null-guards {@code output = fieldWithMetaString.getValue();}. F1: the #297
 * {@code thenArgFilterMetaWrapper} anchor gains a THIRD disjunct
 * ({@code navProducerAnchorsPrevMeta}) — walk back from the preserving op over standalone pipe
 * reshapes to the REAL producer and require the #290 nav-only walker
 * ({@code recoverCollapsedProducerMeta}) to resolve EXACTLY prevRef's wrapper element. F2: the #290
 * {@code collapsedPipeMeta} producer resolution walks back over the same reshapes (the #290
 * carriers' producer at {@code n-2} is not a reshape — bytes unchanged). The #297 UTI/USI
 * over-fire family declines twice over: its conditional block-lambda producer is the chain BASE
 * (walk-back {@code pj < 0}), and a conditional is invisible to the nav-only walker. Flip
 * carriers: {@code SubsequentPositionUTIRule} + {@code SecondaryTransactionIdentifierRule}
 * (common trade link); {@code DTCC_DeliveryLocationRule} + 2 drr FUNCTION files (the
 * #297-precedent shared seat) move toward golden.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED 5/7 — the 3 flip locks
 * + 2 positive-content locks fail on clean source; the 2 green-safety locks (single-corpus enum
 * byte-identity, ASIC distinct-over-base-producer decline) pass either way.
 */
class RuleCorpusJoinDistinctMetaTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Facet A flip carrier: two [regulatoryReference FCA UKEMIR PS23/2 Dissemination …] doc
    // references, each with TWO corpus refs — golden joins each pair on ONE javadoc line.
    private static final String PARTIES_TO_THE_DERIVATIVE =
            "drr/regulation/fca/ukemir/refit/margin/PartiesToTheDerivative.java";
    // Facet B flip carriers: … then extract assignedIdentifier -> identifier
    // then [flatten] then distinct then only-element (common trade link).
    private static final String SUBSEQUENT_POSITION_UTI =
            "drr/regulation/common/trade/link/reports/SubsequentPositionUTIRule.java";
    private static final String SECONDARY_TRANSACTION_IDENTIFIER =
            "drr/regulation/common/trade/link/reports/SecondaryTransactionIdentifierRule.java";
    // Facet A green-safety pin: a GREEN single-corpus javadoc carrier — must stay byte-identical
    // (the joined-form change only touches MULTI-corpus references).
    private static final String CFTC_ENTITY_CLASSIFICATION_ENUM =
            "drr/regulation/common/CFTCEntityClassificationEnum.java";
    // Facet B green-safety/decline pin: the #297 over-fire family — distinct over a thenArg0
    // produced by the chain BASE (a conditional block-lambda). The walk-back lands at pj < 0 and
    // declines, so the decl keeps the bare element (golden derefs INSIDE that lambda instead).
    private static final String ASIC_UTI_MARGIN =
            "drr/regulation/asic/rewrite/margin/reports/ASICUniqueTransactionIdentifierRule.java";

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

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = new EnumGenerator(gm);
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
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
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

    // ==== Flip locks (revert-RED): the carriers now byte-match golden. ====

    @Test
    @EnabledIf("drrCellAvailable")
    void partiesToTheDerivative_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(PARTIES_TO_THE_DERIVATIVE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void subsequentPositionUti_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(SUBSEQUENT_POSITION_UTI);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void secondaryTransactionIdentifier_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(SECONDARY_TRANSACTION_IDENTIFIER);
    }

    // ==== Positive-content locks (revert-RED). ====

    /**
     * Facet A: both 2-corpus doc references join their corpus clauses on ONE line — the
     * {@code Dissemination Margin} clause rides the SAME line as the {@code FCA_BoEPolicyStatement}
     * clause (after its trailing space), never at the start of its own javadoc line.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void partiesToTheDerivative_corpusClausesJoined() {
        String gen = normalize(gen(PARTIES_TO_THE_DERIVATIVE));
        assertTrue(gen.contains("guidance\"  * Corpus Dissemination Margin"),
                "Expected the two Corpus clauses of one doc reference JOINED on a single line");
        assertTrue(!gen.contains("\n\t * Corpus Dissemination"),
                "The second Corpus clause must NOT start its own javadoc line (the fork's split form)");
    }

    /**
     * Facet B: the distinct thenArg keeps the producer's {@code FieldWithMetaString} element
     * (F1) and the collapsed whole-output derefs the wrapper via the hoist + null-guard (F2) —
     * NOT the fork's bare {@code MapperC<String>} decl + bare wrapper-into-String assignment.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void subsequentPositionUti_wrapperKeptAndOutputDerefd() {
        String gen = gen(SUBSEQUENT_POSITION_UTI);
        assertTrue(gen.contains("final MapperC<FieldWithMetaString> thenArg3 = distinct(thenArg2);"),
                "Expected the distinct thenArg to keep the producer's FieldWithMetaString element (F1)");
        assertTrue(gen.contains(
                "final FieldWithMetaString fieldWithMetaString = MapperS.of(thenArg3.get()).get();"),
                "Expected the collapsed wrapper hoisted to a final local (F2)");
        assertTrue(gen.contains("output = fieldWithMetaString.getValue();"),
                "Expected the null-guarded whole-output deref (F2)");
        assertTrue(!gen.contains("final MapperC<String> thenArg3"),
                "The fork's bare-element distinct decl must be gone");
    }

    // ==== Green-safety / decline locks (pass on clean source too). ====

    /**
     * Facet A green-safety: a GREEN single-corpus javadoc carrier stays byte-identical — the
     * corpus-join change moves only the newline placement, so a 0-/1-corpus doc reference
     * renders exactly the pre-#320 bytes.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void cftcEntityClassificationEnum_singleCorpusStaysByteIdentical() throws IOException {
        assertByteMatchesGolden(CFTC_ENTITY_CLASSIFICATION_ENUM);
    }

    /**
     * Facet B green-safety (the #297 over-fire family declines): the ASIC variant's
     * {@code distinct(thenArg0)} sits over a thenArg produced by the chain BASE — a conditional
     * block-lambda whose return golden derefs INSIDE the lambda (a bare-joined arm). The
     * walk-back lands at {@code pj < 0} and declines, so the decl keeps the bare
     * {@code MapperC<String>} element — no over-fire. The file itself stays divergent
     * (co-occupied with cardinality + meta-deref); only the decline is asserted. Passes on
     * clean source too.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void asicUniqueTransactionIdentifier_distinctOverBaseProducerStaysBare() {
        String gen = gen(ASIC_UTI_MARGIN);
        assertTrue(gen.contains("final MapperC<String> thenArg1 = distinct(thenArg0);"),
                "Expected the distinct over the BASE-produced thenArg0 to keep the bare element"
                + " (the walk-back pj < 0 decline)");
        assertTrue(!gen.contains("final MapperC<FieldWithMetaString> thenArg1 = distinct(thenArg0);"),
                "The base-producer distinct must NOT keep the wrapper (over-fire guard)");
    }

    private static String gen(String path) {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String g = drrOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for "
                + path + " (PR #320 corpusJoin + distinctMetaThenArg).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
