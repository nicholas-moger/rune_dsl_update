package com.regnosys.rosetta.ast.builder;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.parser.RosettaParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.stream.Collectors;

/**
 * Static utility methods used by {@link AstBuilder} to extract source ranges,
 * build qualified names, and wire parent pointers.
 *
 * <p>All methods are stateless and side-effect-free unless noted otherwise.
 */
public final class AstBuilderHelper {

    private AstBuilderHelper() {}

    // -- Source range extraction -----------------------------------------------

    /**
     * Extracts a {@link SourceRange} covering the entire span of the given
     * parser rule context.
     *
     * @param ctx      the parser rule context (must not be null)
     * @param fileName the source file name to embed in the range
     * @return a {@link SourceRange} for the full extent of the context
     */
    public static SourceRange rangeOf(ParserRuleContext ctx, String fileName, int[] charToByte) {
        Token start = ctx.getStart();
        Token stop = ctx.getStop();
        if (start == null || stop == null) {
            return SourceRange.NONE;
        }
        if (charToByte == null
                || start.getStartIndex() < 0
                || stop.getStopIndex() < 0
                || stop.getStopIndex() + 1 >= charToByte.length) {
            // No byte-offset table, or synthesised tokens with -1 indices,
            // or stop-index past EOF — fall back to sentinel offsets.
            return SourceRange.of5Arg(
                    fileName,
                    start.getLine(),
                    start.getCharPositionInLine() + 1,
                    stop.getLine(),
                    stop.getCharPositionInLine() + stop.getText().length()
            );
        }
        return SourceRange.of(start, stop, fileName, charToByte);
    }

    /**
     * Extracts a {@link SourceRange} for a single token.
     *
     * @param token      the token (must not be null)
     * @param fileName   the source file name to embed in the range
     * @param charToByte char-to-byte projection table, or {@code null} for sentinel offsets
     * @return a {@link SourceRange} for the token
     */
    public static SourceRange rangeOfToken(Token token, String fileName, int[] charToByte) {
        if (charToByte == null
                || token.getStartIndex() < 0
                || token.getStopIndex() < 0
                || token.getStopIndex() + 1 >= charToByte.length) {
            return SourceRange.of5Arg(
                    fileName,
                    token.getLine(),
                    token.getCharPositionInLine() + 1,
                    token.getLine(),
                    token.getCharPositionInLine() + token.getText().length()
            );
        }
        return SourceRange.of(token, token, fileName, charToByte);
    }

    // -- Parent wiring --------------------------------------------------------

    /**
     * Recursively sets the parent pointer on every child of the given node,
     * then recurses into each child.
     *
     * <p>Call this once on the root {@code RModel} after the builder has
     * constructed the full tree.
     *
     * @param parent the node whose children should have their parent set
     */
    public static void setParents(RNode parent) {
        for (RNode child : parent.children()) {
            child.setParent(parent);
            setParents(child);
        }
    }

    // -- Qualified name extraction --------------------------------------------

    /**
     * Builds the dotted qualified name string from a {@code qualifiedName}
     * parse tree context.
     *
     * <p>Example: for {@code com.example.model}, the context contains three
     * {@code validID} children separated by DOT tokens. This method joins
     * the text of each {@code validID} with dots.
     *
     * @param ctx the qualifiedName context
     * @return the dot-separated qualified name string
     */
    public static String textOfQualifiedName(RosettaParser.QualifiedNameContext ctx) {
        return ctx.validID().stream()
                .map(id -> id.getText())
                .collect(Collectors.joining("."));
    }

    // -- Definable text extraction --------------------------------------------

    /**
     * Extracts the text from a {@code definable} rule context.
     *
     * <p>The grammar is {@code LT STRING GT}, where STRING includes its
     * surrounding quotes. This method strips the quotes and returns the
     * inner text.
     *
     * @param ctx the definable context (must not be null)
     * @return the unquoted definition string
     */
    public static String textOfDefinable(RosettaParser.DefinableContext ctx) {
        return stripQuotes(ctx.STRING().getText());
    }

    // -- Token text extraction (Xtext getTokenText semantics) ------------------

    /**
     * Extracts the normalized token text of a parse subtree — the exact analogue
     * of Xtext's {@code NodeModelUtils.getTokenText(INode)}, which upstream's
     * {@code RosettaGrammarUtil.extractNodeText} applies to a feature's node:
     * the non-hidden token texts in source order, joined with a SINGLE space
     * wherever one-or-more hidden tokens (whitespace, newlines, comments)
     * intervened between two consecutive non-hidden tokens.
     *
     * <p>ANTLR equivalence: the parse tree's {@link TerminalNode}s are exactly
     * the default-channel (non-hidden) tokens, and hidden material between two
     * consecutive terminals manifests as a character-index gap
     * ({@code next.startIndex > prev.stopIndex + 1}) — hidden tokens are never
     * zero-length, so gap-detection and Xtext's {@code hiddenSeen} flag agree
     * exactly. Leading hidden material produces no leading space (Xtext appends
     * only when the builder is non-empty); trailing hidden material is after the
     * last terminal and produces nothing.
     *
     * <p>Consumed by the {@code condition} visitor to capture the expression's
     * source text ({@code RCondition#expressionText()}) for the datarule
     * DEFINITION constant (coverage wave D).
     *
     * @param tree the parse subtree (e.g. an expression rule context)
     * @return the normalized token text; empty string for an empty subtree
     */
    public static String tokenText(org.antlr.v4.runtime.tree.ParseTree tree) {
        StringBuilder sb = new StringBuilder();
        appendTokenText(tree, sb, new int[]{Integer.MIN_VALUE});
        return sb.toString();
    }

    private static void appendTokenText(org.antlr.v4.runtime.tree.ParseTree node,
            StringBuilder sb, int[] prevStopIndex) {
        if (node instanceof org.antlr.v4.runtime.tree.TerminalNode terminal) {
            Token tok = terminal.getSymbol();
            if (tok.getType() == Token.EOF) {
                return;
            }
            if (sb.length() > 0 && tok.getStartIndex() > prevStopIndex[0] + 1) {
                sb.append(' ');
            }
            // The RAW source span, not getText(): RosettaIdLexer de-escapes
            // keyword-escaped identifiers by setText ("^type" → "type") while the
            // char indices keep spanning the escaped source — and the upstream
            // node text this reproduces is the SOURCE form ("optional choice
            // ^type, …", golden CoalProductChoice's DEFINITION).
            var input = tok.getInputStream();
            if (input != null && tok.getStartIndex() >= 0
                    && tok.getStopIndex() >= tok.getStartIndex()) {
                sb.append(input.getText(org.antlr.v4.runtime.misc.Interval.of(
                        tok.getStartIndex(), tok.getStopIndex())));
            } else {
                sb.append(tok.getText());
            }
            prevStopIndex[0] = tok.getStopIndex();
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            appendTokenText(node.getChild(i), sb, prevStopIndex);
        }
    }

    // -- String utility -------------------------------------------------------

    /**
     * Strips surrounding double or single quotes from a STRING token text.
     *
     * @param text the raw token text (e.g., {@code "hello"} or {@code 'hello'})
     * @return the inner text without quotes
     */
    public static String stripQuotes(String text) {
        if (text == null || text.length() < 2) {
            return text;
        }
        char first = text.charAt(0);
        char last = text.charAt(text.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return unescapeStringLiteral(text.substring(1, text.length() - 1));
        }
        return text;
    }

    /**
     * Unescape standard Java/Rosetta string escape sequences.
     * Handles: {@code \'} → {@code '}, {@code \"} → {@code "}, {@code \\} → {@code \},
     * {@code \t} → tab, {@code \n} → newline, {@code \r} → carriage return.
     * Upstream EMF parser unescapes these automatically; our ANTLR parser does not.
     */
    static String unescapeStringLiteral(String s) {
        if (s == null || !s.contains("\\")) return s;
        var sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '\'' -> { sb.append('\''); i++; }
                    case '"'  -> { sb.append('"');  i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case 't'  -> { sb.append('\t'); i++; }
                    case 'n'  -> { sb.append('\n'); i++; }
                    case 'r'  -> { sb.append('\r'); i++; }
                    default   -> sb.append(c); // Unknown escape: preserve as-is
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
