package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.annotations.RAnnotation;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.external.RExternalRuleSource;
import com.regnosys.rosetta.ast.external.RExternalSynonymSource;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RBody;
import com.regnosys.rosetta.ast.regulatory.RCorpus;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.regulatory.RSegmentDef;
import com.regnosys.rosetta.ast.synonyms.RSynonymSource;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RLibraryFunction;
import com.regnosys.rosetta.ast.types.RMetaType;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ast.util.AstWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * AST-level structural comparison baseline test (Task 15, Layer 3).
 *
 * <p>Parses the CDM 6.16.0 and DRR 6.28.0 corpora through the
 * {@link AstBuilder} and counts each kind of root element node using
 * {@link AstWalker#count}. The counts must match the same baselines used by
 * the parse-tree-level
 * {@link com.regnosys.rosetta.parser.StructuralComparisonTest}.
 *
 * <p>This is the strongest end-to-end gate that the typed AST captures
 * exactly the same model elements as the legacy parse-tree extractor —
 * if the AstBuilder ever drops or duplicates a root element type, this
 * test fails immediately.
 *
 * <p>Tests are conditional — they skip if the corpus directories don't exist.
 */
class AstStructuralComparisonTest {

    private static final Path CDM_DIR = Path.of("../test-corpus/cdm/cdm-6.16.0");
    private static final Path DRR_DIR = Path.of("../test-corpus/drr/drr-6.28.0");

    // ========================================================================
    // CDM 6.16.0 baselines — same numbers as StructuralComparisonTest
    // ========================================================================

    @Test
    void cdm616AstStructuralBaseline() throws IOException {
        assumeTrue(Files.exists(CDM_DIR), "CDM 6.16.0 not available");

        Map<String, Integer> counts = countAstElements(CDM_DIR);
        printCounts("CDM 6.16.0 (AST)", counts);

        assertCount(counts, "type", 719, "CDM 6.16.0");
        assertCount(counts, "enum", 262, "CDM 6.16.0");
        assertCount(counts, "func", 1300, "CDM 6.16.0");
        assertCount(counts, "choice", 10, "CDM 6.16.0");
        assertCount(counts, "annotation", 1, "CDM 6.16.0");
        assertCount(counts, "metaType", 6, "CDM 6.16.0");
        assertCount(counts, "body", 3, "CDM 6.16.0");
        assertCount(counts, "corpus", 29, "CDM 6.16.0");
        assertCount(counts, "segment", 15, "CDM 6.16.0");
        assertCount(counts, "synonymSource", 9, "CDM 6.16.0");
        assertCount(counts, "externalSynonymSource", 11, "CDM 6.16.0");

        assertCount(counts, "rule", 0, "CDM 6.16.0");
        assertCount(counts, "report", 0, "CDM 6.16.0");
        assertCount(counts, "typeAlias", 0, "CDM 6.16.0");
        assertCount(counts, "basicType", 0, "CDM 6.16.0");
        assertCount(counts, "recordType", 0, "CDM 6.16.0");
        assertCount(counts, "libraryFunction", 0, "CDM 6.16.0");
        assertCount(counts, "externalRuleSource", 0, "CDM 6.16.0");

        int cdmTotal = counts.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(2365, cdmTotal, "CDM 6.16.0 AST total element count");
    }

    // ========================================================================
    // DRR 6.28.0 baselines — same numbers as StructuralComparisonTest
    // ========================================================================

    @Test
    void drr628AstStructuralBaseline() throws IOException {
        assumeTrue(Files.exists(DRR_DIR), "DRR 6.28.0 not available");

        Map<String, Integer> counts = countAstElements(DRR_DIR);
        printCounts("DRR 6.28.0 (AST)", counts);

        assertCount(counts, "type", 159, "DRR 6.28.0");
        assertCount(counts, "enum", 102, "DRR 6.28.0");
        assertCount(counts, "func", 1233, "DRR 6.28.0");
        assertCount(counts, "rule", 2315, "DRR 6.28.0");
        assertCount(counts, "report", 23, "DRR 6.28.0");
        assertCount(counts, "annotation", 1, "DRR 6.28.0");
        assertCount(counts, "typeAlias", 54, "DRR 6.28.0");
        assertCount(counts, "body", 21, "DRR 6.28.0");
        assertCount(counts, "corpus", 72, "DRR 6.28.0");
        assertCount(counts, "segment", 18, "DRR 6.28.0");
        assertCount(counts, "externalSynonymSource", 1, "DRR 6.28.0");
        assertCount(counts, "externalRuleSource", 6, "DRR 6.28.0");

        assertCount(counts, "choice", 0, "DRR 6.28.0");
        assertCount(counts, "basicType", 0, "DRR 6.28.0");
        assertCount(counts, "recordType", 0, "DRR 6.28.0");
        assertCount(counts, "libraryFunction", 0, "DRR 6.28.0");
        assertCount(counts, "metaType", 0, "DRR 6.28.0");
        assertCount(counts, "synonymSource", 0, "DRR 6.28.0");

        int drrTotal = counts.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(4005, drrTotal, "DRR 6.28.0 AST total element count");
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Walks every {@code .rosetta} file under {@code dir}, builds the typed
     * AST, and tallies root-element counts by kind. Returns a sorted map for
     * stable diagnostic output.
     */
    private Map<String, Integer> countAstElements(Path dir) throws IOException {
        Map<String, Integer> counts = new TreeMap<>();
        List<Path> files;
        try (var walk = Files.walk(dir)) {
            files = walk.filter(p -> p.toString().endsWith(".rosetta"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertTrue(!files.isEmpty(), "No .rosetta files found in " + dir);

        for (Path file : files) {
            RModel model = AstBuilder.buildFromFile(file);
            for (RNode root : model.rootElements()) {
                String kind = kindOf(root);
                counts.merge(kind, 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * Maps a root element node type to the same string identifier used by
     * {@link com.regnosys.rosetta.parser.RosettaStructureExtractor} so the
     * AST counts are directly comparable to the parse-tree counts.
     */
    private String kindOf(RNode node) {
        return switch (node) {
            case RDataType ignored -> "type";
            case REnumeration ignored -> "enum";
            case RFunction ignored -> "func";
            case RChoice ignored -> "choice";
            case RRule ignored -> "rule";
            case RReport ignored -> "report";
            case RAnnotation ignored -> "annotation";
            case RTypeAlias ignored -> "typeAlias";
            case RBasicType ignored -> "basicType";
            case RRecordType ignored -> "recordType";
            case RLibraryFunction ignored -> "libraryFunction";
            case RMetaType ignored -> "metaType";
            case RBody ignored -> "body";
            case RCorpus ignored -> "corpus";
            case RSegmentDef ignored -> "segment";
            // PR #450: RExternalSynonymSource now extends RSynonymSource (the
            // upstream Ecore hierarchy) — the subtype case must precede the
            // supertype case or the supertype label dominates it.
            case RExternalSynonymSource ignored -> "externalSynonymSource";
            case RSynonymSource ignored -> "synonymSource";
            case RExternalRuleSource ignored -> "externalRuleSource";
            default -> "unknown:" + node.getClass().getSimpleName();
        };
    }

    private void assertCount(Map<String, Integer> counts, String kind, int expected, String corpus) {
        int actual = counts.getOrDefault(kind, 0);
        assertEquals(expected, actual, kind + " count mismatch in " + corpus);
    }

    private void printCounts(String label, Map<String, Integer> counts) {
        if (System.getProperty("printStructure") == null) {
            return;
        }
        System.out.println("\n=== AST structural counts for " + label + " ===");
        int total = 0;
        for (var entry : counts.entrySet()) {
            System.out.printf("  %-30s %d%n", entry.getKey(), entry.getValue());
            total += entry.getValue();
        }
        System.out.printf("  %-30s %d%n", "TOTAL", total);
        System.out.println();
    }
}
