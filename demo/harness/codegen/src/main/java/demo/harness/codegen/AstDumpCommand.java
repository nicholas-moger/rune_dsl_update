package demo.harness.codegen;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.model.RModel;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@code ast-dump} — a compact recursive JSON rendering of the parsed spotlight model tree,
 * for the dashboard's AST diagram.
 *
 * <p>Shape, per node: {@code {"kind": <class simple name>, "name": <name() if any>,
 * "fields": {...scalar accessors...}, "children": [...]}}. Depth is capped at
 * {@value #MAX_DEPTH}; a node at the cap reports {@code "childrenTruncated": N} instead of
 * recursing.
 *
 * <h2>Why reflection</h2>
 * The AST has ~40 node classes with no common "describe yourself" contract; a hand-written
 * visitor would enumerate them, drift the moment one gains a field, and add nothing a diagram
 * uses. Traversal itself is NOT reflective -- it uses the typed {@link RNode#children()}
 * contract. Reflection is used only to harvest SCALAR accessors, and every invocation is
 * guarded: an accessor that throws (for example a resolver lookup with no attached workspace)
 * is skipped, never fatal, because a diagram must not be able to fail a demo run.
 */
public final class AstDumpCommand {

    private AstDumpCommand() {
    }

    /** Depth cap required by CONTRACTS section-7A. Root is depth 0. */
    public static final int MAX_DEPTH = 12;

    /**
     * Accessors that are structure or bookkeeping rather than content. {@code children} is
     * traversed explicitly; the rest are noise in a diagram or (in {@code workspace}'s case) a
     * back-reference that must never be followed.
     */
    private static final Set<String> SKIPPED_ACCESSORS = Set.of(
            "children", "parent", "sourceRange", "tokenRanges", "metadata",
            "workspace", "hashCode", "toString", "isFrozen", "getClass");

    public static int run(Args args) throws IOException {
        Path src = args.requirePath("src");
        Path builtinsDir = args.requirePath("builtins");
        Path out = args.requirePath("out");
        Path receipt = args.optionalPath("receipt");

        if (!Files.isRegularFile(src)) {
            throw new IOException("--src is not a file: " + src.toAbsolutePath());
        }

        long jvmStart = ManagementFactory.getRuntimeMXBean().getStartTime();
        DemoOut.start("spotlight.ast", "ast-dump " + src.getFileName());

        Path srcRoot = src.toAbsolutePath().getParent();
        CorpusLoader.Loaded loaded = CorpusLoader.load(List.of(srcRoot), builtinsDir);

        String wanted = src.getFileName().toString();
        RModel spotlight = null;
        for (RModel model : loaded.workspace().files()) {
            String file = model.sourceRange() == null ? null : model.sourceRange().file();
            if (file != null && CorpusLoader.fileName(file).equals(wanted)) {
                spotlight = model;
                break;
            }
        }
        if (spotlight == null) {
            DemoOut.error("spotlight model not found in the linked workspace: " + wanted);
            return 3;
        }

        Counter counter = new Counter();
        Map<String, Object> tree = dump(spotlight, 0, counter,
                Collections.newSetFromMap(new IdentityHashMap<>()));

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("astDumpVersion", 1);
        doc.put("source", wanted);
        doc.put("maxDepth", MAX_DEPTH);
        doc.put("nodes", counter.nodes);
        doc.put("truncatedAt", counter.truncated);
        doc.put("root", tree);

        String json = DemoOut.jsonPretty(doc);
        IrDumpCommand.writeFile(out, json);

        long wallMs = System.currentTimeMillis() - jvmStart;
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("nodes", counter.nodes);
        metrics.put("truncatedAt", counter.truncated);
        metrics.put("maxDepth", MAX_DEPTH);
        metrics.put("jsonBytes", json.getBytes(StandardCharsets.UTF_8).length);
        metrics.put("parseMs", loaded.parseMs());
        metrics.put("linkMs", loaded.linkMs());
        metrics.put("wallMs", wallMs);
        metrics.put("exit", 0);
        DemoOut.done(metrics);

        if (receipt != null) {
            Receipts.write(receipt, "spotlight.ast", "spotlight", "Spotlight AST dump",
                    Receipts.commandLine("ast-dump", args), wallMs, metrics,
                    "Scalar node fields are harvested by reflection; accessors that throw are"
                    + " skipped, so a field may be absent rather than wrong.");
        }
        return 0;
    }

    /** Mutable tallies carried down the walk. */
    static final class Counter {
        int nodes;
        int truncated;
    }

    static Map<String, Object> dump(RNode node, int depth, Counter counter, Set<RNode> visited) {
        counter.nodes++;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kind", node.getClass().getSimpleName());

        Map<String, Object> fields = scalarFields(node);
        Object name = fields.remove("name");
        if (name != null) {
            out.put("name", name);
        }
        if (!fields.isEmpty()) {
            out.put("fields", fields);
        }

        List<? extends RNode> children;
        try {
            children = node.children();
        } catch (RuntimeException e) {
            children = List.of();
        }
        if (children == null || children.isEmpty()) {
            return out;
        }
        if (depth >= MAX_DEPTH) {
            counter.truncated++;
            out.put("childrenTruncated", children.size());
            return out;
        }
        List<Object> kids = new ArrayList<>(children.size());
        for (RNode child : children) {
            if (child == null) {
                continue;
            }
            // The AST is a tree, but a derived children() view could in principle expose the
            // same node twice (upstream's Choice does exactly that through two containments).
            // Identity-scoping keeps the dump a record of the source, not of the traversal.
            if (!visited.add(child)) {
                continue;
            }
            kids.add(dump(child, depth + 1, counter, visited));
        }
        if (!kids.isEmpty()) {
            out.put("children", kids);
        }
        return out;
    }

    /**
     * Every public no-arg accessor of {@code node} whose value is a scalar (string, primitive,
     * boxed primitive or enum), plus {@link Optional} wrappers of the same, in method-name
     * order so the dump is deterministic.
     */
    static Map<String, Object> scalarFields(RNode node) {
        List<Method> methods = new ArrayList<>();
        for (Method m : node.getClass().getMethods()) {
            if (m.getParameterCount() != 0
                    || Modifier.isStatic(m.getModifiers())
                    || m.getDeclaringClass() == Object.class
                    || SKIPPED_ACCESSORS.contains(m.getName())) {
                continue;
            }
            Class<?> ret = m.getReturnType();
            if (!isScalar(ret) && ret != Optional.class) {
                continue;
            }
            methods.add(m);
        }
        methods.sort(Comparator.comparing(Method::getName));

        Map<String, Object> fields = new LinkedHashMap<>();
        for (Method m : methods) {
            Object value;
            try {
                value = m.invoke(node);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // An accessor that resolves through the workspace can legitimately throw
                // (unattached node, stale symbol id). A missing field is acceptable in a
                // diagram; an aborted demo run is not.
                continue;
            }
            if (value instanceof Optional<?> opt) {
                if (opt.isEmpty()) {
                    continue;
                }
                value = opt.get();
            }
            if (value == null) {
                continue;
            }
            Object scalar = asScalar(value);
            if (scalar != null) {
                fields.putIfAbsent(m.getName(), scalar);
            }
        }
        return fields;
    }

    private static boolean isScalar(Class<?> type) {
        return type == String.class
                || (type.isPrimitive() && type != void.class)
                || type == Boolean.class || type == Character.class
                || Number.class.isAssignableFrom(type)
                || type.isEnum();
    }

    /** The JSON-renderable form of a scalar value, or {@code null} if it is not one. */
    private static Object asScalar(Object value) {
        if (value instanceof String || value instanceof Boolean || value instanceof Number) {
            return value;
        }
        if (value instanceof Character c) {
            return String.valueOf(c);
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        return null;
    }
}
