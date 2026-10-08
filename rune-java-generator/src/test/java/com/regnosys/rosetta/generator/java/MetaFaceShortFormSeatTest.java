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
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
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

/**
 * SEAT 21, lever L+Z — facet {@code metaFaceShortForm}: <b>a feature read whose name is a metadata
 * FACE of the receiver ({@code key}, {@code id}, {@code location}, {@code scheme}, {@code reference},
 * {@code address}) is NOT a POJO getter — upstream links it to a {@code RosettaMetaType} and renders a
 * fixed two-hop short form on the UN-dereferenced receiver
 * ({@code .map("getMeta", a->a.getMeta()).map("get<Name>", a->a.get<PojoProperty>())}, {@code reference}
 * the one-hop {@code .map("getReference", a->a.getExternalReference())}; {@code ExpressionGenerator.xtend
 * :776, 345-350, 515-527}; {@code PojoPropertyUtil.toPojoPropertyName}), a MapperS receiver yielding
 * {@code MapperS<String>} and a MapperC receiver {@code MapperC<String>} with the SAME text, and a bare
 * face name over the implicit item ({@code min [ key ]}) taking the same {@code metaCall}
 * ({@code :1113-1116}).</b>
 *
 * <p><b>The defect.</b> The fork's short-form arm ({@code NavigationHandler.metaFeatureShortFormOrNull})
 * admitted only {@code scheme}/{@code reference}, only a MapperS receiver, only a meta-WRAPPER item; the
 * same two-name list was copied at the deref-suppression gate and the bare-name synthesis, and the
 * property-name table twice more (incomplete). The parser ALREADY binds the face to the exported
 * {@code metaType} ({@code TypeInferenceEngine}, the {@code resolvedFeatureNode} slot) and the
 * generator never read it. drr 7.x {@code filter quantity -> location any = …} (FXSwapLeg1/2, FXLeg1/2)
 * rendered a {@code Type coercion} deref + {@code .getLocation()} on the value type;
 * {@code commodityPayouts -> key exists} rendered {@code .getKey()} on the POJO; the bare {@code key}
 * in {@code min [ key ]} echoed the variable {@code MapperS.of(key)} — none of which compile (LAW 74,
 * 16 repairs). 24 band rows = 16 whole-file heals + 8 partial.
 *
 * <p><b>The seat.</b> Admission = G2 FIRST (a resolved REAL attribute wins — the shadowing law), then
 * the parser's {@code RMetaType} binding ({@code HandlerHelper.boundMetaType}, carried onto the
 * synthesized nav by {@code ReferenceHandler.synthesizeFeatureCall} and the bare-name synth) OR, for
 * {@code scheme}/{@code reference} only, the wrapper proof the arm always had; the MapperS-only gate
 * and the wrapper-only proof are gone; the render is the old {@code scheme} branch generalised through
 * ONE name table ({@code HandlerHelper.metaPojoPropertyName} — written FOUR times before this seat, the
 * complete mirror in {@code ConstructionHandler} found and retired at the seat-21 review); the bare-name
 * synth is hoisted out of {@code itemType == null} and admits on the PARSER's binding (a bare face over a
 * {@code CommodityPayout [metadata key]} item IS bound — the type-level-face leg was redundant and is gone,
 * review SF-4) or — {@code scheme}/{@code reference} only — the #285 implicit-item wrapper proof. The
 * result wrapper follows the receiver's Mapper kind (a MapperC receiver → MapperC<String>; a null-typed
 * receiver → its inferred cardinality; else MapperS).
 *
 * <p><b>LAW 75 — measured before the seat over all 275 matrix rows (the seat-21 runtime probe):</b>
 * 1,435 meta-NAMED navs at the nav seat: every {@code id} (1,033) / {@code address} (121) / real
 * {@code location} (41) read resolves a real attribute (G2); the carriers {@code key} 8 + {@code location}
 * 16 arrive UNRESOLVED with the parser's {@code RMetaType} binding on the disguised origin (16 of the
 * 24 carriers are MapperC-TYPED receivers — FXLeg1/2, FXSwapLeg1/2 — the 8 {@code commodityPayouts ->
 * key} are null-typed and take the cardinality fallback); of the 208 already-right {@code scheme}/
 * {@code reference} sites 153 are MapperS-typed and 55 null-typed (the implicit-item walk) — all 208
 * byte-unchanged, the ring is the receipt (the review's MF-4 corrected "208/208 MapperS"). The shadowed cdm
 * {@code LegacyValuationTimeDayAndTime} {@code item -> location} read (a real attribute the binder
 * left unresolved) carries NO binding — G2 does not cover it, the binding does not admit it, and no
 * name list admits anything on its own ({@code b1} the fixture twin).
 *
 * <p><b>RED at the pre-seat blob</b> ({@code rune-java-generator/src/main} at {@code 9c1bd8ef3}, this suite
 * kept — the COMBINED pre-seat blob, re-measured at the final head after the seat-21 review, MF-1):
 * <b>15 run / 10 F</b> = exactly {@code a1, a2, a3, a4, a5, a6, corpus_c1, corpus_c2, corpus_c3,
 * corpus_control1}; every {@code b*} + {@code control0} GREEN in both states; GREEN 15/15 (default and
 * {@code -Pir-on}). <b>LAW 66/76 mutations</b> (each applied → run → reverted AT THE FINAL HEAD;
 * {@code artefacts/review/rv-mut-lz-*.log}): (i) the admission restored to the two-name list (the parser's
 * binding ignored) → <b>15/10F</b> {a1–a6, c1, c2, c3, control1}; (ii) the MapperS-only receiver gate
 * restored → <b>15/4F</b> {a3, c1, c3, control1} (the typed MapperC receivers — the filter item's
 * {@code qty}; a1's {@code payouts} receiver is null-typed and admits through the cardinality fallback);
 * (iii) the wrapper-only proof restored (a type-level face declined) → <b>15/5F</b> {a1, a2, c2, c3,
 * control1}; (iv) the bare meta-symbol arm deleted → <b>15/4F</b> {a2, c2, c3, control1}; (v) the
 * deref-suppression gate's binding leg dropped → <b>15/1F</b> {a6} — a {@code [metadata location]}
 * FUNCTION-INPUT read has ZERO corpus carriers, so the whole-cell control cannot move under it (a6 is
 * the declared fixture pin); (vi) the name table's default emptied → <b>15/2F</b> {b2, control1} (the
 * cell's 8 scheme sites — and control1 now fails on the COUNTS, "meta-form counts differ from golden in
 * 4 file(s)": the whole-cell control scans the UNION of token-bearing files since the review's MF-5, a
 * missing side counting as all-zero, so the 4 files that LOST their scheme sites are seen; the first
 * cut's intersection scan had been blind to them and only its loose reach floor had caught this
 * mutation). The hoisted bare-name synth in {@code synthesizeImplicitItemBareNav} keeps
 * serving the symbol-EMPTY bare face over a meta-wrapper item (drr Extract_BondConnect's
 * {@code filter scheme = …}, byte-identical); a symbol bound to the metaType takes the earlier arm.
 *
 * <p><b>Declared declines / evidence caps.</b> {@code address} (upstream's own arm cannot compile on
 * either wrapper shape — {@code ReferenceWithMeta} has no {@code getMeta()}, {@code MetaFields} no
 * {@code getReference()}) and {@code template} (no {@code metaType template} is declared in any cell,
 * so no binding ever admits it — {@code b4}) have zero corpus carriers; {@code id} ({@code a5}) is
 * emitted for fidelity with zero corpus carriers. The setter-path name-table consumers
 * ({@code ConstructionHandler}, {@code FunctionExpressionRenderer}) read the ONE table but keep
 * their admission set (key/id/scheme) — zero assignment-segment carriers for the other faces.
 */
class MetaFaceShortFormSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /**
     * The fixture declares the six exported {@code metaType}s exactly as CDM's {@code base-desc.rosetta}
     * does (id, key, reference, scheme, address, location — NO template), so the parser's binding is
     * the real one; a type-level {@code [metadata key]} POJO (the CommodityPayout shape), attribute-level
     * {@code [metadata location]} on a multi and a single attribute (the PriceQuantity.quantity shape),
     * a real attribute NAMED location (the shadowing twin), and the scheme/reference byte-locks.
     */
    private static final String MODEL = """
            namespace census.seat21lz
            version "1.0.0"

            metaType id string
            metaType key string
            metaType reference string
            metaType scheme string
            metaType address string
            metaType location string

            type Payout: <"a1/a2 - a TYPE-level [metadata key] POJO (CommodityPayout) — a bare GlobalKey, not a wrapper">
                [metadata key]
                amount number (0..1)

            type Amount:
                value number (0..1)

            type Holder:
                qty Amount (0..*) <"a3 - attribute-level [metadata location] on a MULTI attribute (PriceQuantity.quantity)">
                    [metadata location]
                one Amount (0..1) <"a4 - the single twin">
                    [metadata location]
                ident string (0..1) <"a5 - [metadata id]">
                    [metadata id]

            type Loc:
                code string (0..1)

            type Place: <"b1 - a REAL attribute NAMED location (AssetDeliveryInformation.location): the getter stays">
                location Loc (0..*)

            type Tagged:
                code string (0..1)
                    [metadata scheme]
                ref Amount (0..1)
                    [metadata reference]

            func F1KeyExplicit: <"a1 - type-level key, explicit chain, MapperC receiver (commodityPayouts -> key exists)">
                inputs:
                    payouts Payout (0..*)
                output:
                    result boolean (1..1)
                set result:
                    payouts -> key exists

            func F2KeyBareMin: <"a2 - the BARE face name inside a min lambda (commodityPayouts min [ key ])">
                inputs:
                    payouts Payout (0..*)
                output:
                    result Payout (0..1)
                set result:
                    payouts min [ key ]

            func F3LocationMultiFilter: <"a3 - attribute-level location on a multi wrapper inside a filter lambda (FXSwapLeg1 tlSwap1)">
                inputs:
                    hs Holder (0..*)
                    k string (1..1)
                output:
                    result Holder (0..*)
                set result:
                    hs filter qty -> location any = k

            func F4LocationSingle: <"a4 - attribute-level location on a SINGLE wrapper, MapperS receiver">
                inputs:
                    h Holder (1..1)
                output:
                    result string (0..1)
                set result:
                    h -> one -> location

            func F5Id: <"a5 - id on a [metadata id] attribute (corpus-unexercised, upstream fidelity)">
                inputs:
                    h Holder (1..1)
                output:
                    result string (0..1)
                set result:
                    h -> ident -> id

            func F6FnInputLocation: <"a6 - a [metadata location] FUNCTION INPUT read: the deref-suppression gate keeps the wrapper">
                inputs:
                    q Amount (1..1)
                        [metadata location]
                output:
                    result string (0..1)
                set result:
                    q -> location

            func B1RealAttrNamedLocation: <"b1 - the shadowing law: a REAL attribute named location keeps the getter">
                inputs:
                    p Place (1..1)
                output:
                    result Loc (0..*)
                add result:
                    p -> location

            func B2Scheme: <"b2 - the scheme byte-lock (the branch of old, generalised)">
                inputs:
                    t Tagged (1..1)
                output:
                    result string (0..1)
                set result:
                    t -> code -> scheme

            func B3Reference: <"b3 - the reference one-hop byte-lock">
                inputs:
                    t Tagged (1..1)
                output:
                    result string (0..1)
                set result:
                    t -> ref -> reference

            func B4Template: <"b4 - template: no metaType is declared (as in every cell) — no binding, no short form">
                inputs:
                    t Tagged (1..1)
                output:
                    result string (0..1)
                set result:
                    t -> code -> template
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    private static final String KEY_FORM = ".map(\"getMeta\", a->a.getMeta()).map(\"getKey\", a->a.getExternalKey())";
    private static final String LOCATION_FORM = ".map(\"getMeta\", a->a.getMeta()).map(\"getLocation\", a->a.getScopedKey())";
    private static final String ID_FORM = ".map(\"getMeta\", a->a.getMeta()).map(\"getId\", a->a.getExternalKey())";
    private static final String SCHEME_FORM = ".map(\"getMeta\", a->a.getMeta()).map(\"getScheme\", a->a.getScheme())";
    private static final String REFERENCE_FORM = ".map(\"getReference\", a->a.getExternalReference())";

    /** a1 — type-level key, explicit chain, MapperC receiver: the two-hop form on the un-dereferenced receiver. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_typeLevelKeyExplicitChainOnMapperC() throws IOException {
        String out = function("F1KeyExplicit.java");
        assertContains(out, "MapperC.<Payout>of(payouts)" + KEY_FORM);
        assertNotContains(out, "getKey())");
    }

    /** a2 — the BARE face name inside a min lambda: the implicit item takes the same short form. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_bareKeyInsideMinLambda() throws IOException {
        String out = function("F2KeyBareMin.java");
        assertContains(out, ".min(item -> item" + KEY_FORM + ")");
        assertNotContains(out, "MapperS.of(key)");
    }

    /** a3 — attribute-level location on a MULTI wrapper inside a filter: no `Type coercion` deref, no value getter. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_locationOnMultiWrapperInsideFilter() throws IOException {
        String out = function("F3LocationMultiFilter.java");
        assertContains(out, ".<FieldWithMetaAmount>mapC(\"getQty\", holder -> holder.getQty())" + LOCATION_FORM);
        assertNotContains(out, "Type coercion");
        assertNotContains(out, "getLocation())");
    }

    /** a4 — the single twin: a MapperS receiver, the same text. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_locationOnSingleWrapperMapperS() throws IOException {
        String out = function("F4LocationSingle.java");
        assertContains(out, "MapperS.of(h).<FieldWithMetaAmount>map(\"getOne\", holder -> holder.getOne())" + LOCATION_FORM);
        assertNotContains(out, "Type coercion");
        assertNotContains(out, "getLocation())");
    }

    /** a5 — id on a [metadata id] attribute: the table maps id → externalKey. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_idOnMetaIdAttribute() throws IOException {
        String out = function("F5Id.java");
        assertContains(out, ".<FieldWithMetaString>map(\"getIdent\", holder -> holder.getIdent())" + ID_FORM);
        assertNotContains(out, "getId())");
    }

    /** a6 — a [metadata location] FUNCTION INPUT: the deref-suppression gate keeps the raw wrapper, the short form follows. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_metaLocationFunctionInputKeepsTheWrapper() throws IOException {
        String out = function("F6FnInputLocation.java");
        assertContains(out, "MapperS.of(q)" + LOCATION_FORM);
        assertNotContains(out, "q.getValue()");
        assertNotContains(out, "getLocation())");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — the shadowing law: a REAL attribute named location keeps the getter (G2 first). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_realAttributeNamedLocationKeepsTheGetter() throws IOException {
        String out = function("B1RealAttrNamedLocation.java");
        assertContains(out, "MapperS.of(p).<Loc>mapC(\"getLocation\", place -> place.getLocation())");
        assertNotContains(out, "getMeta");
    }

    /** b2 — the scheme byte-lock: the generalised branch reduces to the old literal. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_schemeByteLock() throws IOException {
        String out = function("B2Scheme.java");
        assertContains(out, ".<FieldWithMetaString>map(\"getCode\", tagged -> tagged.getCode())" + SCHEME_FORM);
    }

    /** b3 — the reference one-hop byte-lock (no getMeta hop). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_referenceOneHopByteLock() throws IOException {
        String out = function("B3Reference.java");
        assertContains(out, ".<ReferenceWithMetaAmount>map(\"getRef\", tagged -> tagged.getRef())" + REFERENCE_FORM);
        assertNotContains(out, "getMeta()).map(\"getReference\"");
    }

    /** b4 — template: no metaType declared → no binding → no short form (the name alone admits nothing). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_templateWithoutAMetaTypeDeclarationIsNotAShortForm() throws IOException {
        String out = function("B4Template.java");
        assertNotContains(out, "getMeta()).map(\"getTemplate\"");
    }

    // =========================================================================
    // Part C — the corpus (drr 7.0.0 FUNCTION: the carriers + the whole-cell control)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), MetaFaceShortFormSeatTest.class);
    }

    private static final List<String> DRR7_WHOLE_FILE_CARRIERS = List.of(
            "drr/regulation/common/functions/FXSwapLeg1.java",
            "drr/regulation/common/functions/FXSwapLeg2.java",
            "drr/regulation/common/functions/CommodityCommodityLeg1.java",
            "drr/regulation/common/functions/CommodityCommodityLeg2.java");

    /** The 2 PARTIAL carriers (12-line rows; the other defects are other levers) — the meta LINE is locked. */
    private static final List<String> DRR7_PARTIAL_CARRIERS = List.of(
            "drr/regulation/common/functions/FXLeg1.java",
            "drr/regulation/common/functions/FXLeg2.java");

    /** c1 — the `-> location` carrier whole-file lock (FXSwapLeg1). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_fxSwapLeg1ByteIdentical() throws IOException {
        lock(DRR7_WHOLE_FILE_CARRIERS.get(0));
    }

    /** c2 — the `-> key` + bare `min [ key ]` carrier whole-file lock (CommodityCommodityLeg1). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_commodityCommodityLeg1ByteIdentical() throws IOException {
        lock(DRR7_WHOLE_FILE_CARRIERS.get(2));
    }

    /** c3 — ALL four whole-file carriers byte-identical + the two partial carriers carry the location form and none of the wrong forms. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c3_allCarriersHealedOrLineLocked() throws IOException {
        for (String path : DRR7_WHOLE_FILE_CARRIERS) {
            lock(path);
        }
        for (String path : DRR7_PARTIAL_CARRIERS) {
            String generated = drr7Output.get(path);
            assertNotNull(generated, "not generated: " + path);
            assertContains(generated, LOCATION_FORM);
            assertNotContains(generated, "nonNegativeQuantitySchedule.getLocation()");
        }
    }

    /**
     * control0 — golden is the oracle (the frozen drr 7.0.0 tree): the meta-form populations at their
     * exact seat-21 census counts (getKey 4, getLocation 4, getScheme 8, getReference 7 — the cell's
     * own + its transitive-CDM output), ZERO by-name `getKey()`/`getScopedKey`-less location getters
     * on the carrier receivers, and ZERO bare `MapperS.of(key)` echoes.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenDrr7IsTheOracle() throws IOException {
        Map<String, String> golden = readGoldenTree(DRR7_GOLDEN);
        MetaScan g = scan(golden);
        assertEquals(4, g.sites(KEY_FORM), "golden getKey meta-form sites");
        assertEquals(4, g.sites(LOCATION_FORM), "golden getLocation meta-form sites");
        assertEquals(8, g.sites(SCHEME_FORM), "golden getScheme meta-form sites");
        assertEquals(7, g.sites(REFERENCE_FORM), "golden getReference meta-form sites");
        assertEquals(0, g.sites("MapperS.of(key)"), "golden carries no bare key echo");
        assertEquals(0, g.sites("commodityPayout.getKey())"), "golden carries no by-name getKey getter");
        assertEquals(0, g.sites("nonNegativeQuantitySchedule.getLocation()"), "golden carries no value-type getLocation getter");
        // the union domain control1 compares over: 13 golden files carry at least one of the seven
        // tokens (KEY 2 + LOCATION 4 + SCHEME 4 + REFERENCE 3 files) — a census pin that moves must
        // move in the same commit
        assertEquals(13, g.perFile.size(), "golden meta-form-bearing files (the whole-cell control's domain)");
        for (String carrier : DRR7_WHOLE_FILE_CARRIERS) {
            assertTrue(g.perFile.containsKey(carrier), "golden carrier carries a meta form: " + carrier);
        }
    }

    /**
     * control1 — the FORK's WHOLE generated drr 7.0.0 cell (every kind, LAW 72): over the UNION of the
     * files either tree carries one of the seven tokens in (13 = control0's KEY 2 + LOCATION 4 +
     * SCHEME 4 + REFERENCE 3 files — a missing side counts as all-zero), the seven token counts agree
     * FILE BY FILE, so a meta form appearing in a file golden has none in (an over-fire: a real getter
     * turned meta form) or disappearing from a file's last site (an under-fire: the mutation-(vi)
     * shape, which the first cut's INTERSECTION scan could not see and only a loose reach floor caught)
     * both fail here; and the carriers are REACHED. (Seat-21 review MF-5.)
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_forkDrr7WholeCellMetaFormCountsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        Map<String, String> golden = readGoldenTree(DRR7_GOLDEN);
        MetaScan f = scan(drr7Output);
        MetaScan g = scan(golden);
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.LinkedHashSet<>(f.perFile.keySet());
        universe.addAll(g.perFile.keySet());
        int[] zero = new int[TOKENS.length];
        int compared = 0;
        for (String key : universe) {
            int[] fc = f.perFile.getOrDefault(key, zero);
            int[] gc = g.perFile.getOrDefault(key, zero);
            compared++;
            if (!java.util.Arrays.equals(gc, fc)) {
                mismatched.add(key + " fork=" + java.util.Arrays.toString(fc)
                        + " golden=" + java.util.Arrays.toString(gc));
            }
        }
        // the per-file diff first (the actionable diagnostic), then the domain pin: the union domain must
        // EQUAL golden's meta-form-bearing file count (control0's 13) — an over-fire into a golden-tokenless
        // file would raise it (and show above as a mismatch), an under-fire that empties a file lowers it
        assertEquals(List.of(), mismatched, "meta-form counts differ from golden in " + mismatched.size() + " file(s)");
        assertEquals(13, compared, "the union domain must equal golden's 13 meta-bearing files (an over-fire raises it, an under-fire lowers it)");
        for (String carrier : DRR7_WHOLE_FILE_CARRIERS) {
            assertTrue(f.perFile.containsKey(carrier), "carrier REACHED (carries a meta form): " + carrier);
        }
        assertEquals(0, f.sites("MapperS.of(key)"), "the fork carries no bare key echo anywhere in the cell");
    }

    // =========================================================================
    // The scan
    // =========================================================================

    private static final String[] TOKENS = {
        KEY_FORM, LOCATION_FORM, SCHEME_FORM, REFERENCE_FORM,
        "MapperS.of(key)", "commodityPayout.getKey())", "nonNegativeQuantitySchedule.getLocation()" };

    private static final class MetaScan {
        final Map<String, int[]> perFile = new LinkedHashMap<>();

        int sites(String token) {
            int idx = indexOf(token), n = 0;
            for (int[] c : perFile.values()) n += c[idx];
            return n;
        }

        private static int indexOf(String token) {
            for (int i = 0; i < TOKENS.length; i++) if (TOKENS[i].equals(token)) return i;
            throw new IllegalArgumentException(token);
        }
    }

    private static MetaScan scan(Map<String, String> tree) {
        MetaScan r = new MetaScan();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int[] counts = new int[TOKENS.length];
            boolean any = false;
            for (int i = 0; i < TOKENS.length; i++) {
                counts[i] = countOccurrences(code, TOKENS[i]);
                any |= counts[i] > 0;
            }
            if (any) {
                r.perFile.put(e.getKey(), counts);
            }
        }
        return r;
    }

    private static Map<String, String> readGoldenTree(Path dir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) stream.filter(q -> q.toString().endsWith(".java"))::iterator) {
                out.put(dir.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    private static String codeOnly(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("/*", i)) {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else if (s.startsWith("//", i)) {
                int end = s.indexOf('\n', i);
                i = end < 0 ? s.length() : end;
            } else if (s.charAt(i) == '"') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != '"') {
                    if (s.charAt(j) == '\\') j++;
                    j++;
                }
                sb.append(s, i, Math.min(j + 1, s.length()));
                i = j + 1;
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0, i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    // =========================================================================
    // Corpus generation (the same harness every seat suite uses)
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors = new ArrayList<>();

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr7Available()) {
            List<String> errs = new ArrayList<>();
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
            drr7GenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
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
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, pojoGen.generateClasses(model, version, output));
                collect(errors, choiceGen.generateClasses(model, version, output));
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lock(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr7GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = DRR7_GOLDEN.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 21 L+Z: the meta-face short form.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat21lz.rosetta");
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
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
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
            fixtureOut = render(m -> "census.seat21lz".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String function(String fileName) throws IOException {
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
            throw new AssertionError("[MetaFaceShortFormSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
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
        assertFalse(out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
