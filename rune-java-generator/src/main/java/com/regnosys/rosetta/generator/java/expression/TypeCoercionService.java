package com.regnosys.rosetta.generator.java.expression;

import com.regnosys.rosetta.generator.java.expression.coercers.ItemToItemCoercer;
import com.regnosys.rosetta.generator.java.expression.coercers.ItemToWrapperCoercer;
import com.regnosys.rosetta.generator.java.expression.coercers.WrapperToItemCoercer;
import com.regnosys.rosetta.generator.java.expression.coercers.WrapperToWrapperCoercer;
import com.regnosys.rosetta.generator.java.expression.coercers.WrappedItemCoercer;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaLiteral;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaReferenceType;
import com.rosetta.util.types.JavaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Translates between Java type representations in compiled expressions.
 *
 * <p>Uses quadrant-based dispatch: classify actual and expected types as
 * wrapper/item, then delegate to the appropriate {@link com.regnosys.rosetta.generator.java.expression.coercers.TypeCoercer}
 * strategy.
 *
 * <p>Early exits (in order):
 * <ol>
 *   <li>null actual or expected → return unchanged</li>
 *   <li>item type is NULL_TYPE or VOID → return {@link #emptyValueFor(JavaType)}</li>
 *   <li>identity (actual.equals(expected)) → return unchanged</li>
 *   <li>auto-boxing (int↔Integer, long↔Long, etc.) → return unchanged</li>
 *   <li>primitive VOID → throw {@link IllegalArgumentException}</li>
 *   <li>quadrant dispatch to coercer</li>
 * </ol>
 */
public class TypeCoercionService {

    private final JavaTypeUtil typeUtil;
    private final ItemToItemCoercer itemToItem;
    private final ItemToWrapperCoercer itemToWrapper;
    private final WrapperToItemCoercer wrapperToItem;
    private final WrapperToWrapperCoercer wrapperToWrapper;
    private final WrappedItemCoercer wrappedItem;

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): the deref ledger — one
     * entry per {@code .<X>map("Type coercion", …)} the wrapped-item coercer RENDERS, carrying the
     * element {@code X} it derefs TO. Recorded by the emitting branch itself (LAW 69) so a
     * consumer asks "did a deref of value {@code X} render since I started?" instead of scanning
     * the rendered text for the {@code Type coercion} marker.
     *
     * <p>THE LEDGER'S COVERAGE IS THE COERCER'S TWO MAPPER ARMS, not every emitter of the
     * {@code Type coercion} text (the seat's code-quality review, SF-5): the generator carries
     * some two dozen hand-rolled emitters of the same label (renderers, handlers, the coercer's
     * own {@code mapListToList} arm — see its comment) that do NOT record. A consumer therefore
     * reads {@code false} as "the coercer's Mapper arms rendered no such deref in my window",
     * never as "no deref rendered at all" — the two retired text reads this replaces were scoped
     * to exactly those arms' renders (the census: 0 fires either way at both consumer seats).
     *
     * <p>The ledger is per-{@code TypeCoercionService} (one per compilation) and append-only,
     * except that a compile whose render is DISCARDED rolls its entries back
     * ({@link #truncateDerefLedger}, via {@code ExpressionCompiler.restoreProducerChannels}): a
     * declined block-lambda dispatch attempt, the ladder's text-order rewalk on failure, and the
     * conditional-arm probe (whose own consumer reads its window BEFORE the rollback) — so a
     * MARK-to-now window reflects the KEPT text, not a discarded attempt's. One disclosed
     * residue: a ladder whose text-order rewalk SUCCEEDS keeps the rewalk's entries AND the
     * discarded verdict pass's (the two-pass design predates the ledger), so a window spanning a
     * whole accepted ladder can be a superset over its kept text.
     */
    private final List<JavaType> derefWitnesses = new ArrayList<>();

    public TypeCoercionService(JavaTypeUtil typeUtil) {
        this.typeUtil = typeUtil;
        this.wrappedItem = new WrappedItemCoercer();
        this.itemToItem = new ItemToItemCoercer();
        this.itemToWrapper = new ItemToWrapperCoercer();
        this.wrapperToItem = new WrapperToItemCoercer();
        this.wrapperToWrapper = new WrapperToWrapperCoercer(wrappedItem);
    }

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): record that a
     * {@code Type coercion} deref to {@code valueType} was just RENDERED. Called by
     * {@code WrappedItemCoercer}'s two Mapper arms beside the render they emit (LAW 69).
     */
    public void recordDeref(JavaType valueType) {
        derefWitnesses.add(valueType);
    }

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): a mark in the deref
     * ledger — take one before a compile, pass it to {@link #derefOfValueSince} after.
     */
    public int derefMark() {
        return derefWitnesses.size();
    }

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): roll the ledger back to
     * {@code mark} — the render the entries after it sat beside was DISCARDED (see the field
     * comment). Called only through {@code ExpressionCompiler.restoreProducerChannels}.
     */
    public void truncateDerefLedger(int mark) {
        if (mark >= 0 && mark < derefWitnesses.size()) {
            derefWitnesses.subList(mark, derefWitnesses.size()).clear();
        }
    }

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): did a {@code Type
     * coercion} deref to {@code valueType} render since {@code mark}? Canonical-name equality
     * (a workspace routinely carries several types sharing a simple name — the
     * {@code sameWrapperDenotation} law), falling back to {@code equals} for the non-class
     * types the ledger can also hold. The ONE point every read goes through.
     */
    public boolean derefOfValueSince(int mark, JavaType valueType) {
        if (valueType == null) {
            return false;
        }
        for (int i = Math.max(mark, 0); i < derefWitnesses.size(); i++) {
            JavaType witness = derefWitnesses.get(i);
            if (witness == null) {
                continue;
            }
            if (witness instanceof JavaClass<?> witnessClass
                    && valueType instanceof JavaClass<?> valueClass) {
                if (witnessClass.getCanonicalName() != null
                        && valueClass.getCanonicalName() != null
                        && witnessClass.getCanonicalName().withDots()
                                .equals(valueClass.getCanonicalName().withDots())) {
                    return true;
                }
            } else if (witness.equals(valueType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Coerce {@code expr} from {@code actualType} to {@code expectedType}.
     * Convenience overload — {@code throwOnFail} defaults to {@code true}.
     */
    public JavaStatementBuilder coerce(
            JavaStatementBuilder expr, JavaType actualType,
            JavaType expectedType, JavaStatementScope scope) {
        return coerce(expr, actualType, expectedType, true, scope);
    }

    /**
     * Coerce {@code expr} from {@code actualType} to {@code expectedType}.
     *
     * @param throwOnFail when {@code true}, narrowing conversions throw on overflow;
     *                    when {@code false}, they return {@code null} instead
     */
    public JavaStatementBuilder coerce(
            JavaStatementBuilder expr, JavaType actualType,
            JavaType expectedType, boolean throwOnFail,
            JavaStatementScope scope) {

        // Early exit 1: null actual or expected
        if (actualType == null || expectedType == null) return expr;

        // Early exit 2: item type is NULL_TYPE or VOID (the ONE predicate, isNullOrVoidItem - the
        // seats whose compiled value carries no Java type consult it with the front-end type instead)
        if (isNullOrVoidItem(actualType)) {
            return emptyValueFor(expectedType);
        }

        // Early exit 3: identity
        if (actualType.equals(expectedType)) return expr;

        // Early exit 4: auto-boxing (int ↔ Integer, long ↔ Long, etc.)
        if (actualType instanceof JavaPrimitiveType p
                && p.toReferenceType().equals(expectedType)) return expr;
        if (expectedType instanceof JavaPrimitiveType p
                && p.toReferenceType().equals(actualType)) return expr;

        // Early exit 5: primitive VOID → throw
        if (JavaPrimitiveType.VOID.equals(actualType)) {
            throw new IllegalArgumentException(
                    "Cannot coerce from primitive void to " + expectedType);
        }

        // Quadrant dispatch
        boolean actualIsWrapper = typeUtil.isWrapper(actualType);
        boolean expectedIsWrapper = typeUtil.isWrapper(expectedType);

        if (actualIsWrapper && expectedIsWrapper) {
            return wrapperToWrapper.coerce(expr, actualType, expectedType,
                    throwOnFail, scope, typeUtil, this);
        } else if (!actualIsWrapper && expectedIsWrapper) {
            return itemToWrapper.coerce(expr, actualType, expectedType,
                    throwOnFail, scope, typeUtil, this);
        } else if (actualIsWrapper && !expectedIsWrapper) {
            return wrapperToItem.coerce(expr, actualType, expectedType,
                    throwOnFail, scope, typeUtil, this);
        } else {
            return itemToItem.coerce(expr, actualType, expectedType,
                    throwOnFail, scope, typeUtil, this);
        }
    }

    /**
     * Coerce a {@link JavaExpression} directly, returning a {@link JavaExpression}.
     *
     * <p>Internal composition overload used by coercers (e.g. changing item type
     * inside a wrapper before changing wrapper type). Applies identical early exits
     * and dispatch as {@link #coerce(JavaStatementBuilder, JavaType, JavaType, boolean, JavaStatementScope)},
     * then collapses the result to a single expression.
     */
    public JavaExpression coerceExpression(
            JavaExpression expr, JavaType actualType,
            JavaType expectedType, boolean throwOnFail,
            JavaStatementScope scope) {

        // Early exit 1: null actual or expected
        if (actualType == null || expectedType == null) return expr;

        // Early exit 2: item type is NULL_TYPE or VOID (isNullOrVoidItem, the same predicate as coerce)
        if (isNullOrVoidItem(actualType)) {
            JavaStatementBuilder empty = emptyValueFor(expectedType);
            // emptyValueFor always returns a JavaExpression
            return (JavaExpression) empty;
        }

        // Early exit 3: identity
        if (actualType.equals(expectedType)) return expr;

        // Early exit 4: auto-boxing
        if (actualType instanceof JavaPrimitiveType p
                && p.toReferenceType().equals(expectedType)) return expr;
        if (expectedType instanceof JavaPrimitiveType p
                && p.toReferenceType().equals(actualType)) return expr;

        // Quadrant dispatch — same as coerce(), but collapse to single expression
        boolean actualIsWrapper = typeUtil.isWrapper(actualType);
        boolean expectedIsWrapper = typeUtil.isWrapper(expectedType);

        JavaStatementBuilder result;
        if (actualIsWrapper && expectedIsWrapper) {
            result = wrapperToWrapper.coerce(expr, actualType, expectedType,
                    throwOnFail, scope, typeUtil, this);
        } else if (!actualIsWrapper && expectedIsWrapper) {
            result = itemToWrapper.coerce(expr, actualType, expectedType,
                    throwOnFail, scope, typeUtil, this);
        } else if (actualIsWrapper && !expectedIsWrapper) {
            result = wrapperToItem.coerce(expr, actualType, expectedType,
                    throwOnFail, scope, typeUtil, this);
        } else {
            result = itemToItem.coerce(expr, actualType, expectedType,
                    throwOnFail, scope, typeUtil, this);
        }

        var collapsed = result.collapseToSingleExpression(scope);
        if (collapsed instanceof JavaExpression je) {
            return je;
        }
        // Coercion produced a block builder — this should not happen with current coercers
        // (all use mapExpression which preserves the expression chain), but guard defensively.
        throw new IllegalStateException(
                "coerceExpression: coercer produced a non-expression result for "
                + actualType + " → " + expectedType
                + ". Use coerce() instead if block statements are needed.");
    }

    /**
     * The early-exit-2 predicate of both {@code coerce} overloads, PUBLIC since v3.2 seat 9 (PR #630, F8 / D47 -
     * the {@code nothing} render law): an actual whose ITEM type is the null type or {@code java.lang.Void} is
     * coerced to the expected type's EMPTY ({@link #emptyValueFor}), never converted - upstream's
     * {@code TypeCoercionService.addCoercions} ({@code actual.itemType == NULL_TYPE || actual.itemType.isVoid}).
     * The render seats whose compiled value carries NO Java type (the operation seat and the existence operand -
     * the #433 untyped class) consult THIS predicate with the Java type the translator derives from the front-end
     * type ({@code HandlerHelper.inferredJavaType}), so the law lives in one place (LAW 69).
     */
    public boolean isNullOrVoidItem(JavaType actualType) {
        if (actualType == null) {
            return false;
        }
        JavaType actualItemType = typeUtil.getItemType(actualType);
        return actualItemType != null
                && (actualItemType == JavaReferenceType.NULL_TYPE || typeUtil.isVoid(actualItemType));
    }

    /**
     * Generate an empty/null value expression for the given type.
     *
     * <p>Wrapper types get a typed empty value; other types get {@code null}.
     * Generic type parameters are included in the code string for correct Java output. Since v3.2 seat 9
     * (PR #630) the List arm carries its item witness, {@code Collections.<X>emptyList()} — upstream's form
     * (this javadoc was orphaned by the seat's insertion above it and restored at round 1).
     */
    public JavaStatementBuilder emptyValueFor(JavaType type) {
        if (typeUtil.isList(type)) {
            // v3.2 seat 9 (PR #630, F8 / D47): the empty LIST carries its item witness - upstream's
            // `Collections.<itemType>emptyList()` (TypeCoercionService.empty), the oracle groups
            // void-mapping-render-edge's IntoList / AddInto (`rs = Collections.<Void>emptyList();` /
            // `rs.addAll(Collections.<Void>emptyList());`). The bare `Collections.emptyList()` this arm rendered
            // before reached FOUR pre-seat call sites (round 1's census: `git grep -n "emptyValueFor(" 97b7f9bf7
            // -- '*/src/main'`): the two RENDER sites hand it an ITEM type (the then-arg null guard) and a Mapper
            // wrapper (the navigation seat), so no vendored render carried the bare form - the 25-cell ring at
            // the chain is the byte receipt; the coercer's OWN two early exits (both `coerce` overloads) hand it
            // `expectedType`, whose List form the leg-C port pins at the raw seat (UpstreamTypeCoercionPortTest
            // cases 3 and 5 - RED at commit 8, moved to upstream's form). The item is ALWAYS present here:
            // `isList` admits a parameterised List alone, whose first argument `getItemType` returns (a wildcard's
            // bound, else Object) - round 1 deleted the unreachable bare-form arm. The item's own import is NOT
            // registered, as the MapperS / MapperC arms below do not register theirs; MEASURED over the four call
            // sites at the 25-cell ring and the 26-cell matrix (every chain since commit 8, both routes), not argued
            // for seats no ring reaches (round 2, cq SF-3). The item is read unguarded here because getItemType never
            // answers null on a parameterised List (its wildcard-bound / Object fallback); the two Mapper arms' `?`
            // guards below are pre-seat belts dead under the same contract, left as the ledger rows them (a retirement
            // candidate, banked).
            JavaType itemType = typeUtil.getItemType(type);
            return JavaExpression.from(
                    "Collections.<" + itemType.getSimpleName() + ">emptyList()",
                    type,
                    Set.of(HandlerHelper.COLLECTIONS));
        }
        if (typeUtil.isMapperS(type)) {
            JavaType itemType = typeUtil.getItemType(type);
            String itemName = itemType != null ? itemType.getSimpleName() : "?";
            return JavaExpression.from(
                    "MapperS.<" + itemName + ">ofNull()",
                    type,
                    Set.of(HandlerHelper.MAPPER_S));
        }
        if (typeUtil.isMapperC(type)) {
            JavaType itemType = typeUtil.getItemType(type);
            String itemName = itemType != null ? itemType.getSimpleName() : "?";
            return JavaExpression.from(
                    "MapperC.<" + itemName + ">ofNull()",
                    type,
                    Set.of(HandlerHelper.MAPPER_C));
        }
        if (typeUtil.isMapperListOfLists(type)) {
            // D1 θ resolution (C3a.4.l): MapperListOfLists.ofNull() emission
            // needs structured MAPPER_LIST_OF_LISTS ref. FunctionGenerator
            // L857 substring scanner is deleted in C3c.2.
            return JavaExpression.from(
                    "MapperListOfLists.ofNull()",
                    type,
                    Set.of(HandlerHelper.MAPPER_LIST_OF_LISTS));
        }
        if (typeUtil.isComparisonResult(type)) {
            return JavaExpression.from(
                    "ComparisonResult.ofEmpty()",
                    type,
                    Set.of(HandlerHelper.COMPARISON_RESULT));
        }
        if (JavaPrimitiveType.BOOLEAN.equals(type)) {
            return JavaExpression.from("false", type);
        }
        return JavaLiteral.NULL;
    }
}
