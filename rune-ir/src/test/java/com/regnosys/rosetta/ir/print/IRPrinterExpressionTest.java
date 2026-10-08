package com.regnosys.rosetta.ir.print;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.Let;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.Optionality;
import com.regnosys.rosetta.testutil.IRSamples;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IRPrinterExpressionTest {

    private static final IRPrinter PRINTER = new IRPrinter();
    private static final RMetaAnnotatedType NUMBER =
            RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
    private static final RMetaAnnotatedType BOOLEAN =
            RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);

    @Test
    void printsNumberLiteralWithPlainStringValue() {
        IRLiteral lit = new IRLiteral(IRLiteral.LiteralKind.NUMBER, new BigDecimal("0.050"),
                NodeId.ROOT, NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("IRLiteral.NUMBER(\"0.050\") : number [SINGLE, PRESENT]\n",
                PRINTER.print(lit));
    }

    @Test
    void printsIntLiteral() {
        IRLiteral lit = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(42),
                NodeId.ROOT, NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("IRLiteral.INT(\"42\") : number [SINGLE, PRESENT]\n",
                PRINTER.print(lit));
    }

    @Test
    void printsEmptyLiteralWithSourceAndOptional() {
        IREmptyLiteral e = new IREmptyLiteral(IREmptyLiteral.EmptySource.USER_EMPTY,
                NodeId.ROOT, RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE,
                Optionality.OPTIONAL, SourceRange.NONE);
        assertEquals("IREmptyLiteral.USER_EMPTY : <missing> [SINGLE, OPTIONAL]\n",
                PRINTER.print(e));
    }

    @Test
    void printsVariableWithKindAndName() {
        IRVariable v = new IRVariable("trade", IRVariable.VariableKind.PARAM,
                NodeId.ROOT, RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertEquals("IRVariable.PARAM(\"trade\") : <missing> [SINGLE, PRESENT]\n",
                PRINTER.print(v));
    }

    @Test
    void printsReferenceWithEmptyTargetForSuper() {
        IRReference r = new IRReference("", IRReference.ReferenceKind.SUPER,
                NodeId.ROOT, RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertEquals("IRReference.SUPER(\"\") : <missing> [SINGLE, PRESENT]\n",
                PRINTER.print(r));
    }

    @Test
    void debugIdsAppendsDeterministicNodeId() {
        IRLiteral lit = new IRLiteral(IRLiteral.LiteralKind.BOOLEAN, Boolean.TRUE,
                NodeId.ROOT, BOOLEAN, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("IRLiteral.BOOLEAN(\"true\") @/ : boolean [SINGLE, PRESENT]\n",
                new IRPrinter(true).print(lit));
    }

    @Test
    void escapesQuotesBackslashesAndNewlinesInStringLiteral() {
        // value chars: a " b \ c <newline> d
        IRLiteral lit = new IRLiteral(IRLiteral.LiteralKind.STRING, "a\"b\\c\nd",
                NodeId.ROOT, RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertEquals("IRLiteral.STRING(\"a\\\"b\\\\c\\nd\") : <missing> [SINGLE, PRESENT]\n",
                PRINTER.print(lit));
    }

    @Test
    void escapesTabAndCarriageReturnInStringLiteral() {
        // value chars: x <tab> y <CR> z
        IRLiteral lit = new IRLiteral(IRLiteral.LiteralKind.STRING, "x\ty\rz",
                NodeId.ROOT, RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertEquals("IRLiteral.STRING(\"x\\ty\\rz\") : <missing> [SINGLE, PRESENT]\n",
                new IRPrinter().print(lit));
    }

    @Test
    void printsBinaryOpWithOperandsAsChildren() {
        IRVariable left = new IRVariable("a", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral right = new IRLiteral(IRLiteral.LiteralKind.NUMBER, new java.math.BigDecimal("0.05"),
                NodeId.ROOT, NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp eq = new BinaryOp(
                BinaryOp.BinOp.EQ, left, right, NodeId.ROOT, BOOLEAN,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                BinaryOp.EQ : boolean [SINGLE, PRESENT]
                  IRVariable.PARAM("a") : number [SINGLE, PRESENT]
                  IRLiteral.NUMBER("0.05") : number [SINGLE, PRESENT]
                """, PRINTER.print(eq));
    }

    @Test
    void printsFieldAccessWithBothCardinalities() {
        IRVariable recv = new IRVariable("trade", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess fa = new FieldAccess(
                recv, "rate", NodeId.ROOT, NUMBER, ExpressionCardinality.MULTI,
                ExpressionCardinality.SINGLE, Optionality.OPTIONAL, SourceRange.NONE);
        assertEquals("""
                FieldAccess("rate") : number [card=MULTI, feat=SINGLE, OPTIONAL]
                  IRVariable.PARAM("trade") : <missing> [SINGLE, PRESENT]
                """, PRINTER.print(fa));
    }

    @Test
    void printsExistenceWithNullModifierOmitted() {
        IRVariable arg = new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        Existence ex = new Existence(
                Existence.ExistOp.EXISTS, null, arg, NodeId.ROOT,
                BOOLEAN, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                Existence.EXISTS : boolean [SINGLE, PRESENT]
                  IRVariable.PARAM("x") : <missing> [SINGLE, PRESENT]
                """, PRINTER.print(ex));
    }

    @Test
    void printsExistenceWithModifier() {
        IRVariable arg = new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        Existence ex = new Existence(Existence.ExistOp.EXISTS, Existence.ExistMod.SINGLE, arg, NodeId.ROOT,
                BOOLEAN, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                Existence.EXISTS(SINGLE) : boolean [SINGLE, PRESENT]
                  IRVariable.PARAM("x") : <missing> [SINGLE, PRESENT]
                """, new IRPrinter().print(ex));
    }

    @Test
    void printsConditionalWithElseBranch() {
        IRLiteral cond = new IRLiteral(IRLiteral.LiteralKind.BOOLEAN, Boolean.TRUE, NodeId.ROOT,
                BOOLEAN, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral thenB = new IRLiteral(IRLiteral.LiteralKind.INT, java.math.BigInteger.ONE, NodeId.ROOT,
                NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral elseB = new IRLiteral(IRLiteral.LiteralKind.INT, java.math.BigInteger.TWO, NodeId.ROOT,
                NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRConditional c = new IRConditional(cond, thenB, elseB, NodeId.ROOT,
                NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                IRConditional : number [SINGLE, PRESENT]
                  IRLiteral.BOOLEAN("true") : boolean [SINGLE, PRESENT]
                  IRLiteral.INT("1") : number [SINGLE, PRESENT]
                  IRLiteral.INT("2") : number [SINGLE, PRESENT]
                """, new IRPrinter().print(c));
    }

    @Test
    void printsLetWithBinderInlineAndConditionalNoElseMarker() {
        IRLiteral one = new IRLiteral(IRLiteral.LiteralKind.INT, java.math.BigInteger.ONE, NodeId.ROOT,
                NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable ref = new IRVariable("v", IRVariable.VariableKind.LET_BINDER, NodeId.ROOT,
                NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        Let let = new Let(
                "v", one, ref, NodeId.ROOT, NUMBER, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                Let("v") : number [SINGLE, PRESENT]
                  IRLiteral.INT("1") : number [SINGLE, PRESENT]
                  IRVariable.LET_BINDER("v") : number [SINGLE, PRESENT]
                """, PRINTER.print(let));

        IRConditional cond = new IRConditional(
                new IRLiteral(IRLiteral.LiteralKind.BOOLEAN, Boolean.TRUE, NodeId.ROOT, BOOLEAN,
                        ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE),
                one, null, NodeId.ROOT, NUMBER, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                IRConditional [no-else] : number [SINGLE, PRESENT]
                  IRLiteral.BOOLEAN("true") : boolean [SINGLE, PRESENT]
                  IRLiteral.INT("1") : number [SINGLE, PRESENT]
                """, PRINTER.print(cond));
    }

    @Test
    void printsApplyListOpListConstructAndToStringExactOutput() {
        // G3: exact-output coverage for the four expr kinds the coverage test only non-blank-checked. Each uses
        // the shared minimal IRSamples node (MISSING type -> "<missing>", a single PARAM "x" leaf child); APPLY's
        // children() are the callee then the args, so its FUNCTION-reference callee prints as a child.
        assertEquals("""
                IRApply : <missing> [SINGLE, PRESENT]
                  IRReference.FUNCTION("R") : <missing> [SINGLE, PRESENT]
                  IRVariable.PARAM("x") : <missing> [SINGLE, PRESENT]
                """, PRINTER.print(IRSamples.expr(IRExprKind.APPLY)));
        assertEquals("""
                IRListOp.COUNT : <missing> [SINGLE, PRESENT]
                  IRVariable.PARAM("x") : <missing> [SINGLE, PRESENT]
                """, PRINTER.print(IRSamples.expr(IRExprKind.LIST_OP)));
        assertEquals("""
                IRListConstruct : <missing> [SINGLE, PRESENT]
                  IRVariable.PARAM("x") : <missing> [SINGLE, PRESENT]
                """, PRINTER.print(IRSamples.expr(IRExprKind.LIST_CONSTRUCT)));
        assertEquals("""
                IRToString : <missing> [SINGLE, PRESENT]
                  IRVariable.PARAM("x") : <missing> [SINGLE, PRESENT]
                """, PRINTER.print(IRSamples.expr(IRExprKind.TO_STRING)));
    }
}
