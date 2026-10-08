package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Test;
import org.stringtemplate.v4.AutoIndentWriter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupString;
import org.stringtemplate.v4.STWriter;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test ST4 escape behavior.
 */
class ST4EscapeTest {

    @Test
    void test_escape_in_singleline_template() {
        String groupDef =
            "delimiters \"<\", \">\"\n" +
            "typeExpr(t) ::= <<\n" +
            "List\\<<t>\\>\n" +
            ">>\n";
        STGroup group = new STGroupString(groupDef);
        ST st = group.getInstanceOf("typeExpr");
        st.add("t", "Date");
        String result = st.render();
        System.out.println("<<>> template: [" + result + "]");
        assertEquals("List<Date>", result);
    }

    @Test
    void test_escape_in_percent_template() {
        // Known ST4 4.3.4 bug: \> does NOT produce literal > in <%...%> templates.
        // It produces literal \> instead. Workaround: use <<...>> templates or
        // pre-compute the string in Java. See evaluateReturnType in FunctionTemplateModel.
        String groupDef =
            "delimiters \"<\", \">\"\n" +
            "typeExpr(t) ::= <%\n" +
            "List\\<<t>\\>\n" +
            "%>\n";
        STGroup group = new STGroupString(groupDef);
        ST st = group.getInstanceOf("typeExpr");
        st.add("t", "Date");
        String result = st.render();
        // Verify the bug still exists (so we know the workaround is still needed)
        assertEquals("List<Date\\>", result, "ST4 <%...%> escape bug should produce backslash");
    }

    @Test
    void test_newline_after_iteration() {
        String groupDef =
            "delimiters \"<\", \">\"\n" +
            "myTemplate(items) ::= <<\n" +
            "before\n" +
            "<items:{item | line <item>\n" +
            "}>" +
            "after\n" +
            ">>\n";
        STGroup group = new STGroupString(groupDef);
        ST st = group.getInstanceOf("myTemplate");
        st.add("items", new String[]{"A"});
        String result = st.render();
        System.out.println("iteration result: [" + result.replace("\n", "\\n") + "]");
    }

    @Test
    void test_tab_only_line_preservation() {
        String groupDef =
            "delimiters \"<\", \">\"\n" +
            "myTemplate(x) ::= <<\n" +
            "before\n" +
            "<\\t><\\t>\n" +
            "after\n" +
            ">>\n";
        STGroup group = new STGroupString(groupDef);
        ST st = group.getInstanceOf("myTemplate");
        st.add("x", "dummy");
        String result = st.render();
        System.out.println("NoIndent: [" + result.replace("\n", "\\n").replace("\t", "\\t") + "]");

        // Now test with AutoIndentWriter
        try {
            ST st2 = group.getInstanceOf("myTemplate");
            st2.add("x", "dummy");
            StringWriter sw = new StringWriter();
            STWriter stw = new AutoIndentWriter(sw);
            st2.write(stw);
            String result2 = sw.toString();
            System.out.println("AutoIndent: [" + result2.replace("\n", "\\n").replace("\t", "\\t") + "]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void test_escaped_brace_in_subtemplate() {
        // Test: \{ inside {..} sub-template
        String groupDef1 =
            "delimiters \"<\", \">\"\n" +
            "myTemplate(items) ::= <<\n" +
            "<items:{item | if (<item>) \\{\n" +
            "  body\n" +
            "\\}\n" +
            "}>\n" +
            ">>\n";
        STGroup group1 = new STGroupString(groupDef1);
        try {
            ST st = group1.getInstanceOf("myTemplate");
            st.add("items", new String[]{"x"});
            StringWriter sw = new StringWriter();
            st.write(new org.stringtemplate.v4.NoIndentWriter(sw));
            System.out.println("Escaped brace: [" + sw.toString().replace("\n", "\\n") + "]");
        } catch (IOException e) { e.printStackTrace(); }

        // Test: balanced braces without escaping
        String groupDef3 =
            "delimiters \"<\", \">\"\n" +
            "myTemplate3(items) ::= <<\n" +
            "<items:{item | if (<item>) {\n" +
            "  body\n" +
            "}\n" +
            "}>\n" +
            ">>\n";
        STGroup group3 = new STGroupString(groupDef3);
        try {
            ST st = group3.getInstanceOf("myTemplate3");
            st.add("items", new String[]{"x"});
            StringWriter sw = new StringWriter();
            st.write(new org.stringtemplate.v4.NoIndentWriter(sw));
            System.out.println("Balanced brace: [" + sw.toString().replace("\n", "\\n") + "]");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Test
    void test_tab_line_after_if_endif() {
        // Reproduce the exact pattern from the function template
        String groupDef =
            "delimiters \"<\", \">\"\n" +
            "myTemplate(hasX, items) ::= <<\n" +
            "before\n" +
            "<if(hasX)>\n" +
            "<items:{item | <\\t><\\t>item <item>\n" +
            "}>\n" +
            "<\\t><\\t>\n" +
            "<endif>\n" +
            "after\n" +
            ">>\n";
        STGroup group = new STGroupString(groupDef);
        try {
            ST st = group.getInstanceOf("myTemplate");
            st.add("hasX", true);
            st.add("items", new String[]{"A"});
            StringWriter sw = new StringWriter();
            STWriter stw = new AutoIndentWriter(sw);
            st.write(stw);
            String result = sw.toString();
            System.out.println("if+iter+tab: [" + result.replace("\n", "\\n").replace("\t", "\\t") + "]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void test_newline_between_iteration_and_next() {
        // Simulates: dependencies followed by <\n><\t>/**
        String groupDef =
            "delimiters \"<\", \">\"\n" +
            "myTemplate(hasDeps, deps) ::= <<\n" +
            "<if(hasDeps)>\n" +
            "<deps:{dep | \\t@Inject <dep>;\n" +
            "}>\n" +
            "<endif>\n" +
            "<\\n>\\t/**\n" +
            ">>\n";
        STGroup group = new STGroupString(groupDef);
        ST st = group.getInstanceOf("myTemplate");
        st.add("hasDeps", true);
        st.add("deps", new String[]{"DayCountBasis dayCountBasis"});
        String result = st.render();
        // Diagnostic harness — pins the current behaviour so changes surface.
        assertTrue(result.contains("@Inject DayCountBasis dayCountBasis;"),
                "@Inject line should be emitted; got: [" + result + "]");
    }

    // =========================================================================
    // Qualify-function implements-clause (Copilot R21 C1 regression guard)
    // =========================================================================
    //
    // Reproduces the exact template fragment from java-function.stg line 30:
    //   public abstract class <m.className> implements
    //       RosettaFunction<if(m.isQualify)>,IQualifyFunctionExtension\<<m.qualifyGenericType>\><endif>
    //
    // Copilot R21 claimed this emits "RosettaFunction>,IQualifyFunctionExtension<...>"
    // (stray ">" after RosettaFunction). Verified empirically here: it actually
    // emits the correct "RosettaFunction,IQualifyFunctionExtension<...>" — the
    // golden form (see e.g. common-domain-model/.../Qualify_Substitution.java).
    // No template change warranted; Copilot suggestion would have INTRODUCED
    // a mismatch by inserting an unwanted space after the comma.

    @Test
    void qualify_function_implements_clause_emits_golden_form() {
        String groupDef =
            "delimiters \"<\", \">\"\n" +
            "tpl(m) ::= <<\n" +
            "public abstract class <m.className> implements RosettaFunction<if(m.isQualify)>,IQualifyFunctionExtension\\<<m.qualifyGenericType>\\><endif> {\n" +
            ">>\n";
        STGroup group = new STGroupString(groupDef);

        java.util.Map<String, Object> qualify = new java.util.HashMap<>();
        qualify.put("className", "Qualify_Substitution");
        qualify.put("isQualify", Boolean.TRUE);
        qualify.put("qualifyGenericType", "BusinessEvent");

        ST st = group.getInstanceOf("tpl");
        st.add("m", qualify);
        String result = st.render();

        // Pins the exact golden form used by CDM's Qualify_* classes:
        // no stray ">", no space after ",", angle brackets round the generic.
        assertEquals(
                "public abstract class Qualify_Substitution implements "
                        + "RosettaFunction,IQualifyFunctionExtension<BusinessEvent> {",
                result);
    }

    @Test
    void non_qualify_function_implements_clause_omits_extension() {
        // Regression guard: when isQualify=false, the if-branch must be
        // suppressed — no ",IQualifyFunctionExtension" fragment, no stray
        // angle brackets.
        String groupDef =
            "delimiters \"<\", \">\"\n" +
            "tpl(m) ::= <<\n" +
            "public abstract class <m.className> implements RosettaFunction<if(m.isQualify)>,IQualifyFunctionExtension\\<<m.qualifyGenericType>\\><endif> {\n" +
            ">>\n";
        STGroup group = new STGroupString(groupDef);

        java.util.Map<String, Object> plain = new java.util.HashMap<>();
        plain.put("className", "Max");
        plain.put("isQualify", Boolean.FALSE);
        plain.put("qualifyGenericType", null);

        ST st = group.getInstanceOf("tpl");
        st.add("m", plain);
        String result = st.render();

        assertEquals(
                "public abstract class Max implements RosettaFunction {",
                result);
    }
}
