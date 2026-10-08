package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * IR-Lab leaf-vs-cascade measurement (relayed via Nick, 2026-06-25) — read-only,
 * 0 byte flips, NOT a parity gate. Reproduces the Lab's MISSING-base classification
 * recipe over the FUNCTION + RULE seam to size the symbol-ref leaf-typing slice
 * (PART B): for each {@link RFeatureCall} whose {@code getInferredType(receiver)} is
 * MISSING, walk {@code receiver.receiver()} through the RFeatureCall hops to the chain
 * BASE, classify the base AST kind (symbol-ref / non-symbol / implicit-item), and
 * sub-classify the symbol-ref bucket by the resolved symbol kind (function-call =
 * case (a) the genuine leaf / shortcut-alias = cascade / param / enum / type-root /
 * unresolved). The genuine-leaf fraction of the symbol-ref bucket is the real ceiling.
 *
 * <p>Run targeted: {@code mvn -f rune-parser/pom.xml test -Dtest=IrLabMissingBaseCensusTest}.
 * Prints a report to stdout; asserts nothing (a census). Counts may differ from the
 * Lab's (full-corpus vs their seam scoping) — the RATIO is the deliverable.
 */
class IrLabMissingBaseCensusTest {

    private static final Path CDM5 = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM6 = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path DRR = Path.of("../test-corpus/drr/drr-6.34.1");

    /** Builtin registry for replicating {@code inferTypeOfAttribute}'s builtin fallback. */
    private static final BuiltinTypeRegistry BUILTINS = BuiltinTypeRegistry.createDefault();

    private enum Seat { FUNCTION, RULE }

    @Test
    void censusMissingBaseKinds() {
        assumeTrue(Files.isDirectory(CDM5) && Files.isDirectory(DRR),
                "test-corpus not available");

        // RWorkspace.build freezes nodes per-instance, so build each workspace EXACTLY
        // once and measure both seats off that one build (a re-build mutates frozen nodes).
        // CRITICAL: use the workspace built here (which RAN the fixed-point engine) to read
        // REAL inferred types — a fresh un-run TypeInferenceEngine has an empty type map, so
        // its getInferredType returns MISSING for everything (the MISSING gate becomes a no-op
        // counting ALL feature-calls). Node-level resolution fields (resolvedSymbol /
        // resolvedRestrictionType / resolvedChoiceOption / referencedType) ARE set by build
        // regardless, but the inferred-TYPE map is per-engine.
        Map<String, CellTally> cells = new LinkedHashMap<>();

        List<RModel> cdm5 = load(CDM5);
        RWorkspace cdm5Ws = RWorkspace.build(cdm5).workspace();
        cells.put("cdm5 FUNC", measureSeat(cdm5, Seat.FUNCTION, cdm5Ws));

        List<RModel> cdm6 = load(CDM6);
        RWorkspace cdm6Ws = RWorkspace.build(cdm6).workspace();
        cells.put("cdm6 FUNC", measureSeat(cdm6, Seat.FUNCTION, cdm6Ws));

        // drr needs its transitive CDM (cdm-5.38.0) in the same workspace to resolve;
        // a FRESH cdm5 copy (independent of the cdm5-alone build's frozen nodes).
        List<RModel> drr = load(DRR);
        List<RModel> drrWs = new ArrayList<>(drr);
        drrWs.addAll(load(CDM5));
        RWorkspace drrWorkspace = RWorkspace.build(drrWs).workspace();
        cells.put("drr FUNC", measureSeat(drr, Seat.FUNCTION, drrWorkspace));
        cells.put("drr RULE", measureSeat(drr, Seat.RULE, drrWorkspace));

        report(cells);
    }

    /** Per-cell counts: base AST kind + the symbol-ref sub-classification. */
    private static final class CellTally {
        long symbolRef, nonSymbol, implicitItem;
        final Map<String, Long> symbolKind = new TreeMap<>();
        final Map<String, Long> baseClass = new TreeMap<>();   // raw AST class of every base
        // case-(a) characterization: WHY is each RFunction-leaf's output MISSING?
        final Map<String, Long> rfnReason = new TreeMap<>();
        final Map<String, List<String>> rfnSamples = new TreeMap<>();   // <=3 fn names per reason
        final Map<String, Long> rfnUnresolvedTypeName = new TreeMap<>(); // ref-absent typeNames
        long total() { return symbolRef + nonSymbol + implicitItem; }
    }

    /**
     * Counts the cell's own models for one seat. The workspace is already resolved
     * (RWorkspace.build called once by the caller).
     *
     * @param cellModels the subset whose functions/rules are counted (this cell only)
     */
    private CellTally measureSeat(List<RModel> cellModels, Seat seat, RWorkspace ws) {
        CellTally t = new CellTally();
        for (RModel model : cellModels) {
            for (RRootElement root : model.rootElements()) {
                boolean isFn = root instanceof RFunction;
                boolean isRule = root instanceof RRule;
                if (seat == Seat.FUNCTION && !isFn) continue;
                if (seat == Seat.RULE && !isRule) continue;
                AstWalker.walk(root, node -> classifyNode(node, ws, t));
            }
        }
        return t;
    }

    private void classifyNode(RNode node, RWorkspace ws, CellTally t) {
        if (!(node instanceof RFeatureCall fc)) return;
        RExpression receiver = fc.receiver();
        if (receiver == null) return;
        if (!missing(ws, receiver)) return;           // gate: genuinely-MISSING-typed receiver
        RExpression base = walkToBase(receiver);
        t.baseClass.merge(base == null ? "<null>" : base.getClass().getSimpleName(), 1L, Long::sum);
        if (base instanceof RSymbolReference sr) {
            t.symbolRef++;
            RNode sym = sr.symbol().orElse(null);
            String kind = sym == null ? "<unresolved>" : sym.getClass().getSimpleName();
            t.symbolKind.merge(kind, 1L, Long::sum);
            if (sym instanceof RFunction fn) tallyRfnReason(fn, t);
        } else if (base instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            // Disguised 2-name `head -> feature` chain (NOT a real enum value) — the Lab folds
            // these into the symbol-ref bucket. Sub-classify by the head's resolved symbol:
            // a func/rule head is a case-(a) function-call leaf; no resolvedSymbol = the head is
            // an attribute/param/type-restriction (a cascade/nav, not a typing leaf).
            t.symbolRef++;
            RNode sym = evr.resolvedSymbol().orElse(null);
            String kind = sym == null ? disguisedNavKind(evr) : sym.getClass().getSimpleName();
            t.symbolKind.merge(kind, 1L, Long::sum);
            if (sym instanceof RFunction fn) tallyRfnReason(fn, t);
        } else if (isImplicitVariable(base)) {
            t.implicitItem++;
        } else {
            t.nonSymbol++;     // real enum value, list-op, deep-feature, filter, arith, etc.
        }
    }

    /**
     * Sub-classifies a disguised {@code head -> feature} {@link REnumValueRef}
     * whose {@code resolvedSymbol()} is empty (head is an attribute / type-
     * restriction, NOT a func/rule leaf). The Lab's disambiguation: is the 67%
     * disguised-attribute bucket the gm-aware / emit-time family (choice-
     * supertype downcast — the L-029 split, their Q#4) or an engine-side cascade?
     * <ul>
     *   <li><b>restriction(downcast)</b> — {@code resolvedRestrictionType} or the
     *       verified {@code resolvedTypeRestriction} present: a {@code payout ->
     *       OptionPayout} downcast. gm-aware / emit-time (Lab's side).</li>
     *   <li><b>choiceOption</b> — {@code resolvedChoiceOption} present: a choice-
     *       option-by-type-name nav. gm-aware / emit-time (Lab's side).</li>
     *   <li><b>plain(attr-&gt;feature)</b> — none of the above: head attribute's
     *       OWN type is unresolved, a deeper cascade (potentially engine-side).</li>
     * </ul>
     */
    private static String disguisedNavKind(REnumValueRef evr) {
        if (evr.resolvedRestrictionType().isPresent() || evr.resolvedTypeRestriction().isPresent()) {
            return "disguisedNav-restriction(downcast,gm-aware)";
        }
        if (evr.resolvedChoiceOption().isPresent()) {
            return "disguisedNav-choiceOption(gm-aware)";
        }
        return "disguisedNav-plain(attr->feature,cascade)";
    }

    /**
     * Case-(a) characterization: WHY does {@code inferTypeOfAttribute(fn.output())}
     * (the engine's function-call output typing) return MISSING for this leaf? Keyed
     * by {@code <origin>|<reason>} where origin ∈ {FUNCTION, RULE, REPORT}:
     * <ul>
     *   <li>{@code no-output} — function has no declared output attribute;</li>
     *   <li>{@code tc-null} — output has no typeCall (rule/report output back-filled
     *       at codegen — the {@code inferAttributeRefType} MISSING shape);</li>
     *   <li>{@code ref=<Class>} — output typeCall RESOLVES to an AST node of that
     *       class (RDataType/REnumeration shouldn't be MISSING; RChoiceType/
     *       RTypeAlias/builtin-node are the astNodeToRType gaps);</li>
     *   <li>{@code ref-absent,builtin:X} — typeCall unresolved but typeName is a
     *       builtin (would resolve via the fallback — should be ~0);</li>
     *   <li>{@code ref-absent,nonbuiltin} — typeCall unresolved, not a builtin (a
     *       cross-namespace / cascade gap; the typeName is tallied separately).</li>
     * </ul>
     */
    private static void tallyRfnReason(RFunction fn, CellTally t) {
        String reason = rfnOutputMissReason(fn, t);
        t.rfnReason.merge(reason, 1L, Long::sum);
        List<String> samples = t.rfnSamples.computeIfAbsent(reason, k -> new ArrayList<>());
        if (samples.size() < 3 && !samples.contains(fn.name())) samples.add(fn.name());
    }

    private static String rfnOutputMissReason(RFunction fn, CellTally t) {
        String origin = fn.origin().name();
        Optional<RAttribute> out = fn.output();
        if (out.isEmpty()) return origin + "|no-output";
        RTypeCall tc = out.get().typeCall();
        if (tc == null) return origin + "|tc-null";
        Optional<RNode> ref;
        try { ref = tc.referencedType(); }
        catch (RuntimeException ex) { return origin + "|ref-throws"; }
        if (ref.isPresent()) {
            return origin + "|ref=" + ref.get().getClass().getSimpleName();
        }
        String tn = tc.typeName();
        boolean builtin = tn != null && BUILTINS.lookup(tn).isPresent();
        if (builtin) return origin + "|ref-absent,builtin:" + tn;
        if (tn != null) t.rfnUnresolvedTypeName.merge(tn, 1L, Long::sum);
        return origin + "|ref-absent,nonbuiltin";
    }

    /** Walk receiver.receiver() through the RFeatureCall hops to the chain base. */
    private static RExpression walkToBase(RExpression e) {
        int depth = 0;
        while (e instanceof RFeatureCall fc && fc.receiver() != null && depth++ < 256) {
            e = fc.receiver();
        }
        return e;
    }

    private static boolean isImplicitVariable(RExpression e) {
        return e != null && e.getClass().getSimpleName().equals("RImplicitVariable");
    }

    private static boolean missing(RWorkspace ws, RExpression e) {
        try {
            RMetaAnnotatedType t = ws.getInferredType(e);
            return t == null || t.isMissing();
        } catch (RuntimeException ex) {
            return true;                              // unresolvable == MISSING for this census
        }
    }

    private static List<RModel> load(Path root) {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> p.toString().endsWith(".rosetta")).sorted().map(p -> {
                try {
                    return AstBuilder.buildFromString(Files.readString(p), p.toString());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void report(Map<String, CellTally> cells) {
        System.out.println("\n=== IR-LAB MISSING-BASE CENSUS (leaf-vs-cascade) ===");
        System.out.printf("%-12s | %9s | %10s | %13s | %7s%n",
                "cell/seat", "symbol-ref", "non-symbol", "implicit-item", "total");
        long sRef = 0, sNon = 0, sItem = 0;
        Map<String, Long> allKinds = new TreeMap<>();
        for (var e : cells.entrySet()) {
            CellTally t = e.getValue();
            System.out.printf("%-12s | %9d | %10d | %13d | %7d%n",
                    e.getKey(), t.symbolRef, t.nonSymbol, t.implicitItem, t.total());
            sRef += t.symbolRef; sNon += t.nonSymbol; sItem += t.implicitItem;
            t.symbolKind.forEach((k, v) -> allKinds.merge(k, v, Long::sum));
        }
        System.out.printf("%-12s | %9d | %10d | %13d | %7d%n",
                "TOTAL", sRef, sNon, sItem, sRef + sNon + sItem);
        System.out.println("\n=== symbol-ref bucket sub-classification (by resolved-symbol kind) ===");
        long sumKinds = allKinds.values().stream().mapToLong(Long::longValue).sum();
        allKinds.forEach((k, v) ->
                System.out.printf("  %-28s %7d  (%.1f%% of symbol-ref)%n",
                        k, v, sumKinds == 0 ? 0.0 : 100.0 * v / sumKinds));
        long fnCall = allKinds.getOrDefault("RFunction", 0L);
        System.out.printf("%n>>> GENUINE-LEAF (case a — function-call output, RFunction symbol) = %d / %d symbol-ref = %.1f%%%n",
                fnCall, sumKinds, sumKinds == 0 ? 0.0 : 100.0 * fnCall / sumKinds);
        System.out.println("\n=== raw base AST-class distribution (characterizes non-symbol + implicit-item) ===");
        Map<String, Long> allBaseClasses = new TreeMap<>();
        cells.values().forEach(t -> t.baseClass.forEach((k, v) -> allBaseClasses.merge(k, v, Long::sum)));
        allBaseClasses.forEach((k, v) -> System.out.printf("  %-28s %7d%n", k, v));

        System.out.println("\n=== case (a): WHY is each RFunction-leaf output MISSING? (origin|reason) ===");
        Map<String, Long> allReasons = new TreeMap<>();
        Map<String, List<String>> allSamples = new TreeMap<>();
        Map<String, Long> allUnresolved = new TreeMap<>();
        for (CellTally t : cells.values()) {
            t.rfnReason.forEach((k, v) -> allReasons.merge(k, v, Long::sum));
            t.rfnSamples.forEach((k, v) -> {
                List<String> s = allSamples.computeIfAbsent(k, x -> new ArrayList<>());
                for (String name : v) if (s.size() < 6 && !s.contains(name)) s.add(name);
            });
            t.rfnUnresolvedTypeName.forEach((k, v) -> allUnresolved.merge(k, v, Long::sum));
        }
        long reasonSum = allReasons.values().stream().mapToLong(Long::longValue).sum();
        allReasons.forEach((k, v) ->
                System.out.printf("  %-34s %6d  (%.1f%%)  e.g. %s%n",
                        k, v, reasonSum == 0 ? 0.0 : 100.0 * v / reasonSum,
                        allSamples.getOrDefault(k, List.of())));
        System.out.printf("  (RFunction-leaf reason sum = %d; matches symbol-ref RFunction tally)%n", reasonSum);
        System.out.println("\n=== ref-absent,nonbuiltin unresolved output typeNames (top 25 by count) ===");
        allUnresolved.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(25)
                .forEach(e -> System.out.printf("  %-40s %6d%n", e.getKey(), e.getValue()));
        System.out.println("=== END CENSUS ===\n");
    }
}
