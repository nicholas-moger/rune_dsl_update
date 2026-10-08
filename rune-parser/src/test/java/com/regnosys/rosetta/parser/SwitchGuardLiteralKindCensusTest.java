package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.ast.builder.AstBuilderHelper;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * v3.1 C2d, retirement family 1 ({@code numeric-literal-kind}) — THE CORPUS WITNESS: every
 * {@code switchCaseGuard} of every {@code .rosetta} file in the test corpus, classified on the
 * ANTLR PARSE TREE (the terminal the lexer produced — {@code INT_LITERAL} / {@code BIG_DECIMAL} /
 * {@code STRING} / {@code TRUE} / {@code FALSE} — or a {@code qualifiedName}), never by scanning
 * text. A text scan is not a census: the PR #608 triage notes had counted twenty
 * {@code 0 then FormatToShortFraction5DecimalNumber} REPORTING-RULE BODIES as "numeric {@code 0}
 * guards"; the close-out review re-took the figures on the parse tree and found NO numeric literal
 * guard in the corpus at all. This test commits that census (the instrument was
 * {@code target/triage-instruments/census/SwitchGuardCensus.java}, local to PR #608) and re-takes
 * the figures live at every run.
 *
 * <p>For STRING guards the value the front-end stores ({@link AstBuilderHelper#stripQuotes} — what
 * {@code RSwitchCaseGuard.literalValue()} holds) is then tested against the four value shapes the
 * generator's switch-guard render ladder ({@code ControlFlowHandler.renderGuard}) classified on the
 * rendered text before this family retired them, so the figures ARE the reachability of its
 * Long / Float / Double / Integer arms on this corpus:
 * <ul>
 *   <li>no {@code INT_LITERAL} and no {@code BIG_DECIMAL} guard exists — the numeric arms have no
 *       numeric-literal carrier;</li>
 *   <li>no STRING guard is long-, float-, decimal- or exponent-shaped — the Long / Float / Double
 *       arms could not fire even on the mis-typed (quote-stripped) STRING channel, which is why
 *       they were deleted as DEAD (the grammar admits no {@code l/L/f/F} suffix on a numeric
 *       literal either: {@code INT_LITERAL : DIGIT+}, {@code BIG_DECIMAL} has no suffix);</li>
 *   <li>240 STRING guards ARE int-shaped ({@code "1"}, {@code "2"}, …) — the only corpus population
 *       the Integer arm could ever have mis-typed; whether any reaches the render seat is the
 *       family's C2c census (a runtime probe), not this test's question.</li>
 * </ul>
 *
 * <p>The regexes below run on TOKEN VALUES (the quote-stripped string a guard carries), mirroring
 * the retired arms' own value tests one for one — they are the witness's definition of "shaped",
 * not a structural read of language content (the structure is read from the parse tree).
 *
 * <p>Corpus-gated: skips with a recorded reason when {@code ../test-corpus} is absent (a fresh
 * clone, CI); {@code -Dcorpus.required=true} — the local receipts chain's setting — turns that skip
 * into a failure, so a local green can never rest on a skipped witness (the #606
 * {@code Drr7Corpus} contract). The file count is the manifest's 4,137 {@code .rosetta}
 * ({@code Corpus983BaselineManifestTest} pins the same population), so a corpus change moves both
 * pins deliberately, together.
 */
class SwitchGuardLiteralKindCensusTest {

    private static final Path CORPUS_DIR = Path.of("../test-corpus");

    /** {@code -Dcorpus.required=true} turns an absent-corpus SKIP into a FAILURE. */
    static final String REQUIRED_PROPERTY = "corpus.required";

    // THE PINS — the PR #608 close-out review's parse-tree census, re-taken live (74 s, one JVM).
    static final int FILES = 4_137;
    static final int FILES_WITH_SYNTAX_ERRORS = 0;
    static final int GUARDS = 19_829;
    static final int NAME_GUARDS = 2_386;
    static final int INT_LITERAL_GUARDS = 0;
    static final int BIG_DECIMAL_GUARDS = 0;
    static final int BOOLEAN_GUARDS = 16;
    static final int STRING_GUARDS = 17_427;
    static final int STRING_INT_SHAPED = 240;
    static final int STRING_LONG_SHAPED = 0;
    static final int STRING_FLOAT_SHAPED = 0;
    static final int STRING_DOUBLE_ARM_SHAPED = 0;

    // The retired arms' value tests, verbatim (ControlFlowHandler.renderGuard before PR #609).
    private static final Pattern INT_SHAPE = Pattern.compile("[+-]?\\d+");
    private static final Pattern LONG_SHAPE = Pattern.compile("[+-]?\\d+[lL]");
    private static final Pattern FLOAT_SHAPE = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)[fF]");
    private static final Pattern DOUBLE_SHAPE =
            Pattern.compile("[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)(?:[eE][+-]?\\d+)?[dD]?");
    private static final Pattern EXPONENT_OR_D = Pattern.compile(".*[eEdD].*");

    /** The census figures — one counter per parse-tree class, one per STRING value shape. */
    static final class Census {
        int files;
        int filesWithSyntaxErrors;
        int guards;
        int nameGuards;
        int intLiteralGuards;
        int bigDecimalGuards;
        int booleanGuards;
        int stringGuards;
        int stringIntShaped;
        int stringLongShaped;
        int stringFloatShaped;
        int stringDoubleArmShaped;
        final TreeMap<String, Integer> intLiteralTexts = new TreeMap<>();
        final TreeMap<String, Integer> bigDecimalTexts = new TreeMap<>();
        final List<String> doubleArmValues = new ArrayList<>();
        final List<String> syntaxErrors = new ArrayList<>();

        void classify(RosettaParser.SwitchCaseGuardContext ctx) {
            guards++;
            RosettaParser.LiteralContext lit = ctx.literal();
            if (lit == null) {
                nameGuards++;
                return;
            }
            if (lit.INT_LITERAL() != null) {
                intLiteralGuards++;
                intLiteralTexts.merge(lit.getText(), 1, Integer::sum);
                return;
            }
            if (lit.BIG_DECIMAL() != null) {
                bigDecimalGuards++;
                bigDecimalTexts.merge(lit.getText(), 1, Integer::sum);
                return;
            }
            if (lit.TRUE() != null || lit.FALSE() != null) {
                booleanGuards++;
                return;
            }
            if (lit.STRING() != null) {
                stringGuards++;
                String value = AstBuilderHelper.stripQuotes(lit.STRING().getText());
                if (INT_SHAPE.matcher(value).matches()) {
                    stringIntShaped++;
                }
                if (LONG_SHAPE.matcher(value).matches()) {
                    stringLongShaped++;
                }
                if (FLOAT_SHAPE.matcher(value).matches()) {
                    stringFloatShaped++;
                }
                if (DOUBLE_SHAPE.matcher(value).matches()
                        && (value.contains(".") || EXPONENT_OR_D.matcher(value).matches())) {
                    stringDoubleArmShaped++;
                    doubleArmValues.add(value);
                }
                return;
            }
            fail("switchCaseGuard literal of an unknown terminal kind at " + ctx.getText());
        }

        String table() {
            return "files parsed          : " + files + " (files with syntax errors: " + filesWithSyntaxErrors
                    + (syntaxErrors.isEmpty() ? "" : " " + syntaxErrors) + ")\n"
                    + "switch case guards    : " + guards + "\n"
                    + "  qualifiedName guards: " + nameGuards + "\n"
                    + "  INT_LITERAL guards  : " + intLiteralGuards + " texts=" + intLiteralTexts + "\n"
                    + "  BIG_DECIMAL guards  : " + bigDecimalGuards + " texts=" + bigDecimalTexts + "\n"
                    + "  boolean guards      : " + booleanGuards + "\n"
                    + "  STRING guards       : " + stringGuards + "\n"
                    + "    int-shaped [+-]?\\d+                 : " + stringIntShaped + "\n"
                    + "    long-shaped [+-]?\\d+[lL]            : " + stringLongShaped + "\n"
                    + "    float-shaped ...[fF]                : " + stringFloatShaped + "\n"
                    + "    Double-arm reachable (. or eEdD)    : " + stringDoubleArmShaped + " " + doubleArmValues + "\n";
        }
    }

    static boolean corpusExists() {
        if (!Files.isDirectory(CORPUS_DIR)) {
            return false;
        }
        try (var walk = Files.walk(CORPUS_DIR, 10)) {
            return walk.anyMatch(p -> p.toString().endsWith(".rosetta"));
        } catch (IOException e) {
            return false;
        }
    }

    static Census take(List<Path> files) {
        Census c = new Census();
        RosettaParserBaseListener listener = new RosettaParserBaseListener() {
            @Override
            public void exitSwitchCaseGuard(RosettaParser.SwitchCaseGuardContext ctx) {
                c.classify(ctx);
            }
        };
        for (Path file : files) {
            c.files++;
            RosettaParseResult result = RosettaParserFacade.parseFile(file);
            if (!result.errors().isEmpty()) {
                c.filesWithSyntaxErrors++;
                if (c.syntaxErrors.size() < 5) {
                    c.syntaxErrors.add(file.getFileName() + ": " + result.errors().get(0));
                }
            }
            ParseTreeWalker.DEFAULT.walk(listener, result.tree());
        }
        return c;
    }

    @Test
    void every_switch_guard_in_the_corpus_classified_on_the_parse_tree() throws IOException {
        if (!corpusExists()) {
            String reason = "[CORPUS ABSENT] " + getClass().getSimpleName() + ": " + CORPUS_DIR
                    + " holds no .rosetta file (acquire it as docs/CORPUS-9.83.md describes)";
            if (Boolean.getBoolean(REQUIRED_PROPERTY)) {
                fail(reason + " and -D" + REQUIRED_PROPERTY + "=true forbids skipping the witness");
            }
            assumeTrue(false, reason);
        }
        List<Path> files = new ArrayList<>();
        try (var walk = Files.walk(CORPUS_DIR)) {
            walk.filter(p -> p.toString().endsWith(".rosetta"))
                    // The chaos cell (in test-corpus/ since v3.2 PR-2) is EXCLUDED from this
                    // witness's population: it DELIBERATELY writes the guard shapes this
                    // census proves the real corpus never does (s18's INT_LITERAL and
                    // int-shaped STRING guards). The witness pins UPSTREAM reality — the
                    // manifest's vendored population — and the chaos cell's guards are the
                    // D11/chaos gates' business, not this retirement witness's.
                    .filter(p -> !p.toString().replace('\\', '/').contains("/test-corpus/chaos/"))
                    .sorted().forEach(files::add);
        }
        long t0 = System.nanoTime();
        Census c = take(files);
        System.out.println("[" + getClass().getSimpleName() + "] the switch-guard literal-kind census over "
                + CORPUS_DIR + " (" + ((System.nanoTime() - t0) / 1_000_000_000L) + " s):\n" + c.table());
        assertAll("the parse-tree census of every switchCaseGuard in the corpus\n" + c.table(),
                () -> assertEquals(FILES, c.files, ".rosetta files walked (the manifest's population)"),
                () -> assertEquals(FILES_WITH_SYNTAX_ERRORS, c.filesWithSyntaxErrors, "files with syntax errors"),
                () -> assertEquals(GUARDS, c.guards, "switch case guards"),
                () -> assertEquals(NAME_GUARDS, c.nameGuards, "qualifiedName guards"),
                () -> assertEquals(INT_LITERAL_GUARDS, c.intLiteralGuards, "INT_LITERAL guards " + c.intLiteralTexts),
                () -> assertEquals(BIG_DECIMAL_GUARDS, c.bigDecimalGuards, "BIG_DECIMAL guards " + c.bigDecimalTexts),
                () -> assertEquals(BOOLEAN_GUARDS, c.booleanGuards, "True/False guards"),
                () -> assertEquals(STRING_GUARDS, c.stringGuards, "STRING guards"),
                () -> assertEquals(STRING_INT_SHAPED, c.stringIntShaped, "int-shaped STRING guards (the Integer arm's only possible corpus arrivals)"),
                () -> assertEquals(STRING_LONG_SHAPED, c.stringLongShaped, "long-shaped STRING guards (the retired Long arm)"),
                () -> assertEquals(STRING_FLOAT_SHAPED, c.stringFloatShaped, "float-shaped STRING guards (the retired Float arm)"),
                () -> assertEquals(STRING_DOUBLE_ARM_SHAPED, c.stringDoubleArmShaped,
                        "decimal/exponent-shaped STRING guards (the retired Double arm) " + c.doubleArmValues),
                () -> assertEquals(c.guards, c.nameGuards + c.intLiteralGuards + c.bigDecimalGuards + c.booleanGuards + c.stringGuards,
                        "every guard is exactly one of the five parse-tree classes"));
    }

    /**
     * The positive control (LAW 60 / "prove the instrument can fail"): a model carrying one guard of
     * every terminal kind, plus the three suffix / decimal shapes the corpus never produces, must be
     * classified into exactly those counters — the walker, the terminal tests and the value shapes
     * are exercised on a known input before their zero readings on the corpus are believed.
     */
    @Test
    void the_census_classifies_every_guard_kind_on_a_known_model() throws IOException {
        String model = String.join("\n",
                "namespace census.test",
                "version \"0.0.0\"",
                "",
                "enum Colour:",
                "    RED",
                "    BLUE",
                "",
                "func Classify:",
                "    inputs:",
                "        s string (1..1)",
                "        n int (1..1)",
                "        d number (1..1)",
                "        b boolean (1..1)",
                "        c Colour (1..1)",
                "    output:",
                "        r string (1..1)",
                "    condition IntGuards:",
                "        n switch 42 then True, 7 then False, default True",
                "    condition DecimalGuards:",
                "        d switch 3.14 then True, default False",
                "    condition BooleanGuards:",
                "        b switch True then True, False then False",
                "    condition NameGuards:",
                "        c switch RED then True, BLUE then False",
                "    set r:",
                "        s switch",
                "            \"one\" then \"a\",",
                "            \"12\" then \"b\",",
                "            \"+3\" then \"c\",",
                "            \"4L\" then \"d\",",
                "            \"1.5f\" then \"e\",",
                "            \"2.5\" then \"f\",",
                "            \"3e2\" then \"g\",",
                "            \"7d\" then \"h\",",
                "            default \"z\"");
        Path tmp = Files.createTempFile("switch-guard-census-control", ".rosetta");
        try {
            Files.writeString(tmp, model);
            Census c = take(List.of(tmp));
            assertAll("the positive control\n" + c.table(),
                    () -> assertEquals(1, c.files),
                    () -> assertEquals(0, c.filesWithSyntaxErrors, "the control model must parse clean"),
                    () -> assertEquals(15, c.guards, "guards"),
                    () -> assertEquals(2, c.nameGuards, "RED + BLUE"),
                    () -> assertEquals(2, c.intLiteralGuards, "42 + 7 " + c.intLiteralTexts),
                    () -> assertEquals(1, c.bigDecimalGuards, "3.14 " + c.bigDecimalTexts),
                    () -> assertEquals(2, c.booleanGuards, "True + False"),
                    () -> assertEquals(8, c.stringGuards, "the eight quoted guards"),
                    () -> assertEquals(2, c.stringIntShaped, "\"12\" + \"+3\""),
                    () -> assertEquals(1, c.stringLongShaped, "\"4L\""),
                    () -> assertEquals(1, c.stringFloatShaped, "\"1.5f\""),
                    () -> assertEquals(3, c.stringDoubleArmShaped, "\"2.5\" + \"3e2\" + \"7d\" " + c.doubleArmValues));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
