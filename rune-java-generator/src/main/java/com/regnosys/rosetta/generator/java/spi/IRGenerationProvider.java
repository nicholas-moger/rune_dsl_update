package com.regnosys.rosetta.generator.java.spi;

import java.util.List;
import java.util.Map;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathUtilGenerator;
import com.regnosys.rosetta.generator.java.object.validators.CardinalityValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

/**
 * Service-provider interface for an IR-routed generation path (the D43 train's
 * Path-2). The shipping generator carries NO IR classes; an IR distribution
 * (the {@code rune-ir-java} module) implements this interface and registers it
 * via {@code META-INF/services}. {@link IRGeneration} discovers it with
 * {@link java.util.ServiceLoader} only when the opt-in flag
 * {@code -Drosetta.generator.ir=true} is set — with the flag off (the default)
 * the provider is never looked up and every call site executes the exact
 * legacy Path-1 code, byte-identical by construction (the lab's L-002
 * contract, inverted from the lab's in-tree {@code instanceof} dispatch to
 * this ServiceLoader seam so the shipping generator has zero IR imports).
 *
 * <p>Two seam families:
 *
 * <ul>
 *   <li><b>Construction seams</b> — mirror the six per-kind generator
 *   constructions whose IR-routed variants exist (pojo, choice, enum,
 *   metafield, function, and since v3.3 seat 3 the data-rule condition
 *   generator). Wiring sites (the D11 harness, future consumers)
 *   construct through {@link IRGeneration}'s helpers so a flag-on run with the
 *   IR distribution present receives the IR-routed subclass; everything else
 *   receives the standard generator.</li>
 *   <li><b>Dispatch seams</b> — mirror the lab's {@code JavaCodeGenerator}
 *   flag-guarded dispatch: the IR-routed variants expose their Path-2 entry
 *   points on separate methods ({@code generateClassesAsIR} /
 *   {@code generateAsIR} in the IR module), so invocation sites must route
 *   through the provider to reach them. A provider returns {@code null} /
 *   {@code false} for a generator it has no IR route for; the caller falls
 *   back to Path-1.</li>
 * </ul>
 */
public interface IRGenerationProvider {

    /** IR-routed variant of {@code new EnumGenerator(gm)}. */
    EnumGenerator enumGenerator(GeneratorModel gm);

    /** IR-routed variant of {@code new ModelObjectGenerator(gm, tt, tu)}. */
    ModelObjectGenerator modelObjectGenerator(GeneratorModel gm, JavaTypeTranslator tt, JavaTypeUtil tu);

    /**
     * IR-routed variant of {@code new ChoiceObjectGenerator(gm, tt, tu, delegate)}.
     * {@code delegate} is the pojo generator the choice generator delegates
     * shared rendering to — the same instance relationship as Path-1.
     */
    ChoiceObjectGenerator choiceObjectGenerator(GeneratorModel gm, JavaTypeTranslator tt, JavaTypeUtil tu,
            ModelObjectGenerator delegate);

    /** IR-routed variant of {@code new MetaFieldGenerator(gm, tt)}. */
    MetaFieldGenerator metaFieldGenerator(GeneratorModel gm, JavaTypeTranslator tt);

    /**
     * IR-routed variant of {@code new FunctionGenerator(gm, tt, tu)}. Unlike
     * the four above, the function route needs no dispatch seam: the IR
     * subclass routes internally via the {@code createExpressionCompiler} /
     * {@code createFunctionExpressionRenderer} factory overrides, so its
     * inherited {@code generateWithErrors} IS the Path-2 entry point.
     */
    FunctionGenerator functionGenerator(GeneratorModel gm, JavaTypeTranslator tt, JavaTypeUtil tu);

    /**
     * IR-routed variant of {@code new DataRuleGenerator(gm, tt, tu)} — the
     * DATA_RULE seam (v3.3 seat 3, D54). Like the function route it needs no
     * dispatch seam: the IR subclass routes its condition bodies internally via
     * the {@code createExpressionCompiler} / {@code createFunctionExpressionRenderer}
     * factory overrides, so its inherited {@code generateClasses} IS the Path-2
     * entry point. ABSTRACT on purpose (not a default): every provider states
     * its verdict on every construction seam — the optimised provider returns
     * the plain legacy generator here because that route has no data-rule
     * twin, and its seam test pins the exact class.
     */
    DataRuleGenerator dataRuleGenerator(GeneratorModel gm, JavaTypeTranslator tt, JavaTypeUtil tu);

    /**
     * IR-routed variant of {@code new OnlyExistsValidatorGenerator(gm, tt, tu)} — one of the FIVE DERIVED-FILE
     * seams (v3.3 seat 9, PR #645 commit 12). A data type's six generated files are emitted as ONE unit, and five
     * of them are written by five separate per-kind generators; without a construction seam for each, no derived
     * file could ever be routed through the IR and the unit's all-or-nothing could not switch on at all.
     *
     * <p>ABSTRACT on purpose (not a default), the data-rule seam's precedent: every provider states its verdict on
     * every construction seam — the optimised provider returns the plain legacy generator because that route has no
     * derived-file twin, and its seam test pins the exact class.
     */
    OnlyExistsValidatorGenerator onlyExistsValidatorGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu);

    /** IR-routed variant of {@code new CardinalityValidatorGenerator(gm, tt, tu)} — see
     * {@link #onlyExistsValidatorGenerator}. */
    CardinalityValidatorGenerator cardinalityValidatorGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu);

    /** IR-routed variant of {@code new TypeFormatValidatorGenerator(gm, tt, tu)} — see
     * {@link #onlyExistsValidatorGenerator}. */
    TypeFormatValidatorGenerator typeFormatValidatorGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu);

    /** IR-routed variant of {@code new ModelMetaGenerator(gm, tt)} — see {@link #onlyExistsValidatorGenerator}. */
    ModelMetaGenerator modelMetaGenerator(GeneratorModel gm, JavaTypeTranslator tt);

    /** IR-routed variant of {@code new DeepPathUtilGenerator(gm, tt, tu)} — see
     * {@link #onlyExistsValidatorGenerator}. */
    DeepPathUtilGenerator deepPathUtilGenerator(GeneratorModel gm, JavaTypeTranslator tt, JavaTypeUtil tu);

    /**
     * Route one per-model generation call through the IR path, or decline.
     *
     * @return the generation errors from the IR route, or {@code null} when
     *         this provider has no IR route for {@code generator} (the caller
     *         falls back to {@code generator.generateClasses})
     */
    List<GenerationException> generateClassesAsIR(JavaClassGenerator<?, ?> generator, RModel model,
            String version, Map<String, String> output);

    /**
     * Route the workspace-wide metafield generation through the IR path, or
     * decline.
     *
     * @return {@code true} when the IR route ran; {@code false} when this
     *         provider has no IR route for {@code metaFieldGenerator} (the
     *         caller falls back to {@code metaFieldGenerator.generate})
     */
    boolean generateMetaAsIR(MetaFieldGenerator metaFieldGenerator, Map<String, String> output);
}
