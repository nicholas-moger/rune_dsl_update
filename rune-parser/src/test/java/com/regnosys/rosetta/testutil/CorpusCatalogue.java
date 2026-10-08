package com.regnosys.rosetta.testutil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reader for {@code test-corpus/corpus-cells.tsv} — the SINGLE declarative source
 * of truth for which model releases the fork is tested against, and what each one
 * depends on.
 *
 * <p><b>Why this exists.</b> From PR #85 (2026-05-28) to 2026-08-14 the corpus was
 * frozen at 5 cells and the cell list accumulated seven independent hand-maintained
 * copies. Nothing kept them in step because nothing ever changed. The 2026-08-14
 * band expansion broke that assumption; the worst instance was
 * {@code CorpusDiagnosticGateTest}, which globbed each corpus's own directory (so a
 * new cell auto-loaded) while HARDCODING the transitive closure per version (so the
 * new cell's dependencies did not). A cell loaded without its dependencies produces
 * thousands of unresolved-reference diagnostics that look exactly like engine
 * defects. A half-dynamic list is worse than a static one: it fails silently and
 * blames the engine.
 *
 * <p><b>Dependency closures are per CELL, never per corpus.</b> Two cells of the
 * same corpus can pin different dependency versions — cdm 6.20.6 pins rune-fpml
 * 1.5.3 while cdm 6.21.0 pins 2.1.1 — so there is no such thing as "the cdm
 * closure". Loading both versions into one merged space would put two definitions
 * of every fpml type in scope; loading only one starves whichever cell pins the
 * other. Per-cell closures also mirror how upstream actually builds each release.
 *
 * <p>Paths are resolved against {@link CorpusWalker#repoRoot()} rather than the
 * working directory, so callers are not sensitive to the module they run from.
 */
public final class CorpusCatalogue {

    /** Repo-relative location of the source-of-truth table. */
    public static final String TSV_REL_PATH = "test-corpus/corpus-cells.tsv";

    /** A single dependency pin, e.g. {@code rune-fpml=2.1.1}. */
    public record Dep(String corpus, String version) {
        public String dirName() {
            return corpus + "-" + version;
        }
    }

    /**
     * One corpus cell.
     *
     * @param active     materialised on disk and gated (vs {@code planned} — in the
     *                   9.83.0 band but not yet materialised)
     * @param catalogue  a tested cell with its own goldens (vs a {@code resolution}
     *                   cell, loaded only as another cell's dependency closure)
     * @param declared   what the cell's own pom pins
     * @param resolved   what we actually load; differs from {@code declared} only
     *                   where we substitute the nearest carried version
     */
    public record Cell(String corpus, String version, boolean active, boolean catalogue,
                       String dsl, List<Dep> declared, List<Dep> resolved) {

        /** e.g. {@code cdm-6.21.0} — also the on-disk directory name and the gate key. */
        public String dirName() {
            return corpus + "-" + version;
        }

        /** e.g. {@code cdm/cdm-6.21.0}, relative to {@code test-corpus/}. */
        public String relPath() {
            return corpus + "/" + dirName();
        }

        /** True when every declared pin is loaded at its exact version. */
        public boolean resolvesExactly() {
            return declared.equals(resolved);
        }
    }

    private static List<Cell> cache;

    private CorpusCatalogue() {
        // utility class — no instances
    }

    /** Absolute path to the source-of-truth table. */
    public static Path tsvPath() {
        return CorpusWalker.repoRoot().resolve(TSV_REL_PATH);
    }

    /** Every row in the table, in file order. */
    public static synchronized List<Cell> all() {
        if (cache != null) {
            return cache;
        }
        Path tsv = tsvPath();
        List<Cell> cells = new ArrayList<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(tsv);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the corpus cell SOT at " + tsv, e);
        }
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\s+");
            if (f.length != 7) {
                throw new IllegalStateException(tsv + ":" + lineNo
                        + " expected 7 whitespace-separated columns (corpus version state role "
                        + "dsl declared resolved) but found " + f.length + ": " + line);
            }
            boolean active = switch (f[2]) {
                case "active" -> true;
                case "planned" -> false;
                default -> throw new IllegalStateException(
                        tsv + ":" + lineNo + " state must be 'active' or 'planned', was: " + f[2]);
            };
            boolean catalogue = switch (f[3]) {
                case "catalogue" -> true;
                case "resolution" -> false;
                default -> throw new IllegalStateException(
                        tsv + ":" + lineNo + " role must be 'catalogue' or 'resolution', was: " + f[3]);
            };
            cells.add(new Cell(f[0], f[1], active, catalogue, f[4],
                    parseDeps(f[5], tsv, lineNo), parseDeps(f[6], tsv, lineNo)));
        }
        if (cells.isEmpty()) {
            throw new IllegalStateException("no cells parsed from " + tsv);
        }
        cache = Collections.unmodifiableList(cells);
        return cache;
    }

    private static List<Dep> parseDeps(String field, Path tsv, int lineNo) {
        if ("-".equals(field)) {
            return List.of();
        }
        List<Dep> deps = new ArrayList<>();
        for (String part : field.split(",")) {
            int eq = part.indexOf('=');
            if (eq <= 0 || eq == part.length() - 1) {
                throw new IllegalStateException(tsv + ":" + lineNo
                        + " dependency must be '<corpus>=<version>', was: " + part);
            }
            deps.add(new Dep(part.substring(0, eq), part.substring(eq + 1)));
        }
        return List.copyOf(deps);
    }

    /** Active cells with their own goldens — the tested matrix. */
    public static List<Cell> activeCatalogue() {
        return all().stream().filter(c -> c.active() && c.catalogue()).toList();
    }

    /** Active cells loaded only as another cell's dependency closure. */
    public static List<Cell> activeResolution() {
        return all().stream().filter(c -> c.active() && !c.catalogue()).toList();
    }

    /**
     * Directory names of active resolution-only cells, for
     * {@link CorpusWalker#TRANSITIVE_DEP_CELL_DIRS}-style exclusion from the
     * catalogue census.
     */
    public static Set<String> activeResolutionDirNames() {
        Set<String> out = new LinkedHashSet<>();
        for (Cell c : activeResolution()) {
            out.add(c.dirName());
        }
        return Collections.unmodifiableSet(out);
    }

    /** Look up a cell by corpus and version. */
    public static Optional<Cell> find(String corpus, String version) {
        return all().stream()
                .filter(c -> c.corpus().equals(corpus) && c.version().equals(version))
                .findFirst();
    }

    // ---------------------------------------------------------------- paths

    /** The shared rune-dsl builtins root (annotations + basictypes). */
    public static Path builtinsRoot() {
        return CorpusWalker.repoRoot()
                .resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model");
    }

    /** The cell's own directory, e.g. {@code <repo>/test-corpus/cdm/cdm-6.21.0}. */
    public static Path cellDir(Cell cell) {
        return CorpusWalker.repoRoot().resolve("test-corpus").resolve(cell.relPath());
    }

    /**
     * The {@code .rosetta} source root a DEPENDENCY is loaded from. Dependencies
     * contribute source for type resolution only, so this points at the model
     * sources rather than the whole cell tree.
     */
    public static Path depSourceRoot(Dep dep) {
        return CorpusWalker.repoRoot()
                .resolve("test-corpus")
                .resolve(dep.corpus())
                .resolve(dep.dirName())
                .resolve("rosetta-source/src/main/rosetta");
    }

    /**
     * Load roots for one cell, in the D11 cell-loader order: builtins, then the
     * cell's TRANSITIVE dependency closure (dependencies before their dependents),
     * then the cell's own tree LAST. Callers rely on the last entry being the cell's
     * own root.
     *
     * <p><b>The closure must be transitive.</b> drr 7.3.0 pins cdm 6.21.0, and cdm
     * 6.21.0 in turn pins rune-fpml 2.1.1 — so loading only drr's direct pins starves
     * it of every fpml type. Measured: a one-level closure left drr 7.3.0 with 3,913
     * linking errors (ENUM_NOT_FOUND 2,340 · TYPE_NOT_FOUND 1,060 · SYMBOL_NOT_FOUND
     * 288 · IMPORT_UNRESOLVED 225). drr 6.34.1 masked the bug because its pin,
     * cdm 5.38.0, happens to have no dependencies of its own.
     */
    public static List<Path> loadRoots(Cell cell) {
        List<Path> roots = new ArrayList<>();
        roots.add(builtinsRoot());
        LinkedHashSet<Dep> closure = new LinkedHashSet<>();
        collectClosure(cell, closure, new LinkedHashSet<>());
        for (Dep d : closure) {
            roots.add(depSourceRoot(d));
        }
        roots.add(cellDir(cell));
        return List.copyOf(roots);
    }

    /**
     * Post-order walk of the resolved dependency graph, so a dependency is always
     * added before anything that depends on it. {@code visiting} guards against a
     * dependency cycle, which would otherwise recurse until the stack blew.
     */
    private static void collectClosure(Cell cell, LinkedHashSet<Dep> out,
                                       LinkedHashSet<String> visiting) {
        if (!visiting.add(cell.dirName())) {
            throw new IllegalStateException("dependency cycle in " + TSV_REL_PATH
                    + ": " + String.join(" -> ", visiting) + " -> " + cell.dirName());
        }
        for (Dep d : cell.resolved()) {
            Optional<Cell> depCell = find(d.corpus(), d.version());
            if (depCell.isPresent()) {
                collectClosure(depCell.get(), out, visiting);
            }
            // A dep with no SOT row cannot contribute a closure of its own;
            // CorpusCatalogueTest fails on that separately rather than here, so the
            // root still gets added and the missing-directory error stays precise.
            out.add(d);
        }
        visiting.remove(cell.dirName());
    }
}
