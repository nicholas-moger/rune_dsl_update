package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaEnum;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.rosetta.util.types.JavaClass;

/**
 * THE WRAPPER SPEC RECONCILE (v3.3 seat 8, PR #644 - the property gate, {@code PLAN.md} § D families D08 and D09).
 *
 * <p>The {@code FieldWithMeta*} / {@code ReferenceWithMeta*} wrapper classes (1,079 on the 25 vendored cells, 85 on
 * chaos) are in the data-type emitter's all-or-nothing unit, and they are the ONE family in it that is not a function
 * of a single declaration: {@code MetaFieldGenerator.collectSpecs} ({@code :151-222}) deduplicates across the WHOLE
 * emitted workspace and draws from FOUR sources - a data type's attributes ({@code :173-174}), a function's inputs
 * and output ({@code :175-177}), a choice's options ({@code :178-179}) and, in a SECOND pass after every model,
 * every {@code with-meta} expression ({@code :214-219}). The measured split is 667 declaration attributes, 4 function
 * inputs, 408 function outputs, 0 choice options and 0 with-meta expressions - <b>412 of the 1,079 wrappers, 38.2%,
 * are FUNCTION-SIGNATURE facts</b>, which is why {@link IRModelNode} carries every function's signature.
 *
 * <p>So this reconciler runs ONCE PER CELL, over every emitted model node and every emitted {@code STRUCT} /
 * {@code CHOICE} node, and asserts, per dedup key: the SET of collectors that PRODUCE it, and the four fields of
 * the spec the wrapper file is written from - its KIND ({@code FIELD_WITH_META} / {@code REFERENCE_WITH_META}), its
 * VALUE CATEGORY ({@code ENUM} tested BEFORE {@code COMPOSITE}, as {@code resolveValueCategory} tests it,
 * {@code :367-371}), its wrapped type's NAMESPACE and its wrapped type's FQN - each against the generator's own
 * {@link MetaFieldGenerator.MetaFieldSpec}. Plus the key SET and its size.
 *
 * <p><b>Why the FIRST CLAIMER is a census and not a gate.</b> An earlier cut of this class gated
 * {@code wrapper.<key>.source} - which of the four collectors got to a key first. That is not a byte-bearing fact
 * and it is no longer asserted. {@code collectSpecs}' own comment says it outright ("spec content carries no
 * per-model state, so order cannot change file bytes - putIfAbsent only ever APPENDS new wrapper names"), and
 * {@code putSpec} ({@code :338-351}) is written so that a spec's four content fields are read off the WRAPPED JAVA
 * TYPE and off nothing else - no per-model, per-collector or per-walk state enters them - so two collectors that
 * name one key build one identical spec and the wrapper file is the same either way. That is not quite "a pure
 * function of the dedup key", and the difference is named rather than glossed (round 1, NIT-6):
 * {@code resolveWrappedTypeNamespace} ({@code :357-362}) forces {@code com.rosetta.model} for every NON-generated
 * wrapped type, so two non-generated types sharing a simple name would share a KEY while carrying different
 * {@code wrappedTypeFqn}s. Nothing is assumed about that here - the four content fields are asserted PER KEY on both
 * halves against the generator's own spec, so a key whose content is not key-determined reads RED by name.
 * Gating the first claimer therefore gated the WALK ORDER, not the
 * emitter's input - and the walk order is the one thing the IR cannot reconstruct: within a single model file
 * {@code collectSpecs} interleaves a data type's attributes with a function's inputs in ROOT-ELEMENT order, while
 * the IR's declarations arrive as a list and its function signatures hang off the model node. cdm/5.38.0 read RED
 * on exactly that: {@code NonNegativeQuantitySchedule|cdm.base.math|FIELD_WITH_META} and
 * {@code PriceSchedule|cdm.observable.asset|FIELD_WITH_META} are claimed first by a {@code functionInput} in file
 * order and by a {@code declarationAttribute} in any order the IR can produce - the same two wrapper files either
 * way. What IS gated is the SOURCE SET: every collector that produces the key, on both halves, sorted. That is the
 * fact the emitter depends on (drop the function collectors and 412 wrapper files vanish - lane D08), and it is
 * order-free. The first-claimer split the seat's probe reported (667 / 4 / 408) is a CENSUS of that walk and is
 * reported as one.
 *
 * <p>The mirror is never a second opinion with nothing to answer to: its key set is asserted EQUAL to the public
 * {@code collectSpecs()}' ({@code wrapper.mirrorEqualsCollectSpecs}).
 */
final class IRWrapperReconciler {

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final MetaFieldGenerator metaFieldGenerator;

    private final AtomicInteger specs = new AtomicInteger();
    private final AtomicInteger facts = new AtomicInteger();
    private final AtomicInteger mismatches = new AtomicInteger();

    private List<MetaFieldGenerator.MetaFieldSpec> collected;
    private final IRTypeIndex index;
    private IRDerivedLie lie = IRDerivedLie.NONE;

    /**
     * @param index the workspace-wide type index the IR half resolves an override's parent attribute through - the
     *     {@code [metadata …]} UNION law ({@code MetaFieldGenerator.allMetaAnnotationRefs}) needs the supertype
     *     chain. A CONSTRUCTOR argument since round 1 (SF-2): it had been a nullable setter with a silent fallback
     *     to a field's OWN annotations, which is the identity of the union on every corpus where no wrapper hangs
     *     off an override - i.e. green for the wrong reason, and green by default if a caller ever forgot the setter
     */
    IRWrapperReconciler(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator, IRTypeIndex index) {
        this.generatorModel = Objects.requireNonNull(generatorModel, "generatorModel");
        this.typeTranslator = Objects.requireNonNull(typeTranslator, "typeTranslator");
        this.index = Objects.requireNonNull(index, "index");
        this.metaFieldGenerator = new MetaFieldGenerator(generatorModel, typeTranslator);
    }

    /** TEST SEAM - see {@link IRDerivedLie}. No production caller. */
    void lie(IRDerivedLie lie) {
        this.lie = Objects.requireNonNull(lie, "lie");
    }

    /** A throw on either half IS a mismatch. */
    void threw() {
        mismatches.incrementAndGet();
    }

    /**
     * {@code {specs reconciled, facts asserted, mismatches}}. The FIRST slot is the SPEC population, not an attempt
     * count: the host's line is {@code specs=N expected=N}, and {@code expected} is the generator's own
     * {@link #expected()} - a cell that threw before the walk reports zero specs against a non-zero expectation and
     * is red, which is the law LAW 84 asks for. That is ALSO why this class books no cell counter (round 1, NIT-2
     * removed the write-only one): the population this line asserts is the spec set, and nothing here ever read a
     * count of cells.
     */
    int[] stats() {
        return new int[] {specs.get(), facts.get(), mismatches.get()};
    }

    /** The generator's OWN spec count - the host recomputes it too, and the two must agree (a third {@code same}). */
    int expected() {
        return collectSpecs().size();
    }

    private List<MetaFieldGenerator.MetaFieldSpec> collectSpecs() {
        if (collected == null) {
            collected = metaFieldGenerator.collectSpecs();
        }
        return collected;
    }

    // ============================================================================================ the reconcile

    /**
     * @param models       every emitted model's node, in the cell's own pass order
     * @param declarations every emitted {@code STRUCT} / {@code CHOICE} node, in the cell's own pass order
     */
    List<String> reconcile(List<IRModelNode> models, List<IRTypeNode> declarations) {
        Check c = new Check("WRAPPERS");

        Split source = sourceSplit();
        Split ir = irSplit(models, declarations);
        specs.set(ir.keys.size());

        // the CONTENT oracle is the generator's OWN MetaFieldSpec, not the mirror: a spec is fully determined by
        // its dedup key, so its four content fields are the facts the wrapper FILE is written from
        Set<String> generatorKeys = new LinkedHashSet<>();
        Map<String, MetaFieldGenerator.MetaFieldSpec> byKey = new LinkedHashMap<>();
        for (MetaFieldGenerator.MetaFieldSpec spec : collectSpecs()) {
            String key = key(spec.wrappedTypeSimpleName(), spec.wrappedTypeNamespace().withDots(), spec.kind());
            generatorKeys.add(key);
            byKey.putIfAbsent(key, spec);
        }
        c.same("wrapper.mirrorEqualsCollectSpecs", sorted(generatorKeys), sorted(source.keys));
        c.same("wrapper.specs.size", generatorKeys.size(), ir.keys.size());
        c.same("wrapper.keys", sorted(generatorKeys), sorted(ir.keys));

        Set<String> everyKey = new LinkedHashSet<>(source.keys);
        everyKey.addAll(ir.keys);
        for (String key : everyKey) {
            MetaFieldGenerator.MetaFieldSpec spec = byKey.get(key);
            c.same("wrapper." + key + ".sources", sorted(source.sources.get(key)), sorted(ir.sources.get(key)));
            c.same("wrapper." + key + ".kind", spec == null ? null : spec.kind().name(), ir.kind.get(key));
            c.same("wrapper." + key + ".valueCategory",
                    spec == null ? null : spec.valueCategory().name(), ir.category.get(key));
            c.same("wrapper." + key + ".wrappedTypeNamespace",
                    spec == null ? null : spec.wrappedTypeNamespace().withDots(), ir.namespace.get(key));
            c.same("wrapper." + key + ".wrappedTypeFqn",
                    spec == null ? null : spec.wrappedTypeFqn(), ir.fqn.get(key));
            // the MIRROR's own content against the generator's spec - so the mirror answers for what it computes
            c.same("wrapper." + key + ".mirrorContent", spec == null ? null : content(spec.kind().name(),
                    spec.valueCategory().name(), spec.wrappedTypeNamespace().withDots(), spec.wrappedTypeFqn()),
                    source.keys.contains(key) ? content(source.kind.get(key), source.category.get(key),
                            source.namespace.get(key), source.fqn.get(key)) : null);
        }
        return c.close();
    }

    // ------------------------------------------------------------------------------------------- the two halves

    /**
     * One spec set: per dedup key, the SET of collectors that produce it and the spec CONTENT it carries.
     *
     * <p>The content is written once - by the first producer, as {@code putIfAbsent} writes it - because a
     * {@code MetaFieldSpec} is a pure function of the dedup key ({@code putSpec}, {@code :338-351}): two collectors
     * that name the same key necessarily build the same spec. The SOURCES, by contrast, accumulate: which collector
     * got there FIRST is not a fact any byte depends on (see the class javadoc), so it is not gated.
     */
    private static final class Split {
        private final Set<String> keys = new LinkedHashSet<>();
        private final Map<String, Set<String>> sources = new LinkedHashMap<>();
        private final Map<String, String> kind = new LinkedHashMap<>();
        private final Map<String, String> category = new LinkedHashMap<>();
        private final Map<String, String> namespace = new LinkedHashMap<>();
        private final Map<String, String> fqn = new LinkedHashMap<>();

        void put(String key, String source, String kind, String category, String namespace, String fqn) {
            sources.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(source);
            if (!keys.add(key)) {
                return;                      // putIfAbsent: the spec CONTENT is written once and is key-determined
            }
            this.kind.put(key, kind);
            this.category.put(key, category);
            this.namespace.put(key, namespace);
            this.fqn.put(key, fqn);
        }
    }

    /**
     * THE SOURCE HALF - {@code MetaFieldGenerator.collectSpecs}' own four collectors ({@code :151-222}), its own
     * dedup key and its own spec construction, walked in the generator's OWN order: per emitted model, the attribute
     * list built over {@code rootElements()} with a data type's attributes and a function's inputs and output
     * INTERLEAVED ({@code :172-181}), then the choice options; and, after EVERY model, the {@code with-meta} pass
     * ({@code :214-219}). The order is kept because it is the generator's, not because any fact depends on it: what
     * this half PRODUCES is the key set, the per-key SOURCE SET and the key-determined spec content, all three
     * order-free.
     */
    private Split sourceSplit() {
        Split split = new Split();
        List<RModel> emitted = new ArrayList<>();
        for (RModel model : generatorModel.files()) {
            if (generatorModel.shouldGenerate(model)) {
                emitted.add(model);
            }
        }
        for (RModel model : emitted) {
            List<RAttribute> attributes = new ArrayList<>();
            List<String> sources = new ArrayList<>();
            List<RChoiceOption> options = new ArrayList<>();
            for (RRootElement element : model.rootElements()) {
                collectSourceElement(element, attributes, sources, options);
            }
            putSourceAttributes(split, attributes, sources);
            putSourceOptions(split, options);
        }
        for (RModel model : emitted) {
            for (RRootElement element : model.rootElements()) {
                collectSourceWithMeta(element, split);
            }
        }
        return split;
    }

    private static void collectSourceElement(RRootElement element, List<RAttribute> attributes, List<String> sources,
            List<RChoiceOption> options) {
        if (element instanceof RDataType dataType) {
            for (RAttribute attribute : dataType.attributes()) {
                attributes.add(attribute);
                sources.add("declarationAttribute");
            }
        } else if (element instanceof RFunction function) {
            for (RAttribute input : function.inputs()) {
                attributes.add(input);
                sources.add("functionInput");
            }
            if (function.output().isPresent()) {
                attributes.add(function.output().get());
                sources.add("functionOutput");
            }
        } else if (element instanceof RChoice choice) {
            options.addAll(choice.options());
        }
    }

    private void putSourceAttributes(Split split, List<RAttribute> attributes, List<String> sources) {
        for (int i = 0; i < attributes.size(); i++) {
            RAttribute attribute = attributes.get(i);
            MetaFieldGenerator.MetaKind kind = MetaFieldGenerator.detectMetaKind(attribute);
            if (kind == MetaFieldGenerator.MetaKind.NONE) {
                continue;
            }
            putSourceSpec(split, typeTranslator.toJavaReferenceType(generatorModel.getType(attribute)), kind,
                    sources.get(i));
        }
    }

    private void putSourceOptions(Split split, List<RChoiceOption> options) {
        for (RChoiceOption option : options) {
            MetaFieldGenerator.MetaKind kind = MetaFieldGenerator.detectMetaKind(option.annotationRefs());
            if (kind == MetaFieldGenerator.MetaKind.NONE) {
                continue;
            }
            putSourceSpec(split, typeTranslator.toJavaReferenceType(generatorModel.resolveTypeCall(option.typeCall())),
                    kind, "choiceOption");
        }
    }

    /** {@code collectFromWithMetaExprs} ({@code :239-272}), guard and both NAMED refusals included. */
    private void collectSourceWithMeta(RNode root, Split split) {
        Deque<RNode> stack = new ArrayDeque<>();
        stack.push(root);
        int guard = 0;
        while (!stack.isEmpty()) {
            if (guard++ >= 100_000) {
                throw new IllegalStateException("the wrapper reconcile's with-meta walk exceeded 100000 nodes");
            }
            RNode current = stack.pop();
            if (current instanceof RWithMetaExpr withMeta) {
                MetaFieldGenerator.MetaKind kind = MetaFieldGenerator.entryMetaKind(withMeta.entries());
                if (kind != MetaFieldGenerator.MetaKind.NONE) {
                    RMetaAnnotatedType inferred = withMeta.argument() == null ? null
                            : generatorModel.workspace().getInferredType(withMeta.argument());
                    if (inferred != null && !inferred.isMissing() && inferred.type() != RBasicType.NOTHING) {
                        JavaClass<?> wrapped = typeTranslator.toJavaReferenceType(inferred.type());
                        if (wrapped != null) {
                            putSourceSpec(split, wrapped, kind, "withMetaExpression");
                        }
                    }
                }
            }
            for (RNode child : current.children()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    /** {@code putSpec} ({@code :338-351}) with its two resolvers ({@code :357-371}), the ENUM arm tested first. */
    private static void putSourceSpec(Split split, JavaClass<?> wrapped, MetaFieldGenerator.MetaKind kind,
            String source) {
        if (wrapped == null) {
            return;
        }
        String namespace = wrapped instanceof RGeneratedJavaClass<?> generated
                ? generated.getPackageName().toString() : "com.rosetta.model";
        String category = wrapped instanceof RJavaEnum ? "ENUM"
                : wrapped instanceof RGeneratedJavaClass<?> ? "COMPOSITE" : "PRIMITIVE";
        split.put(key(wrapped.getSimpleName(), namespace, kind), source, kind.name(), category, namespace,
                wrapped.getCanonicalName().withDots());
    }

    /**
     * THE IR HALF - the same four sources, from the IR alone: a {@code STRUCT} node's fields (their
     * {@code [metadata …]} read under the OVERRIDE UNION through the index), a model node's function signatures, a
     * {@code CHOICE} node's option fields, and, in the second pass, every model node's {@code with-meta} uses. The
     * wrapped type's Java class comes from {@link IRJavaTypeNames} and its namespace and value category from the
     * reference's leaf KIND: a declared leaf ({@code STRUCT} / {@code CHOICE} / {@code ENUM}) keeps its own package
     * and is {@code ENUM} or {@code COMPOSITE}; everything else takes {@code com.rosetta.model} and is
     * {@code PRIMITIVE}.
     */
    private Split irSplit(List<IRModelNode> models, List<IRTypeNode> declarations) {
        Split split = new Split();
        List<String> namespaces = new ArrayList<>();
        for (IRModelNode model : models) {
            if (!namespaces.contains(model.namespace())) {
                namespaces.add(model.namespace());
            }
        }
        for (IRTypeNode declaration : declarations) {
            String namespace = declaration.namespace().orElse("");
            if (!namespaces.contains(namespace)) {
                namespaces.add(namespace);
            }
        }
        for (String namespace : namespaces) {
            for (IRTypeNode declaration : declarations) {
                if (declaration.kind() != IRKind.STRUCT || !namespace.equals(declaration.namespace().orElse(""))) {
                    continue;
                }
                // the chain and the effective surface ONCE per declaration - the override union needs both, and
                // rebuilding them per field would walk the supertype chain once for every attribute of the corpus
                List<IRTypeNode> chain = IRDerivedFacts.chain(declaration, index);
                Map<String, IRDerivedFacts.Owned> effective =
                        IRDerivedFacts.effectiveAttributes(chain);
                for (IRField field : declaration.fields()) {
                    putIrSpec(split, field, irMetaKindOf(field, chain, effective), "declarationAttribute");
                }
            }
            if (lie != IRDerivedLie.WRAPPER_NO_FUNCTION_SOURCE) {
                for (IRModelNode model : models) {
                    if (!namespace.equals(model.namespace())) {
                        continue;
                    }
                    for (IRFunctionSignature signature : model.functionSignatures()) {
                        for (IRField input : signature.inputs()) {
                            putIrSpec(split, input, IRDerivedFacts.metaKind(input.annotations()),
                                    "functionInput");
                        }
                        if (signature.output().isPresent()) {
                            IRField output = signature.output().get();
                            putIrSpec(split, output, IRDerivedFacts.metaKind(output.annotations()),
                                    "functionOutput");
                        }
                    }
                }
            }
            for (IRTypeNode declaration : declarations) {
                if (declaration.kind() != IRKind.CHOICE || !namespace.equals(declaration.namespace().orElse(""))) {
                    continue;
                }
                for (IRField option : declaration.fields()) {
                    putIrSpec(split, option, IRDerivedFacts.metaKind(option.annotations()),
                            "choiceOption");
                }
            }
        }
        for (IRModelNode model : models) {
            for (IRWithMetaUse use : model.withMetaUses()) {
                if (use.refusal().isPresent() || use.argumentType().isEmpty()) {
                    continue;                // the NAMED refusals: `nothing` (the ReferenceWithMetaVoid waiver), `missing`
                }
                MetaFieldGenerator.MetaKind kind = IRModelReconciler.irEntryMetaKind(use.entryNames());
                if (kind == MetaFieldGenerator.MetaKind.NONE) {
                    continue;
                }
                putIrSpec(split, use.argumentType().get(), use.typeArguments(), kind, "withMetaExpression");
            }
        }
        return split;
    }

    /**
     * The field's meta kind under the OVERRIDE UNION - always, with no "no index, own annotations only" arm (round 1,
     * SF-2): {@link IRDerivedFacts#chain} always returns at least the declaration itself, and a field that
     * overrides nothing unions to its own annotations by the law rather than by a fallback.
     */
    private static MetaFieldGenerator.MetaKind irMetaKindOf(IRField field, List<IRTypeNode> chain,
            Map<String, IRDerivedFacts.Owned> effective) {
        return IRDerivedFacts.metaKind(
                IRDerivedFacts.unionedAnnotationsOf(field, chain, effective, true));
    }

    private void putIrSpec(Split split, IRField field, MetaFieldGenerator.MetaKind kind, String source) {
        putIrSpec(split, field.type(), field.typeArguments(), kind, source);
    }

    private void putIrSpec(Split split, IRType reference, List<IRTypeArgument> arguments,
            MetaFieldGenerator.MetaKind kind, String source) {
        if (kind == MetaFieldGenerator.MetaKind.NONE) {
            return;
        }
        String canonical;
        try {
            canonical = IRJavaTypeNames.of(reference, arguments);
        } catch (RuntimeException refused) {
            // a reference the type gate REFUSES has no wrapper name; the key says so by name rather than silently
            split.put(key(IRJavaTypeNames.REFUSED + reference.name(), "?", kind), source, kind.name(), "?", "?", "?");
            return;
        }
        IRKind leafKind = reference.kind() == IRKind.TYPE_ALIAS
                ? reference.effectiveBase().map(IREffectiveBase::kind).orElse(IRKind.BASIC_TYPE)
                : reference.kind();
        boolean declared = leafKind == IRKind.STRUCT || leafKind == IRKind.CHOICE || leafKind == IRKind.ENUM;
        String simpleName = IRDerivedFacts.lastSegment(canonical);
        String namespace = declared && canonical.length() > simpleName.length()
                ? canonical.substring(0, canonical.length() - simpleName.length() - 1) : "com.rosetta.model";
        String category = leafKind == IRKind.ENUM && lie != IRDerivedLie.WRAPPER_ENUM_AS_COMPOSITE
                ? "ENUM" : declared ? "COMPOSITE" : "PRIMITIVE";
        split.put(key(simpleName, namespace, kind), source, kind.name(), category, namespace, canonical);
    }

    // --------------------------------------------------------------------------------------------- the helpers

    /** {@code MetaFieldGenerator.putSpec}'s own dedup key ({@code :344}). */
    private static String key(String simpleName, String namespace, MetaFieldGenerator.MetaKind kind) {
        return simpleName + "|" + namespace + "|" + kind;
    }

    /** The four spec fields the wrapper FILE is written from, in one spelling. */
    private static String content(String kind, String category, String namespace, String fqn) {
        return kind + "|" + category + "|" + namespace + "|" + fqn;
    }

    /**
     * A set in ONE spelling both halves produce: SORTED, so what is asserted is the set and never the walk order.
     * {@code null} - a key one half never produced at all - stays {@code null}, so the mismatch reads
     * "the source says null" rather than pretending an empty set was computed.
     */
    private static String sorted(Set<String> values) {
        if (values == null) {
            return null;
        }
        List<String> ordered = new ArrayList<>(values);
        Collections.sort(ordered);
        return IRDerivedFacts.join(ordered);
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
