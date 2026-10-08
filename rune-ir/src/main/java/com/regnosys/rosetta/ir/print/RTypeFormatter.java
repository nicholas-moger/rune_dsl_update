package com.regnosys.rosetta.ir.print;

import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RRecordType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;

import java.util.stream.Collectors;

/**
 * Renders an expression's {@link RMetaAnnotatedType} to a deterministic type
 * token for the IR text form.
 *
 * <p>Examples: {@code <missing>}, {@code boolean}, {@code number},
 * {@code number<fractionalDigits=2>}, {@code Trade meta[location, scheme]}.
 *
 * <p>The {@link RType} switch is total over the sealed interface's 9 permitted
 * variants ({@code RDataTypeRef, REnumTypeRef, RChoiceTypeRef, RBasicType,
 * RNumberType, RStringType, RRecordType, RAliasType, RMissingType}); the
 * compiler enforces exhaustiveness — there is no {@code default} branch.
 *
 * <p><b>Step-1 constraint drop (intentional):</b> only
 * {@link RNumberType#fractionalDigits()} is surfaced. The remaining numeric
 * parameters (digits, min, max), string parameters (minLength, maxLength,
 * pattern), and alias parameters (refersTo, arguments) are intentionally omitted
 * here; they are deferred to a later step where the typed-expression wire form is
 * finalized and goldened. Step-2 renders only the type name, {@code fractionalDigits}
 * when present, and meta attributes — the same subset as this printer.
 */
final class RTypeFormatter {

    private RTypeFormatter() {
    }

    /**
     * Formats {@code type} to a deterministic string token.
     *
     * <p>When the type is missing, returns {@code "<missing>"}. Otherwise
     * returns the type name, optionally followed by {@code <fractionalDigits=N>}
     * for a constrained {@link RNumberType}, and then {@code meta[a, b, ...]}
     * (sorted alphabetically) when meta attributes are present.
     *
     * @param type the annotated type to render; must not be {@code null}
     * @return a non-null, non-empty token string
     */
    static String format(RMetaAnnotatedType type) {
        if (type.isMissing()) {
            return "<missing>";
        }
        String base = formatType(type.type());
        if (!type.metaAttributes().isEmpty()) {
            String sortedMeta = type.metaAttributes().stream()
                    .sorted()
                    .collect(Collectors.joining(", "));
            base += " meta[" + sortedMeta + "]";
        }
        return base;
    }

    /**
     * Dispatches over all 9 sealed {@link RType} variants.
     * The {@code RNumberType} arm appends {@code <fractionalDigits=N>} when
     * the parameter is present (step-1 only; see class-level note).
     * The {@code RMissingType} arm is unreachable when the caller guards with
     * {@link RMetaAnnotatedType#isMissing()} first, but is required for
     * sealed-switch totality.
     */
    private static String formatType(RType type) {
        return switch (type) {
            case RNumberType n -> n.name() + (n.fractionalDigits().isPresent()
                    ? "<fractionalDigits=" + n.fractionalDigits().getAsInt() + ">" : "");
            case RBasicType b -> b.name();
            case RStringType s -> s.name();
            case RRecordType r -> r.name();
            case RAliasType a -> a.name();
            case RDataTypeRef d -> d.name();
            case REnumTypeRef e -> e.name();
            case RChoiceTypeRef c -> c.name();
            // Unreachable when isMissing() is checked first; present for sealed totality.
            case RMissingType m -> m.name();
        };
    }
}
