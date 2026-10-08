package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.enums.QualifiableKind;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.model.RQualifiableConfig;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Validates function declarations:
 * - Extension functions must match parent's parameter count and types
 * - Extension functions must match parent's output type
 * - Bodyless functions carry the {@code codeImplementation} annotation
 *   (facet warningFamilyWaves, PR #455)
 * - Dispatch families cover their enumeration's values (PR #455)
 *
 * <p>Upstream: FunctionExtensionValidator.checkExtendedFunctionParameters,
 * FunctionExtensionValidator.checkExtendedFunctionOutput,
 * FunctionValidator.warnWhenEmptyFunctionsDontHaveCodeImplementationAnnotation,
 * RosettaSimpleValidator.checkDispatch.
 */
public final class FunctionValidator implements Validator {

    /** The pass's models in load order (v3.2 seat 4, F10) — the first-wins root is read over them. */
    private List<RModel> models = List.of();
    /**
     * The two first-wins roots, computed ONCE per bind (round 2, the code-quality review's SF-6 — the same
     * cache the meta generator carries): the pass's models are fixed for the run, and {@code bindModels} is
     * the hook the seat added for exactly this cross-model read. Both {@code null} until bound.
     */
    private RDataType productRoot;
    private RDataType eventRoot;

    @Override
    public void bindModels(List<RModel> modelsInLoadOrder) {
        this.models = List.copyOf(modelsInLoadOrder);
        this.productRoot = RQualifiableConfig.firstRoot(models, QualifiableKind.IS_PRODUCT).orElse(null);
        this.eventRoot = RQualifiableConfig.firstRoot(models, QualifiableKind.IS_EVENT).orElse(null);
    }

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        if (!(element instanceof RFunction fn)) return;
        checkQualificationRoot(fn, collector);

        if (fn.superFunction().isPresent()) {
            checkExtensionParameters(fn, fn.superFunction().get(), collector);
            checkExtensionOutput(fn, fn.superFunction().get(), collector);
        }
        checkCodeImplementation(fn, collector);
        checkDispatchCoverage(fn, collector);
    }

    /**
     * Upstream {@code RosettaSimpleValidator.checkQualificationAnnotation}'s WARNING (v3.2 seat 4, PR #625, F10 —
     * the released message bytes): a {@code [qualification]} function whose first input type is not the
     * first-wins qualifiable root of EITHER kind ({@link RQualifiableConfig#firstRoot} over the pass's
     * models in load order — the ONE declaration the meta generator reads too, LAW 69) is warned about,
     * exactly as every losing namespace's qualifier is in the chaos cell's s22 (upstream's measured
     * {@code QUAL-ROOT-FIRST-WINS} class). Its sibling ERRORS in that check (one annotation, one input, a
     * boolean output) have no corpus carrier and are not ported here (the #614 law).
     *
     * <p>Round 1, two proposals REFUTED by upstream's source (the 9.83.0 source vendored in-tree at {@code rune-dsl/rune-lang}:
     * {@code RosettaSimpleValidator.checkQualificationAnnotation} line 1016 → {@code RosettaConfigExtension.isRootEventOrProduct})
     * and, at round 2, by the RELEASED 9.83.0 {@code rune-lang} jar's bytecode ({@code javap -c}: the identical
     * null-name guard and two-{@code Objects.equals} disjunction — released bytecode == the post-9.83 source here):
     * (1) the check IS the disjunction — {@code isRootEventOrProduct(type)} is true when the input equals the
     * event root OR the product root, the annotation's kind never reaching it — so a {@code [qualification Product]}
     * function taking the event root is not warned about upstream, and not here (b2's fixture puts each kind on
     * its own root, the only shape the corpus carries); (2) a workspace declaring NO configuration of either
     * kind warns on EVERY qualifier upstream — {@code Objects.equals(type, null)} is false for both roots — so
     * no null guard is added here (b5 witnesses the warning with no root declared; lane P). The anchor is the
     * one thing this port cannot witness: upstream anchors {@code FUNCTION__INPUTS} on the function, the fork the
     * first input's type call; the oracle log carries no positions (b4 pins the file alone).
     */
    private void checkQualificationRoot(RFunction fn, ValidationCollector collector) {
        boolean qualifier = fn.annotationRefs().stream()
                .anyMatch(a -> "qualification".equals(a.annotationName()));
        if (!qualifier || fn.inputs().isEmpty() || fn.inputs().get(0).typeCall() == null) {
            return;
        }
        RAttribute input = fn.inputs().get(0);
        RNode inputType = input.typeCall().referencedType().orElse(null);
        if (inputType == null) {
            return; // unresolved: the linker's own error stands
        }
        if (inputType != productRoot && inputType != eventRoot) {
            collector.warning(input.typeCall().sourceRange(),
                "Input type does not match qualification root type.",
                ValidationIssueCode.QUALIFICATION_ROOT_MISMATCH);
        }
    }

    /**
     * Upstream {@code FunctionValidator.warnWhenEmptyFunctionsDontHaveCode-
     * ImplementationAnnotation} (released bytecode == vendored source,
     * javap-verified): a function with a NAMED output and an empty operations
     * list that lacks the {@code codeImplementation} annotation fires
     * {@code A function should specify an implementation, or they should be
     * annotated with codeImplementation}; the inverse (operations present +
     * annotated) fires {@code Functions annotated with codeImplementation
     * should not have any setter operations as they will be overriden} —
     * both WARNINGS anchored at the function name (the fork anchors the name
     * token since the PR #458 anchor wave — column-exact against the V0
     * banks at the mojo seam). Dispatch variants
     * never fire: they declare no own output and carry operations. The cdm
     * bank witnesses 8 lines of the first form (the ingest-fpml spec
     * shells); the dispatch BASES are annotated {@code codeImplementation}
     * in the corpus and stay silent.
     */
    private void checkCodeImplementation(RFunction fn, ValidationCollector collector) {
        if (fn.output().isEmpty() || fn.output().get().name() == null) {
            return;
        }
        boolean hasCodeImpl = fn.annotationRefs().stream()
                .anyMatch(a -> "codeImplementation".equals(a.annotationName()));
        if (fn.operations().isEmpty() && !hasCodeImpl) {
            collector.warning(
                nameAnchor(fn),
                "A function should specify an implementation, or they should be annotated with codeImplementation",
                ValidationIssueCode.MISSING_IMPLEMENTATION);
        }
        if (!fn.operations().isEmpty() && hasCodeImpl) {
            collector.warning(
                nameAnchor(fn),
                "Functions annotated with codeImplementation should not have any setter operations as they will be overriden",
                ValidationIssueCode.MISSING_IMPLEMENTATION);
        }
    }

    /**
     * Upstream {@code RosettaSimpleValidator.checkDispatch} (the
     * missing-implementation family — released message recipe
     * {@code Missing implementation for : } verified): for a
     * BASE function's same-file same-name dispatch variants
     * ({@code getDispatchingFunctions} = {@code getSiblingsOfType} — the
     * same same-file law {@link RFunction#dispatchBase()} mirrors), group
     * the dispatch values by enumeration, take the enum with the most
     * distinct implemented values, and warn
     * {@code Missing implementation for <Enum>: <absent values, sorted,
     * ", "-joined>} when the enum's value set (own + inherited — upstream
     * {@code getAllEnumValues}) is not covered, anchored at the base
     * function. The sibling {@code Wrong <used> enumeration used. Expecting
     * <mostUsed>.} ERROR arm fires per dispatch on any minority enum —
     * corpus-zero (the V0 streams carry no errors); upstream's second ERROR
     * sibling ({@code Dupplicate usage of <Enum> enumeration value.} — the
     * released bytes carry the double-p) is corpus-zero too and UN-landed
     * (Seat-1 #455 OBS: the recorded ERROR-side follow-on). Both banks
     * witness 3 missing-impl lines each (the daycount/floating-rate
     * dispatch bases; the drr cell's transitive cdm 5.38.0 carries wider
     * enum value sets, which the mechanism derives per cell). Grouping
     * corner (Seat-1 #455 OBS): the fork groups by the value's DECLARING
     * enumeration where upstream groups by the dispatch ref's WRITTEN
     * enumeration — divergent only for a dispatch naming an inherited
     * parent-enum value (corpus-empty).
     */
    private void checkDispatchCoverage(RFunction fn, ValidationCollector collector) {
        if (fn.dispatch().isPresent() || fn.name() == null) {
            return;
        }
        List<RFunction> dispatches = sameFileDispatches(fn);
        if (dispatches.isEmpty()) {
            return;
        }
        // enum -> (value name -> dispatches naming it); LinkedHashMap keeps
        // document order (upstream's HashMap tie-break is unspecified; the
        // corpus families are single-enum).
        Map<REnumeration, Map<String, List<RFunction>>> byEnum = new LinkedHashMap<>();
        for (RFunction fd : dispatches) {
            REnumValue value = fd.dispatch().get().dispatchValue().orElse(null);
            if (value == null) {
                continue;
            }
            REnumeration en = AstWalker.findAncestor(value, REnumeration.class).orElse(null);
            if (en == null) {
                continue;
            }
            byEnum.computeIfAbsent(en, k -> new LinkedHashMap<>())
                    .computeIfAbsent(value.name(), k -> new ArrayList<>())
                    .add(fd);
        }
        if (byEnum.isEmpty()) {
            return;
        }
        REnumeration mostUsed = null;
        int maxSize = -1;
        for (Map.Entry<REnumeration, Map<String, List<RFunction>>> e : byEnum.entrySet()) {
            if (e.getValue().size() > maxSize) {
                maxSize = e.getValue().size();
                mostUsed = e.getKey();
            }
        }
        Set<String> toImplement = new TreeSet<>(allEnumValueNames(mostUsed));
        toImplement.removeAll(byEnum.get(mostUsed).keySet());
        if (!toImplement.isEmpty()) {
            collector.warning(
                nameAnchor(fn),
                "Missing implementation for " + mostUsed.name() + ": "
                        + String.join(", ", toImplement),
                ValidationIssueCode.MISSING_IMPLEMENTATION);
        }
        for (Map.Entry<REnumeration, Map<String, List<RFunction>>> e : byEnum.entrySet()) {
            if (e.getKey() == mostUsed) {
                continue;
            }
            for (List<RFunction> fds : e.getValue().values()) {
                for (RFunction fd : fds) {
                    collector.error(
                        fd.sourceRange(),
                        "Wrong " + e.getKey().name() + " enumeration used. Expecting "
                                + mostUsed.name() + ".",
                        ValidationIssueCode.MISSING_ENUM_VALUE);
                }
            }
        }
    }

    /**
     * The function-NAME token anchor (upstream sites both implementation
     * warnings at the name feature — column 6 for a top-level {@code func },
     * per the mojo-seam stream measure against the V0 banks; PR #458 anchor
     * wave). Falls back to the node range for synthetic functions.
     */
    private static com.regnosys.rosetta.ast.SourceRange nameAnchor(RFunction fn) {
        return fn.tokenRanges().getOrDefault("name", fn.sourceRange());
    }

    /** Same-file dispatch variants sharing the base's name (document order). */
    private static List<RFunction> sameFileDispatches(RFunction base) {
        RNode p = base.parent();
        while (p != null && !(p instanceof RModel)) {
            p = p.parent();
        }
        List<RFunction> result = new ArrayList<>();
        if (p instanceof RModel model) {
            for (RRootElement elem : model.rootElements()) {
                if (elem instanceof RFunction sibling && sibling.dispatch().isPresent()
                        && base.name().equals(sibling.name())) {
                    result.add(sibling);
                }
            }
        }
        return result;
    }

    /** Own + inherited value names (upstream {@code getAllEnumValues}), cycle-guarded. */
    private static Set<String> allEnumValueNames(REnumeration en) {
        Set<String> names = new HashSet<>();
        Set<REnumeration> seen = new HashSet<>();
        REnumeration current = en;
        while (current != null && seen.add(current)) {
            for (REnumValue v : current.values()) {
                names.add(v.name());
            }
            current = current.superType().orElse(null);
        }
        return names;
    }

    private void checkExtensionParameters(RFunction child, RFunction parent,
                                           ValidationCollector collector) {
        List<RAttribute> childInputs = child.inputs();
        List<RAttribute> parentInputs = parent.inputs();

        if (childInputs.size() != parentInputs.size()) {
            collector.error(
                child.sourceRange(),
                "Function '" + child.name() + "' extends '" + parent.name() +
                    "' but has " + childInputs.size() + " parameters (expected " +
                    parentInputs.size() + ")",
                ValidationIssueCode.CHANGED_EXTENDED_FUNCTION_PARAMETERS);
            return;
        }

        for (int i = 0; i < childInputs.size(); i++) {
            String childType = typeName(childInputs.get(i));
            String parentType = typeName(parentInputs.get(i));
            if (childType != null && parentType != null && !childType.equals(parentType)) {
                collector.error(
                    childInputs.get(i).sourceRange(),
                    "Parameter '" + childInputs.get(i).name() + "' has type '" +
                        childType + "' but parent declares '" + parentType + "'",
                    ValidationIssueCode.CHANGED_EXTENDED_FUNCTION_PARAMETERS);
            }
        }
    }

    private void checkExtensionOutput(RFunction child, RFunction parent,
                                       ValidationCollector collector) {
        String childOut = child.output().map(this::typeName).orElse(null);
        String parentOut = parent.output().map(this::typeName).orElse(null);

        if (childOut != null && parentOut != null && !childOut.equals(parentOut)) {
            collector.error(
                child.output().map(RAttribute::sourceRange).orElse(child.sourceRange()),
                "Output type '" + childOut + "' does not match parent output '" + parentOut + "'",
                ValidationIssueCode.TYPE_ERROR);
        }
    }

    private String typeName(RAttribute attr) {
        if (attr.typeCall() == null) return null;
        return attr.typeCall().typeName();
    }
}
