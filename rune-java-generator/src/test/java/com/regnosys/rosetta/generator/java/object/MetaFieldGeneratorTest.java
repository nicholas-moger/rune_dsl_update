package com.regnosys.rosetta.generator.java.object;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetaFieldGeneratorTest {

    private static final Path CDM_ROSETTA_DIR = Path.of("../common-domain-model/rosetta-source/src/main/rosetta");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");
    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    static boolean cdmAvailable() {
        return Files.isDirectory(CDM_ROSETTA_DIR);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void collectSpecs_produces_88_unique_metafield_specs() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var generator = new MetaFieldGenerator(gm, TYPE_TRANSLATOR);

        var specs = generator.collectSpecs();

        long fieldWithMeta = specs.stream()
                .filter(s -> s.kind() == MetaFieldGenerator.MetaKind.FIELD_WITH_META)
                .count();
        long refWithMeta = specs.stream()
                .filter(s -> s.kind() == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META)
                .count();

        System.out.println("MetaFieldGenerator collection: "
                + fieldWithMeta + " FieldWithMeta, " + refWithMeta + " ReferenceWithMeta, "
                + specs.size() + " total");

        // CDM golden has 48 FWM + 42 RWM = 90 total.
        // We collect 47+41=88 from attribute scanning. 2 wrappers come from
        // WithMetaOperation expressions (M7b scope): FieldWithMetaCommodityPayout
        // and ReferenceWithMetaVoid.
        assertEquals(47, fieldWithMeta, "FieldWithMeta count mismatch");
        assertEquals(41, refWithMeta, "ReferenceWithMeta count mismatch");
        assertEquals(88, specs.size(), "Total metafield spec count mismatch");
    }

    @Test
    @EnabledIf("cdmAvailable")
    void collectSpecs_value_categories_match_golden() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var generator = new MetaFieldGenerator(gm, TYPE_TRANSLATOR);

        var specs = generator.collectSpecs();

        long fwmEnum = specs.stream()
                .filter(s -> s.kind() == MetaFieldGenerator.MetaKind.FIELD_WITH_META
                        && s.valueCategory() == MetaFieldGenerator.ValueCategory.ENUM)
                .count();
        long fwmComposite = specs.stream()
                .filter(s -> s.kind() == MetaFieldGenerator.MetaKind.FIELD_WITH_META
                        && s.valueCategory() == MetaFieldGenerator.ValueCategory.COMPOSITE)
                .count();
        long fwmPrimitive = specs.stream()
                .filter(s -> s.kind() == MetaFieldGenerator.MetaKind.FIELD_WITH_META
                        && s.valueCategory() == MetaFieldGenerator.ValueCategory.PRIMITIVE)
                .count();

        System.out.println("FieldWithMeta categories: "
                + fwmEnum + " enum, " + fwmComposite + " composite, " + fwmPrimitive + " primitive");

        // CDM golden: 35 enum, 10 composite, 2 primitive FieldWithMeta
        // (Missing 1 composite: CommodityPayout from WithMetaOperation — M7b scope)
        assertEquals(35, fwmEnum, "FWM enum count");
        assertEquals(10, fwmComposite, "FWM composite count");
        assertEquals(2, fwmPrimitive, "FWM primitive count");
    }

    private com.regnosys.rosetta.symbols.RLinkingResult loadFullCorpus() throws IOException {
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) {
                              System.err.println("Builtin parse error: " + p.getFileName() + " — " + e.getMessage());
                          }
                      });
            }
        }
        try (var stream = Files.list(CDM_ROSETTA_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try { models.add(AstBuilder.buildFromFile(p)); }
                      catch (Exception e) {
                          System.err.println("Parse error: " + p.getFileName() + " — " + e.getMessage());
                      }
                  });
        }
        return RWorkspace.build(models);
    }
    @Test
    @EnabledIf("cdmAvailable")
    void dump_first_metafield_diff() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var generator = new MetaFieldGenerator(gm, TYPE_TRANSLATOR);

        java.util.Map<String, String> output = new java.util.LinkedHashMap<>();
        generator.generate(output);

        // Compare FieldWithMetaString (simplest, primitive builtin)
        String targetFile = "com/rosetta/model/metafields/FieldWithMetaString.java";
        String generated = output.get(targetFile);
        assertNotNull(generated, "Generated output not found for " + targetFile);

        java.nio.file.Path goldenPath = java.nio.file.Path.of("../common-domain-model/rosetta-source/src/generated/java/" + targetFile);
        String golden = java.nio.file.Files.readString(goldenPath).replace("\r\n", "\n").replace("\r", "\n");
        generated = generated.replace("\r\n", "\n").replace("\r", "\n");

        String[] gl = golden.split("\n", -1);
        String[] ge = generated.split("\n", -1);
        int maxLines = Math.max(gl.length, ge.length);
        System.out.println("=== DIFF for " + targetFile + " ===");
        System.out.println("Golden lines: " + gl.length + ", Generated lines: " + ge.length);
        int diffCount = 0;
        for (int i = 0; i < maxLines && diffCount < 15; i++) {
            String g = i < gl.length ? gl[i] : "<EOF>";
            String e = i < ge.length ? ge[i] : "<EOF>";
            if (!g.equals(e)) {
                System.out.println("Line " + (i + 1) + ":");
                System.out.println("  GOLDEN:    [" + g + "]");
                System.out.println("  GENERATED: [" + e + "]");
                diffCount++;
            }
        }
    }
}
