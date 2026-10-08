package com.regnosys.rosetta.generator.java.expression;

import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.rosetta.util.types.JavaReferenceType;
import com.rosetta.util.types.JavaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Engine PR #9 / PR-B: {@link ExpressionCompiler#coerceNavigationReceiver} — the
 * activation lever for the meta-unwrap typed pipeline. It mirrors upstream
 * {@code ExpressionGenerator.attributeCall} L385
 * ({@code addCoercions(receiverCode, MAPPER.wrapExtendsWithoutMeta(receiverCode.expressionType.itemType), scope)}):
 * when a navigation receiver's item type is an {@link RJavaWithMetaValue}, it is
 * meta-stripped (coerced to {@code Mapper<? extends valueType>}) so the dormant
 * {@code WrappedItemCoercer}/{@code ItemToItemCoercer} branch emits the golden
 * {@code .<Value>map("Type coercion", x -> x == null ? null : x.getValue())} before
 * the chain navigates further. Non-meta (or null-typed) receivers are returned
 * unchanged — the byte-flat guarantee for every currently-passing navigation.
 *
 * <p>The bare-coercion mechanics are PR-A-tested ({@code ItemToItemCoercerTest} /
 * {@code WrappedItemCoercerTest}); these tests lock the NEW receiver-type derivation
 * and the no-op contract.
 */
class MetaReceiverCoercionTest {

    private ExpressionCompiler compiler;
    private JavaTypeUtil typeUtil;
    private JavaStatementScope scope;

    @BeforeEach
    void setUp() {
        // Zero-arg compiler: typeUtil + coercionService are wired (gm/tt are null,
        // unused by coerceNavigationReceiver).
        compiler = new ExpressionCompiler();
        typeUtil = new JavaTypeUtil();
        scope = new JavaStatementScope("test", null);
    }

    /** A synthetic generated POJO named {@code Party} in {@code com.rosetta.test}. */
    private JavaReferenceType partyValueType() {
        return (JavaReferenceType) RGeneratedJavaClass.createWithSuperclass(
                JavaPackageName.splitOnDotsAndEscape("com.rosetta.test"), "Party",
                typeUtil.ROSETTA_MODEL_OBJECT);
    }

    @Test
    void meta_receiver_emits_type_coercion_getValue_unwrap() {
        JavaReferenceType party = partyValueType();
        RJavaWithMetaValue meta = RJavaWithMetaValue.create(true, party, typeUtil);
        JavaType receiverType = typeUtil.wrap(typeUtil.MAPPER_S, meta);

        String receiverCode =
                "item.<ReferenceWithMetaParty>map(\"getReportingParty\", "
                        + "reportingSide -> reportingSide.getReportingParty())";
        JavaExpression receiver = JavaExpression.from(receiverCode, receiverType);

        JavaStatementBuilder result = compiler.coerceNavigationReceiver(receiver, scope);

        assertEquals(
                receiverCode
                        + ".<Party>map(\"Type coercion\", referenceWithMetaParty -> "
                        + "referenceWithMetaParty == null ? null : referenceWithMetaParty.getValue())",
                render(result));
    }

    @Test
    void non_meta_receiver_is_returned_unchanged() {
        JavaReferenceType party = partyValueType();
        JavaType receiverType = typeUtil.wrap(typeUtil.MAPPER_S, party); // bare Party, no meta
        JavaExpression receiver = JavaExpression.from(
                "item.<Party>map(\"getParty\", x -> x.getParty())", receiverType);

        assertSame(receiver, compiler.coerceNavigationReceiver(receiver, scope));
    }

    @Test
    void null_typed_receiver_is_returned_unchanged() {
        JavaExpression receiver = JavaExpression.from("item", null);
        assertSame(receiver, compiler.coerceNavigationReceiver(receiver, scope));
    }

    private String render(JavaStatementBuilder builder) {
        String rendered = builder instanceof JavaExpression e
                ? e.renderToString()
                : ((JavaExpression) builder.collapseToSingleExpression(scope)).renderToString();
        // facet meta_coercion_numbering: resolve the guarded arm's deferred
        // param sentinel as the renderer's finalizeDeferredNames does (no-op
        // for renders without a registered guarded coercion param).
        return scope.resolveDeferredCoercionNames(rendered);
    }
}
