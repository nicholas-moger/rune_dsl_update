package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FunctionValidatorTest {

    private final FunctionValidator validator = new FunctionValidator();

    // === Extension parameter matching =========================================

    @Test void extension_matching_params_passes() {
        var resolver = new TestSymbolResolver();
        var parent = makeFunction("Parent", "int");
        addInput(parent, "x", "int");
        var child = makeFunction("Child", "int");
        addInput(child, "x", "int");
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperFunctionId);
        var c = new ValidationCollector();
        validator.validate(child, c);
        // PR #455: the bodyless fixture legitimately carries the
        // codeImplementation WARNING; the extension contract is ERROR-freedom.
        assertTrue(c.toList().stream().noneMatch(d -> d.severity() == Severity.ERROR));
        Reference.reachabilityFence(resolver);
    }

    @Test void extension_different_param_count_errors() {
        var resolver = new TestSymbolResolver();
        var parent = makeFunction("Parent", "int");
        addInput(parent, "x", "int");
        var child = makeFunction("Child", "int");
        addInput(child, "x", "int");
        addInput(child, "y", "string");
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperFunctionId);
        var c = new ValidationCollector();
        validator.validate(child, c);
        assertFalse(c.isEmpty());
        assertEquals(ValidationIssueCode.CHANGED_EXTENDED_FUNCTION_PARAMETERS, c.toList().get(0).issueCode());
        Reference.reachabilityFence(resolver);
    }

    @Test void extension_different_param_type_errors() {
        var resolver = new TestSymbolResolver();
        var parent = makeFunction("Parent", "int");
        addInput(parent, "x", "int");
        var child = makeFunction("Child", "int");
        addInput(child, "x", "string"); // different type
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperFunctionId);
        var c = new ValidationCollector();
        validator.validate(child, c);
        assertFalse(c.isEmpty());
        Reference.reachabilityFence(resolver);
    }

    @Test void extension_different_output_type_errors() {
        var resolver = new TestSymbolResolver();
        var parent = makeFunction("Parent", "int");
        var child = makeFunction("Child", "string"); // different output
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperFunctionId);
        var c = new ValidationCollector();
        validator.validate(child, c);
        assertFalse(c.isEmpty());
        Reference.reachabilityFence(resolver);
    }

    // === No extension — no check =============================================

    @Test void no_extension_passes() {
        var fn = makeFunction("Standalone", "int");
        var c = new ValidationCollector();
        validator.validate(fn, c);
        // PR #455: the bodyless fixture carries exactly the codeImplementation
        // WARNING (the warning-family wave); no errors.
        assertTrue(c.toList().stream().noneMatch(d -> d.severity() == Severity.ERROR));
        assertEquals(1, c.toList().stream().filter(d ->
            d.issueCode() == ValidationIssueCode.MISSING_IMPLEMENTATION).count());
    }

    // === Non-function ignored ================================================

    @Test void non_function_ignored() {
        var dt = new com.regnosys.rosetta.ast.types.RDataType();
        dt.setName("Foo");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    private RFunction makeFunction(String name, String outputType) {
        var fn = new RFunction();
        fn.setName(name);
        var out = new RAttribute();
        out.setName("result");
        var tc = new RTypeCall(); tc.setTypeName(outputType);
        out.setTypeCall(tc);
        fn.setOutput(out);
        return fn;
    }

    private void addInput(RFunction fn, String name, String type) {
        var attr = new RAttribute();
        attr.setName(name);
        var tc = new RTypeCall(); tc.setTypeName(type);
        attr.setTypeCall(tc);
        fn.inputs().add(attr);
    }
}
