package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.enums.QualifiableKind;
import com.regnosys.rosetta.ast.model.RImport;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.model.RQualifiableConfig;
import com.regnosys.rosetta.ast.model.RScope;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RParameter;
import com.regnosys.rosetta.ast.supporting.RRecordFeature;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgument;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgumentExpression;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the model root and supporting type nodes (Task 2).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, list fields, and the {@code children()} traversal.
 */
class ModelNodeTest extends BaseAstTest {

    // =========================================================================
    // RNode contract — null-safety on mutators
    // =========================================================================

    @Test
    void rNode_setSourceRangeRejectsNull() {
        RModel node = new RModel();
        assertThrows(NullPointerException.class, () -> node.setSourceRange(null));
    }

    @Test
    void rNode_setTokenRangesRejectsNull() {
        RModel node = new RModel();
        assertThrows(NullPointerException.class, () -> node.setTokenRanges(null));
    }

    @Test
    void rNode_putTokenRangeRejectsNullName() {
        RModel node = new RModel();
        assertThrows(NullPointerException.class,
                () -> node.putTokenRange(null, SourceRange.NONE));
    }

    @Test
    void rNode_putTokenRangeRejectsNullRange() {
        RModel node = new RModel();
        assertThrows(NullPointerException.class,
                () -> node.putTokenRange("keyword", null));
    }

    @Test
    void rNode_setSourceRangeAcceptsNoneSentinel() {
        RModel node = new RModel();
        node.setSourceRange(SourceRange.NONE);
        assertEquals(SourceRange.NONE, node.sourceRange());
    }

    // =========================================================================
    // RModel
    // =========================================================================

    @Test
    void rModel_defaultState() {
        RModel model = new RModel();
        assertFalse(model.isOverride());
        assertNull(model.namespace());
        assertEquals(Optional.empty(), model.definition());
        assertEquals(Optional.empty(), model.scope());
        assertEquals(Optional.empty(), model.version());
        assertTrue(model.imports().isEmpty());
        assertTrue(model.configurations().isEmpty());
        assertTrue(model.rootElements().isEmpty());
        assertTrue(model.children().isEmpty());
    }

    @Test
    void rModel_setNamespaceAndOverride() {
        RModel model = new RModel();
        model.setNamespace("com.example.model");
        model.setOverride(true);

        assertEquals("com.example.model", model.namespace());
        assertTrue(model.isOverride());
    }

    @Test
    void rModel_setDefinitionAndVersion() {
        RModel model = new RModel();
        model.setDefinition("A sample model.");
        model.setVersion("1.0.0");

        assertEquals(Optional.of("A sample model."), model.definition());
        assertEquals(Optional.of("1.0.0"), model.version());
    }

    @Test
    void rModel_implementsRDefinable() {
        RModel model = new RModel();
        assertInstanceOf(RDefinable.class, model);
        assertEquals(Optional.empty(), model.definition());

        model.setDefinition("Test definition");
        assertEquals(Optional.of("Test definition"), model.definition());
    }

    @Test
    void rModel_setScope() {
        RModel model = new RModel();
        RScope scope = new RScope();
        scope.setName("MyScope");

        model.setScope(scope);
        assertTrue(model.scope().isPresent());
        assertEquals("MyScope", model.scope().get().name());
    }

    @Test
    void rModel_addImports() {
        RModel model = new RModel();

        RImport import1 = new RImport();
        import1.setQualifiedName("com.example.types");
        import1.setWildcard(true);

        RImport import2 = new RImport();
        import2.setQualifiedName("com.example.enums.Color");

        model.imports().add(import1);
        model.imports().add(import2);

        assertEquals(2, model.imports().size());
        assertEquals("com.example.types", model.imports().get(0).qualifiedName());
        assertTrue(model.imports().get(0).isWildcard());
    }

    @Test
    void rModel_addConfigurations() {
        RModel model = new RModel();

        RQualifiableConfig config = new RQualifiableConfig();
        config.setKind(QualifiableKind.IS_EVENT);
        config.setRootTypeName("com.example.Event");

        model.configurations().add(config);

        assertEquals(1, model.configurations().size());
        assertEquals(QualifiableKind.IS_EVENT, model.configurations().get(0).kind());
    }

    @Test
    void rModel_childrenIncludesAllSubNodes() {
        RModel model = new RModel();

        RScope scope = new RScope();
        scope.setName("testScope");
        model.setScope(scope);

        RImport imp = new RImport();
        imp.setQualifiedName("com.example.A");
        model.imports().add(imp);

        RQualifiableConfig cfg = new RQualifiableConfig();
        cfg.setKind(QualifiableKind.IS_PRODUCT);
        cfg.setRootTypeName("com.example.Product");
        model.configurations().add(cfg);

        // children should include: scope, import, config
        List<? extends RNode> children = model.children();
        assertEquals(3, children.size());
        assertSame(scope, children.get(0));
        assertSame(imp, children.get(1));
        assertSame(cfg, children.get(2));
    }

    @Test
    void rModel_childrenWithoutScope() {
        RModel model = new RModel();

        RImport imp = new RImport();
        imp.setQualifiedName("com.example.A");
        model.imports().add(imp);

        // No scope set — children should only include the import
        List<? extends RNode> children = model.children();
        assertEquals(1, children.size());
        assertSame(imp, children.get(0));
    }

    @Test
    void rModel_extendsRNode() {
        RModel model = new RModel();
        assertInstanceOf(RNode.class, model);
        assertEquals(SourceRange.NONE, model.sourceRange());
    }

    // =========================================================================
    // RImport
    // =========================================================================

    @Test
    void rImport_defaultState() {
        RImport imp = new RImport();
        assertNull(imp.qualifiedName());
        assertFalse(imp.isWildcard());
        assertEquals(Optional.empty(), imp.alias());
    }

    @Test
    void rImport_setFields() {
        RImport imp = new RImport();
        imp.setQualifiedName("com.example.types");
        imp.setWildcard(true);
        imp.setAlias("Types");

        assertEquals("com.example.types", imp.qualifiedName());
        assertTrue(imp.isWildcard());
        assertEquals(Optional.of("Types"), imp.alias());
    }

    @Test
    void rImport_noAlias() {
        RImport imp = new RImport();
        imp.setQualifiedName("com.example.Foo");
        imp.setWildcard(false);

        assertEquals("com.example.Foo", imp.qualifiedName());
        assertFalse(imp.isWildcard());
        assertEquals(Optional.empty(), imp.alias());
    }

    @Test
    void rImport_isLeafNode() {
        RImport imp = new RImport();
        assertTrue(imp.children().isEmpty());
    }

    // =========================================================================
    // RScope
    // =========================================================================

    @Test
    void rScope_defaultState() {
        RScope scope = new RScope();
        assertNull(scope.name());
        assertEquals(Optional.empty(), scope.definition());
    }

    @Test
    void rScope_setFields() {
        RScope scope = new RScope();
        scope.setName("regulatoryScope");
        scope.setDefinition("A regulatory scope for reporting.");

        assertEquals("regulatoryScope", scope.name());
        assertEquals(Optional.of("A regulatory scope for reporting."), scope.definition());
    }

    @Test
    void rScope_implementsRDefinable() {
        RScope scope = new RScope();
        assertInstanceOf(RDefinable.class, scope);
    }

    @Test
    void rScope_isLeafNode() {
        RScope scope = new RScope();
        assertTrue(scope.children().isEmpty());
    }

    // =========================================================================
    // RQualifiableConfig
    // =========================================================================

    @Test
    void rQualifiableConfig_isEvent() {
        RQualifiableConfig cfg = new RQualifiableConfig();
        cfg.setKind(QualifiableKind.IS_EVENT);
        cfg.setRootTypeName("cdm.event.BusinessEvent");

        assertEquals(QualifiableKind.IS_EVENT, cfg.kind());
        assertEquals("cdm.event.BusinessEvent", cfg.rootTypeName());
    }

    @Test
    void rQualifiableConfig_isProduct() {
        RQualifiableConfig cfg = new RQualifiableConfig();
        cfg.setKind(QualifiableKind.IS_PRODUCT);
        cfg.setRootTypeName("cdm.product.EconomicTerms");

        assertEquals(QualifiableKind.IS_PRODUCT, cfg.kind());
        assertEquals("cdm.product.EconomicTerms", cfg.rootTypeName());
    }

    @Test
    void rQualifiableConfig_isLeafNode() {
        RQualifiableConfig cfg = new RQualifiableConfig();
        assertTrue(cfg.children().isEmpty());
    }

    // =========================================================================
    // RTypeCall
    // =========================================================================

    @Test
    void rTypeCall_defaultState() {
        RTypeCall tc = new RTypeCall();
        assertNull(tc.typeName());
        assertTrue(tc.arguments().isEmpty());
        assertTrue(tc.children().isEmpty());
    }

    @Test
    void rTypeCall_setTypeName() {
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("string");

        assertEquals("string", tc.typeName());
    }

    @Test
    void rTypeCall_addArguments() {
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("number");

        RTypeCallArgument arg1 = new RTypeCallArgument();
        arg1.setParameterName("digits");
        RTypeCallArgumentExpression val1 = new RTypeCallArgumentExpression();
        val1.setLiteralValue("18");
        arg1.setValue(val1);

        RTypeCallArgument arg2 = new RTypeCallArgument();
        arg2.setParameterName("fractionalDigits");
        RTypeCallArgumentExpression val2 = new RTypeCallArgumentExpression();
        val2.setLiteralValue("2");
        arg2.setValue(val2);

        tc.arguments().add(arg1);
        tc.arguments().add(arg2);

        assertEquals(2, tc.arguments().size());
        assertEquals("digits", tc.arguments().get(0).parameterName());
        assertEquals("fractionalDigits", tc.arguments().get(1).parameterName());
    }

    @Test
    void rTypeCall_childrenIncludesArguments() {
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("number");

        RTypeCallArgument arg = new RTypeCallArgument();
        arg.setParameterName("digits");
        tc.arguments().add(arg);

        List<? extends RNode> children = tc.children();
        assertEquals(1, children.size());
        assertSame(arg, children.get(0));
    }

    // =========================================================================
    // RTypeCallArgument
    // =========================================================================

    @Test
    void rTypeCallArgument_defaultState() {
        RTypeCallArgument arg = new RTypeCallArgument();
        assertNull(arg.parameterName());
        assertNull(arg.value());
    }

    @Test
    void rTypeCallArgument_setFields() {
        RTypeCallArgument arg = new RTypeCallArgument();
        arg.setParameterName("minLength");

        RTypeCallArgumentExpression expr = new RTypeCallArgumentExpression();
        expr.setLiteralValue("5");
        arg.setValue(expr);

        assertEquals("minLength", arg.parameterName());
        assertSame(expr, arg.value());
    }

    @Test
    void rTypeCallArgument_childrenIncludesValue() {
        RTypeCallArgument arg = new RTypeCallArgument();
        RTypeCallArgumentExpression expr = new RTypeCallArgumentExpression();
        arg.setValue(expr);

        assertEquals(1, arg.children().size());
        assertSame(expr, arg.children().get(0));
    }

    @Test
    void rTypeCallArgument_childrenEmptyWhenNoValue() {
        RTypeCallArgument arg = new RTypeCallArgument();
        assertTrue(arg.children().isEmpty());
    }

    // =========================================================================
    // RTypeCallArgumentExpression
    // =========================================================================

    @Test
    void rTypeCallArgumentExpression_defaultState() {
        RTypeCallArgumentExpression expr = new RTypeCallArgumentExpression();
        assertFalse(expr.isNegated());
        assertEquals(Optional.empty(), expr.nameValue());
        assertEquals(Optional.empty(), expr.literalValue());
    }

    @Test
    void rTypeCallArgumentExpression_nameValue() {
        RTypeCallArgumentExpression expr = new RTypeCallArgumentExpression();
        expr.setNameValue("digits");

        assertEquals(Optional.of("digits"), expr.nameValue());
        assertEquals(Optional.empty(), expr.literalValue());
    }

    @Test
    void rTypeCallArgumentExpression_literalValue() {
        RTypeCallArgumentExpression expr = new RTypeCallArgumentExpression();
        expr.setLiteralValue("42");

        assertEquals(Optional.empty(), expr.nameValue());
        assertEquals(Optional.of("42"), expr.literalValue());
    }

    @Test
    void rTypeCallArgumentExpression_negated() {
        RTypeCallArgumentExpression expr = new RTypeCallArgumentExpression();
        expr.setNegated(true);
        expr.setLiteralValue("1");

        assertTrue(expr.isNegated());
        assertEquals(Optional.of("1"), expr.literalValue());
    }

    @Test
    void rTypeCallArgumentExpression_isLeafNode() {
        RTypeCallArgumentExpression expr = new RTypeCallArgumentExpression();
        assertTrue(expr.children().isEmpty());
    }

    // =========================================================================
    // RCardinality
    // =========================================================================

    @Test
    void rCardinality_oneToOne() {
        RCardinality card = new RCardinality();
        card.setInf(1);
        card.setSup(1);
        card.setUnbounded(false);

        assertEquals(BigInteger.ONE, card.inf());
        assertEquals(BigInteger.ONE, card.sup());
        assertFalse(card.isUnbounded());
    }

    @Test
    void rCardinality_zeroToMany() {
        RCardinality card = new RCardinality();
        card.setInf(0);
        card.setSup(RCardinality.UNBOUNDED);

        assertEquals(BigInteger.ZERO, card.inf());
        assertEquals(RCardinality.UNBOUNDED, card.sup());
        assertTrue(card.isUnbounded());
    }

    @Test
    void rCardinality_range() {
        RCardinality card = new RCardinality();
        card.setInf(1);
        card.setSup(5);
        card.setUnbounded(false);

        assertEquals(BigInteger.ONE, card.inf());
        assertEquals(BigInteger.valueOf(5), card.sup());
        assertFalse(card.isUnbounded());
    }

    @Test
    void rCardinality_zeroToOne() {
        RCardinality card = new RCardinality();
        card.setInf(0);
        card.setSup(1);
        card.setUnbounded(false);

        assertEquals(BigInteger.ZERO, card.inf());
        assertEquals(BigInteger.ONE, card.sup());
        assertFalse(card.isUnbounded());
    }

    @Test
    void rCardinality_isLeafNode() {
        RCardinality card = new RCardinality();
        assertTrue(card.children().isEmpty());
    }

    @Test
    void rCardinality_unboundedSentinel() {
        assertEquals(BigInteger.valueOf(-1), RCardinality.UNBOUNDED);
    }

    @Test
    void rCardinality_bigIntegerOverflow() {
        // Real-world Rune DSL files could theoretically use cardinalities that
        // exceed Java int range. Verify the AST handles this without precision loss.
        RCardinality card = new RCardinality();
        BigInteger huge = new BigInteger("9999999999999999999999999");
        card.setInf(huge);
        card.setSup(huge);
        assertEquals(huge, card.inf());
        assertEquals(huge, card.sup());
    }

    @Test
    void rCardinality_invariant_setSupUnboundedSetsFlag() {
        RCardinality card = new RCardinality();
        card.setSup(RCardinality.UNBOUNDED);
        assertTrue(card.isUnbounded());
        assertEquals(RCardinality.UNBOUNDED, card.sup());
    }

    @Test
    void rCardinality_invariant_setUnboundedFalseClearsSentinel() {
        RCardinality card = new RCardinality();
        card.setUnbounded(true);
        assertEquals(RCardinality.UNBOUNDED, card.sup());
        // Clearing the flag should reset sup to ZERO so the invariant holds
        card.setUnbounded(false);
        assertFalse(card.isUnbounded());
        assertEquals(BigInteger.ZERO, card.sup());
    }

    @Test
    void rCardinality_invariant_setSupClearsUnboundedFlag() {
        RCardinality card = new RCardinality();
        card.setUnbounded(true);
        assertTrue(card.isUnbounded());
        card.setSup(5);
        assertFalse(card.isUnbounded());
        assertEquals(BigInteger.valueOf(5), card.sup());
    }

    // =========================================================================
    // RTypeParameter
    // =========================================================================

    @Test
    void rTypeParameter_defaultState() {
        RTypeParameter tp = new RTypeParameter();
        assertNull(tp.name());
        assertNull(tp.typeCall());
        assertEquals(Optional.empty(), tp.definition());
    }

    @Test
    void rTypeParameter_setFields() {
        RTypeParameter tp = new RTypeParameter();
        tp.setName("digits");

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("int");
        tp.setTypeCall(tc);

        tp.setDefinition("The number of digits.");

        assertEquals("digits", tp.name());
        assertEquals("int", tp.typeCall().typeName());
        assertEquals(Optional.of("The number of digits."), tp.definition());
    }

    @Test
    void rTypeParameter_implementsRDefinable() {
        RTypeParameter tp = new RTypeParameter();
        assertInstanceOf(RDefinable.class, tp);
    }

    @Test
    void rTypeParameter_childrenIncludesTypeCall() {
        RTypeParameter tp = new RTypeParameter();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("int");
        tp.setTypeCall(tc);

        List<? extends RNode> children = tp.children();
        assertEquals(1, children.size());
        assertSame(tc, children.get(0));
    }

    @Test
    void rTypeParameter_childrenEmptyWhenNoTypeCall() {
        RTypeParameter tp = new RTypeParameter();
        assertTrue(tp.children().isEmpty());
    }

    // =========================================================================
    // RRecordFeature
    // =========================================================================

    @Test
    void rRecordFeature_defaultState() {
        RRecordFeature rf = new RRecordFeature();
        assertNull(rf.name());
        assertNull(rf.typeCall());
    }

    @Test
    void rRecordFeature_setFields() {
        RRecordFeature rf = new RRecordFeature();
        rf.setName("year");

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("int");
        rf.setTypeCall(tc);

        assertEquals("year", rf.name());
        assertEquals("int", rf.typeCall().typeName());
    }

    @Test
    void rRecordFeature_childrenIncludesTypeCall() {
        RRecordFeature rf = new RRecordFeature();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("int");
        rf.setTypeCall(tc);

        List<? extends RNode> children = rf.children();
        assertEquals(1, children.size());
        assertSame(tc, children.get(0));
    }

    @Test
    void rRecordFeature_childrenEmptyWhenNoTypeCall() {
        RRecordFeature rf = new RRecordFeature();
        assertTrue(rf.children().isEmpty());
    }

    // =========================================================================
    // RParameter
    // =========================================================================

    @Test
    void rParameter_defaultState() {
        RParameter param = new RParameter();
        assertNull(param.name());
        assertNull(param.typeCall());
        assertFalse(param.isArray());
    }

    @Test
    void rParameter_setFields() {
        RParameter param = new RParameter();
        param.setName("inputs");

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("number");
        param.setTypeCall(tc);
        param.setArray(true);

        assertEquals("inputs", param.name());
        assertEquals("number", param.typeCall().typeName());
        assertTrue(param.isArray());
    }

    @Test
    void rParameter_childrenIncludesTypeCall() {
        RParameter param = new RParameter();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("string");
        param.setTypeCall(tc);

        List<? extends RNode> children = param.children();
        assertEquals(1, children.size());
        assertSame(tc, children.get(0));
    }

    @Test
    void rParameter_childrenEmptyWhenNoTypeCall() {
        RParameter param = new RParameter();
        assertTrue(param.children().isEmpty());
    }

    @Test
    void rParameter_nonArrayByDefault() {
        RParameter param = new RParameter();
        param.setName("value");
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("int");
        param.setTypeCall(tc);

        assertFalse(param.isArray());
    }

    // =========================================================================
    // Integration: Full model tree
    // =========================================================================

    @Test
    void fullModelTree_childrenTraversal() {
        // Build a realistic model tree
        RModel model = new RModel();
        model.setNamespace("com.example.model");
        model.setVersion("1.0.0");

        RScope scope = new RScope();
        scope.setName("testScope");
        scope.setDefinition("The test scope");
        model.setScope(scope);

        RImport imp1 = new RImport();
        imp1.setQualifiedName("com.example.types");
        imp1.setWildcard(true);
        model.imports().add(imp1);

        RImport imp2 = new RImport();
        imp2.setQualifiedName("com.example.enums.Color");
        imp2.setAlias("ColorEnum");
        model.imports().add(imp2);

        RQualifiableConfig cfg = new RQualifiableConfig();
        cfg.setKind(QualifiableKind.IS_EVENT);
        cfg.setRootTypeName("com.example.Event");
        model.configurations().add(cfg);

        // Verify all children are present in order: scope, imports, configs
        List<? extends RNode> children = model.children();
        assertEquals(4, children.size());
        assertSame(scope, children.get(0));
        assertSame(imp1, children.get(1));
        assertSame(imp2, children.get(2));
        assertSame(cfg, children.get(3));
    }

    @Test
    void typeCallWithArguments_deepChildrenTraversal() {
        // Build a type call with arguments: number(digits: 18, fractionalDigits: 2)
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("number");

        RTypeCallArgument arg1 = new RTypeCallArgument();
        arg1.setParameterName("digits");
        RTypeCallArgumentExpression val1 = new RTypeCallArgumentExpression();
        val1.setLiteralValue("18");
        arg1.setValue(val1);

        RTypeCallArgument arg2 = new RTypeCallArgument();
        arg2.setParameterName("fractionalDigits");
        RTypeCallArgumentExpression val2 = new RTypeCallArgumentExpression();
        val2.setLiteralValue("2");
        arg2.setValue(val2);

        tc.arguments().add(arg1);
        tc.arguments().add(arg2);

        // Top-level children: the two arguments
        List<? extends RNode> tcChildren = tc.children();
        assertEquals(2, tcChildren.size());

        // Each argument's child: the expression
        assertEquals(1, arg1.children().size());
        assertSame(val1, arg1.children().get(0));

        // Expressions are leaves
        assertTrue(val1.children().isEmpty());
        assertTrue(val2.children().isEmpty());
    }

    // =========================================================================
    // Integration tests (parse + build)
    // =========================================================================
    //
    // These tests exercise the AstBuilder end-to-end: parsing .rosetta source
    // strings and verifying the resulting typed AST.

    @Nested
    class IntegrationParseAndBuild {

        @Test
        void parseMinimalNamespace() {
            var model = parseAndBuild("namespace com.example");
            assertEquals("com.example", model.namespace());
            assertFalse(model.isOverride());
            assertEquals(Optional.empty(), model.definition());
            assertEquals(Optional.empty(), model.scope());
            assertEquals(Optional.empty(), model.version());
            assertTrue(model.imports().isEmpty());
            assertTrue(model.configurations().isEmpty());
            assertTrue(model.rootElements().isEmpty());
        }

        @Test
        void parseNamespaceWithOverride() {
            var model = parseAndBuild("override namespace com.example");
            assertTrue(model.isOverride());
            assertEquals("com.example", model.namespace());
        }

        @Test
        void parseNamespaceWithDefinition() {
            var model = parseAndBuild("namespace com.example : <\"A sample model.\">");
            assertEquals("com.example", model.namespace());
            assertEquals(Optional.of("A sample model."), model.definition());
        }

        @Test
        void parseNamespaceWithColonButNoDefinition() {
            var model = parseAndBuild("namespace com.example :");
            assertEquals("com.example", model.namespace());
            assertEquals(Optional.empty(), model.definition());
        }

        @Test
        void parseNamespaceWithScope() {
            var model = parseAndBuild(
                    "namespace com.example\nscope myScope");
            assertTrue(model.scope().isPresent());
            assertEquals("myScope", model.scope().get().name());
            assertEquals(Optional.empty(), model.scope().get().definition());
        }

        @Test
        void parseNamespaceWithScopeAndDefinition() {
            var model = parseAndBuild(
                    "namespace com.example\nscope myScope <\"The scope\">");
            assertTrue(model.scope().isPresent());
            assertEquals("myScope", model.scope().get().name());
            assertEquals(Optional.of("The scope"), model.scope().get().definition());
        }

        @Test
        void parseNamespaceWithVersion() {
            var model = parseAndBuild(
                    "namespace com.example\nversion \"1.2.3\"");
            assertEquals(Optional.of("1.2.3"), model.version());
        }

        @Test
        void parseNamespaceWithWildcardImport() {
            var model = parseAndBuild(
                    "namespace com.example\nimport com.other.*");
            assertEquals("com.example", model.namespace());
            assertEquals(1, model.imports().size());
            var imp = model.imports().get(0);
            assertEquals("com.other", imp.qualifiedName());
            assertTrue(imp.isWildcard());
            assertEquals(Optional.empty(), imp.alias());
        }

        @Test
        void parseNamespaceWithExactImport() {
            var model = parseAndBuild(
                    "namespace com.example\nimport com.other.Foo");
            assertEquals(1, model.imports().size());
            var imp = model.imports().get(0);
            assertEquals("com.other.Foo", imp.qualifiedName());
            assertFalse(imp.isWildcard());
        }

        @Test
        void parseNamespaceWithAliasedImport() {
            var model = parseAndBuild(
                    "namespace com.example\nimport com.other.Foo as Bar");
            assertEquals(1, model.imports().size());
            var imp = model.imports().get(0);
            assertEquals("com.other.Foo", imp.qualifiedName());
            assertFalse(imp.isWildcard());
            assertEquals(Optional.of("Bar"), imp.alias());
        }

        @Test
        void parseNamespaceWithMultipleImports() {
            var model = parseAndBuild(
                    "namespace com.example\n"
                    + "import com.a.*\n"
                    + "import com.b.Foo\n"
                    + "import com.c.Bar as Baz");
            assertEquals(3, model.imports().size());

            assertEquals("com.a", model.imports().get(0).qualifiedName());
            assertTrue(model.imports().get(0).isWildcard());

            assertEquals("com.b.Foo", model.imports().get(1).qualifiedName());
            assertFalse(model.imports().get(1).isWildcard());

            assertEquals("com.c.Bar", model.imports().get(2).qualifiedName());
            assertEquals(Optional.of("Baz"), model.imports().get(2).alias());
        }

        @Test
        void parseNamespaceWithQualifiableConfigIsEvent() {
            var model = parseAndBuild(
                    "namespace com.example\nisEvent root com.example.MyEvent;");
            assertEquals(1, model.configurations().size());
            var cfg = model.configurations().get(0);
            assertEquals(QualifiableKind.IS_EVENT, cfg.kind());
            assertEquals("com.example.MyEvent", cfg.rootTypeName());
        }

        @Test
        void parseNamespaceWithQualifiableConfigIsProduct() {
            var model = parseAndBuild(
                    "namespace com.example\nisProduct root com.example.MyProduct;");
            assertEquals(1, model.configurations().size());
            var cfg = model.configurations().get(0);
            assertEquals(QualifiableKind.IS_PRODUCT, cfg.kind());
            assertEquals("com.example.MyProduct", cfg.rootTypeName());
        }

        @Test
        void parseFullModelHeader() {
            var model = parseAndBuild(
                    "override namespace com.example : <\"Full model\">\n"
                    + "scope testScope <\"The scope\">\n"
                    + "version \"2.0.0\"\n"
                    + "import com.a.*\n"
                    + "import com.b.Foo\n"
                    + "isEvent root com.example.MyEvent;");

            assertTrue(model.isOverride());
            assertEquals("com.example", model.namespace());
            assertEquals(Optional.of("Full model"), model.definition());
            assertTrue(model.scope().isPresent());
            assertEquals("testScope", model.scope().get().name());
            assertEquals(Optional.of("The scope"), model.scope().get().definition());
            assertEquals(Optional.of("2.0.0"), model.version());
            assertEquals(2, model.imports().size());
            assertEquals(1, model.configurations().size());
        }

        // -- Source range tests -----------------------------------------------

        @Test
        void sourceRangeSetOnModel() {
            var model = parseAndBuild("namespace com.example");
            assertSourceRangeSet(model);
        }

        @Test
        void sourceRangeSetOnImport() {
            var model = parseAndBuild(
                    "namespace com.example\nimport com.other.*");
            assertSourceRangeSet(model);
            assertSourceRangeSet(model.imports().get(0));
        }

        @Test
        void sourceRangeSetOnScope() {
            var model = parseAndBuild(
                    "namespace com.example\nscope myScope");
            assertSourceRangeSet(model);
            assertSourceRangeSet(model.scope().get());
        }

        @Test
        void sourceRangeSetOnQualifiableConfig() {
            var model = parseAndBuild(
                    "namespace com.example\nisEvent root com.example.Ev;");
            assertSourceRangeSet(model.configurations().get(0));
        }

        @Test
        void tokenRangesSetOnModel() {
            var model = parseAndBuild("namespace com.example");
            assertFalse(model.tokenRanges().isEmpty());
            assertNotNull(model.tokenRanges().get("keyword"));
            assertNotNull(model.tokenRanges().get("name"));
        }

        @Test
        void tokenRangesSetOnImport() {
            var model = parseAndBuild(
                    "namespace com.example\nimport com.other.*");
            var imp = model.imports().get(0);
            assertNotNull(imp.tokenRanges().get("keyword"));
            assertNotNull(imp.tokenRanges().get("name"));
        }

        // -- Parent pointer tests ---------------------------------------------

        @Test
        void parentPointerOnImport() {
            var model = parseAndBuild(
                    "namespace com.example\nimport com.other.*");
            assertNull(model.parent());
            assertSame(model, model.imports().get(0).parent());
        }

        @Test
        void parentPointerOnScope() {
            var model = parseAndBuild(
                    "namespace com.example\nscope myScope");
            assertSame(model, model.scope().get().parent());
        }

        @Test
        void parentPointerOnQualifiableConfig() {
            var model = parseAndBuild(
                    "namespace com.example\nisEvent root com.example.Ev;");
            assertSame(model, model.configurations().get(0).parent());
        }

        @Test
        void parentPointersMultipleChildren() {
            var model = parseAndBuild(
                    "namespace com.example\n"
                    + "scope myScope\n"
                    + "import com.a.*\n"
                    + "import com.b.Foo\n"
                    + "isEvent root com.example.Ev;");
            assertNull(model.parent());
            assertSame(model, model.scope().get().parent());
            assertSame(model, model.imports().get(0).parent());
            assertSame(model, model.imports().get(1).parent());
            assertSame(model, model.configurations().get(0).parent());
        }

        // -- Namespace from STRING literal ------------------------------------

        @Test
        void parseNamespaceFromStringLiteral() {
            var model = parseAndBuild("namespace \"com.example.quoted\"");
            assertEquals("com.example.quoted", model.namespace());
        }

        // -- Source range line/column sanity -----------------------------------

        @Test
        void sourceRangeLineColumnSanity() {
            // Use the explicit-fileName overload so the assertion is independent
            // of the derived-from-test-method behaviour of the parameterless
            // parseAndBuild() (Copilot review PR#2 round 6).
            var model = parseAndBuild("namespace com.example", "test.rosetta");
            var range = model.sourceRange();
            assertEquals("test.rosetta", range.file());
            assertEquals(1, range.startLine());
            assertEquals(1, range.startCol());
            assertTrue(range.endCol() > 0);
        }

        @Test
        void parseAndBuildDerivesFileNameFromTestMethod() {
            var model = parseAndBuild("namespace test");
            var range = model.sourceRange();
            // The parameterless overload should derive a unique file name from
            // the calling test method, so failures identify which test produced
            // a given range.
            assertTrue(range.file().contains("ModelNodeTest"),
                    "expected derived file name to contain test class, was: " + range.file());
            assertTrue(range.file().contains("parseAndBuildDerivesFileNameFromTestMethod"),
                    "expected derived file name to contain test method, was: " + range.file());
            assertTrue(range.file().endsWith(".rosetta"),
                    "expected derived file name to end with .rosetta, was: " + range.file());
        }

        @Test
        void importSourceRangeStartsOnCorrectLine() {
            var model = parseAndBuild(
                    "namespace com.example\nimport com.other.*");
            var impRange = model.imports().get(0).sourceRange();
            assertEquals(2, impRange.startLine());
        }

        // -- Error path tests ------------------------------------------------

        @Test
        void buildFromStringRejectsParseErrors() {
            assertThrows(
                    com.regnosys.rosetta.ast.builder.AstBuildException.class,
                    () -> com.regnosys.rosetta.ast.builder.AstBuilder
                            .buildFromString("not valid rune at all {{{{", "bad.rosetta"));
        }

        @Test
        void buildRejectsNullTree() {
            assertThrows(
                    com.regnosys.rosetta.ast.builder.AstBuildException.class,
                    () -> new com.regnosys.rosetta.ast.builder.AstBuilder("test.rosetta")
                            .build(null));
        }
    }
}
