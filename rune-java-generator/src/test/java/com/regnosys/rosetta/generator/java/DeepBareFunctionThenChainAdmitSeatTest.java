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
 * SEAT 32, law C.1 -- facet {@code deepBareFunctionThenChainAdmit}: <b>the DEEP then-hoist seat
 * gets the bare-FUNCTION admit the SET seat has had since PR #359 -- scoped to that ONE caller by
 * a flag, because the shared predicate's other consumers are green.</b>
 *
 * <p><b>The asymmetry, stated exactly.</b> {@code CollectionHandler.isHoistableThenChainLocal}
 * (:4610 at the base head {@code ddcdd151b}) declines a bare-invokable then-body off the rule
 * path unless one of three seat disjuncts fires:
 * <pre>
 *   if (isBareInvokableBody(t) &amp;&amp; !onRulePath &amp;&amp; !condBase &amp;&amp; !ctorFieldSeat
 *           &amp;&amp; !condArmSeat) {
 *       return false;
 *   }
 * </pre>
 * Its SET-seat twin {@code FunctionExpressionRenderer.isHoistableThenChain} (:5439-5473) carries a
 * FOURTH disjunct the deep seat never got -- {@code && !CollectionHandler.isBareFunctionBody(t)},
 * i.e. facet {@code fnPathChainedBareFunctionThen} (PR #359, F-7): "a bare-FUNCTION body now ADMITS
 * on the FUNCTION path too -- its {@code @Inject} receiver field is path-agnostic". This law takes
 * that RULE, but not the SET seat's SCOPE: it adds an overload
 * {@code isHoistableThenChainLocal(then, admitBareFunctionOffRulePath)}, passes {@code true} from
 * exactly ONE of the predicate's 21 call sites -- {@code tryDeepThenHoist} (:2437) -- and leaves
 * the public 1-arg form (and therefore every other consumer) verdict-identical. A bare-RULE body
 * off the rule path keeps its decline everywhere (e1).
 *
 * <p><b>WHY the flag, and the LAW-69 framing CORRECTED.</b> The v1 draft of this law put the
 * disjunct straight into the shared predicate on a "two halves of one law that disagree" reading.
 * The LAW-75 probe REFUTED that framing at the level of the carrier: {@code [P32-C1SET]} measured
 * <b>28</b> rows where the SET seat admits off the rule path and this seat declines, and those 28
 * rows are <b>exactly the five GREEN functions plus {@code TotalNotionalQuantity}</b> --
 * {@code MapNonpublicExecutionReportToWorkflowStep} contributes ZERO SET-seat rows at all (its
 * {@code effectiveDate} chain is an ALIAS body, which never reaches the SET seat). <b>The
 * disagreement set IS the green half.</b> The SET seat is therefore the law's PRECEDENT, never its
 * oracle; the oracle is the in-file green sibling {@code eventDate} (next paragraph).
 *
 * <p><b>GOLDEN &lt;- FORK.</b> drr 7.0-7.3
 * {@code drr/ingest/fpml/recordkeeping/message/functions/MapNonpublicExecutionReportToWorkflowStep.java},
 * the {@code effectiveDate} alias -- ONE 3-line hunk, the file's ONLY hunk, so each of the four
 * cells goes WHOLE:
 * <pre>
 * GOLDEN  final MapperS&lt;ZonedDateTime&gt; thenArg = MapperS.of(tradingEventEffectiveDate(...).getOrDefault(...));
 *         return MapperS.of(mapZoneDateTimeToDate.evaluate(thenArg.get()));
 * FORK    return MapperS.of(tradingEventEffectiveDate(...).getOrDefault(...)).then(item -&gt; mapZoneDateTimeToDate.evaluate(item.get()));
 * </pre>
 * Source: {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
 * ingest-fpml-recordkeeping-message-func.rosetta:93-95} --
 * {@code alias effectiveDate: tradingEventEffectiveDate default fpmlTermination -> effectiveDate
 * then MapZoneDateTimeToDate}. {@code MapZoneDateTimeToDate} is a FUNCTION
 * ({@code cdm-6.21.0 ingest-fpml-confirmation-datetime-func.rosetta:216}), not a rule.
 *
 * <p><b>The discriminator is proven IN THE SAME FILE (a2's role, played by the corpus).</b> The
 * {@code eventDate} alias twelve lines up ({@code :77-88}) ends in the SAME bare function and
 * ALREADY hoists {@code thenArg0..thenArg7} byte-identically to golden -- it passes :4610 on the
 * {@code condArmSeat} disjunct alone (its chain IS the conditional's else branch, PR #399 admit
 * ii). So the hoist machinery, the decl wrap and the collapsed
 * {@code MapperS.of(fn.evaluate(thenArgN.get()))} consumer (CollectionHandler:4281-4295) are all
 * in place; only the plain-base FUNCTION-path ENTRY was missing. e2 is that shape as a fixture.
 *
 * <p><b>Green blast radius -- the measurement that forced the flag.</b> The shared predicate's
 * decline is NOT rare. {@code [P32-C1GATE]} printed <b>2,855</b> rows corpus-wide, both routes,
 * of which <b>48 read {@code declines=true}</b>, over SEVEN functions:
 * <pre>
 *   fn:MapNonpublicExecutionReportToWorkflowStep  20   BAND (the carriers)   chainParent=RShortcut
 *   fn:TotalNotionalQuantity                       4   BAND                  chainParent=ROperation
 *   fn:NotionalAmountLeg1                          5   GREEN (drr 6.3x)      chainParent=ROperation
 *   fn:NotionalAmountLeg2                          5   GREEN (drr 6.3x)      chainParent=ROperation
 *   fn:TotalNotionalQuantityLeg1                   5   GREEN (6.3x + 7.x)    chainParent=ROperation
 *   fn:TotalNotionalQuantityLeg2                   5   GREEN (6.3x + 7.x)    chainParent=ROperation
 *   fn:ValuationAmountFromValuation                4   GREEN (drr 7.x)       chainParent=ROperation
 * </pre>
 * Every declining row has {@code bareFn=true} (there is not one {@code bareFn=false declines=true}
 * row corpus-wide), so an UNSCOPED disjunct flips all 48 -- <b>24 rows on five GREEN files</b>.
 * Four of the five have zero {@code [P32-C1SEAT]} rows at all, i.e. their declines come from other
 * callers entirely, which a rendered-{@code .then(} scan is structurally blind to.
 * {@code corpus_control3} and {@code corpus_control4} pin all five byte-identical.
 *
 * <p><b>Why the flag narrows the flip to 8 rows.</b> {@code [P32-C1SEAT]} (the deep seat itself)
 * measured the only chains that reach {@code tryDeepThenHoist} with a bare-invokable body AND a
 * declining verdict:
 * <pre>
 *   fn:MapNonpublic...  n=1 baseKind=RDefaultExpr  hoistable=false bareInvokableBody=true   x8
 *   fn:MapNonpublic...  n=8 baseKind=REnumValueRef hoistable=true  bareInvokableBody=true   x4  (eventDate, green)
 *   fn:TotalNotionalQuantity      n=1  hoistable=true  bareInvokableBody=FALSE               x8
 *   fn:TotalNotionalQuantityLeg2  n=1  hoistable=true  bareInvokableBody=FALSE               x5
 *   (NotionalAmountLeg1, NotionalAmountLeg2, TotalNotionalQuantityLeg1,
 *    ValuationAmountFromValuation: NO deep-seat rows at all)
 * </pre>
 * {@code bareInvokableBody} there is {@code chainHasBareInvokableBody(expr)} -- a CHAIN-WIDE scan,
 * not just the outermost level -- and both non-carrier deep-seat functions print {@code n=1}, so
 * their single level is all there is. The gate's first conjunct {@code isBareInvokableBody(t)} is
 * false for them whatever the flag says. <b>The flag therefore moves the 8 MapNonpublic rows and
 * nothing else</b>; whole ceiling 4, unchanged.
 *
 * <p><b>The corpus-wide safety net, independent of all of that.</b> Every chain the admit reaches
 * is one that TODAY renders the inline {@code arg.then(lambda)}, and there is no
 * {@code Mapper.then(Function)} in rune-runtime -- so a green file cannot carry the form this
 * replaces. MEASURED at this head by a read-only walk over the cell's goldens: <b>0 of the 7,808
 * drr 7.0.0 goldens carry {@code .then(} in code</b> (identically 0 for 7.1.0 / 7.2.0 / 7.3.0).
 * {@code corpus_control0} re-derives that fact in-suite rather than trusting this sentence. The
 * whole {@code .then(}-bearing FORK population at the base head is 20 files (5 drr 5.61.0
 * {@code Notional*Rule} + CustomBasketCodeRule x3 -- all rule-path, where {@code onRulePath}
 * already suppresses the conjunct -- plus QuantityUnitOfMeasure x4 and TotalNotionalQuantity x4,
 * whose then-bodies are {@code only-element} / a hoisted local, not bare-invokable -- plus
 * MapNonpublic x4, the carriers). The ONLY bare-invokable then-body in that population is the bare
 * FUNCTION {@code mapZoneDateTimeToDate}.
 *
 * <p><b>Carriers.</b> MapNonpublicExecutionReportToWorkflowStep x drr 7.0.0 / 7.1.0 / 7.2.0 /
 * 7.3.0 FUNCTION -- class C015, the sole sig B041, 4 WHOLE files. corpus_c1/c2 lock two of the
 * four; the other two are the whole-matrix checkpoint's (LAW 80).
 *
 * <p><b>CLAIMED RED at the seat's base head</b> (measured by the chain, both routes): {@code a1},
 * {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+ {@code corpus_control2} on
 * {@code -Pir-on}). {@code e1}, {@code e2}, {@code corpus_control0}, {@code corpus_control3} and
 * {@code corpus_control4} are GREEN at RED and must STAY green -- they are the no-move locks.
 * <b>CLAIMED GREEN at the head</b>: 10/0F/1skip default, 10/0F/0skip {@code -Pir-on}.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-lawC1-*.log}):</b>
 * <ul>
 *   <li><b>m-lawC1-deepflag</b> ({@code isHoistableThenChainLocal(expr, true)} -&gt;
 *       {@code (expr, false)} at the deep seat -- the law severed at its only open caller):
 *       MEASURED <b>10/4F/1S</b> = {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 *       {@code corpus_control1} -- the claim exactly; {@code e1}, {@code e2},
 *       {@code corpus_control0/3/4} GREEN. The whole-cell control moves under this lane
 *       (LAW 76).</li>
 *   <li><b>m-lawC1-sharedwiden</b> (the 1-arg delegator's {@code false} -&gt; {@code true},
 *       i.e. the admit opened at every OTHER call site -- the v1 draft's shape): MEASURED
 *       <b>10/0F/1S -- EMPTY</b>. The adjudication this javadoc wrote in advance is therefore
 *       the record: the 24 green rows' predicate VERDICT flips (the probe's
 *       {@code [P32-C1GATE] declines=true} measurement) but their RENDERED BYTES do not --
 *       {@code corpus_control3} (drr 6.34.1, twelve whole-file compares) and
 *       {@code corpus_control4} (drr 7.0.0, three) stayed byte-identical under the v1 shape, and
 *       {@code corpus_control1}'s triple did not move. The single-caller flag is
 *       defence-in-depth at this corpus; its justification is the measured verdict flip plus the
 *       seat's rule that a shared predicate is not widened on a reading, NOT a byte move. It is
 *       still not a reason to widen the predicate.</li>
 *   <li><b>m-lawC1-rulescope</b> ({@code isBareFunctionBody} widened to
 *       {@code isBareInvokableBody} inside the guarded conjunct): MEASURED <b>10/1F/1S =
 *       {@code e1}</b> -- exactly the claim, the corpus half EMPTY for the measured reason
 *       (0 of the 2,855 {@code [P32-C1GATE]} rows read {@code bareFn=false onRulePath=false}).
 *       The widening IS witnessed, by the fixture built for it; the
 *       {@code isBareFunctionBody} choice stands.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on}); GREEN at the head
 * 10/0F/1skip default, 10/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW 77 -- the IR route.</b> The predicate has NO twin in {@code rune-ir-java}: a repo-wide
 * grep for {@code isHoistableThenChainLocal} across every module's main sources returns
 * {@code CollectionHandler.java} and {@code FunctionExpressionRenderer.java} and nothing else, and
 * {@code IRCollectionHandler} extends {@code CollectionHandler} with exactly ONE
 * {@code @Override}, {@code thenArgBaseName} (:65-90) -- so the ADMIT decision is inherited and
 * the route is safe by inheritance. The probe corroborates it end to end: <b>all twelve Scout-C
 * tag files are BYTE-IDENTICAL between the OFF and the ON route</b> (md5 per tag, both rounds
 * {@code Tests run: 275, Failures: 10}), i.e. the IR route takes the same decision at every one of
 * the twelve probed seats. Two notes for the reader: (i) every chain this
 * law newly admits passes through that override (called once per chain at
 * {@code CollectionHandler:2580}) and it THROWS {@code IllegalStateException} at :85 if the ANF
 * base ever disagrees with {@code StatementHoistSession.THEN_ARG} -- a real, low-probability
 * ON-route crash surface, which is why {@code corpus_control2} compares the IR route against
 * GOLDEN rather than against the legacy route; (ii) its {@code operandThenArgDrivenCount} (:57)
 * rises with this law, and its own javadoc says the count is "read by the FUNCTION byte gate,
 * asserted &gt; 0". <b>At this head that reader does not exist</b> -- a repo-wide grep for
 * {@code operandThenArgDriven} finds only the field, the increment and the getter -- so the rise
 * is gate-inert and byte-inert. (The stale javadoc sentence is named here, not edited: it is
 * outside this law's blast radius.)
 *
 * <p><b>LAW 74 (compile) -- MEASURED, not analytic.</b> {@code javac32-report.md} row C4, javac
 * 21.0.8 against the SHIPPED {@code rune-runtime/target/classes}: the fork's current text for this
 * carrier is <b>PRE 1 error</b> --
 * <pre>
 *   Seat32Pre.java:522: error: cannot find symbol
 *     ... .get())).then(item -&gt; mapZoneDateTimeToDate.evaluate(item.get()));
 *   symbol:   method then((item)-&gt;ma[...]et()))
 *   location: class MapperS&lt;ZonedDateTime&gt;
 * </pre>
 * and golden's text is <b>POST 0</b>. So this law is a compile REPAIR as well as a parity heal.
 * (The same report confirms the class at four independent receivers -- {@code MapperS<ZonedDateTime>},
 * {@code MapperC<FieldWithMetaNonNegativeQuantitySchedule>}, and the bare model interfaces
 * {@code NonNegativeQuantitySchedule} and {@code NonTransferableProduct} -- so "every fork file
 * carrying {@code .then(} is non-compiling regardless of the receiver" is measured, not argued.)
 *
 * <p><b>LAW 81 -- the cross-suite residue rows this heal MOVES</b> (each re-pinned in the SAME
 * commit, transcribed from its own measured print, never edited by arithmetic). All five are
 * drr 7.0.0 pins naming this carrier; the fork vector in each becomes the golden vector, so each
 * row is DELETED rather than re-valued, and each suite's {@code DOMAIN_DRR7} is UNMOVED (the file
 * keeps a non-zero golden tuple, so it stays inside the union domain):
 * <ul>
 *   <li>{@code AliasSigElementMetaSeatTest.KNOWN_RESIDUE_DRR7} -- row {@code fork=[0, 0, 8, 0]
 *       golden=[0, 0, 9, 0]} (DOMAIN_DRR7 1585 unmoved)</li>
 *   <li>{@code DefaultRightNestedThenHoistSeatTest.KNOWN_RESIDUE_DRR7} -- row
 *       {@code fork=[1, 10, 0] golden=[0, 10, 0]}</li>
 *   <li>{@code ThenArgDeclKindFromCompiledSeatTest.KNOWN_RESIDUE_DRR7} -- row
 *       {@code fork=[8, 0, 0, 0, 0] golden=[9, 0, 0, 0, 0]} (DOMAIN_DRR7 1592 unmoved)</li>
 *   <li>{@code WildcardLocalDeclSeatTest.KNOWN_RESIDUE_7} -- row {@code fork=[0, 8, 10]
 *       golden=[0, 9, 10]} (DOMAIN_DRR7 1862 unmoved)</li>
 *   <li>{@code InLambdaThenChainCtlAdmitSeatTest.KNOWN_THEN_CARRIERS_7} -- the MapNonpublic ENTRY
 *       (3 carriers -&gt; 2: QuantityUnitOfMeasure + TotalNotionalQuantity)</li>
 * </ul>
 * Two suites name this carrier and are NOT tripwires, stated so nobody re-derives them:
 * {@code DefaultCollapsingRightSeatTest} (its (collapsed arg, un-collapsed arg, nested wrap)
 * triple for this carrier is {@code [4, 0, 1]} on BOTH sides at the base head, so the file is not
 * in its residue and the heal does not move it -- verified against the dumps) and
 * {@code PathedChoiceSwitchSetSeatTest} (its only mention is a FIXTURE twin,
 * {@code func MapNpeToStep: <"MapNonpublicExecutionReportToWorkflowStep twin">} at :158 -- a
 * reduced Rune model, not a corpus pin). The committed band lists
 * ({@code rune-java-generator/src/test/resources/ladder-census/band-{off,on}.tsv}) carry
 * MapNonpublic rows for drr 7.0-7.3; they are SUPERSETS by design and their re-extraction is the
 * stage exit's job, not this law's.
 */
class DeepBareFunctionThenChainAdmitSeatTest {

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

    /** The carrier, at the same relative path in every drr 7.x cell. */
    private static final String MAP_NONPUBLIC =
            "drr/ingest/fpml/recordkeeping/message/functions/"
            + "MapNonpublicExecutionReportToWorkflowStep.java";

    /**
     * Cell A = drr 7.0.0 (the whole-cell control's cell); cell B = drr 7.3.0 (the far end of the
     * carrier family); <b>cell C = drr 6.34.1, the GREEN-PIN cell</b>.
     *
     * <p>Cell C exists for one reason: two of the five functions the shared predicate declines on
     * today -- {@code NotionalAmountLeg1} and {@code NotionalAmountLeg2} -- live ONLY in the drr
     * 6.3x family ({@code find -L test-corpus/drr -name 'NotionalAmountLeg1.java'} returns
     * {@code drr/standards/iosco/cde/version{1,2,3}/quantity/functions/} under 6.34.1 / 6.35.0 /
     * 6.36.0 / 6.37.0 / 6.38.0 and NOTHING under 7.x), so before this control they sat outside
     * every control this suite shipped. 6.34.1 is the D11 gate cell -- byte-clean by the standing
     * D11 20/20 -- which makes "these files are byte-identical to golden" a claim the harness
     * already stands behind, and its corpus parse is shared through
     * {@code D11CorpusRegressionTest}'s JVM-wide {@code CELL_CORPUS_CACHE}.
     */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
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

    static boolean bothCellsAvailable() {
        return cellAAvailable() && cellBAvailable();
    }

    static boolean cellsAndIrProviderAvailable() {
        return bothCellsAvailable() && irProviderOnClasspath();
    }

    /**
     * The GREEN PINS -- the files the LAW-75 probe measured as declining at the SHARED predicate
     * while being byte-clean today. If the law were widened in the predicate instead of scoped by
     * the caller flag, these are the files at risk; {@code m-lawC1-sharedwiden} is the lane that
     * proves the flag is load-bearing.
     *
     * <p>Paths derived by a read-only {@code find -L} over the cell's golden tree at this head, not
     * from the probe's {@code where=fn:<name>} (which carries no path). All twelve 6.34.1 entries
     * are pinned rather than only the version dir the chain happens to touch: the probe's
     * {@code where=} cannot tell version1 from version2 from version3, so the SUPERSET is the
     * honest scope.
     */
    private static final List<String> GREEN_PINS_DRR634 = List.of(
            "drr/standards/iosco/cde/version1/quantity/functions/NotionalAmountLeg1.java",
            "drr/standards/iosco/cde/version1/quantity/functions/NotionalAmountLeg2.java",
            "drr/standards/iosco/cde/version1/quantity/functions/TotalNotionalQuantityLeg1.java",
            "drr/standards/iosco/cde/version1/quantity/functions/TotalNotionalQuantityLeg2.java",
            "drr/standards/iosco/cde/version2/quantity/functions/NotionalAmountLeg1.java",
            "drr/standards/iosco/cde/version2/quantity/functions/NotionalAmountLeg2.java",
            "drr/standards/iosco/cde/version2/quantity/functions/TotalNotionalQuantityLeg1.java",
            "drr/standards/iosco/cde/version2/quantity/functions/TotalNotionalQuantityLeg2.java",
            "drr/standards/iosco/cde/version3/quantity/functions/NotionalAmountLeg1.java",
            "drr/standards/iosco/cde/version3/quantity/functions/NotionalAmountLeg2.java",
            "drr/standards/iosco/cde/version3/quantity/functions/TotalNotionalQuantityLeg1.java",
            "drr/standards/iosco/cde/version3/quantity/functions/TotalNotionalQuantityLeg2.java");

    /**
     * The drr 7.0.0 half of the green pins. {@code NotionalAmountLeg1/2} do not exist as FUNCTIONs
     * in 7.x at all (the 7.x tree restructured {@code TotalNotionalQuantityLeg1/2} to
     * {@code drr/regulation/common/trade/quantity/functions/} and dropped the NotionalAmountLeg
     * pair), which is exactly why cell C is needed. {@code ValuationAmountFromValuation} is the
     * clearest witness in the set: it is GREEN and its golden ALREADY hoists
     * {@code thenArg0}/{@code thenArg1}, i.e. the SET seat admits it while the shared predicate
     * declines it harmlessly elsewhere.
     */
    private static final List<String> GREEN_PINS_DRR700 = List.of(
            "drr/regulation/common/trade/quantity/functions/TotalNotionalQuantityLeg1.java",
            "drr/regulation/common/trade/quantity/functions/TotalNotionalQuantityLeg2.java",
            "drr/standards/iosco/cde/version1/valuation/functions/ValuationAmountFromValuation.java");

    // =========================================================================
    // Fixture -- reduced from ingest-fpml-recordkeeping-message-func.rosetta:73-95
    // =========================================================================

    /**
     * Three functions, one per vector, so every assertion is file-scoped.
     *
     * <ul>
     *   <li>{@code MapRoot32} -- the CARRIER shape: {@code alias a} (the
     *       {@code tradingEventEffectiveDate} twin, a {@code first} collapse) and {@code alias b}
     *       (the {@code effectiveDate} twin: {@code a default termAlias -> effDate then FmtFn} --
     *       a plain {@code default} base, a bare-FUNCTION last body, on the FUNCTION path).</li>
     *   <li>{@code MapRoot32Cond} -- the GREEN twin: {@code alias c} is the {@code eventDate}
     *       shape ({@code if ... then empty else (nav then default nav then FmtFn)}), which
     *       already hoists today through the {@code condArmSeat} disjunct and must not move.</li>
     *   <li>{@code MapRoot32Decline} -- the DECLINE lock: {@code alias e} is the same chain with a
     *       bare-RULE level {@code then Amt32} under the bare-FUNCTION one, so
     *       {@code aliasBodyMayHoist} still opens the sink (it admits on the RFunction reference)
     *       and the ONLY thing declining is the gate this law edits.</li>
     * </ul>
     * {@code FmtFn} is the {@code MapZoneDateTimeToDate} twin verbatim ({@code zonedDateTime ->
     * date}); {@code FmtNum} exists only so {@code MapRoot32Decline}'s inline form carries a
     * DIFFERENT receiver from {@code MapRoot32}'s, keeping the two vectors' tokens disjoint.
     */
    private static final String MODEL = """
            namespace census.seat32c1
            version "1.0.0"

            type Ev32:
                effDate zonedDateTime (0..1)

            type Term32:
                effDate zonedDateTime (0..1)
                agrDate zonedDateTime (0..1)

            type Leg32:
                amt number (0..1)

            type Root32:
                ev Ev32 (0..*)
                term Term32 (0..1)
                nov Term32 (0..1)
                leg Leg32 (0..1)
                leg2 Leg32 (0..1)

            func FmtFn: <"MapZoneDateTimeToDate twin - the BARE FUNCTION then-body">
                inputs:
                    zdt zonedDateTime (0..1)
                output:
                    d date (0..1)
                set d:
                    zdt -> date

            func FmtNum: <"a second bare FUNCTION - e1's OUTER body, so its inline form is distinguishable from a1's">
                inputs:
                    n number (0..1)
                output:
                    s string (0..1)
                set s:
                    n to-string

            reporting rule Amt32 from Leg32: <"a bare RULE then-body - the half of the #254 law this seat KEEPS declining off the rule path">
                extract amt

            func MapRoot32: <"a1 - the MapNonpublicExecutionReportToWorkflowStep effectiveDate shape">
                inputs:
                    rt Root32 (1..1)
                output:
                    out date (0..1)

                alias termAlias:
                    rt -> term

                alias a:
                    rt -> ev -> effDate first

                alias b:
                    a default termAlias -> effDate
                        then FmtFn

                set out:
                    b

            func MapRoot32Cond: <"e2 - the eventDate shape: the SAME bare-function last body admitted today by the condArmSeat disjunct">
                inputs:
                    rt Root32 (1..1)
                output:
                    outC date (0..1)

                alias termAlias:
                    rt -> term

                alias c:
                    if rt -> nov exists
                    then empty
                    else (rt -> term -> agrDate
                        then default termAlias -> effDate
                        then FmtFn)

                set outC:
                    c

            func MapRoot32Decline: <"e1 - the bare-RULE level: same seat, same base, RULE body - the decline this law KEEPS">
                inputs:
                    rt Root32 (1..1)
                output:
                    outS string (0..1)

                alias e:
                    rt -> leg default rt -> leg2
                        then Amt32
                        then FmtNum

                set outS:
                    e
            """;

    // =========================================================================
    // Part A -- the seat
    // =========================================================================

    /**
     * a1 -- THE LAW. A plain-base FUNCTION-path chain whose last body is a bare FUNCTION hoists
     * the golden {@code thenArg} decl + the collapsed consumer, and the non-compiling inline
     * {@code .then(} vanishes from the file.
     *
     * <p>THE SECOND-RUNG QUESTION IS CLOSED -- it was the v1 draft's one open question, and the
     * answer is "no second rung". {@code hunkbook32.md:794-806} shows the fork's own base text for
     * this alias is ALREADY {@code MapperS.of(tradingEventEffectiveDate(...).getOrDefault(
     * ....get()))}, byte-identical to golden's decl RHS up to the closing {@code .get()))}. The
     * {@code MapperS.of(} comes from the {@code default} operation's own renderer, not from the
     * level-wrap seat, and both wrap arms are correctly inert here:
     * {@code bareInvokableValueNeedsWrap} (:198-205) requires an {@code RSymbolReference} base and
     * the k==0 base is MEASURED {@code RDefaultExpr}, while the :4085-4096 collapse re-wrap
     * requires {@code endsWith(".get()")} AND {@code !startsWith("MapperS.of(")} -- the value ends
     * {@code .get()))} and already starts {@code MapperS.of(}. The golden decl falls out of the
     * hoist verbatim. Keep both asserts exactly as written; a failure here is a real defect, not a
     * missing rung.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_deepBareFunctionThenChainHoistsOnTheFunctionPath() throws IOException {
        String out = fixtureFunction("MapRoot32");
        assertContains(out, "final MapperS<ZonedDateTime> thenArg = MapperS.of(");
        assertContains(out, "return MapperS.of(fmtFn.evaluate(thenArg.get()));");
        assertTrue(!codeOnly(out).contains(".then("),
                "the non-compiling inline runtime `.then(` must be gone -- there is no"
                + " Mapper.then(Function):\n" + out);
    }

    // =========================================================================
    // Part B -- the decline locks (GREEN at RED, GREEN at the head)
    // =========================================================================

    /**
     * e1 -- the decline lock AND the {@code m-lawC1-rulescope} lane's only witness: a bare-RULE
     * level under the bare-FUNCTION one keeps the whole chain inline. The alias sink IS open here
     * ({@code aliasBodyMayHoist} admits on the {@code FmtNum} RFunction reference), so nothing but
     * the edited gate can be what declines -- which is exactly what makes this fixture move when
     * the gate is widened to {@code isBareInvokableBody}.
     *
     * <p>MEASURED, so the lane's emptiness at the corpus is an adjudication and not a gap: of the
     * <b>2,855</b> rows {@code [P32-C1GATE]} printed over the whole matrix on both routes, exactly
     * <b>0</b> read {@code bareFn=false onRulePath=false}. There is no corpus carrier for the
     * widening at all -- not a hidden one, not a rule-path one. This fixture is the only witness
     * that exists, which is why it may not be weakened.
     *
     * <p>PIN AT RED: the {@code Amt32Rule} assertion holds because facet
     * {@code injectRuleDepInFunctions} (PR #332) injects the {@code <Name>Rule} field on the
     * FUNCTION path too. If the fixture instead reports a generation error at RED, the rule
     * reference did not resolve -- reshape the fixture, do not drop the vector.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_bareRuleBodyOffTheRulePathKeepsTheDecline() throws IOException {
        String out = fixtureFunction("MapRoot32Decline");
        assertTrue(codeOnly(out).contains(".then("),
                "a bare-RULE level off the rule path must keep the inline form:\n" + out);
        assertTrue(!out.contains("thenArg"),
                "a bare-RULE level off the rule path must hoist NOTHING -- this is the"
                + " m-lawC1-rulescope witness:\n" + out);
        assertContains(out, "Amt32Rule");
    }

    /**
     * e2 -- the no-move lock on the shape that ALREADY works: the conditional-arm chain (the
     * {@code eventDate} twin) keeps its hoist ladder exactly as the {@code condArmSeat} disjunct
     * renders it today. The decl RHS is deliberately NOT pinned here -- {@code corpus_control1}'s
     * whole-cell scan owns the byte question; this vector owns "the ladder is still a ladder".
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_conditionalArmChainKeepsItsExistingHoist() throws IOException {
        String out = fixtureFunction("MapRoot32Cond");
        assertContains(out, "final MapperS<ZonedDateTime> thenArg0 = ");
        assertContains(out, "final MapperS<ZonedDateTime> thenArg1 = ");
        assertContains(out, "return MapperS.of(fmtFn.evaluate(thenArg1.get()));");
        assertTrue(!codeOnly(out).contains(".then("),
                "the conditional-arm chain already hoists -- it must not acquire an inline"
                + " `.then(`:\n" + out);
    }

    // =========================================================================
    // Part C -- the whole-file locks
    // =========================================================================

    /** corpus_c1 -- the drr 7.0.0 whole-file heal (3 lines, 1 hunk, sig B041). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700MapNonpublicMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(MAP_NONPUBLIC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + MAP_NONPUBLIC + ": " + own);
        String gen = drrAOutput.get(MAP_NONPUBLIC);
        assertNotNull(gen, "not generated: " + MAP_NONPUBLIC);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(MAP_NONPUBLIC))), normalize(gen),
                "MapNonpublicExecutionReportToWorkflowStep must byte-match golden - seat 32 law"
                + " C.1: the deep then-hoist gate admits a bare-FUNCTION body on the FUNCTION"
                + " path, as its SET-seat twin has since PR #359");
    }

    /** corpus_c2 -- the far end of the carrier family: the same heal in drr 7.3.0. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730MapNonpublicMatchesGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        List<String> own = drrBGenErrors.stream().filter(e -> e.contains(MAP_NONPUBLIC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + MAP_NONPUBLIC + ": " + own);
        String gen = drrBOutput.get(MAP_NONPUBLIC);
        assertNotNull(gen, "not generated: " + MAP_NONPUBLIC);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(MAP_NONPUBLIC))), normalize(gen),
                "MapNonpublicExecutionReportToWorkflowStep must byte-match golden in drr 7.3.0 too"
                + " - seat 32 law C.1");
    }

    // =========================================================================
    // Part D -- the controls
    // =========================================================================

    /**
     * control0 -- THE GOLDEN-DOMAIN PIN, re-derived in-suite (LAW 75: verify over the WHOLE
     * matrix, never trust a sentence). Golden is the oracle in both directions:
     * <ul>
     *   <li>NO golden in the whole drr 7.0.0 tree carries {@code .then(} in code. The pin is
     *       {@code GOLDEN_THEN_BEARING = 0}, derived by a read-only walk over
     *       {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java} (7,808 {@code
     *       .java} files; the identical walk returns 0 for drr 7.1.0 / 7.2.0 / 7.3.0). This is
     *       the whole green-safety argument as an assertion: the form this law removes cannot
     *       appear in any correct output.</li>
     *   <li>The carrier's golden DOES carry both lines the law must produce.</li>
     * </ul>
     * The scan is code-only, so a {@code .then(} inside a string literal or a comment can never
     * make this pass or fail by accident.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesNoRuntimeThenAnywhereInTheCell() throws IOException {
        final int GOLDEN_THEN_BEARING = 0;
        Map<String, String> golden = goldenTreeA();
        assertTrue(golden.size() > 7000,
                "the golden walk must see the whole cell, saw " + golden.size() + " files");
        List<String> carriers = new ArrayList<>();
        for (Map.Entry<String, String> e : golden.entrySet()) {
            if (e.getValue().contains(".then(")
                    && collapse(codeOnly(e.getValue())).contains(".then(")) {
                carriers.add(e.getKey());
            }
        }
        assertEquals(GOLDEN_THEN_BEARING, carriers.size(),
                "goldens must carry ZERO runtime `.then(` - the green-safety argument of this"
                + " law. Offenders: " + carriers);
        String carrier = golden.get(MAP_NONPUBLIC);
        assertNotNull(carrier, "golden missing: " + MAP_NONPUBLIC);
        assertTrue(collapse(carrier).contains(
                        "final MapperS<ZonedDateTime> thenArg = MapperS.of("),
                "golden must hoist the effectiveDate thenArg decl");
        assertTrue(collapse(carrier).contains(
                        "return MapperS.of(mapZoneDateTimeToDate.evaluate(thenArg.get()));"),
                "golden must consume the hoisted local through the collapsed bare-function form");
    }

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. Per emitted file the triple
     * <ul>
     *   <li><b>T1</b> -- {@code .then(}: the shape the law REMOVES. Golden-side 0 everywhere
     *       (control0), so any fork-side T1 is a defect and a fork-side RISE is an over-fire.</li>
     *   <li><b>T2</b> -- {@code thenArg}: the shape the law ADDS.</li>
     *   <li><b>T3</b> -- {@code final Mapper}: the OVER-FIRE NET. Deliberately global: the green
     *       failure mode of a hoist admit is a decl appearing (or vanishing) at a seat this law
     *       does not name, and that shows up as a hoisted-declaration count change anywhere in
     *       the cell.</li>
     * </ul>
     * must equal golden's, file for file over the union, beyond the NAMED residue. The residue is
     * MEASURED, not drafted: transcribed from the seat-31 final OFF dump for all 18 drr 7.0.0
     * band files (the other 13 have identical triples on both sides, so they are not rows). At
     * the BASE head the carrier is a fifth row -- {@code fork=[1, 16, 8] golden=[0, 18, 9]} -- so
     * this control FAILS at RED and passes only when the file goes whole. It also fails if the
     * law under-fires (the row survives) or over-fires (a sixth row appears).
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertTrue(drrAOutput.containsKey(MAP_NONPUBLIC),
                "the carrier must be INSIDE this scan's domain, else control1 proves nothing");
        assertUnionEqual(scan(drrAOutput), scan(goldenTreeA()), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, DOMAIN_DRR700);
    }

    /**
     * control2 -- LAW 77, written as BOTH-ROUTES-vs-GOLDEN (a route-PARITY control is not enough:
     * two routes can agree by both being wrong). Every chain this law newly admits passes through
     * {@code IRCollectionHandler.thenArgBaseName}, whose :85 self-guard throws on a base
     * mismatch, so the ON-route render of the carrier is compared against GOLDEN in both cells.
     */
    @Test
    @EnabledIf("cellsAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForBothCarriers() throws IOException {
        Map<String, String> irA = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(MAP_NONPUBLIC))),
                normalize(irA.get(MAP_NONPUBLIC)), "IR route vs GOLDEN (drr 7.0.0): "
                + MAP_NONPUBLIC);
        Map<String, String> irB = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(MAP_NONPUBLIC))),
                normalize(irB.get(MAP_NONPUBLIC)), "IR route vs GOLDEN (drr 7.3.0): "
                + MAP_NONPUBLIC);
    }

    /**
     * control3 -- THE GREEN PIN THE v1 DRAFT DID NOT HAVE (LAW 76). Whole-file byte compares over
     * the drr 6.34.1 cell for the four function basenames the probe measured declining at the
     * SHARED predicate while green. {@code NotionalAmountLeg1} and {@code NotionalAmountLeg2} exist
     * NOWHERE in drr 7.x, so before this test they sat outside every control this suite shipped and
     * a widened predicate could have moved them silently.
     *
     * <p>This test is GREEN at RED, GREEN at the law head, and is the failing set of
     * {@code m-lawC1-sharedwiden} -- the lane that opens the admit at every other call site. It is
     * the reason the law is a caller flag and not a fourth disjunct.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_control3_drr634NamedGreenFunctionsStayByteIdentical() throws IOException {
        assertNotNull(drrCOutput, "drr 6.34.1 generation did not run");
        assertEquals(List.of(), drrCGenErrors,
                "drr 6.34.1 reported a generation error - the pin is incomplete");
        for (String rel : GREEN_PINS_DRR634) {
            String gen = drrCOutput.get(rel);
            assertNotNull(gen, "the green pin must be INSIDE this cell's emitted set, else it"
                    + " proves nothing: " + rel);
            assertEquals(normalize(Files.readString(GOLDEN_C.resolve(rel))), normalize(gen),
                    "GREEN PIN moved (drr 6.34.1) - seat 32 law C.1 must not reach this file: "
                    + rel);
        }
    }

    /**
     * control4 -- the drr 7.0.0 half of the green pin. Same role as control3 for the three named
     * functions that DO live in 7.x. {@code corpus_control1}'s union scan already covers this cell
     * with a (T1, T2, T3) triple, but a triple can only see a move that changes one of three
     * tokens; a whole-file compare sees any move at all, which is what a green pin owes.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control4_drr700NamedGreenFunctionsStayByteIdentical() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        for (String rel : GREEN_PINS_DRR700) {
            String gen = drrAOutput.get(rel);
            assertNotNull(gen, "the green pin must be INSIDE this cell's emitted set, else it"
                    + " proves nothing: " + rel);
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(rel))), normalize(gen),
                    "GREEN PIN moved (drr 7.0.0) - seat 32 law C.1 must not reach this file: "
                    + rel);
        }
    }

    // =========================================================================
    // The measured residue + domain (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * MEASURED from the seat-31 final OFF dump at the seat's base head, over all 18 drr 7.0.0
     * band files (13 FUNCTION + 5 POJO-kind {@code *Rule}); the committed band lists are
     * SUPERSETS and were not consulted. Every row is a PRE-EXISTING band file this law does not
     * own, and every row is a LAW-81 tripwire: it fires when its owning law lands, and the re-pin
     * is transcribed from THAT run's print, in that law's commit.
     * <ul>
     *   <li>{@code IndicatorOfTheUnderlyingIndexRule} -- one missing hoisted decl (T3).</li>
     *   <li>{@code Price} -- the 70-line/6-hunk F-family residue (11 golden {@code thenArg}
     *       mentions the fork does not render).</li>
     *   <li>{@code QuantityUnitOfMeasure} -- F7 co-resident with F13/F22; its {@code .then(}
     *       bodies are {@code only-element} / an {@code exists} filter, NOT bare-invokable, so
     *       this law cannot reach them (S32's F22 owner does).</li>
     *   <li>{@code TotalNotionalQuantity} -- F7 crossed with F8/F12; its {@code .then(} receiver is a
     *       HOISTED LOCAL ({@code ifThenElseResult}), a shape no predicate here produces.</li>
     * </ul>
     * The list is in {@code TreeSet} key order, which is the order {@code assertUnionEqual}
     * builds {@code mismatched} in.
     */
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of(
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[0, 4, 2] golden=[0, 4, 3]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on D.1) healed it WHOLE after this suite's pins were measured;
            // transcribed from the checkpoint-2 full-gensuite print (ckpt2-gensuite.log).
            // the Price row (fork=[0, 2, 1] golden=[0, 13, 1]) LEFT this list: law C.1 (iteChainNestedThenLadderAdmit + R2a/R2b/R4)
            // rendered the seven-rung ladder as statements and took this scan's token set to golden's in
            // all four drr 7.x cells - the file stays BANDED on C.2's default join; transcribed from this
            // control's own print (C1-trip1.log), a pure row removal (was == expected minus it).
            // the QuantityUnitOfMeasure row (fork=[3, 14, 7] golden=[0, 20, 10]) LEFT this list: law B.1 (setSeatNestedValueThenTogetherHoist,
            // the k>0 restructure window) healed this scan's token set to golden's in all four drr 7.x cells -
            // the file itself stays BANDED (its close is B.3 + B.24); transcribed from this control's own print
            // (B1-trip1.log), a pure row removal (was == expected minus it).
            // the TotalNotionalQuantity row (fork=[3, 4, 2] golden=[0, 18, 5]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /**
     * The union-domain pin, DERIVED by a read-only walk over the cell's goldens (the same walk
     * control0 runs): of the 7,808 drr 7.0.0 golden {@code .java} files, <b>1,694</b> carry at
     * least one of the three tokens AND sit under {@code /functions/} or {@code /reports/} --
     * i.e. inside what {@code generateCell} emits (rules + reports + functions; drr 7.x's declared
     * generation scope is {@code drr.* + cdm.ingest.* + com.rosetta.model}, and all 1,694 are
     * inside it). The 99 token-bearing goldens outside that set are 98 {@code validation/datarule}
     * classes and one {@code DeepPathUtil}, which no generator in this harness emits, so
     * {@code retainAll(emittedA)} drops them. Nothing on the fork side adds a file: at this head
     * drr 7.0.0's only fork/golden divergences are the 18 band files and every one of them has a
     * non-zero golden tuple.
     *
     * <p>PRINT-FIRST (LAW 73): if the sentinel prints a different {@code MEASURED DOMAIN}, pin THAT
     * number from its own print and record why it moved -- do not re-derive it by arithmetic.
     */
    private static final int DOMAIN_DRR700 = 1694;

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String flat = collapse(codeOnly(e.getValue()));
            int t1 = count(flat, ".then(");
            int t2 = count(flat, "thenArg");
            int t3 = count(flat, "final Mapper");
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
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

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        // The print-first domain-pin sentinel (seat 30): a negative pin FAILS here and PRINTS its
        // measured value in the assert's own message - transcribe the pin FROM this print.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means an"
                + " unpinned call site; MEASURED DOMAIN = " + universe.size()
                + " token-bearing files");
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
                "the union domain must equal the emitted token-bearing files ("
                + expectedDomain + ")");
    }

    // =========================================================================
    // Fixture harness (the ChoiceOptionLadderDeepHopSeatTest renderer + a rule generator)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat32c1.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat32c1".equals(m.namespace()));
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

    private static String fixtureFunction(String fnName) throws IOException {
        Render r = render();
        String path = fnName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return out;
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
            throw new AssertionError("[DeepBareFunctionThenChainAdmitSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the seat-31 cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> drrCOutput;
    private static List<String> drrCGenErrors;
    private static Map<String, String> goldenA;

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
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_C_ROOT), errs);
            drrCGenErrors = errs;
        }
    }

    /** The golden tree, read once (control0 and control1 both scan it). */
    private static Map<String, String> goldenTreeA() throws IOException {
        if (goldenA == null) {
            goldenA = readGoldenTree(GOLDEN_A);
        }
        return goldenA;
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
                    "the IR provider must be resolvable under -Pir-on, else this is not an"
                    + " ON-route render");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got "
                    + funcGen.getClass());
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

    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean inWs = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                inWs = true;
                continue;
            }
            if (inWs && sb.length() > 0) {
                sb.append(' ');
            }
            inWs = false;
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Comments and string literals stripped. NOTE the argument order everywhere in this suite is
     * {@code collapse(codeOnly(raw))} and never the reverse: {@code codeOnly} ends a {@code //}
     * comment at the next {@code \n}, and collapsing first removes every newline -- which would
     * make the {@code // RosettaFunction dependencies} header every generated FUNCTION class
     * carries swallow the entire rest of the file.
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

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
