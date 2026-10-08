package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationQualifier;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.external.RExternalClass;
import com.regnosys.rosetta.ast.external.RExternalEnum;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.functions.RDispatch;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RImport;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.model.RQualifiableConfig;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.synonyms.RSynonymSource;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates import declarations (facet warningFamilyWaves, PR #455):
 * - Unused imports warn ({@code Unused import <namespace>})
 * - Duplicate imports warn ({@code Duplicate import <namespace>})
 *
 * <p>Upstream: {@code RosettaSimpleValidator.checkImport} +
 * {@code ImportManagementService.findUnused}/{@code findDuplicates}
 * (released-jar-verified {@code warning} for both; messages byte-aligned to
 * the released recipes {@code Unused import } /
 * {@code Duplicate import }, the namespace printed exactly as written
 * — wildcard star included).
 *
 * <p>The unused computation mirrors upstream's: collect the fully-qualified
 * names of every RESOLVED cross-referenced ROOT element in the file
 * (upstream {@code eAllContents().eCrossReferences()} filtered to resolved
 * {@code RosettaRootElement} targets — the fork walks its reference-node
 * classes and keeps targets that are {@link RRootElement}s), then an import
 * is used iff some collected name matches (wildcard: the import's namespace
 * equals the target QN's leading segments; exact: the whole QN). The drr
 * bank carries 3 lines (the hkma-rewrite-trade-func file); cdm carries
 * none.
 *
 * <p>This validator operates at the model level via {@link #validateModel},
 * called by {@link com.regnosys.rosetta.validation.ValidationPass}.
 */
public final class ImportValidator implements Validator {

    /**
     * The workspace symbol table (namespace → scope), injected at build time
     * (PR #455): the fork keeps doc-reference body/corpus names and synonym
     * SOURCE names as plain strings (no resolved cross-ref fields), while
     * upstream resolves them — so their import-usage contribution is
     * approximated by a name-in-imported-namespace lookup against this table
     * (an over-approximation only when the same name exists in several
     * imported namespaces — an under-fire-only skew on the unused verdict).
     */
    private final java.util.Map<String, com.regnosys.rosetta.symbols.RNamespaceScope> namespaces;

    public ImportValidator(
            java.util.Map<String, com.regnosys.rosetta.symbols.RNamespaceScope> namespaces) {
        this.namespaces = namespaces;
    }

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        // Import validation operates at model level, not root element level.
        // See validateModel() — called by ValidationPass separately.
    }

    /**
     * Validates imports at the model level. Called by ValidationPass
     * once per file, before per-element validation.
     */
    public void validateModel(RModel model, ValidationCollector collector) {
        checkUnusedImports(model, collector);
        checkDuplicateImports(model, collector);
    }

    private void checkUnusedImports(RModel model, ValidationCollector collector) {
        if (model.imports().isEmpty()) {
            return;
        }
        Set<String> usedNames = collectUsedRootNames(model);
        Set<String> stringRefNames = collectStringRefNames(model);
        for (RImport imp : model.imports()) {
            if (imp.qualifiedName() == null) {
                continue;
            }
            boolean used;
            if (imp.isWildcard()) {
                String prefix = imp.qualifiedName() + ".";
                used = usedNames.stream()
                        .anyMatch(n -> n.startsWith(prefix) || n.equals(imp.qualifiedName()));
                if (!used) {
                    var scope = namespaces.get(imp.qualifiedName());
                    used = scope != null && stringRefNames.stream()
                            .anyMatch(n -> scope.lookup(n).isPresent());
                }
                // An ALIAS-qualified string ref (`cdeV1.CDE` in a
                // regulatoryReference corpus position — the iosco standards
                // files) resolves through the aliased import upstream.
                if (!used && imp.alias().isPresent()) {
                    String aliasPrefix = imp.alias().get() + ".";
                    used = stringRefNames.stream().anyMatch(n -> n.startsWith(aliasPrefix));
                }
            } else {
                used = usedNames.contains(imp.qualifiedName());
                if (!used) {
                    int cut = imp.qualifiedName().lastIndexOf('.');
                    if (cut > 0) {
                        String ns = imp.qualifiedName().substring(0, cut);
                        String local = imp.qualifiedName().substring(cut + 1);
                        var scope = namespaces.get(ns);
                        used = scope != null && stringRefNames.contains(local)
                                && scope.lookup(local).isPresent();
                    }
                }
            }
            if (!used) {
                // Anchored at the imported-namespace token (upstream's
                // importedNamespace feature — column 8 after `import `, per
                // the mojo-seam stream measure; PR #458 anchor wave).
                collector.warning(
                    imp.tokenRanges().getOrDefault("name", imp.sourceRange()),
                    "Unused import " + importedNamespaceText(imp),
                    ValidationIssueCode.UNUSED_IMPORT);
            }
        }
    }

    /**
     * Names the fork keeps as UNRESOLVED strings but upstream resolves as
     * cross-refs: doc-reference bodies + corpora and synonym SOURCE names.
     * Consumed by the name-in-imported-namespace approximation (see the
     * {@code namespaces} field note).
     */
    private Set<String> collectStringRefNames(RModel model) {
        Set<String> names = new HashSet<>();
        for (RRootElement root : model.rootElements()) {
            for (var docRef : AstWalker.findAll(root,
                    com.regnosys.rosetta.ast.regulatory.RRegulatoryDocumentReference.class)) {
                if (docRef.bodyRef() != null) {
                    names.add(docRef.bodyRef());
                }
                names.addAll(docRef.corpusRefs());
            }
            for (var syn : AstWalker.findAll(root,
                    com.regnosys.rosetta.ast.synonyms.RSynonym.class)) {
                names.addAll(syn.sources());
            }
            for (var syn : AstWalker.findAll(root,
                    com.regnosys.rosetta.ast.synonyms.RClassSynonym.class)) {
                names.addAll(syn.sources());
            }
            for (var syn : AstWalker.findAll(root,
                    com.regnosys.rosetta.ast.synonyms.REnumSynonym.class)) {
                names.addAll(syn.sources());
            }
            // A metadata address path head (`[metadata address
            // "pointsTo"=PriceQuantity->price]` — upstream cross-refs the
            // head TYPE; the fork keeps the path as a string qualifier).
            for (var q : AstWalker.findAll(root, RAnnotationQualifier.class)) {
                if (q.isAttributeRef() && q.value() != null) {
                    int arrow = q.value().indexOf("->");
                    names.add((arrow > 0 ? q.value().substring(0, arrow) : q.value()).trim());
                }
            }
            // An external class/enum row whose target the fork did not
            // resolve (the choice-typed `Asset:` row in mapping-dtcc-synonym
            // — RExternalClass's resolved field is RDataType-typed and a
            // CHOICE target stays empty; upstream's Data-typed cross-ref
            // resolves it, Choice extends Data in Xcore). The name lookup
            // recovers the usage; the resolver widening is a recorded
            // follow-on.
            for (RExternalClass ec : AstWalker.findAll(root, RExternalClass.class)) {
                if (ec.referencedType().isEmpty() && ec.typeName() != null) {
                    names.add(ec.typeName());
                }
            }
            for (RExternalEnum ee : AstWalker.findAll(root, RExternalEnum.class)) {
                if (ee.referencedType().isEmpty() && ee.typeName() != null) {
                    names.add(ee.typeName());
                }
            }
        }
        return names;
    }

    /**
     * The fully-qualified names ({@code namespace.Name}) of every resolved
     * root-element target referenced anywhere in the file. The reference-node
     * inventory: symbol references (callables, bare enum heads, globals),
     * enum value references (the enumeration + any root-shaped resolved
     * symbol), type calls (attribute/input/output/constructor/extends types),
     * rule reference annotations, dispatch values (their declaring
     * enumeration — upstream's dispatch value ref carries the enumeration
     * cross-ref), switch case NAME guards (the #451 kind-gated store),
     * annotation references, external class/enum targets, synonym-source
     * supers, function extension supers, and qualifiable-config root types.
     * Non-root targets (attributes, enum values, aliases, parameters) are
     * filtered exactly as upstream's {@code instanceof RosettaRootElement}
     * does.
     */
    private Set<String> collectUsedRootNames(RModel model) {
        Set<String> used = new HashSet<>();
        for (RRootElement root : model.rootElements()) {
            for (RSymbolReference ref : AstWalker.findAll(root, RSymbolReference.class)) {
                ref.symbol().ifPresent(s -> addRootName(used, s));
            }
            for (REnumValueRef evr : AstWalker.findAll(root, REnumValueRef.class)) {
                evr.enumeration().ifPresent(e -> addRootName(used, e));
                evr.resolvedSymbol().ifPresent(s -> addRootName(used, s));
            }
            for (RTypeCall tc : AstWalker.findAll(root, RTypeCall.class)) {
                tc.referencedType().ifPresent(t -> addRootName(used, t));
            }
            for (RRuleReferenceAnnotation ann
                    : AstWalker.findAll(root, RRuleReferenceAnnotation.class)) {
                ann.rule().ifPresent(r -> addRootName(used, r));
            }
            for (RDispatch d : AstWalker.findAll(root, RDispatch.class)) {
                d.dispatchValue().flatMap(v ->
                        AstWalker.findAncestor(v, REnumeration.class))
                    .ifPresent(e -> addRootName(used, e));
            }
            for (RSwitchCaseGuard g : AstWalker.findAll(root, RSwitchCaseGuard.class)) {
                g.resolvedGuard().ifPresent(t -> addRootName(used, t));
            }
            for (var conv : AstWalker.findAll(root,
                    com.regnosys.rosetta.ast.expressions.unary.RConversionExpr.class)) {
                conv.targetEnum().ifPresent(e -> addRootName(used, e));
            }
            for (var seg : AstWalker.findAll(root,
                    com.regnosys.rosetta.ast.regulatory.RSegmentRef.class)) {
                seg.segment().ifPresent(s -> addRootName(used, s));
            }
            for (RAnnotationRef ar : AstWalker.findAll(root, RAnnotationRef.class)) {
                ar.annotation().ifPresent(a -> addRootName(used, a));
            }
            for (RExternalClass ec : AstWalker.findAll(root, RExternalClass.class)) {
                ec.referencedType().ifPresent(t -> addRootName(used, t));
            }
            for (RExternalEnum ee : AstWalker.findAll(root, RExternalEnum.class)) {
                ee.referencedType().ifPresent(t -> addRootName(used, t));
            }
            if (root instanceof com.regnosys.rosetta.ast.external.RExternalSynonymSource src) {
                for (RSynonymSource sup : src.superSources()) {
                    addRootName(used, sup);
                }
            }
            if (root instanceof com.regnosys.rosetta.ast.external.RExternalRuleSource rs) {
                for (var sup : rs.superSources()) {
                    addRootName(used, sup);
                }
            }
            if (root instanceof RFunction fn) {
                fn.superFunction().ifPresent(sup -> addRootName(used, sup));
            }
            // The extends cross-refs (the WeeklyRollConventionEnum extends
            // DayOfWeekEnum / CollateralAgreementFloatingRate extends
            // FloatingRateBase witnesses — upstream's superType EReference).
            if (root instanceof com.regnosys.rosetta.ast.types.RDataType dt) {
                dt.superType().ifPresent(sup -> addRootName(used, sup));
                dt.choiceSuperType().ifPresent(sup -> addRootName(used, sup));
            }
            if (root instanceof REnumeration en) {
                en.superType().ifPresent(sup -> addRootName(used, sup));
            }
        }
        for (RQualifiableConfig qc : model.configurations()) {
            qc.rootType().ifPresent(rt -> addRootName(used, rt));
        }
        return used;
    }

    private static void addRootName(Set<String> used, RNode target) {
        if (!(target instanceof RRootElement)) {
            return;
        }
        String name = rootName((RRootElement) target);
        if (name == null) {
            return;
        }
        RNode p = target.parent();
        while (p != null && !(p instanceof RModel)) {
            p = p.parent();
        }
        if (p instanceof RModel m && m.namespace() != null) {
            used.add(m.namespace() + "." + name);
        }
    }

    /** The root element's declared simple name (null when the class has none). */
    private static String rootName(RRootElement root) {
        if (root instanceof com.regnosys.rosetta.ast.types.RDataType d) return d.name();
        if (root instanceof com.regnosys.rosetta.ast.types.RChoice c) return c.name();
        if (root instanceof REnumeration e) return e.name();
        if (root instanceof com.regnosys.rosetta.ast.types.RTypeAlias a) return a.name();
        if (root instanceof RFunction f) return f.name();
        if (root instanceof com.regnosys.rosetta.ast.functions.RRule r) return r.name();
        if (root instanceof RSynonymSource s) return s.name();
        if (root instanceof com.regnosys.rosetta.ast.external.RExternalRuleSource rs) return rs.name();
        if (root instanceof com.regnosys.rosetta.ast.annotations.RAnnotation an) return an.name();
        if (root instanceof com.regnosys.rosetta.ast.types.RBasicType b) return b.name();
        if (root instanceof com.regnosys.rosetta.ast.types.RRecordType rec) return rec.name();
        if (root instanceof com.regnosys.rosetta.ast.types.RMetaType m) return m.name();
        if (root instanceof com.regnosys.rosetta.ast.types.RLibraryFunction lf) return lf.name();
        if (root instanceof com.regnosys.rosetta.ast.regulatory.RBody b) return b.name();
        if (root instanceof com.regnosys.rosetta.ast.regulatory.RCorpus c) return c.name();
        if (root instanceof com.regnosys.rosetta.ast.regulatory.RSegmentDef s) return s.name();
        return null;
    }

    /** The namespace exactly as written — the wildcard star included. */
    private static String importedNamespaceText(RImport imp) {
        return imp.qualifiedName() + (imp.isWildcard() ? ".*" : "");
    }

    private void checkDuplicateImports(RModel model, ValidationCollector collector) {
        Set<String> seen = new HashSet<>();
        for (RImport imp : model.imports()) {
            String key = importKey(imp);
            if (key != null && !seen.add(key)) {
                collector.warning(
                    imp.sourceRange(),
                    "Duplicate import " + importedNamespaceText(imp),
                    ValidationIssueCode.DUPLICATE_IMPORT);
            }
        }
    }

    /** Creates a unique key for an import: qualifiedName + wildcard flag. */
    private String importKey(RImport imp) {
        if (imp.qualifiedName() == null) return null;
        return imp.qualifiedName() + (imp.isWildcard() ? ".*" : "");
    }
}
