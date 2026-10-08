package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Invariant: {@link SymbolTableSerializer#serialize} must be deterministic.
 * Two independent builds of the same corpus must produce identical
 * serialisations. Guards against hash-map-ordering leaks or other
 * non-determinism that would silently invalidate snapshot comparisons.
 *
 * <p>The corpus is loaded TWICE (not once) so each {@code RWorkspace.build}
 * call sees a fresh {@code List<RModel>} — protects against in-place AST
 * mutation by DerivedStatePass-style passes inside the linker.
 */
class SymbolTableSerializerTest {

    @Test
    void serialize_is_deterministic() throws IOException {
        Path corpusRoot = Paths.get("../test-corpus/cdm");
        assumeTrue(Files.exists(corpusRoot), "cdm corpus not present");

        List<RModel> files1 = SymbolTestCorpus.loadCorpus(corpusRoot);
        assumeTrue(!files1.isEmpty(),
            "cdm corpus at " + corpusRoot + " contains no .rosetta files — "
            + "partially cloned? Determinism guard cannot run meaningfully.");
        List<RModel> files2 = SymbolTestCorpus.loadCorpus(corpusRoot);

        RLinkingResult r1 = RWorkspace.build(files1);
        RLinkingResult r2 = RWorkspace.build(files2);
        String s1 = SymbolTableSerializer.serialize(r1.workspace());
        String s2 = SymbolTableSerializer.serialize(r2.workspace());

        assertEquals(s1, s2,
            "SymbolTableSerializer.serialize must be deterministic across "
            + "independent builds of the same corpus. A mismatch indicates "
            + "hash-map-ordering leakage or other non-determinism in the "
            + "serializer or workspace assembly.");
    }
}
