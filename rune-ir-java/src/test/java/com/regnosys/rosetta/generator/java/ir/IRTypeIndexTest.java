package com.regnosys.rosetta.generator.java.ir;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 8 (PR #644) - THE WORKSPACE-WIDE TYPE INDEX, WITNESSED. One node per declaration over the three kinds it
 * admits; a name declared nowhere, a name declared twice and a reference the linker never resolved each refused BY
 * NAME rather than guessed; the parent adapted and reconciled EXACTLY ONCE however many descendants ask for it; and a
 * PLANTED lying parent refusing every descendant, at every later call, rather than being read clean by the next child.
 *
 * <p>Corpus-free: every fixture is an inline {@code .rosetta} source parsed the way {@code IRTypeGateTest} parses its
 * own.
 */
class IRTypeIndexTest {

    private static final String LIB = """
            namespace seat8.index
            version "1.0.0"

            type Leaf:
                id string (1..1)

            type Middle extends Leaf:
                m string (0..1)

            type LeftChild extends Leaf:
                l string (0..1)

            type RightChild extends Leaf:
                r string (0..1)

            choice Either:
                Middle
                LeftChild

            typeAlias Small: number(digits: 3, fractionalDigits: 0)
            """;

    // ------------------------------------------------------------------------------------ one node per declaration

    @Test
    void oneNodePerDeclarationIsHandedOutForEverAcrossTheThreeKinds() {
        Fixture f = fixture();
        RDataType leaf = dataType(f.lib(), "Leaf");
        RChoice either = choice(f.lib(), "Either");
        RTypeAlias small = alias(f.lib(), "Small");

        assertSame(f.index().node(NAMESPACE, leaf), f.index().node(NAMESPACE, leaf),
                "the node the bytes come from must be the node every fact was asserted on");
        assertSame(f.index().node(NAMESPACE, either), f.index().node(NAMESPACE, either));
        assertSame(f.index().node(NAMESPACE, small), f.index().node(NAMESPACE, small));
        assertEquals(IRKind.STRUCT, f.index().node(NAMESPACE, leaf).kind());
        assertEquals(IRKind.CHOICE, f.index().node(NAMESPACE, either).kind());
        assertEquals(IRKind.TYPE_ALIAS, f.index().node(NAMESPACE, small).kind());
    }

    @Test
    void theIndexAdmitsTypesChoicesAndAliasesAndNothingElse() {
        Fixture f = fixture();
        assertTrue(f.index().declaration("seat8.index.Leaf").isPresent());
        assertTrue(f.index().declaration("seat8.index.Either").isPresent());
        assertTrue(f.index().declaration("seat8.index.Small").isPresent());
        assertEquals(Optional.empty(), f.index().declaration("seat8.index.NoSuchThing"),
                "an ABSENT name is empty here - the REFUSAL is the parent lookup's, which states why it cannot go on");
    }

    @Test
    void namespaceOfIsTheDeclaringModelsNamespace() {
        Fixture f = fixture();
        assertEquals(NAMESPACE, IRTypeIndex.namespaceOf(dataType(f.lib(), "Leaf")));
        assertEquals(NAMESPACE, IRTypeIndex.namespaceOf(choice(f.lib(), "Either")));
        assertEquals(NAMESPACE, IRTypeIndex.namespaceOf(alias(f.lib(), "Small")));
    }

    // ------------------------------------------------------------------------------------------- the parent, ONCE

    @Test
    void theParentIsAdaptedAndReconciledExactlyOnceHoweverManyDescendantsAskForIt() {
        Fixture f = fixture();
        IRType leafReference = reference("Leaf");

        IRTypeNode first = f.index().parent(leafReference);
        IRTypeNode second = f.index().parent(reference("Leaf"));
        assertSame(first, second, "the same instance, every time");
        assertEquals(1, f.index().parentReconcileStats()[0],
                "ONE declaration attempted however many descendants resolve it");
        assertTrue(f.index().parentReconcileStats()[1] > 1, "and its facts were asserted, not assumed");
        assertEquals(0, f.index().parentReconcileStats()[2], "the honest parent agrees with its source");
    }

    @Test
    void theParentsCountersAreKeptApartFromThePassesOwn() {
        Fixture f = fixture();
        f.index().parent(reference("Leaf"));
        assertEquals(0, f.passReconciler().stats()[0],
                "a parent may live outside the cell's emission filter - it is never counted in the pass's population");
    }

    // -------------------------------------------------------------------------------------- the refusals, by name

    @Test
    void anUnresolvedReferenceIsRefusedByNameRatherThanGuessed() {
        Fixture f = fixture();
        IRType unresolved = IRTypeNode.reference("Ghost", IRKind.STRUCT, Optional.empty(), Optional.empty());
        String message = assertThrows(GenerationException.class, () -> f.index().parent(unresolved)).getMessage();
        assertTrue(message.contains("is UNRESOLVED"), message);
        assertTrue(message.contains("'Ghost'"), "the refusal names the reference: " + message);
    }

    @Test
    void anAbsentQualifiedNameIsRefusedByNameRatherThanReadAsAFlatType() {
        Fixture f = fixture();
        String message = assertThrows(GenerationException.class,
                () -> f.index().parent(reference("Nowhere"))).getMessage();
        assertTrue(message.contains("seat8.index.Nowhere"), message);
        assertTrue(message.contains("is declared NOWHERE") && message.contains("ABSENT"),
                "an absent parent is a refusal, never a silently flat type: " + message);
    }

    @Test
    void anAmbiguousNameIsRefusedRatherThanFirstWins() {
        RModel first = AstBuilder.buildFromString(TWIN_A, "seat8-twin-a.rosetta");
        RModel second = AstBuilder.buildFromString(TWIN_B, "seat8-twin-b.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(first, second)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        IRTypeIndex index = new IRTypeIndex(workspace, pass.adapter(), new IRDeclarationReconciler(gm, pass.adapter()));

        String message = assertThrows(GenerationException.class,
                () -> index.declaration("seat8.twin.Twin")).getMessage();
        assertTrue(message.contains("declared more than once") && message.contains("ambiguous"),
                "the index keeps NEITHER and refuses - a first-wins pick would type a whole file against a guess: "
                        + message);
    }

    @Test
    void aTypeAliasIsRefusedAsASupertype() {
        Fixture f = fixture();
        IRType aliasReference = IRTypeNode.reference("Small", IRKind.TYPE_ALIAS,
                Optional.of(NAMESPACE), Optional.of("Small"));
        String message = assertThrows(GenerationException.class,
                () -> f.index().parent(aliasReference)).getMessage();
        assertTrue(message.contains("could not be adapted or reconciled"), message);
        assertEquals(1, f.index().parentReconcileStats()[2], "the refusal is BOOKED as a mismatch, never dropped");
    }

    // -------------------------------------------------------------------------------------- the planted lying parent

    @Test
    void aPlantedLyingParentRefusesEveryDescendantAtEveryLaterCall() {
        Fixture f = fixture();
        RNode leaf = dataType(f.lib(), "Leaf");
        IRTypeNode honest = f.passReconciler().adapter().adaptData(NAMESPACE, (RDataType) leaf);
        // the IR says the declaration carries NO field: the source says it carries one
        IRTypeNode lying = new IRTypeNode(honest.name(), honest.kind(), List.of(), honest.baseType(),
                honest.isAbstract(), honest.sourceRange(), honest.metadata(), honest.namespace(),
                honest.resolvedName(), honest.definition(), honest.docReferences(), honest.annotations(),
                honest.conditionNames(), honest.effectiveBase(), honest.typeParameters(),
                honest.baseTypeArguments(), honest.conditionKinds(), honest.aliasChain());
        f.index().plant(leaf, lying);

        String first = assertThrows(GenerationException.class,
                () -> f.index().parent(reference("Leaf"))).getMessage();
        assertTrue(first.contains("disagrees with its source"), first);
        assertTrue(first.contains("fields.size"), "the refusal carries the first mismatch by its fact name: " + first);

        String second = assertThrows(GenerationException.class,
                () -> f.index().parent(reference("Leaf"))).getMessage();
        assertEquals(first, second, "the refusal is MEMOISED - the next child never reads the parent clean");
        assertEquals(1, f.index().parentReconcileStats()[0],
                "and the reconcile is not re-run: one attempt, one verdict, for ever");
        assertTrue(f.index().parentReconcileStats()[2] > 0, "the mismatches are booked on the parent's own counters");
    }

    // ------------------------------------------------------------------------------------------------- the harness

    private static final String NAMESPACE = "seat8.index";

    private static final String TWIN_A = """
            namespace seat8.twin
            version "1.0.0"

            type Twin:
                a string (0..1)
            """;

    private static final String TWIN_B = """
            namespace seat8.twin
            version "1.0.0"

            type Twin:
                b string (0..1)
            """;

    private record Fixture(RModel lib, GeneratorModel gm, IRDeclarationReconciler passReconciler, IRTypeIndex index) {
    }

    private static Fixture fixture() {
        RModel lib = AstBuilder.buildFromString(LIB, "seat8-index.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(lib)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        IRTypeIndex index = new IRTypeIndex(workspace, pass.adapter(), new IRDeclarationReconciler(gm, pass.adapter()));
        return new Fixture(lib, gm, pass, index);
    }

    private static IRType reference(String simpleName) {
        return IRTypeNode.reference(simpleName, IRKind.STRUCT, Optional.of(NAMESPACE), Optional.of(simpleName));
    }

    private static RDataType dataType(RModel model, String name) {
        return model.rootElements().stream().filter(RDataType.class::isInstance).map(RDataType.class::cast)
                .filter(d -> d.name().equals(name)).findFirst().orElseThrow();
    }

    private static RChoice choice(RModel model, String name) {
        return model.rootElements().stream().filter(RChoice.class::isInstance).map(RChoice.class::cast)
                .filter(d -> d.name().equals(name)).findFirst().orElseThrow();
    }

    private static RTypeAlias alias(RModel model, String name) {
        return model.rootElements().stream().filter(RTypeAlias.class::isInstance).map(RTypeAlias.class::cast)
                .filter(d -> d.name().equals(name)).findFirst().orElseThrow();
    }
}
