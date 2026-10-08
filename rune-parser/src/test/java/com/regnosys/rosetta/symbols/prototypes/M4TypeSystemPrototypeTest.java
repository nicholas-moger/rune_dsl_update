package com.regnosys.rosetta.symbols.prototypes;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.BaseSymbolsTest;
import com.regnosys.rosetta.symbols.RLinkingResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Forward-compat prototype for M4 (type system). Exercises the M3 API
 * the way M4's subsumption walk and attribute type lookup will.
 *
 * <p>Spec: D5/E13 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
class M4TypeSystemPrototypeTest extends BaseSymbolsTest {

    @Test
    void m4_can_walk_super_type_chain() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type A:
                type B extends A:
                type C extends B:
                """);
        RDataType c = findType(result.workspace().files().get(0), "C");
        List<String> chain = new ArrayList<>();
        Optional<RDataType> current = c.superType();
        while (current.isPresent()) {
            chain.add(current.get().name());
            current = current.get().superType();
        }
        assertEquals(List.of("B", "A"), chain);
    }

    @Test
    void m4_can_resolve_attribute_type_to_user_defined_type() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Currency:
                    code string (1..1)
                type Money:
                    currency Currency (1..1)
                """);
        RDataType money = findType(result.workspace().files().get(0), "Money");
        RAttribute currency = money.attributes().get(0);
        Optional<RNode> resolved = currency.typeCall().referencedType();
        assertTrue(resolved.isPresent(), "M4 needs resolved attribute types");
    }
}
