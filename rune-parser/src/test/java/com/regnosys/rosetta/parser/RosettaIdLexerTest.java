package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Locks {@link RosettaIdLexer}'s keyword-escape de-escaping.
 *
 * <p>An identifier may be prefixed with {@code ^} to escape a reserved keyword
 * ({@code ^type}, {@code ^definition} are identifiers, not the {@code type}/
 * {@code definition} keywords). Upstream Xtext strips the caret in the {@code ID}
 * value converter; the ANTLR pipeline does so here, at the single lexer chokepoint,
 * so the canonical logical name — used for the model, cross-reference resolution and
 * generated Java — never carries the caret. Without it, generated Java leaked
 * {@code get^definition()} / {@code @RosettaAttribute("^definition")}, which does not
 * compile (root cause of the rune-fpml POJO {@code ^type}/{@code ^definition}
 * divergences).
 */
class RosettaIdLexerTest {

    private static List<String> idTexts(String src) {
        var lexer = new RosettaIdLexer(CharStreams.fromString(src));
        return lexer.getAllTokens().stream()
                .filter(t -> t.getType() == RosettaLexer.ID)
                .map(Token::getText)
                .toList();
    }

    @Test
    void escapedIdentifiersAreDeEscaped() {
        // ^foo / ^baz are escaped IDs; bar is a plain ID. All surface without the caret.
        assertEquals(List.of("foo", "bar", "baz"), idTexts("^foo bar ^baz"));
    }

    @Test
    void escapedKeywordBecomesIdentifierNotKeyword() {
        // `type` is the TYPE keyword (not an ID); `^type` is an ID whose text de-escapes to `type`.
        assertEquals(List.of(), idTexts("type"));
        assertEquals(List.of("type"), idTexts("^type"));
    }

    @Test
    void plainIdentifierTextUnchanged() {
        assertEquals(List.of("plainName"), idTexts("plainName"));
    }

    @Test
    void escapedAttributeNameDeEscapedInModel() {
        // `definition` and `type` are reserved keywords; as attribute names they must be
        // escaped in source. The model must store the de-escaped logical name.
        String src = """
                namespace foo

                type Bar:
                  ^definition string (1..1)
                  ^type string (1..1)
                  plain string (1..1)
                """;
        RModel model = AstBuilder.buildFromString(src, "inline-test");
        RDataType bar = (RDataType) model.rootElements().get(0);
        List<String> names = bar.attributes().stream().map(RAttribute::name).toList();
        assertEquals(List.of("definition", "type", "plain"), names);
        names.forEach(n -> assertNotEquals('^', n.charAt(0), "attribute name leaked caret: " + n));
    }
}
