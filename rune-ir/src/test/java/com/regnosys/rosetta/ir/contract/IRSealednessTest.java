package com.regnosys.rosetta.ir.contract;

import com.regnosys.rosetta.ir.core.IREnum;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRFunction;
import com.regnosys.rosetta.ir.core.IRModel;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRParameter;
import com.regnosys.rosetta.ir.core.IRRule;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.rune.IRAnnotationDecl;
import com.regnosys.rosetta.ir.rune.IRRegulatoryUnit;
import com.regnosys.rosetta.ir.rune.IRReport;
import com.regnosys.rosetta.ir.rune.IRSourceDecl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Locks the W22 contract: IR interfaces are open (NOT sealed) so external
 * implementations (e.g. cross-DSL importers) can implement them
 * outside this module.
 *
 * <p>Per spec Section 7 reviewer fix C2 — pure reflection check. The
 * "compile a stub from outside" framing is impossible (Java
 * sealed-permits is compile-time, not runtime testable across modules), so
 * this test asserts the negation: every IR interface has no
 * {@code permits} clause.
 */
class IRSealednessTest {

    @Test
    void irInterfacesAreNotSealed() {
        for (Class<?> iface : new Class<?>[]{
                IRNode.class, IRType.class, IRField.class,
                IREnum.class, IREnumValue.class,
                // the model-level node of the property gate (v3.3 seat 8, PR #644)
                IRModel.class,
                IRFunction.class, IRParameter.class, IRRule.class,
                IRReport.class, IRRegulatoryUnit.class,
                IRAnnotationDecl.class, IRSourceDecl.class
        }) {
            assertFalse(iface.isSealed(),
                    iface.getName() + " must NOT be sealed (W22 — external implementations).");
            // Per JEP 409 §13.1: getPermittedSubclasses() returns null for
            // non-sealed classes and a (possibly empty) array for sealed.
            // Both cases must satisfy "no permitted subclasses are listed".
            Class<?>[] permitted = iface.getPermittedSubclasses();
            int count = permitted == null ? 0 : permitted.length;
            assertEquals(0, count,
                    iface.getName() + " must have no permits clause; found: " +
                            (permitted == null ? "null"
                                    : Arrays.stream(permitted)
                                            .map(Class::getName)
                                            .sorted()
                                            .collect(Collectors.joining(", ", "[", "]"))));
        }
    }
}
