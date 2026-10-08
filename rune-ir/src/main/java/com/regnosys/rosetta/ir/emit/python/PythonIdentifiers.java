package com.regnosys.rosetta.ir.emit.python;

import com.regnosys.rosetta.ir.emit.EmitterException;

import java.util.Locale;
import java.util.Set;

/**
 * Python identifier rules shared by the noun ({@link IRPythonDeclarationEmitter}) and verb
 * ({@link IRPythonEmitter}) emitters, so both halves of the Python target mangle a Rune name identically. A hard
 * Python keyword is <strong>sanitized</strong> with a trailing underscore (PEP-8, {@code None → None_}); a null /
 * non-ASCII / blank / bad-character name <strong>declines</strong> via {@link EmitterException}. A
 * <strong>value-level</strong> identifier (struct field, function, parameter, local binder / reference)
 * additionally converts to Pythonic {@code snake_case} before sanitizing (see {@link #snakeName}); a
 * <strong>type</strong> identifier (class / enum name) sanitizes only and keeps its source PascalCase
 * (see {@link #safeName}).
 */
final class PythonIdentifiers {

    private PythonIdentifiers() {}

    /** The 35 hard Python-3 keywords (Java has no runtime {@code keyword.kwlist}). Soft keywords are allowed. */
    private static final Set<String> PY_KEYWORDS = Set.of(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break",
            "class", "continue", "def", "del", "elif", "else", "except", "finally",
            "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
            "not", "or", "pass", "raise", "return", "try", "while", "with", "yield");

    /** A hard keyword → {@code name + "_"} (PEP-8, {@code None → None_}); else, if a valid ASCII identifier,
     *  {@code name} unchanged; otherwise (null / non-ASCII / blank / bad-char) declines. Every keyword is a valid
     *  ASCII identifier, so the validity guard passes it through to the sanitizing return. */
    static String sanitize(String name) {
        if (name == null || !isAsciiIdentifier(name)) {
            throw new EmitterException(
                    "PythonIdentifiers cannot emit the Python-unsafe identifier \"" + name + "\"");
        }
        return PY_KEYWORDS.contains(name) ? name + "_" : name;
    }

    /** Strips a namespace prefix then sanitizes the simple segment ({@code ns.Trade → Trade}, {@code ns.global → global_}). */
    static String safeName(String fqn) {
        return sanitize(simpleName(fqn));
    }

    /** The segment after the last {@code '.'} ({@code ns.Trade → Trade}); identity for an undotted name. */
    static String simpleName(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    /**
     * A <strong>value-level</strong> identifier (struct field, function, parameter, local binder /
     * reference) → Pythonic {@code snake_case}, then keyword-sanitized. Pairs with {@link #safeName}
     * (type names, which stay PascalCase); both return a sanitized Python identifier and differ only in
     * casing policy. Pure and <strong>idempotent</strong> over the ASCII-identifier domain
     * ({@code snakeName(snakeName(x)) == snakeName(x)}), so a caller that pre-normalizes a name will not
     * double-mangle. A {@code null} / non-ASCII / blank / bad-character name <strong>declines</strong> via
     * {@link #sanitize} exactly as a raw name would (the pre-snake {@code null} guard preserves that).
     */
    static String snakeName(String name) {
        return sanitize(name == null ? null : snakeCase(name));
    }

    /**
     * The conventional two-pass camelCase→snake_case (the Rails/{@code inflection.underscore} shape):
     * (1) split an acronym boundary {@code ([A-Z]+)([A-Z][a-z])}; (2) split a camel boundary
     * {@code ([a-z0-9])([A-Z])}; (3) lowercase with {@link Locale#ROOT}. Both passes use global
     * {@link String#replaceAll} with no flags, so the {@code [A-Z]}/{@code [a-z]} classes stay ASCII-only.
     * {@code Locale.ROOT} is load-bearing: a bare {@code toLowerCase()} maps capital {@code I} → {@code ı}
     * (U+0131) under a tr/az locale, which is non-ASCII and would then decline at {@link #sanitize}.
     * Idempotent: the output is fully lowercased, so both passes are no-ops on re-application.
     */
    private static String snakeCase(String name) {
        String s = name.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2");
        s = s.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
        return s.toLowerCase(Locale.ROOT);
    }

    /**
     * Whether {@code id} is reserved as an {@code enum.Enum} member name. The full reserved set is
     * <em>sunder</em> ∪ <em>dunder</em> ∪ {@code {mro}}:
     * <ul>
     *   <li>A <em>sunder</em> ({@code _x_}) raises {@code ValueError} at class creation (reserved for
     *       Enum internals like {@code _name_});</li>
     *   <li>A <em>dunder</em> ({@code __x__}) is treated as a normal attribute and SILENTLY DROPPED from
     *       the enum — not a usable member;</li>
     *   <li>The bare name {@code mro} is additionally reserved by CPython's {@code Enum} metaclass
     *       (verified on CPython 3.13: {@code class C(Enum): mro='mro'} raises
     *       {@code ValueError: invalid enum member name(s) 'mro'} at import). It has no sunder/dunder
     *       shape and is therefore added explicitly.</li>
     * </ul>
     * None of the three is a usable {@code enum.Enum} member, so the declaration emitter declines such a
     * member ({@link IRPythonDeclarationEmitter#emitEnum}).
     *
     * <p>Dunder-shaped identifiers ({@code __x__}) are ALSO hazardous for struct fields / class names: a
     * {@code @dataclass} with a field named {@code __init__} conflicts with the auto-generated constructor,
     * causing {@code TypeError} at instantiation. The declaration emitter declines those via {@link #isDunder}
     * directly (sunder and {@code mro} are enum-member-specific; struct fields / class names with sunder
     * shape still emit normally; {@code mro} as a struct field is harmless).
     *
     * <p>The sunder/dunder predicates mirror CPython's {@code enum._is_sunder}/{@code _is_dunder} exactly:
     * a single leading and trailing underscore that are not themselves doubled (sunder), or a double leading
     * and trailing underscore whose inner chars are not underscores (dunder). A single-side underscore
     * ({@code _x}, {@code global_}) and a bare {@code _}/{@code __} are NOT reserved and emit normally.
     */
    static boolean isEnumReserved(String id) {
        return isSunder(id) || isDunder(id) || id.equals("mro");
    }

    /**
     * Whether {@code id} has a double-underscore-both-ends <em>dunder</em> shape ({@code __x__}).
     * Dunder-shaped identifiers conflict with Python object/dataclass machinery — a {@code @dataclass} field
     * named {@code __init__} or a class named {@code __X__} collides with auto-generated special methods,
     * raising {@code TypeError} at instantiation. The declaration emitter calls this directly to decline
     * dunder-shaped struct fields, class names, and base names (distinct from {@link #isEnumReserved}, which
     * additionally covers sunder — enum-reserved only). Mirrors CPython's {@code enum._is_dunder}.
     */
    static boolean isDunder(String id) {
        int n = id.length();
        return n > 4 && id.charAt(0) == '_' && id.charAt(1) == '_'
                && id.charAt(n - 1) == '_' && id.charAt(n - 2) == '_'
                && id.charAt(2) != '_' && id.charAt(n - 3) != '_';
    }

    private static boolean isSunder(String s) {
        int n = s.length();
        return n > 2 && s.charAt(0) == '_' && s.charAt(n - 1) == '_'
                && s.charAt(1) != '_' && s.charAt(n - 2) != '_';
    }

    private static boolean isAsciiIdentifier(String s) {
        if (s.isEmpty()) {
            return false;
        }
        char c0 = s.charAt(0);
        if (!(isAsciiLetter(c0) || c0 == '_')) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(isAsciiLetter(c) || (c >= '0' && c <= '9') || c == '_')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
}
