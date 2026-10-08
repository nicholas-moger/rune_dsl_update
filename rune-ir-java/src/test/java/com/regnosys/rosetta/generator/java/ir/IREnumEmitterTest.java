package com.regnosys.rosetta.generator.java.ir;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IREnumValueNode;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 6 (PR #642, ruling R3 stage 1) - THE ENUM EMITTER WITNESSED against the old generator on a fixture that
 * carries every arm of the byte contract, and its refusals. The corpus proof is the rings (every vendored cell
 * byte-identical on both routes); this class is the corpus-free proof that the emitter renders EVERY arm the map
 * names ({@code target/v33-seat6-instruments/scratch/enum-emitter-map.md}) and refuses what it must, and the
 * mutation lanes ({@code lanes-s6c3.sh}) drop each arm in turn and read the ring RED on a named file.
 *
 * <p>Each witness says what mutation makes it red: {@link #byteIdenticalToTheOldGeneratorOnEveryArm} - any arm of
 * the emitter dropped or re-spelt (the parent flattening, the synonym import, the corpus line, the segments, the
 * provision, the display-name escape, the version stamp); {@link #aParentOutsideTheGeneratedSetIsFlattenedThroughTheIndex}
 * - the index built over the generated models alone; {@link #anAbsentParentIsARefusalNeverAFlatEnum} /
 * {@link #anUnresolvedParentReferenceIsARefusal} - a silent flat enum on a missing parent;
 * {@link #theCaretIsStrippedAtBothSeats} - the strip dropped at either seat; {@link #aMismatchedDeclarationIsRefusedAndWritesNoFile}
 * - a file written from an IR that disagrees with its source.
 */
class IREnumEmitterTest {

    private static final String OTHER = """
            namespace seat6.other
            version "2.0.0"

            enum Root: <"The root enum.">
                Alpha displayName "alpha" <"The first root value.">
                Beta

            enum Middle extends Root:
                Gamma displayName "gamma \\"quoted\\" \\\\ tab\\t µ"
            """;

    private static final String FACTS = """
            namespace seat6.facts
            version "1.0.0"

            import seat6.other.*

            body Organisation Org1
            corpus Agreement Org1 "Agreement <1> & more" Agr1
            segment name
            segment article

            enum Plain:
                One
                Two

            enum Documented: <"A documented enum & <escaped>.">
                [docReference Org1 Agr1 name "the name" article "art <1>" provision "The provision."]
                [docReference Org1 NoSuchCorpus name "unresolved"]
                One displayName "one" <"The first.">
                    [docReference Org1 Agr1]
                    [synonym FpML value "ONE" definition "the one" pattern "O.*" "O" removeHtml]
                    [synonym FpML, ISDA value "UNO"]
                Two <"">
                    [deprecated]
                Rate3Month
                    [docReference Org1 Agr1 name "three"]
                Camel_CaseName

            enum Leaf extends Middle:
                Delta
            """;

    private static final String NO_VERSION = """
            namespace seat6.noversion

            enum Bare:
                X
            """;

    private record Fixture(RModel other, RModel facts, RModel noVersion, RWorkspace workspace) {
    }

    private static Fixture fixture() {
        RModel other = AstBuilder.buildFromString(OTHER, "seat6-other.rosetta");
        RModel facts = AstBuilder.buildFromString(FACTS, "seat6-facts.rosetta");
        RModel noVersion = AstBuilder.buildFromString(NO_VERSION, "seat6-noversion.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(other, facts, noVersion)).workspace();
        return new Fixture(other, facts, noVersion, ws);
    }

    private static Map<String, String> oldRoute(GeneratorModel gm, RModel... models) {
        EnumGenerator old = new EnumGenerator(gm);
        Map<String, String> out = new LinkedHashMap<>();
        for (RModel model : models) {
            List<GenerationException> errors = old.generateClasses(model, gm.version(model), out);
            assertTrue(errors.isEmpty(), "the old generator refused the fixture: " + errors);
        }
        return out;
    }

    private static Map<String, String> irRoute(IREnumGenerator ir, GeneratorModel gm, RModel... models) {
        Map<String, String> out = new LinkedHashMap<>();
        for (RModel model : models) {
            List<GenerationException> errors = ir.generateClassesAsIR(model, gm.version(model), out);
            assertTrue(errors.isEmpty(), "the IR route refused the fixture: " + errors);
        }
        return out;
    }

    private static void assertSameFiles(Map<String, String> expected, Map<String, String> actual) {
        assertEquals(expected.keySet(), actual.keySet(), "the output keys");
        for (Map.Entry<String, String> e : expected.entrySet()) {
            assertEquals(e.getValue(), actual.get(e.getKey()), "the bytes of " + e.getKey());
        }
    }

    // ------------------------------------------------------------------------------------------- the byte contract

    @Test
    void byteIdenticalToTheOldGeneratorOnEveryArm() {
        Fixture f = fixture();
        GeneratorModel gm = new GeneratorModel(f.workspace(), m -> true);
        Map<String, String> expected = oldRoute(gm, f.other(), f.facts(), f.noVersion());
        IREnumGenerator ir = new IREnumGenerator(gm);
        Map<String, String> actual = irRoute(ir, gm, f.other(), f.facts(), f.noVersion());

        assertEquals(6, expected.size(), "the fixture's six enums: " + expected.keySet());
        assertSameFiles(expected, actual);
        assertEquals(actual.keySet(), ir.filesWrittenByIrEmitter(), "every file booked to the emitter");
        int[] stats = ir.declarationReconcileStats();
        assertEquals(6, stats[0], "declarations attempted");
        assertEquals(0, stats[2], "mismatches");

        // the arms, each visible on the bytes the two routes agree on - a witness that the fixture exercises them
        String documented = actual.get("seat6/facts/Documented.java");
        assertTrue(documented.contains("import com.rosetta.model.lib.annotations.RosettaSynonym;"), "the synonym import");
        assertTrue(documented.contains(" * A documented enum &amp; &lt;escaped&gt;."), "the HTML-escaped definition");
        assertTrue(documented.contains(" * @version 1.0.0"), "the version stamp");
        assertTrue(documented.contains(" * Corpus Agreement Agr1 Agreement &lt;1&gt; &amp; more  "), "the RESOLVED corpus line");
        assertTrue(documented.contains(" * Corpus NoSuchCorpus   "), "the UNRESOLVED corpus arm (the reference and three spaces)");
        assertTrue(documented.contains(" * name \"the name\" * article \"art &lt;1&gt;\""), "the segments on one line");
        assertTrue(documented.contains(" * Provision The provision."), "the provision");
        assertTrue(documented.contains(" * Provision \n"), "the empty provision line");
        assertTrue(documented.contains("\t@RosettaSynonym(value = \"ONE\", source = \"FpML\")"), "a synonym per (value, source)");
        assertTrue(documented.contains("\t@RosettaSynonym(value = \"UNO\", source = \"ISDA\")"), "the second source");
        assertTrue(documented.contains("\tRATE_3_MONTH(\"Rate3Month\", null)"), "the Java name mangling at a digit");
        assertTrue(documented.contains("\tCAMEL_CASE_NAME(\"Camel_CaseName\", null)"), "the underscore and the camel hump mangled");
        assertTrue(documented.contains("\t/**\n\t */\n\t@RosettaEnumValue(value = \"Two\")"), "the explicitly empty definition's block");

        String leaf = actual.get("seat6/facts/Leaf.java");
        int alpha = leaf.indexOf("ALPHA(\"Alpha\", \"alpha\")");
        int gamma = leaf.indexOf("GAMMA(\"Gamma\", \"gamma \\\"quoted\\\" \\\\ tab\\t \\u00B5\")");
        int delta = leaf.indexOf("DELTA(\"Delta\", null)");
        assertTrue(alpha >= 0 && gamma > alpha && delta > gamma, "the parent chain's values first, the escaped constructor literal: " + leaf);
        assertTrue(leaf.contains("displayName = \"gamma \"quoted\" \\ tab\t µ\")"), "the display name RAW in the annotation (D46 F1)");
        assertTrue(leaf.contains(" * @version 1.0.0"), "the CHILD's version, not the parent's");

        String bare = actual.get("seat6/noversion/Bare.java");
        assertTrue(bare.contains(" * @version 0.0.0"), "the undeclared version stamp");
        assertFalse(actual.get("seat6/facts/Plain.java").contains("RosettaSynonym"), "no synonym import without a synonym");
    }

    @Test
    void aParentOutsideTheGeneratedSetIsFlattenedThroughTheIndex() {
        Fixture f = fixture();
        // the cell generates seat6.facts alone; Leaf's parent chain (Middle, Root) lives in seat6.other, outside the filter
        GeneratorModel gm = new GeneratorModel(f.workspace(), m -> m.namespace().equals("seat6.facts"));
        Map<String, String> expected = oldRoute(gm, f.facts());
        IREnumGenerator ir = new IREnumGenerator(gm);
        Map<String, String> actual = irRoute(ir, gm, f.facts());
        assertSameFiles(expected, actual);
        assertTrue(actual.get("seat6/facts/Leaf.java").contains("ALPHA(\"Alpha\", \"alpha\")"), "the root's value flattened in");
        assertEquals(3, ir.declarationReconcileStats()[0], "the pass's population is the generated model's enums alone");
        int[] parents = ir.parentReconcileStats();
        assertEquals(2, parents[0], "the two parents reconciled once each on first resolution");
        assertEquals(0, parents[2], "the parents agree with their source");
    }

    // ------------------------------------------------------------------------------------------------ the refusals

    private static IREnumNode enumNode(String namespace, String name, Optional<IRTypeNode> parent, IREnumValueNode... values) {
        return new IREnumNode(namespace + "." + name, List.of(values), Optional.empty(), IRMetadata.EMPTY,
                Optional.of(namespace), parent.map(p -> (com.regnosys.rosetta.ir.core.IRType) p), Optional.empty(),
                List.of(), List.of());
    }

    private static IREnumValueNode value(String name) {
        return new IREnumValueNode(name, Optional.empty(), Optional.empty(), IRMetadata.EMPTY);
    }

    @Test
    void anAbsentParentIsARefusalNeverAFlatEnum() {
        IREnumEmitter emitter = new IREnumEmitter(qualified -> Optional.empty());   // an index that holds nothing
        IRTypeNode parent = IRTypeNode.reference("Base", IRKind.ENUM, Optional.of("seat6.other"), Optional.of("Base"));
        IREnumNode child = enumNode("seat6.facts", "Child", Optional.of(parent), value("Own"));
        GenerationException e = assertThrows(GenerationException.class, () -> emitter.emit(child, "1.0.0"));
        assertTrue(e.getMessage().contains("the parent seat6.other.Base of seat6.facts.Child is not in the workspace's enum index"), e.getMessage());
        assertEquals("seat6/facts/Child.java", e.getTargetPath(), "the refusal names the file it did not write");
        assertEquals(Set.of(), emitter.written(), "nothing booked");
    }

    @Test
    void anUnresolvedParentReferenceIsARefusal() {
        IREnumEmitter emitter = new IREnumEmitter(qualified -> Optional.empty());
        IRTypeNode unresolved = IRTypeNode.reference("Base", IRKind.ENUM, Optional.empty(), Optional.empty());
        IREnumNode child = enumNode("seat6.facts", "Child", Optional.of(unresolved), value("Own"));
        GenerationException e = assertThrows(GenerationException.class, () -> emitter.emit(child, "1.0.0"));
        assertTrue(e.getMessage().contains("the parent of seat6.facts.Child is unresolved in the IR (Base)"), e.getMessage());
    }

    @Test
    void aNameOutsideItsNamespaceIsRefused() {
        IREnumEmitter emitter = new IREnumEmitter(qualified -> Optional.empty());
        IREnumNode stray = new IREnumNode("elsewhere.Stray", List.of(value("A")), Optional.empty(), IRMetadata.EMPTY,
                Optional.of("seat6.facts"), Optional.empty(), Optional.empty(), List.of(), List.of());
        GenerationException e = assertThrows(GenerationException.class, () -> IREnumEmitter.outputKey(stray));
        assertTrue(e.getMessage().contains("does not start with its namespace seat6.facts"), e.getMessage());
    }

    @Test
    void theCaretIsStrippedAtBothSeats() {
        // the fork's lexer strips the parser-escape caret at the token, so no adapted node carries one; the strip is a belt
        IREnumEmitter emitter = new IREnumEmitter(qualified -> Optional.empty());
        IREnumNode node = enumNode("seat6.facts", "Escaped", Optional.empty(), value("^E"), value("Plain"));
        String text = emitter.emit(node, null);
        assertTrue(text.contains("\t@RosettaEnumValue(value = \"E\") \n\tE(\"E\", null)"), "the caret stripped at the annotation AND the constant: " + text);
        assertFalse(text.contains("^"), "no caret survives");
        assertEquals(Set.of("seat6/facts/Escaped.java"), emitter.written());
    }

    /**
     * v3.3 seat 7 (PR #643, the #642 round-2 SF-1): the PARENT arms of the refusal law witnessed - a parent whose
     * node disagrees with its source refuses EVERY child that extends it (the second child too: the refusal is
     * remembered, never a parent read clean by the next child), the parent's own mismatch is booked on the parent
     * reconciler (not the pass's population), and no child file is written. What makes it red: book the parent as
     * reconciled before its reconcile returns, or forget the refusal after the first child.
     */
    @Test
    void aMismatchedParentRefusesEveryChild() {
        Fixture f = fixture();
        GeneratorModel gm = new GeneratorModel(f.workspace(), m -> m.namespace().equals("seat6.facts"));
        IREnumGenerator ir = new IREnumGenerator(gm);
        // the parent Middle (seat6.other, outside the generated set) lies: its node loses its value
        REnumeration middle = f.other().rootElements().stream().filter(REnumeration.class::isInstance).map(REnumeration.class::cast)
                .filter(e -> e.name().equals("Middle")).findFirst().orElseThrow();
        IRTypeNode rootRef = IRTypeNode.reference("Root", IRKind.ENUM, Optional.of("seat6.other"), Optional.of("Root"));
        ir.indexForTests().plant(middle, enumNode("seat6.other", "Middle", Optional.of(rootRef)));
        Map<String, String> out = new LinkedHashMap<>();
        List<GenerationException> errors = ir.generateClassesAsIR(f.facts(), "1.0.0", out);
        assertEquals(0, ir.declarationReconcileStats()[2], "the pass's own declarations agree with their source");
        int[] parents = ir.parentReconcileStats();
        assertEquals(1, parents[0], "the lying parent reconciled once");
        assertEquals(1, parents[2], "its mismatch booked on the PARENT reconciler");
        assertFalse(out.containsKey("seat6/facts/Leaf.java"), "no file for the child of a lying parent");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("the parent seat6.other.Middle of seat6.facts.Leaf")
                || e.getMessage().contains("the parent seat6.other.Middle disagrees with its source")), errors.toString());
        assertTrue(out.containsKey("seat6/facts/Plain.java") && out.containsKey("seat6/facts/Documented.java"), "the parentless declarations are written");
        // a SECOND child of the same parent is refused too - the refusal is remembered, the parent reconciled once
        // (a fresh parse: a frozen model cannot join a second workspace)
        RModel other2 = AstBuilder.buildFromString(OTHER, "seat6-other.rosetta");
        RModel facts2 = AstBuilder.buildFromString(FACTS, "seat6-facts.rosetta");
        RModel second = AstBuilder.buildFromString("""
                namespace seat6.facts
                version "1.0.0"
                import seat6.other.*
                enum Leaf2 extends Middle:
                    Epsilon
                """, "seat6-second.rosetta");
        RWorkspace ws2 = RWorkspace.build(List.of(other2, facts2, second)).workspace();
        IREnumGenerator ir2 = new IREnumGenerator(new GeneratorModel(ws2, m -> m.namespace().equals("seat6.facts")));
        REnumeration middle2 = other2.rootElements().stream().filter(REnumeration.class::isInstance).map(REnumeration.class::cast)
                .filter(e -> e.name().equals("Middle")).findFirst().orElseThrow();
        ir2.indexForTests().plant(middle2, enumNode("seat6.other", "Middle", Optional.of(rootRef)));
        Map<String, String> out2 = new LinkedHashMap<>();
        List<GenerationException> e1 = ir2.generateClassesAsIR(facts2, "1.0.0", out2);
        List<GenerationException> e2 = ir2.generateClassesAsIR(second, "1.0.0", out2);
        assertFalse(e1.isEmpty() || e2.isEmpty(), "both children refused");
        assertFalse(out2.containsKey("seat6/facts/Leaf.java") || out2.containsKey("seat6/facts/Leaf2.java"), "neither child written");
        assertEquals(1, ir2.parentReconcileStats()[0], "the parent reconciled ONCE across the two passes");
        assertEquals(1, ir2.parentReconcileStats()[2], "one mismatch, remembered");
    }

    /**
     * The throw arm of the same law: a parent whose adapter or reconcile THROWS is booked as a mismatch on the parent
     * reconciler and refused for every child, the cause named. What makes it red: catch nothing around the parent
     * reconcile, or book the parent as reconciled before it runs.
     */
    @Test
    void aThrowingParentIsAMismatchAndRefusesItsChildren() {
        Fixture f = fixture();
        GeneratorModel gm = new GeneratorModel(f.workspace(), m -> m.namespace().equals("seat6.facts"));
        IREnumGenerator ir = new IREnumGenerator(gm);
        REnumeration middle = f.other().rootElements().stream().filter(REnumeration.class::isInstance).map(REnumeration.class::cast)
                .filter(e -> e.name().equals("Middle")).findFirst().orElseThrow();
        // a planted node whose values list carries a value that THROWS on read: the reconcile's comparison throws on it
        IREnumValue poison = new IREnumValue() {
            @Override public String name() { throw new IllegalStateException("LANE: the parent's node throws"); }
            @Override public Optional<String> displayName() { return Optional.empty(); }
            @Override public IRKind kind() { return IRKind.ENUM_VALUE; }
            @Override public Optional<com.regnosys.rosetta.ir.core.SourceRange> sourceRange() { return Optional.empty(); }
            @Override public List<? extends com.regnosys.rosetta.ir.core.IRNode> children() { return List.of(); }
            @Override public com.regnosys.rosetta.ir.core.Metadata metadata() { return IRMetadata.EMPTY; }
        };
        ir.indexForTests().plant(middle, new IREnumNode("seat6.other.Middle", List.of(poison), Optional.empty(), IRMetadata.EMPTY,
                Optional.of("seat6.other"), Optional.of(IRTypeNode.reference("Root", IRKind.ENUM, Optional.of("seat6.other"), Optional.of("Root"))),
                Optional.empty(), List.of(), List.of()));
        Map<String, String> out = new LinkedHashMap<>();
        List<GenerationException> errors = ir.generateClassesAsIR(f.facts(), "1.0.0", out);
        assertFalse(out.containsKey("seat6/facts/Leaf.java"), "no file for the child of a throwing parent");
        assertEquals(1, ir.parentReconcileStats()[2], "the throw is a mismatch on the parent reconciler");
        GenerationException refusal = errors.stream().filter(e -> e.getMessage().contains("could not be adapted or reconciled")).findFirst()
                .orElseThrow(() -> new AssertionError("the throw arm's refusal is not named: " + errors));
        assertTrue(refusal.getCause() instanceof IllegalStateException && refusal.getCause().getMessage().contains("LANE"), "the cause is carried");
    }

    @Test
    void aMismatchedDeclarationIsRefusedAndWritesNoFile() {
        Fixture f = fixture();
        GeneratorModel gm = new GeneratorModel(f.workspace(), m -> true);
        // the reconcile is driven by an IR index that LIES for one declaration: Plain's node loses a value
        IREnumGenerator ir = new IREnumGenerator(gm);
        REnumeration plain = f.facts().rootElements().stream().filter(REnumeration.class::isInstance).map(REnumeration.class::cast)
                .filter(e -> e.name().equals("Plain")).findFirst().orElseThrow();
        ir.indexForTests().plant(plain, enumNode("seat6.facts", "Plain", Optional.empty(), value("One")));
        Map<String, String> out = new LinkedHashMap<>();
        List<GenerationException> errors = ir.generateClassesAsIR(f.facts(), "1.0.0", out);
        assertFalse(errors.isEmpty(), "the mismatch is a generation error");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("ENUM seat6.facts.Plain: values.size")), errors.toString());
        assertFalse(out.containsKey("seat6/facts/Plain.java"), "no file is written from an IR that disagrees with its source");
        assertTrue(out.containsKey("seat6/facts/Documented.java") && out.containsKey("seat6/facts/Leaf.java"), "the agreeing declarations are written");
        assertFalse(ir.filesWrittenByIrEmitter().contains("seat6/facts/Plain.java"));
        assertEquals(1, ir.declarationReconcileStats()[2], "one mismatch booked");
    }
}
