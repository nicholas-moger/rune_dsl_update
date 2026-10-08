package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.linker.CrossRefField;
import com.regnosys.rosetta.symbols.linker.CrossRefFieldRegistry;
import com.regnosys.rosetta.symbols.linker.CrossRefFieldRegistry.CrossRefFieldDescriptor;

import java.util.*;

/**
 * Boolean invariants over a linked workspace. Each method returns the
 * list of violations as human-readable strings; empty = invariant holds.
 *
 * <p>Spec: D5/E9 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class LinkerInvariants {

    public static List<String> checkAll(RWorkspace ws) {
        List<String> all = new ArrayList<>();
        all.addAll(noNodeIsBothResolvedAndDiagnosed(ws));
        all.addAll(everyDiagnosticHasNonEmptySourceRange(ws));
        all.addAll(everyDiagnosticRangeBelongsToAFile(ws));
        all.addAll(everyResolvedSuperTypeIsInWorkspace(ws));
        all.addAll(noSuperTypeChainCycles(ws));
        all.addAll(noNamespaceAppearsTwiceInTopLevelMap(ws));
        all.addAll(everyResolvedRefIsInReferenceIndex(ws));
        all.addAll(everyImportEntryHasNonNullTarget(ws));
        all.addAll(everySubTypeIndexEntryRoundtrips(ws));
        all.addAll(everyErrorDiagnosticHasUnresolvedName(ws));
        all.addAll(everyDiagnosticHasNonEmptyMessage(ws));
        all.addAll(noResolvedFieldPointsBackAtItself(ws));
        return all;
    }

    private static List<String> noNodeIsBothResolvedAndDiagnosed(RWorkspace ws) {
        List<String> failures = new ArrayList<>();

        // Index diagnostics by (category, file, startLine, startCol, endLine, endCol)
        // for O(1) lookup per (node, field) instead of O(D) per (node, field) scan.
        // The lookup key intentionally omits byte offsets — original behaviour
        // matched on exact equality of file + 4 line/col fields only (some ranges
        // carry OFFSETS_UNKNOWN sentinel; record-equals would differ even when
        // logical position is identical).
        Map<DiagnosticLookupKey, List<LinkingDiagnostic>> diagnosticIndex = new HashMap<>();
        for (LinkingDiagnostic d : ws.linkingDiagnostics()) {
            DiagnosticLookupKey key = DiagnosticLookupKey.from(d.category(), d.range());
            diagnosticIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }

        for (RModel file : ws.files()) {
            AstWalker.walk(file, node -> {
                Class<?> cls = node.getClass();
                // Cached @CrossRefField fields per concrete class — avoids
                // per-node Class.getDeclaredFields() + Field.getAnnotation()
                // reflection cost.
                for (CrossRefFieldDescriptor desc : CrossRefFieldRegistry.getCrossRefFields(cls)) {
                    Object value;
                    try { value = desc.field().get(node); }
                    catch (IllegalAccessException e) { throw new IllegalStateException(e); }
                    boolean isResolved = value != null
                        && (!(value instanceof List<?> l) || !l.isEmpty());
                    if (!isResolved) continue;
                    CrossRefField ann = desc.annotation();
                    SourceRange range = ann.tokenRangeKey().isEmpty()
                        ? node.sourceRange()
                        : node.tokenRanges().getOrDefault(ann.tokenRangeKey(), node.sourceRange());

                    // Match on exact range equality — start AND end — to avoid
                    // false positives from parent/child range overlap (e.g. an
                    // RConversionExpr's sourceRange starts at its lhs child's
                    // position, so a child's diagnostic would falsely correlate).
                    DiagnosticLookupKey lookupKey = DiagnosticLookupKey.from(ann.category(), range);
                    List<LinkingDiagnostic> matches = diagnosticIndex.get(lookupKey);
                    if (matches != null) {
                        // Use field's declaring class (NOT the concrete node class) to
                        // preserve original failure-message semantics: pre-fix the
                        // outer `while (cls != null) ... cls.getDeclaredFields()` loop
                        // had `cls` == declaring class for fields it iterated, so
                        // `cls.getSimpleName()` reported the declaring class. Cached
                        // descriptor iteration loses that variable, so derive
                        // declaring class from the Field directly.
                        for (LinkingDiagnostic d : matches) {
                            failures.add(desc.field().getDeclaringClass().getSimpleName()
                                + "." + desc.field().getName()
                                + " is both resolved AND diagnosed as " + d.category()
                                + " at " + range.file() + ":" + range.startLine() + ":" + range.startCol());
                        }
                    }
                }
            });
        }
        return failures;
    }

    /**
     * Lookup key for the diagnostic index in {@link #noNodeIsBothResolvedAndDiagnosed}.
     * Mirrors the original equality predicate (category + file + 4 line/col fields,
     * excluding byte offsets which may carry the {@code OFFSETS_UNKNOWN} sentinel).
     */
    private record DiagnosticLookupKey(
            DiagnosticCategory category,
            String file,
            int startLine,
            int startCol,
            int endLine,
            int endCol) {
        static DiagnosticLookupKey from(DiagnosticCategory category, SourceRange range) {
            return new DiagnosticLookupKey(
                category, range.file(),
                range.startLine(), range.startCol(),
                range.endLine(), range.endCol()
            );
        }
    }

    private static List<String> everyDiagnosticHasNonEmptySourceRange(RWorkspace ws) {
        List<String> failures = new ArrayList<>();
        for (LinkingDiagnostic d : ws.linkingDiagnostics()) {
            if (d.range() == null || d.range() == SourceRange.NONE) {
                failures.add("diagnostic " + d.category() + " has empty source range");
            }
        }
        return failures;
    }

    private static List<String> everyDiagnosticRangeBelongsToAFile(RWorkspace ws) {
        Set<String> filePaths = new HashSet<>();
        for (RModel f : ws.files()) {
            if (f.sourceRange() != null) filePaths.add(f.sourceRange().file());
        }
        List<String> failures = new ArrayList<>();
        for (LinkingDiagnostic d : ws.linkingDiagnostics()) {
            if (!filePaths.contains(d.range().file())) {
                failures.add("diagnostic at unknown file " + d.range().file());
            }
        }
        return failures;
    }

    private static List<String> everyResolvedSuperTypeIsInWorkspace(RWorkspace ws) {
        Set<RDataType> all = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RModel f : ws.files()) {
            all.addAll(AstWalker.findAll(f, RDataType.class));
        }
        List<String> failures = new ArrayList<>();
        for (RDataType dt : all) {
            dt.superType().ifPresent(superType -> {
                if (!all.contains(superType)) {
                    failures.add("super type of " + dt.name() + " is not in workspace");
                }
            });
        }
        return failures;
    }

    private static List<String> noSuperTypeChainCycles(RWorkspace ws) {
        List<String> failures = new ArrayList<>();
        for (RModel f : ws.files()) {
            for (RDataType dt : AstWalker.findAll(f, RDataType.class)) {
                Set<RDataType> visited = Collections.newSetFromMap(new IdentityHashMap<>());
                RDataType current = dt;
                int safety = 0;
                while (current != null && safety++ < 10000) {
                    if (!visited.add(current)) {
                        // Only flag if the linker didn't already emit CIRCULAR_INHERITANCE
                        boolean alreadyDiagnosed = ws.linkingDiagnostics().stream()
                            .anyMatch(d -> d.category() == com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.CIRCULAR_INHERITANCE
                                        && d.unresolvedName().equals(dt.name()));
                        if (!alreadyDiagnosed) {
                            failures.add("super type chain cycle at " + dt.name() + " without CIRCULAR_INHERITANCE diagnostic");
                        }
                        break;
                    }
                    current = current.superType().orElse(null);
                }
            }
        }
        return failures;
    }

    private static List<String> noNamespaceAppearsTwiceInTopLevelMap(RWorkspace ws) {
        Set<String> seen = new HashSet<>();
        List<String> failures = new ArrayList<>();
        for (RNamespaceScope ns : ws.namespaces()) {
            if (!seen.add(ns.qualifiedName())) {
                failures.add("namespace " + ns.qualifiedName() + " appears twice");
            }
        }
        return failures;
    }

    private static List<String> everyResolvedRefIsInReferenceIndex(RWorkspace ws) {
        List<String> failures = new ArrayList<>();
        for (RModel f : ws.files()) {
            for (RDataType dt : AstWalker.findAll(f, RDataType.class)) {
                dt.superType().ifPresent(sup -> {
                    if (!ws.findReferences(sup).contains(dt)) {
                        failures.add(dt.name() + " resolved super type but not in reference index");
                    }
                });
            }
        }
        return failures;
    }

    private static List<String> everyImportEntryHasNonNullTarget(RWorkspace ws) {
        List<String> failures = new ArrayList<>();
        for (RModel f : ws.files()) {
            ws.fileScope(f).ifPresent(scope -> {
                for (ImportEntry imp : scope.imports()) {
                    if (imp.target() == null) {
                        failures.add("import entry " + imp.importedNamespace() + " has null target");
                    }
                }
            });
        }
        return failures;
    }

    private static List<String> everySubTypeIndexEntryRoundtrips(RWorkspace ws) {
        List<String> failures = new ArrayList<>();
        for (RModel f : ws.files()) {
            for (RDataType dt : AstWalker.findAll(f, RDataType.class)) {
                for (RDataType sub : ws.getSubTypes(dt)) {
                    if (sub.superType().orElse(null) != dt) {
                        failures.add(sub.name() + " in getSubTypes(" + dt.name()
                            + ") but its superType is not " + dt.name());
                    }
                }
            }
        }
        return failures;
    }

    private static List<String> everyErrorDiagnosticHasUnresolvedName(RWorkspace ws) {
        List<String> failures = new ArrayList<>();
        for (LinkingDiagnostic d : ws.linkingDiagnostics()) {
            if (d.severity() == Severity.ERROR
                    && (d.unresolvedName() == null || d.unresolvedName().isBlank())) {
                failures.add("ERROR diagnostic " + d.category() + " has blank unresolvedName");
            }
        }
        return failures;
    }

    private static List<String> everyDiagnosticHasNonEmptyMessage(RWorkspace ws) {
        List<String> failures = new ArrayList<>();
        for (LinkingDiagnostic d : ws.linkingDiagnostics()) {
            if (d.message() == null || d.message().isBlank()) {
                failures.add("diagnostic " + d.category() + " has blank message");
            }
        }
        return failures;
    }

    private static List<String> noResolvedFieldPointsBackAtItself(RWorkspace ws) {
        List<String> failures = new ArrayList<>();
        for (RModel f : ws.files()) {
            for (RDataType dt : AstWalker.findAll(f, RDataType.class)) {
                if (dt.superType().orElse(null) == dt) {
                    failures.add(dt.name() + " has itself as super type");
                }
            }
        }
        return failures;
    }
}
