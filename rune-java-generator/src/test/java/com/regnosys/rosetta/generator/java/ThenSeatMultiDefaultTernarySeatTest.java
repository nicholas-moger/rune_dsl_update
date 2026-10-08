package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * v3.1 LADDER RETIREMENT — flip seat 5: the MULTI-cardinality {@code default}
 * at the then-seat renders upstream's list-form ternary with the joined-type
 * coercion, instead of the non-compiling {@code getOrDefault(Mapper)} legacy
 * fallthrough.
 *
 * <p><b>The measured defect (the LAW-65 content dump over drr 7.0.0 FUNCTION,
 * 2026-08-17, post-#574 — {@code target/seat5-charter.md}):</b> upstream
 * (vendored ExpressionGenerator.xtend binaryExpr case "default") compiles both
 * operands against the JOINED type and renders a multi default as
 * {@code left.getMulti().isEmpty() ? right : left}, inserting
 * {@code .<X>map("Type coercion", fieldWithMetaX -> fieldWithMetaX.getValue())}
 * on any arm whose element is a meta wrapper over the bare joined element. The
 * fork's ternary arms are alias-scoped (#345) / elided-left / ctor-field
 * (#365), so a then-BASE multi default ({@code X default Y then …} —
 * {@code GetUniqueTransactionIdentifier} {@code set uti},
 * {@code UniqueSwapIdentifierForValuation} {@code set usi}) fell to the legacy
 * {@code left.getOrDefault(right)} — a Mapper into the T-typed overload,
 * non-compiling, banded. Two adjacent gaps ride the same seat: (a)
 * {@code chainProvesMulti} had no LIST-LITERAL arm, so the alias-scoped
 * FXLeg1/2 pair ({@code [chain, chain] default [chain, chain]} inside
 * {@code alias leg1Currency:}) failed the #345 both-strict proof and kept the
 * legacy form; (b) the #345 arm itself never coerced a meta-elemented arm over
 * a bare join ({@code GetUniqueTransactionIdentifier}'s
 * {@code utiFromReportableInformation} alias body — the ternary fired, the
 * {@code FieldWithMetaString} arm stayed un-coerced, a generics mismatch
 * against the {@code MapperC<String>} return).
 *
 * <p><b>Green-safety (the #325/#345 argument, verbatim):</b> every rung fires
 * only where both operands prove multi and/or a meta/bare element mismatch is
 * proven — in every such case the legacy render never compiled
 * ({@code getOrDefault(Mapper)}; an un-coerced mixed ternary at a typed seat),
 * so no green file carries it. Monotone add-only.
 *
 * <p><b>Test geometry (the seat-1/2/3/4 pattern):</b> same-workspace controls
 * (RED before the seat — the defect is namespace-independent: a missing form
 * arm, not a resolution gap) + same-workspace inert pins (GREEN before AND
 * after) + drr 7.0.0 corpus locks (RED before the seat; two WHOLE-FILE locks —
 * the carriers' entire diff is this seat).
 */
class ThenSeatMultiDefaultTernarySeatTest {

    private static final Path BUILTINS_DIR =
            Path.of("../test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean builtinsAvailable() {
        return Files.isDirectory(BUILTINS_DIR);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), ThenSeatMultiDefaultTernarySeatTest.class);
    }

    // =========================================================================
    // Control model (same-workspace — the defect is namespace-independent)
    // =========================================================================

    /**
     * The corpus shapes at unit grain. {@code Ident.scheme} carries
     * {@code [metadata scheme]} so its nav leaf surfaces
     * {@code FieldWithMetaString} — the mixed-meta join driver.
     */
    private static final String CONTROL_MODEL = """
            namespace census.seat5
            version "1.0.0"
            type Sub:
                id string (1..1)
                    [metadata scheme]
            type Ident:
                scheme string (1..1)
                    [metadata scheme]
                plain string (0..1)
                subs Sub (1..*)
            type Holder:
                idents Ident (1..*)
                ident Ident (0..1)
                note string (0..1)
                note2 string (0..1)
                tags string (0..*)
                labels string (0..*)
                flagOpt boolean (0..1)
            func FnMulti:
                inputs:
                    holder Holder (1..1)
                output:
                    out string (0..*)
                add out: holder -> note
            func ThenBaseMixed:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (1..1)
                set result:
                    FnMulti(holder)
                        default (holder -> idents
                            then filter scheme exists
                            then extract scheme)
                        then distinct
                        then only-element
            func AliasListLit:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..*)
                alias pair:
                    [holder -> note, holder -> note2]
                        default [holder -> idents -> plain, holder -> note]
                        then filter item exists
                add result: pair
            func AliasMixed:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..*)
                alias ids:
                    FnMulti(holder) default holder -> idents -> scheme
                add result: ids
            func ChainedDefault:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (1..1)
                alias aOne:
                    FnMulti(holder)
                alias bTwo:
                    holder -> idents -> scheme
                alias cThree:
                    holder -> idents -> scheme
                set result:
                    ((aOne default bTwo) default cThree)
                        then distinct only-element
            func ThenBaseSingleRight:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (1..1)
                set result:
                    FnMulti(holder)
                        default (holder -> ident
                            then filter scheme exists
                            then extract subs -> id)
                        then distinct
                        then only-element
            func SingleDefault:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (1..1)
                set result:
                    holder -> note default holder -> note2
            func AliasSameTyped:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..*)
                alias both:
                    holder -> tags default holder -> labels
                add result: both
            func StatementSeat:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..*)
                add result:
                    FnMulti(holder) default holder -> tags
            func LiteralDefault:
                inputs:
                    holder Holder (1..1)
                output:
                    result boolean (1..1)
                set result:
                    holder -> flagOpt default False
            func BareListLit:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..*)
                add result:
                    [holder -> note, holder -> note2]
            """;

    // =========================================================================
    // Render harness (the seat-1/2/3/4 pattern)
    // =========================================================================

    private record Rendered(Map<String, String> functions) {}

    private static Rendered render(String... sources) {
        List<RModel> models = new ArrayList<>();
        try (var stream = Files.walk(BUILTINS_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta")).sorted()
                    .forEach(p -> models.add(AstBuilder.buildFromFile(p)));
        } catch (IOException e) {
            throw new AssertionError("builtins walk failed: " + e.getMessage(), e);
        }
        for (int i = 0; i < sources.length; i++) {
            models.add(AstBuilder.buildFromString(sources[i], "seat5-control-" + i + ".rosetta"));
        }
        var workspace = RWorkspace.build(models).workspace();
        var gm = new GeneratorModel(workspace);
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> functions = new LinkedHashMap<>();
        assertNoGenerationErrors(funcGen.generateWithErrors(functions));
        return new Rendered(functions);
    }

    private static Rendered controls;

    @BeforeAll
    static void renderControls() {
        if (builtinsAvailable()) {
            controls = render(CONTROL_MODEL);
        }
    }

    private static String fn(Rendered rendered, String path) {
        String content = rendered.functions().get(path);
        assertNotNull(content, "missing generated function: " + path
                + " (got: " + rendered.functions().keySet() + ")");
        return content;
    }

    /** Occurrence count — python {@code str.count} semantics (the #352 law). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            n++;
            idx += needle.length();
        }
        return n;
    }

    // =========================================================================
    // Part A — same-workspace controls (RED before the seat; the PRE forms
    // probe-recorded before any assertion was finalized)
    // =========================================================================

    /**
     * The UniqueSwapIdentifierForValuation FORM+ELEMENT shape: a then-BASE
     * mixed-meta default (multi fn-call LEFT over a meta-leaf extract RIGHT)
     * renders the list-form ternary with the joined-element coercion, and the
     * decl takes the bare join (PRE: {@code final MapperC<FieldWithMetaString>
     * thenArg2 = MapperC.<String>of(fnMulti.evaluate(holder)).getOrDefault(
     * thenArg1…)} — a Mapper into the T-typed overload, non-compiling).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void thenBaseMixedDefault_rendersTernaryWithCoercion() {
        String gen = fn(controls, "census/seat5/functions/ThenBaseMixed.java");
        assertEquals(1, count(gen,
                "final MapperC<String> thenArg2 = MapperC.<String>of(fnMulti.evaluate(holder))"
                        + ".getMulti().isEmpty() ? thenArg1"),
                "the then-base multi default declares the bare join and renders "
                        + "the ternary (PRE 0 — the getOrDefault legacy over a "
                        + "FieldWithMetaString decl)");
        assertEquals(1, count(gen,
                ".<String>map(\"Type coercion\", fieldWithMetaString -> "
                        + "fieldWithMetaString.getValue()) : MapperC.<String>of("
                        + "fnMulti.evaluate(holder));"),
                "the meta-elemented RIGHT arm derefs to the bare join and the "
                        + "LEFT repeats as the else arm (PRE 0)");
        assertEquals(0, count(gen, "getOrDefault"),
                "the non-compiling getOrDefault(Mapper) legacy must be gone (PRE 1)");
        assertEquals(0, count(gen, "MapperC<FieldWithMetaString> thenArg2"),
                "the #144 wrapper re-leak on the decl must be gone (PRE 1 — the "
                        + "arm's MapperC<String> stamp drives the #391 gate)");
    }

    /**
     * The UniqueSwapIdentifierForValuation WRAPPER shape (the corpus byte-shape
     * at unit grain): the RIGHT is a single-collapsed then-chain whose extract
     * body is a two-hop nav over the per-element item — the walk cannot prove
     * it, and the seat-5 OR-join proves the default through the multi LEFT
     * alone (PRE: {@code final MapperS<FieldWithMetaString> thenArg2 = …
     * .getOrDefault(thenArg1\n.mapSingleToList(…))} — the MapperS decl over the
     * list-producing RHS).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void thenBaseSingleCollapsedRight_orJoinDeclaresMapperC() {
        String gen = fn(controls, "census/seat5/functions/ThenBaseSingleRight.java");
        assertEquals(1, count(gen,
                "final MapperC<String> thenArg2 = MapperC.<String>of(fnMulti.evaluate(holder))"
                        + ".getMulti().isEmpty() ? thenArg1"),
                "the OR-join proves the default through the multi LEFT and the "
                        + "decl reads MapperC of the bare join (PRE 0 — "
                        + "MapperS<FieldWithMetaString> + getOrDefault)");
        assertEquals(1, count(gen,
                ".mapSingleToList(item -> item.<Sub>mapC(\"getSubs\", ident -> "
                        + "ident.getSubs()).<FieldWithMetaString>map(\"getId\", sub -> "
                        + "sub.getId())).<String>map(\"Type coercion\", "
                        + "fieldWithMetaString -> fieldWithMetaString.getValue()) : "
                        + "MapperC.<String>of(fnMulti.evaluate(holder));"),
                "the re-rooted single-collapsed RIGHT arm keeps its render and "
                        + "takes the refs-recovered coercion (PRE 0)");
        assertEquals(0, count(gen, "MapperS<FieldWithMetaString> thenArg2"),
                "the single-read MapperS decl must be gone (PRE 1)");
        assertEquals(0, count(gen, "getOrDefault"),
                "the legacy form must be gone (PRE 1)");
    }

    /**
     * The FXLeg1/2 shape: an ALIAS-scoped default of two 2-element LIST
     * LITERALS — the #345 arm fires once {@code chainProvesMulti} proves the
     * literals (the seat-5 list-literal rung); same-typed, so NO coercion
     * (PRE: {@code MapperC.<String>of(…).getOrDefault(MapperC.<String>of(…))}).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void aliasListLiteralDefault_rendersTernaryNoCoercion() {
        String gen = fn(controls, "census/seat5/functions/AliasListLit.java");
        assertEquals(1, count(gen, ".getMulti().isEmpty() ? MapperC.<String>of("),
                "the list-literal operands prove multi and the #345 ternary "
                        + "fires (PRE 0 — the getOrDefault legacy)");
        assertEquals(0, count(gen, ".getOrDefault("),
                "the non-compiling getOrDefault(MapperC) must be gone (PRE 1)");
        assertEquals(0, count(gen, "Type coercion"),
                "a same-typed join takes NO coercion");
    }

    /**
     * The GetUniqueTransactionIdentifier utiFromReportableInformation shape:
     * the #345 alias-body arm ALREADY fired pre-seat (ternary present) but the
     * meta-elemented RIGHT arm stayed un-coerced — a generics mismatch against
     * the alias's bare-joined {@code MapperC<String>} signature (the same
     * walk's own return type).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void aliasMixedDefault_existing345ArmGainsCoercion() {
        String gen = fn(controls, "census/seat5/functions/AliasMixed.java");
        assertEquals(1, count(gen,
                ".<FieldWithMetaString>map(\"getScheme\", ident -> ident.getScheme())"
                        + ".<String>map(\"Type coercion\", fieldWithMetaString -> "
                        + "fieldWithMetaString.getValue()) : MapperC.<String>of("
                        + "fnMulti.evaluate(holder));"),
                "the meta RIGHT arm derefs to the bare join inside the #345 "
                        + "ternary (PRE 0 — the arm rendered un-coerced)");
        assertEquals(0, count(gen,
                ".<FieldWithMetaString>map(\"getScheme\", ident -> ident.getScheme()) "
                        + ": MapperC.<String>of(fnMulti.evaluate(holder));"),
                "the un-coerced arm tail must be gone (PRE 1)");
    }

    /**
     * The GetUniqueTransactionIdentifier {@code set uti} shape: the CHAINED
     * then-base default {@code (A default B) default C} — both claimed by the
     * seat arm; the nested LEFT re-renders PARENTHESIZED at its receiver and
     * else-arm positions and every meta arm coerces (PRE:
     * {@code aOne(holder).getOrDefault(bTwo(holder)).getOrDefault(cThree(holder))}).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void chainedThenBaseDefault_rendersNestedParenthesizedTernary() {
        String gen = fn(controls, "census/seat5/functions/ChainedDefault.java");
        assertEquals(1, count(gen,
                "final MapperC<String> thenArg = (aOne(holder).getMulti().isEmpty() ? "
                        + "bTwo(holder).<String>map(\"Type coercion\", fieldWithMetaString"
                        + " -> fieldWithMetaString.getValue()) : aOne(holder))"
                        + ".getMulti().isEmpty() ? cThree(holder)"),
                "the inner default renders parenthesized as the outer's receiver "
                        + "(PRE 0 — chained getOrDefault calls)");
        assertEquals(1, count(gen, " : (aOne(holder).getMulti().isEmpty() ? "),
                "the inner repeats parenthesized as the outer's else arm (PRE 0)");
        assertEquals(0, count(gen, ".getOrDefault("),
                "both legacy getOrDefault calls must be gone (PRE 2)");
    }

    // =========================================================================
    // Part B — same-workspace inert pins (GREEN before AND after; the
    // probe-recorded PRE forms, frozen)
    // =========================================================================

    /**
     * LAW 60's untouched-arm control: a SINGLE default keeps the reduced
     * {@code getOrDefault(<bare>.get())} form — no operand proves multi, the
     * seat arm never fires.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_singleDefaultKeepsGetOrDefault() {
        String gen = fn(controls, "census/seat5/functions/SingleDefault.java");
        assertEquals(1, count(gen,
                ".getOrDefault(MapperS.of(holder).<String>map(\"getNote2\", "
                        + "_holder -> _holder.getNote2()).get());"),
                "the single default keeps the reduced legacy form (frozen)");
        assertEquals(0, count(gen, ".getMulti().isEmpty()"),
                "no ternary at the single seat (frozen)");
    }

    /**
     * The adjacent-arm control (the MapCreditIndex shape): an alias-scoped
     * same-typed multi default keeps the EXACT #345 render — no coercion, no
     * text movement.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_aliasSameTypedKeepsExact345Render() {
        String gen = fn(controls, "census/seat5/functions/AliasSameTyped.java");
        assertEquals(1, count(gen,
                "return MapperS.of(holder).<String>mapC(\"getTags\", _holder -> "
                        + "_holder.getTags()).getMulti().isEmpty() ? MapperS.of(holder)"
                        + ".<String>mapC(\"getLabels\", _holder -> _holder.getLabels()) : "
                        + "MapperS.of(holder).<String>mapC(\"getTags\", _holder -> "
                        + "_holder.getTags());"),
                "the #345 same-typed ternary is byte-frozen");
        assertEquals(0, count(gen, "Type coercion"),
                "a same-typed join takes NO coercion (frozen)");
    }

    /**
     * The adversarial placement pin: the STATEMENT-seat multi default (no
     * enclosing then — the FilterChangePriceQuantity class, whose golden is a
     * DIFFERENT if/else-restructure form) stays DECLINED on the legacy render.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_statementSeatDefaultStaysDeclined() {
        String gen = fn(controls, "census/seat5/functions/StatementSeat.java");
        assertEquals(1, count(gen,
                "result.addAll(MapperC.<String>of(fnMulti.evaluate(holder))"
                        + ".getOrDefault(MapperS.of(holder).<String>mapC(\"getTags\", "
                        + "_holder -> _holder.getTags())).getMulti());"),
                "the statement-seat multi default keeps its legacy render "
                        + "(frozen — the seat gate admits only the then-argument "
                        + "position)");
        assertEquals(0, count(gen, ".getMulti().isEmpty()"),
                "no ternary at the statement seat (frozen)");
    }

    /** The #218 scalar-literal arm, untouched. */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_scalarLiteralDefaultKeepsBareArg() {
        String gen = fn(controls, "census/seat5/functions/LiteralDefault.java");
        assertEquals(1, count(gen, ".getOrDefault(false);"),
                "the scalar-literal default keeps the bare-T overload (frozen)");
        assertEquals(0, count(gen, ".getMulti().isEmpty()"),
                "no ternary at the literal seat (frozen)");
    }

    /**
     * The list-literal rung's blast check: a green 2-element literal OUTSIDE
     * any default keeps its bytes — the rung only feeds proof consults, never
     * a render directly.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_bareListLiteralKeepsBytes() {
        String gen = fn(controls, "census/seat5/functions/BareListLit.java");
        assertEquals(1, count(gen,
                "result.addAll(MapperC.<String>of(MapperS.of(holder).<String>map("
                        + "\"getNote\", _holder -> _holder.getNote()), MapperS.of(holder)"
                        + ".<String>map(\"getNote2\", _holder -> _holder.getNote2()))"
                        + ".getMulti());"),
                "the bare 2-element list literal render is byte-frozen");
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (RED before the seat)
    // =========================================================================

    private static Map<String, String> drr7Functions;

    private static Map<String, String> drr7() throws IOException {
        if (drr7Functions == null) {
            var cell = new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT);
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
            Map<String, String> output = new LinkedHashMap<>();
            assertNoGenerationErrors(funcGen.generateWithErrors(output));
            drr7Functions = output;
        }
        return drr7Functions;
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        String gen = drr7().get(path);
        assertNotNull(gen, "missing generated output: " + path);
        String golden = Files.readString(DRR7_GOLDEN_DIR.resolve(path));
        assertEquals(golden.replace("\r\n", "\n"), gen.replace("\r\n", "\n"),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    /**
     * WHOLE-FILE lock (the seat-3 precedent — the file's ENTIRE pre-seat diff
     * was this seat): the chained then-base default {@code set uti:
     * ((utiFromReportableInformation default utiFromTrade) default
     * utiFromPosition) then distinct only-element} + the alias-body mixed
     * default inside {@code utiFromReportableInformation} (the #345 arm's
     * coercion) — 4 pre-seat diff lines, 0 after.
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_getUniqueTransactionIdentifier_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/uti/functions/GetUniqueTransactionIdentifier.java");
    }

    /**
     * WHOLE-FILE lock: {@code set usi: GetRegimeSpecificIdentifiers(…) default
     * (… then filter … then extract assignedIdentifier -> identifier) then
     * distinct then only-element} — the mixed-meta then-base default whose
     * wrapper needed the OR-join, whose element needed the arm's
     * {@code MapperC<String>} stamp, and whose RIGHT arm needed the
     * refs-recovered coercion — 4 pre-seat diff lines, 0 after.
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_uniqueSwapIdentifierForValuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/uti/functions/UniqueSwapIdentifierForValuation.java");
    }

    /**
     * The alias-scoped list-literal carrier (scoped needles, NOT whole-file —
     * the wildcard-decl / conditional-hoist / meta-key families co-occupy):
     * {@code alias leg1Currency: [chain, chain] default [chain, chain] then …}
     * renders the #345 ternary through the list-literal rung.
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_fxLeg1_listLiteralDefaultRendersTernary() throws IOException {
        String gen = drr7().get("drr/regulation/common/functions/FXLeg1.java");
        assertNotNull(gen, "missing generated output: FXLeg1");
        assertEquals(1, count(gen,
                ".getMulti().isEmpty() ? MapperC.<UnitType>of(fxOptionPayout(product, tradeLot)"),
                "the list-literal default renders the ternary (PRE 0 — the "
                        + "getOrDefault legacy; golden count 1)");
        assertEquals(0, count(gen, ".getOrDefault(MapperC.<UnitType>of(fxOptionPayout"),
                "the non-compiling getOrDefault(MapperC) must be gone (PRE 1)");
    }

    /** The FXLeg2 twin of the list-literal carrier. */
    @Test
    @EnabledIf("drr7Available")
    void drr7_fxLeg2_listLiteralDefaultRendersTernary() throws IOException {
        String gen = drr7().get("drr/regulation/common/functions/FXLeg2.java");
        assertNotNull(gen, "missing generated output: FXLeg2");
        assertEquals(1, count(gen,
                ".getMulti().isEmpty() ? MapperC.<UnitType>of(fxOptionPayout(product, tradeLot)"),
                "the list-literal default renders the ternary (PRE 0; golden count 1)");
        assertEquals(0, count(gen, ".getOrDefault(MapperC.<UnitType>of(fxOptionPayout"),
                "the non-compiling getOrDefault(MapperC) must be gone (PRE 1)");
    }
}
