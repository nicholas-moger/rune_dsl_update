package com.regnosys.rosetta.generator.java.object.validators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.builder.AstBuilderHelper;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgument;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;

/**
 * Resolves a declared type call (an attribute's or choice option's type) to its
 * type-format CONSTRAINT ENVELOPE — the fully-parametric {@link RStringType} /
 * {@link RNumberType} after alias stripping, mirroring upstream 9.83
 * {@code TypeSystem.stripFromTypeAliases} semantics: alias chains are walked with
 * parameter substitution ({@code typeAlias int(digits int, min int, max int):
 * number(digits: digits, fractionalDigits: 0, min: min, max: max)}), and BOTH
 * alias-body and inline use-site constraint arguments contribute
 * ({@code string(minLength: 1, maxLength: 52, pattern: "…")} directly on an
 * attribute — drr/iso/fpml carry these; cdm's only constrained basics are
 * {@code int}-typed).
 *
 * <p>FAMILY-OWNED BY DESIGN (the D44 ringfence rule — consume, never modify shared
 * services): the byte-proven shared resolution path
 * ({@code GeneratorModel.resolveTypeCall} → {@code TypeAliasSolver}) deliberately
 * drops string-alias constraints (its {@code RStringType} branch returns
 * {@code RMissingType}, recovered to the UNCONSTRAINED base — sufficient for the
 * POJO byte contract, which needs only the Java type) and drops inline use-site
 * args in the builtin-lookup branch. The type-format family needs the envelope the
 * shared path discards, so it resolves it here without touching Tier-1/Tier-2
 * files. The D11 gate pins both paths against the same goldens.
 *
 * <p>Only string/number envelopes are constructed; every other resolution (data
 * types, enums, choices, records, unconstrained basics) yields
 * {@link Optional#empty()} — "no type-format check", matching upstream's
 * constrained-string-or-number filter.
 *
 * <p><b>RECONCILE SEAM (PR #644, the property gate).</b> This class and its two entry points
 * ({@link #constrainedBasic} and {@link #aliasHierarchy}) are {@code public} so the IR route's derived-facts
 * reconciler ({@code rune-ir-java}, {@code com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconciler}) can read
 * THIS scan's own answer as the source half of the {@code typeFormat.envelope} family, and can hold its OWN
 * independent walk of the alias chain against {@link #aliasHierarchy}'s (LAW 69 - two producers). Visibility ONLY: no
 * body, no signature and no call site moved, and no emission path calls the seam.
 */
public final class TypeFormatConstraintScan {

    private static final int MAX_DEPTH = 100;

    private static final BuiltinTypeRegistry BUILTINS = BuiltinTypeRegistry.createDefault();

    private TypeFormatConstraintScan() {
    }

    /**
     * The constraint envelope for a declared type call, if it resolves (through
     * any alias chain) to a CONSTRAINED string or number.
     */
    public static Optional<RType> constrainedBasic(RTypeCall typeCall, RWorkspace workspace) {
        RType resolved = evaluate(typeCall, Map.of(), workspace, 0);
        if (resolved instanceof RStringType st
                && (st.minLength().isPresent() || st.maxLength().isPresent() || st.pattern().isPresent())) {
            return Optional.of(st);
        }
        if (resolved instanceof RNumberType nt
                && (nt.digits().isPresent() || nt.fractionalDigits().isPresent()
                        || nt.min().isPresent() || nt.max().isPresent())) {
            return Optional.of(nt);
        }
        return Optional.empty();
    }

    /**
     * v3.2 seat 3 (census family F9): the ordered ALIAS HIERARCHY a declared type call walks
     * through to its underlying type — upstream {@code TypeSystem.computeAliasHierarchy}
     * (every {@code RAliasType} followed, outermost first; empty when the call names no
     * alias). The type-format validator reads the conditions each alias carries from it
     * (upstream {@code aliasHierarchyPerAttribute.values.flatMap[aliases].flatMap[conditions]});
     * the constraint envelope above stays the SEPARATE question it always was. Resolution
     * goes through the generator's workspace exactly as {@link #evaluate} does, and the
     * walk is bounded by the same {@link #MAX_DEPTH} (a cyclic alias chain ends the list
     * rather than the JVM).
     */
    public static List<RTypeAlias> aliasHierarchy(RTypeCall typeCall, RWorkspace workspace) {
        List<RTypeAlias> hierarchy = new ArrayList<>();
        RTypeCall call = typeCall;
        int depth = 0;
        while (call != null && depth++ < MAX_DEPTH) {
            RNode node = call.referencedTypeId()
                    .map(workspace::resolveTypeLike)
                    .orElse(null);
            if (!(node instanceof RTypeAlias alias) || hierarchy.contains(alias)) {
                break;
            }
            hierarchy.add(alias);
            call = alias.typeCall();
        }
        return hierarchy;
    }

    /**
     * Evaluate a type call against the enclosing alias's parameter bindings
     * (empty at an attribute use-site). Mirrors
     * {@code TypeAliasSolver.evaluateBodyWithBindings}' recursion shape: resolve
     * this call's arguments against {@code bindings}, then either recurse into a
     * referenced alias's body with the resolved arguments as the new bindings, or
     * construct the parametric basic type from the builtin base + the resolved
     * arguments.
     */
    private static RType evaluate(RTypeCall call, Map<String, ParsedArg> bindings,
                                  RWorkspace workspace, int depth) {
        if (call == null || depth >= MAX_DEPTH) {
            return RMissingType.INSTANCE;
        }
        Map<String, ParsedArg> resolved = new LinkedHashMap<>();
        for (RTypeCallArgument arg : call.arguments()) {
            var valueExpr = arg.value();
            if (valueExpr == null) {
                continue;
            }
            if (valueExpr.literalValue().isPresent()) {
                resolved.put(arg.parameterName(),
                        parse(valueExpr.literalValue().get(), valueExpr.isNegated()));
            } else if (valueExpr.nameValue().isPresent()) {
                ParsedArg bound = bindings.get(valueExpr.nameValue().get());
                if (bound != null) {
                    resolved.put(arg.parameterName(), bound);
                }
                // unbound parameter reference — the slot stays absent
            }
        }
        // Resolve via the generator's workspace (not the node-attached one) so
        // detached copies still resolve — the same discipline as
        // GeneratorModel.resolveTypeCall.
        RNode node = call.referencedTypeId()
                .map(workspace::resolveTypeLike)
                .orElse(null);
        if (node instanceof RTypeAlias alias) {
            return evaluate(alias.typeCall(), resolved, workspace, depth + 1);
        }
        // Base case: a builtin basic type (the `basicType string/number` builtin
        // model node, or an unresolved builtin name). Construct the parametric
        // type from the base merged with the resolved args; non-basic names
        // (data types, enums, choices, records) miss the registry and yield
        // RMissingType — correctly "no envelope".
        RType base = BUILTINS.lookup(call.typeName()).orElse(RMissingType.INSTANCE);
        if (base instanceof RNumberType baseNum) {
            return new RNumberType(
                    intArg(resolved, "digits").isPresent() ? intArg(resolved, "digits") : baseNum.digits(),
                    intArg(resolved, "fractionalDigits").isPresent()
                            ? intArg(resolved, "fractionalDigits") : baseNum.fractionalDigits(),
                    decimalArg(resolved, "min").or(baseNum::min),
                    decimalArg(resolved, "max").or(baseNum::max));
        }
        if (base instanceof RStringType baseStr) {
            OptionalInt minLength = intArg(resolved, "minLength").isPresent()
                    ? intArg(resolved, "minLength") : baseStr.minLength();
            OptionalInt maxLength = intArg(resolved, "maxLength").isPresent()
                    ? intArg(resolved, "maxLength") : baseStr.maxLength();
            Optional<Pattern> pattern = patternArg(resolved).or(baseStr::pattern);
            return new RStringType(minLength, maxLength, pattern);
        }
        return base;
    }

    /**
     * One parsed constraint argument. String literals keep their unescaped text
     * (quotes stripped via the parser's own {@link AstBuilderHelper#stripQuotes}
     * so pattern round-trips match upstream: DSL escape → compile →
     * {@code Pattern.toString()} → {@code escapeJava} reproduces the source
     * escaping); numeric literals parse to int and/or BigDecimal.
     */
    private record ParsedArg(OptionalInt asInt, Optional<BigDecimal> asDecimal, String asString) {
    }

    private static ParsedArg parse(String rawLiteral, boolean negated) {
        String text = negated ? "-" + rawLiteral : rawLiteral;
        String unquoted = AstBuilderHelper.stripQuotes(rawLiteral);
        OptionalInt asInt = OptionalInt.empty();
        Optional<BigDecimal> asDecimal = Optional.empty();
        try {
            // stripTrailingZeros mirrors upstream's RosettaNumber constructor
            // (rune-runtime RosettaNumber: `this.value = value.stripTrailingZeros()`),
            // which every upstream interval bound round-trips through — golden
            // renders `max: 100` as `new BigDecimal("1E+2")` (hold-out witness
            // Foo2TypeFormatValidator, PR #410). Identity on the frozen corpus:
            // its only bound literals are "1"/"0"/"-1" (census at #410).
            asDecimal = Optional.of(new BigDecimal(text).stripTrailingZeros());
            try {
                asInt = OptionalInt.of(Integer.parseInt(text));
            } catch (NumberFormatException ignored) {
                // decimal-only literal (or int overflow) — the int slot stays absent
            }
        } catch (NumberFormatException ignored) {
            // non-numeric literal (a pattern string) — numeric slots stay absent
        }
        return new ParsedArg(asInt, asDecimal, unquoted);
    }

    private static OptionalInt intArg(Map<String, ParsedArg> args, String key) {
        ParsedArg v = args.get(key);
        return v == null ? OptionalInt.empty() : v.asInt();
    }

    private static Optional<BigDecimal> decimalArg(Map<String, ParsedArg> args, String key) {
        ParsedArg v = args.get(key);
        return v == null ? Optional.empty() : v.asDecimal();
    }

    private static Optional<Pattern> patternArg(Map<String, ParsedArg> args) {
        ParsedArg v = args.get("pattern");
        if (v == null || v.asString() == null) {
            return Optional.empty();
        }
        try {
            // ci-allowlist: regex-on-structured-content (compiles the model's own
            // declared pattern literal for re-emission into generated Java — no
            // structural analysis of language content)
            return Optional.of(Pattern.compile(v.asString()));
        } catch (PatternSyntaxException e) {
            throw new IllegalStateException(
                    "Unparseable type-format pattern literal: " + v.asString(), e);
        }
    }
}
