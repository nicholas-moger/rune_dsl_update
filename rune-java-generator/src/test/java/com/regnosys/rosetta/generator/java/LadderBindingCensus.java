package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.generator.java.object.ConditionCases;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The LADDER RETIREMENT's count-then-flip instrument (v3.1, plan § 4): for every
 * reference node the C1 authority slots cover, compare what the LEGACY resolution
 * slots hold (the ladder the emitters read today — Path-1 handlers and the IR
 * adapter alike) against what the AUTHORITY slots hold (the C1 Layer-1 answer no
 * emitter reads yet), and emit one row per node-side whose EFFECTIVE binding the
 * flip can change.
 *
 * <h2>What counts as a differing binding</h2>
 *
 * The authority slots are PARTIAL BY DESIGN — C1's arms are additive and fire
 * where the legacy resolution was wrong or absent, so an empty authority slot
 * over a bound legacy slot is the FALLBACK MASS: under the flip's
 * authority-first-with-legacy-fallback read it changes nothing, and emitting it
 * would drown the census in the corpus's agreeing bulk (measured on the control
 * models: every plain bound symbol reads that way). Side classes:
 *
 * <ul>
 *   <li>{@code REBIND} — both views bound, to genuinely different declarations
 *       (the a2 shape: bare {@code Index} — authority = the item's ChoiceOption,
 *       legacy = the same-named GLOBAL choice through the symbol slot).</li>
 *   <li>{@code SAME_DENOTATION} — both bound, different node species denoting the
 *       same declaration THROUGH AN OPTION-TYPE RUNG (T10 / Gap-B store the
 *       option's TYPE where the authority binds the RChoiceOption whose typeCall
 *       names that type). Still a row: emitters key render arms on the species.
 *       Deliberately rung-conditioned — a SYMBOL-slot bind to the same global
 *       type is the a2 REBIND, not a benign representation split (measured: the
 *       two are indistinguishable by node equality alone).</li>
 *   <li>{@code AUTH_ONLY} — authority bound where every legacy rung is empty.</li>
 * </ul>
 *
 * Node-grain relevance filter (measured on the controls, restated from the flip's
 * fallback semantics):
 *
 * <ul>
 *   <li>a SYM/FC side emits its row for any of the three classes;</li>
 *   <li>an EVR node emits its rows if any side is REBIND/SAME_DENOTATION, or the
 *       RIGHT side is AUTH_ONLY (the E1 class: the nav resolved only by
 *       authority — today's render echoed or refused, a mismatching file);</li>
 *   <li>an EVR whose LEFT is AUTH_ONLY while the RIGHT is SILENT is counted
 *       ({@code headOnlyAuthNodes}), not rowed. The silent right has three
 *       sub-cases, each with its own justification: (a) the right AGREES —
 *       the INPUT-NAV MASS, the legacy engine resolved the nav through a
 *       right-side rung and the head was implied by name; (b) the right is
 *       LEGACY-ONLY (a rung like {@code resolvedInputFeature} bound it, the
 *       authority did not) — under the flip's fallback read the right's
 *       source is unchanged and the left renders the same name text; (c) the
 *       right is BOTH-EMPTY — the refusal contract's class, not the flip's.
 *       Declared limit shared by (a) and (b): if the legacy engine implied a
 *       DIFFERENT head whose type carries the same right-side feature
 *       declaration, this reads as silence — the flip-time ring and the
 *       identical-never-drops matrix accounting own that residue.</li>
 *   <li>legacy-bound-authority-empty sides are counted ({@code legacyOnlySides}),
 *       never rowed — byte-inert under the fallback read.</li>
 * </ul>
 *
 * <h2>The legacy view IS the dump's rung order</h2>
 *
 * The per-side rung precedence is {@code ForkResolutionDump}'s slot-to-side
 * mapping (rune-parser, reviewed at C1 as the faithful legacy translation), minus
 * the authority slots it consults first. It is restated here rather than imported
 * because the dump is another module's test tree. The self-test's live controls
 * pin the SYMBOL, CHOICE_OPTION_TYPE and chain/input-feature rungs; the remaining
 * rungs (GAP_A_SYMBOL, CHAIN_ATTR — corpus-exercised by the drr REBIND rows —
 * and the RESTRICTION family) are held by the corpus count pin only, so a
 * mis-restatement among them surfaces as a pin move, not a unit failure.
 *
 * <h2>Declared scope</h2>
 *
 * The three node kinds carrying C1 authority slots: {@link RSymbolReference}
 * (one side), {@link RFeatureCall} (one side), {@link REnumValueRef} (two sides —
 * upstream would have parsed {@code a -> b} as symbol + feature). {@code
 * RDeepFeatureCall} carries NO authority slot (its resolution rides receiver
 * typing) and is deliberately out of scope, mirroring the dump's declared-scope
 * discipline: excluded HERE rather than silently absent.
 *
 * <h2>Affected-file candidates</h2>
 *
 * Each row maps its containing root element to the generated file(s) whose bytes
 * can read the binding, using the generators' OWN naming (never restated paths):
 * function → {@code toFunctionJavaClass}; rule/report → the same dispatcher over
 * the {@code RFunction.fromRule}/{@code fromReport} bridge; a data-type condition
 * → {@code toConditionJavaClass} (the count-all-named law via
 * {@link DataRuleGenerator#casesOf}) PLUS the type's POJO (imports read condition
 * refs — the E1 import-only class has 10 POJO carriers per drr 7.x cell), the POJO
 * path via {@link RJavaPojoInterface} exactly as {@code ModelObjectGenerator}
 * creates it. A container the mapping does not know yields
 * {@code UNMAPPED:<species>} — a loud triage-required candidate, never a skip.
 */
final class LadderBindingCensus {

    private LadderBindingCensus() {}

    enum RowClass { REBIND, SAME_DENOTATION, AUTH_ONLY }

    /** Which legacy rung supplied the side's binding — the triage column and the
     *  same-denotation guard. */
    enum Rung {
        SYMBOL, RESOLVED_FEATURE,
        ENUM, GAP_A_SYMBOL, CHAIN_ATTR, RESTRICTION_LHS,
        ENUM_VALUE, CHOICE_OPTION_TYPE, CHAIN_FEATURE,
        RESTRICTION_TYPE, RESTRICTION_TYPE_SPECULATIVE, INPUT_FEATURE,
        NONE
    }

    /**
     * One node-side where the effective binding can change. {@code side} ∈
     * {@code SYM} / {@code FC} / {@code EVR.left} / {@code EVR.right}.
     * {@code legacy} prints as {@code <rung>=<SimpleClass>:<dotted-fqn>} or
     * {@code UNBOUND}; {@code authority} as {@code <SimpleClass>:<dotted-fqn>}.
     */
    record Row(String cell, RowClass rowClass, String side, String file, int offset,
               String name, String legacy, String authority, String container,
               List<String> candidateFiles) {

        String tsv() {
            return cell + "\t" + rowClass + "\t" + side + "\t" + file + "\t" + offset
                    + "\t" + name + "\t" + legacy + "\t" + authority + "\t" + container
                    + "\t" + String.join(";", candidateFiles);
        }

        static final Comparator<Row> ORDER = Comparator
                .comparing(Row::file)
                .thenComparingInt(Row::offset)
                .thenComparing(Row::side);
    }

    /** The silenced-classes accounting — proves the census SAW what it chose not to row. */
    record Stats(int nodesSeen, int agreeSides, int legacyOnlySides, int headOnlyAuthNodes) {
        Stats plus(Stats other) {
            return new Stats(nodesSeen + other.nodesSeen,
                    agreeSides + other.agreeSides,
                    legacyOnlySides + other.legacyOnlySides,
                    headOnlyAuthNodes + other.headOnlyAuthNodes);
        }
        static final Stats ZERO = new Stats(0, 0, 0, 0);
    }

    record CensusResult(List<Row> rows, Stats stats) {}

    /** The census over one model's root elements (the caller pre-filters to emitting models). */
    static CensusResult censusOfModel(String cell, RModel model, GeneratorModel gm,
                                      JavaTypeTranslator translator, JavaTypeUtil typeUtil) {
        List<Row> rows = new ArrayList<>();
        int[] counters = new int[4]; // nodesSeen, agreeSides, legacyOnlySides, headOnlyAuthNodes
        String file = Paths.get(model.sourceRange().file()).getFileName().toString();
        for (RRootElement element : model.rootElements()) {
            AstWalker.walk(element, node -> {
                if (node instanceof RSymbolReference ref) {
                    counters[0]++;
                    Side side = classify(new Bound(ref.symbol().orElse(null), Rung.SYMBOL),
                            ref.resolvedFeatureNode().orElse(null));
                    tally(counters, side);
                    if (side.rowClass() != null) {
                        rows.add(row(cell, file, "SYM", node, ref.name(), side, gm, translator, typeUtil));
                    }
                } else if (node instanceof RFeatureCall call) {
                    counters[0]++;
                    Side side = classify(
                            new Bound(call.resolvedFeature().orElse(null), Rung.RESOLVED_FEATURE),
                            call.resolvedFeatureNode().orElse(null));
                    tally(counters, side);
                    if (side.rowClass() != null) {
                        rows.add(row(cell, file, "FC", node, call.featureName(), side,
                                gm, translator, typeUtil));
                    }
                } else if (node instanceof REnumValueRef evr) {
                    counters[0]++;
                    Side left = classify(legacyLeft(evr), evr.resolvedHead().orElse(null));
                    Side right = classify(legacyRight(evr), evr.resolvedFeatureNode().orElse(null));
                    tally(counters, left);
                    tally(counters, right);
                    boolean relevant =
                            left.rowClass() == RowClass.REBIND
                            || left.rowClass() == RowClass.SAME_DENOTATION
                            || right.rowClass() != null;
                    if (!relevant && left.rowClass() == RowClass.AUTH_ONLY) {
                        counters[3]++; // the input-nav mass — head implied, right agreed
                    }
                    if (relevant) {
                        if (left.rowClass() != null) {
                            rows.add(row(cell, file, "EVR.left", node, evr.enumName(), left,
                                    gm, translator, typeUtil));
                        }
                        if (right.rowClass() != null) {
                            rows.add(row(cell, file, "EVR.right", node, evr.valueName(), right,
                                    gm, translator, typeUtil));
                        }
                    }
                }
            });
        }
        rows.sort(Row.ORDER);
        return new CensusResult(rows,
                new Stats(counters[0], counters[1], counters[2], counters[3]));
    }

    // =========================================================================
    // Side classification
    // =========================================================================

    private record Bound(RNode node, Rung rung) {
        static final Bound NONE = new Bound(null, Rung.NONE);
    }

    /** A classified side: {@code rowClass == null} means silent (agree / legacy-only / empty). */
    private record Side(RowClass rowClass, Bound legacy, RNode authority) {}

    private static Side classify(Bound legacy, RNode authority) {
        if (authority == null) {
            // Bound-legacy-empty-authority is the fallback mass; both-empty is the
            // refusal contract's class. Neither is a flip-changeable binding.
            return new Side(null, legacy, null);
        }
        if (legacy.node() == null) {
            return new Side(RowClass.AUTH_ONLY, legacy, authority);
        }
        if (legacy.node() == authority) {
            return new Side(null, legacy, authority);
        }
        if (sameDenotation(authority, legacy)) {
            return new Side(RowClass.SAME_DENOTATION, legacy, authority);
        }
        return new Side(RowClass.REBIND, legacy, authority);
    }

    private static void tally(int[] counters, Side side) {
        if (side.rowClass() != null) {
            return;
        }
        if (side.authority() != null || side.legacy().node() == null) {
            counters[1]++; // agree (or both-empty, which the refusal contract owns)
        } else {
            counters[2]++; // legacy-only — the fallback mass
        }
    }

    /**
     * True where the two views denote the SAME declaration through different node
     * species: the authority binds the {@link RChoiceOption} while a legacy rung
     * DOCUMENTED to store the option's TYPE (T10's {@code resolvedChoiceOption},
     * Gap-B's restriction types) holds that type. Rung-conditioned on purpose: a
     * SYMBOL-slot bind to the same-named GLOBAL type is the a2 wrong-binding —
     * node equality alone cannot tell the two apart (measured), the rung can.
     */
    private static boolean sameDenotation(RNode authority, Bound legacy) {
        if (!(authority instanceof RChoiceOption option) || option.typeCall() == null) {
            return false;
        }
        boolean optionTypeRung = legacy.rung() == Rung.CHOICE_OPTION_TYPE
                || legacy.rung() == Rung.RESTRICTION_TYPE
                || legacy.rung() == Rung.RESTRICTION_TYPE_SPECULATIVE;
        return optionTypeRung
                && option.typeCall().referencedType().orElse(null) == legacy.node();
    }

    /**
     * The LEFT-hand legacy rungs of {@code a -> b}, in {@code ForkResolutionDump}'s
     * documented order: resolvedEnum → Gap-A callable → chain attribute →
     * type-restriction LHS.
     */
    private static Bound legacyLeft(REnumValueRef evr) {
        return firstPresent(
                new Bound(evr.enumeration().orElse(null), Rung.ENUM),
                new Bound(evr.resolvedSymbol().orElse(null), Rung.GAP_A_SYMBOL),
                new Bound(evr.resolvedAttributeChain()
                        .flatMap(REnumValueRef.AttributeChain::attributeOpt).orElse(null),
                        Rung.CHAIN_ATTR),
                new Bound(evr.resolvedTypeRestriction()
                        .map(REnumValueRef.TypeRestriction::lhsAttribute).orElse(null),
                        Rung.RESTRICTION_LHS));
    }

    /**
     * The RIGHT-hand legacy rungs: resolvedValue → T10 choice option (the option's
     * TYPE, not the option) → chain feature → verified restriction type →
     * speculative restriction type → typing-only input feature.
     */
    private static Bound legacyRight(REnumValueRef evr) {
        return firstPresent(
                new Bound(evr.enumValue().orElse(null), Rung.ENUM_VALUE),
                new Bound(evr.resolvedChoiceOption().orElse(null), Rung.CHOICE_OPTION_TYPE),
                new Bound(evr.resolvedAttributeChain()
                        .map(REnumValueRef.AttributeChain::feature).orElse(null),
                        Rung.CHAIN_FEATURE),
                new Bound(evr.resolvedTypeRestriction()
                        .map(REnumValueRef.TypeRestriction::restrictionType).orElse(null),
                        Rung.RESTRICTION_TYPE),
                new Bound(evr.resolvedRestrictionType().orElse(null),
                        Rung.RESTRICTION_TYPE_SPECULATIVE),
                new Bound(evr.resolvedInputFeature().orElse(null), Rung.INPUT_FEATURE));
    }

    private static Bound firstPresent(Bound... candidates) {
        for (Bound candidate : candidates) {
            if (candidate.node() != null) {
                return candidate;
            }
        }
        return Bound.NONE;
    }

    // =========================================================================
    // Row construction
    // =========================================================================

    private static Row row(String cell, String file, String side, RNode node, String name,
                           Side classified, GeneratorModel gm, JavaTypeTranslator translator,
                           JavaTypeUtil typeUtil) {
        Container container = containerOf(node);
        String legacy = classified.legacy().node() == null ? "UNBOUND"
                : classified.legacy().rung() + "=" + describe(classified.legacy().node());
        return new Row(cell, classified.rowClass(), side, file,
                node.sourceRange().startOffset(), name,
                legacy, describe(classified.authority()), container.describe(),
                candidatesFor(container, gm, translator, typeUtil));
    }

    // =========================================================================
    // Containing construct + affected-file candidates
    // =========================================================================

    /** The containing root element plus, where the container is a data type, the condition. */
    private record Container(RRootElement element, RCondition condition) {
        String describe() {
            String base = element.getClass().getSimpleName() + ":" + fqnOf(element);
            return condition == null ? base
                    : base + "#" + condition.name().orElse("<unnamed>");
        }
    }

    private static Container containerOf(RNode node) {
        RCondition condition = null;
        RNode current = node;
        while (current != null && !(current.parent() instanceof RModel)) {
            if (current instanceof RCondition c) {
                condition = c;
            }
            current = current.parent();
        }
        if (!(current instanceof RRootElement element)) {
            throw new AssertionError("census node has no root-element ancestor: "
                    + node.getClass().getSimpleName() + " @ " + node.sourceRange());
        }
        return new Container(element, condition);
    }

    private static List<String> candidatesFor(Container container, GeneratorModel gm,
                                              JavaTypeTranslator translator, JavaTypeUtil typeUtil) {
        RRootElement element = container.element();
        if (element instanceof RFunction fn) {
            return List.of(pathOf(translator.toFunctionJavaClass(fn, gm.symbolId(fn))));
        }
        if (element instanceof RRule rule) {
            RFunction fn = RFunction.fromRule(rule);
            return List.of(pathOf(translator.toFunctionJavaClass(fn, gm.symbolId(fn))));
        }
        if (element instanceof RReport report) {
            RFunction fn = RFunction.fromReport(report);
            return List.of(pathOf(translator.toFunctionJavaClass(fn, gm.symbolId(fn))));
        }
        if (element instanceof RDataType dataType) {
            if (container.condition() == null) {
                // No expression position exists on a data type outside its conditions;
                // a row here means the mapping's model of the AST is stale. Loud.
                return List.of("UNMAPPED:RDataType-outside-condition");
            }
            String conditionName = conditionNameOf(dataType, container.condition());
            if (conditionName == null) {
                return List.of("UNMAPPED:RDataType-condition-not-in-casesOf");
            }
            // The datarule renders the condition body; the POJO's import block reads
            // the type's collected refs, which include condition expressions (the E1
            // import-only class). Both are candidates; a conservative extra candidate
            // fails the band join loudly rather than missing a file silently.
            return List.of(
                    pathOf(translator.toConditionJavaClass(gm.symbolId(dataType), conditionName)),
                    pathOf(new RJavaPojoInterface(dataType, gm, translator, typeUtil)));
        }
        return List.of("UNMAPPED:" + element.getClass().getSimpleName());
    }

    /** The generators' own file-path law: {@code canonicalName.withForwardSlashes() + ".java"}. */
    private static String pathOf(com.rosetta.util.types.JavaTypeDeclaration<?> javaClass) {
        return javaClass.getCanonicalName().withForwardSlashes() + ".java";
    }

    /** The condition's class-name suffix per the count-all-named law — from the generator's own casesOf. */
    private static String conditionNameOf(RDataType dataType, RCondition condition) {
        return ConditionCases.casesOf(dataType).stream()
                .filter(c -> c.condition() == condition)
                .map(ConditionCases.ConditionCase::conditionName)
                .findFirst().orElse(null);
    }

    // =========================================================================
    // Node description (species + qualified name)
    // =========================================================================

    private static String describe(RNode target) {
        if (target == null) {
            return "UNBOUND";
        }
        return target.getClass().getSimpleName() + ":" + fqnOf(target);
    }

    /**
     * Namespace-qualified name: the model namespace, then every named container,
     * then the target — {@code ForkResolutionDump.fullyQualifiedName}'s walk
     * (restated for the same cross-module reason as the rung order), including its
     * {@code metaType} exception (upstream's name provider gives a metaType the
     * bare simple name, no namespace), plus an {@code Optional<String> name()}
     * arm the dump does not need.
     */
    private static String fqnOf(RNode target) {
        if (target instanceof com.regnosys.rosetta.ast.types.RMetaType) {
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

    /** A choice option's identity is the type it names; every other named node answers name(). */
    private static String nameOf(RNode node) {
        if (node instanceof RChoiceOption option) {
            return option.typeCall() == null ? null : option.typeCall().typeName();
        }
        try {
            Method name = node.getClass().getMethod("name");
            if (name.getReturnType() == String.class) {
                return (String) name.invoke(node);
            }
            if (name.getReturnType() == java.util.Optional.class) {
                Object value = name.invoke(node);
                if (value instanceof java.util.Optional<?> optional
                        && optional.orElse(null) instanceof String s) {
                    return s;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Not a named node — contributes no segment.
        }
        return null;
    }
}
