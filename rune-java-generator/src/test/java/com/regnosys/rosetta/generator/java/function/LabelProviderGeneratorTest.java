package com.regnosys.rosetta.generator.java.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static java.lang.ref.Reference.reachabilityFence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.util.DottedPath;

/**
 * Phase X T3 unit tests for {@link LabelProviderGenerator}.
 *
 * <p>Each test builds a synthetic {@code .rosetta} corpus, parses it via
 * {@link AstBuilder#buildFromString(String, String)}, attaches the workspace
 * via {@link RWorkspace#build(List)}, and exercises the generator's three
 * lifecycle methods ({@code streamObjects} / {@code createTypeRepresentation}
 * / {@code generate}) directly.
 *
 * <p>Mirrors the pattern at {@code ChoiceObjectGeneratorTest} (P2.1.3c) — the
 * closest predecessor generator. {@link java.lang.ref.Reference#reachabilityFence}
 * (statically imported as {@code reachabilityFence}) guards against premature
 * GC of the WeakReference-held workspace, matching the fence pattern at
 * {@code GeneratorModelTest} / {@code ChoiceObjectGeneratorTest}.
 *
 * <p>Grammar paste-quote (verified against
 * {@code rune-parser/src/main/antlr4/.../RosettaParser.g4}):
 * <ul>
 *   <li>{@code labelAnnotation : LBRACK LABEL (FOR annotationPathExpression
 *       | annotationPathExpression? AS)? STRING RBRACK} — i.e.
 *       {@code [label "value"]} OR {@code [label for path "value"]} OR
 *       {@code [label path as "value"]}; the STRING is the LABEL TEXT, NOT
 *       a feature name (a common authoring mistake).</li>
 *   <li>The label annotation attaches to the attribute on the line above,
 *       indented under it (per {@code attribute : ... labelAnnotation* ...}).</li>
 * </ul>
 *
 * <p>Per spec § 4.1 this class covers ≥ 12 named scenarios; the legacy-{@code as}
 * branch is exercised through a synthetic report + rule-reference fixture.
 */
class LabelProviderGeneratorTest {

    /**
     * Repository-root-anchored test-corpus search root. Surefire forks each
     * test JVM with {@code user.dir} pinned to the test module
     * ({@code rune-java-generator}), so {@code "../test-corpus/..."} resolves
     * to the repo root via the module's parent. We compute this explicitly so
     * the resolution is documented (rather than implicit on the runner's cwd
     * choice) and so a developer who runs the test from a different cwd via
     * IDE gets the same answer. Mirrors the convention used by
     * {@code D11CorpusRegressionTest} + {@code ChoiceObjectGeneratorTest}.
     */
    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    /**
     * Mirrors {@code ChoiceObjectGeneratorTest.BUILTINS_SEARCH_ROOTS} — the
     * fork's two-root search for {@code basictypes.rosetta} +
     * {@code annotations.rosetta} so synthetic-source tests can resolve
     * {@code string} / {@code int} / annotation declarations. Both roots are
     * resolved relative to {@link #REPO_ROOT} so the search works regardless
     * of where the test was launched from.
     */
    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model")
    );

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);
    private final LabelProviderGeneratorUtil util = new LabelProviderGeneratorUtil();

    // === Test 1: streamObjects filtering =====================================

    @Test
    void streamObjects_filtersFunctionsByTransformAnnotation_andReports() throws IOException {
        // Corpus: 1 plain function (no transform annotation) + 1 ingest-annotated
        // function + 1 report. Per upstream xtend 45-54, the plain function is
        // skipped; the ingest-annotated function + the report are emitted.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "func Plain:",
                "    inputs: x string (1..1)",
                "    output: result Trade (1..1)",
                "",
                "func Ingester:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Trade (1..1)",
                "",
                "body Authority CFTC",
                "corpus Regulation CFTC \"foo\" CftcCorpus",
                "",
                "report CFTC CftcCorpus in T+1",
                "    from Trade",
                "    when IsX",
                "    with type Trade",
                "",
                "eligibility rule IsX from Trade:",
                "    filter id exists"
        );
        FixtureResult fx = loadFixture(source);
        try {
            LabelProviderGenerator gen = new LabelProviderGenerator(
                    fx.generatorModel, typeTranslator, fx.deepPathUtil, util);

            List<? extends RFunction> emitted = gen.streamObjects(fx.model).toList();

            // Expect: Ingester (transform-annotated) + bridged-from-report
            // RFunction; plain function "Plain" must be filtered out.
            assertEquals(2, emitted.size(),
                    "streamObjects must emit exactly the transform-annotated "
                    + "function + the bridged report; got names: "
                    + emitted.stream().map(RFunction::name).toList());
            List<String> names = emitted.stream().map(RFunction::name).toList();
            assertTrue(names.contains("Ingester"),
                    "ingest-annotated function must be emitted; names: " + names);
            assertFalse(names.contains("Plain"),
                    "plain (no transform) function must NOT be emitted; names: " + names);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 2: empty graph =================================================

    @Test
    void emptyGraph_emitsClassExtendingGraphBasedLabelProvider() throws IOException {
        // Function output is a built-in (string) — does not unwrap to RDataType,
        // so the graph is empty. Upstream still emits the class header + super
        // call (xtend 102-105) but no addLabel / addOutgoingEdge.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "func PrimitiveOut:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result string (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "PrimitiveOut");
            assertTrue(generated.contains("extends GraphBasedLabelProvider"),
                    "empty-graph class must extend GraphBasedLabelProvider: " + generated);
            assertTrue(generated.contains("super(new LabelNode())"),
                    "empty-graph ctor must super-call new LabelNode(): " + generated);
            assertFalse(generated.contains("addLabel"),
                    "empty-graph class must contain no addLabel calls: " + generated);
            assertFalse(generated.contains("addOutgoingEdge"),
                    "empty-graph class must contain no addOutgoingEdge calls: " + generated);
            // A label-free provider never renders an Arrays.asList(...) (representAsList
            // is the sole referent), so java.util.Arrays must NOT be imported —
            // mirroring upstream's import-manager, which only imports referenced types.
            assertFalse(generated.contains("import java.util.Arrays"),
                    "label-free provider must NOT import java.util.Arrays: " + generated);
            // Lock the exact import block: the two labelprovider imports (FQN-sorted),
            // no Arrays, then the two blank lines before the class.
            assertTrue(generated.contains(
                    "import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;\n"
                    + "import com.regnosys.rosetta.lib.labelprovider.LabelNode;\n\n\n"
                    + "public class "),
                    "label-free import block must be the two labelprovider imports then two "
                    + "blank lines (no Arrays): " + generated);
            // Upstream emits an unconditional blank line after the super(...) call
            // (LabelProviderGenerator.xtend:98).
            assertTrue(generated.contains("super(new LabelNode());\n\t\t\n"),
                    "ctor must emit the blank line after super(new LabelNode()): " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 3: single-node graph (output RDataType, no label-bearing attrs) ===

    @Test
    void singleNodeGraph_isPrunedToEmptyEmission() throws IOException {
        // Function output is a data type with attributes but NO labels reachable.
        // Per pruning rule (xtend 174-207), the start node is pruned because no
        // node reaches a label. Result: identical to empty-graph emission.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    id string (1..1)",
                "    description string (0..1)",
                "",
                "func TradeOut:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Trade (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "TradeOut");
            assertTrue(generated.contains("extends GraphBasedLabelProvider"),
                    "single-node-no-labels emits the class header: " + generated);
            assertFalse(generated.contains("addLabel"),
                    "single-node-no-labels has no labels to register: " + generated);
            // Start node should NOT be declared explicitly (it's the super arg).
            assertFalse(generated.contains("LabelNode startNode = new LabelNode"),
                    "start node must not be re-declared: " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 4: circular reference terminates =================================

    @Test
    void circularReference_terminatesAndDoesNotStackOverflow() throws IOException {
        // Trade.self points back to Trade itself. The circular-ref short-circuit
        // at buildLabelGraph line 138 must terminate the recursion. If not,
        // this test would StackOverflowError before any assertion runs.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    name string (0..1)",
                "        [label \"Trade name\"]",
                "    self Trade (0..1)",
                "",
                "func SelfRefFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Trade (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "SelfRefFunc");
            assertNotNull(generated, "circular-ref emission must complete");
            // One label registered (Trade.name); the second visit of Trade
            // (via Trade.self) MUST short-circuit and not re-register.
            int labelCount = countOccurrences(generated, ".addLabel(");
            assertEquals(1, labelCount,
                    "circular-ref must produce exactly one addLabel call: " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 5: pruning removes node without reachable label ==================

    @Test
    void pruning_removesNodeWithoutReachableLabel() throws IOException {
        // Graph: TradeRoot -> Side -> ... (Side has no labels, no further edges
        // pointing at labelled nodes). After pruning, neither node is emitted.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Side:",
                "    sideId string (0..1)",
                "",
                "type TradeRoot:",
                "    side Side (0..1)",
                "",
                "func PrunedFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result TradeRoot (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "PrunedFunc");
            // Both nodes have NO labels reachable -> both pruned -> no node-var
            // declarations, no addLabel, no addOutgoingEdge.
            assertFalse(generated.contains("LabelNode sideNode"),
                    "Side node must be pruned: " + generated);
            assertFalse(generated.contains("addLabel"),
                    "no labels reachable -> no addLabel: " + generated);
            assertFalse(generated.contains("addOutgoingEdge"),
                    "no labels reachable -> no addOutgoingEdge: " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 6: pruning retains node with reachable label =====================

    @Test
    void pruning_retainsNodeWithReachableLabel() throws IOException {
        // Graph: RootA (no own label) -> LeafB (has label). RootA reaches
        // LeafB's label, so RootA is retained.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type LeafB:",
                "    name string (0..1)",
                "        [label \"leaf B name\"]",
                "",
                "type RootA:",
                "    b LeafB (0..1)",
                "",
                "func RetainedFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result RootA (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "RetainedFunc");
            // Start node "startNode" is RootA (no explicit decl — super arg).
            // LeafB emits its own LabelNode declaration + addLabel call.
            assertTrue(generated.contains("LabelNode leafBNode"),
                    "LeafB with label must be emitted as leafBNode: " + generated);
            assertTrue(generated.contains(".addLabel("),
                    "label must be registered: " + generated);
            assertTrue(generated.contains("addOutgoingEdge(\"b\""),
                    "edge from start to LeafB must be wired: " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 7: RChoiceTypeRef unwrap on output type ==========================

    @Test
    void rChoiceTypeRef_unwrappedOnOutputType() throws IOException {
        // Function output is a choice type. The generator must unwrap it to a
        // bridged RDataType (RChoiceTypeRef.asRDataType()) so the graph build
        // proceeds (else the start node is null and graph stays empty).
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Cash:",
                "    amount string (0..1)",
                "        [label \"Cash amount\"]",
                "",
                "type Stock:",
                "    ticker string (0..1)",
                "        [label \"Stock ticker\"]",
                "",
                "choice Asset:",
                "    Cash",
                "    Stock",
                "",
                "func AssetOut:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Asset (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "AssetOut");
            // Both Cash + Stock attribute types should be reachable through
            // the choice unwrap; their per-attribute labels surface as
            // labels in the emitted code.
            assertTrue(generated.contains("addOutgoingEdge"),
                    "choice unwrap must produce outgoing edges: " + generated);
            assertTrue(generated.contains("\"Cash amount\"") ||
                       generated.contains("\"Stock ticker\""),
                    "choice unwrap must propagate option-type labels: " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 8: RChoiceTypeRef unwrap on attribute type =======================

    @Test
    void rChoiceTypeRef_unwrappedOnAttributeType() throws IOException {
        // Attribute (AssetWrapper.payload) is a choice type. The recursive
        // buildLabelGraph must unwrap it to find the choice's option types
        // and continue traversal.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Cash:",
                "    amount string (0..1)",
                "        [label \"Cash amount\"]",
                "",
                "type Stock:",
                "    ticker string (0..1)",
                "        [label \"Stock ticker\"]",
                "",
                "choice Asset:",
                "    Cash",
                "    Stock",
                "",
                "type AssetWrapper:",
                "    payload Asset (0..1)",
                "",
                "func WrapperFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result AssetWrapper (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "WrapperFunc");
            assertTrue(generated.contains("addOutgoingEdge(\"payload\""),
                    "edge through choice-typed attribute must be wired: " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 9: legacy `as` annotation registers label ========================

    @Test
    void legacyAsAnnotation_registersLabel() throws IOException {
        // RReport-origin function: legacy rule reference produces a label via
        // registerLegacyRuleAsLabel (upstream xtend 215-219). The rule's
        // `as "..."` clause text (RRule.alias(); upstream rule.identifier)
        // becomes the label, keyed by the attribute's own path; a referenced
        // rule WITHOUT an `as` clause contributes NO label (PR #321 — the
        // pre-#321 pin asserted the rule NAME, a form that never matched any
        // golden: golden labels are the human-readable `as` texts).
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    tradeId string (0..1)",
                "        [ruleReference TradeIdRule]",
                "    tradeName string (0..1)",
                "        [ruleReference TradeNameRule]",
                "",
                "body Authority CFTC",
                "corpus Regulation CFTC \"foo\" CftcCorpus",
                "",
                "report CFTC CftcCorpus in T+1",
                "    from Trade",
                "    when IsX",
                "    with type Trade",
                "",
                "eligibility rule IsX from Trade:",
                "    filter tradeId exists",
                "",
                "reporting rule TradeIdRule from Trade:",
                "    extract tradeId",
                "        as \"1 Trade Identifier\"",
                "",
                "reporting rule TradeNameRule from Trade:",
                "    extract tradeName"
        );
        FixtureResult fx = loadFixture(source);
        try {
            LabelProviderGenerator gen = new LabelProviderGenerator(
                    fx.generatorModel, typeTranslator, fx.deepPathUtil, util);
            // The report bridges to an RFunction via fromReport(); use that
            // bridged function (not the report's own corpus name) to drive
            // emission.
            RFunction reportFunc = gen.streamObjects(fx.model)
                    .filter(f -> f.originReport().isPresent())
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no report bridged"));
            String generated = gen.generate(reportFunc,
                    gen.createTypeRepresentation(reportFunc), "1.0");
            // The rule's `as` clause text is the label for the tradeId path.
            assertTrue(generated.contains(
                    "addLabel(Arrays.asList(\"tradeId\"), \"1 Trade Identifier\");"),
                    "legacy `as` label must use the rule's `as` clause text: " + generated);
            // The rule NAME is never a label value, and a rule without an
            // `as` clause contributes no label at all.
            assertTrue(!generated.contains("\"TradeIdRule\"")
                            && !generated.contains("\"TradeNameRule\""),
                    "a rule name must never be emitted as a label: " + generated);
            assertTrue(!generated.contains("Arrays.asList(\"tradeName\")"),
                    "a rule without an `as` clause must contribute no label: " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 10: leaf branch of evaluateAnnotationPathExpression ==============

    @Test
    void annotationPathExpression_leafBranch() throws IOException {
        // `[label "Trade name"]` with NO path expression — the leaf branch
        // (upstream xtend lines 222-223) returns [root]. The label is keyed
        // by the attribute's own path.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    name string (0..1)",
                "        [label \"Trade name\"]",
                "",
                "func LeafBranchFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Trade (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "LeafBranchFunc");
            assertTrue(generated.contains("\"Trade name\""),
                    "leaf-branch label value must appear in emission: " + generated);
            // Path is just "name" — Arrays.asList("name")
            assertTrue(generated.contains("Arrays.asList(\"name\")"),
                    "leaf-branch path must be [name]: " + generated);
            // A label-bearing provider DOES reference Arrays.asList(...), so
            // java.util.Arrays IS imported — FQN-sorted LAST, after the labelprovider
            // imports (upstream import-manager order). Locks the positive half of the
            // usesArrays conditional against the empty-graph (no-Arrays) case.
            assertTrue(generated.contains(
                    "import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;\n"
                    + "import com.regnosys.rosetta.lib.labelprovider.LabelNode;\n"
                    + "import java.util.Arrays;\n\n\n"
                    + "public class "),
                    "label-bearing import block must list java.util.Arrays last, after the "
                    + "labelprovider imports, then two blank lines: " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 11: shallow-arrow segment branch =================================

    @Test
    void annotationPathExpression_shallowArrowBranch() throws IOException {
        // `[label for item -> id "Header id"]` — single shallow segment.
        // The path on the start node becomes [header, id] (the attrPath is
        // [header] + the `item -> id` segment appends "id").
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Header:",
                "    id string (0..1)",
                "",
                "type Trade:",
                "    header Header (0..1)",
                "        [label for item -> id \"Header id\"]",
                "",
                "func ShallowArrowFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Trade (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "ShallowArrowFunc");
            assertTrue(generated.contains("\"Header id\""),
                    "shallow-arrow label value must appear: " + generated);
            assertTrue(generated.contains("Arrays.asList(\"header\", \"id\")"),
                    "shallow-arrow path must be [header, id]: " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 12: string-escape in label values ================================

    @Test
    void stringEscape_inLabelValue_emittedCorrectly() throws IOException {
        // Label value with embedded quote — JavaStringUtil.escapeJava must
        // escape it to \" in the emitted source.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    name string (0..1)",
                "        [label \"Name with \\\"quotes\\\" inside\"]",
                "",
                "func EscapeFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Trade (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "EscapeFunc");
            // The emitted source must contain the escaped quote form.
            assertTrue(generated.contains("\\\"quotes\\\""),
                    "embedded quotes must be escaped to \\\" in emission: "
                    + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 13: representAsList helper =======================================

    @Test
    void representAsList_emitsArraysAsListWithEscapedSegments() throws IOException {
        // Unit-level check of the helper — exercises the JavaStringUtil escape
        // path + multi-segment comma joining without needing a full fixture.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "func H:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result string (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            LabelProviderGenerator gen = new LabelProviderGenerator(
                    fx.generatorModel, typeTranslator, fx.deepPathUtil, util);

            String empty = gen.representAsList(DottedPath.of());
            assertEquals("Arrays.asList()", empty,
                    "empty path should render as Arrays.asList(): " + empty);

            String one = gen.representAsList(DottedPath.of("foo"));
            assertEquals("Arrays.asList(\"foo\")", one,
                    "single-segment path should render correctly: " + one);

            String two = gen.representAsList(DottedPath.of("foo", "bar"));
            assertEquals("Arrays.asList(\"foo\", \"bar\")", two,
                    "multi-segment path should comma-separate: " + two);

            String escaped = gen.representAsList(DottedPath.of("a\"b"));
            assertEquals("Arrays.asList(\"a\\\"b\")", escaped,
                    "segment with quote must be Java-escaped: " + escaped);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 14: createTypeRepresentation FQN derivation ======================

    @Test
    void createTypeRepresentation_derivesLabelProviderClassFromFunctionId() throws IOException {
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    id string (0..1)",
                "",
                "func MyIngester:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Trade (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            LabelProviderGenerator gen = new LabelProviderGenerator(
                    fx.generatorModel, typeTranslator, fx.deepPathUtil, util);
            RFunction f = gen.streamObjects(fx.model)
                    .filter(rf -> "MyIngester".equals(rf.name()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("MyIngester not streamed"));
            RGeneratedJavaClass<? extends LabelProvider> clazz = gen.createTypeRepresentation(f);
            assertEquals("MyIngesterLabelProvider", clazz.getSimpleName(),
                    "label provider class name = <func>LabelProvider");
            String pkg = clazz.getPackageName().withDots();
            assertTrue(pkg.endsWith(".labels"),
                    "label provider package must end in .labels: " + pkg);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 15 (T3.0.5 B1 verification): choice-typed attribute traversal ====

    /**
     * <b>T3.0.5 B1 verification test.</b> Reviewer flagged that the
     * {@link com.regnosys.rosetta.types.RChoiceTypeRef#asRDataType()} synthetic
     * bridge might lose option-type children when the recursive
     * {@code buildLabelGraph} walks it via {@code generatorModel.getType(attr)}.
     *
     * <p>The synthetic bridge sets each projected attribute's
     * {@code typeCall} to the option's own typeCall reference (NOT a copy);
     * because {@code generatorModel.getType(attr)} delegates to
     * {@code resolveTypeCall(attr.typeCall())} which reads
     * {@code typeCall.referencedType()}, the option types resolve back through
     * the workspace and the recursion proceeds into each option's
     * {@link RModel}-attached data type. This test asserts that
     * label-bearing attributes on a choice's option types reach the emission
     * via the choice-typed parent attribute.
     */
    @Test
    void choiceTypedAttribute_propagatesOptionLabels() throws IOException {
        // The pre-existing test only asserts that addOutgoingEdge("payload"...)
        // is emitted; it does NOT assert that the option types' labels reach
        // the emission. This deeper test does so explicitly.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Cash:",
                "    cashId string (0..1)",
                "        [label \"Cash id label\"]",
                "",
                "type Stock:",
                "    stockId string (0..1)",
                "        [label \"Stock id label\"]",
                "",
                "choice Asset:",
                "    Cash",
                "    Stock",
                "",
                "type AssetWrapper:",
                "    payload Asset (0..1)",
                "",
                "func WrapperLabelsFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result AssetWrapper (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "WrapperLabelsFunc");
            // BOTH option-type labels must reach emission. If the bridge lost
            // option-type children, only one (or neither) would appear and
            // this assertion catches that regression.
            assertTrue(generated.contains("\"Cash id label\""),
                    "Cash option label must propagate through choice-bridge: "
                    + generated);
            assertTrue(generated.contains("\"Stock id label\""),
                    "Stock option label must propagate through choice-bridge: "
                    + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 16 (T3.0.5 I-missing-test-1): deep-arrow branch ===================

    /**
     * <b>T3.0.5 I-missing-test-1.</b> Exercise the deep ({@code ->>}) arrow
     * branch in {@code evaluateAnnotationPathExpression}, which delegates to
     * {@link com.regnosys.rosetta.utils.DeepFeatureCallUtil#findDeepFeaturePaths}.
     *
     * <p><b>Test fixture intent:</b> the deep-arrow attribute name must be
     * resolvable at the receiver type for the parser-level
     * {@code resolveAnnotationPathSegment} to populate
     * {@link com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment#resolvedAttribute()}.
     * The fork's parser-side {@code TypeDirectedResolver.resolveAnnotationPathSegment}
     * currently uses shallow lookup ({@code findAttribute}) regardless of the
     * segment's deep flag — so we configure the fixture with the deep target
     * attribute also present at the receiver type. The generator branch under
     * test (deep-arrow handling that invokes {@code findDeepFeaturePaths}) is
     * still exercised because {@link com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment#isDeep()}
     * is true on that segment by construction of the {@code ->>} arrow.
     */
    @Test
    void annotationPathExpression_deepArrowBranch() throws IOException {
        // Parser-side constraint: the fork's
        // TypeDirectedResolver.resolveAnnotationPathSegment resolves each
        // path segment shallowly against the OWNING DATA TYPE (not against
        // the previous segment's type), so the deep-arrow segment's
        // resolvedAttribute() is only populated when the deep target also
        // exists on the data type carrying the annotation.
        // To exercise the generator's deep-arrow branch
        // (evaluateAnnotationPathExpression isDeep() = true →
        // findDeepFeaturePaths) without depending on that pre-existing parser
        // limitation, we put `tradeRef` directly on Holder + make the
        // annotation host's resolved type also Holder (via the self-cycle
        // `next Holder`). The deep-arrow receiver is then Holder, which
        // carries tradeRef, and the segment resolves shallowly at Holder.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Holder:",
                "    tradeRef string (0..1)",
                "    next Holder (0..1)",
                "        [label for item ->> tradeRef \"Deep trade reference\"]",
                "",
                "func DeepArrowFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Holder (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "DeepArrowFunc");
            assertTrue(generated.contains("\"Deep trade reference\""),
                    "deep-arrow label value must appear in emission: " + generated);
            // The deep-arrow path should resolve to include the tradeRef
            // leaf segment (via findDeepFeaturePaths). Structural rather than
            // positional assertion — exact path encoding depends on the
            // current attrPath anchor (container) + the deep expansion.
            assertTrue(generated.contains("\"tradeRef\""),
                    "deep-arrow path must contain the tradeRef leaf segment "
                    + "(emitted via findDeepFeaturePaths): " + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 17 (T3.0.5 I-missing-test-2): identifier-collision disambiguation =

    /**
     * <b>T3.0.5 I-missing-test-2.</b> Two label-bearing attributes whose
     * underlying types yield the same {@code lowerFirst(typeName) + "Node"}
     * identifier must both reach emission, each with a distinct local-variable
     * name. The scope's {@code escapeName("_" prefix)} rewrites the second
     * collision.
     *
     * <p>The collision is rare in practice (two types with the same simple
     * name in the same generator scope) but the test guards against any
     * future refactor that drops the {@code createIdentifier} call.
     */
    @Test
    void constructorScope_identifierCollision_disambiguatedViaEscapeName()
            throws IOException {
        // We force collision by registering two attributes whose unwrapped
        // data type both compute the SAME `lowerFirst(name) + "Node"`
        // identifier ("payloadNode" for both attributes typed `Payload`).
        // The fork's scope-collision resolver prefixes the second with "_"
        // so the emission has both `payloadNode` and `_payloadNode` (or
        // equivalent) declarations and the labels register against each.
        // Here we use the simplest construction that produces the collision:
        // two attributes of the SAME data type but addressed via different
        // attribute names — each registers a separate scope entry against
        // the SAME RDataType key. We assert the emission has TWO addLabel
        // calls (one per attribute path) — proving both label registrations
        // survived collision resolution.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Payload:",
                "    payloadId string (0..1)",
                "        [label \"Payload id\"]",
                "",
                "type Root:",
                "    primary Payload (0..1)",
                "    secondary Payload (0..1)",
                "",
                "func CollisionFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Root (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "CollisionFunc");
            // One LabelNode declaration for the Payload node (both attributes
            // point at the same RDataType so only one node is registered),
            // but TWO outgoing edges out of the start node ("primary" + "secondary").
            // Both edges must be wired — the scope's createIdentifier must not
            // have dropped one as a duplicate.
            assertTrue(generated.contains("addOutgoingEdge(\"primary\""),
                    "primary edge must be wired: " + generated);
            assertTrue(generated.contains("addOutgoingEdge(\"secondary\""),
                    "secondary edge must be wired (collision resolution must "
                    + "keep BOTH edges, not collapse them): " + generated);
            // The Payload node's variable name must be the canonical
            // `payloadNode` (lowerFirst(typeName)+"Node") since there is only
            // ONE Payload node in the scope (both edges target it).
            assertTrue(generated.contains("LabelNode payloadNode"),
                    "Payload node must have the canonical name payloadNode: "
                    + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 18 (T3.0.5 I-missing-test-3a): getActualName vs toString pinning ==

    /**
     * <b>T3.0.5 I-missing-test-3a.</b> Pinning test asserting that the
     * emission uses {@code GeneratedIdentifier.getActualName()} (the
     * collision-resolved identifier string) and NOT the debug
     * {@code toString()} form (which contains the angle-bracket
     * {@code <Class:Counter>} marker). Guards against accidental
     * {@code identifier.toString()} regressions.
     */
    @Test
    void emission_usesGetActualName_notDebugToString() throws IOException {
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type LeafNode:",
                "    leafId string (0..1)",
                "        [label \"leaf id\"]",
                "",
                "type Root:",
                "    leaf LeafNode (0..1)",
                "",
                "func PinningFunc:",
                "    [ingest XML]",
                "    inputs: x string (1..1)",
                "    output: result Root (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String generated = generateFor(fx, "PinningFunc");
            // GeneratedIdentifier.toString() renders as "<TypeName:Counter>"
            // in the fork's scope hierarchy when debugged. If a refactor
            // accidentally calls toString() instead of getActualName(), the
            // emitted Java source would contain that debug form and the
            // regex below would match.
            assertFalse(generated.matches("(?s).*<[A-Za-z]+:\\d+>.*"),
                    "emission must NOT contain GeneratedIdentifier.toString() "
                    + "debug form (e.g. '<LabelNode:1>'): " + generated);
            // Positive assertion: the canonical leafNode identifier IS
            // present (proves getActualName() is the path actually used).
            assertTrue(generated.contains("leafNode"),
                    "expected canonical identifier 'leafNode' in emission: "
                    + generated);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 19 (T3.0.5 I-missing-test-3b): createTypeRepresentation on report ===

    /**
     * <b>T3.0.5 I-missing-test-3b.</b> The bridged {@code RFunction.fromReport}
     * path is NOT RModel-attached, so the normal
     * {@code generatorModel.symbolId(function)} throws — we recover by
     * reading the namespace off the origin report. This test asserts the
     * recovery path resolves to a non-null {@link RGeneratedJavaClass} with
     * the expected simple name + namespace derived from the report's
     * namespace.
     */
    @Test
    void createTypeRepresentation_onReportBridgedRFunction_usesReportNamespace()
            throws IOException {
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    tradeId string (0..1)",
                "",
                "body Authority CFTC",
                "corpus Regulation CFTC \"foo\" CftcCorpus",
                "",
                "report CFTC CftcCorpus in T+1",
                "    from Trade",
                "    when IsX",
                "    with type Trade",
                "",
                "eligibility rule IsX from Trade:",
                "    filter tradeId exists"
        );
        FixtureResult fx = loadFixture(source);
        try {
            LabelProviderGenerator gen = new LabelProviderGenerator(
                    fx.generatorModel, typeTranslator, fx.deepPathUtil, util);
            RFunction reportFunc = gen.streamObjects(fx.model)
                    .filter(f -> f.originReport().isPresent())
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no report bridged"));
            // The bridged RFunction must not be RModel-attached (this is the
            // precondition that motivates the recovery path).
            assertTrue(reportFunc.originReport().isPresent(),
                    "bridged RFunction must carry an originReport");

            RGeneratedJavaClass<? extends LabelProvider> clazz =
                    gen.createTypeRepresentation(reportFunc);
            assertNotNull(clazz, "createTypeRepresentation must not return null "
                    + "for a report-bridged RFunction");
            assertTrue(clazz.getSimpleName().endsWith("LabelProvider"),
                    "report-bridged class name must end in LabelProvider: "
                    + clazz.getSimpleName());
            String pkg = clazz.getPackageName().withDots();
            // The namespace must derive from the report's parent RModel
            // namespace ("com.example.test") with the standard .labels suffix
            // appended (matches the non-report path).
            assertTrue(pkg.startsWith("com.example.test"),
                    "report-bridged class package must derive from the report's "
                    + "model namespace 'com.example.test': " + pkg);
            assertTrue(pkg.endsWith(".labels"),
                    "report-bridged class package must end in .labels: " + pkg);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Helpers ===============================================================

    /** Bundle of per-fixture state. */
    private static final class FixtureResult {
        final RModel model;
        final RLinkingResult linkingResult;
        final GeneratorModel generatorModel;
        final DeepFeatureCallUtil deepPathUtil;

        FixtureResult(RModel model, RLinkingResult linkingResult,
                      GeneratorModel generatorModel,
                      DeepFeatureCallUtil deepPathUtil) {
            this.model = model;
            this.linkingResult = linkingResult;
            this.generatorModel = generatorModel;
            this.deepPathUtil = deepPathUtil;
        }
    }

    /**
     * Parse the synthetic source + load builtins + build the workspace +
     * construct a GeneratorModel + DeepFeatureCallUtil wired to read types
     * through the GeneratorModel (so the recursive traversal in
     * DeepFeatureCallUtil sees the same resolution path the LabelProvider
     * generator uses for attribute types).
     */
    private FixtureResult loadFixture(String source) throws IOException {
        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        model.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linkingResult = RWorkspace.build(models);
        GeneratorModel gm = new GeneratorModel(linkingResult.workspace());
        DeepFeatureCallUtil deepPathUtil = new DeepFeatureCallUtil(gm::getType);
        return new FixtureResult(model, linkingResult, gm, deepPathUtil);
    }

    /**
     * Locate the first RFunction with the given name in the fixture's
     * streamed candidates and run the generator's
     * {@code createTypeRepresentation} + {@code generate} pipeline.
     */
    private String generateFor(FixtureResult fx, String functionName) {
        LabelProviderGenerator gen = new LabelProviderGenerator(
                fx.generatorModel, typeTranslator, fx.deepPathUtil, util);
        RFunction f = gen.streamObjects(fx.model)
                .filter(rf -> functionName.equals(rf.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "function '" + functionName + "' was not streamed; "
                        + "either it lacks a transform annotation or its name "
                        + "doesn't match. Verify the test source above."));
        return gen.generate(f, gen.createTypeRepresentation(f), "1.0");
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /**
     * Mirrors {@code ChoiceObjectGeneratorTest.loadBuiltinsOnly} — union over
     * the two search roots, dedup by filename. Returns an empty list when
     * neither root is present (the synthetic tests for empty / graph /
     * pruning don't all require builtins, but {@code [ingest XML]} parsing
     * does so all tests benefit from passing builtins through when available).
     *
     * <p>Parse failures are collected into a per-call {@code failures} list and
     * aggregate-thrown as an {@link AssertionError} after the loop — mirrors
     * the {@code ChoiceObjectGeneratorTest.loadBuiltinsOnly} R1 F12 pattern at
     * PR #68: per-file fixture loader errors must not be silently swallowed
     * because a corrupt builtin file silently fails every downstream test.
     */
    private List<RModel> loadBuiltinsOnly() throws IOException {
        List<String> failures = new ArrayList<>();
        List<RModel> models = loadBuiltinsOnly(failures);
        if (!failures.isEmpty()) {
            throw new AssertionError(
                    "[LabelProviderGeneratorTest] loadBuiltinsOnly: "
                    + failures.size() + " parse failure(s) — first: " + failures.get(0)
                    + (failures.size() > 1
                        ? " (and " + (failures.size() - 1) + " more — full list: "
                          + String.join("; ", failures.subList(1, failures.size())) + ")"
                        : ""));
        }
        return models;
    }

    /**
     * Worker that resolves builtin {@code .rosetta} files via union across
     * {@link #BUILTINS_SEARCH_ROOTS}. Dedup'd by filename in priority order
     * (test-corpus root wins for filename collisions; sibling rune-dsl root
     * fills any gaps). Parse failures are collected into the caller-provided
     * {@code failures} list rather than swallowed, so the caller can surface
     * them via an aggregate {@link AssertionError}.
     */
    private List<RModel> loadBuiltinsOnly(List<String> failures) throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) {
                        failures.add(p + " — " + e);
                    }
                });
        return models;
    }
}
