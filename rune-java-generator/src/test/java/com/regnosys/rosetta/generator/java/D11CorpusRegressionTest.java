package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.testutil.CorpusCells;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathUtilGenerator;
import com.regnosys.rosetta.generator.java.object.validators.CardinalityValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-matrix D11 corpus regression test.
 *
 * <p>Walks 5 pinned cells (CDM × 2, DRR × 1, ISO-20022 × 1, rune-fpml × 1), all at
 * uniform rune-dsl 9.83.0 (PR #85 rebaseline). Per cell, runs 9 element-kind passes
 * (POJO / enum / metafield / function, plus the PR #405 coverage-wave-A kinds:
 * only-exists validators / cardinality validators / package-info, plus the PR #407
 * coverage-wave-B kinds: type-format validators / XMeta registries), compares
 * generator output to the cell's goldens, and asserts strict equality.
 *
 * <p>Per-corpus golden conventions (D25):
 * <ul>
 *   <li>CDM, DRR, rune-fpml — {@code <cell>/rosetta-source/src/generated/java/}</li>
 *   <li>ISO-20022 — {@code <cell>/rosetta-source/target/classes/generated/java/}
 *       (Xtext outputConfiguration in upstream pom L459-466; outputDirectory L461-462)</li>
 * </ul>
 *
 * <p>Per-corpus generator version stamp (matches each cell's frozen-at-clone-time output;
 * applied via {@link CellSpec#versionStamp()}):
 * <ul>
 *   <li>CDM, DRR, rune-fpml — {@code 0.0.0.master-SNAPSHOT} for POJO/enum/function output
 *       (DRR was assumed {@code 0.0.0} pre-R2 from a metafield-wrapper sample misdirection;
 *       see {@link CellSpec#versionStamp()} Javadoc for the full investigation)</li>
 *   <li>ISO-20022 — literal {@code ${project.version}} (Xtext built without Maven filtering)</li>
 * </ul>
 * Metafield wrappers are hardcoded {@code version="0.0.0"} in the ST templates
 * {@code java-field-with-meta.stg} / {@code java-reference-with-meta.stg} for ALL
 * corpora regardless of cell — universal wrapper convention, not a per-corpus stamp.
 * (The literal lives in the templates rather than {@code MetaFieldGenerator.java}; a
 * future reader grepping the Java source for {@code 0.0.0} should look in the .stg files.)
 *
 * <p>Scope filter via system properties (CI per-cell dispatch):
 * <ul>
 *   <li>{@code -Dd11.corpus=<corpus>} — restrict to one corpus tag</li>
 *   <li>{@code -Dd11.version=<version>} — restrict to one version tag within the corpus</li>
 *   <li>{@code -Dd11.dump-paths=true} — print full unwaived
 *       mismatches/noGolden/missingOutput file paths to stdout before the
 *       strict-equality assertion fires. Default off. Use for active
 *       investigation when the assertion message's sample-context (first
 *       mismatch + first missingOutput) is insufficient — particularly for
 *       pure-noGolden failures, where the assertion message exposes only
 *       counts. See
 *       {@code docs/bc-verification.md} Layer 4 § Recovery.</li>
 *   <li>{@code -Dd11.dump-content=<N>} — print full golden + generated content
 *       for the first {@code N} unwaived mismatches before the strict-equality
 *       assertion fires. Default 0 (no-op). Use for laser-focused single-file
 *       diff investigation (e.g. {@code -Dd11.dump-content=1} for first
 *       mismatch only) or sampled cluster-pattern diagnosis (e.g.
 *       {@code -Dd11.dump-content=3} across the leading samples). Complements
 *       {@code -Dd11.dump-paths} which surfaces only file names + first-diff
 *       line numbers. Added at P2.1.1 T3.1 for Cluster A residual investigation.</li>
 *   <li>{@code -Dd11.dump-gen-errors=true} — print every rule-family
 *       {@link GenerationException} (waivered + unwaivered) with its target
 *       waiver-key path and error message. Symmetric with {@code -Dd11.dump-paths}
 *       but for the gen-error bucket. Useful for engine-phase burndown auditing
 *       and for constructing waiver entries when retargeting to a new rune-dsl
 *       version. Added at PR #85 T1.5 (9.83.0 rebaseline).</li>
 * </ul>
 *
 * <p>Per-(cell, kind) divergence waivers live in {@link #KNOWN_DIVERGENT}, keyed by
 * {@code "<corpus>/<version>/<KIND>"} (Freezing pattern). Loaded from the
 * classpath resource {@code /d11-known-divergent.txt} at static init; per-cluster
 * investigation evidence at {@code docs/reviews/p1.7-pr1.6-cluster-investigation.md}.
 */
class D11CorpusRegressionTest {

    private static final Path TEST_CORPUS_ROOT = Path.of("../test-corpus");

    /**
     * Builtin search roots in priority order. Mirrors {@code BuiltinParseTest}'s
     * {@code SEARCH_ROOTS} pattern (rune-parser-side):
     * <ol>
     *   <li>{@code ../test-corpus/rune-dsl-builtins/...} — preferred; full corpus clone</li>
     *   <li>{@code ../rune-dsl/rune-runtime/...} — sibling rune-dsl source clone fallback</li>
     * </ol>
     * Either location supplies the same builtin {@code .rosetta} files (annotations,
     * basictypes, etc.). Per Copilot R7 F1 — without a fallback, environments with only
     * the sibling rune-dsl checkout would skip builtin loading silently and then surface
     * the absence as misleading per-cell parse-failure noise (cell {@code .rosetta} files
     * reference builtin types — {@code string}, {@code int}, etc. — that wouldn't resolve).
     */
    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            TEST_CORPUS_ROOT.resolve("rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            Path.of("../rune-dsl/rune-runtime/src/main/resources/model")
    );

    private static final String SCOPE_CORPUS = System.getProperty("d11.corpus", "").trim();
    private static final String SCOPE_VERSION = System.getProperty("d11.version", "").trim();

    // P2.1.1 T3.1 — opt-in diagnostic budget (-Dd11.dump-content=N; default 0).
    // Read once at class load; used by both Mismatch population (Pass 1) and the
    // dump section in assertCellKind. When 0 (default), Mismatch content fields
    // stay null so default-mode runs don't retain duplicate file content in heap.
    private static final int DUMP_CONTENT_MAX = Integer.getInteger("d11.dump-content", 0);
    // -Dd11.dump-waivered-dir=<ABSOLUTE path> — for every divergent (byte-mismatch) file, write BOTH
    // sides of the f-probe corpus the regscan diffs PRE vs POST to catch within-waiver churn
    // (toward / away / flipped): the GENERATED content to <dir>/<cell>_<kind>/gen/<flat~path>.java
    // AND the GOLDEN content to <dir>/<cell>_<kind>/golden/<flat~path>.java (both sides are dumped so
    // the regscan can recompute the gen-vs-golden line ratio at each end). Retaining content implies
    // CAPTURE_CONTENT. MUST be an ABSOLUTE path — the property resolves MODULE-relative under
    // `mvn -f rune-java-generator/pom.xml` (a repo-relative value would land at
    // rune-java-generator/rune-java-generator/target/…), so a relative value is REJECTED at class load
    // (the static block below). Opt-in; default no-op. v3.2 seat 9 (F13): the same flag also writes
    // the FORK's content of every noGolden row to <dir>/<cell>_<kind>/nogolden/<flat~path> — a
    // golden-free emission has no golden side to diff, so the waivered dump alone never showed it
    // (dumpNoGoldenIfRequested).
    private static final String DUMP_WAIVERED_DIR =
            System.getProperty("d11.dump-waivered-dir", "").trim();
    static {
        if (!DUMP_WAIVERED_DIR.isEmpty() && !Path.of(DUMP_WAIVERED_DIR).isAbsolute()) {
            throw new IllegalArgumentException(
                    "-Dd11.dump-waivered-dir must be an ABSOLUTE path (got '" + DUMP_WAIVERED_DIR
                    + "'); a relative value resolves MODULE-relative under `mvn -f rune-java-generator/pom.xml` "
                    + "and would silently land at rune-java-generator/rune-java-generator/target/….");
        }
    }
    private static final boolean CAPTURE_CONTENT = DUMP_CONTENT_MAX > 0 || !DUMP_WAIVERED_DIR.isEmpty();

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    /**
     * The self-certifying IR-route header (the #467 Seat-1 OBS-1): every D11 receipt log opens by
     * naming the route actually in force, read off the SAME seam the generation calls read — "ON"
     * only when the flag is set AND a provider resolved. The flag-on-no-provider case prints its own
     * distinct line (the seam's contract proceeds on Path-1 there after the warn-once), so a receipt
     * can never be misread as IR-routed when the run was Path-1.
     */
    @BeforeAll
    static void printIrRouteHeader() {
        // v3.2 seat 7 round 2 (the spec / rule-6 reviews' MF-1): the register is JVM-global and surefire reuses a
        // fork across classes, so a class that ran BEFORE this one in the same fork can leave its counts behind and
        // the FIRST LOUD line of the walk would report them as the first cell's (the s7c chain's gensuite half
        // printed TYPE_SWITCH_TERNARY_STUB=2 at cdm/5.38.0 DEEP_PATH_UTIL - AliasSwitchBareCaseNavSeatTest's FloatCalc
        // fixture; an instrument artefact, no emission moved). Reset at class start: every LOUD line reads this
        // walk's own activity alone.
        SilentDegradation.reset();
        var provider = IRGeneration.providerOrNull();
        String route;
        if (provider != null) {
            route = "ON (provider " + provider.getClass().getName() + ")";
        } else if (IRGeneration.enabled()) {
            route = "OFF (flag -D" + IRGeneration.PROPERTY
                    + "=true set but NO provider on the classpath — Path-1)";
        } else {
            route = "OFF";
        }
        System.out.println("D11 IR route: " + route);
    }

    /**
     * Cell specification: corpus tag, version tag, on-disk root.
     *
     * <p>PR #85 rebaseline: ALL_CELLS contains 5 entries at uniform rune-dsl 9.83.0.
     * Cells without goldens on disk skip via {@link Assumptions#assumeTrue}.
     */
    record CellSpec(String corpus, String version, Path root) {
        @Override
        public String toString() { return corpus + "/" + version; }

        /**
         * Per-corpus generator version stamp baked into POJO/enum/function goldens at
         * cell clone time. Verified 2026-05-03 (R1 baseline + R2 fix) by sampling each
         * corpus's `version="..."` annotation in `@RosettaDataType`/`@RuneDataType` +
         * `@version` JavaDoc:
         * <ul>
         *   <li>CDM, DRR, rune-fpml — {@code 0.0.0.master-SNAPSHOT} for POJO/enum.
         *       (DRR was assumed {@code 0.0.0} pre-R2 from a metafield-wrapper sample;
         *       metafield wrappers ARE hardcoded {@code version="0.0.0"} in the ST templates
         *       {@code java-field-with-meta.stg} / {@code java-reference-with-meta.stg}
         *       regardless of cell, so the wrapper sample misled. Regular POJO/enum
         *       uses the cell's `project.version` Maven-substituted value, which DRR
         *       upstream sets to {@code 0.0.0.master-SNAPSHOT} same as CDM.)</li>
         *   <li>ISO-20022 — literal {@code ${project.version}} (Xtext built without
         *       Maven filtering; goldens carry the unsubstituted property reference).</li>
         * </ul>
         */
        String versionStamp() {
            return switch (corpus) {
                case "iso20022" -> "${project.version}";
                // The chaos cell's goldens were pinned by the holdout-style scaffold
                // (scripts/chaos-expander/pin-chaos-goldens.sh): NO Maven filtering,
                // so each declared model version passes through VERBATIM — and the
                // declarations are NOT uniform (measured at the first D11 chaos
                // probe, 2026-09-02: 562 sources declare "1.0.0", 22 declare
                // "1.0.0-SNAPSHOT", and the goldens carry the split — 1,912 vs 82
                // version annotations). A constant stamp here manufactured 82
                // version-line mismatches; null = applyVersionStamp leaves the
                // declared version untouched. Locked by D11VersionStampTest.
                case "chaos" -> null;
                default -> "0.0.0.master-SNAPSHOT";
            };
        }
    }

    /**
     * Applies the cell's {@link CellSpec#versionStamp()} to one of the cell's OWN models,
     * emulating the golden build's Maven resource filtering. (Dependency models — the
     * transitive CDM / rune-fpml closures — are never stamped; see their call sites.)
     *
     * <p><b>N4 (v3.1 phase C, C0 item 4) — only models that DECLARE a version are
     * stamped.</b> Maven resource filtering rewrites {@code ${project.version}} where it
     * is written; it cannot substitute an absent declaration. Band-wide exactly two
     * model files declare no version (drr 5.61.0's {@code regulation-techsprint-g20-mas-
     * rule.rosetta} and {@code -type.rosetta}), and exactly the four goldens generated
     * from them carry {@code @version 0.0.0} — the generator default — against the
     * cell's other 2,107 at {@code 0.0.0.master-SNAPSHOT}. Stamping them unconditionally
     * manufactured those four byte mismatches (root-cause audit § N4). The other 4,060
     * band files declare a version and are stamped exactly as before, so no gated cell
     * can move. Locked by {@link D11VersionStampTest}.
     */
    static void applyVersionStamp(RModel model, CellSpec cell) {
        // A null stamp means the cell's golden build ran NO filtering (chaos —
        // pin-chaos-goldens.sh invokes the mojo directly): the declared version
        // reaches the output verbatim, so emulation is a no-op either way.
        if (model.version().isPresent() && cell.versionStamp() != null) {
            model.setVersion(cell.versionStamp());
        }
    }

    /**
     * PR #413 — per-cell golden-build INPUT-ORDER pins (the versionStamp() precedent:
     * a recorded truth about the cell's historical build inputs, replayed as loader
     * data — never a generator hack).
     *
     * <p><b>The upstream law (vendored-source-proven):</b> upstream
     * {@code JavaPackageInfoGenerator} collects {@code (namespace → definition)} into a
     * Guava {@code LinkedHashMultimap} — iteration = INSERTION order, exact duplicate
     * {@code (key, value)} pairs dropped — fed from
     * {@code resource.getResourceSet().getResources()} in LOAD order
     * ({@code RosettaGenerator.java} L237–242). The maven mojo's load order is Xtext
     * {@code StandaloneBuilder} → {@code PathTraverser} → {@code File.listFiles()} =
     * the build machine's readdir order, explicitly unordered per javadoc. A namespace's
     * package-info description order is therefore a FROZEN ARTIFACT of the FINOS build
     * machine's filesystem enumeration at golden-creation time.
     *
     * <p><b>The decode (all four re-derived from bytes, 2026-07-16):</b> each divergent
     * namespace's files carry near-identical definitions differing only in trailing
     * punctuation — the fingerprints map golden blocks to source files unambiguously:
     * <ul>
     *   <li>cdm5+cdm6 {@code cdm.observable.asset}: golden = func/type ("Observable
     *       concepts applicable…") → enum ("Observable asset concepts…"). func and type
     *       carry IDENTICAL text (the multimap dedups them — the golden shows two blocks
     *       for three files), so their relative order is unobservable; func-before-type
     *       is chosen to match the decodable cells' func-first pattern.</li>
     *   <li>cdm5 {@code cdm.observable.asset.calculatedrate}: golden = func ("…compound
     *       rate.") → enum ("…compound rates") → type ("Floating amount…").</li>
     *   <li>cdm6 {@code cdm.product.asset.floatingrate}: golden = func ("…definitions .")
     *       → type ("…definitions.") → enum ("…definitions␣" — trailing space).</li>
     * </ul>
     * The sibling namespaces prove the default: cdm5 floatingrate and cdm6
     * calculatedrate goldens are byte-identical to the alphabetical walk (same texts,
     * different build = different enumeration), as are the other 145 package-info
     * goldens corpus-wide. No single permutation fits — the order is per-build, which
     * is exactly the reproducible-builds defect reported upstream (the FINOS issue
     * drafted in the waiver-file header at PR #405, evidence strengthened here).
     *
     * <p><b>Replay semantics:</b> within the cell's canonical full-path-sorted walk,
     * the positions occupied by each pin group's members are rewritten so the members
     * appear in golden order; every other file keeps its sorted position.
     *
     * <p>⚠️ <b>TWO kinds are order-sensitive, not one (v3.1 C0 item 3 / audit N1).</b>
     * This javadoc previously claimed package-info was "the single order-sensitive
     * kind". It is not: <b>XMETA</b> emits its {@code Qualify_*} registration list in
     * walk order too, and four CDM cells (6.20.2 / 6.20.5 / 6.22.0 / 6.23.0) enumerate
     * {@code event-qualification-func.rosetta} before {@code event-common-func.rosetta},
     * moving an 11-entry block inside {@code BusinessEventMeta.java}. Both kinds are
     * served by THIS one mechanism — the walk is the shared input — so no second
     * channel is needed; the pin data simply has to cover both file sets. Every other
     * kind is per-element/content-keyed and cannot move. The claim that the rewrite
     * cannot disturb other output is now enforced mechanically by the full-matrix
     * byte gate (the 145 clean package-info files must stay identical).
     *
     * <p>Fail-loud: a pin group member missing from the walk (corpus bump, rename)
     * throws immediately rather than silently reverting to alphabetical order.
     */
    // Map.ofEntries, not Map.of: the band expansion took this to ten cells, which is
    // exactly Map.of's pair ceiling — the next cell would not have compiled (v3.1 C0).
    private static final Map<String, List<List<String>>> GOLDEN_INPUT_ORDER_PINS = Map.ofEntries(
            Map.entry("cdm/5.38.0", List.of(
                    List.of("observable-asset-func.rosetta",
                            "observable-asset-type.rosetta",
                            "observable-asset-enum.rosetta"),
                    List.of("observable-asset-calculatedrate-func.rosetta",
                            "observable-asset-calculatedrate-enum.rosetta",
                            "observable-asset-calculatedrate-type.rosetta"))),
            Map.entry("cdm/6.20.6", List.of(
                    List.of("observable-asset-func.rosetta",
                            "observable-asset-type.rosetta",
                            "observable-asset-enum.rosetta"),
                    List.of("product-asset-floatingrate-func.rosetta",
                            "product-asset-floatingrate-type.rosetta",
                            "product-asset-floatingrate-enum.rosetta"))),
            // ---- v3.1 C0 item 3 (N1): the eight cells the band expansion added.
            // Decoded by BYTE ORACLE, not by hand — GoldenInputOrderPinTest feeds each
            // candidate walk through the real package-info template and keeps the order
            // that reproduces the committed golden. Every group below is one namespace
            // whose files carry DIFFERENT definitions, so its build machine's readdir
            // order is observable in the golden; namespaces whose files agree, or whose
            // canonical walk already matches, are absent by construction.
            Map.entry("cdm/5.39.0", List.of(
                    List.of("observable-asset-calculatedrate-func.rosetta",
                            "observable-asset-calculatedrate-enum.rosetta",
                            "observable-asset-calculatedrate-type.rosetta"),
                    List.of("product-asset-floatingrate-type.rosetta",
                            "product-asset-floatingrate-func.rosetta",
                            "product-asset-floatingrate-enum.rosetta"))),
            Map.entry("cdm/6.20.2", List.of(
                    List.of("product-asset-floatingrate-type.rosetta",
                            "product-asset-floatingrate-func.rosetta",
                            "product-asset-floatingrate-enum.rosetta"),
                    // XMETA (BusinessEventMeta's Qualify_* registration list) — the SECOND
                    // order-sensitive kind. Same walk-order mechanism, different file set:
                    // event-qualification-func (26 Qualify_ funcs) enumerated BEFORE
                    // event-common-func (11) on this cell's build machine. Package-info is
                    // untouched by the swap — all three cdm.event.common files carry one
                    // verbatim definition, so the multimap dedups them and their relative
                    // order is unobservable there.
                    List.of("event-qualification-func.rosetta",
                            "event-common-func.rosetta"))),
            Map.entry("cdm/6.20.3", List.of(
                    List.of("observable-asset-calculatedrate-type.rosetta",
                            "observable-asset-calculatedrate-enum.rosetta",
                            "observable-asset-calculatedrate-func.rosetta"),
                    List.of("product-asset-floatingrate-func.rosetta",
                            "product-asset-floatingrate-enum.rosetta",
                            "product-asset-floatingrate-type.rosetta"))),
            Map.entry("cdm/6.20.4", List.of(
                    List.of("observable-asset-calculatedrate-enum.rosetta",
                            "observable-asset-calculatedrate-type.rosetta",
                            "observable-asset-calculatedrate-func.rosetta"),
                    List.of("product-asset-floatingrate-enum.rosetta",
                            "product-asset-floatingrate-type.rosetta",
                            "product-asset-floatingrate-func.rosetta"))),
            Map.entry("cdm/6.20.5", List.of(
                    List.of("observable-asset-calculatedrate-type.rosetta",
                            "observable-asset-calculatedrate-func.rosetta",
                            "observable-asset-calculatedrate-enum.rosetta"),
                    List.of("observable-asset-func.rosetta",
                            "observable-asset-enum.rosetta",
                            "observable-asset-type.rosetta"),
                    List.of("product-asset-floatingrate-type.rosetta",
                            "product-asset-floatingrate-enum.rosetta",
                            "product-asset-floatingrate-func.rosetta"),
                    // XMETA (BusinessEventMeta's Qualify_* registration list) — the SECOND
                    // order-sensitive kind. Same walk-order mechanism, different file set:
                    // event-qualification-func (26 Qualify_ funcs) enumerated BEFORE
                    // event-common-func (11) on this cell's build machine. Package-info is
                    // untouched by the swap — all three cdm.event.common files carry one
                    // verbatim definition, so the multimap dedups them and their relative
                    // order is unobservable there.
                    List.of("event-qualification-func.rosetta",
                            "event-common-func.rosetta"))),
            Map.entry("cdm/6.21.0", List.of(
                    List.of("observable-asset-calculatedrate-type.rosetta",
                            "observable-asset-calculatedrate-enum.rosetta",
                            "observable-asset-calculatedrate-func.rosetta"),
                    List.of("product-asset-floatingrate-type.rosetta",
                            "product-asset-floatingrate-func.rosetta",
                            "product-asset-floatingrate-enum.rosetta"))),
            Map.entry("cdm/6.22.0", List.of(
                    List.of("observable-asset-calculatedrate-func.rosetta",
                            "observable-asset-calculatedrate-type.rosetta",
                            "observable-asset-calculatedrate-enum.rosetta"),
                    // XMETA (BusinessEventMeta's Qualify_* registration list) — the SECOND
                    // order-sensitive kind. Same walk-order mechanism, different file set:
                    // event-qualification-func (26 Qualify_ funcs) enumerated BEFORE
                    // event-common-func (11) on this cell's build machine. Package-info is
                    // untouched by the swap — all three cdm.event.common files carry one
                    // verbatim definition, so the multimap dedups them and their relative
                    // order is unobservable there.
                    List.of("event-qualification-func.rosetta",
                            "event-common-func.rosetta"))),
            Map.entry("cdm/6.23.0", List.of(
                    List.of("observable-asset-calculatedrate-type.rosetta",
                            "observable-asset-calculatedrate-enum.rosetta",
                            "observable-asset-calculatedrate-func.rosetta"),
                    List.of("observable-asset-func.rosetta",
                            "observable-asset-enum.rosetta",
                            "observable-asset-type.rosetta"),
                    List.of("product-asset-floatingrate-type.rosetta",
                            "product-asset-floatingrate-enum.rosetta",
                            "product-asset-floatingrate-func.rosetta"),
                    // XMETA (BusinessEventMeta's Qualify_* registration list) — the SECOND
                    // order-sensitive kind. Same walk-order mechanism, different file set:
                    // event-qualification-func (26 Qualify_ funcs) enumerated BEFORE
                    // event-common-func (11) on this cell's build machine. Package-info is
                    // untouched by the swap — all three cdm.event.common files carry one
                    // verbatim definition, so the multimap dedups them and their relative
                    // order is unobservable there.
                    List.of("event-qualification-func.rosetta",
                            "event-common-func.rosetta"))));

    /**
     * The cell's committed pin groups, or an empty list when it has none. Read by
     * {@link GoldenInputOrderPinTest}, which re-decodes them from the goldens by byte
     * oracle and fails with the exact literal to paste when they disagree.
     */
    static List<List<String>> goldenInputOrderPins(CellSpec cell) {
        return GOLDEN_INPUT_ORDER_PINS.getOrDefault(cell.toString(), List.of());
    }

    /**
     * Applies {@link #GOLDEN_INPUT_ORDER_PINS} to the cell's sorted walk: each pin
     * group's members are re-seated, in golden order, into the index slots the group
     * occupies in the sorted list; all other entries are untouched. Cells without
     * pins return the input list unchanged.
     */
    static List<Path> applyGoldenInputOrderPins(CellSpec cell, List<Path> sortedPaths) {
        List<List<String>> pinGroups = GOLDEN_INPUT_ORDER_PINS.get(cell.toString());
        if (pinGroups == null) {
            return sortedPaths;
        }
        List<Path> reordered = new ArrayList<>(sortedPaths);
        for (List<String> group : pinGroups) {
            Map<String, Path> pathByName = new LinkedHashMap<>();
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < reordered.size(); i++) {
                String name = reordered.get(i).getFileName().toString();
                if (group.contains(name)) {
                    if (pathByName.put(name, reordered.get(i)) != null) {
                        throw new AssertionError("D11 " + cell + " input-order pin: duplicate walk entry for "
                                + name + " — the pin's identity assumption is broken.");
                    }
                    slots.add(i);
                }
            }
            if (pathByName.size() != group.size()) {
                throw new AssertionError("D11 " + cell + " input-order pin: expected all of " + group
                        + " in the cell walk but found only " + pathByName.keySet()
                        + " — corpus layout changed; re-decode the golden order before re-pinning.");
            }
            for (int j = 0; j < group.size(); j++) {
                reordered.set(slots.get(j), pathByName.get(group.get(j)));
            }
        }
        return reordered;
    }

    private static final String KNOWN_DIVERGENT_RESOURCE = "/d11-known-divergent.txt";

    /**
     * Per-(cell, kind) Freezing-pattern allow-list for documented divergences.
     * Loaded from the classpath resource {@value #KNOWN_DIVERGENT_RESOURCE} at
     * static init; keys are {@code "<corpus>/<version>/<KIND>"}; values are
     * golden-relative file paths.
     *
     * <p>Resource-file format: one waiver entry per line as
     * {@code <corpus>/<version>/<KIND>:<path>}. Lines starting with {@code #} and
     * blank lines are ignored; per-cluster section headers + root-cause comments
     * live inside the resource file itself. See
     * {@code docs/reviews/p1.7-pr1.6-cluster-investigation.md} for the consolidated
     * per-cluster investigation evidence.
     *
     * <p>If the resource is absent (classpath misconfiguration), static init
     * fails loud — preferring an obvious init error to silent empty-waiver-set
     * which would mask every divergence behind the strict-equality assertion.
     */
    private static final Map<String, Set<String>> KNOWN_DIVERGENT = loadKnownDivergent();

    private static Map<String, Set<String>> loadKnownDivergent() {
        try (InputStream stream = D11CorpusRegressionTest.class.getResourceAsStream(KNOWN_DIVERGENT_RESOURCE)) {
            if (stream == null) {
                throw new AssertionError("D11 KNOWN_DIVERGENT resource not found: "
                        + KNOWN_DIVERGENT_RESOURCE
                        + " — expected at rune-java-generator/src/test/resources/.");
            }
            Map<String, Set<String>> raw = new LinkedHashMap<>();
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int colonIdx = trimmed.indexOf(':');
                    if (colonIdx < 1 || colonIdx >= trimmed.length() - 1) {
                        throw new AssertionError(KNOWN_DIVERGENT_RESOURCE + ":" + lineNum
                                + " — malformed waiver entry (expected '<corpus>/<version>/<KIND>:<path>'): "
                                + trimmed);
                    }
                    String key = trimmed.substring(0, colonIdx);
                    String path = trimmed.substring(colonIdx + 1);
                    // Validate key shape: <corpus>/<version>/<KIND> — must contain
                    // exactly 2 forward slashes and resolve a known KIND. Catches
                    // typos like "garbage:foo.java" or "cdm/5.35.0:foo" that would
                    // otherwise silently never match cell+"/"+kind during
                    // assertion-time waiver subtraction (R5-F1 / R1-DOK2 fix).
                    String[] keyParts = key.split("/", -1);
                    if (keyParts.length != 3
                            || keyParts[0].isEmpty()
                            || keyParts[1].isEmpty()
                            || keyParts[2].isEmpty()) {
                        throw new AssertionError(KNOWN_DIVERGENT_RESOURCE + ":" + lineNum
                                + " — invalid waiver key shape (expected '<corpus>/<version>/<KIND>'"
                                + " or the class-keyed '*/*/<KIND>'): " + key);
                    }
                    // v3.1 C0 item 5 — CLASS-KEYED waivers. '*' in the corpus and version
                    // positions waives one path across every cell of the band. It is for
                    // divergences whose cause is provably CELL-INDEPENDENT (an upstream
                    // defect in a generated class, not a per-cell artifact) and it must be
                    // both-or-neither: a half-wildcard would silently waive a whole corpus.
                    boolean corpusWild = "*".equals(keyParts[0]);
                    boolean versionWild = "*".equals(keyParts[1]);
                    if (corpusWild != versionWild) {
                        throw new AssertionError(KNOWN_DIVERGENT_RESOURCE + ":" + lineNum
                                + " — a class-keyed waiver must wildcard BOTH corpus and"
                                + " version ('*/*/<KIND>'); got: " + key);
                    }
                    String kindToken = keyParts[2];
                    boolean validKind = false;
                    for (ElementKind ek : ElementKind.values()) {
                        if (ek.name().equals(kindToken)) { validKind = true; break; }
                    }
                    if (!validKind) {
                        throw new AssertionError(KNOWN_DIVERGENT_RESOURCE + ":" + lineNum
                                + " — unknown ElementKind '" + kindToken + "' in waiver key: "
                                + key + " (valid: " + Arrays.toString(ElementKind.values()) + ")");
                    }
                    raw.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(path);
                }
            }
            Map<String, Set<String>> result = new LinkedHashMap<>();
            raw.forEach((k, v) -> result.put(k, Set.copyOf(v)));
            return Map.copyOf(result);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to load D11 KNOWN_DIVERGENT resource " + KNOWN_DIVERGENT_RESOURCE, e);
        }
    }

    /**
     * The waivers in force for one (cell, kind): the cell's own entries plus any
     * CLASS-KEYED ({@code &#42;/&#42;/<KIND>}) entries, which apply band-wide.
     *
     * <p>v3.1 C0 item 5 (audit N2). {@code ReferenceWithMetaVoid} is an upstream CDM
     * defect — the class wraps {@code java.lang.Void}, has zero golden references and no
     * model type behind it — and it reproduces identically in every cell whose CDM line
     * carries it: eight CDM cells and the four DRR 7.x cells that inherit them, twelve
     * in all, verified one per cell. Keying that to {@code cdm/6.20.6} alone was an
     * artifact of the 5-cell corpus; the band exposed eleven more reds for one cause. A
     * class key states the actual claim — this generated class is upstream's, wherever
     * it appears — and it stays exactly as visible: one waiver line, one written reason.
     */
    static Set<String> waiversFor(CellSpec cell, ElementKind kind) {
        Set<String> exact = KNOWN_DIVERGENT.getOrDefault(cell + "/" + kind, Set.of());
        Set<String> byClass = KNOWN_DIVERGENT.getOrDefault("*/*/" + kind, Set.of());
        // The chaos expected-divergence baseline (v3.2, charter § 5) joins ONLY for
        // the chaos cell — a structurally separate, shrink-only, deleted-at-close
        // declared-red set; see CHAOS_EXPECTED_DIVERGENCE.
        Set<String> chaos = "chaos".equals(cell.corpus())
                ? CHAOS_EXPECTED_DIVERGENCE.getOrDefault(cell + "/" + kind, Set.of())
                : Set.<String>of();
        if (byClass.isEmpty() && chaos.isEmpty()) {
            return exact;
        }
        Set<String> union = new LinkedHashSet<>(exact);
        union.addAll(byClass);
        union.addAll(chaos);
        return union;
    }

    private static final String CHAOS_EXPECTED_DIVERGENCE_RESOURCE = "/chaos-expected-divergence.txt";

    /**
     * THE v3.2 CHAOS EXPECTED-DIVERGENCE BASELINE (charter § 5): the measured
     * divergence rows of the FIRST D11 chaos probe (354 at seeding, 2026-09-02 —
     * 301 byte mismatches + 36 missingOutput + 17 noGolden; shrink-only since — 257 after
     * seat 1's 97 heals, PR #622 — the file's own SHRINK LOG is the meter's history), keyed
     * {@code chaos/<version>/<KIND>} ({@code chaos/1.1.0} since v3.2 seat 10) exactly like {@link #KNOWN_DIVERGENT} rows but
     * loaded from its own resource with THREE extra laws:
     * <ul>
     *   <li><b>chaos-only</b> — a non-chaos key is REFUSED at load, so this file can
     *       never dilute {@code d11-known-divergent.txt}'s 1-entry-PERMANENT
     *       semantics (v3.1's closed result);</li>
     *   <li><b>shrink-only</b> — every v3.2 fix seat's merge deletes its healed rows
     *       ({@code -Dd11.dump-now-matching=true} lists the healed mismatch rows —
     *       NOT the noGolden class, whose heal is the fork no longer emitting the
     *       file: those 17 rows WERE the committed {@code chaos-golden-free.txt} until the
     *       F13 seat adjudicated them — v3.2 seat 9, PR #630, D48: the register is the
     *       ASSERTED expected-noGolden set now, {@link #CHAOS_GOLDEN_FREE}, and this
     *       baseline carries NO noGolden row — only the two element REFUSALS); growth is a
     *       regression, and every receipts chain quotes the row count as the
     *       burn-down meter;</li>
     *   <li><b>deleted whole at v3.2 close</b> (the 100% CLEAN bar, charter § 6).</li>
     * </ul>
     * The census ({@code target/v32-chaos-census.md}, local) classifies these rows by
     * mechanism family and prices the seats.
     */
    private static final Map<String, Set<String>> CHAOS_EXPECTED_DIVERGENCE =
            loadChaosExpectedDivergence();

    private static final String CHAOS_GOLDEN_FREE_RESOURCE = "/chaos-golden-free.txt";

    /**
     * THE CHAOS GOLDEN-FREE REGISTER (v3.2 seat 9, PR #630, F13 / D48 — the § 4b union
     * ASSERTED): the golden-relative paths the FORK emits on the chaos cell for which the
     * released plugin defines NO golden — p2 of the s17 rival-enum split, which the plugin
     * refuses WHOLE for two unresolved {@code Sell} references while the fork's licence
     * stays per ELEMENT (the two offending elements REFUSED at {@code ENUM_VALUE_NAME_ECHO},
     * the other seven emitted). Bare paths, no kind: the pair gate reads the same file as
     * its expected-unmatched set, and the kind is what the fork EMITS — so per
     * {@code (cell, kind)} the chaos gate asserts {@code noGolden == register ∩ emitted}
     * in BOTH directions ({@link #assertChaosGoldenFreeUnion}): a register row the fork
     * stops emitting, or that gains a golden (p1's eight did, when the chaos pin script
     * stopped excluding split partners), is a STALE row to delete, never a tolerance.
     * Shrink-only; deleted whole at v3.2 close with the baseline.
     */
    private static final Set<String> CHAOS_GOLDEN_FREE = loadChaosGoldenFree();

    /**
     * THE v3.3 IR-SHARE DECLINE REGISTER (v3.3 seat 1, PR #637): {@code d11-ir-declines.txt}, one row per measured
     * IR decline of the ON route ({@code <corpus>/<version> <SEAM> <axis> <token> <count>}), asserted back per
     * {@code (cell, seam, axis)} as an EQUALITY by {@link #assertDeclaredIrDeclines} - the meter the IR share never
     * had. Until this seat the share was printed and asserted nowhere: one line declining {@code RFeatureCall} roots in
     * {@code ExpressionToIRAdapter.adapt} collapsed cdm/5.38.0 FUNCTION from 100.0% to 602 declines with the D11 GREEN
     * and every byte identical (the seat's groundwork, lane L4). The key is the SEAM (FUNCTION / RULE), never an
     * {@link ElementKind} - the RULE seam is measured inside {@code pojo_comparison}. A (cell, seam) with no row
     * declares the EMPTY breakdown; the ruleDelegation rows are KEPT-BY-DESIGN in their own block; the chaos rows are
     * re-pinned by the chaos-healing seats. Loaded and frozen by {@link IrDeclineRegister#load}.
     */
    private static final Map<String, Map<String, Integer>> IR_DECLARED_DECLINES = IrDeclineRegister.load();

    /** The {@code "<cell> <SEAM>"} keys the gate judged in this JVM - the wholeness law's population. */
    private static final Set<String> IR_DECLINE_KEYS_SEEN = ConcurrentHashMap.newKeySet();
    /** THE IR FALLBACK REGISTER (v3.3 seat 5, ruling R4): every enum / choice / data-type file the OLD generator still writes on the IR route. */
    private static final Map<String, Set<String>> IR_DECLARED_FALLBACKS = IrFallbackRegister.load();
    private static final Set<String> IR_FALLBACK_KEYS_SEEN = ConcurrentHashMap.newKeySet();
    /** {@code -Dd11.ir.fallbacks.dump=<file>}: append every old-generator-written file as a register row (the maker's input). */
    private static final String IR_FALLBACKS_DUMP = System.getProperty("d11.ir.fallbacks.dump", "").trim();

    private static Set<String> loadChaosGoldenFree() {
        try (InputStream stream = D11CorpusRegressionTest.class
                .getResourceAsStream(CHAOS_GOLDEN_FREE_RESOURCE)) {
            if (stream == null) {
                throw new AssertionError("D11 chaos golden-free register not found: "
                        + CHAOS_GOLDEN_FREE_RESOURCE
                        + " — expected at rune-java-generator/src/test/resources/.");
            }
            Set<String> rows = new LinkedHashSet<>();
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    if (trimmed.indexOf(':') >= 0 || !trimmed.endsWith(".java")
                            || !trimmed.startsWith("chaos/")) {
                        throw new AssertionError(CHAOS_GOLDEN_FREE_RESOURCE + ":" + lineNum
                                + " — malformed register row (a bare golden-relative path under"
                                + " chaos/, no kind prefix): " + trimmed);
                    }
                    if (!rows.add(trimmed)) {
                        throw new AssertionError(CHAOS_GOLDEN_FREE_RESOURCE + ":" + lineNum
                                + " — duplicate register row: " + trimmed);
                    }
                }
            }
            return Set.copyOf(rows);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read " + CHAOS_GOLDEN_FREE_RESOURCE, e);
        }
    }

    /** The register rows some chaos kind has EMITTED in this JVM, and the chaos kinds that ran — the whole-register law. */
    private static final Set<String> CHAOS_GOLDEN_FREE_SEEN =
            java.util.Collections.synchronizedSet(new java.util.TreeSet<>());
    private static final Set<ElementKind> CHAOS_KINDS_SEEN =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /**
     * v3.2 seat 9 (PR #630, F13 / D48) — the § 4b union, per chaos {@code (cell, kind)}: the files the
     * fork emitted with no golden ({@code results.noGolden}) must EQUAL the register's rows among the
     * files the fork emitted for this kind, in BOTH directions. {@code emitted} is what the fork wrote
     * for the kind (matches + mismatches + noGolden); the register carries bare paths, so a row's kind
     * is read off the emission itself. Fails loud on an UNDECLARED golden-free file (a new over-emission)
     * and on a STALE row of this kind (it gained a golden — p1's eight did at D48 — or the fork now
     * emits it elsewhere); {@link #chaosGoldenFreeRegisterWhollySeen} closes the last gap, a row NO kind
     * emits any more, once every chaos kind has run in this JVM.
     *
     * @return the register rows of this kind (the tolerance the caller applies to {@code noGolden})
     */
    private static Set<String> assertChaosGoldenFreeUnion(CellSpec cell, ElementKind kind,
                                                          ComparisonResults results) {
        Set<String> emitted = new LinkedHashSet<>(results.matches);
        results.mismatches.forEach(m -> emitted.add(m.file()));
        emitted.addAll(results.noGolden);
        Set<String> registerHere = new java.util.TreeSet<>();
        for (String row : CHAOS_GOLDEN_FREE) {
            if (emitted.contains(row)) {
                registerHere.add(row);
            }
        }
        Set<String> noGolden = new java.util.TreeSet<>(results.noGolden);
        assertEquals(registerHere, noGolden,
                "D11 " + cell + " " + kind + " the chaos golden-free union (D48): the fork's noGolden"
                        + " files must EQUAL the register's rows of this kind - a row only on the right is"
                        + " an UNDECLARED golden-free emission, a row only on the left a STALE register"
                        + " row (the fork matches a golden for it now, or emits it under another kind)");
        CHAOS_GOLDEN_FREE_SEEN.addAll(registerHere);
        CHAOS_KINDS_SEEN.add(kind);
        if (!registerHere.isEmpty()) {
            System.out.println("D11 " + cell + " " + kind + ": " + registerHere.size()
                    + " golden-free file(s) tolerated as rows of chaos-golden-free.txt (the D48"
                    + " register, asserted EQUAL to noGolden in both directions)");
        }
        return registerHere;
    }

    /**
     * The whole-register half of the D48 union: after a run that judged EVERY chaos kind, every register
     * row must have been emitted golden-free by SOME kind — a row none emitted is stale (the fork no
     * longer writes the file at all, e.g. an element it now refuses). A partial run (a kind filter, or a
     * vendored cell alone) judges no whole and is skipped.
     */
    @AfterAll
    static void chaosGoldenFreeRegisterWhollySeen() {
        if (CHAOS_KINDS_SEEN.size() != ElementKind.values().length) {
            return;
        }
        assertEquals(new java.util.TreeSet<>(CHAOS_GOLDEN_FREE), new java.util.TreeSet<>(CHAOS_GOLDEN_FREE_SEEN),
                "D11 chaos golden-free register (D48): rows NO chaos kind emitted golden-free in this run are"
                        + " STALE - delete them from chaos-golden-free.txt (shrink-only)");
    }

    /**
     * THE v3.3 IR-SHARE GATE (seat 1, PR #637) - one shared helper for both seams, called right after each seam's
     * {@code IR untargeted visits:} print where the three breakdown strings are in scope. Per axis (site, family,
     * untargeted) the declared map of {@link #IR_DECLARED_DECLINES} must EQUAL the measured breakdown
     * ({@link IrDeclineRegister#parseBreakdown} over {@code rankedBreakdown}'s print, order-insensitive); the verdict
     * ({@link IrDeclineRegister#verdict}) names the direction - NEW DECLINE (a regression: never absorbed by appending
     * the row) or HEALED (delete or shrink the row in the healing seat's own commit; the set is SHRINK-ONLY) - and
     * {@code assertEquals} prints both maps. The untargeted total is asserted to equal its breakdown's sum first (the
     * print's own conservation). Byte-inert by construction AND measured: the block is unreachable off-route
     * ({@code providerOrNull()} null; the OFF-route D11 log carries ZERO {@code IR share:} lines), and the gate throws
     * BEFORE {@code compareAgainstGolden} like the standing D43 ON-gate asserts, so a red gate masks that cell's byte
     * result for the run - the established shape, kept on purpose.
     */
    private static void assertDeclaredIrDeclines(CellSpec cell, String seam, int declined, String declSites, String declFamilies,
                                                 int untargetedTotal, String untargetedFamilies) {
        String cellName = cell.toString();
        IR_DECLINE_KEYS_SEEN.add(cellName + " " + seam);
        Map<String, Integer> measuredSites = IrDeclineRegister.parseBreakdown(declSites);
        Map<String, Integer> measuredFamilies = IrDeclineRegister.parseBreakdown(declFamilies);
        Map<String, Integer> measuredUntargeted = IrDeclineRegister.parseBreakdown(untargetedFamilies);
        // the print's own conservations FIRST, decided by the register's own corpus-free arbiter (PR #637 round 2,
        // spec SF-5 - the three inline asserts had no case): site and family each sum to irDeclined (the
        // recordDecline single-seat law), the untargeted breakdown to its printed total. A print that does not
        // conserve is a broken print, and the register cannot tell a counting bug from a coverage move.
        String conservation = IrDeclineRegister.conservation(cellName, seam, declined, measuredSites, measuredFamilies,
                untargetedTotal, measuredUntargeted);
        if (conservation != null) {
            throw new AssertionError(conservation);
        }
        // a typed map, not an Object[][] with an unchecked cast (PR #637 round 2, cq NIT-3)
        Map<String, Map<String, Integer>> axes = new LinkedHashMap<>();
        axes.put("site", measuredSites);
        axes.put("family", measuredFamilies);
        axes.put("untargeted", measuredUntargeted);
        int declaredRows = 0;
        for (Map.Entry<String, Map<String, Integer>> axis : axes.entrySet()) {
            String name = axis.getKey();
            Map<String, Integer> measured = axis.getValue();
            Map<String, Integer> declared = IR_DECLARED_DECLINES.getOrDefault(IrDeclineRegister.key(cellName, seam, name), Map.of());
            declaredRows += declared.size();
            String verdict = IrDeclineRegister.verdict(cellName, seam, name, declared, measured);
            if (verdict != null) {
                assertEquals(new java.util.TreeMap<>(declared), new java.util.TreeMap<>(measured), verdict);
            }
        }
        System.out.println("D11 " + cell + " " + seam + " IR-share gate: declared == measured on site / family / untargeted ("
                + declaredRows + " declared row(s))");
    }

    /**
     * The register's wholeness law (the stale-row detector, on the {@link #chaosGoldenFreeRegisterWhollySeen} pattern):
     * after a whole ON-route run - every catalogued cell x both seams judged, the count derived from
     * {@link #ALL_CELLS}, never typed and never read from the register - every declared {@code "<cell> <SEAM>"} key
     * must have been visited; a key the run never reached is STALE (a cell dropped from the catalogue, a seam renamed).
     *
     * <p>THREE run shapes reduce the count and only two of them are benign (PR #637 round 2, spec SF-4 = cq SF-1 -
     * the first cut named two and skipped on all three): (1) a SCOPED run, where {@code -Dd11.corpus} /
     * {@code -Dd11.version} filter {@link #activeCells()}; (2) a run that never enters the gate block at all -
     * OFF-route ({@code IRGeneration.providerOrNull()} null) or the optimised route ({@code optimisedEnabled()}); and
     * (3) an UNSCOPED ON-route run in which a catalogued cell's parametrized instances ABORTED because its goldens are absent from disk
     * ({@code Assumptions.assumeTrue(cellGoldensExist(cell), ...)} - surefire records Skipped, the suite stays GREEN and
     * the cell's key never enters {@link #IR_DECLINE_KEYS_SEEN}), or only part of the class was selected. Cause (3) is
     * exactly the silence this gate exists to break - a whole cell's IR share going unasserted with the suite GREEN -
     * so on an unscoped ON-route run the shortfall is FAIL-CLOSED, not a skip.
     *
     * <p>NOT a cause, checked: a seam block's reflective hop. Both catches ({@code ReflectiveOperationException |
     * ClassCastException}, at the RULE and FUNCTION seam blocks) RETHROW as an {@code AssertionError}, and always have,
     * so a thrown hop reddens its own cell rather than leaving this check short. The first cut of this javadoc said
     * they swallowed it, carried in unchecked from seat 1's banked item (PR #638 round 1, spec MF-1 = cq SF-5).
     */
    @AfterAll
    static void irDeclineRegisterWhollySeen() {
        int whole = ALL_CELLS.size() * IrDeclineRegister.SEAMS.size();
        if (IR_DECLINE_KEYS_SEEN.size() != whole) {
            boolean scoped = !SCOPE_CORPUS.isEmpty() || !SCOPE_VERSION.isEmpty();
            boolean referenceIrRoute = IRGeneration.providerOrNull() != null && !IRGeneration.optimisedEnabled();
            String where = IR_DECLINE_KEYS_SEEN.size() + " of " + whole + " (cell, seam) keys judged";
            if (!scoped && referenceIrRoute) {
                throw new AssertionError("D11 IR-share gate wholeness: " + where + " on an UNSCOPED ON-route run - the gate"
                        + " block did not run for every cell and seam, so some cell's IR share went UNASSERTED. Either a"
                        + " catalogued cell's goldens are absent from disk, so its parametrized instances aborted at"
                        + " Assumptions.assumeTrue(cellGoldensExist(cell)) and surefire recorded them Skipped, or only"
                        + " part of the class was selected. Judged: "
                        + new java.util.TreeSet<>(IR_DECLINE_KEYS_SEEN)
                        + " - a deliberately partial run is scoped with -Dd11.corpus / -Dd11.version, which makes this check"
                        + " judge nothing");
            }
            System.out.println("D11 IR-share gate wholeness: " + where + " - "
                    + (scoped ? "a scoped run (-Dd11.corpus / -Dd11.version)"
                              : "a run that never enters the gate block (OFF route, or the optimised route)")
                    + "; the wholeness check judges nothing");
            return;
        }
        System.out.println("D11 IR-share gate wholeness: all " + whole + " (cell, seam) keys judged; the register declares "
                + IrDeclineRegister.declaredKeys(IR_DECLARED_DECLINES).size() + " of them");
        Set<String> stale = new java.util.TreeSet<>(IrDeclineRegister.declaredKeys(IR_DECLARED_DECLINES));
        stale.removeAll(IR_DECLINE_KEYS_SEEN);
        assertTrue(stale.isEmpty(), "D11 IR-share gate: STALE key(s) - declared in d11-ir-declines.txt but never visited by"
                + " this whole ON-route run: " + stale + " - delete their rows (a cell dropped from the catalogue, or a seam"
                + " renamed); the register is SHRINK-ONLY");
    }

    private static Map<String, Set<String>> loadChaosExpectedDivergence() {
        try (InputStream stream = D11CorpusRegressionTest.class
                .getResourceAsStream(CHAOS_EXPECTED_DIVERGENCE_RESOURCE)) {
            if (stream == null) {
                throw new AssertionError("D11 chaos expected-divergence resource not found: "
                        + CHAOS_EXPECTED_DIVERGENCE_RESOURCE
                        + " — expected at rune-java-generator/src/test/resources/.");
            }
            Map<String, Set<String>> raw = new LinkedHashMap<>();
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int colonIdx = trimmed.indexOf(':');
                    if (colonIdx < 1 || colonIdx >= trimmed.length() - 1) {
                        throw new AssertionError(CHAOS_EXPECTED_DIVERGENCE_RESOURCE + ":" + lineNum
                                + " — malformed entry (expected 'chaos/<version>/<KIND>:<path>'): "
                                + trimmed);
                    }
                    String key = trimmed.substring(0, colonIdx);
                    String path = trimmed.substring(colonIdx + 1);
                    String[] keyParts = key.split("/", -1);
                    if (keyParts.length != 3 || !"chaos".equals(keyParts[0])
                            || keyParts[1].isEmpty() || keyParts[2].isEmpty()) {
                        throw new AssertionError(CHAOS_EXPECTED_DIVERGENCE_RESOURCE + ":" + lineNum
                                + " — the chaos baseline is CHAOS-ONLY (key must be"
                                + " 'chaos/<version>/<KIND>'; it must never carry a vendored"
                                + " cell or a wildcard): " + key);
                    }
                    boolean validKind = false;
                    for (ElementKind ek : ElementKind.values()) {
                        if (ek.name().equals(keyParts[2])) { validKind = true; break; }
                    }
                    if (!validKind) {
                        throw new AssertionError(CHAOS_EXPECTED_DIVERGENCE_RESOURCE + ":" + lineNum
                                + " — unknown element kind in key: " + key);
                    }
                    if (!raw.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(path)) {
                        throw new AssertionError(CHAOS_EXPECTED_DIVERGENCE_RESOURCE + ":" + lineNum
                                + " — duplicate row: " + trimmed);
                    }
                }
            }
            Map<String, Set<String>> frozen = new LinkedHashMap<>();
            raw.forEach((k, v) -> frozen.put(k, Set.copyOf(v)));
            return Map.copyOf(frozen);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read " + CHAOS_EXPECTED_DIVERGENCE_RESOURCE, e);
        }
    }

    /**
     * P2.1.1 — DRR ↔ CDM transitive-dependency version pin map. DRR cells declare a
     * compile-time CDM dependency in upstream pom.xml via `<finos.cdm.version>`; our
     * test loader mirrors that semantics by surfacing the matching CDM `.rosetta`
     * closure into the workspace BEFORE the cell's own walk (see {@link #loadCellCorpus}).
     *
     * <p>Verified by reading each DRR cell's pom.xml at P2.1.1 T1 design-lock
     * (see the development audit "cluster-a-fix-design" § 4). Cluster A
     * (60 ENUM divergences) shares root cause with Cluster D (DRR POJO) per
     * cluster investigation L62-64; P2.1.1 retires Cluster A only (ENUM scope).
     */
    // 2026-08-14: DERIVED from test-corpus/corpus-cells.tsv instead of hand-written.
    // DRR 6.34.1 is the one substitution in the table (it declares cdm 5.37.0, we load
    // 5.38.0); every other pin is exact. DRR 7.x is CDM-6-based where the 6.x line is
    // CDM-5-based — a workspace shape the 5-cell matrix never covered.
    static final Map<String, String> DRR_TO_CDM_VERSION =
            CorpusCells.depMap(TEST_CORPUS_ROOT, "drr", "cdm");

    /**
     * PR #155 — DRR ↔ ISO 20022 transitive-dependency version pin map. DRR upstream
     * builds ALSO declare a compile-time {@code org.iso20022:rosetta-source} dependency
     * (drr pom.xml {@code <iso20022.version>}) whose {@code .rosetta} files are unpacked
     * into {@code target/parent-dependency/iso20022/rosetta} and compiled as Xtext
     * sources alongside the cell's own models. Without this closure every
     * {@code iso20022.auth030.*} reference in the drr projection functions is
     * unresolved — function output types declared as iso20022 type aliases
     * ({@code MICIdentifier}, {@code ISODate}, …) erase to {@code Object} and every
     * iso20022-typed output loses its builder/validator scaffolding.
     *
     * <p>DRR 6.34.1 pins iso20022 1.37.0; we don't carry 1.37.0 as a standalone cell —
     * closest is 1.38.0 (minor diff), mirroring the CDM 5.37.0 → 5.38.0 substitution
     * above. Any resulting resolution skew surfaces as waivered mismatches in the
     * regenerated baseline (same absorption path as DRR_TO_CDM_VERSION).
     */
    // 2026-08-14: DERIVED (see DRR_TO_CDM_VERSION). DRR 7.x pins iso20022 1.38.0 — the
    // version already carried as the ISO catalogue cell — so together with its exact CDM
    // pin, DRR 7.x is the first DRR line whose whole closure resolves with ZERO version
    // substitution. DRR 6.35.0-6.38.0 resolve iso 1.37.0 exactly via a resolution-only cell.
    static final Map<String, String> DRR_TO_ISO20022_VERSION =
            CorpusCells.depMap(TEST_CORPUS_ROOT, "drr", "iso20022");

    /**
     * PR #184 / PR #185 — CDM ↔ rune-fpml transitive-dependency version pin map. The cdm6
     * cell's upstream build declares a compile-time {@code com.regnosys.rune-fpml}
     * dependency (cdm-6.20.6 {@code pom.xml} {@code <rune-fpml.version>}) and unpacks
     * its {@code fpml/rosetta/*.rosetta} into
     * {@code target/parent-dependency/fpml/rosetta}, compiling them as Xtext sources for
     * RESOLUTION only — upstream never EMITS fpml Java from the cdm6 build: its
     * {@code rosetta-maven-plugin} {@code <sourceRoots>} is {@code classes/cdm/rosetta}
     * alone, so {@code fpml.*} is fed to the generator for type resolution but never
     * generated (the cdm6 golden tree emits {@code cdm.ingest.fpml.*} but carries no
     * top-level {@code fpml.*} package). (The pom's separate maven-javadoc-plugin
     * {@code <excludePackageNames>fpml.*} is a javadoc-only exclusion, not the emission
     * gate.) This is why {@link #emissionFilter} keeps fpml resolution-only for cdm6.
     * Without this closure every {@code fpml.consolidated.*} reference in the cdm
     * {@code ingest.fpml.*} functions is unresolved — input parameter types erase to
     * {@code Object} and the ingest functions lose their resolved signatures +
     * imports. This is the missing fpml half of the same transitive-dependency
     * mechanism the DRR cells already use ({@link #DRR_TO_CDM_VERSION} /
     * {@link #DRR_TO_ISO20022_VERSION}); cdm6 simply never had its closure wired.
     *
     * <p>cdm 6.20.6 pins rune-fpml <b>1.5.3</b> (cdm-6.20.6 {@code pom.xml}
     * {@code <rune-fpml.version>1.5.3</rune-fpml.version>}). PR #184 substituted the only
     * rune-fpml then in the frozen corpus — 2.0.0 — as the closest available version
     * (mirroring the DRR 5.37.0 → 5.38.0 / 1.37.0 → 1.38.0 substitutions): 112 cdm6
     * {@code ingest~fpml} FUNCTION goldens flipped BYTE-IDENTICALLY against 2.0.0 (the
     * namespace/type-compatible subset). The residual ~430 carriers stayed waivered NOT
     * because of a generator gap but because rune-fpml 2.0.0 FLATTENED AWAY the FpML
     * model-group wrapper types the 1.5.3-built goldens navigate (e.g.
     * {@code fpml.consolidated.shared.BuyerSellerModel} / {@code ProductModel} —
     * absent from the entire 2.0.0 tree), so those navigations were genuinely
     * unresolvable against 2.0.0 — a VERSION mismatch, not a CODEGEN gap.
     * <p>PR #185 acquires the CORRECT rune-fpml <b>1.5.3</b>
     * ({@code rosetta-models/rune-fpml} @ tag {@code 1.5.3}, the version cdm6 pins and
     * its goldens were built against) into the corpus as cdm6's transitive dependency
     * (sources-only, {@code rosetta-source/src/main/rosetta}). With the model-group
     * types restored, a FURTHER 102 cdm6 {@code ingest~fpml} FUNCTION goldens flip
     * BYTE-IDENTICALLY against 1.5.3 (zero regressions: the #184 112 stay green — 1.5.3
     * is namespace/type-compatible with 2.0.0 for them — and POJO/ENUM/METAFIELD are
     * untouched, since {@link #emissionFilter} keeps fpml resolution-only). The remaining
     * cdm6 {@code ingest~fpml} divergences are now GENUINE generator gaps testable against
     * the correct fpml version (the version noise is removed). The rune-fpml CELL (D11's
     * 5th, its own goldens) stays 2.0.0 — only cdm6's transitive RESOLUTION moved to
     * 1.5.3. cdm 5.38.0 declares no rune-fpml dependency (no map entry → no fpml load).
     */
    // 2026-08-14: DERIVED (see DRR_TO_CDM_VERSION). cdm 6.20.2-6.20.6 pin rune-fpml
    // 1.5.3; cdm 6.21.0+ moved to 2.1.1. Both are exact. The fpml axis has bitten
    // before — PR #184 saw 112 cdm6 mismatches purely because fpml 2.0.0 had flattened
    // away types that 1.5.3 still declared — which is why the pin is per-cell data
    // rather than a per-corpus assumption.
    static final Map<String, String> CDM_TO_FPML_VERSION =
            CorpusCells.depMap(TEST_CORPUS_ROOT, "cdm", "rune-fpml");

    /**
     * The rune-fpml version a cell needs, following the dependency graph TRANSITIVELY.
     *
     * <p>A CDM cell declares its own fpml pin. A DRR cell does not declare one at all —
     * it inherits whatever its transitive CDM pins, so the version must be looked up
     * through {@link #DRR_TO_CDM_VERSION} and then {@link #CDM_TO_FPML_VERSION}.
     * Resolving only the direct pin left drr 7.3.0 (-> cdm 6.21.0 -> rune-fpml 2.1.1)
     * without any fpml source, and the missing input types read as generator defects.
     *
     * @return the fpml version to load, or {@code null} when the cell needs none
     */
    private static String resolveTransitiveFpmlVersion(CellSpec cell) {
        if ("cdm".equals(cell.corpus())) {
            return CDM_TO_FPML_VERSION.get(cell.version());
        }
        if ("drr".equals(cell.corpus())) {
            String transitiveCdm = DRR_TO_CDM_VERSION.get(cell.version());
            return transitiveCdm == null ? null : CDM_TO_FPML_VERSION.get(transitiveCdm);
        }
        return null;
    }

    /**
     * The rune-dsl 9.83.0 band. PR #85 rebaselined onto 5 cells; the 2026-08-14
     * expansion adds every remaining STABLE release pinned to 9.83.0 so the fork is
     * measured against the whole band rather than the five cells it was built against.
     *
     * <p>Cell selection is NOT ad hoc: an exhaustive scan of all 426 stable tags across
     * CDM majors 5-8, DRR majors 5-8, ISO majors 1-2 and rune-fpml majors 1-3 found
     * exactly 25 releases pinning {@code <rosetta.dsl.version>9.83.0}, each band
     * contiguous with an observed 9.78.0 boundary below and 9.84.0 above. CDM 7.x is
     * deliberately absent — it moved to rune-dsl 10.x (7.0.0 on 10.2.2, 7.1.0 on 10.3.0).
     *
     * <p>The generated-code bundle version varies within the band (11.120.2 / 11.121.0 /
     * 11.121.2) but is inert for generated Java: the plugin itself is pinned to
     * {@code rosetta.dsl.version}, and the bundle-versioned {@code default-cdm-generators}
     * is a four-class wiring jar (byte-identical across 11.120.2/11.121.2) supplying only
     * non-Java generators. Confirmed empirically: rune-fpml 2.1.0 and 2.1.1 have identical
     * {@code .rosetta} source under different bundles and emit byte-identical goldens.
     */
    // 2026-08-14: DERIVED from test-corpus/corpus-cells.tsv — every ACTIVE catalogue
    // row, in file order. Hand-listing 25 cells alongside 28 dependency-map entries is
    // exactly the drift surface that made the band expansion painful, so the list is
    // read rather than restated.
    //
    // Note rune-fpml 2.1.0 and 2.1.1 are PROVEN byte-identical in both .rosetta source
    // and emitted goldens (they differ only in generator bundle version, which is
    // inert), so 2.1.0 carries no information 2.1.1 does not. Both are kept because each
    // is a distinct published 9.83.0 release, and the claim being made is that the fork
    // handles EVERY release in the band — not every release bar one.
    private static final List<CellSpec> ALL_CELLS =
            CorpusCells.activeCatalogue(TEST_CORPUS_ROOT).stream()
                    .map(r -> new CellSpec(r.corpus(), r.version(),
                            TEST_CORPUS_ROOT.resolve(r.relPath())))
                    .toList();

    /** Source for parametrized tests: ALL_CELLS filtered by -Dd11.corpus/-Dd11.version. */
    static Stream<CellSpec> activeCells() {
        return ALL_CELLS.stream()
                .filter(c -> SCOPE_CORPUS.isEmpty() || SCOPE_CORPUS.equals(c.corpus()))
                .filter(c -> SCOPE_VERSION.isEmpty() || SCOPE_VERSION.equals(c.version()));
    }

    /**
     * The UNFILTERED cell-name universe ({@code <corpus>/<version>}), deliberately
     * ignoring the -Dd11.corpus/-Dd11.version scope flags: resource-universe
     * validation (LadderBindingCensusTest.gateResources_areInternallyValid) must
     * hold rows for EVERY catalogued cell, or a scoped run would false-fail the
     * rows belonging to the cells scoped out.
     */
    static java.util.Set<String> allCellNames() {
        return ALL_CELLS.stream().map(CellSpec::toString)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * Per-corpus path resolver (D25). ISO Xtext writes to target/classes/generated/java/;
     * other corpora commit src/generated/java/.
     */
    static Path resolveGoldensDir(CellSpec cell) {
        if ("iso20022".equals(cell.corpus())) {
            return cell.root().resolve("rosetta-source/target/classes/generated/java");
        }
        return cell.root().resolve("rosetta-source/src/generated/java");
    }

    static Path resolveRosettaInputDir(CellSpec cell) {
        return cell.root().resolve("rosetta-source/src/main/rosetta");
    }

    static boolean cellGoldensExist(CellSpec cell) {
        return Files.isDirectory(resolveRosettaInputDir(cell))
                && Files.isDirectory(resolveGoldensDir(cell));
    }

    enum ElementKind { ENUM, POJO, METAFIELD, FUNCTION, ONLY_EXISTS_VALIDATOR, CARDINALITY_VALIDATOR, PACKAGE_INFO, TYPE_FORMAT_VALIDATOR, XMETA, DEEP_PATH_UTIL, DATA_RULE }

    /**
     * Deterministic 11-rule classifier for generator output; first match wins.
     *
     * <p>Rule 1 (ONLY_EXISTS_VALIDATOR): files under an {@code exists/} directory
     * (PR #405 coverage wave A — {@code <pkg>.validation.exists.<T>OnlyExistsValidator}).
     * <p>Rule 2 (TYPE_FORMAT_VALIDATOR): files under a {@code validation/} directory
     * whose basename ends {@code TypeFormatValidator.java} (PR #407 coverage wave B —
     * {@code <pkg>.validation.<T>TypeFormatValidator}; the suffix subtraction within
     * the parent rule is exact for the 9.83.0 corpus — see {@link #expectedKind}).
     * <p>Rule 3 (CARDINALITY_VALIDATOR): remaining files under a {@code validation/}
     * directory (PR #405 — {@code <pkg>.validation.<T>Validator}).
     * <p>Rule 4 (XMETA): files under a {@code meta/} directory (PR #407 —
     * {@code <pkg>.meta.<T>Meta}; the survey tiling proves the meta/ parent holds
     * XMeta registries ONLY — exactly 4,736 corpus-wide, all {@code *Meta.java};
     * re-verified live at the #407 wiring).
     * <p>Rule 5 (DEEP_PATH_UTIL): files under a {@code util/} directory (PR #408
     * coverage wave C — {@code <pkg>.util.<T>DeepPathUtil}; the tiling is exact for
     * the 9.83.0 corpus: exactly 356 goldens with an IMMEDIATE {@code util/} parent,
     * all {@code *DeepPathUtil.java} — the drr namespace literally named
     * {@code drr.regulation.common.util} contributes only {@code util/functions/*}
     * files, whose immediate parent is {@code functions}; re-verified live at the
     * #408 wiring).
     * <p>Rule 6 (DATA_RULE): files under a {@code datarule/} directory (PR #409
     * coverage wave D — {@code <pkg>.validation.datarule.<Type><Cond>}; the LAST
     * family — the tiling is exact for the 9.83.0 corpus: exactly 3,853 goldens
     * with an immediate {@code datarule/} parent, per-cell 448/555/2,140/246/464).
     * <p>Rule 7 (PACKAGE_INFO): {@code package-info.java} (PR #405).
     * <p>Rule 8 (FUNCTION): files under a {@code functions/} directory.
     * <p>Rule 9 (METAFIELD): {@code FieldWith*} / {@code ReferenceWith*} / {@code BasicReferenceWith*}.
     * <p>Rule 10 (ENUM): files ending with {@code Enum.java}.
     * <p>Rule 11 (POJO): everything else.
     *
     * <p>The parent-directory rules sit FIRST because they are more specific than the
     * basename rules: a validator for a type whose name starts with {@code ReferenceWith…}
     * must not classify METAFIELD; within the {@code validation/} parent the
     * TypeFormat SUFFIX rule precedes the cardinality catch-all.
     */
    static ElementKind classify(Path javaFile) {
        String name = javaFile.getFileName().toString();
        Path parent = javaFile.getParent();
        String parentName = (parent != null && parent.getFileName() != null)
                ? parent.getFileName().toString()
                : "";
        if ("exists".equals(parentName)) return ElementKind.ONLY_EXISTS_VALIDATOR;
        if ("validation".equals(parentName)) {
            if (name.endsWith("TypeFormatValidator.java")) return ElementKind.TYPE_FORMAT_VALIDATOR;
            return ElementKind.CARDINALITY_VALIDATOR;
        }
        if ("meta".equals(parentName)) return ElementKind.XMETA;
        if ("util".equals(parentName)) return ElementKind.DEEP_PATH_UTIL;
        if ("datarule".equals(parentName)) return ElementKind.DATA_RULE;
        if ("package-info.java".equals(name)) return ElementKind.PACKAGE_INFO;
        if ("functions".equals(parentName)) return ElementKind.FUNCTION;
        if (name.startsWith("FieldWith") || name.startsWith("ReferenceWith") || name.startsWith("BasicReferenceWith")) {
            return ElementKind.METAFIELD;
        }
        if (name.endsWith("Enum.java")) return ElementKind.ENUM;
        return ElementKind.POJO;
    }

    /**
     * Returns the {@link ElementKind} a golden file represents IF it's something our
     * wired generators are expected to emit; {@link Optional#empty()} otherwise.
     *
     * <p>PR #405 (coverage burn-down wave A) brought three formerly-excluded upstream
     * families into scope: only-exists validators ({@code exists/*.java}), cardinality
     * validators ({@code validation/*Validator.java} minus the TypeFormat basenames)
     * and {@code package-info.java}. PR #407 (wave B) brought in the two remaining
     * template families: type-format validators
     * ({@code validation/*TypeFormatValidator.java}) and XMeta registries
     * ({@code meta/*.java}). PR #408 (wave C) brought in the deep-path utils
     * ({@code util/*DeepPathUtil.java}). PR #409 (wave D — the LAST family)
     * brought in the data rules ({@code datarule/*.java}): EVERY upstream family
     * is now in scope — no exclusions remain.
     *
     * <p>The {@code *TypeFormatValidator.java} suffix routing is exact for the
     * 9.83.0 corpus: the survey (#400 §3.3) counts the validator families at 4,736
     * EACH — a model type itself named {@code *TypeFormat} would skew the tiling
     * (its cardinality validator would count into the TypeFormat family), so none
     * exists. The {@code meta/} parent rule is likewise exact: exactly 4,736
     * meta-parented goldens corpus-wide, all {@code *Meta.java} (re-verified live
     * at the #407 wiring).
     */
    static Optional<ElementKind> expectedKind(Path goldenFile) {
        String name = goldenFile.getFileName().toString();
        Path parent = goldenFile.getParent();
        String parentName = (parent != null && parent.getFileName() != null)
                ? parent.getFileName().toString()
                : "";
        if ("meta".equals(parentName)) return Optional.of(ElementKind.XMETA);
        if ("validation".equals(parentName)) {
            if (name.endsWith("TypeFormatValidator.java")) return Optional.of(ElementKind.TYPE_FORMAT_VALIDATOR);
            return Optional.of(ElementKind.CARDINALITY_VALIDATOR);
        }
        if ("util".equals(parentName)) return Optional.of(ElementKind.DEEP_PATH_UTIL);
        if ("exists".equals(parentName)) return Optional.of(ElementKind.ONLY_EXISTS_VALIDATOR);
        if ("datarule".equals(parentName)) return Optional.of(ElementKind.DATA_RULE);
        if ("package-info.java".equals(name)) return Optional.of(ElementKind.PACKAGE_INFO);
        // Phase X T7 R7 F18: Removed the prior Ingest_*LabelProvider exclusion.
        // Pre-Phase-X (P2.1.3b β2 fix), the 4-generator scope (EnumGenerator +
        // ModelObjectGenerator + MetaFieldGenerator + FunctionGenerator) did
        // not emit any *LabelProvider files, so an exclusion at the Pass 2
        // missingOutput layer was the correct way to keep these goldens out
        // of the expected set. Phase X T7 wired LabelProviderGenerator (the
        // 5th-generator scope) into pojo_comparison, which DOES emit
        // Ingest_*LabelProvider files. Keeping the exclusion would hide a
        // regression vector — if a future change suppressed Ingest_*
        // emission, the missing goldens would no longer be counted as
        // missingOutput. The byte-diff between our emission and the goldens
        // is still captured via Pass 1 strict-equality + the
        // Phase-X-T7 sub-section in d11-known-divergent.txt; the present
        // removal restores Pass 2 missingOutput coverage to match the new
        // generator wiring.
        // Enum goldens whose Java filename lacks the conventional `Enum.java` suffix —
        // upstream `enum CompareOp:` (base-math-enum.rosetta L26), the ISO 20022 `*Code.java`
        // / collision-disambiguated `*Code__N.java` code lists, and drr domain enums
        // (CommonAssetClass, …) — are classified ENUM by {@link #compareAgainstGolden} via
        // the model-derived enumPaths set (see {@link #emittedEnumPaths}) BEFORE this
        // filename heuristic is consulted. No per-name special case is needed here; the
        // classify(...) fallthrough below therefore applies only to non-enum goldens.
        return Optional.of(classify(goldenFile));
    }

    // =========================================================================
    // Parametrized tests — one per kind, fanned across activeCells().
    // =========================================================================

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void enum_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell));
        // The D43 IR seams: construction + dispatch route through IRGeneration — with the
        // flag off (the default, every standing run) these are exactly the legacy calls.
        var enumGen = IRGeneration.enumGenerator(gm);

        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                // P2.1.1 T3.3 — emission filter via GeneratorModel.shouldGenerate
                // (implements the L272 TODO). For DRR cells, excludes the
                // transitive-CDM + transitive-ISO20022 models loaded for
                // resolution-only (see the dependency-closure walks in
                // loadCellCorpus(...) and the development audit "cluster-a-fix-design"
                // § 6 + § 9.8). Replaces the
                // T3.1 outer-loop {@code belongsToCellCorpus} helper with the
                // production-side mechanism that {@code MetaFieldGenerator} and
                // {@code FunctionGenerator} also consult during their internal model
                // iteration — uniform filter at the source.
                //
                // JavaClassGenerator#generateClasses returns per-object failures
                // rather than throwing. Capture + throw on non-empty so generator
                // errors surface as direct test failures instead of blending into
                // the matrix drift inventory as secondary missingOutput/mismatch
                // symptoms (Copilot R6 F1 2026-05-04).
                genErrors.addAll(IRGeneration.generateClasses(enumGen, model, gm.version(model), output));
            }
        }
        // v3.3 seat 5 (decision D55): the declaration-reconcile channel - PRINTED before the gate below reads the errors.
        // v3.3 seat 6 (PR #642, round 1 cq SF-2): every line of the pass prints - the reconcile, the PARENT reconcile
        // (the enums the emitter flattened through its index, each reconciled once) and the IR file writers - and only
        // then is any of them judged, the pojo pass's law.
        String enumReconcile = printDeclarationReconcile(cell, "ENUM", enumGen,
                emittedDeclarations(corpus.workspace().files(), gm, com.regnosys.rosetta.ast.types.REnumeration.class));
        String enumParents = printParentReconcile(cell, "ENUM", "parentReconcileStats", enumGen);
        String enumFallback = irFallbackVerdict(cell, "ENUM", enumGen, output.keySet());
        // v3.3 seat 7 (PR #643, round 1 cq SF-2): the enum pass's verdict promised its mismatches "named below" and threw
        // first, exactly as the pojo pass's did before commit 3 - the same by-name print, before the verdict, here too.
        for (GenerationException error : genErrors) {
            if (error.getMessage() != null && error.getMessage().startsWith("IR/AST reconciliation failed")) {
                System.out.println("D11 " + cell + " IR declaration mismatch: " + error.getMessage());
            }
        }
        failOnDeclarationReconcile(enumReconcile, enumParents);
        failOnIrFallbackVerdicts(enumFallback);
        assertNoGenErrors(cell, ElementKind.ENUM, genErrors);

        // The enum pass's own output key set IS the cell's emitted-enum path set; pass it
        // so the comparator routes enums whose Java filename lacks the `Enum.java` suffix
        // (ISO *Code.java / collision-disambiguated *Code__N.java / domain-named enums like
        // CommonAssetClass) to ENUM rather than POJO. Without this, Pass 1 below skips the
        // byte-compare for those files (classify() → POJO ≠ ENUM), leaving them unverified.
        var results = compareAgainstGolden(cell, output, ElementKind.ENUM, Set.copyOf(output.keySet()));
        System.out.println("D11 " + cell + " ENUM: " + results.summary());
        assertCellKind(cell, ElementKind.ENUM, results);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void pojo_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        // facet isoPruneConfig (PR #331): the POJO cell honours the model project's
        // generators.doNotPrune configuration (see readDoNotPrune).
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell), readDoNotPrune(cell));
        // The D43 IR seams (construction; the invocations below dispatch through IRGeneration).
        var pojoGen = IRGeneration.modelObjectGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        // P2.1.3c T2: ChoiceObjectGenerator (β1) emits top-level `choice X: Foo Bar`
        // POJOs which ModelObjectGenerator's RDataType-only filter omitted. Wired
        // alongside the data-type generator; per-model orchestration runs both.
        var choiceGen = IRGeneration.choiceObjectGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL, pojoGen);
        // Phase X T7: wire the 3 new generators (RuleGenerator + ReportGenerator +
        // LabelProviderGenerator) alongside the POJO + choice path. Their emitted
        // classes (*Rule.java, *ReportFunction.java, *LabelProvider.java) are
        // classified as POJO by the 4-rule classify(...) (none lives under a
        // `functions/` parent; none is *Enum.java; none is FieldWith*/ReferenceWith*),
        // so the goldens for these files sit in the POJO bucket and the
        // strict-equality compare runs through compareAgainstGolden(...,POJO).
        // Phase X targets the CODEGEN_MISSING POJO entries in the
        // `# === TRANSITIVE_CDM_GAP` section of the waiver file (see
        // src/test/resources/d11-known-divergent.txt for the canonical pre-flip
        // baseline; Phase X T8 retires byte-matching entries via waiver flip-out).
        var funcGen = IRGeneration.functionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var ruleGen = new RuleGenerator(gm, TYPE_TRANSLATOR, funcGen);
        var reportGen = new ReportGenerator(gm, TYPE_TRANSLATOR, funcGen);
        // Copilot PR #72 R1+R2 F2: wire DeepFeatureCallUtil with gm::getType so the
        // resolver matches LabelProviderGenerator's own type-resolution path
        // (workspace + builtin fallback). The default no-arg resolver only handles
        // RDataType references and returns RMissingType for choice-typed or
        // builtin-typed attributes, causing under-traversal of deep paths during
        // D11 emission. Mirrors LabelProviderGeneratorTest's wiring discipline.
        var labelProviderGen = new LabelProviderGenerator(
                gm, TYPE_TRANSLATOR, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());

        Map<String, String> output = new LinkedHashMap<>();
        // Two failure buckets. Mature generators (POJO + choice) must not fail at
        // all — any per-object failure is a regression. Rule-family generators
        // (Rule/Report/LabelProvider) are still converging on body emission (M7b-3
        // typed pipeline pending — see docs/audits/phase-x/codegen-completeness-
        // audit-2026-05-27.md), so a failure for an already-waivered element is
        // EXPECTED CODEGEN_BODY_GAP debt and is tolerated; only a failure for a
        // NON-waivered element (a new gap) fails the cell.
        List<GenerationException> coreGenErrors = new ArrayList<>();
        List<GenerationException> ruleFamilyGenErrors = new ArrayList<>();
        // v3.3 seat 5 (ruling R4): the files each declaration generator ADDS to the shared output, read as a key-set
        // difference around its call - the generation calls themselves are untouched
        Map<String, String> outputSeen = new java.util.HashMap<>();
        Set<String> dataTypeFiles = new java.util.TreeSet<>();
        Set<String> choiceFiles = new java.util.TreeSet<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                // P2.1.1 T3.3 — emission filter via GeneratorModel.shouldGenerate
                // (see enum_comparison comment above for the full rationale).
                //
                // JavaClassGenerator#generateClasses collects per-object failures into the
                // returned list rather than throwing. Capture + assert makes those silent
                // failures visible (Copilot R6 F1 2026-05-04). Replaces the prior try/catch
                // + System.err.println swallow which also masked unexpected RuntimeExceptions
                // thrown by the generator itself; those now propagate naturally and abort the
                // (cell, kind) instance — symmetric with R4's parse-failure throw fix.
                String version = gm.version(model);
                collectAdded(output, outputSeen, null);
                coreGenErrors.addAll(IRGeneration.generateClasses(pojoGen, model, version, output));
                collectAdded(output, outputSeen, dataTypeFiles);
                coreGenErrors.addAll(IRGeneration.generateClasses(choiceGen, model, version, output));
                collectAdded(output, outputSeen, choiceFiles);
                ruleFamilyGenErrors.addAll(ruleGen.generateClasses(model, version, output));
                ruleFamilyGenErrors.addAll(reportGen.generateClasses(model, version, output));
                ruleFamilyGenErrors.addAll(labelProviderGen.generateClasses(model, version, output));
            }
        }
        // the LAST model's rule-family writes are read too: a file re-written by a second generator is refused wherever it happens
        collectAdded(output, outputSeen, null);

        // v3.3 seat 5 (decision D55): the declaration-reconcile channel, the data-type and choice passes - PRINTED before
        // any gate of this method reads the generation errors. BOTH reconcile lines print before EITHER is judged, on the
        // same law as the file-writer lines below: a red DATA_TYPE reconcile must not hide the CHOICE reconcile line -
        // nor the two writer lines, which used to sit behind it too (v3.3 seat 6, PR #642 - round 1 cq NIT-4)
        String dataTypeReconcile = printDeclarationReconcile(cell, "DATA_TYPE", pojoGen,
                emittedDeclarations(corpus.workspace().files(), gm, com.regnosys.rosetta.ast.types.RDataType.class));
        String choiceReconcile = printDeclarationReconcile(cell, "CHOICE", choiceGen,
                emittedDeclarations(corpus.workspace().files(), gm, com.regnosys.rosetta.ast.types.RChoice.class));
        // v3.3 seat 7 (PR #643 - the type gate): the FOURTH declaration line. A typeAlias writes no file, so it rides
        // the data-type pass through a second reconciler with its own population - printed here, beside the other two
        String typeAliasReconcile = printTypeAliasReconcile(cell, pojoGen,
                emittedDeclarations(corpus.workspace().files(), gm, com.regnosys.rosetta.ast.types.RTypeAlias.class));
        // v3.3 seat 8 (PR #644 - PR B, THE PROPERTY GATE): the five NEW channel lines of this pass, each in the
        // declaration channel's EXISTING printed format (the status scripts parse it unchanged) and each asserting ITS
        // OWN population (LAW 84). PROPERTY and DERIVED share ONE population - the cell's VALIDATED ELEMENTS, its data
        // types and its choices together, so ONE line each and not one per sub-kind; MODEL's population is this host's
        // own count of the models under the emission filter; WRAPPER's is this host's own
        // MetaFieldGenerator.collectSpecs() count, which the generator's answer must equal. All five print BEFORE any of
        // them is judged, the pojo pass's law (round 1 cq NIT-4 of #642).
        int validatedElements = emittedDeclarations(corpus.workspace().files(), gm, com.regnosys.rosetta.ast.types.RDataType.class)
                + emittedDeclarations(corpus.workspace().files(), gm, com.regnosys.rosetta.ast.types.RChoice.class);
        String propertyReconcile = printDeclarationReconcile(cell, "PROPERTY", "propertyReconcileStats", pojoGen, validatedElements);
        // the parent line's population is the INDEX's reach (every declaration the property law's walks touched: the
        // elements' supertypes AND the item types of specialized properties) - this host cannot count it without
        // mirroring the walk, so it asserts a FLOOR it CAN count: the distinct transitive supertypes of the cell's
        // validated elements, every one of which the walks must have reached (PR #644 round 1, rule6 MF-2)
        String propertyParents = printParentReconcile(cell, "PROPERTY", "propertyParentReconcileStats", pojoGen,
                transitiveSupertypes(corpus.workspace().files(), gm));
        String derivedReconcile = printDeclarationReconcile(cell, "DERIVED", "derivedReconcileStats", pojoGen, validatedElements);
        String modelReconcile = printDeclarationReconcile(cell, "MODEL", "modelReconcileStats", pojoGen,
                emittedModels(corpus.workspace().files(), gm));
        String wrapperReconcile = printWrapperReconcile(cell, pojoGen,
                new MetaFieldGenerator(gm, TYPE_TRANSLATOR).collectSpecs().size());
        String derivedTwoReads = printDerivedTwoReads(cell, corpus.workspace().files(), gm, pojoGen);
        // v3.3 seat 9 (PR #645 commit 4 - THE EMITTER PR): the POJO SHADOW line. The IR data-type emitter is asked
        // for a render of every validated data type and NOTHING it answers reaches the output map; the line books
        // its OWN POPULATION (LAW 84) and, since the POJO member went READY at commit 11, the member's gate.
        // ITS ORACLE IS A SECOND REAL PRODUCER (PR #645 commit 18, round 1 cq MF-1): the LEGACY
        // ModelObjectGenerator, constructed DIRECTLY with the arguments IRGeneration.modelObjectGenerator takes -
        // never through that seam, which answers the IR-routed subclass here - and driven over the same models in
        // the same order the pass walked. `output` must NOT be handed in: since commit 15 it holds the UNIT's own
        // text at every written key, so the compare would be the unit against itself.
        Map<String, String> pojoOracle = legacyRender(cell, "POJO",
                () -> new ModelObjectGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL), corpus.workspace().files(), gm);
        ShadowHalf[] pojoHalves = shadowHalves(corpus.workspace().files(), gm, M_POJO);
        String pojoShadow = printPojoShadow(cell, pojoGen, pojoOracle,
                emittedDeclarations(corpus.workspace().files(), gm, com.regnosys.rosetta.ast.types.RDataType.class),
                pojoHalves[0]);
        // v3.3 seat 10 (PR #646 commit 4): THE CHOICE HALF OF THE POJO SHADOW. A choice owns the same six files a
        // data type owns and IRDataTypeEmitter admits a CHOICE node since this commit, so IRChoiceObjectGenerator
        // books what the emitter WOULD write for every choice of the cell - into no output map, the kind switch
        // being off - and this line holds it against a SECOND REAL PRODUCER: a freshly constructed LEGACY
        // ChoiceObjectGenerator over a freshly constructed legacy ModelObjectGenerator delegate, driven beside the
        // pass on this very run (the two-producer law of PR #645 commit 18; `output` must NOT be handed in).
        Map<String, String> pojoChoiceOracle = legacyRender(cell, "POJO-CHOICE",
                () -> new ChoiceObjectGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL,
                        new ModelObjectGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL)),
                corpus.workspace().files(), gm);
        String pojoChoiceShadow = printPojoShadow(cell, choiceGen, pojoChoiceOracle,
                emittedDeclarations(corpus.workspace().files(), gm, com.regnosys.rosetta.ast.types.RChoice.class),
                pojoHalves[1]);
        // BOTH sub-kinds are printed (and dumped) before EITHER is judged: a red DATA_TYPE verdict must not hide the CHOICE line
        String dataTypeFallback = irFallbackVerdict(cell, "DATA_TYPE", pojoGen, dataTypeFiles);
        String choiceFallback = irFallbackVerdict(cell, "CHOICE", choiceGen, choiceFiles);
        // v3.3 seat 7 (PR #643): the reconcile verdict promises "each is a generation error named below" - and it threw BEFORE the
        // pass's generation errors were ever printed (the type gate's own lane G1 read mismatches=11 with no fact named in the
        // log). Every reconcile mismatch of the pass is printed HERE, by name, before the verdict is raised; a clean pass prints
        // nothing, so the OFF route's log and every green ON-route log are unmoved.
        for (GenerationException error : coreGenErrors) {
            if (error.getMessage() != null && error.getMessage().startsWith("IR/AST reconciliation failed")) {
                System.out.println("D11 " + cell + " IR declaration mismatch: " + error.getMessage());
            }
        }
        // v3.3 seat 8 (PR #644): the WRAPPER mismatches are raised by no generation call - the reconcile runs once,
        // after the pass, when the line above first reads it - so they are printed here by name, in the same shape, before
        // the verdict that says they are "named below".
        for (String mismatch : wrapperMismatches(cell, pojoGen)) {
            System.out.println("D11 " + cell + " IR declaration mismatch: " + mismatch);
        }
        failOnDeclarationReconcile(dataTypeReconcile, choiceReconcile, typeAliasReconcile,
                propertyReconcile, propertyParents, derivedReconcile, modelReconcile, wrapperReconcile, derivedTwoReads,
                pojoShadow, pojoChoiceShadow);
        // v3.3 seat 9 (PR #645 commit 12): this pass's UNIT verdict sets, for the cross-pass equality
        // line the @AfterAll below prints and gates (the planning review's Q1(b)).
        // v3.3 seat 10 (PR #646 commit 4): the cell's CHOICE qualified names, this host's own read of the
        // source, for the TYPE UNIT VERDICTS line's routing-law assertion - the production path attempts no
        // choice while IRTypeUnitWiring.CHOICE_AVAILABLE is off, and that is asserted BY NAME rather than counted.
        IR_CELL_CHOICE_NAMES.put(cell.toString(), choiceNames(corpus.workspace().files(), gm));
        bookUnitVerdicts(cell, M_POJO, pojoGen);
        failOnIrFallbackVerdicts(dataTypeFallback, choiceFallback);

        // The D43 RULE-seam ON-gate readers (#474 — closing the mechanization gap the #473 RULE-seam
        // witness decode exposed: "counters unread by any reader block"). THIS pass's funcGen IS the
        // RULE seam — RuleGenerator + ReportGenerator render the rule/report bodies through its
        // IRExpressionCompiler (the Phase X T7 wiring above; generateWithErrors is never invoked here),
        // so the counters read pure rule/report-body activity — disjoint from the FUNCTION seam's own
        // funcGen + readers in function_comparison (a different generator instance per pass; the #473
        // Seat-1 counter-instance isolation). Same discipline as the FUNCTION block: active only when
        // the IR route actually RESOLVED (providerOrNull(), not the bare flag — Copilot #467 C-1),
        // reflective hops because rune-ir-java is not on the standing test classpath (the ir-on profile
        // supplies the m2 jars), flag-off inert (the OFF ring prints zero share/meter lines).
        //
        // v3.1 band survey: EXCLUDE the optimised route. Every reader below is a REFERENCE-route
        // (rune-ir-java) counter — irExpressionCompiler()/postPinServeLoweredCount()/
        // irFunctionExpressionRenderer(). providerOrNull() is non-null for BOTH routes, so the
        // optimised route reached this block and died in the catch BEFORE the byte comparison ran,
        // which is why the optimised route had never been measured against the goldens on any cell.
        // Gating on !optimisedEnabled() leaves the reference ON ring byte-for-byte unchanged
        // (optimisedEnabled() is false there) and lets the optimised route reach compareAgainstGolden.
        // NOTE the gap this exposes: the optimised route has NO route-health assertions in D11.
        if (IRGeneration.providerOrNull() != null && !IRGeneration.optimisedEnabled()) {
            try {
                // (r1) The RULE-seam share line, EVERY cell — same shape as the FUNCTION line, labeled
                // RULE; the same honest-dial framing (attempt-success over IR-targeted families, printed
                // next to the byte count below, never as the parity number).
                Object compiler = funcGen.getClass().getMethod("irExpressionCompiler").invoke(funcGen);
                int driven = (int) compiler.getClass().getMethod("irDrivenCount").invoke(compiler);
                int declined = (int) compiler.getClass().getMethod("irDeclinedCount").invoke(compiler);
                int guardDeclined = (int) compiler.getClass()
                        .getMethod("postPinCoercionDeclinedCount").invoke(compiler);
                int guardDelegated = (int) compiler.getClass()
                        .getMethod("postPinDelegatedCount").invoke(compiler);
                int attempted = driven + declined;
                double share = attempted == 0 ? 0.0
                        : Math.floor(driven * 10000.0 / attempted) / 100.0;
                System.out.println("D11 " + cell + " RULE IR share: irDriven=" + driven
                        + " irDeclined=" + declined + " (" + share + "% of " + attempted
                        + " attempted; postPinCoercionDeclined=" + guardDeclined
                        + " postPinDelegated=" + guardDelegated + ")");
                // (r1b) The #493 driven-metric split line + its two hard conservation gates —
                // see printDrivenSplit (single-sourced for both seams).
                printDrivenSplit(cell, "RULE", compiler, driven);
                // (r2) The decline meter (first-trip attribution; tokens sum to postPinCoercionDeclined
                // by construction) — every arm's explicit ZERO is the taught state on this seam too.
                String armBreakdown = (String) compiler.getClass()
                        .getMethod("postPinCoercionDeclineBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " RULE postPin by arm: " + armBreakdown);
                // (r3) The router's delegation meter — the frozen-zero honest-residue line since the
                // #516 serve conversion (the live attribution moved to the served line below;
                // served + declined = tripped, per arm).
                String delegBreakdown = (String) compiler.getClass()
                        .getMethod("postPinDelegationBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " RULE postPin delegated by arm: " + delegBreakdown);
                // (r3a) The #516 delegated-seat serve, RULE edition: the router's live per-arm
                // attribution + the two flip receipts + the claim-root seat's FIRST per-arm census
                // (Σ of each served line ≡ its receipt per cell BY CONSTRUCTION — the compiler
                // increments both at the single seat).
                String serveBreakdown = (String) compiler.getClass()
                        .getMethod("postPinServeBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " RULE postPin served by arm: " + serveBreakdown);
                Object postPinServed = compiler.getClass()
                        .getMethod("postPinServeLoweredCount").invoke(compiler);
                System.out.println("D11 " + cell + " RULE IR postPinServeLowered: " + postPinServed);
                String claimRootServes = (String) compiler.getClass()
                        .getMethod("claimRootServeBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " RULE claimRoot served by arm: "
                        + claimRootServes);
                Object claimRootServed = compiler.getClass()
                        .getMethod("claimRootServeLoweredCount").invoke(compiler);
                System.out.println("D11 " + cell + " RULE IR claimRootServeLowered: "
                        + claimRootServed);
                // (r3b) The #475 share-growth census, RULE edition — the same three lines as the
                // FUNCTION block's (a4): the declined population by claim-root family and by decline
                // site (both conserve to irDeclined at the recordDecline single seat), plus the
                // UNTARGETED families the §4.2 denominator never saw (count-then-super meters; behavior-
                // inert by construction). Ranked count-descending — the lines ARE the teach worklist.
                String declFamilies = (String) compiler.getClass()
                        .getMethod("irDeclineFamilyBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " RULE IR declined by family: " + declFamilies);
                String declSites = (String) compiler.getClass()
                        .getMethod("irDeclineSiteBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " RULE IR declined by site: " + declSites);
                // (r3b2) The #531 W-facet belt census, RULE edition — the same line as the
                // FUNCTION block's (a4b), on the same OWN non-empty guard outside the (r3c)
                // blockerProbed gate (the closed-board census channel; the alias seat's
                // population is FUNCTION-only today, so this line reads empty — the mirror
                // print is the BY-CALL mirror law, not an expected carrier).
                String wAliasGate = (String) compiler.getClass()
                        .getMethod("wAliasGateBreakdown").invoke(compiler);
                if (!"none".equals(wAliasGate)) {
                    System.out.println("D11 " + cell + " RULE IR wAliasGate census: "
                            + wAliasGate);
                }
                int untargetedTotal = (int) compiler.getClass()
                        .getMethod("untargetedVisitCount").invoke(compiler);
                String untargetedFamilies = (String) compiler.getClass()
                        .getMethod("untargetedVisitBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " RULE IR untargeted visits: total=" + untargetedTotal
                        + " " + untargetedFamilies);
                // (r3b3) THE v3.3 IR-SHARE GATE (seat 1, PR #637): the three breakdowns above asserted EQUAL to the
                // register's declared rows of this (cell, RULE) - see assertDeclaredIrDeclines.
                assertDeclaredIrDeclines(cell, "RULE", declined, declSites, declFamilies, untargetedTotal, untargetedFamilies);
                // (r3c) The #476 adapterGap blocker-attribution probe, RULE edition — the same two
                // ranked lines as the FUNCTION block's (a5), printed ONLY when the probe ran (the
                // property-gated census channel; the standing receipts stay byte-identical probe-off).
                int blockerProbed = (int) compiler.getClass()
                        .getMethod("blockerProbedClaimCount").invoke(compiler);
                if (blockerProbed > 0) {
                    // (r3c0) The #491 implicitVisit facet probe, RULE edition — the same line as the
                    // FUNCTION block's (a5a): the RImplicitVariable visit population by kind ×
                    // typing × binding context (Σ facets ≡ the family's WHOLE visit population —
                    // driven+declined since the #491 teach — by construction).
                    String implicitFacets = (String) compiler.getClass()
                            .getMethod("implicitVisitFacetBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR implicitVisit facets: "
                            + implicitFacets);
                    // (r3c0b) The #492 point-free renderer face meter — the renderer's own exits
                    // (uncorrelated / metaGate / nonExpression / rendered), the decode channel for
                    // the post-teach leafEmitter residue.
                    String pointFreeFaces = (String) compiler.getClass()
                            .getMethod("pointFreeRenderBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR pointFree renders: "
                            + pointFreeFaces);
                    // (r3c0c) The #494 ctorVisit facet probe, RULE edition — the same line as the
                    // FUNCTION block's (a5a3): the RConstructorExpr visit population by target ×
                    // typing × value-lowerability (Σ facets ≡ the family's WHOLE visit population —
                    // driven+declined since the #494 teach — by construction).
                    String ctorFacets = (String) compiler.getClass()
                            .getMethod("ctorVisitFacetBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR ctorVisit facets: "
                            + ctorFacets);
                    // (r3c0d) The #495 condVisit facet probe, RULE edition — the same line as the
                    // FUNCTION block's (a5a4): the RConditionalExpr visit population by shape ×
                    // typing × root-adapt-lowerability (Σ facets ≡ the family's WHOLE visit
                    // population — driven+declined since the #495 teach — by construction).
                    String condFacets = (String) compiler.getClass()
                            .getMethod("condVisitFacetBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR condVisit facets: "
                            + condFacets);
                    // (r3c0e) The #496 lambdaVisit facet probe, RULE edition — the same line as
                    // the FUNCTION block's (a5a5): the RExtractExpr + RFilterExpr visit
                    // populations by family × binder × typing × the root-adapt verdict (Σ facets
                    // ≡ each family's WHOLE visit population per cell — driven+declined since the
                    // #496 teach — by construction).
                    String lambdaFacets = (String) compiler.getClass()
                            .getMethod("lambdaVisitFacetBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR lambdaVisit facets: "
                            + lambdaFacets);
                    // (r3c0f) The #496 binder-gate cross-read, RULE edition — the same line as the
                    // FUNCTION block's (a5a6): the lambda-machinery gate mass attributed by the
                    // exact widening lever.
                    String binderCross = (String) compiler.getClass()
                            .getMethod("binderGateCrossBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR binderGate cross: "
                            + binderCross);
                    // (r3c0g) The #496 L-111 root census, RULE edition — the same line as the
                    // FUNCTION block's (a5a7): the inputFeatureNav seat's shape read (meta-ness ×
                    // typing × the root adapter verdict with the recvKind split; the seat's
                    // population is FUNCTION-only, so this line reads none).
                    String inputNavRoots = (String) compiler.getClass()
                            .getMethod("inputNavRootBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR inputNavRoot census: "
                            + inputNavRoots);
                    // (r3c0h) The #497 metaNav census, RULE edition — the same line as the
                    // FUNCTION block's (a5a8).
                    String metaNavRoots = (String) compiler.getClass()
                            .getMethod("metaNavRootBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR metaNavRoot census: "
                            + metaNavRoots);
                    // (r3c0i) The #497 implicitAttrNav census, RULE edition — the same line as
                    // the FUNCTION block's (a5a9); the L-113 population is drr-RULE dominant.
                    String implicitAttrRoots = (String) compiler.getClass()
                            .getMethod("implicitAttrRootBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR implicitAttrRoot census: "
                            + implicitAttrRoots);
                    // (r3c0i3) The #514 leafEmitter-gap census, RULE edition — the emitter
                    // decline site's raw-family × lowered-root-kind decomposition (since
                    // #515 the served rows carry the .served suffix — Σ served ≡ the flip
                    // receipt below).
                    String leafGap = (String) compiler.getClass()
                            .getMethod("leafEmitterGapBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR leafEmitterGap census: "
                            + leafGap);
                    // (r3c0i4) The #515 emitter-frontier serve's flip receipt, RULE edition
                    // — the same line as the FUNCTION block's (a5a9d).
                    Object emitterServed = compiler.getClass()
                            .getMethod("emitterServeLoweredCount").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR emitterServeLowered: "
                            + emitterServed);
                    // (r3c0i2) The #497 first-sample witness — the qualitative face per bucket
                    // (which real corpus sites the dominant faces hold — the varPath class's
                    // compiling-render question is answered by WHERE the events live).
                    String implicitAttrSamples = (String) compiler.getClass()
                            .getMethod("implicitAttrWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR implicitAttrRoot witness: "
                            + implicitAttrSamples);
                    // (r3c0j) The #497 L-112 census, RULE edition — the same line as the
                    // FUNCTION block's (a5a10).
                    String inputNavReceiverRoots = (String) compiler.getClass()
                            .getMethod("inputNavReceiverRootBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR inputNavReceiverRoot census: "
                            + inputNavReceiverRoots);
                    // (r3c0k) The #498 A+B gate censuses, RULE edition — the same four lines as
                    // the FUNCTION block's (a5a11).
                    String onlyElemGate = (String) compiler.getClass()
                            .getMethod("onlyElemGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR onlyElemGate census: "
                            + onlyElemGate);
                    String onlyElemWitness = (String) compiler.getClass()
                            .getMethod("onlyElemGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR onlyElemGate witness: "
                            + onlyElemWitness);
                    String calleeGate = (String) compiler.getClass()
                            .getMethod("calleeGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR calleeGate census: "
                            + calleeGate);
                    String calleeWitness = (String) compiler.getClass()
                            .getMethod("calleeGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR calleeGate witness: "
                            + calleeWitness);
                    // (r3c0l) The #499 A+B admission-cluster censuses, RULE edition — the same
                    // four lines as the FUNCTION block's (a5a12).
                    String listOpGate = (String) compiler.getClass()
                            .getMethod("listOpGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR listOpGate census: "
                            + listOpGate);
                    String listOpGateWitness = (String) compiler.getClass()
                            .getMethod("listOpGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR listOpGate witness: "
                            + listOpGateWitness);
                    String metaFeatureGate = (String) compiler.getClass()
                            .getMethod("metaFeatureGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR metaFeatureGate census: "
                            + metaFeatureGate);
                    String metaFeatureGateWitness = (String) compiler.getClass()
                            .getMethod("metaFeatureGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR metaFeatureGate witness: "
                            + metaFeatureGateWitness);
                    // (r3c0m) The #500 A+B cluster censuses, RULE edition — the same ten lines
                    // as the FUNCTION block's (a5a13).
                    String metaAccessGate = (String) compiler.getClass()
                            .getMethod("metaAccessGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR metaAccessGate census: "
                            + metaAccessGate);
                    String metaAccessGateWitness = (String) compiler.getClass()
                            .getMethod("metaAccessGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR metaAccessGate witness: "
                            + metaAccessGateWitness);
                    String thenVisit = (String) compiler.getClass()
                            .getMethod("thenVisitBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR thenVisit census: "
                            + thenVisit);
                    String thenVisitWitness = (String) compiler.getClass()
                            .getMethod("thenVisitWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR thenVisit witness: "
                            + thenVisitWitness);
                    String conversionVisit = (String) compiler.getClass()
                            .getMethod("conversionVisitBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR conversionVisit census: "
                            + conversionVisit);
                    String conversionVisitWitness = (String) compiler.getClass()
                            .getMethod("conversionVisitWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR conversionVisit witness: "
                            + conversionVisitWitness);
                    String listLitVisit = (String) compiler.getClass()
                            .getMethod("listLitVisitBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR listLitVisit census: "
                            + listLitVisit);
                    String listLitVisitWitness = (String) compiler.getClass()
                            .getMethod("listLitVisitWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR listLitVisit witness: "
                            + listLitVisitWitness);
                    String onlyExistsVisit = (String) compiler.getClass()
                            .getMethod("onlyExistsVisitBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR onlyExistsVisit census: "
                            + onlyExistsVisit);
                    String onlyExistsVisitWitness = (String) compiler.getClass()
                            .getMethod("onlyExistsVisitWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR onlyExistsVisit witness: "
                            + onlyExistsVisitWitness);
                    // (r3c0n) The #501 A+C cluster censuses, RULE edition — the same four lines
                    // as the FUNCTION block's (a5a14).
                    String shallowGate = (String) compiler.getClass()
                            .getMethod("shallowGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR shallowGate census: "
                            + shallowGate);
                    String shallowGateWitness = (String) compiler.getClass()
                            .getMethod("shallowGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR shallowGate witness: "
                            + shallowGateWitness);
                    String chainDrainGate = (String) compiler.getClass()
                            .getMethod("chainDrainGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR chainDrainGate census: "
                            + chainDrainGate);
                    String chainDrainGateWitness = (String) compiler.getClass()
                            .getMethod("chainDrainGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR chainDrainGate witness: "
                            + chainDrainGateWitness);
                    // (r3c0o) The #502 A+B cluster censuses, RULE edition — the same four lines
                    // as the FUNCTION block's (a5a15).
                    String symbolDrainGate = (String) compiler.getClass()
                            .getMethod("symbolDrainGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR symbolDrainGate census: "
                            + symbolDrainGate);
                    String symbolDrainGateWitness = (String) compiler.getClass()
                            .getMethod("symbolDrainGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR symbolDrainGate witness: "
                            + symbolDrainGateWitness);
                    String synItemGate = (String) compiler.getClass()
                            .getMethod("synItemGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR synItemGate census: "
                            + synItemGate);
                    String synItemGateWitness = (String) compiler.getClass()
                            .getMethod("synItemGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR synItemGate witness: "
                            + synItemGateWitness);
                    // (r3c0p) The #503 A+B cluster censuses, RULE edition — the same four lines
                    // as the FUNCTION block's (a5a16).
                    String equalityGate = (String) compiler.getClass()
                            .getMethod("equalityGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR equalityGate census: "
                            + equalityGate);
                    String equalityGateWitness = (String) compiler.getClass()
                            .getMethod("equalityGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR equalityGate witness: "
                            + equalityGateWitness);
                    String featureDrainGate = (String) compiler.getClass()
                            .getMethod("featureDrainGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR featureDrainGate census: "
                            + featureDrainGate);
                    String featureDrainGateWitness = (String) compiler.getClass()
                            .getMethod("featureDrainGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR featureDrainGate witness: "
                            + featureDrainGateWitness);
                    // (r3c0q) The #504 A+B+C cluster censuses, RULE edition — the same four
                    // lines as the FUNCTION block's (a5a17).
                    String booleanOpGate = (String) compiler.getClass()
                            .getMethod("booleanOpGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR booleanOpGate census: "
                            + booleanOpGate);
                    String booleanOpGateWitness = (String) compiler.getClass()
                            .getMethod("booleanOpGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR booleanOpGate witness: "
                            + booleanOpGateWitness);
                    String qualNameGate = (String) compiler.getClass()
                            .getMethod("qualNameGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR qualNameGate census: "
                            + qualNameGate);
                    String qualNameGateWitness = (String) compiler.getClass()
                            .getMethod("qualNameGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR qualNameGate witness: "
                            + qualNameGateWitness);
                    // (r3c0r) The #505 A+B+C cluster censuses, RULE edition — the same eight
                    // lines as the FUNCTION block's (a5a18).
                    String enumSeatGate = (String) compiler.getClass()
                            .getMethod("enumSeatGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR enumSeatGate census: "
                            + enumSeatGate);
                    String enumSeatGateWitness = (String) compiler.getClass()
                            .getMethod("enumSeatGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR enumSeatGate witness: "
                            + enumSeatGateWitness);
                    String enumSeatRoot = (String) compiler.getClass()
                            .getMethod("enumSeatRootBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR enumSeatRoot census: "
                            + enumSeatRoot);
                    String enumSeatRootWitness = (String) compiler.getClass()
                            .getMethod("enumSeatRootWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR enumSeatRoot witness: "
                            + enumSeatRootWitness);
                    String argResidueGate = (String) compiler.getClass()
                            .getMethod("argResidueGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR argResidueGate census: "
                            + argResidueGate);
                    String argResidueGateWitness = (String) compiler.getClass()
                            .getMethod("argResidueGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR argResidueGate witness: "
                            + argResidueGateWitness);
                    String dispatchGate = (String) compiler.getClass()
                            .getMethod("dispatchGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR dispatchGate census: "
                            + dispatchGate);
                    String dispatchGateWitness = (String) compiler.getClass()
                            .getMethod("dispatchGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR dispatchGate witness: "
                            + dispatchGateWitness);
                    String walkBindGate = (String) compiler.getClass()
                            .getMethod("walkBindGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR walkBindGate census: "
                            + walkBindGate);
                    String walkBindGateWitness = (String) compiler.getClass()
                            .getMethod("walkBindGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR walkBindGate witness: "
                            + walkBindGateWitness);
                    String deepGate = (String) compiler.getClass()
                            .getMethod("deepGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR deepGate census: " + deepGate);
                    String deepGateWitness = (String) compiler.getClass()
                            .getMethod("deepGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR deepGate witness: "
                            + deepGateWitness);
                    String deepVisit = (String) compiler.getClass()
                            .getMethod("deepVisitBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR deepVisit census: " + deepVisit);
                    String deepVisitWitness = (String) compiler.getClass()
                            .getMethod("deepVisitWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR deepVisit witness: "
                            + deepVisitWitness);
                    String noChanGate = (String) compiler.getClass()
                            .getMethod("noChanGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR noChanGate census: "
                            + noChanGate);
                    String noChanGateWitness = (String) compiler.getClass()
                            .getMethod("noChanGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR noChanGate witness: "
                            + noChanGateWitness);
                    String l111Residue = (String) compiler.getClass()
                            .getMethod("l111ResidueBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR l111Residue census: "
                            + l111Residue);
                    String l111ResidueWitness = (String) compiler.getClass()
                            .getMethod("l111ResidueWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR l111Residue witness: "
                            + l111ResidueWitness);
                    // (r3c0t) The #508 A+B cluster censuses, RULE edition — the same six lines
                    // as the FUNCTION edition below (the RULE cells carry the drr-R faces:
                    // chainMeta 80 · twNull 30 · symbolNotAttribute 90 at the #507 SOT).
                    String metaHopGate = (String) compiler.getClass()
                            .getMethod("metaHopGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR metaHopGate census: "
                            + metaHopGate);
                    String metaHopGateWitness = (String) compiler.getClass()
                            .getMethod("metaHopGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR metaHopGate witness: "
                            + metaHopGateWitness);
                    String metaSrcGate = (String) compiler.getClass()
                            .getMethod("metaSrcGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR metaSrcGate census: "
                            + metaSrcGate);
                    String metaSrcGateWitness = (String) compiler.getClass()
                            .getMethod("metaSrcGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR metaSrcGate witness: "
                            + metaSrcGateWitness);
                    String symNotGate = (String) compiler.getClass()
                            .getMethod("symNotGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR symNotGate census: "
                            + symNotGate);
                    String symNotGateWitness = (String) compiler.getClass()
                            .getMethod("symNotGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR symNotGate witness: "
                            + symNotGateWitness);
                    // (r-509) The #509 A+B cluster censuses, RULE edition — the same six
                    // lines as the FUNCTION edition below (the RULE cells carry the drr-R
                    // faces: headUnresolved 10 · nonSymbolReceiver 11 at the #508 SOT).
                    String aliasIdGate = (String) compiler.getClass()
                            .getMethod("aliasIdGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR aliasIdGate census: "
                            + aliasIdGate);
                    String aliasIdGateWitness = (String) compiler.getClass()
                            .getMethod("aliasIdGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR aliasIdGate witness: "
                            + aliasIdGateWitness);
                    String headUnGate = (String) compiler.getClass()
                            .getMethod("headUnGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR headUnGate census: "
                            + headUnGate);
                    String headUnGateWitness = (String) compiler.getClass()
                            .getMethod("headUnGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR headUnGate witness: "
                            + headUnGateWitness);
                    String nsrGate = (String) compiler.getClass()
                            .getMethod("nsrGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR nsrGate census: "
                            + nsrGate);
                    String nsrGateWitness = (String) compiler.getClass()
                            .getMethod("nsrGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR nsrGate witness: "
                            + nsrGateWitness);
                    // #520 arm-A3: the featureUnresolved.headOther face's own decode (the
                    // calleeGate/nsrGate pattern at the third feature-resolution face).
                    String headOtherGate = (String) compiler.getClass()
                            .getMethod("headOtherGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR headOtherGate census: "
                            + headOtherGate);
                    String headOtherGateWitness = (String) compiler.getClass()
                            .getMethod("headOtherGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR headOtherGate witness: "
                            + headOtherGateWitness);
                    // #524: the B-cluster pre-arm census (the synthetic item-attr pair +
                    // the lambdaSrc compose — legacy's bare-name ladder BY CALL).
                    String itemAttrGate = (String) compiler.getClass()
                            .getMethod("itemAttrGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR itemAttrGate census: "
                            + itemAttrGate);
                    String itemAttrGateWitness = (String) compiler.getClass()
                            .getMethod("itemAttrGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR itemAttrGate witness: "
                            + itemAttrGateWitness);
                    // #526: the NotExpressible-cluster pre-arm census (the child-subtree
                    // faces — the adapter's own position-divergence predicates BY CALL).
                    String neGate = (String) compiler.getClass()
                            .getMethod("neGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR neGate census: " + neGate);
                    String neGateWitness = (String) compiler.getClass()
                            .getMethod("neGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR neGate witness: "
                            + neGateWitness);
                    // #527: the ARG_NAV-dict pre-arm census (the typeGap/typeMissing residue
                    // faces — legacy's evaluate-arg coercion-arm preconditions BY CALL + the
                    // standing post-pin scan's claim-root verdict).
                    String argGapGate = (String) compiler.getClass()
                            .getMethod("argGapGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR argGapGate census: "
                            + argGapGate);
                    String argGapGateWitness = (String) compiler.getClass()
                            .getMethod("argGapGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR argGapGate witness: "
                            + argGapGateWitness);
                    // #528: the operand/arg residue pre-arm census (the five surviving
                    // operand/call-seat faces — the adapter's own gate predicates BY CALL,
                    // seat-complete across every family that can mint each spelling).
                    String opResGate = (String) compiler.getClass()
                            .getMethod("opResGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR opResGate census: "
                            + opResGate);
                    String opResGateWitness = (String) compiler.getClass()
                            .getMethod("opResGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR opResGate witness: "
                            + opResGateWitness);
                    // #529: the same four pricing/A-tail censuses at the RULE seat (see the
                    // FUNCTION block's note).
                    for (String gate : new String[] {"itemHeadGate", "headAttrGate",
                            "eqImplGate", "itemArgSrcGate"}) {
                        String gateCensus = (String) compiler.getClass()
                                .getMethod(gate + "Breakdown").invoke(compiler);
                        System.out.println("D11 " + cell + " RULE IR " + gate
                                + " census: " + gateCensus);
                        String gateWitness = (String) compiler.getClass()
                                .getMethod(gate + "WitnessSamples").invoke(compiler);
                        System.out.println("D11 " + cell + " RULE IR " + gate
                                + " witness: " + gateWitness);
                    }
                    // #530: the endgame-residue pre-arm censuses, RULE edition (see the
                    // FUNCTION block's note).
                    for (String gate : new String[] {"pipeCondGate", "nsrCtGate",
                            "kvpGate", "metaArgGate"}) {
                        String gateCensus = (String) compiler.getClass()
                                .getMethod(gate + "Breakdown").invoke(compiler);
                        System.out.println("D11 " + cell + " RULE IR " + gate
                                + " census: " + gateCensus);
                        String gateWitness = (String) compiler.getClass()
                                .getMethod(gate + "WitnessSamples").invoke(compiler);
                        System.out.println("D11 " + cell + " RULE IR " + gate
                                + " witness: " + gateWitness);
                    }
                    int blockerAnomalies = (int) compiler.getClass()
                            .getMethod("blockerProbeAnomalyCount").invoke(compiler);
                    String blockerClaims = (String) compiler.getClass()
                            .getMethod("blockerClaimsBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR adapterGap blockers (probed="
                            + blockerProbed + " anomalies=" + blockerAnomalies + "): " + blockerClaims);
                    String soleBlockers = (String) compiler.getClass()
                            .getMethod("soleBlockerClaimsBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR adapterGap sole-blocker claims: "
                            + soleBlockers);
                    // (r3d) The #477 per-ARM refinement decode, RULE edition — the same three lines
                    // as the FUNCTION block's (a5b): Family:reason participation, the sole-reason
                    // unlock ranking, and the multi-reason residue that closes the per-family
                    // conservation; unattributed is the mirror-fidelity meter (expected 0).
                    int reasonUnattributed = (int) compiler.getClass()
                            .getMethod("blockerReasonUnattributedCount").invoke(compiler);
                    String blockerReasons = (String) compiler.getClass()
                            .getMethod("blockerReasonBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR adapterGap blocker reasons (unattributed="
                            + reasonUnattributed + "): " + blockerReasons);
                    String soleReasons = (String) compiler.getClass()
                            .getMethod("soleReasonClaimsBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR adapterGap sole-reason claims: "
                            + soleReasons);
                    String multiReasonResidue = (String) compiler.getClass()
                            .getMethod("soleFamilyMultiReasonBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR adapterGap sole-family multi-reason claims: "
                            + multiReasonResidue);
                    // (r3e) The #478 nav-gate SHAPE witness, RULE edition — the same three lines as
                    // the FUNCTION block's (a5c): head/root class × the adapter's verdict on the
                    // legacy-synthesized equivalent, the chain-receiver position split, and one
                    // pinned corpus site per bucket (node-occurrence unit).
                    String navShapes = (String) compiler.getClass()
                            .getMethod("navGateShapeBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR nav-gate shapes: " + navShapes);
                    String navPositions = (String) compiler.getClass()
                            .getMethod("navGatePositionBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR nav-gate positions: " + navPositions);
                    String navSamples = (String) compiler.getClass()
                            .getMethod("navGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR nav-gate witness: " + navSamples);
                    // (r3f) The #479 implicit-ROOT shape witness, RULE edition — the same two lines
                    // as the FUNCTION block's (a5d): the three cluster gates classified by the
                    // legacy binding/synthesizer machinery that renders each blocker's implicit
                    // root, one pinned corpus site per bucket (node-occurrence unit).
                    String rootShapes = (String) compiler.getClass()
                            .getMethod("implicitRootShapeBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR implicit-root shapes: " + rootShapes);
                    String rootSamples = (String) compiler.getClass()
                            .getMethod("implicitRootWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR implicit-root witness: " + rootSamples);
                    // (r3g) The #480 SOURCE decode + arm-admission restatement, RULE edition — the
                    // same three lines as the FUNCTION block's (a5e): the unprovableSource residue
                    // + the still-declining equivalents' bases decoded by source shape (the
                    // widening map), one pinned site per bucket, and the planned bare-attr/chain
                    // arms' admissions restated per blocker (post-teach `claims` reads ZERO).
                    String rootSources = (String) compiler.getClass()
                            .getMethod("implicitRootSourceBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR implicit-root sources: " + rootSources);
                    String rootSourceSamples = (String) compiler.getClass()
                            .getMethod("implicitRootSourceWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR implicit-root sources witness: "
                            + rootSourceSamples);
                    String rootArm = (String) compiler.getClass()
                            .getMethod("implicitRootArmBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR implicit-root arm: " + rootArm);
                    // (r3h) The #481 elided-PIPE decode, RULE edition — the same two lines as the
                    // FUNCTION block's (a5f): the planned widening's admission restated per
                    // elidedImplicit source (conservation: Σ pipe facets ≡ the flat token count).
                    String rootPipe = (String) compiler.getClass()
                            .getMethod("implicitRootPipeBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR implicit-root pipe: " + rootPipe);
                    String rootPipeSamples = (String) compiler.getClass()
                            .getMethod("implicitRootPipeWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " RULE IR implicit-root pipe witness: "
                            + rootPipeSamples);
                }

                // (r4) The fully-taught pin, EVERY cell (the #473 census: the seam's whole trip
                // population is drr — 13 router delegations + the 10 intLiteral claim-root declines the
                // #474 seat widening taught; the other cells read zero trivially, so the pin is
                // universally tense): a NEW decline here means the claim-root seat un-firing and
                // re-declining (its self-signal — a root call is RSymbolReference, outside the router's
                // set) or a NEW decline family; both must be triaged, never absorbed (the
                // designed-tripwire class, the FUNCTION seam's #472/#473 pins extended seam-generically).
                assertEquals(0, guardDeclined,
                        "D43 ON-gate: the post-pin guard DECLINED on " + cell + " RULE — expected the"
                                + " fully-taught zero (a claim-root-seat un-fire or a NEW decline"
                                + " family; triage, don't absorb): " + armBreakdown);
                // (r5) The router's un-fire witness, drr ONLY (the census: the RULE seam's trip
                // population — 12 RFeatureCall + 1 REqualityExpr roots, byte-green since #472 — is all
                // drr; the other cells have zero RULE-seam trips, so served>0 cannot pin there): a
                // silent scan un-fire would zero it and readmit the #467 drift class (anti-L-042).
                // RETENSED at #516 (the delegated→serve lock lineage: guard-decline > 0 → delegated
                // > 0 → served > 0 — the serve conversion moved the router's counter, so the un-fire
                // witness reads the flip receipt).
                if ("drr".equals(cell.corpus())) {
                    int routerServed = (int) compiler.getClass()
                            .getMethod("postPinServeLoweredCount").invoke(compiler);
                    assertTrue(routerServed > 0,
                            "D43 ON-gate: the post-pin router served ZERO claims on " + cell
                                    + " RULE — the guard seat's scan silently un-fired (the #467 drift"
                                    + " class would return on the rule/report bodies)");
                }
            } catch (ReflectiveOperationException | ClassCastException e) {
                // The #467 Seat-1 OBS-4 hardening, RULE-seam edition: the provider RESOLVED (the gate
                // above), so the generator should be the IR-routed subclass exposing the readers.
                throw new AssertionError(
                        "D43 ON-gate: could not read the IR counters off the pojo-pass function"
                                + " generator (" + funcGen.getClass().getName() + ") — the provider"
                                + " resolved, so the generator should be the IR-routed subclass exposing"
                                + " irExpressionCompiler(); likely causes: the construction seam did not"
                                + " route through the provider, or a provider/harness classpath skew"
                                + " (stale rune-ir/rune-ir-java m2 jars vs this engine — reinstall both,"
                                + " then re-run -Pir-on)", e);
            }
        }

        // Content-aware ENUM exclusion (model-derived, no filename/content string-match):
        // a golden that is a RosettaEnum whose filename lacks the `Enum.java` suffix (ISO
        // *Code.java etc.) must NOT be counted as a missing POJO in Pass 2 — the fork emits
        // it correctly under ENUM kind. emittedEnumPaths is the set of canonical paths the
        // EnumGenerator emits for this cell; compareAgainstGolden routes those goldens to ENUM.
        Set<String> enumPaths = emittedEnumPaths(cell, corpus, gm);
        var results = compareAgainstGolden(cell, output, ElementKind.POJO, enumPaths);
        System.out.println("D11 " + cell + " POJO: " + results.summary());
        // v3.1 C0: the generation-error gate runs AFTER the byte receipt above, so a cell
        // whose elements REFUSED still records its matrix numbers before failing. Refusals
        // are the intended content of this gate now — measurement first, gate second.
        // The dump rides the receipt for the same reason (ladder-retirement).
        dumpPathsIfRequested(cell, ElementKind.POJO, results);
        assertNoGenErrors(cell, ElementKind.POJO, coreGenErrors);
        assertNoUnwaiveredGenErrors(cell, ElementKind.POJO, ruleFamilyGenErrors);
        assertCellKind(cell, ElementKind.POJO, results);
    }

    /**
     * THE DECLARATION-RECONCILE CHANNEL (v3.3 seat 5, decision D55). On the reference IR route the three declaration
     * generators build the enriched declaration IR for every declaration and assert EVERY fact it carries against the
     * parsed source (and a type reference against the old generator's own resolution) - this prints, per cell and kind,
     * {@code declarations attempted / expected / facts asserted / mismatches}, BEFORE any gate reads the generation errors
     * the mismatches also raise, and then holds {@code mismatches == 0} AND ITS OWN POPULATION (LAW 84):
     * {@code declarations == expected}, where {@code expected} is THIS HOST'S count of the cell's emitted declarations of
     * the kind ({@link #emittedDeclarations}) - the generator counts a declaration BEFORE its adapter runs and books a
     * throw as a mismatch, so a declaration that never reached the reconcile is RED here, never a silent drop (a cell
     * printing {@code declarations=0} over a non-empty population cannot stay green). Flag-off (and on the optimised
     * route, which builds no declaration IR) the generator carries no counters and the channel prints nothing - the OFF
     * ring's log is unmoved.
     *
     * <p>v3.3 seat 6 (PR #642 - round 1 cq NIT-4): the three assertions are BOOKED, not thrown, and this method
     * RETURNS the verdict ({@code null} when clear) for {@link #failOnDeclarationReconcile} to raise once - the
     * {@link #failOnIrFallbackVerdicts} pattern. Asserting inline made a red DATA_TYPE reconcile hide the CHOICE
     * reconcile line AND both IR-file-writer lines of the same pass, which is the one thing a measurement channel must
     * never do. The printed line's format is byte-identical (scripts parse it) and each assertion's message is kept
     * verbatim - what moved is WHEN it is raised, never what it says, and every failure of the pass is now named in one
     * error. The wiring throw above stays a throw: it fires before any line of that kind exists to hide.
     *
     * <p>v3.3 seat 7 (PR #643 - the type gate): the channel now has a FOURTH line, {@code TYPE_ALIAS}
     * ({@link #printTypeAliasReconcile}), in the SAME printed format and under the same three assertions. A
     * {@code typeAlias} declaration writes no file, so it belongs to no emitting generator's population; it rides the
     * data-type pass through a second reconciler whose counters are its own, and its {@code expected} is this host's
     * own count of the cell's {@code RTypeAlias} root elements.
     */
    private static String printDeclarationReconcile(CellSpec cell, String kind, Object generator, int expected) {
        return printDeclarationReconcile(cell, kind, "declarationReconcileStats", generator, expected);
    }

    /**
     * The same line, read off a NAMED counter accessor (v3.3 seat 8, PR #644 - PR B): one generator now carries FOUR
     * populations of its own - its declarations, its type aliases, its property surfaces and its derived facts - plus
     * the models it adapted, and each is a line of this channel. Only the accessor's NAME varies; the printed format,
     * the three assertions and every message are the four-argument form's, verbatim.
     */
    private static String printDeclarationReconcile(CellSpec cell, String kind, String statsAccessor, Object generator, int expected) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return null;
        }
        int[] stats;
        try {
            stats = (int[]) generator.getClass().getMethod(statsAccessor).invoke(generator);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 " + cell + " " + kind + ": the IR route resolved but the " + generator.getClass().getSimpleName()
                    + " exposes no declaration-reconcile counters - the reconcile is not wired", e);
        }
        System.out.println("D11 " + cell + " " + kind + " IR declaration reconcile: declarations=" + stats[0]
                + " expected=" + expected + " factsAsserted=" + stats[1] + " mismatches=" + stats[2]
                + derivedNewFactSuffix(kind, generator, stats[1]));
        List<String> red = new ArrayList<>();
        bookIfRed(red, () -> assertEquals(expected, stats[0], "D11 " + cell + " " + kind + ": the reconcile attempted " + stats[0] + " declaration(s) where the cell"
                + " emits " + expected + " - a declaration left the reconcile's population (the gate asserts its own population, LAW 84)"));
        bookIfRed(red, () -> assertEquals(0, stats[2], "D11 " + cell + " " + kind + ": the declaration IR disagrees with the source on " + stats[2]
                + " fact(s) - each is a generation error named below"));
        bookIfRed(red, () -> assertTrue(stats[0] == 0 || stats[1] > stats[0], "D11 " + cell + " " + kind + ": " + stats[0]
                + " declarations reconciled over " + stats[1] + " facts - a reconcile that asserts nothing is not a gate"));
        return red.isEmpty() ? null : String.join(System.lineSeparator(), red);
    }

    /**
     * THE DERIVED LINE'S SIX SUB-COUNTS (v3.3 seat 9, PR #645 commits 12, 13 and 14) - {@code (carried=<old> +
     * aliasConditionClasses=<n1> + castType=<n2> + qualifyFunctions=<n3> + deepPathDependencies=<n4> +
     * deepPathArms=<n5>)}, appended to the DERIVED family's line and to NO other. Commit 14 adds the last two,
     * ONE fact per validated element each, so {@code carried}, {@code n1}, {@code n2} and {@code n3} must read
     * EXACTLY the commit-13 figures of the cell.
     *
     * <p>Commit 12 added exactly two fact families to the derived reconcile and commit 13 adds exactly one more
     * (the {@code *Meta}'s qualify wing, ONE fact per validated element), so the total moves by exactly their
     * counts and by nothing else. Printing the REMAINDER makes that checkable at a glance against the commit-12
     * figures of the cell (carried 38,384 / aliasConditionClasses 2,330 / castType 2,330 on cdm/5.38.0; carried
     * 86,654 / 4,498 / 4,498 on chaos/1.1.0, READ from {@code c12-scoped-*.log}): a carried sub-count that drifts
     * is a fact this commit moved without saying so. The arithmetic is asserted too, over ALL FIVE families the
     * code sums (round 1 cq SF-1: this sentence named three while {@link #derivedNewFactSuffix} summed five since
     * commit 14) - {@code carried + aliasConditionClasses + castType + qualifyFunctions + deepPathDependencies
     * + deepPathArms == factsAsserted} - so the suffix can never be a decoration over a total it does not
     * decompose.
     *
     * <p>A generator whose counter array is SHORTER than this reader expects is a drift of the seam, not a
     * missing number: it is refused by name rather than read as zeros.
     *
     * <p>Read REFLECTIVELY, like every seam of this channel; a generator that exposes no such counters appends
     * nothing, and so does every family but DERIVED.
     */
    private static String derivedNewFactSuffix(String kind, Object generator, int factsAsserted) {
        if (!"DERIVED".equals(kind)) {
            return "";
        }
        int[] added;
        try {
            added = (int[]) generator.getClass().getMethod("derivedNewFactCounts").invoke(generator);
        } catch (NoSuchMethodException none) {
            return "";
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 DERIVED: the new-fact counters could not be read off "
                    + generator.getClass().getSimpleName(), e);
        }
        assertEquals(5, added.length, "D11 DERIVED: the new-fact counter seam answers five families at this"
                + " commit (aliasConditionClasses, castType, qualifyFunctions, deepPathDependencies,"
                + " deepPathArms) - a shorter array is a drift of the seam and would read the missing family as a"
                + " sub-count of `carried`");
        int carried = factsAsserted - added[0] - added[1] - added[2] - added[3] - added[4];
        assertEquals(factsAsserted, carried + added[0] + added[1] + added[2] + added[3] + added[4],
                "D11 DERIVED: the sub-counts must decompose factsAsserted exactly");
        return " (carried=" + carried + " + aliasConditionClasses=" + added[0]
                + " + castType=" + added[1] + " + qualifyFunctions=" + added[2]
                + " + deepPathDependencies=" + added[3] + " + deepPathArms=" + added[4] + ")";
    }

    /**
     * THE TYPE_ALIAS RECONCILE LINE (v3.3 seat 7, PR #643 - the type gate): a {@code typeAlias} declaration writes no
     * file - its name never reaches a generated byte, its collapsed BASE does - so it joins no emitting generator's
     * population. The data-type pass adapts and reconciles it through a SECOND reconciler with its own counters, and
     * this prints them in the declaration channel's own format and holds the same three: {@code mismatches == 0},
     * {@code declarations == expected} where {@code expected} is THIS HOST'S count of the cell's {@code RTypeAlias}
     * root elements under the emission filter (LAW 84 - the gate asserts its own population, read without asking the
     * generator), and a reconcile that asserts more facts than declarations. A generator that exposes no such counters
     * prints nothing ({@link #printParentReconcile}'s law); flag-off and on the optimised route nothing prints.
     */
    private static String printTypeAliasReconcile(CellSpec cell, Object generator, int expected) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return null;
        }
        int[] stats;
        try {
            stats = (int[]) generator.getClass().getMethod("typeAliasReconcileStats").invoke(generator);
        } catch (NoSuchMethodException none) {
            return null;   // a generator that reconciles no type alias has no such line
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 " + cell + " TYPE_ALIAS: the type-alias reconcile counters could not be read off "
                    + generator.getClass().getSimpleName(), e);
        }
        System.out.println("D11 " + cell + " TYPE_ALIAS IR declaration reconcile: declarations=" + stats[0]
                + " expected=" + expected + " factsAsserted=" + stats[1] + " mismatches=" + stats[2]);
        List<String> red = new ArrayList<>();
        bookIfRed(red, () -> assertEquals(expected, stats[0], "D11 " + cell + " TYPE_ALIAS: the reconcile attempted " + stats[0]
                + " declaration(s) where the cell declares " + expected
                + " - a declaration left the reconcile's population (the gate asserts its own population, LAW 84)"));
        bookIfRed(red, () -> assertEquals(0, stats[2], "D11 " + cell + " TYPE_ALIAS: the declaration IR disagrees with the source on "
                + stats[2] + " fact(s) - each is a generation error named below"));
        bookIfRed(red, () -> assertTrue(stats[0] == 0 || stats[1] > stats[0], "D11 " + cell + " TYPE_ALIAS: " + stats[0]
                + " declarations reconciled over " + stats[1] + " facts - a reconcile that asserts nothing is not a gate"));
        return red.isEmpty() ? null : String.join(System.lineSeparator(), red);
    }

    /**
     * THE PARENT-RECONCILE LINE (v3.3 seat 6, PR #642 - round 1 cq SF-1): the ENUM emitter flattens a parent chain through
     * its workspace-wide index and reconciles each parent against its OWN source once, on first resolution, through a
     * second reconciler kept apart from the pass's population (a parent may live outside the cell's emission filter, so
     * it cannot join {@code declarations == expected}). This prints {@code parents / facts asserted / mismatches} beside
     * the pass's own line and books {@code mismatches == 0} (a throw on the way is a mismatch there too) - the counters
     * would otherwise be read by no host line. Only the generator that flattens parents exposes them; the others print
     * nothing here. Flag-off nothing prints.
     */
    private static String printParentReconcile(CellSpec cell, String kind, String statsAccessor, Object generator) {
        return printParentReconcile(cell, kind, statsAccessor, generator, null);
    }

    /**
     * The parent reconcile line with a FLOOR on its population when the host can count one: {@code supertypes=} is this
     * host's own count of the distinct transitive supertypes of the cell's validated elements, and {@code parents} must
     * reach at least that many (the index walks every supertype; what it reaches beyond the floor - the item types of
     * specialized properties - is READ from the line, never asserted). A {@code null} floor prints and asserts none
     * (the ENUM parent line of PR #642). PR #644 round 1, rule6 MF-2.
     */
    private static String printParentReconcile(CellSpec cell, String kind, String statsAccessor, Object generator,
            Integer supertypesFloor) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return null;
        }
        int[] stats;
        try {
            stats = (int[]) generator.getClass().getMethod(statsAccessor).invoke(generator);
        } catch (NoSuchMethodException none) {
            return null;   // a generator that flattens no parent chain has no parent reconcile
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 " + cell + " " + kind + ": the parent-reconcile counters could not be read off "
                    + generator.getClass().getSimpleName(), e);
        }
        // the floor is printed AFTER factsAsserted= and BEFORE mismatches=, so every existing parser of this line (the
        // offload box runner and the chain read `parents=N factsAsserted=F` and anchor on `mismatches=0$`) still reads it
        System.out.println("D11 " + cell + " " + kind + " IR parent reconcile: parents=" + stats[0] + " factsAsserted=" + stats[1]
                + (supertypesFloor == null ? "" : " supertypes=" + supertypesFloor) + " mismatches=" + stats[2]);
        List<String> red = new ArrayList<>();
        if (supertypesFloor != null) {
            bookIfRed(red, () -> assertTrue(stats[0] >= supertypesFloor, "D11 " + cell + " " + kind + ": the index reconciled "
                    + stats[0] + " parents but the cell's elements have " + supertypesFloor + " distinct transitive supertypes -"
                    + " a supertype the walks never reached left the population silently"));
        }
        bookIfRed(red, () -> assertEquals(0, stats[2], "D11 " + cell + " " + kind + ": a parent the pass flattened through its index disagrees with its source on "
                + stats[2] + " fact(s) (or its adapter threw) - every child of it is refused and named below"));
        bookIfRed(red, () -> assertTrue(stats[0] == 0 || stats[1] > stats[0], "D11 " + cell + " " + kind + ": " + stats[0]
                + " parents reconciled over " + stats[1] + " facts - a reconcile that asserts nothing is not a gate"));
        return red.isEmpty() ? null : String.join(System.lineSeparator(), red);
    }

    /**
     * THE SHADOW'S ORACLE - A SECOND REAL PRODUCER, RENDERED ON THIS VERY RUN (v3.3 seat 9, PR #645 commit 18,
     * round 1 cq MF-1). Every shadow line of this host asks ONE question: does the IR unit's render of a file equal
     * the file the OLD generator writes for it? Through commit 14 the pass's own {@code output} answered the second
     * half, because on the reference route the pass still WAS the old generator. Commit 15 flipped
     * {@code IRTypeUnitWiring.AVAILABLE} and moved the writer: from that commit to commit 17 the pass's output at a
     * written key IS the unit's own text, so an oracle taken from it compared the unit with ITSELF - the
     * {@code identical} column followed by purity and a golden divergence could not turn the line red (no false
     * figure shipped: byte fidelity is held by the per-kind golden compare over the IR-written output, by both
     * rings and by {@code ROUTE ROW DIFF: NONE} - but a line the pages call a GATE must be one).
     *
     * <p>THIS renders the cell ONCE MORE with the LEGACY generator class, constructed DIRECTLY - never through
     * {@link IRGeneration}, whose construction seams answer the IR-routed subclass on the reference route - with
     * the very arguments that seam takes, and driven over the SAME models in the SAME order the pass walked. The
     * map it returns is the oracle the shadow lines take as {@code ownText}, so {@code own} is again the old
     * generator's own text for the key and {@code keyContested} again means "the golden at this path is not the
     * file the OLD generator wrote".
     *
     * <p>THE GOLDEN WAS NOT TAKEN AS THE ORACLE INSTEAD, for three reasons: (a) a key the corpus holds no golden
     * for could not be compared at all (sixteen chaos keys today), where the legacy generator renders it; (b) the
     * two chaos {@code a8pkg} keys - a {@code type} and a {@code func} sharing ONE output path - are declared in NO
     * register and would read {@code differing} with no law to hold them, where the legacy generator's own file is
     * exactly the oracle the contested-key law wants; (c) a golden oracle would make the shadow a THIRD golden
     * compare beside {@code assertCellKind} and the rings, where the shadow's whole reason to exist is the
     * TWO-PRODUCER question - the unit's render against the old code's, on the same model, in the same JVM.
     *
     * <p>The legacy generator's own {@link GenerationException}s are PRINTED (the count and the first three) and
     * NEVER asserted: they are exactly the {@code own == null} keys the shadow's {@code refusedExpected},
     * {@code renderedWhereLegacyRefused} and {@code noFileWhereLegacyWrote} columns read, and the PASS's own errors
     * for the same cell are already judged by {@link #assertNoGenErrors}.
     *
     * <p>THE ORACLE'S REFUSALS ARE NOT THIS RUN'S DEGRADATIONS. The legacy generator books every refusal it makes
     * in {@link SilentDegradation}, a JVM-GLOBAL register the D11 LOUD receipt prints and resets once per
     * (cell, kind) - so a SECOND render of the same cell books the SAME refusals a second time and they surface on
     * the NEXT kind's LOUD line. The first commit-18 chaos run read exactly that: {@code DATA_RULE LOUD:
     * BOILERPLATE_NAME_COLLISION=24} where the cell has twelve, the type-format validator's twelve witnesses
     * filling the site's twelve-witness cap so the data rule's own twelve never printed
     * ({@code scratch/c18-wt-scoped-chaos-pre-loudfix.log:68}). The register is therefore SNAPSHOT before the
     * render and PUT BACK EXACTLY after it - every retained witness replayed in its first-seen order, and each
     * count topped up to its snapshot with {@code record(site, null)}, which increments the count and touches no
     * witness set (a site can legitimately count MORE than it retains: the cap is twelve, and chaos's POJO pass
     * books {@code REPORT_REFERENCE_UNRESOLVED=24}). The put-back is ASSERTED, count for count and witness for
     * witness, so a register this helper failed to restore is RED by name and never a silent leftover. The
     * oracle's own refusals are printed on its own line above, by name, where they belong.
     *
     * <p>A no-op returning {@code null} on the OFF route and on the optimised route, exactly as the shadow
     * functions are - the supplier is not even called there, so no legacy template group is loaded and not one
     * OFF-route line moves.
     *
     * @param label the shadow line this oracle feeds, for the print
     * @param legacy a SUPPLIER of the legacy class, so the construction itself is skipped off-route
     */
    private static Map<String, String> legacyRender(CellSpec cell, String label,
                                                    Supplier<JavaClassGenerator<?, ?>> legacy,
                                                    List<RModel> models, GeneratorModel gm) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return null;
        }
        Map<SilentDegradation.Site, List<String>> witnessesBefore =
                new java.util.EnumMap<>(SilentDegradation.Site.class);
        Map<SilentDegradation.Site, Integer> countsBefore = SilentDegradation.counts();
        for (SilentDegradation.Site site : SilentDegradation.Site.values()) {
            witnessesBefore.put(site, List.copyOf(SilentDegradation.witnesses(site)));
        }
        JavaClassGenerator<?, ?> generator = legacy.get();
        Map<String, String> legacyOutput = new LinkedHashMap<>();
        List<GenerationException> refusals = new ArrayList<>();
        for (RModel model : models) {
            if (gm.shouldGenerate(model)) {
                refusals.addAll(generator.generateClasses(model, gm.version(model), legacyOutput));
            }
        }
        // THE REGISTER, PUT BACK: cleared, then re-recorded witness for witness in first-seen order and each
        // count topped up with the null witness (which increments and retains nothing - SilentDegradation:541-552),
        // so the next LOUD receipt reads this run's own activity and not the oracle's second copy of it
        SilentDegradation.reset();
        for (SilentDegradation.Site site : SilentDegradation.Site.values()) {
            List<String> retained = witnessesBefore.get(site);
            for (String witness : retained) {
                SilentDegradation.record(site, witness);
            }
            for (int i = retained.size(); i < countsBefore.get(site); i++) {
                SilentDegradation.record(site, null);
            }
        }
        assertEquals(countsBefore, SilentDegradation.counts(), "D11 " + cell + " LEGACY ORACLE[" + label
                + "]: the degradation register was NOT put back as the oracle render found it");
        for (SilentDegradation.Site site : SilentDegradation.Site.values()) {
            assertEquals(witnessesBefore.get(site), SilentDegradation.witnesses(site), "D11 " + cell
                    + " LEGACY ORACLE[" + label + "]: the " + site + " witnesses were NOT put back as the oracle"
                    + " render found them");
        }
        System.out.println("D11 " + cell + " LEGACY ORACLE[" + label + "]: files=" + legacyOutput.size()
                + " refusals=" + refusals.size() + " by " + generator.getClass().getSimpleName()
                + " - the shadow's oracle, rendered beside the unit's pass on this very run (PR #645 commit 18)");
        for (int i = 0; i < Math.min(3, refusals.size()); i++) {
            GenerationException refusal = refusals.get(i);
            System.out.println("  LEGACY ORACLE[" + label + "] refusal: " + refusal.getTargetPath() + " -> "
                    + refusal.getMessage());
        }
        return legacyOutput;
    }

    /**
     * THE POJO SHADOW LINE (v3.3 seat 9, PR #645 commit 4 - the emitter PR, the strict path). The data-type emitter
     * began UNDER CONSTRUCTION behind {@code IRTypeUnit}'s refusal, rendering sections 1-8 and 14 of the POJO and
     * refusing 9-13 by name (v3.3 seat 9, PR #645 commit 6 moved the stop from section 7 to section 9). SINCE
     * COMMIT 15 the POJO member is READY and the wiring's switch is ON, so what this line measures is the file the
     * pass WROTE; the line is that member's GATE. It asks the emitter for every validated data type of the cell
     * and prints what it managed:
     * <pre>
     * D11 &lt;cell&gt; POJO SHADOW: types=N identical=I differing=D partial=P refused=R prefixIdentical=PI prefixDiffering=PD prefixNoGolden=PG prefixNoText=PT keyContested=KC noGolden=NG firstDivergence&#123;…&#125; partialAt&#123;…&#125;
     * </pre>
     * <ul>
     *   <li><b>prefixIdentical / prefixDiffering</b> (v3.3 seat 9, PR #645 commit 5) - a PARTIAL render's prefix,
     *       the text the emitter did produce before the first stubbed section, held against the GOLDEN'S FIRST N
     *       LINES (N = the prefix's line count). A differing prefix's first differing line is mapped to its SECTION
     *       through the same {@code firstDivergence} histogram a full render's divergence enters, so one map reads
     *       every divergence this channel can see. The two columns do NOT sum to {@code partial}: a stop that
     *       carried no prefix is counted as {@code prefixNoText}, a key with no golden as {@code prefixNoGolden} -
     *       the four prefix columns sum to {@code partial}, so the population adds up and nothing is silently
     *       "not measured"; the first five differing keys print with their line and section.</li>
     *   <li><b>keyContested</b> (v3.3 seat 9, PR #645 commit 6) - an OVERLAY on the two compare columns, NOT a
     *       fifth summand: the keys whose GOLDEN is not the POJO the old generator wrote for that path, because
     *       two generators address it (the chaos {@code a8pkg} scenario: a {@code type} in a {@code .functions}
     *       namespace beside a {@code func} of the same simple name). For such a key the compare's oracle is the
     *       OLD GENERATOR'S OWN bytes for that key, taken from {@link #legacyRender}'s map - the two-producer
     *       question the shadow asks - and the contest is counted and named rather than absorbed as a divergence.
     *       ERRATUM (round 1 cq MF-1): from commit 15 to commit 17 that map was the PASS's output, which the unit
     *       itself had written, so this re-point booked an IR POJO that diverged from the golden as
     *       {@code identical} + {@code keyContested}. Commit 18 makes the oracle a second REAL producer again.</li>
     *   <li><b>identical / differing</b> - a FULL render byte-compared with the golden of the same output key (the
     *       file the OFF route matched), line endings normalised exactly as {@code compareAgainstGolden} does. A
     *       differing file's FIRST differing line is mapped to the SECTION it falls in, through the section line
     *       starts the emitter hands back with the text - so the print says WHERE the emitter diverges, not only
     *       that it does.</li>
     *   <li><b>partial</b> - the emitter stopped at a section it does not render yet, counted by that section.</li>
     *   <li><b>refused</b> - the emitter, the property model or the index refused the type by name; the first few
     *       reasons are printed verbatim, because a refusal nobody reads is a silent decline.</li>
     * </ul>
     * THE POPULATION IS ALWAYS ASSERTED ({@code types == the cell's data-type count}, LAW 84). While the POJO
     * member was not ready the line asserted THAT ALONE - it was a progress print and must never have booked a cell
     * red for the emitter being incomplete, which is what the unit's refusal already guaranteed. SINCE COMMIT 11
     * the member IS ready, so two gates ride beside the population: {@code identical + noGolden == types} (the
     * columns add up) and, since commit 18, {@code differing == 0} BY NAME with the first five divergent keys and
     * their first differing line - the sum law says the columns balance, the new one says WHICH file broke.
     *
     * <p>Read REFLECTIVELY off the generator, like every other seam of this channel, because
     * {@code rune-ir-java} is not on the standing test classpath; a generator that exposes no shadow prints nothing.
     *
     * @param ownText the OLD GENERATOR'S OWN render of this cell, from {@link #legacyRender} - a second REAL
     *     producer on this very run, never the pass's output map (round 1 cq MF-1; see {@link #legacyRender})
     */
    private static String printPojoShadow(CellSpec cell, Object generator, Map<String, String> ownText,
                                          int expected, ShadowHalf half) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return null;
        }
        String tag = "POJO SHADOW" + half.label();
        if (half.dataTypesHalf()) {
            bookUnitPopulation(cell, expected);   // v3.3 seat 9, PR #645 commit 15 - the host's own data-type count
            // the POJO pass walks every data type - ModelObjectGenerator.streamObjects:68-71 filters nothing else out
            IR_UNIT_MEMBER_POPULATION.put(cell + "|" + M_POJO.name(), expected);
        } else {
            // THE CHOICES' HALF BOOKS THE CHOICES (v3.3 seat 10, PR #646 commit 5). It booked nothing while the
            // kind switch was off, because the TYPE UNIT VERDICTS line's population is the PRODUCTION path's and
            // that path attempted no choice; the flip makes every choice of the cell the POJO member's too - this
            // half's own generator, IRChoiceObjectGenerator, routes them through the delegate's unit.
            bookUnitChoicePopulation(cell, expected);
            IR_UNIT_MEMBER_CHOICE_POPULATION.put(cell + "|" + M_POJO.name(), expected);
        }
        java.util.Map<?, ?> shadow;
        try {
            shadow = (java.util.Map<?, ?>) generator.getClass().getMethod("pojoShadowRenders").invoke(generator);
        } catch (NoSuchMethodException none) {
            return null;   // a generator with no data-type emitter behind it has no shadow line
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 " + cell + " " + tag + ": the shadow renders could not be read off "
                    + generator.getClass().getSimpleName(), e);
        }
        // v3.3 seat 9 (PR #645 commit 11): THE MEMBERS THE WIRING DECLARES READY, read off the same generator. When
        // the POJO is among them this line is no longer a progress print but the member's GATE: every type the
        // shadow rendered must be identical (the ones with no golden apart), else the line is RED (below).
        java.util.Set<?> unitReady;
        try {
            unitReady = (java.util.Set<?>) generator.getClass().getMethod("unitReady").invoke(generator);
        } catch (NoSuchMethodException none) {
            unitReady = java.util.Set.of();   // a generator with no unit behind it declares nothing ready
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 " + cell + " " + tag + ": the unit's ready members could not be read"
                    + " off " + generator.getClass().getSimpleName(), e);
        }
        boolean pojoReady = unitReady.contains("POJO");
        // THE POPULATION FILTER (v3.3 seat 10, PR #646 commit 4). The two kinds' POJO shadows live in TWO maps on
        // TWO generators - IRModelObjectGenerator's data types and IRChoiceObjectGenerator's choices - so this
        // filter is the identity on both halves today. It is applied anyway, because the SURPLUS arm beside it is
        // what holds this host's own key projection and the pass's own spelling together, and a filter that is
        // only written where it currently bites is a filter nobody has proved.
        List<String> surplus = shadowSurplus(shadow, half);
        shadow = shadowOfHalf(shadow, half);
        Path goldensDir = resolveGoldensDir(cell);
        int identical = 0;
        int differing = 0;
        int partial = 0;
        int refused = 0;
        int prefixIdentical = 0;
        int prefixDiffering = 0;
        int prefixNoGolden = 0;   // a partial render whose key has no golden: COUNTED, never folded into a compare column
        int prefixNoText = 0;     // a stop that carried no prefix text: COUNTED too - the four prefix columns sum to partial
        int keyContested = 0;     // an OVERLAY count on the two compare columns - see the contested-key law below
        int noGolden = 0;         // a WHOLE render whose key has no golden: COUNTED apart (v3.3 seat 9, PR #645 commit 9)
        List<String> contestedKeys = new ArrayList<>();
        List<String> noGoldenKeys = new ArrayList<>();
        List<String> prefixDiffs = new ArrayList<>();
        // the first five WHOLE-FILE divergences BY NAME, for the member's `differing == 0` gate (PR #645 commit 18)
        List<String> divergentKeys = new ArrayList<>();
        Map<String, Integer> firstDivergence = new java.util.TreeMap<>();
        Map<String, Integer> partialAt = new java.util.TreeMap<>();
        List<String> refusalReasons = new ArrayList<>();
        for (Map.Entry<?, ?> entry : shadow.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String[] answer = (String[]) entry.getValue();
            switch (answer[0]) {
                case "PARTIAL" -> {
                    partial++;
                    partialAt.merge(answer[1], 1, Integer::sum);
                    // THE PREFIX COMPARE (v3.3 seat 9, PR #645 commit 5): a partial render carries the text it DID
                    // produce; hold it against the golden's FIRST N lines, N being the prefix's own line count. A
                    // stop that carried no prefix, and a key with no golden, are simply not compared - they stay
                    // counted as partial and enter neither prefix column, because a column that silently absorbs
                    // "not measured" is not a measurement.
                    if (answer.length > 3) {
                        Path prefixGolden = goldensDir.resolve(key);
                        if (Files.exists(prefixGolden)) {
                            String golden;
                            try {
                                golden = normalize(Files.readString(prefixGolden));
                            } catch (IOException e) {
                                throw new AssertionError("D11 " + cell + " " + tag + ": the golden " + key
                                        + " could not be read for the prefix compare", e);
                            }
                            // THE CONTESTED KEY (v3.3 seat 9, PR #645 commit 6). Two generators can address ONE
                            // output path: the chaos a8pkg scenario declares a `type` in a namespace that ends in
                            // `.functions` beside a `func` of the same simple name in the parent namespace, so the
                            // POJO and the function generator both write <ns>/functions/<Name>.java and the corpus
                            // holds the FUNCTION's bytes. Holding the POJO emitter's prefix against THAT golden
                            // measures the wrong thing - it diverges at line 3, the first import, on every such key.
                            // The test is mechanical and needs no name list: the OLD generator's own POJO for this
                            // key - rendered by legacyRender BESIDE this pass, a second REAL producer on this very
                            // run (PR #645 commit 18, round 1 cq MF-1) - is held against the golden first. When
                            // the two disagree the golden is not the POJO's, so the oracle for the compare becomes
                            // the old generator's own bytes - the two-producer question the shadow actually asks -
                            // and the contest is COUNTED, because a key silently re-pointed is not a measurement.
                            String own = ownText.get(key);
                            String oracle = golden;
                            if (own != null && findFirstPrefixDiffLine(golden, normalize(own)) != 0) {
                                oracle = normalize(own);
                                keyContested++;
                                if (contestedKeys.size() < 5) {
                                    contestedKeys.add(key);
                                }
                            }
                            int diffLine = findFirstPrefixDiffLine(oracle, normalize(answer[2]));
                            if (diffLine == 0) {
                                prefixIdentical++;
                            } else {
                                prefixDiffering++;
                                String section = sectionOfLine(answer[3], diffLine);
                                firstDivergence.merge(section, 1, Integer::sum);
                                if (prefixDiffs.size() < 5) {
                                    prefixDiffs.add(key + " -> line " + diffLine + " (" + section + ")");
                                }
                            }
                        } else {
                            prefixNoGolden++;
                        }
                    } else {
                        prefixNoText++;
                    }
                }
                case "REFUSED" -> {
                    refused++;
                    // THE CHOICES' HALF NAMES EVERY REFUSAL (v3.3 seat 10, PR #646 commit 4, cap 200): a choice
                    // the IR refuses is the residual the C5 register re-cut has to carry BY NAME, and five of 157
                    // would name a sample of a residual nobody could act on. The data types' half keeps its five.
                    if (refusalReasons.size() < half.refusalSampleCap()) {
                        refusalReasons.add(key + " -> " + answer[1]);
                    }
                }
                case "RENDERED" -> {
                    Path goldenPath = goldensDir.resolve(key);
                    if (!Files.exists(goldenPath)) {
                        // v3.3 seat 9 (PR #645 commit 9): a WHOLE render whose key has no golden is COUNTED in its
                        // own column, never folded into `refused` - the emitter refused nothing; the corpus has
                        // nothing to hold the file against. At c9's first full-file compare the sixteen chaos keys
                        // the PARTIAL arm books as prefixNoGolden read here as `refused` - a column defect of this
                        // print, corrected FROM THAT PRINT (c9-scoped-chaos.log).
                        noGolden++;
                        if (noGoldenKeys.size() < 5) {
                            noGoldenKeys.add(key);
                        }
                        break;
                    }
                    String golden;
                    try {
                        golden = normalize(Files.readString(goldenPath));
                    } catch (IOException e) {
                        throw new AssertionError("D11 " + cell + " " + tag + ": the golden " + key
                                + " could not be read", e);
                    }
                    String generated = normalize(answer[1]);
                    // v3.3 seat 9 (PR #645 commit 9): THE CONTESTED-KEY LAW ON THE WHOLE-FILE ARM - the same law
                    // the PARTIAL arm carries since commit 6, above: when the golden at this path is not the file
                    // the OLD generator wrote for this type, the oracle for the compare is the old generator's own
                    // text and the key is counted keyContested. At c9's first full-file compare the two contested
                    // chaos keys read `differing` at HEADER on this arm - held against the FUNCTION file - a
                    // defect of this arm, corrected FROM THAT PRINT. The old generator's text is legacyRender's,
                    // not the pass's output, since PR #645 commit 18 (round 1 cq MF-1).
                    String own = ownText.get(key);
                    String oracle = golden;
                    if (own != null && findFirstPrefixDiffLine(golden, normalize(own)) != 0) {
                        oracle = normalize(own);
                        keyContested++;
                        if (contestedKeys.size() < 5) {
                            contestedKeys.add(key);
                        }
                    }
                    if (oracle.equals(generated)) {
                        identical++;
                    } else {
                        differing++;
                        int diffLine = findFirstDiffLine(oracle, generated);
                        firstDivergence.merge(
                                sectionOfLine(answer.length > 2 ? answer[2] : "", diffLine), 1, Integer::sum);
                        if (divergentKeys.size() < 5) {
                            // NAMED, not only histogrammed (v3.3 seat 9, PR #645 commit 18): the member's
                            // `differing == 0` gate must say WHICH file broke and where, not a count
                            divergentKeys.add(key + " -> line " + diffLine + " ("
                                    + sectionOfLine(answer.length > 2 ? answer[2] : "", diffLine) + ")");
                        }
                    }
                }
                default -> throw new AssertionError("D11 " + cell + " " + tag + ": the shadow answered '"
                        + answer[0] + "' for " + key
                        + " - the channel knows RENDERED / PARTIAL / REFUSED and nothing else");
            }
        }
        // the population identity, READ by the runners: identical + differing + partial + refused + noGolden == types
        int types = identical + differing + partial + refused + noGolden;
        System.out.println("D11 " + cell + " " + tag + ": " + half.countName() + "=" + types
                + " identical=" + identical
                + " differing=" + differing + " partial=" + partial + " refused=" + refused
                + " prefixIdentical=" + prefixIdentical + " prefixDiffering=" + prefixDiffering
                + " prefixNoGolden=" + prefixNoGolden + " prefixNoText=" + prefixNoText
                + " keyContested=" + keyContested + " noGolden=" + noGolden
                + " firstDivergence{" + renderCounts(firstDivergence) + "}"
                + " partialAt{" + renderCounts(partialAt) + "}");
        for (String reason : refusalReasons) {
            System.out.println("  " + tag + " refusal: " + reason);
        }
        for (String key : noGoldenKeys) {
            System.out.println("  " + tag + " no golden (a whole render the corpus holds no file for): " + key);
        }
        for (String diff : prefixDiffs) {
            System.out.println("  " + tag + " prefix divergence: " + diff);
        }
        for (String diff : divergentKeys) {
            System.out.println("  " + tag + " divergence: " + diff);
        }
        for (String contested : contestedKeys) {
            System.out.println("  " + tag + " contested key (the golden at this path is NOT the POJO the old"
                    + " generator wrote for it - two generators address one output path): " + contested);
        }
        for (int i = 0; i < Math.min(5, surplus.size()); i++) {
            System.out.println("  " + tag + " SURPLUS (a shadow entry no population of either kind claims): "
                    + surplus.get(i));
        }
        System.out.println("  " + tag + " unit ready: " + unitReady + (pojoReady
                ? " - the POJO member is READY, so this line is its GATE: differing == 0 against the old"
                        + " generator's own render, and identical + noGolden == types"
                : " - the POJO member is not ready, so this line is a progress print"));
        List<String> red = new ArrayList<>();
        final List<String> surplusRead = List.copyOf(surplus);
        bookIfRed(red, () -> assertEquals(0, surplusRead.size(), "D11 " + cell + " " + tag + ": "
                + surplusSample(surplusRead) + " - a shadow booked for nothing, or a key this host and the pass"
                + " spell differently (v3.3 seat 10, PR #646 commit 4)"));
        bookIfRed(red, () -> assertEquals(expected, types, "D11 " + cell + " " + tag + ": the emitter was asked for "
                + types + " " + half.elementName() + "(s) where the cell emits " + expected
                + " - a " + half.elementName() + " left the shadow's population (the gate asserts its own"
                + " population, LAW 84)"));
        if (pojoReady) {
            // v3.3 seat 9 (PR #645 commit 11): THE POJO MEMBER'S GATE. Every type the emitter rendered whole is
            // byte-identical to the golden (or to the old generator's own file where the golden is contested);
            // a type with no golden is counted apart and cannot be judged; anything else - a differing file, a
            // partial render, a refusal - is RED by name. The population identity holds beside it.
            //
            // THE ORACLE IS A SECOND REAL PRODUCER (v3.3 seat 9, PR #645 commit 18, round 1 cq MF-1): `ownText` is
            // the LEGACY ModelObjectGenerator's own render of this cell, made beside the pass by legacyRender - so
            // a contested key is judged against the old code's bytes and never against the unit's own. From commit
            // 15 to commit 17 it was the pass's output, which the unit had written, and the re-point booked a
            // divergence as identical + keyContested.
            final int identicalRead = identical;
            final int noGoldenRead = noGolden;
            final int differingRead = differing;
            final int partialRead = partial;
            final int refusedRead = refused;
            // BELT AND BRACES (commit 18): the sum law below says the columns balance; THIS says which file broke.
            // The two are independent - deleting either leaves the other red on a real divergence (lanes O3 / O5).
            final List<String> divergentSample = List.copyOf(divergentKeys);
            bookIfRed(red, () -> assertEquals(0, differingRead, "D11 " + cell + " " + tag + ": the POJO member is"
                    + " declared READY and " + differingRead + " of its " + types + " rendered file(s) DIFFER from"
                    + " the old generator's own render of the same key - the first " + divergentSample.size()
                    + ": " + divergentSample + " (the two-producer gate, PR #645 commit 18)"));
            bookIfRed(red, () -> assertEquals(types, identicalRead + noGoldenRead, "D11 " + cell + " " + tag + ": the"
                    + " POJO member is declared READY, so identical + noGolden must equal " + half.countName()
                    + " - read identical=" + identicalRead + " noGolden=" + noGoldenRead + " differing="
                    + differingRead + " partial=" + partialRead + " refused=" + refusedRead + " over "
                    + half.countName() + "=" + types + " (the member's gate, PR #645 commit 11)"));
        }
        return red.isEmpty() ? null : String.join(System.lineSeparator(), red);
    }

    /**
     * The SECTION a 1-based line falls in, from the {@code SECTION:line,…} map the emitter hands back with a full
     * render: the LAST section whose start is at or before the line. A line BEFORE every recorded boundary is the
     * FRAME - section 1, {@code HEADER}: the package line, the blank, the import block and its blank are section 1
     * and there is no earlier place for a line of a POJO file to be (v3.3 seat 9, PR #645 commit 6; at s9c5 the two
     * contested chaos keys diverged at line 3 and this reader answered {@code firstDivergence&#123;UNKNOWN=2&#125;}).
     * An empty or unparsable map still answers {@code UNKNOWN} rather than guessing a section: a map that names no
     * boundary states nothing.
     *
     * <p>THE LAW IS DECLARED IN {@code IRDataTypeEmitter.Rendered.sectionOfLine}, where it is unit-tested on its
     * own boundary table; this is its READER over the printed string. The two are separate copies by necessity -
     * {@code rune-ir-java} is not on this host's standing test classpath, which is why every seam of this channel
     * is reflective - and the emitter now records {@code HEADER:1} itself, so the fallback below is the belt.
     */
    private static String sectionOfLine(String sectionLineStarts, int line) {
        String found = "UNKNOWN";
        int best = Integer.MIN_VALUE;
        boolean parsed = false;
        for (String pair : sectionLineStarts.split(",")) {
            int colon = pair.lastIndexOf(':');
            if (colon <= 0) {
                continue;
            }
            int start;
            try {
                start = Integer.parseInt(pair.substring(colon + 1).trim());
            } catch (NumberFormatException malformed) {
                continue;
            }
            if (start <= line && start > best) {
                best = start;
                found = pair.substring(0, colon);
            }
            parsed = true;
        }
        if ("UNKNOWN".equals(found) && parsed) {
            return "HEADER";   // a line before every boundary is the frame - section 1 - never nowhere
        }
        return found;
    }

    /** {@code key=count,key=count} in key order - the shape the other D11 count maps print in. */
    private static String renderCounts(Map<String, Integer> counts) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            parts.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join(",", parts);
    }

    /**
     * Runs one reconcile assertion and BOOKS its failure instead of throwing it, so the rest of the pass's reconcile
     * lines still print. The assertion's own message is recorded verbatim; a non-assertion throw is a broken channel
     * and propagates.
     */
    private static void bookIfRed(List<String> red, Executable assertion) {
        try {
            assertion.execute();
        } catch (AssertionError e) {
            red.add(e.getMessage());
        } catch (Throwable t) {
            throw new AssertionError("D11 declaration reconcile: an assertion of the channel threw a non-assertion error", t);
        }
    }

    /**
     * Raises ONE {@link AssertionError} naming every red declaration-reconcile verdict of the pass, after every one of
     * the pass's reconcile lines has printed (v3.3 seat 6, PR #642 - round 1 cq NIT-4; the
     * {@link #failOnIrFallbackVerdicts} shape, kept separate so a stack trace says which channel failed).
     */
    private static void failOnDeclarationReconcile(String... verdicts) {
        List<String> red = new ArrayList<>();
        for (String verdict : verdicts) {
            if (verdict != null) {
                red.add(verdict);
            }
        }
        if (!red.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), red));
        }
    }

    /**
     * THIS HOST'S OWN count of the declarations of {@code kind} the cell emits: the root elements of every model the
     * emission filter admits - the population the declaration reconcile must have attempted, read without asking it.
     */
    /**
     * THIS HOST'S OWN count of the distinct declarations reachable as a supertype - transitively, through the linker's own
     * {@code superType()} / {@code choiceSuperType()} links - from the cell's validated data types: the FLOOR of the
     * {@code PROPERTY IR parent reconcile} population (LAW 84; PR #644 round 1, rule6 MF-2). A parent outside the cell's
     * emission filter counts: the index reconciles it wherever it lives.
     */
    private static int transitiveSupertypes(Iterable<RModel> models, GeneratorModel gm) {
        Set<com.regnosys.rosetta.ast.RNode> reached = new java.util.HashSet<>();
        for (RModel model : models) {
            if (!gm.shouldGenerate(model)) {
                continue;
            }
            for (var element : model.rootElements()) {
                if (element instanceof com.regnosys.rosetta.ast.types.RDataType dataType) {
                    com.regnosys.rosetta.ast.types.RDataType current = dataType;
                    while (current != null) {
                        var choice = current.choiceSuperType();
                        if (choice.isPresent()) {
                            reached.add(choice.get());   // a choice has no supertype of its own: the chain ends here
                            break;
                        }
                        var parent = current.superType();
                        if (parent.isEmpty() || !reached.add(parent.get())) {
                            break;   // no parent, or one already walked (with its own ancestors)
                        }
                        current = parent.get();
                    }
                }
            }
        }
        return reached.size();
    }

    private static int emittedDeclarations(Iterable<RModel> models, GeneratorModel gm, Class<?> kind) {
        int n = 0;
        for (RModel model : models) {
            if (gm.shouldGenerate(model)) {
                for (var element : model.rootElements()) {
                    if (kind.isInstance(element)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /**
     * THIS HOST'S OWN count of the models the cell emits - the population the MODEL reconcile must have attempted, read
     * without asking the generator (LAW 84; v3.3 seat 8, PR #644).
     */
    private static int emittedModels(Iterable<RModel> models, GeneratorModel gm) {
        int n = 0;
        for (RModel model : models) {
            if (gm.shouldGenerate(model)) {
                n++;
            }
        }
        return n;
    }

    /**
     * THE WRAPPER SPEC RECONCILE LINE (v3.3 seat 8, PR #644 - PR B, the property gate). The metafield wrapper set is a
     * fact of the whole CELL and not of a declaration: a spec is claimed by the FIRST of four sources - a declaration's
     * attribute, a choice option, a function input / output, a {@code with-meta} expression - to reach its dedup key. So
     * the generator derives the set from the IR once, lazily, after the pass, and this prints it in the declaration
     * channel's own shape with {@code specs=} in place of {@code declarations=}, because the unit is a spec. FOUR booked
     * assertions: the generator's own {@code expected} equals THIS HOST'S (the host recomputes
     * {@code new MetaFieldGenerator(gm, tt).collectSpecs().size()} for itself rather than taking the generator's word -
     * LAW 84), the population {@code specs == expected}, {@code mismatches == 0}, and a reconcile that asserts more facts
     * than specs. Flag-off and on the optimised route nothing prints.
     */
    private static String printWrapperReconcile(CellSpec cell, Object generator, int expected) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return null;
        }
        int[] stats;
        int declared;
        try {
            stats = (int[]) generator.getClass().getMethod("wrapperReconcileStats").invoke(generator);
            declared = ((Integer) generator.getClass().getMethod("wrapperExpected").invoke(generator)).intValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 " + cell + " WRAPPER: the IR route resolved but the " + generator.getClass().getSimpleName()
                    + " exposes no wrapper-reconcile counters - the reconcile is not wired", e);
        }
        System.out.println("D11 " + cell + " WRAPPER IR spec reconcile: specs=" + stats[0]
                + " expected=" + expected + " factsAsserted=" + stats[1] + " mismatches=" + stats[2]);
        List<String> red = new ArrayList<>();
        bookIfRed(red, () -> assertEquals(expected, declared, "D11 " + cell + " WRAPPER: the generator reports " + declared
                + " collected spec(s) where this host counts " + expected
                + " - the two halves are not reading the same MetaFieldGenerator.collectSpecs()"));
        bookIfRed(red, () -> assertEquals(expected, stats[0], "D11 " + cell + " WRAPPER: the reconcile compared " + stats[0]
                + " spec(s) where the cell collects " + expected
                + " - a wrapper left the reconcile's population (the gate asserts its own population, LAW 84)"));
        bookIfRed(red, () -> assertEquals(0, stats[2], "D11 " + cell + " WRAPPER: the wrapper spec set the IR derives disagrees with the"
                + " old generator's on " + stats[2] + " fact(s) - each is named below"));
        bookIfRed(red, () -> assertTrue(stats[0] == 0 || stats[1] > stats[0], "D11 " + cell + " WRAPPER: " + stats[0]
                + " specs reconciled over " + stats[1] + " facts - a reconcile that asserts nothing is not a gate"));
        return red.isEmpty() ? null : String.join(System.lineSeparator(), red);
    }

    /**
     * The once-per-cell WRAPPER reconcile's mismatch messages, read off the generator by name. Empty when the route is
     * off, when the generator carries no such reconcile, or when the set agrees (v3.3 seat 8, PR #644).
     */
    @SuppressWarnings("unchecked")
    private static List<String> wrapperMismatches(CellSpec cell, Object generator) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return List.of();
        }
        try {
            return (List<String>) generator.getClass().getMethod("wrapperMismatches").invoke(generator);
        } catch (NoSuchMethodException none) {
            return List.of();   // a generator that derives no wrapper set has none to name
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 " + cell + " WRAPPER: the wrapper mismatches could not be read off "
                    + generator.getClass().getSimpleName(), e);
        }
    }

    /**
     * THE DERIVED LINE'S THIRD READ (v3.3 seat 8, PR #644 - PR B; PLAN § F.2 Q6, the maintainer's answer (c)). The
     * derived reconcile's two halves are both WALKS - the IR's own and the reconciler's independent re-walk of the old
     * generator's law - and two walks that agree can still both be wrong about what the generator WRITES. So a THIRD
     * read is taken from the generated TEXT itself: the three validator generators are driven over the cell's emitted
     * models into a map of their own (never the pass's {@code output} - a file has ONE writer), each element's validator
     * path is recomputed exactly as {@code JavaClassGenerator.generateClasses} writes it
     * ({@code JavaTypeTranslator.toTypeFormatValidatorClass} / {@code toValidatorClass} / {@code toOnlyExistsValidatorClass},
     * canonical name, forward slashes, {@code .java}), and the LITERAL TOKENS the generator SPELLS are counted -
     * {@code checkString("} / {@code checkNumber("} for the type-format family, {@code checkCardinality("} for
     * cardinality, {@code .isSet((} for only-exists. This is a fixed-string count over generated text, NOT a structural
     * parse of it: the house rule forbids regex structural analysis of language content, and a token count infers no
     * structure - it is the seat-8 probe's own TWO-READS idiom, whose residue closed at 0 on all 26 cells (the chaos
     * cell's 164 walked / 74 written / 90 withheld by 12 named refusals).
     *
     * <p>The IR side is {@code derivedCheckCounts()} - the reconciler's own count of the checks it believes are emitted,
     * with the type-format leg taken over the elements it judges NOT refused (a {@code java.lang} collision, or a
     * parameterised alias carrying a condition, withholds a whole file). A DISAGREE is booked red, as is a written
     * validator file that no element's recomputed path claims, or a path two elements claim - either would make the
     * text read a fiction. Flag-off and on the optimised route nothing prints and nothing is generated.
     */
    private static String printDerivedTwoReads(CellSpec cell, Iterable<RModel> models, GeneratorModel gm, Object generator) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return null;
        }
        int[] irWalk;
        Map<?, ?> irTypeFormatByElement;
        try {
            irWalk = (int[]) generator.getClass().getMethod("derivedCheckCounts").invoke(generator);
            irTypeFormatByElement = (Map<?, ?>) generator.getClass().getMethod("derivedTypeFormatChecksByElement").invoke(generator);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 " + cell + " DERIVED: the IR route resolved but the " + generator.getClass().getSimpleName()
                    + " exposes no derived check counters - the third read has nothing to hold", e);
        }
        var typeFormatGen = new com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator(
                gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var cardinalityGen = new com.regnosys.rosetta.generator.java.object.validators.CardinalityValidatorGenerator(
                gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var onlyExistsGen = new com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator(
                gm, TYPE_TRANSLATOR, TYPE_UTIL);
        Map<String, String> typeFormatText = new LinkedHashMap<>();
        Map<String, String> cardinalityText = new LinkedHashMap<>();
        Map<String, String> onlyExistsText = new LinkedHashMap<>();
        // a refusal writes NO file, so its checks are absent from the text read. The walk side must withhold them too,
        // or the line disagrees; the refusals are counted here so a DISAGREE names its likeliest cause rather than
        // leaving the reader to find it (chaos/1.1.0: 12 BOILERPLATE_NAME_COLLISION refusals = 90 type-format checks)
        int[] refusedFiles = new int[3];
        // the REFUSED type-format validators by their own target paths: each withholds a WHOLE file, so the IR walk's
        // count for that element is subtracted below and the text is held against the remainder - the decode probe's
        // closure (text + refusedChecks == irWalk; chaos/1.1.0: 74 + 90 == 164 over 12 refused files)
        Set<String> refusedTypeFormatPaths = new java.util.HashSet<>();
        for (RModel model : models) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                for (GenerationException refusal : typeFormatGen.generateClasses(model, version, typeFormatText)) {
                    refusedFiles[0]++;
                    if (refusal.getTargetPath() != null) {
                        refusedTypeFormatPaths.add(refusal.getTargetPath().replace('\\', '/'));
                    }
                }
                refusedFiles[1] += cardinalityGen.generateClasses(model, version, cardinalityText).size();
                refusedFiles[2] += onlyExistsGen.generateClasses(model, version, onlyExistsText).size();
            }
        }
        int[] text = new int[3];
        Set<String> typeFormatSeen = new java.util.HashSet<>();
        Set<String> cardinalitySeen = new java.util.HashSet<>();
        Set<String> onlyExistsSeen = new java.util.HashSet<>();
        int claimedTwice = 0;
        int refusedChecks = 0;
        int refusedElementsUnwalked = 0;
        for (RModel model : models) {
            if (!gm.shouldGenerate(model)) {
                continue;
            }
            for (var element : model.rootElements()) {
                String name;
                if (element instanceof com.regnosys.rosetta.ast.types.RDataType dataType) {
                    name = dataType.name();
                } else if (element instanceof com.regnosys.rosetta.ast.types.RChoice choice) {
                    name = choice.name();
                } else {
                    continue;
                }
                var id = new com.rosetta.model.lib.ModelSymbolId(gm.namespace(element), name);
                String typeFormatPath = pathOf(TYPE_TRANSLATOR.toTypeFormatValidatorClass(id));
                if (refusedTypeFormatPaths.contains(typeFormatPath)) {
                    Object walked = irTypeFormatByElement.get(gm.namespace(element) + "." + name);
                    if (walked instanceof Integer count) {
                        refusedChecks += count;
                    } else {
                        refusedElementsUnwalked++;
                    }
                }
                claimedTwice += countWritten(typeFormatText, typeFormatPath,
                        typeFormatSeen, text, 0, "checkString(\"", "checkNumber(\"");
                claimedTwice += countWritten(cardinalityText, pathOf(TYPE_TRANSLATOR.toValidatorClass(id)),
                        cardinalitySeen, text, 1, "checkCardinality(\"");
                claimedTwice += countWritten(onlyExistsText, pathOf(TYPE_TRANSLATOR.toOnlyExistsValidatorClass(id)),
                        onlyExistsSeen, text, 2, ".isSet((");
            }
        }
        int walkEmitted = irWalk[0] - refusedChecks;
        boolean agree = text[0] == walkEmitted && text[1] == irWalk[1] && text[2] == irWalk[2]
                && refusedElementsUnwalked == 0;
        System.out.println("D11 " + cell + " DERIVED TWO-READS: typeFormat text=" + text[0] + " irWalk=" + irWalk[0]
                + " refusedChecks=" + refusedChecks + " walkEmitted=" + walkEmitted
                + " refusedFiles=" + refusedFiles[0]
                + " cardinality text=" + text[1] + " irWalk=" + irWalk[1]
                + " onlyExists text=" + text[2] + " irWalk=" + irWalk[2]
                + " -> " + (agree ? "AGREE" : "DISAGREE"));
        int unclaimed = (typeFormatText.size() - typeFormatSeen.size())
                + (cardinalityText.size() - cardinalitySeen.size())
                + (onlyExistsText.size() - onlyExistsSeen.size());
        int twice = claimedTwice;
        List<String> red = new ArrayList<>();
        final int refusedChecksRead = refusedChecks;
        final int unwalkedRead = refusedElementsUnwalked;
        bookIfRed(red, () -> assertTrue(agree, "D11 " + cell + " DERIVED: the checks the IR walk counts and the checks the"
                + " generator WROTE disagree - typeFormat " + irWalk[0] + " walked less " + refusedChecksRead
                + " withheld by refused files = " + walkEmitted + " vs " + text[0] + " (" + unwalkedRead
                + " refused element(s) the IR walk never recorded), cardinality " + irWalk[1]
                + " vs " + text[1] + ", onlyExists " + irWalk[2] + " vs " + text[2]
                + " (two walks that agree can still both be wrong about the bytes - this is the read that says so);"
                + " the cell's validator generators REFUSED typeFormat " + refusedFiles[0] + " / cardinality " + refusedFiles[1]
                + " / onlyExists " + refusedFiles[2] + " file(s), and a refused file writes none of its checks - a walk that"
                + " does not withhold the same checks by the same named refusal lands here"));
        bookIfRed(red, () -> assertEquals(0, unclaimed, "D11 " + cell + " DERIVED: " + unclaimed + " validator file(s) the cell"
                + " WROTE are claimed by no element's recomputed path - the third read is counting a different population"));
        bookIfRed(red, () -> assertEquals(0, twice, "D11 " + cell + " DERIVED: " + twice + " validator path(s) were claimed by more"
                + " than one element - a path collision would double-count the written text"));
        return red.isEmpty() ? null : String.join(System.lineSeparator(), red);
    }

    /**
     * Adds the literal-token count of the file at {@code path} into {@code text[slot]}, once. Returns 1 when the path was
     * already claimed by another element (a collision the caller books red), 0 otherwise; a path the cell did not write
     * (a refused validator) contributes nothing and claims nothing.
     */
    private static int countWritten(Map<String, String> written, String path, Set<String> seen, int[] text, int slot,
                                    String... tokens) {
        String body = written.get(path);
        if (body == null) {
            return 0;
        }
        if (!seen.add(path)) {
            return 1;
        }
        for (String token : tokens) {
            text[slot] += countToken(body, token);
        }
        return 0;
    }

    /** A literal token count over generated text - a fixed string the generator SPELLS, never a parse. */
    private static int countToken(String text, String token) {
        int n = 0;
        int i = text.indexOf(token);
        while (i >= 0) {
            n++;
            i = text.indexOf(token, i + token.length());
        }
        return n;
    }

    /** The output key {@code JavaClassGenerator.generateClasses} writes: canonical name, forward slashes, {@code .java}. */
    private static String pathOf(com.rosetta.util.types.JavaTypeDeclaration<?> declaration) {
        return declaration.getCanonicalName().withForwardSlashes() + ".java";
    }

    /**
     * The keys {@code output} gained since {@code seen} was last brought up to date, booked to {@code sink} (null = only
     * catch up). {@code seen} keeps each key's value AS FIRST WRITTEN: a key whose value is no longer that very instance
     * was RE-WRITTEN by a second generator call - the first writer would keep the attribution and the fallback gate could
     * not see the overwrite, so it is REFUSED here.
     */
    private static void collectAdded(Map<String, String> output, Map<String, String> seen, Set<String> sink) {
        for (Map.Entry<String, String> entry : output.entrySet()) {
            String first = seen.putIfAbsent(entry.getKey(), entry.getValue());
            if (first == null) {
                if (sink != null) {
                    sink.add(entry.getKey().replace('\\', '/'));
                }
            } else if (first != entry.getValue()) {
                throw new AssertionError("D11 one-writer law (every route): " + entry.getKey() + " was RE-WRITTEN by a second generator call - a file"
                        + " has ONE writer, or its attribution on the fallback register means nothing");
            }
        }
    }


    // ------------------------------------------------------------------- v3.3 seat 9 (PR #645 commit 12): THE UNIT

    /**
     * ONE member of the TYPE UNIT, as a PATH LAW and a name. The law is declared in
     * {@code IRTypeUnit.Member} ({@code rune-ir-java}); it is restated here because that module is NOT on this
     * host's standing test classpath - every seam of this channel is reflective for the same reason. The sub-package
     * segments are literal because none of the four ({@code validation}, {@code exists}, {@code meta}, {@code util})
     * is a Java keyword, so {@code JavaPackageName.escape} is the identity on every one of them; a segment that ever
     * needed escaping would have to be escaped here too.
     */
    private record UnitMember(String name, List<String> subPackage, String suffix) {

        /** The member's output key for the type whose POJO path is {@code pojoPath}. */
        String projectFrom(String pojoPath) {
            String normalized = pojoPath.replace('\\', '/');
            int slash = normalized.lastIndexOf('/');
            String directory = slash >= 0 ? normalized.substring(0, slash + 1) : "";
            String simple = normalized.substring(slash + 1, normalized.length() - ".java".length());
            StringBuilder key = new StringBuilder(directory);
            for (String segment : subPackage) {
                key.append(segment).append('/');
            }
            return key.append(simple).append(suffix).append(".java").toString();
        }
    }

    private static final UnitMember M_POJO = new UnitMember("POJO", List.of(), "");
    private static final UnitMember M_TYPE_FORMAT =
            new UnitMember("TYPE_FORMAT_VALIDATOR", List.of("validation"), "TypeFormatValidator");
    private static final UnitMember M_CARDINALITY =
            new UnitMember("CARDINALITY_VALIDATOR", List.of("validation"), "Validator");
    private static final UnitMember M_ONLY_EXISTS =
            new UnitMember("ONLY_EXISTS_VALIDATOR", List.of("validation", "exists"), "OnlyExistsValidator");
    private static final UnitMember M_META = new UnitMember("META", List.of("meta"), "Meta");
    private static final UnitMember M_DEEP_PATH = new UnitMember("DEEP_PATH_UTIL", List.of("util"), "DeepPathUtil");

    /**
     * WHICH KIND'S HALF of a shadow line one call prints (v3.3 seat 10, PR #646 commit 4). A {@code choice} and a
     * {@code data} type share ONE pass, ONE unit and ONE shadow map on all six members, so each line reads that
     * map at ITS OWN kind's keys and prints its own columns: the DATA TYPES' half keeps the existing label and
     * every existing column UNMOVED - an unmoved print is this commit's byte-inert proof on the print side - and
     * the CHOICES' half prints the SAME column set under a {@code CHOICES} label beside it.
     *
     * @param label the label suffix: {@code ""} for the data types, {@code " CHOICES"} for the choices
     * @param ownKeys the HOST'S OWN projection of THIS kind's population to the member's output keys - never a key
     *     or a count read off a generator (LAW 84). It carries the {@code "<ns>.<Name> (no output key)"} shape too,
     *     which is what a pass books for a declaration whose node or key could not be formed
     * @param otherKeys the same projection for the OTHER kind, so the line can name a key in the pass's map that
     *     NO population of either kind claims - a shadow booked for nothing
     * @param dataTypesHalf whether this is the DATA TYPES' half. Each half books ITS OWN kind's population for
     *     the {@code TYPE UNIT VERDICTS} line - the data types' into {@link #IR_UNIT_POPULATION} /
     *     {@link #IR_UNIT_MEMBER_POPULATION}, the choices' into {@link #IR_UNIT_CHOICE_POPULATION} /
     *     {@link #IR_UNIT_MEMBER_CHOICE_POPULATION} (v3.3 seat 10, PR #646 commit 5). Until the kind switch
     *     flipped only the data types' half booked: that line's population is the PRODUCTION path's, and the
     *     production path attempted no choice. It attempts every one of them now, so both halves book and the
     *     line reads {@code attempted=N of dataTypes=A choices=B}
     */
    private record ShadowHalf(String label, Set<String> ownKeys, Set<String> otherKeys,
                              boolean dataTypesHalf) {

        /** The FIRST column's name: {@code types} for the data types, {@code choices} for the choices. */
        String countName() {
            return label.isEmpty() ? "types" : "choices";
        }

        /** The element this half counts, for an assertion message that names what left the population. */
        String elementName() {
            return label.isEmpty() ? "data type" : "choice";
        }

        /**
         * How many refusals the line NAMES. The data types' half keeps its five; the CHOICES' half names every
         * one up to 200, because a choice the IR refuses is the residual the C5 register re-cut has to carry BY
         * NAME and five of 157 would be a sample of a residual nobody could act on.
         */
        int refusalSampleCap() {
            return label.isEmpty() ? 5 : 200;
        }
    }

    /**
     * THE TWO HALVES of ONE member's shadow line on one cell (v3.3 seat 10, PR #646 commit 4), each with the
     * host's own source-half key projection of its kind and the other kind's beside it. Index 0 is the DATA
     * TYPES' half (the existing line, its label and its columns unmoved), index 1 the CHOICES'.
     */
    private static ShadowHalf[] shadowHalves(Iterable<RModel> models, GeneratorModel gm, UnitMember member) {
        Set<String> dataTypes = memberKeys(models, gm, com.regnosys.rosetta.ast.types.RDataType.class, member);
        Set<String> choices = memberKeys(models, gm, com.regnosys.rosetta.ast.types.RChoice.class, member);
        return new ShadowHalf[] {
                new ShadowHalf("", dataTypes, choices, true),
                new ShadowHalf(" CHOICES", choices, dataTypes, false)};   // index 1: the CHOICES' half
    }

    /**
     * THE MEMBER'S OUTPUT KEYS for every declaration of ONE element kind under the cell's emission filter (v3.3
     * seat 10, PR #646 commit 4) - this HOST'S own SOURCE-half read, through the TRANSLATOR'S OWN per-member class
     * law, which is the very law the LEGACY generators address their files by. Never a key read off a generator:
     * the whole point of the filter is that the two halves of a shared map are decided by the host's count of the
     * source, not by what the pass happened to book.
     *
     * <p>The {@code "<ns>.<Name> (no output key)"} shape rides with the real keys because that is what a pass books
     * for a declaration whose node or key could not be formed ({@code IRUnitPass.shadow},
     * {@code IRChoiceObjectGenerator.shadowChoicePojos}), and such a declaration is still this kind's - it must not
     * fall into the surplus arm and read as "a shadow booked for nothing".
     */
    private static Set<String> memberKeys(Iterable<RModel> models, GeneratorModel gm, Class<?> kind,
                                          UnitMember member) {
        Set<String> keys = new java.util.TreeSet<>();
        for (RModel model : models) {
            if (!gm.shouldGenerate(model)) {
                continue;
            }
            for (var element : model.rootElements()) {
                String name;
                if (element instanceof com.regnosys.rosetta.ast.types.RDataType dataType) {
                    name = dataType.name();
                } else if (element instanceof com.regnosys.rosetta.ast.types.RChoice choice) {
                    name = choice.name();
                } else {
                    continue;
                }
                if (!kind.isInstance(element)) {
                    continue;
                }
                keys.add(memberKeyOf(member, new com.rosetta.model.lib.ModelSymbolId(
                        gm.namespace(element), name)));
                keys.add(model.namespace() + "." + name + " (no output key)");
            }
        }
        return keys;
    }

    /**
     * ONE member's output key for one declaration's symbol id - the TRANSLATOR'S own class law per member
     * ({@code JavaTypeTranslator:241-290}, {@code :458-465}), and for the POJO the package-escape law
     * {@code JavaClassGenerator.generateClasses} writes a pojo under ({@code JavaPackageName.escape} of the
     * declaring namespace, then the bare simple name). The translator has no {@code toPojoClass(ModelSymbolId)}
     * overload, which is why the POJO arm spells the escape itself; it is the SAME law
     * {@code IRTypeUnit.outputKey} takes, and the surplus arm of every shadow line is what holds the two together.
     */
    private static String memberKeyOf(UnitMember member, com.rosetta.model.lib.ModelSymbolId id) {
        if (M_POJO.name().equals(member.name())) {
            return com.regnosys.rosetta.generator.java.scoping.JavaPackageName.escape(id.getNamespace()).getName()
                    .child(id.getName()).withForwardSlashes() + ".java";
        }
        if (M_TYPE_FORMAT.name().equals(member.name())) {
            return pathOf(TYPE_TRANSLATOR.toTypeFormatValidatorClass(id));
        }
        if (M_CARDINALITY.name().equals(member.name())) {
            return pathOf(TYPE_TRANSLATOR.toValidatorClass(id));
        }
        if (M_ONLY_EXISTS.name().equals(member.name())) {
            return pathOf(TYPE_TRANSLATOR.toOnlyExistsValidatorClass(id));
        }
        if (M_META.name().equals(member.name())) {
            return pathOf(TYPE_TRANSLATOR.toJavaMetaDataClass(id));
        }
        if (M_DEEP_PATH.name().equals(member.name())) {
            return pathOf(TYPE_TRANSLATOR.toDeepPathUtilJavaClass(id));
        }
        throw new AssertionError("the type unit has no member named " + member.name()
                + " - the six are POJO and the five derived kinds");
    }

    /**
     * THE SURPLUS ARM of a shadow line's population filter (v3.3 seat 10, PR #646 commit 4): the keys the pass
     * booked that NEITHER kind's population claims. Zero is the only lawful answer - a shadow entry for a
     * declaration the cell does not emit is a measurement of nothing, and a key spelled differently by the pass
     * and by this host would silently leave one of the two halves short.
     */
    private static List<String> shadowSurplus(java.util.Map<?, ?> shadow, ShadowHalf half) {
        List<String> surplus = new ArrayList<>();
        for (Object key : shadow.keySet()) {
            String k = String.valueOf(key);
            if (!half.ownKeys().contains(k) && !half.otherKeys().contains(k)) {
                surplus.add(k);
            }
        }
        return surplus;
    }

    /** The pass's shadow entries at THIS half's keys alone, in the pass's own booking order. */
    private static Map<String, String[]> shadowOfHalf(java.util.Map<?, ?> shadow, ShadowHalf half) {
        Map<String, String[]> own = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : shadow.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (half.ownKeys().contains(key)) {
                own.put(key, (String[]) entry.getValue());
            }
        }
        return own;
    }

    /** A surplus list, as the assertion message names it: the first five keys and the count. */
    private static String surplusSample(List<String> surplus) {
        return surplus.size() + " key(s) the pass booked that no population of either kind claims, the first "
                + Math.min(5, surplus.size()) + ": " + surplus.subList(0, Math.min(5, surplus.size()));
    }

    /** The five derived kinds whose (cell, kind) projections must all be judged on a whole ON-route run. */
    private static final List<String> IR_DERIVED_KINDS =
            List.of("ONLY_EXISTS", "CARDINALITY", "TYPE_FORMAT", "XMETA", "DEEP_PATH");
    private static final Set<String> IR_DERIVED_KIND_KEYS_SEEN = ConcurrentHashMap.newKeySet();
    /**
     * {@code "<cell>|<member>" -> [the attempted type names, the refused type names]} - the six passes' verdict
     * sets, as SETS (v3.3 seat 9, PR #645 commit 15; they were their {@code toString()}s while every one of them
     * was empty, which no longer serves: the passes' populations are not all the same, so the comparison is a set
     * algebra now, not a string equality).
     */
    private static final Map<String, List<Set<String>>> IR_UNIT_VERDICTS = new ConcurrentHashMap<>();
    /**
     * {@code "<cell>|<member>" -> the number of types THAT PASS's own generator walks} (v3.3 seat 9, PR #645
     * commit 15) - the host's own count, from the cell's emission filter and, where the pass's
     * {@code streamObjects} is narrower than the data types, the host's own SOURCE-half read of what it filters
     * out. FIVE of the six passes walk every data type; the DEEP_PATH pass walks the ELIGIBLE ones alone
     * ({@code DeepPathUtilGenerator.streamObjects:113-118} filters on {@code DeepPathScan.isEligible}), so its
     * unit is asked for those and no others. The distinction was invisible while the unit was UNAVAILABLE - every
     * attempted set was empty - and it is the first thing the switch makes visible.
     */
    private static final Map<String, Integer> IR_UNIT_MEMBER_POPULATION = new ConcurrentHashMap<>();
    /**
     * THE WIRING'S TWO FACTS PER CELL (v3.3 seat 9, PR #645 commit 14), as {@code <readyCount>|<available>} -
     * a SET, so six passes reading one declaration that somehow disagreed would be named rather than averaged.
     */
    private static final Map<String, Set<String>> IR_UNIT_READY = new ConcurrentHashMap<>();
    /**
     * THE CELL'S OWN DATA-TYPE POPULATION (v3.3 seat 9, PR #645 commit 15), as a SET of the counts the six shadow
     * lines were handed - {@code emittedDeclarations(..., RDataType.class)}, this HOST'S own count under the cell's
     * emission filter, never a figure read off a generator. The {@code TYPE UNIT VERDICTS} line asserts the passes'
     * {@code attempted} against it: while the unit was UNAVAILABLE every attempted set was empty and the
     * cross-pass equality was vacuously true, and the commit that switches the unit on is the commit that has to
     * say the sets are FULL (LAW 84 - the gate asserts its own population). A set, so six passes handed different
     * counts would be named rather than averaged.
     */
    private static final Map<String, Set<String>> IR_UNIT_POPULATION = new ConcurrentHashMap<>();
    /**
     * THE CELL'S OWN CHOICE POPULATION (v3.3 seat 10, PR #646 commit 5), beside {@link #IR_UNIT_POPULATION} and on
     * the same law: a SET of the counts the six shadow lines' CHOICES halves were handed -
     * {@code emittedDeclarations(..., RChoice.class)}, this HOST'S own count under the cell's emission filter. The
     * {@code TYPE UNIT VERDICTS} line adds it to the data types to judge every pass's attempted set, because the
     * route writes both kinds since the kind switch flipped.
     */
    private static final Map<String, Set<String>> IR_UNIT_CHOICE_POPULATION = new ConcurrentHashMap<>();
    /** {@code "<cell>|<member>" -> the CHOICES that pass's own generator walks} - beside {@link #IR_UNIT_MEMBER_POPULATION}. */
    private static final Map<String, Integer> IR_UNIT_MEMBER_CHOICE_POPULATION = new ConcurrentHashMap<>();
    /**
     * THE CELL'S CHOICE QUALIFIED NAMES (v3.3 seat 10, PR #646 commit 4), this host's own read of the source under
     * the emission filter. The {@code TYPE UNIT VERDICTS} line asserted that NO pass's ATTEMPTED set contained one
     * of them while {@code IRTypeUnitWiring.CHOICE_AVAILABLE} was off; SINCE PR #646 commit 5 IT ASSERTS THE
     * OPPOSITE - every full-population pass attempted EVERY one of them, by name. Either way it is a proof by
     * NAME, never a count, which would still balance if a choice had quietly replaced a data type.
     */
    private static final Map<String, Set<String>> IR_CELL_CHOICE_NAMES = new ConcurrentHashMap<>();

    /** The cell's choice qualified names ({@code namespace.Name}, the IR's own spelling), under its emission filter. */
    private static Set<String> choiceNames(Iterable<RModel> models, GeneratorModel gm) {
        Set<String> names = new java.util.TreeSet<>();
        for (RModel model : models) {
            if (!gm.shouldGenerate(model)) {
                continue;
            }
            for (var element : model.rootElements()) {
                if (element instanceof com.regnosys.rosetta.ast.types.RChoice choice) {
                    names.add(model.namespace() + "." + choice.name());
                }
            }
        }
        return names;
    }

    /** Book one pass's view of the cell's DATA-TYPE population - the host's own count, per {@link #IR_UNIT_POPULATION}. */
    private static void bookUnitPopulation(CellSpec cell, int dataTypes) {
        IR_UNIT_POPULATION.computeIfAbsent(cell.toString(), c -> ConcurrentHashMap.newKeySet())
                .add(String.valueOf(dataTypes));
    }

    /**
     * Book one pass's view of the cell's CHOICE population (v3.3 seat 10, PR #646 commit 5) - the host's own
     * count, beside the data types'. A SET for the same reason: six passes handed different counts are named
     * rather than averaged.
     */
    private static void bookUnitChoicePopulation(CellSpec cell, int choices) {
        IR_UNIT_CHOICE_POPULATION.computeIfAbsent(cell.toString(), c -> ConcurrentHashMap.newKeySet())
                .add(String.valueOf(choices));
    }

    /**
     * ONE PASS'S WHOLE POPULATION (v3.3 seat 10, PR #646 commit 5): the data types it walks PLUS the choices it
     * walks, each the host's own source-half count under the cell's emission filter, each booked by that kind's
     * own half of the pass's shadow line. {@code null} when either half never booked - a pass whose population
     * this host cannot count is a pass whose attempted set it may not judge.
     */
    private static Integer memberPopulation(String cell, String member) {
        Integer dataTypes = IR_UNIT_MEMBER_POPULATION.get(cell + "|" + member);
        Integer choices = IR_UNIT_MEMBER_CHOICE_POPULATION.get(cell + "|" + member);
        return dataTypes == null || choices == null ? null : Integer.valueOf(dataTypes + choices);
    }

    /**
     * THE FIVE DERIVED KINDS' IR FILE-WRITER LINE, under the OWNER-ROW PROJECTION LAW (v3.3 seat 9, PR #645
     * commit 12; decision D55 ruling R4 as the planning review's Q3 amends it).
     *
     * <p>The fallback register's unit is the TYPE: a {@code DATA_TYPE} row names the type's POJO path and stands for
     * its WHOLE unit, because a derived file's writer is decided by its OWNER's verdict and never on its own. So the
     * register keeps its three sub-kinds ({@code SUB_KINDS} unchanged, the row count UNMOVED) and each derived kind
     * judges the rows' PROJECTION: every {@code DATA_TYPE} and {@code CHOICE} row mapped to THIS member's key.
     *
     * <p>A projected key falls into exactly ONE of three outcomes, decided INDEPENDENTLY of what this pass emitted
     * (the planning review's Q3(b) - "no file appeared" is never a reason):
     * <ul>
     *   <li><b>DECLARED_LEGACY_REFUSAL</b> - the OLD generator's own generation-error list of this pass names the
     *       key as its target path (the chaos cell's twelve type-format refusals). The old generator writes no file
     *       and the register must not demand one;</li>
     *   <li><b>NOT_APPLICABLE</b> - the deep-path member alone, from the host's SOURCE-HALF eligibility read
     *       ({@code DeepPathScan}, {@link #ineligibleDeepPathKeys}): a type the scan calls ineligible writes no util
     *       and the corpus holds no golden for one;</li>
     *   <li><b>EXPECTED_FILE</b> - everything else, and ONLY these are handed to the arbiter as {@code declared}.</li>
     * </ul>
     * Handing the whole projection instead would read {@code HEALED} for every refused and every ineligible key,
     * because {@code IrFallbackRegister.verdict} books {@code declared - byOldGenerator} as healed ({@code :149-153}).
     * The three counts are asserted to sum to the cell's {@code DATA_TYPE + CHOICE} row count, so the split can
     * never quietly lose a row.
     *
     * <p>This is deliberately NOT {@link #irFallbackVerdict}: that method's key is a REGISTER key and its judgement
     * feeds the register's own wholeness law, whose population is {@code cells x SUB_KINDS}. These five judge a
     * projection of the same rows, so they book their own {@code (cell, kind)} keys in their own set
     * ({@link #irDerivedKindProjectionWhollySeen}) and the register's row count does not move.
     */
    private static String irDerivedKindVerdict(CellSpec cell, String kind, UnitMember member, Object generator,
                                               Set<String> emittedKeys, List<GenerationException> genErrors,
                                               Set<String> notApplicableKeys) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return null;
        }
        String cellName = cell.toString();
        IR_DERIVED_KIND_KEYS_SEEN.add(cellName + " " + kind);
        bookUnitVerdicts(cell, member, generator);
        Set<String> emitted = new java.util.TreeSet<>();
        for (String key : emittedKeys) {
            emitted.add(key.replace('\\', '/'));
        }
        Set<String> byNewEmitter = new java.util.TreeSet<>();
        try {
            Object claimed = generator.getClass().getMethod("filesWrittenByIrEmitter").invoke(generator);
            for (Object key : (java.util.Collection<?>) claimed) {
                byNewEmitter.add(String.valueOf(key).replace('\\', '/'));
            }
        } catch (NoSuchMethodException none) {
            // a generator with no IR emitter behind it claims nothing - the whole pass is the old generator's
            byNewEmitter = new java.util.TreeSet<>();
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new AssertionError("D11 " + cell + " " + kind + ": the IR route resolved but the "
                    + generator.getClass().getSimpleName() + " does not say which files its IR emitter wrote"
                    + " - the fallback gate is not wired", e);
        }
        // THE OWNER ROWS: the cell's DATA_TYPE and CHOICE rows, each naming a type's POJO path
        Set<String> ownerRows = new java.util.TreeSet<>();
        ownerRows.addAll(IR_DECLARED_FALLBACKS.getOrDefault(
                IrFallbackRegister.key(cellName, "DATA_TYPE"), Set.of()));
        ownerRows.addAll(IR_DECLARED_FALLBACKS.getOrDefault(
                IrFallbackRegister.key(cellName, "CHOICE"), Set.of()));
        Set<String> refusalTargets = new java.util.TreeSet<>();
        for (GenerationException refusal : genErrors) {
            if (refusal.getTargetPath() != null) {
                refusalTargets.add(refusal.getTargetPath().replace('\\', '/'));
            }
        }
        Set<String> expectedFiles = new java.util.TreeSet<>();
        Set<String> declaredRefusals = new java.util.TreeSet<>();
        Set<String> notApplicable = new java.util.TreeSet<>();
        for (String row : ownerRows) {
            String projected = member.projectFrom(row);
            if (refusalTargets.contains(projected)) {
                declaredRefusals.add(projected);
            } else if (notApplicableKeys.contains(projected)) {
                notApplicable.add(projected);
            } else {
                expectedFiles.add(projected);
            }
        }
        System.out.println("D11 " + cell + " " + kind + " IR file writers: newEmitter=" + byNewEmitter.size()
                + " oldGenerator=" + (emitted.size() - byNewEmitter.size())
                + " projected=" + ownerRows.size() + " (expectedFiles=" + expectedFiles.size()
                + " declaredRefusals=" + declaredRefusals.size() + " notApplicable=" + notApplicable.size() + ")");
        List<String> red = new ArrayList<>();
        final int rows = ownerRows.size();
        final int split = expectedFiles.size() + declaredRefusals.size() + notApplicable.size();
        bookIfRed(red, () -> assertEquals(rows, split, "D11 " + cell + " " + kind + " IR file writers: the three-way"
                + " split must account for EVERY owner row - a row that falls into no outcome is a row the"
                + " projection lost (the gate asserts its own population, LAW 84)"));
        String verdict = IrFallbackRegister.verdict(cellName, kind, expectedFiles, emitted, byNewEmitter);
        if (verdict != null) {
            red.add(verdict);
        }
        return red.isEmpty() ? null : String.join(System.lineSeparator(), red);
    }

    /**
     * The DEEP-PATH keys the host's OWN SOURCE-HALF read says have NO FILE: every validated element the vendored
     * {@code DeepPathScan} calls INELIGIBLE, mapped to its deep-path util path through the translator's own law.
     * This is the projection's {@code NOT_APPLICABLE} outcome - read from the scan, NEVER from "no file appeared".
     */
    private static Set<String> ineligibleDeepPathKeys(Iterable<RModel> models, GeneratorModel gm) {
        return ineligibleDeepPathKeys(models, gm, null);
    }

    /**
     * THE SAME SOURCE-HALF READ, restricted to ONE element kind (v3.3 seat 9, PR #645 commit 14). The projection's
     * population is the cell's {@code DATA_TYPE + CHOICE} rows; the SHADOW's population is the DATA TYPES alone
     * (a choice takes the inherited path on every pass), so the shadow's {@code notEligible} column is
     * cross-checked against THIS read with {@code RDataType.class} - the member's own answer and the scan's,
     * asserted equal by name rather than counted apart.
     *
     * @param onlyKind the element class to count, or {@code null} for every validated element
     */
    private static Set<String> ineligibleDeepPathKeys(Iterable<RModel> models, GeneratorModel gm,
                                                      Class<?> onlyKind) {
        var scan = new com.regnosys.rosetta.generator.java.object.deeppath.DeepPathScan(gm);
        Set<String> keys = new java.util.TreeSet<>();
        for (RModel model : models) {
            if (!gm.shouldGenerate(model)) {
                continue;
            }
            for (var element : model.rootElements()) {
                String name;
                if (element instanceof com.regnosys.rosetta.ast.types.RDataType dataType) {
                    name = dataType.name();
                } else if (element instanceof com.regnosys.rosetta.ast.types.RChoice choice) {
                    name = choice.name();
                } else {
                    continue;
                }
                if (onlyKind != null && !onlyKind.isInstance(element)) {
                    continue;
                }
                if (!scan.isEligible(element)) {
                    keys.add(pathOf(TYPE_TRANSLATOR.toDeepPathUtilJavaClass(
                            new com.rosetta.model.lib.ModelSymbolId(gm.namespace(element), name))));
                }
            }
        }
        return keys;
    }

    /**
     * THE UNIT SHADOW LINE of ONE member (v3.3 seat 9, PR #645 commit 12) - {@link #printPojoShadow} generalised to
     * the six members, and the planning review's Q3(e) sums:
     * <pre>
     * D11 &lt;cell&gt; UNIT SHADOW[&lt;MEMBER&gt;]: types=N identical=I differing=D refused=R refusedExpected=RE renderedWhereLegacyRefused=RL notApplicable=NA notEligible=NE noGolden=NG keyContested=KC
     * </pre>
     * <ul>
     *   <li>THE ORACLE for a member is the LEGACY generator's OWN render of the cell at that key - a SECOND REAL
     *       PRODUCER, built by {@link #legacyRender} directly from the legacy class and driven beside this pass on
     *       this very run - with the GOLDEN as the second read: when the two disagree the golden is not this
     *       member's file (two generators can address one output path) and the key is counted
     *       {@code keyContested} rather than absorbed as a divergence. That is the contested-key law the POJO line
     *       has carried since commit 6, one member over.
     *       <br>ERRATUM (round 1 cq MF-1): from commit 15 to commit 17 the oracle was the KIND PASS'S OWN OUTPUT,
     *       which was true while the pass was the old generator and false the moment
     *       {@code IRTypeUnitWiring.AVAILABLE} flipped - for a written key the line then compared the unit's render
     *       with the unit's own text and {@code identical++} followed by purity, so this gate could not fail.
     *       Commit 18 hands it a second real producer again.</li>
     *   <li>{@code refusedExpected} - the IR member refused AND the old generator wrote no file at that key: the
     *       two routes AGREE that no file belongs there. Both halves are asserted, never assumed.</li>
     *   <li>{@code renderedWhereLegacyRefused} - the IR rendered a file the old generator refused. MUST be 0: it is
     *       a route disagreement, and it is named RED when the member is READY.</li>
     *   <li>{@code notApplicable} - the member answered NO FILE BY LAW; {@code notEligible} is the subset the
     *       host's own SOURCE-half eligibility read confirms (the deep-path member alone).</li>
     * </ul>
     * THE POPULATION IS ALWAYS ASSERTED ({@code types == the cell's data-type count}, LAW 84); a member the wiring
     * has NOT declared ready prints {@code refused=types} and is a PROGRESS READ beside it. SINCE COMMIT 15 all
     * six members are ready and the switch is ON, so every line is that member's GATE:
     * {@code identical + refusedExpected + notEligible == types} with {@code refused == refusedExpected},
     * {@code renderedWhereLegacyRefused}, {@code unnamedRefusals} and {@code noFileWhereLegacyWrote} at zero,
     * {@code notApplicable == notEligible}, and - since commit 18 - {@code differing == 0} BY NAME, with the first
     * five divergent keys and their first differing line. The sum law says the columns balance; the new assertion
     * says WHICH file broke (the two are independent: lanes O1 / O5).
     *
     * @param ownText the LEGACY generator's own render of this cell, from {@link #legacyRender} - never the pass's
     *     output map, which on the reference route is the unit's own text (round 1 cq MF-1)
     */
    private static String printUnitShadow(CellSpec cell, UnitMember member, Object generator,
                                          Map<String, String> ownText, int expected, Set<String> ineligibleKeys,
                                          ShadowHalf half) {
        return printUnitShadow(cell, member, generator, ownText, expected, ineligibleKeys, -1, half);
    }

    /**
     * @param sourceNotApplicable the SOURCE HALF'S OWN count of the cell's DATA TYPES that write NO FILE for this
     *     member ({@link #ineligibleDeepPathKeys} restricted to {@code RDataType}), or {@code -1} for a member
     *     that has no such read. When it is given and the member is READY the host asserts the member's
     *     {@code notEligible} EQUAL to it BY NAME - the two routes' eligibility answers held together per cell,
     *     which counting either alone would not say (v3.3 seat 9, PR #645 commit 14)
     */
    private static String printUnitShadow(CellSpec cell, UnitMember member, Object generator,
                                          Map<String, String> ownText, int expected, Set<String> ineligibleKeys,
                                          int sourceNotApplicable, ShadowHalf half) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return null;
        }
        String tag = "UNIT SHADOW[" + member.name() + "]" + half.label();
        if (half.dataTypesHalf()) {
            bookUnitPopulation(cell, expected);   // v3.3 seat 9, PR #645 commit 15 - the host's own data-type count
            // ...and the population THIS PASS's own generator walks: the cell's data types, less the ones its own
            // streamObjects filters out (the DEEP_PATH pass alone, by DeepPathScan.isEligible - the host's
            // SOURCE-half read, the same number the shadow's notEligible column is held against)
            IR_UNIT_MEMBER_POPULATION.put(cell + "|" + member.name(),
                    sourceNotApplicable >= 0 ? expected - sourceNotApplicable : expected);
        } else {
            // THE CHOICES' HALF BOOKS THE CHOICES (v3.3 seat 10, PR #646 commit 5) - see printPojoShadow for the
            // law - LESS the ones this pass's own streamObjects filters out, by the same eligibility read one
            // kind over (zero on every cell measured so far: a choice with at least one option is eligible).
            bookUnitChoicePopulation(cell, expected);
            IR_UNIT_MEMBER_CHOICE_POPULATION.put(cell + "|" + member.name(),
                    sourceNotApplicable >= 0 ? expected - sourceNotApplicable : expected);
        }
        java.util.Map<?, ?> shadow;
        java.util.Set<?> unitReady;
        try {
            shadow = (java.util.Map<?, ?>) generator.getClass().getMethod("unitShadowRenders").invoke(generator);
            unitReady = (java.util.Set<?>) generator.getClass().getMethod("unitReady").invoke(generator);
        } catch (NoSuchMethodException none) {
            return null;   // a generator with no unit behind it has no shadow line
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 " + cell + " " + tag + ": the shadow renders could"
                    + " not be read off " + generator.getClass().getSimpleName(), e);
        }
        boolean memberReady = unitReady.contains(member.name());
        // THE POPULATION FILTER (v3.3 seat 10, PR #646 commit 4). A choice and a data type share ONE pass, ONE
        // unit and ONE shadow map on this member, so THIS line reads the map at ITS OWN kind's keys - the host's
        // own source-half projection, never the pass's word for which key is whose. The SURPLUS arm beside it
        // names a key NEITHER population claims: a shadow booked for nothing.
        List<String> surplus = shadowSurplus(shadow, half);
        shadow = shadowOfHalf(shadow, half);
        Path goldensDir = resolveGoldensDir(cell);
        int identical = 0;
        int differing = 0;
        int refused = 0;
        int refusedExpected = 0;
        int renderedWhereLegacyRefused = 0;
        int notApplicable = 0;
        int notEligible = 0;
        int noFileWhereLegacyWrote = 0;
        int noGolden = 0;
        int keyContested = 0;
        int unnamedRefusals = 0;
        Map<String, Integer> refusalSites = new java.util.TreeMap<>();
        List<String> refusalReasons = new ArrayList<>();
        // ONE SAMPLE PER ARM (v3.3 seat 10, PR #646 commit 3 - PR #645 round 2 NIT-2): the three arms below each
        // keep their OWN five, so a busy arm can no longer crowd another out of the print or out of the message.
        UnitShadowDivergenceSamples samples = new UnitShadowDivergenceSamples();
        for (Map.Entry<?, ?> entry : shadow.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String[] answer = (String[]) entry.getValue();
            String own = ownText.get(key);
            switch (answer[0]) {
                case "REFUSED" -> {
                    refused++;
                    if (own == null) {
                        refusedExpected++;   // BOTH routes say no file belongs at this key
                    }
                    // the SITE, by name: `refused-site:<SITE>` is the member emitter's own naming
                    // (IRValidatorScan.NamedRefusal), anything else is a decline the route cannot name
                    if (answer[1].startsWith("refused-site:")) {
                        refusalSites.merge(answer[1].substring("refused-site:".length()), 1, Integer::sum);
                    } else {
                        unnamedRefusals++;
                        refusalSites.merge("UNNAMED", 1, Integer::sum);
                    }
                    // the CHOICES' half names EVERY refusal up to 200 - the C5 register re-cut's residual, BY
                    // NAME (v3.3 seat 10, PR #646 commit 4); the data types' half keeps its five
                    if (refusalReasons.size() < half.refusalSampleCap()) {
                        refusalReasons.add(key + " -> " + answer[1]);
                    }
                }
                case "NOFILE" -> {
                    notApplicable++;
                    if (ineligibleKeys.contains(key)) {
                        notEligible++;
                    }
                    if (own != null) {
                        // THE MIRROR OF renderedWhereLegacyRefused (v3.3 seat 9, PR #645 commit 14): the member
                        // said NO FILE BY LAW and the old generator WROTE the file at that key. MUST be 0 - it is
                        // the route disagreement that would make the unit write five files where six belong, and
                        // it is invisible to every compare column because there is nothing to compare.
                        noFileWhereLegacyWrote++;
                        samples.noFileWhereLegacyWrote(key + " -> the IR answered NO FILE BY LAW where the old"
                                + " generator WROTE the file");
                    }
                }
                case "RENDERED" -> {
                    if (own == null) {
                        renderedWhereLegacyRefused++;
                        samples.renderedWhereLegacyRefused(key + " -> the IR wrote a file the old generator did"
                                + " not");
                        break;
                    }
                    // THE ORACLE IS THE LEGACY GENERATOR'S OWN RENDER (legacyRender, PR #645 commit 18), so a key
                    // the corpus holds no golden for is STILL fully compared - noGolden is an OVERLAY on the two
                    // compare columns, exactly as keyContested is, and NOT a bucket of its own (the POJO line's
                    // noGolden IS a bucket because its oracle is the golden). At the first emitter run the two were summed into
                    // `types` AND compared, which double-counted chaos's sixteen no-golden keys (1,731 + 16 =
                    // 1,747 against a population of 1,731) - corrected FROM THAT PRINT (c12-scoped-chaos-c1.log).
                    Path goldenPath = goldensDir.resolve(key);
                    String oracle = normalize(own);
                    if (!Files.exists(goldenPath)) {
                        noGolden++;
                    } else {
                        String golden;
                        try {
                            golden = normalize(Files.readString(goldenPath));
                        } catch (IOException e) {
                            throw new AssertionError("D11 " + cell + " " + tag + ": the"
                                    + " golden " + key + " could not be read", e);
                        }
                        if (!golden.equals(oracle)) {
                            keyContested++;   // the golden at this path is not the file this member wrote
                        }
                    }
                    if (oracle.equals(normalize(answer[1]))) {
                        identical++;
                    } else {
                        differing++;
                        samples.differing(key + " -> line " + findFirstDiffLine(oracle, normalize(answer[1])));
                    }
                }
                default -> throw new AssertionError("D11 " + cell + " " + tag + ": the shadow"
                        + " answered '" + answer[0] + "' for " + key
                        + " - the channel knows RENDERED / NOFILE / REFUSED and nothing else");
            }
        }
        int types = identical + differing + refused + notApplicable + renderedWhereLegacyRefused;
        System.out.println("D11 " + cell + " " + tag + ": " + half.countName() + "=" + types
                + " identical=" + identical + " differing=" + differing + " refused=" + refused
                + " refusedExpected=" + refusedExpected
                + " renderedWhereLegacyRefused=" + renderedWhereLegacyRefused
                + " notApplicable=" + notApplicable + " notEligible=" + notEligible
                + " noFileWhereLegacyWrote=" + noFileWhereLegacyWrote
                + " noGolden=" + noGolden + " keyContested=" + keyContested
                + " refusalSites{" + renderCounts(refusalSites) + "}");
        for (String reason : refusalReasons) {
            System.out.println("  " + tag + " refusal: " + reason);
        }
        for (int i = 0; i < Math.min(5, surplus.size()); i++) {
            System.out.println("  " + tag + " SURPLUS (a shadow entry no population of either kind claims): "
                    + surplus.get(i));
        }
        // PRINT BEFORE JUDGE, AND PRINT EVERY GROUP: an arm with nothing in it prints `none` rather than
        // vanishing, so a reader can tell "no divergence of this shape" from "this arm was never looked at".
        for (String line : samples.lines("  " + tag)) {
            System.out.println(line);
        }
        System.out.println("  " + tag + " unit ready: " + unitReady + (memberReady
                ? " - this member is READY, so this line is its GATE"
                : " - this member is not ready, so this line is a progress print"));
        List<String> red = new ArrayList<>();
        final int typesRead = types;
        final List<String> surplusRead = List.copyOf(surplus);
        bookIfRed(red, () -> assertEquals(0, surplusRead.size(), "D11 " + cell + " " + tag + ": "
                + surplusSample(surplusRead) + " - a shadow booked for nothing, or a key this host and the pass"
                + " spell differently (v3.3 seat 10, PR #646 commit 4)"));
        bookIfRed(red, () -> assertEquals(expected, typesRead, "D11 " + cell + " " + tag
                + ": the member was asked for " + typesRead + " " + half.elementName() + "(s) where the cell emits "
                + expected + " - a " + half.elementName() + " left the shadow's population (the gate asserts its"
                + " own population, LAW 84)"));
        if (memberReady) {
            final int idRead = identical;
            final int reRead = refusedExpected;
            final int neRead = notEligible;
            final int refusedRead = refused;
            final int rlRead = renderedWhereLegacyRefused;
            final int unnamedRead = unnamedRefusals;
            final int differingRead = differing;
            final String divergentSample = samples.message();
            // BELT AND BRACES (v3.3 seat 9, PR #645 commit 18, round 1 cq MF-1): the sum law below says the
            // columns balance; THIS says which file broke, against a SECOND REAL PRODUCER. The two assertions are
            // independent - deleting either leaves the other red on a real divergence (lanes O1 / O5). Its sample
            // names ALL THREE ARMS separately since v3.3 seat 10, PR #646 commit 3 (round 2 NIT-2).
            bookIfRed(red, () -> assertEquals(0, differingRead, "D11 " + cell + " " + tag
                    + ": the member is declared READY and " + differingRead + " of its " + typesRead
                    + " rendered file(s) DIFFER from the old generator's own render of the same key - this line's "
                    + divergentSample + " (the two-producer gate, PR #645 commit 18)"));
            bookIfRed(red, () -> assertEquals(typesRead, idRead + reRead + neRead,
                    "D11 " + cell + " " + tag + ": the member is declared READY, so"
                            + " identical + refusedExpected + notEligible must equal " + half.countName()
                            + " - read identical="
                            + idRead + " refusedExpected=" + reRead + " notEligible=" + neRead + " over types="
                            + typesRead + " (noGolden and keyContested are OVERLAYS on the compare columns,"
                            + " never summands: the oracle is the LEGACY generator's own render)"));
            bookIfRed(red, () -> assertEquals(reRead, refusedRead, "D11 " + cell + " " + tag
                    + ": every refusal of a READY member must be one the old generator agrees with"
                    + " (refusedExpected=" + reRead + " of refused=" + refusedRead + ")"));
            bookIfRed(red, () -> assertEquals(0, rlRead, "D11 " + cell + " " + tag
                    + ": the IR wrote " + rlRead + " file(s) the old generator refused - a ROUTE DISAGREEMENT"));
            // THE REFUSAL SITES ARE ASSERTED, NOT COUNTED (v3.3 seat 9, PR #645 commit 12): a refusal of a READY
            // member must carry one of the NAMED sites the old generator refuses at, so that a nameless decline
            // can never pass for an agreed one just because the file counts happen to match.
            bookIfRed(red, () -> assertEquals(0, unnamedRead, "D11 " + cell + " " + tag
                    + ": " + unnamedRead + " refusal(s) of a READY member carry NO named site - a refusal the"
                    + " route cannot name is not a refusal the old generator agreed with"));
            // THE TWO CROSS-CHECKS OF THE NO-FILE ANSWER (v3.3 seat 9, PR #645 commit 14), each by name.
            final int nfRead = noFileWhereLegacyWrote;
            bookIfRed(red, () -> assertEquals(0, nfRead, "D11 " + cell + " " + tag
                    + ": the IR answered NO FILE BY LAW for " + nfRead + " key(s) the old generator WROTE - a"
                    + " ROUTE DISAGREEMENT, and the one the compare columns cannot see"));
            final int naRead = notApplicable;
            bookIfRed(red, () -> assertEquals(naRead, neRead, "D11 " + cell + " " + tag
                    + ": " + (naRead - neRead) + " NO-FILE answer(s) the host's OWN SOURCE-HALF eligibility read"
                    + " does NOT confirm - an absence the IR asserted alone is never booked as agreed"));
            if (sourceNotApplicable >= 0) {
                final int srcRead = sourceNotApplicable;
                bookIfRed(red, () -> assertEquals(srcRead, neRead, "D11 " + cell + " " + tag
                        + ": the member answered NO FILE for " + neRead + " " + half.elementName() + "(s) where"
                        + " the SOURCE half's own scan calls " + srcRead + " of the cell's " + half.elementName()
                        + "s ineligible - the two routes' eligibility answers must agree element for element"));
            }
        }
        return red.isEmpty() ? null : String.join(System.lineSeparator(), red);
    }

    /**
     * THE UNIT SHADOW LINE'S DIVERGENCE SAMPLE, ONE PER ARM (v3.3 seat 10, PR #646 commit 3 - PR #645 round 2
     * NIT-2). Three arms of {@link #printUnitShadow} name a key as divergent, and they are different defects:
     * <ul>
     *   <li>DIFFERING - the member rendered a file whose bytes are not the old generator's for that key;</li>
     *   <li>RENDERED-WHERE-THE-OLD-GENERATOR-REFUSED - the IR wrote a file the old generator did not;</li>
     *   <li>NO-FILE-WHERE-THE-OLD-GENERATOR-WROTE - the IR answered NO FILE BY LAW where the old generator wrote
     *       one, the disagreement no compare column can see.</li>
     * </ul>
     * They shared ONE five-slot list until this commit, so five differing keys hid the ONE key of another arm -
     * from the print and from the {@code differing == 0} message alike. Each arm now keeps its own {@link #PER_ARM}
     * and is printed and named separately.
     *
     * <p>It is a STATIC class with no host state so that a corpus-free test can drive it
     * ({@code D11IrFallbackRegisterTest}'s two sample cases). Nothing here counts: the columns and the sum law are
     * the caller's, and this holds the SAMPLE alone.
     */
    static final class UnitShadowDivergenceSamples {

        /** Each arm's own cap. The old list's cap was five for the three arms TOGETHER - that is the finding. */
        static final int PER_ARM = 5;

        private final List<String> differing = new ArrayList<>();
        private final List<String> renderedWhereLegacyRefused = new ArrayList<>();
        private final List<String> noFileWhereLegacyWrote = new ArrayList<>();

        void differing(String entry) {
            addCapped(differing, entry);
        }

        void renderedWhereLegacyRefused(String entry) {
            addCapped(renderedWhereLegacyRefused, entry);
        }

        void noFileWhereLegacyWrote(String entry) {
            addCapped(noFileWhereLegacyWrote, entry);
        }

        private static void addCapped(List<String> arm, String entry) {
            if (arm.size() < PER_ARM) {
                arm.add(entry);
            }
        }

        List<String> differing() {
            return List.copyOf(differing);
        }

        List<String> renderedWhereLegacyRefused() {
            return List.copyOf(renderedWhereLegacyRefused);
        }

        List<String> noFileWhereLegacyWrote() {
            return List.copyOf(noFileWhereLegacyWrote);
        }

        /**
         * The three groups, ALWAYS all three, the label first and its keys under it - and {@code none} for an arm
         * that took nothing, because a group that vanished when empty would read as a group nobody measured.
         *
         * @param prefix the line prefix the caller prints under, e.g. {@code "  UNIT SHADOW[META]"}
         */
        List<String> lines(String prefix) {
            List<String> out = new ArrayList<>();
            group(out, prefix, "differing", differing);
            group(out, prefix, "rendered-where-the-old-generator-refused", renderedWhereLegacyRefused);
            group(out, prefix, "no-file-where-the-old-generator-wrote", noFileWhereLegacyWrote);
            return out;
        }

        private static void group(List<String> out, String prefix, String label, List<String> arm) {
            if (arm.isEmpty()) {
                out.add(prefix + " divergence [" + label + "]: none");
                return;
            }
            out.add(prefix + " divergence [" + label + "]:");
            for (String entry : arm) {
                out.add(prefix + "   " + entry);
            }
        }

        /** The three arms BY NAME with their own counts - the {@code differing == 0} assertion's sample. */
        String message() {
            return "first " + differing.size() + " differing " + differing
                    + " / " + renderedWhereLegacyRefused.size() + " rendered-where-the-old-generator-refused "
                    + renderedWhereLegacyRefused
                    + " / " + noFileWhereLegacyWrote.size() + " no-file-where-the-old-generator-wrote "
                    + noFileWhereLegacyWrote;
        }
    }

    /**
     * BOOK one pass's UNIT VERDICT SETS (v3.3 seat 9, PR #645 commit 12, the planning review's Q1(b)): the type
     * NAMES the pass ATTEMPTED and the type NAMES it REFUSED. The six passes build six units - one per generator
     * instance, the memo per node - so their verdicts are equal only if every member's emit is a PURE function of
     * the node, which is the emitters' whole contract. That is ASSERTED here, BY NAME, never assumed.
     */
    private static void bookUnitVerdicts(CellSpec cell, UnitMember member, Object generator) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return;
        }
        try {
            Object attempted = generator.getClass().getMethod("unitAttemptedTypes").invoke(generator);
            Object refusedTypes = generator.getClass().getMethod("unitRefusedTypes").invoke(generator);
            java.util.TreeSet<String> attemptedNames =
                    new java.util.TreeSet<>((java.util.Collection<String>) attempted);
            java.util.TreeSet<String> refusedNames =
                    new java.util.TreeSet<>((java.util.Collection<String>) refusedTypes);
            IR_UNIT_VERDICTS.put(cell + "|" + member.name(),
                    List.of(java.util.Collections.unmodifiableSet(attemptedNames),
                            java.util.Collections.unmodifiableSet(refusedNames)));
            // THE WIRING'S TWO FACTS (v3.3 seat 9, PR #645 commit 14), booked per pass and printed once per cell
            Object ready = generator.getClass().getMethod("unitReady").invoke(generator);
            Object switchedOn = generator.getClass().getMethod("unitAvailable").invoke(generator);
            // THE THIRD FACT (v3.3 seat 10, PR #646 commit 4): the KIND-SCOPED switch. Six passes read ONE
            // declaration, so a cell whose passes disagreed about any of the three is named, never averaged.
            Object choiceSwitchedOn = generator.getClass().getMethod("unitChoiceAvailable").invoke(generator);
            IR_UNIT_READY.computeIfAbsent(cell.toString(), c -> new java.util.TreeSet<>())
                    .add(((java.util.Collection<?>) ready).size() + "|" + switchedOn + "|" + choiceSwitchedOn);
        } catch (NoSuchMethodException none) {
            // a generator with no unit behind it books nothing - and the line below names it as absent
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("D11 " + cell + " TYPE UNIT VERDICTS: the verdict sets could not be read off "
                    + generator.getClass().getSimpleName(), e);
        }
    }

    /**
     * THE UNIT READY LINE (v3.3 seat 9, PR #645 commit 14), one per cell:
     * <pre>
     * D11 &lt;cell&gt; UNIT READY: members 6 of 6 ; AVAILABLE=true (the wiring's switch)
     * </pre>
     * READY and AVAILABLE are TWO facts of the wiring and the line prints BOTH, because "6 of 6" alone would read
     * as a route that is on. SINCE v3.3 seat 9, PR #645 COMMIT 15 BOTH ARE TRUE: every member is ready and the
     * switch is ON, so every pass writes its member's file for every data type the unit does not refuse, every
     * writer line reads {@code newEmitter=<the unit's files>}, and the register's {@code DATA_TYPE} rows are re-cut
     * to the run's own dump in that same commit. Commit 14 and every commit before it asserted
     * {@code AVAILABLE=false} here.
     *
     * <p>The facts are booked per pass ({@link #bookUnitVerdicts}) and asserted to be ONE value per cell - six
     * generators reading one wiring cannot disagree, and if they did, that is the finding.
     */
    private static void printUnitReady(int memberCount) {
        List<String> red = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : IR_UNIT_READY.entrySet()) {
            Set<String> distinct = entry.getValue();
            System.out.println("D11 " + entry.getKey() + " UNIT READY: members "
                    + (distinct.size() == 1 ? distinct.iterator().next().split("\\|")[0] : distinct)
                    + " of " + memberCount + " ; AVAILABLE="
                    + (distinct.size() == 1 ? distinct.iterator().next().split("\\|")[1] : distinct)
                    + " (the wiring's switch) ; CHOICE_AVAILABLE="
                    + (distinct.size() == 1 ? distinct.iterator().next().split("\\|")[2] : distinct)
                    + " (the kind switch)");
            if (distinct.size() != 1) {
                red.add("D11 " + entry.getKey() + " UNIT READY: the passes read DIFFERENT wiring facts " + distinct
                        + " - ONE declaration serves all six, so this cannot happen unless someone copied it");
                continue;
            }
            String[] facts = distinct.iterator().next().split("\\|");
            if (!String.valueOf(memberCount).equals(facts[0])) {
                red.add("D11 " + entry.getKey() + " UNIT READY: the wiring declares " + facts[0] + " of "
                        + memberCount + " members ready - at PR #645 commit 15 all six are");
            }
            if (!"true".equals(facts[1])) {
                red.add("D11 " + entry.getKey() + " UNIT READY: AVAILABLE reads " + facts[1] + " - at PR #645"
                        + " commit 15 the wiring's switch is ON and the unit writes every data type whole");
            }
            // v3.3 seat 10 (PR #646 commit 5): the kind switch is ON since the flip and the line asserts it, so a
            // revert that shipped without the register's CHOICE rows being owed back - and without the meter's
            // choice legs coming off - would be RED here first.
            if (!"true".equals(facts[2])) {
                red.add("D11 " + entry.getKey() + " UNIT READY: CHOICE_AVAILABLE reads " + facts[2] + " - at"
                        + " PR #646 commit 5 the KIND switch is ON: the unit writes every choice's six files from"
                        + " the IR alone or refuses the choice whole, and the register's CHOICE rows are zero");
            }
        }
        if (!red.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), red));
        }
    }

    /**
     * THE TYPE UNIT VERDICTS LINE (v3.3 seat 9, PR #645 commit 12, the planning review's Q1(b)): across the SIX
     * passes of a cell, the set of ATTEMPTED type names and the set of REFUSED type names must be EQUAL - by NAME,
     * not by count, so a pass that silently omits a type is caught. It runs after every pass because the six live in
     * six parametrized test methods, each with its own generator instance and its own unit.
     */
    @AfterAll
    static void typeUnitVerdictsAgreeAcrossThePasses() {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return;
        }
        List<String> members = List.of(M_POJO.name(), M_TYPE_FORMAT.name(), M_CARDINALITY.name(),
                M_ONLY_EXISTS.name(), M_META.name(), M_DEEP_PATH.name());
        printUnitReady(members.size());
        Set<String> cells = new java.util.TreeSet<>();
        for (String key : IR_UNIT_VERDICTS.keySet()) {
            cells.add(key.substring(0, key.lastIndexOf('|')));
        }
        List<String> red = new ArrayList<>();
        for (String cell : cells) {
            // THE PASSES' POPULATIONS ARE NOT ALL THE SAME (v3.3 seat 9, PR #645 commit 15). Five of the six
            // generators walk every data type of the cell; the DEEP_PATH generator's own streamObjects
            // (DeepPathUtilGenerator:113-118) filters on DeepPathScan.isEligible, so its unit is asked only for
            // the ELIGIBLE types. Each member is therefore held against ITS OWN pass population - the host's
            // count, never the generator's - and the cross-pass purity statement is a set algebra: the full
            // passes agree BY NAME, and a narrower pass's verdicts agree with theirs wherever the two overlap.
            List<String> absent = new ArrayList<>();
            List<String> full = new ArrayList<>();
            List<String> narrower = new ArrayList<>();
            Set<String> cellPopulation = IR_UNIT_POPULATION.getOrDefault(cell, Set.of());
            Integer dataTypes = cellPopulation.size() == 1
                    ? Integer.valueOf(cellPopulation.iterator().next()) : null;
            // THE CELL'S CHOICES JOIN THE POPULATION (v3.3 seat 10, PR #646 commit 5): the route writes both
            // validated kinds now, so a FULL pass is one that walks every data type AND every choice, and the
            // narrower DEEP_PATH pass is narrower in both kinds by its own eligibility filter.
            Set<String> cellChoicePopulation = IR_UNIT_CHOICE_POPULATION.getOrDefault(cell, Set.of());
            Integer choices = cellChoicePopulation.size() == 1
                    ? Integer.valueOf(cellChoicePopulation.iterator().next()) : null;
            Integer wholePopulation = dataTypes == null || choices == null
                    ? null : Integer.valueOf(dataTypes + choices);
            for (String member : members) {
                if (IR_UNIT_VERDICTS.get(cell + "|" + member) == null) {
                    absent.add(member);
                    continue;
                }
                Integer own = memberPopulation(cell, member);
                if (own == null) {
                    red.add("D11 " + cell + " TYPE UNIT VERDICTS: the pass of " + member + " booked a verdict set"
                            + " but no population - the host cannot judge an attempted set it has no count for");
                    absent.add(member);
                } else if (wholePopulation != null && own.intValue() == wholePopulation.intValue()) {
                    full.add(member);
                } else {
                    narrower.add(member);
                }
            }
            int passes = members.size() - absent.size();
            // (a) EVERY pass: attempted == its own population
            for (String member : members) {
                List<Set<String>> verdicts = IR_UNIT_VERDICTS.get(cell + "|" + member);
                Integer own = memberPopulation(cell, member);
                if (verdicts == null || own == null) {
                    continue;
                }
                if (verdicts.get(0).size() != own.intValue()) {
                    red.add("D11 " + cell + " TYPE UNIT VERDICTS[" + member + "]: the unit was asked for "
                            + verdicts.get(0).size() + " declaration(s) where that pass walks " + own
                            + " (its data types and, since PR #646 commit 5, its choices) - the unit is AVAILABLE"
                            + " for both validated kinds and EVERY element of a pass's own population goes"
                            + " through it (the gate asserts its own population, LAW 84)");
                }
            }
            // (b) the FULL passes agree by name
            Set<String> attemptedCounts = new java.util.TreeSet<>();
            Set<String> fullAttempted = null;
            Set<String> fullRefused = null;
            boolean equal = true;
            for (String member : full) {
                List<Set<String>> verdicts = IR_UNIT_VERDICTS.get(cell + "|" + member);
                attemptedCounts.add(String.valueOf(verdicts.get(0).size()));
                if (fullAttempted == null) {
                    fullAttempted = verdicts.get(0);
                    fullRefused = verdicts.get(1);
                } else if (!fullAttempted.equals(verdicts.get(0)) || !fullRefused.equals(verdicts.get(1))) {
                    equal = false;
                    red.add("D11 " + cell + " TYPE UNIT VERDICTS: the full-population passes DISAGREE about which"
                            + " types the unit attempted or refused - a member's emit is not a pure function of"
                            + " the node. " + member + " attempted " + verdicts.get(0).size() + " / refused "
                            + verdicts.get(1).size() + " against " + full.get(0) + "'s "
                            + fullAttempted.size() + " / " + fullRefused.size() + "; the disagreeing names: "
                            + sampleOf(symmetricDifference(fullAttempted, verdicts.get(0)))
                            + " attempted, " + sampleOf(symmetricDifference(fullRefused, verdicts.get(1)))
                            + " refused");
                }
            }
            // (c) a NARROWER pass is a subset whose verdicts agree wherever the populations overlap
            List<String> narrowerRead = new ArrayList<>();
            for (String member : narrower) {
                List<Set<String>> verdicts = IR_UNIT_VERDICTS.get(cell + "|" + member);
                narrowerRead.add(member + "=" + verdicts.get(0).size() + " of "
                        + memberPopulation(cell, member));
                if (fullAttempted == null) {
                    continue;
                }
                Set<String> stray = new java.util.TreeSet<>(verdicts.get(0));
                stray.removeAll(fullAttempted);
                if (!stray.isEmpty()) {
                    equal = false;
                    red.add("D11 " + cell + " TYPE UNIT VERDICTS: the " + member + " pass attempted "
                            + stray.size() + " type(s) the full-population passes never saw - a narrower pass is"
                            + " a SUBSET of the cell's data types or it is walking something else: "
                            + sampleOf(stray));
                }
                Set<String> shouldRefuse = new java.util.TreeSet<>(fullRefused);
                shouldRefuse.retainAll(verdicts.get(0));
                if (!shouldRefuse.equals(verdicts.get(1))) {
                    equal = false;
                    red.add("D11 " + cell + " TYPE UNIT VERDICTS: the " + member + " pass and the full-population"
                            + " passes DISAGREE on the types they BOTH saw - the verdict is not a pure function of"
                            + " the node: " + sampleOf(symmetricDifference(shouldRefuse, verdicts.get(1))));
                }
            }
            System.out.println("D11 " + cell + " TYPE UNIT VERDICTS: passes " + passes
                    + " ; attempted=" + (attemptedCounts.size() == 1 ? attemptedCounts.iterator().next()
                                                                     : attemptedCounts)
                    + " of dataTypes=" + (dataTypes == null ? cellPopulation : dataTypes)
                    + " choices=" + (choices == null ? cellChoicePopulation : choices)
                    + " ; narrowerPasses{" + String.join(", ", narrowerRead) + "}"
                    + " ; refusedTypes equal across passes " + (equal ? "YES" : "NO")
                    + " ; refused " + (fullRefused == null ? "[]" : fullRefused));
            if (!absent.isEmpty()) {
                System.out.println("  TYPE UNIT VERDICTS: " + absent
                        + " booked no verdict set (no unit behind that pass's generator)");
            }
            if (dataTypes == null) {
                red.add("D11 " + cell + " TYPE UNIT VERDICTS: the six passes were handed " + cellPopulation
                        + " as the cell's data-type population - ONE emission filter cannot answer twice");
            }
            if (choices == null) {
                red.add("D11 " + cell + " TYPE UNIT VERDICTS: the six passes were handed " + cellChoicePopulation
                        + " as the cell's choice population - ONE emission filter cannot answer twice");
            }
            // THE ROUTING LAW'S OWN PROOF, INVERTED AT THE FLIP (v3.3 seat 10, PR #646 commit 5). Until the kind
            // switch flipped, NO pass's attempted set could carry a choice's qualified name; since the flip EVERY
            // full-population pass's set must carry EVERY one of them. By NAME, not by a count: a count would
            // still balance if a choice had quietly replaced a data type. The DEEP_PATH pass walks a narrower
            // population by its own streamObjects and is held by (c) above, as a subset, exactly as before.
            Set<String> cellChoices = IR_CELL_CHOICE_NAMES.getOrDefault(cell, Set.of());
            if (choices != null && !cellChoices.isEmpty() && cellChoices.size() != choices.intValue()) {
                red.add("D11 " + cell + " TYPE UNIT VERDICTS: this host read " + cellChoices.size()
                        + " choice NAME(s) of the cell and " + choices + " as its choice COUNT - two reads of one"
                        + " emission filter that disagree, and the assertion below is written on the names");
            }
            for (String member : full) {
                List<Set<String>> verdicts = IR_UNIT_VERDICTS.get(cell + "|" + member);
                if (verdicts == null || cellChoices.isEmpty()) {
                    continue;
                }
                Set<String> notAttempted = new java.util.TreeSet<>(cellChoices);
                notAttempted.removeAll(verdicts.get(0));
                if (!notAttempted.isEmpty()) {
                    red.add("D11 " + cell + " TYPE UNIT VERDICTS: the " + member + " pass did NOT attempt "
                            + notAttempted.size() + " of the cell's " + cellChoices.size() + " choice(s) through"
                            + " the unit, and since PR #646 commit 5 the routing law sends EVERY choice through"
                            + " it - written whole from the IR or refused whole by name: "
                            + sampleOf(notAttempted));
                }
            }
        }
        if (!red.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), red));
        }
    }

    /** The names in one set and not the other, both ways - a disagreement, named rather than counted. */
    private static Set<String> symmetricDifference(Set<String> left, Set<String> right) {
        Set<String> both = new java.util.TreeSet<>(left);
        both.addAll(right);
        Set<String> common = new java.util.TreeSet<>(left);
        common.retainAll(right);
        both.removeAll(common);
        return both;
    }

    /** At most five names of a set, with the remainder counted - a message a reader can hold in one screen. */
    private static String sampleOf(Set<String> names) {
        List<String> sample = new ArrayList<>(names).subList(0, Math.min(5, names.size()));
        return sample + (names.size() > 5 ? " (+" + (names.size() - 5) + " more)" : "");
    }

    /**
     * The PROJECTION's wholeness law, on {@link #irFallbackRegisterWhollySeen}'s pattern: after a whole
     * reference-ON-route run every {@code "<cell> <KIND>"} key of the five derived kinds must have been judged. A
     * scoped run, the OFF route and the optimised route judge nothing; an UNSCOPED reference-ON-route run that
     * judged fewer keys than cells x kinds is FAIL-CLOSED.
     */
    @AfterAll
    static void irDerivedKindProjectionWhollySeen() {
        int whole = ALL_CELLS.size() * IR_DERIVED_KINDS.size();
        if (IR_DERIVED_KIND_KEYS_SEEN.size() == whole) {
            System.out.println("D11 IR derived-kind projection wholeness: all " + whole
                    + " (cell, kind) keys judged over the five derived members");
            return;
        }
        boolean scoped = !SCOPE_CORPUS.isEmpty() || !SCOPE_VERSION.isEmpty();
        boolean referenceIrRoute = IRGeneration.providerOrNull() != null && !IRGeneration.optimisedEnabled();
        String where = IR_DERIVED_KIND_KEYS_SEEN.size() + " of " + whole + " (cell, kind) keys judged";
        if (!scoped && referenceIrRoute) {
            throw new AssertionError("D11 IR derived-kind projection wholeness: " + where + " on an UNSCOPED"
                    + " ON-route run - some cell's projection went UNASSERTED. Judged: "
                    + new java.util.TreeSet<>(IR_DERIVED_KIND_KEYS_SEEN));
        }
        System.out.println("D11 IR derived-kind projection wholeness: " + where + " - "
                + (scoped ? "a scoped run (-Dd11.corpus / -Dd11.version)"
                          : "a run that never enters the gate (OFF route, or the optimised route)")
                + "; the wholeness check judges nothing");
    }

    /**
     * THE IR FALLBACK GATE (v3.3 seat 5, PR #641 - decision D55, ruling R4). On the reference IR route, per cell and
     * sub-kind (ENUM / CHOICE / DATA_TYPE), this PRINTS the files written by the NEW IR emitter and by the OLD generator
     * and asserts the old generator's set EQUAL to the committed register {@code d11-ir-fallbacks.txt}: an undeclared file
     * is a NEW FALLBACK (a regression), a declared file the old generator no longer writes is HEALED (delete the row in
     * the healing commit). The strict file meter counts a sub-kind only when the register holds no row of it. Since v3.3
     * seat 6 (PR #642) the ENUM emitter writes every enum file - {@code newEmitter=N oldGenerator=0} on every ENUM line and
     * no ENUM row on the register; since v3.3 seat 9 (PR #645 commit 15) the type unit writes every DATA_TYPE file the same
     * way, and since v3.3 seat 10 (PR #646 commit 5) every CHOICE file - so all three sub-kinds read
     * {@code newEmitter=N oldGenerator=<the whole-unit refusals>}, and the register, which only ever shrinks, holds the
     * refusals alone (the chaos cell's twelve DATA_TYPE rows).
     */
    private static String irFallbackVerdict(CellSpec cell, String subKind, Object generator, Set<String> emittedKeys) {
        if (IRGeneration.providerOrNull() == null || IRGeneration.optimisedEnabled()) {
            return null;
        }
        String cellName = cell.toString();
        IR_FALLBACK_KEYS_SEEN.add(IrFallbackRegister.key(cellName, subKind));
        Set<String> emitted = new java.util.TreeSet<>();
        for (String key : emittedKeys) {
            emitted.add(key.replace('\\', '/'));
        }
        Set<String> byNewEmitter = new java.util.TreeSet<>();
        try {
            Object claimed = generator.getClass().getMethod("filesWrittenByIrEmitter").invoke(generator);
            for (Object key : (java.util.Collection<?>) claimed) {
                byNewEmitter.add(String.valueOf(key).replace('\\', '/'));
            }
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new AssertionError("D11 " + cell + " " + subKind + ": the IR route resolved but the " + generator.getClass().getSimpleName()
                    + " does not say which files its IR emitter wrote - the fallback gate is not wired", e);
        }
        Set<String> declared = IR_DECLARED_FALLBACKS.getOrDefault(IrFallbackRegister.key(cellName, subKind), Set.of());
        System.out.println("D11 " + cell + " " + subKind + " IR file writers: newEmitter=" + byNewEmitter.size()
                + " oldGenerator=" + (emitted.size() - byNewEmitter.size()) + " declaredFallbacks=" + declared.size());
        if (!IR_FALLBACKS_DUMP.isEmpty()) {
            StringBuilder rows = new StringBuilder();
            for (String file : emitted) {
                if (!byNewEmitter.contains(file)) {
                    rows.append(cellName).append(' ').append(subKind).append(' ').append(file).append('\n');
                }
            }
            try {
                Files.writeString(Path.of(IR_FALLBACKS_DUMP), rows.toString(), java.nio.charset.StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException("failed to append the fallback dump " + IR_FALLBACKS_DUMP, e);
            }
        }
        return IrFallbackRegister.verdict(cellName, subKind, declared, emitted, byNewEmitter);
    }

    /** Throws ONE AssertionError naming every non-null verdict of a pass - after all of the pass's lines are printed. */
    private static void failOnIrFallbackVerdicts(String... verdicts) {
        List<String> red = new ArrayList<>();
        for (String verdict : verdicts) {
            if (verdict != null) {
                red.add(verdict);
            }
        }
        if (!red.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), red));
        }
    }

    /**
     * The fallback register's wholeness law, on the {@link #irDeclineRegisterWhollySeen} pattern: after a whole
     * reference-ON-route run every declared {@code "<cell> <SUB_KIND>"} key must have been judged - a key the run never
     * reached is STALE (a cell dropped from the catalogue). A scoped run, the OFF route and the optimised route judge
     * nothing; an UNSCOPED reference-ON-route run that judged fewer keys than cells x sub-kinds is FAIL-CLOSED.
     */
    @AfterAll
    static void irFallbackRegisterWhollySeen() {
        int whole = ALL_CELLS.size() * IrFallbackRegister.SUB_KINDS.size();
        if (IR_FALLBACK_KEYS_SEEN.size() != whole) {
            boolean scoped = !SCOPE_CORPUS.isEmpty() || !SCOPE_VERSION.isEmpty();
            boolean referenceIrRoute = IRGeneration.providerOrNull() != null && !IRGeneration.optimisedEnabled();
            String where = IR_FALLBACK_KEYS_SEEN.size() + " of " + whole + " (cell, sub-kind) keys judged";
            if (!scoped && referenceIrRoute) {
                throw new AssertionError("D11 IR fallback gate wholeness: " + where + " on an UNSCOPED ON-route run - some cell's"
                        + " fallback list went UNASSERTED (a cell's goldens absent from disk, or only part of the class selected)."
                        + " Judged: " + new java.util.TreeSet<>(IR_FALLBACK_KEYS_SEEN));
            }
            System.out.println("D11 IR fallback gate wholeness: " + where + " - "
                    + (scoped ? "a scoped run (-Dd11.corpus / -Dd11.version)"
                              : "a run that never enters the gate (OFF route, or the optimised route)")
                    + "; the wholeness check judges nothing");
            return;
        }
        Set<String> stale = new java.util.TreeSet<>(IR_DECLARED_FALLBACKS.keySet());
        stale.removeAll(IR_FALLBACK_KEYS_SEEN);
        System.out.println("D11 IR fallback gate wholeness: all " + whole + " (cell, sub-kind) keys judged; the register holds ENUM="
                + IrFallbackRegister.rowsOf(IR_DECLARED_FALLBACKS, "ENUM") + " CHOICE=" + IrFallbackRegister.rowsOf(IR_DECLARED_FALLBACKS, "CHOICE")
                + " DATA_TYPE=" + IrFallbackRegister.rowsOf(IR_DECLARED_FALLBACKS, "DATA_TYPE") + " row(s) - a sub-kind counts on the file meter at ZERO");
        assertTrue(stale.isEmpty(), "D11 IR fallback gate: STALE key(s) - declared in d11-ir-fallbacks.txt but never judged by this"
                + " whole ON-route run: " + stale + " - delete their rows; the register is SHRINK-ONLY");
    }

    /**
     * The #493 driven-metric split line + its two HARD conservation gates (the #492
     * relabel-banking discovery's mandate): decompose {@code irDriven} into LOWERED (the adapter
     * lowered the claim and the leaf emitter emitted it — native IR renders; an emission whose
     * per-emission renderer reuses a legacy oracle MID-emission still counts LOWERED, because the
     * split's line is claim-root render provenance, not sub-render provenance) vs DELEGATED (the
     * counter moved on a wholesale legacy fallback/oracle render — the L-111/L-109d/L-109e/L-112/
     * L-113 relabel channels, the L-109 point-free residue belt, plus the #471 claim-root + #472
     * composition seats, both FROZEN ZEROS since the #516 serve conversion: their claims count
     * IR-LOWERED on the standalone claimRootServeLowered/postPinServeLowered receipt lines with the
     * renders unchanged). The asserts are gates, not narration: {@code delegated == Σ} of
     * the eight per-seat sub-counters (the two-channel conservation — a ninth delegated seat added
     * without joining BOTH channels fails the ring, and an {@link AssertionError} here is NOT
     * swallowed by the reader blocks' reflective catch) and {@code 0 <= delegated <= driven} (the
     * lowered complement can never read negative). Reflective like every reader in the seam blocks
     * (rune-ir-java is not on the standing test classpath); called from BOTH seam blocks so the
     * line format cannot drift between them. The walk headline stays {@code driven/denominator};
     * this line is the honest decomposition the #493 chart's second series reads.
     */
    private static void printDrivenSplit(CellSpec cell, String seam, Object compiler, int driven)
            throws ReflectiveOperationException {
        int delegated = (int) compiler.getClass().getMethod("irDrivenDelegatedCount").invoke(compiler);
        int inputFeatureNav = (int) compiler.getClass()
                .getMethod("inputFeatureNavDrivenCount").invoke(compiler);
        int pointFreeBelt = (int) compiler.getClass().getMethod("pointFreeFnDrivenCount").invoke(compiler);
        int implicitInputNav = (int) compiler.getClass()
                .getMethod("implicitInputNavDrivenCount").invoke(compiler);
        int metaNav = (int) compiler.getClass().getMethod("metaNavDrivenCount").invoke(compiler);
        int inputFeatureNavReceiver = (int) compiler.getClass()
                .getMethod("inputFeatureNavReceiverDrivenCount").invoke(compiler);
        int implicitAttrNav = (int) compiler.getClass()
                .getMethod("implicitAttrNavDrivenCount").invoke(compiler);
        int claimRootDeleg = (int) compiler.getClass().getMethod("claimRootDelegatedCount").invoke(compiler);
        int postPinDeleg = (int) compiler.getClass().getMethod("postPinDelegatedCount").invoke(compiler);
        // #496: the L-111 conversion's LOWERED receipt — printed OUTSIDE the braces dict (it is a
        // lowered fact, not a ninth delegated seat; the Σ ≡ delegated conservation below is over
        // the delegated seats alone, and the conversion moves events out of inputFeatureNav into
        // this counter as they flip delegated → IR-LOWERED).
        int inputFeatureNavLowered = (int) compiler.getClass()
                .getMethod("inputFeatureNavLoweredCount").invoke(compiler);
        // #507: the L-111 identity-serve receipt — the same outside-the-braces law (the
        // seat's LOWERABLE residue flips delegated → IR-LOWERED through the literal relabel
        // line, byte-identical BY IDENTITY; separately receipted from the emitter-composed
        // inputFeatureNavLowered so the two serve routes cannot blur).
        int inputNavIdentityLowered = (int) compiler.getClass()
                .getMethod("inputNavIdentityLoweredCount").invoke(compiler);
        // #509: the parsed-seat alias identity-serve receipt — the same outside-the-braces
        // law at the RFeatureCall seat (the guard-held plain alias navs served through the
        // literal fallback line, counted LOWERED on their own receipt; the census's
        // aliasIdGate.served rows Σ-reconcile to it per cell).
        int aliasNavIdentityLowered = (int) compiler.getClass()
                .getMethod("aliasNavIdentityLoweredCount").invoke(compiler);
        // #499: the metaNav conversion's LOWERED receipt — the same outside-the-braces law (the
        // conversion moves events out of the metaNav belt seat into this counter as they flip
        // delegated → IR-LOWERED; the belt keeps the recvBlocked residue).
        int metaNavLowered = (int) compiler.getClass()
                .getMethod("metaNavLoweredCount").invoke(compiler);
        // #500: the generic oracle-root serve receipts — the same outside-the-braces law (every
        // serve is an IR-LOWERED claim rendered by the literal super.visitX legacy line; the
        // per-family dict decomposes the A+B cluster's flips — the arm-B family seats +
        // the arm-A meta-bearing consumer seats).
        int oracleRootLowered = (int) compiler.getClass()
                .getMethod("oracleRootLoweredCount").invoke(compiler);
        String oracleRootByFamily = (String) compiler.getClass()
                .getMethod("oracleRootLoweredBreakdown").invoke(compiler);
        // #505: the converted disguised-REnumValueRef seat's driven receipt — the same
        // outside-the-braces law (a lowered fact: the seat conversion's claims drive natively
        // or through the enumChain oracle serve; the declines join the counted population).
        int enumChainRootDriven = (int) compiler.getClass()
                .getMethod("enumChainRootDrivenCount").invoke(compiler);
        int delegatedSum = inputFeatureNav + pointFreeBelt + implicitInputNav + metaNav
                + inputFeatureNavReceiver + implicitAttrNav + claimRootDeleg + postPinDeleg;
        assertEquals(delegatedSum, delegated, "D11 " + cell + " " + seam
                + ": the delegated total must ≡ Σ of the eight seat sub-counters (two-channel"
                + " conservation — a delegated seat joined one channel but not the other)");
        assertTrue(delegated >= 0 && delegated <= driven, "D11 " + cell + " " + seam
                + ": delegated must sit within [0, driven] — the lowered complement can never read"
                + " negative (delegated=" + delegated + " driven=" + driven + ")");
        System.out.println("D11 " + cell + " " + seam + " IR driven split: lowered="
                + (driven - delegated) + " delegated=" + delegated
                + " {inputFeatureNav=" + inputFeatureNav + " pointFreeBelt=" + pointFreeBelt
                + " implicitInputNav=" + implicitInputNav + " metaNav=" + metaNav
                + " inputFeatureNavReceiver=" + inputFeatureNavReceiver
                + " implicitAttrNav=" + implicitAttrNav + " claimRootDeleg=" + claimRootDeleg
                + " postPinDeleg=" + postPinDeleg + "}"
                + " inputFeatureNavLowered=" + inputFeatureNavLowered
                + " inputNavIdentityLowered=" + inputNavIdentityLowered
                + " aliasNavIdentityLowered=" + aliasNavIdentityLowered
                + " metaNavLowered=" + metaNavLowered
                + " oracleRootLowered=" + oracleRootLowered
                + " {" + oracleRootByFamily + "}"
                + " enumChainRootDriven=" + enumChainRootDriven);
    }

    /**
     * Hard generation-failure gate: any per-object failure is unexpected and fails
     * the cell. Used for the mature generators (POJO/choice/enum/function) where a
     * generation failure is a regression, not tracked debt.
     */
    private static void assertNoGenErrors(CellSpec cell, ElementKind kind,
                                          List<GenerationException> genErrors) {
        if (genErrors.isEmpty()) {
            return;
        }
        // The chaos cell (v3.2) routes through the waiver-aware gate at EVERY kind:
        // a refusal whose target path is a DECLARED row of the chaos
        // expected-divergence baseline is tolerated-but-counted (the missingOutput
        // row it produces is declared by the same baseline row), an undeclared one
        // fails exactly like everywhere else. The strict gate below stays the
        // vendored cells' contract — their refusals were healed at v3.1 close and
        // must never regress behind a waiver.
        if ("chaos".equals(cell.corpus())) {
            assertNoUnwaiveredGenErrors(cell, kind, genErrors);
            return;
        }
        throw new AssertionError("D11 " + cell + " " + kind + ": generator reported "
                + genErrors.size() + " per-object failure(s): "
                + genErrors.stream().limit(5).map(GenerationException::getMessage)
                        .collect(Collectors.joining("; "))
                + (genErrors.size() > 5 ? " ... (+" + (genErrors.size() - 5) + " more)" : ""));
    }

    /**
     * Waiver-aware generation-failure gate for the rule-family generators
     * (Rule/Report/LabelProvider), whose body emission is still converging — the
     * M7b-3 typed pipeline is pending (see
     * the development audit "codegen-completeness-audit-2026-05-27"). A
     * failure for a file already in {@link #KNOWN_DIVERGENT} is EXPECTED: the
     * element is known-incomplete, tracked as CODEGEN_BODY_GAP debt, so it is
     * tolerated — but counted out loud so the debt stays visible even when green.
     * A failure for a NON-waivered file, or one whose target path could not be
     * determined, is a new gap and fails the cell. This mirrors how every other
     * D11 comparison treats waivers (tolerate known divergence, catch new).
     *
     * <p>Limitation: membership is the only test, so this cannot distinguish a
     * rule-family file that has always failed to generate from one that today
     * emits (byte-mismatched, waivered) but later regresses to a generation
     * failure — both are CODEGEN_BODY_GAP debt for the same path, and the engine
     * phase surfaces any such case when it flips that waiver out. For VENDORED
     * cells the mature POJO/choice generators are deliberately NOT routed here
     * (see {@code coreGenErrors} + {@link #assertNoGenErrors}), so a vendored
     * DataType POJO regressing to a generation failure is never masked. The
     * CHAOS cell (v3.2) is the exception: {@link #assertNoGenErrors} routes
     * EVERY chaos kind through this gate, so the Limitation applies there —
     * accepted because the chaos declared rows are shrink-only census findings
     * with their own burn-down law, not open-ended debt.
     */
    private static void assertNoUnwaiveredGenErrors(CellSpec cell, ElementKind kind,
                                                    List<GenerationException> genErrors) {
        if (genErrors.isEmpty()) {
            return;
        }
        Set<String> waivers = waiversFor(cell, kind);
        List<GenerationException> unwaivered = genErrors.stream()
                .filter(e -> e.getTargetPath() == null || !waivers.contains(e.getTargetPath()))
                .collect(Collectors.toList());
        int tolerated = genErrors.size() - unwaivered.size();
        if (tolerated > 0) {
            // Two callers, two debts: the rule-family gate tolerates CODEGEN_BODY_GAP
            // debt; a chaos cell (routed here by assertNoGenErrors) tolerates rows of
            // the v3.2 expected-divergence baseline — say which, so the receipt reads
            // true in both.
            System.out.println("D11 " + cell + " " + kind + ": " + tolerated
                    + ("chaos".equals(cell.corpus())
                            ? " generation refusal(s) tolerated as DECLARED rows of the"
                              + " chaos expected-divergence baseline (shrink-only; the"
                              + " census prices their fix seats)"
                            : " rule-family generation failure(s) tolerated as known-divergent"
                              + " (CODEGEN_BODY_GAP debt - M7b-3 pending)"));
        }
        // -Dd11.dump-gen-errors=true — surface every gen-error with its waiver-key
        // path, useful for engine-phase burndown auditing + for constructing the
        // waiver entries needed when porting to a new rune-dsl version (e.g. PR #85
        // 9.83.0 rebaseline). Complements -Dd11.dump-paths (mismatch surfaces) and
        // -Dd11.dump-now-matching (flip-out candidates).
        if (Boolean.getBoolean("d11.dump-gen-errors") && !genErrors.isEmpty()) {
            System.out.println("D11 " + cell + " " + kind + " gen-errors (" + genErrors.size() + "):");
            for (GenerationException e : genErrors) {
                String mark = (e.getTargetPath() != null && waivers.contains(e.getTargetPath()))
                        ? "WAIVED" : "UNWAIVED";
                System.out.println("  " + mark + " " + e.getTargetPath() + "  (" + e.getMessage() + ")");
            }
        }
        if (!unwaivered.isEmpty()) {
            throw new AssertionError("D11 " + cell + " " + kind + ": generator reported "
                    + unwaivered.size() + " UNWAIVERED rule-family per-object failure(s): "
                    + unwaivered.stream().limit(5).map(GenerationException::getMessage)
                            .collect(Collectors.joining("; "))
                    + (unwaivered.size() > 5 ? " ... (+" + (unwaivered.size() - 5) + " more)" : ""));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void metafield_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        // P2.1.1 T3.3 — emission filter via GeneratorModel.shouldGenerate gates the
        // MetaFieldGenerator's internal model iteration; transitive dependency models
        // (CDM + ISO20022) loaded for resolution stay visible to type-lookup via {@code generatorModel.getType}
        // but their attributes are not walked for metafield collection. See § 9.8 of
        // the development audit "cluster-a-fix-design" — replaces an earlier
        // post-hoc {@code filterOutputToCellCorpus} strip that couldn't distinguish
        // shared-infrastructure wrappers (e.g. {@code com.rosetta.model.metafields.*})
        // triggered by own-corpus vs transitive-dep attributes.
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell));
        // The D43 IR seams (construction + the workspace-wide dispatch below).
        var metaFieldGen = IRGeneration.metaFieldGenerator(gm, TYPE_TRANSLATOR);

        Map<String, String> output = new LinkedHashMap<>();
        IRGeneration.generateMeta(metaFieldGen, output);

        var results = compareAgainstGolden(cell, output, ElementKind.METAFIELD);
        System.out.println("D11 " + cell + " METAFIELD: " + results.summary());
        assertCellKind(cell, ElementKind.METAFIELD, results);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void function_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell));
        var funcGen = IRGeneration.functionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        // FunctionGenerator#generateWithErrors returns per-function failures rather
        // than logging via System.err and continuing (Copilot R9 F1 architectural fix
        // at FunctionGenerator.java — symmetric with JavaClassGenerator#generateClasses
        // which the R6 ENUM/POJO blocks already adopted). At R10 the canonical entry
        // point was renamed from `generate` to `generateWithErrors` and the void
        // `generate(Map)` overload retained as a deprecated discard wrapper for ABI
        // compat (Copilot R10 F1 2026-05-04). Capture + throw on non-empty so
        // generator errors surface as direct test failures instead of blending into
        // the matrix drift inventory as secondary missingOutput / mismatch symptoms.
        List<GenerationException> genErrors = funcGen.generateWithErrors(output);

        // The D43 ON-gate readers — active only when the IR route actually RESOLVED (gated on
        // providerOrNull(), not the bare flag: flag-on with NO provider proceeds on Path-1 after the
        // seam's warn-once, so the readers must stay silent there too — Copilot #467 C-1; the -Pir-on
        // profile always supplies the provider jars, so the ON ring exercises them). The accessor hops
        // are reflective because rune-ir-java is not on the standing test classpath (a test-scope dep
        // would cycle the reactor — the ir-on profile supplies the m2 jars); a wrong-typed reflective
        // result folds into the same AssertionError (the CCE arm). Flag-off: inert.
        //
        // v3.1 band survey: EXCLUDE the optimised route — see the twin note at the RULE-seam block.
        // Every reader here is a REFERENCE-route counter; the optimised route's generator does not
        // expose them, so it died in the catch before compareAgainstGolden and was never measured.
        if (IRGeneration.providerOrNull() != null && !IRGeneration.optimisedEnabled()) {
            try {
                // (a) The §4.2 IR-driven-share line, EVERY cell (the PR-4 honest dial): irDriven /
                // irDeclined off the funcGen's rendering compiler, printed NEXT TO the byte-count
                // summary below — never as the parity number. The share measures attempt-success over
                // the IR-targeted families only (un-overridden families never reach the seam), and the
                // #467 post-pin guard deliberately trades share for bytes, so a low share with a green
                // byte ring is the designed state, not a regression.
                Object compiler = funcGen.getClass().getMethod("irExpressionCompiler").invoke(funcGen);
                int driven = (int) compiler.getClass().getMethod("irDrivenCount").invoke(compiler);
                int declined = (int) compiler.getClass().getMethod("irDeclinedCount").invoke(compiler);
                int guardDeclined = (int) compiler.getClass()
                        .getMethod("postPinCoercionDeclinedCount").invoke(compiler);
                int guardDelegated = (int) compiler.getClass()
                        .getMethod("postPinDelegatedCount").invoke(compiler);
                int attempted = driven + declined;
                double share = attempted == 0 ? 0.0
                        : Math.floor(driven * 10000.0 / attempted) / 100.0;
                System.out.println("D11 " + cell + " FUNCTION IR share: irDriven=" + driven
                        + " irDeclined=" + declined + " (" + share + "% of " + attempted
                        + " attempted; postPinCoercionDeclined=" + guardDeclined
                        + " postPinDelegated=" + guardDelegated + ")");
                // (a1b) The #493 driven-metric split line + its two hard conservation gates —
                // see printDrivenSplit (single-sourced for both seams).
                printDrivenSplit(cell, "FUNCTION", compiler, driven);
                // (a2) The #469 share-growth wave's per-arm meter: the guard's decline total broken down
                // by tripping arm (first-trip attribution; the tokens sum to postPinCoercionDeclined by
                // construction — both increment at the single guard seat). Each arm is one teachable
                // refinement unit; a taught arm's explicit ZERO on this line is the wave's success signal.
                String armBreakdown = (String) compiler.getClass()
                        .getMethod("postPinCoercionDeclineBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " FUNCTION postPin by arm: " + armBreakdown);
                // (a3) The #472 router's per-arm delegation meter — the frozen-zero honest-residue
                // line since the #516 serve conversion (the live attribution moved to the served
                // line below; served + declined = tripped, per arm, by construction).
                String delegBreakdown = (String) compiler.getClass()
                        .getMethod("postPinDelegationBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " FUNCTION postPin delegated by arm: " + delegBreakdown);
                // (a3a) The #516 delegated-seat serve: the router's live per-arm attribution + the
                // two flip receipts + the claim-root seat's FIRST per-arm census (Σ of each served
                // line ≡ its receipt per cell BY CONSTRUCTION — the compiler increments both at the
                // single seat).
                String serveBreakdown = (String) compiler.getClass()
                        .getMethod("postPinServeBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " FUNCTION postPin served by arm: "
                        + serveBreakdown);
                Object postPinServed = compiler.getClass()
                        .getMethod("postPinServeLoweredCount").invoke(compiler);
                System.out.println("D11 " + cell + " FUNCTION IR postPinServeLowered: "
                        + postPinServed);
                String claimRootServes = (String) compiler.getClass()
                        .getMethod("claimRootServeBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " FUNCTION claimRoot served by arm: "
                        + claimRootServes);
                Object claimRootServed = compiler.getClass()
                        .getMethod("claimRootServeLoweredCount").invoke(compiler);
                System.out.println("D11 " + cell + " FUNCTION IR claimRootServeLowered: "
                        + claimRootServed);
                // (a4) The #475 share-growth census: the declined population by claim-root family and
                // by decline site (adapterGap/postPinGuard/ruleDelegation/aliasResolution/leafEmitter —
                // both breakdowns conserve to irDeclined at the recordDecline single seat), plus the
                // UNTARGETED families the §4.2 denominator never saw (the 21 count-then-super meters;
                // behavior-inert by construction — count + the exact super call). Ranked count-
                // descending, so the lines ARE the emitter-teach worklist: the biggest family is the
                // next teach candidate, sized on the live corpus (the decode-first law applied to the
                // whole not-IR-driven population = irDeclined + untargeted).
                String declFamilies = (String) compiler.getClass()
                        .getMethod("irDeclineFamilyBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " FUNCTION IR declined by family: " + declFamilies);
                String declSites = (String) compiler.getClass()
                        .getMethod("irDeclineSiteBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " FUNCTION IR declined by site: " + declSites);
                // (a4b) The #531 W-facet belt census — the tryEmitAlias seat's per-exit
                // population (the aliasResolution decline's decode channel). Printed on its
                // OWN non-empty guard, OUTSIDE the (a5) blockerProbed gate: on the closed
                // board adapterGap-probed is ZERO so that gate suppresses whole, and a
                // DECLINE-seat census must not ride it (collection is probe-flag-gated
                // compiler-side, so the probe-off standing receipts stay byte-identical).
                String wAliasGate = (String) compiler.getClass()
                        .getMethod("wAliasGateBreakdown").invoke(compiler);
                if (!"none".equals(wAliasGate)) {
                    System.out.println("D11 " + cell + " FUNCTION IR wAliasGate census: "
                            + wAliasGate);
                }
                int untargetedTotal = (int) compiler.getClass()
                        .getMethod("untargetedVisitCount").invoke(compiler);
                String untargetedFamilies = (String) compiler.getClass()
                        .getMethod("untargetedVisitBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " FUNCTION IR untargeted visits: total=" + untargetedTotal
                        + " " + untargetedFamilies);
                // (a4c) THE v3.3 IR-SHARE GATE (seat 1, PR #637): the three breakdowns above asserted EQUAL to the
                // register's declared rows of this (cell, FUNCTION) - see assertDeclaredIrDeclines.
                assertDeclaredIrDeclines(cell, "FUNCTION", declined, declSites, declFamilies, untargetedTotal, untargetedFamilies);
                // (a5) The #476 adapterGap blocker-attribution probe (-Drosetta.generator.ir.
                // blockerProbe=true, default OFF — a census-run channel like declineWitness): the
                // family-of-root census above cannot see WHICH nested family blocks an adapterGap
                // claim, so the probe re-adapts each declined claim's subtree and records its MINIMAL
                // blockers. Two rankings per cell: participation (a claim counts once per distinct
                // blocking family) and SOLE-blocker (claims one family alone blocks — the claims its
                // adapter teach fully unblocks: the unlock ranking). Printed ONLY when the probe ran,
                // so the standing flag-on receipts are byte-identical with the probe off. NO pins —
                // a census is a reading (the #475 law).
                int blockerProbed = (int) compiler.getClass()
                        .getMethod("blockerProbedClaimCount").invoke(compiler);
                if (blockerProbed > 0) {
                    // (a5a) The #491 implicitVisit facet probe — the RImplicitVariable visit
                    // population, refined at its single seat (Σ facets ≡ the family's WHOLE visit
                    // population — driven+declined since the #491 teach — by construction): kind ×
                    // typing × binding context, sizing the one-seat teach's honest residue (the
                    // emitter type gates decline typeMissing).
                    String implicitFacets = (String) compiler.getClass()
                            .getMethod("implicitVisitFacetBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR implicitVisit facets: "
                            + implicitFacets);
                    // (a5a2) The #492 point-free renderer face meter — the renderer's own exits
                    // (uncorrelated / metaGate / nonExpression / rendered), the decode channel for
                    // the post-teach leafEmitter residue.
                    String pointFreeFaces = (String) compiler.getClass()
                            .getMethod("pointFreeRenderBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR pointFree renders: "
                            + pointFreeFaces);
                    // (a5a3) The #494 ctorVisit facet probe — the RConstructorExpr visit
                    // population, refined at its single seat (Σ facets ≡ the family's WHOLE visit
                    // population — driven+declined since the #494 teach — by construction):
                    // target × typing × value-lowerability, sizing the deep-IRConstruct admission
                    // against the shallow oracle-closed alternative (the D menu item's
                    // decode-first read).
                    String ctorFacets = (String) compiler.getClass()
                            .getMethod("ctorVisitFacetBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR ctorVisit facets: "
                            + ctorFacets);
                    // (a5a4) The #495 condVisit facet probe — the RConditionalExpr visit
                    // population, refined at its single seat (Σ facets ≡ the family's WHOLE visit
                    // population — driven+declined since the #495 teach — by construction):
                    // shape × typing × root-adapt-lowerability, with the guardTrip suffix sizing
                    // the #494-class post-pin-guard exemption question (the A menu item's
                    // decode-first read; the arm landed with the exemption pre-wired).
                    String condFacets = (String) compiler.getClass()
                            .getMethod("condVisitFacetBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR condVisit facets: "
                            + condFacets);
                    // (a5a5) The #496 lambdaVisit facet probe — the RExtractExpr + RFilterExpr
                    // visit populations, refined at their two seats (Σ facets ≡ each family's
                    // WHOLE visit population per cell — driven+declined since the #496 teach — by
                    // construction): family × binder × typing × the root-adapt verdict with the
                    // piece walk attributing declines, the guardTrip suffix reading the
                    // counterfactual guard scan (the monster wave's leg-1 read; the arm landed
                    // with the exemption pre-wired and the ctorSlot faces as regression belts).
                    String lambdaFacets = (String) compiler.getClass()
                            .getMethod("lambdaVisitFacetBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR lambdaVisit facets: "
                            + lambdaFacets);
                    // (a5a6) The #496 binder-gate cross-read — the adapterGap gate mass the same
                    // lambda machinery gates (noFilterExtractBinder / itemNotFilterExtractBound /
                    // attributeChain.noFilterExtractBinder / receiverSyntheticItem + the
                    // opOnlyElement knock-on), attributed by the exact widening lever
                    // (nearest-binder family × binder spelling; the only-element receiver family).
                    String binderCross = (String) compiler.getClass()
                            .getMethod("binderGateCrossBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR binderGate cross: "
                            + binderCross);
                    // (a5a7) The #496 L-111 root census — the inputFeatureNav seat's shape read
                    // (meta-ness × typing × the root adapter verdict with the recvKind split):
                    // since the leg-2 conversion the adapterLowers.inputHead slice CLAIMS
                    // (lowered receipt ≡ the facet per cell) and the aliasHead / guardTrip /
                    // blocked faces are the delegated residue on the byte-proven relabel.
                    String inputNavRoots = (String) compiler.getClass()
                            .getMethod("inputNavRootBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR inputNavRoot census: "
                            + inputNavRoots);
                    // (a5a8) The #497 metaNav census — the L-109d DELEGATED seat's shape read
                    // (meta kind × typing × receiver verdict; Σ ≡ the seat's relabel count per
                    // cell by construction): the conversion cluster's seat-1 decode.
                    String metaNavRoots = (String) compiler.getClass()
                            .getMethod("metaNavRootBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR metaNavRoot census: "
                            + metaNavRoots);
                    // (a5a9) The #497 implicitAttrNav census — the L-113 DELEGATED seat's shape
                    // read (the adapter's own declineReason × the nearest-binder face × the
                    // legacy ladder branch): the conversion cluster's seat-2 decode.
                    String implicitAttrRoots = (String) compiler.getClass()
                            .getMethod("implicitAttrRootBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR implicitAttrRoot census: "
                            + implicitAttrRoots);
                    // (a5a9c) The #514 leafEmitter-gap census — the emitter decline site's
                    // raw-family × lowered-root-kind decomposition (since #515 the served
                    // rows carry the .served suffix — Σ served ≡ the flip receipt below).
                    String leafGap = (String) compiler.getClass()
                            .getMethod("leafEmitterGapBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR leafEmitterGap census: "
                            + leafGap);
                    // (a5a9d) The #515 emitter-frontier serve's flip receipt — the decline
                    // site's served-claim count (LOWERED; the census's .served rows
                    // Σ-reconcile to it per cell by construction).
                    Object emitterServed = compiler.getClass()
                            .getMethod("emitterServeLoweredCount").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR emitterServeLowered: "
                            + emitterServed);
                    // (a5a9b) The #497 first-sample witness — the qualitative face per bucket.
                    String implicitAttrSamples = (String) compiler.getClass()
                            .getMethod("implicitAttrWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR implicitAttrRoot witness: "
                            + implicitAttrSamples);
                    // (a5a10) The #497 L-112 census — the input-feature-RECEIVER DELEGATED seat's
                    // shape read (typing × the adapter's own declineReason on the whole call):
                    // the conversion cluster's seat-3 decode.
                    String inputNavReceiverRoots = (String) compiler.getClass()
                            .getMethod("inputNavReceiverRootBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR inputNavReceiverRoot census: "
                            + inputNavReceiverRoots);
                    // (a5a11) The #498 A+B gate censuses — the composed cluster's decode: leg A
                    // the deferred only-element operator gate (typing × the receiver re-adapt
                    // verdict) + leg B the calleeNotFunction gate (callee symbol kind × typing ×
                    // enclosing container × arg count), node-occurrence unit at the minimal-
                    // blocker walk (the claim-unit projection reads off the sole-reason line),
                    // each with its first-sample witness (the cheapest decisive instrument,
                    // added EARLY per the #497 retro).
                    String onlyElemGate = (String) compiler.getClass()
                            .getMethod("onlyElemGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR onlyElemGate census: "
                            + onlyElemGate);
                    String onlyElemWitness = (String) compiler.getClass()
                            .getMethod("onlyElemGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR onlyElemGate witness: "
                            + onlyElemWitness);
                    String calleeGate = (String) compiler.getClass()
                            .getMethod("calleeGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR calleeGate census: "
                            + calleeGate);
                    String calleeWitness = (String) compiler.getClass()
                            .getMethod("calleeGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR calleeGate witness: "
                            + calleeWitness);
                    // (a5a12) The #499 A+B admission-cluster censuses — leg A the #498-exposed
                    // IRListOp consumer faces (seat × position × lowered-kind × result-type ×
                    // typing × consumer family) + leg B the metaFeature INTERIOR population
                    // (position × meta kind × cardinality × typing × the receiver verdict with
                    // the recursive-cascade read), node-occurrence unit at the minimal-blocker
                    // walk, each with its first-sample witness.
                    String listOpGate = (String) compiler.getClass()
                            .getMethod("listOpGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR listOpGate census: "
                            + listOpGate);
                    String listOpGateWitness = (String) compiler.getClass()
                            .getMethod("listOpGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR listOpGate witness: "
                            + listOpGateWitness);
                    String metaFeatureGate = (String) compiler.getClass()
                            .getMethod("metaFeatureGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR metaFeatureGate census: "
                            + metaFeatureGate);
                    String metaFeatureGateWitness = (String) compiler.getClass()
                            .getMethod("metaFeatureGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR metaFeatureGate witness: "
                            + metaFeatureGateWitness);
                    // (a5a13) The #500 A+B cluster censuses — arm A the #499-exposed
                    // IRMetaAccess consumer faces (seat × position × meta-child qualifier/
                    // receiver-shape × lowering-child count [the absorption-composition leg] ×
                    // typing × consumer family), node-occurrence unit at the minimal-blocker
                    // walk; arm B the four untargeted-family visit censuses (the #489 one-walk
                    // law — Σ facets ≡ each family's untargeted visit count), each with its
                    // first-sample witness.
                    String metaAccessGate = (String) compiler.getClass()
                            .getMethod("metaAccessGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR metaAccessGate census: "
                            + metaAccessGate);
                    String metaAccessGateWitness = (String) compiler.getClass()
                            .getMethod("metaAccessGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR metaAccessGate witness: "
                            + metaAccessGateWitness);
                    String thenVisit = (String) compiler.getClass()
                            .getMethod("thenVisitBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR thenVisit census: "
                            + thenVisit);
                    String thenVisitWitness = (String) compiler.getClass()
                            .getMethod("thenVisitWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR thenVisit witness: "
                            + thenVisitWitness);
                    String conversionVisit = (String) compiler.getClass()
                            .getMethod("conversionVisitBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR conversionVisit census: "
                            + conversionVisit);
                    String conversionVisitWitness = (String) compiler.getClass()
                            .getMethod("conversionVisitWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR conversionVisit witness: "
                            + conversionVisitWitness);
                    String listLitVisit = (String) compiler.getClass()
                            .getMethod("listLitVisitBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR listLitVisit census: "
                            + listLitVisit);
                    String listLitVisitWitness = (String) compiler.getClass()
                            .getMethod("listLitVisitWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR listLitVisit witness: "
                            + listLitVisitWitness);
                    String onlyExistsVisit = (String) compiler.getClass()
                            .getMethod("onlyExistsVisitBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR onlyExistsVisit census: "
                            + onlyExistsVisit);
                    String onlyExistsVisitWitness = (String) compiler.getClass()
                            .getMethod("onlyExistsVisitWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR onlyExistsVisit witness: "
                            + onlyExistsVisitWitness);
                    // (a5a14) The #501 A+C cluster censuses: arm A the four #500-exposed
                    // shallow-kind consumer faces (seat × kind × [arg-seat callee] × position ×
                    // shallow-child index × lowering-child count [the absorption-composition
                    // leg] × typing × consumer family); arm C the five REnumValueRef chain-drain
                    // faces (face × position × context × per-face detail — the symbol class +
                    // legacy-equivalent verdict for headSym, the lambda split + input-chain
                    // equivalent verdict for ruleChain, the binder-source shape for srcElem, the
                    // meta-hop position + item-chain equivalent verdict for chainMeta/bareMeta),
                    // node-occurrence unit at the minimal-blocker walk, each with its
                    // first-sample witness.
                    String shallowGate = (String) compiler.getClass()
                            .getMethod("shallowGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR shallowGate census: "
                            + shallowGate);
                    String shallowGateWitness = (String) compiler.getClass()
                            .getMethod("shallowGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR shallowGate witness: "
                            + shallowGateWitness);
                    String chainDrainGate = (String) compiler.getClass()
                            .getMethod("chainDrainGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR chainDrainGate census: "
                            + chainDrainGate);
                    String chainDrainGateWitness = (String) compiler.getClass()
                            .getMethod("chainDrainGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR chainDrainGate witness: "
                            + chainDrainGateWitness);
                    // (a5a15) The #502 A+B cluster censuses: arm A the six RSymbolReference-drain
                    // sole faces (face × per-face detail — the symbol class for symNotAttr, the
                    // bound-equivalent verdict + body-type state for aliasHead, the registering
                    // binder + source cross-verdicts for closureParam, the item kind + binder
                    // context + callee kind for argItem, the meta qualifier + arg-lowering count
                    // for calleeMetaOut, the DEEP source shape + allowlist verdict for
                    // srcElemBare — × position × context × typing); arm B the
                    // receiverSyntheticItem residue (context × the deep-source shape × the
                    // three-walk cross-verdict [allowlist × typing-walk × cached-type] × the
                    // thenBody cardinality leg × position), node-occurrence unit at the
                    // minimal-blocker walk, each with its first-sample witness.
                    String symbolDrainGate = (String) compiler.getClass()
                            .getMethod("symbolDrainGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR symbolDrainGate census: "
                            + symbolDrainGate);
                    String symbolDrainGateWitness = (String) compiler.getClass()
                            .getMethod("symbolDrainGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR symbolDrainGate witness: "
                            + symbolDrainGateWitness);
                    String synItemGate = (String) compiler.getClass()
                            .getMethod("synItemGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR synItemGate census: "
                            + synItemGate);
                    String synItemGateWitness = (String) compiler.getClass()
                            .getMethod("synItemGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR synItemGate witness: "
                            + synItemGateWitness);
                    // (a5a16) The #503 A+B cluster censuses: arm A the three REqualityExpr-cluster
                    // faces (the allAnyModifier family/mod/op/lowering decode across both binary
                    // mirrors · the operand:IRToString side/sibling decode · the enumSibling
                    // alias-body-type + sibling-resolution decode); arm B the two RFeatureCall
                    // feature-resolution faces (the nonSymbolReceiver receiver-class/adapt/
                    // cached-type/member-lookup decode · the headUnresolved head-kind/element-
                    // proof/member-lookup decode — the #502-exposed parent-gate class),
                    // node-occurrence unit at the minimal-blocker walk, each with its
                    // first-sample witness.
                    String equalityGate = (String) compiler.getClass()
                            .getMethod("equalityGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR equalityGate census: "
                            + equalityGate);
                    String equalityGateWitness = (String) compiler.getClass()
                            .getMethod("equalityGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR equalityGate witness: "
                            + equalityGateWitness);
                    String featureDrainGate = (String) compiler.getClass()
                            .getMethod("featureDrainGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR featureDrainGate census: "
                            + featureDrainGate);
                    String featureDrainGateWitness = (String) compiler.getClass()
                            .getMethod("featureDrainGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR featureDrainGate witness: "
                            + featureDrainGateWitness);
                    // (a5a17) The #504 A+B+C cluster censuses: arm A the boolean-operand
                    // cluster's three faces (the operandBareBooleanCall side/callee/output/
                    // cardinality/sibling decode [the ComparisonResult.ofNullSafe coercion
                    // family] · the operand:IRPointFreeApply decode at its three operand seats
                    // [eq/log/exist]); arm C the RSymbolReference qualified-name residue's
                    // three faces (the symbolNotAttribute legacy-recovery SEAT decode
                    // [tryBareEnumArg's positional-enum scan · the equality-sibling scan · the
                    // synthesizeImplicitItemBareNav item-attribute scan] · the argEmpty
                    // positional-param decode · the synthetic.absent head-NAME decode),
                    // node-occurrence unit at the minimal-blocker walk, each with its
                    // first-sample witness. Arm B's decode rides the standing #502 synItemGate
                    // census above — no new channel.
                    String booleanOpGate = (String) compiler.getClass()
                            .getMethod("booleanOpGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR booleanOpGate census: "
                            + booleanOpGate);
                    String booleanOpGateWitness = (String) compiler.getClass()
                            .getMethod("booleanOpGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR booleanOpGate witness: "
                            + booleanOpGateWitness);
                    String qualNameGate = (String) compiler.getClass()
                            .getMethod("qualNameGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR qualNameGate census: "
                            + qualNameGate);
                    String qualNameGateWitness = (String) compiler.getClass()
                            .getMethod("qualNameGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR qualNameGate witness: "
                            + qualNameGateWitness);
                    // (a5a18) The #505 A+B+C cluster censuses — arm A the REnumValueRef
                    // chain-bucket faces (enumSeatGate) + the visitEnumValueRef SEAT
                    // conversion sizing (enumSeatRoot — the L-109c excluded population's
                    // channel × adapter-verdict read; the conversion's claimable mass), arm
                    // B the alias-rooted argument residue (argResidueGate — the #492-named
                    // follow-up's own decode; arg:IRListOp/srcElemBare ride the standing
                    // #499/#502 censuses above), arm C the DISPATCH-class design census
                    // (dispatchGate — the #504-decoded base-signature scope-join's
                    // variant/base-input/annotation facts). Node-occurrence unit with
                    // first-sample witnesses.
                    String enumSeatGate = (String) compiler.getClass()
                            .getMethod("enumSeatGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR enumSeatGate census: "
                            + enumSeatGate);
                    String enumSeatGateWitness = (String) compiler.getClass()
                            .getMethod("enumSeatGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR enumSeatGate witness: "
                            + enumSeatGateWitness);
                    String enumSeatRoot = (String) compiler.getClass()
                            .getMethod("enumSeatRootBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR enumSeatRoot census: "
                            + enumSeatRoot);
                    String enumSeatRootWitness = (String) compiler.getClass()
                            .getMethod("enumSeatRootWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR enumSeatRoot witness: "
                            + enumSeatRootWitness);
                    String argResidueGate = (String) compiler.getClass()
                            .getMethod("argResidueGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR argResidueGate census: "
                            + argResidueGate);
                    String argResidueGateWitness = (String) compiler.getClass()
                            .getMethod("argResidueGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR argResidueGate witness: "
                            + argResidueGateWitness);
                    String dispatchGate = (String) compiler.getClass()
                            .getMethod("dispatchGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR dispatchGate census: "
                            + dispatchGate);
                    String dispatchGateWitness = (String) compiler.getClass()
                            .getMethod("dispatchGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR dispatchGate witness: "
                            + dispatchGateWitness);
                    String walkBindGate = (String) compiler.getClass()
                            .getMethod("walkBindGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR walkBindGate census: "
                            + walkBindGate);
                    String walkBindGateWitness = (String) compiler.getClass()
                            .getMethod("walkBindGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR walkBindGate witness: "
                            + walkBindGateWitness);
                    String deepGate = (String) compiler.getClass()
                            .getMethod("deepGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR deepGate census: " + deepGate);
                    String deepGateWitness = (String) compiler.getClass()
                            .getMethod("deepGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR deepGate witness: "
                            + deepGateWitness);
                    String deepVisit = (String) compiler.getClass()
                            .getMethod("deepVisitBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR deepVisit census: "
                            + deepVisit);
                    String deepVisitWitness = (String) compiler.getClass()
                            .getMethod("deepVisitWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR deepVisit witness: "
                            + deepVisitWitness);
                    String noChanGate = (String) compiler.getClass()
                            .getMethod("noChanGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR noChanGate census: "
                            + noChanGate);
                    String noChanGateWitness = (String) compiler.getClass()
                            .getMethod("noChanGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR noChanGate witness: "
                            + noChanGateWitness);
                    String l111Residue = (String) compiler.getClass()
                            .getMethod("l111ResidueBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR l111Residue census: "
                            + l111Residue);
                    String l111ResidueWitness = (String) compiler.getClass()
                            .getMethod("l111ResidueWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR l111Residue witness: "
                            + l111ResidueWitness);
                    // (a5a13) The #508 A+B cluster censuses — the meta-facet + alias-nav
                    // cluster (metaHopGate: the five composed sole faces with the arms' OWN
                    // equivalents re-adapted), the L-029 meta-sourced lambda pair (metaSrcGate:
                    // the twNull binder-source decode + the itemMetaSourced binding shapes),
                    // and the symbolNotAttribute recovery residue (symNotGate: the #504 arm
                    // ladders re-run in the REAL parent context).
                    String metaHopGate = (String) compiler.getClass()
                            .getMethod("metaHopGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR metaHopGate census: "
                            + metaHopGate);
                    String metaHopGateWitness = (String) compiler.getClass()
                            .getMethod("metaHopGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR metaHopGate witness: "
                            + metaHopGateWitness);
                    String metaSrcGate = (String) compiler.getClass()
                            .getMethod("metaSrcGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR metaSrcGate census: "
                            + metaSrcGate);
                    String metaSrcGateWitness = (String) compiler.getClass()
                            .getMethod("metaSrcGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR metaSrcGate witness: "
                            + metaSrcGateWitness);
                    String symNotGate = (String) compiler.getClass()
                            .getMethod("symNotGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR symNotGate census: "
                            + symNotGate);
                    String symNotGateWitness = (String) compiler.getClass()
                            .getMethod("symNotGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR symNotGate witness: "
                            + symNotGateWitness);
                    // (f-509) The #509 A+B cluster censuses — the parsed-seat alias-nav
                    // decline pair (aliasIdGate: featureOnBody 143 guard-held + featureOffBody
                    // 56) and the feature-resolution pair (headUnGate 112 · nsrGate 111).
                    String aliasIdGate = (String) compiler.getClass()
                            .getMethod("aliasIdGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR aliasIdGate census: "
                            + aliasIdGate);
                    String aliasIdGateWitness = (String) compiler.getClass()
                            .getMethod("aliasIdGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR aliasIdGate witness: "
                            + aliasIdGateWitness);
                    String headUnGate = (String) compiler.getClass()
                            .getMethod("headUnGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR headUnGate census: "
                            + headUnGate);
                    String headUnGateWitness = (String) compiler.getClass()
                            .getMethod("headUnGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR headUnGate witness: "
                            + headUnGateWitness);
                    String nsrGate = (String) compiler.getClass()
                            .getMethod("nsrGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR nsrGate census: "
                            + nsrGate);
                    String nsrGateWitness = (String) compiler.getClass()
                            .getMethod("nsrGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR nsrGate witness: "
                            + nsrGateWitness);
                    // #520 arm-A3: the featureUnresolved.headOther face's own decode (the
                    // calleeGate/nsrGate pattern at the third feature-resolution face).
                    String headOtherGate = (String) compiler.getClass()
                            .getMethod("headOtherGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR headOtherGate census: "
                            + headOtherGate);
                    String headOtherGateWitness = (String) compiler.getClass()
                            .getMethod("headOtherGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR headOtherGate witness: "
                            + headOtherGateWitness);
                    // #524: the B-cluster pre-arm census (the synthetic item-attr pair +
                    // the lambdaSrc compose — legacy's bare-name ladder BY CALL).
                    String itemAttrGate = (String) compiler.getClass()
                            .getMethod("itemAttrGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR itemAttrGate census: "
                            + itemAttrGate);
                    String itemAttrGateWitness = (String) compiler.getClass()
                            .getMethod("itemAttrGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR itemAttrGate witness: "
                            + itemAttrGateWitness);
                    // #526: the NotExpressible-cluster pre-arm census (the child-subtree
                    // faces — the adapter's own position-divergence predicates BY CALL).
                    String neGate = (String) compiler.getClass()
                            .getMethod("neGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR neGate census: " + neGate);
                    String neGateWitness = (String) compiler.getClass()
                            .getMethod("neGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR neGate witness: "
                            + neGateWitness);
                    // #527: the ARG_NAV-dict pre-arm census (the typeGap/typeMissing residue
                    // faces — legacy's evaluate-arg coercion-arm preconditions BY CALL + the
                    // standing post-pin scan's claim-root verdict).
                    String argGapGate = (String) compiler.getClass()
                            .getMethod("argGapGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR argGapGate census: "
                            + argGapGate);
                    String argGapGateWitness = (String) compiler.getClass()
                            .getMethod("argGapGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR argGapGate witness: "
                            + argGapGateWitness);
                    // #528: the operand/arg residue pre-arm census (the five surviving
                    // operand/call-seat faces — the adapter's own gate predicates BY CALL,
                    // seat-complete across every family that can mint each spelling).
                    String opResGate = (String) compiler.getClass()
                            .getMethod("opResGateBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR opResGate census: "
                            + opResGate);
                    String opResGateWitness = (String) compiler.getClass()
                            .getMethod("opResGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR opResGate witness: "
                            + opResGateWitness);
                    // #529: the ingest-wall pricing censuses (the headMiss.other pool
                    // re-read against the implicit-item channel + the declMiss pool's head
                    // type-class/qualifier verdict) and the A-tail censuses (the #528
                    // mint's equality-seat frontier + the argItem residue's provable
                    // binder-source channels) — all BY CALL through the adapter's own gates.
                    for (String gate : new String[] {"itemHeadGate", "headAttrGate",
                            "eqImplGate", "itemArgSrcGate"}) {
                        String gateCensus = (String) compiler.getClass()
                                .getMethod(gate + "Breakdown").invoke(compiler);
                        System.out.println("D11 " + cell + " FUNCTION IR " + gate
                                + " census: " + gateCensus);
                        String gateWitness = (String) compiler.getClass()
                                .getMethod(gate + "WitnessSamples").invoke(compiler);
                        System.out.println("D11 " + cell + " FUNCTION IR " + gate
                                + " witness: " + gateWitness);
                    }
                    // #530: the endgame-residue pre-arm censuses (the four surviving
                    // clusters — the elided-pipe deep bottoms, the nsr missing-type
                    // naming, the symbolNotAttribute survivor seats, the meta-param
                    // call-arg render preconditions) — BY CALL through the adapter's own
                    // walks/gates or model-declaration reads.
                    for (String gate : new String[] {"pipeCondGate", "nsrCtGate",
                            "kvpGate", "metaArgGate"}) {
                        String gateCensus = (String) compiler.getClass()
                                .getMethod(gate + "Breakdown").invoke(compiler);
                        System.out.println("D11 " + cell + " FUNCTION IR " + gate
                                + " census: " + gateCensus);
                        String gateWitness = (String) compiler.getClass()
                                .getMethod(gate + "WitnessSamples").invoke(compiler);
                        System.out.println("D11 " + cell + " FUNCTION IR " + gate
                                + " witness: " + gateWitness);
                    }
                    int blockerAnomalies = (int) compiler.getClass()
                            .getMethod("blockerProbeAnomalyCount").invoke(compiler);
                    String blockerClaims = (String) compiler.getClass()
                            .getMethod("blockerClaimsBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR adapterGap blockers (probed="
                            + blockerProbed + " anomalies=" + blockerAnomalies + "): " + blockerClaims);
                    String soleBlockers = (String) compiler.getClass()
                            .getMethod("soleBlockerClaimsBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR adapterGap sole-blocker claims: "
                            + soleBlockers);
                    // (a5b) The #477 per-ARM refinement decode — the same probe run, one key deeper:
                    // each minimal blocker carries the adapter's own first-failing-gate token
                    // (Family:reason), so the three lines split each family's mass by the exact arm
                    // gate that declined. Sole-reason = claims ONE gate refinement fully unblocks;
                    // the multi-reason line is the residue that closes the per-family conservation
                    // (soleBlocker == Σ soleReason + soleFamilyMultiReason). unattributed is the
                    // mirror-fidelity meter (expected 0 — triaged, never absorbed).
                    int reasonUnattributed = (int) compiler.getClass()
                            .getMethod("blockerReasonUnattributedCount").invoke(compiler);
                    String blockerReasons = (String) compiler.getClass()
                            .getMethod("blockerReasonBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR adapterGap blocker reasons (unattributed="
                            + reasonUnattributed + "): " + blockerReasons);
                    String soleReasons = (String) compiler.getClass()
                            .getMethod("soleReasonClaimsBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR adapterGap sole-reason claims: "
                            + soleReasons);
                    String multiReasonResidue = (String) compiler.getClass()
                            .getMethod("soleFamilyMultiReasonBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR adapterGap sole-family multi-reason claims: "
                            + multiReasonResidue);
                    // (a5c) The #478 nav-gate SHAPE witness — the same probe run, the two
                    // disguised-navigation gates decoded one level further: each REnumValueRef
                    // minimal blocker on inputFeatureNav/attributeChain classifies by head/root
                    // class (legacy ReferenceHandler's own resolution order) × the adapter's
                    // verdict on the EXACT legacy-synthesized equivalent nav. `lowers` sizes the
                    // slice by ADAPTER admissibility only (the equivalent's legacy render is the
                    // byte-proven one; the arm pairs the lowering with the cache-boundary retype,
                    // proven at population by the conservation signature); the position line splits
                    // chain-receiver seats from direct operand seats; the witness line pins one
                    // real corpus site per bucket. Node-occurrence unit (a shape census, not the
                    // claim-unit rankings above).
                    String navShapes = (String) compiler.getClass()
                            .getMethod("navGateShapeBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR nav-gate shapes: " + navShapes);
                    String navPositions = (String) compiler.getClass()
                            .getMethod("navGatePositionBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR nav-gate positions: " + navPositions);
                    String navSamples = (String) compiler.getClass()
                            .getMethod("navGateWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR nav-gate witness: " + navSamples);
                    // (a5d) The #479 implicit-ROOT shape witness — the same probe run, the three
                    // cluster gates the #478 witness proved bottom out at the synthetic input/item
                    // base, each classified by the LEGACY machinery that renders its root:
                    // receiverSyntheticItem by handle(RImplicitVariable)'s own binding-arm order ×
                    // the lowered item's type facts; attrOutsideFunction by the legacy synthesizer
                    // chain × the adapter's verdict on the exact synthesized equivalent;
                    // attributeChain's non-ruleInput population down legacy's own fall-through
                    // (itemChain / case-narrowed / generic) — the #478 channel's buckets stay
                    // byte-unchanged. Node-occurrence unit; the witness line pins one site per
                    // bucket.
                    String rootShapes = (String) compiler.getClass()
                            .getMethod("implicitRootShapeBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR implicit-root shapes: " + rootShapes);
                    String rootSamples = (String) compiler.getClass()
                            .getMethod("implicitRootWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR implicit-root witness: " + rootSamples);
                    // (a5e) The #480 SOURCE decode + arm-admission restatement — the same probe
                    // run: the unprovableSource residue sub-decoded by the source expression's AST
                    // shape (Σ synItem:unprovableSource.* ≡ the standing bucket — a receipt-side
                    // conservation check) + the still-declining bareAttr/chainFall/ruleInputLambda
                    // equivalents decoded at their implicit BASES (the widening map; retypeSourceOk
                    // there is a drift detector), one pinned site per bucket; and the planned
                    // bare-attr/chain arms' admissions restated per blocker (the #479 law — the
                    // pre-teach `claims` counts ARE the arms' claim pre-sizing; post-teach they
                    // read ZERO, the conservation signature).
                    String rootSources = (String) compiler.getClass()
                            .getMethod("implicitRootSourceBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR implicit-root sources: " + rootSources);
                    String rootSourceSamples = (String) compiler.getClass()
                            .getMethod("implicitRootSourceWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR implicit-root sources witness: "
                            + rootSourceSamples);
                    String rootArm = (String) compiler.getClass()
                            .getMethod("implicitRootArmBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR implicit-root arm: " + rootArm);
                    // (a5f) The #481 elided-PIPE decode — every elidedImplicit binding source on
                    // the #480 channels restated by the PLANNED widening's admission (the
                    // then-pipe face resolves to the enclosing then's ARGUMENT; Σ over a
                    // channel's pipe facets ≡ its flat elidedImplicit count — receipt-side
                    // conservation; thenArg.provable.typeOk = the claimable slice, post-teach
                    // ZERO), one pinned site per bucket.
                    String rootPipe = (String) compiler.getClass()
                            .getMethod("implicitRootPipeBreakdown").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR implicit-root pipe: " + rootPipe);
                    String rootPipeSamples = (String) compiler.getClass()
                            .getMethod("implicitRootPipeWitnessSamples").invoke(compiler);
                    System.out.println("D11 " + cell + " FUNCTION IR implicit-root pipe witness: "
                            + rootPipeSamples);
                }

                boolean cdmCell = "cdm".equals(cell.corpus());
                if (cdmCell || "drr".equals(cell.corpus())) {
                    // (b) The #467 guard's firing lock, RETENSED at #472 and AGAIN at #516 — the #470
                    // OBS-3 tripwire's lock lineage: guard-decline > 0 → delegated > 0 → served > 0.
                    // The scan still trips on every composition-boundary claim and the router renders
                    // it via the caller's own legacy fallback (the #469 claim-root law widened
                    // per-class); since the #516 serve conversion the router's counter is the flip
                    // receipt, so served>0 is the un-fire witness on every carrier cell — a silent
                    // scan un-fire would zero it and readmit the #467 drift class (anti-L-042).
                    // iso/fpml have zero FUNCTION population and stay outside.
                    int routerServed = (int) compiler.getClass()
                            .getMethod("postPinServeLoweredCount").invoke(compiler);
                    assertTrue(routerServed > 0,
                            "D43 ON-gate: the post-pin router served ZERO claims on " + cell
                                    + " FUNCTION — the guard seat's scan silently un-fired (the #467 drift"
                                    + " class would return)");
                    // (b2) The fully-taught pin, ALL THREE carrier cells since #473 (the singletonList
                    // endgame teach — the last drr FUNCTION residue now delegates at the claim-root seat,
                    // so the whole FUNCTION-seam decline meter reads explicit zeros): every FUNCTION trip
                    // delegates, and a NEW decline here means either the claim-root seat un-firing and
                    // re-declining (its self-signal — a root call is RSymbolReference, outside the router's
                    // set) or a NEW decline family; both must be triaged, never absorbed (the
                    // designed-tripwire class; cdm cells pinned at #472, drr joined at #473).
                    assertEquals(0, guardDeclined,
                            "D43 ON-gate: the post-pin guard DECLINED on " + cell + " FUNCTION — expected"
                                    + " the fully-taught zero (a seat un-fire or a NEW decline family;"
                                    + " triage, don't absorb): " + armBreakdown);
                }
                if (cdmCell && "6.20.6".equals(cell.version())) {
                    // (c) The PR-3 switch-closure reader (the lab's L-056 lock; its lab reader was
                    // deliberately not ported at #466 — this is the fork's own): the cell must have
                    // emitted its whole-output-SET switch carriers through the IR-routed renderer
                    // (choice/type #221 + basic-type map_enum #149 —
                    // FunctionExpressionRenderer.switchHoistCount). Zero means the switch population
                    // silently dropped out of the IR path on a refactor.
                    var renderer = (com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer)
                            funcGen.getClass().getMethod("irFunctionExpressionRenderer").invoke(funcGen);
                    assertTrue(renderer.switchHoistCount() > 0,
                            "D43 ON-gate: the IR-routed renderer emitted zero switch carriers on " + cell
                                    + " FUNCTION — the L-056 switch population dropped out of the IR path");
                }
            } catch (ReflectiveOperationException | ClassCastException e) {
                // The #467 Seat-1 OBS-4 hardening: name the likely causes. The provider RESOLVED (the
                // gate above), so the generator should be the IR-routed subclass exposing the readers;
                // provider-absent runs never reach this block (the C-1 gate).
                throw new AssertionError(
                        "D43 ON-gate: could not read the IR counters off the function generator ("
                                + funcGen.getClass().getName() + ") — the provider resolved, so the"
                                + " generator should be the IR-routed subclass exposing"
                                + " irExpressionCompiler()/irFunctionExpressionRenderer(); likely causes:"
                                + " the construction seam did not route through the provider, or a"
                                + " provider/harness classpath skew (stale rune-ir/rune-ir-java m2 jars"
                                + " vs this engine — reinstall both, then re-run -Pir-on)", e);
            }
        }

        var results = compareAgainstGolden(cell, output, ElementKind.FUNCTION);
        System.out.println("D11 " + cell + " FUNCTION: " + results.summary());
        // v3.1 C0: the generation-error gate runs AFTER the byte receipt above, so a cell
        // whose elements REFUSED still records its matrix numbers before failing. Refusals
        // are the intended content of this gate now — measurement first, gate second.
        // The dump rides the receipt for the same reason (ladder-retirement): without it
        // the refusing drr 7.x / cdm 6.2x cells lose their mismatch identities.
        dumpPathsIfRequested(cell, ElementKind.FUNCTION, results);
        assertNoGenErrors(cell, ElementKind.FUNCTION, genErrors);
        assertCellKind(cell, ElementKind.FUNCTION, results);
    }

    // -- PR #405 coverage burn-down wave A: the three new (cell, kind) passes --------
    // Each mirrors the established per-kind pattern (assume → cached corpus →
    // emission-filtered GeneratorModel → generate → hard gen-error gate →
    // compareAgainstGolden → assertCellKind). Populations come from the live run's
    // emitted=/expected= summaries (the population law — never precomputed).

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void onlyexists_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell));
                // v3.3 seat 9 (PR #645 commit 12) - THE ONLY_EXISTS DERIVED SEAM: a data type's six files are emitted as
        // ONE unit and five of them are written by five separate per-kind generators, so this pass is
        // constructed and driven THROUGH the seam - a flag-on run with the IR distribution present receives the
        // IR-routed variant, flag-off and the optimised route the exact legacy class, byte-identical by
        // construction (the data-rule seam's own law, one kind over).
        var gen = IRGeneration.onlyExistsValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                genErrors.addAll(IRGeneration.generateClasses(gen, model, gm.version(model), output));
            }
        }
        var results = compareAgainstGolden(cell, output, ElementKind.ONLY_EXISTS_VALIDATOR);
        System.out.println("D11 " + cell + " ONLY_EXISTS_VALIDATOR: " + results.summary());
        dumpPathsIfRequested(cell, ElementKind.ONLY_EXISTS_VALIDATOR, results);
        // v3.3 seat 9 (PR #645 commit 12): the kind's IR file-writer line under the PROJECTION law, and the
        // UNIT SHADOW line of the member this kind owns. Both are PRINTED before either is judged.
        String irWriters = irDerivedKindVerdict(cell, "ONLY_EXISTS", M_ONLY_EXISTS, gen, output.keySet(), genErrors,
                java.util.Set.of());
        // THE SHADOW'S ORACLE IS A SECOND REAL PRODUCER (PR #645 commit 18, round 1 cq MF-1): the LEGACY class,
        // constructed directly - `output` is the UNIT's own text at every written key since commit 15
        Map<String, String> oracle = legacyRender(cell, "ONLY_EXISTS",
                () -> new OnlyExistsValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL), corpus.workspace().files(), gm);
        ShadowHalf[] halves = shadowHalves(corpus.workspace().files(), gm, M_ONLY_EXISTS);
        String unitShadow = printUnitShadow(cell, M_ONLY_EXISTS, gen, oracle,
                emittedDeclarations(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RDataType.class),
                java.util.Set.of(), halves[0]);
        // v3.3 seat 10 (PR #646 commit 4): THE CHOICE HALF of this member's shadow line. The choices ride the
        // SAME pass, the SAME unit and the SAME shadow map, and the LEGACY generator above already renders them
        // (its streamObjects yields every validated element), so ONE oracle serves both halves.
        String unitShadowChoices = printUnitShadow(cell, M_ONLY_EXISTS, gen, oracle,
                emittedDeclarations(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RChoice.class),
                java.util.Set.of(), halves[1]);
        assertNoGenErrors(cell, ElementKind.ONLY_EXISTS_VALIDATOR, genErrors);
        assertCellKind(cell, ElementKind.ONLY_EXISTS_VALIDATOR, results);
        failOnIrFallbackVerdicts(irWriters, unitShadow, unitShadowChoices);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void cardinality_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell));
                // v3.3 seat 9 (PR #645 commit 12) - THE CARDINALITY DERIVED SEAM: a data type's six files are emitted as
        // ONE unit and five of them are written by five separate per-kind generators, so this pass is
        // constructed and driven THROUGH the seam - a flag-on run with the IR distribution present receives the
        // IR-routed variant, flag-off and the optimised route the exact legacy class, byte-identical by
        // construction (the data-rule seam's own law, one kind over).
        var gen = IRGeneration.cardinalityValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                genErrors.addAll(IRGeneration.generateClasses(gen, model, gm.version(model), output));
            }
        }
        var results = compareAgainstGolden(cell, output, ElementKind.CARDINALITY_VALIDATOR);
        System.out.println("D11 " + cell + " CARDINALITY_VALIDATOR: " + results.summary());
        dumpPathsIfRequested(cell, ElementKind.CARDINALITY_VALIDATOR, results);
        // v3.3 seat 9 (PR #645 commit 12): the kind's IR file-writer line under the PROJECTION law, and the
        // UNIT SHADOW line of the member this kind owns. Both are PRINTED before either is judged.
        String irWriters = irDerivedKindVerdict(cell, "CARDINALITY", M_CARDINALITY, gen, output.keySet(), genErrors,
                java.util.Set.of());
        // THE SHADOW'S ORACLE IS A SECOND REAL PRODUCER (PR #645 commit 18, round 1 cq MF-1): the LEGACY class,
        // constructed directly - `output` is the UNIT's own text at every written key since commit 15
        Map<String, String> oracle = legacyRender(cell, "CARDINALITY",
                () -> new CardinalityValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL), corpus.workspace().files(), gm);
        ShadowHalf[] halves = shadowHalves(corpus.workspace().files(), gm, M_CARDINALITY);
        String unitShadow = printUnitShadow(cell, M_CARDINALITY, gen, oracle,
                emittedDeclarations(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RDataType.class),
                java.util.Set.of(), halves[0]);
        // v3.3 seat 10 (PR #646 commit 4): THE CHOICE HALF of this member's shadow line. The choices ride the
        // SAME pass, the SAME unit and the SAME shadow map, and the LEGACY generator above already renders them
        // (its streamObjects yields every validated element), so ONE oracle serves both halves.
        String unitShadowChoices = printUnitShadow(cell, M_CARDINALITY, gen, oracle,
                emittedDeclarations(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RChoice.class),
                java.util.Set.of(), halves[1]);
        assertNoGenErrors(cell, ElementKind.CARDINALITY_VALIDATOR, genErrors);
        assertCellKind(cell, ElementKind.CARDINALITY_VALIDATOR, results);
        failOnIrFallbackVerdicts(irWriters, unitShadow, unitShadowChoices);
    }

    // -- PR #407 coverage burn-down wave B: the two new (cell, kind) passes ----------
    // Same shape as the wave-A passes below/above (assume → cached corpus →
    // emission-filtered GeneratorModel → generate → hard gen-error gate →
    // compareAgainstGolden → assertCellKind); populations from the live run's
    // emitted=/expected= summaries (the population law).

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void typeformat_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell));
                // v3.3 seat 9 (PR #645 commit 12) - THE TYPE_FORMAT DERIVED SEAM: a data type's six files are emitted as
        // ONE unit and five of them are written by five separate per-kind generators, so this pass is
        // constructed and driven THROUGH the seam - a flag-on run with the IR distribution present receives the
        // IR-routed variant, flag-off and the optimised route the exact legacy class, byte-identical by
        // construction (the data-rule seam's own law, one kind over).
        var gen = IRGeneration.typeFormatValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                genErrors.addAll(IRGeneration.generateClasses(gen, model, gm.version(model), output));
            }
        }
        var results = compareAgainstGolden(cell, output, ElementKind.TYPE_FORMAT_VALIDATOR);
        System.out.println("D11 " + cell + " TYPE_FORMAT_VALIDATOR: " + results.summary());
        dumpPathsIfRequested(cell, ElementKind.TYPE_FORMAT_VALIDATOR, results);
        // v3.3 seat 9 (PR #645 commit 12): the kind's IR file-writer line under the PROJECTION law, and the
        // UNIT SHADOW line of the member this kind owns. Both are PRINTED before either is judged.
        String irWriters = irDerivedKindVerdict(cell, "TYPE_FORMAT", M_TYPE_FORMAT, gen, output.keySet(), genErrors,
                java.util.Set.of());
        // THE SHADOW'S ORACLE IS A SECOND REAL PRODUCER (PR #645 commit 18, round 1 cq MF-1): the LEGACY class,
        // constructed directly - `output` is the UNIT's own text at every written key since commit 15. This kind's
        // legacy generator REFUSES the chaos BOILERPLATE_NAME_COLLISION types, and those refusals are exactly the
        // `own == null` keys the shadow's refusedExpected column reads.
        Map<String, String> oracle = legacyRender(cell, "TYPE_FORMAT",
                () -> new TypeFormatValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL), corpus.workspace().files(), gm);
        ShadowHalf[] halves = shadowHalves(corpus.workspace().files(), gm, M_TYPE_FORMAT);
        String unitShadow = printUnitShadow(cell, M_TYPE_FORMAT, gen, oracle,
                emittedDeclarations(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RDataType.class),
                java.util.Set.of(), halves[0]);
        // v3.3 seat 10 (PR #646 commit 4): THE CHOICE HALF of this member's shadow line. The choices ride the
        // SAME pass, the SAME unit and the SAME shadow map, and the LEGACY generator above already renders them
        // (its streamObjects yields every validated element), so ONE oracle serves both halves.
        String unitShadowChoices = printUnitShadow(cell, M_TYPE_FORMAT, gen, oracle,
                emittedDeclarations(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RChoice.class),
                java.util.Set.of(), halves[1]);
        assertNoGenErrors(cell, ElementKind.TYPE_FORMAT_VALIDATOR, genErrors);
        assertCellKind(cell, ElementKind.TYPE_FORMAT_VALIDATOR, results);
        failOnIrFallbackVerdicts(irWriters, unitShadow, unitShadowChoices);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void xmeta_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell));
                // v3.3 seat 9 (PR #645 commit 12) - THE XMETA DERIVED SEAM: a data type's six files are emitted as
        // ONE unit and five of them are written by five separate per-kind generators, so this pass is
        // constructed and driven THROUGH the seam - a flag-on run with the IR distribution present receives the
        // IR-routed variant, flag-off and the optimised route the exact legacy class, byte-identical by
        // construction (the data-rule seam's own law, one kind over).
        var gen = IRGeneration.modelMetaGenerator(gm, TYPE_TRANSLATOR);

        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                genErrors.addAll(IRGeneration.generateClasses(gen, model, gm.version(model), output));
            }
        }
        var results = compareAgainstGolden(cell, output, ElementKind.XMETA);
        System.out.println("D11 " + cell + " XMETA: " + results.summary());
        dumpPathsIfRequested(cell, ElementKind.XMETA, results);
        // v3.3 seat 9 (PR #645 commit 12): the kind's IR file-writer line under the PROJECTION law, and the
        // UNIT SHADOW line of the member this kind owns. Both are PRINTED before either is judged.
        String irWriters = irDerivedKindVerdict(cell, "XMETA", M_META, gen, output.keySet(), genErrors,
                java.util.Set.of());
        // THE SHADOW'S ORACLE IS A SECOND REAL PRODUCER (PR #645 commit 18, round 1 cq MF-1): the LEGACY class,
        // constructed directly - `output` is the UNIT's own text at every written key since commit 15
        Map<String, String> oracle = legacyRender(cell, "XMETA",
                () -> new ModelMetaGenerator(gm, TYPE_TRANSLATOR), corpus.workspace().files(), gm);
        ShadowHalf[] halves = shadowHalves(corpus.workspace().files(), gm, M_META);
        String unitShadow = printUnitShadow(cell, M_META, gen, oracle,
                emittedDeclarations(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RDataType.class),
                java.util.Set.of(), halves[0]);
        // v3.3 seat 10 (PR #646 commit 4): THE CHOICE HALF of this member's shadow line. The choices ride the
        // SAME pass, the SAME unit and the SAME shadow map, and the LEGACY generator above already renders them
        // (its streamObjects yields every validated element), so ONE oracle serves both halves.
        String unitShadowChoices = printUnitShadow(cell, M_META, gen, oracle,
                emittedDeclarations(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RChoice.class),
                java.util.Set.of(), halves[1]);
        assertNoGenErrors(cell, ElementKind.XMETA, genErrors);
        assertCellKind(cell, ElementKind.XMETA, results);
        failOnIrFallbackVerdicts(irWriters, unitShadow, unitShadowChoices);
    }

    // -- PR #408 coverage burn-down wave C: the deep-path util (cell, kind) pass ------
    // Same shape as the wave-A/B passes (assume → cached corpus → emission-filtered
    // GeneratorModel → generate → hard gen-error gate → compareAgainstGolden →
    // assertCellKind); populations from the live run's emitted=/expected= summaries
    // (the population law).

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void deeppath_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell));
                // v3.3 seat 9 (PR #645 commit 12) - THE DEEP_PATH DERIVED SEAM: a data type's six files are emitted as
        // ONE unit and five of them are written by five separate per-kind generators, so this pass is
        // constructed and driven THROUGH the seam - a flag-on run with the IR distribution present receives the
        // IR-routed variant, flag-off and the optimised route the exact legacy class, byte-identical by
        // construction (the data-rule seam's own law, one kind over).
        var gen = IRGeneration.deepPathUtilGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                genErrors.addAll(IRGeneration.generateClasses(gen, model, gm.version(model), output));
            }
        }
        var results = compareAgainstGolden(cell, output, ElementKind.DEEP_PATH_UTIL);
        System.out.println("D11 " + cell + " DEEP_PATH_UTIL: " + results.summary());
        dumpPathsIfRequested(cell, ElementKind.DEEP_PATH_UTIL, results);
        // v3.3 seat 9 (PR #645 commit 12): the kind's IR file-writer line under the PROJECTION law, and the
        // UNIT SHADOW line of the member this kind owns. Both are PRINTED before either is judged.
        String irWriters = irDerivedKindVerdict(cell, "DEEP_PATH", M_DEEP_PATH, gen, output.keySet(), genErrors,
                ineligibleDeepPathKeys(corpus.workspace().files(), gm));
        // THE SHADOW'S ORACLE IS A SECOND REAL PRODUCER (PR #645 commit 18, round 1 cq MF-1): the LEGACY class,
        // constructed directly - `output` is the UNIT's own text at every written key since commit 15. This kind's
        // legacy generator streams the ELIGIBLE types alone, so its map is silent at every ineligible key, which
        // is exactly what the noFileWhereLegacyWrote cross-check reads.
        Map<String, String> oracle = legacyRender(cell, "DEEP_PATH",
                () -> new DeepPathUtilGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL), corpus.workspace().files(), gm);
        ShadowHalf[] halves = shadowHalves(corpus.workspace().files(), gm, M_DEEP_PATH);
        String unitShadow = printUnitShadow(cell, M_DEEP_PATH, gen, oracle,
                emittedDeclarations(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RDataType.class),
                ineligibleDeepPathKeys(corpus.workspace().files(), gm),
                // the SOURCE half's own count, over the DATA TYPES alone - the shadow's population
                ineligibleDeepPathKeys(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RDataType.class).size(),
                halves[0]);
        // v3.3 seat 10 (PR #646 commit 4): THE CHOICE HALF. The eligibility cross-check is the same law over the
        // CHOICES alone - DeepPathScan calls a choice eligible iff it has at least one option
        // (DeepPathScan:121-124), so on a vendored cell the count is expected to be ZERO and the member writes a
        // util for EVERY choice; it is READ here, never assumed.
        String unitShadowChoices = printUnitShadow(cell, M_DEEP_PATH, gen, oracle,
                emittedDeclarations(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RChoice.class),
                ineligibleDeepPathKeys(corpus.workspace().files(), gm),
                ineligibleDeepPathKeys(corpus.workspace().files(), gm,
                        com.regnosys.rosetta.ast.types.RChoice.class).size(),
                halves[1]);
        assertNoGenErrors(cell, ElementKind.DEEP_PATH_UTIL, genErrors);
        assertCellKind(cell, ElementKind.DEEP_PATH_UTIL, results);
        failOnIrFallbackVerdicts(irWriters, unitShadow, unitShadowChoices);
    }

    // -- PR #409 coverage burn-down wave D: the data-rule (cell, kind) pass -----------
    // The LAST family. Same shape as the wave-A/B/C passes (assume → cached corpus →
    // emission-filtered GeneratorModel → generate → hard gen-error gate →
    // compareAgainstGolden → assertCellKind); populations from the live run's
    // emitted=/expected= summaries (the population law).

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void datarule_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell));
        // v3.3 seat 3 (D54) - THE DATA_RULE SEAM: the condition generator is constructed through the IR seam,
        // so the reference ON route renders every condition body through IRExpressionCompiler (the
        // IRDataRuleGenerator subclass); flag-off, and the optimised route (which declines the kind), construct
        // the exact legacy class - byte-identical by construction, the same law as the five older seams.
        var gen = IRGeneration.dataRuleGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                genErrors.addAll(gen.generateClasses(model, gm.version(model), output));
            }
        }
        // The DATA_RULE seam's ON-gate readers (v3.3 seat 3, D54) - the RULE block's shape, one kind over: active
        // only when the REFERENCE IR route actually resolved (providerOrNull(), never the bare flag; the optimised
        // route excluded exactly as at the other two seams), reflective hops because rune-ir-java is not on the
        // standing test classpath (the ir-on profile supplies the m2 jars), flag-off inert (the OFF ring prints
        // zero share lines). The counters are this pass's generator's alone (a fresh instance per (cell, kind)).
        if (IRGeneration.providerOrNull() != null && !IRGeneration.optimisedEnabled()) {
            try {
                Object compiler = gen.getClass().getMethod("irExpressionCompiler").invoke(gen);
                int driven = (int) compiler.getClass().getMethod("irDrivenCount").invoke(compiler);
                int declined = (int) compiler.getClass().getMethod("irDeclinedCount").invoke(compiler);
                int guardDeclined = (int) compiler.getClass()
                        .getMethod("postPinCoercionDeclinedCount").invoke(compiler);
                int guardDelegated = (int) compiler.getClass()
                        .getMethod("postPinDelegatedCount").invoke(compiler);
                int attempted = driven + declined;
                double share = attempted == 0 ? 0.0
                        : Math.floor(driven * 10000.0 / attempted) / 100.0;
                System.out.println("D11 " + cell + " DATA_RULE IR share: irDriven=" + driven
                        + " irDeclined=" + declined + " (" + share + "% of " + attempted
                        + " attempted; postPinCoercionDeclined=" + guardDeclined
                        + " postPinDelegated=" + guardDelegated + ")");
                printDrivenSplit(cell, "DATA_RULE", compiler, driven);
                System.out.println("D11 " + cell + " DATA_RULE postPin by arm: "
                        + compiler.getClass().getMethod("postPinCoercionDeclineBreakdown").invoke(compiler));
                System.out.println("D11 " + cell + " DATA_RULE postPin delegated by arm: "
                        + compiler.getClass().getMethod("postPinDelegationBreakdown").invoke(compiler));
                System.out.println("D11 " + cell + " DATA_RULE postPin served by arm: "
                        + compiler.getClass().getMethod("postPinServeBreakdown").invoke(compiler));
                System.out.println("D11 " + cell + " DATA_RULE IR postPinServeLowered: "
                        + compiler.getClass().getMethod("postPinServeLoweredCount").invoke(compiler));
                System.out.println("D11 " + cell + " DATA_RULE claimRoot served by arm: "
                        + compiler.getClass().getMethod("claimRootServeBreakdown").invoke(compiler));
                System.out.println("D11 " + cell + " DATA_RULE IR claimRootServeLowered: "
                        + compiler.getClass().getMethod("claimRootServeLoweredCount").invoke(compiler));
                String declFamilies = (String) compiler.getClass()
                        .getMethod("irDeclineFamilyBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " DATA_RULE IR declined by family: " + declFamilies);
                String declSites = (String) compiler.getClass()
                        .getMethod("irDeclineSiteBreakdown").invoke(compiler);
                System.out.println("D11 " + cell + " DATA_RULE IR declined by site: " + declSites);
                String wAliasGate = (String) compiler.getClass()
                        .getMethod("wAliasGateBreakdown").invoke(compiler);
                if (!"none".equals(wAliasGate)) {
                    System.out.println("D11 " + cell + " DATA_RULE IR wAliasGate census: " + wAliasGate);
                }
                int untargetedTotal = (int) compiler.getClass()
                        .getMethod("untargetedVisitCount").invoke(compiler);
                String untargetedFamilies = (String) compiler.getClass()
                        .getMethod("untargetedVisitBreakdown").invoke(compiler);
                // the cardinality TWINS' roots land here (visitCardinalityCheck is an untargeted-visit seat), so
                // this seam is the one whose vendored cells carry `untargeted` rows in the register (D54 item 1(f))
                System.out.println("D11 " + cell + " DATA_RULE IR untargeted visits: total=" + untargetedTotal
                        + " " + untargetedFamilies);
                // The generation-error count PRINTED BEFORE the gate: a red gate throws before assertNoGenErrors,
                // and a claim that threw records no decline, so a register cut from a print with errors would
                // declare a hole as the goal state - the register's generator refuses a print whose line here is
                // not zero (D54 item 1(g)); the strict assertNoGenErrors below still fails the pass on a GREEN gate.
                System.out.println("D11 " + cell + " DATA_RULE generation errors before the gate: " + genErrors.size());
                // (d3c) The #476 / #477 blocker-attribution PROBE, DATA_RULE edition (v3.3 seat 4 - the measurement
                // before the heal): the RULE block's (r3c) / (r3d) ranked lines, the #506 walkBindGate decode, the
                // #478 nav-gate and #479 / #480 implicit-root witnesses and the #491 implicitVisit facets, printed
                // ONLY when the probe ran (-Drosetta.generator.ir.blockerProbe=true; probe-off nothing prints and the
                // standing receipts stay byte-identical). Printed BEFORE the gate below, unlike the RULE block's: a
                // measurement seat reads the probe at a head whose declaration may lag the print, and a red gate throws.
                int blockerProbed = (int) compiler.getClass().getMethod("blockerProbedClaimCount").invoke(compiler);
                if (blockerProbed > 0) {
                    printDataRuleProbe(cell, compiler, blockerProbed);
                }
                // THE v3.3 IR-SHARE GATE at the third seam: the three breakdowns asserted EQUAL to the register's
                // declared rows of this (cell, DATA_RULE) - see assertDeclaredIrDeclines; a red gate throws BEFORE
                // compareAgainstGolden (the D43 shape).
                assertDeclaredIrDeclines(cell, "DATA_RULE", declined, declSites, declFamilies, untargetedTotal, untargetedFamilies);
            } catch (ReflectiveOperationException | ClassCastException e) {
                throw new AssertionError("D11 " + cell + " DATA_RULE: the IR route resolved but the seam's counters"
                        + " could not be read off " + gen.getClass().getName() + " - " + e, e);
            }
        }
        var results = compareAgainstGolden(cell, output, ElementKind.DATA_RULE);
        System.out.println("D11 " + cell + " DATA_RULE: " + results.summary());
        dumpPathsIfRequested(cell, ElementKind.DATA_RULE, results);
        assertNoGenErrors(cell, ElementKind.DATA_RULE, genErrors);
        assertCellKind(cell, ElementKind.DATA_RULE, results);
    }

    /**
     * (d3c) The DATA_RULE seam's probe channel (v3.3 seat 4): the same reflective reads as the RULE block's (r3c)-(r3g),
     * each line prefixed {@code D11 <cell> DATA_RULE IR ...} so a print reader keys it by seam. Reached only when the
     * #476 probe ran on this pass's compiler (the caller's {@code blockerProbed > 0} guard).
     */
    private static void printDataRuleProbe(CellSpec cell, Object compiler, int blockerProbed)
            throws ReflectiveOperationException {
        String[][] lines = {
            // {print label, breakdown method} - the (r3c) / (r3d) ranked lines: which family blocks, which family
            // blocks ALONE (the unlock ranking), which Family:reason arm-gate pair, and the residues
            {"adapterGap sole-blocker claims", "soleBlockerClaimsBreakdown"},
            {"adapterGap sole-reason claims", "soleReasonClaimsBreakdown"},
            {"adapterGap sole-family multi-reason claims", "soleFamilyMultiReasonBreakdown"},
            // the #506 walkBindGate decode (the face x position x detail token per declined bind) + its first samples
            {"walkBindGate census", "walkBindGateBreakdown"},
            {"walkBindGate witness", "walkBindGateWitnessSamples"},
            // the #478 nav-gate SHAPE witness: head / root class x the adapter's verdict, the chain-receiver position
            // split, one pinned corpus site per bucket
            {"nav-gate shapes", "navGateShapeBreakdown"},
            {"nav-gate positions", "navGatePositionBreakdown"},
            {"nav-gate witness", "navGateWitnessSamples"},
            // the #479 implicit-ROOT shape witness and the #480 SOURCE decode
            {"implicit-root shapes", "implicitRootShapeBreakdown"},
            {"implicit-root witness", "implicitRootWitnessSamples"},
            {"implicit-root sources", "implicitRootSourceBreakdown"},
            {"implicit-root source witness", "implicitRootSourceWitnessSamples"},
            // the #491 implicitVisit facets (the RImplicitVariable visit population by kind x typing x binding context)
            {"implicitVisit facets", "implicitVisitFacetBreakdown"},
        };
        int blockerAnomalies = (int) compiler.getClass().getMethod("blockerProbeAnomalyCount").invoke(compiler);
        String blockerClaims = (String) compiler.getClass().getMethod("blockerClaimsBreakdown").invoke(compiler);
        System.out.println("D11 " + cell + " DATA_RULE IR adapterGap blockers (probed=" + blockerProbed
                + " anomalies=" + blockerAnomalies + "): " + blockerClaims);
        int reasonUnattributed = (int) compiler.getClass().getMethod("blockerReasonUnattributedCount").invoke(compiler);
        String blockerReasons = (String) compiler.getClass().getMethod("blockerReasonBreakdown").invoke(compiler);
        System.out.println("D11 " + cell + " DATA_RULE IR adapterGap blocker reasons (unattributed="
                + reasonUnattributed + "): " + blockerReasons);
        for (String[] line : lines) {
            Object value = compiler.getClass().getMethod(line[1]).invoke(compiler);
            System.out.println("D11 " + cell + " DATA_RULE IR " + line[0] + ": " + value);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeCells")
    void packageinfo_comparison(CellSpec cell) throws IOException {
        Assumptions.assumeTrue(cellGoldensExist(cell),
                "Goldens absent for " + cell + " — cell skipped");
        var corpus = loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter(cell));
        // One file per namespace (after-generate hook semantics), not per element —
        // no per-object error channel; a generator failure propagates and aborts
        // this (cell, kind) instance, mirroring metafield_comparison's shape.
        var gen = new com.regnosys.rosetta.generator.java.object.JavaPackageInfoGenerator(gm);

        Map<String, String> output = new LinkedHashMap<>();
        gen.generatePackageInfoClasses(output);

        var results = compareAgainstGolden(cell, output, ElementKind.PACKAGE_INFO);
        System.out.println("D11 " + cell + " PACKAGE_INFO: " + results.summary());
        assertCellKind(cell, ElementKind.PACKAGE_INFO, results);
    }

    /**
     * The {@code -Dd11.dump-paths=true} diagnostic dump — full unwaived path lists
     * (mismatches with first-diff lines, noGolden, missingOutput) for
     * active-investigation use; the assertion message only carries sample context.
     * See docs/bc-verification.md Layer 4 § Recovery.
     *
     * <p><b>Called from TWO positions, printed once per (cell, kind):</b> the kind
     * tests whose generation-error gate precedes {@link #assertCellKind} call this
     * right after the summary receipt (v3.1 ladder-retirement — the FUNCTION groups
     * on cdm 6.2x/drr 7.x refuse at {@link #assertNoGenErrors} and their 143-file
     * mismatch identities were otherwise lost to the dump), and
     * {@link #assertCellKind} calls it for every path that reaches the byte assert.
     * The printed-once set makes the two calls compose without double output.
     */
    private static final Set<String> DUMPED_PATH_KEYS = ConcurrentHashMap.newKeySet();

    private void dumpPathsIfRequested(CellSpec cell, ElementKind kind, ComparisonResults results) {
        // The waivered-dir materialisation rides the same receipt position for the same
        // reason as the path dump (v3.1 flip seat 2): it previously lived only in
        // assertCellKind, so a kind whose generation-error gate fails first (the drr 7.x
        // FUNCTION refusals) never materialised its mismatch content — the C0
        // measurement-first law applied to the content dump. Its own flag + written-once
        // guard keep the two call positions composing exactly like the path dump's.
        dumpWaiveredDirIfRequested(cell, kind, results);
        if (!Boolean.getBoolean("d11.dump-paths")) {
            return;
        }
        if (!DUMPED_PATH_KEYS.add(cell + "|" + kind)) {
            return;
        }
        Set<String> waivers = waiversFor(cell, kind);
        var unwaivedMismatches = results.mismatches.stream()
                .filter(m -> !waivers.contains(m.file()))
                .toList();
        var unwaivedNoGolden = results.noGolden.stream()
                .filter(f -> !waivers.contains(f))
                .toList();
        var unwaivedMissingOutput = results.missingOutput.stream()
                .filter(f -> !waivers.contains(f))
                .toList();
        if (!unwaivedMismatches.isEmpty()) {
            System.out.println("D11 " + cell + " " + kind + " unwaived mismatches (" + unwaivedMismatches.size() + "):");
            unwaivedMismatches.forEach(m -> System.out.println("  " + m.file() + " (line " + m.firstDiffLine() + ")"));
        }
        if (!unwaivedNoGolden.isEmpty()) {
            System.out.println("D11 " + cell + " " + kind + " unwaived noGolden (" + unwaivedNoGolden.size() + "):");
            unwaivedNoGolden.forEach(p -> System.out.println("  " + p));
        }
        if (!unwaivedMissingOutput.isEmpty()) {
            System.out.println("D11 " + cell + " " + kind + " unwaived missingOutput (" + unwaivedMissingOutput.size() + "):");
            unwaivedMissingOutput.forEach(p -> System.out.println("  " + p));
        }
    }

    /**
     * The {@code -Dd11.dump-waivered-dir=<ABSOLUTE path>} materialisation — BOTH sides
     * of every divergent file: the GENERATED content to
     * {@code <dir>/<cell>_<kind>/gen/<flat~path>} AND the GOLDEN content to
     * {@code <dir>/<cell>_<kind>/golden/<flat~path>} (the regscan's PRE/POST input —
     * both sides are dumped so it can recompute the gen-vs-golden line ratio at each
     * end). Flat path mirrors the historical f-probe layout ({@code '/'}→{@code '~'}).
     * Requires {@code CAPTURE_CONTENT} (set by this flag), so both
     * {@code m.generated()} and {@code m.golden()} are populated. Opt-in; default no-op.
     *
     * <p>Rides {@link #dumpPathsIfRequested}'s receipt position (both call sites), so a
     * kind whose generation-error gate fails before {@link #assertCellKind} still
     * materialises its mismatch content; the written-once set composes the two calls.
     */
    private static final Set<String> WAIVERED_DUMP_KEYS = ConcurrentHashMap.newKeySet();

    private void dumpWaiveredDirIfRequested(CellSpec cell, ElementKind kind, ComparisonResults results) {
        if (DUMP_WAIVERED_DIR.isEmpty() || results.mismatches.isEmpty()) {
            return;
        }
        if (!WAIVERED_DUMP_KEYS.add(cell + "|" + kind)) {
            return;
        }
        try {
            Path cellDir = Path.of(DUMP_WAIVERED_DIR,
                    cell.toString().replace('/', '_') + "_" + kind);
            Path genDir = cellDir.resolve("gen");
            Path goldenDir = cellDir.resolve("golden");
            Files.createDirectories(genDir);
            Files.createDirectories(goldenDir);
            for (Mismatch m : results.mismatches) {
                if (m.generated() == null || m.golden() == null) continue;
                String flat = m.file().replace('/', '~');
                Files.writeString(genDir.resolve(flat), m.generated());
                Files.writeString(goldenDir.resolve(flat), m.golden());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("dump-waivered-dir write failed", e);
        }
    }

    /**
     * v3.2 seat 9 (F13) — the {@code noGolden} side of the {@code -Dd11.dump-waivered-dir} flag: a
     * golden-free emission has no golden to diff against, so {@link #dumpWaiveredDirIfRequested}
     * never carries it. Under the same flag this writes the FORK's content of every noGolden row to
     * {@code <dir>/<cell>_<kind>/nogolden/<flat~path>} (the flat path as the waivered dump's), at the
     * moment the row is classified. Opt-in; default no-op. Written once per file (the set composes
     * the two receipt positions the same way the waivered dump's does).
     */
    private static final Set<String> NOGOLDEN_DUMP_KEYS = ConcurrentHashMap.newKeySet();

    private void dumpNoGoldenIfRequested(CellSpec cell, ElementKind kind, String file, String generated) {
        if (DUMP_WAIVERED_DIR.isEmpty() || generated == null) {
            return;
        }
        if (!NOGOLDEN_DUMP_KEYS.add(cell + "|" + kind + "|" + file)) {
            return;
        }
        try {
            Path dir = Path.of(DUMP_WAIVERED_DIR,
                    cell.toString().replace('/', '_') + "_" + kind).resolve("nogolden");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(file.replace('/', '~')), generated);
        } catch (IOException e) {
            throw new UncheckedIOException("dump-waivered-dir (nogolden side) write failed", e);
        }
    }

    /**
     * Strict equality. Drift signatures (per spec § 3.2):
     * <ul>
     *   <li>{@code mismatches > 0} — generator emits wrong bytes for some files</li>
     *   <li>{@code noGolden > 0} — generator emits files no upstream golden covers
     *       (heuristic-classification escape OR generator over-emits)</li>
     *   <li>{@code missingOutput > 0} — upstream golden exists for a kind we should emit
     *       but our generator did not produce it (under-generation)</li>
     * </ul>
     *
     * <p>Documented per-(cell, kind) divergences are waivered via {@link #KNOWN_DIVERGENT}
     * (Freezing pattern). Investigation evidence at
     * {@code docs/reviews/p1.7-pr1.6-cluster-investigation.md}.
     */
    private void assertCellKind(CellSpec cell, ElementKind kind, ComparisonResults results) {
        Set<String> waivers = waiversFor(cell, kind);
        var unwaivedMismatches = results.mismatches.stream()
                .filter(m -> !waivers.contains(m.file()))
                .toList();
        // v3.2 seat 9 (PR #630, F13 / D48): on the chaos cell the noGolden class is tolerated by the
        // golden-free REGISTER alone, asserted in both directions (assertChaosGoldenFreeUnion), never
        // by the expected-divergence baseline, which carries no noGolden row since D48.
        Set<String> registerHere = "chaos".equals(cell.corpus())
                ? assertChaosGoldenFreeUnion(cell, kind, results)
                : Set.of();
        var unwaivedNoGolden = results.noGolden.stream()
                .filter(f -> !waivers.contains(f) && !registerHere.contains(f))
                .toList();
        var unwaivedMissingOutput = results.missingOutput.stream()
                .filter(f -> !waivers.contains(f))
                .toList();
        // Diagnostic dump (opt-in via -Dd11.dump-paths=true) — see dumpPathsIfRequested.
        // Kept here for every path that reaches the byte assert; the per-kind tests that
        // gate on generation errors BEFORE this method call the helper themselves (the
        // C0 measurement-first law: a refusing cell must still surface its mismatch
        // identities), and the printed-once guard makes the two calls compose.
        dumpPathsIfRequested(cell, kind, results);
        // Engine phase — opt-in flip-out candidate dump (-Dd11.dump-now-matching=true;
        // default off). Lists waivered files that NOW byte-match the golden: each is a
        // candidate for waiver removal (flip-out) once a rendering facet lands. The
        // waiver-aware gate tolerates a waivered file regardless of whether it matches,
        // so a now-matching file leaves a harmless stale waiver until flipped out; this
        // diagnostic surfaces exactly which entries to remove, per cell+kind, for an
        // auditable single-pass flip-out. Complements -Dd11.dump-paths (unwaived drift)
        // — this prints the inverse (waived-but-resolved). Engine PR #4 onward.
        if (Boolean.getBoolean("d11.dump-now-matching")) {
            var nowMatching = results.matches.stream().filter(waivers::contains).sorted().toList();
            if (!nowMatching.isEmpty()) {
                System.out.println("D11 " + cell + " " + kind
                        + " waivered-but-now-matching (" + nowMatching.size() + "):");
                nowMatching.forEach(f -> System.out.println("  FLIP-OUT " + cell + "/" + kind + ":" + f));
            }
        }
        // P2.1.1 T3.1 — opt-in mismatch content dump for active investigation.
        // -Dd11.dump-content=N prints full golden + generated for the first N
        // unwaived mismatches (e.g. -Dd11.dump-content=1 for laser-focused single
        // mismatch; -Dd11.dump-content=3 for sampled cluster-pattern diagnosis).
        // Complements -Dd11.dump-paths (which only prints file names + first-diff
        // line numbers). Same gating semantics: opt-in only, default no-op.
        if (DUMP_CONTENT_MAX > 0 && !unwaivedMismatches.isEmpty()) {
            int dumped = 0;
            for (Mismatch m : unwaivedMismatches) {
                if (dumped >= DUMP_CONTENT_MAX) break;
                dumped++;
                System.out.println("=== D11 " + cell + " " + kind + " MISMATCH " + dumped
                        + "/" + Math.min(DUMP_CONTENT_MAX, unwaivedMismatches.size())
                        + ": " + m.file() + " (first-diff line " + m.firstDiffLine() + ") ===");
                System.out.println("--- GOLDEN (" + m.golden().split("\n", -1).length + " lines) ---");
                System.out.println(m.golden());
                System.out.println("--- GENERATED (" + m.generated().split("\n", -1).length + " lines) ---");
                System.out.println(m.generated());
                System.out.println("=== END MISMATCH " + dumped + " ===");
            }
        }
        assertTrue(unwaivedMismatches.isEmpty() && unwaivedNoGolden.isEmpty() && unwaivedMissingOutput.isEmpty(),
                "D11 " + cell + " " + kind + " strict equality failed. "
                        + "mismatches=" + unwaivedMismatches.size()
                        + " noGolden=" + unwaivedNoGolden.size()
                        + " missingOutput=" + unwaivedMissingOutput.size()
                        + (unwaivedMismatches.isEmpty()
                                ? ""
                                : " — first mismatch: " + unwaivedMismatches.get(0).file()
                                  + " (line " + unwaivedMismatches.get(0).firstDiffLine() + ")")
                        + (unwaivedMissingOutput.isEmpty()
                                ? ""
                                : " — first missing: " + unwaivedMissingOutput.get(0)));
    }

    // =========================================================================
    // Corpus loading
    // =========================================================================

    /**
     * Resolves builtin {@code .rosetta} files via union across {@link #BUILTINS_SEARCH_ROOTS}
     * — collects all {@code .rosetta} files from every existing candidate root, dedup'd by
     * filename in priority order (test-corpus root wins for filename collisions; sibling
     * rune-dsl root fills any gaps). Per Copilot R8 F1 2026-05-04 — refines R7's
     * "first-existing-directory" pick which would commit to a partial/stale test-corpus
     * clone and never try the sibling rune-dsl fallback even when it has the missing files.
     * Mirrors the per-file fallback semantic of {@code BuiltinParseTest.SEARCH_ROOTS} in
     * rune-parser (which iterates roots per file via {@code for (String root : SEARCH_ROOTS)
     * { if (Files.exists(candidate)) ... break; }}).
     *
     * @return ordered list of resolved builtin {@code .rosetta} file paths (sorted by filename
     *         for determinism); empty list if neither candidate root exists or both are empty.
     */
    private static List<Path> resolveBuiltinFiles() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        return resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();
    }

    /**
     * Cache of parsed + linked workspaces per cell. Avoids the 4x parsing cost when
     * the 4 element-kind tests (enum / pojo / metafield / function) all run against
     * the same cell — each kind test would otherwise call {@link #loadCellCorpus}
     * independently, parsing every {@code .rosetta} file 4 times. Per Copilot R8 F2
     * 2026-05-04. {@link ConcurrentHashMap} for safety; {@code putIfAbsent}
     * race-resolution returns the first stored value if two threads compute
     * concurrently. Empty until populated by the first kind test for each cell.
     */
    private static final Map<CellSpec, RLinkingResult> CELL_CORPUS_CACHE = new ConcurrentHashMap<>();

    // Package-private (PR #155): FunctionTransitiveIso20022ClosureTest locks the
    // transitive-dependency closure semantics against THIS loader (not a mirrored
    // copy), so reverting the closure change here turns that facet test RED.
    RLinkingResult loadCellCorpusCached(CellSpec cell) throws IOException {
        var cached = CELL_CORPUS_CACHE.get(cell);
        if (cached != null) return cached;
        var result = loadCellCorpus(cell);
        var existing = CELL_CORPUS_CACHE.putIfAbsent(cell, result);
        return existing != null ? existing : result;
    }

    private RLinkingResult loadCellCorpus(CellSpec cell) throws IOException {
        List<RModel> models = new ArrayList<>();

        // Resolve builtins via union across BUILTINS_SEARCH_ROOTS (Copilot R7 F1 2026-05-04
        // initial fix; R8 F1 refinement — per-file/union resolution mirroring BuiltinParseTest's
        // per-file SEARCH_ROOTS semantic, NOT R7's weaker first-existing-directory pick).
        // Cells reach loadCellCorpus only after cellGoldensExist() guards pass; if cells are
        // present AND builtins are missing from BOTH candidate locations, fail loud with the
        // dependency-resolution context instead of letting the cell .rosetta parse step
        // surface the absence as misleading symbol-not-found noise (cells reference builtin
        // types — string, int, etc. — that won't resolve without builtins loaded first).
        List<Path> builtinFiles = resolveBuiltinFiles();
        if (builtinFiles.isEmpty()) {
            throw new AssertionError(
                    "D11 " + cell + ": rune-dsl builtins not found. Searched (in order): "
                    + BUILTINS_SEARCH_ROOTS + ". Clone one of:\n"
                    + "  1. test-corpus/rune-dsl-builtins/ (preferred — full corpus clone)\n"
                    + "  2. sibling rune-dsl/ checkout at ../rune-dsl/ (full rune-dsl source)\n"
                    + "Cell .rosetta files reference builtin types (string, int, etc.) and will "
                    + "fail to parse with misleading symbol-not-found errors without builtins.");
        }

        for (Path p : builtinFiles) {
            try {
                models.add(AstBuilder.buildFromFile(p));
            } catch (Exception e) {
                // Fail loud on builtin parse failure (Copilot R3 suppressed-finding +
                // R4 follow-up 2026-05-03). Logging-only let baseline-mode capture
                // continue against an incomplete workspace, recording downstream
                // mismatches that masked the actual parse-failure root cause. Throwing
                // aborts THIS (cell, kind) parametrized instance only — other 51
                // instances still run; the matrix-wide drift inventory is preserved
                // except for the cells affected by the broken builtin.
                throw new AssertionError("D11 BUILTIN parse error: " + p.getFileName()
                        + " — " + e.getMessage(), e);
            }
        }

        // P2.1.1 T3 — Transitive-CDM closure for DRR cells (Cluster A fix per
        // the development audit "cluster-a-fix-design" § 6). DRR upstream Maven builds
        // declare a <finos.cdm.version> compile-time dependency that provides CDM
        // types for cross-corpus resolution (e.g. `corpus Scheme`, `extends
        // TimeUnitEnum`); our test loader mirrors that semantics by surfacing the
        // matching CDM .rosetta closure into the workspace BEFORE walking the cell's
        // own .rosetta files. The {@link #emissionFilter} predicate passed to
        // {@link GeneratorModel} — consulted by {@link GeneratorModel#shouldGenerate}
        // — ensures only the cell's own corpus emits Java (CDM types stay
        // resolution-only, matching upstream Maven's "CDM jar provided externally"
        // semantics). Gated on cell.corpus()=="drr"; other corpora (CDM/ISO/rune-fpml)
        // take the unchanged code path.
        if ("drr".equals(cell.corpus())) {
            String cdmVersion = DRR_TO_CDM_VERSION.get(cell.version());
            if (cdmVersion != null) {
                Path cdmRosettaDir = TEST_CORPUS_ROOT
                        .resolve("cdm/cdm-" + cdmVersion)
                        .resolve("rosetta-source/src/main/rosetta");
                // Fail loud on missing transitive-CDM dependency dir (Copilot R4 F11).
                // Silent skip would re-introduce Cluster A-style mismatches downstream
                // with a far less actionable failure mode (drift surfaces as ~15 ENUM
                // golden mismatches on the affected DRR cell, with no immediate hint
                // that the root cause is a missing test-corpus directory). Throwing
                // here aborts THIS (cell, kind) parametrized instance only — other
                // 51 instances still run, and the operator sees the expected path
                // immediately in the failure message.
                if (!Files.isDirectory(cdmRosettaDir)) {
                    throw new AssertionError("D11 " + cell
                            + " transitive-CDM dependency directory missing: " + cdmRosettaDir
                            + " — DRR cell " + cell.version() + " maps to CDM " + cdmVersion
                            + " per DRR_TO_CDM_VERSION; expected the matching CDM "
                            + ".rosetta closure at this path (see "
                            + "the development audit 'cluster-a-fix-design' § 6).");
                }
                try (var stream = Files.walk(cdmRosettaDir)) {
                    stream.filter(p -> p.toString().endsWith(".rosetta"))
                          .sorted()
                          .forEach(p -> {
                              try {
                                  // Dependency models: NOT versionStamped — they retain
                                  // CDM's own version metadata. The emissionFilter
                                  // predicate (consulted by GeneratorModel.shouldGenerate)
                                  // excludes them from Java emission for DRR cells.
                                  models.add(AstBuilder.buildFromFile(p));
                              } catch (Exception e) {
                                  throw new AssertionError("D11 " + cell
                                          + " transitive-CDM parse error: "
                                          + p.getFileName() + " — " + e.getMessage(), e);
                              }
                          });
                }
            }
            // PR #155 — Transitive-ISO20022 closure for DRR cells (same mechanism as
            // the transitive-CDM walk above; see DRR_TO_ISO20022_VERSION javadoc).
            // Upstream drr rosetta-source/pom.xml unpacks org.iso20022:rosetta-source
            // *.rosetta into target/parent-dependency/iso20022/rosetta and compiles
            // them alongside the cell's own sources; mirroring that here lets the
            // drr projection functions resolve iso20022.auth030.* output types
            // (type aliases like MICIdentifier/ISODate and the auth030 Document
            // model types). Resolution-only: emissionFilter restricts drr-cell
            // emission to models whose namespace starts with "drr.".
            String isoVersion = DRR_TO_ISO20022_VERSION.get(cell.version());
            if (isoVersion != null) {
                Path isoRosettaDir = TEST_CORPUS_ROOT
                        .resolve("iso20022/iso20022-" + isoVersion)
                        .resolve("rosetta-source/src/main/rosetta");
                // Fail loud on a missing dependency dir, matching the transitive-CDM
                // branch above — a silent skip would resurface as hundreds of
                // unexplained Object-erased outputs downstream.
                if (!Files.isDirectory(isoRosettaDir)) {
                    throw new AssertionError("D11 " + cell
                            + " transitive-ISO20022 dependency directory missing: " + isoRosettaDir
                            + " — DRR cell " + cell.version() + " maps to iso20022 " + isoVersion
                            + " per DRR_TO_ISO20022_VERSION; expected the matching iso20022 "
                            + ".rosetta closure at this path.");
                }
                try (var stream = Files.walk(isoRosettaDir)) {
                    stream.filter(p -> p.toString().endsWith(".rosetta"))
                          .sorted()
                          .forEach(p -> {
                              try {
                                  // Dependency models: NOT versionStamped — they retain
                                  // iso20022's own version metadata; excluded from Java
                                  // emission for DRR cells by emissionFilter.
                                  models.add(AstBuilder.buildFromFile(p));
                              } catch (Exception e) {
                                  throw new AssertionError("D11 " + cell
                                          + " transitive-ISO20022 parse error: "
                                          + p.getFileName() + " — " + e.getMessage(), e);
                              }
                          });
                }
            }
        }

        // PR #184 — Transitive-fpml closure for CDM cells (same mechanism as the
        // DRR transitive-CDM/ISO20022 walks above; see CDM_TO_FPML_VERSION javadoc).
        // Upstream cdm-6.20.6 rosetta-source/pom.xml unpacks com.regnosys.rune-fpml
        // rosetta into target/parent-dependency/fpml/rosetta and compiles it; the
        // cdm.ingest.fpml.* functions take fpml.consolidated.* input types. Mirroring
        // that here lets those functions resolve their fpml signatures (their input
        // parameter types erase to Object otherwise). Resolution-only: emissionFilter
        // excludes the fpml.* models from this cell's Java emission. Gated on
        // cell.corpus()=="cdm"; only cdm 6.20.6 has a CDM_TO_FPML_VERSION entry.
        // 2026-08-14 — THE CLOSURE MUST BE TRANSITIVE. This was gated on
        // cell.corpus()=="cdm", so a DRR cell loaded its transitive CDM but never
        // that CDM's OWN rune-fpml. Inert while the only DRR cell was drr 6.34.1
        // (-> cdm 5.38.0, which declares no fpml dependency), but drr 7.3.0
        // -> cdm 6.21.0 -> rune-fpml 2.1.1 leaves every cdm.ingest.fpml.* function
        // without its input types. The identical one-level flaw existed independently
        // in CorpusDiagnosticGateTest — two hand-maintained closures, the same bug in
        // both, which is why the fork needs a real build-config concept rather than
        // per-loader version maps.
        {
            String fpmlVersion = resolveTransitiveFpmlVersion(cell);
            if (fpmlVersion != null) {
                Path fpmlRosettaDir = TEST_CORPUS_ROOT
                        .resolve("rune-fpml/rune-fpml-" + fpmlVersion)
                        .resolve("rosetta-source/src/main/rosetta");
                // Fail loud on a missing dependency dir, matching the transitive-CDM
                // and transitive-ISO20022 branches above — a silent skip would
                // resurface as ~112 unexplained Object-erased cdm6 ingest~fpml
                // function signatures downstream.
                if (!Files.isDirectory(fpmlRosettaDir)) {
                    throw new AssertionError("D11 " + cell
                            + " transitive-fpml dependency directory missing: " + fpmlRosettaDir
                            + " — CDM cell " + cell.version() + " maps to rune-fpml " + fpmlVersion
                            + " per CDM_TO_FPML_VERSION; expected the matching rune-fpml "
                            + ".rosetta closure at this path.");
                }
                try (var stream = Files.walk(fpmlRosettaDir)) {
                    stream.filter(p -> p.toString().endsWith(".rosetta"))
                          .sorted()
                          .forEach(p -> {
                              try {
                                  // Dependency models: NOT versionStamped — they retain
                                  // rune-fpml's own version metadata; excluded from Java
                                  // emission for CDM cells by emissionFilter.
                                  models.add(AstBuilder.buildFromFile(p));
                              } catch (Exception e) {
                                  throw new AssertionError("D11 " + cell
                                          + " transitive-fpml parse error: "
                                          + p.getFileName() + " — " + e.getMessage(), e);
                              }
                          });
                }
            }
        }

        Path rosettaDir = resolveRosettaInputDir(cell);
        List<Path> cellFiles;
        try (var stream = Files.walk(rosettaDir)) {
            cellFiles = stream.filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .collect(Collectors.toList());
        }
        // The chaos cell (v3.2 PR-2, charter § 4b) carries 22 files DESIGNED to be
        // parse-refused (BOM refusal parity with upstream — the legacy toolchain
        // refused them too, so they emitted NO goldens; skipping them here is
        // population-consistent, not divergence-masking). The skip set is the
        // committed fork-diagnostics.tsv — the ONE data source; the rune-parser
        // suite's ChaosForkDiagnosticsGateTest asserts (both directions) that the
        // live refusal set equals those rows, so a healed or new refusal fails THERE
        // by name, never silently narrowing this loader. rune-parser publishes no
        // test-jar (the #464 law), hence this second deliberate READER of the same
        // file rather than a shared helper.
        cellFiles = dropChaosExpectedRefusals(cell, cellFiles);
        // PR #413 — replay the golden build's recorded input order for the pinned file
        // groups (TWO order-sensitive kinds: package-info AND XMETA's Qualify_* list —
        // see GOLDEN_INPUT_ORDER_PINS javadoc for the upstream law + the decode).
        applyGoldenInputOrderPins(cell, cellFiles)
                  .forEach(p -> {
                      try {
                          RModel model = AstBuilder.buildFromFile(p);
                          applyVersionStamp(model, cell);
                          models.add(model);
                      } catch (Exception e) {
                          // Fail loud on cell .rosetta parse failure (Copilot R4 #4 2026-05-03).
                          // Logging-only let baseline-mode capture continue with the broken model
                          // silently dropped, turning the rest of the cell's run into secondary
                          // mismatches/missingOutput that masked the actual parse-failure root
                          // cause. Throwing aborts THIS (cell, kind) parametrized instance only.
                          throw new AssertionError("D11 " + cell + " parse error: " + p.getFileName()
                                  + " — " + e.getMessage(), e);
                      }
                  });

        return RWorkspace.build(models);
    }

    /**
     * The chaos cell's expected-refusal FILENAMES, read from the committed § 4b data
     * ({@code scripts/chaos-expander/expectations/fork-diagnostics.tsv} — the
     * {@code FORK-REFUSES} rows; other verdicts, e.g. the {@code PENDING-PR2-L2}
     * resolution-level plan row, are not parse refusals). Read per call — the loader
     * runs once per (cell, kind) load, and the file is 40 lines. Absence or an empty
     * refusal set fails LOUD: the tsv is committed, so either means a broken checkout,
     * and an empty set silently skipping nothing would mask the a5bom family's
     * existence.
     */
    /**
     * The § 4b skip, shared by every generator-side walker that ASTs a cell's sources
     * ({@link #loadCellCorpus}, {@code GoldenInputOrderPinTest}): for the chaos cell,
     * drop the 22 expected-refusal files (they parse-refuse by design and upstream
     * emitted no goldens from them); every other cell passes through untouched.
     */
    static List<Path> dropChaosExpectedRefusals(CellSpec cell, List<Path> files) {
        if (!"chaos".equals(cell.corpus())) {
            return files;
        }
        java.util.Set<String> expectedRefusals = chaosExpectedRefusalFilenames();
        return files.stream()
                .filter(p -> !expectedRefusals.contains(p.getFileName().toString()))
                .collect(Collectors.toList());
    }

    private static java.util.Set<String> chaosExpectedRefusalFilenames() {
        Path tsv = Path.of("../scripts/chaos-expander/expectations/fork-diagnostics.tsv");
        if (!Files.isRegularFile(tsv)) {
            throw new AssertionError("committed chaos expectations missing: " + tsv
                    + " — broken checkout (the § 4b SOT is committed, never generated)");
        }
        java.util.Set<String> names = new java.util.TreeSet<>();
        try {
            int lineNo = 0;
            for (String raw : Files.readAllLines(tsv, java.nio.charset.StandardCharsets.UTF_8)) {
                lineNo++;
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                // Tab-delimited config row, not language content (the no-regex law's
                // trivial-literal exception). ONE strict law across every reader of
                // this file (the cq review's MF-2): a malformed row FAILS LOUD here
                // exactly as in rune-parser's ChaosParseExpectations — a silently
                // dropped row would make this loader AST a file it must skip.
                String[] f = line.split("\t");
                if (f.length != 3) {
                    throw new AssertionError(tsv + ":" + lineNo
                            + " expected 3 tab-separated columns, found " + f.length + ": " + line);
                }
                if ("FORK-REFUSES".equals(f[1]) && !names.add(f[0])) {
                    throw new AssertionError(tsv + ":" + lineNo
                            + " duplicate expected-refusal row: " + f[0]);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + tsv, e);
        }
        if (names.isEmpty()) {
            throw new AssertionError(tsv + " carries no FORK-REFUSES rows — the § 4b"
                    + " refusal-parity contract cannot be empty while the a5bom family exists");
        }
        return names;
    }

    /**
     * P2.1.1 T3.3 — Per-cell namespace-based emission filter for
     * {@link GeneratorModel#shouldGenerate(RModel)}. Models for which the
     * filter returns {@code false} remain visible in the workspace for
     * symbol/type resolution but are not walked by any generator (ENUM /
     * POJO / METAFIELD / FUNCTION) for emission.
     *
     * <p>DRR cells additionally load the transitive-CDM AND transitive-ISO20022
     * closures into the workspace (see {@link #loadCellCorpus}) for cross-corpus
     * symbol lookup; those dependency models must NOT emit Java for the DRR
     * cell's tests (DRR's goldens contain output emitted by DRR's source —
     * including wrappers for dependency types DRR references, but excluding the
     * dependencies' own emission). This mirrors upstream Maven's "compile-time
     * dep present + emit-time own-source-only" semantics.
     *
     * <p>Non-DRR cells: accept-all predicate. The workspace always contains the
     * builtin {@code .rosetta} models (loaded for every cell from
     * {@code BUILTINS_SEARCH_ROOTS} at the top of {@code loadCellCorpus}), so
     * "accept-all" here means: the cell's own-corpus models plus the builtins
     * (all of which carry the ignored-file gate at {@code GeneratorModel.
     * IGNORED_FILES} so they never emit anyway). Only DRR cells additionally
     * load transitive-dependency models (CDM + ISO20022) into the workspace —
     * that is the case the filter exists to discriminate. The DRR branch is an
     * ACCEPT-list ({@code startsWith("drr.")}), so the ISO20022 closure added
     * at PR #155 is excluded with no filter change (the P2.1.2 future-extension
     * note that anticipated a reject-list extension is resolved by it).
     */
    static Predicate<RModel> emissionFilter(CellSpec cell) {
        // 2026-08-15 — DERIVED from the model project's OWN declared generation
        // scope, replacing filters hardcoded per corpus.
        //
        // Every model project declares which namespaces it generates, in
        // rosetta-source/src/main/resources/rosetta-config.yml under
        // `generators.namespaces`. The old hardcoded filters ("drr." accept-list,
        // "fpml." reject-list for cdm6) happened to match the five frozen cells and
        // were WRONG the moment the band expanded:
        //
        //     drr 6.34.1: [drr.*,               com.rosetta.model]
        //     drr 7.3.0:  [drr.*, cdm.ingest.*, com.rosetta.model]
        //
        // The two DRR rosetta-source poms are BYTE-IDENTICAL — nothing about the
        // build changed between the versions — so this one declared line is the ONLY
        // difference, and any DRR version can emit CDM simply by listing the
        // namespace. Hardcoding "drr." left 923 goldens PER DRR 7 CELL with no
        // emitted counterpart, which read as generator defects and were nothing of
        // the kind. Reading the declaration is not fitting the filter to the goldens;
        // it is deferring to the authority that produced them.
        //
        // Note the declaration governs which MODELS are processed, not which output
        // FILES appear: metafields for referenced types still land in the referenced
        // type's package (that is why drr 6.34.1 emits two cdm ReferenceWithMeta*
        // classes despite declaring only drr.*).
        //
        // A project shipping no config (rune-fpml) emits everything — the previous
        // default, preserved.
        List<String> declared = readGeneratorNamespaces(cell);
        if (declared.isEmpty()) {
            return model -> true;
        }
        return model -> {
            String ns = model.namespace();
            if (ns == null) {
                // Accept-list semantics. Unreachable for the current corpus: builtins
                // are dropped earlier by the IGNORED_FILES gate.
                return false;
            }
            for (String pattern : declared) {
                if (namespaceMatches(pattern, ns)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Namespace pattern match mirroring rosetta-config.yml semantics: a trailing
     * {@code .*} is a package-prefix wildcard ({@code cdm.ingest.*} matches
     * {@code cdm.ingest} and anything beneath it), anything else is an exact match
     * ({@code com.rosetta.model}).
     */
    private static boolean namespaceMatches(String pattern, String namespace) {
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return namespace.equals(prefix) || namespace.startsWith(prefix + ".");
        }
        return namespace.equals(pattern);
    }

    /**
     * The model project's declared generation scope — {@code generators.namespaces}
     * from {@code rosetta-source/src/main/resources/rosetta-config.yml} — or an
     * empty list when the project ships no config or declares no namespaces, in
     * which case everything is emitted.
     */
    static List<String> readGeneratorNamespaces(CellSpec cell) {
        Path yml = cell.root().resolve("rosetta-source/src/main/resources/rosetta-config.yml");
        if (!Files.exists(yml)) return List.of();
        try (var in = Files.newInputStream(yml)) {
            Object root = new org.yaml.snakeyaml.Yaml().load(in);
            if (!(root instanceof Map<?, ?> rootMap)) return List.of();
            Object generators = rootMap.get("generators");
            if (!(generators instanceof Map<?, ?> genMap)) return List.of();
            Object namespaces = genMap.get("namespaces");
            if (!(namespaces instanceof List<?> entries)) return List.of();
            List<String> out = new ArrayList<>();
            for (Object entry : entries) {
                if (entry instanceof String s && !s.isBlank()) {
                    out.add(s.trim());
                }
            }
            return List.copyOf(out);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + yml, e);
        }
    }

    /**
     * facet isoPruneConfig (PR #331): read the cell's model-project generator
     * configuration ({@code rosetta-source/src/main/resources/rosetta-config.yml},
     * the file upstream loads as {@code RosettaGeneratorsConfiguration}) and return
     * its {@code generators.doNotPrune} entries as {@code GeneratorModel}
     * {@code <type>#<attribute>} keys. A config-disabled (type, attribute) pair
     * renders the builder {@code prune()} KEEP form + the {@code hasData()}
     * presence-only form (see {@code ModelObjectGenerator}). Absent file / absent
     * section → empty set (all cells except iso20022 today; iso carries exactly
     * the 3 {@code ClearingPartyAndTime*Choice__1.dtls} entries).
     */
    static Set<String> readDoNotPrune(CellSpec cell) {
        Path yml = cell.root().resolve("rosetta-source/src/main/resources/rosetta-config.yml");
        if (!Files.exists(yml)) return Set.of();
        try (var in = Files.newInputStream(yml)) {
            Object root = new org.yaml.snakeyaml.Yaml().load(in);
            if (!(root instanceof Map<?, ?> rootMap)) return Set.of();
            Object generators = rootMap.get("generators");
            if (!(generators instanceof Map<?, ?> genMap)) return Set.of();
            Object doNotPrune = genMap.get("doNotPrune");
            if (!(doNotPrune instanceof List<?> entries)) return Set.of();
            Set<String> keys = new LinkedHashSet<>();
            for (Object entry : entries) {
                if (entry instanceof Map<?, ?> ref
                        && ref.get("type") instanceof String type
                        && ref.get("attribute") instanceof String attribute) {
                    keys.add(type + "#" + attribute);
                }
            }
            return Set.copyOf(keys);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + yml, e);
        }
    }

    // =========================================================================
    // Comparison engine
    // =========================================================================

    /**
     * Model-derived set of canonical output paths for every enum the cell emits
     * ({@code <package-as-dirs>/<SimpleName>.java}). Computed by running
     * {@link EnumGenerator} over the cell's emitted models and taking the resulting
     * output key set — reusing the generator's exact path logic, never a filename
     * suffix or a generated-content string match (engineering-standards compliant:
     * the parser/AST is the source of truth, not regex on structured content).
     *
     * <p>{@link #compareAgainstGolden} consults this set so RosettaEnums whose Java
     * filename does NOT end in {@code Enum.java} — ISO 20022 {@code *Code.java},
     * collision-disambiguated {@code *Code__N.java}, drr/cdm domain enums
     * ({@code CommonAssetClass}, {@code CompareOp}, …) — are classified
     * {@link ElementKind#ENUM} despite the {@link #classify}/{@link #expectedKind}
     * filename heuristic. Without it the enum pass skips their byte-compare (Pass 1)
     * and the POJO pass counts them as missing output (Pass 2) — the CODEGEN_MISSING
     * classifier-heuristic gap this set retires.
     *
     * <p>Re-renders the cell's enums (cheap: ~hundreds per cell over the cached
     * workspace) and fails loud on any enum-generation error via {@link #assertNoGenErrors}
     * — so a scoped {@code pojo_comparison}-only run (which never invokes
     * {@code enum_comparison}'s own gate) still surfaces an enum-gen regression as an ENUM
     * error, rather than silently returning an incomplete path set (which would then
     * misattribute the un-emitted enum golden as a missing POJO in Pass 2). Mirrors the
     * generator-error gating every other D11 pass applies.
     */
    private static Set<String> emittedEnumPaths(CellSpec cell, RLinkingResult corpus, GeneratorModel gm) {
        var enumGen = IRGeneration.enumGenerator(gm);
        Map<String, String> enumOutput = new LinkedHashMap<>();
        List<GenerationException> genErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                genErrors.addAll(IRGeneration.generateClasses(enumGen, model, gm.version(model), enumOutput));
            }
        }
        assertNoGenErrors(cell, ElementKind.ENUM, genErrors);
        return Set.copyOf(enumOutput.keySet());
    }

    private ComparisonResults compareAgainstGolden(CellSpec cell, Map<String, String> output, ElementKind kindUnderTest)
            throws IOException {
        // Kinds that never collide with enums (METAFIELD/FUNCTION) need no enum path set.
        return compareAgainstGolden(cell, output, kindUnderTest, Set.of());
    }

    /**
     * @param enumPaths model-derived canonical paths of the cell's emitted enums (see
     *     {@link #emittedEnumPaths}). A path in this set classifies as
     *     {@link ElementKind#ENUM} regardless of its filename suffix — a content-aware
     *     override of the {@link #classify}/{@link #expectedKind} filename heuristic, so
     *     RosettaEnums whose filenames lack {@code Enum.java} (ISO {@code *Code.java} etc.)
     *     compare in the enum pass (Pass 1) and are excluded from POJO missing-output
     *     (Pass 2). Callers that pass {@link Set#of()} (the 3-arg overload) get the pure
     *     filename heuristic.
     */
    private ComparisonResults compareAgainstGolden(CellSpec cell, Map<String, String> output,
            ElementKind kindUnderTest, Set<String> enumPaths) throws IOException {
        // v3.1 C0 item 1 — the LOUD receipt, one line per (cell, kind): how many times the
        // emitter knowingly degraded while producing THIS kind's output, and reset straight
        // after so each line covers exactly one generation (each surefire fork runs its classes
        // sequentially, and the register is reset at this class's start - round 2 - so a
        // preceding class's counts cannot leak into the first line; a parallel-THREAD runner
        // would need per-thread counters). Emitted HERE, at the start of the comparison, because it
        // must survive every gate that can fire later: a cell whose generation refused
        // fails at the generation-error gate, and a receipt printed after that gate would
        // be lost for exactly the cells that had something to report.
        System.out.println("D11 " + cell + " " + kindUnderTest + " LOUD: "
                + SilentDegradation.render());
        for (SilentDegradation.Site site : SilentDegradation.Site.values()) {
            java.util.List<String> witnesses = SilentDegradation.witnesses(site);
            if (!witnesses.isEmpty()) {
                System.out.println("  LOUD " + site + " witnesses: " + witnesses);
            }
        }
        SilentDegradation.reset();

        Path goldensDir = resolveGoldensDir(cell);
        var results = new ComparisonResults();
        // Pass 1 — iterate generator output, classify by kind, compare bytes to goldens.
        for (var entry : output.entrySet()) {
            Path javaFile = Path.of(entry.getKey());
            ElementKind kind = enumPaths.contains(entry.getKey()) ? ElementKind.ENUM : classify(javaFile);
            if (kind != kindUnderTest) continue;
            Path goldenPath = goldensDir.resolve(entry.getKey());
            if (!Files.exists(goldenPath)) {
                results.noGolden.add(entry.getKey());
                // v3.2 seat 9 (F13): a noGolden row has no golden side, so the waivered dump
                // never sees it; under the SAME flag, write the FORK's content for every
                // golden-free emission to <dir>/<cell>_<kind>/nogolden/<flat~path> - the F13
                // adjudication reads what the fork emits over the x36enum split from here.
                dumpNoGoldenIfRequested(cell, kind, entry.getKey(), entry.getValue());
                continue;
            }
            String golden = normalize(Files.readString(goldenPath));
            String generated = normalize(entry.getValue());
            if (golden.equals(generated)) {
                results.matches.add(entry.getKey());
            } else {
                // Per Copilot R1 F1 — only retain golden/generated content when
                // diagnostic dump is opt-in. Default-mode runs (CAPTURE_CONTENT=false)
                // keep Mismatch lean (file + firstDiffLine only); large failing runs
                // no longer pin tens of thousands of duplicate-content strings in heap.
                results.mismatches.add(new Mismatch(
                        entry.getKey(),
                        findFirstDiffLine(golden, generated),
                        CAPTURE_CONTENT ? golden : null,
                        CAPTURE_CONTENT ? generated : null));
            }
        }
        // Pass 2 — walk goldensDir for files of this kind we should have emitted but didn't (under-generation).
        // Strict-equality coverage gap closure: Pass 1 alone never catches files in goldens that our generator
        // stops emitting; Pass 2 surfaces that as `missingOutput`.
        try (var stream = Files.walk(goldensDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".java"))
                  .forEach(p -> {
                      String key = goldensDir.relativize(p).toString().replace('\\', '/');
                      // Enum goldens (model-derived enumPaths) are ENUM regardless of the
                      // `Enum.java`-suffix filename heuristic in expectedKind(...), so a
                      // RosettaEnum whose filename lacks the suffix is not miscounted as a
                      // missing POJO. enumPaths is empty for the METAFIELD/FUNCTION passes,
                      // which never collide with enum goldens, so this is a no-op there.
                      Optional<ElementKind> ek = enumPaths.contains(key)
                              ? Optional.of(ElementKind.ENUM)
                              : expectedKind(p);
                      ek.filter(k -> k == kindUnderTest)
                        .ifPresent(k -> {
                            if (!output.containsKey(key)) {
                                results.missingOutput.add(key);
                            }
                        });
                  });
        }
        return results;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    /**
     * THE PREFIX COMPARE (v3.3 seat 9, PR #645 commit 5): the 1-based line at which a rendered PREFIX first departs
     * from the golden, or 0 when the golden's first N lines ARE the prefix. N is the prefix's own line count - a
     * final newline closes the last line and does not open another, so it is not counted. A golden that RUNS OUT
     * inside the prefix diverges at the line after its last, which is a real divergence and not a tie.
     *
     * <p>It is a line-by-line byte compare of GENERATED text, exactly as {@link #findFirstDiffLine} is, and it says
     * nothing about the golden's remaining lines: the prefix is the only thing the emitter claims to have written.
     */
    private static int findFirstPrefixDiffLine(String golden, String prefix) {
        String[] gl = golden.split("\n", -1);
        String[] pr = prefix.split("\n", -1);
        int n = pr.length;
        if (n > 0 && pr[n - 1].isEmpty()) {
            n--;   // the prefix's final newline closes its last line; it opens no further one
        }
        for (int i = 0; i < n; i++) {
            String g = i < gl.length ? gl[i] : "<EOF>";
            if (!g.equals(pr[i])) {
                return i + 1;
            }
        }
        return 0;
    }

    private static int findFirstDiffLine(String golden, String generated) {
        String[] gl = golden.split("\n", -1);
        String[] ge = generated.split("\n", -1);
        int max = Math.max(gl.length, ge.length);
        for (int i = 0; i < max; i++) {
            String g = i < gl.length ? gl[i] : "<EOF>";
            String e = i < ge.length ? ge[i] : "<EOF>";
            if (!g.equals(e)) return i + 1;
        }
        return 0;
    }

    // P2.1.1 T3.1 — content fields enable -Dd11.dump-content diagnostic
    // (full golden vs generated dump for first N unwaived mismatches; see
    // assertCellKind). Per Copilot R1 F1, content fields are populated only
    // when CAPTURE_CONTENT (CAPTURE_CONTENT = DUMP_CONTENT_MAX > 0); in default
    // mode (DUMP_CONTENT_MAX == 0) they stay null so large failing runs don't
    // retain duplicate file content in heap.
    record Mismatch(String file, int firstDiffLine, String golden, String generated) {}

    static class ComparisonResults {
        final List<String> matches = new ArrayList<>();
        final List<Mismatch> mismatches = new ArrayList<>();
        final List<String> noGolden = new ArrayList<>();
        final List<String> missingOutput = new ArrayList<>();

        String summary() {
            return "identical=" + matches.size()
                    + " mismatches=" + mismatches.size()
                    + " noGolden=" + noGolden.size()
                    + " missingOutput=" + missingOutput.size()
                    + " emitted=" + (matches.size() + mismatches.size() + noGolden.size())
                    + " expected=" + (matches.size() + mismatches.size() + missingOutput.size());
        }
    }
}
