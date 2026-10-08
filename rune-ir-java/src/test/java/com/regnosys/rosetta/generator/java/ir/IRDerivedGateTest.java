package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;

import static com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconcileTest.choice;
import static com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconcileTest.dataType;
import static com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconcileTest.families;
import static com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconcileTest.fixture;
import static com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconcileTest.named;
import static com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconcileTest.reconcile;
import static com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconcileTest.validatedElements;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 8 (PR #644) - THE DERIVED-FACT GATE, PROVEN ABLE TO FAIL. Lanes D01-D13 plus the two control lanes, each
 * a LYING HALF: one named law of one half is disabled ({@link IRDerivedLie}) and the reconcile must
 * read RED naming THAT fact family and NOTHING else. A gate that cannot fail is not a gate - and a gate that fails
 * everywhere names nothing.
 *
 * <p>Two of the lanes lie to the RECONCILER'S OWN SOURCE WALK rather than to the IR (D02's alias recursion, D05's
 * condition chain, and the G2 control's meta union). Those are the independence proofs: if the two halves were one
 * producer, disabling a law on one of them would keep the fact green.
 *
 * <p>L0, the unmodified fixture, is {@link IRDerivedFactsReconcileTest}'s own green pass; it is asserted here too,
 * because a lane that reads RED against a tree that was already red proves nothing.
 */
class IRDerivedGateTest {

    // ------------------------------------------------------------------------------------------------- L0

    @Test
    void l0TheUnmodifiedFixtureIsGreen() {
        assertEquals(List.of(), everything(IRDerivedLie.NONE));
    }

    // ------------------------------------------------------------------- D01: the type-format number envelope

    @Test
    void d01TheNumberEnvelopeLosesItsRegistryBaseAndTheIntAttributesCheckVanishes() {
        List<String> mismatches = lane(IRDerivedLie.NUMBER_SLOT_NO_BASE, "Counted");
        assertEquals(1, named(mismatches, "typeFormat.n.constrained").size(), messages(mismatches));
        assertTrue(named(mismatches, "typeFormat.n.number").get(0).contains("the IR says -"),
                "the IR half invents no envelope at all once the registry seed is gone: " + messages(mismatches));
        assertEquals(Set.of("typeFormat"), familiesOf(mismatches));
    }

    // -------------------------------------------------------------------------- D02: the alias chain's recursion

    @Test
    void d02TheSourceAliasWalkStopsAtItsFirstRungAndTheChainIsRed() {
        List<String> mismatches = lane(IRDerivedLie.ALIAS_CHAIN_NO_RECURSION, "Priced");
        assertEquals(1, named(mismatches, "typeFormat.ccy.aliasChain").size(), messages(mismatches));
        assertEquals(1, named(mismatches, "typeFormat.ccy.aliasChain.scanAgrees").size(),
                "the scan's own answer is the third read, and it disagrees with the truncated walk too: "
                        + messages(mismatches));
        assertEquals(Set.of("typeFormat"), familiesOf(mismatches));
    }

    // -------------------------------------------------------------------------------- D03: the cardinality skip

    @Test
    void d03TheFullyUnboundedSkipIsInvertedAndEveryMembersSkipFlagIsRed() {
        List<String> mismatches = lane(IRDerivedLie.CARDINALITY_SKIP_INVERTED, "Priced");
        assertEquals(4, named(mismatches, "cardinality.qty.skipped").size()
                + named(mismatches, "cardinality.ccy.skipped").size()
                + named(mismatches, "cardinality.tags.skipped").size()
                + named(mismatches, "cardinality.party.skipped").size(), messages(mismatches));
        assertEquals(1, named(mismatches, "cardinality.size").size(), messages(mismatches));
        assertEquals(Set.of("cardinality"), familiesOf(mismatches));
    }

    // -------------------------------------------------------------------------- D04: the synthetic meta skip

    @Test
    void d04TheSyntheticMetaIsNotSkippedAndTheOnlyExistsPopulationIsRed() {
        List<String> mismatches = lane(IRDerivedLie.ONLY_EXISTS_NO_SYNTHETIC_SKIP, "Keyed");
        assertEquals(1, named(mismatches, "onlyExists.size").size(), messages(mismatches));
        assertEquals(1, named(mismatches, "onlyExists.members").size(), messages(mismatches));
        assertEquals(1, named(mismatches, "onlyExists.syntheticMetaSkipped").size(), messages(mismatches));
        assertEquals(Set.of("onlyExists"), familiesOf(mismatches));
    }

    // -------------------------------------------------------------------- D05: the condition chain's direction

    @Test
    void d05TheConditionChainCollectedElementFirstIsRed() {
        List<String> mismatches = lane(IRDerivedLie.CONDITION_CHAIN_ELEMENT_FIRST, "Priced");
        assertEquals(1, named(mismatches, "meta.conditionRefs").size(), messages(mismatches));
        assertEquals(1, named(mismatches, "meta.conditionRefs.scanAgrees").size(),
                "the GENERATOR'S OWN collectConditionRefs is the third read (round 1, SF-3), and it disagrees with"
                        + " the element-first walk too: " + messages(mismatches));
        assertTrue(named(mismatches, "meta.conditionRefs").get(0).contains("BaseBaseRule"),
                "the inherited condition's class name must appear on one side and not the other: "
                        + messages(mismatches));
        assertEquals(Set.of("meta"), familiesOf(mismatches));
    }

    // ------------------------------------------------- D05b: the unnamed condition's CORPUS-FITTED suffix (risk R7)

    /**
     * The unnamed-condition suffix is the count of ALL NAMED conditions of the declaring type, not the unnamed
     * condition's own ordinal - so a type with one named and two unnamed conditions mints ONE class name twice. The
     * lie indexes by ordinal instead, which is the rule a reasonable author would have written and which no 9.83.0
     * golden supports. Both source reads catch it: this reconciler's mirror walk against the GENERATOR'S OWN answer
     * ({@code meta.conditionRefs.scanAgrees}) and the mirror against the IR ({@code meta.conditionRefs}).
     */
    @Test
    void d05bIndexingAnUnnamedConditionByItsOwnOrdinalIsRed() {
        List<String> mismatches =
                lane(IRDerivedLie.CONDITION_UNNAMED_BY_ORDINAL, "TwoUnnamed");
        assertEquals(1, named(mismatches, "meta.conditionRefs").size(), messages(mismatches));
        assertEquals(1, named(mismatches, "meta.conditionRefs.scanAgrees").size(), messages(mismatches));
        assertTrue(named(mismatches, "meta.conditionRefs").get(0).contains("TwoUnnamedDataRule0"),
                "the ordinal law invents a class name the data-rule generator never writes: "
                        + messages(mismatches));
        assertEquals(Set.of("meta"), familiesOf(mismatches));

        assertEquals(List.of(), lane(IRDerivedLie.CONDITION_UNNAMED_BY_ORDINAL, "Priced"),
                "a type with ONE unnamed condition cannot tell the two laws apart - which is exactly why the"
                        + " corpus has no witness and the fixture had to grow one");
    }

    // ------------------------------------------------------- D06: every condition's KIND, named ones included

    /**
     * A LYING IR NODE rather than a lying walk: the unnamed condition's KIND is re-stated as {@code Choice} where the
     * expression root says {@code DataRule}. Three facts catch it - the kind list itself, the unnamed subset, and the
     * condition-class NAME the {@code *Meta} would import ({@code PricedChoice0} against {@code PricedDataRule0}).
     */
    @Test
    void d06AMangledConditionKindIsRedByName() {
        IRDerivedFactsReconcileTest.Fixture f = fixture();
        RDataType priced = dataType(f.model(), "Priced");
        IRTypeNode honest = f.index().node(f.model().namespace(), priced);
        assertEquals(1, honest.conditionKinds().size(), "Priced declares exactly one condition");
        List<String> kinds = new ArrayList<>(honest.conditionKinds());
        kinds.set(0, "Choice");
        f.index().plant(priced, new IRTypeNode(honest.name(), honest.kind(), honest.fields(), honest.baseType(),
                honest.isAbstract(), honest.sourceRange(), honest.metadata(), honest.namespace(),
                honest.resolvedName(), honest.definition(), honest.docReferences(), honest.annotations(),
                honest.conditionNames(), honest.effectiveBase(), honest.typeParameters(),
                honest.baseTypeArguments(), kinds, honest.aliasChain()));

        List<String> mismatches = reconcile(f, priced);
        assertEquals(1, named(mismatches, "meta.conditionKinds").size(), messages(mismatches));
        assertEquals(1, named(mismatches, "meta.unnamedKind").size(), messages(mismatches));
        assertEquals(1, named(mismatches, "meta.conditionRefs").size(), messages(mismatches));
        assertEquals(Set.of("meta"), familiesOf(mismatches));
    }

    // -------------------------------------------------------------------------- D10: deep-path eligibility

    @Test
    void d10DeepPathEligibilityWithoutTheSingularOptionalClauseIsRed() {
        List<String> mismatches =
                lane(IRDerivedLie.DEEP_ELIGIBLE_NO_SINGULAR_CLAUSE, "NotDeep");
        assertEquals(1, named(mismatches, "deepPath.eligible").size(), messages(mismatches));
        assertEquals(Set.of("deepPath"), familiesOf(mismatches));
    }

    // -------------------------------------------------------------------------------- D12: the collision law

    @Test
    void d12TheJavaLangCollisionPredicateForcedFalseIsRed() {
        List<String> mismatches = lane(IRDerivedLie.COLLISION_ALWAYS_FALSE, "Error");
        assertEquals(1, named(mismatches, "collision.Error").size(), messages(mismatches));
        assertEquals(Set.of("collision"), familiesOf(mismatches));
    }

    // ------------------------------------------------------------------------- D13: the deep-path HASHMAP ORDER

    @Test
    void d13TheDeepFeatureOrderIsAHashMapOrderAndNoOtherContainerReproducesIt() {
        List<String> mismatches = lane(IRDerivedLie.DEEP_ORDER_NOT_HASH_ORDER, "Deep");
        assertEquals(1, named(mismatches, "deepPath.order").size(),
                "the SAME three keys in another map read RED on the order alone: " + messages(mismatches));
        assertEquals(List.of(), named(mismatches, "deepPath.features"),
                "and GREEN on the set - which is exactly what makes the order a fact of its own");
        assertEquals(Set.of("deepPath"), familiesOf(mismatches));
    }

    // --------------------------------------------------- D14: the deep-path METADATA-COLLAPSE value swap

    /**
     * The swap is not cosmetic: without it the retained attribute keeps its identity into a later round and the key
     * SURVIVES. {@code MetaOneOf} is the corpus mechanism in miniature - cdm/5.38.0's {@code SettlementOrigin} reads
     * exactly this way, and the golden is the EMPTY util class.
     */
    @Test
    void d14DroppingTheMetadataCollapseSwapKeepsAKeyTheGoldenDoesNotHave() {
        List<String> mismatches = lane(IRDerivedLie.DEEP_META_COLLAPSE_DROPPED, "MetaOneOf");
        assertEquals(1, named(mismatches, "deepPath.order").size(), messages(mismatches));
        assertTrue(named(mismatches, "deepPath.order").get(0).contains("the IR says mark"),
                "the IR keeps the key the swap removes: " + messages(mismatches));
        assertEquals(1, named(mismatches, "deepPath.features").size(), messages(mismatches));
        assertEquals(Set.of("deepPath"), familiesOf(mismatches));

        assertEquals(List.of(),
                lane(IRDerivedLie.DEEP_META_COLLAPSE_DROPPED, "PlainOneOf"),
                "the same shape WITHOUT the meta is untouched by the lie - the swap's predicate is the meta");
    }

    // ----------------------------------------------------------- G2: the two halves are two producers (LAW 69)

    @Test
    void g2TheReconcilersOwnMetaUnionLosesItsParentLegAndTheOverrideIsRed() {
        List<String> mismatches = lane(IRDerivedLie.META_UNION_NO_PARENT_LEG, "PricedChild");
        assertEquals(1, named(mismatches, "typeFormat.party.metaWrapped").size(),
                "the override inherits [metadata reference] from its parent; without the parent leg it does not: "
                        + messages(mismatches));
        assertTrue(familiesOf(mismatches).contains("typeFormat"));
        assertTrue(familiesOf(mismatches).contains("collision"),
                "the cast-site item type is the wrapper's name only under the union: " + messages(mismatches));
    }

    /**
     * The control that makes every lane above mean something: a lie is not a blanket failure. Each lane's mismatch
     * count is SMALL and its families are the ones named - if a lie reddened everything, "the family is named" would
     * carry no information.
     */
    @Test
    void everyLaneStaysInsideItsOwnFamilyAcrossTheWholeFixture() {
        for (IRDerivedLie lie : IRDerivedLie.values()) {
            if (lie == IRDerivedLie.NONE || !ownedByTheDerivedFacts(lie)) {
                continue;
            }
            List<String> mismatches = everything(lie);
            assertTrue(!mismatches.isEmpty(), lie + " must be able to fail somewhere on the fixture");
            assertTrue(familiesOf(mismatches).size() <= 2,
                    lie + " reddened " + familiesOf(mismatches) + " - a lane names at most two families");
        }
    }

    // ------------------------------------------------------------------------------------------- the harness

    /** The lies this class owns; the model and wrapper lies belong to their own reconcilers' witnesses. */
    private static boolean ownedByTheDerivedFacts(IRDerivedLie lie) {
        return switch (lie) {
            case PACKAGE_INFO_NO_DEDUP, QUALIFY_NO_FIRST_INPUT_MATCH, WRAPPER_NO_FUNCTION_SOURCE,
                    WRAPPER_ENUM_AS_COMPOSITE, NONE -> false;
            default -> true;
        };
    }

    private static List<String> lane(IRDerivedLie lie, String elementName) {
        IRDerivedFactsReconcileTest.Fixture f = fixture();
        f.reconciler().lie(lie);
        RRootElement element = "Pick".equals(elementName) ? choice(f.model(), elementName)
                : dataType(f.model(), elementName);
        return reconcile(f, element);
    }

    private static List<String> everything(IRDerivedLie lie) {
        IRDerivedFactsReconcileTest.Fixture f = fixture();
        f.reconciler().lie(lie);
        List<String> mismatches = new ArrayList<>();
        for (RRootElement element : validatedElements(f.model())) {
            mismatches.addAll(reconcile(f, element));
        }
        return mismatches;
    }

    private static Set<String> familiesOf(List<String> mismatches) {
        return new LinkedHashSet<>(families(mismatches));
    }

    private static String messages(List<String> mismatches) {
        return String.join("\n", mismatches);
    }
}
