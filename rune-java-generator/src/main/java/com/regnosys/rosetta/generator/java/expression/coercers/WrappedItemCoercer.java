package com.regnosys.rosetta.generator.java.expression.coercers;

import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;

import java.util.Set;

/**
 * Coerces the item type inside a wrapper, keeping the same wrapper kind.
 * For example: {@code MapperS<Integer>} → {@code MapperS<BigDecimal>} via
 * {@code .map("Type coercion", item -> convert(item))}.
 *
 * <p>Used internally by {@link WrapperToWrapperCoercer} when item types differ.
 *
 * <h3>Patterns</h3>
 * <ul>
 *   <li>MapperS: {@code .map("Type coercion", item -> convert(item))}</li>
 *   <li>MapperC: {@code .map("Type coercion", item -> convert(item))}</li>
 *   <li>List: {@code .stream().map(item -> convert(item)).collect(Collectors.toList())}</li>
 *   <li>MapperListOfLists: {@code .mapListToList(mapperC -> convert(mapperC))}</li>
 * </ul>
 */
public class WrappedItemCoercer implements TypeCoercer {

    @Override
    public JavaStatementBuilder coerce(
            JavaStatementBuilder expr, JavaType actualType,
            JavaType expectedType, boolean throwOnFail,
            JavaStatementScope scope, JavaTypeUtil typeUtil,
            TypeCoercionService service) {

        JavaType actualItemType = typeUtil.getItemType(actualType);
        JavaType expectedItemType = typeUtil.getItemType(expectedType);

        // If item types are the same, no coercion needed
        if (actualItemType.equals(expectedItemType)) {
            return expr;
        }

        // Build the inner coercion expression: item -> convert(item)
        // We use "item" as a lambda parameter name
        JavaExpression itemParam = JavaExpression.from("item", actualItemType);
        JavaExpression innerCoerced = service.coerceExpression(
                itemParam, actualItemType, expectedItemType, throwOnFail, scope);
        String innerCode = innerCoerced.renderToString();

        // PR-A §9.1 C3a.4.q: structured refs + D4 γ (COLLECTORS) for List
        // stream-collect emission.
        Set<JavaClass<?>> innerRefs = innerCoerced.getRefs();
        Set<JavaClass<?>> innerWildcards = innerCoerced.getStaticWildcardImports();

        // MapperS<A> → MapperS<B> — instance .<B>map method. Byte-faithful to upstream
        // getMapperSItemConversionExpression + convertNullSafe: the <ExpectedItemType>
        // generic witness, a SCOPE-REGISTERED type-derived lambda param, and the
        // null-safe ternary (`p == null ? null : <conv>`; the conversion is applied to
        // the registered param, not the literal `item`).
        //
        // facet meta_coercion_numbering: upstream convertNullSafe REGISTERS the
        // guarded param into the threaded scope (declareAsVariable →
        // createSynonym), so same-desired-name groups in one scope ALL number
        // 0..n-1 at computeActualNames, and a child-scope param whose name a
        // parent took escapes. The fork renders strings eagerly — the group
        // size is unknowable at emission — so the param is registered for
        // DEFERRED naming: a unique sentinel is embedded here and resolved by
        // FunctionExpressionRenderer.finalizeDeferredNames once the statement
        // is final. A unique param keeps its exact pre-registration bytes
        // (group of one resolves to the desired name, escaped iff taken —
        // the same outcome the previous non-mutating disambiguate produced).
        if (typeUtil.isMapperS(actualType)) {
            String param = scope.registerDeferredCoercionParam(
                    toFirstLower(actualItemType.getSimpleName()));
            JavaExpression typedParam = JavaExpression.from(param, actualItemType);
            JavaExpression innerConv = service.coerceExpression(
                    typedParam, actualItemType, expectedItemType, throwOnFail, scope);
            String body = nullSafeBody(param, actualItemType, innerConv.renderToString());
            String witness = expectedItemType.getSimpleName();
            // facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): the deref this
            // arm is about to RENDER, recorded by the emitting branch itself (LAW 69).
            service.recordDeref(expectedItemType);
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString() + ".<" + witness + ">map(\"Type coercion\", "
                                    + param + " -> " + body + ")",
                            expectedType,
                            HandlerHelper.union(HandlerHelper.union(e.getRefs(), innerConv.getRefs()),
                                    witnessRef(expectedItemType)),
                            HandlerHelper.union(e.getStaticWildcardImports(), innerConv.getStaticWildcardImports())));
        }

        // MapperC<A> → MapperC<B>: `.<B>map("Type coercion", p -> conv)` — NO null-safe.
        // Upstream getMapperCItemConversionExpression applies the conversion bare (a
        // MapperC's items are non-null); matches the 399 bare `…getValue()` goldens.
        // facet lambdaNaming M4 (PR #329): the bare param stays UN-numbered (its own
        // lambda-child group of one — the #170 bare-iff-MapperC law) but its ESCAPE now
        // resolves at finalization with the complete method picture, so a hoist local
        // created LATER (golden PriorUSIRule's `final FieldWithMetaString
        // fieldWithMetaString = …` — the #290 whole-output deref, statements after this
        // deref's lambda) forces golden's `_fieldWithMetaString`.
        if (typeUtil.isMapperC(actualType)) {
            String param = scope.registerDeferredLambdaParam(toFirstLower(actualItemType.getSimpleName()));
            JavaExpression typedParam = JavaExpression.from(param, actualItemType);
            JavaExpression innerConv = service.coerceExpression(
                    typedParam, actualItemType, expectedItemType, throwOnFail, scope);
            String witness = expectedItemType.getSimpleName();
            // facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): the deref this
            // arm is about to RENDER, recorded by the emitting branch itself (LAW 69).
            service.recordDeref(expectedItemType);
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString() + ".<" + witness + ">map(\"Type coercion\", "
                                    + param + " -> " + innerConv.renderToString() + ")",
                            expectedType,
                            HandlerHelper.union(HandlerHelper.union(e.getRefs(), innerConv.getRefs()),
                                    witnessRef(expectedItemType)),
                            HandlerHelper.union(e.getStaticWildcardImports(), innerConv.getStaticWildcardImports())));
        }

        // List<A> → List<B> — emits `Collectors.toList()` (D4 γ)
        if (typeUtil.isList(actualType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString() + ".stream().map(item -> " + innerCode + ").collect(Collectors.toList())",
                            expectedType,
                            HandlerHelper.union(HandlerHelper.union(e.getRefs(), innerRefs), Set.of(HandlerHelper.COLLECTORS)),
                            HandlerHelper.union(e.getStaticWildcardImports(), innerWildcards)));
        }

        // MapperListOfLists<A> → MapperListOfLists<B>
        // facet metaWrapperRecovery (PR #613): this arm deliberately does NOT record into the
        // deref ledger (the code-quality review, SF-5): its render carries no `<X>` element
        // witness, so it never matched the element-keyed retired markers the ledger replaces —
        // the ledger's contract is the two Mapper arms above alone (see the ledger's field
        // comment in TypeCoercionService).
        if (typeUtil.isMapperListOfLists(actualType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString() + ".mapListToList(mapperC -> mapperC.map(\"Type coercion\", item -> " + innerCode + "))",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), innerRefs),
                            HandlerHelper.union(e.getStaticWildcardImports(), innerWildcards)));
        }

        return expr;
    }

    /**
     * Register the import for the {@code <ExpectedItemType>} generic witness. Upstream
     * (`getMapperSItemConversionExpression`) embeds the witness as a {@code JavaType} in
     * a {@code StringConcatenationClient}, which auto-registers its import; the fork
     * renders the witness as a raw simple name via {@code getSimpleName()}, losing that
     * registration — so the type must be added to the structured refs explicitly (the
     * sole feeder for expression imports in {@code FunctionGenerator}). Mirrors
     * {@code NavigationHandler.addWitnessTypeRef}. Matters when the conversion body is
     * identity (e.g. the meta-unwrap {@code «p».getValue()}) and therefore carries no
     * ref to the expected item type; {@code java.lang} witnesses are filtered downstream
     * by {@code ImportCollector}. Non-{@code JavaClass} item types (none in practice)
     * contribute nothing.
     */
    private static Set<JavaClass<?>> witnessRef(JavaType expectedItemType) {
        if (expectedItemType instanceof JavaClass<?> c) {
            return Set.of(c);
        }
        return Set.of();
    }

    /** Lower-case the first character (mirror of upstream Xtend {@code .toFirstLower}). */
    private static String toFirstLower(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Wrap a conversion body in the null-safe ternary upstream {@code convertNullSafe}
     * emits for a non-primitive operand: {@code «p» == null ? null : «conv»}. A
     * primitive operand cannot be null, so the conversion is returned bare (mirrors
     * {@code convertNullSafe}'s {@code actual instanceof JavaPrimitiveType} early return).
     */
    private static String nullSafeBody(String param, JavaType actualItemType, String innerCode) {
        if (actualItemType instanceof JavaPrimitiveType) {
            return innerCode;
        }
        return param + " == null ? null : " + innerCode;
    }

}
