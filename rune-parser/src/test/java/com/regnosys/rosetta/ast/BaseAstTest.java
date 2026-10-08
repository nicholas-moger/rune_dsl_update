package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract test helper for AST tests.
 *
 * <p>Provides shared utilities for parsing Rune DSL source into typed AST nodes
 * and asserting structural properties of the resulting tree.
 *
 * <p>Subclasses should extend this to inherit the helper methods and add
 * test cases for specific AST node types.
 */
public abstract class BaseAstTest {

    /**
     * Parses the given Rune DSL source string and builds a typed AST.
     *
     * <p>The file name embedded in {@link SourceRange#file()} is derived from
     * the calling test class and method (via stack inspection), so source-range
     * assertions and error messages identify which test produced a given
     * range. Use {@link #parseAndBuild(String, String)} if you need explicit
     * control over the file name.
     *
     * @param source the Rune DSL source code
     * @return the root {@link RModel} with parent pointers set
     */
    protected RModel parseAndBuild(String source) {
        return AstBuilder.buildFromString(source, deriveTestFileName());
    }

    /**
     * Parses the given Rune DSL source string with an explicit file name and
     * builds a typed AST. Use this when a test needs to assert against a
     * specific file name (e.g., when testing multi-file diagnostics or
     * source-range origins).
     *
     * @param source   the Rune DSL source code
     * @param fileName the file name to embed in the resulting source ranges
     * @return the root {@link RModel} with parent pointers set
     */
    protected RModel parseAndBuild(String source, String fileName) {
        return AstBuilder.buildFromString(source, fileName);
    }

    /**
     * Derives a test file name like {@code "ModelNodeTest_parseFooBar.rosetta"}
     * by walking the current thread's stack to find the first frame whose
     * declaring class is the concrete test class. Falls back to
     * {@code "<TestClass>.rosetta"} if no matching frame is found.
     */
    private String deriveTestFileName() {
        String testClassName = getClass().getSimpleName();
        String testClassFqn = getClass().getName();
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (testClassFqn.equals(element.getClassName())
                    || element.getClassName().startsWith(testClassFqn + "$")) {
                String methodName = element.getMethodName();
                if (!"deriveTestFileName".equals(methodName)
                        && !"parseAndBuild".equals(methodName)) {
                    return testClassName + "_" + methodName + ".rosetta";
                }
            }
        }
        return testClassName + ".rosetta";
    }

    /**
     * Asserts that the given node has a non-{@link SourceRange#NONE} source range.
     *
     * <p>Use this to verify that the AST builder correctly populates source
     * location information for every node in the tree.
     *
     * <p>Fails with a clear assertion message if the node argument itself is
     * {@code null}, rather than dereferencing it and producing an opaque
     * {@link NullPointerException}. The {@code sourceRange} field is
     * initialised to {@link SourceRange#NONE} on construction (see
     * {@link RNode}), so a separate non-null check on the source range is
     * unnecessary — the {@code NONE} sentinel comparison covers both
     * "never set" and "explicitly set to NONE" cases.
     *
     * @param node the AST node to check; must not be {@code null}
     */
    protected void assertSourceRangeSet(RNode node) {
        assertNotNull(node, "node argument must not be null");
        assertNotEquals(SourceRange.NONE, node.sourceRange(),
                "SourceRange should not be NONE for a parsed node: " + node.getClass().getSimpleName());
    }
}
