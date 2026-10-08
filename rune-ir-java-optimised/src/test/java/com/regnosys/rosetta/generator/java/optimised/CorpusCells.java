package com.regnosys.rosetta.generator.java.optimised;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RWorkspace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Shared corpus loading for this module's tests (the census instrument, the alias
 * disposition map, the emission reconciliation gate) — the D11 conventions:
 * module-relative {@code ../test-corpus} roots, the builtin union in priority order,
 * per-cell dependency closures, and the OWN-models set tracked so per-cell accounting
 * never counts a dependency's bodies.
 *
 * <p><b>The cell list is READ from the corpus source of truth</b>,
 * {@code test-corpus/corpus-cells.tsv} — every ACTIVE CATALOGUE row, in file order —
 * and each cell's dependency closure is the TRANSITIVE walk of the TSV's
 * {@code resolved} column (drr 7.x → cdm 6.21.0 → rune-fpml 2.1.1), dependencies
 * first, deduplicated. Until PR #607 this class carried its own fixed five-cell list
 * (the one deliberate exception the testing documentation named): the optimised
 * route's gates covered the three function-bearing ring cells and none of the 20
 * band cells v3.1 re-rendered. Reading the SOT is what lets the emission test and
 * the pair gate run over the whole matrix without a second hand-kept copy of the
 * band. This is a deliberate second reader of ONE shared data file (the generator
 * module's {@code testutil.CorpusCells} reads the same file; the modules share no
 * test-jar), not a second copy of the data.
 *
 * <p>Resolution-only rows (never byte-compared, loaded only as closures) are not
 * cells here; they appear only inside a catalogue cell's {@code depDirs}.
 */
final class CorpusCells {

    static final Path CORPUS_ROOT = Path.of("../test-corpus");
    static final Path CELL_SOT = CORPUS_ROOT.resolve("corpus-cells.tsv");
    static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            CORPUS_ROOT.resolve("rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            Path.of("../rune-dsl/rune-runtime/src/main/resources/model"));

    /**
     * One cell: {@code label} = {@code <corpus>/<version>} (the pin key), {@code ownDir}
     * = {@code <corpus>/<corpus>-<version>}, {@code depDirs} = the transitive resolved
     * closure in load order (dependencies of dependencies first).
     */
    record CellSpec(String label, String ownDir, List<String> depDirs) {
    }

    /** Every active catalogue cell of the SOT, in file order; EMPTY when the SOT is absent. */
    static final List<CellSpec> CELLS = readActiveCatalogue();

    /** One loaded cell: the linked workspace + the cell's OWN models in load order. */
    record LoadedCell(RWorkspace workspace, List<RModel> ownModels) {

        /** An identity membership set over {@link #ownModels} (the emission-filter shape). */
        Set<RModel> ownModelSet() {
            Set<RModel> set = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            set.addAll(ownModels);
            return set;
        }
    }

    private CorpusCells() {
    }

    /** True when the SOT was readable and its first catalogue cell is on disk (the suite gate). */
    static boolean corpusStaged() {
        return !CELLS.isEmpty() && staged(CELLS.get(0));
    }

    /**
     * DIAGNOSTIC ONLY ({@code -Doptnav.cells=cdm/6.20.2,drr/7.3.0}): restrict a run to
     * the named cells while decoding one cell's finding — the whole-matrix walks stay
     * the default, and no gate reads this property to decide what it asserts.
     */
    static final String CELL_FILTER = System.getProperty("optnav.cells");

    /** {@link #CELLS}, or the {@code -Doptnav.cells} subset when that diagnostic is set. */
    static List<CellSpec> cellsUnderTest() {
        if (CELL_FILTER == null || CELL_FILTER.isBlank()) {
            return CELLS;
        }
        Set<String> wanted = new LinkedHashSet<>(List.of(CELL_FILTER.split(",")));
        List<CellSpec> subset = CELLS.stream().filter(c -> wanted.contains(c.label())).toList();
        if (subset.size() != wanted.size()) {
            Set<String> unknown = new LinkedHashSet<>(wanted);
            subset.forEach(c -> unknown.remove(c.label()));
            throw new IllegalStateException("-Doptnav.cells names cells that are not active"
                    + " catalogue cells of the SOT: " + unknown + " (requested " + wanted + ")");
        }
        return subset;
    }

    static boolean cellFilterActive() {
        return CELL_FILTER != null && !CELL_FILTER.isBlank();
    }

    static boolean staged(CellSpec cell) {
        return Files.isDirectory(CORPUS_ROOT.resolve(cell.ownDir()));
    }

    static LoadedCell load(CellSpec cell) throws IOException {
        List<RModel> models = new ArrayList<>();
        for (Path p : resolveBuiltinFiles()) {
            models.add(AstBuilder.buildFromFile(p));
        }
        for (String dep : cell.depDirs()) {
            for (Path p : rosettaFiles(CORPUS_ROOT.resolve(dep + "/rosetta-source/src/main/rosetta"))) {
                models.add(AstBuilder.buildFromFile(p));
            }
        }
        List<RModel> own = new ArrayList<>();
        for (Path p : rosettaFiles(CORPUS_ROOT.resolve(cell.ownDir() + "/rosetta-source/src/main/rosetta"))) {
            RModel model = AstBuilder.buildFromFile(p);
            own.add(model);
            models.add(model);
        }
        RWorkspace ws = RWorkspace.build(models).workspace();
        return new LoadedCell(ws, own);
    }

    static List<Path> rosettaFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        // The § 4b skip is SCOPE-GUARDED to the chaos tree (the cq review's SF-1,
        // mirroring ChaosParseExpectations.isExpectedRefusal's own law: "an
        // expectation must never leak onto a vendored cell") — a vendored or dep
        // file sharing a chaos basename must never be dropped from its workspace.
        boolean chaosTree = dir.toString().replace('\\', '/').contains("/test-corpus/chaos/");
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(p -> p.toString().endsWith(".rosetta"))
                    // The chaos cell's 22 § 4b expected-refusal files (BOM refusal
                    // parity, v3.2 PR-2) parse-refuse by design and emitted no goldens
                    // upstream either — population-consistent to skip at load. The set
                    // is the committed fork-diagnostics.tsv (this module cannot see
                    // rune-parser's test scope — the #464 no-test-jar law — so it reads
                    // the same ONE data file; the rune-parser suite gate asserts the
                    // live refusal set equals it, both directions).
                    .filter(p -> !chaosTree || !chaosExpectedRefusalFilenames().contains(
                            p.getFileName().toString()))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static volatile java.util.Set<String> chaosRefusalCache;

    private static java.util.Set<String> chaosExpectedRefusalFilenames() {
        java.util.Set<String> cached = chaosRefusalCache;
        if (cached != null) {
            return cached;
        }
        Path tsv = CORPUS_ROOT.getParent()
                .resolve("scripts/chaos-expander/expectations/fork-diagnostics.tsv");
        if (!Files.isRegularFile(tsv)) {
            throw new IllegalStateException("committed chaos expectations missing: " + tsv
                    + " — broken checkout (the § 4b SOT is committed, never generated)");
        }
        java.util.Set<String> names = new java.util.TreeSet<>();
        try {
            int lineNo = 0;
            for (String raw : Files.readAllLines(tsv, java.nio.charset.StandardCharsets.UTF_8)) {
                lineNo++;
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                // Tab-delimited config row, not language content (the no-regex law's
                // trivial-literal exception). ONE strict law across every reader of
                // this file (the cq review's MF-2): malformed rows fail loud.
                String[] f = line.split("\t");
                if (f.length != 3) {
                    throw new IllegalStateException(tsv + ":" + lineNo
                            + " expected 3 tab-separated columns, found " + f.length + ": " + line);
                }
                if ("FORK-REFUSES".equals(f[1]) && !names.add(f[0])) {
                    throw new IllegalStateException(tsv + ":" + lineNo
                            + " duplicate expected-refusal row: " + f[0]);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + tsv, e);
        }
        if (names.isEmpty()) {
            throw new IllegalStateException(tsv + " carries no FORK-REFUSES rows — the § 4b"
                    + " refusal-parity contract cannot be empty while the a5bom family exists");
        }
        chaosRefusalCache = java.util.Set.copyOf(names);
        return chaosRefusalCache;
    }

    private static volatile List<Path> builtinFilesCache;

    static List<Path> resolveBuiltinFiles() throws IOException {
        // Memoized: the builtins set is cell-invariant and the tests resolve it
        // once per cell load — one walk per JVM suffices (Copilot #539 R1).
        List<Path> cached = builtinFilesCache;
        if (cached != null) {
            return cached;
        }
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> s = Files.walk(root)) {
                // Sorted so a duplicate filename WITHIN one root resolves
                // deterministically (cross-root priority is the map's putIfAbsent).
                s.filter(p -> p.toString().endsWith(".rosetta"))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<Path> result = resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();
        builtinFilesCache = result;
        return result;
    }

    // ------------------------------------------------------------------ the SOT --

    /** One parsed TSV row (catalogue and resolution alike — closures need both). */
    private record SotRow(String corpus, String version, boolean active, boolean catalogue,
                          List<String> resolvedDeps) {
        String ownDir() {
            return corpus + "/" + corpus + "-" + version;
        }

        String label() {
            return corpus + "/" + version;
        }
    }

    private static List<CellSpec> readActiveCatalogue() {
        if (!Files.isRegularFile(CELL_SOT)) {
            return List.of(); // corpus absent (CI / a fresh clone): every suite skips on corpusStaged()
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(CELL_SOT);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the corpus cell SOT at " + CELL_SOT, e);
        }
        Map<String, SotRow> rows = new LinkedHashMap<>();
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\s+");
            if (f.length != 7) {
                throw new IllegalStateException(CELL_SOT + ":" + lineNo
                        + " expected 7 columns, found " + f.length + ": " + line);
            }
            // An unknown state/role is REJECTED, never read as false — a typo must not
            // quietly shrink the tested matrix (the generator reader's own law).
            boolean active = switch (f[2]) {
                case "active" -> true;
                case "planned" -> false;
                default -> throw new IllegalStateException(CELL_SOT + ":" + lineNo
                        + " state must be 'active' or 'planned', was: " + f[2]);
            };
            boolean catalogue = switch (f[3]) {
                case "catalogue" -> true;
                case "resolution" -> false;
                default -> throw new IllegalStateException(CELL_SOT + ":" + lineNo
                        + " role must be 'catalogue' or 'resolution', was: " + f[3]);
            };
            List<String> deps = new ArrayList<>();
            if (!"-".equals(f[6])) {
                for (String part : f[6].split(",")) {
                    int eq = part.indexOf('=');
                    if (eq <= 0 || eq == part.length() - 1) {
                        throw new IllegalStateException(CELL_SOT + ":" + lineNo
                                + " dependency must be '<corpus>=<version>', was: " + part);
                    }
                    deps.add(part.substring(0, eq) + "/" + part.substring(eq + 1));
                }
            }
            SotRow row = new SotRow(f[0], f[1], active, catalogue, List.copyOf(deps));
            if (rows.put(row.label(), row) != null) {
                throw new IllegalStateException(CELL_SOT + ":" + lineNo
                        + " duplicate cell " + row.label());
            }
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("no cells parsed from " + CELL_SOT);
        }
        List<CellSpec> cells = new ArrayList<>();
        for (SotRow row : rows.values()) {
            if (!row.active() || !row.catalogue()) {
                continue;
            }
            Set<String> closure = new LinkedHashSet<>();
            collectClosure(row, rows, closure, new LinkedHashSet<>());
            cells.add(new CellSpec(row.label(), row.ownDir(), List.copyOf(closure)));
        }
        return List.copyOf(cells);
    }

    /**
     * The transitive resolved closure, dependencies-of-dependencies FIRST (the load
     * order a workspace needs: cdm 6.21.0's fpml types must be present before cdm
     * 6.21.0's ingest functions that reference them), each cell dir once.
     */
    private static void collectClosure(SotRow row, Map<String, SotRow> rows, Set<String> out,
                                       Set<String> onPath) {
        if (!onPath.add(row.label())) {
            throw new IllegalStateException("dependency cycle through " + row.label()
                    + " in " + CELL_SOT);
        }
        for (String depLabel : row.resolvedDeps()) {
            SotRow dep = rows.get(depLabel);
            if (dep == null) {
                throw new IllegalStateException(CELL_SOT + ": " + row.label()
                        + " resolves against " + depLabel + ", which is not a row of the SOT");
            }
            if (!dep.active()) {
                throw new IllegalStateException(CELL_SOT + ": " + row.label()
                        + " resolves against " + depLabel + ", which is not active");
            }
            collectClosure(dep, rows, out, onPath);
            out.add(dep.ownDir());
        }
        onPath.remove(row.label());
    }
}
