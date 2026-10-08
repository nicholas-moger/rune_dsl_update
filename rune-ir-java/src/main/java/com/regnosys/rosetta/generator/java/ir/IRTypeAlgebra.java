package com.regnosys.rosetta.generator.java.ir;

import java.util.Objects;

import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaType;

/**
 * THE PURE STRUCTURAL HALF OF {@code JavaTypeUtil}, AND NOTHING ELSE (v3.3 seat 9, PR #645 commit 10). The
 * coercion algebra {@link IRPojoCompat} ports asks a type table a handful of STRUCTURAL questions - is this a
 * list, what is its item, is the item a model object, which rung of the number ladder is it, wrap this item in a
 * {@code List} - and {@code JavaTypeUtil} answers all of them as a pure function of the {@link JavaType} values it
 * is handed ({@code rune-java-generator/.../types/JavaTypeUtil.java}; its constructor is Guice-free,
 * {@code :46-47}, and it reads no model, no AST node and no {@code GeneratorModel}).
 *
 * <p>This wrapper exists so that the IR route's use of it is DECLARED rather than assumed: the port reaches the
 * table only through the methods below, every one of them listed with the {@code PojoCompatEmitter} line that
 * calls it, and no other member of {@code JavaTypeUtil} - in particular no {@code toJavaType}, no translator
 * seam, no {@code toBuilder} - is reachable from {@link IRPojoCompat} at all. The VALUES the table is handed are
 * the IR's own ({@link IRJavaTypes}), so the answers are the IR's facts, arithmetic'd by a shared, model-free
 * table rather than re-derived by a second implementation (LAW 69).
 *
 * <p><b>THE CALLERS, one line each</b> ({@code CE} = {@code PojoCompatEmitter} at {@code ef66b7b9c}):
 * <ul>
 *   <li>{@link #isList} - {@code CE:516}, {@code CE:517}, {@code CE:870}</li>
 *   <li>{@link #getItemType} - {@code CE:518}, {@code CE:519}, {@code CE:874}</li>
 *   <li>{@link #isRosettaModelObject} - {@code CE:212}, {@code CE:234}, {@code CE:454}, {@code CE:626},
 *       {@code CE:627}, {@code CE:814}</li>
 *   <li>{@link #isInteger} - {@code CE:689}, {@code CE:699}, {@code CE:723}, {@code CE:878};
 *       {@link #isLong} - {@code CE:690}, {@code CE:698}, {@code CE:715}, {@code CE:728}, {@code CE:878};
 *       {@link #isBigInteger} - {@code CE:692}, {@code CE:704}, {@code CE:709}, {@code CE:879};
 *       {@link #isBigDecimal} - {@code CE:879}</li>
 *   <li>{@link #wrapExtends} - {@code CE:440}, {@code CE:445} (the bulk arms' DECLARED wildcard binding, the
 *       #412 recorded corner healed at PR #422)</li>
 *   <li>{@link #wrapExtendsIfNotFinal} - {@code CE:418} (the bulk coercion target)</li>
 *   <li>{@link #hasWildcardArgument} - {@code CE:550}, {@code CE:551} (the wildcard-to-invariant
 *       {@code new ArrayList(«it»)} copy)</li>
 *   <li>{@link #statementAlgebraTypeUtil} - {@code CE:666}, {@code CE:741}, {@code CE:768}, {@code CE:770}: the
 *       three {@code JavaConditionalExpression} / {@code JavaIfThenElseBuilder} constructors take the table
 *       itself, because their convenience form JOINS the two branch types ({@code JavaTypeJoiner}). Every join
 *       the compat algebra reaches is a type with ITSELF or with {@code NULL_TYPE}</li>
 *   <li>{@link #wrapList} - no {@code CE} caller: it is {@link IRJavaTypes#of}'s own list wrap, the invariant
 *       {@code List<X>} the old generator's property type is</li>
 * </ul>
 */
final class IRTypeAlgebra {

    private final JavaTypeUtil typeUtil;

    IRTypeAlgebra(JavaTypeUtil typeUtil) {
        this.typeUtil = Objects.requireNonNull(typeUtil, "typeUtil");
    }

    /** {@code JavaTypeUtil.isList} ({@code :240-245}). */
    boolean isList(JavaType type) {
        return typeUtil.isList(type);
    }

    /** {@code JavaTypeUtil.getItemType} ({@code :208-221}). */
    JavaType getItemType(JavaType type) {
        return typeUtil.getItemType(type);
    }

    /**
     * {@code JavaTypeUtil.isRosettaModelObject} ({@code :236-238}) - {@code getItemType(t).isSubtypeOf(
     * ROSETTA_MODEL_OBJECT)}, which for an {@link IRJavaTypes.IRGeneratedJavaClass} is the RECONCILED
     * {@code itemIsRosettaModelObject} fact and for a classpath class is the classpath's own answer.
     */
    boolean isRosettaModelObject(JavaType type) {
        return typeUtil.isRosettaModelObject(type);
    }

    /** {@code JavaTypeUtil.isInteger} ({@code :296}) - {@code INTEGER.equals(t)} on a classpath class. */
    boolean isInteger(JavaType type) {
        return typeUtil.isInteger(type);
    }

    /** {@code JavaTypeUtil.isLong} ({@code :297}). */
    boolean isLong(JavaType type) {
        return typeUtil.isLong(type);
    }

    /** {@code JavaTypeUtil.isBigInteger} ({@code :298}). */
    boolean isBigInteger(JavaType type) {
        return typeUtil.isBigInteger(type);
    }

    /** {@code JavaTypeUtil.isBigDecimal} ({@code :299}). */
    boolean isBigDecimal(JavaType type) {
        return typeUtil.isBigDecimal(type);
    }

    /** {@code JavaTypeUtil.hasWildcardArgument} ({@code :202-206}). */
    boolean hasWildcardArgument(JavaType type) {
        return typeUtil.hasWildcardArgument(type);
    }

    /** {@code typeUtil.wrapExtends(typeUtil.LIST, item)} ({@code :180-184}) - {@code List<? extends item>}. */
    JavaType wrapExtends(JavaType item) {
        return typeUtil.wrapExtends(typeUtil.LIST, item);
    }

    /** {@code typeUtil.wrapExtendsIfNotFinal(typeUtil.LIST, item)} ({@code :191-198}). */
    JavaType wrapExtendsIfNotFinal(JavaType item) {
        return typeUtil.wrapExtendsIfNotFinal(typeUtil.LIST, item);
    }

    /** {@code typeUtil.wrap(typeUtil.LIST, item)} ({@code :170-173}) - the INVARIANT {@code List<item>}. */
    JavaType wrapList(JavaType item) {
        return typeUtil.wrap(typeUtil.LIST, item);
    }

    /**
     * THE TABLE ITSELF, for the three statement-algebra constructors that take one -
     * {@code JavaConditionalExpression(condition, then, else, typeUtil)} and
     * {@code JavaIfThenElseBuilder(condition, then, else, typeUtil)}. It is exposed for those constructors and
     * for nothing else; the port calls no method on it directly.
     */
    JavaTypeUtil statementAlgebraTypeUtil() {
        return typeUtil;
    }
}
