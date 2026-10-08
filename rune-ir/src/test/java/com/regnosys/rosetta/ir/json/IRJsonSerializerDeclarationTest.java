package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.adapter.IREnumValueNode;
import com.regnosys.rosetta.ir.adapter.IRFieldNode;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class IRJsonSerializerDeclarationTest {

    private static IRType typeRef(String name) {
        return new IRTypeNode(name, IRKind.STRUCT, List.of(), Optional.empty(), false,
                Optional.empty(), IRMetadata.EMPTY);
    }

    @Test
    void serializesStructDocumentWithVersionAndFields() {
        IRFieldNode bar = new IRFieldNode("bar", typeRef("string"), Cardinality.ONE_TO_ONE,
                Optional.empty(), IRMetadata.EMPTY);
        IRFieldNode baz = new IRFieldNode("baz", typeRef("number"), Cardinality.ZERO_TO_MANY,
                Optional.empty(), IRMetadata.EMPTY);
        IRTypeNode foo = new IRTypeNode("test.model.Foo", IRKind.STRUCT, List.of(bar, baz),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY);

        assertEquals("""
                {
                  "irFormatVersion": 1,
                  "nodes": [
                    {
                      "kind": "STRUCT",
                      "name": "test.model.Foo",
                      "isAbstract": false,
                      "baseType": null,
                      "fields": [
                        {
                          "kind": "FIELD",
                          "name": "bar",
                          "type": "string",
                          "cardinality": "ONE_TO_ONE"
                        },
                        {
                          "kind": "FIELD",
                          "name": "baz",
                          "type": "number",
                          "cardinality": "ZERO_TO_MANY"
                        }
                      ]
                    }
                  ]
                }
                """, new IRJsonSerializer().toJson(List.of(foo)));
    }

    @Test
    void serializesChoiceWithBaseTypeNullAndEnumWithDisplayName() {
        IRTypeNode choice = new IRTypeNode("test.model.PaymentMethod", IRKind.CHOICE,
                List.of(new IRFieldNode("CashPayment", typeRef("CashPayment"), Cardinality.ONE_TO_ONE,
                        Optional.empty(), IRMetadata.EMPTY)),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY);
        assertEquals("""
                {
                  "kind": "CHOICE",
                  "name": "test.model.PaymentMethod",
                  "isAbstract": false,
                  "baseType": null,
                  "fields": [
                    {
                      "kind": "FIELD",
                      "name": "CashPayment",
                      "type": "CashPayment",
                      "cardinality": "ONE_TO_ONE"
                    }
                  ]
                }
                """, new IRJsonSerializer().toJson(choice));

        IREnumValue up = new IREnumValueNode("Up", Optional.empty(), Optional.empty(), IRMetadata.EMPTY);
        IREnumValue down = new IREnumValueNode("Down", Optional.of("DOWN"), Optional.empty(), IRMetadata.EMPTY);
        IREnumNode e = new IREnumNode("test.model.DirectionEnum", List.of(up, down),
                Optional.empty(), IRMetadata.EMPTY);
        assertEquals("""
                {
                  "kind": "ENUM",
                  "name": "test.model.DirectionEnum",
                  "values": [
                    {
                      "kind": "ENUM_VALUE",
                      "name": "Up",
                      "displayName": null
                    },
                    {
                      "kind": "ENUM_VALUE",
                      "name": "Down",
                      "displayName": "DOWN"
                    }
                  ]
                }
                """, new IRJsonSerializer().toJson(e));
    }

    @Test
    void serializesBaseTypeNameWhenPresent() {
        IRTypeNode child = new IRTypeNode("test.model.Child", IRKind.STRUCT,
                List.of(new IRFieldNode("extra", typeRef("int"), Cardinality.ZERO_TO_ONE,
                        Optional.empty(), IRMetadata.EMPTY)),
                Optional.of(typeRef("Base")), false, Optional.empty(), IRMetadata.EMPTY);
        assertEquals("""
                {
                  "kind": "STRUCT",
                  "name": "test.model.Child",
                  "isAbstract": false,
                  "baseType": "Base",
                  "fields": [
                    {
                      "kind": "FIELD",
                      "name": "extra",
                      "type": "int",
                      "cardinality": "ZERO_TO_ONE"
                    }
                  ]
                }
                """, new IRJsonSerializer().toJson(child));
    }

    // ── the type gate (v3.3 seat 7, PR #643) ───────────────────────────────────────────────────

    /**
     * THE HAND-BUILT GOLDEN of a {@code typeAlias} DECLARATION: every member of the new node kind at
     * a non-default value, in the writer's own order — kind, name, baseType, the baseType reference
     * facts (the collapsed chain included, because this alias's body names ANOTHER alias), the
     * arguments the body wrote, the declared parameters, then the declaration facts.
     *
     * <p>Able to fail: move any member of {@code IRJsonSerializer.typeAliasJson} or
     * {@code effectiveBaseJson} / {@code typeParameterJson} — the byte comparison fails.
     */
    @Test
    void serializesATypeAliasDeclarationWithItsCollapsedBodyParametersAndArguments() {
        IRTypeNode alias = new IRTypeNode("test.model.Max3Int", IRKind.TYPE_ALIAS, List.of(),
                Optional.of(IRTypeNode.reference("int", IRKind.TYPE_ALIAS, Optional.of("test.model"),
                        Optional.of("int"),
                        Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "number", Optional.empty(),
                                List.of(new IRTypeArgument("digits", Optional.empty(),
                                                Optional.of("3"), false),
                                        new IRTypeArgument("min", Optional.empty(),
                                                Optional.of("1.5"), true)))))),
                false, Optional.empty(), IRMetadata.EMPTY,
                Optional.of("test.model"), Optional.empty(), Optional.of("at most three digits"),
                List.of(), List.of(), List.of(Optional.of("Positive")),
                Optional.empty(),
                List.of(new IRTypeParameter("digits",
                        IRTypeNode.reference("int", IRKind.BASIC_TYPE, Optional.empty(),
                                Optional.of("int")),
                        Optional.of("the digit count"),
                        List.of(new IRTypeArgument("max", Optional.empty(), Optional.of("99"), false)))),
                List.of(new IRTypeArgument("digits", Optional.of("digits"), Optional.empty(), false),
                        new IRTypeArgument("fractionalDigits", Optional.empty(),
                                Optional.of("0"), false)));

        assertEquals("""
                {
                  "kind": "TYPE_ALIAS",
                  "name": "test.model.Max3Int",
                  "baseType": "int",
                  "baseTypeKind": "TYPE_ALIAS",
                  "baseTypeNamespace": "test.model",
                  "baseTypeResolvedName": "int",
                  "baseTypeEffectiveBase": {
                    "kind": "BASIC_TYPE",
                    "name": "number",
                    "arguments": [
                      {
                        "parameter": "digits",
                        "literalValue": "3"
                      },
                      {
                        "parameter": "min",
                        "literalValue": "1.5",
                        "negated": true
                      }
                    ]
                  },
                  "baseTypeArguments": [
                    {
                      "parameter": "digits",
                      "nameValue": "digits"
                    },
                    {
                      "parameter": "fractionalDigits",
                      "literalValue": "0"
                    }
                  ],
                  "typeParameters": [
                    {
                      "name": "digits",
                      "type": "int",
                      "typeKind": "BASIC_TYPE",
                      "typeResolvedName": "int",
                      "typeArguments": [
                        {
                          "parameter": "max",
                          "literalValue": "99"
                        }
                      ],
                      "definition": "the digit count"
                    }
                  ],
                  "namespace": "test.model",
                  "definition": "at most three digits",
                  "conditionNames": [
                    "Positive"
                  ]
                }
                """, new IRJsonSerializer().toJson(alias));
    }

    /**
     * A FIELD whose declared type IS an alias: the collapsed chain rides the reference facts under the
     * {@code type} prefix, after {@code typeResolvedName}, and a builtin leaf writes no
     * {@code namespace}.
     */
    @Test
    void serializesAFieldWhoseTypeReferenceCarriesAnEffectiveBase() {
        IRFieldNode field = new IRFieldNode("rate",
                IRTypeNode.reference("Max3Int", IRKind.TYPE_ALIAS, Optional.of("test.model"),
                        Optional.of("Max3Int"),
                        Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "number", Optional.empty(),
                                List.of(new IRTypeArgument("digits", Optional.empty(),
                                        Optional.of("3"), false))))),
                Cardinality.ONE_TO_ONE, Optional.empty(), IRMetadata.EMPTY);

        assertEquals("""
                {
                  "kind": "FIELD",
                  "name": "rate",
                  "type": "Max3Int",
                  "cardinality": "ONE_TO_ONE",
                  "typeKind": "TYPE_ALIAS",
                  "typeNamespace": "test.model",
                  "typeResolvedName": "Max3Int",
                  "typeEffectiveBase": {
                    "kind": "BASIC_TYPE",
                    "name": "number",
                    "arguments": [
                      {
                        "parameter": "digits",
                        "literalValue": "3"
                      }
                    ]
                  }
                }
                """, new IRJsonSerializer().toJson(field));
    }

    /**
     * THE NON-DEFAULT RULE for the type gate's members: a node with no alias anywhere writes none of
     * them — a RESOLVED non-alias reference still ends at {@code typeResolvedName}, so every v1
     * document's bytes stand.
     *
     * <p>Able to fail: write {@code <prefix>EffectiveBase} unconditionally in
     * {@code addReferenceFacts} — the assertion on the absent member fires.
     */
    @Test
    void aNodeWithNoAliasWritesNoneOfTheTypeGatesMembers() {
        IRTypeNode struct = new IRTypeNode("test.model.Foo", IRKind.STRUCT,
                List.of(new IRFieldNode("bar",
                        IRTypeNode.reference("DirectionEnum", IRKind.ENUM, Optional.of("test.model"),
                                Optional.of("DirectionEnum")),
                        Cardinality.ONE_TO_ONE, Optional.empty(), IRMetadata.EMPTY)),
                Optional.of(IRTypeNode.reference("Base", IRKind.STRUCT, Optional.of("test.model"),
                        Optional.of("Base"))),
                false, Optional.empty(), IRMetadata.EMPTY);

        String json = new IRJsonSerializer().toJson(struct);
        assertFalse(json.contains("EffectiveBase"), json);
        assertFalse(json.contains("typeParameters"), json);
        assertFalse(json.contains("baseTypeArguments"), json);
        assertEquals("""
                {
                  "kind": "STRUCT",
                  "name": "test.model.Foo",
                  "isAbstract": false,
                  "baseType": "Base",
                  "fields": [
                    {
                      "kind": "FIELD",
                      "name": "bar",
                      "type": "DirectionEnum",
                      "cardinality": "ONE_TO_ONE",
                      "typeKind": "ENUM",
                      "typeNamespace": "test.model",
                      "typeResolvedName": "DirectionEnum"
                    }
                  ],
                  "baseTypeKind": "STRUCT",
                  "baseTypeNamespace": "test.model",
                  "baseTypeResolvedName": "Base"
                }
                """, json);
    }

    // ── the property gate (v3.3 seat 8, PR #644) ──────────────────────────────

    /**
     * THE HAND-BUILT GOLDEN of a type REFERENCE that walks an alias CHAIN: the collapsed leaf
     * ({@code typeEffectiveBase}) first, then the RUNGS ({@code typeAliasChain}) in the writer's own
     * order — a parameterised outer link with a named and an unnamed condition and their kinds, then
     * a plain inner link that writes name and namespace alone.
     *
     * <p>Able to fail: move any member of {@code IRJsonSerializer.aliasLinkJson}, or write the chain
     * before the leaf in {@code addReferenceFacts} — the byte comparison fails.
     */
    @Test
    void serializesAFieldWhoseTypeReferenceWalksAnAliasChain() {
        IRFieldNode field = new IRFieldNode("rate",
                IRTypeNode.reference("Max3Int", IRKind.TYPE_ALIAS, Optional.of("test.model"),
                        Optional.of("Max3Int"),
                        Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "number", Optional.empty(),
                                List.of(new IRTypeArgument("digits", Optional.empty(),
                                        Optional.of("3"), false)))),
                        List.of(new IRAliasLink("Max3Int", Optional.of("test.model"),
                                        List.of("digits"),
                                        List.of(Optional.of("Positive"), Optional.empty()),
                                        List.of("DataRule", "OneOf")),
                                new IRAliasLink("PositiveInt", Optional.of("test.model"), List.of(),
                                        List.of(), List.of()))),
                Cardinality.ONE_TO_ONE, Optional.empty(), IRMetadata.EMPTY);

        assertEquals("""
                {
                  "kind": "FIELD",
                  "name": "rate",
                  "type": "Max3Int",
                  "cardinality": "ONE_TO_ONE",
                  "typeKind": "TYPE_ALIAS",
                  "typeNamespace": "test.model",
                  "typeResolvedName": "Max3Int",
                  "typeEffectiveBase": {
                    "kind": "BASIC_TYPE",
                    "name": "number",
                    "arguments": [
                      {
                        "parameter": "digits",
                        "literalValue": "3"
                      }
                    ]
                  },
                  "typeAliasChain": [
                    {
                      "name": "Max3Int",
                      "namespace": "test.model",
                      "parameterNames": [
                        "digits"
                      ],
                      "conditionNames": [
                        "Positive",
                        null
                      ],
                      "conditionKinds": [
                        "DataRule",
                        "OneOf"
                      ]
                    },
                    {
                      "name": "PositiveInt",
                      "namespace": "test.model"
                    }
                  ]
                }
                """, new IRJsonSerializer().toJson(field));
    }

    /**
     * THE HAND-BUILT GOLDEN of the MODEL node: every model-level fact at a non-default value, in the
     * writer's own order — kind, name, namespace, definition, version, then the four lists. A
     * signature's inputs and output are FIELD nodes written by the field writer itself (the resolved
     * input carries its reference facts; the unresolved output carries its annotation and nothing
     * else), and the two with-meta uses show both halves of the record's law: a typed argument with
     * its LITERAL constraints, and a NAMED refusal with no type at all.
     *
     * <p>Able to fail: move or rename any member of {@code IRJsonSerializer.modelJson} and its four
     * sub-shape writers — the byte comparison fails.
     */
    @Test
    void serializesTheModelNodeWithEveryFact() {
        IRType product = IRTypeNode.reference("Product", IRKind.STRUCT, Optional.of("test.model"),
                Optional.of("Product"));
        IRFieldNode input = new IRFieldNode("product", product, Cardinality.ONE_TO_ONE,
                Optional.empty(), IRMetadata.EMPTY);
        IRFieldNode output = new IRFieldNode("trade", typeRef("Trade"), Cardinality.ONE_TO_ONE,
                Optional.empty(), IRMetadata.EMPTY, Optional.empty(), false, List.of(),
                Optional.empty(), List.of(),
                List.of(new IRAnnotationUse("metadata", Optional.of("scheme"), List.of())),
                List.of(), List.of());

        IRModelNode model = new IRModelNode("test.model", Optional.of("the trade model"),
                Optional.of("1.2.3"),
                List.of(new IRQualifiableConfig("IS_PRODUCT", product)),
                List.of(new IRQualificationFunction("Qualify_Swap", product)),
                List.of(new IRFunctionSignature("Create_Trade", List.of(input), Optional.of(output))),
                List.of(new IRWithMetaUse(List.of("scheme"),
                                Optional.of(IRTypeNode.reference("number", IRKind.BASIC_TYPE,
                                        Optional.empty(), Optional.of("number"))),
                                List.of(new IRTypeArgument("digits", Optional.empty(),
                                        Optional.of("18"), false)),
                                Optional.empty()),
                        new IRWithMetaUse(List.of("reference"), Optional.empty(), List.of(),
                                Optional.of("nothing"))),
                Optional.empty(), IRMetadata.EMPTY);

        assertEquals("""
                {
                  "kind": "MODEL",
                  "name": "test.model",
                  "namespace": "test.model",
                  "definition": "the trade model",
                  "version": "1.2.3",
                  "qualifiableConfigs": [
                    {
                      "kind": "IS_PRODUCT",
                      "rootType": "Product",
                      "rootTypeKind": "STRUCT",
                      "rootTypeNamespace": "test.model",
                      "rootTypeResolvedName": "Product"
                    }
                  ],
                  "qualificationFunctions": [
                    {
                      "name": "Qualify_Swap",
                      "firstInputType": "Product",
                      "firstInputTypeKind": "STRUCT",
                      "firstInputTypeNamespace": "test.model",
                      "firstInputTypeResolvedName": "Product"
                    }
                  ],
                  "functionSignatures": [
                    {
                      "name": "Create_Trade",
                      "inputs": [
                        {
                          "kind": "FIELD",
                          "name": "product",
                          "type": "Product",
                          "cardinality": "ONE_TO_ONE",
                          "typeKind": "STRUCT",
                          "typeNamespace": "test.model",
                          "typeResolvedName": "Product"
                        }
                      ],
                      "output": {
                        "kind": "FIELD",
                        "name": "trade",
                        "type": "Trade",
                        "cardinality": "ONE_TO_ONE",
                        "annotations": [
                          {
                            "name": "metadata",
                            "qualifier": "scheme"
                          }
                        ]
                      }
                    }
                  ],
                  "withMetaUses": [
                    {
                      "entryNames": [
                        "scheme"
                      ],
                      "argumentType": "number",
                      "argumentTypeKind": "BASIC_TYPE",
                      "argumentTypeResolvedName": "number",
                      "typeArguments": [
                        {
                          "parameter": "digits",
                          "literalValue": "18"
                        }
                      ]
                    },
                    {
                      "entryNames": [
                        "reference"
                      ],
                      "refusal": "nothing"
                    }
                  ]
                }
                """, new IRJsonSerializer().toJson(model));
    }

    /**
     * THE NON-DEFAULT RULE for the property gate: a model with nothing to say is THREE members long,
     * and a type that states no condition kinds writes no {@code conditionKinds} member.
     */
    @Test
    void aModelWithNoFactsAndATypeWithNoKindsWriteNoOptionalMembers() {
        assertEquals("""
                {
                  "kind": "MODEL",
                  "name": "test.model",
                  "namespace": "test.model"
                }
                """, new IRJsonSerializer().toJson(new IRModelNode("test.model")));

        IRTypeNode struct = new IRTypeNode("test.model.Foo", IRKind.STRUCT, List.of(),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY,
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(),
                List.of(Optional.of("Positive")));
        String json = new IRJsonSerializer().toJson(struct);
        assertFalse(json.contains("conditionKinds"), json);
        assertFalse(json.contains("AliasChain"), json);
    }
}
