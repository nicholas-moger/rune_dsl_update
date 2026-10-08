package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.harness.cache.CachedMatrixExtension;
import com.regnosys.rosetta.harness.matrix.MatrixCellsProvider;
import com.regnosys.rosetta.harness.matrix.MatrixCoordinate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hard parse-gate for every {@code .rosetta} file the matrix discovers.
 *
 * <p>Migrated at P1.2 H15 from a {@code @TestFactory} walking the corpus tree
 * directly to a {@code @ParameterizedTest} driven by
 * {@link MatrixCellsProvider}. Every failure now carries a full
 * {@link MatrixCoordinate} (corpus × project × version × element-kind), so
 * failure grouping by corpus/version is free and no silent CDM-only default
 * survives.
 *
 * <p>Missing corpora are surfaced as a JUnit skip (via
 * {@code TestAbortedException} thrown by the provider) — never a silent pass.
 * Built-in {@code .rosetta} files live outside the corpus tree and are
 * exercised by {@link BuiltinParseTest}.
 *
 * <p>P1.2 audit hook H24.3 — applying {@link CachedMatrixExtension} here
 * makes repeat runs short-circuit on content-addressable cache hits (file
 * SHA × test bytecode SHA × tool version SHA). The mode is controlled by
 * the {@code matrix.cache.mode} system property (default {@code READ_WRITE}),
 * which the Maven {@code pr-gate} / {@code nightly} / {@code release-gate}
 * profiles set per audit Q12.
 */
@ExtendWith(CachedMatrixExtension.class)
class CorpusParseTest {

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(MatrixCellsProvider.class)
    void parses_without_errors(MatrixCoordinate cell) {
        RosettaParseResult result = RosettaParserFacade.parseFile(cell.source());
        // The chaos cell (v3.2 PR-2, charter § 4b) carries 22 files DESIGNED to be
        // parse-refused (BOM refusal parity with upstream). For those, the gate
        // INVERTS: the file must STILL refuse, with the pinned diagnostic — a clean
        // parse here is the refusal healing silently (LAW 81). Every other chaos
        // file keeps the full zero-error contract; the enumerated set is the
        // committed fork-diagnostics.tsv, reconciled against the census pin and the
        // live verdicts by ChaosForkDiagnosticsGateTest.
        if (cell.corpus() == com.regnosys.rosetta.harness.matrix.Corpus.CHAOS
                && ChaosParseExpectations.isExpectedRefusal(cell.source())) {
            assertFalse(result.errors().isEmpty(),
                    "expected-refusal chaos file PARSED CLEAN — the pinned refusal has"
                            + " healed; re-adjudicate " + cell.displayName()
                            + " against " + ChaosParseExpectations.FORK_DIAGNOSTICS_TSV);
            String required = ChaosParseExpectations.requiredDiagnosticSubstring(cell.source());
            assertTrue(result.errors().get(0).contains(required),
                    "expected-refusal chaos file refused with a DIFFERENT diagnostic in "
                            + cell.displayName() + ": wanted a first error containing '"
                            + required + "', got:\n" + String.join("\n", result.errors()));
            return;
        }
        assertTrue(result.errors().isEmpty(),
                "Parse errors in " + cell.displayName() + ":\n"
                        + String.join("\n", result.errors()));
    }
}
