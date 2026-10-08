package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.fail;

/** Golden-snapshot regression test for the v1 IR JSON Schema (regen: -Dir.json.schema.regen=true). */
class IRJsonSchemaGoldenTest {

    private static final Path GOLDEN = CorpusWalker.moduleRoot()
            .resolve("src/test/resources/ir-json-schema/v1.schema.json");

    @Test
    void matchesGolden() throws Exception {
        if (!Files.exists(GOLDEN)) {
            fail("Golden missing: " + GOLDEN + " — regenerate with "
                    + "-Dtest=IRJsonSchemaGoldenTest#regen -Dir.json.schema.regen=true");
        }
        String actual = IRJsonSchema.schemaJson();
        String expected = Files.readString(GOLDEN);
        if (!actual.equals(expected)) {
            Path actualPath = GOLDEN.resolveSibling("v1.schema.json.actual");
            Files.writeString(actualPath, actual);
            fail("IR-JSON-schema golden drift\n  expected: " + GOLDEN + "\n  actual:   " + actualPath
                    + "\nIf deliberate: -Dtest=IRJsonSchemaGoldenTest#regen -Dir.json.schema.regen=true");
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "ir.json.schema.regen", matches = "true")
    void regen() throws Exception {
        Files.createDirectories(GOLDEN.getParent());
        Files.writeString(GOLDEN, IRJsonSchema.schemaJson());
        System.out.println("Regenerated " + GOLDEN);
    }
}
