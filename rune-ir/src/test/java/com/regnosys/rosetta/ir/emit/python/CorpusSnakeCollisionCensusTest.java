package com.regnosys.rosetta.ir.emit.python;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ir.emit.EmitterException;
import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Corpus invariant + census: on the vendored corpus, step-11's value-level {@code snake_case} transform
 * ({@link PythonIdentifiers#snakeName}) introduces NO collision <em>within a declaration</em> (sibling struct fields,
 * function parameters) and NO collision <em>across functions in a namespace</em> — so it caused no coverage regression
 * on those surfaces and the deferred per-scope uniquification is corpus-absent there. This test both <em>measures</em>
 * (prints a {@code [CENSUS]} report) and <em>asserts</em> the invariant, so a future re-vendor that breaks it fails
 * loudly and surfaces the uniquification decision at that point (decision-log L-130).
 *
 * <p>{@code snakeName} is lossy, so two distinct sibling attribute names could in principle collapse onto one Python
 * identifier ({@code orderId}/{@code orderID} → {@code order_id}); {@link IRPythonDeclarationEmitter}'s
 * {@code requireFresh} guard then <strong>declines the whole struct</strong> rather than emit a duplicate field. The
 * census confirms no source-declared corpus type triggers that (nor the function-param or cross-function analogues).
 *
 * <p><strong>Scope and limits</strong> (what this census deliberately does NOT cover — see decision-log L-130):
 * <ul>
 *   <li><strong>Cross-inheritance field collisions</strong> — a child's own field snake-colliding with an
 *       <em>inherited</em> field (the emitted {@code class Child(Base)} carries both). This is UNGUARDED and silent
 *       (data-object field loss, not a loud decline), needs the LINKED super-type chain to measure, and is the
 *       important follow-up — measured by the separate linked decline/inheritance census, not here.</li>
 *   <li><strong>Body-level per-scope</strong> — param-vs-{@code Let}-binder / local-vs-local collisions. Unguarded /
 *       fail-silent in the verb emitter, but corpus-unreachable by <em>construction</em>: the adapter stamps synthetic
 *       fresh {@code _thenN} binder names ({@code adaptThenChainToLet}), so the residual YAGNI here rests on that
 *       construction argument, NOT on this census (which checks only param-vs-param).</li>
 *   <li><strong>Rule/report functions</strong> — {@code RRule}/{@code RReport} synthesize into {@code RFunction}s
 *       post-link ({@code RFunction.fromRule}/{@code fromReport}); this source walk sees only plain {@code func}
 *       declarations. Their synthetic params ({@code input}/{@code output}) cannot collide; their names feed the
 *       module-level surface, left to the linked census.</li>
 *   <li><strong>Namespace = module</strong> — surface 3 keys on {@code RModel.namespace()}, assuming one Python module
 *       per namespace; a coarser future assembler could merge namespaces (noted, not yet resolvable — the assembler
 *       is unbuilt).</li>
 * </ul>
 *
 * <p>The question this answers: does that guard now fire on real CDM/DRR data types that emitted fine
 * <em>before</em> step-11 (when field names were {@link PythonIdentifiers#sanitize}-only)? To isolate exactly
 * the step-11-introduced cases, each data type is simulated under BOTH regimes:
 * <ul>
 *   <li><strong>snake regime</strong> (current): fields transformed via {@code snakeName}, checked for a
 *       duplicate exactly as {@code requireFresh} would;</li>
 *   <li><strong>sanitize regime</strong> (pre-step-11): fields transformed via {@code sanitize}.</li>
 * </ul>
 * A type is a <strong>step-11 regression</strong> iff it collides under snake but NOT under sanitize. A type
 * that collides under both (a keyword twin like {@code global}/{@code global_}, or a duplicate raw name) was
 * already declined pre-step-11 and is counted separately. A type with a non-ASCII / blank field declines for a
 * bad-identifier reason under both regimes and is excluded from the collision buckets.
 *
 * <p>Uses the real {@link PythonIdentifiers} transforms (hence the in-package placement); the field names are
 * the raw {@code RAttribute.name()} values, which the adapter passes through to {@code IRField.name()} verbatim,
 * so this census sees exactly what {@code emitStruct} iterates. Env-gated: skips cleanly when the corpus is not
 * staged (same convention as the byte gate).
 *
 * <p><b>Geometry (the IR-train PR-4 retarget):</b> ported from the lab at its {@code ../../phase1-bundle/sources}
 * staging; now walks THIS repo's {@code test-corpus/} via {@link CorpusWalker}, scoped to each cell's
 * {@code rosetta-source/src/main/rosetta/} — the SAME 576-file universe the lab walked (its staging held exactly
 * those trees) and the same roots the D11 gate loads. The scoping matters: the repo's cells are FULL repo clones
 * carrying extra {@code .rosetta} outside the source root (fixtures etc. — the six clones alone hold 629 files,
 * 631 with the {@code rune-dsl-builtins/} clone that also lives inside {@code test-corpus/}); the source-root
 * filter excludes both, keeping the census universe gate-true and the lab-calibrated non-vacuity floors exactly
 * comparable.
 */
class CorpusSnakeCollisionCensusTest {

    /** The repo corpus root ({@code test-corpus/}), classfile-derived — cwd-proof. */
    private static final Path CORPUS = CorpusWalker.CORPUS_DIR;

    @Test
    void censusSnakeCaseCollisions() throws IOException {
        Assumptions.assumeTrue(CorpusWalker.corpusExists(),
                "test-corpus not staged at " + CORPUS + " — census skipped");

        List<Path> files;
        try (Stream<Path> walk = Files.walk(CORPUS)) {
            // Cell SOURCE roots only — the gate-true, lab-comparable universe (see class javadoc); the
            // filter also excludes the builtins clone (its path lacks the source-root segment).
            // The chaos cell (v3.2 PR-2) is excluded: this census characterises the REAL
            // vendored corpora for the Python-as-IR lane (its zero-regression invariants were
            // adjudicated against that population, decision-log L-130), chaos deliberately
            // authors rival-name shapes that would contaminate the collision profile, and its
            // 22 BOM files are parse-refused by design — tripping the census's own
            // zero-parse-failure enumeration guard. Chaos's IR-route coverage is the D11 IR
            // walk + the v3.2 L2/L3 machinery, not this lab census.
            files = walk.filter(p -> p.toString().endsWith(".rosetta"))
                    .filter(p -> p.toString().replace('\\', '/').contains("/rosetta-source/src/main/rosetta/"))
                    .filter(p -> !p.toString().replace('\\', '/').contains("/test-corpus/chaos/"))
                    .sorted().toList();
        }

        // Surface 1 — struct sibling fields (IRPythonDeclarationEmitter.emitStruct requireFresh, declines whole struct).
        int typesScanned = 0;
        int badIdentifierDeclineTypes = 0;   // non-ASCII/blank field — declines under both regimes (not a collision)
        int preExistingCollisionTypes = 0;   // collide under sanitize too — keyword twin / duplicate raw name
        List<String> structRegressions = new ArrayList<>();  // collide under snake ONLY — step-11 introduced

        // Non-vacuity witnesses: prove the harness actually read attribute data and snakeName actually transformed
        // it — so a reported 0-collisions is "0 because true", not "0 because the pipeline saw nothing".
        long totalAttrs = 0;
        int typesWith2PlusAttrs = 0;         // only these can collide at all
        int fieldsSnakeChanged = 0;          // snakeName(raw) != raw — the transform is active and non-trivial
        List<String> snakeSamples = new ArrayList<>();

        // Surface 2 — function parameter lists (IRPythonEmitter.emitFunction guard, declines whole function).
        int fnsScanned = 0;
        int fnParamBadId = 0;
        int fnParamPreExisting = 0;
        List<String> fnParamRegressions = new ArrayList<>();

        // Surface 3 — cross-function name collisions within a namespace (unguarded; module-assembler concern —
        // two functions whose names snake_case alike would redefine each other in one Python module). Keyed
        // namespace -> snakeName -> set of distinct raw function names.
        Map<String, Map<String, Set<String>>> fnNamesByNs = new LinkedHashMap<>();

        int parseFailures = 0;
        List<String> parseFailSamples = new ArrayList<>();

        for (Path file : files) {
            RModel model;
            try {
                model = AstBuilder.buildFromFile(file);
            } catch (RuntimeException e) {
                parseFailures++;
                if (parseFailSamples.size() < 8) {
                    // v3.2 seat 4 (PR #625, round 2): the MESSAGE and the cause are recorded beside the class - the chain
                    // s4d of 2026-09-06 was voided by ONE transient read failure here whose IOException no receipt kept
                    parseFailSamples.add(CORPUS.relativize(file) + " — " + e.getClass().getSimpleName()
                            + (e.getMessage() == null ? "" : ": " + e.getMessage())
                            + (e.getCause() == null ? "" : " (cause " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage() + ")"));
                }
                continue;
            }
            String ns = model.namespace();
            for (var el : model.rootElements()) {
                if (el instanceof RDataType dt) {
                    typesScanned++;
                    List<String> raw = dt.attributes().stream().map(RAttribute::name).toList();
                    totalAttrs += raw.size();
                    if (raw.size() >= 2) {
                        typesWith2PlusAttrs++;
                    }
                    for (String n : raw) {
                        try {
                            String s = PythonIdentifiers.snakeName(n);
                            if (!s.equals(n)) {
                                fieldsSnakeChanged++;
                                if (snakeSamples.size() < 6) {
                                    snakeSamples.add(n + " -> " + s);
                                }
                            }
                        } catch (EmitterException ignore) {
                            // counted below via anyDeclines
                        }
                    }
                    // A non-collision (bad-identifier) decline throws under BOTH regimes — exclude from collision buckets.
                    if (anyDeclines(raw)) {
                        badIdentifierDeclineTypes++;
                    } else if (hasDuplicate(raw, PythonIdentifiers::snakeName)) {
                        if (hasDuplicate(raw, PythonIdentifiers::sanitize)) {
                            preExistingCollisionTypes++;               // collides pre-step-11 too
                        } else {
                            structRegressions.add(describe(ns, dt.name(), raw));
                        }
                    }
                } else if (el instanceof RFunction fn) {
                    fnsScanned++;
                    List<String> params = fn.inputs().stream().map(RAttribute::name).toList();
                    if (anyDeclines(params)) {
                        fnParamBadId++;
                    } else if (hasDuplicate(params, PythonIdentifiers::snakeName)) {
                        if (hasDuplicate(params, PythonIdentifiers::sanitize)) {
                            fnParamPreExisting++;
                        } else {
                            fnParamRegressions.add(describe(ns, fn.name() + "(params)", params));
                        }
                    }
                    // Collect the function name for the cross-function (module-level) collision scan.
                    try {
                        String snake = PythonIdentifiers.snakeName(fn.name());
                        fnNamesByNs.computeIfAbsent(ns, k -> new LinkedHashMap<>())
                                .computeIfAbsent(snake, k -> new HashSet<>())
                                .add(fn.name());
                    } catch (EmitterException ignore) {
                        // bad-identifier function name — a pre-existing decline, not a snake collision
                    }
                }
            }
        }

        // Reduce surface 3: a namespace's snakeName mapped from >1 distinct raw function name is a module collision.
        int fnNameCollisions = 0;
        List<String> fnNameCollisionSamples = new ArrayList<>();
        for (var nsEntry : fnNamesByNs.entrySet()) {
            for (var snakeEntry : nsEntry.getValue().entrySet()) {
                if (snakeEntry.getValue().size() > 1) {
                    fnNameCollisions++;
                    if (fnNameCollisionSamples.size() < 20) {
                        fnNameCollisionSamples.add(nsEntry.getKey() + ": {"
                                + String.join(", ", snakeEntry.getValue()) + "} -> " + snakeEntry.getKey());
                    }
                }
            }
        }

        StringBuilder r = new StringBuilder();
        r.append("\n[CENSUS] === snake_case collision census (test-corpus, ").append(files.size()).append(" files) ===\n");
        r.append("[CENSUS] parse failures (files):    ").append(parseFailures).append('\n');
        for (String s : parseFailSamples) {
            r.append("[CENSUS]     parse-fail: ").append(s).append('\n');
        }
        r.append("[CENSUS] -- surface 1: struct fields (guarded, declines struct) --\n");
        r.append("[CENSUS] data types scanned:        ").append(typesScanned).append('\n');
        r.append("[CENSUS] total attributes scanned:  ").append(totalAttrs).append(" (non-vacuity witness)\n");
        r.append("[CENSUS] types with >=2 attributes: ").append(typesWith2PlusAttrs).append(" (collision-eligible)\n");
        r.append("[CENSUS] fields snake-transformed:  ").append(fieldsSnakeChanged)
                .append(" (snakeName != raw — proves the transform is active)\n");
        for (String s : snakeSamples) {
            r.append("[CENSUS]     sample: ").append(s).append('\n');
        }
        r.append("[CENSUS] bad-identifier declines:   ").append(badIdentifierDeclineTypes)
                .append(" (pre-existing; non-ASCII/blank field)\n");
        r.append("[CENSUS] pre-existing collisions:   ").append(preExistingCollisionTypes)
                .append(" (collide under sanitize too — keyword twin / duplicate)\n");
        r.append("[CENSUS] STEP-11 REGRESSIONS:       ").append(structRegressions.size())
                .append(" (collide under snake_case ONLY)\n");
        for (String s : structRegressions) {
            r.append("[CENSUS]     regression: ").append(s).append('\n');
        }
        r.append("[CENSUS] -- surface 2: function params (guarded, declines function) --\n");
        r.append("[CENSUS] functions scanned:         ").append(fnsScanned).append('\n');
        r.append("[CENSUS] bad-identifier declines:   ").append(fnParamBadId).append('\n');
        r.append("[CENSUS] pre-existing collisions:   ").append(fnParamPreExisting).append('\n');
        r.append("[CENSUS] STEP-11 REGRESSIONS:       ").append(fnParamRegressions.size())
                .append(" (params collide under snake_case ONLY)\n");
        for (String s : fnParamRegressions) {
            r.append("[CENSUS]     regression: ").append(s).append('\n');
        }
        r.append("[CENSUS] -- surface 3: cross-function names per namespace (UNGUARDED, module-level) --\n");
        r.append("[CENSUS] namespaces with fns:       ").append(fnNamesByNs.size()).append('\n');
        r.append("[CENSUS] MODULE-LEVEL COLLISIONS:   ").append(fnNameCollisions)
                .append(" (distinct fn names snake_case alike in one namespace)\n");
        for (String s : fnNameCollisionSamples) {
            r.append("[CENSUS]     collision: ").append(s).append('\n');
        }
        System.out.println(r);

        // Non-vacuity: fail if the harness saw materially less than the known corpus — a re-vendor that silently
        // dropped most files would otherwise report a false-green 0. parseFailures MUST be 0 (a spike = partial
        // enumeration); the floors sit ~20% below the observed counts (24635 attrs / 5365 multi-attr types / 18779
        // snaked / 2957 fns) — tight enough to catch a major loss, loose enough for ordinary corpus churn.
        assertEquals(0, parseFailures, "corpus files failed to parse — census enumeration is incomplete: " + parseFailSamples);
        assertTrue(totalAttrs > 20000 && typesWith2PlusAttrs > 4000 && fieldsSnakeChanged > 15000 && fnsScanned > 2000,
                "census read too little to be trustworthy: " + totalAttrs + " attributes, " + typesWith2PlusAttrs
                        + " multi-attr types, " + fieldsSnakeChanged + " snaked fields, " + fnsScanned + " functions");

        // Invariant: snake_case is collision-free within a declaration / per namespace on the vendored corpus. If a
        // future re-vendor breaks this, the failure surfaces the deferred uniquification decision at that point — see
        // decision-log L-130 and the class javadoc. preExistingCollisionTypes is asserted 0 too so a snake-only
        // collision can never hide behind a pre-existing one (the masking case).
        assertEquals(List.of(), structRegressions, "step-11 snake_case now declines struct(s) it did not before");
        assertEquals(0, preExistingCollisionTypes, "a struct has a pre-existing sibling collision (would mask a snake-only pair)");
        assertEquals(List.of(), fnParamRegressions, "step-11 snake_case now declines function param list(s) it did not before");
        assertEquals(0, fnNameCollisions, "distinct function names snake_case alike within a namespace (silent module-level collision)");
    }

    /** True if any name declines under {@code snakeName} (a non-ASCII / blank / bad-char identifier). */
    private static boolean anyDeclines(List<String> raw) {
        for (String n : raw) {
            try {
                PythonIdentifiers.snakeName(n);
            } catch (EmitterException e) {
                return true;
            }
        }
        return false;
    }

    /** Reproduces {@code requireFresh}: true if two names map to the same identifier under {@code transform}. */
    private static boolean hasDuplicate(List<String> raw, Function<String, String> transform) {
        Set<String> seen = new HashSet<>();
        for (String n : raw) {
            if (!seen.add(transform.apply(n))) {
                return true;
            }
        }
        return false;
    }

    /** Renders the colliding groups of a regression type: {@code ns.Type: {orderId, orderID} -> order_id;}. */
    private static String describe(String ns, String type, List<String> raw) {
        Map<String, List<String>> bySnake = new LinkedHashMap<>();
        for (String n : raw) {
            bySnake.computeIfAbsent(PythonIdentifiers.snakeName(n), k -> new ArrayList<>()).add(n);
        }
        StringBuilder sb = new StringBuilder(ns + "." + type + ":");
        bySnake.forEach((snake, names) -> {
            if (names.stream().distinct().count() > 1) {
                sb.append(" {").append(String.join(", ", names)).append("} -> ").append(snake).append(';');
            }
        });
        return sb.toString();
    }
}
