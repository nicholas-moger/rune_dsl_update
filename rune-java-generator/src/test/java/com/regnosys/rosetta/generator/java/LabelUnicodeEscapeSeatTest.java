package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.template.JavaStringUtil;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

/**
 * SEAT 22, law F2 — facet {@code labelUnicodeEscape}: <b>every Java string literal the generator emits is
 * escaped exactly as upstream's {@code org.apache.commons.text.StringEscapeUtils.escapeJava} (commons-text
 * 1.12.0) escapes it — the quote/backslash lookup, the {@code JAVA_CTRL_CHARS_ESCAPE} table
 * ({@code \b \n \t \f \r}) and {@code JavaUnicodeEscaper.outsideOf(32, 0x7f)}: every other UTF-16 unit below
 * {@code 0x20} or above {@code 0x7f} renders {@code \\uXXXX} with UPPERCASE hex; {@code 0x20..0x7f} (DEL
 * included) stays raw.</b> Upstream applies that ONE function at every literal emitter ({@code EnumGenerator},
 * {@code LabelProviderGenerator}, {@code TypeFormatValidatorGenerator} and {@code JavaLiteral.from(String)} for
 * expression string literals).
 * <b>Corrected at v3.2 seat 9 (PR #630, D46 — measured against the released plugin):</b> the enum value's
 * {@code displayName} inside {@code @RosettaEnumValue} is a TEMPLATE SPLICE upstream leaves RAW (non-ASCII, a
 * quote, a backslash and a tab alike — its own non-compiling emission for the last three, pinned as such); the
 * ONE escape covers the enum's CONSTRUCTOR literal alone. {@code a4} pins both seats since then.
 *
 * <p><b>The defect.</b> The fork's {@code JavaStringUtil.escapeJava} handled the five
 * {@code " \ \n \r \t} cases and appended every other char RAW, and the two expression-literal emitters
 * ({@code LiteralHandler.handle(RStringLiteral)} on the legacy route, {@code IRJavaLeafEmitter.emitString} on
 * the IR route) each carried a private five-case mirror. drr 7.0–7.3's {@code SECPPDLabelProvider} /
 * {@code SECTradeLabelProvider} carried the en dash of the label
 * {@code "13n–5(b)(1)(iii) Submitting Party ID"} raw where golden has {@code 13n–5(b)…} — 8 whole-file
 * carriers (2 per cell); the files compile both ways (a pure parity heal, LAW 74: 0 repairs).
 *
 * <p><b>The seat.</b> ONE method — {@code JavaStringUtil.escapeJava} — gains the control-char table entries
 * and the {@code \\uXXXX} leg (uppercase hex, the {@code 0x20..0x7f} raw band), and the two expression-literal
 * mirrors are retired to it (LAW 69 / LAW 77: the same text on both routes). The reference behaviour was
 * RE-DERIVED by running commons-text 1.12.0 itself
 * ({@code target/seat22-instruments/artefacts/escapejava-reference.txt}) — the seat-22 charter's
 * {@code c > 0x7e} bound and lowercase-hex expectation were both wrong against it; this suite's unit pins
 * ({@code u1–u7}) ARE that reference table.
 *
 * <p><b>LAW 75 — measured before the seat over all 275 matrix rows (the seat-22 runtime probe):</b> exactly
 * 8 non-ASCII inputs reached {@code escapeJava}, every one {@code LabelProviderGenerator.emitClass}, every one
 * the en-dash string, exactly the 8 carriers; the static corpus census: 8 golden files corpus-wide carry a
 * {@code \\uXXXX} escape (= the carriers), 269 goldens carry raw non-ASCII on 510 COMMENT lines and ZERO code
 * lines — the leg on the literal emitters has no green regression surface, and a comment emitter must NEVER
 * consult it ({@code b3}).
 *
 * <p><b>RED at the pre-seat blob</b> ({@code rune-java-generator/src/main} + {@code rune-ir-java/src/main} at
 * {@code e2807ac1} — the seat-22 commit-1 head — this suite kept): the a-tests, the unit pins that name the new
 * legs, {@code corpus_c1}/{@code c2}/{@code control1}; every {@code b*} + {@code control0} GREEN in both
 * states. <b>LAW 66/76 mutations</b> (each applied → run → reverted at the FINAL head, LAW 78; the receipts
 * are recorded in the PR body §7): (i) the {@code \\uXXXX} leg deleted; (ii) the raw band narrowed to
 * {@code 0x20..0x7e} (DEL escaped) → {@code u4} (a declared fixture pin: no corpus literal carries DEL);
 * (iii) lowercase hex → {@code u1}/{@code a3}/{@code a4}/{@code a6} (declared: the en-dash carriers are
 * digit-only); (iv) the {@code \b}/{@code \f} table entries dropped → {@code u3}; (v) the legacy expression
 * literal's delegation reverted to its old private mirror → {@code a6}; (vi) the IR-route leaf's reverted →
 * {@code a7} ({@code -Pir-on}); (vii) the escape applied to the enum javadoc → {@code b3}.
 */
class LabelUnicodeEscapeSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean builtinsAndIrProviderAvailable() {
        return builtinsAvailable() && irProviderOnClasspath();
    }

    // The non-ASCII fixture chars, spelled as Java escapes here so the SOURCE of this suite stays ASCII.
    private static final String EN_DASH = String.valueOf((char) 0x2013);
    private static final String CJK = new String(new char[] { 0x4E2D, 0x6587 });
    private static final String E_ACUTE = String.valueOf((char) 0xE9);
    private static final String I_DIAERESIS = String.valueOf((char) 0xEF);
    private static final String BSU = String.valueOf((char) 0x5C) + "u";   // the two-char text  backslash-u  in generated Java
    private static final String NUL = String.valueOf((char) 0x00);
    private static final String SOH = String.valueOf((char) 0x01);
    private static final String C1_NEL = String.valueOf((char) 0x85);
    private static final String DEL = String.valueOf((char) 0x7F);
    private static final String SMILEY = new String(Character.toChars(0x1F600));

    /**
     * The fixture: the SEC label shape (a type attribute {@code [label "13n–5(b)…"]} read by the label
     * provider of an {@code [ingest XML]} function), a CJK label, ASCII + legacy-escape labels (the
     * byte-locks), an enum whose DISPLAY NAME carries {@code é} (escaped) and whose DEFINITION carries
     * {@code é} (a javadoc — NOT escaped), and a function whose body is a non-ASCII STRING LITERAL (the
     * expression seat on both routes).
     */
    private static final String MODEL = String.join("\n",
            "namespace census.seat22f2",
            "version \"1.0.0\"",
            "",
            "type Trade:",
            "    submitter string (0..1)",
            "        [label \"13n" + EN_DASH + "5(b)(1)(iii) Submitting Party ID\"]",
            "    cjk string (0..1)",
            "        [label \"" + CJK + "\"]",
            "    plain string (0..1)",
            "        [label \"plain ASCII label\"]",
            "    quoted string (0..1)",
            "        [label \"Name with \\\"quotes\\\" and \\\\ backslash\"]",
            "",
            "enum Kind: <\"D" + E_ACUTE + "finition with a raw accent in the javadoc\">",
            "    ACCENT displayName \"" + E_ACUTE + "-value\"",
            "    PLAIN displayName \"plain\"",
            "",
            "func Ingest:",
            "    [ingest XML]",
            "    inputs:",
            "        x string (1..1)",
            "    output:",
            "        result Trade (1..1)",
            "",
            "func StrLit:",
            "    output:",
            "        result string (1..1)",
            "    set result:",
            "        \"na" + I_DIAERESIS + "ve\"",
            "");

    // =========================================================================
    // Part U — the reference table (commons-text 1.12.0 escapeJava, re-derived by running it)
    // =========================================================================

    /** u1 — non-ASCII above 0x7f renders \\uXXXX with UPPERCASE hex (en dash, CJK, é). */
    @Test
    void u1_nonAsciiRendersUppercaseUnicodeEscape() {
        assertEquals("13n" + BSU + "20135(b)", JavaStringUtil.escapeJava("13n" + EN_DASH + "5(b)"));
        assertEquals(BSU + "4E2D" + BSU + "6587", JavaStringUtil.escapeJava(CJK));
        assertEquals(BSU + "00E9", JavaStringUtil.escapeJava(E_ACUTE));
    }

    /** u2 — control chars below 0x20 (and the C1 range) render \\uXXXX: NUL, SOH, U+0085. */
    @Test
    void u2_controlCharsRenderUnicodeEscape() {
        assertEquals(BSU + "0000", JavaStringUtil.escapeJava(NUL));
        assertEquals(BSU + "0001", JavaStringUtil.escapeJava(SOH));
        assertEquals(BSU + "0085", JavaStringUtil.escapeJava(C1_NEL));
    }

    /** u3 — \\b and \\f take the JAVA_CTRL_CHARS_ESCAPE two-char forms, NOT \\u0008 / \\u000C. */
    @Test
    void u3_backspaceAndFormFeedTakeTheTableForms() {
        assertEquals("a\\bb", JavaStringUtil.escapeJava("a\bb"));
        assertEquals("a\\fb", JavaStringUtil.escapeJava("a\fb"));
        assertEquals("\\b\\f", JavaStringUtil.escapeJava("\b\f"));
    }

    /** u4 — the raw band is 0x20..0x7f INCLUSIVE: space, tilde and DEL stay raw (the outsideOf(32, 0x7f) bound). */
    @Test
    void u4_rawBandIncludesDel() {
        assertEquals(" ~" + DEL, JavaStringUtil.escapeJava(" ~" + DEL));
        assertEquals(DEL, JavaStringUtil.escapeJava(DEL));
    }

    /** u5 — a non-BMP code point renders its two surrogate units, each escaped. */
    @Test
    void u5_surrogatePairRendersTwoEscapes() {
        assertEquals(BSU + "D83D" + BSU + "DE00", JavaStringUtil.escapeJava(SMILEY));
    }

    /** u6 — the five legacy escapes are byte-unchanged. */
    @Test
    void u6_legacyEscapesUnchanged() {
        assertEquals("\\\"", JavaStringUtil.escapeJava("\""));
        assertEquals("\\\\", JavaStringUtil.escapeJava("\\"));
        assertEquals("\\n\\r\\t", JavaStringUtil.escapeJava("\n\r\t"));
        assertEquals("plain ASCII 0-9 A-Z a-z !#$%&'()*+,-./:;<=>?@[]^_`{|}",
                JavaStringUtil.escapeJava("plain ASCII 0-9 A-Z a-z !#$%&'()*+,-./:;<=>?@[]^_`{|}"));
    }

    /** u7 — null in, null out; empty in, empty out. */
    @Test
    void u7_nullAndEmpty() {
        assertNull(JavaStringUtil.escapeJava(null));
        assertEquals("", JavaStringUtil.escapeJava(""));
    }

    // =========================================================================
    // Part A — the seat through the generators (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the SEC shape: an en dash in a label renders \\u2013 in the label provider's literal. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_enDashLabelEscapedInLabelProvider() throws IOException {
        String out = labelProvider();
        assertContains(out, "\"13n" + BSU + "20135(b)(1)(iii) Submitting Party ID\"");
        assertNotContains(out, EN_DASH);
    }

    /** a3 — a CJK label renders \\u4E2D\\u6587 (uppercase hex). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_cjkLabelEscapedUppercase() throws IOException {
        String out = labelProvider();
        assertContains(out, "\"" + BSU + "4E2D" + BSU + "6587\"");
        assertNotContains(out, CJK);
        assertNotContains(out, BSU + "4e2d");
    }

    /**
     * a4 — an enum DISPLAY NAME with é: the CONSTRUCTOR literal renders \\u00E9 (EnumGenerator's literal emitter)
     * while the ANNOTATION carries it RAW (v3.2 seat 9, PR #630, D46: the released plugin splices
     * {@code displayName = "…"} into {@code @RosettaEnumValue} unescaped and Java-escapes the constructor argument
     * alone; this pin had asserted the escape at BOTH seats — RED at the D46 content in the chain's whole generator
     * suite at {@code 44d323815}, re-cut to the measured law; {@code EnumGeneratorTest}'s three D46 pins and the
     * hold-out groups enum-unicode-display / -edge are the byte witnesses).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_enumDisplayNameEscaped() throws IOException {
        String out = enumKind();
        assertContains(out, "(\"ACCENT\", \"" + BSU + "00E9-value\")");
        assertContains(out, "displayName = \"" + E_ACUTE + "-value\"");
        assertNotContains(out, "displayName = \"" + BSU + "00E9-value\"");
    }

    /**
     * a6 — an expression STRING LITERAL with ï renders \\u00EF on the legacy route (LiteralHandler → the
     * ONE escape; the scalar-literal direct SET unwraps the Mapper, so the receipt is the literal text).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_expressionStringLiteralEscapedLegacyRoute() throws IOException {
        String out = function("StrLit.java");
        assertContains(out, "\"na" + BSU + "00EFve\"");
        assertNotContains(out, "na" + I_DIAERESIS + "ve");
    }

    /**
     * a7 — LAW 77: the same literal rendered through the REAL {@code IRGeneration.functionGenerator} seam
     * ({@code -Pir-on}; the IR-route leaf consults the same ONE escape) — the same text.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a7_expressionStringLiteralEscapedIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "functions/StrLit.java");
        assertContains(out, "\"na" + BSU + "00EFve\"");
        assertNotContains(out, "na" + I_DIAERESIS + "ve");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — an ASCII label is byte-unchanged. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_asciiLabelUnchanged() throws IOException {
        assertContains(labelProvider(), "\"plain ASCII label\"");
    }

    /** b2 — the legacy quote/backslash escapes in a label are byte-unchanged. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_legacyEscapesInLabelUnchanged() throws IOException {
        assertContains(labelProvider(), "\"Name with \\\"quotes\\\" and \\\\ backslash\"");
    }

    /** b3 — a JAVADOC (the enum's definition) carrying raw non-ASCII is NOT escaped: the leg lives on the literal emitters only. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_javadocNonAsciiStaysRaw() throws IOException {
        String out = enumKind();
        assertContains(out, "D" + E_ACUTE + "finition with a raw accent in the javadoc");
        assertNotContains(out, "D" + BSU + "00E9finition");
    }

    // =========================================================================
    // Part C — the corpus (drr 7.0.0: the carriers + the whole-cell control)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), LabelUnicodeEscapeSeatTest.class);
    }

    private static final List<String> DRR7_CARRIERS = List.of(
            "drr/regulation/sec/rewrite/trade/labels/SECPPDLabelProvider.java",
            "drr/regulation/sec/rewrite/trade/labels/SECTradeLabelProvider.java");

    /** c1 — SECPPDLabelProvider whole-file lock. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_secPpdLabelProviderByteIdentical() throws IOException {
        lock(DRR7_CARRIERS.get(0));
    }

    /** c2 — SECTradeLabelProvider whole-file lock. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_secTradeLabelProviderByteIdentical() throws IOException {
        lock(DRR7_CARRIERS.get(1));
    }

    /**
     * control0 — golden is the oracle (the frozen drr 7.0.0 tree): exactly 2 files carry a Java
     * {@code \\uXXXX} escape in CODE (1 site each — the two carriers), and ZERO code chars are raw non-ASCII
     * (the 7.0.0 share of the corpus-wide 8 files / 269 comment-only files). The union domain control1
     * compares over is pinned at 2.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenDrr7IsTheOracle() throws IOException {
        EscapeScan g = scan(readGoldenTree(DRR7_GOLDEN));
        assertEquals(2, g.perFile.size(), "golden files carrying an escape or a raw non-ASCII code char");
        assertEquals(2, g.escapeSites(), "golden \\uXXXX escape sites in code");
        assertEquals(0, g.rawNonAscii(), "golden raw non-ASCII code chars");
        for (String carrier : DRR7_CARRIERS) {
            assertTrue(g.perFile.containsKey(carrier), "golden carrier carries the escape: " + carrier);
            assertEquals(1, g.perFile.get(carrier)[0], "one escape site in " + carrier);
        }
    }

    /**
     * control1 — the FORK's WHOLE generated drr 7.0.0 cell (every kind, LAW 72): over the UNION of the files
     * either tree carries an escape site or a raw non-ASCII code char in (a missing side counts as all-zero,
     * LAW 79), the per-file (escape sites, raw non-ASCII code chars) pairs agree FILE BY FILE — an over-fire
     * (a comment emitter escaping, a literal emitter escaping inside the raw band) and an under-fire (a
     * carrier still raw) both fail here; the domain equals golden's 2; the fork carries ZERO raw non-ASCII
     * code chars anywhere in the cell.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_forkDrr7WholeCellEscapeCountsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        EscapeScan f = scan(drr7Output);
        EscapeScan g = scan(readGoldenTree(DRR7_GOLDEN));
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.LinkedHashSet<>(f.perFile.keySet());
        universe.addAll(g.perFile.keySet());
        int[] zero = new int[2];
        for (String key : universe) {
            int[] fc = f.perFile.getOrDefault(key, zero);
            int[] gc = g.perFile.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(gc, fc)) {
                mismatched.add(key + " fork=" + java.util.Arrays.toString(fc)
                        + " golden=" + java.util.Arrays.toString(gc));
            }
        }
        assertEquals(List.of(), mismatched, "escape/raw counts differ from golden in " + mismatched.size() + " file(s)");
        assertEquals(2, universe.size(), "the union domain must equal golden's 2 escape-bearing files");
        assertEquals(0, f.rawNonAscii(), "the fork carries no raw non-ASCII code char anywhere in the cell");
        for (String carrier : DRR7_CARRIERS) {
            assertTrue(f.perFile.containsKey(carrier), "carrier REACHED (carries the escape): " + carrier);
        }
    }

    // =========================================================================
    // The scan — (escape sites, raw non-ASCII chars) per file over CODE only (comments stripped)
    // =========================================================================

    private static final class EscapeScan {
        final Map<String, int[]> perFile = new LinkedHashMap<>();

        int escapeSites() {
            int n = 0;
            for (int[] c : perFile.values()) n += c[0];
            return n;
        }

        int rawNonAscii() {
            int n = 0;
            for (int[] c : perFile.values()) n += c[1];
            return n;
        }
    }

    private static EscapeScan scan(Map<String, String> tree) {
        EscapeScan r = new EscapeScan();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int escapes = unicodeEscapeSites(code);
            int raw = 0;
            for (int i = 0; i < code.length(); i++) {
                char c = code.charAt(i);
                // The RAW band is 0x20..0x7f INCLUSIVE — commons-text's JavaUnicodeEscaper.outsideOf(32, 0x7f),
                // which this seat's u4 unit pin asserts keeps DEL raw. Classifying DEL as "raw non-ASCII"
                // (the earlier `c > 0x7e`) would make control0/control1 — which assert the count is exactly
                // ZERO — fail on output the law says is CORRECT. Zero carriers in this cell either way
                // (measured: the whole golden drr 7.0.0 tree carries no code char at all outside the band
                // — target/seat22-instruments/del-scan.py), so this is a semantic correction, not a count move.
                if (c > 0x7f || (c < 0x20 && c != '\n' && c != '\r' && c != '\t')) {
                    raw++;
                }
            }
            if (escapes > 0 || raw > 0) {
                r.perFile.put(e.getKey(), new int[] { escapes, raw });
            }
        }
        return r;
    }

    /**
     * A Java unicode escape = a {@code u} whose contiguous run of preceding backslashes is ODD.
     *
     * <p>This is JLS 3.3 exactly: a backslash is eligible to begin a unicode escape only when it is
     * preceded by an EVEN number of contiguous backslashes, i.e. when the run ending at the {@code u} has
     * ODD length. Copilot read it the other way on this PR ("in {@code \\\\u2013} the second backslash
     * still begins an active escape"); the COMPILER settles it (LAW 74,
     * {@code target/seat22-instruments/write-jls-probe.py} → {@code JlsProbe}): a source line holding two
     * raw backslashes then {@code u2013} COMPILES and yields a 6-char string beginning {@code 92, 117}
     * (backslash, {@code u}) — no escape was processed — while one backslash yields the single char 8211.
     * Had the second backslash been eligible, the double form would have translated to a backslash + en
     * dash and javac would have rejected it as an illegal escape.
     */
    private static int unicodeEscapeSites(String code) {
        int sites = 0, i = code.indexOf(BSU);
        while (i >= 0) {
            int k = i;
            while (k >= 0 && code.charAt(k) == '\\') k--;
            if ((i - k) % 2 == 1) sites++;
            i = code.indexOf(BSU, i + 2);
        }
        return sites;
    }

    private static Map<String, String> readGoldenTree(Path dir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) stream.filter(q -> q.toString().endsWith(".java"))::iterator) {
                out.put(dir.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    private static String codeOnly(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("/*", i)) {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else if (s.startsWith("//", i)) {
                int end = s.indexOf('\n', i);
                i = end < 0 ? s.length() : end;
            } else if (s.charAt(i) == '"') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != '"') {
                    if (s.charAt(j) == '\\') j++;
                    j++;
                }
                sb.append(s, i, Math.min(j + 1, s.length()));
                i = j + 1;
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // Corpus generation (the same harness every seat suite uses)
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors = new ArrayList<>();

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr7Available()) {
            List<String> errs = new ArrayList<>();
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
            drr7GenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var enumGen = new EnumGenerator(gm);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, pojoGen.generateClasses(model, version, output));
                collect(errors, choiceGen.generateClasses(model, version, output));
                collect(errors, enumGen.generateClasses(model, version, output));
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lock(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr7GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = DRR7_GOLDEN.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 22 F2: the label \\uXXXX escape.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;
    private static Map<String, String> fixtureOutIr;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat22f2.rosetta");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
            mainModel = main;
        }
    }

    private static Map<String, String> render(Predicate<RModel> filter) throws IOException {
        link();
        GeneratorModel gm = new GeneratorModel(linking.workspace(), filter);
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        EnumGenerator eg = new EnumGenerator(gm);
        LabelProviderGenerator lpg = new LabelProviderGenerator(
                gm, tt, new DeepFeatureCallUtil(gm::getType), new LabelProviderGeneratorUtil());
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        eg.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        lpg.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    /** The fixture's functions through the REAL {@code IRGeneration.functionGenerator} seam (the ON route). */
    private static Map<String, String> renderOnIrRoute(Predicate<RModel> filter) throws IOException {
        link();
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            GeneratorModel gm = new GeneratorModel(linking.workspace(), filter);
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
            assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
            Map<String, String> out = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            fg.generateWithErrors(out)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            if (!errors.isEmpty()) {
                throw new AssertionError("fixture generation errors on the IR route: " + errors);
            }
            return out;
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat22f2".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureOutIr == null) {
            fixtureOutIr = renderOnIrRoute(m -> "census.seat22f2".equals(m.namespace()));
        }
        return fixtureOutIr;
    }

    private static String labelProvider() throws IOException {
        return lookup(fixture(), "labels/IngestLabelProvider.java");
    }

    private static String enumKind() throws IOException {
        return lookup(fixture(), "seat22f2/Kind.java");
    }

    private static String function(String fileName) throws IOException {
        return lookup(fixture(), "functions/" + fileName);
    }

    private static String lookup(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "'" + suffix + "' was not generated; keys=" + output.keySet()));
    }

    private static List<RModel> loadBuiltinsOnly() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<String> failures = new ArrayList<>();
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try {
                        models.add(AstBuilder.buildFromFile(p));
                    } catch (Exception e) {
                        failures.add(p + " — " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[LabelUnicodeEscapeSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle),
                "expected needle missing:\n" + needle + "\n--- in output:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertFalse(out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
