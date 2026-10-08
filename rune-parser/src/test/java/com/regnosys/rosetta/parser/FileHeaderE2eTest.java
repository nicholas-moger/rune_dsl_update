package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.ast.RFileHeader;
import com.regnosys.rosetta.ast.model.RModel;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for the H3 fileHeader: block (P1.4.2 / T10).
 */
class FileHeaderE2eTest {

    @Test
    void fileWithoutHeaderHasEmptyOptional() {
        String src = """
                namespace foo

                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        assertTrue(model.fileHeader().isEmpty());
    }

    @Test
    void fileHeaderWithVersionField() {
        String src = """
                fileHeader:
                  version "1.2.0"
                namespace foo

                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        Optional<RFileHeader> hdr = model.fileHeader();
        assertTrue(hdr.isPresent());
        assertEquals(Optional.of("1.2.0"), hdr.get().version());
        assertTrue(hdr.get().dependsOn().isEmpty());
        assertTrue(hdr.get().experimental().isEmpty());
    }

    @Test
    void fileHeaderWithDependsOnField() {
        String src = """
                fileHeader:
                  depends-on "cdm.base", "iso20022.fpml"
                namespace foo

                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RFileHeader hdr = model.fileHeader().orElseThrow();
        assertTrue(hdr.version().isEmpty());
        assertEquals(2, hdr.dependsOn().size());
        assertEquals("cdm.base", hdr.dependsOn().get(0));
        assertEquals("iso20022.fpml", hdr.dependsOn().get(1));
    }

    @Test
    void fileHeaderWithExperimentalField() {
        String src = """
                fileHeader:
                  experimental: [strictTypes, multiVersion]
                namespace foo

                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RFileHeader hdr = model.fileHeader().orElseThrow();
        assertEquals(2, hdr.experimental().size());
        assertEquals("strictTypes", hdr.experimental().get(0));
        assertEquals("multiVersion", hdr.experimental().get(1));
    }

    @Test
    void fileHeaderWithAllFields() {
        String src = """
                fileHeader:
                  version "1.2.0"
                  depends-on "cdm.base"
                  experimental: [strict]
                namespace foo

                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RFileHeader hdr = model.fileHeader().orElseThrow();
        assertEquals(Optional.of("1.2.0"), hdr.version());
        assertEquals(1, hdr.dependsOn().size());
        assertEquals("cdm.base", hdr.dependsOn().get(0));
        assertEquals(1, hdr.experimental().size());
        assertEquals("strict", hdr.experimental().get(0));
    }

    @Test
    void fileHeaderEmptyBlock() {
        String src = """
                fileHeader:
                namespace foo

                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        RFileHeader hdr = model.fileHeader().orElseThrow();
        assertTrue(hdr.version().isEmpty());
        assertTrue(hdr.dependsOn().isEmpty());
        assertTrue(hdr.experimental().isEmpty());
    }

    @Test
    void namespaceLevelVersionDeclStillWorksIndependently() {
        // versionDecl is the existing namespace-level mechanism;
        // fileHeader.version is the new file-level mechanism.
        // Both must coexist additively.
        String src = """
                fileHeader:
                  version "1.2.0"
                namespace foo
                version "2.0.0"

                type Bar:
                  x string (1..1)
                """;
        RModel model = parse(src);
        assertEquals(Optional.of("1.2.0"), model.fileHeader().get().version());
        assertEquals(Optional.of("2.0.0"), model.version(),
            "namespace-level versionDecl unaffected by fileHeader.version");
    }

    @Test
    void fileHeaderFreezePreventsFurtherSetters() {
        RFileHeader h = new RFileHeader();
        h.setSourceRange(com.regnosys.rosetta.ast.SourceRange.NONE);
        h.setVersion("1.0.0");
        h.dependsOn().add("foo");
        h.experimental().add("bar");
        h.freeze();
        assertEquals(Optional.of("1.0.0"), h.version());
        assertEquals(1, h.dependsOn().size());
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> h.setVersion("2.0.0")
        );
    }

    private static RModel parse(String src) {
        return TestParseHelper.parseModel(src);
    }
}
