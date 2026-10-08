package demo.harness.analytics;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.enums.RuleKind;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code plus-jvm} lane's extractor: fork {@link RModel} tree -&gt; {@link Decl} list.
 *
 * <p>Everything downstream of this class is {@link Queries}, which is shared verbatim with the
 * legacy lane. Only the walk below is fork-specific.
 *
 * <p>Unnamed root elements (a {@code report}, for instance, which upstream does not give a name
 * either) are skipped rather than given a synthetic name: the section-5 index is keyed by
 * DECLARATION NAME, and inventing one would put an entry in it that no other lane can produce.
 */
public final class ForkExtractor {

    private ForkExtractor() {
    }

    /** The annotation name that marks metadata: {@code [metadata key]}, {@code [metadata scheme]}, ... */
    private static final String METADATA_ANNOTATION = "metadata";

    /**
     * Extracts every named declaration of every NON-BUILTIN model in the workspace.
     *
     * @param models the workspace's models, builtins included; builtins are filtered here
     */
    public static List<Decl> extract(List<RModel> models) {
        List<Decl> declarations = new ArrayList<>();
        for (RModel model : models) {
            String file = fileNameOf(model);
            if (Queries.isBuiltinFile(file)) {
                continue;
            }
            String namespace = model.namespace();
            for (RRootElement element : model.rootElements()) {
                Decl declaration = toDecl(element, namespace, file);
                if (declaration != null) {
                    declarations.add(declaration);
                }
            }
        }
        return declarations;
    }

    private static Decl toDecl(RRootElement element, String namespace, String file) {
        if (element instanceof RDataType type) {
            List<String> attributeTypes = new ArrayList<>();
            boolean meta = hasMetadata(type.annotationRefs());
            for (RAttribute attribute : type.attributes()) {
                attributeTypes.add(attributeTypeSimpleName(attribute));
                if (!meta && hasMetadata(attribute.annotationRefs())) {
                    meta = true;
                }
            }
            // superTypeName() is the SOURCE text and may be qualified; Q4 and the index both
            // work on simple names, so it is reduced here and not in the query.
            String superName = Decl.simpleName(type.superTypeName().orElse(null));
            return new Decl(type.name(), Decl.Kind.TYPE, namespace, file, superName, meta,
                    List.copyOf(attributeTypes));
        }
        if (element instanceof RChoice choice) {
            // A choice is indexed so a `type X extends SomeChoice` edge can be seen to end at
            // a non-type, but it is never counted as a data type. See Decl.Kind.
            return new Decl(choice.name(), Decl.Kind.CHOICE, namespace, file, null,
                    hasMetadata(choice.annotationRefs()), List.of());
        }
        if (element instanceof REnumeration enumeration) {
            return new Decl(enumeration.name(), Decl.Kind.ENUM, namespace, file, null,
                    hasMetadata(enumeration.annotationRefs()), List.of());
        }
        if (element instanceof RFunction function) {
            // Every RFunction reachable from rootElements() is a parsed `func`: the REPORT and
            // RULE origins are set only by the RFunction.fromReport/fromRule factories, which
            // mint synthetic functions that never enter a model's root-element list.
            return new Decl(function.name(), Decl.Kind.FUNCTION, namespace, file, null,
                    hasMetadata(function.annotationRefs()), List.of());
        }
        if (element instanceof RRule rule) {
            Decl.Kind kind = rule.kind() == RuleKind.ELIGIBILITY
                    ? Decl.Kind.ELIGIBILITY_RULE : Decl.Kind.REPORTING_RULE;
            return new Decl(rule.name(), kind, namespace, file, null, false, List.of());
        }
        if (element instanceof RTypeAlias alias) {
            return new Decl(alias.name(), Decl.Kind.OTHER, namespace, file, null, false,
                    List.of());
        }
        // Unnamed or uncounted: reports, bodies, corpora, synonym and rule sources.
        return null;
    }

    private static boolean hasMetadata(List<RAnnotationRef> annotationRefs) {
        if (annotationRefs == null) {
            return false;
        }
        for (RAnnotationRef ref : annotationRefs) {
            // annotationName() is the name as written -- `[metadata key]` gives
            // annotationName()="metadata", qualifierName()="key" (AstBuilder:1888/1892). The
            // qualifier is deliberately not restricted: Q2 asks for ANY [metadata ...].
            if (METADATA_ANNOTATION.equals(ref.annotationName())) {
                return true;
            }
        }
        return false;
    }

    private static String attributeTypeSimpleName(RAttribute attribute) {
        if (attribute == null || attribute.typeCall() == null) {
            return "";
        }
        String typeName = attribute.typeCall().typeName();
        String simple = Decl.simpleName(typeName);
        return simple == null ? "" : simple;
    }

    /**
     * The model's bare source file name. {@code AstBuilder.buildFromFile} already stores the
     * bare name (AstBuilder:304), but a path is tolerated so the rule is the same either way.
     */
    static String fileNameOf(RModel model) {
        String path = model.sourceRange() == null ? null : model.sourceRange().file();
        if (path == null) {
            return "";
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
