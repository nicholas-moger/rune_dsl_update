package com.regnosys.rosetta.types.builtin;

import com.regnosys.rosetta.types.*;

import java.util.*;

/**
 * Registry of built-in types. Constructed at workspace build time (D10 —
 * types are data, not code). The default registry provides all standard
 * Rune DSL built-in types.
 *
 * <p>Spec: D10 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public final class BuiltinTypeRegistry {

    private final Map<String, RType> types = new LinkedHashMap<>();

    /** Creates the default registry with all standard Rune DSL built-in types. */
    public static BuiltinTypeRegistry createDefault() {
        BuiltinTypeRegistry reg = new BuiltinTypeRegistry();

        // Zero-param basic types
        reg.register("boolean", RBasicType.BOOLEAN);
        reg.register("time", RBasicType.TIME);
        reg.register("pattern", RBasicType.PATTERN);
        reg.register("nothing", RBasicType.NOTHING);
        reg.register("any", RBasicType.ANY);

        // Parametric types
        reg.register("number", RNumberType.unconstrained());
        reg.register("int", RNumberType.intType());
        reg.register("string", RStringType.unconstrained());

        // Record types
        reg.register("date", RRecordType.DATE);
        reg.register("dateTime", RRecordType.DATE_TIME);
        reg.register("zonedDateTime", RRecordType.ZONED_DATE_TIME);

        return reg;
    }

    private void register(String name, RType type) {
        types.put(name, type);
    }

    /** Looks up a built-in type by name. */
    public Optional<RType> lookup(String name) {
        return Optional.ofNullable(types.get(name));
    }

    /** Looks up and wraps in RMetaAnnotatedType (no meta). */
    public Optional<RMetaAnnotatedType> lookupAnnotated(String name) {
        return lookup(name).map(RMetaAnnotatedType::withNoMeta);
    }

    /**
     * v3.2 seat 9 (D47, F8 — the void-mapping cast): the ONE law for a type call the linker resolved to a
     * {@code basicType} / {@code recordType} DECLARATION node ({@code ast.types.RBasicType} /
     * {@code ast.types.RRecordType}). Such a declaration is either the builtins model's — its M4 twin is
     * registered here under the same name ({@code string}, {@code date}, …) — or the MODEL's own
     * ({@code basicType c15token}, a name no registry knows). The released 9.83.0 plugin types a
     * model-declared basic / record type as {@code nothing} and so renders {@code java.lang.Void} at
     * every seat (the oracle groups {@code void-mapping-basic-record} / {@code -edge}: getters, setters,
     * fields, the process class token, the validators' casts, {@code List<Void>} at list cardinality,
     * {@code FieldWithMetaVoid} / {@code ReferenceWithMetaVoid} under metadata, a function's
     * {@code Void evaluate(Void t)}); before this law the fork answered {@link RMissingType} →
     * {@code Object} (the chaos s15 family, 36 declared rows). Consulted by
     * {@code GeneratorModel.astNodeToRType}, {@code ExpressionTypeComputer.astNodeToRType} and
     * {@code TypeAliasSolver.evaluateBodyWithBindings} (LAW 69 — the generator, the parser and the alias
     * body read the same declaration).
     *
     * @return the registered twin for a builtins-model declaration, {@link RBasicType#NOTHING} for a
     *     model-declared one, empty for any node that is not a basic / record type declaration
     *     (a NAME lookup, so a model-declared type whose name collides with a builtin — {@code basicType string}
     *     in a model — resolves to the BUILTIN: MEASURED at v3.2 seat 9 round 1 against the released 9.83.0 plugin,
     *     which accepts the declaration and renders the attribute {@code String} — the oracle group
     *     void-mapping-name-collision, 5 goldens; {@code BuiltinTypeRegistryTest} pins all three cases)
     */
    public Optional<RType> basicOrRecordNodeType(com.regnosys.rosetta.ast.RNode node) {
        String name;
        if (node instanceof com.regnosys.rosetta.ast.types.RBasicType bt) {
            name = bt.name();
        } else if (node instanceof com.regnosys.rosetta.ast.types.RRecordType rt) {
            name = rt.name();
        } else {
            return Optional.empty();
        }
        return Optional.of(lookup(name).orElse(RBasicType.NOTHING));
    }

    /** All registered types. */
    public Collection<RType> allTypes() {
        return Collections.unmodifiableCollection(types.values());
    }
}
