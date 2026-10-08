package com.regnosys.rosetta.ast.sourcerange;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that an AST built via {@link AstBuilder#buildFromString(String, String)}
 * has populated byte offsets on every parsed node (not the {@link SourceRange#NONE}
 * sentinel). Synthesised nodes are allowed to keep {@code NONE}.
 *
 * <p>Per H5 / D13 (byte-offset population through the AstBuilder pipeline).
 */
class AstCorpusByteOffsetTest {

    @Test
    void everyParsedNodeHasPopulatedByteOffsetsOrIsNone() throws Exception {
        Path file = Path.of("src", "test", "resources", "snippets", "builtins", "basic-types.rosetta");
        assertTrue(Files.exists(file), "fixture missing: " + file.toAbsolutePath());

        String content = Files.readString(file);
        RModel model = AstBuilder.buildFromString(content, file.toString());

        AtomicInteger checked = new AtomicInteger(0);
        AtomicInteger populated = new AtomicInteger(0);

        AstWalker.walk(model, (RNode node) -> {
            SourceRange r = node.sourceRange();
            checked.incrementAndGet();
            if (!r.equals(SourceRange.NONE)) {
                assertTrue(
                    r.hasByteOffsets(),
                    "node " + node.getClass().getSimpleName()
                        + " has populated source range without byte offsets: " + r
                );
                assertTrue(
                    r.startOffset() <= r.endOffset(),
                    "byte offsets misordered on " + node.getClass().getSimpleName() + ": " + r
                );
                populated.incrementAndGet();
            }
        });

        assertTrue(checked.get() > 5, "expected to walk >5 nodes; got " + checked.get());
        assertTrue(populated.get() > 5, "expected to find >5 populated source ranges; got " + populated.get());
    }

    @Test
    void deprecatedFileNameOnlyConstructorYieldsSentinelOffsets() throws Exception {
        // The legacy AstBuilder(String fileName) constructor (pre-PR1a) produces
        // sentinel offsets. Confirms backward-compat path still works without
        // throwing — consumers calling the old constructor pre-upgrade keep working.
        Path file = Path.of("src", "test", "resources", "snippets", "builtins", "basic-types.rosetta");
        String content = Files.readString(file);

        @SuppressWarnings("deprecation")
        AstBuilder legacyBuilder = new AstBuilder(file.toString());
        // Use the legacy parse + build dance (caller passes content separately to parseString)
        var parseResult = com.regnosys.rosetta.parser.RosettaParserFacade.parseString(content);
        assertEquals(0, parseResult.errors().size());
        RModel model = legacyBuilder.build(
            (com.regnosys.rosetta.parser.RosettaParser.RosettaModelContext) parseResult.tree());

        AtomicInteger sentinelCount = new AtomicInteger(0);
        AstWalker.walk(model, (RNode node) -> {
            SourceRange r = node.sourceRange();
            if (!r.equals(SourceRange.NONE) && !r.hasByteOffsets()) {
                sentinelCount.incrementAndGet();
            }
        });
        assertTrue(sentinelCount.get() > 0,
            "legacy constructor should produce sentinel offsets on parsed nodes");
    }
}
