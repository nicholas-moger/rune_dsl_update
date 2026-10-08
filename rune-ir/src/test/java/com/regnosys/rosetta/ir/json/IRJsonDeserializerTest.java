package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRModel;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IRJsonDeserializerTest {

    private final IRJsonDeserializer reader = new IRJsonDeserializer();

    @Test
    void deserializesStructFragmentWithFieldAndBase() {
        String json = """
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
                """;
        IRType type = (IRType) reader.fromJsonNode(json);
        assertEquals("test.model.Child", type.name());
        assertEquals(IRKind.STRUCT, type.kind());
        assertEquals("Base", type.baseType().orElseThrow().name());
        IRField field = type.fields().get(0);
        assertEquals("extra", field.name());
        assertEquals("int", field.type().name());
        assertEquals(Cardinality.ZERO_TO_ONE, field.cardinality());
    }

    @Test
    void deserializesDocumentWithOneEnum() {
        String doc = """
                {
                  "irFormatVersion": 1,
                  "nodes": [
                    { "kind": "ENUM", "name": "E", "values": [] }
                  ]
                }
                """;
        List<IRNode> nodes = reader.fromJson(doc);
        assertEquals(1, nodes.size());
        assertEquals(IRKind.ENUM, nodes.get(0).kind());
        assertEquals("E", nodes.get(0).name());
    }

    @Test
    void rejectsUnsupportedVersion() {
        String doc = "{ \"irFormatVersion\": 2, \"nodes\": [] }";
        IRJsonException ex = assertThrows(IRJsonException.class, () -> reader.fromJson(doc));
        assertTrue(ex.getMessage().contains("irFormatVersion"), ex.getMessage());
    }

    @Test
    void rejectsMissingVersion() {
        String doc = """
                { "nodes": [] }
                """;
        IRJsonException ex = assertThrows(IRJsonException.class, () -> reader.fromJson(doc));
        assertTrue(ex.getMessage().contains("irFormatVersion"), ex.getMessage());
    }

    @Test
    void rejectsUnknownKind() {
        IRJsonException ex = assertThrows(IRJsonException.class,
                () -> reader.fromJsonNode("{ \"kind\": \"BOGUS\", \"name\": \"X\" }"));
        assertTrue(ex.getMessage().contains("unknown kind"), ex.getMessage());
    }

    @Test
    void rejectsUnknownMember() {
        String json = "{ \"kind\": \"ENUM\", \"name\": \"E\", \"values\": [], \"extra\": \"x\" }";
        IRJsonException ex = assertThrows(IRJsonException.class, () -> reader.fromJsonNode(json));
        assertTrue(ex.getMessage().contains("unknown member 'extra'"), ex.getMessage());
    }

    @Test
    void rejectsDuplicateKey() {
        String json = "{ \"kind\": \"ENUM\", \"name\": \"E\", \"name\": \"F\", \"values\": [] }";
        IRJsonException ex = assertThrows(IRJsonException.class, () -> reader.fromJsonNode(json));
        assertTrue(ex.getMessage().contains("duplicate member 'name'"), ex.getMessage());
    }

    @Test
    void rejectsMissingRequiredMember() {
        IRJsonException ex = assertThrows(IRJsonException.class,
                () -> reader.fromJsonNode("{ \"kind\": \"ENUM\", \"name\": \"E\" }"));
        assertTrue(ex.getMessage().contains("missing member 'values'"), ex.getMessage());
    }

    /**
     * The type gate (PR #643): a {@code typeAlias} DECLARATION reads back into an
     * {@code IRKind.TYPE_ALIAS} node whose body is a REFERENCE carrying the collapsed chain, with the
     * declared parameters and the arguments the body wrote — and with no fields, no base-less
     * {@code isAbstract} surprise and no resolved name (an alias declaration states none of the three).
     */
    @Test
    void deserializesATypeAliasFragmentWithItsCollapsedBodyAndParameters() {
        String json = """
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
                      { "parameter": "digits", "literalValue": "3" }
                    ]
                  },
                  "baseTypeArguments": [
                    { "parameter": "digits", "nameValue": "digits" }
                  ],
                  "typeParameters": [
                    {
                      "name": "digits",
                      "type": "int",
                      "typeKind": "BASIC_TYPE",
                      "typeResolvedName": "int",
                      "typeArguments": [
                        { "parameter": "max", "literalValue": "99" }
                      ],
                      "definition": "the digit count"
                    }
                  ],
                  "namespace": "test.model"
                }
                """;
        IRType alias = (IRType) reader.fromJsonNode(json);
        assertEquals(IRKind.TYPE_ALIAS, alias.kind());
        assertEquals("test.model.Max3Int", alias.name());
        assertEquals(List.of(), alias.fields());
        assertFalse(alias.isAbstract());
        assertEquals(Optional.empty(), alias.resolvedName());
        assertEquals(Optional.of("test.model"), alias.namespace());
        assertEquals(Optional.empty(), alias.effectiveBase(), "a declaration carries no effective base");

        IRType body = alias.baseType().orElseThrow();
        assertEquals("int", body.name());
        assertEquals(IRKind.TYPE_ALIAS, body.kind());
        IREffectiveBase leaf = body.effectiveBase().orElseThrow();
        assertEquals(IRKind.BASIC_TYPE, leaf.kind());
        assertEquals("number", leaf.qualifiedName());
        assertEquals(Optional.of("3"), leaf.argument("digits"));

        assertEquals(1, alias.baseTypeArguments().size());
        assertEquals(Optional.of("digits"), alias.baseTypeArguments().get(0).nameValue());
        assertEquals(1, alias.typeParameters().size());
        IRTypeParameter parameter = alias.typeParameters().get(0);
        assertEquals("digits", parameter.name());
        assertEquals("int", parameter.type().name());
        assertEquals(Optional.of("int"), parameter.type().resolvedName());
        assertEquals(Optional.of("the digit count"), parameter.definition());
        assertEquals(1, parameter.typeArguments().size(), "the parameter's OWN type call's arguments are read (PR #644)");
        assertEquals("max", parameter.typeArguments().get(0).parameter());
        assertEquals(Optional.of("99"), parameter.typeArguments().get(0).literalValue());
    }

    @Test
    void deserializesGenericKindDiscardingUnmodeled() {
        String json = "{ \"kind\": \"FUNCTION\", \"name\": \"F\", \"unmodeled\": \"IRNodeImpl\" }";
        IRNode node = reader.fromJsonNode(json);
        assertEquals(IRKind.FUNCTION, node.kind());
        assertEquals("F", node.name());
    }

    /**
     * The property gate (PR #644): a MODEL fragment reads back into an {@link IRModelNode} carrying
     * every model-level fact — the definition, the version, the qualifiable roots, the
     * {@code [qualification]} functions, the function signatures (their inputs and output as FIELD
     * nodes) and the with-meta uses, a typed one and a REFUSED one.
     */
    @Test
    void deserializesAModelFragmentWithEveryFact() {
        String json = """
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
                          "cardinality": "ONE_TO_ONE"
                        }
                      ],
                      "output": {
                        "kind": "FIELD",
                        "name": "trade",
                        "type": "Trade",
                        "cardinality": "ONE_TO_ONE"
                      }
                    }
                  ],
                  "withMetaUses": [
                    {
                      "entryNames": [ "scheme" ],
                      "argumentType": "number",
                      "argumentTypeKind": "BASIC_TYPE",
                      "argumentTypeResolvedName": "number",
                      "typeArguments": [ { "parameter": "digits", "literalValue": "18" } ]
                    },
                    {
                      "entryNames": [ "reference" ],
                      "refusal": "nothing"
                    }
                  ]
                }
                """;
        IRModel model = (IRModel) reader.fromJsonNode(json);
        assertEquals(IRKind.MODEL, model.kind());
        assertEquals("test.model", model.name());
        assertEquals("test.model", model.namespace());
        assertEquals(Optional.of("the trade model"), model.definition());
        assertEquals(Optional.of("1.2.3"), model.version());

        assertEquals(1, model.qualifiableConfigs().size());
        IRQualifiableConfig config = model.qualifiableConfigs().get(0);
        assertEquals("IS_PRODUCT", config.kind());
        assertEquals(IRKind.STRUCT, config.rootType().kind());
        assertEquals(Optional.of("test.model"), config.rootType().namespace());

        assertEquals(1, model.qualificationFunctions().size());
        IRQualificationFunction qualification = model.qualificationFunctions().get(0);
        assertEquals("Qualify_Swap", qualification.name());
        assertEquals(Optional.of("Product"), qualification.firstInputType().resolvedName());

        assertEquals(1, model.functionSignatures().size());
        IRFunctionSignature signature = model.functionSignatures().get(0);
        assertEquals("Create_Trade", signature.name());
        assertEquals(1, signature.inputs().size());
        assertEquals("product", signature.inputs().get(0).name());
        assertEquals(IRKind.FIELD, signature.inputs().get(0).kind());
        assertEquals("trade", signature.output().orElseThrow().name());

        assertEquals(2, model.withMetaUses().size());
        IRWithMetaUse typed = model.withMetaUses().get(0);
        assertEquals(List.of("scheme"), typed.entryNames());
        assertEquals("number", typed.argumentType().orElseThrow().name());
        assertEquals(Optional.of("18"), typed.typeArguments().get(0).literalValue());
        assertEquals(Optional.empty(), typed.refusal());
        IRWithMetaUse refused = model.withMetaUses().get(1);
        assertEquals(Optional.empty(), refused.argumentType());
        assertEquals(Optional.of("nothing"), refused.refusal());
    }

    /**
     * The property gate (PR #644): a reference's {@code <prefix>AliasChain} reads back as the RUNGS
     * the use site walks, outermost-first, each with its parameters and its conditions' kinds.
     */
    @Test
    void deserializesAFieldWhoseTypeReferenceWalksAnAliasChain() {
        String json = """
                {
                  "kind": "FIELD",
                  "name": "rate",
                  "type": "Max3Int",
                  "cardinality": "ONE_TO_ONE",
                  "typeKind": "TYPE_ALIAS",
                  "typeNamespace": "test.model",
                  "typeResolvedName": "Max3Int",
                  "typeAliasChain": [
                    {
                      "name": "Max3Int",
                      "namespace": "test.model",
                      "parameterNames": [ "digits" ],
                      "conditionNames": [ "Positive", null ],
                      "conditionKinds": [ "DataRule", "OneOf" ]
                    },
                    { "name": "PositiveInt" }
                  ]
                }
                """;
        IRField field = (IRField) reader.fromJsonNode(json);
        List<IRAliasLink> chain = field.type().aliasChain();
        assertEquals(2, chain.size());
        IRAliasLink outer = chain.get(0);
        assertEquals("Max3Int", outer.name());
        assertEquals("test.model.Max3Int", outer.qualifiedName());
        assertTrue(outer.isParameterised());
        assertEquals(List.of("digits"), outer.parameterNames());
        assertEquals(List.of(Optional.of("Positive"), Optional.empty()), outer.conditionNames());
        assertEquals(List.of("DataRule", "OneOf"), outer.conditionKinds());
        IRAliasLink inner = chain.get(1);
        assertEquals("PositiveInt", inner.name());
        assertEquals(Optional.empty(), inner.namespace());
        assertFalse(inner.isParameterised());
        assertEquals(List.of(), inner.conditionKinds());
    }
}
