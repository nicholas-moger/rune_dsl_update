package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * IR-Lab head&rarr;feature CASCADE BOTTOM-OUT census (relayed via Nick, 2026-06-26) —
 * read-only, 0 byte flips, NOT a parity gate. The follow-on to the leaf-vs-cascade
 * census: that one sized the symbol-ref bucket and split the case-(a) RFunction leaf
 * (~3,331) from the disguised-attribute-navigation cascade (~7,148 / "~5,796"). This
 * one DECOMPOSES the cascade by the TYPE it ultimately bottoms out at — the Pareto the
 * Lab asked for so it can line up a first tranche.
 *
 * <p><b>Recipe.</b> For each {@link RFeatureCall} whose {@code getInferredType(receiver)}
 * is MISSING (the prior census gate — reconciles with the ~5,796), walk the receiver to
 * its chain BASE, then run {@link #bottomOut} which chases the MISSING down the chain to
 * the ROOT CAUSE terminal: an attribute / function-output whose declared {@code typeCall}
 * does not resolve to an {@link com.regnosys.rosetta.types.RType} (the bottom-out TYPE —
 * the Pareto target), OR a resolution failure (feature / symbol / Cat-10 chain never
 * bound). Each terminal is classified for DISPOSITION and its type name tallied.
 *
 * <p><b>Fidelity.</b> Unlike the prior census, this loads the FULL D11 transitive closure
 * per cell (builtins; cdm6 + rune-fpml-1.5.3; drr + cdm-5.38.0 + iso20022-1.38.0), exactly
 * as {@code D11CorpusRegressionTest.loadCellCorpus} does — so a bottom-out that is "just a
 * cross-namespace type the probe never loaded" is NOT mistaken for an engine gap (the
 * inert-filter-artifact guard). A {@code ref-absent,nonbuiltin} terminal is further split
 * by whether the type name is DECLARED somewhere in the loaded workspace (a real
 * linker/engine gap) vs absent (cross-namespace / genuinely unresolvable).
 *
 * <p>Run targeted: {@code mvn -f rune-parser/pom.xml test -Dtest=IrLabCascadeBottomOutCensusTest}.
 * Prints a report to stdout; asserts nothing (a census).
 */
class IrLabCascadeBottomOutCensusTest {

    private static final Path CORPUS = Path.of("../test-corpus");
    private static final Path BUILTINS_DIR =
            CORPUS.resolve("rune-dsl-builtins/rune-runtime/src/main/resources/model");
    private static final Path CDM5 = CORPUS.resolve("cdm/cdm-5.38.0/rosetta-source/src/main/rosetta");
    private static final Path CDM6 = CORPUS.resolve("cdm/cdm-6.20.6/rosetta-source/src/main/rosetta");
    private static final Path FPML153 = CORPUS.resolve("rune-fpml/rune-fpml-1.5.3/rosetta-source/src/main/rosetta");
    private static final Path DRR = CORPUS.resolve("drr/drr-6.34.1/rosetta-source/src/main/rosetta");
    private static final Path ISO = CORPUS.resolve("iso20022/iso20022-1.38.0/rosetta-source/src/main/rosetta");

    private static final BuiltinTypeRegistry BUILTINS = BuiltinTypeRegistry.createDefault();

    private enum Seat { FUNCTION, RULE }

    @Test
    void censusCascadeBottomOut() {
        assumeTrue(Files.isDirectory(CDM5) && Files.isDirectory(DRR) && Files.isDirectory(BUILTINS_DIR),
                "test-corpus not available");

        // RWorkspace.build FREEZES then MUTATES (resolveTypeCall) the model instances,
        // so EVERY shared dependency (builtins, cdm5) must be loaded FRESH per workspace —
        // reusing instances across builds throws "RNode mutated post-freeze".
        Map<String, CellTally> cells = new LinkedHashMap<>();

        // cdm5 = builtins + cdm5 (no transitive dep).
        List<RModel> cdm5Models = load(CDM5);
        List<RModel> cdm5All = concat(load(BUILTINS_DIR), cdm5Models);
        RWorkspace cdm5Ws = RWorkspace.build(cdm5All).workspace();
        cells.put("cdm5 FUNC", measureSeat(cdm5Models, Seat.FUNCTION, cdm5Ws, typeNames(cdm5All)));

        // cdm6 = builtins + rune-fpml-1.5.3 (transitive) + cdm6.
        List<RModel> cdm6Models = load(CDM6);
        List<RModel> cdm6All = concat(load(BUILTINS_DIR), load(FPML153), cdm6Models);
        RWorkspace cdm6Ws = RWorkspace.build(cdm6All).workspace();
        cells.put("cdm6 FUNC", measureSeat(cdm6Models, Seat.FUNCTION, cdm6Ws, typeNames(cdm6All)));

        // drr = builtins + cdm-5.38.0 (transitive) + iso20022-1.38.0 (transitive) + drr.
        List<RModel> drrModels = load(DRR);
        List<RModel> drrAll = concat(load(BUILTINS_DIR), load(CDM5), load(ISO), drrModels);
        RWorkspace drrWs = RWorkspace.build(drrAll).workspace();
        Set<String> drrTypeNames = typeNames(drrAll);
        cells.put("drr FUNC", measureSeat(drrModels, Seat.FUNCTION, drrWs, drrTypeNames));
        cells.put("drr RULE", measureSeat(drrModels, Seat.RULE, drrWs, drrTypeNames));

        report(cells);
    }

    // === tally ===============================================================

    private static final class CellTally {
        long population;                                    // bases (prior census gate)
        final Map<String, Long> baseKind = new TreeMap<>(); // reconcile w/ prior census
        final Map<String, Long> bottomCategory = new TreeMap<>();
        final Map<String, Long> bottomTypeName = new TreeMap<>();   // the Pareto
        // bottom type name restricted to engine-completable categories (ref-absent EXISTS / ref=<gap-class>)
        final Map<String, Long> completableTypeName = new TreeMap<>();
        // toplevel:input-binding-gap head-shape diagnostics (verifies the mechanism)
        long tlHeadIsAttrOnInput, tlHeadIsLowercaseInput, tlHeadIsInputParam, tlHeadOther;
        final List<String> tlSamples = new ArrayList<>();
    }

    private CellTally measureSeat(List<RModel> cellModels, Seat seat, RWorkspace ws, Set<String> wsTypeNames) {
        CellTally t = new CellTally();
        for (RModel model : cellModels) {
            for (RRootElement root : model.rootElements()) {
                if (seat == Seat.FUNCTION && !(root instanceof RFunction)) continue;
                if (seat == Seat.RULE && !(root instanceof RRule)) continue;
                AstWalker.walk(root, node -> classifyNode(node, ws, wsTypeNames, t));
            }
        }
        return t;
    }

    private void classifyNode(RNode node, RWorkspace ws, Set<String> wsTypeNames, CellTally t) {
        if (!(node instanceof RFeatureCall fc)) return;
        RExpression receiver = fc.receiver();
        if (receiver == null) return;
        if (!missing(ws, receiver)) return;             // gate: MISSING-typed receiver (prior census gate)
        RExpression base = walkToBase(receiver);
        t.population++;
        t.baseKind.merge(base == null ? "<null>" : base.getClass().getSimpleName(), 1L, Long::sum);

        BottomOut bo = bottomOut(base, ws, wsTypeNames, new HashSet<>(), 0);
        t.bottomCategory.merge(bo.category, 1L, Long::sum);
        if (bo.typeName != null && !bo.typeName.isEmpty()) {
            t.bottomTypeName.merge(bo.typeName, 1L, Long::sum);
            if (bo.completable) {
                t.completableTypeName.merge(bo.category + " :: " + bo.typeName, 1L, Long::sum);
            }
        }
        if (bo.category.equals("toplevel:input-binding-gap") && base instanceof REnumValueRef evr) {
            diagnoseToplevelHead(evr, t);
        }
    }

    /**
     * For a {@code toplevel:input-binding-gap} nav, classifies the head shape against
     * the enclosing function/rule input type — the decisive verification of WHAT the
     * disguised head is: an attribute on the input (a 2-segment Cat-10 chain needing
     * a function-input fallback), the lowercase input-type name (a 1-segment implicit
     * input-as-receiver nav), an explicit input param name, or other.
     */
    private void diagnoseToplevelHead(REnumValueRef evr, CellTally t) {
        RTypeCall tc = enclosingInputTypeCall(evr);
        RDataType inputType = null;
        if (tc != null) {
            try {
                if (tc.referencedType().orElse(null) instanceof RDataType dt) inputType = dt;
            } catch (RuntimeException ignored) { /* leave null */ }
        }
        String head = evr.enumName();
        String inputName = tc == null ? "?" : simpleName(str(tc.typeName()));
        String shape;
        if (inputType != null && hasAttribute(inputType, head)) {
            t.tlHeadIsAttrOnInput++;
            shape = "attr-on-input";
        } else if (inputName.length() > 1
                && (Character.toLowerCase(inputName.charAt(0)) + inputName.substring(1)).equals(head)) {
            t.tlHeadIsLowercaseInput++;
            shape = "lowercase-input(implicit-receiver)";
        } else if (matchesEnclosingInputParam(evr, head)) {
            t.tlHeadIsInputParam++;
            shape = "input-param";
        } else {
            t.tlHeadOther++;
            shape = "OTHER";
        }
        if (t.tlSamples.size() < 25) {
            RFunction fn = AstWalker.findAncestor(evr, RFunction.class).orElse(null);
            String fnName = fn == null ? "<rule>" : fn.origin() + ":" + fn.name();
            t.tlSamples.add(String.format("%-34s input=%-22s head=%-22s -> %-22s [%s]",
                    fnName, inputName, head, evr.valueName(), shape));
        }
    }

    private static boolean hasAttribute(RDataType dt, String name) {
        java.util.Set<RDataType> seen = new HashSet<>();
        for (RDataType cur = dt; cur != null && seen.add(cur); cur = cur.superType().orElse(null)) {
            for (RAttribute a : cur.attributes()) {
                if (name.equals(a.name())) return true;
            }
        }
        return false;
    }

    private static boolean matchesEnclosingInputParam(RExpression node, String head) {
        RFunction fn = AstWalker.findAncestor(node, RFunction.class).orElse(null);
        if (fn == null) return false;
        for (RAttribute in : fn.inputs()) {
            if (head.equals(in.name())) return true;
        }
        return false;
    }

    // === the bottom-out walk =================================================

    /** A terminal of the cascade: the root cause of the MISSING + its type name. */
    private record BottomOut(String category, String typeName, boolean completable) {}

    private static BottomOut term(String cat, String tn, boolean completable) {
        return new BottomOut(cat, tn, completable);
    }

    /**
     * Chases the MISSING down the chain to the root-cause terminal. {@code e} is
     * MISSING-typed by the caller's gate. Recurses only into the sub-expression
     * whose MISSING this node inherits (mirroring {@link ExpressionTypeComputer}).
     */
    private BottomOut bottomOut(RExpression e, RWorkspace ws, Set<String> wsTypeNames,
                                Set<RExpression> seen, int depth) {
        if (e == null) return term("null-expr", "", false);
        if (depth > 64 || !seen.add(e)) return term("depth-or-cycle", "", false);

        if (e instanceof RFeatureCall fc) {
            // FC type = inferTypeOfAttribute(resolvedFeature) if present; that being
            // MISSING means the feature attribute's declared type is the bottom-out.
            if (fc.resolvedFeature().isPresent()) {
                return classifyAttr(fc.resolvedFeature().get(), wsTypeNames, "feature");
            }
            // feature unresolved: type depends on receiver (choice-option/record nav).
            if (fc.receiver() != null && missing(ws, fc.receiver())) {
                return bottomOut(fc.receiver(), ws, wsTypeNames, seen, depth + 1);
            }
            // receiver typed (or none) but feature not found = a resolution gap on a typed receiver.
            return term("feature-unresolved", fc.featureName(), false);
        }

        if (e instanceof REnumValueRef evr) {
            if (evr.resolvedAttributeChain().isPresent()) {
                return classifyAttr(evr.resolvedAttributeChain().get().feature(), wsTypeNames, "chain-feature");
            }
            if (evr.resolvedSymbol().isPresent()) {
                return fromSymbol(evr.resolvedSymbol().get(), ws, wsTypeNames, seen, depth, evr.valueName());
            }
            if (evr.resolvedChoiceOption().isPresent()) {
                return term("disguised-choiceOption(gm-aware)", evr.valueName(), false);
            }
            if (evr.resolvedRestrictionType().isPresent() || evr.resolvedTypeRestriction().isPresent()) {
                return term("disguised-restriction(downcast,gm-aware)", evr.valueName(), false);
            }
            // The dominant cascade: head -> feature where the Cat-10 chain never bound.
            // Trace WHY: is the implicit-item/input context type KNOWN (an engine-completable
            // binding gap) or itself MISSING (recurse to the true leaf)?
            return classifyDisguisedContext(evr, ws, wsTypeNames, seen, depth);
        }

        if (e instanceof RSymbolReference sr) {
            if (sr.symbol().isPresent()) {
                return fromSymbol(sr.symbol().get(), ws, wsTypeNames, seen, depth, sr.name());
            }
            return term("symbol-unresolved", sr.name(), false);
        }

        // structural ops: descend into the type-determining child, chasing the MISSING.
        RExpression child = typeDeterminingChild(e);
        if (child != null) {
            return bottomOut(child, ws, wsTypeNames, seen, depth + 1);
        }
        return term("other:" + e.getClass().getSimpleName(), "", false);
    }

    /**
     * For a disguised {@code head -> feature} nav whose Cat-10 chain never bound,
     * works out WHY: the nav navigates an implicit item (inside a lambda) or an
     * implicit input (rule from-type / function input). If that context type is
     * KNOWN, the nav is an engine-side resolution/binding gap (the type exists, the
     * engine just didn't bind the disguised nav) — the context type is the Pareto
     * "bottom-out type". If the context type is itself MISSING, recurse to the TRUE
     * leaf (the function output / bare symbol the whole chain hangs off).
     */
    private BottomOut classifyDisguisedContext(REnumValueRef evr, RWorkspace ws,
                                               Set<String> wsTypeNames, Set<RExpression> seen, int depth) {
        RExpression ctxSrc = implicitContextSource(evr);
        if (ctxSrc == null) {
            // Top-level nav on the enclosing rule from-type / function input.
            RTypeCall tc = enclosingInputTypeCall(evr);
            if (tc == null) {
                return term("toplevel:no-context", evr.enumName() + "->" + evr.valueName(), false);
            }
            String tn = str(tc.typeName());
            boolean known;
            try { known = tc.referencedType().isPresent() || wsTypeNames.contains(simpleName(tn)); }
            catch (RuntimeException ex) { known = wsTypeNames.contains(simpleName(tn)); }
            return known
                    ? term("toplevel:input-binding-gap", tn, true)        // engine could bind the implicit-input nav
                    : term("toplevel:input-type-unresolved", tn, false);  // input type cross-namespace/absent
        }
        if (missing(ws, ctxSrc)) {
            // The item source is itself MISSING — recurse to the genuine leaf.
            BottomOut leaf = bottomOut(ctxSrc, ws, wsTypeNames, seen, depth + 1);
            return new BottomOut("cascade-leaf|" + leaf.category(), leaf.typeName(), leaf.completable());
        }
        // Context (lambda item) type resolves, but the Cat-10 binding didn't fire.
        return term("lambda-item-binding-gap", typeName(ws.getInferredType(ctxSrc)), true);
    }

    /** The implicit-item source expression for a node inside a lambda, or null at top level. */
    private static RExpression implicitContextSource(RExpression node) {
        RInlineFunction inline = AstWalker.findAncestor(node, RInlineFunction.class).orElse(null);
        if (inline != null && inline.parent() instanceof RExpression op) {
            return op.left().orElse(null);
        }
        return null;
    }

    /** The enclosing rule's from-type, or the enclosing function's first input type. */
    private static RTypeCall enclosingInputTypeCall(RExpression node) {
        RRule rule = AstWalker.findAncestor(node, RRule.class).orElse(null);
        if (rule != null && rule.fromType().isPresent()) {
            return rule.fromType().get();
        }
        RFunction fn = AstWalker.findAncestor(node, RFunction.class).orElse(null);
        if (fn != null && !fn.inputs().isEmpty()) {
            return fn.inputs().get(0).typeCall();
        }
        return null;
    }

    private static String typeName(RMetaAnnotatedType t) {
        if (t == null || t.isMissing()) return "<missing>";
        try { return t.type().name(); }
        catch (RuntimeException ex) { return t.type().getClass().getSimpleName(); }
    }

    private BottomOut fromSymbol(RNode sym, RWorkspace ws, Set<String> wsTypeNames,
                                 Set<RExpression> seen, int depth, String name) {
        if (sym instanceof RAttribute attr) {
            return classifyAttr(attr, wsTypeNames, "attr");
        }
        if (sym instanceof RFunction fn) {
            return fn.output()
                    .map(o -> classifyAttr(o, wsTypeNames, "fn-output"))
                    .orElse(term("fn-no-output", fn.name(), false));
        }
        if (sym instanceof RRule rule) {
            return rule.expression()
                    .map(x -> bottomOut(x, ws, wsTypeNames, seen, depth + 1))
                    .orElse(term("rule-no-expr", rule.name(), false));
        }
        if (sym instanceof RShortcut sc) {
            return sc.expression() != null
                    ? bottomOut(sc.expression(), ws, wsTypeNames, seen, depth + 1)
                    : term("shortcut-no-expr", name, false);
        }
        return term("symbol=" + sym.getClass().getSimpleName(), name, false);
    }

    /**
     * Classifies an attribute's declared type as a bottom-out terminal. Mirrors
     * {@link ExpressionTypeComputer#inferTypeOfAttribute}: resolves iff the
     * typeCall resolves to RDataType/REnumeration/RChoice/RTypeAlias, or the type
     * name is a builtin. When it would resolve, this attribute is NOT the true
     * cascade root — the MISSING came from a feature/resolution gap above it
     * (reported as {@code RESOLVES-not-bottom}, a walk-overshoot diagnostic).
     */
    private BottomOut classifyAttr(RAttribute attr, Set<String> wsTypeNames, String role) {
        if (attr == null) return term(role + ":attr-null", "", false);
        RTypeCall tc = attr.typeCall();
        if (tc == null) return term(role + ":tc-null", "", false);

        Optional<RNode> ref;
        try { ref = tc.referencedType(); }
        catch (RuntimeException ex) { return term(role + ":ref-throws", str(tc.typeName()), false); }

        if (ref.isPresent()) {
            RNode r = ref.get();
            String cls = r.getClass().getSimpleName();
            String tn = str(tc.typeName());
            // These resolve in inferTypeOfAttribute -> the attr is NOT the bottom-out.
            if (r instanceof RDataType || r instanceof REnumeration
                    || r instanceof RChoice || r instanceof RTypeAlias) {
                return term(role + ":RESOLVES-not-bottom(" + cls + ")", tn, false);
            }
            // resolved to some OTHER node class astNodeToRType + builtins don't cover.
            boolean builtin = tn != null && BUILTINS.lookup(tn).isPresent();
            if (builtin) {
                return term(role + ":ref=" + cls + ",builtin-name", tn, false);
            }
            return term(role + ":ref=" + cls + ",NONbuiltin", tn, true);   // potential engine gap
        }

        // typeCall did not resolve (referencedType empty).
        String tn = str(tc.typeName());
        if (tn != null && BUILTINS.lookup(tn).isPresent()) {
            return term(role + ":ref-absent,builtin", tn, false);   // resolves via builtin fallback (shouldn't be MISSING)
        }
        boolean declaredInWs = tn != null && wsTypeNames.contains(simpleName(tn));
        return declaredInWs
                ? term(role + ":ref-absent,nonbuiltin,EXISTS-in-ws", tn, true)   // linker gap: type IS loaded
                : term(role + ":ref-absent,nonbuiltin,NOT-loaded", tn, false);   // cross-namespace/absent: not an engine gap
    }

    /** The child whose inferred type this structural node inherits, or null. */
    private static RExpression typeDeterminingChild(RExpression e) {
        if (e instanceof RExtractExpr ext) {
            return ext.body() != null ? ext.body().body() : ext.argument();
        }
        // Generic: list-ops, filter, sort, max, min, default, with-meta, then, conditional
        // all inherit from left() (the argument) in the absence of a more specific child.
        return e.left().orElse(null);
    }

    // === helpers =============================================================

    private static RExpression walkToBase(RExpression e) {
        int depth = 0;
        while (e instanceof RFeatureCall fc && fc.receiver() != null && depth++ < 256) {
            e = fc.receiver();
        }
        return e;
    }

    private static boolean missing(RWorkspace ws, RExpression e) {
        try {
            RMetaAnnotatedType t = ws.getInferredType(e);
            return t == null || t.isMissing();
        } catch (RuntimeException ex) {
            return true;
        }
    }

    private static String str(String s) { return s == null ? "<null-typeName>" : s; }

    private static double pct(long n, long d) { return d == 0 ? 0.0 : 100.0 * n / d; }

    private static String simpleName(String qualified) {
        int dot = qualified.lastIndexOf('.');
        return dot >= 0 ? qualified.substring(dot + 1) : qualified;
    }

    /** Simple names of every type-like declaration in the loaded models. */
    private static Set<String> typeNames(List<RModel> models) {
        Set<String> names = new HashSet<>();
        for (RModel m : models) {
            for (RRootElement r : m.rootElements()) {
                if (r instanceof RDataType dt) names.add(dt.name());
                else if (r instanceof REnumeration en) names.add(en.name());
                else if (r instanceof RChoice ch) names.add(ch.name());
                else if (r instanceof RTypeAlias al) names.add(al.name());
            }
        }
        return names;
    }

    @SafeVarargs
    private static List<RModel> concat(List<RModel>... lists) {
        List<RModel> all = new ArrayList<>();
        for (List<RModel> l : lists) all.addAll(l);
        return all;
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

    // === report ==============================================================

    private void report(Map<String, CellTally> cells) {
        System.out.println("\n=== IR-LAB CASCADE BOTTOM-OUT CENSUS (full transitive closure) ===");
        long pop = 0;
        for (var e : cells.entrySet()) {
            System.out.printf("  %-12s population(bases) = %d%n", e.getKey(), e.getValue().population);
            pop += e.getValue().population;
        }
        System.out.printf("  %-12s population(bases) = %d%n", "TOTAL", pop);

        System.out.println("\n--- base AST-kind (reconcile with prior leaf-vs-cascade census) ---");
        Map<String, Long> baseKinds = new TreeMap<>();
        cells.values().forEach(t -> t.baseKind.forEach((k, v) -> baseKinds.merge(k, v, Long::sum)));
        baseKinds.forEach((k, v) -> System.out.printf("  %-26s %7d%n", k, v));

        System.out.println("\n--- BOTTOM-OUT CATEGORY (the disposition split) ---");
        Map<String, Long> cat = new TreeMap<>();
        cells.values().forEach(t -> t.bottomCategory.forEach((k, v) -> cat.merge(k, v, Long::sum)));
        long catSum = cat.values().stream().mapToLong(Long::longValue).sum();
        cat.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> System.out.printf("  %-46s %7d  (%.1f%%)%n",
                        e.getKey(), e.getValue(), catSum == 0 ? 0.0 : 100.0 * e.getValue() / catSum));

        System.out.println("\n--- per-cell BOTTOM-OUT CATEGORY (cdm-vs-drr engine-vs-linker) ---");
        System.out.printf("  %-46s | %8s %8s %8s %8s%n", "category", "cdm5", "cdm6", "drrF", "drrR");
        for (String k : cat.keySet()) {
            System.out.printf("  %-46s | %8d %8d %8d %8d%n", k,
                    cells.get("cdm5 FUNC").bottomCategory.getOrDefault(k, 0L),
                    cells.get("cdm6 FUNC").bottomCategory.getOrDefault(k, 0L),
                    cells.get("drr FUNC").bottomCategory.getOrDefault(k, 0L),
                    cells.get("drr RULE").bottomCategory.getOrDefault(k, 0L));
        }

        System.out.println("\n--- toplevel:input-binding-gap HEAD SHAPE (what the disguised head IS) ---");
        long aOnIn = 0, lcIn = 0, inParam = 0, other = 0;
        for (CellTally c : cells.values()) {
            aOnIn += c.tlHeadIsAttrOnInput; lcIn += c.tlHeadIsLowercaseInput;
            inParam += c.tlHeadIsInputParam; other += c.tlHeadOther;
        }
        long shapeSum = aOnIn + lcIn + inParam + other;
        System.out.printf("  attr-on-input-type (2-seg Cat-10)      %7d  (%.1f%%)%n", aOnIn, pct(aOnIn, shapeSum));
        System.out.printf("  lowercase-input (implicit receiver)    %7d  (%.1f%%)%n", lcIn, pct(lcIn, shapeSum));
        System.out.printf("  explicit-input-param name              %7d  (%.1f%%)%n", inParam, pct(inParam, shapeSum));
        System.out.printf("  OTHER                                  %7d  (%.1f%%)%n", other, pct(other, shapeSum));
        System.out.println("  -- samples --");
        cells.forEach((k, c) -> c.tlSamples.stream().limit(7)
                .forEach(s -> System.out.printf("    [%-9s] %s%n", k, s)));

        System.out.println("\n--- BOTTOM-OUT TYPE-NAME PARETO (top 40 across all cells) ---");
        Map<String, Long> tn = new TreeMap<>();
        cells.values().forEach(t -> t.bottomTypeName.forEach((k, v) -> tn.merge(k, v, Long::sum)));
        tn.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(40)
                .forEach(e -> System.out.printf("  %-44s %7d%n", e.getKey(), e.getValue()));

        System.out.println("\n--- ENGINE-COMPLETABLE bottom-outs (category :: type, top 40) ---");
        Map<String, Long> comp = new TreeMap<>();
        cells.values().forEach(t -> t.completableTypeName.forEach((k, v) -> comp.merge(k, v, Long::sum)));
        long compSum = comp.values().stream().mapToLong(Long::longValue).sum();
        System.out.printf("  (total engine-completable terminals = %d)%n", compSum);
        comp.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(40)
                .forEach(e -> System.out.printf("  %-60s %7d%n", e.getKey(), e.getValue()));
        System.out.println("=== END CASCADE BOTTOM-OUT CENSUS ===\n");
    }
}
