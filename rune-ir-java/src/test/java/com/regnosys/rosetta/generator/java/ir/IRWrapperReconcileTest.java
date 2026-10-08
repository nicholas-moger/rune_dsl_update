package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 8 (PR #644) - THE WRAPPER SPEC SET, ITS SOURCE SETS AND ITS CONTENT, WITNESSED. The fixture puts ONE
 * wrapper behind each of the five collectors {@code MetaFieldGenerator.collectSpecs} draws from - a declaration
 * attribute, a function INPUT, a function OUTPUT, a choice OPTION and a {@code with-meta} EXPRESSION - with a key no
 * other collector produces, so each source is observable on its own; it puts ONE key behind TWO collectors (a
 * declaration attribute AND a function input, the cdm/5.38.0 shape that refuted the first-claimer cut), so the
 * SOURCE SET is observable as a set; and it puts an ENUM-valued wrapper beside a composite and a primitive one,
 * because {@code resolveValueCategory} tests the ENUM arm FIRST.
 */
class IRWrapperReconcileTest {

    private static final String WRAPPERS = """
            namespace seat8.wrappers
            version "1.0.0"

            enum Colour:
                RED
                GREEN

            type Party:
                id string (1..1)

            type Book:
                b string (1..1)

            type Holder:
                party Party (0..1)
                    [metadata reference]
                col Colour (0..1)
                    [metadata scheme]
                coded string (0..1)
                    [metadata scheme]

            choice Either:
                Party
                Book
                    [metadata location]

            func Measure:
                inputs:
                    h Holder (1..1)
                output:
                    amount number (0..1)
                        [metadata scheme]
                set amount: 1.0

            func Ident:
                inputs:
                    bk Book (1..1)
                        [metadata reference]
                    mark string (1..1)
                        [metadata id]
                output:
                    r string (0..1)
                set r: bk -> b

            func Pin:
                inputs:
                    h Holder (1..1)
                output:
                    p Party (0..1)
                set p: h -> party with-meta { id: "seat8-id" }
            """;

    // --------------------------------------------------------------------------------------------- the laws

    @Test
    void theWrapperSetReconcilesGreenAndItsMirrorEqualsTheGeneratorsOwnCollectSpecs() {
        Fixture f = fixture();
        assertEquals(List.of(), reconcile(f));
        assertEquals(f.reconciler().expected(), f.reconciler().stats()[0],
                "the IR half produced exactly the generator's own spec population");
        assertTrue(f.reconciler().stats()[1] > f.reconciler().stats()[0],
                "five facts per key plus the three set-level facts");
        assertEquals(0, f.reconciler().stats()[2]);
    }

    /** Every one of the FIVE collectors is present, so the source SETS are measured, not assumed. */
    @Test
    void allFiveCollectorsAreRepresentedInTheFixture() {
        Fixture f = fixture();
        assertEquals(List.of(), reconcile(f));
        assertTrue(f.reconciler().expected() >= 6,
                "a declaration attribute, an enum-valued attribute, a string attribute, a function input, a"
                        + " function output, a choice option and a with-meta expression: "
                        + f.reconciler().expected());
    }

    /**
     * THE LAW THE FIRST-CLAIMER CUT GOT WRONG (cdm/5.38.0, two keys): {@code String|com.rosetta.model|
     * FIELD_WITH_META} is produced by BOTH {@code Holder.coded [metadata scheme]} (a declaration attribute) AND
     * {@code Ident.mark [metadata id]} (a function input). Which of the two the generator's walk reaches first is a
     * walk-order fact that changes no byte; that BOTH produce it is the fact the emitter depends on, and it is what
     * {@code wrapper.<key>.sources} asserts. The wrapper file is one file either way.
     */
    @Test
    void aKeyTwoCollectorsProduceCarriesBothOfThemAndTheFirstClaimerIsNotAsserted() {
        Fixture f = fixture();
        RDataType holder = IRDerivedFactsReconcileTest.dataType(f.model(), "Holder");
        RAttribute coded = holder.attributes().stream().filter(a -> "coded".equals(a.name())).findFirst()
                .orElseThrow();
        RFunction ident = (RFunction) f.model().rootElements().stream()
                .filter(e -> e instanceof RFunction fn && "Ident".equals(fn.name())).findFirst().orElseThrow();
        RAttribute mark = ident.inputs().stream().filter(a -> "mark".equals(a.name())).findFirst().orElseThrow();
        assertEquals(MetaFieldGenerator.MetaKind.FIELD_WITH_META, MetaFieldGenerator.detectMetaKind(coded));
        assertEquals(MetaFieldGenerator.MetaKind.FIELD_WITH_META, MetaFieldGenerator.detectMetaKind(mark));
        assertEquals("java.lang.String",
                new JavaTypeTranslator(new JavaTypeUtil()).toJavaReferenceType(f.gm().getType(coded))
                        .getCanonicalName().withDots(),
                "a declaration attribute and a function input PRODUCE THE SAME KEY - which one the generator's"
                        + " walk reaches first is the fact that is no longer gated");

        assertEquals(List.of(), reconcile(f),
                "and both halves carry BOTH sources on that key, so the set is green");
    }

    /**
     * D08: the FUNCTION collectors dropped from the IR's scan. 412 of the 1,079 vendored wrappers (38.2%) are
     * function facts, so an emitter that read declarations alone would write 38% fewer wrapper files - and the key
     * that two collectors share loses a source without losing the key, which only the SET can catch.
     */
    @Test
    void d08DroppingTheFunctionCollectorsIsRedOnTheSourceSetsAndTheSize() {
        Fixture f = fixture();
        f.reconciler().lie(IRDerivedLie.WRAPPER_NO_FUNCTION_SOURCE);
        List<String> mismatches = reconcile(f);
        assertEquals(1, named(mismatches, "wrapper.specs.size").size(), String.join("\n", mismatches));
        assertEquals(1, named(mismatches, "wrapper.keys").size(), String.join("\n", mismatches));
        assertEquals(1, named(mismatches, "wrapper.Book|seat8.wrappers|REFERENCE_WITH_META.sources").size(),
                "the key only a function input produces is red: " + String.join("\n", mismatches));
        List<String> shared =
                named(mismatches, "wrapper.String|com.rosetta.model|FIELD_WITH_META.sources");
        assertEquals(1, shared.size(),
                "and the SHARED key keeps its key but loses a source - invisible to a first-claimer fact: "
                        + String.join("\n", mismatches));
        assertTrue(shared.get(0).contains("declarationAttribute,functionInput")
                        && shared.get(0).contains("the IR says declarationAttribute"), shared.get(0));
    }

    /**
     * The spec CONTENT is asserted against the generator's OWN {@link MetaFieldGenerator.MetaFieldSpec} - the four
     * fields the wrapper file is written from - not against the mirror.
     */
    @Test
    void everySpecsContentIsAssertedAgainstTheGeneratorsOwnMetaFieldSpec() {
        Fixture f = fixture();
        assertEquals(List.of(), reconcile(f));
        boolean sawEnum = false;
        boolean sawPrimitive = false;
        for (MetaFieldGenerator.MetaFieldSpec spec : new MetaFieldGenerator(f.gm(),
                new JavaTypeTranslator(new JavaTypeUtil())).collectSpecs()) {
            if (spec.valueCategory() == MetaFieldGenerator.ValueCategory.ENUM) {
                sawEnum = true;
                assertEquals("seat8.wrappers.Colour", spec.wrappedTypeFqn());
            }
            if (spec.valueCategory() == MetaFieldGenerator.ValueCategory.PRIMITIVE) {
                sawPrimitive = true;
                assertEquals("com.rosetta.model", spec.wrappedTypeNamespace().withDots());
            }
        }
        assertTrue(sawEnum && sawPrimitive, "the fixture must exercise the ENUM and the PRIMITIVE arms");
    }

    /** D09: the ENUM arm folded into COMPOSITE - the arm {@code resolveValueCategory} tests FIRST. */
    @Test
    void d09FoldingTheEnumCategoryIntoCompositeIsRedByKey() {
        Fixture f = fixture();
        f.reconciler().lie(IRDerivedLie.WRAPPER_ENUM_AS_COMPOSITE);
        List<String> mismatches = reconcile(f);
        assertEquals(1, named(mismatches, "wrapper.Colour|seat8.wrappers|FIELD_WITH_META.valueCategory").size(),
                String.join("\n", mismatches));
        assertTrue(mismatches.get(0).contains("the source says ENUM")
                        && mismatches.get(0).contains("the IR says COMPOSITE"),
                mismatches.get(0));
    }

    // ------------------------------------------------------------------------------------------- the harness

    private record Fixture(RModel model, GeneratorModel gm, AstToIRAdapter adapter, IRTypeIndex index,
            IRWrapperReconciler reconciler) {
    }

    private static Fixture fixture() {
        RModel model = AstBuilder.buildFromString(WRAPPERS, "seat8-wrappers.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(model)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        JavaTypeTranslator typeTranslator = new JavaTypeTranslator(new JavaTypeUtil());
        AstToIRAdapter adapter =
                new AstToIRAdapter(AstToIRAdapter.CorpusResolver.NONE, workspace::getInferredType);
        IRDeclarationReconciler parent = new IRDeclarationReconciler(gm, adapter);
        IRTypeIndex index = new IRTypeIndex(workspace, adapter, parent);
        // the index is a CONSTRUCTOR argument (round 1, SF-2): there is no setter to forget and no own-annotations-
        // only fallback to be green by
        IRWrapperReconciler reconciler = new IRWrapperReconciler(gm, typeTranslator, index);
        return new Fixture(model, gm, adapter, index, reconciler);
    }

    private static List<String> reconcile(Fixture f) {
        List<IRModelNode> models = List.of(f.adapter().adaptModelNode(f.model()));
        List<IRTypeNode> declarations = new ArrayList<>();
        String namespace = f.model().namespace();
        for (RRootElement element : f.model().rootElements()) {
            if (element instanceof RDataType dataType) {
                declarations.add(f.index().node(namespace, dataType));
            } else if (element instanceof RChoice choice) {
                declarations.add(f.index().node(namespace, choice));
            }
        }
        return f.reconciler().reconcile(models, declarations);
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
