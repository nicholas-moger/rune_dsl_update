package com.regnosys.rosetta.generator.java.object;

import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.types.JavaPojoInterface;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;
import com.regnosys.rosetta.generator.java.types.JavaPojoPropertyOperationType;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaParameterizedType;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelObjectBoilerplateTest {

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final ModelObjectBoilerplate boilerplate = new ModelObjectBoilerplate(typeUtil);

    /**
     * v3.2 seat 11 (D50 - the file-scope first-claim law): the boilerplate writes every library type as a first-claim
     * SENTINEL now (the POJO generator resolves the WHOLE class text once, from the own class as the seed, and drops the
     * losers' imports); this unit test reads the boilerplate ALONE, so it neutralises the sentinels to their bare simple
     * names - the pre-seat form - before asserting (a render with no sentinel is returned unchanged). The whole-suite law's
     * catch at the seat's c8 verify gate: no targeted driver had run this class.
     */
    private static String bare(String rendered) {
        return ImportCollisionResolver.stripToBare(rendered);
    }

    // =========================================================================
    // equals
    // =========================================================================

    @Test void equals_basic() {
        var pojo = makePojo("Trade", List.of(
                makeProp("price", typeUtil.STRING),
                makeProp("quantity", typeUtil.INTEGER)
        ));

        String raw = boilerplate.boilerPlate(pojo, false, pojo.getAllProperties());
        assertTrue(ImportCollisionResolver.hasSentinel(raw));   // round 1's cq NIT-5: the boilerplate DOES write sentinels now (commit 7's reason)
        String result = bare(raw);

        assertTrue(result.contains("if (this == o) return true;"));
        assertTrue(result.contains("if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;"));
        assertTrue(result.contains("Trade _that = getType().cast(o);"));
        assertTrue(result.contains("if (!Objects.equals(price, _that.getPrice())) return false;"));
        assertTrue(result.contains("if (!Objects.equals(quantity, _that.getQuantity())) return false;"));
        assertTrue(result.contains("return true;"));
    }

    @Test void equals_extended() {
        var pojo = makePojo("Child", List.of(
                makeProp("value", typeUtil.STRING)
        ));

        String result = bare(boilerplate.boilerPlate(pojo, true, pojo.getAllProperties()));

        assertTrue(result.contains("if (!super.equals(o)) return false;"));
    }

    @Test void equals_no_properties() {
        var pojo = makePojo("Empty", List.of());

        String result = bare(boilerplate.boilerPlate(pojo, false, pojo.getAllProperties()));

        // No _that assignment when no properties
        assertFalse(result.contains("_that"));
        assertTrue(result.contains("return true;"));
    }

    @Test void equals_list_property_uses_ListEquals() {
        var pojo = makePojo("Container", List.of(
                makeProp("items", JavaParameterizedType.from(typeUtil.LIST, typeUtil.STRING))
        ));

        String result = bare(boilerplate.boilerPlate(pojo, false, pojo.getAllProperties()));

        assertTrue(result.contains("if (!ListEquals.listEquals(items, _that.getItems())) return false;"),
                "List properties should use ListEquals");
    }

    // =========================================================================
    // hashCode
    // =========================================================================

    @Test void hashCode_basic() {
        var pojo = makePojo("Trade", List.of(
                makeProp("price", typeUtil.STRING),
                makeProp("quantity", typeUtil.INTEGER)
        ));

        String result = bare(boilerplate.boilerPlate(pojo, false, pojo.getAllProperties()));

        assertTrue(result.contains("int _result = 0;"));
        assertTrue(result.contains("_result = 31 * _result + (price != null ? price.hashCode() : 0);"));
        assertTrue(result.contains("_result = 31 * _result + (quantity != null ? quantity.hashCode() : 0);"));
        assertTrue(result.contains("return _result;"));
    }

    @Test void hashCode_extended() {
        var pojo = makePojo("Child", List.of(
                makeProp("value", typeUtil.STRING)
        ));

        String result = bare(boilerplate.boilerPlate(pojo, true, pojo.getAllProperties()));

        assertTrue(result.contains("int _result = super.hashCode();"));
    }

    // =========================================================================
    // toString
    // =========================================================================

    @Test void toString_basic() {
        var pojo = makePojo("Trade", List.of(
                makeProp("price", typeUtil.STRING),
                makeProp("quantity", typeUtil.INTEGER)
        ));

        String result = bare(boilerplate.boilerPlate(pojo, false, pojo.getAllProperties()));

        assertTrue(result.contains("return \"Trade {\" +"));
        assertTrue(result.contains("\"price=\" + this.price + \", \" +"));
        assertTrue(result.contains("\"quantity=\" + this.quantity +"));
        assertTrue(result.contains("'}'"));
    }

    @Test void toString_builder_uses_builder_name() {
        var pojo = makePojo("Trade", List.of(
                makeProp("price", typeUtil.STRING)
        ));

        String result = bare(boilerplate.builderBoilerPlate(pojo, false, pojo.getAllProperties()));

        assertTrue(result.contains("return \"TradeBuilder {\" +"));
    }

    @Test void toString_extended() {
        var pojo = makePojo("Child", List.of(
                makeProp("value", typeUtil.STRING)
        ));

        String result = bare(boilerplate.boilerPlate(pojo, true, pojo.getAllProperties()));

        assertTrue(result.contains("+ \" \" + super.toString()"));
    }

    // =========================================================================
    // process
    // =========================================================================

    @Test void process_basic_property() {
        var pojo = makePojo("Trade", List.of(
                makeProp("price", typeUtil.STRING)
        ));

        String result = bare(boilerplate.processMethod(pojo));

        assertTrue(result.contains("default void process(RosettaPath path, Processor processor)"));
        assertTrue(result.contains(
                "processor.processBasic(path.newSubPath(\"price\"), String.class, getPrice(), this)"),
                "Basic types use processBasic");
    }

    @Test void process_rosetta_model_object_property() {
        // Use ROSETTA_MODEL_OBJECT type to test processRosetta branch
        var pojo = makePojo("Container", List.of(
                makeProp("child", typeUtil.ROSETTA_MODEL_OBJECT)
        ));

        String result = bare(boilerplate.processMethod(pojo));

        assertTrue(result.contains("processRosetta(path.newSubPath(\"child\")"),
                "Model objects use processRosetta");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private TestJavaPojoInterface makePojo(String name, List<JavaPojoProperty> properties) {
        return new TestJavaPojoInterface(name, properties, typeUtil);
    }

    private JavaPojoProperty makeProp(String name, com.rosetta.util.types.JavaType type) {
        return new JavaPojoProperty(
                null, // pojo — not needed for boilerplate tests
                name, name, name, name, name,
                type, null, null, false, List.of(), false);
    }

    /**
     * Concrete test implementation of JavaPojoInterface.
     */
    static class TestJavaPojoInterface extends JavaPojoInterface {
        private final List<JavaPojoProperty> properties;

        TestJavaPojoInterface(String name, List<JavaPojoProperty> properties,
                              JavaTypeUtil typeUtil) {
            super(JavaPackageName.escape(DottedPath.splitOnDots("com.example")),
                    name, typeUtil);
            this.properties = properties;
        }

        @Override public String getJavadoc() { return null; }
        @Override public String getRosettaName() { return getSimpleName(); }
        @Override public String getVersion() { return null; }
        @Override public Collection<JavaPojoProperty> getOwnProperties() { return properties; }
        @Override public Collection<JavaPojoProperty> getAllProperties() { return properties; }
        @Override public JavaPojoInterface getSuperPojo() { return null; }
        @Override public List<JavaClass<?>> getInterfaces() { return Collections.emptyList(); }
        @Override public List<com.rosetta.util.types.JavaGenericTypeDeclaration<?>> getInterfaceDeclarations() { return Collections.emptyList(); }
    }
}
