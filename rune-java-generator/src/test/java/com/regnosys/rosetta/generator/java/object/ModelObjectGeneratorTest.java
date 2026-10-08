package com.regnosys.rosetta.generator.java.object;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelObjectGeneratorTest {

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);

    @Test void generates_interface_and_impl() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "price", makeBasicType("string"), 0, 1);

        String generated = generate(model, dt);

        // Interface
        assertTrue(generated.contains("public interface Trade extends RosettaModelObject"));
        // Impl class
        assertTrue(generated.contains("class TradeImpl implements Trade"));
        // Builder interface
        assertTrue(generated.contains("interface TradeBuilder extends Trade, RosettaModelObjectBuilder"));
        // BuilderImpl class
        assertTrue(generated.contains("class TradeBuilderImpl implements Trade.TradeBuilder"));
    }

    @Test void impl_fields_and_constructor() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "name", makeBasicType("string"), 1, 1);

        String generated = generate(model, dt);

        assertTrue(generated.contains("private final String name;"));
        assertTrue(generated.contains("protected TradeImpl(Trade.TradeBuilder builder)"));
        assertTrue(generated.contains("this.name = builder.getName();"));
    }

    @Test void builder_fields_and_getters() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "name", makeBasicType("string"), 0, 1);

        String generated = generate(model, dt);

        // BuilderImpl field
        assertTrue(generated.contains("protected String name;"));
        // BuilderImpl getter with annotations
        assertTrue(generated.contains("@RosettaAttribute(\"name\")"));
        assertTrue(generated.contains("public String getName()"));
    }

    @Test void builder_setters() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "name", makeBasicType("string"), 0, 1);

        String generated = generate(model, dt);

        assertTrue(generated.contains("public Trade.TradeBuilder setName(String _name)"));
    }

    @Test void build_and_toBuilder() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Trade");

        String generated = generate(model, dt);

        // Impl build()
        assertTrue(generated.contains("public Trade build()"));
        // BuilderImpl build()
        assertTrue(generated.contains("return new Trade.TradeImpl(this);"));
        // toBuilder()
        assertTrue(generated.contains("public Trade.TradeBuilder toBuilder()"));
    }

    @Test void prune_and_hasData() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "name", makeBasicType("string"), 0, 1);

        String generated = generate(model, dt);

        assertTrue(generated.contains("public Trade.TradeBuilder prune()"));
        assertTrue(generated.contains("public boolean hasData()"));
    }

    @Test void merge_method() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "name", makeBasicType("string"), 0, 1);

        String generated = generate(model, dt);

        assertTrue(generated.contains("merge(RosettaModelObjectBuilder other, BuilderMerger merger)"));
        assertTrue(generated.contains("merger.mergeBasic(getName(), o.getName(), this::setName)"));
    }

    @Test void boilerplate_equals_hashCode_toString() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "name", makeBasicType("string"), 0, 1);

        String generated = generate(model, dt);

        // Impl boilerplate
        assertTrue(generated.contains("public boolean equals(Object o)"));
        assertTrue(generated.contains("public int hashCode()"));
        assertTrue(generated.contains("return \"Trade {\""));
        // Builder boilerplate
        assertTrue(generated.contains("return \"TradeBuilder {\""));
    }

    @Test void model_object_property_uses_builder_type() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var refType = makeDataType(model, "Address");
        var dt = makeDataType(model, "Person");
        addAttribute(dt, "address", refType, 0, 1);

        String generated = generate(model, dt);

        // BUG-1 fix: isRosettaModelObject now works for generated types
        // BuilderImpl field should use dot-qualified builder type (BUG-3 fix)
        assertTrue(generated.contains("protected Address.AddressBuilder address;"),
                "Model object field should use dot-qualified builder type");
        // Setter should call toBuilder()
        assertTrue(generated.contains("_address.toBuilder()"),
                "Model object setter should call toBuilder()");
        // getOrCreate should be generated
        assertTrue(generated.contains("getOrCreateAddress()"),
                "Model object should have getOrCreate method");
        // Impl constructor should use ofNullable/build pattern
        assertTrue(generated.contains("ofNullable(builder.getAddress()).map(f->f.build()).orElse(null)"),
                "Impl constructor should build model objects");
    }

    @Test void list_model_property() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var itemType = makeDataType(model, "Item");
        var dt = makeDataType(model, "Container");
        addAttribute(dt, "items", itemType, 0, -1); // (0..*)

        String generated = generate(model, dt);

        // Interface: List<? extends Item>
        assertTrue(generated.contains("List<? extends Item> getItems()"),
                "Interface getter should return List<? extends Item>");
        // Builder interface: add + set methods
        assertTrue(generated.contains("addItems(Item"), "Should have add(single)");
        assertTrue(generated.contains("addItems(List<? extends Item>"), "Should have add(list)");
        assertTrue(generated.contains("setItems(List<? extends Item>"), "Should have set(list)");
        // BuilderImpl field: ArrayList init with dot-qualified type
        assertTrue(generated.contains("protected List<Item.ItemBuilder> items = new ArrayList<>()"),
                "Builder list field should init as ArrayList with dot-qualified type");
        // BuilderImpl getter: List<? extends Item.ItemBuilder>
        assertTrue(generated.contains("List<? extends Item.ItemBuilder> getItems()"),
                "Builder getter should return List<? extends Item.ItemBuilder>");
        // Add method: toBuilder call
        assertTrue(generated.contains("this.items.add(_items.toBuilder())"),
                "Add should call toBuilder()");
        // Prune: stream/filter/prune/collect
        assertTrue(generated.contains("items.stream().filter(b->b!=null)"),
                "Prune should stream/filter list");
    }

    @Test void list_basic_property() {
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Tags");
        addAttribute(dt, "values", makeBasicType("string"), 0, -1);

        String generated = generate(model, dt);

        // Interface: List<String> for basic types (no wildcard — matches upstream D11)
        assertTrue(generated.contains("List<String> getValues()"),
                "Interface getter should return List<String> for basic types");
        // BuilderImpl field
        assertTrue(generated.contains("protected List<String> values = new ArrayList<>()"),
                "Builder list field should init as ArrayList");
    }

    @Test void getOrCreate_param_renamed_to_underscore_index_on_property_name_collision() {
        // P2.1.3c T5b — scope-deduplication rule for getOrCreate*(int index) param.
        // When a property named `index` (any-case-first) exists on the pojo, the
        // upstream-equivalent rule forces the param to `_index` to avoid shadowing
        // the inherited setter / getter. See the {@code hasIndexPropertyInScope}
        // helper in {@code ModelObjectGenerator} for the inline narrative.
        //
        // Empirical seed: CDM 5.35 BasketConstituent (extends Product { index Index
        // (0..1) }) — golden {@code getOrCreateQuantity(int _index)}. The legacy
        // extendsChoice heuristic missed this case because Product is RDataType,
        // not RChoice. The unified inherited-property check catches both this and
        // the CDM 6.15+ case (extends `choice Observable` whose `Index` option
        // becomes an `index` property).
        //
        // The golden retains `int index` on the BUILDER INTERFACE declaration
        // (matches upstream behavior: only the BuilderImpl method body is scope-
        // deduplicated against the field set; the interface signature stays
        // schema-public-stable) and uses `int _index` on the BuilderImpl method
        // body — see BasketConstituent golden interface vs impl emission.
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var itemType = makeDataType(model, "Item");
        var indexType = makeDataType(model, "Idx");
        var container = makeDataType(model, "Container");
        // Non-list property named `index` triggers the conflict.
        addAttribute(container, "index", indexType, 0, 1);
        // List of model objects forces emit of `getOrCreateItems(int _index)`.
        addAttribute(container, "items", itemType, 0, -1);

        String generated = generate(model, container);

        // BuilderImpl method body: must use `_index` (not `index`) — mirrors the
        // BasketConstituent golden getOrCreate* method-body emissions.
        assertTrue(generated.contains("getOrCreateItems(int _index)"),
                "BuilderImpl param should be `_index` when an `index` property exists in scope; got:\n"
                        + generated);
        // Body should reference the renamed param.
        assertTrue(generated.contains("getIndex(items, _index, () ->"),
                "getIndex body should reference renamed `_index` param");
    }

    @Test void getOrCreate_param_stays_index_when_no_property_name_collision() {
        // Companion test to the collision case above — when no `index` property
        // exists in scope, the param uses the default `index` name. Validates the
        // empirical BusinessEvent case (extends EventInstruction with no `index`
        // field) where the golden interface declaration and getOrCreate* impl
        // method body both use `int index`.
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var itemType = makeDataType(model, "Item");
        var container = makeDataType(model, "Bag");
        addAttribute(container, "items", itemType, 0, -1);

        String generated = generate(model, container);

        assertTrue(generated.contains("getOrCreateItems(int index)"),
                "Param should be plain `index` when no scope conflict; got:\n" + generated);
        assertFalse(generated.contains("getOrCreateItems(int _index)"),
                "Param must NOT be `_index` when no scope conflict");
    }

    @Test void builder_interface_setter_param_underscore_keyed_on_name_type_collision() {
        // PR #107: the builder-INTERFACE setter param gets a `_` prefix iff the
        // property name collides with its value type's simple name (the upstream
        // convention that avoids `setAsset(Asset Asset)` identifier shadowing), NOT
        // whenever the name starts uppercase. This locks both directions in one POJO:
        //   * attr `LeafA` of type `LeafA` (name == type)  → `setLeafA(LeafA _LeafA)`
        //     — the collision case (mirrors a choice option, or a `type` that extends
        //     a choice such as cdm `BasketConstituent extends Observable`).
        //   * attr `SEAF` of type `LeafA` (uppercase, name != type) → `setSEAF(LeafA SEAF)`
        //     — the ISO-UPI `AnnaDsb*` data-attribute case the prior
        //     `Character.isUpperCase(name)` heuristic mis-prefixed as `_SEAF`.
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var leafA = makeDataType(model, "LeafA");
        addAttribute(leafA, "x", makeBasicType("string"), 0, 1);
        var holder = makeDataType(model, "Holder");
        addAttribute(holder, "LeafA", leafA, 0, 1);
        addAttribute(holder, "SEAF", leafA, 0, 1);

        String generated = generate(model, holder);

        assertTrue(generated.contains("setLeafA(LeafA _LeafA);"),
                "name == type must collide → `_LeafA` interface param; got:\n" + generated);
        assertTrue(generated.contains("setSEAF(LeafA SEAF);"),
                "uppercase name != type must stay bare → `SEAF` interface param; got:\n" + generated);
        assertFalse(generated.contains("setSEAF(LeafA _SEAF);"),
                "non-colliding uppercase attr must NOT get the legacy `_SEAF` prefix");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String generate(RModel model, RDataType type) {
        var result = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(result.workspace());
        var gen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var pojo = gen.createTypeRepresentation(type);
        return gen.generate(type, pojo, gm.version(model));
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
