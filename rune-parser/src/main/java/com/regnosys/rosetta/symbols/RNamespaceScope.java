package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RRootElement;

import java.util.*;

/**
 * One namespace in the workspace symbol table. Holds a multimap of
 * declarations keyed by local name (preserving duplicates for M6's
 * names-are-unique check) and the list of files contributing declarations
 * to this namespace.
 *
 * <p>Spec: D8 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class RNamespaceScope {

    private final String qualifiedName;
    private final Map<String, List<RRootElement>> declarations = new HashMap<>();
    private final List<RModel> contributingFiles = new ArrayList<>();

    /**
     * Written only by the M3 linker — do not call from application code.
     *
     * <p><b>Immutability note:</b> {@code RNamespaceScope} exposes public
     * mutators ({@link #register}, {@link #addContributingFile}) because
     * the linker lives in a different Java package and package-private
     * visibility cannot cross package boundaries. After
     * {@link RWorkspace#build(java.util.List)} returns, the workspace is
     * effectively immutable — callers MUST NOT invoke the mutators. This
     * matches the same convention used for M2 AST setters (public + Javadoc
     * "written only by linker/builder"). See D2 access control in the spec.
     */
    public RNamespaceScope(String qualifiedName) {
        this.qualifiedName = Objects.requireNonNull(qualifiedName);
    }

    public String qualifiedName() { return qualifiedName; }

    /** Adds a declaration. Written only by the M3 linker — do not call from application code. */
    public void register(String localName, RRootElement decl) {
        declarations.computeIfAbsent(localName, k -> new ArrayList<>()).add(decl);
    }

    /** Adds a contributing file. Written only by the M3 linker — do not call from application code. */
    public void addContributingFile(RModel file) {
        contributingFiles.add(file);
    }

    /**
     * Returns the FIRST declaration registered with this name, in workspace
     * order (file by stable sort, then source order within a file). Matches
     * Xtext's index first-wins lookup. {@link Optional#empty()} when nothing
     * is registered with this name.
     */
    public Optional<RRootElement> lookup(String localName) {
        List<RRootElement> matches = declarations.get(localName);
        if (matches == null || matches.isEmpty()) return Optional.empty();
        return Optional.of(matches.get(0));
    }

    /**
     * Returns ALL declarations registered with this name. Used by M6 to
     * detect duplicates for the names-are-unique validation. Returns an
     * empty list if nothing matches.
     */
    public List<RRootElement> allMatching(String localName) {
        List<RRootElement> matches = declarations.get(localName);
        return matches == null ? List.of() : List.copyOf(matches);
    }

    /**
     * Returns all declarations in this namespace, regardless of name.
     * Sorted by local name for deterministic iteration order (D11).
     */
    public List<RRootElement> allDeclarations() {
        List<RRootElement> all = new ArrayList<>();
        declarations.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> all.addAll(e.getValue()));
        return List.copyOf(all);
    }

    public List<RModel> contributingFiles() { return List.copyOf(contributingFiles); }
}
