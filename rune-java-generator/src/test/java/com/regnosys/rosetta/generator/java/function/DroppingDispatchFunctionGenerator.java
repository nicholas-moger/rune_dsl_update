package com.regnosys.rosetta.generator.java.function;

import java.util.List;
import java.util.Map;

import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

/**
 * v3.2 seat 3 (F12) — the accounting counter's positive control (LAW 81): a function generator
 * that DROPS every dispatch group's file without raising an error — the exact class of defect the
 * bare-name grouping produced on the chaos {@code C4Speed} rows. Same-package so it can override
 * the package-private {@link FunctionGenerator#generateDispatchFunction}; used by
 * {@code SilentPairSeatTest.control5_accountingCounter_firesForEveryDroppedFunction_withItsPath}.
 */
public class DroppingDispatchFunctionGenerator extends FunctionGenerator {

    public DroppingDispatchFunctionGenerator(GeneratorModel generatorModel,
                                             JavaTypeTranslator typeTranslator, JavaTypeUtil typeUtil) {
        super(generatorModel, typeTranslator, typeUtil);
    }

    @Override
    void generateDispatchFunction(String baseName, List<RFunction> variants, RFunction base,
                                  Map<String, String> output) {
        // the mutation: the group's file is never written and no error is raised
    }
}
