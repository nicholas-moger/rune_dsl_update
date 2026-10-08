// SEAT 32 LAW E.1 — facet depFieldTypeCollisionSeed (apply script: drafts32/E1/src-pairs.py).
// DRAFTED, not yet measured. Every pin marked  ///PIN:  states whether it is MEASURED (from a
// read-only corpus walk named in the javadoc), DERIVED (from another suite's measured pin plus a
// stated identity), or SENTINEL (-1: the union helper's first assert PRINTS the value — transcribe
// it into the pin in the SAME commit, the print-first domain-pin discipline).
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
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * SEAT 32, law E.1 — facet {@code depFieldTypeCollisionSeed} (census family F17): <b>the
 * {@code @Inject} DEPENDENCY FIELD TYPES seed the import-collision resolver's first-claim map,
 * so a later body sentinel carrying a DIFFERENT canonical of the same simple name renders
 * FQN-inline and its import is suppressed.</b>
 *
 * <p><b>GOLDEN &larr; FORK.</b> Carrier
 * {@code drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying.java}
 * &times; drr 7.0.0 / 7.1.0 / 7.2.0 / 7.3.0 FUNCTION (census class C008; 4 files, 10 signatures,
 * 39 lines / 12 hunks). Golden line 193 is
 * {@code @Inject protected FloatingRateIndex floatingRateIndex;} — the SAME-PACKAGE generated
 * function class {@code drr.enrichment.upi.functions.FloatingRateIndex} (the source file
 * {@code enrichment-upi-func.rosetta} declares {@code func FloatingRateIndex} at line 1306 in the
 * carrier's own {@code namespace drr.enrichment.upi}, and the class exists in the golden tree).
 * That field claims the simple name FIRST, so golden's NINETEEN nav witnesses read
 * {@code .<cdm.observable.asset.FloatingRateIndex>map("getFloatingRateIndex", interestRateIndex
 * -> interestRateIndex.getFloatingRateIndex())} and golden emits NO
 * {@code import cdm.observable.asset.FloatingRateIndex;}. The fork, whose seed list never
 * contained the dependency types, let the first WITNESS claim the name and imported the cdm type.
 * <b>MEASURED: the whole 4-cell divergence is exactly that one import line plus the string
 * {@code cdm.observable.asset.} at 19 sites</b> — dropping the import and prefixing the 19 type
 * params makes the fork file BYTE-IDENTICAL to golden in all four cells (verified against the
 * seat-31 final dumps; no other token moves).
 *
 * <p><b>THE PRODUCER.</b> {@code FunctionGenerator.buildStandardModel} builds
 * {@code seedCanonicals} (class self #366, output, inputs, superclass, alias-return elements
 * #245) and never seeds {@code dependencies} — a live local of the same method, declared at
 * {@code :912} and consumed at {@code :1291}. {@code ImportCollisionResolver} already implements
 * first-claim-wins and the import suppression is already wired
 * ({@code FunctionGenerator:1301-1305}); the machinery was simply UNDER-SEEDED. The law adds the
 * dependency-type seeds LAST ({@code seedFromCanonicals} is {@code putIfAbsent}, so no existing
 * winner can move).
 *
 * <p><b>LAW 69 — three siblings already walk this same list, and one of them already seeds.</b>
 * (i) {@code FunctionGenerator:1697-1707}, facet {@code depFieldNamingSeeds} (PR #334), seeds the
 * dependency field NAMES into the method naming scope — which is exactly why the carrier's
 * {@code _floatingRateIndex} lambda name is ALREADY byte-correct in the fork; the NAME half
 * shipped, the TYPE half did not. (ii) {@code FunctionGenerator.collectStandardImports:2391-2395}
 * walks {@code dependencies} with the SAME {@code dep.getTypeFqn() != null} test to decide which
 * dependency canonicals get IMPORTED — and the canonicals that are imported are precisely the
 * ones that claim a simple name, so the claim loop and the import loop must read the same field
 * with the same test. (iii) <b>the DATARULE kind already does this</b>:
 * {@code DataRuleGenerator:267-271} seeds its dependency types into its own
 * {@code seedCanonicals} using the contrapositive test
 * ({@code !dep.getTypeName().contains(".")}). This law is the FUNCTION kind catching up with its
 * own rule sibling; {@code DataRuleGenerator} is deliberately NOT touched.
 *
 * <p><b>THE DISCRIMINATOR / GREEN BLAST RADIUS.</b> Every function with an import passes this
 * seat, so the discriminator is the resolver's own {@code prior.equals(canonical)} test: a
 * dependency seed only bites when a body sentinel carries a DIFFERENT canonical under the same
 * simple name. An FQN-inline dependency ({@code typeFqn == null} — the #330/#364 convention at
 * {@code FunctionDependencyCollector.addFunctionDependency:828-833} and
 * {@code injectRuleDependency:743-747}: a host-class collision or a #364 group loser renders its
 * field type fully qualified and emits no import) claims NOTHING and is skipped. Green-safety is
 * by CONSTRUCTION: a file that declares a bare {@code @Inject protected X x;} and also imports a
 * different-canonical {@code X} has its dependency class shadowed by that single-type import for
 * the whole compilation unit (JLS 6.4.1), so {@code x.evaluate(...)} cannot resolve — it never
 * compiled, and so was never green.
 *
 * <p><b>THE MEASURED FLIP POPULATION (read-only walks over the goldens; scripts
 * {@code e1census.py} / {@code e1census2.py} / {@code e1trip.py}, and the scout's
 * {@code f17census.py} JSONs).</b> Over the 15 censused cells (cdm 5.38.0, cdm 6.20.6, 6.21.0,
 * 6.22.0, 6.23.0; drr 5.61.0, 6.34.1, 6.37.0, 6.38.0, 7.0.0, 7.1.0, 7.2.0, 7.3.0; iso20022
 * 1.38.0 and rune-fpml 2.0.0, which emit ZERO {@code @Inject} fields at all), the number of
 * golden files whose BARE dependency simple name is also a different-canonical body type param is
 * <b>ONE per drr 7.x cell — the carrier — and ZERO everywhere else</b> (drr 7.0.0: 4,709 files
 * with {@code @Inject protected}, flip population 1; drr 5.61.0: 2,762 files, flip population 0).
 * The same walks put the FQN-inline-dependency-collides-with-a-type-param population at <b>0</b>
 * in every cell — see the {@code m-e1-fqninline} lane below, which is EMPTY BY CONSTRUCTION and
 * not merely by corpus.
 *
 * <p><b>ALSO MEASURED — the same carrier is GREEN in drr 5.61.0 / 6.34.1 / 6.37.0 / 6.38.0.</b>
 * All four carry the identical {@code @Inject protected FloatingRateIndex floatingRateIndex;}
 * field, but their 6.x/5.x sources never navigate to a cdm {@code FloatingRateIndex}, so those
 * goldens hold ZERO {@code cdm.observable.asset.FloatingRateIndex} occurrences and no
 * {@code FloatingRateIndex} import: the seed claims a name no sentinel contests and the files do
 * not move. {@code FnAnnaDsbTogetherRestructureComposeTest} byte-locks the drr 6.34.1 copy of
 * this very file and must stay green — it is the cross-cell tripwire this law is most exposed to.
 *
 * <p><b>LAW 77 — INHERITS.</b> The seed lives in {@code FunctionGenerator.buildStandardModel},
 * which {@code IRFunctionGenerator extends FunctionGenerator} does NOT override; the IR emitter
 * emits the SAME {@code ImportCollisionResolver.typeRef} sentinel and says so
 * ({@code IRJavaLeafEmitter:1034-1036} — "resolved at file assembly"). No IR twin is needed;
 * {@code corpus_control2} measures the carrier on the IR route to prove it.
 *
 * <p><b>LAW 74 — the fork's CURRENT text does NOT compile (MEASURED: {@code javac32-report.md}
 * section 7.3 row C12, PRE 2 errors -> POST 0).</b> Fork line 31
 * {@code import cdm.observable.asset.FloatingRateIndex;} shadows the same-package function class
 * for the whole compilation unit, so fork line 194's field is typed as the cdm data-type INTERFACE
 * and fork lines 460, 468 and 523 ({@code floatingRateIndex.evaluate(...)}) have no such method:
 * 3 <b>call sites</b> &times; 4 cells. The harness hosts ONE of the three sites and javac reports 2
 * errors there; the shadowing STEP itself is not measurable in one compilation unit -- stated, not
 * measured ({@code javac32-report.md} section 6).
 *
 * <p><b>CLAIMED RED (at the pre-law base head, BOTH routes)</b> — {@code a1},
 * {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (residue 4 rows instead of 3),
 * and {@code corpus_control2} on {@code -Pir-on}. <b>NOT in the RED set:</b> {@code a2}
 * (a fixture-fidelity pin on the already-shipped #334 sibling), {@code e1}, {@code corpus_e2}
 * (decline locks — green at RED and at GREEN) and {@code corpus_control3} (drr 5.61.0, a pure
 * decline cell whose residue the drafter assumed empty; MEASURED as three band rows at the law
 * head and two after law F.1 -- it shipped on the -1 domain SENTINEL, transcribed at the landing,
 * and is RED at the seat's base for its post-F.1 pin, not for anything this law does).
 * <b>CLAIMED GREEN at the law's head</b> — 9 tests, 0F on the default profile and 0F under
 * {@code -Pir-on}, once the {@code DOMAIN_DRR561} sentinel is transcribed.
 *
 * <p><b>MUTATION LANES (LAW 66/76) — MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-e1*.log}):</b>
 * <ul>
 *   <li><b>m-e1</b> — the whole seed loop deleted (the law reverted): MEASURED <b>9/4F/1S</b> =
 *       {@code a1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} — the claim
 *       exactly on the default route ({@code corpus_control2} is the {@code -Pir-on} member); the
 *       whole-cell control MOVES under this lane (LAW 76); {@code a2}, {@code e1},
 *       {@code corpus_e2}, {@code corpus_control3} GREEN.</li>
 *   <li><b>m-e1-fqninline</b> — the FQN-inline skip severed as the SUBSTITUTION
 *       {@code seedCanonicals.add(dep.getTypeFqn() != null ? dep.getTypeFqn() : dep.getTypeName())}
 *       (the literal {@code != null} deletion being a provable no-op, since
 *       {@code seedFromCanonicals} skips null and blank canonicals): MEASURED <b>9/0F/1S —
 *       EMPTY</b>, as declared and for the declared reason: an FQN-inline dependency exists only
 *       in the {@code collides} shape (its simple name IS the host class name, seeded by the #366
 *       class-self seed) and the #364 {@code groupLoser} shape (seeded by the group HEAD this law
 *       itself seeds), and {@code seedFromCanonicals} is {@code putIfAbsent}, so the extra seed is
 *       never reachable; the golden walk finds ZERO files in any cell where an FQN-inline
 *       dependency's simple name also appears as a type param. {@code corpus_e2} is the byte lock
 *       that would catch it anyway. A measured zero against a declared zero: an adjudication.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b} ({@code f32-red-default.log}): {@code a1},
 * {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} AND {@code corpus_control3} —
 * the fifth is not a law-E.1 member: {@code KNOWN_RESIDUE_DRR561} was re-pinned at checkpoint 1
 * to the post-F.1 cell (the asic {@code EffectiveDateRule} row LEFT when law F.1 healed it), so
 * at a base where F.1 is absent the cell still carries that row and the control fires. (+
 * {@code corpus_control2} on {@code -Pir-on}.) GREEN at the head 9/0F/1skip default, 9/0F/0skip
 * {@code -Pir-on}.
 *
 * <p><b>LAW-81 TRIPWIRES this law WILL fire in OTHER suites (do not edit them here).</b> Both
 * pin the carrier's IMPORT COUNT as the third component of a whole-cell triple, and the law takes
 * that count 169 &rarr; 168 == golden, so BOTH rows LEAVE their residue lists (neither file leaves
 * its DOMAIN — golden's third component stays non-zero):
 * <ul>
 *   <li>{@code ChoiceSuperOptionIdCarrySeatTest.KNOWN_RESIDUE_DRR7} — the row
 *       {@code "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying.java
 *       fork=[1, 0, 169] golden=[1, 0, 168]"} (4 rows &rarr; 3; {@code DOMAIN_DRR7} 4326
 *       UNMOVED).</li>
 *   <li>{@code ImplicitInputDisguiseSeatTest.KNOWN_RESIDUE_DRR7} — the row
 *       {@code "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying.java
 *       fork=[0, 8, 169] golden=[0, 8, 168]"} (3 rows &rarr; 2; {@code DOMAIN_DRR7} 3278 UNMOVED —
 *       its inclusion is {@code t1 + t2 > 0} and the carrier's {@code t2} stays 8).</li>
 * </ul>
 * Checked and NOT fired: {@code AliasSignatureAuthoritySeatTest} and
 * {@code BareOptionChainHeadAuthoritySeatTest} assert alias signatures / a
 * {@code TradeForEvent} import and dependency on the same carrier — none of those tokens moves;
 * {@code FnAnnaDsbTogetherRestructureComposeTest} byte-locks the drr <b>6.34.1</b> copy, which
 * this law does not touch (see above).
 */
class DepFieldTypeCollisionSeedSeatTest {

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

    /** The carrier. */
    private static final String ANNA =
            "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying.java";

    /**
     * corpus_e2's decline lock: a GREEN drr 7.x function carrying TWO #364 FQN-inline dependencies
     * ({@code @Inject protected drr.regulation.csa.rewrite.trade.functions.CommodityLeg1
     * commodityLeg11;} and the {@code InterestRateLeg1} twin) whose simple names ARE imported from
     * a different package ({@code drr.regulation.common.functions.*}), alongside EIGHT bare
     * dependencies this law now seeds. Exactly the shape {@code m-e1-fqninline} attacks.
     */
    private static final String IS_CSA_LEG1 =
            "drr/regulation/csa/rewrite/trade/functions/IsCSALeg1Aligned.java";

    /** Cell A = drr 7.0.0 — the carrier, the decline lock and the whole-cell control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    /** Cell B = drr 7.3.0 — the second carrier cell (the law heals all four identically). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    /** Cell C = drr 5.61.0 — the DECLINE cell: 2,762 {@code @Inject} files, flip population 0. */
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_C = CELL_C_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellCAvailable() {
        return Files.isDirectory(GOLDEN_C);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixtures — reduced from test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
    // enrichment-upi-func.rosetta: `func Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying`
    // (line 209) and the SAME-NAMESPACE `func FloatingRateIndex` (line 1306), whose generated
    // class is the dependency that claims the simple name. The witness is the carrier's
    // `... -> FloatingRateIndex -> indexTenor -> periodMultiplier` hop, which golden renders as
    // `.<cdm.observable.asset.FloatingRateIndex>map("getFloatingRateIndex", interestRateIndex ->
    // interestRateIndex.getFloatingRateIndex()).<Period>map("getIndexTenor", _floatingRateIndex ->
    // _floatingRateIndex.getIndexTenor()).<Integer>map("getPeriodMultiplier", period ->
    // period.getPeriodMultiplier())` — reduced here to a plain single-hop attribute nav of the
    // same cardinalities.
    // =========================================================================

    private static final String MODEL_DEP = """
            namespace census.seat32e1.dep
            version "1.0.0"

            type Period:
                periodMultiplier int (0..1)

            type FloatingRateIndex:
                indexTenor Period (0..1)

            type InterestRateIndex:
                floatingRateIndex FloatingRateIndex (0..1)

            func Helper: <"e1 - a CROSS-package dependency whose simple name collides with nothing">
                inputs:
                    idx InterestRateIndex (1..1)
                output:
                    r boolean (1..1)
                set r:
                    idx exists
            """;

    private static final String MODEL_MAIN = """
            namespace census.seat32e1
            version "1.0.0"

            import census.seat32e1.dep.*

            func FloatingRateIndex: <"the SAME-PACKAGE dependency whose class simple name IS the collision">
                inputs:
                    idx InterestRateIndex (1..1)
                output:
                    r boolean (1..1)
                set r:
                    idx exists

            func UseIt: <"a1 - the dependency claims FloatingRateIndex, so the witness must FQN-inline">
                inputs:
                    idx InterestRateIndex (1..1)
                output:
                    tenor int (0..1)
                set tenor:
                    if FloatingRateIndex(idx)
                    then idx -> floatingRateIndex -> indexTenor -> periodMultiplier

            func UseHelper: <"e1 - the SAME witness with NO colliding dependency keeps its bare name + import">
                inputs:
                    idx InterestRateIndex (1..1)
                output:
                    tenor int (0..1)
                set tenor:
                    if Helper(idx)
                    then idx -> floatingRateIndex -> indexTenor -> periodMultiplier
            """;

    // =========================================================================
    // a1 / a2 — the fixture
    // =========================================================================

    /**
     * a1 — the heal: the {@code @Inject} dependency field claims {@code FloatingRateIndex}, so the
     * different-canonical body witness renders FQN-inline and its import is suppressed. The three
     * four assertions are exactly the carrier's golden facts, reduced.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_depFieldTypeClaimsTheSimpleNameSoTheWitnessFqnInlines() throws IOException {
        String out = fixture("UseIt.java");
        assertContains(out, "@Inject protected FloatingRateIndex floatingRateIndex;");
        assertContains(out,
                ".<census.seat32e1.dep.FloatingRateIndex>map(\"getFloatingRateIndex\"");
        assertTrue(!out.contains("import census.seat32e1.dep.FloatingRateIndex;"),
                "the witness import must be SUPPRESSED - it would shadow the same-package"
                        + " dependency class for the whole compilation unit:\n" + out);
        assertTrue(!out.contains(".<FloatingRateIndex>map(\"getFloatingRateIndex\""),
                "no witness may keep the bare simple name once the dependency claims it:\n" + out);
    }

    /**
     * a2 — FIXTURE FIDELITY, not law evidence (green at RED and at GREEN): the reduction really
     * does reproduce the carrier's #334 interaction — the dependency FIELD NAME
     * {@code floatingRateIndex} is registered on the method naming scope, so the second hop's
     * lambda parameter escapes to {@code _floatingRateIndex} exactly as golden line 653 does. If
     * this fails, the fixture is NOT a faithful reduction: RESHAPE the fixture, do not weaken the
     * assert.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_fixtureReproducesTheCarrierDepNameEscape() throws IOException {
        String out = fixture("UseIt.java");
        assertContains(out, "@Inject protected FloatingRateIndex floatingRateIndex;");
        assertContains(out, "map(\"getIndexTenor\", _floatingRateIndex ->");
    }

    /**
     * e1 — the DECLINE LOCK: the same witness, the same nav, but the colliding dependency
     * replaced by a cross-package {@code Helper} whose simple name collides with nothing. The new
     * seed still runs (it claims {@code Helper}), and NOTHING moves: the witness keeps its bare
     * simple name and its import, and {@code Helper} keeps its own import. This is the byte-neutral
     * direction of the identical shape.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_nonCollidingDependencySeedIsAByteNeutralNoOp() throws IOException {
        String out = fixture("UseHelper.java");
        assertContains(out, "@Inject protected Helper helper;");
        assertContains(out, "import census.seat32e1.dep.functions.Helper;");
        assertContains(out, "import census.seat32e1.dep.FloatingRateIndex;");
        assertContains(out, ".<FloatingRateIndex>map(\"getFloatingRateIndex\"");
        assertTrue(!out.contains(".<census.seat32e1.dep.FloatingRateIndex>"),
                "an uncontested witness must NOT be FQN-inlined - the seed only bites on a"
                        + " different-canonical same-simple-name claim:\n" + out);
    }

    // =========================================================================
    // corpus — the whole-file byte locks
    // =========================================================================

    /** corpus_c1 — the drr 7.0.0 carrier goes WHOLE. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700AnnaDsbUpiMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(ANNA)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + ANNA + ": " + own);
        String gen = drrAOutput.get(ANNA);
        assertNotNull(gen, "not generated: " + ANNA);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(ANNA))), normalize(gen),
                "Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying must byte-match golden"
                        + " - seat 32 law E.1: the @Inject dependency field type claims"
                        + " FloatingRateIndex, so all 19 witnesses FQN-inline and the cdm import"
                        + " is suppressed");
    }

    /** corpus_c2 — the drr 7.3.0 carrier goes WHOLE (the law heals all four 7.x cells alike). */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730AnnaDsbUpiMatchesGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        List<String> own = drrBGenErrors.stream().filter(e -> e.contains(ANNA)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + ANNA + ": " + own);
        String gen = drrBOutput.get(ANNA);
        assertNotNull(gen, "not generated: " + ANNA);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(ANNA))), normalize(gen),
                "the drr 7.3.0 carrier must byte-match golden - seat 32 law E.1");
    }

    /**
     * corpus_e2 — the FQN-INLINE DECLINE LOCK on a GREEN file: {@code IsCSALeg1Aligned} carries
     * two #364 group-loser dependencies rendered fully qualified with their imports suppressed,
     * their two group HEADS imported bare under the same simple names, and eight further bare
     * dependencies this law now seeds. A whole-file byte compare is the strongest available lock
     * on the {@code typeFqn == null} skip: it fails the moment the law seeds an FQN-inline
     * dependency or disturbs a group head's import.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_e2_drr700IsCsaLeg1AlignedStaysByteIdentical() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String gen = drrAOutput.get(IS_CSA_LEG1);
        assertNotNull(gen, "not generated: " + IS_CSA_LEG1);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(IS_CSA_LEG1))), normalize(gen),
                "a file with #364 FQN-inline dependencies must be byte-UNCHANGED by the seed");
    }

    // =========================================================================
    // corpus controls — LAW 79 UNION whole-cell + LAW 77 route
    // =========================================================================

    /**
     * corpus_control1 — LAW 79, the UNION whole-cell control over drr 7.0.0. The per-file triple
     * is (T1) the number of {@code @Inject protected} dependency fields declared with a BARE
     * simple name (the set this law seeds), (T2) the number of inline-FQN type params
     * {@code .<a.b.C>} (the form the law ADDS), and (T3) the file's TOTAL import count (the
     * over-suppression net — a single import wrongly dropped anywhere in the cell shows up here).
     * A file enters the domain when any component is non-zero, i.e. essentially every emitted
     * file. The control FAILS when the law over-fires (a green file gains an inline FQN or loses
     * an import) AND when it under-fires (the carrier's row stays in the residue).
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_drr700WholeCellClaimTripleEqualsGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, DOMAIN_DRR700);
    }

    /**
     * corpus_control2 — LAW 77, BOTH-ROUTES-vs-GOLDEN. The seed lives in
     * {@code FunctionGenerator.buildStandardModel}, which {@code IRFunctionGenerator} inherits
     * without override, and the IR leaf emitter emits the same collision sentinel
     * ({@code IRJavaLeafEmitter:1034-1036}); this proves the inheritance rather than assuming it,
     * on the carrier AND on the FQN-inline decline lock.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheCarrierAndTheDeclineLock() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(ANNA))),
                normalize(irOut.get(ANNA)), "IR route vs GOLDEN: " + ANNA);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(IS_CSA_LEG1))),
                normalize(irOut.get(IS_CSA_LEG1)), "IR route vs GOLDEN: " + IS_CSA_LEG1);
    }

    /**
     * corpus_control3 — LAW 79 on the DECLINE cell. drr 5.61.0 has 2,762 files with
     * {@code @Inject protected} dependencies and a MEASURED flip population of ZERO, so the whole
     * cell must stay exactly where it is. The LAW's flip population in this cell is zero (the
     * {@code m-e1} lane measures this control GREEN with the law reverted), while the cell's
     * pinned residue is TWO standing B001 import-signature band rows outside this law's reach
     * (three at the law head; the asic EffectiveDateRule row LEFT when law F.1 healed it -- the
     * checkpoint-1 re-pin). It is therefore RED at the seat's base (the pre-F.1 cell) and at the
     * law's own head (the {@code -1} sentinel), for reasons that are not this law's -- the
     * seat-32 review's L3-05 correction of the earlier "EMPTY on both sides".
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_control3_drr5610WholeCellClaimTripleUnmoved() throws IOException {
        assertNotNull(drrCOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrCGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrCOutput), scan(readGoldenTree(GOLDEN_C)), drrCOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    // =========================================================================
    // Pins
    // =========================================================================

    ///PIN: MEASURED at the seat-31 final OFF dump (script e1trip.py, run over
    ///PIN: d31final-off/drr_7.0.0_FUNCTION: every non-band file in the cell is byte-identical to
    ///PIN: golden, so the residue can only come from the 13 band files, and exactly four of those
    ///PIN: carry a triple delta). At the PRE-law head the list has FOUR rows - this one plus
    ///PIN:   "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying.java fork=[83, 0, 169] golden=[83, 19, 168]"
    ///PIN: which is what makes this control part of the failing-first RED set and what m-e1 puts
    ///PIN: back. The three rows below are OTHER-family import residue (S32's own backlog:
    ///PIN: GetBasketConstituents, Price, TotalNotionalQuantity) and are expected to survive.
    ///PIN: Transcribe from the control's own failing print if it disagrees (LAW 81).
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of(
            // LAW 81 re-pin (v3.1 flip seat 33, law A.4): the GetBasketConstituents row (fork=[11, 0, 53] golden=[11, 0, 54]) LEFT this list -
            // facet lolDefaultBodyMulti moved this scan's tuple to golden's (the file stays BANDED on its A.3/A.5/A.1/A.2
            // residue; LolDefaultBodyMultiSeatTest pins that residue by name); transcribed from the control print (A4-trip1.log).
            // the Price row (fork=[16, 0, 50] golden=[16, 0, 52]) LEFT this list: law C.1 (iteChainNestedThenLadderAdmit + R2a/R2b/R4)
            // rendered the seven-rung ladder as statements and took this scan's token set to golden's in
            // all four drr 7.x cells - the file stays BANDED on C.2's default join; transcribed from this
            // control's own print (C1-trip1.log), a pure row removal (was == expected minus it).
            // the TotalNotionalQuantity row (fork=[1, 0, 24] golden=[1, 0, 25]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    ///PIN: MEASURED EMPTY at the seat-31 final OFF dump: drr 5.61.0's FUNCTION band is one file
    ///PIN: (Create_CancelledWorkflowStepFromInstruction) and it carries NO triple delta; its POJO
    ///PIN: band files are not emitted by this harness. A NON-EMPTY result is the ENTERING signal.
    ///PIN: MEASURED at E1-red1.log / E1-green1.log / E1-on1.log - IDENTICAL at RED, at GREEN and on the
    ///PIN: IR route (the law leaves drr 5.61.0 untouched): the cell's OWN three band files, whose
    ///PIN: import count (T3) is one short of golden by the standing B001 import sig. Transcribed
    ///PIN: VERBATIM from the control's own print; the drafter's List.of() assumed a pure-decline cell
    ///PIN: had no band residue - it has three, all outside this law's reach.
    // the asic EffectiveDateRule row (fork=[5, 0, 11] golden=[5, 0, 12]) LEFT this list at seat 32:
    // law F.1 (ruleArmBareOutputEvidence) healed that file WHOLE after this suite landed - a cross-law
    // tripwire caught by the checkpoint-1 full gensuite (ckpt1-gensuite.log), transcribed from its print.
    // LAW 81 re-pin (v3.1 flip seat 33, law F.A): the cftc NotionalCurrencyLeg1Rule + jfsa
    // NotionalCurrencyOfLeg1Rule rows LEFT this list - both files healed WHOLE by facet
    // blockArmWrapperHopDeref (byte-identical to golden, locked by BlockArmWrapperHopDerefSeatTest
    // corpus_c1/c2); the list is EMPTY, transcribed from this control's own print (FA-trip1.log).
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    ///PIN: DERIVED, not printed: ChoiceSuperOptionIdCarrySeatTest pins the drr 7.0.0 union domain
    ///PIN: at 4326 using the IDENTICAL harness (loadCellCorpusCached + emissionFilter, then
    ///PIN: ruleGen + reportGen + funcGen) and the IDENTICAL inclusion rule (t1+t2+t3 > 0 with T3 =
    ///PIN: the file's import count), so the two domains are the same set: every emitted file with
    ///PIN: at least one import. Falsifier: this suite's own domain assert, whose message prints
    ///PIN: the measured value - transcribe it if it disagrees.
    private static final int DOMAIN_DRR700 = 4326;

    ///PIN: shipped as a -1 print-first SENTINEL (no existing suite pins a drr 5.61.0 union domain
    ///PIN: under an any-import inclusion, and the domain is intersected with what this harness
    ///PIN: EMITS, so it cannot be derived from the goldens alone); assertUnionEqual printed
    ///PIN: "MEASURED domain=2506" at E1-red1.log and the value was transcribed in the same commit.
    private static final int DOMAIN_DRR561 = 2506;   // MEASURED at E1-red1.log (the print-first sentinel, transcribed)

    // =========================================================================
    // The scan + the union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /** An inline-FQN type param: {@code .<a.b.C>} — the form this law ADDS. */
    private static final Pattern FQN_TYPE_PARAM =
            Pattern.compile("\\.<[a-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+>");

    private static final String INJECT = "@Inject protected ";

    /**
     * (T1, T2, T3) per file, over COMMENT- and STRING-stripped source: bare-typed
     * {@code @Inject protected} dependency fields (the seeded set), inline-FQN type params (the
     * form the law adds), and the file's TOTAL import count (the over-suppression net).
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(normalize(e.getValue()));
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            Matcher m = FQN_TYPE_PARAM.matcher(code);
            while (m.find()) {
                t2++;
            }
            for (String line : code.split("\n")) {
                String s = line.trim();
                if (s.startsWith("import ") && s.endsWith(";")) {
                    t3++;
                } else if (s.startsWith(INJECT)) {
                    String rest = s.substring(INJECT.length()).trim();
                    int sp = rest.indexOf(' ');
                    String type = sp < 0 ? rest : rest.substring(0, sp);
                    if (type.indexOf('.') < 0) {
                        t1++;
                    }
                }
            }
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        Set<String> universe = new TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[3];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + Arrays.toString(ac)
                        + " " + bName + "=" + Arrays.toString(bc));
            }
        }
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "the triple differs beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
    }

    /**
     * Code outside comments and string/char literals — the same walk
     * {@code ChoiceSuperOptionIdCarrySeatTest} uses, so a generated-Java fragment inside a string
     * literal or a javadoc citation cannot be counted as a real type param or import.
     */
    private static String codeOnly(String java) {
        StringBuilder sb = new StringBuilder(java.length());
        int i = 0;
        int n = java.length();
        while (i < n) {
            char ch = java.charAt(i);
            if (ch == '"' || ch == '\'') {
                char quote = ch;
                int j = i + 1;
                while (j < n && java.charAt(j) != quote) {
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

    // =========================================================================
    // Fixture harness — one linked workspace (the dependency namespace loaded but NOT
    // generated, mirroring the corpus condition), the AliasSigElementMetaSeatTest shape.
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel dep = AstBuilder.buildFromString(MODEL_DEP, "seat32e1-dep.rosetta");
            RModel main = AstBuilder.buildFromString(MODEL_MAIN, "seat32e1.rosetta");
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

    private static String fixture(String fileName) throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat32e1".equals(m.namespace()));
        }
        return fixtureOut.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + fileName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "'" + fileName + "' was not generated; keys=" + fixtureOut.keySet()));
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
            throw new AssertionError("[DepFieldTypeCollisionSeedSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the seat-26..31 suite shape, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> drrCOutput;
    private static List<String> drrCGenErrors;

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
        if (cellCAvailable()) {
            List<String> errs = new ArrayList<>();
            drrCOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_C_ROOT), errs);
            drrCGenErrors = errs;
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

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
