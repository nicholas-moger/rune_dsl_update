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
 * v3.1 LADDER RETIREMENT — flip seat 3 (companion read): the bare-enum
 * function-ARGUMENT seat qualifies by the EXPECTED (child) enum, the #211/#358
 * flatten law at the third rung ({@code HandlerHelper.boundBareEnumValue} + the
 * #215 SAME-INSTANCE descend-only re-verification), exactly as the ctor-value
 * and {@code default}-RHS seats already do (PR #452).
 *
 * <p><b>The measured defect (the LAW-65 content dump over drr 7.0.0 FUNCTION,
 * 2026-08-17):</b> {@code FilterAssetIdentifier(item, ISIN)} — the Cat-16
 * expected-type bind correctly seeds the callee param's enum
 * ({@code AssetIdTypeEnum extends ProductIdTypeEnum}) and walks the super
 * chain, stamping the {@code REnumValue} instance declared on the PARENT
 * ({@code ISIN} lives on {@code ProductIdTypeEnum}); the render seat
 * ({@code ReferenceHandler.tryBareEnumArg}) declined the stamped shape (its
 * allow-set predated the Cat-16 stamp) and the generic bare-symbol path — before
 * v3.1 flip seat 12 — re-derived the qualifier from {@code ev.parent()}, emitting
 * {@code ProductIdTypeEnum.ISIN} where the golden (upstream flattens inherited
 * values under the CHILD name) has {@code AssetIdTypeEnum.ISIN} (since seat 12
 * that root arm qualifies by the node's INFERRED = expected enum too —
 * {@code HandlerHelper.boundEnumInferredOwner} — so the argument seat is covered
 * twice, by this arm's callee-parameter read and by the root; the two agree by
 * construction, the callee parameter being exactly the parser's position enum
 * for a call argument). Measured (pre-seat-3):
 * {@code GetUnderlyingIdentificationType} (1 site — with the import swap the
 * file's enum diff entirely), {@code GetBasketConstituents} (3 sites).
 *
 * <p><b>Test geometry:</b> the unit-grain RED case (parent-declared value,
 * child-typed param) + the same-instance inert control (child-declared value —
 * the text cannot move) + drr 7.0.0 corpus locks (RED before the seat).
 */
class BareEnumArgExpectedOwnerSeatTest {

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
                && Files.isDirectory(DRR7_GOLDEN_DIR), BareEnumArgExpectedOwnerSeatTest.class);
    }

    /**
     * {@code ChildE extends ParentE}: {@code VAL_ONE} is PARENT-declared (the
     * flatten-law shape — the child-qualified constant exists in generated Java
     * because the child enum flattens inherited values), {@code VAL_THREE} is
     * CHILD-declared (the same-instance inert control).
     */
    private static final String ENUM_MODEL = """
            namespace census.enumseat
            version "1.0.0"
            enum ParentE:
                VAL_ONE
                VAL_TWO
            enum ChildE extends ParentE:
                VAL_THREE
            func TakesChild:
                inputs:
                    marker string (1..1)
                    which ChildE (0..1)
                output:
                    result boolean (0..1)
                set result:
                    which exists
            func CallsWithParentDeclared:
                inputs:
                    marker string (1..1)
                output:
                    result boolean (0..1)
                set result:
                    TakesChild(marker, VAL_ONE)
            func CallsWithChildDeclared:
                inputs:
                    marker string (1..1)
                output:
                    result boolean (0..1)
                set result:
                    TakesChild(marker, VAL_THREE)
            """;

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
            models.add(AstBuilder.buildFromString(sources[i], "seat3b-control-" + i + ".rosetta"));
        }
        var workspace = RWorkspace.build(models).workspace();
        var gm = new GeneratorModel(workspace);
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> functions = new LinkedHashMap<>();
        assertNoGenerationErrors(funcGen.generateWithErrors(functions));
        return new Rendered(functions);
    }

    private static Rendered enums;

    @BeforeAll
    static void renderControls() {
        if (builtinsAvailable()) {
            enums = render(ENUM_MODEL);
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

    /**
     * The flatten-law RED case: a parent-declared value passed bare to a
     * child-typed param qualifies by the EXPECTED child enum (PRE:
     * {@code ParentE.VAL_ONE} — the generic path re-derived the qualifier from
     * the stamped value's declaring parent).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void bareArg_parentDeclaredValue_qualifiesByExpectedChildEnum() {
        String gen = fn(enums, "census/enumseat/functions/CallsWithParentDeclared.java");
        assertEquals(1, count(gen, "ChildE.VAL_ONE"),
                "the bare arg qualifies by the callee param's enum — the #211/#358 "
                        + "flatten law (PRE 0)");
        assertEquals(0, count(gen, "ParentE.VAL_ONE"),
                "the declaring-parent qualification must be gone (PRE 1)");
    }

    /**
     * The same-instance inert control: a child-declared value's stamp IS the
     * param-hierarchy instance and the text cannot move (GREEN before and
     * after).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void bareArg_childDeclaredValue_textUnchanged() {
        String gen = fn(enums, "census/enumseat/functions/CallsWithChildDeclared.java");
        assertEquals(1, count(gen, "ChildE.VAL_THREE"),
                "a child-declared value renders child-qualified before AND after "
                        + "(the same-instance control)");
    }

    // =========================================================================
    // drr 7.0.0 corpus locks (RED before the seat)
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

    /**
     * The single-site carrier: with the owner swap ({@code AssetIdTypeEnum.ISIN}
     * + its import) the file's enum diff heals entirely.
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_getUnderlyingIdentificationType_isinQualifiesByExpectedEnum() throws IOException {
        String gen = drr7().get(
                "drr/regulation/common/functions/GetUnderlyingIdentificationType.java");
        assertNotNull(gen, "missing generated output: GetUnderlyingIdentificationType");
        assertEquals(1, count(gen, "AssetIdTypeEnum.ISIN"),
                "the callee param's enum qualifies the bare ISIN (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "ProductIdTypeEnum"),
                "the declaring-parent qualification and its import are gone "
                        + "(PRE 2 — usage + import / golden 0)");
        assertEquals(1, count(gen, "import cdm.base.staticdata.asset.common.AssetIdTypeEnum;"),
                "the expected enum's import registers (PRE 0 / golden 1)");
    }

    /** The three-site carrier. */
    @Test
    @EnabledIf("drr7Available")
    void drr7_getBasketConstituents_isinSitesQualifyByExpectedEnum() throws IOException {
        String gen = drr7().get("drr/base/trade/basket/functions/GetBasketConstituents.java");
        assertNotNull(gen, "missing generated output: GetBasketConstituents");
        assertEquals(3, count(gen, "AssetIdTypeEnum.ISIN"),
                "all three bare ISIN args qualify by the callee param's enum "
                        + "(PRE 0 / golden 3)");
        assertEquals(0, count(gen, "ProductIdTypeEnum"),
                "the declaring-parent qualification and its import are gone "
                        + "(PRE 4 — three usages + import / golden 0)");
    }

    /**
     * WHOLE-FILE lock FROM THE POST-WIRE MEASUREMENT (the seat-2 pattern): the
     * single-site carrier left the drr 7.0.0 band whole — the ISIN owner swap
     * plus its import, and the file's one other pre-seat diff line (a stray
     * gen-only {@code Underlier} import) healed in the same run under the
     * companion analyzer seat. Four drr 7.0.0 report-rule POJO classes
     * ({@code UnderlierIdentification}-family) also left the band with this
     * rung (169 → 165, zero new).
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_getUnderlyingIdentificationType_byteMatchesGolden() throws IOException {
        String path = "drr/regulation/common/functions/GetUnderlyingIdentificationType.java";
        String gen = drr7().get(path);
        assertNotNull(gen, "missing generated output: " + path);
        String golden = Files.readString(DRR7_GOLDEN_DIR.resolve(path));
        assertEquals(golden.replace("\r\n", "\n"), gen.replace("\r\n", "\n"),
                path + " must byte-match the frozen 9.83.0 golden (the ISIN "
                        + "owner swap + the healed ref-collection import)");
    }
}
