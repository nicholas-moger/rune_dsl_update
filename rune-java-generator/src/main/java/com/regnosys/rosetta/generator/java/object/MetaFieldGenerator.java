package com.regnosys.rosetta.generator.java.object;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.expressions.supporting.RWithMetaEntry;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.MetaFieldTemplateModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaEnum;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RType;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates FieldWithMeta and ReferenceWithMeta wrapper classes for
 * attributes annotated with [metadata ...] in the Rosetta DSL.
 *
 * <p>Not a JavaClassGenerator — metafield wrappers are deduplicated
 * across the entire workspace, not per-model. Called once by
 * JavaCodeGenerator after per-model generators.
 *
 * <p>Spec: D32 in docs/specs/2026-04-14-m7a-metafield-generators-design.md
 */
public class MetaFieldGenerator {

    /** Which wrapper type to generate. */
    public enum MetaKind { NONE, FIELD_WITH_META, REFERENCE_WITH_META }

    /** Three-valued type category — controls template conditionals. */
    public enum ValueCategory { ENUM, PRIMITIVE, COMPOSITE }

    /**
     * Specification for a single metafield wrapper class to generate.
     * Deduplicated by (wrappedTypeSimpleName, wrappedTypeNamespace, kind).
     */
    public record MetaFieldSpec(
            String wrappedTypeSimpleName,
            String wrappedTypeFqn,
            DottedPath wrappedTypeNamespace,
            MetaKind kind,
            ValueCategory valueCategory
    ) {}

    private static final String FWM_TEMPLATE_GROUP = "templates/java-field-with-meta.stg";
    private static final String RWM_TEMPLATE_GROUP = "templates/java-reference-with-meta.stg";

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final TemplateRenderer renderer;

    public MetaFieldGenerator(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(FWM_TEMPLATE_GROUP);
        this.renderer.loadGroupFromClasspath(RWM_TEMPLATE_GROUP);
    }

    /**
     * Detect which meta wrapping to apply based on annotation refs.
     * [metadata reference/address] -> REFERENCE_WITH_META
     * [metadata scheme/id/location] -> FIELD_WITH_META
     * [metadata key/template] -> NONE (type-level only)
     *
     * <p>Shared with RJavaPojoInterface — extracted as static utility.
     */
    public static MetaKind detectMetaKind(RAttribute attr) {
        return detectMetaKind(allMetaAnnotationRefs(attr));
    }

    /**
     * The attribute's annotation refs UNDER THE OVERRIDE-INHERITANCE LAW: an
     * {@code override} attribute's meta annotations are the overridden parent
     * chain's UNION its own — upstream
     * {@code RosettaTypeProvider.getRMetaAttributesOfSymbol} recurses
     * {@code getParentAttribute} and unions the sets, so {@code [metadata ...]}
     * persists through an override that does not restate it (hold-out witness,
     * PR #410: {@code Foo3.parentList GrandChild} keeps Foo2's
     * {@code [metadata reference]} → {@code ReferenceWithMetaGrandChild};
     * {@code Foo2.stringAttr string(maxLength: 42)} keeps Foo1's
     * {@code [metadata scheme]} → the plain getter stays
     * {@code FieldWithMetaString}). Corpus-byte-neutral by construction: the
     * frozen 9.83.0 corpus has zero overrides whose parent carries meta the
     * override does not restate (waves A–D TRUE 100% could not have held
     * otherwise), so every corpus attribute resolves to its own refs verbatim.
     * Non-override attributes (and function inputs/outputs, whose parent is
     * not an {@code RDataType}) return their own refs unchanged.
     */
    public static List<RAnnotationRef> allMetaAnnotationRefs(RAttribute attr) {
        RAttribute parent = com.regnosys.rosetta.generator.java.function.RuleReferenceTraversal
                .parentAttributeOf(attr);
        if (parent == null) {
            return attr.annotationRefs();
        }
        List<RAnnotationRef> all = new ArrayList<>(allMetaAnnotationRefs(parent));
        all.addAll(attr.annotationRefs());
        return all;
    }

    /**
     * Annotation-list overload of {@link #detectMetaKind(RAttribute)}.
     *
     * <p>P2.1.3c β1 meta extension: choice options ({@link RChoiceOption})
     * carry their own {@code [metadata ...]} annotations but are not {@code RAttribute} instances,
     * so the {@code RAttribute}-typed overload above cannot be reused directly. Both overloads share
     * the same qualifier→kind mapping below.
     */
    public static MetaKind detectMetaKind(List<RAnnotationRef> annotationRefs) {
        boolean hasReference = false;
        boolean hasFieldMeta = false;
        for (RAnnotationRef ref : annotationRefs) {
            if ("metadata".equals(ref.annotationName())) {
                String qualifier = ref.qualifierName().orElse("");
                if ("reference".equals(qualifier) || "address".equals(qualifier)) {
                    hasReference = true;
                }
                if ("scheme".equals(qualifier) || "id".equals(qualifier)
                        || "location".equals(qualifier)) {
                    hasFieldMeta = true;
                }
            }
        }
        if (hasReference) return MetaKind.REFERENCE_WITH_META;
        if (hasFieldMeta) return MetaKind.FIELD_WITH_META;
        return MetaKind.NONE;
    }

    /**
     * Collect all unique metafield wrapper specs from the workspace.
     */
    public List<MetaFieldSpec> collectSpecs() {
        Map<String, MetaFieldSpec> deduplicated = new LinkedHashMap<>();

        for (RModel model : generatorModel.files()) {
            // P2.1.1 T3.3 — respect emission filter (e.g. own-corpus-only on
            // DRR cells with transitive-CDM closure loaded). Mirrors the
            // outer-loop ENUM/POJO check + closes the noGolden leakage where
            // CDM-only attributes triggered shared metafield wrappers not in
            // DRR's golden. Types referenced by surviving (own-corpus)
            // attributes still resolve through {@code generatorModel.getType}
            // against the full workspace.
            if (!generatorModel.shouldGenerate(model)) continue;

            // Collect all attributes with metadata annotations from all root elements:
            // data types and function inputs/outputs.
            List<RAttribute> allAttributes = new ArrayList<>();
            // P2.1.3c β1 meta extension: choice options can carry their own
            // [metadata location/address/...] annotations (e.g. `choice Index:
            // InterestRateIndex [metadata location]` → FieldWithMetaInterestRateIndex).
            // These options are NOT RAttribute, so collected via a parallel pathway below.
            List<RChoiceOption> allChoiceOptions = new ArrayList<>();
            for (var element : model.rootElements()) {
                if (element instanceof RDataType dt) {
                    allAttributes.addAll(dt.attributes());
                } else if (element instanceof RFunction fn) {
                    allAttributes.addAll(fn.inputs());
                    fn.output().ifPresent(allAttributes::add);
                } else if (element instanceof RChoice ch) {
                    allChoiceOptions.addAll(ch.options());
                }
            }

            for (RAttribute attr : allAttributes) {
                collectFromAttribute(attr, deduplicated);
            }
            for (RChoiceOption opt : allChoiceOptions) {
                collectFromChoiceOption(opt, deduplicated);
            }
        }

        // facet withMetaExprWrapperCollect (PR #421): upstream's collection stream is
        // the UNION of attribute-derived meta types and every WithMetaOperation
        // expression's own meta-annotated type (MetaFieldGenerator.xtend
        // streamObjects L40-47: `model.eAllOfType(WithMetaOperation).stream
        // .map[RMetaAnnotatedType].filter[hasAttributeMeta]`, distinct) — a
        // `with-meta {scheme: …}` in a function body pulls FieldWithMetaString into
        // emission even when NO attribute anywhere declares `[metadata scheme]`
        // (hold-out witness expr-with-meta). The walk runs as a SECOND pass after
        // the attribute/choice-option pass completes so every existing spec keeps
        // its exact current precedence in the dedup map (spec content carries no
        // per-model state, so order cannot change file bytes — putIfAbsent only
        // ever APPENDS new wrapper names). The one deliberate exclusion:
        // an argument whose inferred type is NOTHING or unresolvable is SKIPPED —
        // upstream maps `empty with-meta {…}` to ReferenceWithMetaVoid (a wrapper
        // over java.lang.Void), the cdm 6.20.6 upstream defect this fork
        // deliberately does not reproduce (the #405 user-directed PERMANENT waiver
        // entry — d11-known-divergent.txt); the prior M7b-era note here recorded
        // both this Void case and FieldWithMetaCommodityPayout — a wrapper that
        // existed only in the RETIRED pre-#85 corpus (the note's 88/90
        // arithmetic); the frozen 9.83.0 corpus has no with-meta-on-Payout
        // carrier, so the Void case is its only expression-only wrapper, and any
        // future attribute/expression overlap is absorbed by the putIfAbsent
        // dedup (Seat-1 #421 MF-1).
        for (RModel model : generatorModel.files()) {
            if (!generatorModel.shouldGenerate(model)) continue;
            for (var element : model.rootElements()) {
                collectFromWithMetaExprs(element, deduplicated);
            }
        }

        return new ArrayList<>(deduplicated.values());
    }

    /**
     * facet withMetaExprWrapperCollect (PR #421) — walk one root element's AST for
     * {@link RWithMetaExpr} nodes and register the wrapper spec each one implies:
     * the wrapped type is the ARGUMENT's workspace-inferred type (upstream
     * {@code getRMetaAnnotatedType(expr)} carries the argument's RType; the entry
     * keys supply the meta attributes), the kind mirrors
     * {@link #detectMetaKind(List)}'s qualifier law on the ENTRY names
     * (reference/address win over scheme/id/location; key/template are type-level
     * meta only — an expression whose entries are ALL type-meta has no attribute
     * meta and registers nothing, upstream's {@code filter[hasAttributeMeta]}).
     * NOTHING-typed or unresolvable arguments are skipped (the ReferenceWithMetaVoid
     * exclusion — see the collectSpecs note). The node guard fails LOUD on
     * exhaustion (the A2 law; a silent partial walk could drop a wrapper the
     * function bodies reference — the Copilot #420 census-guard class).
     */
    private void collectFromWithMetaExprs(RNode root, Map<String, MetaFieldSpec> deduplicated) {
        Deque<RNode> stack = new ArrayDeque<>();
        stack.push(root);
        int guard = 0;
        while (!stack.isEmpty()) {
            if (guard++ >= 100_000) {
                throw new IllegalStateException(
                        "collectFromWithMetaExprs: AST walk exceeded 100000 nodes (root '"
                        + root + "') — a partial walk could silently drop a metafield"
                        + " wrapper the generated bodies reference; failing loud instead");
            }
            RNode cur = stack.pop();
            if (cur instanceof RWithMetaExpr wm) {
                MetaKind kind = entryMetaKind(wm.entries());
                if (kind != MetaKind.NONE) {
                    RMetaAnnotatedType inferred =
                            generatorModel.workspace().getInferredType(wm.argument());
                    if (inferred != null && !inferred.isMissing()
                            && inferred.type() != RBasicType.NOTHING) {
                        JavaClass<?> wrappedJavaType =
                                typeTranslator.toJavaReferenceType(inferred.type());
                        if (wrappedJavaType != null) {
                            putSpec(wrappedJavaType, kind, deduplicated);
                        }
                    }
                }
            }
            for (RNode child : cur.children()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    /**
     * The with-meta ENTRY-name → kind mapping — the same qualifier law as
     * {@link #detectMetaKind(List)} (reference/address → REFERENCE_WITH_META,
     * else scheme/id/location → FIELD_WITH_META, key/template contribute no
     * attribute meta), applied to {@link RWithMetaEntry} keys instead of
     * {@code [metadata …]} annotation refs. PUBLIC because it is THE one
     * entry-name→kind resolution: the collection walk above and the render
     * seat's own-type arm ({@code ConstructionHandler.tryOwnTypeWithMeta})
     * both consult it — a second mapping could let the emitted wrapper class
     * and the wrapper the body references disagree.
     */
    public static MetaKind entryMetaKind(List<RWithMetaEntry> entries) {
        boolean hasReference = false;
        boolean hasFieldMeta = false;
        for (RWithMetaEntry entry : entries) {
            String name = entry.metaName();
            if ("reference".equals(name) || "address".equals(name)) {
                hasReference = true;
            }
            if ("scheme".equals(name) || "id".equals(name) || "location".equals(name)) {
                hasFieldMeta = true;
            }
        }
        if (hasReference) return MetaKind.REFERENCE_WITH_META;
        if (hasFieldMeta) return MetaKind.FIELD_WITH_META;
        return MetaKind.NONE;
    }

    private void collectFromAttribute(RAttribute attr, Map<String, MetaFieldSpec> deduplicated) {
        MetaKind metaKind = detectMetaKind(attr);
        if (metaKind == MetaKind.NONE) return;

        RType wrappedType = generatorModel.getType(attr);
        JavaClass<?> wrappedJavaType = typeTranslator.toJavaReferenceType(wrappedType);
        putSpec(wrappedJavaType, metaKind, deduplicated);
    }

    /**
     * P2.1.3c β1 meta extension: collect FieldWithMeta/ReferenceWithMeta wrapper
     * specs from a choice option's {@code [metadata ...]} annotations.
     *
     * <p>Mirrors {@link #collectFromAttribute(RAttribute, Map)} but operates on
     * {@link RChoiceOption}, which is not an {@link RAttribute}. The wrapped type
     * is the option's {@code typeCall} resolved through
     * {@link GeneratorModel#resolveTypeCall}; the qualifier→kind mapping is
     * identical (delegated to {@link #detectMetaKind(List)}).
     */
    private void collectFromChoiceOption(RChoiceOption opt,
                                          Map<String, MetaFieldSpec> deduplicated) {
        MetaKind metaKind = detectMetaKind(opt.annotationRefs());
        if (metaKind == MetaKind.NONE) return;

        RType wrappedType = generatorModel.resolveTypeCall(opt.typeCall());
        JavaClass<?> wrappedJavaType = typeTranslator.toJavaReferenceType(wrappedType);
        putSpec(wrappedJavaType, metaKind, deduplicated);
    }

    /**
     * Register one wrapper spec in the dedup map — the single spec-construction
     * seat shared by the attribute, choice-option and with-meta-expression
     * collectors (PR #421 extraction; the map op is {@code putIfAbsent}, so the
     * first collector to name a wrapper wins and later registrations are no-ops —
     * spec content is order-independent, see the collectSpecs walk note).
     */
    private void putSpec(JavaClass<?> wrappedJavaType, MetaKind metaKind,
                         Map<String, MetaFieldSpec> deduplicated) {
        String simpleName = wrappedJavaType.getSimpleName();
        DottedPath namespace = resolveWrappedTypeNamespace(wrappedJavaType);
        ValueCategory category = resolveValueCategory(wrappedJavaType);

        String key = simpleName + "|" + namespace.withDots() + "|" + metaKind;
        deduplicated.putIfAbsent(key, new MetaFieldSpec(
                simpleName,
                wrappedJavaType.getCanonicalName().withDots(),
                namespace,
                metaKind,
                category));
    }

    /**
     * Resolve namespace for the wrapped type. Generated types use their
     * package; builtins use com.rosetta.model.
     */
    private static DottedPath resolveWrappedTypeNamespace(JavaType valueType) {
        if (valueType instanceof RGeneratedJavaClass<?> gen) {
            return DottedPath.splitOnDots(gen.getPackageName().toString());
        }
        return DottedPath.splitOnDots("com.rosetta.model");
    }

    /**
     * Determine the value category from the Java type.
     */
    private static ValueCategory resolveValueCategory(JavaType javaType) {
        if (javaType instanceof RJavaEnum) return ValueCategory.ENUM;
        if (javaType instanceof RGeneratedJavaClass<?>) return ValueCategory.COMPOSITE;
        return ValueCategory.PRIMITIVE;
    }

    /**
     * Generate all metafield wrapper classes into the output map.
     */
    public void generate(Map<String, String> output) {
        List<MetaFieldSpec> specs = collectSpecs();
        for (MetaFieldSpec spec : specs) {
            MetaFieldTemplateModel model = buildModel(spec);
            String templateGroup = spec.kind() == MetaKind.FIELD_WITH_META
                    ? FWM_TEMPLATE_GROUP : RWM_TEMPLATE_GROUP;
            String templateName = spec.kind() == MetaKind.FIELD_WITH_META
                    ? "fieldWithMetaFile" : "referenceWithMetaFile";
            String code = renderer.render(templateGroup, templateName, "m", model);
            String prefix = spec.kind() == MetaKind.FIELD_WITH_META
                    ? "FieldWithMeta" : "ReferenceWithMeta";
            String className = prefix + spec.wrappedTypeSimpleName();
            DottedPath metafieldsPackage = spec.wrappedTypeNamespace().child("metafields");
            String filePath = metafieldsPackage.withForwardSlashes() + "/" + className + ".java";
            output.put(filePath, code);
        }
    }

    /**
     * Build the template model for a MetaFieldSpec.
     */
    MetaFieldTemplateModel buildModel(MetaFieldSpec spec) {
        String prefix = spec.kind() == MetaKind.FIELD_WITH_META
                ? "FieldWithMeta" : "ReferenceWithMeta";
        String className = prefix + spec.wrappedTypeSimpleName();
        DottedPath metafieldsPackage = spec.wrappedTypeNamespace().child("metafields");
        String packageName = metafieldsPackage.withDots();

        // Model name is first segment of package
        String modelName = packageName.split("\\.")[0];

        boolean isComposite = spec.valueCategory() == ValueCategory.COMPOSITE;
        boolean isEnum = spec.valueCategory() == ValueCategory.ENUM;

        var imports = new ImportCollector(packageName);

        // Import the wrapped type
        imports.addImport(spec.wrappedTypeFqn());

        // Common imports for both wrapper types
        imports.addImport("com.rosetta.model.lib.RosettaModelObject");
        imports.addImport("com.rosetta.model.lib.RosettaModelObjectBuilder");
        imports.addImport("com.rosetta.model.lib.annotations.Accessor");
        imports.addImport("com.rosetta.model.lib.annotations.AccessorType");
        imports.addImport("com.rosetta.model.lib.annotations.RosettaAttribute");
        imports.addImport("com.rosetta.model.lib.annotations.RosettaDataType");
        imports.addImport("com.rosetta.model.lib.annotations.RuneAttribute");
        imports.addImport("com.rosetta.model.lib.annotations.RuneDataType");
        imports.addImport("com.rosetta.model.lib.annotations.RuneMetaType");
        imports.addImport("com.rosetta.model.lib.meta.BasicRosettaMetaData");
        imports.addImport("com.rosetta.model.lib.meta.RosettaMetaData");
        imports.addImport("com.rosetta.model.lib.path.RosettaPath");
        imports.addImport("com.rosetta.model.lib.process.BuilderMerger");
        imports.addImport("com.rosetta.model.lib.process.BuilderProcessor");
        imports.addImport("com.rosetta.model.lib.process.Processor");
        imports.addImport("java.util.Objects");

        if (spec.kind() == MetaKind.FIELD_WITH_META) {
            imports.addImport("com.rosetta.model.lib.GlobalKey");
            imports.addImport("com.rosetta.model.lib.meta.FieldWithMeta");
            imports.addImport("com.rosetta.model.metafields.MetaFields");
        } else {
            imports.addImport("com.rosetta.model.lib.meta.Reference");
            imports.addImport("com.rosetta.model.lib.meta.ReferenceWithMeta");
            imports.addImport("com.rosetta.model.lib.process.AttributeMeta");
        }

        return new MetaFieldTemplateModel(
                packageName, className, spec.wrappedTypeSimpleName(),
                spec.wrappedTypeFqn(), isComposite, isEnum, modelName,
                imports.getImports());
    }
}
