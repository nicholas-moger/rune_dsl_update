package com.regnosys.rosetta.generator.java.template;

import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.template.model.ConditionRefModel;
import com.regnosys.rosetta.generator.java.template.model.MetaTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MetaTemplateModel} and {@link ConditionRefModel}.
 */
class MetaTemplateModelTest {

    @Test void all_fields_populated() {
        var condRefs = List.of(
                new ConditionRefModel("TradeValid", "Trade", "com.example.validation.datarule.TradeValid", true));
        var model = new MetaTemplateModel(
                "com.example.meta", "TradeMeta", "Trade", "Trade", "1.0.0",
                List.of("com.example.Trade", "java.util.List"),
                "TradeValidator", "TradeTypeFormatValidator", "TradeOnlyExistsValidator",
                condRefs, List.of());

        assertEquals("com.example.meta", model.getPackageName());
        assertEquals("TradeMeta", model.getMetaClassName());
        assertEquals("Trade", model.getDataClassName());
        assertEquals("1.0.0", model.getVersion());
        assertEquals(2, model.getImports().size());
        assertEquals("TradeValidator", model.getValidatorSimple());
        assertEquals("TradeTypeFormatValidator", model.getTypeFormatValidatorSimple());
        assertEquals("TradeOnlyExistsValidator", model.getOnlyExistsValidatorSimple());
        assertEquals(1, model.getConditionRefs().size());
    }

    @Test void imports_are_immutable() {
        var mutableImports = new ArrayList<>(List.of("a.B", "c.D"));
        var model = new MetaTemplateModel(
                "pkg", "Meta", "Data", "Data", null, mutableImports,
                "V", "TFV", "OEV", List.of(), List.of());
        mutableImports.add("e.F");
        assertEquals(2, model.getImports().size(), "Import list should be immutable copy");
        assertThrows(UnsupportedOperationException.class, () -> model.getImports().add("x.Y"));
    }

    @Test void condition_refs_are_immutable() {
        var mutableRefs = new ArrayList<>(List.of(
                new ConditionRefModel("Cond", "Type", "fqn", true)));
        var model = new MetaTemplateModel(
                "pkg", "Meta", "Data", "Data", null, List.of(),
                "V", "TFV", "OEV", mutableRefs, List.of());
        mutableRefs.add(new ConditionRefModel("X", "Y", "z", true));
        assertEquals(1, model.getConditionRefs().size(), "Condition refs should be immutable copy");
        assertThrows(UnsupportedOperationException.class, () -> model.getConditionRefs().add(null));
    }

    @Test void null_version_accepted() {
        var model = new MetaTemplateModel(
                "pkg", "Meta", "Data", "Data", null, List.of(),
                "V", "TFV", "OEV", List.of(), List.of());
        assertNull(model.getVersion());
    }

    @Test void empty_condition_refs() {
        var model = new MetaTemplateModel(
                "pkg", "Meta", "Data", "Data", "1.0", List.of(),
                "V", "TFV", "OEV", List.of(), List.of());
        assertTrue(model.getConditionRefs().isEmpty());
    }

    @Test void condition_ref_model_fields() {
        var ref = new ConditionRefModel("TradeValid", "Trade",
                "com.example.validation.datarule.TradeValid", false);
        assertEquals("TradeValid", ref.getSimpleName());
        assertEquals("Trade", ref.getInstanceType());
        assertEquals("com.example.validation.datarule.TradeValid", ref.getFqn());
        assertFalse(ref.getIsLast());
    }

    @Test void condition_ref_isLast_flag() {
        var first = new ConditionRefModel("A", "T", "fqn.A", false);
        var last = new ConditionRefModel("B", "T", "fqn.B", true);
        assertFalse(first.getIsLast());
        assertTrue(last.getIsLast());
    }

    @Test void template_renders_with_conditions() {
        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath("templates/java-meta.stg");

        var condRefs = List.of(
                new ConditionRefModel("TradeFpmlIrd29", "Trade",
                        "com.example.validation.datarule.TradeFpmlIrd29", false),
                new ConditionRefModel("TradeValid", "Trade",
                        "com.example.validation.datarule.TradeValid", true));
        var model = new MetaTemplateModel(
                "com.example.meta", "TradeMeta", "Trade", "Trade", "1.0.0",
                List.of("com.example.Trade",
                        "com.example.validation.TradeTypeFormatValidator",
                        "com.example.validation.TradeValidator",
                        "com.example.validation.datarule.TradeFpmlIrd29",
                        "com.example.validation.datarule.TradeValid",
                        "com.example.validation.exists.TradeOnlyExistsValidator",
                        "com.rosetta.model.lib.annotations.RosettaMeta",
                        "com.rosetta.model.lib.meta.RosettaMetaData",
                        "com.rosetta.model.lib.qualify.QualifyFunctionFactory",
                        "com.rosetta.model.lib.qualify.QualifyResult",
                        "com.rosetta.model.lib.validation.Validator",
                        "com.rosetta.model.lib.validation.ValidatorFactory",
                        "com.rosetta.model.lib.validation.ValidatorWithArg",
                        "java.util.Arrays",
                        "java.util.Collections",
                        "java.util.List",
                        "java.util.Set",
                        "java.util.function.Function"),
                "TradeValidator", "TradeTypeFormatValidator", "TradeOnlyExistsValidator",
                condRefs, List.of());

        // v3.2 seat 11 (D50): the two steps generate() renders - the class text with its sentinels through the
        // generator's token map, resolved from the meta class as the one seed, then the file wrapper.
        String classText = renderer.render("templates/java-meta.stg", "metaBody", "m", model,
                "t", ModelMetaGenerator.META_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getMetaClassName(), model.getImports());
        String rendered = renderer.render("templates/java-meta.stg", "metaFile", "m",
                new PojoTemplateModel(model.getPackageName(), resolved.imports(), List.of(), resolved.classText()));

        assertTrue(rendered.contains("@RosettaMeta(model=Trade.class)"));
        assertTrue(rendered.contains("public class TradeMeta implements RosettaMetaData<Trade>"));
        assertTrue(rendered.contains("factory.<Trade>create(TradeFpmlIrd29.class),"));
        assertTrue(rendered.contains("factory.<Trade>create(TradeValid.class)"));
        // Last condition should NOT have trailing comma
        assertFalse(rendered.contains("TradeValid.class),"));
        assertTrue(rendered.contains("@version 1.0.0"));
        assertTrue(rendered.contains("return new TradeValidator();"));
        assertTrue(rendered.contains("return new TradeTypeFormatValidator();"));
        assertTrue(rendered.contains("return new TradeOnlyExistsValidator();"));
    }

    @Test void template_renders_without_conditions() {
        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath("templates/java-meta.stg");

        var model = new MetaTemplateModel(
                "com.example.meta", "SimpleMeta", "Simple", "Simple", "2.0",
                List.of("com.example.Simple",
                        "com.rosetta.model.lib.annotations.RosettaMeta",
                        "com.rosetta.model.lib.meta.RosettaMetaData",
                        "java.util.Arrays",
                        "java.util.Collections",
                        "java.util.List",
                        "java.util.Set",
                        "java.util.function.Function"),
                "SimpleValidator", "SimpleTypeFormatValidator", "SimpleOnlyExistsValidator",
                List.of(), List.of());

        // v3.2 seat 11 (D50): the two steps generate() renders - the class text with its sentinels through the
        // generator's token map, resolved from the meta class as the one seed, then the file wrapper.
        String classText = renderer.render("templates/java-meta.stg", "metaBody", "m", model,
                "t", ModelMetaGenerator.META_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getMetaClassName(), model.getImports());
        String rendered = renderer.render("templates/java-meta.stg", "metaFile", "m",
                new PojoTemplateModel(model.getPackageName(), resolved.imports(), List.of(), resolved.classText()));

        assertTrue(rendered.contains("return Arrays.asList("));
        assertTrue(rendered.contains("return Collections.emptyList();"));
        // No condition references in dataRules (empty list)
        assertFalse(rendered.contains("datarule"));
    }
}
