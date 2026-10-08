package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathScan;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 8 (PR #644) - THE DERIVED-FILE FACTS, WITNESSED GREEN, law by law, on a corpus-free fixture.
 *
 * <p>Each test names the law it proves and asserts it on a fixture built to exercise it: a two-rung alias chain, an
 * {@code int}-typed attribute whose envelope comes ENTIRELY from the builtin registry, a parameterised alias
 * carrying a condition (the refusal that withholds a WHOLE type-format validator), an inherited condition chain, a
 * {@code [metadata key]} type whose synthetic {@code meta} property the validators skip, a one-of type with a deep
 * feature map, and a type whose simple name collides with {@code java.lang}.
 *
 * <p>Green here means the two halves AGREE; that they can DISAGREE is {@link IRDerivedGateTest}'s job.
 */
class IRDerivedFactsReconcileTest {

    // ------------------------------------------------------------------------------------------- the fixture

    private static final String DERIVED = """
            namespace seat8.derived
            version "1.0.0"

            typeAlias Small: number(digits: 3, fractionalDigits: 0)

            typeAlias Code: string(minLength: 1, maxLength: 4)

            typeAlias Layered: Code

            typeAlias Coded: string(minLength: 3, maxLength: 3, pattern: "[A-Z]{3}")

            typeAlias Guarded(bound int): number(digits: 10, fractionalDigits: 0)
                condition GuardedNonNeg:
                    item >= 0

            type Party:
                id string (1..1)

            type Base:
                qty Small (1..1)
                condition BaseRule:
                    qty exists

            type Priced extends Base:
                ccy Layered (0..1)
                tags string (0..*)
                party Party (0..1)
                    [metadata reference]
                condition:
                    ccy exists or tags exists

            type PricedChild extends Priced:
                override party Party (0..1)

            type Counted:
                n int (1..1)
                code Coded (0..1)

            type TwoUnnamed:
                a string (0..1)
                b string (0..1)
                condition Named:
                    a exists
                condition:
                    b exists
                condition:
                    a exists or b exists

            type Refused:
                risky Guarded(bound: 5) (1..1)
                plain Code (1..1)

            type Keyed:
                [metadata key]
                k string (1..1)

            type Leg:
                rate string (0..1)
                notional Small (0..1)
                tenor Code (0..1)

            type Deep:
                alpha Leg (0..1)
                beta Leg (0..1)
                gamma Leg (0..1)
                condition: one-of

            type NotDeep:
                a string (0..1)
                b string (0..*)
                condition: one-of

            type Error:
                e string (1..1)

            type Marker:
                m string (0..1)

            type Carrier:
                mark Marker (0..1)

            type MetaOneOf:
                alpha1 Carrier (0..1)
                    [metadata reference]
                second Carrier (0..1)
                    [metadata reference]
                mark Marker (0..1)
                    [metadata reference]
                condition: one-of

            type PlainOneOf:
                alpha1 Carrier (0..1)
                second Carrier (0..1)
                mark Marker (0..1)
                condition: one-of

            choice Pick:
                Leg
                Party
            """;

    // --------------------------------------------------------------------------------------------- the laws

    @Test
    void everyValidatedElementOfTheFixtureReconcilesGreenOnBothHalves() {
        Fixture f = fixture();
        List<String> mismatches = new ArrayList<>();
        for (RRootElement element : validatedElements(f.model())) {
            mismatches.addAll(reconcile(f, element));
        }
        assertEquals(List.of(), mismatches, "the source and the IR must agree on every derived fact");
        assertTrue(f.reconciler().stats()[1] > f.reconciler().stats()[0],
                "every element asserts more than one fact: " + f.reconciler().stats()[1] + " facts over "
                        + f.reconciler().stats()[0] + " elements");
        assertEquals(0, f.reconciler().stats()[2]);
    }

    /**
     * THE WHOLE-VALIDATOR REFUSAL ({@code TypeFormatValidatorGenerator.java:240-260}): {@code Refused.risky} is typed
     * through a PARAMETERISED alias that carries a condition, so the type-format validator of the WHOLE element is
     * withheld - {@code plain}'s {@code string} check goes with it. The emitter must write no file where the old
     * generator wrote none: the refusal is a FACT both halves state ({@code typeFormat.refusesValidator}, and
     * {@code typeFormat.size} reads 0 on both), while the WALK still records the element's two envelopes - the D11
     * host's {@code DERIVED TWO-READS} line subtracts every REFUSED element's own record from the walk (the decode
     * probe's closure, {@code text + refusedChecks == irWalk}), so a withheld check is never a silent exclusion here.
     */
    @Test
    void aParameterisedAliasWithAConditionWithholdsTheWholeTypeFormatValidator() {
        Fixture f = fixture();
        int before = f.reconciler().checkCounts()[0];
        assertEquals(List.of(), reconcile(f, dataType(f.model(), "Refused")));
        assertEquals(before + 2, f.reconciler().checkCounts()[0],
                "the walk records the refused element's two envelopes (walkAll) - the host subtracts them by the refusal");
        assertEquals(2, f.reconciler().typeFormatChecksByElement().get("seat8.derived.Refused"),
                "the refused element's own record, keyed by its qualified name, is what the host subtracts");

        int beforeCounted = f.reconciler().checkCounts()[0];
        assertEquals(List.of(), reconcile(f, dataType(f.model(), "Counted")));
        assertEquals(beforeCounted + 2, f.reconciler().checkCounts()[0],
                "an int attribute (its envelope seeded by the builtin registry alone) and a pattern-carrying alias"
                        + " each earn one check");
    }

    /**
     * THE CARDINALITY SKIP ({@code CardinalityValidatorGenerator.java:134-137}): a fully unbounded member
     * ({@code 0..*}) is SKIPPED, every other member is checked - and the inherited member comes first, in the POJO's
     * own order.
     */
    @Test
    void aFullyUnboundedMemberIsSkippedAndEveryOtherMemberIsChecked() {
        Fixture f = fixture();
        int before = f.reconciler().checkCounts()[1];
        int members = f.reconciler().checkCounts()[2];
        assertEquals(List.of(), reconcile(f, dataType(f.model(), "Priced")));
        assertEquals(members + 4, f.reconciler().checkCounts()[2],
                "only-exists takes every member: the inherited qty plus ccy, tags and party");
        assertEquals(before + 3, f.reconciler().checkCounts()[1],
                "cardinality takes every member BUT the 0..* one");
    }

    /**
     * THE SYNTHETIC {@code meta} SKIP ({@code ValidatorScan.java:111-113}): a {@code [metadata key]} type carries a
     * synthetic {@code meta} POJO property that no model attribute backs, and the validators never scan it.
     */
    @Test
    void theSyntheticMetaPropertyIsNotAValidatedMember() {
        Fixture f = fixture();
        int before = f.reconciler().checkCounts()[2];
        assertEquals(List.of(), reconcile(f, dataType(f.model(), "Keyed")));
        assertEquals(before + 1, f.reconciler().checkCounts()[2],
                "Keyed has ONE validated member - the synthetic meta is skipped on both halves");
    }

    /**
     * THE DEEP-PATH FEATURE MAP AND ITS ORDER (D13, risk R6): the fixture's one-of type has THREE deep features, so
     * its {@code HashMap} iteration order is not the trivial order of a zero- or one-feature element - which is the
     * only population on which the order law says anything at all.
     */
    @Test
    void theDeepFeatureMapAndItsHashMapOrderAgreeOnANonTrivialElement() {
        Fixture f = fixture();
        DeepPathScan scan = new DeepPathScan(f.gm());
        RDataType deep = dataType(f.model(), "Deep");
        assertTrue(scan.isEligible(deep), "the one-of type with every attribute 0..1 is deep-path eligible");
        assertEquals(3, scan.findDeepFeatureMap(deep).size(),
                "three features: the option type's own attributes, intersected across the three alternatives");
        assertEquals(List.of(), reconcile(f, deep), "and the IR reproduces both the set and the ORDER");

        assertTrue(!scan.isEligible(dataType(f.model(), "NotDeep")),
                "a one-of type with a 0..* attribute is NOT eligible - the singular-optional clause");
        assertEquals(List.of(), reconcile(f, dataType(f.model(), "NotDeep")));
    }

    /**
     * THE METADATA-COLLAPSE VALUE SWAP, WHICH DECIDES KEY MEMBERSHIP ({@code DeepPathScan:250-256}). The two
     * fixtures differ in ONE thing - whether the one-of type's attributes carry {@code [metadata reference]}:
     *
     * <ul>
     *   <li>{@code MetaOneOf.mark} has meta; {@code Carrier.tag} (the deep feature the descent finds) has not. Round
     *       1 therefore SWAPS the value on key {@code mark} to {@code Carrier}'s instance, and in round 3 - the
     *       round that would have RETAINED {@code mark} as its own attribute - the identity test
     *       {@code attr.equals(attributeToRetain)} no longer fires, so {@code mark} is REMOVED and the map is
     *       EMPTY: the generator writes the empty {@code MetaOneOfDeepPathUtil}.</li>
     *   <li>{@code PlainOneOf} is the same shape WITHOUT the meta: no swap, the retain fires, the map keeps
     *       {@code mark}.</li>
     * </ul>
     *
     * <p>This is the cdm/5.38.0 {@code cdm.event.common.SettlementOrigin} mechanism in miniature (nine
     * {@code (0..1) [metadata reference]} attributes, one of them {@code settlementTerms}, which every payout
     * carries WITHOUT meta through {@code PayoutBase}; the golden {@code SettlementOriginDeepPathUtil} is the empty
     * class). An IR half that skipped the swap kept {@code settlementTerms} and would have emitted a
     * {@code chooseSettlementTerms} the golden has not got.
     */
    @Test
    void theMetadataCollapseSwapEmptiesTheFeatureMapOfAMetaAnnotatedOneOf() {
        Fixture f = fixture();
        DeepPathScan scan = new DeepPathScan(f.gm());
        RDataType meta = dataType(f.model(), "MetaOneOf");
        RDataType plain = dataType(f.model(), "PlainOneOf");
        assertTrue(scan.isEligible(meta) && scan.isEligible(plain), "both shapes are deep-path eligible");
        assertEquals(0, scan.findDeepFeatureMap(meta).size(),
                "the swap costs the meta-annotated type its retained attribute");
        assertEquals(List.of("mark"), new ArrayList<>(scan.findDeepFeatureMap(plain).keySet()),
                "the same shape without the meta keeps it");
        assertEquals(List.of(), reconcile(f, meta), "and the IR reproduces the EMPTY map");
        assertEquals(List.of(), reconcile(f, plain), "and the non-empty one");
    }

    /**
     * RISK R7, WITNESSED RATHER THAN ASSUMED (round 1, SF-4). The unnamed-condition class name is the CORPUS-FITTED
     * law {@code ModelMetaGenerator.java:297-299} writes: the suffix is the count of ALL NAMED conditions of the
     * declaring type, so a type with ONE named and TWO unnamed conditions mints the SAME class name twice. No 9.83.0
     * type carries two unnamed conditions and no golden pins a different behaviour, so the emitter must write what
     * the data-rule generator writes - duplicate and all. Three reads say so here: the generator's own
     * {@code collectConditionRefs} through the seam, this reconciler's mirror walk, and the IR chain.
     */
    @Test
    void twoUnnamedConditionsOnOneTypeTakeTheSameNameOnAllThreeReads() {
        Fixture f = fixture();
        RDataType twice = dataType(f.model(), "TwoUnnamed");
        assertEquals(List.of(), reconcile(f, twice),
                "the corpus-fitted law is reproduced AS WRITTEN on every read");
        List<String> generator = new ArrayList<>();
        for (ModelMetaGenerator.ConditionRef ref : new ModelMetaGenerator(f.gm(),
                new JavaTypeTranslator(new JavaTypeUtil())).collectConditionRefs(twice)) {
            generator.add(ref.simpleName());
        }
        assertEquals(List.of("TwoUnnamedNamed", "TwoUnnamedDataRule1", "TwoUnnamedDataRule1"), generator,
                "ONE named condition, so BOTH unnamed ones take suffix 1 - the latent duplicate, stated");
    }

    /** THE {@code java.lang} COLLISION LAW (risk R3): one function, two callers, one answer. */
    @Test
    void theJavaLangCollisionLawIsOneSharedFunction() {
        assertTrue(IRJavaLangCollision.collides("Error"));
        assertTrue(IRJavaLangCollision.collides("Exception"));
        assertTrue(IRJavaLangCollision.collides("Math"));
        assertTrue(!IRJavaLangCollision.collides("Party"));
        assertTrue(!IRJavaLangCollision.collides(""), "a blank name resolves nothing");
        assertTrue(!IRJavaLangCollision.collides(null));
        Fixture f = fixture();
        assertEquals(List.of(), reconcile(f, dataType(f.model(), "Error")),
                "and the colliding element name agrees on both halves");
    }

    /** THE CHOICE ARM: a choice's options are its validated members, each 0..1 by choice semantics. */
    @Test
    void aChoicesOptionsAreItsValidatedMembers() {
        Fixture f = fixture();
        int before = f.reconciler().checkCounts()[2];
        assertEquals(List.of(), reconcile(f, choice(f.model(), "Pick")));
        assertEquals(before + 2, f.reconciler().checkCounts()[2]);
    }

    // ------------------------------------------------------------------------------------------- the harness

    record Fixture(RModel model, GeneratorModel gm, IRTypeIndex index, IRDerivedFactsReconciler reconciler) {
    }

    static Fixture fixture() {
        return fixture(DERIVED, "seat8-derived.rosetta");
    }

    static Fixture fixture(String source, String fileName) {
        RModel model = AstBuilder.buildFromString(source, fileName);
        RWorkspace workspace = RWorkspace.build(List.of(model)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        AstToIRAdapter adapter =
                new AstToIRAdapter(AstToIRAdapter.CorpusResolver.NONE, workspace::getInferredType);
        IRDeclarationReconciler parent = new IRDeclarationReconciler(gm, adapter);
        IRTypeIndex index = new IRTypeIndex(workspace, adapter, parent);
        // the model index is a CONSTRUCTOR argument of the reconciler since v3.3 seat 9, PR #645 commit 13 (the
        // qualify wing's IR half reads it) - built here on the fixture's OWN adapter, as the pass builds it
        return new Fixture(model, gm, index,
                new IRDerivedFactsReconciler(gm, new IRModelIndex(workspace, adapter)));
    }

    static List<String> reconcile(Fixture f, RRootElement element) {
        String namespace = f.model().namespace();
        IRTypeNode node = element instanceof RChoice ch
                ? f.index().node(namespace, ch)
                : f.index().node(namespace, (RDataType) element);
        IRPropertyModel properties = IRPropertyModel.of(node, f.index());
        f.reconciler().attempt();
        return f.reconciler().reconcile(namespace, element, node, properties, f.index());
    }

    static List<RRootElement> validatedElements(RModel model) {
        List<RRootElement> elements = new ArrayList<>();
        for (RRootElement element : model.rootElements()) {
            if (element instanceof RDataType || element instanceof RChoice) {
                elements.add(element);
            }
        }
        return elements;
    }

    static RDataType dataType(RModel model, String name) {
        for (RRootElement element : model.rootElements()) {
            if (element instanceof RDataType dataType && dataType.name().equals(name)) {
                return dataType;
            }
        }
        throw new IllegalArgumentException("no data type " + name);
    }

    static RChoice choice(RModel model, String name) {
        for (RRootElement element : model.rootElements()) {
            if (element instanceof RChoice ch && ch.name().equals(name)) {
                return ch;
            }
        }
        throw new IllegalArgumentException("no choice " + name);
    }

    /** Every mismatch naming {@code fact} - the fact's own name, which is what a mutation lane greps. */
    static List<String> named(List<String> messages, String fact) {
        List<String> hits = new ArrayList<>();
        for (String message : messages) {
            if (message.contains(": " + fact + " - ")) {
                hits.add(message);
            }
        }
        return hits;
    }

    /** The FAMILY prefix of every mismatch, so a lane can assert that nothing outside its family moved. */
    static List<String> families(List<String> messages) {
        List<String> families = new ArrayList<>();
        for (String message : messages) {
            int start = message.indexOf(": ");
            int end = message.indexOf(" - the source says");
            if (start < 0 || end < 0) {
                families.add(message);
                continue;
            }
            String fact = message.substring(start + 2, end);
            int dot = fact.indexOf('.');
            families.add(dot < 0 ? fact : fact.substring(0, dot));
        }
        return families;
    }
}
