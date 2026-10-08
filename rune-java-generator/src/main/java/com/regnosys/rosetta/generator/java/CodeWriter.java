package com.regnosys.rosetta.generator.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * StringBuilder-based Java source code writer with indentation management
 * and import collection. Replaces Xtend's {@code StringConcatenationClient}
 * and {@code ImportingStringConcatenation} for M7 code generation.
 *
 * <p>Usage:
 * <pre>
 * var w = new CodeWriter("com.example.model");
 * w.appendLine("@RosettaDataType(\"Foo\")");
 * w.appendLine("public interface Foo {");
 * w.indent();
 * w.appendLine("String getName();");
 * w.dedent();
 * w.appendLine("}");
 * String source = w.toSource();
 * </pre>
 *
 * <p>The {@link #toSource()} method assembles the final Java file:
 * package declaration, sorted imports, blank line, then body.
 */
public class CodeWriter {

    private final String packageName;
    private final StringBuilder body = new StringBuilder();
    private final Set<String> imports = new TreeSet<>();
    private final Set<String> staticImports = new TreeSet<>();
    private int indentLevel = 0;
    private boolean lineStart = true;

    private static final String INDENT = "\t";

    /**
     * Creates a new CodeWriter for the given Java package.
     *
     * @param packageName the fully-qualified package name (e.g., "com.example.model")
     */
    public CodeWriter(String packageName) {
        this.packageName = packageName;
    }

    // -- Indentation ----------------------------------------------------------

    /** Increase indentation by one level. */
    public void indent() {
        indentLevel++;
    }

    /** Decrease indentation by one level. */
    public void dedent() {
        if (indentLevel > 0) {
            indentLevel--;
        }
    }

    /** Get current indentation level. */
    public int getIndentLevel() {
        return indentLevel;
    }

    // -- Appending ------------------------------------------------------------

    /**
     * Append text to the current line. If this is the start of a new line,
     * indentation is prepended automatically.
     */
    public CodeWriter append(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        // Handle multi-line text: split on newlines and indent each line
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                String segment = text.substring(start, i);
                if (!segment.isEmpty() || lineStart) {
                    writeIndentIfNeeded();
                }
                body.append(segment);
                body.append('\n');
                lineStart = true;
                start = i + 1;
            }
        }
        // Remaining text after last newline
        if (start < text.length()) {
            writeIndentIfNeeded();
            body.append(text, start, text.length());
            lineStart = false;
        }
        return this;
    }

    /**
     * Append a complete line (text + newline). Indentation is applied
     * at the beginning of the line.
     */
    public CodeWriter appendLine(String text) {
        writeIndentIfNeeded();
        body.append(text);
        body.append('\n');
        lineStart = true;
        return this;
    }

    /** Append a blank line. */
    public CodeWriter appendLine() {
        body.append('\n');
        lineStart = true;
        return this;
    }

    /**
     * Append an indented block: emits the opening line, increases indent,
     * runs the block, decreases indent, then emits the closing line.
     *
     * <p>Example:
     * <pre>
     * w.appendBlock("public class Foo {", "}", () -> {
     *     w.appendLine("// body");
     * });
     * </pre>
     */
    public void appendBlock(String openLine, String closeLine, Runnable block) {
        appendLine(openLine);
        indent();
        block.run();
        dedent();
        appendLine(closeLine);
    }

    // -- Import management ----------------------------------------------------

    /**
     * Record an import and return the simple name to use in code.
     * Types in the same package or in {@code java.lang} are not imported.
     *
     * @param canonicalName fully-qualified class name (e.g., "java.util.List")
     * @return the simple name to use in generated code
     */
    public String importType(String canonicalName) {
        int lastDot = canonicalName.lastIndexOf('.');
        if (lastDot < 0) {
            return canonicalName; // no package
        }
        String pkg = canonicalName.substring(0, lastDot);
        String simpleName = canonicalName.substring(lastDot + 1);

        if (!pkg.equals(packageName) && !pkg.equals("java.lang")) {
            imports.add(canonicalName);
        }
        return simpleName;
    }

    /**
     * Record a static import.
     *
     * @param canonicalName fully-qualified static member name
     *                      (e.g., "java.util.Collections.emptyList")
     * @return the simple member name to use in generated code
     */
    public String importStatic(String canonicalName) {
        int lastDot = canonicalName.lastIndexOf('.');
        String memberName = canonicalName.substring(lastDot + 1);
        staticImports.add(canonicalName);
        return memberName;
    }

    /**
     * Add an import directly without returning a simple name.
     * Useful when the import is needed but the name reference is handled separately.
     */
    public void addImport(String canonicalName) {
        int lastDot = canonicalName.lastIndexOf('.');
        if (lastDot >= 0) {
            String pkg = canonicalName.substring(0, lastDot);
            if (!pkg.equals(packageName) && !pkg.equals("java.lang")) {
                imports.add(canonicalName);
            }
        }
    }

    /**
     * Add a wildcard import for a package.
     */
    public void addWildcardImport(String packagePath) {
        imports.add(packagePath + ".*");
    }

    /** Get all collected imports (sorted). */
    public List<String> getImports() {
        return Collections.unmodifiableList(new ArrayList<>(imports));
    }

    /** Get all collected static imports (sorted). */
    public List<String> getStaticImports() {
        return Collections.unmodifiableList(new ArrayList<>(staticImports));
    }

    // -- Output ---------------------------------------------------------------

    /**
     * Assemble the final Java source file:
     * <ol>
     *   <li>Package declaration</li>
     *   <li>Blank line</li>
     *   <li>Static imports (sorted, if any)</li>
     *   <li>Regular imports (sorted, if any)</li>
     *   <li>Blank line</li>
     *   <li>Body</li>
     * </ol>
     */
    public String toSource() {
        var sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n");

        boolean hasStaticImports = !staticImports.isEmpty();
        boolean hasRegularImports = !imports.isEmpty();

        if (hasStaticImports || hasRegularImports) {
            sb.append('\n');
        }

        if (hasStaticImports) {
            for (String imp : staticImports) {
                sb.append("import static ").append(imp).append(";\n");
            }
            if (hasRegularImports) {
                sb.append('\n');
            }
        }

        if (hasRegularImports) {
            for (String imp : imports) {
                sb.append("import ").append(imp).append(";\n");
            }
        }

        sb.append('\n');
        sb.append(body);
        return sb.toString();
    }

    /** Get just the body content (no package/imports). */
    public String getBody() {
        return body.toString();
    }

    // -- Internal -------------------------------------------------------------

    private void writeIndentIfNeeded() {
        if (lineStart) {
            for (int i = 0; i < indentLevel; i++) {
                body.append(INDENT);
            }
            lineStart = false;
        }
    }
}
