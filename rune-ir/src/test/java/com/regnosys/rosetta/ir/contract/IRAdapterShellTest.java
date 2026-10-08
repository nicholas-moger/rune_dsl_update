package com.regnosys.rosetta.ir.contract;

import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the realized {@code com.regnosys.rosetta.ir.adapter} surface — the
 * planned exit of D21 honest-limitation #1 (decision L-003).
 *
 * <p>In the P1.4.3 scaffold the adapter package was a shell holding only
 * {@code package-info.java}; P2 landed the concrete adapter classes, so the
 * original "only package-info.java" pin has been retired. This test now asserts
 * the <b>exact</b> expected class set: a removal or an unreviewed addition
 * breaks the build, keeping any change to the adapter surface a conscious,
 * reviewed event rather than a silent drift.
 *
 * <p>Path resolution uses {@link CorpusWalker#moduleRoot()} so the scan is
 * independent of JVM working directory.
 */
class IRAdapterShellTest {

    private static final Path ADAPTER_DIR = CorpusWalker.moduleRoot()
            .resolve("src/main/java/com/regnosys/rosetta/ir/adapter");

    /**
     * The expected adapter package contents, sorted. Update this list
     * deliberately when the adapter surface legitimately changes — that edit is
     * the review checkpoint this contract test exists to force.
     */
    private static final List<String> EXPECTED_FILES = List.of(
            "AstToIRAdapter.java",
            "IREnumNode.java",
            "IREnumValueNode.java",
            "IRFieldNode.java",
            "IRMetadata.java",
            // the model-level node of the property gate (v3.3 seat 8, PR #644)
            "IRModelNode.java",
            "IRNodeImpl.java",
            "IRTypeNode.java",
            "package-info.java");

    @Test
    void adapterPackageContainsExpectedClassSet() throws IOException {
        try (Stream<Path> files = Files.list(ADAPTER_DIR)) {
            List<String> names = files
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
            assertEquals(EXPECTED_FILES, names,
                    "The com.regnosys.rosetta.ir.adapter surface drifted from the " +
                            "expected class set (decision L-003, the D21 honest-limitation #1 " +
                            "exit). If this change is intended, update EXPECTED_FILES. Found: " + names);
        }
    }
}
