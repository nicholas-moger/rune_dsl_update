package com.regnosys.rosetta.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * v3.2 seat 7 (F11, the type-switch refusals) — THE CORPUS WITNESS of the seat's REACH: every {@code switch} of every
 * vendored {@code .rosetta} file, classified on the ANTLR PARSE TREE by where it sits (the DIRECT body of an inline
 * function — a list-op lambda's or a {@code then} combinator's — nested deeper inside one, or outside any inline
 * function), by the OWNER operator of that inline function, by the corpus CELL, by its guard kind (NAME / LITERAL /
 * mixed) and, for the lambda-direct class, by the shape of every case arm and of the default. A text scan is not a census
 * (the PR #608 law — the seat's own text census had read 53 lambda-direct switches where the parse tree holds 245: the
 * multi-line bodies and the bracket-less {@code extract item switch} form are invisible to a one-line grep); this test
 * commits the figures and re-takes them live at every run.
 *
 * <p>THE SEAT'S SHAPES — a lambda-direct CHOICE/TYPE-keyed switch whose arms are case-narrowed NAVIGATIONS
 * ({@code item -> av}), a to-string over one, or a LITERAL, and whose default is a non-empty LITERAL; a lambda-direct
 * LITERAL-keyed switch — have ZERO vendored carriers on every one of their counters (nav-on-item 0, to-string-over-nav 0,
 * LITERAL-guarded 0, literal default 0): the chaos cell's twelve {@code C18ToKind} rows and the seat's two oracle groups are
 * the only witnesses, which is why the 25 vendored cells' digests are UNMOVED on both routes (the chain's chaos-FILTERED
 * re-digest) BY CONSTRUCTION as well as by measurement. The lambda-direct switches the corpus DOES carry — every one the
 * body of an {@code extract} (241 with a receiver, 4 the bracket-less rule-body {@code extract switch ...}; no {@code then}
 * combinator body, no {@code filter}) — are the classes the block seats served before the seat: bare FUNCTION-NAME arms
 * (the implicit-argument call form {@code fpml.FixedLeg then MapFixedLegToPriceQuantity}, every one of the 92 symbol arms
 * a declared {@code func}), explicit function-call arms, bare-item arms, the enum-keyed literal arms of the mas-rewrite
 * platform rule (the corpus's only lambda-direct switches WITHOUT a default), a to-string over the DISGUISED case-narrowed
 * attribute of the fpml return leg ({@code notionalAdjustments to-string}, the #226 block's own to-string case), and the
 * iosco price rule's {@code default}-operator and {@code min} arms — with an EMPTY default or none, which this census pins
 * as the pre-seat population, the owner and cell histograms and the example sites printed for the reviewer.
 *
 * <p>Corpus-gated exactly like {@link SwitchGuardLiteralKindCensusTest}: skips with a recorded reason when
 * {@code ../test-corpus} is absent; {@code -Dcorpus.required=true} turns the skip into a failure; the chaos cell is
 * EXCLUDED (it deliberately writes the shapes this census proves the real corpus never does). The file count is the
 * manifest's 4,137.
 */
class LambdaSwitchShapeCensusTest {

    private static final Path CORPUS_DIR = Path.of("../test-corpus");
    static final String REQUIRED_PROPERTY = "corpus.required";

    // THE PINS — the seat-7 parse-tree census over the vendored corpus, re-taken live at every run.
    static final int FILES = 4_137;
    static final int FILES_WITH_SYNTAX_ERRORS = 0;
    static final int SWITCHES = 1_388;
    static final int OUTSIDE_LAMBDA = 1_143;
    static final int LAMBDA_DIRECT = 245;
    static final int LAMBDA_NESTED = 0;
    static final String DIRECT_OWNERS = "{ExtractExpr=241, ExtractWithoutLeftExpr=4}";
    static final String DIRECT_CELLS = "{cdm/cdm-6.20.2=29, cdm/cdm-6.20.3=29, cdm/cdm-6.20.4=29, cdm/cdm-6.20.5=29, cdm/cdm-6.20.6=29, "
            + "cdm/cdm-6.21.0=29, cdm/cdm-6.22.0=29, cdm/cdm-6.23.0=29, drr/drr-6.34.1=1, drr/drr-6.35.0=1, drr/drr-6.36.0=1, "
            + "drr/drr-6.37.0=1, drr/drr-6.38.0=1, drr/drr-7.0.0=2, drr/drr-7.1.0=2, drr/drr-7.2.0=2, drr/drr-7.3.0=2}";
    static final int LAMBDA_DIRECT_NAME_GUARDED = 245;
    static final int LAMBDA_DIRECT_LITERAL_GUARDED = 0;
    static final int LAMBDA_DIRECT_MIXED_GUARDED = 0;
    static final int LAMBDA_DIRECT_ARMS = 307;
    static final int LAMBDA_DIRECT_ARMS_ITEM = 48;
    static final int LAMBDA_DIRECT_ARMS_SYMBOL = 92;
    static final int LAMBDA_DIRECT_ARMS_SYMBOL_FUNCTION_NAME = 92;
    static final int LAMBDA_DIRECT_ARMS_SYMBOL_OTHER = 0;
    static final int LAMBDA_DIRECT_ARMS_FUNCTION_CALL = 132;
    static final int LAMBDA_DIRECT_ARMS_LITERAL = 15;
    static final int LAMBDA_DIRECT_ARMS_NAV_ON_ITEM = 0;
    static final int LAMBDA_DIRECT_ARMS_TO_STRING_OVER_NAV = 0;
    static final int LAMBDA_DIRECT_ARMS_TO_STRING_OTHER = 8;
    static final int LAMBDA_DIRECT_ARMS_THEN_CHAIN = 0;
    static final int LAMBDA_DIRECT_ARMS_OTHER = 12;
    static final String OTHER_ARM_KINDS = "{DefaultExpr=8, MinExpr=4}";
    static final int LAMBDA_DIRECT_DEFAULT_ABSENT = 9;
    static final int LAMBDA_DIRECT_DEFAULT_EMPTY = 236;
    static final int LAMBDA_DIRECT_DEFAULT_LITERAL = 0;
    static final int LAMBDA_DIRECT_DEFAULT_OTHER = 0;
    static final String OTHER_DEFAULT_KINDS = "{}";

    /** The census figures — one counter per parse-tree class, with example sites for the reviewer. */
    static final class Census {
        int files;
        int filesWithSyntaxErrors;
        int switches;
        int lambdaDirect;
        int lambdaNested;
        int outsideLambda;
        int directNameGuarded;
        int directLiteralGuarded;
        int directMixedGuarded;
        int directArms;
        int directArmsItem;
        int directArmsSymbol;
        int directArmsSymbolFunctionName;
        int directArmsSymbolOther;
        int directArmsFunctionCall;
        int directArmsLiteral;
        int directArmsNavOnItem;
        int directArmsToStringOverNav;
        int directArmsToStringOther;
        int directArmsThenChain;
        int directArmsOther;
        int directDefaultAbsent;
        int directDefaultEmpty;
        int directDefaultLiteral;
        int directDefaultOther;
        final TreeMap<String, Integer> directOwners = new TreeMap<>();
        final TreeMap<String, Integer> directCells = new TreeMap<>();
        final TreeMap<String, Integer> otherArmKinds = new TreeMap<>();
        final TreeMap<String, Integer> otherDefaultKinds = new TreeMap<>();
        final TreeMap<String, List<String>> examples = new TreeMap<>();
        final TreeSet<String> declaredFunctions = new TreeSet<>();
        /** The bare-symbol arms, classified once every file's {@code func} declarations are known ({@link #finish()}). */
        final List<String[]> symbolArms = new ArrayList<>();
        final List<String> syntaxErrors = new ArrayList<>();
        String currentFile = "?";

        void example(String kind, String site) {
            List<String> l = examples.computeIfAbsent(kind, k -> new ArrayList<>());
            if (l.size() < 3) {
                l.add(site);
            }
        }

        String site(ParserRuleContext ctx) {
            String text = ctx == null ? "" : ctx.getText();
            if (text.length() > 90) {
                text = text.substring(0, 90) + "...";
            }
            return currentFile + ":" + (ctx == null ? "?" : ctx.getStart().getLine()) + " " + text;
        }

        String cell() {
            String[] seg = currentFile.split("/");
            return seg.length >= 3 ? seg[0] + "/" + seg[1] : "(outside the corpus)";
        }

        void classify(ParserRuleContext sw, List<RosettaParser.SwitchCaseOrDefaultContext> cases) {
            switches++;
            String owner = lambdaOwner(sw);
            if (owner == null) {
                outsideLambda++;
                return;
            }
            if (owner.isEmpty()) {
                lambdaNested++;
                example("nested", site(sw));
                return;
            }
            lambdaDirect++;
            directOwners.merge(owner, 1, Integer::sum);
            directCells.merge(cell(), 1, Integer::sum);
            example("owner " + owner, site(sw));
            boolean anyName = false;
            boolean anyLiteral = false;
            boolean sawDefault = false;
            for (RosettaParser.SwitchCaseOrDefaultContext sc : cases) {
                RosettaParser.SwitchCaseGuardContext guard = sc.switchCaseGuard();
                RosettaParser.ExprWithThenContext armChain = sc.exprWithThen();
                RosettaParser.ExpressionContext arm = armChain == null ? null : armChain.expression();
                boolean thenChained = armChain != null && !armChain.thenSuffix().isEmpty();
                if (guard == null) {
                    sawDefault = true;
                    if (!thenChained && (arm == null || arm instanceof RosettaParser.EmptyExprContext)) {
                        directDefaultEmpty++;
                    } else if (!thenChained && arm instanceof RosettaParser.LiteralExprContext) {
                        directDefaultLiteral++;
                        example("default literal", site(arm));
                    } else {
                        directDefaultOther++;
                        String kind = thenChained ? "ThenChain" : kindName(arm);
                        otherDefaultKinds.merge(kind, 1, Integer::sum);
                        example("default other " + kind, site(armChain));
                    }
                    continue;
                }
                if (guard.literal() != null) {
                    anyLiteral = true;
                } else {
                    anyName = true;
                }
                directArms++;
                if (thenChained) {
                    directArmsThenChain++;
                    example("arm then-chain", site(armChain));
                } else if (arm instanceof RosettaParser.ImplicitVarExprContext) {
                    directArmsItem++;
                } else if (arm instanceof RosettaParser.SymbolRefExprContext) {
                    directArmsSymbol++;
                    String name = arm.getText();
                    symbolArms.add(new String[] {name.substring(name.lastIndexOf('.') + 1), site(arm)});
                } else if (arm instanceof RosettaParser.FunctionCallExprContext) {
                    directArmsFunctionCall++;
                } else if (arm instanceof RosettaParser.LiteralExprContext) {
                    directArmsLiteral++;
                    example("arm literal", site(arm));
                } else if (arm instanceof RosettaParser.ToStringExprContext ts) {
                    if (isNavOnItem(ts.expression())) {
                        directArmsToStringOverNav++;
                        example("arm to-string over nav", site(arm));
                    } else {
                        directArmsToStringOther++;
                        example("arm to-string other", site(arm));
                    }
                } else if (isNavOnItem(arm)) {
                    directArmsNavOnItem++;
                    example("arm nav-on-item", site(arm));
                } else {
                    directArmsOther++;
                    String kind = kindName(arm);
                    otherArmKinds.merge(kind, 1, Integer::sum);
                    example("arm other " + kind, site(arm));
                }
            }
            if (!sawDefault) {
                directDefaultAbsent++;
                example("default absent", site(sw));
            }
            if (anyName && anyLiteral) {
                directMixedGuarded++;
            } else if (anyLiteral) {
                directLiteralGuarded++;
                example("literal-guarded", site(sw));
            } else {
                directNameGuarded++;
            }
        }

        /** The deferred symbol-arm split: a bare name that is a declared {@code func} (the implicit-argument call form) or not. */
        void finish() {
            for (String[] a : symbolArms) {
                if (declaredFunctions.contains(a[0])) {
                    directArmsSymbolFunctionName++;
                    example("arm symbol = function name", a[1]);
                } else {
                    directArmsSymbolOther++;
                    example("arm symbol other", a[1]);
                }
            }
        }

        static String kindName(ParserRuleContext ctx) {
            if (ctx == null) {
                return "null";
            }
            String n = ctx.getClass().getSimpleName();
            return n.endsWith("Context") ? n.substring(0, n.length() - "Context".length()) : n;
        }

        /** A feature-call chain whose ROOT receiver is the implicit item ({@code item -> av}, {@code item -> a -> b}). */
        static boolean isNavOnItem(RosettaParser.ExpressionContext arm) {
            ParserRuleContext cur = arm;
            int depth = 0;
            while (cur instanceof RosettaParser.FeatureCallExprContext fc && depth++ < 32 /* the parser module cannot see the generator's PARENT_WALK_LIMIT (64); no corpus chain nears either */) {
                cur = fc.expression();
            }
            return depth > 0 && cur instanceof RosettaParser.ImplicitVarExprContext;
        }

        /**
         * {@code null} = outside any inline function; the OWNER operator's rule name ({@code ThenSuffix},
         * {@code ExtractExpr}, {@code FilterExpr}, ...) when the switch is the DIRECT body of an inline function (a
         * parenthesised wrapper admitted; a {@code then}-chained body whose HEAD is the switch is NOT direct — the
         * generator sees a then chain there, not a switch); {@code ""} = nested deeper inside one.
         */
        static String lambdaOwner(ParserRuleContext sw) {
            ParserRuleContext cur = sw.getParent();
            boolean direct = true;
            int depth = 0;
            while (cur != null && depth++ < 256) {
                if (cur instanceof RosettaParser.ImplicitInlineFunctionContext
                        || cur instanceof RosettaParser.InlineFunctionContext) {
                    return direct ? kindName(cur.getParent()) : "";
                }
                boolean transparent = cur instanceof RosettaParser.ParenExprContext
                        || (cur instanceof RosettaParser.ExprWithThenContext ewt && ewt.thenSuffix().isEmpty());
                if (!transparent) {
                    direct = false;
                }
                cur = cur.getParent();
            }
            return null;
        }

        String table() {
            StringBuilder b = new StringBuilder();
            b.append("files parsed                : ").append(files).append(" (files with syntax errors: ")
                    .append(filesWithSyntaxErrors).append(syntaxErrors.isEmpty() ? "" : " " + syntaxErrors)
                    .append("; declared funcs ").append(declaredFunctions.size()).append(")\n");
            b.append("switches                    : ").append(switches).append('\n');
            b.append("  outside any lambda        : ").append(outsideLambda).append('\n');
            b.append("  lambda-DIRECT             : ").append(lambdaDirect).append(" (NAME-guarded ").append(directNameGuarded)
                    .append(", LITERAL-guarded ").append(directLiteralGuarded).append(", mixed ").append(directMixedGuarded)
                    .append(") by owner ").append(directOwners).append('\n');
            b.append("  lambda-DIRECT by cell     : ").append(directCells).append('\n');
            b.append("  lambda-nested             : ").append(lambdaNested).append(" (incl. a then-chained body whose head is the switch)\n");
            b.append("  lambda-DIRECT arms        : ").append(directArms).append(" = item ").append(directArmsItem)
                    .append(" + symbol ").append(directArmsSymbol).append(" (function name ").append(directArmsSymbolFunctionName)
                    .append(", other ").append(directArmsSymbolOther).append(") + function call ").append(directArmsFunctionCall)
                    .append(" + literal ").append(directArmsLiteral).append(" + nav-on-item ").append(directArmsNavOnItem)
                    .append(" + to-string-over-nav ").append(directArmsToStringOverNav).append(" + to-string-other ")
                    .append(directArmsToStringOther).append(" + then-chain ").append(directArmsThenChain).append(" + other ")
                    .append(directArmsOther).append(' ').append(otherArmKinds).append('\n');
            b.append("  lambda-DIRECT defaults    : absent ").append(directDefaultAbsent).append(", empty ").append(directDefaultEmpty)
                    .append(", literal ").append(directDefaultLiteral).append(", other ").append(directDefaultOther).append(' ')
                    .append(otherDefaultKinds).append('\n');
            for (Map.Entry<String, List<String>> e : examples.entrySet()) {
                b.append("  examples [").append(e.getKey()).append("]: ").append(String.join(" | ", e.getValue())).append('\n');
            }
            return b.toString();
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

    /** The corpus-relative path with forward slashes, or the bare file name for a file outside the corpus. */
    static String label(Path file) {
        Path abs = file.toAbsolutePath().normalize();
        Path root = CORPUS_DIR.toAbsolutePath().normalize();
        return (abs.startsWith(root) ? root.relativize(abs) : abs.getFileName()).toString().replace('\\', '/');
    }

    static Census take(List<Path> files) {
        Census c = new Census();
        RosettaParserBaseListener listener = new RosettaParserBaseListener() {
            @Override
            public void exitFunction(RosettaParser.FunctionContext ctx) {
                if (!ctx.validID().isEmpty()) {
                    // the FIRST validID is the func's name (the second, when present, is the dispatch parameter)
                    c.declaredFunctions.add(ctx.validID(0).getText());
                }
            }

            @Override
            public void exitSwitchExpr(RosettaParser.SwitchExprContext ctx) {
                c.classify(ctx, ctx.switchCaseOrDefault());
            }

            @Override
            public void exitSwitchWithoutLeftExpr(RosettaParser.SwitchWithoutLeftExprContext ctx) {
                c.classify(ctx, ctx.switchCaseOrDefault());
            }
        };
        for (Path file : files) {
            c.files++;
            c.currentFile = label(file);
            RosettaParseResult result = RosettaParserFacade.parseFile(file);
            if (!result.errors().isEmpty()) {
                c.filesWithSyntaxErrors++;
                if (c.syntaxErrors.size() < 5) {
                    c.syntaxErrors.add(file.getFileName() + ": " + result.errors().get(0));
                }
            }
            ParseTreeWalker.DEFAULT.walk(listener, result.tree());
        }
        c.finish();
        return c;
    }

    @Test
    void every_switch_in_the_corpus_classified_by_lambda_position_guard_kind_and_arm_shape() throws IOException {
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
                    .filter(p -> !p.toString().replace('\\', '/').contains("/test-corpus/chaos/"))
                    .sorted().forEach(files::add);
        }
        long t0 = System.nanoTime();
        Census c = take(files);
        System.out.println("[" + getClass().getSimpleName() + "] the lambda-switch shape census over " + CORPUS_DIR
                + " (" + ((System.nanoTime() - t0) / 1_000_000_000L) + " s):\n" + c.table());
        assertAll("the parse-tree census of every switch in the corpus\n" + c.table(),
                () -> assertEquals(FILES, c.files, ".rosetta files walked (the manifest's population)"),
                () -> assertEquals(FILES_WITH_SYNTAX_ERRORS, c.filesWithSyntaxErrors, "files with syntax errors"),
                () -> assertEquals(SWITCHES, c.switches, "switches"),
                () -> assertEquals(OUTSIDE_LAMBDA, c.outsideLambda, "switches outside any inline function"),
                () -> assertEquals(LAMBDA_DIRECT, c.lambdaDirect, "lambda-DIRECT switches"),
                () -> assertEquals(LAMBDA_NESTED, c.lambdaNested, "lambda-nested switches"),
                () -> assertEquals(DIRECT_OWNERS, c.directOwners.toString(), "lambda-DIRECT switches by owner operator"),
                () -> assertEquals(DIRECT_CELLS, c.directCells.toString(), "lambda-DIRECT switches by corpus cell"),
                () -> assertEquals(LAMBDA_DIRECT_NAME_GUARDED, c.directNameGuarded, "lambda-direct NAME-guarded"),
                () -> assertEquals(LAMBDA_DIRECT_LITERAL_GUARDED, c.directLiteralGuarded, "lambda-direct LITERAL-guarded (the seat's literal block: ZERO vendored carriers)"),
                () -> assertEquals(LAMBDA_DIRECT_MIXED_GUARDED, c.directMixedGuarded, "lambda-direct mixed-guarded"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS, c.directArms, "lambda-direct case arms"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_ITEM, c.directArmsItem, "bare-item arms (#353)"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_SYMBOL, c.directArmsSymbol, "bare symbol arms"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_SYMBOL_FUNCTION_NAME, c.directArmsSymbolFunctionName, "bare symbol arms that name a declared func (the implicit-argument call form, #226 / seat 31)"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_SYMBOL_OTHER, c.directArmsSymbolOther, "bare symbol arms naming anything else (an input, a closure parameter, a disguised attribute)"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_FUNCTION_CALL, c.directArmsFunctionCall, "explicit function-call arms (#226 / seat 31)"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_LITERAL, c.directArmsLiteral, "literal arms (#379)"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_NAV_ON_ITEM, c.directArmsNavOnItem, "case-narrowed nav arms (the seat's shape: ZERO vendored carriers)"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_TO_STRING_OVER_NAV, c.directArmsToStringOverNav, "to-string over a case-narrowed nav (the seat's shape: ZERO vendored carriers)"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_TO_STRING_OTHER, c.directArmsToStringOther, "to-string over anything else (the fpml return leg's disguised attribute)"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_THEN_CHAIN, c.directArmsThenChain, "then-chained arms"),
                () -> assertEquals(LAMBDA_DIRECT_ARMS_OTHER, c.directArmsOther, "other arm kinds " + c.otherArmKinds),
                () -> assertEquals(OTHER_ARM_KINDS, c.otherArmKinds.toString(), "other arm kinds by rule"),
                () -> assertEquals(LAMBDA_DIRECT_DEFAULT_ABSENT, c.directDefaultAbsent, "lambda-direct switches with no default"),
                () -> assertEquals(LAMBDA_DIRECT_DEFAULT_EMPTY, c.directDefaultEmpty, "empty defaults (the pre-seat admission)"),
                () -> assertEquals(LAMBDA_DIRECT_DEFAULT_LITERAL, c.directDefaultLiteral, "literal defaults (the seat's shape: ZERO vendored carriers)"),
                () -> assertEquals(LAMBDA_DIRECT_DEFAULT_OTHER, c.directDefaultOther, "other default kinds " + c.otherDefaultKinds),
                () -> assertEquals(OTHER_DEFAULT_KINDS, c.otherDefaultKinds.toString(), "other default kinds by rule"),
                () -> assertEquals(c.switches, c.outsideLambda + c.lambdaDirect + c.lambdaNested,
                        "every switch is exactly one of the three positions"),
                () -> assertEquals(c.lambdaDirect, c.directOwners.values().stream().mapToInt(Integer::intValue).sum(),
                        "every lambda-direct switch has exactly one owner"),
                () -> assertEquals(c.lambdaDirect, c.directCells.values().stream().mapToInt(Integer::intValue).sum(),
                        "every lambda-direct switch sits in exactly one cell"),
                () -> assertEquals(c.directArmsSymbol, c.directArmsSymbolFunctionName + c.directArmsSymbolOther,
                        "every symbol arm is a function name or not"),
                () -> assertEquals(c.directArms, c.directArmsItem + c.directArmsSymbol + c.directArmsFunctionCall
                        + c.directArmsLiteral + c.directArmsNavOnItem + c.directArmsToStringOverNav + c.directArmsToStringOther
                        + c.directArmsThenChain + c.directArmsOther,
                        "every lambda-direct arm is exactly one of the nine shapes"));
    }

    /**
     * The positive control (LAW 60 / "prove the instrument can fail"): the seat's own shapes — a lambda-direct choice
     * switch with nav / to-string-over-nav arms and a literal default, a nested one, a literal-guarded one with a literal
     * and a symbol arm, an enum-keyed one with a bare-item arm, a to-string over the bare item and a symbol default, a
     * then-combinator body switch with a function-name arm and a then-chained arm and no default, a SET-seat one outside
     * any lambda, a then-chained lambda body whose head is a switch (nested, not direct) — classify into exactly those
     * counters before the zero readings on the corpus are believed.
     */
    @Test
    void the_census_classifies_every_shape_on_a_known_model() throws IOException {
        String model = String.join("\n",
                "namespace census.test",
                "version \"0.0.0\"",
                "",
                "type OptA:",
                "    av string (1..1)",
                "type OptB:",
                "    bv number (1..1)",
                "choice Either:",
                "    OptA",
                "    OptB",
                "enum Colour:",
                "    RED",
                "    BLUE",
                "",
                "func Fmt:",
                "    inputs:",
                "        s string (1..1)",
                "    output:",
                "        out string (1..1)",
                "    set out: s",
                "",
                "func Shapes:",
                "    inputs:",
                "        eths Either (0..*)",
                "        raw string (1..1)",
                "        cs Colour (0..*)",
                "        eth Either (1..1)",
                "    output:",
                "        texts string (0..*)",
                "    add texts:",
                "        eths extract (item switch OptA then item -> av, OptB then item -> bv to-string, default \"none\")",
                "    add texts:",
                "        eths extract [ if item exists then (item switch OptA then Fmt(item -> av), default \"x\") else \"y\" ]",
                "    add texts:",
                "        [raw] extract (item switch \"r\" then \"Red\", \"g\" then raw, default \"Blue\")",
                "    add texts:",
                "        cs extract (item switch RED then item, BLUE then item to-string, default raw)",
                "    add texts:",
                "        cs then item switch RED then Fmt, BLUE then raw then Fmt",
                "    add texts:",
                "        cs extract [ (item switch RED then raw, BLUE then raw) then Fmt ]",
                "    add texts:",
                "        eths extract (item switch \"m\" then \"mixed\", OptA then item -> av, default \"none\")",
                "    add texts:",
                "        raw then item switch \"b\" then \"Blue\", default \"Grey\"",
                "    add texts:",
                "        eth switch OptA then item -> av, default \"none\"");
        Path tmp = Files.createTempFile("lambda-switch-census-control", ".rosetta");
        try {
            Files.writeString(tmp, model);
            Census c = take(List.of(tmp));
            assertAll("the positive control\n" + c.table(),
                    () -> assertEquals(1, c.files),
                    () -> assertEquals(0, c.filesWithSyntaxErrors, "the control model must parse clean"),
                    () -> assertEquals("[Fmt, Shapes]", c.declaredFunctions.toString(), "the declared funcs"),
                    () -> assertEquals(9, c.switches, "switches"),
                    () -> assertEquals(1, c.outsideLambda, "the SET-seat switch"),
                    () -> assertEquals(6, c.lambdaDirect, "the choice, literal, enum-extract, then-body, mixed-guard and then-body-literal switches"),
                    () -> assertEquals(2, c.lambdaNested, "the switch under an if inside the lambda + the then-chained lambda body headed by a switch"),
                    () -> assertEquals("{ExtractExpr=4, ThenSuffix=2}", c.directOwners.toString(), "the owner histogram"),
                    () -> assertEquals("{(outside the corpus)=6}", c.directCells.toString(), "the cell histogram"),
                    () -> assertEquals(3, c.directNameGuarded, "NAME-guarded: the choice, the enum-extract and the then-body switch"),
                    // round 3 (the spec review's NIT-3): the eleventh register site's COMPOSITE class - a LITERAL guard under a
                    // ThenSuffix owner - pinned by measurement here, where the vendored figure LAMBDA_DIRECT_LITERAL_GUARDED = 0
                    // covers it over all owners by construction
                    () -> assertEquals(2, c.directLiteralGuarded, "LITERAL-guarded: the [raw] extract switch and the then-body \"b\" switch"),
                    // round 1 (the spec review's NIT-2): the MIXED counter's positive control - a STRING-literal guard beside
                    // a NAME guard (the seat-2 banked shape), counted as neither NAME- nor LITERAL-guarded
                    () -> assertEquals(1, c.directMixedGuarded, "MIXED-guarded: the \"m\" / OptA switch"),
                    () -> assertEquals(11, c.directArms, "arms: 2 + 2 + 2 + 2 + 2 + 1"),
                    () -> assertEquals(2, c.directArmsNavOnItem, "item -> av (the choice switch and the mixed switch)"),
                    () -> assertEquals(1, c.directArmsToStringOverNav, "item -> bv to-string"),
                    () -> assertEquals(1, c.directArmsToStringOther, "item to-string"),
                    () -> assertEquals(1, c.directArmsItem, "RED then item"),
                    () -> assertEquals(2, c.directArmsSymbol, "\"g\" then raw, RED then Fmt"),
                    () -> assertEquals(1, c.directArmsSymbolFunctionName, "RED then Fmt (a declared func)"),
                    () -> assertEquals(1, c.directArmsSymbolOther, "\"g\" then raw (an input)"),
                    () -> assertEquals(1, c.directArmsThenChain, "BLUE then raw then Fmt"),
                    () -> assertEquals(3, c.directArmsLiteral, "\"Red\", \"mixed\", \"Blue\""),
                    () -> assertEquals(0, c.directArmsFunctionCall, "no lambda-DIRECT explicit function-call arm (the nested one is not counted)"),
                    () -> assertEquals(0, c.directArmsOther, "no other arm kind " + c.otherArmKinds),
                    () -> assertEquals(4, c.directDefaultLiteral, "\"none\", \"Blue\", \"none\", \"Grey\""),
                    () -> assertEquals(0, c.directDefaultEmpty, "no empty default"),
                    () -> assertEquals(1, c.directDefaultOther, "default raw"),
                    () -> assertEquals("{SymbolRefExpr=1}", c.otherDefaultKinds.toString(), "the symbol default"),
                    () -> assertEquals(1, c.directDefaultAbsent, "the then-body switch carries no default"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
