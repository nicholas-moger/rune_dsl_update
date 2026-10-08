package com.regnosys.rosetta.generator.java.ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * THE WORKSPACE-WIDE ENUM INDEX (v3.3 seat 6, PR #642): every {@code enum} declaration of every model the workspace
 * holds - filtered by the cell's emission filter or not - adapted ONCE to its {@link IREnumNode} and keyed by
 * {@code namespace.SimpleName}. The emitter flattens a parent chain through it: {@link IREnumNode#parent()} is a
 * reference (a namespace and a resolved name), and the parent's own node may belong to another model, even one the
 * cell does not generate (DRR's {@code CommodityTimeUnitEnum extends TimeUnitEnum} reaches into the transitive CDM the
 * DRR cells load for resolution only). The old generator reached the parent through the linker's {@code superType()}
 * link; the IR route reaches it through this index, so that the emitter never touches an AST node.
 *
 * <p>ONE node per declaration: the generator's reconcile and the emitter read the SAME instance, so the node the
 * bytes come from is the node every fact was asserted on. A qualified name declared twice in the workspace is
 * AMBIGUOUS - the index keeps neither and resolving it is a refusal (no vendored or chaos cell declares one; the
 * seat's probe over the 26 cells).
 */
final class IREnumIndex {

    private final RWorkspace workspace;
    private final AstToIRAdapter adapter;
    private Map<String, REnumeration> declarations;
    private Set<String> ambiguous;
    private final Map<REnumeration, IREnumNode> nodes = new HashMap<>();

    IREnumIndex(RWorkspace workspace, AstToIRAdapter adapter) {
        this.workspace = Objects.requireNonNull(workspace, "workspace");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    /** The node of a declaration the generator is passing - adapted on first sight, the same instance ever after. */
    IREnumNode node(String namespace, REnumeration enumeration) {
        IREnumNode node = nodes.get(enumeration);
        if (node == null) {
            node = adapter.adaptEnum(namespace, enumeration);
            nodes.put(enumeration, node);
        }
        return node;
    }

    /**
     * TEST SEAM: plants a node for a declaration in place of the adapter's - the corpus-free way to make the reconcile
     * see an IR that disagrees with its source ({@code IREnumEmitterTest}: a mismatched declaration is refused and
     * writes no file). Package-private; no production caller.
     */
    void plant(REnumeration enumeration, IREnumNode node) {
        nodes.put(Objects.requireNonNull(enumeration, "enumeration"), Objects.requireNonNull(node, "node"));
    }

    /**
     * The declaration behind a qualified name, over the WHOLE workspace: empty when ABSENT; a name declared more than
     * once is AMBIGUOUS and a refusal ({@link GenerationException}), never a first-wins pick.
     */
    Optional<REnumeration> declaration(String qualifiedName) {
        build();
        if (ambiguous.contains(qualifiedName)) {
            throw new GenerationException("IR enum index: " + qualifiedName + " is declared more than once in the workspace"
                    + " - the parent reference is ambiguous", null, null);
        }
        return Optional.ofNullable(declarations.get(qualifiedName));
    }

    /** The declaring model's namespace - the same key the generator passes for its own models. */
    static String namespaceOf(REnumeration enumeration) {
        if (enumeration.parent() instanceof RModel model) {
            return model.namespace();
        }
        throw new GenerationException("IR enum index: " + enumeration.name() + " has no parent model", null, null);
    }

    private void build() {
        if (declarations != null) {
            return;
        }
        Map<String, REnumeration> found = new HashMap<>();
        Set<String> twice = new HashSet<>();
        for (RModel model : workspace.files()) {
            for (var element : model.rootElements()) {
                if (element instanceof REnumeration enumeration) {
                    String key = model.namespace() + "." + enumeration.name();
                    if (found.putIfAbsent(key, enumeration) != null) {
                        twice.add(key);
                    }
                }
            }
        }
        declarations = found;
        ambiguous = twice;
    }
}
