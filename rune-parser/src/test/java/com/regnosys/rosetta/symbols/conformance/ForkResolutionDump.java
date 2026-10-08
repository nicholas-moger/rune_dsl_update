package com.regnosys.rosetta.symbols.conformance;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RClosureParameter;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RParameter;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RLibraryFunction;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ast.types.RMetaType;
import com.regnosys.rosetta.ast.util.AstWalker;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The FORK side of the resolution differential — emits the fork's bindings in
 * the byte-for-byte shape of the upstream oracle's dump
 * ({@code scripts/xtext-oracle}), so a green diff means "the fork bound every
 * name where upstream bound it".
 *
 * <h2>The translation this class performs, which IS a spec claim</h2>
 *
 * Upstream has no {@code a -> b} ambiguity to resolve: its grammar reaches
 * {@code RosettaEnumValueReference} from exactly two non-expression positions
 * ({@code FunctionDispatch} and the legacy synonym maps), so in an expression
 * EVERY {@code a -> b} is a {@code RosettaFeatureCall} over a receiver and one
 * scope rule answers it. The fork's ANTLR grammar instead places
 * {@code qualifiedName ARROW validID  #EnumValueRefExpr} in the primary
 * alternatives AHEAD of {@code #SymbolRefExpr}, manufacturing a node upstream
 * never builds — and {@link REnumValueRef} consequently carries EIGHT
 * resolution slots where upstream's feature call carries one.
 *
 * <p>So an {@code REnumValueRef} is emitted here as the TWO records upstream
 * would have produced: a {@code RosettaSymbolReference.symbol} for the
 * left-hand name and a {@code RosettaFeatureCall.feature} for the right-hand
 * one, whichever of the eight slots the fork happened to fill. Translating
 * rather than special-casing is deliberate — it makes the fork's extra node
 * invisible to the diff, so the diff measures RESOLUTION and not
 * representation.
 *
 * <p>What it does NOT paper over: where the fork binds a choice option it binds
 * the option's TYPE ({@code RDataType Security}) while upstream binds the
 * OPTION DECLARATION ({@code ChoiceOption conformance.a1.Instrument.Security}).
 * That is a real difference in what got bound, not an encoding artefact, so it
 * is left visible in the diff.
 *
 * <h2>Declared scope</h2>
 *
 * Only the three reference kinds C1 owns are emitted:
 * {@code RosettaSymbolReference.symbol}, {@code RosettaFeatureCall.feature} and
 * {@code RosettaDeepFeatureCall.feature}. Type calls, annotation refs,
 * operation paths and imports are other layers' surfaces and are excluded HERE
 * rather than silently absent — {@link ResolutionConformanceTest} applies the
 * same filter to the oracle side so both halves are restricted identically.
 */
final class ForkResolutionDump {

    /** The cross-reference kinds this differential covers. */
    static final List<String> IN_SCOPE_REFS = List.of(
            "RosettaSymbolReference.symbol",
            "RosettaFeatureCall.feature",
            "RosettaDeepFeatureCall.feature");

    private ForkResolutionDump() {}

    /**
     * @param sourceByFileName the exact text each model was parsed from, keyed
     *                         by file name — required because the fork records
     *                         BYTE offsets while the oracle records CHARACTER
     *                         offsets, and the two only line up after
     *                         conversion. The a1 snippet deliberately keeps
     *                         non-ASCII text in its comments so this conversion
     *                         stays exercised; on a pure-ASCII suite the bug
     *                         would be invisible.
     */
    static List<String> dump(List<RModel> models, Map<String, String> sourceByFileName) {
        List<String> records = new ArrayList<>();
        for (RModel model : models) {
            String file = Paths.get(model.sourceRange().file()).getFileName().toString();
            String source = sourceByFileName.get(file);
            if (source == null) {
                throw new IllegalArgumentException("no source text supplied for " + file);
            }
            int[] byteToChar = byteToCharTable(source);
            AstWalker.walk(model, node -> emit(records, file, byteToChar, node));
        }
        records.sort(String::compareTo);
        return records;
    }

    private static void emit(List<String> records, String file, int[] byteToChar, RNode node) {
        if (node instanceof RSymbolReference reference) {
            record(records, file, byteToChar, reference.sourceRange().startOffset(), reference.name(),
                    "RosettaSymbolReference.symbol",
                    // resolvedFeatureNode is the AUTHORITY where the name names a
                    // feature of the implicit item (spec R7.1); symbol() otherwise.
                    reference.resolvedFeatureNode().orElseGet(() -> reference.symbol().orElse(null)));
        } else if (node instanceof RFeatureCall call) {
            record(records, file, byteToChar, tailOffset(call, call.featureName()), call.featureName(),
                    "RosettaFeatureCall.feature",
                    // resolvedFeatureNode is the AUTHORITY (v3.1 C1, spec R9);
                    // resolvedFeature is its attribute-only legacy view.
                    call.resolvedFeatureNode().orElseGet(() -> call.resolvedFeature().orElse(null)));
        } else if (node instanceof RDeepFeatureCall call) {
            record(records, file, byteToChar, tailOffset(call, call.featureName()), call.featureName(),
                    "RosettaDeepFeatureCall.feature", call.resolvedFeature().orElse(null));
        } else if (node instanceof REnumValueRef reference) {
            emitEnumValueRefAsUpstreamWouldSeeIt(records, file, byteToChar, reference);
        }
    }

    /**
     * Byte offset to character offset for the given text. Sized one past the
     * last byte so an end-exclusive offset converts too.
     */
    private static int[] byteToCharTable(String source) {
        int byteLength = source.getBytes(StandardCharsets.UTF_8).length;
        int[] table = new int[byteLength + 1];
        int bytePosition = 0;
        for (int charPosition = 0; charPosition < source.length(); charPosition++) {
            int width = String.valueOf(source.charAt(charPosition))
                    .getBytes(StandardCharsets.UTF_8).length;
            // A lone surrogate encodes as '?' (1 byte); a pair is measured on
            // its low half, which is why every byte in the span maps back to
            // the span's first character rather than to the nearest one.
            for (int i = 0; i < width && bytePosition < byteLength; i++) {
                table[bytePosition++] = charPosition;
            }
        }
        table[byteLength] = source.length();
        return table;
    }

    /**
     * Splits the fork-only {@code REnumValueRef} into the receiver-symbol and
     * feature records upstream produces for the same source text. Which of the
     * eight slots is filled decides the TARGET; that a record is emitted at all
     * does not, so an unresolved rung shows up as a missing BINDING rather than
     * as a missing record.
     */
    private static void emitEnumValueRefAsUpstreamWouldSeeIt(
            List<String> records, String file, int[] byteToChar, REnumValueRef reference) {

        // Slot-to-side mapping, read off each slot's contract:
        //   resolvedEnum / resolvedValue          left / right
        //   resolvedSymbol (Gap A callable)       left
        //   resolvedAttributeChain(attr, feature) left / right — and its
        //       one-argument form records only the RIGHT, because the leading
        //       name matched a CLOSURE PARAMETER, which the fork never stores.
        //       That missing left binding is a real information loss, not a
        //       dump artefact, and shows up as an unresolved left.
        //   resolvedTypeRestriction(lhs, type)    left / right
        //   resolvedRestrictionType               right (speculative)
        //   resolvedChoiceOption                  right
        //   resolvedInputFeature                  RIGHT — the feature read on a
        //       lexical head's type. It is documented as TYPING-ONLY and
        //       deliberately separate from resolvedAttributeChain, which the
        //       generator reads to render the same navigation: one name,
        //       resolved twice, by two subsystems that need not agree. Layer 1
        //       exists to end exactly that.
        RNode left = firstPresent(
                // resolvedHead is the AUTHORITY (v3.1 C1): the head resolved by
                // the one symbol rule. The rungs below it are read only where it
                // has not been filled, and are expected to fall away entirely
                // once the ladder is retired.
                reference.resolvedHead().orElse(null),
                reference.enumeration().map(RNode.class::cast).orElse(null),
                reference.resolvedSymbol().map(RNode.class::cast).orElse(null),
                reference.resolvedAttributeChain()
                        .flatMap(chain -> chain.attributeOpt().map(RNode.class::cast)).orElse(null),
                reference.resolvedTypeRestriction()
                        .map(restriction -> (RNode) restriction.lhsAttribute()).orElse(null));

        RNode right = firstPresent(
                // resolvedFeatureNode is the AUTHORITY (v3.1 C1, spec R9).
                reference.resolvedFeatureNode().orElse(null),
                reference.enumValue().map(RNode.class::cast).orElse(null),
                reference.resolvedChoiceOption().map(RNode.class::cast).orElse(null),
                reference.resolvedAttributeChain()
                        .map(chain -> (RNode) chain.feature()).orElse(null),
                reference.resolvedTypeRestriction()
                        .map(restriction -> (RNode) restriction.restrictionType()).orElse(null),
                reference.resolvedRestrictionType().map(RNode.class::cast).orElse(null),
                reference.resolvedInputFeature().map(RNode.class::cast).orElse(null));

        record(records, file, byteToChar, reference.sourceRange().startOffset(), reference.enumName(),
                "RosettaSymbolReference.symbol", left);
        record(records, file, byteToChar, tailOffset(reference, reference.valueName()),
                reference.valueName(), "RosettaFeatureCall.feature", right);
    }

    private static RNode firstPresent(RNode... candidates) {
        for (RNode candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /** Offset of the trailing name token in a node whose range spans {@code a -> b}. */
    private static int tailOffset(RNode node, String name) {
        int end = node.sourceRange().endOffset();
        return name == null || end < 0 ? -1 : end - name.length();
    }

    private static void record(List<String> records, String file, int[] byteToChar, int byteOffset,
                               String text, String reference, RNode target) {
        int offset = byteOffset < 0 || byteOffset >= byteToChar.length ? -1 : byteToChar[byteOffset];
        records.add("XREF\t" + file
                + "\t" + (offset < 0 ? "-0000001" : String.format("%08d", offset))
                + "\t" + reference
                + "\t" + (text == null ? "" : text)
                + "\t" + upstreamEClassOf(target)
                + "\t" + fullyQualifiedName(target));
    }

    /**
     * Maps a fork AST node onto the upstream EClass name the oracle prints.
     *
     * <p>The structural divergence worth naming: upstream models a choice as
     * {@code Choice extends Data} whose options ARE its attributes, which is
     * exactly why ONE feature lookup finds them with no special case. The fork
     * models {@link RChoice} separately from {@link RDataType}, with its own
     * {@link RChoiceOption} list — so it must go out of its way to expose
     * options through the same path, and that gap is the shape of E1.
     */
    private static String upstreamEClassOf(RNode target) {
        if (target == null)                        return "UNRESOLVED";
        if (target instanceof RChoiceOption)       return "ChoiceOption";
        if (target instanceof RAttribute)          return "Attribute";
        if (target instanceof REnumValue)          return "RosettaEnumValue";
        if (target instanceof REnumeration)        return "RosettaEnumeration";
        if (target instanceof RChoice)             return "Choice";
        if (target instanceof RDataType)           return "Data";
        if (target instanceof RMetaType)           return "RosettaMetaType";
        if (target instanceof RBasicType)          return "RosettaBasicType";
        if (target instanceof RFunction)           return "Function";
        if (target instanceof RRule)               return "RosettaRule";
        if (target instanceof RShortcut)           return "ShortcutDeclaration";
        if (target instanceof RLibraryFunction)    return "RosettaExternalFunction";
        if (target instanceof RTypeAlias)          return "RosettaTypeAlias";
        if (target instanceof RRecordType)         return "RosettaRecordType";
        if (target instanceof RTypeParameter)      return "TypeParameter";
        if (target instanceof RParameter)          return "RosettaParameter";
        if (target instanceof RClosureParameter)   return "ClosureParameter";
        // A binding to an RInlineFunction itself is the PRE-node shape (v3.2 seat 8
        // gave declared closure parameters their own node; only a name-only
        // parameter — the synthetic `item`, which upstream never cross-references —
        // still binds to its lambda) and prints as its class name, visibly foreign
        // to the oracle's kinds, so the differential reports it rather than hides it.
        return target.getClass().getSimpleName();
    }

    /** The kind mapping, exposed to the seat suites (v3.2 seat 8: {@code ClosureParameterSeatTest.a9}). */
    static String upstreamEClassOfForTest(RNode target) {
        return upstreamEClassOf(target);
    }

    /**
     * Rebuilds upstream's qualified name: the namespace, then every named
     * container between the file root and the target, then the target's own
     * name. A {@code metaType} is the documented exception — upstream's name
     * provider gives it the bare simple name with no namespace.
     */
    private static String fullyQualifiedName(RNode target) {
        if (target == null) {
            return "UNRESOLVED";
        }
        if (target instanceof RMetaType) {
            return String.valueOf(nameOf(target));
        }
        List<String> segments = new ArrayList<>();
        for (RNode node = target; node != null; node = node.parent()) {
            if (node instanceof RModel model) {
                segments.add(0, model.namespace());
                break;
            }
            String name = nameOf(node);
            if (name != null && !name.isEmpty()) {
                segments.add(0, name);
            }
        }
        return String.join(".", segments);
    }

    /**
     * A choice option carries no name field of its own — its type call IS its
     * identity — so its name is the type it names. Every other named node
     * answers {@code name()}; reflection covers the long tail (functions,
     * rules, shortcuts, closure parameters, reports) without this test class
     * having to enumerate the whole AST.
     */
    private static String nameOf(RNode node) {
        if (node instanceof RChoiceOption option) {
            return option.typeCall() == null ? null : option.typeCall().typeName();
        }
        try {
            Method name = node.getClass().getMethod("name");
            if (name.getReturnType() == String.class) {
                return (String) name.invoke(node);
            }
        } catch (ReflectiveOperationException ignored) {
            // Not a named node — contributes no segment.
        }
        return null;
    }
}
