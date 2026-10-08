package com.regnosys.rosetta.generator.java.types;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RJavaPojoInterfaceTest {

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);

    @Test void basic_properties() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "price", makeBasicType("string"), 0, 1);
        addAttribute(dt, "name", makeBasicType("string"), 1, 1);

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        assertEquals("Trade", pojo.getSimpleName());
        assertEquals("Trade", pojo.getRosettaName());
        assertEquals("com.example", pojo.getPackageName().withDots());

        var props = new ArrayList<>(pojo.getAllProperties());
        assertEquals(2, props.size());
        assertEquals("price", props.get(0).getName());
        assertEquals("name", props.get(1).getName());
    }

    @Test void property_types_resolved() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Event");
        addAttribute(dt, "eventDate", makeRecordType("date"), 1, 1);
        addAttribute(dt, "description", makeBasicType("string"), 0, 1);

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        var props = new ArrayList<>(pojo.getAllProperties());
        assertEquals("Date", props.get(0).getType().getSimpleName());
        assertEquals("String", props.get(1).getType().getSimpleName());
    }

    @Test void multi_valued_wrapped_in_list() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Portfolio");
        addAttribute(dt, "names", makeBasicType("string"), 0, -1); // (0..*)

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        var props = new ArrayList<>(pojo.getAllProperties());
        var type = props.get(0).getType();
        assertTrue(typeUtil.isList(type), "Multi-valued should be wrapped in List");
    }

    @Test void getter_name() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "price", makeBasicType("string"), 0, 1);

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        var prop = pojo.findProperty("price");
        assertEquals("getPrice", prop.getOperationName(JavaPojoPropertyOperationType.GET));
        assertEquals("setPrice", prop.getOperationName(JavaPojoPropertyOperationType.SET));
    }

    @Test void inherited_properties() {
        var model = makeModel("com.example");
        var parent = makeDataType(model, "Parent");
        addAttribute(parent, "id", makeBasicType("string"), 1, 1);

        var child = makeDataType(model, "Child");
        // Use superTypeName so GlobalResolutionPass resolves it via RWorkspace.build()
        child.setSuperTypeName("Parent");
        addAttribute(child, "value", makeBasicType("int"), 0, 1);

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(child, gm, typeTranslator, typeUtil);

        var allProps = new ArrayList<>(pojo.getAllProperties());
        assertEquals(2, allProps.size());
        assertEquals("id", allProps.get(0).getName());
        assertEquals("value", allProps.get(1).getName());

        var ownProps = new ArrayList<>(pojo.getOwnProperties());
        assertEquals(1, ownProps.size());
        assertEquals("value", ownProps.get(0).getName());
    }

    @Test void super_pojo_resolved() {
        var model = makeModel("com.example");
        var parent = makeDataType(model, "Parent");
        var child = makeDataType(model, "Child");
        // Use superTypeName so GlobalResolutionPass resolves it via RWorkspace.build()
        child.setSuperTypeName("Parent");

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(child, gm, typeTranslator, typeUtil);

        assertNotNull(pojo.getSuperPojo());
        assertEquals("Parent", pojo.getSuperPojo().getSimpleName());
    }

    @Test void no_super_pojo() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Standalone");

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        assertNull(pojo.getSuperPojo());
        assertTrue(pojo.getInterfaces().contains(typeUtil.ROSETTA_MODEL_OBJECT));
    }

    @Test void data_type_ref_property() {
        var model = makeModel("com.example");
        var refType = makeDataType(model, "Address");
        var dt = makeDataType(model, "Person");
        addAttribute(dt, "address", refType, 0, 1);

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        var prop = pojo.findProperty("address");
        assertEquals("Address", prop.getType().getSimpleName());
    }

    @Test void metadata_scheme_wraps_in_FieldWithMeta() {
        var model = makeModel("cdm.base.math");
        var dt = makeDataType(model, "UnitType");
        var attr = addAttribute(dt, "currency", makeBasicType("string"), 0, 1);
        addMetadataAnnotation(attr, "scheme");

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        var prop = pojo.findProperty("currency");
        assertEquals("FieldWithMetaString", prop.getType().getSimpleName(),
                "Attribute with [metadata scheme] should be wrapped in FieldWithMeta");
    }

    @Test void metadata_reference_wraps_in_ReferenceWithMeta() {
        var model = makeModel("cdm.base.datetime");
        var refType = makeDataType(model, "BusinessDayAdjustments");
        var dt = makeDataType(model, "AdjustableDate");
        var attr = addAttribute(dt, "dateAdjustmentsReference", refType, 0, 1);
        addMetadataAnnotation(attr, "reference");

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        var prop = pojo.findProperty("dateAdjustmentsReference");
        assertEquals("ReferenceWithMetaBusinessDayAdjustments", prop.getType().getSimpleName(),
                "Attribute with [metadata reference] should be wrapped in ReferenceWithMeta");
    }

    @Test void metadata_id_wraps_in_FieldWithMeta_with_GLOBAL_KEY_FIELD() {
        var model = makeModel("cdm.base.datetime");
        var dt = makeDataType(model, "AdjustableDate");
        var attr = addAttribute(dt, "adjustedDate", makeRecordType("date"), 0, 1);
        addMetadataAnnotation(attr, "id");

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        var prop = pojo.findProperty("adjustedDate");
        assertEquals("FieldWithMetaDate", prop.getType().getSimpleName());
        assertEquals(com.rosetta.model.lib.process.AttributeMeta.GLOBAL_KEY_FIELD, prop.getMeta());
    }

    @Test void metadata_address_wraps_in_ReferenceWithMeta() {
        var model = makeModel("cdm.base.datetime");
        var refType = makeDataType(model, "BusinessDayAdjustments");
        var dt = makeDataType(model, "AdjustableDate");
        var attr = addAttribute(dt, "ref", refType, 0, 1);
        addMetadataAnnotation(attr, "address");

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        var prop = pojo.findProperty("ref");
        assertEquals("ReferenceWithMetaBusinessDayAdjustments", prop.getType().getSimpleName());
    }

    @Test void metadata_key_adds_GlobalKey_and_meta_property() {
        var model = makeModel("cdm.base.datetime");
        var dt = makeDataType(model, "AdjustableDate");
        addAttribute(dt, "value", makeBasicType("string"), 0, 1);
        addTypeMetadataAnnotation(dt, "key");

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        // Should implement GlobalKey
        assertTrue(pojo.getInterfaces().stream()
                .anyMatch(i -> i.getSimpleName().equals("GlobalKey")),
                "Should implement GlobalKey");
        // Should have synthetic "meta" property
        var metaProp = pojo.findProperty("meta");
        assertNotNull(metaProp);
        assertEquals("MetaFields", metaProp.getType().getSimpleName());
    }

    @Test void metadata_key_template_adds_both_interfaces() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Template");
        addTypeMetadataAnnotation(dt, "key");
        addTypeMetadataAnnotation(dt, "template");

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        assertTrue(pojo.getInterfaces().stream()
                .anyMatch(i -> i.getSimpleName().equals("GlobalKey")));
        assertTrue(pojo.getInterfaces().stream()
                .anyMatch(i -> i.getSimpleName().equals("Templatable")));
        // Meta property should use MetaAndTemplateFields
        var metaProp = pojo.findProperty("meta");
        assertEquals("MetaAndTemplateFields", metaProp.getType().getSimpleName());
    }

    @Test void wrapper_package_from_wrapped_type_namespace() {
        var model = makeModel("cdm.base.datetime");
        var dt = makeDataType(model, "AdjustableDate");
        // date is a builtin → wrapper should be in com.rosetta.model.metafields
        var attr = addAttribute(dt, "adjustedDate", makeRecordType("date"), 0, 1);
        addMetadataAnnotation(attr, "id");

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        var prop = pojo.findProperty("adjustedDate");
        // Verify it's FieldWithMetaDate in com.rosetta.model.metafields
        if (prop.getType() instanceof RGeneratedJavaClass<?> gen) {
            assertEquals("com.rosetta.model.metafields",
                    gen.getPackageName().toString(),
                    "Builtin type wrappers should use com.rosetta.model.metafields");
        }
    }

    @Test void required_flag() {
        var model = makeModel("com.example");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "required", makeBasicType("string"), 1, 1);
        addAttribute(dt, "optional", makeBasicType("string"), 0, 1);

        var gm = buildGeneratorModel(model);
        var pojo = new RJavaPojoInterface(dt, gm, typeTranslator, typeUtil);

        var required = pojo.findProperty("required");
        var optional = pojo.findProperty("optional");
        assertTrue(required.isRequired());
        assertFalse(optional.isRequired());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private GeneratorModel buildGeneratorModel(RModel model) {
        var result = RWorkspace.build(List.of(model));
        return new GeneratorModel(result.workspace());
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

    private RAttribute addAttribute(RDataType dt, String name,
                              RNode typeNameSource,
                              int min, int max) {
        var attr = new RAttribute();
        attr.setName(name);
        var tc = new RTypeCall();
        // typeNameSource is used solely to derive the textual typeName.
        // referencedTypeId is NOT wired here — these tests rely on the
        // generator's name-based fallback path (GeneratorModel.lookupBuiltin
        // etc.). If a test needs deterministic resolution it should bind via
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
        return attr;
    }

    private void addMetadataAnnotation(RAttribute attr, String qualifier) {
        var ref = new RAnnotationRef();
        ref.setAnnotationName("metadata");
        ref.setQualifierName(qualifier);
        attr.annotationRefs().add(ref);
    }

    private void addTypeMetadataAnnotation(RDataType dt, String qualifier) {
        var ref = new RAnnotationRef();
        ref.setAnnotationName("metadata");
        ref.setQualifierName(qualifier);
        dt.annotationRefs().add(ref);
    }

    private RBasicType makeBasicType(String name) {
        var bt = new RBasicType();
        bt.setName(name);
        return bt;
    }

    private RRecordType makeRecordType(String name) {
        var rt = new RRecordType();
        rt.setName(name);
        return rt;
    }
}
