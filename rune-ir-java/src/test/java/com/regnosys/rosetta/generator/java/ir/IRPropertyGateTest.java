package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.ir.core.IRDocReference;

import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.NAMESPACE;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.choice;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.choiceModel;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.dataType;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.fixture;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.model;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.property;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 8 (PR #644) - THE PROPERTY GATE, PROVED TO BE ABLE TO FAIL. The reconcile is GREEN on every element of the
 * fixture at its honest IR; then, one lane at a time (P01-P09 of the plan's lane table), the reconciler is handed a
 * model with ONE fact altered and the mismatch set is asserted EXACTLY - the family the lane aims at, and every family
 * that rides along BY CONSTRUCTION, named rather than left implied.
 *
 * <p>THE TEST SEAM is {@link IRPropertyModel#stating} plus the record's {@code with*} copies: they state a surface
 * with NO law applied, which is the only way to make the IR half lie about one fact without also mutating the law that
 * produced it (the law-side mutations are the lane script's job, on a re-installed tree). Neither has a production
 * caller.
 *
 * <p>The mismatch set is read as the list of FACT NAMES, in the order the {@code Check} asserted them, because a
 * lane's RED must NAME its family - "something disagreed" is not a witness.
 */
class IRPropertyGateTest {

    // ------------------------------------------------------------------------------------------------ L0: GREEN

    @Test
    void everyElementOfTheFixtureReconcilesGreenAtItsHonestIR() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyReconciler reconciler = new IRPropertyReconciler(f.gm());
        int elements = 0;
        for (var element : f.lib().rootElements()) {
            if (element instanceof RDataType dataType) {
                reconciler.attempt();
                elements++;
                assertEquals(List.of(), reconciler.reconcile(NAMESPACE, dataType,
                                model(f, dataType.name())),
                        "the IR half and the old generator's own pojo must agree on " + dataType.name());
            } else if (element instanceof RChoice choice && !"TemplatedChoice".equals(choice.name())) {
                reconciler.attempt();
                elements++;
                assertEquals(List.of(), reconciler.reconcile(NAMESPACE, choice, choiceModel(f, choice.name())),
                        "the IR half and the old generator's own pojo must agree on " + choice.name());
            }
        }
        int[] stats = reconciler.stats();
        assertEquals(elements, stats[0], "one declaration booked per element attempted");
        assertTrue(stats[1] > stats[0], "a gate that asserts fewer facts than declarations asserts nothing");
        assertEquals(0, stats[2], "and not one of them disagreed");
    }

    @Test
    void aTopLevelTemplateChoiceIsRefusedByBOTHHalvesForTheSameReason() {
        IRPropertyModelTest.Fixture f = fixture();
        RChoice templated = choice(f.lib(), "TemplatedChoice");
        assertThrows(GenerationException.class, () -> choiceModel(f, "TemplatedChoice"),
                "the IR half refuses the surface outright");
        assertThrows(IllegalStateException.class,
                () -> new IRPropertyReconciler(f.gm()).reconcile(NAMESPACE, templated,
                        IRPropertyModel.stating(List.of(), List.of())),
                "and so does the old generator, from inside initializeProperties - the host books the throw through"
                        + " threw(), so a refused element can never leave the population silently");
    }

    // ------------------------------------------------- P01: the parent seed dropped -> the ORDER and SIZE of `all`

    @Test
    void laneP01TheParentSeedDroppedIsRedOnThePropertySurfaceOrder() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "SeedChild");
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(), honest.ownProperties());

        assertEquals(List.of("property.surface.size.all", "property.surface.order.all",
                        "property.a.name", "property.a.runeName", "property.a.getterName",
                        "property.a.setterName"),
                families(reconcile(f, "SeedChild", lying)),
                "a surface that forgot its parent is red on the ALL surface's size and order - and, because the two"
                        + " surfaces are paired POSITIONALLY, on the FOUR name facts of the property that moved into"
                        + " the inherited slot (the rune name joined them at v3.3 seat 9, PR #645 commit 9, and it"
                        + " rides along BY CONSTRUCTION here: it is named rather than left implied); the OWN"
                        + " surface stays green, which is what makes the two facts distinguishable");
    }

    // ----------------------------------------------------------- P02: the meta wrap dropped from the property type

    @Test
    void laneP02TheMetaWrapDroppedIsRedOnThePropertysJavaTypeAlone() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "MetaCarrier");
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(),
                replacing(honest.allProperties(), "scheme", p -> p.withJavaType("java.lang.String")));

        assertEquals(List.of("property.scheme.javaType"), families(reconcile(f, "MetaCarrier", lying)),
                "FieldWithMetaString on one side and the bare String on the other: the fact that names the wrapper"
                        + " is the one that goes red, and nothing else moves");
    }

    // -------------------------------------------------------- P03: Case 0's requiredness clause dropped

    @Test
    void laneP03ACase0AttributeThatProducedAPropertyIsRedOnItsSpecializationVerdict() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "Child");
        List<IRPropertyModel.IRProperty> own = new ArrayList<>();
        own.add(find(honest.allProperties(), "plain"));   // the Case 0 attribute, wrongly claimed as OWN
        own.addAll(honest.ownProperties());
        IRPropertyModel lying = IRPropertyModel.stating(own, honest.allProperties());

        assertEquals(List.of("property.surface.size.own", "property.surface.order.own",
                        "property.plain.specialized"),
                families(reconcile(f, "Child", lying)),
                "the Case 0 verdict is read on the oracle as the ABSENCE of the redeclared name from"
                        + " getOwnProperties() - claim it and the attribute's own fact names it");
    }

    // ------------------------------------------------------ P04: incompatiblePropertyName's arm dropped

    @Test
    void laneP04ASpecializedSetterThatKeptItsPlainNameIsRedOnTheSetterName() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "Child");
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(),
                replacing(honest.allProperties(), "tech", p -> p.withSetterCompatibilityName("tech")));

        assertEquals(List.of("property.tech.setterName"), families(reconcile(f, "Child", lying)),
                "a requiredness-only specialization whose erasure equals the parent's MUST take the OverriddenAs"
                        + " name - two setters of the same erasure cannot be overloaded, and the generated POJO would"
                        + " not compile");
    }

    // ------------------------------------------------------------- P05: the meta union's verdict on the value type

    @Test
    void laneP05AMetaWrappedPropertyWithNoValueTypeIsRedOnTheMetaValueType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "MetaCarrier");
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(),
                replacing(honest.allProperties(), "scheme", p -> p.withMetaValueType(Optional.empty())));

        assertEquals(List.of("property.scheme.metaValueType"), families(reconcile(f, "MetaCarrier", lying)),
                "the BARE value type behind the wrapper is a fact of its own - the wrapper's own name cannot"
                        + " witness it, because two different value types can spell the same wrapper package");
    }

    // ------------------------------------------------------------------- P06: the doc-reference union's parent leg

    @Test
    void laneP06ADocReferenceUnionThatGrewIsRedOnItsCountAndItsRenderedList() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "Base");
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(),
                replacing(honest.allProperties(), "plain", p -> p.withDocReferences(List.of(docReference()))));

        assertEquals(List.of("property.plain.docReferences.size", "property.plain.docReferences"),
                families(reconcile(f, "Base", lying)),
                "the union is asserted BOTH as a count and as the rendered list, so a union that grew and a union"
                        + " whose members moved are two different reds - the count alone would miss the second");
    }

    // ------------------------------------------------------------- P07: the [metadata template] arm of the synthetic

    @Test
    void laneP07ASyntheticMetaOfTheWrongClassIsRedOnThePropertyMetaType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "Templated");
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(),
                replacing(honest.allProperties(), "meta",
                        p -> p.withJavaType("com.rosetta.model.metafields.MetaFields")));

        assertEquals(List.of("property.meta.type", "property.meta.javaType"),
                families(reconcile(f, "Templated", lying)),
                "a type carrying [metadata template] as well as [metadata key] takes MetaAndTemplateFields; the"
                        + " dedicated meta.type fact and the property's own javaType are RED together BY"
                        + " CONSTRUCTION, since one is derived from the other - both are named rather than folded");
    }

    // ------------------------------------------------------------- P08: the EFFECTIVE definition behind a property

    @Test
    void laneP08ADefinitionTheDeclarationDidWriteIsRedOnThePropertysDefinition() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "Base");
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(),
                replacing(honest.allProperties(), "plain", p -> p.withDefinition(Optional.empty())));

        assertEquals(List.of("property.plain.definition"), families(reconcile(f, "Base", lying)),
                "the definition is read on the oracle through the property's DECLARING pojo - the ancestor's under"
                        + " Case 0, the specializing type's under a specialization - so a definition the IR dropped"
                        + " names itself, and the rendered javadoc beside it stays green because the seam restates"
                        + " ONE component at a time");
    }

    // ---------------------------------------------------------- P09: the RENDERED javadoc the POJO getter carries

    @Test
    void laneP09AJavadocTheGetterWouldNeverHaveCarriedIsRedOnThePropertysJavadoc() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "Documented");
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(),
                replacing(honest.allProperties(), "described",
                        p -> p.withJavadoc(Optional.of("/**\n * a block the old generator never rendered\n */"))));

        assertEquals(List.of("property.described.javadoc"), families(reconcile(f, "Documented", lying)),
                "THE BYTE-BEARING FACT: ModelObjectGenerator:641-644 writes prop.getJavadoc() above the getter, so a"
                        + " block that disagrees by ONE character is a byte that disagrees. The source half READS the"
                        + " old generator's own rendered string; the IR half reproduces it from the IR facts alone -"
                        + " the definition, the doc reference and its RESOLVED corpus - so nothing but the law itself"
                        + " can make the two agree");
    }

    // ------------------------------- P09 (seat 9): the ITEM-KIND fact the getter arm and two import gates read

    @Test
    void laneP09OfSeatNineAnEnumItemClaimedAsAModelObjectIsRedOnTheItemKindFact() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "EnumHolder");
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(),
                replacing(honest.allProperties(), "colours", p -> p.withItemIsRosettaModelObject(true)));

        assertEquals(List.of("property.colours.itemIsRosettaModelObject"),
                families(reconcile(f, "EnumHolder", lying)),
                "THE FACT THE RENDERED NAME CANNOT CARRY: seat8.props.Colour and seat8.props.Leaf are spelt alike,"
                        + " and only the DECLARATION behind the item says which is a RosettaModelObject. Claim the"
                        + " enum as one and the golden's List<Colour> becomes List<? extends Colour>, the"
                        + " java.util.function.Consumer import disappears and the process split flips - the source"
                        + " half is the old generator's OWN predicate, read through its seam");
    }

    // ----------------------------------- P10 (seat 9): the ANCESTOR CHAIN every compat arm is written from

    @Test
    void laneP10OfSeatNineAChainThatDroppedItsLastAncestorIsRedOnTheParentChainAlone() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyModel honest = model(f, "ChainLeaf");
        List<IRPropertyModel.IRParentLink> full = find(honest.allProperties(), "hop").parentChain();
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(),
                replacing(honest.allProperties(), "hop",
                        p -> p.withParentChain(full.subList(0, full.size() - 1))));

        assertEquals(List.of("property.hop.parentChain.types"), families(reconcile(f, "ChainLeaf", lying)),
                "the LAST rung of ChainLeaf.hop is ChainTop's list-shaped hop - the rung that puts"
                        + " java.util.Collections, MapperC and the bare java.util.List in the import set and that"
                        + " every list-shaped compat arm is signed with. Drop it and the DEPTH beside it stays"
                        + " right, which is exactly why the two are separate facts: the chain fact alone goes red");
    }

    // ------------------ the SIX RUNG FACTS of seat 9 commit 10: the ancestor's own, carried on the link

    /**
     * THE SIX FACTS PR #645 COMMIT 10 ADDED TO A RUNG, read by name on the chain that needs every one of them.
     * {@code ChainLeaf.hop} is {@code Sub (0..1)} over {@code ChainMid.hop} {@code Leaf (0..1)} over
     * {@code ChainTop.hop} {@code List<Leaf>}, so the two rungs differ in cardinality, in compatibility name and
     * in getter-override verdict - which is exactly what a compat member needs to be written at all.
     *
     * <p>Every one of them is RECONCILED against the old generator's own ancestor property by the L0 test above
     * (the row {@code property.hop.parentChain.types} grew from four fields to ten and the FACT COUNT did not
     * move - it is still one assertion per property). This test states the VALUES, so a law that flipped
     * wholesale could not hide behind an oracle that flipped with it.
     */
    @Test
    void theSixRungFactsAreTheAncestorPropertysOwnOnTheDepthTwoChain() {
        IRPropertyModelTest.Fixture f = fixture();
        List<IRPropertyModel.IRParentLink> chain = property(model(f, "ChainLeaf"), "hop").parentChain();
        assertEquals(2, chain.size(), "the lane's own subject must exist: a depth-2 chain");

        IRPropertyModel.IRParentLink mid = chain.get(0);
        assertEquals("seat8.props.Leaf", mid.javaType());
        assertFalse(mid.multi(), "ChainMid restricted the list to a single");
        assertTrue(mid.itemIsRosettaModelObject(),
                "a `type` item - the fact that decides the rung's List<? extends X> arm, its X.XBuilder single"
                        + " type and the .toBuilder() suffix of the coerced result");
        assertFalse(mid.itemIsEnum(), "and not an enum, which is the half itemIsRosettaModelObject cannot express");
        assertEquals(Optional.empty(), mid.metaValueIsRosettaModelObject(),
                "the rung carries no meta value type at all, which is not the same statement as `false`");
        assertFalse(mid.getterOverridesParentGetter(),
                "THE FACT THAT CLOSES COMMIT 8's OVER-REFUSAL: ChainMid.hop's own getter does NOT keep"
                        + " ChainTop's name, so the walk writes a member for this rung - a verdict no fact of the"
                        + " specialized property carries");
        assertEquals("hopOverriddenAsSingle", mid.getterCompatibilityName(),
                "and the member is called by the RUNG's own getter name");
        assertEquals("hop", mid.setterCompatibilityName(),
                "while its setter OVERLOADS, because a list and a single erase differently");

        IRPropertyModel.IRParentLink top = chain.get(1);
        assertEquals("List<seat8.props.Leaf>", top.javaType());
        assertTrue(top.multi());
        assertFalse(top.getterOverridesParentGetter(),
                "the top of a chain is unspecialized, so it overrides nothing");
        assertEquals("hop", top.getterCompatibilityName());
        assertEquals("hop", top.setterCompatibilityName());

        // and the MAIN property's own flag, which is what decides the FIRST hop (CE:175's cursor)
        assertTrue(property(model(f, "ChainLeaf"), "hop").getterOverridesParentGetter(),
                "ChainLeaf.hop keeps ChainMid's compatibility name, so the first rung appends NOTHING and the"
                        + " second one does - the sequence PojoCompatEmitter's cursor walks");
    }

    /**
     * THE RUNG FACTS ON A META-WRAPPED ANCESTOR (same commit). Nothing in {@code seat8.props} specializes a
     * meta-wrapped property, so the witness is the HOLD-OUT battery the compat arms' own oracle goldens come
     * from: {@code pojo-inheritance}'s {@code Foo3.parentList} sits over {@code Foo2.parentList}, which is
     * {@code Child (1..1) [metadata reference]} - a rung whose bare value type is a MODEL object, the arm the
     * meta unwrap / wrap table is typed from.
     */
    @Test
    void aMetaWrappedRungCarriesItsValuesKindAndReconcilesGreen() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.holdOutFixture("pojo-inheritance");
        IRPropertyModel foo3 = IRPropertyModel.of(
                f.index().node("test.pojo", dataType(f.lib(), "Foo3")), f.index());
        IRPropertyModel.IRParentLink rung = property(foo3, "parentList").parentChain().get(0);
        assertEquals(Optional.of("test.pojo.Child"), rung.metaValueType(),
                "Foo2.parentList is [metadata reference] over Child");
        assertEquals(Optional.of(Boolean.TRUE), rung.metaValueIsRosettaModelObject(),
                "and the VALUE behind that wrap is a model object - the fact the meta unwrap arm is typed from");
        assertTrue(rung.itemIsRosettaModelObject(),
                "the ITEM is the generated ReferenceWithMetaChild class, which is one too");
        assertFalse(rung.itemIsEnum());
    }

    /**
     * THE GATE RUNS ON THE HOLD-OUT MODELS TOO (v3.3 seat 9, PR #645 commit 10). The five {@code pojo-*}
     * batteries are the only fixtures in the tree that carry a meta-wrapped specialization, a number ladder and a
     * deep inheritance chain at once, which is exactly where the six new rung facts can be wrong; holding the
     * IR half against {@code RJavaPojoInterface} on every one of their types is what makes the emitter's
     * byte-compare on them a comparison of two producers rather than of one.
     */
    @Test
    void everyTypeOfEveryHoldOutPojoModelReconcilesGreenAtItsHonestIR() {
        int elements = 0;
        for (String group : IRPropertyModelTest.HOLDOUT_POJO_GROUPS) {
            IRPropertyModelTest.Fixture f = IRPropertyModelTest.holdOutFixture(group);
            IRPropertyReconciler reconciler = new IRPropertyReconciler(f.gm());
            String namespace = f.lib().namespace();
            for (String name : IRPropertyModelTest.holdOutTypes(f)) {
                RDataType type = dataType(f.lib(), name);
                reconciler.attempt();
                elements++;
                assertEquals(List.of(), reconciler.reconcile(namespace, type,
                                IRPropertyModel.of(f.index().node(namespace, type), f.index())),
                        "the IR half and the old generator's own pojo must agree on " + group + "/" + name);
            }
            assertEquals(0, reconciler.stats()[2], group + ": not one fact disagreed");
        }
        assertEquals(15, elements,
                "the population this gate actually reconciled, counted rather than trusted: 8 types in"
                        + " pojo-inheritance, 1 in pojo-number-ladder and 2 in each of the three bulk batteries");
    }

    // -------------------------------- the fact of seat 9 commit 9: the RUNE NAME (the pruning key's half)

    /**
     * THE RUNE NAME, per carrier (v3.3 seat 9, PR #645 commit 9). It is the ATTRIBUTE half of the pruning-config
     * key {@code GeneratorModel.isPruningDisabled} reads ({@code <typeFqn>#<attrRuneName>}, {@code :116-119}),
     * which section 12's {@code prune()} and {@code hasData()} arms branch on.
     *
     * <p>On this fork it is ALWAYS equal to the property's own name - {@code RJavaPojoInterface.addProperty}
     * ({@code :440-443}) fills all five name slots from the declaration's name and
     * {@code JavaPojoProperty.specialize} ({@code :105-113}) passes the parent's through - so this test states
     * that equality across the four SHAPES a property can take rather than asserting it once: a fresh attribute,
     * a SPECIALIZED one (whose getter and setter compatibility names have MOVED off the name, which is exactly
     * where a careless derivation would follow them), a CHOICE OPTION (whose name is the option's PascalCase
     * TYPE name) and the SYNTHETIC {@code meta}. The equality is stated here and MEASURED per property by the
     * reconcile's own {@code property.<name>.runeName} row against {@code getRuneName()} - the emitter is built
     * on the measured fact, not on this expectation.
     */
    @Test
    void theRuneNameIsTheDeclarationsOwnNameInEveryShapeAPropertyCanTake() {
        IRPropertyModelTest.Fixture f = fixture();
        assertEquals("id", property(model(f, "Leaf"), "id").runeName(),
                "a plain attribute - the FIRST property of the fixture, which is what lane P13 reads RED");
        assertEquals("leaves", property(model(f, "Base"), "leaves").runeName(),
                "a list-shaped attribute, whose pruning arm is the stream form");

        IRPropertyModel.IRProperty specialized = property(model(f, "ListToSingle"), "leaves");
        assertEquals("leaves", specialized.runeName(),
                "a SPECIALIZED property keeps the parent's rune name - which is also its own declaration's name;"
                        + " the two would only part if a specialization ever renamed an attribute");
        assertEquals("leavesOverriddenAsSingle", specialized.getterCompatibilityName(),
                "and the GETTER compatibility name HAS moved off it, which is the point of asserting both: a"
                        + " rune name derived from the accessor names would already be wrong here");

        assertEquals("Leaf", property(choiceModel(f, "Either"), "Leaf").runeName(),
                "a CHOICE option's rune name is its PascalCase option name, not the field identifier");
        assertEquals("meta", property(model(f, "Keyed"), "meta").runeName(),
                "and the SYNTHETIC meta property carries one too - it is added through the same addProperty law");
    }

    // ------------------- the two facts of seat 9 commit 8: the hashCode enum arm and the meta-value kind

    /**
     * THE ITEM-IS-ENUM FACT, per carrier. It is a fact about the DECLARATION behind the item, not about the
     * rendered name: {@code seat8.props.Colour} and {@code seat8.props.Leaf} are spelt exactly alike and only the
     * kind tells them apart. The reconcile itself (the L0 test above) holds every one of these against
     * {@code ModelObjectGenerator.itemIsEnum} - the expression {@code ModelObjectBoilerplate:205-206} now
     * delegates to; this test states the VALUES the fixture is expected to carry, so a law that flipped wholesale
     * could not hide behind an oracle that flipped with it.
     */
    @Test
    void theItemIsEnumFactIsTrueForAnEnumItemInEitherCardinalityAndFalseForEveryOtherKind() {
        IRPropertyModelTest.Fixture f = fixture();
        assertTrue(property(model(f, "EnumHolder"), "tone").itemIsEnum(),
                "a SINGLE enum attribute - the hashCode arm that hashes the class name");
        assertTrue(property(model(f, "EnumHolder"), "colours").itemIsEnum(),
                "and a LIST of the same enum - the ITEM is what is asked about, never the List wrapper");
        assertFalse(property(model(f, "Base"), "leaf").itemIsEnum(),
                "a STRUCT item is no enum");
        assertFalse(property(model(f, "Leaf"), "id").itemIsEnum(),
                "and neither is a builtin");
        assertFalse(property(model(f, "MetaCarrier"), "refd").itemIsEnum(),
                "a META-WRAPPED item is the GENERATED ReferenceWithMeta* CLASS, which is never an enum whatever"
                        + " the value type is");
    }

    /**
     * THE META-VALUE-KIND FACT, per carrier - an {@code Optional} on both halves, because EMPTY ("this property
     * has no meta value type") is a different statement from {@code false} ("it has one and it is not a model
     * object"). The old generator's own fact is a nullable {@code getMetaValueType()}, and the three sites that
     * write bytes from it ({@code ModelObjectGenerator:1325-1327}, {@code :1341-1342}, {@code :1585}) ask exactly
     * that two-step question.
     */
    @Test
    void theMetaValueKindFactIsEmptyWithoutAWrapAndNamesTheValuesKindWithOne() {
        IRPropertyModelTest.Fixture f = fixture();
        assertEquals(Optional.of(Boolean.TRUE),
                property(model(f, "MetaCarrier"), "refd").metaValueIsRosettaModelObject(),
                "[metadata reference] over Leaf - the VALUE behind the wrap is a model object, so hasData asks"
                        + " it for .hasData() rather than for presence alone");
        assertEquals(Optional.of(Boolean.FALSE),
                property(model(f, "MetaCarrier"), "scheme").metaValueIsRosettaModelObject(),
                "[metadata scheme] over string - a FieldWithMetaString wrapping a basic, which is the"
                        + " presence-only hasData arm");
        assertEquals(Optional.empty(), property(model(f, "Base"), "plain").metaValueIsRosettaModelObject(),
                "an unwrapped property states NOTHING - it does not state false");
        assertEquals(Optional.empty(), property(model(f, "Keyed"), "meta").metaValueIsRosettaModelObject(),
                "and neither does the synthetic meta property");
    }

    // ------------------------------------------------------------------------------------- the counters can fail

    @Test
    void aMismatchIsBookedOnTheReconcilersOwnCountersAndAThrowCountsAsOne() {
        IRPropertyModelTest.Fixture f = fixture();
        IRPropertyReconciler reconciler = new IRPropertyReconciler(f.gm());
        IRPropertyModel honest = model(f, "Child");
        IRPropertyModel lying = IRPropertyModel.stating(honest.ownProperties(),
                replacing(honest.allProperties(), "tech", p -> p.withRequired(false)));

        reconciler.attempt();
        List<String> mismatches = reconciler.reconcile(NAMESPACE, dataType(f.lib(), "Child"), lying);
        assertEquals(List.of("property.tech.required"), families(mismatches));
        assertEquals(1, reconciler.stats()[0]);
        assertEquals(1, reconciler.stats()[2], "the mismatch is booked, not only returned");

        reconciler.attempt();
        reconciler.threw();
        assertEquals(2, reconciler.stats()[0]);
        assertEquals(2, reconciler.stats()[2], "a throw IS a mismatch - never a silent drop from the population");
    }

    // ------------------------------------------------------------------------------------------------- the harness

    private static List<String> reconcile(IRPropertyModelTest.Fixture f, String typeName, IRPropertyModel ir) {
        return new IRPropertyReconciler(f.gm()).reconcile(NAMESPACE, dataType(f.lib(), typeName), ir);
    }

    /** Every mismatch's FACT NAME, in the order the check asserted them - what a lane greps for. */
    private static List<String> families(List<String> mismatches) {
        List<String> names = new ArrayList<>();
        for (String mismatch : mismatches) {
            int start = mismatch.indexOf(": ");
            int end = mismatch.indexOf(" - the source says");
            assertTrue(start > 0 && end > start, "a mismatch must name its fact: " + mismatch);
            names.add(mismatch.substring(start + 2, end));
        }
        return names;
    }

    private static List<IRPropertyModel.IRProperty> replacing(List<IRPropertyModel.IRProperty> properties,
            String name, UnaryOperator<IRPropertyModel.IRProperty> change) {
        List<IRPropertyModel.IRProperty> restated = new ArrayList<>();
        boolean found = false;
        for (IRPropertyModel.IRProperty property : properties) {
            if (property.name().equals(name)) {
                restated.add(change.apply(property));
                found = true;
            } else {
                restated.add(property);
            }
        }
        assertTrue(found, "the lane's own subject must exist: no property named " + name);
        return restated;
    }

    private static IRPropertyModel.IRProperty find(List<IRPropertyModel.IRProperty> properties, String name) {
        for (IRPropertyModel.IRProperty property : properties) {
            if (property.name().equals(name)) {
                return property;
            }
        }
        throw new AssertionError("no property named " + name);
    }

    /** A doc reference the source does NOT carry - the lane's lie, rendered by the same row law both halves use. */
    private static IRDocReference docReference() {
        return new IRDocReference(true, Optional.empty(), Optional.of("SeatEight"), List.of(), List.of(), List.of(),
                Optional.empty(), Optional.of("a provision the source never wrote"), false, List.of());
    }
}
