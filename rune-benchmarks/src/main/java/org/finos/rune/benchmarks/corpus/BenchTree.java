package org.finos.rune.benchmarks.corpus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The benchmark-side tree selector (the PR-4 before/after switch): with
 * {@code -Dbench.optimisedTree=true} on the FORK JVM the named cell compiles via
 * {@link CorpusClasses#ensureCompiledWithOverlay} over the materialized optimised
 * tree ({@code rune-ir-java-optimised/target/optimised-tree/<cell>} — run that
 * module's {@code OptimisedNavigationEmissionTest} first); without it, the standard
 * reference compile. Which tree ran (and the overlay's content stamp) is printed so
 * every JMH receipt is self-describing — a benchmark number without its tree label
 * is not a receipt.
 */
public final class BenchTree {

    public static final String PROPERTY = "bench.optimisedTree";
    private static final String OVERLAY_LABEL = "optnav";

    private BenchTree() {}

    public static Path cellClasses(String cellRel, List<Path> upstreamOutDirs,
                                   String excludeContains) {
        if (!Boolean.getBoolean(PROPERTY)) {
            Path out = CorpusClasses.ensureCompiled(cellRel, upstreamOutDirs, excludeContains);
            System.out.println("[BenchTree] " + cellRel + " tree=reference -> " + out);
            return out;
        }
        String leaf = cellRel.substring(cellRel.lastIndexOf('/') + 1);
        Path overlay = CorpusClasses.repoRoot().resolve("rune-ir-java-optimised")
                .resolve("target").resolve("optimised-tree").resolve(leaf);
        Path out = CorpusClasses.ensureCompiledWithOverlay(cellRel, upstreamOutDirs,
                excludeContains, overlay, OVERLAY_LABEL);
        String stamp;
        try {
            stamp = Files.readString(overlay.resolve(".overlay-marker"), StandardCharsets.UTF_8)
                    .trim();
        } catch (java.io.IOException e) {
            stamp = "unreadable";
        }
        System.out.println("[BenchTree] " + cellRel + " tree=optimised{" + stamp + "} -> " + out);
        return out;
    }
}
