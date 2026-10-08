package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

/**
 * SEAT 21, lever X — facet {@code overrideChainWitness}: <b>the LAST hop of a call-ARGUMENT
 * navigation whose resolved leaf is an {@code override} witnesses the override-chain property the
 * callee's DECLARED input type selects — the {@code <Witness>} and its import follow the
 * selection — in EVERY context (function bodies, rule bodies, type conditions), because upstream's
 * {@code attributeCall} consults {@code JavaPojoInterface.findProperty(name, expectedType)}
 * ({@code ExpressionGenerator.xtend:359-380}; {@code JavaPojoInterface.java:47-57}: walk leaf →
 * parent, return the first property whose type the expected ITEM type is a subtype of, else the
 * leaf) and the item expected type is installed at the call-argument seat
 * ({@code evaluateCall :296}).</b>
 *
 * <p><b>The defect.</b> The fork's wave-D arm ({@code NavigationHandler.calleeArgBaseAttributeOrNull})
 * carried the law TYPE-CONDITION-gated ("function/rule paths keep their bytes" — the band it was
 * cut from held only datarule carriers, LAW 75 in reverse), matched the argument by identity plus
 * one disguise, and re-implemented the selector one supertype level deep on Rune-node identity;
 * the fork's OWN port of upstream's selector ({@code JavaPojoInterface.findProperty(String,
 * JavaType)}) had ZERO production callers — the POJO declaration half and the nav-witness half
 * never shared it (LAW 69). drr 7.x {@code override reportableInformation ReportableInformation}
 * on {@code ReportableEvent}/{@code TransactionReportInstruction}/{@code CollateralReportInstruction}
 * passed to {@code PayoutFromProductLeg1/2}/{@code ReportingTimestampFromReportableInformation}
 * (declared {@code ReportableInformationBase}) rendered {@code <ReportableInformation>} + its import
 * where golden has {@code <ReportableInformationBase>} + {@code drr.base.trade.ReportableInformationBase}
 * — 32 band rows (24 whole-file heals + 8 partial), a pure parity heal (both forms compile: the
 * override getter is covariant, {@code MapperS.map} is generic — LAW 74, 0 repairs).
 *
 * <p><b>The seat.</b> The gate is gone; the arm matches the three argument shapes the
 * ReferenceHandler synthesizers produce (identity · the disguised 2-name REnumValueRef · the
 * synthesized BARE-symbol nav by its resolved attribute's identity) and CONSULTS ONE selector
 * ({@code overrideChainBaseForDeclaredType} → {@code RJavaPojoInterface.findProperty(name,
 * desired)} with the desired type built by THE property computation itself —
 * {@code RJavaPojoInterface.declaredPropertyJavaType}: the generated {@code FieldWithMeta<X>}/
 * {@code ReferenceWithMeta<X>} class when the input carries {@code [metadata …]}, {@code List<?
 * extends …>} when multi (review SF-3: the generic runtime {@code FieldWithMeta<T>} never matched the
 * generated class and degraded every meta-annotated input to the leaf — {@code a7}) — mapped back to
 * the owning type's attribute). The index lookup is two-pass — IDENTITY at every index first, then the
 * synthesized shapes, a DOUBLE shape match declining to the leaf ({@code b7}, review SF-2). The IR route
 * declines the same predicate at every claim ROOT — five seats ({@code IRExpressionCompiler.
 * tryEmitFromIR}: an apply with a base-witness arg, such a nav itself (resolved through the SAME
 * three-rung leaf ladder the legacy seat reads, {@code NavigationHandler.navLeafAttrOrNull} — review
 * SF-1), a disguised {@code <input> -> <feature>} root, a BARE attribute symbol that is a call arg
 * (evaluated on the {@code synthesizeImplicitItemNavigation} node legacy renders — review SF-5); and
 * the L-111 {@code visitEnumValueRef} quiet FieldAccess claim) so both routes render it through
 * NavigationHandler (LAW 77 — the seat-21 ON probe showed the IR emitter naming the stamped LEAF on
 * 3,959 such navs; the both-route drr 7.0.0 dump at the first chain head named the margin
 * {@code ReportingTimestampRule} — the bare-symbol seat — as the ONE ON-only band file, closed by the
 * fourth branch; {@code a5}/{@code a6} are the function-/rule-seat receipts and {@code control2} the
 * ON-route WHOLE-CELL control). The nested-composition residue — a base-witnessed arg whose containing
 * call is an operand of a NATIVELY-COMPOSED root, the L-105 call-as-operand family — is unguarded and
 * declared in the guard's javadoc: 0 live carriers (every one of the 132 base-witness sites sits under a
 * DELEGATING root).
 *
 * <p><b>LAW 75 — measured before the seat over all 275 matrix rows (the seat-21 runtime probe):</b>
 * of 16,917 override-leaf navs at call seats the un-gated arm fires on exactly the 72 already-right
 * {@code periodicPayment} datarule sites (byte-identical) + the 56 direct/disguised carrier sites
 * + the 4 bare-symbol Group-C sites the third shape admits; every other call-seat override nav
 * keeps its leaf (an inner hop of a synthesized chain, or a callee declaring the leaf type — all
 * nine drr 7.0.0 leaf-declaring callees verified). Static golden census: base-witness sites 132
 * matrix-wide = 72 periodicPayment (already right) + 60 reportableInformation (the carriers);
 * cdm/fpml/iso 0.
 *
 * <p><b>RED at the pre-seat blob</b> ({@code rune-java-generator/src/main} at {@code 9c1bd8ef3}, this suite
 * kept — the COMBINED pre-seat blob, re-measured at the final head after the seat-21 review, MF-1):
 * default <b>22 run / 11 F / 3 skipped</b> = exactly {@code a1, a2, a3, a4, a7, corpus_c1, corpus_c2,
 * corpus_c3, corpus_c4, corpus_c5, corpus_control1}; {@code -Pir-on} with BOTH modules' {@code src/main}
 * at {@code 9c1bd8ef3} and re-installed: <b>22 run / 14 F</b> = the same + {@code a5, a6,
 * corpus_control2} (the ON-route seam receipts now carry a failing-first receipt of their own — review
 * SF-5); every {@code b*} + {@code control0} GREEN in both states. GREEN: default 22 / 0 F / 3 skipped
 * ({@code a5}, {@code a6}, {@code control2} run only under {@code -Pir-on}), {@code -Pir-on} 22/22.
 * <b>LAW 66/76 mutations</b> (each applied → run → reverted AT THE FINAL HEAD;
 * {@code artefacts/review/rv-mut-x-*.log}): (i) the type-condition gate restored → <b>22/11F</b>
 * {a1, a2, a3, a4, a7, c1, c2, c3, c4, c5, control1}; (ii) the bare-symbol (third) argument shape deleted
 * → <b>22/4F</b> {a3, c3, c5, control1} (exactly Group C); (iii) the disguised 2-name shape deleted
 * → <b>22/8F</b> {a1, a4, a7, b4, c1, c4, c5, control1} (a2's `item -> info` is a real RFeatureCall and
 * stays); (iv) the desired type built by the OLD mirror ({@code toMetaJavaType}, no List wrap) instead
 * of THE property computation → <b>22/2F</b> {a4, a7} — the multi and the meta-annotated halves have
 * ZERO corpus carriers, so no whole-cell control can move under it (a4/a7 are the declared fixture
 * pins); (v) the selector replaced by an unconditional parent walk (the desired type ignored; the
 * anchor re-pointed to the C0 pre-check form) → <b>22/2F</b> {b3, control1} (the drr 7.0.0
 * leaf-declaring-callee sites flip — the control sees it); (vi) the IR-route guard deleted at three of
 * the four {@code tryEmitFromIR} branches (call root · disguised root · bare-symbol root) + the L-111
 * claim condition, under {@code -Pir-on} → <b>22/3F</b> {a5, a6, control2} — {@code control2} moves
 * (4 drr 7.0.0 files' witness counts differ from golden): the ON-route whole-cell control SEES the
 * mutation, which {@code control1} (directly-constructed legacy generators) structurally cannot;
 * (vii) ONLY the nav-root {@code tryEmitFromIR} branch deleted, under {@code -Pir-on} → <b>22/2F</b>
 * {a6, control2} — the isolating pin: the branch is LOAD-BEARING on the corpus (3 drr 7.0.0 files) and
 * at the RULE bare-symbol seat; (viii) the ambiguity guard removed (the last shape match wins) →
 * <b>22/1F</b> {b7} (a declared narrowing — zero corpus carriers).
 *
 * <p><b>Declared declines / evidence caps.</b> An {@code RRule} callee (upstream's
 * {@code evaluateCall} serves {@code Function} AND {@code RosettaRule}): zero corpus carriers, no
 * fixture — banked. A nav inside a conditional ARM that is itself the argument: upstream
 * propagates the item expected type into the arms ({@code :710-718}), the fork's arm reads
 * {@code expr.parent()} — {@code b6} locks the LEAF as a declared narrowing (zero corpus carriers).
 * SET/ADD item-expected seats ({@code FunctionGenerator.xtend:409-411, 614}) and the constructor-
 * value seat ({@code :1296}): zero corpus carriers, not claimed.
 */
class OverrideChainWitnessSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /** The override chain (Sub → Mid → Base, the attribute declared two levels up) and the callees that demand the base / the leaf. */
    private static final String MODEL = """
            namespace census.seat21x
            version "1.0.0"

            type InfoBase:
                y string (0..1)

            type Info extends InfoBase:
                x string (0..1)

            type Base: <"the attribute's declaration, two levels above the override (TransactionReportInstruction -> TransactionReportInstructionBase -> ReportableEventBase)">
                info InfoBase (0..1)
                infos InfoBase (0..*)

            type Mid extends Base: <"re-declares nothing — the depth-2 rung">
                note string (0..1)

            type Sub extends Mid:
                override info Info (1..1)
                override infos Info (1..*)

            type NoOvr: <"b1 - a plain DECLARATION of the leaf type: no override chain (ReportableValuation)">
                info Info (1..1)

            type PP:
                v string (0..1)

            type CommonPP extends PP:
                w string (0..1)

            type Leg:
                pp PP (1..1)

            type CommonLeg extends Leg:
                override pp CommonPP (1..1)

            type Report: <"b4 - the wave-D CONDITION carrier shape (CommonLeg.periodicPayment into a PeriodicPayment input); b7 - two SAME-shaped disguised args to one call">
                leg1 CommonLeg (1..1)
                condition LegOk:
                    Validate(leg1 -> pp)
                condition LegOk2:
                    Validate2(leg1 -> pp, leg1 -> pp)

            func Validate:
                inputs:
                    p PP (1..1)
                output:
                    b boolean (1..1)
                set b:
                    p -> v exists

            func Validate2: <"b7 - the callee whose TWO inputs differ (CommonPP at 0, PP at 1): a synthesized nav matching both args by shape cannot tell which index it stands for">
                inputs:
                    p CommonPP (1..1)
                    q PP (1..1)
                output:
                    b boolean (1..1)
                set b:
                    p -> v exists and q -> v exists

            type BaseRef: <"a7 - a META-ANNOTATED override chain: the base declares [metadata reference]">
                ref InfoBase (0..1)
                    [metadata reference]

            type SubRef extends BaseRef:
                override ref Info (1..1)
                    [metadata reference]

            func ConsumeRef: <"a7 - the callee declaring the meta-annotated BASE input">
                inputs:
                    i InfoBase (0..1)
                        [metadata reference]
                output:
                    b boolean (1..1)
                set b:
                    i exists

            func A7MetaAnnotatedArg: <"a7 - a [metadata reference] override into a [metadata reference] base input: the desired type is the GENERATED ReferenceWithMetaInfoBase — the POJO property's own computation — not the generic FieldWithMeta<T>, which never matched">
                inputs:
                    s SubRef (1..1)
                output:
                    r boolean (1..1)
                set r:
                    ConsumeRef(s -> ref)

            func Consume: <"the callee declaring the BASE input (PayoutFromProductLeg1)">
                inputs:
                    i InfoBase (0..1)
                output:
                    r string (0..1)
                set r:
                    i -> y

            func ConsumeMany: <"a4 - the MULTI twin: a List<? extends InfoBase> input">
                inputs:
                    items InfoBase (0..*)
                output:
                    r int (1..1)
                set r:
                    items count

            func ConsumeLeaf: <"b3 - a callee declaring the OVERRIDE type keeps the leaf (GetUniqueTransactionIdentifier)">
                inputs:
                    i Info (0..1)
                output:
                    r string (0..1)
                set r:
                    i -> x

            func A1ExplicitReceiverArg: <"a1 - GROUP A: an explicit-receiver nav as the arg (ChangeInNotionalAmountLeg1), a depth-2 chain">
                inputs:
                    s Sub (1..1)
                output:
                    r string (0..1)
                set r:
                    Consume(s -> info)

            func A4MultiArg: <"a4 - the multi override into a multi base input: the List wrap on the desired type">
                inputs:
                    s Sub (1..1)
                output:
                    r int (1..1)
                set r:
                    ConsumeMany(s -> infos)

            func B3LeafDeclaringCallee: <"b3 - the callee declares the override type: the selector keeps the leaf on rung 1">
                inputs:
                    s Sub (1..1)
                output:
                    r string (0..1)
                set r:
                    ConsumeLeaf(s -> info)

            func B5NonArgChainedNav: <"b5 - the same override attribute at a NON-argument chained nav keeps the leaf (ClearingExceptionsAndExemptionsCounterparty)">
                inputs:
                    s Sub (1..1)
                output:
                    r string (0..1)
                set r:
                    s -> info -> x

            func B6ConditionalArmArg: <"b6 - a nav inside a conditional ARM that is itself the arg: a declared narrowing (the arm reads the nav's parent) — zero corpus carriers">
                inputs:
                    s Sub (1..1)
                    c boolean (1..1)
                output:
                    r string (0..1)
                set r:
                    Consume(if c then s -> info else s -> info)

            reporting rule A2ImplicitItemArg from Sub: <"a2 - GROUP B: `item -> info` as a direct arg in a rule (PayoutForQuantityLeg1)">
                Consume(item -> info)

            reporting rule A3BareSymbolArg from Sub: <"a3 - GROUP C: the BARE symbol arg in an extract (ReportingTimestampRule)">
                extract Consume(info)

            reporting rule B1NoOverrideFallback from NoOvr: <"b1 - no override on the chain: upstream's `// Fallback` keeps the leaf">
                extract Consume(info)

            reporting rule B2BaseReceiver from Base: <"b2 - a BASE-typed receiver witnesses the base by plain resolution">
                extract Consume(info)
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    private static final String BASE_IMPORT = "import census.seat21x.InfoBase;";
    private static final String LEAF_IMPORT = "import census.seat21x.Info;";

    /** a1 — Group A: the explicit-receiver nav arg witnesses the BASE (depth-2 chain), and the import follows. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_explicitReceiverArgWitnessesTheBase() throws IOException {
        String out = function("A1ExplicitReceiverArg.java");
        assertContains(out, "consume.evaluate(MapperS.of(s).<InfoBase>map(\"getInfo\", sub -> sub.getInfo()).get())");
        assertContains(out, BASE_IMPORT);
        assertNotContains(out, LEAF_IMPORT);
        assertNotContains(out, "<Info>map(");
    }

    /** a2 — Group B: `item -> info` as a direct arg in a rule body. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_implicitItemArgWitnessesTheBase() throws IOException {
        String out = rule("A2ImplicitItemArgRule.java");
        assertContains(out, ".<InfoBase>map(\"getInfo\", sub -> sub.getInfo())");
        assertContains(out, BASE_IMPORT);
        assertNotContains(out, "<Info>map(");
    }

    /** a3 — Group C: the BARE symbol arg synthesized into an item nav (the third argument shape). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_bareSymbolArgWitnessesTheBase() throws IOException {
        String out = rule("A3BareSymbolArgRule.java");
        assertContains(out, ".<InfoBase>map(\"getInfo\", sub -> sub.getInfo())");
        assertContains(out, BASE_IMPORT);
        assertNotContains(out, "<Info>map(");
    }

    /** a4 — the MULTI override into a multi base input: the desired type is List-wrapped like the POJO property. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_multiOverrideArgWitnessesTheBaseList() throws IOException {
        String out = function("A4MultiArg.java");
        assertContains(out, ".<InfoBase>mapC(\"getInfos\", sub -> sub.getInfos())");
        assertContains(out, BASE_IMPORT);
        assertNotContains(out, "<Info>mapC(");
    }

    /**
     * a5 — LAW 77: the FUNCTION seat rendered through the REAL {@code IRGeneration.functionGenerator}
     * seam (the IR provider on the classpath, {@code -Pir-on}) witnesses the base too — the IR
     * compiler declines the claim and legacy renders it. Were the IR emitter rendering the arg nav
     * natively (it names the stamped LEAF), this test would be RED.
     */
    @Test
    @EnabledIf("irProviderOnClasspath")
    void a5_theFunctionSeatWitnessesTheBaseOnTheIrRouteToo() throws IOException {
        String out = lookup(renderOnIrRoute(), "functions/A1ExplicitReceiverArg.java");
        assertContains(out, "consume.evaluate(MapperS.of(s).<InfoBase>map(\"getInfo\", sub -> sub.getInfo()).get())");
        assertContains(out, BASE_IMPORT);
        assertNotContains(out, "<Info>map(");
    }

    /**
     * a6 — LAW 77 at the RULE seat: the bare-symbol arg (Group C, {@code extract Consume(info)}) rendered
     * through a RuleGenerator whose FunctionGenerator came from the REAL {@code IRGeneration} seam — the
     * IR route lowers the bare implicit-item feature to a FieldAccess off the item and would name the
     * LEAF; {@code bareArgIsOverrideBaseWitness} keeps it legacy-side. The seat-21 both-route drr 7.0.0
     * dump named exactly this shape (the margin {@code ReportingTimestampRule}) as the ONE ON-only band
     * file before this guard existed.
     */
    @Test
    @EnabledIf("irProviderOnClasspath")
    void a6_theRuleBareSymbolSeatWitnessesTheBaseOnTheIrRouteToo() throws IOException {
        String out = lookup(renderOnIrRoute(), "reports/A3BareSymbolArgRule.java");
        assertContains(out, ".<InfoBase>map(\"getInfo\", sub -> sub.getInfo())");
        assertContains(out, BASE_IMPORT);
        assertNotContains(out, "<Info>map(");
    }

    /**
     * a7 — seat-21 review SF-3 (LAW 69): a {@code [metadata reference]} override into a
     * {@code [metadata reference]} base input. The selector's desired type is THE property computation
     * ({@code RJavaPojoInterface.declaredPropertyJavaType} — the GENERATED {@code ReferenceWithMetaInfoBase}),
     * so {@code findProperty(name, desired)} walks to the parent whose property type it equals; the first
     * cut's generic runtime {@code FieldWithMeta<T>} was never a subtype of the generated class and
     * degraded every meta-annotated input to the leaf (zero corpus carriers — the rings could not see it).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a7_metaAnnotatedOverrideArgWitnessesTheMetaBase() throws IOException {
        String out = function("A7MetaAnnotatedArg.java");
        assertContains(out, ".<ReferenceWithMetaInfoBase>map(\"getRef\", subRef -> subRef.getRef())");
        assertNotContains(out, "<ReferenceWithMetaInfo>map(");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — NO override on the chain: the selector's fallback keeps the leaf (ReportableValuation twin). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_noOverrideOnTheChainKeepsTheLeaf() throws IOException {
        String out = rule("B1NoOverrideFallbackRule.java");
        assertContains(out, ".<Info>map(\"getInfo\", noOvr -> noOvr.getInfo())");
        assertContains(out, LEAF_IMPORT);
        assertNotContains(out, BASE_IMPORT);
    }

    /** b2 — a BASE-typed receiver already witnesses the base by plain resolution; the arm must not double-handle it. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_baseTypedReceiverWitnessesTheBaseByResolution() throws IOException {
        String out = rule("B2BaseReceiverRule.java");
        assertContains(out, ".<InfoBase>map(\"getInfo\", base -> base.getInfo())");
        assertContains(out, BASE_IMPORT);
    }

    /** b3 — the callee declares the OVERRIDE type: rung 1 keeps the leaf (every drr 7.0.0 leaf-declaring callee). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_leafDeclaringCalleeKeepsTheLeaf() throws IOException {
        String out = function("B3LeafDeclaringCallee.java");
        assertContains(out, "consumeLeaf.evaluate(MapperS.of(s).<Info>map(\"getInfo\", sub -> sub.getInfo()).get())");
        assertContains(out, LEAF_IMPORT);
        assertNotContains(out, BASE_IMPORT);
    }

    /** b4 — the wave-D CONDITION carrier (the disguised 2-name arg in a type condition) keeps firing through the rewrite. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_conditionCarrierStillWitnessesTheBase() throws IOException {
        String out = datarule("ReportLegOk.java");
        assertContains(out, ".<PP>map(\"getPp\", commonLeg -> commonLeg.getPp())");
        assertContains(out, "import census.seat21x.PP;");
        assertNotContains(out, "<CommonPP>map(");
    }

    /** b5 — the same override attribute at a NON-argument chained nav keeps the leaf (a Mapper-expected seat). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_nonArgChainedNavKeepsTheLeaf() throws IOException {
        String out = function("B5NonArgChainedNav.java");
        assertContains(out, "MapperS.of(s).<Info>map(\"getInfo\", sub -> sub.getInfo()).<String>map(\"getX\", info -> info.getX())");
        assertNotContains(out, "<InfoBase>map(");
    }

    /**
     * b6 — a nav inside a conditional ARM that is itself the argument: upstream propagates the item
     * expected type into the arms (base witness); the fork's arm reads the nav's parent (the
     * conditional) and keeps the LEAF — a DECLARED narrowing with zero corpus carriers, locked so a
     * future widening is a deliberate seat.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_conditionalArmArgKeepsTheLeaf_declaredNarrowing() throws IOException {
        String out = function("B6ConditionalArmArg.java");
        assertContains(out, "<Info>map(\"getInfo\", sub -> sub.getInfo())");
        assertNotContains(out, "<InfoBase>map(");
    }

    /**
     * b7 — seat-21 review SF-2: two SAME-shaped disguised args to one call ({@code Validate2(leg1 -> pp,
     * leg1 -> pp)}, inputs CommonPP at 0 / PP at 1). A synthesized nav matches both by shape and cannot tell
     * which index it stands for; the index lookup is ambiguous and the seat DECLINES to the leaf for both
     * (never guesses the declared input — the pre-seat bytes). A declared narrowing with zero corpus
     * carriers (upstream, rendering the real arg node at its own index, would base-witness arg 1); the
     * mutation that lets the last shape match win flips both to {@code <PP>}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b7_ambiguousSameShapedArgsKeepTheLeaf_declaredNarrowing() throws IOException {
        String out = datarule("ReportLegOk2.java");
        assertContains(out, ".<CommonPP>map(\"getPp\", commonLeg -> commonLeg.getPp())");
        assertNotContains(out, "<PP>map(");
    }

    // =========================================================================
    // Part C — the corpus (drr 7.0.0: the carriers + the whole-cell control)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), OverrideChainWitnessSeatTest.class);
    }

    /** The 6 WHOLE-FILE carriers of the generated drr 7.0.0 cell (the same files in 7.1–7.3 are locked by the 275-row matrix digest — not the ring, which is the five uniform 9.83.0 cells). */
    private static final List<String> DRR7_WHOLE_FILE_CARRIERS = List.of(
            "drr/regulation/cftc/rewrite/functions/ChangeInNotionalAmountLeg1.java",
            "drr/regulation/cftc/rewrite/functions/ChangeInNotionalAmountLeg2.java",
            "drr/enrichment/common/valuation/functions/Create_ValuationDetailsFromReportableEvent.java",
            "drr/regulation/common/margin/datetime/reports/ReportingTimestampRule.java",
            "drr/regulation/common/trade/reports/PayoutForQuantityLeg1Rule.java",
            "drr/regulation/common/trade/reports/PayoutForQuantityLeg2Rule.java");

    /** The 2 PARTIAL carriers (the residue is another lever — the witness LINE is locked, not the file). */
    private static final List<String> DRR7_PARTIAL_CARRIERS = List.of(
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetTradingPlatformIdentifierLeg1Rule.java",
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetTradingPlatformIdentifierLeg2Rule.java");

    private static final String T_BASE = ".<ReportableInformationBase>map(\"getReportableInformation\"";
    private static final String T_LEAF = ".<ReportableInformation>map(\"getReportableInformation\"";
    private static final String T_PP_BASE = ".<PeriodicPayment>map(\"getPeriodicPayment\"";
    private static final String T_PP_LEAF = ".<CommonPeriodicPayment>map(\"getPeriodicPayment\"";
    private static final String BASE_RI_IMPORT = "import drr.base.trade.ReportableInformationBase;";

    /** c1 — Group A whole-file lock (FUNCTION: the disguised explicit-receiver arg). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_changeInNotionalAmountLeg1ByteIdentical() throws IOException {
        lock(DRR7_WHOLE_FILE_CARRIERS.get(0));
    }

    /** c2 — Group B whole-file lock (POJO: `item -> reportableInformation` as a direct arg). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_payoutForQuantityLeg1RuleByteIdentical() throws IOException {
        lock(DRR7_WHOLE_FILE_CARRIERS.get(4));
    }

    /** c3 — Group C whole-file lock (POJO: the bare symbol arg `extract Fn(reportableInformation)`). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c3_reportingTimestampRuleByteIdentical() throws IOException {
        lock(DRR7_WHOLE_FILE_CARRIERS.get(3));
    }

    /** c4 — the biggest carrier: 8 sites in one FUNCTION file (the ReportableEvent override), whole-file lock. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c4_createValuationDetailsFromReportableEventByteIdentical() throws IOException {
        lock(DRR7_WHOLE_FILE_CARRIERS.get(2));
    }

    /** c5 — ALL six whole-file carriers byte-identical + the two partial carriers carry the base witness LINE and import. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c5_allCarriersHealedOrLineLocked() throws IOException {
        for (String path : DRR7_WHOLE_FILE_CARRIERS) {
            lock(path);
        }
        for (String path : DRR7_PARTIAL_CARRIERS) {
            String generated = drr7Output.get(path);
            assertNotNull(generated, "not generated: " + path);
            assertContains(generated, T_BASE);
            assertContains(generated, BASE_RI_IMPORT);
            assertNotContains(generated, T_LEAF);
        }
    }

    /**
     * control0 — golden is the oracle (the frozen drr 7.0.0 tree): the four witness populations at
     * their exact seat-21 census counts — base {@code reportableInformation} 48 sites / 32 files (the
     * carriers + the base-typed receivers), leaf 247 / 177 (the Mapper-expected seats), base
     * {@code periodicPayment} 20 / 11 (the wave-D datarule set + the {@code feeLeg} receivers), leaf
     * 929 / 221 — the scan sees every population it must keep apart.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenDrr7IsTheOracle() throws IOException {
        Map<String, String> golden = readGoldenTree(DRR7_GOLDEN);
        WitnessScan g = scan(golden);
        assertEquals(48, g.sites(T_BASE), "golden base reportableInformation sites");
        assertEquals(32, g.files(T_BASE), "golden base reportableInformation files");
        assertEquals(247, g.sites(T_LEAF), "golden leaf reportableInformation sites");
        assertEquals(177, g.files(T_LEAF), "golden leaf reportableInformation files");
        assertEquals(20, g.sites(T_PP_BASE), "golden base periodicPayment sites");
        assertEquals(11, g.files(T_PP_BASE), "golden base periodicPayment files");
        assertEquals(929, g.sites(T_PP_LEAF), "golden leaf periodicPayment sites");
        assertEquals(221, g.files(T_PP_LEAF), "golden leaf periodicPayment files");
        // the union domain control1/control2 compare over: 441 = 32 + 177 + 11 + 221 (the four token
        // file-sets are pairwise disjoint) — a census pin that moves must move in the same commit
        assertEquals(441, g.perFile.size(), "golden witness-bearing files (the whole-cell controls' domain)");
        for (String carrier : DRR7_WHOLE_FILE_CARRIERS) {
            assertTrue(g.perFile.containsKey(carrier) && g.perFile.get(carrier)[0] > 0,
                    "golden carrier carries the base witness: " + carrier);
        }
    }

    /**
     * control1 — the FORK's WHOLE generated drr 7.0.0 cell (every kind the suite generates, LAW 72):
     * over the UNION of the files either tree carries a witness token in (441 = control0's 32 + 177 + 11
     * + 221, the four token file-sets being disjoint — a missing side counts as all-zero), the four
     * witness counts agree FILE BY FILE, so a witness token appearing in a file golden has none in
     * (an over-fire) or disappearing from a file's last site (an under-fire) both fail here, not only
     * at the carriers; and the carriers are REACHED. (Seat-21 review MF-5: the first cut compared the
     * INTERSECTION of token-bearing files, blind to both directions; the pinned domain replaces the
     * loose reach floor that alone had caught the L+Z mutation (vi).) Renders through directly-
     * constructed LEGACY generators — it can never see an IR-route mutation; control2 is its ON twin.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_forkDrr7WholeCellWitnessCountsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        assertWholeCellWitnessCountsEqualGolden(drr7Output, "legacy route");
    }

    /**
     * control2 — the SAME whole-cell file-by-file witness compare on the IR ROUTE: the cell generated
     * through the REAL {@code IRGeneration} seams ({@code modelObjectGenerator} / {@code choiceObjectGenerator}
     * / {@code functionGenerator} / {@code generateClasses} — the D11 ON ring's own wiring, the provider
     * asserted present and the seam asserted to hand back the IR-route generator) and scanned with the
     * same tokens over the same union domain. control1 renders through directly-constructed legacy
     * generators and can NEVER see an IR-route mutation (the #591 LAW-66 near-miss: the {@code -Pir-on}
     * flag is read only at the seams) — this is the ONLY whole-cell control that can move under the
     * IR-guard mutations (vi)/(vii) (LAW 76; seat-21 review MF-2); {@code a5}/{@code a6} are its
     * fixture-scale twins. Runs under {@code -Pir-on} only.
     */
    @Test
    @EnabledIf("irRouteDrr7Available")
    void corpus_control2_irRouteDrr7WholeCellWitnessCountsEqualGoldenFileByFile() throws IOException {
        assertWholeCellWitnessCountsEqualGolden(irRouteDrr7Output(), "IR route");
    }

    private static void assertWholeCellWitnessCountsEqualGolden(Map<String, String> forkCell, String route)
            throws IOException {
        Map<String, String> golden = readGoldenTree(DRR7_GOLDEN);
        WitnessScan f = scan(forkCell);
        WitnessScan g = scan(golden);
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.LinkedHashSet<>(f.perFile.keySet());
        universe.addAll(g.perFile.keySet());
        int[] zero = new int[TOKENS.length];
        int compared = 0;
        for (String key : universe) {
            int[] fc = f.perFile.getOrDefault(key, zero);
            int[] gc = g.perFile.getOrDefault(key, zero);
            compared++;
            if (!java.util.Arrays.equals(gc, fc)) {
                mismatched.add(key + " fork=" + java.util.Arrays.toString(fc)
                        + " golden=" + java.util.Arrays.toString(gc));
            }
        }
        // the per-file diff first (the actionable diagnostic), then the domain pin: the union domain must
        // EQUAL golden's witness-bearing file count (control0's 441) — an over-fire into a golden-tokenless
        // file would raise it (and show above as a mismatch), an under-fire that empties a file lowers it
        assertEquals(List.of(), mismatched, "[" + route + "] witness counts differ from golden in " + mismatched.size() + " file(s)");
        assertEquals(441, compared, "[" + route + "] the union domain must equal golden's 441 witness-bearing files (an over-fire raises it, an under-fire lowers it)");
        for (String carrier : DRR7_WHOLE_FILE_CARRIERS) {
            assertTrue(f.perFile.containsKey(carrier) && f.perFile.get(carrier)[0] > 0,
                    "[" + route + "] carrier REACHED (carries the base witness): " + carrier);
        }
    }

    // =========================================================================
    // The scan
    // =========================================================================

    private static final String[] TOKENS = { T_BASE, T_LEAF, T_PP_BASE, T_PP_LEAF };

    private static final class WitnessScan {
        /** path → counts of the four tokens (in TOKENS order), only for files carrying at least one. */
        final Map<String, int[]> perFile = new LinkedHashMap<>();

        int sites(String token) {
            int idx = indexOf(token), n = 0;
            for (int[] c : perFile.values()) n += c[idx];
            return n;
        }

        int files(String token) {
            int idx = indexOf(token), n = 0;
            for (int[] c : perFile.values()) if (c[idx] > 0) n++;
            return n;
        }

        private static int indexOf(String token) {
            for (int i = 0; i < TOKENS.length; i++) if (TOKENS[i].equals(token)) return i;
            throw new IllegalArgumentException(token);
        }
    }

    private static WitnessScan scan(Map<String, String> tree) {
        WitnessScan r = new WitnessScan();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int[] counts = new int[TOKENS.length];
            boolean any = false;
            for (int i = 0; i < TOKENS.length; i++) {
                counts[i] = countOccurrences(code, TOKENS[i]);
                any |= counts[i] > 0;
            }
            if (any) {
                r.perFile.put(e.getKey(), counts);
            }
        }
        return r;
    }

    private static Map<String, String> readGoldenTree(Path dir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) stream.filter(q -> q.toString().endsWith(".java"))::iterator) {
                out.put(dir.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    /** Strip block and line comments so doc text cannot pollute a token count (string literals kept). */
    private static String codeOnly(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("/*", i)) {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else if (s.startsWith("//", i)) {
                int end = s.indexOf('\n', i);
                i = end < 0 ? s.length() : end;
            } else if (s.charAt(i) == '"') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != '"') {
                    if (s.charAt(j) == '\\') j++;
                    j++;
                }
                sb.append(s, i, Math.min(j + 1, s.length()));
                i = j + 1;
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0, i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    // =========================================================================
    // Corpus generation (the same harness every seat suite uses)
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors = new ArrayList<>();

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr7Available()) {
            List<String> errs = new ArrayList<>();
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
            drr7GenErrors = errs;
        }
    }

    private static Map<String, String> drr7OutputIr;

    static boolean irRouteDrr7Available() {
        return irProviderOnClasspath() && drr7Available();
    }

    /** The drr 7.0.0 cell rendered on the IR ROUTE (once per JVM; control2's subject). */
    private static Map<String, String> irRouteDrr7Output() throws IOException {
        if (drr7OutputIr == null) {
            drr7OutputIr = generateCellOnIrRoute(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), new ArrayList<>());
        }
        return drr7OutputIr;
    }

    /**
     * The cell through the REAL {@code IRGeneration} seams — the D11 ON ring's wiring (the flag set for
     * the render and restored after; the provider asserted present; the function seam asserted to hand
     * back the IR-route generator, so this can never silently be an OFF-route render).
     */
    private static Map<String, String> generateCellOnIrRoute(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            var pojoGen = IRGeneration.modelObjectGenerator(gm, typeTranslator, typeUtil);
            var choiceGen = IRGeneration.choiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
            var labelProviderGen = new LabelProviderGenerator(
                    gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                    new LabelProviderGeneratorUtil());
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, IRGeneration.generateClasses(pojoGen, model, version, output));
                    collect(errors, IRGeneration.generateClasses(choiceGen, model, version, output));
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
                    collect(errors, dataRuleGen.generateClasses(model, version, output));
                    collect(errors, labelProviderGen.generateClasses(model, version, output));
                }
            }
            collect(errors, funcGen.generateWithErrors(output));
            return output;
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, pojoGen.generateClasses(model, version, output));
                collect(errors, choiceGen.generateClasses(model, version, output));
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lock(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr7GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = DRR7_GOLDEN.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 21 X: the override-chain witness at a call-argument seat.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat21x.rosetta");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
            mainModel = main;
        }
    }

    private static Map<String, String> render(Predicate<RModel> filter) throws IOException {
        link();
        GeneratorModel gm = new GeneratorModel(linking.workspace(), filter);
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        DataRuleGenerator dataRuleGen = new DataRuleGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        dataRuleGen.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat21x".equals(m.namespace()));
        }
        return fixtureOut;
    }

    /** True only when the IR route's provider is on the test classpath (the {@code -Pir-on} profile). */
    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * The fixture's FUNCTIONS rendered on the IR route: the flag set for the duration of the render
     * (restored after), the generator obtained through the REAL {@code IRGeneration.functionGenerator}
     * seam, and the provider asserted present so the render cannot silently take the legacy path.
     */
    private static Map<String, String> renderOnIrRoute() throws IOException {
        link();
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            GeneratorModel gm = new GeneratorModel(linking.workspace(),
                    m -> "census.seat21x".equals(m.namespace()));
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
            assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
            // the RULE seat on the IR route: a RuleGenerator over the IR-route FunctionGenerator (the
            // same wiring the D11 ON ring uses — rule bodies compile through that generator's compiler)
            RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
            Map<String, String> out = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            ruleGen.generateClasses(mainModel, "1.0", out)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            fg.generateWithErrors(out)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            if (!errors.isEmpty()) {
                throw new AssertionError("IR-route fixture generation errors: " + errors);
            }
            return out;
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
    }

    private static String function(String fileName) throws IOException {
        return lookup(fixture(), "functions/" + fileName);
    }

    private static String datarule(String fileName) throws IOException {
        return lookup(fixture(), "datarule/" + fileName);
    }

    private static String lookup(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "'" + suffix + "' was not generated; keys=" + output.keySet()));
    }

    private static List<RModel> loadBuiltinsOnly() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<String> failures = new ArrayList<>();
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try {
                        models.add(AstBuilder.buildFromFile(p));
                    } catch (Exception e) {
                        failures.add(p + " — " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[OverrideChainWitnessSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle),
                "expected needle missing:\n" + needle + "\n--- in output:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertFalse(out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
