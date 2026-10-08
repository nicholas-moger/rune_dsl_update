package com.regnosys.rosetta.ir.json;

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
import java.util.OptionalInt;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IRJsonSerializerExpressionTest {

    private static final RMetaAnnotatedType NUMBER = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
    private static final RMetaAnnotatedType BOOLEAN = RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);

    @Test
    void serializesNumberLiteralValueAsStringWithTypeObject() {
        IRLiteral lit = new IRLiteral(IRLiteral.LiteralKind.NUMBER, new BigDecimal("0.050"),
                NodeId.ROOT, NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "LITERAL",
                  "literalKind": "NUMBER",
                  "value": "0.050",
                  "type": {
                    "name": "number"
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(lit));
    }

    @Test
    void serializesBooleanLiteralAsJsonBoolean() {
        IRLiteral lit = new IRLiteral(IRLiteral.LiteralKind.BOOLEAN, Boolean.TRUE,
                NodeId.ROOT, BOOLEAN, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "LITERAL",
                  "literalKind": "BOOLEAN",
                  "value": true,
                  "type": {
                    "name": "boolean"
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(lit));
    }

    @Test
    void emptyLiteralHasMissingTypeObjectAndOptional() {
        IREmptyLiteral e = new IREmptyLiteral(IREmptyLiteral.EmptySource.USER_EMPTY, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.OPTIONAL, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "EMPTY_LITERAL",
                  "source": "USER_EMPTY",
                  "type": {
                    "missing": true
                  },
                  "cardinality": "SINGLE",
                  "optionality": "OPTIONAL"
                }
                """, new IRJsonSerializer().toJson(e));
    }

    @Test
    void numberTypeWithFractionalDigitsAddsParam() {
        RNumberType n = new RNumberType(OptionalInt.empty(), OptionalInt.of(2),
                java.util.Optional.empty(), java.util.Optional.empty());
        IRLiteral lit = new IRLiteral(IRLiteral.LiteralKind.NUMBER, new BigDecimal("1.00"), NodeId.ROOT,
                RMetaAnnotatedType.withNoMeta(n), ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "LITERAL",
                  "literalKind": "NUMBER",
                  "value": "1.00",
                  "type": {
                    "name": "number",
                    "fractionalDigits": 2
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(lit));
    }

    @Test
    void variableWithMetaSortedAndDebugIdsAddsNodeId() {
        RMetaAnnotatedType withMeta = RMetaAnnotatedType.withMeta(RBasicType.BOOLEAN,
                java.util.List.of("scheme", "location"));
        IRVariable v = new IRVariable("trade", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                withMeta, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "VARIABLE",
                  "name": "trade",
                  "variableKind": "PARAM",
                  "nodeId": "/",
                  "type": {
                    "name": "boolean",
                    "meta": [
                      "location",
                      "scheme"
                    ]
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer(true).toJson(v));
    }

    @Test
    void referenceWithEmptyTargetForSuper() {
        IRReference r = new IRReference("", IRReference.ReferenceKind.SUPER, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "REFERENCE",
                  "target": "",
                  "referenceKind": "SUPER",
                  "type": {
                    "missing": true
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(r));
    }

    @Test
    void serializesBinaryOpWithNamedChildren() {
        IRVariable a = new IRVariable("a", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral b = new IRLiteral(IRLiteral.LiteralKind.NUMBER, new java.math.BigDecimal("0.05"),
                NodeId.ROOT, NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp eq = new BinaryOp(
                BinaryOp.BinOp.EQ, a, b, NodeId.ROOT, BOOLEAN,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "BINARY_OP",
                  "op": "EQ",
                  "left": {
                    "kind": "VARIABLE",
                    "name": "a",
                    "variableKind": "PARAM",
                    "type": {
                      "name": "number"
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "right": {
                    "kind": "LITERAL",
                    "literalKind": "NUMBER",
                    "value": "0.05",
                    "type": {
                      "name": "number"
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "type": {
                    "name": "boolean"
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(eq));
    }

    @Test
    void fieldAccessEmitsBothCardinalitiesOnce() {
        IRVariable recv = new IRVariable("trade", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess fa = new FieldAccess(
                recv, "rate", NodeId.ROOT, NUMBER, ExpressionCardinality.MULTI,
                ExpressionCardinality.SINGLE, Optionality.OPTIONAL, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "FIELD_ACCESS",
                  "receiver": {
                    "kind": "VARIABLE",
                    "name": "trade",
                    "variableKind": "PARAM",
                    "type": {
                      "missing": true
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "feature": "rate",
                  "featureCardinality": "SINGLE",
                  "type": {
                    "name": "number"
                  },
                  "cardinality": "MULTI",
                  "optionality": "OPTIONAL"
                }
                """, new IRJsonSerializer().toJson(fa));
    }

    @Test
    void existenceNullModifierAndConditionalNullElseAreExplicitNull() {
        IRVariable x = new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        Existence ex = new Existence(
                Existence.ExistOp.EXISTS, null, x, NodeId.ROOT,
                BOOLEAN, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "EXISTENCE",
                  "op": "EXISTS",
                  "modifier": null,
                  "arg": {
                    "kind": "VARIABLE",
                    "name": "x",
                    "variableKind": "PARAM",
                    "type": {
                      "missing": true
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "type": {
                    "name": "boolean"
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(ex));

        IRLiteral one = new IRLiteral(IRLiteral.LiteralKind.INT, java.math.BigInteger.ONE, NodeId.ROOT,
                NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRConditional cond = new IRConditional(
                new IRLiteral(IRLiteral.LiteralKind.BOOLEAN, Boolean.TRUE, NodeId.ROOT, BOOLEAN,
                        ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE),
                one, null, NodeId.ROOT, NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "CONDITIONAL",
                  "condition": {
                    "kind": "LITERAL",
                    "literalKind": "BOOLEAN",
                    "value": true,
                    "type": {
                      "name": "boolean"
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "then": {
                    "kind": "LITERAL",
                    "literalKind": "INT",
                    "value": "1",
                    "type": {
                      "name": "number"
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "else": null,
                  "type": {
                    "name": "number"
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(cond));
    }

    @Test
    void letEmitsBinderInline() {
        IRLiteral one = new IRLiteral(IRLiteral.LiteralKind.INT, java.math.BigInteger.ONE, NodeId.ROOT,
                NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable ref = new IRVariable("v", IRVariable.VariableKind.LET_BINDER, NodeId.ROOT,
                NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        Let let = new Let(
                "v", one, ref, NodeId.ROOT, NUMBER, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("""
                {
                  "kind": "LET",
                  "binder": "v",
                  "value": {
                    "kind": "LITERAL",
                    "literalKind": "INT",
                    "value": "1",
                    "type": {
                      "name": "number"
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "in": {
                    "kind": "VARIABLE",
                    "name": "v",
                    "variableKind": "LET_BINDER",
                    "type": {
                      "name": "number"
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "type": {
                    "name": "number"
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(let));
    }

    @Test
    void serializesApplyExactJson() {
        // G3: exact-JSON coverage (previously only key-presence). The shared IRSamples APPLY node: a FUNCTION
        // callee "R" + one PARAM "x" arg, MISSING types ({"missing": true}), SINGLE/PRESENT facts.
        assertEquals("""
                {
                  "kind": "APPLY",
                  "callee": {
                    "kind": "REFERENCE",
                    "target": "R",
                    "referenceKind": "FUNCTION",
                    "type": {
                      "missing": true
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "args": [
                    {
                      "kind": "VARIABLE",
                      "name": "x",
                      "variableKind": "PARAM",
                      "type": {
                        "missing": true
                      },
                      "cardinality": "SINGLE",
                      "optionality": "PRESENT"
                    }
                  ],
                  "type": {
                    "missing": true
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(IRSamples.expr(IRExprKind.APPLY)));
    }

    @Test
    void serializesListOpExactJson() {
        assertEquals("""
                {
                  "kind": "LIST_OP",
                  "op": "COUNT",
                  "child": {
                    "kind": "VARIABLE",
                    "name": "x",
                    "variableKind": "PARAM",
                    "type": {
                      "missing": true
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "type": {
                    "missing": true
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(IRSamples.expr(IRExprKind.LIST_OP)));
    }

    @Test
    void serializesListConstructExactJson() {
        assertEquals("""
                {
                  "kind": "LIST_CONSTRUCT",
                  "elements": [
                    {
                      "kind": "VARIABLE",
                      "name": "x",
                      "variableKind": "PARAM",
                      "type": {
                        "missing": true
                      },
                      "cardinality": "SINGLE",
                      "optionality": "PRESENT"
                    }
                  ],
                  "type": {
                    "missing": true
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(IRSamples.expr(IRExprKind.LIST_CONSTRUCT)));
    }

    @Test
    void serializesToStringExactJson() {
        assertEquals("""
                {
                  "kind": "TO_STRING",
                  "child": {
                    "kind": "VARIABLE",
                    "name": "x",
                    "variableKind": "PARAM",
                    "type": {
                      "missing": true
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "type": {
                    "missing": true
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(IRSamples.expr(IRExprKind.TO_STRING)));
    }

    @Test
    void serializesConstructExactJson() {
        // #494: the shared IRSamples CONSTRUCT node — typeName "T", one attribute "a", no spread,
        // MISSING type, SINGLE/PRESENT facts (the shallow node: no value children by design).
        assertEquals("""
                {
                  "kind": "CONSTRUCT",
                  "typeName": "T",
                  "attributeNames": [
                    "a"
                  ],
                  "spread": false,
                  "type": {
                    "missing": true
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(IRSamples.expr(IRExprKind.CONSTRUCT)));
    }

    @Test
    void serializesLambdaOpExactJson() {
        // #496: the shared IRSamples LAMBDA_OP node — op EXTRACT, binder "a", the LEAF variable
        // as both receiver and body (the deep node: both children load-bearing by design),
        // MISSING type, SINGLE/PRESENT facts.
        assertEquals("""
                {
                  "kind": "LAMBDA_OP",
                  "op": "EXTRACT",
                  "binderName": "a",
                  "receiver": {
                    "kind": "VARIABLE",
                    "name": "x",
                    "variableKind": "PARAM",
                    "type": {
                      "missing": true
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "body": {
                    "kind": "VARIABLE",
                    "name": "x",
                    "variableKind": "PARAM",
                    "type": {
                      "missing": true
                    },
                    "cardinality": "SINGLE",
                    "optionality": "PRESENT"
                  },
                  "type": {
                    "missing": true
                  },
                  "cardinality": "SINGLE",
                  "optionality": "PRESENT"
                }
                """, new IRJsonSerializer().toJson(IRSamples.expr(IRExprKind.LAMBDA_OP)));
    }
}
