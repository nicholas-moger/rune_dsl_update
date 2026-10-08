package com.regnosys.rosetta.generator.java.object;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * N4, engine half (v3.1 phase C, C0 item 4) — a model that declares no version emits
 * upstream's EMF default, {@code 0.0.0}, never an empty version.
 *
 * <p><b>Why this is upstream's rule and not a choice of ours.</b> Upstream's model
 * declares the default literally, in {@code rune-dsl/rune-lang/model/Rosetta.xcore}
 * (the vendored tree, local-only):
 *
 * <pre>
 * class RosettaModel extends RosettaDefinable {
 *     String name
 *     contains RosettaScope scope opposite model
 *     String version = "0.0.0"
 *     ...
 * </pre>
 *
 * and the grammar clause is optional — {@code ('version' version=STRING)?}. So Xtext
 * hands upstream's emitters {@code "0.0.0"} for an undeclared model, and the goldens
 * for drr 5.61.0's two {@code techsprint.g20.mas} models carry {@code @version 0.0.0}.
 *
 * <p><b>What the fork did instead.</b> {@code GeneratorModel.version} returned null and
 * the templates' {@code <else>} branches rendered a valueless {@code @version} line —
 * a shape upstream cannot produce, because upstream's version is never null. The
 * harness's unconditional stamp (N4's other half, see
 * {@code D11VersionStampTest}) had been masking it by supplying a version to every
 * model; removing the stamp exposed it.
 *
 * <p>The default cannot move any other cell: band-wide exactly two of 4,062 model
 * files declare no version, and both are the drr 5.61.0 pair witnessed here.
 */
class ModelVersionDefaultTest {

    private static final Path DRR_561_ROSETTA = Path.of(
            "..", "test-corpus", "drr", "drr-5.61.0", "rosetta-source", "src", "main", "rosetta");

    /** A GeneratorModel over a one-model workspace — version resolution needs nothing more. */
    private static GeneratorModel generatorModelFor(RModel model) {
        return new GeneratorModel(RWorkspace.build(List.of(model)).workspace());
    }

    @Test
    void undeclaredVersionResolvesToUpstreamDefault() throws Exception {
        assumeTrue(Files.isDirectory(DRR_561_ROSETTA), "drr 5.61.0 corpus cell absent");
        Path source = DRR_561_ROSETTA.resolve("regulation-techsprint-g20-mas-type.rosetta");
        assumeTrue(Files.isRegularFile(source), "corpus file absent");

        RModel model = AstBuilder.buildFromFile(source);
        assertTrue(model.version().isEmpty(), "the witness model must declare no version");

        assertEquals("0.0.0", generatorModelFor(model).version(model),
                "an undeclared version must resolve to upstream's Rosetta.xcore default "
                        + "(String version = \"0.0.0\"), which is what the golden carries. "
                        + "Returning null renders a valueless @version line — a shape upstream "
                        + "cannot emit.");
    }

    @Test
    void declaredVersionIsUnchanged() throws Exception {
        assumeTrue(Files.isDirectory(DRR_561_ROSETTA), "drr 5.61.0 corpus cell absent");
        Path source = DRR_561_ROSETTA.resolve("regulation-mas-rewrite-trade-type.rosetta");
        assumeTrue(Files.isRegularFile(source), "corpus file absent");

        RModel model = AstBuilder.buildFromFile(source);
        assumeTrue(model.version().isPresent(), "expected a version-declaring model");

        assertEquals(model.version().orElseThrow(), generatorModelFor(model).version(model),
                "a declared version must pass through untouched — the default applies only "
                        + "where the source declares nothing.");
    }
}
