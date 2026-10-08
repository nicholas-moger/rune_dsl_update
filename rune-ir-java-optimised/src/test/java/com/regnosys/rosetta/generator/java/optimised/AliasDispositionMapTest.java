package com.regnosys.rosetta.generator.java.optimised;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RPostCondition;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionAliasHelper;
import com.regnosys.rosetta.generator.java.function.FunctionDependencyCollector;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.ExpressionCardinality;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE § 6.3 PER-MEMBER DISPOSITION MAP + THE GOLDEN-INVOCATION ORACLE (the
 * program plan § 8 T0 items 2–3; the program's SOT artifact): every alias
 * declaration of the five cells joined to its body kind, consumer-shape
 * multisets (both census channels), D1–D5 flags, S/C bit per channel, generated
 * class file, golden-oracle seat counts and the machine-derived TRANCHE
 * assignment — written per cell to
 * {@code target/alias-disposition-map/<cellLeaf>.tsv} (LF, SHA-256 printed) with
 * an {@code ALIASMAP} stdout summary. Tranche membership is read FROM this
 * artifact (census-then-edit, the program-local form of census-then-price): no
 * tranche PR quotes a membership number this map did not derive.
 *
 * <p><b>A SEPARATE walk from {@link NavigationChainCensusTest}</b> — this
 * instrument bumps no census row, so every standing § 4/§ 9/§ 10/§ 12/§ 19 row
 * stays byte-comparable; the consumer-seat classifier and the chain-link kind
 * set are SHARED with the census (package-visible there), never reimplemented,
 * so the per-member multisets reconcile to the census's global rows by
 * construction.
 *
 * <p><b>The golden-invocation oracle</b> (the D4 gate input — law 48: a counting
 * channel's zero means zero-CHANNEL-VISIBLE, never dead): a FIXED-TOKEN count of
 * {@code aliasName(} occurrences over the declaring function's reference golden,
 * used as a CROSS-CHECK RECEIPT per the engineering standards' trivial-literal
 * exception (the § 19a golden-grep column precedent) — the AST/IR channels stay
 * the structural SOT. An occurrence counts only when the preceding character is
 * neither a Java identifier part nor {@code '.'} (excludes {@code getRate(} for
 * alias {@code rate} and qualified calls {@code x.execute(} for alias
 * {@code execute}); each occurrence classifies by its LINE: a
 * {@code protected abstract }-headed line is the seam declaration, any other
 * modifier-headed line the override header, everything else an INVOCATION seat.
 * The scanned token is the RENDERED method name: where an alias name collides
 * with a dependency field name, the render's own collision law
 * ({@link FunctionDependencyCollector#collidingDependencyAliasNames} — the
 * decl-seat authority) numbers the group, dependency field {@code name0} /
 * alias method {@code name1}, and the oracle consults that same law rather than
 * re-deriving it.
 * Machinery-correctness pins (channel-agreement, corpus-bump belts): per cell
 * the abstract-seat total equals the declaration population and the
 * Mapper-typed abstract-seat total equals the non-usesOutput seam population;
 * the POSITIVE CONTROL is the census § 19c disguised-spelling witness — the
 * cdm5 {@code NewEquitySwapProduct.payout} usesOutput alias counts ZERO on both
 * counting channels while its golden carries EXACTLY SIX invocation seats. For
 * dispatch files whose case units re-declare one alias name, oracle counts
 * carry FILE-GROUP grain (recorded per row; per-unit attribution is a T4
 * careful-class job).
 *
 * <p><b>The D1 transitive closure</b> (machine-derived here, sized nowhere
 * else): a D1/D3-excluded alias keeps its Mapper body VERBATIM (plan § 6), so
 * any alias that body references could not re-type without breaking the
 * verbatim text — the exclusion propagates along alias→alias reference edges to
 * a fixpoint, and the map labels those rows {@code EXCLUDED_D1T}. The tranche
 * partition (T1–T4 / EXCLUDED_*) conserves to the declaration population, per
 * cell, pinned.
 */
class AliasDispositionMapTest {

    private static final Path MAP_DIR = Path.of("target", "alias-disposition-map");

    /** The census § 19c positive-control witness (corpus facts; a firing reopens § 19c). */
    private static final String WITNESS_CELL = "cdm/5.38.0";
    private static final String WITNESS_NAMESPACE = "cdm.event.common";
    private static final String WITNESS_FUNCTION = "NewEquitySwapProduct";
    private static final String WITNESS_ALIAS = "payout";
    private static final int WITNESS_INVOCATION_SEATS = 6;

    /** The alias-body top kinds the plan § 4 groups as nav-chain-headed. */
    private static final Set<IRExprKind> CHAIN_HEAD_KINDS = Set.of(
            IRExprKind.FIELD_ACCESS, IRExprKind.META_ACCESS);

    private final FunctionAliasHelper aliasHelper = new FunctionAliasHelper();
    private final NavigationChainClassifier classifier = new NavigationChainClassifier();

    /** One map row = one alias declaration (the 1,820-grain). */
    private static final class Row {
        String cell;
        String namespace;
        String function;
        String dispatchValue = "-";
        int unitOrdinal;
        String alias;
        /**
         * The RENDERED method name — the oracle's token. Differs from
         * {@link #alias} exactly where the render's own collision law numbers a
         * dependency-field/alias name group
         * ({@link FunctionDependencyCollector#collidingDependencyAliasNames}:
         * the dependency field takes {@code name0}, the alias method
         * {@code name1} — the decl-seat authority FunctionGenerator itself
         * consults).
         */
        String renderedName;
        String file;
        boolean usesOutput;
        String irSc;
        String renderSc = "-";
        String d5 = "-";
        String bodyTopKind;
        String wholeChain = "-";
        final TreeMap<String, Integer> boundConsumers = new TreeMap<>();
        final TreeMap<String, Integer> boundContexts = new TreeMap<>();
        final TreeMap<String, Integer> irConsumers = new TreeMap<>();
        final TreeMap<String, Integer> irContexts = new TreeMap<>();
        boolean d1Direct;
        boolean d1Transitive;
        int oracleAbstract;
        int oracleImpl;
        int oracleInvocations;
        String oracleGrain = "EXACT";
        String tranche;

        int boundTotal() {
            return boundConsumers.values().stream().mapToInt(Integer::intValue).sum();
        }

        int irTotal() {
            return irConsumers.values().stream().mapToInt(Integer::intValue).sum();
        }

        int onlyExistsSeats() {
            return boundConsumers.getOrDefault("ONLY_EXISTS_ELEMENT", 0);
        }

        int boundConditionSeats() {
            return boundContexts.getOrDefault("condition", 0)
                    + boundContexts.getOrDefault("postCondition", 0);
        }

        int irConditionSeats() {
            return irContexts.getOrDefault("condition", 0)
                    + irContexts.getOrDefault("postCondition", 0);
        }

        boolean d4ZeroVisible() {
            return boundTotal() == 0 && irTotal() == 0;
        }
    }

    @Test
    void aliasDispositionMapOverTheFiveCells() throws IOException, NoSuchAlgorithmException {
        // The name predates PR #607: the map is now written for every active
        // catalogue cell of the corpus SOT (26 since the chaos cell, v3.2 PR-2),
        // one TSV per cell as before.
        Assumptions.assumeTrue(CorpusCells.corpusStaged(),
                "test-corpus not staged — skipped");
        Assumptions.assumeTrue(!CorpusCells.resolveBuiltinFiles().isEmpty(),
                "rune-dsl builtins absent (searched " + CorpusCells.BUILTINS_SEARCH_ROOTS
                        + ") — skipped");
        Files.createDirectories(MAP_DIR);

        for (CorpusCells.CellSpec cell : CorpusCells.cellsUnderTest()) {
            Assumptions.assumeTrue(CorpusCells.staged(cell),
                    "test-corpus " + cell.ownDir() + " not staged — skipped");
            mapCell(cell);
        }
    }

    /**
     * THE CHAOS DECLARED ALIAS-LAW EXCEPTIONS (v3.2 PR-2): the law-tagged grains where
     * the alias policy and the map legitimately DIVERGED on the chaos cell — census
     * F11 (s18: the type-keyed-switch alias whose render seat REFUSED, so the channel
     * the law reads never formed; HEALED at v3.2 seat 7, PR #628 — the 24 `sc|` rows
     * fell with the twelve F11 rows, 24 -> 0) and, until v3.2 seat 2, F4 (s05: the 26
     * `tranche|` C5Forms.fallback/lambdaIte members - their seam was the mis-joined
     * `MapperC<Integer>`; the seat's Law 4 typed the join and the rows fell, 50 -> 24).
     * Measured by the collect-mode run at the PR-2 head; EXACT set equality both
     * directions (an extra violation is a NEW finding, a missing one a STALE row);
     * shrink-only. EMPTY since seat 7 and KEPT as the belt (the `//` note below): the
     * collect mode has no declared grains, so any violation on the chaos cell is a NEW
     * finding. (Round 1, the rule-6 review's MF-3: this javadoc had kept the refusal live.)
     */
    // v3.2 seat 7 (F11): the 24 `sc|` rows (the C18ToKind `pulled` / `switched` aliases over every s18 placement
    // variant, declared at seat 2 when the function file was MISSING) are GONE - the twelve `pulled` rows healed at the
    // seat's second cut (the NAME-guarded lambda-item switch join, `MapperC<String>`) and the twelve `switched` rows at
    // its third (the list-literal receiver read MULTI): the aliases render on the optimised route with the S/C bit the
    // map's channel carries. The set stays as the belt: a NEW row is a finding to census, never to append.
    // chaos-1.1.0 (v3.2 seat 10, D49): RE-DERIVED at the swap from the collect-mode print of the FIRST gen-2 census
    // (consumers-c1, ov-optimised-c1.log; scratch/alias-violations-c1.txt) - 110 rows, every one on a FRESH family:
    //   38 `sc|` rows = s26's C26Lit.flags / C26Lit.nums over their placement variants - the alias signature walk under
    //     BOOLEAN / NUMBER literal guards (the seat-7 banked literal kinds beyond string and int: the walk signs them
    //     under NAME guards only, so the alias still reaches the raw-name fallback);
    //   60 `tranche|` rows at map=T3 = s26's five C26AliasArms switch aliases over their variants (the switch-ternary
    //     class M2 at the ALIAS seat - the seat-7 refusal does not guard it);
    //   12 `tranche|` rows at map=T4 = s29's C29Piped.piped over its variants (the closure-parameter NAME typed as the
    //     seam - M7c, NEW at chaos-1.1.0).
    // Shrink-only from here (the sixth declared set: 0 at #630 on chaos-1.0.0 -> 110 at the gen-2 swap).
    // v3.2 seat 12 (D52, COUNTERS FIRST): 110 -> 197 at the counter tree FROM THE PRINT (the second
    // optimised run at the counter tree, target/v32-seat12-instruments/scratch/alias-violations-c3pre2.txt, local): the
    // 38 `sc|` C26Lit rows and the 60 `tranche|` C26AliasArms rows are GONE as law grains - their functions now REFUSE at the
    // alias SIGNATURE (the R3 site ALIAS_SIGNATURE_RAW_TYPE, FunctionAliasHelper: the walk declined and the output's raw
    // rune name is a builtin) before the channel the law reads ever forms, and the refusal reaches this instrument
    // DIRECTLY (mapCell computes the seam per alias, outside the generator's per-element boundary) - collected as
    // `refused|<grain>|<site>` grains, 185 of them = EVERY alias of the 52 refused function files (C25Shadow 7 aliases x 9
    // variants = 63, C26AliasArms 5 x 12 = 60, C26Lit 2 x 19 = 38, C26Nested 2 x 12 = 24: the walk declines at every
    // alias of those functions). The 12 `tranche|` C29Piped rows (M7c, map=T4) UNMOVED. Shrink-only holds by KIND: the
    // law-violation rows 110 -> 12, the refused grains a re-classification of 98 of them plus the sibling aliases the
    // instrument never reached before (a refusal is LOUD where the violation was silent) - the D11's own
    // ALIAS_SIGNATURE_RAW_TYPE count is 52 FUNCTION files on both routes, one refusal per file at its first alias.
    // v3.2 seat 13 (D53, commit 3): 197 -> 197 FROM THE PRINT (target/v32-seat13-instruments/scratch/alias-violations-
    // actual-c3a.txt, local): 12 row(s) GONE as law grains (tranche x 12) - their aliases now REFUSE at a seat-13 site
    // before the channel the law reads forms - and 12 `refused|` grain(s) JOINED (ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE x
    // 12); a re-classification by KIND, shrink-only holding for the law-violation rows ({'tranche': 12} -> {}). R4
    // ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE refuses C29Piped at its alias signature on this route as on the default one,
    // LAW 77 by identity; R6 ALIAS_BODY_ITEM_UNWRAPPED fires on NO file here - the value-form re-seam of this route
    // returns the count's int against an Integer value seam, which compiles, so C23Scale stays an emitted, register-
    // declared twin
    private static final Set<String> CHAOS_DECLARED_ALIAS_LAW_EXCEPTIONS = Set.of(
            "refused|chaos/1.1.0 chaos.s25.a1o1:C25Shadow.bySub|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a1o1:C25Shadow.hi|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a1o1:C25Shadow.kept|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a1o1:C25Shadow.lo|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a1o1:C25Shadow.mixed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a1o1:C25Shadow.sorted|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a1o1:C25Shadow.total|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2alias:C25Shadow.bySub|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2alias:C25Shadow.hi|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2alias:C25Shadow.kept|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2alias:C25Shadow.lo|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2alias:C25Shadow.mixed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2alias:C25Shadow.sorted|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2alias:C25Shadow.total|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2dangle:C25Shadow.bySub|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2dangle:C25Shadow.hi|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2dangle:C25Shadow.kept|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2dangle:C25Shadow.lo|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2dangle:C25Shadow.mixed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2dangle:C25Shadow.sorted|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2dangle:C25Shadow.total|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2qual:C25Shadow.bySub|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2qual:C25Shadow.hi|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2qual:C25Shadow.kept|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2qual:C25Shadow.lo|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2qual:C25Shadow.mixed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2qual:C25Shadow.sorted|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2qual:C25Shadow.total|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2wild:C25Shadow.bySub|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2wild:C25Shadow.hi|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2wild:C25Shadow.kept|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2wild:C25Shadow.lo|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2wild:C25Shadow.mixed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2wild:C25Shadow.sorted|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a2wild:C25Shadow.total|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3half.p2:C25Shadow.bySub|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3half.p2:C25Shadow.hi|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3half.p2:C25Shadow.kept|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3half.p2:C25Shadow.lo|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3half.p2:C25Shadow.mixed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3half.p2:C25Shadow.sorted|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3half.p2:C25Shadow.total|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3hub.p2:C25Shadow.bySub|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3hub.p2:C25Shadow.hi|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3hub.p2:C25Shadow.kept|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3hub.p2:C25Shadow.lo|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3hub.p2:C25Shadow.mixed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3hub.p2:C25Shadow.sorted|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3hub.p2:C25Shadow.total|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3third.p3:C25Shadow.bySub|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3third.p3:C25Shadow.hi|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3third.p3:C25Shadow.kept|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3third.p3:C25Shadow.lo|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3third.p3:C25Shadow.mixed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3third.p3:C25Shadow.sorted|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.a3third.p3:C25Shadow.total|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.base:C25Shadow.bySub|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.base:C25Shadow.hi|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.base:C25Shadow.kept|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.base:C25Shadow.lo|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.base:C25Shadow.mixed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.base:C25Shadow.sorted|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s25.base:C25Shadow.total|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o1:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o1:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o1:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o1:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o1:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o1:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o1:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o1:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o1:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o2:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o2:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o2:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o2:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o2:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o2:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o2:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o2:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o2:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o3:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o3:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o3:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o3:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o3:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o3:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o3:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o3:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o3:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o4:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o4:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o4:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o4:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o4:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o4:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o4:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o4:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a1o4:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2alias:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2alias:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2alias:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2alias:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2alias:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2alias:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2alias:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2alias:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2alias:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2dangle:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2dangle:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2dangle:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2dangle:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2dangle:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2dangle:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2dangle:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2dangle:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2dangle:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2qual:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2qual:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2qual:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2qual:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2qual:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2qual:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2qual:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2qual:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2qual:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2wild:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2wild:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2wild:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2wild:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2wild:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2wild:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2wild:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2wild:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a2wild:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3half.p1:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3half.p1:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3half.p1:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3half.p1:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3half.p1:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3half.p1:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3half.p1:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3half.p2:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3half.p2:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3hub.p2:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3hub.p2:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3hub.p2:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3hub.p2:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3hub.p2:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3hub.p2:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3hub.p2:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3hub.p2:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3hub.p2:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3third.p1:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3third.p1:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3third.p2:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3third.p2:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3third.p2:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3third.p2:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3third.p2:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3third.p2:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a3third.p2:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a4none:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a4none:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a4snap:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a4snap:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a5crlf:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a5crlf:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a5mixed:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a5mixed:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a5uni:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a5uni:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a9comment:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a9comment:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a9tabs:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.a9tabs:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.base:C26AliasArms.bare|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.base:C26AliasArms.called|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.base:C26AliasArms.navved|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.base:C26AliasArms.paramed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.base:C26AliasArms.stringed|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.base:C26Lit.flags|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.base:C26Lit.nums|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.base:C26Nested.cmp|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s26.base:C26Nested.inner|ALIAS_SIGNATURE_RAW_TYPE",
            "refused|chaos/1.1.0 chaos.s29.a1o1:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.a1o2:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.a1o3:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.a2alias:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.a2dangle:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.a2qual:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.a2wild:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.a3half.p2:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.a3hub.p2:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.a3third.p2:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.base:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE",
            "refused|chaos/1.1.0 chaos.s29.x13half.p2:C29Piped.piped|ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE"
    );

    private void mapCell(CorpusCells.CellSpec cell) throws IOException, NoSuchAlgorithmException {
        CorpusCells.LoadedCell loaded = CorpusCells.load(cell);
        RWorkspace ws = loaded.workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();
        Path goldenRoot = CorpusCells.CORPUS_ROOT
                .resolve(cell.ownDir() + "/rosetta-source/src/generated/java");

        List<Row> rows = new ArrayList<>();
        // The alias→alias reference edges (enclosing body's alias → target) that
        // carry the D1/D3 verbatim-body exclusion transitively.
        List<RShortcut[]> aliasEdges = new ArrayList<>();
        Map<RShortcut, Row> rowByShortcut = new IdentityHashMap<>();
        Map<RShortcut, RFunction> ownerByShortcut = new IdentityHashMap<>();
        Map<String, Integer> unitOrdinals = new HashMap<>();

        for (RModel model : loaded.ownModels()) {
            for (RRootElement element : model.rootElements()) {
                if (element instanceof RFunction func && !func.shortcuts().isEmpty()) {
                    mapFunction(cell, ws, adapter, model.namespace(), func, rows,
                            rowByShortcut, aliasEdges, unitOrdinals);
                    for (RShortcut alias : func.shortcuts()) {
                        ownerByShortcut.put(alias, func);
                    }
                }
            }
        }

        // -------------------------------------------------- the golden oracle --
        Map<String, List<Row>> rowsByFile = new LinkedHashMap<>();
        for (Row r : rows) {
            rowsByFile.computeIfAbsent(r.file, f -> new ArrayList<>()).add(r);
        }
        int nameMiss = 0;
        int abstractSeats = 0;
        int mapperAbstractSeats = 0;
        for (Map.Entry<String, List<Row>> e : rowsByFile.entrySet()) {
            Path golden = goldenRoot.resolve(e.getKey());
            assertTrue(Files.isRegularFile(golden), cell.label()
                    + ": every alias-bearing function must have its reference golden — missing "
                    + golden);
            List<String> lines = Files.readAllLines(golden, StandardCharsets.UTF_8);
            // Grouped by the RENDERED method name — the token the golden carries.
            Map<String, List<Row>> byName = new LinkedHashMap<>();
            for (Row r : e.getValue()) {
                byName.computeIfAbsent(r.renderedName, n -> new ArrayList<>()).add(r);
            }
            for (Map.Entry<String, List<Row>> nameGroup : byName.entrySet()) {
                Oracle o = scanGolden(lines, nameGroup.getKey());
                List<Row> members = nameGroup.getValue();
                String grain = members.size() == 1 ? "EXACT"
                        : "FILE_GROUP:" + members.size();
                for (Row r : members) {
                    r.oracleAbstract = o.abstractSeats;
                    r.oracleImpl = o.implSeats;
                    r.oracleInvocations = o.invocationSeats;
                    r.oracleGrain = grain;
                }
                if (o.abstractSeats == 0) {
                    nameMiss++;
                }
                abstractSeats += o.abstractSeats;
                mapperAbstractSeats += o.mapperS + o.mapperC;
                resolveRenderSc(members, o);
            }
        }

        // ------------------------------------------- the D1 transitive closure --
        Set<RShortcut> excluded = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map.Entry<RShortcut, Row> e : rowByShortcut.entrySet()) {
            if (e.getValue().usesOutput || e.getValue().d1Direct) {
                excluded.add(e.getKey());
            }
        }
        boolean grew = true;
        while (grew) {
            grew = false;
            for (RShortcut[] edge : aliasEdges) {
                if (excluded.contains(edge[0]) && !excluded.contains(edge[1])) {
                    excluded.add(edge[1]);
                    rowByShortcut.get(edge[1]).d1Transitive = true;
                    grew = true;
                }
            }
        }

        // ------------------------------------------------ the tranche partition --
        for (Row r : rows) {
            r.tranche = assignTranche(r);
        }

        // -- the T1∪T2∪T3∪T4 flip-policy belt (T1 PR-22 … T4 PR-25 = TOTAL) --
        // THE EMITTER-PREDICATE ≡ MAP-TRANCHE LAW: the emitter's value-seam
        // policy (AliasValueSeamPolicy — the per-alias predicate the optimised
        // route flips on) must reproduce this map's LANDED-tranche partition
        // (T1 ∪ T2 ∪ T3 ∪ T4 since PR-25 — the FULL flip population; the
        // residue is exactly the EXCLUDED_* rows) EXACTLY, per row, on every
        // cell — the census-then-EDIT tie between the receipt artifact and the
        // emitted tranche. Assertion-only: the TSV bytes above stay
        // byte-identical to the T0 receipts (`326a3b78`/`a1dc5cc3`/…), so the
        // instrument itself is provably undrifted while gaining the belt.
        com.regnosys.rosetta.generator.java.GeneratorModel gm =
                new com.regnosys.rosetta.generator.java.GeneratorModel(ws,
                        loaded.ownModelSet()::contains);
        com.regnosys.rosetta.generator.java.types.JavaTypeUtil typeUtil =
                new com.regnosys.rosetta.generator.java.types.JavaTypeUtil();
        AliasValueSeamPolicy policy = new AliasValueSeamPolicy(gm,
                new com.regnosys.rosetta.generator.java.types.JavaTypeTranslator(typeUtil),
                typeUtil, new NavigationChainClassifier());
        // v3.2 PR-2: the chaos cell runs the four policy≡map laws in COLLECT mode —
        // every violation becomes a law-tagged grain, and the collected SET must equal
        // the DECLARED set exactly (frozen-exception semantics, the
        // ResolutionConformance pattern: an undeclared violation fails, a declared one
        // that stops violating fails as STALE). The declared grains WERE the census's
        // F11 (s18 — the type-keyed-switch alias whose render seat refused; healed at
        // v3.2 seat 7, #628) and F4 (s05 — the lambda-type-join members; healed at v3.2
        // seat 2, #623) — see CHAOS_DECLARED_ALIAS_LAW_EXCEPTIONS' javadoc; the set is
        // EMPTY since seat 7 and kept as the belt. Every vendored cell keeps the hard
        // per-row asserts.
        boolean chaosCell = cell.label().startsWith("chaos/");
        java.util.Set<String> chaosViolations = new java.util.TreeSet<>();
        for (Map.Entry<RShortcut, Row> e : rowByShortcut.entrySet()) {
            Row r = e.getValue();
            String grain = cell.label() + " " + r.namespace + ":" + r.function
                    + "." + r.alias;
            AliasValueSeamPolicy.FlipFacts facts;
            try {
                facts = policy.flipFactsOrNull(ownerByShortcut.get(e.getKey()), e.getKey());
            } catch (com.regnosys.rosetta.generator.java.SilentDegradation.Refusal refusal) {
                // v3.2 seat 12 (D52): a refusal raised at the alias SIGNATURE (ALIAS_SIGNATURE_RAW_TYPE - the walk
                // declined and the function output's raw rune name is a builtin) reaches this instrument directly,
                // because the policy computes the seam without the generator's per-element boundary. On the chaos
                // cell it is a law-tagged grain of the collected set - declared FROM THE PRINT, exact both ways like
                // the four laws' grains; on a vendored cell it propagates: a refusal there is a regression to
                // investigate (0 predicted at every site; the rings decide), never to absorb.
                if (!chaosCell) {
                    throw refusal;
                }
                chaosViolations.add("refused|" + grain + "|" + refusal.site());
                continue;
            }
            boolean policyFlips = facts != null;
            boolean trancheAgrees = ("T1".equals(r.tranche) || "T2".equals(r.tranche)
                    || "T3".equals(r.tranche) || "T4".equals(r.tranche)) == policyFlips;
            if (chaosCell && !trancheAgrees) {
                chaosViolations.add("tranche|" + grain + "|map=" + r.tranche
                        + " policyFlips=" + policyFlips);
            } else {
                assertEquals("T1".equals(r.tranche) || "T2".equals(r.tranche)
                        || "T3".equals(r.tranche) || "T4".equals(r.tranche), policyFlips,
                        grain
                        + ": the emitter's flip policy must reproduce the map's"
                        + " T1∪T2∪T3∪T4 partition exactly (map tranche=" + r.tranche
                        + ") — decode the diverging leg BEFORE any tranche emission"
                        + " is trusted");
            }
            if (facts != null) {
                // The per-row fact cross-checks: the policy's form discriminator
                // must agree with the map's whole-chain channel (a classified
                // whole-body chain ⟺ the ladder plan — the SAME shared
                // classifier both sides ride, so T1 and the T4 whole-chain
                // careful rows carry plans while every value-body row carries
                // the adapted top kind instead), and the S/C bit with the map's
                // RENDER channel (the § 3 authority — at T4 the D5 rows land
                // exactly here, the render-authority receipt per member).
                boolean planAgrees = (!"-".equals(r.wholeChain)) == (facts.plan() != null);
                boolean topKindPresenceAgrees =
                        ("-".equals(r.wholeChain)) == (facts.valueBodyTopKind() != null);
                boolean topKindAgrees = facts.valueBodyTopKind() == null
                        || r.bodyTopKind.equals(facts.valueBodyTopKind().name());
                boolean scAgrees = "C".equals(r.renderSc) == facts.isMulti();
                if (chaosCell && !(planAgrees && topKindPresenceAgrees && topKindAgrees
                        && scAgrees)) {
                    if (!planAgrees)            chaosViolations.add("plan|" + grain);
                    if (!topKindPresenceAgrees) chaosViolations.add("topkind-presence|" + grain);
                    if (!topKindAgrees)         chaosViolations.add("topkind|" + grain
                            + "|map=" + r.bodyTopKind
                            + " policy=" + facts.valueBodyTopKind().name());
                    if (!scAgrees)              chaosViolations.add("sc|" + grain);
                } else {
                    assertEquals(!"-".equals(r.wholeChain), facts.plan() != null, grain
                            + ": whole-chain rows carry the ladder plan; value-body"
                            + " rows carry none");
                    assertEquals("-".equals(r.wholeChain),
                            facts.valueBodyTopKind() != null,
                            grain + ": value-body rows carry the adapted body top kind");
                    if (facts.valueBodyTopKind() != null) {
                        assertEquals(r.bodyTopKind, facts.valueBodyTopKind().name(), grain
                                + ": the policy's value-body top kind must be the map's"
                                + " bodyTopKind");
                    }
                    assertEquals("C".equals(r.renderSc), facts.isMulti(), grain
                            + ": the policy's S/C bit must be the map's render channel");
                }
            }
        }
        if (chaosCell) {
            assertEquals(CHAOS_DECLARED_ALIAS_LAW_EXCEPTIONS, chaosViolations,
                    "the chaos alias policy≡map violations must equal the DECLARED set"
                            + " exactly — an extra row is a NEW finding (census it, never"
                            + " append silently); a missing row is a STALE declaration"
                            + " (delete it in the seat that healed it; shrink-only)");
        }

        writeAndPrint(cell, rows, nameMiss, abstractSeats, mapperAbstractSeats);
    }

    private void mapFunction(CorpusCells.CellSpec cell, RWorkspace ws,
            ExpressionToIRAdapter adapter, String namespace, RFunction func, List<Row> rows,
            Map<RShortcut, Row> rowByShortcut, List<RShortcut[]> aliasEdges,
            Map<String, Integer> unitOrdinals) {
        Set<String> usesOutputNames = new LinkedHashSet<>();
        for (FunctionTemplateModel.AliasModel model : aliasHelper.analyze(func)) {
            if (model.getUsesOutput()) {
                usesOutputNames.add(model.getName());
            }
        }
        String classGrain = namespace + ":" + func.name();
        int unit = unitOrdinals.merge(classGrain, 1, Integer::sum);
        // The render's own dependency-vs-alias collision law (the decl-seat
        // authority): a colliding alias renders as `name1` (the dependency field
        // takes `name0`) — the oracle must scan for the RENDERED name.
        Set<String> collidingNames =
                FunctionDependencyCollector.collidingDependencyAliasNames(func);

        Map<String, Row> rowByName = new LinkedHashMap<>();
        for (RShortcut alias : func.shortcuts()) {
            Row r = new Row();
            r.cell = cell.label();
            r.namespace = namespace;
            r.function = func.name();
            func.dispatch().ifPresent(d -> r.dispatchValue = d.valueName());
            r.unitOrdinal = unit;
            r.alias = alias.name();
            r.renderedName = collidingNames.contains(alias.name())
                    ? alias.name() + "1" : alias.name();
            r.file = namespace.replace('.', '/') + "/functions/" + func.name() + ".java";
            r.usesOutput = usesOutputNames.contains(alias.name());
            Optional<IRExpr> ir = alias.expression() == null ? Optional.empty()
                    : adapter.adapt(alias.expression(), ws);
            r.bodyTopKind = ir.map(e -> e.kind().name()).orElse("adaptEmpty");
            r.irSc = r.usesOutput ? "usesOutput"
                    : ir.isEmpty() ? "adaptEmpty"
                            : ir.get().cardinality() == ExpressionCardinality.MULTI ? "C" : "S";
            if (alias.expression() != null) {
                classifier.classify(alias.expression(), ws, func).ifPresent(p ->
                        r.wholeChain = p.rootKind() + ":" + p.shape()
                                + (p.bearsMeta() ? ":meta" : ""));
            }
            rows.add(r);
            rowByShortcut.put(alias, r);
            rowByName.put(alias.name(), r);
        }

        for (ROperation op : func.operations()) {
            boundWalk(op.expression(), op.expression(), "operation", func, null,
                    rowByShortcut, aliasEdges);
            irWalk(adapter, ws, op.expression(), "operation", rowByName);
        }
        for (RShortcut alias : func.shortcuts()) {
            boundWalk(alias.expression(), alias.expression(), "alias", func, alias,
                    rowByShortcut, aliasEdges);
            irWalk(adapter, ws, alias.expression(), "alias", rowByName);
        }
        for (RCondition cond : func.conditions()) {
            boundWalk(cond.expression(), cond.expression(), "condition", func, null,
                    rowByShortcut, aliasEdges);
            irWalk(adapter, ws, cond.expression(), "condition", rowByName);
        }
        for (RPostCondition post : func.postConditions()) {
            boundWalk(post.expression(), post.expression(), "postCondition", func, null,
                    rowByShortcut, aliasEdges);
            irWalk(adapter, ws, post.expression(), "postCondition", rowByName);
        }
    }

    /**
     * The AST (bound) channel per member — the SAME consumer classifier as the
     * census ({@link NavigationChainCensusTest#classifyAliasConsumer}); when the
     * walked body is itself an alias body, the enclosing→target edge feeds the
     * D1 transitive closure.
     */
    private void boundWalk(RNode node, RExpression bodyRoot, String context, RFunction func,
            RShortcut enclosingAlias, Map<RShortcut, Row> rowByShortcut,
            List<RShortcut[]> aliasEdges) {
        if (node == null) {
            return;
        }
        if (node instanceof RSymbolReference ref) {
            RShortcut target = ref.symbol().filter(RShortcut.class::isInstance)
                    .map(RShortcut.class::cast).orElse(null);
            if (target != null && rowByShortcut.containsKey(target)) {
                Row r = rowByShortcut.get(target);
                String consumer = NavigationChainCensusTest.classifyAliasConsumer(ref, bodyRoot);
                r.boundConsumers.merge(consumer, 1, Integer::sum);
                r.boundContexts.merge(context, 1, Integer::sum);
                if ("ONLY_EXISTS_ELEMENT".equals(consumer)) {
                    r.d1Direct = true;
                }
                if (enclosingAlias != null) {
                    aliasEdges.add(new RShortcut[] {enclosingAlias, target});
                }
            }
        }
        for (RNode child : node.children()) {
            boundWalk(child, bodyRoot, context, func, enclosingAlias, rowByShortcut, aliasEdges);
        }
    }

    /** The IR (spelling-normalized) channel per member — name-keyed, the adapter's own key. */
    private void irWalk(ExpressionToIRAdapter adapter, RWorkspace ws, RExpression body,
            String context, Map<String, Row> rowByName) {
        if (body == null) {
            return;
        }
        adapter.adapt(body, ws).ifPresent(ir -> irNodeWalk(ir, "BODY_ROOT", context, rowByName));
    }

    private void irNodeWalk(IRExpr node, String consumerLabel, String context,
            Map<String, Row> rowByName) {
        if (node instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ALIAS
                && rowByName.containsKey(ref.target())) {
            Row r = rowByName.get(ref.target());
            r.irConsumers.merge(consumerLabel, 1, Integer::sum);
            r.irContexts.merge(context, 1, Integer::sum);
        }
        if (NavigationChainCensusTest.CHAIN_LINK_KINDS.contains(node.kind())) {
            IRExpr cursor = node;
            while (NavigationChainCensusTest.CHAIN_LINK_KINDS.contains(cursor.kind())) {
                assertEquals(1, cursor.children().size(),
                        cursor.kind() + ": a chain-link kind must carry exactly one"
                                + " (receiver) child — the map's IR walk cannot descend"
                                + " this shape");
                cursor = cursor.children().get(0);
            }
            irNodeWalk(cursor, "CHAIN_ROOT", context, rowByName);
            return;
        }
        for (IRExpr child : node.children()) {
            irNodeWalk(child, node.kind().name(), context, rowByName);
        }
    }

    // ------------------------------------------------------------------ the oracle scan --

    private static final class Oracle {
        int abstractSeats;
        int implSeats;
        int invocationSeats;
        int mapperS;
        int mapperC;
        int otherTyped;
    }

    /**
     * Fixed-token scan of one golden for one alias name (the class javadoc's
     * occurrence + line rules). Also reads the render-channel S/C bit off the
     * abstract seam lines ({@code MapperS<} / {@code MapperC<} — the § 19a
     * golden-token identity's per-member form).
     */
    private static Oracle scanGolden(List<String> lines, String aliasName) {
        Oracle o = new Oracle();
        String token = aliasName + "(";
        for (String line : lines) {
            int occurrences = occurrenceCount(line, token);
            if (occurrences == 0) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.startsWith("protected abstract ")) {
                o.abstractSeats += occurrences;
                if (trimmed.contains("MapperC<")) {
                    o.mapperC += occurrences;
                } else if (trimmed.contains("MapperS<")) {
                    o.mapperS += occurrences;
                } else {
                    o.otherTyped += occurrences;
                }
            } else if (trimmed.startsWith("protected ") || trimmed.startsWith("public ")
                    || trimmed.startsWith("private ")) {
                o.implSeats += occurrences;
            } else {
                o.invocationSeats += occurrences;
            }
        }
        return o;
    }

    /** Occurrences of {@code token} whose preceding char is neither an identifier part nor '.'. */
    private static int occurrenceCount(String line, String token) {
        int count = 0;
        int idx = line.indexOf(token);
        while (idx >= 0) {
            if (idx == 0 || (!Character.isJavaIdentifierPart(line.charAt(idx - 1))
                    && line.charAt(idx - 1) != '.')) {
                count++;
            }
            idx = line.indexOf(token, idx + 1);
        }
        return count;
    }

    /**
     * The render-channel S/C bit per member from the name-group's abstract-seat
     * multiset: EXACT when the group is single or uniform; otherwise the group
     * multiset is recorded on every member and, when it disagrees with the IR
     * multiset, flagged {@code D5_GROUP} (per-unit attribution = T4's
     * per-member reconciliation receipt). A resolved bit disagreeing with the
     * row's IR bit is {@code D5_EXACT} — the § 19a 27-seam class, now named.
     */
    private static void resolveRenderSc(List<Row> members, Oracle o) {
        boolean uniform = o.mapperS == 0 || o.mapperC == 0;
        if (members.size() == 1 || uniform) {
            String bit = o.mapperC > 0 ? "C" : o.mapperS > 0 ? "S"
                    : o.otherTyped > 0 ? "nonMapper" : "absent";
            for (Row r : members) {
                r.renderSc = bit;
                if (!r.usesOutput && ("S".equals(bit) || "C".equals(bit))
                        && !bit.equals(r.irSc)) {
                    r.d5 = "D5_EXACT";
                }
            }
            return;
        }
        String groupBits = "S=" + o.mapperS + ",C=" + o.mapperC
                + (o.otherTyped > 0 ? ",other=" + o.otherTyped : "");
        int irS = 0;
        int irC = 0;
        for (Row r : members) {
            if ("S".equals(r.irSc)) {
                irS++;
            } else if ("C".equals(r.irSc)) {
                irC++;
            }
        }
        boolean multisetsAgree = irS == o.mapperS && irC == o.mapperC;
        for (Row r : members) {
            r.renderSc = "GROUP[" + groupBits + "]";
            if (!multisetsAgree && !r.usesOutput) {
                r.d5 = "D5_GROUP";
            }
        }
    }

    // ------------------------------------------------------------- tranche + receipts --

    /**
     * The machine tranche partition (plan § 8; precedence top-down — every row
     * exactly one label): D3/D1/D1T excluded; dispatch case units, D5 rows and
     * oracle-LIVE D4 rows (invocation seats under zero channel visibility = the
     * disguised class) are T4 careful-class; then body kind — whole-chain T1,
     * chain-headed/APPLY T2, CONDITIONAL + tail + adaptEmpty T3. A D4 row whose
     * oracle count is ZERO invocation seats rides its body-kind tranche: the
     * oracle receipt IS the enumeration D4 demands, and it enumerates nothing to
     * cover. D2 never moves a row — it mandates the § 14f capture in whichever
     * tranche the row rides.
     */
    private static String assignTranche(Row r) {
        if (r.usesOutput) {
            return "EXCLUDED_D3";
        }
        if (r.d1Direct) {
            return "EXCLUDED_D1";
        }
        if (r.d1Transitive) {
            return "EXCLUDED_D1T";
        }
        if (!"-".equals(r.dispatchValue)) {
            return "T4";
        }
        if (!"-".equals(r.d5)) {
            return "T4";
        }
        if (r.d4ZeroVisible() && r.oracleInvocations > 0) {
            return "T4";
        }
        if (!"-".equals(r.wholeChain)) {
            return "T1";
        }
        IRExprKind kind = safeKind(r.bodyTopKind);
        if (kind != null && (CHAIN_HEAD_KINDS.contains(kind) || kind == IRExprKind.APPLY)) {
            return "T2";
        }
        return "T3";
    }

    private static IRExprKind safeKind(String name) {
        try {
            return IRExprKind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null; // adaptEmpty — the T3 tail by fall-through
        }
    }

    private void writeAndPrint(CorpusCells.CellSpec cell, List<Row> rows, int nameMiss,
            int abstractSeats, int mapperAbstractSeats)
            throws IOException, NoSuchAlgorithmException {
        String head = "ALIASMAP " + cell.label() + " ";
        int usesOutput = 0;
        int d1Direct = 0;
        int d1Transitive = 0;
        int d2Rows = 0;
        int boundCondSeats = 0;
        int irCondSeats = 0;
        int d4BoundZero = 0;
        int d4IrZero = 0;
        int d4BothZero = 0;
        int d4Live = 0;
        int d4Uninvoked = 0;
        int d5Exact = 0;
        int d5Group = 0;
        int onlyExistsSeats = 0;
        int boundRefs = 0;
        int irRefs = 0;
        int invocationSeats = 0;
        TreeMap<String, Integer> byTranche = new TreeMap<>();
        TreeMap<String, Integer> byTopKind = new TreeMap<>();
        int wholeChain = 0;
        int mangledNames = 0;
        int renderS = 0;
        int renderC = 0;
        int d5SToC = 0;
        int d5CToS = 0;
        Set<String> files = new LinkedHashSet<>();
        Set<String> seamFiles = new LinkedHashSet<>();
        Set<String> units = new LinkedHashSet<>();
        Set<String> seamUnits = new LinkedHashSet<>();

        StringBuilder tsv = new StringBuilder();
        tsv.append("cell\tnamespace\tfunction\tdispatch\tunit\talias\trenderedName\tfile\t"
                + "usesOutput\tirSC\trenderSC\td5\tbodyTopKind\twholeChain\tboundTotal\t"
                + "boundConsumers\tboundContexts\tirTotal\tirConsumers\tirContexts\t"
                + "onlyExistsSeats\tcondCtxSeatsBound\tcondCtxSeatsIr\td1\td2\td4\t"
                + "oracleAbstract\toracleImpl\toracleInvocations\toracleGrain\ttranche\n");
        for (Row r : rows) {
            // Per-row conservation: every seat classifies exactly once per family.
            assertEquals(r.boundTotal(),
                    r.boundContexts.values().stream().mapToInt(Integer::intValue).sum(),
                    cell.label() + " " + r.alias + ": bound consumer/context conservation");
            assertEquals(r.irTotal(),
                    r.irContexts.values().stream().mapToInt(Integer::intValue).sum(),
                    cell.label() + " " + r.alias + ": IR consumer/context conservation");

            usesOutput += r.usesOutput ? 1 : 0;
            d1Direct += r.d1Direct ? 1 : 0;
            d1Transitive += r.d1Transitive ? 1 : 0;
            boolean d2 = r.boundConditionSeats() + r.irConditionSeats() > 0;
            d2Rows += d2 ? 1 : 0;
            boundCondSeats += r.boundConditionSeats();
            irCondSeats += r.irConditionSeats();
            d4BoundZero += r.boundTotal() == 0 ? 1 : 0;
            d4IrZero += r.irTotal() == 0 ? 1 : 0;
            if (r.d4ZeroVisible()) {
                d4BothZero++;
                if (r.oracleInvocations > 0) {
                    d4Live++;
                } else {
                    d4Uninvoked++;
                }
            }
            d5Exact += "D5_EXACT".equals(r.d5) ? 1 : 0;
            d5Group += "D5_GROUP".equals(r.d5) ? 1 : 0;
            onlyExistsSeats += r.onlyExistsSeats();
            boundRefs += r.boundTotal();
            irRefs += r.irTotal();
            invocationSeats += r.oracleInvocations;
            byTranche.merge(r.tranche, 1, Integer::sum);
            byTopKind.merge(r.bodyTopKind, 1, Integer::sum);
            wholeChain += "-".equals(r.wholeChain) ? 0 : 1;
            mangledNames += r.alias.equals(r.renderedName) ? 0 : 1;
            renderS += "S".equals(r.renderSc) ? 1 : 0;
            renderC += "C".equals(r.renderSc) ? 1 : 0;
            if ("D5_EXACT".equals(r.d5)) {
                d5SToC += "S".equals(r.irSc) ? 1 : 0;
                d5CToS += "C".equals(r.irSc) ? 1 : 0;
            }
            files.add(r.file);
            units.add(r.namespace + ":" + r.function + "#" + r.unitOrdinal);
            if (!r.usesOutput) {
                seamFiles.add(r.file);
                seamUnits.add(r.namespace + ":" + r.function + "#" + r.unitOrdinal);
            }

            tsv.append(r.cell).append('\t').append(r.namespace).append('\t')
                    .append(r.function).append('\t').append(r.dispatchValue).append('\t')
                    .append(r.unitOrdinal).append('\t').append(r.alias).append('\t')
                    .append(r.renderedName).append('\t')
                    .append(r.file).append('\t').append(r.usesOutput).append('\t')
                    .append(r.irSc).append('\t').append(r.renderSc).append('\t')
                    .append(r.d5).append('\t').append(r.bodyTopKind).append('\t')
                    .append(r.wholeChain).append('\t').append(r.boundTotal()).append('\t')
                    .append(renderMap(r.boundConsumers)).append('\t')
                    .append(renderMap(r.boundContexts)).append('\t')
                    .append(r.irTotal()).append('\t')
                    .append(renderMap(r.irConsumers)).append('\t')
                    .append(renderMap(r.irContexts)).append('\t')
                    .append(r.onlyExistsSeats()).append('\t')
                    .append(r.boundConditionSeats()).append('\t')
                    .append(r.irConditionSeats()).append('\t')
                    .append(r.d1Direct ? "D1" : r.d1Transitive ? "D1T" : "-").append('\t')
                    .append(d2 ? "D2" : "-").append('\t')
                    .append(r.d4ZeroVisible()
                            ? (r.oracleInvocations > 0 ? "D4_LIVE" : "D4_UNINVOKED") : "-")
                    .append('\t')
                    .append(r.oracleAbstract).append('\t').append(r.oracleImpl).append('\t')
                    .append(r.oracleInvocations).append('\t').append(r.oracleGrain).append('\t')
                    .append(r.tranche).append('\n');
        }

        String leaf = cell.ownDir().substring(cell.ownDir().lastIndexOf('/') + 1);
        Path out = MAP_DIR.resolve(leaf + ".tsv");
        byte[] bytes = tsv.toString().getBytes(StandardCharsets.UTF_8);
        Files.write(out, bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) {
            hex.append(String.format("%02x", b));
        }

        System.out.println(head + "rows=" + rows.size() + " usesOutput=" + usesOutput
                + " files=" + files.size() + " seamFiles=" + seamFiles.size()
                + " units=" + units.size() + " seamUnits=" + seamUnits.size()
                + " boundRefs=" + boundRefs + " irRefs=" + irRefs);
        System.out.println(head + "bodyTopKind: " + renderMap(byTopKind));
        System.out.println(head + "wholeChain=" + wholeChain);
        System.out.println(head + "d1: directAliases=" + d1Direct + " transitiveAliases="
                + d1Transitive + " onlyExistsSeats=" + onlyExistsSeats);
        System.out.println(head + "d2: rows=" + d2Rows + " boundCondSeats=" + boundCondSeats
                + " irCondSeats=" + irCondSeats);
        System.out.println(head + "d4: boundZero=" + d4BoundZero + " irZero=" + d4IrZero
                + " bothZero=" + d4BothZero + " oracleLive=" + d4Live + " oracleUninvoked="
                + d4Uninvoked);
        System.out.println(head + "d5: exact=" + d5Exact + " group=" + d5Group
                + " sToC=" + d5SToC + " cToS=" + d5CToS
                + " renderS=" + renderS + " renderC=" + renderC);
        System.out.println(head + "oracle: abstractSeats=" + abstractSeats
                + " mapperAbstractSeats=" + mapperAbstractSeats + " invocationSeats="
                + invocationSeats + " nameMiss=" + nameMiss + " mangledNames=" + mangledNames);
        System.out.println(head + "tranche: " + renderMap(byTranche));
        // Forward-slash the printed path so the receipt line is OS-stable (the
        // TSV content is separator-clean by construction; Seat-1 #557 N2).
        System.out.println(head + "tsv=" + out.toString().replace('\\', '/')
                + " bytes=" + bytes.length + " sha256=" + hex);

        // -------------------------------------------------------------- the pins --
        // (channel-agreement + conservation only; distributions stay RECORDED)
        assertEquals(0, nameMiss, cell.label() + ": every alias name must land on at least"
                + " one abstract seam line of its golden (a miss = name mangling or a"
                + " layout drift — the oracle cannot be trusted until decoded)");
        assertEquals(rows.size(), abstractSeats, cell.label() + ": the golden abstract-seat"
                + " total must equal the alias declaration population (each declaration"
                + " emits exactly one protected-abstract seam; a drift = token collision"
                + " or corpus drift — reopen census § 19a before trusting the map)");
        assertEquals(rows.size() - usesOutput, mapperAbstractSeats, cell.label()
                + ": the Mapper-typed abstract-seat total must equal the non-usesOutput"
                + " seam population (the § 19a golden-token identity, per-member form)");
        int trancheTotal = byTranche.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(rows.size(), trancheTotal, cell.label()
                + ": the tranche partition must conserve to the declaration population");
        if (WITNESS_CELL.equals(cell.label())) {
            Row witness = rows.stream()
                    .filter(r -> WITNESS_NAMESPACE.equals(r.namespace)
                            && WITNESS_FUNCTION.equals(r.function)
                            && WITNESS_ALIAS.equals(r.alias))
                    .findFirst().orElse(null);
            assertTrue(witness != null, cell.label() + ": the § 19c disguised-spelling"
                    + " witness alias must exist (corpus drift reopens census § 19c)");
            assertEquals(WITNESS_INVOCATION_SEATS, witness.oracleInvocations,
                    cell.label() + " " + WITNESS_FUNCTION + "." + WITNESS_ALIAS
                            + ": the positive-control oracle count (census § 19c: six"
                            + " disguised invocations at zero channel visibility) — a"
                            + " drift means the oracle's line/occurrence rules broke or"
                            + " the corpus moved; reopen census § 19c");
            assertTrue(witness.d4ZeroVisible(), cell.label()
                    + ": the witness must count zero on BOTH channels (law 48's shape)");
        }
        if (cell.label().startsWith("cdm") || cell.label().startsWith("drr")) {
            assertTrue(rows.size() > 0,
                    cell.label() + ": the cell is known alias-bearing (census § 19a)");
        }
    }

    private static String renderMap(Map<String, Integer> map) {
        if (map.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(k).append('=').append(v);
        });
        return sb.toString();
    }
}
