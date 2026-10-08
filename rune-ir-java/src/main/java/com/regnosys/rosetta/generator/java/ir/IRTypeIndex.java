package com.regnosys.rosetta.generator.java.ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * THE WORKSPACE-WIDE TYPE INDEX (v3.3 seat 8, PR #644 - the property gate): the {@link IREnumIndex} shape, cloned fact
 * for fact over the three DECLARATION kinds a POJO property surface stands on - {@code type} ({@link RDataType}),
 * {@code choice} ({@link RChoice}) and {@code typeAlias} ({@link RTypeAlias}). Every declaration of every model the
 * workspace holds - filtered by the cell's emission filter or NOT - is adapted ONCE to its {@link IRTypeNode} and keyed
 * by {@code namespace.SimpleName}.
 *
 * <p>WHY IT IS WORKSPACE-WIDE and not cell-wide: {@link IRTypeNode#baseType()} is a REFERENCE (a namespace and a
 * resolved name), and the declaration behind it may belong to another model, even one the cell does not generate (the
 * transitive CDM the DRR cells load for resolution only). The old generator reached the supertype through the linker's
 * {@code superType()} link; the IR route reaches it HERE, so that the property model never touches an AST node. The
 * LAW, not the corpus, is what the index encodes - 0 of the corpus's extending types reach a parent outside the filter
 * at this head, and the index would serve one if the model declared it.
 *
 * <p>ONE node per declaration: the generator's reconcile, the property model and the emitter read the SAME instance,
 * so the node the bytes come from is the node every fact was asserted on. A qualified name declared TWICE in the
 * workspace is AMBIGUOUS: {@link #build()} keeps the FIRST declaration it meets under that key (a {@code putIfAbsent}),
 * but the key is booked ambiguous in the same step and {@link #declaration(String)} refuses it BEFORE the map is read -
 * so the kept declaration is never served to anyone, and resolving the name is a refusal rather than a first-wins pick.
 * A name declared NOWHERE is ABSENT, and an ABSENT parent is a refusal, never a silently flat type.
 *
 * <p>THE PARENT IS RECONCILED (the {@link IREnumGenerator} law, generalised from the enum seat): on first resolution
 * {@link #parent(IRType)} books the declaration on a SECOND {@link IRDeclarationReconciler} - counters kept apart from
 * the pass's own population, because a parent may live outside the cell's emission filter and the D11 host holds the
 * pass's {@code declarations} equal to ITS OWN count (LAW 84) - adapts it, reconciles it ONCE, and MEMOISES a refusal
 * so that a parent which disagrees with its source refuses EVERY descendant, at every later call, rather than being
 * read clean by the next child.
 */
final class IRTypeIndex {

    private final RWorkspace workspace;
    private final AstToIRAdapter adapter;
    private final IRDeclarationReconciler parentReconciler;
    private Map<String, RNode> declarations;
    private Set<String> ambiguous;
    private final Map<RNode, IRTypeNode> nodes = new HashMap<>();
    private final Set<RNode> parentsReconciled = new HashSet<>();
    private final Map<RNode, GenerationException> refusals = new HashMap<>();

    IRTypeIndex(RWorkspace workspace, AstToIRAdapter adapter, IRDeclarationReconciler parentReconciler) {
        this.workspace = Objects.requireNonNull(workspace, "workspace");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.parentReconciler = Objects.requireNonNull(parentReconciler, "parentReconciler");
    }

    /** The node of a {@code type} the generator is passing - adapted on first sight, the same instance ever after. */
    IRTypeNode node(String namespace, RDataType declaration) {
        return memoised(namespace, declaration);
    }

    /** The node of a {@code choice} the generator is passing - adapted on first sight, the same instance ever after. */
    IRTypeNode node(String namespace, RChoice declaration) {
        return memoised(namespace, declaration);
    }

    /** The node of a {@code typeAlias} the generator is passing - adapted on first sight, the same instance ever after. */
    IRTypeNode node(String namespace, RTypeAlias declaration) {
        return memoised(namespace, declaration);
    }

    /**
     * The declaration behind a qualified name, over the WHOLE workspace: a {@link RDataType}, a {@link RChoice} or a
     * {@link RTypeAlias}. Empty when ABSENT; a name declared more than once is AMBIGUOUS and a refusal
     * ({@link GenerationException}), never a first-wins pick.
     */
    Optional<RNode> declaration(String qualifiedName) {
        build();
        if (ambiguous.contains(qualifiedName)) {
            throw new GenerationException("IR type index: " + qualifiedName + " is declared more than once in the"
                    + " workspace - the reference is ambiguous", null, null);
        }
        return Optional.ofNullable(declarations.get(qualifiedName));
    }

    /**
     * THE RECONCILED DECLARATION behind a type REFERENCE - the {@code extends} base of a {@code type} (a STRUCT or a
     * CHOICE), and the item type of a specialization candidate whose ancestry the property model's subtype law walks.
     * The reference is resolved by {@link IRType#resolvedQualifiedName()} (a namespace AND a resolved name: the IR
     * route never types a name the linker did not resolve), the declaration is adapted and reconciled ONCE through the
     * index's own second reconciler, and the outcome is MEMOISED - so a parent whose IR disagrees with its source
     * refuses every descendant, for ever, and the reconcile is never re-run for it.
     *
     * @throws GenerationException when the reference is unresolved, when the name is declared NOWHERE (ABSENT) or more
     *     than once (AMBIGUOUS), when the declaration is a {@code typeAlias} (an alias is not a supertype), and when
     *     the declaration's own reconcile mismatched or threw
     */
    IRTypeNode parent(IRType baseTypeReference) {
        Objects.requireNonNull(baseTypeReference, "baseTypeReference");
        String qualifiedName = baseTypeReference.resolvedQualifiedName()
                .orElseThrow(() -> new GenerationException("IR type index: the reference '" + baseTypeReference.name()
                        + "' is UNRESOLVED - a declaration the linker never resolved is refused, never guessed",
                        null, null));
        Optional<RNode> found = declaration(qualifiedName);
        if (found.isEmpty()) {
            throw new GenerationException("IR type index: " + qualifiedName + " is declared NOWHERE in the workspace"
                    + " - the declaration is ABSENT and every descendant is refused", null, null);
        }
        RNode declaration = found.get();
        // the declaring model's namespace, computed ONCE for both the reconcile and the node handed back
        String namespace = namespaceOf(declaration);
        if (parentsReconciled.add(declaration)) {
            // booked BEFORE the adapter and the reconcile run, as the pass does: a throw on the way is a mismatch that
            // REFUSES the declaration for every descendant
            parentReconciler.attempt();
            try {
                List<String> mismatches = reconcile(namespace, declaration, memoised(namespace, declaration));
                if (!mismatches.isEmpty()) {
                    refusals.put(declaration, new GenerationException("IR type index: the declaration " + qualifiedName
                            + " disagrees with its source (" + mismatches.size() + " mismatches, the first: "
                            + mismatches.get(0) + ") - every descendant is refused", null, declaration));
                }
            } catch (RuntimeException e) {
                parentReconciler.threw();
                refusals.put(declaration, new GenerationException("IR type index: the declaration " + qualifiedName
                        + " could not be adapted or reconciled - every descendant is refused", null, declaration, e));
            }
        }
        GenerationException refused = refusals.get(declaration);
        if (refused != null) {
            throw refused;
        }
        return memoised(namespace, declaration);
    }

    /**
     * THE NODE BEHIND A RESOLVED TYPE REFERENCE, or {@code null} when there is none to name - the DESCEND
     * TARGET of the deep-path feature walk ({@code DeepPathScan}'s own hop, one route over). Unlike
     * {@link #parent} it neither reconciles nor refuses: a reference the linker never resolved, a name
     * declared NOWHERE, and a declaration that is a {@code typeAlias} rather than a {@code type} or a
     * {@code choice}, all answer NO TARGET - which is exactly what the walk that calls it requires. A name
     * declared TWICE is still AMBIGUOUS and still throws, through {@link #declaration}.
     *
     * <p>IT LIVES HERE, and not on the emitter that calls it, because the declaration map is keyed by the
     * parser's own AST nodes: this class is the ADAPTER BOUNDARY - the one place in the IR package that
     * holds an AST type - and the sharing lint ({@code IRSharingLawTest}) forbids one to every emitter as of
     * v3.3 seat 9, PR #645 commit 18 (round 1 cq SF-4). Moved from {@code IRDerivedFacts.Descend}
     * statement for statement; no behaviour of the walk changed with it.
     */
    IRTypeNode descendTarget(IRType reference) {
        Optional<String> qualifiedName = reference.resolvedQualifiedName();
        if (qualifiedName.isEmpty()) {
            return null;
        }
        Optional<RNode> declaration = declaration(qualifiedName.get());
        if (declaration.isEmpty()) {
            return null;
        }
        RNode node = declaration.get();
        if (node instanceof RDataType dataType) {
            return node(namespaceOf(dataType), dataType);
        }
        if (node instanceof RChoice choice) {
            return node(namespaceOf(choice), choice);
        }
        return null;
    }

    /**
     * TEST SEAM: plants a node for a declaration in place of the adapter's - the corpus-free way to make a reconcile
     * see an IR that disagrees with its source ({@code IRTypeIndexTest}: a planted lying parent refuses every
     * descendant). Package-private; no production caller.
     */
    void plant(RNode declaration, IRTypeNode node) {
        nodes.put(Objects.requireNonNull(declaration, "declaration"), Objects.requireNonNull(node, "node"));
    }

    /** The declaring model's namespace - the same key the generator passes for its own models. */
    static String namespaceOf(RNode declaration) {
        if (declaration.parent() instanceof RModel model) {
            return model.namespace();
        }
        throw new GenerationException("IR type index: " + simpleNameOf(declaration) + " has no parent model",
                null, declaration);
    }

    /** The counters of the index's own second reconciler - the D11 host prints them on their own line. */
    int[] parentReconcileStats() {
        return parentReconciler.stats();
    }

    // ------------------------------------------------------------------------------------------------ the internals

    private IRTypeNode memoised(String namespace, RNode declaration) {
        IRTypeNode node = nodes.get(declaration);
        if (node == null) {
            node = adapt(namespace, declaration);
            nodes.put(declaration, node);
        }
        return node;
    }

    private IRTypeNode adapt(String namespace, RNode declaration) {
        if (declaration instanceof RDataType dataType) {
            return adapter.adaptData(namespace, dataType);
        }
        if (declaration instanceof RChoice choice) {
            return adapter.adaptChoice(namespace, choice);
        }
        if (declaration instanceof RTypeAlias alias) {
            return adapter.adaptTypeAlias(namespace, alias);
        }
        throw new GenerationException("IR type index: " + simpleNameOf(declaration) + " is not a type declaration the"
                + " index admits (type / choice / typeAlias)", null, declaration);
    }

    /** The declaration's own reconcile, by its kind. A {@code typeAlias} is not a supertype and is refused as one. */
    private List<String> reconcile(String namespace, RNode declaration, IRTypeNode node) {
        if (declaration instanceof RDataType dataType) {
            return parentReconciler.reconcileData(namespace, dataType, node);
        }
        if (declaration instanceof RChoice choice) {
            return parentReconciler.reconcileChoice(namespace, choice, node);
        }
        throw new GenerationException("IR type index: " + simpleNameOf(declaration) + " is a typeAlias - an alias is"
                + " not a supertype and is refused as one", null, declaration);
    }

    private void build() {
        if (declarations != null) {
            return;
        }
        Map<String, RNode> found = new HashMap<>();
        Set<String> twice = new HashSet<>();
        for (RModel model : workspace.files()) {
            for (var element : model.rootElements()) {
                String simpleName = simpleNameOrNull(element);
                if (simpleName == null) {
                    continue;
                }
                String key = model.namespace() + "." + simpleName;
                if (found.putIfAbsent(key, element) != null) {
                    twice.add(key);
                }
            }
        }
        declarations = found;
        ambiguous = twice;
    }

    /** The simple name of a declaration the index admits, or {@code null} for every other root element. */
    private static String simpleNameOrNull(RNode element) {
        if (element instanceof RDataType dataType) {
            return dataType.name();
        }
        if (element instanceof RChoice choice) {
            return choice.name();
        }
        if (element instanceof RTypeAlias alias) {
            return alias.name();
        }
        return null;
    }

    private static String simpleNameOf(RNode declaration) {
        String simpleName = simpleNameOrNull(declaration);
        return simpleName == null ? String.valueOf(declaration) : simpleName;
    }
}
