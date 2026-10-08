package com.regnosys.rosetta.generator.java.ir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 8 (PR #644) - THE POJO PROPERTY LAW, DERIVED FROM THE IR ALONE. Every decision of
 * {@code RJavaPojoInterface.initializeProperties} reproduced over {@link IRPropertyModel} and witnessed on a
 * corpus-free fixture: Case 0, the two specialization arms and their compatibility names, the re-put ORDER (a
 * specialized key keeps the ANCESTOR's position while {@code ownProperties} stays in declaration order), the meta wrap
 * and the namespace its wrapper takes, the override UNION that makes an annotations-only override a Case 0, the
 * DOC-REFERENCE union over an override (PR #644 commit 5: {@code Base.tech} carries one {@code [docReference]}
 * and {@code Child.tech} inherits it through the parent-first union - lane P06 drops that parent leg), the
 * synthetic {@code meta} and its {@code [metadata template]} verdict, a {@code type} inheriting a {@code choice}'s
 * options as {@code 0..1} properties, and the two NAMED refusals.
 *
 * <p>Nothing here reads the old generator - that is {@code IRPropertyGateTest}'s job. This class states what the IR
 * half SAYS, so that a lane which changes the law is red HERE before it is red against the corpus.
 */
class IRPropertyModelTest {

    // ----------------------------------------------------------------------------------------------- the two maps

    @Test
    void aTypeWithNoSupertypeHasTheSameOwnAndAllSurfaceInDeclarationOrder() {
        Fixture f = fixture();
        IRPropertyModel base = model(f, "Base");
        assertEquals("plain,leaf,leaves,tech", names(base.ownProperties()));
        assertEquals("plain,leaf,leaves,tech", names(base.allProperties()));
        assertEquals("java.lang.String", property(base, "plain").javaType());
        assertEquals("seat8.props.Leaf", property(base, "leaf").javaType());
        assertFalse(property(base, "plain").required(), "a (0..1) attribute is not required");
        assertEquals(0, property(base, "plain").parentChainDepth());
    }

    @Test
    void aMultiAttributeIsInvariantListWrappedAndNeverUpperBounded() {
        Fixture f = fixture();
        IRPropertyModel base = model(f, "Base");
        assertEquals("List<seat8.props.Leaf>", property(base, "leaves").javaType(),
                "wrapExtendsIfNotFinal takes its `? extends` arm only for a JavaPojoInterface item, and the"
                        + " translator renders every model type as a plain RGeneratedJavaClass");
        assertTrue(property(base, "leaves").multi());
        assertFalse(property(base, "leaves").required(), "(0..*) is multi, not required");
    }

    // ---------------------------------------------------------------------------------------------------- Case 0

    @Test
    void case0ProducesNoPropertyInEitherMapAndDropsTheChildsOwnFacts() {
        Fixture f = fixture();
        IRPropertyModel child = model(f, "Child");
        assertFalse(names(child.ownProperties()).contains("plain"),
                "an annotations-only override of the same type AND the same requiredness contributes nothing");
        assertEquals(0, property(child, "plain").parentChainDepth(),
                "the PARENT's property continues to represent it - it was never specialized");
        assertEquals("plain", property(child, "plain").getterCompatibilityName());
    }

    @Test
    void theOverrideMetaUnionMakesAnAnnotationsOnlyOverrideACase0() {
        Fixture f = fixture();
        IRPropertyModel metaChild = model(f, "MetaChild");
        assertEquals("", names(metaChild.ownProperties()),
                "the override INHERITS the parent's [metadata scheme], so its Java type equals the parent's and the"
                        + " attribute contributes no property - drop the union's parent leg and a specialized"
                        + " property appears where the golden has none");
        assertEquals("com.rosetta.model.metafields.FieldWithMetaString", property(metaChild, "m").javaType());
    }

    // ------------------------------------------------------------------------------ the specializations and names

    @Test
    void aRequirednessOnlySpecializationKeepsTheGetterAndRenamesTheSetter() {
        Fixture f = fixture();
        IRPropertyModel.IRProperty tech = property(model(f, "Child"), "tech");
        assertTrue(tech.required());
        assertEquals("tech", tech.getterCompatibilityName(), "the types are EQUAL, so the getter is a subtype getter");
        assertEquals("techOverriddenAsString", tech.setterCompatibilityName(),
                "the erasures are equal and Java cannot overload - the setter takes the OverriddenAs name");
        assertTrue(tech.getterOverridesParentGetter());
        assertTrue(tech.compatibleTypeWithParent());
        assertEquals(1, tech.docReferences().size(),
                "the override INHERITS its parent's [docReference] through the parent-first union: the leg lane"
                        + " P06 drops - and the ONE doc reference of this fixture, so a trim here is red before"
                        + " the lane goes quiet");
        assertTrue(tech.sameTypeAsParent(), "a requiredness-only change keeps the type");
        assertEquals(1, tech.parentChainDepth());
    }

    @Test
    void aTypeChangedSpecializationToAModelSubtypeKeepsTheGetterAndOverloadsTheSetter() {
        Fixture f = fixture();
        IRPropertyModel.IRProperty leaf = property(model(f, "Child"), "leaf");
        assertEquals("seat8.props.Sub", leaf.javaType());
        assertEquals("leaf", leaf.getterCompatibilityName(),
                "Sub STRICTLY extends Leaf - the walk is the index's, not the flat Java class's");
        assertEquals("leaf", leaf.setterCompatibilityName(),
                "the erasures DIFFER, so the setter overloads the parent's name rather than being renamed");
        assertTrue(leaf.compatibleTypeWithParent());
        assertFalse(leaf.sameTypeAsParent());
    }

    @Test
    void aListToSingleSpecializationTakesTheOverriddenAsSingleName() {
        Fixture f = fixture();
        IRPropertyModel.IRProperty same = property(model(f, "ListToSingle"), "leaves");
        assertEquals("seat8.props.Leaf", same.javaType());
        assertEquals("leavesOverriddenAsSingle", same.getterCompatibilityName(),
                "the parent's ITEM type equals the specialized type - the name carries no suffix");
        assertEquals("leaves", same.setterCompatibilityName(), "a list and a single erase differently: the setter overloads");
        assertFalse(same.compatibleTypeWithParent(), "list-ness differs, so it is no POJO subtype");

        IRPropertyModel.IRProperty other = property(model(f, "ListToSingleOther"), "leaves");
        assertEquals("leavesOverriddenAsSingleSub", other.getterCompatibilityName(),
                "a DIFFERING item appends the specialized item's SIMPLE name, never the attribute's");
    }

    @Test
    void anIncompatibleTypeSpecializationTakesTheOverriddenAsName() {
        Fixture f = fixture();
        IRPropertyModel.IRProperty plain = property(model(f, "Incompatible"), "plain");
        assertEquals("java.lang.Boolean", plain.javaType());
        assertEquals("plainOverriddenAsBoolean", plain.getterCompatibilityName());
        assertEquals("plain", plain.setterCompatibilityName());
        assertFalse(plain.getterOverridesParentGetter());
        assertFalse(plain.compatibleTypeWithParent());
        assertFalse(plain.sameTypeAsParent());
    }

    // ------------------------------------------------------------------------------------------------- the ORDER

    @Test
    void aSpecializedKeyKeepsTheAncestorsPositionWhileOwnStaysInDeclarationOrder() {
        Fixture f = fixture();
        IRPropertyModel child = model(f, "Child");
        assertEquals("plain,leaf,leaves,tech,own", names(child.allProperties()),
                "the re-put keeps the ANCESTOR's iteration position: `tech` stays fourth although it is declared"
                        + " second, and only the genuinely new key appends");
        assertEquals("tech,leaf,own", names(child.ownProperties()),
                "ownProperties is plain declaration order, minus the Case 0 attribute");
    }

    // -------------------------------------------------------------------------------------------- the meta facts

    @Test
    void theMetaWrapperTakesTheWrappedTypesOwnNamespaceAndTheKindsPrecedence() {
        Fixture f = fixture();
        IRPropertyModel carrier = model(f, "MetaCarrier");
        assertEquals("com.rosetta.model.metafields.FieldWithMetaString", property(carrier, "scheme").javaType(),
                "a BUILTIN value type wraps in com.rosetta.model.metafields");
        assertEquals(Optional.of("java.lang.String"), property(carrier, "scheme").metaValueType());
        assertEquals("seat8.props.metafields.ReferenceWithMetaLeaf", property(carrier, "refd").javaType(),
                "a GENERATED value type wraps in ITS OWN namespace, not the containing type's");
        assertEquals("List<seat8.props.metafields.FieldWithMetaLeaf>", property(carrier, "many").javaType(),
                "the meta wrap happens INSIDE the list wrap");
        assertEquals(Optional.of("seat8.props.Leaf"), property(carrier, "many").metaValueType(),
                "the bare value type is the ITEM's, never the list's");
        assertEquals("seat8.props.metafields.ReferenceWithMetaLeaf", property(carrier, "bothmeta").javaType(),
                "[metadata reference] WINS over [metadata scheme] when both are written");
    }

    @Test
    void theAttributeMetaFlagsAreOrderedAddressThenLocation() {
        Fixture f = fixture();
        IRPropertyModel flags = model(f, "Flags");
        assertEquals(List.of("SCOPED_REFERENCE", "SCOPED_KEY"), property(flags, "both").attributeMetaTypes());
        assertTrue(property(flags, "both").hasLocation());
        assertEquals(List.of("GLOBAL_KEY_FIELD"), property(flags, "ided").attributeMeta());
        assertEquals(List.of(), property(flags, "ided").attributeMetaTypes());
    }

    @Test
    void theSyntheticMetaIsAddedLastAndTakesMetaAndTemplateFieldsOnlyWithATemplate() {
        Fixture f = fixture();
        IRPropertyModel keyed = model(f, "Keyed");
        assertEquals("v,meta", names(keyed.allProperties()), "the synthetic meta is added LAST");
        assertEquals("com.rosetta.model.metafields.MetaFields", property(keyed, "meta").javaType());
        assertTrue(property(keyed, "meta").synthetic());
        assertFalse(property(keyed, "meta").required());

        assertEquals("com.rosetta.model.metafields.MetaAndTemplateFields",
                property(model(f, "Templated"), "meta").javaType());
        assertFalse(names(model(f, "Base").allProperties()).contains("meta"),
                "a type with no [metadata key] carries no synthetic meta at all");
    }

    // ------------------------------------------------------------------------------------------ the CHOICE arms

    @Test
    void aChoicesOptionsAreItsPropertiesNamedByTheOptionsTypeAndNeverListWrapped() {
        Fixture f = fixture();
        IRPropertyModel either = choiceModel(f, "Either");
        assertEquals("Leaf,Sub", names(either.allProperties()));
        assertEquals("seat8.props.Leaf", property(either, "Leaf").javaType());
        for (IRPropertyModel.IRProperty option : either.allProperties()) {
            assertFalse(option.required(), "every option is 0..1 by the literal isRequired = false");
            assertFalse(option.multi(), "an option is never list-wrapped");
            assertTrue(option.inheritedChoiceOption());
        }
    }

    @Test
    void aTypeExtendingAChoiceInheritsItsOptionsAsZeroToOneProperties() {
        Fixture f = fixture();
        IRPropertyModel extender = model(f, "ChoiceExtender");
        assertEquals("Leaf,Sub,weight", names(extender.allProperties()),
                "the choice IS the super pojo, so its options seed the surface exactly as a type's would");
        assertEquals("weight", names(extender.ownProperties()));
        assertTrue(property(extender, "Leaf").inheritedChoiceOption());
        assertFalse(property(extender, "weight").inheritedChoiceOption());
        assertEquals("java.math.BigDecimal", property(extender, "weight").javaType());
    }

    // ------------------------------------------------------------------------------------- the two NAMED refusals

    @Test
    void aChoiceWithNoOptionIsANamedRefusal() {
        Fixture f = fixture();
        IRTypeNode empty = new IRTypeNode("seat8.props.Empty", IRKind.CHOICE, List.of(), Optional.empty(), false,
                Optional.empty(), IRMetadata.EMPTY, Optional.of(NAMESPACE), Optional.empty(), Optional.empty(),
                List.of(), List.of(), List.of());
        String message = assertThrows(GenerationException.class,
                () -> IRPropertyModel.of(empty, f.index())).getMessage();
        assertTrue(message.contains("declares no option") && message.contains("at least 1 option"),
                "the old generator's ctor precondition, reproduced by name: " + message);
    }

    @Test
    void aTopLevelChoiceWithMetadataTemplateIsANamedRefusal() {
        Fixture f = fixture();
        IRTypeNode node = f.index().node(NAMESPACE, choice(f.lib(), "TemplatedChoice"));
        String message = assertThrows(GenerationException.class,
                () -> IRPropertyModel.of(node, f.index())).getMessage();
        assertTrue(message.contains("[metadata template]") && message.contains("refuses"),
                "the choice path's template scaffolding was never mirrored - it refuses rather than degrades: "
                        + message);
    }

    @Test
    void aNodeOfNeitherKindHasNoPropertySurface() {
        Fixture f = fixture();
        IRTypeNode alias = IRTypeNode.reference("Small", IRKind.TYPE_ALIAS, Optional.of(NAMESPACE),
                Optional.of("Small"));
        assertTrue(assertThrows(GenerationException.class, () -> IRPropertyModel.of(alias, f.index()))
                .getMessage().contains("has no POJO property surface"));
    }

    // ------------------------------------------------------------- the DEFINITION and the RENDERED javadoc (MF-1)

    @Test
    void aDeclaredAttributesDefinitionAndRenderedJavadocStandOnItsProperty() {
        Fixture f = fixture();
        IRPropertyModel.IRProperty plain = property(model(f, "Base"), "plain");
        assertEquals(Optional.of("the base plain."), plain.definition());
        assertEquals(Optional.of("/**\n * the base plain.\n */"), plain.javadoc(),
                "a definition with no doc reference renders the block, the one body line and nothing else - no"
                        + " @version line, because both property call sites pass a null version");
        assertEquals(Optional.empty(), property(model(f, "Base"), "leaf").definition(),
                "an attribute that wrote no definition has none");
        assertEquals(Optional.empty(), property(model(f, "Base"), "leaf").javadoc(),
                "and with no doc reference either it carries NO javadoc block at all - the old generator's null");
    }

    @Test
    void case0DropsTheChildsDefinitionAndJavadocAndKeepsTheParentsOwn() {
        Fixture f = fixture();
        IRPropertyModel.IRProperty plain = property(model(f, "Child"), "plain");
        assertEquals(Optional.of("the base plain."), plain.definition(),
                "the child redeclared `plain` WITH a definition of its own, and Case 0 produced no property at all -"
                        + " so the PARENT's property, and the parent's definition, continue to represent it");
        assertEquals(Optional.of("/**\n * the base plain.\n */"), plain.javadoc(),
                "the child's rendered block is dropped with it: the getter keeps the parent's javadoc");
    }

    @Test
    void aSpecializationReplacesTheParentsDefinitionAndJavadoc() {
        Fixture f = fixture();
        IRPropertyModel.IRProperty tech = property(model(f, "Child"), "tech");
        assertEquals(1, tech.parentChainDepth(), "a requiredness-only specialization - the property IS rebuilt");
        assertEquals(Optional.of("the child tech - which replaces."), tech.definition(),
                "JavaPojoProperty.specialize takes the NEW javadoc, so the child's definition wins outright");
        assertEquals(Optional.of("/**\n * the child tech - which replaces.\n *\n * Body Org1\n"
                + " * Corpus Agreement Agr1 Agreement One \"The agreement&#39;s text.\" \n * name \"tech\"\n"
                + " *\n * Provision \n *\n */"), tech.javadoc(),
                "the child's definition wins, and the parent's [docReference] (Base.tech carries the fixture's ONE,"
                        + " commit 5) is INHERITED through the parent-first union and rendered under it - the"
                        + " block IRPropertyGateTest reconciles against the old generator's own getJavadoc()");
        assertEquals(Optional.of("the base tech."), property(model(f, "Base"), "tech").definition(),
                "and the parent's own property is untouched by the child that specialized it");
    }

    @Test
    void aChoiceOptionCarriesItsOwnDefinitionAndInheritsItWithTheOption() {
        Fixture f = fixture();
        IRPropertyModel.IRProperty option = property(choiceModel(f, "Either"), "Leaf");
        assertEquals(Optional.of("the leaf option."), option.definition());
        assertEquals(Optional.of("/**\n * the leaf option.\n */"), option.javadoc(),
                "an option's javadoc is its definition ALONE - RJavaPojoInterface:379-380 passes an empty doc-"
                        + "reference list, because an option carries no regulatory reference");
        assertEquals(Optional.empty(), property(choiceModel(f, "Either"), "Sub").definition());

        IRPropertyModel.IRProperty inherited = property(model(f, "ChoiceExtender"), "Leaf");
        assertEquals(Optional.of("the leaf option."), inherited.definition(),
                "a type extending the choice inherits the option's property WHOLE - its definition with it");
        assertEquals(option.javadoc(), inherited.javadoc());
    }

    @Test
    void theSyntheticMetaCarriesNeitherADefinitionNorAJavadocBlock() {
        Fixture f = fixture();
        IRPropertyModel.IRProperty meta = property(model(f, "Keyed"), "meta");
        assertTrue(meta.synthetic());
        assertEquals(Optional.empty(), meta.definition(), "there is no declaration behind it to carry one");
        assertEquals(Optional.empty(), meta.javadoc(),
                "the old generator adds it with a null javadoc (RJavaPojoInterface:258, :321)");
    }

    @Test
    void aDocumentedAttributesJavadocRendersEveryLineOfItsDocReference() {
        Fixture f = fixture();
        IRPropertyModel documented = model(f, "Documented");

        assertEquals(String.join("\n",
                        "/**",
                        " * a &amp; b&#39;s end",
                        " *",
                        " * Body Org1",
                        " * Corpus Agreement Agr1 Agreement One \"The agreement&#39;s text.\" ",
                        " * name \"described\"",
                        " *",
                        " * Provision The provision.",
                        " *",
                        " */"),
                property(documented, "described").javadoc().orElseThrow(),
                "the definition is HTML-escaped and the corpus renders the RESOLVED declaration's own keyword, name,"
                        + " display name and definition - each followed by the space the old generator writes, the"
                        + " trailing one included, because every one of them is byte-bearing");

        assertEquals(String.join("\n",
                        "/**",
                        " *",
                        " * pathed",
                        " * Body Org1",
                        " * Corpus Agreement Agr1 Agreement One \"The agreement&#39;s text.\" ",
                        " * name \"pathed\"",
                        " *",
                        " * Provision ",
                        " *",
                        " */"),
                property(documented, "pathed").javadoc().orElseThrow(),
                "an attribute with a doc reference but NO definition still takes the block, with no body line; a"
                        + " PATHED reference emits its path line FIRST; and a reference with no provision emits the"
                        + " bare `Provision ` line, trailing space and all");

        assertEquals(Optional.empty(), property(documented, "bare").javadoc(),
                "and its neighbour, which wrote neither, takes no block at all");
    }

    @Test
    void anExplicitEmptyDefinitionStillWarrantsABlockWhileAnAbsentOneDoesNot() {
        assertEquals(Optional.empty(), IRPropertyModel.javadocOf(Optional.empty(), List.of()),
                "absent, with no doc reference: the old generator returns null and the getter carries no block");
        assertEquals(Optional.of("/**\n */"), IRPropertyModel.javadocOf(Optional.of(""), List.of()),
                "the explicit <\"\"> is a deliberate source-level distinction - it warrants the empty block, and the"
                        + " renderer must not collapse it onto absence");
        assertEquals(Optional.of("/**\n * &lt;b&gt;bold&lt;/b&gt; &amp; &quot;quoted&quot; &#39;\n */"),
                IRPropertyModel.javadocOf(Optional.of("<b>bold</b> & \"quoted\" '"), List.of()),
                "all five entities of the old generator's escaper, in its own order");
        assertEquals(Optional.of("/**\n * first\n second\n */"),
                IRPropertyModel.javadocOf(Optional.of("first\nsecond"), List.of()),
                "the Xtend template's continuation prefix: one space after every embedded newline");
    }

    // ------------------------------------------- the ITEM-KIND fact and the ANCESTOR CHAIN (PR #645 commit 5)

    @Test
    void theItemKindFactFollowsTheDeclarationBehindTheItemAndNeverItsRenderedSpelling() {
        Fixture f = fixture();
        IRPropertyModel base = model(f, "Base");
        assertTrue(property(base, "leaf").itemIsRosettaModelObject(), "a `type` item IS a RosettaModelObject");
        assertTrue(property(base, "leaves").itemIsRosettaModelObject(),
                "and so is the ITEM of a list of them - the list wrapper is not what the fact is about");
        assertFalse(property(base, "plain").itemIsRosettaModelObject(), "java.lang.String is not one");

        IRPropertyModel holder = model(f, "EnumHolder");
        assertFalse(property(holder, "colours").itemIsRosettaModelObject(),
                "an ENUM item is a plain Java enum - THE case the rendered name cannot decide, since"
                        + " seat8.props.Colour and seat8.props.Leaf are spelt alike");
        assertFalse(property(holder, "tone").itemIsRosettaModelObject());

        IRPropertyModel carrier = model(f, "MetaCarrier");
        assertTrue(property(carrier, "many").itemIsRosettaModelObject(),
                "a meta-wrapped item is the GENERATED FieldWithMeta* class, which is a RosettaModelObject");
        assertTrue(property(carrier, "scheme").itemIsRosettaModelObject(),
                "even when the VALUE behind the wrap is a builtin - the wrapper is the item");

        assertTrue(property(model(f, "Keyed"), "meta").itemIsRosettaModelObject(),
                "MetaFields is a generated model object, so the synthetic meta's fact is true");
        assertTrue(property(choiceModel(f, "Either"), "Leaf").itemIsRosettaModelObject(),
                "a choice option's item is the option's own declaration");
        assertFalse(property(model(f, "ChoiceExtender"), "weight").itemIsRosettaModelObject(),
                "a number renders as java.math.BigDecimal - a value class, not a model object");
    }

    @Test
    void aSpecializedPropertyCarriesItsAncestorsNearestFirstAndAnUnspecializedOneCarriesNone() {
        Fixture f = fixture();
        assertEquals(List.of(), property(model(f, "Base"), "leaf").parentChain(),
                "depth 0 carries no rung at all");

        IRPropertyModel.IRProperty mid = property(model(f, "ChainMid"), "hop");
        assertEquals(1, mid.parentChainDepth());
        assertEquals(List.of(chainTopHop()),
                mid.parentChain(), "one rung: the list-shaped ancestor the single-shaped override restricts");

        IRPropertyModel.IRProperty chainLeaf = property(model(f, "ChainLeaf"), "hop");
        assertEquals(2, chainLeaf.parentChainDepth());
        assertEquals(List.of(chainMidHop(), chainTopHop()),
                chainLeaf.parentChain(),
                "NEAREST FIRST, which is the order `for (anc = p.getParentProperty(); anc != null; anc ="
                        + " anc.getParentProperty())` visits - the order every compat arm and the chain's import arm"
                        + " are written in");
        assertEquals(chainLeaf.parentChainDepth(), chainLeaf.parentChain().size(),
                "the chain IS the hops: the size and the depth are two readings of one law");
        assertEquals("seat8.props.Leaf|false|false||true|false||false|hopOverriddenAsSingle|hop",
                chainLeaf.parentChain().get(0).row(),
                "the row both halves of the reconcile produce independently - TEN fields since PR #645"
                        + " commit 10, and still ONE fact per property");
    }

    /**
     * {@code ChainMid.hop} AS A RUNG (v3.3 seat 9, PR #645 commit 10), stated once so the two tests that read it
     * cannot drift apart: a single-shaped {@code Leaf} restricting {@code ChainTop}'s list, whose own getter
     * therefore does NOT keep its parent's name ({@code hopOverriddenAsSingle}) while its setter OVERLOADS it
     * ({@code hop}, because a list and a single erase differently).
     */
    static IRPropertyModel.IRParentLink chainMidHop() {
        return new IRPropertyModel.IRParentLink("seat8.props.Leaf", false, false, Optional.empty(),
                true, false, Optional.empty(), false, "hopOverriddenAsSingle", "hop");
    }

    /** {@code ChainTop.hop} AS A RUNG: the unspecialized list at the top of the chain - all five names its own. */
    static IRPropertyModel.IRParentLink chainTopHop() {
        return new IRPropertyModel.IRParentLink("List<seat8.props.Leaf>", true, false, Optional.empty(),
                true, false, Optional.empty(), false, "hop", "hop");
    }

    // ------------------------------------------------------- the refusal an ABSENT parent is (SF-1, PLAN § C.1)

    @Test
    void aTypeWhoseExtendsIsUnresolvedIsRefusedByNameWhereTheOldGeneratorWouldEmitAFlatPojo() {
        RModel lib = AstBuilder.buildFromString(UNRESOLVED_PARENT, "seat8-orphan.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(lib)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        IRTypeIndex index = new IRTypeIndex(workspace, pass.adapter(), new IRDeclarationReconciler(gm, pass.adapter()));
        IRTypeNode orphan = index.node("seat8.orphan", dataType(lib, "Orphan"));

        String message = assertThrows(GenerationException.class,
                () -> IRPropertyModel.of(orphan, index)).getMessage();
        assertTrue(message.contains("'Missing'"), "the refusal names the reference it could not follow: " + message);
        assertTrue(message.contains("UNRESOLVED"), message);
        assertTrue(message.contains("never guessed"),
                "THE DESIGN (PLAN § C.1): the old generator's own walk (RJavaPojoInterface:565-597 through"
                        + " RuleReferenceTraversal.parentAttributeOf:415-431) reads an unresolved parent as ABSENT,"
                        + " stops with false / null and emits a FLAT pojo that silently lost every inherited member."
                        + " The IR route refuses the file instead, by name: " + message);
    }

    // --------------------------------------------------------------------------- the rendered-type string helpers

    @Test
    void theRenderedTypeHelpersReproduceTheJavaTypeSpelling() {
        assertTrue(IRPropertyModel.isList("List<java.lang.String>"));
        assertFalse(IRPropertyModel.isList("java.util.List"));
        assertEquals("java.lang.String", IRPropertyModel.itemOf("List<java.lang.String>"));
        assertEquals("List", IRPropertyModel.simpleNameOf("List<cdm.base.Foo>"),
                "JavaParameterizedType.getSimpleName() is the WRAPPER's simple name, not the item's");
        assertEquals("Foo", IRPropertyModel.simpleNameOf("cdm.base.Foo"));
        assertEquals("java.util.List", IRPropertyModel.erasure("List<cdm.base.Foo>"),
                "every list erases to java.util.List whatever its item - which is why two list types' setters clash");
        assertEquals("cdm.base.Foo", IRPropertyModel.erasure("cdm.base.Foo"));
        assertEquals("xOverriddenAsSingle",
                IRPropertyModel.incompatiblePropertyName("x", "List<cdm.base.Foo>", "cdm.base.Foo"));
        assertEquals("xOverriddenAsSingleBar",
                IRPropertyModel.incompatiblePropertyName("x", "List<cdm.base.Foo>", "cdm.base.Bar"));
        assertEquals("xOverriddenAsBar",
                IRPropertyModel.incompatiblePropertyName("x", "List<cdm.base.Foo>", "List<cdm.base.Bar>"),
                "list to list recurses on the ITEM types");
        assertEquals("xOverriddenAsInteger",
                IRPropertyModel.incompatiblePropertyName("x", "java.lang.String", "java.lang.Integer"));
    }

    // ------------------------------------------------------------------------------------------------- the harness

    static final String NAMESPACE = "seat8.props";

    /**
     * A type whose {@code extends} names a declaration NOTHING declares - its own source, because the shared fixture
     * must stay reconcilable end to end (the gate's L0 runs every one of its elements).
     */
    private static final String UNRESOLVED_PARENT = """
            namespace seat8.orphan
            version "1.0.0"

            type Orphan extends Missing:
                own string (0..1)
            """;

    static final String SOURCE = """
            namespace seat8.props
            version "1.0.0"

            body Organisation Org1
            corpus Agreement Org1 "Agreement One" Agr1 <"The agreement's text.">
            segment name

            type Leaf:
                id string (1..1)

            type Sub extends Leaf:
                extra string (0..1)

            type Base:
                plain string (0..1) <"the base plain.">
                leaf Leaf (0..1)
                leaves Leaf (0..*)
                tech string (0..1) <"the base tech.">
                    [docReference Org1 Agr1 name "tech"]

            type Child extends Base:
                override plain string (0..1) <"the child plain - dropped by Case 0.">
                override tech string (1..1) <"the child tech - which replaces.">
                override leaf Sub (0..1)
                own boolean (0..1)

            type ListToSingle extends Base:
                override leaves Leaf (0..1)

            type ListToSingleOther extends Base:
                override leaves Sub (0..1)

            type Incompatible extends Base:
                override plain boolean (0..1)

            type SeedBase:
                a string (0..1)

            type SeedChild extends SeedBase:
                b string (0..1)

            type Keyed:
                [metadata key]
                v string (0..1)

            type Templated:
                [metadata key]
                [metadata template]
                v string (0..1)

            type MetaCarrier:
                scheme string (0..1)
                    [metadata scheme]
                refd Leaf (0..1)
                    [metadata reference]
                many Leaf (0..*)
                    [metadata scheme]
                bothmeta Leaf (0..1)
                    [metadata scheme]
                    [metadata reference]

            type Flags:
                both Leaf (0..1)
                    [metadata address]
                    [metadata location]
                ided string (0..1)
                    [metadata id]

            type Documented:
                described string (0..1) <"a & b's end">
                    [docReference Org1 Agr1 name "described" provision "The provision."]
                pathed Leaf (0..1)
                    [docReference for pathed Org1 Agr1 name "pathed"]
                bare string (0..1)

            type MetaBase:
                m string (0..1)
                    [metadata scheme]

            type MetaChild extends MetaBase:
                override m string (0..1)

            choice Either:
                Leaf <"the leaf option.">
                Sub

            type ChoiceExtender extends Either:
                weight number (0..1)

            choice TemplatedChoice:
                [metadata key]
                [metadata template]
                Leaf
                Sub

            choice KeyedChoice:
                [metadata key]
                Leaf <"the keyed leaf option.">
                Sub <"the keyed sub option.">
                Base <"the keyed base option.">

            enum Colour:
                RED
                GREEN

            type EnumHolder:
                colours Colour (0..*)
                tone Colour (0..1)

            type ChainTop:
                hop Leaf (0..*)

            type ChainMid extends ChainTop:
                override hop Leaf (0..1)

            type ChainLeaf extends ChainMid:
                override hop Sub (0..1)

            typeAlias Small: number(digits: 3, fractionalDigits: 0)
            """;

    // ------------------------------------------------- THE SIMPLE-NAME COLLISION FIXTURE (PR #645 commit 6)

    /** The namespace of the type the collision fixture DECLARES under a name a library type also has. */
    static final String COLLIDE_LIB_NAMESPACE = "seat9.collide.lib";

    /** The namespace of the two POJOs whose import set carries BOTH {@code List}s. */
    static final String COLLIDE_NAMESPACE = "seat9.collide";

    /**
     * ONE HALF OF THE COLLISION: a {@code type} whose simple name is {@code List}, in its own namespace. The
     * chaos corpus carries exactly this shape ({@code chaos/s32/a8pkg/List.java} and its siblings in the a2qual /
     * a6fn variants) - "a type named List beside every sibling POJO's java.util.List import (the file-scope law at
     * the POJO seat)", in the corpus author's own words.
     */
    static final String COLLIDE_LIB = """
            namespace seat9.collide.lib
            version "1.0.0"

            type List: <"a model type whose simple name a library type also has.">
                items string (0..*)
            """;

    /**
     * THE OTHER HALF: two POJOs in a SECOND namespace, each importing {@code seat9.collide.lib.List} AND
     * {@code java.util.List} - two different canonical types of ONE simple name in one file's import set, which is
     * the collision the D50 first-claim law decides. The two types decide it in OPPOSITE directions, and only the
     * ORDER of their getters says which way:
     * <ul>
     *   <li>{@code ListLoser} - its first getter is the LIST-shaped one, so the {@code java.util.List} token claims
     *       the simple name and the model type is FQN-inlined and unimported;</li>
     *   <li>{@code ListWinner} - its first getter returns the MODEL type unwrapped, which claims the name first, so
     *       {@code java.util.List} is the loser on the list getter that follows.</li>
     * </ul>
     * A fixture with one direction only would pass under a law that always prefers the library token.
     */
    static final String COLLIDE = """
            namespace seat9.collide
            version "1.0.0"
            import seat9.collide.lib.*

            type ListLoser: <"the LIST-shaped getter comes first, so java.util.List claims the name.">
                boxes List (0..*)
                marker string (0..1)

            type ListWinner: <"the MODEL-typed getter comes first, so the model type claims the name.">
                box List (0..1)
                markers string (0..*)
            """;

    // ------------------------------------------------------- THE SETTER-SHAPE FIXTURE (PR #645 commit 7, § 4)

    /** The namespace of the setter-shape fixture - its OWN, so {@code DECIDABLE}'s enumeration stays what it is. */
    static final String SETTERS_NAMESPACE = "seat9.setters";

    /**
     * THE SETTER SHAPE MATRIX, as a model (v3.3 seat 9, PR #645 commit 7). Section 10's setter group has four
     * shapes ({@code ModelObjectGenerator:866-946}) and the {@code seat8.props} fixture witnesses three of them;
     * this model carries all four PLUS the two parameter-name escapes, in ONE type, so
     * {@code section10TheSetterMatrixIsTheOldGeneratorsOwnOnEveryShape} holds every arm against the old
     * generator's own bytes at once:
     * <ul>
     *   <li>{@code schemed} - single WITH a meta value ({@code setSchemed(FieldWithMetaString)} +
     *       {@code setSchemedValue(String)});</li>
     *   <li>{@code codes} - a BASIC list with a meta value, the eight-line arm whose {@code <V>} is
     *       {@code java.lang.String};</li>
     *   <li>{@code refs} - a MODEL list with {@code [metadata reference]}, the eight-line arm whose {@code <LP>}
     *       takes {@code ? extends} and whose {@code <V>} is the model type;</li>
     *   <li>{@code new} - a JAVA keyword in the SINGLE arm ({@code setNew(String _new)});</li>
     *   <li>{@code package} - a JAVA keyword in the LIST arm ({@code addPackage(String _package)});</li>
     *   <li>{@code Object} - a name the FILE always writes as a type, so it escapes through the
     *       {@code fileWrittenSimpleNames} half of the law rather than the keyword half ({@code _Object});</li>
     *   <li>{@code codess} - a SIBLING whose field identifier is exactly the PLURAL of {@code codes}
     *       ({@code toFirstLower("codes") + "s"}), so section 12's {@code add(List)} / {@code set(List)} parameter
     *       escapes to {@code _codess} ({@code ModelObjectGenerator:1500-1502}) while the bulk meta-value setters
     *       of the SAME property write the plural BARE ({@code :1645-1646}, which writes the raw name + "s" with
     *       no escape at all) - the two laws, in one class, in opposite directions (v3.3 seat 9, PR #645
     *       commit 9).</li>
     * </ul>
     *
     * <p><b>THREE MORE TYPES, for section 12's own escapes</b> (same commit), each in this model rather than in
     * {@code seat8.props} for the reason {@link #settersFixture()} gives:
     * <ul>
     *   <li>{@code Indexed} - an attribute literally named {@code index} beside a LIST of a model item, so the
     *       {@code getOrCreate*(int index)} parameter escapes to {@code _index}
     *       ({@code ModelObjectGenerator:1171}, {@code hasIndexPropertyInScope});</li>
     *   <li>{@code Resulting} - an attribute named {@code result} beside a SINGLE model item, so the
     *       single-cardinality {@code getOrCreate*()} local escapes to {@code _result} ({@code :1200});</li>
     *   <li>{@code Located} - {@code [metadata location]} on a LIST of a model item AND on a single one, which is
     *       the {@code addKey(Key.builder().setScope("DOCUMENT"))} arm in BOTH its shapes ({@code :1177-1179} and
     *       {@code :1199-1201}). The {@code seat8.props} fixture's {@code Flags.both} already reaches the SINGLE
     *       arm ({@code [metadata address] [metadata location]} on {@code Leaf (0..1)} - it is the scoped-key
     *       carrier there); nothing in either model reached the LIST arm before {@code Located.anchors}.</li>
     * </ul>
     *
     * <p><b>THE TWO KEYWORD NAMES WERE CHOSEN AGAINST THE LEXER</b>, not from memory:
     * {@code rune-parser/src/main/antlr4/com/regnosys/rosetta/parser/RosettaLexer.g4} reserves {@code default},
     * {@code switch}, {@code case}, {@code tag}, {@code label}, {@code set}, {@code add}, {@code item},
     * {@code meta}, {@code value}, {@code path} and {@code pattern} as keyword tokens - an attribute of any of
     * those names does not parse. It reserves NEITHER {@code new} NOR {@code package}, both of which are Java
     * keywords ({@code SourceVersion.isName} rejects them, which is what
     * {@code JavaNamingUtil.escapeJavaKeyword} asks), so they are the pair this fixture uses.
     */
    static final String SETTERS = """
            namespace seat9.setters
            version "1.0.0"

            type SLeaf:
                id string (1..1)

            type Setters:
                schemed string (0..1)
                    [metadata scheme]
                codes string (0..*)
                    [metadata scheme]
                codess string (0..1)
                refs SLeaf (0..*)
                    [metadata reference]
                new string (0..1)
                package string (0..*)
                Object string (0..1)
                plainLeaf SLeaf (0..1)
                plainLeaves SLeaf (0..*)

            type Indexed:
                index string (0..1)
                items SLeaf (0..*)

            type Resulting:
                result string (0..1)
                one SLeaf (0..1)

            type Located:
                anchors SLeaf (0..*)
                    [metadata location]
                anchor SLeaf (0..1)
                    [metadata location]
            """;

    record Fixture(RModel lib, GeneratorModel gm, IRDeclarationReconciler passReconciler, IRTypeIndex index) {
    }

    static Fixture fixture() {
        RModel lib = AstBuilder.buildFromString(SOURCE, "seat8-props.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(lib)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        IRTypeIndex index = new IRTypeIndex(workspace, pass.adapter(), new IRDeclarationReconciler(gm, pass.adapter()));
        return new Fixture(lib, gm, pass, index);
    }

    /**
     * The TWO-MODEL workspace of the simple-name collision (v3.3 seat 9, PR #645 commit 6). {@code lib} is the
     * {@code seat9.collide} model - the one that declares the two POJOs under test; {@code seat9.collide.lib} is in
     * the same workspace, so the index resolves the imported type, and both models are under the emission filter.
     */
    static Fixture collisionFixture() {
        RModel collideLib = AstBuilder.buildFromString(COLLIDE_LIB, "seat9-collide-lib.rosetta");
        RModel collide = AstBuilder.buildFromString(COLLIDE, "seat9-collide.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(collideLib, collide)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        IRTypeIndex index = new IRTypeIndex(workspace, pass.adapter(), new IRDeclarationReconciler(gm, pass.adapter()));
        return new Fixture(collide, gm, pass, index);
    }

    /**
     * The one-model workspace of the setter-shape fixture (v3.3 seat 9, PR #645 commit 7). Deliberately NOT folded
     * into {@link #fixture()}: {@code IRDataTypeEmitterTest.DECIDABLE} enumerates the {@code seat8.props} types by
     * name (Rule 4), and adding types to that model would make the enumeration and the population disagree
     * silently.
     */
    static Fixture settersFixture() {
        RModel setters = AstBuilder.buildFromString(SETTERS, "seat9-setters.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(setters)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        IRTypeIndex index = new IRTypeIndex(workspace, pass.adapter(), new IRDeclarationReconciler(gm, pass.adapter()));
        return new Fixture(setters, gm, pass, index);
    }

    /**
     * THE SAME {@code seat8.props} WORKSPACE, under a generator model that DISABLES PRUNING for the named
     * {@code <typeFqn>#<attrRuneName>} pairs (v3.3 seat 9, PR #645 commit 9). It is a second {@link Fixture}
     * rather than a knob on {@link #fixture()} because the pruning set is the GENERATOR's fact, not the model's:
     * the very same declarations must render two different {@code prune()} / {@code hasData()} bodies under two
     * different configs, and holding both against the oracle is what proves the emitter reads the config rather
     * than a property of the type.
     */
    /**
     * THE HOLD-OUT POJO FIXTURES' ROOT (v3.3 seat 9, PR #645 commit 10) - the {@code .rosetta} SOURCES of the
     * {@code holdout-goldens/pojo-*} batteries, which live beside the goldens in the sibling module's test
     * resources ({@code HoldOutGenerationTest}'s own {@code HOLDOUT_ROOT}, one directory up from this module).
     * The path is relative to the surefire working directory, which is the module root.
     */
    static final Path HOLDOUT_ROOT = Path.of("..", "rune-java-generator", "src", "test", "resources", "holdout");

    /**
     * THE FIVE HOLD-OUT POJO MODELS, ENUMERATED BY NAME (Rule 4). They are the old generator's own oracle
     * batteries for exactly the compat arms {@code PojoCompatEmitter}'s class javadoc names ({@code :88-118}):
     * the meta-DROPPING override that does NOT specialize, the same-value meta-KIND change that DOES and
     * reaches the wildcard-to-invariant raw {@code ArrayList} copy, the int-under-number bulk stream form, the
     * number ladder's own rungs and the deep inheritance chain whose {@code parentList Parent (0..10)} is
     * overridden first as a meta-wrapped single {@code Child} and then as a {@code GrandChild}.
     *
     * <p>Their SOURCES are in the tree, so this seat holds the IR emitter against
     * {@code PojoSectionOracle.wholeFile()} - the old generator's own bytes for the same parsed model - rather
     * than against the golden FILES. That is the stronger oracle of the two here: the goldens are the released
     * plugin's output under its own version stamp, while {@code wholeFile()} is literally what
     * {@code ModelObjectGenerator.generate} returns for this very model on this very tree.
     */
    static final List<String> HOLDOUT_POJO_GROUPS =
            List.of("pojo-inheritance", "pojo-number-ladder", "pojo-bulk-meta-drop", "pojo-bulk-meta-kind",
                    "pojo-bulk-value-narrow");

    /**
     * ONE hold-out model as a {@link Fixture}, in the SAME workspace / {@code GeneratorModel} shape as
     * {@link #fixture()}. Each group gets its OWN workspace deliberately: four of the five declare the
     * namespace {@code test.pojo}, and a shared workspace would make their {@code Parent} / {@code Child}
     * declarations collide.
     *
     * <p>The models carry no {@code version} line, so one is stamped exactly as the byte bar stamps it
     * ({@code HoldOutByteCompareTest.generateAllKindsFromFiles}). No builtin model is loaded: the parser's own
     * {@code BuiltinTypeRegistry} resolves {@code string} / {@code int} / {@code number} and the
     * {@code [metadata ...]} annotations, which is the same thing the {@code seat8.props} fixture relies on.
     */
    static Fixture holdOutFixture(String group) {
        RModel model = AstBuilder.buildFromFile(holdOutSource(group));
        if (model.version().isEmpty()) {
            model.setVersion("0.0.0");
        }
        RWorkspace workspace = RWorkspace.build(List.of(model)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        IRTypeIndex index = new IRTypeIndex(workspace, pass.adapter(), new IRDeclarationReconciler(gm, pass.adapter()));
        return new Fixture(model, gm, pass, index);
    }

    /**
     * EVERY {@code .rosetta} source of a group, in ONE workspace (v3.3 seat 9, PR #645 commit 12). Most groups
     * declare a single model and {@link #holdOutFixture} serves them; {@code alias-conditions-twins} declares
     * THREE, because the shape it exists for - two condition classes of one simple name in two namespaces - needs
     * more than one model to exist at all. The models are parsed in sorted order and stamped exactly as the byte
     * bar stamps them.
     */
    record HoldOutWorkspace(List<RModel> models, GeneratorModel gm, IRTypeIndex index) {
    }

    static HoldOutWorkspace holdOutWorkspace(String group) {
        List<RModel> models = new ArrayList<>();
        for (Path source : holdOutSources(group)) {
            RModel model = AstBuilder.buildFromFile(source);
            if (model.version().isEmpty()) {
                model.setVersion("0.0.0");
            }
            models.add(model);
        }
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        IRTypeIndex index = new IRTypeIndex(workspace, pass.adapter(), new IRDeclarationReconciler(gm, pass.adapter()));
        return new HoldOutWorkspace(List.copyOf(models), gm, index);
    }

    /** Every {@code .rosetta} source of a group, sorted - at least one, or the group is not readable. */
    static List<Path> holdOutSources(String group) {
        Path dir = HOLDOUT_ROOT.resolve(group);
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> sources = files.filter(f -> f.toString().endsWith(".rosetta")).sorted().toList();
            assertFalse(sources.isEmpty(), group + ": at least one .rosetta source is expected in " + dir);
            return sources;
        } catch (java.io.IOException e) {
            throw new AssertionError("the hold-out group " + group + " is not readable at " + dir, e);
        }
    }

    /** The group's ONE {@code .rosetta} source - counted rather than globbed, so a renamed file fails loudly. */
    static Path holdOutSource(String group) {
        Path dir = HOLDOUT_ROOT.resolve(group);
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> sources = files.filter(f -> f.toString().endsWith(".rosetta")).sorted().toList();
            assertEquals(1, sources.size(), group + ": exactly one .rosetta source is expected in " + dir);
            return sources.get(0);
        } catch (java.io.IOException e) {
            throw new AssertionError("the hold-out group " + group + " is not readable at " + dir, e);
        }
    }

    @Test
    void theHoldOutPojoFixturesParseAndCarryTheShapesTheyAreHeldFor() {
        assertTrue(Files.isDirectory(HOLDOUT_ROOT),
                "the hold-out sources are COMMITTED, so their absence is a failure and not a skip: "
                        + HOLDOUT_ROOT.toAbsolutePath());
        for (String group : HOLDOUT_POJO_GROUPS) {
            assertFalse(holdOutTypes(holdOutFixture(group)).isEmpty(), group + " declares at least one type");
        }
        // the shapes each battery exists to carry, asserted by the FACTS their arms are gated on
        Fixture inheritance = holdOutFixture("pojo-inheritance");
        IRPropertyModel foo2 = IRPropertyModel.of(
                inheritance.index().node("test.pojo", dataType(inheritance.lib(), "Foo2")), inheritance.index());
        IRPropertyModel.IRProperty parentList = property(foo2, "parentList");
        assertEquals(1, parentList.parentChainDepth(), "Foo2.parentList specializes Foo1's list");
        assertTrue(parentList.parentChain().get(0).multi(),
                "whose rung IS list-shaped - the cardinality change the singletonList / MapperC arms are written"
                        + " for");
        assertTrue(parentList.metaValueType().isPresent(),
                "and the override is [metadata reference], so the main property is meta-wrapped");

        Fixture kind = holdOutFixture("pojo-bulk-meta-kind");
        IRPropertyModel child = IRPropertyModel.of(
                kind.index().node("test.pojo", dataType(kind.lib(), "Child")), kind.index());
        assertEquals("List<com.rosetta.model.metafields.ReferenceWithMetaString>",
                property(child, "attr").javaType(),
                "the meta-KIND change: the child re-wraps the same String value as a ReferenceWithMeta, which is"
                        + " the #412 recorded corner's own carrier");

        Fixture drop = holdOutFixture("pojo-bulk-meta-drop");
        IRPropertyModel dropChild = IRPropertyModel.of(
                drop.index().node("test.pojo", dataType(drop.lib(), "Child")), drop.index());
        assertEquals(0, property(dropChild, "attr").parentChainDepth(),
                "a meta-DROPPING override does NOT specialize at all - the override INHERITS the parent's"
                        + " [metadata scheme] through the union, so its Java type equals the parent's (Case 0)");
    }

    /** Every {@code type} a hold-out model declares, in declaration order. */
    static List<String> holdOutTypes(Fixture f) {
        List<String> names = new ArrayList<>();
        for (var element : f.lib().rootElements()) {
            if (element instanceof RDataType dataType) {
                names.add(dataType.name());
            }
        }
        return names;
    }

    static Fixture pruningFixture(Set<String> doNotPrune) {
        RModel lib = AstBuilder.buildFromString(SOURCE, "seat8-props.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(lib)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true, doNotPrune);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        IRTypeIndex index = new IRTypeIndex(workspace, pass.adapter(), new IRDeclarationReconciler(gm, pass.adapter()));
        return new Fixture(lib, gm, pass, index);
    }

    /**
     * THE FIXTURE MUST PARSE WITH THE NAMES IT CLAIMS (v3.3 seat 9, PR #645 commit 7). Two of them are Java
     * keywords the Rune lexer does NOT reserve and one is upper-initial; if the grammar ever reserved any of them,
     * every setter-matrix assertion would silently be testing a model without the arm it names, so the population
     * is asserted here by name before any byte is compared.
     */
    @Test
    void theSetterShapeFixtureParsesEveryNameItClaims() {
        Fixture f = settersFixture();
        IRPropertyModel setters = IRPropertyModel.of(
                f.index().node(SETTERS_NAMESPACE, dataType(f.lib(), "Setters")), f.index());
        assertEquals("schemed,codes,codess,refs,new,package,Object,plainLeaf,plainLeaves",
                names(setters.ownProperties()),
                "the two Java-keyword names (new, package) and the upper-initial one (Object) parse as attribute"
                        + " names - the lexer reserves none of the three - and so does `codess`, the sibling whose"
                        + " field identifier IS the plural of `codes`");
        assertTrue(property(setters, "schemed").metaValueType().isPresent(), "single WITH a meta value");
        assertTrue(property(setters, "codes").multi(), "a BASIC list with a meta value");
        assertTrue(property(setters, "codes").metaValueType().isPresent());
        assertTrue(property(setters, "refs").multi(), "a MODEL list with [metadata reference]");
        assertTrue(property(setters, "refs").metaValueType().isPresent());
        assertFalse(property(setters, "new").multi(), "the keyword name in the SINGLE arm");
        assertTrue(property(setters, "package").multi(), "and the keyword name in the LIST arm");
        assertFalse(property(setters, "codess").multi(),
                "and the plural-of-a-sibling name is a SINGLE attribute, so it contributes a FIELD identifier"
                        + " `codess` that the list parameter of `codes` would otherwise shadow");

        // the three section-12 escape carriers, asserted by the FACTS their arms are gated on (v3.3 seat 9,
        // PR #645 commit 9) - a fixture whose names parsed but whose shapes were wrong would test nothing
        IRPropertyModel indexed = IRPropertyModel.of(
                f.index().node(SETTERS_NAMESPACE, dataType(f.lib(), "Indexed")), f.index());
        assertEquals("index,items", names(indexed.ownProperties()));
        assertTrue(property(indexed, "items").multi() && property(indexed, "items").itemIsRosettaModelObject(),
                "a LIST of a MODEL item is what makes the generator write getOrCreateItems(int index) at all");
        IRPropertyModel resulting = IRPropertyModel.of(
                f.index().node(SETTERS_NAMESPACE, dataType(f.lib(), "Resulting")), f.index());
        assertEquals("result,one", names(resulting.ownProperties()));
        assertTrue(!property(resulting, "one").multi() && property(resulting, "one").itemIsRosettaModelObject(),
                "and a SINGLE model item is what makes it write the getOrCreateOne() local");
        IRPropertyModel located = IRPropertyModel.of(
                f.index().node(SETTERS_NAMESPACE, dataType(f.lib(), "Located")), f.index());
        assertEquals("anchors,anchor", names(located.ownProperties()));
        assertTrue(property(located, "anchors").attributeMetaTypes().contains("SCOPED_KEY")
                        && property(located, "anchors").multi(),
                "[metadata location] on a LIST of a model item - the addKey arm's LIST shape, which nothing in"
                        + " either fixture reached before");
        assertTrue(property(located, "anchor").attributeMetaTypes().contains("SCOPED_KEY"),
                "and on a single one - the shape Flags.both already carries in seat8.props");
    }

    static IRPropertyModel model(Fixture f, String typeName) {
        return IRPropertyModel.of(f.index().node(NAMESPACE, dataType(f.lib(), typeName)), f.index());
    }

    static IRPropertyModel choiceModel(Fixture f, String choiceName) {
        return IRPropertyModel.of(f.index().node(NAMESPACE, choice(f.lib(), choiceName)), f.index());
    }

    static String names(List<IRPropertyModel.IRProperty> properties) {
        List<String> names = new ArrayList<>();
        for (IRPropertyModel.IRProperty property : properties) {
            names.add(property.name());
        }
        return String.join(",", names);
    }

    static IRPropertyModel.IRProperty property(IRPropertyModel model, String name) {
        for (IRPropertyModel.IRProperty property : model.allProperties()) {
            if (property.name().equals(name)) {
                return property;
            }
        }
        throw new AssertionError("no property named " + name + " in " + names(model.allProperties()));
    }

    static RDataType dataType(RModel model, String name) {
        return model.rootElements().stream().filter(RDataType.class::isInstance).map(RDataType.class::cast)
                .filter(d -> d.name().equals(name)).findFirst().orElseThrow();
    }

    static RChoice choice(RModel model, String name) {
        return model.rootElements().stream().filter(RChoice.class::isInstance).map(RChoice.class::cast)
                .filter(d -> d.name().equals(name)).findFirst().orElseThrow();
    }
}
