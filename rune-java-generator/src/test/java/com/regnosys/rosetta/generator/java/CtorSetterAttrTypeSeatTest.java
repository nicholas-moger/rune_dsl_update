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
 * SEAT 28, law A — facet {@code ctorCondArmSingleCoerce}: <b>a ctor/builder SETTER value that is a
 * CONDITIONAL declares its hoisted local at the ATTRIBUTE's Java type and converts EACH ARM in
 * place</b>, through a new {@code CondSingleCoerce} handshake — the SINGLE, meta-NONE twin of the
 * existing {@code CondListCoerce} the same pair loop pushes for a MULTI attribute
 * ({@code ConstructionHandler}'s pair loop, gated {@code cgm.isMulti(attr)}).
 *
 * <p><b>The golden vs fork shape</b> (drr 5.61.0 {@code Create_FloatingRate}, {@code setBsisPtSprd}
 * whose attribute is {@code int}, source {@code bsisPtSprd: if … -> Basis then spreadOfLeg1Basis}):
 * <pre>
 * golden:  Integer    ifThenElseResult3 = null;
 *              ifThenElseResult3 = spreadOfLeg1Basis == null ? null : spreadOfLeg1Basis.intValueExact();
 *          .setBsisPtSprd(ifThenElseResult3)
 * fork:    BigDecimal ifThenElseResult3 = null;
 *              ifThenElseResult3 = spreadOfLeg1Basis;
 *          .setBsisPtSprd((ifThenElseResult3 == null ? null : ifThenElseResult3.intValueExact()))
 * </pre>
 * and the CHAIN-armed sibling (drr 5.61.0 {@code GetPackg}, source
 * {@code bsisPtSprd: if … -> Basis then packageTransactionSpreadBasis}), where the guard's
 * twice-read operand hoists a value local INSIDE the branch first:
 * <pre>
 * golden:  Integer ifThenElseResult3 = null;
 *          if (…) {
 *              final BigDecimal bigDecimal = item.&lt;BigDecimal&gt;map("getPackageTransactionSpreadBasis", …).get();
 *              ifThenElseResult3 = bigDecimal == null ? null : bigDecimal.intValueExact();
 *          }
 * </pre>
 * That in-arm decl is byte-for-byte the one {@code ConstructionHandler
 * .hoistNumericCoerceCtorChainOrNull} already emits for the NON-conditional twin of the same pair
 * (drr 6.34.1 {@code GetPackg}: {@code final BigDecimal bigDecimal = …; .setBsisPtSprd((bigDecimal
 * == null ? null : bigDecimal.intValueExact()))}). The conditional PUSHES the existing conversion
 * into the arm; it does not invent one. Note the parens: the setter-arg context has them, the
 * assignment context does not.
 *
 * <p><b>The seat-27 negative history this suite exists to lock.</b> Law A was implemented once at
 * seat 27 as an {@code ctx.expectedType()} ride / a blanket re-type of {@code declClazz}, and
 * MEASURED 0 healed / 51 ENTERED, then reverted byte-clean. Root cause: four arms inside
 * {@code hoistAsItemLocalOrNull}'s renderer key on {@code declClazz} by VALUE-TYPE EQUALITY, and
 * moving it silences them — chiefly the #377 {@code iteArmBareInvokableMetaDeref} arm
 * ({@code wrapperValue.getCanonicalName().withDots().equals(declClazz.getCanonicalName().withDots())}).
 * This design NEVER moves {@code declClazz}: the handshake wraps the arm renderer and re-types only
 * the DECL TEXT. {@code corpus_control4} byte-locks the golden witness of that class
 * (cdm 6.20.6 {@code MapUnitTypeWithScheme}, three {@code FieldWithMetaXEnum} arms) so the
 * regression cannot recur silently.
 *
 * <p><b>The probe verdict this law answers (LAW 75).</b> Seat 27's P27B measured the producer side:
 * {@code attrResolved=true} at 9,658/9,658 conditional ctor pairs, every law-A carrier
 * {@code isMulti=false}, and {@code ctx.expectedType()} {@code null} at 100% of sites (so the
 * charter's "override the expected type" premise was wrong — a type must be SUPPLIED, and it is
 * supplied to the DECL, not to the arm compiles). P27C measured the consumer side:
 * {@code declWork=BigDecimal parent=RKeyValuePair thenKind=RSymbolReference} at drr 5.61.0
 * {@code Create_FloatingRate} against golden's {@code Integer}. Seat 28's PROBE28-A prints
 * {@code attrJava} per conditional ctor pair corpus-wide.
 * MEASURED (PROBE28-A = 9,658 rows, route OFF, archived at
 * {@code target/seat28-instruments/lawA-out.txt}): the joined {@code declWork}/{@code attrJava}
 * verdict over the differs-gate's own domain — 343 rows, {@code lawA-differs.txt} — taken BEFORE coding.
 *
 * <p><b>RED at the pre-seat blob</b>: a1, a2, corpus_c1, corpus_control1. b1, b2, b3,
 * corpus_control3, corpus_control4, corpus_control5, corpus_control6 GREEN in BOTH states (the
 * decline pins).
 *
 * <p><b>LAW 81 — the controls this law's carriers are pinned by elsewhere, AUDITED:</b>
 * <ul>
 *   <li>{@code FunctionPr208CtorNarrowTest} byte-locks FOUR drr <b>6.34.1</b>
 *       {@code Create_FloatingRate*} files whose {@code bsisPtSprd} value is NOT a conditional in
 *       that cell's rune source ({@code bsisPtSprd: spreadOfLeg1Basis}). The handshake is never
 *       even pushed there. NO re-pin; {@code corpus_control3} scans that whole cell from this
 *       side.</li>
 *   <li>{@code IntLiteralNumberSeatTest.corpus_c4_adjustFrequencyPeriodArmCoercesIntoTheForksOwn
 *       BigDecimalLocal} and {@code corpus_control3}'s {@code KNOWN_DRR7_RESIDUE} pin the fork's
 *       {@code final BigDecimal ifThenElseResult1;} + {@code ifThenElseResult1 =
 *       BigDecimal.valueOf(1);} at drr 7.0–7.3 {@code AdjustFrequencyPeriod} — a file that IS a
 *       law-A shape (walk {@code BigDecimal} via the #355 numeric JOIN, attribute {@code int}) and
 *       that this law DECLINES on the scalar-literal-arm gate, because the int literal must
 *       compile at the new slot type and the fork's literal seat
 *       ({@code HandlerHelper.intLiteralConditionalArmExpectedType}) reads the LADDER ROOT's
 *       inference on BOTH routes. NO re-pin needed; {@code corpus_control5} re-asserts both
 *       tokens from this side so a future widening cannot land silently. The widening is BANKED
 *       with its mechanism named (4 more whole-file rows).</li>
 * </ul>
 *
 * <p><b>LAW 66/76 mutations — THE PLANNED SEVERANCES, NOT RUN.</b> The seat's chain ran ONE
 * mutation on this suite — the WHOLE law reverted (see MEASURED MUTATIONS at the foot of this
 * javadoc). Each severance below is <b>NOT RUN — banked to S29</b>; its "expected" set is a PLAN:
 * <ol>
 *   <li>(a-i) the whole handshake reverted (the {@code condSingleCoerceFor} push deleted) —
 *       PLANNED: a1, a2, corpus_c1, corpus_control1. NOT RUN — banked to S29;</li>
 *   <li>(a-ii) the DIFFERS-GATE reverted alone (fire whenever a handshake exists, walk equality
 *       ignored) — PLANNED: b2 and corpus_control4 EXACTLY (every walk==attribute pair re-types);
 *       this is the link that proves the gate, not the mechanism. NOT RUN — banked to S29;</li>
 *   <li>(a-iii) the {@code condSingleCoercible} numeric class widened away (any real coercion
 *       fires) — NOT RUN, banked to S29. If it ever measures ZERO, the narrow class is
 *       CONTRIBUTION-FREE and must be adjudicated (removed, or kept with the measurement
 *       recorded), per the seat-26 zero-adjudication close;</li>
 *   <li>(b-i) the ARM conversion reverted while the DECL type still moves (the half-landed
 *       state) — PLANNED: a1, a2, corpus_c1, corpus_control1 (the files stop compiling: a
 *       {@code BigDecimal} value into an {@code Integer} local). NOT RUN — banked to S29;</li>
 *   <li>(b-ii) the in-arm value-local hoist reverted (the guard composed inline over a chain) —
 *       PLANNED: a2 and the four {@code GetPackg} rows of corpus_c1 EXACTLY; the six
 *       bare-identifier rows are untouched (the arm-shape split). NOT RUN — banked to S29;</li>
 *   <li>(c-i) the {@code coerceCtorArg} fired-bypass reverted — PLANNED: a1, a2, corpus_c1,
 *       corpus_control1, AND a LAW-74 javac failure ({@code Integer.intValueExact()} does not
 *       exist), which is the point: the bypass is a correctness requirement, not a byte
 *       preference. NOT RUN — banked to S29;</li>
 *   <li>(c-ii) the scalar-literal-arm gate reverted — PLANNED: corpus_control5 EXACTLY (drr 7.x
 *       {@code AdjustFrequencyPeriod} moves), and {@code IntLiteralNumberSeatTest.corpus_c4} +
 *       its {@code corpus_control3} cross-suite (the LAW-81 tripwire firing from the other
 *       side). NOT RUN — banked to S29;</li>
 *   <li>(c-iii) the meta-NONE producer guard reverted — NOT RUN, banked to S29. The seat-27
 *       evidence says the entered class is 51 files, but the differs-gate and the numeric class
 *       stand behind this guard, so the measured set may be EMPTY — adjudicate it, do not declare
 *       it.</li>
 * </ol>
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>the whole law reverted (lawA-apply --revert: handshake class + producer + consumer + trio) -> a1, a2, corpus_c1, corpus_control1 (4F)</li>
 * </ul>
 */
class CtorSetterAttrTypeSeatTest {

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

    // =========================================================================
    // Part A — the fixture (a1/a2 = the two arm shapes; b1/b2/b3 = the decline pins)
    // =========================================================================

    /**
     * a1 = the {@code Create_FloatingRate} shape: an {@code int}-typed attribute takes a
     * CONDITIONAL {@code number} value whose arm is a BARE fn input — the guard stays inline.
     * a2 = the {@code GetPackg} shape: the same attribute, arm a NAV CHAIN — the guard's
     * twice-read operand hoists a value local INSIDE the branch first.
     * b1 = the same attribute with a PLAIN (non-conditional) value: the setter-side narrow is
     * golden's own form ({@code FunctionPr208CtorNarrowTest} at unit grain) and must not move.
     * b2 = a conditional whose walk type EQUALS the attribute type ({@code number} → {@code number}):
     * the DIFFERS-GATE must decline and the local must stay {@code BigDecimal} with a bare arm.
     * b3 = a conditional with a SCALAR-LITERAL arm at the same {@code int} attribute (the drr 7.x
     * {@code AdjustFrequencyPeriod} class): the literal-arm gate must decline the WHOLE ladder —
     * no re-type, no guard, no hoist.
     */
    private static final String MODEL = """
            namespace census.seat28a
            version "1.0.0"

            type Src:
                note string (0..1)
                basisVal number (0..1)
                dcmlVal number (0..1)

            type Dst:
                basis int (0..1)
                dcml number (0..1)

            func A1CtorSetterCondNarrowBareArm: <"a1 - the conditional value, BARE-input arm">
                inputs:
                    note string (1..1)
                    basisVal number (1..1)
                output:
                    out Dst (1..1)
                set out:
                    Dst {
                        basis: if note = "BASIS" then basisVal
                    }

            func A2CtorSetterCondNarrowChainArm: <"a2 - the conditional value, NAV-CHAIN arm">
                inputs:
                    s Src (1..1)
                output:
                    out Dst (1..1)
                set out:
                    Dst {
                        basis: if s -> note = "BASIS" then s -> basisVal
                    }

            func B1CtorSetterPlainNarrow: <"b1 - the PLAIN value keeps the setter-side narrow">
                inputs:
                    basisVal number (1..1)
                output:
                    out Dst (1..1)
                set out:
                    Dst {
                        basis: basisVal
                    }

            func B2CtorSetterCondSameType: <"b2 - walk == attribute: the differs-gate declines">
                inputs:
                    note string (1..1)
                    dcmlVal number (1..1)
                output:
                    out Dst (1..1)
                set out:
                    Dst {
                        dcml: if note = "DECIMAL" then dcmlVal
                    }

            func B3CtorSetterCondLiteralArm: <"b3 - a scalar-literal arm declines the whole ladder">
                inputs:
                    note string (1..1)
                    basisVal number (1..1)
                output:
                    out Dst (1..1)
                set out:
                    Dst {
                        basis: if note = "ONE" then 1 else basisVal
                    }
            """;

    /**
     * a1 — the local declares at the ATTRIBUTE type, the arm carries the null-safe conversion with
     * NO outer parens (the assignment context), and the setter splices the local BARE.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_ctorSetterCondBareArmDeclaresAtAttributeTypeAndConvertsInArm() throws IOException {
        String out = fn("A1CtorSetterCondNarrowBareArm.java");
        String code = codeOnly(out);
        // MEASURED at the chain head c6871ed5: the sentinel token is `ifThenElseResult` bare (a
        // singleton group) - pinned from this fixture's own render, suite GREEN at that head.
        assertContains(out, "Integer ifThenElseResult = null;");
        assertContains(out, "ifThenElseResult = basisVal == null ? null : basisVal.intValueExact();");
        assertContains(out, ".setBasis(ifThenElseResult)");
        assertTrue(!code.contains("BigDecimal ifThenElseResult"),
                "the local must NOT keep the THEN arm's own (walk) type:\n" + out);
        assertTrue(!code.contains(".setBasis((ifThenElseResult"),
                "the setter must splice the local BARE — the narrow moved into the arm:\n" + out);
        assertTrue(!code.contains("(ifThenElseResult == null ? null : ifThenElseResult"),
                "the setter-side narrow must not double-fire on the already-narrowed local "
                + "(Integer has no intValueExact()):\n" + out);
    }

    /**
     * a2 — the CHAIN arm: the guard reads its operand twice, so the collapsed chain hoists to a
     * walk-typed local INSIDE the branch (upstream's convertNullSafe law; the same decl
     * {@code hoistNumericCoerceCtorChainOrNull} emits for the non-conditional twin) and the guard
     * consumes the local.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_ctorSetterCondChainArmHoistsTheValueLocalInsideTheBranch() throws IOException {
        String out = fn("A2CtorSetterCondNarrowChainArm.java");
        String code = codeOnly(out);
        assertContains(out, "Integer ifThenElseResult = null;");
        // MEASURED at the chain head c6871ed5: the value local's name is the walk type's lowercased
        // simple name through the session group — `bigDecimal` bare for a singleton, pinned GREEN.
        assertContains(out, "final BigDecimal bigDecimal = ");
        assertContains(out, "ifThenElseResult = bigDecimal == null ? null : bigDecimal.intValueExact();");
        assertContains(out, ".setBasis(ifThenElseResult)");
        assertTrue(!code.contains("BigDecimal ifThenElseResult"),
                "the SENTINEL must be Integer even though the in-arm value local is BigDecimal:\n" + out);
        assertTrue(!code.contains(".setBasis((ifThenElseResult"),
                "the setter must splice the local BARE:\n" + out);
        // The value local belongs INSIDE the if-block, not above the sentinel decl: the arm
        // mark/drain window relocates it (golden drr 5.61.0 GetPackg:110-111).
        int declAt = code.indexOf("Integer ifThenElseResult = null;");
        int localAt = code.indexOf("final BigDecimal bigDecimal = ");
        assertTrue(declAt >= 0 && localAt > declAt,
                "the in-arm value local must follow the sentinel decl (it lives inside the "
                + "branch, not hoisted above it):\n" + out);
    }

    /**
     * b1 — the decline pin: a PLAIN value at the same {@code int} attribute keeps the setter-side
     * narrow byte-for-byte. The unit-grain twin of {@code FunctionPr208CtorNarrowTest}: the
     * handshake is only pushed for an {@code RConditionalExpr} value, so this shape never reaches
     * the hoist at all.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_ctorSetterPlainValueKeepsSetterSideNarrow() throws IOException {
        String out = fn("B1CtorSetterPlainNarrow.java");
        String code = codeOnly(out);
        assertContains(out, ".setBasis((basisVal == null ? null : basisVal.intValueExact()))");
        assertTrue(!code.contains("ifThenElseResult"),
                "a non-conditional value must not hoist a local at all:\n" + out);
    }

    /**
     * b2 — THE DIFFERS-GATE's own pin: walk type == attribute type, so nothing may move. The local
     * stays at the walk type and the arm stays BARE. This is the link that separates the gate from
     * the mechanism (mutation a-ii is PLANNED to move this and nothing else — NOT RUN, S29).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_ctorSetterCondWithWalkEqualToAttributeIsUnchanged() throws IOException {
        String out = fn("B2CtorSetterCondSameType.java");
        String code = codeOnly(out);
        assertContains(out, "BigDecimal ifThenElseResult = null;");
        assertContains(out, "ifThenElseResult = dcmlVal;");
        assertContains(out, ".setDcml(ifThenElseResult)");
        assertTrue(!code.contains("== null ? null :"),
                "a walk==attribute pair must carry NO conversion guard at all:\n" + out);
        assertTrue(!code.contains(".intValueExact()"),
                "a walk==attribute pair must carry no narrowing:\n" + out);
    }

    /**
     * b3 — the SCALAR-LITERAL arm gate: the drr 7.x {@code AdjustFrequencyPeriod} class at unit
     * grain. Golden's form there assigns the literal BARE at a seat-typed local. This test pinned
     * the DECLINE of that ladder (the walk-typed local + the coerced literal) from seat 28 until
     * seat 32, with the instruction that the banked literal-arm law would invert it.
     * <b>RE-PINNED at seat 32, law B.2 (ctorCondSingleCoerceLiteralArm)</b> — the ladder is now
     * ADMITTED: the local is typed at the ctor-pair ATTRIBUTE ({@code Integer}), the int literal
     * assigns bare at that type, the else arm takes the guarded {@code intValueExact()} and the
     * setter splices the local bare — every token below transcribed from this fixture's own
     * post-law render (B2-trip1.log, print-first).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_ctorSetterCondWithScalarLiteralArmTypesTheLocalAtTheAttribute() throws IOException {
        String out = fn("B3CtorSetterCondLiteralArm.java");
        String code = codeOnly(out);
        assertContains(out, "final Integer ifThenElseResult;");
        assertContains(out, "ifThenElseResult = 1;");
        assertContains(out, "ifThenElseResult = basisVal == null ? null : basisVal.intValueExact();");
        assertContains(out, ".setBasis(ifThenElseResult)");
        assertTrue(!code.contains("BigDecimal ifThenElseResult"),
                "seat 32 law B.2: the literal-armed ladder types its local at the ctor-pair "
                + "attribute, never at the walk type any more:\n" + out);
        assertTrue(!code.contains("BigDecimal.valueOf(1)"),
                "seat 32 law B.2: the int literal assigns BARE at the attribute-typed local:\n" + out);
        assertTrue(!code.contains("final BigDecimal bigDecimal"),
                "a literal-armed ladder must hoist NO in-arm value local:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carriers (drr 5.61.0 FUNCTION; 10 whole-file rows)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR634_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR634_GOLDEN = DRR634_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    static boolean drr634Available() {
        return Files.isDirectory(DRR634_GOLDEN);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), CtorSetterAttrTypeSeatTest.class);
    }

    static boolean cdm6Available() {
        return Files.isDirectory(CDM6_GOLDEN);
    }

    static boolean drr561AndIrProviderAvailable() {
        return drr561Available() && irProviderOnClasspath();
    }

    /**
     * The ten whole-file carriers — every path named, LAW 4 (per-member, never
     * first-3-extrapolated). Six carry the BARE-input arm shape ({@code Create_FloatingRate*}) and
     * four the CHAIN arm shape ({@code GetPackg}); mutation b-ii (PLANNED, NOT RUN — S29) splits them.
     * All ten heal at exactly ONE conditional each — the {@code bsisPtSprd} pair. Their sibling
     * {@code mntryVal} / {@code dcml} / {@code pctg} / {@code sgn} conditionals in the SAME files
     * have walk == attribute and must not move a byte, which is what makes the whole-file byte
     * lock (rather than a token count) the right instrument here.
     */
    private static final List<String> CARRIERS_561 = List.of(
            "drr/projection/iso20022/asic/rewrite/trade/functions/Create_FloatingRate.java",
            "drr/projection/iso20022/esma/emir/refit/trade/functions/Create_FloatingRate.java",
            "drr/projection/iso20022/fca/ukemir/refit/trade/functions/Create_FloatingRate.java",
            "drr/projection/iso20022/mas/rewrite/trade/functions/Create_FloatingRate.java",
            "drr/projection/iso20022/asic/rewrite/trade/functions/Create_FloatingRate2.java",
            "drr/projection/iso20022/jfsa/rewrite/trade/functions/Create_FloatingRate13__1.java",
            "drr/projection/iso20022/asic/rewrite/trade/functions/GetPackg.java",
            "drr/projection/iso20022/esma/emir/refit/trade/functions/GetPackg.java",
            "drr/projection/iso20022/fca/ukemir/refit/trade/functions/GetPackg.java",
            "drr/projection/iso20022/jfsa/rewrite/trade/functions/GetPackg.java");

    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_allTenCarriersByteIdentical() throws IOException {
        for (String p : CARRIERS_561) {
            lock561(p);
        }
    }

    /**
     * control0 — the ORACLE is golden, and it is read here rather than asserted from memory: in
     * every carrier golden declares the {@code bsisPtSprd} local at the SETTER's {@code Integer}
     * type and writes the conversion inside the arm, with NO setter-side parenthesised narrow.
     * A positive control on the instrument itself (LAW: prove the oracle can fail).
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenDeclaresTheBsisPtSprdLocalAtTheSetterType() throws IOException {
        for (String p : CARRIERS_561) {
            String golden = Files.readString(DRR561_GOLDEN.resolve(p));
            assertTrue(golden.contains("Integer ifThenElseResult"),
                    "golden must declare the hoisted bsisPtSprd local at the setter's Integer "
                    + "type: " + p);
            assertTrue(golden.contains(".intValueExact();"),
                    "golden must carry the conversion in the ARM (an assignment, so it ends with "
                    + "`;` and has no outer parens): " + p);
            assertTrue(!golden.contains(".setBsisPtSprd((ifThenElseResult"),
                    "golden must splice the local BARE at the setter: " + p);
            // and the walk==attribute siblings in the SAME file keep the walk type
            assertTrue(golden.contains("BigDecimal ifThenElseResult")
                            || golden.contains("AmountAndDirection106"),
                    "golden must keep the file's walk==attribute sibling locals at the walk "
                    + "type — the law must move ONE local per file, not all of them: " + p);
        }
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan over drr 5.61.0: per file, the
     * (ifThenElseResult decls, BigDecimal-typed decls, Integer-typed decls, setter-side narrows)
     * quadruple must equal golden's file for file, over the UNION of both key sets, beyond the
     * NAMED residue (LAW 73 — the set, not the count). This is the control that sees an over-fire
     * in a file the ten carriers do not name.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control1_forkDrr561WholeCellIteDeclTypesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drr561GenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drr561Output), scan(readGoldenTree(DRR561_GOLDEN)),
                drr561Output.keySet(), "fork", "golden", KNOWN_RESIDUE_561,
                217);
    }

    /**
     * The named pre-existing residue of OTHER families in this cell — filled from the GREEN read of
     * this suite; any entry that LEAVES is re-pinned in the commit that heals it (LAW 81).
     */
    private static final List<String> KNOWN_RESIDUE_561 = List.of();  // MEASURED at the law-A head: EMPTY - all ten carriers whole

    /** control2 — LAW 77 route parity: the IR-route render of every carrier matches the legacy route. */
    @Test
    @EnabledIf("drr561AndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), new ArrayList<>());
        for (String p : CARRIERS_561) {
            assertEquals(drr561Output.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /**
     * control3 — THE STRUCTURAL NEGATIVE CONTROL, LAW 79 (the mechanism's reach beyond its charter
     * cell): the whole drr 6.34.1 cell. In that cell's rune source the SAME attribute takes a
     * NON-conditional value ({@code bsisPtSprd: spreadOfLeg1Basis} /
     * {@code bsisPtSprd: packageTransactionSpread -> basis}), so golden keeps the setter-side
     * narrow and the handshake is never pushed. The cell's pairs that ARE conditionals
     * ({@code sgn} ×17, {@code pctg} ×1 — seat-27 P27B) have walk == attribute and must decline on
     * the differs-gate. If this reddens, law A over-fired: fix the gate, do not re-pin.
     */
    @Test
    @EnabledIf("drr634Available")
    void corpus_control3_forkDrr634WholeCellUnchanged() throws IOException {
        assertNotNull(drr634Output, "drr 6.34.1 generation did not run");
        assertEquals(List.of(), drr634GenErrors,
                "drr 6.34.1 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drr634Output), scan(readGoldenTree(DRR634_GOLDEN)),
                drr634Output.keySet(), "fork", "golden", KNOWN_RESIDUE_634,
                389);
    }

    private static final List<String> KNOWN_RESIDUE_634 = List.of();

    /**
     * control4 — THE 51-ENTERED WITNESS, byte-locked against GOLDEN. cdm 6.20.6
     * {@code MapUnitTypeWithScheme} is the #377 {@code iteArmBareInvokableMetaDeref} carrier and is
     * GREEN today: three conditional ctor pairs, each declaring an ENUM-typed local from the walk
     * and each hoisting a {@code FieldWithMetaXEnum} wrapper INSIDE its branch, whose deref the
     * #377 arm gates on {@code wrapperValue.equals(declClazz)}. Seat 27's law-A attempt moved
     * {@code declClazz} and silenced that equality, entering 51 files. This design never moves it —
     * and, independently, walk == attribute here so the differs-gate declines and the numeric
     * class refuses an enum. Three locks, one file, byte-for-byte.
     */
    @Test
    @EnabledIf("cdm6Available")
    void corpus_control4_metaDerefArmCarrierByteIdentical() throws IOException {
        assertNotNull(cdm6Output, "cdm 6.20.6 generation did not run");
        String path = "cdm/ingest/fpml/confirmation/common/functions/MapUnitTypeWithScheme.java";
        String generated = cdm6Output.get(path);
        assertNotNull(generated, "not generated in cdm 6.20.6: " + path);
        String golden = Files.readString(CDM6_GOLDEN.resolve(path));
        // the oracle first — prove the witness is really the #377 shape (positive control)
        assertTrue(golden.contains("CapacityUnitEnum ifThenElseResult0 = null;")
                        && golden.contains("final FieldWithMetaCapacityUnitEnum fieldWithMetaCapacityUnitEnum = ")
                        && golden.contains("ifThenElseResult0 = fieldWithMetaCapacityUnitEnum == null ? null : fieldWithMetaCapacityUnitEnum.getValue();"),
                "the #377 witness shape moved in golden — re-derive this control before trusting it");
        assertEquals(normalize(golden), normalize(generated),
                "the #377 bare-invokable meta-deref carrier must stay byte-identical — this is the "
                + "seat-27 51-entered class. If it moved, law A re-typed declClazz (it must not) "
                + "or the meta-NONE producer guard was lost.");
    }

    /**
     * control5 — THE LITERAL-ARM SEAT, and the LAW-81 tripwire from this side. drr 7.0.0
     * {@code AdjustFrequencyPeriod} IS a law-A shape (walk {@code BigDecimal} from the #355 numeric
     * JOIN, attribute {@code int}, else arm convertible) and golden declares
     * {@code final Integer ifThenElseResult1;} with the THEN arm the int literal {@code 1} assigned
     * bare at the seat type. From seat 28 to seat 32 this test pinned the fork's DECLINE of that
     * ladder together with {@code IntLiteralNumberSeatTest.corpus_c4}, with the instruction that
     * the banked literal-seat law would fail both and re-pin both in one commit.
     * <b>RE-PINNED at seat 32, law B.2 (ctorCondSingleCoerceLiteralArm)</b>: the ladder is
     * admitted — the local typed at the ctor-pair attribute, the literal bare, the else arm
     * guarded — and the file is byte-identical to golden (B2-trip1.log / the B2 suite's
     * corpus_c1). This control now locks that heal.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control5_adjustFrequencyPeriodLiteralArmLadderAdmitted() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        String path = "drr/regulation/common/functions/AdjustFrequencyPeriod.java";
        String fork = drr7Output.get(path);
        assertNotNull(fork, "not generated in drr 7.0.0: " + path);
        String golden = Files.readString(DRR7_GOLDEN.resolve(path));
        assertTrue(golden.contains("final Integer ifThenElseResult1;")
                        && golden.contains("ifThenElseResult1 = 1;")
                        && golden.contains("ifThenElseResult1 = periodMultiplier == null ? null : periodMultiplier.intValueExact();"),
                "golden's AdjustFrequencyPeriod is the literal-arm shape — re-derive this "
                + "control before trusting it");
        assertTrue(fork.contains("final Integer ifThenElseResult1;"),
                "seat 32 law B.2: the local is typed at the ctor-pair attribute (Integer)");
        assertTrue(fork.contains("ifThenElseResult1 = 1;"),
                "seat 32 law B.2: the int literal assigns BARE at the attribute-typed local");
        assertTrue(fork.contains("ifThenElseResult1 = periodMultiplier == null ? null : periodMultiplier.intValueExact();"),
                "seat 32 law B.2: the else arm converts through the attribute type");
        assertTrue(!fork.contains("BigDecimal ifThenElseResult1"),
                "the walk-typed local of the declined form must be gone");
        assertEquals(golden.replace("\r\n", "\n"), fork.replace("\r\n", "\n"),
                "seat 32 law B.2 healed AdjustFrequencyPeriod WHOLE in drr 7.0.0");
    }

    /**
     * control6 — LAW 79 over the DENSEST cell: drr 7.0.0 carries 992 SINGLE conditional ctor pairs
     * (seat-27 P27B), the largest single-cell population the pushed handshake reaches. The scan is
     * the same quadruple; the permitted residue is this cell's pre-existing set PLUS the disclosed
     * {@code AdjustFrequencyPeriod} row.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control6_forkDrr7WholeCellIteDeclTypesEqualGoldenExceptTheDisclosed()
            throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        assertUnionEqual(scan(drr7Output), scan(readGoldenTree(DRR7_GOLDEN)),
                drr7Output.keySet(), "fork", "golden", KNOWN_RESIDUE_DRR7,
                // LAW 81 re-pin (seat 33, law D.3): 445 -> 444 - TotalNotionalQuantity's ONE
                // fork-only T1 token healed away and its golden tuple is all-zero, so the file
                // LEFT the union token-bearing domain; transcribed from this control's own
                // print (D3-trip3.log).
                // LAW 81 re-pin (seat 33, law C.1): 444 -> 443 - Price's fork-only T1 tokens (the
                // mixed-pole ternary's ite decls) healed away and its golden tuple is all-zero, so the
                // file LEFT the union token-bearing domain (the D.3/TNQ precedent); from C1-trip3.log.
                443);
    }

    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // the AdjustFrequencyPeriod row (fork=[2, 1, 0, 1] golden=[2, 0, 1, 0]) LEFT this list at
            // seat 32: law B.2 (ctorCondSingleCoerceLiteralArm) healed it WHOLE in all four drr 7.x
            // cells; transcribed from this control's own print (B2-trip1.log). Domain 445 UNMOVED
            // (golden's tuple stays non-zero).
            // the Price row (fork=[2, 0, 0, 0] golden=[0, 0, 0, 0]) LEFT this list: law C.1 (iteChainNestedThenLadderAdmit + R2a/R2b/R4)
            // rendered the seven-rung ladder as statements and took this scan's token set to golden's in
            // all four drr 7.x cells - the file stays BANDED on C.2's default join; transcribed from this
            // control's own print (C1-trip1.log), a pure row removal (was == expected minus it).
            // the TotalNotionalQuantity row (fork=[1, 0, 0, 0] golden=[0, 0, 0, 0]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    // =========================================================================
    // The scan + the union assert
    // =========================================================================

    /**
     * Per file over CODE only (string literals stripped, so a {@code map("getX", …)} witness or a
     * javadoc can never count):
     * <ol>
     *   <li>T1 — every {@code ifThenElseResult} DECLARATION line (both the {@code <T> x = null;}
     *       initializer form and the {@code final <T> x;} blank-final form);</li>
     *   <li>T2 — of those, the ones declared {@code BigDecimal} (the walk type this law moves
     *       AWAY from);</li>
     *   <li>T3 — of those, the ones declared {@code Integer} or {@code Long} (the attribute types
     *       this law moves TO);</li>
     *   <li>T4 — setter-side parenthesised narrows {@code .setX((ifThenElseResult…} (the form this
     *       law's fired-bypass removes).</li>
     * </ol>
     * A file with none of the four is omitted, so the union domain is the token-bearing set on
     * either side.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            int t4 = 0;
            for (String line : code.split("\n")) {
                String s = line.trim();
                if (isIteDeclLine(s)) {
                    t1++;
                    String declType = declaredTypeOf(s);
                    if ("BigDecimal".equals(declType)) {
                        t2++;
                    } else if ("Integer".equals(declType) || "Long".equals(declType)) {
                        t3++;
                    }
                }
                if (s.contains("((ifThenElseResult")) {
                    t4++;
                }
            }
            if (t1 + t4 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3, t4});
            }
        }
        return out;
    }

    /**
     * A DECLARATION of an {@code ifThenElseResult} local, not an assignment to one. Two golden
     * forms: {@code <T> ifThenElseResultN = null;} (the initializer form) and
     * {@code final <T> ifThenElseResultN;} (the blank-final form). The assignment
     * {@code ifThenElseResultN = null;} inside an arm also ends with {@code = null;}, so a decl
     * must not START with the name. Literal token work on rendered Java, not structural analysis
     * of language content — permitted by the engineering standard's stated exception.
     */
    private static boolean isIteDeclLine(String s) {
        if (!s.contains("ifThenElseResult") || s.startsWith("ifThenElseResult")) {
            return false;
        }
        if (s.startsWith("final ") && s.endsWith(";") && s.indexOf('=') < 0) {
            return true;
        }
        return s.endsWith("= null;");
    }

    /** The declared type token of a decl line — the word before the local name. */
    private static String declaredTypeOf(String s) {
        String t = s.startsWith("final ") ? s.substring("final ".length()) : s;
        int nameAt = t.indexOf("ifThenElseResult");
        return nameAt <= 0 ? "" : t.substring(0, nameAt).trim();
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        // Scoped to the files this harness emits (the seat-28 correction class).
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
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3, T4) differ beyond the named residue in " + mismatched.size()
                + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-26/27 suite shape verbatim)
    // =========================================================================

    private static Map<String, String> drr561Output;
    private static List<String> drr561GenErrors;
    private static Map<String, String> drr634Output;
    private static List<String> drr634GenErrors;
    private static Map<String, String> drr7Output;
    private static Map<String, String> cdm6Output;

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr561Available()) {
            List<String> errs = new ArrayList<>();
            drr561Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), errs);
            drr561GenErrors = errs;
        }
        if (drr634Available()) {
            List<String> errs = new ArrayList<>();
            drr634Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR634_CELL_ROOT), errs);
            drr634GenErrors = errs;
        }
        if (drr7Available()) {
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT),
                    new ArrayList<>());
        }
        if (cdm6Available()) {
            cdm6Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT),
                    new ArrayList<>());
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lock561(String path) throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr561GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr561Output.get(path);
        assertNotNull(generated, "not generated in drr 5.61.0: " + path);
        Path goldenPath = DRR561_GOLDEN.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 28 law A: the ctor-setter conditional declares its hoisted local "
                + "at the ATTRIBUTE's type and converts each arm in place.");
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

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat28a.rosetta");
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
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat28a".equals(m.namespace()));
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
            throw new AssertionError("[CtorSetterAttrTypeSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
