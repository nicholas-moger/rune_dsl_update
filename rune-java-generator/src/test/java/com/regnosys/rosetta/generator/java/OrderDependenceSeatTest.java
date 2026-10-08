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
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.validation.ValidationIssueCode;

/**
 * v3.2 seat 4 — the chaos census's ORDER-DEPENDENT PAIR: family F7 "report-type-resolved-by-workspace-first-match"
 * (11 declared D11 rows, the s07 {@code C7AUTHC7RegReportFunction} in every placement variant but the first
 * namespace's, which was right by accident) and family F10 "qualifiable-root-any-model" (10 rows, the s22
 * {@code C22TermsMeta} of every losing namespace). Both are answers that depended on WHICH model came first;
 * the seat pinned the released 9.83.0 plugin's answers FIRST (ten oracle groups in the hold-out bar, commit 2)
 * and then made the fork give them by scope and by ONE order rule.
 *
 * <p>THE MEASURED MECHANISMS (the seat census, both routes byte-identical):
 * <ul>
 *   <li><b>F7</b> — {@code RReport.withType} / {@code withSource} were bare strings the linker never resolved;
 *       {@code RFunction.fromReport} typed the synthetic output by NAME and the generator's workspace-wide
 *       first match handed every report function (and its label provider, which walks the same output
 *       type) the FIRST namespace's report type and rules — emissions that did not compile. The fix: the
 *       two references are linker-resolved ids ({@code GlobalResolutionPass.resolveReport} — the own
 *       namespace, the imports, a qualified name; {@code TYPE_NOT_FOUND} / {@code EXTERNAL_SOURCE_NOT_FOUND}),
 *       the synthetic output is typed by the id and attached to the report's workspace, the rule source is
 *       read by id, and an id-less reference is REFUSED at
 *       {@link SilentDegradation.Site#REPORT_REFERENCE_UNRESOLVED} at BOTH report seats from ONE
 *       declaration ({@code RuleReferenceTraversal.requireResolvedReportType}, LAW 69).</li>
 *   <li><b>F10</b> — {@code ModelMetaGenerator.isQualifiableRoot} accepted ANY model's configuration where
 *       the released plugin takes the FIRST resolved configuration per kind over its index
 *       ({@code RosettaConfigExtension.findRosettaQualifiableConfiguration}, {@code getFirst} — an order
 *       that is a function of the resource PATH, exposed by the seat's three order probes: the same three
 *       files, byte-identical, won differently under two directory names; a hash over resource URIs is
 *       consistent with it and inferred, not measured). The fix: ONE declaration,
 *       {@code RQualifiableConfig.firstRoot(modelsInLoadOrder, kind)}, read by the meta generator over the
 *       WORKSPACE's models in load order (dependency models included — upstream's {@code isProjectLocal}
 *       admits every candidate under a non-platform URI, in the post-9.83 source and the released 9.83.0
 *       jar alike, {@code javap}-verified at round 2; what stays unmeasured is the released plugin's runtime
 *       URI scheme, banked) and by the validator's port of
 *       upstream's "Input type does not match
 *       qualification root type." warning; where a golden fixes a winner the loader replays it by pin
 *       (the #413 law — {@code HoldOutByteCompareTest.GOLDEN_QUALIFIABLE_ROOT_ORDER_PINS}).</li>
 * </ul>
 *
 * <p>The fixtures below mirror the seat's ORACLE groups ({@code holdout/report-withtype-shadow},
 * {@code report-withtype-qualified}, {@code report-rules-split}, {@code report-withsource-shadow},
 * {@code qualify-first-wins}, {@code qualify-event-and-product}, {@code qualify-cross-namespace-input}, and
 * round 1's {@code report-withtype-rule-shadow} —
 * pinned from the released 9.83.0 plugin, byte-locked whole by {@code HoldOutByteCompareTest}) under
 * {@code census.seat4*} namespaces; their expected strings are the golden forms with the namespace
 * substituted, so each assertion is a mutation witness, not a guess. The unresolvable references (a5, a6)
 * are refusal witnesses — the released plugin does not generate on a linking error at all. The chaos
 * carriers are byte-locked whole over every placement variant on the DEFAULT route (the corpus locks: the
 * twelve s07 report functions incl. a1o1, their twelve label providers, the sixteen s22 metas — the IR route's
 * agreement on the cell is the chain's ROUTE CONTENT COMPARE, not these locks), the counter is proven able to
 * fire at both seats (a5 / a6 — LAW 81), the default route's populations are pinned (control2a) and LAW 77
 * holds on the IR route (control2b).
 */
class OrderDependenceSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    private static final Path CHAOS_ROOT = com.regnosys.rosetta.testutil.ChaosCell.root();
    private static final Path CHAOS_GOLDEN = CHAOS_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean chaosAvailable() {
        return Files.isDirectory(CHAOS_GOLDEN)
                && Files.isDirectory(CHAOS_ROOT.resolve("rosetta-source/src/main/rosetta"));
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean builtinsAndIrProviderAvailable() {
        return builtinsAvailable() && irProviderOnClasspath();
    }

    /** The s07 placement families: twelve report functions, a1o1 the workspace's first namespace. */
    private static final List<String> S07_FAMILIES = List.of(
            "a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half", "a3hub", "a3third", "base");
    /**
     * The s22 placement families carrying a {@code C22TermsMeta} golden: sixteen (no a1o4 in s22; the
     * a4none / a4snap / a5* variants declare NO qualifier and never diverged — locked all the same, so the
     * winner rule cannot register a qualifier where the golden has none).
     */
    private static final List<String> S22_FAMILIES = List.of(
            "a1o1", "a1o2", "a1o3", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half", "a3hub", "a3third", "a4none", "a4snap", "a5crlf", "a5mixed", "a5uni", "base");

    // =========================================================================
    // F7 fixtures — the four report shapes of the oracle groups, and the two unresolvable references
    // =========================================================================

    /** a1: the oracle group report-withtype-shadow — the OWN namespace's report type shadows the imported one. */
    private static final String RWS_A = """
            namespace census.seat4rws.a
            version "0.0.0"

            body Authority RwsAUTH <"The authority.">
            corpus Regulation "Rws Regulation" RwsReg <"The corpus.">
            segment rwsarticle

            type RwsTrade: <"The report-from type.">
                utid string (1..1)
                    [label as "The Trade UTI"]
                notional number (0..1)

            eligibility rule RwsEligible from RwsTrade: <"Report when-gate.">
                notional exists
            reporting rule RwsUtid from RwsTrade: <"Reported field.">
                extract utid
                    as "UTI"
            reporting rule RwsNotional from RwsTrade: <"Reported numeric with default.">
                extract notional default 0
                    as "Notional"

            type RwsReport: <"Report output type with rule references.">
                utiField string (1..1)
                    [ruleReference RwsUtid]
                notionalField number (1..1)
                    [ruleReference RwsNotional]
            """;

    private static final String RWS_B = """
            namespace census.seat4rws.b
            version "0.0.0"

            import census.seat4rws.a.*

            body Authority RwsAUTH <"The authority.">
            corpus Regulation "Rws Regulation" RwsReg <"The corpus.">
            segment rwsarticle

            type RwsTrade: <"The report-from type.">
                utid string (1..1)
                    [label as "The Trade UTI"]
                notional number (0..1)

            eligibility rule RwsEligible from RwsTrade: <"Report when-gate.">
                notional exists
            reporting rule RwsUtid from RwsTrade: <"Reported field.">
                extract utid
                    as "UTI"
            reporting rule RwsNotional from RwsTrade: <"Reported numeric with default.">
                extract notional default 0
                    as "Notional"

            type RwsReport: <"Report output type with rule references - the OWN namespace's, shadowing the imported one.">
                utiField string (1..1)
                    [ruleReference RwsUtid]
                notionalField number (1..1)
                    [ruleReference RwsNotional]

            corpus Standard "Rws Standard" RwsStd <"The standard.">
            rule source RwsRS
            {
            }
            report RwsAUTH RwsReg in T+1
                from RwsTrade
                when RwsEligible
                using standard RwsStd
                with type RwsReport
                with source RwsRS
            """;

    /** a2: the oracle group report-withtype-qualified — a QUALIFIED with-type into another namespace, past same-named decoys. */
    private static final String RWQ_A = """
            namespace census.seat4rwq.a
            version "0.0.0"

            body Authority RwqAUTH <"The authority.">
            corpus Regulation "Rwq Regulation" RwqReg <"The corpus.">
            segment rwqarticle

            type RwqTrade: <"The report-from type.">
                utid string (1..1)
                    [label as "The Trade UTI"]
                notional number (0..1)

            eligibility rule RwqEligible from RwqTrade: <"Report when-gate.">
                notional exists
            reporting rule RwqUtid from RwqTrade: <"Reported field.">
                extract utid
                    as "UTI"
            reporting rule RwqNotional from RwqTrade: <"Reported numeric with default.">
                extract notional default 0
                    as "Notional"

            type RwqReport: <"Report output type with rule references.">
                utiField string (1..1)
                    [ruleReference RwqUtid]
                notionalField number (1..1)
                    [ruleReference RwqNotional]
            """;

    private static final String RWQ_B = """
            namespace census.seat4rwq.b
            version "0.0.0"

            import census.seat4rwq.a.*

            body Authority RwqAUTH <"The authority.">
            corpus Regulation "Rwq Regulation" RwqReg <"The corpus.">
            segment rwqarticle

            type RwqTrade: <"A same-named DECOY from-type in the report's own namespace - the report names a's by qualified name.">
                utid string (1..1)
                notional number (0..1)

            reporting rule RwqUtid from RwqTrade: <"A same-named decoy rule.">
                extract utid
                    as "UTI"

            type RwqReport: <"A same-named DECOY report type in the report's own namespace.">
                utiField string (1..1)
                    [ruleReference RwqUtid]

            corpus Standard "Rwq Standard" RwqStd <"The standard.">
            rule source RwqRS
            {
            }
            report RwqAUTH RwqReg in T+1
                from census.seat4rwq.a.RwqTrade
                when census.seat4rwq.a.RwqEligible
                using standard RwqStd
                with type census.seat4rwq.a.RwqReport
                with source RwqRS
            """;

    /** a3: the oracle group report-rules-split (the chaos a3third shape) — the decoy namespace FIRST, the report's rules in a sibling part. */
    private static final String RSP_A = """
            namespace census.seat4rsp.a
            version "0.0.0"

            body Authority RspAUTH <"The authority.">
            corpus Regulation "Rsp Regulation" RspReg <"The corpus.">
            segment rwsarticle

            type RspTrade: <"The report-from type.">
                utid string (1..1)
                    [label as "The Trade UTI"]
                notional number (0..1)

            eligibility rule RspEligible from RspTrade: <"Report when-gate.">
                notional exists
            reporting rule RspUtid from RspTrade: <"Reported field.">
                extract utid
                    as "UTI"
            reporting rule RspNotional from RspTrade: <"Reported numeric with default.">
                extract notional default 0
                    as "Notional"

            type RspReport: <"Report output type with rule references.">
                utiField string (1..1)
                    [ruleReference RspUtid]
                notionalField number (1..1)
                    [ruleReference RspNotional]
            """;

    private static final String RSP_P2 = """
            namespace census.seat4rsp.p2
            version "0.0.0"

            import census.seat4rsp.p3.*

            type RspTrade: <"The report-from type.">
                utid string (1..1)
                    [label as "The Trade UTI"]
                notional number (0..1)

            eligibility rule RspEligible from RspTrade: <"Report when-gate.">
                notional exists
            reporting rule RspUtid from RspTrade: <"Reported field.">
                extract utid
                    as "UTI"
            reporting rule RspNotional from RspTrade: <"Reported numeric with default.">
                extract notional default 0
                    as "Notional"
            """;

    private static final String RSP_P3 = """
            namespace census.seat4rsp.p3
            version "0.0.0"

            import census.seat4rsp.p2.*

            body Authority RspAUTH <"The authority.">
            corpus Regulation "Rsp Regulation" RspReg <"The corpus.">
            segment rsparticle

            type RspReport: <"Report output type with rule references into the sibling part.">
                utiField string (1..1)
                    [ruleReference RspUtid]
                notionalField number (1..1)
                    [ruleReference RspNotional]

            corpus Standard "Rsp Standard" RspStd <"The standard.">
            rule source RspRS
            {
            }
            report RspAUTH RspReg in T+1
                from RspTrade
                when RspEligible
                using standard RspStd
                with type RspReport
                with source RspRS
            """;

    /** a4: the oracle group report-withsource-shadow — the notional rule comes from the OWN rule source, over the own report type. */
    private static final String RSR_A = """
            namespace census.seat4rsr.a
            version "0.0.0"

            type RsrTrade: <"The report-from type.">
                utid string (1..1)
                notional number (0..1)

            reporting rule RsrUtid from RsrTrade: <"Reported field.">
                extract utid
                    as "UTI"
            reporting rule RsrNotional from RsrTrade: <"Reported numeric with default.">
                extract notional default 0
                    as "Notional"

            type RsrReport: <"Report output type; the notional field has NO inline rule reference.">
                utiField string (1..1)
                    [ruleReference RsrUtid]
                notionalField number (1..1)

            rule source RsrRS
            {
            }
            """;

    private static final String RSR_B = """
            namespace census.seat4rsr.b
            version "0.0.0"

            import census.seat4rsr.a.*

            body Authority RsrAUTH <"The authority.">
            corpus Regulation "Rsr Regulation" RsrReg <"The corpus.">
            segment rsrarticle

            type RsrTrade: <"The report-from type.">
                utid string (1..1)
                notional number (0..1)

            eligibility rule RsrEligible from RsrTrade: <"Report when-gate.">
                notional exists
            reporting rule RsrUtid from RsrTrade: <"Reported field.">
                extract utid
                    as "UTI"
            reporting rule RsrNotional from RsrTrade: <"Reported numeric with default.">
                extract notional default 0
                    as "Notional"

            type RsrReport: <"Report output type; the notional field's rule comes from the OWN rule source.">
                utiField string (1..1)
                    [ruleReference RsrUtid]
                notionalField number (1..1)

            corpus Standard "Rsr Standard" RsrStd <"The standard.">
            rule source RsrRS
            {
                RsrReport:
                    + notionalField
                        [ruleReference RsrNotional]
            }
            report RsrAUTH RsrReg in T+1
                from RsrTrade
                when RsrEligible
                using standard RsrStd
                with type RsrReport
                with source RsrRS
            """;

    /** a5: an UNRESOLVABLE with-type — the linker's TYPE_NOT_FOUND, the refusal at both report seats. */
    private static final String UNT = """
            namespace census.seat4unt.a
            version "0.0.0"

            body Authority UntAUTH <"The authority.">
            corpus Regulation "Unt Regulation" UntReg <"The corpus.">
            segment untarticle

            type UntTrade: <"The report-from type.">
                utid string (1..1)
                notional number (0..1)

            eligibility rule UntEligible from UntTrade: <"Report when-gate.">
                notional exists
            reporting rule UntUtid from UntTrade: <"Reported field.">
                extract utid
                    as "UTI"
            reporting rule UntNotional from UntTrade: <"Reported numeric with default.">
                extract notional default 0
                    as "Notional"

            type UntReport: <"Report output type with rule references.">
                utiField string (1..1)
                    [ruleReference UntUtid]
                notionalField number (1..1)
                    [ruleReference UntNotional]

            corpus Standard "Unt Standard" UntStd <"The standard.">
            rule source UntRS
            {
            }
            report UntAUTH UntReg in T+1
                from UntTrade
                when UntEligible
                using standard UntStd
                with type NoSuchReport
                with source UntRS
            """;

    /**
     * a7 (oracle group report-withtype-rule-shadow, round 1): a reporting rule that SHARES the report type's
     * name, declared BEFORE the type — the released plugin resolves {@code with type} by KIND (15 goldens).
     */
    private static final String RSH = """
            namespace census.seat4rsh.a
            version "0.0.0"

            body Authority RshAUTH <"The authority.">
            corpus Regulation "Rsh Regulation" RshReg <"The corpus.">
            segment rsharticle

            type RshTrade: <"The report-from type.">
                utid string (1..1)
                    [label as "The Trade UTI"]
                notional number (0..1)

            eligibility rule RshEligible from RshTrade: <"Report when-gate.">
                notional exists
            reporting rule RshReport from RshTrade: <"A reporting rule that SHARES the report type's name, declared BEFORE the type.">
                extract utid
                    as "UTI"
            reporting rule RshNotional from RshTrade: <"Reported numeric with default.">
                extract notional default 0
                    as "Notional"

            type RshReport: <"Report output type whose name a reporting rule in the same namespace shares.">
                utiField string (1..1)
                    [ruleReference RshReport]
                notionalField number (1..1)
                    [ruleReference RshNotional]

            corpus Standard "Rsh Standard" RshStd <"The standard.">
            rule source RshRS
            {
            }
            report RshAUTH RshReg in T+1
                from RshTrade
                when RshEligible
                using standard RshStd
                with type RshReport
                with source RshRS
            """;

    /**
     * b5 (round 1): a qualifier in a workspace declaring NO {@code isProduct} / {@code isEvent} root at all —
     * warned about, because upstream's {@code isRootEventOrProduct} compares the input against two null roots.
     */
    private static final String QNR = """
            namespace census.seat4qnr.a
            version "0.0.0"

            type QnrTerms: <"A would-be root: no isProduct / isEvent configuration anywhere in the workspace.">
                kind string (0..1)

            func Qualify_QnrA1: <"A qualifier with no root declared.">
                [qualification Product]
                inputs:
                    terms QnrTerms (1..1)
                output:
                    is_product boolean (1..1)
                set is_product:
                    terms -> kind = "vanilla"
            """;

    /** a6: an UNRESOLVABLE with-source — the linker's EXTERNAL_SOURCE_NOT_FOUND, the refusal at both report seats. */
    private static final String UNS = """
            namespace census.seat4uns.a
            version "0.0.0"

            body Authority UnsAUTH <"The authority.">
            corpus Regulation "Uns Regulation" UnsReg <"The corpus.">
            segment unsarticle

            type UnsTrade: <"The report-from type.">
                utid string (1..1)
                notional number (0..1)

            eligibility rule UnsEligible from UnsTrade: <"Report when-gate.">
                notional exists
            reporting rule UnsUtid from UnsTrade: <"Reported field.">
                extract utid
                    as "UTI"
            reporting rule UnsNotional from UnsTrade: <"Reported numeric with default.">
                extract notional default 0
                    as "Notional"

            type UnsReport: <"Report output type with rule references.">
                utiField string (1..1)
                    [ruleReference UnsUtid]
                notionalField number (1..1)
                    [ruleReference UnsNotional]

            corpus Standard "Uns Standard" UnsStd <"The standard.">
            rule source UnsRS
            {
            }
            report UnsAUTH UnsReg in T+1
                from UnsTrade
                when UnsEligible
                using standard UnsStd
                with type UnsReport
                with source NoSuchRS
            """;

    // =========================================================================
    // F10 fixtures — the three qualifiable-root shapes of the oracle groups
    // =========================================================================

    /** b1 / b4: the oracle group qualify-first-wins — two same-named roots, ONE winner per kind, in load order. */
    private static final String QFW_A = """
            namespace census.seat4qfw.a
            version "0.0.0"
            isProduct root QfwTerms;

            type QfwTerms: <"The qualifiable root type - the first in load order.">
                kind string (0..1)
                notional number (0..1)

            func Qualify_QfwA1: <"Product qualification against the declared root.">
                [qualification Product]
                inputs:
                    terms QfwTerms (1..1)
                output:
                    is_product boolean (1..1)
                set is_product:
                    terms -> kind = "vanilla"

            func Qualify_QfwA2: <"Product qualification against the declared root.">
                [qualification Product]
                inputs:
                    terms QfwTerms (1..1)
                output:
                    is_product boolean (1..1)
                set is_product:
                    terms -> notional exists
            """;

    private static final String QFW_B = """
            namespace census.seat4qfw.b
            version "0.0.0"
            isProduct root QfwTerms;

            type QfwTerms: <"The same-named root type in the LATER namespace - its configuration loses the first-wins race.">
                kind string (0..1)
                notional number (0..1)

            func Qualify_QfwB1: <"Product qualification against the declared root.">
                [qualification Product]
                inputs:
                    terms QfwTerms (1..1)
                output:
                    is_product boolean (1..1)
                set is_product:
                    terms -> kind = "exotic"
            """;

    /** b2: the oracle group qualify-event-and-product — first-wins is PER KIND. */
    private static final String QEP_A = """
            namespace census.seat4qep.a
            version "0.0.0"
            isEvent root QepEvent;

            type QepEvent: <"The event root.">
                kind string (0..1)

            func Qualify_QepE: <"Event qualification.">
                [qualification BusinessEvent]
                inputs:
                    event QepEvent (1..1)
                output:
                    is_event boolean (1..1)
                set is_event:
                    event -> kind = "trade"
            """;

    private static final String QEP_B = """
            namespace census.seat4qep.b
            version "0.0.0"
            isProduct root QepProduct;

            type QepProduct: <"The product root, declared in the LATER namespace - first-wins is PER KIND.">
                kind string (0..1)

            func Qualify_QepP: <"Product qualification against the declared root.">
                [qualification Product]
                inputs:
                    terms QepProduct (1..1)
                output:
                    is_product boolean (1..1)
                set is_product:
                    terms -> kind = "vanilla"
            """;

    /** b3: the oracle group qualify-cross-namespace-input — a qualifier in ANOTHER namespace registers on the root's meta. */
    private static final String QCN_A = """
            namespace census.seat4qcn.a
            version "0.0.0"
            isProduct root QcnTerms;

            type QcnTerms: <"The root; a qualifier in namespace b takes it as input.">
                kind string (0..1)
                notional number (0..1)

            func Qualify_QcnA: <"Product qualification against the declared root.">
                [qualification Product]
                inputs:
                    terms QcnTerms (1..1)
                output:
                    is_product boolean (1..1)
                set is_product:
                    terms -> kind = "vanilla"
            """;

    private static final String QCN_B = """
            namespace census.seat4qcn.b
            version "0.0.0"

            import census.seat4qcn.a.*

            func Qualify_QcnB: <"Product qualification against the declared root.">
                [qualification Product]
                inputs:
                    terms QcnTerms (1..1)
                output:
                    is_product boolean (1..1)
                set is_product:
                    terms -> notional exists
            """;

    // =========================================================================
    // F7 — the report's with-type / with-source resolved by SCOPE, never by a workspace-wide first match
    // =========================================================================

    /**
     * a1 (oracle group report-withtype-shadow): the report's own namespace declares the same report type,
     * from-type and rules the imported first namespace does; the report function is typed and wired by
     * the OWN ones — the golden's imports, and NOT ONE mention of the first namespace. Before the seat the
     * workspace-wide first match returned {@code census.seat4rws.a}'s report type and rules (the chaos s07
     * shape: the a1o1 rows were right by accident, every other variant imported a1o1).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_ownNamespaceReportType_shadowsTheImportedFirstNamespace() throws IOException {
        Render r = renderRws();
        String code = pick(r, "census/seat4rws/b/reports/RwsAUTHRwsRegReportFunction.java");
        assertContains(code, "import census.seat4rws.b.RwsReport;\nimport census.seat4rws.b.RwsTrade;\nimport census.seat4rws.b.labels.RwsAUTHRwsRegLabelProvider;\n");
        assertContains(code, "@RosettaReport(namespace=\"census.seat4rws.b\", body=\"RwsAUTH\", corpusList={\"RwsReg\"})");
        assertContains(code, "public abstract class RwsAUTHRwsRegReportFunction implements ReportFunction<RwsTrade, RwsReport> {");
        // the rules are the own package's: injected UNQUALIFIED (no import), both wired
        assertContains(code, "\t@Inject protected RwsNotionalRule rwsNotionalRule;\n\t@Inject protected RwsUtidRule rwsUtidRule;\n");
        assertContains(code, "\t\t\toutput\n\t\t\t\t.setUtiField(rwsUtidRule.evaluate(input));\n\t\t\t\n\t\t\toutput\n\t\t\t\t.setNotionalField(rwsNotionalRule.evaluate(input));\n");
        assertAbsent(code, "census.seat4rws.a");
        String labels = pick(r, "census/seat4rws/b/labels/RwsAUTHRwsRegLabelProvider.java");
        assertContains(labels, "\t\tstartNode.addLabel(Arrays.asList(\"utiField\"), \"UTI\");\n\t\tstartNode.addLabel(Arrays.asList(\"notionalField\"), \"Notional\");\n");
        assertTrue(r.errors().isEmpty(), r.errors().toString());
        assertEquals(0, r.counts().getOrDefault(SilentDegradation.Site.REPORT_REFERENCE_UNRESOLVED, 0));
    }

    /**
     * a2 (oracle group report-withtype-qualified): the report names ANOTHER namespace's report type,
     * from-type and when-rule by QUALIFIED name past same-named decoys in its own namespace; the report
     * function imports the qualified namespace's types AND its rules (they live in another package now),
     * while its label provider stays its own. The released plugin refused the first cut of this group — a
     * report type's rules must take the report's from-type — so the fixture's {@code from} and {@code when}
     * are qualified too, exactly as the golden's model.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_qualifiedWithType_resolvesPastTheOwnNamespacesDecoys() throws IOException {
        Render r = renderRwq();
        String code = pick(r, "census/seat4rwq/b/reports/RwqAUTHRwqRegReportFunction.java");
        assertContains(code, "import census.seat4rwq.a.RwqReport;\nimport census.seat4rwq.a.RwqTrade;\nimport census.seat4rwq.a.reports.RwqNotionalRule;\nimport census.seat4rwq.a.reports.RwqUtidRule;\nimport census.seat4rwq.b.labels.RwqAUTHRwqRegLabelProvider;\n");
        assertContains(code, "@RosettaReport(namespace=\"census.seat4rwq.b\", body=\"RwqAUTH\", corpusList={\"RwqReg\"})");
        assertContains(code, "\t@Inject protected RwqNotionalRule rwqNotionalRule;\n\t@Inject protected RwqUtidRule rwqUtidRule;\n");
        assertContains(code, "\t\t\t\t.setNotionalField(rwqNotionalRule.evaluate(input));\n");
        assertAbsent(code, "import census.seat4rwq.b.RwqReport;");
        assertAbsent(code, "import census.seat4rwq.b.RwqTrade;");
        assertTrue(r.errors().isEmpty(), r.errors().toString());
    }

    /**
     * a3 (oracle group report-rules-split — the chaos a3third placement): the from-type and the rules sit
     * in a sibling part ({@code p2}), the report type and the report in another ({@code p3}), each
     * importing the other; a DECOY namespace with the same names sorts FIRST. The report function imports
     * p2's from-type and rules and p3's report type — nothing from the decoy.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_rulesSplitAcrossSiblingParts_pastAFirstSortedDecoy() throws IOException {
        Render r = renderRsp();
        String code = pick(r, "census/seat4rsp/p3/reports/RspAUTHRspRegReportFunction.java");
        assertContains(code, "import census.seat4rsp.p2.RspTrade;\nimport census.seat4rsp.p2.reports.RspNotionalRule;\nimport census.seat4rsp.p2.reports.RspUtidRule;\nimport census.seat4rsp.p3.RspReport;\nimport census.seat4rsp.p3.labels.RspAUTHRspRegLabelProvider;\n");
        assertContains(code, "public abstract class RspAUTHRspRegReportFunction implements ReportFunction<RspTrade, RspReport> {");
        // the BODY too (lane C's run-1 catch): a detached synthetic output renders a TODO comment here, silently
        assertContains(code, "\t@Inject protected RspNotionalRule rspNotionalRule;\n\t@Inject protected RspUtidRule rspUtidRule;\n");
        assertContains(code, "\t\t\toutput\n\t\t\t\t.setUtiField(rspUtidRule.evaluate(input));\n\t\t\t\n\t\t\toutput\n\t\t\t\t.setNotionalField(rspNotionalRule.evaluate(input));\n");
        assertAbsent(code, "TODO");
        assertAbsent(code, "census.seat4rsp.a");
        assertTrue(r.errors().isEmpty(), r.errors().toString());
    }

    /**
     * a4 (oracle group report-withsource-shadow): the notional field carries NO inline rule reference — its
     * rule comes from the OWN namespace's rule source over the OWN report type. Before the seat the report
     * type came from the first namespace (whose notional field has no rule anywhere), so the traversal
     * found nothing for it: the report function lacked the notional setter and the label provider the
     * "Notional" label (the two pinned mismatches of this group). Both are the golden's now.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_ownRuleSource_overTheOwnReportType_suppliesTheNotionalRule() throws IOException {
        Render r = renderRsr();
        String code = pick(r, "census/seat4rsr/b/reports/RsrAUTHRsrRegReportFunction.java");
        assertContains(code, "import census.seat4rsr.b.RsrReport;\nimport census.seat4rsr.b.RsrTrade;\nimport census.seat4rsr.b.labels.RsrAUTHRsrRegLabelProvider;\n");
        assertContains(code, "\t@Inject protected RsrNotionalRule rsrNotionalRule;\n\t@Inject protected RsrUtidRule rsrUtidRule;\n");
        assertContains(code, "\t\t\toutput\n\t\t\t\t.setUtiField(rsrUtidRule.evaluate(input));\n\t\t\t\n\t\t\toutput\n\t\t\t\t.setNotionalField(rsrNotionalRule.evaluate(input));\n");
        assertAbsent(code, "census.seat4rsr.a");
        String labels = pick(r, "census/seat4rsr/b/labels/RsrAUTHRsrRegLabelProvider.java");
        assertContains(labels, "\t\tstartNode.addLabel(Arrays.asList(\"utiField\"), \"UTI\");\n\t\tstartNode.addLabel(Arrays.asList(\"notionalField\"), \"Notional\");\n");
        assertTrue(r.errors().isEmpty(), r.errors().toString());
    }

    /**
     * a5 (LAW 81 — the counter proven able to fire, the with-type arm): a {@code with type} that resolves to
     * nothing is the LINKER's error ({@code TYPE_NOT_FOUND}, anchored on the reference's own range), and
     * the generator REFUSES at BOTH report seats — the report function and its label provider, by TYPE and
     * SITE, each with its own target path — and emits NEITHER file; the namespace's rules and metas still
     * emit. Before the seat the report seat resolved the name workspace-wide (nothing, then an
     * {@code Object}-typed output) and the label provider walked a null start node into an EMPTY provider,
     * both in silence.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_unresolvableWithType_isTheLinkersError_andIsRefusedAtBothReportSeats() throws IOException {
        Render r = renderModels(List.<String[]>of(new String[] {"seat4unt.rosetta", UNT}), "census.seat4unt", false);
        List<LinkingDiagnostic> notFound = r.linking(DiagnosticCategory.TYPE_NOT_FOUND);
        assertEquals(1, notFound.size(), "exactly the report's with-type is unresolved: " + r.diagnostics());
        assertEquals("NoSuchReport", notFound.get(0).unresolvedName());
        assertEquals(Severity.ERROR, notFound.get(0).severity());
        assertEquals("seat4unt.rosetta", notFound.get(0).range().file(), "anchored on the reference's own file");
        // anchored on the REFERENCE's own token range (AstBuilder's withType range), not the report's
        assertEquals(lineOf(UNT, "with type NoSuchReport"), notFound.get(0).range().startLine(), "anchored on the with-type line: " + notFound.get(0).range());
        List<SilentDegradation.Refusal> refusals = r.refusalsAt(SilentDegradation.Site.REPORT_REFERENCE_UNRESOLVED);
        assertEquals(2, refusals.size(), "the report function AND its label provider refuse, once each: " + r.errors());
        assertTrue(refusals.stream().anyMatch(e -> "census/seat4unt/a/reports/UntAUTHUntRegReportFunction.java".equals(e.getTargetPath())
                && e.getMessage().contains("with type NoSuchReport")), refusals.toString());
        assertTrue(refusals.stream().anyMatch(e -> "census/seat4unt/a/labels/UntAUTHUntRegLabelProvider.java".equals(e.getTargetPath())
                && e.getMessage().contains("with type NoSuchReport")), refusals.toString());
        assertEquals(2, r.errors().size(), "no other error: " + r.errors());
        assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("UntAUTHUntRegReportFunction.java")
                || k.endsWith("UntAUTHUntRegLabelProvider.java")), "a refused file must not be emitted: " + r.output().keySet());
        pick(r, "census/seat4unt/a/reports/UntUtidRule.java");
        pick(r, "census/seat4unt/a/meta/UntReportMeta.java");
        assertEquals(2, r.counts().getOrDefault(SilentDegradation.Site.REPORT_REFERENCE_UNRESOLVED, 0), "the register counts both refusals");
    }

    /**
     * a6 (LAW 81 — the with-source arm): a {@code with source} that resolves to nothing is the linker's
     * {@code EXTERNAL_SOURCE_NOT_FOUND}, and the rule-source read ({@code RuleReferenceTraversal.resolveRuleSource},
     * consulted by both report seats) REFUSES at the same site with each seat's own path — never a
     * simple-name search across the workspace (the retired walk, corpus-inert only because every chaos
     * rule source sits beside its report).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_unresolvableWithSource_isTheLinkersError_andIsRefusedAtBothReportSeats() throws IOException {
        Render r = renderModels(List.<String[]>of(new String[] {"seat4uns.rosetta", UNS}), "census.seat4uns", false);
        List<LinkingDiagnostic> notFound = r.linking(DiagnosticCategory.EXTERNAL_SOURCE_NOT_FOUND);
        assertEquals(1, notFound.size(), "exactly the report's with-source is unresolved: " + r.diagnostics());
        assertEquals("NoSuchRS", notFound.get(0).unresolvedName());
        assertEquals(lineOf(UNS, "with source NoSuchRS"), notFound.get(0).range().startLine(), "anchored on the with-source line: " + notFound.get(0).range());
        assertTrue(r.linking(DiagnosticCategory.TYPE_NOT_FOUND).isEmpty(), "the with-type resolved: " + r.diagnostics());
        List<SilentDegradation.Refusal> refusals = r.refusalsAt(SilentDegradation.Site.REPORT_REFERENCE_UNRESOLVED);
        assertEquals(2, refusals.size(), "the report function AND its label provider refuse, once each: " + r.errors());
        assertTrue(refusals.stream().anyMatch(e -> "census/seat4uns/a/reports/UnsAUTHUnsRegReportFunction.java".equals(e.getTargetPath())
                && e.getMessage().contains("with source NoSuchRS")), refusals.toString());
        assertTrue(refusals.stream().anyMatch(e -> "census/seat4uns/a/labels/UnsAUTHUnsRegLabelProvider.java".equals(e.getTargetPath())
                && e.getMessage().contains("with source NoSuchRS")), refusals.toString());
        assertEquals(2, r.errors().size(), "no other error: " + r.errors());
        assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("UnsAUTHUnsRegReportFunction.java")
                || k.endsWith("UnsAUTHUnsRegLabelProvider.java")), "a refused file must not be emitted: " + r.output().keySet());
        assertEquals(2, r.counts().getOrDefault(SilentDegradation.Site.REPORT_REFERENCE_UNRESOLVED, 0), "the register counts both refusals");
    }

    // =========================================================================
    // F10 — the qualifiable root: the FIRST configuration per kind in load order, and the losers warned
    // =========================================================================

    /**
     * a7 (oracle group report-withtype-rule-shadow — round 1, the code-quality review's MF-1): a reporting rule
     * that SHARES the report type's name, declared BEFORE the type. The released plugin resolves {@code with
     * type} as a {@code Data} cross-reference, so the rule is invisible to it (15 goldens, deterministic x2);
     * the fix's first cut resolved the name KIND-BLIND, took the rule, raised a false {@code TYPE_NOT_FOUND}
     * and refused BOTH report seats — a regression against the type-filtered search it replaced (measured
     * before the fix: {@code scratch/gate-prefix-rsh.log:344} — the battery 173/2F with the group staged; its
     * {@code .status} carries EXIT=1 alone). Now the kind-gated ladder
     * ({@code resolveQualifiedOrLocalOfKind}, the rule and synonym-source references' ladder); lane M is its
     * witness. The expected strings are the golden's with the namespace substituted.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a7_reportTypeSharingAReportingRulesName_resolvesByKind() throws IOException {
        Render r = renderRsh();
        assertTrue(r.linking(DiagnosticCategory.TYPE_NOT_FOUND).isEmpty(), "no false TYPE_NOT_FOUND: " + r.diagnostics());
        assertTrue(r.errors().isEmpty(), r.errors().toString());
        String code = pick(r, "census/seat4rsh/a/reports/RshAUTHRshRegReportFunction.java");
        assertContains(code, "import census.seat4rsh.a.RshReport;\nimport census.seat4rsh.a.RshTrade;\nimport census.seat4rsh.a.labels.RshAUTHRshRegLabelProvider;\n");
        assertContains(code, "public abstract class RshAUTHRshRegReportFunction implements ReportFunction<RshTrade, RshReport> {");
        assertContains(code, "\t@Inject protected RshNotionalRule rshNotionalRule;\n\t@Inject protected RshReportRule rshReportRule;\n");
        assertContains(code, "\t\t\toutput\n\t\t\t\t.setUtiField(rshReportRule.evaluate(input));\n\t\t\t\n\t\t\toutput\n\t\t\t\t.setNotionalField(rshNotionalRule.evaluate(input));\n");
        assertAbsent(code, "TODO");
        String labels = pick(r, "census/seat4rsh/a/labels/RshAUTHRshRegLabelProvider.java");
        assertContains(labels, "\t\tstartNode.addLabel(Arrays.asList(\"utiField\"), \"UTI\");\n\t\tstartNode.addLabel(Arrays.asList(\"notionalField\"), \"Notional\");\n");
        assertEquals(0, r.counts().getOrDefault(SilentDegradation.Site.REPORT_REFERENCE_UNRESOLVED, 0));
    }

    /**
     * b1 (oracle group qualify-first-wins): two namespaces each declare {@code isProduct root} over a
     * same-named type; ONLY the first in load order registers its qualifiers — the other's meta is the
     * golden's empty list ({@code Collections.emptyList()}, the {@code Collections} import, no function
     * import). The SAME two files in the other order make the other namespace win: the rule is the ORDER,
     * not a name or a hash (the released plugin's path-dependent order is replayed by pin where a golden fixes it).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_firstConfigurationInLoadOrderWins_theOtherMetaIsEmpty_andTheOrderDecides() throws IOException {
        Render ab = renderQfw();
        String a = pick(ab, "census/seat4qfw/a/meta/QfwTermsMeta.java");
        String b = pick(ab, "census/seat4qfw/b/meta/QfwTermsMeta.java");
        assertContains(a, "import census.seat4qfw.a.functions.Qualify_QfwA1;\nimport census.seat4qfw.a.functions.Qualify_QfwA2;\n");
        assertContains(a, "\t\treturn Arrays.asList(\n\t\t\tfactory.<QfwTerms>create(Qualify_QfwA1.class),\n\t\t\tfactory.<QfwTerms>create(Qualify_QfwA2.class)\n\t\t);\n");
        assertAbsent(a, "import java.util.Collections;");
        assertContains(b, "import java.util.Collections;");
        assertContains(b, "\tpublic List<Function<? super QfwTerms, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {\n\t\treturn Collections.emptyList();\n\t}\n");
        assertAbsent(b, "Qualify_QfwB1");
        assertTrue(ab.errors().isEmpty(), ab.errors().toString());
        // the same files, b FIRST: b wins and a's meta is the empty one
        Render ba = renderModels(List.<String[]>of(new String[] {"seat4qfw-b.rosetta", QFW_B}, new String[] {"seat4qfw-a.rosetta", QFW_A}),
                "census.seat4qfw", false);
        String a2 = pick(ba, "census/seat4qfw/a/meta/QfwTermsMeta.java");
        String b2 = pick(ba, "census/seat4qfw/b/meta/QfwTermsMeta.java");
        assertContains(b2, "\t\treturn Arrays.asList(\n\t\t\tfactory.<QfwTerms>create(Qualify_QfwB1.class)\n\t\t);\n");
        assertContains(a2, "\t\treturn Collections.emptyList();\n");
        assertAbsent(a2, "Qualify_QfwA1");
        assertEquals(ab.output().keySet(), ba.output().keySet(), "the order moves the winner, never the file set");
    }

    /**
     * b2 (oracle group qualify-event-and-product): first-wins is PER KIND — an {@code isEvent root} in the
     * first namespace and an {@code isProduct root} in the second both register their qualifiers.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_firstWinsIsPerKind_eventAndProductRootsBothRegister() throws IOException {
        Render r = renderQep();
        String event = pick(r, "census/seat4qep/a/meta/QepEventMeta.java");
        String product = pick(r, "census/seat4qep/b/meta/QepProductMeta.java");
        assertContains(event, "\t\treturn Arrays.asList(\n\t\t\tfactory.<QepEvent>create(Qualify_QepE.class)\n\t\t);\n");
        assertContains(product, "\t\treturn Arrays.asList(\n\t\t\tfactory.<QepProduct>create(Qualify_QepP.class)\n\t\t);\n");
        assertAbsent(event, "Collections.emptyList()");
        assertAbsent(product, "Collections.emptyList()");
        assertTrue(r.errors().isEmpty(), r.errors().toString());
        assertTrue(r.validation(ValidationIssueCode.QUALIFICATION_ROOT_MISMATCH).isEmpty(),
                "every qualifier takes its kind's root: " + r.diagnostics());
    }

    /**
     * b3 (oracle group qualify-cross-namespace-input): a qualifier declared in ANOTHER namespace over the
     * root type registers on the root's meta beside the root namespace's own, imported from its package.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_qualifierInAnotherNamespace_registersOnTheRootsMeta() throws IOException {
        Render r = renderQcn();
        String meta = pick(r, "census/seat4qcn/a/meta/QcnTermsMeta.java");
        assertContains(meta, "import census.seat4qcn.a.functions.Qualify_QcnA;\n");
        assertContains(meta, "import census.seat4qcn.b.functions.Qualify_QcnB;\n");
        assertContains(meta, "\t\treturn Arrays.asList(\n\t\t\tfactory.<QcnTerms>create(Qualify_QcnA.class),\n\t\t\tfactory.<QcnTerms>create(Qualify_QcnB.class)\n\t\t);\n");
        assertTrue(r.errors().isEmpty(), r.errors().toString());
        assertTrue(r.validation(ValidationIssueCode.QUALIFICATION_ROOT_MISMATCH).isEmpty(),
                "the cross-namespace qualifier takes THE root: " + r.diagnostics());
    }

    /**
     * b4 (upstream {@code RosettaSimpleValidator.checkQualificationAnnotation}, the released message bytes): every
     * qualifier whose input is NOT the first-wins root of either kind is WARNED about — the losing
     * namespace's, in b1's order ONE ({@code Qualify_QfwB1}); in the other order TWO ({@code Qualify_QfwA1},
     * {@code Qualify_QfwA2}) — from the SAME declaration the meta generator reads
     * ({@code RQualifiableConfig.firstRoot}, LAW 69), so the warning and the empty meta can never disagree.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_theLosingQualifiers_areWarnedAbout_withUpstreamsMessage() throws IOException {
        Render ab = renderQfw();
        List<ValidationDiagnostic> warnings = ab.validation(ValidationIssueCode.QUALIFICATION_ROOT_MISMATCH);
        assertEquals(1, warnings.size(), "b's one qualifier loses: " + ab.diagnostics());
        assertEquals("Input type does not match qualification root type.", warnings.get(0).message());
        assertEquals(Severity.WARNING, warnings.get(0).severity());
        assertEquals("seat4qfw-b.rosetta", warnings.get(0).range().file(), "anchored in the losing file");
        Render ba = renderModels(List.<String[]>of(new String[] {"seat4qfw-b.rosetta", QFW_B}, new String[] {"seat4qfw-a.rosetta", QFW_A}),
                "census.seat4qfw", false);
        List<ValidationDiagnostic> reversed = ba.validation(ValidationIssueCode.QUALIFICATION_ROOT_MISMATCH);
        assertEquals(2, reversed.size(), "a's two qualifiers lose in the other order: " + ba.diagnostics());
        assertTrue(reversed.stream().allMatch(w -> "seat4qfw-a.rosetta".equals(w.range().file())), reversed.toString());
        assertTrue(ab.linking(DiagnosticCategory.TYPE_NOT_FOUND).isEmpty(), "nothing else is wrong with the fixture: " + ab.diagnostics());
    }

    /**
     * b5 (round 1 — the code-quality review's SF-2 proposal REFUTED by upstream's source): a qualifier in a
     * workspace that declares NO {@code isProduct} / {@code isEvent} root at all IS warned about —
     * {@code RosettaConfigExtension.isRootEventOrProduct} compares the input against two null roots and
     * answers false, so the released validator warns; a null guard would silence upstream's warning. The
     * type's meta registers nothing (there is no root). Lane P (the guard added) is the witness.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_aQualifierWithNoRootDeclared_isWarnedAbout_asUpstreamsNullRootAnswersFalse() throws IOException {
        Render r = renderQnr();
        List<ValidationDiagnostic> warnings = r.validation(ValidationIssueCode.QUALIFICATION_ROOT_MISMATCH);
        assertEquals(1, warnings.size(), "the one qualifier is warned about with no root declared: " + r.diagnostics());
        assertEquals("Input type does not match qualification root type.", warnings.get(0).message());
        assertEquals(Severity.WARNING, warnings.get(0).severity());
        assertEquals("seat4qnr.rosetta", warnings.get(0).range().file());
        assertTrue(r.errors().isEmpty(), r.errors().toString());
        String meta = pick(r, "census/seat4qnr/a/meta/QnrTermsMeta.java");
        assertContains(meta, "Collections.emptyList()");
        assertAbsent(meta, "Qualify_QnrA1");
    }

    // =========================================================================
    // Controls — the populations, LAW 77, the corpus locks and their ability to fail
    // =========================================================================

    /** The two fixtures that DECLARE an unresolvable reference (round 2, cq NIT-4: a declared set, never a prefix test on the key). */
    private static final Set<String> REFUSING_FIXTURES = Set.of("unt", "uns");
    /** The eleven fixture renders' emitted-file counts — the population pins of control2a (measured, never guessed). */
    private static final Map<String, Integer> POPULATION = Map.ofEntries(
            Map.entry("rws", 12), Map.entry("rwq", 10), Map.entry("rsp", 12), Map.entry("rsr", 11), Map.entry("unt", 5),
            Map.entry("uns", 5), Map.entry("qfw", 5), Map.entry("qep", 4), Map.entry("qcn", 3),
            Map.entry("rsh", 7), Map.entry("qnr", 2));   // measured at pre-commit6 (rules + report fn + label + metas; meta + function)

    /**
     * control2a: the DEFAULT route renders every fixture at its declared population and without an
     * error but the two declared refusal pairs — the preconditions the identity control needs, red on
     * their own whenever a mutation changes WHAT the default route emits.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void control2a_defaultRoute_populationPins() throws IOException {
        Map<String, Render> renders = allRenders(false);
        for (Map.Entry<String, Integer> pin : POPULATION.entrySet()) {
            Render r = renders.get(pin.getKey());
            assertEquals(pin.getValue().intValue(), r.output().size(),
                    "fixture " + pin.getKey() + " emits " + pin.getValue() + " files on the default route; rendered " + r.output().keySet());
            int expectedErrors = REFUSING_FIXTURES.contains(pin.getKey()) ? 2 : 0;
            assertEquals(expectedErrors, r.errors().size(), "fixture " + pin.getKey() + " errors: " + r.errors());
        }
        assertEquals(POPULATION.keySet(), renders.keySet(), "every fixture is pinned");
    }

    /**
     * control2b — LAW 77: the IR route renders every fixture byte-identically to the default route,
     * refusing alike. The report function's body goes through the IR-route {@code FunctionGenerator}
     * (the seam hands it to {@code ReportGenerator}); the label provider and the meta generator have no
     * route subclass, so their half is a same-generator comparison over a route-specific workspace.
     * Runs only with the IR provider on the classpath ({@code -Pir-on} — the chain's ON-route seat step).
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void control2b_irRoute_rendersEveryFixtureIdenticallyToTheDefaultRoute() throws IOException {
        Map<String, Render> def = allRenders(false);
        Map<String, Render> ir = withIr(() -> allRenders(true));
        assertEquals(def.keySet(), ir.keySet());
        for (String fixture : def.keySet()) {
            Render d = def.get(fixture);
            Render i = ir.get(fixture);
            assertEquals(d.errors().size(), i.errors().size(), fixture + ": the two routes must refuse alike: " + d.errors() + " vs " + i.errors());
            assertEquals(d.refusalsAt(SilentDegradation.Site.REPORT_REFERENCE_UNRESOLVED).size(),
                    i.refusalsAt(SilentDegradation.Site.REPORT_REFERENCE_UNRESOLVED).size(), fixture + ": the same refusals at the site");
            assertEquals(d.output().keySet(), i.output().keySet(), fixture + ": the two routes must emit the SAME file set");
            for (String key : d.output().keySet()) {
                assertEquals(normalize(d.output().get(key)), normalize(i.output().get(key)),
                        fixture + ": the IR route must agree with the default route for " + key);
            }
        }
    }

    /** corpus_c1: the twelve s07 report functions, a1o1 included, byte-identical to the chaos goldens. */
    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c1_C7AUTHC7RegReportFunction_allTwelveVariants() throws IOException {
        lockChaos("s07", "C7AUTHC7RegReportFunction", "reports", S07_FAMILIES, Set.of());
    }

    /**
     * corpus_c2: the sixteen s22 metas — the winner's qualifiers registered, every loser's list empty.
     * chaos-1.1.0 (v3.2 seat 10, D49): the fresh files reshuffled the Product race — upstream's GENERATOR crowned a
     * qualifier-free slim carrier, so NO s22 meta registers at 1.1.0 and the a1o1 golden moved to the empty list
     * while the fork (the lexical first, upstream's VALIDATOR's winner) still registers a1o1's two qualifiers: the
     * baseline's ONE gen-1 byte row, {@code chaos/1.1.0/XMETA:chaos/s22/a1o1/meta/C22TermsMeta.java} (the census's
     * M13, the #413-class replay pin owed to the second round). The lock tolerates EXACTLY that declared carrier
     * and asserts it still diverges (LAW 81); the other fifteen stay byte-locked. Round 1: the c6 verify gate's
     * catch — the whole-suite law (the seat's targeted drivers had never run this class).
     */
    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c2_C22TermsMeta_allSixteenVariants() throws IOException {
        lockChaos("s22", "C22TermsMeta", "meta", S22_FAMILIES,
                Set.of("chaos/s22/a1o1/meta/C22TermsMeta.java"));
    }

    /** corpus_c3: the twelve s07 label providers (the seat's second consumer of the report's output type). */
    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c3_C7AUTHC7RegLabelProvider_allTwelveVariants() throws IOException {
        lockChaos("s07", "C7AUTHC7RegLabelProvider", "labels", S07_FAMILIES, Set.of());
    }

    /**
     * control3: the whole-file lock is proven able to fail — a doctored golden is reported by name. chaos-1.1.0
     * (round 1): the carrier is a1o2 — byte-identical at 1.1.0 (a1o1 is the declared M13 row, see corpus_c2) — and the
     * doctor rewrites its empty qualifier list (no s22 golden registers a qualifier at 1.1.0, so the 1.0.0 doctor's
     * {@code create(Qualify_C22Exotic.class)} token no longer exists to doctor).
     */
    @Test
    @EnabledIf("chaosAvailable")
    void control3_doctoredGolden_isReportedByTheLock() throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        String carrier = "chaos/s22/a1o2/meta/C22TermsMeta.java";
        String generated = chaosOutput.get(carrier);
        assertNotNull(generated, "not generated: " + carrier);
        String golden = normalize(Files.readString(CHAOS_GOLDEN.resolve(carrier)));
        assertTrue(compareCarrier(carrier, golden, generated) == null,
                "the undoctored carrier must lock green before the doctor is applied");
        String doctored = golden.replace("return Collections.emptyList();",
                "return Collections.<Function<? super C22Terms, QualifyResult>>emptyList();");
        assertTrue(!doctored.equals(golden), "the doctor must change a byte");
        String verdict = compareCarrier(carrier, doctored, generated);
        assertNotNull(verdict, "a doctored golden MUST be reported by the lock");
        assertContains(verdict, "emptyList");
    }

    /** control4: the placement-family identity pin proven able to fail (over the sixteen-family s22 set). */
    @Test
    void control4_placementFamilyPin_canFail() {
        assertEquals("a3half", placementFamily("a3half/p1"));
        assertEquals("base", placementFamily("base"));
        List<String> good = S22_FAMILIES.stream().map(v -> "chaos/s99/" + v + "/p1/meta/X.java").toList();
        assertTrue(placementFamilyVerdict("s99", good, "meta", S22_FAMILIES) == null, "the enumerated sixteen must pass");
        List<String> doctored = new ArrayList<>(good);
        doctored.set(doctored.indexOf("chaos/s99/a4none/p1/meta/X.java"), "chaos/s99/a1o1/p2/meta/X.java");
        String verdict = placementFamilyVerdict("s99", doctored, "meta", S22_FAMILIES);
        assertNotNull(verdict, "a doctored population MUST be reported");
        String found = verdict.substring(verdict.indexOf("found "));
        assertTrue(!found.contains("a4none"), "the missing family must be absent from the found list: " + found);
        assertTrue(found.indexOf("a1o1") != found.lastIndexOf("a1o1"), "the doubled family must appear twice: " + found);
    }

    // =========================================================================
    // Fixture harness — multi-model, four kinds + functions, both routes, the diagnostics kept per render
    // =========================================================================

    /**
     * One render: the emitted files, the TYPED generation errors (a refusal is checked by its class and
     * site, never by its message text), the register's counts and the workspace's diagnostics (the
     * linker's and the validators').
     */
    private record Render(Map<String, String> output, List<GenerationException> errors,
                          Map<SilentDegradation.Site, Integer> counts, List<RDiagnostic> diagnostics) {
        List<SilentDegradation.Refusal> refusalsAt(SilentDegradation.Site site) {
            return errors.stream()
                    .filter(e -> e instanceof SilentDegradation.Refusal r && r.site() == site)
                    .map(e -> (SilentDegradation.Refusal) e)
                    .toList();
        }

        List<LinkingDiagnostic> linking(DiagnosticCategory category) {
            return diagnostics.stream()
                    .filter(d -> d instanceof LinkingDiagnostic l && l.category() == category)
                    .map(d -> (LinkingDiagnostic) d)
                    .toList();
        }

        List<ValidationDiagnostic> validation(ValidationIssueCode code) {
            return diagnostics.stream()
                    .filter(d -> d instanceof ValidationDiagnostic v && v.issueCode() == code)
                    .map(d -> (ValidationDiagnostic) d)
                    .toList();
        }
    }

    private static final Map<String, Render> RENDERED = new LinkedHashMap<>();

    private static Render cached(String fixture, RenderCall call) throws IOException {
        Render r = RENDERED.get(fixture);
        if (r == null) {
            r = call.call();
            RENDERED.put(fixture, r);
        }
        return r;
    }

    private static Render renderRws() throws IOException {
        return cached("rws", () -> renderModels(files("rws"), "census.seat4rws", false));
    }

    private static Render renderRwq() throws IOException {
        return cached("rwq", () -> renderModels(files("rwq"), "census.seat4rwq", false));
    }

    private static Render renderRsp() throws IOException {
        return cached("rsp", () -> renderModels(files("rsp"), "census.seat4rsp", false));
    }

    private static Render renderRsr() throws IOException {
        return cached("rsr", () -> renderModels(files("rsr"), "census.seat4rsr", false));
    }

    private static Render renderQfw() throws IOException {
        return cached("qfw", () -> renderModels(files("qfw"), "census.seat4qfw", false));
    }

    private static Render renderQep() throws IOException {
        return cached("qep", () -> renderModels(files("qep"), "census.seat4qep", false));
    }

    private static Render renderQcn() throws IOException {
        return cached("qcn", () -> renderModels(files("qcn"), "census.seat4qcn", false));
    }

    private static Render renderRsh() throws IOException {
        return cached("rsh", () -> renderModels(files("rsh"), "census.seat4rsh", false));
    }

    private static Render renderQnr() throws IOException {
        return cached("qnr", () -> renderModels(files("qnr"), "census.seat4qnr", false));
    }

    /** The fixture files of one set, in LOAD ORDER (the first namespace first — the order the oracle groups were pinned in). */
    private static List<String[]> files(String fixture) {
        return switch (fixture) {
            case "rws" -> List.<String[]>of(new String[] {"seat4rws-a.rosetta", RWS_A}, new String[] {"seat4rws-b.rosetta", RWS_B});
            case "rwq" -> List.<String[]>of(new String[] {"seat4rwq-a.rosetta", RWQ_A}, new String[] {"seat4rwq-b.rosetta", RWQ_B});
            case "rsp" -> List.<String[]>of(new String[] {"seat4rsp-a.rosetta", RSP_A}, new String[] {"seat4rsp-p2.rosetta", RSP_P2},
                    new String[] {"seat4rsp-p3.rosetta", RSP_P3});
            case "rsr" -> List.<String[]>of(new String[] {"seat4rsr-a.rosetta", RSR_A}, new String[] {"seat4rsr-b.rosetta", RSR_B});
            case "unt" -> List.<String[]>of(new String[] {"seat4unt.rosetta", UNT});
            case "uns" -> List.<String[]>of(new String[] {"seat4uns.rosetta", UNS});
            case "qfw" -> List.<String[]>of(new String[] {"seat4qfw-a.rosetta", QFW_A}, new String[] {"seat4qfw-b.rosetta", QFW_B});
            case "qep" -> List.<String[]>of(new String[] {"seat4qep-a.rosetta", QEP_A}, new String[] {"seat4qep-b.rosetta", QEP_B});
            case "qcn" -> List.<String[]>of(new String[] {"seat4qcn-a.rosetta", QCN_A}, new String[] {"seat4qcn-b.rosetta", QCN_B});
            case "rsh" -> List.<String[]>of(new String[] {"seat4rsh.rosetta", RSH});
            case "qnr" -> List.<String[]>of(new String[] {"seat4qnr.rosetta", QNR});
            default -> throw new IllegalArgumentException(fixture);
        };
    }

    /** Every fixture set rendered on one route, keyed by its short name (the population pins' domain). */
    private static Map<String, Render> allRenders(boolean irRoute) throws IOException {
        Map<String, Render> out = new LinkedHashMap<>();
        for (String fixture : List.of("rws", "rwq", "rsp", "rsr", "unt", "uns", "qfw", "qep", "qcn", "rsh", "qnr")) {
            String prefix = "census.seat4" + fixture;
            out.put(fixture, irRoute ? renderModels(files(fixture), prefix, true)
                    : cached(fixture, () -> renderModels(files(fixture), prefix, false)));
        }
        return out;
    }

    private interface RenderCall {
        Render call() throws IOException;
    }

    private interface RendersCall {
        Map<String, Render> call() throws IOException;
    }

    private static Map<String, Render> withIr(RendersCall call) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            return call.call();
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static RModel model(String source, String fileName) {
        RModel m = AstBuilder.buildFromString(source, fileName);
        m.setVersion("0.0.0.test");
        return m;
    }

    /**
     * The render: the fixture models (in the given LOAD ORDER) plus the builtins, linked and validated
     * ({@code RWorkspace.build} runs the validation pass — the b4 warning is read from its diagnostics),
     * then the four kinds the seat touches (rules, report functions, label providers, metas) through the
     * production dispatch seam ({@code IRGeneration.generateClasses}) and the functions through the
     * route's own {@code FunctionGenerator}, whose IR-route instance the report generator renders with.
     */
    private static Render renderModels(List<String[]> files, String namespacePrefix, boolean irRoute) throws IOException {
        List<RModel> models = new ArrayList<>();
        for (String[] f : files) {
            models.add(model(f[1], f[0]));
        }
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linked = RWorkspace.build(models);
        RWorkspace workspace = linked.workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> m.namespace().startsWith(namespacePrefix));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = irRoute
                ? IRGeneration.functionGenerator(gm, tt, typeUtil)
                : new FunctionGenerator(gm, tt, typeUtil);
        if (irRoute) {
            assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
        }
        RuleGenerator rules = new RuleGenerator(gm, tt, fg);
        ReportGenerator reports = new ReportGenerator(gm, tt, fg);
        LabelProviderGenerator labels = new LabelProviderGenerator(gm, tt, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        ModelMetaGenerator metas = new ModelMetaGenerator(gm, tt);
        SilentDegradation.reset();
        Map<String, String> out = new LinkedHashMap<>();
        List<GenerationException> errors = new ArrayList<>();
        for (RModel model : workspace.files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                errors.addAll(IRGeneration.generateClasses(rules, model, version, out));
                errors.addAll(IRGeneration.generateClasses(reports, model, version, out));
                errors.addAll(IRGeneration.generateClasses(labels, model, version, out));
                errors.addAll(IRGeneration.generateClasses(metas, model, version, out));
            }
        }
        errors.addAll(fg.generateWithErrors(out));
        Map<SilentDegradation.Site, Integer> counts = SilentDegradation.counts();
        SilentDegradation.reset();
        return new Render(out, errors, counts, linked.diagnostics());
    }

    private static String pick(Render r, String suffix) {
        String fileName = suffix.substring(suffix.lastIndexOf('/') + 1);
        List<GenerationException> own = r.errors().stream()
                .filter(e -> (e.getTargetPath() != null && e.getTargetPath().endsWith("/" + suffix))
                        || (e.getTargetPath() == null && String.valueOf(e.getMessage()).contains(fileName)))
                .toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for " + fileName + " (a broken fixture must fail"
                + " loudly, not skip): " + own);
        List<String> matches = r.output().keySet().stream()
                .filter(k -> k.endsWith("/" + suffix) || k.equals(suffix))
                .sorted()
                .toList();
        assertTrue(!matches.isEmpty(), "not generated: " + suffix + " (have: " + r.output().keySet() + ")");
        assertEquals(1, matches.size(), "exactly ONE emitted file may match " + suffix
                + " (an ambiguous suffix is a harness error, never a first-match pick): " + matches);
        return normalize(r.output().get(matches.get(0)));
    }

    /** The 1-based line of {@code needle}'s first occurrence in a fixture (the token-range anchoring witness). */
    private static int lineOf(String fixture, String needle) {
        int at = fixture.indexOf(needle);
        assertTrue(at >= 0, "fixture lacks " + needle);
        return (int) fixture.substring(0, at).chars().filter(c -> c == '\n').count() + 1;
    }

    private static void assertContains(String code, String needle) {
        assertTrue(code.contains(needle), "expected <" + needle + "> in:\n" + code);
    }

    private static void assertAbsent(String code, String needle) {
        assertTrue(!code.contains(needle), "did NOT expect <" + needle + "> in:\n" + code);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    /**
     * {@code null} when the generated text byte-matches the golden (newline-normalised); else the
     * named difference — the ONE comparison every whole-class lock and the L3 control go through.
     */
    private static String compareCarrier(String carrier, String golden, String generated) {
        String gen = normalize(generated);
        return golden.equals(gen) ? null
                : carrier + ": differs from its golden — " + firstDifference(golden, gen);
    }

    private static String firstDifference(String golden, String generated) {
        String[] g = golden.split("\n");
        String[] r = generated.split("\n");
        int n = Math.min(g.length, r.length);
        for (int i = 0; i < n; i++) {
            if (!g[i].equals(r[i])) {
                return "line " + (i + 1) + " golden <" + g[i].strip() + "> vs generated <" + r[i].strip() + ">";
            }
        }
        return "lengths differ: golden " + g.length + " lines vs generated " + r.length + " lines";
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
            throw new AssertionError("[OrderDependenceSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // The chaos cell — generated once (report functions, label providers, metas), every carrier locked whole
    // =========================================================================

    private static Map<String, String> chaosOutput;
    private static List<GenerationException> chaosGenErrors;

    @BeforeAll
    static void generateChaosCell() throws IOException {
        if (chaosAvailable()) {
            List<GenerationException> errs = new ArrayList<>();
            chaosOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("chaos", "1.0.0", CHAOS_ROOT), errs);
            chaosGenErrors = errs;
        }
    }

    /**
     * The three kinds this seat's declared rows live in, over the D11 loader's cell (its load order is the
     * sorted full-path walk plus the cell's pins — the chaos cell needs none: a1o1 sorts first and is the
     * released plugin's winner); the functions are NOT generated here (the report function is the report
     * generator's file, rendered with the function generator's template).
     */
    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<GenerationException> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var labelGen = new LabelProviderGenerator(gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        var metaGen = new ModelMetaGenerator(gm, typeTranslator);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                errors.addAll(IRGeneration.generateClasses(reportGen, model, version, output));
                errors.addAll(IRGeneration.generateClasses(labelGen, model, version, output));
                errors.addAll(IRGeneration.generateClasses(metaGen, model, version, output));
            }
        }
        return output;
    }

    /** {@code a3half/p1} -> {@code a3half}; {@code base} -> {@code base}. */
    private static String placementFamily(String variant) {
        int slash = variant.indexOf('/');
        return slash < 0 ? variant : variant.substring(0, slash);
    }

    /**
     * {@code null} when {@code carriers} (golden-relative paths under {@code chaos/<seat>/}) hold every
     * placement FAMILY of {@code expectedFamilies} exactly once; else the named difference.
     */
    private static String placementFamilyVerdict(String seat, List<String> carriers, String parentDir,
            List<String> expectedFamilies) {
        String prefix = "chaos/" + seat + "/";
        String marker = "/" + parentDir + "/";
        List<String> families = carriers.stream()
                .map(c -> c.substring(prefix.length(), c.lastIndexOf(marker)))
                .map(OrderDependenceSeatTest::placementFamily)
                .sorted()
                .toList();
        List<String> expected = expectedFamilies.stream().sorted().toList();
        return expected.equals(families) ? null
                : "expected one carrier per placement family " + expected + ", found " + families;
    }

    /**
     * Byte-lock every placement variant of one declared class against the chaos goldens. A carrier
     * the generator refused, did not emit, or emitted differently fails by NAME — unless it is one of
     * {@code expectedDeclared}: a carrier the committed baseline ({@code chaos-expected-divergence.txt}, read through
     * {@code ChaosDeclaredRows}) DECLARES divergent, which must (a) be a row of that baseline and (b) still diverge —
     * a byte-identical declared carrier is a HEALED row to delete (LAW 81; the D11's stale-waiver law), never a silent
     * pass — and the set of tolerated carriers must equal {@code expectedDeclared} exactly (LAW 73: the set).
     */
    private static void lockChaos(String seat, String simpleName, String parentDir, List<String> families,
            Set<String> expectedDeclared) throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        List<String> carriers = carriersOf(seat, simpleName, parentDir);
        assertEquals(families.size(), carriers.size(),
                "expected the declared " + families.size() + " placement variants of " + simpleName
                + " under chaos/" + seat + ", found " + carriers);
        String familyVerdict = placementFamilyVerdict(seat, carriers, parentDir, families);
        assertTrue(familyVerdict == null,
                "the carriers of " + simpleName + " must be one per placement family: " + familyVerdict);
        Set<String> declared = com.regnosys.rosetta.testutil.ChaosDeclaredRows.declaredPaths();
        for (String expected : expectedDeclared) {
            assertTrue(declared.contains(expected), "the lock tolerates " + expected
                    + " as declared-divergent, but the baseline holds no such row");
        }
        List<String> failures = new ArrayList<>();
        java.util.TreeSet<String> toleratedDeclared = new java.util.TreeSet<>();
        for (String carrier : carriers) {
            String verdict = lockOne(carrier);
            if (declared.contains(carrier)) {
                if (verdict == null) {
                    failures.add(carrier + ": DECLARED divergent in chaos-expected-divergence.txt but byte-identical"
                            + " to its golden now — a HEALED row to delete from the baseline (LAW 81)");
                } else {
                    toleratedDeclared.add(carrier);
                }
                continue;
            }
            if (verdict != null) {
                failures.add(verdict);
            }
        }
        System.out.println("[LOCK] " + simpleName + " declared-divergent carriers tolerated ("
                + toleratedDeclared.size() + "/" + families.size() + "): " + toleratedDeclared);
        assertEquals(new java.util.TreeSet<>(expectedDeclared), toleratedDeclared,
                "the declared-divergent carriers of " + simpleName + " must equal the lock's expected set exactly");
        assertTrue(failures.isEmpty(), "carriers of " + simpleName + " not byte-identical to the chaos"
                + " goldens (" + failures.size() + "/" + families.size() + "):\n  "
                + String.join("\n  ", failures));
    }

    private static List<String> carriersOf(String seat, String simpleName, String parentDir) throws IOException {
        Path seatRoot = CHAOS_GOLDEN.resolve("chaos").resolve(seat);
        try (var stream = Files.walk(seatRoot)) {
            return stream
                    .filter(p -> p.getFileName().toString().equals(simpleName + ".java"))
                    .filter(p -> p.getParent().getFileName().toString().equals(parentDir))
                    .map(p -> CHAOS_GOLDEN.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }

    /** {@code null} when one carrier byte-matches its golden; else the named failure. */
    private static String lockOne(String carrier) throws IOException {
        List<GenerationException> own = chaosGenErrors.stream()
                .filter(e -> carrier.equals(e.getTargetPath())
                        || (e.getTargetPath() == null && String.valueOf(e.getMessage()).contains(carrier)))
                .toList();
        if (!own.isEmpty()) {
            return carrier + ": generator errors " + own;
        }
        String generated = chaosOutput.get(carrier);
        if (generated == null) {
            return carrier + ": not generated";
        }
        Path goldenPath = CHAOS_GOLDEN.resolve(carrier);
        if (!Files.isRegularFile(goldenPath)) {
            return carrier + ": golden missing at " + goldenPath;
        }
        return compareCarrier(carrier, normalize(Files.readString(goldenPath)), generated);
    }
}
