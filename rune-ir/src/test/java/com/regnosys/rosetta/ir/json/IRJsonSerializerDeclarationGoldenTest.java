package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/** Golden-snapshot regression test for the declaration IR JSON serializer (regen: -Dir.json.regen=true). */
class IRJsonSerializerDeclarationGoldenTest {

    private static final String[][] CASES = {
        {"data-types/simple-type.rosetta", "simple-type"},
        {"enums/simple-enum.rosetta", "simple-enum"},
        {"data-types/simple-choice.rosetta", "simple-choice"},
        {"data-types/type-with-extends.rosetta", "type-with-extends"},
        {"enums/enum-with-extends.rosetta", "enum-with-extends"},
    };
    private static final Path MODULE_ROOT = CorpusWalker.moduleRoot();
    private static final Path SNIPPETS = MODULE_ROOT.resolve("src/test/resources/snippets");
    private static final Path GOLDEN_DIR = MODULE_ROOT.resolve("src/test/resources/ir-json-golden");

    private static String render(String fixtureRelPath) {
        RModel model = AstBuilder.buildFromFile(SNIPPETS.resolve(fixtureRelPath));
        return new IRJsonSerializer().toJson(new AstToIRAdapter().adaptModel(model));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("cases")
    void matchesGolden(String fixtureRelPath, String goldenName) throws Exception {
        Path golden = GOLDEN_DIR.resolve(goldenName + ".ir.json");
        if (!Files.exists(golden)) {
            fail("Golden missing: " + golden + " — regenerate with "
                + "-Dtest=IRJsonSerializerDeclarationGoldenTest#regen -Dir.json.regen=true");
        }
        String actual = render(fixtureRelPath);
        String expected = Files.readString(golden);
        if (!actual.equals(expected)) {
            Path actualPath = golden.resolveSibling(goldenName + ".ir.json.actual");
            Files.writeString(actualPath, actual);
            fail("IR-JSON golden drift for " + goldenName + "\n  expected: " + golden
                + "\n  actual:   " + actualPath
                + "\nIf deliberate: -Dtest=IRJsonSerializerDeclarationGoldenTest#regen -Dir.json.regen=true");
        }
    }

    static Stream<Arguments> cases() {
        return Arrays.stream(CASES).map(c -> Arguments.of(c[0], c[1]));
    }

    @Test
    @EnabledIfSystemProperty(named = "ir.json.regen", matches = "true")
    void regen() throws Exception {
        Files.createDirectories(GOLDEN_DIR);
        for (String[] c : CASES) {
            Files.writeString(GOLDEN_DIR.resolve(c[1] + ".ir.json"), render(c[0]));
            System.out.println("Regenerated " + c[1] + ".ir.json");
        }
    }
}
