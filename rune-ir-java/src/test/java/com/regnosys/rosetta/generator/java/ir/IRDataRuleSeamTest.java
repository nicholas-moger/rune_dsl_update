package com.regnosys.rosetta.generator.java.ir;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 SEAT 3 (D54) - THE DATA_RULE SEAM's seat test: the two halves that must agree (LAW 69) on ONE fixture
 * carrying every shape D54's byte-identity argument names (the ADR review's SF-9):
 * <ul>
 *   <li>a {@code one-of} condition and an explicit {@code optional choice} (the generator-synthesized cardinality
 *       TWINS - their roots are untargeted visits, their synthesized receivers one in-share claim each);</li>
 *   <li>a {@code choice} type (its implicit one-of - a third twin);</li>
 *   <li>a bare boolean attribute at root, a bare-attribute comparison (the {@code IRImplicitAttrNav} leaf the first heal
 *       admits), a bare-attribute existence;</li>
 *   <li>a bigInteger literal (the {@code IRLiteralHandler.bigIntegerBaseName} seat);</li>
 *   <li>a then-chain inside a ladder rung and at root (the {@code IRCollectionHandler.thenArgBaseName} seat);</li>
 *   <li>a bare-function-call condition; a no-else conditional as a call ARGUMENT and inside a constructor's field value
 *       (the seat-agnostic {@code IRControlFlowHandler.ifThenElseResultBaseName} arm's two illustrative forms);</li>
 *   <li>a conditional with a real else as the whole body (the datarule ladder's else terminal) and as an equality
 *       operand.</li>
 * </ul>
 * The OFF half renders through the legacy {@link DataRuleGenerator}, the ON half through {@link IRDataRuleGenerator};
 * every generated class must be BYTE-IDENTICAL, the IR compiler must have been reached (a zero-attempt pass would be
 * a vacuous identity), the twin roots must read on the untargeted axis, and the bare-attribute comparison must decline
 * at the operand gate with the exact token the census named (the first heal's target) - AS SEAT 3 WROTE IT: since
 * PR #640 (v3.3 seat 4, the first heal) that comparison LOWERS and the pins below are the heal's own reading
 * (scratch/seamtest-c4-reading.print: adapterGap=2); the rest of this paragraph is the seat-3 provenance. The receiver claims' outcome
 * WAS PINNED from the fixture's own first reading (the offload box measurement measure-s3c3b at 3ac0080d3 and the run of record
 * run-s3c4 at 07de05ea3 print the same line): the site, family and untargeted breakdowns asserted EQUAL as strings, so a
 * receiver that moves from a decline to a serve (or a twin root that leaves the untargeted axis) is a red test that names
 * the heal - the run of record's ON run (run-s3c4) prints 3,106 twin roots over the 25 vendored cells on its
 * {@code DATA_RULE IR untargeted visits} lines, one synthesized receiver each (the census's 2,931 was the pre-run
 * hypothesis - D54 1(f)); PR #639 round 1, spec SF-5; round 2, SF-2.
 *
 * <p>A generation error on either half is a red test, never a swallow (the seam's own {@code assertNoGenErrors} law).
 */
class IRDataRuleSeamTest {

    private static final String SOURCE = """
            namespace "test.step639"

            type Pair:
                a int (1..1)
                b int (1..1)

            type Bounds:
                start date (0..1)
                end date (0..1)
                flag boolean (0..1)
                big number (0..1)
                amounts number (0..*)
                pair Pair (0..1)

                condition Ordered: start <= end
                condition Flag: flag
                condition StartExists: start exists
                condition Big: big <= 12345678901234567890
                condition Fn: IsOk(start)
                condition CallIte: IsOk(if flag = True then start else end)
                condition Ladder: if flag = True then amounts count > 0
                condition LadderChain: if flag = True then amounts extract [ item * 2 ] then only-element > 0
                condition Chain: amounts extract [ item * 2 ] then only-element > 0
                condition IteElse: if flag = True then start exists else end exists
                condition IteOperand: (if flag = True then 1 else 2) = 1
                condition Ctor: Pair { a: if flag = True then 1 else 2, b: 3 } exists
                condition Opt: optional choice start, end

            type Exclusive:
                left string (0..1)
                right string (0..1)

                condition OneOf: one-of

            choice Either:
                Pair
                Bounds

            func IsOk:
                inputs:
                    d date (0..1)
                output:
                    ok boolean (1..1)
                set ok:
                    d exists
            """;

    private record Fixture(RModel model, RWorkspace ws, GeneratorModel gm, JavaTypeTranslator tt, JavaTypeUtil tu) {
    }

    private static Fixture fixture() {
        RModel model = AstBuilder.buildFromString(SOURCE, "step639-seam.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        JavaTypeUtil tu = new JavaTypeUtil();
        return new Fixture(model, ws, new GeneratorModel(ws, m -> true), new JavaTypeTranslator(tu), tu);
    }

    @Test
    void everyConditionClassRendersByteIdenticallyOnBothRoutesAndTheIrCompilerIsReached() {
        Fixture f = fixture();
        // the fixture's population, COUNTED from the AST (never typed): 13 own conditions on Bounds + 1 on Exclusive
        long conditions = f.model().rootElements().stream()
                .filter(e -> e instanceof RDataType)
                .mapToLong(e -> ((RDataType) e).conditions().size()).sum();
        assertEquals(14, conditions, "the fixture parsed every condition (a dropped shape is a silently narrower test)");

        DataRuleGenerator legacy = new DataRuleGenerator(f.gm(), f.tt(), f.tu());
        IRDataRuleGenerator ir = new IRDataRuleGenerator(f.gm(), f.tt(), f.tu());
        Map<String, String> off = new LinkedHashMap<>();
        Map<String, String> on = new LinkedHashMap<>();
        List<GenerationException> offErrors = legacy.generateClasses(f.model(), "0.0.0", off);
        List<GenerationException> onErrors = ir.generateClasses(f.model(), "0.0.0", on);
        assertTrue(offErrors.isEmpty(), "the legacy route reported generation errors: " + offErrors);
        assertTrue(onErrors.isEmpty(), "the IR route reported generation errors: " + onErrors);
        assertEquals(off.keySet(), on.keySet(), "both routes write the same condition classes");
        // 14 conditions + the choice type's implicit one-of = 15 classes (COUNTED: a class per case)
        assertEquals(conditions + 1, off.size(), "one class per condition plus the choice's implicit one-of: " + off.keySet());
        for (String path : off.keySet()) {
            assertEquals(off.get(path), on.get(path), "BYTE-IDENTICAL on both routes: " + path);
        }

        IRExpressionCompiler compiler = ir.irExpressionCompiler();
        int attempted = compiler.irDrivenCount() + compiler.irDeclinedCount();
        assertTrue(attempted > 0, "the IR compiler's claim seats were reached (a zero-attempt identity is vacuous)");
        // D54 item 1(f): the cardinality TWINS' roots are untargeted visits - the one-of, the optional choice and the
        // choice type's implicit one-of, three of them
        String untargeted = compiler.untargetedVisitBreakdown();
        assertEquals("RCardinalityCheckExpr=3", untargeted,
                "the three twin roots read on the untargeted axis, and nothing else does (an EQUALITY - a substring test"
                        + " admitted RCardinalityCheckExpr=30; PR #639 round 1, cq NIT-6): " + untargeted);
        // the receiver claims and the bare-attribute claims: IN the share, where the adapter puts them. HISTORY (seat 3,
        // NOT the pins below - those are the #640 heal's reading, re-pinned under the print): first PINNED from the
        // fixture's own first reading (measure-s3c3b / run-s3c4: 'by site: adapterGap=29 by family: RFeatureCall=21
        // RExistenceExpr=4 RComparisonExpr=2 REqualityExpr=1 RSymbolReference=1'); the one RSymbolReference decline is a
        // twin receiver at the mint gate, the other two receivers are served - a moved breakdown names its heal (spec SF-5)
        // the line is PRINTED before the pins, so a moved reading is on the log beside the red assertion (v3.3 seat 4)
        System.out.println("IRDataRuleSeamTest: irDriven=" + compiler.irDrivenCount() + " irDeclined="
                + compiler.irDeclinedCount() + " by site: " + compiler.irDeclineSiteBreakdown()
                + " by family: " + compiler.irDeclineFamilyBreakdown() + " untargeted: " + untargeted
                + " emitterServeLowered=" + compiler.emitterServeLoweredCount()
                + " oracleRootLowered=" + compiler.oracleRootLoweredCount());
        // re-pinned at v3.3 seat 4 (PR #640, the first heal under the DATA_RULE gate) FROM THE FIXTURE'S OWN reading
        // at the heal (scratch/seamtest-c4-reading.print): the bare-attribute comparisons, existences, call arguments,
        // chains and the twins' synthesized receivers all serve now; the two that stay declined are the equality
        // `flag = True` against a boolean literal (the equality seat's sibling set) and the constructor existence
        assertEquals("adapterGap=2", compiler.irDeclineSiteBreakdown(), "the fixture's decline sites, pinned from the heal's reading");
        assertEquals("REqualityExpr=1 RExistenceExpr=1",
                compiler.irDeclineFamilyBreakdown(), "the fixture's decline families, pinned from the heal's reading");
        assertEquals(125, compiler.irDrivenCount(), "the fixture's irDriven, pinned from the heal's reading");
        assertEquals(2, compiler.irDeclinedCount(), "the fixture's irDeclined, pinned from the heal's reading");
    }

    @Test
    void theBareAttributeComparisonAndExistenceLowerSinceThe640Heal() {
        // the census's largest direct class (CENSUS.md § 3) pinned its token at seat 3 (`operand:IRImplicitAttrNav`);
        // the first heal under the DATA_RULE gate (v3.3 seat 4, PR #640 - D54 item 3) admits the kind at the comparison
        // and existence seats: the token is gone and both roots lower (the adapter test carries the seat-by-seat witnesses)
        Fixture f = fixture();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();
        RCondition ordered = condition(f.model(), "Bounds", "Ordered");
        assertTrue(adapter.adapt(ordered.expression(), f.ws()).isPresent(), "the bare-attribute comparison lowers");
        assertEquals(Optional.empty(), adapter.declineReason(ordered.expression(), f.ws()),
                "the operand gate's token retired with the heal");
        RCondition exists = condition(f.model(), "Bounds", "StartExists");
        assertTrue(adapter.adapt(exists.expression(), f.ws()).isPresent(), "the bare-attribute existence lowers");
        assertEquals(Optional.empty(), adapter.declineReason(exists.expression(), f.ws()),
                "the existence gate's token retired with the heal");
    }

    @Test
    void theProviderAndTheSeamHelperReturnTheIrClassOnTheReferenceRouteOnly() {
        Fixture f = fixture();
        assertInstanceOf(IRDataRuleGenerator.class,
                new IRGenerationProviderImpl().dataRuleGenerator(f.gm(), f.tt(), f.tu()),
                "the provider's DATA_RULE construction seam returns the IR-routed subclass");
        assertFalse(Boolean.getBoolean(IRGeneration.PROPERTY), "precondition: the suite starts flag-off");
        assertEquals(DataRuleGenerator.class, IRGeneration.dataRuleGenerator(f.gm(), f.tt(), f.tu()).getClass(),
                "flag-off: the exact legacy class, never looked up");
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertInstanceOf(IRDataRuleGenerator.class, IRGeneration.dataRuleGenerator(f.gm(), f.tt(), f.tu()),
                    "flag-on with this module on the classpath: the seam helper constructs the IR-routed subclass");
        } finally {
            System.clearProperty(IRGeneration.PROPERTY);
        }
    }

    private static RCondition condition(RModel model, String typeName, String conditionName) {
        RDataType type = model.rootElements().stream()
                .filter(e -> e instanceof RDataType dt && typeName.equals(dt.name()))
                .map(e -> (RDataType) e).findFirst().orElseThrow();
        return type.conditions().stream()
                .filter(c -> c.name().isPresent() && conditionName.equals(c.name().get()))
                .findFirst().orElseThrow();
    }
}
