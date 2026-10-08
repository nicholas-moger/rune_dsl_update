package com.regnosys.rosetta.generator.java.function;

import java.util.Set;

import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.functions.RFunction;

/**
 * Phase X T3 — port of upstream {@code LabelProviderGeneratorUtil}
 * ({@code rune-dsl-9.83.0-line upstream/rune-lang/src/main/java/com/regnosys/rosetta/generator/java/function/LabelProviderGeneratorUtil.java};
 * 15 LOC, already Java upstream — verbatim port with two surface adaptations).
 *
 * <p><b>Fork divergences vs upstream:</b>
 * <ol>
 *   <li>Upstream's helper accepts a {@code Function} (EMF AST) and delegates to
 *       {@code RosettaFunctionExtensions.getTransformAnnotations(Function)} —
 *       which filters {@code function.getAnnotations()} for transform-namespace
 *       annotations ({@code ingest} / {@code enrich} / {@code projection}, all
 *       declared in the {@code com.rosetta.model} namespace per upstream
 *       {@code RosettaFunctionExtensions.java:141-156}). The fork has no
 *       {@code RosettaFunctionExtensions} (its Xtext / EMF roots) and no
 *       {@code Function} EMF type — the equivalent AST node is {@link RFunction}.
 *       Method signature changed accordingly.</li>
 *   <li>The annotation-namespace check ({@code "com.rosetta.model".equals(annotation.getModel().getName())})
 *       cannot be performed because the fork's {@link RAnnotationRef} only
 *       exposes the annotation name (not the resolved annotation's declaring
 *       model). Matching by short name is sufficient for the user-corpus where
 *       these transform annotation names are reserved to the
 *       {@code com.rosetta.model} namespace.</li>
 * </ol>
 *
 * <p>Per-annotation matching is by short name only — {@code "ingest"},
 * {@code "enrich"}, {@code "projection"} — the three transform-namespace
 * annotations that gate label-provider emission per upstream
 * {@code RosettaFunctionExtensions.getTransformAnnotations}.
 */
public class LabelProviderGeneratorUtil {

    /**
     * The three transform-annotation names that gate label-provider emission.
     * Paste-quoted from upstream {@code RosettaFunctionExtensions.java:154}:
     * {@code Stream.of("ingest", "enrich", "projection").anyMatch(...)}.
     */
    private static final Set<String> TRANSFORM_ANNOTATION_NAMES =
            Set.of("ingest", "enrich", "projection");

    /**
     * Whether label-provider emission should be generated for the given
     * function. Mirrors upstream
     * {@code LabelProviderGeneratorUtil.shouldGenerateLabelProvider(Function)}.
     *
     * @param function the function being considered for emission
     * @return {@code true} iff {@code function} carries at least one of the
     *         {@code [ingest]} / {@code [enrich]} / {@code [projection]}
     *         annotations (mirrors
     *         {@code !funcExtensions.getTransformAnnotations(function).isEmpty()})
     */
    public boolean shouldGenerateLabelProvider(RFunction function) {
        if (function == null) return false;
        for (RAnnotationRef ref : function.annotationRefs()) {
            if (ref != null && TRANSFORM_ANNOTATION_NAMES.contains(ref.annotationName())) {
                return true;
            }
        }
        return false;
    }
}
