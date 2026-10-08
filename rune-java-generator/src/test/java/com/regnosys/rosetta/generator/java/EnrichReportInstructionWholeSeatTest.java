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
 * SEAT 33, law <b>E.234</b> -- the composite <b>Enrich</b> law, THREE named rungs at three
 * seats, one carrier file that goes WHOLE only with all three:
 *
 * <ul>
 *   <li><b>rung E.2</b> facet {@code deepThenBareElementStampSuppress} (sig B015,
 *       {@code CollectionHandler}): the deep-then DECL of a {@code default} level whose every
 *       face is a with-args call to a META-FREE-output function keeps the parser's BARE
 *       element -- the #144 refs re-key must not stamp a wrapper that merely SURVIVES in the
 *       level's refs.
 *       <pre>golden: final MapperC&lt;Party&gt; thenArg = MapperC.&lt;Party&gt;of(filterPartyNoMatch.evaluate(...
 *fork:   final MapperC&lt;ReferenceWithMetaParty&gt; thenArg = MapperC.&lt;Party&gt;of(filterPartyNoMatch.evaluate(...</pre></li>
 *   <li><b>rung E.3</b> facet {@code aliasDefaultSingleTopWrap} (sig B030,
 *       {@code FunctionExpressionRenderer}): an {@code RDefaultExpr} alias top under a
 *       {@code MapperS<} signature is a BARE-ITEM top, so the sink route re-applies
 *       compileAliases' item-top wrap on the consumer.
 *       <pre>golden: return MapperS.of(MapperS.of(extractPartyByNameContains.evaluate(...)).getOrDefault(...));
 *fork:   return MapperS.of(extractPartyByNameContains.evaluate(...)).getOrDefault(...);</pre></li>
 *   <li><b>rung E.4</b> facet {@code ctorAsKeyBareValueReference} (sigs B012 + B027,
 *       {@code ConstructionHandler}): an {@code as-key} ctor pair whose value is an ALIAS CALL
 *       already yielding the keyed type's BARE value hoists that value into a local and copies
 *       the meta KEYS into a reference-only wrapper builder.
 *       <pre>golden: final Party _reportingParty = reportingParty(reportableEvent).get();
 *        ....setReportingParty(ReferenceWithMetaParty.builder().setGlobalReference(Optional.ofNullable(_reportingParty)...
 *fork:   ....setReportingPartyValue(reportingParty(reportableEvent).get())</pre></li>
 * </ul>
 *
 * <p><b>CARRIER (CLAIMED -- measured by the chain).</b>
 * {@code drr/enrichment/common/test/functions/Enrich_TransactionReportInstructionTestPackDefault.java}
 * x drr 7.0.0 / 7.1.0 / 7.2.0 / 7.3.0 FUNCTION. <b>52 changed lines / 4 hunks</b> per file at
 * {@code diff -U0}, and the four goldens are ONE md5 -- so the four cells are one shape, not
 * four. Per rung: E.3 is the outer {@code MapperS.of(} (1 line), E.2 is one decl token (1
 * line), E.4 is 4 hoisted locals + 4 setter blocks (42 of the 52). <b>Each rung alone heals
 * ZERO files; the three together heal FOUR (band 20 -&gt; 16).</b>
 * {@code import java.util.Optional;} is already line 26 of the fork, so no import sig moves.
 *
 * <p><b>GREEN-SAFETY, MEASURED, per rung (LAW 75).</b>
 * <ul>
 *   <li><b>E.2</b> -- the arm can only fire where the #144 scan would otherwise return a
 *       wrapper AND the level is an {@code RDefaultExpr}: corpus-wide that is <b>29 rows</b>.
 *       Four are the carrier; the other <b>25 are GREEN and their goldens KEEP the wrapper</b>
 *       -- {@code rule:QuantitySchedule} 16 (golden
 *       {@code final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule> thenArg1..4}) and
 *       {@code fn:MessageID} 9 (golden {@code final MapperS<FieldWithMetaString> _thenArg}).
 *       The FACE test is what separates them: their faces are NAV paths, the carrier's are
 *       with-args fn calls whose declared output carries no metadata. Both green files are
 *       locked at BYTE grade by {@code corpus_cGUARD} in this suite.</li>
 *   <li><b>E.3</b> -- <b>16 rows over FOUR aliases</b> reach the sink route's item-top gate
 *       with an {@code RDefaultExpr} body ({@code reportingParty}, {@code fpmlTrade},
 *       {@code payerReceiver}, {@code utiFromReportableInformation}). The
 *       {@code signatureType.startsWith("MapperS<")} test sits ON the new flag, so
 *       {@code utiFromReportableInformation} (a {@code MapperC<String>} signature) is not
 *       touched at all; the residue is {@code fpmlTrade} + {@code payerReceiver}, and if
 *       {@code corpus_control1} ever moves, that is where to look first.</li>
 *   <li><b>E.4</b> -- of the <b>76</b> as-key ctor pairs in the corpus,
 *       {@code itemIsWrapper=false} selects <b>exactly the 16 carrier rows</b>; the other
 *       <b>60 are GREEN</b> ({@code Create_ReportingSideFromReportableEvent} 40 +
 *       {@code Create_PartyChange} 20) and all read {@code itemIsWrapper=true}. The drr 7.0.0
 *       twin is locked at BYTE grade by {@code corpus_cGUARD}.</li>
 * </ul>
 *
 * <p><b>The E.4 divergence is SEMANTIC, and the green argument is a corpus scan, not a
 * compiler verdict.</b> The fork's {@code .setReportingPartyValue(Party)} COMPILES -- the
 * generated builder carries both the wrapper setter and the {@code ...Value} overload. What is
 * wrong is that it sets the VALUE where upstream's {@code assignAsKey} copies the
 * global/external REFERENCE. Read-only walk of every {@code generated/java} file under
 * {@code test-corpus} (<b>174,141</b> goldens, {@code os.walk(followlinks=True)} -- ripgrep
 * does not follow the corpus junctions): <b>115</b> carry the as-key reference idiom
 * {@code setGlobalReference(Optional.ofNullable(} and <b>0 of those 115</b> carry any
 * {@code .set<X>Value(} call. No golden mixes the two forms in one file, let alone at one pair.
 *
 * <p><b>CLAIMED RED at the base head {@code fa49da010}</b> (both routes; the chain measures
 * it): {@code a1}, {@code a2}, {@code a3}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1} (+ {@code corpus_control2}, {@code corpus_control3} under
 * {@code -Pir-on}). {@code corpus_control1}'s base RED is DOUBLE: the print-first domain
 * sentinel AND the Enrich residue row. <b>CLAIMED GREEN at the law head:</b> the whole suite.
 * {@code corpus_control0}, {@code corpus_cGUARD}, {@code e1} and {@code e2} are GREEN at BOTH
 * heads by design -- control0 is the golden-side oracle (prove the instrument can fail),
 * cGUARD the three-file corpus-grade DECLINE lock, e1/e2 the fixture-grade ones.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- CLAIMED; each is MEASURED by the chain and this javadoc is
 * rewritten from the lane logs (LAW 82).</b>
 * <ul>
 *   <li><b>m-lawE2-facesbare</b> (the FACE test severed, leaving a bare
 *       {@code declKind == RDefaultExpr} suppressor): CLAIMED NON-EMPTY -- <b>25 green rows
 *       over two named files</b>. {@code corpus_cGUARD} (QuantitySchedule + MessageID) and
 *       {@code corpus_control1} (T3) must MOVE; {@code e1} must FAIL; {@code a1} still
 *       passes.</li>
 *   <li><b>m-lawE3-wrap</b> (the wrap disjunct severed while {@code defaultBareTop} still
 *       feeds {@code itemTypedTop}, so the alias takes the {@code FER:2742} return instead):
 *       CLAIMED {@code a2}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1}.</li>
 *   <li><b>m-lawE3-sig</b> (the {@code MapperS<} conjunct severed from {@code defaultBareTop}
 *       -- the dossier's REFUTED sketch, restored): CLAIMED {@code e2} FAILS.
 *       <b>Whether any corpus BYTE moves is UNMEASURED</b> and this javadoc does not claim it:
 *       the FER:2769-2771 rollback of a declined sink attempt is documented complete, so the
 *       lane may score EMPTY at corpus grade with {@code e2} as its only witness. Score it
 *       from the log.</li>
 *   <li><b>m-lawE3-single</b> (the cardinality conjunct severed): CLAIMED <b>EMPTY BY
 *       CONSTRUCTION</b>, not merely empty at this corpus --
 *       {@code CardinalityComputer.compute} answers {@code SINGLE} for EVERY
 *       {@code RDefaultExpr} on the global path ({@code case RDefaultExpr d -> SINGLE;}, the
 *       conservative default), so the conjunct cannot change a decision anywhere. It ships as
 *       a FORWARD guard against the faithful fold and the lane exists so the chain MEASURES
 *       the zero rather than this javadoc asserting it.</li>
 *   <li><b>m-lawE4-scope</b> (the {@code !itemIsWrapper} guard severed): CLAIMED NON-EMPTY --
 *       <b>60 green rows</b>. Severing the guard makes the #390 wrapper arm UNREACHABLE, so
 *       {@code Create_ReportingSideFromReportableEvent} and {@code Create_PartyChange} fall
 *       back to the caller's whole-wrapper splice; {@code corpus_cGUARD} and
 *       {@code corpus_control1} (T1 and T4) must MOVE. {@code a3} still passes.</li>
 *   <li><b>m-lawE4-reach</b> (the guard AND the {@code exprType == null} conjunct severed
 *       together, so the new arm does not merely shadow the #390 arm but RENDERS at the 60
 *       green pairs): CLAIMED the same two controls move, by the arm's own over-fire rather
 *       than by unreachability. The pair of lanes separates "the guard's position" from "the
 *       arm's reach".</li>
 *   <li><b>m-lawE4-name</b> ({@code pair.key()} emitted literally instead of
 *       {@code disambiguate(pair.key())}): CLAIMED WITNESS-UNIQUE -- the THREE escaped locals
 *       break while the fourth ({@code reportingCounterparty}, whose key was never taken)
 *       still passes, so {@code a3}'s escaped assert fails and its non-colliding assert does
 *       not.</li>
 *   <li><b>m-lawE4-exprtype / m-lawE4-sink</b>: DELIBERATELY NOT SHIPPED. Both would be EMPTY
 *       and the reason is measured, not assumed -- all 16 carrier rows read
 *       {@code exprType=null} and ALL 76 rows read a reachable sink, so neither conjunct
 *       excludes anything at this corpus. They ship as defence-in-depth with their zeros
 *       written into the src comment; declaring them as lanes would claim a population they
 *       do not have.</li>
 * </ul>
 *
 * <p><b>LAW 77 -- INHERITED at all three seats, and MEASURED, not assumed.</b> The seat-33
 * probe's four tag logs are byte-identical between the routes
 * ({@code md5sum p33off/{DEEPKEY,CTORNAME,DEFCARD,CARRY}.txt == p33on/...}), so every field
 * every rung reads has the same value on both. Structurally: {@code IRCollectionHandler}
 * extends {@code CollectionHandler} and overrides ONLY {@code thenArgBaseName} (the hoist
 * NAME, not the element read); {@code IRFunctionExpressionRenderer} does not override
 * {@code renderAliasSinkHoistsOrNull}; and {@code rune-ir-java} carries no
 * {@code isAsKey}/{@code tryCtorAsKeyReference} re-implementation at all. Analytic is not
 * measured: {@code corpus_control2} compares the IR route against GOLDEN for the carrier AND
 * route-to-route, and {@code corpus_control3} runs the whole-cell union on the IR route.
 *
 * <p><b>LAW 74.</b> Two of the three rungs are COMPILE repairs, cited from the seat-33 PRE
 * measurement ({@code target/seat33-instruments/javac33/}, the seat-32 report's row labels):
 * <b>C6a</b> ({@code reportingParty}, rung E.3) <b>1 error</b> --
 * {@code Seat33Pre.java:436 incompatible types: Party cannot be converted to
 * MapperS<? extends Party>}; <b>C6b</b> ({@code reportingCounterpartyRole}, rung E.2)
 * <b>3 errors</b> -- {@code :440} x2 {@code bad type in conditional expression} and
 * {@code :442 MapperS<ReferenceWithMetaParty> cannot be converted to MapperS<? extends
 * Party>}. PRE 4 -&gt; POST 0 of the 38 STANDING. <b>Row C6c ({@code reportingSide}, rung E.4)
 * is STANDING 0, MEASURED CLEAN -- rung E.4 is a BYTE law and nothing in this suite, its
 * commit message or the PR body may say it "cannot compile".</b>
 *
 * <p><b>LAW-81 tripwires this heal WILL fire in OTHER suites</b> (not edited here; each is
 * re-run and re-pinned from ITS OWN print in the same commit). All five rows are pure
 * REMOVALS -- the carrier becomes byte-identical to golden, so no scan can gain a row, and no
 * DOMAIN pin moves (every one of the five goldens keeps a non-zero tuple):
 * {@code AliasCallInputEscapeSeatTest:431} ({@code fork=[6, 0, 12, 0] golden=[6, 0, 15, 6]});
 * {@code CtorSetterValueWrapperRecoverySeatTest:615} ({@code fork=[4, 4, 8] golden=[0, 16,
 * 16]}, rung E.4); {@code DefaultCollapsingRightSeatTest:445} ({@code fork=[0, 0, 0]
 * golden=[0, 0, 1]}, rung E.3); {@code ThenWrappedDefaultSeatTest:418} ({@code fork=[16, 1,
 * 0] golden=[16, 17, 1]}); {@code DeclaredThenArgTypeSeatTest:539-541}, whose
 * {@code CONTROL1_PRE_EXISTING_MISMATCHES} becomes <b>EMPTY</b> (Enrich is its only entry, and
 * its own comment already names the lever as "direction B (gen WRAPPER / golden BARE)" = rung
 * E.2; its {@code scanned.size() >= 8} floor is unaffected). Prose-only mentions that move
 * nothing: {@code DefaultMetaJoinSeatTest:430}, {@code RulePathFilterLolSeatTest:156},
 * {@code FilterPredicateMetaDerefSeatTest:593} (its {@code KNOWN_RESIDUE_7} carries NO Enrich
 * row) and {@code CtorSetterValueWrapperRecoverySeatTest:152}.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 12/0F/2skip default (f33-green-default.log) /
 * 12/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 6F = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1; {@code -Pir-on} 8F = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1, corpus_control2, corpus_control3 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawE2-facesbare}</b> ({@code CH_PAIRS_MUT_E2}): MEASURED 12/3F/2skip = corpus_cGUARD, corpus_control1, e1 - MATCH on the default-profile members (e1, cGUARD, control1); the claimed control3 is an IR-route control (@EnabledIf cellAAndIrProviderAvailable) SKIPPED on the default profile this lane runs - NOT MEASURED, and the claim listing it was mis-specified.</li>
 *   <li><b>{@code m-lawE3-wrap}</b> ({@code FER_PAIRS_MUT_E3WRAP}): MEASURED 12/4F/2skip = a2, corpus_c1, corpus_c2, corpus_control1 - MATCH on the default-profile members (a2, c1, c2, control1); control3 (IR-gated) SKIPPED - NOT MEASURED, the claim listing it was mis-specified.</li>
 *   <li><b>{@code m-lawE3-sig}</b> ({@code FER_PAIRS_MUT_E3SIG}): MEASURED 12/0F/2skip = (none) - RE-SCORED: EMPTY - e2 HELD: restoring the refuted `MapperS<` conjunct on defaultBareTop moves nothing; e2 is held by the FACE test, not by this conjunct - defence-in-depth, its corpus effect now MEASURED empty rather than unknown.</li>
 *   <li><b>{@code m-lawE3-single}</b> ({@code FER_PAIRS_MUT_E3SINGLE}): MEASURED 12/0F/2skip = (none) - EMPTY-as-declared (by construction, NOTES § 6).</li>
 *   <li><b>{@code m-lawE4-scope}</b> ({@code CO_PAIRS_MUT_E4SCOPE}): MEASURED 12/2F/2skip = corpus_cGUARD, corpus_control1 - MATCH on the default-profile members (cGUARD, control1); control3 (IR-gated) SKIPPED - NOT MEASURED, the claim listing it was mis-specified.</li>
 *   <li><b>{@code m-lawE4-reach}</b> ({@code CO_PAIRS_MUT_E4REACH}): MEASURED 12/2F/2skip = corpus_cGUARD, corpus_control1 - MATCH on the default-profile members - the same 2 as scope (cGUARD, control1); control3 (IR-gated) SKIPPED - NOT MEASURED.</li>
 *   <li><b>{@code m-lawE4-name}</b> ({@code CO_PAIRS_MUT_E4NAME}): MEASURED 12/3F/2skip = a3, corpus_c1, corpus_c2 - RE-SCORED (superset): a3 AND c1, c2 - the un-disambiguated key breaks the carrier too, so the collision the fixture reproduces is the corpus's own; the lane is corpus-witnessed, not fixture-grade as claimed (control1 held: the name is not a scanned token).</li>
 * </ul>
 */
class EnrichReportInstructionWholeSeatTest {

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

    /** The carrier -- identical in all four drr 7.x cells (52 diff lines / 4 hunks each). */
    private static final String ENRICH =
            "drr/enrichment/common/test/functions/"
            + "Enrich_TransactionReportInstructionTestPackDefault.java";

    /**
     * The THREE green near-misses, at corpus grade. The first two are rung E.2's other
     * {@code declKind=RDefaultExpr} scan carriers (16 + 9 of the 29 rows) whose goldens KEEP
     * the wrapper; the third is rung E.4's {@code itemIsWrapper=true} twin (40 of the 60 green
     * as-key rows) and the ONLY other drr 7.0.0 golden that carries the as-key reference copy.
     */
    private static final String QUANTITY_SCHEDULE_RULE =
            "drr/base/trade/quantity/reports/QuantityScheduleRule.java";
    private static final String MESSAGE_ID =
            "drr/regulation/common/trade/link/functions/MessageID.java";
    private static final String CREATE_REPORTING_SIDE =
            "drr/enrichment/common/trade/functions/Create_ReportingSideFromReportableEvent.java";

    private static final List<String> GREEN_NEAR_MISSES =
            List.of(QUANTITY_SCHEDULE_RULE, MESSAGE_ID, CREATE_REPORTING_SIDE);

    /** Cell A = drr 7.0.0 -- the carrier, the green witnesses and every whole-cell control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    /** Cell B = drr 7.3.0 -- the far end of the carrier's cell range (a different CDM). */
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
    // The three rungs' golden tokens, transcribed from
    //   test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java/<ENRICH>
    // and cross-checked against the fork's own OFF-route band dump.
    // =========================================================================

    /** rung E.2 -- golden's BARE decl element (fork carries the wrapper form below). */
    private static final String E2_GOLDEN =
            "final MapperC<Party> thenArg = MapperC.<Party>of(filterPartyNoMatch.evaluate(";
    private static final String E2_FORK = "final MapperC<ReferenceWithMetaParty> thenArg = ";

    /** rung E.3 -- golden's item-top wrap (fork returns the bare consumer). */
    private static final String E3_GOLDEN =
            "return MapperS.of(MapperS.of(extractPartyByNameContains.evaluate("
            + "parties(reportableEvent).getMulti(), \"reportingParty\")).getOrDefault(";
    private static final String E3_FORK =
            "return MapperS.of(extractPartyByNameContains.evaluate("
            + "parties(reportableEvent).getMulti(), \"reportingParty\")).getOrDefault(";

    /** rung E.4 -- golden's four hoisted locals, the builder head, and the fork's splice. */
    private static final List<String> E4_GOLDEN_LOCALS = List.of(
            "final Party _reportingParty = reportingParty(reportableEvent).get();",
            "final Party _partyResponsibleForReporting = "
                    + "partyResponsibleForReporting(reportableEvent).get();",
            "final Party _reportSubmittingParty = reportSubmittingParty(reportableEvent).get();",
            "final Party reportingCounterparty = "
                    + "reportingCounterpartyRole(reportableEvent).get();");
    private static final String E4_GOLDEN_BUILDER =
            ".setReportingParty(ReferenceWithMetaParty.builder()";
    private static final String E4_GOLDEN_KEYCOPY = "map(m -> m.getGlobalKey())";
    private static final List<String> E4_FORK_SPLICES = List.of(
            ".setReportingPartyValue(reportingParty(reportableEvent).get())",
            ".setPartyResponsibleForReportingValue(",
            ".setReportSubmittingPartyValue(",
            ".setReportingCounterpartyValue(");

    // =========================================================================
    // control0 -- golden is the oracle (prove the instrument can fail)
    // =========================================================================

    /**
     * control0 -- every token this suite asserts is read back off GOLDEN first, so a broken
     * scan or a mistranscribed literal cannot pass quietly. Golden Enrich carries each rung's
     * token exactly once (four times for the key copy, one per as-key pair) and carries NONE
     * of the fork's forms; the two E.2 green near-misses carry the wrapper decls the law must
     * leave alone; the E.4 green twin carries the same four key copies the carrier will gain.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenIsTheOracle() throws IOException {
        String g = normalize(Files.readString(GOLDEN_A.resolve(ENRICH)));
        assertEquals(1, count(g, E2_GOLDEN), "golden must carry the bare decl element once");
        assertEquals(0, count(g, E2_FORK), "golden must NOT carry the wrapper decl element");
        assertEquals(1, count(g, E3_GOLDEN), "golden must carry the alias item-top wrap once");
        assertEquals(0, count(g, E3_FORK), "golden must NOT carry the unwrapped consumer");
        for (String local : E4_GOLDEN_LOCALS) {
            assertEquals(1, count(g, local), "golden must carry the hoisted local: " + local);
        }
        assertEquals(1, count(g, E4_GOLDEN_BUILDER),
                "golden must carry the reference-only builder head once");
        assertEquals(4, count(g, E4_GOLDEN_KEYCOPY),
                "golden must copy the global key for all four as-key pairs");
        for (String splice : E4_FORK_SPLICES) {
            assertEquals(0, count(g, splice), "golden must NOT carry the value splice: " + splice);
        }
        // The green near-misses' own oracle rows.
        String gq = normalize(Files.readString(GOLDEN_A.resolve(QUANTITY_SCHEDULE_RULE)));
        assertEquals(5, count(gq, "final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule>"),
                "golden QuantityScheduleRule KEEPS five wrapper-typed then-arg decls -- rung"
                + " E.2 must not drop one of them");
        String gm = normalize(Files.readString(GOLDEN_A.resolve(MESSAGE_ID)));
        assertEquals(1, count(gm, "final MapperS<FieldWithMetaString> _thenArg"),
                "golden MessageID KEEPS its wrapper-typed then-arg decl");
        String gc = normalize(Files.readString(GOLDEN_A.resolve(CREATE_REPORTING_SIDE)));
        assertEquals(4, count(gc, E4_GOLDEN_KEYCOPY),
                "golden Create_ReportingSideFromReportableEvent carries the SAME four key"
                + " copies through the #390 wrapper arm -- rung E.4 must not re-render them");
    }

    // =========================================================================
    // Part A -- the reduced fixtures, one per rung
    // =========================================================================

    /**
     * a1 (rung E.2) -- the deep-then decl of a {@code default} level whose BOTH faces are
     * with-args calls to a meta-free-output function keeps the BARE element.
     *
     * <p>Reduced from {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
     * enrichment-common-test-func.rosetta}: the {@code reportingCounterpartyRole} alias at
     * lines 36-39 ({@code FilterPartyNoMatch(counterparties -> partyReference, reportingParty)
     * default FilterPartyNoMatch(parties, reportingParty) then first}) plus {@code func
     * FilterPartyNoMatch} at lines 80-87, whose declared output
     * {@code filteredParties Party (1..*)} carries NO metadata annotation -- which is the
     * whole discriminator. The reduction keeps every construct the mechanism needs: a
     * {@code [metadata reference]} hop into the left face's argument (so the wrapper enters
     * the level's refs), MULTI faces (so the level is a {@code MapperC}, the carrier's own
     * cardinality), and the bare-item {@code then first} that makes the alias take the
     * deep-then SINK route rather than one of the other three alias arms.
     *
     * <p>PIN AT RED: the assert names the LAW's token (the decl element). If the fixture does
     * not render a {@code thenArg} decl at all, RESHAPE the fixture -- do not weaken it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_e2_deepThenDefaultDeclKeepsTheBareElement() throws IOException {
        String out = fixtureFunction("A1DeepThenBareElementStamp");
        assertContains(out, "<Pty> thenArg");
        assertNotContains(out, "<ReferenceWithMetaPty> thenArg");
    }

    /**
     * a2 (rung E.3) -- an {@code RDefaultExpr} alias top under a {@code MapperS<} signature
     * gets compileAliases' item-top wrap on the sink route's consumer.
     *
     * <p>Reduced from the same file's {@code reportingParty} alias at lines 30-34
     * ({@code ExtractPartyByNameContains(parties, "reportingParty") default
     * ExtractCounterpartyByRole(counterparties, ...) -> partyReference}). The
     * {@code -> partyReference} meta hop on the RIGHT face is load-bearing: it is the producer
     * that fires the sink route in the first place (it hoists the wrapper local), so the first
     * assert is the fixture's OWN witness -- if that local is absent the fixture is not
     * exercising the sink route and the law's assert would be vacuous.
     *
     * <p>PIN AT RED: if the hoist witness is missing, RESHAPE the fixture.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_e3_aliasDefaultSingleTopWrapsTheConsumer() throws IOException {
        String out = fixtureFunction("A2AliasDefaultSingleTopWrap");
        assertContains(out, "final ReferenceWithMetaPty referenceWithMetaPty = ");
        assertContains(out, "return MapperS.of(MapperS.of(");
    }

    /**
     * a3 (rung E.4) -- an {@code as-key} ctor pair whose value is a bare-yielding ALIAS CALL
     * hoists the value into a local and copies the meta KEYS into a reference-only wrapper
     * builder, and the local's NAME is the ctor key run through the scope's escape loop.
     *
     * <p>Reduced from the same file's {@code reportingSide} alias at lines 47-53. Two pairs,
     * deliberately: {@code reportingParty} collides with the alias METHOD of the same name and
     * must escape to {@code _reportingParty}, while {@code reportingCounterparty} (whose value
     * is the {@code reportingCounterpartyRole} alias) collides with nothing and must stay
     * bare. That split is the {@code m-lawE4-name} lane's witness and it is golden's own split
     * at the carrier (three escaped, one not).
     *
     * <p>PIN AT RED: the asserts name the LAW's tokens (the hoisted locals, the builder head,
     * the key copy). If the fixture renders no as-key ctor pair at all, RESHAPE it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_e4_ctorAsKeyBareValueHoistsTheReferenceCopy() throws IOException {
        String out = fixtureFunction("A3CtorAsKeyBareValueReference");
        assertContains(out, "final Pty _reportingParty = reportingParty(");
        assertContains(out, "final Pty reportingCounterparty = reportingCounterpartyRole(");
        assertContains(out, ".setReportingParty(ReferenceWithMetaPty.builder()");
        assertContains(out, ".setReportingCounterparty(ReferenceWithMetaPty.builder()");
        assertEquals(2, count(out, E4_GOLDEN_KEYCOPY),
                "both as-key pairs must copy the global key:\n" + out);
        assertNotContains(out, ".setReportingPartyValue(");
        assertNotContains(out, ".setReportingCounterpartyValue(");
    }

    // =========================================================================
    // The DECLINE locks
    // =========================================================================

    /**
     * e1 (rung E.2's decline) -- the minimal pair of {@code a1}: the SAME level, the SAME
     * refs, the SAME cardinality, with ONE face changed from a with-args fn call to a NAV
     * path. That is the {@code rule:QuantitySchedule} / {@code fn:MessageID} shape, and the
     * decl must KEEP the wrapper (bytes unchanged, at both heads). It is the direction that
     * proves the FACE test -- not "is this a default?" -- is the discriminator.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_e2_aNavFaceDefaultKeepsTheWrapperDecl() throws IOException {
        String out = fixtureFunction("E1DeepThenDefaultNavFace");
        assertContains(out, "<ReferenceWithMetaPty> thenArg");
        assertNotContains(out, "<Pty> thenArg");
    }

    /**
     * e2 (rung E.3's decline) -- an {@code RDefaultExpr} alias top whose faces are MULTI, so
     * the alias signature is a {@code MapperC<...>}. This is the
     * {@code alias:utiFromReportableInformation} shape, one of the four rows that reach the
     * sink route's item-top gate. The signature test lives ON {@code defaultBareTop}, so this
     * alias gains NEITHER flag and takes neither the new wrap nor a new early return.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_e3_aMapperCSignatureAliasTakesNoWrap() throws IOException {
        String out = fixtureFunction("E2AliasDefaultMultiTop");
        assertContains(out, "protected MapperC<");
        assertNotContains(out, "return MapperS.of(MapperS.of(");
    }

    // =========================================================================
    // Corpus -- the whole-file heals
    // =========================================================================

    /**
     * corpus_c1 -- the drr 7.0.0 whole-file heal. The three rung asserts run AHEAD of the byte
     * compare so a single-rung regression reports as that rung rather than as a 52-line diff,
     * and so each rung's mutation lane names its own line.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700EnrichMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(ENRICH)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + ENRICH + ": " + own);
        String gen = drrAOutput.get(ENRICH);
        assertNotNull(gen, "not generated: " + ENRICH);
        gen = normalize(gen);
        assertContains(gen, E2_GOLDEN);
        assertNotContains(gen, E2_FORK);
        assertContains(gen, E3_GOLDEN);
        assertNotContains(gen, E3_FORK);
        for (String local : E4_GOLDEN_LOCALS) {
            assertContains(gen, local);
        }
        assertEquals(4, count(gen, E4_GOLDEN_KEYCOPY),
                "rung E.4 must copy the global key for all four as-key pairs (fork 0 ->"
                + " golden 4)");
        for (String splice : E4_FORK_SPLICES) {
            assertNotContains(gen, splice);
        }
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(ENRICH))), gen,
                ENRICH + " (drr 7.0.0) must be byte-identical to golden -- seat 33 law E.234:"
                + " the file goes WHOLE only with all three rungs");
    }

    /**
     * corpus_c2 -- the SECOND cell. drr 7.3.0 resolves against a different transitive CDM than
     * 7.0.0, so this is an independent render even though the two goldens are one md5 (7.1.0
     * and 7.2.0 carry the identical row and are covered by the chain's matrix, not here).
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730EnrichMatchesGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        List<String> own = drrBGenErrors.stream().filter(e -> e.contains(ENRICH)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + ENRICH + ": " + own);
        String gen = drrBOutput.get(ENRICH);
        assertNotNull(gen, "not generated: " + ENRICH);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(ENRICH))), normalize(gen),
                ENRICH + " (drr 7.3.0) must be byte-identical to golden too -- the law is"
                + " cell-independent");
    }

    // =========================================================================
    // cGUARD -- the corpus-grade DECLINE lock for rungs E.2 and E.4
    // =========================================================================

    /**
     * corpus_cGUARD -- <b>the three green near-misses stay byte-identical to golden.</b> These
     * are not decoration: {@code QuantityScheduleRule} (16 of rung E.2's 29 reachable rows)
     * and {@code MessageID} (9 more) are the files a naive
     * {@code declKind == RDefaultExpr} suppressor BREAKS, and
     * {@code Create_ReportingSideFromReportableEvent} (40 of rung E.4's 60 green rows) is the
     * file a rung-E.4 arm without its {@code !itemIsWrapper} scoping breaks. All three are
     * byte-identical to golden at the base head (they are outside the drr 7.0.0 band, whose
     * six files are the cell's entire mismatch set), so this is a pure decline lock -- GREEN
     * at both heads by construction, and the target of {@code m-lawE2-facesbare},
     * {@code m-lawE4-scope} and {@code m-lawE4-reach}.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_cGUARD_greenNearMissesStayByteIdentical() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        for (String p : GREEN_NEAR_MISSES) {
            String gen = drrAOutput.get(p);
            assertNotNull(gen, "not generated: " + p);
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(p))), normalize(gen),
                    "a GREEN near-miss moved -- the law over-fired at " + p);
        }
        // Named separately so the failure text says WHICH mechanism moved, not just "bytes".
        assertEquals(5, count(normalize(drrAOutput.get(QUANTITY_SCHEDULE_RULE)),
                        "final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule>"),
                "rung E.2 dropped a wrapper decl golden KEEPS (QuantityScheduleRule)");
        assertEquals(1, count(normalize(drrAOutput.get(MESSAGE_ID)),
                        "final MapperS<FieldWithMetaString> _thenArg"),
                "rung E.2 dropped a wrapper decl golden KEEPS (MessageID)");
        assertEquals(4, count(normalize(drrAOutput.get(CREATE_REPORTING_SIDE)),
                        E4_GOLDEN_KEYCOPY),
                "rung E.4 moved the #390 wrapper arm's own key copies"
                + " (Create_ReportingSideFromReportableEvent)");
    }

    // =========================================================================
    // The whole-cell UNION controls (LAW 79)
    // =========================================================================

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0 (default route). Domain =
     * every EMITTED file whose GOLDEN <b>or</b> FORK text carries a non-zero tuple; per file
     * the (T1, T2, T3, T4) tuple must equal golden's beyond the NAMED residue. Union, never
     * intersection -- an intersection scan is blind in both directions.
     *
     * <p>The control fails when the law OVER-fires (a green file gains or loses a counted
     * shape anywhere in the cell) and when it UNDER-fires (the carrier's row stays).
     *
     * <p>The GOLDEN-side domain is pinned exactly and is derivable WITHOUT the generator: a
     * read-only walk of {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java}
     * (7,808 goldens) finds <b>762</b> carrying a non-zero tuple
     * ({@code target/seat33-instruments/drafts33/E234/derive-domain.py}). The UNION domain
     * additionally depends on which of those the fork EMITS, so it carries the print-first
     * sentinel until the chain prints it.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellShapeEqualsGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error -- the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_A));
        assertEquals(GOLDEN_DOMAIN_DRR700, golden.size(),
                "the GOLDEN tree this control scans must be the frozen one (read-only walk:"
                + " 762 of 7808 goldens carry a non-zero tuple)");
        assertUnionEqual(scan(drrAOutput), golden, drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, UNION_DOMAIN_DRR700);
    }

    /**
     * control2 -- LAW 77, per FILE, on BOTH routes. All three seats are legacy handlers the IR
     * route delegates to ({@code IRCollectionHandler} overrides only {@code thenArgBaseName};
     * {@code IRFunctionExpressionRenderer} does not override the alias sink route;
     * {@code rune-ir-java} has no as-key re-implementation at all), and the seat-33 probe's
     * four tag logs are byte-identical between the routes -- so the ON route must reach the
     * SAME bytes. Asserted against GOLDEN <b>and</b> route-to-route, so a shared-wrong render
     * still reports as a route fact.
     *
     * <p>Skipped unless the IR provider is on the classpath: the {@code -Pir-on} leg of the
     * chain is what makes it load-bearing.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        String golden = normalize(Files.readString(GOLDEN_A.resolve(ENRICH)));
        String ir = irCellA().get(ENRICH);
        assertNotNull(ir, "the IR route did not generate: " + ENRICH);
        assertEquals(golden, normalize(drrAOutput.get(ENRICH)),
                "the LEGACY route differs from golden: " + ENRICH);
        assertEquals(golden, normalize(ir),
                "the IR route differs from golden -- the LAW-77 leg: if this fails while"
                + " corpus_c1 passes, one of the three seats has an IR twin that"
                + " re-implements it: " + ENRICH);
        assertEquals(normalize(drrAOutput.get(ENRICH)), normalize(ir),
                "route divergence on " + ENRICH);
    }

    /**
     * control3 -- the SAME whole-cell UNION scan, on the IR ROUTE. A two-file byte compare is
     * not a whole-cell instrument, and LAW 77 is a claim about the WHOLE cell: the band lists
     * are file-for-file identical on both routes at this head, so the residue list is shared
     * with {@code control1} and route identity is the assertion (its rows read "fork=", which
     * the IR route must reproduce exactly).
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control3_irRouteDrr700WholeCellShapeEqualsGolden() throws IOException {
        Map<String, String> irOut = irCellA();
        assertUnionEqual(scan(irOut), scan(readGoldenTree(GOLDEN_A)), irOut.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, UNION_DOMAIN_DRR700);
    }

    // =========================================================================
    // Pins
    // =========================================================================

    /**
     * DERIVED by a read-only walk of the FROZEN golden tree at the seat-33 base -- the count
     * of drr 7.0.0 goldens carrying a non-zero (T1, T2, T3, T4) tuple: <b>762 of 7,808</b>
     * ({@code drafts33/E234/derive-domain.py}, 2026-08-27). Not a generator measurement: it
     * locks the CORPUS side of the control, so a corpus drift or a silently-empty golden read
     * fails loudly instead of making the union vacuous.
     */
    private static final int GOLDEN_DOMAIN_DRR700 = 762;

    ///PIN: PRINT-FIRST SENTINEL (LAW 73). -1 makes assertUnionEqual FAIL and PRINT
    ///PIN:   "MEASURED domain=<n> residue=[...]"
    ///PIN: at this law's head; transcribe BOTH from that print, in the same commit. It cannot
    ///PIN: be derived offline: the universe is the GOLDEN-or-FORK tuple-bearing files
    ///PIN: INTERSECTED with the cell's own emission key set, and the emission set is only
    ///PIN: known once the generator has run. Derived scale: 762 of the 7,808 goldens carry a
    ///PIN: non-zero tuple, and the seat-32 precedent (A.2's control on the same cell) measured
    ///PIN: a union domain roughly half its golden domain -- expect the mid hundreds.
    ///PIN: MEASURED 529 at the RED run (E234-red1.log, corpus_control1) - transcribed.
    private static final int UNION_DOMAIN_DRR700 = 529;

    /**
     * DERIVED at the seat-33 base from the OFF-route band dump: every non-band file in the
     * cell is byte-identical to golden and therefore cannot contribute a row, so the residue
     * is exactly the band files whose TUPLE differs. The drr 7.0.0 FUNCTION band is SIX files;
     * {@code StrikePrice} already agrees on this tuple, and this law removes
     * {@code Enrich_...TestPackDefault fork=[0, 0, 1, 1] golden=[4, 1, 0, 1]}, leaving these
     * FOUR -- each an OTHER law's charter in this same seat.
     *
     * <p><b>LAW 81, disclosed up front:</b> this list is the seat's own remaining band and
     * later commits WILL empty it -- {@code GetBasketConstituents} at laws A.4/A.3/A.5/A.1/A.2,
     * {@code Price} at C.1/C.2, {@code QuantityUnitOfMeasure} at B.1/B.3/B.24,
     * {@code TotalNotionalQuantity} at D.3. Re-pin from THIS control's own print at each, and
     * treat a row that moves for any other reason as a measurement, not a licence to weaken
     * the list.
     *
     * <p>Sorted as {@code assertUnionEqual} prints them (a {@code TreeSet} universe).
     */
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of(
            // the GetBasketConstituents row (fork=[0, 0, 0, 2] golden=[0, 0, 0, 4]) LEFT this
            // list at seat 33: laws A.4/A.3/A.5/A.1/A.2 healed the file WHOLE in all four
            // drr 7.x cells (exactly as this list's own javadoc scheduled); transcribed from
            // the checkpoint's own gensuite print (ckpt1-gensuite.log), a pure row removal.
            // the Price row (fork=[0, 0, 0, 10] golden=[0, 0, 0, 12]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary) took Price WHOLE in all four drr 7.x cells;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            // the QuantityUnitOfMeasure row (fork=[0, 0, 6, 1] golden=[0, 0, 7, 7]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth + iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) took QUOM WHOLE in all four drr 7.x cells - the band's last four files;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            // the TotalNotionalQuantity row (fork=[0, 0, 0, 10] golden=[0, 0, 2, 11]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit, seven rungs) took TNQ WHOLE in all four drr 7.x cells;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            );

    // =========================================================================
    // The scan + the union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * (T1, T2, T3, T4) per file -- LITERAL occurrence counts (no regex on generated Java; the
     * house {@code contains}-style idiom), one per rung plus one global over-fire net:
     * <ul>
     *   <li><b>T1</b> {@code map(m -> m.getGlobalKey())} -- rung E.4's ADDED shape, the as-key
     *       reference copy. Only TWO drr 7.0.0 goldens carry it (the carrier and its green
     *       twin), so a third file appearing here is an over-fire by itself. Carrier
     *       0 -&gt; 4.</li>
     *   <li><b>T2</b> {@code return MapperS.of(MapperS.of(} -- rung E.3's ADDED shape, the
     *       alias item-top double wrap. 28 drr 7.0.0 goldens already carry it; a green alias
     *       newly gaining one shows here. Carrier 0 -&gt; 1.</li>
     *   <li><b>T3</b> the four {@code final Mapper[CS]<(Reference|Field)WithMeta} literals --
     *       the WRAPPER-TYPED hoist declaration population, which is the ONLY thing rung E.2
     *       can move and it can only move it DOWN. Deliberately global: 119 drr 7.0.0 goldens
     *       carry one, {@code QuantityScheduleRule} five and {@code MessageID} one. Carrier
     *       1 -&gt; 0.</li>
     *   <li><b>T4</b> {@code == null ? null :} -- the over-fire NET for rung E.4. The #390
     *       wrapper arm this law must not disturb emits a guarded value local per pair
     *       ({@code x == null ? null : x.getValue();}); rung E.4's own arm emits none, so the
     *       carrier's count does NOT move (1 -&gt; 1) while a green as-key file losing its
     *       #390 render drops by four. Deliberately global -- a guarded deref added in one
     *       place cannot cancel one lost in another.</li>
     * </ul>
     * A file with an all-zero tuple in BOTH trees stays out of the domain.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = normalize(e.getValue());
            int t1 = count(code, "map(m -> m.getGlobalKey())");
            int t2 = count(code, "return MapperS.of(MapperS.of(");
            int t3 = count(code, "final MapperC<ReferenceWithMeta")
                    + count(code, "final MapperS<ReferenceWithMeta")
                    + count(code, "final MapperC<FieldWithMeta")
                    + count(code, "final MapperS<FieldWithMeta");
            int t4 = count(code, "== null ? null :");
            if (t1 + t2 + t3 + t4 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3, t4});
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
        int[] zero = new int[4];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "the tuple differs beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted tuple-bearing files (" + expectedDomain
                        + ")");
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

    // =========================================================================
    // Harness (the seat-26..32 suite shape verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> irAOutput;

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

    /** The drr 7.0.0 cell on the IR route, generated once and shared by control2 + control3. */
    private static Map<String, String> irCellA() throws IOException {
        if (irAOutput == null) {
            irAOutput = generateCellOnIrRoute(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                    new ArrayList<>());
        }
        return irAOutput;
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

    // =========================================================================
    // Fixture harness (the AliasCallInputEscapeSeatTest link/render pattern)
    // =========================================================================

    /**
     * Reduced from {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
     * enrichment-common-test-func.rosetta}: {@code func
     * Enrich_TransactionReportInstructionTestPackDefault} (lines 16-66 -- the
     * {@code reportingParty} alias at 30-34, {@code reportingCounterpartyRole} at 36-39 and
     * {@code reportingSide} at 47-53) split into three single-rung functions, plus its own
     * callee {@code func FilterPartyNoMatch} (lines 80-87, output
     * {@code filteredParties Party (1..*)} with NO metadata annotation -- the discriminator)
     * and reductions of {@code ExtractPartyByNameContains} (68-78) and
     * {@code ExtractCounterpartyByRole}.
     *
     * <p>The types are reduced to the shape the mechanism needs and nothing more: a
     * {@code [metadata key]} party, a counterparty carrying a {@code [metadata reference]} hop
     * to it (the wrapper that enters the refs), and a side whose two attributes are single
     * {@code [metadata reference]}s (the as-key targets).
     *
     * <p>{@code E1DeepThenDefaultNavFace} is {@code A1}'s minimal pair with ONE face changed
     * to a NAV path; {@code E2AliasDefaultMultiTop} is {@code A2}'s with MULTI faces (a
     * {@code MapperC} signature).
     */
    private static final String MODEL = """
            namespace census.seat33e234
            version "1.0.0"

            type Pty:
                [metadata key]

                name string (0..1)

            type Cpty:
                role string (0..1)
                partyReference Pty (0..1)
                    [metadata reference]

            type Side:
                reportingParty Pty (0..1)
                    [metadata reference]
                reportingCounterparty Pty (0..1)
                    [metadata reference]

            type Evt:
                party Pty (0..*)
                counterparty Cpty (0..*)

            func ExtractPartyByName:
                inputs:
                    parties Pty (0..*)
                    partyName string (1..1)
                output:
                    party Pty (0..1)
                set party:
                    parties
                        filter name = partyName
                        then only-element

            func ExtractCptyByRole:
                inputs:
                    counterparties Cpty (0..*)
                    wanted string (1..1)
                output:
                    cpty Cpty (0..1)
                set cpty:
                    counterparties
                        filter role = wanted
                        then only-element

            func FilterPartyNoMatch:
                inputs:
                    parties Pty (0..*)
                    party Pty (1..1)
                output:
                    filteredParties Pty (1..*)
                set filteredParties:
                    parties filter item <> party

            func PassSide:
                inputs:
                    s Side (1..1)
                output:
                    r Side (1..1)
                set r:
                    s

            func A1DeepThenBareElementStamp:
                inputs:
                    evt Evt (1..1)
                output:
                    result Pty (0..1)
                alias parties: evt -> party
                alias counterparties: evt -> counterparty
                alias reportingParty: ExtractPartyByName(parties, "reportingParty")
                alias reportingCounterpartyRole:
                    FilterPartyNoMatch(counterparties -> partyReference, reportingParty)
                        default FilterPartyNoMatch(parties, reportingParty)
                        then first
                set result:
                    reportingCounterpartyRole

            func E1DeepThenDefaultNavFace:
                inputs:
                    evt Evt (1..1)
                output:
                    result Pty (0..1)
                alias parties: evt -> party
                alias counterparties: evt -> counterparty
                alias reportingParty: ExtractPartyByName(parties, "reportingParty")
                alias navDefaultRole:
                    FilterPartyNoMatch(counterparties -> partyReference, reportingParty)
                        default counterparties -> partyReference
                        then first
                set result:
                    navDefaultRole

            func A2AliasDefaultSingleTopWrap:
                inputs:
                    evt Evt (1..1)
                output:
                    result Pty (0..1)
                alias parties: evt -> party
                alias counterparties: evt -> counterparty
                alias reportingParty:
                    ExtractPartyByName(parties, "reportingParty") default ExtractCptyByRole(
                            counterparties,
                            "P1"
                        ) -> partyReference
                set result:
                    reportingParty

            func E2AliasDefaultMultiTop:
                inputs:
                    evt Evt (1..1)
                output:
                    result Pty (1..*)
                alias parties: evt -> party
                alias counterparties: evt -> counterparty
                alias reportingParty: ExtractPartyByName(parties, "reportingParty")
                alias multiDefaultTop:
                    FilterPartyNoMatch(parties, reportingParty)
                        default counterparties -> partyReference
                set result:
                    multiDefaultTop

            func A3CtorAsKeyBareValueReference:
                inputs:
                    evt Evt (1..1)
                output:
                    result Side (1..1)
                alias parties: evt -> party
                alias reportingParty: ExtractPartyByName(parties, "reportingParty")
                alias reportingCounterpartyRole: ExtractPartyByName(parties, "counterparty")
                alias side:
                    Side {
                        reportingParty: reportingParty as-key,
                        reportingCounterparty: reportingCounterpartyRole as-key
                    }
                set result:
                    PassSide(side)
            """;

    private static RLinkingResult linking;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat33e234.rosetta");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
        }
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            link();
            GeneratorModel gm = new GeneratorModel(linking.workspace(),
                    m -> "census.seat33e234".equals(m.namespace()));
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
            Map<String, String> out = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            fg.generateWithErrors(out)
                    .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
            if (!errors.isEmpty()) {
                throw new AssertionError("fixture generation errors (a broken fixture must fail"
                        + " loudly, not skip): " + errors);
            }
            fixtureOut = out;
        }
        return fixtureOut;
    }

    private static String fixtureFunction(String fnName) throws IOException {
        return lookup(fixture(), fnName + ".java");
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
            throw new AssertionError("[EnrichReportInstructionWholeSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertFalse(out.contains(token), "forbidden token present:\n  " + token + "\nin:\n" + out);
    }
}
