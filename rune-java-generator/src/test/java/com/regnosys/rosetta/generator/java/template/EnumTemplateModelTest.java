package com.regnosys.rosetta.generator.java.template;

import com.regnosys.rosetta.generator.java.template.model.EnumTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.EnumValueModel;
import com.regnosys.rosetta.generator.java.template.model.SynonymModel;
import com.regnosys.rosetta.generator.java.util.ModelGeneratorUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnumTemplateModelTest {

    @Test void basic_model_fields() {
        var model = new EnumTemplateModel(
                "com.example", "Color", "1.0", null,
                List.of("com.rosetta.model.lib.annotations.RosettaEnum"),
                List.of(
                    new EnumValueModel("RED", "Red", null, null, null, null, List.of(), false),
                    new EnumValueModel("BLUE", "Blue", null, null, null, null, List.of(), true)
                ),
                false);

        assertEquals("com.example", model.getPackageName());
        assertEquals("Color", model.getEnumName());
        assertEquals("1.0", model.getVersion());
        assertNull(model.getJavadoc());
        assertFalse(model.getHasSynonyms());
        assertEquals(2, model.getValues().size());
    }

    @Test void value_with_display_name() {
        // v3.2 seat 9 (D46): the RAW display name (the annotation seat) beside its Java-escaped literal (the ctor seat)
        var val = new EnumValueModel("ACTIVE_TRADE", "ActiveTrade", "Active µTrade", "Active \\u00B5Trade",
                null, null, List.of(), true);
        assertEquals("Active µTrade", val.getDisplayName());
        assertEquals("Active \\u00B5Trade", val.getDisplayNameLiteral());
        assertEquals("ActiveTrade", val.getRosettaName());
        assertEquals("ACTIVE_TRADE", val.getJavaName());
    }

    @Test void value_with_definition() {
        var val = new EnumValueModel("NEW", "New", null, null,
                "A new instance.", null, List.of(), false);
        assertEquals("A new instance.", val.getDefinition());
        assertFalse(val.getIsLast());
    }

    @Test void value_with_synonyms() {
        var syn = new SynonymModel("newTrade", List.of("FpML_5_10", "FIX_4_4"));
        var val = new EnumValueModel("NEW", "New", null, null, null, null, List.of(syn), true);
        assertEquals(1, val.getSynonyms().size());
        assertEquals("newTrade", val.getSynonyms().get(0).getValue());
        assertEquals(2, val.getSynonyms().get(0).getSources().size());
    }

    @Test void isLast_flag() {
        var first = new EnumValueModel("A", "A", null, null, null, null, List.of(), false);
        var last = new EnumValueModel("B", "B", null, null, null, null, List.of(), true);
        assertFalse(first.getIsLast());
        assertTrue(last.getIsLast());
    }

    @Test void imports_are_immutable() {
        var original = new java.util.ArrayList<>(List.of("java.util.List"));
        var model = new EnumTemplateModel("com.example", "Foo", null, null,
                original, List.of(), false);
        original.add("java.util.Map");
        assertEquals(1, model.getImports().size());
    }

    @Test void synonyms_are_immutable() {
        var original = new java.util.ArrayList<>(List.of(
                new SynonymModel("x", List.of("src"))));
        var val = new EnumValueModel("A", "A", null, null, null, null, original, true);
        original.add(new SynonymModel("y", List.of("src2")));
        assertEquals(1, val.getSynonyms().size());
    }
}
