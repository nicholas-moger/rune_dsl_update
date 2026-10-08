package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engine PR #147 (facet {@code date_record_feature_nav}) — guards that navigating the
 * built-in {@code date} record feature off a {@code zonedDateTime}/{@code dateTime}
 * receiver renders the upstream {@code RecordJavaUtil} lambda
 * {@code .<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))}, not the non-compiling
 * POJO-getter form {@code .map("getDate", zonedDateTime -> zonedDateTime.getDate())}
 * ({@code ZonedDateTime}/{@code LocalDateTime} have no {@code getDate()}).
 *
 * <p>Root cause: a built-in record value carries no {@code RAttribute} for its features,
 * so {@code RFeatureCall.resolvedFeature()} is empty and
 * {@code NavigationHandler.fallbackResolveFeature} returns {@code null} (a record is not
 * an {@code RDataType}); the generic getter path then synthesised {@code getDate()}.
 * {@code NavigationHandler.tryRecordFeatureNav} detects the record receiver via
 * {@code resolveTypeCall(receiver) instanceof RRecordType} and emits the
 * {@code RecordJavaUtil}-faithful lambda (var {@code zdt} for {@code zonedDateTime} /
 * {@code dt} for {@code dateTime}), with the {@code <Date>} witness +
 * {@code com.rosetta.model.lib.records.Date} import.
 *
 * <p>Probe: {@code MapZoneDateTimeToDate} ({@code fpmlZoneDateTime -> date}) is the clean
 * SOLE flip — its only residual diff was this record-feature lambda. This exercises the
 * {@code ZONED_DATE_TIME} branch (var {@code zdt}); the {@code DATE_TIME} branch (var
 * {@code dt} — drr {@code IsActionTypePositionMODI}, differing only by the kind-keyed var
 * literal) is exercised by the full 5-cell D11 byte-comparison.
 *
 * <p>Determinism follows {@link FunctionGeneratorImportCollectionTest} /
 * {@link FunctionMultiArgGetMultiTest}: the corpus is the version-frozen {@code cdm/6.20.6}
 * cell, so the generator output is a pure function of the fork's code — this test flips
 * only when the generator changes.
 */
class FunctionRecordDateNavTest {

    private static final Path CDM_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/generated/java");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    private static Map<String, String> functionOutput;

    static boolean cdmAvailable() {
        if (!Files.isDirectory(CDM_ROSETTA_DIR) || !Files.isDirectory(CDM_GOLDEN_DIR)) {
            return false;
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            return stream.anyMatch(p -> p.toString().endsWith(".rosetta"));
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeAll
    static void generateAllFunctions() throws IOException {
        if (!cdmAvailable()) return;
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        functionOutput = new LinkedHashMap<>();
        gen.generate(functionOutput);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void zonedDateTimeToDateRecordNav_emitsRecordFeatureLambda_notGetterForm() {
        String body = requireGenerated(
                "cdm/ingest/fpml/confirmation/datetime/functions/MapZoneDateTimeToDate.java");

        // zonedDateTime -> date: the RecordJavaUtil lambda (var zdt, Date.of(zdt.toLocalDate())).
        String recordFeatureTail = ".<Date>map(\"Date\", zdt -> Date.of(zdt.toLocalDate()))";
        assertTrue(body.contains(recordFeatureTail),
                "record-feature `date` nav off a zonedDateTime receiver must render the "
                + "RecordJavaUtil lambda — tryRecordFeatureNav did not fire. Body:\n" + body);

        // The non-compiling POJO-getter form (ZonedDateTime has no getDate()) must be gone.
        assertFalse(body.contains("zonedDateTime.getDate()"),
                "the non-compiling .getDate() getter form of the record-feature nav must be gone");
        assertFalse(body.contains("\"getDate\""),
                "the getDate getter-name string of the record-feature nav must be gone");

        // The <Date> witness import must be present for the source to compile.
        assertTrue(body.contains("import com.rosetta.model.lib.records.Date;"),
                "the com.rosetta.model.lib.records.Date import (for the <Date> witness + "
                + "Date.of body) must be emitted. Body:\n" + body);
    }

    // =========================================================================
    // Helpers (mirror FunctionMultiArgGetMultiTest)
    // =========================================================================

    private static String requireGenerated(String path) {
        assertNotNull(functionOutput,
                "Function generation did not run — CDM corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        return generated.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static RLinkingResult loadFullCorpus() throws IOException {
        List<RModel> models = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.walk(BUILTINS_DIR)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) {
                              skipped.add(p + ": " + e.getMessage());
                          }
                      });
            }
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          RModel model = AstBuilder.buildFromFile(p);
                          model.setVersion("0.0.0.master-SNAPSHOT");
                          models.add(model);
                      } catch (Exception e) {
                          skipped.add(p + ": " + e.getMessage());
                      }
                  });
        }
        if (!skipped.isEmpty()) {
            throw new IOException(
                    "Corpus loader failed to parse " + skipped.size()
                            + " file(s); refusing to produce partial workspace: "
                            + String.join("; ", skipped));
        }
        return RWorkspace.build(models);
    }
}
