package com.regnosys.rosetta.generator.java.object.validators;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.ValidatorCheckModel;
import com.regnosys.rosetta.generator.java.template.model.ValidatorTemplateModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the three validator generators: Cardinality, TypeFormat, OnlyExists.
 * Validates output format matches upstream D11 golden references.
 */
class ValidatorGeneratorTest {

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);

    // =========================================================================
    // CardinalityValidatorGenerator
    // =========================================================================

    @Test void cardinality_single_attribute_optional() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "price", makeRecordType(model, "date"), 0, 1);

        String generated = generateCardinality(model, dt);

        // Package
        assertTrue(generated.startsWith("package com.example.validation;\n"),
                "Wrong package: " + generated.substring(0, Math.min(50, generated.length())));
        // Class declaration — no Javadoc (upstream validators don't emit @version)
        assertTrue(generated.contains(
                "public class TradeValidator implements Validator<Trade>"));
        assertFalse(generated.contains("@version"), "Validators should not have @version Javadoc");
        // Static imports
        assertTrue(generated.contains("import static com.google.common.base.Strings.isNullOrEmpty;"));
        assertTrue(generated.contains("import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;"));
        assertTrue(generated.contains("import static com.rosetta.model.lib.validation.ValidationResult.failure;"));
        assertTrue(generated.contains("import static com.rosetta.model.lib.validation.ValidationResult.success;"));
        assertTrue(generated.contains("import static java.util.stream.Collectors.toList;"));
        // Guava Lists (D11 — generated output uses Guava even though our code doesn't)
        assertTrue(generated.contains("Lists.<ComparisonResult>newArrayList("));
        // Cardinality check with type cast (single: != null ? 1 : 0)
        assertTrue(generated.contains(
                "checkCardinality(\"price\", (Date) o.getPrice() != null ? 1 : 0, 0, 1)"),
                "Missing cardinality check");
        // Validation result mapping
        assertTrue(generated.contains("ValidationType.CARDINALITY"));
        assertTrue(generated.contains("\"Trade\""));
    }

    @Test void cardinality_multi_valued_attribute() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Portfolio");
        addAttribute(dt, "trades", makeDataTypeRef(model, "Trade"), 1, 10);

        String generated = generateCardinality(model, dt);

        // Multi model-object: cast to List<? extends T> (the golden wildcard law —
        // PR #405; basics stay List<T>, see cardinality_unbounded_nonzero_min_included)
        assertTrue(generated.contains(
                "checkCardinality(\"trades\", (List<? extends Trade>) o.getTrades() == null ? 0 : o.getTrades().size(), 1, 10)"),
                "Multi-valued model type should cast to List<? extends T>");
    }

    @Test void cardinality_unbounded_zero_min_skipped() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Container");
        addAttribute(dt, "items", makeBasicType(model, "string"), 0, -1); // (0..*)

        String generated = generateCardinality(model, dt);

        // (0..*) should be skipped
        assertFalse(generated.contains("checkCardinality(\"items\""),
                "(0..*) attribute should be skipped");
    }

    @Test void cardinality_unbounded_nonzero_min_included() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Container");
        addAttribute(dt, "items", makeBasicType(model, "string"), 1, -1); // (1..*)

        String generated = generateCardinality(model, dt);

        // (1..*) should NOT be skipped — only (0..*) is skipped
        assertTrue(generated.contains("checkCardinality(\"items\""),
                "(1..*) attribute should NOT be skipped");
        // Max should be 0 for unbounded (upstream: cardinality.max.orElse(0))
        assertTrue(generated.contains(
                "checkCardinality(\"items\", (List<String>) o.getItems() == null ? 0 : o.getItems().size(), 1, 0)"),
                "Unbounded (1..*) should have max=0");
    }

    @Test void cardinality_inherited_attributes() {
        var model = makeModel("com.example");
        var parent = makeDataType(model, "Parent");
        addAttribute(parent, "id", makeBasicType(model, "string"), 1, 1);

        var child = makeDataType(model, "Child");
        // Use superTypeName so GlobalResolutionPass resolves it via RWorkspace.build()
        child.setSuperTypeName("Parent");
        addAttribute(child, "value", makeBasicType(model, "string"), 0, 1);

        String generated = generateCardinality(model, child);

        // Both inherited and own attributes should be present
        assertTrue(generated.contains("checkCardinality(\"id\""), "Inherited attr missing");
        assertTrue(generated.contains("checkCardinality(\"value\""), "Own attr missing");
        // Parent's attribute should come first
        int idIdx = generated.indexOf("checkCardinality(\"id\"");
        int valueIdx = generated.indexOf("checkCardinality(\"value\"");
        assertTrue(idIdx < valueIdx, "Inherited attr should come before own");
    }

    @Test void cardinality_required_attribute() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Required");
        addAttribute(dt, "name", makeBasicType(model, "string"), 1, 1);

        String generated = generateCardinality(model, dt);

        assertTrue(generated.contains(
                "checkCardinality(\"name\", (String) o.getName() != null ? 1 : 0, 1, 1)"),
                "Required attr cardinality check wrong");
    }

    @Test void cardinality_no_attributes() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Empty");

        String generated = generateCardinality(model, dt);

        // Should produce empty list
        assertTrue(generated.contains("Lists.<ComparisonResult>newArrayList("));
        assertTrue(generated.contains("public class EmptyValidator implements Validator<Empty>"));
    }

    // =========================================================================
    // TypeFormatValidatorGenerator
    // =========================================================================

    /**
     * v3.1 C2d family 2 ({@code typeformat-optional-presence}): the {@code java.util.Optional.of} /
     * {@code .empty} static imports are keyed on the typed envelope's PRESENCE
     * ({@code RNumberType.max().isPresent()} …), no longer on the rendered {@code "of("} /
     * {@code "empty()"} prefix of the argument text. The carriers are the single-constraint types —
     * a {@code number} with only {@code max}, a {@code string} with only {@code pattern} (drr
     * {@code CountryCode: string(pattern: "[A-Z]{2,2}")}), a {@code string} with only
     * {@code maxLength} (iso20022 {@code freeForm255}) — where exactly ONE of the two imports would
     * be lost by a presence read that consulted the wrong field. Each witness pins both imports
     * and the argument spelling they serve.
     */
    @Test void type_format_number_with_only_max_imports_of_and_empty() {
        RModel model = AstBuilder.buildFromString(String.join("\n",
                "namespace com.example",
                "version \"0.0.0\"",
                "",
                "type Capped:",
                "    amount number(max: 99) (0..1)"), "capped.rosetta");
        RDataType dt = (RDataType) model.rootElements().get(0);

        String generated = generateTypeFormat(model, dt);

        assertTrue(generated.contains(
                "checkNumber(\"amount\", o.getAmount(), empty(), empty(), empty(), of(new BigDecimal(\"99\")))"),
                generated);
        assertTrue(generated.contains("import static java.util.Optional.of;"), generated);
        assertTrue(generated.contains("import static java.util.Optional.empty;"), generated);
        assertTrue(generated.contains("import java.math.BigDecimal;"), generated);
    }

    @Test void type_format_string_with_only_pattern_imports_of_and_empty() {
        RModel model = AstBuilder.buildFromString(String.join("\n",
                "namespace com.example",
                "version \"0.0.0\"",
                "",
                "type Coded:",
                "    code string(pattern: \"[A-Z]{2,2}\") (0..1)"), "coded.rosetta");
        RDataType dt = (RDataType) model.rootElements().get(0);

        String generated = generateTypeFormat(model, dt);

        assertTrue(generated.contains(
                "checkString(\"code\", o.getCode(), 0, empty(), of(Pattern.compile(\"[A-Z]{2,2}\")))"),
                generated);
        assertTrue(generated.contains("import static java.util.Optional.of;"), generated);
        assertTrue(generated.contains("import static java.util.Optional.empty;"), generated);
        assertTrue(generated.contains("import java.util.regex.Pattern;"), generated);
    }

    @Test void type_format_string_with_only_max_length_imports_of_and_empty() {
        RModel model = AstBuilder.buildFromString(String.join("\n",
                "namespace com.example",
                "version \"0.0.0\"",
                "",
                "type Bounded:",
                "    text string(maxLength: 255) (0..1)"), "bounded.rosetta");
        RDataType dt = (RDataType) model.rootElements().get(0);

        String generated = generateTypeFormat(model, dt);

        assertTrue(generated.contains("checkString(\"text\", o.getText(), 0, of(255), empty())"), generated);
        assertTrue(generated.contains("import static java.util.Optional.of;"), generated);
        assertTrue(generated.contains("import static java.util.Optional.empty;"), generated);
        assertFalse(generated.contains("import java.util.regex.Pattern;"), generated);
    }

    /** Every number constraint present: {@code empty} is NOT imported (nothing is absent). */
    @Test void type_format_number_fully_constrained_imports_of_only() {
        RModel model = AstBuilder.buildFromString(String.join("\n",
                "namespace com.example",
                "version \"0.0.0\"",
                "",
                "type Exact:",
                "    n number(digits: 5, fractionalDigits: 2, min: 1, max: 99) (0..1)"), "exact.rosetta");
        RDataType dt = (RDataType) model.rootElements().get(0);

        String generated = generateTypeFormat(model, dt);

        assertTrue(generated.contains(
                "checkNumber(\"n\", o.getN(), of(5), of(2), of(new BigDecimal(\"1\")), of(new BigDecimal(\"99\")))"),
                generated);
        assertTrue(generated.contains("import static java.util.Optional.of;"), generated);
        assertFalse(generated.contains("import static java.util.Optional.empty;"), generated);
    }

    @Test void type_format_no_constraints() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Simple");
        addAttribute(dt, "name", makeBasicType(model, "string"), 0, 1);

        String generated = generateTypeFormat(model, dt);

        assertTrue(generated.startsWith("package com.example.validation;\n"));
        assertTrue(generated.contains(
                "public class SimpleTypeFormatValidator implements Validator<Simple>"));
        assertFalse(generated.contains("@version"), "Validators should not have @version Javadoc");
        // No constrained types → empty comparison list
        assertTrue(generated.contains("Lists.<ComparisonResult>newArrayList("));
        assertTrue(generated.contains("ValidationType.TYPE_FORMAT"));
        // checkString/checkNumber should NOT be imported when not used
        assertFalse(generated.contains("import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkString;"),
                "checkString should not be imported when not used");
    }

    /**
     * Template-level test: validate the ST4 template renders checkString expressions correctly.
     * End-to-end constrained type testing is deferred until parametric type resolution
     * handles type call arguments (currently all basic types resolve to unconstrained).
     */
    @Test void type_format_template_renders_checkString() {
        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath("templates/java-validator-typeformat.stg");

        var checks = List.of(
                new ValidatorCheckModel("checkString(\"simpleAttr\", o.getSimpleAttr(), 0, of(42), empty())", false),
                new ValidatorCheckModel("checkString(\"patternAttr\", o.getPatternAttr(), 0, empty(), of(Pattern.compile(\"[A-Z]+\")))", true)
        );
        var model = new ValidatorTemplateModel(
                "test.pojo.validation", "PojoTypeFormatValidator",
                "Pojo", "Pojo", "test.pojo.Pojo",
                List.of("com.google.common.collect.Lists",
                        "com.rosetta.model.lib.expression.ComparisonResult",
                        "com.rosetta.model.lib.path.RosettaPath",
                        "com.rosetta.model.lib.validation.ValidationResult",
                        "com.rosetta.model.lib.validation.Validator",
                        "java.util.List",
                        "java.util.regex.Pattern",
                        "test.pojo.Pojo"),
                List.of("com.google.common.base.Strings.isNullOrEmpty",
                        "com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkString",
                        "com.rosetta.model.lib.validation.ValidationResult.failure",
                        "com.rosetta.model.lib.validation.ValidationResult.success",
                        "java.util.Optional.empty",
                        "java.util.Optional.of",
                        "java.util.stream.Collectors.toList"),
                checks);

        // v3.2 seat 11 (D50): the two steps generate() renders - the class text with its sentinels through the
        // generator's token map, resolved from the validator class as the one seed, then the file wrapper.
        String classText = renderer.render("templates/java-validator-typeformat.stg",
                "typeFormatValidatorBody", "m", model, "t", TypeFormatValidatorGenerator.TYPE_FORMAT_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getValidatorClassName(), model.getImports());
        String rendered = renderer.render("templates/java-validator-typeformat.stg", "typeFormatValidator", "m",
                new PojoTemplateModel(model.getPackageName(), resolved.imports(), model.getStaticImports(),
                        resolved.classText()));

        assertTrue(rendered.contains("public class PojoTypeFormatValidator implements Validator<Pojo>"));
        assertTrue(rendered.contains("checkString(\"simpleAttr\", o.getSimpleAttr(), 0, of(42), empty()), "));
        assertTrue(rendered.contains("checkString(\"patternAttr\", o.getPatternAttr(), 0, empty(), of(Pattern.compile(\"[A-Z]+\")))"));
        assertTrue(rendered.contains("ValidationType.TYPE_FORMAT"));
        assertTrue(rendered.contains("import static java.util.Optional.of;"));
        assertTrue(rendered.contains("import static java.util.Optional.empty;"));
        assertTrue(rendered.contains("import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkString;"));
        assertTrue(rendered.contains("import java.util.regex.Pattern;"));
        // Trailing comma only on non-last check
        assertFalse(rendered.contains("of(Pattern.compile(\"[A-Z]+\"))), "),
                "Last check should not have trailing comma");
    }

    /**
     * Template-level test: validate the ST4 template renders checkNumber expressions correctly.
     */
    @Test void type_format_template_renders_checkNumber() {
        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath("templates/java-validator-typeformat.stg");

        var checks = List.of(
                new ValidatorCheckModel("checkNumber(\"periodMultiplier\", o.getPeriodMultiplier(), empty(), of(0), empty(), empty())", true)
        );
        var model = new ValidatorTemplateModel(
                "cdm.base.datetime.validation", "AdjustedRelativeDateOffsetTypeFormatValidator",
                "AdjustedRelativeDateOffset", "AdjustedRelativeDateOffset",
                "cdm.base.datetime.AdjustedRelativeDateOffset",
                List.of("cdm.base.datetime.AdjustedRelativeDateOffset",
                        "com.google.common.collect.Lists",
                        "com.rosetta.model.lib.expression.ComparisonResult",
                        "com.rosetta.model.lib.path.RosettaPath",
                        "com.rosetta.model.lib.validation.ValidationResult",
                        "com.rosetta.model.lib.validation.Validator",
                        "java.util.List"),
                List.of("com.google.common.base.Strings.isNullOrEmpty",
                        "com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber",
                        "com.rosetta.model.lib.validation.ValidationResult.failure",
                        "com.rosetta.model.lib.validation.ValidationResult.success",
                        "java.util.Optional.empty",
                        "java.util.Optional.of",
                        "java.util.stream.Collectors.toList"),
                checks);

        // v3.2 seat 11 (D50): the two steps generate() renders - the class text with its sentinels through the
        // generator's token map, resolved from the validator class as the one seed, then the file wrapper.
        String classText = renderer.render("templates/java-validator-typeformat.stg",
                "typeFormatValidatorBody", "m", model, "t", TypeFormatValidatorGenerator.TYPE_FORMAT_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getValidatorClassName(), model.getImports());
        String rendered = renderer.render("templates/java-validator-typeformat.stg", "typeFormatValidator", "m",
                new PojoTemplateModel(model.getPackageName(), resolved.imports(), model.getStaticImports(),
                        resolved.classText()));

        assertTrue(rendered.contains("public class AdjustedRelativeDateOffsetTypeFormatValidator implements Validator<AdjustedRelativeDateOffset>"));
        assertTrue(rendered.contains("checkNumber(\"periodMultiplier\", o.getPeriodMultiplier(), empty(), of(0), empty(), empty())"));
        assertTrue(rendered.contains("ValidationType.TYPE_FORMAT"));
        assertTrue(rendered.contains("import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;"));
    }

    // =========================================================================
    // OnlyExistsValidatorGenerator
    // =========================================================================

    @Test void only_exists_basic() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "price", makeBasicType(model, "string"), 0, 1);
        addAttribute(dt, "quantity", makeBasicType(model, "int"), 0, 1);

        String generated = generateOnlyExists(model, dt);

        // Package in exists subdirectory
        assertTrue(generated.startsWith("package com.example.validation.exists;\n"));
        // No Javadoc
        assertFalse(generated.contains("@version"), "Validators should not have @version Javadoc");
        // Class declaration with Set<String> arg
        assertTrue(generated.contains(
                "public class TradeOnlyExistsValidator implements ValidatorWithArg<Trade, Set<String>>"));
        // Method signature with T2 extends
        assertTrue(generated.contains(
                "public <T2 extends Trade> ValidationResult<Trade> validate(RosettaPath path, T2 o, Set<String> fields)"));
        // ImmutableMap builder
        assertTrue(generated.contains("ImmutableMap.<String, Boolean>builder()"));
        // Field existence checks with type cast
        assertTrue(generated.contains(".put(\"price\", ExistenceChecker.isSet((String) o.getPrice()))"));
        assertTrue(generated.contains(".put(\"quantity\", ExistenceChecker.isSet((Integer) o.getQuantity()))"));
        // Build call
        assertTrue(generated.contains(".build();"));
        // Success/failure
        assertTrue(generated.contains("ValidationType.ONLY_EXISTS"));
        // Comment preserved from upstream
        assertTrue(generated.contains("/* Casting is required to ensure types are output"));
    }

    @Test void only_exists_inherited_attributes() {
        var model = makeModel("com.example");
        var parent = makeDataType(model, "Parent");
        addAttribute(parent, "id", makeBasicType(model, "string"), 1, 1);

        var child = makeDataType(model, "Child");
        // Use superTypeName so GlobalResolutionPass resolves it via RWorkspace.build()
        child.setSuperTypeName("Parent");
        addAttribute(child, "value", makeBasicType(model, "string"), 0, 1);

        String generated = generateOnlyExists(model, child);

        // Both inherited and own attributes
        assertTrue(generated.contains(".put(\"id\""), "Inherited attr missing");
        assertTrue(generated.contains(".put(\"value\""), "Own attr missing");
    }

    @Test void only_exists_format_string() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "price", makeBasicType(model, "string"), 0, 1);

        String generated = generateOnlyExists(model, dt);

        assertTrue(generated.contains(
                "String.format(\"[%s] should only be set.  Set fields: %s\", fields, setFields)"),
                "Format string should have double space after 'set.'");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String generateCardinality(RModel model, RDataType type) {
        var result = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(result.workspace());
        var gen = new CardinalityValidatorGenerator(gm, typeTranslator, typeUtil);
        var validatorClass = gen.createTypeRepresentation(type);
        return gen.generate(type, validatorClass, gm.version(model));
    }

    private String generateTypeFormat(RModel model, RDataType type) {
        var result = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(result.workspace());
        var gen = new TypeFormatValidatorGenerator(gm, typeTranslator, typeUtil);
        var validatorClass = gen.createTypeRepresentation(type);
        return gen.generate(type, validatorClass, gm.version(model));
    }

    private String generateOnlyExists(RModel model, RDataType type) {
        var result = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(result.workspace());
        var gen = new OnlyExistsValidatorGenerator(gm, typeTranslator, typeUtil);
        var validatorClass = gen.createTypeRepresentation(type);
        return gen.generate(type, validatorClass, gm.version(model));
    }

    private RModel makeModel(String namespace) {
        var model = new RModel();
        model.setNamespace(namespace);
        return model;
    }

    private RDataType makeDataType(RModel model, String name) {
        var dt = new RDataType();
        dt.setName(name);
        model.rootElements().add(dt);
        dt.setParent(model);
        return dt;
    }

    private void addAttribute(RDataType dt, String name,
                              RNode typeNameSource,
                              int min, int max) {
        var attr = new RAttribute();
        attr.setName(name);
        var tc = new RTypeCall();
        // typeNameSource is used solely to derive the textual typeName.
        // referencedTypeId is NOT wired here — these tests rely on the
        // generator's name-based fallback path. If a test needs
        // deterministic resolution it should bind via
        // GeneratorTestSymbolResolver and call setReferencedTypeId explicitly.
        tc.setTypeName(typeNameSource instanceof RBasicType bt ? bt.name()
                : typeNameSource instanceof RRecordType rt ? rt.name()
                : typeNameSource instanceof RDataType rdt ? rdt.name()
                : "unknown");
        attr.setTypeCall(tc);
        var card = new RCardinality();
        card.setInf(min);
        if (max < 0) {
            card.setUnbounded(true);
        } else {
            card.setSup(max);
        }
        attr.setCardinality(card);
        dt.attributes().add(attr);
    }

    private RBasicType makeBasicType(RModel model, String name) {
        var bt = new RBasicType();
        bt.setName(name);
        return bt;
    }

    private RRecordType makeRecordType(RModel model, String name) {
        var rt = new RRecordType();
        rt.setName(name);
        return rt;
    }

    private RDataType makeDataTypeRef(RModel model, String name) {
        for (var elem : model.rootElements()) {
            if (elem instanceof RDataType dt && dt.name().equals(name)) {
                return dt;
            }
        }
        return makeDataType(model, name);
    }
}
