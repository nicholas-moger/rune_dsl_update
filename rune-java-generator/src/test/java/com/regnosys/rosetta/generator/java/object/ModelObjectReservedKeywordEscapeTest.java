package com.regnosys.rosetta.generator.java.object;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PR #231 anchor — reserved-keyword escaping in the POJO generator.
 *
 * <p>When a Rosetta attribute is literally named with a Java reserved word
 * ({@code new}, {@code short}, {@code long}, {@code return}, …) the generated
 * POJO must ESCAPE it everywhere the name is used as a Java IDENTIFIER, mirroring
 * upstream's scope-driven escape (AbstractJavaScope.isValidIdentifier →
 * {@code SourceVersion.isName}; JavaClassScope.escapeName → {@code "_" + name}):
 * <ul>
 *   <li>FIELD identifier (and every {@code this.X} / {@code return X} /
 *       equals/hashCode/toString / getOrCreate / prune reference) → {@code _new}
 *       (single underscore).</li>
 *   <li>builder-INTERFACE setter param → {@code _new} (single underscore — no
 *       field in the interface scope).</li>
 *   <li>builder-IMPL setter param → {@code __new} (double underscore — the
 *       escaped {@code _new} field is already taken in the impl scope, so the
 *       same single-{@code _} escape applies twice).</li>
 *   <li>STRING literals ({@code @RosettaAttribute}, {@code @RuneAttribute},
 *       {@code path.newSubPath}, toString {@code "new="} label) and accessor
 *       METHOD names ({@code getNew}/{@code setNew}) stay RAW — never escaped.</li>
 * </ul>
 *
 * <p>The fork previously emitted a BARE keyword field ({@code private final … new;}
 * — a syntax error) while escaping only the impl setter param to single-{@code _new}.
 * These anchors are RED on revert. Empirical carriers: 8 iso {@code TradeReport*Choice}
 * ({@code new}), fpml {@code CreditLimitUtilizationPosition} ({@code short}/{@code long}),
 * fpml {@code ReturnLeg} ({@code return}).
 */
class ModelObjectReservedKeywordEscapeTest {

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);

    @Test void model_object_keyword_field_and_accessors_escaped() {
        // Mirrors the iso TradeReport*Choice carriers: a model-object attribute
        // literally named `new`.
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var tradeData = makeDataType(model, "TradeData");
        addAttribute(tradeData, "x", makeBasicType("string"), 0, 1);
        var choice = makeDataType(model, "TradeReportChoice");
        addAttribute(choice, "new", tradeData, 0, 1);

        String g = generate(model, choice);

        // FIELD declarations — impl + builderImpl — escaped to _new (never bare `new`).
        assertTrue(g.contains("private final TradeData _new;"),
                "impl field must be `_new`; got:\n" + g);
        assertTrue(g.contains("protected TradeData.TradeDataBuilder _new;"),
                "builderImpl field must be `_new`; got:\n" + g);
        assertFalse(g.contains("private final TradeData new;"),
                "impl field must NOT be the bare keyword `new`");
        assertFalse(g.contains("protected TradeData.TradeDataBuilder new;"),
                "builderImpl field must NOT be the bare keyword `new`");

        // Accessor METHOD names stay raw (getNew/setNew are valid identifiers).
        assertTrue(g.contains("TradeData getNew();"), "interface getter is getNew()");
        // Getter body returns the escaped field.
        assertTrue(g.contains("return _new;"), "getter body returns `_new`");

        // builder-INTERFACE setter param = single underscore `_new`.
        assertTrue(g.contains("setNew(TradeData _new);"),
                "interface setter param must be `_new`; got:\n" + g);
        // builder-IMPL setter param = DOUBLE underscore `__new`, body uses _new field.
        assertTrue(g.contains("setNew(TradeData __new) {"),
                "impl setter param must be `__new`; got:\n" + g);
        assertTrue(g.contains("this._new = __new == null ? null : __new.toBuilder();"),
                "impl setter body must assign `this._new = __new...`; got:\n" + g);

        // getOrCreate body references the escaped field.
        assertTrue(g.contains("getOrCreateNew()"), "getOrCreate method name is raw");
        assertTrue(g.contains("if (_new!=null)") || g.contains("if (_new != null)"),
                "getOrCreate body references `_new` field");

        // STRING literals stay RAW.
        assertTrue(g.contains("@RosettaAttribute(\"new\")"), "@RosettaAttribute stays raw \"new\"");
        assertTrue(g.contains("@RuneAttribute(\"new\")"), "@RuneAttribute stays raw \"new\"");
        assertTrue(g.contains("path.newSubPath(\"new\")"), "path string stays raw \"new\"");
    }

    @Test void basic_keyword_fields_escaped_primitive_and_statement_keywords() {
        // fpml CreditLimitUtilizationPosition (short/long) + ReturnLeg (return):
        // basic-typed attributes whose names are primitive / statement keywords.
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Holder");
        addAttribute(dt, "short", makeBasicType("string"), 0, 1);
        addAttribute(dt, "long", makeBasicType("string"), 0, 1);
        addAttribute(dt, "return", makeBasicType("string"), 0, 1);
        // `volatile` — the census-missed keyword the byte-oracle surfaced in the
        // fpml CoalStandardQuality carrier; locks full-reserved-set coverage.
        addAttribute(dt, "volatile", makeBasicType("string"), 0, 1);

        String g = generate(model, dt);

        // Fields escaped.
        assertTrue(g.contains("private final String _short;"), "field `_short`; got:\n" + g);
        assertTrue(g.contains("private final String _long;"), "field `_long`");
        assertTrue(g.contains("private final String _return;"), "field `_return`");
        assertTrue(g.contains("private final String _volatile;"), "field `_volatile`");
        assertFalse(g.contains("private final String short;"), "no bare `short` field");
        assertFalse(g.contains("private final String long;"), "no bare `long` field");
        assertFalse(g.contains("private final String return;"), "no bare `return` field");
        assertFalse(g.contains("private final String volatile;"), "no bare `volatile` field");

        // Impl setter: interface param `_short`, impl param `__short`, basic body.
        assertTrue(g.contains("setShort(String _short);"), "interface param `_short`");
        assertTrue(g.contains("setShort(String __short) {"), "impl param `__short`");
        assertTrue(g.contains("this._short = __short == null ? null : __short;"),
                "basic impl setter body `this._short = __short...`; got:\n" + g);

        // toString: label RAW, field escaped.
        assertTrue(g.contains("\"short=\" + this._short"),
                "toString label raw `short=`, field `_short`; got:\n" + g);
        // equals + hashCode field refs escaped — independently lock the two
        // ModelObjectBoilerplate seats (RED on revert: `Objects.equals(short, ...)`
        // / `(short != null ?` are syntax errors). toString alone under-locked them.
        assertTrue(g.contains("Objects.equals(_short, _that.getShort())"),
                "equals field ref `_short`; got:\n" + g);
        assertTrue(g.contains("(_short != null ? _short.hashCode()"),
                "hashCode field ref `_short`");
        // Impl constructor field-assignment cascade (fieldName SoT).
        assertTrue(g.contains("this._short = builder.getShort();"),
                "impl ctor `this._short = builder.getShort();`");
        // Annotation stays raw for the statement keyword too.
        assertTrue(g.contains("@RosettaAttribute(\"return\")"), "@RosettaAttribute raw \"return\"");
    }

    @Test void non_keyword_attribute_is_not_escaped() {
        // Green-safety: an ordinary attribute name must be byte-unchanged
        // (escape is a no-op for valid identifiers — no over-escaping).
        var model = makeModel("com.example");
        model.setVersion("1.0");
        var dt = makeDataType(model, "Trade");
        addAttribute(dt, "name", makeBasicType("string"), 0, 1);

        String g = generate(model, dt);

        assertTrue(g.contains("private final String name;"), "field stays `name`");
        assertFalse(g.contains("private final String _name;"), "field must NOT be over-escaped");
        // Impl setter param stays the conventional single `_name`.
        assertTrue(g.contains("setName(String _name) {"), "impl param stays `_name`");
        // Interface setter param stays bare `name` (no name-vs-type collision).
        assertTrue(g.contains("setName(String name);"), "interface param stays bare `name`");
    }

    // =========================================================================
    // Helpers (mirror ModelObjectGeneratorTest)
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

    private void addAttribute(RDataType dt, String name, RNode typeNameSource, int min, int max) {
        var attr = new RAttribute();
        attr.setName(name);
        var tc = new RTypeCall();
        tc.setTypeName(typeNameSource instanceof RBasicType bt ? bt.name()
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
}
