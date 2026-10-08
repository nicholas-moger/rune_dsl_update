package com.regnosys.rosetta.ir.emit;

import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.testutil.IRSamples;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-tests the SPI base in isolation via a minimal fixture backend that supports two kinds
 * and declines the rest. Verifies: per-kind dispatch routes to the right hook; the no-default
 * switch covers every {@link IRExprKind}; an un-overridden kind declines through the single
 * {@code unsupported} choke with a message naming the emitter class and the kind.
 */
class AbstractIRExprEmitterTest {

    /** A minimal backend: supports LITERAL + VARIABLE; every other kind declines via the base default. */
    private static final class FixtureEmitter extends AbstractIRExprEmitter<String> {
        @Override protected String emitLiteral(IRLiteral node)   { return "lit"; }
        @Override protected String emitVariable(IRVariable node) { return "var"; }
    }

    private static final Set<IRExprKind> SUPPORTED =
            EnumSet.of(IRExprKind.LITERAL, IRExprKind.VARIABLE);

    private final FixtureEmitter emitter = new FixtureEmitter();

    @Test
    void dispatchesEachSupportedKindToItsOwnHook() {
        assertEquals("lit", emitter.emit(IRSamples.expr(IRExprKind.LITERAL)));
        assertEquals("var", emitter.emit(IRSamples.expr(IRExprKind.VARIABLE)));
    }

    @ParameterizedTest
    @EnumSource(IRExprKind.class)
    void everyKindEmitsOrDeclinesCleanly(IRExprKind kind) {
        IRExpr sample = IRSamples.expr(kind);
        if (SUPPORTED.contains(kind)) {
            assertNotNull(emitter.emit(sample), kind + " should emit a non-null result");
        } else {
            EmitterException ex = assertThrows(EmitterException.class,
                    () -> emitter.emit(sample), kind + " should decline via EmitterException");
            assertTrue(ex.getMessage().contains("FixtureEmitter"),
                    "decline message should name the emitter class, was: " + ex.getMessage());
            assertTrue(ex.getMessage().contains(kind.name()),
                    "decline message should name the kind, was: " + ex.getMessage());
        }
    }
}
