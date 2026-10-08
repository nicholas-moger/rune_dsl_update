package com.regnosys.rosetta.symbols.linker;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import com.regnosys.rosetta.symbols.linker.CrossRefFieldRegistry.CrossRefFieldDescriptor;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Reflection-driven audit pass — walks every node in every file in the
 * workspace, finds every field annotated with {@link CrossRefField}, and
 * asserts that EITHER the field is set OR a diagnostic of the matching
 * {@link DiagnosticCategory} exists for the field's source range.
 *
 * <p><b>Test infrastructure only.</b> This class lives under
 * {@code src/test/java} and must NOT be wired into production build paths
 * ({@code Linker.link}, {@code RWorkspace.build}). M8 LSP must not pay
 * reflection-walk overhead.
 *
 * <p>Spec: D2 + D5/E2 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class ResolutionAudit {

    private ResolutionAudit() {}

    /**
     * Asserts the workspace passes the audit. Throws an
     * {@link AssertionError} naming the file, node, and field that failed.
     */
    public static void assertFullyHandled(RWorkspace workspace) {
        List<String> failures = new ArrayList<>();
        Map<String, List<LinkingDiagnostic>> diagsByFile = new HashMap<>();
        for (LinkingDiagnostic d : workspace.linkingDiagnostics()) {
            diagsByFile.computeIfAbsent(d.range().file(), k -> new ArrayList<>()).add(d);
        }

        for (RModel file : workspace.files()) {
            AstWalker.walk(file, node -> checkNode(node, diagsByFile, failures));
        }

        if (!failures.isEmpty()) {
            throw new AssertionError(
                "ResolutionAudit failed — " + failures.size() + " cross-ref field(s) "
                + "are neither resolved nor diagnosed:\n  " + String.join("\n  ", failures));
        }
    }

    private static void checkNode(
            RNode node,
            Map<String, List<LinkingDiagnostic>> diagsByFile,
            List<String> failures) {

        // Exempt the synthesized only-exists navigation path (AstBuilder.buildOnlyExistsPath —
        // the root, every hop and, since v3.2 seat 8, the LEAF): the TEXT is user-written, the
        // NODES are the fork's synthesis of it, resolved best-effort for the generator's nav
        // rendering and NOT source-validated. The leaf IS bound by the resolver like any other
        // feature (the L2 differential and ClosureParameterSeatTest b1/b3/corpus_c3 witness
        // it); what this exemption covers is the DIAGNOSTIC half — an unresolvable root (e.g. a
        // bare function/rule name the generator declines on) is intentionally left unresolved
        // AND undiagnosed, LexicalResolutionPass applies the symmetric suppression, so a
        // synthesized node would otherwise trip the "unresolved AND undiagnosed" check here on
        // valid user code. The exemption's scope is the whole element (unchanged at round 1):
        // narrowing it to the root and the hops would expose the LEAF, which is unresolvable
        // exactly when its root is (a bare function / rule name the generator declines on), so
        // this check would fire on the leaf instead — UNMEASURED whether any such root exists
        // in the audited corpus (the vendored cells parse diagnostic-clean; the chaos cell's
        // only-exists roots are inputs).
        if (AstWalker.findAncestor(node, ROnlyExistsElement.class).isPresent()) {
            return;
        }

        Class<?> cls = node.getClass();
        // Cached @CrossRefField fields per concrete class — avoids per-node
        // Class.getDeclaredFields() + Field.getAnnotation() reflection cost.
        for (CrossRefFieldDescriptor desc : CrossRefFieldRegistry.getCrossRefFields(cls)) {
            Field f = desc.field();
            CrossRefField annotation = desc.annotation();

            Object value;
            try {
                value = f.get(node);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                    "audit cannot read " + f.getDeclaringClass().getName() + "." + f.getName(), e);
            }

            boolean isResolved = isFieldResolved(value);
            // Special case: RDataType.superTypeId may be null when the super type
            // is a choice type (stored in choiceSuperTypeId), and vice versa.
            // Covers both SymbolId field names so the audit doesn't flag the null
            // field when the OTHER field carries the resolution. Also retains
            // "resolvedSuperType" for any not-yet-migrated branches.
            if (!isResolved && node instanceof com.regnosys.rosetta.ast.types.RDataType dt
                    && ("resolvedSuperType".equals(f.getName())
                        || "superTypeId".equals(f.getName())
                        || "choiceSuperTypeId".equals(f.getName()))
                    && dt.isSuperTypeResolved()) {
                isResolved = true;
            }
            if (isResolved) continue;

            // Check if the SOURCE reference even exists. For optional
            // cross-refs (e.g. RDataType.superTypeName() = Optional.empty()),
            // a null resolved field is EXPECTED — there's nothing to resolve.
            // We only flag unresolved fields where the source name IS present.
            if (!hasSourceReference(node, f)) continue;

            // Field is unresolved AND source reference exists — must find a matching diagnostic
            SourceRange range = lookupRange(node, annotation);
            boolean covered = hasMatchingDiagnostic(
                diagsByFile, range, annotation.category(), annotation.tokenRangePrefix(), node);
            if (!covered) {
                // Use field's declaring class (NOT the concrete node class) to
                // preserve original failure-message semantics: pre-fix the outer
                // `while (cls != null) ... cls.getDeclaredFields()` loop had
                // `cls` == declaring class for fields it iterated, so
                // `cls.getSimpleName()` reported the declaring class. Cached
                // descriptor iteration loses that variable, so derive declaring
                // class from the Field directly.
                failures.add(
                    f.getDeclaringClass().getSimpleName() + "." + f.getName()
                    + " in " + range.file() + ":" + range.startLine() + ":" + range.startCol()
                    + " is unresolved AND undiagnosed (expected category " + annotation.category() + ")");
            }
        }
    }

    /**
     * Checks whether the node actually has a source-level reference that
     * this cross-ref field is meant to resolve. For optional cross-refs
     * (e.g. RDataType with no "extends" clause), the source name is absent
     * and a null resolved field is expected, not a failure.
     *
     * <p>Strategy: look for a "source name" method on the node that
     * corresponds to the resolved field. For example, if the field is
     * "resolvedSuperType" on RDataType, the source method is
     * "superTypeName()". We check the common naming patterns.
     */
    private static boolean hasSourceReference(RNode node, Field resolvedField) {
        // For multi-valued cross-refs (List fields), the source is a List<String>;
        // if empty, no source references exist.
        String fieldName = resolvedField.getName();

        // P1.4.1b SymbolId convention: fieldName ends with "Id" (but isn't just "Id").
        // e.g. superTypeId       → superTypeName
        //      referencedTypeId  → typeName  (special override)
        //      rootTypeId        → rootTypeName
        //      superFunctionId   → superFunctionName
        //      choiceSuperTypeId → superTypeName (special override — shares source with superTypeId)
        if (fieldName.endsWith("Id") && !fieldName.equals("Id")) {
            String baseName = fieldName.substring(0, fieldName.length() - "Id".length());

            // Special-case overrides for fields whose source method name doesn't
            // follow the baseName+"Name" pattern. Checked FIRST so a generic
            // baseName+Name probe doesn't preempt them.
            if ("choiceSuperType".equals(baseName)) {
                Boolean r = invokeStringOrOptionalAccessor(node, "superTypeName");
                if (r != null) return r;
            }
            if ("referencedType".equals(baseName)) {
                Boolean r = invokeStringOrOptionalAccessor(node, "typeName");
                if (r != null) return r;
            }

            // Default: try baseName + "Name" (e.g. superTypeId → superTypeName)
            Boolean result = invokeStringOrOptionalAccessor(node, baseName + "Name");
            if (result != null) return result;

            // Conservative default — assume source ref present (so audit may flag
            // the field as expected-to-be-resolved). Same posture as the legacy
            // "resolved*" branch.
            return true;
        }

        // Legacy "resolvedXxx" pattern — keep for direct-pointer fields not yet migrated.
        if (fieldName.startsWith("resolved")) {
            // Try common M2 source-name methods
            String baseName = fieldName.substring("resolved".length());
            // resolvedSuperType → superTypeName
            // resolvedAnnotation → annotationName
            // resolvedFunction → funcName (RMapTestFunc) or superFunctionName (RFunction)
            // etc.
            for (String suffix : new String[]{"Name", "TypeName", "FunctionName"}) {
                String candidate = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1) + suffix;
                try {
                    java.lang.reflect.Method m = node.getClass().getMethod(candidate);
                    Object result = m.invoke(node);
                    if (result == null) return false;
                    if (result instanceof Optional<?> opt) return opt.isPresent();
                    if (result instanceof String s) return !s.isEmpty();
                    if (result instanceof List<?> list) return !list.isEmpty();
                    return true;
                } catch (NoSuchMethodException e) {
                    // Try next suffix
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
            // Also try direct name without suffix (e.g. resolvedSymbol → name)
            try {
                java.lang.reflect.Method m = node.getClass().getMethod("name");
                Object result = m.invoke(node);
                if (result == null) return false;
                if (result instanceof String s) return !s.isEmpty();
                return true;
            } catch (ReflectiveOperationException e) {
                // fall through
            }
        }
        // For multi-valued (List) resolved fields, check the source list
        if (fieldName.equals("resolvedSuperSources")) {
            try {
                java.lang.reflect.Method m = node.getClass().getMethod("superSourceNames");
                Object result = m.invoke(node);
                if (result instanceof List<?> list) return !list.isEmpty();
            } catch (ReflectiveOperationException e) {
                // fall through
            }
        }
        // Default: assume source reference exists (conservative — will flag as failure)
        return true;
    }

    /**
     * Invokes a no-arg accessor by name on the given node. Returns:
     * <ul>
     *   <li>{@code true} if the result is non-empty (non-null String, non-empty
     *       Optional / String / List).</li>
     *   <li>{@code false} if the result is null, empty Optional, empty String, or
     *       empty List.</li>
     *   <li>{@code null} if the accessor doesn't exist on the node's class.</li>
     * </ul>
     *
     * <p>The {@code null}-vs-{@code false} distinction allows callers to fall
     * through to the next probe when the method is absent, without masking the
     * "source ref is explicitly absent" ({@code false}) case.
     */
    private static Boolean invokeStringOrOptionalAccessor(RNode node, String methodName) {
        try {
            java.lang.reflect.Method m = node.getClass().getMethod(methodName);
            Object result = m.invoke(node);
            if (result == null) return false;
            if (result instanceof Optional<?> opt) return opt.isPresent();
            if (result instanceof String s) return !s.isEmpty();
            if (result instanceof List<?> list) return !list.isEmpty();
            return true;
        } catch (NoSuchMethodException e) {
            return null; // accessor absent — let caller try next probe
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isFieldResolved(Object value) {
        if (value == null) return false;
        if (value instanceof List<?> list) return !list.isEmpty();
        return true;
    }

    private static SourceRange lookupRange(RNode node, CrossRefField annotation) {
        String key = annotation.tokenRangeKey();
        if (key != null && !key.isEmpty()) {
            SourceRange range = node.tokenRanges().get(key);
            if (range != null) return range;
        }
        return node.sourceRange();
    }

    private static boolean hasMatchingDiagnostic(
            Map<String, List<LinkingDiagnostic>> diagsByFile,
            SourceRange range,
            DiagnosticCategory expectedCategory,
            String tokenRangePrefix,
            RNode node) {

        List<LinkingDiagnostic> diagsForFile = diagsByFile.getOrDefault(range.file(), List.of());

        // Multi-valued: any diagnostic of the matching category at any
        // tokenRangePrefix_i covers the field
        if (tokenRangePrefix != null && !tokenRangePrefix.isEmpty()) {
            for (Map.Entry<String, SourceRange> e : node.tokenRanges().entrySet()) {
                if (e.getKey().startsWith(tokenRangePrefix + "_")) {
                    for (LinkingDiagnostic d : diagsForFile) {
                        if (d.category() == expectedCategory && rangesOverlap(d.range(), e.getValue())) {
                            return true;
                        }
                    }
                }
            }
            for (LinkingDiagnostic d : diagsForFile) {
                if (d.category() == expectedCategory && rangesOverlap(d.range(), node.sourceRange())) {
                    return true;
                }
            }
            return false;
        }

        // Single-valued
        for (LinkingDiagnostic d : diagsForFile) {
            if (d.category() == expectedCategory && rangesOverlap(d.range(), range)) {
                return true;
            }
        }
        return false;
    }

    private static boolean rangesOverlap(SourceRange a, SourceRange b) {
        if (!a.file().equals(b.file())) return false;
        int aStart = a.startLine() * 10000 + a.startCol();
        int aEnd   = a.endLine()   * 10000 + a.endCol();
        int bStart = b.startLine() * 10000 + b.startCol();
        int bEnd   = b.endLine()   * 10000 + b.endCol();
        return aStart <= bEnd && bStart <= aEnd;
    }
}
