package com.regnosys.rosetta.generator.java.ir;

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
import com.regnosys.rosetta.generator.java.spi.IRGenerationProvider;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

/**
 * The rune-ir-java implementation of the generator's {@link IRGenerationProvider}
 * service — the ServiceLoader inversion of the lab's in-tree flag dispatch
 * (L-002). Registered via {@code META-INF/services}; discovered by the
 * generator's {@code IRGeneration} loader only when {@code -Drosetta.generator.ir=true}
 * is set. The construction seams return the lab's IR-routed generator variants;
 * the dispatch seams carry the lab's {@code instanceof IREmittable*} checks —
 * moved HERE from the lab's {@code JavaCodeGenerator} so the shipping generator
 * has zero IR imports. Fork-authored (not part of the lab port).
 */
public final class IRGenerationProviderImpl implements IRGenerationProvider {

    @Override
    public EnumGenerator enumGenerator(GeneratorModel gm) {
        return new IREnumGenerator(gm);
    }

    @Override
    public ModelObjectGenerator modelObjectGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new IRModelObjectGenerator(gm, tt, tu);
    }

    @Override
    public ChoiceObjectGenerator choiceObjectGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu, ModelObjectGenerator delegate) {
        return new IRChoiceObjectGenerator(gm, tt, tu, delegate);
    }

    @Override
    public MetaFieldGenerator metaFieldGenerator(GeneratorModel gm, JavaTypeTranslator tt) {
        return new IRMetaFieldGenerator(gm, tt);
    }

    @Override
    public FunctionGenerator functionGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new IRFunctionGenerator(gm, tt, tu);
    }

    @Override
    public DataRuleGenerator dataRuleGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        // v3.3 seat 3 (D54): the DATA_RULE seam - condition bodies through the IR compiler.
        return new IRDataRuleGenerator(gm, tt, tu);
    }

    // THE FIVE DERIVED-FILE SEAMS (v3.3 seat 9, PR #645 commit 12): a data type's six files are emitted as ONE
    // unit, and five of them are written by five separate per-kind generators - so each kind needs its own IR-routed
    // variant before the unit can write anything at all. SINCE COMMIT 15 the unit is AVAILABLE
    // (IRTypeUnitWiring.READY_MEMBERS names all six members and IRTypeUnitWiring.AVAILABLE is true), so each of
    // these writes its member's file from the IR for every data type the unit does not refuse whole; a refused
    // type takes the INHERITED generator's path for all six of its files (PR #645 round 1 cq SF-1).

    @Override
    public OnlyExistsValidatorGenerator onlyExistsValidatorGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new IROnlyExistsValidatorGenerator(gm, tt, tu);
    }

    @Override
    public CardinalityValidatorGenerator cardinalityValidatorGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new IRCardinalityValidatorGenerator(gm, tt, tu);
    }

    @Override
    public TypeFormatValidatorGenerator typeFormatValidatorGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new IRTypeFormatValidatorGenerator(gm, tt, tu);
    }

    @Override
    public ModelMetaGenerator modelMetaGenerator(GeneratorModel gm, JavaTypeTranslator tt) {
        return new IRModelMetaGenerator(gm, tt);
    }

    @Override
    public DeepPathUtilGenerator deepPathUtilGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        return new IRDeepPathUtilGenerator(gm, tt, tu);
    }

    @Override
    public List<GenerationException> generateClassesAsIR(JavaClassGenerator<?, ?> generator,
            RModel model, String version, Map<String, String> output) {
        if (generator instanceof IREmittableGenerator irGenerator) {
            return irGenerator.generateClassesAsIR(model, version, output);
        }
        return null;
    }

    @Override
    public boolean generateMetaAsIR(MetaFieldGenerator metaFieldGenerator, Map<String, String> output) {
        if (metaFieldGenerator instanceof IREmittableMetaGenerator irMetaGenerator) {
            irMetaGenerator.generateAsIR(output);
            return true;
        }
        return false;
    }
}
