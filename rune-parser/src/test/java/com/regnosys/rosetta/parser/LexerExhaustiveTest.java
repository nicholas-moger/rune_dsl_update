package com.regnosys.rosetta.parser;

import org.antlr.v4.runtime.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exhaustive lexer tests verifying every non-fragment token rule in RosettaLexer.g4.
 *
 * <p>The lexer grammar has 163 rules in total:
 * <ul>
 *   <li>158 non-fragment, non-hidden tokens (keywords, punctuation, literals, ID)</li>
 *   <li>3 hidden-channel tokens (WS, LINE_COMMENT, BLOCK_COMMENT)</li>
 *   <li>2 fragments (DIGIT, EXPONENT)</li>
 * </ul>
 * This test class covers all 161 non-fragment token rules (158 default-channel + 3 hidden)
 * through parameterized keyword/punctuation tests, literal/identifier tests, hidden-channel
 * tests, plus edge-case and error tests.
 */
class LexerExhaustiveTest {

    // ========================================================================
    // Helpers
    // ========================================================================

    /** Lex input, failing on any lexer error. Returns ALL tokens (including HIDDEN channel). */
    private CommonTokenStream lex(String input) {
        RosettaLexer lexer = new RosettaLexer(CharStreams.fromString(input));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> r, Object o, int line, int col, String msg, RecognitionException e) {
                fail("Lexer error at " + line + ":" + col + " - " + msg);
            }
        });
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        return tokens;
    }

    /** Lex input, collecting errors instead of failing. */
    private LexResult lexWithErrors(String input) {
        RosettaLexer lexer = new RosettaLexer(CharStreams.fromString(input));
        List<String> errors = new ArrayList<>();
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> r, Object o, int line, int col, String msg, RecognitionException e) {
                errors.add(line + ":" + col + " " + msg);
            }
        });
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        return new LexResult(tokens, errors);
    }

    private record LexResult(CommonTokenStream tokens, List<String> errors) {}

    /** Get on-channel (non-HIDDEN) tokens, excluding EOF. */
    private List<Token> onChannel(CommonTokenStream tokens) {
        return tokens.getTokens().stream()
                .filter(t -> t.getType() != Token.EOF && t.getChannel() == 0)
                .toList();
    }

    /** Assert that input produces exactly one on-channel token of the given type. */
    private void assertSingleToken(String input, int expectedType, String tokenName) {
        CommonTokenStream tokens = lex(input);
        List<Token> real = onChannel(tokens);
        assertEquals(1, real.size(),
                "Expected exactly 1 on-channel token for input '" + input + "' (" + tokenName + "), got " + real.size());
        assertEquals(expectedType, real.get(0).getType(),
                "Token type mismatch for input '" + input + "': expected " + tokenName
                        + " (" + expectedType + ") but got "
                        + RosettaLexer.VOCABULARY.getSymbolicName(real.get(0).getType())
                        + " (" + real.get(0).getType() + ")");
    }

    // ========================================================================
    // 1. Parameterized: ALL keyword tokens
    // ========================================================================

    static Stream<Arguments> allKeywords() {
        return Stream.of(
                // --- Top-level structure ---
                Arguments.of("namespace",               RosettaLexer.NAMESPACE,               "NAMESPACE"),
                Arguments.of("override",                RosettaLexer.OVERRIDE,                "OVERRIDE"),
                Arguments.of("version",                 RosettaLexer.VERSION,                 "VERSION"),
                Arguments.of("scope",                   RosettaLexer.SCOPE,                   "SCOPE"),
                Arguments.of("import",                  RosettaLexer.IMPORT,                  "IMPORT"),
                Arguments.of("as",                      RosettaLexer.AS,                      "AS"),

                // --- Type declarations ---
                Arguments.of("type",                    RosettaLexer.TYPE,                    "TYPE"),
                Arguments.of("extends",                 RosettaLexer.EXTENDS,                 "EXTENDS"),
                Arguments.of("enum",                    RosettaLexer.ENUM,                    "ENUM"),
                Arguments.of("choice",                  RosettaLexer.CHOICE,                  "CHOICE"),
                Arguments.of("basicType",               RosettaLexer.BASIC_TYPE,              "BASIC_TYPE"),
                Arguments.of("recordType",              RosettaLexer.RECORD_TYPE,             "RECORD_TYPE"),
                Arguments.of("typeAlias",               RosettaLexer.TYPE_ALIAS,              "TYPE_ALIAS"),
                Arguments.of("metaType",                RosettaLexer.META_TYPE,               "META_TYPE"),
                Arguments.of("library",                 RosettaLexer.LIBRARY,                 "LIBRARY"),
                Arguments.of("function",                RosettaLexer.FUNCTION,                "FUNCTION"),

                // --- Annotations ---
                Arguments.of("annotation",              RosettaLexer.ANNOTATION,              "ANNOTATION"),
                Arguments.of("prefix",                  RosettaLexer.PREFIX,                  "PREFIX"),

                // --- Attributes ---
                Arguments.of("displayName",             RosettaLexer.DISPLAY_NAME,            "DISPLAY_NAME"),

                // --- Functions ---
                Arguments.of("func",                    RosettaLexer.FUNC,                    "FUNC"),
                Arguments.of("inputs",                  RosettaLexer.INPUTS,                  "INPUTS"),
                Arguments.of("output",                  RosettaLexer.OUTPUT,                  "OUTPUT"),
                Arguments.of("alias",                   RosettaLexer.ALIAS,                   "ALIAS"),
                Arguments.of("set",                     RosettaLexer.SET,                     "SET"),
                Arguments.of("add",                     RosettaLexer.ADD,                     "ADD"),
                Arguments.of("post-condition",          RosettaLexer.POST_CONDITION,          "POST_CONDITION"),

                // --- Conditions / Control ---
                Arguments.of("condition",               RosettaLexer.CONDITION,               "CONDITION"),
                Arguments.of("if",                      RosettaLexer.IF,                      "IF"),
                Arguments.of("then",                    RosettaLexer.THEN,                    "THEN"),
                Arguments.of("else",                    RosettaLexer.ELSE,                    "ELSE"),
                Arguments.of("switch",                  RosettaLexer.SWITCH,                  "SWITCH"),
                Arguments.of("default",                 RosettaLexer.DEFAULT,                 "DEFAULT"),
                Arguments.of("case",                    RosettaLexer.CASE,                    "CASE"),
                Arguments.of("when",                    RosettaLexer.WHEN,                    "WHEN"),
                Arguments.of("from",                    RosettaLexer.FROM,                    "FROM"),
                Arguments.of("to",                      RosettaLexer.TO,                      "TO"),

                // --- Logic / Comparison operators ---
                Arguments.of("and",                     RosettaLexer.AND,                     "AND"),
                Arguments.of("or",                      RosettaLexer.OR,                      "OR"),
                Arguments.of("is",                      RosettaLexer.IS,                      "IS"),
                Arguments.of("exists",                  RosettaLexer.EXISTS,                  "EXISTS"),
                Arguments.of("absent",                  RosettaLexer.ABSENT,                  "ABSENT"),
                Arguments.of("contains",                RosettaLexer.CONTAINS,                "CONTAINS"),
                Arguments.of("disjoint",                RosettaLexer.DISJOINT,                "DISJOINT"),
                Arguments.of("only",                    RosettaLexer.ONLY,                    "ONLY"),
                Arguments.of("empty",                   RosettaLexer.EMPTY,                   "EMPTY"),

                // --- Cardinality modifiers ---
                Arguments.of("any",                     RosettaLexer.ANY,                     "ANY"),
                Arguments.of("all",                     RosettaLexer.ALL,                     "ALL"),
                Arguments.of("single",                  RosettaLexer.SINGLE,                  "SINGLE"),
                Arguments.of("multiple",                RosettaLexer.MULTIPLE,                "MULTIPLE"),
                Arguments.of("optional",                RosettaLexer.OPTIONAL,                "OPTIONAL"),
                Arguments.of("required",                RosettaLexer.REQUIRED,                "REQUIRED"),

                // --- List operations (hyphenated first) ---
                Arguments.of("only-element",            RosettaLexer.ONLY_ELEMENT,            "ONLY_ELEMENT"),
                Arguments.of("one-of",                  RosettaLexer.ONE_OF,                  "ONE_OF"),
                Arguments.of("extract",                 RosettaLexer.EXTRACT,                 "EXTRACT"),
                Arguments.of("filter",                  RosettaLexer.FILTER,                  "FILTER"),
                Arguments.of("reduce",                  RosettaLexer.REDUCE,                  "REDUCE"),
                Arguments.of("sort",                    RosettaLexer.SORT,                    "SORT"),
                Arguments.of("first",                   RosettaLexer.FIRST,                   "FIRST"),
                Arguments.of("last",                    RosettaLexer.LAST,                    "LAST"),
                Arguments.of("flatten",                 RosettaLexer.FLATTEN,                 "FLATTEN"),
                Arguments.of("distinct",                RosettaLexer.DISTINCT,                "DISTINCT"),
                Arguments.of("reverse",                 RosettaLexer.REVERSE,                 "REVERSE"),
                Arguments.of("sum",                     RosettaLexer.SUM,                     "SUM"),
                Arguments.of("min",                     RosettaLexer.MIN,                     "MIN"),
                Arguments.of("max",                     RosettaLexer.MAX,                     "MAX"),
                Arguments.of("count",                   RosettaLexer.COUNT,                   "COUNT"),

                // --- Conversion operations (all hyphenated) ---
                Arguments.of("to-zoned-date-time",      RosettaLexer.TO_ZONED_DATE_TIME,      "TO_ZONED_DATE_TIME"),
                Arguments.of("to-date-time",            RosettaLexer.TO_DATE_TIME,            "TO_DATE_TIME"),
                Arguments.of("to-string",               RosettaLexer.TO_STRING,               "TO_STRING"),
                Arguments.of("to-number",               RosettaLexer.TO_NUMBER,               "TO_NUMBER"),
                Arguments.of("to-int",                  RosettaLexer.TO_INT,                  "TO_INT"),
                Arguments.of("to-time",                 RosettaLexer.TO_TIME,                 "TO_TIME"),
                Arguments.of("to-enum",                 RosettaLexer.TO_ENUM,                 "TO_ENUM"),
                Arguments.of("to-date",                 RosettaLexer.TO_DATE,                 "TO_DATE"),

                // --- Special (hyphenated first) ---
                Arguments.of("as-key",                  RosettaLexer.AS_KEY,                  "AS_KEY"),
                Arguments.of("with-meta",               RosettaLexer.WITH_META,               "WITH_META"),
                Arguments.of("super",                   RosettaLexer.SUPER,                   "SUPER"),
                Arguments.of("item",                    RosettaLexer.ITEM,                    "ITEM"),
                Arguments.of("join",                    RosettaLexer.JOIN,                    "JOIN"),
                Arguments.of("True",                    RosettaLexer.TRUE,                    "TRUE"),
                Arguments.of("False",                   RosettaLexer.FALSE,                   "FALSE"),

                // --- Synonyms ---
                Arguments.of("synonym",                 RosettaLexer.SYNONYM,                 "SYNONYM"),
                Arguments.of("source",                  RosettaLexer.SOURCE,                  "SOURCE"),
                Arguments.of("value",                   RosettaLexer.VALUE,                   "VALUE"),
                Arguments.of("hint",                    RosettaLexer.HINT,                    "HINT"),
                Arguments.of("merge",                   RosettaLexer.MERGE,                   "MERGE"),
                Arguments.of("mapper",                  RosettaLexer.MAPPER,                  "MAPPER"),
                Arguments.of("meta",                    RosettaLexer.META,                    "META"),
                Arguments.of("removeHtml",              RosettaLexer.REMOVE_HTML,             "REMOVE_HTML"),
                Arguments.of("dateFormat",              RosettaLexer.DATE_FORMAT,             "DATE_FORMAT"),
                Arguments.of("pattern",                 RosettaLexer.PATTERN,                 "PATTERN"),
                Arguments.of("path",                    RosettaLexer.PATH,                    "PATH"),
                Arguments.of("maps",                    RosettaLexer.MAPS,                    "MAPS"),
                Arguments.of("tag",                     RosettaLexer.TAG,                     "TAG"),
                Arguments.of("componentID",             RosettaLexer.COMPONENT_ID,            "COMPONENT_ID"),
                Arguments.of("definition",              RosettaLexer.DEFINITION,              "DEFINITION"),
                Arguments.of("enums",                   RosettaLexer.ENUMS,                   "ENUMS"),

                // --- Mapping (hyphenated) ---
                Arguments.of("condition-func",          RosettaLexer.CONDITION_FUNC,          "CONDITION_FUNC"),
                Arguments.of("condition-path",          RosettaLexer.CONDITION_PATH,          "CONDITION_PATH"),
                Arguments.of("rosettaPath",             RosettaLexer.ROSETTA_PATH,            "ROSETTA_PATH"),

                // --- Regulatory / Reporting ---
                Arguments.of("body",                    RosettaLexer.BODY,                    "BODY"),
                Arguments.of("corpus",                  RosettaLexer.CORPUS,                  "CORPUS"),
                Arguments.of("segment",                 RosettaLexer.SEGMENT,                 "SEGMENT"),
                Arguments.of("report",                  RosettaLexer.REPORT,                  "REPORT"),
                Arguments.of("rule",                    RosettaLexer.RULE,                    "RULE"),
                Arguments.of("eligibility",             RosettaLexer.ELIGIBILITY,             "ELIGIBILITY"),
                Arguments.of("reporting",               RosettaLexer.REPORTING,               "REPORTING"),
                Arguments.of("regulatoryReference",     RosettaLexer.REGULATORY_REFERENCE,    "REGULATORY_REFERENCE"),
                Arguments.of("docReference",            RosettaLexer.DOC_REFERENCE,           "DOC_REFERENCE"),
                Arguments.of("for",                     RosettaLexer.FOR,                     "FOR"),
                Arguments.of("rationale",               RosettaLexer.RATIONALE,               "RATIONALE"),
                Arguments.of("rationale_author",        RosettaLexer.RATIONALE_AUTHOR,        "RATIONALE_AUTHOR"),
                Arguments.of("structured_provision",    RosettaLexer.STRUCTURED_PROVISION,    "STRUCTURED_PROVISION"),
                Arguments.of("provision",               RosettaLexer.PROVISION,               "PROVISION"),
                Arguments.of("reportedField",           RosettaLexer.REPORTED_FIELD,          "REPORTED_FIELD"),
                Arguments.of("ruleReference",           RosettaLexer.RULE_REFERENCE,          "RULE_REFERENCE"),
                Arguments.of("label",                   RosettaLexer.LABEL,                   "LABEL"),
                Arguments.of("in",                      RosettaLexer.IN,                      "IN"),
                Arguments.of("using",                   RosettaLexer.USING,                   "USING"),
                Arguments.of("standard",                RosettaLexer.STANDARD,                "STANDARD"),
                Arguments.of("with",                    RosettaLexer.WITH,                    "WITH"),
                Arguments.of("root",                    RosettaLexer.ROOT,                    "ROOT"),

                // --- Timing (report) ---
                Arguments.of("real-time",               RosettaLexer.REAL_TIME,               "REAL_TIME"),
                Arguments.of("T+1",                     RosettaLexer.T_PLUS_1,                "T_PLUS_1"),
                Arguments.of("T+2",                     RosettaLexer.T_PLUS_2,                "T_PLUS_2"),
                Arguments.of("T+3",                     RosettaLexer.T_PLUS_3,                "T_PLUS_3"),
                Arguments.of("T+4",                     RosettaLexer.T_PLUS_4,                "T_PLUS_4"),
                Arguments.of("T+5",                     RosettaLexer.T_PLUS_5,                "T_PLUS_5"),
                Arguments.of("ASATP",                   RosettaLexer.ASATP,                   "ASATP"),

                // --- Qualifiable ---
                Arguments.of("isEvent",                 RosettaLexer.IS_EVENT,                "IS_EVENT"),
                Arguments.of("isProduct",               RosettaLexer.IS_PRODUCT,              "IS_PRODUCT")
        );
    }

    @ParameterizedTest(name = "[{index}] {2} <- \"{0}\"")
    @MethodSource("allKeywords")
    void keywordMatchesExpectedToken(String keyword, int expectedTokenType, String tokenName) {
        assertSingleToken(keyword, expectedTokenType, tokenName);
    }

    // ========================================================================
    // 2. Parameterized: ALL punctuation / operator tokens
    // ========================================================================

    static Stream<Arguments> allPunctuation() {
        return Stream.of(
                // Multi-character operators (must appear before single-char in grammar)
                Arguments.of("->>",  RosettaLexer.DEEP_ARROW, "DEEP_ARROW"),
                Arguments.of("->",   RosettaLexer.ARROW,      "ARROW"),
                Arguments.of("..",   RosettaLexer.DOT_DOT,    "DOT_DOT"),
                Arguments.of("<>",   RosettaLexer.NEQ,        "NEQ"),
                Arguments.of("<=",   RosettaLexer.LTE,        "LTE"),
                Arguments.of(">=",   RosettaLexer.GTE,        "GTE"),

                // Single-character punctuation
                Arguments.of("(",    RosettaLexer.LPAREN,     "LPAREN"),
                Arguments.of(")",    RosettaLexer.RPAREN,     "RPAREN"),
                Arguments.of("{",    RosettaLexer.LBRACE,     "LBRACE"),
                Arguments.of("}",    RosettaLexer.RBRACE,     "RBRACE"),
                Arguments.of("[",    RosettaLexer.LBRACK,     "LBRACK"),
                Arguments.of("]",    RosettaLexer.RBRACK,     "RBRACK"),
                Arguments.of(".",    RosettaLexer.DOT,        "DOT"),
                Arguments.of(",",    RosettaLexer.COMMA,      "COMMA"),
                Arguments.of(":",    RosettaLexer.COLON,      "COLON"),
                Arguments.of(";",    RosettaLexer.SEMI,       "SEMI"),
                Arguments.of("*",    RosettaLexer.STAR,       "STAR"),
                Arguments.of("+",    RosettaLexer.PLUS,       "PLUS"),
                Arguments.of("-",    RosettaLexer.MINUS,      "MINUS"),
                Arguments.of("/",    RosettaLexer.SLASH,      "SLASH"),
                Arguments.of("=",    RosettaLexer.EQ,         "EQ"),
                Arguments.of("<",    RosettaLexer.LT,         "LT"),
                Arguments.of(">",    RosettaLexer.GT,         "GT")
        );
    }

    @ParameterizedTest(name = "[{index}] {2} <- \"{0}\"")
    @MethodSource("allPunctuation")
    void punctuationMatchesExpectedToken(String input, int expectedTokenType, String tokenName) {
        assertSingleToken(input, expectedTokenType, tokenName);
    }

    // ========================================================================
    // 3. String escape tests
    // ========================================================================

    @Test
    void stringWithEscapedDoubleQuotes() {
        // "hello \"world\"" should be a single STRING token
        CommonTokenStream tokens = lex("\"hello \\\"world\\\"\"");
        List<Token> real = onChannel(tokens);
        assertEquals(1, real.size());
        assertEquals(RosettaLexer.STRING, real.get(0).getType());
    }

    @Test
    void stringWithEscapedSingleQuotes() {
        // 'it\'s' should be a single STRING token
        CommonTokenStream tokens = lex("'it\\'s'");
        List<Token> real = onChannel(tokens);
        assertEquals(1, real.size());
        assertEquals(RosettaLexer.STRING, real.get(0).getType());
    }

    @Test
    void stringWithNewlineEscape() {
        // "line1\nline2" should be a single STRING token
        CommonTokenStream tokens = lex("\"line1\\nline2\"");
        List<Token> real = onChannel(tokens);
        assertEquals(1, real.size());
        assertEquals(RosettaLexer.STRING, real.get(0).getType());
    }

    @Test
    void stringWithTabEscape() {
        // "tab\there" should be a single STRING token
        CommonTokenStream tokens = lex("\"tab\\there\"");
        List<Token> real = onChannel(tokens);
        assertEquals(1, real.size());
        assertEquals(RosettaLexer.STRING, real.get(0).getType());
    }

    // ========================================================================
    // 4. Error / negative tests
    // ========================================================================

    @Test
    void unterminatedStringProducesError() {
        LexResult result = lexWithErrors("\"unterminated");
        assertFalse(result.errors().isEmpty(),
                "Unterminated string should produce at least one lexer error");
    }

    @Test
    void trailingDotIsNotBigDecimal() {
        // "3." should lex as INT_LITERAL DOT (two tokens), NOT BIG_DECIMAL.
        // This preserves the "1..5" range syntax (INT DOT_DOT INT).
        CommonTokenStream tokens = lex("3.");
        List<Token> real = onChannel(tokens);
        assertEquals(2, real.size());
        assertEquals(RosettaLexer.INT_LITERAL, real.get(0).getType());
        assertEquals(RosettaLexer.DOT, real.get(1).getType());
    }

    @Test
    void illegalCharacterProducesError() {
        // '@' and '#' and '$' are not valid Rosetta tokens
        LexResult result = lexWithErrors("@#$");
        assertFalse(result.errors().isEmpty(),
                "Illegal characters should produce lexer errors");
    }

    // ========================================================================
    // 5. ValidID keywords — these are keywords but also allowed as IDs by parser
    // ========================================================================

    @Test
    void validIdKeywordsAreKeywordTokens() {
        // These 6 keywords can also be identifiers in certain parser contexts via the validID rule.
        // The lexer should always produce their KEYWORD token; the parser handles the ambiguity.
        CommonTokenStream tokens = lex("condition source value version pattern scope");
        List<Token> real = onChannel(tokens);
        assertEquals(6, real.size());
        assertEquals(RosettaLexer.CONDITION, real.get(0).getType());
        assertEquals(RosettaLexer.SOURCE, real.get(1).getType());
        assertEquals(RosettaLexer.VALUE, real.get(2).getType());
        assertEquals(RosettaLexer.VERSION, real.get(3).getType());
        assertEquals(RosettaLexer.PATTERN, real.get(4).getType());
        assertEquals(RosettaLexer.SCOPE, real.get(5).getType());
    }

    // ========================================================================
    // 6. Edge case tests
    // ========================================================================

    @Test
    void caretEscapedKeywordIsId() {
        // ^type should be ID, not TYPE
        CommonTokenStream tokens = lex("^type");
        List<Token> real = onChannel(tokens);
        assertEquals(1, real.size());
        assertEquals(RosettaLexer.ID, real.get(0).getType());
        assertEquals("^type", real.get(0).getText());

        // ^namespace should be ID, not NAMESPACE
        tokens = lex("^namespace");
        real = onChannel(tokens);
        assertEquals(1, real.size());
        assertEquals(RosettaLexer.ID, real.get(0).getType());
        assertEquals("^namespace", real.get(0).getText());
    }

    @Test
    void keywordPrefixIsId() {
        // "namespace123" should be ID, not NAMESPACE followed by INT_LITERAL
        CommonTokenStream tokens = lex("namespace123");
        List<Token> real = onChannel(tokens);
        assertEquals(1, real.size());
        assertEquals(RosettaLexer.ID, real.get(0).getType());

        // "typeAlias" should be the TYPE_ALIAS keyword, not TYPE followed by ID
        tokens = lex("typeAlias");
        real = onChannel(tokens);
        assertEquals(1, real.size());
        assertEquals(RosettaLexer.TYPE_ALIAS, real.get(0).getType());
    }

    @Test
    void adjacentTokensDisambiguate() {
        // "1..5" -> INT_LITERAL DOT_DOT INT_LITERAL (not INT DOT DOT INT)
        CommonTokenStream tokens = lex("1..5");
        List<Token> real = onChannel(tokens);
        assertEquals(3, real.size());
        assertEquals(RosettaLexer.INT_LITERAL, real.get(0).getType());
        assertEquals(RosettaLexer.DOT_DOT, real.get(1).getType());
        assertEquals(RosettaLexer.INT_LITERAL, real.get(2).getType());

        // "a->b" -> ID ARROW ID
        tokens = lex("a->b");
        real = onChannel(tokens);
        assertEquals(3, real.size());
        assertEquals(RosettaLexer.ID, real.get(0).getType());
        assertEquals(RosettaLexer.ARROW, real.get(1).getType());
        assertEquals(RosettaLexer.ID, real.get(2).getType());

        // "a->>b" -> ID DEEP_ARROW ID
        tokens = lex("a->>b");
        real = onChannel(tokens);
        assertEquals(3, real.size());
        assertEquals(RosettaLexer.ID, real.get(0).getType());
        assertEquals(RosettaLexer.DEEP_ARROW, real.get(1).getType());
        assertEquals(RosettaLexer.ID, real.get(2).getType());

        // "x<>y" -> ID NEQ ID
        tokens = lex("x<>y");
        real = onChannel(tokens);
        assertEquals(3, real.size());
        assertEquals(RosettaLexer.ID, real.get(0).getType());
        assertEquals(RosettaLexer.NEQ, real.get(1).getType());
        assertEquals(RosettaLexer.ID, real.get(2).getType());
    }

    @Test
    void hyphenatedKeywordWithSpacesDisambiguates() {
        // "to - date" should lex as TO MINUS ID, not TO_DATE.
        // Documented in the grammar header: hyphenated keywords require no spaces.
        CommonTokenStream tokens = lex("to - date");
        List<Token> real = onChannel(tokens);
        assertEquals(3, real.size());
        assertEquals(RosettaLexer.TO, real.get(0).getType());
        assertEquals(RosettaLexer.MINUS, real.get(1).getType());
        assertEquals(RosettaLexer.ID, real.get(2).getType());
    }

    @Test
    void tPlusWithSpacesDisambiguates() {
        // "T + 1" should lex as ID PLUS INT_LITERAL, not T_PLUS_1.
        // Documented in the grammar header: T+N tokens require no spaces.
        CommonTokenStream tokens = lex("T + 1");
        List<Token> real = onChannel(tokens);
        assertEquals(3, real.size());
        assertEquals(RosettaLexer.ID, real.get(0).getType());
        assertEquals(RosettaLexer.PLUS, real.get(1).getType());
        assertEquals(RosettaLexer.INT_LITERAL, real.get(2).getType());
    }

    @Test
    void numberEdgeCases() {
        // "0" -> INT_LITERAL
        assertSingleToken("0", RosettaLexer.INT_LITERAL, "INT_LITERAL");

        // ".5" -> BIG_DECIMAL
        assertSingleToken(".5", RosettaLexer.BIG_DECIMAL, "BIG_DECIMAL");

        // "3.14" -> BIG_DECIMAL
        assertSingleToken("3.14", RosettaLexer.BIG_DECIMAL, "BIG_DECIMAL");

        // "1e5" -> BIG_DECIMAL
        assertSingleToken("1e5", RosettaLexer.BIG_DECIMAL, "BIG_DECIMAL");

        // "3.14e-2" -> BIG_DECIMAL
        assertSingleToken("3.14e-2", RosettaLexer.BIG_DECIMAL, "BIG_DECIMAL");

        // "42" -> INT_LITERAL
        assertSingleToken("42", RosettaLexer.INT_LITERAL, "INT_LITERAL");

        // "3.e4" -> BIG_DECIMAL (trailing-dot with required exponent)
        assertSingleToken("3.e4", RosettaLexer.BIG_DECIMAL, "BIG_DECIMAL");

        // ".5e2" -> BIG_DECIMAL (leading-dot with exponent)
        assertSingleToken(".5e2", RosettaLexer.BIG_DECIMAL, "BIG_DECIMAL");

        // "3.14E10" -> BIG_DECIMAL (uppercase E)
        assertSingleToken("3.14E10", RosettaLexer.BIG_DECIMAL, "BIG_DECIMAL");
    }

    // ========================================================================
    // 7. Literal and identifier token coverage
    // ========================================================================

    @Test
    void doubleQuotedStringIsStringToken() {
        assertSingleToken("\"hello world\"", RosettaLexer.STRING, "STRING");
    }

    @Test
    void singleQuotedStringIsStringToken() {
        assertSingleToken("'hello world'", RosettaLexer.STRING, "STRING");
    }

    @Test
    void emptyDoubleQuotedStringIsStringToken() {
        assertSingleToken("\"\"", RosettaLexer.STRING, "STRING");
    }

    @Test
    void emptySingleQuotedStringIsStringToken() {
        assertSingleToken("''", RosettaLexer.STRING, "STRING");
    }

    @Test
    void identifierToken() {
        assertSingleToken("myIdentifier", RosettaLexer.ID, "ID");
    }

    @Test
    void caretPrefixedIdentifier() {
        assertSingleToken("^escaped", RosettaLexer.ID, "ID");
    }

    @Test
    void underscoreIdentifier() {
        assertSingleToken("_private", RosettaLexer.ID, "ID");
    }

    // ========================================================================
    // 8. Hidden channel token coverage (WS, LINE_COMMENT, BLOCK_COMMENT)
    // ========================================================================

    @Test
    void whitespaceOnHiddenChannel() {
        CommonTokenStream tokens = lex("  \t\n  ");
        // Should produce no on-channel tokens
        List<Token> real = onChannel(tokens);
        assertEquals(0, real.size());

        // But HIDDEN channel should have the WS token
        List<Token> allTokens = tokens.getTokens().stream()
                .filter(t -> t.getType() != Token.EOF)
                .toList();
        assertEquals(1, allTokens.size());
        assertEquals(RosettaLexer.WS, allTokens.get(0).getType());
    }

    @Test
    void lineCommentOnHiddenChannel() {
        CommonTokenStream tokens = lex("// this is a comment");
        List<Token> real = onChannel(tokens);
        assertEquals(0, real.size());

        List<Token> allTokens = tokens.getTokens().stream()
                .filter(t -> t.getType() != Token.EOF)
                .toList();
        assertEquals(1, allTokens.size());
        assertEquals(RosettaLexer.LINE_COMMENT, allTokens.get(0).getType());
    }

    @Test
    void blockCommentOnHiddenChannel() {
        CommonTokenStream tokens = lex("/* block comment */");
        List<Token> real = onChannel(tokens);
        assertEquals(0, real.size());

        List<Token> allTokens = tokens.getTokens().stream()
                .filter(t -> t.getType() != Token.EOF)
                .toList();
        assertEquals(1, allTokens.size());
        assertEquals(RosettaLexer.BLOCK_COMMENT, allTokens.get(0).getType());
    }

    @Test
    void multiLineBlockCommentOnHiddenChannel() {
        CommonTokenStream tokens = lex("/* line1\nline2 */");
        List<Token> real = onChannel(tokens);
        assertEquals(0, real.size());

        List<Token> allTokens = tokens.getTokens().stream()
                .filter(t -> t.getType() != Token.EOF)
                .toList();
        assertEquals(1, allTokens.size());
        assertEquals(RosettaLexer.BLOCK_COMMENT, allTokens.get(0).getType());
    }
}
