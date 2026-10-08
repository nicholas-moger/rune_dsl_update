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
 * PR #189 (facet {@code convertNullSafe}, slice 3 — the B meta&rarr;value coercion
 * lifted at the WHOLE-RHS SET / conditional-arm seat and the bare-value
 * then-output seat) — the continuation of PR #187/#188's {@code convertNullSafe}
 * slices into the remaining deferred render positions characterized in
 * {@code target/wf187-reversal.json} (local).
 *
 * <p>The law: a SET / conditional-arm / then-output value whose RHS item type is a
 * value-level meta wrapper ({@link com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue}
 * {@code ReferenceWithMetaX} / {@code FieldWithMetaX}) but whose target is the plain
 * value (or the value's builder) hoists the wrapper into a {@code final <Meta> refN =
 * <RHS>;} local and null-guards the {@code .getValue()} deref — exactly the upstream
 * {@code TypeCoercionService.convertNullSafe} statement form:
 * <pre>
 *   final ReferenceWithMetaParty referenceWithMetaParty0 = «rhs».get();
 *   if (referenceWithMetaParty0 == null) {
 *       party = null;
 *   } else {
 *       party = toBuilder(referenceWithMetaParty0.getValue());
 *   }
 * </pre>
 * Pre-fix the fork emitted the bare non-compiling {@code party = toBuilder(«rhs».get())}
 * (a {@code Party.PartyBuilder = ReferenceWithMetaParty.Builder} type mismatch), the
 * bare {@code partyBIC = «rhs».get()} ({@code String = FieldWithMetaString}), or — when
 * two branches set the same target — a DUPLICATE {@code final FieldWithMetaString
 * fieldWithMetaString} local (a non-compiling redeclaration that the
 * {@link com.regnosys.rosetta.generator.java.function.StatementHoistSession} numbering
 * resolves to {@code fieldWithMetaString0/1}).
 *
 * <p>Wiring (all revert-locked by these whole-file anchors):
 * <ul>
 *   <li>{@code FunctionExpressionRenderer.renderMetaValueDerefOrNull} now fires for a
 *       model (builder) output too — the {@code outputNeedsBuilder} blanket decline is
 *       replaced by the narrower metaWit keep-guard ({@code outputTypeName.equals(metaSimple)}
 *       &rarr; the PR #186 output-IS-the-wrapper case keeps the wrapper) — and consumes
 *       {@code toBuilder(refN.getValue())} for a builder output;</li>
 *   <li>the hoisted local var name rides the {@code StatementHoistSession}
 *       per-meta-base-name group: a singleton method keeps the bare
 *       {@code referenceWithMetaParty}; two branches setting the same target number
 *       {@code referenceWithMetaParty0/1} / {@code fieldWithMetaString0/1};</li>
 *   <li>{@code renderThenExtractSet} wires the same deref into the bare then-output
 *       assignment (PartyBIC).</li>
 * </ul>
 *
 * <p>This is the revert-verified lock: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader) and
 * byte-compares WHOLE FILES against the frozen goldens (newline-normalized —
 * fragment assertions are insufficient per the PR #153 lesson).
 *
 * <p>Sub-cases anchored: the {@code toBuilder} whole-RHS deref in a single-arm SET
 * (bare local — {@code Extract_ReportingCounterparty}) and in a two-arm conditional
 * SET (numbered locals — {@code ExtractPartyResponsibleForReporting},
 * {@code ExtractReportSubmittingParty}, {@code RateOption}); the bare-value then-output
 * deref ({@code PartyBIC}); and the bare-value NUMBERED case the generous byte-oracle
 * surfaced beyond the brief ({@code StandardizedScheduleMonetaryNotionalCurrencyFromResolvablePQ}
 * — cdm 6.20.6, two conditional arms, {@code notionalCurrency = fieldWithMetaString0/1.getValue()}).
 *
 * <p>PR #249 (distinct whole-RHS recovery): {@code ExtractPartyFromRelatedPartyByRole}
 * — whose whole-RHS is {@code distinct(...).get()}, erasing the compiled expression
 * type to {@code null} so the {@code getItemType} gate could not see the wrapper — now
 * flips. {@code renderMetaValueDerefOrNull} recovers the terminal nav feature's meta
 * wrapper from the RHS AST via {@code NavigationHandler.tryTerminalMetaMapperType} (now
 * descending the element-preserving {@code DISTINCT} list-op), then the existing
 * builder-output deref fires byte-identically. Green-safe by construction: NO green
 * file carries a whole-RHS-SET {@code distinct(...)} (every such carrier is a meta
 * wrapper assigned bare = a non-compiling, waivered mismatch), and the recovery is a
 * STRICT fallback consulted only when the compiled type erased.
 *
 * <p>The {@code PartyLei} then-OUTPUT path this note originally deferred CONVERTED at
 * PR #345 (facet onlyElementMapperSRoundTrip, {@code OnlyElementRoundTripComposeTest}):
 * the {@code MapperC}&rarr;single {@code MapperS.of(...).get()} re-wrap landed as the
 * distinct-collapse-over-multi-chain arm of the #266 then-output wrap (the deref itself
 * had ALREADY fired here — only the RHS lacked the round-trip). Still deferred: the cdm
 * conditional-arm SET ({@code UnitEquals}, co-occupied with
 * {@code ComparisonResult.ofNullSafe} / {@code .asMapper}; the {@code Qualify_*} extract
 * operands took the #345 ofNullSafe arm).
 */
class FunctionConvertNullSafeWholeRhsDerefTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrFunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    // ---- toBuilder whole-RHS, single-arm SET (bare local) ---------------------

    /** Single deep-nav SET in an exists-guard: bare {@code referenceWithMetaParty}, toBuilder. */
    @Test
    @EnabledIf("cellsAvailable")
    void extractReportingCounterparty_bareToBuilderDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/hkma/rewrite/trade/functions/Extract_ReportingCounterparty.java");
    }

    // ---- toBuilder whole-RHS, two-arm conditional SET (numbered locals) -------

    /** Two-arm conditional SET: {@code referenceWithMetaParty0/1}, toBuilder consumer. */
    @Test
    @EnabledIf("cellsAvailable")
    void extractPartyResponsibleForReporting_numberedToBuilderDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/ExtractPartyResponsibleForReporting.java");
    }

    /** Two-arm conditional SET sibling: {@code referenceWithMetaParty0/1}. */
    @Test
    @EnabledIf("cellsAvailable")
    void extractReportSubmittingParty_numberedToBuilderDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/ExtractReportSubmittingParty.java");
    }

    /** Two-arm conditional SET, deeper meta type: {@code referenceWithMetaFloatingRateOption0/1}. */
    @Test
    @EnabledIf("cellsAvailable")
    void rateOption_numberedToBuilderDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/RateOption.java");
    }

    // ---- bare-value then-output deref -----------------------------------------

    /** then-output bare-value deref: {@code partyBIC = fieldWithMetaString.getValue()} (no toBuilder). */
    @Test
    @EnabledIf("cellsAvailable")
    void partyBIC_thenOutputBareValueDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/hkma/rewrite/trade/functions/PartyBIC.java");
    }

    // ---- bare-value, two-arm conditional SET (numbered locals) — oracle sibling

    /** cdm6 sibling: two conditional arms set {@code notionalCurrency = fieldWithMetaString0/1.getValue()}. */
    @Test
    @EnabledIf("cellsAvailable")
    void standardizedScheduleMonetaryNotionalCurrency_numberedBareValueDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/margin/schedule/functions/StandardizedScheduleMonetaryNotionalCurrencyFromResolvablePQ.java");
    }

    // ---- PR #249: distinct(...) whole-RHS recovery (erased type) --------------

    /**
     * PR #249: a {@code distinct(...).get()} whole-RHS SET whose terminal nav feature is
     * a meta wrapper ({@code ReferenceWithMetaParty}) erases the compiled expression type
     * to {@code null}, so the {@code getItemType} gate cannot see the wrapper. The
     * recovery walks the RHS AST ({@code NavigationHandler.tryTerminalMetaMapperType}, now
     * descending {@code DISTINCT}) to recover the wrapper, then the existing builder-output
     * deref fires: {@code final ReferenceWithMetaParty referenceWithMetaParty =
     * distinct(...).get(); if (== null) party = null; else party =
     * toBuilder(referenceWithMetaParty.getValue());}. Reverting the recovery re-emits the
     * bare non-compiling {@code party = toBuilder(distinct(...).get())}.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void extractPartyFromRelatedPartyByRole_distinctRhsToBuilderDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/ExtractPartyFromRelatedPartyByRole.java");
    }

    // ---- PR #249 green-safety: a green distinct(...) over a NON-meta terminal --

    /**
     * PR #249 green-safety lock: {@code IsActionTypePositionMODI} renders a green
     * {@code distinct(...)} whose terminal nav feature is a non-meta {@code Date}
     * (a {@code .map("Date", dt -> Date.of(...))} record leaf). The {@code DISTINCT}
     * descent + meta recovery added for {@code ExtractParty} must NOT spuriously resolve
     * this non-meta terminal as a wrapper — this byte-locks the green file unchanged.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void isActionTypePositionMODI_greenNonMetaDistinct_stillByteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/IsActionTypePositionMODI.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the bare non-compiling meta-wrapper SET reappears if the whole-RHS "
                + "convertNullSafe deref (renderMetaValueDerefOrNull builder-output arm + "
                + "StatementHoistSession numbering + the then-output wiring) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
