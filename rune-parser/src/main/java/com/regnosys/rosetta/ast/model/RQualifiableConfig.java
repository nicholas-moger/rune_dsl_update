package com.regnosys.rosetta.ast.model;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.QualifiableKind;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.Optional;

/**
 * Qualifiable configuration node, corresponding to the
 * {@code rosettaQualifiableConfiguration} grammar rule.
 *
 * <p>Grammar:
 * <pre>
 * rosettaQualifiableConfiguration:
 *     (IS_EVENT | IS_PRODUCT) ROOT qualifiedName SEMI
 * ;
 * </pre>
 */
public class RQualifiableConfig extends RNode {

    private QualifiableKind kind;
    private String rootTypeName;

    // -- kind -----------------------------------------------------------------

    public QualifiableKind kind() {
        return kind;
    }

    public void setKind(QualifiableKind kind) {
        checkMutable();
        this.kind = kind;
    }

    // -- rootTypeName ---------------------------------------------------------

    public String rootTypeName() {
        return rootTypeName;
    }

    public void setRootTypeName(String rootTypeName) {
        checkMutable();
        this.rootTypeName = rootTypeName;
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.TYPE_NOT_FOUND, tokenRangeKey = "rootTypeName")
    private SymbolId rootTypeId;

    /**
     * Returns the resolved root data type, looked up lazily through the
     * attached workspace. Returns Optional.empty() if not declared or not
     * resolved.
     *
     * @throws IllegalStateException if this node has no attached workspace
     * @throws com.regnosys.rosetta.symbols.StaleSymbolIdException if the stored SymbolId was issued by a
     *     different workspace generation
     */
    public Optional<RDataType> rootType() {
        if (rootTypeId == null) return Optional.empty();
        // PR #445: this node is the requester — duplicate FQNs resolve
        // same-file/same-cell first (CellPreference).
        return Optional.ofNullable(workspace().resolve(rootTypeId, RDataType.class, this));
    }

    /**
     * Returns the {@link SymbolId} of the resolved root data type. Use
     * {@link #rootType()} for the resolved RDataType node. New in P1.4.1b.
     */
    public Optional<SymbolId> rootTypeId() {
        return Optional.ofNullable(rootTypeId);
    }

    public void setRootTypeId(SymbolId id) {
        checkMutable();
        this.rootTypeId = id;
    }

    /**
     * v3.2 seat 4 (PR #625, F10): the qualifiable root of {@code kind} over {@code modelsInLoadOrder} — the
     * FIRST configuration of that kind whose root resolved; every later configuration of the same kind
     * LOSES. ONE declaration for the meta generator and the qualification validator's warning, both over the
     * workspace's models in load order — LAW 69. What is PORTED: first-of-kind, in the order the caller
     * supplies (upstream's {@code RosettaConfigExtension.findRosettaQualifiableConfiguration} is
     * {@code Iterables.getFirst} over the index's exported configurations of the kind). What is NOT the
     * same, said plainly (round 1): upstream skips a configuration whose OWN node is unresolved
     * ({@code isResolved(eObj)}); this loop skips one whose ROOT did not resolve — the fork's choice, no
     * oracle either way. Upstream's iteration order is a function of the resource PATH — the seat's three
     * order probes: the same three files, byte-identical, under another directory name pick a different
     * winner — so it is not a sorted order; a hash over resource URIs is consistent with that and with the
     * post-9.83 source, but INFERRED, not measured. Hence the caller's load order is the fork's rule: where
     * a golden fixes a winner the loader replays it by pin (#413); elsewhere the first file in load order
     * wins. The index's SCOPE: {@code findRosettaQualifiableConfiguration} filters by {@code isProjectLocal},
     * which returns {@code true} at once for a non-platform ({@code file:}) context URI — in the post-9.83
     * source AND in the released 9.83.0 {@code rune-lang} jar ({@code javap -c} at round 2: {@code
     * isPlatformResource()} false → {@code true}; the {@code filter → transform → filter(nonNull) → getFirst}
     * chain identical) — so under {@code file:} URIs the index is the whole resource set, which is what this
     * loop reads over. What stays UNMEASURED is not the code but the released Maven plugin's RUNTIME URI
     * scheme (whether its resources are non-platform URIs; no oracle group can load a dependency) — banked,
     * see {@code ModelMetaGenerator.isQualifiableRoot}.
     */
    public static Optional<RDataType> firstRoot(Iterable<RModel> modelsInLoadOrder, QualifiableKind kind) {
        for (RModel model : modelsInLoadOrder) {
            for (RQualifiableConfig config : model.configurations()) {
                if (config.kind() == kind) {
                    Optional<RDataType> root = config.rootType();
                    if (root.isPresent()) {
                        return root;
                    }
                }
            }
        }
        return Optional.empty();
    }
}
