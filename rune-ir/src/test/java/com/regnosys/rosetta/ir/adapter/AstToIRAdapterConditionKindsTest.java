package com.regnosys.rosetta.ir.adapter;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * v3.3 seat 8 (PR #644 — the property gate), the two facts the type node gained: {@link IRType#conditionKinds()} —
 * index-parallel to {@link IRType#conditionNames()}, the token the old generator names an UNNAMED condition's
 * class by ({@code ModelMetaGenerator.unnamedConditionKind}, the oracle: a {@code one-of} cardinality check →
 * {@code OneOf}, a {@code choice} one → {@code Choice}, anything else → {@code DataRule}); and
 * {@link IRType#aliasChain()} — the rungs of a {@code typeAlias} chain a use site walks, beside the
 * {@code effectiveBase} that COLLAPSES the same chain. The kind is stated for EVERY condition, named ones
 * included: a half-stated fact is refused by the record, and a fact stated only where it is ambiguous can never be
 * reconciled against the whole population.
 *
 * <p>This class also holds the two RECORD laws the adapter cannot reach from a {@code .rosetta} source — a kinds
 * list of the wrong length or with a token outside the vocabulary, and an alias chain on a node that is no
 * {@code TYPE_ALIAS} — and the CYCLE witness, which lives here rather than in
 * {@code AstToIRAdapterTypeAliasTest} so a cyclic fixture can never narrow that test's shared model.
 */
class AstToIRAdapterConditionKindsTest {

    private static final String CONDITIONS = """
            namespace seat8.conditions
            version "1.0.0"

            typeAlias Ranged: string(maxLength: 10)
                condition Bounded:
                    item exists

            type Left:
                l string (1..1)

            type Right:
                r string (1..1)

            choice Pick:
                Left
                Right

            type Conditioned:
                a string (0..1)
                b string (0..1)
                condition: one-of
                condition Chosen:
                    required choice a, b
                condition Named:
                    a exists
            """;

    /** Two aliases naming each other: the walk must END at the last link it walked, not loop. */
    private static final String CYCLE = """
            namespace seat8.cycle
            version "1.0.0"

            typeAlias Loop1: Loop2

            typeAlias Loop2: Loop1

            type Looped:
                v Loop1 (1..1)
            """;

    private final AstToIRAdapter adapter = new AstToIRAdapter();

    private static RModel linked(String source, String file) {
        RModel model = AstBuilder.buildFromString(source, file);
        RWorkspace.build(List.of(model));
        return model;
    }

    // ----------------------------------------------------------------------------- the condition kinds

    @Test
    void aDataTypesConditionKindsAreIndexParallelToItsNames() {
        IRTypeNode conditioned = node(linked(CONDITIONS, "seat8-conditions.rosetta"), "Conditioned");
        assertEquals(List.of(Optional.empty(), Optional.of("Chosen"), Optional.of("Named")),
                conditioned.conditionNames(), "the names in source order — an unnamed condition reads empty");
        assertEquals(List.of("OneOf", "Choice", "DataRule"), conditioned.conditionKinds(),
                "the expression ROOT decides the kind, and EVERY condition states one");
    }

    @Test
    void aTypeAliasStatesTheKindOfItsOwnCondition() {
        IRTypeNode ranged = node(linked(CONDITIONS, "seat8-conditions-alias.rosetta"), "Ranged");
        assertEquals(IRKind.TYPE_ALIAS, ranged.kind());
        assertEquals(List.of(Optional.of("Bounded")), ranged.conditionNames());
        assertEquals(List.of("DataRule"), ranged.conditionKinds(),
                "an alias condition is neither one-of nor choice — the type-format validator wires it");
    }

    /**
     * A {@code choice} declares NO condition: the grammar gives {@code rosettaChoice} options and annotations only
     * ({@code RChoice} carries no {@code conditions()} accessor at all). So a choice node states an EMPTY kinds
     * list beside its empty names list — the absence is the fact, not an omission.
     */
    @Test
    void aChoiceDeclaresNoConditionSoItStatesNoKind() {
        IRTypeNode pick = node(linked(CONDITIONS, "seat8-conditions-choice.rosetta"), "Pick");
        assertEquals(IRKind.CHOICE, pick.kind());
        assertEquals(List.of(), pick.conditionNames());
        assertEquals(List.of(), pick.conditionKinds());
    }

    // -------------------------------------------------------------------------------- the records' laws

    @Test
    void aKindsListOfTheWrongLengthIsRefusedOnBothCarriers() {
        assertEquals("type seat8.x.T states 2 condition kinds for 1 conditions"
                        + " - the kinds are index-parallel to the names, or absent",
                assertThrows(IllegalArgumentException.class,
                        () -> declaration(List.of(Optional.of("One")), List.of("DataRule", "OneOf")))
                        .getMessage());

        assertEquals("alias link 'A' states 2 condition kinds for 1 conditions"
                        + " - the kinds are index-parallel to the names, or absent",
                assertThrows(IllegalArgumentException.class,
                        () -> new IRAliasLink("A", Optional.of("seat8.x"), List.of(),
                                List.of(Optional.of("One")), List.of("DataRule", "OneOf"))).getMessage());
    }

    @Test
    void aTokenOutsideTheVocabularyIsRefusedOnBothCarriers() {
        assertEquals("type seat8.x.T states the condition kind 'Rule'"
                        + " - the vocabulary is [OneOf, Choice, DataRule]",
                assertThrows(IllegalArgumentException.class,
                        () -> declaration(List.of(Optional.empty()), List.of("Rule"))).getMessage());

        assertEquals("alias link 'A' states the condition kind 'Rule'"
                        + " - the vocabulary is [OneOf, Choice, DataRule]",
                assertThrows(IllegalArgumentException.class,
                        () -> new IRAliasLink("A", Optional.of("seat8.x"), List.of(),
                                List.of(Optional.empty()), List.of("Rule"))).getMessage());
    }

    @Test
    void anAliasChainIsRefusedOnAReferenceThatIsNoTypeAlias() {
        IRAliasLink link = new IRAliasLink("A", Optional.of("seat8.x"), List.of(), List.of(), List.of());
        assertEquals("type Far of kind STRUCT carries an alias chain"
                        + " - only a TYPE_ALIAS reference walks one",
                assertThrows(IllegalArgumentException.class,
                        () -> IRTypeNode.reference("Far", IRKind.STRUCT, Optional.of("seat8.x"),
                                Optional.of("Far"), Optional.empty(), List.of(link))).getMessage());
    }

    // ------------------------------------------------------------------------------------ the cycle

    @Test
    void aCyclicAliasPairEndsTheChainAtTheLastLinkWalkedAndCollapsesToNothing() {
        RModel model = linked(CYCLE, "seat8-cycle.rosetta");
        IRType reference = fieldType(node(model, "Looped"), "v");
        assertEquals(IRKind.TYPE_ALIAS, reference.kind());
        assertEquals(List.of("Loop1", "Loop2"), reference.aliasChain().stream().map(IRAliasLink::name).toList(),
                "each alias is walked ONCE — the second sight of Loop1 ends the chain");
        assertEquals(Optional.empty(), reference.effectiveBase(),
                "the chain has no leaf, so the collapse failed — the absence is the fact");
    }

    // ---------------------------------------------------------------------------------------- the helpers

    private IRTypeNode node(RModel model, String name) {
        RRootElement element = model.rootElements().stream()
                .filter(e -> (e instanceof RDataType d && d.name().equals(name))
                        || (e instanceof RChoice c && c.name().equals(name))
                        || (e instanceof RTypeAlias a && a.name().equals(name)))
                .findFirst().orElseThrow(() -> new AssertionError("no declaration named " + name));
        IRNode adapted = adapter.adaptRootElement(model.namespace(), element).orElseThrow();
        return assertInstanceOf(IRTypeNode.class, adapted);
    }

    private static IRType fieldType(IRTypeNode type, String fieldName) {
        IRField field = type.fields().stream().filter(f -> f.name().equals(fieldName)).findFirst().orElseThrow();
        return field.type();
    }

    /** A STRUCT declaration stating {@code names} and {@code kinds} — the record's own law, corpus-free. */
    private static IRTypeNode declaration(List<Optional<String>> names, List<String> kinds) {
        return new IRTypeNode("seat8.x.T", IRKind.STRUCT, List.of(), Optional.empty(), false, Optional.empty(),
                IRMetadata.EMPTY, Optional.of("seat8.x"), Optional.empty(), Optional.empty(), List.of(), List.of(),
                names, Optional.empty(), List.of(), List.of(), kinds, List.of());
    }
}
