package com.regnosys.rosetta.ir.print;

import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IREnumValueNode;
import com.regnosys.rosetta.ir.adapter.IRFieldNode;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IRAnnotationPath;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRBounds;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IREnumSynonym;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRLabel;
import com.regnosys.rosetta.ir.core.IRRuleReference;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import com.regnosys.rosetta.testutil.IRSamples;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The declaration facts of the declaration-IR enrichment (decision D55) in the MLIR-style print:
 * each fact is printed as an indented detail line under its node's header, and each ONLY when it is
 * non-default. The four content tests pin the whole rendering fact-for-fact; the last test is the
 * backward-compatibility law — a node built through a pre-enrichment (old-arity) constructor prints
 * byte-for-byte the string {@code IRPrinterDeclarationTest} pinned before the enrichment.
 */
class IRPrinterDeclarationFactsTest {

    private final IRPrinter printer = new IRPrinter();

    @Test
    void printsEveryTypeFact() {
        IRTypeNode type = new IRTypeNode("test.model.T", IRKind.STRUCT, List.of(),
                Optional.of(IRTypeNode.reference("Base", IRKind.CHOICE,
                        Optional.of("test.model"), Optional.of("BaseChoice"))),
                false, Optional.empty(), IRMetadata.EMPTY,
                Optional.of("test.model"), Optional.of("T"), Optional.of("what the type is"),
                List.of(IRSamples.fullDocReference()), annotations(),
                List.of(Optional.of("Named"), Optional.empty()));

        assertEquals("""
                STRUCT test.model.T : Base [abstract=false]
                  namespace "test.model"
                  resolvedName "T"
                  baseTypeRef CHOICE ns="test.model" name="BaseChoice"
                  definition "what the type is"
                  docReference [regulatory] for item -> leg ->> payout
                    body "ESMA"
                    corpus "MiFIR" -> Regulation "MiFIR" displayName "MiFIR (600/2014)" definition "the corpus definition"
                    corpus "Unresolved"
                    segment "article" = "26"
                    segment "paragraph" = "1"
                    rationale "the rationale" author "the author"
                    rationale "a second rationale"
                    structuredProvision "a structured provision"
                    provision "a provision"
                    reportedField
                    arg "note" = "a named argument"
                  annotation metadata:scheme
                  annotation deprecated [reason = "old", replacement -> "other"]
                  condition "Named"
                  condition <unnamed>
                """, printer.print(type));
    }

    @Test
    void printsEveryFieldFact() {
        IRFieldNode field = new IRFieldNode("f",
                IRTypeNode.reference("Direction", IRKind.ENUM,
                        Optional.of("test.model"), Optional.of("DirectionEnum")),
                Cardinality.ONE_TO_ONE, Optional.empty(), IRMetadata.EMPTY,
                Optional.of(new IRBounds(BigInteger.ONE, Optional.of(BigInteger.TWO))), true,
                List.of(new IRTypeArgument("maxLength", Optional.empty(), Optional.of("35"), false),
                        new IRTypeArgument("min", Optional.empty(), Optional.of("1.5"), true),
                        new IRTypeArgument("digits", Optional.of("n"), Optional.empty(), false)),
                Optional.of("what the field is"), List.of(minimalDocReference()), annotations(),
                List.of(new IRLabel("for-scoped", Optional.of(path()), Optional.empty()),
                        new IRLabel("as-scoped", Optional.empty(), Optional.of(path()))),
                List.of(new IRRuleReference(Optional.of(path()), Optional.of("SomeRule"),
                                Optional.of("test.model"), Optional.of("SomeRule")),
                        new IRRuleReference(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty())));

        assertEquals("""
                FIELD f : Direction [ONE_TO_ONE]
                  typeRef ENUM ns="test.model" name="DirectionEnum"
                  bounds (1..2)
                  override
                  typeArg maxLength = "35"
                  typeArg min = -"1.5"
                  typeArg digits = n
                  definition "what the field is"
                  docReference
                    body "ESMA"
                  annotation metadata:scheme
                  annotation deprecated [reason = "old", replacement -> "other"]
                  label "for-scoped" for item -> leg ->> payout
                  label "as-scoped" as item -> leg ->> payout
                  ruleReference "SomeRule" for item -> leg ->> payout -> ns="test.model" name="SomeRule"
                  ruleReference empty
                """, printer.print(field));
    }

    @Test
    void printsEveryEnumFact() {
        IREnumNode enumeration = new IREnumNode("test.model.E", List.<IREnumValue>of(),
                Optional.empty(), IRMetadata.EMPTY, Optional.of("test.model"),
                Optional.of(IRTypeNode.reference("BaseEnum", IRKind.ENUM,
                        Optional.of("test.model"), Optional.of("BaseEnum"))),
                Optional.of("what the enum is"), List.of(minimalDocReference()), annotations());

        assertEquals("""
                ENUM test.model.E
                  namespace "test.model"
                  parent BaseEnum ns="test.model" name="BaseEnum"
                  definition "what the enum is"
                  docReference
                    body "ESMA"
                  annotation metadata:scheme
                  annotation deprecated [reason = "old", replacement -> "other"]
                """, printer.print(enumeration));
    }

    @Test
    void printsEveryEnumValueFact() {
        IREnumValueNode value = new IREnumValueNode("V", Optional.empty(), Optional.empty(),
                IRMetadata.EMPTY, Optional.of("what the value is"),
                List.of(minimalDocReference()), annotations(),
                List.of(new IREnumSynonym(List.of("FpML", "ISO"), "EXTERNAL",
                        Optional.of("the synonym definition"), Optional.of("a(.*)"),
                        Optional.of("b$1"), true)));

        assertEquals("""
                VALUE V
                  definition "what the value is"
                  docReference
                    body "ESMA"
                  annotation metadata:scheme
                  annotation deprecated [reason = "old", replacement -> "other"]
                  synonym "EXTERNAL" sources [FpML, ISO] definition "the synonym definition" pattern "a(.*)" -> "b$1" removeHtml
                """, printer.print(value));
    }

    // ── the type gate (v3.3 seat 7, PR #643) ───────────────────────────────────────────────────

    /**
     * A {@code typeAlias} DECLARATION, fact for fact: the header carries the body's written name, then
     * the namespace, one {@code param} line per declared parameter (its own reference line and its
     * documentation indented under it, each only when stated), the body's reference line with the
     * COLLAPSED chain after the {@code =>} arrow, the arguments the body wrote, then the documentation,
     * the doc references, the annotations and the condition names.
     *
     * <p>Able to fail: move or drop a line in {@code IRPrinter.printTypeAlias} — the string comparison
     * fails.
     */
    @Test
    void printsEveryTypeAliasFact() {
        IRTypeNode alias = new IRTypeNode("test.model.Max3Int", IRKind.TYPE_ALIAS, List.of(),
                Optional.of(IRTypeNode.reference("int", IRKind.TYPE_ALIAS, Optional.of("test.model"),
                        Optional.of("int"),
                        Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "number", Optional.empty(),
                                List.of(new IRTypeArgument("digits", Optional.empty(),
                                                Optional.of("3"), false),
                                        new IRTypeArgument("min", Optional.empty(),
                                                Optional.of("1.5"), true)))))),
                false, Optional.empty(), IRMetadata.EMPTY,
                Optional.of("test.model"), Optional.empty(), Optional.of("at most three digits"),
                List.of(minimalDocReference()), annotations(),
                List.of(Optional.of("Positive"), Optional.empty()),
                Optional.empty(),
                List.of(new IRTypeParameter("digits",
                                IRTypeNode.reference("int", IRKind.BASIC_TYPE, Optional.empty(),
                                        Optional.of("int")),
                                Optional.of("the digit count"), List.of()),
                        new IRTypeParameter("min", oldArityRef("number"), Optional.empty(),
                                List.of(new IRTypeArgument("fractionalDigits", Optional.empty(), Optional.of("0"), false),
                                        new IRTypeArgument("max", Optional.empty(), Optional.of("9"), true)))),
                List.of(new IRTypeArgument("digits", Optional.of("digits"), Optional.empty(), false),
                        new IRTypeArgument("fractionalDigits", Optional.empty(),
                                Optional.of("0"), false)));

        assertEquals("""
                TYPE_ALIAS test.model.Max3Int : int
                  namespace "test.model"
                  param digits : int
                    typeRef BASIC_TYPE name="int"
                    definition "the digit count"
                  param min : number
                    typeArg fractionalDigits = "0"
                    typeArg max = -"9"
                  baseTypeRef TYPE_ALIAS ns="test.model" name="int" => BASIC_TYPE number[digits="3", min=-"1.5"]
                  typeArg digits = digits
                  typeArg fractionalDigits = "0"
                  definition "at most three digits"
                  docReference
                    body "ESMA"
                  annotation metadata:scheme
                  annotation deprecated [reason = "old", replacement -> "other"]
                  condition "Positive"
                  condition <unnamed>
                """, printer.print(alias));
    }

    /**
     * A field whose declared type resolves to an alias: the collapsed chain rides the {@code typeRef}
     * line after a {@code =>} arrow. A BUILTIN leaf prints its bare name with its arguments; a DECLARED
     * leaf prints {@code namespace.name} and, with no argument left after substitution, no bracket at
     * all.
     *
     * <p>Able to fail: drop the {@code effectiveBase()} arm of {@code IRPrinter.printReference}.
     */
    @Test
    void printsTheCollapsedChainOnAFieldsTypeReference() {
        IRFieldNode builtinLeaf = new IRFieldNode("rate",
                IRTypeNode.reference("Max3Int", IRKind.TYPE_ALIAS, Optional.of("test.model"),
                        Optional.of("Max3Int"),
                        Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "number", Optional.empty(),
                                List.of(new IRTypeArgument("digits", Optional.empty(),
                                        Optional.of("3"), false))))),
                Cardinality.ONE_TO_ONE, Optional.empty(), IRMetadata.EMPTY);
        assertEquals("""
                FIELD rate : Max3Int [ONE_TO_ONE]
                  typeRef TYPE_ALIAS ns="test.model" name="Max3Int" => BASIC_TYPE number[digits="3"]
                """, printer.print(builtinLeaf));

        IRFieldNode declaredLeaf = new IRFieldNode("direction",
                IRTypeNode.reference("Direction", IRKind.TYPE_ALIAS, Optional.of("test.model"),
                        Optional.of("DirectionAlias"),
                        Optional.of(new IREffectiveBase(IRKind.ENUM, "DirectionEnum",
                                Optional.of("test.model"), List.of()))),
                Cardinality.ONE_TO_ONE, Optional.empty(), IRMetadata.EMPTY);
        assertEquals("""
                FIELD direction : Direction [ONE_TO_ONE]
                  typeRef TYPE_ALIAS ns="test.model" name="DirectionAlias" => ENUM test.model.DirectionEnum
                """, printer.print(declaredLeaf));
    }

    /**
     * The backward-compatibility law: the two pre-enrichment expectations of
     * {@code IRPrinterDeclarationTest}, asserted again here against old-arity nodes, so a future
     * change that starts printing a fact unconditionally fails HERE and not only over there.
     */
    @Test
    void oldArityNodesPrintExactlyWhatTheyPrintedBeforeTheEnrichment() {
        IRTypeNode foo = new IRTypeNode("test.model.Foo", IRKind.STRUCT,
                List.of(new IRFieldNode("bar", oldArityRef("string"), Cardinality.ONE_TO_ONE,
                                Optional.empty(), IRMetadata.EMPTY),
                        new IRFieldNode("baz", oldArityRef("number"), Cardinality.ZERO_TO_MANY,
                                Optional.empty(), IRMetadata.EMPTY)),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY);
        assertEquals("""
                STRUCT test.model.Foo [abstract=false]
                  FIELD bar : string [ONE_TO_ONE]
                  FIELD baz : number [ZERO_TO_MANY]
                """, printer.print(foo));

        IREnumValue up = new IREnumValueNode("Up", Optional.empty(),
                Optional.empty(), IRMetadata.EMPTY);
        IREnumValue down = new IREnumValueNode("Down", Optional.of("DOWN"),
                Optional.empty(), IRMetadata.EMPTY);
        IREnumNode direction = new IREnumNode("test.model.DirectionEnum", List.of(up, down),
                Optional.empty(), IRMetadata.EMPTY);
        assertEquals("""
                ENUM test.model.DirectionEnum
                  VALUE Up
                  VALUE Down displayName "DOWN"
                """, printer.print(direction));
    }

    // ── the property gate (v3.3 seat 8, PR #644) ───────────────────────────────────────────────

    /**
     * THE CONDITION KINDS, on all three type shapes that print conditions: a STRUCT and a CHOICE through
     * {@code printType}, a {@code typeAlias} declaration through {@code printTypeAlias}. The kind token
     * follows the name after a {@code :}, and an UNNAMED condition still shows its kind — which is the point
     * of the fact: the old generator names an unnamed condition's class from exactly that token.
     *
     * <p>Able to fail: drop the kind from {@code IRPrinter.conditionDisplay}, or have one call site keep its
     * own condition loop — each expectation is a whole-output string comparison.
     */
    @Test
    void printsTheConditionKindBesideEachConditionName() {
        IRTypeNode struct = new IRTypeNode("test.model.T", IRKind.STRUCT, List.of(),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY,
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(),
                List.of(Optional.of("Named"), Optional.empty()),
                Optional.empty(), List.of(), List.of(),
                List.of("DataRule", "OneOf"), List.of());
        assertEquals("""
                STRUCT test.model.T [abstract=false]
                  condition "Named" : DataRule
                  condition <unnamed> : OneOf
                """, printer.print(struct));

        IRTypeNode choice = new IRTypeNode("test.model.C", IRKind.CHOICE, List.of(),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY,
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(),
                List.of(Optional.<String>empty()),
                Optional.empty(), List.of(), List.of(),
                List.of("Choice"), List.of());
        assertEquals("""
                CHOICE test.model.C [abstract=false]
                  condition <unnamed> : Choice
                """, printer.print(choice));

        IRTypeNode alias = new IRTypeNode("test.model.A", IRKind.TYPE_ALIAS, List.of(),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY,
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(),
                List.of(Optional.of("Positive")),
                Optional.empty(), List.of(), List.of(),
                List.of("DataRule"), List.of());
        assertEquals("""
                TYPE_ALIAS test.model.A
                  condition "Positive" : DataRule
                """, printer.print(alias));
    }

    /**
     * The non-default-only law for the new fact: a node whose producer stated NO kinds prints its condition
     * lines exactly as it printed them before the property gate — the kinds are index-parallel to the names
     * or absent altogether, never half-stated.
     *
     * <p>Able to fail: print an empty kind suffix ({@code condition "Named" : }) when the list is empty.
     */
    @Test
    void printsNoConditionKindWhenTheProducerStatedNone() {
        IRTypeNode struct = new IRTypeNode("test.model.T", IRKind.STRUCT, List.of(),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY,
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(),
                List.of(Optional.of("Named"), Optional.empty()));
        assertEquals("""
                STRUCT test.model.T [abstract=false]
                  condition "Named"
                  condition <unnamed>
                """, printer.print(struct));
    }

    /**
     * THE ALIAS CHAIN under a field's type reference: one {@code alias} rung line per link, OUTERMOST-FIRST,
     * indented under the reference line that already carries the leaf the chain collapses to. The first rung
     * is PARAMETERISED and carries two conditions with their kinds (the shape that refuses a whole
     * type-format validator); the second is plain, so it prints its name alone.
     *
     * <p>Able to fail: drop the {@code aliasChain()} loop from {@code IRPrinter.printReference}, reverse the
     * rung order, or drop a rung's parameters or conditions.
     */
    @Test
    void printsEachRungOfTheAliasChainUnderTheReference() {
        IRFieldNode field = new IRFieldNode("rate",
                IRTypeNode.reference("Max3Int", IRKind.TYPE_ALIAS, Optional.of("test.model"),
                        Optional.of("Max3Int"),
                        Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "number", Optional.empty(),
                                List.of(new IRTypeArgument("digits", Optional.empty(),
                                        Optional.of("3"), false)))),
                        List.of(new IRAliasLink("Max3Int", Optional.of("test.model"), List.of("digits"),
                                        List.of(Optional.of("Positive"), Optional.empty()),
                                        List.of("DataRule", "OneOf")),
                                new IRAliasLink("int", Optional.of("test.model"), List.of(), List.of(),
                                        List.of()))),
                Cardinality.ONE_TO_ONE, Optional.empty(), IRMetadata.EMPTY);

        assertEquals("""
                FIELD rate : Max3Int [ONE_TO_ONE]
                  typeRef TYPE_ALIAS ns="test.model" name="Max3Int" => BASIC_TYPE number[digits="3"]
                    alias "test.model.Max3Int" params [digits] conditions ["Positive" : DataRule, <unnamed> : OneOf]
                    alias "test.model.int"
                """, printer.print(field));
    }

    /**
     * THE MODEL NODE, fact for fact, off the shared {@link IRSamples#model()} sample: the documentation, the
     * version, both qualifiable roots with their root references, the qualification function with its first
     * input's reference, the function signature whose input and output print as the FIELD lines they are
     * (each with its {@code [metadata …]} annotation — the wrapper source), and the three {@code with-meta}
     * uses: one over a declared type, one over a CONSTRAINED builtin with its literal arguments, and one
     * REFUSED, whose refusal is stated on the line.
     *
     * <p>Able to fail: drop or reorder any line of {@code IRPrinter.printModel} — the whole output is
     * compared as one string.
     */
    @Test
    void printsEveryModelFact() {
        assertEquals("""
                MODEL test.model
                  definition "what the model is"
                  version "1.2.3"
                  isProduct root Product
                    rootTypeRef STRUCT ns="test.model" name="Product"
                  isEvent root Event
                    rootTypeRef STRUCT ns="test.model" name="Event"
                  qualification Qualify_Product : Product
                    firstInputTypeRef STRUCT ns="test.model" name="Product"
                  function DoIt
                    input
                      FIELD in : Foo [ONE_TO_ONE]
                        typeRef STRUCT ns="test.model" name="Foo"
                        annotation metadata:scheme
                    output
                      FIELD result : Bar [ONE_TO_ONE]
                        typeRef STRUCT ns="test.model" name="Bar"
                        annotation metadata:reference
                  with-meta [scheme] : Foo
                    argumentTypeRef STRUCT ns="test.model" name="Foo"
                  with-meta [reference, id] : number
                    argumentTypeRef BASIC_TYPE name="number"
                    typeArg fractionalDigits = "2"
                    typeArg min = "0"
                  with-meta [scheme] refused "nothing"
                """, printer.print(IRSamples.model()));
    }

    /**
     * The non-default-only law on the model node: a model that states nothing prints its header alone, and a
     * model whose only fact is a REFUSED {@code with-meta} use prints that one line — the refusal is a fact,
     * so it is never the silence the old generator leaves.
     *
     * <p>Able to fail: print an empty {@code definition} / {@code version} line, or skip a refused use.
     */
    @Test
    void printsOnlyTheFactsAModelStates() {
        assertEquals("MODEL test.model\n", printer.print(new IRModelNode("test.model")));

        IRModelNode refusedOnly = new IRModelNode("test.model", Optional.empty(), Optional.empty(),
                List.of(), List.of(), List.of(),
                List.of(new IRWithMetaUse(List.of("scheme"), Optional.empty(), List.of(),
                        Optional.of("nothing"))),
                Optional.empty(), IRMetadata.EMPTY);
        assertEquals("""
                MODEL test.model
                  with-meta [scheme] refused "nothing"
                """, printer.print(refusedOnly));
    }

    // ── fixtures ───────────────────────────────────────────────────────────────────────────────

    /** One annotation with a bare qualifier, one with {@code key = value} arguments. */
    private static List<IRAnnotationUse> annotations() {
        return List.of(
                new IRAnnotationUse("metadata", Optional.of("scheme")),
                new IRAnnotationUse("deprecated", Optional.empty(),
                        List.of(new IRAnnotationUse.Argument("reason", "old", false),
                                new IRAnnotationUse.Argument("replacement", "other", true))));
    }

    /** A doc reference carrying its body alone — enough to prove the member is printed. */
    private static IRDocReference minimalDocReference() {
        return new IRDocReference(false, Optional.empty(), Optional.of("ESMA"), List.of(), List.of(),
                List.of(), Optional.empty(), Optional.empty(), false, List.of());
    }

    /** {@code item -> leg ->> payout} — the {@code item} keyword root with a deep step. */
    private static IRAnnotationPath path() {
        return new IRAnnotationPath(true, "", List.of(
                new IRAnnotationPath.Step("leg", false),
                new IRAnnotationPath.Step("payout", true)));
    }

    /** The pre-enrichment name-only type reference. */
    private static IRType oldArityRef(String name) {
        return new IRTypeNode(name, IRKind.STRUCT, List.of(), Optional.empty(), false,
                Optional.empty(), IRMetadata.EMPTY);
    }
}
