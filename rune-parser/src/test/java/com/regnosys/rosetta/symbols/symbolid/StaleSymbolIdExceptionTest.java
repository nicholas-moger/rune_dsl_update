package com.regnosys.rosetta.symbols.symbolid;

import com.regnosys.rosetta.symbols.StaleSymbolIdException;
import com.regnosys.rosetta.symbols.SymbolId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaleSymbolIdExceptionTest {

    @Test
    void carriesIdAndExpectedGeneration() {
        SymbolId id = SymbolId.of("com.foo", "Bar", 55L);
        StaleSymbolIdException ex = new StaleSymbolIdException(id, 77L);
        assertSame(id, ex.symbolId());
        assertEquals(77L, ex.expectedGeneration());
    }

    @Test
    void messageIncludesFqnAndBothGenerations() {
        // Use distinct multi-digit generations so substring matches don't
        // collide with potential single-digit appearances elsewhere in the
        // message (e.g. embedded inside the FQN).
        SymbolId id = SymbolId.of("com.foo", "Bar", 55L);
        StaleSymbolIdException ex = new StaleSymbolIdException(id, 77L);
        String msg = ex.getMessage();
        assertTrue(msg.contains("com.foo.Bar"));
        assertTrue(msg.contains("55"));
        assertTrue(msg.contains("77"));
    }

    @Test
    void nullSymbolId_throwsNullPointerException() {
        // Defensive: ctor should fail-fast with a clear NPE rather than
        // dereference null inside the message-building expression.
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new StaleSymbolIdException(null, 1L));
        assertTrue(ex.getMessage().contains("symbolId"));
    }
}
