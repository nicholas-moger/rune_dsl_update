package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the upstream-aligned {@link NamingValidator} (the #456
 * naming-surface alignment): populations and message bytes per the released
 * 9.83.0 naming checks. The full-parse witness locks live in
 * {@code NamingAlignmentWaveTest}.
 */
class NamingValidatorTest {

    private final NamingValidator validator = new NamingValidator();

    // === Type names (upstream Data only) ======================================

    @Test void data_type_uppercase_passes() {
        var dt = new RDataType(); dt.setName("Trade");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    @Test void data_type_lowercase_warns_released_bytes() {
        var dt = new RDataType(); dt.setName("trade");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertEquals(1, c.toList().size());
        assertEquals("Type name should start with a capital", c.toList().get(0).message());
        assertEquals(ValidationIssueCode.INVALID_CASE, c.toList().get(0).issueCode());
    }

    @Test void type_alias_lowercase_is_silent() {
        // Upstream RosettaTypeAlias is not a Data — type-alias names are never
        // checked (the 48 former corpus over-fires: the builtins' typeAlias
        // int/productType/eventType/calculation + iso20022's 40).
        var ta = new RTypeAlias(); ta.setName("freeForm255");
        var c = new ValidationCollector();
        validator.validate(ta, c);
        assertTrue(c.isEmpty());
    }

    @Test void enum_type_uppercase_passes() {
        var en = new REnumeration(); en.setName("Color");
        var c = new ValidationCollector();
        validator.validate(en, c);
        assertTrue(c.isEmpty());
    }

    @Test void enum_type_lowercase_warns_released_bytes() {
        var en = new REnumeration(); en.setName("color");
        var c = new ValidationCollector();
        validator.validate(en, c);
        assertEquals(1, c.toList().size());
        assertEquals("Enumeration name should start with a capital", c.toList().get(0).message());
    }

    // === Function names =======================================================

    @Test void function_uppercase_passes() {
        var fn = new RFunction(); fn.setName("Calculate");
        var c = new ValidationCollector();
        validator.validate(fn, c);
        assertTrue(c.isEmpty());
    }

    @Test void function_lowercase_warns_released_bytes() {
        var fn = new RFunction(); fn.setName("calculate");
        var c = new ValidationCollector();
        validator.validate(fn, c);
        assertEquals(1, c.toList().size());
        assertEquals("Function name should start with a capital", c.toList().get(0).message());
    }

    // === Attribute names ======================================================

    @Test void attribute_lowercase_passes() {
        var dt = new RDataType(); dt.setName("Foo");
        var attr = new RAttribute(); attr.setName("price");
        var tc = new RTypeCall(); tc.setTypeName("int");
        attr.setTypeCall(tc);
        dt.attributes().add(attr);
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    @Test void attribute_uppercase_warns_released_bytes() {
        var dt = new RDataType(); dt.setName("Foo");
        var attr = new RAttribute(); attr.setName("Price");
        var tc = new RTypeCall(); tc.setTypeName("int");
        attr.setTypeCall(tc);
        dt.attributes().add(attr);
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertEquals(1, c.toList().size());
        assertEquals("Attribute name should start with a lower case", c.toList().get(0).message());
        assertEquals(ValidationIssueCode.INVALID_CASE, c.toList().get(0).issueCode());
    }

    @Test void function_input_and_output_uppercase_warn() {
        // Function inputs/output are Attributes upstream
        // (RosettaSimple.xcore:162-163) — the widened population.
        var fn = new RFunction(); fn.setName("Calc");
        var in = new RAttribute(); in.setName("Input");
        var out = new RAttribute(); out.setName("Result");
        fn.inputs().add(in);
        fn.setOutput(out);
        var c = new ValidationCollector();
        validator.validate(fn, c);
        assertEquals(2, c.toList().stream()
                .filter(d -> d.message().equals("Attribute name should start with a lower case"))
                .count());
    }

    // === Condition names ======================================================

    @Test void unnamed_non_constraint_condition_warns_invalid_name() {
        var dt = new RDataType(); dt.setName("Foo");
        var cond = new RCondition(); // unnamed, no expression => not a constraint
        dt.conditions().add(cond);
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertEquals(1, c.toList().size());
        assertEquals("Condition name should be specified", c.toList().get(0).message());
        assertEquals(ValidationIssueCode.INVALID_NAME, c.toList().get(0).issueCode());
    }

    @Test void unnamed_constraint_condition_is_silent() {
        // one-of / required|optional choice parse as RCardinalityCheckExpr —
        // upstream's isConstraintCondition exemption (the corpus's 54 unnamed
        // conditions are all constraints).
        var dt = new RDataType(); dt.setName("Foo");
        var cond = new RCondition();
        var oneOf = new RCardinalityCheckExpr();
        oneOf.setOp(CardCheckOp.ONE_OF);
        cond.setExpression(oneOf);
        dt.conditions().add(cond);
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    @Test void lowercase_named_condition_warns_invalid_case() {
        var dt = new RDataType(); dt.setName("Foo");
        var cond = new RCondition(); cond.setName("myCondition");
        dt.conditions().add(cond);
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertEquals(1, c.toList().size());
        assertEquals("Condition name should start with a capital", c.toList().get(0).message());
        assertEquals(ValidationIssueCode.INVALID_CASE, c.toList().get(0).issueCode());
    }

    @Test void uppercase_named_condition_passes() {
        var dt = new RDataType(); dt.setName("Foo");
        var cond = new RCondition(); cond.setName("MyCondition");
        dt.conditions().add(cond);
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    @Test void type_alias_conditions_are_checked() {
        // Upstream RosettaTypeAlias extends RosettaTypeWithConditions — its
        // conditions are Condition EObjects, name-checked like any other.
        var ta = new RTypeAlias(); ta.setName("maxBounded");
        var cond = new RCondition(); cond.setName("lowerBound");
        ta.conditions().add(cond);
        var c = new ValidationCollector();
        validator.validate(ta, c);
        assertEquals(1, c.toList().size());
        assertEquals("Condition name should start with a capital", c.toList().get(0).message());
    }

    // === Empty/null names don't crash ========================================

    @Test void null_name_no_crash() {
        var dt = new RDataType(); // name is null
        var c = new ValidationCollector();
        validator.validate(dt, c);
        // Should not crash
    }
}
