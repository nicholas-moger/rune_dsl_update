package com.regnosys.rosetta.generator.java.ir;

import java.util.List;
import java.util.Map;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;

/**
 * Marker interface implemented by lab-side, IR-routed variants of the per-model
 * {@code JavaClassGenerator}s. When the {@link IRFlag} is enabled,
 * {@code JavaCodeGenerator} dispatches matching generators through
 * {@link #generateClassesAsIR} instead of {@code generateClasses} (decision
 * L-002), keeping Path-1 untouched.
 *
 * <p>Implementations build the declaration IR for the model via the AST→IR
 * adapter (exercising and reconciling it over real corpus models) and then emit
 * byte-identically - through the unchanged Path-1 pipeline (decision L-001) for
 * the kinds no NEW IR emitter writes yet, and from the IR ALONE for the kinds one
 * does ({@link IREnumGenerator} since v3.3 seat 6, PR #642 - the files it wrote
 * named by {@link #filesWrittenByIrEmitter()}).
 *
 * <p>Lab-authored Phase-1 (not present upstream).
 */
public interface IREmittableGenerator {

    /**
     * IR-routed counterpart of {@code JavaClassGenerator#generateClasses}. Same
     * contract: writes generated files into {@code output} and returns the list
     * of generation errors (empty on success) rather than throwing.
     */
    List<GenerationException> generateClassesAsIR(RModel model, String version,
                                                  Map<String, String> output);

    /**
     * The output keys of the files a NEW IR EMITTER wrote - from the IR alone, the old generator class not
     * involved - over this generator's passes so far. The D11 host reads this per cell and sub-kind, books every
     * OTHER emitted file to the old generator and holds that set EQUAL to the committed fallback register
     * ({@code d11-ir-fallbacks.txt}); an emitter PR overrides this and shrinks the register in the same commit
     * (v3.3 seat 5, decision D55 - rulings R3 / R4). Overridden by {@link IREnumGenerator} since v3.3 seat 6
     * (PR #642): every enum file. The data-type and choice files are still written by the inherited Phase-1
     * pipeline (this default), and so are the body files.
     */
    default java.util.Set<String> filesWrittenByIrEmitter() {
        return java.util.Set.of();
    }
}
