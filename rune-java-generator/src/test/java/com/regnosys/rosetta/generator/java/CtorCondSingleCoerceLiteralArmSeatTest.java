package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 32, law B2 -- facet {@code ctorCondSingleCoerceLiteralArm} (close-census family F8): a
 * ctor-setter conditional whose ladder carries an INT-LITERAL arm at a SINGLE, meta-NONE
 * {@code int} attribute declares its hoisted local at the ATTRIBUTE's type and assigns the
 * literal BARE there, exactly as golden does.
 *
 * <pre>
 * GOLDEN                                                          FORK (pre-law)
 * final Integer ifThenElseResult1;                            &lt;-  final BigDecimal ifThenElseResult1;
 * ifThenElseResult1 = 1;                                      &lt;-  ifThenElseResult1 = BigDecimal.valueOf(1);
 * ifThenElseResult1 = periodMultiplier == null ? null         &lt;-  ifThenElseResult1 = periodMultiplier;
 *         : periodMultiplier.intValueExact();
 * .setPeriodMultiplier(ifThenElseResult1)                     &lt;-  .setPeriodMultiplier((ifThenElseResult1 == null ? null : ifThenElseResult1.intValueExact()))
 * </pre>
 *
 * <p><b>The carrier.</b> drr 7.0-7.3 FUNCTION {@code AdjustFrequencyPeriod} -- the reduction
 * below is its REAL source, {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
 * regulation-common-func.rosetta:2555-2569} ({@code func AdjustFrequencyPeriod}), with its ctor
 * type from {@code regulation-common-trade-quantity-type.rosetta:6-8} ({@code type
 * QuantityFrequency}) and its {@code Max3Number} alias from {@code standards-iso-type.rosetta:
 * 68-69} ({@code int(digits: 3)}). The alias is kept, not flattened: the law's rung (b) resolves
 * the attribute's item type through {@code gm.getType(attr)}, so the alias hop is part of the
 * shape under test. Composition class C013, sigs <b>B027 B030 B067 B070</b> = the file's COMPLETE
 * non-import sig set, so all four cells go WHOLE on this law alone (whole ceiling 4).
 *
 * <p><b>The two rungs.</b> (a) {@code ControlFlowHandler}'s seat-28 law-A gate
 * {@code condSingleLadderHasLiteralArm} declined the WHOLE ladder on ANY scalar literal arm; it
 * is narrowed to a literal arm the seat cannot assign bare, and {@code condSingleArmValue} gains
 * the matching per-arm rung -- ONE shared test, {@code condSingleBareLiteralArm}, read by both
 * halves (LAW 69). (b) {@code HandlerHelper.intLiteralConditionalArmExpectedType} walks OUT to
 * the ladder ROOT and reads its workspace inference ({@code number} -> {@code BigDecimal}); it
 * now prefers the CTOR ATTRIBUTE when that root IS a key-value pair's value and the attribute is
 * a SINGLE, meta-NONE {@code Integer}/{@code Long}. Rung (c), the consumer's bare splice, was
 * already built at seat 28 ({@code ConstructionHandler.coerceCtorArg}'s {@code fired} bypass) and
 * is what heals sig B027.
 *
 * <p><b>The measured blast radius (LAW-75 probe at {@code ddcdd151b}, whole matrix, BOTH routes,
 * route-identical).</b> Rung (a): the 9,861-row gate space is four clean buckets and
 * {@code differs=true coercible=true hasLiteralArm=true} is <b>4 rows / 1 distinct site</b>, all
 * {@code fn:AdjustFrequencyPeriod}; the other 971 literal-arm ladders are held out by
 * {@code differs}/{@code coercible}, conjuncts this law does not touch. Positive control:
 * {@code fired=true} is 10 rows over 4 GREEN basenames, so the carrier's {@code fired=false} is a
 * trusted negative. Rung (b): of 422 conditional-arm int literals, 106 verdict {@code BigDecimal},
 * and the carrier's 4 are the ONLY ones rooted under an {@code RKeyValuePair} (the others are 82
 * {@code RInlineFunction} + 20 {@code RShortcut}) -- green blast radius <b>0</b>.
 *
 * <p><b>LAW 74.</b> {@code javac32-report.md} Sec C3: PRE = <b>0</b> errors, MEASURED CLEAN. The
 * fork's pre-law text COMPILES ({@code BigDecimal.intValueExact()} boxes into the
 * {@code Integer} setter through the reference conditional). This is a BYTE law, NOT a compile
 * repair -- no commit message may claim otherwise.
 *
 * <p><b>LAW 77.</b> Both rungs are route-safe WITHOUT an IR twin, and for two different reasons.
 * Rung (a): {@code IRControlFlowHandler extends ControlFlowHandler} and overrides ONLY
 * {@code ifThenElseResultBaseName}, so the gate and the arm renderer INHERIT; {@code rune-ir-java}
 * contains no occurrence of {@code condSingleLadderHasLiteralArm}, {@code CondSingleCoerce} or
 * {@code isScalarLiteral}. Rung (b): the narrowed static keeps its SIGNATURE, so all four readers
 * -- {@code LiteralHandler:153}, {@code IRExpressionCompiler:2921} (which threads the result via
 * {@code ctx.withExpected}), {@code CollectionHandler:10405}, {@code FunctionExpressionRenderer:
 * 3351} -- see the new verdict untouched: route-safe by SHARING. {@code corpus_control2} measures
 * the IR route against GOLDEN, not against legacy, so a route split cannot hide.
 *
 * <p><b>CLAIMED RED at the seat's base head</b> (measured by the chain, not by this file):
 * {@code a1}, {@code a2}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+
 * {@code corpus_control2} under {@code -Pir-on}). {@code e1}, {@code e2}, {@code e3} GREEN in
 * BOTH states -- they are the decline locks and must never move.
 * <b>CLAIMED GREEN at the law's head</b>: 9/0F/1skip default, 9/0F/0skip {@code -Pir-on}.
 *
 * <p><b>MUTATION LANES (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-b2-*.log}; five lanes, each applied -> run -> reverted at
 * the final head):</b>
 * <ul>
 *   <li><b>m-b2-gate</b> (rung (a) reverted -- the blanket {@code isScalarLiteral} decline
 *       restored): MEASURED <b>9/5F/1S</b> = {@code a1}, {@code a2}, {@code corpus_c1},
 *       {@code corpus_c2}, {@code corpus_control1} -- the claim exactly ({@code corpus_control2}
 *       is the {@code -Pir-on} member); every {@code e*} GREEN.</li>
 *   <li><b>m-b2-root</b> (rung (b) reverted -- {@code ctorPairAttributeTypeOrNull} always
 *       {@code null}): MEASURED <b>9/4F/1S</b> = {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 *       {@code corpus_control1}; {@code a2} GREEN as claimed -- the two rungs are BOTH
 *       load-bearing, MEASURED.</li>
 *   <li><b>m-b2-range</b> (the magnitude test dropped): MEASURED <b>9/1F/1S = {@code e3}</b>
 *       EXACTLY; the whole-cell control did not move, as claimed (no corpus carrier has a
 *       beyond-int literal at an int attribute).</li>
 *   <li><b>m-b2-meta</b> (the meta-NONE / {@code isMulti} guards dropped from
 *       {@code ctorPairAttributeTypeOrNull}): MEASURED <b>9/0F/1S -- EMPTY</b>, as declared. The
 *       guards are {@code condSingleCoerceFor}'s, kept so the two halves key the same population
 *       (LAW 69); the measured zero is the adjudication, not a pass.</li>
 *   <li><b>m-b2-355guard</b> (the #355 arm-expected guard dropped -- the lane {@code mut32.py}
 *       lists and this block had not): MEASURED <b>9/0F/1S -- EMPTY</b>, as declared in the
 *       commit note: the carrier is a SINK-path seat and the guard is defence-in-depth; the
 *       measured zero is the adjudication.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code a1}, {@code a2}, {@code corpus_c1},
 * {@code corpus_c2}, {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on});
 * GREEN at the head 9/0F/1skip default, 9/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW-81 tripwires this law FIRES (re-pin in the SAME commit, from their own prints).</b>
 * All four are pins on the pre-law text and every one of them says so itself:
 * <ul>
 *   <li>{@code CtorSetterAttrTypeSeatTest.b3_ctorSetterCondWithScalarLiteralArmDeclinesTheWhole
 *       Ladder} -- the unit-grain replica of this very carrier;</li>
 *   <li>{@code CtorSetterAttrTypeSeatTest.corpus_control5_adjustFrequencyPeriodLiteralArmLadder
 *       Declines} -- "when the banked literal-seat law lands, THIS TEST AND THAT ONE MUST BOTH
 *       FAIL, together";</li>
 *   <li>{@code CtorSetterAttrTypeSeatTest.KNOWN_RESIDUE_DRR7} -- the
 *       {@code AdjustFrequencyPeriod fork=[2, 1, 0, 1] golden=[2, 0, 1, 0]} row is REMOVED;</li>
 *   <li>{@code IntLiteralNumberSeatTest.corpus_c4_...} + its {@code KNOWN_DRR7_RESIDUE}
 *       ({@code fork=3 golden=2} -> the list goes EMPTY; the golden 194/258 pins do NOT move).</li>
 * </ul>
 */
class CtorCondSingleCoerceLiteralArmSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** The carrier, identical in every drr 7.x cell. */
    private static final String CARRIER =
            "drr/regulation/common/functions/AdjustFrequencyPeriod.java";

    /** Cell A = drr 7.0.0 (the control cell too -- the densest ctor-pair cell). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.2.0 -- the second cell of the four. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.2.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixtures -- reduced from the carrier's REAL source (see the class javadoc)
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat32b2
            version "1.0.0"

            typeAlias Max3Number: <"Number (max 999) of objects represented as an integer.">
                int(digits: 3)

            enum FreqPeriodEnum:
                MNTH
                YEAR

            type QuantityFreq:
                period FreqPeriodEnum (0..1)
                periodMultiplier Max3Number (0..1)

            type NumSlot:
                basis int (0..1)
                dcml number (0..1)

            func A1AdjustFrequencyPeriod: <"a1/a2 - the carrier reduced verbatim: an int-attribute ctor pair whose ladder has an INT-LITERAL then arm and a number-typed else arm, beside an enum pair the differs-gate must leave alone">
                inputs:
                    period FreqPeriodEnum (0..1)
                    periodMultiplier number (0..1)
                output:
                    periodValues QuantityFreq (0..1)
                set periodValues:
                    QuantityFreq {
                        period: if period = MNTH and periodMultiplier = 12
                                then YEAR
                            else period,
                        periodMultiplier: if period = MNTH and periodMultiplier = 12
                                then 1
                            else periodMultiplier
                    }

            func E1CtorNumberAttrLiteralArm: <"e1 - the WIDENING direction: a number attribute keeps the literal's BigDecimal.valueOf seat and its BigDecimal local">
                inputs:
                    note string (1..1)
                    dcmlVal number (1..1)
                output:
                    out NumSlot (1..1)
                set out:
                    NumSlot {
                        dcml: if note = "ONE" then 1 else dcmlVal
                    }

            func E2CtorSetterCondNarrowBareArm: <"e2 - the already-firing seat-28 law-A shape (no literal arm at all): bytes unchanged">
                inputs:
                    note string (1..1)
                    basisVal number (1..1)
                output:
                    out NumSlot (1..1)
                set out:
                    NumSlot {
                        basis: if note = "BASIS" then basisVal
                    }

            func E3CtorIntAttrBeyondIntLiteralArm: <"e3 - the magnitude guard: a beyond-int literal at an int attribute keeps the ladder's decline">
                inputs:
                    note string (1..1)
                    basisVal number (1..1)
                output:
                    out NumSlot (1..1)
                set out:
                    NumSlot {
                        basis: if note = "BIG" then 99999999999 else basisVal
                    }
            """;

    // =========================================================================
    // Part A -- the fixture (the law's SHAPE; the corpus compares carry the bytes)
    // =========================================================================

    /**
     * a1 -- the hoisted local declares at the CONSUMER's type and the LITERAL arm assigns BARE
     * there, with no null guard and no in-arm value local. Both halves of the law in one
     * assertion set: the decl is rung (a)'s, the bare {@code = 1;} is rung (b)'s.
     *
     * <p>The decl assertions are deliberately FORM-ROBUST ({@code "Integer ifThenElseResult"}
     * matches both the {@code final <T> xN;} blank-final and the {@code <T> xN = null;}
     * initializer form, and both the bare and the numbered sentinel): which of the two this
     * reduced fixture takes is a #328 seat property, not this law's claim, and the byte-exact
     * work is done by {@code corpus_c1}/{@code corpus_c2}. PIN AT GREEN: tighten these from the
     * fixture's own render at the chain if you want the exact text -- do NOT weaken them.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_intLiteralArmAssignsBareAtTheAttributeTypedLocal() throws IOException {
        String out = fn("A1AdjustFrequencyPeriod.java");
        String code = codeOnly(out);
        assertTrue(code.contains("Integer ifThenElseResult"),
                "the local must declare at the ATTRIBUTE's type (int -> Integer):\n" + out);
        assertTrue(!code.contains("BigDecimal ifThenElseResult"),
                "the local must NOT keep the ladder's walk type (the #355 numeric JOIN):\n" + out);
        assertTrue(hasIteLine(code, "= 1;"),
                "the int-literal arm must assign BARE at the seat type:\n" + out);
        assertTrue(!hasIteLine(code, "= BigDecimal.valueOf(1);"),
                "the literal must NOT take the ladder ROOT's BigDecimal seat -- rung (b):\n" + out);
        assertTrue(!code.contains("final BigDecimal bigDecimal"),
                "a bare literal arm must hoist NO in-arm value local:\n" + out);
    }

    /**
     * a2 -- the other three sigs of the same block: the NON-literal else arm takes the seat-28
     * null-safe conversion, the setter splices the local BARE (the {@code fired} bypass, rung
     * (c)), and the ENUM pair beside it is untouched (its walk EQUALS its attribute type, so the
     * differs-gate declines -- the in-file decline lock).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_elseArmConvertsAndTheSetterSplicesBareWhileTheEnumPairIsUntouched() throws IOException {
        String out = fn("A1AdjustFrequencyPeriod.java");
        String code = codeOnly(out);
        assertTrue(hasIteLine(code,
                        "= periodMultiplier == null ? null : periodMultiplier.intValueExact();"),
                "the bare-input else arm must carry the null-safe narrow IN the arm:\n" + out);
        assertTrue(code.contains(".setPeriodMultiplier(ifThenElseResult"),
                "the setter must splice the local BARE:\n" + out);
        assertTrue(!code.contains(".setPeriodMultiplier((ifThenElseResult"),
                "the setter-side parenthesised narrow must be BYPASSED (rung (c)):\n" + out);
        assertTrue(code.contains("FreqPeriodEnum ifThenElseResult"),
                "the enum pair's local must stay at its own type -- the differs-gate declines "
                + "there and this law must not reach it:\n" + out);
    }

    /**
     * e1 -- THE OVER-FIRE LOCK for rung (b), and the WIDENING direction. A {@code number}
     * attribute makes the walk type and the target both {@code BigDecimal}, so the differs-gate
     * declines at its equality test and the literal legitimately keeps its
     * {@code BigDecimal.valueOf(...)} seat. Bytes unchanged by this law in both states.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_numberAttributeLiteralArmKeepsTheBigDecimalSeat() throws IOException {
        String out = fn("E1CtorNumberAttrLiteralArm.java");
        String code = codeOnly(out);
        assertTrue(code.contains("BigDecimal ifThenElseResult"),
                "a number attribute's local must stay BigDecimal:\n" + out);
        assertTrue(!code.contains("Integer ifThenElseResult"),
                "rung (b) must NOT narrow at a BigDecimal target:\n" + out);
        assertTrue(hasIteLine(code, "= BigDecimal.valueOf(1);"),
                "the literal must keep its BigDecimal seat at a number attribute:\n" + out);
    }

    /**
     * e2 -- the ALREADY-FIRING population (the probe's 10 {@code fired=true} rows over 4 green
     * basenames, e.g. drr 5.61.0 {@code Create_FloatingRate}/{@code GetPackg}): a conditional at
     * the same {@code int} attribute with NO literal arm. Seat 28's law A already fires here and
     * this law must not disturb one byte of it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_theAlreadyFiringNonLiteralLadderIsUntouched() throws IOException {
        String out = fn("E2CtorSetterCondNarrowBareArm.java");
        String code = codeOnly(out);
        assertTrue(code.contains("Integer ifThenElseResult"),
                "the seat-28 law-A shape must still declare at the attribute type:\n" + out);
        assertTrue(hasIteLine(code, "= basisVal == null ? null : basisVal.intValueExact();"),
                "the seat-28 arm conversion must be unchanged:\n" + out);
        assertTrue(code.contains(".setBasis(ifThenElseResult"),
                "the seat-28 bare splice must be unchanged:\n" + out);
    }

    /**
     * e3 -- THE MAGNITUDE GUARD. A beyond-{@code int} literal at an {@code int} attribute cannot
     * be assigned bare at an {@code Integer} slot, so {@code condSingleBareLiteralArm} rejects it
     * and the ladder keeps the seat-28 decline. Without the range test this fixture would emit
     * {@code Integer ifThenElseResult; ... = 99999999999;} -- which does not compile (LAW 74),
     * and no golden carries it. This is the ONLY test the {@code m-b2-range} lane moves.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e3_beyondIntLiteralArmKeepsTheLadderDecline() throws IOException {
        String out = fn("E3CtorIntAttrBeyondIntLiteralArm.java");
        String code = codeOnly(out);
        assertTrue(code.contains("BigDecimal ifThenElseResult"),
                "a beyond-int literal arm must keep the WALK-typed local:\n" + out);
        assertTrue(!code.contains("Integer ifThenElseResult"),
                "the ladder must DECLINE -- the literal cannot compile at an Integer slot:\n"
                + out);
        assertTrue(!hasIteLine(code, "= 99999999999;"),
                "a beyond-int literal must never be assigned bare at an Integer slot:\n" + out);
    }

    // =========================================================================
    // Part B -- the corpus carriers (drr 7.0.0 + drr 7.2.0; 2 of the 4 whole files)
    // =========================================================================

    /** corpus_c1 -- the drr 7.0.0 whole-file heal (sigs B027 B030 B067 B070, all four). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700AdjustFrequencyPeriodMatchesGolden() throws IOException {
        assertNotNull(cellAOutput, "drr 7.0.0 generation did not run");
        List<String> own = cellAGenErrors.stream().filter(e -> e.contains(CARRIER)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CARRIER + ": " + own);
        String gen = cellAOutput.get(CARRIER);
        assertNotNull(gen, "not generated in drr 7.0.0: " + CARRIER);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CARRIER))), normalize(gen),
                "AdjustFrequencyPeriod must byte-match golden - seat 32 law B2: the literal arm "
                + "assigns bare at the attribute-typed local");
    }

    /** corpus_c2 -- the drr 7.2.0 whole-file heal (the same four sigs, second cell). */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr720AdjustFrequencyPeriodMatchesGolden() throws IOException {
        assertNotNull(cellBOutput, "drr 7.2.0 generation did not run");
        String gen = cellBOutput.get(CARRIER);
        assertNotNull(gen, "not generated in drr 7.2.0: " + CARRIER);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(CARRIER))), normalize(gen),
                "AdjustFrequencyPeriod must byte-match golden in drr 7.2.0 - seat 32 law B2");
    }

    // =========================================================================
    // Part C -- the whole-cell UNION control (LAW 79) and the IR route (LAW 77)
    // =========================================================================

    /**
     * The number of GOLDEN files in the drr 7.0.0 cell carrying at least one of the four tokens
     * below. DERIVED by a read-only walk over
     * {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java} --
     * {@code target/seat32-instruments/drafts32/B2/derive-control1-domain.py}, run at
     * {@code ddcdd151b}: 7,808 golden {@code .java} files walked, <b>21</b> token-bearing, golden
     * token totals {@code [T1=18, T2=8, T3=0, T4=3]}.
     *
     * <p>The same script reproduces {@code IntLiteralNumberSeatTest.corpus_control3}'s
     * independently-pinned golden domain EXACTLY (194 files / 258 sites) from the same tree with
     * the same {@code codeOnly} rules -- the instrument's positive control.
     */
    private static final int GOLDEN_DOMAIN_DRR700 = 21;

    /**
     * The per-file token differences this cell is permitted to carry AFTER the law. EMPTY: the
     * ONLY band file in drr 7.0.0 whose quadruple differed was the carrier
     * ({@code fork=[1, 0, 1, 1] golden=[0, 1, 0, 0]}, measured from the seat-31 final OFF dump by
     * the derivation script), and this law closes it. Every non-band file is byte-identical to
     * golden, so it cannot contribute a difference.
     *
     * <p>This is what makes the control two-sided: it FAILS if the law UNDER-fires (the carrier
     * stays in the list) and it FAILS if the law OVER-fires (any other file appears).
     */
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of();

    /**
     * The golden token-bearing files this harness does not emit. EMPTY: all 21 are
     * {@code functions/} or {@code reports/} classes, and the three non-{@code drr/} carriers in
     * the sibling {@code corpus_control3} domain (transitive CDM, e.g.
     * {@code cdm/ingest/fpml/.../MapPrincipalPayment.java}) are emitted by this cell today --
     * that suite's own {@code NOT_EMITTED_DRR7} is empty and passes at HEAD. PIN FROM THE PRINT
     * if the chain says otherwise.
     */
    private static final List<String> NOT_EMITTED_DRR700 = List.of();

    /**
     * corpus_control1 (LAW 79, the UNION whole-cell control) -- the fork's WHOLE generated drr
     * 7.0.0 cell against golden, per file, over the union of the files either side carries one of
     * the four tokens the law moves:
     * <ol>
     *   <li><b>T1</b> -- {@code ifThenElseResult} DECL lines declared {@code BigDecimal} (the walk
     *       type the law moves AWAY from);</li>
     *   <li><b>T2</b> -- the same declared {@code Integer}/{@code Long} (the consumer type it
     *       moves TO);</li>
     *   <li><b>T3</b> -- {@code ((ifThenElseResult} setter-side narrows (the form rung (c)'s
     *       bypass removes);</li>
     *   <li><b>T4</b> -- {@code ifThenElseResultN = BigDecimal.valueOf(...)} arm assignments (the
     *       LITERAL form rung (b) removes -- the dimension the sibling seat-28 control
     *       structurally cannot see, and the reason this control is not a duplicate of it).</li>
     * </ol>
     * Both directions of the law are therefore counted, and the golden side is pinned
     * independently so a scan that silently reads nothing cannot pass.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellIteCoercionShapesEqualGolden() throws IOException {
        assertNotNull(cellAOutput, "drr 7.0.0 generation did not run");
        Map<String, int[]> f = scan(cellAOutput);
        Map<String, int[]> g = scan(readGoldenTree(GOLDEN_A));
        assertEquals(GOLDEN_DOMAIN_DRR700, g.size(),
                "golden drr 7.0.0 files carrying an ifThenElseResult coercion token - the oracle "
                + "pin; if this moved, the golden tree moved and the control must be re-derived");
        List<String> notEmitted = new ArrayList<>();
        List<String> mismatched = new ArrayList<>();
        Set<String> universe = new TreeSet<>(f.keySet());
        universe.addAll(g.keySet());
        int[] zero = new int[4];
        for (String key : universe) {
            if (!cellAOutput.containsKey(key)) {
                notEmitted.add(key);
                continue;
            }
            int[] fc = f.getOrDefault(key, zero);
            int[] gc = g.getOrDefault(key, zero);
            if (!Arrays.equals(fc, gc)) {
                mismatched.add(key + " fork=" + Arrays.toString(fc)
                        + " golden=" + Arrays.toString(gc));
            }
        }
        assertEquals(KNOWN_RESIDUE_DRR700, mismatched,
                "(T1, T2, T3, T4) differ from golden beyond the named residue in "
                + mismatched.size() + " file(s) - an UNDER-fire leaves the carrier here, an "
                + "OVER-fire adds a green file");
        assertEquals(NOT_EMITTED_DRR700, notEmitted,
                "golden token-bearing files this cell does not emit must be exactly the named set");
    }

    /**
     * corpus_control2 (LAW 77) -- the IR route measured against GOLDEN, not against legacy, so a
     * route split cannot hide behind an agreeing pair. Rung (a) inherits
     * ({@code IRControlFlowHandler} overrides only {@code ifThenElseResultBaseName}); rung (b) is
     * SHARED (the static keeps its signature, and {@code IRExpressionCompiler.visitIntLiteral}
     * threads its verdict through {@code ctx.withExpected}). Skips unless the IR provider is on
     * the classpath ({@code -Pir-on}).
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheCarrier() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CARRIER))),
                normalize(irOut.get(CARRIER)), "IR route vs GOLDEN: " + CARRIER);
    }

    // =========================================================================
    // The scan (the CtorSetterAttrTypeSeatTest quadruple, plus the literal dimension)
    // =========================================================================

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            int t4 = 0;
            for (String line : code.split("\n")) {
                String s = line.trim();
                if (isIteDeclLine(s)) {
                    String declType = declaredTypeOf(s);
                    if ("BigDecimal".equals(declType)) {
                        t1++;
                    } else if ("Integer".equals(declType) || "Long".equals(declType)) {
                        t2++;
                    }
                }
                if (s.contains("((ifThenElseResult")) {
                    t3++;
                }
                if (s.startsWith("ifThenElseResult") && s.contains("= BigDecimal.valueOf(")) {
                    t4++;
                }
            }
            if (t1 + t2 + t3 + t4 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3, t4});
            }
        }
        return out;
    }

    /**
     * A DECLARATION of an {@code ifThenElseResult} local, not an assignment to one -- the
     * {@code CtorSetterAttrTypeSeatTest} predicate verbatim (LAW 69: two controls over the same
     * seat must classify a line the same way). Two golden forms:
     * {@code <T> ifThenElseResultN = null;} and {@code final <T> ifThenElseResultN;}. The
     * in-arm assignment {@code ifThenElseResultN = null;} also ends {@code = null;}, so a decl
     * must not START with the name. Literal token work on rendered Java, not structural analysis
     * of language content -- the engineering standard's stated exception.
     */
    private static boolean isIteDeclLine(String s) {
        if (!s.contains("ifThenElseResult") || s.startsWith("ifThenElseResult")) {
            return false;
        }
        if (s.startsWith("final ") && s.endsWith(";") && s.indexOf('=') < 0) {
            return true;
        }
        return s.endsWith("= null;");
    }

    /** The declared type token of a decl line -- the word before the local name. */
    private static String declaredTypeOf(String s) {
        String t = s.startsWith("final ") ? s.substring("final ".length()) : s;
        int nameAt = t.indexOf("ifThenElseResult");
        return nameAt <= 0 ? "" : t.substring(0, nameAt).trim();
    }

    /**
     * An ASSIGNMENT line {@code ifThenElseResultN <suffix>} -- index-robust, because the
     * sentinel's group numbering is a fixture artefact while the assigned SHAPE is the law.
     */
    private static boolean hasIteLine(String code, String suffix) {
        for (String line : code.split("\n")) {
            String s = line.trim();
            if (s.startsWith("ifThenElseResult") && s.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // Fixture harness (the InLambdaBoolHoistShadowSeatTest render(), function lookup)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat32b2.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat32b2".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(main, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fn(String fileName) throws IOException {
        Render r = render();
        String path = "functions/" + fileName;
        List<String> own = r.errors().stream().filter(e -> e.contains(fileName)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return out;
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[CtorCondSingleCoerceLiteralArmSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> cellAOutput;
    private static List<String> cellAGenErrors;
    private static Map<String, String> cellBOutput;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            cellAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            cellAGenErrors = errs;
        }
        if (cellBAvailable()) {
            cellBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.2.0", CELL_B_ROOT),
                    new ArrayList<>());
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    private static Map<String, String> generateCellOnIrRoute(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator");
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static Map<String, String> readGoldenTree(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> {
                try {
                    out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
                } catch (IOException e) {
                    throw new AssertionError("golden read failed: " + p, e);
                }
            });
        }
        return out;
    }

    /** Strip line and block comments plus string literals so a javadoc or label never counts. */
    private static String codeOnly(String java) {
        StringBuilder sb = new StringBuilder(java.length());
        int i = 0;
        int n = java.length();
        while (i < n) {
            char ch = java.charAt(i);
            if (ch == '"') {
                int j = i + 1;
                while (j < n && java.charAt(j) != '"') {
                    if (java.charAt(j) == '\\') {
                        j++;
                    }
                    j++;
                }
                i = j + 1;
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '/') {
                while (i < n && java.charAt(i) != '\n') {
                    i++;
                }
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '*') {
                int e = java.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
