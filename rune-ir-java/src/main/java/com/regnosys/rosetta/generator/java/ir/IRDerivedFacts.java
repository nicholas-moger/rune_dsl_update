package com.regnosys.rosetta.generator.java.ir;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import com.regnosys.rosetta.ast.builder.AstBuilderHelper;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator.MetaKind;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRBounds;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.rosetta.util.DottedPath;

/**
 * THE DERIVED-FILE FACTS, FROM THE IR ALONE (v3.3 seat 9, PR #645 commit 12).
 *
 * <p>Every fact the per-type DERIVED files stand on - the validated MEMBER list, the cardinality bounds and the
 * fully-unbounded skip, the supertype chain and the effective attributes, the DECLARED field behind every property,
 * the alias chain and the whole-validator refusal, the string / number constraint envelope with its registry-base
 * fallback, the meta wrap under the override union, the meta KIND, the {@code *Meta} condition refs root-first,
 * deep-path eligibility and the HashMap-ordered feature map, the {@code java.lang} collision and the cast-site item
 * simple name - computed from {@link IRTypeNode}, {@link IRTypeIndex} and {@link IRPropertyModel} and from NOTHING
 * else. No AST node, no {@code GeneratorModel}, no workspace.
 *
 * <p><b>ONE LAW, TWO CALLERS (LAW 69).</b> These derivations were PRIVATE methods of
 * {@link IRDerivedFactsReconciler} while that class held both halves of the reconcile, and an emitter that called
 * them would have compiled against a class with an AST foot ({@code GeneratorModel}, the three generator seams).
 * Commit 12 RELOCATES them here, unchanged in law: the reconciler keeps its SOURCE half and reads its IR half from
 * this class, and the three validator emitters read the very same instance methods. The reconcile's counts do not
 * move - {@code PROPERTY}, the parents, {@code MODEL} and {@code WRAPPER} read EXACTLY the s9c10b figures, and
 * {@code DERIVED}'s carried sub-count reads 1,753,950 - because not one predicate changed on the way.
 *
 * <p><b>THE ENVELOPE IS TYPED HERE.</b> The reconciler compared a RENDERED envelope string
 * ({@code string[minLength=…]}); an emitter needs the slots themselves. So {@link #envelope} answers the
 * {@link Envelope} record and the RENDERING stays here too ({@link #renderEnvelope}), which is what the reconciler
 * calls for BOTH halves - the fact compared is byte-for-byte the one it compared before.
 *
 * <p><b>THE LIE.</b> {@link IRDerivedLie} is the test seam; it is a CONSTRUCTOR ARGUMENT here, so every existing
 * lane (D01-D14) still reads RED on its family through the reconciler, and an emitter built with
 * {@link IRDerivedLie#NONE} can never be lied to by a test that lies to a reconciler.
 */
final class IRDerivedFacts {

    /** The supertype-chain walk's depth bound - a cyclic {@code extends} ends the list rather than the JVM. */
    static final int MAX_CHAIN_DEPTH = 100;

    /** The builtin table the envelope's base case reads, by NAME - {@code TypeFormatConstraintScan}'s own. */
    private static final BuiltinTypeRegistry BUILTINS = BuiltinTypeRegistry.createDefault();

    private final IRTypeIndex index;
    private final IRDerivedLie lie;

    IRDerivedFacts(IRTypeIndex index, IRDerivedLie lie) {
        this.index = Objects.requireNonNull(index, "index");
        this.lie = Objects.requireNonNull(lie, "lie");
    }

    // ------------------------------------------------------------------------------------ the member lists

    /**
     * The validated MEMBERS of the element: {@code allProperties} - own AND inherited - with the synthetic
     * {@code meta} property removed, which is exactly what {@code ValidatorScan.scan} leaves after its
     * {@code isSyntheticMeta} skip ({@code ValidatorScan.java:111-113}, {@code :143-147}). The skip counts over the
     * EFFECTIVE surface, so an inherited synthetic {@code meta} is skipped too (992 on the 25 vendored cells, not the
     * 804 declared ones - {@code PROBE-C3.md} § 3).
     */
    List<IRPropertyModel.IRProperty> members(IRPropertyModel properties) {
        List<IRPropertyModel.IRProperty> members = new ArrayList<>();
        for (IRPropertyModel.IRProperty property : properties.allProperties()) {
            if (property.synthetic() && lie != IRDerivedLie.ONLY_EXISTS_NO_SYNTHETIC_SKIP) {
                continue;
            }
            members.add(property);
        }
        return members;
    }

    /**
     * {@code {min, max, fullyUnbounded}} for one member, from the IR alone - {@code ValidatorScan.scan}'s own law
     * ({@code :114-130}): an inherited choice OPTION has no backing attribute and is {@code 0..1} by choice
     * semantics; otherwise the DECLARED bounds, with {@code max == 0} the "no upper bound" sentinel and
     * {@code fullyUnbounded} exactly {@code unbounded && lower == 0}. An absent cardinality reads {@code 0..0}, which
     * is {@code orElse(0)}'s own answer.
     */
    static int[] bounds(IRPropertyModel.IRProperty member, Map<String, Owned> effective) {
        Owned owned = member.inheritedChoiceOption() ? null : effective.get(member.name());
        if (owned == null) {
            return new int[] {0, 1, 0};
        }
        Optional<IRBounds> bounds = owned.field().bounds();
        if (bounds.isEmpty()) {
            return new int[] {0, 0, 0};
        }
        IRBounds b = bounds.get();
        int min = b.lower().intValue();
        int max = b.isUnbounded() ? 0 : b.upper().get().intValue();
        int fullyUnbounded = b.isUnbounded() && b.lower().signum() == 0 ? 1 : 0;
        return new int[] {min, max, fullyUnbounded};
    }

    // ------------------------------------------------------------------------------------- the chain and maps

    /** One field with the chain position of the link that DECLARES it - what the override union's walk needs. */
    record Owned(IRField field, int chainIndex) {
    }

    /** The element's supertype chain, element FIRST, through THIS instance's index. */
    List<IRTypeNode> chain(IRTypeNode node) {
        return chain(node, index);
    }

    /**
     * The element's supertype chain, element FIRST, through the index's reconciled parents - the IR's route to a
     * declaration in another model (and, on a corpus where every parent is also in the pass, a second independent
     * read of it: risk R2, stated rather than hidden).
     */
    static List<IRTypeNode> chain(IRTypeNode node, IRTypeIndex index) {
        List<IRTypeNode> chain = new ArrayList<>();
        IRTypeNode current = node;
        int depth = 0;
        while (current != null && depth++ < MAX_CHAIN_DEPTH && !holdsIdentically(chain, current)) {
            chain.add(current);
            Optional<IRType> base = current.baseType();
            current = base.isPresent() ? index.parent(base.get()) : null;
        }
        return chain;
    }

    /**
     * The cycle break, by IDENTITY. The index hands out ONE node per declaration, so identity is the right
     * comparison here - and a record's structural {@code equals} over a whole type node would be both a deep walk
     * and a false positive for two distinct but identical declarations.
     */
    private static boolean holdsIdentically(List<IRTypeNode> chain, IRTypeNode node) {
        for (IRTypeNode link : chain) {
            if (link == node) {
                return true;
            }
        }
        return false;
    }

    /**
     * The EFFECTIVE attribute of every name, root-first with the child overriding the parent -
     * {@code GeneratorModel.allAttributes}' own law ({@code :230-246}). A CHOICE link on the chain contributes
     * nothing: {@code allAttributes} walks {@code superType()} only, and a data type extending a choice inherits its
     * OPTIONS, not attributes.
     */
    static Map<String, Owned> effectiveAttributes(List<IRTypeNode> chain) {
        Map<String, Owned> effective = new LinkedHashMap<>();
        for (int i = chain.size() - 1; i >= 0; i--) {
            IRTypeNode link = chain.get(i);
            if (link.kind() != IRKind.STRUCT) {
                continue;
            }
            for (IRField field : link.fields()) {
                effective.put(field.name(), new Owned(field, i));
            }
        }
        return effective;
    }

    /**
     * The DECLARED field behind every POJO property name - the IR mirror of
     * {@code TypeFormatValidatorGenerator.declaredTypesByPropertyName} ({@code :653-692}): a choice's own options
     * keyed by the option type's last segment; a data type's effective attributes, then the NEAREST ancestor choice's
     * options by {@code putIfAbsent}.
     */
    static Map<String, IRField> declaredFieldsByProperty(IRTypeNode ir, List<IRTypeNode> chain) {
        Map<String, IRField> result = new LinkedHashMap<>();
        if (ir.kind() == IRKind.CHOICE) {
            for (IRField option : ir.fields()) {
                result.put(lastSegment(option.name()), option);
            }
            return result;
        }
        for (Map.Entry<String, Owned> entry : effectiveAttributes(chain).entrySet()) {
            result.put(entry.getKey(), entry.getValue().field());
        }
        for (IRTypeNode link : chain) {
            if (link.kind() == IRKind.CHOICE) {
                for (IRField option : link.fields()) {
                    result.putIfAbsent(lastSegment(option.name()), option);
                }
                break;
            }
        }
        return result;
    }

    // ---------------------------------------------------------------------------------------- the alias chain

    /** The field's own alias chain, outermost first - the adapter's walk, which the reconciler holds two ways. */
    static List<IRAliasLink> aliasChain(IRField field) {
        return field.type().aliasChain();
    }

    /**
     * THE WHOLE-VALIDATOR REFUSAL as a pure function of the field's IR alias chain - what the emitter reads. A rung
     * that is PARAMETERISED and carries at least one condition refuses the type-format validator of the WHOLE element
     * ({@code TypeFormatValidatorGenerator.java:248-264}): the wing is one method over every attribute, so there is
     * no per-attribute file to drop.
     */
    static boolean refusesWholeValidator(IRField field) {
        return refusesWholeValidator(aliasChain(field));
    }

    /** The same refusal over a chain already in hand. */
    static boolean refusesWholeValidator(List<IRAliasLink> chain) {
        for (IRAliasLink link : chain) {
            if (refusesAtLink(link)) {
                return true;
            }
        }
        return false;
    }

    /**
     * THE REFUSAL PREDICATE OF ONE RUNG - {@code ConditionCases.parameterisedOwner(alias) != null &&
     * !ConditionCases.casesOf(alias).isEmpty()} over the IR's own alias link.
     *
     * <p>ONE DECLARATION, TWO CALLERS (LAW 69, and lane V11's finding): {@link #refusesWholeValidator} reads it for
     * the reconcile's {@code typeFormat.<p>.refusesValidator} fact, and {@link IRTypeFormatValidatorEmitter}'s
     * wiring loop reads it to decide whether to refuse the type. The emitter first RE-DERIVED the predicate inline,
     * so lane V11 - which inverts the law - left the emitter untouched and read GREEN where it had to read RED. Two
     * producers of one law is exactly what the lane exists to catch, and it caught it.
     */
    static boolean refusesAtLink(IRAliasLink link) {
        return link.isParameterised() && !link.conditionNames().isEmpty();
    }

    // ------------------------------------------------------------------------------- the type-format envelope

    /**
     * ONE constraint envelope, TYPED. Exactly one of the two arms is inhabited; {@link #isString} says which. The
     * decimal slots carry the {@code stripTrailingZeros().toString()} SPELLING - the one normalization both halves
     * apply, and the very literal the old generator writes into {@code new BigDecimal("…")}.
     */
    record Envelope(boolean isString, OptionalInt minLength, OptionalInt maxLength, Optional<String> pattern,
                    OptionalInt digits, OptionalInt fractionalDigits, Optional<String> min, Optional<String> max) {

        static Envelope string(OptionalInt minLength, OptionalInt maxLength, Optional<String> pattern) {
            return new Envelope(true, minLength, maxLength, pattern,
                    OptionalInt.empty(), OptionalInt.empty(), Optional.empty(), Optional.empty());
        }

        static Envelope number(OptionalInt digits, OptionalInt fractionalDigits, Optional<String> min,
                Optional<String> max) {
            return new Envelope(false, OptionalInt.empty(), OptionalInt.empty(), Optional.empty(),
                    digits, fractionalDigits, min, max);
        }
    }

    /**
     * THE ENVELOPE FROM THE IR ALONE - {@code TypeFormatConstraintScan}'s base case ({@code :147-169}) over the
     * facts a reference carries: a {@code typeAlias} reference states its COLLAPSED chain
     * ({@code effectiveBase()}, the use-site arguments already substituted in - PR #643); every other reference
     * states its own name and its use-site arguments. The builtin table seeds the slots the arguments do not state
     * ({@code int}'s {@code fractionalDigits: 0} is why {@code int}-typed attributes carry a number check at all),
     * and "constrained" is the scan's own presence test ({@code :65-77}).
     */
    Optional<Envelope> envelope(IRField field) {
        IRType reference = field.type();
        String baseName;
        List<IRTypeArgument> arguments;
        IRKind leafKind;
        if (reference.kind() == IRKind.TYPE_ALIAS) {
            IREffectiveBase base = reference.effectiveBase().orElse(null);
            if (base == null) {
                return Optional.empty();
            }
            baseName = base.name();
            arguments = base.arguments();
            leafKind = base.kind();
        } else {
            baseName = reference.resolvedName().orElse(reference.name());
            arguments = field.typeArguments();
            leafKind = reference.kind();
        }
        if (leafKind != IRKind.BASIC_TYPE && leafKind != IRKind.RECORD_TYPE) {
            return Optional.empty();
        }
        RType base = BUILTINS.lookup(baseName).orElse(null);
        if (base instanceof RNumberType number) {
            OptionalInt digits = intArgument(arguments, "digits");
            OptionalInt fractionalDigits = intArgument(arguments, "fractionalDigits");
            Optional<String> min = decimalArgument(arguments, "min");
            Optional<String> max = decimalArgument(arguments, "max");
            if (lie != IRDerivedLie.NUMBER_SLOT_NO_BASE) {
                if (digits.isEmpty()) {
                    digits = number.digits();
                }
                if (fractionalDigits.isEmpty()) {
                    fractionalDigits = number.fractionalDigits();
                }
                if (min.isEmpty()) {
                    min = number.min().map(IRDerivedFacts::decimal);
                }
                if (max.isEmpty()) {
                    max = number.max().map(IRDerivedFacts::decimal);
                }
            }
            if (digits.isEmpty() && fractionalDigits.isEmpty() && min.isEmpty() && max.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(Envelope.number(digits, fractionalDigits, min, max));
        }
        if (base instanceof RStringType string) {
            OptionalInt minLength = intArgument(arguments, "minLength");
            OptionalInt maxLength = intArgument(arguments, "maxLength");
            Optional<String> pattern = literal(arguments, "pattern").map(AstBuilderHelper::stripQuotes);
            if (minLength.isEmpty()) {
                minLength = string.minLength();
            }
            if (maxLength.isEmpty()) {
                maxLength = string.maxLength();
            }
            if (pattern.isEmpty()) {
                pattern = string.pattern().map(java.util.regex.Pattern::toString);
            }
            if (minLength.isEmpty() && maxLength.isEmpty() && pattern.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(Envelope.string(minLength, maxLength, pattern));
        }
        return Optional.empty();
    }

    /** The CANONICAL rendering of an envelope - the spelling the reconcile's fact compares, on BOTH halves. */
    static String renderEnvelope(Envelope envelope) {
        return envelope.isString()
                ? renderString(envelope.minLength(), envelope.maxLength(), envelope.pattern())
                : renderNumber(envelope.digits(), envelope.fractionalDigits(), envelope.min(), envelope.max());
    }

    static String renderString(OptionalInt minLength, OptionalInt maxLength, Optional<String> pattern) {
        return "string[minLength=" + text(minLength) + ",maxLength=" + text(maxLength)
                + ",pattern=" + pattern.orElse("-") + "]";
    }

    static String renderNumber(OptionalInt digits, OptionalInt fractionalDigits, Optional<String> min,
            Optional<String> max) {
        return "number[digits=" + text(digits) + ",fractionalDigits=" + text(fractionalDigits)
                + ",min=" + min.orElse("-") + ",max=" + max.orElse("-") + "]";
    }

    /**
     * A decimal constraint in ONE spelling on both halves: {@code stripTrailingZeros().toString()} - the same
     * normalization the parser applies on the way in ({@code TypeFormatConstraintScan.parse}) and the spelling the
     * generator writes into {@code new BigDecimal("…")}.
     */
    static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toString();
    }

    private static Optional<String> literal(List<IRTypeArgument> arguments, String parameter) {
        for (IRTypeArgument argument : arguments) {
            if (argument.parameter().equals(parameter) && argument.literalValue().isPresent()) {
                return Optional.of(argument.negated() ? "-" + argument.literalValue().get()
                        : argument.literalValue().get());
            }
        }
        return Optional.empty();
    }

    private static OptionalInt intArgument(List<IRTypeArgument> arguments, String parameter) {
        Optional<String> text = literal(arguments, parameter);
        if (text.isEmpty()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(text.get()));
        } catch (NumberFormatException notAnInt) {
            return OptionalInt.empty();
        }
    }

    private static Optional<String> decimalArgument(List<IRTypeArgument> arguments, String parameter) {
        Optional<String> text = literal(arguments, parameter);
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(decimal(new BigDecimal(text.get())));
        } catch (NumberFormatException notADecimal) {
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------------------------- the meta annotations

    /**
     * Whether a property's DECLARED type is meta-wrapped for the type-format value expression -
     * {@code TypeFormatValidatorGenerator.hasMetadataAnnotation} over
     * {@code MetaFieldGenerator.allMetaAnnotationRefs} ({@code :670-673}): ANY {@code [metadata …]}, qualifier and
     * all, read over the OVERRIDE UNION (parent chain first, own last).
     */
    boolean metaWrapped(IRField field, Map<String, Owned> effective, List<IRTypeNode> chain) {
        for (IRAnnotationUse annotation : unionedAnnotations(field, effective, chain)) {
            if ("metadata".equals(annotation.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * A field's annotations under the OVERRIDE-INHERITANCE UNION - {@code MetaFieldGenerator.allMetaAnnotationRefs}
     * ({@code :109-118}) through {@code RuleReferenceTraversal.parentAttributeOf} ({@code :415-432}): only when the
     * field {@code isOverride()} and the DECLARING element is a data type, the same-named field is found walking
     * THAT link's own supertype chain, and the parent's own union comes FIRST.
     */
    List<IRAnnotationUse> unionedAnnotations(IRField field, Map<String, Owned> effective,
            List<IRTypeNode> chain) {
        Owned owned = effective.get(field.name());
        int declaredAt = owned != null && owned.field() == field ? owned.chainIndex() : -1;
        return union(field, chain, declaredAt, lie != IRDerivedLie.META_UNION_NO_PARENT_LEG);
    }

    /**
     * THE OVERRIDE UNION as a pure function of a declaration node and the index - the entry point the WRAPPER family
     * reads too ({@link IRWrapperReconciler}), so the two families state the law once.
     *
     * @param withParentLeg {@code false} is the G2 lying-walk witness - the union loses its parent leg
     */
    static List<IRAnnotationUse> unionedAnnotationsOf(IRField field, List<IRTypeNode> chain,
            Map<String, Owned> effective, boolean withParentLeg) {
        Owned owned = effective.get(field.name());
        int declaredAt = owned != null && owned.field() == field ? owned.chainIndex() : -1;
        return union(field, chain, declaredAt, withParentLeg);
    }

    private static List<IRAnnotationUse> union(IRField field, List<IRTypeNode> chain, int declaredAt,
            boolean withParentLeg) {
        List<IRAnnotationUse> union = new ArrayList<>();
        if (withParentLeg && field.isOverride() && declaredAt >= 0 && declaredAt < chain.size()
                && chain.get(declaredAt).kind() == IRKind.STRUCT) {
            for (int i = declaredAt + 1; i < chain.size(); i++) {
                IRTypeNode link = chain.get(i);
                if (link.kind() != IRKind.STRUCT) {
                    break;                        // parentAttributeOf walks superType() only
                }
                IRField parent = fieldNamed(link, field.name());
                if (parent != null) {
                    union.addAll(union(parent, chain, i, true));
                    break;
                }
            }
        }
        union.addAll(field.annotations());
        return union;
    }

    private static IRField fieldNamed(IRTypeNode link, String name) {
        for (IRField field : link.fields()) {
            if (field.name().equals(name)) {
                return field;
            }
        }
        return null;
    }

    /**
     * The meta KIND of a field from the IR alone - {@code MetaFieldGenerator.detectMetaKind}'s qualifier law
     * ({@code :128-146}): {@code reference} / {@code address} win {@code REFERENCE_WITH_META} over
     * {@code scheme} / {@code id} / {@code location}'s {@code FIELD_WITH_META}; {@code key} / {@code template} are
     * type-level meta and contribute neither.
     */
    static MetaKind metaKind(List<IRAnnotationUse> annotations) {
        boolean hasReference = false;
        boolean hasFieldMeta = false;
        for (IRAnnotationUse annotation : annotations) {
            if (!"metadata".equals(annotation.name())) {
                continue;
            }
            String qualifier = annotation.qualifier().orElse("");
            if ("reference".equals(qualifier) || "address".equals(qualifier)) {
                hasReference = true;
            }
            if ("scheme".equals(qualifier) || "id".equals(qualifier) || "location".equals(qualifier)) {
                hasFieldMeta = true;
            }
        }
        if (hasReference) {
            return MetaKind.REFERENCE_WITH_META;
        }
        if (hasFieldMeta) {
            return MetaKind.FIELD_WITH_META;
        }
        return MetaKind.NONE;
    }

    /** The meta KIND of ONE field under the override union - the two laws above in the order the emitter takes them. */
    MetaKind metaKind(IRField field, Map<String, Owned> effective, List<IRTypeNode> chain) {
        return metaKind(unionedAnnotations(field, effective, chain));
    }

    // ------------------------------------------------------------------------------------ the condition refs

    /**
     * ONE condition ref of a chain: the chain LINK that DECLARES the condition, and the condition class's SIMPLE
     * NAME (v3.3 seat 9, PR #645 commit 13). {@code ModelMetaGenerator.ConditionRef}'s IR twin, minus the rendered
     * spellings - the {@code *Meta} emitter builds the declaring type's canonical name and its
     * {@code validation.datarule} package from the link itself, which is the only part of a ref that is not a
     * function of its simple name.
     *
     * @param link       the chain link that DECLARES the condition ({@code ModelMetaGenerator.java:292-295}'s
     *                   {@code link} - a data type, or a choice contributing its implicit condition)
     * @param simpleName the condition class's simple name
     */
    record ConditionRefLink(IRTypeNode link, String simpleName) {
    }

    /**
     * The condition-class refs of an element's chain, ROOT-FIRST, WITH THEIR DECLARING LINKS - the ONE walk
     * ({@code ModelMetaGenerator.collectConditionRefs}, {@code :273-321}) that {@link #conditionRefs} and the
     * {@code *Meta} emitter both read (v3.3 seat 9, PR #645 commit 13; LAW 69 - one law, two callers). A condition's
     * KIND stands in for its expression root, as it did in {@link #conditionRefs}'s own former body, which this
     * method IS: {@code conditionRefs} is re-expressed over it, so not one name it answers can move.
     */
    static List<ConditionRefLink> conditionRefLinks(List<IRTypeNode> chain) {
        List<ConditionRefLink> refs = new ArrayList<>();
        for (int i = chain.size() - 1; i >= 0; i--) {
            IRTypeNode link = chain.get(i);
            String declaringName = simpleName(link);
            if (link.kind() == IRKind.CHOICE) {
                refs.add(new ConditionRefLink(link, declaringName + "Choice"));
                continue;
            }
            List<Optional<String>> names = link.conditionNames();
            List<String> kinds = link.conditionKinds();
            long namedCount = names.stream().filter(Optional::isPresent).count();
            for (int j = 0; j < names.size(); j++) {
                String kind = j < kinds.size() ? kinds.get(j) : "DataRule";
                // orElseGet, not orElse: the suffixed name is built only for the conditions that have no name of
                // their own (round 1, NIT-9)
                refs.add(new ConditionRefLink(link, declaringName + names.get(j).orElseGet(() -> kind + namedCount)));
            }
        }
        return refs;
    }

    /**
     * The condition-class refs of an element's chain, ROOT-FIRST, from the IR alone - the SIMPLE NAMES of
     * {@link #conditionRefLinks}, which carries this method's own former body verbatim (v3.3 seat 9, PR #645
     * commit 13: the walk was lifted so the {@code *Meta} emitter reads the DECLARING LINK from the same law, and
     * NOT ONE NAME MOVED - the reconciled fact {@code meta.conditionRefs} and its count are EXACT across the
     * re-expression, which the D05 lane still reads RED on its family). The reconciler holds this against its own
     * walk of {@code ModelMetaGenerator}'s law and against the GENERATOR'S OWN answer before an emitter reads it.
     */
    static List<String> conditionRefs(List<IRTypeNode> chain) {
        List<String> refs = new ArrayList<>();
        for (ConditionRefLink ref : conditionRefLinks(chain)) {
            refs.add(ref.simpleName());
        }
        return refs;
    }

    /**
     * ONE ALIAS LINK's condition class SIMPLE NAMES, under the same {@code namedCount} law applied to the ALIAS's own
     * conditions ({@code ModelMetaGenerator.java:297-299} as {@link #conditionRefs} applies it per chain link). The
     * type-format wing wires {@code <AliasSimpleName><conditionName>} in the ALIAS's own
     * {@code <namespace>.validation.datarule} package ({@code JavaTypeTranslator:296-302}).
     */
    static List<String> aliasConditionSimpleNames(IRAliasLink link) {
        List<String> names = new ArrayList<>();
        String declaringName = lastSegment(link.qualifiedName());
        List<Optional<String>> conditionNames = link.conditionNames();
        List<String> kinds = link.conditionKinds();
        long namedCount = conditionNames.stream().filter(Optional::isPresent).count();
        for (int i = 0; i < conditionNames.size(); i++) {
            String kind = i < kinds.size() ? kinds.get(i) : "DataRule";
            names.add(declaringName + conditionNames.get(i).orElseGet(() -> kind + namedCount));
        }
        return names;
    }

    /**
     * THE CONDITION CLASSES the type-format wing wires for ONE property, as CANONICAL NAMES, in wiring order - the
     * alias chain outermost first, each rung's conditions in declaration order. ONE declaration, TWO callers
     * (LAW 69): {@link IRTypeFormatValidatorEmitter} builds its injected fields from it, and the derived-facts
     * reconcile holds it against the generator's own {@code wiredConditionClasses} seam
     * ({@code typeFormat.<p>.aliasConditionClasses}, v3.3 seat 9, PR #645 commit 12).
     */
    static List<String> aliasConditionClassNames(IRField field) {
        List<String> canonical = new ArrayList<>();
        for (IRAliasLink link : aliasChain(field)) {
            String rulePackage = aliasConditionPackage(link);
            for (String simpleName : aliasConditionSimpleNames(link)) {
                canonical.add(rulePackage + "." + simpleName);
            }
        }
        return canonical;
    }

    /**
     * The {@code validation.datarule} package of ONE alias rung, ESCAPED as a whole -
     * {@code JavaTypeTranslator.toConditionJavaClass}'s own law ({@code :296-302}).
     */
    static String aliasConditionPackage(IRAliasLink link) {
        String namespace = link.namespace().orElse("");
        DottedPath rulePackage = (namespace.isEmpty() ? DottedPath.of() : DottedPath.splitOnDots(namespace))
                .child("validation").child("datarule");
        return JavaPackageName.escape(rulePackage).getName().withDots();
    }

    // ---------------------------------------------------------------------------------------- the deep path

    /**
     * ELIGIBILITY from the IR alone - {@code DeepPathScan.isEligible} ({@code :121-141}): a choice is eligible iff it
     * declares at least one option; a data type iff it declares an OWN {@code one-of} condition, has a non-empty
     * effective attribute set and EVERY one of those attributes is exactly {@code 0..1} (an absent cardinality counts
     * as single-optional, the port's own convention).
     */
    boolean eligible(IRTypeNode node, List<IRTypeNode> chain) {
        if (node.kind() == IRKind.CHOICE) {
            return !node.fields().isEmpty();
        }
        if (node.kind() != IRKind.STRUCT) {
            return false;
        }
        if (!node.conditionKinds().contains("OneOf")) {
            return false;
        }
        Map<String, Owned> effective = effectiveAttributes(chain);
        if (effective.isEmpty()) {
            return false;
        }
        if (lie == IRDerivedLie.DEEP_ELIGIBLE_NO_SINGULAR_CLAUSE) {
            return true;
        }
        for (Owned owned : effective.values()) {
            if (!singularOptional(owned.field())) {
                return false;
            }
        }
        return true;
    }

    private static boolean singularOptional(IRField field) {
        Optional<IRBounds> bounds = field.bounds();
        if (bounds.isEmpty()) {
            return true;
        }
        IRBounds b = bounds.get();
        return !b.isUnbounded() && b.lower().signum() == 0 && b.upper().get().equals(BigInteger.ONE);
    }

    /**
     * THE DEEP-FEATURE MAP, in the JDK {@code HashMap} ITERATION ORDER that IS the golden {@code choose*} method
     * order - a fresh {@link IrDeepPath} per call, exactly as the reconciler took one per element.
     */
    Map<String, Feature> featureMap(IRTypeNode node) {
        return reordered(deepPath().featureMap(node));
    }

    /**
     * THE SAME LAW OVER A WALKER HANDED IN (v3.3 seat 9, PR #645 commit 14). {@link IRDeepPathUtilEmitter} asks
     * for one element's feature map AND, per alternative, its target's - three or four walks per type - and the
     * old generator runs all of them on ONE {@link DeepPathScan} whose ATTRIBUTE view is memoised
     * ({@code DeepPathScan:62-70}: stable instances are what its retain check compares). A walker handed in
     * shares that memo exactly as the scan does; the MAPS are still fresh every call, which is the order law.
     */
    Map<String, Feature> featureMap(IRTypeNode node, IrDeepPath walk) {
        return reordered(walk.featureMap(node));
    }

    /** A deep-path walker over this facts' index - the emitter holds ONE per emit, as the scan holds one. */
    IrDeepPath deepPath() {
        return new IrDeepPath(index);
    }

    /**
     * One deep feature on the IR side. Its {@code equals} is IDENTITY, exactly as {@code DeepPathScan.ScanAttr}'s is
     * - the scan's {@code intersectButRetainAttribute} compares the retained attribute by {@code equals}, and its
     * per-element attribute list is cached, so the same instances come back on every call.
     */
    static final class Feature {
        private final String name;
        /**
         * The element this feature was read OFF - {@code DeepPathScan.ScanAttr.owner()} ({@code :101-103}). The
         * emitter resolves the feature's getter and item type against THIS node's property model, because a deep
         * feature reached through an alternative is declared by the TARGET and not by the subject
         * ({@code DeepPathUtilGenerator.renderMethod:215}, {@code propOf(feature.owner(), feature.name())}).
         */
        private final IRTypeNode owner;
        private final String typeKey;
        private final boolean multi;
        /** {@code ScanAttr.hasMeta()} - value-wrapping meta, the metadata-collapse swap's whole predicate. */
        private final boolean hasMeta;
        private final IRField field;
        /**
         * The IR's counterpart of {@code ScanAttr.resolvedType == null} - see {@link IrDeepPath#match}. The scan
         * leaves a resolved type null in exactly ONE case, a choice option with NO type call at all
         * ({@code DeepPathScan.buildAttributes}: a data attribute goes through {@code resolveTypeCall}, which
         * answers {@code RMissingType.INSTANCE} and never {@code null}); the adapter's own answer to that case is
         * the {@code typeRef(null)} sentinel - an EMPTY written name with no resolved name and no namespace.
         */
        private final boolean absentType;

        Feature(String name, IRTypeNode owner, String typeKey, boolean multi, boolean hasMeta, IRField field) {
            this.name = name;
            this.owner = owner;
            this.typeKey = typeKey;
            this.multi = multi;
            this.hasMeta = hasMeta;
            this.field = field;
            IRType reference = field.type();
            this.absentType = reference.name().isEmpty() && reference.resolvedName().isEmpty()
                    && reference.resolvedQualifiedName().isEmpty();
        }

        /** The feature's own name - the {@code choose<Name>} method's. */
        String name() {
            return name;
        }

        /** Value-wrapping meta: what {@code DeepPathUtilGenerator}'s "Type coercion" unwrap branches on. */
        boolean hasMeta() {
            return hasMeta;
        }

        /** The element that DECLARES this feature - the property model the emitter resolves it against. */
        IRTypeNode owner() {
            return owner;
        }

        /** {@code ScanAttr.isMulti()} - the {@code List<X>} / {@code mapC} / {@code getMulti()} decision. */
        boolean multi() {
            return multi;
        }

        /** The field behind the feature. */
        IRField field() {
            return field;
        }
    }

    /**
     * THE DEEP-FEATURE MAP FROM THE IR - {@code DeepPathScan.findDeepFeatureMap}'s own algorithm ({@code :192-224})
     * reproduced operation for operation on a fresh {@code java.util.HashMap}, so that the ITERATION ORDER (the
     * golden {@code choose*} method order, {@code DeepPathUtilGenerator.java:51-52}) is reproduced and not merely
     * the key set.
     *
     * <p><b>The metadata-collapse VALUE SWAPS are reproduced, and they are LOAD-BEARING</b> ({@code :250-256} in
     * {@code intersectButRetainAttribute}, {@code :265-268} in {@code merge}). The scan's own comment says the swap
     * "Swaps the VALUE only - key membership (and therefore iteration order) is untouched", and that is true OF
     * THAT CALL and false of the WALK: the swapped-in value is a different {@code ScanAttr} INSTANCE, and
     * {@code intersectButRetainAttribute}'s retain test is {@code attr.equals(attributeToRetain)} on a class with no
     * {@code equals} override - identity. So a key whose value was swapped in round {@code i} is NO LONGER
     * recognised as the retained attribute in round {@code j > i}, and is REMOVED. cdm/5.38.0's
     * {@code cdm.event.common.SettlementOrigin} is the corpus witness: nine {@code (0..1) [metadata reference]}
     * attributes, one of them {@code settlementTerms}, which every payout option also carries (inherited from
     * {@code PayoutBase}) WITHOUT meta - so round 1 swaps it to the payout's non-meta instance, round 7 fails to
     * retain it, and the golden {@code SettlementOriginDeepPathUtil} is the EMPTY class. Without the swap the IR
     * keeps {@code settlementTerms} and the emitter would write a {@code chooseSettlementTerms} the golden has not
     * got. The swap also decides the value the emitter READS ({@code DeepPathUtilGenerator}'s "Type coercion"
     * {@code getValue()} unwrap is driven by {@code hasMeta}), so {@code deepPath.featureMeta} gates it directly.
     *
     * <p>The match key is a stated NORMALIZATION: the scan compares resolved {@code RType}s by {@code equals}, which
     * the IR has no node identity for, so the IR half compares a reference's KIND, its resolved qualified name and
     * its literal arguments. A pair the AST would call equal and this key calls different (an alias against a
     * builtin spelled to the same constraints) is a divergence the corpus would read RED here - the fact is watched
     * rather than assumed.
     */
    final class IrDeepPath {
        private final IRTypeIndex index;
        private final Map<IRTypeNode, List<Feature>> attributeCache = new IdentityHashMap<>();

        IrDeepPath(IRTypeIndex index) {
            this.index = index;
        }

        Map<String, Feature> featureMap(IRTypeNode element) {
            if (!eligible(element, chain(element, index))) {
                return new HashMap<>();
            }
            Map<String, Feature> deepIntersection = null;
            Map<String, Feature> result = new HashMap<>();
            List<Feature> all = attributesOf(element);
            for (Feature attribute : all) {
                result.put(attribute.name, attribute);
            }
            for (Feature attribute : all) {
                IRTypeNode target = descendTarget(attribute.field);
                Map<String, Feature> sub;
                if (target != null) {
                    sub = featureMap(target);
                    for (Feature feature : attributesOf(target)) {
                        sub.put(feature.name, feature);
                    }
                } else {
                    sub = new HashMap<>();
                }
                if (deepIntersection == null) {
                    deepIntersection = sub;
                } else {
                    intersect(deepIntersection, sub, null);
                }
                intersect(result, sub, attribute);
            }
            if (deepIntersection != null) {
                merge(result, deepIntersection);
            }
            return result;
        }

        List<Feature> attributesOf(IRTypeNode element) {
            List<Feature> cached = attributeCache.get(element);
            if (cached != null) {
                return cached;
            }
            List<Feature> features = new ArrayList<>();
            if (element.kind() == IRKind.CHOICE) {
                // DeepPathScan:160-161 - a choice OPTION's meta is its OWN annotation refs, with no override union
                for (IRField option : element.fields()) {
                    features.add(new Feature(option.name(), element, typeKey(option), false,
                            metaKind(option.annotations()) != MetaKind.NONE, option));
                }
            } else {
                // DeepPathScan:168 - a data ATTRIBUTE's meta is detectMetaKind(attr), i.e. the OVERRIDE UNION
                List<IRTypeNode> ownChain = chain(element, index);
                Map<String, Owned> ownEffective = effectiveAttributes(ownChain);
                for (Owned owned : ownEffective.values()) {
                    IRField field = owned.field();
                    boolean hasMeta = metaKind(unionedAnnotationsOf(field, ownChain, ownEffective, true))
                            != MetaKind.NONE;
                    features.add(new Feature(field.name(), element, typeKey(field),
                            field.cardinality() == Cardinality.ZERO_TO_MANY
                                    || field.cardinality() == Cardinality.ONE_TO_MANY, hasMeta, field));
                }
            }
            attributeCache.put(element, features);
            return features;
        }

        IRTypeNode descendTarget(IRField field) {
            IRType reference = field.type();
            if (reference.kind() != IRKind.STRUCT && reference.kind() != IRKind.CHOICE) {
                return null;
            }
            // THE AST HOP LIVES ON THE INDEX (v3.3 seat 9, PR #645 commit 18, round 1 cq SF-4): the
            // workspace declaration map is keyed by the PARSER'S OWN nodes, and IRTypeIndex is the adapter
            // boundary that may hold one - an EMITTER may not, and the sharing lint now says so by prefix.
            // The branches and the null answers are unchanged, moved statement for statement.
            return index.descendTarget(reference);
        }

        /**
         * {@code DeepPathScan.intersectButRetainAttribute} ({@code :230-257}) - INCLUDING the metadata-collapse
         * VALUE SWAP at {@code :250-256}, which is NOT cosmetic (see the class javadoc): the swapped-in value is a
         * DIFFERENT instance, so a LATER round's {@code attr.equals(attributeToRetain)} identity test no longer
         * fires for that key and the key IS removed. The swap decides key membership.
         */
        void intersect(Map<String, Feature> toModify, Map<String, Feature> other, Feature retain) {
            toModify.entrySet().removeIf(entry -> {
                Feature feature = entry.getValue();
                if (feature == retain) {
                    return false;
                }
                Feature otherFeature = other.get(entry.getKey());
                return otherFeature == null || !match(feature, otherFeature);
            });
            if (lie == IRDerivedLie.DEEP_META_COLLAPSE_DROPPED) {
                return;
            }
            for (Map.Entry<String, Feature> entry : toModify.entrySet()) {
                Feature current = entry.getValue();
                Feature otherFeature = other.get(entry.getKey());
                if (otherFeature != null && current.hasMeta && !otherFeature.hasMeta) {
                    entry.setValue(otherFeature);
                }
            }
        }

        /** {@code DeepPathScan.merge} ({@code :259-273}) - its own metadata-collapse value swap included. */
        void merge(Map<String, Feature> toModify, Map<String, Feature> other) {
            other.forEach((name, feature) -> {
                Feature candidate = toModify.get(name);
                if (candidate != null) {
                    if (!match(candidate, feature)) {
                        toModify.remove(name);
                    } else if (candidate.hasMeta && !feature.hasMeta
                            && lie != IRDerivedLie.DEEP_META_COLLAPSE_DROPPED) {
                        toModify.put(name, feature);
                    }
                } else {
                    toModify.put(name, feature);
                }
            });
        }

        /**
         * {@code DeepPathScan.match} ({@code :283-291}) INCLUDING its null-safe arm (round 1, SF-5). The oracle
         * answers {@code ta == tb} when EITHER resolved type is null - so two attributes with no resolved type at
         * all match each other whatever their multiplicity, and an unresolved one never matches a resolved one. The
         * mirror is exact rather than convenient: {@link Feature#absentType} is the IR's own spelling of that null.
         *
         * <p>NO FIXTURE WITNESS EXISTS, and that is a fact about the language rather than a gap here: the only
         * producer of a null resolved type is a choice option with no type call, which no parseable model writes,
         * and a choice option is {@code ONE_TO_ONE} on BOTH halves by construction - so the "different
         * cardinalities" half of the arm is unreachable through the linker. The arm is therefore behaviour-identical
         * to the previous key comparison on everything the corpus can produce (two absent-type options both key
         * {@code STRUCT:} and both read singular), and it is written out so the emitter inherits the oracle's law
         * and not a coincidence.
         */
        boolean match(Feature a, Feature b) {
            if (a.absentType || b.absentType) {
                return a.absentType && b.absentType;
            }
            return a.typeKey.equals(b.typeKey) && a.multi == b.multi;
        }

        String typeKey(IRField field) {
            IRType reference = field.type();
            return reference.kind().name() + ":" + reference.resolvedQualifiedName().orElse(reference.name())
                    + renderArguments(field.typeArguments());
        }
    }

    /**
     * The D13 witness (see {@link IRDerivedLie#DEEP_ORDER_NOT_HASH_ORDER}): the SAME keys in a map that is not a
     * {@code HashMap} over them. The order law is not a function of the key set alone the way a sorted or a
     * declaration order would be - it is the JDK's table walk - so the only way to show that the fact CAN fail is to
     * hand it the same keys in another container. That is precisely the dependency the emitter inherits.
     */
    private Map<String, Feature> reordered(Map<String, Feature> features) {
        if (lie != IRDerivedLie.DEEP_ORDER_NOT_HASH_ORDER) {
            return features;
        }
        List<String> keys = new ArrayList<>(features.keySet());
        Collections.reverse(keys);
        Map<String, Feature> reversed = new LinkedHashMap<>();
        for (String key : keys) {
            reversed.put(key, features.get(key));
        }
        return reversed;
    }

    private static String renderArguments(List<IRTypeArgument> arguments) {
        if (arguments.isEmpty()) {
            return "";
        }
        List<String> rendered = new ArrayList<>();
        for (IRTypeArgument argument : arguments) {
            rendered.add(argument.parameter() + "="
                    + argument.nameValue().orElseGet(() -> (argument.negated() ? "-" : "")
                            + argument.literalValue().orElse("")));
        }
        return "[" + join(rendered) + "]";
    }

    // ----------------------------------------------------------------------------------------- the collision

    /** The {@code java.lang} collision predicate, honouring the D12 lie. */
    boolean collides(String simpleName) {
        return lie != IRDerivedLie.COLLISION_ALWAYS_FALSE && IRJavaLangCollision.collides(simpleName);
    }

    /**
     * The SIMPLE NAME of a property's cast-site item type from the IR alone - the bare Java type's simple name, or
     * the {@code FieldWithMeta…} / {@code ReferenceWithMeta…} wrapper's when the declared field carries attribute
     * meta ({@code RJavaPojoInterface.wrapFieldWithMeta} / {@code wrapReferenceWithMeta}, {@code :658-677}). A
     * reference the type gate REFUSES ({@code pattern}, an unresolved name) has no simple name to compare and is
     * skipped - the refusal itself is already a fact of the declaration channel.
     */
    String itemSimpleName(IRField field, Map<String, Owned> effective, List<IRTypeNode> chain) {
        if (field == null) {
            return null;
        }
        String bare;
        try {
            bare = lastSegment(IRJavaTypeNames.of(field.type(), field.typeArguments()));
        } catch (RuntimeException refused) {
            return null;
        }
        MetaKind kind = metaKind(unionedAnnotations(field, effective, chain));
        if (kind == MetaKind.FIELD_WITH_META) {
            return "FieldWithMeta" + bare;
        }
        if (kind == MetaKind.REFERENCE_WITH_META) {
            return "ReferenceWithMeta" + bare;
        }
        return bare;
    }

    // --------------------------------------------------------------------------------------------- the helpers

    /** A declaration node's simple name: its own name with its namespace stripped. */
    static String simpleName(IRType node) {
        String name = node.name();
        Optional<String> namespace = node.namespace();
        if (namespace.isPresent() && !namespace.get().isEmpty() && name.startsWith(namespace.get() + ".")) {
            return name.substring(namespace.get().length() + 1);
        }
        return node.resolvedName().orElseGet(() -> lastSegment(name));
    }

    static String lastSegment(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : name;
    }

    static String join(List<String> values) {
        return String.join(",", values);
    }

    static String text(OptionalInt value) {
        return value.isPresent() ? Integer.toString(value.getAsInt()) : "-";
    }
}
