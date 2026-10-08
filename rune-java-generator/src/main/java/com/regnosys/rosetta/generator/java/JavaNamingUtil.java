package com.regnosys.rosetta.generator.java;

import javax.lang.model.SourceVersion;

/**
 * Shared naming conventions for Java identifier transformations used across
 * the code generator.
 *
 * <p>Centralises the first-letter case flip so that POJO getters/setters,
 * alias invocations, validator method names, and function-body builder
 * chains all produce identical method names for the same Rune attribute.
 * A per-file reimplementation risks drift — a segment named {@code iD}
 * could become {@code getID} in one generator and {@code getId} in
 * another, breaking byte-identical code generation.
 *
 * <p>Null/empty handling: these methods are tolerant — {@code null} and
 * {@code ""} pass through unchanged. Callers that need fail-loud behaviour
 * (e.g. when a null value would emit invalid Java like {@code .setnull(...)})
 * must validate with {@link java.util.Objects#requireNonNull} before the
 * call.
 */
public final class JavaNamingUtil {

    private JavaNamingUtil() {}

    /**
     * Capitalise the first character of a name, leaving the remainder
     * unchanged. Used to build Java method names from Rune attribute names
     * (e.g., {@code "adjustedDate"} → {@code "AdjustedDate"}, then prefixed
     * with {@code get}/{@code set}/{@code getOrCreate}).
     *
     * @param name identifier name; {@code null} or empty is returned unchanged
     * @return the input with its first character upper-cased, or the input if null/empty
     */
    public static String toFirstUpper(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Lower-case the first character of a name, leaving the remainder
     * unchanged. Used to build lowerCamelCase identifiers from PascalCase
     * type names (e.g., {@code "BusinessEvent"} → {@code "businessEvent"}).
     *
     * @param name identifier name; {@code null} or empty is returned unchanged
     * @return the input with its first character lower-cased, or the input if null/empty
     */
    public static String toFirstLower(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Escape a Java reserved word used as an IDENTIFIER (POJO field, setter
     * parameter, local) by prefixing {@code "_"}, leaving any valid identifier
     * unchanged.
     *
     * <p>Mirrors the generator's scope-driven escape exactly: upstream registers
     * POJO field/param names into a {@code JavaClassScope}/{@code JavaMethodScope}
     * whose {@code isValidIdentifier} delegates to
     * {@link SourceVersion#isName(CharSequence)} and whose {@code escapeName}
     * prepends {@code "_"} (see {@code AbstractJavaScope} / {@code JavaClassScope}).
     * {@code SourceVersion.isName} returns {@code false} for any Java reserved
     * keyword ({@code new}/{@code short}/{@code long}/{@code return}/…) and for the
     * {@code true}/{@code false}/{@code null} literals, so this escapes exactly
     * that set — no hardcoded keyword list, no over-escaping of ordinary names
     * (e.g. {@code value}, {@code getValue}) or the <em>restricted</em> keywords
     * ({@code var}/{@code yield}/{@code record}/{@code sealed}/{@code permits}),
     * which {@code SourceVersion.isName} accepts as valid identifiers and so leaves
     * unescaped (verified on the JDK 21 build JVM: {@code isName("record")} →
     * {@code true}, {@code isName("new")} → {@code false}).
     *
     * <p>This is the FIELD-side single-{@code _} escape. The IMPL setter parameter
     * gets a SECOND {@code _} (→ {@code __new}) for free where the caller already
     * prefixes {@code "_"} to the (now-escaped) field name, matching upstream's
     * field-vs-param scope de-duplication.
     *
     * <p>Null/empty pass through unchanged, consistent with the sibling helpers.
     *
     * @param name an identifier name; {@code null} or empty is returned unchanged
     * @return {@code "_" + name} when {@code name} is not a valid identifier per
     *         {@link SourceVersion#isName(CharSequence)} — i.e. a reserved keyword
     *         or a {@code true}/{@code false}/{@code null} literal — otherwise
     *         {@code name} unchanged
     */
    public static String escapeJavaKeyword(String name) {
        if (name == null || name.isEmpty()) return name;
        return SourceVersion.isName(name) ? name : "_" + name;
    }
}
