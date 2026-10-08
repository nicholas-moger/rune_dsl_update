package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import com.regnosys.rosetta.types.inference.ExpressionTypeComputer;
import com.regnosys.rosetta.types.inference.TypeInferenceEngine;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the PR #442 override seat (upstream
 * {@code AttributeValidator.checkAttributeOverride} semantics: parent must
 * exist in a COMPLETE supertype chain; the overridden type must be a SUBTYPE
 * of the parent's — restriction legal; messages upstream-byte-identical).
 * Full-stack witnesses (data-type restriction, alias restriction, the choice
 * #797 rejection, the decline gates) live in {@code SmokeTypeInferenceTest}.
 */
class AttributeValidatorTest {

    private final SubtypeRelation subtypeRelation = new SubtypeRelation();
    private final AttributeValidator validator = new AttributeValidator(
            new TypeInferenceEngine(new ExpressionTypeComputer(
                    BuiltinTypeRegistry.createDefault(), subtypeRelation,
                    new TypeJoin(subtypeRelation), new TypeAliasSolver())),
            subtypeRelation);

    @Test void override_same_type_passes() {
        var resolver = new TestSymbolResolver();
        var parent = makeType("Parent", "value", "int");
        var child = makeType("Child", "value", "int");
        child.attributes().get(0).setOverride(true);
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(child, c);
        assertTrue(c.isEmpty());
        Reference.reachabilityFence(resolver);
    }

    @Test void override_non_subtype_errors_with_upstream_message() {
        var resolver = new TestSymbolResolver();
        var parent = makeType("Parent", "value", "int");
        var child = makeType("Child", "value", "string");
        child.attributes().get(0).setOverride(true);
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(child, c);
        assertEquals(1, c.toList().size());
        assertEquals(ValidationIssueCode.TYPE_ERROR, c.toList().get(0).issueCode());
        assertEquals("The overridden type should be a subtype of the parent type int",
                c.toList().get(0).message());
        Reference.reachabilityFence(resolver);
    }

    @Test void override_numeric_restriction_passes() {
        // int is a subtype of number (SubtypeRelation Rule 4) — the pre-#442
        // name-string comparison erred exactly this legal restriction class.
        var resolver = new TestSymbolResolver();
        var parent = makeType("Parent", "value", "number");
        var child = makeType("Child", "value", "int");
        child.attributes().get(0).setOverride(true);
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(child, c);
        assertTrue(c.isEmpty());
        Reference.reachabilityFence(resolver);
    }

    @Test void override_without_parent_attribute_errors_with_upstream_message() {
        var resolver = new TestSymbolResolver();
        var parent = new RDataType(); parent.setName("Parent");
        var child = makeType("Child", "value", "int");
        child.attributes().get(0).setOverride(true);
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(child, c);
        assertEquals(1, c.toList().size());
        assertEquals(ValidationIssueCode.MISSING_ATTRIBUTE, c.toList().get(0).issueCode());
        assertEquals("Attribute value does not exist in supertype", c.toList().get(0).message());
        Reference.reachabilityFence(resolver);
    }

    @Test void non_override_no_check() {
        var dt = makeType("Foo", "value", "int");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    @Test void no_super_type_errors_as_not_in_supertype() {
        // Upstream's getParentAttribute() is null both when the type has no
        // extends clause and when the name is absent from the chain — ONE
        // message covers both.
        var dt = makeType("Foo", "value", "int");
        dt.attributes().get(0).setOverride(true);
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertEquals(1, c.toList().size());
        assertEquals(ValidationIssueCode.MISSING_ATTRIBUTE, c.toList().get(0).issueCode());
        assertEquals("Attribute value does not exist in supertype", c.toList().get(0).message());
    }

    @Test void unresolved_super_type_declines() {
        // The extends clause names a parent the workspace cannot resolve: the
        // chain is INCOMPLETE, absence is unprovable, the check declines (the
        // #437 fabrication guard). A DELIBERATE recorded divergence: upstream
        // errors here (its getSuperType() returns null on an unresolved proxy,
        // so getParentAttribute() is null), but the fork's merged snapshot
        // corpora carry stale linking failures at scale and mirroring upstream
        // would fabricate corpus-wide.
        var dt = makeType("Foo", "value", "int");
        dt.attributes().get(0).setOverride(true);
        dt.setSuperTypeName("UnresolvedParent");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    @Test void non_data_type_ignored() {
        var en = new com.regnosys.rosetta.ast.types.REnumeration();
        en.setName("Color");
        var c = new ValidationCollector();
        validator.validate(en, c);
        assertTrue(c.isEmpty());
    }

    private RDataType makeType(String typeName, String attrName, String attrTypeName) {
        var dt = new RDataType(); dt.setName(typeName);
        var attr = new RAttribute(); attr.setName(attrName);
        var tc = new RTypeCall(); tc.setTypeName(attrTypeName);
        attr.setTypeCall(tc);
        dt.attributes().add(attr);
        return dt;
    }
}
