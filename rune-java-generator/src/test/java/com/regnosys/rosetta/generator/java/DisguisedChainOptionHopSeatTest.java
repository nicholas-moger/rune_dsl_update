package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * v3.1 LADDER RETIREMENT — flip seat 13: the disguised-chain CHOICE-OPTION hop
 * (lever C1 of the seat-11 census, re-traced at runtime in
 * {@code target/seat13-c1-probe-trace.md}; the charter {@code target/seat13-charter.md}).
 *
 * <p><b>The mechanism.</b> A Rune navigation chain whose ROOT is elided parses its
 * first two names as ONE {@code REnumValueRef} (the D39-Category-10 "disguised
 * feature call"). When the CHOICE-OPTION hop is that second name
 * ({@code payout -> CommodityPayout -> delivery}), the next hop's receiver is the
 * EVR node: {@code NavigationHandler.resolveLambdaVarName}'s EVR arm declined
 * through all three of its rungs and emitted the raw {@code evr.valueName()} — the
 * PascalCase {@code CommodityPayout -> CommodityPayout.getDelivery()} — where
 * upstream ({@code ExpressionGenerator.xtend:359-368}) names EVERY nav lambda param
 * {@code toFirstLower} of the RECEIVER's own type. Any EXPLICIT root ({@code item ->},
 * an alias, a function call) makes the option hop a genuine {@code RFeatureCall},
 * which resolves — the split is in the SOURCE, invisible in the emitted Java (why
 * 54 byte-identical-shaped corpus files split 13 carrier / 41 green). The second
 * half is one decline: the option hop renders through a projected attribute minted
 * by the gm-aware choice narrowing ({@code attributeToDataType(attr, compiler)} →
 * {@code RChoiceTypeRef.asRDataType()}), whose {@code RTypeCall.deepCopy} DROPS the
 * linker id by design; every consumer resolving that typeCall falls to the
 * {@code shouldGenerate}-FILTERED name lookup, and in every drr cell the option's
 * cdm namespace is a resolved-but-not-generated dependency → {@code RMissingType}:
 * {@code addWitnessTypeRef} registers NO import (the witness TEXT still renders from
 * the stored typeName — a non-compiling file) and {@code metaWrapperOf} returns null
 * (a meta-annotated option's next hop gets no {@code Type coercion} — Rm1-A).
 *
 * <p><b>The seat (two rungs, generator-side; the parser untouched):</b>
 * Rung A (facet {@code disguisedOptionHopLambdaVar}) — the EVR arm consults the SAME
 * gm-aware walk the witness/arity consumers read ({@code resolveReceiverDataType(evr,
 * compiler)}, measured returning the option type at the carrier hop TODAY) before the
 * raw-name fallback, with the #380 closure-param escape mirrored and the deferred
 * registration. Rung B (facet {@code optionProjectionIdAttach}) — the gm-aware choice
 * narrowing projects the options with the seat-1 id+attach PAIR (LAW 62;
 * {@code projectedOptionAttribute}: the option's linker id copied, the copy attached
 * and phantom-parented) instead of the id-less {@code asRDataType()} bridge — ONE
 * helper shared with the nested-choice bridge {@code authorityOptionDeclaredType}
 * already ran (LAW 69). Namespace-EXACT by construction: a name-based recovery was
 * REJECTED because the drr 7.0.0 workspace loads rune-fpml 2.1.1 transitively and
 * declares NINE option type names in two namespaces ({@code Product}, {@code Asset},
 * {@code Basket}, {@code Commodity}, {@code FloatingRateIndex}, …) — b9 pins it.
 *
 * <p><b>Green-safety:</b> the two-sided instrument (a PascalCase nav lambda param) is
 * 0 golden files across ALL 25 cells (159,318 goldens) vs 30 gen files PRE; the ring
 * cells cdm 5.38.0 · drr 6.34.1 · iso20022 1.38.0 · rune-fpml 2.0.0 declare ZERO
 * {@code choice} types (no option projection can exist there) and cdm 6.20.6's option
 * names are intra-tree unique (the filtered name lookup and the linker id denote the
 * SAME declaration); in dependency cells the change is recovery-only.
 */
class DisguisedChainOptionHopSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model")
    );

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /** The DECOY namespace — a same-simple-name {@code Product} loaded FIRST (b9). */
    private static final String MODEL_OTHER = """
            namespace census.seat13.other
            version "1.0.0"

            type Product: <"the DECOY - the same simple name as the option, another namespace">
                other string (0..1)
            """;

    /**
     * The dependency namespace — the corpus condition: these types are LOADED but
     * NOT GENERATED (the emission filter accepts {@code census.seat13} only), exactly
     * as every drr cell navigates vendored cdm types. {@code Payout} mirrors cdm's
     * choice with its {@code CommodityPayout}/{@code SettlementPayout} options;
     * {@code UnderlierC} mirrors {@code choice Underlier} whose {@code Observable}
     * option is meta-annotated (Rm1-A) and whose {@code Product} option shares its
     * simple name with the decoy (b9); {@code fixedRate : FixedRateSpecification} is
     * the NORMAL disguised pair whose feature name differs from its type (b6).
     */
    private static final String MODEL_DEP = """
            namespace census.seat13.dep
            version "1.0.0"

            type Delivery:
                loc string (0..1)
            type Basket:
                bname string (0..1)
            type ObservableM:
                basket Basket (0..1)
            type Product:
                pname string (0..1)
            type Underlier:
                prod Product (0..1)
            type CommodityPayout:
                delivery Delivery (0..1)
                underlier Underlier (0..1)
            type SettlementPayout:
                delivery Delivery (0..1)
                underlier Underlier (0..1)
            choice Payout:
                CommodityPayout
                SettlementPayout
            choice UnderlierC:
                ObservableM
                    [metadata reference]
                Product
            choice OuterC:
                Payout
                Delivery
            type FixedRateSpecification:
                rate number (0..1)
            type EconomicTerms:
                payout Payout (0..*)
                underlierC UnderlierC (0..1)
                outer OuterC (0..1)
                fixedRate FixedRateSpecification (0..1)
            type Root:
                economicTerms EconomicTerms (0..1)
                flag boolean (0..1)
            """;

    /** The generated namespace — the rules/functions (the probe's measured P1..P9 + the seat pins). */
    private static final String MODEL_MAIN = """
            namespace census.seat13
            version "1.0.0"

            import census.seat13.dep.*

            func GetEconomicTerms: <"b3 helper">
                inputs:
                    r Root (1..1)
                output:
                    et EconomicTerms (0..1)
                set et: r -> economicTerms

            reporting rule A1Carrier from EconomicTerms: <"a1/a3 - the CARRIER: elided root, the option is the 2nd name">
                extract payout -> CommodityPayout -> delivery first

            reporting rule A2Hoisted from Root: <"a2 - the then-hoisted production shape (DeliveryPointOrZone)">
                extract economicTerms
                then extract payout -> CommodityPayout -> delivery first

            reporting rule A4MetaOption from EconomicTerms: <"a4 - Rm1-A: the meta-annotated option -> the next hop's Type coercion">
                extract underlierC -> ObservableM -> basket first

            func PayoutOf: <"a5 helper - a FUNCTION whose DECLARED output is the choice (RateOption / UnderlierForProduct)">
                inputs:
                    et EconomicTerms (1..1)
                output:
                    p Payout (0..1)
                set p: et -> payout first

            reporting rule A5CallableHead from EconomicTerms: <"a5 - head forms (ii)/(iii): a CALLABLE head bound on the disguised pair, the option the 2nd name">
                extract PayoutOf -> CommodityPayout -> delivery first

            reporting rule B1ExplicitItem from EconomicTerms: <"b1 - GREEN control: the explicit item root">
                extract item -> payout -> CommodityPayout -> delivery first

            reporting rule B2AliasRoot from Root: <"b2 - GREEN control: an attribute/alias root">
                extract economicTerms -> payout -> CommodityPayout -> delivery first

            reporting rule B3FuncRoot from Root: <"b3 - GREEN control: a function-call root">
                extract GetEconomicTerms(item) -> payout -> CommodityPayout -> delivery first

            func B4FuncSeat: <"b4 - GREEN control: the function set seat">
                inputs:
                    et EconomicTerms (1..1)
                output:
                    d Delivery (0..1)
                set d: et -> payout -> CommodityPayout -> delivery first

            func B5ListExtract: <"b5 - GREEN control: an elided root under an EXPLICIT list extract">
                inputs:
                    ets EconomicTerms (0..*)
                output:
                    ds Delivery (0..*)
                add ds: ets extract payout -> CommodityPayout -> delivery first

            reporting rule B6NormalPair from Root: <"b6 - the NORMAL disguised pair: rung 1 keeps winning with the TYPE-derived name">
                extract economicTerms -> fixedRate -> rate

            reporting rule B9DupName from EconomicTerms: <"b9 - THE adversarial pin: the option's namespace, not the decoy's">
                extract underlierC -> Product -> pname first

            reporting rule B7Escape from Root: <"b7 - the #380 escape at the new rung: the type-derived name collides with the ENCLOSING extract's explicit closure param">
                extract economicTerms
                then extract commodityPayout [ payout -> CommodityPayout -> delivery first ]

            reporting rule A10NestedChoice from EconomicTerms: <"a10 - a NESTED-choice option (an option that is itself a choice) reached through the disguised hop">
                extract outer -> Payout -> CommodityPayout -> delivery first
            """;

    private static final String CARRIER_LAMBDA = "CommodityPayout -> CommodityPayout.getDelivery()";
    private static final String HEALED_LAMBDA = "commodityPayout -> commodityPayout.getDelivery()";
    private static final String OPTION_IMPORT = "import census.seat13.dep.CommodityPayout;";

    // =========================================================================
    // Part A — RED pre-seat (unit grain, the dependency namespace NOT generated)
    // =========================================================================

    /** a1 — the carrier: the hop after the option names from the option's TYPE
     *  ({@code commodityPayout}), not the raw {@code valueName()} ({@code CommodityPayout}). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_elidedRootOptionHop_lambdaNamesFromOptionType() throws IOException {
        String out = filtered("A1CarrierRule.java");
        assertContains(out, ".<Delivery>map(\"getDelivery\", " + HEALED_LAMBDA + ")");
        assertNotContains(out, CARRIER_LAMBDA);
    }

    /** a2 — the then-hoisted production shape (the {@code DeliveryPointOrZoneRule}
     *  minimal pair's carrier side). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_thenHoistedElidedRoot_lambdaNamesFromOptionType() throws IOException {
        String out = filtered("A2HoistedRule.java");
        assertContains(out, HEALED_LAMBDA);
        assertNotContains(out, CARRIER_LAMBDA);
    }

    /** a3 — Rung B: the option-witness IMPORT registers under the dependency-namespace
     *  filter (the corpus condition) — the witness text rendered both before and after;
     *  the import is what the id+attach projection recovers. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_optionWitnessImport_registersUnderDependencyFilter() throws IOException {
        String out = filtered("A1CarrierRule.java");
        assertContains(out, ".<CommodityPayout>map(\"getCommodityPayout\"");
        assertContains(out, OPTION_IMPORT);
    }

    /** a4 — Rm1-A, the fifth consumer of the same resolution: a META-annotated option
     *  ({@code ObservableM [metadata reference]} — the corpus carrier uses
     *  {@code [metadata address "pointsTo"=…]}, the same REFERENCE_WITH_META family;
     *  the address form is covered by the three Rm1-A corpus locks) reached through the disguised hop
     *  gets its wrapper result type, so the NEXT hop derefs it ({@code Type coercion}),
     *  the wrapper + value imports register, and the following lambda names from the
     *  option's type. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_metaAnnotatedOption_nextHopTypeCoercion() throws IOException {
        String out = filtered("A4MetaOptionRule.java");
        assertContains(out, ".<ReferenceWithMetaObservableM>map(\"getObservableM\", underlierC -> underlierC.getObservableM())");
        assertContains(out, ".<ObservableM>map(\"Type coercion\", referenceWithMetaObservableM -> "
                + "referenceWithMetaObservableM == null ? null : referenceWithMetaObservableM.getValue())");
        assertContains(out, ".<Basket>map(\"getBasket\", observableM -> observableM.getBasket())");
        assertContains(out, "metafields.ReferenceWithMetaObservableM;");
        assertContains(out, "import census.seat13.dep.ObservableM;");
        assertNotContains(out, "ObservableM -> ObservableM.getBasket()");
    }

    /** a5 — head forms (ii)/(iii): a CALLABLE head the parser bound on the disguised
     *  pair ({@code PayoutOf -> CommodityPayout -> delivery} — the corpus's
     *  {@code RateOption -> FloatingRateIndex}, {@code underlier.UnderlierForProduct ->
     *  Product}: FUNCTIONS whose DECLARED output is a choice); the symbol-receiver walk
     *  reaches the SAME choice narrowing for the option hop's witness + import, and the
     *  walk's EVR arm types the chained hop from the option — name + import. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_callableHead_optionSecondName_nameAndImport() throws IOException {
        String out = filtered("A5CallableHeadRule.java");
        assertContains(out, HEALED_LAMBDA);
        assertNotContains(out, CARRIER_LAMBDA);
        assertContains(out, OPTION_IMPORT);
    }

    /** a10 — a NESTED-choice option: {@code outer -> Payout -> CommodityPayout ->
     *  delivery} where {@code Payout} is an option of {@code OuterC} AND itself a choice
     *  (the corpus {@code choice Underlier}'s {@code Observable}/{@code Product} shape,
     *  the Copilot #571 R1 nested bridge). The hop after the nested option names from
     *  the nested choice ({@code payout}), its option hop resolves ({@code commodityPayout},
     *  witness + import), and both option imports register under the dependency filter. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a10_nestedChoiceOption_underDependencyFilter() throws IOException {
        String out = filtered("A10NestedChoiceRule.java");
        assertContains(out, ".<CommodityPayout>map(\"getCommodityPayout\", payout -> payout.getCommodityPayout())");
        assertContains(out, HEALED_LAMBDA);
        assertNotContains(out, "Payout -> Payout.getCommodityPayout()");
        assertNotContains(out, CARRIER_LAMBDA);
        assertContains(out, "import census.seat13.dep.Payout;");
        assertContains(out, OPTION_IMPORT);
    }

    // =========================================================================
    // Part B — inert pins (green PRE and POST; must not move)
    // =========================================================================

    /** b1 — the explicit {@code item ->} root: the option hop is a genuine
     *  {@code RFeatureCall}; already {@code commodityPayout} — the in-corpus minimal
     *  pair's green side. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_explicitItemRoot_unchanged() throws IOException {
        String out = filtered("B1ExplicitItemRule.java");
        assertContains(out, HEALED_LAMBDA);
        assertNotContains(out, CARRIER_LAMBDA);
    }

    /** b2 — an attribute/alias root ({@code economicTerms -> payout -> …}). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_attributeRoot_unchanged() throws IOException {
        String out = filtered("B2AliasRootRule.java");
        assertContains(out, HEALED_LAMBDA);
        assertNotContains(out, CARRIER_LAMBDA);
    }

    /** b3 — a function-call root ({@code GetEconomicTerms(item) -> payout -> …}). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_functionCallRoot_unchanged() throws IOException {
        String out = filtered("B3FuncRootRule.java");
        assertContains(out, HEALED_LAMBDA);
        assertNotContains(out, CARRIER_LAMBDA);
    }

    /** b4 — the FUNCTION {@code set} seat over an input root. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_functionSetSeat_unchanged() throws IOException {
        String out = filtered("B4FuncSeat.java");
        assertContains(out, HEALED_LAMBDA);
        assertNotContains(out, CARRIER_LAMBDA);
    }

    /** b5 — an elided root under an EXPLICIT list {@code extract} (the written argument
     *  binds the inline lambda's item, so the pair is not folded into a disguised EVR). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_elidedRootUnderExplicitListExtract_unchanged() throws IOException {
        String out = filtered("B5ListExtract.java");
        assertContains(out, HEALED_LAMBDA);
        assertNotContains(out, CARRIER_LAMBDA);
    }

    /** b6 — the NORMAL disguised pair ({@code economicTerms -> fixedRate}, a plain
     *  attribute whose NAME differs from its TYPE): rung 1 (the bound chain) keeps
     *  winning with the type-derived {@code fixedRateSpecification} — the proxy
     *  {@code toLowerCamelCase(valueName())} would have said {@code fixedRate}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_normalDisguisedPair_rungOneTypeDerivedName() throws IOException {
        String out = filtered("B6NormalPairRule.java");
        assertContains(out, "fixedRateSpecification -> fixedRateSpecification.getRate()");
        assertNotContains(out, "fixedRate -> fixedRate.getRate()");
    }

    /** b7 — the #380 escape MIRRORED on the new rung: inside {@code extract
     *  commodityPayout [ payout -> CommodityPayout -> delivery first ]} the hop after
     *  the option would be named {@code commodityPayout} — the ENCLOSING extract's
     *  explicit closure param — so the rung escapes it ({@code _commodityPayout}),
     *  exactly as upstream's {@code createUniqueIdentifier} does; the raw
     *  {@code CommodityPayout} is gone. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b7_typeDerivedNameCollidesWithEnclosingClosureParam_escaped() throws IOException {
        String out = filtered("B7EscapeRule.java");
        assertContains(out, "_commodityPayout -> _commodityPayout.getDelivery()");
        assertNotContains(out, CARRIER_LAMBDA);
    }

    /** b8 — the SAME-workspace control (no emission filter: the option's namespace IS
     *  generated): the import registered before the seat and still does — the filtered
     *  name lookup and the linker id denote the same declaration (the ring cells'
     *  situation, cdm 6.20.6). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b8_sameWorkspaceOptionImport_unchanged() throws IOException {
        String out = unfiltered("A1CarrierRule.java");
        assertContains(out, OPTION_IMPORT);
    }

    /** b9 — THE adversarial placement pin: NAMESPACE EXACTNESS. Two loaded namespaces
     *  declare a {@code Product} (the decoy {@code census.seat13.other.Product} is
     *  registered FIRST); the option hop must import the OPTION's declaring namespace
     *  ({@code census.seat13.dep.Product}) — RED-capable against a first-match
     *  name-based recovery ({@code resolveTypeByName}), which is why that form was
     *  rejected (the drr 7.0.0 workspace has nine such names). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b9_sameSimpleNameInAnotherNamespace_importsTheOptionsNamespace() throws IOException {
        String out = filtered("B9DupNameRule.java");
        assertContains(out, "import census.seat13.dep.Product;");
        assertNotContains(out, "census.seat13.other.Product");
        assertContains(out, "product -> product.getPname()");
        assertNotContains(out, "Product -> Product.getPname()");
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (20 WHOLE-FILE, RED pre-seat; 1 STAYS)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(builtinsAvailable()
                && Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), DisguisedChainOptionHopSeatTest.class);
    }

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors;

    @BeforeAll
    static void generateDrr7() throws IOException {
        if (drr7Available()) {
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT));
        }
    }

    /**
     * The 16 C1 WHOLE-FILE heals (every diff line = the PascalCase lambda param and/or
     * the option-witness import; goldens sha-identical across drr 7.0.0/7.1.0/7.2.0/7.3.0):
     * head form (i) elided-item roots, (ii) qualified FUNCTION-reference heads
     * ({@code underlier.UnderlierForProduct -> Product}: Series/Version/Tranche/…),
     * (iii) bare FUNCTION-reference heads ({@code RateOption -> FloatingRateIndex})
     * — drr {@code func}s whose DECLARED output is a choice.
     */
    private static final List<String> C1_WHOLE_FILE_LOCKS = List.of(
            "drr/regulation/common/dtcc/trade/reports/DTCC_DeliveryLocationRule.java",
            "drr/regulation/common/dtcc/trade/reports/DTCC_Leg2CommodityInstrumentIDRule.java",
            "drr/regulation/csa/rewrite/dtcc/trade/reports/DTCC_LoadTypeRule.java",
            "drr/regulation/common/dtcc/trade/reports/DTCC_TradeLegTypesRule.java",
            "drr/regulation/common/emir/reports/DeliveryPointOrZoneRule.java",
            "drr/standards/iosco/cde/version1/datetime/reports/ExpirationDateRule.java",
            "drr/regulation/common/trade/underlier/reports/FloatingRateReferencePeriodMultiplierRule.java",
            "drr/regulation/common/trade/underlier/reports/FloatingRateReferencePeriodRule.java",
            "drr/regulation/common/emir/reports/InterconnectionPointRule.java",
            "drr/regulation/common/trade/underlier/functions/IsUnderlierForIndex.java",
            "drr/regulation/common/emir/reports/LoadTypeRule.java",
            "drr/regulation/common/trade/datetime/reports/MaturityDateOfTheUnderlierRule.java",
            "drr/regulation/common/trade/index/reports/SeriesRule.java",
            "drr/regulation/common/emir/reports/TrancheRule.java",
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlierIDOtherSourceLeg2Rule.java",
            "drr/regulation/common/trade/index/reports/VersionRule.java");

    /**
     * The 3 Rm1-A WHOLE-FILE heals — the meta-annotated option {@code Observable
     * [metadata address …]} of {@code choice Underlier} reached through a disguised
     * hop: the next hop's {@code Type coercion} + the wrapper import (the SAME
     * projection recovery, its fifth consumer); C1 lines ride in the same files.
     */
    private static final List<String> RM1A_WHOLE_FILE_LOCKS = List.of(
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetPriceSourceLeg1Rule.java",
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetPriceSourceLeg2Rule.java",
            "drr/regulation/common/trade/contract/reports/DeliveryTypeFromProductRule.java");

    /** The Rm1-A in-file pair whose whole-file heal is BLOCKED by a rider found at the
     *  seat head: golden's per-lambda scope reuses the BARE coercion param across the
     *  three sibling lambdas ({@code referenceWithMetaObservable} ×3) while the fork
     *  numbers them across the method ({@code 0,1,2}; PRE {@code 0,1} for its two
     *  coercions — pre-existing, hidden on the same long lines). Pinned SUB-FILE on
     *  the chartered tokens; the numbering is a NEW menu item (the seat-14 trace). */
    private static final String RM1A_SUBFILE_PIN =
            "drr/regulation/asic/rewrite/trade/reports/UnderlyingIdentificationTypeRule.java";

    @Test @EnabledIf("drr7Available") void corpus_c1_01_dtccDeliveryLocation() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(0)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_02_dtccLeg2CommodityInstrumentId() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(1)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_03_dtccLoadType_csa() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(2)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_04_dtccTradeLegTypes() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(3)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_05_deliveryPointOrZone_theMinimalPair() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(4)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_06_expirationDate_38lines() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(5)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_07_floatingRateReferencePeriodMultiplier() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(6)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_08_floatingRateReferencePeriod_bareFunctionHead() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(7)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_09_interconnectionPoint() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(8)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_10_isUnderlierForIndex_function() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(9)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_11_loadType_rungAAlone() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(10)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_12_maturityDateOfTheUnderlier() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(11)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_13_series_qualifiedFunctionHead() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(12)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_14_tranche() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(13)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_15_underlierIdOtherSourceLeg2() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(14)); }
    @Test @EnabledIf("drr7Available") void corpus_c1_16_version() throws IOException { lock(C1_WHOLE_FILE_LOCKS.get(15)); }
    @Test @EnabledIf("drr7Available") void corpus_rm1a_01_underlyingAssetPriceSourceLeg1() throws IOException { lock(RM1A_WHOLE_FILE_LOCKS.get(0)); }
    @Test @EnabledIf("drr7Available") void corpus_rm1a_02_underlyingAssetPriceSourceLeg2() throws IOException { lock(RM1A_WHOLE_FILE_LOCKS.get(1)); }
    @Test @EnabledIf("drr7Available") void corpus_rm1a_03_deliveryTypeFromProduct() throws IOException { lock(RM1A_WHOLE_FILE_LOCKS.get(2)); }

    /** The in-file minimal pair moves SUB-FILE as chartered: the disguised
     *  {@code underlier.UnderlierForProduct -> Observable -> Basket} hop gains the
     *  {@code Type coercion} deref, the {@code Observable} import and the
     *  type-derived {@code observable ->} lambda; the raw {@code Observable ->
     *  Observable.getBasket()} is gone. The numbering rider stays (see
     *  {@link #RM1A_SUBFILE_PIN}). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_rm1a_04_underlyingIdentificationType_inFilePair_movesAsChartered() {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        String out = drr7Output.get(RM1A_SUBFILE_PIN);
        assertNotNull(out, "not generated: " + RM1A_SUBFILE_PIN);
        assertContains(out, "import cdm.observable.asset.Observable;");
        assertContains(out, "MapperS.of(underlierForProduct.evaluate(item.get())).<ReferenceWithMetaObservable>map(\"getObservable\", "
                + "underlier -> underlier.getObservable()).<Observable>map(\"Type coercion\", referenceWithMetaObservable");
        assertContains(out, ".<Basket>map(\"getBasket\", observable -> observable.getBasket())");
        assertNotContains(out, "Observable -> Observable.getBasket()");
        assertNotContains(out, "Product -> Product.");
    }

    /** STAYS-IDENTICAL — the in-corpus GREEN side of the minimal pair: the alias root
     *  {@code economicTerms -> payout -> CommodityPayout -> delivery} was already
     *  {@code commodityPayout} (green PRE and POST). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_getReportableDelivery_staysIdentical() throws IOException {
        lock("drr/regulation/common/emir/functions/GetReportableDelivery.java");
    }

    /**
     * LAW-66 Control 1 at corpus grain — the two-sided instrument's gen side: the 14
     * carrier tokens ({@code X -> X.} for every option/type name the PRE dump emitted
     * PascalCase) appear in NO generated drr 7.0.0 file (golden side: 0 files in all 25
     * cells; PRE gen: 30 files / 92 sites). A literal-token scan, not a structural read.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_noPascalCaseNavLambdaParamAnywhere() {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> tokens = List.of("Product -> Product.", "OptionPayout -> OptionPayout.",
                "Instrument -> Instrument.", "Observable -> Observable.", "Asset -> Asset.",
                "CommodityPayout -> CommodityPayout.", "SettlementPayout -> SettlementPayout.",
                "Basket -> Basket.", "Commodity -> Commodity.", "FloatingRateIndex -> FloatingRateIndex.",
                "InterestRatePayout -> InterestRatePayout.", "PerformancePayout -> PerformancePayout.",
                "FixedPricePayout -> FixedPricePayout.", "FixedRateSpecification -> FixedRateSpecification.");
        List<String> hits = new ArrayList<>();
        for (Map.Entry<String, String> e : drr7Output.entrySet()) {
            for (String t : tokens) {
                if (e.getValue().contains(t)) hits.add(e.getKey() + " :: " + t);
            }
        }
        assertTrue(hits.isEmpty(), "PascalCase nav lambda params remain: " + hits);
    }

    private static void lock(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr7GenErrors.stream()
                .filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        Path goldenPath = DRR7_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr 7.0.0 output must byte-match the golden (newline-normalized) for "
                + path + " (seat 13 — the disguised-chain option hop: lambda name + the option projection's id).");
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        drr7GenErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                ruleGen.generateClasses(model, version, output)
                        .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                dataRuleGen.generateClasses(model, version, output)
                        .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        funcGen.generateWithErrors(output)
                .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
        return output;
    }

    // =========================================================================
    // Harness — one linked workspace (decoy FIRST, then the dependency, then the
    // generated namespace), rendered twice: FILTERED (the dependency namespace loaded
    // but not generated — the corpus condition) and UNFILTERED (b8).
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> filteredOut;
    private static Map<String, String> unfilteredOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel other = AstBuilder.buildFromString(MODEL_OTHER, "seat13-other.rosetta");
            RModel dep = AstBuilder.buildFromString(MODEL_DEP, "seat13-dep.rosetta");
            RModel main = AstBuilder.buildFromString(MODEL_MAIN, "seat13.rosetta");
            other.setVersion("0.0.0.test");
            dep.setVersion("0.0.0.test");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(other); // the decoy registered FIRST (b9)
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
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static String filtered(String fileName) throws IOException {
        if (filteredOut == null) {
            filteredOut = render(m -> "census.seat13".equals(m.namespace()));
        }
        return lookup(filteredOut, fileName);
    }

    private static String unfiltered(String fileName) throws IOException {
        if (unfilteredOut == null) {
            unfilteredOut = render(m -> true);
        }
        return lookup(unfilteredOut, fileName);
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
            if (!Files.isDirectory(root)) continue;
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
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) { failures.add(p + " — " + e); }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[DisguisedChainOptionHopSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle),
                "expected needle missing:\n" + needle + "\n--- in output:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertTrue(!out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
