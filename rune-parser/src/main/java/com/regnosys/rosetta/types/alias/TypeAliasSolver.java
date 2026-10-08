package com.regnosys.rosetta.types.alias;

import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;

import java.math.BigDecimal;
import java.util.*;

/**
 * Solves type alias parameter equations. Supports forward evaluation
 * (given parameter values, compute the aliased type) and reverse evaluation
 * (given a concrete type, solve for parameter values).
 *
 * <p>Matches Xtext's {@code RosettaSimpleSystemSolver} semantics.
 *
 * <p>Spec: section 3.7 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public final class TypeAliasSolver {

    private static final int MAX_DEPTH = 100;

    private static final BuiltinTypeRegistry BUILTINS = BuiltinTypeRegistry.createDefault();

    /**
     * Forward evaluation: unwraps alias chains to the underlying concrete type.
     * Parameters are already substituted in the RAliasType's refersTo during
     * construction — this method simply walks the chain. Returns the leaf type.
     * Bounded by MAX_DEPTH to prevent infinite loops on cyclic aliases.
     */
    public RType evaluateForward(RType type) {
        RType current = type;
        int depth = 0;
        while (current instanceof RAliasType alias && depth++ < MAX_DEPTH) {
            current = alias.refersTo();
        }
        return current;
    }

    /**
     * Reverse evaluation: given an alias template and a concrete type,
     * attempts to solve for the alias's parameter values.
     *
     * <p>Returns empty if the concrete type is incompatible with the alias's
     * underlying type structure.
     *
     * @param alias the alias type template
     * @param concrete the concrete type to match against
     * @return solved parameter map, or empty if unsolvable
     */
    public Optional<Map<String, Object>> evaluateReverse(RAliasType alias, RType concrete) {
        RType underlying = evaluateForward(alias.refersTo());

        // Simple case: no parameters — just check compatibility
        if (alias.arguments().isEmpty()) {
            if (underlying.equals(concrete)) {
                return Optional.of(Map.of());
            }
            return Optional.empty();
        }

        // Parametric case: match concrete type against underlying template
        if (underlying instanceof RNumberType && concrete instanceof RNumberType concreteNum) {
            return solveNumberParams(alias, concreteNum);
        }
        if (underlying instanceof RStringType && concrete instanceof RStringType concreteStr) {
            return solveStringParams(alias, concreteStr);
        }

        return Optional.empty();
    }

    /**
     * Solves number type parameters by extracting constraint values from
     * the concrete type based on the alias's argument-to-field mapping.
     *
     * <p>Convention: alias arguments map parameter names to field names.
     * E.g., {@code Map.of("n", "digits")} means parameter "n" maps to
     * the "digits" field of the number type.
     */
    private Optional<Map<String, Object>> solveNumberParams(
            RAliasType alias, RNumberType concrete) {
        Map<String, Object> solved = new LinkedHashMap<>();
        for (var entry : alias.arguments().entrySet()) {
            String paramName = entry.getKey();
            Object fieldRef = entry.getValue();
            if (!(fieldRef instanceof String fieldName)) continue;

            OptionalInt value = switch (fieldName) {
                case "digits" -> concrete.digits();
                case "fractionalDigits" -> concrete.fractionalDigits();
                default -> OptionalInt.empty();
            };
            if (value.isPresent()) {
                solved.put(paramName, value.getAsInt());
            } else {
                return Optional.empty(); // can't solve — field not constrained
            }
        }
        return Optional.of(Map.copyOf(solved));
    }

    private Optional<Map<String, Object>> solveStringParams(
            RAliasType alias, RStringType concrete) {
        Map<String, Object> solved = new LinkedHashMap<>();
        for (var entry : alias.arguments().entrySet()) {
            String paramName = entry.getKey();
            Object fieldRef = entry.getValue();
            if (!(fieldRef instanceof String fieldName)) continue;

            OptionalInt value = switch (fieldName) {
                case "minLength" -> concrete.minLength();
                case "maxLength" -> concrete.maxLength();
                default -> OptionalInt.empty();
            };
            if (value.isPresent()) {
                solved.put(paramName, value.getAsInt());
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(Map.copyOf(solved));
    }

    // === evaluateAliasBody (P2.1.3) ==========================================

    /**
     * Evaluates an {@link RTypeAlias}'s body type-call arguments and constructs
     * the parametric underlying {@link RType}. Handles literal values (e.g.
     * {@code fractionalDigits: 0}) and references to alias parameters (e.g.
     * {@code digits: digits}) which resolve to bound values from the use-site
     * call's arguments or remain unbound.
     *
     * <p>Fix for P2.1.3 Cluster F regression — {@code int (1..N)} attributes
     * were resolving to {@code BigDecimal} because the previous bridge in
     * {@code GeneratorModel.astNodeToRType(RTypeAlias)} called
     * {@code BUILTINS.lookup("number")} without evaluating the alias body's
     * literal {@code fractionalDigits: 0}, leaving the underlying
     * {@link RNumberType} with empty fractional digits and thus failing
     * {@link RNumberType#isInteger()}.
     *
     * @param alias the alias AST node (e.g. {@code int(digits, min, max)})
     * @param useSiteCall the call site (e.g. {@code int} bare, or {@code int(digits: 9)})
     * @return the parametric underlying RType, or {@code RMissingType.INSTANCE}
     *         for malformed aliases (no body, unknown base type name)
     */
    public RType evaluateAliasBody(RTypeAlias alias, RTypeCall useSiteCall) {
        Map<String, ParsedLiteral> bindings = buildBindings(useSiteCall);
        return evaluateBodyWithBindings(alias.typeCall(), bindings, 0);
    }

    /**
     * Build alias-param → ParsedLiteral bindings from a use-site call's arguments.
     * Only literal-valued arguments contribute bindings (NameValue at the use-site
     * would be a parser-level forward reference; currently unsupported).
     */
    private Map<String, ParsedLiteral> buildBindings(RTypeCall useSiteCall) {
        Map<String, ParsedLiteral> bindings = new LinkedHashMap<>();
        if (useSiteCall == null) return bindings;
        for (var arg : useSiteCall.arguments()) {
            var valueExpr = arg.value();
            if (valueExpr != null && valueExpr.literalValue().isPresent()) {
                bindings.put(arg.parameterName(),
                        parseLiteral(valueExpr.literalValue().get(), valueExpr.isNegated()));
            }
        }
        return bindings;
    }

    /**
     * Recursively evaluate an alias body. If the body type-call refers to another
     * {@link RTypeAlias}, recurse with the body's resolved arguments as the new
     * bindings. Otherwise look up the base builtin and construct a parametric type.
     *
     * <p>Depth-bounded by {@link #MAX_DEPTH} to prevent infinite loops on cyclic
     * alias declarations.
     */
    private RType evaluateBodyWithBindings(RTypeCall body, Map<String, ParsedLiteral> bindings, int depth) {
        if (body == null) return RMissingType.INSTANCE;
        if (depth >= MAX_DEPTH) return RMissingType.INSTANCE;

        // Resolve body arguments against current bindings
        Map<String, ParsedLiteral> resolved = new LinkedHashMap<>();
        for (var arg : body.arguments()) {
            var paramName = arg.parameterName();
            var valueExpr = arg.value();
            if (valueExpr == null) continue;

            if (valueExpr.literalValue().isPresent()) {
                resolved.put(paramName,
                        parseLiteral(valueExpr.literalValue().get(), valueExpr.isNegated()));
            } else if (valueExpr.nameValue().isPresent()) {
                var aliasParam = valueExpr.nameValue().get();
                if (bindings.containsKey(aliasParam)) {
                    resolved.put(paramName, bindings.get(aliasParam));
                }
                // else: unbound — leave absent in resolved map
            }
        }

        // If body refers to another RTypeAlias, recurse with resolved args as
        // new bindings (this handles CDM alias chains like
        // BusinessCenter → FpMLCodingScheme → string).
        // Theoretical: a typeAlias body could resolve to a data type / enum
        // (no observed CDM/DRR/ISO/rune-fpml corpus typeAlias declarations
        // exercise these cases; bodies in basictypes.rosetta + CDM derived
        // aliases all target builtins or other aliases). Handle defensively
        // per Copilot PR #64 R2 F3 2026-05-13. RChoice is intentionally not
        // handled here (RChoiceTypeRef construction requires the choice's
        // option-types list which is not derivable from the typeAlias body
        // alone; defer to P2.1.3b).
        var bodyResolved = body.referencedType();
        if (bodyResolved.isPresent()) {
            var node = bodyResolved.get();
            if (node instanceof RTypeAlias innerAlias) {
                return evaluateBodyWithBindings(innerAlias.typeCall(), resolved, depth + 1);
            }
            if (node instanceof RDataType dt) {
                return new RDataTypeRef(dt);
            }
            if (node instanceof REnumeration en) {
                return new REnumTypeRef(en);
            }
            // v3.2 seat 9 (D47, F8): the alias BODY names a MODEL-declared basicType / recordType
            // (`typeAlias TokenAlias: etoken`, the oracle group void-mapping-basic-record-edge's
            // AliasCarrier) — ONE law with the two astNodeToRTypes (BuiltinTypeRegistry
            // .basicOrRecordNodeType, LAW 69): `nothing`, Void at the Java seat, where the by-name
            // lookup below answered MISSING → Object. A builtins-model body (`number(digits: 3)`) keeps
            // that by-name path, which applies the body's resolved arguments over the twin — the law's
            // twin verdict IS the base it looks up, so only the `nothing` verdict returns here.
            var declared = BUILTINS.basicOrRecordNodeType(node);
            if (declared.isPresent() && declared.get() == RBasicType.NOTHING) {
                return RBasicType.NOTHING;
            }
            // Unhandled AST node type (e.g., RChoice) — fall through to
            // BUILTINS lookup; if typeName isn't a builtin, returns
            // RMissingType, which is correct for un-modellable cases.
        }

        // Otherwise look up the base type by name
        var baseType = BUILTINS.lookup(body.typeName()).orElse(RMissingType.INSTANCE);
        if (baseType instanceof RMissingType) {
            return baseType;
        }

        // Construct parametric underlying type from base + resolved args.
        // Merge resolved args with the builtin's pre-existing constraints so
        // {@code int}'s fractionalDigits=0 survives alias bodies that don't
        // restate it (e.g. {@code typeAlias positiveInt: int(min: 0)}). Without
        // the merge, the rebuild drops fractionalDigits and {@code isInteger()}
        // returns false, routing the alias to BigDecimal instead of Integer in
        // downstream Java codegen — the exact regression Cluster F was opened
        // to fix (per Copilot PR #64 R7 F17 2026-05-13).
        //
        // Surface present-but-unparseable args explicitly: if a slot is provided
        // in the alias body but the literal doesn't fit the slot's expected type
        // (e.g. {@code digits: 9999999999999999} overflows int; {@code min: "abc"}
        // is non-numeric for BigDecimal), return {@code RMissingType.INSTANCE}
        // rather than silently falling back to the base's constraint. Without
        // this validation the user's stated intent is dropped without notice
        // (per Copilot PR #64 R11 F51 2026-05-14).
        if (baseType instanceof RNumberType base) {
            if (hasPresentInvalid(resolved, "digits", /*intSlot*/ true)
                    || hasPresentInvalid(resolved, "fractionalDigits", /*intSlot*/ true)
                    || hasPresentInvalid(resolved, "min", /*intSlot*/ false)
                    || hasPresentInvalid(resolved, "max", /*intSlot*/ false)) {
                return RMissingType.INSTANCE;
            }
            OptionalInt rDigits = asOptionalInt(resolved, "digits");
            OptionalInt rFrac = asOptionalInt(resolved, "fractionalDigits");
            return new RNumberType(
                    rDigits.isPresent() ? rDigits : base.digits(),
                    rFrac.isPresent() ? rFrac : base.fractionalDigits(),
                    asOptionalBigDecimal(resolved, "min").or(base::min),
                    asOptionalBigDecimal(resolved, "max").or(base::max)
            );
        }

        // RStringType reconstruction is P2.1.3b scope (pattern slot expects
        // an Optional<java.util.regex.Pattern> requiring full regex
        // compilation; this branch refers to the JDK regex API only by name
        // and does not itself execute regex against structured language
        // content). No string alias in basictypes.rosetta currently passes
        // typeCall args
        // that constrain string parameters, so this branch isn't reached for
        // current corpora. For string base with no body args (e.g.
        // productType: string), return the unconstrained baseType.
        // If string-constraint args ARE present (pattern / minLength /
        // maxLength), return RMissingType to surface the unsupported case
        // explicitly rather than silently dropping the constraints
        // (per Copilot PR #64 R2 F1 2026-05-13).
        if (baseType instanceof RStringType) {
            if (resolved.containsKey("pattern")
                    || resolved.containsKey("minLength")
                    || resolved.containsKey("maxLength")) {
                return RMissingType.INSTANCE;
            }
            return baseType;
        }

        // Unknown base or non-parametric — return as-is
        return baseType;
    }

    /**
     * Applies an INLINE (direct, non-alias) type call's literal arguments over its
     * builtin base type — the use-site analogue of the alias-body merge in
     * {@code evaluateBodyWithBindings}. An attribute declared
     * {@code number(digits: 10, fractionalDigits: 0)} carries its parameters on the
     * type call itself; without this application the builtin resolution returns the
     * BARE {@code number} (empty fractionalDigits), {@link RNumberType#isInteger()}
     * fails, and Java codegen renders {@code BigDecimal} where upstream ladders
     * Integer/Long/BigInteger (W42 leg-C finding #2 — the {@code pojo-number-ladder}
     * oracle group is the byte witness).
     *
     * <p>Scope: {@link RNumberType} bases only — digits/fractionalDigits/min/max
     * merge over the base's own constraints (args win, the base fills absent slots,
     * so a direct {@code int(digits: 3)} keeps {@code int}'s fractionalDigits: 0).
     * A present-but-unparseable arg returns {@link RMissingType#INSTANCE} (the
     * alias-body law above — fail loud, never silently drop stated intent). Every
     * other base (string included) returns UNCHANGED: a constrained string projects
     * to {@code String} either way, and an {@code RMissingType} here would regress
     * inline {@code string(...)} attributes to {@code Object}. Argless calls
     * short-circuit to the base; non-literal args merge nothing (no parameters
     * are in scope at a direct use-site — the alias-body law's unbound-nameValue
     * behavior, kept for consistency).
     */
    public RType applyDirectTypeCallArguments(RType baseType, RTypeCall call) {
        if (!(baseType instanceof RNumberType base) || call == null
                || call.arguments().isEmpty()) {
            return baseType;
        }
        Map<String, ParsedLiteral> resolved = new LinkedHashMap<>();
        for (var arg : call.arguments()) {
            var valueExpr = arg.value();
            if (valueExpr != null && valueExpr.literalValue().isPresent()) {
                resolved.put(arg.parameterName(),
                        parseLiteral(valueExpr.literalValue().get(), valueExpr.isNegated()));
            }
        }
        if (resolved.isEmpty()) {
            return baseType;
        }
        if (hasPresentInvalid(resolved, "digits", /*intSlot*/ true)
                || hasPresentInvalid(resolved, "fractionalDigits", /*intSlot*/ true)
                || hasPresentInvalid(resolved, "min", /*intSlot*/ false)
                || hasPresentInvalid(resolved, "max", /*intSlot*/ false)) {
            return RMissingType.INSTANCE;
        }
        OptionalInt rDigits = asOptionalInt(resolved, "digits");
        OptionalInt rFrac = asOptionalInt(resolved, "fractionalDigits");
        return new RNumberType(
                rDigits.isPresent() ? rDigits : base.digits(),
                rFrac.isPresent() ? rFrac : base.fractionalDigits(),
                asOptionalBigDecimal(resolved, "min").or(base::min),
                asOptionalBigDecimal(resolved, "max").or(base::max)
        );
    }

    /**
     * Sentinel record for parametric arg values. Replaces mixed-type
     * {@code Object}-keyed maps with type-consistent slots.
     */
    private record ParsedLiteral(OptionalInt optInt,
                                 Optional<BigDecimal> optDecimal,
                                 String raw) {}

    /**
     * Parses a literal token. Tries {@link Integer#parseInt(String)} first;
     * on overflow or non-integer format, falls back to {@link BigDecimal}.
     */
    private ParsedLiteral parseLiteral(String raw, boolean negated) {
        String text = negated ? "-" + raw : raw;
        try {
            int n = Integer.parseInt(text);
            return new ParsedLiteral(OptionalInt.of(n),
                    Optional.of(new BigDecimal(text)), raw);
        } catch (NumberFormatException e) {
            try {
                var bd = new BigDecimal(text);
                return new ParsedLiteral(OptionalInt.empty(), Optional.of(bd), raw);
            } catch (NumberFormatException e2) {
                return new ParsedLiteral(OptionalInt.empty(), Optional.empty(), raw);
            }
        }
    }

    private OptionalInt asOptionalInt(Map<String, ParsedLiteral> map, String key) {
        var v = map.get(key);
        if (v == null) return OptionalInt.empty();
        return v.optInt();
    }

    private Optional<BigDecimal> asOptionalBigDecimal(Map<String, ParsedLiteral> map, String key) {
        var v = map.get(key);
        if (v == null) return Optional.empty();
        return v.optDecimal();
    }

    /**
     * Returns {@code true} when the slot is present in the resolved map BUT the
     * parsed literal failed to fit the expected slot type (int vs BigDecimal).
     * Distinguishes "absent argument" (returns false) from "present argument
     * with unparseable/overflow literal" (returns true). Used to surface
     * present-but-unparseable args as {@link RMissingType#INSTANCE} explicitly
     * instead of silently falling back to the base's constraint
     * (per Copilot PR #64 R11 F51 2026-05-14).
     */
    private boolean hasPresentInvalid(Map<String, ParsedLiteral> map, String key, boolean intSlot) {
        var v = map.get(key);
        if (v == null) return false; // absent — not invalid
        return intSlot ? v.optInt().isEmpty() : v.optDecimal().isEmpty();
    }
}
