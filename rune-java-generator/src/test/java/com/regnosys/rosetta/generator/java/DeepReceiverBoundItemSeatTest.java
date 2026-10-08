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
import java.util.function.Predicate;

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
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 28, law 12 — facet {@code deepReceiverBoundItemFallback}: <b>a deep-arrow ({@code ->>}) call
 * whose RECEIVER is a bare, parser-UNBOUND name on the enclosing lambda's implicit item loses its
 * whole {@code DeepPathUtil} wiring</b> — the rendered lambda, the {@code @Inject} field and the
 * import — and emits {@code /* TODO(M7b-4): wire DeepPathUtil *}{@code /} instead.
 *
 * <p><b>The seat is {@code resolveDeepReceiverJavaClass} ({@code :8837}), NOT
 * {@code handle(RDeepFeatureCall)}'s fallback branch ({@code :1584-1594}).</b> That single resolver
 * feeds BOTH halves: {@code resolveDeepReceiverTypeName} (the renderer's lambda var +
 * {@code <type>DeepPathUtil} reference) and {@code resolveDeepReceiverSymbolId}, which
 * {@code FunctionDependencyCollector:858} uses for the {@code @Inject} field and its namespace.
 * Patching only the renderer would emit {@code indexDeepPathUtil.chooseIdentifier(index)} into a
 * class with no such field and no import — a WORSE non-compiling output than the stub. LAW 69.
 *
 * <p><b>The receiver's type is NOT the bound ITEM type.</b> The probe measured
 * {@code boundItemSelf}/{@code boundItemRecv} = {@code BasketConstituent} (or {@code Observable}) at
 * all 20 fallback rows, but golden's util is {@code IndexDeepPathUtil} at every one of them: the
 * bound item is the ROOT the bare name is re-rooted ONTO, and the receiver is one hop further on.
 * The hop is taken inside {@code resolveReceiverDataType}'s #348
 * {@code ReferenceHandler.synthesizeImplicitItemBareNav} arm — the SAME synthesis that RENDERED the
 * receiver's {@code .<Index>map("getIndex", basketConstituent -> …)} text (so the two cannot
 * disagree), and the SAME walk {@code resolveDeepFeature} already consults fourteen lines away at
 * {@code :8696} to recover the deep FEATURE.
 *
 * <p><b>The settling bytes</b> (drr 7.0.0 {@code GetBasketConstituents}):
 * <pre>
 * golden:20   import cdm.observable.asset.util.IndexDeepPathUtil;
 * golden:73   @Inject protected IndexDeepPathUtil indexDeepPathUtil;
 * golden:128  return item.&lt;Index&gt;map("getIndex", basketConstituent -&gt; basketConstituent.getIndex())
 *                 .&lt;AssetIdentifier&gt;mapC("chooseIdentifier", index -&gt; indexDeepPathUtil.chooseIdentifier(index));
 * fork  :124  return MapperC.of(item.&lt;Index&gt;map("getIndex", basketConstituent -&gt; basketConstituent.getIndex())
 *                 .&lt;AssetIdentifier&gt;mapC("chooseIdentifier", _identifier -&gt; /* TODO(M7b-4)… *&#47; chooseIdentifier(_identifier)));
 *             (no import, no @Inject field)
 * </pre>
 * {@code UnderlierProductIdentifier} is the in-file positive control: its line 271 receiver IS an
 * {@code RFeatureCall}, takes the NAMED branch, and is byte-identical in fork and golden — which is
 * why that file already carries the import and the field while three of its sites still stub.
 *
 * <p><b>ROWS: 0 WHOLE, 12 FILES UNBLOCKED.</b> Every carrier is multi-family:
 * {@code GetBasketConstituents} 19 hunks, {@code UnderlierProductIdentifier} 8,
 * {@code GetUnderlierProductIdentifierLeg1} 3 (two of which are F11's
 * {@code MapperListOfLists}/{@code mapItemToList} cardinality family — including the
 * {@code MapperC.of(…)} re-wrap on the very same line). The PR claim is
 * <b>"12 files unblocked, 0 whole heals, 20 non-compiling stubs removed, both routes"</b> — never
 * "12 heals". Expected LAW-80 line: <b>0 healed / 0 entered / 12 improved / 0 worsened</b>.
 *
 * <p><b>MEASURED OVER-FIRE: ZERO.</b> {@code PROBE28-F16} = 756 lines per route;
 * {@code fellBack=true} occurs exactly 20 times corpus-wide, in exactly those 12 band files
 * (1 + 3 + 1 per cell × drr 7.0-7.3), and NEVER in a green file — cross-checked 1:1 against the 20
 * emitted {@code TODO(M7b-4)} placeholders. The rung is reachable only from that set by
 * construction (it fires only where both existing rungs returned {@code null}).
 *
 * <p><b>LAW 74</b>: the stub is genuinely non-compiling ({@code chooseIdentifier(_identifier)} is an
 * unresolved method on the generated function class), so this law is a real compile fix — record
 * the per-file PRE errors this law claims BEFORE the run.
 * {@code GetUnderlierProductIdentifierLeg1} may still fail on F11's {@code MapperC.of} wrapper.
 *
 * <p><b>NOT this law (do not conflate):</b> {@code GetBasketConstituents} also drops
 * {@code import cdm.base.staticdata.asset.common.Asset} — census F29, a DIFFERENT producer whose
 * probe {@code probe28-verdicts.md} §6 declares MIS-SITED and whose producer is UNPINNED.
 *
 * <p><b>DRAFT STATUS.</b> The corpus expectations are byte-quoted from the LAW-65 dump
 * {@code d28-off}; the FIXTURE expectations are DERIVED and must be confirmed against the actual
 * render at the RED head. In particular <b>a1 and a2 are only witnesses if they reproduce
 * {@code receiverTypeName == null}</b> — if either already takes the NAMED branch at the pre-seat
 * blob it is a decline pin, not a positive fixture, and must be reshaped (a1 first: a lowercase
 * bare feature name may bind where a capitalised choice-option projection does not).
 * {@code DOMAIN_*} are MEASURED and pinned: {@code DOMAIN_DRR7 = 19}, {@code DOMAIN_DRR561 = 0}.
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>the whole law reverted (law12-apply --revert) -> a2, corpus_c1, corpus_control1 (3F)</li>
 * </ul>
 */
class DeepReceiverBoundItemSeatTest {

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

    /** drr 7.0.0 — the cell that carries all three carriers. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    /** drr 5.61.0 — deep calls exist, law-12 rows do not: the over-fire tripwire. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellBAndIrProviderAvailable() {
        return cellBAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // The fixture
    // =========================================================================

    /**
     * a1 = a lowercase bare item-feature receiver; a2 = the carrier-faithful CHOICE-OPTION
     * projection receiver ({@code Idx ->> legs}, mirroring {@code Index ->> identifier} in
     * {@code base-trade-basket-func.rosetta:44-45}); b1 = the NAMED-receiver deep call that must
     * stay byte-identical; b2's assertion rides a1/a2's output (see
     * {@link #b2_theUtilIsDerivedFromTheReceiverTypeNotTheBoundItemType()}).
     *
     * <p>{@code note} not {@code tag} — {@code tag} is a rune lexer keyword.
     */
    private static final String MODEL = """
            namespace census.seat28law12
            version "1.0.0"

            type Leg:
                code string (0..1)
                note string (0..1)

            type Sub:
                legs Leg (0..*)

            type Idx:
                subA Sub (0..1)
                subB Sub (0..1)

            type Ast:
                assetCode string (0..1)

            choice Thing:
                Idx
                Ast

            type Cons:
                idx Idx (0..1)
                thing Thing (0..1)
                note string (0..1)

            type Bskt:
                cons Cons (0..*)
                topIdx Idx (0..1)
                things Thing (0..*)

            func A1BareItemDeepReceiver: <"a1 - the deep receiver is a BARE lowercase feature of the lambda's implicit item">
                inputs:
                    b Bskt (1..1)
                output:
                    result Leg (0..*)
                add result:
                    b -> cons
                        then extract idx ->> legs
                        then flatten

            func A2ChoiceOptionDeepReceiver: <"a2 - the carrier shape: the deep receiver is the bare CHOICE-OPTION projection of a choice-typed item (the UnderlierProductIdentifier class)">
                inputs:
                    b Bskt (1..1)
                output:
                    result Leg (0..*)
                add result:
                    b -> things
                        then extract Idx ->> legs
                        then flatten

            func X1SingleArmTernaryOptionProjection: <"x1 - BANKED gap pin: a single-arm elseless conditional over the option projection renders an inline ternary whose receiver never synthesizes (zero corpus carriers)">
                inputs:
                    b Bskt (1..1)
                output:
                    result Leg (0..*)
                add result:
                    b -> cons
                        then extract
                            if Idx exists
                            then Idx ->> legs
                        then flatten

            func B1NamedReceiverDeepCall: <"b1 - an RFeatureCall receiver: the NAMED branch already works and must NOT move">
                inputs:
                    b Bskt (1..1)
                output:
                    result Leg (0..*)
                add result:
                    b -> topIdx ->> legs
            """;

    // =========================================================================
    // Part A — the fixtures (RED before the flip)
    // =========================================================================

    /**
     * a1 — the bare item-feature receiver. MEASURED AT THE RED HEAD: this shape ALREADY resolves
     * (the lowercase bare feature binds to the item's attribute, so {@code resolveReceiverRType}'s
     * attribute arm answers) — a1 was GREEN pre-law and serves as the pin that the existing
     * resolution path is untouched, exactly the reshape the draft's protocol anticipated.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_bareItemFeatureDeepReceiverWiresTheDeepPathUtil() throws IOException {
        String out = fn("A1BareItemDeepReceiver.java");
        assertContains(out, ".<Leg>mapC(\"chooseLegs\", idx -> idxDeepPathUtil.chooseLegs(idx))");
        assertContains(out, "@Inject protected IdxDeepPathUtil idxDeepPathUtil;");
        assertContains(out, "IdxDeepPathUtil;");
        assertTrue(!out.contains("TODO(M7b-4)"),
                "the DeepPathUtil stub must be gone once the receiver type resolves:\n" + out);
    }

    /**
     * a2 — the carrier-faithful CHOICE-OPTION projection receiver: the item is the CHOICE type
     * ({@code Thing}) and the bare capitalised {@code Idx} projects its option — the
     * {@code extract Index ->> identifier} shape of {@code UnderlierProductIdentifier} (item
     * {@code Observable}, a choice). This is the RED witness the law heals.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_choiceOptionProjectionDeepReceiverWiresTheDeepPathUtil() throws IOException {
        String out = fn("A2ChoiceOptionDeepReceiver.java");
        assertContains(out, "idxDeepPathUtil.chooseLegs(");
        assertContains(out, "@Inject protected IdxDeepPathUtil idxDeepPathUtil;");
        assertTrue(!out.contains("TODO(M7b-4)"),
                "the DeepPathUtil stub must be gone once the receiver type resolves:\n" + out);
    }

    /**
     * x1 — BANKED GAP PIN (fixture-only, ZERO corpus carriers): a SINGLE-ARM elseless conditional
     * over the option projection renders an inline TERNARY ({@code exists(MapperS.of(Idx))
     * .getOrDefault(false) ? MapperS.of(Idx).map(…stub…) : MapperC.of()}) whose receiver never
     * reaches the #348 synthesis at the RENDER seat — {@code MapperS.of(Idx)} is the raw type
     * name, non-compiling. Law 12 fixes the RESOLVER half only, so the stub (and the raw name)
     * survive here. Measured at the seat-28 law-12 head: no corpus file carries this shape (the
     * corpus conditional carriers are all multi-arm guard-return BLOCK lambdas, healed and locked
     * by corpus_c1/control1). This pin fails the moment any law moves the shape — re-pin then
     * (LAW 81), do not weaken.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void x1_singleArmTernaryOptionProjection_bankedGapPin() throws IOException {
        // v3.2 seat 12 (D52, R2): the banked gap is a REFUSAL now - the deep path `->> legs` over a receiver the resolver
        // cannot name refuses at DEEP_PATH_UTIL_UNRESOLVED instead of writing the `TODO(M7b-4)` stub into Java (the
        // chaos M7b class; the heal is seat 13's). The test keeps its name as the seat-28 record of the gap.
        fixture();
        assertTrue(fixture().keySet().stream().noneMatch(k -> k.endsWith("/functions/X1SingleArmTernaryOptionProjection.java")),
                "no X1 file is emitted - the seat refuses");
        String refusal = fixtureErrors.stream()
                .filter(e -> e.startsWith("census/seat28law12/functions/X1SingleArmTernaryOptionProjection.java ")).findFirst()
                .orElseThrow(() -> new AssertionError("the X1 refusal is not among the fixture's errors: " + fixtureErrors));
        assertTrue(refusal.contains("[DEEP_PATH_UTIL_UNRESOLVED]"), refusal);
        assertTrue(refusal.contains("deep path '->> legs'"), refusal);
    }

    // =========================================================================
    // Part B — the decline pins
    // =========================================================================

    /**
     * b1 — the NAMED branch. An {@code RFeatureCall} receiver ({@code b -> topIdx}) already
     * resolves through {@code resolveReceiverRType}'s attribute arm, so this file must be
     * byte-identical before and after: the new rung sits LAST and is unreachable here.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_namedReceiverDeepCallUnmoved() throws IOException {
        String out = fn("B1NamedReceiverDeepCall.java");
        assertContains(out, "MapperS.of(b).<Idx>map(\"getTopIdx\", bskt -> bskt.getTopIdx())"
                + ".<Leg>mapC(\"chooseLegs\", idx -> idxDeepPathUtil.chooseLegs(idx))");
        assertContains(out, "@Inject protected IdxDeepPathUtil idxDeepPathUtil;");
        assertTrue(!out.contains("TODO(M7b-4)"),
                "the NAMED branch never emitted the stub:\n" + out);
    }

    /**
     * b2 — the sharpest control in the suite, and the reason the design does NOT read the bound
     * item type directly. In a1/a2 the enclosing lambda's bound ITEM is {@code Cons}; the deep
     * receiver is {@code Idx}. Golden's rule is receiver-derived, so the emitted util must be
     * {@code IdxDeepPathUtil}/{@code idxDeepPathUtil} and {@code ConsDeepPathUtil} must appear
     * NOWHERE — a bound-item-typed implementation would emit exactly that non-existent class.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_theUtilIsDerivedFromTheReceiverTypeNotTheBoundItemType() throws IOException {
        for (String f : List.of("A1BareItemDeepReceiver.java", "A2ChoiceOptionDeepReceiver.java")) {
            String out = fn(f);
            assertTrue(!out.contains("ConsDeepPathUtil") && !out.contains("consDeepPathUtil"),
                    "the DeepPathUtil must derive from the RECEIVER type (Idx), never from the"
                    + " enclosing lambda's bound item type (Cons), in " + f + ":\n" + out);
            assertTrue(!out.contains("ThingDeepPathUtil") && !out.contains("thingDeepPathUtil"),
                    "nor from the choice-typed item itself (Thing), in " + f + ":\n" + out);
            assertTrue(!out.contains("BsktDeepPathUtil") && !out.contains("bsktDeepPathUtil"),
                    "nor from the function input type, in " + f + ":\n" + out);
        }
    }

    // =========================================================================
    // Part C — the corpus carriers (12 files unblocked; drr 7.0.0 is the scanned cell)
    // =========================================================================

    private static final String CARRIER_GBC =
            "drr/base/trade/basket/functions/GetBasketConstituents.java";
    private static final String CARRIER_UPI =
            "drr/base/trade/underlier/functions/UnderlierProductIdentifier.java";
    private static final String CARRIER_GUPI1 =
            "drr/regulation/common/functions/GetUnderlierProductIdentifierLeg1.java";

    private static final List<String> CARRIERS_B = List.of(CARRIER_GBC, CARRIER_UPI, CARRIER_GUPI1);

    /**
     * control0 — golden is the oracle, so the instrument can fail. Golden carries the import, the
     * {@code @Inject} field and the NAMED lambda at every one of the twenty sites, and carries the
     * stub token nowhere.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control0_goldenIsFullyWired() throws IOException {
        String gbc = Files.readString(GOLDEN_B.resolve(CARRIER_GBC));
        assertTrue(gbc.contains("import cdm.observable.asset.util.IndexDeepPathUtil;"),
                "golden GetBasketConstituents imports IndexDeepPathUtil");
        assertTrue(gbc.contains("@Inject protected IndexDeepPathUtil indexDeepPathUtil;"),
                "golden GetBasketConstituents injects indexDeepPathUtil");
        assertTrue(gbc.contains(".<AssetIdentifier>mapC(\"chooseIdentifier\","
                + " index -> indexDeepPathUtil.chooseIdentifier(index))"),
                "golden GetBasketConstituents renders the NAMED deep lambda");
        for (String p : CARRIERS_B) {
            assertTrue(!Files.readString(GOLDEN_B.resolve(p)).contains("TODO(M7b-4)"),
                    "golden must carry no DeepPathUtil stub: " + p);
        }
    }

    /**
     * c1 — the per-carrier WIRING lock. These files stay in the band on their OTHER families
     * (19 / 8 / 3 hunks; F8, F11, F12, F13, F29), so this is deliberately NOT a byte lock: it locks
     * the three tokens this law owns, per carrier. A byte lock belongs to the seat that lands the
     * LAST family in each file.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c1_carriersAreWiredAndStubFree() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run");
        for (String p : CARRIERS_B) {
            String gen = drrBOutput.get(p);
            assertNotNull(gen, "not generated in drr 7.0.0: " + p);
            String golden = Files.readString(GOLDEN_B.resolve(p));
            assertTrue(!gen.contains("TODO(M7b-4)"),
                    "the DeepPathUtil stub must be gone from " + p + ":\n" + gen);
            assertEquals(count(golden, "DeepPathUtil.choose"), count(gen, "DeepPathUtil.choose"),
                    "wired deep-path call sites must equal golden's in " + p);
            assertEquals(countInjectedUtils(golden), countInjectedUtils(gen),
                    "injected <Type>DeepPathUtil fields must equal golden's in " + p);
            assertTrue(gen.contains("import cdm.observable.asset.util.IndexDeepPathUtil;"),
                    "the @Inject field's import must be registered in " + p);
        }
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the
     * (wired call sites, stub sites, injected fields) triple must equal golden's, file for file.
     * T2 is the under-fire detector; T1 without T3 is the RENDERER-ONLY-FIX detector — the failure
     * mode the seat choice in {@code law12-design.md} §0 exists to prevent.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control1_forkDrr7WholeCellDeepPathSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, DOMAIN_DRR7);
    }

    /** control3 — LAW 79 on drr 5.61.0, a cell with NO law-12 row: nothing may move. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control3_forkDrr561WholeCellDeepPathSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_561, DOMAIN_DRR561);
    }

    /** control2 — LAW 77 route parity for the three carriers. */
    @Test
    @EnabledIf("cellBAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_B_ROOT), new ArrayList<>());
        for (String p : CARRIERS_B) {
            assertEquals(drrBOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /** The NAMED residue of drr 7.0.0 (LAW 73). MEASURED EMPTY at the chain head c6871ed5. */
    private static final List<String> KNOWN_RESIDUE_7 = List.of();

    /** The NAMED residue of drr 5.61.0 — MEASURED EMPTY at the chain head c6871ed5. */
    private static final List<String> KNOWN_RESIDUE_561 = List.of();

    /** MEASURED at the law-12 head: 19 token-bearing files in the drr 7.0.0 fork∪golden union. */
    private static final int DOMAIN_DRR7 = 19;

    /** MEASURED at the law-12 head: drr 5.61.0 carries ZERO deep-path tokens on either side. */
    private static final int DOMAIN_DRR561 = 0;

    /**
     * (T1, T2, T3) = wired deep-path call sites, {@code TODO(M7b-4)} stub sites, injected
     * {@code <Type>DeepPathUtil} fields.
     *
     * <p><b>Scanned on RAW text</b>: the stub token lives inside a {@code /* … *}{@code /} comment,
     * which {@code codeOnly()} (and any comment stripper) deletes. The three tokens are emitted
     * only by the deep-call render and by {@code FunctionDependencyCollector}, so raw scanning
     * carries no false positives.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String raw = e.getValue();
            int t1 = count(raw, "DeepPathUtil.choose");
            int t2 = count(raw, "TODO(M7b-4)");
            int t3 = countInjectedUtils(raw);
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    /** {@code @Inject protected <X>DeepPathUtil <x>DeepPathUtil;} declarations. */
    private static int countInjectedUtils(String java) {
        int n = 0;
        for (String line : java.split("\n")) {
            String s = line.trim();
            if (s.startsWith("@Inject protected ") && s.contains("DeepPathUtil ")
                    && s.endsWith(";")) {
                n++;
            }
        }
        return n;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means "
                        + "an unpinned call site");
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        int[] zero = new int[3];
        for (String key : universe) {
            if (!emittedA.contains(key)) {
                notEmitted.add(key);
                continue;
            }
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "token-bearing files " + bName + " carries that " + aName + " does not emit at all");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-26/27 suite shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_B_ROOT), errs);
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

    /** The cell through the REAL {@code IRGeneration} seams (the D11 ON ring's wiring). */
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
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    /** Strip line and block comments plus string literals — used for ABSENCE asserts only. */
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

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;
    /** The fixture's generation errors at the last render - the declared refusals' messages (v3.2 seat 12). */
    private static List<String> fixtureErrors;
    /** fixture function name -> the register site it refuses at (v3.2 seat 12, D52 R2); asserted as a SET by render(). */
    private static final Map<String, String> DECLARED_REFUSALS = Map.of(
            "X1SingleArmTernaryOptionProjection", "DEEP_PATH_UTIL_UNRESOLVED");

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat28law12.rosetta");
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
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        // v3.2 seat 12 (D52, R2): the fixture's X1SingleArmTernaryOptionProjection REFUSES at DEEP_PATH_UTIL_UNRESOLVED by law
        // now (the `TODO(M7b-4)` stub it pinned as the banked gap is never written). The declared refusal SET is admitted EXACTLY (LAW 73): any other
        // error still fails the fixture loudly, and a declared refusal that did NOT fire fails it too (a heal to re-pin).
        java.util.Set<String> declared = new java.util.TreeSet<>();
        List<String> undeclared = new ArrayList<>();
        for (String e : errors) {
            String fixtureName = DECLARED_REFUSALS.keySet().stream()
                    .filter(n -> e.startsWith("census/seat28law12/functions/" + n + ".java ")).findFirst().orElse(null);
            if (fixtureName != null && e.contains("[" + DECLARED_REFUSALS.get(fixtureName) + "]")) {
                declared.add(fixtureName);
            } else {
                undeclared.add(e);
            }
        }
        if (!undeclared.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + undeclared);
        }
        if (!declared.equals(DECLARED_REFUSALS.keySet())) {
            throw new AssertionError("the declared refusals did not fire as a SET (a heal to re-pin, never to absorb):"
                    + " expected " + DECLARED_REFUSALS + " fired " + declared);
        }
        fixtureErrors = List.copyOf(errors);
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat28law12".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String fn(String fileName) throws IOException {
        return lookup(fixture(), "functions/" + fileName);
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
            throw new AssertionError("[DeepReceiverBoundItemSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
