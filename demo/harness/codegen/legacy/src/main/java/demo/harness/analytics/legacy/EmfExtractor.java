package demo.harness.analytics.legacy;

import com.regnosys.rosetta.rosetta.RosettaEnumeration;
import com.regnosys.rosetta.rosetta.RosettaModel;
import com.regnosys.rosetta.rosetta.RosettaRootElement;
import com.regnosys.rosetta.rosetta.RosettaRule;
import com.regnosys.rosetta.rosetta.RosettaType;
import com.regnosys.rosetta.rosetta.RosettaTypeAlias;
import com.regnosys.rosetta.rosetta.TypeCall;
import com.regnosys.rosetta.rosetta.simple.Annotated;
import com.regnosys.rosetta.rosetta.simple.Annotation;
import com.regnosys.rosetta.rosetta.simple.AnnotationRef;
import com.regnosys.rosetta.rosetta.simple.Attribute;
import com.regnosys.rosetta.rosetta.simple.Choice;
import com.regnosys.rosetta.rosetta.simple.Data;
import com.regnosys.rosetta.rosetta.simple.Function;

import demo.harness.legacy.Decl;
import demo.harness.legacy.Queries;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code legacy-emf} lane's extractor: upstream EMF model -&gt; {@link Decl} list.
 *
 * <p>Everything downstream is {@link Queries}, shared verbatim with the fork lane. Only the
 * walk below is EMF-specific, so a disagreement between the lanes is always located here.
 *
 * <h2>EMF facts this walk depends on (all verified against the 9.83.0 jar)</h2>
 * <ul>
 *   <li>{@code RosettaModel.getName()} IS the namespace. There is no {@code getNamespace()}.</li>
 *   <li>{@code RosettaTyped.getTypeCall().getType().getName()} is the idiom; there is no
 *       {@code getType()} on the typed element itself.</li>
 *   <li>{@code Choice extends Data} and {@code FunctionDispatch extends Function}, so subtype
 *       tests must come first. {@code Choice} is NOT counted as a data type here -- see
 *       {@link Decl.Kind}.</li>
 *   <li>{@code RosettaEnumeration} uses {@code getParent()}, not {@code getSuperType()} -- not
 *       needed here, since Q4 walks data types only.</li>
 *   <li>{@code RosettaReport} has NO {@code getName()}; it is unnamed and therefore skipped,
 *       exactly as the fork lane skips it.</li>
 *   <li>A {@code [metadata scheme]} annotation is one {@code AnnotationRef} whose
 *       {@code getAnnotation().getName()} is {@code "metadata"}; the qualifier is
 *       {@code getAttribute().getName()}. Q2 matches the annotation name only.</li>
 * </ul>
 *
 * <h2>Two things this walk deliberately never calls</h2>
 * {@code Choice.getConditions()} MUTATES the resource (it lazily inserts a synthetic
 * one-of condition), and {@code ChoiceOption.getName()} derives from the node model and can
 * NPE. Choices are recorded by name and cardinality-free, and neither method is touched.
 *
 * <h2>Proxies</h2>
 * {@code AnnotationRef.getAnnotation()} is a cross-resource reference into
 * {@code annotations.rosetta} and is a PROXY if that builtin was never loaded, so every
 * reference read here is {@code eIsProxy()}-guarded. Where a proxy would cost an answer -- an
 * unresolved super-type or attribute type -- the source text is recovered from the node model
 * instead, and the number of times that happened is reported rather than hidden.
 */
public final class EmfExtractor {

    private EmfExtractor() {
    }

    private static final String METADATA_ANNOTATION = "metadata";

    /** Counts of the recoveries that fired, so a lane disagreement is never silent. */
    public static final class Stats {
        public int unresolvedSuperTypes;
        public int unresolvedAttributeTypes;
        public int skippedRootElements;
    }

    /** Extracts every named declaration of every corpus resource. */
    public static List<Decl> extract(List<Resource> resources, Stats stats) {
        List<Decl> declarations = new ArrayList<>();
        for (Resource resource : resources) {
            String file = resource.getURI() == null ? "" : resource.getURI().lastSegment();
            if (Queries.isBuiltinFile(file)) {
                continue;
            }
            if (resource.getContents().isEmpty()
                    || !(resource.getContents().get(0) instanceof RosettaModel model)) {
                continue;
            }
            // RosettaModel.getName() is the NAMESPACE.
            String namespace = model.getName();
            // getElements() is the top-level declaration list -- the exact counterpart of the
            // fork's RModel.rootElements(). Using it instead of eAllContents() also sidesteps
            // the duplicate-visit trap, where a Choice hands back each ChoiceOption twice.
            for (RosettaRootElement element : model.getElements()) {
                Decl declaration = toDecl(element, namespace, file, stats);
                if (declaration != null) {
                    declarations.add(declaration);
                } else {
                    stats.skippedRootElements++;
                }
            }
        }
        return declarations;
    }

    private static Decl toDecl(RosettaRootElement element, String namespace, String file,
                               Stats stats) {
        // Choice BEFORE Data: Choice extends Data.
        if (element instanceof Choice choice) {
            return new Decl(choice.getName(), Decl.Kind.CHOICE, namespace, file, null,
                    hasMetadata(choice), List.of());
        }
        if (element instanceof Data data) {
            List<String> attributeTypes = new ArrayList<>();
            boolean meta = hasMetadata(data);
            for (Attribute attribute : data.getAttributes()) {
                attributeTypes.add(attributeTypeSimpleName(attribute, stats));
                if (!meta && hasMetadata(attribute)) {
                    meta = true;
                }
            }
            return new Decl(data.getName(), Decl.Kind.TYPE, namespace, file,
                    superTypeSimpleName(data, stats), meta, List.copyOf(attributeTypes));
        }
        if (element instanceof RosettaEnumeration enumeration) {
            return new Decl(enumeration.getName(), Decl.Kind.ENUM, namespace, file, null,
                    hasMetadata(enumeration), List.of());
        }
        if (element instanceof Function function) {
            // FunctionDispatch extends Function; both are `func` declarations and both count.
            return new Decl(function.getName(), Decl.Kind.FUNCTION, namespace, file, null,
                    hasMetadata(function), List.of());
        }
        if (element instanceof RosettaRule rule) {
            Decl.Kind kind = rule.isEligibility()
                    ? Decl.Kind.ELIGIBILITY_RULE : Decl.Kind.REPORTING_RULE;
            return new Decl(rule.getName(), kind, namespace, file, null, false, List.of());
        }
        if (element instanceof RosettaTypeAlias alias) {
            return new Decl(alias.getName(), Decl.Kind.OTHER, namespace, file, null, false,
                    List.of());
        }
        // Unnamed or uncounted: RosettaReport (no getName()), bodies, corpora, rule sources.
        return null;
    }

    /** Whether the element or -- for a type -- any attribute carries a {@code [metadata ...]}. */
    private static boolean hasMetadata(Object candidate) {
        if (!(candidate instanceof Annotated annotated)) {
            return false;
        }
        for (AnnotationRef ref : annotated.getAnnotations()) {
            Annotation annotation = ref.getAnnotation();
            if (annotation == null || annotation.eIsProxy()) {
                continue;
            }
            if (METADATA_ANNOTATION.equals(annotation.getName())) {
                return true;
            }
        }
        return false;
    }

    private static String superTypeSimpleName(Data data, Stats stats) {
        Data superType = data.getSuperType();
        if (superType == null) {
            return null;
        }
        if (!superType.eIsProxy()) {
            String name = superType.getName();
            if (name != null && !name.isEmpty()) {
                return Decl.simpleName(name);
            }
        }
        stats.unresolvedSuperTypes++;
        String text = sourceTextForFeature(data, "superType");
        return text == null ? null : Decl.simpleName(text);
    }

    private static String attributeTypeSimpleName(Attribute attribute, Stats stats) {
        TypeCall typeCall = attribute.getTypeCall();
        if (typeCall == null) {
            return "";
        }
        RosettaType type = typeCall.getType();
        if (type != null && !type.eIsProxy()) {
            String name = type.getName();
            if (name != null && !name.isEmpty()) {
                // RosettaType.getName() is already the SIMPLE name -- a namespace lives on the
                // model, not on the type -- but reducing anyway keeps the two lanes' rule
                // literally identical.
                return Decl.simpleName(name);
            }
        }
        stats.unresolvedAttributeTypes++;
        String text = sourceTextForFeature(attribute, "typeCall");
        if (text == null) {
            return "";
        }
        // A parametrised type call carries arguments, e.g. `number(digits: 30)`. Only the
        // type reference itself is wanted.
        int paren = text.indexOf('(');
        String head = (paren >= 0 ? text.substring(0, paren) : text).trim();
        String simple = Decl.simpleName(head);
        return simple == null ? "" : simple;
    }

    /**
     * The source text of one structural feature of {@code owner}, from the node model.
     *
     * <p>The feature is looked up BY NAME on {@code owner.eClass()} rather than through
     * {@code SimplePackage.Literals}, so this compiles against any generated metamodel that
     * still calls the reference the same thing -- and a rename degrades to "no recovery"
     * rather than to a broken build.
     */
    private static String sourceTextForFeature(EObject owner, String featureName) {
        EReference feature = null;
        for (EReference candidate : owner.eClass().getEAllReferences()) {
            if (featureName.equals(candidate.getName())) {
                feature = candidate;
                break;
            }
        }
        if (feature == null) {
            return null;
        }
        List<INode> nodes = NodeModelUtils.findNodesForFeature(owner, feature);
        if (nodes.isEmpty()) {
            return null;
        }
        String text = NodeModelUtils.getTokenText(nodes.get(0));
        return text == null ? null : text.trim();
    }
}
