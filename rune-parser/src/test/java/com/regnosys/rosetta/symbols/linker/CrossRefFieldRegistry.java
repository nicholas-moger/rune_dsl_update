package com.regnosys.rosetta.symbols.linker;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Test-only cache for {@link CrossRefField} annotation lookups on AST node
 * classes.
 *
 * <p>The reflection-driven invariants ({@code LinkerInvariants},
 * {@code ResolutionAudit}) walk every node in every file and look up
 * {@code @CrossRefField} on every field of every class in every node's
 * class hierarchy. Without caching, this is
 * {@code O(N nodes * H hierarchy * F fields)} reflection calls — measured
 * at {@code >30 minutes} for the combined cdm+drr workspace, exceeding
 * surefire's fork-process timeout.
 *
 * <p>This registry caches the {@code @CrossRefField} fields per concrete
 * node class once, so subsequent walks pay {@code O(F')} per node where
 * {@code F'} is the (typically small) count of cross-ref fields on that
 * class hierarchy. The cache is populated lazily on first lookup and is
 * thread-safe via {@link ConcurrentHashMap#computeIfAbsent}. Each
 * descriptor's {@link Field#setAccessible(boolean)} flag has already been
 * set so the field is immediately readable via {@link Field#get(Object)}.
 *
 * <p><b>Test-only.</b> This class lives under {@code src/test/java} and
 * must NOT be wired into production build paths
 * ({@code Linker.link}, {@code RWorkspace.build}, codegen). M8 LSP must
 * not pay reflection-walk overhead.
 */
public final class CrossRefFieldRegistry {

    private CrossRefFieldRegistry() {}

    /** A single cached cross-ref field paired with its resolved annotation. */
    public record CrossRefFieldDescriptor(Field field, CrossRefField annotation) {}

    private static final ConcurrentMap<Class<?>, List<CrossRefFieldDescriptor>> CACHE =
            new ConcurrentHashMap<>();

    /**
     * Returns the cached list of {@code @CrossRefField} fields declared by
     * {@code cls} or any of its superclasses up to (but not including)
     * {@link Object}. Each returned {@link Field} has already had
     * {@link Field#setAccessible(boolean)} called, so callers can invoke
     * {@link Field#get(Object)} directly without further setup.
     *
     * <p>The returned list is immutable; callers must not mutate it.
     *
     * @param cls a node class (typically the concrete class of an
     *            {@code RNode} instance encountered during a walk); must
     *            not be null
     * @return immutable list of cached descriptors; empty if no cross-ref
     *         fields exist on {@code cls} or its supertypes
     */
    public static List<CrossRefFieldDescriptor> getCrossRefFields(Class<?> cls) {
        return CACHE.computeIfAbsent(cls, CrossRefFieldRegistry::computeForClass);
    }

    private static List<CrossRefFieldDescriptor> computeForClass(Class<?> cls) {
        List<CrossRefFieldDescriptor> result = new ArrayList<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                CrossRefField ann = f.getAnnotation(CrossRefField.class);
                if (ann != null) {
                    f.setAccessible(true);
                    result.add(new CrossRefFieldDescriptor(f, ann));
                }
            }
            current = current.getSuperclass();
        }
        return List.copyOf(result);
    }
}
