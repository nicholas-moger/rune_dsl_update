package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks the back-compat 2-arg constructor of {@link RosettaParseResult}.
 *
 * <p>Pre-P1.4.1c shape: {@code RosettaParseResult(ParseTree, List<String>)}.
 * P1.4.1c added {@code List<RDiagnostic> diagnostics} as a 3rd record component.
 * Without an explicit 2-arg secondary constructor, any external caller using
 * the legacy 2-arg form would fail to compile.
 *
 * <p>This test exercises the deprecated 2-arg constructor directly to ensure
 * back-compat survives across the structural change (independent reviewer
 * finding C1 on round 1). Uses a real ParseTree from
 * {@link RosettaParserFacade#parseString(String)} rather than mocking — no
 * Mockito on the rune-parser test classpath.
 */
@SuppressWarnings("deprecation")
class RosettaParseResultLegacyConstructorTest {

    private static ParseTree validTree() {
        return RosettaParserFacade.parseString(
            "namespace com.test\ntype Foo:\n  a string (1..1)\n").tree();
    }

    @Test
    void twoArgConstructorBuildsValidResult() {
        ParseTree tree = validTree();
        RosettaParseResult result = new RosettaParseResult(tree, List.of("1:0 sample error"));
        assertNotNull(result);
        assertEquals(tree, result.tree());
        assertEquals(List.of("1:0 sample error"), result.errors());
        assertNotNull(result.diagnostics(), "diagnostics() must be non-null even via 2-arg ctor");
        assertTrue(result.diagnostics().isEmpty(),
            "diagnostics() defaults to empty list when ctor doesn't supply structured diagnostics");
    }

    @Test
    void twoArgConstructorIsAnnotatedDeprecated() throws NoSuchMethodException {
        var ctor = RosettaParseResult.class.getConstructor(ParseTree.class, List.class);
        Deprecated dep = ctor.getAnnotation(Deprecated.class);
        assertNotNull(dep, "2-arg constructor must be @Deprecated");
        assertEquals("0.1.0", dep.since());
        assertFalse(dep.forRemoval());
    }

    @Test
    void threeArgConstructorIsCanonical() {
        ParseTree tree = validTree();
        RosettaParseResult result = new RosettaParseResult(tree, List.of(), List.<RDiagnostic>of());
        assertNotNull(result);
    }
}
