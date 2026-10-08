package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel.AliasModel;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel.ConditionModel;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel.DependencyModel;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel.DispatchVariantModel;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel.OperationModel;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel.ParamModel;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel.PathSegmentModel;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FunctionTemplateModel} and its inner sub-model POJOs.
 */
class FunctionTemplateModelTest {

    // =========================================================================
    // Helper builders
    // =========================================================================

    private static FunctionTemplateModel minimalModel() {
        return new FunctionTemplateModel(
                "com.example.functions",        // packageName
                "MyFunc",                        // className
                List.of("java.util.List"),       // imports
                List.of(),                       // staticImports
                false,                           // isQualify
                false,                           // isDispatch
                false,                           // overridesEvaluate
                null,                            // qualifyGenericType
                null,                            // labelProviderClassName
                null,                            // version
                false,                           // hasDeepOperations
                List.of(),                       // inputs
                null,                            // output (void)
                false,                           // outputIsMulti
                false,                           // outputNeedsBuilder
                List.of(),                       // aliases
                List.of(),                       // operations
                List.of(),                       // preConditions
                List.of(),                       // postConditions
                false,                           // hasConditions
                false,                           // hasObjectValidator
                List.of(),                       // dependencies
                null,                            // superClassName
                null,                            // superClassFqn
                null,                            // definition
                List.of(),                       // javadocParams
                List.of(),                       // dispatchVariants
                null,                            // dispatchParamName
                null                             // dispatchEnumType
        );
    }

    // =========================================================================
    // Identity
    // =========================================================================

    @Test
    void identity_fields_round_trip() {
        var model = minimalModel();
        assertEquals("com.example.functions", model.getPackageName());
        assertEquals("MyFunc", model.getClassName());
        assertEquals(List.of("java.util.List"), model.getImports());
    }

    // =========================================================================
    // Category flags
    // =========================================================================

    @Test
    void category_flags_false_by_default() {
        var model = minimalModel();
        assertFalse(model.getIsQualify());
        assertFalse(model.getIsDispatch());
        assertFalse(model.getOverridesEvaluate());
        assertFalse(model.getHasDeepOperations());
        assertNull(model.getQualifyGenericType());
        assertNull(model.getLabelProviderClassName());
        assertNull(model.getVersion());
    }

    @Test
    void qualify_function_flags() {
        var model = new FunctionTemplateModel(
                "pkg", "Qualify_Trade", List.of(), List.of(),
                true, false, true, "Trade", null, null, false,
                List.of(), null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(), null, null);

        assertTrue(model.getIsQualify());
        assertTrue(model.getOverridesEvaluate());
        assertEquals("Trade", model.getQualifyGenericType());
    }

    @Test
    void dispatch_function_flags() {
        var model = new FunctionTemplateModel(
                "pkg", "PricingDispatch", List.of(), List.of(),
                false, true, false, null, null, "2.0.0", false,
                List.of(), null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(), "pricingType", "PricingTypeEnum");

        assertTrue(model.getIsDispatch());
        assertEquals("2.0.0", model.getVersion());
        assertEquals("pricingType", model.getDispatchParamName());
        assertEquals("PricingTypeEnum", model.getDispatchEnumType());
    }

    // =========================================================================
    // Parameters
    // =========================================================================

    @Test
    void output_null_for_void_function() {
        var model = minimalModel();
        assertNull(model.getOutput());
        assertFalse(model.getOutputIsMulti());
        assertFalse(model.getOutputNeedsBuilder());
    }

    @Test
    void inputs_and_output_round_trip() {
        var input = new ParamModel("trade", "Trade", "com.example.Trade", false, false, true);
        var output = new ParamModel("result", "Boolean", "java.lang.Boolean", false, false, false);

        var model = new FunctionTemplateModel(
                "pkg", "ValidateTrade", List.of(), List.of(),
                false, false, false, null, null, null, false,
                List.of(input), output, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(), null, null);

        assertEquals(1, model.getInputs().size());
        assertSame(input, model.getInputs().get(0));
        assertSame(output, model.getOutput());
    }

    // =========================================================================
    // Body
    // =========================================================================

    @Test
    void aliases_round_trip() {
        var alias = new AliasModel("tradeDate", "Date", List.of(), "trade.getDate()", false);

        var model = new FunctionTemplateModel(
                "pkg", "Func", List.of(), List.of(),
                false, false, false, null, null, null, false,
                List.of(), null, false, false,
                List.of(alias), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(), null, null);

        assertEquals(1, model.getAliases().size());
        assertEquals("tradeDate", model.getAliases().get(0).getName());
    }

    @Test
    void operations_round_trip() {
        var op = new OperationModel("set", "result.setValue(input.getValue())", List.of(), false);

        var model = new FunctionTemplateModel(
                "pkg", "Func", List.of(), List.of(),
                false, false, false, null, null, null, false,
                List.of(), null, false, false,
                List.of(), List.of(op), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(), null, null);

        assertEquals(1, model.getOperations().size());
        assertEquals("set", model.getOperations().get(0).getOperator());
        assertEquals("result.setValue(input.getValue())",
                model.getOperations().get(0).getCompiledStatement());
    }

    @Test
    void pre_and_post_conditions_round_trip() {
        var pre = new ConditionModel("PreCheck", "input must exist",
                "input != null");
        var post = new ConditionModel("PostCheck", "result must be positive",
                "result > 0");

        var model = new FunctionTemplateModel(
                "pkg", "Func", List.of(), List.of(),
                false, false, false, null, null, null, false,
                List.of(), null, false, false,
                List.of(), List.of(), List.of(pre), List.of(post),
                true, false, List.of(),
                null, null, null, List.of(),
                List.of(), null, null);

        assertEquals(1, model.getPreConditions().size());
        assertEquals("PreCheck", model.getPreConditions().get(0).getName());
        assertEquals(1, model.getPostConditions().size());
        assertEquals("PostCheck", model.getPostConditions().get(0).getName());
        assertTrue(model.getHasConditions());
    }

    // =========================================================================
    // Dependencies
    // =========================================================================

    @Test
    void dependencies_round_trip() {
        var dep = new DependencyModel("calculatePrice", "CalculatePrice",
                "com.example.functions.CalculatePrice", true);

        var model = new FunctionTemplateModel(
                "pkg", "Func", List.of(), List.of(),
                false, false, false, null, null, null, false,
                List.of(), null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, true, List.of(dep),
                null, null, null, List.of(),
                List.of(), null, null);

        assertEquals(1, model.getDependencies().size());
        assertEquals("calculatePrice", model.getDependencies().get(0).getFieldName());
        assertEquals("CalculatePrice", model.getDependencies().get(0).getTypeName());
        assertEquals("com.example.functions.CalculatePrice",
                model.getDependencies().get(0).getTypeFqn());
        assertTrue(model.getDependencies().get(0).getIsFunction());
        assertTrue(model.getHasObjectValidator());
    }

    // =========================================================================
    // Super function
    // =========================================================================

    @Test
    void super_class_null_when_not_set() {
        var model = minimalModel();
        assertNull(model.getSuperClassName());
        assertNull(model.getSuperClassFqn());
    }

    @Test
    void super_class_round_trip() {
        var model = new FunctionTemplateModel(
                "pkg", "ConcreteFunc", List.of(), List.of(),
                false, false, false, null, null, null, false,
                List.of(), null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                "AbstractFunc", "com.example.functions.AbstractFunc",
                null, List.of(),
                List.of(), null, null);

        assertEquals("AbstractFunc", model.getSuperClassName());
        assertEquals("com.example.functions.AbstractFunc", model.getSuperClassFqn());
    }

    // =========================================================================
    // Documentation
    // =========================================================================

    @Test
    void definition_and_javadoc_params_round_trip() {
        var model = new FunctionTemplateModel(
                "pkg", "Func", List.of(), List.of(),
                false, false, false, null, null, null, false,
                List.of(), null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null,
                "Validates a trade.", List.of("@param trade the trade to validate"),
                List.of(), null, null);

        assertEquals("Validates a trade.", model.getDefinition());
        assertEquals(1, model.getJavadocParams().size());
        assertEquals("@param trade the trade to validate",
                model.getJavadocParams().get(0));
    }

    // =========================================================================
    // Dispatch variants
    // =========================================================================

    @Test
    void dispatch_variants_round_trip() {
        var variantModel = minimalModel();
        var variant = new DispatchVariantModel("FIXED", "Pricing_Fixed", "pricing_Fixed", variantModel);

        var model = new FunctionTemplateModel(
                "pkg", "PricingDispatch", List.of(), List.of(),
                false, true, false, null, null, "1.0", false,
                List.of(), null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(variant), "pricingType", "PricingTypeEnum");

        assertEquals(1, model.getDispatchVariants().size());
        assertEquals("FIXED", model.getDispatchVariants().get(0).getEnumValue());
        assertEquals("Pricing_Fixed", model.getDispatchVariants().get(0).getVariantClassName());
        assertSame(variantModel, model.getDispatchVariants().get(0).getVariantModel());
    }

    // =========================================================================
    // Immutability
    // =========================================================================

    @Test
    void imports_are_immutable() {
        var mutableImports = new ArrayList<>(List.of("a.B", "c.D"));
        var model = new FunctionTemplateModel(
                "pkg", "Func", mutableImports, List.of(),
                false, false, false, null, null, null, false,
                List.of(), null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(), null, null);

        mutableImports.add("e.F");
        assertEquals(2, model.getImports().size(), "Import list must be an immutable copy");
        assertThrows(UnsupportedOperationException.class, () -> model.getImports().add("x.Y"));
    }

    @Test
    void inputs_are_immutable() {
        var input = new ParamModel("x", "Integer", "java.lang.Integer", false, false, false);
        var mutableInputs = new ArrayList<>(List.of(input));
        var model = new FunctionTemplateModel(
                "pkg", "Func", List.of(), List.of(),
                false, false, false, null, null, null, false,
                mutableInputs, null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(), null, null);

        mutableInputs.add(new ParamModel("y", "String", "java.lang.String", false, false, false));
        assertEquals(1, model.getInputs().size(), "Inputs list must be an immutable copy");
        assertThrows(UnsupportedOperationException.class, () -> model.getInputs().add(null));
    }

    @Test
    void dependencies_are_immutable() {
        var dep = new DependencyModel("foo", "Foo", "com.example.Foo", false);
        var mutableDeps = new ArrayList<>(List.of(dep));
        var model = new FunctionTemplateModel(
                "pkg", "Func", List.of(), List.of(),
                false, false, false, null, null, null, false,
                List.of(), null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, mutableDeps,
                null, null, null, List.of(),
                List.of(), null, null);

        mutableDeps.add(new DependencyModel("bar", "Bar", "com.example.Bar", false));
        assertEquals(1, model.getDependencies().size(), "Dependencies must be an immutable copy");
        assertThrows(UnsupportedOperationException.class, () -> model.getDependencies().add(null));
    }

    @Test
    void dispatch_variants_are_immutable() {
        var variant = new DispatchVariantModel("A", "Func_A", "func_A", minimalModel());
        var mutableVariants = new ArrayList<>(List.of(variant));
        var model = new FunctionTemplateModel(
                "pkg", "Dispatch", List.of(), List.of(),
                false, true, false, null, null, null, false,
                List.of(), null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                mutableVariants, "type", "TypeEnum");

        mutableVariants.add(new DispatchVariantModel("B", "Func_B", "func_B", minimalModel()));
        assertEquals(1, model.getDispatchVariants().size(),
                "Dispatch variants must be an immutable copy");
        assertThrows(UnsupportedOperationException.class,
                () -> model.getDispatchVariants().add(null));
    }

    // =========================================================================
    // multiInputNullChecks parity facet (PR multiinput_nullcheck_blank)
    // =========================================================================

    private static FunctionTemplateModel modelWithInputs(List<ParamModel> inputs) {
        return new FunctionTemplateModel(
                "com.example.functions", "MyFunc", List.of(), List.of(),
                false, false, false, null, null, null, false,
                inputs, null, false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(), null, null);
    }

    @Test
    void multi_input_null_check_has_no_trailing_newline() {
        // A single multi (list) input renders one normalization block ending at
        // the closing brace with NO trailing newline. java-function.stg places
        // <m.multiInputNullChecks> on its own line and supplies the line
        // terminator; a trailing newline here would double into a spurious blank
        // line before the first doEvaluate body statement that the golden never
        // emits.
        var model = modelWithInputs(List.of(
                new ParamModel("businessCenters", "BusinessCenterEnum",
                        "com.example.BusinessCenterEnum", true, false, false)));
        assertEquals(
                "\t\t\tif (businessCenters == null) {\n"
              + "\t\t\t\tbusinessCenters = Collections.emptyList();\n"
              + "\t\t\t}",
                model.getMultiInputNullChecks());
    }

    @Test
    void multi_input_null_check_empty_when_no_multi_inputs() {
        // Scalar-only inputs produce an EMPTY string; ST4 trims the whole
        // <m.multiInputNullChecks> line, so no-list functions emit no blank.
        var model = modelWithInputs(List.of(
                new ParamModel("date", "Date", "com.example.Date", false, false, false)));
        assertEquals("", model.getMultiInputNullChecks());
    }

    @Test
    void multi_input_null_check_multiple_blocks_no_trailing_blank() {
        // Two multi inputs render two blocks separated by a single newline, with
        // no trailing newline after the last block (so only the inter-block break
        // survives — never a trailing blank line).
        var model = modelWithInputs(List.of(
                new ParamModel("a", "Foo", "com.example.Foo", true, false, false),
                new ParamModel("b", "Bar", "com.example.Bar", true, false, false)));
        assertEquals(
                "\t\t\tif (a == null) {\n\t\t\t\ta = Collections.emptyList();\n\t\t\t}\n"
              + "\t\t\tif (b == null) {\n\t\t\t\tb = Collections.emptyList();\n\t\t\t}",
                model.getMultiInputNullChecks());
    }

    // =========================================================================
    // assignOutput operations-separator parity facet (PR operations_separator_blank)
    //
    // Golden emits a blank line after EACH operation statement in assignOutput
    // (between consecutive operations AND after the last one, before the return).
    // The fork previously emitted only ONE trailing blank after all operations.
    // The fix adds an inter-op separator ("\t\t\t\n") to the
    // <m.operations:{...}> loop while keeping the existing
    // <if(m.operations)><\t><\t><\t> trailing blank — so a separator fires only
    // BETWEEN iterations (0-op and 1-op bodies render byte-identically to before).
    // =========================================================================

    /** Render the full functionFile template for the given model (NoIndentWriter,
     *  matching the production FunctionGenerator path; output is \n-normalized). */
    private static String renderFunction(FunctionTemplateModel model) {
        TemplateRenderer renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath("templates/java-function.stg");
        return renderer.renderNoIndent("templates/java-function.stg", "functionFile", "m", model);
    }

    private static FunctionTemplateModel modelWithOperations(List<OperationModel> ops) {
        return new FunctionTemplateModel(
                "com.example.functions", "MyFunc", List.of(), List.of(),
                false, false, false, null, null, null, false,
                List.of(new ParamModel("input", "Foo", "com.example.Foo", false, false, true)),
                new ParamModel("result", "Bar", "com.example.Bar", false, false, true),
                false, false,
                List.of(), ops, List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(), null, null);
    }

    @Test
    void assign_output_blank_line_between_each_operation() {
        // Two operations → blank between them AND after the last (before return).
        String code = renderFunction(modelWithOperations(List.of(
                new OperationModel("set", "result.setX(input.getX());", List.of(), false),
                new OperationModel("set", "result.setY(input.getY());", List.of(), false))));
        assertTrue(code.contains(
                "\t\t\tresult.setX(input.getX());\n"
              + "\t\t\t\n"
              + "\t\t\tresult.setY(input.getY());\n"
              + "\t\t\t\n"
              + "\t\t\treturn result;"),
                "Expected a blank line after each operation in assignOutput; got:\n" + code);
    }

    @Test
    void assign_output_single_operation_one_trailing_blank() {
        // Regression guard: a single-operation function keeps exactly ONE trailing
        // blank (the separator fires only between iterations) — byte-identical to
        // the pre-fix rendering, so no previously-green 1-op function regresses.
        String code = renderFunction(modelWithOperations(List.of(
                new OperationModel("set", "result.setX(input.getX());", List.of(), false))));
        assertTrue(code.contains(
                "\t\t\tresult.setX(input.getX());\n"
              + "\t\t\t\n"
              + "\t\t\treturn result;"),
                "Expected exactly one trailing blank after the single operation; got:\n" + code);
        // And NOT a double blank.
        assertFalse(code.contains(
                "\t\t\tresult.setX(input.getX());\n\t\t\t\n\t\t\t\n"),
                "Single-operation assignOutput must not emit a double blank; got:\n" + code);
    }

    @Test
    void assign_output_no_operations_no_blank() {
        // Zero operations → no blank at all before the return (the separator and
        // the <if(m.operations)> trailing both no-op). Byte-identical to pre-fix.
        String code = renderFunction(modelWithOperations(List.of()));
        assertTrue(code.contains("assignOutput(Bar result, Foo input) {\n\t\t\treturn result;"),
                "Expected no blank between assignOutput's brace and return; got:\n" + code);
    }

    /** Render the dispatch template (java-function-dispatch.stg) for a model whose
     *  single variant carries the given operations — the dispatch-variant
     *  assignOutput carries the SAME inter-op separator fix as the standard
     *  template, one indent level deeper (4 tabs). */
    private static String renderDispatch(List<OperationModel> variantOps) {
        TemplateRenderer renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath("templates/java-function-dispatch.stg");
        var variant = new FunctionTemplateModel.DispatchVariantModel(
                "SomeEnumValue", "MyFuncVariant", "myFuncVariant",
                modelWithOperations(variantOps));
        var model = new FunctionTemplateModel(
                "com.example.functions", "MyFunc", List.of(), List.of(),
                false, true, false, null, null, "test", false,
                List.of(new ParamModel("input", "Foo", "com.example.Foo", false, false, true)),
                new ParamModel("result", "Bar", "com.example.Bar", false, false, true),
                false, false,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null, null, List.of(),
                List.of(variant), "input", "SomeEnum");
        return renderer.renderNoIndent("templates/java-function-dispatch.stg", "dispatchFile", "m", model);
    }

    @Test
    void dispatch_variant_assign_output_blank_line_between_each_operation() {
        // Same facet, second template: the dispatch-variant assignOutput must also
        // emit a blank line after each operation (4-tab indent). Regression-safe by
        // the same construction (the separator fires only between iterations).
        String code = renderDispatch(List.of(
                new OperationModel("set", "result.setX(input.getX());", List.of(), false),
                new OperationModel("set", "result.setY(input.getY());", List.of(), false)));
        assertTrue(code.contains(
                "\t\t\t\tresult.setX(input.getX());\n"
              + "\t\t\t\t\n"
              + "\t\t\t\tresult.setY(input.getY());\n"
              + "\t\t\t\t\n"
              + "\t\t\t\treturn result;"),
                "Expected a blank line after each dispatch-variant operation; got:\n" + code);
    }

    @Test
    void dispatch_variant_assign_output_single_operation_one_trailing_blank() {
        // Regression guard for the dispatch template: a single-op variant keeps
        // exactly one trailing blank, byte-identical to the pre-fix rendering.
        String code = renderDispatch(List.of(
                new OperationModel("set", "result.setX(input.getX());", List.of(), false)));
        assertTrue(code.contains(
                "\t\t\t\tresult.setX(input.getX());\n"
              + "\t\t\t\t\n"
              + "\t\t\t\treturn result;"),
                "Expected one trailing blank after the single dispatch-variant op; got:\n" + code);
        assertFalse(code.contains(
                "\t\t\t\tresult.setX(input.getX());\n\t\t\t\t\n\t\t\t\t\n"),
                "Single-op dispatch-variant assignOutput must not emit a double blank; got:\n" + code);
    }

    // =========================================================================
    // Sub-model: ParamModel
    // =========================================================================

    @Test
    void param_model_all_fields() {
        var param = new ParamModel("trade", "Trade", "com.example.Trade", true, false, true);
        assertEquals("trade", param.getName());
        assertEquals("Trade", param.getTypeName());
        assertEquals("com.example.Trade", param.getTypeFqn());
        assertTrue(param.getIsMulti());
        assertFalse(param.getIsMeta());
        assertTrue(param.getIsRosettaModelType());
    }

    @Test
    void param_model_meta_flag() {
        var param = new ParamModel("wrappedTrade",
                "FieldWithMetaTrade", "com.example.metafields.FieldWithMetaTrade",
                false, true, false);
        assertTrue(param.getIsMeta());
        assertFalse(param.getIsMulti());
    }

    // =========================================================================
    // Sub-model: AliasModel
    // =========================================================================

    @Test
    void alias_model_all_fields() {
        var alias = new AliasModel("tradeDate", "Date",
                List.of("Trade trade"), "trade.getDate()", false);
        assertEquals("tradeDate", alias.getName());
        assertEquals("Date", alias.getReturnType());
        assertEquals(List.of("Trade trade"), alias.getParams());
        assertEquals("trade.getDate()", alias.getCompiledBody());
        assertFalse(alias.getUsesOutput());
    }

    @Test
    void alias_model_uses_output_flag() {
        var alias = new AliasModel("priceCheck", "Boolean", List.of(),
                "output.getValue() > 0", true);
        assertTrue(alias.getUsesOutput());
    }

    @Test
    void alias_model_params_are_immutable() {
        var mutableParams = new ArrayList<>(List.of("String s"));
        var alias = new AliasModel("a", "void", mutableParams, "body", false);
        mutableParams.add("int i");
        assertEquals(1, alias.getParams().size(), "Alias params must be immutable");
        assertThrows(UnsupportedOperationException.class, () -> alias.getParams().add("x"));
    }

    // =========================================================================
    // Sub-model: OperationModel
    // =========================================================================

    @Test
    void operation_model_set_operator() {
        var op = new OperationModel("set", "result.setValue(42)", List.of(), false);
        assertEquals("set", op.getOperator());
        assertEquals("result.setValue(42)", op.getCompiledStatement());
        assertEquals(List.of(), op.getTargetPath());
        assertFalse(op.getIsAsKey());
    }

    @Test
    void operation_model_add_operator() {
        var op = new OperationModel("add", "result.addItem(item)", List.of(), false);
        assertEquals("add", op.getOperator());
        assertEquals("result.addItem(item)", op.getCompiledStatement());
    }

    @Test
    void operation_model_target_path_and_is_as_key() {
        var seg1 = new PathSegmentModel("output", "MyType", false, false);
        var seg2 = new PathSegmentModel("field", "FieldType", true, false);
        var op = new OperationModel("set", "result.setField(v)", List.of(seg1, seg2), true);
        assertEquals(2, op.getTargetPath().size());
        assertSame(seg1, op.getTargetPath().get(0));
        assertSame(seg2, op.getTargetPath().get(1));
        assertTrue(op.getIsAsKey());
    }

    @Test
    void operation_model_target_path_is_immutable() {
        var seg = new PathSegmentModel("output", "MyType", false, false);
        var mutablePath = new ArrayList<>(List.of(seg));
        var op = new OperationModel("set", "result.setField(v)", mutablePath, false);
        mutablePath.add(new PathSegmentModel("extra", "ExtraType", false, false));
        assertEquals(1, op.getTargetPath().size(), "targetPath must be an immutable copy");
        assertThrows(UnsupportedOperationException.class, () -> op.getTargetPath().add(null));
    }

    // =========================================================================
    // Sub-model: PathSegmentModel
    // =========================================================================

    @Test
    void path_segment_model_all_fields() {
        var seg = new PathSegmentModel("tradeDate", "Date", false, false);
        assertEquals("tradeDate", seg.getName());
        assertEquals("Date", seg.getTypeName());
        assertFalse(seg.getIsMulti());
        assertFalse(seg.getIsMeta());
    }

    @Test
    void path_segment_model_multi_flag() {
        var seg = new PathSegmentModel("items", "Item", true, false);
        assertTrue(seg.getIsMulti());
        assertFalse(seg.getIsMeta());
    }

    @Test
    void path_segment_model_meta_flag() {
        var seg = new PathSegmentModel("wrappedValue", "FieldWithMetaString", false, true);
        assertFalse(seg.getIsMulti());
        assertTrue(seg.getIsMeta());
    }

    // =========================================================================
    // Sub-model: ConditionModel
    // =========================================================================

    @Test
    void condition_model_all_fields() {
        var cond = new ConditionModel("NotNull", "input must not be null",
                "Objects.requireNonNull(input)");
        assertEquals("NotNull", cond.getName());
        assertEquals("input must not be null", cond.getDefinition());
        assertEquals("Objects.requireNonNull(input)", cond.getCompiledExpression());
    }

    // =========================================================================
    // Sub-model: DependencyModel
    // =========================================================================

    @Test
    void dependency_model_all_fields() {
        var dep = new DependencyModel("myFunc", "MyFunc", "com.example.functions.MyFunc", true);
        assertEquals("myFunc", dep.getFieldName());
        assertEquals("MyFunc", dep.getTypeName());
        assertEquals("com.example.functions.MyFunc", dep.getTypeFqn());
        assertTrue(dep.getIsFunction());
    }

    @Test
    void dependency_model_is_function_false_for_service() {
        var dep = new DependencyModel("validator", "MyValidator",
                "com.example.validators.MyValidator", false);
        assertFalse(dep.getIsFunction());
    }

    // =========================================================================
    // Sub-model: DispatchVariantModel
    // =========================================================================

    @Test
    void dispatch_variant_model_all_fields() {
        var inner = minimalModel();
        var variant = new DispatchVariantModel("SPOT", "Pricing_Spot", "pricing_Spot", inner);
        assertEquals("SPOT", variant.getEnumValue());
        assertEquals("Pricing_Spot", variant.getVariantClassName());
        assertEquals("pricing_Spot", variant.getFieldName());
        assertSame(inner, variant.getVariantModel());
    }

    @Test
    void dispatch_variant_model_null_variant_model_accepted() {
        // A variant whose nested model hasn't been built yet — null is permitted
        var variant = new DispatchVariantModel("FORWARD", "Pricing_Forward", "pricing_Forward", null);
        assertEquals("FORWARD", variant.getEnumValue());
        assertNull(variant.getVariantModel());
    }

    // =========================================================================
    // Full model construction smoke test
    // =========================================================================

    @Test
    void full_model_smoke_test() {
        var input1 = new ParamModel("trade", "Trade", "com.example.Trade", false, false, true);
        var input2 = new ParamModel("rates", "Rate", "com.example.Rate", true, false, true);
        var output = new ParamModel("result", "Money", "com.example.Money", false, false, true);
        var alias = new AliasModel("notional", "BigDecimal",
                List.of("Trade trade"), "trade.getNotional()", false);
        var op = new OperationModel("set", "result.setAmount(notional(trade))", List.of(), false);
        var pre = new ConditionModel("TradeExists", "trade must exist", "trade != null");
        var post = new ConditionModel("PositiveResult", "result must be positive",
                "result.getAmount().compareTo(BigDecimal.ZERO) > 0");
        var dep = new DependencyModel("fxConvert", "FxConvert",
                "com.example.functions.FxConvert", true);
        var variantModel = minimalModel();
        var variant = new DispatchVariantModel("USD", "CalcPrice_Usd", "calcPrice_Usd", variantModel);

        var model = new FunctionTemplateModel(
                "com.example.functions",
                "CalcPrice",
                List.of("java.math.BigDecimal", "com.example.Trade", "com.example.Money",
                        "com.example.Rate", "com.example.functions.FxConvert"),
                List.of(),
                false, true, false, null, "CalcPriceLabelProvider", "3.0", true,
                List.of(input1, input2), output, false, true,
                List.of(alias), List.of(op), List.of(pre), List.of(post),
                true, true, List.of(dep),
                "AbstractCalcPrice", "com.example.functions.AbstractCalcPrice",
                "Calculates the price of a trade.", List.of("@param trade the trade"),
                List.of(variant), "pricingType", "PricingTypeEnum");

        // Spot-check every field category
        assertEquals("com.example.functions", model.getPackageName());
        assertEquals("CalcPrice", model.getClassName());
        assertEquals(5, model.getImports().size());
        assertFalse(model.getIsQualify());
        assertTrue(model.getIsDispatch());
        assertFalse(model.getOverridesEvaluate());
        assertNull(model.getQualifyGenericType());
        assertEquals("CalcPriceLabelProvider", model.getLabelProviderClassName());
        assertEquals("3.0", model.getVersion());
        assertTrue(model.getHasDeepOperations());
        assertEquals(2, model.getInputs().size());
        assertSame(output, model.getOutput());
        assertFalse(model.getOutputIsMulti());
        assertTrue(model.getOutputNeedsBuilder());
        assertEquals(1, model.getAliases().size());
        assertEquals(1, model.getOperations().size());
        assertEquals(1, model.getPreConditions().size());
        assertEquals(1, model.getPostConditions().size());
        assertTrue(model.getHasConditions());
        assertTrue(model.getHasObjectValidator());
        assertEquals(1, model.getDependencies().size());
        assertEquals("AbstractCalcPrice", model.getSuperClassName());
        assertEquals("com.example.functions.AbstractCalcPrice", model.getSuperClassFqn());
        assertEquals("Calculates the price of a trade.", model.getDefinition());
        assertEquals(1, model.getJavadocParams().size());
        assertEquals(1, model.getDispatchVariants().size());
        assertEquals("pricingType", model.getDispatchParamName());
        assertEquals("PricingTypeEnum", model.getDispatchEnumType());
    }
}
