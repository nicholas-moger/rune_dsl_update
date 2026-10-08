package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathUtilGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.ValidatorScan;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE RELOCATED DERIVATIONS AND THE TWO NEW FACTS (v3.3 seat 9, PR #645 commit 12).
 *
 * <p><b>THE RELOCATION'S OWN TEST IS {@code IRDerivedGateTest}</b>: the D01-D14 lanes drive the reconcile with one
 * named law of one half disabled and read RED on that family and on nothing else. They pass UNCHANGED after the IR
 * half moved to {@link IRDerivedFacts}, which is the statement that the move changed no law - and the D11 DERIVED
 * line's {@code carried} sub-count is the same statement over the whole corpus.
 *
 * <p>What this class adds is the POSITIVE reading of the two fact families commit 12 introduces, each held against
 * the OLD GENERATOR'S OWN seam rather than against a second opinion about it (LAW 69). The RED proofs of the two -
 * the chain-link law in place of the alias-link law (P16) and the inverted wildcard law (P17) - are the lane
 * runner's, because a lie about them has no test seam to ride: it is a source edit, which is exactly what a lane is.
 */
class IRDerivedFactsTest {

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    /**
     * FACT 1 - {@code typeFormat.<p>.aliasConditionClasses}. The condition classes the type-format wing wires for
     * each property, from the IR's alias links, are the CANONICAL NAMES the generator's own
     * {@code wiredConditionClasses} seam returns: the alias's namespace with {@code validation.datarule} below it,
     * escaped, and {@code <AliasName><ConditionName>} under the count-all-named law.
     *
     * <p>The seven {@code alias-conditions*} batteries are the only place outside the chaos cell where the list is
     * NON-EMPTY, so the test asserts a positive witness count as well as the per-property equality - a law that
     * agreed only about empty lists would not be a law about anything.
     */
    @Test
    void theAliasConditionClassNamesAreTheGeneratorsOwnWiredClassesOnEveryAliasConditionBattery() {
        List<String> groups = List.of("alias-conditions", "alias-conditions-filescope", "alias-conditions-header",
                "alias-conditions-meta", "alias-conditions-reserved", "alias-conditions-scope",
                "alias-conditions-twins");
        int properties = 0;
        int wiredProperties = 0;
        int wiredClasses = 0;
        for (String group : groups) {
            IRPropertyModelTest.HoldOutWorkspace workspace = IRPropertyModelTest.holdOutWorkspace(group);
            TypeFormatValidatorGenerator seam =
                    new TypeFormatValidatorGenerator(workspace.gm(), TYPE_TRANSLATOR, TYPE_UTIL);
            IRDerivedFacts facts = new IRDerivedFacts(workspace.index(), IRDerivedLie.NONE);
            for (RModel model : workspace.models()) {
                for (var element : model.rootElements()) {
                    if (!(element instanceof RDataType dataType)) {
                        continue;
                    }
                    Map<String, List<String>> source = new LinkedHashMap<>();
                    seam.wiredConditionClasses(dataType).forEach((name, classes) -> {
                        List<String> canonical = new ArrayList<>();
                        for (RGeneratedJavaClass<?> conditionClass : classes) {
                            canonical.add(conditionClass.getCanonicalName().withDots());
                        }
                        source.put(name, canonical);
                    });
                    IRTypeNode node = workspace.index().node(model.namespace(), dataType);
                    List<IRTypeNode> chain = facts.chain(node);
                    Map<String, IRField> declared = IRDerivedFacts.declaredFieldsByProperty(node, chain);
                    for (Map.Entry<String, List<String>> entry : source.entrySet()) {
                        IRField field = declared.get(entry.getKey());
                        assertTrue(field != null || entry.getValue().isEmpty(),
                                group + "/" + dataType.name() + "." + entry.getKey()
                                        + ": the source wires condition classes for a property the IR has no"
                                        + " declared field for");
                        List<String> ir = field == null ? List.of()
                                : IRDerivedFacts.aliasConditionClassNames(field);
                        assertEquals(entry.getValue(), ir,
                                group + "/" + dataType.name() + "." + entry.getKey()
                                        + ": the wired condition classes, the generator's own against the IR's"
                                        + " alias-link law");
                        properties++;
                        if (!ir.isEmpty()) {
                            wiredProperties++;
                            wiredClasses += ir.size();
                        }
                    }
                }
            }
        }
        System.out.println("aliasConditionClasses: " + properties + " propert(ies), " + wiredProperties
                + " wired, " + wiredClasses + " condition class(es)");
        assertTrue(properties > 0, "the batteries carry properties at all");
        assertTrue(wiredProperties > 0, "at least one property WIRES a condition class - a law that only ever"
                + " agreed about empty lists would state nothing");
    }

    /**
     * FACT 2 - {@code cardinality.<p>.castType}. The cast text at every check site: the old scan's own answer
     * through the {@code ValidatorScan.castType} seam against {@code IRDataTypeEmitter.interfaceGetterType}, which
     * is what the three validator emitters render and what the POJO member writes at its getters.
     *
     * <p>The seat fixtures carry the shapes the law turns on - a list of model types ({@code List<? extends X>}),
     * a list of basics ({@code List<X>}), a meta-wrapped single and the {@code java.lang} collision - so the test
     * asserts that each of those spellings was actually SEEN, not merely that the two halves agreed.
     */
    @Test
    void theCastTextOfEveryMemberIsTheOldScansOwnOnTheSeatFixtures() {
        int members = 0;
        boolean sawWildcardList = false;
        boolean sawPlainList = false;
        boolean sawSingle = false;
        for (IRPropertyModelTest.Fixture f : List.of(IRPropertyModelTest.fixture(),
                IRPropertyModelTest.collisionFixture(), IRPropertyModelTest.settersFixture())) {
            IRDerivedFacts facts = new IRDerivedFacts(f.index(), IRDerivedLie.NONE);
            for (RModel model : f.gm().workspace().files()) {
                for (var element : model.rootElements()) {
                    if (!(element instanceof RDataType dataType)) {
                        continue;
                    }
                    RJavaPojoInterface pojo =
                            ValidatorScan.toPojo(dataType, f.gm(), TYPE_TRANSLATOR, TYPE_UTIL);
                    List<ValidatorScan.ScannedAttribute> scanned =
                            ValidatorScan.scan(dataType, pojo, f.gm(), TYPE_UTIL);
                    IRTypeNode node = f.index().node(model.namespace(), dataType);
                    List<IRPropertyModel.IRProperty> irMembers =
                            facts.members(IRPropertyModel.of(node, f.index()));
                    assertEquals(scanned.size(), irMembers.size(),
                            dataType.name() + ": the two member lists are the same length before the cast"
                                    + " texts are paired");
                    for (int i = 0; i < scanned.size(); i++) {
                        String source = ValidatorScan.castType(scanned.get(i).prop(), TYPE_UTIL);
                        String ir = IRDataTypeEmitter.interfaceGetterType(irMembers.get(i));
                        assertEquals(source, ir, dataType.name() + "." + scanned.get(i).name()
                                + ": the cast text at a check site");
                        members++;
                        // THE LIST TOKEN IS A D50 SENTINEL, never the literal "List<" (typeRefOrBare wraps it),
                        // so the shape is read off the ENVELOPE: a list cast ends in '>' and a single does not.
                        // The first reading of this test tested for "List<" and answered plainList=false on a
                        // fixture that declares `items string (0..*)` - corrected FROM THAT PRINT.
                        boolean list = ir.endsWith(">");
                        sawWildcardList |= list && ir.contains("<? extends ");
                        sawPlainList |= list && !ir.contains("<? extends ");
                        sawSingle |= !list;
                    }
                }
            }
        }
        System.out.println("castType: " + members + " member(s) compared; wildcardList=" + sawWildcardList
                + " plainList=" + sawPlainList + " single=" + sawSingle);
        assertTrue(members > 0, "the fixtures carry members at all");
        assertTrue(sawWildcardList, "a list of MODEL types (List<? extends X>) was among them");
        assertTrue(sawSingle, "a SINGLE-valued member was among them");
        assertTrue(sawPlainList, "a list of BASICS (List<X>, no wildcard) was among them");
    }

    /**
     * LANE X3 (the planning review's Q5): a unit that REFUSES a type must send that element down the INHERITED
     * generator's path - once, and with the inherited generator's own errors surfaced - never convert the refusal
     * into an error of its own and write nothing, which is what commit 11's branch did and what would have left a
     * hole where the OFF route writes a file.
     *
     * <p>The arm is unreachable through {@code route} while a member is missing, so the witness drives
     * {@code routeWith} with a unit whose six members are all declared ready and whose first member throws.
     */
    @Test
    void laneX3ARefusedTypeTakesTheInheritedPathOncePerElementAndItsErrorsSurface() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.fixture();
        IRUnitPass pass = new IRUnitPass(f.gm(), TYPE_UTIL, IRTypeUnit.Member.CARDINALITY_VALIDATOR);

        java.util.EnumMap<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> refusing =
                new java.util.EnumMap<>(IRTypeUnit.Member.class);
        for (IRTypeUnit.Member member : IRTypeUnit.Member.values()) {
            refusing.put(member, node -> {
                throw new IllegalStateException("the witness refuses every type");
            });
        }
        IRTypeUnit unit = new IRTypeUnit(refusing);
        assertTrue(unit.available(), "all six are DECLARED ready - the refusal comes from the render");

        List<RDataType> dataTypes = new ArrayList<>();
        for (var element : f.lib().rootElements()) {
            if (element instanceof RDataType dataType) {
                dataTypes.add(dataType);
            }
        }
        assertFalse(dataTypes.isEmpty(), "the fixture declares data types for the witness to refuse");

        AtomicInteger inheritedCalls = new AtomicInteger();
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = pass.routeWith(unit, dataTypes.stream(), f.lib(), "0.0.0", output,
                element -> TYPE_TRANSLATOR.toValidatorClass(new com.rosetta.model.lib.ModelSymbolId(
                        com.rosetta.util.DottedPath.splitOnDots(f.lib().namespace()),
                        ((RDataType) element).name())),
                (element, representation, version) -> {
                    inheritedCalls.incrementAndGet();
                    if ("Leaf".equals(((RDataType) element).name())) {
                        throw new GenerationException("the inherited generator's own refusal", null, element);
                    }
                    return "// the inherited generator wrote " + ((RDataType) element).name() + "\n";
                });

        assertEquals(dataTypes.size(), inheritedCalls.get(),
                "the INHERITED path runs ONCE per refused element - not zero times (commit 11's hole), not twice");
        assertEquals(dataTypes.size() - 1, output.size(),
                "every refused element but the one the inherited generator itself refused wrote its file");
        assertEquals(1, errors.size(),
                "the INHERITED generator's own error is the one that surfaces - the unit's Refusal never lands in"
                        + " errors on its own");
        assertEquals("the inherited generator's own refusal", errors.get(0).getMessage());
        assertTrue(pass.filesWrittenByIrEmitter().isEmpty(),
                "a refused element's file is the OLD generator's - the IR emitter claims none of them");
        assertEquals(dataTypes.size(), pass.refusedTypes().size(), "every type was refused, by name");
        assertEquals(pass.attemptedTypes(), pass.refusedTypes(), "and every attempted type was a refused one");
    }

    // -------------------------------------------------- the two fact families of v3.3 seat 9, PR #645 commit 13

    /**
     * FACT 3 - {@code meta.qualifyFunctions}. The ORDERED list of qualification-function CLASS CANONICALS the
     * {@code *Meta} file writes into {@code getQualifyFunctions}: the generator's OWN answer through the
     * {@code collectQualifyFunctions} seam (opened {@code public} at this commit, the banked cq SF-3 of PR #644)
     * against {@link IRModelMetaEmitter#qualifyFunctionClasses}, which is the very method the emitter renders from
     * (ONE declaration, TWO callers - LAW 69).
     *
     * <p>The six {@code qualify-*} batteries are the only place outside the cdm cells where the list is NON-EMPTY,
     * so the test asserts a positive witness count as well as the per-element equality. It also asserts the
     * CROSS-MODEL witness: at least one matched function must be declared in a model OTHER than the root's, which
     * is what a walk of the element's own model alone would miss (lane P19).
     */
    @Test
    void theQualifyFunctionClassesAreTheGeneratorsOwnOnEveryQualifyBattery() {
        List<String> groups = List.of("qualify-cross-namespace-input", "qualify-event-and-product",
                "qualify-first-wins", "qualify-order-probe-a", "qualify-order-probe-b",
                "zz-qualify-order-probe-c");
        int elements = 0;
        int roots = 0;
        int entries = 0;
        int crossModelEntries = 0;
        for (String group : groups) {
            IRPropertyModelTest.HoldOutWorkspace workspace = IRPropertyModelTest.holdOutWorkspace(group);
            ModelMetaGenerator seam = new ModelMetaGenerator(workspace.gm(), TYPE_TRANSLATOR);
            IRModelIndex modelIndex = new IRModelIndex(workspace.gm().workspace(),
                    new IRDeclarationReconciler(workspace.gm()).adapter());
            for (RModel model : workspace.models()) {
                for (var element : model.rootElements()) {
                    if (!(element instanceof RDataType dataType)) {
                        continue;
                    }
                    List<String> source = new ArrayList<>();
                    for (ModelMetaGenerator.QualifyRef ref : seam.collectQualifyFunctions(dataType)) {
                        source.add(ref.fqn());
                    }
                    IRTypeNode node = workspace.index().node(model.namespace(), dataType);
                    List<String> ir = IRModelMetaEmitter.qualifyFunctionClasses(node, modelIndex);
                    assertEquals(source, ir, group + "/" + dataType.name()
                            + ": the qualify function classes, the generator's own against the emitter's IR law");
                    elements++;
                    if (!ir.isEmpty()) {
                        roots++;
                        entries += ir.size();
                        // the CROSS-MODEL witness: the function class lives under a namespace other than the
                        // root's own, so a walk of the root's own model could not have found it
                        String ownFunctions = IRDataTypeEmitter.packageOf(node) + ".functions.";
                        for (String qualifyClass : ir) {
                            if (!qualifyClass.startsWith(ownFunctions)) {
                                crossModelEntries++;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("qualifyFunctions: " + elements + " element(s), " + roots + " root(s), "
                + entries + " function class(es), " + crossModelEntries + " of them cross-model");
        assertTrue(elements > 0, "the batteries carry data types at all");
        assertTrue(roots > 0, "at least one ROOT carries a non-empty qualify list - a law that only ever agreed"
                + " about Collections.emptyList() would state nothing");
        assertTrue(crossModelEntries > 0, "at least one matched qualification function is declared OUTSIDE the"
                + " root's own namespace - the WORKSPACE-WIDE walk in load order is the law, and a walk of the"
                + " element's own model alone would agree with the seam on every other entry");
    }

    // -------------------------------------------------- the two fact families of v3.3 seat 9, PR #645 commit 14

    /**
     * FACT 5 - {@code deepPath.dependencies}. The injected sibling utils' CANONICAL NAMES in the ITERATION ORDER
     * of the set they live in: the generator's OWN answer through the {@code dependenciesOf} seam opened at this
     * commit (visibility and a projection of its own {@code HashSet}) against
     * {@link IRDeepPathUtilEmitter#dependencyCanonicals}, which is the very method the emitter renders the fields,
     * the constructor parameters and the assignments from (ONE declaration, TWO callers - LAW 69).
     *
     * <p><b>WHY THE ORDER IS THE WHOLE FACT.</b> The order is neither alphabetical nor declaration order: it is a
     * JDK {@code HashMap} table walk over {@code Objects.hash(packageName, simpleName)}. A list comparison of a
     * SORTED set would agree whatever either side did; this one compares the walks.
     *
     * <p>The population is every data type of the nine hold-out groups that carry a deep-path golden PLUS the
     * seat-9 injection fixture, which is the only workspace in this build where the list is non-empty at all -
     * the corpus witness ({@code ObservableDeepPathUtil}) lives in the cdm6 cells, which the D11 rings reach and a
     * hold-out group cannot. The test asserts a positive witness count as well as the per-element equality.
     */
    @Test
    void theDeepPathDependenciesAreTheGeneratorsOwnSetInItsOwnOrder() {
        int[] tally = new int[3];   // {elements, elements with a dependency, dependencies in all}
        for (String group : IRDeepPathUtilEmitterTest.HOLDOUT_GROUPS) {
            IRPropertyModelTest.HoldOutWorkspace workspace = IRPropertyModelTest.holdOutWorkspace(group);
            countDeepPathFacts(group, workspace.gm(), workspace.index(), tally);
        }
        IRDeepPathUtilEmitterTest.InjectionFixture fixture = IRDeepPathUtilEmitterTest.injectionFixture();
        countDeepPathFacts("seat9.deeppath", fixture.gm(), fixture.index(), tally);

        System.out.println("deepPath.dependencies / arms: " + tally[0] + " element(s), " + tally[1]
                + " with a dependency, " + tally[2] + " dependency edge(s) in all");
        assertTrue(tally[0] > 0, "the batteries carry validated elements at all");
        assertTrue(tally[1] > 0, "at least one element INJECTS a sibling util - a law that only ever agreed about"
                + " the empty list would state nothing about the order it exists for");
        assertTrue(tally[2] >= 2, "and at least one of them injects TWO, which is the only shape in which the"
                + " HashSet iteration order differs from every order a re-derivation could produce");
    }

    /**
     * FACT 6 - {@code deepPath.<feature>.arms}. The three decisions {@code renderAlternativeExpr} takes for every
     * (deep feature x alternative) pair, in RENDER ORDER: the {@code self} / {@code deeper} / {@code direct} arm,
     * the meta unwrap and the feature-side divergence unwrap. The generator's own {@code armsOf} seam against
     * {@link IRDeepPathUtilEmitter#armDecisions}. Both are projections of ONE decision method on their own side,
     * so this compares two derivations and never one with itself.
     *
     * <p>The DEEPER arm and the meta unwrap have no hold-out witness (READ: the 13 deep-path goldens carry neither
     * an injected field nor the string {@code "Type coercion"}), so the seat-9 injection fixture supplies them and
     * the test asserts each arm was actually taken.
     */
    @Test
    void theDeepPathArmsAreTheGeneratorsOwnDecisionsInRenderOrder() {
        Map<String, Integer> byArm = new LinkedHashMap<>();
        int pairs = 0;
        int metaUnwraps = 0;
        List<String> workspaces = new ArrayList<>(IRDeepPathUtilEmitterTest.HOLDOUT_GROUPS);
        for (String group : workspaces) {
            IRPropertyModelTest.HoldOutWorkspace workspace = IRPropertyModelTest.holdOutWorkspace(group);
            DeepPathUtilGenerator seam =
                    new DeepPathUtilGenerator(workspace.gm(), TYPE_TRANSLATOR, TYPE_UTIL);
            IRDeepPathUtilEmitter emitter = new IRDeepPathUtilEmitter(workspace.index(),
                    new IRDerivedFacts(workspace.index(), IRDerivedLie.NONE));
            for (RModel model : workspace.models()) {
                for (var element : model.rootElements()) {
                    if (!(element instanceof RDataType dataType)) {
                        continue;
                    }
                    IRTypeNode node = workspace.index().node(model.namespace(), dataType);
                    List<String> source = seam.armsOf(dataType);
                    assertEquals(source, emitter.armDecisions(node), group + "/" + dataType.name()
                            + ": the arms, the generator's own decisions against the emitter's IR law");
                    for (String arm : source) {
                        pairs++;
                        byArm.merge(arm.substring(arm.indexOf('=') + 1, arm.indexOf("|unwrap=")), 1,
                                Integer::sum);
                        if (arm.contains("|unwrap=true")) {
                            metaUnwraps++;
                        }
                    }
                }
            }
        }
        IRDeepPathUtilEmitterTest.InjectionFixture fixture = IRDeepPathUtilEmitterTest.injectionFixture();
        DeepPathUtilGenerator fixtureSeam =
                new DeepPathUtilGenerator(fixture.gm(), TYPE_TRANSLATOR, TYPE_UTIL);
        IRDeepPathUtilEmitter fixtureEmitter = new IRDeepPathUtilEmitter(fixture.index(),
                new IRDerivedFacts(fixture.index(), IRDerivedLie.NONE));
        for (RModel model : fixture.models()) {
            for (var element : model.rootElements()) {
                if (!(element instanceof RDataType dataType)) {
                    continue;
                }
                IRTypeNode node = fixture.index().node(model.namespace(), dataType);
                List<String> source = fixtureSeam.armsOf(dataType);
                assertEquals(source, fixtureEmitter.armDecisions(node), "seat9.deeppath/" + dataType.name()
                        + ": the arms, the generator's own decisions against the emitter's IR law");
                for (String arm : source) {
                    pairs++;
                    byArm.merge(arm.substring(arm.indexOf('=') + 1, arm.indexOf("|unwrap=")), 1, Integer::sum);
                    if (arm.contains("|unwrap=true")) {
                        metaUnwraps++;
                    }
                }
            }
        }
        System.out.println("deepPath.arms: " + pairs + " (feature x alternative) pair(s) " + byArm
                + " ; meta unwraps " + metaUnwraps);
        assertTrue(pairs > 0, "the batteries carry deep features at all");
        assertTrue(byArm.getOrDefault("direct", 0) > 0, "the DIRECT arm was taken: " + byArm);
        assertTrue(byArm.getOrDefault("deeper", 0) > 0, "the DEEPER arm was taken - the injection fixture is the"
                + " only workspace in this build that reaches it: " + byArm);
        assertTrue(metaUnwraps > 0, "the META UNWRAP was taken at least once - the 'Type coercion' step no"
                + " hold-out golden carries");
    }

    /** The per-workspace half of FACT 5: {elements, with a dependency, dependency edges}. */
    private static void countDeepPathFacts(String what, com.regnosys.rosetta.generator.java.GeneratorModel gm,
            IRTypeIndex index, int[] tally) {
        DeepPathUtilGenerator seam = new DeepPathUtilGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        IRDeepPathUtilEmitter emitter =
                new IRDeepPathUtilEmitter(index, new IRDerivedFacts(index, IRDerivedLie.NONE));
        for (RModel model : gm.workspace().files()) {
            for (var element : model.rootElements()) {
                if (!(element instanceof RDataType dataType)) {
                    continue;
                }
                IRTypeNode node = index.node(model.namespace(), dataType);
                List<String> source = seam.dependenciesOf(dataType);
                assertEquals(source, emitter.dependencyCanonicals(node), what + "/" + dataType.name()
                        + ": the injected sibling utils, the generator's own set IN ITS OWN ORDER against the"
                        + " emitter's");
                tally[0]++;
                if (!source.isEmpty()) {
                    tally[1]++;
                    tally[2] += source.size();
                }
            }
        }
    }

    /**
     * FACT 4 - ONE WALK, TWO READERS. {@link IRDerivedFacts#conditionRefLinks} carries the body
     * {@link IRDerivedFacts#conditionRefs} used to own, and {@code conditionRefs} is now expressed over it. This
     * states both halves of that: the SIMPLE NAMES are identical in order (so the reconciled fact
     * {@code meta.conditionRefs} cannot have moved), and every ref's DECLARING LINK is a link of the element's own
     * chain whose simple name is the prefix the class name is built from - which is what the {@code *Meta} emitter
     * needs and what no simple-name list can carry.
     */
    @Test
    void theConditionRefLinksCarryTheSameNamesAsTheRefsAndNameTheirDeclaringLink() {
        int[] tally = new int[3];   // {refs, declared by an ANCESTOR, from a CHOICE link}
        // THE POPULATION IS THREE SOURCES, and it has to be (LAW 84 / the measured reason, READ from this test's
        // own first run): the four condition-bearing hold-out groups carry exactly ONE condition ref on a TYPE
        // between them and not one of them declares a type that INHERITS a condition - so a run over them alone
        // would have agreed about a single self-declared ref and proved nothing about the DECLARING LINK, which is
        // the whole reason conditionRefLinks exists. The seat fixture adds the CHOICE-link ancestor
        // (ChoiceExtender extends the choice Either, so its meta carries EitherChoice typed to Either) and the
        // derived-reconcile fixture adds the DATA ancestor (Priced extends Base, whose BaseRule it inherits).
        for (String group : List.of("pojo-inheritance", "alias-conditions", "alias-conditions-twins",
                "hero-model")) {
            IRPropertyModelTest.HoldOutWorkspace workspace = IRPropertyModelTest.holdOutWorkspace(group);
            IRDerivedFacts facts = new IRDerivedFacts(workspace.index(), IRDerivedLie.NONE);
            for (RModel model : workspace.models()) {
                countConditionRefLinks(group, model, workspace.index(), facts, tally);
            }
        }
        IRPropertyModelTest.Fixture seat = IRPropertyModelTest.fixture();
        countConditionRefLinks("seat8.props", seat.lib(), seat.index(),
                new IRDerivedFacts(seat.index(), IRDerivedLie.NONE), tally);
        IRDerivedFactsReconcileTest.Fixture derived = IRDerivedFactsReconcileTest.fixture();
        countConditionRefLinks("seat8.derived", derived.model(), derived.index(),
                new IRDerivedFacts(derived.index(), IRDerivedLie.NONE), tally);

        System.out.println("conditionRefLinks: " + tally[0] + " ref(s), " + tally[1]
                + " declared by an ANCESTOR, " + tally[2] + " from a choice link");
        assertTrue(tally[0] > 0, "the fixtures carry condition refs at all");
        assertTrue(tally[1] > 0, "at least one ref is declared by an ANCESTOR of the subject - the link is"
                + " what the emitter types the factory call to, and a chain of one would never show it");
        assertTrue(tally[2] > 0, "at least one ref comes from a CHOICE link - the implicit <Name>Choice condition"
                + " a data-extends-choice boundary contributes, which no data link ever writes");
    }

    /**
     * The per-model half of the statement above: for every data type of {@code model}, the simple names of
     * {@link IRDerivedFacts#conditionRefLinks} ARE {@link IRDerivedFacts#conditionRefs}' own answer in order, and
     * every ref's declaring link is a link of the element's own chain whose simple name is the prefix the class
     * name is built from. {@code tally} accumulates {refs, ancestor-declared, choice-declared}.
     */
    private static void countConditionRefLinks(String what, RModel model, IRTypeIndex index,
            IRDerivedFacts facts, int[] tally) {
        for (var element : model.rootElements()) {
            if (!(element instanceof RDataType dataType)) {
                continue;
            }
            IRTypeNode node = index.node(model.namespace(), dataType);
            List<IRTypeNode> chain = facts.chain(node);
            List<IRDerivedFacts.ConditionRefLink> links = IRDerivedFacts.conditionRefLinks(chain);
            List<String> fromLinks = new ArrayList<>();
            for (IRDerivedFacts.ConditionRefLink link : links) {
                fromLinks.add(link.simpleName());
            }
            assertEquals(IRDerivedFacts.conditionRefs(chain), fromLinks, what + "/" + dataType.name()
                    + ": conditionRefs IS the simple names of conditionRefLinks - one walk, two readers");
            for (IRDerivedFacts.ConditionRefLink link : links) {
                assertTrue(chain.contains(link.link()), what + "/" + dataType.name()
                        + ": a ref's declaring link is a link of the element's OWN chain");
                String declaring = IRTypeUnit.simpleName(link.link());
                assertTrue(link.simpleName().startsWith(declaring), what + "/" + dataType.name()
                        + ": the class name is built from its DECLARING type's simple name, not the subject's - "
                        + link.simpleName() + " against " + declaring);
                tally[0]++;
                if (link.link() != node) {
                    tally[1]++;
                }
                if (link.link().kind() == IRKind.CHOICE) {
                    assertEquals(declaring + "Choice", link.simpleName(), what + "/" + dataType.name()
                            + ": a choice link contributes exactly its implicit Choice condition");
                    tally[2]++;
                }
            }
        }
    }
}
