package com.regnosys.rosetta.generator.java.object.validators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;
import com.regnosys.rosetta.generator.java.types.JavaPojoPropertyOperationType;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;
import com.regnosys.rosetta.generator.java.SilentDegradation;

/**
 * Shared attribute scan for the validator family generators (cardinality +
 * only-exists; the type-format family reuses it at its own wave).
 *
 * <p>Walks a {@code Data} or {@code choice} type's EFFECTIVE properties via the
 * byte-proven POJO property machinery ({@link RJavaPojoInterface#getAllProperties()})
 * so validator casts and getter names equal the POJO getter surface BY CONSTRUCTION —
 * meta wrappers ({@code FieldWithMetaX}/{@code ReferenceWithMetaX} stay wrapped in both
 * validator families), the list wildcard ({@code List<? extends X>} for model types,
 * {@code List<X>} for basics), specialization compatibility getter names, inherited
 * choice options (a data type extending a choice validates the choice's options —
 * golden-verified {@code SpecificAssetValidator}), and the java.lang-collision FQN law
 * at cast sites (the #306 facet). Each property joins back to its declaring
 * {@link RAttribute} (by name, over the data-supertype chain) for cardinality bounds.
 *
 * <p>Excluded: the synthetic {@code meta} property (types/choices with
 * {@code [metadata key]}) — upstream validators walk the MODEL's attributes, which
 * never include it. Identified precisely as: no backing {@link RAttribute} AND named
 * {@code meta} with the {@code MetaFields}/{@code MetaAndTemplateFields} type. A
 * property with no backing RAttribute otherwise is an inherited choice OPTION, whose
 * bounds are fixed {@code 0..1} by choice semantics (golden-verified {@code 0, 1}).
 *
 * <p><b>RECONCILE SEAM (PR #644, the property gate).</b> This class and four of its members are {@code public} so the
 * IR route's derived-facts reconciler ({@code rune-ir-java},
 * {@code com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconciler}) can read the OLD GENERATOR'S OWN scan as
 * the SOURCE half of the cardinality / only-exists / collision fact families - LAW 69 asks the source half to be the
 * generator's own decision, not a second opinion about it. Visibility ONLY: no body, no signature and no call site
 * moved, and no emission path calls the seam. The scan stays family-owned; the widening serves the gate that precedes
 * the data-type emitter, and the emitter itself will read the IR.
 */
public final class ValidatorScan {

    private ValidatorScan() {
    }

    /**
     * One scanned check-entry candidate: the backing POJO property (for cast-site
     * import collection), the check key ({@code name}), the cast type text, the getter
     * call expression, and the cardinality bounds ({@code max == 0} is the "no upper
     * bound" sentinel, mirroring upstream {@code max.orElse(0)}).
     */
    public record ScannedAttribute(JavaPojoProperty prop, String name, String castType,
                            String getterExpr, int min, int max, boolean isFullyUnbounded) {
        /**
         * The getter CALL alone ({@code getX()}), for a consumer that applies it to an instance
         * identifier of its own naming (the type-format wing's {@code runConditions}, whose
         * instance parameter is a scope identifier — v3.2 seat 3, round-1 cq MF-2); the same
         * typed operation-name read {@link #getterExpr} is built from.
         */
        String getterCall() {
            return prop.getOperationName(JavaPojoPropertyOperationType.GET) + "()";
        }
    }

    /** Whether the validator families process this root element (Data or choice). */
    static boolean isValidatedType(RRootElement element) {
        return element instanceof RDataType || element instanceof RChoice;
    }

    /** The element's declared name ({@code RRootElement} itself exposes none). */
    static String elementName(RRootElement element) {
        if (element instanceof RDataType dataType) {
            return dataType.name();
        }
        return ((RChoice) element).name();
    }

    /**
     * The POJO interface for a validated element — the single source for the
     * validator's data-class name/package and its effective property surface.
     */
    public static RJavaPojoInterface toPojo(RRootElement element, GeneratorModel generatorModel,
                                     JavaTypeTranslator typeTranslator, JavaTypeUtil typeUtil) {
        if (element instanceof RDataType dataType) {
            return new RJavaPojoInterface(dataType, generatorModel, typeTranslator, typeUtil);
        }
        return new RJavaPojoInterface((RChoice) element, generatorModel, typeTranslator, typeUtil);
    }

    /**
     * Scan the element's effective properties in POJO order (root-supertype-first,
     * name-deduped — the same order the golden validators list their checks in).
     */
    public static List<ScannedAttribute> scan(RRootElement element, RJavaPojoInterface pojo,
                                       GeneratorModel generatorModel, JavaTypeUtil typeUtil) {
        Map<String, RAttribute> attrByName = new LinkedHashMap<>();
        if (element instanceof RDataType dataType) {
            for (RAttribute attr : generatorModel.allAttributes(dataType)) {
                attrByName.put(attr.name(), attr);
            }
        }
        List<ScannedAttribute> result = new ArrayList<>();
        for (JavaPojoProperty prop : pojo.getAllProperties()) {
            RAttribute attr = attrByName.get(prop.getName());
            if (attr == null && isSyntheticMeta(prop, typeUtil)) {
                continue;
            }
            int min;
            int max;
            boolean fullyUnbounded;
            if (attr != null) {
                min = attr.cardinality().map(c -> c.inf().intValue()).orElse(0);
                max = attr.cardinality()
                        .map(c -> c.isUnbounded() ? 0 : c.sup().intValue())
                        .orElse(0);
                fullyUnbounded = attr.cardinality()
                        .map(c -> c.isUnbounded() && c.inf().intValue() == 0)
                        .orElse(false);
            } else {
                // Inherited choice option: single-optional by choice semantics.
                min = 0;
                max = 1;
                fullyUnbounded = false;
            }
            String getterExpr = "o." + prop.getOperationName(JavaPojoPropertyOperationType.GET) + "()";
            result.add(new ScannedAttribute(prop, prop.getName(), castType(prop, typeUtil),
                    getterExpr, min, max, fullyUnbounded));
        }
        return result;
    }

    /**
     * The synthetic {@code meta} property added for {@code [metadata key]} — has no
     * backing model attribute and carries exactly the MetaFields /
     * MetaAndTemplateFields type.
     */
    private static boolean isSyntheticMeta(JavaPojoProperty prop, JavaTypeUtil typeUtil) {
        return "meta".equals(prop.getName())
                && (typeUtil.META_FIELDS.equals(prop.getType())
                        || typeUtil.META_AND_TEMPLATE_FIELDS.equals(prop.getType()));
    }

    /**
     * The cast type text at a validator check site — the POJO getter type verbatim:
     * single {@code X} / meta-wrapped {@code FieldWithMetaX}; list
     * {@code List<? extends X>} for model types, {@code List<X>} for basics (the
     * {@code interfaceGetterType} law in {@code ModelObjectGenerator}).
     *
     * <p><b>RECONCILE SEAM (v3.3 seat 9, PR #645 commit 12).</b> {@code public} so the IR route's derived-facts
     * reconciler can hold THIS scan's own cast text against the IR emitter's own
     * ({@code IRDataTypeEmitter.interfaceGetterType}) - the {@code cardinality.<p>.castType} family. Visibility
     * ONLY: the body, the signature and every call site are unmoved, and no emission path calls the seam.
     */
    public static String castType(JavaPojoProperty prop, JavaTypeUtil typeUtil) {
        if (typeUtil.isList(prop.getType())) {
            String item = itemTypeName(prop, typeUtil);
            // v3.2 seat 11 (D50): the list token is a first-claim sentinel like the item type
            String list = ImportCollisionResolver.typeRefOrBare("java.util.List");
            return typeUtil.isRosettaModelObject(prop.getType())
                    ? list + "<? extends " + item + ">"
                    : list + "<" + item + ">";
        }
        return itemTypeName(prop, typeUtil);
    }

    /**
     * The property item type at a cast site. facet javaLangAttrFqn (PR #306): a MODEL
     * type whose simple name collides with an implicitly-imported {@code java.lang}
     * type is FQN-inlined (golden refuses to import it); a {@code java.lang} type
     * itself stays bare. v3.2 seat 11 (D50): every other class is a first-claim sentinel
     * ({@link ImportCollisionResolver#typeRefOrBare}) — bare or canonical by the file's text
     * order once the generator resolves the class text.
     */
    static String itemTypeName(JavaPojoProperty prop, JavaTypeUtil typeUtil) {
        JavaType itemType = typeUtil.getItemType(prop.getType());
        String simple = itemType.getSimpleName();
        if (itemType instanceof JavaClass<?> javaClass) {
            String fqn = javaClass.getCanonicalName().withDots();
            if (!fqn.startsWith("java.lang.") && collidesWithJavaLang(simple)) {
                return fqn;
            }
            if (ImportCollisionResolver.simpleIsLastSegment(fqn, simple)) {
                return ImportCollisionResolver.typeRefOrBare(fqn);
            }
        }
        return simple;
    }

    /**
     * Collect the imports a check entry's cast needs: the item type (skipped for
     * {@code java.lang} and for collision-FQN-inlined types) plus {@code java.util.List}
     * for list casts. Unlike the POJO's import walk, the meta VALUE type is NOT
     * imported — validators never reference the unwrapped value type.
     */
    static void addCastImports(ImportCollector imports, JavaPojoProperty prop, JavaTypeUtil typeUtil) {
        JavaType itemType = typeUtil.getItemType(prop.getType());
        if (itemType instanceof JavaClass<?> javaClass) {
            String fqn = javaClass.getCanonicalName().withDots();
            if (!fqn.startsWith("java.lang.") && !collidesWithJavaLang(javaClass.getSimpleName())) {
                imports.addImport(fqn);
            }
        }
        if (typeUtil.isList(prop.getType())) {
            imports.addImport("java.util.List");
        }
    }

    // Mirrors ModelObjectGenerator.collidesWithJavaLang (package-private there; the
    // validators package cannot reach it). Cached Class.forName lookup, no init; ANY
    // lookup failure = "no collision" (the green-safe fail-direction — bare simple
    // name is the corpus default).
    private static final Map<String, Boolean> JAVA_LANG_COLLISION = new ConcurrentHashMap<>();

    public static boolean collidesWithJavaLang(String simpleName) {
        return JAVA_LANG_COLLISION.computeIfAbsent(simpleName, n -> {
            try {
                Class.forName("java.lang." + n, false, ValidatorScan.class.getClassLoader());
                return Boolean.TRUE;
            } catch (ClassNotFoundException | LinkageError | RuntimeException e) {
                if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                // A refusal is not a probe failure: these clauses exist to answer
                // "does this JDK name resolve?" with FALSE, and answering that for a
                // refusal would convert a deliberate decline into a quiet wrong answer.
                // No refusal can arise inside Class.forName today; the guard keeps the
                // C0 contract true by construction if that ever changes (Copilot R2).
                return Boolean.FALSE;
            }
        });
    }
}
