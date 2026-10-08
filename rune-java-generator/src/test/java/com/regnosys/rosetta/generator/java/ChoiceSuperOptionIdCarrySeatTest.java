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
 * SEAT 29, law 6 — facet {@code choiceSuperOptionIdCarry}: <b>the PR #207 choice-SUPERTYPE option
 * projection carries the option's linker id and attaches the copy, exactly as seats 1 and 13
 * already do — so the witness IMPORT registers for an option type living in a non-generated
 * dependency namespace</b>. {@code NavigationHandler.findChoiceSuperOption} was the ONE structural
 * projection in the handler still minting an id-LESS, DETACHED {@code RTypeCall.deepCopy}; it now
 * routes through {@code projectedOptionAttribute} — the id-carry + workspace-attach +
 * phantom-parent + annotation-ref copy that {@code authorityChoiceOptionAttr} (seat 1) and
 * {@code idCarryingChoiceBridge} (seat 13) share (LAW 62: the pair stays implemented in exactly
 * one place).
 *
 * <p><b>The mechanism, measured from both halves.</b> Without the id,
 * {@code GeneratorModel.resolveTypeCall} can resolve the copy only BY NAME through
 * {@code resolveFromWorkspace}, which filters by {@code shouldGenerate} — so a vendored-CDM option
 * type inside a drr cell never resolves. {@code resolveJavaSimpleName} SURVIVES that (it has a
 * graceful rung: {@code rt == null || rt instanceof RMissingType} → return the rune name), so the
 * {@code <Asset>} witness TEXT still renders; {@code witnessSentinelTypeParam} and
 * {@code addWitnessTypeRef} do NOT — they hard-return on the same condition — so the first-claim
 * sentinel and the IMPORT are both silently lost while the rendered text stays right. That is the
 * LAW-69 two-halves disagreement the seat-1 javadoc predicted verbatim ("the E1 import-only
 * mismatch class").
 *
 * <p><b>The golden vs fork shape</b> — drr 7.x {@code GetBasketConstituents} (one hunk of six):
 * the fork renders {@code item.<Asset>map("getAsset", basketConstituent ->
 * basketConstituent.getAsset())} six times byte-identically to golden and keeps the six sibling
 * imports from the same package ({@code AssetIdTypeEnum}, {@code AssetIdentifier},
 * {@code Commodity}, {@code Instrument}, {@code Loan}, {@code Security}) — and drops
 * {@code import cdm.base.staticdata.asset.common.Asset;}. Every surviving sibling is a
 * second-or-later hop; {@code Asset} is the FIRST hop off the lambda item in all six, and it is
 * the only one reached through the choice supertype ({@code type BasketConstituent extends
 * Observable}, {@code choice Observable: Asset / Basket / Index}).
 *
 * <p><b>The probe verdict this law answers (LAW 75).</b> {@code PROBE29-F29w} at the PRODUCER
 * ({@code NavigationHandler:609 addWitnessTypeRef}), whole corpus, both routes:
 * <ul>
 *   <li>the carrier reads {@code feature=Asset typeParam=<Asset> collisionFqn=none attr=Asset
 *       added=(NONE) addedHas=false} on ALL 52 producer rows, where every positive control in the
 *       same cell reads the first-claim SENTINEL form
 *       {@code typeParam=<{cdm.base.staticdata.asset.common.Asset}> added=Asset addedHas=true};</li>
 *   <li>the gate-decline class corpus-wide is 25 {@code bare-PLAIN} rows in 5 files; the law's
 *       domain is the <b>24 {@code feature=Asset} rows in 4 band files</b> (drr
 *       7.0.0/7.1.0/7.2.0/7.3.0 {@code GetBasketConstituents}, 6 rows each) — <b>ZERO green</b>,
 *       route-IDENTICAL, and none of them in the 598-row OFF-only {@code F29w} delta. (The 25th
 *       row, drr 5.61.0 {@code EffectiveDateRule} with {@code attr=null} and an EMPTY type-param,
 *       is a DIFFERENT family and is excluded from the spec and from these fixtures.)</li>
 * </ul>
 * The census's own "first-claim sentinel + {@code suppressedCanonicals}" hypothesis was REFUTED
 * twice over: {@code F29u} → {@code F29i} is set-identical at {@code n=35},
 * {@code suppressedN=0}, {@code nulledInputs=(NONE)}. Nothing was filtered and nothing was
 * suppressed — the ref is NEVER PRODUCED.
 *
 * <p><b>LAW 74</b>: {@code GetBasketConstituents.java} uses the type {@code Asset} at six sites
 * while its import block carries only {@code AssetIdTypeEnum}, {@code AssetIdentifier} and
 * {@code FilterAssetIdentifier} — {@code cannot find symbol: class Asset}, ×4 drr 7.x cells. The
 * PRE javac probe must show those four and the POST must exit 0.
 *
 * <p><b>LAW 80 — what this law does NOT do.</b> It heals <b>ZERO whole files</b> (−4 diff lines =
 * one import hunk × 4 files) and the band stays 152. {@code GetBasketConstituents} remains
 * MULTI-FAMILY: the import is 1 of its 6 hunks (the others are the {@code java.util.Collections}
 * list-preserving form, the lambda-var naming, the F16 {@code MapperC.of} wrap, the hoist/CSE
 * class and the elseless-cascade ternary). This law's corpus receipt is <b>the import plus the
 * javac repair</b>, never a whole-file heal — which is why {@code corpus_c1} is an IMPORT-LINE
 * lock rather than a byte lock.
 *
 * <p><b>The risk the chain must measure.</b> The id makes
 * {@code attr.typeCall().referencedType()} resolvable where it was {@code Optional.empty()}, so
 * the change reaches EVERY consumer of the projected attribute — {@code metaNavResultType},
 * {@code resolveDeepMapMethod}/{@code resolveMapMethod} arity, {@code resolveLambdaVarName},
 * {@code attributeToDataType}'s chained-receiver walk — not only the two witness halves.
 * {@code findChoiceSuperOption} itself was NOT instrumented, so its firing count corpus-wide is
 * UNMEASURED (probe gap §8.1); only its observable FAILURE class is. A WHOLE-MATRIX D11
 * checkpoint is mandatory before the law is believed (the seat-28 law-6 lesson: a cell-scoped
 * control could not see a 10-file over-fire). {@code corpus_control3} scans cdm 6.20.6 for exactly
 * that reason: there the option types ARE generated, the by-name lookup already succeeded, and
 * the seat-1 SAME_DENOTATION argument says nothing may move.
 *
 * <p><b>Why the two witness halves were NOT patched instead.</b> Without the RType there is no
 * canonical name to build a {@code JavaClass} from, and patching them would leave the OTHER
 * consumers of the same projection blind. And the id is never carried WITHOUT the attach: an
 * id-bearing DETACHED copy makes {@code RTypeCall.referencedType()} THROW where the id-less copy
 * returned a harmless {@code Optional.empty()} (measured at seat 1 on cdm 6.20.6
 * {@code Qualify_Commodity_Option_Cash}). Since {@code RNode.workspace()} is
 * {@code protected final}, the workspace cannot be recovered from the {@link RModel} nodes at the
 * projection — it must be THREADED, which is why the eight compiler-reachable call sites convert.
 *
 * <p><b>RED at the pre-law head — MEASURED at the seat-29 chain's RED leg ({@code fa277393},
 * identical on BOTH routes)</b>: corpus_c1, corpus_control1. The drafted claim had a1/a2 RED
 * too; MEASURED they are GREEN
 * pre-law — the unit workspace's namespace-filtered GeneratorModel still RESOLVES the
 * dependency-namespace option (the corpus decline needs the vendored-model shouldGenerate split
 * at resolveFromWorkspace, which this harness's single linked workspace cannot reproduce
 * faithfully), so the fixture's nav never takes the id-less deepCopy path. a1/a2 therefore serve
 * as POST-LAW MECHANISM PINS (green in both states; the id-carry keeps them green), and the
 * failing-first witness is the CORPUS carrier corpus_c1 (the PROBE29-F29w 52-row evidence). The
 * mid-seat draft also listed corpus_control3 RED at the pre-law head — MEASURED GREEN in both
 * states at the chain: the cdm 6.20.6 cell's option sites never take the id-less deepCopy path
 * (their options resolve in-namespace), so the law's reach is drr-only at this corpus.
 * b1, b2, b3 and corpus_control2 are GREEN in BOTH states — b1 the authority-path twin (already
 * id-carried at seat 1), b2 the SAME-workspace control, b3 the harness's own positive control (a
 * REAL id-bearing attribute in the same dependency namespace whose import always registered — if
 * b3 were red the fixture would be proving nothing).
 *
 * <p><b>MEASURED MUTATIONS (LAW 82 — the seat-29 chain's suite-lane loop at {@code 687c8feb};
 * every set below is the RECORDED failing set from the f29-mut logs):</b>
 * <ul>
 *   <li><b>m-law6</b> the whole law reverted ({@code law6-apply29.py --revert}) → MEASURED
 *       corpus_c1, corpus_control1 (2F); a1/a2 unmoved (the post-law mechanism pins, per the
 *       paragraph above — the drafted {@code <ul>} entry wrongly re-listed them);
 *       b1/b2/b3/corpus_control3 unmoved as designed</li>
 *   <li><b>m-law6cs</b> (the {@code f29-mut-m-law6cs} log) the eight call sites reverted to the
 *       2-arg form while the three overloads
 *       STAY ({@code law6-apply29.py --mut-callsites}) → MEASURED corpus_c1, corpus_control1 (2F —
 *       the SAME set as m-law6, exactly the drafted claim). This isolates the THREADING from the
 *       projection: measured proof that the heal is the threaded workspace and not some other edit
 *       in the bundle.</li>
 * </ul>
 */
class ChoiceSuperOptionIdCarrySeatTest {

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

    /** Cell A = drr 7.0.0 — the carrier cell (7.1/7.2/7.3 carry the identical six rows). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = cdm 6.20.6 — the OVER-FIRE cell. Its choice option types are IN the generated set,
     * so the by-name lookup already succeeded and the linker id denotes the SAME declaration (the
     * seat-1 SAME_DENOTATION predicate): nothing may move there.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
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
    // The fixture — the CROSS-MODEL witness IS constructible in a unit fixture
    // =========================================================================

    /**
     * The dependency namespace — the corpus condition reproduced at unit grain: these types are
     * LOADED but NOT GENERATED (the emission filter accepts {@code census.seat29law6} only),
     * exactly as every drr cell navigates vendored cdm types. {@code ConstituentD extends
     * ObservableD} is {@code type BasketConstituent extends Observable} — a DATA type whose
     * options live on its CHOICE supertype, invisible to the data-supertype walk in
     * {@code HandlerHelper.findAttributeOnDataType} ({@code RDataType.superType()} is EMPTY for an
     * extends-choice relationship; the resolved choice is held by the separate
     * {@code choiceSuperType()} accessor) — which is precisely why
     * {@code findChoiceSuperOption} exists and why its projection is the only one that reaches
     * these options.
     *
     * <p>{@code PlainD} is b3: a REAL, id-bearing attribute in the SAME non-generated namespace,
     * whose import registers today. It is the harness's own positive control — it proves the
     * dependency filter is not simply suppressing every import from that namespace, so a1's
     * missing import is the projection's defect and not the fixture's.
     */
    private static final String MODEL_DEP = """
            namespace census.seat29law6.dep
            version "1.0.0"

            type AssetD:
                aid string (0..1)

            type BasketD:
                bid string (0..1)

            type IndexD:
                iid string (0..1)

            choice ObservableD:
                AssetD
                BasketD
                IndexD

            type ConstituentD extends ObservableD:
                weight number (0..1)

            type PlainD:
                pid string (0..1)

            type HolderD:
                cons ConstituentD (0..*)
                obs ObservableD (0..1)
                plain PlainD (0..1)
            """;

    /**
     * The generated namespace. a1 is the {@code GetBasketConstituents} shape verbatim — a BARE
     * navigate-by-type option name inside a conditional ladder whose implicit item is the data
     * type that extends the choice ({@code if Asset -> Instrument -> Security exists then …}),
     * which enters through {@code ReferenceHandler.synthesizeImplicitItemBareNav}'s
     * {@code itemNavChoiceSuperOption} rung (RH:5568). a2 is the EXPLICIT-root form, which enters
     * through {@code NavigationHandler.fallbackResolveFeature}'s rung 3 (NH:7365) — the two
     * distinct call sites the law converts, pinned apart.
     */
    private static final String MODEL_MAIN = """
            namespace census.seat29law6
            version "1.0.0"

            import census.seat29law6.dep.*

            func A1BareChoiceSuperOptionNav: <"a1 - THE GetBasketConstituents SHAPE: a BARE navigate-by-type option off an item whose type EXTENDS the choice (the RH bare-item re-root seat)">
                inputs:
                    h HolderD (1..1)
                output:
                    out string (0..*)
                add out:
                    h -> cons
                        extract
                            if AssetD exists
                            then AssetD -> aid
                            else if BasketD exists
                            then BasketD -> bid

            func A2ItemRootedChoiceSuperOptionNav: <"a2 - the EXPLICIT-root form: the fallbackResolveFeature rung-3 seat">
                inputs:
                    cs ConstituentD (0..*)
                output:
                    out string (0..*)
                add out:
                    cs extract item -> AssetD -> aid

            func B1DirectChoiceOptionNav: <"b1 - the AUTHORITY-path twin: the receiver IS the choice, so seat 1's projection already carries the id">
                inputs:
                    h HolderD (1..1)
                output:
                    out string (0..1)
                set out:
                    h -> obs -> AssetD -> aid

            func B3PlainAttributeNav: <"b3 - the HARNESS's positive control: a REAL id-bearing attribute in the SAME non-generated namespace; its import always registered">
                inputs:
                    h HolderD (1..1)
                output:
                    out string (0..1)
                set out:
                    h -> plain -> pid
            """;

    private static final String ASSET_IMPORT = "import census.seat29law6.dep.AssetD;";
    private static final String BASKET_IMPORT = "import census.seat29law6.dep.BasketD;";
    private static final String PLAIN_IMPORT = "import census.seat29law6.dep.PlainD;";
    private static final String ASSET_WITNESS = ".<AssetD>map(\"getAssetD\"";

    // =========================================================================
    // Part A — the carriers (RED before the flip)
    // =========================================================================

    /**
     * a1 — the POST-LAW MECHANISM PIN at the seat the corpus row actually enters through: a BARE
     * option name re-rooted onto the implicit item. MEASURED GREEN in BOTH states (the class
     * javadoc's adjudication: the unit workspace's namespace-filtered GeneratorModel still
     * resolves the option, so the fixture's nav never takes the id-less deepCopy path) — the
     * failing-first witness is the CORPUS carrier corpus_c1; this fixture pins the mechanism the
     * id-carry keeps green, it does not flip.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_bareChoiceSuperOptionNav_registersTheOptionImport() throws IOException {
        String out = filtered("A1BareChoiceSuperOptionNav.java");
        assertContains(out, ASSET_WITNESS);
        assertContains(out, ASSET_IMPORT);
        assertContains(out, BASKET_IMPORT);
    }

    /**
     * a2 — the same defect one call site over: an EXPLICIT {@code item ->} root makes the option
     * hop a genuine {@code RFeatureCall}, so it enters through {@code fallbackResolveFeature}'s
     * rung 3 instead. Pinning both apart is what proves the law had to convert every
     * compiler-reachable call site, not just the one the probe attributed.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_itemRootedChoiceSuperOptionNav_registersTheOptionImport() throws IOException {
        String out = filtered("A2ItemRootedChoiceSuperOptionNav.java");
        assertContains(out, ASSET_WITNESS);
        assertContains(out, ASSET_IMPORT);
    }

    // =========================================================================
    // Part B — the inert pins (green PRE and POST; they must not move)
    // =========================================================================

    /**
     * b1 — the AUTHORITY-path twin. When the receiver IS the choice, the C1 authority slot binds
     * the hop to an {@link com.regnosys.rosetta.ast.supporting.RChoiceOption} and seat 1's
     * {@code authorityChoiceOptionAttr} already projects it with the id+attach pair, so the import
     * registered before this law and must still. It is the in-fixture evidence that law 6 is the
     * LAST projection to be converted, not a new mechanism.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_directChoiceOptionNav_importUnchanged() throws IOException {
        String out = filtered("B1DirectChoiceOptionNav.java");
        assertContains(out, ASSET_IMPORT);
    }

    /**
     * b2 — the SAME-workspace control (no emission filter: the option's namespace IS generated).
     * The by-name lookup already succeeded there and the linker id denotes the SAME declaration —
     * the seat-1 SAME_DENOTATION predicate, and the cdm 6.20.6 situation
     * {@code corpus_control3} scans at corpus grain. The import registered before the seat and
     * still does.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_sameWorkspaceOptionImport_unchanged() throws IOException {
        String out = unfiltered("A1BareChoiceSuperOptionNav.java");
        assertContains(out, ASSET_IMPORT);
    }

    /**
     * b3 — the HARNESS's own positive control (prove the instrument can fail). A REAL, id-bearing
     * attribute in the SAME non-generated dependency namespace resolves through rung 2
     * ({@code findAttributeOnDataType}) and its import always registered. If this ever goes red
     * the fixture is proving nothing: a1's missing import would then be the dependency filter, not
     * the id-less projection.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_plainAttributeInTheSameDependencyNamespace_importUnchanged() throws IOException {
        String out = filtered("B3PlainAttributeNav.java");
        assertContains(out, PLAIN_IMPORT);
    }

    // =========================================================================
    // Part C — the corpus (LAW 80: an IMPORT-LINE lock, NOT byte-whole)
    // =========================================================================

    private static final String GBC =
            "drr/base/trade/basket/functions/GetBasketConstituents.java";
    private static final String CDM_ASSET_IMPORT =
            "import cdm.base.staticdata.asset.common.Asset;";
    private static final String CDM_ASSET_WITNESS = "<Asset>map(\"getAsset\"";

    /**
     * control0 — golden is the oracle (prove the instrument can fail). Golden
     * {@code GetBasketConstituents} carries BOTH the witness text and the import; the fork carries
     * only the text. Read from golden bytes, so a broken scan cannot pass this quietly.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesTheAssetImportAndTheWitness() throws IOException {
        String g = normalize(Files.readString(GOLDEN_A.resolve(GBC)));
        assertTrue(g.contains(CDM_ASSET_IMPORT),
                "golden must carry " + CDM_ASSET_IMPORT + " in " + GBC);
        assertTrue(g.contains(CDM_ASSET_WITNESS),
                "golden must carry the <Asset> witness text in " + GBC);
        assertTrue(g.contains("import cdm.base.staticdata.asset.common.AssetIdentifier;"),
                "golden's sibling second-hop imports from the same package must also be there"
                + " - they are what makes the missing FIRST-hop import a projection defect");
    }

    /**
     * c1 — the carrier's IMPORT-LINE lock. LAW 80: this file does NOT go byte-whole under law 6
     * (the import is 1 of its 6 hunks), so the lock is the one line the law owns — and the
     * corresponding {@code cannot find symbol: class Asset} javac error is the LAW-74 half of the
     * same receipt. drr 7.1.0/7.2.0/7.3.0 carry the identical row; the D11 ring is their gate.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_getBasketConstituentsRegistersTheAssetImport() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String gen = drrAOutput.get(GBC);
        assertNotNull(gen, "not generated: " + GBC);
        String code = normalize(gen);
        assertTrue(code.contains(CDM_ASSET_WITNESS),
                "the <Asset> witness text must still render in " + GBC);
        assertTrue(code.contains(CDM_ASSET_IMPORT),
                "the choice-super-option witness import must register in " + GBC + ":\n" + gen);
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0 (the carrier cell): per file the
     * (Asset import, {@code <Asset>map} witness, TOTAL import count) triple must equal golden's,
     * file for file over the UNION, beyond the NAMED residue. Only {@code GetBasketConstituents}
     * may move, and only by T1 +1 / T3 +1.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellOptionImportsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /** control2 — LAW 77 route parity for the carrier (a ReferenceHandler + NavigationHandler seat). */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(GBC), irOut.get(GBC), "route divergence: " + GBC);
    }

    /**
     * control3 — LAW 79 in the OVER-FIRE cell. cdm 6.20.6 declares the choice types the drr cells
     * navigate, and there they ARE generated: the by-name lookup already succeeded, so by the
     * seat-1 SAME_DENOTATION argument the id-carry is byte-neutral and the whole-cell triple must
     * be flat against golden beyond the named residue. This is the cell-scoped half of the
     * whole-matrix checkpoint the law's blast radius demands.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkCdm6WholeCellOptionImportsEqualGoldenFileByFile() throws IOException {
        assertNotNull(cdmBOutput, "cdm 6.20.6 generation did not run");
        assertEquals(List.of(), cdmBGenErrors,
                "cdm 6.20.6 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(cdmBOutput), scan(readGoldenTree(GOLDEN_B)), cdmBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM6, DOMAIN_CDM6);
    }

    /**
     * MEASURE at the law's head and pin the SET, not the count (LAW 73). T3 is the file's TOTAL
     * import count, so this list will name every band file whose import block differs from golden
     * for ANY reason — that breadth is the point (this law's whole blast-radius class is "imports
     * that did not resolve before"), and it is what makes an over-fire impossible to hide.
     *
     * <p><b>RE-MEASURED at the seat-30 chain head {@code e223ce19}</b> — 5 entries, transcribed
     * VERBATIM from that run's own failing print (LAW 81); the prior set was the seat-29 head's 7.
     * <b>RE-MEASURED at the seat-31 chain head {@code f2a4d5c0}</b> — 4 entries, the
     * UnderlierBasketIdentifier row having LEFT (noted inline), transcribed VERBATIM from that run's
     * own failing print.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // LAW 81 re-pin (v3.1 flip seat 33, law A.4): the GetBasketConstituents row (fork=[1, 0, 53] golden=[1, 0, 54]) LEFT this list -
            // facet lolDefaultBodyMulti moved this scan's tuple to golden's (the file stays BANDED on its A.3/A.5/A.1/A.2
            // residue; LolDefaultBodyMultiSeatTest pins that residue by name); transcribed from the control print (A4-trip1.log).
            // UnderlierBasketIdentifier.java (was fork=[0, 0, 12] golden=[0, 0, 15]) left this list at
            // seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so its import block (the three imports the deep option walk needs:
            // Observable, ReferenceWithMetaObservable, Product) now equals golden's 15. Golden's T3 = 15
            // keeps the file inside the union domain: DOMAIN_DRR7 UNMOVED at 4326.
            // GetUnderlierProductIdentifierLeg1.java (was fork=[1, 0, 38] golden=[1, 0, 39]) left
            // this list at seat 30: law 5 (thenArgDeclKindFromCompiled — the then-arg DECL reads the
            // compiled stamp instead of re-reading S) healed it WHOLE in all four drr 7.x cells, so
            // its import block now equals golden's.
            // ExecutionTimestampRule.java (was fork=[0, 0, 11] golden=[0, 0, 12]) left this list at
            // seat 30: law 3 (the disguised-nav REnumValueRef arm in chainProvesMulti, leaf rung)
            // healed it WHOLE in all four drr 7.x cells.
            // Both files keep a non-zero GOLDEN import count, so both stay inside the union domain
            // and DOMAIN_DRR7 is UNMOVED at 4326.
            // the Price row (fork=[1, 0, 50] golden=[1, 0, 52]) LEFT this list: law C.1 (iteChainNestedThenLadderAdmit + R2a/R2b/R4)
            // rendered the seven-rung ladder as statements and took this scan's token set to golden's in
            // all four drr 7.x cells - the file stays BANDED on C.2's default join; transcribed from this
            // control's own print (C1-trip1.log), a pure row removal (was == expected minus it).
            // the TotalNotionalQuantity row (fork=[0, 0, 24] golden=[0, 0, 25]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /** MEASURE at the law's head. Expected EMPTY — cdm 6.20.6 is the SAME_DENOTATION cell. */
    private static final List<String> KNOWN_RESIDUE_CDM6 = List.of();

    /**
     * MEASURED at the seat: fill from the first corpus run — the drr 7.0.0 union domain
     * (golden ∪ fork token-bearing files, intersected with what this harness emits).
     */
    private static final int DOMAIN_DRR7 = 4326;

    /** MEASURED at the seat: fill from the first corpus run — the cdm 6.20.6 union domain. */
    private static final int DOMAIN_CDM6 = 1280;

    /**
     * (T1, T2, T3) per file:
     * <ul>
     *   <li><b>T1</b> — the law's ADDED token at the measured carrier: the exact import line
     *       {@code import cdm.base.staticdata.asset.common.Asset;}.</li>
     *   <li><b>T2</b> — the witness TEXT {@code <Asset>map("getAsset"}, which renders in BOTH
     *       states ({@code resolveJavaSimpleName}'s graceful rung): a move here means the RENDER
     *       changed, not the import registration, and that would be an over-fire.</li>
     *   <li><b>T3</b> — the OVER-FIRE NET: the file's TOTAL import-line count. Deliberately global
     *       and not Asset-scoped — the id-carry's entire blast-radius class is "an import that did
     *       not resolve before", so a spurious import ANYWHERE in the cell fails the control.</li>
     * </ul>
     * The carrier moves T1 +1, T2 unchanged, T3 +1; every other file must match golden exactly.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            for (String line : code.split("\n")) {
                String s = line.trim();
                if (s.startsWith("import ") && s.endsWith(";")) {
                    t3++;
                    if (s.equals("import cdm.base.staticdata.asset.common.Asset;")) {
                        t1++;
                    }
                }
                int from = 0;
                while ((from = s.indexOf("<Asset>map(\"getAsset\"", from)) >= 0) {
                    t2++;
                    from += "<Asset>map(\"getAsset\"".length();
                }
            }
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * The scan universe is the token-bearing union INTERSECTED with the files this harness emits
     * (rule/report/function kinds): golden's POJO and metafield kinds carry imports these
     * generators never produce, and whether the fork emits every expected FILE is the D11 ring's
     * question (missingOutput), not this suite's.
     */
    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means an"
                + " unpinned call site");
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
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-26/27/28 suite shape; the two-model link is seat 13's)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> cdmBOutput;
    private static List<String> cdmBGenErrors;

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
            cdmBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_B_ROOT), errs);
            cdmBGenErrors = errs;
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    /** Strip line and block comments plus string literals so a javadoc or label never counts as code. */
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

    // -------------------------------------------------------------------------
    // The unit fixture: TWO models (a dependency namespace + the generated
    // namespace), rendered twice - FILTERED (the dependency namespace loaded but
    // NOT generated, i.e. the corpus condition) and UNFILTERED (b2).
    // -------------------------------------------------------------------------

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> filteredOut;
    private static Map<String, String> unfilteredOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel dep = AstBuilder.buildFromString(MODEL_DEP, "seat29law6-dep.rosetta");
            RModel main = AstBuilder.buildFromString(MODEL_MAIN, "seat29law6.rosetta");
            dep.setVersion("0.0.0.test");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(dep);
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
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static String filtered(String fileName) throws IOException {
        if (filteredOut == null) {
            filteredOut = render(m -> "census.seat29law6".equals(m.namespace()));
        }
        return lookup(filteredOut, "functions/" + fileName);
    }

    private static String unfiltered(String fileName) throws IOException {
        if (unfilteredOut == null) {
            unfilteredOut = render(m -> true);
        }
        return lookup(unfilteredOut, "functions/" + fileName);
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[ChoiceSuperOptionIdCarrySeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
