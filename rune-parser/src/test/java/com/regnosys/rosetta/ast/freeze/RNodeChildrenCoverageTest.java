package com.regnosys.rosetta.ast.freeze;

import com.regnosys.rosetta.ast.RNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Reflective sweep across every concrete {@link RNode} subclass. For each
 * subclass, lists fields typed as {@code RNode} or
 * {@code Collection<? extends RNode>}. Reports any subclass missing the
 * {@code children()} method or returning a non-List type from it.
 *
 * <p>JDK-only — no {@code org.reflections} dependency added. Subclass list
 * is discovered by walking the source tree at test runtime.
 */
class RNodeChildrenCoverageTest {

    @Test
    void everyConcreteRNodeSubclassDeclaresChildrenMethod() throws Exception {
        List<Class<? extends RNode>> subclasses = discoverConcreteSubclasses();

        assertTrue(
            subclasses.size() >= 10,
            "expected at least 10 concrete RNode subclasses; found " + subclasses.size()
        );

        Map<Class<?>, String> issues = new LinkedHashMap<>();
        for (Class<? extends RNode> sub : subclasses) {
            try {
                Method m = sub.getMethod("children");
                if (!List.class.isAssignableFrom(m.getReturnType())) {
                    issues.put(sub, "children() return type is not List, got "
                        + m.getReturnType().getSimpleName());
                }
            } catch (NoSuchMethodException nsme) {
                issues.put(sub, "no children() method visible");
            }
        }

        if (!issues.isEmpty()) {
            StringBuilder sb = new StringBuilder("RNode subclasses missing or mis-typed children():\n");
            issues.forEach((k, v) -> sb.append("  ").append(k.getName()).append(": ").append(v).append('\n'));
            fail(sb.toString());
        }
    }

    @Test
    void atLeastFiveConcreteSubclassesDeclareRNodeChildFields() throws Exception {
        // Permissive default. PR1b's H7 cross-ref refactor tightens further by
        // auditing every field as part of the SymbolId migration.
        List<Class<? extends RNode>> subclasses = discoverConcreteSubclasses();

        long withChildren = subclasses.stream()
            .map(this::listChildFields)
            .filter(l -> !l.isEmpty())
            .count();

        assertTrue(
            withChildren >= 5,
            "at least 5 concrete RNode subclasses should declare child fields; found " + withChildren
        );
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends RNode>> discoverConcreteSubclasses() throws Exception {
        Path candidate = Path.of("src", "main", "java", "com", "regnosys", "rosetta", "ast");
        if (!Files.exists(candidate)) {
            candidate = Path.of("rune-parser", "src", "main", "java",
                "com", "regnosys", "rosetta", "ast");
        }
        final Path astRoot = candidate;  // effectively final for lambda capture
        assertNotNull(astRoot);
        assertTrue(Files.exists(astRoot),
            "Cannot locate ast/ source root from " + Path.of(".").toAbsolutePath());

        List<Class<? extends RNode>> result = new ArrayList<>();
        try (var walk = Files.walk(astRoot)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        String content = Files.readString(p);
                        if (content.contains("abstract class") || content.contains("sealed interface"))
                            return;
                        if (!content.contains("extends RNode") && !content.matches("(?s).*extends\\s+R\\w+.*"))
                            return;

                        String rel = astRoot.relativize(p).toString()
                            .replace(java.io.File.separatorChar, '.')
                            .replaceAll("\\.java$", "");
                        String fqn = "com.regnosys.rosetta.ast." + rel;
                        Class<?> cls = Class.forName(fqn);
                        if (RNode.class.isAssignableFrom(cls)
                            && !Modifier.isAbstract(cls.getModifiers())) {
                            result.add((Class<? extends RNode>) cls);
                        }
                    } catch (Exception | Error e) {
                        // Skip unparseable / unloadable files
                    }
                });
        }
        return result;
    }

    private List<String> listChildFields(Class<?> clazz) {
        List<String> result = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            Type t = f.getGenericType();
            if (RNode.class.isAssignableFrom(f.getType())) {
                result.add(f.getName());
            } else if (t instanceof ParameterizedType pt
                && Collection.class.isAssignableFrom(f.getType())) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length == 1 && typeIsRNodeAssignable(args[0])) {
                    result.add(f.getName());
                }
            }
        }
        return result;
    }

    /**
     * Returns true if {@code t} ultimately resolves to an {@link RNode} or
     * subclass — handles concrete classes ({@code Collection<RAttribute>}),
     * wildcards with a non-null upper bound ({@code Collection<? extends RNode>}),
     * and type variables with an RNode upper bound ({@code Collection<R>} where
     * {@code R extends RNode}). Matches the class-level Javadoc claim that
     * this test recognises {@code Collection<? extends RNode>} fields.
     *
     * <p><strong>No current callers in the codebase</strong> use the wildcard
     * or type-variable forms — every {@code RNode} subclass declares its
     * Collection field with a concrete element type. This helper exists to
     * align the implementation with the documented contract; do not remove
     * as "unused" without first re-checking via grep
     * (e.g., {@code grep -rn "List<\\? extends R" rune-parser/src/main}).
     */
    private static boolean typeIsRNodeAssignable(Type t) {
        if (t instanceof Class<?> c) {
            return RNode.class.isAssignableFrom(c);
        }
        if (t instanceof WildcardType wt) {
            for (Type ub : wt.getUpperBounds()) {
                if (typeIsRNodeAssignable(ub)) return true;
            }
            return false;
        }
        if (t instanceof TypeVariable<?> tv) {
            for (Type ub : tv.getBounds()) {
                if (typeIsRNodeAssignable(ub)) return true;
            }
            return false;
        }
        if (t instanceof ParameterizedType pt) {
            return typeIsRNodeAssignable(pt.getRawType());
        }
        return false;
    }
}
