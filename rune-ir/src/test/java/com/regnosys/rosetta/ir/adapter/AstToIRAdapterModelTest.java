package com.regnosys.rosetta.ir.adapter;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 8 (PR #644 — the property gate), the MODEL-LEVEL node: a {@code namespace} declaration adapted to an
 * {@link IRModelNode} ({@link IRKind#MODEL}) carrying the facts no type node holds and the data-type emitter's
 * DERIVED files read — the namespace documentation ({@code package-info.java}), the version stamp, the
 * {@code isEvent root} / {@code isProduct root} configurations and the {@code [qualification]} functions (the
 * {@code *Meta} qualify wing), every function's signature and every {@code with-meta} expression (the two
 * non-declaration sources of the {@code FieldWithMeta*} / {@code ReferenceWithMeta*} wrapper set).
 *
 * <p><b>The inferrer seam.</b> A {@code with-meta} use's argument is typed by the WORKSPACE, not by the adapter, so
 * the adapter takes an {@link AstToIRAdapter.TypeInferrer} exactly as it takes a
 * {@link AstToIRAdapter.CorpusResolver}. An adapter built without one does NOT guess: every use reads the
 * {@code missing} refusal, witnessed below. The refusals are FACTS, never silent drops — the
 * {@code ReferenceWithMetaVoid} line (the one permanent waiver of the byte gate) is the {@code nothing} token here.
 *
 * <p><b>Which arm the builtins take here:</b> as in {@code AstToIRAdapterTypeAliasTest}, the fixture is parsed from
 * STRINGS ALONE — the model library is not loaded — so {@code string} / {@code number} / {@code boolean} resolve to
 * no declaration and reach the shared registry by NAME.
 */
class AstToIRAdapterModelTest {

    private static final String MODEL = """
            namespace seat8.model : <"The model's own documentation.">
            version "2.4.1"

            isProduct root Product;
            isEvent root Event;

            typeAlias Amount2: number(fractionalDigits: 2)

            type Product:
                id string (1..1)

            type Event:
                id string (1..1)

            type Party:
                partyId string (1..1)

            func Qualify_Product: <"The product qualifier.">
                [qualification Product]
                inputs:
                    product Product (1..1)
                output:
                    is_product boolean (1..1)
                set is_product:
                    product -> id exists

            func Wrap:
                inputs:
                    party Party (1..1)
                        [metadata scheme]
                output:
                    result Party (1..1)
                        [metadata reference]
                set result:
                    party with-meta {scheme: "s"}

            func WrapNumber:
                inputs:
                    amount Amount2 (1..1)
                output:
                    result number (1..1)
                set result:
                    amount with-meta {scheme: "s"}

            func WrapNothing:
                output:
                    result string (0..1)
                set result:
                    empty with-meta {scheme: "s"}
            """;

    /** A namespace that declares NONE of it — every fact must read its empty default, never a guess. */
    private static final String BARE = """
            namespace seat8.bare
            """;

    private static RModel parse(String source, String file) {
        return AstBuilder.buildFromString(source, file);
    }

    /** The model node of {@link #MODEL}, adapted by an adapter whose inferrer IS the fixture's workspace. */
    private static IRModelNode modelNode() {
        RModel model = parse(MODEL, "seat8-model.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(model)).workspace();
        AstToIRAdapter adapter = new AstToIRAdapter(AstToIRAdapter.CorpusResolver.NONE, workspace::getInferredType);
        return adapter.adaptModelNode(model);
    }

    // ------------------------------------------------------------------------------- the node and its facts

    @Test
    void aNamespaceDeclarationAdaptsToAModelNodeCarryingItsDefinitionAndVersion() {
        IRModelNode node = modelNode();
        assertEquals("seat8.model", node.name(), "the model node's name IS its namespace");
        assertEquals("seat8.model", node.namespace());
        assertEquals(IRKind.MODEL, node.kind());
        assertEquals(Optional.of("The model's own documentation."), node.definition(),
                "package-info.java is written from this fact");
        assertEquals(Optional.of("2.4.1"), node.version(),
                "the version stamp is an IR fact since this seat, not a host parameter");
        assertEquals(List.of(), node.children(),
                "a model's declarations are the workspace's own nodes — this node states MODEL-level facts only");
        assertEquals(Optional.empty(), node.sourceRange());
    }

    @Test
    void theQualifiableConfigurationsAreCarriedWithTheirRootsResolved() {
        List<IRQualifiableConfig> configs = modelNode().qualifiableConfigs();
        assertEquals(List.of("IS_PRODUCT", "IS_EVENT"), configs.stream().map(IRQualifiableConfig::kind).toList(),
                "in declaration order — which configuration WINS its kind is the index's law, not this node's");

        IRType product = configs.get(0).rootType();
        assertEquals("Product", product.name());
        assertEquals(IRKind.STRUCT, product.kind());
        assertEquals(Optional.of("seat8.model"), product.namespace());
        assertEquals(Optional.of("Product"), product.resolvedName());

        IRType event = configs.get(1).rootType();
        assertEquals("Event", event.name());
        assertEquals(Optional.of("seat8.model.Event"), event.resolvedQualifiedName());
    }

    @Test
    void everyFunctionSignatureIsCarriedWithItsInputsAndOutputAsFields() {
        List<IRFunctionSignature> signatures = modelNode().functionSignatures();
        assertEquals(List.of("Qualify_Product", "Wrap", "WrapNumber", "WrapNothing"),
                signatures.stream().map(IRFunctionSignature::name).toList(),
                "every function of the model, in declaration order — a dropped one is a dropped wrapper source");

        IRFunctionSignature wrap = signature(signatures, "Wrap");
        assertEquals(1, wrap.inputs().size());
        IRField party = wrap.inputs().get(0);
        assertEquals("party", party.name());
        assertEquals("Party", party.type().name());
        assertEquals(IRKind.STRUCT, party.type().kind());
        assertEquals(List.of("metadata:scheme"), annotations(party),
                "the input's own [metadata …] is what pulls its wrapper into the set");

        IRField result = wrap.output().orElseThrow();
        assertEquals("result", result.name());
        assertEquals(List.of("metadata:reference"), annotations(result));

        assertEquals(List.of(), signature(signatures, "WrapNothing").inputs(),
                "a function may declare no input - the parser's `__synthesized_input__` placeholder"
                        + " (GeneratedInputRule) is scaffolding the model never wrote, excluded by the parser's"
                        + " own predicate; the IR states the DECLARED signature");
        assertTrue(signature(signatures, "WrapNothing").output().isPresent());
    }

    @Test
    void onlyAQualificationFunctionWithAnInputIsCarriedWithThatInputsType() {
        List<IRQualificationFunction> functions = modelNode().qualificationFunctions();
        assertEquals(1, functions.size(), "only the [qualification]-annotated function with an input");
        IRQualificationFunction qualifier = functions.get(0);
        assertEquals("Qualify_Product", qualifier.name());
        assertEquals("Product", qualifier.firstInputType().name());
        assertEquals(IRKind.STRUCT, qualifier.firstInputType().kind());
        assertEquals(Optional.of("seat8.model.Product"), qualifier.firstInputType().resolvedQualifiedName(),
                "the old generator compares node IDENTITY; the IR compares the resolved qualified name");
    }

    // ------------------------------------------------------------------------------- the with-meta uses

    @Test
    void everyWithMetaExpressionIsCarriedWithItsEntryNames() {
        List<IRWithMetaUse> uses = modelNode().withMetaUses();
        assertEquals(3, uses.size(), "one per with-meta expression in the model's bodies");
        for (IRWithMetaUse use : uses) {
            assertEquals(List.of("scheme"), use.entryNames(), "the entries as written, in order");
        }
    }

    @Test
    void aWithMetaUseOverADataTypedArgumentCarriesThatDeclarationAsAReference() {
        IRWithMetaUse use = useTyped(modelNode(), "Party");
        IRType argument = use.argumentType().orElseThrow();
        assertEquals(IRKind.STRUCT, argument.kind());
        assertEquals(Optional.of("seat8.model"), argument.namespace());
        assertEquals(Optional.of("Party"), argument.resolvedName());
        assertEquals(List.of(), use.typeArguments(), "a declared type carries no constraint argument");
        assertEquals(Optional.empty(), use.refusal());
    }

    @Test
    void aWithMetaUseOverAConstrainedNumberCarriesItsConstraintsAsLiteralArguments() {
        IRWithMetaUse use = useTyped(modelNode(), "number");
        IRType argument = use.argumentType().orElseThrow();
        assertEquals(IRKind.BASIC_TYPE, argument.kind());
        assertEquals("number", argument.name(), "a builtin leaf is named by the registry, never by the alias");
        assertEquals(Optional.empty(), argument.namespace());
        assertEquals(List.of("fractionalDigits=2"), literals(use.typeArguments()),
                "the alias's constraint is IN FORCE on the inferred type — it decides the wrapper's Java type");
    }

    @Test
    void anArgumentTypedNothingIsANamedRefusalNotASilentDrop() {
        IRWithMetaUse use = refused(modelNode(), "nothing");
        assertEquals(Optional.empty(), use.argumentType());
        assertEquals(List.of(), use.typeArguments(), "a refused use carries no argument — the record's law");
        assertEquals(List.of("scheme"), use.entryNames(),
                "the entries survive the refusal: the skip is stated, not silent");
    }

    @Test
    void anAdapterWithoutATypeInferrerRefusesEveryUseAsMissingRatherThanGuessing() {
        RModel model = parse(MODEL, "seat8-model-noseam.rosetta");
        RWorkspace.build(List.of(model));
        List<IRWithMetaUse> uses = new AstToIRAdapter().adaptModelNode(model).withMetaUses();
        assertEquals(3, uses.size());
        for (IRWithMetaUse use : uses) {
            assertEquals(Optional.of("missing"), use.refusal(),
                    "without the seam the adapter states its ignorance — it does not type the argument itself");
            assertEquals(Optional.empty(), use.argumentType());
        }
    }

    // --------------------------------------------------------------------------------- the empty defaults

    @Test
    void aModelThatDeclaresNoneOfItAdaptsToANodeWhoseEveryListIsEmpty() {
        RModel bare = parse(BARE, "seat8-bare.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(bare)).workspace();
        IRModelNode node = new AstToIRAdapter(AstToIRAdapter.CorpusResolver.NONE, workspace::getInferredType)
                .adaptModelNode(bare);
        assertEquals("seat8.bare", node.name());
        assertEquals(IRKind.MODEL, node.kind());
        assertEquals(Optional.empty(), node.definition());
        assertEquals(Optional.empty(), node.version());
        assertEquals(List.of(), node.qualifiableConfigs());
        assertEquals(List.of(), node.qualificationFunctions());
        assertEquals(List.of(), node.functionSignatures());
        assertEquals(List.of(), node.withMetaUses());
    }

    // --------------------------------------------------------------------------------- the records' laws

    @Test
    void theModelLevelRecordsRefuseAHalfStatedFact() {
        assertEquals("a model node needs its namespace as its name",
                assertThrows(IllegalArgumentException.class, () -> new IRModelNode("")).getMessage());

        IRType root = IRTypeNode.reference("Product", IRKind.STRUCT, Optional.of("seat8.model"),
                Optional.of("Product"));
        assertEquals("a qualifiable configuration's kind is one of [IS_EVENT, IS_PRODUCT], not 'IS_SOMETHING'",
                assertThrows(IllegalArgumentException.class,
                        () -> new IRQualifiableConfig("IS_SOMETHING", root)).getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> new IRWithMetaUse(List.of("scheme"), Optional.of(root), List.of(), Optional.of("nothing")),
                "BOTH a type and a refusal — exactly one is the law");
        assertThrows(IllegalArgumentException.class,
                () -> new IRWithMetaUse(List.of("scheme"), Optional.empty(), List.of(), Optional.empty()),
                "NEITHER a type nor a refusal");
        assertThrows(IllegalArgumentException.class,
                () -> new IRWithMetaUse(List.of("scheme"), Optional.empty(), List.of(), Optional.of("unknown")),
                "a refusal token outside the vocabulary");
        assertThrows(IllegalArgumentException.class,
                () -> new IRWithMetaUse(List.of("scheme"), Optional.empty(),
                        List.of(new IRTypeArgument("digits", Optional.empty(), Optional.of("3"), false)),
                        Optional.of("nothing")),
                "a refused use carrying arguments");
    }

    // ---------------------------------------------------------------------------------------- the helpers

    private static IRFunctionSignature signature(List<IRFunctionSignature> signatures, String name) {
        return signatures.stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
    }

    /** {@code metadata:scheme} — {@link IRAnnotationUse#key()}, the census key of an annotation use. */
    private static List<String> annotations(IRField field) {
        return field.annotations().stream().map(IRAnnotationUse::key).toList();
    }

    /** The one use whose argument type is named {@code typeName}. */
    private static IRWithMetaUse useTyped(IRModelNode node, String typeName) {
        return node.withMetaUses().stream()
                .filter(u -> u.argumentType().map(t -> t.name().equals(typeName)).orElse(false))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no with-meta use typed " + typeName + " among " + node.withMetaUses()));
    }

    /** The one use carrying {@code token} as its refusal. */
    private static IRWithMetaUse refused(IRModelNode node, String token) {
        return node.withMetaUses().stream().filter(u -> u.refusal().map(token::equals).orElse(false))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no with-meta use refused '" + token + "' among " + node.withMetaUses()));
    }

    /** {@code parameter=value}, a negated literal keeping its {@code -}; an inferred constraint is a literal. */
    private static List<String> literals(List<IRTypeArgument> arguments) {
        return arguments.stream()
                .map(a -> a.parameter() + "=" + (a.negated() ? "-" : "") + a.literalValue().orElseThrow())
                .toList();
    }
}
