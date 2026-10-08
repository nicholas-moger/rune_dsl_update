package com.regnosys.rosetta.generator.java.template;

/**
 * Shared string utilities for Java code generation templates.
 */
public final class JavaStringUtil {

    private JavaStringUtil() {}

    /**
     * Escape a string for a Java source string literal — the escape every LITERAL-emitting seat of the
     * fork's generated Java goes through (two switch-guard emitters are a declared exception, named
     * below), byte-identical to upstream's {@code org.apache.commons.text.StringEscapeUtils.escapeJava}
     * (commons-text 1.12.0, the version {@code rune-dsl/pom.xml} pins): the quote/backslash lookup
     * ({@code "} → {@code \"}, {@code \} → {@code \\}), the {@code JAVA_CTRL_CHARS_ESCAPE} table
     * ({@code \b \n \t \f \r} → their two-char escapes) and
     * {@code JavaUnicodeEscaper.outsideOf(32, 0x7f)} — every other UTF-16 unit below {@code 0x20} or
     * above {@code 0x7f} renders {@code \\uXXXX} with UPPERCASE hex ({@code CharSequenceTranslator.HEX_DIGITS});
     * {@code 0x20..0x7f} (space through DEL) stays raw. A non-BMP code point is two surrogate units, each
     * escaped ({@code \\uD83D\\uDE00} — the same text commons-text's {@code toUtf16Escape} writes).
     *
     * <p>v3.1 flip seat 22 (facet {@code labelUnicodeEscape}): before this seat only the five
     * {@code " \ \n \r \t} cases were handled and every other char was appended RAW — drr 7.0–7.3's
     * {@code SECPPDLabelProvider}/{@code SECTradeLabelProvider} carried the en dash of
     * {@code "13n–5(b)(1)(iii) Submitting Party ID"} raw where golden has {@code 13n\\u20135(b)…}
     * (8 whole-file carriers; compiles both ways — a pure parity heal). LAW 75: exactly 8 golden files
     * corpus-wide carry a {@code \\uXXXX} escape (the 8 carriers); 269 goldens carry raw non-ASCII on
     * 510 javadoc/comment lines and ZERO code lines — so this escape is applied ONLY by the literal
     * emitters (enum display names / synonyms, label-provider labels, paths and edges, type-format
     * patterns, and the expression string literals on BOTH routes) and never by a comment emitter.
     * The reference behaviour was re-derived by running commons-text 1.12.0 itself
     * ({@code target/seat22-instruments/artefacts/escapejava-reference.txt}): {@code \b}/{@code \f}
     * take the two-char table form (NOT {@code \\u0008}/{@code \\u000C}), DEL ({@code 0x7f}) stays raw,
     * hex is uppercase ({@code \\u4E2D}, {@code \\u00E9}).
     *
     * <p>LAW 69 (the two halves that must agree): the legacy expression string literal
     * ({@code LiteralHandler.handle(RStringLiteral)}) and the IR-route leaf
     * ({@code IRJavaLeafEmitter.emitString}) each carried a five-case private mirror of this method
     * before seat 22; both now CONSULT this ONE method — upstream's {@code JavaLiteral.from(String)}
     * uses the very same {@code StringEscapeUtils.escapeJava} as the template emitters.
     *
     * <p><b>THE DECLARED EXCEPTION — two quote/backslash-only mirrors survive at the SWITCH-GUARD seats</b>
     * ({@code FunctionExpressionRenderer.renderSwitchGuardMapper} and {@code ControlFlowHandler.renderGuard}):
     * each spells a string guard as {@code "\"" + value.replace(BACKSLASH, BACKSLASH BACKSLASH).replace(QUOTE,
     * BACKSLASH QUOTE) + "\""}. They take the SAME decoded text this method's callers take (the parser's
     * {@code buildSwitchCaseGuard} → {@code stripQuotes} → {@code unescapeStringLiteral}) and upstream routes
     * it through {@code JavaLiteral.from(String)} = {@code StringEscapeUtils.escapeJava}, so they are a LAW-69
     * RESIDUE, not a reasoned decline. They are NOT retired here because this seat's rings, matrix and
     * receipts are already measured at its final head and a provably zero-delta edit would cost the whole
     * chain again. <b>The delta IS provably zero today (LAW 75, measured at the seat-22 head over the whole
     * 4,062-file corpus by {@code target/seat22-instruments/switchguard-scan.py}): 17,427 string-literal
     * switch guards, 2,113 distinct, and ZERO carrying a backslash or a character outside
     * {@code 0x20..0x7e}</b> — inside that band this method escapes exactly {@code "} and {@code \}, which is
     * exactly what the two mirrors do, so they are byte-identical to it on every corpus input. (The same
     * scan finds 1,020 corpus files whose OTHER string literals do carry such characters — label, synonym and
     * description text — which is why those seats route through this method.) The retirement is BANKED for
     * its own seat, and it is not a pure body swap: neither class imports this one, and a mutation over it
     * would be information-free until a fixture carries an escape-needing guard (LAW 66).
     *
     * @param str the string to escape, or null
     * @return the escaped string, or null if input was null
     */
    public static String escapeJava(String str) {
        if (str == null) return null;
        var sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                case '\f' -> sb.append("\\f");
                case '\r' -> sb.append("\\r");
                default -> {
                    if (c < 0x20 || c > 0x7f) {
                        // JavaUnicodeEscaper.outsideOf(32, 0x7f): four UPPERCASE hex digits per UTF-16 unit
                        sb.append("\\u")
                          .append(HEX_DIGITS[(c >> 12) & 0xF])
                          .append(HEX_DIGITS[(c >> 8) & 0xF])
                          .append(HEX_DIGITS[(c >> 4) & 0xF])
                          .append(HEX_DIGITS[c & 0xF]);
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /** commons-text {@code CharSequenceTranslator.HEX_DIGITS} — uppercase, the golden {@code \\uXXXX} case. */
    private static final char[] HEX_DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
}
