package com.regnosys.rosetta.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract scope for code generation. Manages generated identifiers with
 * automatic name uniquification and conflict resolution.
 *
 * <p>Ported from upstream {@code com.regnosys.rosetta.generator.GeneratorScope}
 * with Guava's {@code LinkedListMultimap} replaced by plain Java collections,
 * and EMF's {@code RosettaNamed} removed (our AST nodes use {@code name()} not
 * {@code getName()}).
 *
 * <p>D7: This class must produce identical identifier names to upstream
 * for D11 byte-identical output compliance.
 */
public abstract class GeneratorScope<Scope extends GeneratorScope<?>> {
    private final Scope parent;
    private final Map<Object, GeneratedIdentifier> identifiers = new LinkedHashMap<>();
    private final Map<Object, Object> keySynonyms = new HashMap<>();

    private boolean isClosed = false;
    private Map<GeneratedIdentifier, String> actualNames = null;

    private final String description;

    protected GeneratorScope(String description) {
        this(description, null);
    }

    protected GeneratorScope(String description, Scope parent) {
        this.description = description;
        this.parent = parent;
    }

    /**
     * Determine whether {@code name} is a valid identifier in the target language.
     */
    public abstract boolean isValidIdentifier(String name);

    public String escapeName(String name) {
        return "_" + name;
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    public Scope getParent() {
        return this.parent;
    }

    public String getDebugInfo() {
        StringBuilder b = new StringBuilder();
        b.append(this.description);
        if (!this.isClosed) {
            b.append(" <unclosed>");
        }
        b.append(":");
        Map<Object, GeneratedIdentifier> ownIdentifiers = getOwnIdentifiersMap();
        if (ownIdentifiers.isEmpty()) {
            b.append(" <no identifiers>");
        } else {
            ownIdentifiers.entrySet().forEach(e ->
                    b.append("\n\t").append(normalizeKey(e.getKey()))
                     .append(" -> \"").append(e.getValue().getDesiredName()).append("\""));
        }
        this.keySynonyms.entrySet().forEach(e ->
            b.append("\n\t")
             .append("(keySynonym): ")
             .append(normalizeKey(e.getKey()))
             .append(" -> ")
             .append(normalizeKey(e.getValue())));

        if (parent != null) {
            // ci-allowlist: regex-on-structured-content (debug-info string indentation)
            b.append("\n").append(parent.getDebugInfo().replaceAll("(?m)^", "\t"));
        }
        return b.toString();
    }

    private String normalizeKey(Object key) {
        return key.toString().replace("\n", "\\n");
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("=========== Scope Description ==========\n");
        b.append(getDebugInfo());
        b.append("\n========================================\n");
        return b.toString();
    }

    protected Map<Object, GeneratedIdentifier> getOwnIdentifiersMap() {
        return identifiers;
    }

    public boolean isNameTaken(String desiredName) {
        return getOwnIdentifiersMap().values().stream()
                    .anyMatch(id -> desiredName.equals(id.getDesiredName()))
                || getParent() != null && getParent().isNameTaken(desiredName);
    }

    public Optional<GeneratedIdentifier> getIdentifier(Object obj) {
        return Optional.ofNullable(parent)
                .flatMap(p -> p.getIdentifier(obj))
                .or(() -> Optional.ofNullable(getOwnIdentifiersMap().get(obj)))
                .or(() -> Optional.ofNullable(this.keySynonyms.get(obj))
                        .flatMap(key -> getIdentifier(key)));
    }

    public GeneratedIdentifier getIdentifierOrThrow(Object obj) {
        return getIdentifier(obj)
                .orElseThrow(() -> new NoSuchElementException(
                        "No identifier defined for " + normalizeKey(obj) + " in scope.\n" + this));
    }

    protected GeneratedIdentifier overwriteIdentifier(Object obj, String name) {
        if (isClosed) {
            throw new IllegalStateException(
                    "Cannot create a new identifier in a closed scope. ("
                    + normalizeKey(obj) + " -> " + name + ")\n" + this);
        }
        GeneratedIdentifier id = new GeneratedIdentifier(this, name);
        this.identifiers.put(obj, id);
        return id;
    }

    public GeneratedIdentifier createIdentifier(Object obj, String name) {
        if (this.getIdentifier(obj).isPresent()) {
            throw new IllegalStateException(
                    "There is already a name defined for object `" + normalizeKey(obj) + "`.\n" + this);
        }
        return overwriteIdentifier(obj, name);
    }

    public GeneratedIdentifier createUniqueIdentifier(String name) {
        Object token = new Object() {
            @Override
            public String toString() {
                return "{unique token for \"" + name + "\"}";
            }
        };
        return createIdentifier(token, name);
    }

    public GeneratedIdentifier getOrCreateIdentifier(Object obj, String name) {
        return this.getIdentifier(obj).orElseGet(() -> createIdentifier(obj, name));
    }

    /**
     * Return {@code desiredName} escaped ({@link #escapeName(String)}) as many
     * times as needed until it neither clashes with a name already declared in
     * this scope or an ancestor ({@link #isNameTaken(String)}) nor is an invalid
     * identifier.
     *
     * <p><b>Non-mutating:</b> does NOT register the name or close the scope, so
     * it is safe to call repeatedly while the scope is still open — unlike
     * {@link #getActualName(GeneratedIdentifier)}, which closes this scope and
     * its ancestors to materialise names. Mirrors the escape loop in
     * {@code computeActualNames()} for a single fresh name. The same-scope
     * numeric-suffix branch there applies only to multiple identifiers sharing
     * a desired name in one scope — names that must OBSERVE that branch (the
     * null-guarded Type-coercion lambda params upstream registers via
     * {@code convertNullSafe}, numbered {@code 0..n-1} per scope group) go
     * through {@code JavaStatementScope.registerDeferredCoercionParam} instead
     * (facet meta_coercion_numbering); names that are one-off per scope (nav
     * lambda vars, bare MapperC coercion params — upstream never registers the
     * latter, so they repeat bare) stay on this read-only path.
     *
     * <p>Phase X1 (rendering pivot): used by {@code NavigationHandler} to name
     * feature-call lambda variables faithfully to upstream
     * {@code ExpressionGenerator.xtend}'s
     * {@code scope.lambdaScope.createUniqueIdentifier(type.toFirstLower)}
     * semantics. A function body whose input is named {@code businessEvent}
     * (registered in the body scope) forces a lambda {@code businessEvent} to
     * escape to {@code _businessEvent}; a rule body whose only in-scope name is
     * the implicit {@code input} leaves a lambda {@code settlementTerms}
     * un-escaped.
     */
    public String disambiguate(String desiredName) {
        String name = desiredName;
        boolean lastWasValid = true;
        while (true) {
            boolean isValid = isValidIdentifier(name);
            if (!lastWasValid && !isValid) {
                throw new RuntimeException(
                        "Tried escaping the identifier `" + name
                        + "`, but it is still not a valid identifier.");
            }
            if (isNameTaken(name) || !isValid) {
                name = escapeName(name);
            } else {
                break;
            }
            lastWasValid = isValid;
        }
        return name;
    }

    public void createKeySynonym(Object key, Object keyWithIdentifier) {
        if (isClosed) {
            throw new IllegalStateException(
                    "Cannot create a new key synonym in a closed scope. ("
                    + normalizeKey(key) + " -> " + normalizeKey(keyWithIdentifier) + ")\n" + this);
        }
        if (this.getIdentifier(key).isPresent()) {
            throw new IllegalStateException(
                    "There is already a name defined for key `" + normalizeKey(key) + "`.\n" + this);
        }
        if (this.getIdentifier(keyWithIdentifier).isEmpty()) {
            throw new IllegalStateException(
                    "There is no name defined for key `" + normalizeKey(keyWithIdentifier) + "`.\n" + this);
        }
        this.keySynonyms.put(key, keyWithIdentifier);
    }

    public void createSynonym(Object key, GeneratedIdentifier identifier) {
        if (isClosed) {
            throw new IllegalStateException(
                    "Cannot create a new synonym in a closed scope. ("
                    + normalizeKey(key) + " -> " + normalizeKey(identifier) + ")\n" + this);
        }
        if (this.getIdentifier(key).isPresent()) {
            throw new IllegalStateException(
                    "There is already a name defined for key `" + normalizeKey(key) + "`.\n" + this);
        }
        this.identifiers.put(key, identifier);
    }

    public void close() {
        if (this.isClosed) {
            throw new IllegalStateException("The scope is already closed.\n" + this);
        }
        this.isClosed = true;
    }

    public Optional<String> getActualName(GeneratedIdentifier identifier) {
        if (!this.isClosed) {
            this.close();
        }
        return Optional.ofNullable(parent)
                .flatMap(p -> p.getActualName(identifier))
                .or(() -> {
                    if (this.actualNames == null) {
                        this.computeActualNames();
                    }
                    return Optional.ofNullable(this.actualNames.get(identifier));
                });
    }

    private void computeActualNames() {
        this.actualNames = new HashMap<>();

        Set<String> takenNames = getAllTakenNamesFromParent();
        // LinkedListMultimap replacement: group identifiers by desired name, preserving insertion order
        LinkedHashMap<String, List<GeneratedIdentifier>> idsByDesiredName = new LinkedHashMap<>();
        identifiers.values().stream().distinct().forEach(id ->
                idsByDesiredName.computeIfAbsent(id.getDesiredName(), k -> new ArrayList<>()).add(id));

        for (var entry : idsByDesiredName.entrySet()) {
            String desiredName = entry.getKey();
            List<GeneratedIdentifier> ids = entry.getValue();
            for (int i = 0; i < ids.size(); i++) {
                GeneratedIdentifier id = ids.get(i);
                String name = desiredName;
                if (ids.size() > 1) {
                    name += i;
                }
                boolean lastWasValid = true;
                while (true) {
                    boolean isValid = isValidIdentifier(name);
                    if (!lastWasValid && !isValid) {
                        throw new RuntimeException(
                                "Tried escaping the identifier `" + name
                                + "`, but it is still not a valid identifier.");
                    }
                    if (takenNames.contains(name) || !isValid) {
                        name = escapeName(name);
                    } else {
                        break;
                    }
                    lastWasValid = isValid;
                }
                takenNames.add(name);
                this.actualNames.put(id, name);
            }
        }
    }

    protected Set<String> getAllTakenNames() {
        Set<String> result = getAllTakenNamesFromParent();
        result.addAll(this.getOwnTakenNames());
        return result;
    }

    protected Collection<String> getOwnTakenNames() {
        if (this.actualNames == null) {
            this.computeActualNames();
        }
        return this.actualNames.values();
    }

    protected Set<String> getAllTakenNamesFromParent() {
        if (parent == null) {
            return new HashSet<>();
        }
        return parent.getAllTakenNames();
    }
}
