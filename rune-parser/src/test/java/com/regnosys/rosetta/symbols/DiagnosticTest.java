package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.symbols.diagnostics.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticTest {

    @Test
    void diagnostic_carries_all_required_fields() {
        SourceRange range = SourceRange.of5Arg("test.rosetta", 5, 10, 5, 17);
        LinkingDiagnostic d = new LinkingDiagnostic(
            Severity.ERROR,
            DiagnosticCategory.SUPER_TYPE_NOT_FOUND,
            range,
            "MissingType",
            "Super type 'MissingType' not found",
            List.of("MissingTypeName", "MissingThing"));

        assertEquals(Severity.ERROR, d.severity());
        assertEquals(DiagnosticCategory.SUPER_TYPE_NOT_FOUND, d.category());
        assertEquals(range, d.range());
        assertEquals("MissingType", d.unresolvedName());
        assertTrue(d.message().contains("MissingType"));
        assertEquals(2, d.candidates().size());
    }

    @Test
    void diagnostics_collector_starts_empty_and_returns_immutable() {
        Diagnostics collector = new Diagnostics();
        assertTrue(collector.toList().isEmpty());

        collector.error(
            DiagnosticCategory.TYPE_NOT_FOUND,
            SourceRange.NONE,
            "Foo",
            "Type 'Foo' not found",
            List.of());

        List<LinkingDiagnostic> snapshot = collector.toList();
        assertEquals(1, snapshot.size());

        // Immutable
        assertThrows(UnsupportedOperationException.class,
            () -> snapshot.add(snapshot.get(0)));
    }
}
