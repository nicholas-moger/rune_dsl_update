package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * JUnit {@link ArgumentsProvider} that walks the regression {@code test-corpus/}
 * tree and yields one {@link MatrixCoordinate} per {@code .rosetta} input file.
 *
 * <p>Part of P1.2 test-harness infrastructure (audit hook H14). Drives every
 * matrix-aware {@code @ParameterizedTest}, so a single provider implementation
 * is the only place that knows how {@code test-corpus} is laid out on disk.
 *
 * <h2>Layout assumptions</h2>
 * The root directory (default {@code test-corpus/}) contains one subdirectory
 * per {@link Corpus} named {@link Corpus#dirName()}. Each corpus directory in
 * turn contains one subdirectory per shipped version, named
 * {@code <dirName>-<version>} (e.g. {@code cdm-6.16.0}, {@code drr-7.0.0-dev.111}).
 * {@code .rosetta} files are discovered recursively below each version
 * directory. No other path conventions are assumed.
 *
 * <h2>No regex rule</h2>
 * Layout parsing uses literal-delimiter string operations only:
 * {@link String#startsWith(String)} plus a single {@link String#substring(int)}
 * to strip the version prefix. No regular expressions, no string munging on
 * language content. Filename-suffix classification is literal matching against
 * a fixed whitelist — permitted by the CLAUDE.md exception for non-structured
 * file-system metadata.
 *
 * <h2>Missing-corpus policy</h2>
 * A missing corpus does not throw. {@link #discover(Path)} simply skips it and
 * returns an empty stream for that corpus. Test classes that want to report
 * missing corpora to the user should call {@link #missingCorporaDiagnostic(Path)}
 * and pass it to {@code Assumptions.abort} (audit Q8 — tiered skip with
 * diagnostic, never silent pass).
 */
public final class MatrixCellsProvider implements ArgumentsProvider {

    /**
     * Default corpus root, resolved relative to the working directory
     * Surefire uses for the {@code rune-parser/} module. The repo layout
     * puts the cloned corpora at {@code <repo-root>/test-corpus/}, so from
     * {@code rune-parser/} the relative path is {@code ../test-corpus}.
     */
    public static final Path DEFAULT_ROOT = Path.of("..", "test-corpus");

    /**
     * Filename-suffix → element-kind classification table. Literal string match
     * against the segment after the final {@code -} in the filename stem.
     */
    private static final Map<String, ElementKind> SUFFIX_KINDS = Map.of(
            "type", ElementKind.TYPE,
            "enum", ElementKind.ENUM,
            "func", ElementKind.FUNCTION,
            "rule", ElementKind.RULE,
            "report", ElementKind.REPORT,
            "synonym", ElementKind.SYNONYM,
            "annotations", ElementKind.ANNOTATION,
            "basictypes", ElementKind.BASIC_TYPE
    );

    private final Path root;

    /** Required by JUnit's no-arg provider contract. Uses {@link #DEFAULT_ROOT}. */
    public MatrixCellsProvider() {
        this(DEFAULT_ROOT);
    }

    /** Visible for tests — inject a hermetic tmp root. */
    MatrixCellsProvider(Path root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    /**
     * Return the matrix cells under the configured root, or abort the invoking
     * parameterised test via {@link TestAbortedException} if zero cells were
     * discovered. An empty stream is treated by JUnit as a configuration error,
     * so turning an empty discovery into a skip-with-diagnostic is the only way
     * to honour the audit's missing-corpus policy (Q8) without masking real
     * failures.
     */
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        List<MatrixCoordinate> cells = discover(root).toList();
        if (cells.isEmpty()) {
            String diag = missingCorporaDiagnostic(root);
            String reason = diag.isEmpty()
                    ? "no .rosetta files discovered under " + root
                    : diag;
            throw new TestAbortedException(reason);
        }
        return cells.stream().map(Arguments::of);
    }

    /**
     * Discover every {@link MatrixCoordinate} under {@code root}.
     *
     * <p>Missing {@code root}, missing corpus subdirectories, and missing version
     * subdirectories are silently skipped — the stream simply contains no cells
     * for those. Call {@link #missingCorporaDiagnostic(Path)} separately if the
     * test needs to surface the absence as a skip diagnostic.
     *
     * <p>Ordering is deterministic: cells are returned in natural
     * {@link MatrixCoordinate} order, independent of filesystem traversal order.
     */
    public static Stream<MatrixCoordinate> discover(Path root) {
        if (!Files.isDirectory(root)) {
            return Stream.empty();
        }
        List<MatrixCoordinate> all = new ArrayList<>();
        for (Corpus corpus : Corpus.values()) {
            Path corpusDir = root.resolve(corpus.dirName());
            if (!Files.isDirectory(corpusDir)) continue;
            collectCorpus(corpus, corpusDir, all);
        }
        Collections.sort(all);
        return all.stream();
    }

    /**
     * Return a human-readable list of every corpus that is not present under
     * {@code root}. Empty string if every corpus is present. Intended to be
     * passed to {@code Assumptions.abort} when a test requires the full matrix.
     */
    public static String missingCorporaDiagnostic(Path root) {
        List<String> missing = new ArrayList<>();
        for (Corpus c : Corpus.values()) {
            if (!Files.isDirectory(root.resolve(c.dirName()))) {
                missing.add(c.dirName());
            }
        }
        if (missing.isEmpty()) return "";
        return "missing corpora under " + root + ": " + String.join(", ", missing);
    }

    private static void collectCorpus(Corpus corpus, Path corpusDir, List<MatrixCoordinate> out) {
        String prefix = corpus.dirName() + "-";
        try (Stream<Path> versionDirs = Files.list(corpusDir)) {
            versionDirs.filter(Files::isDirectory).forEach(verDir -> {
                String dirName = verDir.getFileName().toString();
                if (!dirName.startsWith(prefix)) return;
                // PR #185: skip transitive-DEPENDENCY cells (rune-fpml-1.5.3) — they are
                // loaded only as another cell's resolution closure, never byte-compared as
                // a cell of their own, so they stay out of the 5-cell CATALOGUE census /
                // structural baselines (still pinned by the frozen-baseline manifest).
                if (com.regnosys.rosetta.testutil.CorpusWalker.TRANSITIVE_DEP_CELL_DIRS.contains(dirName)) return;
                String rawVersion = dirName.substring(prefix.length());
                Version version;
                try {
                    version = Version.parse(rawVersion);
                } catch (IllegalArgumentException e) {
                    return;
                }
                collectVersion(corpus, version, verDir, out);
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Walks a single corpus-version directory and adds one
     * {@link MatrixCoordinate} per {@code .rosetta} file.
     *
     * <p>The {@code project} axis is deliberately left as the empty string
     * for every cell: no currently-shipped corpus (CDM, DRR, ISO-20022,
     * rune-fpml) organises its sources into named sub-projects. Reserving
     * the axis preserves the scope matrix shape for the future case where
     * a corpus does ship per-project sources (for example a multi-module
     * CDM variant), without forcing every cell today to carry a synthetic
     * project name derived from path heuristics.
     */
    private static void collectVersion(Corpus corpus, Version version, Path verDir, List<MatrixCoordinate> out) {
        // Prune VCS metadata (`.git`) and build output (`target`, `node_modules`)
        // rather than walking them. This MIRRORS StructuralComparisonTest#extractCounts
        // EXACTLY, and the two must not diverge: this provider is the PRODUCER of the
        // committed structural baselines and that test is their CONSUMER.
        //
        // The pruning is correctness, not just speed. ISO-20022's build copies all 47
        // of its .rosetta sources into rosetta-source/target/classes/iso20022/rosetta,
        // so an unpruned walk counts every ISO element TWICE — it emitted a baseline of
        // 94 files / 4,816 elements against the consumer's 47 / 2,408, i.e. a baseline
        // its own consumer rejects on sight. That stayed latent from PR #85 until the
        // 2026-08-14 band expansion simply because nothing re-ran the dumper while the
        // corpus was frozen. (ISO's GENERATED goldens also live under target/ by design
        // — see D11CorpusRegressionTest#resolveGoldensDir/D25 — which is why the
        // directory itself must stay on disk and cannot just be deleted.)
        //
        // The walk collects into a local list which is SORTED before any coordinate is
        // emitted, because `Files.walkFileTree` yields directory entries in filesystem
        // order — stable on one box, not guaranteed across filesystems or platforms.
        // The consumer sorts (StructuralComparisonTest#extractCounts, Collections.sort),
        // so a producer that does not is not the "EXACT" mirror this comment claims and
        // makes the emitted cell ORDER environment-dependent (Copilot R7, PR #566).
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(verDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Path name = dir.getFileName();
                    if (name != null) {
                        String n = name.toString();
                        if (n.equals(".git") || n.equals("target") || n.equals("node_modules")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().endsWith(".rosetta")) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Collections.sort(files);
        for (Path file : files) {
            out.add(new MatrixCoordinate(
                    corpus, "", version, classifyByFilename(file), file));
        }
    }

    /**
     * Classify an element kind from the filename suffix alone (literal match on
     * the segment after the final {@code -} in the filename stem). Files whose
     * suffix does not appear in {@link #SUFFIX_KINDS} — including descriptive
     * {@code -desc.rosetta} files which mix multiple kinds — are classified as
     * {@link ElementKind#UNKNOWN}. Honest classification beats wrong
     * classification; {@code UNKNOWN} is recorded as a skip diagnostic downstream.
     */
    static ElementKind classifyByFilename(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return ElementKind.UNKNOWN;
        String stem = name.substring(0, dot);
        int dash = stem.lastIndexOf('-');
        if (dash < 0) return ElementKind.UNKNOWN;
        String suffix = stem.substring(dash + 1);
        return SUFFIX_KINDS.getOrDefault(suffix, ElementKind.UNKNOWN);
    }
}
