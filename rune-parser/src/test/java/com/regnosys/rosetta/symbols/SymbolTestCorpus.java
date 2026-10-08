package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.builder.AstBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Shared corpus-loading helper for symbol-table tests. Walks a corpus root,
 * parses every {@code .rosetta} file under it, and returns them in sorted
 * (deterministic) path order. Extracted to eliminate the identical private
 * {@code loadCorpus} implementations previously duplicated across
 * {@link SymbolTableSnapshotTest}, {@link SymbolTableSerializerTest} and
 * {@link CrossCorpusConsistencyTest}.
 *
 * <p><b>Build-output copies are excluded</b>, using the same
 * {@link CorpusWalker#isBuildOutputPath} rule the catalogue census applies —
 * one rule, one place. ISO-20022 ships 47 {@code .rosetta} files duplicated
 * under {@code rosetta-source/target/classes/}, and a cell's load root is the
 * CELL DIRECTORY, so an unpruned walk fed every one of those sources to the
 * linker TWICE. The census had already learned this (its pre-expansion "589"
 * exceeded the catalogue's 540 by exactly 47 + 2) and pruned; this loader had
 * not, so the two disagreed about what the corpus even contains — and this is
 * the loader the 25-cell diagnostic gate runs on. Found by Copilot R4 on
 * PR #566 as a drift surface; it was live.
 */
final class SymbolTestCorpus {

    private SymbolTestCorpus() {}

    static List<RModel> loadCorpus(Path root) throws IOException {
        return loadCorpus(root, p -> false);
    }

    /**
     * The same walk, less the files {@code skip} names — added at v3.2 seat 4 (PR #625, round 2: the code-quality
     * review's SF-3) for the qualification-warning census, whose chaos cell must drop the 22 {@code a5bom} files the
     * fork parser is EXPECTED to refuse ({@code ChaosParseExpectations.isExpectedRefusal}; the released plugin refuses
     * them whole too, so they carry none of its warnings — the resolution-conformance test's section 4b law) BEFORE
     * parsing, which the one-argument form cannot do. One walk and one prune rule for both callers: a prune added
     * here reaches every cell the census measures, which a private copy of this method would not. The predicate
     * is applied AFTER the build-output prune and BEFORE the sort, so a skip never moves the surviving files'
     * relative order; {@code isExpectedRefusal} is keyed on the chaos PATH and the basename, so no vendored path can
     * match it. Order matters to a first-wins reader (F10's qualifiable root, hence a warning count) in principle;
     * here the walk is the full-path sort the D11 loader also uses, chaos's winner {@code a1o1} sorts first under
     * both, and no vendored cell declares two configurations of one kind (eleven cdm cells × one {@code isEvent}
     * + one {@code isProduct}, zero elsewhere — measured at round 2), so the census's map is order-free where it
     * is measured; the generator routes can load a PINNED order (the #413 replay pins), which this walk does not.
     */
    static List<RModel> loadCorpus(Path root, Predicate<Path> skip) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                .filter(p -> p.toString().endsWith(".rosetta"))
                .filter(p -> !com.regnosys.rosetta.testutil.CorpusWalker.isBuildOutputPath(p))
                .filter(p -> !skip.test(p))
                .sorted()
                .map(p -> {
                    try {
                        return AstBuilder.buildFromString(Files.readString(p), p.toString());
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to read corpus file: " + p, e);
                    }
                })
                .toList();
        }
    }
}
