package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * SEAT 33, law A.1 -- facet {@code ctorSetterMetaDerefFunctionHost}: <b>the in-lambda
 * ctor-setter meta deref serves the FUNCTION host too, and it serves a {@code default}-valued
 * pair</b>. THREE RUNGS, ONE COMMIT.
 *
 * <p><b>rung 1 -- the #232 RULE gate dropped.</b> {@code ConstructionHandler
 * .hoistMetaDerefCtorNavInLambdaOrNull} (facet {@code ctorSetterMetaDerefLambda}, PR #312) hoists
 * a wrapper-producing NAVIGATION consumed by a NON-meta single setter into a lambda-local and
 * derefs it null-safely -- but declined outright when {@code HandlerHelper.findEnclosingRule(pair
 * .value()) == null}. That gate was a blast-radius hold, not a discriminator: the RULE path
 * renders golden's exact shape today (the #360 javadoc names drr esma/fca
 * {@code IdentifierOfBasketConstituentsRule}), so the law CONSULTS that seat rather than
 * re-implementing it (LAW 69) and drops one gate.
 * <pre>
 * GOLDEN  final FieldWithMetaString fieldWithMetaString0 = item.&lt;FieldWithMetaString&gt;map("getIdentifier", …).get();
 *         … .setIdentifier((fieldWithMetaString0 == null ? null : fieldWithMetaString0.getValue()))
 * FORK    … .setIdentifier(item.&lt;FieldWithMetaString&gt;map("getIdentifier", …).get())
 * </pre>
 * Sigs <b>B010</b> (the deref) + <b>B026</b>/<b>B011</b> (the renumbers: the new hoist takes
 * index 0 in ctor-FIELD order, shifting the two evaluate-arg derefs to 1 and 2 -- pure
 * consequences, no extra rung, and {@code corpus_c1} is what MEASURES that claim rather than
 * assuming it).
 *
 * <p><b>rung 2 -- the {@code default}-valued sibling.</b> The pool arm's {@code identifier:}
 * value is {@code underlier.FilterAssetIdentifier(item, ISIN) -> identifier default item ->
 * identifier} ({@code base-trade-basket-func.rosetta:75}), a HOMOGENEOUS-meta {@code default}
 * whose collapsed render already IS the item. Golden hoists that render VERBATIM and derefs at
 * the setter:
 * <pre>
 * GOLDEN  final FieldWithMetaString fieldWithMetaString = MapperS.of(filterAssetIdentifier.evaluate(…)).&lt;FieldWithMetaString&gt;map("getIdentifier", …).getOrDefault(item.&lt;FieldWithMetaString&gt;map("getIdentifier", …).get());
 *         … .setIdentifier((fieldWithMetaString == null ? null : fieldWithMetaString.getValue()))
 * FORK    … .setIdentifier(&lt;the whole default chain, spliced&gt;)
 * </pre>
 * Sigs <b>B025</b> + <b>B009</b>. The new arm is
 * {@code hoistMetaDerefCtorDefaultInLambdaOrNull}, called from {@code coerceCtorArg}'s
 * {@code unwrap.isPresent()} fall-through, with the decl value {@code raw} VERBATIM (arm S2's
 * own law: {@code Mapper.getOrDefault(T)} returns the VALUE) and the wrapper taken from render
 * truth first, the operands' recovered JOIN second
 * ({@code NavigationHandler.recoverExprMetaWrapper} + the seat-14 canonical comparator
 * {@code sameWrapperDenotation}, PR #585, promoted to package-private for this consult).
 *
 * <p><b>RUNG 2 WAS RE-SITED BY THE PROBE.</b> The seat-A dossier put it inside the S2
 * {@code default} arm at {@code ConstructionHandler:749-762}. {@code [P33-CTORDEFAULT]}
 * (LAW-75 round 2 at {@code fa49da010}, both routes) measured that arm at <b>80 rows over
 * exactly two {@code where=}</b> -- {@code fn:Create_SubmissionHeader} 40,
 * {@code fn:Create_SubmissionCore} 40 -- and <b>ZERO carrier rows</b>. The positive control
 * fires, so the zero is trusted: the pool arm never reaches S2, because
 * {@code compiledExpr.unwrapToBuilder()} IS present for it and {@code coerceCtorArg} returns
 * from the {@code :513-693} block first. Its fall-through at {@code :643-644} IS the fork's bare
 * splice, and that is where the new arm goes. {@code [P33-DEFJOIN]} confirms the wrapper is
 * knowable there: {@code where=fn:GetBasketConstituents … leftWrap=…FieldWithMetaString
 * rightWrap=…FieldWithMetaString rightChan=stamp sameWrap=true vtEq=true}. <b>Rung 2's declining
 * conjunct at the new seat is UNMEASURED (confidence LOW-MED)</b> -- {@code a2}'s RED and
 * {@code corpus_c1}'s residue lock are the arbiters, and the seat-33 lead sees the verdict on
 * the first run.
 *
 * <p><b>The measured blast radius (rung 1).</b> {@code [P32-CTORHOIST]}, 28,150 rows, BOTH
 * routes, identical group set: {@code encRule=- wrapper=true card=SINGLE sinkNull=true
 * drainable=true attrMeta=NONE} = <b>4 rows / 1 {@code where=}</b> = drr 7.0.0/7.1.0/7.2.0/7.3.0
 * {@code GetBasketConstituents}'s basket arm. The one channel that probe could not see -- the
 * #398 {@code bareItemThenPipeMetaType} recovery for a {@code valueKind=RImplicitVariable} -- has
 * four fn-hosted basenames / 96 rows ({@code Create_NonFinancialInstitutionSector10__1}/{@code __2},
 * {@code Create_ContractType14__1}, {@code GetNrgySpcfcAttrbts}), <b>none a band carrier</b>;
 * {@code corpus_control1} is what holds them.
 *
 * <p><b>rung 3 -- facet {@code ctorSetterHoistTextOrder}: the scoped numbering rule.</b> The
 * first GREEN run with rungs 1+2 alone rendered golden's three hoist lines in golden's ORDER
 * but named {@code fieldWithMetaString2 / 0 / 1} where golden has {@code 0 / 1 / 2} -- the
 * verdict's "emission-order" refuter, live: {@code ConstructionHandler} compiles every pair's
 * value FIRST and coerces in a SECOND loop, so the coercion-phase hoist registers its deferred
 * name AFTER the later siblings' compile-phase evaluate-arg derefs while the #361 window
 * renders it FIRST. {@code [P33-A1ORDER]} probe v2 (per-group, whole 7.0.0 + 7.3.0 cells)
 * REFUTED the general "every group numbers in text order" rule -- three OTHER same-site groups
 * per cell ({@code referenceWithMetaParty} n=3 / n=4, {@code ifThenElseResult} n=2) re-order in
 * files GREEN today -- so the sort is SCOPED: {@code JavaStatementScope
 * .registerDeferredCoercionNameTextOrdered} marks the two hoist registrations, and
 * {@code resolveUnifiedDeferredNames} re-orders exactly the replay groups carrying a mark, in
 * place. Probe v3 (marked-only, same run as this suite's GREEN) measured the radius: exactly
 * two {@code fieldWithMetaString n=3 changed=true} rows (the carrier, both cells) plus two
 * {@code n=2 changed=false} no-op rows -- no other marked group exists, and an unchanged
 * marked group moves zero bytes. {@code corpus_c1}'s pinned renumber texts ARE this rung's
 * lock.
 *
 * <p><b>LAW 80 -- PARTIAL BY DESIGN.</b> {@code GetBasketConstituents} x drr 7.0-7.3 goes WHOLE
 * only when ALL FIVE seat-33 GBC laws land (A.4 -&gt; A.3 -&gt; A.5 -&gt; <b>A.1</b> -&gt; A.2); 13
 * sigs, no proper subset closes it. {@code corpus_c1} therefore pins the EXACT residue at this
 * law's head -- ONE line each way, law A.2's {@code _underliers} lambda name -- and names the law
 * that flips it.
 *
 * <p><b>LAW 74 -- a COMPILE REPAIR.</b> {@code target/seat33-instruments/javac33/}: rung 1
 * repairs row <b>C8b</b>'s {@code Seat33Pre.java:533}
 * {@code incompatible types: FieldWithMetaString cannot be converted to String} and rung 2 row
 * <b>C8c</b>'s {@code :548}, the same message in the pool arm. The remaining C8b/C8c lines are
 * OTHER laws' -- {@code :526}/{@code :527} are A.4's, {@code :537}/{@code :552} are A.3's, and
 * {@code :544} is the enclosing-lambda inference cascade that clears with them -- so this law's
 * OWN claim is <b>PRE 2 -&gt; POST 0</b> and the arms' whole POST-0 is JOINT with A.4 and A.3. The
 * commit message may say "cannot compile" for these two lines and no others.
 *
 * <p><b>LAW 77 -- INHERITS.</b> {@code rune-ir-java}'s {@code IRExpressionCompiler
 * .visitConstructor} is {@code tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitConstructor(
 * expr, ctx))} and the range-correlated {@code ConstructRenderer} is the literal legacy fallback
 * -- same method, same handler instance. {@code grep -rn 'hoistMetaDerefCtor'
 * rune-ir-java/src/main/java} and {@code grep -rn 'sameWrapperDenotation'
 * rune-ir-java/src/main/java} are both EMPTY: there is no route twin. {@code corpus_control2}
 * measures it rather than asserting it.
 *
 * <p><b>CLAIMED RED at this law's head-of-branch (both routes):</b> {@code a1}, {@code a2},
 * {@code corpus_c1}, {@code corpus_control1} (+ {@code corpus_control2} under {@code -Pir-on}).
 * {@code e1} and {@code e2} are GREEN at RED and must never invert -- they are the decline locks.
 * <b>CLAIMED GREEN after the law:</b> 8/0F/1skip default, 8/0F/0skip under {@code -Pir-on}.
 * (CLAIMED -- measured by the chain.)
 *
 * <p><b>MUTATION LANES (LAW 66/76) -- CLAIMED, adjudicated by the chain (LAW 82):</b>
 * <ul>
 *   <li><b>m-lawA1-rulegate</b> ({@code CN_PAIRS_MUT_RULEGATE}: the #232 rule gate RESTORED --
 *       rung 1 severed): CLAIMED {@code a1}, {@code corpus_c1}, {@code corpus_control1} fail;
 *       {@code a2}, {@code e1}, {@code e2} GREEN. LAW 76: the whole-cell control MOVES.</li>
 *   <li><b>m-lawA1-defaulthoist</b> ({@code CN_PAIRS_MUT_DEFAULTHOIST}: rung 2's dispatch
 *       severed at the call site): CLAIMED {@code a2}, {@code corpus_c1},
 *       {@code corpus_control1} fail; {@code a1}, {@code e1}, {@code e2} GREEN. LAW 76: the
 *       whole-cell control MOVES.</li>
 *   <li><b>m-lawA1-vteq</b> ({@code CN_PAIRS_MUT_VTEQ}: rung 2's value-type-equality gate --
 *       the CALLER-side gate {@code recoverExprMetaWrapper}'s own javadoc mandates -- severed):
 *       DECLARED as a defence-in-depth adjudication. At this corpus the failing set may be
 *       EMPTY (every wrapper the admitted population recovers already unwraps to the setter's
 *       bare type). The chain's measurement decides; nothing here claims it passes.</li>
 *   <li><b>m-lawA1-textorder</b> ({@code JSS_PAIRS_MUT_TEXTORDER}: rung 3's scoped
 *       text-order sort severed -- counter order kept): CLAIMED {@code corpus_c1} and
 *       {@code corpus_c2} fail on the wrapper-local NAMES ({@code 2/0/1} against golden's
 *       {@code 0/1/2}); {@code a1}, {@code a2}, {@code e1}, {@code e2} GREEN (they assert
 *       hoist/deref presence, not numbering) and {@code corpus_control1} GREEN DECLARED (the
 *       whole-cell tuple scan is name-blind -- LAW 76 is satisfied by the corpus-anchored
 *       residue locks moving, not the shape control). The chain's measurement decides.</li>
 * </ul>
 *
 * <p><b>LAW-81 TRIPWIRES this law FIRES in OTHER suites</b> (do not edit them from here -- the
 * lead re-pins each from its OWN failing print, in this law's commit). Every row below was
 * re-scored by replaying that suite's own scan over the staged carrier texts
 * ({@code target/seat33-instruments/drafts33/A1/NOTES.md} names the walk); the arrow is the
 * MEASURED post-law tuple:
 * <ul>
 *   <li>{@code ChoiceOptionProjectionTypeIdSeatTest.KNOWN_RESIDUE_700} --
 *       {@code GetBasketConstituents.java fork=[1, 1, 2] golden=[1, 1, 4]} HEALS (T3
 *       {@code == null ? null :} +2) and leaves the list;</li>
 *   <li>{@code CondArmMultiMetaElementDerefSeatTest} -- {@code fork=[1, 2, 1] golden=[1, 3, 1]}
 *       HEALS (T2, the {@code final …WithMeta… .get();} decl +1);</li>
 *   <li>{@code DefaultJoinDerefAtCollapsedLeftSeatTest} -- {@code fork=[0, 1, 2] golden=[0, 1, 4]}
 *       HEALS (T3 {@code final FieldWithMeta}/{@code final ReferenceWithMeta} +2);</li>
 *   <li>{@code DefaultMetaJoinSeatTest} -- {@code fork=[0, 1, 2] golden=[0, 1, 4]} HEALS (same
 *       scan);</li>
 *   <li>{@code FilterPredicateMetaDerefSeatTest} -- {@code fork=[0, 0, 2] golden=[0, 0, 4]}
 *       HEALS (T3 {@code == null ? null :} +2);</li>
 *   <li>{@code SetTerminalRuleMultiSeatTest} -- {@code fork=[0, 0, 7] golden=[0, 1, 7]} HEALS
 *       (T2 {@code .get());} +1, rung 2's hoisted decl line).</li>
 * </ul>
 * The other nine suites pinning a {@code GetBasketConstituents.java} row are INVARIANT under
 * this law's token deltas and are expected NOT to move.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 8/0F/1skip default (f33-green-default.log) /
 * 8/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 5F = a1, a2, corpus_c1, corpus_c2, corpus_control1; {@code -Pir-on} 6F = a1, a2, corpus_c1, corpus_c2, corpus_control1, corpus_control2 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawA1-rulegate}</b> ({@code CN_PAIRS_MUT_RULEGATE}): MEASURED 8/4F/1skip = a1, corpus_c1, corpus_c2, corpus_control1 - RE-SCORED (superset): c2 fell as well as the claimed a1, c1, control1 - the drr 7.3.0 twin the claim omitted; a2, e1, e2 held.</li>
 *   <li><b>{@code m-lawA1-defaulthoist}</b> ({@code CN_PAIRS_MUT_DEFAULTHOIST}): MEASURED 8/4F/1skip = a2, corpus_c1, corpus_c2, corpus_control1 - RE-SCORED (superset): c2 fell as well as the claimed a2, c1, control1 - the same omitted twin; a1, e1, e2 held.</li>
 *   <li><b>{@code m-lawA1-vteq}</b> ({@code CN_PAIRS_MUT_VTEQ}): MEASURED 8/0F/1skip = (none) - EMPTY-as-declared (defence-in-depth: every wrapper hoist at this corpus has an equal value type).</li>
 *   <li><b>{@code m-lawA1-textorder}</b> ({@code JSS_PAIRS_MUT_TEXTORDER}): MEASURED 8/2F/1skip = corpus_c1, corpus_c2 - MATCH (c1, c2 on the names; control1 held as declared - the tuple scan is name-blind).</li>
 * </ul>
 */
class CtorSetterMetaDerefFunctionHostSeatTest {

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

    /** The carrier, identical in drr 7.0.0 / 7.1.0 / 7.2.0 / 7.3.0. */
    private static final String GBC = "drr/base/trade/basket/functions/GetBasketConstituents.java";

    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
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
    // The fixture -- reduced from the REAL carrier source
    // (test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
    //  base-trade-basket-func.rosetta:52-56 = the basket arm's ctor, and :74-76
    //  = the pool arm's `default`-valued ctor)
    // =========================================================================

    /**
     * {@code IdentA} mirrors {@code cdm.base.staticdata.asset.common.AssetIdentifier}: a
     * {@code [metadata scheme]} string leaf beside a plain enum leaf. That PAIR is the whole
     * discriminator, and the corpus carrier ships it inside ONE constructor -- the
     * {@code identifier:} field fires and the {@code source:} field must not -- so each
     * fixture carries its own negative control in the same rendered method.
     *
     * <ul>
     *   <li><b>a1</b> = rung 1: {@code idents extract RepA { identifier: item -> identifier, … }}
     *       -- a wrapper-producing NAVIGATION consumed by a non-meta {@code string} setter,
     *       inside a drainable map/extract lambda, hosted by a <b>func</b> (which is exactly
     *       what the dropped #232 gate refused). No statement-hoist sink is reachable in a
     *       lambda interior, so the #236 method-level arm declines and this is the #312 seat.</li>
     *   <li><b>a2</b> = rung 2: the same shape with the {@code default}-valued pair the pool arm
     *       carries. Both operands navigate to the SAME {@code [metadata scheme]} leaf, so the
     *       join is homogeneous -- the shape {@code SetOperationHandler:430-433} names by name
     *       as the class that KEEPS the wrapper, which is why the deref belongs at the setter.</li>
     *   <li><b>e1</b> = the decline lock on a conjunct the law LEAVES: the setter is itself
     *       {@code [metadata scheme]}-annotated, so it takes the wrapper and there is nothing to
     *       deref. The assertions are shape-free (NO hoist, NO guarded deref) so they cannot
     *       accidentally encode a meta-setter naming choice.</li>
     *   <li><b>e2</b> = the decline lock on rung 2's LAMBDA_CHANNEL gate: the identical
     *       {@code default}-valued ctor at a TOP-LEVEL {@code set}, where a statement-hoist sink
     *       IS reachable. The new arm must decline and the bare splice must survive.</li>
     * </ul>
     */
    private static final String MODEL = """
            namespace census.seat33a1
            version "1.0.0"

            enum IdTypeEnumA:
                ISIN
                CUSIP

            type IdentA:
                identifier string (1..1)
                    [metadata scheme]
                identifierType IdTypeEnumA (1..1)

            type RepA:
                identifier string (0..1)
                source IdTypeEnumA (0..1)

            type RepMetaA:
                identifier string (0..1)
                    [metadata scheme]
                source IdTypeEnumA (0..1)

            func PickIdentA:
                inputs:
                    xs IdentA (0..*)
                    k IdTypeEnumA (1..1)
                output:
                    picked IdentA (0..1)
                set picked:
                    xs only-element

            func A1CtorNavMetaDerefInFunction: <"a1 - rung 1: THE GetBasketConstituents BASKET-ARM SHAPE - a wrapper-producing nav into a non-meta setter, inside an extract lambda, hosted by a FUNC (the #232 gate's whole population)">
                inputs:
                    idents IdentA (0..*)
                output:
                    result RepA (0..*)
                add result:
                    idents
                        extract RepA {
                            identifier: item -> identifier,
                            source: item -> identifierType
                        }

            func A2CtorDefaultMetaDerefInFunction: <"a2 - rung 2: THE GetBasketConstituents POOL-ARM SHAPE - a homogeneous-meta `default` ctor value inside the same extract lambda; the enum sibling is the negative half of the pair">
                inputs:
                    idents IdentA (0..*)
                output:
                    result RepA (0..*)
                add result:
                    idents
                        extract RepA {
                            identifier: PickIdentA(idents, ISIN) -> identifier default item -> identifier,
                            source: PickIdentA(idents, ISIN) -> identifierType default item -> identifierType
                        }

            func E1MetaAnnotatedSetterDeclines: <"e1 - the DECLINE LOCK: the setter is itself [metadata scheme]-annotated, so it takes the WRAPPER - no hoist, no guarded deref, bytes unchanged">
                inputs:
                    idents IdentA (0..*)
                output:
                    result RepMetaA (0..*)
                add result:
                    idents
                        extract RepMetaA {
                            identifier: item -> identifier,
                            source: item -> identifierType
                        }

            func E2TopLevelDefaultDeclines: <"e2 - the DECLINE LOCK on rung 2's LAMBDA_CHANNEL gate: the identical `default` ctor at a TOP-LEVEL set, where a statement-hoist sink IS reachable - the new arm must decline and the bare splice must survive">
                inputs:
                    idents IdentA (0..*)
                    one IdentA (1..1)
                output:
                    result RepA (0..1)
                set result:
                    RepA {
                        identifier: PickIdentA(idents, ISIN) -> identifier default one -> identifier,
                        source: PickIdentA(idents, ISIN) -> identifierType default one -> identifierType
                    }
            """;

    /** The hoisted wrapper local golden declares (rung 1 mints it inside the lambda). */
    private static final String NAV_HOIST =
            "final FieldWithMetaString fieldWithMetaString = item.<FieldWithMetaString>map(";
    /** The hoisted wrapper local golden declares for the collapsed `default` (rung 2). */
    private static final String DEFAULT_HOIST =
            "final FieldWithMetaString fieldWithMetaString = MapperS.of(pickIdentA.evaluate(";
    /** Golden's guarded deref at the setter -- the token BOTH rungs produce. */
    private static final String GUARDED_DEREF = ".setIdentifier((fieldWithMetaString == null"
            + " ? null : fieldWithMetaString.getValue()))";
    /** The fork's rung-1 bare splice. */
    private static final String NAV_BARE = ".setIdentifier(item.<FieldWithMetaString>map(";
    /** The fork's rung-2 bare splice (the whole collapsed default in the setter). */
    private static final String DEFAULT_BARE = ".setIdentifier(MapperS.of(pickIdentA.evaluate(";

    // =========================================================================
    // Part A -- the reduced fixtures (unit grain)
    // =========================================================================

    /**
     * a1 -- rung 1's heal. Every assert fails PRE-law for THIS law's reason: the #232 gate
     * declines the FUNCTION host, so no local is hoisted and the wrapper is spliced bare into a
     * {@code String} setter. The {@code source} setter is the in-fixture negative control -- it
     * is an enum with no metadata, must never gain a hoist, and pins that the law keys on the
     * WRAPPER and not on "any ctor setter".
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_functionHostedCtorNavMetaValue_hoistsAndDerefsInTheLambda() throws IOException {
        String out = fixtureFunction("A1CtorNavMetaDerefInFunction");
        assertContains(out, NAV_HOIST);
        assertContains(out, GUARDED_DEREF);
        assertTrue(!out.contains(NAV_BARE),
                "the fork's bare wrapper splice must be gone:\n  " + NAV_BARE + "\nin:\n" + out);
        assertEquals(1, count(out, "final FieldWithMetaString"),
                "exactly ONE wrapper local - the enum sibling must not hoist:\n" + out);
        assertContains(out, ".setSource(item.<IdTypeEnumA>map(");
        assertEquals(1, count(out, "== null ? null :"),
                "exactly ONE guarded deref - the enum sibling stays a plain splice:\n" + out);
    }

    /**
     * a2 -- rung 2's heal. PRE-law the whole collapsed {@code default} is spliced into the
     * {@code String} setter ({@code DEFAULT_BARE}); the law hoists that render VERBATIM -- no
     * {@code .get()} appended, because {@code Mapper.getOrDefault(T)} already returns the value --
     * and derefs at the setter. The {@code source} pair is the in-fixture negative control: an
     * enum {@code default} recovers no wrapper on either operand and must keep its inline splice.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_functionHostedCtorDefaultMetaValue_hoistsTheCollapsedRenderVerbatim()
            throws IOException {
        String out = fixtureFunction("A2CtorDefaultMetaDerefInFunction");
        assertEquals(1, count(out, DEFAULT_HOIST),
                "the collapsed default must be hoisted ONCE into the lambda:\n" + out);
        assertContains(out, GUARDED_DEREF);
        assertTrue(!out.contains(DEFAULT_BARE),
                "the fork's whole-chain splice must be gone:\n  " + DEFAULT_BARE + "\nin:\n" + out);
        String decl = lineContaining(out, DEFAULT_HOIST);
        assertNotNull(decl, "the hoisted decl line must exist:\n" + out);
        assertTrue(decl.contains(".getOrDefault("),
                "what is hoisted IS the collapsed `default` render:\n  " + decl);
        assertTrue(!decl.endsWith(").get();"),
                "the decl value is `raw` VERBATIM - `Mapper.getOrDefault(T)` already returns the"
                        + " VALUE, so no `.get()` collapse may be appended (the appended form"
                        + " ends `).get();`):\n  " + decl);
        assertEquals(1, count(out, "final FieldWithMetaString"),
                "exactly ONE wrapper local - the enum `default` sibling must not hoist:\n" + out);
        assertContains(out, ".setSource(MapperS.of(pickIdentA.evaluate(");
        assertEquals(1, count(out, "== null ? null :"),
                "exactly ONE guarded deref - the enum `default` sibling stays inline:\n" + out);
    }

    /**
     * e1 -- the decline lock on the conjunct the law LEAVES IN PLACE: a {@code [metadata scheme]}
     * setter takes the wrapper itself, so there is nothing to deref. Asserted shape-free (NO
     * wrapper local, NO guarded deref) so the lock cannot accidentally encode which meta-setter
     * name the ctor path chooses.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_metaAnnotatedSetter_gainsNoHoistAndNoDeref() throws IOException {
        String out = fixtureFunction("E1MetaAnnotatedSetterDeclines");
        assertTrue(out.contains(".set"),
                "PREMISE: the ctor must render at least one setter, else the lock is vacuous"
                        + " (shape-free on purpose - no meta-setter name is encoded):\n" + out);
        assertEquals(0, count(out, "final FieldWithMetaString"),
                "a meta-annotated setter must gain NO wrapper local:\n" + out);
        assertEquals(0, count(out, "== null ? null :"),
                "a meta-annotated setter must gain NO guarded deref:\n" + out);
    }

    /**
     * e2 -- the decline lock on rung 2's LAMBDA_CHANNEL gate. The identical
     * {@code default}-valued ctor pair at a TOP-LEVEL {@code set}: a statement-hoist sink IS
     * reachable there, so {@code hoistMetaDerefCtorDefaultInLambdaOrNull} must decline (its
     * {@code findStatementHoistSink() != null} gate) and the bare splice must survive. This is
     * the test that fails if the new arm is written without its channel gate.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_topLevelDefaultCtorValue_keepsTheBareSplice() throws IOException {
        String out = fixtureFunction("E2TopLevelDefaultDeclines");
        assertContains(out, DEFAULT_BARE);
        assertEquals(0, count(out, "final FieldWithMetaString"),
                "no sink-reachable seat may gain the in-lambda wrapper local:\n" + out);
        assertEquals(0, count(out, "== null ? null :"),
                "no sink-reachable seat may gain the guarded deref:\n" + out);
    }

    // =========================================================================
    // Part B -- the corpus (the PARTIAL residue pin + the LAW-79 union control)
    // =========================================================================

    /**
     * corpus_c1 -- {@code GetBasketConstituents} x drr 7.0.0: <b>IMPROVED, NOT WHOLE at this
     * law's head</b> (LAW 80 -- disclosed, not re-scoped); this law owns 5 of the file's 15 hunks
     * (B010 B026 B011 · B025 B009). <b>WHOLE since A.2</b>: the byte compare below was re-pinned
     * at CHECKPOINT 1 ({@code 7e94f2d16}, the commit AFTER A.2's {@code ad9f0ce87}, from
     * {@code ckpt1-gensuite.log}) -- not in A.2's own commit as the paragraph below predicted.
     *
     * <p><b>This is also the RENUMBER measurement.</b> The seat-A verdict flagged the
     * "B026/B011 come free" claim as UNMEASURED: it assumes hoists are allocated in ctor-FIELD
     * order, so rung 1's new hoist takes index {@code 0} and shifts the two evaluate-arg derefs
     * to {@code 1} and {@code 2}. The three exact decl/consumer texts below are pinned, so if
     * the allocator is reached in EMISSION order instead (the new hoist taking index {@code 2}),
     * this test fails and the commit message may not claim five sigs.
     *
     * <p>The residue at this law's head is <b>ONE line each way</b> -- law A.2's
     * ({@code aliasCondLadderChoiceJoin}) {@code _underliers} lambda name on the
     * {@code thenArg0} decl. When A.2 landed, THIS TEST became a whole-file byte compare -- one
     * commit later than predicted, at CHECKPOINT 1 (the LAW-81 tripwire fired in the checkpoint's
     * gensuite, not in A.2's own batch).
     *
     * <p><b>ORDER-COUPLED</b> to the charter's GBC order A.4 -&gt; A.3 -&gt; A.5 -&gt; A.1: the
     * residue counts assume the first three have landed. If any lands differently the pin is
     * re-taken FROM THIS TEST'S OWN PRINT in this law's commit.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700GetBasketConstituentsCtorSettersDerefTheRestDisclosed()
            throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(GBC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + GBC + ": " + own);
        String fork = normalize(drrAOutput.get(GBC));
        assertNotNull(fork, "not generated: " + GBC);
        String golden = normalize(Files.readString(GOLDEN_A.resolve(GBC)));

        // --- rung 1: the basket arm's deref AND the renumbers, exactly ---------------------
        assertContains(fork, ".setIdentifier((fieldWithMetaString0 == null ? null :"
                + " fieldWithMetaString0.getValue()))");
        assertContains(fork, "final FieldWithMetaString fieldWithMetaString2 = item."
                + "<FieldWithMetaString>map(\"getIdentifier\", assetIdentifier ->"
                + " assetIdentifier.getIdentifier()).get();");
        assertContains(fork, "(fieldWithMetaString1 == null ? null :"
                + " fieldWithMetaString1.getValue()), tradeLots(trade).getMulti())).<BigDecimal>");
        assertContains(fork, "(fieldWithMetaString2 == null ? null :"
                + " fieldWithMetaString2.getValue()), tradeLots(trade).getMulti())).<UnitType>");
        assertTrue(!fork.contains(".setIdentifier(item.<FieldWithMetaString>map("),
                "the fork's basket-arm bare splice must be gone:\n" + fork);

        // --- rung 2: the pool arm's hoist + deref ------------------------------------------
        // golden's decl line VERBATIM: the collapsed `default` render with NO appended .get()
        assertContains(fork, "final FieldWithMetaString fieldWithMetaString = MapperS.of("
                + "filterAssetIdentifier.evaluate(item.getMulti(), AssetIdTypeEnum.ISIN))"
                + ".<FieldWithMetaString>map(\"getIdentifier\", assetIdentifier ->"
                + " assetIdentifier.getIdentifier()).getOrDefault(item.<FieldWithMetaString>map("
                + "\"getIdentifier\", assetIdentifier -> assetIdentifier.getIdentifier()).get());");
        assertContains(fork, ".setIdentifier((fieldWithMetaString == null ? null :"
                + " fieldWithMetaString.getValue()))");
        assertTrue(!fork.contains(".setIdentifier(MapperS.of(filterAssetIdentifier.evaluate("),
                "the fork's pool-arm whole-chain splice must be gone:\n" + fork);
        // the pool arm's ENUM sibling is the negative control and keeps its inline splice
        assertContains(fork, ".setSource(MapperS.of(filterAssetIdentifier.evaluate(");

        // --- the residue is CLOSED: law A.2 landed and the file is WHOLE -------------------
        // LAW 81 re-pin (v3.1 flip seat 33, A.2 / CHECKPOINT 1): the disclosed 1/1
        // `_underliers` residue left when aliasCondLadderChoiceJoin renamed the alias-rooted
        // hop's lambda - this test is now the whole-file byte compare its javadoc promised;
        // transcribed from the checkpoint's own gensuite print (ckpt1-gensuite.log, 1 -> 0).
        List<String> goldenLines = List.of(golden.split("\n"));
        List<String> forkLines = List.of(fork.split("\n"));
        List<String> goldenOnly = new ArrayList<>(goldenLines);
        goldenOnly.removeAll(forkLines);
        List<String> forkOnly = new ArrayList<>(forkLines);
        forkOnly.removeAll(goldenLines);
        assertEquals(0, goldenOnly.size(),
                "GetBasketConstituents must be WHOLE after A.2: " + goldenOnly);
        assertEquals(0, forkOnly.size(),
                "GetBasketConstituents must be WHOLE after A.2: " + forkOnly);
        assertEquals(golden, fork, "the whole-file byte compare (drr 7.0.0)");
    }

    /**
     * corpus_c2 -- the CROSS-CELL half: the four cells' generated
     * {@code GetBasketConstituents.java} are ONE byte string
     * ({@code md5 634e3137d981058e1c058d62c77eb48b} across 7.0/7.1/7.2/7.3 on BOTH routes,
     * verified from the seat-33 probe dumps), so one fix heals four cells. This test asserts
     * both rungs in the SECOND cell so a cell-scoped over/under-fire cannot hide.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730GetBasketConstituentsCtorSettersDeref() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        List<String> own = drrBGenErrors.stream().filter(e -> e.contains(GBC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + GBC + " (drr 7.3.0): " + own);
        String fork = normalize(drrBOutput.get(GBC));
        assertNotNull(fork, "not generated (drr 7.3.0): " + GBC);
        assertContains(fork, ".setIdentifier((fieldWithMetaString0 == null ? null :"
                + " fieldWithMetaString0.getValue()))");
        assertContains(fork, ".setIdentifier((fieldWithMetaString == null ? null :"
                + " fieldWithMetaString.getValue()))");
        assertEquals(4, count(fork, "final FieldWithMetaString"),
                "three basket-arm locals + one pool-arm local, exactly as golden:\n" + fork);
        String golden = normalize(Files.readString(GOLDEN_B.resolve(GBC)));
        assertEquals(count(golden, "final FieldWithMetaString"),
                count(fork, "final FieldWithMetaString"),
                "the wrapper-local COUNT must equal golden's in the cross cell too");
    }

    /**
     * corpus_control1 -- LAW 79, the UNION whole-cell control over drr 7.0.0.
     *
     * <p>Per file, over a CODE-ONLY view (string literals and both comment forms blanked -- the
     * {@code FilterPredicateMetaDerefSeatTest} walk, so generated Java text inside a literal can
     * never be counted as a real site), the tuple is <b>(T1, T2, T3)</b>:
     * <ul>
     *   <li><b>T1</b> -- {@code final …WithMeta… = …;} locals: every meta-wrapper hoist in the
     *       file, whoever minted it (this law ADDS two at the carrier);</li>
     *   <li><b>T2</b> -- setter lines carrying a GUARDED deref
     *       ({@code == null ? null :} … {@code .getValue())}): golden's consuming form, which
     *       this law ADDS twice;</li>
     *   <li><b>T3</b> -- setter lines splicing a WRAPPER bare (a {@code .set…} line naming a
     *       {@code WithMeta} type with no guarded deref): the fork's defect, which this law
     *       REMOVES twice.</li>
     * </ul>
     * T3 is the over-fire net in both directions: a green file that LOSES a legitimate bare
     * wrapper splice, or GAINS a hoist it should not have, fails the row set.
     *
     * <p><b>The domain</b> was DERIVED by a read-only walk of
     * {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java}
     * ({@code target/seat33-instruments/drafts33/A1/domain-walk.py}): <b>412</b> goldens carry a
     * non-zero tuple, every one of them a {@code .../functions/} or {@code .../reports/} output
     * (0 under {@code .../validation/datarule/}), so the {@code retainAll(emitted)} intersection
     * is a no-op; and no band file carries a non-zero FORK tuple over an all-zero golden, so
     * there are no fork-side additions. Re-pin from this control's OWN print if it moves.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_drr700WholeCellMetaDerefShapeEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_700, DOMAIN_DRR700);
    }

    /** MEASURED by {@code domain-walk.py} at {@code fa49da010} (see the control's javadoc). */
    private static final int DOMAIN_DRR700 = 412;

    /**
     * The NAMED residue of drr 7.0.0 after law A.1 (LAW 73: pin the SET, not the count). Neither
     * row is this law's -- both are the {@code QuantityUnitOfMeasure} / {@code TotalNotionalQuantity}
     * families chartered for laws B.1/B.2/B.3 and D.3 LATER in this seat. When those land the
     * rows heal out and the list is re-pinned in THEIR commits, from this control's own print.
     *
     * <p>At the RED head the list carries a THIRD row --
     * {@code drr/base/trade/basket/functions/GetBasketConstituents.java fork=[4, 1, 2]
     * golden=[6, 3, 0]} -- and that row is exactly this law's heal (T1 +2, T2 +2, T3 -2).
     *
     * <p><b>ORDER NOTE.</b> A FOURTH row,
     * {@code drr/enrichment/common/test/functions/Enrich_TransactionReportInstructionTestPackDefault.java
     * fork=[2, 0, 0] golden=[2, 0, 4]}, is present at the seat-33 BASE and is expected to have
     * healed at charter row #6 (law E.234, {@code ctorAsKeyBareValueReference} et al), which
     * lands FOUR commits before this one. If E.234 has not landed, or left residue, this control
     * fails with that row and it is re-pinned here from the print -- do NOT weaken the control.
     */
    private static final List<String> KNOWN_RESIDUE_700 = List.of(
            // the QuantityUnitOfMeasure row (fork=[7, 0, 0] golden=[14, 0, 0]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth + iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) took QUOM WHOLE in all four drr 7.x cells - the band's last four files;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            // the TotalNotionalQuantity row (fork=[6, 0, 0] golden=[2, 0, 0]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit, seven rungs) took TNQ WHOLE in all four drr 7.x cells;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            );

    /**
     * corpus_control2 -- LAW 77, the ROUTE gate. {@code rune-ir-java} carries no
     * {@code hoistMetaDerefCtor*} and no {@code sameWrapperDenotation};
     * {@code IRExpressionCompiler.visitConstructor} falls back to {@code super.visitConstructor}
     * on the same handler instance. Because the file is IMPROVED-not-whole under this law, the
     * assertion is ROUTE IDENTITY (the IR text must equal the legacy text byte for byte) PLUS
     * both rungs' own tokens. Skips unless {@code -Pir-on}.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteRendersTheSameCtorDerefs() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        String ir = normalize(irOut.get(GBC));
        assertNotNull(ir, "IR route did not generate: " + GBC);
        assertContains(ir, ".setIdentifier((fieldWithMetaString0 == null ? null :"
                + " fieldWithMetaString0.getValue()))");
        assertContains(ir, ".setIdentifier((fieldWithMetaString == null ? null :"
                + " fieldWithMetaString.getValue()))");
        assertEquals(normalize(drrAOutput.get(GBC)), ir,
                "LAW 77: the IR route must render the SAME text as the legacy route for the"
                        + " IMPROVED-not-whole carrier " + GBC);
    }

    // =========================================================================
    // The scan + the union assert (the FilterPredicateMetaDerefSeatTest shape)
    // =========================================================================

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = new int[3];
            for (String line : codeOnly(normalize(e.getValue())).split("\n")) {
                String s = line.trim();
                if (s.startsWith("final ") && s.contains("WithMeta") && s.contains(" = ")) {
                    t[0]++;
                }
                if (s.startsWith(".set")) {
                    if (s.contains("== null ? null :") && s.contains(".getValue())")) {
                        t[1]++;
                    } else if (s.contains("WithMeta")) {
                        t[2]++;
                    }
                }
            }
            if (t[0] + t[1] + t[2] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[3];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted tuple-bearing files (" + expectedDomain
                        + ")");
    }

    /**
     * A CODE-ONLY view: string literals and both comment forms removed, newlines preserved so
     * the line walk still computes. A character walk, not a pattern match, and it performs no
     * structural analysis of the generated language content -- only a literal-token scan of the
     * fork's own just-emitted output.
     */
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

    private static int count(String haystack, String needle) {
        int n = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    /** The FIRST trimmed line of {@code out} containing {@code token}, or {@code null}. */
    private static String lineContaining(String out, String token) {
        for (String line : out.split("\n")) {
            if (line.contains(token)) {
                return line.trim();
            }
        }
        return null;
    }

    // =========================================================================
    // Fixture harness (the IteChainCtorArmMapperWrapSeatTest helpers, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat33a1.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33a1".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fixtureFunction(String fnName) throws IOException {
        Render r = render();
        String path = fnName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith(path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return normalize(out);
    }

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
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
            throw new AssertionError("[CtorSetterMetaDerefFunctionHostSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
