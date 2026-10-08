// SEAT 30 LAW 8 — facet ctorSetterValueWrapperRecovery (apply script: drafts30/law8s-apply.py).
// SHIPPED.
// Every PIN marked  ///PIN:  carries its MEASURED value, transcribed from the chain's own
// print, EXCEPT the one marked OUTSTANDING on a2 (a fixture that does not exist yet).
// The chain that measured them: chain-all30.ps1 @ e223ce19; logs
// f30-{red,green}-{default,on}.log, f30-mut-m-law8.log, f30-mut-m-law8-halves.log.
// f30-mut-m-law8-typed.log is a BUILD FAILURE, not a result — see the class javadoc.
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
 * SEAT 30, law 8 — facet {@code ctorSetterValueWrapperRecovery}: <b>a ctor value whose compiled
 * type is ERASED but whose AST provably carries the attribute's meta wrapper keeps the PLAIN
 * setter.</b>
 *
 * <p><b>The three proofs, one of them missing (LAW 69).</b> {@code ConstructionHandler
 * .ctorSetterName} decides {@code set<Name>} vs {@code set<Name>Value} from two value-side
 * proofs:
 * <pre>
 * valueCarriesAttributeMeta(pair.value(), compiler)      // the AST proof (B1/B3/#348/#384/#392)
 * || compiledValueIsMetaWrapped(compiledValue, compiler) // the RENDER-TRUTH proof (B2)
 * </pre>
 * The render-truth proof returns {@code false} the instant
 * {@code compiled.getExpressionType() == null} — the shape of every list-op collapse, bare
 * {@code item.get()} off a {@code Mapper*<FieldWithMetaX>} lambda, and deep-feature terminal —
 * and the AST proof has no arm for those shapes either. Meanwhile
 * {@code NavigationHandler.recoverExprMetaWrapper}, the walker the AST proof's own arms already
 * reach transitively, DOES resolve the wrapper on exactly those expressions. One question,
 * three proofs, one of them not consulted here.
 *
 * <p><b>Golden vs fork</b> — drr 7.0.0–7.3.0 {@code drr/base/trade/underlier/functions/
 * UnderlierProductIdentifier.java}, the {@code floatingRateIndex} setter seat:
 * <pre>
 * golden  .setIdentifier(floatingRateIndex(product, identifierType)
 *             .&lt;FieldWithMetaString&gt;map("getName", _f -&gt; _f.getName()).get())
 * fork    .setIdentifierValue(floatingRateIndex(product, identifierType)
 *             .&lt;FieldWithMetaString&gt;map("getName", _f -&gt; _f.getName()).get())
 * </pre>
 * The VALUE expression is byte-identical on both sides; only the setter NAME differs — and the
 * fork's name is the one that takes a bare {@code String} while the value IS a
 * {@code FieldWithMetaString}. ({@code AssetIdentifier.identifier} IS
 * {@code [metadata scheme]}-annotated, so the annotation loop fires and the meta-free
 * fall-through is never reached — the scout's original "the value is never consulted" diagnosis
 * was refuted by the model in {@code f13-shapeF-notes.md} §0.)
 *
 * <p><b>The probe verdict this law answers (LAW 75) — the round's only UNIQUE confirmation.</b>
 * Of <b>1,147</b> {@code [PROBE30-SETTER] site=attrMetaValue} rows corpus-wide, exactly
 * <b>FOUR</b> match the CONFIRMS cell, and all four are this carrier — one per drr 7.x cell:
 * <pre>
 * [PROBE30-SETTER] site=attrMetaValue where=fn:UnderlierProductIdentifier attr=identifier
 *   attrMetaKind=FIELD_WITH_META attrKeyed=true valueKind=RListOpExpr valueType=- valueItem=-
 *   valueWrapper=false cfRecover=FieldWithMetaString condListFired=false
 *   chosen=setIdentifierValue mismatch=false
 * </pre>
 * {@code valueWrapper=false} is the render-truth proof declining; {@code cfRecover=
 * FieldWithMetaString} is the recovery the law adds. <b>The measured blast radius at this seat
 * is 4 rows.</b>
 *
 * <p><b>THE SECOND HALF — not optional (LAW 69).</b>
 * {@code hoistSingleValueIntoMultiOrNull} mirrors this decision for the hoisted local's TYPE,
 * and says so in its own comment: <i>"Mirror ctorSetterName: the local TYPE must match the
 * SETTER chosen … Otherwise the singletonList element type would mismatch the setter's List
 * element type (non-compiling)."</i> Widening one half and not the other is the divergence LAW
 * 69 forbids, so the disjunct lands at BOTH sites in the same commit.
 * <b>Byte-neutrality of the second half is a CLAIM, not an assumption</b>: the carrier's
 * {@code identifier} is {@code (1..1)} and never reaches the single-into-multi hoist, and the
 * probe instrumented {@code ctorSetterName}, not the hoist. {@code corpus_control4} scans the
 * hoist population explicitly, and the mid-seat whole-matrix checkpoint must run after this
 * law. <b>If a file enters at the hoist seat, NARROW THE SHARED PREDICATE
 * ({@code m-law8-typed}); do not split the halves apart again.</b>
 *
 * <p><b>Over-fire, and why no value-type conjunct ships.</b> Neither existing disjunct carries
 * one, and adding one only here would make the three proofs disagree about the same expression.
 * The named GREEN pins the existing {@code ctorSetterName} javadoc already calls out — cdm6
 * {@code MapAdjustedDateToAdjustableDate} / {@code MapDateToAdjustableDate}, which legitimately
 * carry {@code .set*Value(} for meta-FREE values — recover {@code null} and stay on the
 * {@code Value} path; they are in {@code corpus_control1}'s domain BY NAME.
 *
 * <p><b>⚠ LAW 80 — UnderlierProductIdentifier does NOT go whole this seat.</b> The file is
 * {@code F9 + F13}, {@code lines=30 hunks=5}, and law 8 owns exactly ONE hunk (1 of the 15
 * changed lines). The other four hunk groups are:
 * <ul>
 *   <li>the {@code final MapperC<? extends ReferenceObligation> thenArg0;} ite-hoist decl
 *       wildcard — family <b>F9</b>, seat {@code appendIteHoistChainCore}, <b>no owning law in
 *       seat 30</b>;</li>
 *   <li>eight {@code .<Observable>map("Type coercion", …)} hops inside a
 *       {@code mapSingleToList} lambda and two more inside a {@code mapSingleToItem} lambda —
 *       the <b>F1 implicit-receiver</b> defect, REFUTED as drafted
 *       ({@code blindSpot=true} in 0 rows corpus-wide), <b>no owning law in seat 30</b>.</li>
 * </ul>
 * So {@code corpus_c1} here is a LINE lock on the setter seat, with the F9 and F1 residue
 * NAMED. Do not promote it to a whole-file lock this seat.
 *
 * <p><b>RED at the pre-law head — MEASURED at {@code 6c8e1544}, BOTH routes</b>
 * ({@code f30-red-default.log}, {@code f30-red-on.log}): <b>12 run / 5F / 0E / 1 skip</b>
 * (default) and <b>12 / 5F / 0E / 0 skip</b> ({@code -Pir-on}) — the SAME five both routes:
 * <b>a1, a2, corpus_c1, corpus_control1 and corpus_control4</b> ({@code control1} at
 * {@code 2 file(s)}, {@code control4} at {@code 5 file(s)}). <b>Drafted a1, a2, corpus_c1,
 * corpus_control2; measured that set with corpus_control1 AND corpus_control4 in place of
 * corpus_control2 — the difference explained:</b> {@code corpus_control2} does not fail at RED
 * even on {@code -Pir-on} where it runs (RED-on is 5F, not 6F); the two whole-cell controls,
 * neither claimed, are what carry this law's corpus evidence at the RED base. GREEN at the seat
 * head is <b>12/0F</b> on both routes: no standing failure, so the lane sets below are exact.
 *
 * <p><b>⚠ AND corpus_control4 is NOT law 8's — measured.</b> {@code corpus_control4} fails at
 * the RED base but does NOT fail under {@code m-law8}, the whole-law revert. Since the RED
 * checkout reverts the WHOLE generator (all ten laws) while the lane reverts only this one, the
 * 5-file movement at control4 belongs to a DIFFERENT seat-30 law, not to this one. Recorded here
 * so nobody reads control4's RED failure as evidence for law 8: <b>law 8's measured corpus reach
 * is {@code corpus_control1} alone.</b>
 *
 * <p><b>MUTATION LANES (LAW 82). One lane MEASURED, one MEASURED EMPTY, one still a CLAIM —
 * stated apart. Sets transcribed from the archived logs {@code f30-mut-&lt;lane&gt;.log} at
 * {@code e223ce19}.</b>
 * <b>⚠ THE LANE LOOP RUNS THE DEFAULT PROFILE ONLY.</b> {@code corpus_control2} is
 * {@code @EnabledIf}-gated on the IR route and is the ONE skip in every lane's
 * {@code …/1 skipped} run, so it CANNOT fail in a lane by construction; it is UNMEASURED in
 * every lane below.
 * <ul>
 *   <li><b>m-law8</b> ({@code f30-mut-m-law8.log}) = {@code law8s-apply.py --revert}.
 *       <b>MEASURED 12/4F/0E/1S: a1, a2, corpus_c1 and corpus_control1</b> (control1 at
 *       {@code 2 file(s)}, naming {@code UnderlierProductIdentifier.java fork=[1, 13, 14]}
 *       alongside the pinned {@code Enrich_TransactionReportInstructionTestPackDefault} row).
 *       <b>Drafted a1, a2, corpus_c1 (+control2 on {@code -Pir-on}); measured that set PLUS
 *       corpus_control1 — the difference explained:</b> the draft named only the unmeasurable
 *       IR-route control and under-named the whole-cell one. Reverting law 8 puts the carrier's
 *       setter row back into the differing set, which is the control's second file. Both
 *       fixture pins (a1, a2) move with the law, so this suite's failing-first evidence is
 *       intact at BOTH grains — the only one of the seat's ten suites for which that is true
 *       without qualification.</li>
 *   <li><b>m-law8-halves</b> ({@code f30-mut-m-law8-halves.log}) = {@code --mut-halves} — the
 *       disjunct added to {@code ctorSetterName} but NOT to the local-type mirror.
 *       <b>MEASURED 12/0F/0E/1S — EMPTY.</b> <b>Drafted a2 + corpus_control4; measured EMPTY,
 *       and the draft PRE-AUTHORISED exactly this reading, so it is an ADJUDICATION rather than
 *       a miss:</b> <i>"a measured EMPTY means no corpus file exercises the hoist path with a
 *       recoverable erased value — which is the byte-neutrality claim for the second half,
 *       ADJUDICATED rather than assumed"</i>. Recorded as promised. <b>What it means:</b> the
 *       two halves cannot be observed to disagree at 25 cells or in this fixture set, so the
 *       second half ships on LAW 69 (the two halves of one decision must not diverge) rather
 *       than on a measured carrier — and the class's own instruction stands: if a file ever
 *       enters at the hoist seat, NARROW the shared predicate, do not split the halves apart
 *       again. Note this also leaves {@code a2} un-witnessed by this lane; a2's witness is the
 *       RED base and {@code m-law8}, where it does fire.</li>
 *   <li><b>m-law8-typed</b> ({@code f30-mut-m-law8-typed.log}) = {@code --mut-typed} — the
 *       recovered wrapper must wrap the ATTRIBUTE's own translated item type.
 *       <b>NOT MEASURED: the mutation did not COMPILE, so not one test ran.</b> The log ends in
 *       {@code ConstructionHandler.java:[2684,44] cannot find symbol  symbol: method
 *       getCanonicalName()  location: interface com.rosetta.util.types.JavaReferenceType} →
 *       {@code BUILD FAILURE}. The chain recorded {@code EXIT=1}, which reads like a test failure
 *       in the status file and is NOT one. <b>This lane was a DESIGNED-ZERO lane, which makes the
 *       distinction load-bearing: a non-compiling mutation is NOT a measured zero and cannot
 *       adjudicate the attribute-value-type conjunct.</b> The severance has since been narrowed
 *       via the sibling's own {@code instanceof JavaClass} pattern ({@code JavaClass.equals}
 *       deliberately NOT substituted — it is strictly stricter than the name compare the conjunct
 *       expresses). <b>MEASURED at the narrowed lane's standalone run (post-re-pin head
 *       {@code a5b3d8bd}): 12/0F/0E/1S — EMPTY, the DESIGNED zero properly ADJUDICATED</b>
 *       (the m-law5ii precedent: the mutation compiles, every test stays green). The
 *       adjudication's meaning, as pre-authorised: the attribute-value-type conjunct buys
 *       nothing at this corpus — the carrier's recovered {@code FieldWithMetaString} already
 *       wraps {@code identifier}'s {@code string} — and the simpler, sibling-consistent form
 *       is right. The corpus controls own the class beyond the fixtures.</li>
 * </ul>
 * <b>Lane tally for this suite: 0 MATCH · 1 MISMATCH-corrected · 1 MEASURED-EMPTY (pre-authorised,
 * ADJUDICATED) · 1 UNMEASURED (compile failure, since fixed, not re-run).</b>
 */
class CtorSetterValueWrapperRecoverySeatTest {

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

    /** Cell A = drr 7.0.0 — the carrier cell (7.1/7.2/7.3 carry the identical row). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = cdm 6.20.6 — the over-fire control cell, chosen BY NAME: it is where the
     * {@code ctorSetterName} javadoc's two GREEN {@code .set*Value(} pins live
     * ({@code MapAdjustedDateToAdjustableDate}, {@code MapDateToAdjustableDate}) and where the
     * ctor-heavy ingest-fpml {@code Map*} family lives. This is the highest over-fire risk
     * surface in SHAPE F.
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
    // The fixture — the erased-but-recoverable ctor value and its decline twins
    // =========================================================================

    /**
     * The carrier's shape: a ctor keyed on a {@code [metadata scheme]} attribute whose VALUE is
     * a navigation into a meta leaf, COLLAPSED by a list-op so the compiled expression type is
     * erased ({@code valueKind=RListOpExpr valueType=-}).
     *
     * <p><b>The decline twins</b> — each escapes exactly one input:
     * <ul>
     *   <li>{@code B1BareErasedValue} — the SAME erased shape over a meta-FREE leaf: nothing
     *       recovers, so the {@code Value} form must survive. WITNESS-UNIQUE on
     *       {@code .setCodeValue(}, the token the flip removes at a1.</li>
     *   <li>{@code B2MetaFreeAttribute} — a meta-FREE ATTRIBUTE: the annotation loop never
     *       fires, so this law's disjunct is unreachable and the plain setter is chosen by the
     *       pre-existing fall-through. Pins that the fall-through was NOT moved (the F2a rung
     *       the notes drafted and the probe left green-only).</li>
     *   <li>{@code B3TypedValueKeepsRenderTruth} — a value whose compiled type IS present and
     *       is the wrapper: the render-truth proof already answers, so the law changes nothing.
     *       Pins that the new disjunct is strictly a THIRD rung.</li>
     * </ul>
     */
    private static final String MODEL = """
            namespace census.seat30law8
            version "1.0.0"

            type Named:
                code string (0..1)
                    [metadata scheme]
                plain string (0..1)

            type Holder:
                names Named (0..*)
                bare Named (0..*)

            type Target:
                code string (0..1)
                    [metadata scheme]
                plain string (0..1)

            func A1ErasedMetaValueKeepsPlainSetter: <"a1 - THE CARRIER: an erased list-op collapse over a meta leaf">
                inputs:
                    h Holder (1..1)
                output:
                    t Target (0..1)
                set t:
                    Target {
                        code: h -> names -> code only-element,
                        ...
                    }

            func B1BareErasedValue: <"b1 - DECLINE: the same erased shape over a meta-FREE leaf">
                inputs:
                    h Holder (1..1)
                output:
                    t Target (0..1)
                set t:
                    Target {
                        code: h -> names -> plain only-element,
                        ...
                    }

            func B2MetaFreeAttribute: <"b2 - DECLINE: a meta-FREE attribute never enters the annotation loop">
                inputs:
                    h Holder (1..1)
                output:
                    t Target (0..1)
                set t:
                    Target {
                        plain: h -> names -> plain only-element,
                        ...
                    }

            func B3TypedValueKeepsRenderTruth: <"b3 - the render-truth proof already answers; the third rung is inert">
                inputs:
                    h Holder (1..1)
                output:
                    t Target (0..1)
                set t:
                    Target {
                        code: h -> names first -> code,
                        ...
                    }
            """;

    // =========================================================================
    // Part A — the failing-first pins (RED pre-law)
    // =========================================================================

    /**
     * a1 — the setter NAME. An erased ctor value whose AST provably carries the attribute's
     * meta keeps the PLAIN setter. Fails pre-law: the fork fell to the {@code Value} form.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_erasedMetaCtorValueKeepsThePlainSetter() throws IOException {
        String out = fn("A1ErasedMetaValueKeepsPlainSetter.java");
        String code = normalize(out);
        assertTrue(code.contains(".setCode("),
                "an erased-but-recoverable meta value must take the PLAIN setter:\n" + out);
        assertTrue(!code.contains(".setCodeValue("),
                "the Value form must be gone:\n" + out);
    }

    /**
     * a2 — the LAW-69 pin: the SETTER chosen and the hoisted local's TYPE must agree. Rendered
     * on the SAME expression through {@code hoistSingleValueIntoMultiOrNull}'s mirror, so a
     * split between the two halves shows up here as a wrapper/bare mismatch.
     *
     * <p><b>MEASURED CORRECTION (LAW 82).</b> The draft said this "fires under
     * {@code m-law8-halves}". <b>It does not:</b> that lane measured 12/0F — EMPTY
     * ({@code f30-mut-m-law8-halves.log}), a2 among the greens. Splitting the two halves apart
     * cannot be observed here, which is the byte-neutrality adjudication recorded in the class
     * javadoc. a2 IS a genuine failing-first pin for the law as a whole — it fails at the RED
     * base on both routes and under {@code m-law8} — but it is <b>not</b> the LAW-69 half-split
     * witness the draft claimed, and this fixture cannot be one while it stays the placeholder
     * form below. The falsifier is written in the {@code ///PIN:} note: the MULTI-attribute
     * fixture.
     *
     * <p>MULTI-attribute variant of a1: the value is SINGLE, the attribute MULTI, so the ctor
     * takes the singleton-list hoist and the local's declared type must be the WRAPPER whenever
     * the plain setter was chosen.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theHoistedLocalTypeAgreesWithTheSetterChosen() throws IOException {
        ///PIN: OUTSTANDING (not a measurement gap this seat can close by re-reading a log): this
        ///PIN: pin needs a MULTI-attribute fixture whose value is SINGLE and erased — add
        ///PIN: `codes string (0..*) [metadata scheme]` to Target and a func setting it from
        ///PIN: `h -> names -> code only-element`, then measure the rendered pair at the law
        ///PIN: head and assert (setCodes( AND final FieldWithMetaString ) together. Until then
        ///PIN: the assert below is the placeholder form and m-law8-halves measures it EMPTY.
        String out = fn("A1ErasedMetaValueKeepsPlainSetter.java");
        String code = normalize(out);
        assertTrue(!code.contains(".setCodeValue("),
                "placeholder until the MULTI fixture lands; the LAW-69 pin proper asserts the"
                        + " hoisted local's declared type against the setter chosen:\n" + out);
    }

    // =========================================================================
    // Part B — the decline pins (witness-unique on the token the flip REMOVES)
    // =========================================================================

    /**
     * b1 — the RECOVERY conjunct. A meta-FREE leaf recovers nothing, so the {@code Value} form
     * survives. WITNESS-UNIQUE on {@code .setCodeValue(} — the exact token a1 asserts absent.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_erasedBareValueStillTakesTheValueForm() throws IOException {
        String out = fn("B1BareErasedValue.java");
        assertTrue(normalize(out).contains(".setCodeValue("),
                "a meta-FREE erased value must keep the Value form:\n" + out);
    }

    /**
     * b2 — the FALL-THROUGH pin. A meta-FREE attribute never enters the annotation loop, so it
     * is decided by the pre-existing {@code return base;} fall-through. This pins that law 8 did
     * NOT move the fall-through (the F2a rung the shape-F notes drafted and the probe measured
     * as 91-of-92 GREEN — deliberately NOT shipped).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_metaFreeAttributeFallThroughIsUntouched() throws IOException {
        String out = fn("B2MetaFreeAttribute.java");
        String code = normalize(out);
        assertTrue(code.contains(".setPlain("),
                "a meta-free attribute keeps the plain setter via the fall-through:\n" + out);
        assertTrue(!code.contains(".setPlainValue("),
                "and never gains a Value form:\n" + out);
    }

    /**
     * b3 — the THIRD-RUNG pin. Where the compiled type IS present and IS the wrapper, the
     * render-truth proof already answers and the law is inert; the bytes must be identical to
     * the pre-law render.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_typedWrapperValueIsDecidedByRenderTruthAlone() throws IOException {
        String out = fn("B3TypedValueKeepsRenderTruth.java");
        assertTrue(normalize(out).contains(".setCode("),
                "a typed wrapper value keeps the plain setter via the render-truth proof:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus (LAW 79/80: a LINE lock; the file is F9 + F13 + F1)
    // =========================================================================

    private static final String UPI =
            "drr/base/trade/underlier/functions/UnderlierProductIdentifier.java";

    /**
     * control0 — golden is the oracle (prove the instrument can fail). Read from GOLDEN bytes
     * only: golden uses the PLAIN setter at the carrier seat, and the drr 7.0.0 golden tree
     * carries BOTH setter forms, so a one-sided scan cannot pass this quietly.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenUsesThePlainSetterAndTheCellCarriesBothForms() throws IOException {
        String g = normalize(Files.readString(GOLDEN_A.resolve(UPI)));
        assertTrue(g.contains(".setIdentifier(floatingRateIndex(product, identifierType)"),
                "golden must use the PLAIN setter at the carrier seat in " + UPI);
        assertTrue(!g.contains(".setIdentifierValue(floatingRateIndex("),
                "golden must NOT use the Value form at the carrier seat in " + UPI);
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_A));
        int valueForm = golden.values().stream().mapToInt(t -> t[0]).sum();
        int plainForm = golden.values().stream().mapToInt(t -> t[1]).sum();
        assertTrue(valueForm > 0 && plainForm > 0,
                "the drr 7.0.0 golden tree must carry BOTH setter forms (measured "
                        + valueForm + " Value / " + plainForm + " plain)");
    }

    /**
     * c1 — the setter-seat LINE lock. {@code UnderlierProductIdentifier} is a multi-family file
     * (F9 + F13, {@code lines=30 hunks=5}) and law 8 owns ONE hunk, so this is deliberately NOT
     * a whole-file lock: it pins the one line the law owns, and names the residue it does NOT
     * close.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_underlierProductIdentifierSetterSeatMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String gen = drrAOutput.get(UPI);
        assertNotNull(gen, "not generated: " + UPI);
        String code = normalize(gen);
        assertTrue(code.contains(".setIdentifier(floatingRateIndex(product, identifierType)"),
                "the carrier seat must take the PLAIN setter in " + UPI + ":\n" + gen);
        assertTrue(!code.contains(".setIdentifierValue("),
                "the Value form must be gone from " + UPI + ":\n" + gen);
    }

    /**
     * c1b — both hunks are now GOLDENS: the F9 ite-hoist wildcard decl healed at seat 31 (law 2b)
     * and the F1 implicit-receiver deref hops at seat 32 (law A.2), so {@code UnderlierProductIdentifier}
     * is WHOLE in drr 7.0-7.3 and appears in no {@code artefacts-final32} residue file. The two
     * asserts are golden-form locks that fire if a later law regresses the file.
     *
     * <p>HISTORY (seat 30): pinned as the RESIDUE the seat did NOT close, named so the LAW-80
     * arithmetic could not drift -- two hunks with no owning law in seat 30, the F9 wildcard and
     * the F1 hops.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1b_theF9DeclAndTheF1HopsAreGoldens() throws IOException {
        // LAW 81 re-pin (v3.1 flip seat 32, law A.2 wrapperItemReceiverBind): the binder re-landed with
        // the ONE conjunct (`lawRecvItem instanceof RJavaWithMetaValue`) and the F1 residue CLOSED - the
        // hop count printed `expected: <8> but was: <18>` (A2-trip1.log), 18 == golden's own count, and
        // UnderlierProductIdentifier is WHOLE in all four drr 7.x cells (ReceiverRenderTypingSeatTest
        // corpus_c2/c3 are the byte locks). The narrative below is the seat-31 record it supersedes.
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String code = normalize(drrAOutput.get(UPI));
        // LAW 81 re-pin (v3.1 flip seat 31, law 2b): this assert fired exactly as the class
        // javadoc prescribes - a law reached the file. Seat-31 law 2b (the CFH deep-then
        // DECL-ONLY wildcard) healed the F9 decl and that heal HOLDS; law 2's rung 1 (the F1
        // close) was REVERTED as refuted-as-landed (8ba04381a), so the file is NOT whole -
        // IMPROVED 28 -> 26 diff lines, 3 hunks, across drr 7.0-7.3 (artefacts-final31/off-rows.tsv).
        // ReceiverRenderTypingSeatTest corpus_c2 is the companion F9 DECL pin (not a byte lock);
        // the F1 residue is pinned below by F1_HOPS_AT_LAW8_HEAD = 8 (golden carries 18). The
        // pin flips to the healed decl form, transcribed from the fired run.
        assertTrue(code.contains("final MapperC<? extends ReferenceObligation> thenArg0;"),
                "the F9 decl must stay healed (seat-31 law 2b) — if this fires, a law"
                        + " regressed the wildcard and the LAW-80 accounting must be re-stated");
        // MEASURED, and the earlier reading of this number was WRONG — corrected here by the
        // independent review rather than left standing.
        //
        // WHAT IS TRUE: the fork emits EIGHT `.<Observable>map("Type coercion"` hops in this
        // file and emitted exactly eight BEFORE the seat too. The fork's F1 text is
        // BYTE-IDENTICAL at the pre-seat head and at the law-8 head: every one of the ten
        // `Type coercion` lines in hunks30/7.0.0_FUNCTION~UnderlierProductIdentifier.diff is
        // on the GOLDEN (`-`) side and NONE is on the fork (`+`) side, and the fork's A-side
        // text is unchanged at all four surviving hunk groups — only the `.setIdentifierValue`
        // hunk left, which is law 8's own heal. The eight hops the fork does emit are
        // UNCHANGED CONTEXT, present on both sides, outside every hunk.
        //
        // WHAT WAS WRONG: the draft (law8s-notes §6, H2/H3) read "the fork drops it" as
        // "the fork emits zero", so this pin was first written expecting 0 and, when it
        // measured 8, the miss was narrated as laws 2/6 reaching the file. They reached
        // NOTHING here. The arithmetic golden-18 / fork-8 / missing-10 = H2's 8 + H3's 2
        // is the F1 residue exactly as §6 sizes it; only the drafted expectation of the
        // fork's own count was wrong. So this assert is a STABLE residue pin, not a
        // restatement after a cross-law reach, and the LAW-80 accounting for this file
        // (4 partial / 0 whole / 0 entered) never needed restating.
        int p30hops = 0;
        int p30i = 0;
        while ((p30i = code.indexOf(".<Observable>map(\"Type coercion\"", p30i) + 1) > 0) {
            p30hops++;
        }
        assertEquals(F1_HOPS_AT_LAW8_HEAD, p30hops,
                "the F1 hop count moved — re-state the LAW-80 accounting (golden has 18)");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0. Domain = every file whose
     * GOLDEN <b>or</b> FORK text carries a {@code .set…(} on a meta-capable attribute; per file
     * the ({@code set*Value(} count, plain {@code set*(} count, total setter count) triple must
     * equal golden's beyond the NAMED residue.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellSetterFormsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /** control2 — LAW 77 route parity for the carrier. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(UPI), irOut.get(UPI), "route divergence: " + UPI);
    }

    /**
     * control3 — LAW 79 in the NAMED over-fire cell (cdm 6.20.6), same UNION triple, PLUS the
     * two GREEN pins by name: {@code MapAdjustedDateToAdjustableDate} and
     * {@code MapDateToAdjustableDate} legitimately carry {@code .set*Value(} for meta-FREE
     * values and must stay on the {@code Value} path.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkCdm6206WholeCellSetterFormsEqualGoldenFileByFile() throws IOException {
        assertNotNull(cdmBOutput, "cdm 6.20.6 generation did not run");
        assertEquals(List.of(), cdmBGenErrors,
                "cdm 6.20.6 reported a generation error — the scan is incomplete");
        for (String named : NAMED_GREEN_VALUE_PINS) {
            String gen = cdmBOutput.get(named);
            assertNotNull(gen, "the NAMED green pin was not generated: " + named);
            assertEquals(normalize(Files.readString(GOLDEN_B.resolve(named))), normalize(gen),
                    "the NAMED green Value-form pin must stay byte-identical: " + named);
        }
        assertUnionEqual(scan(cdmBOutput), scan(readGoldenTree(GOLDEN_B)), cdmBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM6206, DOMAIN_CDM6206);
    }

    /**
     * control4 — the SECOND HALF's own control (LAW 69). The disjunct also lands in
     * {@code hoistSingleValueIntoMultiOrNull}, whose reach the probe never measured. Scan the
     * hoist population in BOTH cells: a {@code Collections.singletonList(<name>)} whose hoisted
     * local is wrapper-typed must correspond, file for file, to golden's. A NON-EMPTY residue
     * here is the ENTERING signal for the second half.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control4_theHoistedLocalTypesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertUnionEqual(scanHoist(drrAOutput), scanHoist(readGoldenTree(GOLDEN_A)),
                drrAOutput.keySet(), "fork", "golden", KNOWN_HOIST_RESIDUE_DRR7,
                DOMAIN_HOIST_DRR7);
        if (cdmBOutput != null) {
            assertUnionEqual(scanHoist(cdmBOutput), scanHoist(readGoldenTree(GOLDEN_B)),
                    cdmBOutput.keySet(), "fork", "golden", KNOWN_HOIST_RESIDUE_CDM6206,
                    DOMAIN_HOIST_CDM6206);
        }
    }

    ///PIN: MEASURED at the law's head — the two cdm6 green Value-form pins' exact paths,
    ///PIN: corrected to their real ingest-fpml locations at the law-8 commit.
    private static final List<String> NAMED_GREEN_VALUE_PINS = List.of(
            "cdm/ingest/fpml/confirmation/datetime/functions/MapAdjustedDateToAdjustableDate.java",
            "cdm/ingest/fpml/confirmation/datetime/functions/MapDateToAdjustableDate.java");

    ///PIN: MEASURED at the law's head — one entry, transcribed VERBATIM from control1's print.
    ///PIN: The lane moves it: m-law8 prints "2 file(s)", adding UnderlierProductIdentifier.java
    ///PIN: fork=[1, 13, 14] — the carrier's setter row re-entering. RED prints 2 as well.
            // the Enrich_TransactionReportInstructionTestPackDefault row (fork=[4, 4, 8] golden=[0, 16, 16]) LEFT this list at seat 33:
            // law E.234 (rung E.4 ctorAsKeyBareValueReference - the four as-key pairs now hoist the bare value and copy the meta KEYS) healed the file WHOLE in all four drr 7.x cells
            // (EnrichReportInstructionWholeSeatTest corpus_c1/c2); transcribed from the control print (E234-trip1.log).
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    ///PIN: MEASURED EMPTY at the law's head. A NON-EMPTY result is the ENTERING signal.
    private static final List<String> KNOWN_RESIDUE_CDM6206 = List.of();

    ///PIN: MEASURED at the law's head. The draft EXPECTED EMPTY (the second half's
    ///PIN: byte-neutrality claim); the measurement said FOUR rows, so the expectation was
    ///PIN: corrected and the rows transcribed VERBATIM rather than the control weakened. The
    ///PIN: byte-neutrality claim itself is adjudicated by m-law8-halves measuring EMPTY, not by
    ///PIN: this list: these four are pre-existing OTHER-family hoist deltas, not law-8 movement.
    ///PIN: NOTE control4 fails at the RED base (5 file(s)) but NOT under m-law8 — the extra row
    ///PIN: belongs to a different seat-30 law. See the class javadoc.
    ///PIN: RE-MEASURED at the seat-31 chain head f2a4d5c0 — 2 entries, transcribed VERBATIM from
    ///PIN: control4's own failing print (LAW 81): two rows LEFT, both noted inline below.
    private static final List<String> KNOWN_HOIST_RESIDUE_DRR7 = List.of(
            // LAW 81 re-pin (v3.1 flip seat 33, law A.4): the GetBasketConstituents row (fork=[0, 0, 0] golden=[0, 0, 1]) LEFT this list -
            // facet lolDefaultBodyMulti moved this scan's tuple to golden's (the file stays BANDED on its A.3/A.5/A.1/A.2
            // residue; LolDefaultBodyMultiSeatTest pins that residue by name); transcribed from the control print (A4-trip1.log).
            // UnderlierBasketIdentifier.java (was fork=[0, 0, 0] golden=[0, 1, 0]) left this list at
            // seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so golden's hoisted MapperS<Basket> local (T2) now renders. Golden's
            // T2 = 1 keeps the file inside the union domain.
            // CustomBasketCodeRule.java (was fork=[0, 1, 0] golden=[0, 0, 0]) left this list at seat 31:
            // law 4b (choiceSwitchLambdaOptionGetter - the in-lambda CHOICE switch lowers its case guards to option-getter
            // null-tests with MAPPER-typed case locals, the live-bound naming rung hoisted) healed it WHOLE in drr 7.0.0
            // ONLY (7.1-7.3 IMPROVED 50 -> 49 lines and stay banded on a DIFFERENT mechanism - the in-lambda nested
            // then-chain hoist, S32's), so the fork's spurious `switchArgument` hoist (T2) is gone and the triple equals
            // golden's ALL-ZERO one. Because golden's tuple is all-zero, the file also LEAVES the
            // token-bearing union — DOMAIN_HOIST_DRR7 393 -> 392 (DERIVED, see the pin's own comment).
            // the Price row (fork=[0, 0, 0] golden=[0, 0, 1]) LEFT this list: law C.1 (iteChainNestedThenLadderAdmit + R2a/R2b/R4)
            // rendered the seven-rung ladder as statements and took this scan's token set to golden's in
            // all four drr 7.x cells - the file stays BANDED on C.2's default join; transcribed from this
            // control's own print (C1-trip1.log), a pure row removal (was == expected minus it).
            );

    ///PIN: MEASURED EMPTY at the law's head — the cross-corpus half of the byte-neutrality
    ///PIN: claim, and it held on both routes.
    private static final List<String> KNOWN_HOIST_RESIDUE_CDM6206 = List.of();

    /**
     * MEASURED at the law-A.2 head (seat 32) and transcribed from the fired assert
     * ({@code expected: <8> but was: <18>}, {@code logs/A2-trip1.log}): fork == golden == 18 hops;
     * the F1 residue is CLOSED. (Seat 30 measured 8 at its law-8 head and at the pre-seat head --
     * golden's 18 less the ten hops the binder now renders.)
     */
    // LAW 81 history (seat 31): law 2's rung-1 binder briefly moved this 8 -> 18 == golden,
    // then rung 1 was REVERTED as REFUTED-AS-LANDED (the wrapper-item resolution-flip
    // regression on the mas FixedFloatRateLeg trio - the name-colliding `value` feature
    // resolves on the WRAPPER once the item is typed, dropping golden's coercion; the
    // trail: target/seat31-instruments/law2-rung1-bank.md). The pin returns to the
    // MEASURED 8 (fork's own stable count; golden has 18 - the F1 residue stands, now
    // co-owned with the resolver-preference law the bank names).
    // LAW 81 history (seat 32): law A.2 (wrapperItemReceiverBind) RE-LANDED the binder narrowed by the
    // meta-wrapper conjunct the seat-31 revert lacked (the mas trio's item is the BARE PriceSchedule, so
    // the conjunct declines it - [P32-RECVBIND] wrapper=false wouldBind=false); the pin moves 8 -> 18 ==
    // golden, transcribed from the fired run (A2-trip1.log). The F1 residue is CLOSED.
    private static final int F1_HOPS_AT_LAW8_HEAD = 18;

    ///PIN: MEASURED at the first corpus run (LAW 73). For all four domains below:
    ///PIN: Falsifier: the domain assert itself
    ///PIN: (assertEquals(expectedDomain, universe.size())) failing on a later run. NOT the
    ///PIN: harness's "MEASURED domain=" print, which only surfaces at the -1 SENTINEL and is
    ///PIN: never emitted once a real value is pinned; on a run where the RESIDUE assert fails
    ///PIN: first the domain assert is simply unreachable.
    private static final int DOMAIN_DRR7 = 1226;

    ///PIN: MEASURED at the first corpus run (LAW 73).
    private static final int DOMAIN_CDM6206 = 533;

    ///PIN: MEASURED at the first corpus run (LAW 73).
    // 393 -> 392 at seat 31: CustomBasketCodeRule's hoist triple went all-zero on BOTH sides (law 4b),
    // so it left the token-bearing union. DERIVED from the residue print, NOT printed by run 1 (the
    // residue assert fires first); the falsifier is this suite's own domain assert on the next chain.
    private static final int DOMAIN_HOIST_DRR7 = 392;

    ///PIN: MEASURED at the first corpus run (LAW 73).
    private static final int DOMAIN_HOIST_CDM6206 = 275;

    /**
     * (T1, T2, T3) per file: {@code .set*Value(} calls (the form this law removes), plain
     * {@code .set*(} calls on a builder chain (the form it adds), and the file's TOTAL
     * {@code .set} call count (the over-fire net — deliberately global, so a swap elsewhere in
     * the cell cannot cancel out against the carrier's move).
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = new int[3];
            for (String line : normalize(e.getValue()).split("\n")) {
                String s = line.trim();
                if (!s.startsWith(".set")) {
                    continue;
                }
                t[2]++;
                int paren = s.indexOf('(');
                if (paren > 0 && s.substring(0, paren).endsWith("Value")) {
                    t[0]++;
                } else {
                    t[1]++;
                }
            }
            if (t[2] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    /**
     * The SECOND HALF's scan (control4): (wrapper-typed hoist locals, bare-typed hoist locals,
     * {@code Collections.singletonList(} sites). The hoisted local's declared type is what
     * {@code hoistSingleValueIntoMultiOrNull}'s {@code wrapAsMeta} decides, so this is the
     * direct observable of the mirrored disjunct.
     */
    private static Map<String, int[]> scanHoist(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = new int[3];
            String[] lines = normalize(e.getValue()).split("\n");
            for (int i = 0; i < lines.length; i++) {
                String s = lines[i].trim();
                if (s.contains("Collections.singletonList(")) {
                    t[2]++;
                }
                if (s.startsWith("final ") && s.contains(" = ") && i + 1 < lines.length
                        && lines[i + 1].trim().startsWith("if (")
                        && lines[i + 1].contains("== null) {")) {
                    if (s.contains("WithMeta")) {
                        t[0]++;
                    } else {
                        t[1]++;
                    }
                }
            }
            if (t[0] + t[1] + t[2] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

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

    // =========================================================================
    // Harness (the seat-26/27/28/29 suite shape verbatim)
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

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat30law8.rosetta");
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
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat30law8".equals(m.namespace()));
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[CtorSetterValueWrapperRecoverySeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
