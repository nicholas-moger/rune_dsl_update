package com.regnosys.rosetta.ir.emit.python;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/** Golden-snapshot regression for the Python declaration emitter (regen: -Dir.python.decl.regen=true). */
class IRPythonDeclarationGoldenTest {

    private static final String[][] CASES = {
        {"data-types/simple-type.rosetta", "simple-type"},
        {"enums/simple-enum.rosetta", "simple-enum"},
        {"data-types/type-with-extends.rosetta", "type-with-extends"},
        {"data-types/simple-choice.rosetta", "simple-choice"},
        {"data-types/keyword-names.rosetta", "keyword-names"},
        {"data-types/camel-case-names.rosetta", "camel-case-names"},
    };
    private static final Path MODULE_ROOT = CorpusWalker.moduleRoot();
    private static final Path SNIPPETS = MODULE_ROOT.resolve("src/test/resources/snippets");
    private static final Path GOLDEN_DIR = MODULE_ROOT.resolve("src/test/resources/ir-python-decl-golden");

    private static String render(String fixtureRelPath) {
        RModel model = AstBuilder.buildFromFile(SNIPPETS.resolve(fixtureRelPath));
        IRPythonDeclarationEmitter emitter = new IRPythonDeclarationEmitter();
        String body = new AstToIRAdapter().adaptModel(model).stream()
                .map(emitter::emit)
                .collect(Collectors.joining("\n\n"));
        return IRPythonDeclarationEmitter.DECL_PREAMBLE + "\n\n" + body + "\n";
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("cases")
    void matchesGolden(String fixtureRelPath, String goldenName) throws Exception {
        Path golden = GOLDEN_DIR.resolve(goldenName + ".py");
        if (!Files.exists(golden)) {
            fail("Golden missing: " + golden + " — regenerate with "
                + "-Dtest=IRPythonDeclarationGoldenTest#regen -Dir.python.decl.regen=true");
        }
        String actual = render(fixtureRelPath);
        String expected = Files.readString(golden);
        if (!actual.equals(expected)) {
            Path actualPath = golden.resolveSibling(goldenName + ".py.actual");
            Files.writeString(actualPath, actual);
            fail("Python-decl golden drift for " + goldenName + "\n  expected: " + golden
                + "\n  actual:   " + actualPath
                + "\nIf deliberate: -Dtest=IRPythonDeclarationGoldenTest#regen -Dir.python.decl.regen=true");
        }
    }

    static Stream<Arguments> cases() {
        return Arrays.stream(CASES).map(c -> Arguments.of(c[0], c[1]));
    }

    @Test
    @EnabledIfSystemProperty(named = "ir.python.decl.regen", matches = "true")
    void regen() throws Exception {
        Files.createDirectories(GOLDEN_DIR);
        for (String[] c : CASES) {
            Files.writeString(GOLDEN_DIR.resolve(c[1] + ".py"), render(c[0]));
            System.out.println("Regenerated " + c[1] + ".py");
        }
    }
}
