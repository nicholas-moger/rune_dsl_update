package com.regnosys.rosetta.ir.print;

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

/**
 * Golden-snapshot regression test for the declaration IR printer. Parses a tiny
 * fixed set of self-contained {@code .rosetta} fixtures, lowers them through
 * {@link AstToIRAdapter}, prints with {@link IRPrinter}, and compares against a
 * checked-in {@code *.ir.txt} snapshot. Drift fails loudly with a written
 * {@code *.actual.txt}; regenerate deliberately with
 * {@code -Dtest=IRPrinterDeclarationGoldenTest#regen -Dir.print.regen=true}.
 */
class IRPrinterDeclarationGoldenTest {

    /** fixture-relative-path -> golden base name. */
    private static final String[][] CASES = {
        {"data-types/simple-type.rosetta",       "simple-type"},
        {"enums/simple-enum.rosetta",            "simple-enum"},
        {"data-types/simple-choice.rosetta",     "simple-choice"},
        {"data-types/type-with-extends.rosetta", "type-with-extends"},
        {"enums/enum-with-extends.rosetta",      "enum-with-extends"},
    };

    private static final Path MODULE_ROOT = CorpusWalker.moduleRoot();
    private static final Path SNIPPETS    = MODULE_ROOT.resolve("src/test/resources/snippets");
    private static final Path GOLDEN_DIR  = MODULE_ROOT.resolve("src/test/resources/ir-print-golden");

    private static String render(String fixtureRelPath) {
        RModel model = AstBuilder.buildFromFile(SNIPPETS.resolve(fixtureRelPath));
        return new IRPrinter().printAll(new AstToIRAdapter().adaptModel(model));
    }

    static Stream<Arguments> cases() {
        return Arrays.stream(CASES)
                .map(c -> Arguments.of(c[0], c[1]));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("cases")
    void matchesGolden(String fixtureRelPath, String goldenName) throws Exception {
        Path golden = GOLDEN_DIR.resolve(goldenName + ".ir.txt");
        if (!Files.exists(golden)) {
            fail("Golden missing: " + golden + " — regenerate with "
                    + "-Dtest=IRPrinterDeclarationGoldenTest#regen -Dir.print.regen=true");
        }
        String actual   = render(fixtureRelPath);
        String expected = Files.readString(golden);
        if (!actual.equals(expected)) {
            Path actualPath = golden.resolveSibling(goldenName + ".ir.actual.txt");
            Files.writeString(actualPath, actual);
            fail("IR-print golden drift for " + goldenName
                    + "\n  expected: " + golden
                    + "\n  actual:   " + actualPath
                    + "\nIf deliberate, regenerate: -Dtest=IRPrinterDeclarationGoldenTest#regen -Dir.print.regen=true");
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "ir.print.regen", matches = "true")
    void regen() throws Exception {
        Files.createDirectories(GOLDEN_DIR);
        for (String[] c : CASES) {
            Path golden = GOLDEN_DIR.resolve(c[1] + ".ir.txt");
            Files.writeString(golden, render(c[0]));
            System.out.println("Regenerated " + golden);
        }
    }
}
