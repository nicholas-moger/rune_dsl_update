package com.regnosys.rosetta.symbols.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RClosureParameter;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * v3.2 seat 8: the seat suite of F14 + F15 — the two L2 (resolution-differential) families the chaos cell's
 * 258 declared rows were, both healed at the fork's AST so its OWN resolver binds what upstream binds.
 *
 * <ul>
 *   <li><b>(A) F14 — the closure parameter is a node.</b> Upstream binds a lambda parameter to a
 *       {@code ClosureParameter}; the fork kept a lambda's parameters as bare names
 *       ({@code RInlineFunction.paramNames()}) and bound a reference to the DECLARING lambda instead — the
 *       resolution spec's § 7 gap, the vendored conformance suite's one frozen exception, 222 chaos rows
 *       (s05 78 + s20 144). Every declared parameter is an {@link RClosureParameter} now: a child of its
 *       lambda, ranged on its ID token, the binding target ({@code LexicalResolutionPass.lookupInParentChain}
 *       registers the node; the synthetic {@code item} of an implicit lambda — upstream's implicit variable —
 *       stays name-only and keeps binding the lambda). {@link RInlineFunction#declaringLambdaOf} is the ONE
 *       read of both shapes every consumer consults (LAW 69).</li>
 *   <li><b>(B) F15 — the only-exists leaf is a reference.</b> Upstream models an {@code only exists}
 *       argument as an ordinary feature call and binds its leaf; the fork's {@link ROnlyExistsElement} kept the
 *       leaf a bare string (only the RECEIVER chain was synthesized as nodes), so nothing bound it and the dump
 *       had no record at its offset (36 chaos rows, s21's {@code t -> p only exists}). The whole path is one
 *       expression now — {@link ROnlyExistsElement#leafReference()}, an {@link RFeatureCall} whose receiver IS the
 *       unchanged {@link ROnlyExistsElement#receiverExpression()} — ranged hop by hop to its own feature.</li>
 * </ul>
 *
 * <p>The witnesses read the fork's bindings THROUGH the differential's own translator
 * ({@link ForkResolutionDump}) wherever a claim is about what upstream would see (the {@code ClosureParameter}
 * kind, the container-walking qualified name, the leaf record at the leaf's offset), and through the AST where the
 * claim is structural. The corpus locks parse the three chaos BASE files of the healed families and pin the
 * healed records by kind and qualified name; the chaos-wide receipt is {@link ChaosResolutionConformanceTest}
 * with its declared set EMPTY.
 *
 * <p><b>THE LANE SET</b> ({@code target/v32-seat8-instruments/lanes-s8.py}, measured at the seat-suite head
 * {@code 6d3737e2d} — {@code scratch/lanes-s8-c6.status}, local — and re-taken WHOLE at the round-1 code head
 * {@code 688d78539} ({@code scratch/lanes-s8-c10.status}) and the round-2 code head {@code 22f71e1c8}
 * ({@code scratch/lanes-s8-c12.status}) with every set below UNMOVED — the PINNED per-run copies, never the rolling
 * {@code lanes-s8.status} the runner overwrites; each lane an exact-string mutation of the committed code,
 * restored from git, run inside its own module over this suite + the two conformance gates + the list-of-lists wave;
 * LAW 82 — the record of truth is the log): A1 the lexical scope registers the LAMBDA instead of the parameter node →
 * a1 a2 a3 a4 a6 a8 corpus_c1 corpus_c2 + both chaos-gate tests + the vendored gate (a7 predicted, GREEN: the
 * name-only arm derives the same type for the lambda binding, so typing cannot tell the shapes apart); A2 the builder
 * adds the NAME only → the same eleven (a1 / a3 as errors past the empty node list); B1 the dump's ClosureParameter arm
 * removed → a6 a9 corpus_c1 corpus_c2 + the three gate tests (EXACT); C1 the scope filter's param-hit exemption
 * narrowed to RInlineFunction → a8 ALONE (EXACT); D1 the type computer's RClosureParameter arm gated off → a7 +
 * {@code LoLItemCardinalityWaveTest.named_then_param_receiver_carries_nesting} (a wave fixture DOES type a declared
 * then-parameter); E1 the cardinality computer's then-param leg reverted → that wave test ALONE (the leg IS witnessed);
 * J1 {@code UpstreamSymbolKinds}' RClosureParameter admission removed → a1 a4 a6 a8 corpus_c1 corpus_c2 + the three gate
 * tests (the admission filters the node out of the scope's answer); F1 the element's leaf reference dropped → b1 b2 b3
 * corpus_c3 + both chaos-gate tests (EXACT); G1 the optimised classifier's enclosure read reverted to the PARENT test →
 * {@code OnlyExistsGuardSeatTest}'s two catch tests (EXACT); H1 the IR adapter's closure-param mint reverted →
 * {@code ExpressionToIRAdapterTest.aliasNameMatchAndClosureParamsLowerAtBothFlavors} ALONE; I1 {@code
 * IRExpressionCompiler}'s cpSym token reverted → GREEN (112/0F), WITHDRAWN under the #614 law: the two sites render
 * probe tokens read by the IR derivation's diagnostic print alone. Ten lanes kept, one withdrawn; every kept lane red
 * on a non-empty set of its own witnesses and on nothing outside the named suites.
 */
class ClosureParameterSeatTest {

    // The cell from the corpus SOT through ONE read (v3.2 seat 10, D49 — the 1.0.0 -> 1.1.0 cut).
    private static final Path CHAOS_SOURCES = com.regnosys.rosetta.testutil.ChaosCatalogue.sources();
    private static final Path BUILTINS =
            Paths.get("..", "test-corpus", "rune-dsl-builtins", "rune-runtime", "src", "main", "resources", "model");

    private static final String THINGS = """
            namespace test
            type Sub:
                v number (1..1)
                w string (0..1)
            type Thing:
                v number (1..1)
                w string (0..1)
                subs Sub (0..*)
            """;

    // ------------------------------------------------------------------ (A) F14 --

    @Test
    void a1_explicitParameter_bindsItsOwnNode_notTheLambda() {
        Linked l = link(THINGS + """
                func Fn:
                    inputs:
                        items Thing (0..*)
                    output:
                        vs number (0..*)
                    add vs:
                        items extract x [ x -> v ]
                """);
        RInlineFunction lambda = onlyLambda(l);
        assertEquals(List.of("x"), lambda.paramNames(), "the name view");
        assertEquals(1, lambda.parameters().size(), "the declared node");
        RClosureParameter x = lambda.parameters().get(0);
        assertEquals("x", x.name());
        assertSame(lambda, x.declaringFunction(), "the parameter's parent is the lambda that declares it");
        assertSame(lambda, x.parent());
        assertEquals(List.of(x, lambda.body()), lambda.children(), "children(): the parameters, then the body");
        assertTrue(x.sourceRange().hasByteOffsets(), "the node is ranged on its ID token");
        // `x -> v` parses as an REnumValueRef whose HEAD is the parameter: the head binding is the node
        REnumValueRef nav = onlyEnumValueRef(l, "x");
        assertSame(x, nav.resolvedHead().orElseThrow(), "the head of `x -> v` binds the parameter node");
        assertSame(lambda, RInlineFunction.declaringLambdaOf(x, "x").orElseThrow(), "the ONE read answers the lambda");
        assertTrue(RInlineFunction.declaringLambdaOf(x, "y").isEmpty(), "…for the parameter's OWN name only");
    }

    @Test
    void a2_bareReference_bindsTheNode() {
        Linked l = link(THINGS + """
                func Fn:
                    inputs:
                        items Thing (0..*)
                    output:
                        out Thing (0..*)
                    add out:
                        items extract x [ x ]
                """);
        RSymbolReference x = onlySymbolRef(l, "x");
        RNode bound = x.symbol().orElseThrow();
        assertTrue(bound instanceof RClosureParameter, "a bare reference binds the parameter node, got " + bound.getClass().getSimpleName());
        assertSame(onlyLambda(l), ((RClosureParameter) bound).declaringFunction());
    }

    @Test
    void a3_reduce_twoParametersAreTwoDistinctNodes() {
        Linked l = link("""
                namespace test
                func Sum:
                    inputs:
                        nums number (0..*)
                    output:
                        total number (1..1)
                    set total:
                        nums reduce a, b [ a + b ]
                """);
        RInlineFunction lambda = onlyLambda(l);
        assertEquals(List.of("a", "b"), lambda.paramNames());
        assertEquals(2, lambda.parameters().size());
        RNode a = onlySymbolRef(l, "a").symbol().orElseThrow();
        RNode b = onlySymbolRef(l, "b").symbol().orElseThrow();
        assertSame(lambda.parameters().get(0), a);
        assertSame(lambda.parameters().get(1), b);
        assertNotSame(a, b, "the pre-node binding could not tell `a` from `b` — both were the lambda");
        assertSame(lambda, ((RClosureParameter) a).declaringFunction());
        assertSame(lambda, ((RClosureParameter) b).declaringFunction());
    }

    @Test
    void a4_nestedLambdas_theOuterParameterIsVisibleInsideTheInner() {
        Linked l = link(THINGS + """
                func Fn:
                    inputs:
                        items Thing (0..*)
                    output:
                        ws string (0..*)
                    add ws:
                        items extract x [ x -> subs extract y [ y -> w + x -> w ] ] then flatten
                """);
        List<RInlineFunction> lambdas = AstWalker.findAll(l.model, RInlineFunction.class).stream()
                .filter(fn -> !fn.isImplicit()).toList();
        assertEquals(2, lambdas.size(), "the outer and the inner explicit lambdas");
        RInlineFunction outer = lambdas.stream().filter(fn -> fn.paramNames().equals(List.of("x"))).findFirst().orElseThrow();
        RInlineFunction inner = lambdas.stream().filter(fn -> fn.paramNames().equals(List.of("y"))).findFirst().orElseThrow();
        List<REnumValueRef> xNavs = AstWalker.findAll(l.model, REnumValueRef.class).stream()
                .filter(r -> "x".equals(r.enumName())).toList();
        assertEquals(2, xNavs.size(), "`x -> subs` outside the inner lambda and `x -> w` inside it");
        for (REnumValueRef xNav : xNavs) {
            assertSame(outer.parameters().get(0), xNav.resolvedHead().orElseThrow(),
                    "every `x` — the inner lambda's included — binds the OUTER lambda's parameter node");
        }
        REnumValueRef yNav = onlyEnumValueRef(l, "y");
        assertSame(inner.parameters().get(0), yNav.resolvedHead().orElseThrow());
    }

    @Test
    void a5_theParameterlessLambdas_keepNameOnlyOrNothing_neverANode() {
        // (i) the grammar's IMPLICIT form (no brackets): the derived-state rule injects the synthetic `item` — a NAME
        // (upstream's implicit variable, which has no ClosureParameter on either side). The first run of this test had
        // claimed the bracketed form below is the implicit one — it is not (AstBuilder.buildInlineFunction sets
        // implicit=false for every bracketed lambda; ImplicitVariableRule injects `item` into implicit lambdas ONLY).
        Linked implicit = link(THINGS + """
                func Fn:
                    inputs:
                        items Thing (0..*)
                    output:
                        vs number (0..*)
                    add vs:
                        items extract item -> v
                """);
        RInlineFunction implicitLambda = onlyLambda(implicit);
        assertTrue(implicitLambda.isImplicit(), "the bracket-less body is the grammar's implicitInlineFunction");
        assertEquals(List.of("item"), implicitLambda.paramNames(), "the derived-state rule's synthetic item — a NAME");
        assertTrue(implicitLambda.parameters().isEmpty(), "…and NO node: upstream's implicit variable is not a ClosureParameter");
        assertEquals(List.of(implicitLambda.body()), implicitLambda.children());
        assertEquals(1, AstWalker.count(implicit.model, RImplicitVariable.class), "`item` parses as the implicit variable, never a symbol reference");
        assertTrue(AstWalker.findAll(implicit.model, RSymbolReference.class).stream().noneMatch(r -> "item".equals(r.name())));
        // (ii) the BRACKETED form with no declared parameter: an explicit EMPTY parameter list — no name, no node
        // (the rule's own comment: `extract []` deliberately declares an empty list and receives no synthetic binding)
        Linked bracketed = link(THINGS + """
                func Fn:
                    inputs:
                        items Thing (0..*)
                    output:
                        vs number (0..*)
                    add vs:
                        items extract [ item -> v ]
                """);
        RInlineFunction bracketedLambda = onlyLambda(bracketed);
        assertFalse(bracketedLambda.isImplicit(), "a bracketed lambda is never the implicit form");
        assertTrue(bracketedLambda.paramNames().isEmpty(), "no declared name and no synthetic one");
        assertTrue(bracketedLambda.parameters().isEmpty(), "no node");
        assertEquals(List.of(bracketedLambda.body()), bracketedLambda.children());
        assertEquals(1, AstWalker.count(bracketed.model, RImplicitVariable.class));
    }

    @Test
    void a6_theDumpPrintsUpstreamsKind_andWalksTheContainersForTheQualifiedName() {
        Linked l = link(THINGS + """
                func Fn:
                    inputs:
                        items Thing (0..*)
                    output:
                        vs number (0..*)
                    alias pick: items extract x [ x -> v ]
                    add vs:
                        items extract x [ x -> v ]
                """);
        Map<String, String> records = dump(l);
        String source = l.source;
        int aliasX = source.indexOf("[ x -> v ]", source.indexOf("alias pick")) + 2;
        int addX = source.indexOf("[ x -> v ]", source.indexOf("add vs")) + 2;
        assertEquals("ClosureParameter test.Fn.pick.x", records.get(key(aliasX, "RosettaSymbolReference.symbol", "x")),
                "inside an alias the enclosing shortcut contributes its segment, the lambda contributes none — upstream's name");
        assertEquals("ClosureParameter test.Fn.x", records.get(key(addX, "RosettaSymbolReference.symbol", "x")),
                "inside an operation only the function contributes — upstream's name");
    }

    @Test
    void a7_theBoundParameterTypesAsTheElementType() {
        Linked l = link(THINGS + """
                func Fn:
                    inputs:
                        items Thing (0..*)
                    output:
                        out Thing (0..*)
                    add out:
                        items extract x [ x ]
                """);
        RSymbolReference x = onlySymbolRef(l, "x");
        var t = l.ws.getInferredType(x);
        assertFalse(t.isMissing(), "the bound parameter types (the ExpressionTypeComputer's RClosureParameter arm)");
        assertEquals("Thing", t.type().name(), "…as the declaring lambda's argument element type");
    }

    @Test
    void a8_aParameterNamedLikeTheEnclosingAlias_isExemptFromTheAliasSelfFilter() {
        Linked l = link(THINGS + """
                func Fn:
                    inputs:
                        items Thing (0..*)
                    output:
                        vs number (0..*)
                    alias pick: items extract pick [ pick -> v ]
                    add vs: pick
                """);
        RInlineFunction lambda = onlyLambda(l);
        REnumValueRef nav = onlyEnumValueRef(l, "pick");
        assertSame(lambda.parameters().get(0), nav.resolvedHead().orElseThrow(),
                "lambda parameters sit BELOW the alias boundary (spec R5): the param hit stands, the alias's own-name filter never applies to it");
    }

    @Test
    void a9_aHandBuiltLambdaWithNameOnlyParameters_keepsThePreNodeBinding() {
        // the shape every generator / IR unit test builds (paramNames().add) and the synthetic item: no node exists,
        // the lambda itself is the binding, and the ONE read still answers it
        RInlineFunction fn = new RInlineFunction();
        fn.paramNames().add("q");
        assertTrue(fn.parameters().isEmpty());
        assertSame(fn, RInlineFunction.declaringLambdaOf(fn, "q").orElseThrow());
        assertTrue(RInlineFunction.declaringLambdaOf(fn, "z").isEmpty());
        assertTrue(RInlineFunction.isClosureParameterBinding(fn));
        assertFalse(RInlineFunction.isClosureParameterBinding(new RAttribute()));
        // the dump prints the pre-node shape as its class name — visibly foreign to upstream's kinds, never hidden
        assertEquals("RInlineFunction", ForkResolutionDump.upstreamEClassOfForTest(fn));
        RClosureParameter p = new RClosureParameter();
        p.setName("q");
        assertEquals("ClosureParameter", ForkResolutionDump.upstreamEClassOfForTest(p));
    }

    // ------------------------------------------------------------------ (B) F15 --

    private static final String PATHS = """
            namespace test
            type A:
                b string (0..1)
            type T:
                p string (0..1)
                q string (0..1)
                r string (0..1)
                a A (0..1)
            """;

    @Test
    void b1_onlyExistsLeaf_isAFeatureCallOverTheUnchangedReceiver_andBinds() {
        Linked l = link(PATHS + """
                func G:
                    inputs:
                        t T (1..1)
                    output:
                        ok boolean (1..1)
                    set ok:
                        t -> p only exists
                """);
        ROnlyExistsElement el = onlyElement(l);
        RFeatureCall leaf = el.leafReference();
        assertNotNull(leaf, "the leaf is a node now");
        assertEquals("p", leaf.featureName());
        assertSame(el.receiverExpression(), leaf.receiver(), "receiverExpression() is the leaf's receiver — the SAME node the generator read before");
        assertTrue(el.receiverExpression() instanceof RSymbolReference, "…the bare root for a one-hop path");
        assertEquals(List.of(leaf), el.children(), "the children() walk enters through the leaf");
        assertSame(el, leaf.parent());
        RAttribute p = attribute(l, "T", "p");
        assertSame(p, leaf.resolvedFeature().orElseThrow(), "the fork's OWN resolver binds the leaf");
        // the differential's record: the leaf at ITS offset, bound to upstream's Attribute kind and name
        int pAt = l.source.indexOf("t -> p only") + 5;
        assertEquals("Attribute test.T.p", dump(l).get(key(pAt, "RosettaFeatureCall.feature", "p")));
        assertEquals("Attribute test.G.t", dump(l).get(key(pAt - 5, "RosettaSymbolReference.symbol", "t")), "the root record, as before");
    }

    @Test
    void b2_onlyExistsTwoHops_everyHopIsRangedToItsOwnFeature() {
        Linked l = link(PATHS + """
                func G:
                    inputs:
                        t T (1..1)
                    output:
                        ok boolean (1..1)
                    set ok:
                        t -> a -> b only exists
                """);
        ROnlyExistsElement el = onlyElement(l);
        RFeatureCall leaf = el.leafReference();
        assertEquals("b", leaf.featureName());
        RFeatureCall hopA = (RFeatureCall) el.receiverExpression();
        assertSame(hopA, leaf.receiver());
        assertEquals("a", hopA.featureName());
        int aAt = l.source.indexOf("t -> a -> b") + 5;
        int bAt = aAt + 5;
        assertEquals(aAt + 1, hopA.sourceRange().endOffset(), "the receiver hop's range ends at ITS feature (`a`), no longer at the whole element's end");
        assertEquals(bAt + 1, leaf.sourceRange().endOffset(), "the leaf's range ends at `b`");
        Map<String, String> records = dump(l);
        assertEquals("Attribute test.T.a", records.get(key(aAt, "RosettaFeatureCall.feature", "a")));
        assertEquals("Attribute test.A.b", records.get(key(bAt, "RosettaFeatureCall.feature", "b")));
    }

    @Test
    void b3_onlyExistsListForm_everyElementsLeafBinds() {
        Linked l = link(PATHS + """
                func G:
                    inputs:
                        t T (1..1)
                    output:
                        ok boolean (1..1)
                    set ok:
                        (t -> q, t -> r) only exists
                """);
        ROnlyExistsExpr oe = AstWalker.findFirst(l.model, ROnlyExistsExpr.class).orElseThrow();
        assertEquals(2, oe.elements().size());
        assertSame(attribute(l, "T", "q"), oe.elements().get(0).leafReference().resolvedFeature().orElseThrow());
        assertSame(attribute(l, "T", "r"), oe.elements().get(1).leafReference().resolvedFeature().orElseThrow());
        Map<String, String> records = dump(l);
        int qAt = l.source.indexOf("t -> q") + 5;
        int rAt = l.source.indexOf("t -> r") + 5;
        assertEquals("Attribute test.T.q", records.get(key(qAt, "RosettaFeatureCall.feature", "q")));
        assertEquals("Attribute test.T.r", records.get(key(rAt, "RosettaFeatureCall.feature", "r")));
    }

    @Test
    void b4_theItemRootSynthesizesAndBinds_theBareShapeStaysDeclined() {
        // v3.2 seat 12 (D52, H3 - M6): the item root was the banked, declined edge shape until this seat - the
        // released plugin was MEASURED (the hold-out group only-exists-item-root): `item -> p` IS the navigation `p`
        // over the implicit variable. The root is a real, non-synthetic RImplicitVariable ranged at the token; the
        // leaf binds through the type engine (Cat 8 types the literal item inside the extract's implicit body, Cat
        // 1 binds the hop) - the differential's record at the leaf's offset. The bare symbol stays declined.
        Linked l = link(PATHS + """
                func G:
                    inputs:
                        ts T (0..*)
                    output:
                        ok boolean (0..*)
                    add ok:
                        ts extract [ item -> p only exists ]
                    add ok:
                        ts extract [ p only exists ]
                """);
        List<ROnlyExistsElement> els = AstWalker.findAll(l.model, ROnlyExistsElement.class);
        assertEquals(2, els.size());
        ROnlyExistsElement itemRooted = els.get(0);
        assertTrue(itemRooted.isRootItem());
        assertEquals(List.of("p"), itemRooted.featureChain());
        RFeatureCall leaf = itemRooted.leafReference();
        assertNotNull(leaf, "the item root synthesizes its path now (v3.2 seat 12, H3)");
        assertEquals("p", leaf.featureName());
        assertSame(itemRooted.receiverExpression(), leaf.receiver(), "receiverExpression() is the leaf's receiver");
        assertTrue(itemRooted.receiverExpression() instanceof RImplicitVariable, "...the implicit variable for a one-hop item path");
        RImplicitVariable root = (RImplicitVariable) itemRooted.receiverExpression();
        assertFalse(root.isSynthetic(), "the user wrote `item` - never synthetic");
        int itemAt = l.source.indexOf("[ item -> p only exists") + 2;
        assertEquals(itemAt, root.sourceRange().startOffset(), "ranged at the token");
        assertEquals(itemAt + 4, root.sourceRange().endOffset());
        assertEquals(List.of(leaf), itemRooted.children(), "the children() walk enters through the leaf");
        assertSame(attribute(l, "T", "p"), leaf.resolvedFeature().orElseThrow(),
                "the fork's own resolver binds the leaf over the typed implicit item");
        int pAt = itemAt + 8;
        assertEquals("Attribute test.T.p", dump(l).get(key(pAt, "RosettaFeatureCall.feature", "p")),
                "the differential's record at the leaf's offset");
        ROnlyExistsElement bare = els.get(1);
        assertEquals("p", bare.root());
        assertTrue(bare.featureChain().isEmpty());
        assertNull(bare.leafReference(), "the bare shape stays declined (the generator's arm 3 / 4 synthesise its parent; no L2 carrier prices it)");
        assertNull(bare.receiverExpression());
        assertTrue(bare.children().isEmpty());
    }

    // ------------------------------------------------------------- corpus locks --

    @Test
    void corpus_c1_s05Base_theSixLambdaParameterRecords() throws IOException {
        Map<String, Integer> fqns = closureParameterRecords("chaos-s05-base.rosetta");
        Map<String, Integer> expected = new TreeMap<>(Map.of(
                "chaos.s05.base.C5Deep.x", 1, "chaos.s05.base.C5Deep.y", 2,
                "chaos.s05.base.C5Forms.lambdaThen.x", 1, "chaos.s05.base.C5Forms.lambdaIte.x", 2));
        assertEquals(expected, fqns, "the s05 seed's six closure-parameter references, each bound to its ClosureParameter with upstream's qualified name");
    }

    @Test
    void corpus_c2_s20Base_theTwelveLambdaParameterRecords() throws IOException {
        Map<String, Integer> fqns = closureParameterRecords("chaos-s20-base.rosetta");
        Map<String, Integer> expected = new TreeMap<>(Map.of(
                "chaos.s20.base.C20Harvest.t", 1, "chaos.s20.base.C20Harvest.b", 1, "chaos.s20.base.C20Harvest.w", 1, "chaos.s20.base.C20Harvest.l", 1,
                "chaos.s20.base.C20Pipeline.a", 1, "chaos.s20.base.C20Pipeline.b", 1,
                "chaos.s20.base.C20Shade.t", 2, "chaos.s20.base.C20Shade.tb", 1));
        expected.put("chaos.s20.base.C20Spread.hiLeaf.l", 1);
        expected.put("chaos.s20.base.C20Spread.loLeaf.l", 1);
        expected.put("chaos.s20.base.C20Spread.ordered.l", 1);
        assertEquals(expected, fqns, "the s20 seed's twelve closure-parameter references (depth-4 nesting, the two-parameter reduce, the sibling re-use of `t`, the three key lambdas)");
        assertEquals(12, fqns.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void corpus_c3_s21Base_theThreeOnlyExistsLeafRecords() throws IOException {
        Map<String, String> records = chaosDump("chaos-s21-base.rosetta");
        List<String> leafRecords = records.entrySet().stream()
                .filter(e -> e.getKey().contains("RosettaFeatureCall.feature") && e.getValue().startsWith("Attribute chaos.s21.base.C21Paths."))
                .map(e -> e.getKey().substring(e.getKey().indexOf(' ') + 1) + " -> " + e.getValue()).sorted().toList();
        // five feature-call records bind C21Paths attributes: the three only-exists LEAVES (p / q / r, the F15 rows)
        // and C21Void's two `t -> p` navigations, bound before the seat
        assertEquals(List.of(
                "RosettaFeatureCall.feature 'p' -> Attribute chaos.s21.base.C21Paths.p",
                "RosettaFeatureCall.feature 'p' -> Attribute chaos.s21.base.C21Paths.p",
                "RosettaFeatureCall.feature 'p' -> Attribute chaos.s21.base.C21Paths.p",
                "RosettaFeatureCall.feature 'q' -> Attribute chaos.s21.base.C21Paths.q",
                "RosettaFeatureCall.feature 'r' -> Attribute chaos.s21.base.C21Paths.r"), leafRecords);
        String source = Files.readString(CHAOS_SOURCES.resolve("chaos-s21-base.rosetta"));
        int pAt = source.indexOf("t -> p only") + 5;
        assertEquals("Attribute chaos.s21.base.C21Paths.p", records.get(key(pAt, "RosettaFeatureCall.feature", "p")),
                "the leaf record sits at the leaf's own offset — the key the declared row named");
    }

    // ---------------------------------------------------------------- plumbing --

    private record Linked(RModel model, RWorkspace ws, String source, String file) {}

    private static Linked link(String source) {
        String file = "ClosureParameterSeatTest.rosetta";
        RModel model = AstBuilder.buildFromString(source, file);
        RLinkingResult result = RWorkspace.build(List.of(model));
        return new Linked(model, result.workspace(), source, file);
    }

    private static RInlineFunction onlyLambda(Linked l) {
        List<RInlineFunction> lambdas = AstWalker.findAll(l.model, RInlineFunction.class);
        assertEquals(1, lambdas.size(), "exactly one lambda in the fixture");
        return lambdas.get(0);
    }

    private static RSymbolReference onlySymbolRef(Linked l, String name) {
        List<RSymbolReference> refs = AstWalker.findAll(l.model, RSymbolReference.class).stream()
                .filter(r -> name.equals(r.name())).toList();
        assertEquals(1, refs.size(), "exactly one bare reference named " + name);
        return refs.get(0);
    }

    private static REnumValueRef onlyEnumValueRef(Linked l, String head) {
        List<REnumValueRef> refs = AstWalker.findAll(l.model, REnumValueRef.class).stream()
                .filter(r -> head.equals(r.enumName())).toList();
        assertEquals(1, refs.size(), "exactly one `" + head + " -> …` navigation");
        return refs.get(0);
    }

    private static ROnlyExistsElement onlyElement(Linked l) {
        List<ROnlyExistsElement> els = AstWalker.findAll(l.model, ROnlyExistsElement.class);
        assertEquals(1, els.size());
        return els.get(0);
    }

    private static RAttribute attribute(Linked l, String type, String name) {
        return AstWalker.findAll(l.model, RAttribute.class).stream()
                .filter(a -> name.equals(a.name()) && a.parent() != null
                        && a.parent() instanceof com.regnosys.rosetta.ast.types.RDataType dt && type.equals(dt.name()))
                .findFirst().orElseThrow();
    }

    /** The differential's records for the fixture: {@code <offset> <ref kind> '<text>' -> <EClass> <FQN>}. */
    private static Map<String, String> dump(Linked l) {
        return index(ForkResolutionDump.dump(List.of(l.model), Map.of(l.file, l.source)));
    }

    private static Map<String, String> index(List<String> records) {
        Map<String, String> byKey = new LinkedHashMap<>();
        for (String record : records) {
            String[] c = record.split("\t", -1);
            if (c.length == 7) {
                byKey.put(key(Integer.parseInt(c[2]), c[3], c[4]), c[5] + " " + c[6]);
            }
        }
        return byKey;
    }

    private static String key(int offset, String reference, String text) {
        return String.format("%08d %s '%s'", offset, reference, text);
    }

    private static Map<String, String> chaosDump(String fileName) throws IOException {
        Path file = CHAOS_SOURCES.resolve(fileName);
        assumeTrue(Files.isRegularFile(file), "chaos cell not present at " + file + " — checkout incomplete");
        assumeTrue(Files.isDirectory(BUILTINS), "the builtin models must be present at " + BUILTINS);
        String source = Files.readString(file);
        List<RModel> models = new ArrayList<>();
        try (Stream<Path> files = Files.list(BUILTINS)) {
            for (Path b : files.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList()) {
                models.add(AstBuilder.buildFromString(Files.readString(b), b.toString()));
            }
        }
        RModel subject = AstBuilder.buildFromString(source, file.toString());
        models.add(subject);
        RWorkspace.build(models);
        return index(ForkResolutionDump.dump(List.of(subject), Map.of(fileName, source)));
    }

    private static Map<String, Integer> closureParameterRecords(String fileName) throws IOException {
        Map<String, Integer> fqns = new TreeMap<>();
        for (String value : chaosDump(fileName).values()) {
            if (value.startsWith("ClosureParameter ")) {
                fqns.merge(value.substring("ClosureParameter ".length()), 1, Integer::sum);
            }
        }
        return fqns;
    }
}
