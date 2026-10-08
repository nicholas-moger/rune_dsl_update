package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.ast.annotations.RRuneAnnotation;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end attach test for runeAnnotations? on dataType (P1.4.2 H2 / T4).
 *
 * <p>Verifies that {@code @experimental type Foo:} parses and surfaces on
 * {@link RDataType#runeAnnotations()}. Foundation milestone — this test
 * passing is the trigger to open the PR.
 */
class RuneAnnotationDataTypeAttachTest {

    @Test
    void dataTypeWithSingleRuneAnnotation() {
        String src = """
                namespace foo

                @experimental
                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RDataType bar = (RDataType) model.rootElements().get(0);
        assertEquals(1, bar.runeAnnotations().size());
        assertEquals("experimental", bar.runeAnnotations().get(0).annotationName());
    }

    @Test
    void dataTypeWithMultipleRuneAnnotations() {
        String src = """
                namespace foo

                @experimental
                @beta(version = "1")
                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RDataType bar = (RDataType) model.rootElements().get(0);
        List<RRuneAnnotation> ann = bar.runeAnnotations();
        assertEquals(2, ann.size());
        assertEquals("experimental", ann.get(0).annotationName());
        assertEquals("beta", ann.get(1).annotationName());
        assertEquals(1, ann.get(1).arguments().size());
        assertEquals("version", ann.get(1).arguments().get(0).name());
        assertEquals("1", ann.get(1).arguments().get(0).valueAsString());
    }

    @Test
    void dataTypeWithoutRuneAnnotationParsesUnchanged() {
        String src = """
                namespace foo

                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RDataType bar = (RDataType) model.rootElements().get(0);
        assertTrue(bar.runeAnnotations().isEmpty());
        assertEquals("Bar", bar.name());
    }

    @Test
    void dataTypeWithMultiArgRuneAnnotation() {
        String src = """
                namespace foo

                @feature(name = "strictTypes", since = "0.2")
                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RDataType bar = (RDataType) model.rootElements().get(0);
        RRuneAnnotation a = bar.runeAnnotations().get(0);
        assertEquals("feature", a.annotationName());
        assertEquals(2, a.arguments().size());
        assertEquals("name", a.arguments().get(0).name());
        assertEquals("strictTypes", a.arguments().get(0).valueAsString());
        assertEquals("since", a.arguments().get(1).name());
        assertEquals("0.2", a.arguments().get(1).valueAsString());
    }

    @Test
    void runeAnnotationWithIntLiteralArg() {
        String src = """
                namespace foo

                @since(version = 1)
                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RDataType bar = (RDataType) model.rootElements().get(0);
        RRuneAnnotation a = bar.runeAnnotations().get(0);
        assertEquals("since", a.annotationName());
        assertEquals(1, a.arguments().size());
        assertEquals("version", a.arguments().get(0).name());
        assertEquals("1", a.arguments().get(0).valueAsString());
    }

    @Test
    void runeAnnotationWithBooleanArg() {
        String src = """
                namespace foo

                @flag(enabled = True)
                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RDataType bar = (RDataType) model.rootElements().get(0);
        RRuneAnnotation a = bar.runeAnnotations().get(0);
        assertEquals("flag", a.annotationName());
        assertEquals("enabled", a.arguments().get(0).name());
        assertEquals("True", a.arguments().get(0).valueAsString());
    }

    @Test
    void runeAnnotationWithQualifiedNameArg() {
        String src = """
                namespace foo

                type Other:
                  y int (1..1)

                @feature(target = my.pkg.Other)
                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RDataType bar = (RDataType) model.rootElements().stream()
            .filter(r -> r instanceof RDataType d && "Bar".equals(d.name()))
            .map(r -> (RDataType) r).findFirst().orElseThrow();
        RRuneAnnotation a = bar.runeAnnotations().get(0);
        assertEquals("feature", a.annotationName());
        assertEquals("target", a.arguments().get(0).name());
        assertEquals("my.pkg.Other", a.arguments().get(0).valueAsString());
    }

    @Test
    void runeAnnotationWithEmptyParens() {
        String src = """
                namespace foo

                @experimental()
                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RDataType bar = (RDataType) model.rootElements().get(0);
        RRuneAnnotation a = bar.runeAnnotations().get(0);
        assertEquals("experimental", a.annotationName());
        assertTrue(a.arguments().isEmpty(),
            "@foo() with empty parens parses to zero-args annotation");
    }

    @Test
    void runeAnnotationOnDataTypeWithExtends() {
        String src = """
                namespace foo

                type Base:
                  shared string (1..1)

                @experimental
                type Derived extends Base:
                  extra string (1..1)
                """;
        RModel model = parse(src);
        RDataType derived = (RDataType) model.rootElements().stream()
            .filter(r -> r instanceof RDataType d && "Derived".equals(d.name()))
            .map(r -> (RDataType) r).findFirst().orElseThrow();
        assertEquals(1, derived.runeAnnotations().size());
        assertEquals("experimental", derived.runeAnnotations().get(0).annotationName());
        // verify extends still parses (super-type-name field populated)
        assertEquals("Base", derived.superTypeName().orElseThrow());
    }

    @Test
    void runeAndBracketAnnotationsCoexistSourceLevel() {
        // Both rune-style and bracket-style annotations attach independently;
        // bracket annotations are on attribute-level (annotationRef) and remain
        // unchanged by the rune annotation prefix on the type.
        String src = """
                namespace foo

                @experimental
                type Bar:
                  x string (1..1)
                    [metadata key]
                """;
        RModel model = parse(src);
        RDataType bar = (RDataType) model.rootElements().get(0);
        assertEquals(1, bar.runeAnnotations().size(),
            "rune annotation @experimental on type populates runeAnnotations");
        assertEquals("experimental", bar.runeAnnotations().get(0).annotationName());
    }

    private static RModel parse(String src) {
        return TestParseHelper.parseModel(src);
    }
}
