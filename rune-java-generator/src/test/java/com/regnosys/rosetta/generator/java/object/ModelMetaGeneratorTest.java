package com.regnosys.rosetta.generator.java.object;

import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelMetaGeneratorTest {

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);

    @Test void meta_class_structure() {
        var model = makeModel("cdm.base.datetime");
        model.setVersion("0.0.0.master-SNAPSHOT");
        var dt = new RDataType(); dt.setName("AdjustableDate");
        model.rootElements().add(dt); dt.setParent(model);

        String generated = generate(model, dt);

        // Package
        assertTrue(generated.startsWith("package cdm.base.datetime.meta;\n"));
        // Imports
        assertTrue(generated.contains("import cdm.base.datetime.AdjustableDate;"));
        assertTrue(generated.contains("import com.rosetta.model.lib.annotations.RosettaMeta;"));
        assertTrue(generated.contains("import com.rosetta.model.lib.meta.RosettaMetaData;"));
        // Class declaration
        assertTrue(generated.contains("@RosettaMeta(model=AdjustableDate.class)"));
        assertTrue(generated.contains("public class AdjustableDateMeta implements RosettaMetaData<AdjustableDate>"));
        // Validator references
        assertTrue(generated.contains("AdjustableDateValidator.class"));
        assertTrue(generated.contains("AdjustableDateTypeFormatValidator.class"));
        assertTrue(generated.contains("AdjustableDateOnlyExistsValidator"));
        // Version
        assertTrue(generated.contains("@version 0.0.0.master-SNAPSHOT"));
    }

    @Test void meta_with_conditions() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = new RDataType(); dt.setName("Trade");
        var cond = new RCondition(); cond.setName("TradeValid");
        dt.conditions().add(cond);
        model.rootElements().add(dt); dt.setParent(model);

        String generated = generate(model, dt);

        assertTrue(generated.contains("TradeTradeValid.class"));
        assertTrue(generated.contains("factory.<Trade>create(TradeTradeValid.class)"));
    }

    @Test void meta_no_conditions() {
        var model = makeModel("com.example");
        var dt = new RDataType(); dt.setName("Simple");
        model.rootElements().add(dt); dt.setParent(model);

        String generated = generate(model, dt);

        // Empty data rules list
        assertTrue(generated.contains("return Arrays.asList(\n\t\t);"));
    }

    @Test void meta_inherited_conditions() {
        var model = makeModel("com.example");
        var parent = new RDataType(); parent.setName("Parent");
        var parentCond = new RCondition(); parentCond.setName("ParentRule");
        parent.conditions().add(parentCond);
        model.rootElements().add(parent); parent.setParent(model);

        var child = new RDataType(); child.setName("Child");
        // Use superTypeName so GlobalResolutionPass resolves it via RWorkspace.build()
        child.setSuperTypeName("Parent");
        var childCond = new RCondition(); childCond.setName("ChildRule");
        child.conditions().add(childCond);
        model.rootElements().add(child); child.setParent(model);

        String generated = generate(model, child);

        // Parent condition first, then child condition
        int parentIdx = generated.indexOf("ParentParentRule.class");
        int childIdx = generated.indexOf("ChildChildRule.class");
        assertTrue(parentIdx > 0, "Parent condition should be present");
        assertTrue(childIdx > 0, "Child condition should be present");
        assertTrue(parentIdx < childIdx, "Parent condition should come first");
    }

    @Test void qualify_functions_empty() {
        var model = makeModel("com.example");
        var dt = new RDataType(); dt.setName("Foo");
        model.rootElements().add(dt); dt.setParent(model);

        String generated = generate(model, dt);

        assertTrue(generated.contains("return Collections.emptyList();"));
    }

    // === Helpers ==============================================================

    private String generate(RModel model, RDataType type) {
        var result = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(result.workspace());
        var generator = new ModelMetaGenerator(gm, typeTranslator);
        var metaClass = generator.createTypeRepresentation(type);
        return generator.generate(type, metaClass, gm.version(model));
    }

    private RModel makeModel(String namespace) {
        var model = new RModel();
        model.setNamespace(namespace);
        return model;
    }
}
