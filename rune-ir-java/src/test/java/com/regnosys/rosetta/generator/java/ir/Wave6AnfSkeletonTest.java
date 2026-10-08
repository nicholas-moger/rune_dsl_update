package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.Let;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.Optionality;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.ir.expr.anf.ANFExpr;
import com.regnosys.rosetta.ir.expr.anf.Bind;
import com.regnosys.rosetta.ir.expr.anf.Block;
import com.regnosys.rosetta.ir.expr.anf.ConditionalHoist;
import com.regnosys.rosetta.ir.expr.anf.JoinPoint;
import com.regnosys.rosetta.ir.expr.anf.Normalize;
import com.regnosys.rosetta.ir.expr.anf.ThenChainHoist;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase-A offline validation of the Wave-6 ANF hoist substrate (design §5; decision-log L-056). Each test runs
 * the REAL pipeline on a real corpus carrier — load → find function → {@code adaptConditionalForHarness} →
 * {@code Normalize.conditionalToBind} → {@link AnfSkeletonRenderer} — and byte-diffs the rendered hoist
 * <strong>skeleton</strong> (temp name/number/order + declared type + the declare-then-assign shape) against
 * the maintainer's Q3 hoist-trace dump ({@code handover/q3-hoist-trace-dump.md}). Branch interior bytes are
 * masked ({@code __COND__}/{@code __THEN__}/{@code __VALUE__}) — they depend on render-scope state and are the
 * live-gate ("C") obligation; what is validated here is reproducible offline.
 * <ul>
 *   <li><strong>Slice 1</strong> ({@link #conditionalHoistSkeletonMatchesDumpSection1_1}) — the statement-position
 *       conditional {@code ifThenElseResult} hoist (dump §1.1).</li>
 *   <li><strong>Slice 2</strong> ({@link #booleanRenderOrderReorderMatchesDumpSection6}) — the cross-group
 *       render-walk reorder ({@code boolean} decl before the {@code ifThenElseResult} it guards, dump §6).</li>
 *   <li><strong>Slice 3</strong> ({@link #thenChainHoistSingleMatchesDumpSection5_1} /
 *       {@link #thenChainHoistNestedMatchesDumpSection5_2} / {@link #normalizeOfLetChainFlattensToThenArgs}) — the
 *       {@code then}-chain {@code thenArg} hoist: a single bare {@code thenArg} (dump §5.1), the nested
 *       {@code thenArg0..3} re-rooting chain (dump §5.2), and the {@code normalize(Let)} flatten.</li>
 * </ul>
 *
 * <h2>Carrier — {@code ConvertToAdjustableOrRelativeDate} (cdm6)</h2>
 * Its body has three leading plain-nav {@code set}s (NOT conditionals — they must NOT register a hoist) then
 * seven {@code set … : if relativeDate exists then …} ops, so the numbering is {@code ifThenElseResult0..6}
 * (dump §1.1) — the first conditional ({@code adjustedDate}) is {@code ifThenElseResult0}, NOT
 * {@code ifThenElseResult3}.
 *
 * <h2>L-032 finding: RESOLVED at the PR-4 retarget — the §1.1 decl-type cross-check is live</h2>
 * The dump locals are typed {@code Date}/{@code BusinessCenters}/{@code BusinessDayConventionEnum} (dump
 * lines 62/67/72). Those types were NOT resolvable offline at the #219 pin: the then-branches are
 * <em>alias-receiver</em> navs ({@code relativeDate} is an alias), so {@code getInferredType} returned
 * {@code MISSING} (the L-032 shortcut-type gap) and the skeleton's type was masked ({@code __TYPE__}),
 * with the decl-type byte cross-check deferred to "C". The original pin existed to catch exactly the
 * engine that resolves it — and the PR-4 retarget onto today's engine tripped it as designed: the fork's
 * #447 typed-alias-nav wave types alias-headed navs as their leaf attribute, so the decl types resolve
 * and slice 1 now byte-checks the REAL dump §1.1 types (the slice-2 §6 {@code string} retense precedent).
 * Still validated alongside: the conditional count (the 3 leading navs excluded), the
 * {@code ifThenElseResult} base name, the {@code 0..6} render-walk numbering, the declare-then-assign
 * shape and the absent {@code else} — the §5 temp-naming parity linchpin. The §5.1/§5.2 {@code thenArg}
 * decl types stay masked (render-derived {@code MapperC} wrappers, still a "C" cross-check — those pins
 * held on today's engine).
 *
 * <h2>Why offline, not the live byte gate</h2>
 * The SET-position conditional is renderer-intercepted at {@code FunctionExpressionRenderer:463} before the
 * expression compiler, so the IR cannot drive it live without subclassing that renderer (the "C" obligation).
 * {@code adaptConditionalForHarness} is a harness-only adapter entry (NOT in the shared {@code adapt}
 * dispatch), so this is fully byte-inert — the 3-cell FUNCTION/RULE byte gate is unaffected.
 *
 * <p>Requires the corpus; skips cleanly when absent.
 *
 * <h2>Geometry (the IR-train PR-4 retarget)</h2>
 * Ported from the lab at its {@code ../../phase1-bundle} staging (the {@code ir.lab.bundle} property override
 * dropped with it); now resolves THIS repo's {@code test-corpus/} cell layout, module-relative like the D11
 * gate's {@code ../test-corpus} convention, with the builtins resolved by the D11 union convention
 * ({@code test-corpus/rune-dsl-builtins/} preferred, the sibling {@code ../rune-dsl/} checkout as per-file
 * fallback). The dep closures mirror the gate's own maps (cdm6 → rune-fpml 1.5.3; drr → cdm 5.38.0 +
 * iso20022 1.38.0).
 */
class Wave6AnfSkeletonTest {

    private static final Path CORPUS_ROOT = Path.of("../test-corpus");
    /** Builtin search roots in priority order (the D11 {@code BUILTINS_SEARCH_ROOTS} convention). */
    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            CORPUS_ROOT.resolve("rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            Path.of("../rune-dsl/rune-runtime/src/main/resources/model"));

    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(new JavaTypeUtil());

    private static final String CARRIER = "ConvertToAdjustableOrRelativeDate";
    private static final String BOOL_CARRIER = "Create_MarginCorrectionData";
    private static final String THEN_SINGLE_CARRIER = "ExtractAncillaryPartyByRole"; // dump §5.1 (single bare thenArg)
    private static final String THEN_NESTED_CARRIER = "CheckAssetType";              // dump §5.2 (nested thenArg0..3)

    /**
     * The dump §1.1 hoist skeleton for the first three conditionals — branch interiors masked, decl types
     * REAL (dump lines 62/67/72: {@code Date}/{@code BusinessCenters}/{@code BusinessDayConventionEnum}).
     * The types were masked ({@code __TYPE__}) while the alias-receiver then-branch type was L-032-MISSING
     * at the #219 pin; the PR-4 retarget onto today's engine resolved them (the fork's #447 typed-alias-nav
     * wave — see the test body's FINDING note), so the decl-type byte cross-check the class docs deferred
     * to "C" is LIVE here. Validates the count, base name, {@code 0..2} numbering, declare-then-assign
     * shape AND the decl-type bytes.
     */
    private static final String EXPECTED_FIRST_3 = ""
            + "Date ifThenElseResult0 = null;\n"
            + "if (__COND__) {\n"
            + "    ifThenElseResult0 = __THEN__;\n"
            + "}\n"
            + "BusinessCenters ifThenElseResult1 = null;\n"
            + "if (__COND__) {\n"
            + "    ifThenElseResult1 = __THEN__;\n"
            + "}\n"
            + "BusinessDayConventionEnum ifThenElseResult2 = null;\n"
            + "if (__COND__) {\n"
            + "    ifThenElseResult2 = __THEN__;\n"
            + "}\n";

    @Test
    void conditionalHoistSkeletonMatchesDumpSection1_1() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(CORPUS_ROOT.resolve("cdm/cdm-6.20.6")),
                "test-corpus cdm/cdm-6.20.6 not staged — skipped");
        Assumptions.assumeTrue(!resolveBuiltinFiles().isEmpty(),
                "rune-dsl builtins absent (searched " + BUILTINS_SEARCH_ROOTS + ") — skipped (linking needs them — Copilot #468 C-1)");

        RWorkspace ws = loadCdm6().workspace();
        RFunction carrier = findFunction(ws, CARRIER);
        assertNotNull(carrier, "carrier function " + CARRIER + " not found in the cdm6 closure");

        // Walk the method's operations in source (= render-walk) order; register an ifThenElseResult hoist
        // ONLY for a conditional RHS — the three leading plain-nav sets must NOT enter the group.
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();
        List<Bind> binds = new ArrayList<>();
        List<RMetaAnnotatedType> declTypes = new ArrayList<>();
        for (ROperation op : carrier.operations()) {
            if (op.expression() instanceof RConditionalExpr conditional) {
                appendHoists(adapter.adaptConditionalFacts(conditional, NodeId.ROOT, ws), binds, declTypes);
            }
        }
        assertEquals(7, binds.size(),
                "expected 7 conditional sets (ifThenElseResult0..6); the 3 leading plain-nav sets must not register");

        // FINDING (was pinned MISSING at #219; the PR-4 retarget onto today's engine RESOLVED it — the
        // ANTICIPATED trip the pin existed to catch): the fork's #447 typed-alias-nav wave types the
        // alias-receiver then-branch navs (`relativeDate -> adjustedDate` heads the `relativeDate` alias,
        // exactly the L-032 shape), so the decl types now resolve and the dump §1.1 decl-type byte
        // cross-check the class docs deferred to "C" is wired LIVE via EXPECTED_FIRST_3's real types
        // (Date/BusinessCenters/BusinessDayConventionEnum, dump lines 62/67/72) — per the original
        // note's instruction for exactly this event (the slice-2 §6 retense precedent).
        RMetaAnnotatedType firstType = declTypes.get(0);
        assertTrue(firstType != null && !firstType.isMissing(),
                "post-#447 the typed-alias-nav wave resolves the alias-receiver then-branch type"
                        + " (was L-032-MISSING at #219) — a regression here re-masks dump §1.1's decl types");
        assertEquals("date", String.valueOf(firstType),
                "ifThenElseResult0's then-branch resolves to date (dump §1.1 Date cross-check)");

        AnfSkeletonRenderer renderer = new AnfSkeletonRenderer(TYPE_TRANSLATOR);

        // (a) Byte-diff the first three hoists against the dump §1.1 skeleton (the dump cross-check).
        assertEquals(EXPECTED_FIRST_3, renderer.renderHoists(binds.subList(0, 3), declTypes.subList(0, 3)),
                "hoist skeleton for ifThenElseResult0..2 must byte-match dump §1.1");

        // (b) The full method renders ifThenElseResult0..6 in render-walk order, declare-then-assign shape,
        //     no else, and stops at 6 (per-method counter reset / numbering invariant).
        String all = renderer.renderHoists(binds, declTypes);
        for (int i = 0; i < 7; i++) {
            String name = "ifThenElseResult" + i;
            assertTrue(all.contains(name + " = null;\n"), "missing declare-then-assign for " + name);
            assertTrue(all.contains(name + " = __THEN__;\n"), "missing guarded assign for " + name);
        }
        assertFalse(all.contains("ifThenElseResult7"), "numbering must stop at 6 (7 hoists, 0..6)");
        assertFalse(all.contains("ifThenElseResult ="), "a multi-member group must be numbered, never bare");
        assertFalse(all.contains("else"), "an else-less conditional must hoist with no else branch");

        // (c) Single-member group → BARE name (no '0' suffix), the ratified §5:318 rule that the carrier's
        //     multi-member group does not exercise (the dump's `ifThenElseResult0` parenthetical is a typo).
        List<Bind> singleHoist = Normalize.conditionalToBind(
                new ConditionalHoist(NodeId.ROOT, firstType, false, NodeId.ROOT.child(0), false,
                        RMetaAnnotatedType.MISSING));
        assertEquals(""
                        + "Date ifThenElseResult = null;\n"
                        + "if (__COND__) {\n"
                        + "    ifThenElseResult = __THEN__;\n"
                        + "}\n",
                renderer.renderHoists(singleHoist, List.of(firstType)),
                "a single ifThenElseResult must render bare (no numeric suffix)");

        // (d) normalize : IRExpr -> ANFExpr produces the same ifThenElseResult hoist for a real (synthetic,
        //     else-less) IRConditional — the IRExpr->ANF deliverable, sharing conditionalToBind.
        IRExpr leaf = new IRVariable("p", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                firstType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRConditional synthetic = new IRConditional(leaf, leaf, null, NodeId.ROOT,
                firstType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        ANFExpr anf = Normalize.normalize(synthetic);
        assertTrue(anf instanceof Block, "normalize of a conditional yields a Block");
        Block block = (Block) anf;
        assertEquals(1, block.lets().size(), "one ifThenElseResult hoist");
        assertEquals("ifThenElseResult", block.lets().get(0).name().base());
        assertTrue(block.lets().get(0).value() instanceof JoinPoint,
                "a statement-position conditional lowers to a JoinPoint (declare-then-assign)");
    }

    /**
     * Slice 2: the §6 cross-group render-walk REORDER — a hoisted {@code boolean} condition decl renders BEFORE
     * the {@code ifThenElseResult} it guards (dump §6), with two independent group counters. Carrier =
     * {@code Create_MarginCorrectionData} (drr), whose conditionals are CONSTRUCTOR-FIELD-nested (collected in
     * pre-order = render-walk order). Only the bare-fn-call condition ({@code IsMax32…(uti)}) hoists a
     * {@code _boolean}; the equality/existence conditions stay inline. The then-branch decl types are MISSING
     * offline at #219 (drr's type engine under-resolves these expressions, like §1.1's alias coupling), so the
     * types are masked and validated at "C"; this test validates the STRUCTURE — the cross-group reorder, the
     * escaped-single {@code _boolean}, the two independent counters, the declare-then-assign shape. Byte-inert.
     */
    @Test
    void booleanRenderOrderReorderMatchesDumpSection6() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(CORPUS_ROOT.resolve("drr/drr-6.34.1")),
                "test-corpus drr/drr-6.34.1 not staged — skipped");
        Assumptions.assumeTrue(!resolveBuiltinFiles().isEmpty(),
                "rune-dsl builtins absent (searched " + BUILTINS_SEARCH_ROOTS + ") — skipped (linking needs them — Copilot #468 C-1)");

        RWorkspace ws = loadDrr().workspace();
        RFunction carrier = findFunction(ws, BOOL_CARRIER);
        assertNotNull(carrier, "carrier function " + BOOL_CARRIER + " not found in the drr closure");

        // The conditionals are nested in the single `set details` constructor; collect them in pre-order
        // (= render-walk order) WITHOUT descending into a conditional's own branches.
        List<RConditionalExpr> conditionals = new ArrayList<>();
        for (ROperation op : carrier.operations()) {
            collectConditionals(op.expression(), conditionals);
        }
        assertEquals(4, conditionals.size(),
                "expected 4 constructor-field conditionals (unqTxIdr, prtry, cd, noPrtfl)");

        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();
        List<Bind> binds = new ArrayList<>();
        List<RMetaAnnotatedType> declTypes = new ArrayList<>();
        for (RConditionalExpr conditional : conditionals) {
            appendHoists(adapter.adaptConditionalFacts(conditional, NodeId.ROOT, ws), binds, declTypes);
        }
        // 4 ifThenElseResult hoists + exactly 1 boolean hoist (only cond-0's `IsMax32…(uti)` is a bare fn-call).
        assertEquals(5, binds.size(), "4 ifThenElseResult + 1 boolean hoist");
        assertEquals("boolean", binds.get(0).name().base(),
                "the boolean hoist registers FIRST — the render-walk reorder (before the ifThenElseResult it guards)");

        // FINDING (was pinned MISSING at #219; the re-vendor to 3c60acec RESOLVED it — the ANTICIPATED trip): the
        // fork's head->feature cascade fix improved offline inference, so ifThenElseResult0's then-branch type now
        // resolves to `string` (no longer the drr under-resolution gap). The hoist STRUCTURE checks below stand
        // regardless — the decl type only affects the masked decl line, never the cross-group reorder / boolean
        // family / two independent counters this test pins. So we now cross-check the RESOLVED type (dump §6 String),
        // per the original note's instruction for exactly this re-vendor event.
        assertTrue(declTypes.get(1) != null && !declTypes.get(1).isMissing(),
                "post-3c60acec the cascade fix resolves ifThenElseResult0's then-branch type (was MISSING at #219)");
        assertEquals("string", String.valueOf(declTypes.get(1)),
                "ifThenElseResult0's then-branch resolves to string at 3c60acec (dump §6 String cross-check)");

        AnfSkeletonRenderer renderer = new AnfSkeletonRenderer(TYPE_TRANSLATOR);
        String all = renderer.renderHoists(binds, declTypes);

        // (a) The make-or-break REORDER: the hoisted boolean condition decl (a `final …` initializer) renders
        //     BEFORE the ifThenElseResult it guards. _boolean is the escaped-single boolean (keyword → leading
        //     underscore, NEVER boolean0) — the §5 escape rule slice 1's multi-member group does not exercise.
        assertTrue(all.startsWith("final "), "the boolean hoist (a final-initializer) renders first");
        int booleanAt = all.indexOf("_boolean = " + AnfSkeletonRenderer.MASK_VALUE + ";");
        assertTrue(booleanAt >= 0 && booleanAt < all.indexOf("ifThenElseResult0"),
                "the _boolean decl must render BEFORE ifThenElseResult0 (the §6 cross-group reorder)");
        assertFalse(all.contains("boolean0"), "a single boolean member is escaped (_boolean), never boolean0");

        // (b) Two independent group counters: ifThenElseResult 0..3 (4 conditionals), declare-then-assign, no else.
        for (int i = 0; i < 4; i++) {
            assertTrue(all.contains("ifThenElseResult" + i + " = null;\n"), "missing ifThenElseResult" + i);
            assertTrue(all.contains("ifThenElseResult" + i + " = __THEN__;\n"),
                    "missing guarded assign for ifThenElseResult" + i);
        }
        assertFalse(all.contains("ifThenElseResult4"), "4 conditionals -> ifThenElseResult0..3");
        assertFalse(all.contains("else"), "else-less conditionals hoist with no else branch");
    }

    /**
     * Slice 3, dump §5.1: a SINGLE {@code then} hoists ONE <strong>bare</strong> {@code thenArg} (no numeric
     * suffix; {@code thenArg} is not a Java keyword, so no {@code _} escape). Carrier =
     * {@code ExtractAncillaryPartyByRole} (cdm6), whose SET RHS is directly the {@code then}-chain
     * {@code ancillaryParties filter role = roleEnumToExtract then only-element} — a {@code then <list-primitive>}
     * which (dump §5.1, correcting the design's V6 "no Let binder" hypothesis) DOES hoist its argument. The decl
     * type is masked (the {@code MapperC<…>} wrapper is render-derived — a "C" cross-check). Byte-inert.
     */
    @Test
    void thenChainHoistSingleMatchesDumpSection5_1() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(CORPUS_ROOT.resolve("cdm/cdm-6.20.6")),
                "test-corpus cdm/cdm-6.20.6 not staged — skipped");
        Assumptions.assumeTrue(!resolveBuiltinFiles().isEmpty(),
                "rune-dsl builtins absent (searched " + BUILTINS_SEARCH_ROOTS + ") — skipped (linking needs them — Copilot #468 C-1)");

        RWorkspace ws = loadCdm6().workspace();
        RFunction carrier = findFunction(ws, THEN_SINGLE_CARRIER);
        assertNotNull(carrier, "carrier function " + THEN_SINGLE_CARRIER + " not found in the cdm6 closure");

        // Find the chain via the stop-on-match walk — exactly ONE chain (the SET RHS is the then directly).
        List<RThenExpr> chains = new ArrayList<>();
        for (ROperation op : carrier.operations()) {
            collectThenChains(op.expression(), chains);
        }
        assertEquals(1, chains.size(), "ExtractAncillaryPartyByRole has exactly one then-chain (the SET RHS)");

        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();
        ThenChainHoist facts = adapter.adaptThenChainFacts(chains.get(0), NodeId.ROOT, ws);
        assertEquals(1, facts.binderKeys().size(), "a single `then` -> one thenArg hoist");

        AnfSkeletonRenderer renderer = new AnfSkeletonRenderer(TYPE_TRANSLATOR);
        String all = renderer.renderHoists(Normalize.thenChainToBinds(facts), facts.binderTypes());

        // (a) single member -> BARE `thenArg` (no numeric suffix), the §5:318 rule; dump §5.1 line 164.
        assertTrue(all.contains("thenArg = " + AnfSkeletonRenderer.MASK_VALUE + ";"),
                "the single then-chain hoists a bare `thenArg` initializer");
        assertFalse(all.contains("thenArg0"), "a single thenArg is bare, never thenArg0");
        // (b) `thenArg` is not a Java keyword -> no leading-underscore escape (unlike the slice-2 `_boolean`).
        assertFalse(all.contains("_thenArg"), "thenArg is not a keyword -> never escaped");
        assertFalse(AnfSkeletonRenderer.isJavaKeyword("thenArg"), "thenArg is not a Java keyword");
        // (c) the render form is a fluent `final <T> name = <value>;` initializer (Block-value Bind), NOT the
        //     conditional's declare-then-assign (`= null; if(...)`).
        assertTrue(all.startsWith("final "), "a thenArg hoist is a fluent initializer (final), not declare-then-assign");
        assertFalse(all.contains("= null;"), "a thenArg initializer is not the conditional declare-then-assign form");
        assertFalse(all.contains("if ("), "a thenArg hoist has no guard");
        // (d) FINDING (pinned, like §1.1/§6): the decl type is MISSING offline at #219 (the Mapper wrapper +
        //     element type are render-derived — a "C" cross-check); the type renders masked (__TYPE__).
        assertTrue(facts.binderTypes().get(0) == null || facts.binderTypes().get(0).isMissing(),
                "the thenArg decl type is a C cross-check (MISSING offline at #219)");
        assertTrue(all.contains(AnfSkeletonRenderer.MASK_TYPE + " thenArg ="), "the decl type is masked offline");
    }

    /**
     * Slice 3, dump §5.2: a NESTED {@code then}-chain hoists {@code thenArg0..3} (a multi-member group → numeric
     * suffix from 0, no escape), each re-rooting the next. Carrier = {@code CheckAssetType} (cdm6), whose 4-{@code
     * then} chain is the parenthesised RIGHT operand of a logical {@code or} ({@code collateralAssetTypes is absent
     * or (collateralAssetTypes then filter… then filter… then filter… then item exists)}). The harness therefore
     * FINDS the chain by a stop-on-match tree walk that yields exactly ONE outermost chain — a naive full-tree
     * collector would over-count 4 sibling chains by descending the chain's own {@code argument()} spine. The decl
     * types are masked (render-derived — a "C" cross-check). Byte-inert.
     */
    @Test
    void thenChainHoistNestedMatchesDumpSection5_2() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(CORPUS_ROOT.resolve("cdm/cdm-6.20.6")),
                "test-corpus cdm/cdm-6.20.6 not staged — skipped");
        Assumptions.assumeTrue(!resolveBuiltinFiles().isEmpty(),
                "rune-dsl builtins absent (searched " + BUILTINS_SEARCH_ROOTS + ") — skipped (linking needs them — Copilot #468 C-1)");

        RWorkspace ws = loadCdm6().workspace();
        RFunction carrier = findFunction(ws, THEN_NESTED_CARRIER);
        assertNotNull(carrier, "carrier function " + THEN_NESTED_CARRIER + " not found in the cdm6 closure");

        List<RThenExpr> chains = new ArrayList<>();
        for (ROperation op : carrier.operations()) {
            collectThenChains(op.expression(), chains);
        }
        assertEquals(1, chains.size(),
                "CheckAssetType has exactly one then-chain (the `or` right operand); the inner thens are NOT separate chains");

        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();
        ThenChainHoist facts = adapter.adaptThenChainFacts(chains.get(0), NodeId.ROOT, ws);
        assertEquals(4, facts.binderKeys().size(), "the 4-`then` chain -> thenArg0..3");

        AnfSkeletonRenderer renderer = new AnfSkeletonRenderer(TYPE_TRANSLATOR);
        String all = renderer.renderHoists(Normalize.thenChainToBinds(facts), facts.binderTypes());

        // (a) multi-member group -> numbered thenArg0..3 (never bare), in re-rooting (registration) order.
        for (int i = 0; i < 4; i++) {
            assertTrue(all.contains("thenArg" + i + " = " + AnfSkeletonRenderer.MASK_VALUE + ";"),
                    "missing thenArg" + i + " initializer");
        }
        assertFalse(all.contains("thenArg4"), "4 thens -> thenArg0..3, never thenArg4");
        assertFalse(all.contains("thenArg = " + AnfSkeletonRenderer.MASK_VALUE + ";"),
                "a multi-member group is numbered, never bare");
        // (b) fluent initializers (Block-value), not declare-then-assign; types masked offline (the pinned finding).
        assertTrue(all.startsWith("final "), "thenArg hoists are fluent initializers");
        assertFalse(all.contains("= null;"), "thenArg initializers are not declare-then-assign");
        for (RMetaAnnotatedType declType : facts.binderTypes()) {
            assertTrue(declType == null || declType.isMissing(),
                    "the thenArg decl types are a C cross-check (MISSING offline at #219)");
        }
    }

    /**
     * Slice 3, the C-reusable desugar: {@code normalize(Let)} flattens a (right-nested) {@link Let} chain into the
     * {@code thenArg} {@link Bind} sequence + the residual result. Exercised on a SYNTHETIC chain of lowerable
     * param atoms (the corpus then-bodies do not lower at #219, so this validates the flatten/numbering on a
     * lowerable case). The two desugar paths — {@code factsOf(Let)} (the {@code in}-spine) and the harness's
     * {@code adaptThenChainForHarness} (the {@code argument()}-spine) — converge on the same {@code thenArg}
     * sequence; their byte cross-check on a real carrier is a "C" obligation. Byte-inert (no live path).
     */
    @Test
    void normalizeOfLetChainFlattensToThenArgs() {
        IRExpr residual = param("residual", NodeId.ROOT.child(9));
        NodeId k0 = NodeId.ROOT.child(0);
        NodeId k1 = NodeId.ROOT.child(1);
        NodeId k2 = NodeId.ROOT.child(2);
        // Outermost Let = thenArg0 (the chain base); descending `in` gives thenArg1, thenArg2; the innermost
        // `in` (residual) is NOT a binder.
        Let inner = let("t2", param("v2", k2), residual, k2);
        Let mid = let("t1", param("v1", k1), inner, k1);
        Let outer = let("t0", param("v0", k0), mid, k0);

        ANFExpr anf = Normalize.normalize(outer);
        assertTrue(anf instanceof Block, "normalize of a Let chain yields a Block");
        Block block = (Block) anf;

        // (a) N thenArg Binds; (b) outermost-first order matching construction (the binder KEYS).
        assertEquals(3, block.lets().size(), "3 Lets -> 3 thenArg hoists");
        assertEquals(List.of(k0, k1, k2), block.lets().stream().map(b -> b.name().key()).toList(),
                "thenArg binders in outermost-first (render-walk) order");
        for (Bind b : block.lets()) {
            assertEquals("thenArg", b.name().base(), "the then-chain hoist family base lexeme");
            assertTrue(b.value() instanceof Block, "a thenArg is a fluent-initializer (Block-value) Bind, not a JoinPoint");
        }
        // (c) the residual is the innermost `in` atom — by identity.
        assertSame(residual, block.result(), "the residual is the innermost `in` atom (by identity)");

        // (d) a single Let -> ONE BARE thenArg (renders bare, no suffix).
        Block single = (Block) Normalize.normalize(let("t", param("v", k0), residual, k0));
        assertEquals(1, single.lets().size(), "1 Let -> 1 thenArg");
        String rendered = new AnfSkeletonRenderer(TYPE_TRANSLATOR)
                .renderHoists(single.lets(), List.of(RMetaAnnotatedType.MISSING));
        assertTrue(rendered.contains("thenArg = " + AnfSkeletonRenderer.MASK_VALUE + ";"), "single Let -> bare thenArg");
        assertFalse(rendered.contains("thenArg0"), "a single Let thenArg is bare, never thenArg0");
    }

    /** A synthetic single scalar param leaf atom (a lowerable IR node for the {@code normalize(Let)} flatten test). */
    private static IRExpr param(String name, NodeId id) {
        return new IRVariable(name, IRVariable.VariableKind.PARAM, id, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }

    /** A synthetic {@link Let} (a desugared {@code then} step) over the given value/continuation. */
    private static Let let(String binder, IRExpr value, IRExpr in, NodeId id) {
        return new Let(binder, value, in, id, RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
    }

    /** Flatten a conditional's hoist {@link Bind}s with their aligned decl types (boolean → its boolean type). */
    private static void appendHoists(ConditionalHoist facts, List<Bind> binds, List<RMetaAnnotatedType> declTypes) {
        for (Bind b : Normalize.conditionalToBind(facts)) {
            binds.add(b);
            declTypes.add(b.name().base().equals("boolean") ? facts.conditionType() : facts.resultType());
        }
    }

    /** Pre-order collect of {@link RConditionalExpr}s, NOT descending into a conditional's own branches. */
    private static void collectConditionals(RNode node, List<RConditionalExpr> out) {
        if (node instanceof RConditionalExpr conditional) {
            out.add(conditional);
            return;
        }
        for (RNode child : node.children()) {
            collectConditionals(child, out);
        }
    }

    /**
     * Stop-on-match pre-order collect of the OUTERMOST {@link RThenExpr}s — on the first {@code RThenExpr} it adds
     * it and RETURNS, so it does NOT descend into a chain's own {@code argument()} spine: a nested chain is ONE
     * chain, not N sibling chains (the double-count the slice-3 review flagged). Mirrors {@link #collectConditionals}.
     */
    private static void collectThenChains(RNode node, List<RThenExpr> out) {
        if (node instanceof RThenExpr then) {
            out.add(then);
            return;
        }
        for (RNode child : node.children()) {
            collectThenChains(child, out);
        }
    }

    // ----- corpus loading (cdm6 closure: builtins + rune-fpml/1.5.3 dep + cdm6 sources) ------------------

    private static RLinkingResult loadCdm6() throws IOException {
        List<RModel> models = new ArrayList<>();
        for (Path p : resolveBuiltinFiles()) {
            models.add(AstBuilder.buildFromFile(p));
        }
        // cdm/6.20.6 transitive dependency (resolution-only, NOT version-stamped) — mirrors the gate's deps().
        for (Path p : rosettaFiles(CORPUS_ROOT.resolve("rune-fpml/rune-fpml-1.5.3/rosetta-source/src/main/rosetta"))) {
            models.add(AstBuilder.buildFromFile(p));
        }
        // The cell's own models — version-stamped to match its frozen goldens.
        for (Path p : rosettaFiles(CORPUS_ROOT.resolve("cdm/cdm-6.20.6/rosetta-source/src/main/rosetta"))) {
            RModel model = AstBuilder.buildFromFile(p);
            model.setVersion("0.0.0.master-SNAPSHOT");
            models.add(model);
        }
        return RWorkspace.build(models);
    }

    /** Load the drr/6.34.1 closure (builtins + cdm/5.38.0 + iso20022/1.38.0 deps + drr sources). */
    private static RLinkingResult loadDrr() throws IOException {
        List<RModel> models = new ArrayList<>();
        for (Path p : resolveBuiltinFiles()) {
            models.add(AstBuilder.buildFromFile(p));
        }
        // drr transitive dependencies (resolution-only, NOT version-stamped) — mirrors the gate's deps().
        for (String dep : List.of("cdm/cdm-5.38.0", "iso20022/iso20022-1.38.0")) {
            for (Path p : rosettaFiles(CORPUS_ROOT.resolve(dep + "/rosetta-source/src/main/rosetta"))) {
                models.add(AstBuilder.buildFromFile(p));
            }
        }
        for (Path p : rosettaFiles(CORPUS_ROOT.resolve("drr/drr-6.34.1/rosetta-source/src/main/rosetta"))) {
            RModel model = AstBuilder.buildFromFile(p);
            model.setVersion("0.0.0.master-SNAPSHOT");
            models.add(model);
        }
        return RWorkspace.build(models);
    }

    private static List<Path> rosettaFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(p -> p.toString().endsWith(".rosetta"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    /**
     * Union of builtin {@code .rosetta} files across {@link #BUILTINS_SEARCH_ROOTS}, dedup'd by
     * filename in priority order (the D11 {@code resolveBuiltinFiles} convention — the test-corpus
     * root wins filename collisions; the sibling checkout fills gaps).
     */
    private static List<Path> resolveBuiltinFiles() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> s = Files.walk(root)) {
                s.filter(p -> p.toString().endsWith(".rosetta"))
                        .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        return resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();
    }

    private static RFunction findFunction(RWorkspace ws, String name) {
        for (RModel model : ws.files()) {
            for (RRootElement element : model.rootElements()) {
                if (element instanceof RFunction func && name.equals(func.name())) {
                    return func;
                }
            }
        }
        return null;
    }
}
