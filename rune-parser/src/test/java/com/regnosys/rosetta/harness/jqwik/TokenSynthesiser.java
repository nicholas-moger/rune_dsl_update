package com.regnosys.rosetta.harness.jqwik;

import org.antlr.v4.runtime.Vocabulary;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Maps a grammar token type to a valid source-level lexeme.
 *
 * <p>P1.2 audit hook H18. The ATN walker ({@link AtnStringGenerator})
 * emits token types as it traces transitions; this class turns each
 * type into a string the lexer will re-tokenise identically.
 *
 * <ol>
 *   <li>Literal tokens ({@code 'namespace'}, {@code ':'}, …) —
 *       {@link Vocabulary#getLiteralName(int)} returns the quoted form;
 *       strip the surrounding {@code '…'}.
 *   <li>Synthesised tokens ({@code ID}, {@code STRING},
 *       {@code INT_LITERAL}, {@code BIG_DECIMAL}) — produce a valid,
 *       non-reserved lexeme using the supplied {@link Random}.
 * </ol>
 *
 * <p>Unknown / non-reproducible token types return {@code null}; the
 * walker treats that as "skip — no emit" (used for {@code EOF} and
 * lexer-only tokens that never appear in parser rules).
 */
final class TokenSynthesiser {

    private final Vocabulary vocabulary;
    /**
     * Symbolic name → synthesis strategy. Eagerly built in the constructor;
     * it reads the full literal-name table from {@link Vocabulary} to populate
     * its reserved-keyword set for collision detection in {@code randomIdent}.
     */
    private final SymbolicLookup symbolicLookup;

    TokenSynthesiser(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
        this.symbolicLookup = new SymbolicLookup(vocabulary);
    }

    /**
     * Return a source-level lexeme for {@code tokenType}, or {@code null}
     * if the type cannot be synthesised (caller should skip).
     */
    String synthesise(int tokenType, Random random) {
        String literal = vocabulary.getLiteralName(tokenType);
        if (literal != null) return stripQuotes(literal);

        String symbolic = vocabulary.getSymbolicName(tokenType);
        if (symbolic == null) return null;
        return symbolicLookup.synthesise(symbolic, random);
    }

    /** Strip ANTLR's surrounding {@code '…'} from a literal name. */
    static String stripQuotes(String literal) {
        if (literal.length() >= 2 && literal.charAt(0) == '\'' && literal.charAt(literal.length() - 1) == '\'') {
            return literal.substring(1, literal.length() - 1);
        }
        return literal;
    }

    /**
     * Small, explicit table of symbolic-name → synthesiser. Each entry
     * emits a lexeme the corresponding lexer rule will accept. Entries
     * not listed are deliberately {@code null}-returning: the walker
     * skips tokens it cannot synthesise rather than emit bogus bytes.
     */
    private static final class SymbolicLookup {

        /**
         * Reserved keywords / literal tokens known to the lexer. A
         * randomly-generated identifier matching any of these would
         * re-tokenise as the keyword, not as {@code ID} — breaking the
         * contract that an {@code ID} token type yields a lexeme the
         * lexer classifies identically on the way back.
         */
        private final Set<String> reservedLiterals;

        SymbolicLookup(Vocabulary vocabulary) {
            this.reservedLiterals = collectLiterals(vocabulary);
        }

        private static Set<String> collectLiterals(Vocabulary vocabulary) {
            Set<String> out = new HashSet<>();
            for (int t = 0; t <= vocabulary.getMaxTokenType(); t++) {
                String literal = vocabulary.getLiteralName(t);
                if (literal == null) continue;
                out.add(stripQuotes(literal));
            }
            return out;
        }

        String synthesise(String name, Random random) {
            return switch (name) {
                case "ID" -> randomIdent(random);
                case "STRING" -> "\"" + randomWord(random, 3, 8) + "\"";
                case "INT_LITERAL" -> Integer.toString(random.nextInt(1000));
                case "BIG_DECIMAL" -> random.nextInt(100) + "." + random.nextInt(100);
                default -> null;
            };
        }

        /**
         * Emit a lexeme that will lex as {@code ID}. First re-rolls on
         * collisions with reserved literals so the natural form is
         * preferred; on persistent collision falls back to the ANTLR
         * {@code ^} escape prefix ({@code RosettaLexer.g4:287}:
         * {@code ID : '^'? [a-zA-Z_] [a-zA-Z_0-9]* ;}), which the lexer
         * always tokenises as {@code ID} regardless of the keyword
         * table. This makes the method provably total under any future
         * keyword — including keywords with underscores or digits such
         * as {@code rationale_author} or {@code structured_provision}.
         */
        private String randomIdent(Random random) {
            for (int attempt = 0; attempt < 16; attempt++) {
                String candidate = rollIdent(random);
                if (!reservedLiterals.contains(candidate)) return candidate;
            }
            // Guaranteed ID — `^`-prefix tells the Rosetta lexer to
            // treat what follows as an identifier even if it matches a
            // reserved word exactly. The prefix is part of the token
            // text but not of the identifier's logical value.
            return "^" + rollIdent(random);
        }

        private static String rollIdent(Random random) {
            StringBuilder sb = new StringBuilder();
            sb.append((char) ('a' + random.nextInt(26)));
            int len = 2 + random.nextInt(6);
            for (int i = 0; i < len; i++) {
                int r = random.nextInt(36);
                if (r < 26) sb.append((char) ('a' + r));
                else sb.append((char) ('0' + (r - 26)));
            }
            return sb.toString();
        }

        private static String randomWord(Random random, int minLen, int maxLen) {
            int len = minLen + random.nextInt(maxLen - minLen + 1);
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append((char) ('a' + random.nextInt(26)));
            }
            return sb.toString();
        }
    }
}
