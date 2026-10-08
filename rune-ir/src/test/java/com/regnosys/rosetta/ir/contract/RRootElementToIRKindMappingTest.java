package com.regnosys.rosetta.ir.contract;

import com.regnosys.rosetta.ast.IRKindForClass;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.builder.AstBuildException;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * P1.4.3 IR-contract test (spec Section 7 reviewer fix I1).
 *
 * <p>Locks the contract that <em>every RRootElement subclass actually
 * encountered</em> in the populated 1,628-fixture corpus (gitignored /
 * cloned-on-demand) has a declared IRKind mapping
 * in {@link IRKindForClass}. This is a corpus-driven complement to the
 * registry-driven drift detector in
 * {@code com.regnosys.rosetta.ast.RRootElementRegistryConsistencyTest} (rune-parser):
 *
 * <ul>
 *   <li>Drift detector (cheap, ~10ms) — asserts the two registries agree.
 *   <li>This test (corpus walk, ~5min) — asserts every encountered subclass
 *       is mapped, catching new RRootElement subclasses introduced by future
 *       PRs that bypass dead-subclass reclassification.
 * </ul>
 *
 * <p>Lives in {@code com.regnosys.rosetta.ir.contract} (not under
 * {@code ast.*}) so it does not pollute the {@code SchemaLockGenerator}
 * AST-package scan and so the IR-contract surface is discoverable as a
 * named test sub-tree.
 *
 * <p>Counterparts in rune-parser: {@code com.regnosys.rosetta.ast.RRootElementRegistryConsistencyTest}
 * and {@code com.regnosys.rosetta.ast.RRootElementCorpusOccurrenceTest}.
 */
class RRootElementToIRKindMappingTest {

    @Test
    void everyEncounteredRRootElementHasIRKindMapping() throws IOException {
        assumeTrue(CorpusWalker.corpusExists(),
                "test-corpus directory not found — mapping test skipped");

        Set<String> encountered = new HashSet<>();
        for (Path file : CorpusWalker.allRosettaFiles()) {
            String content = Files.readString(file);
            // Per discipline rule 14: 2-arg helper, byte-offset wiring.
            // Skip parse-error files (covered by AstCorpusRegressionTest).
            // The chaos cell (v3.2 PR-2) DELIBERATELY joins this walk - its
            // authored shapes can only WIDEN the encountered-kind set, and every
            // kind it writes is already mapped (the sibling audit test excludes
            // chaos because its golden pins vendored frequencies; this test pins
            // mapping COMPLETENESS, where more coverage is strictly better).
            try {
                RModel model = AstBuilder.buildFromString(content, file.toString());
                AstWalker.walk(model, node -> {
                    if (node instanceof RRootElement re) {
                        encountered.add(re.getClass().getName());
                    }
                });
            } catch (AstBuildException e) {
                // intentionally skipped
            }
        }

        Set<String> mapped = IRKindForClass.knownFqns();
        Set<String> unmapped = new TreeSet<>();
        for (String fqn : encountered) {
            if (!mapped.contains(fqn)) {
                unmapped.add(fqn);
            }
        }

        assertEquals(Set.of(), unmapped,
                "Encountered RRootElement subclass(es) without IRKind mapping in " +
                        "IRKindForClass.MAP. Add mapping rows AND a matching entry in " +
                        "CorpusExpected.EXPECTED, then update spec Section 4 coverage " +
                        "matrix: " + unmapped);
    }

    /**
     * THE MODEL KIND'S EXCLUSION, stated rather than left to be rediscovered (v3.3 seat 8, PR #644 — the
     * property gate). {@link IRKind#MODEL} is a TOP-LEVEL kind that maps from NO {@code RRootElement}
     * subclass: a model IS the {@code namespace} declaration ({@code RModel extends RNode}, never an
     * {@code RRootElement}), so {@link IRKindForClass} gets no row for it and neither does the sibling
     * registry {@code CorpusExpected.EXPECTED} — which is why the walk above stays green without one.
     *
     * <p>Needs no corpus, so it is not {@code assumeTrue}-skipped: it reads the two registries only.
     *
     * <p><b>Able to fail:</b> add an {@code RModel} row to {@link IRKindForClass} (the first assertion goes
     * red); add a kind to {@link IRKind} without deciding whether an {@code RRootElement} produces it, or
     * drop a mapping row (the second goes red naming the kind).
     */
    @Test
    void theModelKindMapsFromNoRRootElementSubclass() {
        Set<String> mappedKinds = IRKindForClass.knownKinds();
        assertFalse(mappedKinds.contains(IRKind.MODEL.name()),
                "IRKind.MODEL has a row in IRKindForClass.MAP — a model is the namespace declaration " +
                        "itself (RModel extends RNode), not an RRootElement; a row here would also have " +
                        "to appear in CorpusExpected.EXPECTED, which the corpus walk would then refute");

        Set<String> unmappedKinds = new TreeSet<>();
        for (IRKind kind : IRKind.values()) {
            if (!mappedKinds.contains(kind.name())) {
                unmappedKinds.add(kind.name());
            }
        }
        assertEquals(new TreeSet<>(Set.of("MODEL", "FIELD", "PARAMETER", "ENUM_VALUE")), unmappedKinds,
                "the IRKinds NOT produced by an RRootElement subclass are exactly the 3 sub-element kinds " +
                        "plus MODEL — a new kind either gets a mapping row (and a CorpusExpected.EXPECTED " +
                        "entry) or is declared here with its reason");
    }
}
