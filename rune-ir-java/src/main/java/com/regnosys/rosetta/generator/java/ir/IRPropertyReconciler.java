package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.function.RuleReferenceTraversal;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.types.AttributeMetaType;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.rosetta.util.types.JavaType;

/**
 * THE PROPERTY RECONCILE (v3.3 seat 8, PR #644 - the property gate): every fact of the IR-derived
 * {@link IRPropertyModel} asserted, property for property and IN ORDER, against the OLD GENERATOR'S OWN OBJECT -
 * {@code RJavaPojoInterface}, the object whose {@code getAllProperties()} the emitted POJO bytes are written from. The
 * two halves are two PRODUCERS (LAW 69): the IR half computes the surface from {@link com.regnosys.rosetta.ir.adapter.IRTypeNode}
 * and {@link IRTypeIndex} alone and never touches an AST node; this half constructs the old generator's pojo and READS
 * its answers.
 *
 * <p>WHAT IS MIRRORED HERE rather than read, and why - said plainly, because a mirror without its reason is a second
 * implementation pretending to be an oracle:
 * <ul>
 *   <li>{@code RJavaPojoInterface.allDocReferences} is {@code private static} ({@code :539-556}), so the doc-reference
 *       UNION is walked here, through the very same PUBLIC {@code RuleReferenceTraversal.parentAttributeOf}
 *       ({@code :415-432}) the original recurses on. The union's CONTENTS are already asserted per attribute by the
 *       declaration reconcile; what this fact adds is the union's ORDER and COUNT.</li>
 *   <li>The Case 0 verdict is NOT mirrored: it is read on the oracle as the ABSENCE of a redeclared name from
 *       {@code getOwnProperties()}, which is exactly the observable the law produces ({@code :449-452}).</li>
 *   <li>The synthetic-{@code meta} predicate is {@code ValidatorScan.isSyntheticMeta}'s ({@code :143-147}), mirrored
 *       verbatim over the PUBLIC {@code JavaTypeUtil.META_FIELDS} / {@code META_AND_TEMPLATE_FIELDS} constants.</li>
 *   <li>The RENDERED javadoc is NOT mirrored at all: it is READ off the old generator's own
 *       {@code JavaPojoProperty.getJavadoc()} - the very string {@code ModelObjectGenerator:641-644} writes into the
 *       POJO - while the IR half reproduces it from the IR facts alone. Only the DEFINITION behind it is joined here,
 *       through the declaring pojo, because {@code JavaPojoProperty} keeps the rendered block and not its input.</li>
 * </ul>
 *
 * <p>The Java type is compared as ONE rendered string, the OLD GENERATOR'S OWN spelling ({@code JavaType.toString()} -
 * {@code JavaClass:202-205}, {@code JavaParameterizedType:223-226}); {@link IRPropertyModel}'s javadoc states the
 * same law for the half that derives it.
 *
 * <p>A missing or extra property is a {@code property.surface.size} and {@code property.surface.order} mismatch, not
 * an exception: the per-property facts then run over the pairs that exist, so one dropped property does not hide the
 * facts of every property behind it.
 */
final class IRPropertyReconciler {

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    private final AtomicInteger declarations = new AtomicInteger();
    private final AtomicInteger facts = new AtomicInteger();
    private final AtomicInteger mismatches = new AtomicInteger();

    IRPropertyReconciler(GeneratorModel generatorModel) {
        this.generatorModel = Objects.requireNonNull(generatorModel, "generatorModel");
        // the OLD GENERATOR'S OWN translator and type table, built exactly as IRDeclarationReconciler builds them
        this.typeUtil = new JavaTypeUtil();
        this.typeTranslator = new JavaTypeTranslator(typeUtil);
    }

    /** Books ONE declaration as attempted - called BEFORE the pojo is built, so a throwing one is still counted. */
    void attempt() {
        declarations.incrementAndGet();
    }

    /** A pojo (or reconcile) throw IS a mismatch - never a silent drop from the counters. */
    void threw() {
        mismatches.incrementAndGet();
    }

    /** {@code {declarations attempted, facts asserted, mismatches}} of this pass so far. */
    int[] stats() {
        return new int[] {declarations.get(), facts.get(), mismatches.get()};
    }

    /** The property surface of a {@code type}, against {@code new RJavaPojoInterface(dataType, …)}. */
    List<String> reconcile(String namespace, RDataType ast, IRPropertyModel ir) {
        RJavaPojoInterface oracle = new RJavaPojoInterface(ast, generatorModel, typeTranslator, typeUtil);
        Check c = new Check("PROPERTIES STRUCT " + qualified(namespace, ast.name()));
        List<String> declared = new ArrayList<>();
        for (RAttribute attribute : ast.attributes()) {
            declared.add(attribute.name());
        }
        surface(c, oracle, ir, declared);
        return c.close();
    }

    /** The property surface of a {@code choice}, against {@code new RJavaPojoInterface(choice, …)}. */
    List<String> reconcile(String namespace, RChoice ast, IRPropertyModel ir) {
        RJavaPojoInterface oracle = new RJavaPojoInterface(ast, generatorModel, typeTranslator, typeUtil);
        Check c = new Check("PROPERTIES CHOICE " + qualified(namespace, ast.name()));
        List<String> declared = new ArrayList<>();
        for (RChoiceOption option : ast.options()) {
            declared.add(option.typeCall() == null ? "" : option.typeCall().typeName());
        }
        surface(c, oracle, ir, declared);
        return c.close();
    }

    // ------------------------------------------------------------------------------------------------ the facts

    private void surface(Check c, RJavaPojoInterface oracle, IRPropertyModel ir, List<String> declaredNames) {
        List<JavaPojoProperty> own = copyOf(oracle.getOwnProperties());
        List<JavaPojoProperty> all = copyOf(oracle.getAllProperties());
        List<IRPropertyModel.IRProperty> irOwn = ir.ownProperties();
        List<IRPropertyModel.IRProperty> irAll = ir.allProperties();

        c.same("property.surface.size.own", own.size(), irOwn.size());
        c.same("property.surface.size.all", all.size(), irAll.size());
        // the ITERATION ORDER of the two collections IS the law (the re-put keeps the ancestor's position): one
        // joined name list each, so a moved property names the family rather than hiding inside a set compare
        c.same("property.surface.order.own", joinNames(own), joinIrNames(irOwn));
        c.same("property.surface.order.all", joinNames(all), joinIrNames(irAll));
        c.same("property.meta.type", syntheticMetaType(all), irSyntheticMetaType(irAll));

        for (int i = 0; i < Math.min(all.size(), irAll.size()); i++) {
            property(c, all.get(i), irAll.get(i));
        }

        // THE CASE 0 VERDICT, per DECLARED member: a redeclared attribute that produced no property is ABSENT from
        // getOwnProperties() - the observable the law itself produces (RJavaPojoInterface:449-452)
        Set<String> ownNames = new LinkedHashSet<>();
        for (JavaPojoProperty property : own) {
            ownNames.add(property.getName());
        }
        Set<String> irOwnNames = new LinkedHashSet<>();
        for (IRPropertyModel.IRProperty property : irOwn) {
            irOwnNames.add(property.name());
        }
        for (String declared : declaredNames) {
            c.same("property." + declared + ".specialized",
                    ownNames.contains(declared) ? "property" : "case0",
                    irOwnNames.contains(declared) ? "property" : "case0");
        }
    }

    private void property(Check c, JavaPojoProperty oracle, IRPropertyModel.IRProperty ir) {
        String at = "property." + oracle.getName() + ".";
        c.same(at + "name", oracle.getName(), ir.name());
        // THE RUNE NAME (v3.3 seat 9, PR #645 commit 9): the ATTRIBUTE half of the pruning-config key
        // GeneratorModel.isPruningDisabled reads (:116-119), which section 12's prune() / hasData() arms branch
        // on. The source half is the old generator's own slot (RJavaPojoInterface:440-443 fills it from the
        // declaration's name; JavaPojoProperty.specialize :105-113 keeps the parent's); the IR half carries the
        // same fact. They are equal on this fork for every property - which is a MEASURED equality from here on,
        // not an assumption the emitter is built on.
        c.same(at + "runeName", oracle.getRuneName(), ir.runeName());
        c.same(at + "javaType", render(oracle.getType()), ir.javaType());
        c.same(at + "required", oracle.isRequired(), ir.required());
        c.same(at + "multi", typeUtil.isList(oracle.getType()), ir.multi());
        c.same(at + "getterName", oracle.getGetterCompatibilityName(), ir.getterCompatibilityName());
        c.same(at + "setterName", oracle.getSetterCompatibilityName(), ir.setterCompatibilityName());
        c.same(at + "getterOverridesParent", oracle.getterOverridesParentGetter(), ir.getterOverridesParentGetter());
        c.same(at + "compatibleWithParent", oracle.isCompatibleTypeWithParent(), ir.compatibleTypeWithParent());
        c.same(at + "sameTypeAsParent", oracle.isSameTypeAsParent(), ir.sameTypeAsParent());
        c.same(at + "chainDepth", chainDepth(oracle), ir.parentChainDepth());
        // THE ITEM-KIND FACT (v3.3 seat 9, PR #645 commit 5 - the gate commit): the source half is the OLD
        // GENERATOR'S OWN predicate, called through the seam ModelObjectGenerator.itemIsRosettaModelObject - the
        // very function its private isModelObj(prop) now delegates to (:1846-1849), so the two can never drift; the
        // IR half derives it from IRType.kind() on the item's declaration alone. Nothing of the lattice test is
        // reproduced here: a mirror of isRosettaModelObject in this class would be a second implementation
        // pretending to be an oracle.
        c.same(at + "itemIsRosettaModelObject", ModelObjectGenerator.itemIsRosettaModelObject(oracle, typeUtil),
                ir.itemIsRosettaModelObject());
        // THE ANCESTOR CHAIN (same commit): the source half walks PojoCompatEmitter's OWN chain - the link every
        // compat arm and the chain's import arm (ModelObjectGenerator:288-330) are written from - through the seam
        // ModelObjectGenerator.compatAncestorChain, and reads each rung's four inputs off the old generator's own
        // property. The IR half carries the same rungs, nearest first, built where the specialization is built.
        c.same(at + "parentChain.types", sourceParentChainRows(oracle), irParentChainRows(ir.parentChain()));
        // THE ITEM-IS-ENUM FACT (v3.3 seat 9, PR #645 commit 8): the source half is the OLD GENERATOR'S OWN
        // expression, called through the seam ModelObjectGenerator.itemIsEnum - the one
        // ModelObjectBoilerplate:205-206 now delegates to, so the seam IS the site the hashCode enum arm is
        // written from; the IR half derives it from IRType.kind() on the item's declaration alone.
        c.same(at + "itemIsEnum", ModelObjectGenerator.itemIsEnum(oracle, typeUtil), ir.itemIsEnum());
        // THE META-VALUE-KIND FACT (same commit): an Optional on BOTH halves, because the old generator's own
        // fact is a nullable getMetaValueType() and "no value type" is not "a value type that is not a model
        // object". The source half is the seam the two hasData arms (:1325-1327, :1341-1342) and the list
        // meta-value setter (:1585) delegate to.
        c.same(at + "metaValueIsRosettaModelObject",
                ModelObjectGenerator.metaValueIsRosettaModelObject(oracle, typeUtil),
                ir.metaValueIsRosettaModelObject());
        c.same(at + "metaValueType", Optional.ofNullable(oracle.getMetaValueType()).map(IRPropertyReconciler::render),
                ir.metaValueType());
        c.same(at + "hasLocation", oracle.hasLocation(), ir.hasLocation());
        c.same(at + "attributeMetaTypes", metaTypeNames(oracle.getAttributeMetaTypes()), ir.attributeMetaTypes());
        c.same(at + "attributeMeta",
                oracle.getMeta() == null ? List.<String>of() : List.of(oracle.getMeta().name()), ir.attributeMeta());
        c.same(at + "synthetic", isSyntheticMeta(oracle), ir.synthetic());
        c.same(at + "inheritedChoiceOption", isInheritedChoiceOption(oracle), ir.inheritedChoiceOption());
        List<String> expectedDocReferences = sourceDocReferenceRows(oracle);
        c.same(at + "docReferences.size", expectedDocReferences.size(), ir.docReferences().size());
        c.same(at + "docReferences", expectedDocReferences, irDocReferenceRows(ir.docReferences()));
        c.same(at + "definition", sourceDefinition(oracle), ir.definition());
        // THE BYTE-BEARING ONE: the block ModelObjectGenerator writes above the getter ({@code :641-644}). The source
        // half READS it off the old generator's own property - the object the emitted POJO bytes come from; the IR
        // half REPRODUCES it from the IR facts alone. The two share no renderer, which is what makes it a fact.
        c.same(at + "javadoc", Optional.ofNullable(oracle.getJavadoc()), ir.javadoc());
    }

    // ------------------------------------------------------------------------------------- the source half's reads

    /**
     * The EFFECTIVE definition behind a property, read through the SAME join the doc-reference row uses: the property
     * names its DECLARING pojo ({@code JavaPojoProperty.getPojo()} - the ancestor's under Case 0, because the child's
     * property was never created; the specializing type's under a specialization, because
     * {@code RJavaPojoInterface.addProperty} passes {@code this}), and the declaration of that name inside it is the
     * attribute or the choice option whose {@code <"…">} the javadoc was built from
     * ({@code RJavaPojoInterface:287-288} / {@code :379-380}).
     *
     * <p>The synthetic {@code meta} is EXCLUDED by the same predicate the {@code synthetic} fact is read with: the old
     * generator adds it with a null javadoc and no declaration behind it ({@code :258}, {@code :321}), so a type that
     * happened to declare an attribute called {@code meta} of the meta-fields type must not lend it one here - and
     * that type would already be red on the {@code synthetic} fact itself.
     */
    private Optional<String> sourceDefinition(JavaPojoProperty property) {
        if (isSyntheticMeta(property) || !(property.getPojo() instanceof RJavaPojoInterface pojo)) {
            return Optional.empty();
        }
        if (pojo.isChoiceType()) {
            for (RChoiceOption option : pojo.getChoiceNode().options()) {
                if (option.typeCall() != null && property.getName().equals(option.typeCall().typeName())) {
                    return option.definition();
                }
            }
            return Optional.empty();
        }
        RDataType declaring = pojo.getAstNode();
        if (declaring == null) {
            return Optional.empty();
        }
        for (RAttribute attribute : declaring.attributes()) {
            if (attribute.name().equals(property.getName())) {
                return attribute.definition();
            }
        }
        return Optional.empty();
    }

    /**
     * The doc references in force on a property, walked HERE: the declaring pojo's own attribute of that name, then
     * {@code allDocReferences}' recursion over {@code RuleReferenceTraversal.parentAttributeOf}. The synthetic
     * {@code meta} property and every choice-option property have no backing attribute and so carry none.
     */
    private static List<String> sourceDocReferenceRows(JavaPojoProperty property) {
        List<RDocReference> references = List.of();
        if (property.getPojo() instanceof RJavaPojoInterface pojo && !pojo.isChoiceType()) {
            RDataType declaring = pojo.getAstNode();
            if (declaring != null) {
                for (RAttribute attribute : declaring.attributes()) {
                    if (attribute.name().equals(property.getName())) {
                        references = allDocReferences(attribute);
                        break;
                    }
                }
            }
        }
        List<String> rows = new ArrayList<>();
        for (RDocReference reference : references) {
            rows.add(sourceDocReferenceRow(reference));
        }
        return rows;
    }

    /** The reconciler's OWN copy of the private {@code RJavaPojoInterface.allDocReferences} ({@code :539-556}). */
    private static List<RDocReference> allDocReferences(RAttribute attribute) {
        List<RDocReference> own = attribute.docReferences();
        RAttribute parent = RuleReferenceTraversal.parentAttributeOf(attribute);
        if (parent == null) {
            return own;
        }
        List<RDocReference> parentAll = allDocReferences(parent);
        if (parentAll.isEmpty()) {
            return own;
        }
        List<RDocReference> all = new ArrayList<>(parentAll.size() + own.size());
        all.addAll(parentAll);
        all.addAll(own);
        return all;
    }

    /**
     * ONE doc reference, rendered so both halves can produce the same row independently: its kind, the body it cites,
     * the corpora it names, its segments and its provision. The reference's every other fact is already asserted per
     * attribute by the declaration reconcile; what this row carries is WHICH reference stands WHERE in the union.
     */
    private static String sourceDocReferenceRow(RDocReference reference) {
        var doc = reference.regulatoryDocRef();
        StringBuilder row = new StringBuilder(reference.isRegulatoryReference() ? "reg|" : "doc|");
        row.append(doc == null || doc.bodyRef() == null ? "" : doc.bodyRef()).append('|');
        if (doc != null) {
            row.append(String.join(",", doc.corpusRefs()));
        }
        row.append('|');
        if (doc != null) {
            List<String> segments = new ArrayList<>();
            for (var segment : doc.segmentRefs()) {
                segments.add(segment.segmentName() + "=" + (segment.value() == null ? "" : segment.value()));
            }
            row.append(String.join(",", segments));
        }
        return row.append('|').append(reference.provision().orElse("")).toString();
    }

    private static List<String> irDocReferenceRows(List<IRDocReference> references) {
        List<String> rows = new ArrayList<>();
        for (IRDocReference reference : references) {
            StringBuilder row = new StringBuilder(reference.regulatory() ? "reg|" : "doc|");
            row.append(reference.body().orElse("")).append('|');
            List<String> corpora = new ArrayList<>();
            for (IRDocReference.Corpus corpus : reference.corpora()) {
                corpora.add(corpus.reference());
            }
            row.append(String.join(",", corpora)).append('|');
            List<String> segments = new ArrayList<>();
            for (IRDocReference.Segment segment : reference.segments()) {
                segments.add(segment.name() + "=" + segment.value());
            }
            row.append(String.join(",", segments)).append('|').append(reference.provision().orElse(""));
            rows.add(row.toString());
        }
        return rows;
    }

    /** {@code ValidatorScan.isSyntheticMeta} ({@code :143-147}), mirrored over the public type-table constants. */
    private boolean isSyntheticMeta(JavaPojoProperty property) {
        JavaType type = property.getType();
        return "meta".equals(property.getName())
                && (typeUtil.META_FIELDS.equals(type) || typeUtil.META_AND_TEMPLATE_FIELDS.equals(type));
    }

    /** A property produced by a CHOICE's option loop: its declaring pojo IS a choice, and it is not the synthetic meta. */
    private boolean isInheritedChoiceOption(JavaPojoProperty property) {
        return property.getPojo() instanceof RJavaPojoInterface pojo && pojo.isChoiceType()
                && !isSyntheticMeta(property);
    }

    private String syntheticMetaType(List<JavaPojoProperty> properties) {
        for (JavaPojoProperty property : properties) {
            if (isSyntheticMeta(property)) {
                return property.getType().getSimpleName();
            }
        }
        return "<none>";
    }

    private static String irSyntheticMetaType(List<IRPropertyModel.IRProperty> properties) {
        for (IRPropertyModel.IRProperty property : properties) {
            if (property.synthetic()) {
                return IRPropertyModel.simpleNameOf(property.javaType());
            }
        }
        return "<none>";
    }

    /**
     * The ancestor rungs of a specialized property, NEAREST FIRST, read off the old generator's own chain (v3.3
     * seat 9, PR #645 commit 5). Each rung renders the facts a compat arm and the chain's import arm consume:
     * the ancestor's Java type in the old generator's own spelling, whether it is LIST shaped
     * ({@code typeUtil.isList}, the same predicate {@code ModelObjectGenerator:298} asks), whether it was REQUIRED,
     * and the BARE value type behind its meta wrap ({@code addTypeImports}, {@code :1775-1808}). An unspecialized
     * property answers the empty list, which is exactly what the IR half carries at depth 0.
     *
     * <p><b>THE ROW GREW BY SIX FIELDS at PR #645 commit 10</b> - the facts the compat MEMBERS themselves are
     * written from, and every one of them read through a seam on the old generator rather than mirrored here:
     * the rung's item kind ({@code ModelObjectGenerator.itemIsRosettaModelObject} / {@code .itemIsEnum}, the
     * seams the byte-writing sites delegate to), the kind of its bare value type
     * ({@code .metaValueIsRosettaModelObject}), its own getter-override verdict - the flag
     * {@code PojoCompatEmitter:175} and {@code :210} test as the walk's cursor climbs - and the two
     * compatibility names its members are called by ({@code JavaPojoProperty:120-133}). The FACT does not grow
     * with the row: {@code property.<name>.parentChain.types} is still ONE assertion per property, so the
     * reconciler's {@code factsAsserted} count is UNMOVED by this commit.
     */
    private List<String> sourceParentChainRows(JavaPojoProperty property) {
        List<String> rows = new ArrayList<>();
        for (JavaPojoProperty ancestor : ModelObjectGenerator.compatAncestorChain(property)) {
            rows.add(render(ancestor.getType()) + "|" + typeUtil.isList(ancestor.getType()) + "|"
                    + ancestor.isRequired() + "|"
                    + Optional.ofNullable(ancestor.getMetaValueType()).map(IRPropertyReconciler::render).orElse("")
                    + "|" + ModelObjectGenerator.itemIsRosettaModelObject(ancestor, typeUtil)
                    + "|" + ModelObjectGenerator.itemIsEnum(ancestor, typeUtil)
                    + "|" + ModelObjectGenerator.metaValueIsRosettaModelObject(ancestor, typeUtil)
                            .map(String::valueOf).orElse("")
                    + "|" + ancestor.getterOverridesParentGetter()
                    + "|" + ancestor.getGetterCompatibilityName()
                    + "|" + ancestor.getSetterCompatibilityName());
        }
        return rows;
    }

    /** The IR half's rungs in the SAME row spelling - {@link IRPropertyModel.IRParentLink#row()} states it once. */
    private static List<String> irParentChainRows(List<IRPropertyModel.IRParentLink> chain) {
        List<String> rows = new ArrayList<>();
        for (IRPropertyModel.IRParentLink link : chain) {
            rows.add(link.row());
        }
        return rows;
    }

    private static int chainDepth(JavaPojoProperty property) {
        int depth = 0;
        for (JavaPojoProperty parent = property.getParentProperty(); parent != null;
                parent = parent.getParentProperty()) {
            depth++;
        }
        return depth;
    }

    private static List<String> metaTypeNames(List<AttributeMetaType> metaTypes) {
        List<String> names = new ArrayList<>();
        for (AttributeMetaType metaType : metaTypes) {
            names.add(metaType.name());
        }
        return names;
    }

    /** The OLD GENERATOR'S OWN spelling of a Java type - the one rendering both halves produce. */
    private static String render(JavaType type) {
        return type.toString();
    }

    private static List<JavaPojoProperty> copyOf(Collection<JavaPojoProperty> properties) {
        return new ArrayList<>(properties);
    }

    private static String joinNames(List<JavaPojoProperty> properties) {
        List<String> names = new ArrayList<>();
        for (JavaPojoProperty property : properties) {
            names.add(property.getName());
        }
        return String.join(",", names);
    }

    private static String joinIrNames(List<IRPropertyModel.IRProperty> properties) {
        List<String> names = new ArrayList<>();
        for (IRPropertyModel.IRProperty property : properties) {
            names.add(property.name());
        }
        return String.join(",", names);
    }

    private static String qualified(String namespace, String simpleName) {
        return namespace == null || namespace.isEmpty() ? simpleName : namespace + "." + simpleName;
    }

    /**
     * One declaration's assertions - the {@link IRDeclarationReconciler} idiom, copied: every {@code same} counts a
     * fact, a mismatch names the fact, and {@link #close()} books both. The declaration itself is booked by
     * {@link #attempt()}, before the pojo is built - never here.
     */
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
