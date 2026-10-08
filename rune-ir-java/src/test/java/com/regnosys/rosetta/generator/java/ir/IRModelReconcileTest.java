package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 8 (PR #644) - THE MODEL-LEVEL FACTS, WITNESSED. The namespace DEFINITION and its {@code LinkedHashSet}
 * dedup (the {@code package-info} law), the qualifiable ROOT and the {@code [qualification]} functions matched to it
 * (the {@code *Meta} qualify wing, risk R4), every function's declared SIGNATURE with the parser's synthesized
 * placeholder excluded by the parser's own predicate, and every {@code with-meta} expression with its argument's
 * inferred leaf type - or one of the two NAMED refusals.
 *
 * <p>Since round 1 (MF-2) the ROOT is two producers: the generator's {@code RQualifiableConfig.firstRoot} against
 * {@link IRModelIndex#firstRoot}, the same first-wins-in-load-order law computed over the MODEL NODES. Three
 * witnesses stand behind it - the load-order pair whose SECOND model declares the root, the planted root that reads
 * {@code model.qualify.root.<kind>} RED, and the {@code shouldGenerate} population guard that replaced the
 * one-producer {@code model.shouldGenerate} fact (SF-7).
 */
class IRModelReconcileTest {

    private static final String MODEL = """
            namespace seat8.model : <"The model-level facts the package-info and the Meta stand on.">
            version "1.0.0"

            isProduct root Terms;

            type Terms:
                kind string (1..1)
                notional number (0..1)

            type Other:
                o string (1..1)

            type Holder:
                coded string (0..1)
                    [metadata scheme]
                qty number (0..1)

            func Qualify_Vanilla:
                [qualification Product]
                inputs:
                    terms Terms (1..1)
                output:
                    is_product boolean (1..1)
                set is_product: terms -> kind = "vanilla"

            func Qualify_Stray:
                [qualification Product]
                inputs:
                    other Other (1..1)
                output:
                    is_product boolean (1..1)
                set is_product: other -> o = "stray"

            func Stamp:
                inputs:
                    h Holder (1..1)
                output:
                    c string (0..1)
                        [metadata scheme]
                set c: h -> coded with-meta { scheme: "seat8-scheme" }

            func Nullary:
                output:
                    z string (0..1)
                set z: "z"
            """;

    /** A SECOND file in the SAME namespace carrying the SAME definition - the dedup the package-info law performs. */
    private static final String SAME_NAMESPACE = """
            namespace seat8.model : <"The model-level facts the package-info and the Meta stand on.">
            version "1.0.0"

            type Extra:
                e string (1..1)
            """;

    /**
     * THE LOAD-ORDER PAIR (MF-2). The FIRST model declares a {@code [qualification]} function and NO configuration;
     * the SECOND declares the {@code isProduct root} the wing hangs on. Nothing in the first model can name the root,
     * so the IR half can only find it by scanning EVERY model node of the workspace in load order - which is what
     * {@link IRModelIndex} does and what a per-model read cannot.
     */
    private static final String LATE_A = """
            namespace seat8.late.a
            version "1.0.0"

            type Terms:
                kind string (1..1)

            func Qualify_Early:
                [qualification Product]
                inputs:
                    terms Terms (1..1)
                output:
                    is_product boolean (1..1)
                set is_product: terms -> kind = "vanilla"
            """;

    private static final String LATE_B = """
            namespace seat8.late.b
            version "1.0.0"

            isProduct root Deal;

            type Deal:
                ref string (1..1)

            func Qualify_Late:
                [qualification Product]
                inputs:
                    deal Deal (1..1)
                output:
                    is_product boolean (1..1)
                set is_product: deal -> ref = "late"
            """;

    // --------------------------------------------------------------------------------------------- the laws

    @Test
    void everyModelFactOfTheFixtureReconcilesGreen() {
        Fixture f = fixture(true);
        assertEquals(List.of(), reconcileAll(f));
        assertTrue(f.reconciler().stats()[1] > f.reconciler().stats()[0]);
        assertEquals(2, f.reconciler().stats()[0], "two models, both booked before their adapter ran");
        assertEquals(0, f.reconciler().stats()[2]);
    }

    /**
     * THE PACKAGE-INFO DEDUP ({@code JavaPackageInfoGenerator.java:66-75}): two files, one namespace, one verbatim
     * definition - ONE description. Without the {@code LinkedHashSet} the second model's row is red by name.
     */
    @Test
    void twoModelsOfOneNamespaceSharingADefinitionContributeOneDescription() {
        Fixture green = fixture(true);
        assertEquals(List.of(), reconcileAll(green));

        Fixture lying = fixture(true);
        lying.reconciler().lie(IRDerivedLie.PACKAGE_INFO_NO_DEDUP);
        List<String> mismatches = reconcileAll(lying);
        assertEquals(1, named(mismatches, "model.seat8.model.definitions").size(),
                "only the SECOND model can see the duplicate: " + String.join("\n", mismatches));
    }

    /**
     * THE QUALIFY WING (risk R4): the source half matches the function's first input against the workspace's first
     * {@code isProduct root} by NODE IDENTITY; the IR half matches by RESOLVED QUALIFIED NAME. Exactly one of the
     * two {@code [qualification]} functions names the root, and both halves must say so.
     */
    @Test
    void onlyTheQualificationFunctionWhoseFirstInputIsTheRootMatches() {
        Fixture green = fixture(true);
        assertEquals(List.of(), reconcileAll(green));

        Fixture lying = fixture(true);
        lying.reconciler().lie(IRDerivedLie.QUALIFY_NO_FIRST_INPUT_MATCH);
        List<String> mismatches = reconcileAll(lying);
        assertEquals(1, named(mismatches, "model.qualify.matched").size(),
                "without the first-input test every qualifier claims the root: " + String.join("\n", mismatches));
        assertTrue(mismatches.get(0).contains("Qualify_Stray"),
                "and the one that does not name the root is the witness: " + mismatches.get(0));
    }

    /**
     * THE ROOT IS THE INDEX'S, AND THE INDEX SCANS THE WHOLE WORKSPACE IN LOAD ORDER (MF-2). The root the wing hangs
     * on is declared by the SECOND model; the first model's own node names no configuration at all. The IR half
     * still finds it - {@link IRModelIndex#firstRoot} walks every model node in the workspace's load order - and
     * {@code model.qualify.root.IS_PRODUCT} holds that answer against the generator's own {@code firstRoot}.
     */
    @Test
    void theRootDeclaredByTheSecondModelIsFoundByTheIndexAndTheWingFollowsLoadOrder() {
        Fixture f = lateRoot();
        assertEquals(List.of(), reconcileAll(f));
        assertEquals("seat8.late.b.Deal", f.index().firstRoot("IS_PRODUCT")
                .flatMap(IRType::resolvedQualifiedName).orElse("-"),
                "the SECOND model's configuration, found by the index's own load-order scan");
        assertEquals(Optional.empty(), f.index().firstRoot("IS_EVENT"),
                "and no model declares an isEvent root, which is a root of its own to be absent");

        // the wing that root decides: the FIRST model's qualifier does not name it, the second's does - which is
        // exactly what the all-qualifiers lie contradicts, on the first model's line and on no other
        Fixture lying = lateRoot();
        lying.reconciler().lie(IRDerivedLie.QUALIFY_NO_FIRST_INPUT_MATCH);
        List<String> mismatches = reconcileAll(lying);
        assertEquals(1, named(mismatches, "model.qualify.matched").size(), String.join("\n", mismatches));
        assertTrue(mismatches.get(0).contains("Qualify_Early"),
                "the function whose first input is NOT the root is the witness: " + mismatches.get(0));
    }

    /**
     * THE LYING INDEX (MF-2's own lane): a planted root makes the IR half's first-wins answer disagree with the
     * generator's {@code firstRoot}. The root fact is red on EVERY model's line - it is a workspace fact, asserted
     * wherever a model is reconciled - while {@code model.qualify.matched} is red only where a qualifier exists to
     * be mismatched. A root the IR could not be wrong about would be no fact at all.
     */
    @Test
    void aPlantedRootIsRedAgainstTheGeneratorsOwnFirstWinsScan() {
        Fixture f = fixture(true);
        f.index().plant("IS_PRODUCT", IRTypeNode.reference("Other", IRKind.STRUCT,
                Optional.of("seat8.model"), Optional.of("Other")));
        List<String> mismatches = reconcileAll(f);
        List<String> root = named(mismatches, "model.qualify.root.IS_PRODUCT");
        assertEquals(2, root.size(), "one per model reconciled: " + String.join("\n", mismatches));
        assertTrue(root.get(0).contains("the source says seat8.model.Terms")
                        && root.get(0).contains("the IR says seat8.model.Other"), root.get(0));
        assertEquals(1, named(mismatches, "model.qualify.matched").size(),
                "and the wing follows the lie: the stray qualifier's first input IS the planted root, on the one"
                        + " model that declares a qualifier - " + String.join("\n", mismatches));
        assertEquals(List.of(), named(mismatches, "model.qualify.root.IS_EVENT"),
                "the other kind is untouched - a planted root is one kind's lie, not a blanket failure");
    }

    /**
     * THE POPULATION GUARD, NOT A FACT (SF-7). {@code shouldGenerate} used to be asserted as {@code same(x, TRUE)} -
     * one producer, never red for a reason either half owns, and counted in {@code factsAsserted}. It is a HOST
     * precondition: the namespace definition list this reconcile accumulates is the EMITTED models', so a model
     * outside the filter reaching the reconciler is refused BY NAME.
     */
    @Test
    void aModelOutsideTheCellsEmissionFilterIsRefusedByName() {
        RModel model = AstBuilder.buildFromString(MODEL, "seat8-model.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(model)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> false);
        AstToIRAdapter adapter =
                new AstToIRAdapter(AstToIRAdapter.CorpusResolver.NONE, workspace::getInferredType);
        IRModelReconciler reconciler = new IRModelReconciler(gm, new IRModelIndex(workspace, adapter));
        IRModelNode node = adapter.adaptModelNode(model);
        GenerationException refused =
                assertThrows(GenerationException.class, () -> reconciler.reconcile(model, node));
        assertTrue(refused.getMessage().contains("seat8.model"), refused.getMessage());
        assertTrue(refused.getMessage().contains("not emitted"), refused.getMessage());
    }

    /**
     * THE SYNTHESIZED PLACEHOLDER: a function with no {@code inputs:} block carries the parser's own
     * {@code __synthesized_input__}, which no model wrote and no wrapper collector can claim. Both halves exclude it
     * by {@code GeneratedInputRule.isSynthesized}, so the declared signature reads ZERO inputs.
     */
    @Test
    void aZeroInputFunctionDeclaresNoInputOnEitherHalf() {
        Fixture f = fixture(true);
        assertEquals(List.of(), reconcileAll(f));
        IRModelNode node = f.adapter().adaptModelNode(f.models().get(0));
        boolean found = false;
        for (var signature : node.functionSignatures()) {
            if ("Nullary".equals(signature.name())) {
                found = true;
                assertEquals(0, signature.inputs().size(),
                        "the placeholder is excluded by the parser's own predicate, never by its name");
            }
        }
        assertTrue(found, "the fixture declares a zero-input function");
    }

    /**
     * THE {@code with-meta} REFUSAL, NAMED. An adapter that types no expression makes every use a {@code missing}
     * refusal; the source half asks the real workspace and gets an argument type - so the refusal is a FACT that can
     * be wrong, not a silent drop.
     */
    @Test
    void aWithMetaUseThatTheIrCannotTypeIsARefusalByName() {
        Fixture blind = fixture(false);
        List<String> mismatches = reconcileAll(blind);
        assertEquals(1, named(mismatches, "model.withMeta.0.refusal").size(), String.join("\n", mismatches));
        assertTrue(named(mismatches, "model.withMeta.0.refusal").get(0).contains("the IR says missing"),
                "the refusal token is the law's own vocabulary: " + mismatches);
        assertEquals(1, named(mismatches, "model.withMeta.0.argumentType").size(),
                "and the argument type it did not carry is red beside it: " + String.join("\n", mismatches));
    }

    // ------------------------------------------------------------------------------------------- the harness

    private record Fixture(List<RModel> models, GeneratorModel gm, AstToIRAdapter adapter, IRModelIndex index,
            IRModelReconciler reconciler) {
    }

    private static Fixture fixture(boolean withTypeInferrer) {
        return fixture(withTypeInferrer, MODEL, "seat8-model.rosetta", SAME_NAMESPACE, "seat8-model-b.rosetta");
    }

    /** The load-order pair: LATE_A first, LATE_B - which declares the root - second. */
    private static Fixture lateRoot() {
        return fixture(true, LATE_A, "seat8-late-a.rosetta", LATE_B, "seat8-late-b.rosetta");
    }

    private static Fixture fixture(boolean withTypeInferrer, String firstSource, String firstFile,
            String secondSource, String secondFile) {
        RModel model = AstBuilder.buildFromString(firstSource, firstFile);
        RModel same = AstBuilder.buildFromString(secondSource, secondFile);
        RWorkspace workspace = RWorkspace.build(List.of(model, same)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        AstToIRAdapter adapter = new AstToIRAdapter(AstToIRAdapter.CorpusResolver.NONE,
                withTypeInferrer ? workspace::getInferredType : AstToIRAdapter.TypeInferrer.NONE);
        // the index shares the pass's OWN adapter, as the generator wires it: one CorpusResolver, one node per model
        IRModelIndex index = new IRModelIndex(workspace, adapter);
        return new Fixture(List.of(model, same), gm, adapter, index, new IRModelReconciler(gm, index));
    }

    /** The pass's own shape: the node comes from the INDEX, so each model is adapted exactly once per cell. */
    private static List<String> reconcileAll(Fixture f) {
        List<String> mismatches = new ArrayList<>();
        for (RModel model : f.models()) {
            f.reconciler().attempt();
            mismatches.addAll(f.reconciler().reconcile(model, f.index().node(model)));
        }
        return mismatches;
    }

    private static List<String> named(List<String> messages, String fact) {
        List<String> hits = new ArrayList<>();
        for (String message : messages) {
            if (message.contains(": " + fact + " - ")) {
                hits.add(message);
            }
        }
        return hits;
    }
}
