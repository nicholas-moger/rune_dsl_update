package com.regnosys.rosetta.testutil;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IREnumValueNode;
import com.regnosys.rosetta.ir.adapter.IRFieldNode;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.adapter.IRNodeImpl;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IRAnnotationPath;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRBounds;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IREnumSynonym;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRLabel;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRRuleReference;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IRConversion;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.ir.expr.IRConstruct;
import com.regnosys.rosetta.ir.expr.IRLambdaOp;
import com.regnosys.rosetta.ir.expr.IRListConstruct;
import com.regnosys.rosetta.ir.expr.IRListOp;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.IROnlyExists;
import com.regnosys.rosetta.ir.expr.IRPipe;
import com.regnosys.rosetta.ir.expr.IRAllAnyCompare;
import com.regnosys.rosetta.ir.expr.IRChoiceOptionNav;
import com.regnosys.rosetta.ir.expr.IRDeepFeatureNav;
import com.regnosys.rosetta.ir.expr.IRDispatchInputRef;
import com.regnosys.rosetta.ir.expr.IRMetaItemNav;
import com.regnosys.rosetta.ir.expr.IRQualifierItemNav;
import com.regnosys.rosetta.ir.expr.IRChoiceReceiverNav;
import com.regnosys.rosetta.ir.expr.IRQualifierReceiverNav;
import com.regnosys.rosetta.ir.expr.IRRecordFeatureNav;
import com.regnosys.rosetta.ir.expr.IRRecordReceiverNav;
import com.regnosys.rosetta.ir.expr.IRSwitchOp;
import com.regnosys.rosetta.ir.expr.IRDefaultOp;
import com.regnosys.rosetta.ir.expr.IRMembershipOp;
import com.regnosys.rosetta.ir.expr.IRCollectOp;
import com.regnosys.rosetta.ir.expr.IRMetaParamRef;
import com.regnosys.rosetta.ir.expr.IROutputRef;
import com.regnosys.rosetta.ir.expr.IRImplicitAttrNav;
import com.regnosys.rosetta.ir.expr.IRConditionInstance;
import com.regnosys.rosetta.ir.expr.IRRuleInputNav;
import com.regnosys.rosetta.ir.expr.IRWithMetaOp;
import com.regnosys.rosetta.ir.expr.IRJoinOp;
import com.regnosys.rosetta.ir.expr.IRLibraryApply;
import com.regnosys.rosetta.ir.expr.IROutputAliasNav;
import com.regnosys.rosetta.ir.expr.IRSynItemNav;
import com.regnosys.rosetta.ir.expr.IRClosureParam;
import com.regnosys.rosetta.ir.expr.IRMetaOutputApply;
import com.regnosys.rosetta.ir.expr.IRSymbolNav;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRPointFreeApply;
import com.regnosys.rosetta.ir.expr.IRToString;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.Let;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.Optionality;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * One valid, hand-built IR sample per {@link IRKind} / {@link IRExprKind}, for coverage tests.
 *
 * <p>Shared by the {@code ir.print}, {@code ir.json} and {@code ir.emit} test suites so none
 * duplicates sampler logic. The two factory methods are the sole public surface; the constants are
 * implementation detail.
 */
public final class IRSamples {

    private IRSamples() {}

    // ── shared model-fact constants (drift sentinels) ──────────────────────────────────────────

    /**
     * Record components excluded from rendering/serialization coverage checks. These are
     * infrastructure fields ({@code sourceRange}, {@code metadata}, {@code nodeId}) that every
     * IR record carries but no renderer or serializer surfaces.
     */
    public static final Set<String> EXCLUDED_COMPONENTS =
            Set.of("sourceRange", "metadata", "nodeId");

    /**
     * Per concrete record: the components the printer and serializer render (everything not in
     * {@link #EXCLUDED_COMPONENTS}). Both renderers cover the same component set. Used as the
     * "expected" side in the model-drift sentinel tests, which compare each record's reflected
     * components (minus {@link #EXCLUDED_COMPONENTS}) to this map.
     *
     * <p>The sentinel checks component <b>names</b>, not renderer output: a new model component that
     * is neither listed here nor excluded fails the test, forcing a conscious renderer + map update.
     * It does not — and is not meant to — detect a renderer that <i>stops</i> emitting an
     * already-mapped component; that regression is caught by the per-kind content tests and the
     * golden snapshots.
     */
    public static final Map<Class<?>, Set<String>> RENDERED_COMPONENTS = Map.ofEntries(
        // The declaration-IR enrichment (decision D55) added the declaration facts after
        // `metadata` as proper named components; the printer and the serializer both render every
        // one of them (each only when it is non-default), so they are listed, not allowlisted.
        // IRSamples.enriched(IRKind) carries each of them non-default for the tests that exercise
        // the rendering; node(IRKind) stays at the pre-enrichment arity as the backward-compat
        // witness.
        // The type gate (v3.3 seat 7, PR #643) added three more IRTypeNode components: a TYPE_ALIAS
        // reference's collapsed chain and, on a TYPE_ALIAS declaration, its parameters and the
        // arguments its body wrote. The printer and the serializer both render all three (each only
        // when non-default), so they are listed, not allowlisted.
        // The property gate (v3.3 seat 8, PR #644) added two more IRTypeNode components: the condition KINDS
        // (index-parallel to the names) and, on a TYPE_ALIAS reference, the alias CHAIN's rungs. Both are
        // rendered by the printer and the serializer (each only when non-default), so they are listed too.
        Map.entry(IRTypeNode.class,
            Set.of("name", "kind", "fields", "baseType", "isAbstract",
                "namespace", "resolvedName", "definition", "docReferences", "annotations",
                "conditionNames", "effectiveBase", "typeParameters", "baseTypeArguments",
                "conditionKinds", "aliasChain")),
        // The model-level node (the property gate, PR #644): every component is a model FACT both renderers
        // surface. `namespace()` is `name`, not a component of its own, so it is not listed here.
        Map.entry(IRModelNode.class,
            Set.of("name", "definition", "version", "qualifiableConfigs", "qualificationFunctions",
                "functionSignatures", "withMetaUses")),
        Map.entry(IREnumNode.class,
            Set.of("name", "values",
                "namespace", "parent", "definition", "docReferences", "annotations")),
        Map.entry(IREnumValueNode.class,
            Set.of("name", "displayName",
                "definition", "docReferences", "annotations", "synonyms")),
        Map.entry(IRFieldNode.class,
            Set.of("name", "type", "cardinality",
                "bounds", "isOverride", "typeArguments", "definition", "docReferences",
                "annotations", "labels", "ruleReferences")),
        Map.entry(IRLiteral.class,
            Set.of("literalKind", "value", "type", "cardinality", "optionality")),
        Map.entry(IREmptyLiteral.class,
            Set.of("source", "type", "cardinality", "optionality")),
        Map.entry(IRVariable.class,
            Set.of("name", "variableKind", "type", "cardinality", "optionality")),
        Map.entry(IRReference.class,
            Set.of("target", "referenceKind", "type", "cardinality", "optionality")),
        Map.entry(IRApply.class,
            Set.of("callee", "args", "type", "cardinality", "optionality")),
        Map.entry(BinaryOp.class,
            Set.of("op", "left", "right", "type", "cardinality", "optionality")),
        Map.entry(Existence.class,
            Set.of("op", "modifier", "arg", "type", "cardinality", "optionality")),
        Map.entry(FieldAccess.class,
            Set.of("receiver", "feature", "type", "cardinality", "featureCardinality", "optionality")),
        Map.entry(IRMetaAccess.class,
            Set.of("receiver", "feature", "metaQualifiers", "type", "cardinality",
                "featureCardinality", "optionality")),
        Map.entry(IRListOp.class,
            Set.of("op", "child", "type", "cardinality", "optionality")),
        Map.entry(IRListConstruct.class,
            Set.of("elements", "type", "cardinality", "optionality")),
        Map.entry(IRConditional.class,
            Set.of("condition", "thenBranch", "elseBranch", "type", "cardinality", "optionality")),
        Map.entry(Let.class,
            Set.of("binder", "value", "in", "type", "cardinality", "optionality")),
        Map.entry(IRToString.class,
            Set.of("child", "type", "cardinality", "optionality")),
        Map.entry(IRPointFreeApply.class,
            Set.of("callee", "type", "cardinality", "optionality")),
        Map.entry(IRConstruct.class,
            Set.of("typeName", "attributeNames", "spread", "type", "cardinality", "optionality")),
        Map.entry(IRLambdaOp.class,
            Set.of("op", "receiver", "binderName", "body", "type", "cardinality", "optionality")),
        Map.entry(IRConversion.class,
            Set.of("conversionKind", "targetTypeName", "child", "type", "cardinality", "optionality")),
        Map.entry(IRPipe.class,
            Set.of("spineLength", "type", "cardinality", "optionality")),
        Map.entry(IROnlyExists.class,
            Set.of("pathCount", "type", "cardinality", "optionality")),
        Map.entry(IRSymbolNav.class,
            Set.of("symbolKind", "type", "cardinality", "optionality")),
        Map.entry(IRClosureParam.class,
            Set.of("paramName", "type", "cardinality", "optionality")),
        Map.entry(IRMetaOutputApply.class,
            Set.of("calleeName", "type", "cardinality", "optionality")),
        Map.entry(IRAllAnyCompare.class,
            Set.of("op", "modifier", "type", "cardinality", "optionality")),
        Map.entry(IRSynItemNav.class,
            Set.of("featureName", "type", "cardinality", "optionality")),
        Map.entry(IRChoiceOptionNav.class,
            Set.of("headName", "optionName", "type", "cardinality", "optionality")),
        Map.entry(IRDispatchInputRef.class,
            Set.of("inputName", "type", "cardinality", "optionality")),
        Map.entry(IRDeepFeatureNav.class,
            Set.of("receiver", "featureName", "type", "cardinality", "optionality")),
        Map.entry(IRRecordFeatureNav.class,
            Set.of("headName", "featureName", "recordTypeName", "type", "cardinality",
                "optionality")),
        Map.entry(IRMetaItemNav.class,
            Set.of("featureName", "type", "cardinality", "optionality")),
        Map.entry(IRRecordReceiverNav.class,
            Set.of("receiver", "featureName", "recordTypeName", "type", "cardinality",
                "optionality")),
        Map.entry(IRQualifierItemNav.class,
            Set.of("qualifierName", "type", "cardinality", "optionality")),
        // #514 hygiene: the #513 kinds joined the enum without sentinel entries (the map is
        // iterated, not discovered, so the gap was silent); listed now with the #514 pair so
        // the model-drift sentinel covers every childless/shallow kind again.
        Map.entry(IRSwitchOp.class,
            Set.of("caseCount", "hasDefault", "type", "cardinality", "optionality")),
        Map.entry(IRDefaultOp.class,
            Set.of("type", "cardinality", "optionality")),
        Map.entry(IRMembershipOp.class,
            Set.of("op", "type", "cardinality", "optionality")),
        Map.entry(IRCollectOp.class,
            Set.of("op", "hasBody", "type", "cardinality", "optionality")),
        Map.entry(IROutputRef.class,
            Set.of("outputName", "type", "cardinality", "optionality")),
        Map.entry(IRMetaParamRef.class,
            Set.of("paramName", "type", "cardinality", "optionality")),
        Map.entry(IRRuleInputNav.class,
            Set.of("featureName", "type", "cardinality", "optionality")),
        Map.entry(IRImplicitAttrNav.class,
            Set.of("attributeName", "type", "cardinality", "optionality")),
        Map.entry(IRConditionInstance.class,
            Set.of("typeName", "type", "cardinality", "optionality")),
        Map.entry(IRChoiceReceiverNav.class,
            Set.of("receiver", "optionName", "choiceName", "type", "cardinality",
                "optionality")),
        Map.entry(IRWithMetaOp.class,
            Set.of("entryCount", "type", "cardinality", "optionality")),
        Map.entry(IRJoinOp.class,
            Set.of("hasSeparator", "type", "cardinality", "optionality")),
        Map.entry(IROutputAliasNav.class,
            Set.of("headName", "featureName", "type", "cardinality", "optionality")),
        Map.entry(IRLibraryApply.class,
            Set.of("calleeName", "type", "cardinality", "optionality")),
        Map.entry(IRQualifierReceiverNav.class,
            Set.of("receiver", "qualifierName", "type", "cardinality", "optionality")));

    // ── constants shared by expr(IRExprKind) ────────────────────────────────────────────────────

    private static final RMetaAnnotatedType T = RMetaAnnotatedType.MISSING;
    private static final ExpressionCardinality C = ExpressionCardinality.SINGLE;
    private static final Optionality O = Optionality.PRESENT;
    private static final SourceRange SR = SourceRange.NONE;

    /** A trivial non-null leaf to satisfy child-bearing constructors (never null, never mutated). */
    private static final IRExpr LEAF =
            new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT, T, C, O, SR);

    // ── public factory methods ───────────────────────────────────────────────────────────────────

    /**
     * Returns one valid {@link IRNode} for every {@link IRKind}.
     *
     * <p>The seven modeled kinds ({@code STRUCT}, {@code CHOICE}, {@code TYPE_ALIAS}, {@code ENUM},
     * {@code MODEL}, {@code ENUM_VALUE}, {@code FIELD}) use real adapter records — {@code MODEL} since the
     * property gate (v3.3 seat 8, PR #644). All other kinds use {@link IRNodeImpl}.
     *
     * @param kind the kind to sample; must not be {@code null}
     * @return a non-null, structurally valid {@code IRNode}
     */
    public static IRNode node(IRKind kind) {
        return switch (kind) {
            case STRUCT, CHOICE -> new IRTypeNode(
                    "X", kind, List.of(), Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY);
            // a minimal typeAlias DECLARATION: a body that is an UNRESOLVED reference, so the sample
            // carries the alias node's REQUIRED members and nothing more (the faithfulness sentinel
            // compares the emitted key set to the schema's `required`)
            case TYPE_ALIAS -> new IRTypeNode(
                    "X", IRKind.TYPE_ALIAS, List.of(),
                    Optional.of(new IRTypeNode("T", IRKind.BASIC_TYPE, List.of(), Optional.empty(),
                            false, Optional.empty(), IRMetadata.EMPTY)),
                    false, Optional.empty(), IRMetadata.EMPTY);
            case ENUM -> new IREnumNode(
                    "X", List.of(), Optional.empty(), IRMetadata.EMPTY);
            // the model-level node (the property gate, PR #644): its namespace alone, so the sample carries
            // the node's REQUIRED members and nothing more (as the TYPE_ALIAS arm does)
            case MODEL -> new IRModelNode("X");
            case ENUM_VALUE -> new IREnumValueNode(
                    "X", Optional.empty(), Optional.empty(), IRMetadata.EMPTY);
            case FIELD -> new IRFieldNode(
                    "X", new IRTypeNode("T", IRKind.STRUCT, List.of(),
                        Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY),
                    Cardinality.ONE_TO_ONE, Optional.empty(), IRMetadata.EMPTY);
            default -> new IRNodeImpl("X", kind, List.of(), Optional.empty(), IRMetadata.EMPTY);
        };
    }

    // ── the D55 declaration facts: one FULLY-ENRICHED sample per declaration-node kind ──────────

    /** The namespace every enriched sample lives in. */
    private static final String NS = "test.model";

    /**
     * Returns one FULLY-ENRICHED {@link IRNode} for each declaration-node kind the declaration-IR
     * enrichment (decision D55) touches — every declaration fact at a NON-default value, so a test
     * exercises the new components rather than their empty defaults.
     *
     * <p>{@link #node(IRKind)} deliberately stays at the pre-enrichment arity: it is the
     * backward-compatibility witness (its wire form and its print must not move). Use this factory
     * wherever the facts themselves are under test.
     *
     * @param kind one of {@code STRUCT}, {@code CHOICE}, {@code TYPE_ALIAS}, {@code ENUM},
     *             {@code ENUM_VALUE}, {@code FIELD}
     * @return a non-null, structurally valid node carrying every D55 fact
     * @throws IllegalArgumentException for a kind the enrichment does not reach
     */
    public static IRNode enriched(IRKind kind) {
        return switch (kind) {
            // the type gate (PR #643): a parameterised alias whose body names ANOTHER alias — the
            // body reference carries the collapsed chain, the declaration its parameters and the
            // arguments the body wrote (one passed through by name, one literal). The declaration
            // itself carries NO effective base (only a reference collapses) and no resolvedName.
            case TYPE_ALIAS -> new IRTypeNode(NS + ".EnrichedAlias", IRKind.TYPE_ALIAS, List.of(),
                    // the property gate (PR #644): the body reference carries the chain's RUNGS as well as the
                    // leaf the chain collapses to
                    Optional.of(IRTypeNode.reference("int", IRKind.TYPE_ALIAS, Optional.of(NS),
                            Optional.of("int"), Optional.of(enrichedEffectiveBase()), enrichedAliasChain())),
                    false, Optional.empty(), IRMetadata.EMPTY,
                    Optional.of(NS), Optional.empty(), Optional.of("an enriched alias"),
                    List.of(fullDocReference()), enrichedAnnotations(),
                    List.of(Optional.of("named"), Optional.empty()),
                    Optional.empty(),
                    List.of(new IRTypeParameter("digits",
                                    IRTypeNode.reference("int", IRKind.BASIC_TYPE, Optional.empty(),
                                            Optional.of("int")),
                                    Optional.of("how many digits the alias allows"), List.of()),
                            // a parameter whose OWN type call wrote arguments (PR #644) - the enriched sample carries it
                            new IRTypeParameter("min",
                                    IRTypeNode.reference("number", IRKind.BASIC_TYPE, Optional.empty(),
                                            Optional.of("number")),
                                    Optional.empty(),
                                    List.of(new IRTypeArgument("fractionalDigits", Optional.empty(), Optional.of("2"), false)))),
                    List.of(new IRTypeArgument("digits", Optional.of("digits"), Optional.empty(), false),
                            new IRTypeArgument("min", Optional.empty(), Optional.of("1.5"), true)),
                    // the alias DECLARATION states no condition kinds (its conditions are the contrast case
                    // for the printer's "kinds absent" arm) and carries no chain — only a reference does
                    List.of(), List.of());
            case STRUCT, CHOICE -> new IRTypeNode(NS + ".Enriched", kind,
                    List.of(enrichedField()),
                    Optional.of(IRTypeNode.reference("Base", IRKind.STRUCT,
                            Optional.of(NS), Optional.of("Base"))),
                    false, Optional.empty(), IRMetadata.EMPTY,
                    Optional.of(NS), Optional.of("Enriched"), Optional.of("an enriched type"),
                    List.of(fullDocReference()), enrichedAnnotations(),
                    List.of(Optional.of("named"), Optional.empty()),
                    Optional.empty(), List.of(), List.of(),
                    // the property gate (PR #644): the KINDS, index-parallel to the two condition names above
                    List.of("DataRule", "OneOf"), List.of());
            case ENUM -> new IREnumNode(NS + ".EnrichedEnum", List.of(enrichedEnumValue()),
                    Optional.empty(), IRMetadata.EMPTY, Optional.of(NS),
                    Optional.of(IRTypeNode.reference("BaseEnum", IRKind.ENUM,
                            Optional.of(NS), Optional.of("BaseEnum"))),
                    Optional.of("an enriched enum"), List.of(fullDocReference()),
                    enrichedAnnotations());
            case ENUM_VALUE -> enrichedEnumValue();
            case FIELD -> enrichedField();
            default -> throw new IllegalArgumentException("no enriched sample for " + kind);
        };
    }

    /**
     * The leaf {@code typeAlias int(digits int, min number): number(digits: digits, fractionalDigits: 0,
     * min: min)} collapses to at a {@code int(digits: 3, min: -1.5)} use site: a BUILTIN leaf (no
     * namespace) and LITERAL arguments only, the record's own law.
     */
    private static IREffectiveBase enrichedEffectiveBase() {
        return new IREffectiveBase(IRKind.BASIC_TYPE, "number", Optional.empty(),
                List.of(new IRTypeArgument("digits", Optional.empty(), Optional.of("3"), false),
                        new IRTypeArgument("fractionalDigits", Optional.empty(), Optional.of("0"), false),
                        new IRTypeArgument("min", Optional.empty(), Optional.of("1.5"), true)));
    }

    /**
     * The chain the enriched alias's body walks, OUTERMOST-FIRST (v3.3 seat 8, PR #644 — the property gate): a
     * PARAMETERISED rung carrying TWO conditions, one named and one not, with their kinds — the shape that
     * REFUSES a whole type-format validator — then a plain rung with neither parameter nor condition.
     */
    private static List<IRAliasLink> enrichedAliasChain() {
        return List.of(
                new IRAliasLink("int", Optional.of(NS), List.of("digits"),
                        List.of(Optional.of("Positive"), Optional.empty()),
                        List.of("DataRule", "OneOf")),
                new IRAliasLink("Max3Number", Optional.of(NS), List.of(), List.of(), List.of()));
    }

    // ── the model-level facts (v3.3 seat 8, PR #644 — the property gate) ────────────────────────

    /**
     * THE MODEL SAMPLE: one {@link IRModelNode} with EVERY model-level fact populated — the namespace's
     * documentation, the version, BOTH qualifiable roots ({@code IS_PRODUCT} and {@code IS_EVENT}), one
     * {@code [qualification]} function with its first input's type, one function signature whose input AND
     * output each carry a {@code [metadata …]} annotation (the wrapper set's function source), and THREE
     * {@code with-meta} uses: one over a declared type, one over a CONSTRAINED builtin (two literal type
     * arguments) and one REFUSED ({@code nothing}). Shared by the print and the json suites so neither
     * invents a model of its own.
     */
    public static IRModelNode model() {
        return new IRModelNode(NS, Optional.of("what the model is"), Optional.of("1.2.3"),
                List.of(new IRQualifiableConfig("IS_PRODUCT", structRef("Product")),
                        new IRQualifiableConfig("IS_EVENT", structRef("Event"))),
                List.of(new IRQualificationFunction("Qualify_Product", structRef("Product"))),
                List.of(new IRFunctionSignature("DoIt",
                        List.of(annotatedField("in", structRef("Foo"), "scheme")),
                        Optional.of(annotatedField("result", structRef("Bar"), "reference")))),
                List.of(new IRWithMetaUse(List.of("scheme"), Optional.of(structRef("Foo")), List.of(),
                                Optional.empty()),
                        new IRWithMetaUse(List.of("reference", "id"), Optional.of(numberRef()),
                                List.of(new IRTypeArgument("fractionalDigits", Optional.empty(),
                                                Optional.of("2"), false),
                                        new IRTypeArgument("min", Optional.empty(),
                                                Optional.of("0"), false)),
                                Optional.empty()),
                        new IRWithMetaUse(List.of("scheme"), Optional.empty(), List.of(),
                                Optional.of("nothing"))),
                Optional.empty(), IRMetadata.EMPTY);
    }

    /** A STRUCT reference into the samples' own namespace, resolved. */
    private static IRType structRef(String name) {
        return IRTypeNode.reference(name, IRKind.STRUCT, Optional.of(NS), Optional.of(name));
    }

    /** The {@code number} builtin as a reference: no namespace, its own registry name. */
    private static IRType numberRef() {
        return IRTypeNode.reference("number", IRKind.BASIC_TYPE, Optional.empty(), Optional.of("number"));
    }

    /**
     * A function input / output carrying one {@code [metadata <qualifier>]} annotation — the fact the wrapper
     * collector reads off a function's signature.
     */
    private static IRField annotatedField(String name, IRType type, String qualifier) {
        return new IRFieldNode(name, type, Cardinality.ONE_TO_ONE, Optional.empty(), IRMetadata.EMPTY,
                Optional.empty(), false, List.of(), Optional.empty(), List.of(),
                List.of(new IRAnnotationUse("metadata", Optional.of(qualifier))), List.of(), List.of());
    }

    /** A field carrying every D55 field fact: the exact bounds, override, type arguments, the lot. */
    private static IRField enrichedField() {
        return new IRFieldNode("enriched",
                IRTypeNode.reference("DirectionEnum", IRKind.ENUM,
                        Optional.of(NS), Optional.of("DirectionEnum")),
                Cardinality.ONE_TO_MANY, Optional.empty(), IRMetadata.EMPTY,
                Optional.of(new IRBounds(BigInteger.ONE, Optional.of(BigInteger.TWO))), true,
                List.of(new IRTypeArgument("maxLength", Optional.empty(), Optional.of("35"), false),
                        new IRTypeArgument("digits", Optional.of("n"), Optional.empty(), false)),
                Optional.of("an enriched field"), List.of(fullDocReference()), enrichedAnnotations(),
                List.of(new IRLabel("A label", Optional.of(itemPath()), Optional.empty())),
                List.of(new IRRuleReference(Optional.of(itemPath()), Optional.of("SomeRule"),
                                Optional.of(NS), Optional.of("SomeRule")),
                        new IRRuleReference(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty())));
    }

    /** An enum value carrying every D55 enum-value fact, synonyms included. */
    private static IREnumValue enrichedEnumValue() {
        return new IREnumValueNode("Enriched", Optional.of("ENRICHED"),
                Optional.empty(), IRMetadata.EMPTY, Optional.of("an enriched value"),
                List.of(fullDocReference()), enrichedAnnotations(),
                List.of(new IREnumSynonym(List.of("FpML", "ISO"), "ENRICHED_EXTERNAL",
                        Optional.of("the synonym's own definition"), Optional.of("a(.*)"),
                        Optional.of("b$1"), true)));
    }

    /** Two annotation uses: one with a bare qualifier, one with {@code key = value} arguments. */
    private static List<IRAnnotationUse> enrichedAnnotations() {
        return List.of(
                new IRAnnotationUse("metadata", Optional.of("scheme")),
                new IRAnnotationUse("deprecated", Optional.empty(),
                        List.of(new IRAnnotationUse.Argument("reason", "superseded", false),
                                new IRAnnotationUse.Argument("replacement", "other", true))));
    }

    /** {@code item -> leg ->> payout} — the {@code item} keyword root with a deep step. */
    private static IRAnnotationPath itemPath() {
        return new IRAnnotationPath(true, "", List.of(
                new IRAnnotationPath.Step("leg", false),
                new IRAnnotationPath.Step("payout", true)));
    }

    /**
     * A FULL {@code [regulatoryReference …]}: a scope path with a deep step, a body, two corpora
     * (one resolved to its declaration, one not), segments, rationales, both provisions, the
     * {@code reportedField} mark and a named argument.
     */
    public static IRDocReference fullDocReference() {
        return new IRDocReference(true, Optional.of(itemPath()), Optional.of("ESMA"),
                List.of(new IRDocReference.Corpus("MiFIR",
                                Optional.of(new IRDocReference.Corpus.Declaration(
                                        Optional.of("Regulation"), "MiFIR",
                                        Optional.of("MiFIR (600/2014)"),
                                        Optional.of("the corpus definition")))),
                        new IRDocReference.Corpus("Unresolved", Optional.empty())),
                List.of(new IRDocReference.Segment("article", "26"),
                        new IRDocReference.Segment("paragraph", "1")),
                List.of(new IRDocReference.Rationale(Optional.of("the rationale"),
                                Optional.of("the author")),
                        new IRDocReference.Rationale(Optional.of("a second rationale"),
                                Optional.empty())),
                Optional.of("a structured provision"), Optional.of("a provision"), true,
                List.of(new IRDocReference.NamedArg("note", "a named argument")));
    }

    /**
     * Returns one valid {@link IRExpr} for every {@link IRExprKind}.
     *
     * <p>Every structural kind is threaded with a non-null {@code LEAF} child so that
     * printers/serializers receive a fully-formed tree.
     *
     * @param kind the kind to sample; must not be {@code null}
     * @return a non-null, structurally valid {@code IRExpr}
     */
    public static IRExpr expr(IRExprKind kind) {
        return switch (kind) {
            case LITERAL -> new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.ONE, NodeId.ROOT, T, C, O, SR);
            case EMPTY_LITERAL -> new IREmptyLiteral(IREmptyLiteral.EmptySource.USER_EMPTY, NodeId.ROOT, T, C, O, SR);
            case VARIABLE -> LEAF;
            case REFERENCE -> new IRReference("R", IRReference.ReferenceKind.FUNCTION, NodeId.ROOT, T, C, O, SR);
            case APPLY -> new IRApply(
                    new IRReference("R", IRReference.ReferenceKind.FUNCTION, NodeId.ROOT, T, C, O, SR),
                    List.of(LEAF), NodeId.ROOT, T, C, O, SR);
            case BINARY_OP -> new BinaryOp(BinaryOp.BinOp.EQ, LEAF, LEAF, NodeId.ROOT, T, C, O, SR);
            case EXISTENCE -> new Existence(Existence.ExistOp.EXISTS, null, LEAF, NodeId.ROOT, T, C, O, SR);
            case FIELD_ACCESS -> new FieldAccess(LEAF, "f", NodeId.ROOT, T, C, ExpressionCardinality.SINGLE, O, SR);
            case META_ACCESS -> new IRMetaAccess(
                    LEAF, "f", List.of("scheme"), NodeId.ROOT, T, C, ExpressionCardinality.SINGLE, O, SR);
            case LIST_OP -> new IRListOp(IRListOp.Kind.COUNT, LEAF, NodeId.ROOT, T, C, O, SR);
            case LIST_CONSTRUCT -> new IRListConstruct(List.of(LEAF), NodeId.ROOT, T, C, O, SR);
            case CONDITIONAL -> new IRConditional(LEAF, LEAF, null, NodeId.ROOT, T, C, O, SR);
            case LET -> new Let("b", LEAF, LEAF, NodeId.ROOT, T, C, O, SR);
            case TO_STRING -> new IRToString(LEAF, NodeId.ROOT, T, C, O, SR);
            case POINT_FREE_APPLY -> new IRPointFreeApply("F", NodeId.ROOT, T, C, O, SR);
            case CONSTRUCT -> new IRConstruct("T", List.of("a"), false, NodeId.ROOT, T, C, O, SR);
            case LAMBDA_OP -> new IRLambdaOp(IRLambdaOp.Op.EXTRACT, LEAF, "a", LEAF, NodeId.ROOT, T, C, O, SR);
            case CONVERSION -> new IRConversion(LEAF, "ENUM", "TargetEnum", NodeId.ROOT, T, C, O, SR);
            case PIPE -> new IRPipe(2, NodeId.ROOT, T, C, O, SR);
            case ONLY_EXISTS -> new IROnlyExists(1, NodeId.ROOT, T, C, O, SR);
            case SYMBOL_NAV -> new IRSymbolNav("function", NodeId.ROOT, T, C, O, SR);
            case CLOSURE_PARAM -> new IRClosureParam("p", NodeId.ROOT, T, C, O, SR);
            case META_OUTPUT_APPLY -> new IRMetaOutputApply("F", NodeId.ROOT, T, C, O, SR);
            case ALL_ANY_COMPARE -> new IRAllAnyCompare("EQ", "ANY", NodeId.ROOT, T, C, O, SR);
            case SYN_ITEM_NAV -> new IRSynItemNav("f", NodeId.ROOT, T, C, O, SR);
            case CHOICE_OPTION_NAV -> new IRChoiceOptionNav("h", "Opt", NodeId.ROOT, T, C, O, SR);
            case DISPATCH_INPUT_REF -> new IRDispatchInputRef("in", NodeId.ROOT, T, C, O, SR);
            case DEEP_FEATURE_NAV -> new IRDeepFeatureNav(LEAF, "f", NodeId.ROOT, T, C, O, SR);
            case RECORD_FEATURE_NAV -> new IRRecordFeatureNav("h", "day", "date", NodeId.ROOT, T, C, O, SR);
            case META_ITEM_NAV -> new IRMetaItemNav("f", NodeId.ROOT, T, C, O, SR);
            case RECORD_RECEIVER_NAV ->
                new IRRecordReceiverNav(LEAF, "day", "date", NodeId.ROOT, T, C, O, SR);
            case QUALIFIER_ITEM_NAV -> new IRQualifierItemNav("scheme", NodeId.ROOT, T, C, O, SR);
            case SWITCH_OP -> new IRSwitchOp(2, true, NodeId.ROOT, T, C, O, SR);
            case DEFAULT_OP -> new IRDefaultOp(NodeId.ROOT, T, C, O, SR);
            case MEMBERSHIP_OP ->
                new IRMembershipOp(IRMembershipOp.Op.CONTAINS, NodeId.ROOT, T, C, O, SR);
            case COLLECT_OP -> new IRCollectOp(IRCollectOp.Op.MAX, false, NodeId.ROOT, T, C, O, SR);
            case OUTPUT_REF -> new IROutputRef("result", NodeId.ROOT, T, C, O, SR);
            case META_PARAM_REF -> new IRMetaParamRef("price", NodeId.ROOT, T, C, O, SR);
            case RULE_INPUT_NAV -> new IRRuleInputNav("leg", NodeId.ROOT, T, C, O, SR);
            case IMPLICIT_ATTR_NAV -> new IRImplicitAttrNav("leg", NodeId.ROOT, T, C, O, SR);
            case CONDITION_INSTANCE -> new IRConditionInstance("Leg", NodeId.ROOT, T, C, O, SR);
            case WITH_META_OP -> new IRWithMetaOp(2, NodeId.ROOT, T, C, O, SR);
            case JOIN_OP -> new IRJoinOp(true, NodeId.ROOT, T, C, O, SR);
            case OUTPUT_ALIAS_NAV -> new IROutputAliasNav("outAlias", "rate", NodeId.ROOT, T, C, O, SR);
            case LIBRARY_APPLY -> new IRLibraryApply("Min", NodeId.ROOT, T, C, O, SR);
            case QUALIFIER_RECEIVER_NAV ->
                new IRQualifierReceiverNav(LEAF, "reference", NodeId.ROOT, T, C, O, SR);
            case CHOICE_RECEIVER_NAV ->
                new IRChoiceReceiverNav(LEAF, "Asset", "Observable", NodeId.ROOT, T, C, O, SR);
        };
    }
}
