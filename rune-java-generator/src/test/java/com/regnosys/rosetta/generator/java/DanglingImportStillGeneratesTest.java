package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.D11CorpusRegressionTest.CellSpec;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * v3.1 phase C, C0 item 2 — the must-still-generate test for the refusal policy's
 * BLOCKING PREDICATE.
 *
 * <p><b>The policy.</b> An element whose body USES an unresolved reference refuses
 * rather than emitting an unbound Java identifier. The predicate is per-element BODY
 * USE — deliberately NOT file-level diagnostics.
 *
 * <p><b>Why the distinction is load-bearing, and why this test exists.</b> cdm 6.21.0
 * carries two dangling {@code import cdm.base.staticdata.codelist.*} statements
 * ({@code ingest-fpml-confirmation-datetime-func.rosetta} and
 * {@code -other-func.rosetta}) for a namespace the model declares nowhere. The linker
 * reports them, upstream tolerates them, nothing references them — and the files'
 * generated output byte-matches the goldens today. A coarse "this file has an unresolved
 * reference, refuse it" rule would turn those passing files into missing output:
 * a regression manufactured by the safety mechanism itself. The plan's rev-3 review
 * (R2-6) called that out and required this cell as the committed guard, so the policy
 * cannot be coarsened later without a red test.
 *
 * <p>Skips when the corpus is absent (CI / fresh clone).
 */
class DanglingImportStillGeneratesTest {

    private static final CellSpec CDM_6_21_0 = new CellSpec("cdm", "6.21.0",
            java.nio.file.Path.of("..", "test-corpus", "cdm", "cdm-6.21.0"));

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    @Test
    void unusedDanglingImportsDoNotBlockGeneration() throws Exception {
        assumeTrue(Files.isDirectory(D11CorpusRegressionTest.resolveRosettaInputDir(CDM_6_21_0)),
                "cdm 6.21.0 corpus cell absent");

        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(CDM_6_21_0);
        var generatorModel = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(CDM_6_21_0));

        // The linker DOES report the dangling imports — the precondition for this test to
        // mean anything. If it stops, the cell no longer witnesses the policy.
        boolean danglingReported = corpus.workspace().linkingDiagnostics().stream()
                .anyMatch(d -> d.unresolvedName().contains("codelist"));
        assumeTrue(danglingReported,
                "cdm 6.21.0 no longer reports the dangling codelist import — re-derive the witness");

        var generator = new ModelObjectGenerator(generatorModel, TYPE_TRANSLATOR, TYPE_UTIL);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (generatorModel.shouldGenerate(model)) {
                errors.addAll(generator.generateClasses(model, generatorModel.version(model), output));
            }
        }

        assertTrue(errors.isEmpty(),
                () -> "the unused dangling imports must not refuse ANY element — the blocking "
                        + "predicate is per-element BODY USE, never file-level diagnostics. Refused: "
                        + errors.stream().map(GenerationException::getMessage).limit(5).toList());
        assertFalse(output.isEmpty(), "expected the cell to emit POJOs");
    }
}
