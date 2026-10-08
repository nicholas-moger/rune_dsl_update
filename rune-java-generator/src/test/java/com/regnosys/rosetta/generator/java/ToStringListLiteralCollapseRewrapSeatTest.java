package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 32, law D.3 -- facet {@code toStringListLiteralCollapseRewrap}: a {@code to-string}
 * whose ARGUMENT is a single-collapsing list-op over a NON-EMPTY list literal takes the
 * #356 {@code MapperS.of(<collapse>)} re-wrap as its OWN disjunct at
 * {@code ConversionHandler.handle(RToStringExpr)}.
 *
 * <p><b>GOLDEN {@literal <-} FORK</b>, the file's whole diff (2 lines, 2 hunks -- sigs B059 + B019):
 * <pre>
 *   golden  return MapperC.of(MapperS.of(MapperC.&lt;String&gt;of(&lt;a&gt;, &lt;b&gt;)
 *                   .get()).map("to-string", Object::toString));
 *   fork    return MapperC.&lt;String&gt;of(&lt;a&gt;, &lt;b&gt;)
 *                   .get().map("to-string", Object::toString);
 * </pre>
 *
 * <p><b>The mechanism.</b> The #356 gate admits exactly ONE seat -- a SINGLE-LINE collapse
 * under an {@code RKeyValuePair} ctor-setter value -- and this carrier declines on BOTH of
 * those conjuncts: its {@code [<a> first to-string, <b> first to-string] only-element}
 * renders MULTI-LINE and its parent is a conditional ARM. The new disjunct is keyed on the
 * AST SHAPE, not on rendered text: the argument is a single-collapsing list-op over a
 * non-empty {@code RListLiteral}, read through the SHARED
 * {@code HandlerHelper.listLiteralCollapseArgument} the #337 value-returning ladder rung now
 * consults too (LAW 69 -- one walk, two consumers, each passing its own operator set). The
 * meta guard is KEPT and UNWIDENED. The OUTER {@code MapperC.of(...)} half needs no code
 * change: once the arm text begins {@code MapperS.of(}, the {@code MapperC.} render-prefix
 * short-circuit at the arm-lift seat (facet {@code mapperCFactoryArmNoRewrap}) no longer
 * matches -- the same missing wrap {@code PROBE29-F16w} measured on this very file a seat
 * ago ({@code golden=2 gen=1 decidedBy=mapperCPrefix}, four cells, both routes).
 *
 * <p><b>Charter:</b> {@code drr/regulation/common/trade/underlier/reports/
 * NameOfTheUnderlyingIndexRule.java} x drr 7.0.0/7.1.0/7.2.0/7.3.0 POJO -- FOUR WHOLE files
 * (its sig set is exactly {@code B019} + {@code B059}, no import sig). Band 88 -&gt; 84 CLAIMED.
 *
 * <p><b>GREEN SAFETY -- MEASURED, not constructed.</b> The gate's only effect is to wrap an
 * {@code arg} that ENDS {@code .get()}, so the only byte it can move is an emitted
 * {@code <x>.get().map("to-string", }. A read-only walk over every golden of every cell
 * finds that token <b>ZERO</b> times in 174,141 goldens; outside the 88-file band the fork
 * IS golden, so the token exists in the fork exactly FOUR times -- the four carrier cells.
 * The WRAPPED form {@code <x>.get()).map("to-string", } is a 22-file golden domain: 18 GREEN
 * {@code Create_ContractType15__1} (9 drr cells x the hkma dtcc/tr projections, which keep
 * riding the untouched ctor-setter disjunct) plus these 4 carriers. Per drr 7.x cell that is
 * golden 3 files / fork 2 today; per drr 6.34.1-6.38.0 cell it is 2 and 2.
 *
 * <p><b>LAW 77.</b> The to-string render is IR-INHERITED --
 * {@code IRExpressionCompiler.visitToString} falls back to {@code super}, and its IR-DRIVEN
 * leg calls {@code getConversionHandler().handle(toString, ctx, this)} verbatim. BUT
 * {@code IRJavaLeafEmitter} RE-IMPLEMENTS the collapse render including its line breaks, so
 * a newline-keyed admission would be a standing route-divergence risk; this law REMOVES the
 * newline test from the admitted path rather than adding one. {@code corpus_control2} is
 * therefore mandatory and is written BOTH-ROUTES-vs-GOLDEN. (The carrier's fork text is
 * byte-IDENTICAL OFF vs ON in the seat-31 final dumps, all four cells -- measured.)
 *
 * <p><b>RED at the seat's base head (CLAIMED -- measured by the chain), both routes:</b>
 * {@code a1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+
 * {@code corpus_control2} on {@code -Pir-on}). GREEN at RED: {@code e1}, {@code e2},
 * {@code e3}. GREEN at the head: 8/0F/1skip default, 8/0F/0skip {@code -Pir-on}.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-lawD3-*.log}; every set below is the log's, not the
 * draft's):</b>
 * <ul>
 *   <li><b>m1</b> (the {@code recoverExprMetaWrapper(...) == null} guard severed): MEASURED
 *       <b>8/0F/1S -- EMPTY</b>, the claim as this javadoc stated it. It is worth more than a
 *       MATCH, because the LAW-75 probe had REFUTED the claim ({@code verdicts32-D.md}, the
 *       m1 correction): nine GREEN rule definitions ({@code NameOfTheUnderlyingIndex} x8 across
 *       drr 5.61.0's three packages and drr 6.34.1-6.38.0, {@code NameOfTheUnderlyingIndexDTCC}
 *       x1) print {@code endsGet=true innerIsListLiteral=true recoverMeta!=null wrapFired=false}
 *       in the LIVE window, "held out by the meta guard alone". The chain adjudicates between
 *       the two readings: severing the guard moved NO BYTE -- {@code e3} (the drr 6.38.0 meta
 *       form) and {@code corpus_control1}'s cell C (drr 6.38.0, the same population) stayed
 *       GREEN -- because those nine rows are LIVE-window renders whose text is superseded by
 *       the replay-window twin, where the #361 hoist has already fired and the wrap already
 *       applies (the probe's own table: the same eight-plus-one rows again at
 *       {@code replayWindow=true wrapFired=true}). The guard is defence-in-depth at the FILE
 *       level; the probe's "load-bearing" was a predicate-level reading. Kept, for the reason
 *       the original text gives.</li>
 *   <li><b>m2</b> (the {@code RListLiteral} requirement severed): MEASURED 8/1F/1S =
 *       <b>{@code e1}</b> -- exactly the claim; the corpus half EMPTY as the green-safety
 *       measurement said it must be (the bare token sits at four sites corpus-wide, all the
 *       carrier).</li>
 *   <li><b>m3</b> (the law reverted): MEASURED 8/4F/1S = {@code a1}, {@code corpus_c1},
 *       {@code corpus_c2}, {@code corpus_control1} -- the claim exactly on the default route
 *       ({@code corpus_control2} is the {@code -Pir-on} member and skips under the lane's
 *       default profile); {@code e1}, {@code e2}, {@code e3} and cell C GREEN.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b} ({@code f32-red-default.log}): {@code a1},
 * {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+ {@code corpus_control2} on
 * {@code -Pir-on}); GREEN at the head 8/0F/1skip default, 8/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW 81 -- the pinned residue this heal MOVES</b> (re-pinned in the SAME commit, from
 * the control's own print): {@code ThenWrappedDefaultSeatTest}'s {@code KNOWN_RESIDUE_7} row
 * {@code "drr/regulation/common/trade/underlier/reports/NameOfTheUnderlyingIndexRule.java
 * fork=[108, 3, 12] golden=[108, 3, 13]"} -- the heal adds exactly one {@code (MapperS.of(}
 * (its T3) and no {@code >map(} / {@code .map(} at all, so the fork tuple becomes golden's
 * and the ROW IS REMOVED; golden's T1 = 108 keeps the file inside the union domain, so
 * {@code DOMAIN_DRR7} is UNMOVED at 3044.
 *
 * <p><b>LAW 74.</b> The fork's current text does NOT compile:
 * {@code MapperC.<String>of(a, b).get()} evaluates to {@code String} and {@code String} has
 * no {@code map(String, Function)} -- <i>cannot find symbol: method map</i>. Already
 * waivered; this is a NON_COMPILING -&gt; compiling flip, MEASURED ({@code javac32-report.md}
 * section 7.3 row C15): PRE 1 error -> POST 0.
 */
class ToStringListLiteralCollapseRewrapSeatTest {

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

    // =========================================================================
    // The domain: the 22-file golden population of the wrapped collapse-fed to-string
    // =========================================================================

    /** The CARRIER (drr 7.x only; the rule is declared once, in the common namespace). */
    private static final String NOTUI =
            "drr/regulation/common/trade/underlier/reports/NameOfTheUnderlyingIndexRule.java";
    /** The #356 GREENS -- the ctor-setter disjunct's own carriers, in every drr cell. */
    private static final String C15_DTCC =
            "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/Create_ContractType15__1.java";
    private static final String C15_TR =
            "drr/projection/iso20022/hkma/rewrite/trade/tr/functions/Create_ContractType15__1.java";

    /** Cell A = drr 7.0.0 -- carrier + both greens. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.3.0 -- the far end of the carrier's four cells. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell C = drr 6.38.0 -- the GREEN META TWIN: the SAME basename carries the SAME
     * {@code [.., ..] only-element to-string} construct over META navs, and golden hoists it
     * through the #361/#387 family instead. The law must be byte-INERT over this whole cell.
     */
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/drr/drr-6.38.0");
    private static final Path GOLDEN_C = CELL_C_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellCAvailable() {
        return Files.isDirectory(GOLDEN_C);
    }

    static boolean allCellsAvailable() {
        return cellAAvailable() && cellBAvailable() && cellCAvailable();
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    /** GOLDEN's form: the collapse re-wrapped before the to-string map. */
    private static final String WRAPPED = ".get()).map(\"to-string\"";
    /** The FORK's pre-law form: the bare collapsed item consumed by {@code .map}. */
    private static final String BARE = ".get().map(\"to-string\"";

    /**
     * The GOLDEN-DOMAIN pins (LAW 79): the number of files carrying {@link #WRAPPED} or
     * {@link #BARE} in the cell, intersected with what this harness emits.
     *
     * <p>DERIVED by a read-only {@code os.walk} over
     * {@code test-corpus/drr/drr-*}{@code /rosetta-source/src/generated/java} counting both
     * tokens per file, at the seat's base head. The complete result, all ten drr cells:
     * {@code WRAPPED} = 0 files in 5.61.0; 2 files in each of 6.34.1 / 6.35.0 / 6.36.0 /
     * 6.37.0 / 6.38.0 (both {@code Create_ContractType15__1}); 3 files in each of 7.0.0 /
     * 7.1.0 / 7.2.0 / 7.3.0 (the two greens + the carrier) -- 22 files, 22 occurrences, the
     * whole corpus-wide domain. {@code BARE} = 0 in EVERY golden of EVERY cell.
     */
    private static final int DOMAIN_A = 3;
    private static final int DOMAIN_B = 3;
    private static final int DOMAIN_C = 2;

    // =========================================================================
    // Fixtures -- reduced from the carrier's REAL source
    // =========================================================================

    /**
     * Reduced from {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
     * regulation-common-trade-underlier-rule.rosetta:12-43} -- the {@code NameOfTheUnderlyingIndex}
     * rule: a {@code then extract} whose body is an elseless conditional LADDER joining to MULTI
     * (so the block renders {@code mapSingleToList(item -> { ... })} with one {@code return} per
     * rung), whose LAST rung is
     * {@code ([<navA> first to-string, <navB> first to-string] only-element) to-string}
     * (source :41-42), closed by {@code then distinct only-element} (source :43).
     *
     * <p>{@code E1PlainCollapse} keeps every one of those properties EXCEPT the list literal:
     * its collapse is an {@code only-element} over a plain navigation, which is what the new
     * disjunct's {@code RListLiteral} requirement must decline.
     */
    private static final String MODEL = """
            namespace census.seat32d3
            version "1.0.0"

            type Ident32:
                code string (0..1)
                alt string (0..1)

            type Leg32:
                idents Ident32 (0..*)
                name string (0..1)

            type Instr32:
                legA Leg32 (0..1)
                legB Leg32 (0..1)
                legs Leg32 (0..*)
                isOpt boolean (0..1)
                allowed boolean (0..1)

            func IsAllowable32: <"the leading filter - keeps the extract's receiver a MapperS">
                inputs:
                    i Instr32 (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func IsOpt32: <"the IsFRA twin - a bare fn-call rung condition">
                inputs:
                    i Instr32 (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isOpt = True

            reporting rule A1ListLitToString from Instr32: <"a1 - THE NameOfTheUnderlyingIndex SHAPE: a MULTI (MapperC) block ladder whose last rung is a to-string over a LIST-LITERAL only-element collapse">
                filter IsAllowable32
                then extract
                    if legs -> name exists
                    then legs -> name
                    else if legA -> idents -> code exists
                    then legA -> idents -> code
                    else if IsOpt32(item)
                    then ([legA -> idents -> code first to-string, legB -> idents -> alt first to-string] only-element) to-string
                then distinct only-element

            reporting rule E1PlainCollapse from Instr32: <"e1 - the DECLINE lock: the SAME seat with a plain-navigation only-element collapse instead of a list literal; bytes must not move">
                filter IsAllowable32
                then extract
                    if legs -> name exists
                    then legs -> name
                    else if legA -> idents -> code exists
                    then legA -> idents -> code
                    else if IsOpt32(item)
                    then (legB -> idents -> alt only-element) to-string
                then distinct only-element
            """;

    /**
     * a1 -- the heal at fixture grain: the list-literal collapse is re-wrapped
     * {@code MapperS.of(...)} INSIDE the to-string, and the arm lift then adds golden's outer
     * {@code MapperC.of(...)} with no further code change.
     *
     * <p><b>PIN AT RED.</b> If the fixture does not render the bare
     * {@code MapperC.<String>of(<a>, <b>).get().map("to-string", Object::toString)} form at the
     * base head, RESHAPE THE FIXTURE (lengthen the navigations so the collapse breaks lines, or
     * add rungs so the ladder stays a block) -- do NOT weaken the assert. The negative token
     * below is the one that proves the fixture reached the seat at all.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_listLiteralCollapseToStringGainsBothWraps() throws IOException {
        String out = fixtureRule("A1ListLitToStringRule");
        assertContains(out, "MapperC.of(MapperS.of(MapperC.<String>of(");
        assertContains(out, ".get()).map(\"to-string\", Object::toString));");
        assertTrue(!out.contains(".get().map(\"to-string\", Object::toString)"),
                "the bare collapsed to-string form must be gone:\n" + out);
    }

    /**
     * e1 -- the DECLINE lock, and the {@code m2} witness: an {@code only-element} collapse over
     * a plain NAVIGATION (not a list literal) at the very same conditional-arm seat must keep
     * today's bytes. Green at the base head and green at the law's head; RED under {@code m2}.
     *
     * <p>The first assert is the anti-vacuity pin: it also proves the seat was reached, so a
     * fixture that silently rendered some other shape cannot pass this test quietly.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_plainNavCollapseToStringIsLeftAlone() throws IOException {
        String out = fixtureRule("E1PlainCollapseRule");
        assertContains(out, ".get().map(\"to-string\", Object::toString)");
        assertTrue(!out.contains(WRAPPED),
                "a plain-navigation collapse must NOT gain the re-wrap (the list-literal"
                + " requirement is what keeps the #356 gate narrow):\n" + out);
    }

    /**
     * e2 -- the #356 GREEN carriers at corpus grain: the ctor-setter value seat
     * ({@code .setCd(MapperS.of(<chain>.get()).map("to-string", Object::toString).get())}) is a
     * SINGLE-LINE collapse of a plain navigation under an {@code RKeyValuePair}, so it rides
     * the disjunct this law does not touch. Both projections, byte-locked against golden.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void e2_ctorSetterSingleLineCollapseStaysByteIdentical() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        for (String path : List.of(C15_DTCC, C15_TR)) {
            String gen = drrAOutput.get(path);
            assertNotNull(gen, "not generated in drr 7.0.0: " + path);
            assertTrue(gen.contains(WRAPPED),
                    "the #356 green carrier must still carry the wrapped form (else this lock"
                    + " proves nothing about it): " + path);
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(path))), normalize(gen),
                    "the #356 ctor-setter carrier must stay byte-identical: " + path);
        }
    }

    /**
     * e3 -- the GREEN META TWIN, whole-file: drr 6.38.0's {@code NameOfTheUnderlyingIndexRule}
     * carries the SAME {@code [.., ..] only-element} construct under a {@code to-string}, but
     * its elements are META navs, so golden HOISTS the collapse to a wrapper local and
     * null-guard-derefs the value (the #361/#387 family). The meta guard is what reserves that
     * family; this locks the whole file against golden.
     *
     * <p>Golden is asserted FIRST (prove the instrument can fail): the two tokens below are
     * read from golden's own bytes, so a mis-pointed path cannot pass quietly.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void e3_metaListLiteralCollapseKeepsTheHoistForm() throws IOException {
        assertNotNull(drrCOutput, "drr 6.38.0 generation did not run");
        String golden = normalize(Files.readString(GOLDEN_C.resolve(NOTUI)));
        assertTrue(golden.contains(
                        "final FieldWithMetaString fieldWithMetaString2 = MapperC.<FieldWithMetaString>of("),
                "golden 6.38.0 must carry the #387 in-rung collapse hoist");
        assertTrue(golden.contains(
                        "return MapperC.of((fieldWithMetaString2 == null ? MapperS.<String>ofNull()"
                        + " : MapperS.of(fieldWithMetaString2.getValue())).map(\"to-string\","
                        + " Object::toString));"),
                "golden 6.38.0 must consume the hoisted wrapper through the #361 guarded deref");
        assertTrue(!golden.contains(WRAPPED) && !golden.contains(BARE),
                "the meta twin sits OUTSIDE this law's token domain in golden - if that stops"
                + " being true the decline argument has to be re-made");
        String gen = drrCOutput.get(NOTUI);
        assertNotNull(gen, "not generated in drr 6.38.0: " + NOTUI);
        List<String> own = drrCGenErrors.stream().filter(e -> e.contains(NOTUI)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + NOTUI + ": " + own);
        assertEquals(golden, normalize(gen),
                "the drr 6.38.0 meta twin must stay byte-identical - the meta guard is"
                + " load-bearing for the #361/#387 family");
    }

    /** corpus_c1 -- the whole-file heal, carrier cell drr 7.0.0. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700NameOfTheUnderlyingIndexMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(NOTUI)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + NOTUI + ": " + own);
        String gen = drrAOutput.get(NOTUI);
        assertNotNull(gen, "not generated: " + NOTUI);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(NOTUI))), normalize(gen),
                "NameOfTheUnderlyingIndexRule must byte-match golden - seat 32 law D.3: the"
                + " list-literal collapse takes the #356 re-wrap and the arm lift follows");
    }

    /** corpus_c2 -- the same whole-file heal at the far cell drr 7.3.0. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730NameOfTheUnderlyingIndexMatchesGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        String gen = drrBOutput.get(NOTUI);
        assertNotNull(gen, "not generated: " + NOTUI);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(NOTUI))), normalize(gen),
                "NameOfTheUnderlyingIndexRule must byte-match golden in drr 7.3.0 too -"
                + " seat 32 law D.3");
    }

    /**
     * corpus_control1 -- LAW 79, the UNION control over this law's WHOLE golden domain in the
     * three loaded cells. Per file the {@code (WRAPPED, BARE)} pair must equal golden's, over
     * the union of the fork's and golden's token-bearing files intersected with what this
     * harness emits, with NO named residue in either state's expectation -- so the control is
     * RED at the base head (the carrier reads {@code fork=[0, 1] golden=[1, 0]}) and GREEN
     * after, and it fails just as loudly if the law OVER-fires: a green file that gains or
     * loses either token enters the mismatch list AND moves the domain size off its pin.
     *
     * <p>The domain pin is the anti-vacuity half (LAW: a control scans the domain it claims):
     * each cell's golden-domain paths must be INSIDE the emitted key set, and the union size
     * must equal {@link #DOMAIN_A} / {@link #DOMAIN_B} / {@link #DOMAIN_C}.
     *
     * <p>FAIL-CLOSED, at the strength each cell's error set is actually known at: drr 7.0.0 is
     * pinned error-free by two committed suites, so cell A asserts the empty list outright.
     * For drr 7.3.0 and 6.38.0 no committed suite has measured that set, so pinning it here
     * would import an unmeasured claim; those cells instead assert that no generation error
     * names any file in the union domain, which is the property this control actually needs
     * (a dropped domain file cannot hide -- the presence and domain-size pins catch it).
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void corpus_control1_wrappedCollapseDomainEqualsGoldenFileByFile() throws IOException {
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertCellDomain("drr 7.0.0", drrAOutput, drrAGenErrors, GOLDEN_A, DOMAIN_A);
        assertCellDomain("drr 7.3.0", drrBOutput, drrBGenErrors, GOLDEN_B, DOMAIN_B);
        assertCellDomain("drr 6.38.0", drrCOutput, drrCGenErrors, GOLDEN_C, DOMAIN_C);
    }

    /**
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN and MANDATORY for this law:
     * {@code IRJavaLeafEmitter} re-implements the ONLY_ELEMENT collapse render (its link is
     * {@code "."} and its method {@code get}, against the {@code "\n\t."}/{@code first} of the
     * other collapses), which is the very render the #356 gate reads. The healed carrier and
     * BOTH untouched greens must be golden-identical on the IR route too.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForCarrierAndGreens() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        for (String path : List.of(NOTUI, C15_DTCC, C15_TR)) {
            String gen = irOut.get(path);
            assertNotNull(gen, "not generated on the IR route: " + path);
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(path))), normalize(gen),
                    "IR route vs GOLDEN: " + path);
        }
    }

    // =========================================================================
    // The union instrument
    // =========================================================================

    private static void assertCellDomain(String cell, Map<String, String> fork,
            List<String> genErrors, Path goldenRoot, int expectedDomain) throws IOException {
        assertNotNull(fork, cell + " generation did not run");
        Map<String, int[]> forkScan = scan(fork);
        Map<String, int[]> goldenScan = scanGoldenTree(goldenRoot);

        Set<String> universe = new TreeSet<>(forkScan.keySet());
        universe.addAll(goldenScan.keySet());
        universe.retainAll(fork.keySet());

        for (String key : universe) {
            List<String> own = genErrors.stream().filter(e -> e.contains(key)).toList();
            assertTrue(own.isEmpty(),
                    cell + ": generation errors for a domain file " + key + ": " + own);
        }
        for (String key : goldenScan.keySet()) {
            assertTrue(fork.containsKey(key),
                    cell + ": the golden-domain file " + key + " is NOT emitted by this"
                    + " harness, so this control proves nothing about it");
        }

        List<String> mismatched = new ArrayList<>();
        int[] zero = new int[2];
        for (String key : universe) {
            int[] f = forkScan.getOrDefault(key, zero);
            int[] g = goldenScan.getOrDefault(key, zero);
            if (f[0] != g[0] || f[1] != g[1]) {
                mismatched.add(key + " fork=[" + f[0] + ", " + f[1] + "]"
                        + " golden=[" + g[0] + ", " + g[1] + "]");
            }
        }
        assertEquals(List.of(), mismatched,
                cell + ": (wrapped, bare) collapse-fed to-string counts differ from golden in "
                + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                cell + ": the union domain must equal the pinned golden-domain size");
    }

    /** (WRAPPED, BARE) per file; files carrying neither drop out of the scan. */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String text = normalize(e.getValue());
            int wrapped = count(text, WRAPPED);
            int bare = count(text, BARE);
            if (wrapped + bare > 0) {
                out.put(e.getKey(), new int[] {wrapped, bare});
            }
        }
        return out;
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    /**
     * The same {@link #scan} over a golden TREE, counting file by file rather than
     * materialising the whole tree: these cells hold ~7,800 goldens each and three of them are
     * scanned in one run, so holding every file's text at once buys nothing here (the scan
     * keeps only the token-bearing entries anyway).
     */
    private static Map<String, int[]> scanGoldenTree(Path root) throws IOException {
        Map<String, int[]> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> {
                String text;
                try {
                    text = normalize(Files.readString(p));
                } catch (IOException e) {
                    throw new AssertionError("golden read failed: " + p, e);
                }
                int wrapped = count(text, WRAPPED);
                int bare = count(text, BARE);
                if (wrapped + bare > 0) {
                    out.put(root.relativize(p).toString().replace('\\', '/'),
                            new int[] {wrapped, bare});
                }
            });
        }
        return out;
    }

    // =========================================================================
    // Fixture harness (the InLambdaBoolHoistShadowSeatTest rule renderer, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat32d3.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat32d3".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(main, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fixtureRule(String ruleName) throws IOException {
        Render r = render();
        String path = ruleName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith(path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[ToStringListLiteralCollapseRewrapSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> drrCOutput;
    private static List<String> drrCGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
        }
        if (cellCAvailable()) {
            List<String> errs = new ArrayList<>();
            drrCOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.38.0", CELL_C_ROOT), errs);
            drrCGenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    private static Map<String, String> generateCellOnIrRoute(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator");
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
                }
            }
            collect(errors, funcGen.generateWithErrors(output));
            return output;
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
