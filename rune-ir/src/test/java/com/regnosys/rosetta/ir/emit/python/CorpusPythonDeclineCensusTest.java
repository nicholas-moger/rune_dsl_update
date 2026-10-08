package com.regnosys.rosetta.ir.emit.python;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ir.emit.EmitterException;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DIAGNOSTIC census (not a golden): ranks which expression family the <strong>Python verb emitter</strong>
 * ({@link IRPythonEmitter}) declines most often over the real corpus, so the highest-leverage next emitter slice is
 * chosen by evidence rather than guessed (decision-log L-130 — Nick's "measure decline-rate, then build"). It also
 * measures the <strong>cross-inheritance field collisions</strong> the unlinked {@code CorpusSnakeCollisionCensusTest}
 * could not (that census's skeptic-review Finding 1): a child's own field snake-colliding with an inherited one.
 *
 * <p><strong>The load-bearing distinction (two decline classes).</strong> A function-body expression reaches Python
 * via {@code ExpressionToIRAdapter.adapt(expr, ws) -> Optional<IRExpr>} and then {@link IRPythonEmitter}:
 * <ul>
 *   <li><strong>Adapter decline</strong> = {@code Optional.empty()} — the neutral IR could not represent the seam.
 *       This is the current dominant <em>bottleneck</em>, but it is NOT (as first assumed) uniformly "fork-gated":
 *       the largest families — {@code RConstructorExpr}, {@code RConditionalExpr}, {@code RThenExpr} (~47% of the
 *       adapter declines) — simply have <strong>no {@code adapt} arm yet</strong> and decline regardless of linking,
 *       so they are <strong>lab-buildable neutral-IR headroom</strong> (that feeds every target), not fork-gated
 *       inference. Only a residue reflects fork-gated / under-resolved inference ({@code getInferredType} MISSING →
 *       empty), so the per-family split is indicative and mixes both causes (decision-log L-131).</li>
 *   <li><strong>Emitter decline</strong> = a thrown {@link EmitterException} — the adapter produced an {@code IRExpr}
 *       but the Python emitter cannot lower it. This is the <strong>in-house buildable gap</strong>, and (because it
 *       required a successful adaptation) it is <em>immune to under-linking</em>. THIS ranking is reliable — but note
 *       it speaks only to the ~18% of seam-roots that adapt at all (e.g. {@code reference:ALIAS} = 317 is 94% of
 *       emitter declines but ~6% of all seam-roots).</li>
 * </ul>
 *
 * <p><strong>Scope</strong>: plain {@code func} bodies only ({@code RRule}/{@code RReport} synthetics are not in
 * {@code rootElements()}), operation-RHS + conditions + shortcuts (not post-conditions). The cross-inheritance pass
 * checks own-vs-inherited names over resolved <em>data</em>-super chains only (choice-super and inherited-vs-inherited
 * are out of scope). A partial-but-representative view, not a total census.
 * Each seam-root is driven through {@link IRPythonEmitter#emitFunction} (not bare {@code emit}) so a {@code then}-chain
 * body ({@code Let}) is flattened exactly as a real emission would; a decline anywhere in the tree surfaces as the
 * first {@link EmitterException} and is bucketed by a normalized message stem (the exception carries no structured
 * kind). {@code emitFunction} runs the whole tree through {@code emit}, so the bucket is the body's first blocker.
 *
 * <p>Env-gated on both the corpus AND the rune-dsl builtins (linking needs them); skips cleanly when either is
 * absent. Loads each cell fully linked (builtins + transitive deps + version-stamped cell models ->
 * {@code RWorkspace.build}), mirroring the D11 gate's {@code loadCellCorpus} with rune-parser-only APIs.
 *
 * <p><b>Geometry (the IR-train PR-4 retarget):</b> ported from the lab at its {@code ../../phase1-bundle/sources}
 * staging; now resolves THIS repo's {@code test-corpus/} via {@link CorpusWalker} (classfile-derived, cwd-proof) —
 * the same 5-cell frozen-9.83.0 catalogue at {@code test-corpus/<corpus>/<corpus>-<version>/}, with the builtins
 * resolved by the D11 union convention ({@code test-corpus/rune-dsl-builtins/} preferred, the sibling
 * {@code ../rune-dsl/} checkout as per-file fallback). The cell set, dep closures and version stamps mirror the
 * gate's own ({@code ALL_CELLS} / {@code DRR_TO_CDM_VERSION} / {@code DRR_TO_ISO20022_VERSION} /
 * {@code CDM_TO_FPML_VERSION} / {@code versionStamp()}).
 */
class CorpusPythonDeclineCensusTest {

    /**
     * Builtin search roots in priority order (the D11 {@code BUILTINS_SEARCH_ROOTS} convention):
     * the test-corpus builtins clone first, the sibling rune-dsl checkout as fallback; files
     * are unioned per-filename across the existing roots.
     */
    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            CorpusWalker.repoRoot().resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            CorpusWalker.repoRoot().resolve("rune-dsl/rune-runtime/src/main/resources/model"));

    private record CellSpec(String corpus, String version) {
        Path rosettaDir() {
            return CorpusWalker.CORPUS_DIR.resolve(corpus).resolve(corpus + "-" + version)
                    .resolve("rosetta-source/src/main/rosetta");
        }
        String versionStamp() {
            return "iso20022".equals(corpus) ? "${project.version}" : "0.0.0.master-SNAPSHOT";
        }
        @Override public String toString() { return corpus + "/" + version; }
    }

    private static final List<CellSpec> CELLS = List.of(
            new CellSpec("cdm", "5.38.0"),
            new CellSpec("cdm", "6.20.6"),
            new CellSpec("drr", "6.34.1"),
            new CellSpec("iso20022", "1.38.0"),
            new CellSpec("rune-fpml", "2.0.0"));

    /** Transitive dependency closure per cell (mirrors the byte gate's DRR->CDM/ISO, CDM6->fpml pins). */
    private static List<CellSpec> deps(CellSpec cell) {
        if ("drr".equals(cell.corpus())) {
            return List.of(new CellSpec("cdm", "5.38.0"), new CellSpec("iso20022", "1.38.0"));
        }
        if ("cdm".equals(cell.corpus()) && "6.20.6".equals(cell.version())) {
            return List.of(new CellSpec("rune-fpml", "1.5.3"));
        }
        return List.of();
    }

    @Test
    void censusPythonEmitterDeclines() throws IOException {
        Assumptions.assumeTrue(CorpusWalker.corpusExists(),
                "test-corpus not staged at " + CorpusWalker.CORPUS_DIR + " — census skipped");
        Assumptions.assumeTrue(!resolveBuiltinFiles().isEmpty(),
                "rune-dsl builtins absent (searched " + BUILTINS_SEARCH_ROOTS + ") — census skipped");

        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();
        IRPythonEmitter emitter = new IRPythonEmitter();

        int functions = 0, seamRoots = 0, lowered = 0, adaptErrors = 0, emitErrors = 0;
        Map<String, Integer> adapterDeclines = new HashMap<>();   // fork-gated ceiling, by AST family
        Map<String, Integer> emitterDeclines = new HashMap<>();   // THE buildable ranking, by message stem
        Map<String, String> emitterSamples = new LinkedHashMap<>();
        List<String> cellNotes = new ArrayList<>();

        // Cross-inheritance field-collision census (skeptic Finding 1) — needs the linked super-type chain.
        int typesScanned = 0, typesWithSuper = 0;
        List<String> crossInheritanceCollisions = new ArrayList<>();

        for (CellSpec cell : CELLS) {
            List<RModel> ownModels = new ArrayList<>();
            RWorkspace ws;
            try {
                ws = loadCell(cell, ownModels);
            } catch (RuntimeException | IOException e) {
                cellNotes.add(cell + " FAILED to load: " + e.getClass().getSimpleName() + " " + e.getMessage());
                continue;
            }
            cellNotes.add(cell + " loaded (" + ownModels.size() + " own models)");

            for (RModel model : ownModels) {
                for (var el : model.rootElements()) {
                    if (el instanceof RFunction fn) {
                        functions++;
                        List<String> params = fn.inputs().stream().map(RAttribute::name).toList();
                        for (RExpression seam : seamRoots(fn)) {
                            if (seam == null) {
                                continue;
                            }
                            seamRoots++;
                            Optional<IRExpr> ir;
                            try {
                                ir = adapter.adapt(seam, ws);
                            } catch (RuntimeException ex) {
                                adaptErrors++;
                                continue;
                            }
                            if (ir.isEmpty()) {
                                adapterDeclines.merge(seam.getClass().getSimpleName(), 1, Integer::sum);
                                continue;
                            }
                            try {
                                emitter.emitFunction("_probe", params, ir.get());
                                lowered++;
                            } catch (EmitterException ex) {
                                String key = familyKey(ex.getMessage());
                                emitterDeclines.merge(key, 1, Integer::sum);
                                emitterSamples.putIfAbsent(key, cell + " " + fn.name() + ": " + ex.getMessage());
                            } catch (RuntimeException ex) {
                                emitErrors++;
                                emitterSamples.putIfAbsent("ERROR:" + ex.getClass().getSimpleName(),
                                        cell + " " + fn.name() + ": " + ex.getClass().getSimpleName() + " " + ex.getMessage());
                            }
                        }
                    } else if (el instanceof RDataType dt) {
                        typesScanned++;
                        if (checkCrossInheritance(dt, crossInheritanceCollisions)) {
                            typesWithSuper++;
                        }
                    }
                }
            }
        }

        System.out.println(report(functions, seamRoots, lowered, adaptErrors, emitErrors,
                adapterDeclines, emitterDeclines, emitterSamples, cellNotes,
                typesScanned, typesWithSuper, crossInheritanceCollisions));

        // Non-vacuity: the linked walk must have seen the corpus (a broken load would make every count meaningless).
        assertTrue(functions > 2000 && seamRoots > 2000 && typesScanned > 4000 && typesWithSuper > 500,
                "census read too little to be trustworthy: " + functions + " functions, " + seamRoots
                        + " seam-roots, " + typesScanned + " types, " + typesWithSuper + " with resolved super-type");
    }

    /** The expression seam-roots of a function the Python emitter would need to lower: operation RHS, condition, shortcut. */
    private static List<RExpression> seamRoots(RFunction fn) {
        List<RExpression> roots = new ArrayList<>();
        fn.operations().forEach(op -> roots.add(op.expression()));
        fn.conditions().forEach(c -> roots.add(c.expression()));
        fn.shortcuts().forEach(s -> roots.add(s.expression()));
        return roots;
    }

    /**
     * Records a cross-inheritance field collision: a child's OWN field snake-colliding with a DIFFERENT inherited
     * field name (the emitted {@code class Child(Base)} would silently merge them — unguarded). Returns whether the
     * type had a resolved super-type (a non-vacuity witness that the linked walk works).
     */
    private static boolean checkCrossInheritance(RDataType dt, List<String> out) {
        Map<String, String> inheritedBySnake = new HashMap<>();  // snakeName -> one inherited raw name
        boolean hasSuper = false;
        try {
            Optional<RDataType> sup = dt.superType();
            while (sup.isPresent()) {
                hasSuper = true;
                for (RAttribute a : sup.get().attributes()) {
                    snakeOrNull(a.name()).ifPresent(s -> inheritedBySnake.putIfAbsent(s, a.name()));
                }
                sup = sup.get().superType();
            }
        } catch (RuntimeException e) {
            return false; // unresolved super-type chain (under-linking) — cannot measure this type
        }
        for (RAttribute own : dt.attributes()) {
            String o = own.name();
            snakeOrNull(o).ifPresent(s -> {
                String inheritedRaw = inheritedBySnake.get(s);
                if (inheritedRaw != null && !inheritedRaw.equals(o)) {
                    out.add(dt.name() + ": own '" + o + "' collides with inherited '" + inheritedRaw + "' -> " + s);
                }
            });
        }
        return hasSuper;
    }

    private static Optional<String> snakeOrNull(String name) {
        try {
            return Optional.of(PythonIdentifiers.snakeName(name));
        } catch (EmitterException e) {
            return Optional.empty();
        }
    }

    /** Normalizes an {@link EmitterException} message to a stable family key (the exception carries no structured kind). */
    private static String familyKey(String m) {
        if (m == null) {
            return "other:null";
        }
        if (m.contains("does not lower IR expression kind ") || m.contains("reference of kind ")) {
            return (m.contains("reference of kind ") ? "reference:" : "kind:") + lastToken(m, "kind ");
        }
        if (m.contains("MULTI-cardinality operand")) return "binaryop:MULTI-operand";
        if (m.contains("over an OPTIONAL operand")) return "binaryop:OPTIONAL-operand";
        if (m.contains("FLATTEN list-op")) return "listop:FLATTEN";
        if (m.contains("APPLY callee is not an IRReference")) return "apply:non-ref-callee";
        if (m.contains("collapsed two parameter names")) return "params:collision";
        if (m.contains("ENUM_VALUE reference type is not an enum")) return "enum-value:non-enum";
        return "other:" + m;
    }

    /** The first whitespace-delimited token after the last occurrence of {@code marker}. */
    private static String lastToken(String m, String marker) {
        String tail = m.substring(m.lastIndexOf(marker) + marker.length()).trim();
        int sp = tail.indexOf(' ');
        return sp < 0 ? tail : tail.substring(0, sp);
    }

    // ----- linked corpus loading (rune-parser-only replica of loadCellCorpus) --------------------

    private static RWorkspace loadCell(CellSpec cell, List<RModel> ownModelsOut) throws IOException {
        List<RModel> models = new ArrayList<>();
        for (Path p : resolveBuiltinFiles()) {
            models.add(AstBuilder.buildFromFile(p));
        }
        for (CellSpec dep : deps(cell)) {
            for (Path p : rosettaFiles(dep.rosettaDir())) {
                models.add(AstBuilder.buildFromFile(p));
            }
        }
        for (Path p : rosettaFiles(cell.rosettaDir())) {
            RModel model = AstBuilder.buildFromFile(p);
            model.setVersion(cell.versionStamp());
            models.add(model);
            ownModelsOut.add(model);
        }
        return RWorkspace.build(models).workspace();
    }

    private static List<Path> rosettaFiles(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList();
        }
    }

    /**
     * Union of builtin {@code .rosetta} files across {@link #BUILTINS_SEARCH_ROOTS}, dedup'd by
     * filename in priority order (the D11 {@code resolveBuiltinFiles} convention — the test-corpus
     * root wins filename collisions; the sibling checkout fills gaps).
     */
    private static List<Path> resolveBuiltinFiles() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> s = Files.walk(root)) {
                s.filter(p -> p.toString().endsWith(".rosetta"))
                        .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        return resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();
    }

    // ----- report --------------------------------------------------------------------------------

    private static String report(int functions, int seamRoots, int lowered, int adaptErrors, int emitErrors,
                                 Map<String, Integer> adapterDeclines, Map<String, Integer> emitterDeclines,
                                 Map<String, String> emitterSamples, List<String> cellNotes,
                                 int typesScanned, int typesWithSuper, List<String> crossInheritance) {
        StringBuilder r = new StringBuilder("\n[DECLINE] === Python emitter decline census (linked corpus) ===\n");
        cellNotes.forEach(n -> r.append("[DECLINE] cell: ").append(n).append('\n'));
        r.append("[DECLINE] functions scanned:     ").append(functions).append('\n');
        r.append("[DECLINE] seam-roots scanned:     ").append(seamRoots).append(" (operation RHS + conditions + shortcuts)\n");
        r.append("[DECLINE] LOWERED ok:             ").append(lowered).append('\n');
        r.append("[DECLINE] adapt errors (throw):   ").append(adaptErrors).append('\n');
        r.append("[DECLINE] emit errors (non-EmitterException throw): ").append(emitErrors).append('\n');
        int adapterTotal = adapterDeclines.values().stream().mapToInt(Integer::intValue).sum();
        r.append("[DECLINE] -- ADAPTER declines (Optional.empty = current bottleneck; top families have NO adapt arm"
                + " = lab-buildable, NOT fork-gated; residue = under-linking) = ")
                .append(adapterTotal).append(" --\n");
        appendSorted(r, adapterDeclines, null);
        int emitterTotal = emitterDeclines.values().stream().mapToInt(Integer::intValue).sum();
        r.append("[DECLINE] === EMITTER declines (EmitterException = the BUILDABLE ranking) = ")
                .append(emitterTotal).append(" ===\n");
        appendSorted(r, emitterDeclines, emitterSamples);
        r.append("[DECLINE] -- cross-inheritance field collisions (skeptic Finding 1; own-vs-inherited, resolved"
                + " data-super chains only — choice-super + inherited-vs-inherited excluded) --\n");
        r.append("[DECLINE] data types scanned:     ").append(typesScanned)
                .append(" (with resolved super-type: ").append(typesWithSuper).append(")\n");
        r.append("[DECLINE] CROSS-INHERITANCE COLLISIONS: ").append(crossInheritance.size()).append('\n');
        crossInheritance.stream().limit(30).forEach(c -> r.append("[DECLINE]     collision: ").append(c).append('\n'));
        return r.toString();
    }

    private static void appendSorted(StringBuilder r, Map<String, Integer> counts, Map<String, String> samples) {
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> {
                    r.append("[DECLINE]     ").append(e.getKey()).append(": ").append(e.getValue());
                    if (samples != null && samples.containsKey(e.getKey())) {
                        r.append("   e.g. ").append(samples.get(e.getKey()));
                    }
                    r.append('\n');
                });
    }
}
