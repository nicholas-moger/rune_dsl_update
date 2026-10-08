package com.regnosys.rosetta.symbols.prototypes;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.BaseSymbolsTest;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Forward-compat prototype for M7 (codegen).
 */
class M7CodegenPrototypeTest extends BaseSymbolsTest {

    @Test
    void m7_can_compute_fqn_of_super_type() {
        RModel sup = parseModel("namespace cdm.bar\ntype Super:\n");
        RModel sub = parseModel("namespace cdm.foo\nimport cdm.bar.*\ntype Sub extends Super:\n");
        RWorkspace ws = RWorkspace.build(List.of(sup, sub)).workspace();

        RDataType subType = findType(sub, "Sub");
        Optional<String> fqn = subType.superType()
            .map(t -> findNamespace(t) + "." + t.name());
        assertEquals(Optional.of("cdm.bar.Super"), fqn);
    }

    @Test
    void m7_can_walk_attributes_with_resolved_types() {
        RModel party = parseModel("namespace test\ntype Party:\n    name string (1..1)\n");
        RModel trade = parseModel("namespace test\ntype Trade:\n    counterparty Party (1..1)\n");
        RWorkspace ws = RWorkspace.build(List.of(party, trade)).workspace();

        RDataType tradeType = findType(trade, "Trade");
        for (RAttribute attr : tradeType.attributes()) {
            Optional<RNode> resolved = attr.typeCall().referencedType();
            assertTrue(resolved.isPresent(), "M7 needs resolved types for " + attr.name());
        }
    }

    private static String findNamespace(RNode node) {
        RNode current = node;
        while (current != null) {
            if (current instanceof RModel m) return m.namespace();
            current = current.parent();
        }
        return "";
    }
}
