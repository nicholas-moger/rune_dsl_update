package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeWriterTest {

    // === Basic output ========================================================

    @Test void empty_body_produces_package_and_blank_line() {
        var w = new CodeWriter("com.example");
        assertEquals("package com.example;\n\n", w.toSource());
    }

    @Test void single_line_body() {
        var w = new CodeWriter("com.example");
        w.appendLine("public class Foo {}");
        assertEquals(
                "package com.example;\n\npublic class Foo {}\n",
                w.toSource());
    }

    // === Indentation =========================================================

    @Test void indent_adds_tabs() {
        var w = new CodeWriter("com.example");
        w.appendLine("class Foo {");
        w.indent();
        w.appendLine("int x;");
        w.dedent();
        w.appendLine("}");
        assertEquals(
                "package com.example;\n\n" +
                "class Foo {\n" +
                "\tint x;\n" +
                "}\n",
                w.toSource());
    }

    @Test void nested_indent() {
        var w = new CodeWriter("com.example");
        w.indent();
        w.indent();
        w.appendLine("deep");
        w.dedent();
        w.dedent();
        assertEquals("package com.example;\n\n\t\tdeep\n", w.toSource());
    }

    @Test void dedent_below_zero_is_noop() {
        var w = new CodeWriter("com.example");
        w.dedent();
        w.dedent();
        assertEquals(0, w.getIndentLevel());
    }

    // === Multi-line append ===================================================

    @Test void append_multiline_text_indents_each_line() {
        var w = new CodeWriter("com.example");
        w.indent();
        w.append("line1\nline2\nline3\n");
        w.dedent();
        assertEquals(
                "package com.example;\n\n" +
                "\tline1\n" +
                "\tline2\n" +
                "\tline3\n",
                w.toSource());
    }

    @Test void append_without_newline_continues_on_same_line() {
        var w = new CodeWriter("com.example");
        w.append("public ");
        w.append("class ");
        w.appendLine("Foo {}");
        assertEquals(
                "package com.example;\n\npublic class Foo {}\n",
                w.toSource());
    }

    // === Block helper ========================================================

    @Test void appendBlock_wraps_with_indent() {
        var w = new CodeWriter("com.example");
        w.appendBlock("if (true) {", "}", () -> {
            w.appendLine("return 1;");
        });
        assertEquals(
                "package com.example;\n\n" +
                "if (true) {\n" +
                "\treturn 1;\n" +
                "}\n",
                w.toSource());
    }

    // === Import management ===================================================

    @Test void importType_returns_simple_name() {
        var w = new CodeWriter("com.example");
        String simple = w.importType("java.util.List");
        assertEquals("List", simple);
    }

    @Test void importType_same_package_not_imported() {
        var w = new CodeWriter("com.example");
        w.importType("com.example.Foo");
        assertTrue(w.getImports().isEmpty());
    }

    @Test void importType_java_lang_not_imported() {
        var w = new CodeWriter("com.example");
        w.importType("java.lang.String");
        assertTrue(w.getImports().isEmpty());
    }

    @Test void imports_sorted_alphabetically() {
        var w = new CodeWriter("com.example");
        w.importType("java.util.Map");
        w.importType("java.util.List");
        w.importType("java.io.File");
        assertEquals(
                java.util.List.of("java.io.File", "java.util.List", "java.util.Map"),
                w.getImports());
    }

    @Test void imports_appear_in_source() {
        var w = new CodeWriter("com.example");
        w.importType("java.util.List");
        w.importType("java.util.Map");
        w.appendLine("class Foo {}");
        String source = w.toSource();
        assertTrue(source.contains("import java.util.List;\n"));
        assertTrue(source.contains("import java.util.Map;\n"));
        // imports before body
        int importPos = source.indexOf("import java.util.List;");
        int bodyPos = source.indexOf("class Foo {}");
        assertTrue(importPos < bodyPos);
    }

    @Test void static_imports_before_regular_imports() {
        var w = new CodeWriter("com.example");
        w.importType("java.util.List");
        w.importStatic("java.util.Collections.emptyList");
        w.appendLine("class Foo {}");
        String source = w.toSource();
        int staticPos = source.indexOf("import static");
        int regularPos = source.indexOf("import java.util.List");
        assertTrue(staticPos < regularPos,
                "static imports should appear before regular imports");
    }

    @Test void duplicate_imports_deduplicated() {
        var w = new CodeWriter("com.example");
        w.importType("java.util.List");
        w.importType("java.util.List");
        assertEquals(1, w.getImports().size());
    }

    @Test void wildcard_import() {
        var w = new CodeWriter("com.example");
        w.addWildcardImport("java.util");
        assertTrue(w.getImports().contains("java.util.*"));
    }

    // === Blank line ==========================================================

    @Test void appendLine_no_args_emits_blank_line() {
        var w = new CodeWriter("com.example");
        w.appendLine("a");
        w.appendLine();
        w.appendLine("b");
        assertEquals(
                "package com.example;\n\na\n\nb\n",
                w.toSource());
    }

    // === getBody =============================================================

    @Test void getBody_returns_only_body() {
        var w = new CodeWriter("com.example");
        w.appendLine("class Foo {}");
        assertEquals("class Foo {}\n", w.getBody());
    }
}
