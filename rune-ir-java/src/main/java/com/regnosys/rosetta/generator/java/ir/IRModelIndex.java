package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * THE WORKSPACE-WIDE MODEL INDEX (v3.3 seat 8, PR #644 - the property gate; {@code PLAN.md} § B family 5).
 *
 * <p>{@link IRTypeIndex}'s shape over the MODEL node: every model the workspace holds - inside the cell's emission
 * filter or not - is adapted ONCE, through the pass's OWN {@link AstToIRAdapter}, in the workspace's LOAD ORDER, and
 * memoised by declaration identity. It exists for one law the model node deliberately does not carry:
 *
 * <p><b>The qualifiable ROOT is a load-order fact, so it is the INDEX's.</b>
 * {@code RQualifiableConfig.firstRoot} ({@code :107-119}) takes the FIRST configuration of a kind, over the
 * workspace's models in load order, WHOSE ROOT RESOLVED; every later configuration of that kind loses. That law is
 * not a fact of any one model - the drr cells' roots live in the transitive CDM they load for resolution only - so
 * before this class the IR half of {@code model.qualify.matched} had to call the SOURCE's own {@code firstRoot} and
 * was therefore asserted against itself (round-1 MF-2). {@link #firstRoot(String)} computes the same law over the
 * MODEL NODES: the first node, in load order, with a configuration of that kind whose root reference carries a
 * RESOLVED qualified name. The two producers then answer for the root itself
 * ({@code model.qualify.root.<kind>}, {@link IRModelReconciler}) and for the wing it decides.
 *
 * <p>"Root resolved" is the IR's own spelling of the source's {@code config.rootType().isPresent()}: the adapter
 * builds an unresolved configuration root as a reference with no namespace and no resolved name
 * ({@code AstToIRAdapter.resolvedRef}), so a reference with no {@link IRType#resolvedQualifiedName()} is exactly the
 * configuration the source loop skips.
 *
 * <p>A model the adapter cannot adapt is a REFUSAL that names it, never a model quietly missing from the load order:
 * the root law is a first-wins over a POPULATION, and a population with a hole in it can pick the wrong winner.
 */
final class IRModelIndex {

    private final RWorkspace workspace;
    private final AstToIRAdapter adapter;
    /** One node per {@link RModel}, by declaration IDENTITY - a record's structural equality is not the key here. */
    private final Map<RModel, IRModelNode> byModel = new IdentityHashMap<>();
    /** TEST SEAM (see {@link #plant}) - a planted root for a kind, ahead of the load-order scan. */
    private final Map<String, IRType> planted = new LinkedHashMap<>();
    private List<IRModelNode> nodes;

    IRModelIndex(RWorkspace workspace, AstToIRAdapter adapter) {
        this.workspace = Objects.requireNonNull(workspace, "workspace");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    /** Every model of the workspace as a node, in LOAD ORDER - adapted once, the same instances ever after. */
    List<IRModelNode> nodes() {
        build();
        return nodes;
    }

    /** The node of one model - the SAME instance {@link #nodes()} holds, so the pass adapts each model once. */
    IRModelNode node(RModel model) {
        Objects.requireNonNull(model, "model");
        build();
        IRModelNode node = byModel.get(model);
        if (node == null) {
            // a model the workspace does not list (a test harness's detached model): adapted and memoised here
            node = adapt(model);
            byModel.put(model, node);
        }
        return node;
    }

    /**
     * The qualifiable ROOT of {@code kind} over the model nodes in LOAD ORDER - the FIRST configuration of that kind
     * whose root RESOLVED, every later one losing ({@code RQualifiableConfig.firstRoot}'s own law, computed here from
     * the IR alone).
     *
     * @param kind one of {@link IRQualifiableConfig#KINDS} ({@code IS_EVENT} / {@code IS_PRODUCT})
     */
    Optional<IRType> firstRoot(String kind) {
        Objects.requireNonNull(kind, "kind");
        IRType lie = planted.get(kind);
        if (lie != null) {
            return Optional.of(lie);
        }
        for (IRModelNode node : nodes()) {
            for (IRQualifiableConfig configuration : node.qualifiableConfigs()) {
                if (!kind.equals(configuration.kind())) {
                    continue;
                }
                if (configuration.rootType().resolvedQualifiedName().isPresent()) {
                    return Optional.of(configuration.rootType());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * TEST SEAM: plants the root of a kind in place of the load-order scan's - the corpus-free way to make the IR
     * half of {@code model.qualify.root.<kind>} disagree with the source's {@code firstRoot}
     * ({@code IRModelReconcileTest}: a lying index reads the root fact RED). Package-private; no production caller.
     */
    void plant(String kind, IRType root) {
        planted.put(Objects.requireNonNull(kind, "kind"), Objects.requireNonNull(root, "root"));
    }

    // ------------------------------------------------------------------------------------------------ the internals

    private void build() {
        if (nodes != null) {
            return;
        }
        List<IRModelNode> built = new ArrayList<>();
        for (RModel model : workspace.files()) {
            IRModelNode node = byModel.get(model);
            if (node == null) {
                node = adapt(model);
                byModel.put(model, node);
            }
            built.add(node);
        }
        nodes = List.copyOf(built);
    }

    private IRModelNode adapt(RModel model) {
        try {
            return adapter.adaptModelNode(model);
        } catch (RuntimeException e) {
            throw new GenerationException("IR model index: the model '" + model.namespace() + "' could not be adapted"
                    + " - the workspace-wide first-wins root law is a scan over a POPULATION and cannot be computed"
                    + " with a model missing from it", null, model, e);
        }
    }
}
