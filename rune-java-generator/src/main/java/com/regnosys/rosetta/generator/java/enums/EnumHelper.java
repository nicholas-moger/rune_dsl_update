package com.regnosys.rosetta.generator.java.enums;

import com.regnosys.rosetta.ast.supporting.REnumValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts Rosetta enum value names to Java enum constant names.
 * E.g., {@code "ActiveTrade"} → {@code "ACTIVE_TRADE"},
 * {@code "3MonthRate"} → {@code "_3_MONTH_RATE"}.
 *
 * <p>Ported from upstream Xtend — Guava's {@code CaseFormat} replaced
 * with hand-rolled camelCase→UPPER_UNDERSCORE conversion.
 */
public final class EnumHelper {

    private EnumHelper() {}

    /** Convert an enum value AST node to its Java constant name. */
    public static String convertValue(REnumValue enumValue) {
        return formatEnumName(stripEscape(enumValue.name()));
    }

    /**
     * Strip the leading caret ({@code ^}) parser-escape marker from an
     * identifier. In rune-dsl source, {@code ^} prefixes an identifier to
     * disambiguate it from a reserved keyword (e.g. DRR
     * {@code NonFinancialSectorEnum.^E} where bare {@code E} would collide
     * with the {@code E}-notation math keyword). The caret is a parse-time
     * marker, not part of the identifier; codegen must drop it before
     * emitting Java surfaces ({@code @RosettaEnumValue} value attr +
     * Java enum constant name). Upstream rune-dsl strips at the same
     * boundary; mirroring restores byte-parity per P2.1.1 T3 Cluster A
     * pilot sub-cause A.3.
     */
    public static String stripEscape(String name) {
        return name != null && name.startsWith("^") ? name.substring(1) : name;
    }

    /** Convert a Rosetta enum value name to a Java enum constant name. */
    public static String formatEnumName(String name) {
        if (noFormattingRequired(name)) {
            return name;
        }

        List<String> parts = Arrays.stream(splitAtNumbers(replaceSeparatorsWithUnderscores(name)))
                .flatMap(part -> splitAtUnderscore(part).stream())
                .flatMap(part -> splitAtCamelCase(part).stream())
                .map(EnumHelper::camelCaseToUpperUnderscoreCase)
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        return removeDuplicateUnderscores(
                prefixWithUnderscoreIfStartsWithNumber(
                        String.join("_", parts)));
    }

    private static boolean noFormattingRequired(String name) {
        // ci-allowlist: regex-on-structured-content (Java-emitted enum identifier formatting check)
        return name.matches("^[A-Z0-9_]*$");
    }

    private static String replaceSeparatorsWithUnderscores(String name) {
        return name.replace(".", "_").replace("-", "_").replace(" ", "_");
    }

    private static List<String> splitAtCamelCase(String namePart) {
        return Arrays.asList(namePart.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"));
    }

    private static List<String> splitAtUnderscore(String namePart) {
        return Arrays.asList(namePart.split("_"));
    }

    private static String[] splitAtNumbers(String namePart) {
        return namePart.split("(?=\\d)(?<=[^\\d])|(?=[^\\d])(?<=\\d)");
    }

    /**
     * If the part starts with uppercase and ends with lowercase, assume
     * it's CamelCase and convert to UPPER_UNDERSCORE.
     * Replaces Guava's {@code CaseFormat.UPPER_CAMEL.to(UPPER_UNDERSCORE)}.
     */
    private static String camelCaseToUpperUnderscoreCase(String namePart) {
        if (!namePart.isEmpty()
                && Character.isUpperCase(namePart.charAt(0))
                && Character.isLowerCase(namePart.charAt(namePart.length() - 1))) {
            return upperCamelToUpperUnderscore(namePart);
        }
        return namePart;
    }

    /**
     * Hand-rolled CamelCase → UPPER_UNDERSCORE conversion.
     * Matches Guava's {@code CaseFormat.UPPER_CAMEL.to(UPPER_UNDERSCORE)}.
     */
    private static String upperCamelToUpperUnderscore(String camelCase) {
        var sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                char prev = camelCase.charAt(i - 1);
                if (Character.isLowerCase(prev)) {
                    sb.append('_');
                } else if (i + 1 < camelCase.length() && Character.isLowerCase(camelCase.charAt(i + 1))) {
                    sb.append('_');
                }
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    private static String removeDuplicateUnderscores(String name) {
        return name.replace("__", "_");
    }

    private static String prefixWithUnderscoreIfStartsWithNumber(String name) {
        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            return "_" + name;
        }
        return name;
    }
}
