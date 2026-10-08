package com.regnosys.rosetta.generator.java;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.AggregateGenerationException;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * Main orchestrator for Java code generation. Drives all registered
 * generators against each model in the workspace.
 *
 * <p>Replaces upstream's {@code RosettaGenerator implements IGenerator2}.
 * No Xtext/EMF dependency — takes our {@link RWorkspace} and produces
 * a map of file paths to generated Java source.
 *
 * <p>Usage:
 * <pre>
 * RLinkingResult result = RWorkspace.build(models);
 * JavaCodeGenerator codegen = new JavaCodeGenerator(result.workspace());
 * Map&lt;String, String&gt; output = codegen.generate();
 * // output: "com/example/model/Trade.java" → "package com.example.model; ..."
 * </pre>
 */
public class JavaCodeGenerator {

    private final GeneratorModel generatorModel;
    private final List<JavaClassGenerator<?, ?>> generators;
    private final MetaFieldGenerator metaFieldGenerator;
    private final FunctionGenerator functionGenerator;

    /**
     * Create a code generator with the given workspace and registered generators.
     *
     * @param workspace the linked/typed workspace from RWorkspace.build()
     * @param generators the list of generators to run (order matters for output determinism)
     */
    public JavaCodeGenerator(RWorkspace workspace, List<JavaClassGenerator<?, ?>> generators) {
        this.generatorModel = new GeneratorModel(workspace);
        this.generators = generators;
        this.metaFieldGenerator = null;
        this.functionGenerator = null;
    }

    /**
     * Create a code generator with per-model generators and MetaFieldGenerator.
     *
     * @param workspace the linked/typed workspace from RWorkspace.build()
     * @param generators the list of per-model generators
     * @param metaFieldGenerator workspace-wide metafield wrapper generator (D32)
     */
    public JavaCodeGenerator(RWorkspace workspace, List<JavaClassGenerator<?, ?>> generators,
                              MetaFieldGenerator metaFieldGenerator) {
        this.generatorModel = new GeneratorModel(workspace);
        this.generators = generators;
        this.metaFieldGenerator = metaFieldGenerator;
        this.functionGenerator = null;
    }

    /**
     * Create a code generator with per-model generators, MetaFieldGenerator,
     * and FunctionGenerator.
     *
     * @param workspace the linked/typed workspace from RWorkspace.build()
     * @param generators the list of per-model generators
     * @param metaFieldGenerator workspace-wide metafield wrapper generator (D32)
     * @param functionGenerator workspace-wide function code generator (M7b)
     */
    public JavaCodeGenerator(RWorkspace workspace, List<JavaClassGenerator<?, ?>> generators,
                              MetaFieldGenerator metaFieldGenerator,
                              FunctionGenerator functionGenerator) {
        this.generatorModel = new GeneratorModel(workspace);
        this.generators = generators;
        this.metaFieldGenerator = metaFieldGenerator;
        this.functionGenerator = functionGenerator;
    }

    /**
     * Create a code generator with no generators (for testing scaffold).
     */
    public JavaCodeGenerator(RWorkspace workspace) {
        this(workspace, List.of());
    }

    public GeneratorModel getGeneratorModel() {
        return generatorModel;
    }

    /**
     * Run all generators against all models in the workspace.
     *
     * @return map of generated file paths to Java source code
     * @throws AggregateGenerationException if any generator fails
     */
    public Map<String, String> generate() {
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> allErrors = new ArrayList<>();

        for (RModel model : generatorModel.files()) {
            if (!generatorModel.shouldGenerate(model)) {
                continue;
            }
            String version = generatorModel.version(model);

            for (JavaClassGenerator<?, ?> generator : generators) {
                // The D43 IR dispatch seam: with -Drosetta.generator.ir off (the
                // default) or no IR provider registered, this is exactly the
                // legacy generator.generateClasses call — byte-identical by
                // construction (the ServiceLoader inversion of the lab's L-002
                // flag dispatch; zero IR imports here).
                List<GenerationException> errors =
                        IRGeneration.generateClasses(generator, model, version, output);
                allErrors.addAll(errors);
            }
        }

        // Workspace-wide metafield wrapper generation (D32). Same D43 seam
        // shape: flag off ⇒ the legacy metaFieldGenerator.generate call.
        if (metaFieldGenerator != null) {
            IRGeneration.generateMeta(metaFieldGenerator, output);
        }

        // Workspace-wide function generation (M7b).
        // FunctionGenerator#generateWithErrors returns per-function failures into the
        // list rather than throwing — symmetric with the per-model JavaClassGenerator
        // path above (Copilot R9 F1 architectural fix 2026-05-04). At R10 the
        // canonical entry point was renamed from `generate` to `generateWithErrors`
        // and the void `generate(Map)` overload retained as a deprecated discard
        // wrapper for ABI compat (Copilot R10 F1 2026-05-04).
        if (functionGenerator != null) {
            allErrors.addAll(functionGenerator.generateWithErrors(output));
        }

        if (!allErrors.isEmpty()) {
            if (allErrors.size() == 1) {
                throw allErrors.get(0);
            }
            throw new AggregateGenerationException(
                    "Multiple errors during Java code generation",
                    null, allErrors);
        }

        return output;
    }
}
