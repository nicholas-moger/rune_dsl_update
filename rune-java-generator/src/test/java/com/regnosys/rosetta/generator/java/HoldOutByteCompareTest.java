package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.JavaPackageInfoGenerator;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathUtilGenerator;
import com.regnosys.rosetta.generator.java.object.validators.CardinalityValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * W42 sweep leg B (hold-out corpora), slice 2 — the byte-compare bar (PR #410).
 *
 * <p>Slice 1 ({@link HoldOutGenerationTest}) proved the golden-free bars (crash-freedom,
 * determinism, file sanity) over model groups the engine never gated on. This test adds
 * the fourth bar: the fork's output must BYTE-MATCH goldens pinned by the RELEASED
 * 9.83.0 {@code rosetta-maven-plugin} (the oracle law — never the vendored
 * {@code rune-dsl/} source, which is {@code 0.0.0.main-SNAPSHOT}, post-9.83). Goldens are
 * pinned by {@code scripts/holdout-oracle/run-holdout-oracle.sh} (two oracle runs,
 * byte-agreement gated) into {@code src/test/resources/holdout-goldens/<group>/} —
 * deliberately OUTSIDE {@code test-corpus/} so the frozen-baseline manifest is untouched.
 *
 * <p><b>The full-kind battery.</b> The oracle emits EVERY generator family in one tree
 * (data types, choices, enums, functions, rules, reports, label providers, metafields,
 * only-exists + cardinality + type-format validators, XMeta registries, deep-path utils,
 * data rules, package-info) — so this test runs the fork's full battery, mirroring the
 * D11 per-kind wiring verbatim ({@link D11CorpusRegressionTest}), where slice 1
 * deliberately ran only the pre-coverage-wave set.
 *
 * <p><b>The version-stamp law</b> (shared by BOTH hold-out bars since PR #415 —
 * {@code HoldOutGenerationTest} delegates to this battery):
 * upstream's EMF model layer defaults an undeclared model version to {@code "0.0.0"}
 * ({@code RosettaModelImpl.VERSION_EDEFAULT}, oracle-probe-verified: unversioned hold-out
 * fixtures emit {@code @version 0.0.0}), while the fork's {@code RModel} keeps the
 * version ABSENT and {@code GeneratorModel#version} returns null. The loader here stamps
 * {@code "0.0.0"} onto version-ABSENT models only — declared versions (hero-model's
 * {@code version "test"}) pass through verbatim on both sides.
 *
 * <p><b>Population.</b> Two hold-out groups are formatter-INPUT fixtures with
 * deliberately unresolvable references ({@code formatting-nestedConstructor},
 * {@code formatting-onlyExists}); the oracle REFUSES them ("severe validation error" —
 * upstream's mojo validation gate), so no golden is definable and they sit out this bar
 * (slice 1's crash-freedom bars keep covering them; the fork's library-level pipeline has
 * no mojo-layer validation gate — refusal is the embedding build's concern, a documented
 * design difference, not tolerable drift). The two alias-only groups pin EMPTY goldens
 * (zero emission on both sides is asserted — an absent goldens dir means an empty set,
 * because git does not track empty directories).
 *
 * <p>Comparison normalizes line endings only (the D11 convention — the declared
 * normalization of the four-layer gate). ANY divergence fails: a hold-out miss is a
 * facet lead feeding the D34 taxonomy, never a new waiver bucket.
 *
 * <p>Skips (does not fail) when the builtins are absent (CI / fresh clone), mirroring
 * the corpus-absent convention of the D11 gate.
 */
class HoldOutByteCompareTest {

    static final Path HOLDOUT_ROOT = Path.of("src/test/resources/holdout");
    private static final Path GOLDENS_ROOT = Path.of("src/test/resources/holdout-goldens");

    // -Dholdout.dump-dir=<ABSOLUTE path> — for every divergent file, write BOTH sides
    // (golden + generated) for offline diff triage, mirroring the D11 dump convention.
    // Dump dirs are SINGLE-USE (the standing law). Opt-in; default no-op.
    private static final String DUMP_DIR = System.getProperty("holdout.dump-dir", "").trim();
    static {
        if (!DUMP_DIR.isEmpty() && !Path.of(DUMP_DIR).isAbsolute()) {
            throw new IllegalArgumentException("-Dholdout.dump-dir must be an ABSOLUTE path"
                    + " (got '" + DUMP_DIR + "') — a relative value resolves MODULE-relative"
                    + " under `mvn -f rune-java-generator/pom.xml`.");
        }
    }

    /**
     * Oracle-rejected groups: formatter-input fixtures whose references are deliberately
     * unresolvable — the released plugin fails validation on them, so no byte golden
     * exists to compare against. See the class Javadoc "Population" note.
     */
    static final Set<String> ORACLE_REJECTED =
            Set.of("formatting-nestedConstructor", "formatting-onlyExists");

    /**
     * Upstream's undeclared-model-version default — {@code RosettaModelImpl.VERSION_EDEFAULT}
     * in the 9.83.0 EMF model layer (the vendored source documents the mechanism; the
     * oracle probe verified the emission: {@code @version 0.0.0} on every unversioned
     * fixture's output).
     */
    // Package-private: UpstreamPortHarness (PR #414 leg-C port) replays the same
    // version law on in-memory snippet models.
    static final String UPSTREAM_VERSION_DEFAULT = "0.0.0";

    /**
     * PINNED FACET LEADS (Freezing pattern — the D11 waiver discipline applied to the
     * hold-out bar; un-pin DELIBERATELY when the facet lands). EMPTY since PR #419 (re-opened and
     * re-emptied WITHIN #625 — the chain's tail):
     * BOTH #418 leads healed first-run from their banked maps — (1) expr-sort-min-max
     * landed as facet sortMinMaxClosureParam (sort/min/max comparator-key owners
     * joined the #342 closureParamDirectNav owner set in
     * {@code NavigationHandler.resolveReceiverDataType}'s closure arm — the ONE gate
     * whose null had degraded the whole lockstep cascade: the wrap-elision, the
     * {@code <BigDecimal>} witness and the item-type-derived inner lambda name all
     * cascaded green off the single resolution); (2) expr-conversions-valid landed as
     * facet conversionAliasSigTypes ({@code FunctionAliasHelper}'s RConversionExpr
     * arm extended to the five NULL kinds — the Seat-1 #418 MF-1-corrected
     * output-fallback leak) + facet toTimeLambdaEscape ({@code ConversionHandler}'s
     * to-time lambda param routed through the deferred lambda-param channel — the
     * `_s` underscore escape at finalization, upstream's createUniqueIdentifier law).
     * All 239 hold-out goldens byte-identical; the CompileGate fork-lead set and the
     * WiderPool sort pin retired in the same change.
     *
     * <p>Contract (unchanged for future leads): a path listed here MUST mismatch (a
     * heal fails the test loudly so the entry is un-pinned in the same change); any
     * divergence NOT listed fails; missing and extra files always fail. EMPTIED at
     * PR #412; RE-OPENED #416; EMPTIED #417 (the default-op heal); RE-OPENED #418
     * (the burn-4 leads); EMPTIED at PR #419 (both heals); RE-OPENED #625 (v3.2 seat 4, commit 2 — the
     * ten order-dependence oracle groups' 13 mismatching files over 8 groups); EMPTIED at #625 (commit 3,
     * the fix — every group byte-identical); RE-OPENED #626 (v3.2 seat 5, commit 2 — the six F6 + F3 oracle
     * groups' 24 mismatching files, every one ALSO non-compiling: the compile gate's twin set); RE-OPENED #627
     * (v3.2 seat 6, commit 2 — the two deep-path oracle groups' 16 mismatching files, every one ALSO non-compiling:
     * a deep feature call whose receiver is a list-op lambda's item, rendered as the {@code TODO(M7b-4)} placeholder
     * with no util injected); EMPTIED of them at #627 (commit 4 — the fix at commit 3 rendered all 16 byte-identical
     * on its first measured run, {@code target/v32-seat6-instruments/scratch/t-holdout-s6-fix2.log}, local; InLambda
     * the one declared pin that stays); RE-OPENED #628 (v3.2 seat 7, commit 2 — the core choice-switch-in-lambda
     * group's two CONTROLS, which the oracle turned into FINDINGS: the fork's emissions for the two switch-in-lambda
     * forms it does NOT refuse, both non-compiling at the pre-fix head ({@code scratch/battery-pre2.status}, local —
     * the compile gate's twin set). ControlEnum: the ENUM-keyed switch in an extract lambda fell to the
     * {@code Objects.equals(Red, item) ? … : …} ternary (a bare enum-value name, no class) where the released plugin
     * renders the #379 {@code ==}-guard block — the #379 emitter had DECLINED on the non-empty literal default.
     * ControlLiteral: the LITERAL-keyed switch in an extract lambda over a list literal had NO block renderer at all —
     * the {@code Objects.equals("r", item)} ternary it emitted compiles and is always false (a WRONG-RESULT guard),
     * while the FILE did not compile: the alias signature leaked the raw rune name {@code string} through the
     * signature walk's output-type fallback); EMPTIED of them at #628 (commit 4 — both byte-identical at the fix,
     * commit 3: the bar's "now byte-IDENTICAL" print, {@code scratch/d11-fix3.status} / {@code d11-fix4.status});
     * RE-OPENED #630 (v3.2 seat 9, commit 3 — the F1 / F8 oracle groups' eight mismatching files at the commit's
     * content, {@code target/v32-seat9-instruments/scratch/t-holdout-fix1.log}: the six F1 enums whose annotation
     * displayName / synonym the fork Java-escapes where the released plugin renders it RAW, and the two F8 edge files
     * that carry the {@code nothing} RENDER laws the D47 mapping does not reach — every one of the eight COMPILES, so
     * the compile gate's twin set was unchanged AT COMMIT 3 (round 2 added two to it — see below)); EMPTIED of the six
     * F1 files at #630 (commit 4 — the D46 fix: all six byte-identical on its first measured run, the bar's "now
     * byte-IDENTICAL" print at {@code scratch/t-holdout-f1b.log}; EscapeEnum moved to the compile gate's UPSTREAM-BUG
     * set, being the released plugin's own non-compiling emission reproduced byte for byte); RE-OPENED #630 round 2
     * (commit 14 — the c12 sweep's separating shapes, put to the released plugin BEFORE the code: {@code SetMetaVoid}
     * (a Void value SET into a scheme-annotated Void output — the fork's {@code toBuilder(<Void>)} non-compiling) and
     * {@code ExistsThenConditional} (a literal-armed conditional handed to {@code areEqual} — non-compiling), each the
     * fork's own divergent render on a legal shape the plugin accepts, so each joins the compile gate's twin set too;
     * DECLARED, the heal the F8 lineage's last leg — these two STAY); round 3 (commit 17 — the code-quality review's
     * MF-1, the spec review's SF-6) added FOUR byte-identical groups with NO new pin: void-mapping-segment-conditional-set
     * (the operation seat's third path, healed), void-mapping-collapsed-receiver and void-mapping-exists-then-clean (the
     * hoist-counter witnesses — the counter advances on both sides) and void-mapping-empty-else.
     */
    // round 2 (cq SF-8, round-1 NIT-8): the seat-9 note that sat INSIDE the Map.of initializer, moved above the field
    // v3.2 seat 9 (F1 / F8 / F13), commit 3 - the five groups pinned from the released plugin BEFORE the code, the
    // files the fork MISMATCHES at the commit's content declared (scratch/t-holdout-fix1.log, local) so the bar
    // stays green and the defect stays visible. F1 (D46, upstream wins): the golden's @RosettaEnumValue
    // displayName is RAW and the constructor argument Java-escaped; the fork escaped once (EnumGenerator) and
    // read the one string at both seats (java-enum.stg) - the four enums of the core group and the two of the
    // edge were pinned here at commit 3 and UN-PINNED at commit 4 (the D46 fix: one escape law at the
    // constructor; EscapeEnum - RAW even for a quote / a backslash / a tab, upstream's own NON-COMPILING
    // emission - is the compile gate's upstream-bug pin now; SynonymEnum's @RosettaSynonym value RAW too).
    // F8 (D47, upstream wins): the D47 mapping (commit 3 - a model-declared basicType / recordType is `nothing`,
    // Void at the Java seat) renders the core group WHOLE (15/15, ListCarrier compiling again) and 22 of the
    // edge's 24, the missing / extra FieldWithMetaVoid pair gone; the two that stayed were the `nothing` RENDER
    // laws, not the type: UseToken's `set r: t` rendered `r = t` where the plugin renders `r = null` (the
    // assignment seat), ConditionCarrierTokPresent's `tok exists` rendered the getter map where the plugin
    // renders `exists(MapperS.<Void>ofNull())` (the mapper seat) - pinned here at commit 3 and UN-PINNED at the
    // render-law commit (a Void-item value is the EMPTY of the expected type at the coercion seats the render
    // groups pin - the operation seat and the existence operand; the two render-edge groups pinned from the
    // released plugin BEFORE that code decide the wider arms - see HoldOutGenerationTest's registry note; round
    // 1 narrowed the STATED scope to those seats and banked the rest - cq SF-7).
    // v3.2 seat 5 (F6 + F3), commit 2 — the six groups pinned from the released plugin BEFORE the code: the
    // files the fork MISMATCHES today (measured by this bar at the pre-fix head), declared so the bar stays
    // green and the defect stays visible. F3: the beyond-long literal's BigInteger -> BigDecimal null guard
    // rendered as the lambda channel's Mapper ternary at the STATEMENT seats (and raw at the whole-set, add
    // and alias seats) where the released plugin emits the if/else block. F6: `with-meta` on an ALREADY-
    // wrapped argument hoisted as the value type where the plugin hoists the wrapper's builder and stamps
    // through getOrCreateMeta(); the meta ladder's ALIAS rung assigned unwrapped where the plugin hoists the
    // wrapper into a numbered local and null-guards getValue(). Un-pinned at the fix commits: arm (a) commit 3
    // (the with-meta groups whole), arm (c) commit 5 (six of the seven meta-ladder files), arm (d) commit 7 (the
    // two conv-bigint groups whole - nine files: the conditional arm at the statement sink incl. the nested, the
    // both-arms and the multi-output forms, the whole set, the add seat, the alias seat, the int output).
    // v3.2 seat 5, arm (c) - the F6-Sieve fix (the alias-call collapse typed from the alias signature walk,
    // the only-element rung recovered at the deref seat) healed six of the seven meta-ladder files first-run.
    // InLambda STAYS PINNED, DECLARED: it carries NO alias - it is the DIRECT ladder inside an `extract h [ ... ]`
    // lambda with an EXPLICIT filter parameter, and its two remaining diff lines are lambda-channel TYPING gaps of
    // the F4 (lambda-type-join) family, not this seat's mechanism: (1) the explicit closure param `c` over the
    // then-bound MapperC<FieldWithMetaString> compiles null-typed, so the comparison operand keeps the bare `c`
    // where the plugin derefs `c.<String>map("Type coercion", ...)` (HandlerHelper.bareItemThenPipeMetaType admits
    // the IMPLICIT item only - the explicit twin is unwitnessed at the corpus); (2) the block-bodied `mapItem`
    // extract publishes no chain type (CollectionHandler's extract stamp: `mapItem` MEASURED EMPTY at seat 30,
    // 0 of 132 rows, deliberately un-stamped), so the multi whole-output SET's wrapper -> item coercion
    // (`.<String>map("Type coercion", ...)` before `.getMulti()`) stays blind. BANKED with this golden as the
    // witness of both; the chaos cell carries neither shape (C9Sieve = the Sieve fixture, 12/12 healed).
    // v3.2 seat 9 ROUND 2 - the two round-2 entries' notes, moved out of the Map.of initializer at round 4 (spec NIT-5 /
    // round-2 cq NIT-8; re-flowed with their paren at round 5, spec NIT-1 / cq NIT-1): the SetMetaVoid entry (the c12
    // sweep's separating shapes put to the released plugin at 5e84fc2c3 - scratch/oracle-s9f.status; the fork against them
    // scratch/t-holdout-r2a.log): a Void value SET into a scheme-annotated Void output - the plugin renders the EMPTY
    // wrapper `toBuilder(FieldWithMetaVoid.builder().build())` (the value dropped at its coercion), the fork
    // `toBuilder(<the Void value>)`, non-compiling (the compile gate's twin set). The seat's META-OUTPUT decline at the
    // render seat hands the value to the plain path; the heal is the F8 lineage's last leg (the Void wrapper minted at a
    // meta output), a seat of its own - DECLARED, not hidden.
    // The ExistsThenConditional entry - THE ORACLE'S OWN CATCH: the group written to settle the naming question (it did -
    // the kept conditional after a discarded Void one is the BARE `ifThenElseResult` on both sides) found an OLDER fork
    // defect beside it: a LITERAL-armed conditional as a COMPARISON operand (`(if flag then "a" else "b") = "a"`) renders
    // as a plain `String ifThenElseResult = "b"; if (...) { ifThenElseResult = "a"; }` where the released plugin hoists a
    // `final MapperS<String>` and assigns `MapperS.of("a")` / `MapperS.of("b")` arm by arm; the fork's String then
    // reaches `areEqual(...)`, which does not compile (the compile gate's twin set). Not the Void law's: the Void half
    // of the expression is byte-identical. DECLARED, banked for the heal (scratch/t-targeted-r2a.log, holdout-r2b-dump/).
    // v3.2 seat 11 (PR #632, M1 / D50 — the file-scope first-claim law at six kinds): the five type-named-* groups
    // (232 goldens pinned from the released plugin BEFORE the code, target/v32-seat11-instruments/scratch/
    // oracle-s11a.status, local) measured 155 / 232 IDENTICAL at the pre-code head (scratch/battery-pre.log:
    // list 4 / 17, util 42 / 60, guava 13 / 20, rosetta 56 / 85, annotations 40 / 50 - the mismatches
    // 13 + 18 + 7 + 29 + 10 = 77; until round 4f of the PR read the print back this comment had put the mismatch
    // counts under the identical total, a copy of commit 2's message without its "77 mismatches" lead-in) and
    // 231 / 232 at the fix (scratch/battery-c4.log). The ONE residual is the FUNCTION kind: UseList's signature
    // writes `List<String>` as a literal beside a model input typed by the data type `List`, and the fork imports
    // BOTH names (`import holdout.typenamedlist.List;` + `import java.util.List;`) where the released plugin claims
    // java.util.List at the RETURN type — the first write in text order — and writes the model List canonical,
    // importing it nowhere. DECLARED here and at the compile gate (`reference to List is ambiguous`); the heal is D50's
    // banked FUNCTION kind — the signature and body tokens as sentinels at the function generator — a seat of its own,
    // not this seat's six data-type kinds.
    // v3.2 seat 12 (PR #633, H1 = M12 / D52): the group closure-param-duplicate (9 goldens pinned from the released
    // plugin BEFORE the code, target/v32-seat12-instruments/scratch/oracle-s12h2.status, local) measured 6 / 9 identical
    // at the pre-code head (scratch/battery-pre-h.log: ReduceDup, ReduceDupMultiply, ReduceDupThenUse mismatching -
    // the fork's `(a, a) -> ...add(a, a)`) and 8 / 9 at the H1 fix (scratch/battery-h1b.log). THE ONE RESIDUAL,
    // ReduceDupThenUse, is NOT the naming - its pair reads `(a0, a1)` with every read `a0` - but an OLDER mechanism
    // the fixture's third function surfaced (the new-fixture law): a CONDITIONAL as the whole BODY of a REDUCE lambda
    // renders the inline ternary `cond ? add(a0, a0) : a0` where the released plugin renders the BLOCK lambda
    // `(a0, a1) -> { if (cond) { return ...; } return a0; }` - the single-parameter block forms
    // (CollectionHandler.compileLadderConditionalBlock and its siblings) have no two-parameter reduce twin. DECLARED
    // here with the golden as its witness; BANKED for a seat of its own (the reduce twin of the block forms). The chaos
    // M12 row (C95Reduce, a plain body) is the heal's target.
    // v3.2 seat 13 (PR #634, D53 - the closing seat, commit 4): TWO of the five pins MOVED. SetMetaVoid is REFUSED at
    // the seat's register site R13 (VOID_INTO_META_OUTPUT) - a declared refusal now, in DECLARED_REFUSALS below
    // (LAW 73), its compile-gate pin dropped with it; ExistsThenConditional is HEALED - the equality parent joined the Mapper-form slot and the
    // hoist block renders the bare-boolean guard (`final MapperS<String> ifThenElseResult0;` under
    // `if ((flag == null ? false : flag))`), byte-identical at the seat's c4e battery. The c4g battery's OWN CATCH:
    // meta-ladder-alias-rung-edge's InLambda (the seat-5 lambda-channel edge, pinned since #626) is byte-IDENTICAL too -
    // the R12 heal's element deref after the block lambda closed both F4-family gaps its note named - un-pinned here
    // and at the compile gate. TWO byte pins remain: UseList (the FUNCTION kind's library-token literals, D50's banked
    // seat) and ReduceDupThenUse - the seat-12 pin RESTORED at the R14 narrowing (the whole-suite pre-screen on the offload box
    // offload box: R14 refuses only a ternary whose arms' item types differ, and this file's `cond ? add(a0, a0) : a0`
    // has agreeing arms - the fork EMITS it again, compiling, byte-different from the plugin's block lambda; the older
    // mechanism the seat-12 note names, banked for the reduce twin of the block forms).
    private static final Map<String, Set<String>> PINNED_FACET_LEADS = Map.of(
            "type-named-list", Set.of("holdout/typenamedlist/functions/UseList.java"),
            "closure-param-duplicate", Set.of("holdout/closureparamduplicate/functions/ReduceDupThenUse.java"));

    /**
     * v3.2 seat 4 (PR #625, F10) — the #413 law at this bar: the released plugin's qualifiable root is the
     * FIRST configuration per kind in its index order, an order that follows the resource PATH (the three
     * `qualify-order-probe-*` groups: the same files won differently under two directory names; a hash over
     * resource URIs is consistent with it and INFERRED, not measured), which no other machine's paths reproduce. Where a golden fixes the winner and the sorted full-path walk
     * does not put it first, the group's pinned file is loaded FIRST — the fork's rule (the first
     * configuration in load order) then replays upstream's answer. Groups absent here load sorted.
     */
    static final Map<String, String> GOLDEN_QUALIFIABLE_ROOT_ORDER_PINS = Map.of(
            "qualify-first-wins", "qualify-first-wins-b.rosetta",
            "zz-qualify-order-probe-c", "probe-c.rosetta");

    /** The group's model files: the sorted full-path walk, the group's pinned file (if any) moved first. */
    static List<Path> groupFiles(Path groupDir) throws IOException {
        List<Path> files;
        try (var stream = Files.walk(groupDir)) {
            files = new ArrayList<>(stream.filter(p -> p.toString().endsWith(".rosetta"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList());
        }
        String pinned = GOLDEN_QUALIFIABLE_ROOT_ORDER_PINS.get(groupDir.getFileName().toString());
        if (pinned != null) {
            Path first = files.stream().filter(p -> p.getFileName().toString().equals(pinned)).findFirst()
                    .orElseThrow(() -> new AssertionError(groupDir.getFileName() + ": the pinned first file "
                            + pinned + " is not in the group"));
            if (files.indexOf(first) == 0) {
                // round 1 (cq NIT-6): a pin that no longer moves anything is STALE (a rename, a group edit) -
                // it fails loudly like a missing one, never sits silently in the map
                throw new AssertionError(groupDir.getFileName() + ": the pinned first file " + pinned
                        + " already sorts first - a no-op pin; delete it or re-measure the group");
            }
            files.remove(first);
            files.add(0, first);
        }
        return List.copyOf(files);
    }

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            Path.of("../test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            Path.of("../rune-dsl/rune-runtime/src/main/resources/model")
    );

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    static Stream<Path> byteCompareGroups() throws IOException {
        try (var stream = Files.list(HOLDOUT_ROOT)) {
            return stream.filter(Files::isDirectory)
                    .filter(p -> !ORACLE_REJECTED.contains(p.getFileName().toString()))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList().stream();
        }
    }

    /**
     * Seat-1 #410 OBS-2: the parametrized bar iterates FIXTURE groups, so a goldens
     * dir with no fixture counterpart (an orphan — e.g. a renamed group leaving stale
     * goldens behind) would silently never be compared. This lock makes every goldens
     * dir claim a fixture group.
     */
    @org.junit.jupiter.api.Test
    void every_goldens_dir_has_a_fixture_group() throws IOException {
        if (!Files.isDirectory(GOLDENS_ROOT)) {
            return; // no goldens pinned at all — nothing to orphan-check
        }
        Set<String> fixtureGroups;
        try (var stream = Files.list(HOLDOUT_ROOT)) {
            fixtureGroups = stream.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(java.util.stream.Collectors.toSet());
        }
        List<String> orphans;
        try (var stream = Files.list(GOLDENS_ROOT)) {
            orphans = stream.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> !fixtureGroups.contains(name))
                    .sorted()
                    .toList();
        }
        Assertions.assertTrue(orphans.isEmpty(),
                "orphan goldens dir(s) with no fixture group — stale goldens are never"
                        + " compared; delete or restore the fixtures: " + orphans);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("byteCompareGroups")
    void holdout_group_byte_matches_oracle_goldens(Path groupDir) throws IOException {
        List<Path> builtinFiles = resolveBuiltinFiles();
        Assumptions.assumeTrue(!builtinFiles.isEmpty(),
                "rune-dsl builtins absent (searched " + BUILTINS_SEARCH_ROOTS + ") — hold-out skipped");

        String group = groupDir.getFileName().toString();
        Map<String, String> generated = generateAllKinds(groupDir, builtinFiles);
        Map<String, String> golden = loadGoldens(GOLDENS_ROOT.resolve(group));

        List<String> mismatches = new ArrayList<>();
        List<String> missing = new ArrayList<>();   // golden exists, fork did not emit
        List<String> extra = new ArrayList<>();     // fork emitted, no golden
        int identical = 0;

        Set<String> allPaths = new TreeSet<>();
        allPaths.addAll(golden.keySet());
        allPaths.addAll(generated.keySet());
        StringBuilder firstDiff = new StringBuilder();
        for (String path : allPaths) {
            String g = golden.get(path);
            String f = generated.get(path);
            if (g == null) {
                extra.add(path);
            } else if (f == null) {
                missing.add(path);
            } else if (normalize(g).equals(normalize(f))) {
                identical++;
            } else {
                mismatches.add(path);
                if (firstDiff.length() == 0) {
                    firstDiff.append(firstDiffExcerpt(path, normalize(g), normalize(f)));
                }
                dumpDivergent(group, path, g, f);
            }
        }

        // v3.2 seat 7: a DECLARED refusal's target path is an EXPECTED missing golden (admitDeclaredRefusals has
        // already proven the refusal fired); every other missing file fails as it always did.
        Set<String> declaredMissing = new java.util.HashSet<>();
        for (DeclaredRefusal d : declaredRefusals(group)) {
            declaredMissing.add(d.targetPath());
        }
        List<String> undeclaredMissing = missing.stream()
                .filter(p -> !declaredMissing.contains(p)).toList();
        Set<String> pinned = PINNED_FACET_LEADS.getOrDefault(group, Set.of());
        List<String> unpinnedMismatches = mismatches.stream()
                .filter(p -> !pinned.contains(p)).toList();
        List<String> healedPins = pinned.stream()
                .filter(p -> !mismatches.contains(p)).sorted().toList();

        System.out.println("HOLDOUT-BYTE " + group + ": identical=" + identical
                + " mismatch=" + mismatches.size() + " missing=" + missing.size()
                + " extra=" + extra.size() + " (golden total " + golden.size()
                + (declaredMissing.isEmpty() ? "" : "; declared refusals: " + declaredMissing.size())
                + (pinned.isEmpty() ? ")" : "; pinned facet-lead files: " + pinned.size() + ")"));

        if (!unpinnedMismatches.isEmpty() || !undeclaredMissing.isEmpty() || !extra.isEmpty()) {
            Assertions.fail(group + ": hold-out byte-compare divergence — a facet lead, never a"
                    + " waiver (the leg-B contract).\n"
                    + "  mismatches (" + unpinnedMismatches.size() + "): " + unpinnedMismatches + "\n"
                    + "  missing    (" + undeclaredMissing.size() + "): " + undeclaredMissing + "\n"
                    + "  extra      (" + extra.size() + "): " + extra + "\n"
                    + firstDiff);
        }
        if (!healedPins.isEmpty()) {
            Assertions.fail(group + ": pinned facet-lead file(s) now byte-IDENTICAL — the facet"
                    + " landed; un-pin deliberately (remove from PINNED_FACET_LEADS in the same"
                    + " change): " + healedPins);
        }
    }

    /**
     * One full-battery pipeline run: parse builtins + group (version-stamped), link,
     * generate ALL kinds. Package-private: {@code HoldOutCompileGateTest} (the PR #412
     * real-javac bar) reuses this exact pipeline so the compiled sources and the
     * byte-compared sources can never drift. Fails the calling test on ANY generation
     * error (the oracle-comparable groups are all error-free by contract).
     */
    static Map<String, String> generateAllKinds(Path groupDir, List<Path> builtinFiles)
            throws IOException {
        List<Path> groupFiles = groupFiles(groupDir);
        Assertions.assertFalse(groupFiles.isEmpty(),
                groupDir.getFileName() + ": no .rosetta files in the group");
        GenerationRun run = generateAllKindsFromFiles(groupFiles, builtinFiles);
        return admitDeclaredRefusals(groupDir.getFileName().toString(), run);
    }

    /**
     * One pipeline run's outcome: the emitted files plus the generation errors themselves — each carrying the TARGET
     * PATH the writer seam attributed (a refused function's would-be file, {@code GenerationException.getTargetPath()};
     * v3.2 seat 7 reads it to match a refusal against {@link #DECLARED_REFUSALS}). {@link #errorMessages()} keeps the
     * pre-seat message view for the classification bars.
     */
    record GenerationRun(Map<String, String> output, List<GenerationException> errors) {
        List<String> errorMessages() {
            return errors.stream().map(GenerationException::getMessage).toList();
        }
    }

    /**
     * v3.2 seat 7 (F11), commit 2 — A DECLARED FORK REFUSAL on a hold-out group: the released plugin emitted the golden at
     * {@code targetPath}, the fork REFUSES the element at the LOUD register {@code site} (a {@code GenerationException}
     * whose message carries {@code needle}) and emits nothing. The {@code PINNED_FACET_LEADS} Freezing contract one seat
     * earlier in the pipeline: every entry MUST refuse today ({@link #admitDeclaredRefusals} fails loud when a declared
     * refusal stops firing — un-declare it in the same change), a refusal NOT declared fails as before, and the byte bar
     * treats exactly the declared target paths as the group's expected {@code missing} set. Keyed by the target path so
     * ONE declaration names the missing golden and the refusing element at once.
     */
    record DeclaredRefusal(String targetPath, String site, String needle) { }

    /**
     * v3.2 seat 7 (F11), commit 2 — the two choice-switch-in-lambda oracle groups pinned from the released plugin BEFORE
     * the code ({@code target/v32-seat7-instruments/scratch/oracle-s7c.status} the core group, 45 goldens;
     * {@code oracle-s7a.status} the edge group, 58 — the core group's first two cuts REFUSED by the plugin on two grammar
     * laws: a closure parameter may not be named {@code e}, and inside a switch case only {@code item} is case-narrowed,
     * never the explicit closure parameter): the fork REFUSES every element carrying a CHOICE-keyed {@code switch} inside
     * a list-op lambda whose arms are case-narrowed navigations / a to-string over one / a non-empty literal default
     * ({@code ControlFlowHandler.renderGuard}'s TYPE_SWITCH_TERNARY_STUB — the chaos s18 {@code C18ToKind} class, 12
     * declared rows), measured by this battery at the pre-fix head ({@code scratch/battery-pre.status}: 8 + 8 refusals,
     * every one this site). Sixteen declared, both routes; the three controls and {@code Fmt} emit. Un-declared at the
     * fix commit as they heal.
     */
    // v3.2 seat 7, commit 4: EMPTIED - all sixteen refusals healed at the fix (commit 3): every switch-carrying
    // element of both groups emits, and every one of the 103 goldens is byte-identical and compiles
    // (target/v32-seat7-instruments/scratch/d11-fix4.status, the battery run under -Dholdout.declared-refusals.off=true
    // at the fix content, local). The mechanism stays as the belt for the seats to come.
    // v3.2 seat 13 (PR #634, D53, commit 4): the former byte pin the closing seat REFUSES - SetMetaVoid at R13 (a Void
    // value into a with-meta output; the released plugin renders the EMPTY wrapper, the v3.3 heal), admitted by SITE and
    // message needle; the generation bar's count for the group drops by the refused file. (ReduceDupThenUse sat here for
    // the first cut of R14, which refused the last-resort ternary WHOLE; the narrowed R14 - arms whose item types
    // differ - lets its agreeing-arm ternary through, so it is a byte pin again above.)
    static final Map<String, List<DeclaredRefusal>> DECLARED_REFUSALS = Map.of(
            "void-meta-output-set", List.of(new DeclaredRefusal(
                    "holdout/voidmetaoutput/functions/SetMetaVoid.java", "VOID_INTO_META_OUTPUT",
                    "a Void-typed value assigned to the meta-annotated output")));

    /**
     * v3.2 seat 7: the ONE reader of {@link #DECLARED_REFUSALS} the three bars consult. Under
     * {@code -Dholdout.declared-refusals.off=true} — a MEASUREMENT run at a fix content, never the gate's setting — every
     * group reads an EMPTY declaration set, so the bars report the group's byte / compile state (every refusal that still
     * fires fails as undeclared, every file that emits compares) instead of failing at declarations the fix has healed;
     * the un-declare commit reads that print, exactly as the pin commits read the byte bar's "now byte-IDENTICAL" list.
     */
    static List<DeclaredRefusal> declaredRefusals(String group) {
        if (Boolean.getBoolean("holdout.declared-refusals.off")) {
            return List.of();
        }
        return DECLARED_REFUSALS.getOrDefault(group, List.of());
    }

    /**
     * v3.2 seat 7: admit a run's generation errors against the group's {@link #DECLARED_REFUSALS} and return its output —
     * the ONE consult for every bar (the byte bar and the compile gate through {@link #generateAllKinds}, the generation
     * bar through its own {@code generateOnce}). Each declared refusal claims EXACTLY one error (same target path, the
     * site token and the needle in the message); an error no declaration claims fails as it always did, and a declaration
     * no error fires for fails as HEALED — the fork emits the file now; un-declare it (and re-pin the generation bar's
     * count) in the same change. A group with no declarations keeps the pre-seat law verbatim: any error fails.
     */
    static Map<String, String> admitDeclaredRefusals(String group, GenerationRun run) {
        List<DeclaredRefusal> declared = declaredRefusals(group);
        List<String> undeclared = new ArrayList<>();
        Map<DeclaredRefusal, Integer> fired = new LinkedHashMap<>();
        for (GenerationException ge : run.errors()) {
            String message = ge.getMessage() == null ? "" : ge.getMessage();
            DeclaredRefusal claim = null;
            for (DeclaredRefusal d : declared) {
                if (d.targetPath().equals(ge.getTargetPath()) && message.contains("[" + d.site() + "]")
                        && message.contains(d.needle())) {
                    claim = d;
                    break;
                }
            }
            if (claim == null) {
                undeclared.add((ge.getTargetPath() == null ? "<no target path>" : ge.getTargetPath()) + ": " + message);
            } else {
                fired.merge(claim, 1, Integer::sum);
            }
        }
        if (!undeclared.isEmpty()) {
            StringBuilder sb = new StringBuilder(group + ": " + undeclared.size()
                    + " generation error(s) on hold-out input" + (declared.isEmpty() ? "" : " NOT declared in DECLARED_REFUSALS") + ":\n");
            for (String msg : undeclared) {
                sb.append("  - ").append(msg).append('\n');
            }
            Assertions.fail(sb.toString());
        }
        List<String> healed = declared.stream().filter(d -> !fired.containsKey(d)).map(DeclaredRefusal::targetPath).toList();
        Assertions.assertTrue(healed.isEmpty(), group + ": declared refusal(s) no longer fire — the fork EMITS the file now;"
                + " un-declare (DECLARED_REFUSALS) and re-pin the generation bar's count in the same change: " + healed);
        List<String> doubled = fired.entrySet().stream().filter(e -> e.getValue() > 1)
                .map(e -> e.getKey().targetPath() + " x" + e.getValue()).toList();
        Assertions.assertTrue(doubled.isEmpty(), group + ": a declared refusal claimed more than one error: " + doubled);
        return run.output();
    }

    /**
     * The file-list form of {@link #generateAllKinds} — returns generation errors
     * instead of failing, so a classification bar ({@code WiderPoolCompileGateTest},
     * PR #412) can pin the expected-error population per fixture.
     */
    static GenerationRun generateAllKindsFromFiles(List<Path> groupFiles,
            List<Path> builtinFiles) throws IOException {
        List<RModel> models = new ArrayList<>();
        for (Path p : builtinFiles) {
            models.add(AstBuilder.buildFromFile(p));
        }
        Set<RModel> groupModels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Path p : groupFiles) {
            RModel m = AstBuilder.buildFromFile(p);
            if (m.version().isEmpty()) {
                // The version-stamp law — see the class Javadoc.
                m.setVersion(UPSTREAM_VERSION_DEFAULT);
            }
            groupModels.add(m);
            models.add(m);
        }
        return generateAllKindsFromModels(models, groupModels);
    }

    /**
     * The model-list core of {@link #generateAllKindsFromFiles} — extracted at PR #414
     * so the leg-C port harness ({@code UpstreamPortHarness}) can drive the SAME
     * full-battery pipeline from in-memory snippet models (parsed via
     * {@code AstBuilder.buildFromString}) instead of on-disk fixture files.
     * {@code models} = builtins + group models in load order; {@code groupModels} =
     * the identity-set of models that should EMIT (everything else is resolution-only).
     */
    static GenerationRun generateAllKindsFromModels(List<RModel> models,
            Set<RModel> groupModels) {
        return generateAllKindsFromModels(models, groupModels::contains);
    }

    /**
     * The predicate form of {@link #generateAllKindsFromModels(List, Set)} —
     * added at PR #423 (leg-C slice 2) so the filtered-namespace port
     * ({@code UpstreamModelMetaFilteredNamespacePortTest}) can drive the SAME
     * pipeline through a namespace-based emission filter, mirroring upstream's
     * {@code rosetta-config.yml} {@code generators.namespaces} channel (the fork
     * seat is {@code GeneratorModel}'s emission-filter predicate).
     */
    static GenerationRun generateAllKindsFromModels(List<RModel> models,
            java.util.function.Predicate<RModel> emissionFilter) {
        RLinkingResult corpus = RWorkspace.build(models);
        var gm = new GeneratorModel(corpus.workspace(), emissionFilter);

        var enumGen = new EnumGenerator(gm);
        var pojoGen = new ModelObjectGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var choiceGen = new ChoiceObjectGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL, pojoGen);
        var funcGen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var ruleGen = new RuleGenerator(gm, TYPE_TRANSLATOR, funcGen);
        var reportGen = new ReportGenerator(gm, TYPE_TRANSLATOR, funcGen);
        var labelProviderGen = new LabelProviderGenerator(
                gm, TYPE_TRANSLATOR, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        var metaFieldGen = new MetaFieldGenerator(gm, TYPE_TRANSLATOR);
        var onlyExistsGen = new OnlyExistsValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var cardinalityGen = new CardinalityValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var typeFormatGen = new TypeFormatValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var xmetaGen = new ModelMetaGenerator(gm, TYPE_TRANSLATOR);
        var deepPathGen = new DeepPathUtilGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var dataRuleGen = new DataRuleGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var packageInfoGen = new JavaPackageInfoGenerator(gm);

        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                genErrors.addAll(enumGen.generateClasses(model, version, output));
                genErrors.addAll(pojoGen.generateClasses(model, version, output));
                genErrors.addAll(choiceGen.generateClasses(model, version, output));
                genErrors.addAll(ruleGen.generateClasses(model, version, output));
                genErrors.addAll(reportGen.generateClasses(model, version, output));
                genErrors.addAll(labelProviderGen.generateClasses(model, version, output));
                genErrors.addAll(onlyExistsGen.generateClasses(model, version, output));
                genErrors.addAll(cardinalityGen.generateClasses(model, version, output));
                genErrors.addAll(typeFormatGen.generateClasses(model, version, output));
                genErrors.addAll(xmetaGen.generateClasses(model, version, output));
                genErrors.addAll(deepPathGen.generateClasses(model, version, output));
                genErrors.addAll(dataRuleGen.generateClasses(model, version, output));
            }
        }
        metaFieldGen.generate(output);
        genErrors.addAll(funcGen.generateWithErrors(output));
        packageInfoGen.generatePackageInfoClasses(output);

        return new GenerationRun(output, genErrors);
    }

    /**
     * Golden map: forward-slash relative path → raw content. Absent dir = empty set.
     * EVERY regular file is a golden (no extension filter — Seat-1 #410 OBS-1: a
     * non-{@code .java} oracle emission must surface as a loud {@code missing}
     * divergence, never be silently invisible; today all 210 goldens are .java).
     */
    private static Map<String, String> loadGoldens(Path groupGoldens) throws IOException {
        Map<String, String> golden = new TreeMap<>();
        if (!Files.isDirectory(groupGoldens)) {
            return golden;
        }
        try (var stream = Files.walk(groupGoldens)) {
            for (Path p : stream.filter(Files::isRegularFile).toList()) {
                String rel = groupGoldens.relativize(p).toString().replace('\\', '/');
                golden.put(rel, Files.readString(p));
            }
        }
        return golden;
    }

    private static String firstDiffExcerpt(String path, String golden, String generated) {
        String[] g = golden.split("\n", -1);
        String[] f = generated.split("\n", -1);
        int n = Math.min(g.length, f.length);
        for (int i = 0; i < n; i++) {
            if (!g[i].equals(f[i])) {
                return "  first diff — " + path + " line " + (i + 1) + ":\n"
                        + "    golden   : " + g[i] + "\n"
                        + "    generated: " + f[i] + "\n";
            }
        }
        return "  first diff — " + path + ": line counts differ (golden " + g.length
                + " vs generated " + f.length + ")\n";
    }

    /** The D11 normalization: line endings only. */
    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    /** Opt-in divergence dump — both sides, flat {@code ~}-encoded names (the D11 shape). */
    private static void dumpDivergent(String group, String relPath, String golden, String generated) {
        if (DUMP_DIR.isEmpty()) return;
        try {
            Path dir = Path.of(DUMP_DIR).resolve(group);
            Files.createDirectories(dir);
            String flat = relPath.replace('/', '~');
            Files.writeString(dir.resolve(flat + ".golden"), golden);
            Files.writeString(dir.resolve(flat + ".generated"), generated);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<Path> resolveBuiltinFiles() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
        return resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();
    }
}
