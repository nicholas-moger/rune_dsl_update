package com.regnosys.rosetta.generator.java.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Collects and deduplicates Java import statements for generated source files.
 * Extracted from {@link com.regnosys.rosetta.generator.java.CodeWriter} — retains
 * only the import-tracking responsibility, not body generation.
 *
 * <p>Imports are sorted alphabetically (TreeSet). Same-package and {@code java.lang}
 * imports are automatically excluded.
 *
 * @see com.regnosys.rosetta.generator.java.CodeWriter
 */
public class ImportCollector {

    private final String packageName;
    private final Set<String> imports = new TreeSet<>();
    private final Set<String> staticImports = new TreeSet<>();

    /**
     * Creates an import collector for the given Java package.
     *
     * @param packageName the fully-qualified package name (e.g., "com.example.model")
     */
    public ImportCollector(String packageName) {
        this.packageName = java.util.Objects.requireNonNull(packageName, "packageName");
    }

    /**
     * Add a regular import. Same-package and {@code java.lang} imports are skipped.
     *
     * @param canonicalName fully-qualified class name (e.g., "java.util.List")
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
     * Add an import and return the simple name for use in generated code.
     * Same-package and {@code java.lang} imports are skipped but the simple name
     * is still returned.
     *
     * @param canonicalName fully-qualified class name (e.g., "java.util.List")
     * @return the simple class name (e.g., "List")
     */
    public String importType(String canonicalName) {
        int lastDot = canonicalName.lastIndexOf('.');
        if (lastDot < 0) return canonicalName;
        String pkg = canonicalName.substring(0, lastDot);
        String simpleName = canonicalName.substring(lastDot + 1);
        if (!pkg.equals(packageName) && !pkg.equals("java.lang")) {
            imports.add(canonicalName);
        }
        return simpleName;
    }

    /**
     * Add a static import.
     *
     * @param canonicalName fully-qualified static member name
     *                      (e.g., "java.util.Collections.emptyList")
     */
    public void addStaticImport(String canonicalName) {
        staticImports.add(canonicalName);
    }

    /**
     * Add a static import and return the simple member name for use in code.
     * Matches {@link com.regnosys.rosetta.generator.java.CodeWriter#importStatic(String)}.
     *
     * @param canonicalName fully-qualified static member name
     *                      (e.g., "java.util.Collections.emptyList")
     * @return the simple member name (e.g., "emptyList")
     */
    public String importStatic(String canonicalName) {
        int lastDot = canonicalName.lastIndexOf('.');
        String memberName = canonicalName.substring(lastDot + 1);
        staticImports.add(canonicalName);
        return memberName;
    }

    /** Get all regular imports, sorted alphabetically. */
    public List<String> getImports() {
        return Collections.unmodifiableList(new ArrayList<>(imports));
    }

    /** Get all static imports, sorted alphabetically. */
    public List<String> getStaticImports() {
        return Collections.unmodifiableList(new ArrayList<>(staticImports));
    }

    /** Check if there are any regular imports. */
    public boolean hasImports() { return !imports.isEmpty(); }

    /** Check if there are any static imports. */
    public boolean hasStaticImports() { return !staticImports.isEmpty(); }
}
