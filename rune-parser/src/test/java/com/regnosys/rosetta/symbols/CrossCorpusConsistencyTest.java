package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.util.AstWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Loads CDM + DRR together as one workspace and verifies cross-project
 * references resolve correctly. Without this test, intra-project
 * resolution could pass while cross-project references silently break.
 *
 * <p>Spec: D5/E11 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
class CrossCorpusConsistencyTest {

    private static final List<Path> CORPUS_ROOTS = List.of(
        Paths.get("../test-corpus/cdm"),
        Paths.get("../test-corpus/drr")
    );

    @Test
    void cdm_and_drr_link_together_as_one_workspace() throws IOException {
        List<RModel> all = new ArrayList<>();
        for (Path root : CORPUS_ROOTS) {
            if (Files.exists(root)) {
                all.addAll(SymbolTestCorpus.loadCorpus(root));
            }
        }
        if (all.isEmpty()) {
            System.out.println("Skipping — no corpora present locally");
            return;
        }

        System.out.println("Loading " + all.size() + " files into combined workspace...");
        RLinkingResult result = RWorkspace.build(all);

        // Invariants — strict assertion. The LexicalResolutionPass bug that
        // caused "both resolved AND diagnosed" violations has been fixed
        // (skip-already-resolved refs). All invariants must hold.
        List<String> violations = LinkerInvariants.checkAll(result.workspace());
        assertTrue(violations.isEmpty(),
            "Linker invariant violations:\n  " + String.join("\n  ", violations));

        // Every cross-project super-type must resolve to a node in the workspace
        Set<RDataType> allTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RModel f : all) {
            allTypes.addAll(AstWalker.findAll(f, RDataType.class));
        }
        for (RDataType dt : allTypes) {
            dt.superType().ifPresent(sup -> assertTrue(allTypes.contains(sup),
                "super type of " + dt.name() + " resolved to node outside workspace"));
        }

        System.out.println("Cross-corpus: " + all.size() + " files, "
            + result.workspace().namespaces().size() + " namespaces, "
            + result.linkingDiagnostics().size() + " diagnostics");
    }

}
