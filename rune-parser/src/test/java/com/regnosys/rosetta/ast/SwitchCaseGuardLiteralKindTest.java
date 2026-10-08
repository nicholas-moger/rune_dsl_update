package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.enums.SwitchGuardLiteralKind;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * v3.1 C2d, retirement family 1 ({@code numeric-literal-kind}) — the front-end addition the
 * family's Integer-arm row named as {@code NONE-YET}: the AST builder records the lexer-terminal
 * kind of every LITERAL switch-case guard beside its value text
 * ({@link RSwitchCaseGuard#literalKind()}), so a consumer can tell a STRING guard {@code "42"}
 * from an INT_LITERAL guard {@code 42} without classifying the text. Parsed from source — the
 * only place the terminal is visible — over one guard of each kind, in the order the grammar
 * lists them.
 */
class SwitchCaseGuardLiteralKindTest {

    private static final String MODEL = String.join("\n",
            "namespace test.guards",
            "version \"0.0.0\"",
            "",
            "enum Colour:",
            "    RED",
            "",
            "func Classify:",
            "    inputs:",
            "        s string (1..1)",
            "        n int (1..1)",
            "        d number (1..1)",
            "        b boolean (1..1)",
            "        c Colour (1..1)",
            "    output:",
            "        r boolean (1..1)",
            "    condition Strings:",
            "        s switch \"42\" then True, \"one\" then False, 'two' then True, default False",
            "    condition Ints:",
            "        n switch 42 then True, 007 then False, default False",
            "    condition Decimals:",
            "        d switch 3.14 then True, 1e3 then False, .5 then True, 3.e4 then False, default False",
            "    condition Booleans:",
            "        b switch True then True, False then False",
            "    condition Names:",
            "        c switch RED then True, default False",
            "    set r: True");

    private static List<RSwitchCaseGuard> guards() {
        RModel model = AstBuilder.buildFromString(MODEL, "guards.rosetta");
        return AstWalker.findAll(model, RSwitchCaseGuard.class);
    }

    @Test
    void every_literal_guard_carries_the_terminal_kind_beside_its_value() {
        List<RSwitchCaseGuard> g = guards();
        assertEquals(12, g.size(), "guards in source order: 3 strings, 2 ints, 4 decimals, 2 booleans, 1 name");

        // STRING — quote-stripped values, both quote styles; "42" is a STRING, not an INT
        assertLiteral(g.get(0), SwitchGuardLiteralKind.STRING, "42");
        assertLiteral(g.get(1), SwitchGuardLiteralKind.STRING, "one");
        assertLiteral(g.get(2), SwitchGuardLiteralKind.STRING, "two");
        // INT_LITERAL — raw digits (a leading zero is kept: the value is the terminal's text)
        assertLiteral(g.get(3), SwitchGuardLiteralKind.INT, "42");
        assertLiteral(g.get(4), SwitchGuardLiteralKind.INT, "007");
        // BIG_DECIMAL — all four alternatives of the lexer rule: DIGIT+ '.' DIGIT+ EXPONENT?,
        // DIGIT+ EXPONENT, '.' DIGIT+ EXPONENT?, DIGIT+ '.' EXPONENT
        assertLiteral(g.get(5), SwitchGuardLiteralKind.DECIMAL, "3.14");
        assertLiteral(g.get(6), SwitchGuardLiteralKind.DECIMAL, "1e3");
        assertLiteral(g.get(7), SwitchGuardLiteralKind.DECIMAL, ".5");
        assertLiteral(g.get(8), SwitchGuardLiteralKind.DECIMAL, "3.e4");
        // TRUE / FALSE — the raw keyword text, capitalised as the grammar spells it
        assertLiteral(g.get(9), SwitchGuardLiteralKind.BOOLEAN, "True");
        assertLiteral(g.get(10), SwitchGuardLiteralKind.BOOLEAN, "False");
        // a NAME guard carries no literal kind
        assertEquals(SwitchGuardKind.NAME, g.get(11).kind());
        assertEquals(Optional.of("RED"), g.get(11).qualifiedName());
        assertEquals(Optional.empty(), g.get(11).literalKind());
        assertEquals(Optional.empty(), g.get(11).literalValue());
    }

    @Test
    void the_same_value_text_is_told_apart_by_the_kind_alone() {
        List<RSwitchCaseGuard> g = guards();
        RSwitchCaseGuard stringFortyTwo = g.get(0);
        RSwitchCaseGuard intFortyTwo = g.get(3);
        assertEquals(stringFortyTwo.literalValue(), intFortyTwo.literalValue(), "the value text is identical");
        assertEquals(Optional.of(SwitchGuardLiteralKind.STRING), stringFortyTwo.literalKind());
        assertEquals(Optional.of(SwitchGuardLiteralKind.INT), intFortyTwo.literalKind());
    }

    private static void assertLiteral(RSwitchCaseGuard guard, SwitchGuardLiteralKind kind, String value) {
        assertEquals(SwitchGuardKind.LITERAL, guard.kind());
        assertEquals(Optional.of(kind), guard.literalKind(), "kind of " + value);
        assertEquals(Optional.of(value), guard.literalValue(), "value of " + kind);
        assertEquals(Optional.empty(), guard.qualifiedName());
    }
}
