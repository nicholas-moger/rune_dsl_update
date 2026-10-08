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
 * Facet {@code mapitem_ctor_wrap} — four independent upstream sub-laws meeting
 * at the {@code mapItem}/{@code mapSingleToItem} lambda seat (upstream
 * {@code caseMapOperation} compiles the body against
 * {@code MAPPER_S.wrapExtends(bodyItemType)} and the argument against the
 * meta-KEEPING {@code MAPPER_C.wrapExtends}):
 *
 * <ol>
 *   <li><b>MapperS.of ctor wrap</b> — a constructor body compiles to a BARE
 *       item (the POJO builder block, never a Mapper), so the body-seat
 *       coercion wraps it {@code MapperS.of(X.builder()…build())} — guard-free
 *       and hoist-free ({@code itemToWrapper}'s {@code extendsMapper} branch
 *       with no item conversion). The fork arm rides the SAME
 *       {@code wrappedInMapperSOf} factory as the bare-fn-invocation arm
 *       (MAPPER_S ref enters refs atomically with the wrap).</li>
 *   <li><b>+1 tab per lambda nesting level</b> — upstream's Xtend
 *       StringConcatenation prepends the chain-template placeholder line's
 *       leading tab to EVERY continuation line of the embedded lambda code,
 *       compounding per nesting level (golden CompareTradeLot: outer
 *       {@code .mapItem(} at 4 tabs, inner at 5, ctor setters at 6). The fork
 *       re-indents the multi-line EXPRESSION body once per
 *       {@code compileLambda} level (the ConstructionHandler line-238
 *       literal-replace precedent); block bodies keep their pre-baked
 *       depths.</li>
 *   <li><b>implicit-item meta typing</b> — the inline-function implicit item
 *       keeps its META-wrapped element type upstream, so a navigation step off
 *       a meta-element item triggers the EXISTING
 *       {@code coerceNavigationReceiver} gate, emitting the golden inline
 *       {@code .<Value>map("Type coercion", fieldWithMetaX0 -> fieldWithMetaX0
 *       == null ? null : fieldWithMetaX0.getValue())} with guarded REGISTERED
 *       params numbering 0..n-1 per same-name group INSIDE the per-lambda
 *       scope (golden CompareTradeLot: 0/1 in the first mapItem lambda, the
 *       sibling lambda RESTARTS at 0/1 — the #170 computeActualNames
 *       machinery, zero new emission code).</li>
 *   <li><b>closure-param evaluate-arg collapse</b> — a DECLARED closure param
 *       is a Mapper-typed Java variable at runtime; upstream's evaluate-arg
 *       coercion (callee input's item-level expected type) collapses it
 *       wrapperToItem with {@code .get()} (golden {@code unitOfAmount.get()}).
 *       The fork appends {@code .get()} when the structural unwrap returns the
 *       bare param name, gated by the existing
 *       {@code isEnclosingClosureParam} walk.</li>
 * </ol>
 *
 * <p>Green-safety: a bare builder block does not compile against the
 * {@code Function<MapperS<T>, MapperS<F>>} lambda signature, and ZERO of the
 * 34,686 goldens carry {@code .mapItem(<param> -> X.builder()} /
 * {@code .mapSingleToItem(<param> -> X.builder()} for ANY param name
 * (plain-grep verified — ripgrep silently skips the gitignored corpus;
 * 182 + 828 goldens carry the wrapped form with the literal {@code item}
 * param, 186 + 840 across all param names); upstream's embed law always
 * +1-indents multi-line lambda continuations, so no green file can carry the
 * fork's flat form; the meta retype is a strict no-op on non-meta / untyped
 * receivers; and the {@code .get()} append fires only on a bare-name render of
 * a declared closure param in evaluate-arg position (the implicit {@code item}
 * param already collapses through the existing channel). The full 5-cell D11
 * matrix (20/20) is the empirical arbiter.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionMapItemCtorWrapTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(),
                cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    /**
     * ALL FOUR mechanisms in one carrier (cdm5 cell): two sibling mapItem
     * lambdas each wrap their ctor body {@code MapperS.of(…)}, the ctor
     * setters land +2 tabs under the outer chain (one per nesting level), each
     * lambda's two meta-element navigations emit the guarded "Type coercion"
     * unwrap numbered {@code fieldWithMetaNonNegativeQuantitySchedule0/1} —
     * RESTARTING in the sibling lambda — and the trailing
     * {@code unitOfAmount} closure-param evaluate arg collapses to
     * {@code unitOfAmount.get()}.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void compareTradeLotCdm5_allFourMechanisms_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/template/functions/CompareTradeLot.java");
    }

    /**
     * The same four-mechanism carrier on the cdm6 cell — the per-lambda
     * "Type coercion" numbering pool (0/1 then 0/1 again) and the
     * {@code .get()} collapse must reproduce identically against the 6.20.6
     * goldens.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void compareTradeLotCdm6_typeCoercionNumberingPerLambda_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/template/functions/CompareTradeLot.java");
    }

    /**
     * Wrap + nesting indent deep inside nested constructor setter chains
     * (esma drr): the {@code .mapItem(item -> MapperS.of(Schedule10__1.builder()…}
     * body sits at the 7-tab leaf of an assignOutput ctor-setter ladder; its
     * setter lines land exactly +1 tab under the {@code .mapItem(} line, with
     * the closing {@code .build()))} carrying the wrap's extra paren.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getNtnlQtyEsmaDrr_wrapAndNestingIndent_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/esma/emir/refit/trade/functions/GetNtnlQty.java");
    }

    /**
     * The wrap's atomic import channel (esma drr): pre-facet the gen file
     * carried NO {@code MapperS} import (the checkedMap to-enum body never
     * references the class); {@code wrappedInMapperSOf} adds MAPPER_S to refs
     * with the wrap, closing the golden's {@code import
     * com.rosetta.model.lib.mapper.MapperS;} alongside the body bytes.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createFinancialInstitutionSector1EsmaDrr_wrapImportAdd_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/esma/emir/refit/trade/functions/"
                + "Create_FinancialInstitutionSector1__1.java");
    }

    /**
     * TWO wrap sites composing with the EXISTING nested-ctor re-indent (esma
     * drr): each mapItem ctor body contains a nested
     * {@code AmountAndDirection106__2.builder()} ctor whose own +1
     * (ConstructionHandler's literal-replace) now compounds with the lambda
     * level's +1 — golden setters ladder 1/2/3 tabs under the
     * {@code .mapItem(} line.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getNtnlAmtEsmaDrr_twoWrapSitesNestedCtorIndent_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/esma/emir/refit/trade/functions/GetNtnlAmt.java");
    }

    /**
     * The jfsa-cell family carrier (jfsa drr): the same wrap + nested-ctor
     * indent shape against the jfsa report types — the law is
     * jurisdiction-independent.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getNtnlAmt1JfsaDrr_jfsaCellFamily_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/jfsa/rewrite/trade/functions/GetNtnlAmt1.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — the mapItem/mapSingleToItem ctor-body MapperS.of wrap, the "
                + "per-lambda-nesting +1 body indent, the implicit-item meta Type-coercion "
                + "unwrap, or the closure-param evaluate-arg .get() collapse is missing or "
                + "regressed, if the mapitem_ctor_wrap recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
