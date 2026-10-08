package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.expressions.binary.*;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.ast.expressions.references.*;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.*;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.types.ExpressionCardinality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardinalityComputerTest {

    private final CardinalityComputer computer = new CardinalityComputer();

    // === Literals are always single ==========================================

    @Test void int_literal_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RIntLiteral()));
    }

    @Test void string_literal_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RStringLiteral()));
    }

    @Test void boolean_literal_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RBooleanLiteral()));
    }

    @Test void empty_literal_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new REmptyLiteral()));
    }

    // === List literal is multi ===============================================

    @Test void list_literal_is_multi() {
        assertEquals(ExpressionCardinality.MULTI, computer.compute(new RListLiteral()));
    }

    // === Operators ============================================================

    @Test void comparison_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RComparisonExpr()));
    }

    @Test void equality_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new REqualityExpr()));
    }

    @Test void logical_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RLogicalExpr()));
    }

    @Test void count_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RCountExpr()));
    }

    @Test void existence_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RExistenceExpr()));
    }

    // === List operations — cardinality depends on receiver / body ============
    // Engine PR #2: ported from upstream CardinalityProvider#caseMapOperation /
    // caseFilterOperation / caseSortOperation. The pre-PR fork unconditionally
    // returned MULTI for all three — wrong for `extract` over a SCALAR receiver
    // with SCALAR body (e.g. `reporting rule R from Foo: extract bar -> baz`),
    // which drove the `List<? extends T>` return-type emission for SCALAR rules.

    @Test void sort_is_multi() {
        // Upstream caseSortOperation: always true (sort produces a list).
        assertEquals(ExpressionCardinality.MULTI, computer.compute(new RSortExpr()));
    }

    @Test void filter_with_no_argument_defaults_to_single() {
        // Defensive default — a bare RFilterExpr with no argument cannot be
        // typed (grammar requires one), but the implementation must not NPE.
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RFilterExpr()));
    }

    @Test void filter_inherits_cardinality_of_argument_multi() {
        RFilterExpr filter = new RFilterExpr();
        filter.setArgument(makeMultiSymbolRef());
        assertEquals(ExpressionCardinality.MULTI, computer.compute(filter));
    }

    @Test void filter_inherits_cardinality_of_argument_single() {
        RFilterExpr filter = new RFilterExpr();
        filter.setArgument(makeSingleSymbolRef());
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(filter));
    }

    @Test void extract_with_no_argument_or_body_defaults_to_single() {
        // Defensive default — grammar requires both, but the impl must not NPE.
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RExtractExpr()));
    }

    @Test void extract_with_scalar_argument_and_scalar_body_is_single() {
        // The EnrichmentDataRule case:
        //   reporting rule R from Foo: extract bar -> baz
        // Both receiver (rule's implicit input, scalar) and body (chain of
        // scalar features) are scalar → result is scalar.
        RExtractExpr extract = new RExtractExpr();
        extract.setArgument(makeSingleSymbolRef());
        extract.setBody(makeInlineFunctionWithBody(makeSingleSymbolRef()));
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(extract));
    }

    @Test void extract_with_multi_argument_is_multi() {
        // Mapping each element of a list produces a list (one body result per
        // input element). Body cardinality doesn't matter here.
        RExtractExpr extract = new RExtractExpr();
        extract.setArgument(makeMultiSymbolRef());
        extract.setBody(makeInlineFunctionWithBody(makeSingleSymbolRef()));
        assertEquals(ExpressionCardinality.MULTI, computer.compute(extract));
    }

    @Test void extract_with_multi_body_is_multi() {
        // A scalar receiver mapped through a multi-returning body still yields
        // multi (the body's list).
        RExtractExpr extract = new RExtractExpr();
        extract.setArgument(makeSingleSymbolRef());
        extract.setBody(makeInlineFunctionWithBody(makeMultiSymbolRef()));
        assertEquals(ExpressionCardinality.MULTI, computer.compute(extract));
    }

    @Test void list_op_flatten_is_multi() {
        var lop = new RListOpExpr();
        lop.setOp(ListOp.FLATTEN);
        assertEquals(ExpressionCardinality.MULTI, computer.compute(lop));
    }

    @Test void list_op_distinct_is_multi() {
        var lop = new RListOpExpr();
        lop.setOp(ListOp.DISTINCT);
        assertEquals(ExpressionCardinality.MULTI, computer.compute(lop));
    }

    // === List operations that return single ===================================

    @Test void list_op_first_is_single() {
        var lop = new RListOpExpr();
        lop.setOp(ListOp.FIRST);
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(lop));
    }

    @Test void list_op_last_is_single() {
        var lop = new RListOpExpr();
        lop.setOp(ListOp.LAST);
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(lop));
    }

    @Test void list_op_only_element_is_single() {
        var lop = new RListOpExpr();
        lop.setOp(ListOp.ONLY_ELEMENT);
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(lop));
    }

    @Test void list_op_sum_is_single() {
        var lop = new RListOpExpr();
        lop.setOp(ListOp.SUM);
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(lop));
    }

    @Test void reduce_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RReduceExpr()));
    }

    @Test void min_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RMinExpr()));
    }

    @Test void max_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RMaxExpr()));
    }

    // === Symbol reference cardinality from attribute ==========================

    @Test void symbol_ref_to_single_attr_is_single() {
        var card = new RCardinality();
        card.setInf(1); card.setSup(1);
        var tc = new RTypeCall(); tc.setTypeName("int");
        var attr = new RAttribute();
        attr.setName("x"); attr.setTypeCall(tc); attr.setCardinality(card);
        var ref = new RSymbolReference();
        ref.setName("x"); ref.setResolvedSymbol(attr);
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(ref));
    }

    @Test void symbol_ref_to_multi_attr_is_multi() {
        var card = new RCardinality();
        card.setInf(0); card.setUnbounded(true);
        var tc = new RTypeCall(); tc.setTypeName("int");
        var attr = new RAttribute();
        attr.setName("xs"); attr.setTypeCall(tc); attr.setCardinality(card);
        var ref = new RSymbolReference();
        ref.setName("xs"); ref.setResolvedSymbol(attr);
        assertEquals(ExpressionCardinality.MULTI, computer.compute(ref));
    }

    // === Constructor is always single ========================================

    @Test void constructor_is_single() {
        assertEquals(ExpressionCardinality.SINGLE, computer.compute(new RConstructorExpr()));
    }

    // === Helpers ==============================================================

    private static RSymbolReference makeSingleSymbolRef() {
        var card = new RCardinality();
        card.setInf(1); card.setSup(1);
        var tc = new RTypeCall(); tc.setTypeName("int");
        var attr = new RAttribute();
        attr.setName("x"); attr.setTypeCall(tc); attr.setCardinality(card);
        var ref = new RSymbolReference();
        ref.setName("x"); ref.setResolvedSymbol(attr);
        return ref;
    }

    private static RSymbolReference makeMultiSymbolRef() {
        var card = new RCardinality();
        card.setInf(0); card.setUnbounded(true);
        var tc = new RTypeCall(); tc.setTypeName("int");
        var attr = new RAttribute();
        attr.setName("xs"); attr.setTypeCall(tc); attr.setCardinality(card);
        var ref = new RSymbolReference();
        ref.setName("xs"); ref.setResolvedSymbol(attr);
        return ref;
    }

    private static RInlineFunction makeInlineFunctionWithBody(
            com.regnosys.rosetta.ast.RExpression body) {
        var fn = new RInlineFunction();
        fn.setBody(body);
        return fn;
    }
}
