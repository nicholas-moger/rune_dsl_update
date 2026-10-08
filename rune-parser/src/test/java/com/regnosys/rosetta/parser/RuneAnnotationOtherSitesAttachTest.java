package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.REnumeration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end attach tests for runeAnnotations? on choice / enumeration / function
 * (P1.4.2 H2 / T5). DataType is exercised in {@link RuneAnnotationDataTypeAttachTest}.
 */
class RuneAnnotationOtherSitesAttachTest {

    @Test
    void choiceWithRuneAnnotation() {
        String src = """
                namespace foo

                type FooType:
                  fa string (1..1)

                type BazType:
                  ba string (1..1)

                @experimental
                choice Bar:
                  FooType
                  BazType
                """;
        RModel model = parse(src);
        RChoice c = (RChoice) model.rootElements().stream()
            .filter(r -> r instanceof RChoice ch && "Bar".equals(ch.name()))
            .findFirst().orElseThrow();
        assertEquals(1, c.runeAnnotations().size());
        assertEquals("experimental", c.runeAnnotations().get(0).annotationName());
    }

    @Test
    void enumerationWithRuneAnnotation() {
        String src = """
                namespace foo

                @experimental
                enum Status:
                  ACTIVE
                  INACTIVE
                """;
        RModel model = parse(src);
        REnumeration e = (REnumeration) model.rootElements().get(0);
        assertEquals(1, e.runeAnnotations().size());
        assertEquals("experimental", e.runeAnnotations().get(0).annotationName());
    }

    @Test
    void functionWithRuneAnnotation() {
        String src = """
                namespace foo

                @experimental
                func Compute:
                  inputs:
                    x string (1..1)
                  output:
                    result string (1..1)
                """;
        RModel model = parse(src);
        RFunction f = (RFunction) model.rootElements().get(0);
        assertEquals(1, f.runeAnnotations().size());
        assertEquals("experimental", f.runeAnnotations().get(0).annotationName());
    }

    @Test
    void choiceWithoutRuneAnnotationsParsesUnchanged() {
        String src = """
                namespace foo

                type FooType:
                  fa string (1..1)

                type BazType:
                  ba string (1..1)

                choice Bar:
                  FooType
                  BazType
                """;
        RModel model = parse(src);
        RChoice c = (RChoice) model.rootElements().stream()
            .filter(r -> r instanceof RChoice ch && "Bar".equals(ch.name()))
            .findFirst().orElseThrow();
        assertTrue(c.runeAnnotations().isEmpty());
    }

    @Test
    void enumerationWithMultiArgRuneAnnotation() {
        String src = """
                namespace foo

                @feature(name = "strict", since = "0.2")
                enum Status:
                  ACTIVE
                  INACTIVE
                """;
        RModel model = parse(src);
        REnumeration e = (REnumeration) model.rootElements().get(0);
        assertEquals(1, e.runeAnnotations().size());
        var args = e.runeAnnotations().get(0).arguments();
        assertEquals(2, args.size());
        assertEquals("name", args.get(0).name());
        assertEquals("strict", args.get(0).valueAsString());
    }

    @Test
    void functionWithMultipleRuneAnnotations() {
        String src = """
                namespace foo

                @experimental
                @beta(version = "1")
                func Compute:
                  inputs:
                    x string (1..1)
                  output:
                    result string (1..1)
                """;
        RModel model = parse(src);
        RFunction f = (RFunction) model.rootElements().get(0);
        assertEquals(2, f.runeAnnotations().size());
        assertEquals("experimental", f.runeAnnotations().get(0).annotationName());
        assertEquals("beta", f.runeAnnotations().get(1).annotationName());
    }

    private static RModel parse(String src) {
        return TestParseHelper.parseModel(src);
    }
}
