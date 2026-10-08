package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.enums.QualifiableKind;
import com.regnosys.rosetta.ast.expressions.supporting.RWithMetaEntry;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.model.RQualifiableConfig;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import com.regnosys.rosetta.symbols.derived.GeneratedInputRule;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RRecordType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;

/**
 * THE MODEL-LEVEL RECONCILE (v3.3 seat 8, PR #644 - the property gate, {@code PLAN.md} § B families 5, 6, 7, 10, 11).
 *
 * <p>{@link IRModelNode} is the FIRST model-level node the declaration IR carries, and it exists because four of the
 * files in the data-type emitter's all-or-nothing unit are not functions of any one type:
 *
 * <ul>
 *   <li><b>{@code package-info.java}</b> (888 vendored files) is a function of the NAMESPACE - every emitted model's
 *       {@code definition}, deduped inside its namespace by a {@code LinkedHashSet}
 *       ({@code JavaPackageInfoGenerator.java:52-80}; 345 of the 1,283 definitions are deduped away). Both halves
 *       here accumulate over the models the pass hands them, in the pass's own order, so the fact asserted per model
 *       is the namespace's definition list SO FAR - and the {@code shouldGenerate} gate is asserted as a fact of its
 *       own ({@code model.shouldGenerate}), since the host's population is the gate.</li>
 *   <li>the <b>{@code *Meta} QUALIFY wing</b> - the {@code [qualification]} functions that a qualifiable ROOT's
 *       {@code *Meta} lists. <b>Risk R4, stated:</b> {@code ModelMetaGenerator.java:382} matches a function's first
 *       input against the root by NODE IDENTITY ({@code ==}); the IR has no node identity and must match by RESOLVED
 *       QUALIFIED NAME. {@code model.qualify.matched} is exactly that substitution under assertion - the source half
 *       computes the match by identity and the IR half by name, and they must agree. What keeps the substitution
 *       sound is the index's AMBIGUOUS refusal: a qualified name declared twice is a refusal, never a first-wins
 *       pick. The ROOT ITSELF IS TWO PRODUCERS TOO (round 1, MF-2): the source half reads the generator's own
 *       {@code RQualifiableConfig.firstRoot} ({@code :107-119}) over {@code generatorModel.files()}, and the IR half
 *       reads {@link IRModelIndex#firstRoot(String)} - the same first-wins-in-load-order law computed over the MODEL
 *       NODES of the whole workspace, dependency models included. {@code model.qualify.root.<kind>} asserts the two
 *       against each other, per kind, so the root the wing hangs on is a fact the emitter can be wrong about rather
 *       than a call the IR half makes to the source. It is workspace-wide on BOTH halves because a drr cell's roots
 *       live in the transitive CDM it loads for resolution only.</li>
 *   <li>the <b>wrapper set's two non-declaration sources</b> - every function's SIGNATURE (412 of the 1,079 vendored
 *       wrappers are function-input or function-output facts) and every {@code with-meta} EXPRESSION (0 on the
 *       corpus: every use is claimed first by an attribute or a function, so the family's lane is a unit fixture).
 *       The parser's synthesized zero-input placeholder is excluded on BOTH halves by the parser's own predicate,
 *       {@code GeneratedInputRule.isSynthesized} - never by its name here.</li>
 * </ul>
 *
 * <p>The {@code with-meta} walk is re-walked here rather than read: the adapter pushes an element's children on a
 * stack and collects every {@link RWithMetaExpr}; so does this reconciler, and the two lists must agree entry for
 * entry, argument type for argument type, refusal for refusal. The two NAMED refusals are the law, not a silent drop:
 * an argument the workspace cannot type reads {@code missing}, and one typed {@code nothing} reads {@code nothing} -
 * the latter is the {@code ReferenceWithMetaVoid} line, the fork's ONE PERMANENT waiver (the cdm 6.20.6 upstream
 * defect). Both halves ask the SAME typing engine ({@code RWorkspace.getInferredType}) - that engine is a shared
 * producer here exactly as the corpus lookup is in the declaration channel, and the independence this class buys is
 * over the WALK and the classification, not over the type system.
 */
final class IRModelReconciler {

    private final GeneratorModel generatorModel;
    /** The workspace-wide model index - the IR half's OWN producer of the first-wins qualifiable root (MF-2). */
    private final IRModelIndex modelIndex;
    private final JavaTypeTranslator typeTranslator;

    private final AtomicInteger declarations = new AtomicInteger();
    private final AtomicInteger facts = new AtomicInteger();
    private final AtomicInteger mismatches = new AtomicInteger();

    /** The namespace to definitions accumulation, one map per half - the package-info law under assertion. */
    private final Map<String, List<String>> sourceDefinitions = new LinkedHashMap<>();
    private final Map<String, List<String>> irDefinitions = new LinkedHashMap<>();

    private IRDerivedLie lie = IRDerivedLie.NONE;

    /**
     * @param modelIndex the workspace-wide model index, built over the PASS'S OWN adapter - a CONSTRUCTOR argument
     *     and never a nullable setter, because an absent index would silently turn the IR half of the root law back
     *     into a read of the source's (round 1, MF-2 and SF-2: no silent fallback anywhere in this seat)
     */
    IRModelReconciler(GeneratorModel generatorModel, IRModelIndex modelIndex) {
        this.generatorModel = Objects.requireNonNull(generatorModel, "generatorModel");
        this.modelIndex = Objects.requireNonNull(modelIndex, "modelIndex");
        this.typeTranslator = new JavaTypeTranslator(new JavaTypeUtil());
    }

    /** TEST SEAM - see {@link IRDerivedLie}. No production caller. */
    void lie(IRDerivedLie lie) {
        this.lie = Objects.requireNonNull(lie, "lie");
    }

    /** Books ONE model as attempted - BEFORE the adapter runs. */
    void attempt() {
        declarations.incrementAndGet();
    }

    /** A throw IS a mismatch. */
    void threw() {
        mismatches.incrementAndGet();
    }

    /** {@code {models attempted, facts asserted, mismatches}}. */
    int[] stats() {
        return new int[] {declarations.get(), facts.get(), mismatches.get()};
    }

    // ============================================================================================ the reconcile

    List<String> reconcile(RModel ast, IRModelNode ir) {
        // THE POPULATION GUARD, not a fact (round 1, SF-7). `shouldGenerate` had been asserted as
        // `same(x, TRUE)` - a one-producer tautology that counted in factsAsserted and could never be red for a
        // reason either half owns. The package-info accumulation below is an accumulation over the EMITTED models,
        // so a model outside the filter reaching this reconciler is a HOST error: it is refused by name here.
        if (!generatorModel.shouldGenerate(ast)) {
            throw new GenerationException("IR model reconcile: the model '" + ast.namespace() + "' is not emitted by"
                    + " this cell, and the namespace definition list this reconcile accumulates is the EMITTED"
                    + " models' - a non-emitted model must not reach the model reconciler", null, ast);
        }
        Check c = new Check("MODEL " + ir.name());
        String namespace = ast.namespace() == null ? "" : ast.namespace();

        c.same("model.namespace", namespace, ir.namespace());
        c.same("model.definition", ast.definition(), ir.definition());
        c.same("model.version", ast.version(), ir.version());

        // ---- D11: the namespace to definitions map, and its LinkedHashSet dedup ----------------------------
        if (ast.definition().isPresent()) {
            sourceDefinitions.computeIfAbsent(namespace, k -> new ArrayList<>()).add(ast.definition().get());
        }
        ir.definition().ifPresent(
                definition -> irDefinitions.computeIfAbsent(ir.namespace(), k -> new ArrayList<>()).add(definition));
        List<String> sourceNamespaceDefinitions =
                deduped(sourceDefinitions.getOrDefault(namespace, List.of()), true);
        List<String> irNamespaceDefinitions = deduped(irDefinitions.getOrDefault(ir.namespace(), List.of()),
                lie != IRDerivedLie.PACKAGE_INFO_NO_DEDUP);
        c.same("model." + namespace + ".definitions", IRDerivedFacts.join(sourceNamespaceDefinitions),
                IRDerivedFacts.join(irNamespaceDefinitions));

        // ---- family 5: the qualifiable configurations ------------------------------------------------------
        List<RQualifiableConfig> configurations = ast.configurations();
        c.same("model.configs.size", configurations.size(), ir.qualifiableConfigs().size());
        for (int i = 0; i < Math.min(configurations.size(), ir.qualifiableConfigs().size()); i++) {
            RQualifiableConfig configuration = configurations.get(i);
            IRQualifiableConfig irConfiguration = ir.qualifiableConfigs().get(i);
            c.same("model.config." + i + ".kind", configuration.kind().name(), irConfiguration.kind());
            c.same("model.config." + i + ".root",
                    renderDeclaration(configuration.rootType().orElse(null), IRKind.STRUCT,
                            configuration.rootTypeName()),
                    renderReference(irConfiguration.rootType()));
        }

        // ---- D07: the [qualification] functions, and the first-input match (risk R4) -----------------------
        List<RFunction> qualifications = new ArrayList<>();
        List<RFunction> functions = new ArrayList<>();
        for (RRootElement element : ast.rootElements()) {
            if (!(element instanceof RFunction function)) {
                continue;
            }
            functions.add(function);
            if (isQualification(function) && !declaredInputs(function).isEmpty()) {
                qualifications.add(function);
            }
        }
        c.same("model.qualify.size", qualifications.size(), ir.qualificationFunctions().size());
        for (int i = 0; i < Math.min(qualifications.size(), ir.qualificationFunctions().size()); i++) {
            RFunction function = qualifications.get(i);
            IRQualificationFunction irFunction = ir.qualificationFunctions().get(i);
            c.same("model.qualify." + i + ".name", function.name(), irFunction.name());
            RAttribute firstInput = declaredInputs(function).get(0);
            c.same("model.qualify." + i + ".firstInputType",
                    renderDeclaration(resolvedDeclarationOf(firstInput), IRKind.STRUCT,
                            firstInput.typeCall() == null ? "" : firstInput.typeCall().typeName()),
                    renderReference(irFunction.firstInputType()));
        }
        // the ROOT the wing hangs on, TWO PRODUCERS, per kind (MF-2): the generator's own first-wins scan over
        // `generatorModel.files()` against the index's own over the MODEL NODES of the same workspace
        for (String kind : IRQualifiableConfig.KINDS) {
            c.same("model.qualify.root." + kind, sourceRootName(kind), irRootName(kind));
        }
        c.same("model.qualify.matched", IRDerivedFacts.join(sourceMatchedQualifications(qualifications)),
                IRDerivedFacts.join(irMatchedQualifications(ir)));

        // ---- family 10: every function's SIGNATURE (the wrapper set's function sources) --------------------
        c.same("model.signatures.size", functions.size(), ir.functionSignatures().size());
        for (int i = 0; i < Math.min(functions.size(), ir.functionSignatures().size()); i++) {
            RFunction function = functions.get(i);
            IRFunctionSignature signature = ir.functionSignatures().get(i);
            String at = "model.signature." + i + ".";
            c.same(at + "name", function.name(), signature.name());
            List<RAttribute> inputs = declaredInputs(function);
            c.same(at + "inputs.size", inputs.size(), signature.inputs().size());
            for (int j = 0; j < Math.min(inputs.size(), signature.inputs().size()); j++) {
                c.same(at + "input." + j, renderAttribute(inputs.get(j)),
                        renderField(signature.inputs().get(j)));
            }
            c.same(at + "output.present", function.output().isPresent(), signature.output().isPresent());
            if (function.output().isPresent() && signature.output().isPresent()) {
                c.same(at + "output", renderAttribute(function.output().get()),
                        renderField(signature.output().get()));
            }
        }

        // ---- family 11: the with-meta EXPRESSIONS, walked independently ------------------------------------
        List<RWithMetaExpr> uses = new ArrayList<>();
        for (RRootElement element : ast.rootElements()) {
            collectWithMetaUses(element, uses);
        }
        c.same("model.withMetaUses.size", uses.size(), ir.withMetaUses().size());
        for (int i = 0; i < Math.min(uses.size(), ir.withMetaUses().size()); i++) {
            RWithMetaExpr use = uses.get(i);
            IRWithMetaUse irUse = ir.withMetaUses().get(i);
            String at = "model.withMeta." + i + ".";
            List<String> entryNames = new ArrayList<>();
            for (RWithMetaEntry entry : use.entries()) {
                entryNames.add(entry.metaName() == null ? "" : entry.metaName());
            }
            c.same(at + "entryNames", IRDerivedFacts.join(entryNames),
                    IRDerivedFacts.join(irUse.entryNames()));
            c.same(at + "kind", MetaFieldGenerator.entryMetaKind(use.entries()).name(),
                    irEntryMetaKind(irUse.entryNames()).name());
            RMetaAnnotatedType inferred = inferredTypeOf(use);
            String sourceRefusal;
            String sourceArgument;
            if (inferred == null || inferred.isMissing()) {
                sourceRefusal = "missing";
                sourceArgument = "-";
            } else if (inferred.type() == com.regnosys.rosetta.types.RBasicType.NOTHING) {
                sourceRefusal = "nothing";
                sourceArgument = "-";
            } else {
                sourceRefusal = "-";
                sourceArgument = renderInferred(inferred.type());
            }
            c.same(at + "refusal", sourceRefusal, irUse.refusal().orElse("-"));
            c.same(at + "argumentType", sourceArgument, irUse.argumentType()
                    .map(type -> IRDerivedFacts.join(List.of(renderReference(type),
                            renderArguments(irUse))))
                    .orElse("-"));
        }

        return c.close();
    }

    // -------------------------------------------------------------------------------------- the qualify wing

    /**
     * The match by NODE IDENTITY, exactly as {@code ModelMetaGenerator.java:377-382} takes it: the first DECLARED
     * input's type call resolved through the workspace, compared {@code ==} against the workspace's first root of
     * each kind ({@code RQualifiableConfig.firstRoot}).
     */
    private List<String> sourceMatchedQualifications(List<RFunction> qualifications) {
        RDataType productRoot =
                RQualifiableConfig.firstRoot(generatorModel.files(), QualifiableKind.IS_PRODUCT).orElse(null);
        RDataType eventRoot =
                RQualifiableConfig.firstRoot(generatorModel.files(), QualifiableKind.IS_EVENT).orElse(null);
        List<String> matched = new ArrayList<>();
        for (RFunction function : qualifications) {
            RNode firstInputType = resolvedDeclarationOf(declaredInputs(function).get(0));
            if (firstInputType != null && (firstInputType == productRoot || firstInputType == eventRoot)) {
                matched.add(function.name());
            }
        }
        return matched;
    }

    /** The SOURCE's first-wins root of a kind, by qualified name - {@code RQualifiableConfig.firstRoot}'s answer. */
    private String sourceRootName(String kind) {
        return RQualifiableConfig.firstRoot(generatorModel.files(), QualifiableKind.valueOf(kind))
                .map(IRModelReconciler::qualifiedNameOf).orElse("-");
    }

    /** The IR's own first-wins root of a kind - {@link IRModelIndex#firstRoot(String)}, the other producer. */
    private String irRootName(String kind) {
        return modelIndex.firstRoot(kind).flatMap(IRType::resolvedQualifiedName).orElse("-");
    }

    /**
     * The SAME match by RESOLVED QUALIFIED NAME - the IR's substitute for node identity (risk R4). The roots
     * themselves come from the IR's OWN index (MF-2): {@link IRModelIndex} adapts EVERY model of the workspace, the
     * cell's emission filter or not, so the IR half reads the same LOAD-ORDER population the source's
     * {@code firstRoot} reads - a drr cell's roots live in the transitive CDM it loads for resolution only - and the
     * two answers are held against each other by {@code model.qualify.root.<kind>} above.
     */
    private List<String> irMatchedQualifications(IRModelNode ir) {
        if (lie == IRDerivedLie.QUALIFY_NO_FIRST_INPUT_MATCH) {
            List<String> all = new ArrayList<>();
            for (IRQualificationFunction function : ir.qualificationFunctions()) {
                all.add(function.name());
            }
            return all;
        }
        List<String> rootNames = new ArrayList<>();
        for (String kind : IRQualifiableConfig.KINDS) {
            modelIndex.firstRoot(kind).flatMap(IRType::resolvedQualifiedName).ifPresent(rootNames::add);
        }
        List<String> matched = new ArrayList<>();
        for (IRQualificationFunction function : ir.qualificationFunctions()) {
            Optional<String> qualifiedName = function.firstInputType().resolvedQualifiedName();
            if (qualifiedName.isPresent() && rootNames.contains(qualifiedName.get())) {
                matched.add(function.name());
            }
        }
        return matched;
    }

    // ------------------------------------------------------------------------------------- the with-meta walk

    /** The adapter's own walk, re-walked: a stack over {@code children()}, the same guard, the same order. */
    private static void collectWithMetaUses(RNode root, List<RWithMetaExpr> uses) {
        Deque<RNode> stack = new ArrayDeque<>();
        stack.push(root);
        int guard = 0;
        while (!stack.isEmpty()) {
            if (guard++ >= 100_000) {
                throw new IllegalStateException("the with-meta reconcile walk exceeded 100000 nodes under '" + root
                        + "' - a partial walk would compare a truncated list against a whole one");
            }
            RNode current = stack.pop();
            if (current instanceof RWithMetaExpr withMeta) {
                uses.add(withMeta);
            }
            for (RNode child : current.children()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    /** The seam's read, tolerant of a node the engine never saw: null - and so a refusal - never a throw. */
    private RMetaAnnotatedType inferredTypeOf(RWithMetaExpr use) {
        try {
            return use.argument() == null ? null : generatorModel.workspace().getInferredType(use.argument());
        } catch (RuntimeException untypeable) {
            return null;
        }
    }

    /** {@code MetaFieldGenerator.entryMetaKind}'s law over the IR's entry NAMES. */
    static MetaFieldGenerator.MetaKind irEntryMetaKind(List<String> entryNames) {
        boolean hasReference = false;
        boolean hasFieldMeta = false;
        for (String name : entryNames) {
            if ("reference".equals(name) || "address".equals(name)) {
                hasReference = true;
            }
            if ("scheme".equals(name) || "id".equals(name) || "location".equals(name)) {
                hasFieldMeta = true;
            }
        }
        if (hasReference) {
            return MetaFieldGenerator.MetaKind.REFERENCE_WITH_META;
        }
        if (hasFieldMeta) {
            return MetaFieldGenerator.MetaKind.FIELD_WITH_META;
        }
        return MetaFieldGenerator.MetaKind.NONE;
    }

    /**
     * An inferred type rendered in the SAME spelling {@link #renderReference} gives the IR's reference plus its
     * literal constraint arguments: the alias chain is unwrapped to its LEAF (the adapter's {@code aliasLeaf}), a
     * declared leaf is its qualified name and a builtin its own name, and a {@code pattern} constraint is the
     * COMPILED pattern's source text on both halves.
     */
    private static String renderInferred(RType type) {
        RType leaf = type;
        int depth = 0;
        while (leaf instanceof RAliasType alias && depth++ < IRDerivedFactsReconciler.MAX_ALIAS_DEPTH) {
            leaf = alias.refersTo();
        }
        String reference;
        if (leaf instanceof RDataTypeRef data) {
            reference = "STRUCT:" + qualifiedNameOf(data.astNode());
        } else if (leaf instanceof REnumTypeRef enumeration) {
            reference = "ENUM:" + qualifiedNameOf(enumeration.astNode());
        } else if (leaf instanceof RChoiceTypeRef choice) {
            reference = "CHOICE:" + qualifiedNameOf(choice.astNode());
        } else if (leaf instanceof RNumberType) {
            reference = "BASIC_TYPE:number";
        } else if (leaf instanceof RStringType) {
            reference = "BASIC_TYPE:string";
        } else if (leaf instanceof RRecordType record) {
            reference = "RECORD_TYPE:" + record.name();
        } else if (leaf instanceof com.regnosys.rosetta.types.RBasicType basic) {
            reference = "BASIC_TYPE:" + basic.name();
        } else {
            reference = "?:" + leaf;
        }
        List<String> arguments = new ArrayList<>();
        if (leaf instanceof RNumberType number) {
            number.digits().ifPresent(d -> arguments.add("digits=" + d));
            number.fractionalDigits().ifPresent(f -> arguments.add("fractionalDigits=" + f));
            number.min().ifPresent(m -> arguments.add("min=" + (m.signum() < 0 ? "-" : "")
                    + m.abs().toPlainString()));
            number.max().ifPresent(m -> arguments.add("max=" + (m.signum() < 0 ? "-" : "")
                    + m.abs().toPlainString()));
        }
        if (leaf instanceof RStringType string) {
            string.minLength().ifPresent(m -> arguments.add("minLength=" + m));
            string.maxLength().ifPresent(m -> arguments.add("maxLength=" + m));
            string.pattern().ifPresent(p -> arguments.add("pattern=" + p.pattern()));
        }
        return reference + "," + "[" + IRDerivedFacts.join(arguments) + "]";
    }

    /** The IR's own constraint arguments, in the canonical order the adapter states them. */
    private static String renderArguments(IRWithMetaUse use) {
        List<String> arguments = new ArrayList<>();
        use.typeArguments().forEach(argument -> arguments.add(argument.parameter() + "="
                + (argument.negated() ? "-" : "") + argument.literalValue().orElse("")));
        return "[" + IRDerivedFacts.join(arguments) + "]";
    }

    // ------------------------------------------------------------------------------------------ the renderers

    /** A resolved declaration, or the WRITTEN name behind a {@code ?} when nothing resolved it. */
    private static String renderDeclaration(RNode declaration, IRKind unresolvedKind, String writtenName) {
        if (declaration == null) {
            return unresolvedKind.name() + ":?" + (writtenName == null ? "" : writtenName);
        }
        IRKind kind = declaration instanceof RChoice ? IRKind.CHOICE
                : declaration instanceof REnumeration ? IRKind.ENUM
                : declaration instanceof RTypeAlias ? IRKind.TYPE_ALIAS : IRKind.STRUCT;
        return kind.name() + ":" + qualifiedNameOf(declaration);
    }

    /**
     * An IR reference in the same spelling: a declared type by its resolved qualified name; a BUILTIN (a resolved name
     * and no namespace - the registry declares it in none) by its own name, as {@link #renderInferred} spells it; an
     * UNRESOLVED reference (no resolved name at all) as the written name behind a {@code ?}.
     */
    private static String renderReference(IRType reference) {
        String name = reference.resolvedQualifiedName()
                .or(reference::resolvedName)
                .orElseGet(() -> "?" + reference.name());
        return reference.kind().name() + ":" + name;
    }

    /**
     * One function input or output on the SOURCE half: its name, the OLD GENERATOR's own Java type for it and its
     * cardinality bucket - the facts the wrapper collector and the signature emitter read
     * ({@code MetaFieldGenerator.java:175-177}).
     */
    private String renderAttribute(RAttribute attribute) {
        RType resolved;
        try {
            resolved = generatorModel.getType(attribute);
        } catch (RuntimeException unresolvable) {
            resolved = com.regnosys.rosetta.types.RMissingType.INSTANCE;
        }
        String javaType;
        try {
            javaType = typeTranslator.toJavaReferenceType(resolved).toString();
        } catch (RuntimeException refused) {
            javaType = IRJavaTypeNames.legacyRefusalToken(resolved, refused);
        }
        return attribute.name() + ":" + javaType + ":" + bucket(attribute) + ":"
                + IRDerivedFacts.metaKind(annotationsOf(attribute)).name();
    }

    /** The same field on the IR half, its Java type derived from the IR facts ALONE ({@link IRJavaTypeNames}). */
    private static String renderField(IRField field) {
        String javaType;
        try {
            javaType = IRJavaTypeNames.of(field.type(), field.typeArguments());
        } catch (RuntimeException refused) {
            javaType = refused instanceof IRJavaTypeNames.Refusal named
                    ? IRJavaTypeNames.REFUSED + named.reason().token() : IRJavaTypeNames.REFUSED + "throw";
        }
        return field.name() + ":" + javaType + ":" + field.cardinality().name() + ":"
                + IRDerivedFacts.metaKind(field.annotations()).name();
    }

    /**
     * A function input's or output's own {@code [metadata …]} annotations, rendered as {@link IRAnnotationUse}s so
     * the ONE qualifier law ({@link IRDerivedFacts#metaKind}) serves both halves. A function attribute
     * never has an override parent ({@code parentAttributeOf} returns null when the enclosing is not a data type),
     * so the union is its own refs - which is why no chain is walked here.
     */
    private static List<IRAnnotationUse> annotationsOf(RAttribute attribute) {
        List<IRAnnotationUse> annotations = new ArrayList<>();
        for (RAnnotationRef ref : MetaFieldGenerator.allMetaAnnotationRefs(attribute)) {
            annotations.add(new IRAnnotationUse(ref.annotationName(), ref.qualifierName()));
        }
        return annotations;
    }

    /**
     * The cardinality BUCKET of a declared cardinality, under {@code AstToIRAdapter.cardinality}'s OWN null guards
     * ({@code :917-929}, round 1 SF-6): an absent lower bound reads ZERO and an absent upper bound reads SINGULAR
     * unless the cardinality is unbounded. The adapter guards both; this half must guard both too, or a
     * partially-written cardinality reads as a NullPointerException on the source half against a clean bucket on the
     * IR half - a throw where the fact is "the two agree".
     */
    private static String bucket(RAttribute attribute) {
        return attribute.cardinality().map(cardinality -> {
            boolean many = cardinality.isUnbounded()
                    || (cardinality.sup() != null
                            && cardinality.sup().compareTo(java.math.BigInteger.ONE) > 0);
            boolean zero = cardinality.inf() == null || cardinality.inf().signum() == 0;
            return (zero ? "ZERO" : "ONE") + "_TO_" + (many ? "MANY" : "ONE");
        }).orElse("ONE_TO_ONE");
    }

    // --------------------------------------------------------------------------------------------- the helpers

    /** The DECLARED inputs - the parser's synthesized placeholder excluded by the parser's OWN predicate. */
    private static List<RAttribute> declaredInputs(RFunction function) {
        List<RAttribute> declared = new ArrayList<>();
        for (RAttribute input : function.inputs()) {
            if (!GeneratedInputRule.isSynthesized(input)) {
                declared.add(input);
            }
        }
        return declared;
    }

    private static boolean isQualification(RFunction function) {
        for (RAnnotationRef ref : function.annotationRefs()) {
            if ("qualification".equals(ref.annotationName())) {
                return true;
            }
        }
        return false;
    }

    private RNode resolvedDeclarationOf(RAttribute attribute) {
        if (attribute.typeCall() == null) {
            return null;
        }
        return attribute.typeCall().referencedTypeId()
                .map(id -> generatorModel.workspace().resolveTypeLike(id)).orElse(null);
    }

    private static String qualifiedNameOf(RNode declaration) {
        if (declaration == null) {
            return "?";
        }
        Optional<String> namespace = com.regnosys.rosetta.ast.util.AstWalker
                .findAncestor(declaration, RModel.class).map(RModel::namespace);
        String simpleName = declaration instanceof RDataType data ? data.name()
                : declaration instanceof RChoice choice ? choice.name()
                : declaration instanceof REnumeration enumeration ? enumeration.name()
                : declaration instanceof RTypeAlias alias ? alias.name() : String.valueOf(declaration);
        return namespace.filter(ns -> !ns.isEmpty()).map(ns -> ns + "." + simpleName).orElse(simpleName);
    }

    private static List<String> deduped(List<String> definitions, boolean dedupe) {
        return dedupe ? new ArrayList<>(new LinkedHashSet<>(definitions)) : new ArrayList<>(definitions);
    }

    /** The {@code IRDeclarationReconciler.Check} idiom - one subject, every fact counted, the mismatches named. */
    private final class Check {
        private final String subject;
        private final List<String> failed = new ArrayList<>();
        private int asserted;

        Check(String subject) {
            this.subject = subject;
        }

        void same(String fact, Object expected, Object actual) {
            asserted++;
            if (!Objects.equals(expected, actual)) {
                failed.add("IR/AST reconciliation failed for " + subject + ": " + fact + " - the source says "
                        + expected + ", the IR says " + actual);
            }
        }

        List<String> close() {
            facts.addAndGet(asserted);
            mismatches.addAndGet(failed.size());
            return failed;
        }
    }
}
