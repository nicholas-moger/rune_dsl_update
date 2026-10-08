package com.regnosys.rosetta.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;

/**
 * {@link RosettaLexer} subclass that de-escapes keyword-escaped identifiers.
 *
 * <p>The Rune grammar lets any identifier be prefixed with {@code ^} to escape a
 * reserved keyword — {@code ^type}, {@code ^definition}, {@code ^inputs} are
 * identifiers, not the {@code type}/{@code definition}/{@code inputs} keywords. The
 * lexer rule {@code ID : '^'? [a-zA-Z_] [a-zA-Z_0-9]* ;} therefore admits the caret
 * into the matched {@code ID} token text. Upstream Xtext strips the caret in the
 * {@code ID} terminal's value converter; the ANTLR pipeline has no value converter,
 * so without this hook the caret leaks into every downstream consumer — including
 * the canonical attribute / parameter name stored in the model and, in turn,
 * generated Java ({@code get^definition()}, {@code @RosettaAttribute("^definition")},
 * {@code evaluate(^type)}), which does not compile.
 *
 * <p>De-escaping in {@link #emit()} is the single <em>symmetric</em> chokepoint:
 * every identifier — definition and reference alike — is normalised to its logical
 * name before the parser or AST builder reads the token, so cross-references keep
 * resolving (the linker compares {@code definition} to {@code definition}, exactly
 * as it previously compared {@code ^definition} to {@code ^definition}). Mirrors the
 * Java-side de-escaping the {@code AstBuilderHelper} already performs for string
 * literals, rather than embedding a target-specific action in the grammar.
 *
 * <p>{@link #setText} does not change the token's source char indices
 * ({@code getStartIndex()} / {@code getStopIndex()}), so the load-bearing byte
 * offsets in {@code SourceRange} — the only range data the code generator consumes —
 * still span the escaped form ({@code ^definition}) in the source. The 1-based column
 * <em>end</em> is, however, derived from {@code getText().length()}
 * ({@code SourceRange.of}), so for an escaped identifier the reported end column is one
 * short ({@code definition}=10, not {@code ^definition}=11). That is cosmetic: it
 * affects only LSP / diagnostic squiggle extents ({@code ParserDiagnostic}) on escaped
 * identifiers, never code generation. Correcting the column to the char-index span is a
 * separate {@code SourceRange} concern, out of scope here.
 */
public class RosettaIdLexer extends RosettaLexer {

    public RosettaIdLexer(CharStream input) {
        super(input);
    }

    @Override
    public Token emit() {
        if (getType() == ID) {
            String text = getText();
            if (text != null && !text.isEmpty() && text.charAt(0) == '^') {
                setText(text.substring(1));
            }
        }
        return super.emit();
    }
}
