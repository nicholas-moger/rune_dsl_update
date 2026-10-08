package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Facet {@code filter_predicate_item_typing} — four rendering recoveries that the
 * cdm {@code Filter*} function family exposes, all rooted in the implicit ITEM
 * binding of an inline filter/extract lambda and the Boolean/value coercions its
 * predicate position demands:
 *
 * <ol>
 *   <li><b>Implicit-item lambda var naming.</b> The first navigation step on the
 *       lambda's implicit {@code item} ({@code quantities filter item -> unit = unit})
 *       named its lambda var from the FEATURE with an underscore ({@code _unit ->
 *       _unit.getUnit()}) because {@code NavigationHandler.resolveLambdaVarName}'s
 *       {@code RImplicitVariable} branch only knew the RULE from-type case; the golden
 *       names it from the lambda's ITEM type — the owning list-op argument's item data
 *       type ({@code Quantity} → {@code quantity -> quantity.getUnit()}), scope-
 *       disambiguated exactly like every other type-derived lambda var.</li>
 *   <li><b>Implicit-item attribute reference.</b> A bare attribute reference inside a
 *       filter lambda ({@code partyRoles filter role = partyRoleEnum}) resolved to the
 *       item type's {@code RAttribute} but rendered through the variable path as the
 *       non-compiling bare name {@code MapperS.of(role)}; the golden navigates the
 *       implicit item ({@code item.<PartyRoleEnum>map("getRole", partyRole ->
 *       partyRole.getRole())}). The rule-side implicit-INPUT synthesis
 *       ({@code ReferenceHandler.synthesizeImplicitInputNavigation}) gains the
 *       inline-lambda implicit-ITEM analogue, guarded by the same attribute-identity
 *       check against the item data type.</li>
 *   <li><b>Comparison-operand meta-strip.</b> A comparison operand whose navigation
 *       chain ends on a {@code [metadata]} attribute carries a
 *       {@code Mapper<FieldWithMetaX>} expression type, and upstream compiles every
 *       comparison operand against the meta-stripped join of the operand types — so
 *       the golden appends the Type-coercion deref
 *       ({@code .<String>map("Type coercion", fieldWithMetaString ->
 *       fieldWithMetaString == null ? null : fieldWithMetaString.getValue())}); the
 *       fork rendered the raw wrapper. {@code ComparisonHandler} now meta-strips each
 *       compiled operand via {@code ExpressionCompiler.coerceNavigationReceiver} — the
 *       same single-purpose lever navigation receivers use, dispatching the byte-
 *       validated {@code WrappedItemCoercer} arms (null-guarded MapperS / bare MapperC).</li>
 *   <li><b>Existence predicate Boolean coercion.</b> A filter predicate body that is an
 *       existence check ({@code quantities filter item -> unit -> currency exists})
 *       compiles to a {@code ComparisonResult} — not the {@code Boolean} the
 *       {@code filterItemNullSafe} signature demands — and golden coerces it with
 *       {@code .get()}, exactly as the comparison/logical predicate bodies already do
 *       ({@code CollectionHandler.isComparisonFilterPredicate} /
 *       {@code isLogicalFilterPredicate}); the existence body was the missing arm.</li>
 * </ol>
 *
 * <p>This test is REVERT-VERIFIED RED: reverting any one mechanism reverts its anchors
 * to the divergent shape. Anchors are WHOLE-FILE byte comparisons against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen from the family's
 * sole-mechanism waivered files (census over f-probe-161):
 * <ul>
 *   <li>{@code FilterQuantity} — mechanism 1 alone: {@code quantity ->
 *       quantity.getUnit()} on the implicit-item step;</li>
 *   <li>{@code FilterPartyRole} — mechanism 2: the bare {@code role} predicate renders
 *       as implicit-item navigation;</li>
 *   <li>{@code FilterQuantityByCurrency} — mechanisms 1 + 3: item-type lambda var AND
 *       the meta Type-coercion deref on the {@code currency} comparison operand;</li>
 *   <li>{@code FilterQuantityByCurrencyExists} — mechanisms 1 + 4: item-type lambda var
 *       AND {@code .get()} on the {@code exists(...)} predicate body.</li>
 * </ul>
 */
class FunctionFilterPredicateItemTypingTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm6CellAvailable()) {
            var cell = new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT);
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
            Map<String, String> output = new LinkedHashMap<>();
            List<GenerationException> genErrors = gen.generateWithErrors(output);
            assertTrue(genErrors.isEmpty(),
                    cell + " FUNCTION generation reported errors: " + genErrors);
            cdm6FunctionOutput = output;
        }
    }

    /** Mechanism 1 alone: item-type-derived lambda var on the implicit-item step. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void filterQuantity_itemTypeLambdaVar_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/base/math/functions/FilterQuantity.java");
    }

    /** Mechanism 2: bare item-attribute predicate renders as implicit-item navigation. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void filterPartyRole_implicitItemAttributeNav_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/base/staticdata/party/functions/FilterPartyRole.java");
    }

    /** Mechanisms 1 + 3: item-type lambda var + comparison-operand meta Type-coercion. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void filterQuantityByCurrency_comparisonMetaStrip_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/base/math/functions/FilterQuantityByCurrency.java");
    }

    /** Mechanisms 1 + 4: item-type lambda var + .get() on the exists(...) predicate body. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void filterQuantityByCurrencyExists_existsPredicateGet_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/base/math/functions/FilterQuantityByCurrencyExists.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — an item-type-blind implicit-var branch names the lambda var "
                + "_<feature>, a declined implicit-item synthesis renders the bare "
                + "non-compiling MapperS.of(<attr>), a meta-blind comparison operand drops "
                + "the Type-coercion deref, and an existence predicate body misses the "
                + "Boolean .get() if the respective mechanism is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
