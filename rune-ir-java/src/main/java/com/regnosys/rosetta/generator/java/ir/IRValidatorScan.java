package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRField;
import com.rosetta.util.DottedPath;

/**
 * THE VALIDATOR SCAN, FROM THE IR ALONE (v3.3 seat 9, PR #645 commit 12) - the IR counterpart of
 * {@code ValidatorScan} ({@code rune-java-generator/.../object/validators/ValidatorScan.java}), which the three
 * validator generators share and which reads the AST through {@code RJavaPojoInterface}.
 *
 * <p>Every fact below is an {@link IRDerivedFacts} or {@link IRPropertyModel} read, RECONCILED per element against
 * the old generator's own scan since PR #644 - the member list and the synthetic-{@code meta} skip
 * ({@code onlyExists.members} / {@code .syntheticMetaSkipped}), the bounds and the fully-unbounded skip
 * ({@code cardinality.<p>.bounds} / {@code .skipped}), the declared field behind a property
 * ({@code typeFormat.<p>.declared}), the {@code java.lang} collision ({@code collision.*}) - or an
 * {@link IRDataTypeEmitter} rendering law over the reconciled property surface (the cast type and the getter name,
 * which the POJO member already writes byte-identically on the whole corpus). NOTHING here reads an AST node, a
 * {@code GeneratorModel} or a workspace, and nothing calls a legacy generator's model-reading path.
 *
 * <p>The three emitters are pure functions of a node, so this class is too: it holds no state and every method is
 * static.
 */
final class IRValidatorScan {

    private IRValidatorScan() {
    }

    /**
     * ONE scanned check-entry candidate - {@code ValidatorScan.ScannedAttribute}'s IR twin, plus the DECLARED field
     * behind the property (which the type-format member needs and the other two do not).
     *
     * @param prop           the POJO property, in POJO order
     * @param name           the check key - the property's own name
     * @param castType       the cast text at a check site, {@code IRDataTypeEmitter.interfaceGetterType}'s own
     * @param getterExpr     {@code o.getX()}
     * @param min            the lower bound
     * @param max            the upper bound, {@code 0} the "no upper bound" sentinel
     * @param fullyUnbounded {@code unbounded && lower == 0} - the cardinality member's skip
     * @param declared       the DECLARED field, or {@code null} when the property has none
     */
    record Scanned(IRPropertyModel.IRProperty prop, String name, String castType, String getterExpr,
                   int min, int max, boolean fullyUnbounded, IRField declared) {

        /** The getter CALL alone ({@code getX()}) - the wing applies it to its own instance identifier. */
        String getterCall() {
            return IRDataTypeEmitter.getterName(prop) + "()";
        }
    }

    /** The whole IR-side view one validator emitter needs of a node, computed once per emit. */
    record Surface(IRPropertyModel properties, List<IRTypeNode> chain,
                   Map<String, IRDerivedFacts.Owned> effective, List<Scanned> scanned) {
    }

    /**
     * The element's effective properties in POJO order, the synthetic {@code meta} skipped - exactly what
     * {@code ValidatorScan.scan} leaves ({@code :108-144}), from the IR.
     */
    static Surface scan(IRTypeNode node, IRTypeIndex index, IRDerivedFacts facts) {
        Objects.requireNonNull(node, "node");
        IRPropertyModel properties = IRPropertyModel.of(node, index);
        List<IRPropertyModel.IRProperty> members = facts.members(properties);
        List<IRTypeNode> chain = facts.chain(node);
        Map<String, IRDerivedFacts.Owned> effective = IRDerivedFacts.effectiveAttributes(chain);
        Map<String, IRField> declaredByProperty = IRDerivedFacts.declaredFieldsByProperty(node, chain);
        List<Scanned> scanned = new ArrayList<>();
        for (IRPropertyModel.IRProperty member : members) {
            int[] bounds = IRDerivedFacts.bounds(member, effective);
            scanned.add(new Scanned(member, member.name(),
                    IRDataTypeEmitter.interfaceGetterType(member),
                    "o." + IRDataTypeEmitter.getterName(member) + "()",
                    bounds[0], bounds[1], bounds[2] == 1, declaredByProperty.get(member.name())));
        }
        return new Surface(properties, chain, effective, scanned);
    }

    // ----------------------------------------------------------------------------------- the class and its package

    /** The POJO's canonical name - the escaped declaring namespace and the type's simple name. */
    static String dataClassFqn(IRTypeNode node) {
        return IRDataTypeEmitter.packageOf(node) + "." + IRTypeUnit.simpleName(node);
    }

    /**
     * The member's own package - the declaring namespace with the member's sub-package segments below it, escaped
     * as a whole, which is what {@code JavaTypeTranslator} does before it takes the canonical name
     * ({@code :255-289}). The SAME law {@link IRTypeUnit#outputKey} takes, so the class and its file can never be
     * spelled apart.
     */
    static String validatorPackage(IRTypeNode node, IRTypeUnit.Member member) {
        DottedPath namespace = DottedPath.splitOnDots(IRTypeUnit.namespaceOf(node));
        for (String segment : member.subPackage()) {
            namespace = namespace.child(segment);
        }
        return JavaPackageName.escape(namespace).getName().withDots();
    }

    /** The validator class's simple name - the type's own with the member's suffix. */
    static String validatorClassName(IRTypeNode node, IRTypeUnit.Member member) {
        return IRTypeUnit.simpleName(node) + member.suffix();
    }

    /**
     * The data class as written at every Java TYPE position: the #306 law on the subject type itself - a simple name
     * that collides with an implicitly-imported {@code java.lang} type is never imported and every TYPE position
     * fully qualifies (golden {@code ErrorValidator}); otherwise a D50 first-claim sentinel.
     */
    static String dataClassJavaType(String dataClassFqn, boolean collides) {
        return collides ? dataClassFqn : ImportCollisionResolver.typeRefOrBare(dataClassFqn);
    }

    // --------------------------------------------------------------------------------------------- the imports

    /**
     * {@code ValidatorScan.addCastImports} ({@code :204-215}) over the IR property: the ITEM type, unless it is a
     * {@code java.lang} class or a class whose simple name collides with one (such a type is written fully
     * qualified, so it is never imported), plus {@code java.util.List} for a list cast. Unlike the POJO's import
     * walk the meta VALUE type is NOT imported - validators never reference the unwrapped value type.
     */
    static void addCastImports(ImportCollector imports, IRPropertyModel.IRProperty prop) {
        String item = IRDataTypeEmitter.itemTypeOf(prop);
        if (isClassName(item) && !item.startsWith("java.lang.")
                && !IRJavaLangCollision.collides(simpleOf(item))) {
            imports.addImport(item);
        }
        if (prop.multi()) {
            imports.addImport("java.util.List");
        }
    }

    /**
     * A dotted class name - {@code IRDataTypeEmitter.isClassName}'s law ({@code :2487-2489}), copied here because
     * that one is private to the POJO emitter and this scan is the validators'. A parameterized or bare spelling is
     * not a class name.
     */
    static boolean isClassName(String rendered) {
        return rendered.indexOf('.') >= 0 && rendered.indexOf('<') < 0 && rendered.indexOf('[') < 0;
    }

    static String simpleOf(String canonical) {
        int dot = canonical.lastIndexOf('.');
        return dot >= 0 ? canonical.substring(dot + 1) : canonical;
    }

    // ------------------------------------------------------------------------------------------- the refusals

    /**
     * A NAMED refusal of a validator member - the IR counterpart of {@code SilentDegradation.Site}. It names the
     * SITE, so the D11 UNIT SHADOW line can assert that every refusal of a READY member is one of the sites the old
     * generator refuses at, rather than merely counting refusals.
     */
    static final class NamedRefusal extends GenerationException {
        private static final long serialVersionUID = 1L;

        /** The refusal sites, spelled as {@code SilentDegradation.Site} spells them. */
        enum Site {
            /** A parameterised {@code typeAlias} carrying a condition - the post-9.83 sub-wing. */
            TYPE_ALIAS_CONDITION_DROPPED,
            /** A condition class whose simple name is already claimed by a type the file writes after its fields. */
            BOILERPLATE_NAME_COLLISION,
            /** The meta-wrap law and the meta-value fact disagree about one property (the review's Q10). */
            META_VALUE_TYPE_ABSENT
        }

        private final Site site;

        NamedRefusal(Site site, String why) {
            super("IR validator emitter: " + site + " - " + why, null, null);
            this.site = site;
        }

        Site site() {
            return site;
        }
    }
}
