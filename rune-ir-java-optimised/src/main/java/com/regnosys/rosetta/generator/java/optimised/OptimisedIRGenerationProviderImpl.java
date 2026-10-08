package com.regnosys.rosetta.generator.java.optimised;

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
import com.regnosys.rosetta.generator.java.spi.OptimisedIRGenerationProvider;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

/**
 * The rune-ir-java-optimised implementation of the generator's
 * {@link OptimisedIRGenerationProvider} service. Registered via
 * {@code META-INF/services} under the OPTIMISED interface's name only;
 * discovered by the generator's {@code IRGeneration} loader only when
 * {@code -Drosetta.generator.ir.optimised=true} is set.
 *
 * <p><b>PR-4 state — the FIRST flipped seam.</b> The {@code functionGenerator}
 * construction seam now returns {@link OptimisedFunctionGenerator} — the
 * navigation-chain family, tranche 1 (pure all-single input-PARAM-rooted
 * chains in FUNCTION operation bodies re-emitted as direct ladders; the census
 * {@code research/p3-navigation-family-census.md} § 4/§ 5). Every OTHER seam
 * keeps the PR-3 decline-by-default contract (plain legacy construction;
 * dispatch seams decline): un-flipped kinds execute Path-1 byte-identically,
 * which is what serves the frozen kinds for free. Each further family flip
 * re-pins its exact-class assertion in {@code OptimisedGenerationSeamTest} in
 * the same PR (the un-pin-deliberately contract), behind that family PR's own
 * differential gate.
 */
public final class OptimisedIRGenerationProviderImpl implements OptimisedIRGenerationProvider {

    @Override
    public EnumGenerator enumGenerator(GeneratorModel gm) {
        return new EnumGenerator(gm);
    }

    @Override
    public ModelObjectGenerator modelObjectGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new ModelObjectGenerator(gm, tt, tu);
    }

    @Override
    public ChoiceObjectGenerator choiceObjectGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu, ModelObjectGenerator delegate) {
        return new ChoiceObjectGenerator(gm, tt, tu, delegate);
    }

    @Override
    public MetaFieldGenerator metaFieldGenerator(GeneratorModel gm, JavaTypeTranslator tt) {
        return new MetaFieldGenerator(gm, tt);
    }

    @Override
    public FunctionGenerator functionGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        // THE PR-4 FLIP — the navigation-chain family's generator (tranche 1).
        return new OptimisedFunctionGenerator(gm, tt, tu);
    }

    @Override
    public DataRuleGenerator dataRuleGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        // The DATA_RULE seam (v3.3 seat 3, D54) DECLINES on this route: the optimised route
        // has no data-rule twin (its census reads the kind as owner-less, no funcGen emission
        // path), so the plain legacy generator renders the conditions - Path-1, byte-identical.
        // An explicit override, never a default: the seam test pins the exact class.
        return new DataRuleGenerator(gm, tt, tu);
    }

    // THE FIVE DERIVED-FILE SEAMS (v3.3 seat 9, PR #645 commit 12) DECLINE on this route: the optimised backend
    // has no per-type derived-file family, so the plain legacy generators render them - Path-1, byte-identical. An
    // explicit override, never a default: the seam test pins the exact class, the data-rule seam's precedent.

    @Override
    public OnlyExistsValidatorGenerator onlyExistsValidatorGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new OnlyExistsValidatorGenerator(gm, tt, tu);
    }

    @Override
    public CardinalityValidatorGenerator cardinalityValidatorGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new CardinalityValidatorGenerator(gm, tt, tu);
    }

    @Override
    public TypeFormatValidatorGenerator typeFormatValidatorGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new TypeFormatValidatorGenerator(gm, tt, tu);
    }

    @Override
    public ModelMetaGenerator modelMetaGenerator(GeneratorModel gm, JavaTypeTranslator tt) {
        return new ModelMetaGenerator(gm, tt);
    }

    @Override
    public DeepPathUtilGenerator deepPathUtilGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new DeepPathUtilGenerator(gm, tt, tu);
    }

    @Override
    public List<GenerationException> generateClassesAsIR(JavaClassGenerator<?, ?> generator,
            RModel model, String version, Map<String, String> output) {
        // No optimised route exists for any generator yet — Path-1 fallback.
        return null;
    }

    @Override
    public boolean generateMetaAsIR(MetaFieldGenerator metaFieldGenerator, Map<String, String> output) {
        // No optimised route exists for the metafield generation yet — Path-1 fallback.
        return false;
    }
}
