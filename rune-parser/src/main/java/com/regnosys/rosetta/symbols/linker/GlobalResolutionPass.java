package com.regnosys.rosetta.symbols.linker;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.annotations.RAnnotation;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.external.RExternalClass;
import com.regnosys.rosetta.ast.external.RExternalEnum;
import com.regnosys.rosetta.ast.external.RExternalRuleSource;
import com.regnosys.rosetta.ast.external.RExternalSynonymSource;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.mapping.RMapTestFunc;
import com.regnosys.rosetta.ast.model.RQualifiableConfig;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.regulatory.RSegmentDef;
import com.regnosys.rosetta.ast.regulatory.RSegmentRef;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.synonyms.RSynonymSource;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RFileScope;
import com.regnosys.rosetta.symbols.RNamespaceScope;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.SymbolResolver;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;
import com.regnosys.rosetta.symbols.index.ReferenceIndex;
import com.regnosys.rosetta.symbols.index.SubTypeIndex;

import java.util.*;
import java.util.function.Predicate;

/**
 * Pass 4 — resolves global cross-references against per-file scopes.
 * T8 implements type-related categories; T9 extends with annotations,
 * enums, rules, regulatory, external, and multi-valued categories.
 *
 * <p>Spec: D7 (pass 4) + D1 + D11 in
 * {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class GlobalResolutionPass implements LinkerPass {

    private final long generation;
    private final Map<String, RNamespaceScope> namespaces;
    private final Map<RModel, RFileScope> fileScopes;
    private final SubTypeIndex subTypeIndex;
    private final ReferenceIndex referenceIndex;
    private final com.regnosys.rosetta.symbols.CellPreference cellPreference;

    public GlobalResolutionPass(
            long generation,
            Map<String, RNamespaceScope> namespaces,
            Map<RModel, RFileScope> fileScopes,
            SubTypeIndex subTypeIndex,
            ReferenceIndex referenceIndex) {
        this(generation, namespaces, fileScopes, subTypeIndex, referenceIndex,
                com.regnosys.rosetta.symbols.CellPreference.NONE);
    }

    /** PR #445 — the build's constructor, carrying the workspace cell preference. */
    public GlobalResolutionPass(
            long generation,
            Map<String, RNamespaceScope> namespaces,
            Map<RModel, RFileScope> fileScopes,
            SubTypeIndex subTypeIndex,
            ReferenceIndex referenceIndex,
            com.regnosys.rosetta.symbols.CellPreference cellPreference) {
        this.generation = generation;
        this.namespaces = namespaces;
        this.fileScopes = fileScopes;
        this.subTypeIndex = subTypeIndex;
        this.referenceIndex = referenceIndex;
        this.cellPreference = cellPreference;
    }

    public void run(List<RModel> files, Diagnostics collector) {
        for (RModel file : files) {
            RFileScope scope = fileScopes.get(file);
            if (scope == null) continue;

            for (RTypeCall tc : AstWalker.findAll(file, RTypeCall.class)) {
                resolveTypeCall(tc, scope, collector);
            }
            for (RDataType dt : AstWalker.findAll(file, RDataType.class)) {
                resolveDataSuperType(dt, scope, collector);
            }
            for (REnumeration en : AstWalker.findAll(file, REnumeration.class)) {
                resolveEnumSuperType(en, scope, collector);
            }
            for (RFunction fn : AstWalker.findAll(file, RFunction.class)) {
                resolveSuperFunction(fn, scope, collector);
            }
            for (RQualifiableConfig qc : AstWalker.findAll(file, RQualifiableConfig.class)) {
                resolveQualifiableRoot(qc, scope, collector);
            }

            // T9 categories
            for (RAnnotationRef ar : AstWalker.findAll(file, RAnnotationRef.class)) {
                resolveAnnotationRef(ar, scope, collector);
            }
            for (REnumValueRef evr : AstWalker.findAll(file, REnumValueRef.class)) {
                resolveEnumValueRef(evr, scope, collector);
            }
            for (RConversionExpr cv : AstWalker.findAll(file, RConversionExpr.class)) {
                resolveConversionTarget(cv, scope, collector);
            }
            for (RRuleReferenceAnnotation rr : AstWalker.findAll(file, RRuleReferenceAnnotation.class)) {
                resolveRuleRef(rr, scope, collector);
            }
            for (RSegmentRef sr : AstWalker.findAll(file, RSegmentRef.class)) {
                resolveSegmentRef(sr, scope, collector);
            }
            for (RExternalClass ec : AstWalker.findAll(file, RExternalClass.class)) {
                resolveExternalClass(ec, scope, collector);
            }
            for (RExternalEnum ee : AstWalker.findAll(file, RExternalEnum.class)) {
                resolveExternalEnum(ee, scope, collector);
            }
            for (RMapTestFunc mtf : AstWalker.findAll(file, RMapTestFunc.class)) {
                resolveMapTestFunc(mtf, scope, collector);
            }
            for (RExternalSynonymSource ess : AstWalker.findAll(file, RExternalSynonymSource.class)) {
                resolveExternalSynonymSourceExtends(ess, scope, collector);
            }
            for (RExternalRuleSource ers : AstWalker.findAll(file, RExternalRuleSource.class)) {
                resolveExternalRuleSourceExtends(ers, scope, collector);
            }
            // v3.2 seat 4 (PR #625, F7): a report's `with type` / `with source` are scoped references
            for (RReport rp : AstWalker.findAll(file, RReport.class)) {
                resolveReport(rp, scope, collector);
            }
            // PR #451 — speculative TYPE resolution of switch-case NAME
            // guards. Upstream scopes a guard by its SUBJECT's kind
            // (RosettaScopeProvider SWITCH_CASE_GUARD__REFERENCE_GUARD):
            // enum subject → the enum's values, choice subject → the
            // choice's options, DATA subject → the global DATA-filtered
            // default scope — i.e. a data guard (`fpml.CreditDefaultSwap
            // then ...`) is an ordinary import/alias-qualified global type
            // reference. The subject's type is an engine-side fact, so the
            // linker resolves every still-unresolved NAME guard through the
            // qualified ladder KIND-GATED to type-like nodes (RDataType /
            // RChoice — the #450 ofKind sibling machinery) and stores the
            // node SPECULATIVELY: the engine's Cat-4 pass overwrites with
            // the subject-typed resolution where the subject turns out to
            // be an enum (value) or choice (option), and the switch-case
            // narrowing consumes the node for data subjects. NO diagnostic
            // is ever emitted here — a guard that fails the type lookup may
            // legitimately be an enum value or a choice option (upstream's
            // guard-kind errors are validator territory, unported). Both
            // generator resolvedGuard consumers treat non-REnumValue nodes
            // exactly like EMPTY, so the store is render-neutral.
            for (var guard : AstWalker.findAll(file,
                    com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard.class)) {
                resolveSwitchGuardType(guard, scope);
            }
            // Phase X1 PR #76 — resolve qualified cross-namespace RSymbolReference
            // (dotted-name shape, e.g. `cdeV3.collateral.X`). Bare unqualified
            // refs continue to resolve in LexicalResolutionPass (pass 5) via the
            // scope-chain walk. The skip at LexicalResolutionPass:94 honors any
            // symbol() already set here.
            for (RSymbolReference ref
                    : AstWalker.findAll(file, RSymbolReference.class)) {
                if (ref.symbol().isPresent()) continue;
                String name = ref.name();
                if (name == null || !name.contains(".")) continue;
                Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
                if (target.isPresent()) {
                    ref.setResolvedSymbol(target.get());
                    referenceIndex.registerReference(ref, target.get());
                }
            }
        }

        detectCircularInheritance(files, collector);
    }

    private void resolveTypeCall(RTypeCall tc, RFileScope scope, Diagnostics collector) {
        String name = tc.typeName();
        if (name == null || name.isEmpty()) return;
        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        // When both a type and a function share the same name (e.g. CreditSupportAmount),
        // the generic lookup may return the function. For type calls, prefer type-like nodes.
        if (target.isPresent() && !isTypeNode(target.get())) {
            Optional<RRootElement> typeTarget = resolveTypeNodeByName(name, scope);
            if (typeTarget.isPresent()) {
                target = typeTarget;
            }
        }
        if (target.isPresent()) {
            // R7-1 + R11-3 fix: tc.typeName() in source can be unqualified ("Bar"),
            // qualified ("com.foo.Bar"), or aliased — namespace alias ("f.Bar" via
            // `import com.foo.* as f`) or single-type alias ("MyType" via
            // `import com.foo.Bar as MyType`). Deriving localName from source text
            // works for unqualified + qualified but breaks for single-type aliases
            // because "MyType" doesn't appear in any namespace's local-name table
            // (only "Bar" does). symbolIdOf would fall through to BUILTIN_NAMESPACE
            // with the wrong localName.
            //
            // Use the resolved target's ACTUAL declared name via type-specific
            // accessor. RRootElement has no abstract name() method, so dispatch on
            // the type-like subtypes that resolveTypeCall handles (per isTypeNode).
            String localName = typeNodeName(target.get(), name);
            tc.setReferencedTypeId(symbolIdOf(target.get(), localName));
            referenceIndex.registerReference(tc, target.get());
        } else {
            SourceRange range = tc.tokenRanges().getOrDefault("typeName", tc.sourceRange());
            collector.error(DiagnosticCategory.TYPE_NOT_FOUND, range, name,
                "Type '" + name + "' not found", List.of());
        }
    }

    /** Check if an AST node represents a type (data type, enum, choice, basic, record, alias). */
    private static boolean isTypeNode(RRootElement node) {
        return node instanceof RDataType
            || node instanceof REnumeration
            || node instanceof RChoice
            || node instanceof RBasicType
            || node instanceof RRecordType
            || node instanceof RTypeAlias;
    }

    /**
     * Returns the declared local name of a resolved type-like node, dispatching on
     * the six known type-node subtypes (matches {@link #isTypeNode}). Falls back to
     * a source-text-derived local name for non-type-like nodes (rare — the caller
     * should already have filtered via isTypeNode).
     *
     * <p>Per R11-3: needed because rune-dsl supports single-type aliased imports
     * (e.g. {@code import com.foo.Bar as MyType}, then {@code MyType} in a type
     * call). The alias appears in source text but the registered name in the
     * namespace is {@code Bar}. Using the resolved target's actual declared name
     * keeps the SymbolId aligned with what {@code namespace.lookup} would find.
     */
    private static String typeNodeName(RRootElement node, String sourceName) {
        if (node instanceof RDataType x) return x.name();
        if (node instanceof REnumeration x) return x.name();
        if (node instanceof RChoice x) return x.name();
        if (node instanceof RBasicType x) return x.name();
        if (node instanceof RRecordType x) return x.name();
        if (node instanceof RTypeAlias x) return x.name();
        // Fallback for any future type-node kind that slipped past isTypeNode:
        // strip qualified prefix from source text. R7-1's heuristic.
        int lastDot = sourceName.lastIndexOf('.');
        return lastDot >= 0 ? sourceName.substring(lastDot + 1) : sourceName;
    }

    /**
     * Resolve a name to a type-like node, skipping non-type declarations.
     * Uses allMatching to find type declarations that may be shadowed by
     * functions with the same name.
     */
    private Optional<RRootElement> resolveTypeNodeByName(String name, RFileScope scope) {
        // PR #445 — TWO-PASS type-position walk, the exact analogue of
        // RFileScope.lookup's shape (see its javadoc for the namespace-layout
        // drift this handles): pass 1 admits only same-file (tier 0) then
        // same-cell (tier 1) type nodes across own + imports; pass 2 is the
        // historical registration-order walk.
        Optional<RRootElement> sameCell = Optional.empty();
        for (RRootElement candidate : scope.ownNamespace().allMatching(name)) {
            if (!isTypeNode(candidate)) continue;
            if (com.regnosys.rosetta.symbols.CellPreference.modelOf(candidate) == scope.model()) {
                return Optional.of(candidate);
            }
            if (sameCell.isEmpty() && cellPreference.prefers(candidate, scope.model())) {
                sameCell = Optional.of(candidate);
            }
        }
        for (var imp : scope.imports()) {
            for (RRootElement candidate : imp.allMatching(name)) {
                if (!isTypeNode(candidate)) continue;
                if (com.regnosys.rosetta.symbols.CellPreference.modelOf(candidate) == scope.model()) {
                    return Optional.of(candidate);
                }
                if (sameCell.isEmpty() && cellPreference.prefers(candidate, scope.model())) {
                    sameCell = Optional.of(candidate);
                }
            }
        }
        if (sameCell.isPresent()) return sameCell;
        // Pass 2 — the historical walk. Check own namespace for all
        // declarations with this name
        for (RRootElement candidate : scope.ownNamespace().allMatching(name)) {
            if (isTypeNode(candidate)) return Optional.of(candidate);
        }
        // Check imported namespaces — walk ALL matches per import (not just
        // first), so a non-type-node like a rule that shares its name with a
        // type in the same imported namespace doesn't shadow the type.
        // Example: namespace `drr.enrichment.common` declares both
        // `reporting rule EnrichmentData` and `type EnrichmentData`; a
        // type-position reference to `EnrichmentData` from `drr.base.trade`
        // (which imports `drr.enrichment.common.*`) must reach the type even
        // when `lookup` returned the rule first.
        for (var imp : scope.imports()) {
            for (RRootElement candidate : imp.allMatching(name)) {
                if (isTypeNode(candidate)) return Optional.of(candidate);
            }
        }
        // Check builtins
        if (scope.builtinNamespace() != null) {
            for (RRootElement candidate : scope.builtinNamespace().allMatching(name)) {
                if (isTypeNode(candidate)) return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private void resolveDataSuperType(RDataType dt, RFileScope scope, Diagnostics collector) {
        Optional<String> nameOpt = dt.superTypeName();
        if (nameOpt.isEmpty()) return;
        String name = nameOpt.get();

        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        if (target.isPresent() && target.get() instanceof RDataType resolved) {
            dt.setSuperTypeId(symbolIdOf(resolved, resolved.name()));
            subTypeIndex.registerInheritance(dt, resolved);
            referenceIndex.registerReference(dt, resolved);
        } else if (target.isPresent() && target.get() instanceof com.regnosys.rosetta.ast.types.RChoice resolved) {
            // Choice types are valid super types for data types
            // (upstream: caseChoiceType → caseDataType — choice types generate as POJO interfaces)
            // Migrated from direct pointer to SymbolId in Task 4.2 (P1.4.1b — H7 / U005).
            dt.setChoiceSuperTypeId(symbolIdOf(resolved, resolved.name()));
            referenceIndex.registerReference(dt, resolved);
        } else {
            SourceRange range = dt.tokenRanges().getOrDefault("superType", dt.sourceRange());
            collector.error(DiagnosticCategory.SUPER_TYPE_NOT_FOUND, range, name,
                "Super type '" + name + "' not found", List.of());
        }
    }

    /**
     * Builds a {@link SymbolId} from a resolved
     * root element + its local name. Looks up the element's containing namespace
     * via linear scan over {@link #namespaces} (every {@link RRootElement} is
     * registered in exactly one namespace during pass 1).
     *
     * <p>Caller passes {@code localName} explicitly — {@link RRootElement} does
     * not define a {@code name()} method on the abstract base, so we don't try
     * to reflect or cast through the hierarchy. The caller has already cast
     * {@code target} to a concrete subtype (e.g. {@code RDataType resolved}) and
     * knows the name, so passing it through is type-safe and zero-overhead.
     *
     * <p>Builtin types (e.g., {@code string}, {@code int}) are NOT in the
     * user-facing {@link #namespaces} map — they live in
     * {@link RFileScope#builtinNamespace()}. For builtin targets, this method
     * returns a {@link SymbolId} with the sentinel
     * namespace {@link SymbolResolver#BUILTIN_NAMESPACE}
     * ({@code "<builtin>"}); {@link RWorkspace#resolve}
     * routes that sentinel to {@code builtinNamespace()}. Per SF8.
     *
     * <p>Cost note: the linear scan over {@code namespaces} is O(N_namespaces)
     * per cross-ref. Acceptable for P1.4.1b; D5 wall-clock budget gate catches
     * regressions. Escalation path: a {@code Map<RRootElement, String>}
     * reverse-index built once in pass 1 — documented in U005.
     */
    private SymbolId symbolIdOf(RRootElement target, String localName) {
        for (Map.Entry<String, RNamespaceScope> e : namespaces.entrySet()) {
            // R12-1 fix: use allMatching (returns ALL declarations bound to
            // localName) instead of lookup (first-wins). Required for cases
            // where a function and a type share the same local name in the
            // same namespace (e.g. CreditSupportAmount): resolveTypeCall's
            // type-filter at lines 134-138 can pick the non-first match,
            // and `lookup(localName) == target` would then be false even
            // though the target IS in this namespace — symbolIdOf would
            // incorrectly fall through to BUILTIN_NAMESPACE.
            if (e.getValue().allMatching(localName).stream().anyMatch(t -> t == target)) {
                return SymbolId.of(e.getKey(), localName, generation);
            }
        }
        // Not in any user namespace → must be a builtin (RBasicType / RRecordType
        // resolved against scope.builtinNamespace()). Mark with sentinel namespace.
        return SymbolId.of(SymbolResolver.BUILTIN_NAMESPACE, localName, generation);
    }

    private void resolveEnumSuperType(REnumeration en, RFileScope scope, Diagnostics collector) {
        Optional<String> nameOpt = en.superTypeName();
        if (nameOpt.isEmpty()) return;
        String name = nameOpt.get();

        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        if (target.isPresent() && target.get() instanceof REnumeration resolved) {
            en.setSuperTypeId(symbolIdOf(resolved, resolved.name()));
            subTypeIndex.registerInheritance(en, resolved);
            referenceIndex.registerReference(en, resolved);
        } else {
            SourceRange range = en.tokenRanges().getOrDefault("superType", en.sourceRange());
            collector.error(DiagnosticCategory.SUPER_TYPE_NOT_FOUND, range, name,
                "Super enum '" + name + "' not found", List.of());
        }
    }

    private void resolveSuperFunction(RFunction fn, RFileScope scope, Diagnostics collector) {
        Optional<String> nameOpt = fn.superFunctionName();
        if (nameOpt.isEmpty()) return;
        String name = nameOpt.get();

        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        if (target.isPresent() && target.get() instanceof RFunction resolved) {
            fn.setSuperFunctionId(symbolIdOf(resolved, resolved.name()));
            referenceIndex.registerReference(fn, resolved);
        } else {
            SourceRange range = fn.tokenRanges().getOrDefault("superFunction", fn.sourceRange());
            collector.error(DiagnosticCategory.SUPER_FUNCTION_NOT_FOUND, range, name,
                "Super function '" + name + "' not found", List.of());
        }
    }

    private void resolveQualifiableRoot(RQualifiableConfig qc, RFileScope scope, Diagnostics collector) {
        String name = qc.rootTypeName();
        if (name == null || name.isEmpty()) return;
        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        if (target.isPresent() && target.get() instanceof RDataType resolved) {
            qc.setRootTypeId(symbolIdOf(resolved, resolved.name()));
            referenceIndex.registerReference(qc, resolved);
        } else {
            SourceRange range = qc.tokenRanges().getOrDefault("rootTypeName", qc.sourceRange());
            collector.error(DiagnosticCategory.TYPE_NOT_FOUND, range, name,
                "Root type '" + name + "' not found", List.of());
        }
    }

    /**
     * v3.2 seat 4 (PR #625, F7): a report's {@code with type} and {@code with source} are scoped
     * cross-references — upstream {@code reportType=[Data|QualifiedName]} and
     * {@code ruleSource=[RosettaExternalRuleSource|QualifiedName]} — resolved here like every other
     * reference (the own namespace, then the imports, then a qualified name) and by KIND - the ladder the
     * rule and synonym-source references use, so a same-named element of another kind cannot shadow the
     * report type (round 1: the kind-blind lookup returned a reporting rule declared before the type it
     * shares a name with, a false TYPE_NOT_FOUND and both report seats refused; the released plugin
     * resolves it - the oracle group report-withtype-rule-shadow, 15 goldens) - never by a
     * workspace-wide search on the simple name at codegen time: that search handed the chaos s07
     * report functions the FIRST namespace's report type and rules. An unresolved reference is the
     * linker's error; the generator refuses rather than guess.
     *
     * <p>Round 2 (the code-quality review's SF-1), said plainly: the {@code with type} gate is {@code RDataType},
     * a strict SUBSET of upstream's {@code Data} EClass — in the released 9.83.0 metamodel {@code Choice extends
     * Data} ({@code javap} on the released {@code rune-lang} jar: {@code interface Choice extends Data}), so the
     * released plugin RESOLVES a {@code with type} naming a {@code choice} where this gate (and the
     * {@code instanceof RDataType} bind below it, from the fix's first cut on) records {@code TYPE_NOT_FOUND} and
     * both report seats refuse; the fork's {@code RChoice} is not an {@code RDataType}, and the report seats type
     * the synthetic output as a data type. Carrier-free — MEASURED at round 3 over a JUNCTION-FOLLOWING walk of the whole
     * corpus (the code-quality review's MF-1: the round-2 "twelve" had walked the chaos cell alone, the vendored cells under
     * {@code test-corpus/} being junctions): 254 report {@code with type} declarations across the eleven cells that carry
     * them (chaos 12, drr 242 — 31 distinct target names), 14 {@code choice} names declared, the intersection EMPTY: no
     * corpus {@code with type} names a choice on any cell ({@code target/v32-seat4-instruments/scratch/withtype-census.txt},
     * local — the walk's own print, its method stated); BANKED with its oracle group named — {@code report-withtype-choice} —
     * the heal being a
     * report-seat question (a choice output's rule traversal), not a linker one. The {@code with source} gate is
     * exact: {@code RosettaExternalRuleSource} has no subclass. An enum named by {@code with type} errors on both
     * sides.
     */
    private void resolveReport(RReport report, RFileScope scope, Diagnostics collector) {
        String typeName = report.withType();
        if (typeName != null && !typeName.isEmpty()) {
            Optional<RRootElement> target = resolveQualifiedOrLocalOfKind(typeName, scope, RDataType.class::isInstance);
            if (target.isPresent() && target.get() instanceof RDataType resolved) {
                report.setWithTypeId(symbolIdOf(resolved, resolved.name()));
                referenceIndex.registerReference(report, resolved);
            } else {
                SourceRange range = report.tokenRanges().getOrDefault("withType", report.sourceRange());
                collector.error(DiagnosticCategory.TYPE_NOT_FOUND, range, typeName,
                    "Type '" + typeName + "' not found", List.of());
            }
        }
        String sourceName = report.withSource().orElse(null);
        if (sourceName != null && !sourceName.isEmpty()) {
            Optional<RRootElement> target = resolveQualifiedOrLocalOfKind(sourceName, scope, RExternalRuleSource.class::isInstance);
            if (target.isPresent() && target.get() instanceof RExternalRuleSource resolved) {
                report.setWithSourceId(symbolIdOf(resolved, resolved.name()));
                referenceIndex.registerReference(report, resolved);
            } else {
                SourceRange range = report.tokenRanges().getOrDefault("withSource", report.sourceRange());
                collector.error(DiagnosticCategory.EXTERNAL_SOURCE_NOT_FOUND, range, sourceName,
                    "External source '" + sourceName + "' not found", List.of());
            }
        }
    }

    // === T9 resolver methods ================================================

    private void resolveAnnotationRef(RAnnotationRef ar, RFileScope scope, Diagnostics collector) {
        String name = ar.annotationName();
        if (name == null || name.isEmpty()) return;
        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        if (target.isPresent() && target.get() instanceof RAnnotation resolved) {
            ar.setResolvedAnnotation(resolved);
            referenceIndex.registerReference(ar, resolved);

            // Qualifier resolution: qualifierName refers to an attribute name
            // on the resolved annotation (e.g. [metadata key] → "key" is an
            // attribute on the "metadata" annotation declaration).
            ar.qualifierName().ifPresent(qName -> {
                for (var attr : resolved.attributes()) {
                    if (qName.equals(attr.name())) {
                        ar.setResolvedQualifier(attr);
                        referenceIndex.registerReference(ar, attr);
                        return;
                    }
                }
                collector.error(DiagnosticCategory.ANNOTATION_QUALIFIER_NOT_FOUND,
                    ar.sourceRange(), qName,
                    "Annotation qualifier '" + qName + "' not found on '" + name + "'",
                    List.of());
            });
        } else {
            collector.error(DiagnosticCategory.ANNOTATION_NOT_FOUND,
                ar.sourceRange(), name,
                "Annotation '" + name + "' not found", List.of());
        }
    }

    private void resolveEnumValueRef(REnumValueRef evr, RFileScope scope, Diagnostics collector) {
        String enumName = evr.enumName();
        if (enumName == null || enumName.isEmpty()) return;

        // Step 1: resolve the enum
        Optional<RRootElement> enumTarget = resolveQualifiedOrLocal(enumName, scope);
        if (enumTarget.isEmpty() || !(enumTarget.get() instanceof REnumeration resolvedEnum)) {
            // T0h Gap A: LHS may be a callable symbol (RFunction or RRule). The
            // grammar parses `<func-name> -> <feature>` as REnumValueRef but
            // semantically it's a func-call result + feature access. Capture the
            // resolved symbol so TypeInferenceEngine.runTypeDirectedResolution
            // can complete the chain by looking up valueName as a feature on the
            // symbol's output type. Skips ENUM_NOT_FOUND in this case so the
            // diagnostic surface stays clean (the engine binds the chain or
            // surfaces a feature-not-found further down).
            if (enumTarget.isPresent()
                    && (enumTarget.get() instanceof RFunction
                        || enumTarget.get() instanceof RRule)) {
                evr.setResolvedSymbol(enumTarget.get());
                referenceIndex.registerReference(evr, enumTarget.get());
                return;
            }
            // T0i Gap B: RHS may be a workspace type (type-restriction / downcast).
            // The grammar parses `<attribute> -> <SubtypeName>` as REnumValueRef
            // but semantically it's a type-restriction on the LHS attribute.
            // Speculatively bind the resolved type here; Phase B's
            // TypeInferenceEngine Cat 12 completes the resolution by looking up
            // LHS as an attribute on item-type + verifying the subtype relation
            // against LHS's element type. If Phase B succeeds, it binds the
            // verified {@link com.regnosys.rosetta.ast.expressions.references.REnumValueRef.TypeRestriction}
            // record + registers the cross-ref + clears ENUM_NOT_FOUND. If
            // Phase B fails verification (LHS not an attribute on item-type, or
            // RHS not a subtype), the speculative bind remains as metadata but
            // ENUM_NOT_FOUND stays in place (no silent failure for genuine
            // typos like `foo -> Trade` where Trade is a real type but foo is
            // not an attribute on item-type). Downstream type computation reads
            // ONLY the verified record. Differs from the T0h Gap A pattern
            // above: Gap A's bind is conclusive (LHS resolves to a callable
            // symbol definitively), so Gap A skips ENUM_NOT_FOUND + registers
            // cross-ref at Phase A. Gap B's speculation must be verified before
            // either side-effect fires.
            String valueName = evr.valueName();
            if (valueName != null && !valueName.isEmpty()) {
                Optional<RRootElement> typeTarget = resolveQualifiedOrLocal(valueName, scope);
                if (typeTarget.isPresent() && typeTarget.get() instanceof RDataType resolvedType) {
                    evr.setResolvedRestrictionType(resolvedType);
                    // Do NOT register cross-ref or skip ENUM_NOT_FOUND here —
                    // defer both to Phase B Cat 12 on verified bind.
                }
            }
            collector.error(DiagnosticCategory.ENUM_NOT_FOUND,
                evr.sourceRange(), enumName,
                "Enum '" + enumName + "' not found", List.of());
            return;
        }
        evr.setResolvedEnum(resolvedEnum);
        referenceIndex.registerReference(evr, resolvedEnum);

        // Step 2: resolve the value within the enum — the supertype chain
        // included (PR #444). Upstream scopes ROSETTA_ENUM_VALUE_REFERENCE__VALUE
        // against buildREnumType(...).getAllEnumValues() — own AND inherited
        // values (vendored RosettaScopeProvider:233-236); DRR leans on this
        // (PartyIdentifierFormatEnum extends PartyIdentifierFormat2Enum extends
        // LeiIdentifierFormatEnum, with `Lei` declared two levels up). Own
        // values first, then the chain — the same walk
        // TypeDirectedResolver.findEnumValue uses; identity-guarded against
        // cyclic extends chains (which that seat leaves unguarded — a cycle
        // there recurses; here the guard makes the walk total).
        String valueName = evr.valueName();
        if (valueName == null || valueName.isEmpty()) return;
        java.util.Set<REnumeration> seen =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (REnumeration en = resolvedEnum; en != null && seen.add(en);
                en = superEnumOf(en)) {
            for (REnumValue val : en.values()) {
                if (valueName.equals(val.name())) {
                    evr.setResolvedValue(val);
                    referenceIndex.registerReference(evr, val);
                    return;
                }
            }
        }
        collector.error(DiagnosticCategory.ENUM_VALUE_NOT_FOUND,
            evr.sourceRange(), valueName,
            "Enum value '" + valueName + "' not found in " + resolvedEnum.name(), List.of());
    }

    /**
     * The direct super-enum of {@code en}, resolved PASS-SAFELY: each hop's
     * {@code superTypeName} resolves in the hop's OWN declaring file's scope —
     * exactly the resolution {@link #resolveEnumSuperType} performs for that
     * declaration ({@code REnumeration.superType()} itself is unusable here:
     * it resolves through {@code workspace()}, which only attaches after the
     * linker passes complete — the seed run's fail-loud IllegalState). Returns
     * {@code null} at a chain end, an unresolvable hop, or a hop declared in a
     * file outside this build's scope map.
     */
    private REnumeration superEnumOf(REnumeration en) {
        Optional<String> nameOpt = en.superTypeName();
        if (nameOpt.isEmpty()) return null;
        RNode p = en.parent();
        while (p != null && !(p instanceof RModel)) {
            p = p.parent();
        }
        if (!(p instanceof RModel model)) return null;
        RFileScope scope = fileScopes.get(model);
        if (scope == null) return null;
        Optional<RRootElement> target = resolveQualifiedOrLocal(nameOpt.get(), scope);
        if (target.isPresent() && target.get() instanceof REnumeration parent) {
            return parent;
        }
        return null;
    }

    private void resolveConversionTarget(RConversionExpr cv, RFileScope scope, Diagnostics collector) {
        Optional<String> nameOpt = cv.targetEnumName();
        if (nameOpt.isEmpty()) return;
        String name = nameOpt.get();
        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        if (target.isPresent() && target.get() instanceof REnumeration resolved) {
            cv.setResolvedTargetEnum(resolved);
            referenceIndex.registerReference(cv, resolved);
        } else {
            collector.error(DiagnosticCategory.ENUM_NOT_FOUND,
                cv.sourceRange(), name,
                "Target enum '" + name + "' not found", List.of());
        }
    }

    private void resolveRuleRef(RRuleReferenceAnnotation rr, RFileScope scope, Diagnostics collector) {
        Optional<String> nameOpt = rr.ruleName();
        if (nameOpt.isEmpty()) return;
        String name = nameOpt.get();
        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        // PR #450: a ruleReference position is a RULE position — upstream's
        // grammar types the cross-ref [RosettaRule|QualifiedName]
        // (Rosetta.xtext:906), so its scope candidates are rules ONLY and a
        // same-named type is never admissible (the D40 type-vs-rule
        // invariant's ruleReference seat). Corpus witness: iosco
        // `[ruleReference payment.OtherPayment]` — the attribute's TYPE lives
        // in ...cde.base.payment (reached FIRST through the import-ordered
        // wildcard walk) while the rule lives in ...cde.versionN.payment
        // (a LATER import), so the kind-blind walk returned the type and the
        // post-check errored. When the generic lookup lands a non-rule,
        // re-resolve rule-kind-filtered through the same strategy ladder.
        if (target.isPresent() && !(target.get() instanceof RRule)) {
            Optional<RRootElement> ruleTarget =
                resolveQualifiedOrLocalOfKind(name, scope, RRule.class::isInstance);
            if (ruleTarget.isPresent()) {
                target = ruleTarget;
            }
        }
        if (target.isPresent() && target.get() instanceof RRule resolved) {
            rr.setResolvedRule(resolved);
            referenceIndex.registerReference(rr, resolved);
        } else {
            collector.error(DiagnosticCategory.RULE_NOT_FOUND,
                rr.sourceRange(), name,
                "Rule '" + name + "' not found", List.of());
        }
    }

    private void resolveSegmentRef(RSegmentRef sr, RFileScope scope, Diagnostics collector) {
        String name = sr.segmentName();
        if (name == null || name.isEmpty()) return;
        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        if (target.isPresent() && target.get() instanceof RSegmentDef resolved) {
            sr.setResolvedSegment(resolved);
            referenceIndex.registerReference(sr, resolved);
        } else {
            collector.error(DiagnosticCategory.SEGMENT_DEFINITION_NOT_FOUND,
                sr.sourceRange(), name,
                "Segment definition '" + name + "' not found", List.of());
        }
    }

    private void resolveExternalClass(RExternalClass ec, RFileScope scope, Diagnostics collector) {
        String name = ec.typeName();
        if (name == null || name.isEmpty()) return;
        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        // PR #445: an external class position is a TYPE position — when the
        // generic lookup lands a same-named FUNCTION (CalculationPeriod,
        // ReturnAmount, DeliveryAmount — the resolveTypeCall :150 shadowing
        // class at this seat), re-resolve preferring type nodes. Upstream's
        // builds resolve every corpus carrier (the V0 oracles are error-free),
        // so the shadowed diagnostic was fork-native.
        if (target.isPresent() && !isTypeNode(target.get())) {
            Optional<RRootElement> typeTarget = resolveTypeNodeByName(name, scope);
            if (typeTarget.isPresent()) {
                target = typeTarget;
            }
        }
        if (target.isPresent() && target.get() instanceof RDataType resolved) {
            ec.setResolvedType(resolved);
            referenceIndex.registerReference(ec, resolved);
        } else if (target.isPresent() && target.get() instanceof RChoice resolvedChoice) {
            // PR #445: a CHOICE-typed external class (CDM 6's restructured
            // Payout/Product/Underlier/Asset… carry external synonyms in the
            // cdm6 mapping files). Upstream scopes external classes over data
            // AND choice declarations (choice-as-data) and its 6.20.6 build
            // is error-free over every carrier; the same-cell preference now
            // resolves these refs to the c6 choice where the merged space's
            // first-wins pick used to hand back the c5 data type — the kind
            // gate must admit what upstream admits. Parallel field: see
            // RExternalClass.referencedChoice().
            ec.setResolvedChoice(resolvedChoice);
            referenceIndex.registerReference(ec, resolvedChoice);
        } else {
            collector.error(DiagnosticCategory.EXTERNAL_TYPE_NOT_FOUND,
                ec.tokenRanges().getOrDefault("typeName", ec.sourceRange()), name,
                "External class type '" + name + "' not found", List.of());
        }
    }

    private void resolveExternalEnum(RExternalEnum ee, RFileScope scope, Diagnostics collector) {
        String name = ee.typeName();
        if (name == null || name.isEmpty()) return;
        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        if (target.isPresent() && target.get() instanceof REnumeration resolved) {
            ee.setResolvedType(resolved);
            referenceIndex.registerReference(ee, resolved);
        } else {
            collector.error(DiagnosticCategory.EXTERNAL_TYPE_NOT_FOUND,
                ee.tokenRanges().getOrDefault("typeName", ee.sourceRange()), name,
                "External enum type '" + name + "' not found", List.of());
        }
    }

    private void resolveMapTestFunc(RMapTestFunc mtf, RFileScope scope, Diagnostics collector) {
        String name = mtf.funcName();
        if (name == null || name.isEmpty()) return;
        Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
        if (target.isPresent() && target.get() instanceof RFunction resolved) {
            mtf.setResolvedFunction(resolved);
            referenceIndex.registerReference(mtf, resolved);
        } else {
            collector.error(DiagnosticCategory.FUNCTION_NOT_FOUND,
                mtf.sourceRange(), name,
                "Function '" + name + "' not found", List.of());
        }
    }

    private void resolveExternalSynonymSourceExtends(
            RExternalSynonymSource ess, RFileScope scope, Diagnostics collector) {
        List<String> names = ess.superSourceNames();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
            // PR #450: upstream's extends cross-ref is typed
            // [RosettaSynonymSource|QualifiedName] (Rosetta.xtext:821) and
            // RosettaExternalSynonymSource EXTENDS RosettaSynonymSource in
            // the Ecore model (Rosetta.xcore:480) — the plain body-less form
            // and the external form are BOTH admissible targets. Corpus
            // witness: `synonym source FIS extends FIS_BASE` where FIS_BASE
            // is the plain form declared two lines above (cdm5/cdm6
            // mapping-fis-synonym.rosetta; likewise FpML / ORE / CreateiQ).
            // The fork's admission previously required the external form
            // only, so every plain-form target errored. RExternalSynonymSource
            // now extends RSynonymSource (the upstream hierarchy mirror), and
            // the admission is the supertype. The kind-filtered re-resolve
            // guards the shadow case exactly like the ruleReference seat
            // (upstream's ECLASS-typed scope never sees a non-source
            // candidate; no corpus row needs it today).
            if (target.isPresent() && !(target.get() instanceof RSynonymSource)) {
                Optional<RRootElement> sourceTarget =
                    resolveQualifiedOrLocalOfKind(name, scope, RSynonymSource.class::isInstance);
                if (sourceTarget.isPresent()) {
                    target = sourceTarget;
                }
            }
            if (target.isPresent() && target.get() instanceof RSynonymSource resolved) {
                ess.addResolvedSuperSource(resolved);
                referenceIndex.registerReference(ess, resolved);
            } else {
                SourceRange range = ess.tokenRanges().get("superSource_" + i);
                if (range == null) range = ess.sourceRange();
                collector.error(DiagnosticCategory.EXTERNAL_SOURCE_NOT_FOUND, range, name,
                    "External source '" + name + "' not found", List.of());
            }
        }
    }

    private void resolveExternalRuleSourceExtends(
            RExternalRuleSource ers, RFileScope scope, Diagnostics collector) {
        List<String> names = ers.superSourceNames();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            Optional<RRootElement> target = resolveQualifiedOrLocal(name, scope);
            if (target.isPresent() && target.get() instanceof RExternalRuleSource resolved) {
                ers.addResolvedSuperSource(resolved);
                referenceIndex.registerReference(ers, resolved);
            } else {
                SourceRange range = ers.tokenRanges().get("superSource_" + i);
                if (range == null) range = ers.sourceRange();
                collector.error(DiagnosticCategory.EXTERNAL_SOURCE_NOT_FOUND, range, name,
                    "External source '" + name + "' not found", List.of());
            }
        }
    }

    // === Cycle detection =====================================================

    private void detectCircularInheritance(List<RModel> files, Diagnostics collector) {
        for (RModel file : files) {
            for (RDataType dt : AstWalker.findAll(file, RDataType.class)) {
                Set<RDataType> visited = Collections.newSetFromMap(new IdentityHashMap<>());
                RDataType current = dt;
                while (current != null) {
                    if (!visited.add(current)) {
                        collector.error(DiagnosticCategory.CIRCULAR_INHERITANCE,
                            dt.sourceRange(), dt.name(),
                            "Circular inheritance detected starting at '" + dt.name() + "'",
                            List.of());
                        break;
                    }
                    // Cannot call superType() here — pass 4 itself runs BEFORE the interim
                    // attachToWorkspace step (which runs after this pass completes); the
                    // post-pass-7 re-attach to the final workspace happens later. Resolve directly
                    // via the namespace map using the SymbolId if one was set.
                    SymbolId sid = current.superTypeId().orElse(null);
                    if (sid == null) break;
                    if (SymbolResolver.BUILTIN_NAMESPACE.equals(sid.namespace())) {
                        break; // builtin super — not a user-defined type, no cycle risk
                    }
                    RNamespaceScope ns = namespaces.get(sid.namespace());
                    if (ns == null) break;
                    // R14-1 fix: use allMatching not lookup. lookup() is first-wins
                    // and can return a non-RDataType (e.g. a function with the same
                    // local name) when the namespace has type/non-type shadowing.
                    // Falling through with current=null would prematurely stop the
                    // walk and miss a real type cycle. Filter for RDataType across
                    // ALL declarations bound to this localName, picking the first
                    // RDataType match (cycle detection only cares about types).
                    // PR #445: walk in CellPreference rank order relative to the
                    // node whose superTypeId is being followed, so the detector
                    // traverses the same cell-consistent supertype graph the
                    // requester-aware lazy accessors resolve.
                    Optional<RRootElement> resolved = cellPreference.order(
                                ns.allMatching(sid.localName()),
                                com.regnosys.rosetta.symbols.CellPreference.modelOf(current))
                            .stream()
                            .filter(RDataType.class::isInstance)
                            .findFirst();
                    current = resolved.filter(RDataType.class::isInstance)
                                     .map(RDataType.class::cast)
                                     .orElse(null);
                }
            }
        }
    }

    /**
     * Resolves a qualified-or-local name against the file scope.
     * Always tries the file scope chain first (which handles aliased wildcard
     * imports like {@code import com.foo.* as f} where "f.Bar" must resolve).
     * For dotted names, falls back to direct namespace lookup if the scope
     * chain misses.
     */
    private Optional<RRootElement> resolveQualifiedOrLocal(String name, RFileScope scope) {
        // Try file scope chain first — handles unqualified names, aliased
        // wildcard imports (e.g. "f.Bar"), and aliased named imports.
        Optional<RRootElement> fromScope = scope.lookup(name);
        if (fromScope.isPresent()) return fromScope;

        // Fall back to direct namespace lookup for fully-qualified names. If
        // the prefix DOES match a registered namespace, that namespace IS the
        // authoritative scope for the local-name lookup: return whatever
        // `ns.lookup` says (present or empty). Falling through to wildcard
        // traversal when `ns.lookup` is empty would silently re-bind a broken
        // fully-qualified reference (e.g. `a.b.Missing`) to a same-named
        // symbol pulled in via an unrelated wildcard import — a correctness
        // hazard flagged by Copilot R9 F3 on PR #76 commit `5340a88`. If the
        // prefix does NOT match a registered namespace (e.g. it's an alias
        // prefix like `cdeV3.collateral` where `cdeV3` is an alias for
        // `drr.standards.iosco.cde.version2`), fall through to the
        // wildcard-import + sub-namespace traversal at L626+ — which is what
        // resolves the v3 DRR pattern.
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            String namespaceName = name.substring(0, lastDot);
            String localName = name.substring(lastDot + 1);
            RNamespaceScope ns = namespaces.get(namespaceName);
            if (ns != null) {
                // PR #445: requester-ranked pick on duplicate FQNs
                return cellPreference.pick(ns.allMatching(localName), scope.model());
            }
        }

        // Phase X1 PR #76 — aliased wildcard import + sub-namespace traversal.
        // For an import "import N.* as alias" and a reference "alias.A.B.X",
        // expand the alias prefix to N + remainder ("N.A.B.X"), then try every
        // possible split as <namespace>.<localName>. Handles the real-world
        // DRR pattern where the consumer file imports a parent namespace via
        // wildcard alias but the actual rule/function lives in a deeper
        // sub-namespace (e.g. cdeV3.collateral.X where cdeV3 aliases
        // drr.standards.iosco.cde.version2 and the rule X is registered in
        // drr.standards.iosco.cde.version2.collateral).
        //
        // Verified empirically: 207 of 444 RuleGenerator fail-fast errors on
        // drr/7.0.0-dev.113 POJO are RSymbolReference with symbol() EMPTY,
        // all using this aliased-wildcard + sub-namespace pattern. See
        // the development audit "phase-x1-T2-mvp1-ast-bucket-evidence".
        for (com.regnosys.rosetta.symbols.ImportEntry imp : scope.imports()) {
            if (!imp.isWildcard()) continue;
            // Compute the de-aliased name. Aliased wildcard ("import N.* as a"):
            // strip the alias prefix and prepend the imported namespace, so
            // "a.X.Y" → "N.X.Y". Non-aliased wildcard ("import N.*"): the name
            // itself is the relative suffix (e.g. "price.X" matches the child
            // sub-namespace "N.price"); prepend the imported namespace.
            String dealiased;
            if (imp.alias().isPresent()) {
                String aliasPrefix = imp.alias().get() + ".";
                if (!name.startsWith(aliasPrefix)) continue;
                dealiased = imp.importedNamespace() + "." + name.substring(aliasPrefix.length());
            } else {
                // Non-aliased wildcard: prepend unconditionally. If the caller
                // already passed a fully-qualified name that happens to start
                // with the imported namespace (e.g. user wrote
                // "drr.enrichment.common.X" while also importing
                // "drr.enrichment.common.*"), the doubled-prefix form will
                // miss every namespace in the workspace — a harmless miss, not
                // a wrong match. The earlier fully-qualified-name fallback at
                // L604-613 already handles that shape.
                dealiased = imp.importedNamespace() + "." + name;
            }
            // Termination: `dealiased.lastIndexOf('.', dot - 1)` returns -1 when
            // no earlier dot exists, exiting the loop. Each iteration shrinks
            // `nsName` by one segment, so the loop runs at most O(dotCount)
            // times — bounded by the depth of the de-aliased qualified name
            // (typically ≤4 for DRR's cdeV3.collateral.X shape). No
            // `visited`-set is needed: this method does not recurse, and
            // `ns.lookup` is non-recursive (it consults its own namespace's
            // declaration map only).
            for (int dot = dealiased.lastIndexOf('.'); dot >= 0; dot = dealiased.lastIndexOf('.', dot - 1)) {
                String nsName = dealiased.substring(0, dot);
                String localName = dealiased.substring(dot + 1);
                RNamespaceScope ns = namespaces.get(nsName);
                if (ns != null) {
                    Optional<RRootElement> hit =
                        cellPreference.pick(ns.allMatching(localName), scope.model());
                    if (hit.isPresent()) return hit;
                }
            }
        }

        // Phase X1 closure T10 lever 9 — CURRENT-NAMESPACE-RELATIVE qualified
        // resolution. A reference can be written RELATIVE to the current file's
        // own namespace: e.g. a func whose file namespace is
        // `drr.regulation.common` writes `output: result
        // trade.underlier.UnderlyingIdentificationTypeEnum`, where the enum
        // actually lives in `drr.regulation.common.trade.underlier` (current ns +
        // relative path = full path). None of the paths above try the current
        // namespace as an implicit prefix — the file-scope chain (L644) walks the
        // own namespace's DIRECT declarations + imports, not its sub-namespaces;
        // the fully-qualified fallback (L660) and the wildcard traversal (L684)
        // key off registered namespaces / imports, not the current namespace.
        // Prepend the file's own namespace and try every <namespace>.<localName>
        // split (rightmost-first), mirroring the wildcard loop. Runs LAST (lowest
        // priority) so it never shadows a closer match; callers that need a kind
        // filter (e.g. RTypeCall.referencedType via resolveTypeLike, D40) apply it
        // on the returned node, so the type-vs-rule disambiguation invariant is
        // preserved. Bounded O(dotCount); no recursion (ns.lookup consults one
        // namespace's own declaration map only).
        String ownNs = scope.ownNamespace().qualifiedName();
        if (ownNs != null && !ownNs.isEmpty()) {
            String relative = ownNs + "." + name;
            for (int dot = relative.lastIndexOf('.'); dot >= 0; dot = relative.lastIndexOf('.', dot - 1)) {
                String nsName = relative.substring(0, dot);
                String localName = relative.substring(dot + 1);
                RNamespaceScope ns = namespaces.get(nsName);
                if (ns != null) {
                    Optional<RRootElement> hit =
                        cellPreference.pick(ns.allMatching(localName), scope.model());
                    if (hit.isPresent()) return hit;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Kind-filtered sibling of {@link #resolveQualifiedOrLocal} — the SAME
     * four-strategy ladder (file-scope chain → fully-qualified prefix →
     * aliased/plain wildcard sub-namespace traversal → current-namespace-
     * relative splits, each strategy's narrative in the sibling's comments)
     * with every candidate pick additionally gated by {@code kind}, so a
     * kind-mismatched candidate can no longer shadow a kind-matched one at
     * ANY rung (PR #450). Upstream's scoping is ECLASS-typed per
     * cross-reference — a {@code [RosettaRule|QualifiedName]} ref never sees
     * a same-named type as a candidate at all — and this ladder is that
     * filter expressed over the fork's strategies. The witness mechanism:
     * iosco {@code payment.OtherPayment} at ruleReference position hits the
     * {@code ...cde.base.payment} namespace through an EARLIER wildcard
     * import (which holds only the same-named TYPE) before the
     * {@code ...cde.versionN.payment} namespace holding the RULE — the
     * kind-blind ladder returns the type at the first hit and never reaches
     * the rule; kind-filtering the pick empties the first hit and lets the
     * walk continue to upstream's target. {@link #resolveQualifiedOrLocal}
     * is byte-untouched; callers use this sibling only as the wrong-kind
     * fallback with their position's upstream ECLASS filter (the #445
     * resolveTypeNodeByName / #449 choiceOptionNodeOnType sibling shape).
     */
    /**
     * PR #451 — speculative switch-guard TYPE resolution (see the run-loop
     * comment). NAME guards only; already-resolved guards are honored (the
     * engine's subject-typed arms may re-store — resolution here is a
     * convenience pre-pass, not an authority). Kind gate = type-like
     * (RDataType / RChoice): upstream's data-subject guard scope filters to
     * the DATA eClass, and choices are the fork's first-class option
     * carriers at the same seat. Emits NO diagnostic on failure.
     *
     * <p>Deliberately does NOT pair the store with
     * {@code ReferenceIndex.registerReference} (the R11-F1 pairing law's
     * one documented exemption): the store is SPECULATIVE — the engine's
     * Cat-4 subject-typed arms may overwrite it (enum value / same-cell
     * option correction), and a registered edge to the superseded node
     * would go stale. Guard cross-refs join {@code findReferences()} when
     * a final-state registration seat exists (the recorded follow-on).
     */
    private void resolveSwitchGuardType(
            com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard guard,
            RFileScope scope) {
        if (guard.kind() != com.regnosys.rosetta.ast.enums.SwitchGuardKind.NAME) {
            return;
        }
        if (guard.resolvedGuard().isPresent()) {
            return;
        }
        String name = guard.qualifiedName().orElse(null);
        if (name == null || name.isEmpty()) {
            return;
        }
        resolveQualifiedOrLocalOfKind(name, scope,
                n -> n instanceof RDataType || n instanceof RChoice)
            .ifPresent(guard::setResolvedGuard);
    }

    private Optional<RRootElement> resolveQualifiedOrLocalOfKind(
            String name, RFileScope scope, Predicate<RRootElement> kind) {
        // S1 — the file scope chain, kind-gated (RFileScope.lookupOfKind is
        // lookup's two-pass walk with the same gate).
        Optional<RRootElement> fromScope = scope.lookupOfKind(name, kind);
        if (fromScope.isPresent()) return fromScope;

        // S2 — fully-qualified prefix. The authoritative-namespace hard stop
        // applies unchanged: if the prefix names a registered namespace, that
        // namespace decides — return its kind-filtered pick, present or empty.
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            String namespaceName = name.substring(0, lastDot);
            String localName = name.substring(lastDot + 1);
            RNamespaceScope ns = namespaces.get(namespaceName);
            if (ns != null) {
                return cellPreference.pick(ofKind(ns.allMatching(localName), kind), scope.model());
            }
        }

        // S3 — aliased/plain wildcard sub-namespace traversal, kind-gated
        // per split (the termination argument is the sibling's: each
        // iteration shrinks the namespace prefix by one segment).
        for (com.regnosys.rosetta.symbols.ImportEntry imp : scope.imports()) {
            if (!imp.isWildcard()) continue;
            String dealiased;
            if (imp.alias().isPresent()) {
                String aliasPrefix = imp.alias().get() + ".";
                if (!name.startsWith(aliasPrefix)) continue;
                dealiased = imp.importedNamespace() + "." + name.substring(aliasPrefix.length());
            } else {
                dealiased = imp.importedNamespace() + "." + name;
            }
            for (int dot = dealiased.lastIndexOf('.'); dot >= 0; dot = dealiased.lastIndexOf('.', dot - 1)) {
                String nsName = dealiased.substring(0, dot);
                String localName = dealiased.substring(dot + 1);
                RNamespaceScope ns = namespaces.get(nsName);
                if (ns != null) {
                    Optional<RRootElement> hit =
                        cellPreference.pick(ofKind(ns.allMatching(localName), kind), scope.model());
                    if (hit.isPresent()) return hit;
                }
            }
        }

        // S4 — current-namespace-relative splits, kind-gated (runs LAST so it
        // never shadows a closer match, exactly like the sibling).
        String ownNs = scope.ownNamespace().qualifiedName();
        if (ownNs != null && !ownNs.isEmpty()) {
            String relative = ownNs + "." + name;
            for (int dot = relative.lastIndexOf('.'); dot >= 0; dot = relative.lastIndexOf('.', dot - 1)) {
                String nsName = relative.substring(0, dot);
                String localName = relative.substring(dot + 1);
                RNamespaceScope ns = namespaces.get(nsName);
                if (ns != null) {
                    Optional<RRootElement> hit =
                        cellPreference.pick(ofKind(ns.allMatching(localName), kind), scope.model());
                    if (hit.isPresent()) return hit;
                }
            }
        }
        return Optional.empty();
    }

    /** The kind gate for a candidate list — order-preserving filter. */
    private static List<RRootElement> ofKind(List<RRootElement> candidates, Predicate<RRootElement> kind) {
        List<RRootElement> out = new ArrayList<>();
        for (RRootElement c : candidates) {
            if (kind.test(c)) out.add(c);
        }
        return out;
    }
}
