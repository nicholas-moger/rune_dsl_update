package com.regnosys.rosetta.generator.java.ir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SHARING LAW, witnessed (v3.3 seat 9, PR #645 commit 12): an emitter writes from the IR ALONE plus the shared PURE text
 * functions - it never reaches a legacy GENERATOR or a legacy MODEL-READING class. The law is stated on every emitter's javadoc
 * (CONTRACT-C10 § 1, CONTRACT-C12 § 1.2) and, until this commit, was enforced by reading. Lane P18 of {@code lanes-s9c12.sh}
 * (an emitter calling {@code ValidatorScan.castType}) was aimed at the evidence-API ledger, and the driver's re-take of that
 * lane on the seat's tree read GREEN: the ledger enumerates RENDERED-STRING sites, not calls, so the RED the implementer's run
 * had read named an unrelated, then-unledgered site. No byte test can see the mutation either (the reached seam renders the
 * same text). This class is the witness that can: it reads each emitter's SOURCE and refuses any reference, imported or fully
 * qualified, to a class of the legacy generator's model-reading packages.
 *
 * <p>It is a LITERAL-TOKEN lint over import lines and fully-qualified names with the comments stripped - the evidence-API
 * ledger's own precedent ({@code scripts/ci/evidence-api-ledger.py}), not a structural analysis of Java; a dependency rule on
 * bytecode (ArchUnit) is the stronger form and is not in this build's offline repository. The allowed references are the ones
 * the emitters carry at this commit and the reviewers ratified: the vendored scope machinery over VALUES
 * ({@code scoping.*}), the text helpers ({@code template.*}, {@code util.ModelGeneratorUtil}, {@code enums.EnumHelper},
 * {@code JavaNamingUtil}), the statement builders the compat member ports ({@code statement.*}), {@code types.JavaTypeUtil}
 * (stateless; the POJO emitter's compat algebra takes it since commit 10) and {@code MetaFieldGenerator.MetaKind} - the ENUM
 * VALUE the meta-kind law answers, never the generator. A NEW reference to anything else under
 * {@code com.regnosys.rosetta.generator.java} that is not the IR package fails by name, file and line.
 *
 * <p><b>THE AST AND THE WORKSPACE, since v3.3 seat 9, PR #645 commit 18 (round 1 cq SF-4).</b> Through commit 17 the lint
 * forbade the legacy GENERATOR packages and nothing else, so an emitter importing {@code com.regnosys.rosetta.ast.*} or
 * {@code com.regnosys.rosetta.symbols.RWorkspace} and reading a DECLARATION directly passed it - the larger breach of the very
 * law this class states, and a live one: {@code IRDerivedFacts} carried {@code RNode}, {@code RChoice} and {@code RDataType}
 * for the deep-path walk's descend target. Both roots are forbidden now. The three AST imports were not allow-listed, they were
 * REMOVED: the hop moved onto {@link IRTypeIndex#descendTarget}, the ADAPTER BOUNDARY, which is not an emitter and whose job
 * IS to hold the parser's nodes. What remains is ONE per-class allowance, {@link #ALLOWED_PER_CLASS} - a PURE STATIC TEXT
 * FUNCTION over a literal's characters, in the same category as the template and naming helpers above, never a model read.
 *
 * <p>THE SCAN follows the FORBIDDEN LIST and is not a second copy of it: every dotted name under
 * {@code com.regnosys.rosetta.} is collected and {@link #FORBIDDEN_PREFIXES} alone decides, so a prefix added there is
 * enforced by that edit and no other (LAW 69). Before commit 18 the scan was pinned to the generator package, which is why
 * adding a root to the forbidden list alone would have enforced nothing.
 */
class IRSharingLawTest {

    /** The classes the law binds: every member emitter, its shared pure helpers, and the unit. */
    static final List<String> EMITTERS = List.of(
            "IRDataTypeEmitter", "IRPojoCompat", "IRJavaTypes", "IREnumEmitter",
            "IRTypeFormatValidatorEmitter", "IRCardinalityValidatorEmitter", "IROnlyExistsValidatorEmitter",
            "IRModelMetaEmitter", "IRDeepPathUtilEmitter",
            "IRValidatorScan", "IRDerivedFacts", "IRTypeUnit");

    /** The legacy packages / classes no emitter may reach - the generators and everything that reads the model for them. */
    static final List<String> FORBIDDEN_PREFIXES = List.of(
            "com.regnosys.rosetta.generator.java.object.",
            "com.regnosys.rosetta.generator.java.function.",
            "com.regnosys.rosetta.generator.java.types.JavaTypeTranslator",
            "com.regnosys.rosetta.generator.java.types.JavaPojoProperty",
            "com.regnosys.rosetta.generator.java.GeneratorModel",
            "com.regnosys.rosetta.generator.java.JavaClassGenerator",
            "com.regnosys.rosetta.generator.java.spi.",
            // v3.3 seat 9, PR #645 commit 18 (round 1 cq SF-4): the DECLARATION itself. An emitter that reads the parser's
            // AST, or the linked workspace, is not writing from the IR alone - the larger breach of the law above.
            "com.regnosys.rosetta.ast.",
            "com.regnosys.rosetta.symbols.");

    /** The one name under a forbidden prefix an emitter may carry: the meta-kind ENUM VALUE, not the generator. */
    static final Set<String> ALLOWED_UNDER_FORBIDDEN = Set.of(
            "com.regnosys.rosetta.generator.java.object.MetaFieldGenerator.MetaKind");

    /**
     * THE PER-CLASS ALLOWANCES (v3.3 seat 9, PR #645 commit 18): a name ONE named emitter may carry although it sits under a
     * forbidden prefix, with the law it serves. It is NOT a place to park a convenience - a use the IR can replace is
     * replaced, as the descend target was (see the class javadoc).
     * <ul>
     *   <li>{@code IRDerivedFacts} -> {@code AstBuilderHelper}: {@code stripQuotes} ALONE, a PURE STATIC TEXT FUNCTION over
     *       a literal's own characters ({@code AstBuilderHelper:202-212}: the surrounding quotes off, the standard escapes
     *       unescaped). The constraint envelope's {@code pattern} argument arrives as a quoted literal and must be
     *       unquoted EXACTLY as the old generator unquotes it, so a second copy here would be a drift surface, not an
     *       independence. It reads no model, resolves no name and holds no state - the same category as
     *       {@code template.*}, {@code util.ModelGeneratorUtil} and {@code JavaNamingUtil}, which this lint already
     *       allows; it sits under {@code ast.} only because that is where the parser keeps its text helpers.</li>
     * </ul>
     */
    static final java.util.Map<String, Set<String>> ALLOWED_PER_CLASS = java.util.Map.of(
            "IRDerivedFacts", Set.of("com.regnosys.rosetta.ast.builder.AstBuilderHelper"));

    /**
     * The root every dotted name is collected under. It is deliberately WIDER than the forbidden list, so that
     * {@link #FORBIDDEN_PREFIXES} is the ONE declaration of what is refused (LAW 69): before commit 18 the scan was pinned
     * to {@code com.regnosys.rosetta.generator.java.} and a root added to the forbidden list alone would have enforced
     * nothing, because no name outside that package was ever looked at.
     */
    private static final String PACKAGE_PREFIX = "com.regnosys.rosetta.";

    @Test
    void noEmitterReachesALegacyGeneratorOrModelReadingClass() throws IOException {
        List<String> findings = new ArrayList<>();
        for (String emitter : EMITTERS) {
            Path source = sourceOf(emitter);
            assertTrue(Files.exists(source), "the emitter source exists: " + source);
            findings.addAll(violations(emitter + ".java", Files.readString(source, StandardCharsets.UTF_8)));
        }
        assertEquals(List.of(), findings, "the sharing law: an emitter references a legacy generator class it may not reach");
    }

    /** The lint can FAIL: a source that calls the legacy validator scan is named by file, line and canonical name. */
    @Test
    void theLintNamesAnEmitterThatReachesTheLegacyValidatorScan() {
        String text = "package com.regnosys.rosetta.generator.java.ir;\n"
                + "// a comment naming com.regnosys.rosetta.generator.java.object.validators.ValidatorScan is not a reference\n"
                + "/* nor is com.regnosys.rosetta.generator.java.object.ModelObjectGenerator in a block comment */\n"
                + "import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;\n"
                + "final class Probe {\n"
                + "    static String cast(Object p) {\n"
                + "        return com.regnosys.rosetta.generator.java.object.validators.ValidatorScan.castType(p, null);\n"
                + "    }\n"
                + "    static Object kind() { return com.regnosys.rosetta.generator.java.object.MetaFieldGenerator.MetaKind.NONE; }\n"
                + "}\n";
        List<String> findings = violations("Probe.java", text);
        assertEquals(List.of("Probe.java:7 references com.regnosys.rosetta.generator.java.object.validators.ValidatorScan"
                + " - a legacy generator class no emitter may reach (the sharing law)"), findings);
        String imported = "import com.regnosys.rosetta.generator.java.object.validators.ValidatorScan;\n";
        assertEquals(List.of("Probe.java:1 references com.regnosys.rosetta.generator.java.object.validators.ValidatorScan"
                + " - a legacy generator class no emitter may reach (the sharing law)"), violations("Probe.java", imported));
    }

    /**
     * THE NEW ROOTS CAN FAIL (v3.3 seat 9, PR #645 commit 18, round 1 cq SF-4). An emitter that reads a DECLARATION - the
     * parser's AST or the linked workspace - is named by file, line and canonical name, exactly as a legacy-generator
     * reference is; a COMMENT naming one is still not a reference; and the per-class allowance answers for the ONE name
     * {@link #ALLOWED_PER_CLASS} states a law for, on THAT class alone and not on its neighbour.
     */
    @Test
    void theLintNamesAnEmitterThatReadsTheAstOrTheWorkspaceDirectly() {
        String text = "package com.regnosys.rosetta.generator.java.ir;\n"
                + "// a comment naming com.regnosys.rosetta.ast.types.RDataType is not a reference\n"
                + "import com.regnosys.rosetta.ast.RNode;\n"
                + "import com.regnosys.rosetta.ir.core.IRField;\n"
                + "final class Probe {\n"
                + "    static Object target(Object index, String name) {\n"
                + "        return com.regnosys.rosetta.symbols.RWorkspace.class;\n"
                + "    }\n"
                + "}\n";
        assertEquals(List.of(
                "Probe.java:3 references com.regnosys.rosetta.ast.RNode"
                        + " - a legacy generator class no emitter may reach (the sharing law)",
                "Probe.java:7 references com.regnosys.rosetta.symbols.RWorkspace"
                        + " - a legacy generator class no emitter may reach (the sharing law)"),
                violations("Probe.java", text));

        // the PER-CLASS allowance: the pure text helper passes on IRDerivedFacts and on no other class
        String helper = "import com.regnosys.rosetta.ast.builder.AstBuilderHelper;\n";
        assertEquals(List.of(), violations("IRDerivedFacts.java", helper));
        assertEquals(List.of("Probe.java:1 references com.regnosys.rosetta.ast.builder.AstBuilderHelper"
                + " - a legacy generator class no emitter may reach (the sharing law)"),
                violations("Probe.java", helper));

        // and the allowance is EXACTLY that name: a second AST class on the allowed class is still named
        assertEquals(List.of("IRDerivedFacts.java:1 references com.regnosys.rosetta.ast.types.RDataType"
                + " - a legacy generator class no emitter may reach (the sharing law)"),
                violations("IRDerivedFacts.java", "import com.regnosys.rosetta.ast.types.RDataType;\n"));
    }

    static Path sourceOf(String simpleName) {
        Path relative = Paths.get("src", "main", "java", "com", "regnosys", "rosetta", "generator", "java", "ir",
                simpleName + ".java");
        if (Files.exists(relative)) {
            return relative;
        }
        return Paths.get("rune-ir-java").resolve(relative);   // a runner whose working directory is the repository root
    }

    /** Every forbidden reference of a source text, comments stripped, as {@code file:line references canonical - …}. */
    static List<String> violations(String fileName, String text) {
        List<String> findings = new ArrayList<>();
        // the per-class allowances are keyed by the SIMPLE NAME, which is this file's own (v3.3 seat 9, PR #645 commit 18)
        String simpleName = fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - ".java".length())
                : fileName;
        Set<String> allowedHere = ALLOWED_PER_CLASS.getOrDefault(simpleName, Set.of());
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        boolean inBlockComment = false;
        for (int i = 0; i < lines.length; i++) {
            String code = stripComments(lines[i], inBlockComment);
            inBlockComment = endsInsideBlockComment(lines[i], inBlockComment);
            for (String canonical : qualifiedNames(code)) {
                // the FULL name decides (so MetaFieldGenerator.MetaKind.NONE is the allowed enum value); the CLASS is named
                if (isForbidden(canonical, allowedHere)) {
                    findings.add(fileName + ":" + (i + 1) + " references " + className(canonical)
                            + " - a legacy generator class no emitter may reach (the sharing law)");
                }
            }
        }
        return findings;
    }

    /** The name up to and including its first capitalised segment - the class, without the member or nested type after it. */
    private static String className(String canonical) {
        String[] segments = canonical.split("\\.");
        StringBuilder name = new StringBuilder();
        for (String segment : segments) {
            if (name.length() > 0) {
                name.append('.');
            }
            name.append(segment);
            if (!segment.isEmpty() && Character.isUpperCase(segment.charAt(0))) {
                break;
            }
        }
        return name.toString();
    }

    private static boolean isForbidden(String canonical, Set<String> allowedHere) {
        for (String allowed : ALLOWED_UNDER_FORBIDDEN) {
            if (canonical.equals(allowed) || canonical.startsWith(allowed + ".")) {
                return false;
            }
        }
        // the allowances THIS class carries, by name and with its law stated on ALLOWED_PER_CLASS
        for (String allowed : allowedHere) {
            if (canonical.equals(allowed) || canonical.startsWith(allowed + ".")) {
                return false;
            }
        }
        for (String prefix : FORBIDDEN_PREFIXES) {
            if (canonical.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** The dotted names starting with the generator package on a line of CODE, longest form (up to the member that follows). */
    private static List<String> qualifiedNames(String code) {
        List<String> names = new ArrayList<>();
        int from = 0;
        while (true) {
            int at = code.indexOf(PACKAGE_PREFIX, from);
            if (at < 0) {
                return names;
            }
            int end = at;
            while (end < code.length() && (Character.isJavaIdentifierPart(code.charAt(end)) || code.charAt(end) == '.')) {
                end++;
            }
            String name = code.substring(at, end);
            while (name.endsWith(".")) {
                name = name.substring(0, name.length() - 1);
            }
            names.add(name);
            from = end;
        }
    }

    /** The line with its comments removed: a block comment in progress, {@code /* … *}{@code /} spans, and a trailing {@code //}. */
    private static String stripComments(String line, boolean inBlockComment) {
        StringBuilder code = new StringBuilder();
        boolean inBlock = inBlockComment;
        int i = 0;
        while (i < line.length()) {
            if (inBlock) {
                int close = line.indexOf("*/", i);
                if (close < 0) {
                    return code.toString();
                }
                i = close + 2;
                inBlock = false;
                continue;
            }
            int open = line.indexOf("/*", i);
            int lineComment = line.indexOf("//", i);
            if (lineComment >= 0 && (open < 0 || lineComment < open)) {
                code.append(line, i, lineComment);
                return code.toString();
            }
            if (open >= 0) {
                code.append(line, i, open);
                i = open + 2;
                inBlock = true;
                continue;
            }
            code.append(line, i, line.length());
            return code.toString();
        }
        return code.toString();
    }

    private static boolean endsInsideBlockComment(String line, boolean inBlockComment) {
        boolean inBlock = inBlockComment;
        int i = 0;
        while (i < line.length()) {
            if (inBlock) {
                int close = line.indexOf("*/", i);
                if (close < 0) {
                    return true;
                }
                i = close + 2;
                inBlock = false;
                continue;
            }
            int open = line.indexOf("/*", i);
            int lineComment = line.indexOf("//", i);
            if (lineComment >= 0 && (open < 0 || lineComment < open)) {
                return false;
            }
            if (open >= 0) {
                i = open + 2;
                inBlock = true;
                continue;
            }
            return false;
        }
        return inBlock;
    }
}
