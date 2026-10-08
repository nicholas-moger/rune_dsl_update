package com.regnosys.rosetta.generator.java.ir;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RRecordType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.rosetta.util.DottedPath;

/**
 * THE EMITTER-SIDE JAVA TYPE (v3.3 seat 7, PR #643 - the type gate): the canonical Java REFERENCE type name of a
 * declared type reference, derived from THE IR FACTS ALONE - the reference's {@link IRType#kind()}, its
 * {@link IRType#namespace()} and {@link IRType#resolvedName()}, the use-site {@link IRTypeArgument}s, and, for a
 * {@code typeAlias} reference, its collapsed {@link IREffectiveBase}. Boxed throughout ({@code java.lang.Integer},
 * never {@code int}), because an IR-routed emitter types a field, and a field is a reference.
 *
 * <p>NO AST NODE, NO {@code GeneratorModel}, NO {@code JavaTypeTranslator} is read here. WHAT IS SHARED, said
 * plainly, because a shared producer is a producer both halves of the D11 reconcile inherit rather than agree on:
 * <ul>
 *   <li>{@link JavaPackageName#escape} and {@link DottedPath} - PURE STRING FUNCTIONS over a namespace the IR
 *       already carries (segment escaping of a Java keyword, dotted join). They decide no type; they spell one.</li>
 *   <li>{@link BuiltinTypeRegistry#createDefault()} - a PURE TABLE of the builtin types ({@code number}, {@code int},
 *       {@code string}, {@code boolean}, {@code time}, {@code pattern}, {@code nothing}, {@code any}, {@code date},
 *       {@code dateTime}, {@code zonedDateTime}) keyed by NAME, the same registry the adapter's own L1 fallback
 *       reads. It answers "what does the name {@code int} constrain", not "what does this reference resolve to".</li>
 * </ul>
 *
 * <p>THE LAW is the old generator's D4 table ({@code JavaTypeTranslator}), prototyped and MEASURED at the seat's
 * probe: the derivation agreed with {@code JavaTypeTranslator.toJavaReferenceType(GeneratorModel.resolveTypeCall(...))}
 * on 85,927 / 85,927 references over the 26 corpus cells, 0 disagreements. The D11 declaration reconcile asserts that
 * equality per reference, per cell, on every ring.
 *
 * <p>WHAT IT REFUSES, by name, rather than guessing: an unresolved reference (the old generator's three UNLINKED
 * workspace fallbacks - the three-entry alias table, the simple-name first match and the alias search - type a name
 * the linker never resolved and are REFUSED here; its post-resolution {@code RMissingType} bypass answers a name the
 * linker DID resolve, so this derivation types that declaration and the reconciler's {@code .kind} /
 * {@code .legacyDeclaration} facts are what catch the bypass - PR #643 round 1, spec MF-1), an alias
 * chain that did not collapse, a kind that names no value type ({@code META_TYPE}, {@code FIELD}, ...), the
 * {@code pattern} basic type (which the D4 table has no Java mapping for), and an argument the model states but
 * neither {@code Integer.parseInt} nor {@link BigDecimal} can read. Every refusal is a {@link Refusal} - a
 * {@link GenerationException} whose message starts {@code "IR type gate: "}, NAMES the reference and carries its
 * {@link Reason}, so the reconcile's {@code .javaType} fact reads {@code REFUSED:<reason>} rather than one folded token
 * (PR #644, the banked cq NIT-3 of #643: two refusals for different reasons must not read EQUAL).
 */
public final class IRJavaTypeNames {

    /** The shared builtin table - a pure by-NAME lookup, the same registry the adapter's L1 fallback reads. */
    private static final BuiltinTypeRegistry BUILTINS = BuiltinTypeRegistry.createDefault();

    /** The default digit count of an integer-grained number the model left unconstrained (the D4 table's own). */
    private static final int DEFAULT_DIGITS = 9;

    /** The prefix of every refusal token the reconcile's {@code .javaType} fact carries: {@code REFUSED:<reason>}. */
    static final String REFUSED = "REFUSED:";

    /**
     * WHY a derivation refused (PR #644). Each constant is ONE token of the reconcile's {@code .javaType} fact; the two
     * that the OLD GENERATOR can also refuse for - {@link #PATTERN} and {@link #ESCAPE} - are classed on its side by
     * {@link #legacyRefusalToken} from its own throw sites, so a paired refusal reads the same token on both halves and
     * an unpaired one (the IR refuses where the old generator invents a type, or the reverse) reads RED by name.
     */
    public enum Reason {
        /** A reference the linker never resolved - the three unlinked workspace fallbacks are refused. */
        UNRESOLVED("unresolved"),
        /** A namespace segment no {@code _} prefix makes a Java identifier ({@code JavaPackageName.escape} throws). */
        ESCAPE("escape"),
        /** The {@code pattern} basic type - grammar-level only, no Java mapping in the D4 table. */
        PATTERN("pattern"),
        /** An argument the model states that neither {@code Integer.parseInt} nor {@code BigDecimal} can read. */
        ARGUMENT("argument"),
        /** An alias reference whose chain the adapter could not end - no effective base to type. */
        COLLAPSE("collapse"),
        /** A kind that names no value type at all ({@code META_TYPE}, {@code FIELD}, ...). */
        NO_VALUE_TYPE("no-value-type"),
        /** A builtin, record or basic type the D4 table maps to nothing. */
        NO_JAVA_TYPE("no-java-type");

        private final String token;

        Reason(String token) {
            this.token = token;
        }

        /** The {@code REFUSED:<reason>} token the reconcile's fact carries. */
        public String token() {
            return REFUSED + token;
        }
    }

    /** A named refusal of the derivation: the message names the reference, {@link #reason()} says why. */
    public static final class Refusal extends GenerationException {
        private static final long serialVersionUID = 1L;
        private final Reason reason;

        Refusal(Reason reason, String message, Throwable cause) {
            super(message, null, null, cause);
            this.reason = Objects.requireNonNull(reason, "reason");
        }

        public Reason reason() {
            return reason;
        }
    }

    private IRJavaTypeNames() {
    }

    /**
     * THE OLD GENERATOR'S refusal token, classed from ITS OWN throw sites - no message is read (PR #644). Its
     * {@code JavaTypeTranslator.toJavaReferenceType} refuses at exactly two sites a RESOLVED reference reaches: the
     * {@code pattern} basic type ({@code caseBasicType}, an {@code IllegalStateException} when the resolved leaf - through
     * every {@code RAliasType} - IS {@code RBasicType.PATTERN}) and a namespace segment {@code JavaPackageName.escape}
     * cannot make an identifier (the other {@code IllegalStateException}; the translator's "Unknown RBasicType" arm is
     * unreachable over the sealed set). Anything else it throws is classed by its exception's simple name, so it can
     * never read equal to a reason the IR side names - an unforeseen pairing is RED, never silently GREEN.
     *
     * @param legacy  the old generator's own resolved type ({@code GeneratorModel.resolveTypeCall})
     * @param refused what {@code toJavaReferenceType(legacy)} threw
     */
    static String legacyRefusalToken(RType legacy, RuntimeException refused) {
        RType leaf = legacy;
        for (int depth = 0; leaf instanceof RAliasType alias && depth < 100; depth++) {
            leaf = alias.refersTo();
        }
        if (leaf == RBasicType.PATTERN) {
            return Reason.PATTERN.token();
        }
        if (refused instanceof IllegalStateException) {
            return Reason.ESCAPE.token();
        }
        return REFUSED + "legacy-" + refused.getClass().getSimpleName();
    }

    /**
     * The canonical Java reference type name of {@code reference}, derived from the IR facts alone.
     *
     * @param reference        a type REFERENCE (a field's type, an option's type, a parameter's type, an alias body)
     * @param useSiteArguments the arguments the model wrote AT THIS USE SITE ({@code number(digits: 19)}); applied
     *                         over a BASIC_TYPE / RECORD_TYPE reference only - a TYPE_ALIAS reference has them
     *                         substituted into its effective base already, and applying them twice would be a lie
     * @return the boxed canonical name, e.g. {@code java.lang.Integer}, {@code java.math.BigDecimal},
     *     {@code com.rosetta.model.lib.records.Date}, {@code cdm.base.datetime.AdjustableDate}
     * @throws GenerationException when the facts do not determine a Java type - see the class javadoc's refusals
     */
    public static String of(IRType reference, List<IRTypeArgument> useSiteArguments) {
        Objects.requireNonNull(reference, "reference");
        List<IRTypeArgument> arguments = useSiteArguments == null ? List.of() : useSiteArguments;
        IRKind kind = reference.kind();
        switch (kind) {
            case STRUCT:
            case CHOICE:
            case ENUM:
                return qualify(reference, reference.namespace(), reference.resolvedName());
            case BASIC_TYPE:
            case RECORD_TYPE:
                return builtin(reference, reference.resolvedName().orElseThrow(() -> unresolved(reference)), arguments);
            case TYPE_ALIAS:
                return ofAlias(reference);
            default:
                throw refuse(Reason.NO_VALUE_TYPE, "a reference of kind " + kind + " names no value type: '" + reference.name() + "'");
        }
    }

    /**
     * A {@code typeAlias} reference types as its COLLAPSED CHAIN: the alias name never reaches a generated byte, the
     * base does. The effective base's arguments are the use-site ones ALREADY SUBSTITUTED - they are not applied again.
     */
    private static String ofAlias(IRType reference) {
        IREffectiveBase base = reference.effectiveBase()
                .orElseThrow(() -> refuse(Reason.COLLAPSE, "the alias chain of '" + reference.name() + "' did not collapse"
                        + " - a reference whose chain the adapter could not end has no Java type"));
        switch (base.kind()) {
            case STRUCT:
            case CHOICE:
            case ENUM:
                return qualify(reference, base.namespace(), Optional.of(base.name()));
            default:
                return builtin(reference, base.name(), base.arguments());
        }
    }

    /**
     * A declared type's Java name: the declaring namespace escaped segment by segment (a segment that is a Java
     * keyword takes the {@code _} prefix - {@code fpml.new.x} spells {@code fpml._new.x}), then the declaration's own
     * simple name. An EMPTY namespace spells the bare name. An absent namespace or name is the unresolved refusal.
     */
    private static String qualify(IRType reference, Optional<String> namespace, Optional<String> simpleName) {
        if (namespace.isEmpty() || simpleName.isEmpty()) {
            throw unresolved(reference);
        }
        String ns = namespace.get();
        if (ns.isEmpty()) {
            return simpleName.get();
        }
        try {
            return JavaPackageName.escape(DottedPath.splitOnDots(ns)).getName().child(simpleName.get()).withDots();
        } catch (IllegalStateException cannotEscape) {
            // a segment no `_` prefix makes a Java identifier: the old generator throws at the same site; here it is a
            // NAMED refusal, so both halves of the reconcile read REFUSED:escape rather than one throwing past the other
            throw new Refusal(Reason.ESCAPE, "IR type gate: the namespace '" + ns + "' of '" + reference.name()
                    + "' cannot be escaped to a Java package - " + cannotEscape.getMessage(), cannotEscape);
        }
    }

    /**
     * A builtin leaf by NAME, with the arguments in force over it. A name the registry does not know is a
     * MODEL-declared basic / record type, which the released 9.83.0 plugin types {@code nothing} and so renders
     * {@code java.lang.Void} at every seat.
     */
    private static String builtin(IRType reference, String name, List<IRTypeArgument> arguments) {
        Optional<RType> looked = BUILTINS.lookup(name);
        if (looked.isEmpty()) {
            return "java.lang.Void";
        }
        RType base = looked.get();
        if (base instanceof RNumberType numberType) {
            return ladder(reference, numberType, arguments);
        }
        if (base instanceof RStringType) {
            return "java.lang.String";
        }
        if (base instanceof RRecordType recordType) {
            switch (recordType.name()) {
                case "date":
                    return "com.rosetta.model.lib.records.Date";
                case "dateTime":
                    return "java.time.LocalDateTime";
                case "zonedDateTime":
                    return "java.time.ZonedDateTime";
                default:
                    throw refuse(Reason.NO_JAVA_TYPE, "the record type '" + recordType.name() + "' has no Java type ('" + reference.name() + "')");
            }
        }
        if (base instanceof RBasicType basic) {
            switch (basic.name()) {
                case "boolean":
                    return "java.lang.Boolean";
                case "time":
                    return "java.time.LocalTime";
                case "nothing":
                    return "java.lang.Void";
                case "any":
                    return "java.lang.Object";
                case "pattern":
                    throw refuse(Reason.PATTERN, "pattern has no Java type - it is grammar-level only ('" + reference.name() + "')");
                default:
                    throw refuse(Reason.NO_JAVA_TYPE, "the basic type '" + basic.name() + "' has no Java type ('" + reference.name() + "')");
            }
        }
        throw refuse(Reason.NO_JAVA_TYPE, "the builtin '" + name + "' has no Java type ('" + reference.name() + "')");
    }

    /**
     * THE NUMBER LADDER: the registry type's own constraints are the seed ({@code int} carries
     * {@code fractionalDigits: 0} whether or not the model restates it), the LITERAL arguments overlay them, and an
     * integer-grained number ladders by its digit count - {@code <= 9} Integer, {@code <= 18} Long, else BigInteger,
     * an unconstrained count reading 9. Anything not integer-grained is BigDecimal. An argument that passes a
     * parameter through by NAME binds nothing here (it is unbound at this use site) and is ignored, as is a parameter
     * no number knows; {@code min} and {@code max} constrain no Java type but MUST read as decimals, so a
     * present-but-unparseable one refuses rather than being dropped.
     */
    private static String ladder(IRType reference, RNumberType base, List<IRTypeArgument> arguments) {
        Integer digits = base.digits().isPresent() ? base.digits().getAsInt() : null;
        Integer fractionalDigits = base.fractionalDigits().isPresent() ? base.fractionalDigits().getAsInt() : null;
        for (IRTypeArgument argument : arguments) {
            if (argument.literalValue().isEmpty()) {
                continue;   // a name value binds nothing at a use site
            }
            String text = (argument.negated() ? "-" : "") + argument.literalValue().get();
            switch (argument.parameter()) {
                case "digits":
                    digits = integer(reference, argument.parameter(), text);
                    break;
                case "fractionalDigits":
                    fractionalDigits = integer(reference, argument.parameter(), text);
                    break;
                case "min":
                case "max":
                    decimal(reference, argument.parameter(), text);
                    break;
                default:
                    break;   // a parameter no number type knows constrains nothing
            }
        }
        if (fractionalDigits == null || fractionalDigits != 0) {
            return "java.math.BigDecimal";
        }
        int count = digits == null ? DEFAULT_DIGITS : digits;
        if (count <= 9) {
            return "java.lang.Integer";
        }
        return count <= 18 ? "java.lang.Long" : "java.math.BigInteger";
    }

    private static int integer(IRType reference, String parameter, String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException notAnInt) {
            throw refuse(Reason.ARGUMENT, "unparseable argument '" + parameter + "' = '" + text + "' on '" + reference.name() + "'");
        }
    }

    private static void decimal(IRType reference, String parameter, String text) {
        try {
            new BigDecimal(text);
        } catch (NumberFormatException notADecimal) {
            throw refuse(Reason.ARGUMENT, "unparseable argument '" + parameter + "' = '" + text + "' on '" + reference.name() + "'");
        }
    }

    /**
     * The one refusal the three UNLINKED workspace fallbacks of the old generator would have answered instead (its
     * post-resolution bypass never reaches here: it answers a name the linker resolved, which this derivation types).
     */
    private static Refusal unresolved(IRType reference) {
        return refuse(Reason.UNRESOLVED, "unresolved reference '" + reference.name() + "' - the old generator's unlinked workspace fallbacks"
                + " (the alias table, the simple-name first match, the alias search)"
                + " are refused on the IR route");
    }

    private static Refusal refuse(Reason reason, String what) {
        return new Refusal(reason, "IR type gate: " + what, null);
    }
}
