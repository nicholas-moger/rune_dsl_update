package com.regnosys.rosetta.generator.java.enums;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumGeneratorTest {

    // === Golden file comparison ================================================

    @Test void action_enum_matches_golden() {
        // Build AST matching CDM ActionEnum
        var model = makeModel("cdm.event.common");
        model.setVersion("0.0.0.master-SNAPSHOT");

        var enumeration = new REnumeration();
        enumeration.setName("ActionEnum");
        enumeration.setDefinition("The enumeration values to specify the actions associated with transactions.");

        addValue(enumeration, "New",
                "A new instance of a transaction event, which is also characterized by the fact that the eventIdentifier has an associated version 1.");
        addValue(enumeration, "Correct",
                "A correction of a prior instance of the transaction event. The eventIdentifier has an associated version greater than 1.");
        addValue(enumeration, "Cancel",
                "A cancellation of a prior instance of the transaction event. The eventIdentifier has an associated version greater than 1.");

        model.rootElements().add(enumeration);
        enumeration.setParent(model);

        String generated = generate(model, enumeration);

        // Verify structure
        assertTrue(generated.startsWith("package cdm.event.common;\n"), "Wrong package");
        assertTrue(generated.contains("@RosettaEnum(\"ActionEnum\")"), "Missing @RosettaEnum");
        assertTrue(generated.contains("public enum ActionEnum {"), "Missing enum declaration");
        assertTrue(generated.contains("@RosettaEnumValue(value = \"New\") \n"), "Missing @RosettaEnumValue for New");
        assertTrue(generated.contains("NEW(\"New\", null)"), "Missing NEW constant");
        assertTrue(generated.contains("CORRECT(\"Correct\", null)"), "Missing CORRECT constant");
        assertTrue(generated.contains("CANCEL(\"Cancel\", null)"), "Missing CANCEL constant");
        assertTrue(generated.contains("@version 0.0.0.master-SNAPSHOT"), "Missing version");
        assertTrue(generated.contains("fromDisplayName"), "Missing fromDisplayName method");
        assertTrue(generated.contains("toDisplayString"), "Missing toDisplayString method");

        // Verify comma/semicolon placement
        assertTrue(generated.contains("null),\n\t\n\t"), "Wrong separator between values");
        assertTrue(generated.contains("null)\n;"), "Wrong terminator after last value");
    }

    @Test void simple_enum_no_definition() {
        var model = makeModel("com.example");
        var enumeration = new REnumeration();
        enumeration.setName("Color");
        addValue(enumeration, "Red", null);
        addValue(enumeration, "Blue", null);
        model.rootElements().add(enumeration);
        enumeration.setParent(model);

        String generated = generate(model, enumeration);

        assertTrue(generated.contains("@RosettaEnum(\"Color\")"));
        assertTrue(generated.contains("RED(\"Red\", null)"));
        assertTrue(generated.contains("BLUE(\"Blue\", null)"));
        // No javadoc (no definition)
        assertFalse(generated.contains("/**\n * \n"));
    }

    @Test void enum_with_display_name() {
        var model = makeModel("com.example");
        var enumeration = new REnumeration();
        enumeration.setName("Status");
        var val = new REnumValue();
        val.setName("ActiveTrade");
        val.setDisplayName("Active Trade");
        enumeration.values().add(val);
        model.rootElements().add(enumeration);
        enumeration.setParent(model);

        String generated = generate(model, enumeration);

        assertTrue(generated.contains(
                "@RosettaEnumValue(value = \"ActiveTrade\", displayName = \"Active Trade\")"));
        assertTrue(generated.contains(
                "ACTIVE_TRADE(\"ActiveTrade\", \"Active Trade\")"));
    }

    @Test void enum_value_name_formatting() {
        var model = makeModel("com.example");
        var enumeration = new REnumeration();
        enumeration.setName("MyEnum");
        addValue(enumeration, "CamelCase", null);
        addValue(enumeration, "ALREADY_CAPS", null);
        addValue(enumeration, "3MonthRate", null);
        model.rootElements().add(enumeration);
        enumeration.setParent(model);

        String generated = generate(model, enumeration);

        assertTrue(generated.contains("CAMEL_CASE("), "CamelCase should become CAMEL_CASE");
        assertTrue(generated.contains("ALREADY_CAPS("), "ALREADY_CAPS unchanged");
        assertTrue(generated.contains("_3_MONTH_RATE("), "3MonthRate should become _3_MONTH_RATE");
    }

    @Test void imports_are_correct() {
        var model = makeModel("com.example");
        var enumeration = new REnumeration();
        enumeration.setName("Foo");
        addValue(enumeration, "A", null);
        model.rootElements().add(enumeration);
        enumeration.setParent(model);

        String generated = generate(model, enumeration);

        assertTrue(generated.contains("import com.rosetta.model.lib.annotations.RosettaEnum;"));
        assertTrue(generated.contains("import com.rosetta.model.lib.annotations.RosettaEnumValue;"));
        assertTrue(generated.contains("import java.util.Collections;"));
        assertTrue(generated.contains("import java.util.Map;"));
        assertTrue(generated.contains("import java.util.concurrent.ConcurrentHashMap;"));
    }

    @Test void static_block_formatting() {
        var model = makeModel("com.example");
        var enumeration = new REnumeration();
        enumeration.setName("Foo");
        addValue(enumeration, "A", null);
        model.rootElements().add(enumeration);
        enumeration.setParent(model);

        String generated = generate(model, enumeration);

        // Verify the mixed tab/space indentation in static block matches upstream
        assertTrue(generated.contains("\tprivate static Map<String, Foo> values;"));
        assertTrue(generated.contains("\tstatic {\n"));
        assertTrue(generated.contains("        Map<String, Foo> map = new ConcurrentHashMap<>();"));
        assertTrue(generated.contains("    }\n")); // closing brace with spaces
    }

    @Test void double_space_in_toDisplayString() {
        var model = makeModel("com.example");
        var enumeration = new REnumeration();
        enumeration.setName("Foo");
        addValue(enumeration, "A", null);
        model.rootElements().add(enumeration);
        enumeration.setParent(model);

        String generated = generate(model, enumeration);

        // D11: upstream has double space before displayName in ternary
        assertTrue(generated.contains("return displayName != null ?  displayName : rosettaName;"));
    }

    // === v3.2 seat 9 (D46, F1 - enum-unicode-display): the TWO display-name laws ==============
    // The released 9.83.0 plugin renders the display name RAW inside the @RosettaEnumValue annotation and
    // Java-ESCAPED as the constructor argument (the oracle groups enum-unicode-display / -edge, 16 goldens);
    // the fork used to escape once and read that one string at both seats (the chaos a5uni family, 22 rows).

    @Test void display_name_non_ascii_raw_in_annotation_escaped_in_ctor() {
        // the chaos a5uni shape VERBATIM: `V displayName "µ–„display“"`
        var model = makeModel("chaos.s01.a5uni");
        var enumeration = new REnumeration();
        enumeration.setName("A5UniProbeEnum");
        var val = new REnumValue();
        val.setName("V");
        val.setDisplayName("\u00B5\u2013\u201Edisplay\u201C");
        enumeration.values().add(val);
        model.rootElements().add(enumeration);
        enumeration.setParent(model);

        String generated = generate(model, enumeration);

        assertTrue(generated.contains(
                "\t@RosettaEnumValue(value = \"V\", displayName = \"\u00B5\u2013\u201Edisplay\u201C\") \n"),
                "the annotation seat must carry the RAW display name (the golden's byte): " + generated);
        assertTrue(generated.contains("\tV(\"V\", \"\\u00B5\\u2013\\u201Edisplay\\u201C\")"),
                "the constructor seat must carry the Java-escaped literal: " + generated);
    }

    @Test void display_name_with_quote_backslash_tab_raw_in_annotation() {
        // the edge group's EscapeEnum: upstream's own NON-COMPILING emission - a quote, a backslash and a tab go
        // into the annotation unescaped (the byte contract reproduces it; HoldOutCompileGateTest pins the file as
        // an upstream bug), while the constructor argument is a legal Java literal.
        var model = makeModel("test.enumuniedge");
        var enumeration = new REnumeration();
        enumeration.setName("EscapeEnum");
        var quote = new REnumValue();
        quote.setName("Quote");
        quote.setDisplayName("say \"hi\"");
        enumeration.values().add(quote);
        var backslash = new REnumValue();
        backslash.setName("Backslash");
        backslash.setDisplayName("back\\slash");
        enumeration.values().add(backslash);
        var tab = new REnumValue();
        tab.setName("Tab");
        tab.setDisplayName("tab\there");
        enumeration.values().add(tab);
        model.rootElements().add(enumeration);
        enumeration.setParent(model);

        String generated = generate(model, enumeration);

        assertTrue(generated.contains("@RosettaEnumValue(value = \"Quote\", displayName = \"say \"hi\"\") \n"), generated);
        assertTrue(generated.contains("\tQUOTE(\"Quote\", \"say \\\"hi\\\"\")"), generated);
        assertTrue(generated.contains("@RosettaEnumValue(value = \"Backslash\", displayName = \"back\\slash\") \n"), generated);
        assertTrue(generated.contains("\tBACKSLASH(\"Backslash\", \"back\\\\slash\")"), generated);
        assertTrue(generated.contains("@RosettaEnumValue(value = \"Tab\", displayName = \"tab\there\") \n"), generated);
        assertTrue(generated.contains("\tTAB(\"Tab\", \"tab\\there\")"), generated);
    }

    @Test void synonym_value_non_ascii_raw_in_annotation() {
        // the edge group's SynonymEnum: @RosettaSynonym's value (and source) are the RAW model text
        var model = makeModel("test.enumuniedge");
        var enumeration = new REnumeration();
        enumeration.setName("SynonymEnum");
        var val = new REnumValue();
        val.setName("S");
        val.setDisplayName("\u00B5");
        var syn = new com.regnosys.rosetta.ast.synonyms.REnumSynonym();
        syn.setValue("\u00B5\u2013syn");
        syn.sources().add("EdgeSrc");
        val.synonyms().add(syn);
        enumeration.values().add(val);
        model.rootElements().add(enumeration);
        enumeration.setParent(model);

        String generated = generate(model, enumeration);

        assertTrue(generated.contains("\t@RosettaSynonym(value = \"\u00B5\u2013syn\", source = \"EdgeSrc\")\n"), generated);
        assertTrue(generated.contains("\t@RosettaEnumValue(value = \"S\", displayName = \"\u00B5\") \n"), generated);
        assertTrue(generated.contains("\tS(\"S\", \"\\u00B5\")"), generated);
        assertTrue(generated.contains("import com.rosetta.model.lib.annotations.RosettaSynonym;"), generated);
    }

    // === Helpers ==============================================================

    private String generate(RModel model, REnumeration enumeration) {
        var result = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(result.workspace());
        var generator = new EnumGenerator(gm);
        var javaEnum = generator.createTypeRepresentation(enumeration);
        return generator.generate(enumeration, javaEnum, gm.version(model));
    }

    private RModel makeModel(String namespace) {
        var model = new RModel();
        model.setNamespace(namespace);
        return model;
    }

    private void addValue(REnumeration enumeration, String name, String definition) {
        var val = new REnumValue();
        val.setName(name);
        if (definition != null) {
            val.setDefinition(definition);
        }
        enumeration.values().add(val);
    }
}
