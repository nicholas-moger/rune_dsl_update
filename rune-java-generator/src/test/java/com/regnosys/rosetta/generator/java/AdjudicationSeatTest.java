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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * v3.2 SEAT 9 (PR #630) — THE F1 / F8 / F13 ADJUDICATION SEAT SUITE: the seat's laws pinned at the
 * generator on BOTH routes, each witness a shape the seat's oracle groups or the chaos cell carry.
 *
 * <p><b>F13 / D48 — the enum value the linker could not bind is REFUSED, never echoed by name.</b>
 * A qualified enum-value reference ({@code Side -> Hold}) whose enumeration resolves but whose VALUE
 * does not used to render {@code Side.HOLD} — a constant the enum does not declare — from
 * {@code ReferenceHandler.handle(REnumValueRef)}'s raw value-name fallback, a NON-COMPILING emission
 * with no refusal and every LOUD counter at 0 (the chaos s17 rival-enum split's p2 {@code C17Sift}
 * and {@code C17BooksC17Agree}, hidden behind the golden-free declaration until the seat's commit-2
 * instrument dumped the fork's side). The twelfth register site
 * {@code SilentDegradation.Site.ENUM_VALUE_NAME_ECHO} REFUSES when the linker's own verdict is on
 * record ({@code GeneratorModel.isReportedUnresolved}, keyed by the reference's range and the value
 * name) — a1 the function seat, a2 the data-rule seat, a3 the rival split the chaos cell carries
 * (mutual wildcard imports, a same-named enum in each half: the fork keeps its per-ELEMENT licence and
 * emits the clean siblings, D48 item 1), a4 the IR route (LAW 77: the IR adapter declines the shape to
 * the same render, ONE site both routes; skipped without the IR provider, RUN on the ON-route seat
 * step). a5 is the positive control: a value the linker binds renders its constant and the counter
 * stays 0. Round 1 (spec SF-1): a6 the DISPATCH seat — a dispatch value the linker reported unresolved
 * ({@code func Route(kind: Side -> Hold)}) is REFUSED at the same site, never echoed into a {@code case}
 * label / variant class name (the seat was unguarded; no chaos carrier). The COUNT arm (a value absent AND unreported — an AST built outside the linker) has no
 * carrier through this harness and is BANKED; the released plugin's verdict on every a-fixture is a
 * whole-invocation refusal ("Couldn't resolve reference to RosettaFeature", the mojo's all-or-nothing
 * gate — {@code target/v32-seat9-instruments/refused-groups/}, local), recorded as the difference.
 *
 * <p><b>F8 / D47 — the two {@code nothing} RENDER laws</b> the D47 type mapping does not reach (the
 * oracle group {@code void-mapping-basic-record-edge}'s UseToken and ConditionCarrierTokPresent, pinned
 * at commit 3 and un-pinned at the render-law commit): upstream's {@code TypeCoercionService} maps an
 * expression whose Java item type is {@code Void} to the EMPTY of the expected type at every coercion
 * seat ({@code addCoercions}: {@code actual.itemType.isVoid → expected.empty}), so (b1) a whole-output
 * {@code set r: t} over a model-declared basic type renders {@code r = null;} and (b2) an {@code exists}
 * whose argument is a {@code Void}-typed feature call renders {@code exists(MapperS.<Void>ofNull())}
 * — the getter chain the fork rendered is DISCARDED by the coercion, not wrapped. The fork's coercer
 * carries the same early exit; the two seats had bypassed it. b3 is b1's IR-route twin (the function seat
 * under the {@code IRGeneration} seam); b4 measures the data-rule seat ONCE for both routes — {@code DataRuleGenerator}
 * is ROUTE-BLIND (not an {@code IREmittableGenerator}, a plain {@code ExpressionCompiler}, as
 * {@code BooleanLiteralOperandNullSafeSeatTest} records), so a2 and b4 run OFF and the existence law's IR witnesses
 * are b6 (InputExists, a function) and {@code VoidOperandExistenceDeclineTest} (round 1, cq MF-2). Round 1 added
 * b7 / b8 (a Void CONDITIONAL operand under {@code exists} and a Void conditional value SET / ADDED — the verdict read
 * from the front end BEFORE any compile: round 1 had compiled the discarded value and drained its hoists, round 2 deleted
 * those compiles on the oracle's verdict, and round 3 extended the fixture with a Void conditional SET into a SEGMENT leaf
 * — the operation seat's third path, the bare {@code .setTok(null)}; the oracle groups void-mapping-render-hoist and
 * void-mapping-segment-conditional-set, pinned first; b7 renamed readAsEmptyBeforeAnyCompile at round 3) and b9 (a META-annotated Void feature under {@code exists} keeps the
 * {@code FieldWithMetaVoid} getter — the {@code hasMeta} guard's witness, the re-pinned edge group's
 * MetaCarrier.TokPresent). The seat suite's fixtures are TWINS of the oracle groups' models, not the models
 * themselves (the edge fixture omits InputAbsent and the Carrier conditions the group carries; the byte bar covers
 * those) — a drift between a twin and its group is a silent gap, named here.
 *
 * <p><b>F1 / D46</b> is locked where its generator lives: {@code EnumGeneratorTest} (+3 at commit 4 —
 * the a5uni shape verbatim, the quote / backslash / tab trio, the synonym) and
 * {@code HoldOutByteCompareTest}'s six un-pinned enum files; not duplicated here (one lock per law).
 *
 * <p><b>THE LANE SET OF RECORD</b> ({@code target/v32-seat9-instruments/lanes-s9.py} — twenty-two exact-string mutations of
 * the committed code, each tested INSIDE its own module (L14 and L20 multi-edit lanes: two edits at once); the generator lanes on ONE
 * uniform target (this suite, the three hold-out bars, {@code TypeCoercionServiceTest}, {@code EnumGeneratorTest} and
 * {@code FunctionConvertToMetaSetTest}); measured WHOLE at the commit-17 head {@code 2e7bdbd65} — {@code scratch/lanes-s9-c17.status} and
 * the per-lane logs, the figures printed to {@code scratch/lanes-c17-figures.txt}, local; LAW 82; the ten seat lanes first at
 * {@code aa27ef95e} (c8), the nineteen at {@code 5f3f716bf} (c12), the twenty at {@code 041febe2f} (c15); every c15 set reproduced
 * at c17 up to b7's RENAME (readAsEmptyBeforeAnyCompile for theDiscardedCompilesHoistsGoWithThem — the same test), L14 re-cut
 * NARROWER and L21 / L22 new; the three DECLARED byte pins mismatch under every mutation - the InLambda edge pin is filtered
 * by the figures instrument, the two round-2 pins are subtracted HERE from its print (round 4, cq NIT-3 / spec NIT-3)):
 * L1 the refusal gate short-circuited → a1, a2, a3; no golden moved
 * L2 the D11 register intersection short-circuited → D11CorpusRegressionTest.cardinality_comparison, D11CorpusRegressionTest.enum_comparison, D11CorpusRegressionTest.function_comparison, D11CorpusRegressionTest.onlyexists_comparison, D11CorpusRegressionTest.pojo_comparison, D11CorpusRegressionTest.typeformat_comparison, D11CorpusRegressionTest.xmeta_comparison; no golden moved
 * L3 the annotation reads the Java-escaped name → EnumGeneratorTest.display_name_non_ascii_raw_in_annotation_escaped_in_ctor, EnumGeneratorTest.display_name_with_quote_backslash_tab_raw_in_annotation, EnumGeneratorTest.synonym_value_non_ascii_raw_in_annotation, ByteCompare; the byte compare enum-display 4, enum-display-edge 2
 * L4 the D47 {@code nothing} verdict filtered at the generator's own type → b1, b5, b7, b9, ByteCompare, CompileGate; the byte compare basic-record 8, basic-record-edge 11 + 2 missing + 2 extra, collapsed-receiver 7, deep-tok 8 + 1 missing + 1 extra, empty-else 2, exists-then-clean 1, render-builder 2, render-edge 9, render-hoist 3, render-hoist-second 1, segment-conditional-set 4, void-meta-output-set 3 + 1 missing + 1 extra + the compile gate basic-record 2, basic-record-edge 1, deep-tok 1, render-edge 1; the deep-tok group moves too
 * L5 the operation arm declines always → b1, b5, b7, ByteCompare, CompileGate; the byte compare basic-record-edge 1, collapsed-receiver 2, empty-else 2, render-builder 2, render-edge 3, render-hoist 2, render-hoist-second 1, segment-conditional-set 1 + the compile gate collapsed-receiver 2, render-builder 1, render-edge 1
 * L6 the existence arm's Void operand nulled → b2, b4, b5, b7, ByteCompare, CompileGate; the byte compare basic-record-edge 1, exists-then-clean 1, render-edge 5, render-hoist 1 + the compile gate exists-then-clean 1, render-hoist 1
 * L7 the IR decline removed → VoidOperandExistenceDeclineTest.voidItemOperand_declinesTheClaim_theSharedHandlerRendersTheEmptyMapper; no golden moved
 * L8 the empty list's item witness reverted → b5, b7, ByteCompare, TypeCoercionServiceTest.emptyValueFor_list_returns_collections_emptyList; the byte compare empty-else 1, render-edge 2, render-hoist 1, render-hoist-second 1
 * L9 the bare with-meta exclusion dropped → GREEN; no golden moved; GREEN since c12 — WITHDRAWN: its c8 witness c1 is pre-empted by the META-OUTPUT decline; its own shape (a with-meta into a plain output) is one of round 2's PARKED groups
 * L10 the law returns false → b1, b2, b4, b5, b7, ByteCompare, CompileGate, TypeCoercionServiceTest.null_type_actual_returns_emptyValueFor; the byte compare basic-record-edge 2, collapsed-receiver 2, empty-else 2, exists-then-clean 1, render-builder 2, render-edge 8, render-hoist 3, render-hoist-second 1, segment-conditional-set 1 + the compile gate collapsed-receiver 2, exists-then-clean 1, render-builder 1, render-edge 1, render-hoist 1
 * L11 the dispatch gate short-circuited → a6; no golden moved
 * L12 the structural meta guard dropped → b9, ByteCompare; the byte compare basic-record-edge 1, deep-tok 1; the deep-tok group moves too — the {@code RDeepFeatureCall} arm's witness
 * L13 the D47 declaration emptied (inside rune-parser) → BuiltinTypeRegistryTest.basicOrRecordNodeType_modelDeclaredBasicOrRecord_isNothing; no golden moved
 * L14 round 1's compile-then-drain re-introduced BEFORE the Void verdict at the existence seat (round 3's re-cut: ONE compile, the round-2 second compile gone) → ByteCompare; the byte compare exists-then-clean 1; the discarded exists operand's compile mints a name the drain does not release, so the clean witness's kept conditional is numbered {@code ifThenElseResult1} — EXACTLY the leak round 2's deletion prevents; the round-2 cut's meta-ladder-alias-rung red (a double compile of every non-Void operand) is GONE
 * L15 the conditional SET verdict declined → b7, ByteCompare; the byte compare empty-else 1, render-hoist 1
 * L16 the plain-value SET path's hoist drain removed → GREEN; no golden moved; GREEN — WITHDRAWN: AddConditional returns from the ADD branch and never reaches this drain
 * L17 the ADD conditional verdict declined → b7, ByteCompare; the byte compare empty-else 1, render-hoist 1, render-hoist-second 1
 * L18 commit 11's with-meta-ARM decline dropped → GREEN; no golden moved; GREEN — WITHDRAWN: the cdm ingest unit witness is pre-empted by the META-OUTPUT decline alone; the PAIR is L20's
 * L19 commit 11's META-OUTPUT decline dropped → CompileGate; no golden moved; RED since c15 on the compile gate's declared-lead contract: the render law's {@code t = null} at the scheme-annotated Void output COMPILES where the pinned SetMetaVoid must not — the round-2 group its witness (GREEN at c12, withdrawn then)
 * L20 commit 11's two declines dropped TOGETHER (a multi-edit lane) → FunctionConvertToMetaSetTest.mapPartyReference_cdm6_reference_byteMatchesGolden, CompileGate; no golden moved; the cdm ingest unit witness red where each decline alone left it green (c12), plus the gate
 * L21 the segment-pathed Void CONDITIONAL consult forced null (round 3 - the operation seat's third path) → b7, ByteCompare; the byte compare segment-conditional-set 1; the fork hoists a Void-typed {@code ifThenElseResult} local + its if/else where the released plugin renders the bare {@code c.setTok(null)} — b7's SegmentConditionalSet assertion and the group's byte compare, EXACTLY the prediction
 * L22 the DISPATCH_PARAM_NOT_FOUND gate short-circuited (round 3) → a7; no golden moved; the parameter path's decline-lock witness (round 2 had added the gate with no lane); a6 stays green on its own ENUM_VALUE_NOT_FOUND gate — EXACTLY the prediction
 * 19 kept, 3 withdrawn (L9, L16, L18).
 * The c6 sweep at {@code 1460e3380} ({@code lanes-s9-c6.status}) is the record of the seat's own catch:
 * every generator lane's set carried the stale bare-{@code emptyList()} unit pin commit 7 moved; the c7 sweep at
 * {@code 44d323815} carried these same ten sets before commit 8's L1 re-anchor (its per-lane logs preserved under
 * {@code scratch/lanes-c7-logs/}).
 */
class AdjudicationSeatTest {

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

    // =========================================================================
    // Fixtures
    // =========================================================================

    /** F13: the function seat and the data-rule seat over an enum value the linker cannot bind. */
    private static final String ECHO = """
            namespace census.seat9echo
            version "0.0.0"

            enum Side: <"Two values; Hold is NOT one of them.">
                Buy
                Sell

            func Pick: <"A qualified enum-value comparand the linker leaves unresolved (ENUM_VALUE_NOT_FOUND).">
                inputs:
                    s Side (1..1)
                output:
                    ok boolean (1..1)
                set ok: s = Side -> Hold

            type Books: <"The data-rule seat over the same unresolved value.">
                side Side (0..1)
                condition Agree:
                    side = Side -> Hold
            """;

    /** F13: the positive control — a value the linker binds. */
    private static final String ECHO_CONTROL = """
            namespace census.seat9echoctl
            version "0.0.0"

            enum Side: <"Two values.">
                Buy
                Sell

            func Pick: <"A qualified enum-value comparand the linker binds.">
                inputs:
                    s Side (1..1)
                output:
                    ok boolean (1..1)
                set ok: s = Side -> Sell

            type Books: <"The data-rule seat over a bound value.">
                side Side (0..1)
                condition Agree:
                    side = Side -> Sell
            """;

    /** F13: the chaos s17 rival-enum split in miniature — p1 owns Buy and Sell, p2's rival owns Buy and Hold. */
    private static final String SPLIT_P1 = """
            namespace census.seat9split.p1
            version "0.0.0"
            import census.seat9split.p2.*

            enum SideEnum: <"First enum - owns Buy and Sell.">
                Buy
                Sell

            func Which: <"Bare enum-value switch guards - the resolver picks the input's enum, not a rival.">
                inputs:
                    s SideEnum (1..1)
                output:
                    r string (1..1)
                set r:
                    s switch
                        Buy then "side-buy",
                        Sell then "side-sell"
            """;

    private static final String SPLIT_P2 = """
            namespace census.seat9split.p2
            version "0.0.0"
            import census.seat9split.p1.*

            func Sift: <"A BARE enum comparand against the pipe item beside a qualified twin whose value the rival lacks.">
                inputs:
                    sides SideEnum (0..*)
                output:
                    buys SideEnum (0..*)
                add buys:
                    sides filter item = Buy
                add buys:
                    sides filter item = SideEnum -> Sell

            func Act: <"A clean sibling in the SAME file - emitted under the per-element licence.">
                inputs:
                    s SideEnum (1..1)
                output:
                    ok boolean (1..1)
                set ok: s = SideEnum -> Buy

            enum SideEnum: <"RIVAL - a same-named enum in an imported namespace whose Buy is the WRONG Buy.">
                Buy
                Hold
            """;

    /**
     * F8: the render-edge and render-builder groups' carriers in miniature — the law's wider arms, each pinned from
     * the released plugin (oracle-s9c): a Void INPUT under exists / is absent / single exists (the IR route's
     * scalar-parameter claim seat), a Void value into a non-Void scalar, a MULTI and a MODEL-typed output, ADDED to a
     * multi output, and into a deep-path setter.
     */
    private static final String VOID_EDGE = """
            namespace census.seat9voidedge
            version "0.0.0"

            basicType etoken <"A model-declared basic type.">

            type Holder: <"A model-typed output target.">
                name string (0..1)

            type Outer: <"A deep-path output target.">
                holder Holder (0..1)

            func InputExists: <"A Void-typed input under exists - the scalar-parameter seat.">
                inputs:
                    t etoken (0..1)
                output:
                    r boolean (1..1)
                set r: t exists

            func InputSingleExists: <"A Void-typed multi input under single exists.">
                inputs:
                    ts etoken (0..*)
                output:
                    r boolean (1..1)
                set r: ts single exists

            func IntoString: <"A Void value into a non-Void scalar output.">
                inputs:
                    t etoken (1..1)
                output:
                    r string (1..1)
                set r: t

            func IntoList: <"A Void list into a MULTI output.">
                inputs:
                    ts etoken (0..*)
                output:
                    rs etoken (0..*)
                set rs: ts

            func AddInto: <"A Void value ADDED to a multi output.">
                inputs:
                    t etoken (1..1)
                output:
                    rs etoken (0..*)
                add rs: t

            func IntoHolder: <"A Void value into a model-typed output - the builder seat.">
                inputs:
                    t etoken (1..1)
                output:
                    h Holder (1..1)
                set h: t

            func IntoDeep: <"A Void value into a model-typed attribute of the output - the setter seat.">
                inputs:
                    t etoken (1..1)
                output:
                    o Outer (1..1)
                set o -> holder: t
            """;

    /** F8: the edge group's two render-law carriers, verbatim in shape. */
    private static final String VOID = """
            namespace census.seat9void
            version "0.0.0"

            basicType etoken <"A model-declared basic type.">

            type ConditionCarrier: <"An expression over a Void-typed attribute.">
                tok etoken (0..1)
                flag boolean (0..1)
                condition TokPresent:
                    if flag = True then tok exists

            func UseToken: <"A function whose input and output are the model-declared basic type.">
                inputs:
                    t etoken (1..1)
                output:
                    r etoken (1..1)
                set r: t
            """;

    // =========================================================================
    // F13 / D48 — the refusal through the linker
    // =========================================================================

    @Test
    @EnabledIf("builtinsAvailable")
    void a1_functionSeat_unresolvedEnumValue_refusedAtEnumValueNameEcho() throws IOException {
        SilentDegradation.reset();
        try {
            Render r = renderSources(List.of(ECHO), "census.seat9echo", "echo", false);
            List<GenerationException> fn = r.functionErrors();
            assertEquals(1, fn.size(), "exactly one function refusal: " + fn);
            assertRefusal(fn.get(0), "census/seat9echo/functions/Pick.java", "Side -> Hold");
            assertTrue(!r.output().containsKey("census/seat9echo/functions/Pick.java"),
                    "the refused function does not emit");
            assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("functions/Pick.java")),
                    "no Pick.java under any path: " + r.output().keySet());
            assertEquals(2, SilentDegradation.counts().getOrDefault(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO, 0),
                    "the site counted once per refusal - the function AND the data rule of this fixture");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a2_dataRuleSeat_unresolvedEnumValue_refusedAtEnumValueNameEcho() throws IOException {
        SilentDegradation.reset();
        try {
            Render r = renderSources(List.of(ECHO), "census.seat9echo", "echo", false);
            List<GenerationException> dr = r.dataRuleErrors();
            assertEquals(1, dr.size(), "exactly one data-rule refusal: " + dr);
            assertRefusal(dr.get(0), "census/seat9echo/validation/datarule/BooksAgree.java", "Side -> Hold");
            assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("BooksAgree.java")),
                    "the refused data rule does not emit: " + r.output().keySet());
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a3_rivalSplit_theOffendingElementRefused_theCleanSiblingsEmitted() throws IOException {
        SilentDegradation.reset();
        try {
            Render r = renderSources(List.of(SPLIT_P1, SPLIT_P2), "census.seat9split", "split", false);
            List<GenerationException> fn = r.functionErrors();
            assertEquals(1, fn.size(), "exactly one refusal over the two halves: " + fn);
            assertRefusal(fn.get(0), "census/seat9split/p2/functions/Sift.java", "SideEnum -> Sell");
            assertTrue(r.dataRuleErrors().isEmpty(), "no data rule in the split: " + r.dataRuleErrors());
            // the per-ELEMENT licence (D48 item 1): the clean siblings of the refused element emit
            String which = pick(r, "p1/functions/Which.java");
            assertContains(which, "SideEnum.BUY");
            assertContains(which, "SideEnum.SELL");
            String act = pick(r, "p2/functions/Act.java");
            assertContains(act, "SideEnum.BUY");
            assertTrue(!r.output().containsKey("census/seat9split/p2/functions/Sift.java"), "Sift refused, not emitted");
            assertEquals(1, SilentDegradation.counts().getOrDefault(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO, 0));
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a4_irRoute_sameRefusalAtTheSameSite() throws IOException {
        SilentDegradation.reset();
        try {
            Render r = renderSources(List.of(SPLIT_P1, SPLIT_P2), "census.seat9split", "split-ir", true);
            List<GenerationException> fn = r.functionErrors();
            assertEquals(1, fn.size(), "exactly one refusal on the IR route: " + fn);
            assertRefusal(fn.get(0), "census/seat9split/p2/functions/Sift.java", "SideEnum -> Sell");
            assertContains(pick(r, "p2/functions/Act.java"), "SideEnum.BUY");
            assertEquals(1, SilentDegradation.counts().getOrDefault(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO, 0));
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a5_control_boundValue_rendersItsConstant_counterZero() throws IOException {
        SilentDegradation.reset();
        try {
            Render r = renderSources(List.of(ECHO_CONTROL), "census.seat9echoctl", "echoctl", false);
            assertTrue(r.functionErrors().isEmpty(), "no refusal: " + r.functionErrors());
            assertTrue(r.dataRuleErrors().isEmpty(), "no refusal: " + r.dataRuleErrors());
            assertContains(pick(r, "functions/Pick.java"), "Side.SELL");
            assertContains(pick(r, "datarule/BooksAgree.java"), "Side.SELL");
            assertEquals(0, SilentDegradation.counts().getOrDefault(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO, 0),
                    "a bound value neither refuses nor counts");
        } finally {
            SilentDegradation.reset();
        }
    }

    // =========================================================================
    // F8 / D47 — the two `nothing` render laws
    // =========================================================================

    private static final String USE_TOKEN_ASSIGN =
            "\t\tprotected Void assignOutput(Void r, Void t) {\n"
            + "\t\t\tr = null;\n"
            + "\t\t\t\n"
            + "\t\t\treturn r;\n"
            + "\t\t}\n";

    private static final String TOK_EXISTS = "return exists(MapperS.<Void>ofNull());";

    @Test
    @EnabledIf("builtinsAvailable")
    void b1_wholeOutputSet_ofAVoidTypedValue_assignsNull() throws IOException {
        Render r = renderSources(List.of(VOID), "census.seat9void", "void", false);
        assertTrue(r.functionErrors().isEmpty(), "no refusal: " + r.functionErrors());
        String use = pick(r, "functions/UseToken.java");
        assertContains(use, USE_TOKEN_ASSIGN);
        assertAbsent(use, "r = t;");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void b2_existsOverAVoidTypedFeature_takesTheEmptyMapper() throws IOException {
        Render r = renderSources(List.of(VOID), "census.seat9void", "void", false);
        assertTrue(r.dataRuleErrors().isEmpty(), "no refusal: " + r.dataRuleErrors());
        String rule = pick(r, "datarule/ConditionCarrierTokPresent.java");
        assertContains(rule, TOK_EXISTS);
        assertAbsent(rule, "map(\"getTok\"");
        assertContains(rule, "import com.rosetta.model.lib.mapper.MapperS;");
    }

    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void b3_irRoute_wholeOutputSet_ofAVoidTypedValue_assignsNull() throws IOException {
        Render r = renderSources(List.of(VOID), "census.seat9void", "void-ir", true);
        assertTrue(r.functionErrors().isEmpty(), "no refusal: " + r.functionErrors());
        String use = pick(r, "functions/UseToken.java");
        assertContains(use, USE_TOKEN_ASSIGN);
        assertAbsent(use, "r = t;");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void b4_dataRuleSeat_routeBlind_existsOverAVoidTypedFeature_takesTheEmptyMapper() throws IOException {
        // round 1 (cq MF-2): the data-rule generator is route-blind, so this is ONE measurement for both routes
        Render r = renderSources(List.of(VOID), "census.seat9void", "void-rb", false);
        assertTrue(r.dataRuleErrors().isEmpty(), "no refusal: " + r.dataRuleErrors());
        String rule = pick(r, "datarule/ConditionCarrierTokPresent.java");
        assertContains(rule, TOK_EXISTS);
        assertAbsent(rule, "map(\"getTok\"");
    }

    private static void assertVoidEdgeRenders(Render r) {
        assertTrue(r.functionErrors().isEmpty(), "no refusal: " + r.functionErrors());
        // the scalar-parameter seat: the compiled operand (MapperS.of(t) / MapperC.<Void>of(ts)) is DISCARDED
        String inputExists = pick(r, "functions/InputExists.java");
        assertContains(inputExists, "r = exists(MapperS.<Void>ofNull()).get();");
        assertAbsent(inputExists, "MapperS.of(t)");
        String single = pick(r, "functions/InputSingleExists.java");
        assertContains(single, "r = singleExists(MapperS.<Void>ofNull()).get();");
        assertAbsent(single, "MapperC");
        // the operation seat: the empty of the TARGET's type
        assertContains(pick(r, "functions/IntoString.java"), "\t\t\tr = null;\n");
        String intoList = pick(r, "functions/IntoList.java");
        assertContains(intoList, "\t\t\trs = Collections.<Void>emptyList();\n");
        assertAbsent(intoList, "rs = ts;");
        String addInto = pick(r, "functions/AddInto.java");
        assertContains(addInto, "\t\t\trs.addAll(Collections.<Void>emptyList());\n");
        assertAbsent(addInto, "singletonList");
        String intoHolder = pick(r, "functions/IntoHolder.java");
        assertContains(intoHolder, "\t\t\th = null;\n");
        assertAbsent(intoHolder, "toBuilder(t)");
        String intoDeep = pick(r, "functions/IntoDeep.java");
        assertContains(intoDeep, "\t\t\to\n\t\t\t\t.setHolder(null);\n");
        assertAbsent(intoDeep, ".setHolder(t)");
    }

    /**
     * THE CONTROL the first cut needed (measured, not argued): a with-meta construction over {@code empty} — the
     * cdm ingest shape {@code set quantityReference: empty with-meta { reference: fpmlHref }} (14 cdm 6.20.2 / drr 7.x
     * functions, {@code MapResolvablePriceQuantityReference} first) — is typed {@code nothing} by the front end (its
     * ARGUMENT's type) but rendered as the meta WRAPPER upstream mints from the context, the argument alone coerced
     * to {@code null} inside; the render law must NOT fire on it. The first cut did (every one of the 14 became
     * {@code x = null;}, {@code scratch/d11-f8r1.status}); {@code HandlerHelper.inferredJavaType} declines the shape.
     * The control reads the {@code assignOutput} body ALONE: its first needle ({@code t = null;} absent from the whole
     * file) matched the {@code evaluate()} null guard every model-typed output carries at the same indent - RED at
     * {@code scratch/t-unit-f8r2.log} over a CORRECT render (the seat's own catch, the needle fixed, never the law).
     */
    private static final String VOID_CONTROL = """
            namespace census.seat9voidctl
            version "0.0.0"

            type Thing: <"A referenced type.">
                name string (0..1)

            func MapRef: <"The cdm ingest shape: a with-meta over empty, into a reference-annotated output.">
                inputs:
                    href string (0..1)
                output:
                    t Thing (0..1)
                        [metadata reference]
                set t: empty with-meta {
                    reference: href
                }
            """;

    @Test
    @EnabledIf("builtinsAvailable")
    void c1_control_withMetaOverEmpty_keepsTheWrapper_theLawDoesNotFire() throws IOException {
        Render r = renderSources(List.of(VOID_CONTROL), "census.seat9voidctl", "voidctl", false);
        assertTrue(r.functionErrors().isEmpty(), "no refusal: " + r.functionErrors());
        String body = assignOutputBody(pick(r, "functions/MapRef.java"));
        assertContains(body,
                "t = toBuilder(ReferenceWithMetaThing.builder().setValue(null).setExternalReference(href).build());");
        assertAbsent(body, "t = null;");
    }

    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void c2_irRoute_control_withMetaOverEmpty_keepsTheWrapper() throws IOException {
        Render r = renderSources(List.of(VOID_CONTROL), "census.seat9voidctl", "voidctl-ir", true);
        assertTrue(r.functionErrors().isEmpty(), "no refusal: " + r.functionErrors());
        String body = assignOutputBody(pick(r, "functions/MapRef.java"));
        assertContains(body,
                "t = toBuilder(ReferenceWithMetaThing.builder().setValue(null).setExternalReference(href).build());");
        assertAbsent(body, "t = null;");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void b5_theWiderArms_inputOperand_multiOutput_add_builder_deepSetter() throws IOException {
        assertVoidEdgeRenders(renderSources(List.of(VOID_EDGE), "census.seat9voidedge", "voidedge", false));
    }

    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void b6_irRoute_theWiderArms_theScalarParameterClaimDeclinesToTheOneSite() throws IOException {
        assertVoidEdgeRenders(renderSources(List.of(VOID_EDGE), "census.seat9voidedge", "voidedge-ir", true));
    }

    // =========================================================================
    // Round 1 - the dispatch seat (spec SF-1), the hoist shapes (cq SF-3 / SF-7), the hasMeta witness (cq SF-4)
    // =========================================================================

    /** Round 2 (cq SF-6): the dispatch PARAMETER the linker leaves unresolved (DISPATCH_PARAM_NOT_FOUND at the dispatch's own range) - the value is never looked up. */
    private static final String DISPATCH_PARAM = """
            namespace census.seat9dispatchparam
            version "0.0.0"

            enum Side: <"Two values.">
                Buy
                Sell

            func Route: <"A dispatch base whose variant names a parameter the base does not declare.">
                inputs:
                    kind Side (1..1)
                output:
                    r string (1..1)

            func Route(kindx: Side -> Buy):
                set r: "buy"
            """;

    /** Round 1: a dispatch value the linker leaves unresolved (ENUM_VALUE_NOT_FOUND at the dispatch's own range). */
    private static final String DISPATCH = """
            namespace census.seat9dispatch
            version "0.0.0"

            enum Side: <"Two values; Hold is NOT one of them.">
                Buy
                Sell

            func Route: <"A dispatch base whose Hold variant the linker leaves unresolved.">
                inputs:
                    kind Side (1..1)
                output:
                    r string (1..1)

            func Route(kind: Side -> Buy):
                set r: "buy"

            func Route(kind: Side -> Hold):
                set r: "hold"
            """;

    /** Round 1: the oracle group void-mapping-render-hoist's three shapes (a twin of the group's model). */
    private static final String VOID_HOIST = """
            namespace census.seat9hoist
            version "0.0.0"

            basicType etoken <"A model-declared basic type.">

            func ExistsOverConditional: <"A Void-typed conditional operand under exists.">
                inputs:
                    flag boolean (1..1)
                    t etoken (0..1)
                    u etoken (0..1)
                output:
                    r boolean (1..1)
                set r: (if flag then t else u) exists

            func SetConditional: <"A Void-typed conditional value into a Void output.">
                inputs:
                    flag boolean (1..1)
                    t etoken (0..1)
                    u etoken (0..1)
                output:
                    r etoken (0..1)
                set r: if flag then t else u

            func AddConditional: <"A Void-typed conditional value ADDED to a Void multi output.">
                inputs:
                    flag boolean (1..1)
                    t etoken (0..1)
                    u etoken (0..1)
                output:
                    rs etoken (0..*)
                add rs: if flag then t else u

            type Carrier: <"A model type carrying a Void-typed attribute.">
                tok etoken (0..1)

            func SegmentConditionalSet: <"A Void conditional value SET into a single-hop SEGMENT leaf - the operation seat's THIRD path (round 3, cq MF-1).">
                inputs:
                    flag boolean (1..1)
                    t etoken (0..1)
                    u etoken (0..1)
                output:
                    c Carrier (1..1)
                set c -> tok: if flag then t else u
            """;

    /** Round 1: a META-annotated Void feature under exists (a twin of the re-pinned edge group's MetaCarrier). */
    private static final String VOID_META = """
            namespace census.seat9voidmeta
            version "0.0.0"

            basicType etoken <"A model-declared basic type.">

            type MetaCarrier: <"Metadata over the model-declared type.">
                tok etoken (0..1)
                    [metadata scheme]
                flag boolean (0..1)
                condition TokPresent:
                    if flag = True then tok exists
            """;

    @Test
    @EnabledIf("builtinsAvailable")
    void a6_dispatchSeat_unresolvedDispatchValue_refusedAtEnumValueNameEcho() throws IOException {
        SilentDegradation.reset();
        try {
            Render r = renderSources(List.of(DISPATCH), "census.seat9dispatch", "dispatch", false);
            List<GenerationException> fn = r.functionErrors();
            assertEquals(1, fn.size(), "exactly one function refusal - the dispatch base: " + fn);
            assertRefusal(fn.get(0), "census/seat9dispatch/functions/Route.java", "Side -> Hold");
            assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("functions/Route.java")),
                    "the refused dispatch base does not emit: " + r.output().keySet());
            assertEquals(1, SilentDegradation.counts().getOrDefault(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO, 0),
                    "the site counted once - the one refused dispatch");
        } finally {
            SilentDegradation.reset();
        }
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a7_dispatchSeat_unresolvedDispatchParameter_refusedAtEnumValueNameEcho() throws IOException {
        // round 2 (cq SF-6): the linker's OTHER unresolved-dispatch path - the parameter not found, the value never
        // looked up - reaches the same gate on its own category (DISPATCH_PARAM_NOT_FOUND on the parameter name)
        SilentDegradation.reset();
        try {
            Render r = renderSources(List.of(DISPATCH_PARAM), "census.seat9dispatchparam", "dispatchparam", false);
            List<GenerationException> fn = r.functionErrors();
            assertEquals(1, fn.size(), "exactly one function refusal - the dispatch base: " + fn);
            GenerationException e = fn.get(0);
            assertTrue(e instanceof SilentDegradation.Refusal, "a register refusal, got " + e);
            assertEquals(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO, ((SilentDegradation.Refusal) e).site(), "the twelfth site: " + e);
            assertEquals("census/seat9dispatchparam/functions/Route.java", e.getTargetPath());
            assertTrue(e.getMessage().contains("DISPATCH_PARAM_NOT_FOUND") && e.getMessage().contains("kindx"),
                    "the refusal names the linker's verdict and the parameter: " + e.getMessage());
            assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("functions/Route.java")),
                    "the refused dispatch base does not emit: " + r.output().keySet());
            assertEquals(1, SilentDegradation.counts().getOrDefault(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO, 0),
                    "the site counted once - the one refused dispatch");
        } finally {
            SilentDegradation.reset();
        }
    }

    private static void assertVoidHoistRenders(Render r) {
        assertTrue(r.functionErrors().isEmpty(), "no refusal: " + r.functionErrors());
        String ex = assignOutputBody(pick(r, "functions/ExistsOverConditional.java"));
        assertContains(ex, "r = exists(MapperS.<Void>ofNull()).get();");
        assertAbsent(ex, "ifThenElseResult");
        String set = assignOutputBody(pick(r, "functions/SetConditional.java"));
        assertContains(set, "r = null;");
        assertAbsent(set, "if (");
        assertAbsent(set, "ifThenElseResult");
        String add = assignOutputBody(pick(r, "functions/AddConditional.java"));
        assertContains(add, "rs.addAll(Collections.<Void>emptyList());");
        assertAbsent(add, "ifThenElseResult");
        // round 2 (the c12 sweep's L17 gap): the distributed arms render the same addAll INSIDE an if - pin its absence
        assertAbsent(add, "if (");
        // round 3 (cq MF-1): the operation seat's THIRD path - a Void conditional SET into a single-hop SEGMENT leaf is
        // the bare `.setTok(null)` (the front-end verdict read BEFORE any compile), never the pathed-conditional's
        // Void-typed `ifThenElseResult` hoist. Rides both routes (b7 default, b8 IR - LAW 77 by the shared renderer).
        String seg = assignOutputBody(pick(r, "functions/SegmentConditionalSet.java"));
        assertContains(seg, ".setTok(null);");
        assertAbsent(seg, "ifThenElseResult");
        assertAbsent(seg, "if (");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    // round 3 (cq SF-6 / rule6 NIT-3): renamed - round 2 DELETED the discarded compiles at the SET / exists seats, so
    // the old name (`theDiscardedCompilesHoistsGoWithThem`) named a mechanism the tree no longer has; the verdict is
    // read from the front end BEFORE any compile now (the assertions below pin the bare `.setTok(null)` / `r = null` /
    // `exists(MapperS.<Void>ofNull())` with no `ifThenElseResult` hoist).
    void b7_conditionalVoidValues_readAsEmptyBeforeAnyCompile() throws IOException {
        assertVoidHoistRenders(renderSources(List.of(VOID_HOIST), "census.seat9hoist", "hoist", false));
    }

    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void b8_irRoute_conditionalVoidValues_theSameRenders() throws IOException {
        assertVoidHoistRenders(renderSources(List.of(VOID_HOIST), "census.seat9hoist", "hoist-ir", true));
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void b9_metaAnnotatedVoidFeature_underExists_keepsTheWrapperGetter() throws IOException {
        Render r = renderSources(List.of(VOID_META), "census.seat9voidmeta", "voidmeta", false);
        assertTrue(r.dataRuleErrors().isEmpty(), "no refusal: " + r.dataRuleErrors());
        String rule = pick(r, "datarule/MetaCarrierTokPresent.java");
        assertContains(rule, "<FieldWithMetaVoid>map(\"getTok\"");
        assertAbsent(rule, "MapperS.<Void>ofNull()");
    }

    // =========================================================================
    // Fixture harness — functions and data rules, both routes
    // =========================================================================

    private record Render(Map<String, String> output, List<GenerationException> functionErrors,
                          List<GenerationException> dataRuleErrors) { }

    private static Render renderSources(List<String> sources, String namespacePrefix, String label,
                                        boolean irRoute) throws IOException {
        List<RModel> models = new ArrayList<>();
        int n = 0;
        for (String source : sources) {
            RModel m = AstBuilder.buildFromString(source, "seat9" + label + "-" + (n++) + ".rosetta");
            m.setVersion("0.0.0.test");
            models.add(m);
        }
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linked = RWorkspace.build(models);
        RWorkspace workspace = linked.workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> m.namespace().startsWith(namespacePrefix));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        String previous = System.getProperty(IRGeneration.PROPERTY);
        if (irRoute) {
            System.setProperty(IRGeneration.PROPERTY, "true");
        }
        try {
            FunctionGenerator fg = irRoute
                    ? IRGeneration.functionGenerator(gm, tt, typeUtil)
                    : new FunctionGenerator(gm, tt, typeUtil);
            if (irRoute) {
                assertNotNull(IRGeneration.providerOrNull(),
                        "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
                assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                        "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
            }
            DataRuleGenerator dataRuleGen = new DataRuleGenerator(gm, tt, typeUtil);
            Map<String, String> out = new LinkedHashMap<>();
            List<GenerationException> dataRuleErrors = new ArrayList<>();
            for (RModel model : workspace.files()) {
                if (gm.shouldGenerate(model)) {
                    dataRuleErrors.addAll(dataRuleGen.generateClasses(model, gm.version(model), out));
                }
            }
            List<GenerationException> functionErrors = new ArrayList<>(fg.generateWithErrors(out));
            return new Render(out, functionErrors, dataRuleErrors);
        } finally {
            if (irRoute) {
                if (previous == null) {
                    System.clearProperty(IRGeneration.PROPERTY);
                } else {
                    System.setProperty(IRGeneration.PROPERTY, previous);
                }
            }
        }
    }

    private static void assertRefusal(GenerationException e, String targetPath, String witnessFragment) {
        assertTrue(e instanceof SilentDegradation.Refusal, "a register refusal, got " + e);
        SilentDegradation.Refusal r = (SilentDegradation.Refusal) e;
        assertEquals(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO, r.site(), "the twelfth site: " + e);
        assertEquals(targetPath, e.getTargetPath());
        assertTrue(e.getMessage().contains(witnessFragment),
                "the refusal names the reference <" + witnessFragment + ">: " + e.getMessage());
        assertTrue(e.getMessage().contains("ENUM_VALUE_NOT_FOUND"),
                "the refusal names the linker's verdict: " + e.getMessage());
        List<String> witnesses = SilentDegradation.witnesses(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO);
        assertTrue(witnesses.stream().anyMatch(w -> w.contains(witnessFragment)),
                "the register's witness list carries the reference: " + witnesses);
    }

    private static String pick(Render r, String suffix) {
        List<String> matches = r.output().keySet().stream()
                .filter(k -> k.endsWith("/" + suffix))
                .sorted()
                .toList();
        assertTrue(!matches.isEmpty(), "not generated: " + suffix + " (have: " + r.output().keySet() + ")");
        assertEquals(1, matches.size(), "exactly ONE emitted file may match " + suffix + ": " + matches);
        return normalize(r.output().get(matches.get(0)));
    }

    /**
     * The {@code assignOutput} method of a rendered function, from its definition (the LAST {@code assignOutput(} -
     * {@code doEvaluate}'s call precedes it) to its closing brace: the operation seat's bytes alone, without the
     * {@code evaluate()} null guard ({@code t = null;} at the same indent) that every model-typed output carries.
     */
    private static String assignOutputBody(String code) {
        // round 1 (cq NIT-6): the slice starts at the LAST `assignOutput(` - the declaration, the call in doEvaluate
        // preceding it - so a function with a second declaration would slice the wrong one: assert the one shape
        int occurrences = 0;
        for (int at = code.indexOf("assignOutput("); at >= 0; at = code.indexOf("assignOutput(", at + 1)) {
            occurrences++;
        }
        assertEquals(2, occurrences, "two occurrences of `assignOutput(` - the call and the declaration - in:\n" + code);
        int start = code.lastIndexOf("assignOutput(");
        assertTrue(start >= 0, "assignOutput defined in:\n" + code);
        int end = code.indexOf("\n\t\t}\n", start);
        assertTrue(end > start, "assignOutput closes in:\n" + code);
        return code.substring(start, end);
    }

    private static void assertContains(String code, String needle) {
        assertTrue(code.contains(needle), "expected <" + needle + "> in:\n" + code);
    }

    private static void assertAbsent(String code, String needle) {
        assertTrue(!code.contains(needle), "did NOT expect <" + needle + "> in:\n" + code);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
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
            throw new AssertionError("[AdjudicationSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }
}
