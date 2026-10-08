package demo.harness.analytics;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The demo contract section 5 query set, and the SOURCE OF TRUTH for its semantics.
 *
 * <p>This file is duplicated byte-for-byte (apart from its package line) into the legacy
 * module, so both JVM lanes execute literally the same query code over a {@link Decl} list
 * their own extractor built. The no-JVM Python lane must reproduce the rules below.
 *
 * <h2>Population</h2>
 * Every {@code .rosetta} file loaded, EXCLUDING the two builtins by FILE NAME
 * ({@code basictypes.rosetta}, {@code annotations.rosetta}). File name, not namespace, is the
 * rule: it is what upstream's own {@code RosettaGenerator.ignoredFiles} uses, it needs no
 * resolver, and a lane with only a grammar can apply it. The builtins are still LOADED -- they
 * must be, or nothing resolves -- they are simply not counted.
 *
 * <h2>INDEX</h2>
 * declaration simple name -&gt; ({@code kind}, {@code namespace}, {@code file}). Keyed by SIMPLE
 * name, because that is the only key every lane can produce. Simple names are not unique across
 * a merged corpus, so the first entry wins and the number of collisions is reported rather than
 * hidden.
 *
 * <h2>Q1 -- counts by kind</h2>
 * {@code types} = {@link Decl.Kind#TYPE} ({@code type} declarations; {@code choice} is NOT a
 * data type here -- see {@link Decl.Kind}). {@code enums} = {@link Decl.Kind#ENUM}.
 * {@code functions} = {@link Decl.Kind#FUNCTION}. {@code rules} =
 * {@link Decl.Kind#REPORTING_RULE} only; eligibility rules are excluded.
 *
 * <h2>Q2 -- metadata-annotated data types</h2>
 * The number of {@link Decl.Kind#TYPE} declarations carrying ANY {@code [metadata ...]}
 * annotation on the type itself or on any of its attributes. The annotation NAME is what is
 * matched ({@code metadata}); the qualifier ({@code key}, {@code scheme}, {@code reference},
 * {@code id}, {@code address}, {@code location}, {@code template}) is not restricted -- "ANY
 * [metadata ...]".
 *
 * <h2>Q3 -- reverse reference to {@code Party}</h2>
 * The number of DISTINCT {@link Decl.Kind#TYPE} declarations having at least one attribute
 * whose declared type's SIMPLE name is exactly {@value #TARGET_TYPE_SIMPLE_NAME}. Distinct
 * TYPES, so a type with three {@code Party} attributes counts once. Simple-name matching, so
 * {@code Party} and {@code cdm.base.staticdata.party.Party} both match and no lane needs a
 * resolver to agree.
 *
 * <h2>Q4 -- deepest extends chain</h2>
 * The largest number of {@code extends} EDGES above any {@link Decl.Kind#TYPE} declaration; a
 * type with no super-type scores 0. The walk follows {@link Decl#superSimpleName()} through the
 * index. An edge to a name that is not an indexed {@code type} -- a choice, or an unresolvable
 * name -- still counts as one edge and then ends the walk, so the answer never depends on how
 * completely a lane resolved. A cycle stops the walk at the repeated name; cycles are reported.
 */
public final class Queries {

    private Queries() {
    }

    /** The Q3 target: {@code Party}, cdm base static data party (demo-contract section 5). */
    public static final String TARGET_TYPE_SIMPLE_NAME = "Party";

    /** The builtin file names excluded from every count. */
    public static final List<String> BUILTIN_FILE_NAMES =
            List.of("basictypes.rosetta", "annotations.rosetta");

    /** The section-5 INDEX: simple name -&gt; declaration, first-wins, plus the collision count. */
    public record Index(Map<String, Decl> byName, int collisions) {
    }

    /** The four answers, in the demo-contract section 4 {@code results} shape. */
    public record Results(int types, int enums, int functions, int rules,
                          int metaAnnotatedTypes, int referencesToTarget,
                          int deepestExtendsChain, int extendsCycles) {

        /** Exactly the {@code results} object of the section-4 analytics metrics contract. */
        public Map<String, Object> toResultsMap() {
            Map<String, Object> q1 = new LinkedHashMap<>();
            q1.put("types", types);
            q1.put("enums", enums);
            q1.put("functions", functions);
            q1.put("rules", rules);
            Map<String, Object> q2 = new LinkedHashMap<>();
            q2.put("metaAnnotatedTypes", metaAnnotatedTypes);
            Map<String, Object> q3 = new LinkedHashMap<>();
            q3.put("referencesToTarget", referencesToTarget);
            Map<String, Object> q4 = new LinkedHashMap<>();
            q4.put("deepestExtendsChain", deepestExtendsChain);
            Map<String, Object> results = new LinkedHashMap<>();
            results.put("q1", q1);
            results.put("q2", q2);
            results.put("q3", q3);
            results.put("q4", q4);
            return results;
        }
    }

    /** Per-query wall times, in ms. */
    public record Timings(long indexMs, long q1Ms, long q2Ms, long q3Ms, long q4Ms) {
    }

    /** Whether a source file name is one of the two builtins, and so out of the population. */
    public static boolean isBuiltinFile(String fileName) {
        return fileName != null && BUILTIN_FILE_NAMES.contains(fileName);
    }

    /** Builds the section-5 index. First entry wins on a simple-name collision. */
    public static Index buildIndex(List<Decl> declarations) {
        Map<String, Decl> byName = new LinkedHashMap<>();
        int collisions = 0;
        for (Decl declaration : declarations) {
            if (declaration.name() == null) {
                continue;
            }
            Decl previous = byName.putIfAbsent(declaration.name(), declaration);
            if (previous != null) {
                collisions++;
            }
        }
        return new Index(byName, collisions);
    }

    /** Q1 counts, Q2, Q3 and Q4, each timed separately. */
    public static Outcome run(List<Decl> declarations) {
        long t0 = System.nanoTime();
        Index index = buildIndex(declarations);
        long indexMs = millisSince(t0);

        long t1 = System.nanoTime();
        int types = 0;
        int enums = 0;
        int functions = 0;
        int rules = 0;
        for (Decl declaration : declarations) {
            switch (declaration.kind()) {
                case TYPE -> types++;
                case ENUM -> enums++;
                case FUNCTION -> functions++;
                case REPORTING_RULE -> rules++;
                default -> {
                    // choices, eligibility rules, aliases, reports: indexed, not counted
                }
            }
        }
        long q1Ms = millisSince(t1);

        long t2 = System.nanoTime();
        int metaAnnotatedTypes = 0;
        for (Decl declaration : declarations) {
            if (declaration.kind() == Decl.Kind.TYPE && declaration.metaAnnotated()) {
                metaAnnotatedTypes++;
            }
        }
        long q2Ms = millisSince(t2);

        long t3 = System.nanoTime();
        int referencesToTarget = 0;
        for (Decl declaration : declarations) {
            if (declaration.kind() != Decl.Kind.TYPE) {
                continue;
            }
            for (String attributeType : declaration.attributeTypeSimpleNames()) {
                if (TARGET_TYPE_SIMPLE_NAME.equals(attributeType)) {
                    // DISTINCT types: count this declaration once and stop scanning it.
                    referencesToTarget++;
                    break;
                }
            }
        }
        long q3Ms = millisSince(t3);

        long t4 = System.nanoTime();
        int deepest = 0;
        int cycles = 0;
        for (Decl declaration : declarations) {
            if (declaration.kind() != Decl.Kind.TYPE) {
                continue;
            }
            int depth = 0;
            Set<String> seen = new HashSet<>();
            seen.add(declaration.name());
            String superName = declaration.superSimpleName();
            while (superName != null) {
                depth++;
                if (!seen.add(superName)) {
                    // A cycle. The edge into it is already counted; stop and record.
                    cycles++;
                    break;
                }
                Decl parent = index.byName().get(superName);
                if (parent == null || parent.kind() != Decl.Kind.TYPE) {
                    // The edge counts, but nothing above it is a data type (a choice, or a
                    // name this lane could not place). Ending here keeps the answer
                    // independent of how completely a lane resolved.
                    break;
                }
                superName = parent.superSimpleName();
            }
            deepest = Math.max(deepest, depth);
        }
        long q4Ms = millisSince(t4);

        Results results = new Results(types, enums, functions, rules, metaAnnotatedTypes,
                referencesToTarget, deepest, cycles);
        return new Outcome(results, new Timings(indexMs, q1Ms, q2Ms, q3Ms, q4Ms),
                index.byName().size(), index.collisions());
    }

    /** Everything a lane reports: the answers, the timings, and the index shape. */
    public record Outcome(Results results, Timings timings, int indexSize, int indexCollisions) {
    }

    static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
