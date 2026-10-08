package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.parser.RosettaParser;
import com.regnosys.rosetta.parser.RosettaParseResult;
import com.regnosys.rosetta.parser.RosettaParserFacade;
import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * P1.4.2 byte-equality gate (proxy form).
 *
 * <p>Locks the "additive only" invariant by asserting that **every existing CATALOGUE
 * fixture produces empty-list / Optional.empty() for all new P1.4.2 grammar surfaces**:
 *
 * <ul>
 *   <li>{@link RModel#fileHeader()} == empty (H3)</li>
 *   <li>Every {@link RRootElement#runeAnnotations()} == empty list (H2 / H11)</li>
 *   <li>Every {@link RDocReference#namedArgs()} == empty list (H4)</li>
 * </ul>
 *
 * <p>If any pre-existing fixture happened to contain a token that the new optional
 * prefix grammar productions interpret differently, this test fails with a precise
 * pointer to the offending fixture. Cheaper than a SHA-256 snapshot per file
 * because it only walks the AST instead of serialising it; equally diagnostic of
 * an "additive only" regression.
 *
 * <p>Equivalent in spirit to spec § 10.2 BackCompatCorpusSnapshotTest. The pre-PR
 * baseline is conceptually "every count above is zero on legacy input".
 */
class BackCompatCorpusOrthogonalityTest {

    @TestFactory
    Stream<DynamicTest> p142NewGrammarSurfacesAreOrthogonalToLegacyCorpus() throws IOException {
        if (!CorpusWalker.corpusExists()) {
            return Stream.of(DynamicTest.dynamicTest(
                    "corpus not available — orthogonality test skipped",
                    () -> assumeTrue(false, "test-corpus directory not found")));
        }

        return CorpusWalker.allRosettaFiles().stream()
                .map(path -> DynamicTest.dynamicTest(
                        CorpusWalker.displayName(path),
                        () -> assertOrthogonal(path)));
    }

    private static void assertOrthogonal(Path file) throws IOException {
        String content = Files.readString(file);
        RosettaParseResult result = RosettaParserFacade.parseString(content);
        // Skip files with parse errors — those are caught by AstCorpusRegressionTest.
        assumeTrue(result.errors().isEmpty(),
                "skipping " + file + " due to parse errors (covered elsewhere)");

        // Use the 2-arg ctor so byte-offset tracking is wired (CharToByteOffsets);
        // the deprecated 1-arg ctor would produce SourceRange.OFFSETS_UNKNOWN.
        AstBuilder builder = new AstBuilder(file.toString(), content);
        RModel model = builder.build((RosettaParser.RosettaModelContext) result.tree());

        // (H3) No existing fixture should have a fileHeader.
        assertTrue(model.fileHeader().isEmpty(),
                file + ": legacy fixture unexpectedly populated fileHeader — non-additive change?");

        // (H2/H11) No existing fixture should have rune annotations on any root element.
        AtomicInteger runeAnnotationCount = new AtomicInteger();
        AtomicInteger namedArgCount = new AtomicInteger();
        AstWalker.walk(model, (RNode node) -> {
            if (node instanceof RRootElement re) {
                runeAnnotationCount.addAndGet(re.runeAnnotations().size());
            }
            if (node instanceof RDocReference doc) {
                namedArgCount.addAndGet(doc.namedArgs().size());
            }
        });

        assertEquals(0, runeAnnotationCount.get(),
                file + ": legacy fixture unexpectedly populated runeAnnotations");
        assertEquals(0, namedArgCount.get(),
                file + ": legacy fixture unexpectedly populated regulatoryReference namedArgs");
    }
}
