package com.regnosys.rosetta.ir.print;

import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRNodeImpl;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.testutil.IRSamples;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IRPrintabilityCoverageTest {

    @Test
    void everyIRKindRendersWithoutDeclining() {
        IRPrinter printer = new IRPrinter();
        for (IRKind kind : IRKind.values()) {
            IRNode node = IRSamples.node(kind);
            String out = printer.print(node);
            assertFalse(out.isBlank(), "blank output for IRKind " + kind);
            assertTrue(out.contains(kind.name()) || isModeledLabel(kind, out),
                    "output for " + kind + " did not surface the kind: " + out);
        }
    }

    /**
     * Modeled kinds whose rendered keyword differs from {@code IRKind.name()}.
     * Only ENUM_VALUE truly needs this (it renders {@code VALUE …}); STRUCT/CHOICE
     * are covered for clarity even though the {@code contains(kind.name())} arm
     * already matches "STRUCT "/"CHOICE ". (ENUM→"ENUM …", FIELD→"FIELD …" both
     * contain their own name, so they pass via that arm.)
     */
    private static boolean isModeledLabel(IRKind kind, String out) {
        return (kind == IRKind.STRUCT && out.startsWith("STRUCT "))
            || (kind == IRKind.CHOICE && out.startsWith("CHOICE "))
            || (kind == IRKind.ENUM_VALUE && out.startsWith("VALUE "));
    }

    @Test
    void fallbackNodeRecursesChildrenWithoutLeakingExcludedComponentsOrIdentityHash() {
        // A child-bearing UNMODELED (fallback) node routed through printGeneric: a FUNCTION parent holding a
        // RULE child, each an IRNodeImpl. Before the fix the child list was reflectively String.valueOf'd,
        // splicing the child record's toString() into the dump — which leaked the excluded
        // sourceRange/metadata components and a non-deterministic IRMetadata identity hash (@...). (F2)
        IRNode child = new IRNodeImpl("Child", IRKind.RULE, List.of(), Optional.empty(), IRMetadata.EMPTY);
        IRNode parent = new IRNodeImpl("Parent", IRKind.FUNCTION, List.of(child), Optional.empty(), IRMetadata.EMPTY);
        String out = new IRPrinter().print(parent);
        assertFalse(out.contains("@"), "identity hash leaked into the deterministic dump: " + out);
        for (String excluded : IRSamples.EXCLUDED_COMPONENTS) {
            assertFalse(out.contains(excluded),
                    "excluded component '" + excluded + "' leaked into the dump: " + out);
        }
        // the child is dumped structurally via the node path, not as a flat record toString()
        assertTrue(out.contains("RULE Child"), "child not recursively printed: " + out);
    }

    @Test
    void everyIRExprKindRendersWithoutDeclining() {
        IRPrinter printer = new IRPrinter();
        for (IRExprKind kind : IRExprKind.values()) {
            IRExpr node = IRSamples.expr(kind);
            String out = printer.print(node);
            assertFalse(out.isBlank(), "blank output for IRExprKind " + kind);
        }
    }

    /**
     * Model-drift sentinel (NOT a proof of emission). It reflects each record's
     * components, removes the excluded-component set, and compares to a hand-maintained
     * RENDERED set — so a NEW model component (neither mapped nor excluded)
     * fails this test, forcing a conscious printer + map update. It checks component
     * names, not output, so it cannot catch a printer that STOPS emitting an
     * already-mapped component; actual rendering is proven by the unit tests + golden
     * snapshots, and switch-totality by the coverage tests. Keyed by Class, it cannot
     * distinguish IRTypeNode's two roles: the full-declaration role renders all of
     * {name,kind,fields,baseType,isAbstract}; the lightweight type-ref role (a
     * field/base type) renders name() only — an accepted, documented loss this
     * component-level test does not (and need not) cover.
     */
    @Test
    void modelDrift_everyRecordComponentIsMappedOrAllowlisted() {
        IRSamples.RENDERED_COMPONENTS.forEach((cls, expectedRendered) -> {
            TreeSet<String> actual = new TreeSet<>();
            for (RecordComponent rc : cls.getRecordComponents()) {
                if (!IRSamples.EXCLUDED_COMPONENTS.contains(rc.getName())) {
                    actual.add(rc.getName());
                }
            }
            assertEquals(new TreeSet<>(expectedRendered), actual,
                cls.getSimpleName() + ": a component is neither rendered nor allowlisted "
                    + "(update the printer AND this expected set, or add to the allowlist).");
        });
    }
}
