package com.regnosys.rosetta.types;

import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;

import java.util.List;
import java.util.Objects;

/**
 * Union/choice type reference. Holds the choice name, its flattened
 * option types, and optionally the AST node for namespace resolution.
 */
public final class RChoiceTypeRef implements RType {
    private final String name;
    private final List<RType> options;
    private final RChoice astNode;

    public RChoiceTypeRef(String name, List<RType> options) {
        this(name, options, null);
    }

    public RChoiceTypeRef(String name, List<RType> options,
                          RChoice astNode) {
        this.name = Objects.requireNonNull(name);
        this.options = List.copyOf(options);
        this.astNode = astNode;
    }

    @Override public String name() { return name; }
    @Override public boolean hasNaturalOrder() { return false; }
    public List<RType> options() { return options; }
    public RChoice astNode() { return astNode; }

    /**
     * Bridge from this choice type reference to an {@link RDataType}
     * representation, mirroring upstream {@code RChoiceType.asRDataType()}.
     *
     * <p>Phase X T2 — added per spec § 3.4 + plan {@code 2026-05-19-phase-x-port-3-generators.md}
     * T2 step 6. Required by {@link com.regnosys.rosetta.utils.DeepFeatureCallUtil}'s
     * unwrap branch (recursion through choice-typed intermediate attributes).
     *
     * <p>The bridge synthesises a fresh {@link RDataType} whose attributes
     * are the choice's options projected onto data-type attributes. The
     * source {@link RChoice} astNode is the same node {@code ChoiceObjectGenerator}
     * uses to drive {@code RJavaPojoInterface(RChoice, ...)}-based POJO
     * emission; the data-type projection here serves the upstream-parity
     * use-case (deep-feature traversal through choice options) only.
     *
     * <p>When {@code astNode} is null (choice ref constructed without an
     * AST anchor — used in some IR-only fixtures), returns {@code null}
     * so callers can short-circuit cleanly.
     *
     * <p><b>Naming divergence from upstream:</b> upstream's
     * {@code RChoiceType.asRDataType()} delegates to
     * {@code rObjectFactory.buildRDataType(choice)} and the projected
     * attributes carry the choice option's own attribute names. The fork's
     * {@link RChoice} options have no separate name field — the {@code typeCall}
     * IS the option identity (per the choice grammar), so the projection
     * uses the option's type name as the attribute name. Downstream consumers
     * that key on attribute names (e.g. {@code DeepFeatureCallUtil}'s
     * {@code findDeepFeatureMap}) therefore see type-name keys for
     * choice-projected attributes, which can collide with same-named
     * non-choice attributes elsewhere in the model. Callers needing
     * collision-free keying must disambiguate at the call site.
     */
    public RDataType asRDataType() {
        if (astNode == null) return null;
        RDataType bridge = new RDataType();
        bridge.setName(astNode.name());
        // Project each choice option's typeCall onto a data-type attribute
        // whose name mirrors the option's referenced type. The choice
        // option's typeCall IS the option's identity (per RChoiceOption
        // grammar: "There is no separate name field — the typeCall IS the
        // identity"), so the projection preserves the upstream attribute
        // set used by DeepFeatureCallUtil's path traversal.
        // Copilot PR #72 R4 F9 — deep-copy the option's typeCall onto a fresh
        // RTypeCall so the synthesized RDataType's attribute owns its own
        // AST subtree. Reusing opt.typeCall() directly would create a shared
        // subtree (one RTypeCall referenced from both the source RChoiceOption
        // AND the synthetic RAttribute), conflicting with RNode's single-parent
        // contract via setParent's checkMutable() guard once either side is
        // frozen by a downstream pass.
        astNode.options().forEach(opt -> {
            RAttribute attr = new RAttribute();
            RTypeCall sourceTypeCall = opt.typeCall();
            attr.setName(sourceTypeCall == null ? "?" : sourceTypeCall.typeName());
            attr.setTypeCall(RTypeCall.deepCopy(sourceTypeCall));
            // Facet choice_nav_chain_typing — carry the option's annotation refs
            // (name + qualifier only) onto the projected attribute. A choice option
            // can be meta-annotated (`choice Underlier: Observable [metadata address ...]`),
            // and the upstream projection (rObjectFactory.buildRDataType) keeps those
            // annotations, so consumers reading the metadata-kind signal off a projected
            // attribute (MetaFieldGenerator.detectMetaKind → the navigation witness /
            // meta result type / SET_VALUE ctor setter) see the same answer as for a
            // declared attribute. FRESH nodes (not the option's own subtree) for the
            // same single-parent-contract reason the typeCall is deep-copied; the
            // qualifier ARGUMENTS (e.g. "pointsTo"=...) and resolved slots are not
            // part of that signal and are deliberately not projected.
            opt.annotationRefs().forEach(srcRef -> {
                RAnnotationRef refCopy = new RAnnotationRef();
                refCopy.setAnnotationName(srcRef.annotationName());
                srcRef.qualifierName().ifPresent(refCopy::setQualifierName);
                attr.annotationRefs().add(refCopy);
            });
            bridge.attributes().add(attr);
        });
        return bridge;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RChoiceTypeRef that)) return false;
        return name.equals(that.name) && options.equals(that.options);
    }
    @Override public int hashCode() { return Objects.hash(name, options); }
    @Override public String toString() { return "choice " + name; }
}
